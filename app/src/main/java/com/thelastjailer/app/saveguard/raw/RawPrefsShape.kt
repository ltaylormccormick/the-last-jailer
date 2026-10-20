package com.thelastjailer.app.saveguard.raw

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

// G2L Stage B1 (dormant): strict validation of the raw bytes of a SharedPreferences XML file.
//
// Why this exists. When Android cannot parse the preferences file it logs and loads an EMPTY map, which is
// indistinguishable, through the SharedPreferences API, from a brand-new install. A game that treats that as "fresh"
// and then autosaves destroys the player's damaged-but-recoverable file. So the raw bytes are judged separately, and
// anything that cannot be positively established as a legitimate preferences file is "suspect".
//
// Shape contract (verified against AOSP XmlUtils android-14.0.0_r1, see qa/G2L_B1.md): Android's map reader accepts
// far more than this app ever writes (null keys, last-duplicate-wins, arrays, lists, nested maps, trailing bytes after
// the root). This validator accepts ONLY the six shapes SharedPreferences.Editor can produce, with a name on every
// entry, and rejects everything else rather than letting a partial decode pass for a whole one.
//
// Pure: no Android classes, no file access, no logging. Nothing returned or thrown ever contains a key or a value.

/** Why a raw file cannot be trusted. Reason codes only: they never carry saved-game content. */
enum class SuspectReason {
    UNREADABLE, TOO_LARGE, ZERO_LENGTH, BOM, BINARY_XML, DOCTYPE, UNPARSEABLE, TRAILING_DATA, NOT_A_MAP,
    UNKNOWN_TAG, UNSUPPORTED_SHAPE, NO_NAME, DUPLICATE_KEY, BAD_VALUE, BAD_STRUCTURE,
    MISMATCH, UNREADABLE_PREFS, STALE_BAK
}

sealed class RawShapeResult {
    /** [entries] hold exactly the runtime types SharedPreferences.getAll() returns: Int, Long, Float, Boolean, String, Set<String>. */
    class Valid(val entries: Map<String, Any>) : RawShapeResult() {
        override fun toString() = "Valid(${entries.size} entries)"
    }

    class Suspect(val reason: SuspectReason) : RawShapeResult() {
        override fun toString() = "Suspect($reason)"
    }
}

object RawPrefsShape {
    /** Provisional ceiling for one preferences file (design decision D11). A real three-slot file is tens of KB. */
    const val MAX_BYTES = 1 shl 20

    private val WRITER_TAGS = setOf("boolean", "float", "int", "long", "string", "set")

    /** Tags Android's reader would accept that this app never writes. */
    private val ANDROID_ONLY_TAGS = setOf(
        "null", "double", "byte-array", "int-array", "long-array", "double-array", "string-array", "boolean-array", "map", "list"
    )

    private val INT_TEXT = Regex("0|-?[1-9][0-9]*")

    /** The grammar of java.lang.Float.toString: what Android's writer emits for a float, including NaN and infinities. */
    private val FLOAT_TEXT = Regex("NaN|-?Infinity|-?[0-9]+\\.[0-9]+(E-?[0-9]+)?")

    private val EMPTY_MAP = Regex(
        "(<\\?xml version='1\\.0' encoding='utf-8' standalone='yes' \\?>\\r?\\n)?(<map\\s*/>|<map>\\s*</map>)\\s*"
    )

    /** True only for exactly the forms of a legitimate EMPTY preferences file (no BOM, nothing else). */
    fun isCanonicalEmptyMap(bytes: ByteArray): Boolean =
        bytes.isNotEmpty() && bytes.size <= 4096 && EMPTY_MAP.matches(String(bytes, Charsets.ISO_8859_1))

    fun validate(bytes: ByteArray): RawShapeResult {
        if (bytes.isEmpty()) return suspect(SuspectReason.ZERO_LENGTH)
        if (bytes.size > MAX_BYTES) return suspect(SuspectReason.TOO_LARGE)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return suspect(SuspectReason.BOM)
        }
        if (bytes.size >= 3 && bytes[0] == 'A'.code.toByte() && bytes[1] == 'B'.code.toByte() && bytes[2] == 'X'.code.toByte()) {
            return suspect(SuspectReason.BINARY_XML)
        }
        if (containsAscii(bytes, "<!DOCTYPE") || containsAscii(bytes, "<!ENTITY")) return suspect(SuspectReason.DOCTYPE)
        val root = try {
            parse(bytes)
        } catch (e: Exception) {
            return suspect(if (isTrailingDataOnly(bytes)) SuspectReason.TRAILING_DATA else SuspectReason.UNPARSEABLE)
        } catch (e: StackOverflowError) {
            return suspect(SuspectReason.UNPARSEABLE)
        }
        return walk(root)
    }

    private fun suspect(reason: SuspectReason) = RawShapeResult.Suspect(reason)

    private fun parse(bytes: ByteArray): Element {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isValidating = false
        factory.isCoalescing = false
        factory.isExpandEntityReferences = false
        // Not every platform parser knows every feature; the textual DOCTYPE/ENTITY pre-scan above is the guarantee.
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature(factory, "http://javax.xml.XMLConstants/feature/secure-processing", true)
        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(object : ErrorHandler {
            override fun warning(e: SAXParseException) = Unit
            override fun error(e: SAXParseException) { throw e }
            override fun fatalError(e: SAXParseException) { throw e }
        })
        val doc = builder.parse(ByteArrayInputStream(bytes))
        // Anything at document level other than the root (a comment, a processing instruction) is not what the writer emits.
        var n = doc.firstChild
        var elements = 0
        while (n != null) {
            when (n.nodeType) {
                Node.ELEMENT_NODE -> elements++
                else -> throw IllegalStateException("unexpected document-level node")
            }
            n = n.nextSibling
        }
        if (elements != 1) throw IllegalStateException("not exactly one root")
        return doc.documentElement
    }

    private fun trySetFeature(factory: DocumentBuilderFactory, name: String, value: Boolean) {
        try {
            factory.setFeature(name, value)
        } catch (e: Exception) {
            // unsupported on this parser
        }
    }

    private fun isTrailingDataOnly(bytes: ByteArray): Boolean {
        val marker = "</map>".toByteArray(Charsets.ISO_8859_1)
        var end = -1
        var i = bytes.size - marker.size
        while (i >= 0) {
            var same = true
            for (j in marker.indices) if (bytes[i + j] != marker[j]) { same = false; break }
            if (same) { end = i + marker.size; break }
            i--
        }
        if (end < 0 || end == bytes.size) return false
        var tailHasData = false
        for (k in end until bytes.size) {
            val c = bytes[k].toInt()
            if (c != ' '.code && c != '\t'.code && c != '\r'.code && c != '\n'.code) tailHasData = true
        }
        if (!tailHasData) return false
        return try { parse(bytes.copyOf(end)); true } catch (e: Exception) { false }
    }

    private fun containsAscii(bytes: ByteArray, needle: String): Boolean {
        val n = needle.toByteArray(Charsets.ISO_8859_1)
        if (n.isEmpty() || bytes.size < n.size) return false
        for (i in 0..bytes.size - n.size) {
            var same = true
            for (j in n.indices) if (bytes[i + j] != n[j]) { same = false; break }
            if (same) return true
        }
        return false
    }

    private fun walk(root: Element): RawShapeResult {
        if (root.tagName != "map") return suspect(SuspectReason.NOT_A_MAP)
        if (root.attributes.length != 0) return suspect(SuspectReason.BAD_STRUCTURE)
        val entries = LinkedHashMap<String, Any>()
        var child = root.firstChild
        while (child != null) {
            when (child.nodeType) {
                Node.TEXT_NODE -> if (!isBlank(child.nodeValue)) return suspect(SuspectReason.BAD_STRUCTURE)
                Node.ELEMENT_NODE -> {
                    val e = child as Element
                    val tag = e.tagName
                    if (tag !in WRITER_TAGS) {
                        return suspect(if (tag in ANDROID_ONLY_TAGS) SuspectReason.UNSUPPORTED_SHAPE else SuspectReason.UNKNOWN_TAG)
                    }
                    val name = e.getAttribute("name")
                    if (!e.hasAttribute("name") || name.isEmpty()) return suspect(SuspectReason.NO_NAME)
                    val value: Any = when (val parsed = readValue(e, tag)) {
                        is Parsed.Bad -> return suspect(parsed.reason)
                        is Parsed.Ok -> parsed.value
                    }
                    if (entries.containsKey(name)) return suspect(SuspectReason.DUPLICATE_KEY)
                    entries[name] = value
                }
                else -> return suspect(SuspectReason.BAD_STRUCTURE)
            }
            child = child.nextSibling
        }
        return RawShapeResult.Valid(entries)
    }

    private sealed class Parsed {
        class Ok(val value: Any) : Parsed()
        class Bad(val reason: SuspectReason) : Parsed()
    }

    private fun readValue(e: Element, tag: String): Parsed {
        val attrs = e.attributes
        val attrNames = (0 until attrs.length).map { attrs.item(it).nodeName }.toSet()
        return when (tag) {
            "string" -> {
                if (attrNames != setOf("name")) return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                val text = textOnly(e) ?: return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                Parsed.Ok(text)
            }
            "set" -> {
                if (attrNames != setOf("name")) return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                val members = LinkedHashSet<String>()
                var c = e.firstChild
                while (c != null) {
                    when (c.nodeType) {
                        Node.TEXT_NODE -> if (!isBlank(c.nodeValue)) return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                        Node.ELEMENT_NODE -> {
                            val m = c as Element
                            if (m.tagName != "string" || m.attributes.length != 0) return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                            val text = textOnly(m) ?: return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                            if (!members.add(text)) return Parsed.Bad(SuspectReason.BAD_VALUE)
                        }
                        else -> return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                    }
                    c = c.nextSibling
                }
                Parsed.Ok(members)
            }
            else -> { // int, long, float, boolean
                if (attrNames != setOf("name", "value")) {
                    return Parsed.Bad(if ("value" !in attrNames) SuspectReason.BAD_VALUE else SuspectReason.BAD_STRUCTURE)
                }
                if (e.firstChild != null) return Parsed.Bad(SuspectReason.BAD_STRUCTURE)
                val text = e.getAttribute("value")
                val v: Any? = when (tag) {
                    "int" -> if (INT_TEXT.matches(text)) text.toIntOrNull() else null
                    "long" -> if (INT_TEXT.matches(text)) text.toLongOrNull() else null
                    "float" -> if (FLOAT_TEXT.matches(text)) text.toFloatOrNull() else null
                    else -> when (text) { "true" -> true; "false" -> false; else -> null }
                }
                if (v == null) Parsed.Bad(SuspectReason.BAD_VALUE) else Parsed.Ok(v)
            }
        }
    }

    /** The element's text if it contains only text; null if it has any child element, CDATA, comment, etc. */
    private fun textOnly(e: Element): String? {
        val sb = StringBuilder()
        var c = e.firstChild
        while (c != null) {
            if (c.nodeType != Node.TEXT_NODE) return null
            sb.append(c.nodeValue)
            c = c.nextSibling
        }
        return sb.toString()
    }

    private fun isBlank(s: String?): Boolean = s == null || s.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
}
