package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    Column(modifier = modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SAVE SLOTS", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Text(
            "${entitlements.maxSaveSlots()} slot${if (entitlements.maxSaveSlots() == 1) "" else "s"} available" +
                if (!entitlements.hasUnlockedFullStory()) " · unlock the full story for more" else "",
            style = MaterialTheme.typography.bodyMedium
        )

        (1..entitlements.maxSaveSlots()).forEach { slot ->
            val saved = store.load(slot)
            val isActive = slot == state.activeSlot
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                            store.save(slot, state.copy(activeSlot = slot))
                            store.setActiveSlot(slot)
                            onStateChange(state.copy(activeSlot = slot))
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
