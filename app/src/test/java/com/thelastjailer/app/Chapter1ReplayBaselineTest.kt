package com.thelastjailer.app

import com.thelastjailer.app.data.Chapter1Paths
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-0 replay baseline. Pins what "Begin the tale again" does TODAY, including the behaviours the
 * Chapter 1 redesign intends to change. Each test says which batch is expected to alter it.
 *
 * Findings:
 *  - Replay keeps everything: stats, level, XP, gold, items, trophies and every flag. It only adds the
 *    "The Watch Goes On" trophy and moves the player to `fallen_knight`.
 *  - A full reset happens only through SaveStore.restart (the New Game path).
 */
class Chapter1ReplayBaselineTest {

    private val replayNodes = listOf(
        "the_watch_continues_dark_aid_captured", "the_watch_continues_dark_aid_mercy",
        "the_watch_continues_dark_aid_deferred", "the_watch_continues_unaided_captured",
        "the_watch_continues_unaided_mercy", "the_watch_continues_unaided_deferred"
    )

    /** A completed game, as the player holds it at the replay prompt. */
    private fun finished(node: String = replayNodes.first()) = GameState(
        chapterId = "chapter_30", sceneId = node, courage = 14, honour = 9, health = 190, maxHealth = 190,
        gold = 412, level = 11, xp = 35, xpToNextLevel = 1100,
        inventory = listOf("dwarven_token", "healing_draught", "sturdy_buckler"),
        trophies = setOf("Friend of Stonebeard", "First Blood"),
        flags = setOf("helped_dwarf", "confessed_to_dwarf", "kept_the_token", "opened_black_door", "took_buckler")
    )

    private fun replay(s: GameState): GameState {
        val node = StoryRepository.node(s.sceneId)
        return s.applyChoice(StoryRepository.visibleChoices(node, s).single { it.nextNodeId == "fallen_knight" })
    }

    @Test
    fun `all six ending nodes offer one replay choice that goes to fallen_knight and unlocks The Watch Goes On`() {
        replayNodes.forEach { id ->
            val node = StoryRepository.node(id)
            val replayChoices = node.choices.filter { it.nextNodeId == "fallen_knight" }
            assertEquals(id, 1, replayChoices.size)
            assertEquals(id, "The Watch Goes On", replayChoices.single().consequences.unlockTrophy)
            assertTrue(id, replayChoices.single().consequences.setFlags.isEmpty())
            assertTrue(id, replayChoices.single().consequences.statDeltas.isEmpty())
        }
    }

    @Test
    fun `replaying keeps stats, level, xp, gold, items and every flag, and only adds the trophy`() {
        replayNodes.forEach { id ->
            val before = finished(id)
            val after = replay(before)
            assertEquals("fallen_knight", after.sceneId)
            assertEquals(listOf(14, 9, 190, 412, 11, 35, 1100), listOf(after.courage, after.honour, after.maxHealth, after.gold, after.level, after.xp, after.xpToNextLevel))
            assertEquals(before.inventory, after.inventory)
            assertEquals(before.flags, after.flags)
            assertEquals(before.trophies + "The Watch Goes On", after.trophies)
        }
    }

    @Test
    fun `replaying twice does not duplicate the trophy`() {
        val twice = replay(replay(finished()).copy(sceneId = replayNodes.first()))
        assertEquals(1, twice.trophies.count { it == "The Watch Goes On" })
    }

    @Test
    fun `a replayed opening no longer stacks route flags from both tellings - changed by C1-2`() {
        // Before C1-2 nothing cleared flags, so opening the door in telling one and walking away in
        // telling two left both route flags set. Every fallen_knight choice now clears the opening family.
        // Full coverage: Chapter1OpeningResetTest.
        val second = Chapter1Paths.pick(replay(finished()), "WALK AWAY")
        assertTrue("walked_away" in second.flags)
        assertFalse("opened_black_door" in second.flags)
    }

    @Test
    fun `a replayed Stonebeard decision no longer inherits the previous telling's flags - changed by C1-1`() {
        // Telling one: helped the dwarf. Telling two: "Move on without a word." Before C1-1 the stale
        // helped_dwarf survived and made a (dead) buckler offer visible. The hold now clears the Stonebeard
        // family, so the second telling starts from its own decision. Full coverage: Chapter1StonebeardResetTest.
        var s = replay(finished())
        s = Chapter1Paths.follow(s, "OPEN THE DOOR", "", "", "Keep", "", "Call out", "Move on without")
        assertEquals("smiths_warning_moved_on", s.sceneId)
        assertFalse("helped_dwarf" in s.flags)
        assertFalse("took_buckler" in s.flags)
        val offered = StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).map { it.label }
        assertEquals(listOf("Heed his warning and go on."), offered)
    }

    @Test
    fun `replay grants the one-time Stonebeard items a second time - tracked as a separate ticket`() {
        val s = Chapter1Paths.follow(replay(finished()), "OPEN THE DOOR", "", "", "Keep", "", "Call out", "Help him", "Take the buckler")
        assertEquals(2, s.inventory.count { it == "dwarven_token" })
        assertEquals(2, s.inventory.count { it == "sturdy_buckler" })
        assertEquals(2, s.inventory.count { it == "healing_draught" })
    }

    @Test
    fun `New Game restart is a full reset, unlike replay`() {
        val store = SaveStore(FakeSharedPreferences())
        store.save(3, finished())
        store.savePreviousState(3, finished())
        val fresh = store.restart(3)
        assertEquals(GameState(activeSlot = 3), fresh)
        assertEquals(GameState(activeSlot = 3), store.load(3))
        assertFalse(store.hasPreviousState(3))
    }
}
