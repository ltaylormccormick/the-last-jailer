package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveStoreTest {
    private lateinit var store: SaveStore

    @Before
    fun setUp() {
        store = SaveStore(FakeSharedPreferences())
    }

    @Test
    fun `load returns null for a slot that has never been saved`() {
        assertNull(store.load(1))
    }

    @Test
    fun `hasSave is false until the slot has been saved`() {
        assertFalse(store.hasSave(1))
        store.save(1, GameState(activeSlot = 1))
        assertTrue(store.hasSave(1))
    }

    @Test
    fun `currentActiveSlot is null before any save`() {
        assertNull(store.currentActiveSlot())
    }

    @Test
    fun `saving a slot round-trips the full game state`() {
        val original = GameState(
            activeSlot = 2,
            chapterId = "chapter_1",
            sceneId = "stonebeard_hold",
            courage = 3,
            honour = 2,
            health = 62,
            maxHealth = 100,
            gold = 47,
            level = 2,
            xp = 15,
            xpToNextLevel = 200,
            inventory = listOf("dwarven_token", "healing_draught", "healing_draught"),
            trophies = setOf("Friend of Stonebeard", "First Blood"),
            flags = setOf("opened_black_door", "helped_dwarf")
        )

        store.save(2, original)
        val loaded = store.load(2)

        assertEquals(original, loaded)
    }

    @Test
    fun `saving updates the active slot`() {
        store.save(3, GameState(activeSlot = 3))
        assertEquals(3, store.currentActiveSlot())
    }

    @Test
    fun `setActiveSlot changes the active slot without touching saved data`() {
        store.save(1, GameState(activeSlot = 1, gold = 99))

        store.setActiveSlot(1)

        assertEquals(1, store.currentActiveSlot())
        assertEquals(99, store.load(1)?.gold)
    }

    @Test
    fun `separate slots do not cross-contaminate`() {
        val slot1 = GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 25, inventory = listOf("dwarven_token"))
        val slot2 = GameState(activeSlot = 2, sceneId = "stonebeard_hold", gold = 60, inventory = listOf("healing_draught"))

        store.save(1, slot1)
        store.save(2, slot2)

        val loaded1 = store.load(1)
        val loaded2 = store.load(2)

        assertEquals(slot1, loaded1)
        assertEquals(slot2, loaded2)
        assertEquals(1, loaded1?.activeSlot)
        assertEquals(2, loaded2?.activeSlot)
    }

    @Test
    fun `loading a slot always reports that slot as active regardless of what was saved`() {
        // A GameState saved under one slot but later loaded from another should report the slot
        // it was actually loaded from, not whatever activeSlot it happened to carry when saved.
        store.save(5, GameState(activeSlot = 1))

        val loaded = store.load(5)

        assertEquals(5, loaded?.activeSlot)
    }
}
