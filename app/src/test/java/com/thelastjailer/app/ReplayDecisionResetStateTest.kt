package com.thelastjailer.app

import com.thelastjailer.app.data.DARK_AID_FAMILY
import com.thelastjailer.app.data.FlagDescriptions
import com.thelastjailer.app.data.ILSEVET_FATE_FAMILY
import com.thelastjailer.app.data.ReplayKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity at the state level: what taking each of the eight decision choices does to a
 * [GameState] that already holds flags from an earlier telling.
 */
class ReplayDecisionResetStateTest {

    private fun at(nodeId: String, flags: Set<String>): GameState =
        ReplayKit.jumpTo(ReplayKit.STRONG.copy(flags = flags), nodeId)

    /** The same choice with its clears removed: how the choice behaved before the correction. */
    private fun Choice.withoutClears(): Choice = copy(consequences = consequences.copy(clearFlags = emptySet()))

    private fun familyOf(nodeId: String): Set<String> =
        if (nodeId == ReplayKit.AID_NODE) DARK_AID_FAMILY else ILSEVET_FATE_FAMILY

    @Test
    fun `with none of the family flags held, the correction changes nothing (first play)`() {
        val unrelated = setOf("trusted_voss_fully", "received_wax_disc", "refused_envoy", "ledger_end_1")
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            for (held in listOf(emptySet<String>(), unrelated)) {
                val s = at(nodeId, held)
                assertEquals(choice.label, s.applyChoice(choice.withoutClears()), s.applyChoice(choice))
            }
        }
    }

    @Test
    fun `with stale flags held, the only difference is inside the family, and exactly one flag is kept`() {
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            val members = familyOf(nodeId).toList()
            for (mask in 0 until (1 shl members.size)) {
                val held = members.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }.toSet() + "kept_unrelated"
                val s = at(nodeId, held)
                val corrected = s.applyChoice(choice)
                val before = s.applyChoice(choice.withoutClears())
                val label = "$nodeId held=$held ${choice.label}"
                assertEquals(label, corrected.copy(flags = emptySet()), before.copy(flags = emptySet()))
                assertEquals(label, choice.consequences.setFlags, corrected.flags.intersect(familyOf(nodeId)))
                assertTrue(label, "kept_unrelated" in corrected.flags)
                assertTrue(label, (corrected.flags - before.flags).isEmpty()) // the correction only ever removes
            }
        }
    }

    @Test
    fun `whatever answer was held, the telling ends with exactly the answer it chose`() {
        for (held in DARK_AID_FAMILY) {
            val s = at(ReplayKit.AID_NODE, setOf(held))
            assertEquals(setOf("accepted_dark_aid"), s.applyChoice(ReplayKit.aidChoice(true)).flags.intersect(DARK_AID_FAMILY))
            assertEquals(setOf("refused_dark_aid_again"), s.applyChoice(ReplayKit.aidChoice(false)).flags.intersect(DARK_AID_FAMILY))
        }
        for (nodeId in ReplayKit.FATE_NODES) for (held in ReplayKit.FATES) for (chosen in ReplayKit.FATES) {
            val after = at(nodeId, setOf(held)).applyChoice(ReplayKit.fateChoice(nodeId, chosen))
            assertEquals("$nodeId held=$held chosen=$chosen", setOf(chosen), after.flags.intersect(ILSEVET_FATE_FAMILY))
        }
        // All three held together (only an older save could hold that) also collapses to the one chosen.
        val everyFate = at(ReplayKit.FATE_NODES[0], ILSEVET_FATE_FAMILY)
        val after = everyFate.applyChoice(ReplayKit.fateChoice(ReplayKit.FATE_NODES[0], "deferred_ilsevets_fate"))
        assertEquals(setOf("deferred_ilsevets_fate"), after.flags.intersect(ILSEVET_FATE_FAMILY))
    }

    @Test
    fun `the chosen flag is in its own clear set and still survives, so the reset depends on clear running before set`() {
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            val own = choice.consequences.setFlags.single()
            assertTrue(choice.label, own in choice.consequences.clearFlags)
            assertTrue(choice.label, own in at(nodeId, setOf(own)).applyChoice(choice).flags)
            assertTrue(choice.label, own in at(nodeId, emptySet()).applyChoice(choice).flags)
        }
    }

    @Test
    fun `flags from other systems are never touched`() {
        val others = setOf(
            "trusted_voss_fully", "stayed_guarded", "received_wax_disc", "refused_envoy",
            "waited_at_emberlow", "asked_directly_ch22", "ledger_end_1", "ledger_end_6"
        )
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            val after = at(nodeId, others).applyChoice(choice)
            assertTrue(choice.label, after.flags.containsAll(others))
        }
    }

    @Test
    fun `stats, inventory, trophies, gold and level are not touched by the reset`() {
        val held = setOf("accepted_dark_aid", "captured_ilsevet", "showed_ilsevet_mercy")
        ReplayKit.decisionChoices().forEach { (nodeId, choice) ->
            val s = at(nodeId, held).copy(inventory = listOf("healing_draught", "ilsevets_blade"), trophies = setOf("The Watch Goes On"))
            val corrected = s.applyChoice(choice)
            val before = s.applyChoice(choice.withoutClears())
            assertEquals(choice.label, before.inventory, corrected.inventory)
            assertEquals(choice.label, before.trophies, corrected.trophies)
            assertEquals(choice.label, before.courage, corrected.courage)
            assertEquals(choice.label, before.honour, corrected.honour)
            assertEquals(choice.label, before.gold, corrected.gold)
            assertEquals(choice.label, before.level, corrected.level)
            assertEquals(choice.label, before.sceneId, corrected.sceneId)
        }
    }

    // ---- Journal: a changed answer reads as one specific line, not the neutral "more than once" line -------

    private fun journalLines(flags: Set<String>): List<String> = FlagDescriptions.sections(flags).flatMap { it.lines }

    @Test
    fun `a changed aid answer shows one specific Journal line`() {
        val after = at(ReplayKit.AID_NODE, setOf("accepted_dark_aid")).applyChoice(ReplayKit.aidChoice(false))
        val lines = journalLines(after.flags)
        assertTrue(lines.toString(), "I kept my hand closed and finished the fight on my own strength." in lines)
        assertFalse(lines.toString(), lines.any { it.contains("more than once") })
    }

    @Test
    fun `a changed Ilsevet answer shows one specific Journal line`() {
        val nodeId = ReplayKit.FATE_NODES[0]
        val after = at(nodeId, setOf("captured_ilsevet")).applyChoice(ReplayKit.fateChoice(nodeId, "showed_ilsevet_mercy"))
        val lines = journalLines(after.flags)
        assertTrue(lines.toString(), "I let Ilsevet go, on condition she never comes back armed." in lines)
        assertFalse(lines.toString(), lines.any { it.contains("more than once") })
    }
}
