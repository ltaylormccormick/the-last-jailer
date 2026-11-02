package com.thelastjailer.app.data

import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import java.util.Random

/**
 * A deterministic witness for a gated choice that random walks reach only by luck, in two stages that are both made of the real
 * transitions ([applyChoice], [resolveCombat]):
 *
 *  1. Real walks from the start of the story, each with its own random sequence (the seed is the walk's number), lean towards the flags
 *     [lean] and stop at the first one that stands on [pivot] holding all of them. Because every walk has its own sequence and the walks
 *     are tried in order, the result depends only on the scenes and choices a walk crosses, so scenes added later in the story cannot
 *     change which walk is found. That is the instability of one shared sequence, which a global sampler has.
 *  2. From that real state, every real transition inside [inScope] is taken, breadth first, so no combination of the local choices
 *     (here, the order of the places of the night before, and what is said in each) is left to chance. A gated choice any reached
 *     state satisfies has a witness.
 *
 * A witness is a proof of life, as for [GatedChoiceSampler]. Finding none proves nothing, and the exact search is still asked.
 */
internal class PivotWitness(
    nodes: List<StoryNode>,
    private val combat: (StoryNode) -> CombatEncounter?,
    private val pivot: String,
    private val lean: Set<String>,
    private val inScope: (String) -> Boolean,
    private val startNodeId: String = "fallen_knight"
) {
    private val byId = nodes.associateBy { it.id }

    /** One step of a walk: the scene, the choice taken (its index in the scene, or -1 for a fight), and whether the fight was won. */
    class Step(val sceneId: String, val choiceIndex: Int, val label: String, val won: Boolean? = null)

    class Found(val seed: Int, val state: GameState, val trail: List<Step>)

    fun findPivotState(maxSeeds: Int = 20_000, maxSteps: Int = 3_000): Found? {
        for (seed in 0 until maxSeeds) {
            val random = Random(seed.toLong())
            var s = GameState(sceneId = startNodeId)
            val trail = ArrayList<Step>()
            var steps = 0
            while (steps++ < maxSteps) {
                if (s.sceneId == pivot) {
                    if (s.flags.containsAll(lean)) return Found(seed, s, trail)
                    break
                }
                val node = byId[s.sceneId] ?: break
                if (node.combatEncounterId != null) {
                    val encounter = combat(node) ?: break
                    val win = random.nextBoolean()
                    trail += Step(node.id, -1, "(fight)", win)
                    s = s.resolveCombat(encounter, CombatOutcome(victory = win, damageTaken = 0))
                    continue
                }
                val takeable = node.choices.withIndex().filter { (_, c) -> (c.requirements?.isSatisfiedBy(s) ?: true) && c.nextNodeId != startNodeId }
                if (takeable.isEmpty()) break
                val wanted = takeable.filter { (_, c) -> c.consequences.setFlags.any { it in lean } }
                val pool = if (wanted.isNotEmpty() && random.nextInt(10) < 8) wanted else takeable
                val (index, choice) = pool[random.nextInt(pool.size)]
                trail += Step(node.id, index, choice.label)
                s = s.applyChoice(choice)
            }
        }
        return null
    }

    /** Every gated choice, as (scene, index), that some state reached from [start] inside the scope satisfies, with the path to the first such state. */
    class Reached(val satisfied: Set<Pair<String, Int>>, val paths: Map<Pair<String, Int>, List<Step>>, val statesVisited: Int)

    fun completeFrom(start: GameState, maxStates: Int = 200_000): Reached {
        val satisfied = LinkedHashSet<Pair<String, Int>>()
        val paths = HashMap<Pair<String, Int>, List<Step>>()
        val seen = HashSet<Triple<String, Set<String>, Pair<Int, Int>>>()
        val queue = ArrayDeque<Pair<GameState, List<Step>>>()
        fun key(s: GameState) = Triple(s.sceneId, s.flags, s.courage to s.honour)
        seen += key(start)
        queue.addLast(start to emptyList())
        while (queue.isNotEmpty()) {
            check(seen.size <= maxStates) { "the local search outgrew $maxStates states" }
            val (s, path) = queue.removeFirst()
            val node = byId[s.sceneId] ?: continue
            if (!inScope(node.id) || node.combatEncounterId != null) continue
            node.choices.forEachIndexed { index, choice ->
                val req = choice.requirements
                val ok = req == null || req.isSatisfiedBy(s)
                if (req != null && ok && (node.id to index) !in satisfied) {
                    satisfied += node.id to index
                    paths[node.id to index] = path
                }
                if (ok && choice.nextNodeId != startNodeId) {
                    val next = s.applyChoice(choice)
                    if (seen.add(key(next))) queue.addLast(next to (path + Step(node.id, index, choice.label)))
                }
            }
        }
        return Reached(satisfied, paths, seen.size)
    }
}

/**
 * The gated choices the real-story reachability analysis asks a deterministic witness for before it asks the exact search. Each is
 * reached only through a rare combination of earlier decisions, so a global random sample finds it by luck: it did in the Chapters 10
 * to 12 build and, with the same seed, did not once Chapters 13 to 16 had changed the sequence.
 */
internal object PivotWitnesses {
    /** The night before the column in Chapter 10, entered holding the Chapter 9 decision to stand with Halvard on the stair. */
    fun forgeSide(nodes: List<StoryNode>, combat: (StoryNode) -> CombatEncounter?) = PivotWitness(
        nodes, combat, pivot = "the_last_two", lean = setOf("c9_stair_rules"),
        inScope = { id -> id == "the_last_two" || id.startsWith("c10_") }
    )

    fun satisfied(nodes: List<StoryNode>, combat: (StoryNode) -> CombatEncounter?): Set<Pair<String, Int>> {
        val witness = forgeSide(nodes, combat)
        val found = witness.findPivotState() ?: return emptySet()
        return witness.completeFrom(found.state).satisfied
    }
}
