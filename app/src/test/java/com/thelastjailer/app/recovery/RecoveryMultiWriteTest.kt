package com.thelastjailer.app.recovery

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * F3: the single-write sweeps start every scenario from a durable, quiet disk. Real use is a SEQUENCE: a write whose
 * outcome was uncertain, then another write on whatever that left behind. These tests chain writes and crashes and
 * check the invariants over the whole chain, plus the unexpected-object and lock-identity cases on the simulator.
 *
 * Same boundary as every simulator test: this proves the store's logic under the [SimFs] durability model, not
 * that a phone's filesystem behaves like it.
 */
class RecoveryMultiWriteTest {

    private val req1 = FaultHarness.request
    private val req2 = Rb.request(88, opKind = Rb.RESTORE_SWAP)
    private val req3 = Rb.request(99, opKind = Rb.OVERWRITE)

    private fun validGenerations(fs: SimFs): List<Long> =
        Rb.truth(fs).reports.mapNotNull { (it.status as? BufferStatus.Valid)?.bundle?.meta?.generation }

    /** Properties of a whole chain, measured against the disk as it was before the chain began. */
    private fun assertChain(label: String, start: FaultHarness.Before, fs: SimFs) {
        val now = Rb.truth(fs)
        assertTrue("$label: chain I1 - a known-good copy was lost (${Rb.kinds(start.inspection)} -> ${Rb.kinds(now)})",
            Rb.validCount(now) >= minOf(1, Rb.validCount(start.inspection)))
        val startMax = start.inspection.reports.mapNotNull { (it.status as? BufferStatus.Valid)?.bundle?.meta?.generation }.maxOrNull()
        val nowMax = validGenerations(fs).maxOrNull()
        if (startMax != null) assertTrue("$label: chain - the newest generation went backwards ($startMax -> $nowMax)", nowMax != null && nowMax >= startMax)
        for ((i, buf) in listOf(Buffer.A, Buffer.B).withIndex()) {
            if (start.inspection.report(buf).status is BufferStatus.Future) {
                assertArrayEquals("$label: chain I2 - a future file changed", start.disk[i], fs.bytesOf(if (i == 0) Rb.A else Rb.B))
            }
        }
    }

    private val uncertainModes = listOf(FaultMode.THROW, FaultMode.THROW_AFTER_EFFECT)
    private val reducedImages = listOf(
        NameOutcome.AS_SYNCED to DataOutcome.SYNCED_ONLY,
        NameOutcome.ALL_APPLIED to DataOutcome.PARTIAL,
        NameOutcome.ALL_APPLIED to DataOutcome.ALL_PERSISTED
    )

    @Test
    fun `a faulted write followed by a different write keeps every invariant at every operation`() {
        var chains = 0
        for (pre in FaultHarness.pres()) for ((op, nth) in FaultHarness.profile(pre)) for (mode in uncertainModes) {
            val fs = pre.build()
            val start = FaultHarness.before(fs)
            fs.faults.add(SimFault(op, nth, mode))
            val r1 = Rb.store(fs).put(req1)
            fs.clearFaults()
            val mid = FaultHarness.before(fs)
            val label = "${pre.label} / put1 $mode $op#$nth (${r1::class.simpleName})"
            val r2 = Rb.store(fs).put(req2)
            FaultHarness.assertInvariants("$label / put2", mid, fs, r2, req2)
            assertChain(label, start, fs)
            chains++
        }
        println("G2 multi-write (fault, then write): $chains chains")
        assertTrue("sweep must be substantial, was $chains", chains > 300)
    }

    @Test
    fun `retrying an uncertain write is safe - it lands exactly once`() {
        var retried = 0
        for (pre in FaultHarness.pres()) for ((op, nth) in FaultHarness.profile(pre)) for (mode in uncertainModes) {
            val fs = pre.build()
            fs.faults.add(SimFault(op, nth, mode))
            val r1 = Rb.store(fs).put(req1)
            if (r1 !is PutResult.Uncertain) continue
            fs.clearFaults()
            val mid = FaultHarness.before(fs)
            val label = "${pre.label} / put1 $mode $op#$nth"
            val r2 = Rb.store(fs).put(req1)
            FaultHarness.assertInvariants("$label / retry", mid, fs, r2, req1)
            val copies = Rb.truth(fs).reports.count { (it.status as? BufferStatus.Valid)?.bundle?.payload?.contentEquals(req1.payload) == true }
            val blocked = r2 is PutResult.Blocked
            assertTrue("$label: the retried bundle must exist at most once, found $copies (${r2::class.simpleName})", copies <= 1)
            if (!blocked) assertTrue("$label: a retry that was not blocked must leave the bundle present", copies == 1)
            retried++
        }
        println("G2 multi-write (uncertain, then retry): $retried retries")
        assertTrue("some outcomes must have been uncertain, was $retried", retried > 20)
    }

    @Test
    fun `a crash during one write then a faulted second write then a clean third keeps every invariant`() {
        var chains = 0
        for (pre in FaultHarness.pres()) for ((op, nth) in FaultHarness.profile(pre)) for (crash in listOf(FaultMode.CRASH_BEFORE, FaultMode.CRASH_AFTER)) {
            val fs = pre.build()
            val start = FaultHarness.before(fs)
            fs.faults.add(SimFault(op, nth, crash))
            try { Rb.store(fs).put(req1); fail("${pre.label}: should have died") } catch (e: SimulatedCrash) {}
            for ((names, data) in reducedImages) {
                val image = fs.crashImage(names, data)
                // Second write on the post-crash disk, itself faulted at every one of its operations.
                for ((op2, nth2) in FaultHarness.profile(Pre("image") { image.duplicate() })) for (mode2 in uncertainModes) {
                    val fs2 = image.duplicate()
                    fs2.faults.add(SimFault(op2, nth2, mode2))
                    Rb.store(fs2).put(req2)
                    fs2.clearFaults()
                    val mid = FaultHarness.before(fs2)
                    val label = "${pre.label} / crash $crash $op#$nth / $names+$data / put2 $mode2 $op2#$nth2"
                    val r3 = Rb.store(fs2).put(req3)
                    FaultHarness.assertInvariants("$label / put3", mid, fs2, r3, req3)
                    assertChain(label, start, fs2)
                    chains++
                }
            }
        }
        println("G2 multi-write (crash, faulted write, write): $chains chains")
        assertTrue("sweep must be substantial, was $chains", chains > 3000)
    }

    @Test
    fun `two crashes in a row, each resolved adversarially, still leave a known-good copy`() {
        var chains = 0
        for (pre in FaultHarness.pres()) for ((op, nth) in FaultHarness.profile(pre)) {
            val fs = pre.build()
            val start = FaultHarness.before(fs)
            fs.faults.add(SimFault(op, nth, FaultMode.CRASH_AFTER))
            try { Rb.store(fs).put(req1) } catch (e: SimulatedCrash) {}
            for ((n1, d1) in reducedImages) {
                val image = fs.crashImage(n1, d1)
                for ((op2, nth2) in FaultHarness.profile(Pre("image") { image.duplicate() })) {
                    val fs2 = image.duplicate()
                    fs2.faults.add(SimFault(op2, nth2, FaultMode.CRASH_AFTER))
                    try { Rb.store(fs2).put(req2) } catch (e: SimulatedCrash) {}
                    for ((n2, d2) in reducedImages) {
                        val image2 = fs2.crashImage(n2, d2)
                        assertChain("${pre.label} / $op#$nth $n1+$d1 / $op2#$nth2 $n2+$d2", start, image2)
                        chains++
                    }
                }
            }
        }
        println("G2 multi-write (double crash): $chains images")
        assertTrue("sweep must be substantial, was $chains", chains > 3000)
    }

    // ---------------------------------------------------------------- unexpected objects (simulator)

    @Test
    fun `a symlink planted at the temp name is replaced, never written through`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        fs.seedSymlink("${Rb.ROOT}/slot_1/b.tmp")
        val r = Rb.store(fs).put(req1)
        assertTrue("$r", r is PutResult.Stored)
        assertFalse(fs.isLinkAt("${Rb.ROOT}/slot_1/b.tmp"))
        assertFalse("a link must never become a bundle", fs.isLinkAt(Rb.B))
    }

    @Test
    fun `a directory squatting on the temp name fails closed and the store recovers once it is gone`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        val tmpName = "${Rb.ROOT}/slot_1/b.tmp"
        fs.seedDir(tmpName)
        val before = Rb.disk(fs)
        // The simulator's delete removes any node; model a delete that cannot remove a (non-empty) directory, both
        // for the store's clear-the-name step (#1) and its cleanup after the failure (#2).
        fs.faults.add(SimFault(SimOp.DELETE, 1, FaultMode.THROW))
        fs.faults.add(SimFault(SimOp.DELETE, 2, FaultMode.THROW))
        val r = Rb.store(fs).put(req1)
        assertTrue("$r", r is PutResult.Failed && r.stage == PutStage.WRITE_TEMP)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
        assertTrue("the squatting directory is left alone", fs.isDirAt(tmpName))
        fs.clearFaults()
        // Once the owner has cleared the name (here: the simulated delete simply works again), the store carries on.
        assertTrue(Rb.store(fs).put(req1) is PutResult.Stored)
    }

    @Test
    fun `a symlink where a bundle should be is UNREADABLE and the write is blocked`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        fs.seedSymlink(Rb.B)
        val store = Rb.store(fs)
        assertEquals("VALID+UNREADABLE", Rb.kinds(store.inspect(1)))
        val r = store.put(req1)
        assertTrue("$r", r is PutResult.Blocked && r.reason == BlockReason.UNREADABLE_BUFFER)
        assertTrue(fs.isLinkAt(Rb.B))
    }

    @Test
    fun `a missing parent of the root is not created on the simulator either`() {
        val fs = Rb.world(withDirs = false)
        val store = RecoveryBundleStore(File("/nowhere/deep/recovery"), fs)
        val r = store.put(req1)
        assertTrue("$r", r is PutResult.Failed && r.stage == PutStage.PREPARE)
        assertFalse(fs.exists("/nowhere"))
        assertFalse(fs.exists("/nowhere/deep"))
    }

    @Test
    fun `the root and the slot directory are the only directories ever created`() {
        val fs = Rb.world(withDirs = false)
        assertTrue(Rb.store(fs).put(req1) is PutResult.Stored)
        val dirs = listOf("/", "/sim", Rb.ROOT, "${Rb.ROOT}/slot_1")
        for (d in dirs) assertTrue("$d should exist", fs.isDirAt(d))
        assertEquals(listOf("a.bundle"), fs.names("${Rb.ROOT}/slot_1"))
        assertEquals(listOf("recovery"), fs.names("/sim"))
    }

    // ---------------------------------------------------------------- lock identity (simulator)

    @Test
    fun `a store reaching the directory through an alias waits for the writer holding the lock`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT) }
        fs.addAlias("/alias", "/sim")
        val a = Rb.store(fs)
        val b = RecoveryBundleStore(File("/alias/recovery"), fs)
        val inside = CountDownLatch(1)
        val release = CountDownLatch(1)
        val bFinished = CountDownLatch(1)
        var bResult: PutResult? = null
        // Store A is parked in the middle of its write, holding the slot lock, while B (via the alias) tries to write.
        fs.interference = { op, n ->
            if (op == SimOp.SYNC_FILE && n == 1) {
                inside.countDown()
                release.await(10, TimeUnit.SECONDS)
            }
        }
        val ta = Thread { a.put(req1) }
        ta.start()
        assertTrue(inside.await(10, TimeUnit.SECONDS))
        fs.interference = null
        val tb = Thread { bResult = b.put(req2); bFinished.countDown() }
        tb.start()
        assertFalse("B must wait for A's lock; it finished while A still held it", bFinished.await(400, TimeUnit.MILLISECONDS))
        release.countDown()
        ta.join(10_000); tb.join(10_000)
        assertTrue(bFinished.await(10, TimeUnit.SECONDS))
        val gens = validGenerations(fs).sorted()
        assertEquals("both writes stored in turn: generations 5 (seeded), 6, 7 kept as the two newest", listOf(6L, 7L), gens)
        assertTrue(bResult is PutResult.Stored)
    }

    @Test
    fun `stores over genuinely different directories do not block each other`() {
        val fs = Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT); it.seedDir("/sim/other"); it.seedDir("/sim/other/slot_1") }
        val a = Rb.store(fs)
        val other = RecoveryBundleStore(File("/sim/other"), fs)
        val inside = CountDownLatch(1)
        val release = CountDownLatch(1)
        fs.interference = { op, n -> if (op == SimOp.SYNC_FILE && n == 1) { inside.countDown(); release.await(10, TimeUnit.SECONDS) } }
        val ta = Thread { a.put(req1) }
        ta.start()
        assertTrue(inside.await(10, TimeUnit.SECONDS))
        fs.interference = null
        val done = CountDownLatch(1)
        val tb = Thread { other.put(req2); done.countDown() }
        tb.start()
        assertTrue("an unrelated directory must not wait", done.await(10, TimeUnit.SECONDS))
        release.countDown()
        ta.join(10_000)
    }
}
