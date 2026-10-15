package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A checkpoint file in a newer format belongs to a newer app version (the player may have downgraded).
 * The incremental writes already refuse it; the whole-file operations must not become a back door.
 */
class CheckpointNewerFormatGuardTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private val file = Cp.file(1)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(4)).isCommitted)
    }

    private fun makeNewer(version: Int = CHECKPOINT_FORMAT_VERSION + 1) = device.putDiskValue(file, "h.version", version)

    private fun assertRefusedAndUntouched(result: StoreWriteResult, label: String) {
        assertTrue("$label: expected Rejected but was $result", result is StoreWriteResult.Rejected)
        assertTrue("$label: ${(result as StoreWriteResult.Rejected).reason}", result.reason.contains("newer format"))
    }

    @Test
    fun `replaceAll refuses to overwrite a newer-format file`() {
        makeNewer()
        val disk = device.disk(file)
        val commits = device.commitCount(file)
        assertRefusedAndUntouched(store.replaceAll(1, Cp.L1, Cp.records(2)), "replaceAll")
        assertEquals(disk, device.disk(file))
        assertEquals(commits, device.commitCount(file))
        assertTrue(store.isTrusted(1))
    }

    @Test
    fun `clear refuses to wipe a newer-format file`() {
        makeNewer()
        val disk = device.disk(file)
        val commits = device.commitCount(file)
        assertRefusedAndUntouched(store.clear(1), "clear")
        assertEquals(disk, device.disk(file))
        assertEquals(commits, device.commitCount(file))
    }

    @Test
    fun `an unrecognised older or zero version is protected the same way`() {
        listOf(0, CHECKPOINT_FORMAT_VERSION + 5).forEach { v ->
            setUp()
            makeNewer(v)
            assertRefusedAndUntouched(store.replaceAll(1, Cp.L1, Cp.records(1)), "version $v replaceAll")
            assertRefusedAndUntouched(store.clear(1), "version $v clear")
        }
    }

    @Test
    fun `the full set of write operations leaves a newer-format file byte for byte unchanged`() {
        makeNewer()
        val disk = device.disk(file)
        listOf(
            store.replaceAll(1, Cp.L1, Cp.records(3)),
            store.clear(1),
            store.upsert(1, Cp.L1, Cp.record(5)),
            store.replaceFrom(1, Cp.L1, Cp.record(2)),
            store.removeFrom(1, Cp.L1, 2),
            store.pruneAbove(1, Cp.L1, 1)
        ).forEach { assertTrue("got $it", it is StoreWriteResult.Rejected) }
        assertEquals(disk, device.disk(file))
    }

    @Test
    fun `an explicit discard flag is the only way to destroy it`() {
        makeNewer()
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(2), discardNewerFormat = true).isCommitted)
        assertEquals(listOf(1, 2), store.readAll(1, Cp.L1).records.map { it.chapterNumber })

        setUp()
        makeNewer()
        assertTrue(store.clear(1, discardNewerFormat = true).isCommitted)
        assertTrue(device.disk(file).isEmpty())
    }

    @Test
    fun `the discard flag does not bypass the untrusted refusal`() {
        makeNewer()
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(2), discardNewerFormat = true) is StoreWriteResult.StateUnknown)
        device.clearFaults()
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(2), discardNewerFormat = true) is StoreWriteResult.Rejected)
        assertTrue(store.clear(1, discardNewerFormat = true) is StoreWriteResult.Rejected)
    }

    @Test
    fun `the guard does not change replaceAll or clear on every other kind of file`() {
        val damages: List<Pair<String, () -> Unit>> = listOf(
            "healthy" to { },
            "wrong slot" to { device.putDiskValue(file, "h.slot", 2) },
            "other lineage" to { device.putDiskValue(file, "h.lineage", Cp.L2) },
            "no lineage" to { device.removeDiskKey(file, "h.lineage") },
            "no version" to { device.removeDiskKey(file, "h.version") },
            "wrong type version" to { device.putDiskValue(file, "h.version", "1") },
            "junk only" to { device.wipeDisk(file); device.putDiskValue(file, "x", "y") }
        )
        damages.forEach { (label, damage) ->
            setUp(); damage()
            assertTrue("$label replaceAll", store.replaceAll(1, Cp.L1, Cp.records(2)).isCommitted)
            assertEquals("$label", listOf(1, 2), store.readAll(1, Cp.L1).records.map { it.chapterNumber })
            setUp(); damage()
            assertTrue("$label clear", store.clear(1).isCommitted)
            assertTrue("$label", device.disk(file).isEmpty())
        }
    }

    @Test
    fun `an empty file can be replaced and cleared as before`() {
        val d = SimulatedDevice()
        val s = Cp.store(d)
        assertTrue(s.clear(1).isCommitted)
        assertTrue(s.replaceAll(1, Cp.L1, Cp.records(1)).isCommitted)
    }

    @Test
    fun `a newer-format file in a different slot does not block this slot`() {
        store.replaceAll(2, Cp.L2, Cp.records(2, Cp.L2))
        device.putDiskValue(Cp.file(2), "h.version", 9)
        assertTrue(store.replaceAll(1, Cp.L1, Cp.records(2)).isCommitted)
        assertTrue(store.replaceAll(2, Cp.L2, Cp.records(1, Cp.L2)) is StoreWriteResult.Rejected)
    }

    @Test
    fun `staging cannot clobber a newer-format staging file either`() {
        val stageStore = Cp.stageStore(device)
        stageStore.replaceAll(1, Cp.L1, Cp.records(2))
        device.putDiskValue(Cp.stageFile(1), "h.version", 9)
        val disk = device.disk(Cp.stageFile(1))
        val result = CheckpointStaging(stageStore).stage(1, Cp.L2, Cp.records(3, Cp.L2))
        assertTrue("got $result", result is StageResult.Failed)
        assertEquals(disk, device.disk(Cp.stageFile(1)))
    }

    @Test
    fun `the rejection tells the caller how to proceed deliberately`() {
        makeNewer()
        val r = store.replaceAll(1, Cp.L1, Cp.records(1)) as StoreWriteResult.Rejected
        assertTrue(r.reason, r.reason.contains("discardNewerFormat"))
    }
}
