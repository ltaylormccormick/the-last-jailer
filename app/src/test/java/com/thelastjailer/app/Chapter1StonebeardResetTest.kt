package com.thelastjailer.app

import com.thelastjailer.app.data.Chapter1Paths
import com.thelastjailer.app.data.STONEBEARD_FAMILY
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-1: the dead buckler offers are gone, and the Stonebeard flag family (Family S) resets at the
 * `stonebeard_hold` decision, and nowhere else.
 */
class Chapter1StonebeardResetTest {

    private val hold = "stonebeard_hold"
    private val afterNodes = listOf("smiths_warning_helped", "smiths_warning_confessed", "smiths_warning_moved_on", "smiths_warning_robbed")

    /** A state arriving at the hold carrying every Family S flag plus plenty of unrelated state. */
    private fun atHoldWithStaleFlags() = GameState(
        chapterId = "chapter_1", sceneId = hold, courage = 6, honour = 5, gold = 77, level = 3, xp = 12,
        inventory = listOf("dwarven_token", "healing_draught", "sturdy_buckler"),
        trophies = setOf("Friend of Stonebeard", "First Blood", "The Watch Goes On"),
        flags = STONEBEARD_FAMILY + setOf("opened_black_door", "kept_the_token", "announced_himself", "halvard_died", "unknown_future_flag")
    )

    private val unrelated = setOf("opened_black_door", "kept_the_token", "announced_himself", "halvard_died", "unknown_future_flag")

    private fun choose(s: GameState, prefix: String): GameState = Chapter1Paths.pick(s, prefix)

    @Test
    fun `the family is the three original Stonebeard flags plus the bench, atonement and vouching flags`() {
        assertEquals(
            setOf(
                "helped_dwarf", "confessed_to_dwarf", "took_buckler",
                "took_from_the_bench", "returned_the_draught", "owes_the_smith", "smith_vouched"
            ),
            STONEBEARD_FAMILY
        )
    }

    @Test
    fun `each of the four hold choices clears the whole family and keeps only its own flags`() {
        val expected = mapOf(
            "Help him" to setOf("helped_dwarf"),
            "Tell him honestly" to setOf("confessed_to_dwarf"),
            "Move on" to emptySet(),
            "Take a draught and" to setOf("took_from_the_bench", "owes_the_smith")
        )
        expected.forEach { (prefix, own) ->
            val after = choose(atHoldWithStaleFlags(), prefix)
            assertEquals(prefix, unrelated + own, after.flags)
        }
    }

    @Test
    fun `the reset leaves stats, gold, level, xp, items and trophies exactly as they were`() {
        val before = atHoldWithStaleFlags()
        listOf("Tell him honestly", "Move on").forEach { prefix ->
            val after = choose(before, prefix)
            assertEquals(prefix, before.copy(sceneId = after.sceneId, flags = after.flags, honour = after.honour), after)
        }
        val helped = choose(before, "Help him")
        // Helping grants its own items and trophy as it always did; nothing else moves.
        assertEquals(before.courage, helped.courage)
        assertEquals(before.gold, helped.gold)
        assertEquals(before.trophies, helped.trophies)
    }

    @Test
    fun `only the four hold choices anywhere in the story clear flags`() {
        val clearers = StoryRepository.chapters
            .flatMap { StoryRepository.nodesInChapter(it.id) }
            .flatMap { n -> n.choices.filter { it.consequences.clearFlags.isNotEmpty() }.map { n.id to it.label } }
            .filter { (id, _) -> id.startsWith("stonebeard") || id.startsWith("smiths_warning") }
        assertEquals(setOf(hold), clearers.map { it.first }.toSet())
        assertEquals(4, clearers.size)
        StoryRepository.node(hold).choices.forEach { assertEquals(STONEBEARD_FAMILY, it.consequences.clearFlags) }
    }

    @Test
    fun `no scene after the hold clears anything, so revisiting or continuing never repeats the reset`() {
        afterNodes.forEach { id ->
            StoryRepository.node(id).choices.forEach { assertTrue("$id: ${it.label}", it.consequences.clearFlags.isEmpty()) }
        }
    }

    @Test
    fun `the two dead buckler offers are gone and each node has exactly one way on`() {
        listOf("smiths_warning_confessed", "smiths_warning_moved_on").forEach { id ->
            val choices = StoryRepository.node(id).choices
            assertEquals(id, listOf("Heed his warning and go on." to "first_blood"), choices.map { it.label to it.nextNodeId })
        }
    }

    @Test
    fun `the buckler is offered only after helping, and only once per telling`() {
        val offers = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
            .filter { n -> n.chapterId == "chapter_1" }
            .flatMap { n -> n.choices.filter { "sturdy_buckler" in it.consequences.grantItemIds }.map { n.id to it } }
        assertEquals(listOf("smiths_warning_helped"), offers.map { it.first })
        assertEquals(setOf("helped_dwarf"), offers.single().second.requirements!!.requiredFlags)
    }

    @Test
    fun `a fresh game never holds more than one buckler on any path to the first fight`() {
        val paths = Chapter1Paths.enumerate(GameState(), setOf("first_blood"))
        assertEquals(364, paths.size)
        paths.forEach { assertTrue(it.labels.toString(), it.end.inventory.count { i -> i == "sturdy_buckler" } <= 1) }
    }

    // ---- Replay: the old Stonebeard decision cannot leak into a new telling ----

    private val replayedAtHold = {
        // Telling one ended with all the Stonebeard flags set; telling two reaches the hold again.
        GameState(
            chapterId = "chapter_1", sceneId = hold, courage = 9, honour = 8,
            inventory = listOf("dwarven_token", "healing_draught", "sturdy_buckler"),
            trophies = setOf("Friend of Stonebeard"),
            flags = setOf("helped_dwarf", "took_buckler", "kept_the_token", "opened_black_door")
        )
    }

    @Test
    fun `replay then move on - the old help no longer makes a buckler appear`() {
        val s = choose(replayedAtHold(), "Move on")
        assertEquals("smiths_warning_moved_on", s.sceneId)
        assertFalse("helped_dwarf" in s.flags)
        assertFalse("took_buckler" in s.flags)
        assertTrue("kept_the_token" in s.flags && "opened_black_door" in s.flags)
        val offered = StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).map { it.label }
        assertEquals(listOf("Heed his warning and go on."), offered)
    }

    @Test
    fun `replay then confess - the old help is cleared and the confession stands`() {
        val s = choose(replayedAtHold(), "Tell him honestly")
        assertEquals("smiths_warning_confessed", s.sceneId)
        assertTrue("confessed_to_dwarf" in s.flags)
        assertFalse("helped_dwarf" in s.flags)
        assertEquals(listOf("Heed his warning and go on."), StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).map { it.label })
    }

    @Test
    fun `replay then help - the buckler is offered again and taking it sets the flag again`() {
        val helped = choose(replayedAtHold(), "Help him")
        assertEquals("smiths_warning_helped", helped.sceneId)
        assertTrue("helped_dwarf" in helped.flags)
        assertFalse("the reset cleared the stale took_buckler", "took_buckler" in helped.flags)
        val offered = StoryRepository.visibleChoices(StoryRepository.node(helped.sceneId), helped).map { it.label }
        assertEquals(listOf("Take the buckler he offers.", "Heed his warning and go on."), offered)
        val taken = choose(helped, "Take the buckler")
        assertTrue("took_buckler" in taken.flags && "helped_dwarf" in taken.flags)
        assertEquals("first_blood", taken.sceneId)
    }

    @Test
    fun `after the reset a later choice never wipes the new decision`() {
        val helped = choose(replayedAtHold(), "Help him")
        val heeded = choose(helped, "Heed his warning")
        assertTrue("helped_dwarf" in heeded.flags)
    }

    @Test
    fun `replay no longer stacks a second buckler - token duplication is still the separate ticket`() {
        val s = choose(choose(replayedAtHold(), "Help him"), "Take the buckler")
        assertEquals(1, s.inventory.count { it == "sturdy_buckler" })
        assertEquals(2, s.inventory.count { it == "dwarven_token" })
    }

    // ---- Existing saves ----

    private fun roundTrip(state: GameState): GameState {
        val store = SaveStore(FakeSharedPreferences())
        store.save(1, state)
        return store.load(1)!!
    }

    @Test
    fun `saves already past the hold keep every Stonebeard decision they hold - loading never clears`() {
        afterNodes.forEach { id ->
            val s = GameState(chapterId = "chapter_1", sceneId = id, courage = 4, honour = 3, flags = STONEBEARD_FAMILY + "kept_the_token")
            assertEquals(id, s, roundTrip(s))
        }
        // A save in a later chapter that carries the old decisions is untouched too.
        val later = GameState(chapterId = "chapter_5", sceneId = StoryRepository.chapter("chapter_5")!!.startNodeId, flags = STONEBEARD_FAMILY + "halvard_died")
        assertEquals(later, roundTrip(later))
    }

    @Test
    fun `a save parked on the buckler-less nodes can still play forward to the first fight`() {
        listOf("smiths_warning_confessed", "smiths_warning_moved_on").forEach { id ->
            // Includes the stale-flag case that used to show the dead offer.
            val s = GameState(chapterId = "chapter_1", sceneId = id, flags = setOf("helped_dwarf"))
            val visible = StoryRepository.visibleChoices(StoryRepository.node(id), s)
            assertEquals(1, visible.size)
            assertEquals("first_blood", s.applyChoice(visible.single()).sceneId)
        }
    }

    @Test
    fun `a save parked at the hold with old flags resolves cleanly whichever way it goes`() {
        val s = replayedAtHold()
        listOf("Help him", "Tell him honestly", "Move on").forEach { prefix ->
            val after = choose(s, prefix)
            assertTrue(prefix, after.sceneId in afterNodes)
            assertTrue(prefix, unrelated.intersect(s.flags).all { it in after.flags })
        }
    }
}
