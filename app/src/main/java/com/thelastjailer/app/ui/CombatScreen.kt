package com.thelastjailer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatEngine
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.EnemyCatalog

private const val HEALING_DRAUGHT_ID = "healing_draught"

/**
 * A turn-based fight: Attack / Defend / (if carried) drink a Healing Draught, resolved one round
 * at a time against the enemy's own attack. Never fatal to the run — a loss still continues the
 * story via [CombatEncounter.defeatNodeId], just with no reward. All the actual resolution logic
 * lives in [CombatEngine]; this screen just renders it and forwards button taps.
 */
@Composable
fun CombatScreen(
    encounter: CombatEncounter,
    playerState: GameState,
    onResolved: (CombatOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember(encounter.id) {
        CombatEngine(
            enemy = EnemyCatalog.get(encounter.enemyId),
            startingPlayerHealth = playerState.health,
            playerMaxHealth = playerState.maxHealth,
            playerCourage = playerState.courage,
            availableDraughts = playerState.inventory.count { it == HEALING_DRAUGHT_ID }
        )
    }
    val enemy = remember(encounter.id) { EnemyCatalog.get(encounter.enemyId) }

    Column(modifier = modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⚔ ${enemy.name.uppercase()}", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text(enemy.name, style = MaterialTheme.typography.titleMedium)
            Text(enemy.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Enemy health: ${engine.enemyHealth}/${enemy.maxHealth}", style = MaterialTheme.typography.bodyMedium)
            Text("Your health: ${engine.playerHealth}/${playerState.maxHealth}", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).background(JailerColors.Panel, RoundedCornerShape(8.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(engine.log.asReversed()) { line ->
                Text("• $line", style = MaterialTheme.typography.bodyMedium)
            }
        }

        val currentOutcome = engine.outcome
        if (currentOutcome == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { engine.attack() }) { Text("ATTACK") }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = { engine.defend() }) { Text("DEFEND") }
                if (engine.remainingDraughts > 0) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = { engine.useDraught() }) { Text("DRINK DRAUGHT (+HP)") }
                }
            }
        } else {
            Text(
                if (currentOutcome.victory) "VICTORY" else "YOU SURVIVE, BATTERED",
                style = MaterialTheme.typography.labelLarge,
                color = JailerColors.Gold
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onResolved(currentOutcome) }) {
                Text("CONTINUE")
            }
        }
    }
}
