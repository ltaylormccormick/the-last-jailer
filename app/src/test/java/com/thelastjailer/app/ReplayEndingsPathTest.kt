package com.thelastjailer.app

import com.thelastjailer.app.data.DARK_AID_FAMILY
import com.thelastjailer.app.data.ILSEVET_FATE_FAMILY
import com.thelastjailer.app.data.ReplayKit
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity along real replay paths: telling after telling in one slot, each ending reached with
 * exactly one arrival button, and the scenes that read the aid and fate flags (Chapter 5, Chapter 23,
 * Chapter 30) seeing only the current telling's answers.
 *
 * Two styles, both on the real story and the real choices:
 *  - "decisions": jump between the decision scenes (fast, deterministic, no dependence on the rest of the story);
 *  - "end to end": walk the whole story scene by scene, as a player would.
 */
class ReplayEndingsPathTest {

    private val envoyNodes = listOf("the_envoys_offer_alone", "the_envoys_offer_with_halvard")

    /** The Chapter 5 lines that assume the aid was taken, as offered to [state]. */
    private fun aidAssumingLines(state: GameState): List<Choice> = envoyNodes.flatMap { id ->
        StoryRepository.visibleChoices(StoryRepository.node(id), state).filter { it.nextNodeId == "the_envoys_mask_slips_confronted" }
    }

    private fun lookCloser(state: GameState): List<Choice> =
        StoryRepository.visibleChoices(StoryRepository.node("the_road_to_emberlow"), state).filter { it.label == "Look closer." }

    private fun assertTellingIsJudgedOnItsOwnAnswers(index: Int, ending: ReplayKit.Ending, atEnd: GameState) {
        val label = "telling ${index + 1} (${ending.nodeId})"
        val expectedAid = if (ending.aided) "accepted_dark_aid" else "refused_dark_aid_again"
        val shown = ReplayKit.arrivals(atEnd)
        // Every check runs and the failures are reported together, so one symptom can never hide another.
        val problems = ArrayList<String>()
        fun expectEqual(what: String, expected: Any?, actual: Any?) {
            if (expected != actual) problems += "$what: expected:<$expected> but was:<$actual>"
        }
        // What the player sees: Chapter 30 arrival (one button, this telling's ending), Chapter 23, Chapter 5.
        expectEqual("arrival buttons", 1, shown.size)
        expectEqual("arrival target", ending.nodeId, shown.singleOrNull()?.nextNodeId)
        expectEqual("Look closer buttons", 1, lookCloser(atEnd).size)
        expectEqual("aid-assuming Chapter 5 lines", if (ending.aided) 2 else 0, aidAssumingLines(atEnd).size)
        // The cause: only this telling's answers are held.
        expectEqual("aid flags held", setOf(expectedAid), atEnd.flags.intersect(DARK_AID_FAMILY))
        expectEqual("fate flags held", setOf(ending.fate), atEnd.flags.intersect(ILSEVET_FATE_FAMILY))
        assertTrue("$label:\n  " + problems.joinToString("\n  "), problems.isEmpty())
    }

    private fun decisionsChain(order: List<ReplayKit.Ending>) {
        var s = ReplayKit.STRONG
        order.forEachIndexed { i, ending ->
            val fateNode = ReplayKit.FATE_NODES[i % 2] // alternate the trusted and guarded Chapter 18 scenes
            val atEnd = ReplayKit.tellingByDecisions(s, ending, fateNode)
            assertTellingIsJudgedOnItsOwnAnswers(i, ending, atEnd)
            s = ReplayKit.finishAndReplay(atEnd)
            assertEquals("fallen_knight", s.sceneId)
        }
    }

    // ---- decisions style ------------------------------------------------------------------------------------

    @Test
    fun `decisions - six consecutive tellings in one slot reach all six endings, one arrival each`() =
        decisionsChain(ReplayKit.ENDINGS)

    @Test
    fun `decisions - the same in reverse order, so every change of answer is exercised`() =
        decisionsChain(ReplayKit.ENDINGS.reversed())

    @Test
    fun `decisions - an aided telling followed by unaided ones and back again`() {
        val e = ReplayKit.ENDINGS
        decisionsChain(listOf(e[0], e[3], e[0], e[5], e[2], e[4], e[1], e[3]))
    }

    @Test
    fun `decisions - the same ending told twice in a row stays one arrival`() {
        val e = ReplayKit.ENDINGS
        decisionsChain(listOf(e[1], e[1], e[4], e[4]))
    }

    @Test
    fun `decisions - a first play with no earlier telling is unaffected`() {
        ReplayKit.ENDINGS.forEachIndexed { i, ending ->
            assertTellingIsJudgedOnItsOwnAnswers(i, ending, ReplayKit.tellingByDecisions(ReplayKit.STRONG, ending))
        }
    }

    // ---- end to end style -----------------------------------------------------------------------------------

    private fun toTheEnd(from: GameState, ending: ReplayKit.Ending): GameState =
        ReplayKit.play(from, stop = { it.sceneId == "chapter30_end" }, pick = ReplayKit.decisions(ending.aided, ending.fate))

    @Test
    fun `end to end - each ending is reachable on a first play`() {
        ReplayKit.ENDINGS.forEach { ending ->
            val atEnd = toTheEnd(ReplayKit.STRONG, ending)
            assertEquals("first play ${ending.nodeId}", ending.nodeId, ReplayKit.arrivals(atEnd).single().nextNodeId)
        }
    }

    @Test
    fun `end to end - six consecutive real tellings reach all six endings with one arrival each`() {
        var s = ReplayKit.STRONG
        ReplayKit.ENDINGS.forEachIndexed { i, ending ->
            val atEnd = toTheEnd(s, ending)
            assertTellingIsJudgedOnItsOwnAnswers(i, ending, atEnd)
            s = ReplayKit.finishAndReplay(atEnd)
        }
    }

    @Test
    fun `end to end - Chapter 5 sees the aid flag only after a telling that took the aid`() {
        var s = ReplayKit.STRONG
        listOf(true, false, true, false, false, true).forEach { aided ->
            val fate = "deferred_ilsevets_fate"
            val atChapter5 = ReplayKit.play(
                s, stop = { it.chapterId == "chapter_5" }, pick = ReplayKit.decisions(aided, fate)
            )
            assertEquals("aided=$aided", aided, "accepted_dark_aid" in atChapter5.flags)
            val atEnd = ReplayKit.play(
                atChapter5, stop = { it.sceneId == "chapter30_end" }, pick = ReplayKit.decisions(aided, fate)
            )
            assertTrue("aided=$aided", ReplayKit.arrivals(atEnd).size == 1)
            s = ReplayKit.finishAndReplay(atEnd)
        }
    }
}
