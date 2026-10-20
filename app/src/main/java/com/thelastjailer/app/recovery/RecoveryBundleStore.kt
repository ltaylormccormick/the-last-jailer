package com.thelastjailer.app.recovery

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Two atomic bundle files per slot (`a.bundle`, `b.bundle`) under `<root>/slot_<n>/`. DORMANT (G2): no production
 * code calls this, it never reads or writes the live save, and it never restores anything. It only stores recovery
 * bundles and tells the truth about what is on disk.
 *
 * Because it lives in plain files, it can be found and read when the live `SharedPreferences` file is unreadable,
 * missing or empty ([slotsOnDisk], [inspect]).
 *
 * ## What a caller can rely on
 *
 *  * **I1 — the sole known-good copy is never evicted.** A write only ever replaces a buffer when another VALID
 *    bundle stays on disk, or when no VALID bundle existed. Otherwise it refuses ([PutResult.Blocked]).
 *  * **I2 — a newer-format (FUTURE) file is never touched.** Not overwritten, deleted, renamed or rewritten.
 *  * **I3 — generations strictly increase** among VALID bundles; equal generations with different contents are
 *    never resolved by guessing.
 *  * **I4 — nothing outside the slot's two bundle files and their two temp files is created, changed or deleted.**
 *  * **I5 — "stored" means verified.** [PutResult.Stored] is returned only after the new file was read back from
 *    disk and fully re-verified, and the other buffer was found unchanged.
 *
 * Nothing here assumes a failed operation left the disk as it was. After any failure the store rescans both
 * buffers and reports what it found: [PutResult.Failed] only when the rescan proves nothing changed, otherwise
 * [PutResult.Uncertain]. The caller must never blindly retry an uncertain outcome; it should read [PutResult.post]
 * (or call [inspect]) first.
 *
 * What is proved where: the JVM tests prove all of the above against a *model* of a filesystem (including adversarial
 * crash images). They cannot prove real Android storage honours fsync and rename the way the model assumes; that
 * is a device question. See qa/G2_RECOVERY_STORE.md.
 *
 * One writer per slot at a time (an in-process lock shared by every store over the same directory). The app is a
 * single process; a second process writing the same files is outside this contract.
 */
class RecoveryBundleStore(
    private val root: File,
    private val ops: RecoveryFileOps = JavaRecoveryFileOps(),
    private val newBundleId: () -> ByteArray = ::randomBundleId,
    val maxFileBytes: Int = RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES
) {
    init {
        require(maxFileBytes >= RecoveryFormat.OVERHEAD_BYTES && maxFileBytes < Int.MAX_VALUE) {
            "maxFileBytes must hold at least an empty bundle"
        }
    }

    /** Largest payload this store will accept. */
    val maxPayloadBytes: Int get() = maxFileBytes - RecoveryFormat.OVERHEAD_BYTES

    /** Slots that have a recovery directory on disk. Needs nothing from the live save. */
    fun slotsOnDisk(): List<Int> = try {
        ops.listNames(root).mapNotNull { SLOT_DIR.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.sorted()
    } catch (e: Exception) {
        emptyList()
    }

    /** Reads and classifies both buffers of [slot]. Read-only. */
    fun inspect(slot: Int): SlotInspection {
        require(slot > 0) { "slot must be positive" }
        return synchronized(lockFor(slot)) { inspectLocked(slot) }
    }

    /**
     * Stores [request] in the buffer that [BufferSelection] allows, or refuses. Never throws for a filesystem
     * problem: every outcome is a [PutResult].
     */
    fun put(request: BundleRequest): PutResult {
        if (request.slot <= 0 || request.opKind !in 0..RecoveryFormat.MAX_U16 ||
            request.payloadSchema !in 0..RecoveryFormat.MAX_U16
        ) return PutResult.Blocked(BlockReason.INVALID_REQUEST, null)
        if (request.payload.size > maxPayloadBytes) return PutResult.Blocked(BlockReason.PAYLOAD_TOO_LARGE, null)
        return synchronized(lockFor(request.slot)) { putLocked(request) }
    }

    private fun putLocked(request: BundleRequest): PutResult {
        val slot = request.slot
        val pre = inspectLocked(slot)

        // 1. Nothing to do if this exact meaning is already stored.
        val key = RecoveryCodec.semanticKey(slot, request.opKind, request.payloadSchema, request.payload)
        val existing = pre.reports.firstOrNull {
            val s = it.status
            s is BufferStatus.Valid && MessageDigest.isEqual(s.bundle.semanticKey, key)
        }
        if (existing != null) {
            val s = existing.status as BufferStatus.Valid
            val newest = pre.newestValid()
            val isNewest = newest is Newest.One && newest.report.buffer == existing.buffer
            return PutResult.SkippedIdentical(existing.buffer, s.bundle.meta.generation, isNewest, pre)
        }

        // 2. Choose where the write may go, or refuse before touching anything.
        val target = when (val choice = BufferSelection.choose(pre.a, pre.b)) {
            is BufferChoice.Refuse -> return PutResult.Blocked(choice.reason, pre)
            is BufferChoice.Write -> choice.buffer
        }
        val highest = pre.reports.mapNotNull { (it.status as? BufferStatus.Valid)?.bundle?.meta?.generation }.maxOrNull()
        if (highest == RecoveryFormat.MAX_GENERATION) return PutResult.Blocked(BlockReason.GENERATION_EXHAUSTED, pre)
        val generation = (highest ?: 0L) + 1L

        val id = BundleId(newBundleId())
        val bytes = RecoveryCodec.encode(
            BundleMeta(slot, generation, id, request.opKind, request.payloadSchema), request.payload
        )
        val ourDigest = bytes.copyOfRange(bytes.size - RecoveryFormat.DIGEST_BYTES, bytes.size)

        val dir = slotDir(slot)
        val tmp = File(dir, target.tempName)
        val dest = File(dir, target.fileName)
        var stage = PutStage.PREPARE
        try {
            var allSynced = true
            if (ops.ensureDirectory(root)) allSynced = isSynced(ops.syncDirectory(root.parentFile ?: root)) && allSynced
            if (ops.ensureDirectory(dir)) allSynced = isSynced(ops.syncDirectory(root)) && allSynced

            stage = PutStage.WRITE_TEMP
            ops.writeFile(tmp, bytes)
            stage = PutStage.SYNC_TEMP
            ops.syncFile(tmp)
            stage = PutStage.RENAME
            ops.rename(tmp, dest)
            stage = PutStage.SYNC_DIRECTORY
            allSynced = isSynced(ops.syncDirectory(dir)) && allSynced
            stage = PutStage.READBACK
            val back = readBuffer(slot, target)
            val backStatus = back.status
            check(backStatus is BufferStatus.Valid && backStatus.bundle.meta.bundleId == id &&
                MessageDigest.isEqual(backStatus.bundle.fileDigest, ourDigest)) { "readback did not match" }
            stage = PutStage.VERIFY
            val post = inspectLocked(slot)
            check(post.report(target.other()).sameDiskStateAs(pre.report(target.other()))) { "other buffer changed" }
            check(post.report(target).sameDiskStateAs(back)) { "target changed after readback" }
            return PutResult.Stored(target, generation, id, dirSynced = allSynced, post = post)
        } catch (e: Exception) {
            return classifyFailure(slot, stage, pre, target, id, tmp)
        }
    }

    /** True only when the directory entry was really synced; UNSUPPORTED reports as not synced. */
    private fun isSynced(result: DirSync): Boolean = result == DirSync.SYNCED

    /**
     * After a failure, decide honestly. The disk is re-read; nothing is assumed. [PutResult.Failed] requires proof that
     * both buffers are exactly as they were AND that the failure happened before the rename could have taken effect.
     */
    private fun classifyFailure(
        slot: Int, stage: PutStage, pre: SlotInspection, target: Buffer, id: BundleId, tmp: File
    ): PutResult {
        val post = try { inspectLocked(slot) } catch (e: Exception) { null }
        try { ops.delete(tmp) } catch (e: Exception) { /* best effort: a stray temp file is never trusted */ }
        if (post == null) return PutResult.Uncertain(stage, newBundleVisible = false, post = null)
        val unchanged = post.a.sameDiskStateAs(pre.a) && post.b.sameDiskStateAs(pre.b)
        val beforeRename = stage == PutStage.PREPARE || stage == PutStage.WRITE_TEMP ||
            stage == PutStage.SYNC_TEMP || stage == PutStage.RENAME
        if (unchanged && beforeRename) return PutResult.Failed(stage, post)
        val status = post.report(target).status
        val visible = status is BufferStatus.Valid && status.bundle.meta.bundleId == id
        return PutResult.Uncertain(stage, visible, post)
    }

    private fun inspectLocked(slot: Int): SlotInspection =
        SlotInspection(slot, readBuffer(slot, Buffer.A), readBuffer(slot, Buffer.B))

    /**
     * Reads and classifies one buffer. CORRUPT is the only verdict that lets a write replace an existing file, so it
     * is never trusted from a single read: it is confirmed by a second read that must agree byte for byte. A read
     * that disagrees with itself (a transient I/O fault, not damage) is reported UNREADABLE, which fails closed.
     */
    private fun readBuffer(slot: Int, buffer: Buffer): BufferReport {
        val first = readOnce(slot, buffer)
        if (first.status !is BufferStatus.Corrupt) return first
        val second = readOnce(slot, buffer)
        return if (second.status is BufferStatus.Corrupt && second.sameDiskStateAs(first)) first
        else BufferReport(buffer, BufferStatus.Unreadable, null)
    }

    private fun readOnce(slot: Int, buffer: Buffer): BufferReport {
        val file = File(slotDir(slot), buffer.fileName)
        val read = try {
            ops.read(file, maxFileBytes + 1)
        } catch (e: Exception) {
            return BufferReport(buffer, BufferStatus.Unreadable, null)
        }
        return when (read) {
            is FileRead.Missing -> BufferReport(buffer, BufferStatus.Absent, null)
            is FileRead.Data -> {
                val fingerprint = Fingerprint(read.totalLength, RecoveryCodec.sha256(read.bytes, 0, read.bytes.size))
                val status = when (val d = RecoveryCodec.decode(read.bytes, read.totalLength, slot, maxFileBytes)) {
                    is DecodeResult.Valid -> BufferStatus.Valid(d.bundle)
                    is DecodeResult.Corrupt -> BufferStatus.Corrupt(d.reason)
                    is DecodeResult.Future -> BufferStatus.Future(d.version)
                }
                BufferReport(buffer, status, fingerprint)
            }
        }
    }

    private fun slotDir(slot: Int): File = File(root, "slot_$slot")

    private fun lockFor(slot: Int): Any =
        LOCKS.computeIfAbsent("${root.absoluteFile.normalize().path}#$slot") { Any() }

    private companion object {
        val SLOT_DIR = Regex("slot_([1-9][0-9]{0,8})")
        val LOCKS = ConcurrentHashMap<String, Any>()
        val RANDOM = SecureRandom()

        fun randomBundleId(): ByteArray = ByteArray(RecoveryFormat.BUNDLE_ID_BYTES).also { RANDOM.nextBytes(it) }
    }
}

enum class Buffer(val fileName: String, val tempName: String) {
    A("a.bundle", "a.tmp"),
    B("b.bundle", "b.tmp");

    fun other(): Buffer = if (this == A) B else A
}

/** Length and SHA-256 of what was read from a file. Used only to tell whether a file changed; never logged. */
class Fingerprint(val length: Long, private val sha: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is Fingerprint && length == other.length && MessageDigest.isEqual(sha, other.sha)
    override fun hashCode(): Int = length.hashCode() * 31 + sha.contentHashCode()
}

sealed interface BufferStatus {
    object Absent : BufferStatus
    class Valid(val bundle: RecoveryBundle) : BufferStatus
    class Corrupt(val reason: CorruptReason) : BufferStatus
    class Future(val version: Int) : BufferStatus

    /** The file exists (or may exist) but could not be read. Treated as untouchable: the store fails closed. */
    object Unreadable : BufferStatus
}

class BufferReport(val buffer: Buffer, val status: BufferStatus, val fingerprint: Fingerprint?) {
    /** True only when both reports prove the same bytes, or both prove the file is absent. */
    fun sameDiskStateAs(other: BufferReport): Boolean {
        if (status === BufferStatus.Absent && other.status === BufferStatus.Absent) return true
        val f = fingerprint ?: return false
        return f == other.fingerprint
    }
}

sealed interface Newest {
    object None : Newest
    class One(val report: BufferReport) : Newest

    /** Two VALID bundles share a generation but differ. Nothing may pick a winner. */
    class Ambiguous(val a: BufferReport, val b: BufferReport) : Newest
}

class SlotInspection(val slot: Int, val a: BufferReport, val b: BufferReport) {
    val reports: List<BufferReport> get() = listOf(a, b)
    fun report(buffer: Buffer): BufferReport = if (buffer == Buffer.A) a else b

    fun newestValid(): Newest {
        val valid = reports.filter { it.status is BufferStatus.Valid }
        if (valid.isEmpty()) return Newest.None
        if (valid.size == 1) return Newest.One(valid[0])
        val x = (valid[0].status as BufferStatus.Valid).bundle
        val y = (valid[1].status as BufferStatus.Valid).bundle
        return when {
            x.meta.generation > y.meta.generation -> Newest.One(valid[0])
            y.meta.generation > x.meta.generation -> Newest.One(valid[1])
            MessageDigest.isEqual(x.fileDigest, y.fileDigest) -> Newest.One(valid[0]) // identical copies
            else -> Newest.Ambiguous(valid[0], valid[1])
        }
    }
}

class BundleRequest(val slot: Int, val opKind: Int, val payloadSchema: Int, val payload: ByteArray)

enum class BlockReason {
    INVALID_REQUEST,
    PAYLOAD_TOO_LARGE,

    /** One VALID bundle is the only known-good copy and the other buffer cannot be used. */
    SOLE_KNOWN_GOOD_COPY,

    /** Both buffers hold newer-format files. */
    NO_WRITABLE_BUFFER,
    GENERATION_EXHAUSTED,
    AMBIGUOUS_GENERATIONS,

    /** A buffer could not be read, so the slot's state cannot be established. Fail closed. */
    UNREADABLE_BUFFER
}

enum class PutStage { PREPARE, WRITE_TEMP, SYNC_TEMP, RENAME, SYNC_DIRECTORY, READBACK, VERIFY }

sealed interface PutResult {
    /** Verified: written, read back, fully re-checked. [dirSynced] false means rename durability is not guaranteed. */
    class Stored(
        val buffer: Buffer, val generation: Long, val bundleId: BundleId, val dirSynced: Boolean, val post: SlotInspection
    ) : PutResult

    /** The same meaning is already stored; nothing was written. [isNewest] says whether it is the newest generation. */
    class SkippedIdentical(
        val buffer: Buffer, val generation: Long, val isNewest: Boolean, val post: SlotInspection
    ) : PutResult

    /** Nothing was attempted and nothing on disk changed. [post] is null when the request was rejected unread. */
    class Blocked(val reason: BlockReason, val post: SlotInspection?) : PutResult

    /** The write failed and a rescan PROVED both buffers are exactly as before. */
    class Failed(val stage: PutStage, val post: SlotInspection) : PutResult

    /**
     * The disk may have changed. [post] is a fresh rescan (null if even that failed); [newBundleVisible] says whether
     * the new bundle is now a VALID file on disk. Do not retry blindly: read [post] or [RecoveryBundleStore.inspect].
     */
    class Uncertain(val stage: PutStage, val newBundleVisible: Boolean, val post: SlotInspection?) : PutResult
}

sealed interface BufferChoice {
    class Write(val buffer: Buffer) : BufferChoice
    class Refuse(val reason: BlockReason) : BufferChoice
}

/**
 * Which buffer a new bundle may go into, as a pure function of the two buffers' states. Total over all
 * 5 x 5 = 25 combinations:
 *
 * ```
 *  1. any Unreadable                  -> refuse UNREADABLE_BUFFER (state cannot be established)
 *  2. any Absent                      -> write the Absent one (A if both): touches no existing file
 *  3. Valid + Valid, generations differ -> replace the LOWER generation (the other stays Valid)
 *     Valid + Valid, identical copies   -> replace B (no distinct state is lost)
 *     Valid + Valid, same gen, differ   -> refuse AMBIGUOUS_GENERATIONS
 *  4. Valid + Corrupt (either order)  -> replace the Corrupt one
 *     Future + Corrupt (either order) -> replace the Corrupt one
 *     Corrupt + Corrupt               -> replace A (nothing known-good exists to lose)
 *  5. Valid + Future (either order)   -> refuse SOLE_KNOWN_GOOD_COPY
 *     Future + Future                 -> refuse NO_WRITABLE_BUFFER
 * ```
 *
 * A "Corrupt" buffer is replaceable because it holds nothing recoverable. A damaged file that merely looks corrupt
 * but was really written by a newer format cannot be told apart from garbage; that residual risk is documented.
 */
object BufferSelection {
    fun choose(a: BufferReport, b: BufferReport): BufferChoice {
        val sa = a.status
        val sb = b.status
        if (sa === BufferStatus.Unreadable || sb === BufferStatus.Unreadable) return BufferChoice.Refuse(BlockReason.UNREADABLE_BUFFER)
        if (sa === BufferStatus.Absent) return BufferChoice.Write(Buffer.A)
        if (sb === BufferStatus.Absent) return BufferChoice.Write(Buffer.B)
        if (sa is BufferStatus.Valid && sb is BufferStatus.Valid) {
            val ga = sa.bundle.meta.generation
            val gb = sb.bundle.meta.generation
            return when {
                ga < gb -> BufferChoice.Write(Buffer.A)
                gb < ga -> BufferChoice.Write(Buffer.B)
                MessageDigest.isEqual(sa.bundle.fileDigest, sb.bundle.fileDigest) -> BufferChoice.Write(Buffer.B)
                else -> BufferChoice.Refuse(BlockReason.AMBIGUOUS_GENERATIONS)
            }
        }
        if (sa is BufferStatus.Corrupt && sb is BufferStatus.Corrupt) return BufferChoice.Write(Buffer.A)
        if (sa is BufferStatus.Corrupt) return BufferChoice.Write(Buffer.A)
        if (sb is BufferStatus.Corrupt) return BufferChoice.Write(Buffer.B)
        // Remaining: Valid+Future (either order) and Future+Future.
        return if (sa is BufferStatus.Future && sb is BufferStatus.Future) {
            BufferChoice.Refuse(BlockReason.NO_WRITABLE_BUFFER)
        } else {
            BufferChoice.Refuse(BlockReason.SOLE_KNOWN_GOOD_COPY)
        }
    }
}
