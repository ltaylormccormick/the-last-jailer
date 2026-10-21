package com.thelastjailer.app

import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R1, the ending ledger. Reaching an ending is recorded on the one choice that ARRIVES at it (the
 * "Rest, for now." choices on `chapter30_end`), not on "Begin the tale again":
 *  - a reversible flag `ledger_end_N` (ordinary story state, undone by Back like any flag), and
 *  - one permanent, distinctly named trophy per ending (kept by Back, so it is the durable record of discovery).
 * The shared trophy "The Watch Goes On" and every ending scene and target are unchanged.
 */
class EndingLedgerTest {
    private class Ending(val node: String, val flag: String, val trophy: String, val aided: Boolean, val fate: String)

    private val endings = listOf(
        Ending("the_watch_continues_dark_aid_captured", "ledger_end_1", "Ending: Dark Aid, Ilsevet Captured", true, "captured_ilsevet"),
        Ending("the_watch_continues_dark_aid_mercy", "ledger_end_2", "Ending: Dark Aid, Ilsevet Shown Mercy", true, "showed_ilsevet_mercy"),
        Ending("the_watch_continues_dark_aid_deferred", "ledger_end_3", "Ending: Dark Aid, Ilsevet's Fate Deferred", true, "deferred_ilsevets_fate"),
        Ending("the_watch_continues_unaided_captured", "ledger_end_4", "Ending: Unaided, Ilsevet Captured", false, "captured_ilsevet"),
        Ending("the_watch_continues_unaided_mercy", "ledger_end_5", "Ending: Unaided, Ilsevet Shown Mercy", false, "showed_ilsevet_mercy"),
        Ending("the_watch_continues_unaided_deferred", "ledger_end_6", "Ending: Unaided, Ilsevet's Fate Deferred", false, "deferred_ilsevets_fate")
    )

    private val prefs = FakeSharedPreferences()
    private val store = SaveStore(prefs)

    private fun atChapter30End(e: Ending, extraTrophies: Set<String> = emptySet(), extraFlags: Set<String> = emptySet()): GameState =
        GameState(
            activeSlot = 1,
            sceneId = "chapter30_end",
            trophies = extraTrophies,
            flags = buildSet { add(e.fate); if (e.aided) add("accepted_dark_aid"); addAll(extraFlags) }
        ).withSceneChapter()

    private fun arrival(state: GameState): Choice =
        StoryRepository.visibleChoices(StoryRepository.node("chapter30_end"), state).single()

    private fun replayChoice(nodeId: String): Choice =
        StoryRepository.node(nodeId).choices.single { it.nextNodeId == "fallen_knight" }

    // ---- the story data ----------------------------------------------------------------------------------

    @Test
    fun `each ending has exactly one arrival choice and it records that ending`() {
        endings.forEach { e ->
            val inbound = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
                .flatMap { n -> n.choices.filter { it.nextNodeId == e.node } }
            assertEquals(e.node, 1, inbound.size)
            assertEquals(e.node, setOf(e.flag), inbound.single().consequences.setFlags)
            assertEquals(e.node, e.trophy, inbound.single().consequences.unlockTrophy)
            assertTrue(inbound.single().consequences.clearFlags.isEmpty())
        }
    }

    @Test
    fun `only the six arrival choices set a ledger flag or unlock an ending trophy`() {
        val allChoices = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.flatMap { it.choices }
        val ledgerSetters = allChoices.filter { c -> c.consequences.setFlags.any { it.startsWith("ledger_end_") } }
        assertEquals(6, ledgerSetters.size)
        assertEquals(endings.map { it.node }.toSet(), ledgerSetters.map { it.nextNodeId }.toSet())
        val endingTrophyUnlocks = allChoices.filter { it.consequences.unlockTrophy?.startsWith("Ending: ") == true }
        assertEquals(6, endingTrophyUnlocks.size)
    }

    @Test
    fun `ledger flags are never cleared by any choice`() {
        val cleared = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
            .flatMap { n -> n.choices.flatMap { it.consequences.clearFlags } }
        assertTrue(cleared.none { it.startsWith("ledger_end_") })
    }

    @Test
    fun `ending trophies are distinct, permanent names and are all available`() {
        assertEquals(6, endings.map { it.trophy }.toSet().size)
        assertTrue(endings.all { it.trophy in StoryRepository.availableTrophies })
        assertFalse("The Watch Goes On" in endings.map { it.trophy })
        assertTrue("The Watch Goes On" in StoryRepository.availableTrophies)
    }

    @Test
    fun `the six ending scenes, their targets and the shared replay trophy are unchanged`() {
        endings.forEach { e ->
            val node = StoryRepository.node(e.node)
            assertTrue(StoryRepository.isEnding(node))
            val replay = replayChoice(e.node)
            assertEquals("The Watch Goes On", replay.consequences.unlockTrophy)
            assertTrue(replay.consequences.setFlags.isEmpty())
            assertTrue(replay.consequences.clearFlags.isEmpty())
        }
        val arrivals = StoryRepository.node("chapter30_end").choices
        assertEquals(endings.map { it.node }, arrivals.map { it.nextNodeId })
        assertTrue(arrivals.all { it.label == "Rest, for now." })
    }

    @Test
    fun `every flag combination at chapter30_end offers exactly the arrival for its own ending`() {
        endings.forEach { e ->
            val choice = arrival(atChapter30End(e))
            assertEquals(e.node, choice.nextNodeId)
        }
    }

    // ---- arrival, persistence, Back ------------------------------------------------------------------------

    @Test
    fun `arriving records the flag and the trophy and persists them`() {
        endings.forEach { e ->
            val s = SaveStore(FakeSharedPreferences())
            val before = atChapter30End(e)
            val after = s.commitChoice(before, arrival(before))
            assertEquals(e.node, after.sceneId)
            assertTrue(e.flag in after.flags)
            assertTrue(e.trophy in after.trophies)
            assertEquals(after, s.load(1))
            assertEquals(1, after.flags.count { it.startsWith("ledger_end_") })
            assertEquals(1, after.trophies.count { it.startsWith("Ending: ") })
        }
    }

    @Test
    fun `a record survives closing and reopening the game`() {
        val e = endings[3]
        val before = atChapter30End(e)
        store.commitChoice(before, arrival(before))
        val reopened = SaveStore(prefs).load(1)!!
        assertEquals(e.node, reopened.sceneId)
        assertTrue(e.flag in reopened.flags && e.trophy in reopened.trophies)
    }

    @Test
    fun `Back after arrival undoes the flag but keeps the trophy, and arriving again does not duplicate it`() {
        endings.forEach { e ->
            val s = SaveStore(FakeSharedPreferences())
            val before = atChapter30End(e)
            s.commitChoice(before, arrival(before))
            val back = s.restorePreviousState(1)!!
            assertEquals("chapter30_end", back.sceneId)
            assertFalse(e.flag in back.flags)
            assertTrue("discovery stays credited by the trophy", e.trophy in back.trophies)
            assertEquals(back, s.load(1))
            val again = s.commitChoice(back, arrival(back))
            assertTrue(e.flag in again.flags)
            assertEquals(1, again.trophies.count { it == e.trophy })
        }
    }

    @Test
    fun `replay keeps the ledger and every ending trophy and earns the shared trophy once`() {
        val e = endings[0]
        val before = atChapter30End(e)
        val onEnding = store.commitChoice(before, arrival(before))
        val replayed = store.commitChoice(onEnding, replayChoice(e.node))
        assertEquals("fallen_knight", replayed.sceneId)
        assertTrue(e.flag in replayed.flags)
        assertTrue(e.trophy in replayed.trophies)
        assertEquals(1, replayed.trophies.count { it == "The Watch Goes On" })
    }

    @Test
    fun `all six endings can be discovered in one slot and none is lost on replay`() {
        var state = GameState(activeSlot = 1)
        endings.forEach { e ->
            state = atChapter30End(e, extraTrophies = state.trophies, extraFlags = state.flags.filter { it.startsWith("ledger_end_") }.toSet())
            state = store.commitChoice(state, arrival(state))
            state = store.commitChoice(state, replayChoice(e.node))
        }
        assertEquals(endings.map { it.flag }.toSet(), state.flags.filter { it.startsWith("ledger_end_") }.toSet())
        assertTrue(endings.all { it.trophy in state.trophies })
    }

    @Test
    fun `slots are independent and Start Again clears the record like any other trophy`() {
        val e = endings[1]
        val before = atChapter30End(e)
        store.commitChoice(before, arrival(before))
        assertNull(store.load(2))
        val fresh = store.restart(1)
        assertTrue(fresh.trophies.isEmpty() && fresh.flags.isEmpty())
    }

    // ---- compatibility with older saves ------------------------------------------------------------------

    @Test
    fun `an older save at chapter30_end with no record earns it on arrival`() {
        val e = endings[4]
        val old = atChapter30End(e, extraTrophies = setOf("Six, Not One", "The Watch Goes On"))
        store.save(1, old)
        val loaded = store.load(1)!!
        val after = store.commitChoice(loaded, arrival(loaded))
        assertTrue(e.flag in after.flags)
        assertTrue(after.trophies.containsAll(setOf("Six, Not One", "The Watch Goes On", e.trophy)))
    }

    @Test
    fun `an older save already standing on an ending scene loads and replays normally`() {
        val e = endings[2]
        val old = GameState(
            activeSlot = 1, sceneId = e.node,
            flags = setOf("accepted_dark_aid", e.fate), trophies = setOf("Six, Not One")
        ).withSceneChapter()
        store.save(1, old)
        val loaded = store.load(1)!!
        assertEquals(old, loaded)
        val replayed = store.commitChoice(loaded, replayChoice(e.node))
        assertEquals("fallen_knight", replayed.sceneId)
        assertTrue("The Watch Goes On" in replayed.trophies)
        assertFalse(replayed.flags.any { it.startsWith("ledger_end_") })
    }

    @Test
    fun `a save carrying ledger flags and ending trophies round-trips unchanged`() {
        val state = GameState(
            activeSlot = 1, sceneId = "fallen_knight",
            flags = setOf("ledger_end_1", "ledger_end_6", "accepted_dark_aid"),
            trophies = endings.map { it.trophy }.toSet() + "The Watch Goes On"
        )
        store.save(1, state)
        assertEquals(state, SaveStore(prefs).load(1))
        assertNotNull(SaveStore(prefs).load(1))
    }
}
