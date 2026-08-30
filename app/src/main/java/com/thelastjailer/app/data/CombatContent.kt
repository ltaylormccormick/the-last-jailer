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

/** Every scripted fight in the game, keyed by [CombatEncounter.id]. */
object CombatRepository {
    private val encounters: Map<String, CombatEncounter> = listOf(firstBloodEncounter).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
