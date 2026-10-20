package com.thelastjailer.app.saveguard

import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.StrictFakeSharedPreferences
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveClassifierSlotTest {

    private val classifier = SaveClassifier()

    private fun entries(p: StrictFakeSharedPreferences): Map<String, Any?> = p.all.toMap()
    private fun live(p: StrictFakeSharedPreferences, slot: Int = 1) = classifier.classifyLive(entries(p), slot)

    private fun saved(state: GameState = GameState(activeSlot = 1), slot: Int = 1): StrictFakeSharedPreferences =
        StrictFakeSharedPreferences().also { SaveStore(it).save(slot, state.copy(activeSlot = slot)) }

    private fun malformedKeys(c: Classification): List<String> = (c as Classification.Malformed).faults.map { it.key }

    // ---- the layout is pinned to the real writer ---------------------------------------------------------------

    @Test
    fun `a slot written by SaveStore classifies Valid, and the layout matches exactly what the writer stores`() {
        val state = GameState(
            sceneId = "fallen_knight", courage = 3, honour = 2, health = 80, gold = 40, level = 2, xp = 30,
            inventory = listOf("healing_draught", "dwarven_token"), trophies = setOf("t1"), flags = setOf("f1", "f2")
        )
        val p = saved(state)
        assertEquals(Classification.Valid, live(p))
        val stored = p.keys().filter { it.startsWith("1.") }.map { it.removePrefix("1.") }.toSet()
        val expected = SaveLayout.FIELDS.map { it.name }.toSet()
        val allowedExtra = SaveLayout.AUXILIARY.toSet()
        assertEquals("the writer stores a field the classifier does not know", emptySet<String>(), stored - expected - allowedExtra)
        assertEquals("the classifier expects a field the writer no longer stores", emptySet<String>(), expected - stored)
        assertEquals(ActiveSlotClass.Valid::class, classifier.classifyActiveSlot(entries(p))::class)
    }

    @Test
    fun `the Back snapshot written by SaveStore classifies Valid under the Back prefix`() {
        val p = saved()
        SaveStore(p).savePreviousState(1, GameState(activeSlot = 1, gold = 99))
        assertEquals(Classification.Valid, classifier.classifyBack(entries(p), 1))
        val stored = p.keys().filter { it.startsWith("1.previous.") }.map { it.removePrefix("1.previous.") }.toSet()
        assertEquals(SaveLayout.FIELDS.map { it.name }.toSet(), stored)
    }

    // ---- missing -------------------------------------------------------------------------------------------------

    @Test
    fun `nothing stored is Missing`() {
        val p = StrictFakeSharedPreferences()
        assertEquals(Classification.Missing, live(p))
        assertEquals(Classification.Missing, classifier.classifyBack(entries(p), 1))
        assertEquals(emptyList<Int>(), classifier.slotsWithData(entries(p)))
    }

    @Test
    fun `a slot holding only timeline keys is Missing, matching hasSave today`() {
        val p = StrictFakeSharedPreferences().putRaw("1.stamp", "x").putRaw("1.lineage", "y").putRaw("1.arrivals", 5)
        assertEquals(Classification.Missing, live(p))
        assertEquals(emptyList<Int>(), classifier.slotsWithData(entries(p)))
        assertFalse(SaveStore(p).hasSave(1))
    }

    @Test
    fun `null values count as absent`() {
        val p = StrictFakeSharedPreferences()
        val e = mapOf<String, Any?>("1.scene" to null, "1.gold" to null)
        assertEquals(Classification.Missing, classifier.classifyLive(e, 1))
        assertEquals(emptyList<Int>(), classifier.slotsWithData(e))
        assertEquals(0, p.editCalls)
    }

    // ---- historical saves must not look damaged ------------------------------------------------------------------

    @Test
    fun `an old save that lacks fields is Valid, because the loader defaults them`() {
        val p = StrictFakeSharedPreferences().putRaw("1.scene", "fallen_knight")
        assertEquals("only the scene", Classification.Valid, live(p))
        p.putRaw("1.chapter", "chapter_1").putRaw("1.courage", 2).putRaw("1.gold", 30).putRaw("1.health", 90)
        assertEquals("a mid-history subset", Classification.Valid, live(p))
        assertEquals("and the loader really does read it", 30, SaveStore(p).load(1)?.gold)
    }

    @Test
    fun `the pre-rework StringSet inventory is Valid, as SaveStore still reads it`() {
        val p = StrictFakeSharedPreferences().putRaw("1.scene", "fallen_knight").putRaw("1.inventory", setOf("dwarven_token"))
        assertEquals(Classification.Valid, live(p))
        assertEquals(listOf("dwarven_token"), SaveStore(p).load(1)?.inventory)
    }

    @Test
    fun `unknown extra keys under a slot do not make it damaged`() {
        val p = saved().putRaw("1.futureThing", 5).putRaw("1.anotherThing", "x")
        assertEquals(Classification.Valid, live(p))
    }

    // ---- wrong types: every field, several wrong types ----------------------------------------------------------

    private fun wrongValuesFor(kind: FieldKind): List<Any> = when (kind) {
        FieldKind.INT -> listOf("seven", 7L, 7.5f, true, setOf("a"))
        FieldKind.STRING -> listOf(7, 7L, 7.5f, true, setOf("a"))
        FieldKind.STRING_SET -> listOf("seven", 7, 7L, 7.5f, true, setOf(1, 2))   // last: a set of non-strings
    }

    @Test
    fun `a wrong-typed value in any field is Malformed, naming that key, for every wrong type`() {
        var cases = 0
        for (field in SaveLayout.FIELDS) {
            val bad = wrongValuesFor(field.kind).filter { v ->
                // the legacy inventory form is legitimately accepted
                !(field.alsoAccepts != null && ((field.alsoAccepts == FieldKind.STRING_SET && v is Set<*> && v.all { it is String })))
            }
            for (v in bad) {
                val p = saved().putRaw("1.${field.name}", v)
                val c = live(p)
                assertTrue("${field.name}=$v should be Malformed, was $c", c is Classification.Malformed)
                assertEquals(listOf("1.${field.name}"), malformedKeys(c))
                assertEquals(FaultReason.WRONG_TYPE, (c as Classification.Malformed).faults.single().reason)
                cases++
            }
        }
        assertTrue("matrix too small: $cases", cases > 60)
    }

    @Test
    fun `a fault reports the type found and never the stored value`() {
        val p = saved().putRaw("1.gold", "SECRET-VALUE-123")
        val fault = (live(p) as Classification.Malformed).faults.single()
        assertEquals("String", fault.foundType)
        assertFalse(fault.toString().contains("SECRET"))
        assertFalse(live(p).toString().contains("SECRET"))
    }

    @Test
    fun `several wrong fields are all reported`() {
        val p = saved().putRaw("1.gold", "x").putRaw("1.flags", 3).putRaw("1.level", 2L)
        assertEquals(setOf("1.gold", "1.flags", "1.level"), malformedKeys(live(p)).toSet())
    }

    // ---- scene problems ------------------------------------------------------------------------------------------

    @Test
    fun `other fields present without a scene is Malformed, not an empty slot`() {
        val p = StrictFakeSharedPreferences().putRaw("3.gold", 10).putRaw("3.level", 2)
        val c = classifier.classifyLive(entries(p), 3)
        assertEquals(FaultReason.SCENE_MISSING_WITH_OTHER_FIELDS, (c as Classification.Malformed).faults.single().reason)
        assertEquals(listOf(3), classifier.slotsWithData(entries(p)))
    }

    @Test
    fun `an empty scene string is Malformed`() {
        val c = live(saved().putRaw("1.scene", ""))
        assertEquals(FaultReason.EMPTY_SCENE, (c as Classification.Malformed).faults.single().reason)
    }

    @Test
    fun `a scene that is not a string is Malformed`() {
        assertEquals(listOf("1.scene"), malformedKeys(live(saved().putRaw("1.scene", 12))))
    }

    @Test
    fun `an intact slot whose scene this build does not know is UnsupportedContent, not Malformed`() {
        val c = live(saved().putRaw("1.scene", "a_scene_from_a_later_version"))
        assertTrue("was $c", c is Classification.UnsupportedContent)
        assertEquals("a_scene_from_a_later_version", (c as Classification.UnsupportedContent).sceneId)
        assertFalse("the printed form carries no scene id", c.toString().contains("later_version"))
    }

    @Test
    fun `Malformed wins over UnsupportedContent`() {
        val c = live(saved().putRaw("1.scene", "unknown_scene").putRaw("1.level", "high"))
        assertTrue(c is Classification.Malformed)
    }

    @Test
    fun `scene existence comes from the supplied catalogue`() {
        val strictCatalogue = SaveClassifier(sceneExists = { it == "only_this" })
        val p = saved().putRaw("1.scene", "only_this")
        assertEquals(Classification.Valid, strictCatalogue.classifyLive(entries(p), 1))
        assertTrue(strictCatalogue.classifyLive(entries(saved()), 1) is Classification.UnsupportedContent)
    }

    // ---- many slots, ordering, independence ----------------------------------------------------------------------

    @Test
    fun `slots are classified independently and listed in ascending order`() {
        val p = StrictFakeSharedPreferences()
        val store = SaveStore(p)
        store.save(3, GameState(activeSlot = 3)); store.save(1, GameState(activeSlot = 1)); store.save(2, GameState(activeSlot = 2))
        p.putRaw("2.honour", "bad")
        val r = classifier.report(p) as SaveReport.Readable
        assertEquals(listOf(1, 2, 3), r.slots.map { it.slot })
        assertEquals(Classification.Valid, r.slot(1)!!.live)
        assertTrue(r.slot(2)!!.live is Classification.Malformed)
        assertEquals(Classification.Valid, r.slot(3)!!.live)
    }

    @Test
    fun `huge or odd slot numbers in keys are ignored safely`() {
        val e = mapOf<String, Any?>("99999999999.scene" to "x", "0.scene" to "x", "-1.scene" to "x", "a.scene" to "x", ".scene" to "x", "1scene" to "x")
        assertEquals(emptyList<Int>(), classifier.slotsWithData(e))
    }

    // ---- unreadable ----------------------------------------------------------------------------------------------

    @Test
    fun `an unreadable file is reported as Unreadable, never as Missing`() {
        val p = saved()
        p.unreadableCause = OutOfMemoryError("load failed")
        assertEquals(SaveReport.Unreadable, classifier.report(p))
        assertEquals(EntitlementReport.Unreadable, classifier.classifyEntitlements(p))
    }

    // ---- the classifier is read-only and never throws -------------------------------------------------------------

    @Test
    fun `classification never writes`() {
        val p = saved().putRaw("1.gold", "x").putRaw("active_slot", "bad").putRaw("2.gold", 4)
        val before = p.fingerprint()
        val editsBefore = p.editCalls
        classifier.report(p); classifier.classifyEntitlements(p)
        assertEquals(before, p.fingerprint())
        assertEquals(editsBefore, p.editCalls)
        assertEquals(1, p.writeCalls)   // only the seeding save()
    }

    @Test
    fun `fuzz - arbitrary garbage never makes the classifier throw or write`() {
        val rnd = Random(20261020)
        val names = SaveLayout.FIELDS.map { it.name } + listOf("previous.scene", "previous.gold", "stamp", "lineage", "arrivals", "junk")
        val values = listOf<Any?>("s", "", 0, -4, Int.MAX_VALUE, 5L, 1.5f, true, false, emptySet<String>(), setOf("a", "b"), setOf(1), null)
        repeat(3000) {
            val p = StrictFakeSharedPreferences()
            repeat(rnd.nextInt(25)) {
                val slot = rnd.nextInt(4)
                val key = if (rnd.nextInt(12) == 0) listOf("active_slot", "unlocked_full_story", "x.y", "", ".")[rnd.nextInt(5)]
                          else "$slot.${names[rnd.nextInt(names.size)]}"
                p.putRaw(key, values[rnd.nextInt(values.size)])
            }
            val writesBefore = p.writeCalls
            val report = classifier.report(p)
            classifier.classifyEntitlements(p)
            assertTrue(report is SaveReport.Readable)
            assertEquals(writesBefore, p.writeCalls)
            assertEquals(0, p.editCalls)
        }
    }
}
