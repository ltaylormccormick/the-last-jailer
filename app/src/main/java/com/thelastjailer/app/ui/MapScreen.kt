package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.StoryRepository

/** Minimal chapter map: which chapters exist and whether they're unlocked. Real map art is future work. */
@Composable
fun MapScreen(entitlements: EntitlementRepository, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MAP", style = MaterialTheme.typography.labelLarge)
        StoryRepository.chapters.forEach { chapter ->
            val unlocked = entitlements.isChapterUnlocked(chapter.id)
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.title, style = MaterialTheme.typography.bodyLarge)
                    Text(if (unlocked) "UNLOCKED" else "LOCKED", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
