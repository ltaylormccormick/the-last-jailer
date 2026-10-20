package com.thelastjailer.app.recovery

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

enum class Kind { ABSENT, VALID, CORRUPT, FUTURE }

/** Fixtures shared by the recovery-store tests. */
object Rb {
    const val ROOT = "/sim/recovery"
    const val SLOT = 1
    const val A = "$ROOT/slot_1/a.bundle"
    const val B = "$ROOT/slot_1/b.bundle"

    // opKind values used only by tests; the real meanings are G3's to define.
    const val START_AGAIN = 1
    const val OVERWRITE = 2
    const val RESTORE_SWAP = 3

    fun payload(seed: Int, size: Int = 200): ByteArray = ByteArray(size) { (seed * 31 + it * 7).toByte() }

    fun validBytes(
        generation: Long, seed: Int, slot: Int = SLOT, opKind: Int = START_AGAIN, schema: Int = 1, idByte: Int = generation.toInt()
    ): ByteArray = RecoveryCodec.encode(
        BundleMeta(slot, generation, BundleId(ByteArray(16) { idByte.toByte() }), opKind, schema), payload(seed)
    )

    /** A bundle with one byte flipped: well-formed length, wrong digest. (Not the byte or mask SimFs uses for CORRUPT_READ.) */
    fun corruptBytes(generation: Long = 1, seed: Int = 99): ByteArray =
        validBytes(generation, seed).also { it[it.size / 3] = (it[it.size / 3].toInt() xor 0xC3).toByte() }

    /** A file a newer format wrote: right magic, version 2, content this build cannot interpret. */
    fun futureBytes(version: Int = 2, size: Int = 300): ByteArray {
        val out = ByteArray(size) { (it * 13 + 5).toByte() }
        System.arraycopy(RecoveryFormat.MAGIC, 0, out, 0, 8)
        ByteBuffer.wrap(out).putShort(8, version.toShort())
        return out
    }

    /** Builds raw version-1 bytes with any field overridden, and a correct digest unless asked otherwise. */
    fun raw(
        slot: Int = SLOT, generation: Long = 1, id: Int = 7, opKind: Int = 1, schema: Int = 1,
        payload: ByteArray = payload(1), reserved: Int = 0, version: Int = 1,
        bodyLengthDelta: Int = 0, payloadLengthDelta: Int = 0, goodDigest: Boolean = true, trailing: Int = 0
    ): ByteArray {
        val bodyLength = RecoveryFormat.BODY_FIXED_BYTES + payload.size
        val total = RecoveryFormat.HEADER_BYTES + bodyLength + RecoveryFormat.DIGEST_BYTES
        val buf = ByteBuffer.allocate(total)
        buf.put(RecoveryFormat.MAGIC)
        buf.putShort(version.toShort()).putShort(reserved.toShort()).putInt(bodyLength + bodyLengthDelta)
        buf.putInt(slot).putLong(generation).put(ByteArray(16) { id.toByte() })
        buf.putShort(opKind.toShort()).putShort(schema.toShort()).putInt(payload.size + payloadLengthDelta)
        buf.put(payload)
        val array = buf.array()
        val digest = RecoveryCodec.sha256(array, 0, total - RecoveryFormat.DIGEST_BYTES)
        if (!goodDigest) digest[0] = (digest[0].toInt() xor 1).toByte()
        System.arraycopy(digest, 0, array, total - RecoveryFormat.DIGEST_BYTES, RecoveryFormat.DIGEST_BYTES)
        return if (trailing > 0) array + ByteArray(trailing) { 9 } else array
    }

    fun world(withDirs: Boolean = true): SimFs {
        val fs = SimFs()
        fs.seedDir("/")
        fs.seedDir("/sim")
        if (withDirs) { fs.seedDir(ROOT); fs.seedDir("$ROOT/slot_1") }
        return fs
    }

    /** A store whose ids are 100, 101, ... so they never collide with the ids [validBytes] seeds. */
    fun store(fs: RecoveryFileOps, max: Int = RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES): RecoveryBundleStore {
        val next = AtomicInteger(100)
        return RecoveryBundleStore(File(ROOT), fs, { val n = next.getAndIncrement(); ByteArray(16) { n.toByte() } }, max)
    }

    fun seed(fs: SimFs, a: Kind, b: Kind, genA: Long = 5, genB: Long = 4) {
        seedOne(fs, A, a, genA, 1)
        seedOne(fs, B, b, genB, 2)
    }

    private fun seedOne(fs: SimFs, path: String, kind: Kind, gen: Long, seed: Int) {
        when (kind) {
            Kind.ABSENT -> Unit
            Kind.VALID -> fs.seedFile(path, validBytes(gen, seed))
            Kind.CORRUPT -> fs.seedFile(path, corruptBytes(gen, seed))
            Kind.FUTURE -> fs.seedFile(path, futureBytes())
        }
    }

    fun request(seed: Int, opKind: Int = START_AGAIN, schema: Int = 1, size: Int = 200, slot: Int = SLOT) =
        BundleRequest(slot, opKind, schema, payload(seed, size))

    /** The two buffer files as bytes (null = absent), for "nothing changed" comparisons. */
    fun disk(fs: SimFs): List<ByteArray?> = listOf(fs.bytesOf(A), fs.bytesOf(B))

    fun sameDisk(x: List<ByteArray?>, y: List<ByteArray?>): Boolean =
        x.size == y.size && x.indices.all { i ->
            val p = x[i]; val q = y[i]
            (p == null && q == null) || (p != null && q != null && p.contentEquals(q))
        }

    fun statusKind(s: BufferStatus): String = when (s) {
        is BufferStatus.Absent -> "ABSENT"
        is BufferStatus.Valid -> "VALID"
        is BufferStatus.Corrupt -> "CORRUPT"
        is BufferStatus.Future -> "FUTURE"
        is BufferStatus.Unreadable -> "UNREADABLE"
    }

    fun kinds(i: SlotInspection) = statusKind(i.a.status) + "+" + statusKind(i.b.status)

    fun validCount(i: SlotInspection): Int = i.reports.count { it.status is BufferStatus.Valid }

    /**
     * What is really on disk, read from a private copy so that no fault fires and no operation is counted
     * against the filesystem under test.
     */
    fun truth(fs: SimFs): SlotInspection = store(fs.duplicate()).inspect(SLOT)
}
