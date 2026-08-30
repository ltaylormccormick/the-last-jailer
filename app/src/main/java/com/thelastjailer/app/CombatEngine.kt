package com.thelastjailer.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

private const val HEALING_DRAUGHT_ID = "healing_draught"
private const val HEALING_DRAUGHT_AMOUNT = 25
private const val PLAYER_ATTACK_MIN = 8
private const val PLAYER_ATTACK_MAX_EXCLUSIVE = 15

/**
 * Round-by-round resolution for a [CombatEncounter], independent of any Compose UI so it can be
 * unit tested directly. [com.thelastjailer.app.ui.CombatScreen] holds one instance per encounter
 * (keyed by [CombatEncounter.id]) and renders its [State][androidx.compose.runtime.State]-backed
 * properties; every mutating call here triggers recomposition the same way a Composable's own
 * `mutableStateOf` would.
 *
 * Combat never kills the player outright — reaching 0 health ends the fight as a loss (see
 * [outcome]), it doesn't throw or leave the engine in an invalid state. Whether a loss floors the
 * run's actual health above 0 is decided by the caller (see [GameState.resolveCombat]).
 */
class CombatEngine(
    private val enemy: Enemy,
    startingPlayerHealth: Int,
    private val playerMaxHealth: Int,
    private val playerCourage: Int,
    private val availableDraughts: Int,
    private val random: Random = Random.Default
) {
    private val startingPlayerHealth = startingPlayerHealth

    var enemyHealth: Int by mutableStateOf(enemy.maxHealth)
        private set

    var playerHealth: Int by mutableStateOf(startingPlayerHealth)
        private set

    var consumedItems: List<String> by mutableStateOf(emptyList())
        private set

    var log: List<String> by mutableStateOf(listOf("The ${enemy.name} lunges out of the dark."))
        private set

    var outcome: CombatOutcome? by mutableStateOf(null)
        private set

    val remainingDraughts: Int
        get() = availableDraughts - consumedItems.count { it == HEALING_DRAUGHT_ID }

    fun attack() {
        if (outcome != null) return
        val dmg = random.nextInt(PLAYER_ATTACK_MIN, PLAYER_ATTACK_MAX_EXCLUSIVE) + (playerCourage / 2)
        enemyHealth = (enemyHealth - dmg).coerceAtLeast(0)
        val round = mutableListOf("You strike the ${enemy.name} for $dmg damage.")
        if (enemyHealth <= 0) {
            round += "The ${enemy.name} falls."
            finish(victory = true)
        } else {
            round += enemyStrikes(reduced = false)
            if (outcome != null) round += "You collapse — but you're still breathing."
        }
        log = log + round
    }

    fun defend() {
        if (outcome != null) return
        val round = mutableListOf("You brace for the next blow.")
        round += enemyStrikes(reduced = true)
        if (outcome != null) round += "You collapse — but you're still breathing."
        log = log + round
    }

    fun useDraught() {
        if (outcome != null || remainingDraughts <= 0) return
        consumedItems = consumedItems + HEALING_DRAUGHT_ID
        playerHealth = (playerHealth + HEALING_DRAUGHT_AMOUNT).coerceAtMost(playerMaxHealth)
        val round = mutableListOf("You drink a Healing Draught and feel restored.")
        round += enemyStrikes(reduced = false)
        if (outcome != null) round += "You collapse — but you're still breathing."
        log = log + round
    }

    /** Enemy strikes back; returns the log line and finalizes [outcome] on a knockout. */
    private fun enemyStrikes(reduced: Boolean): String {
        val raw = random.nextInt(enemy.minAttack, enemy.maxAttack + 1)
        val dmg = if (reduced) raw / 2 else raw
        playerHealth = (playerHealth - dmg).coerceAtLeast(0)
        if (playerHealth <= 0) finish(victory = false)
        return "The ${enemy.name} hits you for $dmg damage."
    }

    private fun finish(victory: Boolean) {
        outcome = CombatOutcome(
            victory = victory,
            // Can be negative when Healing Draughts more than offset damage taken during the
            // fight; GameState.resolveCombat subtracts this, so a negative value restores health.
            damageTaken = startingPlayerHealth - playerHealth,
            consumedItemIds = consumedItems
        )
    }
}
