package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.LockedDisplay
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Opt-in locked choices: available, locked and hidden rows, stat and flag explanations, and the unchanged default. */
class ChoiceAvailabilityTest {

    private fun node(vararg choices: Choice) =
        StoryNode("n", "chapter_1", "N", "ill", "text", choices.toList())

    private fun choice(
        label: String,
        req: ChoiceRequirement? = null,
        display: LockedDisplay = LockedDisplay.HIDE,
        hint: String? = null
    ) = Choice(label, "next", req, Consequences(), display, hint)

    private val courage3 = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3))
    private val honour3 = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3))
    private val needsFlag = ChoiceRequirement(requiredFlags = setOf("secret_flag_name"))

    private fun rows(n: StoryNode, s: GameState) = StoryRepository.choicesWithAvailability(n, s)

    // ---------- defaults ----------

    @Test
    fun `a new Choice hides when locked and has no hint`() {
        val c = Choice("x", "y")
        assertEquals(LockedDisplay.HIDE, c.lockedDisplay)
        assertNull(c.lockedHint)
    }

    @Test
    fun `a choice that does not opt in stays hidden exactly as before`() {
        val n = node(choice("Needs courage", courage3), choice("Free"))
        val r = rows(n, GameState(courage = 1))
        assertEquals(listOf("Free"), r.map { it.choice.label })
        assertTrue(r.all { it.isAvailable })
    }

    @Test
    fun `with no opt-ins the new rows are identical to visibleChoices for every real scene and a spread of states`() {
        val states = listOf(
            GameState(),
            GameState(courage = 5, honour = 5),
            GameState(courage = 30, honour = 30, flags = setOf("helped_dwarf", "trusted_voss_fully", "accepted_dark_aid")),
            GameState(courage = 0, honour = -2, flags = setOf("confessed_to_dwarf"))
        )
        StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.forEach { n ->
            states.forEach { s ->
                val old = StoryRepository.visibleChoices(n, s)
                val new = rows(n, s)
                assertEquals("scene ${n.id}", old, new.map { it.choice })
                assertTrue("scene ${n.id}: nothing may be locked while no choice opts in", new.all { it.isAvailable })
            }
        }
    }

    // ---------- available / locked / hidden ----------

    @Test
    fun `an opted-in choice whose requirement is met is a normal available row`() {
        val r = rows(node(choice("Stand", courage3, LockedDisplay.SHOW_LOCKED)), GameState(courage = 3)).single()
        assertTrue(r.isAvailable)
        assertTrue(r.lockedReasons.isEmpty())
    }

    @Test
    fun `an opted-in choice whose requirement is unmet is shown locked with the stat explained`() {
        val r = rows(node(choice("Stand", courage3, LockedDisplay.SHOW_LOCKED)), GameState(courage = 1)).single()
        assertFalse(r.isAvailable)
        assertEquals(listOf("Requires Courage 3 (you have 1)"), r.lockedReasons)
    }

    @Test
    fun `the stat explanation tracks the current stat value`() {
        val n = node(choice("Plead", honour3, LockedDisplay.SHOW_LOCKED))
        assertEquals(listOf("Requires Honour 3 (you have 0)"), rows(n, GameState(honour = 0)).single().lockedReasons)
        assertEquals(listOf("Requires Honour 3 (you have 2)"), rows(n, GameState(honour = 2)).single().lockedReasons)
        assertTrue(rows(n, GameState(honour = 3)).single().isAvailable)
    }

    @Test
    fun `negative stats are explained accurately`() {
        val n = node(choice("Plead", honour3, LockedDisplay.SHOW_LOCKED))
        assertEquals(listOf("Requires Honour 3 (you have -2)"), rows(n, GameState(honour = -2)).single().lockedReasons)
    }

    @Test
    fun `only the unmet stats are listed`() {
        val both = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3, StatType.HONOUR to 3))
        val n = node(choice("Both", both, LockedDisplay.SHOW_LOCKED))
        assertEquals(
            listOf("Requires Courage 3 (you have 1) and Honour 3 (you have 0)"),
            rows(n, GameState(courage = 1, honour = 0)).single().lockedReasons
        )
        assertEquals(
            listOf("Requires Honour 3 (you have 0)"),
            rows(n, GameState(courage = 5, honour = 0)).single().lockedReasons
        )
    }

    @Test
    fun `locked and available and hidden rows keep authoring order`() {
        val n = node(
            choice("A free"),
            choice("B locked", courage3, LockedDisplay.SHOW_LOCKED),
            choice("C hidden", honour3),
            choice("D free")
        )
        val r = rows(n, GameState(courage = 1))
        assertEquals(listOf("A free", "B locked", "D free"), r.map { it.choice.label })
        assertEquals(listOf(true, false, true), r.map { it.isAvailable })
    }

    // ---------- flag gates ----------

    @Test
    fun `a flag-locked choice shows only its authored hint, never the flag name`() {
        val n = node(choice("Take it", needsFlag, LockedDisplay.SHOW_LOCKED, hint = "You would need to have helped someone first."))
        val r = rows(n, GameState()).single()
        assertFalse(r.isAvailable)
        assertEquals(listOf("You would need to have helped someone first."), r.lockedReasons)
        assertFalse(r.lockedReasons.any { it.contains("secret_flag_name") || it.contains("secret flag name") })
    }

    @Test
    fun `a flag-locked choice with no authored hint falls back to hidden rather than an unexplained lock`() {
        val n = node(choice("Take it", needsFlag, LockedDisplay.SHOW_LOCKED))
        assertTrue(rows(n, GameState()).isEmpty())
    }

    @Test
    fun `a forbidden flag locks with the authored hint`() {
        val forbidden = ChoiceRequirement(forbiddenFlags = setOf("blocking_flag"))
        val n = node(choice("Again", forbidden, LockedDisplay.SHOW_LOCKED, hint = "That door has already closed."))
        assertTrue(rows(n, GameState()).single().isAvailable)
        val locked = rows(n, GameState(flags = setOf("blocking_flag"))).single()
        assertFalse(locked.isAvailable)
        assertEquals(listOf("That door has already closed."), locked.lockedReasons)
    }

    @Test
    fun `mixed stat and flag gate - each unmet part is explained and met parts are not`() {
        val mixed = ChoiceRequirement(requiredFlags = setOf("secret_flag_name"), minStats = mapOf(StatType.COURAGE to 3))
        val n = node(choice("Mixed", mixed, LockedDisplay.SHOW_LOCKED, hint = "Someone must have trusted you."))

        // stat unmet, flag met: only the stat line, and the (now-false) flag hint is not shown
        val statOnly = rows(n, GameState(courage = 1, flags = setOf("secret_flag_name"))).single()
        assertEquals(listOf("Requires Courage 3 (you have 1)"), statOnly.lockedReasons)

        // stat met, flag unmet: only the hint
        val flagOnly = rows(n, GameState(courage = 5)).single()
        assertEquals(listOf("Someone must have trusted you."), flagOnly.lockedReasons)

        // both unmet: both, stat first
        val both = rows(n, GameState(courage = 1)).single()
        assertEquals(listOf("Requires Courage 3 (you have 1)", "Someone must have trusted you."), both.lockedReasons)

        // both met: available
        assertTrue(rows(n, GameState(courage = 3, flags = setOf("secret_flag_name"))).single().isAvailable)
    }

    @Test
    fun `an authored hint is ignored when only a stat is unmet`() {
        val n = node(choice("Stat", courage3, LockedDisplay.SHOW_LOCKED, hint = "Should not appear."))
        assertEquals(listOf("Requires Courage 3 (you have 1)"), rows(n, GameState(courage = 1)).single().lockedReasons)
    }

    // ---------- invariants ----------

    @Test
    fun `availability always agrees with the requirement and every locked row has a reason`() {
        val reqs = listOf(
            courage3, honour3, needsFlag,
            ChoiceRequirement(requiredFlags = setOf("secret_flag_name"), minStats = mapOf(StatType.HONOUR to 2)),
            ChoiceRequirement(forbiddenFlags = setOf("blocking_flag"), minStats = mapOf(StatType.COURAGE to 2))
        )
        val states = listOf(
            GameState(), GameState(courage = 3, honour = 3),
            GameState(flags = setOf("secret_flag_name")), GameState(courage = 4, flags = setOf("blocking_flag")),
            GameState(honour = 2, flags = setOf("secret_flag_name"))
        )
        reqs.forEach { req ->
            states.forEach { s ->
                val c = choice("c", req, LockedDisplay.SHOW_LOCKED, hint = "A hint.")
                val row = rows(node(c), s).single()
                assertEquals(req.isSatisfiedBy(s), row.isAvailable)
                if (!row.isAvailable) assertTrue(row.lockedReasons.isNotEmpty())
                if (row.isAvailable) assertTrue(row.lockedReasons.isEmpty())
            }
        }
    }

    @Test
    fun `lockedReasonsFor is empty for a choice with no requirement or a satisfied one`() {
        assertTrue(lockedReasonsFor(choice("free"), GameState()).isEmpty())
        assertTrue(lockedReasonsFor(choice("ok", courage3), GameState(courage = 9)).isEmpty())
    }

    @Test
    fun `opting in does not change what taking the choice does`() {
        val consequences = Consequences(statDeltas = mapOf(StatType.COURAGE to 1), setFlags = setOf("f"), grantItemIds = listOf("sturdy_buckler"), unlockTrophy = "t")
        val hidden = Choice("x", "next", courage3, consequences)
        val shown = hidden.copy(lockedDisplay = LockedDisplay.SHOW_LOCKED, lockedHint = "hint")
        val s = GameState(courage = 3)
        assertEquals(s.applyChoice(hidden), s.applyChoice(shown))
    }
}
