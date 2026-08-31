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
        ),
        Enemy(
            id = "sanctum_sentinel",
            name = "Sanctum Sentinel",
            maxHealth = 130,
            minAttack = 13,
            maxAttack = 20,
            description = "Armor quarried from the same black stone as the Sanctum's walls, moving like something built rather than born."
        ),
        Enemy(
            id = "cinder_castellan",
            name = "Castellan Ordrun",
            maxHealth = 145,
            minAttack = 14,
            maxAttack = 21,
            description = "Ilsevet's own hand, sent for exactly this. Twin blades, and nothing left to prove by holding back."
        ),
        Enemy(
            id = "ilsevets_vanguard_captain",
            name = "Ilsevet's Vanguard Captain",
            maxHealth = 155,
            minAttack = 15,
            maxAttack = 22,
            description = "Armored head to foot in cinder black-grey, and utterly untroubled by the idea that Ilsevet herself is watching this fight rather than fighting it."
        ),
        Enemy(
            id = "cinder_extraction_leader",
            name = "Cinder Extraction Team Leader",
            maxHealth = 165,
            minAttack = 16,
            maxAttack = 23,
            description = "Ilsevet's people have stopped pretending diplomacy is anything but a formality reserved for jailers she'd rather not fight twice."
        ),
        Enemy(
            id = "the_unfinished",
            name = "The Unfinished",
            maxHealth = 175,
            minAttack = 17,
            maxAttack = 24,
            description = "It doesn't have a shape so much as an argument about what shape it should have — pieces of six different wards fighting over one body that was never meant to hold all of them at once."
        ),
        Enemy(
            id = "the_memory_itself",
            name = "The Memory Itself",
            maxHealth = 190,
            minAttack = 18,
            maxAttack = 25,
            description = "Not flesh, not quite ghost — the shape of a moment three centuries old, defending itself the only way memory knows how."
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

private val sanctumSentinelEncounter = CombatEncounter(
    id = "sanctum_sentinel_encounter",
    enemyId = "sanctum_sentinel",
    victoryNodeId = "the_price_of_escape",
    xpReward = 190,
    goldReward = 50,
    unlockTrophy = "Out of the Ash"
)

private val rightHandEncounter = CombatEncounter(
    id = "right_hand_encounter",
    enemyId = "cinder_castellan",
    victoryNodeId = "what_the_castellan_says",
    xpReward = 220,
    goldReward = 60,
    unlockTrophy = "Ilsevet's Own Hand"
)

private val stonebeardSiegeEncounter = CombatEncounter(
    id = "stonebeard_siege_encounter",
    enemyId = "ilsevets_vanguard_captain",
    victoryNodeId = "halvard_falls",
    xpReward = 260,
    goldReward = 70,
    unlockTrophy = "What Was Lost"
)

private val fenmoorExtractionEncounter = CombatEncounter(
    id = "fenmoor_extraction_encounter",
    enemyId = "cinder_extraction_leader",
    victoryNodeId = "what_thessaly_decides",
    xpReward = 290,
    goldReward = 75,
    unlockTrophy = "The Marsh Holds"
)

private val unfinishedThingEncounter = CombatEncounter(
    id = "unfinished_thing_encounter",
    enemyId = "the_unfinished",
    victoryNodeId = "what_kaelen_does_with_her",
    xpReward = 330,
    goldReward = 85,
    unlockTrophy = "What Four Settings Do"
)

private val memoryConfrontationEncounter = CombatEncounter(
    id = "memory_confrontation_encounter",
    enemyId = "the_memory_itself",
    victoryNodeId = "what_kaelen_remembers",
    xpReward = 370,
    goldReward = 95,
    unlockTrophy = "What the Stone Remembers"
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
            loyalistAmbushEncounter,
            sanctumSentinelEncounter,
            rightHandEncounter,
            stonebeardSiegeEncounter,
            fenmoorExtractionEncounter,
            unfinishedThingEncounter,
            memoryConfrontationEncounter
        ).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
