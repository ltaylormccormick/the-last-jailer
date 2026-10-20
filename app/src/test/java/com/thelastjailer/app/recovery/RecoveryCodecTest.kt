package com.thelastjailer.app.recovery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecoveryCodecTest {

    private val max = RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES

    private fun decode(bytes: ByteArray, slot: Int = Rb.SLOT, total: Long = bytes.size.toLong(), limit: Int = max) =
        RecoveryCodec.decode(bytes, total, slot, limit)

    private fun assertCorrupt(expected: CorruptReason, r: DecodeResult) {
        assertTrue("expected CORRUPT($expected) but was ${describe(r)}", r is DecodeResult.Corrupt && r.reason == expected)
    }

    private fun describe(r: DecodeResult) = when (r) {
        is DecodeResult.Valid -> "VALID"
        is DecodeResult.Corrupt -> "CORRUPT(${r.reason})"
        is DecodeResult.Future -> "FUTURE(${r.version})"
    }

    @Test
    fun `a bundle round-trips with every field intact`() {
        val meta = BundleMeta(3, 42L, BundleId(ByteArray(16) { it.toByte() }), 0xABCD, 0x1234)
        val payload = Rb.payload(5, 1000)
        val bytes = RecoveryCodec.encode(meta, payload)
        val r = decode(bytes, slot = 3)
        assertTrue(r is DecodeResult.Valid)
        val b = (r as DecodeResult.Valid).bundle
        assertEquals(3, b.meta.slot)
        assertEquals(42L, b.meta.generation)
        assertEquals(meta.bundleId, b.meta.bundleId)
        assertEquals(0xABCD, b.meta.opKind)
        assertEquals(0x1234, b.meta.payloadSchema)
        assertArrayEquals(payload, b.payload)
        assertArrayEquals(bytes.copyOfRange(bytes.size - 32, bytes.size), b.fileDigest)
        assertEquals(bytes.size, RecoveryFormat.OVERHEAD_BYTES + payload.size)
    }

    @Test
    fun `an empty payload is a valid bundle`() {
        val bytes = RecoveryCodec.encode(BundleMeta(1, 1, BundleId(ByteArray(16)), 0, 0), ByteArray(0))
        assertEquals(RecoveryFormat.OVERHEAD_BYTES, bytes.size)
        assertTrue(decode(bytes) is DecodeResult.Valid)
    }

    @Test
    fun `every truncation of a valid bundle is rejected`() {
        val bytes = Rb.validBytes(7, 1)
        for (n in 0 until bytes.size) {
            val r = decode(bytes.copyOf(n))
            assertFalse("a ${n}-byte prefix must not be valid", r is DecodeResult.Valid)
            assertTrue("a ${n}-byte prefix must be corrupt, was ${describe(r)}", r is DecodeResult.Corrupt)
        }
    }

    @Test
    fun `flipping any single byte of a valid bundle never yields a valid bundle`() {
        val bytes = Rb.validBytes(7, 1)
        for (i in bytes.indices) {
            val damaged = bytes.copyOf().also { it[i] = (it[i].toInt() xor 0x01).toByte() }
            assertFalse("byte $i flipped must not be valid", decode(damaged) is DecodeResult.Valid)
        }
    }

    @Test
    fun `trailing bytes after the digest are rejected`() {
        assertCorrupt(CorruptReason.LENGTH_MISMATCH, decode(Rb.raw(trailing = 1)))
        assertCorrupt(CorruptReason.LENGTH_MISMATCH, decode(Rb.raw(trailing = 100)))
    }

    @Test
    fun `a wrong magic is corrupt`() {
        val bytes = Rb.validBytes(1, 1).also { it[0] = 'X'.code.toByte() }
        assertCorrupt(CorruptReason.BAD_MAGIC, decode(bytes))
    }

    @Test
    fun `fewer than ten bytes is too short, even with a matching magic`() {
        assertCorrupt(CorruptReason.TOO_SHORT, decode(ByteArray(0)))
        assertCorrupt(CorruptReason.TOO_SHORT, decode(RecoveryFormat.MAGIC.copyOf(9)))
    }

    @Test
    fun `version zero is corrupt and any higher version is future`() {
        assertCorrupt(CorruptReason.UNSUPPORTED_VERSION, decode(Rb.raw(version = 0)))
        for (v in listOf(2, 3, 255, 256, 0x7FFF, 0xFFFF)) {
            val r = decode(Rb.raw(version = v))
            assertTrue("version $v", r is DecodeResult.Future && r.version == v)
        }
    }

    @Test
    fun `a newer-format file is recognised from its first ten bytes alone`() {
        val header = Rb.futureBytes(version = 2, size = 10)
        val r = decode(header)
        assertTrue(r is DecodeResult.Future && r.version == 2)
    }

    @Test
    fun `a newer-format file larger than this build's ceiling is future, never corrupt`() {
        val r = decode(Rb.futureBytes(2, 64).copyOf(64), total = max.toLong() * 10)
        assertTrue(describe(r), r is DecodeResult.Future)
    }

    @Test
    fun `a nonzero reserved field is corrupt`() {
        assertCorrupt(CorruptReason.HEADER_RESERVED, decode(Rb.raw(reserved = 1)))
        assertCorrupt(CorruptReason.HEADER_RESERVED, decode(Rb.raw(reserved = 0x8000)))
    }

    @Test
    fun `a body length that disagrees with the file length is corrupt, even with a correct digest`() {
        assertCorrupt(CorruptReason.LENGTH_MISMATCH, decode(Rb.raw(bodyLengthDelta = 1)))
        assertCorrupt(CorruptReason.LENGTH_MISMATCH, decode(Rb.raw(bodyLengthDelta = -1)))
        assertCorrupt(CorruptReason.LENGTH_MISMATCH, decode(Rb.raw(bodyLengthDelta = Int.MAX_VALUE)))
    }

    @Test
    fun `a payload length that disagrees with the body is corrupt, even with a correct digest`() {
        assertCorrupt(CorruptReason.PAYLOAD_LENGTH_MISMATCH, decode(Rb.raw(payloadLengthDelta = 1)))
        assertCorrupt(CorruptReason.PAYLOAD_LENGTH_MISMATCH, decode(Rb.raw(payloadLengthDelta = -1)))
        assertCorrupt(CorruptReason.PAYLOAD_LENGTH_MISMATCH, decode(Rb.raw(payloadLengthDelta = Int.MIN_VALUE)))
    }

    @Test
    fun `a bad digest is corrupt`() {
        assertCorrupt(CorruptReason.DIGEST_MISMATCH, decode(Rb.raw(goodDigest = false)))
    }

    @Test
    fun `malformed generations are corrupt, even with a correct digest`() {
        for (g in listOf(0L, -1L, -5L, Long.MIN_VALUE)) {
            assertCorrupt(CorruptReason.BAD_GENERATION, decode(Rb.raw(generation = g)))
        }
        assertTrue(decode(Rb.raw(generation = 1)) is DecodeResult.Valid)
        assertTrue(decode(Rb.raw(generation = Long.MAX_VALUE)) is DecodeResult.Valid)
    }

    @Test
    fun `a bundle that belongs to another slot is corrupt in this one`() {
        assertCorrupt(CorruptReason.WRONG_SLOT, decode(Rb.raw(slot = 2), slot = 1))
        assertCorrupt(CorruptReason.WRONG_SLOT, decode(Rb.raw(slot = 0), slot = 1))
        assertCorrupt(CorruptReason.WRONG_SLOT, decode(Rb.raw(slot = -3), slot = 1))
    }

    @Test
    fun `the size ceiling is exact`() {
        val limit = 1024
        val atLimit = Rb.raw(payload = ByteArray(limit - RecoveryFormat.OVERHEAD_BYTES))
        assertEquals(limit, atLimit.size)
        assertTrue(decode(atLimit, limit = limit) is DecodeResult.Valid)
        val over = Rb.raw(payload = ByteArray(limit - RecoveryFormat.OVERHEAD_BYTES + 1))
        // The store reads at most limit + 1 bytes, so an oversize file arrives truncated to that.
        assertCorrupt(CorruptReason.OVERSIZE, decode(over.copyOf(limit + 1), total = over.size.toLong(), limit = limit))
    }

    @Test
    fun `opKind and payloadSchema are opaque - unknown values are preserved, not rejected`() {
        val r = decode(Rb.raw(opKind = 0xFFFF, schema = 0xFFFF))
        assertTrue(r is DecodeResult.Valid)
        val m = (r as DecodeResult.Valid).bundle.meta
        assertEquals(0xFFFF, m.opKind)
        assertEquals(0xFFFF, m.payloadSchema)
    }

    @Test
    fun `semantic identity ignores which write produced a bundle but not what it means`() {
        val base = RecoveryCodec.semanticKey(1, 1, 1, Rb.payload(1))
        // Same meaning, regardless of generation or bundle id (neither is an input).
        assertArrayEquals(base, (decode(Rb.validBytes(5, 1, idByte = 9)) as DecodeResult.Valid).bundle.semanticKey)
        assertArrayEquals(base, (decode(Rb.validBytes(900, 1, idByte = 200)) as DecodeResult.Valid).bundle.semanticKey)
        // Anything that changes the meaning changes the key.
        assertFalse(base.contentEquals(RecoveryCodec.semanticKey(1, 2, 1, Rb.payload(1))))
        assertFalse(base.contentEquals(RecoveryCodec.semanticKey(1, 1, 2, Rb.payload(1))))
        assertFalse(base.contentEquals(RecoveryCodec.semanticKey(2, 1, 1, Rb.payload(1))))
        assertFalse(base.contentEquals(RecoveryCodec.semanticKey(1, 1, 1, Rb.payload(2))))
        assertFalse(base.contentEquals(RecoveryCodec.semanticKey(1, 1, 1, Rb.payload(1) + byteArrayOf(0))))
    }

    @Test
    fun `encoding rejects values the format cannot hold`() {
        val id = BundleId(ByteArray(16))
        for (meta in listOf(
            BundleMeta(0, 1, id, 0, 0), BundleMeta(-1, 1, id, 0, 0), BundleMeta(1, 0, id, 0, 0),
            BundleMeta(1, -1, id, 0, 0), BundleMeta(1, 1, id, -1, 0), BundleMeta(1, 1, id, 0x10000, 0),
            BundleMeta(1, 1, id, 0, -1), BundleMeta(1, 1, id, 0, 0x10000)
        )) {
            try {
                RecoveryCodec.encode(meta, ByteArray(1))
                fail("should have been rejected")
            } catch (expected: IllegalArgumentException) {
            }
        }
        try { BundleId(ByteArray(15)); fail() } catch (expected: IllegalArgumentException) {}
    }

    @Test
    fun `a bundle id never prints its bytes`() {
        assertEquals("BundleId", BundleId(ByteArray(16) { 7 }).toString())
        assertNotEquals(BundleId(ByteArray(16) { 1 }), BundleId(ByteArray(16) { 2 }))
    }
}
