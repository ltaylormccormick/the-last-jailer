package com.thelastjailer.app.saveguard.quarantine

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.StandardOpenOption

// G2L Stage B1 (dormant): every filesystem touch the quarantine store makes goes through this seam, one operation per
// method, so tests can fail or kill the process at any single step.
//
// Confinement rules every implementation must keep (the store relies on them):
//  * Nothing is ever followed through a symbolic link. A link is its own kind (OTHER) and is never read or written.
//  * Creation is exclusive and single-level: it fails if ANYTHING exists at the path, and never creates ancestors.
//  * There is deliberately NO rename and NO overwrite. Publishing a snapshot is a matter of exclusive creates only.
//  * Deletion is narrow: deleteFile refuses directories; deleteEmptyDirectory refuses anything with entries. There is
//    no recursive delete, so no code path can remove a tree.
//
// What this seam does not promise: that syncFile / syncDirectory make data durable on a real Android filesystem
// (the F5 limitation from the G2 store applies unchanged). "Verified" in this package means "read back equal".

enum class EntryKind { MISSING, FILE, DIRECTORY, OTHER }

enum class DirSync { SYNCED, UNSUPPORTED }

interface QuarantineFileOps {
    /** What is at [path], without following a link. A link, socket, etc. is OTHER. Never throws for a missing path. */
    fun kind(path: File): EntryKind

    /** Length of a plain regular file; throws if it is anything else. */
    fun size(path: File): Long

    /** The whole file, which must be a plain regular file of at most [maxBytes]; throws otherwise. */
    fun readAll(path: File, maxBytes: Int): ByteArray

    /** Creates exactly [dir]; its parent must exist. Throws java.nio.file.FileAlreadyExistsException if anything is there. */
    fun createDirectoryExclusive(dir: File)

    /** Creates [path] and writes all of [bytes]; throws java.nio.file.FileAlreadyExistsException if anything is there. */
    fun createFileExclusive(path: File, bytes: ByteArray)

    fun syncFile(path: File)

    fun syncDirectory(dir: File): DirSync

    /** Deletes a plain file or a symbolic link itself. Refuses a directory. A missing path is not an error. */
    fun deleteFile(path: File)

    /** Deletes a directory with no entries. Refuses anything else. */
    fun deleteEmptyDirectory(dir: File)

    /** Names directly inside [dir] (sorted); empty if it does not exist. */
    fun listNames(dir: File): List<String>

    fun usableSpace(dir: File): Long
}

/** java.nio.file implementation. UNVERIFIED on Android (qa/G2L_B1.md lists what Stage D must check). */
class JavaQuarantineFileOps : QuarantineFileOps {
    override fun kind(path: File): EntryKind {
        val p = path.toPath()
        return when {
            !Files.exists(p, LinkOption.NOFOLLOW_LINKS) -> EntryKind.MISSING
            Files.isSymbolicLink(p) -> EntryKind.OTHER
            Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) -> EntryKind.DIRECTORY
            Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) -> EntryKind.FILE
            else -> EntryKind.OTHER
        }
    }

    override fun size(path: File): Long {
        val p = path.toPath()
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("not a plain regular file")
        return Files.size(p)
    }

    override fun readAll(path: File, maxBytes: Int): ByteArray {
        val p = path.toPath()
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("not a plain regular file")
        val total = Files.size(p)
        if (total > maxBytes) throw IOException("file longer than the limit")
        val out = ByteArray(total.toInt())
        Files.newInputStream(p, LinkOption.NOFOLLOW_LINKS).use { input ->
            var read = 0
            while (read < out.size) {
                val n = input.read(out, read, out.size - read)
                if (n < 0) break
                read += n
            }
            if (read != out.size) throw IOException("file shrank while reading")
        }
        return out
    }

    override fun createDirectoryExclusive(dir: File) {
        Files.createDirectory(dir.toPath()) // not createDirectories: a missing parent is an error
    }

    override fun createFileExclusive(path: File, bytes: ByteArray) {
        Files.newOutputStream(
            path.toPath(),
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS
        ).use { it.write(bytes) }
    }

    override fun syncFile(path: File) {
        FileChannel.open(path.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { it.force(true) }
    }

    override fun syncDirectory(dir: File): DirSync {
        FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
        return DirSync.SYNCED
    }

    override fun deleteFile(path: File) {
        val p = path.toPath()
        if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("refusing to delete a directory as a file")
        try {
            Files.deleteIfExists(p)
        } catch (e: NoSuchFileException) {
            // already gone
        }
    }

    override fun deleteEmptyDirectory(dir: File) {
        val p = dir.toPath()
        if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) throw IOException("not a real directory")
        Files.delete(p) // fails with DirectoryNotEmptyException if it has entries
    }

    override fun listNames(dir: File): List<String> {
        val p = dir.toPath()
        if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.newDirectoryStream(p).use { stream -> stream.map { it.fileName.toString() }.sorted() }
    }

    override fun usableSpace(dir: File): Long = dir.usableSpace
}
