package com.thelastjailer.app

import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 2B-3: Back reverses a choice but never takes back a trophy the player has earned. Everything else
 * reversible returns exactly as it was; Start Again, other slots and the save format are unchanged.
 */
class TrophyBackTest {
    private val prefs = FakeSharedPreferences()
    private val store = SaveStore(prefs)

    private fun choiceWithTrophy(trophy: String?, next: String = "the_road_away") = Choice(
        label = "go",
        nextNodeId = next,
        consequences = Consequences(
            statDeltas = mapOf(StatType.GOLD to 10),
            grantItemIds = listOf("sturdy_buckler"),
            setFlags = setOf("abandoned_branch"),
            unlockTrophy = trophy
        )
    )

    // ---- the pure helper ---------------------------------------------------------------------------------

    @Test
    fun `the merge is a union that never loses either side`() {
        assertEquals(setOf("a", "b", "c"), mergeEarnedTrophies(setOf("a", "b"), setOf("b", "c")))
        assertEquals(setOf("a"), mergeEarnedTrophies(setOf("a"), emptySet()))
        assertEquals(setOf("c"), mergeEarnedTrophies(emptySet(), setOf("c")))
        assertEquals(emptySet<String>(), mergeEarnedTrophies(emptySet(), emptySet()))
    }

    @Test
    fun `the merge is idempotent and does not mutate its inputs`() {
        val restored = setOf("a")
        val live = setOf("a", "b")
        val once = mergeEarnedTrophies(restored, live)
        assertEquals(once, mergeEarnedTrophies(once, live))
        assertEquals(setOf("a"), restored)
        assertEquals(setOf("a", "b"), live)
    }

    @Test
    fun `the merge never invents a trophy that is in neither set`() {
        val result = mergeEarnedTrophies(setOf("a", "b"), setOf("b", "c"))
        assertTrue(result.all { it in setOf("a", "b", "c") })
    }

    // ---- Back keeps earned trophies, reverses everything else ---------------------------------------------

    @Test
    fun `Back after a trophy choice keeps the trophy and returns every reversible thing exactly`() {
        val before = GameState(
            activeSlot = 1, sceneId = "fallen_knight", gold = 25, honour = 1,
            inventory = listOf("healing_draught"), flags = setOf("a"), trophies = setOf("T")
        )
        store.commitChoice(before, choiceWithTrophy("New"))

        val restored = store.restorePreviousState(1)!!

        assertEquals(before.copy(chapterId = restored.chapterId, trophies = setOf("T", "New")), restored)
        assertFalse("abandoned flag" , "abandoned_branch" in restored.flags)
        assertFalse("abandoned item", "sturdy_buckler" in restored.inventory)
        assertEquals(25, restored.gold)
        assertEquals("fallen_knight", restored.sceneId)
        assertEquals(restored, store.load(1))
        assertFalse(store.hasPreviousState(1))
    }

    @Test
    fun `every choice in the story that awards a trophy keeps it through Back`() {
        val sites = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
            .flatMap { node -> node.choices.filter { it.consequences.unlockTrophy != null }.map { node to it } }
        // R1 (ending ledger) deliberately adds six sites: one distinct trophy on each ending's arrival choice. The
        // loop below then proves that every one of them survives Back, which is what makes the trophy the durable
        // record of ending discovery.
        assertEquals("the eighteen story-choice trophy sites", 18, sites.size)
        assertEquals(setOf("Friend of Stonebeard", "Blood and Silver", "The Last Jailer", "The Watch Goes On",
            "Ending: Dark Aid, Ilsevet Captured", "Ending: Dark Aid, Ilsevet Shown Mercy",
            "Ending: Dark Aid, Ilsevet's Fate Deferred", "Ending: Unaided, Ilsevet Captured",
            "Ending: Unaided, Ilsevet Shown Mercy", "Ending: Unaided, Ilsevet's Fate Deferred"),
            sites.map { it.second.consequences.unlockTrophy }.toSet())

        for ((node, choice) in sites) {
            val s = SaveStore(FakeSharedPreferences())
            val before = GameState(activeSlot = 1, sceneId = node.id, gold = 40, trophies = setOf("Earlier")).withSceneChapter()
            val after = s.commitChoice(before, choice)
            assertTrue("${node.id}: awarded", choice.consequences.unlockTrophy in after.trophies)

            val restored = s.restorePreviousState(1)!!

            val label = "${node.id} -> ${choice.nextNodeId}"
            assertEquals(label, node.id, restored.sceneId)
            assertTrue(label, choice.consequences.unlockTrophy in restored.trophies)
            assertTrue(label, "Earlier" in restored.trophies)
            assertEquals(label, before.copy(trophies = restored.trophies), restored)
        }
    }

    @Test
    fun `choosing the same trophy choice again after Back does not duplicate anything`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 0)
        val choice = choiceWithTrophy("New")
        store.commitChoice(before, choice)
        val restored = store.restorePreviousState(1)!!
        assertEquals(setOf("New"), restored.trophies)

        val again = store.commitChoice(restored, choice)

        assertEquals(setOf("New"), again.trophies)
        assertEquals("the reward is paid once", 10, again.gold)
        assertEquals(1, again.inventory.count { it == "sturdy_buckler" })
    }

    @Test
    fun `a different choice after Back drops the abandoned branch but keeps the earned trophy`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 5)
        store.commitChoice(before, choiceWithTrophy("New"))
        val restored = store.restorePreviousState(1)!!

        val other = store.commitChoice(restored, Choice("other", "somewhere_else", consequences = Consequences(setFlags = setOf("other_branch"))))

        assertEquals(setOf("New"), other.trophies)
        assertTrue("other_branch" in other.flags)
        assertFalse("abandoned_branch" in other.flags)
        assertEquals(5, other.gold)
    }

    @Test
    fun `trophies the player already held are untouched by Back`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", trophies = setOf("A", "B"))
        store.commitChoice(before, choiceWithTrophy(null))
        assertEquals(setOf("A", "B"), store.restorePreviousState(1)!!.trophies)
    }

    @Test
    fun `Back is unavailable after a fight, so a combat trophy is never in question`() {
        val encounter = CombatEncounter(
            id = "e", enemyId = "x", victoryNodeId = "after_victory", defeatNodeId = "after_defeat",
            xpReward = 10, goldReward = 5, unlockTrophy = "Fight Trophy"
        )
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight")
        store.commitChoice(before, choiceWithTrophy(null))
        val after = store.commitCombatResult(before.copy(sceneId = "the_road_away"), encounter, CombatOutcome(true, 0, emptyList()))

        assertTrue("Fight Trophy" in after.trophies)
        assertFalse(store.hasPreviousState(1))
        assertNull(store.restorePreviousState(1))
        assertEquals(after, store.load(1))
    }

    // ---- Chapter 30 --------------------------------------------------------------------------------------

    @Test
    fun `Back across the Chapter 30 loop keeps The Watch Goes On and does not award it twice`() {
        val node = StoryRepository.nodesInChapter("chapter_30").first { n -> n.choices.any { it.nextNodeId == "fallen_knight" } }
        val loop = node.choices.first { it.nextNodeId == "fallen_knight" }
        val ending = GameState(activeSlot = 1, sceneId = node.id, trophies = setOf("Six, Not One")).withSceneChapter()

        store.commitChoice(ending, loop)
        val back = store.restorePreviousState(1)!!

        assertEquals(node.id, back.sceneId)
        assertEquals(setOf("Six, Not One", "The Watch Goes On"), back.trophies)

        val again = store.commitChoice(back, loop)
        assertEquals(setOf("Six, Not One", "The Watch Goes On"), again.trophies)
    }

    // ---- restart, other slots, format -------------------------------------------------------------------------

    @Test
    fun `Start Again still wipes trophies and leaves no Back snapshot to resurrect them`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", trophies = setOf("Old"))
        store.commitChoice(before, choiceWithTrophy("New"))

        val fresh = store.commitRestart(1)

        assertTrue(fresh.trophies.isEmpty())
        assertTrue(store.load(1)!!.trophies.isEmpty())
        assertFalse(store.hasPreviousState(1))
        assertNull(store.restorePreviousState(1))
    }

    @Test
    fun `Back on one slot never reads or changes another slot`() {
        store.save(2, GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("Slot Two Only")))
        val slotTwoBefore = store.load(2)
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", trophies = setOf("A"))
        store.commitChoice(before, choiceWithTrophy("New"))

        val restored = store.restorePreviousState(1)!!

        assertEquals(setOf("A", "New"), restored.trophies)
        assertFalse("Slot Two Only" in restored.trophies)
        assertEquals(slotTwoBefore, store.load(2))
    }

    @Test
    fun `SAVE into another slot keeps its existing behaviour (the destination's trophies are replaced)`() {
        store.save(2, GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("Destination Old")))
        val source = GameState(activeSlot = 1, sceneId = "the_road_away", trophies = setOf("Source"))

        val saved = store.commitSlotSave(2, source)

        assertEquals(setOf("Source"), saved.trophies)
        assertEquals(setOf("Source"), store.load(2)!!.trophies)
    }

    @Test
    fun `Back writes no key that a Back did not write before`() {
        val before = GameState(activeSlot = 1, sceneId = "fallen_knight", trophies = setOf("A"))
        store.commitChoice(before, choiceWithTrophy("New"))
        val keysBefore = prefs.all.keys.toSet()

        store.restorePreviousState(1)

        val keysAfter = prefs.all.keys.toSet()
        assertTrue("only keys that already existed", keysBefore.containsAll(keysAfter))
        assertTrue(keysAfter.none { it.contains("earned") })
    }

    // ---- older saves --------------------------------------------------------------------------------------------

    @Test
    fun `an older save with no trophies key at all goes Back without trouble`() {
        prefs.edit()
            .putString("1.scene", "the_road_away").putString("1.chapter", "chapter_1")
            .putString("1.previous.scene", "fallen_knight").putString("1.previous.chapter", "chapter_1")
            .apply()

        val restored = store.restorePreviousState(1)!!

        assertEquals("fallen_knight", restored.sceneId)
        assertTrue(restored.trophies.isEmpty())
    }

    @Test
    fun `an older Back snapshot is merged with what the slot holds, and unknown trophy names are kept`() {
        prefs.edit()
            .putString("1.scene", "the_road_away").putString("1.chapter", "chapter_1")
            .putStringSet("1.trophies", setOf("Removed In A Later Update", "First Blood"))
            .putString("1.previous.scene", "fallen_knight").putString("1.previous.chapter", "chapter_1")
            .putStringSet("1.previous.trophies", setOf("First Blood"))
            .apply()

        val restored = store.restorePreviousState(1)!!

        assertEquals(setOf("Removed In A Later Update", "First Blood"), restored.trophies)
        assertEquals("fallen_knight", restored.sceneId)
    }

    @Test
    fun `a trophy only the snapshot holds is kept as well - Back is a union, not a copy of the live set`() {
        prefs.edit()
            .putString("1.scene", "the_road_away").putString("1.chapter", "chapter_1")
            .putStringSet("1.trophies", setOf("B", "C"))
            .putString("1.previous.scene", "fallen_knight").putString("1.previous.chapter", "chapter_1")
            .putStringSet("1.previous.trophies", setOf("A", "B"))
            .apply()

        assertEquals(setOf("A", "B", "C"), store.restorePreviousState(1)!!.trophies)
    }

    @Test
    fun `an unreadable live state falls back to plain Back`() {
        prefs.edit()
            .putString("1.previous.scene", "fallen_knight").putString("1.previous.chapter", "chapter_1")
            .putStringSet("1.previous.trophies", setOf("A"))
            .apply()
        assertNull(store.load(1))

        assertEquals(setOf("A"), store.restorePreviousState(1)!!.trophies)
    }

    @Test
    fun `Back with nothing to go back to changes nothing`() {
        store.save(1, GameState(activeSlot = 1, sceneId = "fallen_knight", trophies = setOf("A")))
        assertNull(store.restorePreviousState(1))
        assertEquals(setOf("A"), store.load(1)!!.trophies)
    }
}
