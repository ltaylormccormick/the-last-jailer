package com.thelastjailer.app.recovery

import java.util.EnumMap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/** A starting disk state for a sweep. */
class Pre(val label: String, val build: () -> SimFs)

/**
 * Shared machinery for the fault-injection and crash sweeps: the starting states, the per-operation profile of a
 * clean write, and the invariant checks that every outcome, however it was reached, must satisfy.
 */
object FaultHarness {

    val request = Rb.request(77, opKind = Rb.OVERWRITE)

    /** Every pair of buffer kinds, plus the cases a bare pair cannot express. */
    fun pres(): List<Pre> {
        val out = ArrayList<Pre>()
        for (a in Kind.values()) for (b in Kind.values()) {
            out.add(Pre("$a+$b") { Rb.world().also { Rb.seed(it, a, b) } })
        }
        out.add(Pre("VALID(3)+VALID(6)") { Rb.world().also { Rb.seed(it, Kind.VALID, Kind.VALID, genA = 3, genB = 6) } })
        out.add(Pre("VALID identical copies") {
            Rb.world().also { val same = Rb.validBytes(5, 1); it.seedFile(Rb.A, same); it.seedFile(Rb.B, same) }
        })
        out.add(Pre("VALID same generation, different content") {
            Rb.world().also { it.seedFile(Rb.A, Rb.validBytes(5, 1, idByte = 11)); it.seedFile(Rb.B, Rb.validBytes(5, 2, idByte = 12)) }
        })
        out.add(Pre("no directories at all") { Rb.world(withDirs = false) })
        out.add(Pre("stale temp files present") {
            Rb.world().also { Rb.seed(it, Kind.VALID, Kind.ABSENT); it.seedFile("${Rb.ROOT}/slot_1/b.tmp", Rb.validBytes(99, 9)) }
        })
        return out
    }

    /** The (operation, nth occurrence) pairs a clean write performs on [pre], in order. */
    fun profile(pre: Pre): List<Pair<SimOp, Int>> {
        val fs = pre.build()
        Rb.store(fs).put(request)
        val seen = EnumMap<SimOp, Int>(SimOp::class.java)
        return fs.opLog.map { op -> val n = (seen[op] ?: 0) + 1; seen[op] = n; op to n }
    }

    /** What the sweep captured about the disk before the operation under test. */
    class Before(val disk: List<ByteArray?>, val inspection: SlotInspection, val names: List<String>) {
        val trueChoice: BufferChoice = BufferSelection.choose(inspection.a, inspection.b)
    }

    fun before(fs: SimFs): Before = Before(Rb.disk(fs), Rb.truth(fs), fs.duplicate().names("${Rb.ROOT}/slot_1"))

    private fun expectedGeneration(b: Before): Long =
        (b.inspection.reports.mapNotNull { (it.status as? BufferStatus.Valid)?.bundle?.meta?.generation }.maxOrNull() ?: 0L) + 1

    /**
     * The invariants every outcome must satisfy, for [fs] as it is now (after a fault, or as a crash image).
     * [result] is null when the process died before the store could answer.
     */
    fun assertInvariants(label: String, b: Before, fs: SimFs, result: PutResult?, req: BundleRequest) {
        val after = Rb.truth(fs)

        // I1: a known-good copy is never lost.
        assertTrue("$label: I1 violated - valid bundles ${Rb.validCount(b.inspection)} -> ${Rb.validCount(after)} (${Rb.kinds(b.inspection)} -> ${Rb.kinds(after)})",
            Rb.validCount(after) >= minOf(1, Rb.validCount(b.inspection)))

        // I2: newer-format files are byte-identical.
        for ((i, buf) in listOf(Buffer.A, Buffer.B).withIndex()) {
            if (b.inspection.report(buf).status is BufferStatus.Future) {
                assertArrayEquals("$label: I2 violated - future file changed", b.disk[i], fs.bytesOf(if (i == 0) Rb.A else Rb.B))
            }
        }

        // I4: only the two bundle files and their two temp files were created or removed.
        val allowed = setOf("a.bundle", "b.bundle", "a.tmp", "b.tmp")
        val after4 = fs.duplicate().names("${Rb.ROOT}/slot_1")
        for (n in b.names.toSet() - after4.toSet()) assertTrue("$label: I4 violated - $n was removed", n in allowed)
        for (n in after4.toSet() - b.names.toSet()) assertTrue("$label: I4 violated - $n appeared", n in allowed)

        // Any VALID bundle on disk is either one that was already there or exactly the one we tried to write.
        val preDigests = b.inspection.reports.mapNotNull { (it.status as? BufferStatus.Valid)?.bundle?.fileDigest }
        for (r in after.reports) {
            val st = r.status as? BufferStatus.Valid ?: continue
            val bundle = st.bundle
            val wasThere = preDigests.any { it.contentEquals(bundle.fileDigest) }
            val isOurs = bundle.payload.contentEquals(req.payload) && bundle.meta.opKind == req.opKind &&
                bundle.meta.payloadSchema == req.payloadSchema && bundle.meta.generation == expectedGeneration(b)
            assertTrue("$label: a VALID bundle appeared that is neither pre-existing nor the requested one", wasThere || isOurs)
        }

        // The result must tell the truth about the disk.
        when (result) {
            null -> Unit
            is PutResult.Stored -> {
                assertTrue("$label: Stored must target the buffer the table allows", b.trueChoice is BufferChoice.Write && (b.trueChoice as BufferChoice.Write).buffer == result.buffer)
                val st = after.report(result.buffer).status
                assertTrue("$label: Stored but the target is not VALID", st is BufferStatus.Valid && st.bundle.meta.bundleId == result.bundleId)
                assertEquals("$label: Stored generation", expectedGeneration(b), result.generation)
                val other = result.buffer.other()
                assertTrue("$label: Stored but the other buffer changed", Rb.sameDisk(
                    listOf(b.disk[if (other == Buffer.A) 0 else 1]), listOf(fs.bytesOf(if (other == Buffer.A) Rb.A else Rb.B))))
            }
            is PutResult.SkippedIdentical, is PutResult.Blocked, is PutResult.Failed -> {
                assertTrue("$label: ${result::class.simpleName} promised an unchanged disk", Rb.sameDisk(b.disk, Rb.disk(fs)))
            }
            is PutResult.Uncertain -> {
                val post = result.post
                if (post != null) for (buf in listOf(Buffer.A, Buffer.B)) {
                    assertTrue("$label: Uncertain.post disagrees with the disk for $buf",
                        post.report(buf).sameDiskStateAs(after.report(buf)) && Rb.statusKind(post.report(buf).status) == Rb.statusKind(after.report(buf).status))
                }
                if (result.newBundleVisible) {
                    assertTrue("$label: newBundleVisible but no VALID new bundle on disk",
                        after.reports.any { (it.status as? BufferStatus.Valid)?.bundle?.payload?.contentEquals(req.payload) == true })
                }
            }
        }
    }

    /** After any fault the store must still work: a second, different write must also satisfy every invariant. */
    fun assertStoreStillUsable(label: String, fs: SimFs) {
        fs.clearFaults()
        val b = before(fs)
        val req2 = Rb.request(88, opKind = Rb.RESTORE_SWAP)
        val result = try { Rb.store(fs).put(req2) } catch (e: Exception) { fail("$label: follow-up put threw $e"); return }
        assertInvariants("$label / follow-up", b, fs, result, req2)
    }
}
