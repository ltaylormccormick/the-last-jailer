package com.thelastjailer.app

import com.thelastjailer.app.data.C21_TELLING_FLAGS
import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.ReplayKit
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity for Chapters XXI to XXX: the two opening choices of Chapter XXI clear every flag the later
 * chapters set, so a replay never carries last telling's answers into this one, and nothing else moves.
 *
 * Chapter XIX and XX flags, the aid and fate answers, stats, health, gold, level, XP, items and trophies are
 * checked untouched, on one choice and on whole consecutive tellings walked on the real story graph.
 */
class Chapter21ReplayResetTest {

    private val entry = "the_prisoner_answers"

    /** Set in Chapters XIX and XX, before the reset runs. They must survive it, and every later telling. */
    private val chapters1920 = listOf(
        "pressed_about_the_archive", "let_her_finish", "defied_the_prophecy", "claimed_his_own_meaning",
        "left_it_unanswered", "went_to_workshop_alone", "brought_voss_to_workshop", "lost_to_the_ghostwriter",
        "demanded_the_truth", "gave_the_prisoner_time", "waited_in_silence"
    )

    /** Owned by other systems, or unknown to the story: the reset must leave them alone. */
    private val keep = setOf("halvard_died", "earned_voss_seal", "c7_rung_trusted", "unknown_future_flag")

    /** The decisions every telling takes exactly once between Chapter XXI and the epilogue. */
    private val decisions: List<Set<String>> = listOf(
        setOf("vowed_to_protect_the_prisoner", "affirmed_its_own_choice", "left_the_question_open"),
        setOf("asked_directly_ch22", "offered_patience_ch22"),
        setOf("vowed_constant_vigilance", "vowed_to_share_the_burden", "simply_stayed"),
        setOf("refused_to_leave_emberlow", "insisted_on_real_choice", "waited_at_emberlow"),
        setOf("reminded_greymoor_of_choice", "offered_presence_not_replacement", "sat_with_greymoors_silence"),
        setOf("vowed_to_hold_duskmere", "sought_to_remember_wren", "secured_duskmere_in_silence"),
        setOf("pressed_the_forgotten_to_remember", "respected_the_forgetting", "stepped_back_from_the_sundering"),
        setOf("offered_the_vigil_alliance", "respected_the_vigils_pace", "simply_bore_witness_at_wraithspire"),
        setOf("stood_defiant_before_the_whole", "asked_what_it_needed_before"),
        setOf("chose_to_seek_the_ending", "insisted_on_another_way"),
        setOf("demanded_it_be_earned", "offered_connection_without_merging", "simply_stayed_at_the_end")
    )

    private val baseItems = listOf("healing_draught", "promise_of_the_six", "voss_seal")
    private val baseTrophies = setOf("Six, Not One", "The Last Gate")

    private fun atEntry(flags: Set<String>): GameState = ReplayKit.jumpTo(
        ReplayKit.STRONG.copy(flags = flags, level = 4, xp = 37, gold = 120, inventory = baseItems, trophies = baseTrophies),
        entry
    )

    private val entryChoices: List<Choice> get() = StoryRepository.node(entry).choices

    /** The same choice with its clears removed: how it behaved before the reset. */
    private fun Choice.withoutClears(): Choice = copy(consequences = consequences.copy(clearFlags = emptySet()))

    private val base: GameState get() = ReplayKit.STRONG.copy(flags = keep, inventory = baseItems, trophies = baseTrophies)

    // ---- the data ------------------------------------------------------------------------------------------

    @Test
    fun `the reset set is exactly the flags first set in Chapters 21 to 30`() {
        fun setIn(chapters: IntRange): Set<String> = chapters.flatMap { n ->
            StoryRepository.nodesInChapter("chapter_$n").flatMap { node -> node.choices.flatMap { it.consequences.setFlags } }
        }.toSet()
        val late = setIn(21..30)
        val earlier = setIn(1..20)
        assertTrue("set both before and after Chapter 21: ${late.intersect(earlier)}", late.intersect(earlier).isEmpty())
        assertEquals(late, C21_TELLING_FLAGS)
        assertEquals(62, C21_TELLING_FLAGS.size)
        assertTrue(C21_TELLING_FLAGS.intersect(chapters1920.toSet()).isEmpty())
    }

    @Test
    fun `both Chapter 21 choices clear all of it and the flag they set is in the set`() {
        assertEquals(2, entryChoices.size)
        entryChoices.forEach { c ->
            assertEquals(c.label, C21_TELLING_FLAGS, c.consequences.clearFlags)
            assertEquals(c.label, 1, c.consequences.setFlags.size)
            assertTrue(c.label, c.consequences.setFlags.single() in C21_TELLING_FLAGS)
        }
    }

    // ---- one choice, on a state that holds everything --------------------------------------------------------

    @Test
    fun `the new flag survives the reset because clear runs before set, and nothing else from the set does`() {
        entryChoices.forEach { c ->
            val own = c.consequences.setFlags.single()
            val held = atEntry(C21_TELLING_FLAGS + chapters1920 + keep).applyChoice(c)
            assertEquals(c.label, chapters1920.toSet() + keep + own, held.flags)
            assertTrue(c.label, own in atEntry(emptySet()).applyChoice(c).flags)
        }
    }

    @Test
    fun `on a first play the reset changes nothing`() {
        entryChoices.forEach { c ->
            for (held in listOf(emptySet<String>(), chapters1920.toSet() + keep)) {
                val s = atEntry(held)
                assertEquals(c.label, s.applyChoice(c.withoutClears()), s.applyChoice(c))
            }
        }
    }

    @Test
    fun `stats, health, gold, level, xp, inventory, trophies and position are exactly as without the reset`() {
        entryChoices.forEach { c ->
            val s = atEntry(C21_TELLING_FLAGS + chapters1920 + keep)
            val corrected = s.applyChoice(c)
            val before = s.applyChoice(c.withoutClears())
            assertEquals(c.label, before.copy(flags = emptySet()), corrected.copy(flags = emptySet()))
            assertTrue(c.label, (corrected.flags - before.flags).isEmpty())
        }
    }

    // ---- no bypass -------------------------------------------------------------------------------------------

    private fun successors(node: StoryNode): List<String> {
        val viaChoices = node.choices.map { it.nextNodeId }
        val viaFight = node.combatEncounterId?.let { id ->
            val encounter = CombatRepository.encounter(id)
            listOfNotNull(encounter.victoryNodeId, encounter.defeatNodeId)
        } ?: emptyList()
        return viaChoices + viaFight
    }

    @Test
    fun `chapter 22 cannot be reached without passing the Chapter 21 entry`() {
        val seen = HashSet<String>()
        val pending = ArrayList<String>()
        pending += StoryRepository.chapters.minByOrNull { it.number }!!.startNodeId
        while (pending.isNotEmpty()) {
            val id = pending.removeAt(pending.size - 1)
            if (id == entry || !seen.add(id)) continue
            pending += successors(StoryRepository.nodeOrNull(id) ?: continue)
        }
        val numbers = StoryRepository.chapters.associate { it.id to it.number }
        val furthest = seen.maxOf { numbers.getValue(StoryRepository.node(it).chapterId) }
        assertTrue("reached chapter $furthest without the Chapter 21 entry", furthest <= 20)
    }

    // ---- consecutive tellings on the real graph ----------------------------------------------------------------

    private class Pick(val sceneId: String, val visible: List<Choice>, val taken: Choice) {
        val index: Int get() = visible.indexOfFirst { it === taken }
    }

    private class Telling(val from: GameState, val end: GameState, val picks: List<Pick>) {
        val chosen: List<Choice> get() = picks.map { it.taken }
    }

    /** One telling: aid and fate answered with the real choices, [earlier] Chapter 19-20 flags added, then Chapter 21 to the epilogue. */
    private fun tell(from: GameState, ending: ReplayKit.Ending, earlier: Set<String>, choose: (List<Choice>) -> Choice): Telling {
        val decided = ReplayKit.tellingByDecisions(from, ending)
        val start = ReplayKit.jumpTo(decided.copy(flags = decided.flags + earlier), entry)
        val picks = ArrayList<Pick>()
        val end = ReplayKit.play(
            start,
            stop = { it.sceneId == "chapter30_end" },
            pick = { node, visible -> choose(visible).also { picks += Pick(node.id, visible, it) } }
        )
        return Telling(from, end, picks)
    }

    private val first: (List<Choice>) -> Choice = { it.first() }
    private val last: (List<Choice>) -> Choice = { it.last() }
    private val middle: (List<Choice>) -> Choice = { it[it.size / 2] }

    private fun assertJudgedOnItsOwnAnswers(label: String, t: Telling, ending: ReplayKit.Ending, earlierSoFar: Set<String>) {
        val answeredNow = t.chosen.flatMap { it.consequences.setFlags }.toSet()
        assertEquals("$label: nothing from an earlier telling survives", answeredNow.intersect(C21_TELLING_FLAGS), t.end.flags.intersect(C21_TELLING_FLAGS))
        decisions.forEach { g -> assertEquals("$label: $g", 1, t.end.flags.count { it in g }) }
        assertEquals("$label: epilogue", ending.nodeId, ReplayKit.arrivals(t.end).single().nextNodeId)
        assertEquals("$label: road back", 1, StoryRepository.visibleChoices(StoryRepository.node("the_road_back_to_the_sundering_ground"), t.end).size)
        assertEquals("$label: chapter 28", 1, StoryRepository.visibleChoices(StoryRepository.node("chapter28_end"), t.end).size)
        assertTrue("$label: Chapter 19-20 flags kept", t.end.flags.containsAll(earlierSoFar))
        assertTrue("$label: other flags kept", t.end.flags.containsAll(keep))
        assertTrue("$label: items kept", t.end.inventory.containsAll(t.from.inventory))
        assertTrue("$label: trophies kept", t.end.trophies.containsAll(t.from.trophies))
        assertTrue("$label: level never falls", t.end.level >= t.from.level)
    }

    @Test
    fun `first, middle and last really take the first, middle and last available choice at every scene with a choice`() {
        val ending = ReplayKit.ENDINGS[1]
        val runs = listOf("first" to first, "middle" to middle, "last" to last).map { (name, f) -> name to tell(base, ending, emptySet(), f) }
        runs.forEach { (name, t) ->
            val branching = t.picks.filter { it.visible.size > 1 }
            assertTrue("$name met only ${branching.size} scenes with a choice", branching.size >= decisions.size)
            branching.forEach { p ->
                val expected = when (name) {
                    "first" -> 0
                    "last" -> p.visible.size - 1
                    else -> p.visible.size / 2
                }
                assertEquals("$name at ${p.sceneId}", expected, p.index)
            }
            decisions.forEach { g ->
                val answeredAt = t.picks.filter { p -> p.taken.consequences.setFlags.any { it in g } }
                assertEquals("$name: $g answered once", 1, answeredAt.size)
                val offered = answeredAt.single().visible.flatMap { it.consequences.setFlags }.filter { it in g }.toSet()
                assertEquals("$name: every alternative of $g was on offer", g, offered)
            }
        }
        decisions.forEach { g ->
            val answers = runs.map { (_, t) -> t.end.flags.single { it in g } }
            assertEquals("first, middle and last answered $g differently", minOf(3, g.size), answers.toSet().size)
        }
    }

    @Test
    fun `two consecutive tellings with opposite decisions each hold only their own answers`() {
        val a = ReplayKit.ENDINGS[0]   // took the dark aid, bound Ilsevet
        val b = ReplayKit.ENDINGS[5]   // kept his hand closed, deferred Ilsevet
        val early1 = chapters1920.take(5).toSet()
        val early2 = chapters1920.drop(5).toSet()
        val t1 = tell(base, a, early1, first)
        assertJudgedOnItsOwnAnswers("telling 1", t1, a, early1)
        val t2 = tell(ReplayKit.finishAndReplay(t1.end), b, early2, last)
        assertJudgedOnItsOwnAnswers("telling 2", t2, b, early1 + early2)
        decisions.forEach { g ->
            assertNotEquals("the tellings did answer differently: $g", t1.end.flags.filter { it in g }, t2.end.flags.filter { it in g })
        }
        assertTrue("The Watch Goes On" in t2.end.trophies)
    }

    @Test
    fun `the same pair in reverse order`() {
        val a = ReplayKit.ENDINGS[5]
        val b = ReplayKit.ENDINGS[0]
        val t1 = tell(base, a, chapters1920.take(5).toSet(), last)
        assertJudgedOnItsOwnAnswers("telling 1", t1, a, chapters1920.take(5).toSet())
        val t2 = tell(ReplayKit.finishAndReplay(t1.end), b, chapters1920.drop(5).toSet(), first)
        assertJudgedOnItsOwnAnswers("telling 2", t2, b, chapters1920.toSet())
    }

    @Test
    fun `six consecutive tellings reach all six endings, answering first, last and middle in turn`() {
        val choosers = listOf(first, last, middle)
        val parts = chapters1920.chunked(4)
        var s = base
        var earlierSoFar = emptySet<String>()
        ReplayKit.ENDINGS.forEachIndexed { i, ending ->
            val added = parts[i % parts.size].toSet()
            earlierSoFar = earlierSoFar + added
            val t = tell(s, ending, added, choosers[i % choosers.size])
            assertJudgedOnItsOwnAnswers("telling ${i + 1} (${ending.nodeId})", t, ending, earlierSoFar)
            s = ReplayKit.finishAndReplay(t.end)
        }
    }

    @Test
    fun `every ordered pair of the six endings is reached and exclusive when the two tellings answer oppositely`() {
        ReplayKit.ENDINGS.forEachIndexed { i, a ->
            ReplayKit.ENDINGS.forEachIndexed { j, b ->
                val early1 = chapters1920.take(5).toSet()
                val early2 = chapters1920.drop(5).toSet()
                val t1 = tell(base, a, early1, first)
                assertJudgedOnItsOwnAnswers("pair $i-$j telling 1", t1, a, early1)
                val t2 = tell(ReplayKit.finishAndReplay(t1.end), b, early2, last)
                assertJudgedOnItsOwnAnswers("pair $i-$j telling 2", t2, b, early1 + early2)
            }
        }
    }

    @Test
    fun `answering the same way twice changes nothing`() {
        val e = ReplayKit.ENDINGS[2]
        val t1 = tell(base, e, chapters1920.toSet(), first)
        val t2 = tell(ReplayKit.finishAndReplay(t1.end), e, chapters1920.toSet(), first)
        assertEquals(t1.end.flags.intersect(C21_TELLING_FLAGS), t2.end.flags.intersect(C21_TELLING_FLAGS))
    }

    // ---- Go Back ----------------------------------------------------------------------------------------------

    @Test
    fun `Back after a Chapter 21 choice restores the exact prior state, and the other answer then gives a clean result`() {
        entryChoices.forEach { firstChoice ->
            val store = SaveStore(FakeSharedPreferences())
            val before = atEntry(C21_TELLING_FLAGS + chapters1920 + keep)
            val after = store.commitChoice(before, firstChoice)
            val ownFirst = firstChoice.consequences.setFlags.single()
            assertEquals(firstChoice.label, chapters1920.toSet() + keep + ownFirst, after.flags)

            val back = store.restorePreviousState(1)
            assertNotNull(firstChoice.label, back)
            assertEquals("${firstChoice.label}: Back restores every field, including the cleared flags", before, back!!)

            val other = entryChoices.first { it !== firstChoice }
            val ownOther = other.consequences.setFlags.single()
            val again = store.commitChoice(back, other)
            assertEquals(firstChoice.label, chapters1920.toSet() + keep + ownOther, again.flags)
            assertTrue("${firstChoice.label}: the undone answer is gone", ownFirst !in again.flags)
            assertEquals(firstChoice.label, before.applyChoice(other), again)
        }
    }

    @Test
    fun `Back and then the same Chapter 21 choice again gives the same result as the first time`() {
        val store = SaveStore(FakeSharedPreferences())
        val choice = entryChoices.first()
        val before = atEntry(C21_TELLING_FLAGS + chapters1920 + keep)
        val once = store.commitChoice(before, choice)
        val back = store.restorePreviousState(1)!!
        val twice = store.commitChoice(back, choice)
        assertEquals(once, twice)
    }

    // ---- stale saves -------------------------------------------------------------------------------------------

    @Test
    fun `a save at the end of an earlier telling loads untouched and is reset when Chapter 21 is entered again`() {
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        val early1 = chapters1920.take(5).toSet()
        val old = tell(base, ReplayKit.ENDINGS[0], early1, first)
        val oldAnswers = old.end.flags.intersect(C21_TELLING_FLAGS)
        assertTrue("an earlier telling holds answers", oldAnswers.size >= decisions.size)
        store.save(1, old.end)

        val loaded = SaveStore(prefs).load(1)!!
        assertEquals("loading never edits flags", old.end.flags, loaded.flags)

        // Replay, answering everything the other way round.
        val onEnding = store.commitChoice(loaded, ReplayKit.arrivals(loaded).single())
        val replayed = store.commitChoice(onEnding, ReplayKit.loopChoice(onEnding.sceneId))
        val unaided = store.commitChoice(ReplayKit.jumpTo(replayed, ReplayKit.AID_NODE), ReplayKit.aidChoice(false))
        val node = ReplayKit.FATE_NODES[1]
        val decided = store.commitChoice(ReplayKit.jumpTo(unaided, node), ReplayKit.fateChoice(node, "deferred_ilsevets_fate"))
        val atChapter21 = ReplayKit.jumpTo(decided, entry)
        assertTrue("until Chapter 21 is entered the old answers are still carried", atChapter21.flags.containsAll(oldAnswers))

        val choice = entryChoices.last()
        val entered = store.commitChoice(atChapter21, choice)
        assertEquals("only the new Chapter 21 answer remains", choice.consequences.setFlags, entered.flags.intersect(C21_TELLING_FLAGS))
        assertTrue("Chapter 19-20 flags kept", entered.flags.containsAll(early1))
        assertTrue("trophies kept, including the replay trophy", entered.trophies.containsAll(old.end.trophies + "The Watch Goes On"))
        assertTrue("items kept", entered.inventory.containsAll(old.end.inventory))
        assertEquals("the reset is saved", entered.flags, SaveStore(prefs).load(1)!!.flags)
    }

    @Test
    fun `a save parked in the middle of an earlier telling is not repaired on load, only on re-entering Chapter 21`() {
        val prefs = FakeSharedPreferences()
        val old = tell(base, ReplayKit.ENDINGS[3], chapters1920.take(5).toSet(), first)
        // Two Emberlow answers, as a replay under the old rules would have left.
        val staleFlags = old.end.flags + "waited_at_emberlow" + "refused_to_leave_emberlow"
        val parked = ReplayKit.jumpTo(old.end.copy(flags = staleFlags), "what_yielding_cost")
        SaveStore(prefs).save(1, parked)

        val store = SaveStore(prefs)
        val loaded = store.load(1)!!
        assertEquals("loading never edits flags", staleFlags, loaded.flags)
        assertEquals("the old behaviour is kept until Chapter 21 is entered again", 2,
            StoryRepository.visibleChoices(StoryRepository.node("the_road_back_to_the_sundering_ground"), loaded).size)

        val entered = store.commitChoice(ReplayKit.jumpTo(loaded, entry), entryChoices.first())
        assertEquals(entryChoices.first().consequences.setFlags, entered.flags.intersect(C21_TELLING_FLAGS))
    }
}
