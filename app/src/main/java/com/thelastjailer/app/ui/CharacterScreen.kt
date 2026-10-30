package com.thelastjailer.app.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.StoryRepository

@Composable
fun CharacterScreen(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CHARACTER", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("Kaelen, the Fallen Knight", style = MaterialTheme.typography.headlineSmall)
            Text("Level ${state.level} · XP ${state.xp}/${state.xpToNextLevel}", style = MaterialTheme.typography.bodyMedium)
        }

        StatsBar(state)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("WHAT YOUR STATS DO", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
            Text(
                "Courage: bigger hits on Attack, and steadier nerve on Defend too, though Attack " +
                    "still pays out more. High Courage also unlocks the boldest dialogue choices.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Honour: touches no fight at all. It measures the choices Kaelen makes, not the " +
                    "battles he wins, and enough of it opens the more honest, costlier answers " +
                    "other characters won't give him otherwise.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("TROPHIES · ${state.trophies.intersect(StoryRepository.availableTrophies).size} / ${StoryRepository.availableTrophies.size}", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
            // Presentation only: says the x / 37 above is the original collection, once the separate Endings panel exists.
            EndingsPanel.collectionNote(state)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // The nine ending trophies are shown in the Endings panel below, not in this list; nothing else is filtered.
            val listed = EndingsPanel.listedTrophies(state.trophies)
            if (listed.isEmpty()) {
                Text("None earned yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column {
                    listed.forEach { trophy ->
                        Text("🏆 $trophy", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        EndingsSection(state)
    }
}
