package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStatePurchaseItemTest {

    @Test
    fun `purchasing an item spends gold and adds it to the inventory`() {
        val state = GameState(gold = 100)

        val result = state.purchaseItem("leather_armor", 70)

        assertEquals(30, result.gold)
        assertEquals(listOf("leather_armor"), result.inventory)
    }

    @Test
    fun `purchasing with insufficient gold is a no-op`() {
        val state = GameState(gold = 20)

        val result = state.purchaseItem("proper_helmet", 160)

        assertEquals(20, result.gold)
        assertEquals(emptyList<String>(), result.inventory)
    }

    @Test
    fun `an item costing exactly the player's gold can still be bought`() {
        val state = GameState(gold = 40)

        val result = state.purchaseItem("healing_draught", 40)

        assertEquals(0, result.gold)
        assertEquals(listOf("healing_draught"), result.inventory)
    }

    @Test
    fun `repeatable items can be purchased more than once, stacking in the inventory`() {
        val state = GameState(gold = 100)

        val result = state.purchaseItem("healing_draught", 40).purchaseItem("healing_draught", 40)

        assertEquals(20, result.gold)
        assertEquals(listOf("healing_draught", "healing_draught"), result.inventory)
    }
}
