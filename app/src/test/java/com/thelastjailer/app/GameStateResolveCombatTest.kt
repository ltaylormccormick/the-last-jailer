package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val testEncounter = CombatEncounter(
    id = "test_encounter",
    enemyId = "test_enemy",
    victoryNodeId = "after_victory",
    defeatNodeId = "after_defeat",
    xpReward = 40,
    goldReward = 10,
    unlockTrophy = "Test Trophy"
)

class GameStateResolveCombatTest {

    @Test
    fun `victory applies rewards and post-fight recovery`() {
        val state = GameState(health = 100, maxHealth = 100, xp = 0, gold = 0)
        val outcome = CombatOutcome(victory = true, damageTaken = 15, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(100, result.health)
        assertEquals(40, result.xp)
        assertEquals(10, result.gold)
        assertTrue(result.trophies.contains("Test Trophy"))
        assertEquals("after_victory", result.sceneId)
    }

    @Test
    fun `defeat grants no reward and moves to the defeat node`() {
        val state = GameState(health = 100, maxHealth = 100, xp = 5, gold = 5)
        val outcome = CombatOutcome(victory = false, damageTaken = 30, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(70, result.health)
        assertEquals(5, result.xp)
        assertEquals(5, result.gold)
        assertTrue(result.trophies.isEmpty())
        assertEquals("after_defeat", result.sceneId)
    }

    @Test
    fun `defeat with no defeat node falls back to the victory node`() {
        val state = GameState(health = 100, maxHealth = 100)
        val encounterWithoutDefeatNode = testEncounter.copy(defeatNodeId = null)
        val outcome = CombatOutcome(victory = false, damageTaken = 10, consumedItemIds = emptyList())

        val result = state.resolveCombat(encounterWithoutDefeatNode, outcome)

        assertEquals("after_victory", result.sceneId)
    }

    @Test
    fun `a knockout recovers to forty five percent instead of leaving the next fight at one hp`() {
        val state = GameState(health = 20, maxHealth = 100)
        val outcome = CombatOutcome(victory = false, damageTaken = 999, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(45, result.health)
        assertTrue(result.health > 0)
    }

    @Test
    fun `defeat recovery floor scales with max health and rounds up`() {
        val state = GameState(health = 20, maxHealth = 109)
        val outcome = CombatOutcome(victory = false, damageTaken = 999, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(50, result.health)
    }

    @Test
    fun `narrow victory recovers to at least forty percent`() {
        val state = GameState(health = 20, maxHealth = 100)
        val outcome = CombatOutcome(victory = true, damageTaken = 19, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(40, result.health)
    }

    @Test
    fun `victory above the floor receives twenty percent recovery capped at max`() {
        val state = GameState(health = 40, maxHealth = 100)
        val outcome = CombatOutcome(victory = true, damageTaken = -20, consumedItemIds = emptyList())

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(80, result.health)
    }

    @Test
    fun `items used during the fight are removed from inventory`() {
        val state = GameState(health = 100, maxHealth = 100, inventory = listOf("healing_draught", "dwarven_token"))
        val outcome = CombatOutcome(victory = true, damageTaken = 0, consumedItemIds = listOf("healing_draught"))

        val result = state.resolveCombat(testEncounter, outcome)

        assertEquals(listOf("dwarven_token"), result.inventory)
    }

    @Test
    fun `a big enough xp reward levels the player up and raises maxHealth`() {
        val state = GameState(level = 1, xp = 90, xpToNextLevel = 100, maxHealth = 100)
        val bigReward = testEncounter.copy(xpReward = 40, goldReward = 0, unlockTrophy = null)
        val outcome = CombatOutcome(victory = true, damageTaken = 0, consumedItemIds = emptyList())

        val result = state.resolveCombat(bigReward, outcome)

        assertEquals(2, result.level)
        assertEquals(30, result.xp)
        assertEquals(200, result.xpToNextLevel)
        assertEquals(109, result.maxHealth)
        assertEquals(109, result.health)
    }

    @Test
    fun `an xp reward spanning multiple level-ups raises maxHealth once per level gained`() {
        val state = GameState(level = 1, xp = 0, xpToNextLevel = 100, maxHealth = 100)
        val hugeReward = testEncounter.copy(xpReward = 350, goldReward = 0, unlockTrophy = null)
        val outcome = CombatOutcome(victory = true, damageTaken = 0, consumedItemIds = emptyList())

        val result = state.resolveCombat(hugeReward, outcome)

        assertEquals(3, result.level)
        assertEquals(118, result.maxHealth)
        assertEquals(118, result.health)
    }
}
