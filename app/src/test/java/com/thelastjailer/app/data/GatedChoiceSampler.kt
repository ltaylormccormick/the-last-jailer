package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import java.util.Random

/**
 * Walks the story at random from its start, the way a first playthrough goes, and records every gated choice that some walk
 * stood in a state that satisfied.
 *
 * A walk is a real sequence of the real transitions ([applyChoice] for a choice, [resolveCombat] for a fight, won or lost at
 * random), so a gated choice the sampler witnesses is alive, with no assumption about stats or flags to defend. It is a
 * lower bound and nothing more: a choice it has not witnessed is unproven, not dead. [ChoiceReachabilityAnalyzer] settles
 * those.
 *
 * It exists because the exact search over every reachable state stopped being affordable once Chapter IX began to read the
 * decisions of Chapters VII and VIII: a single hub there stands over a product of about twenty independent earlier choices,
 * and the exact search has to hold all of them at once, while a walk holds one.
 *
 * The walks lean towards the choices that set a flag some still-unwitnessed gate requires (four times in ten), so a gate
 * that needs several specific earlier decisions is found sooner than uniform chance would find it. Leaning never takes a
 * choice that is not takeable.
 */
class GatedChoiceSampler(
    nodes: List<StoryNode>,
    private val combat: (StoryNode) -> CombatEncounter? = { null },
    private val startNodeId: String = "fallen_knight",
    private val startState: GameState = GameState(sceneId = startNodeId)
) {
    private val byId = nodes.associateBy { it.id }
    private val gated: List<Pair<String, Int>> = nodes.flatMap { n ->
        n.choices.mapIndexedNotNull { i, c -> if (c.requirements != null) n.id to i else null }
    }

    /** Steps taken by the last [satisfied] call, for the record. */
    var stepsTaken: Long = 0
        private set

    fun satisfied(walks: Int, seed: Long, maxSteps: Int = 3_000): Set<Pair<String, Int>> {
        val random = Random(seed)
        val found = HashSet<Pair<String, Int>>()
        var wanted = wantedFlags(found)
        stepsTaken = 0
        for (w in 0 until walks) {
            if (w > 0 && w % 500 == 0) wanted = wantedFlags(found)
            walk(random, found, wanted, maxSteps)
        }
        return found
    }

    private fun wantedFlags(found: Set<Pair<String, Int>>): Set<String> {
        val out = HashSet<String>()
        for (key in gated) if (key !in found) byId.getValue(key.first).choices[key.second].requirements?.let { out += it.requiredFlags }
        return out
    }

    private fun walk(random: Random, found: MutableSet<Pair<String, Int>>, wanted: Set<String>, maxSteps: Int) {
        var s = startState
        var steps = 0
        while (steps++ < maxSteps) {
            stepsTaken++
            val node = byId[s.sceneId] ?: return
            if (node.combatEncounterId != null) {
                val encounter = combat(node) ?: return
                s = s.resolveCombat(encounter, CombatOutcome(victory = random.nextBoolean(), damageTaken = 0))
                continue
            }
            val takeable = ArrayList<Choice>(4)
            node.choices.forEachIndexed { index, choice ->
                val req = choice.requirements
                val ok = req == null || req.isSatisfiedBy(s)
                if (req != null && ok) found += node.id to index
                if (ok && choice.nextNodeId != startNodeId) takeable += choice
            }
            if (takeable.isEmpty()) return
            val pool = if (wanted.isNotEmpty() && random.nextInt(10) < 4) {
                takeable.filter { c -> c.consequences.setFlags.any { it in wanted } }.ifEmpty { takeable }
            } else {
                takeable
            }
            s = s.applyChoice(pool[random.nextInt(pool.size)])
        }
    }
}
