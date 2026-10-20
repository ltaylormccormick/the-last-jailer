package com.thelastjailer.app.recovery

import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The production filesystem adapter and the store on a REAL (JVM) filesystem. These show the adapter does what the
 * simulator assumed of it on the CI host; they say nothing about Android's storage stack.
 */
class JavaRecoveryFileOpsTest {

    @get:Rule val tmp = TemporaryFolder()

    private val ops = JavaRecoveryFileOps()

    @Test
    fun `a missing file reads as Missing, not as an error`() {
        assertTrue(ops.read(File(tmp.root, "nope"), 100) is FileRead.Missing)
    }

    @Test
    fun `read returns at most maxBytes but reports the true length`() {
        val f = tmp.newFile("f")
        f.writeBytes(ByteArray(1000) { it.toByte() })
        val r = ops.read(f, 100) as FileRead.Data
        assertEquals(100, r.bytes.size)
        assertEquals(1000L, r.totalLength)
        assertArrayEquals(ByteArray(100) { it.toByte() }, r.bytes)
    }

    @Test
    fun `reading a directory is an error, not Missing`() {
        try { ops.read(tmp.newFolder("d"), 10); fail() } catch (expected: IOException) {}
    }

    @Test
    fun `rename atomically replaces an existing file`() {
        val from = tmp.newFile("from"); from.writeBytes(byteArrayOf(1, 2, 3))
        val to = tmp.newFile("to"); to.writeBytes(byteArrayOf(9))
        ops.rename(from, to)
        assertFalse(from.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), to.readBytes())
    }

    @Test
    fun `write sync and directory sync work on a normal directory`() {
        val d = tmp.newFolder("slot")
        val f = File(d, "x.tmp")
        ops.writeFile(f, ByteArray(10) { 1 })
        ops.syncFile(f)
        assertEquals(DirSync.SYNCED, ops.syncDirectory(d))
    }

    @Test
    fun `ensureDirectory reports creation only once and delete tolerates a missing file`() {
        val d = File(tmp.root, "a/b")
        assertTrue(ops.ensureDirectory(d))
        assertFalse(ops.ensureDirectory(d))
        ops.delete(File(d, "ghost"))
    }

    @Test
    fun `listNames is sorted and empty for a missing directory`() {
        assertEquals(emptyList<String>(), ops.listNames(File(tmp.root, "none")))
        tmp.newFile("b"); tmp.newFile("a")
        assertEquals(listOf("a", "b"), ops.listNames(tmp.root))
    }

    @Test
    fun `the store works end to end on a real filesystem and survives a restart`() {
        val root = File(tmp.root, "recovery")
        val first = RecoveryBundleStore(root)
        val r = first.put(BundleRequest(1, 1, 1, Rb.payload(1)))
        assertTrue(r is PutResult.Stored && r.dirSynced)
        first.put(BundleRequest(1, 2, 1, Rb.payload(2)))
        // A brand new store object over the same directory (a process restart) sees both bundles.
        val second = RecoveryBundleStore(root)
        assertEquals(listOf(1), second.slotsOnDisk())
        val i = second.inspect(1)
        assertEquals("VALID+VALID", Rb.kinds(i))
        assertEquals(2L, (second.inspect(1).newestValid() as Newest.One).report.let { (it.status as BufferStatus.Valid).bundle.meta.generation })
        assertEquals(listOf("a.bundle", "b.bundle"), ops.listNames(File(root, "slot_1")))
    }

    @Test
    fun `a damaged file on a real filesystem is classified and left in place`() {
        val root = File(tmp.root, "recovery")
        val store = RecoveryBundleStore(root)
        store.put(BundleRequest(1, 1, 1, Rb.payload(1)))
        val a = File(root, "slot_1/a.bundle")
        a.writeBytes(a.readBytes().copyOf(40)) // truncate
        assertEquals("CORRUPT+ABSENT", Rb.kinds(store.inspect(1)))
        assertEquals(40L, a.length())
    }

    @Test
    fun `concurrent writers to one slot never corrupt it and every write gets its own generation`() {
        val root = File(tmp.root, "recovery")
        val threads = 4
        val each = 15
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val generations = java.util.Collections.synchronizedList(ArrayList<Long>())
        val failures = java.util.Collections.synchronizedList(ArrayList<String>())
        repeat(threads) { t ->
            pool.execute {
                // Each thread has its own store object over the same directory: the lock is shared by path.
                val store = RecoveryBundleStore(root)
                start.await()
                repeat(each) { n ->
                    when (val r = store.put(BundleRequest(1, t + 1, 1, Rb.payload(t * 1000 + n)))) {
                        is PutResult.Stored -> generations.add(r.generation)
                        else -> failures.add(r::class.simpleName ?: "?")
                    }
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        assertEquals("no write may fail or be skipped: $failures", emptyList<String>(), failures)
        assertEquals((1L..(threads * each).toLong()).toList(), generations.sorted())
        val final = RecoveryBundleStore(root).inspect(1)
        assertEquals("VALID+VALID", Rb.kinds(final))
    }
}
