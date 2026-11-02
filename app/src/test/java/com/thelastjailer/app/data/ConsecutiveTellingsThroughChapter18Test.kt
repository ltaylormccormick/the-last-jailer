package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.ending.LegacyRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Consecutive whole tellings with changed answers through Chapters 13 to 18 (and the rest of the way to the ending): each
 * telling must end holding exactly its own answers to the fourteen legacy decisions, whatever the telling before it chose.
 * The tellings are real walks of the real story from the first scene, with replay between them as the game does it.
 */
class ConsecutiveTellingsThroughChapter18Test {
    private val numbers = StoryRepository.chapters.associate { it.id to it.number }
    private val decisions = LegacyRule.decisions
    private val early = decisions.take(5)          // aid, wardens, old debts, frame, fate
    private fun flagsOf(id: String): List<String> = decisions.first { it.id == id }.options.map { it.flag }

    private class Pick(val sceneId: String, val visible: List<Choice>, val taken: Choice) {
        val index: Int get() = visible.indexOfFirst { it === taken }
    }

    private class Telling(val end: GameState, val picks: List<Pick>)

    private val first: (List<Choice>) -> Choice = { it.first() }
    private val last: (List<Choice>) -> Choice = { it.last() }
    private val middle: (List<Choice>) -> Choice = { it[it.size / 2] }

    /** One whole telling from [from] to chapter30_end: the wanted early answers where offered, [late] at every branching scene from Chapter 21. */
    private fun tell(from: GameState, wanted: Set<String>, late: (List<Choice>) -> Choice): Telling {
        val picks = ArrayList<Pick>()
        val end = ReplayKit.play(
            from,
            stop = { it.sceneId == "chapter30_end" },
            pick = { node, visible ->
                val answer = visible.firstOrNull { c -> c.consequences.setFlags.any { it in wanted } }
                val chosen = answer ?: if (visible.size > 1 && (numbers[node.chapterId] ?: 0) >= 21) late(visible) else null
                if (chosen != null) picks += Pick(node.id, visible, chosen)
                chosen
            }
        )
        return Telling(end, picks)
    }

    private fun heldIn(end: GameState, id: String): List<String> = end.flags.filter { it in flagsOf(id) }

    private fun assertFourteenExactlyOnce(label: String, end: GameState) {
        decisions.forEach { d -> assertEquals("$label: ${d.id} held ${heldIn(end, d.id)}", 1, heldIn(end, d.id).size) }
    }

    @Test
    fun `the five early decisions - all 162 answer sets as one chain of consecutive tellings, answers changed each time`() {
        val options = early.map { d -> d.options.map { it.flag } }
        var combos: List<List<String>> = listOf(emptyList())
        options.forEach { opts -> combos = combos.flatMap { c -> opts.map { c + it } } }
        assertEquals(2 * 3 * 3 * 3 * 3, combos.size)
        val order = combos.shuffled(Random(0x1318))
        val strategies = listOf(first, middle, last)
        var state = ReplayKit.STRONG
        var previous: List<String>? = null
        val changes = IntArray(early.size)
        order.forEachIndexed { i, combo ->
            val telling = tell(state, combo.toSet(), strategies[i % 3])
            val label = "telling ${i + 1} $combo"
            early.forEachIndexed { k, d ->
                assertEquals("$label: ${d.id}", listOf(combo[k]), heldIn(telling.end, d.id))
                if (previous != null && previous!![k] != combo[k]) changes[k]++
            }
            assertFourteenExactlyOnce(label, telling.end)
            assertEquals("$label: the legacy has nothing undetermined", 0, LegacyRule.evaluate(telling.end.flags).undetermined)
            assertEquals("$label: one arrival offered", 1, ReplayKit.arrivals(telling.end).size)
            previous = combo
            state = ReplayKit.finishAndReplay(telling.end)
        }
        println("CHAIN tellings=${order.size} answerChangedFromPreviousTelling(aid,wardens,old_debts,frame,fate)=${changes.toList()} of ${order.size - 1} pairs")
        assertTrue("every early decision changed answer in at least 60 of the ${order.size - 1} consecutive pairs: ${changes.toList()}", changes.all { it >= 60 })
    }

    @Test
    fun `first, middle and last really take the first, middle and last available choice at every branching scene from Chapter 21`() {
        val runs = listOf("first" to first, "middle" to middle, "last" to last).map { (name, f) -> name to tell(ReplayKit.STRONG, emptySet(), f) }
        runs.forEach { (name, telling) ->
            val branching = telling.picks.filter { it.visible.size > 1 }
            assertTrue("$name met only ${branching.size} branching scenes", branching.size >= 9)
            branching.forEach { p ->
                val expected = when (name) { "first" -> 0; "last" -> p.visible.size - 1; else -> p.visible.size / 2 }
                assertEquals("$name at ${p.sceneId}", expected, p.index)
            }
            assertFourteenExactlyOnce(name, telling.end)
        }
        val distinct = decisions.drop(5).map { d -> runs.map { (_, t) -> heldIn(t.end, d.id).single() }.toSet().size }
        println("STRATEGIES distinct answers across first/middle/last for the nine Chapter 21-30 decisions: $distinct")
        assertTrue(distinct.all { it >= 2 })
    }

    @Test
    fun `Go Back at each early decision and choosing differently leaves only the new answer`() {
        val scenes = mapOf(
            "aid" to "what_the_prisoner_offers_again", "wardens" to "who_stands_with_stonebeard", "old_debts" to "old_debts",
            "frame" to "what_must_be_broken", "fate" to "what_kaelen_chooses_trusted"
        )
        scenes.forEach { (id, scene) ->
            val atDecision = ReplayKit.play(ReplayKit.STRONG, stop = { it.sceneId == scene || (id == "fate" && it.sceneId == "what_kaelen_chooses_guarded") })
            val node = StoryRepository.node(atDecision.sceneId)
            val offered = StoryRepository.visibleChoices(node, atDecision).filter { c -> c.consequences.setFlags.any { it in flagsOf(id) } }
            assertTrue("$id offers at least two recorded answers", offered.size >= 2)
            val store = SaveStore(FakeSharedPreferences())
            val a = offered.first()
            val b = offered.last()
            store.commitChoice(atDecision, a)
            val back = store.restorePreviousState(1)!!
            assertEquals("$id: Back restores the decision scene", atDecision.sceneId, back.sceneId)
            assertTrue("$id: Back removed the first answer", heldIn(back, id).isEmpty())
            val after = store.commitChoice(back, b)
            assertEquals("$id: only the second answer", b.consequences.setFlags.filter { it in flagsOf(id) }, heldIn(after, id))
            assertFalse("$id: the abandoned answer is gone", a.consequences.setFlags.any { it in after.flags && it !in b.consequences.setFlags })
        }
    }
}
