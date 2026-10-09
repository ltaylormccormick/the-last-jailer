package com.thelastjailer.app.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
    "ashen_vanguard" to "ashen_vanguard_combat",
    "cinder_adept" to "cinder_adept_combat",
    "unbound_horror" to "unbound_horror_combat",
    "loyalist_enforcer" to "loyalist_enforcer_combat",
    "sanctum_sentinel" to "sanctum_sentinel_combat",
    "cinder_castellan" to "cinder_castellan_combat",
    "ilsevets_vanguard_captain" to "ilsevets_vanguard_captain_combat",
    "cinder_extraction_leader" to "cinder_extraction_leader_combat",
    "the_unfinished" to "the_unfinished_combat",
    "the_memory_itself" to "the_memory_itself_combat",
    "cinder_reprisal_leader" to "cinder_reprisal_leader_combat",
    "cinder_reliquary_thief" to "cinder_reliquary_thief_combat",
    "sanctum_construct" to "sanctum_construct_combat",
    "ilsevet_the_cinder_marshal" to "ilsevet_the_cinder_marshal_combat",
    "the_ghostwriter" to "the_ghostwriter_combat",
    "the_patient_voice" to "the_patient_voice_combat",
    "cinder_straggler_captain" to "cinder_straggler_captain_combat",
    "greymoor_ward_wraith" to "greymoor_ward_wraith_combat",
    "the_answering_door" to "the_answering_door_combat",
    "the_unremembering" to "the_unremembering_combat",
    "the_reclamation" to "the_reclamation_combat",
    "vigil_captain_of_wraithspire" to "vigil_captain_of_wraithspire_combat",
    "the_whole_undisguised" to "the_whole_undisguised_combat",
    "the_whole_at_full_reach" to "the_whole_at_full_reach_combat"
)

/**
 * Story-first turn-based combat. Kaelen uses a dedicated combat portrait; all 26 enemies use dedicated
 * enemy portraits. The full log remains available underneath but
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

    var actionLabel by remember(encounter.id) { mutableStateOf("Choose your action") }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("⚔ ${enemy.name.uppercase()}", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CombatantCard(
                name = "KAELEN",
                illustrationId = KAELEN_COMBAT_PORTRAIT,
                health = engine.playerHealth.coerceAtLeast(0),
                maxHealth = playerState.maxHealth,
                showCondition = true,
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

        Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = JailerColors.Gold)
        val currentOutcome = engine.outcome
        if (currentOutcome == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { actionLabel = "ATTACK · You strike"; engine.attack() }) { Text("ATTACK") }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = { actionLabel = "DEFEND · You brace"; engine.defend() }) { Text("DEFEND") }
            }
            if (engine.remainingDraughts > 0) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { actionLabel = "HEAL · Draught used"; engine.useDraught() }) {
                    Text("HEALING DRAUGHT +25 HP (${engine.remainingDraughts})")
                }
            }
            if (engine.remainingGreaterDraughts > 0) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { actionLabel = "HEAL · Greater draught used"; engine.useGreaterDraught() }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 180.dp)
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
    showCondition: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = (health.toFloat() / maxHealth.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, tween(250), label = "health")
    val impact = remember { Animatable(0f) }
    var previousHealth by remember { mutableStateOf(health) }
    LaunchedEffect(health) {
        val damaged = health < previousHealth
        previousHealth = health
        if (damaged) {
            impact.snapTo(1f)
            impact.animateTo(0f, tween(280))
        }
    }
    val condition = when {
        progress <= LOW_HEALTH_FRACTION -> "CRITICAL"
        progress <= .5f -> "WOUNDED"
        else -> "NORMAL"
    }
    val conditionTint = when (condition) {
        "CRITICAL" -> JailerColors.HealthRed.copy(alpha = .25f)
        "WOUNDED" -> JailerColors.GoldSoft.copy(alpha = .15f)
        else -> Color.Transparent
    }
    val lowHealth = progress <= LOW_HEALTH_FRACTION
    val healthColor = if (lowHealth) MaterialTheme.colorScheme.error else JailerColors.Gold

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(128.dp).graphicsLayer {
            translationX = impact.value * 5.dp.toPx()
        }) {
            SceneIllustration(
                illustrationId = illustrationId,
                modifier = Modifier.matchParentSize(),
                imageAlignment = portraitAlignment
            )
            if (showCondition) {
                Box(Modifier.matchParentSize().background(conditionTint))
                Text(condition, modifier = Modifier.align(Alignment.BottomStart)
                    .background(JailerColors.Panel.copy(alpha = .9f)).padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lowHealth) JailerColors.HealthRed else JailerColors.TextPrimary)
            }
            Box(Modifier.matchParentSize().background(JailerColors.HealthRed.copy(alpha = impact.value * .18f)))
        }
        Text(name, style = MaterialTheme.typography.labelMedium, color = JailerColors.Gold)
        LinearProgressIndicator(
            progress = { animatedProgress },
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
