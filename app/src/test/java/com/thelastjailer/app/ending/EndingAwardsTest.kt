package com.thelastjailer.app.ending

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitSlotSave
import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.ReplayKit
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.ending.EndingKit.LEGACIES
import com.thelastjailer.app.ending.EndingKit.NINE
import com.thelastjailer.app.ending.EndingKit.arrival
import com.thelastjailer.app.ending.EndingKit.awardsOf
import com.thelastjailer.app.ending.EndingKit.mine
import com.thelastjailer.app.ending.EndingKit.playTo
import com.thelastjailer.app.ending.EndingKit.routeA
import com.thelastjailer.app.ending.EndingKit.routeB
import com.thelastjailer.app.ending.EndingKit.routeC
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * When the nine ending trophies are awarded, and when they are not. Everything here runs on the real story graph and the real
 * [applyChoice] and [SaveStore], so the award step is tested where it lives and not in isolation.
 */
class EndingAwardsTest {
    private val decisions = LegacyRule.decisions

    private fun store() = SaveStore(FakeSharedPreferences())

    private fun epilogueFor(flags: Set<String>): String {
        val aided = "accepted_dark_aid" in flags
        val fate = listOf("captured_ilsevet", "showed_ilsevet_mercy", "deferred_ilsevets_fate").first { it in flags }
        return ReplayKit.ENDINGS.first { it.aided == aided && it.fate == fate }.nodeId
    }

    // ---- arriving ----------------------------------------------------------------------------------------------

    @Test
    fun `arriving awards completion, both outcomes and the right legacy on the three example routes`() {
        val expected = mapOf(
            routeA to setOf("The Tale Told", "The Unyielding", "Borrowed Strength", "Her Chains"),
            routeB to setOf("The Tale Told", "The Honourbound", "Unaided", "Her Freedom"),
            routeC to setOf("The Tale Told", "The Steadfast", "Borrowed Strength", "Her Fate Unwritten")
        )
        expected.forEach { (wanted, awards) ->
            val end = playTo(ReplayKit.STRONG, wanted)
            assertTrue("nothing is awarded before the arrival", mine(end.trophies).isEmpty())
            val onEnding = store().commitChoice(end, arrival(end))
            assertEquals(awards, mine(onEnding.trophies))
            assertTrue("what was held is kept", onEnding.trophies.containsAll(end.trophies))
            assertTrue(StoryRepository.isEnding(StoryRepository.node(onEnding.sceneId)))
        }
    }

    @Test
    fun `every one of the six endings awards its own completion and outcomes whatever the flags`() {
        ReplayKit.ENDINGS.forEach { e ->
            val atEnd = ReplayKit.STRONG.copy(sceneId = "chapter30_end", chapterId = "chapter_30")
            val choice = StoryRepository.node("chapter30_end").choices.first { it.nextNodeId == e.nodeId }
            val arrived = atEnd.applyChoice(choice)
            assertEquals(e.nodeId, awardsOf(e.nodeId) + LEGACIES.intersect(arrived.trophies), mine(arrived.trophies))
        }
    }

    // ---- nothing else awards -----------------------------------------------------------------------------------

    @Test
    fun `loading a saved game never awards anything`() {
        val end = playTo(ReplayKit.STRONG, routeA)
        val onEnding = end.applyChoice(arrival(end))
        val parkedByAnOlderBuild = onEnding.copy(trophies = onEnding.trophies - NINE)
        val store = store()
        store.save(1, parkedByAnOlderBuild)
        assertEquals(parkedByAnOlderBuild, store.load(1))
        assertTrue(mine(store.load(1)!!.trophies).isEmpty())
    }

    @Test
    fun `an ordinary move awards nothing, even for a player who holds The Watch Goes On`() {
        val holder = ReplayKit.STRONG.copy(trophies = setOf("The Watch Goes On"))
        val node = StoryRepository.node(holder.sceneId)
        val moved = holder.applyChoice(StoryRepository.visibleChoices(node, holder).first())
        assertTrue(mine(moved.trophies).isEmpty())
        assertEquals(setOf("The Watch Goes On"), moved.trophies)
    }

    @Test
    fun `a holder of The Watch Goes On gets completion and the legacy when the next telling reaches an ending`() {
        val midSecond = ReplayKit.STRONG.copy(trophies = setOf("The Watch Goes On"))
        val end = playTo(midSecond, routeB)
        val onEnding = end.applyChoice(arrival(end))
        assertTrue("The Tale Told" in onEnding.trophies)
        assertEquals(setOf("The Honourbound"), onEnding.trophies.intersect(LEGACIES))
    }

    @Test
    fun `a won fight can never award an ending trophy`() {
        val fights = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.filter { it.combatEncounterId != null }
        assertTrue(fights.size > 20)
        fights.forEach { n ->
            val s = ReplayKit.STRONG.copy(sceneId = n.id, chapterId = n.chapterId, flags = routeA, trophies = emptySet())
            val after = s.resolveCombat(CombatRepository.encounter(n.combatEncounterId!!), CombatOutcome(victory = true, damageTaken = 0))
            assertTrue(n.id, mine(after.trophies).isEmpty())
        }
    }

    @Test
    fun `across the whole story only the six arrivals and the six replays can award an ending trophy`() {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        var checked = 0
        var awarding = 0
        nodes.forEach { n ->
            n.choices.forEach { c ->
                checked++
                val after = ReplayKit.STRONG.copy(sceneId = n.id, chapterId = n.chapterId, flags = routeA).applyChoice(c)
                if (mine(after.trophies).isNotEmpty()) {
                    awarding++
                    assertTrue("${n.id} -> ${c.nextNodeId} awarded outside an arrival or a replay",
                        c.nextNodeId in EndingAwards.endingIds || n.id in EndingAwards.endingIds)
                }
            }
        }
        println("AWARDS whole graph: choices=$checked awarding=$awarding")
        assertEquals(12, awarding)
    }

    @Test
    fun `each ending offers exactly one way out, the replay, so leaving cannot be a different transition`() {
        EndingAwards.endingIds.forEach { id ->
            val choices = StoryRepository.node(id).choices
            assertEquals(id, 1, choices.size)
            assertEquals(id, "fallen_knight", choices.single().nextNodeId)
        }
    }

    // ---- Back, replay, a second telling ------------------------------------------------------------------------

    @Test
    fun `Back keeps what was earned, restores the flags one step, and arriving again adds nothing`() {
        val end = playTo(ReplayKit.STRONG, routeB)
        val store = store()
        val first = store.commitChoice(end, arrival(end))
        val back = store.restorePreviousState(1)!!
        assertEquals("chapter30_end", back.sceneId)
        assertEquals(end.flags, back.flags)
        assertEquals(mine(first.trophies), mine(back.trophies))
        val again = store.commitChoice(back, arrival(back))
        assertEquals(first, again)
    }

    @Test
    fun `Begin the tale again adds only the replay trophy to what the arrival gave, and never a different legacy`() {
        val end = playTo(ReplayKit.STRONG, routeA)
        val store = store()
        val arrived = store.commitChoice(end, arrival(end))
        val replayed = store.commitChoice(arrived, ReplayKit.loopChoice(arrived.sceneId))
        assertEquals("fallen_knight", replayed.sceneId)
        assertEquals(arrived.flags, replayed.flags)
        assertEquals(arrived.trophies + "The Watch Goes On", replayed.trophies)
        assertEquals(setOf("The Unyielding"), replayed.trophies.intersect(LEGACIES))
    }

    @Test
    fun `a second telling with opposite answers earns a second legacy, keeps the first, and is judged on its own flags`() {
        val store = store()
        val endA = playTo(ReplayKit.STRONG, routeA)
        val arrivedA = store.commitChoice(endA, arrival(endA))
        val again = store.commitChoice(arrivedA, ReplayKit.loopChoice(arrivedA.sceneId))
        val endB = playTo(again, routeB)
        decisions.forEach { d -> assertEquals(d.id, 1, endB.flags.count { f -> d.options.any { it.flag == f } }) }
        val arrivedB = store.commitChoice(endB, arrival(endB))
        assertEquals(setOf("The Unyielding", "The Honourbound"), arrivedB.trophies.intersect(LEGACIES))
        assertTrue(arrivedB.trophies.containsAll(arrivedA.trophies))
        assertEquals(setOf("The Tale Told", "The Unyielding", "The Honourbound", "Borrowed Strength", "Unaided", "Her Chains", "Her Freedom"), mine(arrivedB.trophies))
    }

    @Test
    fun `the award step is idempotent and only ever adds`() {
        val end = playTo(ReplayKit.STRONG, routeA)
        val once = end.applyChoice(arrival(end))
        val twice = once.copy(sceneId = "chapter30_end").applyChoice(arrival(end))
        assertEquals(once.trophies, twice.trophies)
        assertTrue(twice.trophies.containsAll(end.trophies))
        assertEquals(1, once.trophies.intersect(LEGACIES).size)
    }

    // ---- older, damaged and stale saves ------------------------------------------------------------------------

    @Test
    fun `older and damaged saves parked on an ending - completion and outcomes always, a legacy only when it is proven`() {
        val ending = "the_watch_continues_dark_aid_captured"
        val parked = ReplayKit.STRONG.copy(chapterId = "chapter_30", sceneId = ending)
        val leave = ReplayKit.loopChoice(ending)
        val zero = store().commitChoice(parked, leave)
        assertEquals(setOf("The Tale Told", "Borrowed Strength", "Her Chains"), mine(zero.trophies))
        val contradicted = store().commitChoice(parked.copy(flags = routeA + "waited_at_emberlow"), leave)
        assertEquals("a harmless contradiction still gives the right legacy", setOf("The Unyielding"), contradicted.trophies.intersect(LEGACIES))
        val narrowFlags = EndingKit.route(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0) + "offered_connection_without_merging"
        val narrow = store().commitChoice(parked.copy(flags = narrowFlags), leave)
        assertTrue("a contradiction that could change the result gives no legacy", narrow.trophies.intersect(LEGACIES).isEmpty())
        assertTrue("The Tale Told" in narrow.trophies)
    }

    @Test
    fun `older replayed saves - over many pairs of tellings no legacy is ever awarded that the last telling did not earn`() {
        val random = Random(0x2910)
        var awarded = 0; var withheld = 0; var wrong = 0
        repeat(4000) {
            val first = decisions.map { d -> d.options[random.nextInt(d.options.size)].flag }
            val second = decisions.mapIndexed { i, d -> if (i >= 5 && random.nextInt(10) < 7) first[i] else d.options[random.nextInt(d.options.size)].flag }
            val stale = HashSet<String>().apply { addAll(second.take(5)); addAll(first.drop(5)); addAll(second.drop(5)) }
            val want = LegacyRule.evaluate(second.toSet()).axis
            val ending = epilogueFor(second.toSet())
            val parked = ReplayKit.STRONG.copy(chapterId = "chapter_30", sceneId = ending, flags = stale)
            val after = parked.applyChoice(ReplayKit.loopChoice(ending))
            val got = after.trophies.intersect(LEGACIES)
            assertTrue(got.size <= 1)
            assertTrue("completion is always given", "The Tale Told" in after.trophies)
            assertEquals("the outcomes come from the ending, not the flags", awardsOf(ending), after.trophies.intersect(NINE) - LEGACIES)
            if (got.isEmpty()) withheld++ else {
                awarded++
                val axis = when (got.single()) { "The Unyielding" -> Axis.COURAGE; "The Honourbound" -> Axis.HONOUR; else -> Axis.STEADFAST }
                if (axis != want) wrong++
            }
        }
        println("AWARDS older saves: pairs=4000 awarded=$awarded withheld=$withheld wrong=$wrong")
        assertEquals(0, wrong)
    }

    // ---- save-copy and Start Again ---------------------------------------------------------------------------

    @Test
    fun `SAVE into another slot replaces the destination's trophies with the source's and awards nothing`() {
        val end = playTo(ReplayKit.STRONG, routeC)
        val store = store()
        val arrived = store.commitChoice(end, arrival(end))
        store.save(2, GameState(activeSlot = 2, trophies = setOf("First Blood", "The Unyielding")))
        val copied = store.commitSlotSave(2, arrived)
        val loaded = store.load(2)!!
        assertEquals(arrived.trophies, loaded.trophies)
        assertEquals(arrived.trophies, copied.trophies)
        assertFalse("the destination's own legacy was replaced, not merged", "The Unyielding" in loaded.trophies)
    }

    @Test
    fun `Start Again wipes the ending trophies with everything else`() {
        val end = playTo(ReplayKit.STRONG, routeA)
        val store = store()
        store.commitChoice(end, arrival(end))
        val wiped = store.commitRestart(1)
        assertTrue(wiped.trophies.isEmpty())
        assertTrue(store.load(1)!!.trophies.isEmpty())
    }
}
