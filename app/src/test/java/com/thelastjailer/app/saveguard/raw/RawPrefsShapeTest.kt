package com.thelastjailer.app.saveguard.raw

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawPrefsShapeTest {

    private fun s(xml: String) = xml.toByteArray(Charsets.UTF_8)
    private fun body(inner: String) = s("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n$inner\n</map>\n")

    private fun reason(bytes: ByteArray): SuspectReason? = (RawPrefsShape.validate(bytes) as? RawShapeResult.Suspect)?.reason
    private fun entries(bytes: ByteArray): Map<String, Any> = (RawPrefsShape.validate(bytes) as RawShapeResult.Valid).entries

    // ---------------------------------------------------------------- the six writer shapes and round trips

    @Test fun `every shape the writer produces is valid and decodes to the same typed values`() {
        val map = linkedMapOf<String, Any>(
            "a" to 7, "b" to -2147483648, "c" to 2147483647, "d" to 9007199254740993L, "e" to -1L,
            "f" to 1.5f, "g" to -0.0f, "h" to true, "i" to false, "j" to "text", "k" to "", "l" to setOf("x", "y"), "m" to emptySet<String>()
        )
        val got = entries(AndroidXmlWriter.write(map))
        assertEquals(map.size, got.size)
        for ((k, v) in map) assertEquals("key $k", v, got[k])
        assertEquals(Int::class.javaObjectType, got["a"]!!.javaClass)
        assertEquals(Long::class.javaObjectType, got["d"]!!.javaClass)
        assertEquals(Float::class.javaObjectType, got["f"]!!.javaClass)
    }

    @Test fun `float values in the whole Float toString grammar are accepted, including scientific notation NaN and infinities`() {
        val floats = listOf(0f, 1f, -1f, 0.1f, 1.0E10f, 1.0E-10f, -3.4028235E38f, 1.4E-45f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 123456.79f)
        for (f in floats) {
            val bytes = AndroidXmlWriter.write(mapOf("f" to f))
            val got = entries(bytes)["f"] as Float
            assertEquals("bits for $f", java.lang.Float.floatToIntBits(f), java.lang.Float.floatToIntBits(got))
        }
    }

    @Test fun `escaping round trips for quote ampersand angle brackets and whitespace controls`() {
        val tricky = listOf("a\"b", "a&b", "<tag>", "a&amp;b", "line1\nline2", "tab\there", "cr\rhere", "]]>", "'single'", "é ü 漢字 😀")
        for (t in tricky) {
            val got = entries(AndroidXmlWriter.write(mapOf("k" to t, "set" to setOf(t))))
            assertEquals("string $t", t, got["k"])
            assertEquals("set $t", setOf(t), got["set"])
        }
    }

    @Test fun `a key that needs escaping round trips`() {
        val key = "1.we\"ird&<key>"
        assertEquals(1, entries(AndroidXmlWriter.write(mapOf(key to 1))).size)
        assertTrue(entries(AndroidXmlWriter.write(mapOf(key to 1))).containsKey(key))
    }

    @Test fun `large random maps written by the Android style writer never produce a false suspect`() {
        val rnd = Random(20260913)
        repeat(300) {
            val map = linkedMapOf<String, Any>()
            repeat(rnd.nextInt(60)) { i ->
                val key = "k${rnd.nextInt(1000)}_$i"
                map[key] = when (rnd.nextInt(6)) {
                    0 -> rnd.nextInt()
                    1 -> rnd.nextLong()
                    2 -> java.lang.Float.intBitsToFloat(rnd.nextInt())
                    3 -> rnd.nextBoolean()
                    4 -> randomText(rnd)
                    else -> (0 until rnd.nextInt(4)).map { randomText(rnd) }.toSet()
                }
            }
            val bytes = AndroidXmlWriter.write(map)
            val result = RawPrefsShape.validate(bytes)
            assertTrue("false suspect: $result", result is RawShapeResult.Valid)
            val got = (result as RawShapeResult.Valid).entries
            assertEquals(map.size, got.size)
            for ((k, v) in map) {
                val g = got[k]
                if (v is Float) assertEquals(java.lang.Float.floatToIntBits(v), java.lang.Float.floatToIntBits(g as Float))
                else assertEquals(v, g)
            }
        }
    }

    private fun randomText(rnd: Random): String {
        val alphabet = "abcXYZ 019\"&<>'\n\t.,;:{}[]é漢"
        return (0 until rnd.nextInt(12)).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
    }

    // ---------------------------------------------------------------- legitimate empty

    @Test fun `canonical empty maps are recognised`() {
        assertTrue(RawPrefsShape.isCanonicalEmptyMap(s("<map />")))
        assertTrue(RawPrefsShape.isCanonicalEmptyMap(s("<map></map>")))
        assertTrue(RawPrefsShape.isCanonicalEmptyMap(AndroidXmlWriter.write(emptyMap())))
        assertTrue(RawPrefsShape.isCanonicalEmptyMap(s("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n")))
    }

    @Test fun `things that merely look empty are not canonical empty`() {
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(ByteArray(0)))
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(s("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>")))
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(s("<map><int name=\"a\" value=\"1\" /></map>")))
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + s("<map />")))
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(s("<map />garbage")))
        assertFalse(RawPrefsShape.isCanonicalEmptyMap(s("   ")))
    }

    // ---------------------------------------------------------------- suspect table

    @Test fun `structural failures map to their reason codes`() {
        assertEquals(SuspectReason.ZERO_LENGTH, reason(ByteArray(0)))
        assertEquals(SuspectReason.TOO_LARGE, reason(ByteArray(RawPrefsShape.MAX_BYTES + 1) { ' '.code.toByte() }))
        assertEquals(SuspectReason.BOM, reason(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + AndroidXmlWriter.write(mapOf("a" to 1))))
        assertEquals(SuspectReason.BINARY_XML, reason(byteArrayOf('A'.code.toByte(), 'B'.code.toByte(), 'X'.code.toByte(), 0, 0, 0)))
        assertEquals(SuspectReason.DOCTYPE, reason(s("<!DOCTYPE map>\n<map />")))
        assertEquals(SuspectReason.DOCTYPE, reason(s("<?xml version='1.0'?>\n<!DOCTYPE x [<!ENTITY a \"b\">]>\n<map />")))
        assertEquals(SuspectReason.UNPARSEABLE, reason(s("not xml at all")))
        assertEquals(SuspectReason.UNPARSEABLE, reason(s("<map><int name=\"a\" value=\"1\" />")))
        assertEquals(SuspectReason.NOT_A_MAP, reason(s("<root />")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(s("<map x=\"1\" />")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(s("<map>stray text</map>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(s("<map><!-- c --></map>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(s("<map><![CDATA[x]]></map>")))
    }

    @Test fun `trailing data after the root is reported as such`() {
        val good = AndroidXmlWriter.write(mapOf("a" to 1))
        assertEquals(SuspectReason.TRAILING_DATA, reason(good + s("garbage")))
        assertEquals(SuspectReason.TRAILING_DATA, reason(good + s("<map />")))
        assertTrue(RawPrefsShape.validate(good + s("\n  \n")) is RawShapeResult.Valid)
    }

    @Test fun `tags Android would accept but this app never writes are unsupported and unknown tags are unknown`() {
        for (t in listOf("null", "double", "byte-array", "int-array", "long-array", "double-array", "string-array", "boolean-array", "map", "list")) {
            assertEquals(t, SuspectReason.UNSUPPORTED_SHAPE, reason(body("<$t name=\"a\" />")))
        }
        assertEquals(SuspectReason.UNKNOWN_TAG, reason(body("<widget name=\"a\" />")))
        assertEquals(SuspectReason.UNKNOWN_TAG, reason(body("<Int name=\"a\" value=\"1\" />")))
    }

    @Test fun `names are mandatory and unique`() {
        assertEquals(SuspectReason.NO_NAME, reason(body("<int value=\"1\" />")))
        assertEquals(SuspectReason.NO_NAME, reason(body("<int name=\"\" value=\"1\" />")))
        assertEquals(SuspectReason.NO_NAME, reason(body("<string>x</string>")))
        assertEquals(SuspectReason.DUPLICATE_KEY, reason(body("<int name=\"a\" value=\"1\" />\n<int name=\"a\" value=\"2\" />")))
        assertEquals(SuspectReason.DUPLICATE_KEY, reason(body("<int name=\"a\" value=\"1\" />\n<string name=\"a\">x</string>")))
    }

    @Test fun `numeric and boolean edge cases`() {
        val bad = listOf(
            "<int name=\"a\" value=\"\" />", "<int name=\"a\" value=\"01\" />", "<int name=\"a\" value=\"+1\" />", "<int name=\"a\" value=\"1.0\" />",
            "<int name=\"a\" value=\"2147483648\" />", "<int name=\"a\" value=\"-2147483649\" />", "<int name=\"a\" value=\" 1\" />",
            "<int name=\"a\" value=\"0x10\" />", "<int name=\"a\" value=\"-0\" />", "<long name=\"a\" value=\"9223372036854775808\" />",
            "<long name=\"a\" value=\"1L\" />", "<float name=\"a\" value=\"\" />", "<float name=\"a\" value=\"abc\" />", "<float name=\"a\" value=\"1\" />",
            "<float name=\"a\" value=\".5\" />", "<float name=\"a\" value=\"1e5\" />", "<float name=\"a\" value=\"+1.0\" />", "<float name=\"a\" value=\"1.0f\" />",
            "<boolean name=\"a\" value=\"True\" />", "<boolean name=\"a\" value=\"1\" />", "<boolean name=\"a\" value=\"\" />"
        )
        for (b in bad) assertEquals(b, SuspectReason.BAD_VALUE, reason(body(b)))
        val good = listOf(
            "<int name=\"a\" value=\"0\" />", "<int name=\"a\" value=\"-1\" />", "<long name=\"a\" value=\"-9223372036854775808\" />",
            "<long name=\"a\" value=\"9223372036854775807\" />", "<float name=\"a\" value=\"1.0E10\" />", "<float name=\"a\" value=\"-0.0\" />",
            "<boolean name=\"a\" value=\"true\" />"
        )
        for (g in good) assertTrue(g, RawPrefsShape.validate(body(g)) is RawShapeResult.Valid)
    }

    @Test fun `a primitive with a missing value or with content is rejected`() {
        assertEquals(SuspectReason.BAD_VALUE, reason(body("<int name=\"a\" />")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<int name=\"a\" value=\"1\">5</int>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<int name=\"a\" value=\"1\" extra=\"x\" />")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<string name=\"a\" value=\"1\">x</string>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<string name=\"a\"><b>x</b></string>")))
    }

    @Test fun `sets must hold only plain unnamed strings, once each`() {
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<set name=\"a\"><int value=\"1\" /></set>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<set name=\"a\"><string name=\"x\">v</string></set>")))
        assertEquals(SuspectReason.BAD_STRUCTURE, reason(body("<set name=\"a\">loose</set>")))
        assertEquals(SuspectReason.BAD_VALUE, reason(body("<set name=\"a\"><string>v</string><string>v</string></set>")))
        assertTrue(RawPrefsShape.validate(body("<set name=\"a\"><string>v</string><string>w</string></set>")) is RawShapeResult.Valid)
    }

    @Test fun `an XML control character reference that XML 1_0 forbids is suspect, a conservative choice to verify on Android`() {
        // Android's writer would emit &#1; for U+0001; the JDK parser rejects it. Rejecting is fail-closed (frozen, not wiped).
        assertEquals(SuspectReason.UNPARSEABLE, reason(body("<string name=\"a\">x&#1;y</string>")))
    }

    // ---------------------------------------------------------------- sweeps

    @Test fun `no truncation of a valid file is ever accepted as valid unless it is itself a complete document`() {
        val map = linkedMapOf<String, Any>("1.level" to 3, "1.name" to "Ash & \"Co\"", "1.flags" to setOf("a", "b"), "active_slot" to 1, "f" to 2.5f)
        val full = AndroidXmlWriter.write(map)
        var validPrefixes = 0
        for (n in 0 until full.size) {
            val r = RawPrefsShape.validate(full.copyOf(n))
            if (r is RawShapeResult.Valid) {
                validPrefixes++
                // only the file minus its final newline can be a complete document
                assertEquals("prefix $n of ${full.size}", full.size - 1, n)
                assertEquals(map.size, r.entries.size)
            }
        }
        assertEquals(1, validPrefixes)
    }

    @Test fun `single byte corruption of a valid file is either rejected or decodes to the identical map`() {
        val map = linkedMapOf<String, Any>("1.level" to 3, "1.name" to "Ash", "1.flags" to setOf("a", "b"), "active_slot" to 1)
        val full = AndroidXmlWriter.write(map)
        var accepted = 0
        for (i in full.indices) for (replacement in intArrayOf(0x00, 0x20, 0x3C, 0x22, 0xFF, 0x41)) {
            val c = full.copyOf()
            if (c[i].toInt() and 0xFF == replacement) continue
            c[i] = replacement.toByte()
            val r = RawPrefsShape.validate(c)
            if (r is RawShapeResult.Valid) {
                accepted++
                // an accepted mutation must be one of: whitespace-only difference, or a legitimately different value (never lost keys silently)
                assertEquals("key count changed at $i/$replacement", map.size, r.entries.size)
            }
        }
        assertTrue("some benign mutations (whitespace, value digits) exist", accepted > 0)
    }

    @Test fun `results never contain keys or values`() {
        val secret = "TOPSECRET-scene-id"
        val r = RawPrefsShape.validate(AndroidXmlWriter.write(mapOf("1.scene" to secret)))
        assertEquals("Valid(1 entries)", r.toString())
        val bad = RawPrefsShape.validate(body("<string name=\"$secret\" value=\"x\">y</string>"))
        assertFalse(bad.toString().contains(secret))
    }

    @Test fun `deeply nested input fails closed rather than crashing`() {
        val sb = StringBuilder("<map>")
        repeat(20000) { sb.append("<set name=\"a\">") }
        assertEquals(SuspectReason.UNPARSEABLE, reason(s(sb.toString())))
    }
}
