package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Operations on one slot are serialised, across store objects, because snapshot -> commit -> restore is a
 * read-modify-write on one shared in-memory map. The first tests hold one thread inside a commit and prove
 * the others wait; the last two hammer a slot from many threads and check the file is always valid.
 */
class CheckpointConcurrencyTest {
    private lateinit var device: SimulatedDevice
    private val file = Cp.file(1)
    private val old = Cp.records(4)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        assertTrue(Cp.store(device).replaceAll(1, Cp.L1, old).isCommitted)
    }

    /** A store over slot 1 that goes through [gated] and over every other slot straight to the device. */
    private fun storeThrough(gated: android.content.SharedPreferences): CheckpointStore =
        Cp.storeOver(device) { slot -> if (slot == 1) gated else device.prefs(Cp.file(slot)) }

    private class Worker(body: () -> Unit) {
        val error = AtomicReference<Throwable?>(null)
        val done = CountDownLatch(1)
        private val thread = Thread {
            try { body() } catch (t: Throwable) { error.set(t) } finally { done.countDown() }
        }.apply { isDaemon = true }
        fun start() = apply { thread.start() }
        fun finished(waitMillis: Long) = done.await(waitMillis, TimeUnit.MILLISECONDS)
        fun joinOrFail() {
            assertTrue("worker did not finish", done.await(20, TimeUnit.SECONDS))
            error.get()?.let { throw AssertionError("worker failed: $it", it) }
        }
    }

    private fun fresh(n: Int, tag: String) = Cp.rebuilt(Cp.record(n), flags = setOf(tag))

    // ---- a writer cannot get in while another is between its snapshot and its restore ------------------

    @Test
    fun `a second writer waits while the first is mid-commit, and then the rollback cannot clobber it`() {
        val gate = CommitGate()
        val gated = gatedPrefs(device.prefs(file), pauseOnCommit = 1, gate = gate)
        val a = storeThrough(gated)
        val b = storeThrough(gated)                       // a different store object over the same file
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)   // A's commit fails, mutating memory

        val resultA = AtomicReference<StoreWriteResult>()
        val resultB = AtomicReference<StoreWriteResult>()
        val workerA = Worker { resultA.set(a.replaceAll(1, Cp.L1, Cp.records(8).map { fresh(it.chapterNumber, "A") })) }.start()
        assertTrue("A never reached its commit", gate.entered.await(10, TimeUnit.SECONDS))
        val commitsWhileAPaused = device.commitCount(file)

        val workerB = Worker { resultB.set(b.upsert(1, Cp.L1, fresh(2, "B"))) }.start()
        assertFalse("B must be blocked while A holds the slot", workerB.finished(400))
        assertEquals("B must not have committed while A was mid-commit", commitsWhileAPaused, device.commitCount(file))

        gate.release.countDown()
        workerA.joinOrFail(); workerB.joinOrFail()

        assertTrue("A: ${resultA.get()}", resultA.get() is StoreWriteResult.RolledBack)
        assertTrue("B: ${resultB.get()}", resultB.get().let { it != null && it.isCommitted })
        // Final state: the old file with B's record, and none of A's failed replacement.
        val read = Cp.store(device).readAll(1, Cp.L1)
        assertEquals(listOf(1, 2, 3, 4), read.records.map { it.chapterNumber })
        assertEquals(setOf("B"), read.records.single { it.chapterNumber == 2 }.flags)
        assertTrue(read.records.none { "A" in it.flags })
        device.restart()
        val persisted = Cp.store(device).readAll(1, Cp.L1)
        assertEquals(read.records, persisted.records)
    }

    @Test
    fun `a reader waits for a rollback in progress and never sees the failed write's values`() {
        val gate = CommitGate()
        // Pause the SECOND commit: the compensating re-commit. At that moment memory holds the failed write.
        val gated = gatedPrefs(device.prefs(file), pauseOnCommit = 2, gate = gate)
        val writer = storeThrough(gated)
        val reader = storeThrough(gated)
        device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)

        val resultW = AtomicReference<StoreWriteResult>()
        val seen = AtomicReference<CheckpointReadResult>()
        val w = Worker { resultW.set(writer.replaceAll(1, Cp.L1, Cp.records(8).map { fresh(it.chapterNumber, "W") })) }.start()
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS))
        assertTrue("sanity: memory really holds the failed write now", device.memory(file).keys.any { it.startsWith("c08.") })

        val r = Worker { seen.set(reader.readAll(1, Cp.L1)) }.start()
        assertFalse("the reader must wait for the restore", r.finished(400))

        gate.release.countDown()
        w.joinOrFail(); r.joinOrFail()

        assertTrue(resultW.get() is StoreWriteResult.RolledBack)
        assertEquals(old, seen.get().records)
        assertFalse(seen.get().memoryMayDiffer)
    }

    @Test
    fun `different slots never block each other`() {
        store2Setup()
        val gate = CommitGate()
        val gated = gatedPrefs(device.prefs(file), pauseOnCommit = 1, gate = gate)
        val a = storeThrough(gated)

        val workerA = Worker { a.upsert(1, Cp.L1, fresh(2, "A")) }.start()
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS))

        val other = AtomicReference<StoreWriteResult>()
        val workerB = Worker { other.set(a.upsert(2, Cp.L2, Cp.record(3, Cp.L2))) }.start()
        assertTrue("slot 2 must not wait for slot 1", workerB.finished(5000))
        workerB.joinOrFail()
        assertTrue(other.get().isCommitted)

        gate.release.countDown()
        workerA.joinOrFail()
    }

    private fun store2Setup() {
        assertTrue(Cp.store(device).replaceAll(2, Cp.L2, Cp.records(2, Cp.L2)).isCommitted)
    }

    @Test
    fun `reads in a fault-free moment are not blocked by writers on another slot`() {
        store2Setup()
        val gate = CommitGate()
        val gated = gatedPrefs(device.prefs(file), pauseOnCommit = 1, gate = gate)
        val a = storeThrough(gated)
        val w = Worker { a.upsert(1, Cp.L1, fresh(2, "A")) }.start()
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS))
        assertEquals(2, a.readAll(2, Cp.L2).records.size)
        gate.release.countDown()
        w.joinOrFail()
    }

    // ---- every operation takes the lock, as the one in progress and as the one waiting ----------------

    private val operations: List<Pair<String, (CheckpointStore) -> Any?>> = listOf(
        "replaceAll" to { s -> s.replaceAll(1, Cp.L1, Cp.records(3)) },
        "upsert" to { s -> s.upsert(1, Cp.L1, fresh(6, "u")) },
        "replaceFrom" to { s -> s.replaceFrom(1, Cp.L1, fresh(3, "rf")) },
        "removeFrom" to { s -> s.removeFrom(1, Cp.L1, 3) },
        "pruneAbove" to { s -> s.pruneAbove(1, Cp.L1, 2) },
        "clear" to { s -> s.clear(1) }
    )

    private val readers: List<Pair<String, (CheckpointStore) -> Any?>> = listOf(
        "readAll" to { s -> s.readAll(1, Cp.L1) },
        "storedLineage" to { s -> s.storedLineage(1) },
        "verify" to { s -> s.verify(1, Cp.L1, old) }
    )

    /**
     * Holds [first] inside its commit, starts [second] on another store object over a different wrapper of
     * the same file, and checks that [second] cannot make progress until [first] is released.
     */
    private fun assertSecondWaits(firstName: String, first: (CheckpointStore) -> Any?, secondName: String, second: (CheckpointStore) -> Any?, secondCommits: Boolean) {
        val gate = CommitGate()
        val gatedWrapper = gatedPrefs(device.prefs(file), pauseOnCommit = 1, gate = gate)
        val otherWrapper = object : android.content.SharedPreferences by device.prefs(file) {}
        val a = Cp.storeOver(device) { slot -> if (slot == 1) gatedWrapper else device.prefs(Cp.file(slot)) }
        val b = Cp.storeOver(device) { slot -> if (slot == 1) otherWrapper else device.prefs(Cp.file(slot)) }

        val workerA = Worker { first(a) }.start()
        assertTrue("$firstName never reached its commit", gate.entered.await(10, TimeUnit.SECONDS))
        val commitsWhilePaused = device.commitCount(file)
        val workerB = Worker { second(b) }.start()
        assertFalse("$secondName must wait while $firstName is mid-commit", workerB.finished(250))
        assertEquals("$secondName committed while $firstName was mid-commit", commitsWhilePaused, device.commitCount(file))

        gate.release.countDown()
        workerA.joinOrFail(); workerB.joinOrFail()
        if (secondCommits) assertTrue("$secondName should have committed after the release", device.commitCount(file) > commitsWhilePaused)
    }

    @Test
    fun `every write operation holds the lock while it is mid-commit`() {
        operations.forEach { (name, op) ->
            setUp()
            assertSecondWaits(name, op, "upsert", { s -> s.upsert(1, Cp.L1, fresh(2, "second")) }, secondCommits = true)
        }
    }

    @Test
    fun `every write operation waits for a writer that is mid-commit`() {
        operations.forEach { (name, op) ->
            setUp()
            assertSecondWaits("upsert", { s -> s.upsert(1, Cp.L1, fresh(4, "first")) }, name, op, secondCommits = true)
        }
    }

    @Test
    fun `every read waits for a writer that is mid-commit`() {
        readers.forEach { (name, op) ->
            setUp()
            assertSecondWaits("upsert", { s -> s.upsert(1, Cp.L1, fresh(4, "first")) }, name, op, secondCommits = false)
        }
    }

    // ---- stress ----------------------------------------------------------------------------------

    @Test
    fun `many threads upserting different chapters through different store objects lose nothing`() {
        val threads = 10
        val workers = (1..threads).map { t ->
            Worker {
                val s = Cp.store(device)
                repeat(5) { round -> assertTrue(s.upsert(1, Cp.L1, fresh(t + 4, "t$t-r$round")).isCommitted) }
            }.start()
        }
        workers.forEach { it.joinOrFail() }
        val read = Cp.store(device).readAll(1, Cp.L1)
        assertTrue(read.rejected.isEmpty())
        assertEquals((1..14).toList(), read.records.map { it.chapterNumber })
        (5..14).forEach { n ->
            assertEquals(setOf("t${n - 4}-r4"), read.records.single { it.chapterNumber == n }.flags)
        }
        device.restart()
        assertEquals(read.records, Cp.store(device).readAll(1, Cp.L1).records)
    }

    @Test
    fun `racing arrivals always leave a valid ascending file whichever wins`() {
        val workers = (1..8).map { t ->
            Worker {
                val s = Cp.store(device)
                repeat(10) { round ->
                    val chapter = ((t * 7 + round * 3) % 12) + 1
                    val result = s.replaceFrom(1, Cp.L1, fresh(chapter, "t$t-r$round"))
                    assertTrue("got $result", result.isCommitted)
                }
            }.start()
        }
        workers.forEach { it.joinOrFail() }
        val read = Cp.store(device).readAll(1, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertTrue("rejected: ${read.rejected}", read.rejected.isEmpty())
        val chapters = read.records.map { it.chapterNumber }
        assertEquals(chapters.sorted(), chapters)
        assertEquals(chapters.distinct(), chapters)
        assertTrue(Cp.store(device).isTrusted(1))
    }

    @Test
    fun `racing writers with failing commits never leave a poisoned or half-restored file`() {
        // Every third commit fails with memory mutation. Rollbacks and writes from many threads must not
        // clobber each other; the final file must be valid and the slot must still be trusted.
        val countdown = java.util.concurrent.atomic.AtomicInteger()
        val real = device.prefs(file)
        val flaky = object : android.content.SharedPreferences by real {
            override fun edit(): android.content.SharedPreferences.Editor {
                val inner = real.edit()
                return object : android.content.SharedPreferences.Editor by inner {
                    override fun commit(): Boolean {
                        val n = countdown.incrementAndGet()
                        if (n % 3 == 0) {
                            // fail like Android: mutate memory, report false (no disk write)
                            device.armFault(file, 1, CommitFault.FALSE_MEMORY_MUTATED)
                        }
                        return inner.commit()
                    }
                }
            }
        }
        val workers = (1..6).map { t ->
            Worker {
                val s = Cp.storeOver(device) { slot -> if (slot == 1) flaky else device.prefs(Cp.file(slot)) }
                repeat(8) { round -> s.upsert(1, Cp.L1, fresh(5 + (t + round) % 10, "t$t-r$round")) }
            }.start()
        }
        workers.forEach { it.joinOrFail() }
        val shared = Cp.store(device)
        val inProcess = Cp.storeOver(device) { flaky }.readAll(1, Cp.L1)
        assertTrue("in-process view must be valid: ${inProcess.rejected}", inProcess.rejected.isEmpty())
        assertEquals(inProcess.records, shared.readAll(1, Cp.L1).records)
        device.restart()
        val persisted = Cp.store(device).readAll(1, Cp.L1)
        assertTrue(persisted.rejected.isEmpty())
        assertEquals(inProcess.records, persisted.records)
        assertTrue(persisted.records.map { it.chapterNumber }.containsAll(listOf(1, 2, 3, 4)))
    }
}
