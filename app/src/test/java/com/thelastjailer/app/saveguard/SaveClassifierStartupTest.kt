package com.thelastjailer.app.saveguard

import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.StrictFakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveClassifierStartupTest {

    private val classifier = SaveClassifier()

    private fun decision(p: StrictFakeSharedPreferences): StartupDecision = (classifier.report(p) as SaveReport.Readable).startup

    private fun withSlots(vararg slots: Int): StrictFakeSharedPreferences =
        StrictFakeSharedPreferences().also { p -> slots.forEach { SaveStore(p).save(it, GameState(activeSlot = it)) } }

    // ---- active slot marker ---------------------------------------------------------------------------------------

    @Test
    fun `active slot marker classes`() {
        fun cls(v: Any?) = classifier.classifyActiveSlot(mapOf("active_slot" to v))
        assertTrue(cls(null) is ActiveSlotClass.Absent)
        assertEquals(4, (cls(4) as ActiveSlotClass.Valid).slot)
        assertEquals(ActiveSlotFault.NON_POSITIVE, (cls(0) as ActiveSlotClass.Invalid).reason)
        assertEquals(ActiveSlotFault.NON_POSITIVE, (cls(-3) as ActiveSlotClass.Invalid).reason)
        for (bad in listOf<Any>("2", 2L, 2.0f, true, setOf("2"))) {
            assertEquals("$bad", ActiveSlotFault.WRONG_TYPE, (cls(bad) as ActiveSlotClass.Invalid).reason)
        }
    }

    // ---- startup decisions ----------------------------------------------------------------------------------------

    @Test
    fun `a brand new install is a fresh start in slot 1`() {
        val d = decision(StrictFakeSharedPreferences())
        assertEquals(1, (d as StartupDecision.FreshStart).slot)
    }

    @Test
    fun `a missing active marker with saved slots shows the chooser rather than guessing slot 1`() {
        val p = withSlots(1, 2).also { it.edit().remove("active_slot").apply() }
        val d = decision(p)
        assertEquals(ChooserReason.ACTIVE_ABSENT_WITH_DATA, (d as StartupDecision.Chooser).reason)
    }

    @Test
    fun `a missing active marker with only a Back snapshot still counts as saved data`() {
        val p = StrictFakeSharedPreferences().putRaw("2.previous.scene", "fallen_knight")
        assertEquals(ChooserReason.ACTIVE_ABSENT_WITH_DATA, (decision(p) as StartupDecision.Chooser).reason)
    }

    @Test
    fun `an invalid active marker shows the chooser, whatever else is stored`() {
        for (bad in listOf<Any>("1", 0, -1, 1L, true)) {
            assertEquals("$bad", ChooserReason.ACTIVE_INVALID,
                ((decision(withSlots(1).putRaw("active_slot", bad))) as StartupDecision.Chooser).reason)
            assertEquals("$bad empty", ChooserReason.ACTIVE_INVALID,
                (decision(StrictFakeSharedPreferences().putRaw("active_slot", bad)) as StartupDecision.Chooser).reason)
        }
    }

    @Test
    fun `an active slot that is valid resumes`() {
        val d = decision(withSlots(1, 2).also { it.edit().putInt("active_slot", 2).apply() })
        assertEquals(2, (d as StartupDecision.Resume).slot)
    }

    @Test
    fun `an active slot that points at an empty slot is a fresh start there, as today`() {
        val d = decision(withSlots(1).putRaw("active_slot", 3))
        assertEquals(3, (d as StartupDecision.FreshStart).slot)
    }

    @Test
    fun `an active slot that is Malformed needs attention and names the problem`() {
        val d = decision(withSlots(1).putRaw("1.level", "x"))
        d as StartupDecision.NeedsAttention
        assertEquals(1, d.slot)
        assertTrue(d.state is Classification.Malformed)
    }

    @Test
    fun `an active slot with an unknown scene needs attention as UnsupportedContent`() {
        val d = decision(withSlots(1).putRaw("1.scene", "from_another_version"))
        assertTrue((d as StartupDecision.NeedsAttention).state is Classification.UnsupportedContent)
    }

    @Test
    fun `a damaged slot other than the active one does not stop the active slot resuming`() {
        val p = withSlots(1, 2).putRaw("2.gold", "x").also { it.edit().putInt("active_slot", 1).apply() }
        assertEquals(1, (decision(p) as StartupDecision.Resume).slot)
        assertTrue((classifier.report(p) as SaveReport.Readable).slot(2)!!.live is Classification.Malformed)
    }

    @Test
    fun `a damaged Back snapshot does not stop the slot resuming`() {
        val p = withSlots(1)
        SaveStore(p).savePreviousState(1, GameState(activeSlot = 1))
        p.putRaw("1.previous.xp", "x")
        val r = classifier.report(p) as SaveReport.Readable
        assertEquals(1, (r.startup as StartupDecision.Resume).slot)
        assertEquals(Classification.Valid, r.slot(1)!!.live)
        assertTrue(r.slot(1)!!.back is Classification.Malformed)
    }

    @Test
    fun `an unknown scene inside the Back snapshot is UnsupportedContent for the snapshot only`() {
        val p = withSlots(1)
        SaveStore(p).savePreviousState(1, GameState(activeSlot = 1))
        p.putRaw("1.previous.scene", "gone")
        val r = classifier.report(p) as SaveReport.Readable
        assertEquals(Classification.Valid, r.slot(1)!!.live)
        assertTrue(r.slot(1)!!.back is Classification.UnsupportedContent)
    }

    @Test
    fun `a Back snapshot with other fields but no scene is Malformed`() {
        val p = withSlots(1).putRaw("1.previous.gold", 5)
        assertTrue((classifier.report(p) as SaveReport.Readable).slot(1)!!.back is Classification.Malformed)
    }

    // ---- exhaustive: marker x slot1 state x slot2 state -------------------------------------------------------------

    @Test
    fun `every combination of marker and slot states yields exactly the documented decision`() {
        val markers: List<Pair<String, Any?>> = listOf("absent" to null, "1" to 1, "2" to 2, "3" to 3, "bad" to "x", "zero" to 0)
        val states = listOf("missing", "valid", "malformed", "unsupported")
        fun build(state: String, slot: Int, p: StrictFakeSharedPreferences) {
            when (state) {
                "missing" -> Unit
                "valid" -> SaveStore(p).save(slot, GameState(activeSlot = slot))
                "malformed" -> { SaveStore(p).save(slot, GameState(activeSlot = slot)); p.putRaw("$slot.gold", "x") }
                "unsupported" -> { SaveStore(p).save(slot, GameState(activeSlot = slot)); p.putRaw("$slot.scene", "alien") }
            }
        }
        var combos = 0
        for ((mName, m) in markers) for (s1 in states) for (s2 in states) {
            val p = StrictFakeSharedPreferences()
            build(s1, 1, p); build(s2, 2, p)
            p.edit().remove("active_slot").apply()
            if (m != null) p.putRaw("active_slot", m)
            val d = decision(p)
            val anyData = s1 != "missing" || s2 != "missing"
            val label = "marker=$mName slot1=$s1 slot2=$s2 -> $d"
            when {
                mName == "bad" || mName == "zero" -> assertTrue(label, d is StartupDecision.Chooser && d.reason == ChooserReason.ACTIVE_INVALID)
                m == null && anyData -> assertTrue(label, d is StartupDecision.Chooser && d.reason == ChooserReason.ACTIVE_ABSENT_WITH_DATA)
                m == null -> assertTrue(label, d is StartupDecision.FreshStart && d.slot == 1)
                else -> {
                    val slot = m as Int
                    val st = when (slot) { 1 -> s1; 2 -> s2; else -> "missing" }
                    when (st) {
                        "missing" -> assertTrue(label, d is StartupDecision.FreshStart && d.slot == slot)
                        "valid" -> assertTrue(label, d is StartupDecision.Resume && d.slot == slot)
                        else -> assertTrue(label, d is StartupDecision.NeedsAttention && d.slot == slot)
                    }
                }
            }
            combos++
        }
        assertEquals(6 * 4 * 4, combos)
    }
}
