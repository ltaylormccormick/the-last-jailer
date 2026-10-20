package com.thelastjailer.app.recovery

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The on-disk container for one recovery bundle (G2, dormant: nothing in the app calls this yet).
 *
 * Layout, version 1, all integers big-endian:
 *
 * ```
 *  0  magic          8 bytes  "TLJRBNDL"
 *  8  formatVersion  u16      1
 * 10  reserved       u16      must be 0
 * 12  bodyLength     u32      bytes between the header and the digest
 * 16  slot           i32      must equal the slot whose directory holds the file
 * 20  generation     i64      1..Long.MAX_VALUE
 * 28  bundleId       16 bytes random, unique per bundle written
 * 44  opKind         u16      opaque to this layer (G3 assigns meanings)
 * 46  payloadSchema  u16      opaque to this layer (G3 assigns meanings)
 * 48  payloadLength  u32      must equal bodyLength - 36
 * 52  payload        n bytes  opaque
 *  .  sha256         32 bytes over every byte before it
 * ```
 *
 * The container never interprets the payload. [payloadSchema] is the payload's own version, so the state, Back
 * snapshot and trophy data G3 needs can evolve without a new container format. Anything the container itself must
 * add later needs a new [RecoveryFormat.SUPPORTED_VERSION]; an older build then classifies the file as
 * [DecodeResult.Future] and leaves it alone, which is the whole point of the version field.
 *
 * Pure JVM: no Android classes, so the whole package is unit-testable on the JVM.
 */
object RecoveryFormat {
    const val MAGIC_TEXT = "TLJRBNDL"
    const val SUPPORTED_VERSION = 1
    const val HEADER_BYTES = 16
    const val DIGEST_BYTES = 32
    const val BUNDLE_ID_BYTES = 16

    /** slot(4) + generation(8) + bundleId(16) + opKind(2) + payloadSchema(2) + payloadLength(4). */
    const val BODY_FIXED_BYTES = 36

    /** Bytes a bundle costs on top of its payload. */
    const val OVERHEAD_BYTES = HEADER_BYTES + BODY_FIXED_BYTES + DIGEST_BYTES

    /** Magic plus version: all that is needed to recognise a file written by a newer format. */
    const val PEEK_BYTES = 10

    /**
     * PROVISIONAL ceiling on one bundle file. Not an accepted maximum: see qa/G2_RECOVERY_STORE.md for what was
     * measured. A file over the ceiling is classified CORRUPT (never trusted, never read in full).
     */
    const val PROVISIONAL_MAX_FILE_BYTES = 256 * 1024

    const val MIN_GENERATION = 1L
    const val MAX_GENERATION = Long.MAX_VALUE
    const val MAX_U16 = 0xFFFF

    internal val MAGIC: ByteArray = MAGIC_TEXT.toByteArray(Charsets.US_ASCII)
}

/** 16 random bytes naming one bundle write. Its text form is deliberately uninformative. */
class BundleId(bytes: ByteArray) {
    private val value: ByteArray = bytes.copyOf()

    init {
        require(value.size == RecoveryFormat.BUNDLE_ID_BYTES) { "a bundle id is exactly 16 bytes" }
    }

    fun toByteArray(): ByteArray = value.copyOf()
    override fun equals(other: Any?): Boolean = other is BundleId && value.contentEquals(other.value)
    override fun hashCode(): Int = value.contentHashCode()
    override fun toString(): String = "BundleId"
}

class BundleMeta(
    val slot: Int,
    val generation: Long,
    val bundleId: BundleId,
    val opKind: Int,
    val payloadSchema: Int
)

/** A fully verified bundle. [fileDigest] is the SHA-256 trailer, which covers the whole file. */
class RecoveryBundle(
    val meta: BundleMeta,
    val payload: ByteArray,
    val fileDigest: ByteArray
) {
    /** What this bundle means, ignoring which write produced it. See [RecoveryCodec.semanticKey]. */
    val semanticKey: ByteArray by lazy {
        RecoveryCodec.semanticKey(meta.slot, meta.opKind, meta.payloadSchema, payload)
    }
}

enum class CorruptReason {
    TOO_SHORT,
    BAD_MAGIC,
    UNSUPPORTED_VERSION,
    HEADER_RESERVED,
    OVERSIZE,
    LENGTH_MISMATCH,
    BODY_TOO_SHORT,
    DIGEST_MISMATCH,
    BAD_GENERATION,
    WRONG_SLOT,
    PAYLOAD_LENGTH_MISMATCH
}

sealed interface DecodeResult {
    class Valid(val bundle: RecoveryBundle) : DecodeResult
    class Corrupt(val reason: CorruptReason) : DecodeResult

    /** Written by a newer format. Recognised from the first [RecoveryFormat.PEEK_BYTES] bytes alone. */
    class Future(val version: Int) : DecodeResult
}

object RecoveryCodec {

    fun encode(meta: BundleMeta, payload: ByteArray): ByteArray {
        require(meta.slot > 0) { "slot must be positive" }
        require(meta.generation >= RecoveryFormat.MIN_GENERATION) { "generation must be at least 1" }
        require(meta.opKind in 0..RecoveryFormat.MAX_U16) { "opKind must fit in 16 bits" }
        require(meta.payloadSchema in 0..RecoveryFormat.MAX_U16) { "payloadSchema must fit in 16 bits" }
        require(payload.size <= Int.MAX_VALUE - RecoveryFormat.OVERHEAD_BYTES) { "payload too large" }
        val bodyLength = RecoveryFormat.BODY_FIXED_BYTES + payload.size
        val total = RecoveryFormat.HEADER_BYTES + bodyLength + RecoveryFormat.DIGEST_BYTES
        val buf = ByteBuffer.allocate(total)
        buf.put(RecoveryFormat.MAGIC)
        buf.putShort(RecoveryFormat.SUPPORTED_VERSION.toShort())
        buf.putShort(0)
        buf.putInt(bodyLength)
        buf.putInt(meta.slot)
        buf.putLong(meta.generation)
        buf.put(meta.bundleId.toByteArray())
        buf.putShort(meta.opKind.toShort())
        buf.putShort(meta.payloadSchema.toShort())
        buf.putInt(payload.size)
        buf.put(payload)
        val array = buf.array()
        buf.put(sha256(array, 0, total - RecoveryFormat.DIGEST_BYTES))
        return array
    }

    /**
     * Classifies [bytes], which hold the first `bytes.size` bytes of a file that is [totalLength] bytes long
     * (the caller reads at most `maxFileBytes + 1`, so an oversize file is never pulled into memory).
     *
     * The order matters: a newer-format file is recognised from the header alone, before any size or digest
     * check, so a future file that is larger than this build's ceiling is still FUTURE and never CORRUPT.
     */
    fun decode(bytes: ByteArray, totalLength: Long, expectedSlot: Int, maxFileBytes: Int): DecodeResult {
        if (bytes.size < RecoveryFormat.PEEK_BYTES) return DecodeResult.Corrupt(CorruptReason.TOO_SHORT)
        for (i in RecoveryFormat.MAGIC.indices) {
            if (bytes[i] != RecoveryFormat.MAGIC[i]) return DecodeResult.Corrupt(CorruptReason.BAD_MAGIC)
        }
        val head = ByteBuffer.wrap(bytes)
        val version = head.getShort(8).toInt() and 0xFFFF
        if (version > RecoveryFormat.SUPPORTED_VERSION) return DecodeResult.Future(version)
        if (version != RecoveryFormat.SUPPORTED_VERSION) return DecodeResult.Corrupt(CorruptReason.UNSUPPORTED_VERSION)

        if (totalLength > maxFileBytes) return DecodeResult.Corrupt(CorruptReason.OVERSIZE)
        if (totalLength < RecoveryFormat.OVERHEAD_BYTES || bytes.size.toLong() != totalLength) {
            return DecodeResult.Corrupt(CorruptReason.TOO_SHORT)
        }
        if (head.getShort(10).toInt() != 0) return DecodeResult.Corrupt(CorruptReason.HEADER_RESERVED)
        val bodyLength = head.getInt(12).toLong() and 0xFFFFFFFFL
        if (RecoveryFormat.HEADER_BYTES.toLong() + bodyLength + RecoveryFormat.DIGEST_BYTES != totalLength) {
            return DecodeResult.Corrupt(CorruptReason.LENGTH_MISMATCH)
        }
        if (bodyLength < RecoveryFormat.BODY_FIXED_BYTES) return DecodeResult.Corrupt(CorruptReason.BODY_TOO_SHORT)

        val digestStart = bytes.size - RecoveryFormat.DIGEST_BYTES
        val expected = sha256(bytes, 0, digestStart)
        val trailer = bytes.copyOfRange(digestStart, bytes.size)
        if (!MessageDigest.isEqual(expected, trailer)) return DecodeResult.Corrupt(CorruptReason.DIGEST_MISMATCH)

        val slot = head.getInt(16)
        val generation = head.getLong(20)
        if (generation < RecoveryFormat.MIN_GENERATION) return DecodeResult.Corrupt(CorruptReason.BAD_GENERATION)
        if (slot != expectedSlot) return DecodeResult.Corrupt(CorruptReason.WRONG_SLOT)
        val id = ByteArray(RecoveryFormat.BUNDLE_ID_BYTES)
        System.arraycopy(bytes, 28, id, 0, id.size)
        val opKind = head.getShort(44).toInt() and 0xFFFF
        val schema = head.getShort(46).toInt() and 0xFFFF
        val payloadLength = head.getInt(48).toLong() and 0xFFFFFFFFL
        if (payloadLength != bodyLength - RecoveryFormat.BODY_FIXED_BYTES) {
            return DecodeResult.Corrupt(CorruptReason.PAYLOAD_LENGTH_MISMATCH)
        }
        val payload = bytes.copyOfRange(52, 52 + payloadLength.toInt())
        return DecodeResult.Valid(
            RecoveryBundle(BundleMeta(slot, generation, BundleId(id), opKind, schema), payload, trailer)
        )
    }

    /**
     * Identity of a bundle's *meaning*: slot, opKind, payloadSchema and payload bytes. Generation and bundleId are
     * deliberately excluded, because they say which write produced the bundle, not what it recovers. The same
     * payload under a different opKind or payloadSchema is a different key: the same bytes can mean something
     * else, so it is never treated as a duplicate.
     */
    fun semanticKey(slot: Int, opKind: Int, payloadSchema: Int, payload: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update("TLJ-RB-SEM1".toByteArray(Charsets.US_ASCII))
        md.update(ByteBuffer.allocate(4 + 2 + 2 + 4).putInt(slot).putShort(opKind.toShort())
            .putShort(payloadSchema.toShort()).putInt(payload.size).array())
        md.update(payload)
        return md.digest()
    }

    internal fun sha256(bytes: ByteArray, offset: Int, length: Int): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, offset, length)
        return md.digest()
    }
}
