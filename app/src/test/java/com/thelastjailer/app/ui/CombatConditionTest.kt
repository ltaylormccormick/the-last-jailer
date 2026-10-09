package com.thelastjailer.app.ui
import org.junit.Assert.assertEquals
import org.junit.Test
class CombatConditionTest {
    @Test fun thresholdsAndHealingUseOneMapping() {
        val health = listOf(100, 51, 50, 31, 30, 0, 30, 31, 50, 51, 100)
        val expected = listOf("NORMAL", "NORMAL", "WOUNDED", "WOUNDED", "CRITICAL", "CRITICAL", "CRITICAL", "WOUNDED", "WOUNDED", "NORMAL", "NORMAL")
        assertEquals(expected, health.map { CombatCondition.fromHealth(it, 100).name })
        assertEquals(CombatCondition.CRITICAL, CombatCondition.fromHealth(60, 200))
    }
}
