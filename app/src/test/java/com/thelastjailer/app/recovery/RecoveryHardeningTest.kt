package com.thelastjailer.app.recovery

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Regression tests for the two defects the adversarial review reproduced against the first G2 commit, on a REAL
 * filesystem with real symbolic links (the simulator cannot show a link being followed):
 *
 *  F1  two stores reaching one directory through different spellings did not share a lock, so both claimed to have
 *      stored the same generation (19 of 300 rounds in the original experiment).
 *  F2  a symbolic link planted at the temp-file name made the store overwrite a file outside the recovery directory,
 *      and directory creation made any missing ancestors on the way.
 *
 * Each test here fails against the first commit (702f5f9) and passes now; the QA note records that run.
 */
class RecoveryHardeningTest {

    @get:Rule val tmp = TemporaryFolder()
    private val ops = JavaRecoveryFileOps()

    private fun link(link: File, target: File) {
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath())
        } catch (e: UnsupportedOperationException) {
            assumeNoException("this platform cannot create symbolic links", e)
        } catch (e: IOException) {
            assumeNoException("this platform cannot create symbolic links", e)
        }
    }

    private fun req(seed: Int, op: Int = 1) = BundleRequest(1, op, 1, Rb.payload(seed))

    // ---------------------------------------------------------------- F1: lock identity

    @Test
    fun `F1 two spellings of one directory share a lock identity`() {
        val real = tmp.newFolder("real")
        val alias = File(tmp.root, "alias")
        link(alias, real)
        assertEquals(ops.lockIdentity(File(real, "recovery")), ops.lockIdentity(File(alias, "recovery")))
        // dots and a different working-directory-relative spelling too
        assertEquals(ops.lockIdentity(File(real, "recovery")), ops.lockIdentity(File(real, "x/../recovery")))
        assertEquals(ops.lockIdentity(File(real, "recovery")), ops.lockIdentity(File(alias, "./recovery")))
        // different directories never share one
        assertNotEquals(ops.lockIdentity(File(real, "recovery")), ops.lockIdentity(File(real, "other")))
    }

    @Test
    fun `F1 aliased stores never both report the same generation as stored`() {
        val real = tmp.newFolder("real")
        val alias = File(tmp.root, "alias")
        link(alias, real)
        val a = RecoveryBundleStore(File(real, "recovery"))
        val b = RecoveryBundleStore(File(alias, "recovery"))
        var duplicateRounds = 0
        var nonStored = 0
        val rounds = 200
        repeat(rounds) {
            File(real, "recovery").deleteRecursively()
            val gens = java.util.Collections.synchronizedList(ArrayList<Long>())
            val start = CountDownLatch(1)
            val threads = listOf(a to 1, b to 2).map { (store, id) ->
                Thread {
                    start.await()
                    repeat(4) { n ->
                        val r = store.put(req(id * 100 + n, op = id))
                        if (r is PutResult.Stored) gens.add(r.generation) else nonStored++
                    }
                }
            }
            threads.forEach { it.start() }
            start.countDown()
            threads.forEach { it.join() }
            if (gens.toSet().size != gens.size) duplicateRounds++
        }
        assertEquals("rounds where two writers both 'stored' one generation", 0, duplicateRounds)
        assertEquals("every write should simply be stored, none Uncertain or Failed", 0, nonStored)
    }

    @Test
    fun `F1 control - stores spelled identically also never collide`() {
        val dir = File(tmp.newFolder("same"), "recovery")
        val a = RecoveryBundleStore(dir)
        val b = RecoveryBundleStore(dir)
        val gens = java.util.Collections.synchronizedList(ArrayList<Long>())
        val ts = listOf(a, b).mapIndexed { i, s -> Thread { repeat(20) { n -> (s.put(req(i * 50 + n, op = i + 1)) as? PutResult.Stored)?.let { gens.add(it.generation) } } } }
        ts.forEach { it.start() }; ts.forEach { it.join() }
        assertEquals(gens.size, gens.toSet().size)
    }

    // ---------------------------------------------------------------- F2: confinement of writes

    @Test
    fun `F2 a symlink planted at the temp name never makes the store write outside the recovery directory`() {
        val victim = File(tmp.root, "victim.txt").apply { writeText("PRECIOUS-OUTSIDE-FILE") }
        val root = File(tmp.root, "recovery")
        val store = RecoveryBundleStore(root)
        assertTrue(store.put(req(1)) is PutResult.Stored)           // a.bundle exists; the next write targets b
        link(File(root, "slot_1/b.tmp"), victim)
        val r = store.put(req(2))
        assertEquals("PRECIOUS-OUTSIDE-FILE", victim.readText())
        assertTrue("the link is replaced, the write lands in the recovery directory: $r", r is PutResult.Stored)
        assertFalse(Files.isSymbolicLink(File(root, "slot_1/b.bundle").toPath()))
        assertEquals("VALID+VALID", Rb.kinds(store.inspect(1)))
    }

    @Test
    fun `F2 a dangling symlink at the temp name is neither followed nor created through`() {
        val outside = File(tmp.root, "does-not-exist-yet.txt")
        val root = File(tmp.root, "recovery")
        val store = RecoveryBundleStore(root)
        store.put(req(1))
        link(File(root, "slot_1/b.tmp"), outside)
        assertTrue(store.put(req(2)) is PutResult.Stored)
        assertFalse("nothing may be created at the link's target", outside.exists())
    }

    @Test
    fun `F2 createFileExclusive refuses a symlink, a directory and an existing file, and changes nothing`() {
        val victim = File(tmp.root, "victim.txt").apply { writeText("PRECIOUS") }
        val viaLink = File(tmp.root, "l"); link(viaLink, victim)
        try { ops.createFileExclusive(viaLink, ByteArray(5) { 7 }); fail("followed a symlink") } catch (e: IOException) {}
        assertEquals("PRECIOUS", victim.readText())

        val dir = tmp.newFolder("d")
        try { ops.createFileExclusive(dir, ByteArray(5)); fail("replaced a directory") } catch (e: IOException) {}
        assertTrue(dir.isDirectory)

        val existing = tmp.newFile("e").apply { writeText("KEEP") }
        try { ops.createFileExclusive(existing, ByteArray(5)); fail("overwrote an existing file") } catch (e: IOException) {}
        assertEquals("KEEP", existing.readText())
    }

    @Test
    fun `F2 a directory squatting on the temp name fails closed and leaves both buffers unchanged`() {
        val root = File(tmp.root, "recovery")
        val store = RecoveryBundleStore(root)
        store.put(req(1))
        val before = Rb.kinds(store.inspect(1))
        val squat = File(root, "slot_1/b.tmp").apply { mkdirs() }
        File(squat, "inner").writeText("x")                          // non-empty, so it cannot be deleted
        val r = store.put(req(2))
        assertTrue("$r", r is PutResult.Failed && r.stage == PutStage.WRITE_TEMP)
        assertEquals(before, Rb.kinds(store.inspect(1)))
        assertEquals("x", File(squat, "inner").readText())
    }

    @Test
    fun `F2 a bundle path that is a symlink is not read through and blocks the write`() {
        val outside = File(tmp.root, "outside.bin").apply { writeBytes(Rb.validBytes(9, 1)) }
        val root = File(tmp.root, "recovery")
        val store = RecoveryBundleStore(root)
        store.put(req(1))                                            // a.bundle
        val b = File(root, "slot_1/b.bundle"); link(b, outside)
        val i = store.inspect(1)
        assertEquals("a link is UNREADABLE, never followed to the valid file behind it", "UNREADABLE", Rb.statusKind(i.report(Buffer.B).status))
        val r = store.put(req(2))
        assertTrue("$r", r is PutResult.Blocked && r.reason == BlockReason.UNREADABLE_BUFFER)
        assertEquals(Rb.validBytes(9, 1).toList(), outside.readBytes().toList())
    }

    // ---------------------------------------------------------------- F2: directory creation

    @Test
    fun `F2 a missing parent of the recovery root is not created`() {
        val missing = File(tmp.root, "no/such/parent/recovery")
        val r = RecoveryBundleStore(missing).put(req(1))
        assertTrue("$r", r is PutResult.Failed && r.stage == PutStage.PREPARE)
        assertFalse(File(tmp.root, "no").exists())
    }

    @Test
    fun `F2 ensureDirectory creates one level only and refuses a symlink`() {
        try { ops.ensureDirectory(File(tmp.root, "x/y")); fail("created an ancestor") } catch (e: IOException) {}
        assertFalse(File(tmp.root, "x").exists())

        val real = tmp.newFolder("real")
        val viaLink = File(tmp.root, "lnk"); link(viaLink, real)
        try { ops.ensureDirectory(viaLink); fail("accepted a symlink as the directory") } catch (e: IOException) {}

        val file = tmp.newFile("f")
        try { ops.ensureDirectory(file); fail("accepted a file as the directory") } catch (e: IOException) {}
    }

    @Test
    fun `F2 a slot directory that is a symlink out of the root is never written into`() {
        val root = tmp.newFolder("recovery")
        val outsideDir = tmp.newFolder("outside")
        link(File(root, "slot_1"), outsideDir)
        val r = RecoveryBundleStore(root).put(req(1))
        assertTrue("$r", r is PutResult.Failed)
        assertEquals(emptyList<String>(), outsideDir.list()!!.toList())
    }

    @Test
    fun `F2 reading a symlink as a bundle is an error, not Missing and not the target's bytes`() {
        val target = tmp.newFile("t").apply { writeText("data") }
        val l = File(tmp.root, "l"); link(l, target)
        try { ops.read(l, 100); fail("read through a symlink") } catch (e: IOException) {}
    }
}
