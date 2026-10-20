package com.thelastjailer.app.saveguard.quarantine

import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

/** The process died. An Error, so no `catch (e: Exception)` / `catch (e: IOException)` in the code under test can absorb it. */
class SimCrash : Error("simulated process death")

enum class Fault { NONE, THROW_BEFORE, THROW_AFTER_EFFECT, CRASH_BEFORE, CRASH_AFTER_EFFECT, SHORT_WRITE_THROW, SHORT_WRITE_CRASH }

/**
 * An in-memory filesystem implementing the quarantine seam, with a single numbered fault. Everything an operation does
 * to the "disk" stays after a crash, which is what the next launch then sees. Symlinks / specials are modelled as OTHER.
 */
class SimQuarantineFs : QuarantineFileOps {
    sealed class Node {
        class Dir : Node()
        class Data(var bytes: ByteArray) : Node()
        class Other : Node()
    }

    val nodes = LinkedHashMap<String, Node>()
    var opCount = 0
        private set
    var faultAt = -1
    var fault = Fault.NONE
    var space = Long.MAX_VALUE / 4
    var syncDirFails = false
    val log = ArrayList<String>()
    val deleted = ArrayList<String>()

    /** Called before every operation; tests use it to inject races or to assert invariants at the moment of a delete. */
    var beforeOp: ((String, String) -> Unit)? = null

    fun resetCounter() { opCount = 0 }
    fun arm(at: Int, f: Fault) { faultAt = at; fault = f; opCount = 0 }
    fun disarm() { faultAt = -1; fault = Fault.NONE }

    fun mkdirs(path: String) { var p = ""; for (part in path.split("/").filter { it.isNotEmpty() }) { p += "/$part"; nodes.putIfAbsent(p, Node.Dir()) } }
    fun putFile(path: String, bytes: ByteArray) { nodes[path] = Node.Data(bytes.copyOf()) }
    fun putOther(path: String) { nodes[path] = Node.Other() }
    fun bytesOf(path: String): ByteArray? = (nodes[path] as? Node.Data)?.bytes
    fun children(dir: String): List<String> = nodes.keys.filter { it.startsWith("$dir/") && !it.substring(dir.length + 1).contains('/') }.map { it.substring(dir.length + 1) }.sorted()

    private fun p(f: File) = f.path

    /** Counts the operation and decides what the armed fault does. Returns true if the effect should be applied before throwing. */
    private fun enter(op: String, path: File): Fault {
        beforeOp?.invoke(op, p(path))
        opCount++
        log.add("$op ${p(path)}")
        return if (faultAt == opCount) fault else Fault.NONE
    }

    private fun after(f: Fault) {
        when (f) {
            Fault.THROW_AFTER_EFFECT -> throw IOException("injected after effect")
            Fault.CRASH_AFTER_EFFECT -> throw SimCrash()
            else -> Unit
        }
    }

    private fun before(f: Fault) {
        when (f) {
            Fault.THROW_BEFORE -> throw IOException("injected before effect")
            Fault.CRASH_BEFORE -> throw SimCrash()
            else -> Unit
        }
    }

    override fun kind(path: File): EntryKind {
        val f = enter("kind", path); before(f)
        val r = when (nodes[p(path)]) { null -> EntryKind.MISSING; is Node.Dir -> EntryKind.DIRECTORY; is Node.Data -> EntryKind.FILE; is Node.Other -> EntryKind.OTHER }
        after(f); return r
    }

    override fun size(path: File): Long {
        val f = enter("size", path); before(f)
        val n = nodes[p(path)] as? Node.Data ?: throw IOException("not a plain file")
        after(f); return n.bytes.size.toLong()
    }

    override fun readAll(path: File, maxBytes: Int): ByteArray {
        val f = enter("read", path); before(f)
        val n = nodes[p(path)] as? Node.Data ?: throw IOException("not a plain file")
        if (n.bytes.size > maxBytes) throw IOException("too long")
        after(f); return n.bytes.copyOf()
    }

    override fun createDirectoryExclusive(dir: File) {
        val f = enter("mkdir", dir); before(f)
        if (nodes.containsKey(p(dir))) throw FileAlreadyExistsException(p(dir))
        val parent = dir.parent
        if (parent != null && nodes[parent] !is Node.Dir) throw IOException("no parent")
        nodes[p(dir)] = Node.Dir()
        after(f)
    }

    override fun createFileExclusive(path: File, bytes: ByteArray) {
        val f = enter("create", path); before(f)
        if (nodes.containsKey(p(path))) throw FileAlreadyExistsException(p(path))
        if (nodes[path.parent ?: ""] !is Node.Dir) throw IOException("no parent")
        when (f) {
            Fault.SHORT_WRITE_THROW, Fault.SHORT_WRITE_CRASH -> {
                nodes[p(path)] = Node.Data(bytes.copyOf(bytes.size / 2))
                if (f == Fault.SHORT_WRITE_CRASH) throw SimCrash() else throw IOException("short write")
            }
            else -> nodes[p(path)] = Node.Data(bytes.copyOf())
        }
        after(f)
    }

    override fun syncFile(path: File) {
        val f = enter("sync", path); before(f)
        if (nodes[p(path)] !is Node.Data) throw IOException("not a file")
        after(f)
    }

    override fun syncDirectory(dir: File): DirSync {
        val f = enter("syncdir", dir); before(f)
        if (syncDirFails) throw IOException("directory sync unsupported")
        after(f); return DirSync.SYNCED
    }

    override fun deleteFile(path: File) {
        val f = enter("delete", path); before(f)
        if (nodes[p(path)] is Node.Dir) throw IOException("refusing a directory")
        if (nodes.remove(p(path)) != null) deleted.add(p(path))
        after(f)
    }

    override fun deleteEmptyDirectory(dir: File) {
        val f = enter("rmdir", dir); before(f)
        if (nodes[p(dir)] !is Node.Dir) throw IOException("not a directory")
        if (children(p(dir)).isNotEmpty()) throw IOException("not empty")
        nodes.remove(p(dir)); deleted.add(p(dir))
        after(f)
    }

    override fun listNames(dir: File): List<String> {
        val f = enter("list", dir); before(f)
        val r = if (nodes[p(dir)] is Node.Dir) children(p(dir)) else emptyList()
        after(f); return r
    }

    override fun usableSpace(dir: File): Long {
        val f = enter("space", dir); before(f)
        after(f); return space
    }
}
