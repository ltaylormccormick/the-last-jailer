package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.commitChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 2B-3 crash behaviour of Back. Live saves are written with apply(), which can be lost, and Back is two
 * separate writes (restored state, then clearing the snapshot). The merge only adds and is idempotent, so every
 * prefix of those writes leaves a state Back can safely be pressed from again, and a trophy that never reached
 * disk is never invented.
 */
class TrophyBackCrashTest {
    private val before = GameState(activeSlot = 1, sceneId = "fallen_knight", gold = 5, trophies = setOf("Held"))
    private val choice = Choice(
        "go", "the_road_away",
        consequences = Consequences(statDeltas = mapOf(StatType.GOLD to 10), setFlags = setOf("abandoned"), unlockTrophy = "New")
    )

    private fun rig(): Rig = Rig(SimulatedDevice().also { it.asyncApply = true }, withRecorder = false)

    private fun settled(rig: Rig): GameState {
        rig.saves.save(1, before)
        rig.device.flushApplies()
        val after = rig.saves.commitChoice(before, choice, null)
        rig.device.flushApplies()
        return after
    }

    @Test
    fun `Back that never reached disk leaves the trophy and a Back that still works`() {
        val rig = rig()
        settled(rig)
        rig.saves.restorePreviousState(1) // queued, then the process dies
        rig.device.dropQueuedApplies()

        val re = rig.reopen()

        assertTrue("New" in re.saves.load(1)!!.trophies)
        assertTrue(re.saves.hasPreviousState(1))
        val restored = re.saves.restorePreviousState(1)!!
        assertEquals(setOf("Held", "New"), restored.trophies)
        assertEquals("fallen_knight", restored.sceneId)
        assertEquals(5, restored.gold)
        assertFalse("abandoned" in restored.flags)
    }

    @Test
    fun `restored state landed but the snapshot was not cleared - pressing Back again changes nothing`() {
        val rig = rig()
        settled(rig)
        val preBack = rig.device.disk(Story.LIVE)
        val firstBack = rig.saves.restorePreviousState(1)!!
        rig.device.flushApplies()
        val postBack = rig.device.disk(Story.LIVE)
        // the clear never landed: the snapshot keys are back on disk next to the restored state
        rig.device.writeDisk(Story.LIVE, postBack + preBack.filterKeys { it.startsWith("1.previous.") })

        val re = rig.reopen()

        assertEquals(firstBack, re.saves.load(1))
        assertTrue(re.saves.hasPreviousState(1))
        val again = re.saves.restorePreviousState(1)!!
        assertEquals(firstBack, again)
        assertEquals(setOf("Held", "New"), again.trophies)
        assertFalse(re.saves.hasPreviousState(1))
    }

    @Test
    fun `a complete Back followed by a restart keeps the trophy and has no snapshot`() {
        val rig = rig()
        settled(rig)
        rig.saves.restorePreviousState(1)
        rig.device.flushApplies()

        val re = rig.reopen()

        assertEquals(setOf("Held", "New"), re.saves.load(1)!!.trophies)
        assertFalse(re.saves.hasPreviousState(1))
        assertNull(re.saves.restorePreviousState(1))
    }

    @Test
    fun `a trophy whose choice never reached disk is not invented by Back`() {
        val rig = rig()
        rig.saves.save(1, before)
        rig.device.flushApplies()
        val preChoice = rig.device.disk(Story.LIVE)
        rig.saves.commitChoice(before, choice, null)
        rig.device.flushApplies()
        val postChoice = rig.device.disk(Story.LIVE)
        // only the snapshot landed; the choice's autosave (with the trophy) was lost
        rig.device.writeDisk(Story.LIVE, preChoice + postChoice.filterKeys { it.startsWith("1.previous.") })

        val re = rig.reopen()

        assertEquals(setOf("Held"), re.saves.load(1)!!.trophies)
        val restored = re.saves.restorePreviousState(1)!!
        assertEquals(setOf("Held"), restored.trophies)
        assertEquals("fallen_knight", restored.sceneId)
    }

    @Test
    fun `Back writes no checkpoint file and does not touch the ledger`() {
        val rig = Rig(SimulatedDevice().also { it.asyncApply = true })
        rig.playTo(4)
        rig.device.flushApplies()
        val timeline = rig.timeline()
        val ckptBefore = rig.ckptFile()
        val commitsBefore = rig.device.commitCount(Cp.file(1))

        rig.saves.restorePreviousState(1)
        rig.device.flushApplies()

        assertEquals(timeline, rig.timeline())
        assertEquals(ckptBefore, rig.ckptFile())
        assertEquals(commitsBefore, rig.device.commitCount(Cp.file(1)))
    }
}
