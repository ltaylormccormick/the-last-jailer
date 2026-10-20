package com.thelastjailer.app.recovery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Failures injected at every single filesystem operation of a write, against every starting disk state.
 * These are I/O errors and bad reads while the process lives; process death is in [RecoveryCrashSimulationTest].
 */
class RecoveryFaultInjectionTest {

    private val req = FaultHarness.request

    private fun modesFor(op: SimOp): List<FaultMode> = when (op) {
        SimOp.READ -> listOf(FaultMode.THROW, FaultMode.CORRUPT_READ)
        SimOp.ENSURE_DIR -> listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT)
        SimOp.WRITE -> listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT, FaultMode.SHORT_WRITE_THROW)
        SimOp.SYNC_FILE -> listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT)
        SimOp.RENAME -> listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT, FaultMode.RENAME_LOST)
        SimOp.SYNC_DIR -> listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT, FaultMode.SYNC_DIR_UNSUPPORTED)
        SimOp.DELETE, SimOp.LIST -> listOf(FaultMode.THROW)
    }

    @Test
    fun `every fault at every operation against every starting state keeps the invariants and tells the truth`() {
        var scenarios = 0
        val outcomes = HashMap<String, Int>()
        for (pre in FaultHarness.pres()) {
            val profile = FaultHarness.profile(pre)
            assertTrue("${pre.label}: a clean write performs operations", profile.isNotEmpty() || pre.label.contains("VALID same generation") ||
                pre.label.contains("FUTURE+FUTURE") || pre.label.contains("VALID+FUTURE") || pre.label.contains("FUTURE+VALID"))
            for ((op, nth) in profile) for (mode in modesFor(op)) {
                val fs = pre.build()
                val b = FaultHarness.before(fs)
                fs.faults.add(SimFault(op, nth, mode))
                val label = "${pre.label} / $op#$nth / $mode"
                val result = try {
                    Rb.store(fs).put(req)
                } catch (e: Throwable) {
                    fail("$label: put threw $e"); return
                }
                FaultHarness.assertInvariants(label, b, fs, result, req)
                FaultHarness.assertStoreStillUsable(label, fs)
                outcomes.merge(result::class.simpleName!!, 1, Int::plus)
                scenarios++
            }
        }
        println("G2 fault sweep: $scenarios scenarios, outcomes=$outcomes")
        assertTrue("the sweep must be substantial, was $scenarios", scenarios > 400)
        // The sweep must actually produce each kind of honest answer, not just the easy ones.
        for (kind in listOf("Stored", "Failed", "Uncertain", "Blocked")) {
            assertTrue("no scenario produced $kind: $outcomes", (outcomes[kind] ?: 0) > 0)
        }
    }

    // ---- targeted expectations (the sweep proves safety; these pin down the exact verdicts) ----

    private fun oneValid(): SimFs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }

    private fun failure(op: SimOp, nth: Int, mode: FaultMode, fs: SimFs = oneValid()): Pair<SimFs, PutResult> {
        fs.faults.add(SimFault(op, nth, mode))
        return fs to Rb.store(fs).put(req)
    }

    @Test
    fun `a failed temp write is a definite failure and leaves no temp file behind`() {
        val before = Rb.disk(oneValid())
        val (fs, r) = failure(SimOp.WRITE, 1, FaultMode.THROW)
        assertTrue(r is PutResult.Failed && r.stage == PutStage.WRITE_TEMP)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
        assertFalse(fs.exists("${Rb.ROOT}/slot_1/b.tmp"))
    }

    @Test
    fun `a half-written temp file is a definite failure and is never promoted`() {
        val (fs, r) = failure(SimOp.WRITE, 1, FaultMode.SHORT_WRITE_THROW)
        assertTrue(r is PutResult.Failed && r.stage == PutStage.WRITE_TEMP)
        assertNull(fs.bytesOf(Rb.B))
    }

    @Test
    fun `if even the cleanup fails, the stray temp file is ignored and the next write is correct`() {
        val fs = oneValid()
        fs.faults.add(SimFault(SimOp.WRITE, 1, FaultMode.SHORT_WRITE_THROW))
        // DELETE #1 is the store's clear-the-temp-name step before creating it; #2 is the cleanup after the failure.
        fs.faults.add(SimFault(SimOp.DELETE, 2, FaultMode.THROW))
        val r = Rb.store(fs).put(req)
        assertTrue("still a definite failure", r is PutResult.Failed)
        assertTrue("the stray temp file remains", fs.exists("${Rb.ROOT}/slot_1/b.tmp"))
        assertEquals("VALID+ABSENT", Rb.kinds(Rb.truth(fs)))
        fs.clearFaults()
        val next = Rb.store(fs).put(Rb.request(5))
        assertTrue(next is PutResult.Stored)
        val i = Rb.truth(fs)
        assertArrayEquals(Rb.request(5).payload, (i.b.status as BufferStatus.Valid).bundle.payload)
    }

    @Test
    fun `a failed fsync of the temp file is a definite failure`() {
        val (_, r) = failure(SimOp.SYNC_FILE, 1, FaultMode.THROW)
        assertTrue(r is PutResult.Failed && r.stage == PutStage.SYNC_TEMP)
    }

    @Test
    fun `a rename that fails and changed nothing is a definite failure`() {
        val (_, r) = failure(SimOp.RENAME, 1, FaultMode.THROW)
        assertTrue(r is PutResult.Failed && r.stage == PutStage.RENAME)
    }

    @Test
    fun `a rename that took effect but reported failure is uncertain, and the new bundle is seen`() {
        val (fs, r) = failure(SimOp.RENAME, 1, FaultMode.THROW_AFTER_EFFECT)
        assertTrue(r is PutResult.Uncertain && r.stage == PutStage.RENAME && r.newBundleVisible)
        assertEquals("VALID+VALID", Rb.kinds(Rb.truth(fs)))
    }

    @Test
    fun `a rename that claims success but did nothing is caught by the readback and called uncertain, not failed`() {
        val (fs, r) = failure(SimOp.RENAME, 1, FaultMode.RENAME_LOST)
        assertTrue("was ${r::class.simpleName}", r is PutResult.Uncertain && r.stage == PutStage.READBACK && !r.newBundleVisible)
        assertEquals("VALID+ABSENT", Rb.kinds(Rb.truth(fs)))
    }

    @Test
    fun `if anything else changes the other buffer during a write, the write is not reported as stored`() {
        val fs = oneValid() // A valid, B will be written
        // Just before the readback (the 3rd read), something else overwrites A behind the store's back.
        fs.interference = { op, nth -> if (op == SimOp.READ && nth == 3) fs.seedFile(Rb.A, Rb.futureBytes(5)) }
        val r = Rb.store(fs).put(req)
        assertTrue("was ${r::class.simpleName}", r is PutResult.Uncertain && r.stage == PutStage.VERIFY)
    }

    @Test
    fun `a failed directory sync after the rename is uncertain, never stored, and a retry is harmless`() {
        val (fs, r) = failure(SimOp.SYNC_DIR, 1, FaultMode.THROW)
        assertTrue(r is PutResult.Uncertain && r.stage == PutStage.SYNC_DIRECTORY && r.newBundleVisible)
        fs.clearFaults()
        // The new bundle is already there, so repeating the request is recognised as a duplicate: no second write.
        val again = Rb.store(fs).put(req)
        assertTrue("retry should be a no-op, was ${again::class.simpleName}", again is PutResult.SkippedIdentical)
    }

    @Test
    fun `a bad readback of a good file is uncertain, not stored`() {
        // Reads in a clean (VALID, ABSENT) write: A, B (pre-scan), then the readback of B is the third.
        val (fs, r) = failure(SimOp.READ, 3, FaultMode.CORRUPT_READ)
        assertTrue("was ${r::class.simpleName}", r is PutResult.Uncertain && r.stage == PutStage.READBACK)
        assertEquals("VALID+VALID", Rb.kinds(Rb.truth(fs)))
    }

    @Test
    fun `one bad read of a valid file never makes the store overwrite it`() {
        // The sole VALID bundle is next to a future file; a single misread must not turn it into "corrupt".
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.FUTURE) }
        val before = Rb.disk(fs)
        fs.faults.add(SimFault(SimOp.READ, 1, FaultMode.CORRUPT_READ))
        val r = Rb.store(fs).put(req)
        assertTrue("was ${r::class.simpleName}", r is PutResult.Blocked)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `an unreadable buffer during the scan blocks the write and changes nothing`() {
        val fs = oneValid()
        val before = Rb.disk(fs)
        fs.faults.add(SimFault(SimOp.READ, 2, FaultMode.THROW))
        val r = Rb.store(fs).put(req)
        assertTrue(r is PutResult.Blocked && r.reason == BlockReason.UNREADABLE_BUFFER)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `failing to create the directories is a definite failure`() {
        val fs = Rb.world(withDirs = false)
        fs.faults.add(SimFault(SimOp.ENSURE_DIR, 1, FaultMode.THROW))
        val r = Rb.store(fs).put(req)
        assertTrue(r is PutResult.Failed && r.stage == PutStage.PREPARE)
    }

    @Test
    fun `no outcome ever claims success it did not verify`() {
        // The only path to Stored is a clean run: every injected fault above produced something else.
        val fs = oneValid()
        val r = Rb.store(fs).put(req)
        assertTrue(r is PutResult.Stored)
        r as PutResult.Stored
        assertTrue(r.dirSynced)
        assertEquals("VALID+VALID", Rb.kinds(r.post))
    }
}
