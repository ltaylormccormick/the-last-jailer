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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.EnemyCatalog
import kotlin.random.Random

private const val HEALING_DRAUGHT_ID = "healing_draught"
private const val HEALING_DRAUGHT_AMOUNT = 25

/**
 * A turn-based fight: Attack / Defend / (if carried) drink a Healing Draught, resolved one round
 * at a time against the enemy's own attack. Never fatal to the run — a loss still continues the
 * story via [CombatEncounter.defeatNodeId], just with no reward.
 */
@Composable
fun CombatScreen(
    encounter: CombatEncounter,
    playerState: GameState,
    onResolved: (CombatOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val enemy = remember(encounter.id) { EnemyCatalog.get(encounter.enemyId) }
    var enemyHealth by remember(encounter.id) { mutableStateOf(enemy.maxHealth) }
    var playerHealth by remember(encounter.id) { mutableStateOf(playerState.health) }
    var consumedItems by remember(encounter.id) { mutableStateOf(listOf<String>()) }
    var log by remember(encounter.id) { mutableStateOf(listOf("The ${enemy.name} lunges out of the dark.")) }
    var outcome by remember(encounter.id) { mutableStateOf<CombatOutcome?>(null) }

    fun buildOutcome(victory: Boolean) = CombatOutcome(
        victory = victory,
        damageTaken = (playerState.health - playerHealth).coerceAtLeast(0),
        consumedItemIds = consumedItems
    )

    /** Enemy strikes back; returns the log line and finalizes [outcome] on a knockout. */
    fun enemyStrikes(reduced: Boolean): String {
        val raw = Random.nextInt(enemy.minAttack, enemy.maxAttack + 1)
        val dmg = if (reduced) raw / 2 else raw
        playerHealth = (playerHealth - dmg).coerceAtLeast(0)
        return "The ${enemy.name} hits you for $dmg damage."
    }

    val availableDraughts = playerState.inventory.count { it == HEALING_DRAUGHT_ID } -
        consumedItems.count { it == HEALING_DRAUGHT_ID }

    Column(modifier = modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⚔ ${enemy.name.uppercase()}", style = MaterialTheme.typography.labelLarge)

        OrnatePanel(modifier = Modifier.fillMaxWidth()) {
            Text(enemy.name, style = MaterialTheme.typography.titleMedium)
            Text(enemy.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Enemy health: $enemyHealth/${enemy.maxHealth}", style = MaterialTheme.typography.bodyMedium)
            Text("Your health: $playerHealth/${playerState.maxHealth}", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).background(JailerColors.Panel, RoundedCornerShape(8.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(log.asReversed()) { line ->
                Text("• $line", style = MaterialTheme.typography.bodyMedium)
            }
        }

        val currentOutcome = outcome
        if (currentOutcome == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val dmg = Random.nextInt(8, 15) + (playerState.courage / 2)
                        enemyHealth = (enemyHealth - dmg).coerceAtLeast(0)
                        val round = mutableListOf("You strike the ${enemy.name} for $dmg damage.")
                        if (enemyHealth <= 0) {
                            round += "The ${enemy.name} falls."
                            outcome = buildOutcome(victory = true)
                        } else {
                            round += enemyStrikes(reduced = false)
                            if (playerHealth <= 0) {
                                round += "You collapse — but you're still breathing."
                                outcome = buildOutcome(victory = false)
                            }
                        }
                        log = log + round
                    }
                ) { Text("ATTACK") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val round = mutableListOf("You brace for the next blow.")
                        round += enemyStrikes(reduced = true)
                        if (playerHealth <= 0) {
                            round += "You collapse — but you're still breathing."
                            outcome = buildOutcome(victory = false)
                        }
                        log = log + round
                    }
                ) { Text("DEFEND") }

                if (availableDraughts > 0) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            consumedItems = consumedItems + HEALING_DRAUGHT_ID
                            playerHealth = (playerHealth + HEALING_DRAUGHT_AMOUNT).coerceAtMost(playerState.maxHealth)
                            val round = mutableListOf("You drink a Healing Draught and feel restored.")
                            round += enemyStrikes(reduced = false)
                            if (playerHealth <= 0) {
                                round += "You collapse — but you're still breathing."
                                outcome = buildOutcome(victory = false)
                            }
                            log = log + round
                        }
                    ) { Text("DRAUGHT") }
                }
            }
        } else {
            Text(
                if (currentOutcome.victory) "VICTORY" else "YOU SURVIVE, BATTERED",
                style = MaterialTheme.typography.labelLarge
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onResolved(currentOutcome) }) {
                Text("CONTINUE")
            }
        }
    }
}
