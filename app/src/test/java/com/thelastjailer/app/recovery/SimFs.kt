package com.thelastjailer.app.recovery

import java.io.File
import java.io.IOException
import java.util.EnumMap

/** The process dies here. An [Error], so the store's `catch (e: Exception)` cannot swallow it. */
class SimulatedCrash : Error("simulated process death")

enum class SimOp { READ, ENSURE_DIR, WRITE, SYNC_FILE, RENAME, SYNC_DIR, DELETE, LIST }

enum class FaultMode {
    /** The call fails with an IOException before doing anything. */
    THROW,

    /** The call takes effect, then reports an IOException anyway (the worst case for "did it happen?"). */
    THROW_AFTER_EFFECT,

    CRASH_BEFORE,
    CRASH_AFTER,

    /** WRITE only: half the bytes land, then the call fails. */
    SHORT_WRITE_THROW,

    /** READ only: one byte of what is returned is flipped. The file on disk is fine. */
    CORRUPT_READ,

    /** SYNC_DIR only: the platform reports it cannot sync directories, and nothing is synced. */
    SYNC_DIR_UNSUPPORTED,

    /** RENAME only: the call reports success but did nothing (a lying filesystem). */
    RENAME_LOST
}

class SimFault(val op: SimOp, val nth: Int, val mode: FaultMode)

/** What a crash does to directory entries that were created or renamed but whose directory was never synced. */
enum class NameOutcome { AS_SYNCED, ALL_APPLIED }

/** What a crash does to file data that was written but never fsynced. */
enum class DataOutcome { SYNCED_ONLY, PARTIAL, ALL_PERSISTED }

/**
 * An in-memory filesystem with an explicit durability model, so tests can fail or kill the process at any single
 * operation and then inspect what could really be on disk afterwards.
 *
 * Model (deliberately adversarial, in the spirit of ext4 without barriers):
 *  * Each file has *volatile* content (what the running process sees) and *durable* content (what the last
 *    `syncFile` saved; none if never synced).
 *  * Directory entries have a volatile namespace and a durable one; `syncDirectory(dir)` copies that directory's
 *    entries from the first to the second.
 *  * A crash image picks, independently, how unsynced names and unsynced data are resolved: [NameOutcome] x
 *    [DataOutcome]. Every combination is a legal outcome, including a rename that persisted while the file's data
 *    did not.
 *
 * This is a MODEL. What it proves is the store's logic under that model; it cannot prove that Android's storage
 * stack behaves like it. That is a device question (see qa/G2_RECOVERY_STORE.md).
 */
class SimFs : RecoveryFileOps {

    private class Node(var volatile: ByteArray?, var durable: ByteArray?, val isDir: Boolean, val isLink: Boolean = false)

    private val vNames = LinkedHashMap<String, Node>()
    private var dNames = LinkedHashMap<String, Node>()
    val opLog = ArrayList<SimOp>()
    private val counts = EnumMap<SimOp, Int>(SimOp::class.java)
    val faults = ArrayList<SimFault>()

    /** Called at the start of every operation with (op, nth). Lets a test change the disk behind the store's back. */
    var interference: ((SimOp, Int) -> Unit)? = null

    private fun key(f: File): String {
        var p = f.path
        for ((alias, real) in aliases) if (p == alias || p.startsWith("$alias/")) p = real + p.substring(alias.length)
        return p
    }
    private fun parentKey(path: String) = File(path).parent

    private fun hit(op: SimOp): SimFault? {
        val n = (counts[op] ?: 0) + 1
        counts[op] = n
        opLog.add(op)
        interference?.invoke(op, n)
        return faults.firstOrNull { it.op == op && it.nth == n }
    }

    fun count(op: SimOp): Int = counts[op] ?: 0

    // ---- seeding and inspection (not subject to faults) ----

    fun seedDir(path: String) {
        val n = Node(null, null, true)
        vNames[path] = n
        dNames[path] = n
    }

    fun seedFile(path: String, bytes: ByteArray) {
        val n = Node(bytes.copyOf(), bytes.copyOf(), false)
        vNames[path] = n
        dNames[path] = n
    }

    /** An unexpected object (a symbolic link) planted at [path]. The store must never read or write through it. */
    fun seedSymlink(path: String) {
        val n = Node(ByteArray(0), ByteArray(0), false, isLink = true)
        vNames[path] = n
        dNames[path] = n
    }

    /** Test-only naive overwrite of an existing file's content, unsynced. The store itself never does this. */
    fun overwriteInPlaceForTest(path: String, bytes: ByteArray) { vNames.getValue(path).volatile = bytes.copyOf() }

    fun isLinkAt(path: String): Boolean = vNames[path]?.isLink == true
    fun isDirAt(path: String): Boolean = vNames[path]?.isDir == true

    /** Test hook: [alias] is another spelling of the directory [real] (a symbolic link in an ancestor). */
    private val aliases = LinkedHashMap<String, String>()
    fun addAlias(alias: String, real: String) { aliases[alias] = real }

    fun bytesOf(path: String): ByteArray? = vNames[path]?.takeIf { !it.isDir }?.volatile?.copyOf()
    fun exists(path: String): Boolean = vNames.containsKey(path)
    fun names(dir: String): List<String> =
        vNames.keys.filter { parentKey(it) == dir }.map { File(it).name }.sorted()

    /** Removes every fault and starts counting from zero again. */
    fun clearFaults() { faults.clear() }

    // ---- RecoveryFileOps ----

    override fun read(path: File, maxBytes: Int): FileRead {
        val f = hit(SimOp.READ)
        if (f?.mode == FaultMode.THROW) throw IOException("injected read failure")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        val node = vNames[key(path)] ?: run {
            if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
            return FileRead.Missing
        }
        if (node.isDir || node.isLink) throw IOException("not a plain regular file")
        var bytes = node.volatile!!
        if (f?.mode == FaultMode.CORRUPT_READ && bytes.isNotEmpty()) {
            bytes = bytes.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x5A).toByte() }
        }
        val data = FileRead.Data(bytes.copyOf(minOf(bytes.size, maxBytes)), bytes.size.toLong())
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        return data
    }

    override fun ensureDirectory(dir: File): Boolean {
        val f = hit(SimOp.ENSURE_DIR)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        val existing = vNames[key(dir)]
        if (existing != null && !existing.isDir) throw IOException("not a real directory")
        val created = existing == null
        if (created) {
            // Like Files.createDirectory: the parent must already exist. Ancestors are never made on the way.
            val parent = vNames[parentKey(key(dir)) ?: ""]
            if (parent == null || !parent.isDir) throw IOException("parent directory missing")
            vNames[key(dir)] = Node(null, null, true)
        }
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
        return created
    }

    override fun createFileExclusive(path: File, bytes: ByteArray) {
        val f = hit(SimOp.WRITE)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        val parent = vNames[parentKey(key(path)) ?: ""]
        if (parent == null || !parent.isDir) throw IOException("no such directory")
        // O_CREAT | O_EXCL: anything at all already there (file, directory, symbolic link) is an error.
        if (vNames.containsKey(key(path))) throw java.nio.file.FileAlreadyExistsException(key(path))
        val content = if (f?.mode == FaultMode.SHORT_WRITE_THROW) bytes.copyOf(bytes.size / 2) else bytes.copyOf()
        vNames[key(path)] = Node(content, null, false)
        if (f?.mode == FaultMode.SHORT_WRITE_THROW) throw IOException("injected short write")
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
    }

    override fun syncFile(path: File) {
        val f = hit(SimOp.SYNC_FILE)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        val node = vNames[key(path)] ?: throw IOException("no such file")
        if (node.isLink || node.isDir) throw IOException("not a plain regular file")
        node.durable = node.volatile?.copyOf()
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
    }

    override fun rename(from: File, to: File) {
        val f = hit(SimOp.RENAME)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        if (f?.mode == FaultMode.RENAME_LOST) return
        val node = vNames.remove(key(from)) ?: throw IOException("no such file")
        vNames[key(to)] = node
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
    }

    override fun syncDirectory(dir: File): DirSync {
        val f = hit(SimOp.SYNC_DIR)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        if (f?.mode == FaultMode.SYNC_DIR_UNSUPPORTED) return DirSync.UNSUPPORTED
        val d = key(dir)
        val live = vNames.filterKeys { parentKey(it) == d }
        dNames.keys.removeAll { parentKey(it) == d && !live.containsKey(it) }
        for ((k, v) in live) dNames[k] = v
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
        return DirSync.SYNCED
    }

    override fun delete(path: File) {
        val f = hit(SimOp.DELETE)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        if (f?.mode == FaultMode.CRASH_BEFORE) throw SimulatedCrash()
        vNames.remove(key(path))
        if (f?.mode == FaultMode.CRASH_AFTER) throw SimulatedCrash()
        if (f?.mode == FaultMode.THROW_AFTER_EFFECT) throw IOException("injected after effect")
    }

    override fun lockIdentity(dir: File): String = key(dir)

    override fun listNames(dir: File): List<String> {
        val f = hit(SimOp.LIST)
        if (f?.mode == FaultMode.THROW) throw IOException("injected")
        val node = vNames[key(dir)]
        if (node == null || !node.isDir) return emptyList()
        return names(key(dir))
    }

    /**
     * A fresh filesystem holding what could be on disk after the process died right now. An entry whose parent
     * directory's own entry did not survive is unreachable, so it is dropped (that is how a missing parent-directory
     * sync loses a freshly created slot directory and everything in it).
     */
    fun crashImage(names: NameOutcome, data: DataOutcome): SimFs {
        val image = SimFs()
        val source = if (names == NameOutcome.AS_SYNCED) dNames else vNames
        val kept = HashSet<String>()
        for (path in source.keys.sortedBy { it.length }) {
            val node = source.getValue(path)
            val parent = parentKey(path)
            val reachable = parent == null || parent in kept
            if (!reachable) continue
            if (node.isDir) { kept.add(path); image.seedDir(path); continue }
            if (node.isLink) { image.seedSymlink(path); continue }
            val volatile = node.volatile!!
            val content = when (data) {
                DataOutcome.SYNCED_ONLY -> node.durable ?: ByteArray(0)
                DataOutcome.ALL_PERSISTED -> volatile
                DataOutcome.PARTIAL -> {
                    val durable = node.durable
                    if (durable != null && durable.contentEquals(volatile)) volatile else volatile.copyOf(volatile.size / 2)
                }
            }
            image.seedFile(path, content)
        }
        return image
    }

    /** An independent copy of this filesystem's *current* contents, fully durable, with no faults. */
    fun duplicate(): SimFs = crashImage(NameOutcome.ALL_APPLIED, DataOutcome.ALL_PERSISTED)
}
