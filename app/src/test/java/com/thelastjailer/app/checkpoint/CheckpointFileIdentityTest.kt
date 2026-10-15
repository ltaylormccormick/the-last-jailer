package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The lock and the "untrusted" flag belong to the physical file, identified by (memory scope, file
 * name). A wrapper, a decorator, or a brand-new `SharedPreferences` object per call over the same file
 * must therefore be unable to dodge either protection.
 */
class CheckpointFileIdentityTest {
    private lateinit var device: SimulatedDevice
    private val file = Cp.file(1)
    private val old = Cp.records(4)

    @Before
    fun setUp() {
        device = SimulatedDevice()
        assertTrue(Cp.store(device).replaceAll(1, Cp.L1, old).isCommitted)
    }

    private fun wrap(base: SharedPreferences): SharedPreferences = object : SharedPreferences by base {}

    private fun poison(through: CheckpointStore) {
        device.armFaultAlways(file, CommitFault.FALSE_MEMORY_MUTATED)
        assertTrue(through.replaceAll(1, Cp.L1, Cp.records(6)) is StoreWriteResult.StateUnknown)
        device.clearFaults()
    }

    // ---- untrusted ---------------------------------------------------------------------------

    @Test
    fun `a store that builds a new wrapper on every call still shares the flag with other stores`() {
        val freshWrapperEachCall = Cp.storeOver(device) { slot -> wrap(device.prefs(Cp.file(slot))) }
        val otherFreshWrapperEachCall = Cp.storeOver(device) { slot -> wrap(device.prefs(Cp.file(slot))) }
        poison(freshWrapperEachCall)
        assertFalse(freshWrapperEachCall.isTrusted(1))
        assertFalse(otherFreshWrapperEachCall.isTrusted(1))
        assertTrue(otherFreshWrapperEachCall.upsert(1, Cp.L1, Cp.record(5)) is StoreWriteResult.Rejected)
    }

    @Test
    fun `two stores over distinct wrapper objects of one file share the flag`() {
        val w1 = wrap(device.prefs(file))
        val w2 = wrap(device.prefs(file))
        val s1 = Cp.storeOver(device) { w1 }
        val s2 = Cp.storeOver(device) { w2 }
        poison(s1)
        assertFalse(s2.isTrusted(1))
        assertTrue(s2.readAll(1, Cp.L1).memoryMayDiffer)
    }

    @Test
    fun `a plain store and a wrapped store over one file share the flag in both directions`() {
        val plain = Cp.store(device)
        val wrapped = Cp.storeOver(device) { wrap(device.prefs(file)) }
        poison(wrapped)
        assertFalse(plain.isTrusted(1))
        assertTrue(plain.upsert(1, Cp.L1, Cp.record(5)) is StoreWriteResult.Rejected)
    }

    @Test
    fun `different file names never share it`() {
        val staging = Cp.stageStore(device)
        poison(Cp.store(device))
        assertTrue(staging.isTrusted(1))
        assertTrue(Cp.store(device).isTrusted(2))
    }

    @Test
    fun `a new memory scope, which is a new process, starts trusted even for the same file name`() {
        val store = Cp.store(device)
        poison(store)
        device.restart()
        assertTrue(store.isTrusted(1))                  // the same store object, looking the file up in the new process
        assertEquals(old, store.readAll(1, Cp.L1).records)
        assertTrue(store.upsert(1, Cp.L1, Cp.record(5)).isCommitted)
    }

    @Test
    fun `two devices never share it even though the file names are identical`() {
        val other = SimulatedDevice()
        poison(Cp.store(device))
        assertTrue(Cp.store(other).isTrusted(1))
    }

    // ---- lock --------------------------------------------------------------------------------

    @Test
    fun `a writer behind a different wrapper of the same file waits for the one mid-commit`() {
        val gate = CommitGate()
        val gatedWrapper = gatedPrefs(device.prefs(file), pauseOnCommit = 1, gate = gate)
        val otherWrapper = wrap(device.prefs(file))
        val a = Cp.storeOver(device) { gatedWrapper }
        val b = Cp.storeOver(device) { otherWrapper }

        val doneB = java.util.concurrent.CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        val threadA = Thread { try { a.upsert(1, Cp.L1, Cp.record(5)) } catch (t: Throwable) { error.set(t) } }.apply { isDaemon = true }
        threadA.start()
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS))
        val commitsWhilePaused = device.commitCount(file)

        val threadB = Thread {
            try { b.upsert(1, Cp.L1, Cp.record(6)) } catch (t: Throwable) { error.set(t) } finally { doneB.countDown() }
        }.apply { isDaemon = true }
        threadB.start()
        assertFalse("B must wait even though it holds a different wrapper", doneB.await(300, TimeUnit.MILLISECONDS))
        assertEquals(commitsWhilePaused, device.commitCount(file))

        gate.release.countDown()
        threadA.join(20_000); threadB.join(20_000)
        assertEquals(null, error.get())
        assertEquals(listOf(1, 2, 3, 4, 5, 6), Cp.store(device).readAll(1, Cp.L1).records.map { it.chapterNumber })
    }

    // ---- the production implementation of the storage contract -------------------------------

    @Test
    fun `PrefsCheckpointStorage shares by scope and file name whatever objects it opens`() {
        val scope = Any()
        fun storage(s: Any) = PrefsCheckpointStorage(s) { name -> wrap(device.prefs(name)) }   // new wrapper per open
        val one = CheckpointStore(storage(scope), Cp.rules)
        val two = CheckpointStore(storage(scope), Cp.rules)
        val elsewhere = CheckpointStore(storage(Any()), Cp.rules)
        poison(one)
        assertFalse(one.isTrusted(1))
        assertFalse(two.isTrusted(1))
        assertTrue(elsewhere.isTrusted(1))
    }

    @Test
    fun `PrefsCheckpointStorage defaults to the live checkpoint files and can be pointed at the staging files`() {
        val scope = Any()
        val live = PrefsCheckpointStorage(scope) { device.prefs(it) }
        val staging = PrefsCheckpointStorage(scope, CheckpointFiles::staging) { device.prefs(it) }
        assertEquals("jailer_ckpt_3", live.fileName(3))
        assertEquals("jailer_ckpt_stage_3", staging.fileName(3))
    }

    @Test
    fun `checkpoint file names can never collide with the ordinary save or entitlement files or each other`() {
        val names = (1..10).flatMap { listOf(CheckpointFiles.live(it), CheckpointFiles.staging(it)) }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.none { it == "jailer_saves" || it == "jailer_entitlements" })
        assertNotEquals(CheckpointFiles.live(1), CheckpointFiles.staging(1))
    }
}
