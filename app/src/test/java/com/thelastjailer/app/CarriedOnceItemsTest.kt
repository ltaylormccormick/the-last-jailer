package com.thelastjailer.app

import com.thelastjailer.app.data.ItemCatalog
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R3: story keepsakes are carried once. Pins the audited 30-item allow-list, the one deliberate
 * repeatable (healing_draught), that every item the story grants is classified, that no single
 * telling can legitimately grant a carried-once item twice, and that historical duplicate
 * inventories are left exactly as saved. Save format, shops and consumption are not involved.
 */
class CarriedOnceItemsTest {

    private val expectedAllowList = setOf(
        "archivists_final_page", "archivists_frame_notes", "ashen_signet",
        "broken_grasp_of_the_whole", "cinder_marshal_missive", "duskmere_keepers_journal",
        "dwarven_token", "emberlow_wardens_token", "fenmoor_ward_shard",
        "greymoor_wardens_ledger", "halvards_warden_chain", "ilsevets_blade", "jailers_brand",
        "kestrels_locket", "ordruns_broken_blade", "promise_of_the_six", "record_of_ashwell",
        "rubbing_of_halvards_mark", "scraped_wax_disc", "shard_of_the_first_ward",
        "shard_of_the_seventh_door", "sigil_of_the_unbroken_vigil", "splinter_of_the_whole",
        "stonebeard_lantern", "tarnished_guard_token", "thread_of_the_sundering",
        "torn_sanctum_plans", "twisted_frame_component", "voss_seal", "warden_sigil"
    )

    private val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val choices: List<Choice> = nodes.flatMap { it.choices }

    private fun grant(vararg ids: String) = Consequences(grantItemIds = ids.toList())

    @Test
    fun `allow-list is exactly the audited 30 items`() {
        assertEquals(30, ItemCatalog.carriedOnceStoryItems.size)
        assertEquals(expectedAllowList, ItemCatalog.carriedOnceStoryItems)
    }

    @Test
    fun `every allow-listed id exists in the catalog and is not combat equipment`() {
        ItemCatalog.carriedOnceStoryItems.forEach { id ->
            assertTrue("$id missing from catalog", ItemCatalog.get(id) != null)
            assertFalse("$id is already unique equipment", ItemCatalog.isUniqueEquipment(id))
        }
    }

    @Test
    fun `healing_draught is repeatable by design and not carried once`() {
        assertEquals(setOf("healing_draught"), ItemCatalog.repeatableByDesign)
        assertFalse(ItemCatalog.isCarriedOnce("healing_draught"))
        assertTrue(ItemCatalog.carriedOnceStoryItems.intersect(ItemCatalog.repeatableByDesign).isEmpty())
    }

    @Test
    fun `shop and unlisted items keep their behaviour`() {
        listOf("greater_healing_draught", "traveler_ration", "broken_sword", "reforged_guard_clasp").forEach {
            assertFalse("$it should stay repeatable", ItemCatalog.isCarriedOnce(it))
        }
        listOf("sturdy_buckler", "leather_armor", "proper_helmet", "upgraded_sword").forEach {
            assertTrue("$it should stay unique equipment", ItemCatalog.isCarriedOnce(it))
        }
    }

    @Test
    fun `every item a story choice grants is classified exactly once`() {
        val granted = choices.flatMap { it.consequences.grantItemIds }.toSet()
        assertTrue(granted.isNotEmpty())
        granted.forEach { id ->
            val classes = listOf(
                ItemCatalog.isUniqueEquipment(id),
                id in ItemCatalog.carriedOnceStoryItems,
                id in ItemCatalog.repeatableByDesign
            ).count { it }
            assertEquals("$id must be in exactly one class (unique equipment, carried once, repeatable)", 1, classes)
        }
    }

    @Test
    fun `no combat encounter or single choice grants a carried-once item twice`() {
        choices.forEach { c ->
            val repeats = c.consequences.grantItemIds.groupBy { it }.filter { it.value.size > 1 }.keys
            assertTrue("choice '${c.label}' grants ${repeats} twice", repeats.none { ItemCatalog.isCarriedOnce(it) })
        }
    }

    /**
     * Over-approximate graph check (requirements ignored, so it can only report extra duplicates):
     * from every choice that grants a carried-once keepsake, no route through the story reaches a
     * choice that grants the same keepsake again. If a future scene adds one, this fails and the
     * item must be reconsidered, not silently dropped on the floor by the grant guard.
     */
    @Test
    fun `no story route grants the same carried-once keepsake twice in one telling`() {
        fun next(nodeId: String): List<String> {
            val node = StoryRepository.nodeOrNull(nodeId) ?: return emptyList()
            // "Begin the tale again" (-> fallen_knight) starts a NEW telling, which is exactly the
            // case the grant guard exists for, so it is not an edge within one telling.
            val edges = node.choices.map { it.nextNodeId }.filter { it != "fallen_knight" }.toMutableList()
            node.combatEncounterId?.let {
                val e = com.thelastjailer.app.data.CombatRepository.encounter(it)
                edges += e.victoryNodeId
                e.defeatNodeId?.let { d -> edges += d }
            }
            return edges
        }
        fun reachableFrom(start: String): Set<String> {
            val seen = mutableSetOf<String>()
            val queue = ArrayDeque(listOf(start))
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (seen.add(id)) queue.addAll(next(id))
            }
            return seen
        }
        val reachCache = HashMap<String, Set<String>>()
        val offenders = mutableListOf<String>()
        ItemCatalog.carriedOnceStoryItems.filterNot { ItemCatalog.isUniqueEquipment(it) }.forEach { item ->
            val sites = nodes.flatMap { n -> n.choices.filter { item in it.consequences.grantItemIds }.map { n to it } }
            sites.forEach { (_, choice) ->
                val reach = reachCache.getOrPut(choice.nextNodeId) { reachableFrom(choice.nextNodeId) }
                val again = nodes.filter { n -> n.id in reach && n.choices.any { item in it.consequences.grantItemIds } }
                if (again.isNotEmpty()) offenders += "$item: '${choice.label}' can reach ${again.map { it.id }}"
            }
        }
        assertTrue("Routes granting a carried-once keepsake twice: $offenders", offenders.isEmpty())
    }

    @Test
    fun `each allow-listed keepsake is granted by at least one story choice`() {
        val granted = choices.flatMap { it.consequences.grantItemIds }.toSet()
        assertTrue(ItemCatalog.carriedOnceStoryItems.filterNot { it in granted }.toString(), granted.containsAll(ItemCatalog.carriedOnceStoryItems))
    }

    @Test
    fun `granting an allow-listed item twice leaves one copy`() {
        ItemCatalog.carriedOnceStoryItems.forEach { id ->
            val once = GameState().applyChoice(Choice("a", "x", consequences = grant(id)))
            val twice = once.applyChoice(Choice("b", "x", consequences = grant(id)))
            assertEquals(id, 1, twice.inventory.count { it == id })
        }
    }

    @Test
    fun `two grants of the same item in one choice leave one copy`() {
        val s = GameState().applyChoice(Choice("a", "x", consequences = grant("kestrels_locket", "kestrels_locket")))
        assertEquals(1, s.inventory.count { it == "kestrels_locket" })
    }

    @Test
    fun `healing_draught still stacks across grants`() {
        var s = GameState()
        repeat(3) { s = s.applyChoice(Choice("a", "x", consequences = grant("healing_draught"))) }
        assertEquals(3, s.inventory.count { it == "healing_draught" })
    }

    @Test
    fun `unique combat equipment is still carried once`() {
        var s = GameState()
        repeat(2) { s = s.applyChoice(Choice("a", "x", consequences = grant("sturdy_buckler"))) }
        assertEquals(1, s.inventory.count { it == "sturdy_buckler" })
    }

    @Test
    fun `a historical inventory with two lockets keeps both and gains no third`() {
        val old = GameState(inventory = listOf("kestrels_locket", "healing_draught", "kestrels_locket"))
        val after = old.applyChoice(Choice("a", "x", consequences = grant("kestrels_locket")))
        assertEquals(2, after.inventory.count { it == "kestrels_locket" })
        assertEquals(1, after.inventory.count { it == "healing_draught" })
        assertEquals(old.inventory, after.inventory)
    }

    @Test
    fun `historical duplicates are untouched by an unrelated choice`() {
        val old = GameState(inventory = listOf("voss_seal", "voss_seal", "dwarven_token", "dwarven_token"))
        val after = old.applyChoice(Choice("a", "x", consequences = Consequences(setFlags = setOf("f"))))
        assertEquals(old.inventory, after.inventory)
    }

    @Test
    fun `a new keepsake in a grant batch is added alongside a skipped duplicate`() {
        val s = GameState(inventory = listOf("kestrels_locket"))
            .applyChoice(Choice("a", "x", consequences = grant("kestrels_locket", "voss_seal", "healing_draught")))
        assertEquals(listOf("kestrels_locket", "voss_seal", "healing_draught"), s.inventory)
    }

    @Test
    fun `Back restores the previous inventory exactly`() {
        val before = GameState(inventory = listOf("healing_draught"))
        val after = before.applyChoice(Choice("a", "x", consequences = grant("warden_sigil")))
        assertEquals(listOf("healing_draught", "warden_sigil"), after.inventory)
        assertEquals(listOf("healing_draught"), before.inventory)
    }

    @Test
    fun `purchaseItem and consumeItems are unchanged`() {
        val bought = GameState(gold = 200).purchaseItem("healing_draught", 40).purchaseItem("healing_draught", 40)
        assertEquals(2, bought.inventory.count { it == "healing_draught" })
        assertEquals(120, bought.gold)
        assertEquals(1, bought.consumeItems(listOf("healing_draught")).inventory.size)
        val clasp = GameState(gold = 200).purchaseItem("reforged_guard_clasp", 50)
        assertEquals(listOf("reforged_guard_clasp"), clasp.inventory)
    }

    @Test
    fun `replay keeps a single copy of each carried keepsake`() {
        val finished = GameState(
            chapterId = "chapter_30", sceneId = "the_watch_continues_dark_aid_captured",
            inventory = listOf("kestrels_locket", "cinder_marshal_missive", "healing_draught")
        )
        val replayChoice = StoryRepository.visibleChoices(StoryRepository.node(finished.sceneId), finished)
            .single { it.nextNodeId == "fallen_knight" }
        val replayed = finished.applyChoice(replayChoice)
        val again = replayed.applyChoice(Choice("a", "x", consequences = grant("kestrels_locket", "cinder_marshal_missive")))
        assertEquals(1, again.inventory.count { it == "kestrels_locket" })
        assertEquals(1, again.inventory.count { it == "cinder_marshal_missive" })
        assertEquals(1, again.inventory.count { it == "healing_draught" })
    }
}
