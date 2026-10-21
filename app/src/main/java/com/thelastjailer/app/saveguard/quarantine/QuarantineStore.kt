package com.thelastjailer.app.saveguard.quarantine

import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException

// G2L Stage B1 (dormant): keeps verified byte-for-byte copies of a suspect preferences file, and nothing else.
//
// Protocol (design freeze section 1 as amended by A3 and C2): exclusive creation only, no rename, no overwrite.
//   1. reserve    createDirectoryExclusive(snap-NNNN)               the name reservation
//   2. evidence   createFileExclusive(main.xml / bak.xml) + sync    then re-read and compare with the bytes held
//   3. manifest   createFileExclusive(MANIFEST) + sync
//   4. complete   createFileExclusive(COMPLETE) + sync               written last
//   5. secure     re-read COMPLETE, MANIFEST and every evidence file again; only if all agree is the result Secured
// A snapshot is complete iff step 5 would pass, recomputed from disk on every scan, so no count ever depends on a
// flag held in memory. Anything else is incomplete. Incomplete is NOT disposable by default: it may hold the only
// surviving copy of something. The only deletions this class performs are listed in qa/G2L_B1.md and each needs a
// positive proof (an empty directory; this call's own partial copy before it was verified; an invalid metadata file
// whose bytes were first preserved; or, via the confirmed cleanup, a directory whose every evidence byte is provably
// a prefix of, or equal to, the same file in a complete snapshot). A directory with a valid COMPLETE is never touched.
//
// Dormant: no production code refers to it. No logging; results carry reason codes only, never file contents.

class QuarantineLimits(
    val maxFileBytes: Int = 1 shl 20,
    val completeMax: Int = 3,
    val incompleteMax: Int = 3,
    val budgetBytes: Long = 16L shl 20,
    val spaceMargin: Long = 64L shl 10,
    val blockSize: Int = 4096,
    val reserveTries: Int = 5,
    val repairMax: Int = 8
)

/** The bytes of the two preferences files as read before loading. null = the file did not exist. */
class SourceFiles(val main: ByteArray?, val bak: ByteArray?) {
    internal fun evidence(): List<Pair<String, ByteArray>> =
        listOfNotNull(main?.let { MAIN_NAME to it }, bak?.let { BAK_NAME to it })
}

enum class RefuseReason { EMPTY_SOURCE, TOO_LARGE, CAP_FULL, ATTEMPTS_EXHAUSTED, BUDGET_FULL, NO_SPACE, COLLISION, REPAIR_EXHAUSTED }

enum class FailStage { ROOT, SCAN, RESERVE, WRITE_EVIDENCE, VERIFY_EVIDENCE, WRITE_MANIFEST, WRITE_COMPLETE, VERIFY_COMPLETE, REPAIR }

enum class SecureHow { CREATED, REUSED, PROMOTED }

sealed class SecureResult {
    /** A snapshot of exactly these bytes exists and passed the post-marker verification. */
    class Secured(val name: String, val how: SecureHow) : SecureResult()

    /** Not secured, by policy. Nothing was deleted or overwritten. */
    class Refused(val reason: RefuseReason) : SecureResult()

    /** Not secured, an operation failed. Treat the evidence as NOT kept. */
    class Failed(val stage: FailStage, val cause: Exception) : SecureResult()
}

class QuarantineSummary(
    val completeNames: List<String>,
    val incompleteNames: List<String>,
    val emptyNames: List<String>,
    val scannedBytes: Long
)

class CleanupPlan(val names: List<String>)

sealed class CleanupResult {
    class Deleted(val names: List<String>) : CleanupResult()

    /** What is eligible changed since the plan was shown, so nothing was deleted. */
    object PlanChanged : CleanupResult()

    class Failed(val cause: Exception, val deleted: List<String>) : CleanupResult()
}

class QuarantineStore(
    private val ops: QuarantineFileOps,
    private val root: File,
    private val limits: QuarantineLimits = QuarantineLimits()
) {
    private class Complete(val name: String, val seq: Int, val manifest: Manifest)
    private class Incomplete(val name: String, val entries: List<String>)
    private class Scan(
        val complete: List<Complete>,
        val incomplete: List<Incomplete>,
        val empty: List<String>,
        val maxSeq: Int,
        val bytes: Long
    )

    private class StageFailure(val stage: FailStage, cause: Exception) : Exception(cause)

    // ------------------------------------------------------------------ public

    @Synchronized
    fun secure(source: SourceFiles, meta: SnapshotMeta): SecureResult {
        val evidence = source.evidence()
        if (evidence.isEmpty()) return SecureResult.Refused(RefuseReason.EMPTY_SOURCE)
        if (evidence.any { it.second.size > limits.maxFileBytes }) return SecureResult.Refused(RefuseReason.TOO_LARGE)
        try {
            ensureRoot()
        } catch (e: IOException) {
            return SecureResult.Failed(FailStage.ROOT, e)
        }
        val scan = try { scanRoot() } catch (e: IOException) { return SecureResult.Failed(FailStage.SCAN, e) }

        // An empty snapshot directory holds nothing, so removing it destroys nothing. Best effort.
        for (name in scan.empty) {
            try { ops.deleteEmptyDirectory(File(root, name)) } catch (e: IOException) { /* stays; not counted */ }
        }

        val wanted = evidence.associate { (name, bytes) -> name to Pair(bytes.size.toLong(), sha256Hex(bytes)) }
        scan.complete.firstOrNull { sameEvidence(it.manifest, wanted) }?.let { return SecureResult.Secured(it.name, SecureHow.REUSED) }

        if (scan.complete.size >= limits.completeMax) return SecureResult.Refused(RefuseReason.CAP_FULL)

        for (inc in scan.incomplete) {
            if (matchesSource(inc, source)) return finalizeExisting(inc.name, source, meta, SecureHow.PROMOTED)
        }
        if (scan.incomplete.size >= limits.incompleteMax) return SecureResult.Refused(RefuseReason.ATTEMPTS_EXHAUSTED)

        val need = evidence.sumOf { blocks(it.second.size.toLong()) } + 3L * limits.blockSize
        if (scan.bytes + need > limits.budgetBytes) return SecureResult.Refused(RefuseReason.BUDGET_FULL)
        val free = try { ops.usableSpace(root) } catch (e: Exception) { return SecureResult.Failed(FailStage.SCAN, IOException(e)) }
        if (free < 2 * need + limits.spaceMargin) return SecureResult.Refused(RefuseReason.NO_SPACE)

        return createNew(scan.maxSeq + 1, source, meta)
    }

    @Synchronized
    fun summary(): QuarantineSummary {
        val s = scanRoot()
        return QuarantineSummary(s.complete.map { it.name }, s.incomplete.map { it.name }, s.empty, s.bytes)
    }

    /** What a confirmed cleanup would remove: only directories whose every evidence byte is provably redundant. */
    @Synchronized
    fun cleanupPreview(): CleanupPlan = try { CleanupPlan(eligibleNames(scanRoot())) } catch (e: IOException) { CleanupPlan(emptyList()) }

    /** Re-derives eligibility now; deletes nothing unless it is exactly the set the player was shown. */
    @Synchronized
    fun cleanupConfirmed(plan: CleanupPlan): CleanupResult {
        val scan = try { scanRoot() } catch (e: IOException) { return CleanupResult.Failed(e, emptyList()) }
        val now = eligibleNames(scan)
        if (now.toSet() != plan.names.toSet()) return CleanupResult.PlanChanged
        val deleted = ArrayList<String>()
        try {
            for (name in plan.names) {
                val dir = File(root, name)
                val entries = ops.listNames(dir)
                if (verifyComplete(dir, entries, seqOf(name) ?: -1) != null) return CleanupResult.PlanChanged // never a complete one
                for (entry in entries) ops.deleteFile(File(dir, entry))
                ops.deleteEmptyDirectory(dir)
                deleted.add(name)
            }
        } catch (e: IOException) {
            return CleanupResult.Failed(e, deleted)
        }
        return CleanupResult.Deleted(deleted)
    }

    // ------------------------------------------------------------------ creation

    private fun createNew(firstSeq: Int, source: SourceFiles, meta: SnapshotMeta): SecureResult {
        var seq = firstSeq
        var dir: File? = null
        for (attempt in 0 until limits.reserveTries) {
            val candidate = File(root, snapName(seq))
            try {
                ops.createDirectoryExclusive(candidate)
                dir = candidate
                break
            } catch (e: FileAlreadyExistsException) {
                seq++
            } catch (e: IOException) {
                return SecureResult.Failed(FailStage.RESERVE, e)
            }
        }
        if (dir == null) return SecureResult.Refused(RefuseReason.COLLISION)
        bestEffortSync(root)

        val created = ArrayList<File>()
        var evidenceVerified = false
        try {
            for ((name, bytes) in source.evidence()) {
                val f = File(dir, name)
                try {
                    ops.createFileExclusive(f, bytes)
                } catch (e: FileAlreadyExistsException) {
                    throw StageFailure(FailStage.WRITE_EVIDENCE, e) // not ours: never tracked, never deleted
                } catch (e: IOException) {
                    created.add(f) // a partial file may exist and is ours
                    throw StageFailure(FailStage.WRITE_EVIDENCE, e)
                }
                created.add(f)
                try { ops.syncFile(f) } catch (e: IOException) { throw StageFailure(FailStage.WRITE_EVIDENCE, e) }
            }
            for ((name, bytes) in source.evidence()) {
                val back = try { ops.readAll(File(dir, name), limits.maxFileBytes) } catch (e: IOException) { throw StageFailure(FailStage.VERIFY_EVIDENCE, e) }
                if (!back.contentEquals(bytes)) throw StageFailure(FailStage.VERIFY_EVIDENCE, IOException("evidence did not read back equal"))
            }
            evidenceVerified = true
        } catch (f: StageFailure) {
            if (!evidenceVerified) discardOwnPartial(dir, created)
            return SecureResult.Failed(f.stage, f.cause as Exception)
        }
        return finalizeDir(dir, seq, source, meta, SecureHow.CREATED)
    }

    /** The only place this class deletes evidence-named files: copies it created in this very call and never verified. */
    private fun discardOwnPartial(dir: File, created: List<File>) {
        for (f in created) {
            try { ops.deleteFile(f) } catch (e: IOException) { /* an orphan; accounted for by the incomplete cap */ }
        }
        try { ops.deleteEmptyDirectory(dir) } catch (e: IOException) { /* same */ }
    }

    private fun finalizeExisting(name: String, source: SourceFiles, meta: SnapshotMeta, how: SecureHow): SecureResult {
        val seq = seqOf(name) ?: return SecureResult.Failed(FailStage.SCAN, IOException("unparseable snapshot name"))
        return finalizeDir(File(root, name), seq, source, meta, how)
    }

    /** Writes whatever metadata is missing (never overwriting) and runs the post-marker verification. */
    private fun finalizeDir(dir: File, seq: Int, source: SourceFiles, meta: SnapshotMeta, how: SecureHow): SecureResult {
        var stage = FailStage.WRITE_MANIFEST
        try {
            val files = source.evidence().map { (n, b) -> ManifestFile(n, b.size.toLong(), sha256Hex(b)) }
            val rendered = Manifest.render(seq, meta, files)
            var manifestBytes = rendered
            val manifestFile = File(dir, MANIFEST_NAME)
            when (ops.kind(manifestFile)) {
                EntryKind.MISSING -> writeSynced(manifestFile, rendered)
                EntryKind.FILE -> {
                    val existing = ops.readAll(manifestFile, METADATA_MAX)
                    val parsed = Manifest.parse(existing)
                    if (parsed != null && parsed.seq == seq && sameFiles(parsed, files)) {
                        manifestBytes = existing
                    } else {
                        stage = FailStage.REPAIR
                        if (!repairMetadata(dir, MANIFEST_NAME)) return SecureResult.Refused(RefuseReason.REPAIR_EXHAUSTED)
                        stage = FailStage.WRITE_MANIFEST
                        writeSynced(manifestFile, rendered)
                    }
                }
                else -> throw IOException("manifest path is not a plain file")
            }
            stage = FailStage.WRITE_COMPLETE
            val completeFile = File(dir, COMPLETE_NAME)
            val expected = CompleteMarker.render(manifestBytes)
            when (ops.kind(completeFile)) {
                EntryKind.MISSING -> writeSynced(completeFile, expected)
                EntryKind.FILE -> {
                    if (!ops.readAll(completeFile, METADATA_MAX).contentEquals(expected)) {
                        stage = FailStage.REPAIR
                        if (!repairMetadata(dir, COMPLETE_NAME)) return SecureResult.Refused(RefuseReason.REPAIR_EXHAUSTED)
                        stage = FailStage.WRITE_COMPLETE
                        writeSynced(completeFile, expected)
                    }
                }
                else -> throw IOException("marker path is not a plain file")
            }
            stage = FailStage.VERIFY_COMPLETE
            if (verifyComplete(dir, ops.listNames(dir), seq) == null) throw IOException("marker did not verify")
            bestEffortSync(dir)
            bestEffortSync(root)
            return SecureResult.Secured(dir.name, how)
        } catch (e: IOException) {
            return SecureResult.Failed(stage, e)
        }
    }

    private fun writeSynced(f: File, bytes: ByteArray) {
        ops.createFileExclusive(f, bytes)
        ops.syncFile(f)
    }

    /**
     * Replaces an INVALID metadata file without ever losing its bytes: copy to NAME.invalid-k (exclusive), read the
     * copy back and compare, only then delete the original. Returns false when every k up to the bound is taken.
     */
    private fun repairMetadata(dir: File, name: String): Boolean {
        val original = File(dir, name)
        val bytes = ops.readAll(original, METADATA_MAX)
        for (k in 1..limits.repairMax) {
            val copy = File(dir, "$name.invalid-$k")
            if (ops.kind(copy) != EntryKind.MISSING) continue
            ops.createFileExclusive(copy, bytes)
            ops.syncFile(copy)
            if (!ops.readAll(copy, METADATA_MAX).contentEquals(bytes)) throw IOException("preserved copy did not read back equal")
            ops.deleteFile(original)
            return true
        }
        return false
    }

    // ------------------------------------------------------------------ scanning and verification

    private fun ensureRoot() {
        when (ops.kind(root)) {
            EntryKind.DIRECTORY -> Unit
            EntryKind.MISSING -> try {
                ops.createDirectoryExclusive(root)
            } catch (e: FileAlreadyExistsException) {
                if (ops.kind(root) != EntryKind.DIRECTORY) throw IOException("quarantine path is not a real directory")
            }
            else -> throw IOException("quarantine path is not a real directory")
        }
    }

    private fun scanRoot(): Scan {
        val complete = ArrayList<Complete>()
        val incomplete = ArrayList<Incomplete>()
        val empty = ArrayList<String>()
        var maxSeq = 0
        var bytes = 0L
        for (name in ops.listNames(root)) {
            val f = File(root, name)
            val kind = ops.kind(f)
            val seq = seqOf(name)
            if (seq != null && seq > maxSeq) maxSeq = seq
            if (seq == null || kind != EntryKind.DIRECTORY) {
                bytes += if (kind == EntryKind.FILE) blocks(sizeOrZero(f)).coerceAtLeast(limits.blockSize.toLong()) else limits.blockSize.toLong()
                continue
            }
            bytes += limits.blockSize
            val entries = ops.listNames(f)
            for (e in entries) {
                val ef = File(f, e)
                bytes += if (ops.kind(ef) == EntryKind.FILE) blocks(sizeOrZero(ef)).coerceAtLeast(limits.blockSize.toLong()) else limits.blockSize.toLong()
            }
            when {
                entries.isEmpty() -> empty.add(name)
                else -> {
                    val manifest = verifyComplete(f, entries, seq)
                    if (manifest != null) complete.add(Complete(name, seq, manifest)) else incomplete.add(Incomplete(name, entries))
                }
            }
        }
        return Scan(complete, incomplete, empty, maxSeq, bytes)
    }

    /**
     * The manifest if [dir] is a complete, fully verified snapshot; null if it positively is not (a missing or
     * non-plain file, a marker or hash that does not match). A genuine I/O error is NOT "not complete": it propagates,
     * so a transient read failure can never make a verified snapshot look incomplete and let the caps be exceeded.
     */
    private fun verifyComplete(dir: File, entries: List<String>, seq: Int): Manifest? {
        if (COMPLETE_NAME !in entries || MANIFEST_NAME !in entries) return null
        val manifestFile = File(dir, MANIFEST_NAME)
        val completeFile = File(dir, COMPLETE_NAME)
        if (ops.kind(manifestFile) != EntryKind.FILE || ops.kind(completeFile) != EntryKind.FILE) return null
        val manifestBytes = ops.readAll(manifestFile, METADATA_MAX)
        val marker = ops.readAll(completeFile, METADATA_MAX)
        if (!CompleteMarker.matches(marker, manifestBytes)) return null
        val manifest = Manifest.parse(manifestBytes) ?: return null
        if (manifest.seq != seq) return null
        for (name in EVIDENCE_NAMES) {
            val listed = manifest.fileNamed(name)
            val present = name in entries
            if (listed == null) {
                if (present) return null
                continue
            }
            if (!present || ops.kind(File(dir, name)) != EntryKind.FILE) return null
            val data = ops.readAll(File(dir, name), limits.maxFileBytes)
            if (data.size.toLong() != listed.length || sha256Hex(data) != listed.sha256) return null
        }
        return manifest
    }

    private fun sameEvidence(m: Manifest, wanted: Map<String, Pair<Long, String>>): Boolean = EVIDENCE_NAMES.all { name ->
        val a = m.fileNamed(name)
        val b = wanted[name]
        if (a == null || b == null) a == null && b == null else a.length == b.first && a.sha256 == b.second
    }

    private fun sameFiles(m: Manifest, files: List<ManifestFile>): Boolean =
        m.files.size == files.size && files.all { f -> m.fileNamed(f.name)?.let { it.length == f.length && it.sha256 == f.sha256 } == true }

    /** The incomplete directory holds exactly the source's evidence files, byte for byte, plus only our metadata names. */
    private fun matchesSource(inc: Incomplete, source: SourceFiles): Boolean {
        if (inc.entries.any { !isKnownName(it) }) return false
        val present = inc.entries.filter { it in EVIDENCE_NAMES }.toSet()
        val wanted = source.evidence()
        if (present != wanted.map { it.first }.toSet()) return false
        return try {
            wanted.all { (name, bytes) ->
                val f = File(File(root, inc.name), name)
                ops.kind(f) == EntryKind.FILE && ops.readAll(f, limits.maxFileBytes).contentEquals(bytes)
            }
        } catch (e: IOException) {
            false
        }
    }

    // ------------------------------------------------------------------ cleanup eligibility (positive proof)

    private fun eligibleNames(scan: Scan): List<String> = scan.incomplete.filter { provablyRedundant(it, scan.complete) }.map { it.name }

    /**
     * True only when every entry is a plain file with a known name and every evidence file is empty, or is a byte
     * prefix of (or equal to) the same-named file in some COMPLETE snapshot: it then holds nothing the complete
     * snapshot does not. Absence of a marker is never, by itself, a reason to delete.
     */
    private fun provablyRedundant(inc: Incomplete, completes: List<Complete>): Boolean {
        if (inc.entries.any { !isKnownName(it) }) return false
        val dir = File(root, inc.name)
        try {
            for (entry in inc.entries) {
                if (ops.kind(File(dir, entry)) != EntryKind.FILE) return false
                if (entry !in EVIDENCE_NAMES) continue
                val bytes = ops.readAll(File(dir, entry), limits.maxFileBytes)
                if (bytes.isEmpty()) continue
                val covered = completes.any { c ->
                    c.manifest.fileNamed(entry) != null && startsWith(ops.readAll(File(File(root, c.name), entry), limits.maxFileBytes), bytes)
                }
                if (!covered) return false
            }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    private fun startsWith(whole: ByteArray, prefix: ByteArray): Boolean {
        if (prefix.size > whole.size) return false
        for (i in prefix.indices) if (whole[i] != prefix[i]) return false
        return true
    }

    // ------------------------------------------------------------------ helpers

    private fun isKnownName(n: String): Boolean =
        n in EVIDENCE_NAMES || n == MANIFEST_NAME || n == COMPLETE_NAME ||
            INVALID_COPY.matches(n)

    private fun sizeOrZero(f: File): Long = try { ops.size(f) } catch (e: IOException) { 0L }

    private fun blocks(len: Long): Long {
        if (len <= 0) return 0
        val b = limits.blockSize.toLong()
        return (len + b - 1) / b * b
    }

    private fun bestEffortSync(dir: File) {
        try { ops.syncDirectory(dir) } catch (e: IOException) { /* F5: directory sync is best effort and no durability claim is made */ }
    }

    private fun seqOf(name: String): Int? = SNAP.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()

    private fun snapName(seq: Int): String = "snap-" + seq.toString().padStart(4, '0')

    companion object {
        private val SNAP = Regex("snap-([0-9]{4,9})")
        private val INVALID_COPY = Regex("(MANIFEST|COMPLETE)\\.invalid-[0-9]{1,3}")
        private const val METADATA_MAX = 65536
    }
}
