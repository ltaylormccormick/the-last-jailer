package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Incremental writes must never modify or adopt a checkpoint file whose header is not intact for this
 * slot and lineage. Each case builds a healthy three-record file, damages the header one way, then
 * tries every incremental operation and checks that nothing at all changed.
 */
class CheckpointHeaderPreflightTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private val file = Cp.file(1)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(3)).isCommitted)
    }

    private val incrementalOperations: List<Pair<String, (CheckpointStore) -> StoreWriteResult>> = listOf(
        "upsert" to { s -> s.upsert(1, Cp.L1, Cp.record(4)) },
        "replaceFrom" to { s -> s.replaceFrom(1, Cp.L1, Cp.record(2)) },
        "removeFrom" to { s -> s.removeFrom(1, Cp.L1, 2) },
        "pruneAbove" to { s -> s.pruneAbove(1, Cp.L1, 1) }
    )

    private fun assertEveryIncrementalWriteRefusedAndNothingChanged(label: String) {
        val disk = device.disk(file)
        val commits = device.commitCount(file)
        incrementalOperations.forEach { (name, op) ->
            val result = op(store)
            assertTrue("$label: $name should be Rejected but was $result", result is StoreWriteResult.Rejected)
            assertEquals("$label: $name changed the disk", disk, device.disk(file))
            assertEquals("$label: $name committed", commits, device.commitCount(file))
            assertTrue("$label: $name must not make the slot untrusted", store.isTrusted(1))
        }
    }

    @Test
    fun `a newer format version is refused and never overwritten`() {
        device.putDiskValue(file, "h.version", CHECKPOINT_FORMAT_VERSION + 1)
        assertEquals(FileState.UNSUPPORTED_VERSION, store.readAll(1, Cp.L1).fileState)
        assertEveryIncrementalWriteRefusedAndNothingChanged("future version")
        assertEquals(CHECKPOINT_FORMAT_VERSION + 1, device.disk(file)["h.version"])
    }

    @Test
    fun `an older or zero format version is refused too`() {
        device.putDiskValue(file, "h.version", 0)
        assertEveryIncrementalWriteRefusedAndNothingChanged("version 0")
    }

    @Test
    fun `a file whose header names another slot is refused`() {
        device.putDiskValue(file, "h.slot", 2)
        assertEquals(FileState.SLOT_MISMATCH, store.readAll(1, Cp.L1).fileState)
        assertEveryIncrementalWriteRefusedAndNothingChanged("slot mismatch")
        assertEquals(2, device.disk(file)["h.slot"])
    }

    @Test
    fun `a file missing any one header key is refused`() {
        listOf("h.version", "h.slot", "h.lineage").forEach { missing ->
            setUp()
            device.removeDiskKey(file, missing)
            assertEquals("missing $missing", FileState.NO_HEADER, store.readAll(1, Cp.L1).fileState)
            assertEveryIncrementalWriteRefusedAndNothingChanged("missing $missing")
        }
    }

    @Test
    fun `a header value of the wrong type is refused`() {
        listOf("h.version" to "1", "h.slot" to "1", "h.lineage" to 7).forEach { (key, value) ->
            setUp()
            device.putDiskValue(file, key, value)
            assertEveryIncrementalWriteRefusedAndNothingChanged("wrong type for $key")
        }
    }

    @Test
    fun `a file with records but no header at all is refused`() {
        listOf("h.version", "h.slot", "h.lineage").forEach { device.removeDiskKey(file, it) }
        assertEveryIncrementalWriteRefusedAndNothingChanged("headerless")
    }

    @Test
    fun `a file holding only unrelated junk is refused, not adopted`() {
        device.wipeDisk(file)
        device.putDiskValue(file, "something", "else")
        assertEveryIncrementalWriteRefusedAndNothingChanged("junk only")
    }

    @Test
    fun `a file whose lineage differs from the caller's is refused`() {
        device.putDiskValue(file, "h.lineage", Cp.L2)
        assertEveryIncrementalWriteRefusedAndNothingChanged("other lineage")
        assertEquals(Cp.L2, device.disk(file)["h.lineage"])
    }

    @Test
    fun `an unreadable file is refused without escaping an exception`() {
        val broken = unreadablePrefs(device.prefs(file))
        val unreadableStore = CheckpointStore({ broken }, Cp.rules)
        val disk = device.disk(file)
        incrementalOperations.forEach { (name, op) ->
            val result = op(unreadableStore)
            assertTrue("$name: got $result", result is StoreWriteResult.Rejected)
        }
        assertEquals(disk, device.disk(file))
    }

    // ---- the supported exception: first write into an empty file --------------------------------

    @Test
    fun `an empty file may be initialised by upsert or replaceFrom`() {
        for (init in listOf<(CheckpointStore) -> StoreWriteResult>(
            { s -> s.upsert(1, Cp.L1, Cp.record(1)) },
            { s -> s.replaceFrom(1, Cp.L1, Cp.record(1)) }
        )) {
            val d = SimulatedDevice()
            val s = Cp.store(d)
            assertEquals(FileState.EMPTY, s.readAll(1, Cp.L1).fileState)
            assertTrue(init(s).isCommitted)
            assertEquals(FileState.OK, s.readAll(1, Cp.L1).fileState)
            assertEquals(listOf(1), s.readAll(1, Cp.L1).records.map { it.chapterNumber })
        }
    }

    @Test
    fun `removing from an empty file succeeds without writing anything`() {
        val d = SimulatedDevice()
        val s = Cp.store(d)
        assertTrue(s.removeFrom(1, Cp.L1, 1).isCommitted)
        assertTrue(s.pruneAbove(1, Cp.L1, 1).isCommitted)
        assertEquals(0, d.commitCount(Cp.file(1)))
        assertTrue(d.disk(Cp.file(1)).isEmpty())
    }

    @Test
    fun `removing nothing from an intact file writes nothing`() {
        val commits = device.commitCount(file)
        val disk = device.disk(file)
        assertTrue(store.removeFrom(1, Cp.L1, 9).isCommitted)
        assertTrue(store.pruneAbove(1, Cp.L1, 3).isCommitted)
        assertEquals(commits, device.commitCount(file))
        assertEquals(disk, device.disk(file))
    }

    @Test
    fun `removing really removes when there is something to remove`() {
        assertTrue(store.removeFrom(1, Cp.L1, 2).isCommitted)
        assertEquals(listOf(1), store.readAll(1, Cp.L1).records.map { it.chapterNumber })
        assertTrue(store.pruneAbove(1, Cp.L1, 0).isCommitted)
        assertTrue(store.readAll(1, Cp.L1).records.isEmpty())
        assertEquals(FileState.OK, store.readAll(1, Cp.L1).fileState)
    }

    // ---- the explicit whole-file operations remain the way out ----------------------------------

    @Test
    fun `replaceAll is the explicit recovery path over a file the incremental writes refuse`() {
        device.removeDiskKey(file, "h.lineage")
        assertEveryIncrementalWriteRefusedAndNothingChanged("headerless")
        val fresh = Cp.records(2)
        assertTrue(store.replaceAll(1, Cp.L1, fresh).isCommitted)
        assertEquals(fresh, store.readAll(1, Cp.L1).records)
        assertTrue(store.upsert(1, Cp.L1, Cp.record(3)).isCommitted)
    }

    @Test
    fun `clear empties a refused file and incremental writes work again afterwards`() {
        device.putDiskValue(file, "h.slot", 2)
        assertTrue(store.clear(1).isCommitted)
        assertTrue(store.upsert(1, Cp.L1, Cp.record(1)).isCommitted)
    }

    @Test
    fun `refusal reasons name the problem`() {
        device.putDiskValue(file, "h.version", 9)
        val r = store.upsert(1, Cp.L1, Cp.record(4)) as StoreWriteResult.Rejected
        assertTrue(r.reason, r.reason.contains("UNSUPPORTED_VERSION"))
    }

    @Test
    fun `other slots are not affected by one slot's bad header`() {
        store.replaceAll(2, Cp.L2, Cp.records(3, Cp.L2))
        device.putDiskValue(file, "h.version", 9)
        assertTrue(store.upsert(1, Cp.L1, Cp.record(4)) is StoreWriteResult.Rejected)
        assertTrue(store.upsert(2, Cp.L2, Cp.record(4, Cp.L2)).isCommitted)
    }
}
