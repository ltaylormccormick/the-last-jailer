package com.thelastjailer.app

import com.thelastjailer.app.checkpoint.ArrivalPlan
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.checkpoint.TimelineWrite

/**
 * What happened to a commit's write (G2L B2). The gate is permanently open in this release, so the only ways to
 * land on anything but [SAVED] are an editor that cannot be created or an `apply()` that throws.
 */
enum class CommitStatus {
    /** The batch was handed to SharedPreferences. ("Handed" is not "on disk": `apply()` gives no result.) */
    SAVED,

    /** Nothing was written and no checkpoint work ran. The game is exactly as it was. */
    NOT_SAVED,

    /** `apply()` itself threw: memory may or may not hold the change, so the result carries what is stored now. */
    UNCERTAIN
}

/**
 * [state] is the game the player should see next: the new state only when [status] is [CommitStatus.SAVED];
 * the unchanged state when nothing was written; what is actually stored now when the outcome is uncertain.
 * A failed or uncertain write therefore never advances the story, inventory or currency on screen.
 */
class CommitResult(val state: GameState, val status: CommitStatus)

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
fun SaveStore.commitChoiceResult(state: GameState, choice: Choice, recorder: ArrivalRecorder? = null): CommitResult =
    commitTransition(state, state.applyChoice(choice), recorder, backSnapshot = state, clearBack = false)

fun SaveStore.commitChoice(state: GameState, choice: Choice, recorder: ArrivalRecorder? = null): GameState =
    commitChoiceResult(state, choice, recorder).state

fun SaveStore.commitCombatResultOutcome(
    state: GameState,
    encounter: CombatEncounter,
    outcome: CombatOutcome,
    recorder: ArrivalRecorder? = null
): CommitResult = commitTransition(state, state.resolveCombat(encounter, outcome), recorder, backSnapshot = null, clearBack = true)

fun SaveStore.commitCombatResult(
    state: GameState,
    encounter: CombatEncounter,
    outcome: CombatOutcome,
    recorder: ArrivalRecorder? = null
): GameState = commitCombatResultOutcome(state, encounter, outcome, recorder).state

fun SaveStore.commitPurchaseResult(state: GameState, itemId: String, price: Int, recorder: ArrivalRecorder? = null): CommitResult =
    commitTransition(state, state.purchaseItem(itemId, price), recorder, backSnapshot = null, clearBack = true)

fun SaveStore.commitPurchase(state: GameState, itemId: String, price: Int, recorder: ArrivalRecorder? = null): GameState =
    commitPurchaseResult(state, itemId, price, recorder).state

/**
 * Start Again: replaces the slot with a fresh Chapter I game. This is a different timeline, so the old
 * lineage and ledger are dropped in the same batch; with a recorder the new timeline starts with its
 * Chapter 1 record. The Back snapshot goes in the same batch too.
 */
fun SaveStore.commitRestartResult(slot: Int, recorder: ArrivalRecorder? = null): CommitResult {
    val fresh = GameState(activeSlot = slot)
    val plan = recorder?.planRestart(fresh)
    val status = save(
        slot, fresh, plan?.timeline ?: TimelineWrite.Invalidate, clearBackSnapshot = true,
        after = if (plan != null) ({ recorder.afterSave(slot, plan) }) else null
    )
    return when (status) {
        CommitStatus.SAVED -> CommitResult(fresh, status)
        CommitStatus.NOT_SAVED, CommitStatus.UNCERTAIN -> CommitResult(load(slot) ?: fresh, status)
    }
}

fun SaveStore.commitRestart(slot: Int, recorder: ArrivalRecorder? = null): GameState = commitRestartResult(slot, recorder).state

/**
 * SAVE on the slot screen. Saving onto the active slot keeps its timeline and its Back snapshot; copying into
 * another slot makes the destination a different game, so in **one** editor batch the destination gets the
 * incoming state (and so the incoming trophies - the destination's old trophies are replaced, never merged),
 * its lineage and ledger are removed (the old ledger must not vouch for the copied-in state), and its old
 * Back snapshot is removed. One batch means a crash leaves either the old game complete or the new game
 * complete, never the new game next to the old game's snapshot. The source slot is not written.
 */
fun SaveStore.commitSlotSaveResult(slot: Int, state: GameState): CommitResult {
    val moved = state.copy(activeSlot = slot)
    val sameSlot = slot == state.activeSlot
    val status = save(
        slot,
        moved,
        if (sameSlot) TimelineWrite.Keep else TimelineWrite.Invalidate,
        clearBackSnapshot = !sameSlot
    )
    return when (status) {
        CommitStatus.SAVED -> CommitResult(moved, status)
        CommitStatus.NOT_SAVED -> CommitResult(state, status)
        CommitStatus.UNCERTAIN -> CommitResult(load(state.activeSlot) ?: state, status)
    }
}

fun SaveStore.commitSlotSave(slot: Int, state: GameState): GameState = commitSlotSaveResult(slot, state).state

/**
 * One story transition in ONE gated batch: the Back snapshot (or its removal), the new state, the stamp and the
 * timeline keys. The recorder's sequence is unchanged: `beforeCommit` on the pre-change state ahead of the main
 * apply, then `afterSave` after the batch was handed over. Neither runs when the write is refused or fails. Because
 * `beforeCommit` can change whether the slot is still recording, the plan is checked again after it and the
 * timeline keys restaged if it changed, so the result is the plan the old order (beforeCommit, then plan) produced.
 */
private fun SaveStore.commitTransition(
    state: GameState,
    next: GameState,
    recorder: ArrivalRecorder?,
    backSnapshot: GameState?,
    clearBack: Boolean
): CommitResult {
    val slot = next.activeSlot
    var plan: ArrivalPlan? = recorder?.plan(state, next, readTimeline(slot))
    val plannedWhileRecording = recorder?.isRecording(slot)
    val status = save(
        slot, next, plan?.timeline ?: TimelineWrite.Invalidate,
        clearBackSnapshot = clearBack, backSnapshot = backSnapshot,
        before = if (recorder != null) ({ recorder.beforeCommit(state.activeSlot, state, readTimeline(state.activeSlot)) }) else null,
        after = if (recorder != null) ({ plan?.let { recorder.afterSave(slot, it) } }) else null,
        retimeline = if (recorder != null) {
            {
                if (recorder.isRecording(slot) != plannedWhileRecording) {
                    val replanned = recorder.plan(state, next, readTimeline(slot))
                    plan = replanned
                    replanned.timeline
                } else {
                    null
                }
            }
        } else {
            null
        }
    )
    return when (status) {
        CommitStatus.SAVED -> CommitResult(next, status)
        CommitStatus.NOT_SAVED -> CommitResult(state, status)
        CommitStatus.UNCERTAIN -> CommitResult(load(slot) ?: state, status)
    }
}
