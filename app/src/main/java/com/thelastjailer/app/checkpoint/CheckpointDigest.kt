package com.thelastjailer.app.checkpoint

import java.security.MessageDigest

/**
 * The 128-bit digest that binds a checkpoint record to the arrival ledger in the live save.
 *
 * The record keeps its own CRC32 (an integrity check against accidents; the record format is unchanged).
 * The ledger is the only trust anchor between two files that cannot be written atomically, so it stores
 * this wider digest: the first 16 bytes of SHA-256 over a domain tag, the slot, the lineage, the chapter
 * and the record's canonical rendering. Binding the slot and lineage also stops a file or record copied
 * between slots, or restored from another timeline, from matching.
 *
 * It is not authentication: someone who can edit both files can forge both. That is out of scope.
 */
internal object CheckpointDigest {
    private const val DOMAIN = "TLJ-ARRIVAL-1|"
    const val HEX_LENGTH = 32

    fun of(slot: Int, record: CheckpointRecord): String {
        val input = DOMAIN + slot + "|" + record.lineage + "|" + record.chapterNumber + "|" + CheckpointCrc.canonical(record)
        val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(HEX_LENGTH)
        for (i in 0 until HEX_LENGTH / 2) {
            val b = hash[i].toInt() and 0xff
            sb.append(HEX[b ushr 4]).append(HEX[b and 0x0f])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
