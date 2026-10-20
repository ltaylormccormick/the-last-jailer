package com.thelastjailer.app.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The buffer-selection table, checked exhaustively: every one of the 5 x 5 = 25 ordered pairs of buffer states,
 * so reversed A/B positions are covered by construction, not by sampling.
 */
class BufferSelectionTest {

    private val dummyFingerprint = Fingerprint(1, ByteArray(32))

    private fun report(buffer: Buffer, state: String, generation: Long, seed: Int): BufferReport {
        val status: BufferStatus = when (state) {
            "Ab" -> BufferStatus.Absent
            "V" -> BufferStatus.Valid(
                (RecoveryCodec.decode(Rb.validBytes(generation, seed), Rb.validBytes(generation, seed).size.toLong(), 1,
                    RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES) as DecodeResult.Valid).bundle
            )
            "C" -> BufferStatus.Corrupt(CorruptReason.DIGEST_MISMATCH)
            "F" -> BufferStatus.Future(2)
            "U" -> BufferStatus.Unreadable
            else -> error(state)
        }
        return BufferReport(buffer, status, if (state == "Ab" || state == "U") null else dummyFingerprint)
    }

    private fun choose(a: String, b: String, genA: Long = 5, genB: Long = 4): BufferChoice =
        BufferSelection.choose(report(Buffer.A, a, genA, 1), report(Buffer.B, b, genB, 2))

    private fun assertWrite(expected: Buffer, c: BufferChoice, label: String) {
        assertTrue("$label: expected Write($expected) but was ${describe(c)}", c is BufferChoice.Write && c.buffer == expected)
    }

    private fun assertRefuse(expected: BlockReason, c: BufferChoice, label: String) {
        assertTrue("$label: expected Refuse($expected) but was ${describe(c)}", c is BufferChoice.Refuse && c.reason == expected)
    }

    private fun describe(c: BufferChoice) = when (c) {
        is BufferChoice.Write -> "Write(${c.buffer})"
        is BufferChoice.Refuse -> "Refuse(${c.reason})"
    }

    @Test
    fun `all twenty-five ordered pairs behave exactly as the table says`() {
        val u = BlockReason.UNREADABLE_BUFFER
        // (A state, B state) -> expected. For V+V the generations are 5 (A) and 4 (B), so the lower is B.
        val table = linkedMapOf<Pair<String, String>, Any>(
            // Anything unreadable: refuse, in either position.
            "U" to "U" to u, "U" to "Ab" to u, "U" to "V" to u, "U" to "C" to u, "U" to "F" to u,
            "Ab" to "U" to u, "V" to "U" to u, "C" to "U" to u, "F" to "U" to u,
            // An absent buffer is used when present: touches no existing file.
            "Ab" to "Ab" to Buffer.A, "Ab" to "V" to Buffer.A, "Ab" to "C" to Buffer.A, "Ab" to "F" to Buffer.A,
            "V" to "Ab" to Buffer.B, "C" to "Ab" to Buffer.B, "F" to "Ab" to Buffer.B,
            // Both present: a corrupt buffer holds nothing recoverable and is the one replaced.
            "V" to "C" to Buffer.B, "C" to "V" to Buffer.A,
            "F" to "C" to Buffer.B, "C" to "F" to Buffer.A,
            "C" to "C" to Buffer.A,
            // Both valid, A is newer (5 vs 4): the older (B) is replaced.
            "V" to "V" to Buffer.B,
            // A future file is never touched, and a valid copy next to it is the sole known-good copy.
            "V" to "F" to BlockReason.SOLE_KNOWN_GOOD_COPY, "F" to "V" to BlockReason.SOLE_KNOWN_GOOD_COPY,
            "F" to "F" to BlockReason.NO_WRITABLE_BUFFER
        )
        assertEquals("the table must cover all 25 ordered pairs", 25, table.size)
        for ((pair, expected) in table) {
            val label = "${pair.first}+${pair.second}"
            val c = choose(pair.first, pair.second)
            when (expected) {
                is Buffer -> assertWrite(expected, c, label)
                is BlockReason -> assertRefuse(expected, c, label)
                else -> error(label)
            }
        }
    }

    @Test
    fun `two valid bundles - the lower generation is replaced in either position`() {
        assertWrite(Buffer.B, choose("V", "V", genA = 9, genB = 3), "A newer")
        assertWrite(Buffer.A, choose("V", "V", genA = 3, genB = 9), "B newer")
        assertWrite(Buffer.A, choose("V", "V", genA = 1, genB = 2), "adjacent, A older")
        assertWrite(Buffer.B, choose("V", "V", genA = Long.MAX_VALUE, genB = Long.MAX_VALUE - 1), "near the maximum")
    }

    @Test
    fun `two valid bundles with the same generation - identical copies replace B, differing ones are ambiguous`() {
        val x = Rb.validBytes(5, seed = 1)
        val y = Rb.validBytes(5, seed = 2)
        fun rep(b: Buffer, bytes: ByteArray) = BufferReport(
            b, BufferStatus.Valid((RecoveryCodec.decode(bytes, bytes.size.toLong(), 1, RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES) as DecodeResult.Valid).bundle),
            dummyFingerprint
        )
        assertWrite(Buffer.B, BufferSelection.choose(rep(Buffer.A, x), rep(Buffer.B, x)), "identical copies")
        assertRefuse(BlockReason.AMBIGUOUS_GENERATIONS, BufferSelection.choose(rep(Buffer.A, x), rep(Buffer.B, y)), "same generation, different content")
        assertRefuse(BlockReason.AMBIGUOUS_GENERATIONS, BufferSelection.choose(rep(Buffer.A, y), rep(Buffer.B, x)), "reversed")
    }
}
