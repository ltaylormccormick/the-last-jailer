package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat

/**
 * Shared helpers for the Chapter 6 tests. Everything runs on the real story graph and the real
 * [applyChoice] / [resolveCombat], so the tests exercise the transitions the game does.
 */
internal object Chapter6Kit {
    const val TOLD = "told_halvard_the_truth"
    const val KEPT = "kept_the_offer_from_halvard"
    const val ROADS = "the_choice_of_roads"
    const val ARRIVE = "c6_arrive_greymoor"
    const val SIGHTED = "c6_unbound_sighted"
    const val NEXT_CHAPTER = "voss_returns_alone"

    val ROAD_STOPS = setOf("c6_tollhouse", "c6_cairn", "c6_camp")
    val YARD_SPOTS = setOf("c6_watchpost", "c6_cellar", "c6_garden")

    /** Max health 500 so that every cost and gain is visible without hitting a clamp. */
    fun at(
        nodeId: String,
        flags: Set<String> = emptySet(),
        courage: Int = 99,
        honour: Int = 99,
        health: Int = 500,
        gold: Int = 100,
        inventory: List<String> = emptyList()
    ): GameState = ReplayKit.jumpTo(
        GameState(
            activeSlot = 1, courage = courage, honour = honour, health = health, maxHealth = 500,
            gold = gold, flags = flags, inventory = inventory
        ),
        nodeId
    )

    fun node(id: String): StoryNode = StoryRepository.node(id)

    fun visible(s: GameState): List<Choice> = StoryRepository.visibleChoices(node(s.sceneId), s)

    /** Takes the one visible choice to [target]; [labelPart] picks between several labels that share a target. */
    fun take(s: GameState, target: String, labelPart: String? = null): GameState {
        val matches = visible(s).filter { it.nextNodeId == target && (labelPart == null || it.label.contains(labelPart)) }
        check(matches.size == 1) {
            "expected exactly one visible route to $target from ${s.sceneId} (label '$labelPart'), found ${visible(s).map { it.label to it.nextNodeId }}"
        }
        return s.applyChoice(matches.single())
    }

    /** Takes the one visible choice whose label is exactly [label]. */
    fun takeLabel(s: GameState, label: String): GameState {
        val matches = visible(s).filter { it.label == label }
        check(matches.size == 1) { "expected exactly one visible '$label' at ${s.sceneId}, found ${visible(s).map { it.label }}" }
        return s.applyChoice(matches.single())
    }

    fun path(s: GameState, vararg targets: String): GameState = targets.fold(s) { acc, t -> take(acc, t) }

    /** Every way through the graph from [start] to a state where [stop] holds, as (scenes stood in, final state). */
    fun explore(start: GameState, stop: (GameState) -> Boolean, maxDepth: Int = 16): List<Pair<List<String>, GameState>> {
        val out = ArrayList<Pair<List<String>, GameState>>()
        fun go(s: GameState, depth: Int, trail: List<String>) {
            if (stop(s)) {
                out += (trail to s)
                return
            }
            check(depth < maxDepth) { "too deep at ${s.sceneId} after $trail" }
            val choices = visible(s)
            check(choices.isNotEmpty()) { "dead end at ${s.sceneId} with flags ${s.flags}" }
            choices.forEach { go(s.applyChoice(it), depth + 1, trail + s.sceneId) }
        }
        go(start, 0, emptyList())
        return out
    }

    /**
     * A [ReplayKit.play] picker from ordered rules. Each rule is (scene id, key). A key starting with "->" picks the
     * visible choice that leads to that scene; any other key picks the first visible choice whose label contains it.
     * A scene that is stood in several times consumes its rules in order; with no rule left it falls to the default.
     */
    fun picker(vararg rules: Pair<String, String>): (StoryNode, List<Choice>) -> Choice? {
        val queues = HashMap<String, ArrayDeque<String>>()
        rules.forEach { (scene, key) -> queues.getOrPut(scene) { ArrayDeque() }.addLast(key) }
        return { node, visible ->
            val key = queues[node.id]?.removeFirstOrNull()
            when {
                key == null -> null
                key.startsWith("->") -> visible.firstOrNull { it.nextNodeId == key.drop(2) }
                else -> visible.firstOrNull { it.label.contains(key) }
            }
        }
    }

    /** Plays from [start] to the first scene of Chapter 7, winning fights, using [pick] where it has an opinion. */
    fun playToChapter7(start: GameState, pick: (StoryNode, List<Choice>) -> Choice? = { _, _ -> null }): GameState =
        ReplayKit.play(start, stop = { it.sceneId == NEXT_CHAPTER }, pick = pick)

    /** Health after a story step that applies [delta] to a state at least half healthy and below the cap. */
    fun healthAfter(start: Int, delta: Int, max: Int = 500): Int = minOf(max, (start + delta).coerceIn(0, max) + 1)

    fun winOrLose(s: GameState, win: Boolean): GameState {
        val encounter = CombatRepository.encounter(node(s.sceneId).combatEncounterId!!)
        return s.resolveCombat(encounter, CombatOutcome(victory = win, damageTaken = 0))
    }
}
