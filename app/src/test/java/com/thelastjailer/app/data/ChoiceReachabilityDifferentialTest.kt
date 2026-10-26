package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.ChoiceReachabilityAnalyzer.Status
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ChoiceReachabilityAnalyzer] prunes a state when another at the same node, with the same relevant flags, has at least as
 * much Courage, Honour and Gold, and it works through the story in graph order. Both are sound only if every transition is
 * monotone in those three stats: a choice adds fixed amounts (Gold is clamped at zero), flags never depend on stats, and a
 * gate is a minimum. This test holds that assumption against an exhaustive search that prunes nothing: on over a thousand random
 * small stories (random flags, clears, stat gains and losses, gates on flags and on all three stats) the two must agree on
 * which gated choices can ever be taken.
 */
class ChoiceReachabilityDifferentialTest {

    private val flagNames = listOf("f1", "f2", "f3", "f4")

    private fun randomStory(seed: Long): List<StoryNode> {
        val r = Random(seed)
        val n = 5 + r.nextInt(5)

        fun requirement(): ChoiceRequirement? {
            if (r.nextInt(2) == 0) return null
            val required = flagNames.filter { r.nextInt(8) == 0 }.toSet()
            val forbidden = flagNames.filter { it !in required && r.nextInt(10) == 0 }.toSet()
            val stats = HashMap<StatType, Int>()
            if (r.nextInt(10) < 3) stats[StatType.COURAGE] = 1 + r.nextInt(6)
            if (r.nextInt(10) < 2) stats[StatType.HONOUR] = r.nextInt(5)
            if (r.nextInt(10) < 4) stats[StatType.GOLD] = r.nextInt(70)
            return ChoiceRequirement(required, forbidden, stats)
        }

        fun consequences(): Consequences {
            val deltas = HashMap<StatType, Int>()
            if (r.nextInt(4) == 0) deltas[StatType.COURAGE] = 1 + r.nextInt(2)
            if (r.nextInt(4) == 0) deltas[StatType.HONOUR] = 1 + r.nextInt(2)
            if (r.nextInt(10) < 6) deltas[StatType.GOLD] = r.nextInt(51) - 20
            val set = flagNames.filter { r.nextInt(7) == 0 }.toSet()
            val clear = flagNames.filter { r.nextInt(12) == 0 }.toSet()
            return Consequences(statDeltas = deltas, setFlags = set, clearFlags = clear)
        }

        // Few nodes and two or three choices each, always forward, so that routes of different strength keep meeting at the same node.
        return (0 until n).map { i ->
            val choices = if (i == n - 1) {
                emptyList()
            } else {
                (0 until 2 + r.nextInt(2)).map { k ->
                    val to = i + 1 + r.nextInt(minOf(3, n - i - 1))
                    Choice("c$i-$k", "n$to", requirement(), consequences())
                }
            }
            StoryNode("n$i", "chapter_1", "n$i", "ill", "text", choices, null)
        }
    }

    /** Every (node, choice index) of a gated choice that some exactly-tracked reachable state satisfies. Nothing is pruned. */
    private fun exhaustiveAlive(nodes: List<StoryNode>): Set<Pair<String, Int>> {
        val byId = nodes.associateBy { it.id }
        val seen = HashSet<Triple<String, Set<String>, Triple<Int, Int, Int>>>()
        val alive = HashSet<Pair<String, Int>>()
        val stack = ArrayDeque<GameState>()
        stack.addLast(GameState(sceneId = "n0"))
        while (stack.isNotEmpty()) {
            val s = stack.removeLast()
            if (!seen.add(Triple(s.sceneId, s.flags, Triple(s.courage, s.honour, s.gold)))) continue
            val node = byId[s.sceneId] ?: continue
            node.choices.forEachIndexed { index, choice ->
                val req = choice.requirements
                if (req == null || req.isSatisfiedBy(s)) {
                    if (req != null) alive += node.id to index
                    stack.addLast(s.applyChoice(choice))
                }
            }
        }
        return alive
    }

    @Test
    fun `the analyzer agrees with an unpruned exhaustive search on random stories`() {
        var gated = 0
        var aliveCount = 0
        var deadCount = 0
        repeat(1500) { seed ->
            val nodes = randomStory(seed.toLong())
            val verdicts = ChoiceReachabilityAnalyzer(
                nodes = nodes, startNodeId = "n0", startState = GameState(sceneId = "n0"), maxStates = 5_000_000
            ).analyse()
            val expected = exhaustiveAlive(nodes)
            verdicts.forEach { v ->
                val index = nodes.first { it.id == v.nodeId }.choices.indexOf(v.choice)
                val shouldBeAlive = (v.nodeId to index) in expected
                assertTrue("seed $seed ${v.nodeId} #$index is never inconclusive here, was ${v.status}", v.status != Status.INCONCLUSIVE)
                assertEquals("seed $seed ${v.nodeId} #$index (${v.choice.requirements})", shouldBeAlive, v.status == Status.ALIVE)
                gated++
                if (shouldBeAlive) aliveCount++ else deadCount++
            }
        }
        assertTrue("enough gated choices to mean something: $gated", gated > 3000)
        assertTrue("both outcomes occur often: alive $aliveCount, dead $deadCount", aliveCount > 300 && deadCount > 300)
    }
}
