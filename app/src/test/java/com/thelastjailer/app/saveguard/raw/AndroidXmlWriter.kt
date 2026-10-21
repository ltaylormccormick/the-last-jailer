package com.thelastjailer.app.saveguard.raw

/**
 * A test-only re-implementation of what Android's XmlUtils.writeMapXml + FastXmlSerializer emit for a preferences map,
 * transcribed from the AOSP sources (android-8.0.0_r1 and android-14.0.0_r1; see qa/G2L_B1.md). It is a SIMULATION:
 * real Android writer fixtures captured on an emulator remain mandatory before any activation.
 */
object AndroidXmlWriter {
    const val DECLARATION = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"

    fun write(map: Map<String, Any>): ByteArray {
        val sb = StringBuilder(DECLARATION)
        if (map.isEmpty()) {
            sb.append("<map />\n")
            return sb.toString().toByteArray(Charsets.UTF_8)
        }
        sb.append("<map>\n")
        for ((k, v) in map) {
            when (v) {
                is Int -> sb.append("    <int name=\"").append(esc(k)).append("\" value=\"").append(v).append("\" />\n")
                is Long -> sb.append("    <long name=\"").append(esc(k)).append("\" value=\"").append(v).append("\" />\n")
                is Float -> sb.append("    <float name=\"").append(esc(k)).append("\" value=\"").append(v.toString()).append("\" />\n")
                is Boolean -> sb.append("    <boolean name=\"").append(esc(k)).append("\" value=\"").append(v).append("\" />\n")
                is String -> {
                    if (v.isEmpty()) sb.append("    <string name=\"").append(esc(k)).append("\" />\n")
                    else sb.append("    <string name=\"").append(esc(k)).append("\">").append(esc(v)).append("</string>\n")
                }
                is Set<*> -> {
                    if (v.isEmpty()) sb.append("    <set name=\"").append(esc(k)).append("\" />\n")
                    else {
                        sb.append("    <set name=\"").append(esc(k)).append("\">\n")
                        for (m in v) sb.append("        <string>").append(esc(m as String)).append("</string>\n")
                        sb.append("    </set>\n")
                    }
                }
                else -> error("unsupported")
            }
        }
        sb.append("</map>\n")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** FastXmlSerializer.escapeAndAppendString: quote, ampersand, angle brackets, and control characters as references. */
    fun esc(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch == '"' -> sb.append("&quot;")
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch.code < 32 -> sb.append("&#").append(ch.code).append(';')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
