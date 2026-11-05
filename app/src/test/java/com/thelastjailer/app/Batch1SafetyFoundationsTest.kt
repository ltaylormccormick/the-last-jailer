package com.thelastjailer.app

import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.ItemCatalog
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.ui.AppScreen
import com.thelastjailer.app.ui.bottomNavScreens
import com.thelastjailer.app.ui.isNavigationEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Batch 1 safety foundations: chapter-id consistency, unique-equipment protection, and
 * characterisation of the save/undo/combat contract that later checkpoint work will build on.
 * Tests named "characterisation" pin CURRENT behaviour on purpose so a future change to it is a
 * deliberate, reviewed one rather than an accident.
 */
class Batch1SafetyFoundationsTest {
    private lateinit var store: SaveStore

    @Before
    fun setUp() {
        store = SaveStore(FakeSharedPreferences())
    }

    private fun anyNodeIn(chapterId: String): StoryNode =
        StoryRepository.nodesInChapter(chapterId).first()

    private fun choiceInto(nextNodeId: String, consequences: Consequences = Consequences()) =
        Choice(label = "go", nextNodeId = nextNodeId, consequences = consequences)

    // ---- chapter-id consistency ---------------------------------------------------------

    @Test
    fun `every story node resolves to its own chapter id`() {
        val nodes = (1..30).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
        assertTrue(nodes.isNotEmpty())
        nodes.forEach { node ->
            val state = GameState(chapterId = "chapter_1", sceneId = node.id).withSceneChapter()
            assertEquals(node.id, node.chapterId, state.chapterId)
        }
    }

    @Test
    fun `applying a choice into another chapter updates the chapter id`() {
        val target = anyNodeIn("chapter_2")
        val result = GameState(chapterId = "chapter_1", sceneId = "fallen_knight")
            .applyChoice(choiceInto(target.id))

        assertEquals("chapter_2", result.chapterId)
        assertEquals(target.id, result.sceneId)
    }

    @Test
    fun `a choice to an unknown node leaves the chapter id untouched`() {
        val result = GameState(chapterId = "chapter_3", sceneId = anyNodeIn("chapter_3").id)
            .applyChoice(choiceInto("not_a_real_node"))

        assertEquals("chapter_3", result.chapterId)
        assertNull(StoryRepository.nodeOrNull("not_a_real_node"))
    }

    @Test
    fun `resolving a fight keeps the chapter id in step on victory and on defeat`() {
        val encounter = CombatRepository.encounter("siege_encounter")
        val state = GameState(chapterId = "chapter_1", sceneId = "fallen_knight")

        val won = state.resolveCombat(encounter, CombatOutcome(victory = true, damageTaken = 0, consumedItemIds = emptyList()))
        val lost = state.resolveCombat(encounter, CombatOutcome(victory = false, damageTaken = 0, consumedItemIds = emptyList()))

        assertEquals(StoryRepository.node(encounter.victoryNodeId).chapterId, won.chapterId)
        assertEquals(StoryRepository.node(encounter.defeatNodeId!!).chapterId, lost.chapterId)
    }

    @Test
    fun `a legacy save whose chapter id never moved heals to the scene's chapter on load`() {
        val scene = anyNodeIn("chapter_7")
        store.save(1, GameState(chapterId = "chapter_1", sceneId = scene.id))

        assertEquals("chapter_7", store.load(1)!!.chapterId)
    }

    @Test
    fun `a saved scene that no longer exists keeps its stored chapter id`() {
        store.save(1, GameState(chapterId = "chapter_5", sceneId = "node_removed_by_an_update"))

        assertEquals("chapter_5", store.load(1)!!.chapterId)
    }

    @Test
    fun `the undo snapshot also reports the scene's chapter`() {
        val later = GameState(chapterId = "chapter_1", sceneId = anyNodeIn("chapter_4").id)
        store.savePreviousState(1, later)

        assertEquals("chapter_4", store.loadPreviousState(1)!!.chapterId)
    }

    // ---- unique equipment ----------------------------------------------------------------

    @Test
    fun `combat equipment is classified as unique and consumables are not`() {
        listOf("sturdy_buckler", "leather_armor", "proper_helmet", "upgraded_sword")
            .forEach { assertTrue(it, ItemCatalog.isUniqueEquipment(it)) }
        listOf("healing_draught", "greater_healing_draught", "dwarven_token", "stonebeard_lantern", "not_an_item")
            .forEach { assertFalse(it, ItemCatalog.isUniqueEquipment(it)) }
    }

    @Test
    fun `granting equipment already carried does not add a second copy`() {
        val state = GameState(inventory = listOf("sturdy_buckler"))

        val result = state.applyChoice(choiceInto("x", Consequences(grantItemIds = listOf("sturdy_buckler"))))

        assertEquals(listOf("sturdy_buckler"), result.inventory)
    }

    @Test
    fun `a single grant naming the same equipment twice adds one copy`() {
        val result = GameState().applyChoice(
            choiceInto("x", Consequences(grantItemIds = listOf("upgraded_sword", "upgraded_sword")))
        )

        assertEquals(listOf("upgraded_sword"), result.inventory)
    }

    @Test
    fun `consumables still stack but audited story keepsakes no longer do`() {
        val result = GameState(inventory = listOf("healing_draught", "dwarven_token")).applyChoice(
            choiceInto("x", Consequences(grantItemIds = listOf("healing_draught", "dwarven_token")))
        )

        assertEquals(2, result.inventory.count { it == "healing_draught" })
        // R3 (deliberate update, was 2): dwarven_token is on the audited carried-once allow-list.
        assertEquals(1, result.inventory.count { it == "dwarven_token" })
    }

    @Test
    fun `characterisation - existing duplicates in a save are left alone`() {
        val state = GameState(inventory = listOf("sturdy_buckler", "sturdy_buckler"))

        val result = state.applyChoice(choiceInto("x", Consequences(grantItemIds = listOf("healing_draught"))))

        assertEquals(2, result.inventory.count { it == "sturdy_buckler" })
    }

    @Test
    fun `replaying the Stonebeard buckler reward does not stack a second buckler`() {
        val reward = StoryRepository.node("smiths_warning_helped").choices
            .first { "sturdy_buckler" in it.consequences.grantItemIds }
        val helped = GameState(flags = setOf("helped_dwarf"))

        val firstTelling = helped.applyChoice(reward)
        val secondTelling = firstTelling.copy(sceneId = "smiths_warning_helped").applyChoice(reward)

        assertEquals(1, firstTelling.inventory.count { it == "sturdy_buckler" })
        assertEquals(1, secondTelling.inventory.count { it == "sturdy_buckler" })
    }

    @Test
    fun `buying equipment already carried is a no-op and costs nothing`() {
        val state = GameState(gold = 500, inventory = listOf("leather_armor"))

        val result = state.purchaseItem("leather_armor", 70)

        assertEquals(state, result)
    }

    @Test
    fun `buying unowned equipment and repeatable draughts still works`() {
        val bought = GameState(gold = 300).purchaseItem("proper_helmet", 160)
        assertEquals(140, bought.gold)
        assertEquals(listOf("proper_helmet"), bought.inventory)

        val twice = GameState(gold = 100).purchaseItem("healing_draught", 40).purchaseItem("healing_draught", 40)
        assertEquals(2, twice.inventory.count { it == "healing_draught" })
    }

    // ---- characterisation: save / undo / combat contract -----------------------------

    @Test
    fun `characterisation - a story choice snapshots the pre-choice state, autosaves, and Back restores it exactly`() {
        val before = GameState(
            activeSlot = 1, sceneId = "fallen_knight", gold = 25, honour = 1,
            inventory = listOf("healing_draught"), flags = setOf("a"), trophies = setOf("T")
        )
        val choice = choiceInto(
            "the_road_away",
            Consequences(
                statDeltas = mapOf(StatType.GOLD to 10),
                grantItemIds = listOf("sturdy_buckler"),
                setFlags = setOf("b"),
                unlockTrophy = "New"
            )
        )

        val after = store.commitChoice(before, choice)

        assertEquals(after, store.load(1))
        assertTrue(store.hasPreviousState(1))
        val restored = store.restorePreviousState(1)!!
        // Batch 2B-3 (deliberate change): everything reversible comes back exactly as it was, but a trophy the
        // taken-back choice earned stays earned. This used to assert the trophy was dropped.
        assertEquals(before.copy(chapterId = restored.chapterId, trophies = setOf("T", "New")), restored)
        assertTrue(restored.trophies.contains("New"))
        assertFalse(restored.inventory.contains("sturdy_buckler"))
        assertFalse(store.hasPreviousState(1))
    }

    @Test
    fun `characterisation - choosing again after Back does not duplicate rewards`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 0)
        val choice = choiceInto("x", Consequences(statDeltas = mapOf(StatType.GOLD to 10), grantItemIds = listOf("healing_draught")))

        store.commitChoice(before, choice)
        val back = store.restorePreviousState(1)!!
        val again = store.commitChoice(back, choice)

        assertEquals(10, again.gold)
        assertEquals(1, again.inventory.count { it == "healing_draught" })
    }

    @Test
    fun `characterisation - Back is still available while a fight has been entered but not resolved`() {
        store.commitChoice(GameState(activeSlot = 1, sceneId = "fallen_knight"), choiceInto("the_road_away"))

        assertTrue(store.hasPreviousState(1))
    }

    @Test
    fun `characterisation - resolving a fight clears Back and autosaves the result`() {
        val encounter = CombatRepository.encounter("first_blood_encounter")
        listOf(true, false).forEach { victory ->
            val start = store.commitChoice(GameState(activeSlot = 1, sceneId = "fallen_knight"), choiceInto("the_road_away"))
            assertTrue(store.hasPreviousState(1))

            val result = store.commitCombatResult(start, encounter, CombatOutcome(victory, damageTaken = 20, consumedItemIds = emptyList()))

            assertFalse("victory=$victory", store.hasPreviousState(1))
            assertEquals(result, store.load(1))
        }
    }

    @Test
    fun `characterisation - a purchase clears Back even when it does nothing`() {
        val state = store.commitChoice(GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 0), choiceInto("the_road_away"))
        assertTrue(store.hasPreviousState(1))

        val result = store.commitPurchase(state, "healing_draught", 40)

        assertEquals(state, result)
        assertFalse(store.hasPreviousState(1))
    }

    @Test
    fun `characterisation - combat consumables are only removed when the fight resolves`() {
        val encounter = CombatRepository.encounter("first_blood_encounter")
        val start = GameState(activeSlot = 1, inventory = listOf("healing_draught", "healing_draught"))

        val result = store.commitCombatResult(
            start, encounter,
            CombatOutcome(victory = true, damageTaken = 10, consumedItemIds = listOf("healing_draught"))
        )

        assertEquals(1, result.inventory.count { it == "healing_draught" })
        assertEquals(2, start.inventory.count { it == "healing_draught" })
    }

    @Test
    fun `characterisation - every encounter has a defeat scene that exists`() {
        val ids = listOf(
            "first_blood_encounter", "opening_rider_encounter", "seal_breaker_encounter", "siege_encounter",
            "cinder_envoy_encounter", "unbound_encounter", "loyalist_ambush_encounter", "sanctum_sentinel_encounter",
            "right_hand_encounter", "the_last_reach_encounter"
        )
        ids.forEach { id ->
            val encounter = CombatRepository.encounter(id)
            assertNotNull(id, encounter.defeatNodeId)
            assertNotNull(id, StoryRepository.nodeOrNull(encounter.defeatNodeId!!))
            assertNotNull(id, StoryRepository.nodeOrNull(encounter.victoryNodeId))
        }
    }

    // ---- combat navigation ---------------------------------------------------------------

    @Test
    fun `characterisation - during a fight only Inventory is disabled in the bottom bar`() {
        val disabled = bottomNavScreens.filter { !isNavigationEnabled(it, combatActive = true) }
        assertEquals(listOf(AppScreen.INVENTORY), disabled)
        assertTrue(isNavigationEnabled(AppScreen.SAVE, combatActive = true))
    }

    @Test
    fun `outside a fight every bottom bar screen is enabled`() {
        assertTrue(bottomNavScreens.all { isNavigationEnabled(it, combatActive = false) })
        assertTrue(isNavigationEnabled(AppScreen.JOURNAL, combatActive = true))
    }
}
