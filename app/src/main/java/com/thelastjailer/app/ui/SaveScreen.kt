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
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.StoryRepository

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
    modifier: Modifier = Modifier
) {
    var confirmRestart by remember(state.activeSlot) { mutableStateOf(false) }

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
                        val fresh = store.restart(state.activeSlot)
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
                "Go Back restores the complete state from before your last story choice — including health, stats, items and flags. " +
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
                            val movedState = state.copy(activeSlot = slot)
                            store.save(slot, movedState)
                            // Copying progress into another slot must not inherit that slot's old
                            // one-step checkpoint. Saving the currently active slot preserves its
                            // legitimate story undo checkpoint.
                            if (slot != state.activeSlot) store.clearPreviousState(slot)
                            onStateChange(movedState)
                        }) { Text("SAVE") }
                        OutlinedButton(
                            enabled = saved != null,
                            onClick = {
                                saved?.let {
                                    store.setActiveSlot(slot)
                                    onStateChange(it)
                                }
                            }
                        ) { Text("LOAD") }
                    }
                }
            }
        }
    }
}
