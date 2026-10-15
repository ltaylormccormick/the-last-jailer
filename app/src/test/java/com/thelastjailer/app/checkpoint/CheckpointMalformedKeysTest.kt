package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Keys that look like chapter groups but can never be a valid record (`c5.`, `c123.`, `c00.`, `c31.`) are
 * damage or leftovers: the arrival and removal writes sweep them. Keys that do not look like a chapter
 * group at all (`C07.`, `x.c07.`, `h.extra`) are not ours and are never touched.
 */
class CheckpointMalformedKeysTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private val file = Cp.file(1)
    private val maxChapter = Cp.rules.maxChapter

    private val malformed = listOf("c5.scene", "c123.scene", "c00.x", "c31.x", "c99.gold", "c001.x", "c0.x")
    private val foreign = listOf("C07.x", "x.c07.scene", "h.extra", "jailer_other", "c.x", "cxx.y", "chapter07.x")

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(4)).isCommitted)
    }

    private fun plant() {
        (malformed + foreign).forEach { device.putDiskValue(file, it, "j") }
    }

    private fun fresh(n: Int) = Cp.rebuilt(Cp.record(n), flags = setOf("new"))

    // ---- classification ----------------------------------------------------------------------

    @Test
    fun `the classifier flags exactly the group-shaped keys that cannot be valid`() {
        val flagged = CheckpointCodec.malformedGroupKeys(malformed + foreign + listOf("c01.scene", "c30.crc", "h.slot"), maxChapter)
        assertEquals(malformed.toSet(), flagged.toSet())
    }

    @Test
    fun `the boundaries are exactly chapter one and the last chapter`() {
        assertTrue(CheckpointCodec.malformedGroupKeys(listOf("c00.x"), maxChapter).isNotEmpty())
        assertTrue(CheckpointCodec.malformedGroupKeys(listOf("c01.x", "c${maxChapter}.x"), maxChapter).isEmpty())
        assertTrue(CheckpointCodec.malformedGroupKeys(listOf("c${maxChapter + 1}.x"), maxChapter).isNotEmpty())
    }

    // ---- the arrival write -------------------------------------------------------------------

    @Test
    fun `replaceFrom sweeps malformed group keys and leaves foreign keys and valid lower records alone`() {
        plant()
        assertTrue(store.replaceFrom(1, Cp.L1, fresh(3)).isCommitted)
        val disk = device.disk(file)
        malformed.forEach { assertFalse("$it should be gone", it in disk) }
        foreign.forEach { assertEquals("$it must survive", "j", disk[it]) }
        val read = store.readAll(1, Cp.L1)
        assertEquals(Cp.records(2) + fresh(3), read.records)
        assertTrue("no rejected entries should remain: ${read.rejected}", read.rejected.isEmpty())
    }

    @Test
    fun `after the sweep verify and staging reads work again`() {
        device.putDiskValue(file, "c00.scene", "x")
        assertFalse("sanity: the stray group is reported", store.readAll(1, Cp.L1).rejected.isEmpty())
        store.replaceFrom(1, Cp.L1, fresh(5))
        assertTrue(store.verify(1, Cp.L1, Cp.records(4) + fresh(5)))
    }

    @Test
    fun `the sweep and the arrival are one commit and a crash gives the old file or the clean new one`() {
        plant()
        val commits = device.commitCount(file)
        store.replaceFrom(1, Cp.L1, fresh(3))
        assertEquals(commits + 1, device.commitCount(file))

        for (fault in listOf(CommitFault.CRASH_BEFORE_WRITE, CommitFault.CRASH_AFTER_WRITE)) {
            val d = SimulatedDevice()
            val s = Cp.store(d)
            s.replaceAll(1, Cp.L1, Cp.records(4))
            (malformed + foreign).forEach { d.putDiskValue(file, it, "j") }
            val diskBefore = d.disk(file)
            d.armFault(file, 1, fault)
            try { s.replaceFrom(1, Cp.L1, fresh(3)) } catch (e: SimulatedCrash) { }
            d.restart()
            val after = d.disk(file)
            if (fault == CommitFault.CRASH_BEFORE_WRITE) {
                assertEquals(diskBefore, after)
            } else {
                assertTrue(malformed.none { it in after })
                assertEquals(Cp.records(2) + fresh(3), Cp.store(d).readAll(1, Cp.L1).records)
            }
        }
    }

    @Test
    fun `a failed sweep rolls back with the junk exactly as it was`() {
        plant()
        val disk = device.disk(file)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(store.replaceFrom(1, Cp.L1, fresh(3)) is StoreWriteResult.RolledBack)
        assertEquals(disk, device.disk(file))
        assertEquals(disk, device.memory(file))
    }

    // ---- removal ------------------------------------------------------------------------------

    @Test
    fun `removeFrom and pruneAbove sweep malformed keys even when no valid record is removed`() {
        plant()
        assertTrue(store.pruneAbove(1, Cp.L1, 9).isCommitted)    // nothing valid above 9
        val disk = device.disk(file)
        malformed.forEach { assertFalse(it in disk) }
        foreign.forEach { assertTrue(it in disk) }
        assertEquals(Cp.records(4), store.readAll(1, Cp.L1).records)

        setUp(); plant()
        assertTrue(store.removeFrom(1, Cp.L1, 99).isCommitted)
        malformed.forEach { assertFalse(it in device.disk(file)) }
    }

    @Test
    fun `a removal whose only work is malformed keys outside every removal range still sweeps them`() {
        // None of these is a two-digit group at or above the cut, so only the malformed sweep can remove them.
        val onlyMalformed = listOf("c5.scene", "c123.scene", "c00.x", "c0.x")
        onlyMalformed.forEach { device.putDiskValue(file, it, "j") }
        val commits = device.commitCount(file)
        assertTrue(store.pruneAbove(1, Cp.L1, 9).isCommitted)
        assertEquals(commits + 1, device.commitCount(file))
        onlyMalformed.forEach { assertFalse("$it should be gone", it in device.disk(file)) }
        assertEquals(Cp.records(4), store.readAll(1, Cp.L1).records)
        assertTrue(store.readAll(1, Cp.L1).rejected.isEmpty())
    }

    @Test
    fun `removal with nothing to remove and nothing malformed still writes nothing`() {
        device.putDiskValue(file, "h.extra", "keep")
        val commits = device.commitCount(file)
        assertTrue(store.removeFrom(1, Cp.L1, 9).isCommitted)
        assertTrue(store.pruneAbove(1, Cp.L1, 4).isCommitted)
        assertEquals(commits, device.commitCount(file))
    }

    @Test
    fun `removal removes the valid groups and the malformed keys in one commit`() {
        plant()
        val commits = device.commitCount(file)
        assertTrue(store.removeFrom(1, Cp.L1, 3).isCommitted)
        assertEquals(commits + 1, device.commitCount(file))
        assertEquals(listOf(1, 2), store.readAll(1, Cp.L1).records.map { it.chapterNumber })
        malformed.forEach { assertFalse(it in device.disk(file)) }
    }

    // ---- reads and the other writers never choke ----------------------------------------------

    @Test
    fun `foreign and malformed keys never crash a read and never produce a record`() {
        plant()
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertEquals(Cp.records(4), read.records)
    }

    @Test
    fun `upsert leaves malformed keys alone because it is not an arrival`() {
        plant()
        assertTrue(store.upsert(1, Cp.L1, fresh(2)).isCommitted)
        malformed.forEach { assertTrue("$it", it in device.disk(file)) }
    }

    @Test
    fun `replaceAll and clear remove everything including junk`() {
        plant()
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(2)).isCommitted)
        (malformed + foreign).forEach { assertFalse(it in device.disk(file)) }
        plant()
        assertTrue(store.clear(1).isCommitted)
        assertTrue(device.disk(file).isEmpty())
    }

    @Test
    fun `junk in one slot is never swept by another slot's writes`() {
        store.replaceAll(2, Cp.L2, Cp.records(3, Cp.L2))
        device.putDiskValue(Cp.file(2), "c00.x", "j")
        store.replaceFrom(1, Cp.L1, fresh(2))
        assertEquals("j", device.disk(Cp.file(2))["c00.x"])
    }
}
