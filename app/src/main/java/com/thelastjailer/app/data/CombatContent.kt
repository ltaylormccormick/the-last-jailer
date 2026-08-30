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
        ),
        Enemy(
            id = "unbound_horror",
            name = "The Unbound",
            maxHealth = 110,
            minAttack = 11,
            maxAttack = 18,
            description = "Nothing about it is patient anymore. It got out once already, and it isn't interested in waiting for a second chance."
        ),
        Enemy(
            id = "loyalist_enforcer",
            name = "Cinder Loyalist Enforcer",
            maxHealth = 120,
            minAttack = 12,
            maxAttack = 19,
            description = "Marked twice over — Ilsevet's cinder brand, and Voss's name crossed through beneath it. Sent to clean up a problem, not negotiate with one."
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

private val unboundEncounter = CombatEncounter(
    id = "unbound_encounter",
    enemyId = "unbound_horror",
    victoryNodeId = "what_must_be_decided",
    xpReward = 150,
    goldReward = 40,
    unlockTrophy = "What Got Out"
)

private val loyalistAmbushEncounter = CombatEncounter(
    id = "loyalist_ambush_encounter",
    enemyId = "loyalist_enforcer",
    victoryNodeId = "after_the_ambush",
    xpReward = 170,
    goldReward = 45,
    unlockTrophy = "An Uneasy Alliance"
)

/** Every scripted fight in the game, keyed by [CombatEncounter.id]. */
object CombatRepository {
    private val encounters: Map<String, CombatEncounter> =
        listOf(
            firstBloodEncounter,
            sealBreakerEncounter,
            siegeEncounter,
            cinderEnvoyEncounter,
            unboundEncounter,
            loyalistAmbushEncounter
        ).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
