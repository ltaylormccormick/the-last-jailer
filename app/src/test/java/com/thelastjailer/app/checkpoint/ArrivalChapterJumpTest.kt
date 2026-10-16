package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.commitChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recorder never trusts the story graph to stay as it is today: a forward jump of more than one chapter
 * drops the timeline, because the chapters it skips may still carry ledger entries from an abandoned branch.
 */
class ArrivalChapterJumpTest {
    private fun jump(rig: Rig, from: Int, to: Int, tag: String = "a"): GameState {
        val node = Story.crossing(from).first
        return rig.saves.commitChoice(Story.stateAt(node, 1, tag), Choice("skip ahead", Cp.startScene(to)), rig.recorder)
    }

    // ---- the story today: only +1 edges and the six loops ---------------------------------------------------

    @Test
    fun `every chapter change in the story is either one chapter forward or one of the six Chapter 30 loops`() {
        val all = Story.allCrossings()
        assertTrue(all.isNotEmpty())
        var loops = 0
        for ((node, choice, dest) in all) {
            val from = ChapterIds.numberOf(node.chapterId)!!
            val to = ChapterIds.numberOf(dest.chapterId)!!
            if (to == from + 1) continue
            assertTrue("${node.id} -> ${choice.nextNodeId}: $from -> $to", from == 30 && to == 1)
            loops++
        }
        assertEquals(6, loops)
    }

    // ---- ordinary transitions are unchanged ------------------------------------------------------------------

    @Test
    fun `a normal one chapter arrival is recorded`() {
        val rig = Rig()
        rig.playTo(4)
        assertEquals((2..4).toList(), rig.timeline()!!.ledger.chapters())
        assertEquals((2..4).toList(), rig.usable())
    }

    @Test
    fun `a Chapter 30 loop still starts a new timeline`() {
        val (node, choice) = Story.loops().first()
        val rig = Rig()
        rig.playTo(30)
        val old = rig.timeline()!!.lineage
        rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)
        assertTrue(rig.timeline()!!.lineage != old)
        assertEquals(listOf(1), rig.timeline()!!.ledger.chapters())
        assertEquals(listOf(1), rig.usable())
    }

    // ---- unexpected jumps ------------------------------------------------------------------------------------

    @Test
    fun `a jump of two chapters invalidates the timeline`() {
        val rig = Rig()
        rig.playTo(5)
        val after = jump(rig, 5, 7)

        assertEquals(Cp.chapterId(7), after.chapterId)
        assertTrue(rig.timeline() == null)
        assertTrue(rig.usable().isEmpty())
        assertTrue("the jump is reported", rig.logs.any { it.contains("5 -> 7") })
    }

    @Test
    fun `a longer jump invalidates the timeline too`() {
        val rig = Rig()
        rig.playTo(3)
        jump(rig, 3, 12)
        assertTrue(rig.timeline() == null)
        assertTrue(rig.usable().isEmpty())
    }

    @Test
    fun `a jump never lets an abandoned branch's record vouch for the new timeline`() {
        val rig = Rig()
        rig.playTo(6) // ledger 2..6
        rig.saves.restorePreviousState(1) // Back: live save in chapter 5, ledger still lists 6
        assertEquals((2..6).toList(), rig.timeline()!!.ledger.chapters())

        jump(rig, 5, 7) // skips chapter 6

        assertTrue(rig.timeline() == null)
        assertFalse(6 in rig.usable())
        // and nothing from the old branch comes back through later play
        rig.arrive(7)
        rig.arrive(8)
        assertEquals(listOf(8, 9), rig.usable())
        assertFalse(6 in rig.usable())
        assertFalse(2 in rig.usable())
    }

    @Test
    fun `a jump from a brand new game records neither genesis nor an arrival`() {
        val rig = Rig()
        val fresh = GameState(activeSlot = 1)
        rig.saves.commitChoice(fresh, Choice("skip ahead", Cp.startScene(3)), rig.recorder)
        assertTrue(rig.timeline() == null)
        assertTrue(rig.fileRecords().isEmpty())
        assertTrue(rig.usable().isEmpty())
    }

    @Test
    fun `a jump with recording off still invalidates`() {
        val on = Rig()
        on.playTo(5)
        val off = on.reopen(enabled = false)
        jump(off, 5, 8)
        assertTrue(off.timeline() == null)
        assertTrue(off.usable().isEmpty())
    }

    @Test
    fun `a failure to plan a jump is contained and the game still moves`() {
        val rig = Rig(hooks = ArrivalRecorder.Hooks(digest = { _, _ -> throw IllegalStateException("boom") }))
        val after = jump(rig, 2, 4)
        assertEquals(Cp.chapterId(4), after.chapterId)
        assertEquals(after, rig.saves.load(1))
    }
}
