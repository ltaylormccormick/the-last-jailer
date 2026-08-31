package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateApplyChoiceTest {

    @Test
    fun `applying a choice moves to its next node`() {
        val state = GameState(sceneId = "fallen_knight")
        val choice = Choice(label = "Go", nextNodeId = "the_road_away")

        val result = state.applyChoice(choice)

        assertEquals("the_road_away", result.sceneId)
    }

    @Test
    fun `stat deltas from a choice are applied cumulatively`() {
        val state = GameState(courage = 1, honour = 0)
        val choice = Choice(
            label = "Stand firm",
            nextNodeId = "next",
            consequences = Consequences(statDeltas = mapOf(StatType.COURAGE to 2, StatType.HONOUR to 1))
        )

        val result = state.applyChoice(choice)

        assertEquals(3, result.courage)
        assertEquals(1, result.honour)
    }

    @Test
    fun `setFlags are added to any flags the state already has`() {
        val state = GameState(flags = setOf("kept_the_token"))
        val choice = Choice(
            label = "Announce yourself",
            nextNodeId = "next",
            consequences = Consequences(setFlags = setOf("announced_himself"))
        )

        val result = state.applyChoice(choice)

        assertEquals(setOf("kept_the_token", "announced_himself"), result.flags)
    }

    @Test
    fun `grantItemIds are appended to inventory, not replacing it`() {
        val state = GameState(inventory = listOf("broken_sword"))
        val choice = Choice(
            label = "Take the token",
            nextNodeId = "next",
            consequences = Consequences(grantItemIds = listOf("tarnished_guard_token"))
        )

        val result = state.applyChoice(choice)

        assertEquals(listOf("broken_sword", "tarnished_guard_token"), result.inventory)
    }

    @Test
    fun `a granted item id can appear more than once in inventory`() {
        val state = GameState(inventory = listOf("healing_draught"))
        val choice = Choice(
            label = "Take another",
            nextNodeId = "next",
            consequences = Consequences(grantItemIds = listOf("healing_draught"))
        )

        val result = state.applyChoice(choice)

        assertEquals(listOf("healing_draught", "healing_draught"), result.inventory)
    }

    @Test
    fun `unlockTrophy adds to existing trophies without dropping them`() {
        val state = GameState(trophies = setOf("First Blood"))
        val choice = Choice(
            label = "Finish it",
            nextNodeId = "next",
            consequences = Consequences(unlockTrophy = "Seal Held")
        )

        val result = state.applyChoice(choice)

        assertEquals(setOf("First Blood", "Seal Held"), result.trophies)
    }

    @Test
    fun `a choice with no unlockTrophy leaves existing trophies untouched`() {
        val state = GameState(trophies = setOf("First Blood"))
        val choice = Choice(label = "Move on", nextNodeId = "next")

        val result = state.applyChoice(choice)

        assertEquals(setOf("First Blood"), result.trophies)
    }

    @Test
    fun `a HEALTH stat delta from a choice is clamped to maxHealth`() {
        val state = GameState(health = 95, maxHealth = 100)
        val choice = Choice(
            label = "Drink deeply",
            nextNodeId = "next",
            consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to 50))
        )

        val result = state.applyChoice(choice)

        assertEquals(100, result.health)
    }

    @Test
    fun `a GOLD stat delta from a choice never drops gold below zero`() {
        val state = GameState(gold = 10)
        val choice = Choice(
            label = "Pay the toll",
            nextNodeId = "next",
            consequences = Consequences(statDeltas = mapOf(StatType.GOLD to -50))
        )

        val result = state.applyChoice(choice)

        assertEquals(0, result.gold)
    }

    @Test
    fun `an XP stat delta from a choice can level the player up the same as combat xp`() {
        val state = GameState(level = 1, xp = 90, xpToNextLevel = 100)
        val choice = Choice(
            label = "A quiet victory",
            nextNodeId = "next",
            consequences = Consequences(statDeltas = mapOf(StatType.XP to 25))
        )

        val result = state.applyChoice(choice)

        assertEquals(2, result.level)
        assertEquals(15, result.xp)
    }

    @Test
    fun `a default Consequences leaves stats, flags, inventory and trophies unchanged`() {
        val state = GameState(courage = 3, honour = 2, flags = setOf("a"), inventory = listOf("x"), trophies = setOf("y"))
        val choice = Choice(label = "Continue", nextNodeId = "next")

        val result = state.applyChoice(choice)

        assertEquals(3, result.courage)
        assertEquals(2, result.honour)
        assertEquals(setOf("a"), result.flags)
        assertEquals(listOf("x"), result.inventory)
        assertEquals(setOf("y"), result.trophies)
        assertTrue(result.sceneId == "next")
    }
}
