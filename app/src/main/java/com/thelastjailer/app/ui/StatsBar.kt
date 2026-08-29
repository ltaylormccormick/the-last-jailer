package com.thelastjailer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.GameState

@Composable
fun StatsBar(state: GameState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(JailerColors.Panel, RoundedCornerShape(6.dp)).padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("❤ ${state.health}/${state.maxHealth}", color = JailerColors.HealthRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("⚔ COURAGE ${state.courage}", color = JailerColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("⚖ HONOUR ${state.honour}", color = JailerColors.HonourBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("LVL ${state.level} · XP ${state.xp}/${state.xpToNextLevel}", color = JailerColors.TextPrimary, fontSize = 12.sp)
            Text("⛁ ${state.gold}", color = JailerColors.Gold, fontSize = 12.sp)
            Text("🏆 ${state.trophies.size}", color = JailerColors.TextPrimary, fontSize = 12.sp)
        }
    }
}
