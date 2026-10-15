package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Every test here separates three views of the same file: what the live process can read now ("memory"),
 * what is persisted ("disk"), and what a freshly restarted process reads ("after restart"). A failed
 * `commit()` can leave memory showing values that never reached disk, so none of these tests assume a
 * failure restores the old values by itself.
 */
class CheckpointFailedWriteTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore
    private val file = Cp.file(1)
    private val old = Cp.records(3)
    private val replacement = Cp.records(6).map { Cp.rebuilt(it, flags = setOf("replacement")) }

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
        assertTrue(store.replaceAll(1, Cp.L1, old).isCommitted)
    }

    private fun afterRestart(): CheckpointReadResult {
        device.restart()
        return Cp.store(device).readAll(1, Cp.L1)
    }

    // ---- the harness itself ----------------------------------------------------------------

    @Test
    fun `harness - a failed commit really does leave new values in memory while disk keeps the old`() {
        val diskBefore = device.disk(file)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        val ok = device.prefs(file).edit().putString("probe", "new").commit()

        assertFalse(ok)
        assertEquals("new", device.memory(file)["probe"])          // memory shows the failed write
        assertEquals(diskBefore, device.disk(file))                // disk does not
        device.restart()
        assertEquals(null, device.prefs(file).getString("probe", null)) // and a restart forgets it
    }

    @Test
    fun `harness - typed getters throw on a type mismatch like real Android`() {
        device.putDiskValue("x", "k", "text")
        assertThrows(ClassCastException::class.java) { device.prefs("x").getInt("k", 0) }
    }

    // ---- failed commit, previous contents recovered ----------------------------------------

    @Test
    fun `failed commit with memory mutation is rolled back only because the old contents were re-committed`() {
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)

        val result = store.replaceAll(1, Cp.L1, replacement)

        assertTrue(result is StoreWriteResult.RolledBack)
        // setUp's write, the write that failed, and the compensating re-commit that proved recovery.
        assertEquals(3, device.commitCount(file))
    }

    @Test
    fun `rolled back means memory, disk and a restarted process all show the previous data`() {
        val diskBefore = device.disk(file)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)

        val result = store.replaceAll(1, Cp.L1, replacement)

        assertTrue(result is StoreWriteResult.RolledBack)
        assertEquals(old, store.readAll(1, Cp.L1).records)          // memory
        assertEquals(diskBefore, device.disk(file))                  // disk
        assertTrue(store.isTrusted(1))
        assertEquals(old, afterRestart().records)                    // after restart
    }

    @Test
    fun `a failure that changes nothing is also reported as rolled back and leaves the old data`() {
        device.armFault(file, 1, CommitFault.FALSE_UNCHANGED)
        val result = store.replaceAll(1, Cp.L1, replacement)
        assertTrue(result is StoreWriteResult.RolledBack)
        assertEquals(old, store.readAll(1, Cp.L1).records)
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `upsert, removeFrom, pruneAbove and clear each roll back when their first commit fails`() {
        val diskBefore = device.disk(file)
        val operations: List<() -> StoreWriteResult> = listOf(
            { store.upsert(1, Cp.L1, Cp.record(2, state = Cp.state(2, flags = setOf("zzz")))) },
            { store.removeFrom(1, Cp.L1, 2) },
            { store.pruneAbove(1, Cp.L1, 1) },
            { store.clear(1) }
        )
        operations.forEach { operation ->
            device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
            val result = operation()
            assertTrue("expected RolledBack but got $result", result is StoreWriteResult.RolledBack)
            assertEquals(old, store.readAll(1, Cp.L1).records)
            assertEquals(diskBefore, device.disk(file))
        }
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `an exception thrown by commit is treated as a failure and rolled back`() {
        val throwing = commitThrowsOnce(device.prefs(file))
        val flaky = CheckpointStore({ throwing }, Cp.rules)

        val result = flaky.replaceAll(1, Cp.L1, replacement)

        assertTrue("got $result", result is StoreWriteResult.RolledBack)
        assertEquals(old, afterRestart().records)
    }

    // ---- failed commit, previous contents NOT recovered ------------------------------------

    @Test
    fun `when the re-commit also fails the result is state unknown, never rolled back`() {
        val diskBefore = device.disk(file)
        // Commit 1 (the write) fails and leaves its values in memory; commit 2 (the attempt to put the old
        // contents back) fails too and changes nothing, so memory keeps showing the write that never landed.
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        device.armFault(file, 2, CommitFault.FALSE_UNCHANGED)

        val result = store.replaceAll(1, Cp.L1, replacement)

        assertTrue("got $result", result is StoreWriteResult.StateUnknown)
        // The in-memory view now shows data that never reached disk. This is the trap.
        assertNotEquals(diskBefore, device.memory(file))
        assertEquals(diskBefore, device.disk(file))
        // So the store must not trust that memory.
        assertFalse(store.isTrusted(1))
        val read = store.readAll(1, Cp.L1)
        assertTrue(read.memoryMayDiffer)
        assertFalse(store.verify(1, Cp.L1, replacement))
        // A restart reads the truth from disk: the old data.
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `when every commit fails the result is state unknown and a restart shows the old data`() {
        val diskBefore = device.disk(file)
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)

        val result = store.replaceAll(1, Cp.L1, replacement)

        assertTrue("got $result", result is StoreWriteResult.StateUnknown)
        assertFalse(store.isTrusted(1))
        assertEquals(diskBefore, device.disk(file))
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `an untrusted slot refuses further writes until restart, other slots carry on`() {
        store.replaceAll(2, Cp.L2, Cp.records(2, Cp.L2))
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(store.replaceAll(1, Cp.L1, replacement) is StoreWriteResult.StateUnknown)
        device.clearFaults()

        val refused = store.upsert(1, Cp.L1, Cp.record(1))
        assertTrue(refused is StoreWriteResult.Rejected)
        assertTrue(store.clear(1) is StoreWriteResult.Rejected)
        assertTrue(store.isTrusted(2))
        assertTrue(store.upsert(2, Cp.L2, Cp.record(3, Cp.L2)).isCommitted)

        val fresh = Cp.store(device)
        device.restart()
        assertTrue(fresh.isTrusted(1))
        assertEquals(old, fresh.readAll(1, Cp.L1).records)
        assertTrue(fresh.upsert(1, Cp.L1, Cp.record(4)).isCommitted)
    }

    @Test
    fun `a clear that cannot be undone shows empty in memory but the records are still on disk`() {
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
        device.armFault(file, 2, CommitFault.FALSE_UNCHANGED)
        val result = store.clear(1)
        assertTrue(result is StoreWriteResult.StateUnknown)
        assertTrue(device.memory(file).isEmpty())
        assertEquals(old, afterRestart().records)
    }

    @Test
    fun `a re-commit that throws is also state unknown`() {
        val failsFirst = commitThrowsOnce(device.prefs(file))
        // First commit throws; make the re-commit throw too by wrapping a second time.
        val alwaysThrows = commitThrowsOnce(failsFirst)
        val flaky = CheckpointStore({ alwaysThrows }, Cp.rules)
        val result = flaky.replaceAll(1, Cp.L1, replacement)
        // The outer wrapper throws once; the inner throws once more on the re-commit.
        assertTrue("got $result", result is StoreWriteResult.StateUnknown)
        assertEquals(old, afterRestart().records)
    }

    // ---- crashes ---------------------------------------------------------------------------

    @Test
    fun `a crash before the write leaves exactly the old data after restart`() {
        device.armFault(file, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.replaceAll(1, Cp.L1, replacement) }
        val read = afterRestart()
        assertEquals(old, read.records)
        assertTrue(read.rejected.isEmpty())
    }

    @Test
    fun `a crash just after the write leaves exactly the new data, never a mixture`() {
        device.armFault(file, 1, CommitFault.CRASH_AFTER_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.replaceAll(1, Cp.L1, replacement) }
        val read = afterRestart()
        assertEquals(replacement, read.records)
        assertTrue(read.rejected.isEmpty())
    }

    @Test
    fun `a crash cannot be swallowed by the store`() {
        device.armFault(file, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.upsert(1, Cp.L1, Cp.record(2)) }
        device.armFault(file, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertThrows(SimulatedCrash::class.java) { store.clear(1) }
    }

    @Test
    fun `crashing before any one of thirty sequential writes leaves a clean prefix`() {
        for (n in 1..30) {
            val d = SimulatedDevice()
            val s = Cp.store(d)
            d.armFault(Cp.file(1), n, CommitFault.CRASH_BEFORE_WRITE)
            var written = 0
            try {
                for (chapter in 1..30) {
                    assertTrue(s.upsert(1, Cp.L1, Cp.record(chapter)).isCommitted)
                    written++
                }
            } catch (e: SimulatedCrash) {
                // expected exactly at write n
            }
            d.restart()
            val read = Cp.store(d).readAll(1, Cp.L1)
            assertEquals("crash before write $n", n - 1, written)
            assertEquals((1..written).toList(), read.records.map { it.chapterNumber })
            assertTrue(read.rejected.isEmpty())
        }
    }

    @Test
    fun `crashing after any one of thirty sequential writes leaves a clean prefix including that write`() {
        for (n in 1..30) {
            val d = SimulatedDevice()
            val s = Cp.store(d)
            d.armFault(Cp.file(1), n, CommitFault.CRASH_AFTER_WRITE)
            try {
                for (chapter in 1..30) s.upsert(1, Cp.L1, Cp.record(chapter))
            } catch (e: SimulatedCrash) {
                // expected exactly at write n
            }
            d.restart()
            val read = Cp.store(d).readAll(1, Cp.L1)
            assertEquals("crash after write $n", (1..n).toList(), read.records.map { it.chapterNumber })
            assertTrue(read.rejected.isEmpty())
        }
    }

    @Test
    fun `a failed write on one slot never changes another slot's persisted data`() {
        store.replaceAll(2, Cp.L2, Cp.records(4, Cp.L2))
        val two = device.disk(Cp.file(2))
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        store.replaceAll(1, Cp.L1, replacement)
        assertEquals(two, device.disk(Cp.file(2)))
        assertEquals(Cp.records(4, Cp.L2), store.readAll(2, Cp.L2).records)
    }
}
