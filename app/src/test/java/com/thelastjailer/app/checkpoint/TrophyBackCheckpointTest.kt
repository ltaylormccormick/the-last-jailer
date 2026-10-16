package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.commitChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 2B-3 against the dormant 2B-2 ledger. A Back that keeps a trophy makes the live state differ from the
 * chapter-arrival state, so rule R can no longer prove an arrival record from it. The record must then stay
 * missing; a record built from a state that is not the arrival state must never be written. Checkpoint code
 * is untouched by 2B-3: these tests only pin how it behaves with the new Back.
 */
class TrophyBackCheckpointTest {
    private val ckptFile = Cp.file(1)

    private fun tap(rig: Rig, trophy: String? = null) {
        val live = rig.saves.load(1)!!
        val c = Story.staying(ChapterIds.numberOf(live.chapterId)!!).second
        val withTrophy = c.copy(consequences = c.consequences.copy(unlockTrophy = trophy))
        rig.saves.commitChoice(live, Choice(withTrophy.label, withTrophy.nextNodeId, consequences = withTrophy.consequences), rig.recorder)
    }

    @Test
    fun `a record that was written stays valid through a trophy-keeping Back`() {
        val rig = Rig()
        rig.playTo(9)
        val (_, arrival) = rig.arrive(9)
        val record = rig.usableRecord(10)
        assertEquals(expectedRecord(arrival, 10, rig.timeline()!!.lineage), record)

        tap(rig, "Test Trophy")
        rig.saves.restorePreviousState(1)
        tap(rig)

        assertEquals((2..10).toList(), rig.usable())
        assertEquals("the record is exactly the arrival state, without the trophy", record, rig.usableRecord(10))
        assertFalse("Test Trophy" in rig.usableRecord(10)!!.trophies)
    }

    @Test
    fun `a lost record is not rebuilt from a state that kept an extra trophy - it stays missing`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val first = Rig(device)
        first.playTo(9)
        device.flushApplies()
        device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED) // the chapter 10 record cannot be written
        first.arrive(9)
        tap(first, "Test Trophy") // the player moves on and earns a trophy; the snapshot is the arrival state
        device.flushApplies()
        device.clearFaults()

        val re = first.reopen()
        assertFalse(10 in re.usable())
        val back = re.saves.restorePreviousState(1)!!
        assertTrue("the trophy was kept", "Test Trophy" in back.trophies)
        tap(re)

        assertFalse("not proven, so not usable", 10 in re.usable())
        assertTrue("and nothing was written for chapter 10", re.fileRecords().none { it.chapterNumber == 10 })
        assertEquals("earlier chapters are untouched", (2..9).toList(), re.usable())
    }

    @Test
    fun `control - without the extra trophy the same lost record is repaired, so the trophy is what changes it`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val first = Rig(device)
        first.playTo(9)
        device.flushApplies()
        device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED)
        val (_, arrival) = first.arrive(9)
        tap(first) // no trophy
        device.flushApplies()
        device.clearFaults()

        val re = first.reopen()
        re.saves.restorePreviousState(1)
        tap(re)

        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(arrival, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }
}
