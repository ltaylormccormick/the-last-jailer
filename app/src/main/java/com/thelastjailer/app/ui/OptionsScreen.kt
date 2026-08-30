package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.BuildConfig
import com.thelastjailer.app.data.EntitlementRepository

@Composable
fun OptionsScreen(entitlements: EntitlementRepository, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("OPTIONS", style = MaterialTheme.typography.labelLarge)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                if (entitlements.hasUnlockedFullStory()) "Full story unlocked" else "Chapters I-III free",
                style = MaterialTheme.typography.bodyLarge
            )
            if (!entitlements.hasUnlockedFullStory()) {
                Button(onClick = { entitlements.unlockFullStory() }) {
                    Text("UNLOCK FULL STORY")
                }
            }
        }

        if (BuildConfig.DEBUG) {
            var simulated by remember { mutableStateOf(entitlements.hasUnlockedFullStory()) }
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Text("DEBUG", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Simulate purchased", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = simulated,
                        onCheckedChange = {
                            simulated = it
                            entitlements.setDebugPurchaseSimulated(it)
                        }
                    )
                }
            }
        }
    }
}
