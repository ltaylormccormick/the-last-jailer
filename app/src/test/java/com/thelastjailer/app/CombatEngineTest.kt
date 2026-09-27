package com.thelastjailer.app

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testEnemy(
    id: String = "test_enemy",
    name: String = "Test Foe",
    maxHealth: Int = 100,
    minAttack: Int = 4,
    maxAttack: Int = 9
) = Enemy(id = id, name = name, maxHealth = maxHealth, minAttack = minAttack, maxAttack = maxAttack, description = "")

private fun storyScaled(raw: Int): Int = (raw * 4 + 4) / 5

class CombatEngineTest {

    @Test
    fun `attack damages enemy and retaliation uses story damage scale`() {
        val seed = 123L
        val enemy = testEnemy()
        val engine = CombatEngine(enemy, 100, 100, 4, 0, random = Random(seed))
        val expected = Random(seed)
        val playerDamage = expected.nextInt(8, 15) + 2
        val rawEnemyDamage = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.attack()

        assertEquals(enemy.maxHealth - playerDamage, engine.enemyHealth)
        assertEquals(100 - storyScaled(rawEnemyDamage), engine.playerHealth)
        assertNull(engine.outcome)
    }

    @Test
    fun `defend halves story scaled damage`() {
        val seed = 55L
        val enemy = testEnemy()
        val engine = CombatEngine(enemy, 100, 100, 0, 0, random = Random(seed))
        val expected = Random(seed)
        val raw = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.defend()

        assertEquals(100 - storyScaled(raw) / 2, engine.playerHealth)
        assertEquals(enemy.maxHealth, engine.enemyHealth)
    }

    @Test
    fun `damage reduction and courage stack with defend`() {
        val seed = 55L
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 100,
            playerMaxHealth = 100,
            playerCourage = 9,
            availableDraughts = 0,
            damageReduction = 2,
            random = Random(seed)
        )
        val expected = Random(seed)
        val raw = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)
        val damage = (storyScaled(raw) / 2 - 2 - 3).coerceAtLeast(0)

        engine.defend()

        assertEquals(100 - damage, engine.playerHealth)
    }

    @Test
    fun `attack bonus adds flat damage`() {
        val seed = 123L
        val enemy = testEnemy()
        val engine = CombatEngine(enemy, 100, 100, 4, 0, attackBonus = 6, random = Random(seed))
        val expected = Random(seed)
        val playerDamage = expected.nextInt(8, 15) + 2 + 6

        engine.attack()

        assertEquals(enemy.maxHealth - playerDamage, engine.enemyHealth)
    }

    @Test
    fun `regular draught heals then enemy attacks`() {
        val seed = 7L
        val enemy = testEnemy()
        val engine = CombatEngine(enemy, 50, 100, 0, 1, random = Random(seed))
        val expected = Random(seed)
        val raw = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.useDraught()

        assertEquals(0, engine.remainingDraughts)
        assertEquals(listOf("healing_draught"), engine.consumedItems)
        assertEquals(75 - storyScaled(raw), engine.playerHealth)
    }

    @Test
    fun `greater draught heals fifty then enemy attacks`() {
        val seed = 7L
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 30,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 0,
            availableGreaterDraughts = 1,
            random = Random(seed)
        )
        val expected = Random(seed)
        val raw = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.useGreaterDraught()

        assertEquals(0, engine.remainingGreaterDraughts)
        assertEquals(listOf("greater_healing_draught"), engine.consumedItems)
        assertEquals(80 - storyScaled(raw), engine.playerHealth)
    }

    @Test
    fun `killing blow wins before retaliation`() {
        val enemy = testEnemy(maxHealth = 5, minAttack = 1, maxAttack = 3)
        val engine = CombatEngine(enemy, 100, 100, 0, 0, random = Random(1))

        engine.attack()

        val outcome = engine.outcome
        assertNotNull(outcome)
        assertTrue(outcome!!.victory)
        assertEquals(100, engine.playerHealth)
        assertEquals(0, outcome.damageTaken)
    }

    @Test
    fun `zero health ends combat as non fatal defeat`() {
        val enemy = testEnemy(maxHealth = 100, minAttack = 10, maxAttack = 12)
        val engine = CombatEngine(enemy, 1, 100, 0, 0, random = Random(2))

        engine.defend()

        val outcome = engine.outcome
        assertNotNull(outcome)
        assertFalse(outcome!!.victory)
        assertEquals(0, engine.playerHealth)
        assertEquals(1, outcome.damageTaken)
    }

    @Test
    fun `actions stop after resolution`() {
        val enemy = testEnemy(maxHealth = 5, minAttack = 1, maxAttack = 2)
        val engine = CombatEngine(enemy, 100, 100, 0, 1, random = Random(3))

        engine.attack()
        val health = engine.playerHealth
        val logSize = engine.log.size
        engine.attack()
        engine.defend()
        engine.useDraught()

        assertEquals(health, engine.playerHealth)
        assertEquals(logSize, engine.log.size)
    }
}
