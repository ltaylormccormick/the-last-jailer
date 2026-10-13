package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.data.ChoiceReachabilityAnalyzer.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Path-aware dead-choice regression tests. The strict "every node is reachable" test lives in
 * [StoryContentIntegrityTest] and is deliberately untouched; this adds the question that test
 * cannot answer: *can the player ever actually arrive at a gated choice in a state that satisfies
 * it?* The first half exercises the analyzer on tiny hand-built stories; the second half runs it on
 * the real story.
 */
class ChoiceReachabilityTest {

    // ---------- analyzer behaviour on synthetic stories ----------

    private fun node(id: String, vararg choices: Choice, combat: String? = null) =
        StoryNode(id, "chapter_1", id, "ill", "text", choices.toList(), combat)

    private fun go(to: String, label: String = "to $to", req: ChoiceRequirement? = null, c: Consequences = Consequences()) =
        Choice(label, to, req, c)

    private fun analyse(
        vararg nodes: StoryNode,
        combat: Map<String, List<String>> = emptyMap(),
        max: Int = 100_000
    ) = ChoiceReachabilityAnalyzer(
        nodes = nodes.toList(),
        combatTargets = { combat[it.combatEncounterId].orEmpty() },
        startNodeId = "start",
        maxStates = max
    ).analyse()

    private fun statusOf(v: List<ChoiceReachabilityAnalyzer.Verdict>, nodeId: String, label: String) =
        v.single { it.nodeId == nodeId && it.choice.label == label }.status

    @Test
    fun `a flag set earlier on the path makes a gated choice alive`() {
        val v = analyse(
            node("start", go("a", c = Consequences(setFlags = setOf("f")))),
            node("a", go("end", "gated", ChoiceRequirement(requiredFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.ALIVE, statusOf(v, "a", "gated"))
    }

    @Test
    fun `a flag that exists in the story but is never set before the node is dead`() {
        // 'f' is set, but only AFTER the gated choice, so presence-checking would wrongly pass it.
        val v = analyse(
            node("start", go("a")),
            node("a", go("b", "gated", ChoiceRequirement(requiredFlags = setOf("f"))), go("b", "plain")),
            node("b", go("end", c = Consequences(setFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "a", "gated"))
    }

    @Test
    fun `a flag set only on a different branch is dead on the other branch`() {
        val v = analyse(
            node("start", go("left", "left", c = Consequences(setFlags = setOf("f"))), go("right")),
            node("left", go("end")),
            node("right", go("end", "gated", ChoiceRequirement(requiredFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "right", "gated"))
    }

    @Test
    fun `clearFlags can make an otherwise set flag dead`() {
        val v = analyse(
            node("start", go("a", c = Consequences(setFlags = setOf("f")))),
            node("a", go("b", c = Consequences(clearFlags = setOf("f")))),
            node("b", go("end", "gated", ChoiceRequirement(requiredFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "b", "gated"))
    }

    @Test
    fun `a flag cleared and set in the same choice stays alive`() {
        val v = analyse(
            node("start", go("a", c = Consequences(setFlags = setOf("f")))),
            node("a", go("b", c = Consequences(clearFlags = setOf("f"), setFlags = setOf("f")))),
            node("b", go("end", "gated", ChoiceRequirement(requiredFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.ALIVE, statusOf(v, "b", "gated"))
    }

    @Test
    fun `a forbidden flag that is always set on the only path makes the choice dead`() {
        val v = analyse(
            node("start", go("a", c = Consequences(setFlags = setOf("f")))),
            node("a", go("end", "gated", ChoiceRequirement(forbiddenFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "a", "gated"))
    }

    @Test
    fun `a forbidden flag is alive when some path avoids it`() {
        val v = analyse(
            node("start", go("a", "set it", c = Consequences(setFlags = setOf("f"))), go("a", "skip it")),
            node("a", go("end", "gated", ChoiceRequirement(forbiddenFlags = setOf("f")))),
            node("end")
        )
        assertEquals(Status.ALIVE, statusOf(v, "a", "gated"))
    }

    @Test
    fun `stat gates are evaluated against stats accumulated along the path`() {
        val gain = Consequences(statDeltas = mapOf(StatType.COURAGE to 2))
        val v = analyse(
            node("start", go("a", c = gain)),
            node(
                "a",
                go("end", "reachable", ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3))), // 1 + 2
                go("end", "too high", ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 4)))
            ),
            node("end")
        )
        assertEquals(Status.ALIVE, statusOf(v, "a", "reachable"))
        assertEquals(Status.DEAD, statusOf(v, "a", "too high"))
    }

    @Test
    fun `honour lost on one branch is not counted on another`() {
        val v = analyse(
            node(
                "start",
                go("a", "gain", c = Consequences(statDeltas = mapOf(StatType.HONOUR to 2))),
                go("a", "lose", c = Consequences(statDeltas = mapOf(StatType.HONOUR to -1)))
            ),
            node("a", go("end", "gated", ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 2)))),
            node("end")
        )
        assertEquals(Status.ALIVE, statusOf(v, "a", "gated"))
    }

    @Test
    fun `a higher-stat path never hides a lower-stat path that carries a different flag`() {
        // Branch "strong": Honour 2, no flag. Branch "flagged": Honour 0, flag f. No path has both,
        // so a choice needing f AND Honour 2 is dead; one needing only f must stay alive.
        val v = analyse(
            node(
                "start",
                go("a", "strong", c = Consequences(statDeltas = mapOf(StatType.HONOUR to 2))),
                go("a", "flagged", c = Consequences(setFlags = setOf("f")))
            ),
            node(
                "a",
                go("end", "both", ChoiceRequirement(requiredFlags = setOf("f"), minStats = mapOf(StatType.HONOUR to 2))),
                go("end", "flag only", ChoiceRequirement(requiredFlags = setOf("f")))
            ),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "a", "both"))
        assertEquals(Status.ALIVE, statusOf(v, "a", "flag only"))
    }

    @Test
    fun `combat victory and defeat both lead onward`() {
        val v = analyse(
            node("start", go("fight")),
            node("fight", combat = "enc"),
            node("won", go("end", "gated-won", ChoiceRequirement())),
            node("lost", go("end", "gated-lost", ChoiceRequirement())),
            node("end"),
            combat = mapOf("enc" to listOf("won", "lost"))
        )
        assertEquals(Status.ALIVE, statusOf(v, "won", "gated-won"))
        assertEquals(Status.ALIVE, statusOf(v, "lost", "gated-lost"))
    }

    @Test
    fun `a combat node's own choices are not treated as reachable edges`() {
        val v = analyse(
            node("start", go("fight")),
            node("fight", go("hidden", "ignored"), combat = "enc"),
            node("won", go("end")),
            node("hidden", go("end", "gated", ChoiceRequirement())),
            node("end"),
            combat = mapOf("enc" to listOf("won"))
        )
        assertEquals(Status.DEAD, statusOf(v, "hidden", "gated"))
    }

    @Test
    fun `a choice on an unreachable node is dead`() {
        val v = analyse(
            node("start", go("end")),
            node("orphan", go("end", "gated", ChoiceRequirement())),
            node("end")
        )
        assertEquals(Status.DEAD, statusOf(v, "orphan", "gated"))
    }

    @Test
    fun `replay edges back to the start do not make later flags available early`() {
        // Without excluding the back-edge, 'f' (set at the end of a run) would reach 'a' on "replay".
        val v = analyse(
            node("start", go("a")),
            node("a", go("b", "gated", ChoiceRequirement(requiredFlags = setOf("f"))), go("b", "plain")),
            node("b", go("start", "replay", c = Consequences(setFlags = setOf("f")))),
        )
        assertEquals(Status.DEAD, statusOf(v, "a", "gated"))
    }

    @Test
    fun `a gate on an unmodelled stat is inconclusive rather than passed`() {
        val v = analyse(
            node("start", go("end", "gated", ChoiceRequirement(minStats = mapOf(StatType.XP to 1000)))),
            node("end")
        )
        assertEquals(Status.INCONCLUSIVE, statusOf(v, "start", "gated"))
    }

    @Test
    fun `exceeding the state budget makes unresolved choices inconclusive, never alive or dead`() {
        val v = analyse(
            node("start", go("a", "x", c = Consequences(setFlags = setOf("f1"))), go("a", "y")),
            node("a", go("end", "gated", ChoiceRequirement(requiredFlags = setOf("never")))),
            node("end"),
            max = 1
        )
        assertEquals(Status.INCONCLUSIVE, statusOf(v, "a", "gated"))
    }

    // ---------- the real story ----------

    private val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }

    private val realAnalyzer = ChoiceReachabilityAnalyzer(
            nodes = nodes,
            combatTargets = { n ->
                n.combatEncounterId?.let { CombatRepository.encounter(it) }
                    ?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId ?: it.victoryNodeId) }
                    ?: emptyList()
            }
        )

    private val verdicts: List<ChoiceReachabilityAnalyzer.Verdict> by lazy { realAnalyzer.analyse() }

    /**
     * Choices known to be dead, as (nodeId, label). Empty since C1-1 removed the two unreachable
     * buckler offers. This is still a ratchet: it fails if any *new* dead choice appears, and an entry
     * added here must be justified in the same commit.
     */
    private val knownDead: Set<Pair<String, String>> = setOf(
        // Replay-only by design: offered once the durable Scraped Wax Disc has already been received on an
        // earlier telling, so it can never be satisfied on a first playthrough. See the C-class replay rules.
        "ch1_rider_down" to "Press him again for what he knows."
    )

    @Test
    fun `the analysis of the real story completes without hitting its state budget`() {
        val inconclusive = verdicts.filter { it.status == Status.INCONCLUSIVE }
        assertTrue(
            "Inconclusive choices (analysis could not establish a verdict): " +
                inconclusive.map { it.nodeId to it.choice.label },
            inconclusive.isEmpty()
        )
    }

    @Test
    fun `the real analysis keeps comfortable headroom under its state budget`() {
        verdicts // force the run
        assertTrue(
            "Explored ${realAnalyzer.statesExplored} states; the budget is 1,500,000; keep this check well under it. If this is " +
                "approaching it, tighten the state abstraction before the story grows further.",
            realAnalyzer.statesExplored < 1_000_000
        )
    }

    @Test
    fun `every gated choice in the real story is reachable except the known dead ones`() {
        val dead = verdicts.filter { it.status == Status.DEAD }.map { it.nodeId to it.choice.label }.toSet()
        val unexpected = dead - knownDead
        assertTrue("New dead choices (no reachable state can ever satisfy them): $unexpected", unexpected.isEmpty())
    }

    @Test
    fun `every known dead choice is still dead and still exists`() {
        val dead = verdicts.filter { it.status == Status.DEAD }.map { it.nodeId to it.choice.label }.toSet()
        val staleOrFixed = knownDead - dead
        assertTrue(
            "These are no longer dead (or no longer exist) - remove them from knownDead: $staleOrFixed",
            staleOrFixed.isEmpty()
        )
    }

    @Test
    fun `the real story only gates on Courage, Honour and Gold`() {
        val analyzer = ChoiceReachabilityAnalyzer(nodes)
        assertEquals(emptySet<StatType>(), analyzer.reportsUnsupportedGates())
    }
}
