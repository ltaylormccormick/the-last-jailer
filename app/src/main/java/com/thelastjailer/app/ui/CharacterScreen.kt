package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState

@Composable
fun CharacterScreen(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CHARACTER", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("Kaelen, the Fallen Knight", style = MaterialTheme.typography.headlineSmall)
            Text("Level ${state.level} · XP ${state.xp}/${state.xpToNextLevel}", style = MaterialTheme.typography.bodyMedium)
        }

        StatsBar(state)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text("TROPHIES", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
            if (state.trophies.isEmpty()) {
                Text("None earned yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(state.trophies.toList()) { trophy ->
                        Text("🏆 $trophy", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
