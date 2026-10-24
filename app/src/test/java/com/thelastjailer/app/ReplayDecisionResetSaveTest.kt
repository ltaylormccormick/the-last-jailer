package com.thelastjailer.app

import com.thelastjailer.app.data.DARK_AID_FAMILY
import com.thelastjailer.app.data.ILSEVET_FATE_FAMILY
import com.thelastjailer.app.data.ReplayKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity through the save store: a decision is committed, saved and reloaded, older saves that
 * already hold stale flags are left alone until the decision is taken again, slots stay independent, and Back
 * restores the exact state from before the choice.
 */
class ReplayDecisionResetSaveTest {

    private fun at(nodeId: String, flags: Set<String>, slot: Int = 1): GameState =
        ReplayKit.jumpTo(ReplayKit.STRONG.copy(activeSlot = slot, flags = flags), nodeId)

    // ---- save and load --------------------------------------------------------------------------------------

    @Test
    fun `the decision survives a commit and a reload`() {
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        val node = ReplayKit.FATE_NODES[0]
        val stale = at(node, setOf("captured_ilsevet", "accepted_dark_aid"))
        val after = store.commitChoice(stale, ReplayKit.fateChoice(node, "showed_ilsevet_mercy"))
        assertEquals(setOf("showed_ilsevet_mercy"), after.flags.intersect(ILSEVET_FATE_FAMILY))
        // Compare the parts under test: a reload recomputes some derived fields (such as max health).
        val reloaded = SaveStore(prefs).load(1)
        assertNotNull(reloaded)
        assertEquals(after.flags, reloaded!!.flags)
        assertEquals(after.sceneId, reloaded.sceneId)
    }

    @Test
    fun `a save taken mid-telling with stale flags is loaded untouched and heals at the decision`() {
        val prefs = FakeSharedPreferences()
        val staleFlags = setOf("accepted_dark_aid", "refused_dark_aid_again")
        SaveStore(prefs).save(1, at(ReplayKit.AID_NODE, staleFlags))

        val reopened = SaveStore(prefs)
        val loaded = reopened.load(1)!!
        assertEquals("loading never edits flags", staleFlags, loaded.flags)

        val after = reopened.commitChoice(loaded, ReplayKit.aidChoice(false))
        assertEquals(setOf("refused_dark_aid_again"), after.flags)
    }

    @Test
    fun `a save that is already stale is not repaired on load (older saves are left as they are)`() {
        val prefs = FakeSharedPreferences()
        val stale = at("chapter30_end", setOf("captured_ilsevet", "showed_ilsevet_mercy", "accepted_dark_aid"))
        SaveStore(prefs).save(1, stale)
        val loaded = SaveStore(prefs).load(1)!!
        assertEquals(stale.flags, loaded.flags)
        assertEquals("the old behaviour is kept until the decision is taken again", 2, ReplayKit.arrivals(loaded).size)
    }

    @Test
    fun `a change of answer in one slot leaves the other slot alone`() {
        val store = SaveStore(FakeSharedPreferences())
        val node = ReplayKit.FATE_NODES[0]
        store.save(2, at(node, setOf("captured_ilsevet"), slot = 2))
        store.commitChoice(at(node, setOf("captured_ilsevet"), slot = 1), ReplayKit.fateChoice(node, "showed_ilsevet_mercy"))
        assertEquals(setOf("captured_ilsevet"), store.load(2)!!.flags)
    }

    @Test
    fun `a replay saved through the store keeps the carried flags until the decision, then holds one answer`() {
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        // The end of an aided, captured telling.
        val finished = at("chapter30_end", setOf("accepted_dark_aid", "captured_ilsevet"))
        val onEnding = store.commitChoice(finished, ReplayKit.arrivals(finished).single())
        val replayed = store.commitChoice(onEnding, ReplayKit.loopChoice(onEnding.sceneId))
        assertTrue("the replay itself carries every flag", replayed.flags.containsAll(finished.flags))

        // Second telling: unaided, and mercy.
        val unaided = store.commitChoice(ReplayKit.jumpTo(replayed, ReplayKit.AID_NODE), ReplayKit.aidChoice(false))
        val node = ReplayKit.FATE_NODES[1]
        val mercy = store.commitChoice(ReplayKit.jumpTo(unaided, node), ReplayKit.fateChoice(node, "showed_ilsevet_mercy"))

        val reloaded = SaveStore(prefs).load(1)!!
        // What the player sees first: one arrival button, and it is this telling's ending.
        val shown = ReplayKit.arrivals(ReplayKit.jumpTo(reloaded, "chapter30_end"))
        assertEquals("arrival buttons", 1, shown.size)
        assertEquals("the_watch_continues_unaided_mercy", shown.single().nextNodeId)
        // Then the cause: the saved flags hold only this telling's answers.
        assertEquals(setOf("refused_dark_aid_again"), reloaded.flags.intersect(DARK_AID_FAMILY))
        assertEquals(setOf("showed_ilsevet_mercy"), reloaded.flags.intersect(ILSEVET_FATE_FAMILY))
        assertEquals(mercy.flags, reloaded.flags)
    }

    // ---- Back -----------------------------------------------------------------------------------------------

    @Test
    fun `Back after any of the eight choices restores the exact pre-choice flags`() {
        val stale = setOf("accepted_dark_aid", "captured_ilsevet", "showed_ilsevet_mercy", "kept_unrelated")
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            val store = SaveStore(FakeSharedPreferences())
            val before = at(nodeId, stale)
            store.commitChoice(before, choice)
            val back = store.restorePreviousState(1)
            assertNotNull(choice.label, back)
            assertEquals(choice.label, before.flags, back!!.flags)
            assertEquals(choice.label, before.sceneId, back.sceneId)
        }
    }

    @Test
    fun `Back and then the other answer gives the fresh outcome, never a mixture`() {
        val store = SaveStore(FakeSharedPreferences())
        val before = at(ReplayKit.AID_NODE, setOf("accepted_dark_aid")) // stale from an earlier telling
        store.commitChoice(before, ReplayKit.aidChoice(true))
        val back = store.restorePreviousState(1)!!
        assertEquals(before.flags, back.flags)
        val again = store.commitChoice(back, ReplayKit.aidChoice(false))
        assertEquals(setOf("refused_dark_aid_again"), again.flags)
    }

    @Test
    fun `Back after a fate answer and then a different fate gives one fate`() {
        val store = SaveStore(FakeSharedPreferences())
        val node = ReplayKit.FATE_NODES[0]
        val before = at(node, setOf("captured_ilsevet"))
        store.commitChoice(before, ReplayKit.fateChoice(node, "deferred_ilsevets_fate"))
        val back = store.restorePreviousState(1)!!
        val again = store.commitChoice(back, ReplayKit.fateChoice(node, "showed_ilsevet_mercy"))
        assertEquals(setOf("showed_ilsevet_mercy"), again.flags.intersect(ILSEVET_FATE_FAMILY))
    }
}
