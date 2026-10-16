package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitRestart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PR #120 review hardening: (1) a lost Chapter 1 genesis record is rebuilt, but only from the exact fresh
 * state its ledger digest names; (2) a state that has moved on costs no checkpoint I/O, while a record that
 * is legitimately missing stays recoverable (no cached failure can turn a temporary problem permanent).
 */
class ArrivalHardeningTest {
    private val ckptFile = Cp.file(1)
    private val fresh = GameState(activeSlot = 1)
    private val straightOn = Choice("straight on", Cp.startScene(2))

    private fun asyncRig(device: SimulatedDevice = SimulatedDevice()): Rig = Rig(device.also { it.asyncApply = true })

    private fun tap(rig: Rig) {
        val live = rig.saves.load(1)!!
        rig.saves.commitChoice(live, Story.staying(ChapterIds.numberOf(live.chapterId)!!).second, rig.recorder)
    }

    private fun dropChapterOneFromFile(device: SimulatedDevice) {
        CheckpointCodec.keysOfGroup(device.disk(ckptFile).keys, 1).forEach { device.removeDiskKey(ckptFile, it) }
    }

    // ---- 1. genesis recovery -------------------------------------------------------------------------

    @Test
    fun `a crash on the first transition that loses both records rebuilds Chapter 1 and Chapter 2 on the first tap`() {
        val rig = asyncRig()
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        try {
            rig.saves.commitChoice(fresh, straightOn, rig.recorder)
        } catch (e: SimulatedCrash) {
        }
        rig.device.flushApplies() // the live batch (state + ledger {1,2}) reached disk before the process died

        val re = rig.reopen()
        val arrival = re.saves.load(1)!!
        assertEquals(listOf(1, 2), re.timeline()!!.ledger.chapters())
        assertTrue("nothing is usable before the repair", re.usable().isEmpty())

        tap(re)

        val lineage = re.timeline()!!.lineage
        assertEquals(listOf(1, 2), re.usable())
        assertEquals(expectedRecord(fresh, 1, lineage), re.usableRecord(1))
        assertEquals(expectedRecord(arrival, 2, lineage), re.usableRecord(2))
    }

    @Test
    fun `genesis is rebuilt even after the player has moved on and the Chapter 2 record can no longer be proven`() {
        val rig = asyncRig()
        rig.device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED) // every checkpoint write fails
        val arrival = rig.saves.commitChoice(fresh, straightOn, rig.recorder)
        tap(rig)
        tap(rig)
        rig.device.flushApplies()
        rig.device.clearFaults()

        val re = rig.reopen() // the in-memory retry list is gone
        assertEquals(listOf(1, 2), re.timeline()!!.ledger.chapters())

        tap(re)

        val lineage = re.timeline()!!.lineage
        assertEquals("only what can be proven comes back", listOf(1), re.usable())
        assertEquals(expectedRecord(fresh, 1, lineage), re.usableRecord(1))
        assertTrue("the moved-on arrival state is not guessed", arrival != re.saves.load(1))
    }

    @Test
    fun `a missing genesis next to valid later records is added without disturbing them`() {
        val rig = Rig()
        rig.saves.commitChoice(fresh, straightOn, rig.recorder)
        rig.playTo(5, tag = "a").let { }
        val before = rig.usable()
        val two = rig.usableRecord(2)
        val five = rig.usableRecord(5)
        dropChapterOneFromFile(rig.device)

        val re = rig.reopen()
        assertEquals(before - 1, re.usable())
        tap(re)

        assertEquals(before, re.usable())
        assertEquals(two, re.usableRecord(2))
        assertEquals(five, re.usableRecord(5))
        assertEquals(expectedRecord(fresh, 1, re.timeline()!!.lineage), re.usableRecord(1))
    }

    @Test
    fun `a Chapter 1 record that came from the Chapter 30 loop is never rebuilt`() {
        val (node, choice) = Story.loops().first()
        val rig = Rig()
        rig.playTo(30)
        rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)
        assertEquals(listOf(1), rig.timeline()!!.ledger.chapters())
        assertEquals(listOf(1), rig.usable())
        tap(rig) // the player moves on, so the loop's arrival state can no longer be proven
        rig.device.wipeDisk(ckptFile)

        val re = rig.reopen()
        tap(re)
        tap(re)

        assertTrue("no invented Chapter 1 state", re.fileRecords().isEmpty())
        assertTrue(re.usable().isEmpty())
    }

    @Test
    fun `a legacy Chapter 1 save gets no genesis even though other repairs run`() {
        val rig = Rig()
        val mid = Story.stateAt(Story.staying(1).first, 1, "legacy")
        rig.saves.save(1, mid) // a save written before this feature: no timeline at all
        rig.saves.commitChoice(mid, Story.staying(1).second, rig.recorder)
        assertTrue(rig.timeline() == null)
        assertTrue(rig.fileRecords().isEmpty())
    }

    @Test
    fun `Start Again genesis is rebuilt from the fresh state`() {
        val rig = Rig()
        rig.playTo(4)
        rig.saves.commitRestart(1, rig.recorder)
        rig.device.wipeDisk(ckptFile)

        val re = rig.reopen()
        assertTrue(re.usable().isEmpty())
        tap(re)

        assertEquals(listOf(1), re.usable())
        assertEquals(expectedRecord(fresh, 1, re.timeline()!!.lineage), re.usableRecord(1))
    }

    @Test
    fun `genesis repair respects a newer format file`() {
        val rig = asyncRig()
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        try {
            rig.saves.commitChoice(fresh, straightOn, rig.recorder)
        } catch (e: SimulatedCrash) {
        }
        rig.device.flushApplies()
        rig.device.writeDisk(
            ckptFile,
            mapOf("h.version" to CHECKPOINT_FORMAT_VERSION + 1, "h.slot" to 1, "h.lineage" to Cp.L1, "c05.future" to "data", "c05.crc" to 7L)
        )
        val untouched = rig.device.disk(ckptFile)

        val re = rig.reopen()
        tap(re)
        tap(re)

        assertEquals(untouched, re.device.disk(ckptFile))
        assertTrue(re.recorder!!.isDisabledForSlot(1))
    }

    // ---- 2. no repeated failed reconciliation I/O, and nothing cached that blocks recovery -------------

    @Test
    fun `a state that has moved on costs no checkpoint reads or writes`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        var counting: CountingPrefs? = null
        val first = Rig(device)
        first.device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED)
        first.playTo(9)
        first.arrive(9)
        tap(first)
        device.flushApplies()
        device.clearFaults()

        val rig = Rig(device.also { it.restart(); it.asyncApply = true }, ckptStore = Cp.storeOver(device) { slot ->
            CountingPrefs(device.prefs(Cp.file(slot))).also { if (slot == 1) counting = it }
        })
        repeat(6) { tap(rig) } // the record for chapter 10 cannot be proven any more
        assertEquals(0, counting?.touches ?: 0)
        assertTrue(10 !in rig.usable())
    }

    @Test
    fun `Back to the arrival state still repairs the record after a moved-on stretch`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val first = Rig(device)
        first.playTo(9)
        device.flushApplies()
        device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED)
        val (_, arrival) = first.arrive(9)
        tap(first) // moves on; its snapshot is the arrival state
        device.flushApplies()
        device.clearFaults()

        val re = first.reopen()
        assertTrue(10 !in re.usable())
        re.saves.restorePreviousState(1) // Back: the snapshot the tap took is the arrival state itself
        assertEquals(arrival, re.saves.load(1))

        tap(re)

        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(arrival, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    @Test
    fun `a temporary checkpoint failure heals once the store works again`() {
        val rig = asyncRig()
        rig.playTo(9)
        rig.device.flushApplies()
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        try {
            rig.arrive(9)
        } catch (e: SimulatedCrash) {
        }
        rig.device.flushApplies()
        val re = rig.reopen()
        re.device.armFault(ckptFile, 1, CommitFault.FALSE_UNCHANGED) // one commit fails, the rollback commit works
        tap(re) // repair attempt fails; the state moves on
        assertTrue(10 !in re.usable())

        tap(re) // retry of the frozen record, still vouched for by the ledger

        assertEquals((2..10).toList(), re.usable())
    }

    @Test
    fun `a record the file no longer holds is not treated as present after another route replaced it`() {
        val rig = Rig()
        rig.playTo(9)
        val (_, viaA) = rig.arrive(9, tag = "a") // file holds A's chapter 10, remembered as present
        rig.saves.restorePreviousState(1)
        rig.arrive(9, tag = "b") // replaces it with B's
        rig.saves.restorePreviousState(1)
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        try {
            rig.arrive(9, tag = "a") // the ledger vouches for A again, but the file still holds B's
        } catch (e: SimulatedCrash) {
        }
        rig.device.clearFaults()

        tap(rig) // live state is A's arrival state again: the record must be repaired, not assumed present

        assertEquals(expectedRecord(viaA, 10, rig.timeline()!!.lineage), rig.usableRecord(10))
    }

    // ---- the stale-lineage file (review item 3) ----------------------------------------------------------

    @Test
    fun `a file from another lineage is replaced by the retry - the arrival heals, older chapters stay missing, nothing stale is trusted`() {
        val rig = Rig()
        rig.playTo(5)
        val live = rig.timeline()!!.lineage
        // the file belongs to an older timeline (for example an earlier whole-file write never landed)
        assertEquals(StoreWriteResult.Committed, rig.ckpt.replaceAll(1, Cp.L1, Cp.records(5, Cp.L1)))
        assertTrue(live != Cp.L1)
        assertTrue(rig.usable().isEmpty())

        rig.arrive(5) // replaceFrom is refused (wrong lineage) and the record is kept for a retry
        assertTrue(6 !in rig.usable())
        assertTrue("the stale file was not touched by the refused write", rig.ckpt.storedLineage(1) == Cp.L1)

        tap(rig) // the retry uses a whole-file replace, because nothing in the file belongs to this timeline

        assertEquals(listOf(6), rig.usable())
        assertEquals(live, rig.ckpt.storedLineage(1))
        assertEquals("only the proven record; the old timeline's records are gone, not trusted", listOf(6), rig.fileRecords().map { it.chapterNumber })

        // later arrivals extend it incrementally again
        rig.arrive(6)
        assertEquals(listOf(6, 7), rig.usable())
    }

    @Test
    fun `after a restart a stale-lineage file still heals at the next arrival`() {
        val rig = Rig()
        rig.playTo(5)
        rig.ckpt.replaceAll(1, Cp.L1, Cp.records(5, Cp.L1))
        val re = rig.reopen() // nothing remembered; the ledger vouches for records the file does not have
        assertTrue(re.usable().isEmpty())

        val (_, arrival) = re.arrive(5)
        tap(re)

        assertEquals(listOf(6), re.usable())
        assertEquals(expectedRecord(arrival, 6, re.timeline()!!.lineage), re.usableRecord(6))
    }
}
