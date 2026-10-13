package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.FlagDescriptions

/**
 * Kaelen's record of the road: short first-person lines grouped by kind, built from the story flags
 * the player has set. Internal flags and flags the game doesn't recognise (for example from an old
 * save) are never shown, and a raw flag name is never shown.
 */
@Composable
fun JournalScreen(state: GameState, modifier: Modifier = Modifier) {
    val sections = remember(state.flags) { FlagDescriptions.sections(state.flags) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text("JOURNAL", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        if (sections.isEmpty()) {
            OrnatePanel(modifier = Modifier.padding(top = 8.dp)) {
                Text("Nothing recorded yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            sections.forEach { section ->
                Text(
                    section.category.title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = JailerColors.Gold,
                    modifier = Modifier.padding(top = 14.dp)
                )
                OrnatePanel(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    section.lines.forEach { line ->
                        Text(
                            "• $line",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
