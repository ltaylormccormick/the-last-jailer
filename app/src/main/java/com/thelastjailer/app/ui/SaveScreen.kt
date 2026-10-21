package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.CommitStatus
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.OverwriteRequest
import com.thelastjailer.app.OverwriteResult
import com.thelastjailer.app.STALE_MESSAGE
import com.thelastjailer.app.SlotSaveRequest
import com.thelastjailer.app.confirmSlotOverwrite
import com.thelastjailer.app.requestSlotSave
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.StoryRepository

private const val NOT_SAVED_MESSAGE = "Could not save. Please check your save slots and try again."

/**
 * Lets the player pick or create the slot they're actively playing on. Autosaves during play
 * always go to [GameState.activeSlot], which this screen is the only place that changes.
 */
@Composable
fun SaveScreen(
    store: SaveStore,
    entitlements: EntitlementRepository,
    state: GameState,
    onStateChange: (GameState) -> Unit,
    modifier: Modifier = Modifier,
    recorder: ArrivalRecorder? = null
) {
    var confirmRestart by remember(state.activeSlot) { mutableStateOf(false) }
    // Not rememberSaveable on purpose: a rotation or process restart drops the question instead of resuming it.
    // A change of the game on screen also drops it. Dismissing never writes; see SlotOverwrite.kt.
    var pendingOverwrite by remember(state) { mutableStateOf<OverwriteRequest?>(null) }
    var notice by remember(state) { mutableStateOf<String?>(null) }

    pendingOverwrite?.let { request ->
        AlertDialog(
            onDismissRequest = {
                request.cancel()
                pendingOverwrite = null
            },
            title = { Text(request.title) },
            text = { Text(request.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingOverwrite = null
                        when (val result = store.confirmSlotOverwrite(request, state)) {
                            is OverwriteResult.Saved -> onStateChange(result.state)
                            is OverwriteResult.Stale -> { notice = result.message }
                            OverwriteResult.AlreadyHandled -> Unit
                            is OverwriteResult.NotSaved -> { notice = NOT_SAVED_MESSAGE }
                        }
                    }
                ) { Text(request.confirmLabel) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        request.cancel()
                        pendingOverwrite = null
                    }
                ) { Text("CANCEL") }
            }
        )
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Start again?") },
            text = {
                Text(
                    "This will replace Slot ${state.activeSlot} with a new game at Chapter I. " +
                        "Other save slots and your full-story purchase are not affected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val fresh = store.commitRestart(state.activeSlot, recorder)
                        onStateChange(fresh)
                        confirmRestart = false
                    }
                ) { Text("START AGAIN") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("CANCEL") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SAVE SLOTS", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Text(
            "${entitlements.maxSaveSlots()} slot${if (entitlements.maxSaveSlots() == 1) "" else "s"} available" +
                if (!entitlements.hasUnlockedFullStory()) " · unlock the full story for more" else "",
            style = MaterialTheme.typography.bodyMedium
        )

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("ACTIVE SLOT ${state.activeSlot}", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
            Text(
                "Go Back restores the state from before your last story choice — including health, stats, items and flags. " +
                    "Trophies you have earned are kept. " +
                    "It is cleared after combat or shop purchases.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                enabled = store.hasPreviousState(state.activeSlot),
                onClick = {
                    store.restorePreviousState(state.activeSlot)?.let(onStateChange)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("GO BACK ONE STEP") }
            TextButton(
                onClick = { confirmRestart = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("START AGAIN") }
        }

        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JailerColors.Gold) }

        (1..entitlements.maxSaveSlots()).forEach { slot ->
            val saved = store.load(slot)
            val isActive = slot == state.activeSlot
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Slot $slot" + if (isActive) " (active)" else "", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (saved != null) {
                                val node = StoryRepository.node(saved.sceneId)
                                "${node.title} · Level ${saved.level}"
                            } else "Empty",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row {
                        TextButton(onClick = {
                            // Saving the active slot or an empty slot happens at once. Replacing a different
                            // occupied slot asks first (requestSlotSave); the copy itself is commitSlotSave.
                            pendingOverwrite?.cancel()
                            pendingOverwrite = null
                            notice = null
                            when (val result = store.requestSlotSave(slot, state)) {
                                is SlotSaveRequest.Saved -> onStateChange(result.state)
                                is SlotSaveRequest.NeedsConfirmation -> { pendingOverwrite = result.request }
                                SlotSaveRequest.Stale -> { notice = STALE_MESSAGE }
                                is SlotSaveRequest.NotSaved -> { notice = NOT_SAVED_MESSAGE }
                            }
                        }) { Text("SAVE") }
                        OutlinedButton(
                            enabled = saved != null,
                            onClick = {
                                saved?.let {
                                    if (store.setActiveSlot(slot) == CommitStatus.SAVED) {
                                        onStateChange(it)
                                    } else {
                                        notice = NOT_SAVED_MESSAGE
                                    }
                                }
                            }
                        ) { Text("LOAD") }
                    }
                }
            }
        }
    }
}
