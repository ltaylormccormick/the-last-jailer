package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat

/**
 * Shared helpers for the replay-integrity tests: the Chapter 4 aid decision and the Chapter 18 Ilsevet fate
 * decision. Everything here runs on the real story graph and the real [applyChoice], so the tests exercise
 * the same transitions the game does.
 */
internal object ReplayKit {
    const val AID_NODE = "what_the_prisoner_offers_again"
    val FATE_NODES = listOf("what_kaelen_chooses_trusted", "what_kaelen_chooses_guarded")
    val FATES = listOf("captured_ilsevet", "showed_ilsevet_mercy", "deferred_ilsevets_fate")

    /** One of the six endings: the answers that lead to [nodeId]. */
    class Ending(val aided: Boolean, val fate: String, val nodeId: String)

    val ENDINGS = listOf(
        Ending(true, "captured_ilsevet", "the_watch_continues_dark_aid_captured"),
        Ending(true, "showed_ilsevet_mercy", "the_watch_continues_dark_aid_mercy"),
        Ending(true, "deferred_ilsevets_fate", "the_watch_continues_dark_aid_deferred"),
        Ending(false, "captured_ilsevet", "the_watch_continues_unaided_captured"),
        Ending(false, "showed_ilsevet_mercy", "the_watch_continues_unaided_mercy"),
        Ending(false, "deferred_ilsevets_fate", "the_watch_continues_unaided_deferred")
    )

    /** Strong enough for every stat-gated answer (Courage 16, Honour 15), with room to spare. */
    val STRONG = GameState(activeSlot = 1, courage = 99, honour = 99, health = 500, maxHealth = 500)

    /** [state] standing on [nodeId], with the chapter that scene belongs to. Flags and everything else are kept. */
    fun jumpTo(state: GameState, nodeId: String): GameState =
        state.copy(chapterId = StoryRepository.node(nodeId).chapterId, sceneId = nodeId)

    fun aidChoice(aided: Boolean): Choice = StoryRepository.node(AID_NODE).choices.single {
        it.nextNodeId == if (aided) "the_cost_aided" else "the_cost_unaided"
    }

    fun fateChoice(nodeId: String, flag: String): Choice =
        StoryRepository.node(nodeId).choices.single { flag in it.consequences.setFlags }

    /** The eight choices the replay correction touches, each with the id of the node it belongs to. */
    fun decisionChoices(): List<Pair<String, Choice>> =
        StoryRepository.node(AID_NODE).choices.map { AID_NODE to it } +
            FATE_NODES.flatMap { nodeId -> StoryRepository.node(nodeId).choices.map { nodeId to it } }

    /** The "Rest, for now." arrival buttons [state] is offered on chapter30_end. */
    fun arrivals(state: GameState): List<Choice> =
        StoryRepository.visibleChoices(StoryRepository.node("chapter30_end"), state)
            .filter { it.label == "Rest, for now." }

    /** The "Begin the tale again" choice on an ending node. */
    fun loopChoice(endingNodeId: String): Choice =
        StoryRepository.node(endingNodeId).choices.single { it.nextNodeId == "fallen_knight" }

    /**
     * One telling taken by jumping between the real decision scenes (aid, then fate) and then to
     * chapter30_end. The answers are applied with the real choices, so any reset on those choices runs.
     * Everything else carried in [from] (flags included) is kept, exactly as a replay keeps it.
     */
    fun tellingByDecisions(from: GameState, ending: Ending, fateNode: String = FATE_NODES[0]): GameState {
        var s = jumpTo(from, AID_NODE).applyChoice(aidChoice(ending.aided))
        s = jumpTo(s, fateNode).applyChoice(fateChoice(fateNode, ending.fate))
        return jumpTo(s, "chapter30_end")
    }

    /** From chapter30_end: takes the single arrival, then "Begin the tale again". Returns the state on fallen_knight. */
    fun finishAndReplay(atEnd: GameState): GameState {
        val arrival = arrivals(atEnd).single()
        val onEnding = atEnd.applyChoice(arrival)
        return onEnding.applyChoice(loopChoice(onEnding.sceneId))
    }

    /**
     * Walks the real story from [start] until [stop] holds. [pick] may choose; otherwise the first choice
     * whose target has not been visited in this walk is taken. Fights are won without damage.
     */
    fun play(
        start: GameState,
        stop: (GameState) -> Boolean,
        pick: (StoryNode, List<Choice>) -> Choice? = { _, _ -> null },
        maxSteps: Int = 5_000
    ): GameState {
        var s = start
        val visits = HashMap<String, Int>()
        var steps = 0
        while (steps++ < maxSteps) {
            if (stop(s)) return s
            val node = StoryRepository.node(s.sceneId)
            visits[node.id] = (visits[node.id] ?: 0) + 1
            val encounterId = node.combatEncounterId
            if (encounterId != null) {
                s = s.resolveCombat(CombatRepository.encounter(encounterId), CombatOutcome(victory = true, damageTaken = 0))
                continue
            }
            val visible = StoryRepository.visibleChoices(node, s)
            check(visible.isNotEmpty()) { "dead end at ${node.id} with flags ${s.flags}" }
            val choice = pick(node, visible)
                ?: visible.firstOrNull { (visits[it.nextNodeId] ?: 0) == 0 }
                ?: visible.first()
            s = s.applyChoice(choice)
        }
        error("no stop condition after $maxSteps steps; stuck near ${s.sceneId}")
    }

    /** A [play] picker that gives the aid answer and the Ilsevet fate, and leaves every other scene to the default. */
    fun decisions(aided: Boolean, fate: String): (StoryNode, List<Choice>) -> Choice? = { node, visible ->
        when (node.id) {
            AID_NODE -> visible.firstOrNull { it.nextNodeId == if (aided) "the_cost_aided" else "the_cost_unaided" }
            in FATE_NODES -> visible.firstOrNull { fate in it.consequences.setFlags }
            else -> null
        }
    }
}
