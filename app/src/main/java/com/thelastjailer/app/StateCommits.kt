package com.thelastjailer.app

import com.thelastjailer.app.checkpoint.ArrivalPlan
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.checkpoint.TimelineWrite

/**
 * The places a play session changes and persists the active [GameState], pulled out of
 * `MainActivity` so their save/undo contract can be unit tested. Behaviour is exactly what the
 * activity did inline before:
 *
 * - an ordinary story choice keeps one exact pre-choice snapshot (for "go back one step"), applies
 *   the choice and autosaves;
 * - a resolved fight is deliberately irreversible, so it clears the snapshot before autosaving;
 * - a shop purchase also clears the snapshot, so a later undo cannot refund or duplicate it.
 *
 * Each of them accepts an optional [ArrivalRecorder]. It records chapter arrivals into the (still
 * invisible) checkpoint files and can never change what is returned or saved for the player: its plan only
 * decides what happens to the slot's timeline keys inside the same autosave batch, and every failure on its
 * side is contained. Without a recorder the slot's timeline is simply invalidated by the default `save`.
 */
fun SaveStore.commitChoice(state: GameState, choice: Choice, recorder: ArrivalRecorder? = null): GameState {
    recorder?.beforeCommit(state.activeSlot, state, readTimeline(state.activeSlot))
    savePreviousState(state.activeSlot, state)
    val next = state.applyChoice(choice)
    saveTransition(state, next, recorder)
    return next
}

fun SaveStore.commitCombatResult(
    state: GameState,
    encounter: CombatEncounter,
    outcome: CombatOutcome,
    recorder: ArrivalRecorder? = null
): GameState {
    recorder?.beforeCommit(state.activeSlot, state, readTimeline(state.activeSlot))
    val next = state.resolveCombat(encounter, outcome)
    clearPreviousState(next.activeSlot)
    saveTransition(state, next, recorder)
    return next
}

fun SaveStore.commitPurchase(state: GameState, itemId: String, price: Int, recorder: ArrivalRecorder? = null): GameState {
    recorder?.beforeCommit(state.activeSlot, state, readTimeline(state.activeSlot))
    val next = state.purchaseItem(itemId, price)
    clearPreviousState(next.activeSlot)
    saveTransition(state, next, recorder)
    return next
}

/**
 * Start Again: replaces the slot with a fresh Chapter I game. This is a different timeline, so the old
 * lineage and ledger are dropped in the same batch; with a recorder the new timeline starts with its
 * Chapter 1 record.
 */
fun SaveStore.commitRestart(slot: Int, recorder: ArrivalRecorder? = null): GameState {
    clearPreviousState(slot)
    val fresh = GameState(activeSlot = slot)
    val plan = recorder?.planRestart(fresh)
    save(slot, fresh, plan?.timeline ?: TimelineWrite.Invalidate)
    if (plan != null) recorder.afterSave(slot, plan)
    return fresh
}

/**
 * SAVE on the slot screen. Saving onto the active slot keeps its timeline; copying into another slot makes
 * the destination a different timeline (its old ledger must not vouch for the copied-in state), so the
 * destination's lineage and ledger are removed in the same batch. The destination's one-step Back snapshot
 * is cleared as before.
 */
fun SaveStore.commitSlotSave(slot: Int, state: GameState): GameState {
    val moved = state.copy(activeSlot = slot)
    val sameSlot = slot == state.activeSlot
    save(slot, moved, if (sameSlot) TimelineWrite.Keep else TimelineWrite.Invalidate)
    if (!sameSlot) clearPreviousState(slot)
    return moved
}

private fun SaveStore.saveTransition(state: GameState, next: GameState, recorder: ArrivalRecorder?) {
    val plan: ArrivalPlan? = recorder?.plan(state, next, readTimeline(next.activeSlot))
    save(next.activeSlot, next, plan?.timeline ?: TimelineWrite.Invalidate)
    if (plan != null) recorder.afterSave(next.activeSlot, plan)
}
