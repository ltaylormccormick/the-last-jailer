package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * What survives when the live save (an asynchronous `apply()`) and the checkpoint file (a `commit()`) reach
 * disk in every combination, and how a lost record is repaired - or refused (T8, T9).
 *
 * "Live durable" means the queued live-save batch really reached disk before the process died. When it did
 * not, the game correctly resumes from the earlier durable state.
 */
class ArrivalDurabilityTest {
    private val ckptFile = Cp.file(1)

    private fun asyncRig(): Rig = Rig(SimulatedDevice().also { it.asyncApply = true })

    /** Plays to chapter 9 with everything durable, ready for the arrival into chapter 10. */
    private fun baseline(): Rig = asyncRig().also {
        it.playTo(9)
        it.device.flushApplies()
    }

    private fun tenth(): Triple<GameState, com.thelastjailer.app.Choice, GameState> {
        val (node, choice) = Story.crossing(9)
        val before = Story.stateAt(node)
        return Triple(before, choice, before.applyChoice(choice))
    }

    /** Runs the arrival; returns true when the process "died" in the middle of it. */
    private fun arriveMaybeCrash(rig: Rig): Boolean {
        val (before, choice, _) = tenth()
        return try {
            rig.saves.commitChoice(before, choice, rig.recorder)
            false
        } catch (e: SimulatedCrash) {
            true
        }
    }

    private fun tap(rig: Rig) {
        val live = rig.saves.load(1)!!
        val n = ChapterIds.numberOf(live.chapterId)!!
        rig.saves.commitChoice(live, Story.staying(n).second, rig.recorder)
    }

    // ---- the four durability combinations -------------------------------------------------------------

    @Test
    fun `neither write durable - the game resumes in chapter 9 and nothing for chapter 10 is usable`() {
        val rig = baseline()
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertTrue(arriveMaybeCrash(rig))

        val re = rig.reopen() // the queued live batch is lost with the process
        assertEquals(Cp.chapterId(9), re.saves.load(1)!!.chapterId)
        assertEquals((2..9).toList(), re.timeline()!!.ledger.chapters())
        assertEquals((2..9).toList(), re.usable())
        assertEquals((2..9).toList(), re.fileRecords().map { it.chapterNumber })
    }

    @Test
    fun `live save durable but checkpoint lost - the first tap after reopening rebuilds the original arrival record`() {
        val rig = baseline()
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        assertTrue(arriveMaybeCrash(rig))
        rig.device.flushApplies() // the live batch reached disk before the process died

        val re = rig.reopen()
        val (_, _, arrival) = tenth()
        assertEquals(Cp.chapterId(10), re.saves.load(1)!!.chapterId)
        assertEquals((2..10).toList(), re.timeline()!!.ledger.chapters())
        assertEquals("chapter 10 has no record yet", (2..9).toList(), re.usable())

        tap(re)

        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(arrival, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    @Test
    fun `checkpoint durable but live save lost - the record is invisible and a different route replaces it`() {
        val rig = baseline()
        assertFalse(arriveMaybeCrash(rig)) // the checkpoint commit lands at once; the live batch is still queued
        assertEquals((2..10).toList(), rig.fileRecords().map { it.chapterNumber })

        val re = rig.reopen() // the queued live batch is lost
        assertEquals(Cp.chapterId(9), re.saves.load(1)!!.chapterId)
        assertEquals("record 10 sits in the file but nothing vouches for it", (2..9).toList(), re.usable())

        val (_, viaB) = re.arrive(9, tag = "b")
        re.device.flushApplies()
        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(viaB, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    @Test
    fun `both durable - everything is usable`() {
        val rig = baseline()
        assertFalse(arriveMaybeCrash(rig))
        rig.device.flushApplies()
        val re = rig.reopen()
        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(tenth().third, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    @Test
    fun `every durability combination and checkpoint fault only ever yields missing records never wrong ones`() {
        val faults = listOf(
            null, CommitFault.CRASH_BEFORE_WRITE, CommitFault.CRASH_AFTER_WRITE,
            CommitFault.FALSE_UNCHANGED, CommitFault.FALSE_MEMORY_MUTATED
        )
        val arrival = tenth().third
        for (liveDurable in listOf(true, false)) for (fault in faults) {
            val label = "liveDurable=$liveDurable fault=$fault"
            val rig = baseline()
            val originals = rig.fileRecords().associateBy { it.chapterNumber }
            if (fault != null) rig.device.armFault(ckptFile, 1, fault)
            arriveMaybeCrash(rig)
            if (liveDurable) rig.device.flushApplies()

            val re = rig.reopen()
            val checkpointOnDisk = fault == null || fault == CommitFault.CRASH_AFTER_WRITE
            val expectNow = if (liveDurable && checkpointOnDisk) (2..10).toList() else (2..9).toList()
            assertEquals(label, expectNow, re.usable())
            re.usable().forEach { c ->
                val want = if (c == 10) expectedRecord(arrival, 10, re.timeline()!!.lineage) else originals.getValue(c)
                assertEquals("$label chapter $c", want, re.usableRecord(c))
            }

            tap(re) // the repair point
            val expectAfter = if (liveDurable) (2..10).toList() else (2..9).toList()
            assertEquals("$label after the first tap", expectAfter, re.usable())
            if (liveDurable) {
                assertEquals(label, expectedRecord(arrival, 10, re.timeline()!!.lineage), re.usableRecord(10))
            }
        }
    }

    // ---- rule R refuses to guess ----------------------------------------------------------------------------

    @Test
    fun `if the state moved on before the process died the record stays absent - it is never reconstructed`() {
        val rig = baseline()
        rig.device.armFaultAlways(ckptFile, CommitFault.FALSE_UNCHANGED)
        val (before, choice, arrival) = tenth()
        rig.saves.commitChoice(before, choice, rig.recorder) // checkpoint write fails, the game goes on
        val (node, stay) = Story.staying(10)
        rig.saves.commitChoice(arrival.copy(sceneId = node.id), stay, rig.recorder) // the player keeps playing
        rig.device.flushApplies()

        val re = rig.reopen()
        assertEquals((2..10).toList(), re.timeline()!!.ledger.chapters())
        tap(re)
        tap(re)
        assertEquals("only chapters 2..9 have records", (2..9).toList(), re.usable())
        assertFalse(re.fileRecords().any { it.chapterNumber == 10 })
    }

    @Test
    fun `a save that a reload would heal is refused rather than rebuilt wrongly`() {
        val rig = asyncRig()
        rig.playTo(9)
        rig.device.flushApplies()
        // an arrival state whose max health is below what the level implies: a reload raises it
        val (node, choice) = Story.crossing(9)
        val before = Story.stateAt(node).copy(maxHealth = 100, health = 90)
        rig.device.armFault(ckptFile, 1, CommitFault.CRASH_BEFORE_WRITE)
        try { rig.saves.commitChoice(before, choice, rig.recorder); fail("expected the simulated crash") } catch (e: SimulatedCrash) { }
        rig.device.flushApplies()

        val re = rig.reopen()
        val loaded = re.saves.load(1)!!
        assertTrue("the reload healed max health", loaded.maxHealth > 100)
        tap(re)
        assertEquals("absent rather than wrong", (2..9).toList(), re.usable())
        assertFalse("and no wrong record was written either", re.fileRecords().any { it.chapterNumber == 10 })
    }

    // ---- failed commits while the app stays alive ------------------------------------------------------------

    @Test
    fun `a rolled back checkpoint write is retried on the next commit with the exact frozen record`() {
        val rig = baseline()
        rig.device.armFault(ckptFile, 1, CommitFault.FALSE_MEMORY_MUTATED) // the failed write, then a good rollback
        val (before, choice, arrival) = tenth()
        val reached = rig.saves.commitChoice(before, choice, rig.recorder)
        assertEquals("the game is unaffected", arrival, reached)
        assertTrue(rig.logs.any { it.contains("did not commit") })
        assertEquals((2..9).toList(), rig.usable())

        // the player moves on within chapter 10, then the retry happens at the next commit
        val (node, stay) = Story.staying(10)
        val moved = reached.copy(sceneId = node.id, gold = reached.gold + 7)
        rig.saves.commitChoice(moved, stay, rig.recorder)

        assertEquals((2..10).toList(), rig.usable())
        assertEquals("the arrival state, not the later state", expectedRecord(arrival, 10, rig.timeline()!!.lineage), rig.usableRecord(10))
    }

    @Test
    fun `an unrecoverable failed write stops further checkpoint writes for the slot and never touches the game`() {
        val rig = baseline()
        rig.device.armFaultAlways(ckptFile, CommitFault.FALSE_MEMORY_MUTATED) // the rollback fails too
        val (before, choice, arrival) = tenth()
        val reached = rig.saves.commitChoice(before, choice, rig.recorder)
        assertEquals(arrival, reached)
        assertFalse(rig.ckpt.isTrusted(1))

        val commits = rig.device.commitCount(ckptFile)
        val (node, stay) = Story.staying(10)
        val next = rig.saves.commitChoice(reached.copy(sceneId = node.id), stay, rig.recorder)
        assertEquals("no more checkpoint commits while untrusted", commits, rig.device.commitCount(ckptFile))
        assertEquals("the live save still happened", next, rig.saves.load(1)!!.copy(maxHealth = next.maxHealth, health = next.health))

        // after a restart the disk is the truth and the first tap repairs, because the state never moved on
        rig.device.flushApplies()
        val re = rig.reopen()
        val liveState = re.saves.load(1)!!
        assertEquals(Cp.chapterId(10), liveState.chapterId)
    }

    @Test
    fun `after an unrecoverable failure a restart with the arrival state still current repairs the record`() {
        val rig = baseline()
        rig.device.armFaultAlways(ckptFile, CommitFault.FALSE_MEMORY_MUTATED)
        val (before, choice, arrival) = tenth()
        rig.saves.commitChoice(before, choice, rig.recorder)
        assertFalse(rig.ckpt.isTrusted(1))
        rig.device.flushApplies()

        val re = rig.reopen() // new process: faults gone, memory reloaded from disk
        assertTrue(re.ckpt.isTrusted(1))
        tap(re)
        assertEquals((2..10).toList(), re.usable())
        assertEquals(expectedRecord(arrival, 10, re.timeline()!!.lineage), re.usableRecord(10))
    }

    // ---- rule R and the retry never delete anything ----------------------------------------------------------

    @Test
    fun `rule R only adds a record - it never removes a newer valid one`() {
        val rig = Rig()
        rig.playTo(10)
        val lineage = rig.timeline()!!.lineage
        assertTrue(rig.ckpt.removeFrom(1, lineage, 10).isCommitted)
        val stray = CheckpointRecord.fromState(Story.stateAt(StoryRepositoryNode(11)), 11, lineage)
        assertTrue(rig.ckpt.upsert(1, lineage, stray).isCommitted)

        val re = rig.reopen()
        val arrival = re.saves.load(1)!! // the live state is still exactly the chapter 10 arrival state
        re.saves.commitChoice(arrival, Story.staying(10).second, re.recorder)

        assertEquals((2..11).toList(), re.fileRecords().map { it.chapterNumber })
        assertEquals("11 stays unvouched, 10 is back", (2..10).toList(), re.usable())
    }

    @Test
    fun `an old pending record is not written once the ledger no longer vouches for it`() {
        val rig = baseline()
        rig.device.armFault(ckptFile, 1, CommitFault.FALSE_MEMORY_MUTATED) // route A's checkpoint write fails
        rig.arrive(9, tag = "a")
        rig.saves.restorePreviousState(1) // Back, then another route into chapter 10
        val (_, viaB) = rig.arrive(9, tag = "b")

        rig.device.flushApplies()
        assertEquals((2..10).toList(), rig.usable())
        assertEquals(expectedRecord(viaB, 10, rig.timeline()!!.lineage), rig.usableRecord(10))
        assertTrue("route A never ends up in the file", rig.fileRecords().none { it.flags.contains("a") && it.chapterNumber == 10 })
    }

    @Test
    fun `a pending record whose chapter was dropped from the ledger is never written`() {
        val rig = Rig()
        rig.playTo(9)
        rig.device.armFault(ckptFile, 1, CommitFault.FALSE_MEMORY_MUTATED)
        rig.arrive(9, tag = "a") // chapter 10: the write fails and the record is held for a retry
        rig.device.armFault(ckptFile, 1, CommitFault.FALSE_MEMORY_MUTATED) // the retry's first attempt fails
        rig.device.armFault(ckptFile, 3, CommitFault.FALSE_MEMORY_MUTATED) // and so does the re-arrival's own write
        val (_, viaB) = rig.arrive(7, tag = "b") // chapter 8 by another route: the ledger drops 8, 9 and 10

        assertEquals((2..8).toList(), rig.timeline()!!.ledger.chapters())
        val (node, stay) = Story.staying(8)
        rig.saves.commitChoice(Story.stateAt(node), stay, rig.recorder)

        assertTrue("chapter 10 is not vouched for, so it is not written", rig.fileRecords().none { it.chapterNumber == 10 })
        assertEquals("chapter 8 is recovered", expectedRecord(viaB, 8, rig.timeline()!!.lineage), rig.usableRecord(8))
    }

    @Test
    fun `rule R looks again when the ledger's latest entry changed after an earlier confirmation`() {
        val rig = Rig()
        rig.playTo(9)
        rig.arrive(9, tag = "a") // route A: confirmed in this process
        rig.saves.restorePreviousState(1)
        val lineage = rig.timeline()!!.lineage
        val (node, choice) = Story.crossing(9)
        val viaB = Story.stateAt(node, 1, "b").applyChoice(choice)
        val vouchingForB = rig.timeline()!!.ledger.without(10).with(10, CheckpointDigest.of(1, expectedRecord(viaB, 10, lineage)))
        rig.saves.save(1, viaB, TimelineWrite.Set(lineage, vouchingForB)) // the live save moved on; B's record never made it

        rig.recorder!!.beforeCommit(1, viaB, rig.timeline())

        assertEquals(expectedRecord(viaB, 10, lineage), rig.usableRecord(10))
    }

    private fun StoryRepositoryNode(n: Int) = com.thelastjailer.app.data.StoryRepository.nodesInChapter(Cp.chapterId(n)).minByOrNull { it.id }!!
}
