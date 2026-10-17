package com.thelastjailer.app.checkpoint

import java.security.MessageDigest

/**
 * The slot stamp (Batch O3-B): one 128-bit digest, stored as `<slot>.stamp` in the live save, over the slot
 * number, **every stored game-state field** and the slot's stored `<slot>.lineage` / `<slot>.arrivals` text.
 *
 * It says one thing: "this exact state, lineage and ledger were last written together by a build that stamps".
 * `SaveStore.save` writes it in the **same editor batch** as the state and the timeline keys, so the three can
 * never disagree on disk because of a crash. A build that does not know about the stamp (an older build) writes
 * the state fields and leaves the stamp, the lineage and the ledger alone, so afterwards the stamp no longer
 * matches the state and the timeline is treated as absent (see `SaveStore.readTimeline` and the Keep rule in
 * `SaveStore.save`). That is the O3 rollback gate: stale history can go missing, it can never be trusted.
 *
 * The stamp is an additional gate, never a replacement: the lineage, ledger-digest, record and checkpoint-file
 * checks all still apply to anything the stamp lets through. It is not authentication - someone who can edit
 * the whole preferences file can forge it. It is computed over the values exactly as stored (typed, not
 * normalised), so a missing or wrong-typed field cannot match.
 */
internal object SlotStamp {
    private const val DOMAIN = "TLJ-STAMP-1|"
    const val HEX_LENGTH = 32
    private val HEX = "0123456789abcdef".toCharArray()

    /** [fields] are (key suffix, value as stored) in a fixed order; [lineage] and [arrivals] as stored text. */
    fun of(slot: Int, fields: List<Pair<String, Any?>>, lineage: String?, arrivals: String?): String {
        val sb = StringBuilder(DOMAIN).append(slot).append('|')
        for ((name, value) in fields) {
            sb.append(name.length).append(':').append(name).append('=')
            appendValue(sb, value)
            sb.append(';')
        }
        sb.append("L=")
        appendText(sb, lineage)
        sb.append(";A=")
        appendText(sb, arrivals)
        val hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray(Charsets.UTF_8))
        val out = StringBuilder(HEX_LENGTH)
        for (i in 0 until HEX_LENGTH / 2) {
            val b = hash[i].toInt() and 0xff
            out.append(HEX[b ushr 4]).append(HEX[b and 0x0f])
        }
        return out.toString()
    }

    private fun appendText(sb: StringBuilder, text: String?) {
        if (text == null) sb.append('n') else sb.append('s').append(text.length).append(':').append(text)
    }

    private fun appendValue(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append('n')
            is String -> appendText(sb, value)
            is Int -> sb.append('i').append(value)
            is Set<*> -> {
                val items = value.map { it.toString() }.sorted()
                sb.append('S').append(items.size).append('[')
                items.forEach { sb.append(it.length).append(':').append(it) }
                sb.append(']')
            }
            else -> sb.append('x').append(value.javaClass.simpleName).append(':').append(value.toString())
        }
    }
}
