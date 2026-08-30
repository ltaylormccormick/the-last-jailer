package com.thelastjailer.app.data

import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.Enemy

object EnemyCatalog {
    private val enemies: Map<String, Enemy> = listOf(
        Enemy(
            id = "cave_lurker",
            name = "Cave Lurker",
            maxHealth = 40,
            minAttack = 4,
            maxAttack = 9,
            description = "Pale, many-limbed, and hungrier than anything that size should be."
        ),
        Enemy(
            id = "seal_wraith",
            name = "Seal-Bound Wraith",
            maxHealth = 65,
            minAttack = 7,
            maxAttack = 13,
            description = "A shape without real edges, pressed thin against the gate until it found a way through."
        )
    ).associateBy { it.id }

    fun get(id: String): Enemy = enemies.getValue(id)
}

private val firstBloodEncounter = CombatEncounter(
    id = "first_blood_encounter",
    enemyId = "cave_lurker",
    victoryNodeId = "chapter1_end",
    xpReward = 40,
    goldReward = 10,
    unlockTrophy = "First Blood"
)

private val sealBreakerEncounter = CombatEncounter(
    id = "seal_breaker_encounter",
    enemyId = "seal_wraith",
    victoryNodeId = "chapter2_end",
    xpReward = 70,
    goldReward = 20,
    unlockTrophy = "Seal Held"
)

/** Every scripted fight in the game, keyed by [CombatEncounter.id]. */
object CombatRepository {
    private val encounters: Map<String, CombatEncounter> =
        listOf(firstBloodEncounter, sealBreakerEncounter).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
