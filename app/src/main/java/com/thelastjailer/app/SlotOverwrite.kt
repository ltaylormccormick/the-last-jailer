package com.thelastjailer.app

import com.thelastjailer.app.data.StoryRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Batch 2C-1b-1: the confirmation that stands between SAVE and an occupied slot.
 *
 * Everything here is plain Kotlin so the decision logic is covered by JVM tests; the Save screen only shows
 * the dialog and forwards taps. Nothing in this file writes anything except the one [commitSlotSave] call at
 * the end of a successful [confirmSlotOverwrite] - that is the same single editor batch Batch 2C-1a made, so
 * the save format, the trophy replacement, the Back-snapshot clearing and the timeline invalidation are
 * exactly what they were. All reads here are reads of the already-loaded preferences map (the Save screen
 * already does the same on every recomposition), so no new disk operation runs on the main thread.
 *
 * The flow:
 * 1. [requestSlotSave] when SAVE is pressed. Saving onto the active slot, or into an empty slot, commits
 *    straight away as before. Only a different, occupied destination returns a [OverwriteRequest].
 * 2. The player reads the dialog. Cancel / dismiss / system Back / rotation call nothing that writes.
 * 3. [confirmSlotOverwrite] re-checks everything against the live data and only then commits. A request is
 *    single use: it is spent the moment confirm is attempted (or cancelled), so a second tap, a queued tap
 *    or a stale dialog can never commit twice. Any mismatch aborts without writing.
 */

/** What happened when SAVE was pressed. */
sealed class SlotSaveRequest {
    /** Saved straight away (same slot, or an empty destination). */
    class Saved(val state: GameState) : SlotSaveRequest()

    /** The destination holds a different game: ask first. Nothing has been written. */
    class NeedsConfirmation(val request: OverwriteRequest) : SlotSaveRequest()

    /** The screen was out of date (see [requestSlotSave]); nothing was written. */
    object Stale : SlotSaveRequest()
}

/** What happened when the player confirmed. */
sealed class OverwriteResult {
    class Saved(val state: GameState) : OverwriteResult()

    /** Something changed since the dialog opened. Nothing was written. */
    class Stale(val reason: StaleReason) : OverwriteResult() {
        val message: String get() = STALE_MESSAGE
    }

    /** The request was already confirmed or cancelled. Nothing was written (again). */
    object AlreadyHandled : OverwriteResult()
}

enum class StaleReason {
    /** The state on screen is no longer the game the dialog was opened for. */
    SCREEN_STATE_CHANGED,

    /** The persisted active slot is no longer the slot being copied from. */
    ACTIVE_SLOT_CHANGED,

    /** The stored data of the slot being copied from changed. */
    SOURCE_SAVE_CHANGED,

    /** The stored data of the destination slot changed (or it was emptied). */
    DESTINATION_CHANGED
}

const val STALE_MESSAGE = "Your save slots changed while you were deciding, so nothing was saved. Please try again."

/**
 * One pending "replace this slot?" question. It captures an exact identity of the game being copied and of
 * everything stored in the destination slot, and is spent after one use.
 */
class OverwriteRequest internal constructor(
    val sourceSlot: Int,
    val destinationSlot: Int,
    val title: String,
    val message: String,
    val confirmLabel: String,
    internal val sourceIdentity: String,
    internal val sourceSignature: String,
    internal val destinationSignature: String
) {
    private val spent = AtomicBoolean(false)

    /** True once the request was confirmed (successfully or not) or cancelled. */
    val isSpent: Boolean get() = spent.get()

    /** Takes the single use. Only the caller that gets `true` may go on to commit. */
    internal fun claim(): Boolean = spent.compareAndSet(false, true)

    /** Cancel, dismiss, Back: spends the request without writing anything. */
    fun cancel() {
        spent.set(true)
    }
}

/**
 * Called when SAVE is pressed for [slot] with the game currently on screen.
 *
 * The occupancy check is made now, on the live data, not taken from what the row showed when it was last drawn.
 * On every path it refuses to act when the screen is behind the store (a tap queued behind an earlier SAVE would
 * otherwise copy or ask about a game the player has already moved off); nothing is written then.
 */
fun SaveStore.requestSlotSave(
    slot: Int,
    state: GameState,
    titleOf: (String) -> String = { StoryRepository.nodeOrNull(it)?.title ?: it }
): SlotSaveRequest {
    // First of all: a tap carrying a screen that is behind the store (a queued tap after an earlier SAVE switched
    // the active slot) writes nothing, on every path. A game with no persisted active slot is always current.
    if (!screenIsCurrent(state)) return SlotSaveRequest.Stale
    if (slot == state.activeSlot || !hasSave(slot)) return SlotSaveRequest.Saved(commitSlotSave(slot, state))
    val destination = load(slot) ?: return SlotSaveRequest.Saved(commitSlotSave(slot, state))
    return SlotSaveRequest.NeedsConfirmation(
        OverwriteRequest(
            sourceSlot = state.activeSlot,
            destinationSlot = slot,
            title = "Replace Slot $slot?",
            message = "Slot $slot currently holds ${summary(destination, titleOf)}.\n\n" +
                "Saving will replace that game - its progress and its trophies - with your current game " +
                "from Slot ${state.activeSlot} (${summary(state, titleOf)}).\n\n" +
                "You will continue playing in Slot $slot. Slot ${state.activeSlot} is not changed.\n\n" +
                "This cannot be undone: the game being replaced cannot be recovered.",
            confirmLabel = "REPLACE SLOT $slot",
            sourceIdentity = state.identity(),
            sourceSignature = slotSignature(state.activeSlot),
            destinationSignature = slotSignature(slot)
        )
    )
}

/**
 * Called when the player taps the dialog's confirm button, with the game that is on screen right now.
 * Spends the request first, then re-checks the game on screen, the source slot and the whole destination slot
 * against what the dialog was opened for, and only if every check passes performs the 2C-1a slot copy.
 */
fun SaveStore.confirmSlotOverwrite(request: OverwriteRequest, current: GameState): OverwriteResult {
    if (!request.claim()) return OverwriteResult.AlreadyHandled
    if (current.activeSlot != request.sourceSlot || current.identity() != request.sourceIdentity) {
        return OverwriteResult.Stale(StaleReason.SCREEN_STATE_CHANGED)
    }
    if (!screenIsCurrent(current)) return OverwriteResult.Stale(StaleReason.ACTIVE_SLOT_CHANGED)
    if (slotSignature(request.sourceSlot) != request.sourceSignature) {
        return OverwriteResult.Stale(StaleReason.SOURCE_SAVE_CHANGED)
    }
    if (!hasSave(request.destinationSlot) || slotSignature(request.destinationSlot) != request.destinationSignature) {
        return OverwriteResult.Stale(StaleReason.DESTINATION_CHANGED)
    }
    return OverwriteResult.Saved(commitSlotSave(request.destinationSlot, current))
}

/** The persisted active slot, when there is one, is the slot the screen thinks it is playing on. */
private fun SaveStore.screenIsCurrent(state: GameState): Boolean {
    val persisted = currentActiveSlot()
    return persisted == null || persisted == state.activeSlot
}

private fun summary(state: GameState, titleOf: (String) -> String): String {
    val trophies = state.trophies.size
    val noun = if (trophies == 1) "trophy" else "trophies"
    return "${titleOf(state.sceneId)} · Level ${state.level} · $trophies $noun"
}

/**
 * Every field of a [GameState], exact and order-independent for sets, in a form where two different games can
 * never produce the same text. Used to tell "the game on screen is still the one the dialog was opened for".
 */
internal fun GameState.identity(): String = buildString {
    fun field(name: String, value: String) {
        append(name).append('=').append(value.length).append(':').append(value).append(';')
    }
    fun items(values: Collection<String>) = values.joinToString("") { "${it.length}:$it" }
    field("slot", activeSlot.toString())
    field("chapter", chapterId)
    field("scene", sceneId)
    field("courage", courage.toString())
    field("honour", honour.toString())
    field("health", health.toString())
    field("maxHealth", maxHealth.toString())
    field("gold", gold.toString())
    field("level", level.toString())
    field("xp", xp.toString())
    field("xpToNext", xpToNextLevel.toString())
    field("inventory", items(inventory))
    field("trophies", items(trophies.sorted()))
    field("flags", items(flags.sorted()))
}
