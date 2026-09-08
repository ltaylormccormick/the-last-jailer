package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateLevelBonusTest {

    @Test
    fun `level 1 grants no combat bonus`() {
        val state = GameState(level = 1)

        assertEquals(0, state.levelAttackBonus())
        assertEquals(0, state.levelDamageReduction())
    }

    @Test
    fun `combat bonus grows with level`() {
        val state = GameState(level = 15)

        // (15 - 1) * 7 / 10 = 9 (integer division), (15 - 1) * 5 / 10 = 7
        assertEquals(9, state.levelAttackBonus())
        assertEquals(7, state.levelDamageReduction())
    }
}
