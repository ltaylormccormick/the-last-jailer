package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.LockedDisplay
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * One row in a scene's choice list. [isAvailable] false means the choice is shown but cannot be
 * selected; [lockedReasons] then explains why, one short line each (never empty for a locked row).
 */
data class ChoiceAvailability(
    val choice: Choice,
    val isAvailable: Boolean,
    val lockedReasons: List<String> = emptyList()
)

/** Player-facing name for a stat in a requirement line. */
fun StatType.displayName(): String = when (this) {
    StatType.COURAGE -> "Courage"
    StatType.HONOUR -> "Honour"
    StatType.HEALTH -> "Health"
    StatType.GOLD -> "Gold"
    StatType.XP -> "XP"
}

/**
 * Why [choice] is locked for [state], or empty if it isn't. Only the requirements that are actually
 * unmet are explained: stat gates automatically ("Requires Courage 3 (you have 1)"), flag gates only
 * through the author's [Choice.lockedHint], so a flag name is never exposed.
 */
fun lockedReasonsFor(choice: Choice, state: GameState): List<String> {
    val req = choice.requirements ?: return emptyList()
    if (req.isSatisfiedBy(state)) return emptyList()

    val reasons = ArrayList<String>()
    val unmetStats = req.minStats.entries
        .filter { (stat, min) -> state.statValue(stat) < min }
        .sortedBy { it.key.ordinal }
    if (unmetStats.isNotEmpty()) {
        val parts = unmetStats.map { (stat, min) -> "${stat.displayName()} $min (you have ${state.statValue(stat)})" }
        reasons += "Requires " + parts.joinToString(" and ")
    }
    val flagGateUnmet = !state.flags.containsAll(req.requiredFlags) || state.flags.any { it in req.forbiddenFlags }
    if (flagGateUnmet) {
        val hint = choice.lockedHint?.takeIf { it.isNotBlank() }
        if (hint != null) reasons += hint
    }
    return reasons
}

/**
 * Every choice of [node] the player should see, in authoring order: available ones, plus locked
 * ones that opted in with [LockedDisplay.SHOW_LOCKED]. A choice that is unmet and not opted in is
 * left out entirely - the same as [StoryRepository.visibleChoices], which is unchanged.
 *
 * A locked row is guaranteed at least one reason line: if an opted-in choice is gated by a flag
 * but has no authored hint, it falls back to being hidden rather than showing an unexplained lock
 * (the content test also fails the build for that case).
 */
fun StoryRepository.choicesWithAvailability(node: StoryNode, state: GameState): List<ChoiceAvailability> =
    node.choices.mapNotNull { choice ->
        val req = choice.requirements
        if (req == null || req.isSatisfiedBy(state)) {
            ChoiceAvailability(choice, isAvailable = true)
        } else if (choice.lockedDisplay == LockedDisplay.SHOW_LOCKED) {
            val reasons = lockedReasonsFor(choice, state)
            if (reasons.isEmpty()) null else ChoiceAvailability(choice, isAvailable = false, lockedReasons = reasons)
        } else {
            null
        }
    }
