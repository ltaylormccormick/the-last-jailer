package com.thelastjailer.app.data

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.ending.LegacyRule
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Are the fourteen legacy decisions reached, and recorded exactly once, on every road to every ending? Two kinds of evidence,
 * each sound for what it claims (see [FlagFamilyProbe]):
 *  - the structure of the choice graph, with every gate ignored (which can only make a claim harder to prove, never wrong),
 *    proves that each decision's flags are held at least once on every road to an ending and never two at a time;
 *  - real random tellings on the real transitions witness it at each ending, and show the rule reads the same answers the
 *    player gave.
 */
class LegacyDecisionProofTest {
    private val nodes: List<StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val combatTargets: (StoryNode) -> List<String> = { n ->
        n.combatEncounterId?.let { CombatRepository.encounter(it) }?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId ?: it.victoryNodeId) } ?: emptyList()
    }
    private val graph = FlagFamilyProbe.StoryGraph(nodes, combatTargets)
    private val endings = ReplayKit.ENDINGS.map { it.nodeId }

    @Test
    fun `structural proof - each of the 14 decisions is held at least once and never twice, at each of the six endings`() {
        val failures = ArrayList<String>()
        for (d in LegacyRule.decisions) {
            val family = d.options.map { it.flag }.toSet()
            for (ending in endings) {
                val proof = graph.prove(family, ending)
                if (!(proof.exhaustive && proof.exclusive)) {
                    failures += "${d.id} @ $ending: exhaustive=${proof.exhaustive} exclusive=${proof.exclusive} ${proof.notes.take(2)}"
                }
            }
        }
        println("PROOF decisions=${LegacyRule.decisions.size} endings=${endings.size} proofs=${LegacyRule.decisions.size * endings.size} failures=${failures.size}")
        assertEquals("decision families the story structure does not support", emptyList<String>(), failures)
    }

    @Test
    fun `every decision scene is on the only road to every ending`() {
        val sceneIds = listOf(
            "what_the_prisoner_offers_again", "who_stands_with_stonebeard", "old_debts", "what_must_be_broken",
            "what_the_sixth_wants", "what_kaelen_commits_to", "reaching_emberlow", "reaching_greymoor", "what_yielding_cost",
            "what_kaelen_offers_the_forgotten", "what_wraithspire_reveals", "what_must_happen_next", "what_kaelen_offers_at_the_end"
        )
        sceneIds.forEach { id ->
            val banned = graph.edges.filter { it.from == id }.toSet()
            assertTrue("$id has no way on", banned.isNotEmpty())
            endings.forEach { ending -> assertTrue("a road reaches $ending without passing $id", graph.mustPass(banned, ending)) }
        }
        // the fate decision is one decision in two variant scenes: every road passes one of them
        val fate = listOf("what_kaelen_chooses_trusted", "what_kaelen_chooses_guarded")
        val bannedFate = graph.edges.filter { it.from in fate }.toSet()
        endings.forEach { ending -> assertTrue("a road reaches $ending without a fate decision", graph.mustPass(bannedFate, ending)) }
    }

    /** Own random walks, strong enough for every stat gate; at each ending every decision must hold exactly one flag. */
    @Test
    fun `random real tellings - every ending holds exactly one flag per decision, and the rule agrees with the choices taken`() {
        val random = Random(0x30C0L)
        val perEnding = HashMap<String, Int>()
        var tellings = 0
        val violations = ArrayList<String>()
        while (tellings < 1500) {
            var s: GameState = ReplayKit.STRONG
            var steps = 0
            val taken = HashSet<String>()
            var reached = false
            while (steps++ < 4000) {
                val node = StoryRepository.node(s.sceneId)
                if (node.id == "chapter30_end") { reached = true; break }
                val encounter = node.combatEncounterId
                if (encounter != null) {
                    s = s.resolveCombat(CombatRepository.encounter(encounter), CombatOutcome(victory = true, damageTaken = 0))
                    continue
                }
                val visible = StoryRepository.visibleChoices(node, s).filter { it.nextNodeId != "fallen_knight" }
                if (visible.isEmpty()) break
                val choice = visible[random.nextInt(visible.size)]
                taken += choice.consequences.setFlags
                s = s.applyChoice(choice)
            }
            if (!reached) continue
            tellings++
            val offered = ReplayKit.arrivals(s)
            val end = s.applyChoice(offered[random.nextInt(offered.size)])
            perEnding.merge(end.sceneId, 1, Int::plus)
            val bad = LegacyRule.decisions.filter { d -> end.flags.count { f -> d.options.any { it.flag == f } } != 1 }
            if (bad.isNotEmpty()) violations += "${end.sceneId}: ${bad.map { it.id }}"
            val decisionFlags = LegacyRule.decisions.flatMap { d -> d.options.map { it.flag } }.toSet()
            val held = end.flags.filter { it in decisionFlags }.toSet()
            val chosen = taken.filter { it in decisionFlags }.toSet()
            if (LegacyRule.evaluate(held).axis != LegacyRule.evaluate(chosen).axis) violations += "${end.sceneId}: rule disagrees with the picks"
        }
        println("WALKS tellings=$tellings perEnding=$perEnding violations=${violations.size}")
        assertEquals(endings.toSet(), perEnding.keys)
        assertTrue("every ending was reached at least 100 times: $perEnding", perEnding.values.all { it >= 100 })
        assertEquals(emptyList<String>(), violations.take(5))
    }
}
