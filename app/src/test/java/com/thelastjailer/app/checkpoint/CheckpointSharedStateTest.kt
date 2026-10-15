package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `StateUnknown` is a fact about one file's in-memory map, and Android gives every caller in a process
 * the same map. So every store built over the file must see it, and only a restart clears it.
 */
class CheckpointSharedStateTest {
    private lateinit var device: SimulatedDevice
    private lateinit var first: CheckpointStore
    private val file = Cp.file(1)
    private val old = Cp.records(4)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        first = Cp.store(device)
        assertTrue(first.replaceAll(1, Cp.L1, old).isCommitted)
    }

    private fun poison() {
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(first.replaceAll(1, Cp.L1, Cp.records(6)) is StoreWriteResult.StateUnknown)
        device.clearFaults()
    }

    @Test
    fun `a second store over the same file sees the slot as untrusted`() {
        val second = Cp.store(device)
        assertTrue(second.isTrusted(1))
        poison()
        assertFalse(first.isTrusted(1))
        assertFalse(second.isTrusted(1))
    }

    @Test
    fun `every write operation on a second store is refused and writes nothing`() {
        val second = Cp.store(device)
        poison()
        val disk = device.disk(file)
        val commits = device.commitCount(file)
        val attempts: List<Pair<String, StoreWriteResult>> = listOf(
            "replaceAll" to second.replaceAll(1, Cp.L1, old),
            "upsert" to second.upsert(1, Cp.L1, Cp.record(2)),
            "replaceFrom" to second.replaceFrom(1, Cp.L1, Cp.record(2)),
            "removeFrom" to second.removeFrom(1, Cp.L1, 2),
            "pruneAbove" to second.pruneAbove(1, Cp.L1, 1),
            "clear" to second.clear(1)
        )
        attempts.forEach { (name, result) ->
            assertTrue("$name should be Rejected but was $result", result is StoreWriteResult.Rejected)
        }
        assertEquals(disk, device.disk(file))
        assertEquals(commits, device.commitCount(file))
    }

    @Test
    fun `a second store's reads say the in-memory view may differ and its verify refuses`() {
        val second = Cp.store(device)
        poison()
        val read = second.readAll(1, Cp.L1)
        assertTrue(read.memoryMayDiffer)
        assertFalse(second.verify(1, Cp.L1, old))
    }

    @Test
    fun `a store built after the failure, in the same process, is also untrusted`() {
        poison()
        val late = Cp.store(device)
        assertFalse(late.isTrusted(1))
        assertTrue(late.upsert(1, Cp.L1, Cp.record(5)) is StoreWriteResult.Rejected)
    }

    @Test
    fun `other slots stay trusted for every store`() {
        val second = Cp.store(device)
        second.replaceAll(2, Cp.L2, Cp.records(2, Cp.L2))
        poison()
        assertTrue(first.isTrusted(2))
        assertTrue(second.isTrusted(2))
        assertTrue(second.replaceFrom(2, Cp.L2, Cp.record(3, Cp.L2)).isCommitted)
    }

    @Test
    fun `a staging file for the same slot number is a different file and is not affected`() {
        val staging = Cp.stageStore(device)
        poison()
        assertTrue(staging.isTrusted(1))
        assertTrue(staging.replaceAll(1, Cp.L1, Cp.records(2)).isCommitted)
    }

    @Test
    fun `only a restart clears it and the restarted process reads the truth from disk`() {
        val second = Cp.store(device)
        poison()
        device.restart()
        val restarted = Cp.store(device)
        assertTrue(restarted.isTrusted(1))
        assertEquals(old, restarted.readAll(1, Cp.L1).records)
        assertTrue(restarted.upsert(1, Cp.L1, Cp.record(5)).isCommitted)
        // The stores that existed before the restart look the file up again, so they recover too.
        assertTrue(first.isTrusted(1))
        assertTrue(second.isTrusted(1))
    }

    @Test
    fun `being rolled back is not state unknown and does not poison other stores`() {
        val second = Cp.store(device)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(first.replaceAll(1, Cp.L1, Cp.records(6)) is StoreWriteResult.RolledBack)
        assertTrue(second.isTrusted(1))
        assertTrue(second.upsert(1, Cp.L1, Cp.record(5)).isCommitted)
    }

    @Test
    fun `a refusal because of a bad header does not poison the slot`() {
        device.putDiskValue(file, "h.version", 9)
        val second = Cp.store(device)
        assertTrue(first.upsert(1, Cp.L1, Cp.record(5)) is StoreWriteResult.Rejected)
        assertTrue(second.isTrusted(1))
    }

    @Test
    fun `the failed-write state of different devices never leaks between them`() {
        val otherDevice = SimulatedDevice()
        val other = Cp.store(otherDevice)
        poison()
        assertTrue(other.isTrusted(1))
        assertTrue(other.replaceAll(1, Cp.L1, old).isCommitted)
    }
}
