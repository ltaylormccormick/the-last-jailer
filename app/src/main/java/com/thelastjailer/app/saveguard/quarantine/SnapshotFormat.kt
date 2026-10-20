package com.thelastjailer.app.saveguard.quarantine

import java.security.MessageDigest

// G2L Stage B1 (dormant): the on-disk text formats of a quarantine snapshot. Pure functions, no file access.
//
// A snapshot directory `snap-NNNN/` holds
//   main.xml / bak.xml   EVIDENCE: raw bytes of the preferences file(s), exactly as read before loading
//   MANIFEST             derived metadata: what was kept, with length and SHA-256 of each evidence file
//   COMPLETE             derived metadata: `tljq1 <sha256(MANIFEST)> <length(MANIFEST)>\n`, written last
// Evidence is never rewritten, renamed or deleted by this package (apart from the store's own just-created partial
// copy during a failed attempt). MANIFEST and COMPLETE can always be regenerated from the evidence.
//
// The manifest holds reason codes, slot NUMBERS and sizes. It never holds a key, a value or a scene id.

internal const val MAIN_NAME = "main.xml"
internal const val BAK_NAME = "bak.xml"
internal const val MANIFEST_NAME = "MANIFEST"
internal const val COMPLETE_NAME = "COMPLETE"
internal val EVIDENCE_NAMES = listOf(MAIN_NAME, BAK_NAME)

internal fun sha256Hex(bytes: ByteArray): String {
    val d = MessageDigest.getInstance("SHA-256").digest(bytes)
    val sb = StringBuilder(d.size * 2)
    for (b in d) {
        val v = b.toInt() and 0xFF
        sb.append("0123456789abcdef"[v ushr 4]).append("0123456789abcdef"[v and 0x0F])
    }
    return sb.toString()
}

class ManifestFile(val name: String, val length: Long, val sha256: String)

class Manifest(val seq: Int, val files: List<ManifestFile>) {
    fun fileNamed(name: String): ManifestFile? = files.firstOrNull { it.name == name }

    companion object {
        private const val HEADER = "tljq-manifest 1"
        private val TOKEN = Regex("[A-Za-z0-9._:+-]{1,64}")
        private val HEX64 = Regex("[0-9a-f]{64}")
        private val NUMBER = Regex("0|[1-9][0-9]{0,9}")
        private val SLOTS = Regex("-|[0-9]{1,9}(,[0-9]{1,9})*")

        /** Metadata fields are restricted to a safe alphabet so nothing can inject a line into the manifest. */
        private fun token(s: String): String = if (TOKEN.matches(s)) s else "invalid"

        fun render(seq: Int, meta: SnapshotMeta, files: List<ManifestFile>): ByteArray {
            val sb = StringBuilder()
            sb.append(HEADER).append('\n')
            sb.append("seq ").append(seq).append('\n')
            sb.append("app ").append(token(meta.appVersion)).append('\n')
            sb.append("sdk ").append(meta.sdk).append('\n')
            sb.append("tier ").append(meta.tier).append('\n')
            sb.append("reason ").append(token(meta.reason)).append('\n')
            sb.append("slots ").append(if (meta.slots.isEmpty()) "-" else meta.slots.joinToString(",")).append('\n')
            sb.append("utc ").append(token(meta.utc)).append('\n')
            for (f in files) sb.append("file ").append(f.name).append(' ').append(f.length).append(' ').append(f.sha256).append('\n')
            return sb.toString().toByteArray(Charsets.UTF_8)
        }

        /** Strict parse: unknown line, duplicate, bad number, bad hash, no file line, or a missing final newline gives null. */
        fun parse(bytes: ByteArray): Manifest? {
            if (bytes.isEmpty() || bytes.size > 4096) return null
            val text = try {
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (e: java.nio.charset.CharacterCodingException) {
                return null
            }
            if (!text.endsWith("\n")) return null
            val lines = text.dropLast(1).split("\n")
            if (lines.isEmpty() || lines[0] != HEADER) return null
            var seq: Int? = null
            val seen = HashSet<String>()
            val files = ArrayList<ManifestFile>()
            for (line in lines.drop(1)) {
                val parts = line.split(" ")
                when (parts[0]) {
                    "seq" -> {
                        if (parts.size != 2 || !seen.add("seq") || !NUMBER.matches(parts[1])) return null
                        seq = parts[1].toIntOrNull() ?: return null
                    }
                    "app", "reason", "utc" -> if (parts.size != 2 || !seen.add(parts[0]) || !TOKEN.matches(parts[1])) return null
                    "sdk", "tier" -> if (parts.size != 2 || !seen.add(parts[0]) || !NUMBER.matches(parts[1])) return null
                    "slots" -> if (parts.size != 2 || !seen.add("slots") || !SLOTS.matches(parts[1])) return null
                    "file" -> {
                        if (parts.size != 4 || parts[1] !in EVIDENCE_NAMES) return null
                        if (!NUMBER.matches(parts[2]) || !HEX64.matches(parts[3])) return null
                        if (files.any { it.name == parts[1] }) return null
                        files.add(ManifestFile(parts[1], parts[2].toLongOrNull() ?: return null, parts[3]))
                    }
                    else -> return null
                }
            }
            val required = listOf("seq", "app", "sdk", "tier", "reason", "slots", "utc")
            if (seq == null || required.any { it !in seen } || files.isEmpty()) return null
            return Manifest(seq, files)
        }
    }
}

/** Caller-supplied description of why a snapshot was taken. Nothing in it is saved-game content. */
class SnapshotMeta(
    val appVersion: String,
    val sdk: Int,
    val tier: Int,
    val reason: String,
    val slots: List<Int>,
    val utc: String
)

internal object CompleteMarker {
    fun render(manifestBytes: ByteArray): ByteArray =
        "tljq1 ${sha256Hex(manifestBytes)} ${manifestBytes.size}\n".toByteArray(Charsets.UTF_8)

    /** True only if [marker] is exactly the marker for [manifestBytes]: no extra byte, no variation. */
    fun matches(marker: ByteArray, manifestBytes: ByteArray): Boolean = marker.contentEquals(render(manifestBytes))
}
