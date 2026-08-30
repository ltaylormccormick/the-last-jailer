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
        ),
        Enemy(
            id = "ashen_vanguard",
            name = "Ashen Vanguard",
            maxHealth = 85,
            minAttack = 9,
            maxAttack = 15,
            description = "Ash-grey plate, unhurried, and utterly certain the door is coming down today."
        ),
        Enemy(
            id = "cinder_adept",
            name = "Cinder-Marked Adept",
            maxHealth = 95,
            minAttack = 10,
            maxAttack = 16,
            description = "Trained by the Order, marked by something the Order never sanctioned. Fights like a zealot with nothing left to prove."
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

private val siegeEncounter = CombatEncounter(
    id = "siege_encounter",
    enemyId = "ashen_vanguard",
    victoryNodeId = "what_the_prisoner_offers_again",
    xpReward = 100,
    goldReward = 30,
    unlockTrophy = "Line Held"
)

private val cinderEnvoyEncounter = CombatEncounter(
    id = "cinder_envoy_encounter",
    enemyId = "cinder_adept",
    victoryNodeId = "aftermath_of_betrayal",
    xpReward = 120,
    goldReward = 35,
    unlockTrophy = "The Marshal's Warning"
)

/** Every scripted fight in the game, keyed by [CombatEncounter.id]. */
object CombatRepository {
    private val encounters: Map<String, CombatEncounter> =
        listOf(firstBloodEncounter, sealBreakerEncounter, siegeEncounter, cinderEnvoyEncounter).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
