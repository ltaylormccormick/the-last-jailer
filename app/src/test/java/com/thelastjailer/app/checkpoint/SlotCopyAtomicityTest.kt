package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitSlotSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 2C-1a. SAVE into another slot replaces the destination's game. The new state, the removal of the old
 * game's lineage/ledger and the removal of the old game's Back snapshot are ONE editor batch, so a crash can
 * only leave the old game complete or the new game complete - never the new game next to the old game's Back
 * snapshot (which used to let Back restore the old game's state with the new game's trophies merged in).
 */
class SlotCopyAtomicityTest {
    private val forward = Choice("go", "the_road_away")

    private fun newRig(): Rig = Rig(SimulatedDevice().also { it.asyncApply = true }, withRecorder = false)

    /** Destination slot 2 holds an old game with its own trophy and a Back snapshot; slot 1 is another game. */
    private fun twoGames(rig: Rig): Pair<GameState, GameState> {
        val oldBefore = GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("OldDestTrophy"), gold = 7)
        rig.saves.commitChoice(oldBefore, forward, null)
        val source = GameState(activeSlot = 1, sceneId = "the_road_away", trophies = setOf("SourceTrophy"), gold = 99, flags = setOf("src"))
        rig.saves.commitChoice(source.copy(sceneId = "fallen_knight", gold = 90), forward.copy(nextNodeId = "the_road_away"), null)
        rig.saves.save(1, source)
        rig.device.flushApplies()
        return oldBefore to source
    }

    private fun destKeys(rig: Rig, slot: Int = 2): Map<String, Any?> =
        rig.liveFile().filterKeys { it.startsWith("$slot.") }

    // ---- the reproduction of the reported corruption ------------------------------------------------------

    @Test
    fun `a slot copy is a single write batch, so no half-written intermediate state can reach disk`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        assertEquals(0, rig.device.queuedApplyCount())

        rig.saves.commitSlotSave(2, source)

        assertEquals("state, timeline keys and Back snapshot removal are one batch", 1, rig.device.queuedApplyCount())
    }

    @Test
    fun `crash before the copy reaches disk leaves the old destination game complete, Back snapshot included`() {
        val rig = newRig()
        val (oldBefore, source) = twoGames(rig)
        val oldDest = rig.saves.load(2)

        rig.saves.commitSlotSave(2, source)
        rig.device.dropQueuedApplies() // the process dies before the batch is written

        val re = rig.reopen()
        assertEquals(oldDest, re.saves.load(2))
        assertEquals(setOf("OldDestTrophy"), re.saves.load(2)!!.trophies)
        assertTrue("the old game's own Back snapshot is still there", re.saves.hasPreviousState(2))
        val back = re.saves.restorePreviousState(2)!!
        assertEquals("fallen_knight", back.sceneId)
        assertEquals(7, back.gold)
        assertEquals("only the old game's own trophy", setOf("OldDestTrophy"), back.trophies)
        assertEquals(oldBefore.sceneId, back.sceneId)
    }

    @Test
    fun `crash after the copy reaches disk leaves the new game complete with no old Back snapshot`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)

        rig.saves.commitSlotSave(2, source)
        rig.device.flushApplies()

        val re = rig.reopen()
        val dest = re.saves.load(2)!!
        assertEquals(source.copy(activeSlot = 2, chapterId = dest.chapterId), dest)
        assertEquals("exactly the source's trophies; the old destination's are gone", setOf("SourceTrophy"), dest.trophies)
        assertFalse(re.saves.hasPreviousState(2))
        assertTrue("Back has nothing to restore, so it cannot reach the old game", re.saves.restorePreviousState(2) == null)
        assertTrue("no previous.* key remains on disk", destKeys(re).keys.none { it.startsWith("2.previous.") })
    }

    @Test
    fun `a crash after any number of the copy's writes leaves one complete game`() {
        // Every prefix of the queued writes: 0 = nothing reached disk ... N = everything did. With the old
        // two-batch code, prefix 1 was the corrupt state (new game next to the old game's Back snapshot).
        val probe = newRig()
        val (_, probeSource) = twoGames(probe)
        probe.saves.commitSlotSave(2, probeSource)
        val writes = probe.device.queuedApplyCount()
        assertTrue(writes >= 1)

        for (prefix in 0..writes) {
            val rig = newRig()
            val (_, source) = twoGames(rig)
            rig.saves.commitSlotSave(2, source)
            rig.device.flushFirstApplies(prefix)

            val re = rig.reopen()
            val live = re.saves.load(2)!!
            val snapshot = re.saves.loadPreviousState(2)
            val label = "after $prefix of $writes writes"
            if (live.trophies == setOf("SourceTrophy")) {
                assertEquals(label, 99, live.gold)
                assertTrue("$label: the new game has no Back snapshot", snapshot == null)
            } else {
                assertEquals(label, setOf("OldDestTrophy"), live.trophies)
                assertTrue("$label: the old game keeps its own snapshot", snapshot != null)
                assertEquals(label, setOf("OldDestTrophy"), snapshot!!.trophies)
                assertEquals(label, 7, snapshot.gold)
            }
            val back = re.saves.restorePreviousState(2)
            assertTrue("$label: Back never mixes games", back == null || back.trophies == setOf("OldDestTrophy"))
        }
    }

    // ---- what must not change --------------------------------------------------------------------------------

    @Test
    fun `the source slot is not written by the copy`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        val before = destKeys(rig, 1)

        rig.saves.commitSlotSave(2, source)
        rig.device.flushApplies()

        assertEquals(before, destKeys(rig, 1))
        assertTrue("the source's own Back snapshot is intact", rig.saves.hasPreviousState(1))
        assertEquals(source, rig.saves.load(1))
    }

    @Test
    fun `the copy makes the destination active, as before`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        val moved = rig.saves.commitSlotSave(2, source)
        assertEquals(2, moved.activeSlot)
        assertEquals(2, rig.saves.currentActiveSlot())
        assertEquals(moved, rig.saves.load(2))
    }

    @Test
    fun `SAVE onto an empty slot works and writes no Back snapshot`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)

        rig.saves.commitSlotSave(3, source)
        rig.device.flushApplies()

        assertEquals(setOf("SourceTrophy"), rig.saves.load(3)!!.trophies)
        assertFalse(rig.saves.hasPreviousState(3))
        assertTrue(destKeys(rig, 3).keys.none { it.startsWith("3.previous.") })
    }

    @Test
    fun `SAVE onto the active slot keeps its own Back snapshot`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        assertTrue(rig.saves.hasPreviousState(1))

        rig.saves.commitSlotSave(1, source)
        rig.device.flushApplies()

        assertTrue(rig.saves.hasPreviousState(1))
        assertEquals(source, rig.saves.load(1))
    }

    @Test
    fun `playing on after a copy gives Back the copied game, never the old destination`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        val moved = rig.saves.commitSlotSave(2, source)
        rig.device.flushApplies()

        val next = rig.saves.commitChoice(moved, forward.copy(nextNodeId = "somewhere_else"), null)
        val back = rig.saves.restorePreviousState(2)!!

        assertEquals(moved.sceneId, back.sceneId)
        assertEquals(setOf("SourceTrophy"), back.trophies)
        assertEquals(99, back.gold)
        assertEquals("somewhere_else", next.sceneId)
    }

    @Test
    fun `repeating the same copy leaves the same disk state`() {
        val rig = newRig()
        val (_, source) = twoGames(rig)
        rig.saves.commitSlotSave(2, source)
        rig.device.flushApplies()
        val once = rig.device.disk(Story.LIVE)

        rig.saves.commitSlotSave(2, source)
        rig.device.flushApplies()

        assertEquals(once, rig.device.disk(Story.LIVE))
    }

    // ---- the timeline keys travel in the same batch ------------------------------------------------------------

    @Test
    fun `the destination's old ledger goes in the same batch as its old state is replaced`() {
        val dev = SimulatedDevice().also { it.asyncApply = true }
        val rig = Rig(dev)
        rig.playTo(5, slot = 2)
        rig.playTo(3, slot = 1)
        dev.flushApplies()
        val oldTimeline = rig.timeline(2)
        assertTrue(oldTimeline != null)
        val source = rig.saves.load(1)!!

        rig.saves.commitSlotSave(2, source)
        dev.dropQueuedApplies()
        val lost = rig.reopen()
        assertEquals("copy lost: the old game keeps its own timeline", oldTimeline, lost.timeline(2))
        assertEquals((2..5).toList(), lost.usable(2))

        val rig2 = Rig(SimulatedDevice().also { it.asyncApply = true })
        rig2.playTo(5, slot = 2)
        rig2.playTo(3, slot = 1)
        rig2.device.flushApplies()
        rig2.saves.commitSlotSave(2, rig2.saves.load(1)!!)
        rig2.device.flushApplies()
        val landed = rig2.reopen()
        assertTrue("copy landed: the old ledger is gone with the old state", landed.timeline(2) == null)
        assertTrue(landed.usable(2).isEmpty())
    }
}
