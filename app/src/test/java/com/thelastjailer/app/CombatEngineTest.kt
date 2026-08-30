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

class CombatEngineTest {

    @Test
    fun `attack deals random damage and the enemy retaliates`() {
        val seed = 123L
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 100,
            playerMaxHealth = 100,
            playerCourage = 4,
            availableDraughts = 0,
            random = Random(seed)
        )
        // Same call order as CombatEngine.attack(): player roll, then the enemy's retaliation roll.
        val expected = Random(seed)
        val playerDamage = expected.nextInt(8, 15) + (4 / 2)
        val enemyDamage = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.attack()

        assertEquals(enemy.maxHealth - playerDamage, engine.enemyHealth)
        assertEquals(100 - enemyDamage, engine.playerHealth)
        assertNull(engine.outcome)
        assertTrue(engine.log.any { it.contains("You strike") })
        assertTrue(engine.log.any { it.contains("hits you for") })
    }

    @Test
    fun `defend halves the enemy's incoming damage and deals none`() {
        val seed = 55L
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 100,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 0,
            random = Random(seed)
        )
        val expected = Random(seed)
        val rawEnemyDamage = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.defend()

        assertEquals(100 - (rawEnemyDamage / 2), engine.playerHealth)
        assertEquals(enemy.maxHealth, engine.enemyHealth)
    }

    @Test
    fun `draught heals the player, consumes one item, then the enemy still attacks`() {
        val seed = 7L
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 50,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 1,
            random = Random(seed)
        )
        assertEquals(1, engine.remainingDraughts)
        val expected = Random(seed)
        val enemyDamage = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.useDraught()

        assertEquals(0, engine.remainingDraughts)
        assertEquals(listOf("healing_draught"), engine.consumedItems)
        assertEquals((50 + 25) - enemyDamage, engine.playerHealth)
    }

    @Test
    fun `draught healing is capped at max health`() {
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 90,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 1,
            random = Random(9)
        )
        val expected = Random(9)
        val enemyDamage = expected.nextInt(enemy.minAttack, enemy.maxAttack + 1)

        engine.useDraught()

        assertEquals(100 - enemyDamage, engine.playerHealth)
    }

    @Test
    fun `healing more than the damage taken reports a negative damageTaken`() {
        // Player's own attack always deals at least 8, so this 5-health enemy always dies to it —
        // guaranteeing victory regardless of the random roll, with no further enemy retaliation.
        val seed = 7L
        val weakEnemy = testEnemy(maxHealth = 5, minAttack = 4, maxAttack = 9)
        val engine = CombatEngine(
            enemy = weakEnemy,
            startingPlayerHealth = 50,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 1,
            random = Random(seed)
        )
        val expected = Random(seed)
        val enemyDamageDuringDraughtRound = expected.nextInt(weakEnemy.minAttack, weakEnemy.maxAttack + 1)

        engine.useDraught()
        engine.attack()

        val outcome = engine.outcome
        assertNotNull(outcome)
        assertTrue(outcome!!.victory)
        val expectedHealth = 50 + 25 - enemyDamageDuringDraughtRound
        assertEquals(expectedHealth, engine.playerHealth)
        // Ended the fight above the starting health, so damageTaken must be negative: GameState
        // .resolveCombat subtracts it, and subtracting a negative restores the net healing.
        assertEquals(50 - expectedHealth, outcome.damageTaken)
        assertTrue(outcome.damageTaken < 0)
    }

    @Test
    fun `using a draught with none available is a no-op`() {
        val enemy = testEnemy()
        val engine = CombatEngine(
            enemy = enemy,
            startingPlayerHealth = 50,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 0,
            random = Random(1)
        )
        val logSizeBefore = engine.log.size

        engine.useDraught()

        assertEquals(logSizeBefore, engine.log.size)
        assertEquals(50, engine.playerHealth)
        assertTrue(engine.consumedItems.isEmpty())
    }

    @Test
    fun `a killing blow ends combat in victory without the enemy striking back`() {
        // minAttack for the player's own hit is 8, so any enemy with maxHealth under that dies in one blow.
        val weakEnemy = testEnemy(maxHealth = 5, minAttack = 1, maxAttack = 3)
        val engine = CombatEngine(
            enemy = weakEnemy,
            startingPlayerHealth = 100,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 0,
            random = Random(1)
        )

        engine.attack()

        val outcome = engine.outcome
        assertNotNull(outcome)
        assertTrue(outcome!!.victory)
        assertEquals(0, outcome.damageTaken)
        assertEquals(100, engine.playerHealth)
        assertTrue(engine.log.any { it.contains("falls") })
    }

    @Test
    fun `losing all health ends combat as a non-fatal defeat`() {
        // minAttack 5 halved is still at least 2, which always exceeds startingPlayerHealth of 1.
        val bruteEnemy = testEnemy(maxHealth = 100, minAttack = 5, maxAttack = 9)
        val engine = CombatEngine(
            enemy = bruteEnemy,
            startingPlayerHealth = 1,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 0,
            random = Random(2)
        )

        engine.defend()

        val outcome = engine.outcome
        assertNotNull(outcome)
        assertFalse(outcome!!.victory)
        assertEquals(0, engine.playerHealth)
        assertEquals(1, outcome.damageTaken)
        assertTrue(engine.log.any { it.contains("collapse") })
    }

    @Test
    fun `no actions change state once combat has resolved`() {
        val weakEnemy = testEnemy(maxHealth = 5, minAttack = 1, maxAttack = 2)
        val engine = CombatEngine(
            enemy = weakEnemy,
            startingPlayerHealth = 100,
            playerMaxHealth = 100,
            playerCourage = 0,
            availableDraughts = 1,
            random = Random(3)
        )

        engine.attack()
        assertNotNull(engine.outcome)
        val logSizeAfterVictory = engine.log.size
        val healthAfterVictory = engine.playerHealth

        engine.attack()
        engine.defend()
        engine.useDraught()

        assertEquals(logSizeAfterVictory, engine.log.size)
        assertEquals(healthAfterVictory, engine.playerHealth)
    }
}
