package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitCombatResult
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitPurchase
import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The transition table: what each kind of move does to the live timeline and the checkpoint file (T6, T7). */
class ArrivalRecorderTest {
    private val ckptFile = Cp.file(1)

    // ---- same chapter / forward arrivals ------------------------------------------------------------

    @Test
    fun `a same chapter choice does no checkpoint io and leaves the timeline alone`() {
        val rig = Rig()
        rig.playTo(5)
        val commits = rig.device.commitCount(ckptFile)
        val timeline = rig.timeline()
        val fileBefore = rig.ckptFile()
        val (node, choice) = Story.staying(5)

        rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)

        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertEquals(timeline, rig.timeline())
        assertEquals(fileBefore, rig.ckptFile())
    }

    @Test
    fun `a same chapter choice on a save with no timeline writes no key and creates no checkpoint file`() {
        val rig = Rig()
        val (node, choice) = Story.staying(3)
        rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)
        assertNull(rig.timeline())
        assertFalse(rig.device.fileNames().contains(ckptFile))
        assertFalse(rig.liveFile().keys.any { it.endsWith(".lineage") || it.endsWith(".arrivals") })
    }

    @Test
    fun `the first forward arrival mints a lineage and writes one record built from the arrival state`() {
        val rig = Rig()
        val (_, after) = rig.arrive(5)

        val tl = rig.timeline()!!
        assertTrue(Lineage.isWellFormed(tl.lineage))
        assertEquals(listOf(6), tl.ledger.chapters())
        val expected = expectedRecord(after, 6, tl.lineage)
        assertEquals(listOf(expected), rig.fileRecords())
        assertEquals(CheckpointDigest.of(1, expected), tl.ledger.digestFor(6))
        assertEquals(listOf(6), rig.usable())
        assertEquals(Cp.chapterId(6), after.chapterId)
    }

    @Test
    fun `later arrivals extend the ledger and the file together`() {
        val rig = Rig()
        val arrivals = rig.playTo(6)
        val tl = rig.timeline()!!
        assertEquals(listOf(2, 3, 4, 5, 6), tl.ledger.chapters())
        assertEquals(arrivals.map { (n, s) -> expectedRecord(s, n, tl.lineage) }, rig.fileRecords())
        assertEquals(listOf(2, 3, 4, 5, 6), rig.usable())
    }

    @Test
    fun `an arrival drops the ledger and the file from its chapter upward and replaces that chapter`() {
        val rig = Rig()
        val first = rig.playTo(5)
        val oldLineage = rig.timeline()!!.lineage

        val (_, again) = rig.arrive(3, tag = "b") // chapter 3 -> 4 by a different route

        val tl = rig.timeline()!!
        assertEquals(oldLineage, tl.lineage)
        assertEquals(listOf(2, 3, 4), tl.ledger.chapters())
        assertEquals(
            listOf(expectedRecord(first.getValue(2), 2, oldLineage), expectedRecord(first.getValue(3), 3, oldLineage), expectedRecord(again, 4, oldLineage)),
            rig.fileRecords()
        )
        assertEquals(listOf(2, 3, 4), rig.usable())
    }

    // ---- recording off (the kill switch) -------------------------------------------------------------

    @Test
    fun `with recording off an arrival still drops the abandoned ledger entries and never touches the file`() {
        val on = Rig()
        on.playTo(5)
        val off = on.reopen(enabled = false)
        val commits = off.device.commitCount(ckptFile)
        val fileBefore = off.ckptFile()

        off.arrive(2, tag = "b") // re-arrive in chapter 3 by another route

        assertEquals(listOf(2), off.timeline()!!.ledger.chapters())
        assertEquals(commits, off.device.commitCount(ckptFile))
        assertEquals(fileBefore, off.ckptFile())

        // recording comes back: the stale chapter 3/4/5 records in the file are not vouched for
        val back = off.reopen(enabled = true)
        assertEquals(listOf(2), back.usable())
    }

    @Test
    fun `with recording off a save without a timeline stays without one and no file appears`() {
        val rig = Rig(enabled = false)
        rig.arrive(5)
        assertNull(rig.timeline())
        assertFalse(rig.device.fileNames().contains(ckptFile))
    }

    @Test
    fun `with recording off Start Again style invalidation still happens`() {
        val on = Rig()
        on.playTo(4)
        val off = on.reopen(enabled = false)
        val s = com.thelastjailer.app.GameState(activeSlot = 1)
        off.saves.commitRestart(1, off.recorder)
        assertNull(off.timeline())
        assertTrue("no record may be usable", off.usable().isEmpty())
        assertEquals(1, s.activeSlot)
    }

    // ---- the Chapter 30 loop --------------------------------------------------------------------------

    @Test
    fun `there are exactly six loop endings`() {
        assertEquals(6, Story.loops().size)
    }

    @Test
    fun `each Chapter 30 to Chapter 1 loop starts a new timeline whose only record is the carried over state`() {
        for ((node, choice) in Story.loops()) {
            val rig = Rig()
            rig.playTo(30)
            val oldTimeline = rig.timeline()!!
            assertEquals((2..30).toList(), oldTimeline.ledger.chapters())

            val before = Story.stateAt(node)
            val after = rig.saves.commitChoice(before, choice, rig.recorder)

            val tl = rig.timeline()!!
            assertTrue(node.id, tl.lineage != oldTimeline.lineage)
            assertEquals(node.id, listOf(1), tl.ledger.chapters())
            assertEquals(node.id, listOf(expectedRecord(after, 1, tl.lineage)), rig.fileRecords())
            assertEquals("carried over trophies", after.trophies, rig.fileRecords().single().trophies)
            assertEquals(Cp.chapterId(1), after.chapterId)
            assertEquals(listOf(1), rig.usable())
        }
    }

    @Test
    fun `a loop with recording off just invalidates`() {
        val (node, choice) = Story.loops().first()
        val on = Rig()
        on.playTo(30)
        val off = on.reopen(enabled = false)
        off.saves.commitChoice(Story.stateAt(node), choice, off.recorder)
        assertNull(off.timeline())
        assertTrue(off.usable().isEmpty())
    }

    // ---- unknown / unexpected chapter changes ---------------------------------------------------------

    @Test
    fun `an unknown chapter invalidates instead of guessing`() {
        val rig = Rig()
        rig.playTo(4)
        val fileBefore = rig.ckptFile()
        val (node, _) = Story.staying(4)
        val broken = Story.stateAt(node).copy(sceneId = "no_such_scene", chapterId = "garbage")

        val next = rig.saves.commitChoice(broken, Choice("lost", "also_missing"), rig.recorder)

        assertEquals("also_missing", next.sceneId)
        assertNull(rig.timeline())
        assertEquals(fileBefore, rig.ckptFile())
    }

    @Test
    fun `any backward move other than the Chapter 30 loop invalidates`() {
        val rig = Rig()
        rig.playTo(8)
        val (node, _) = Story.staying(7)
        rig.saves.commitChoice(Story.stateAt(node), Choice("back to three", Cp.startScene(3)), rig.recorder)
        assertNull(rig.timeline())
        assertTrue(rig.usable().isEmpty())
    }

    // ---- the first choice of a fresh game (D2) --------------------------------------------------------

    private val fresh = GameState(activeSlot = 1)
    private val firstChoice: Choice get() = StoryRepository.node(fresh.sceneId).choices.first()

    @Test
    fun `the first choice of a fresh game records Chapter 1 from the state before the choice`() {
        assertEquals("the first choice stays in chapter 1", Cp.chapterId(1), StoryRepository.node(firstChoice.nextNodeId).chapterId)
        val rig = Rig()
        val after = rig.saves.commitChoice(fresh, firstChoice, rig.recorder)

        val tl = rig.timeline()!!
        assertEquals(listOf(1), tl.ledger.chapters())
        val record = rig.fileRecords().single()
        assertEquals(expectedRecord(fresh, 1, tl.lineage), record)
        assertEquals("the pre-choice scene, not the scene after the choice", fresh.sceneId, record.sceneId)
        assertTrue(after.sceneId != record.sceneId)
        assertEquals(listOf(1), rig.usable())
    }

    @Test
    fun `genesis happens once - later choices in Chapter 1 leave it alone`() {
        val rig = Rig()
        val after = rig.saves.commitChoice(fresh, firstChoice, rig.recorder)
        val commits = rig.device.commitCount(ckptFile)
        val timeline = rig.timeline()
        val next = StoryRepository.node(after.sceneId).choices.first { StoryRepository.node(it.nextNodeId).chapterId == Cp.chapterId(1) }

        rig.saves.commitChoice(after, next, rig.recorder)

        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertEquals(timeline, rig.timeline())
    }

    @Test
    fun `a legacy save in the middle of Chapter 1 gets no genesis record`() {
        val rig = Rig()
        rig.saves.commitChoice(fresh.copy(gold = fresh.gold + 1), firstChoice, rig.recorder)
        assertNull(rig.timeline())
        assertFalse(rig.device.fileNames().contains(ckptFile))
    }

    @Test
    fun `genesis is per slot`() {
        val rig = Rig()
        rig.saves.commitChoice(GameState(activeSlot = 2), firstChoice, rig.recorder)
        assertNull(rig.timeline(1))
        assertNotNull(rig.timeline(2))
        assertEquals(listOf(1), rig.usable(2))
        assertFalse(rig.device.fileNames().contains(Cp.file(1)))
    }

    @Test
    fun `a first choice that goes straight into Chapter 2 keeps both the Chapter 1 and the Chapter 2 record`() {
        val rig = Rig()
        val toChapterTwo = Choice("straight on", Cp.startScene(2))
        val after = rig.saves.commitChoice(fresh, toChapterTwo, rig.recorder)

        assertEquals(Cp.chapterId(2), after.chapterId)
        val tl = rig.timeline()!!
        assertEquals(listOf(1, 2), tl.ledger.chapters())
        assertEquals(
            listOf(expectedRecord(fresh, 1, tl.lineage), expectedRecord(after, 2, tl.lineage)),
            rig.fileRecords()
        )
        assertEquals(listOf(1, 2), rig.usable())

        // and the next arrival keeps both
        rig.arrive(2)
        assertEquals(listOf(1, 2, 3), rig.timeline()!!.ledger.chapters())
        assertEquals(listOf(1, 2, 3), rig.usable())
        assertEquals(expectedRecord(fresh, 1, tl.lineage), rig.usableRecord(1))
        assertEquals(expectedRecord(after, 2, tl.lineage), rig.usableRecord(2))
    }

    @Test
    fun `if that double arrival write fails the next commit still lands both records even after the state moved on`() {
        val rig = Rig()
        val after = rig.saves.commitChoice(fresh, Choice("straight on", Cp.startScene(2)), rig.recorder).also { }
        // (the commit above succeeded; redo the scenario with the checkpoint commit failing)
        val failing = Rig()
        failing.device.armFault(ckptFile, 1, CommitFault.FALSE_UNCHANGED)
        val arrived = failing.saves.commitChoice(fresh, Choice("straight on", Cp.startScene(2)), failing.recorder)
        val tl = failing.timeline()!!
        assertEquals("the game moved on regardless", Cp.chapterId(2), arrived.chapterId)
        assertEquals(listOf(1, 2), tl.ledger.chapters())
        assertTrue("nothing usable yet", failing.usable().isEmpty())
        assertEquals(after.sceneId, arrived.sceneId)

        val (node, choice) = Story.staying(2)
        failing.saves.commitChoice(arrived.copy(sceneId = node.id, flags = arrived.flags + "moved_on"), choice, failing.recorder)

        assertEquals(listOf(1, 2), failing.usable())
        assertEquals(expectedRecord(fresh, 1, tl.lineage), failing.usableRecord(1))
        assertEquals("the original arrival state, not the state after moving on", expectedRecord(arrived, 2, tl.lineage), failing.usableRecord(2))
    }

    // ---- other commits ----------------------------------------------------------------------------------

    @Test
    fun `combat and purchases never arrive and keep the timeline`() {
        val chapter = (2..30).first { n -> StoryRepository.nodesInChapter(Cp.chapterId(n)).any { it.combatEncounterId != null } }
        val combatNode = StoryRepository.nodesInChapter(Cp.chapterId(chapter)).sortedBy { it.id }.first { it.combatEncounterId != null }
        val rig = Rig()
        rig.playTo(chapter)
        val timeline = rig.timeline()
        val commits = rig.device.commitCount(ckptFile)

        val inFight = Story.stateAt(combatNode)
        val won = rig.saves.commitCombatResult(
            inFight, CombatRepository.encounter(combatNode.combatEncounterId!!),
            CombatOutcome(victory = true, damageTaken = 0), rig.recorder
        )
        assertEquals(timeline, rig.timeline())
        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertFalse("combat is still irreversible", rig.saves.hasPreviousState(1))

        rig.saves.commitPurchase(won.copy(gold = 500), "healing_draught", 10, rig.recorder)
        assertEquals(timeline, rig.timeline())
        assertEquals(commits, rig.device.commitCount(ckptFile))
        assertFalse(rig.saves.hasPreviousState(1))
    }

    @Test
    fun `without a recorder the commits invalidate the timeline - fail safe`() {
        val rig = Rig()
        rig.playTo(5)
        assertNotNull(rig.timeline())
        val (node, choice) = Story.staying(5)
        val plain = Rig(rig.device, withRecorder = false)
        plain.saves.commitChoice(Story.stateAt(node), choice)
        assertNull(plain.timeline())
        assertTrue(plain.usable().isEmpty())
    }
}
