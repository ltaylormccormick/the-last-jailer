package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckpointRebindAndStagingTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private lateinit var staging: CheckpointStaging

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
        staging = CheckpointStaging(Cp.stageStore(device))
    }

    // ---- rebinding -------------------------------------------------------------------------

    @Test
    fun `rebinding gives valid records under the new lineage and does not touch the originals`() {
        val originals = Cp.records(6, Cp.L1)
        val snapshot = originals.map { it.crc to it.lineage }

        val rebound = CheckpointSets.rebind(originals, Cp.L2)

        assertEquals(6, rebound.size)
        rebound.forEach {
            assertEquals(Cp.L2, it.lineage)
            assertTrue(it.hasValidCrc())
            assertNull(Cp.rules.validate(it, Cp.L2))
        }
        assertEquals(snapshot, originals.map { it.crc to it.lineage })
        originals.forEach { assertTrue(it.hasValidCrc()) }
        // Same content, new identity.
        assertEquals(originals.map { it.toGameState(1) }, rebound.map { it.toGameState(1) })
        // No record is shared between the two sets.
        rebound.forEachIndexed { i, r -> assertFalse(r === originals[i]) }
    }

    @Test
    fun `rebinding refuses a malformed lineage`() {
        assertThrows(IllegalArgumentException::class.java) { CheckpointSets.rebind(Cp.records(2), "bad") }
    }

    @Test
    fun `rebinding orders by chapter`() {
        val rebound = CheckpointSets.rebind(Cp.records(5).reversed(), Cp.L2)
        assertEquals(listOf(1, 2, 3, 4, 5), rebound.map { it.chapterNumber })
    }

    // ---- choosing what to copy -------------------------------------------------------------

    @Test
    fun `selection keeps chapters up to the live chapter and skips the abandoned future`() {
        val selection = CheckpointSets.selectCopyable(Cp.records(8), liveChapterNumber = 5)
        assertEquals(listOf(1, 2, 3, 4, 5), selection.kept.map { it.chapterNumber })
        assertEquals(listOf(6, 7, 8), selection.skipped.map { it.chapterNumber })
        assertTrue(selection.skipped.all { it.reason == RejectReason.CHAPTER_OUT_OF_RANGE })
    }

    @Test
    fun `selection skips a record whose CRC is wrong and a duplicate chapter`() {
        val good = Cp.records(4)
        val forged = Cp.withCrc(Cp.record(2), 5L)
        val selection = CheckpointSets.selectCopyable(good + forged + Cp.record(3), liveChapterNumber = 4)
        assertEquals(listOf(1, 2, 3, 4), selection.kept.map { it.chapterNumber })
        assertEquals(setOf(RejectReason.CRC_MISMATCH, RejectReason.DUPLICATE_CHAPTER), selection.skipped.map { it.reason }.toSet())
    }

    @Test
    fun `selection of nothing is nothing`() {
        val selection = CheckpointSets.selectCopyable(emptyList(), 10)
        assertTrue(selection.kept.isEmpty())
        assertTrue(selection.skipped.isEmpty())
    }

    @Test
    fun `withoutFuture matches selection`() {
        assertEquals(listOf(1, 2, 3), CheckpointSets.withoutFuture(Cp.records(6), 3).map { it.chapterNumber })
    }

    // ---- independence of a copy ------------------------------------------------------------

    @Test
    fun `a copied history is independent of its source in both directions`() {
        store.replaceAll(1, Cp.L1, Cp.records(6, Cp.L1))
        val source = store.readAll(1, Cp.L1)
        val copy = CheckpointSets.rebind(CheckpointSets.selectCopyable(source.records, 6).kept, Cp.L2)
        assertTrue(store.replaceAll(2, Cp.L2, copy).isCommitted)

        val sourceBefore = device.disk(Cp.file(1))
        val copyBefore = device.disk(Cp.file(2))
        assertEquals(6, store.readAll(2, Cp.L2).records.size)

        // Change the copy: the source must not move.
        store.removeFrom(2, Cp.L2, 3)
        store.upsert(2, Cp.L2, Cp.record(2, Cp.L2, Cp.state(2, flags = setOf("only in the copy"))))
        assertEquals(sourceBefore, device.disk(Cp.file(1)))

        // Change the source: the copy must not move.
        val copyAfter = device.disk(Cp.file(2))
        store.removeFrom(1, Cp.L1, 2)
        store.upsert(1, Cp.L1, Cp.record(1, Cp.L1, Cp.state(1, flags = setOf("only in the source"))))
        assertEquals(copyAfter, device.disk(Cp.file(2)))
        assertTrue(copyBefore != copyAfter)

        // Different lineages: neither can be read as the other.
        assertEquals(FileState.LINEAGE_MISMATCH, store.readAll(1, Cp.L2).fileState)
        assertEquals(FileState.LINEAGE_MISMATCH, store.readAll(2, Cp.L1).fileState)
    }

    @Test
    fun `copying into two slots gives three distinct lineages and no shared records`() {
        store.replaceAll(1, Cp.L1, Cp.records(4, Cp.L1))
        val source = store.readAll(1, Cp.L1).records
        store.replaceAll(2, Cp.L2, CheckpointSets.rebind(source, Cp.L2))
        store.replaceAll(3, Cp.L3, CheckpointSets.rebind(source, Cp.L3))
        val crcs = listOf(1 to Cp.L1, 2 to Cp.L2, 3 to Cp.L3).map { (slot, l) -> store.readAll(slot, l).records.map { it.crc } }
        assertEquals(3, crcs.toSet().size)
        assertEquals(4, store.readAll(3, Cp.L3).records.size)
    }

    // ---- staging ---------------------------------------------------------------------------

    @Test
    fun `staging writes and verifies without touching the real checkpoint file`() {
        store.replaceAll(2, Cp.L1, Cp.records(2, Cp.L1))
        val realBefore = device.disk(Cp.file(2))
        val records = Cp.records(5, Cp.L2)

        assertEquals(StageResult.Staged, staging.stage(2, Cp.L2, records))

        assertEquals(realBefore, device.disk(Cp.file(2)))
        assertEquals(records, staging.readVerified(2, Cp.L2))
        assertTrue(device.fileNames().contains(Cp.stageFile(2)))
    }

    @Test
    fun `promotion is a plain replace of the real file with the verified staged records`() {
        store.replaceAll(2, Cp.L1, Cp.records(2, Cp.L1))
        val records = Cp.records(5, Cp.L2)
        staging.stage(2, Cp.L2, records)

        val staged = staging.readVerified(2, Cp.L2)
        assertNotNull(staged)
        assertTrue(store.replaceAll(2, Cp.L2, staged!!).isCommitted)
        assertTrue(staging.discard(2).isCommitted)

        assertEquals(records, store.readAll(2, Cp.L2).records)
        assertNull(staging.readVerified(2, Cp.L2))
    }

    @Test
    fun `an empty staged history is staged, and is not the same as no staging`() {
        assertEquals(StageResult.Staged, staging.stage(2, Cp.L2, emptyList()))
        assertEquals(emptyList<CheckpointRecord>(), staging.readVerified(2, Cp.L2))
        staging.discard(2)
        assertNull(staging.readVerified(2, Cp.L2))
    }

    @Test
    fun `old staging content is overwritten, not merged`() {
        staging.stage(2, Cp.L2, Cp.records(6, Cp.L2))
        staging.stage(2, Cp.L3, Cp.records(2, Cp.L3))
        assertNull(staging.readVerified(2, Cp.L2))
        assertEquals(Cp.records(2, Cp.L3), staging.readVerified(2, Cp.L3))
    }

    @Test
    fun `invalid records are refused before anything is staged`() {
        val result = staging.stage(2, Cp.L2, Cp.records(3, Cp.L1)) // wrong lineage
        assertTrue(result is StageResult.Failed)
        assertTrue((result as StageResult.Failed).write is StoreWriteResult.Rejected)
        assertTrue(device.disk(Cp.stageFile(2)).isEmpty())
    }

    @Test
    fun `a staging write that fails and is rolled back leaves no staged data behind`() {
        device.armFault(Cp.stageFile(2), 1, CommitFault.FALSE_MEMORY_MUTATED)
        val result = staging.stage(2, Cp.L2, Cp.records(4, Cp.L2))

        assertTrue(result is StageResult.Failed)
        assertTrue((result as StageResult.Failed).write is StoreWriteResult.RolledBack)
        assertNull(staging.readVerified(2, Cp.L2))
        device.restart()
        assertNull(CheckpointStaging(Cp.stageStore(device)).readVerified(2, Cp.L2))
    }

    @Test
    fun `a staging write that cannot be recovered is reported as failed and the real file is untouched`() {
        store.replaceAll(2, Cp.L1, Cp.records(2, Cp.L1))
        val realBefore = device.disk(Cp.file(2))
        device.armFaultAlways(Cp.stageFile(2), CommitFault.FALSE_MEMORY_MUTATED)

        val result = staging.stage(2, Cp.L2, Cp.records(4, Cp.L2))

        assertTrue(result is StageResult.Failed)
        assertTrue((result as StageResult.Failed).write is StoreWriteResult.StateUnknown)
        assertNull(staging.readVerified(2, Cp.L2))   // untrusted, so nothing is offered for promotion
        assertEquals(realBefore, device.disk(Cp.file(2)))
        device.restart()
        assertNull(CheckpointStaging(Cp.stageStore(device)).readVerified(2, Cp.L2))
    }

    @Test
    fun `staged data damaged after staging is not offered for promotion`() {
        staging.stage(2, Cp.L2, Cp.records(5, Cp.L2))
        device.putDiskValue(Cp.stageFile(2), "c03.gold", 12345)       // CRC now wrong
        assertNull(CheckpointStaging(Cp.stageStore(device)).readVerified(2, Cp.L2))

        staging.stage(2, Cp.L2, Cp.records(5, Cp.L2))
        device.removeDiskKey(Cp.stageFile(2), "c04.scene")             // field missing
        assertNull(CheckpointStaging(Cp.stageStore(device)).readVerified(2, Cp.L2))

        staging.stage(2, Cp.L2, Cp.records(5, Cp.L2))
        device.removeDiskKey(Cp.stageFile(2), CheckpointCodec.HEADER_LINEAGE)  // header gone
        assertNull(CheckpointStaging(Cp.stageStore(device)).readVerified(2, Cp.L2))
    }

    @Test
    fun `staging for one slot never touches another slot's staging or real files`() {
        store.replaceAll(1, Cp.L1, Cp.records(3, Cp.L1))
        staging.stage(1, Cp.L3, Cp.records(2, Cp.L3))
        val one = device.disk(Cp.file(1))
        val stageOne = device.disk(Cp.stageFile(1))

        staging.stage(2, Cp.L2, Cp.records(4, Cp.L2))
        staging.discard(2)

        assertEquals(one, device.disk(Cp.file(1)))
        assertEquals(stageOne, device.disk(Cp.stageFile(1)))
    }
}
