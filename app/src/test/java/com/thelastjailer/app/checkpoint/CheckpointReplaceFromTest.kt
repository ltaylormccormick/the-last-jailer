package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckpointReplaceFromTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private val file = Cp.file(1)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
    }

    private fun fresh(n: Int, tag: String = "new") = Cp.rebuilt(Cp.record(n), flags = setOf(tag, "flag_$n"))

    private fun afterRestart(lineage: String = Cp.L1): CheckpointReadResult {
        device.restart()
        return Cp.store(device).readAll(1, lineage)
    }

    // ---- behaviour -------------------------------------------------------------------------

    @Test
    fun `on an empty file it initialises the header and writes the record`() {
        val r = fresh(1)
        assertTrue(store.replaceFrom(1, Cp.L1, r).isCommitted)
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertEquals(listOf(r), read.records)
        assertEquals(Cp.L1, device.disk(file)["h.lineage"])
        assertEquals(1, device.disk(file)["h.slot"])
    }

    @Test
    fun `it removes the record for K and everything above, and keeps everything below untouched`() {
        store.replaceAll(1, Cp.L1, Cp.records(8))
        val replacement = fresh(5)

        assertTrue(store.replaceFrom(1, Cp.L1, replacement).isCommitted)

        val read = store.readAll(1, Cp.L1)
        assertEquals(Cp.records(4) + replacement, read.records)
        assertTrue(read.rejected.isEmpty())
        assertEquals(listOf(1, 2, 3, 4, 5), read.records.map { it.chapterNumber })
    }

    @Test
    fun `it replaces K rather than merging into it`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        val replacement = Cp.rebuilt(Cp.record(5), flags = setOf("only_this"), inventory = listOf("proper_helmet"))
        store.replaceFrom(1, Cp.L1, replacement)
        val got = store.readAll(1, Cp.L1).records.single { it.chapterNumber == 5 }
        assertEquals(setOf("only_this"), got.flags)
        assertEquals(listOf("proper_helmet"), got.inventory)
    }

    @Test
    fun `K equal to one clears all history and K equal to thirty appends the last chapter`() {
        store.replaceAll(1, Cp.L1, Cp.records(9))
        val one = fresh(1)
        store.replaceFrom(1, Cp.L1, one)
        assertEquals(listOf(one), store.readAll(1, Cp.L1).records)

        store.replaceAll(1, Cp.L1, Cp.records(29))
        val thirty = fresh(30)
        store.replaceFrom(1, Cp.L1, thirty)
        assertEquals(Cp.records(29) + thirty, store.readAll(1, Cp.L1).records)
    }

    @Test
    fun `it also removes malformed leftovers in groups at or above K but never touches groups below K`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        device.putDiskValue(file, "c02.junk", "left over below")   // a stray extra key inside group 2
        device.putDiskValue(file, "c07.scene", "half a record")    // a broken group above K
        device.putDiskValue(file, "c04.junk", "stray key inside K") // a stray extra key inside K's own group
        store.replaceFrom(1, Cp.L1, fresh(4))

        val disk = device.disk(file)
        assertTrue("stray key inside K must be gone", "c04.junk" !in disk)
        assertEquals("left over below", disk["c02.junk"])
        assertTrue("groups above K must be gone", disk.keys.none { it.startsWith("c07.") })
        assertTrue(disk.keys.any { it.startsWith("c04.") })
    }

    @Test
    fun `it is exactly one commit`() {
        store.replaceAll(1, Cp.L1, Cp.records(6))
        val before = device.commitCount(file)
        store.replaceFrom(1, Cp.L1, fresh(3))
        assertEquals(before + 1, device.commitCount(file))
    }

    @Test
    fun `repeating the same arrival leaves the identical file`() {
        store.replaceAll(1, Cp.L1, Cp.records(6))
        val r = fresh(3)
        store.replaceFrom(1, Cp.L1, r)
        val once = device.disk(file)
        store.replaceFrom(1, Cp.L1, r)
        assertEquals(once, device.disk(file))
    }

    @Test
    fun `a second arrival into an earlier chapter discards the first arrival's future`() {
        store.replaceAll(1, Cp.L1, Cp.records(4))
        store.replaceFrom(1, Cp.L1, fresh(5))
        store.replaceFrom(1, Cp.L1, fresh(5, "again"))
        store.replaceFrom(1, Cp.L1, fresh(3, "earlier"))
        assertEquals(listOf(1, 2, 3), store.readAll(1, Cp.L1).records.map { it.chapterNumber })
    }

    // ---- refusals leave the file alone ------------------------------------------------------

    private fun assertRefusedUntouched(result: StoreWriteResult) {
        assertTrue("expected Rejected, got $result", result is StoreWriteResult.Rejected)
    }

    @Test
    fun `an invalid record is refused and nothing is written`() {
        store.replaceAll(1, Cp.L1, Cp.records(4))
        val disk = device.disk(file)
        val commits = device.commitCount(file)
        val bad = listOf(
            Cp.withCrc(fresh(5), 1L),
            Cp.rebuilt(fresh(5), sceneId = "no_such_scene"),
            Cp.rebuilt(fresh(5), gold = -1),
            Cp.record(5, Cp.L2),                       // a record for another lineage
            fresh(5).let { Cp.rebuilt(it, chapterNumber = 31, chapterId = "chapter_31") }
        )
        bad.forEach { assertRefusedUntouched(store.replaceFrom(1, Cp.L1, it)) }
        assertEquals(disk, device.disk(file))
        assertEquals(commits, device.commitCount(file))
    }

    @Test
    fun `a malformed lineage argument is refused`() {
        assertRefusedUntouched(store.replaceFrom(1, "short", fresh(2)))
        assertEquals(0, device.commitCount(file))
    }

    @Test
    fun `a file belonging to another lineage is refused untouched`() {
        store.replaceAll(1, Cp.L2, Cp.records(4, Cp.L2))
        val disk = device.disk(file)
        assertRefusedUntouched(store.replaceFrom(1, Cp.L1, fresh(5)))
        assertEquals(disk, device.disk(file))
    }

    @Test
    fun `slot numbers below one are rejected loudly`() {
        assertThrows(IllegalArgumentException::class.java) { store.replaceFrom(0, Cp.L1, fresh(1)) }
    }

    // ---- failure injection -----------------------------------------------------------------

    @Test
    fun `a failed commit that mutates memory is rolled back and memory, disk and restart all show the old file`() {
        val old = Cp.records(6)
        store.replaceAll(1, Cp.L1, old)
        val diskBefore = device.disk(file)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)

        val result = store.replaceFrom(1, Cp.L1, fresh(3))

        assertTrue("got $result", result is StoreWriteResult.RolledBack)
        assertEquals(old, store.readAll(1, Cp.L1).records)
        assertEquals(diskBefore, device.disk(file))
        assertTrue(store.isTrusted(1))
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `when the old file cannot be put back the result is state unknown, memory is not trusted and a restart shows the old file`() {
        val old = Cp.records(6)
        store.replaceAll(1, Cp.L1, old)
        val diskBefore = device.disk(file)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        device.armFault(file, 2, CommitFault.FALSE_UNCHANGED)

        val result = store.replaceFrom(1, Cp.L1, fresh(3))

        assertTrue("got $result", result is StoreWriteResult.StateUnknown)
        assertNotEquals(diskBefore, device.memory(file))     // memory shows an arrival that never reached disk
        assertEquals(diskBefore, device.disk(file))
        assertTrue(!store.isTrusted(1))
        assertTrue(store.readAll(1, Cp.L1).memoryMayDiffer)
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `an exception thrown by commit is a failure, not an escape`() {
        val old = Cp.records(4)
        store.replaceAll(1, Cp.L1, old)
        val flaky = Cp.storeOver(device) { commitThrowsOnce(device.prefs(file)) }
        val result = flaky.replaceFrom(1, Cp.L1, fresh(2))
        assertTrue("got $result", result is StoreWriteResult.RolledBack || result is StoreWriteResult.StateUnknown)
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `a crash before the commit leaves exactly the old file`() {
        val old = Cp.records(6)
        store.replaceAll(1, Cp.L1, old)
        device.armFault(file, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.replaceFrom(1, Cp.L1, fresh(3)) }
        val read = afterRestart()
        assertEquals(old, read.records)
        assertTrue(read.rejected.isEmpty())
    }

    @Test
    fun `a crash just after the commit leaves exactly the new file, never the future removed with K missing`() {
        store.replaceAll(1, Cp.L1, Cp.records(6))
        val r = fresh(3)
        device.armFault(file, 1, CommitFault.CRASH_AFTER_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.replaceFrom(1, Cp.L1, r) }
        val read = afterRestart()
        assertEquals(Cp.records(2) + r, read.records)
        assertTrue(read.rejected.isEmpty())
    }

    /** The file after each step of a scripted run of arrivals, including re-arrivals into earlier chapters. */
    private fun script(): List<CheckpointRecord> =
        (1..12).map { fresh(it, "a") } + listOf(fresh(7, "b"), fresh(8, "b"), fresh(4, "c"), fresh(5, "c"), fresh(6, "c"))

    private fun model(steps: Int): List<CheckpointRecord> {
        var current = emptyList<CheckpointRecord>()
        script().take(steps).forEach { r -> current = current.filter { it.chapterNumber < r.chapterNumber } + r }
        return current
    }

    @Test
    fun `a crash before or after any write of a scripted run leaves exactly the model state before or after that write`() {
        val steps = script().size
        for (n in 1..steps) {
            for (fault in listOf(CommitFault.CRASH_BEFORE_WRITE, CommitFault.CRASH_AFTER_WRITE)) {
                val d = SimulatedDevice()
                val s = Cp.store(d)
                d.armFault(Cp.file(1), n, fault)
                try {
                    script().forEach { assertTrue(s.replaceFrom(1, Cp.L1, it).isCommitted) }
                    throw AssertionError("expected a crash at write $n")
                } catch (e: SimulatedCrash) {
                    // expected
                }
                d.restart()
                val read = Cp.store(d).readAll(1, Cp.L1)
                val expected = if (fault == CommitFault.CRASH_BEFORE_WRITE) model(n - 1) else model(n)
                assertTrue("write $n $fault left rejects ${read.rejected}", read.rejected.isEmpty())
                if (expected.isEmpty()) {
                    assertTrue("write $n $fault", read.records.isEmpty())
                } else {
                    assertEquals("write $n $fault", expected, read.records)
                }
            }
        }
    }

    // ---- isolation -------------------------------------------------------------------------

    @Test
    fun `it never touches another slot, the ordinary save file or the entitlement file`() {
        device.writeDisk("jailer_saves", mapOf("1.scene" to "fallen_knight", "active_slot" to 1))
        device.writeDisk("jailer_entitlements", mapOf("full_story_unlocked" to true))
        store.replaceAll(1, Cp.L1, Cp.records(6))
        store.replaceAll(2, Cp.L2, Cp.records(4, Cp.L2))
        store.replaceAll(3, Cp.L3, Cp.records(2, Cp.L3))
        val others = listOf(Cp.file(2), Cp.file(3), "jailer_saves", "jailer_entitlements").associateWith { device.disk(it) }
        val commits = others.keys.associateWith { device.commitCount(it) }

        store.replaceFrom(1, Cp.L1, fresh(3))
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        store.replaceFrom(1, Cp.L1, fresh(2))

        others.forEach { (name, before) ->
            assertEquals("$name changed", before, device.disk(name))
            assertEquals("$name committed", commits[name], device.commitCount(name))
        }
    }

    @Test
    fun `a failed replaceFrom on one slot leaves a different slot fully usable`() {
        store.replaceAll(2, Cp.L2, Cp.records(4, Cp.L2))
        store.replaceAll(1, Cp.L1, Cp.records(4))
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(store.replaceFrom(1, Cp.L1, fresh(2)) is StoreWriteResult.StateUnknown)
        device.clearFaults()
        assertTrue(store.replaceFrom(2, Cp.L2, Cp.record(3, Cp.L2)).isCommitted)
        assertEquals(listOf(1, 2, 3), store.readAll(2, Cp.L2).records.map { it.chapterNumber })
    }
}
