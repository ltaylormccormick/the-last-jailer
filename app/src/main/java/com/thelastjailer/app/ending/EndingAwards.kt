package com.thelastjailer.app.ending

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState

/**
 * The award step for the nine [EndingTrophies]. [com.thelastjailer.app.applyChoice] calls it once per story transition and
 * adds whatever it returns to the trophy set; nothing is ever removed, so Back, Go Back and a repeat are all harmless.
 *
 * Trophies are awarded on exactly two kinds of transition: **arriving** at one of the six endings (the six "Rest, for
 * now." choices) and **leaving** one (its single "Begin the tale again" choice). Leaving matters because a save parked
 * on an ending by an older build has never had an arrival under this rule; replaying earns it its awards. Loading,
 * Back, SAVE into another slot, a fight, a shop and every other choice award nothing.
 *
 * What is awarded:
 *  - [EndingTrophies.TALE_TOLD], always, decided first by a plain membership test that nothing can block;
 *  - the aid and fate outcome, read from the **ending scene id** and not from flags. Those two answers are the only ones
 *    the story turns into six different endings, so the id cannot be stale;
 *  - the legacy, when [LegacyRule] can prove one from the flags. It is computed inside [runCatching] so that a fault there
 *    leaves the legacy unawarded instead of costing the player the other awards.
 */
internal object EndingAwards {
    private class Outcome(val aid: String, val fate: String)

    private val endings: Map<String, Outcome> = mapOf(
        "the_watch_continues_dark_aid_captured" to Outcome(EndingTrophies.BORROWED_STRENGTH, EndingTrophies.HER_CHAINS),
        "the_watch_continues_dark_aid_mercy" to Outcome(EndingTrophies.BORROWED_STRENGTH, EndingTrophies.HER_FREEDOM),
        "the_watch_continues_dark_aid_deferred" to Outcome(EndingTrophies.BORROWED_STRENGTH, EndingTrophies.HER_FATE_UNWRITTEN),
        "the_watch_continues_unaided_captured" to Outcome(EndingTrophies.UNAIDED, EndingTrophies.HER_CHAINS),
        "the_watch_continues_unaided_mercy" to Outcome(EndingTrophies.UNAIDED, EndingTrophies.HER_FREEDOM),
        "the_watch_continues_unaided_deferred" to Outcome(EndingTrophies.UNAIDED, EndingTrophies.HER_FATE_UNWRITTEN)
    )

    /** The six ending scene ids; a test pins them to [com.thelastjailer.app.data.StoryRepository.isEnding]. */
    val endingIds: Set<String> get() = endings.keys

    /** The trophies to add for the transition [from] -> [to] made by [choice] (empty for every ordinary transition). */
    fun forTransition(from: GameState, choice: Choice, to: GameState): Set<String> {
        val endingId = when {
            choice.nextNodeId in endings -> choice.nextNodeId
            from.sceneId in endings -> from.sceneId
            else -> return emptySet()
        }
        val outcome = endings.getValue(endingId)
        val awards = linkedSetOf(EndingTrophies.TALE_TOLD, outcome.aid, outcome.fate)
        runCatching { LegacyRule.evaluate(to.flags).axis }.getOrNull()?.let { awards += EndingTrophies.forAxis(it) }
        return awards
    }
}
