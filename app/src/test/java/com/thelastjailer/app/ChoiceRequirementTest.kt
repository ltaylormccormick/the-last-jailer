package com.thelastjailer.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoiceRequirementTest {

    @Test
    fun `a requirement with no conditions is satisfied by any state`() {
        val requirement = ChoiceRequirement()

        assertTrue(requirement.isSatisfiedBy(GameState()))
    }

    @Test
    fun `requiredFlags must all be present`() {
        val requirement = ChoiceRequirement(requiredFlags = setOf("drew_sword", "kept_the_token"))

        assertFalse(requirement.isSatisfiedBy(GameState(flags = setOf("drew_sword"))))
        assertTrue(requirement.isSatisfiedBy(GameState(flags = setOf("drew_sword", "kept_the_token"))))
    }

    @Test
    fun `extra flags beyond what's required don't fail the check`() {
        val requirement = ChoiceRequirement(requiredFlags = setOf("drew_sword"))

        assertTrue(requirement.isSatisfiedBy(GameState(flags = setOf("drew_sword", "buried_the_past"))))
    }

    @Test
    fun `any forbiddenFlag present fails the check even if requiredFlags are met`() {
        val requirement = ChoiceRequirement(
            requiredFlags = setOf("drew_sword"),
            forbiddenFlags = setOf("sheathed_sword")
        )

        assertFalse(requirement.isSatisfiedBy(GameState(flags = setOf("drew_sword", "sheathed_sword"))))
        assertTrue(requirement.isSatisfiedBy(GameState(flags = setOf("drew_sword"))))
    }

    @Test
    fun `minStats requires every listed stat to meet its threshold`() {
        val requirement = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5, StatType.HONOUR to 3))

        assertFalse(requirement.isSatisfiedBy(GameState(courage = 5, honour = 2)))
        assertTrue(requirement.isSatisfiedBy(GameState(courage = 5, honour = 3)))
    }

    @Test
    fun `minStats is satisfied when the stat exceeds the threshold`() {
        val requirement = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5))

        assertTrue(requirement.isSatisfiedBy(GameState(courage = 20)))
    }

    @Test
    fun `flags and minStats requirements both have to pass`() {
        val requirement = ChoiceRequirement(
            requiredFlags = setOf("accepted_dark_aid"),
            minStats = mapOf(StatType.COURAGE to 10)
        )

        assertFalse(requirement.isSatisfiedBy(GameState(courage = 10)))
        assertFalse(requirement.isSatisfiedBy(GameState(courage = 5, flags = setOf("accepted_dark_aid"))))
        assertTrue(requirement.isSatisfiedBy(GameState(courage = 10, flags = setOf("accepted_dark_aid"))))
    }
}
