package com.thelastjailer.app.saveguard

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.StrictFakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Proves the strict double really behaves like Android, and CHARACTERISES what the live code does today when it is
 * fed damaged data. The second half documents CURRENT behaviour (the G1 crash loop). It is not a requirement: Stage
 * B+C is expected to change it, and when it does these assertions are replaced deliberately in that reviewed change.
 */
class StrictDoubleTest {

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try { block() } catch (e: Throwable) {
            assertTrue("expected ${T::class.simpleName}, got $e", e is T)
            return
        }
        fail("expected ${T::class.simpleName}, nothing was thrown")
    }

    @Test
    fun `every typed getter throws ClassCastException on a type mismatch, like Android`() {
        val p = StrictFakeSharedPreferences()
            .putRaw("s", "text").putRaw("i", 7).putRaw("l", 7L).putRaw("f", 1.5f).putRaw("b", true).putRaw("set", setOf("a"))
        assertThrows<ClassCastException> { p.getInt("s", 0) }
        assertThrows<ClassCastException> { p.getInt("l", 0) }        // Long where an Int is read
        assertThrows<ClassCastException> { p.getLong("i", 0) }
        assertThrows<ClassCastException> { p.getFloat("i", 0f) }
        assertThrows<ClassCastException> { p.getBoolean("i", false) }
        assertThrows<ClassCastException> { p.getString("i", null) }
        assertThrows<ClassCastException> { p.getStringSet("s", null) }
        assertEquals(7, p.getInt("i", 0))
        assertEquals(3, p.getInt("missing", 3))
    }

    @Test
    fun `an unreadable file makes every read throw IllegalStateException`() {
        val p = StrictFakeSharedPreferences().putRaw("a", 1)
        p.unreadableCause = OutOfMemoryError("load failed")
        assertThrows<IllegalStateException> { p.all }
        assertThrows<IllegalStateException> { p.getInt("a", 0) }
        assertThrows<IllegalStateException> { p.getString("a", null) }
        assertThrows<IllegalStateException> { p.contains("a") }
    }

    @Test
    fun `the lenient double the existing tests use does not reproduce the crash`() {
        val lenient = FakeSharedPreferences()
        lenient.edit().putString("1.level", "three").apply()
        assertEquals("the lenient fake silently returns the default", 1, lenient.getInt("1.level", 1))
        val strict = StrictFakeSharedPreferences().putRaw("1.level", "three")
        assertThrows<ClassCastException> { strict.getInt("1.level", 1) }
    }

    @Test
    fun `null puts remove keys and writes are counted`() {
        val p = StrictFakeSharedPreferences().putRaw("k", "v")
        assertEquals(0, p.editCalls)
        p.edit().putString("k", null).apply()
        assertTrue(!p.contains("k"))
        assertEquals(1, p.editCalls); assertEquals(1, p.writeCalls)
    }

    // ---- CURRENT behaviour of the live code on damaged data (to be changed by Stage B+C) -------------------------

    private fun savedSlot1(): StrictFakeSharedPreferences {
        val p = StrictFakeSharedPreferences()
        SaveStore(p).save(1, GameState(activeSlot = 1))
        return p
    }

    @Test
    fun `CURRENT - G1 reproduction - a string where level is an int crashes load`() {
        val p = savedSlot1().putRaw("1.level", "three")
        assertThrows<ClassCastException> { SaveStore(p).load(1) }
    }

    @Test
    fun `CURRENT - a wrong-typed active_slot crashes currentActiveSlot`() {
        val p = savedSlot1().putRaw("active_slot", "1")
        assertThrows<ClassCastException> { SaveStore(p).currentActiveSlot() }
    }

    @Test
    fun `CURRENT - an int where the inventory is stored escapes the legacy fallback and crashes`() {
        val p = savedSlot1().putRaw("1.inventory", 5)
        assertThrows<ClassCastException> { SaveStore(p).load(1) }
    }

    @Test
    fun `CURRENT - an unreadable file crashes load`() {
        val p = savedSlot1()
        p.unreadableCause = OutOfMemoryError("load failed")
        assertThrows<IllegalStateException> { SaveStore(p).load(1) }
    }

    @Test
    fun `CURRENT - a damaged slot only crashes when it is loaded, and the Save screen loads every slot`() {
        val p = savedSlot1()
        SaveStore(p).save(2, GameState(activeSlot = 2))
        p.putRaw("2.gold", "rich")
        assertEquals("slot 1 loads", 1, SaveStore(p).load(1)?.activeSlot)
        assertThrows<ClassCastException> { SaveStore(p).load(2) }   // SaveScreen calls load() for every slot
    }

    @Test
    fun `CURRENT - scene stored without its siblings reads as an empty slot`() {
        val p = StrictFakeSharedPreferences().putRaw("3.gold", 10).putRaw("3.level", 2)
        assertEquals(null, SaveStore(p).load(3))
        assertEquals(false, SaveStore(p).hasSave(3))
    }
}
