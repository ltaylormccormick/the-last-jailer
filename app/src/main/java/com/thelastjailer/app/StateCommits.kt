package com.thelastjailer.app

/**
 * The three places a play session changes and persists the active [GameState], pulled out of
 * `MainActivity` so their save/undo contract can be unit tested. Behaviour is exactly what the
 * activity did inline before:
 *
 * - an ordinary story choice keeps one exact pre-choice snapshot (for "go back one step"), applies
 *   the choice and autosaves;
 * - a resolved fight is deliberately irreversible, so it clears the snapshot before autosaving;
 * - a shop purchase also clears the snapshot, so a later undo cannot refund or duplicate it.
 */
fun SaveStore.commitChoice(state: GameState, choice: Choice): GameState {
    savePreviousState(state.activeSlot, state)
    val next = state.applyChoice(choice)
    save(next.activeSlot, next)
    return next
}

fun SaveStore.commitCombatResult(state: GameState, encounter: CombatEncounter, outcome: CombatOutcome): GameState {
    val next = state.resolveCombat(encounter, outcome)
    clearPreviousState(next.activeSlot)
    save(next.activeSlot, next)
    return next
}

fun SaveStore.commitPurchase(state: GameState, itemId: String, price: Int): GameState {
    val next = state.purchaseItem(itemId, price)
    clearPreviousState(next.activeSlot)
    save(next.activeSlot, next)
    return next
}
