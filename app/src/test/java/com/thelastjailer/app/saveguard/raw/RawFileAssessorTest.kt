package com.thelastjailer.app.saveguard.raw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RawFileAssessorTest {

    private val saved = linkedMapOf<String, Any>("active_slot" to 1, "1.level" to 4, "1.name" to "Ash", "1.flags" to setOf("x"), "1.f" to 0.5f, "1.big" to 5L, "1.b" to true)
    private val savedBytes = AndroidXmlWriter.write(saved)
    private val empty = LoadedPrefs.Entries(emptyMap<String, Any>())
    private val full = LoadedPrefs.Entries(saved)

    private fun assess(main: RawSource, bak: RawSource = RawSource.Absent, loaded: LoadedPrefs) = RawFileAssessor.assess(main, bak, loaded)

    @Test fun `no files and an empty load is the only absent case`() {
        val a = assess(RawSource.Absent, loaded = empty)
        assertEquals(RawClass.ABSENT, a.cls)
        assertTrue(a.freshStartEligible)
        assertFalse(a.snapshotRequired)
    }

    @Test fun `no file but a non-empty load is a mismatch, never fresh`() {
        val a = assess(RawSource.Absent, loaded = full)
        assertEquals(SuspectReason.MISMATCH, a.reason)
        assertFalse(a.freshStartEligible)
    }

    @Test fun `a canonical empty file with an empty load is a legitimate empty start`() {
        val a = assess(RawSource.Bytes(AndroidXmlWriter.write(emptyMap())), loaded = empty)
        assertEquals(RawClass.LEGIT_EMPTY, a.cls)
        assertTrue(a.freshStartEligible)
    }

    @Test fun `a valid file whose content matches the loaded map is valid and not fresh`() {
        val a = assess(RawSource.Bytes(savedBytes), loaded = full)
        assertEquals(RawClass.VALID, a.cls)
        assertFalse(a.freshStartEligible)
        assertFalse(a.snapshotRequired)
    }

    @Test fun `the dangerous case, a damaged file that Android loaded as empty, is suspect and snapshot-worthy`() {
        for (n in listOf(1, 10, savedBytes.size / 2, savedBytes.size - 20)) {
            val a = assess(RawSource.Bytes(savedBytes.copyOf(n)), loaded = empty)
            assertEquals("cut at $n", RawClass.SUSPECT, a.cls)
            assertFalse(a.freshStartEligible)
            assertTrue(a.snapshotRequired)
            assertTrue(a.canSnapshot)
        }
    }

    @Test fun `zero length file is suspect`() {
        val a = assess(RawSource.Bytes(ByteArray(0)), loaded = empty)
        assertEquals(SuspectReason.ZERO_LENGTH, a.reason)
        assertFalse(a.freshStartEligible)
    }

    @Test fun `a valid but empty non-canonical spelling is not trusted as fresh`() {
        val bytes = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<!-- c --><map />".toByteArray()
        assertFalse(assess(RawSource.Bytes(bytes), loaded = empty).freshStartEligible)
    }

    @Test fun `unreadable, oversized and unloadable sources are suspect with the right reason and snapshot ability`() {
        val u = assess(RawSource.Unreadable, loaded = empty)
        assertEquals(SuspectReason.UNREADABLE, u.reason); assertFalse(u.canSnapshot); assertFalse(u.freshStartEligible)
        val t = assess(RawSource.TooLarge, loaded = empty)
        assertEquals(SuspectReason.TOO_LARGE, t.reason); assertFalse(t.canSnapshot); assertFalse(t.freshStartEligible)
        val p = assess(RawSource.Bytes(savedBytes), loaded = LoadedPrefs.Unreadable)
        assertEquals(SuspectReason.UNREADABLE_PREFS, p.reason); assertFalse(p.freshStartEligible)
    }

    @Test fun `a bak file is the effective source and is never a fresh start`() {
        val a = assess(RawSource.Bytes(savedBytes), RawSource.Bytes(savedBytes), full)
        assertEquals(RawClass.VALID, a.cls)
        assertTrue(a.bakPresent)
        assertTrue("an interrupted write must be kept", a.snapshotRequired)
        assertFalse(a.freshStartEligible)
    }

    @Test fun `an empty bak beside a main file is a stale bak, never fresh`() {
        val a = assess(RawSource.Bytes(savedBytes), RawSource.Bytes(AndroidXmlWriter.write(emptyMap())), empty)
        assertEquals(SuspectReason.STALE_BAK, a.reason)
        assertFalse(a.freshStartEligible)
        assertTrue(a.snapshotRequired)
        val b = assess(RawSource.Absent, RawSource.Bytes(AndroidXmlWriter.write(emptyMap())), empty)
        assertEquals(SuspectReason.STALE_BAK, b.reason)
    }

    @Test fun `a bak is compared with what was loaded, not the main file`() {
        val older = linkedMapOf<String, Any>("active_slot" to 1, "1.level" to 1)
        val a = assess(RawSource.Bytes(savedBytes), RawSource.Bytes(AndroidXmlWriter.write(older)), LoadedPrefs.Entries(older))
        assertEquals(RawClass.VALID, a.cls)
        val b = assess(RawSource.Bytes(savedBytes), RawSource.Bytes(AndroidXmlWriter.write(older)), full)
        assertEquals(SuspectReason.MISMATCH, b.reason)
    }

    @Test fun `every cross-check difference is a mismatch`() {
        fun differs(mutate: (MutableMap<String, Any>) -> Unit) {
            val loaded = LinkedHashMap(saved); mutate(loaded)
            val a = assess(RawSource.Bytes(savedBytes), loaded = LoadedPrefs.Entries(loaded))
            assertEquals(SuspectReason.MISMATCH, a.reason)
            assertFalse(a.freshStartEligible)
        }
        differs { it.remove("1.b") }
        differs { it["extra"] = 1 }
        differs { it["1.level"] = 5 }
        differs { it["1.level"] = 4L }          // int vs long
        differs { it["1.f"] = 0.25f }
        differs { it["1.f"] = 0.5 }             // float vs double
        differs { it["1.name"] = "Bob" }
        differs { it["1.flags"] = setOf("y") }
        differs { it["1.flags"] = "x" }
        differs { it["1.b"] = false }
        differs { it.remove("1.level"); it["other"] = 4 }
    }

    @Test fun `NaN and negative zero compare by bits`() {
        val map = mapOf<String, Any>("n" to Float.NaN, "z" to -0.0f)
        assertEquals(RawClass.VALID, assess(RawSource.Bytes(AndroidXmlWriter.write(map)), loaded = LoadedPrefs.Entries(map)).cls)
        val flipped = mapOf<String, Any>("n" to Float.NaN, "z" to 0.0f)
        assertEquals(SuspectReason.MISMATCH, assess(RawSource.Bytes(AndroidXmlWriter.write(map)), loaded = LoadedPrefs.Entries(flipped)).reason)
    }

    @Test fun `property, only absent or canonical empty with no bak and an empty load can be a fresh start`() {
        val mains = listOf(RawSource.Absent, RawSource.Unreadable, RawSource.TooLarge, RawSource.Bytes(ByteArray(0)), RawSource.Bytes(savedBytes),
            RawSource.Bytes(AndroidXmlWriter.write(emptyMap())), RawSource.Bytes(savedBytes.copyOf(30)), RawSource.Bytes("<map />junk".toByteArray()))
        val baks = listOf(RawSource.Absent, RawSource.Unreadable, RawSource.TooLarge, RawSource.Bytes(ByteArray(0)), RawSource.Bytes(savedBytes),
            RawSource.Bytes(AndroidXmlWriter.write(emptyMap())))
        val loads = listOf(empty, full, LoadedPrefs.Unreadable)
        for (m in mains) for (b in baks) for (l in loads) {
            val a = assess(m, b, l)
            if (a.freshStartEligible) {
                assertTrue(l is LoadedPrefs.Entries && l.map.isEmpty())
                assertTrue(b is RawSource.Absent)
                assertTrue(m is RawSource.Absent || (m is RawSource.Bytes && RawPrefsShape.isCanonicalEmptyMap(m.bytes)))
                assertEquals(null, a.reason)
            }
            if (a.cls == RawClass.SUSPECT) assertFalse(a.freshStartEligible)
            assertTrue(a.toString().none { it == '<' })
        }
    }

    @Test fun `the fresh-start rule itself refuses a bak even if an assessment were built by hand`() {
        assertFalse(RawAssessment(RawClass.ABSENT, null, true).freshStartEligible)
        assertFalse(RawAssessment(RawClass.LEGIT_EMPTY, null, true).freshStartEligible)
        assertTrue(RawAssessment(RawClass.ABSENT, null, false).freshStartEligible)
        assertTrue(RawAssessment(RawClass.LEGIT_EMPTY, null, true).snapshotRequired)
    }

    @Test fun `valid has no reason`() {
        assertNull(assess(RawSource.Bytes(savedBytes), loaded = full).reason)
    }
}
