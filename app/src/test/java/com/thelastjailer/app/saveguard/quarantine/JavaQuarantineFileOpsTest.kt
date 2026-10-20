package com.thelastjailer.app.saveguard.quarantine

import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** The real java.nio implementation against a real temporary directory, including symbolic links. */
class JavaQuarantineFileOpsTest {
    private lateinit var dir: File
    private val ops = JavaQuarantineFileOps()

    @Before fun setUp() { dir = Files.createTempDirectory("tljq").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun fail(block: () -> Unit, type: Class<out Throwable> = IOException::class.java) {
        try { block() } catch (e: Throwable) { assertTrue("wrong type ${e.javaClass}", type.isInstance(e)); return }
        throw AssertionError("expected ${type.simpleName}")
    }

    @Test fun `create is exclusive for files and directories`() {
        val d = File(dir, "d"); ops.createDirectoryExclusive(d)
        fail({ ops.createDirectoryExclusive(d) }, FileAlreadyExistsException::class.java)
        val f = File(d, "f"); ops.createFileExclusive(f, byteArrayOf(1, 2, 3))
        fail({ ops.createFileExclusive(f, byteArrayOf(9)) }, FileAlreadyExistsException::class.java)
        assertArrayEquals(byteArrayOf(1, 2, 3), ops.readAll(f, 10))
    }

    @Test fun `creation never makes ancestors`() {
        fail({ ops.createDirectoryExclusive(File(dir, "a/b")) })
        fail({ ops.createFileExclusive(File(dir, "x/y"), byteArrayOf(1)) })
    }

    @Test fun `read enforces the limit and plain files only`() {
        val f = File(dir, "f"); ops.createFileExclusive(f, ByteArray(100))
        assertEquals(100, ops.readAll(f, 100).size)
        fail({ ops.readAll(f, 99) })
        fail({ ops.readAll(dir, 100) })
        assertEquals(100L, ops.size(f))
    }

    @Test fun `symbolic links are their own kind and are never followed`() {
        val target = File(dir, "target"); ops.createFileExclusive(target, "secret".toByteArray())
        val link = File(dir, "link")
        try { Files.createSymbolicLink(link.toPath(), target.toPath()) } catch (e: Exception) { assumeTrue("symlinks unsupported", false) }
        assertEquals(EntryKind.OTHER, ops.kind(link))
        fail({ ops.readAll(link, 100) })
        fail({ ops.size(link) })
        fail({ ops.createFileExclusive(link, byteArrayOf(1)) }, FileAlreadyExistsException::class.java)
        val dirLink = File(dir, "dlink"); Files.createSymbolicLink(dirLink.toPath(), dir.toPath())
        assertEquals(EntryKind.OTHER, ops.kind(dirLink))
        assertEquals(emptyList<String>(), ops.listNames(dirLink))
        // deleting a link removes the link, not its target
        ops.deleteFile(link)
        assertEquals(EntryKind.MISSING, ops.kind(link))
        assertEquals(EntryKind.FILE, ops.kind(target))
    }

    @Test fun `delete is narrow`() {
        val d = File(dir, "d"); ops.createDirectoryExclusive(d)
        val f = File(d, "f"); ops.createFileExclusive(f, byteArrayOf(1))
        fail({ ops.deleteFile(d) })
        fail({ ops.deleteEmptyDirectory(d) })
        fail({ ops.deleteEmptyDirectory(f) })
        ops.deleteFile(f)
        ops.deleteFile(f) // already gone is fine
        fail({ ops.deleteFile(d) })   // an EMPTY directory is still not deletable as a file
        assertEquals(EntryKind.DIRECTORY, ops.kind(d))
        ops.deleteEmptyDirectory(d)
        assertEquals(EntryKind.MISSING, ops.kind(d))
    }

    @Test fun `kinds and listing`() {
        assertEquals(EntryKind.MISSING, ops.kind(File(dir, "nope")))
        assertEquals(EntryKind.DIRECTORY, ops.kind(dir))
        ops.createFileExclusive(File(dir, "b"), byteArrayOf()); ops.createFileExclusive(File(dir, "a"), byteArrayOf())
        assertEquals(listOf("a", "b"), ops.listNames(dir))
        assertEquals(emptyList<String>(), ops.listNames(File(dir, "nope")))
        assertEquals(EntryKind.FILE, ops.kind(File(dir, "a")))
        assertTrue(ops.usableSpace(dir) > 0)
    }

    @Test fun `sync of a file works and directory sync reports its result`() {
        val f = File(dir, "f"); ops.createFileExclusive(f, byteArrayOf(1)); ops.syncFile(f)
        // Directory fsync may be unsupported on some platforms; the store treats that as best effort.
        try { assertEquals(DirSync.SYNCED, ops.syncDirectory(dir)) } catch (e: IOException) { /* acceptable */ }
    }

    @Test fun `the whole store works end to end on a real directory`() {
        val root = File(dir, "q")
        val store = QuarantineStore(ops, root)
        val meta = SnapshotMeta("1.0", 34, 0, "SUSPECT_FILE", listOf(1), "2026-09-13T00:00:00Z")
        val bytes = "<map><int name=\"a\" value=\"".toByteArray()
        val r = store.secure(SourceFiles(bytes, null), meta) as SecureResult.Secured
        assertEquals(SecureHow.CREATED, r.how)
        assertArrayEquals(bytes, File(root, "snap-0001/main.xml").readBytes())
        assertEquals(SecureHow.REUSED, (store.secure(SourceFiles(bytes, null), meta) as SecureResult.Secured).how)
        assertEquals(listOf("snap-0001"), store.summary().completeNames)
        assertEquals(emptyList<String>(), store.summary().incompleteNames)
    }
}
