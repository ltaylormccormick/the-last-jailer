package com.thelastjailer.app.ending

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.ReplayKit

/**
 * Shared helpers for the ending-trophy tests. The nine ids and the awards of each ending are spelled out here on purpose,
 * independently of [EndingTrophies] and [EndingAwards], so a mistake in the production constants or table cannot be
 * mirrored by the test that checks it.
 */
internal object EndingKit {
    val NINE: Set<String> = setOf(
        "The Tale Told", "The Unyielding", "The Honourbound", "The Steadfast", "Borrowed Strength", "Unaided",
        "Her Chains", "Her Freedom", "Her Fate Unwritten"
    )

    val LEGACIES: Set<String> = setOf("The Unyielding", "The Honourbound", "The Steadfast")

    /** Completion plus the aid and fate outcome of [endingId], never the legacy. */
    fun awardsOf(endingId: String): Set<String> = setOf(
        "The Tale Told",
        if ("dark_aid" in endingId) "Borrowed Strength" else "Unaided",
        when {
            endingId.endsWith("_captured") -> "Her Chains"
            endingId.endsWith("_mercy") -> "Her Freedom"
            endingId.endsWith("_deferred") -> "Her Fate Unwritten"
            else -> error("not an ending: $endingId")
        }
    )

    fun mine(trophies: Set<String>): Set<String> = trophies.intersect(NINE)

    /** One flag set per option index, in story order: [picks] has one entry per decision of [LegacyRule]. */
    fun route(vararg picks: Int): Set<String> {
        require(picks.size == LegacyRule.decisions.size)
        return LegacyRule.decisions.mapIndexed { i, d -> d.options[picks[i]].flag }.toSet()
    }

    /** The first option of every decision: the Courage answer throughout. */
    val routeA: Set<String> = route(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    /** The second option of every decision: the Honour answer throughout. */
    val routeB: Set<String> = route(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)

    /** Aid taken, the four postponing answers, the eight stay/wait answers, "another way" at the start. */
    val routeC: Set<String> = route(0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2)

    /** Plays the real story from [from] to chapter30_end, taking the answers in [wanted] wherever they are offered. */
    fun playTo(from: GameState, wanted: Set<String>): GameState = ReplayKit.play(
        from,
        stop = { it.sceneId == "chapter30_end" },
        pick = { _, visible -> visible.firstOrNull { c -> c.consequences.setFlags.any { it in wanted } } }
    )

    /** The single "Rest, for now." button [end] is offered. */
    fun arrival(end: GameState): Choice = ReplayKit.arrivals(end).single()
}
