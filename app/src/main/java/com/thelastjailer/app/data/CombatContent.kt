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
        ),
        Enemy(
            id = "cinder_reprisal_leader",
            name = "Cinder Reprisal Squad Leader",
            maxHealth = 205,
            minAttack = 19,
            maxAttack = 26,
            description = "Sent for Voss specifically — a debt Ilsevet apparently considers overdue, and entirely separate from anything to do with the gates."
        ),
        Enemy(
            id = "cinder_reliquary_thief",
            name = "Cinder Reliquary Thief",
            maxHealth = 220,
            minAttack = 20,
            maxAttack = 27,
            description = "Moves like something that was never meant to be caught, sent for exactly the kind of theft a siege could never manage."
        ),
        Enemy(
            id = "sanctum_construct",
            name = "The Chamber's Ward",
            maxHealth = 235,
            minAttack = 21,
            maxAttack = 28,
            description = "Assembled the same desperate way the frame itself was — pieces that were never meant to share a single purpose, forced to anyway."
        ),
        Enemy(
            id = "ilsevet_the_cinder_marshal",
            name = "Ilsevet, the Cinder Marshal",
            maxHealth = 260,
            minAttack = 22,
            maxAttack = 30,
            description = "No garrison behind her this time, no frame to finish, nothing left to spend but herself. She fights like someone who has already decided this is the last version of this argument she intends to have."
        ),
        Enemy(
            id = "the_ghostwriter",
            name = "The Ghostwriter",
            maxHealth = 280,
            minAttack = 23,
            maxAttack = 31,
            description = "No clean shape, just pressure and intent — something that finished another man's work rather than write its own, and clearly intends to keep doing exactly that."
        ),
        Enemy(
            id = "the_patient_voice",
            name = "The Patient Voice",
            maxHealth = 300,
            minAttack = 24,
            maxAttack = 32,
            description = "Gentle rather than violent, which somehow makes it harder to refuse — every exchange feels like closing a door on an outstretched hand rather than parrying a blade."
        ),
        Enemy(
            id = "cinder_straggler_captain",
            name = "Cinder Straggler Captain",
            maxHealth = 310,
            minAttack = 25,
            maxAttack = 33,
            description = "Leaderless doesn't mean harmless. Weeks with nothing to do but get very good at defending a gate they don't fully understand."
        ),
        Enemy(
            id = "greymoor_ward_wraith",
            name = "Greymoor Ward-Wraith",
            maxHealth = 325,
            minAttack = 26,
            maxAttack = 34,
            description = "Grief given just enough shape to defend what's left of a failing ward. It isn't trying to kill him. It's trying to make sure nothing else gets taken."
        ),
        Enemy(
            id = "the_answering_door",
            name = "The Answering Door",
            maxHealth = 340,
            minAttack = 27,
            maxAttack = 35,
            description = "A threshold given just enough shape to defend what it's climbing out of. The first foothold the whole has managed to take with a body of its own, however small."
        ),
        Enemy(
            id = "the_unremembering",
            name = "The Unremembering",
            maxHealth = 355,
            minAttack = 28,
            maxAttack = 36,
            description = "Not a guardian defending the Sundering Ground, but the ground's own forgetting given shape — three centuries of insisting, with everything it has, that there was never anything here at all."
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

private val reprisalSquadEncounter = CombatEncounter(
    id = "reprisal_squad_encounter",
    enemyId = "cinder_reprisal_leader",
    victoryNodeId = "after_the_reprisal",
    xpReward = 410,
    goldReward = 105,
    unlockTrophy = "Not Today"
)

private val reliquaryThiefEncounter = CombatEncounter(
    id = "reliquary_thief_encounter",
    enemyId = "cinder_reliquary_thief",
    victoryNodeId = "what_was_taken",
    xpReward = 450,
    goldReward = 115,
    unlockTrophy = "Nearly Enough"
)

private val chamberGuardianEncounter = CombatEncounter(
    id = "chamber_guardian_encounter",
    enemyId = "sanctum_construct",
    victoryNodeId = "what_must_be_broken",
    xpReward = 500,
    goldReward = 125,
    unlockTrophy = "What Almost Finished"
)

private val ilsevetDuelEncounter = CombatEncounter(
    id = "ilsevet_duel_encounter",
    enemyId = "ilsevet_the_cinder_marshal",
    victoryNodeId = "what_is_left_of_her",
    xpReward = 600,
    goldReward = 150,
    unlockTrophy = "No More Between Us"
)

private val ghostwriterEncounter = CombatEncounter(
    id = "ghostwriter_encounter",
    enemyId = "the_ghostwriter",
    victoryNodeId = "the_last_page",
    xpReward = 650,
    goldReward = 160,
    unlockTrophy = "Whoever Finished It"
)

private val patientVoiceEncounter = CombatEncounter(
    id = "patient_voice_encounter",
    enemyId = "the_patient_voice",
    victoryNodeId = "what_it_costs_to_refuse",
    xpReward = 700,
    goldReward = 170,
    unlockTrophy = "Refused Gently"
)

private val emberlowStragglersEncounter = CombatEncounter(
    id = "emberlow_stragglers_encounter",
    enemyId = "cinder_straggler_captain",
    victoryNodeId = "reaching_emberlow",
    xpReward = 750,
    goldReward = 180,
    unlockTrophy = "Leaderless, Not Harmless"
)

private val greymoorUnravelingEncounter = CombatEncounter(
    id = "greymoor_unraveling_encounter",
    enemyId = "greymoor_ward_wraith",
    victoryNodeId = "reaching_greymoor",
    xpReward = 800,
    goldReward = 190,
    unlockTrophy = "The Second Silence"
)

private val duskmereThresholdEncounter = CombatEncounter(
    id = "duskmere_threshold_encounter",
    enemyId = "the_answering_door",
    victoryNodeId = "what_yielding_cost",
    xpReward = 850,
    goldReward = 200,
    unlockTrophy = "When Duskmere Answered"
)

private val sunderingGroundEncounter = CombatEncounter(
    id = "sundering_ground_encounter",
    enemyId = "the_unremembering",
    victoryNodeId = "what_kaelen_offers_the_forgotten",
    xpReward = 900,
    goldReward = 210,
    unlockTrophy = "Where It Began"
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
            memoryConfrontationEncounter,
            reprisalSquadEncounter,
            reliquaryThiefEncounter,
            chamberGuardianEncounter,
            ilsevetDuelEncounter,
            ghostwriterEncounter,
            patientVoiceEncounter,
            emberlowStragglersEncounter,
            greymoorUnravelingEncounter,
            duskmereThresholdEncounter,
            sunderingGroundEncounter
        ).associateBy { it.id }

    fun encounter(id: String): CombatEncounter = encounters.getValue(id)
}
