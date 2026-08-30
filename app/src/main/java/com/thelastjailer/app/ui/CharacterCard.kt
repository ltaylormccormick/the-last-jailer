package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.GameState

/** Compact character summary shown at the foot of the Story screen. */
@Composable
fun CharacterSummaryCard(state: GameState, modifier: Modifier = Modifier) {
    OrnatePanel(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("KAELEN", color = JailerColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("The Fallen Knight · Level ${state.level}", color = JailerColors.TextPrimary.copy(alpha = .8f), fontSize = 11.sp)
            }
            Text("❤ ${state.health}/${state.maxHealth}", color = JailerColors.HealthRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
