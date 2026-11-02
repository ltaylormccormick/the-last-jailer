package com.thelastjailer.app.ui

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitSlotSave
import com.thelastjailer.app.data.ReplayKit
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.ending.EndingKit
import com.thelastjailer.app.ending.LegacyRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * The Endings section of the Character screen, as far as plain JVM tests can reach it: every rule about when it appears, what
 * it lists, what it must never say, and how the original trophy list is kept apart from it. The composables only draw what
 * this model returns. How they look and sound on a device is checked on a device.
 */
class EndingsPanelTest {
    private val nine = EndingKit.NINE
    private val canonicalOrder = listOf(
        "The Tale Told", "The Unyielding", "The Honourbound", "The Steadfast", "Borrowed Strength", "Unaided",
        "Her Chains", "Her Freedom", "Her Fate Unwritten"
    )

    private fun held(vararg trophies: String) = GameState(trophies = trophies.toSet())
    private fun rows(state: GameState) = EndingsPanel.entries(state).map { it.id }
    private fun store() = SaveStore(FakeSharedPreferences())

    /** An ending reached through the real arrival choice, with the aid and fate answers that lead there. */
    private fun arrivedAt(e: ReplayKit.Ending): GameState {
        val flags = setOf(if (e.aided) "accepted_dark_aid" else "refused_dark_aid_again", e.fate)
        val atEnd = ReplayKit.STRONG.copy(sceneId = "chapter30_end", chapterId = "chapter_30", flags = flags)
        return atEnd.applyChoice(StoryRepository.node("chapter30_end").choices.first { it.nextNodeId == e.nodeId })
    }

    // ---- the approved words ----------------------------------------------------------------------------------

    @Test
    fun `the fixed strings are exactly the approved ones`() {
        assertEquals("ENDINGS", EndingsPanel.TITLE)
        assertEquals("Not every telling of this tale leaves the same mark.", EndingsPanel.EPIGRAPH)
        assertEquals("Your earlier telling is remembered. New marks will appear when another telling reaches its end.", EndingsPanel.EMPTY_STATE)
        assertEquals("The original collection. Ending marks are kept in their own panel below.", EndingsPanel.COLLECTION_NOTE)
        assertEquals("Show a hint", EndingsPanel.HINT_BUTTON)
        assertEquals("Show another hint", EndingsPanel.HINT_BUTTON_AGAIN)
    }

    @Test
    fun `the catalogue is the nine approved trophies in the fixed order, each with its approved sentence`() {
        assertEquals(canonicalOrder, EndingsPanel.catalogue.map { it.id })
        assertEquals(EndingKit.NINE, EndingsPanel.catalogue.map { it.id }.toSet())
        val sentences = EndingsPanel.catalogue.associate { it.id to it.description }
        // Provisional wording pending narrative reconciliation: a change here is deliberate and updates this test with it.
        assertEquals("You carried a telling to its end.", sentences["The Tale Told"])
        assertEquals("Where the road offered a harder way, you took it.", sentences["The Unyielding"])
        assertEquals("You kept faith with what you owed, whatever it cost.", sentences["The Honourbound"])
        assertEquals("You stayed, and let others reach their own answers.", sentences["The Steadfast"])
        assertEquals("At Stonebeard's gate you opened your hand and took what was offered.", sentences["Borrowed Strength"])
        assertEquals("At Stonebeard's gate you kept your hand closed.", sentences["Unaided"])
        assertEquals("You bound her, and brought her back to face the Order.", sentences["Her Chains"])
        assertEquals("You let her go, on one condition.", sentences["Her Freedom"])
        assertEquals("You took her weapon and left the rest for later.", sentences["Her Fate Unwritten"])
    }

    @Test
    fun `the sentences are short, distinct, number-free and never name another trophy`() {
        val all = EndingsPanel.catalogue.map { it.description }
        assertEquals(all.size, all.toSet().size)
        EndingsPanel.catalogue.forEach { e ->
            assertTrue("${e.id} is empty", e.description.isNotBlank())
            assertTrue("${e.id} is ${e.description.length} characters", e.description.length < 72)
            assertTrue("${e.id} contains a digit", e.description.none { it.isDigit() })
            nine.filter { it != e.id }.forEach { other -> assertFalse("${e.id}'s sentence names $other", other in e.description) }
        }
    }

    // ---- when the section appears ------------------------------------------------------------------------------

    @Test
    fun `the section is absent before the first completion`() {
        assertFalse(EndingsPanel.isVisible(GameState()))
        assertFalse(EndingsPanel.isVisible(held("First Blood", "Friend of Stonebeard", "Six, Not One")))
        assertFalse(EndingsPanel.isVisible(held("A Trophy This Build Has Never Heard Of")))
        assertFalse(EndingsPanel.isVisible(ReplayKit.STRONG.copy(sceneId = "the_cost_aided", flags = EndingKit.routeA)))
        assertTrue(EndingsPanel.entries(held("First Blood")).isEmpty())
        assertFalse(EndingsPanel.showEmptyState(held("First Blood")))
        assertNull(EndingsPanel.collectionNote(held("First Blood")))
    }

    @Test
    fun `after arriving at each of the six real endings the section shows exactly the earned rows`() {
        ReplayKit.ENDINGS.forEach { e ->
            val s = arrivedAt(e)
            assertTrue(e.nodeId, EndingsPanel.isVisible(s))
            assertEquals(e.nodeId, canonicalOrder.filter { it in EndingKit.awardsOf(e.nodeId) }, rows(s))
            assertFalse("rows exist, so no explanation", EndingsPanel.showEmptyState(s))
            assertTrue("never a row that was not earned", rows(s).all { it in s.trophies })
        }
    }

    @Test
    fun `a legacy row appears only on the telling that earned it`() {
        val a = playedTo(EndingKit.routeA)
        assertEquals(listOf("The Tale Told", "The Unyielding", "Borrowed Strength", "Her Chains"), rows(a))
        val b = playedTo(EndingKit.routeB)
        assertEquals(listOf("The Tale Told", "The Honourbound", "Unaided", "Her Freedom"), rows(b))
        val c = playedTo(EndingKit.routeC)
        assertEquals(listOf("The Tale Told", "The Steadfast", "Borrowed Strength", "Her Fate Unwritten"), rows(c))
    }

    private fun playedTo(route: Set<String>): GameState {
        val end = EndingKit.playTo(ReplayKit.STRONG, route)
        return end.applyChoice(EndingKit.arrival(end))
    }

    @Test
    fun `Back keeps the rows, replay keeps them and adds only the replay trophy to the original list, a second telling only adds`() {
        val store = store()
        val end = EndingKit.playTo(ReplayKit.STRONG, EndingKit.routeA)
        val arrived = store.commitChoice(end, EndingKit.arrival(end))
        val back = store.restorePreviousState(1)!!
        assertEquals(rows(arrived), rows(back))
        assertTrue(EndingsPanel.isVisible(back))

        val again = store.commitChoice(back, EndingKit.arrival(back))
        val replayed = store.commitChoice(again, ReplayKit.loopChoice(again.sceneId))
        assertTrue("still visible while the second telling begins", EndingsPanel.isVisible(replayed))
        assertEquals(rows(arrived), rows(replayed))
        assertEquals(
            (EndingsPanel.listedTrophies(arrived.trophies) + "The Watch Goes On").toSet(),
            EndingsPanel.listedTrophies(replayed.trophies).toSet()
        )
        assertEquals(EndingsPanel.listedTrophies(arrived.trophies).size + 1, EndingsPanel.listedTrophies(replayed.trophies).size)

        val endB = EndingKit.playTo(replayed, EndingKit.routeB)
        val second = store.commitChoice(endB, EndingKit.arrival(endB))
        assertTrue("every earlier row is still there", rows(second).containsAll(rows(arrived)))
        assertTrue("a second legacy was added", "The Honourbound" in rows(second) && "The Unyielding" in rows(second))
    }

    @Test
    fun `Start Again hides the section and SAVE into another slot shows the copied rows`() {
        val store = store()
        val end = EndingKit.playTo(ReplayKit.STRONG, EndingKit.routeC)
        val arrived = store.commitChoice(end, EndingKit.arrival(end))
        store.save(2, GameState(activeSlot = 2, trophies = setOf("First Blood", "The Unyielding")))
        store.commitSlotSave(2, arrived)
        assertEquals(rows(arrived), rows(store.load(2)!!))
        val wiped = store.commitRestart(1)
        assertFalse(EndingsPanel.isVisible(wiped))
        assertTrue(EndingsPanel.entries(wiped).isEmpty())
    }

    // ---- older saves --------------------------------------------------------------------------------------------

    @Test
    fun `an older save parked on an ending shows the section with the explanation and no rows`() {
        ReplayKit.ENDINGS.forEach { e ->
            val parked = ReplayKit.STRONG.copy(sceneId = e.nodeId, chapterId = "chapter_30", trophies = setOf("First Blood", "Six, Not One"))
            assertTrue(e.nodeId, EndingsPanel.isVisible(parked))
            assertTrue(EndingsPanel.entries(parked).isEmpty())
            assertTrue("the explanation is shown only here", EndingsPanel.showEmptyState(parked))
            assertNotNull(EndingsPanel.collectionNote(parked))
        }
    }

    @Test
    fun `an older save that replayed before the ending trophies existed shows the section with the explanation and no rows`() {
        val replayer = ReplayKit.STRONG.copy(trophies = setOf("The Watch Goes On", "First Blood"))
        assertTrue(EndingsPanel.isVisible(replayer))
        assertTrue(EndingsPanel.entries(replayer).isEmpty())
        assertTrue(EndingsPanel.showEmptyState(replayer))
    }

    @Test
    fun `the explanation appears only while the section is visible and has no rows`() {
        assertFalse("hidden section", EndingsPanel.showEmptyState(held("First Blood")))
        assertFalse("rows present", EndingsPanel.showEmptyState(held("The Tale Told")))
        assertFalse("rows present, replayer", EndingsPanel.showEmptyState(held("The Watch Goes On", "Unaided")))
        assertTrue("visible and empty", EndingsPanel.showEmptyState(held("The Watch Goes On")))
        val finishedThenWiped = held("The Watch Goes On").copy(trophies = emptySet())
        assertFalse(EndingsPanel.showEmptyState(finishedThenWiped))
    }

    // ---- the original collection stays as it was -----------------------------------------------------------------

    @Test
    fun `the original progress calculation and denominator do not move when the nine are held`() {
        val existing = StoryRepository.availableTrophies
        assertEquals(37, existing.size)
        assertTrue(existing.intersect(nine).isEmpty())
        val before = existing.take(12).toSet()
        val after = before + nine
        assertEquals(before.intersect(existing).size, after.intersect(existing).size)
        assertEquals(12, after.intersect(existing).size)
        assertEquals(12 + 9, after.size)
    }

    @Test
    fun `the collection note appears exactly when the section does`() {
        listOf(GameState(), held("First Blood"), held("The Tale Told"), held("The Watch Goes On"), ReplayKit.STRONG.copy(sceneId = ReplayKit.ENDINGS[0].nodeId)).forEach { s ->
            assertEquals(EndingsPanel.isVisible(s), EndingsPanel.collectionNote(s) != null)
        }
    }

    @Test
    fun `the original list loses the nine and nothing else, in the order it had, and no trophy is listed twice or lost`() {
        val pool = StoryRepository.availableTrophies.toList() + nine.toList() + listOf("A Removed Trophy", "Another Old One", "Third Unknown")
        val random = Random(0x37)
        repeat(10_000) {
            val picked = pool.filter { random.nextInt(100) < 45 }.shuffled(kotlin.random.Random(random.nextLong()))
            val trophies = LinkedHashSet(picked)
            val listed = EndingsPanel.listedTrophies(trophies)
            val s = GameState(trophies = trophies)
            assertEquals("same trophies, same order, minus the nine", trophies.filter { it !in nine }, listed)
            val shown = rows(s)
            assertTrue("a nine member listed twice", listed.none { it in nine })
            assertEquals("every held trophy is shown exactly once, in one place", trophies, (listed + shown).toSet())
            assertEquals("the header count is flat list plus Endings rows", trophies.size, listed.size + shown.size)
            assertEquals(listed.size, listed.toSet().size)
            assertTrue(shown.toSet().intersect(listed.toSet()).isEmpty())
        }
    }

    // ---- nothing unearned is ever shown --------------------------------------------------------------------------

    /** Everything the section would put in front of a player, as text. */
    private fun visibleText(s: GameState): List<String> {
        if (!EndingsPanel.isVisible(s)) return emptyList()
        val out = ArrayList<String>()
        out += EndingsPanel.TITLE
        out += EndingsPanel.EPIGRAPH
        EndingsPanel.entries(s).forEach { out += it.id; out += it.description }
        if (EndingsPanel.showEmptyState(s)) out += EndingsPanel.EMPTY_STATE
        out += EndingsPanel.COLLECTION_NOTE
        out += EndingsPanel.HINT_BUTTON
        out += EndingsPanel.HINT_BUTTON_AGAIN
        (1..EndingsPanel.hints.size).forEach { out += EndingsPanel.hintFor(it)!! }
        return out
    }

    @Test
    fun `no unearned name or sentence is ever visible, and nothing visible carries a number`() {
        val random = Random(0xE9D)
        repeat(2_000) {
            val earned = canonicalOrder.filter { random.nextBoolean() }.toSet()
            val s = GameState(trophies = earned + "The Watch Goes On")
            val text = visibleText(s).joinToString("\n")
            EndingsPanel.catalogue.filter { it.id !in earned }.forEach { locked ->
                assertFalse("unearned ${locked.id} is visible", locked.id in text)
                assertFalse("unearned sentence for ${locked.id} is visible", locked.description in text)
            }
            assertTrue("a digit is visible, which could reveal a count: $text", text.none { it.isDigit() })
        }
    }

    @Test
    fun `everything but the earned rows is identical whatever else is or is not held`() {
        val one = GameState(trophies = setOf("The Tale Told"))
        val many = GameState(trophies = setOf("The Tale Told", "The Unyielding", "Unaided", "Her Freedom"))
        fun chrome(s: GameState) = visibleText(s).filter { line -> EndingsPanel.catalogue.none { it.id == line || it.description == line } }
        assertEquals(chrome(one), chrome(many))
        assertEquals(EndingsPanel.hints, (1..3).map { EndingsPanel.hintFor(it) })
    }

    // ---- hints ---------------------------------------------------------------------------------------------------

    /** The chapters that hold a legacy decision scene; every completion has passed every one of them. */
    private val decisionChapters: Set<Int> = listOf(
        "what_the_prisoner_offers_again", "who_stands_with_stonebeard", "old_debts", "what_must_be_broken", "what_the_sixth_wants",
        "what_kaelen_commits_to", "reaching_emberlow", "reaching_greymoor", "what_yielding_cost", "what_kaelen_offers_the_forgotten",
        "what_wraithspire_reveals", "what_must_happen_next", "what_kaelen_offers_at_the_end", "what_kaelen_chooses_trusted"
    ).map { id -> StoryRepository.chapters.first { it.id == StoryRepository.node(id).chapterId }.number }.toSet()

    @Test
    fun `there are exactly the three approved hints`() {
        assertEquals(
            listOf(
                "A turning point lies in Chapter IV.",
                "A turning point lies in Chapter XVIII.",
                "The closing chapters, XXI to XXX, leave their own mark."
            ),
            EndingsPanel.hints
        )
    }

    @Test
    fun `a hint names only chapters that exist and that hold a decision every completion has passed`() {
        val byRoman = (1..30).associate { EndingsPanel.romanNumeral(it) to it }
        EndingsPanel.hints.forEach { hint ->
            val named = Regex("""\b[IVXL]+\b""").findAll(hint).map { it.value }.toList()
            assertTrue("$hint names no chapter", named.isNotEmpty())
            named.forEach { r ->
                val n = byRoman[r]
                assertNotNull("$hint names a chapter that does not exist: $r", n)
                assertTrue("$hint names chapter $n, which holds no legacy decision", n in decisionChapters)
            }
        }
    }

    @Test
    fun `a hint never carries a trophy name, a choice, an answer, an outcome or a number`() {
        val flagWords = LegacyRule.decisions.flatMap { d -> d.options.map { it.flag } }
        val banned = nine.toList() + flagWords + flagWords.map { it.replace('_', ' ') } +
            listOf("bound", "chains", "free", "freedom", "unwritten", "aid", "unaided", "hand", "strength", "mercy", "captured",
                "deferred", "refused", "accepted", "spared", "ilsevet", "voss", "halvard", "courage", "honour", "steadfast", "unyielding")
        EndingsPanel.hints.forEach { hint ->
            val lower = hint.lowercase()
            banned.forEach { word -> assertFalse("hint \"$hint\" contains \"$word\"", Regex("\\b" + Regex.escape(word.lowercase()) + "\\b").containsMatchIn(lower)) }
            assertTrue(hint.none { it.isDigit() })
        }
    }

    @Test
    fun `hints cycle deterministically from the tap count alone`() {
        assertNull(EndingsPanel.hintFor(0))
        assertNull(EndingsPanel.hintFor(-3))
        assertEquals(EndingsPanel.hints[0], EndingsPanel.hintFor(1))
        assertEquals(EndingsPanel.hints[1], EndingsPanel.hintFor(2))
        assertEquals(EndingsPanel.hints[2], EndingsPanel.hintFor(3))
        assertEquals(EndingsPanel.hints[0], EndingsPanel.hintFor(4))
        (1..30).forEach { assertEquals(EndingsPanel.hintFor(it), EndingsPanel.hintFor(it)) }
    }

    @Test
    fun `chapter numerals are the ones the story's own chapter titles use`() {
        StoryRepository.chapters.forEach { c ->
            assertTrue("${c.title} should start with Chapter ${EndingsPanel.romanNumeral(c.number)}", c.title.startsWith("Chapter " + EndingsPanel.romanNumeral(c.number) + " "))
        }
        assertEquals("IV", EndingsPanel.romanNumeral(4))
        assertEquals("XVIII", EndingsPanel.romanNumeral(18))
        assertEquals("XXIX", EndingsPanel.romanNumeral(29))
        assertEquals("XXX", EndingsPanel.romanNumeral(30))
    }
}
