package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Follow-up Batch A: the tarnished token, the smith's vouch, and the reforged clasp's description.
 * Halvard answers the token and the vouch, only when the player actually offers them.
 */
class BatchATokenAndVouchTest {

    private fun node(id: String) = StoryRepository.node(id)
    private fun pick(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun labels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }
    private fun at(id: String, flags: Set<String> = emptySet(), honour: Int = 0, courage: Int = 1) =
        GameState(chapterId = node(id).chapterId, sceneId = id, flags = flags, honour = honour, courage = courage)

    private val tokenScenes = listOf("the_cloaks_truth_asked", "the_cloaks_truth_offered")
    private val holdingToken = setOf("kept_the_token")

    // ---------------------------------------------------------------- The token

    @Test
    fun `showing the token gets a distinct answer from Halvard and then the first seal`() {
        tokenScenes.forEach { id ->
            val before = at(id, flags = holdingToken).copy(inventory = listOf("tarnished_guard_token"))
            val shown = pick(before, "Show him")
            assertEquals(id, "the_clasp_answered", shown.sceneId)
            assertTrue("revealed_token" in shown.flags)
            assertTrue("Blood and Silver" in shown.trophies)
            val next = pick(shown, "Put it away")
            assertEquals("the_first_seal", next.sceneId)
        }
    }

    @Test
    fun `showing the token costs and pays nothing, grants no trust and sets no extra flag`() {
        tokenScenes.forEach { id ->
            val before = at(id, flags = holdingToken, honour = 2).copy(inventory = listOf("tarnished_guard_token"), gold = 40, xp = 5)
            val end = pick(pick(before, "Show him"), "Put it away")
            assertEquals(before.courage, end.courage)
            assertEquals(before.honour, end.honour)
            assertEquals(before.gold, end.gold)
            assertEquals(before.xp, end.xp)
            assertEquals(before.health, end.health)
            assertEquals(before.inventory, end.inventory)
            assertEquals(before.flags + "revealed_token", end.flags)
            assertEquals(setOf("Blood and Silver"), end.trophies)
        }
        val scene = node("the_clasp_answered")
        assertTrue(scene.choices.all { it.consequences.setFlags.isEmpty() && it.consequences.statDeltas.isEmpty() && it.consequences.grantItemIds.isEmpty() })
    }

    @Test
    fun `withholding the token skips the answer and earns nothing`() {
        tokenScenes.forEach { id ->
            val before = at(id, flags = holdingToken)
            val silent = pick(before, "Say nothing")
            assertEquals("the_first_seal", silent.sceneId)
            assertFalse("revealed_token" in silent.flags)
            assertTrue(silent.trophies.isEmpty())
            assertEquals(before.flags, silent.flags)
        }
    }

    @Test
    fun `without the token there is no show row, and the answer scene is reached only through it`() {
        tokenScenes.forEach { id -> assertTrue(labels(at(id)).none { it.startsWith("Show him") }) }
        val refs = listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }
            .flatMap { n -> n.choices.map { n.id to it } }.filter { it.second.nextNodeId == "the_clasp_answered" }
        assertEquals(tokenScenes.sorted(), refs.map { it.first }.sorted())
        assertTrue(refs.all { it.second.label.startsWith("Show him") })
    }

    @Test
    fun `Halvard's answer recognises the clasp, explains the Guard's duty and does not name an owner or reveal canon`() {
        val t = node("the_clasp_answered").narrativeText.replace(Regex("\\s+"), " ")
        assertTrue(t.contains("Guard"))
        assertTrue(t.contains("the way to it") || t.contains("kept the way"))
        assertTrue("the Guard's decline is Halvard's own reading, not stated as fact", t.contains("Somewhere along the line, I reckon, they forgot"))
        assertTrue("answer must withhold trust", t.contains("Don't mistake this for welcome"))
        listOf("Ilsevet", "Order", "prisoner", "Marshal", "Selvane", "Thessaly", "hunter", "rider", "paid", "Door of", "belonged", "wore it", "your friend", "I trust")
            .forEach { assertFalse("answer scene reveals '$it'", t.contains(it, ignoreCase = true)) }
    }

    @Test
    fun `the trophy and the Journal line are unchanged`() {
        assertTrue("Blood and Silver" in StoryRepository.availableTrophies)
        val lines = FlagDescriptions.sections(setOf("revealed_token")).flatMap { it.lines }
        assertEquals(listOf("I showed Halvard the tarnished token from the roots."), lines)
    }

    @Test
    fun `back restores the pre-choice state, and repeating the choice cannot double the trophy or items`() {
        val before = at("the_cloaks_truth_asked", flags = holdingToken).copy(inventory = listOf("tarnished_guard_token"))
        val store = SaveStore(FakeSharedPreferences())
        val after = pick(before, "Show him")
        store.save(1, after); store.savePreviousState(1, before)
        val restored = store.restorePreviousState(1)!!
        // Batch 2B-3 (deliberate change): the trophy the taken-back choice earned stays earned; everything else
        // is exactly the pre-choice state. The old assertion expected the trophy to be dropped.
        assertEquals(before.copy(activeSlot = 1, trophies = after.trophies), restored)
        assertTrue(after.trophies.isNotEmpty())
        val again = pick(restored, "Show him")
        assertEquals(after.trophies, again.trophies)
        assertEquals(after.inventory, again.inventory)
        // A second telling that shows it again does not add a second trophy.
        val second = pick(again.copy(sceneId = "the_cloaks_truth_offered"), "Show him")
        assertEquals(1, second.trophies.size)
        assertEquals(before.inventory, second.inventory)
    }

    // ---------------------------------------------------------------- The smith's vouch

    @Test
    fun `invoking the smith's word is acknowledged by Halvard, then he answers the question`() {
        val before = at("the_warden_elder", flags = setOf("smith_vouched"), honour = 1)
        val named = pick(before, "\"The smith at the broken gate")
        assertEquals("the_smiths_word", named.sceneId)
        assertEquals(before.honour + 1, named.honour)
        assertTrue("asked_about_jailers" in named.flags)
        val heard = pick(named, "Hear him out")
        assertEquals("the_cloaks_truth_asked", heard.sceneId)
        assertEquals(named.honour, heard.honour)
        assertEquals(named.flags, heard.flags)
    }

    @Test
    fun `the plain question and the offer are unchanged and the vouch row never shows without earned support`() {
        val plain = at("the_warden_elder")
        assertEquals(2, labels(plain).size)
        assertEquals("the_cloaks_truth_asked", pick(plain, "\"What are you guarding").sceneId)
        assertEquals("the_cloaks_truth_offered", pick(plain, "\"I'll help you").sceneId)
        val vouched = at("the_warden_elder", flags = setOf("smith_vouched"))
        assertEquals(3, labels(vouched).size)
        assertEquals("the_cloaks_truth_asked", pick(vouched, "\"What are you guarding").sceneId)
    }

    @Test
    fun `earning the smith's support gives nothing until the player tells Halvard`() {
        val atHold = GameState(chapterId = "chapter_2", sceneId = "word_at_stonebeard", flags = setOf("helped_dwarf"), honour = 2)
        val down = pick(atHold, "Go back down")
        assertTrue("smith_vouched" in down.flags)
        assertEquals(atHold.honour, down.honour)
        val elder = pick(down, "Press a hand")
        assertEquals("the_warden_elder", elder.sceneId)
        val plain = pick(elder, "\"What are you guarding")
        assertEquals("a player who never makes the claim gains no Honour from it", atHold.honour, plain.honour)
        assertTrue("revealed_token" !in plain.flags)
    }

    @Test
    fun `the smith's word scene is reached only by the vouch row and never hands over trust`() {
        val refs = listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }
            .flatMap { n -> n.choices.map { n.id to it } }.filter { it.second.nextNodeId == "the_smiths_word" }
        assertEquals(listOf("the_warden_elder"), refs.map { it.first })
        val scene = node("the_smiths_word")
        val text = scene.narrativeText.replace(Regex("\\s+"), " ")
        assertTrue(text.contains("lantern") && text.contains("doesn't mean I will"))
        assertTrue(scene.choices.all { it.consequences.setFlags.isEmpty() && it.consequences.statDeltas.isEmpty() })
    }

    @Test
    fun `the Journal no longer claims the smith spoke to Halvard`() {
        val line = FlagDescriptions.sections(setOf("smith_vouched")).flatMap { it.lines }.single()
        assertFalse(line.contains("Halvard"))
        assertTrue(line.contains("lantern"))
    }

    // ---------------------------------------------------------------- Old saves

    @Test
    fun `saves parked at the affected Chapter 2 scenes load and play on`() {
        val store = SaveStore(FakeSharedPreferences())
        val parked = listOf(
            at("the_cloaks_truth_asked", flags = holdingToken + "asked_about_jailers"),
            at("the_cloaks_truth_offered", flags = holdingToken + "offered_to_help"),
            at("the_warden_elder", flags = setOf("smith_vouched", "helped_dwarf")),
            at("the_first_seal", flags = holdingToken + setOf("revealed_token", "asked_about_jailers"))
        )
        parked.forEachIndexed { i, s ->
            store.save(i + 1, s)
            val loaded = store.load(i + 1)!!
            assertEquals(s.copy(activeSlot = i + 1), loaded)
            assertTrue(labels(loaded).isNotEmpty())
        }
        val shown = pick(store.load(1)!!, "Show him")
        assertEquals("the_clasp_answered", shown.sceneId)
        assertEquals("the_smiths_word", pick(store.load(3)!!, "\"The smith").sceneId)
        // A save that already showed the token before this update plays on from the seal exactly as before.
        assertEquals(3, labels(store.load(4)!!).size)
    }

    // ---------------------------------------------------------------- Shop clasp

    @Test
    fun `the reforged clasp description holds for every player, and its id and price are unchanged`() {
        val item = ItemCatalog.get("reforged_guard_clasp")!!
        assertEquals("Reforged Guard's Clasp", item.name)
        assertFalse(item.description.contains("tarnished", ignoreCase = true))
        assertFalse(item.description.contains("he carries", ignoreCase = true))
        assertTrue(item.description.contains("struck new", ignoreCase = true))
    }
}
