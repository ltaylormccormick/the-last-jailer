package com.thelastjailer.app.recovery

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

sealed interface FileRead {
    /** There is no such file. This is *not* an error: it is how an absent bundle looks. */
    object Missing : FileRead

    /** [bytes] are the first `min(totalLength, maxBytes)` bytes; [totalLength] is the whole file's size. */
    class Data(val bytes: ByteArray, val totalLength: Long) : FileRead
}

enum class DirSync {
    SYNCED,

    /** The platform adapter cannot sync a directory. The store reports this instead of pretending. */
    UNSUPPORTED
}

/**
 * Every filesystem touch the recovery store makes goes through here, one operation per method, so tests can fail
 * or crash the process at any single step. All methods throw [IOException] on failure.
 *
 * Confinement rules every implementation must keep (the store relies on them):
 *  * [read] treats anything that is not a plain regular file as an error. A symbolic link, directory or special
 *    file is never followed or read through.
 *  * [createFileExclusive] fails if *anything* already exists at the path, a symbolic link included, and never
 *    follows a link. Together with a delete just before it, a planted link cannot redirect the write.
 *  * [ensureDirectory] creates only the one directory it is given, never missing ancestors, and refuses a symbolic
 *    link where the directory should be.
 *
 * What this seam does **not** promise: that [syncFile], [rename] and [syncDirectory] make data durable on a real
 * Android filesystem. The JVM simulator in the tests proves the store's logic against a *model* of durability;
 * only a device can show the model is true. See qa/G2_RECOVERY_STORE.md.
 */
interface RecoveryFileOps {
    /** Reads at most [maxBytes] from the start of [path]. Throws if [path] exists but is not a readable regular file. */
    fun read(path: File, maxBytes: Int): FileRead

    /**
     * Creates [dir] only. Its parent must already exist. Returns true if [dir] was created, false if it was already
     * a real directory. Throws if [dir] exists as anything else (including a symbolic link, even to a directory).
     */
    fun ensureDirectory(dir: File): Boolean

    /** Creates [path], failing if anything exists there, and writes all of [bytes]. Does not sync. */
    fun createFileExclusive(path: File, bytes: ByteArray)

    /** Forces [path]'s data to stable storage. */
    fun syncFile(path: File)

    /** Atomically moves [from] over [to], replacing it (a symbolic link at [to] is replaced, not followed). */
    fun rename(from: File, to: File)

    /** Forces the directory's entries (a created or renamed file) to stable storage. */
    fun syncDirectory(dir: File): DirSync

    /** Deletes [path] itself (a symbolic link is removed, not followed). A missing file is not an error. */
    fun delete(path: File)

    /** Names directly inside [dir]; empty if [dir] does not exist. */
    fun listNames(dir: File): List<String>

    /**
     * The identity of the physical directory [dir] names, used to decide which writers must share a lock. Two
     * spellings of the same directory (different relative or absolute forms, or a symbolic link in an *ancestor*
     * component such as `/data/data` vs `/data/user/0`) must return the same string. Never throws.
     */
    fun lockIdentity(dir: File): String
}

/**
 * The production implementation, on `java.nio.file` (available from API 26, the project's minSdk).
 *
 * UNVERIFIED on Android: the no-follow open options and [syncDirectory] opens the directory read-only and calls `force`, which is the standard
 * JVM-on-Linux way to fsync a directory. Whether Android's runtime accepts that on every supported API level and
 * filesystem is an open question for the device-matrix gate (G7). If it throws, the store reports an UNCERTAIN
 * outcome rather than ignoring the failure; an adapter that knows better can return [DirSync.UNSUPPORTED].
 */
class JavaRecoveryFileOps : RecoveryFileOps {

    override fun read(path: File, maxBytes: Int): FileRead {
        val p = path.toPath()
        try {
            if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) return FileRead.Missing
            if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("not a plain regular file")
            val total = Files.size(p)
            val want = minOf(total, maxBytes.toLong()).toInt()
            val out = ByteArray(want)
            Files.newInputStream(p, LinkOption.NOFOLLOW_LINKS).use { input ->
                var read = 0
                while (read < want) {
                    val n = input.read(out, read, want - read)
                    if (n < 0) break
                    read += n
                }
                if (read != want) throw IOException("file shrank while reading")
            }
            return FileRead.Data(out, total)
        } catch (e: NoSuchFileException) {
            return FileRead.Missing
        }
    }

    override fun ensureDirectory(dir: File): Boolean {
        val p = dir.toPath()
        if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) return false
        if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("not a real directory (file or symbolic link in the way)")
        try {
            Files.createDirectory(p) // not createDirectories: a missing parent is an error, never created on the way
        } catch (e: FileAlreadyExistsException) {
            if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) return false
            throw e
        }
        return true
    }

    override fun createFileExclusive(path: File, bytes: ByteArray) {
        // CREATE_NEW is an atomic exclusive create (O_CREAT | O_EXCL): it fails on an existing file, directory or
        // symbolic link (dangling or not) rather than following it. NOFOLLOW_LINKS is belt and braces.
        Files.newOutputStream(
            path.toPath(),
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS
        ).use { it.write(bytes) }
    }

    override fun syncFile(path: File) {
        FileChannel.open(path.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { it.force(true) }
    }

    override fun rename(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            // Never fall back to a non-atomic copy: a torn replacement is exactly what this store exists to avoid.
            throw IOException("atomic rename not supported", e)
        }
    }

    override fun syncDirectory(dir: File): DirSync {
        FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
        return DirSync.SYNCED
    }

    override fun delete(path: File) {
        Files.deleteIfExists(path.toPath()) // deletes a symbolic link itself, never its target
    }

    override fun listNames(dir: File): List<String> {
        val p = dir.toPath()
        if (!Files.isDirectory(p)) return emptyList()
        return Files.newDirectoryStream(p).use { stream -> stream.map { it.fileName.toString() }.sorted() }
    }

    /**
     * The canonical path: every existing component is resolved, so an alias made by a symbolic link (or `.`, `..`,
     * a relative spelling) in any ancestor maps to one string. Components that do not exist yet are kept as written.
     * If the platform cannot canonicalise, the normalised absolute path is the weaker fallback.
     */
    override fun lockIdentity(dir: File): String = try {
        dir.canonicalPath
    } catch (e: IOException) {
        dir.absoluteFile.normalize().path
    }
}
