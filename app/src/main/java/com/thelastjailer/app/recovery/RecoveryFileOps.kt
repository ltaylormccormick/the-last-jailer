package com.thelastjailer.app.recovery

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
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
 * What this seam does **not** promise: that [syncFile], [rename] and [syncDirectory] make data durable on a real
 * Android filesystem. The JVM simulator in the tests proves the store's logic against a *model* of durability;
 * only a device can show the model is true. See qa/G2_RECOVERY_STORE.md.
 */
interface RecoveryFileOps {
    /** Reads at most [maxBytes] from the start of [path]. Throws if [path] exists but cannot be read. */
    fun read(path: File, maxBytes: Int): FileRead

    /** Creates [dir] (and missing parents). Returns true only if [dir] itself did not exist and was created. */
    fun ensureDirectory(dir: File): Boolean

    /** Creates or truncates [path] and writes all of [bytes]. Does not sync. */
    fun writeFile(path: File, bytes: ByteArray)

    /** Forces [path]'s data to stable storage. */
    fun syncFile(path: File)

    /** Atomically moves [from] over [to], replacing it. */
    fun rename(from: File, to: File)

    /** Forces the directory's entries (a created or renamed file) to stable storage. */
    fun syncDirectory(dir: File): DirSync

    /** Deletes [path]. A missing file is not an error. */
    fun delete(path: File)

    /** Names directly inside [dir]; empty if [dir] does not exist. */
    fun listNames(dir: File): List<String>
}

/**
 * The production implementation, on `java.nio.file` (available from API 26, the project's minSdk).
 *
 * UNVERIFIED on Android: [syncDirectory] opens the directory read-only and calls `force`, which is the standard
 * JVM-on-Linux way to fsync a directory. Whether Android's runtime accepts that on every supported API level and
 * filesystem is an open question for the device-matrix gate (G7). If it throws, the store reports an UNCERTAIN
 * outcome rather than ignoring the failure; an adapter that knows better can return [DirSync.UNSUPPORTED].
 */
class JavaRecoveryFileOps : RecoveryFileOps {

    override fun read(path: File, maxBytes: Int): FileRead {
        val p = path.toPath()
        try {
            if (!Files.exists(p)) return FileRead.Missing
            if (!Files.isRegularFile(p)) throw IOException("not a regular file")
            val total = Files.size(p)
            val want = minOf(total, maxBytes.toLong()).toInt()
            val out = ByteArray(want)
            Files.newInputStream(p).use { input ->
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
        if (Files.isDirectory(p)) return false
        Files.createDirectories(p)
        return true
    }

    override fun writeFile(path: File, bytes: ByteArray) {
        Files.newOutputStream(
            path.toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
        ).use { it.write(bytes) }
    }

    override fun syncFile(path: File) {
        FileChannel.open(path.toPath(), StandardOpenOption.WRITE).use { it.force(true) }
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
        Files.deleteIfExists(path.toPath())
    }

    override fun listNames(dir: File): List<String> {
        val p = dir.toPath()
        if (!Files.isDirectory(p)) return emptyList()
        return Files.newDirectoryStream(p).use { stream -> stream.map { it.fileName.toString() }.sorted() }
    }
}
