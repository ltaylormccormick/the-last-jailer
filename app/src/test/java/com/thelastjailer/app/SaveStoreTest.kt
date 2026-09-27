package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveStoreTest {
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: SaveStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = SaveStore(prefs)
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
            maxHealth = 109,
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
    fun `loading a slot saved by the pre-rework StringSet inventory format does not crash`() {
        // Before the foundation rework, inventory was stored as a StringSet under this same key.
        // Real Android's SharedPreferences throws ClassCastException reading that back as a
        // String, so a returning player's old save must not crash the app on load.
        prefs.edit()
            .putString("4.scene", "fallen_knight")
            .putStringSet("4.inventory", mutableSetOf("dwarven_token", "healing_draught"))
            .apply()

        val loaded = store.load(4)

        assertEquals(setOf("dwarven_token", "healing_draught"), loaded?.inventory?.toSet())
    }

    @Test
    fun `loading corrects a stale maxHealth left over from before level granted a health bonus`() {
        // A save written before level started granting +9 maxHealth per level (see
        // GameState.applyXpGain) would have persisted maxHealth = 100 even at a high level, since
        // only future level-ups added the bonus. Loading must recompute the level-derived floor
        // rather than trusting a stale stored value forever.
        prefs.edit()
            .putString("6.scene", "fallen_knight")
            .putInt("6.level", 4)
            .putInt("6.maxHealth", 100)
            .apply()

        val loaded = store.load(6)

        assertEquals(127, loaded?.maxHealth) // 100 + 9 * (4 - 1)
    }

    @Test
    fun `loading never lowers maxHealth below what was actually saved`() {
        prefs.edit()
            .putString("7.scene", "fallen_knight")
            .putInt("7.level", 1)
            .putInt("7.maxHealth", 150)
            .apply()

        val loaded = store.load(7)

        assertEquals(150, loaded?.maxHealth)
    }

    @Test
    fun `loading a slot always reports that slot as active regardless of what was saved`() {
        // A GameState saved under one slot but later loaded from another should report the slot
        // it was actually loaded from, not whatever activeSlot it happened to carry when saved.
        store.save(5, GameState(activeSlot = 1))

        val loaded = store.load(5)

        assertEquals(5, loaded?.activeSlot)
    }

    @Test
    fun `previous state round-trips the full game state`() {
        val previous = GameState(
            activeSlot = 2,
            chapterId = "chapter_12",
            sceneId = "before_choice",
            courage = 11,
            honour = 9,
            health = 37,
            maxHealth = 127,
            gold = 88,
            level = 4,
            xp = 45,
            xpToNextLevel = 400,
            inventory = listOf("healing_draught", "kestrels_locket"),
            trophies = setOf("First Blood"),
            flags = setOf("remember_this")
        )

        store.savePreviousState(2, previous)

        assertTrue(store.hasPreviousState(2))
        assertEquals(previous, store.loadPreviousState(2))
    }

    @Test
    fun `previous state survives recreating the save store`() {
        val previous = GameState(activeSlot = 1, sceneId = "before_choice", health = 41, gold = 73)
        store.savePreviousState(1, previous)

        val reopenedStore = SaveStore(prefs)

        assertTrue(reopenedStore.hasPreviousState(1))
        assertEquals(previous, reopenedStore.loadPreviousState(1))
    }

    @Test
    fun `restoring previous state rewinds the whole save and consumes the checkpoint`() {
        val before = GameState(
            activeSlot = 1,
            sceneId = "before_choice",
            courage = 4,
            honour = 3,
            health = 35,
            gold = 70,
            inventory = listOf("dwarven_token"),
            flags = setOf("old_flag")
        )
        val after = before.copy(
            sceneId = "after_choice",
            courage = 5,
            health = 50,
            gold = 30,
            inventory = before.inventory + "healing_draught",
            flags = before.flags + "new_flag"
        )
        store.savePreviousState(1, before)
        store.save(1, after)

        val restored = store.restorePreviousState(1)

        assertEquals(before, restored)
        assertEquals(before, store.load(1))
        assertFalse(store.hasPreviousState(1))
        assertNull(store.loadPreviousState(1))
    }

    @Test
    fun `previous checkpoints are isolated by slot`() {
        store.savePreviousState(1, GameState(activeSlot = 1, sceneId = "slot_one_previous"))
        store.savePreviousState(2, GameState(activeSlot = 2, sceneId = "slot_two_previous"))

        store.clearPreviousState(1)

        assertFalse(store.hasPreviousState(1))
        assertTrue(store.hasPreviousState(2))
        assertEquals("slot_two_previous", store.loadPreviousState(2)?.sceneId)
    }

    @Test
    fun `restart resets only the chosen slot and clears its previous checkpoint`() {
        store.save(1, GameState(activeSlot = 1, sceneId = "late_game", health = 12, gold = 999, level = 8))
        store.savePreviousState(1, GameState(activeSlot = 1, sceneId = "one_step_back"))
        val untouched = GameState(activeSlot = 2, sceneId = "other_slot", health = 77, gold = 44)
        store.save(2, untouched)

        val fresh = store.restart(1)

        assertEquals(GameState(activeSlot = 1), fresh)
        assertEquals(GameState(activeSlot = 1), store.load(1))
        assertEquals(untouched, store.load(2))
        assertFalse(store.hasPreviousState(1))
        assertEquals(1, store.currentActiveSlot())
    }
}
