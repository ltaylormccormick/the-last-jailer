package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice

/**
 * Test helper: walks every selectable path through the story from a starting state until it reaches
 * one of [stopAt]. It only follows ordinary choices (visible ones, exactly as the player would see
 * them) and never a combat node, so it is safe to use for the part of Chapter 1 before the first fight.
 *
 * Used by the Chapter 1 baseline tests to pin down how the opening behaves *today*, so that later
 * batches that rebuild the opening can prove what changed and what did not.
 */
internal object Chapter1Paths {

    data class Path(val labels: List<String>, val nodeIds: List<String>, val end: GameState)

    fun enumerate(start: GameState, stopAt: Set<String>, limit: Int = 10_000): List<Path> {
        val out = mutableListOf<Path>()
        fun walk(state: GameState, labels: List<String>, nodes: List<String>) {
            check(out.size < limit) { "path explosion past $limit" }
            if (state.sceneId in stopAt) {
                out += Path(labels, nodes + state.sceneId, state)
                return
            }
            val node = StoryRepository.node(state.sceneId)
            check(node.combatEncounterId == null) { "walker reached combat node ${node.id} before a stop node" }
            val choices = StoryRepository.visibleChoices(node, state)
            check(choices.isNotEmpty()) { "dead end at ${node.id} with flags ${state.flags}" }
            check(nodes.size < 60) { "cycle suspected at ${node.id}" }
            choices.forEach { c: Choice ->
                walk(state.applyChoice(c), labels + c.label, nodes + node.id)
            }
        }
        walk(start, emptyList(), emptyList())
        return out
    }

    /** First choice whose label starts with [prefix], taken from what the player can currently see. */
    fun pick(state: GameState, prefix: String): GameState {
        val node = StoryRepository.node(state.sceneId)
        val choice = StoryRepository.visibleChoices(node, state).firstOrNull { it.label.startsWith(prefix, ignoreCase = true) }
            ?: error("no visible choice starting with '$prefix' at ${node.id}")
        return state.applyChoice(choice)
    }

    /** Follows a sequence of label prefixes from [start]. */
    fun follow(start: GameState, vararg prefixes: String): GameState = prefixes.fold(start) { s, p -> pick(s, p) }

    // Short codes for the labels that distinguish one opening path from another. Labels that are the
    // only choice at their node ("Descend into the dark.", "Follow the tunnel...") are omitted.
    private val codes = listOf(
        "OPEN THE DOOR" to "door", "DRAW YOUR SWORD" to "sword", "WALK AWAY" to "walk",
        "Search the gully" to "search", "Leave it" to "leave",
        "Keep the token" to "keep", "Leave the past" to "bury",
        "Call out" to "call", "Wait, and listen" to "wait", "Move without a sound" to "creep", "Find the old shaft" to "shaft",
        "Help him" to "help", "Tell him honestly" to "confess", "Move on without" to "moveon",
        "Take the buckler" to "buckler", "Heed his warning" to "heed"
    )

    fun routeKey(labels: List<String>): String =
        labels.mapNotNull { l -> codes.firstOrNull { l.startsWith(it.first) }?.second }.joinToString(".")

    /** Everything the baseline pins about the state a path ends in. Flags and items are sorted. */
    fun summary(s: GameState): String =
        "C${s.courage} H${s.honour} HP${s.health} G${s.gold} XP${s.xp} L${s.level}" +
            " | inv=${s.inventory.sorted().joinToString(",")}" +
            " | tro=${s.trophies.sorted().joinToString(",")}" +
            " | flags=${s.flags.sorted().joinToString(",")}"
}
