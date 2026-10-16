package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitPurchase
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitSlotSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How Back, Start Again, SAVE, LOAD and partial restores interact with the ledger (T10-T14, open item O2). */
class ArrivalTimelineInteractionTest {
    private val ckptFile = Cp.file(1)

    /** Chapters 2..9 reached, then an arrival into chapter 10 by route "a". */
    private fun atTen(rig: Rig = Rig(), tag: String = "a"): Triple<Rig, GameState, GameState> {
        if (rig.timeline() == null) rig.playTo(9)
        val (before, after) = rig.arrive(9, tag = tag)
        return Triple(rig, before, after)
    }

    // ---- Back --------------------------------------------------------------------------------------------

    @Test
    fun `Back across a chapter boundary leaves the ledger and the checkpoint file exactly as they were`() {
        val (rig, before, _) = atTen()
        val timeline = rig.timeline()
        val file = rig.ckptFile()
        val commits = rig.device.commitCount(ckptFile)

        val restored = rig.saves.restorePreviousState(1)!!

        assertEquals(Cp.chapterId(9), restored.chapterId)
        assertEquals(before.sceneId, restored.sceneId)
        assertEquals(timeline, rig.timeline())
        assertEquals(file, rig.ckptFile())
        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertEquals("chapter 10 is invisible while the player is back in 9", (2..9).toList(), rig.usable())
    }

    @Test
    fun `Back then a different route into the same chapter replaces the record and the ledger entry`() {
        val (rig, _, _) = atTen(tag = "a")
        rig.saves.restorePreviousState(1)
        val (_, viaB) = rig.arrive(9, tag = "b")

        val tl = rig.timeline()!!
        assertEquals((2..10).toList(), tl.ledger.chapters())
        assertEquals(expectedRecord(viaB, 10, tl.lineage), rig.usableRecord(10))
        assertEquals((2..10).toList(), rig.fileRecords().map { it.chapterNumber })
    }

    @Test
    fun `Back then route B where B's live write is lost but its checkpoint landed - route A is rebuilt not B`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val rig = Rig(device)
        rig.playTo(9); device.flushApplies()
        val (_, viaA) = rig.arrive(9, tag = "a"); device.flushApplies()
        rig.saves.restorePreviousState(1) // queued, then lost
        rig.arrive(9, tag = "b")          // checkpoint B lands, live batch queued, then lost
        assertEquals("checkpoint B is on disk", 10, rig.fileRecords().last().chapterNumber)

        val re = rig.reopen()
        assertEquals("live is still route A in chapter 10", Cp.chapterId(10), re.saves.load(1)!!.chapterId)
        assertEquals("the file holds B but the ledger vouches for A: not usable", (2..9).toList(), re.usable())

        val live = re.saves.load(1)!!
        re.saves.commitChoice(live, Story.staying(10).second, re.recorder)
        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(viaA, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    @Test
    fun `Back then route B where B's live write landed but its checkpoint was lost - route B is rebuilt not A`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val rig = Rig(device)
        rig.playTo(9); device.flushApplies()
        rig.arrive(9, tag = "a"); device.flushApplies()
        rig.saves.restorePreviousState(1)
        device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        val (before, choice) = Story.crossing(9).let { (n, c) -> Story.stateAt(n, 1, "b") to c }
        try { rig.saves.commitChoice(before, choice, rig.recorder) } catch (e: SimulatedCrash) { }
        device.flushApplies() // the live batch (Back and route B) did reach disk

        val re = rig.reopen()
        assertEquals("the ledger vouches for B but the file holds A", (2..9).toList(), re.usable())
        val live = re.saves.load(1)!!
        re.saves.commitChoice(live, Story.staying(10).second, re.recorder)
        assertEquals((2..10).toList(), re.usable())
        assertTrue("route B's flag", re.usableRecord(10)!!.flags.contains("b"))
        assertFalse("not route A", re.usableRecord(10)!!.flags.contains("a"))
    }

    @Test
    fun `Back then a purchase keeps the ledger and does not touch the file`() {
        val (rig, _, _) = atTen()
        rig.saves.restorePreviousState(1)
        val timeline = rig.timeline()
        val file = rig.ckptFile()
        val commits = rig.device.commitCount(ckptFile)

        val live = rig.saves.load(1)!!
        rig.saves.commitPurchase(live.copy(gold = 99), "healing_draught", 5, rig.recorder)

        assertEquals(timeline, rig.timeline())
        assertEquals(file, rig.ckptFile())
        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertEquals((2..9).toList(), rig.usable())
    }

    @Test
    fun `Back then SAVE into another slot invalidates the destination only`() {
        val (rig, _, _) = atTen()
        rig.playTo(4, slot = 2)
        val source = rig.timeline(1)
        val destFile = rig.ckptFile(2)
        rig.saves.restorePreviousState(1)
        val live = rig.saves.load(1)!!

        val copied = rig.saves.commitSlotSave(2, live)

        assertEquals(2, copied.activeSlot)
        assertNull(rig.timeline(2))
        assertEquals("the old file is left alone", destFile, rig.ckptFile(2))
        assertTrue("and nothing in it is usable", rig.usable(2).isEmpty())
        assertEquals(source, rig.timeline(1))
        assertEquals((2..9).toList(), rig.usable(1))
    }

    @Test
    fun `Back then Start Again begins a new timeline with only the Chapter 1 record`() {
        val (rig, _, _) = atTen()
        val oldLineage = rig.timeline()!!.lineage
        rig.saves.restorePreviousState(1)

        val fresh = rig.saves.commitRestart(1, rig.recorder)

        val tl = rig.timeline()!!
        assertTrue(tl.lineage != oldLineage)
        assertEquals(listOf(1), tl.ledger.chapters())
        assertEquals(listOf(expectedRecord(fresh, 1, tl.lineage)), rig.fileRecords())
        assertEquals(listOf(1), rig.usable())
    }

    @Test
    fun `Back then route B then Back then route C ends with route C's record`() {
        val (rig, _, _) = atTen(tag = "a")
        rig.saves.restorePreviousState(1)
        rig.arrive(9, tag = "b")
        rig.saves.restorePreviousState(1)
        val (_, viaC) = rig.arrive(9, tag = "c")
        assertEquals(expectedRecord(viaC, 10, rig.timeline()!!.lineage), rig.usableRecord(10))
        assertEquals(1, rig.fileRecords().count { it.chapterNumber == 10 })
    }

    @Test
    fun `an abandoned branch cannot come back through an arrival made while recording was off`() {
        val (rig, _, _) = atTen(tag = "a")
        rig.saves.restorePreviousState(1)
        val off = rig.reopen(enabled = false)
        off.arrive(9, tag = "b") // chapter 10 again, by route B, with recording off
        assertEquals((2..9).toList(), off.timeline()!!.ledger.chapters())

        val on = off.reopen(enabled = true)
        assertEquals("the old record 10 is still in the file but nothing vouches for it", Cp.chapterId(10), on.saves.load(1)!!.chapterId)
        assertEquals((2..9).toList(), on.usable())
        assertTrue(on.fileRecords().any { it.chapterNumber == 10 })
    }

    // ---- Start Again / SAVE / LOAD ---------------------------------------------------------------------

    @Test
    fun `Start Again replaces the timeline and the file in one go`() {
        val (rig, _, _) = atTen()
        val oldLineage = rig.timeline()!!.lineage
        val fresh = rig.saves.commitRestart(1, rig.recorder)
        val tl = rig.timeline()!!
        assertTrue(tl.lineage != oldLineage)
        assertEquals(listOf(1), rig.fileRecords().map { it.chapterNumber })
        assertEquals(expectedRecord(fresh, 1, tl.lineage), rig.usableRecord(1))
        assertFalse(rig.saves.hasPreviousState(1))
    }

    @Test
    fun `Start Again after a restart of the app still leaves nothing from the old timeline usable`() {
        val (rig, _, _) = atTen()
        val oldFile = rig.ckptFile()
        rig.saves.commitRestart(1, rig.recorder)
        // put the old checkpoint file back, as an out-of-order restore would
        rig.device.writeDisk(ckptFile, oldFile)
        val re = rig.reopen()
        assertTrue("the old file belongs to another lineage", re.usable().isEmpty())
    }

    @Test
    fun `SAVE onto another slot gives that slot a fresh empty timeline and keeps the source intact`() {
        val rig = Rig()
        rig.playTo(5)
        rig.playTo(7, slot = 2)
        val source = rig.timeline(1)
        val destFile = rig.ckptFile(2)
        val state = rig.saves.load(1)!!

        val moved = rig.saves.commitSlotSave(2, state)

        assertEquals(rig.saves.load(2)!!.sceneId, moved.sceneId)
        assertNull(rig.timeline(2))
        assertEquals(destFile, rig.ckptFile(2))
        assertTrue(rig.usable(2).isEmpty())
        assertEquals(source, rig.timeline(1))
        assertEquals((2..5).toList(), rig.usable(1))
    }

    @Test
    fun `SAVE onto the active slot keeps its timeline`() {
        val (rig, _, _) = atTen()
        val timeline = rig.timeline()
        val state = rig.saves.load(1)!!
        rig.saves.commitSlotSave(1, state)
        assertEquals(timeline, rig.timeline())
        assertEquals((2..10).toList(), rig.usable())
    }

    @Test
    fun `LOAD and switching the active slot write nothing to any timeline or checkpoint file`() {
        val rig = Rig()
        rig.playTo(5)
        rig.playTo(6, slot = 2)
        val disks = rig.device.allDisks().filterKeys { it.startsWith("jailer_ckpt") }
        val keys = { s: Int -> rig.timeline(s) }
        val before = keys(1) to keys(2)

        rig.saves.setActiveSlot(2)
        rig.saves.load(1); rig.saves.load(2); rig.saves.currentActiveSlot()

        assertEquals(disks, rig.device.allDisks().filterKeys { it.startsWith("jailer_ckpt") })
        assertEquals(before, keys(1) to keys(2))
    }

    // ---- partial restores (Android Auto Backup can bring back one file but not the other) ------------------

    @Test
    fun `a checkpoint file restored from an older point can only lose records never mislead`() {
        val rig = Rig()
        rig.playTo(6)
        val oldFile = rig.ckptFile()
        val oldRecords = rig.fileRecords().associateBy { it.chapterNumber }
        rig.playTo(9) // plays on, re-arriving 2..9 by the same route: records are identical
        rig.arrive(6, tag = "z")
        rig.device.writeDisk(ckptFile, oldFile)

        val re = rig.reopen()
        re.usable().forEach { c -> assertEquals(oldRecords.getValue(c), re.usableRecord(c)) }
        assertTrue(re.usable().all { it <= 7 })
    }

    @Test
    fun `a live save restored from an older point cannot make newer records usable`() {
        val rig = Rig()
        rig.playTo(6)
        val oldLive = rig.liveFile()
        rig.arrive(6, tag = "n") // chapter 7 by a new route; live and file both move on
        rig.device.writeDisk(Story.LIVE, oldLive)

        val re = rig.reopen()
        assertEquals(Cp.chapterId(6), re.saves.load(1)!!.chapterId)
        assertEquals((2..6).toList(), re.usable())
        assertFalse(7 in re.usable())
    }

    @Test
    fun `a checkpoint file from another timeline never matches`() {
        val rig = Rig()
        rig.playTo(6)
        val stolen = rig.ckptFile()
        rig.saves.commitRestart(1, rig.recorder)
        rig.playTo(6, tag = "second")
        rig.device.writeDisk(ckptFile, stolen)
        val re = rig.reopen()
        assertTrue(re.usable().isEmpty())
    }

    @Test
    fun `a checkpoint file copied from another slot never matches`() {
        val rig = Rig()
        rig.playTo(6)
        rig.playTo(6, slot = 2)
        rig.device.writeDisk(Cp.file(2), rig.ckptFile(1))
        val re = rig.reopen()
        assertTrue(re.usable(2).isEmpty())
        assertEquals((2..6).toList(), re.usable(1))
    }

    // ---- isolation ---------------------------------------------------------------------------------------

    @Test
    fun `a whole session never touches the entitlement file or another slot's files`() {
        val device = SimulatedDevice()
        device.writeDisk(Story.ENTITLEMENTS, mapOf("full_story_unlocked" to true, "slots" to 5))
        val rig = Rig(device)
        rig.playTo(4, slot = 2)
        val slot2Live = rig.liveFile().filterKeys { it.startsWith("2.") }
        val slot2Ckpt = rig.ckptFile(2)
        val entitlements = device.disk(Story.ENTITLEMENTS)

        rig.playTo(9)
        rig.arrive(9)
        rig.saves.restorePreviousState(1)
        rig.arrive(9, tag = "b")
        rig.reopen().saves.commitRestart(1)
        val loops = Story.loops().first()
        Rig(device).saves.commitChoice(Story.stateAt(loops.first), loops.second, Rig(device).recorder)

        assertEquals(entitlements, device.disk(Story.ENTITLEMENTS))
        assertEquals(slot2Live, rig.liveFile().filterKeys { it.startsWith("2.") })
        assertEquals(slot2Ckpt, rig.ckptFile(2))
        assertNotNull(rig.timeline(2))
    }
}
