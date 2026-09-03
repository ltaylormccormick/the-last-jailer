package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState

/** Stub — a real journal (flags/discoveries encountered so far) is future work. */
@Composable
fun JournalScreen(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(14.dp)) {
        Text("JOURNAL", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        OrnatePanel(modifier = Modifier.padding(top = 8.dp)) {
            if (state.flags.isEmpty()) {
                Text("Nothing recorded yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.flags.sorted().forEach { flag ->
                    Text("• ${flag.replace('_', ' ')}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
