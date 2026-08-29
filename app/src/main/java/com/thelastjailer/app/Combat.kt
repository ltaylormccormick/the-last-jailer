package com.thelastjailer.app

/** A foe the player can fight in a [CombatEncounter]. */
data class Enemy(
    val id: String,
    val name: String,
    val maxHealth: Int,
    val minAttack: Int,
    val maxAttack: Int,
    val description: String
)

/**
 * A scripted fight triggered by a [StoryNode.combatEncounterId]. Combat is never fatal to the
 * story — a defeat still continues (via [defeatNodeId], defaulting to the same node as victory)
 * with no reward, rather than ending the run.
 */
data class CombatEncounter(
    val id: String,
    val enemyId: String,
    val victoryNodeId: String,
    val defeatNodeId: String? = null,
    val xpReward: Int = 0,
    val goldReward: Int = 0,
    val unlockTrophy: String? = null
)

/** The result of playing out a [CombatEncounter] in [com.thelastjailer.app.ui.CombatScreen]. */
data class CombatOutcome(
    val victory: Boolean,
    val damageTaken: Int,
    val consumedItemIds: List<String> = emptyList()
)
