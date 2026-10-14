package com.thelastjailer.app.data

import com.thelastjailer.app.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Follow-up Batch C.
 *
 * Part 1 pins the Chapters 1-3 continuity corrections (the blade, the rope, the coin).
 * Part 2 records today's Stonebeard theft/restitution numbers exactly as the engine plays them. It
 * changes nothing: if a later batch retunes a price or reward on purpose, these tests are the
 * list of numbers to review.
 */
class BatchCContinuityEconomyTest {

    private fun node(id: String) = StoryRepository.node(id)
    private fun pick(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun labels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }
    private fun at(id: String, flags: Set<String> = emptySet(), gold: Int = 25, health: Int = 100, honour: Int = 0, inventory: List<String> = emptyList()) =
        GameState(chapterId = node(id).chapterId, sceneId = id, flags = flags, gold = gold, health = health, honour = honour, inventory = inventory)
    private fun text(id: String) = node(id).narrativeText.replace(Regex("\\s+"), " ")

    // ---------------------------------------------------------------- Part 1: continuity

    @Test
    fun `the blade is sheathed once on every route into the knocking, never twice`() {
        val t = text("ch1_knock_resumes")
        assertTrue(t, t.contains("back in its sheath"))
        assertFalse(t, t.contains("sheathes the blade"))
        // Every route that arrives here either wipes or sheathes in its own choice; the scene itself
        // describes the state, not a second action.
        val arrivals = StoryRepository.nodesInChapter("chapter_1").filter { n -> n.choices.any { it.nextNodeId == "ch1_knock_resumes" } }
        assertTrue(arrivals.isNotEmpty())
        arrivals.forEach { n -> assertEquals(n.id, 1, n.choices.size) }
    }

    @Test
    fun `after sheathing, the hand returning to the hilt explains the protected redraw`() {
        assertTrue(text("ch1_knock_resumes").contains("His hand goes back to the hilt"))
        assertEquals("sword_drawn", node("ch1_knock_resumes").choices.single().nextNodeId)
        // The protected legacy scene is untouched.
        assertTrue(text("sword_drawn").startsWith("Kaelen draws his sword. The motion is old habit, older than shame."))
        assertEquals(listOf("Enter, blade first." to "beneath_the_roots"), node("sword_drawn").choices.map { it.label to it.nextNodeId })
    }

    @Test
    fun `the smith gives Kaelen a lantern and no rope, and the lantern is what is granted`() {
        val t = text("word_at_stonebeard")
        assertFalse(t, t.contains("rope"))
        assertTrue(t, t.contains("presses a second lantern into Kaelen's hands"))
        node("word_at_stonebeard").choices.forEach { c ->
            val granted = c.consequences.grantItemIds
            assertEquals(c.label, listOf("stonebeard_lantern"), granted)
        }
    }

    @Test
    fun `no Chapter 1 to 3 scene hands over an item its text names but the game does not grant`() {
        // Rope was the only confirmed case. Nothing in these chapters may mention a handed-over rope again.
        val handedRope = Regex("(coil of rope|length of rope|rope into|presses? .{0,40}rope)", RegexOption.IGNORE_CASE)
        listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }.forEach { n ->
            assertFalse("${n.id} mentions rope it never grants", handedRope.containsMatchIn(n.narrativeText))
        }
    }

    @Test
    fun `the stolen coin is a purse in both the theft and Voss's line`() {
        assertTrue(text("ch3_camp_robbed").contains("A purse of coin set aside for fodder"))
        assertFalse(text("ch3_camp_robbed").contains("sack"))
        assertTrue(text("ch3_surface_robbed").contains("A purse of coin has gone missing"))
    }

    // ---------------------------------------------------------------- Part 2: Stonebeard economy, as it stands

    /** Gold-equivalent used in the ledger: a healing draught costs 40 in the shop and heals 25, so 1 HP is 1.6 gold. */
    private val draught = 40

    @Test
    fun `economy snapshot - each choice at the Hold, from the same starting state`() {
        val start = at("stonebeard_hold")
        val helped = pick(start, "Help him")
        val confessed = pick(at("stonebeard_hold", honour = 1), "Tell him honestly")
        val movedOn = pick(start, "Move on")
        val robbed = pick(start, "Take a draught")
        // gold, honour, draughts, token
        assertEquals(listOf(25, 0, 1, true), listOf(helped.gold, helped.honour, helped.inventory.count { it == "healing_draught" }, "dwarven_token" in helped.inventory))
        assertEquals(listOf(25, 2, 0, false), listOf(confessed.gold, confessed.honour, confessed.inventory.count { it == "healing_draught" }, "dwarven_token" in confessed.inventory))
        assertEquals(listOf(25, 0, 0, false), listOf(movedOn.gold, movedOn.honour, movedOn.inventory.count { it == "healing_draught" }, "dwarven_token" in movedOn.inventory))
        assertEquals(listOf(40, -1, 1, false), listOf(robbed.gold, robbed.honour, robbed.inventory.count { it == "healing_draught" }, "dwarven_token" in robbed.inventory))
        assertTrue("helped_dwarf" in helped.flags && "Friend of Stonebeard" in helped.trophies)
        assertTrue("took_from_the_bench" in robbed.flags && "owes_the_smith" in robbed.flags)
    }

    @Test
    fun `economy snapshot - what a thief ends with for each way of answering the smith`() {
        // A thief arriving with exactly what the theft left them: 40 gold, one draught, Honour -1.
        val thief = at("word_at_stonebeard_thief", flags = setOf("took_from_the_bench", "owes_the_smith"), gold = 40, honour = -1, inventory = listOf("healing_draught"))
        val paid = pick(thief, "Put forty gold")
        val worked = pick(thief, "Take a shift")
        val silent = pick(thief, "Say nothing")
        assertEquals(listOf(0, 0), listOf(paid.gold, paid.honour))
        assertEquals(listOf(40, 0), listOf(worked.gold, worked.honour))
        // -10 for the shift, +1 passive regen on the scene transition (100 -> 91).
        assertEquals(thief.health - 9, worked.health)
        assertEquals(listOf(40, -1), listOf(silent.gold, silent.honour))
        assertTrue("owes_the_smith" in silent.flags && "owes_the_smith" !in paid.flags && "owes_the_smith" !in worked.flags)
        // The lantern is cosmetic (no combat effect), so silence loses no mechanical value.
        assertTrue(ItemCatalog.get("stonebeard_lantern")!!.combatEffect == null)
    }

    @Test
    fun `economy snapshot - the forty gold row is reachable only for some thieves`() {
        // Victory route: 25 start + 15 theft + 10 reward = 50.   Defeat route: 25 + 15 - 10 = 30 (locked).
        val victoryThief = at("word_at_stonebeard_thief", flags = setOf("took_from_the_bench"), gold = 25 + 15 + 10)
        val defeatThief = at("word_at_stonebeard_thief", flags = setOf("took_from_the_bench"), gold = 25 + 15 - 10)
        val payRow = node("word_at_stonebeard_thief").choices.first { it.label.startsWith("Put forty") }.requirements!!
        assertTrue(payRow.isSatisfiedBy(victoryThief))
        assertFalse(payRow.isSatisfiedBy(defeatThief))
        // Work and silence are never locked.
        listOf("Take a shift", "Say nothing").forEach { p ->
            assertTrue(p, labels(defeatThief).any { it.startsWith(p) })
        }
    }

    @Test
    fun `economy snapshot - the Chapter 3 second chance costs exactly what the first did`() {
        val ledger = at("ch3_halvards_ledger", flags = setOf("took_from_the_bench", "owes_the_smith"), gold = 60)
        assertEquals(20, pick(ledger, "Give Halvard forty gold").gold)
        assertEquals(ledger.health - 9, pick(ledger, "Spend the night").health) // -10 plus 1 regen
        assertEquals(60, pick(ledger, "Say nothing").gold)
    }
}
