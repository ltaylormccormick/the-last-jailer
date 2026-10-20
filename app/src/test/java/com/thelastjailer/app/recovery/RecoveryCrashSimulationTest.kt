package com.thelastjailer.app.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The process dies at every single operation of a write, before or after it, and the disk is then resolved in
 * every legal way ([NameOutcome] x [DataOutcome], six images). Whatever is left must satisfy the invariants, and the
 * store must still work afterwards.
 *
 * WHAT THIS PROVES, AND WHAT IT DOES NOT: it proves the store's protocol is safe under the durability model in
 * [SimFs]. It does not prove that Android's filesystem honours fsync, rename atomicity or directory sync the way
 * the model assumes. That is a device-matrix question (G7), and is recorded as an open risk.
 */
class RecoveryCrashSimulationTest {

    private val req = FaultHarness.request

    @Test
    fun `a crash at every operation, resolved every legal way, never loses the only known-good copy`() {
        var scenarios = 0
        for (pre in FaultHarness.pres()) {
            for ((op, nth) in FaultHarness.profile(pre)) for (mode in listOf(FaultMode.CRASH_BEFORE, FaultMode.CRASH_AFTER)) {
                val fs = pre.build()
                val b = FaultHarness.before(fs)
                fs.faults.add(SimFault(op, nth, mode))
                try {
                    Rb.store(fs).put(req)
                    fail("${pre.label} / $op#$nth / $mode: the process should have died")
                } catch (expected: SimulatedCrash) {
                }
                for (names in NameOutcome.values()) for (data in DataOutcome.values()) {
                    val image = fs.crashImage(names, data)
                    val label = "${pre.label} / $mode $op#$nth / $names+$data"
                    FaultHarness.assertInvariants(label, b, image, null, req)
                    FaultHarness.assertStoreStillUsable(label, image)
                    scenarios++
                }
            }
        }
        println("G2 crash sweep: $scenarios crash images")
        assertTrue("the sweep must be substantial, was $scenarios", scenarios > 2000)
    }

    @Test
    fun `the first write to an empty slot is torn at worst, never half-trusted`() {
        // No known-good copy exists yet, so I1 asks nothing; but a torn file must still never classify as VALID.
        val pre = Pre("empty") { Rb.world() }
        for ((op, nth) in FaultHarness.profile(pre)) for (mode in listOf(FaultMode.CRASH_BEFORE, FaultMode.CRASH_AFTER)) {
            val fs = pre.build()
            fs.faults.add(SimFault(op, nth, mode))
            try { Rb.store(fs).put(req) } catch (e: SimulatedCrash) {}
            for (names in NameOutcome.values()) for (data in DataOutcome.values()) {
                val after = Rb.truth(fs.crashImage(names, data))
                for (r in after.reports) {
                    val st = r.status
                    if (st is BufferStatus.Valid) {
                        assertTrue("a VALID file must be exactly the requested bundle", st.bundle.payload.contentEquals(req.payload))
                    }
                }
            }
        }
    }

    /**
     * NEGATIVE CONTROL. A naive writer that overwrites the file in place, with no temp file, fsync or rename, must be
     * caught by this same simulator. If this test ever stops failing the naive writer, the simulator has gone blind
     * and the sweeps above prove nothing.
     */
    @Test
    fun `negative control - the simulator does expose a naive in-place overwrite`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        val b = FaultHarness.before(fs)
        // Naive: truncate-and-rewrite the only good bundle, then die before anything is synced.
        fs.writeFile(java.io.File(Rb.A), RecoveryCodec.encode(
            BundleMeta(1, 6, BundleId(ByteArray(16) { 55 }), 1, 1), req.payload))
        var lostSomewhere = false
        for (names in NameOutcome.values()) for (data in DataOutcome.values()) {
            val after = Rb.truth(fs.crashImage(names, data))
            if (Rb.validCount(after) < Rb.validCount(b.inspection)) lostSomewhere = true
        }
        assertTrue("the simulator must be able to show the only good copy being lost", lostSomewhere)
    }

    @Test
    fun `negative control - the invariant check itself fails when a copy is lost`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        val b = FaultHarness.before(fs)
        fs.seedFile(Rb.A, Rb.corruptBytes())
        try {
            FaultHarness.assertInvariants("control", b, fs, null, req)
            fail("assertInvariants should have rejected a lost sole copy")
        } catch (expected: AssertionError) {
            assertTrue(expected.message!!.contains("I1"))
        }
    }

    @Test
    fun `the rename-persisted-but-data-lost hazard is real in the model and the store survives it`() {
        // Pre: one valid bundle. Crash right after the rename, with names persisted and the new file's data lost.
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        fs.faults.add(SimFault(SimOp.RENAME, 1, FaultMode.CRASH_AFTER))
        try { Rb.store(fs).put(req); fail() } catch (e: SimulatedCrash) {}
        val image = fs.crashImage(NameOutcome.ALL_APPLIED, DataOutcome.SYNCED_ONLY)
        // The data WAS fsynced before the rename, so even this image holds the new bundle intact.
        assertEquals("VALID+VALID", Rb.kinds(Rb.truth(image)))
        // Had the store skipped the fsync, the same image would hold an empty B - covered by the control above.
    }
}
