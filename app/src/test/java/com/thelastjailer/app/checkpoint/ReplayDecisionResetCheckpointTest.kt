package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.data.DARK_AID_FAMILY
import com.thelastjailer.app.data.ILSEVET_FATE_FAMILY
import com.thelastjailer.app.data.ReplayKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity and the checkpoint records. A record is a faithful copy of the state at the moment of
 * arrival, so one taken in a second telling before a decision holds the carried-over flags. The reset runs on
 * the choice, so restoring such a record and taking the decision still gives a clean answer.
 */
class ReplayDecisionResetCheckpointTest {

    private val carried = setOf("accepted_dark_aid", "captured_ilsevet", "showed_ilsevet_mercy")

    @Test
    fun `a record taken before the aid decision restores the carried flags, and the decision then cleans them`() {
        val stale = ReplayKit.jumpTo(ReplayKit.STRONG.copy(flags = carried), ReplayKit.AID_NODE)
        val restored = CheckpointRecord.fromState(stale, 4, Cp.L1).toGameState(1)
        assertEquals("a record is a faithful copy", stale.flags, restored.flags)

        val after = restored.applyChoice(ReplayKit.aidChoice(false))
        assertFalse("accepted_dark_aid" in after.flags)
        assertTrue("refused_dark_aid_again" in after.flags)
        assertEquals("the other family belongs to a later decision", carried - "accepted_dark_aid" + "refused_dark_aid_again", after.flags)
    }

    @Test
    fun `a record taken before the fate decision restores the carried flags, and the decision then holds one fate`() {
        val node = ReplayKit.FATE_NODES[1]
        val stale = ReplayKit.jumpTo(ReplayKit.STRONG.copy(flags = carried), node)
        val restored = CheckpointRecord.fromState(stale, 18, Cp.L1).toGameState(1)
        assertEquals(stale.flags, restored.flags)

        val after = restored.applyChoice(ReplayKit.fateChoice(node, "deferred_ilsevets_fate"))
        assertEquals(setOf("deferred_ilsevets_fate"), after.flags.intersect(ILSEVET_FATE_FAMILY))
        assertTrue("accepted_dark_aid" in after.flags)
    }

    @Test
    fun `a record taken after the decisions holds only that telling's answers`() {
        var s = ReplayKit.jumpTo(ReplayKit.STRONG.copy(flags = carried), ReplayKit.AID_NODE).applyChoice(ReplayKit.aidChoice(false))
        val node = ReplayKit.FATE_NODES[0]
        s = ReplayKit.jumpTo(s, node).applyChoice(ReplayKit.fateChoice(node, "deferred_ilsevets_fate"))
        val restored = CheckpointRecord.fromState(s, 18, Cp.L1).toGameState(1)
        assertEquals(setOf("refused_dark_aid_again"), restored.flags.intersect(DARK_AID_FAMILY))
        assertEquals(setOf("deferred_ilsevets_fate"), restored.flags.intersect(ILSEVET_FATE_FAMILY))
    }

    @Test
    fun `each Chapter 30 to Chapter 1 loop still starts a new timeline and carries the flags unchanged`() {
        for ((node, choice) in Story.loops()) {
            val rig = Rig()
            rig.playTo(30)
            val oldLineage = rig.timeline()!!.lineage

            val before = Story.stateAt(node).copy(flags = setOf("accepted_dark_aid", "captured_ilsevet"))
            val after = rig.saves.commitChoice(before, choice, rig.recorder)

            assertEquals("the loop sets and clears nothing: ${node.id}", before.flags, after.flags)
            assertTrue(node.id, rig.timeline()!!.lineage != oldLineage)
            assertEquals("the new lineage's only record is the carried state: ${node.id}", after.flags, rig.fileRecords().single().flags)
        }
    }
}
