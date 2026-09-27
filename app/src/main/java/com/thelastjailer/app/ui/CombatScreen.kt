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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatEngine
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.EnemyCatalog
import com.thelastjailer.app.data.ItemCatalog
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.levelAttackBonus
import com.thelastjailer.app.levelDamageReduction

private const val HEALING_DRAUGHT_ID = "healing_draught"
private const val GREATER_HEALING_DRAUGHT_ID = "greater_healing_draught"
private const val KAELEN_COMBAT_PORTRAIT = "kaelen_combat_portrait_v2"
private const val LOW_HEALTH_FRACTION = 0.30f

/** Explicit opponent art avoids selecting a different story scene on different routes. */
private val dedicatedEnemyPortraits = mapOf(
    "cave_lurker" to "cave_lurker_combat",
    "seal_wraith" to "seal_wraith_combat",
    "ashen_vanguard" to "ashen_vanguard_combat"
)

/**
 * Story-first turn-based combat. Kaelen uses a dedicated combat portrait; enemy artwork is
 * gradually gaining dedicated enemy portraits. The full log remains available underneath but
 * no longer has to carry the entire presentation by itself.
 */
@Composable
fun CombatScreen(
    encounter: CombatEncounter,
    playerState: GameState,
    onResolved: (CombatOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val enemy = remember(encounter.id) { EnemyCatalog.get(encounter.enemyId) }
    val dedicatedPortrait = dedicatedEnemyPortraits[encounter.enemyId]
    val enemyPortrait = remember(encounter.id, encounter.enemyId) {
        dedicatedPortrait ?: StoryRepository.combatNode(encounter.id)?.illustrationId
            ?: "black_door_beneath_the_tree"
    }
    val enemyPortraitAlignment = remember(encounter.enemyId) {
        if (dedicatedPortrait != null) Alignment.Center else enemyPortraitAlignment(encounter.enemyId)
    }
    val equipment = remember(playerState.inventory) { ItemCatalog.resolve(playerState.inventory) }
    val equipmentDamageReduction = equipment.sumOf { it.combatEffect?.damageReduction ?: 0 }
    val equipmentAttackBonus = equipment.sumOf { it.combatEffect?.attackBonus ?: 0 }

    val engine = remember(encounter.id) {
        CombatEngine(
            enemy = enemy,
            startingPlayerHealth = playerState.health,
            playerMaxHealth = playerState.maxHealth,
            playerCourage = playerState.courage,
            availableDraughts = playerState.inventory.count { it == HEALING_DRAUGHT_ID },
            availableGreaterDraughts = playerState.inventory.count { it == GREATER_HEALING_DRAUGHT_ID },
            // Purchased/found gear should feel materially useful, not merely shave a point or two
            // off attacks that can otherwise land in the teens and twenties.
            damageReduction = equipmentDamageReduction * 2 + playerState.levelDamageReduction(),
            attackBonus = equipmentAttackBonus * 2 + playerState.levelAttackBonus()
        )
    }

    Column(modifier = modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⚔ ${enemy.name.uppercase()}", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CombatantCard(
                name = "KAELEN",
                illustrationId = KAELEN_COMBAT_PORTRAIT,
                health = engine.playerHealth.coerceAtLeast(0),
                maxHealth = playerState.maxHealth,
                modifier = Modifier.weight(1f)
            )
            CombatantCard(
                name = enemy.name.uppercase(),
                illustrationId = enemyPortrait,
                health = engine.enemyHealth,
                maxHealth = enemy.maxHealth,
                portraitAlignment = enemyPortraitAlignment,
                modifier = Modifier.weight(1f)
            )
        }

        Text(enemy.description, style = MaterialTheme.typography.bodyMedium, color = JailerColors.TextPrimary)

        val latest = engine.log.lastOrNull()
        if (latest != null) {
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Text("LATEST", style = MaterialTheme.typography.labelSmall, color = JailerColors.Gold)
                Text(latest, style = MaterialTheme.typography.bodyLarge)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(JailerColors.Panel, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text("COMBAT LOG", style = MaterialTheme.typography.labelSmall, color = JailerColors.Gold)
                Spacer(Modifier.height(4.dp))
            }
            items(engine.log.asReversed()) { line ->
                Text("• $line", style = MaterialTheme.typography.bodyMedium)
            }
        }

        val currentOutcome = engine.outcome
        if (currentOutcome == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { engine.attack() }) { Text("ATTACK") }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = { engine.defend() }) { Text("DEFEND") }
            }
            if (engine.remainingDraughts > 0) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { engine.useDraught() }) {
                    Text("HEALING DRAUGHT +25 HP (${engine.remainingDraughts})")
                }
            }
            if (engine.remainingGreaterDraughts > 0) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { engine.useGreaterDraught() }) {
                    Text("GREATER DRAUGHT +50 HP (${engine.remainingGreaterDraughts})")
                }
            }
        } else {
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (currentOutcome.victory) "VICTORY" else "YOU SURVIVE, BATTERED",
                    style = MaterialTheme.typography.titleMedium,
                    color = JailerColors.Gold
                )
                if (currentOutcome.victory) {
                    Text(
                        "+${encounter.goldReward} gold  •  +${encounter.xpReward} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JailerColors.TextPrimary
                    )
                } else {
                    Text(
                        "The story continues. You recover enough to carry on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JailerColors.TextPrimary
                    )
                }
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { onResolved(currentOutcome) }) {
                Text("CONTINUE")
            }
        }
    }
}

/**
 * A few of the finished fight illustrations are wide compositions where Kaelen/Voss sit near the
 * centre and the actual foe is pushed to one side. Bias only those known cases so the combat card
 * reads as an enemy portrait rather than a miniature replay of the whole scene. Everything else
 * stays centred until a real phone test says otherwise.
 */
private fun enemyPortraitAlignment(enemyId: String): Alignment = when (enemyId) {
    "cave_lurker",
    "seal_wraith",
    "unbound_horror",
    "loyalist_enforcer",
    "sanctum_sentinel" -> Alignment.CenterEnd

    "cinder_adept" -> Alignment.CenterStart

    else -> Alignment.Center
}

@Composable
private fun CombatantCard(
    name: String,
    illustrationId: String,
    health: Int,
    maxHealth: Int,
    portraitAlignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier
) {
    val progress = (health.toFloat() / maxHealth.coerceAtLeast(1)).coerceIn(0f, 1f)
    val lowHealth = progress <= LOW_HEALTH_FRACTION
    val healthColor = if (lowHealth) MaterialTheme.colorScheme.error else JailerColors.Gold

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SceneIllustration(
            illustrationId = illustrationId,
            modifier = Modifier.fillMaxWidth().height(128.dp),
            imageAlignment = portraitAlignment
        )
        Text(name, style = MaterialTheme.typography.labelMedium, color = JailerColors.Gold)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = healthColor
        )
        Text(
            "$health / $maxHealth HP",
            style = MaterialTheme.typography.bodySmall,
            color = if (lowHealth) healthColor else JailerColors.TextPrimary
        )
    }
}
