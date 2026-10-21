package com.thelastjailer.app.saveguard.quarantine

import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.security.MessageDigest
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuarantineStoreTest {

    private val root = File("/data/q")
    private val meta = SnapshotMeta("1.2.3", 34, 0, "SUSPECT_FILE", listOf(1, 2), "2026-09-13T10:00:00Z")

    private fun fs(): SimQuarantineFs = SimQuarantineFs().also { it.mkdirs("/data") }
    private fun store(fs: SimQuarantineFs, limits: QuarantineLimits = QuarantineLimits()) = QuarantineStore(fs, root, limits)
    private fun src(main: String?, bak: String? = null) = SourceFiles(main?.toByteArray(), bak?.toByteArray())
    private fun sha(b: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    /** Independent re-implementation of "this directory is a complete, verified snapshot" (does not use the store). */
    private fun isComplete(fs: SimQuarantineFs, name: String): Boolean {
        val dir = "/data/q/$name"
        val manifest = fs.bytesOf("$dir/MANIFEST") ?: return false
        val marker = fs.bytesOf("$dir/COMPLETE") ?: return false
        if (String(marker) != "tljq1 ${sha(manifest)} ${manifest.size}\n") return false
        val m = Manifest.parse(manifest) ?: return false
        val present = fs.children(dir).filter { it == "main.xml" || it == "bak.xml" }.toSet()
        if (present != m.files.map { it.name }.toSet()) return false
        return m.files.all { f -> fs.bytesOf("$dir/${f.name}")?.let { it.size.toLong() == f.length && sha(it) == f.sha256 } == true }
    }

    private fun snapNames(fs: SimQuarantineFs) = fs.children("/data/q").filter { Regex("snap-[0-9]{4,}").matches(it) }
    private fun completeNames(fs: SimQuarantineFs) = snapNames(fs).filter { isComplete(fs, it) }
    private fun incompleteNames(fs: SimQuarantineFs) = snapNames(fs).filter { !isComplete(fs, it) && fs.children("/data/q/$it").isNotEmpty() }

    private fun assertSecured(r: SecureResult, how: SecureHow? = null): SecureResult.Secured {
        assertTrue("expected Secured but was ${describe(r)}", r is SecureResult.Secured)
        if (how != null) assertEquals(how, (r as SecureResult.Secured).how)
        return r as SecureResult.Secured
    }

    private fun describe(r: SecureResult) = when (r) {
        is SecureResult.Secured -> "Secured(${r.how})"
        is SecureResult.Refused -> "Refused(${r.reason})"
        is SecureResult.Failed -> "Failed(${r.stage}, ${r.cause.message})"
    }

    private fun assertRefused(r: SecureResult, reason: RefuseReason) {
        assertTrue("expected Refused($reason) but was ${describe(r)}", r is SecureResult.Refused && r.reason == reason)
    }

    // ---------------------------------------------------------------- happy path

    @Test fun `a new snapshot is created, verified, marked complete and contains the exact bytes`() {
        val f = fs()
        val bytes = byteArrayOf(0, 1, 2, 3, -1, -128, 127, 60)
        val r = assertSecured(store(f).secure(SourceFiles(bytes, "bak".toByteArray()), meta), SecureHow.CREATED)
        assertEquals("snap-0001", r.name)
        assertTrue(isComplete(f, "snap-0001"))
        assertArrayEquals(bytes, f.bytesOf("/data/q/snap-0001/main.xml"))
        assertArrayEquals("bak".toByteArray(), f.bytesOf("/data/q/snap-0001/bak.xml"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `the root is created when missing and a bak only source works`() {
        val f = fs()
        assertSecured(store(f).secure(SourceFiles(null, "only-bak".toByteArray()), meta))
        assertNull(f.bytesOf("/data/q/snap-0001/main.xml"))
        assertTrue(isComplete(f, "snap-0001"))
    }

    @Test fun `the manifest carries codes numbers and hashes only`() {
        val f = fs()
        val secret = "SECRET-SCENE-ID-xyz"
        assertSecured(store(f).secure(src("<map><string name=\"1.scene\">$secret</string></map>"), meta))
        val manifest = String(f.bytesOf("/data/q/snap-0001/MANIFEST")!!)
        assertFalse(manifest.contains(secret))
        assertFalse(manifest.contains("scene"))
        assertTrue(manifest.contains("reason SUSPECT_FILE"))
        assertTrue(manifest.contains("slots 1,2"))
    }

    @Test fun `metadata tokens that are not plain are replaced rather than written`() {
        val f = fs()
        val bad = SnapshotMeta("1.0 \nreason hacked", 1, 0, "x y", emptyList(), "now\n")
        assertSecured(store(f).secure(src("a"), bad))
        val manifest = String(f.bytesOf("/data/q/snap-0001/MANIFEST")!!)
        assertFalse(manifest.contains("hacked"))
        assertNotNull(Manifest.parse(manifest.toByteArray()))
    }

    @Test fun `empty and oversized sources are refused before any filesystem change`() {
        val f = fs()
        assertRefused(store(f).secure(SourceFiles(null, null), meta), RefuseReason.EMPTY_SOURCE)
        assertRefused(store(f).secure(SourceFiles(ByteArray((1 shl 20) + 1), null), meta), RefuseReason.TOO_LARGE)
        assertEquals(0, f.opCount)
        assertEquals(null, f.nodes["/data/q"])
    }

    @Test fun `a zero length source is a legitimate thing to keep`() {
        val f = fs()
        assertSecured(store(f).secure(SourceFiles(ByteArray(0), null), meta))
        assertTrue(isComplete(f, "snap-0001"))
    }

    // ---------------------------------------------------------------- dedupe and caps

    @Test fun `repeated launches with the same bytes keep exactly one snapshot`() {
        val f = fs()
        assertSecured(store(f).secure(src("damaged"), meta), SecureHow.CREATED)
        repeat(25) { assertSecured(store(f).secure(src("damaged"), meta), SecureHow.REUSED) }
        assertEquals(listOf("snap-0001"), snapNames(f))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `main and bak are deduplicated as a pair`() {
        val f = fs()
        assertSecured(store(f).secure(src("m", "b"), meta), SecureHow.CREATED)
        assertSecured(store(f).secure(src("m", "b"), meta), SecureHow.REUSED)
        assertSecured(store(f).secure(src("m", "b2"), meta), SecureHow.CREATED)
        assertSecured(store(f).secure(src("m", null), meta), SecureHow.CREATED)
        assertEquals(3, completeNames(f).size)
    }

    @Test fun `the complete cap is global and refuses a new distinct file without deleting anything`() {
        val f = fs()
        for (i in 1..3) assertSecured(store(f).secure(src("v$i"), meta))
        assertRefused(store(f).secure(src("v4"), meta), RefuseReason.CAP_FULL)
        assertRefused(store(f).secure(src("v5"), meta), RefuseReason.CAP_FULL)
        assertSecured(store(f).secure(src("v2"), meta), SecureHow.REUSED) // known content still resolves
        assertEquals(3, completeNames(f).size)
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `the incomplete cap stops further attempts and nothing is deleted`() {
        val f = fs()
        f.mkdirs("/data/q")
        for (i in 1..3) { f.mkdirs("/data/q/snap-000$i"); f.putFile("/data/q/snap-000$i/main.xml", "partial-$i".toByteArray()) }
        assertRefused(store(f).secure(src("brand-new"), meta), RefuseReason.ATTEMPTS_EXHAUSTED)
        assertEquals(3, incompleteNames(f).size)
        assertEquals(emptyList<String>(), f.deleted)
        assertEquals(3, snapNames(f).size)
    }

    @Test fun `empty snapshot directories are removed and do not count`() {
        val f = fs()
        f.mkdirs("/data/q")
        for (i in 1..5) f.mkdirs("/data/q/snap-000$i")
        assertSecured(store(f).secure(src("x"), meta), SecureHow.CREATED)
        assertEquals(1, snapNames(f).size)
        assertEquals(1, completeNames(f).size)
        assertTrue(f.deleted.all { it.startsWith("/data/q/snap-") && f.children(it).isEmpty() })
        assertEquals(5, f.deleted.size)
    }

    @Test fun `byte budget refuses when the measured quarantine plus the new snapshot would exceed it`() {
        val f = fs()
        val limits = QuarantineLimits(budgetBytes = 40L * 1024)
        val big = "x".repeat(10_000)
        assertSecured(store(f, limits).secure(src(big + "1"), meta))
        var refused = false
        for (i in 2..6) {
            val r = store(f, limits).secure(src(big + i), meta)
            if (r is SecureResult.Refused) { assertEquals(RefuseReason.BUDGET_FULL, r.reason); refused = true; break }
        }
        assertTrue(refused)
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `budget counts block rounded sizes including foreign files, not just payload bytes`() {
        val f = fs()
        f.mkdirs("/data/q")
        f.putFile("/data/q/foreign.bin", ByteArray(1))
        val limits = QuarantineLimits(budgetBytes = 4096L * 4)
        // foreign (4096) + new snap dir (4096... counted next scan) + need: 1 byte file -> 4096 + manifest/complete 2*4096 = 3 blocks => 4096 + 3*4096 + 4096 > 16384
        assertRefused(store(f, limits).secure(src("a"), meta), RefuseReason.BUDGET_FULL)
    }

    @Test fun `free space must cover twice the need plus the margin`() {
        val f = fs()
        f.space = 1000
        assertRefused(store(f).secure(src("a"), meta), RefuseReason.NO_SPACE)
        f.space = 10L * 1024 * 1024
        assertSecured(store(f).secure(src("a"), meta))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `a failing space query is a failure not a pass`() {
        val f = fs()
        assertSecured(store(f).secure(src("seed"), meta))
        f.arm(1, Fault.NONE)
        f.beforeOp = { op, _ -> if (op == "space") throw IllegalStateException("statfs") }
        val r = store(f).secure(src("other"), meta)
        assertTrue(describe(r), r is SecureResult.Failed)
    }

    // ---------------------------------------------------------------- collisions and filesystem oddities

    @Test fun `a racing creator of the same name pushes us to the next number`() {
        val f = fs()
        var injected = false
        f.beforeOp = { op, path ->
            if (!injected && op == "mkdir" && path == "/data/q/snap-0001") { injected = true; f.mkdirs("/data/q/snap-0001"); f.putFile("/data/q/snap-0001/main.xml", "theirs".toByteArray()) }
        }
        val r = assertSecured(store(f).secure(src("mine"), meta))
        assertEquals("snap-0002", r.name)
        assertArrayEquals("theirs".toByteArray(), f.bytesOf("/data/q/snap-0001/main.xml"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `persistent collisions end in a bounded refusal`() {
        val f = fs()
        f.beforeOp = { op, path -> if (op == "mkdir" && Regex("/data/q/snap-\\d+").matches(path) && f.nodes[path] == null) { f.mkdirs(path); f.putFile("$path/x", byteArrayOf(1)) } }
        val r = store(f).secure(src("mine"), meta)
        assertRefused(r, RefuseReason.COLLISION)
        assertTrue(f.log.count { it.startsWith("mkdir /data/q/snap-") } <= 6)
    }

    @Test fun `a plain file or link at the root path is refused and untouched`() {
        for (setup in listOf<(SimQuarantineFs) -> Unit>({ it.putFile("/data/q", byteArrayOf(1)) }, { it.putOther("/data/q") })) {
            val f = fs(); setup(f)
            val r = store(f).secure(src("a"), meta)
            assertTrue(describe(r), r is SecureResult.Failed && r.stage == FailStage.ROOT)
            assertEquals(emptyList<String>(), f.deleted)
        }
    }

    @Test fun `links and special files inside the root are never followed, counted as evidence or deleted`() {
        val f = fs()
        f.mkdirs("/data/q")
        f.putOther("/data/q/snap-0001")           // a symlink named like a snapshot
        f.mkdirs("/data/q/snap-0002"); f.putOther("/data/q/snap-0002/main.xml")
        f.putOther("/data/q/link-to-elsewhere")
        val r = assertSecured(store(f).secure(src("fresh"), meta))
        assertEquals("snap-0003", r.name)
        assertNotNull(f.nodes["/data/q/snap-0001"])
        assertNotNull(f.nodes["/data/q/snap-0002/main.xml"])
        assertNotNull(f.nodes["/data/q/link-to-elsewhere"])
        assertEquals(emptyList<String>(), f.deleted)
        assertTrue("no delete is even attempted on a link", f.log.none { (it.startsWith("rmdir") || it.startsWith("delete")) && it.contains("snap-0001") })
        assertTrue("a link is never listed as if it were a directory", f.log.none { it == "list /data/q/snap-0001" })
        // a link as the evidence file of an otherwise matching incomplete dir is not a promotion candidate
        assertEquals(1, completeNames(f).size)
    }

    @Test fun `directory sync failure is tolerated, evidence verification is not skipped`() {
        val f = fs(); f.syncDirFails = true
        assertSecured(store(f).secure(src("a"), meta))
        assertTrue(isComplete(f, "snap-0001"))
    }

    // ---------------------------------------------------------------- verification of completion

    @Test fun `a torn marker is never accepted and every prefix is handled`() {
        val base = fs()
        assertSecured(store(base).secure(src("payload"), meta))
        val good = base.bytesOf("/data/q/snap-0001/COMPLETE")!!
        for (n in 0 until good.size) {
            val f = fs(); assertSecured(store(f).secure(src("payload"), meta))
            f.putFile("/data/q/snap-0001/COMPLETE", good.copyOf(n))
            assertFalse("prefix $n accepted", isComplete(f, "snap-0001"))
            assertEquals("prefix $n counted", emptyList<String>(), store(f).summary().completeNames)
            // repair
            val r = assertSecured(store(f).secure(src("payload"), meta), SecureHow.PROMOTED)
            assertEquals("snap-0001", r.name)
            assertTrue(isComplete(f, "snap-0001"))
            assertArrayEquals("torn bytes preserved", good.copyOf(n), f.bytesOf("/data/q/snap-0001/COMPLETE.invalid-1"))
        }
    }

    @Test fun `a torn manifest is preserved then regenerated and the marker re-derived`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        val manifest = f.bytesOf("/data/q/snap-0001/MANIFEST")!!
        f.putFile("/data/q/snap-0001/MANIFEST", manifest.copyOf(manifest.size / 2))
        assertEquals(0, completeNames(f).size)
        assertSecured(store(f).secure(src("payload"), meta), SecureHow.PROMOTED)
        assertTrue(isComplete(f, "snap-0001"))
        assertArrayEquals(manifest.copyOf(manifest.size / 2), f.bytesOf("/data/q/snap-0001/MANIFEST.invalid-1"))
    }

    @Test fun `a valid marker over flipped evidence is not complete, and the flipped evidence is kept`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        val ev = f.bytesOf("/data/q/snap-0001/main.xml")!!.copyOf()
        ev[0] = (ev[0].toInt() xor 1).toByte()
        f.putFile("/data/q/snap-0001/main.xml", ev)
        assertEquals(emptyList<String>(), store(f).summary().completeNames)
        val r = assertSecured(store(f).secure(src("payload"), meta), SecureHow.CREATED)
        assertEquals("snap-0002", r.name)
        assertArrayEquals(ev, f.bytesOf("/data/q/snap-0001/main.xml"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `a marker whose manifest names a different sequence number is not complete`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        val fs2 = fs()
        assertSecured(store(fs2).secure(src("other"), meta))
        // copy snap-0001's files into a directory called snap-0007
        f.mkdirs("/data/q/snap-0007")
        for (n in f.children("/data/q/snap-0001")) f.putFile("/data/q/snap-0007/$n", f.bytesOf("/data/q/snap-0001/$n")!!)
        assertEquals(listOf("snap-0001"), store(f).summary().completeNames)
    }

    @Test fun `a stray evidence file not listed in the manifest makes the directory incomplete`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        f.putFile("/data/q/snap-0001/bak.xml", "extra".toByteArray())
        assertEquals(emptyList<String>(), store(f).summary().completeNames)
    }

    @Test fun `promotion adopts an incomplete directory whose evidence equals the source, without creating a new one`() {
        val f = fs()
        f.mkdirs("/data/q/snap-0001"); f.putFile("/data/q/snap-0001/main.xml", "m".toByteArray()); f.putFile("/data/q/snap-0001/bak.xml", "b".toByteArray())
        val r = assertSecured(store(f).secure(src("m", "b"), meta), SecureHow.PROMOTED)
        assertEquals("snap-0001", r.name)
        assertEquals(listOf("snap-0001"), snapNames(f))
        assertTrue(isComplete(f, "snap-0001"))
    }

    @Test fun `promotion is refused for a directory with different or extra content`() {
        val f = fs()
        f.mkdirs("/data/q/snap-0001"); f.putFile("/data/q/snap-0001/main.xml", "m".toByteArray()); f.putFile("/data/q/snap-0001/notes.txt", "hello".toByteArray())
        val r = assertSecured(store(f).secure(src("m"), meta), SecureHow.CREATED)
        assertEquals("snap-0002", r.name)
        assertNotNull(f.nodes["/data/q/snap-0001/notes.txt"])
    }

    @Test fun `a well-formed manifest that describes different evidence is preserved and replaced when the directory is adopted`() {
        val f = fs()
        assertSecured(store(f).secure(src("older-content"), meta))
        val staleManifest = f.bytesOf("/data/q/snap-0001/MANIFEST")!!
        f.putFile("/data/q/snap-0001/main.xml", "payload".toByteArray())   // evidence no longer matches its manifest
        assertEquals(emptyList<String>(), completeNames(f))
        val r = assertSecured(store(f).secure(src("payload"), meta), SecureHow.PROMOTED)
        assertEquals("snap-0001", r.name)
        assertTrue(isComplete(f, "snap-0001"))
        assertArrayEquals(staleManifest, f.bytesOf("/data/q/snap-0001/MANIFEST.invalid-1"))
    }

    @Test fun `a preserved copy that does not read back equal stops the repair before the original is removed`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        f.putFile("/data/q/snap-0001/COMPLETE", "torn".toByteArray())
        f.beforeOp = { op, path -> if (op == "read" && path.endsWith(".invalid-1")) f.putFile(path, "corrupted-copy".toByteArray()) }
        val r = store(f).secure(src("payload"), meta)
        assertTrue(describe(r), r is SecureResult.Failed)
        assertArrayEquals("torn".toByteArray(), f.bytesOf("/data/q/snap-0001/COMPLETE"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `repair gives up after a bounded number of preserved copies and never deletes the original`() {
        val f = fs()
        assertSecured(store(f).secure(src("payload"), meta))
        f.putFile("/data/q/snap-0001/COMPLETE", "junk".toByteArray())
        for (k in 1..8) f.putFile("/data/q/snap-0001/COMPLETE.invalid-$k", "old$k".toByteArray())
        assertRefused(store(f).secure(src("payload"), meta), RefuseReason.REPAIR_EXHAUSTED)
        assertArrayEquals("junk".toByteArray(), f.bytesOf("/data/q/snap-0001/COMPLETE"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    // ---------------------------------------------------------------- crashes and faults

    /** Runs secure() against [f], converting a simulated crash into null. */
    private fun attempt(f: SimQuarantineFs, s: SourceFiles, limits: QuarantineLimits = QuarantineLimits()): SecureResult? = try {
        store(f, limits).secure(s, meta)
    } catch (e: SimCrash) {
        null
    }

    private fun totalOps(): Int { val f = fs(); store(f).secure(src("m", "b"), meta); return f.opCount }

    @Test fun `a fault at every single operation, then a clean relaunch, always ends with one complete snapshot of the exact bytes`() {
        val n = totalOps()
        assertTrue(n > 20)
        for (fault in Fault.values().filter { it != Fault.NONE }) for (at in 1..n) {
            val f = fs()
            f.arm(at, fault)
            val first = attempt(f, src("m", "b"))
            f.disarm()
            // invariant 1: a Secured result is never a lie
            if (first is SecureResult.Secured) assertTrue("$fault@$at: secured but not complete", isComplete(f, first.name))
            // invariant 2: a failed attempt left no complete snapshot with wrong bytes
            for (c in completeNames(f)) assertArrayEquals("$fault@$at", "m".toByteArray(), f.bytesOf("/data/q/$c/main.xml"))
            // relaunch
            val second = store(f).secure(src("m", "b"), meta)
            assertTrue("$fault@$at relaunch: ${describe(second)}", second is SecureResult.Secured)
            assertEquals("$fault@$at", 1, completeNames(f).size)
            assertTrue("$fault@$at leftovers ${incompleteNames(f)}", incompleteNames(f).size <= 1)
            // the third launch is a pure reuse
            assertSecured(store(f).secure(src("m", "b"), meta), SecureHow.REUSED)
        }
    }

    @Test fun `a fault never deletes a complete snapshot and never deletes a file inside one`() {
        // a prior complete snapshot of other bytes must survive every fault of a later attempt
        val n = totalOps()
        for (fault in Fault.values().filter { it != Fault.NONE }) for (at in 1..n + 6) {
            val f = fs()
            assertSecured(store(f).secure(src("older"), meta))
            val before = f.nodes.filter { it.key.startsWith("/data/q/snap-0001/") }.mapValues { (it.value as SimQuarantineFs.Node.Data).bytes.toList() }
            f.arm(at, fault)
            attempt(f, src("m", "b"))
            f.disarm()
            assertTrue(isComplete(f, "snap-0001"))
            for ((path, bytes) in before) assertEquals("$fault@$at $path", bytes, (f.nodes[path] as SimQuarantineFs.Node.Data).bytes.toList())
            assertTrue(f.deleted.none { it.startsWith("/data/q/snap-0001") })
        }
    }

    @Test fun `repair is crash safe at every operation`() {
        val base = fs()
        assertSecured(store(base).secure(src("payload"), meta))
        val good = base.bytesOf("/data/q/snap-0001/COMPLETE")!!
        val probe = fs(); assertSecured(store(probe).secure(src("payload"), meta))
        probe.putFile("/data/q/snap-0001/COMPLETE", good.copyOf(5)); probe.resetCounter()
        store(probe).secure(src("payload"), meta)
        val n = probe.opCount
        for (fault in Fault.values().filter { it != Fault.NONE }) for (at in 1..n) {
            val f = fs(); assertSecured(store(f).secure(src("payload"), meta))
            f.putFile("/data/q/snap-0001/COMPLETE", good.copyOf(5))
            f.arm(at, fault); attempt(f, src("payload")); f.disarm()
            // the torn bytes are always somewhere: the original, or a preserved copy
            val preserved = f.children("/data/q/snap-0001").filter { it.startsWith("COMPLETE") }.mapNotNull { f.bytesOf("/data/q/snap-0001/$it") }
            assertTrue("$fault@$at torn bytes lost", preserved.any { it.contentEquals(good.copyOf(5)) } || isComplete(f, "snap-0001"))
            assertSecured(store(f).secure(src("payload"), meta))
            assertEquals("$fault@$at", 1, completeNames(f).size)
        }
    }

    @Test fun `a half-written evidence file from this very call is removed, with its empty directory`() {
        val f = fs()
        f.beforeOp = { op, path -> if (op == "create" && path.endsWith("main.xml")) { f.faultAt = f.opCount + 1; f.fault = Fault.SHORT_WRITE_THROW } }
        val r = store(f).secure(src("A".repeat(100)), meta)
        assertTrue(describe(r), r is SecureResult.Failed && r.stage == FailStage.WRITE_EVIDENCE)
        assertEquals(emptyList<String>(), snapNames(f))
        assertEquals(listOf("/data/q/snap-0001/main.xml", "/data/q/snap-0001"), f.deleted)
    }

    @Test fun `a short write is cleaned up only when it is this call's own unverified copy`() {
        val f = fs()
        f.arm(8, Fault.SHORT_WRITE_THROW) // lands on a create call in the sequence
        val r = attempt(f, src("A".repeat(100)))
        f.disarm()
        assertTrue(describe(r!!), r is SecureResult.Failed || r is SecureResult.Secured)
        // anything deleted was inside a snapshot directory that has no marker
        for (d in f.deleted) { val dir = d.substringBeforeLast('/'); assertFalse("deleted from a complete dir: $d", isComplete(f, dir.substringAfterLast('/'))) }
    }

    @Test fun `a transient read error while scanning never makes a verified snapshot look incomplete`() {
        val n = run { val p = fs(); for (i in 1..3) assertSecured(store(p).secure(src("v$i"), meta)); p.resetCounter(); store(p).secure(src("v4"), meta); p.opCount }
        for (at in 1..n) for (fault in listOf(Fault.THROW_BEFORE, Fault.THROW_AFTER_EFFECT)) {
            val f = fs()
            for (i in 1..3) assertSecured(store(f).secure(src("v$i"), meta))
            f.arm(at, fault)
            attempt(f, src("v4"))
            f.disarm()
            assertEquals("$fault@$at", 3, completeNames(f).size)
            assertEquals("$fault@$at", 3, snapNames(f).size)
            assertEquals(emptyList<String>(), f.deleted)
        }
    }

    @Test fun `evidence that does not read back equal is never reported as secured`() {
        val f = fs()
        f.beforeOp = { op, path -> if (op == "read" && path.endsWith("main.xml") && f.bytesOf(path) != null) f.putFile(path, "tampered".toByteArray()) }
        val r = store(f).secure(src("original-bytes"), meta)
        assertTrue(describe(r), r is SecureResult.Failed && r.stage == FailStage.VERIFY_EVIDENCE)
        assertEquals(emptyList<String>(), completeNames(f))
        assertTrue("no metadata is written over unverified evidence", f.log.none { it.startsWith("create") && (it.endsWith("MANIFEST") || it.endsWith("COMPLETE")) })
    }

    @Test fun `evidence flipped between the marker and the final check is caught`() {
        val f = fs()
        var armed = false
        f.beforeOp = { op, path ->
            if (op == "create" && path.endsWith("/COMPLETE")) armed = true
            else if (armed && op == "read" && path.endsWith("main.xml")) { f.putFile(path, "flipped!".toByteArray()); armed = false }
        }
        val r = store(f).secure(src("original-bytes"), meta)
        assertTrue(describe(r), r is SecureResult.Failed && r.stage == FailStage.VERIFY_COMPLETE)
        assertEquals(emptyList<String>(), completeNames(f))
    }

    @Test fun `a randomized fault campaign preserves every invariant`() {
        val rnd = Random(424242)
        val pool = listOf(src("one"), src("two", "twobak"), src(null, "bakonly"), src("three-3333"), src("four"), src("five", "fivebak"))
        repeat(150) { run ->
            val f = fs()
            val secured = ArrayList<SourceFiles>()
            repeat(14) { step ->
                val s = pool[rnd.nextInt(pool.size)]
                if (rnd.nextInt(3) == 0) f.arm(1 + rnd.nextInt(40), Fault.values()[1 + rnd.nextInt(Fault.values().size - 1)]) else f.disarm()
                val completeBefore = completeNames(f).associateWith { n -> f.children("/data/q/$n").associateWith { c -> f.bytesOf("/data/q/$n/$c")!!.toList() } }
                val r = attempt(f, s)
                f.disarm()
                if (r is SecureResult.Secured) {
                    assertTrue("run $run step $step", isComplete(f, r.name))
                    assertArrayEquals(s.main, f.bytesOf("/data/q/${r.name}/main.xml"))
                    assertArrayEquals(s.bak, f.bytesOf("/data/q/${r.name}/bak.xml"))
                    secured.add(s)
                }
                // complete snapshots are immutable and never disappear (repair may only add .invalid copies to incomplete dirs)
                for ((n, files) in completeBefore) {
                    assertTrue("run $run step $step lost $n", isComplete(f, n))
                    assertEquals(files, f.children("/data/q/$n").associateWith { c -> f.bytesOf("/data/q/$n/$c")!!.toList() })
                }
                assertTrue(completeNames(f).size <= 3)
                assertTrue("incomplete ${incompleteNames(f)}", incompleteNames(f).size <= 4)
            }
            // everything ever reported Secured is still held, byte for byte, by some complete snapshot
            for (s in secured) {
                assertTrue("run $run lost evidence", completeNames(f).any { n ->
                    f.bytesOf("/data/q/$n/main.xml")?.toList() == s.main?.toList() && f.bytesOf("/data/q/$n/bak.xml")?.toList() == s.bak?.toList()
                })
            }
        }
    }

    @Test fun `complete snapshots never lose a byte over a long randomized history with no faults`() {
        val rnd = Random(7)
        val f = fs()
        val reported = ArrayList<SourceFiles>()
        repeat(200) {
            val s = src("v${rnd.nextInt(6)}", if (rnd.nextBoolean()) "b${rnd.nextInt(3)}" else null)
            val r = store(f).secure(s, meta)
            if (r is SecureResult.Secured) reported.add(s)
        }
        for (s in reported) {
            assertTrue(completeNames(f).any { n ->
                f.bytesOf("/data/q/$n/main.xml")?.contentEquals(s.main) == true && (f.bytesOf("/data/q/$n/bak.xml")?.toList() == s.bak?.toList())
            })
        }
        assertEquals(3, completeNames(f).size)
        assertEquals(emptyList<String>(), f.deleted)
    }

    // ---------------------------------------------------------------- cleanup: positive proof only

    private fun withComplete(content: String = "0123456789ABCDEF"): SimQuarantineFs {
        val f = fs()
        assertSecured(store(f).secure(src(content), meta))
        return f
    }

    @Test fun `with no complete snapshot, no directory is eligible for cleanup, however damaged`() {
        val f = fs()
        f.mkdirs("/data/q/snap-0001"); f.putFile("/data/q/snap-0001/main.xml", "only-copy".toByteArray())
        f.mkdirs("/data/q/snap-0002"); f.putFile("/data/q/snap-0002/main.xml", "garbage".toByteArray()); f.putFile("/data/q/snap-0002/MANIFEST", byteArrayOf(1, 2))
        assertEquals(emptyList<String>(), store(f).cleanupPreview().names)
    }

    @Test fun `a prefix of a complete snapshot's file is eligible, anything else is not`() {
        val f = withComplete("0123456789ABCDEF")
        f.mkdirs("/data/q/snap-0010"); f.putFile("/data/q/snap-0010/main.xml", "01234567".toByteArray())                 // prefix: eligible
        f.mkdirs("/data/q/snap-0011"); f.putFile("/data/q/snap-0011/main.xml", "0123456789ABCDEF".toByteArray())         // equal: eligible
        f.mkdirs("/data/q/snap-0012"); f.putFile("/data/q/snap-0012/main.xml", "01234X".toByteArray())                   // diverges: keep
        f.mkdirs("/data/q/snap-0013"); f.putFile("/data/q/snap-0013/main.xml", "0123456789ABCDEFG".toByteArray())        // longer: keep
        f.mkdirs("/data/q/snap-0014"); f.putFile("/data/q/snap-0014/main.xml", "0123".toByteArray()); f.putFile("/data/q/snap-0014/bak.xml", "0123".toByteArray()) // no bak in complete: keep
        f.mkdirs("/data/q/snap-0015"); f.putFile("/data/q/snap-0015/main.xml", ByteArray(0))                              // zero length: eligible
        f.mkdirs("/data/q/snap-0016"); f.putFile("/data/q/snap-0016/main.xml", "0123".toByteArray()); f.putFile("/data/q/snap-0016/notes.txt", "x".toByteArray()) // foreign name: keep
        f.mkdirs("/data/q/snap-0017"); f.putFile("/data/q/snap-0017/main.xml", "0123".toByteArray()); f.mkdirs("/data/q/snap-0017/sub") // sub directory: keep
        f.mkdirs("/data/q/snap-0018"); f.putOther("/data/q/snap-0018/main.xml")                                           // link: keep
        f.mkdirs("/data/q/snap-0019"); f.putFile("/data/q/snap-0019/main.xml", "0123".toByteArray()); f.putFile("/data/q/snap-0019/MANIFEST", byteArrayOf(9)) // known metadata names: eligible
        assertEquals(listOf("snap-0010", "snap-0011", "snap-0015", "snap-0019"), store(f).cleanupPreview().names.sorted())
    }

    @Test fun `an incomplete directory with its own only copy is never previewed even if a complete one exists for other data`() {
        val f = withComplete("complete-data")
        f.mkdirs("/data/q/snap-0002"); f.putFile("/data/q/snap-0002/main.xml", "the-only-surviving-copy".toByteArray())
        assertEquals(emptyList<String>(), store(f).cleanupPreview().names)
        val plan = CleanupPlan(listOf("snap-0002"))
        assertTrue(store(f).cleanupConfirmed(plan) is CleanupResult.PlanChanged)
        assertNotNull(f.bytesOf("/data/q/snap-0002/main.xml"))
    }

    @Test fun `confirmed cleanup deletes exactly the shown plan and nothing in a complete snapshot`() {
        val f = withComplete("0123456789ABCDEF")
        f.mkdirs("/data/q/snap-0010"); f.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray())
        f.mkdirs("/data/q/snap-0011"); f.putFile("/data/q/snap-0011/main.xml", "keep-me-different".toByteArray())
        val s = store(f)
        val plan = s.cleanupPreview()
        assertEquals(listOf("snap-0010"), plan.names)
        val result = s.cleanupConfirmed(plan)
        assertEquals(listOf("snap-0010"), (result as CleanupResult.Deleted).names)
        assertNull(f.nodes["/data/q/snap-0010"])
        assertNotNull(f.bytesOf("/data/q/snap-0011/main.xml"))
        assertTrue(isComplete(f, "snap-0001"))
        assertTrue(f.deleted.none { it.startsWith("/data/q/snap-0001") })
    }

    @Test fun `if the eligible set changes between preview and confirm nothing is deleted`() {
        val f = withComplete("0123456789ABCDEF")
        f.mkdirs("/data/q/snap-0010"); f.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray())
        val s = store(f)
        val plan = s.cleanupPreview()
        // the content is replaced by something that is not a prefix (e.g. another process wrote there)
        f.putFile("/data/q/snap-0010/main.xml", "different".toByteArray())
        assertTrue(s.cleanupConfirmed(plan) is CleanupResult.PlanChanged)
        assertArrayEquals("different".toByteArray(), f.bytesOf("/data/q/snap-0010/main.xml"))
        // a newly eligible directory also invalidates the plan
        f.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray())
        val plan2 = s.cleanupPreview()
        f.mkdirs("/data/q/snap-0020"); f.putFile("/data/q/snap-0020/main.xml", "01".toByteArray())
        assertTrue(s.cleanupConfirmed(plan2) is CleanupResult.PlanChanged)
        assertNotNull(f.nodes["/data/q/snap-0010"])
    }

    @Test fun `a directory that becomes complete between the scan and the delete is not deleted`() {
        val f = withComplete("0123456789ABCDEF")
        f.mkdirs("/data/q/snap-0010"); f.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray())
        val s = store(f)
        val plan = s.cleanupPreview()
        assertEquals(listOf("snap-0010"), plan.names)
        var lists = 0
        f.beforeOp = { op, path ->
            if (op == "list" && path == "/data/q/snap-0010" && ++lists == 2) {
                val full = "0123456789ABCDEF".toByteArray()
                val manifest = Manifest.render(10, meta, listOf(ManifestFile("main.xml", full.size.toLong(), sha(full))))
                f.putFile("/data/q/snap-0010/main.xml", full)
                f.putFile("/data/q/snap-0010/MANIFEST", manifest)
                f.putFile("/data/q/snap-0010/COMPLETE", CompleteMarker.render(manifest))
            }
        }
        assertTrue(s.cleanupConfirmed(plan) is CleanupResult.PlanChanged)
        assertTrue(isComplete(f, "snap-0010"))
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `a fake plan naming a complete snapshot deletes nothing`() {
        val f = withComplete()
        assertTrue(store(f).cleanupConfirmed(CleanupPlan(listOf("snap-0001"))) is CleanupResult.PlanChanged)
        assertTrue(isComplete(f, "snap-0001"))
        assertTrue(store(f).cleanupConfirmed(CleanupPlan(listOf("../escape", "/etc"))) is CleanupResult.PlanChanged)
        assertEquals(emptyList<String>(), f.deleted)
    }

    @Test fun `a crash in the middle of cleanup leaves a state that is still safe and re-cleanable`() {
        val probe = withComplete("0123456789ABCDEF")
        probe.mkdirs("/data/q/snap-0010"); probe.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray()); probe.putFile("/data/q/snap-0010/MANIFEST", byteArrayOf(1))
        val s0 = store(probe); val p0 = s0.cleanupPreview(); probe.resetCounter(); s0.cleanupConfirmed(p0)
        val n = probe.opCount
        for (fault in Fault.values().filter { it != Fault.NONE }) for (at in 1..n) {
            val f = withComplete("0123456789ABCDEF")
            f.mkdirs("/data/q/snap-0010"); f.putFile("/data/q/snap-0010/main.xml", "0123".toByteArray()); f.putFile("/data/q/snap-0010/MANIFEST", byteArrayOf(1))
            val s = store(f); val plan = s.cleanupPreview()
            f.arm(at, fault)
            try { s.cleanupConfirmed(plan) } catch (e: SimCrash) { }
            f.disarm()
            assertTrue(isComplete(f, "snap-0001"))
            assertTrue(f.deleted.none { it.startsWith("/data/q/snap-0001") })
            val again = store(f); again.cleanupConfirmed(again.cleanupPreview())
            assertTrue(isComplete(f, "snap-0001"))
            assertTrue("$fault@$at: the re-run leaves nothing half-deleted that still lacks its evidence", f.nodes["/data/q/snap-0010"] == null || f.bytesOf("/data/q/snap-0010/main.xml") != null || f.children("/data/q/snap-0010").isEmpty())
        }
    }

    // ---------------------------------------------------------------- whole-class invariants

    @Test fun `the only deletions the store ever performs are on paths it created itself or on provably redundant or empty entries`() {
        val rnd = Random(99)
        repeat(60) {
            val f = fs()
            f.beforeOp = { op, path ->
                if ((op == "delete" || op == "rmdir")) {
                    val parent = path.substringBeforeLast('/')
                    val dirName = if (op == "delete") parent.substringAfterLast('/') else path.substringAfterLast('/')
                    assertFalse("delete inside complete snapshot: $op $path", isComplete(f, dirName) && op == "delete" && !path.contains(".invalid"))
                    assertTrue("delete outside snapshots: $path", path.startsWith("/data/q/snap-"))
                    assertFalse("complete dir removed whole: $path", op == "rmdir" && isComplete(f, dirName))
                }
            }
            repeat(10) {
                if (rnd.nextInt(4) == 0) f.arm(1 + rnd.nextInt(30), Fault.values()[1 + rnd.nextInt(6)]) else f.disarm()
                attempt(f, src("v${rnd.nextInt(5)}", if (rnd.nextBoolean()) "b" else null))
                f.disarm()
                try { val s = store(f); s.cleanupConfirmed(s.cleanupPreview()) } catch (e: SimCrash) { }
            }
        }
    }

    @Test fun `a source file that is a link is not something the ops can read`() {
        val f = fs(); f.putOther("/data/x")
        try { f.readAll(File("/data/x"), 10); throw AssertionError("must throw") } catch (e: java.io.IOException) { }
        try { f.createDirectoryExclusive(File("/data/x")); throw AssertionError() } catch (e: FileAlreadyExistsException) { }
    }
}
