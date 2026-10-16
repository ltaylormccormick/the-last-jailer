package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.OverwriteRequest
import com.thelastjailer.app.OverwriteResult
import com.thelastjailer.app.SlotSaveRequest
import com.thelastjailer.app.StaleReason
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitSlotSave
import com.thelastjailer.app.confirmSlotOverwrite
import com.thelastjailer.app.requestSlotSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 2C-1b-1. SAVE into a different, occupied slot asks first. These tests drive the same pure functions the
 * Save screen calls ([requestSlotSave], [confirmSlotOverwrite], [OverwriteRequest.cancel]); the Compose layer only
 * shows the dialog and forwards taps.
 */
class SlotOverwriteConfirmationTest {
    private val forward = Choice("go", "the_road_away")

    private fun newRig(withRecorder: Boolean = false): Rig =
        Rig(SimulatedDevice().also { it.asyncApply = true }, withRecorder = withRecorder)

    /** Slot 2 holds an old game (own trophy, own Back snapshot); slot 1 is the game on screen, with its own Back snapshot. */
    private fun twoGames(rig: Rig): GameState {
        rig.saves.commitChoice(GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("OldDestTrophy"), gold = 7), forward, null)
        val source = GameState(activeSlot = 1, sceneId = "the_road_away", trophies = setOf("SourceTrophy"), gold = 99, flags = setOf("src"))
        rig.saves.commitChoice(source.copy(sceneId = "fallen_knight", gold = 90), forward.copy(nextNodeId = "the_road_away"), null)
        rig.saves.save(1, source)
        rig.device.flushApplies()
        return source
    }

    private fun live(rig: Rig): Map<String, Any?> = rig.device.memory(Story.LIVE)

    private fun SlotSaveRequest.asked(): OverwriteRequest = (this as SlotSaveRequest.NeedsConfirmation).request

    // ---- when the question is asked ---------------------------------------------------------------------------

    @Test
    fun `an occupied different destination asks first and writes nothing`() {
        val rig = newRig()
        val source = twoGames(rig)
        val memory = live(rig)
        val disk = rig.liveFile()

        val outcome = rig.saves.requestSlotSave(2, source)

        assertTrue(outcome is SlotSaveRequest.NeedsConfirmation)
        assertEquals("nothing queued", 0, rig.device.queuedApplyCount())
        assertEquals(memory, live(rig))
        assertEquals(disk, rig.liveFile())
        assertEquals(2, outcome.asked().destinationSlot)
        assertEquals(1, outcome.asked().sourceSlot)
    }

    @Test
    fun `an empty destination saves at once exactly as before`() {
        val a = newRig(); val b = newRig()
        val sa = twoGames(a); val sb = twoGames(b)

        val outcome = a.saves.requestSlotSave(3, sa)
        val direct = b.saves.commitSlotSave(3, sb)

        assertTrue(outcome is SlotSaveRequest.Saved)
        assertEquals(direct, (outcome as SlotSaveRequest.Saved).state)
        a.device.flushApplies(); b.device.flushApplies()
        assertEquals("same stored result as the unchanged commit", b.liveFile(), a.liveFile())
    }

    @Test
    fun `saving onto the active slot saves at once and keeps its Back snapshot`() {
        val rig = newRig()
        val source = twoGames(rig)
        assertTrue(rig.saves.hasPreviousState(1))

        val outcome = rig.saves.requestSlotSave(1, source.copy(gold = 5))

        assertTrue(outcome is SlotSaveRequest.Saved)
        assertEquals(5, rig.saves.load(1)!!.gold)
        assertTrue("same-slot SAVE keeps Back", rig.saves.hasPreviousState(1))
    }

    @Test
    fun `occupancy is checked when SAVE is pressed, not when the row was drawn`() {
        val rig = newRig()
        val source = twoGames(rig)
        // A row drawn while slot 3 was empty; by the time of the press it holds a game.
        rig.saves.save(3, GameState(activeSlot = 3, gold = 1))
        rig.device.flushApplies()
        rig.saves.save(1, source) // keep slot 1 the active slot

        assertTrue(rig.saves.requestSlotSave(3, source) is SlotSaveRequest.NeedsConfirmation)
    }

    // ---- the dialog text --------------------------------------------------------------------------------------

    @Test
    fun `the dialog names both slots, the loss, the slot switch and that it cannot be undone`() {
        val rig = newRig()
        val source = twoGames(rig)

        val r = rig.saves.requestSlotSave(2, source) { "Title:$it" }.asked()

        assertEquals("Replace Slot 2?", r.title)
        assertEquals("REPLACE SLOT 2", r.confirmLabel)
        val m = r.message
        assertTrue(m, "Slot 2 currently holds Title:${rig.saves.load(2)!!.sceneId}" in m)
        assertTrue(m, "progress and its trophies" in m)
        assertTrue(m, "from Slot 1 (Title:the_road_away" in m)
        assertTrue(m, "You will continue playing in Slot 2" in m)
        assertTrue(m, "Slot 1 is not changed" in m)
        assertTrue(m, "cannot be undone" in m)
        assertTrue(m, "1 trophy" in m && "1 trophies" !in m)
    }

    // ---- cancel / dismiss / Back / rotation -------------------------------------------------------------------

    @Test
    fun `cancel writes nothing and spends the request`() {
        val rig = newRig()
        val source = twoGames(rig)
        val memory = live(rig); val disk = rig.liveFile()
        val r = rig.saves.requestSlotSave(2, source).asked()

        r.cancel()

        assertTrue(r.isSpent)
        assertEquals(memory, live(rig)); assertEquals(disk, rig.liveFile())
        assertEquals(0, rig.device.queuedApplyCount())
        assertEquals("a queued confirm tap after cancel does nothing", OverwriteResult.AlreadyHandled, rig.saves.confirmSlotOverwrite(r, source))
        assertEquals(0, rig.device.queuedApplyCount())
        assertEquals(memory, live(rig))
    }

    @Test
    fun `a dialog that is simply dropped by a rotation or process death leaves every stored byte alone`() {
        val rig = newRig()
        val source = twoGames(rig)
        val disk = rig.liveFile()
        rig.saves.requestSlotSave(2, source).asked() // never confirmed, never cancelled: the UI state is gone

        val re = rig.reopen()

        assertEquals(disk, re.liveFile())
        assertEquals(setOf("OldDestTrophy"), re.saves.load(2)!!.trophies)
        assertTrue("destination Back snapshot intact", re.saves.hasPreviousState(2))
        // and the player can simply start again
        assertTrue(re.saves.requestSlotSave(2, source) is SlotSaveRequest.NeedsConfirmation)
    }

    // ---- confirm ----------------------------------------------------------------------------------------------

    @Test
    fun `confirm replaces the destination in one batch, trophies replaced not merged, source untouched`() {
        val rig = newRig()
        val source = twoGames(rig)
        val sourceKeys = rig.liveFile().filterKeys { it.startsWith("1.") }
        val r = rig.saves.requestSlotSave(2, source).asked()

        val result = rig.saves.confirmSlotOverwrite(r, source)

        assertTrue(result is OverwriteResult.Saved)
        assertEquals("one editor batch (2C-1a)", 1, rig.device.queuedApplyCount())
        rig.device.flushApplies()
        val dest = rig.saves.load(2)!!
        assertEquals("exactly the source's trophies - the old destination's are gone", setOf("SourceTrophy"), dest.trophies)
        assertEquals(99, dest.gold)
        assertEquals(setOf("src"), dest.flags)
        assertEquals(2, dest.activeSlot)
        assertEquals("play continues in the destination", 2, rig.saves.currentActiveSlot())
        assertFalse("the old game's Back snapshot is gone", rig.saves.hasPreviousState(2))
        assertEquals("source slot untouched", sourceKeys, rig.liveFile().filterKeys { it.startsWith("1.") })
        assertEquals(setOf("SourceTrophy"), rig.saves.load(1)!!.trophies)
    }

    @Test
    fun `confirm leaves the same stored result as the unchanged 2C-1a commit - no new keys, same format`() {
        val a = newRig(); val b = newRig()
        val sa = twoGames(a); val sb = twoGames(b)

        a.saves.confirmSlotOverwrite(a.saves.requestSlotSave(2, sa).asked(), sa)
        b.saves.commitSlotSave(2, sb)
        a.device.flushApplies(); b.device.flushApplies()

        assertEquals(b.liveFile(), a.liveFile())
    }

    @Test
    fun `the destination's checkpoint timeline is still invalidated`() {
        val rig = newRig(withRecorder = true)
        rig.arrive(1, slot = 2, tag = "old")
        val (_, sourceState) = rig.arrive(1, slot = 1, tag = "src")
        rig.device.flushApplies()
        assertTrue("destination had a timeline", rig.timeline(2) != null)
        val r = rig.saves.requestSlotSave(2, sourceState).asked()

        assertTrue(rig.saves.confirmSlotOverwrite(r, sourceState) is OverwriteResult.Saved)
        rig.device.flushApplies()

        assertTrue("the old ledger cannot vouch for the copied game", rig.timeline(2) == null)
        assertTrue("the source keeps its own timeline", rig.timeline(1) != null)
    }

    @Test
    fun `a crash before the confirmed batch reaches disk leaves the old destination complete, one after leaves the copy`() {
        val before = newRig(); val after = newRig()
        val sb = twoGames(before); val sa = twoGames(after)
        before.saves.confirmSlotOverwrite(before.saves.requestSlotSave(2, sb).asked(), sb)
        after.saves.confirmSlotOverwrite(after.saves.requestSlotSave(2, sa).asked(), sa)
        before.device.dropQueuedApplies()
        after.device.flushApplies()

        val b = before.reopen(); val a = after.reopen()

        assertEquals(setOf("OldDestTrophy"), b.saves.load(2)!!.trophies)
        assertTrue(b.saves.hasPreviousState(2))
        assertEquals(setOf("SourceTrophy"), a.saves.load(2)!!.trophies)
        assertFalse(a.saves.hasPreviousState(2))
    }

    // ---- duplicate / re-entrant submission --------------------------------------------------------------------

    @Test
    fun `a second confirm of the same request does nothing`() {
        val rig = newRig()
        val source = twoGames(rig)
        val r = rig.saves.requestSlotSave(2, source).asked()

        assertTrue(rig.saves.confirmSlotOverwrite(r, source) is OverwriteResult.Saved)
        val memory = live(rig)
        assertEquals(OverwriteResult.AlreadyHandled, rig.saves.confirmSlotOverwrite(r, source))
        assertEquals(OverwriteResult.AlreadyHandled, rig.saves.confirmSlotOverwrite(r, source))

        assertEquals("still exactly one batch", 1, rig.device.queuedApplyCount())
        assertEquals(memory, live(rig))
    }

    @Test
    fun `two requests for the same slot cannot both commit`() {
        val rig = newRig()
        val source = twoGames(rig)
        val first = rig.saves.requestSlotSave(2, source).asked()
        val second = rig.saves.requestSlotSave(2, source).asked() // a queued second tap on SAVE

        assertTrue(rig.saves.confirmSlotOverwrite(first, source) is OverwriteResult.Saved)
        val again = rig.saves.confirmSlotOverwrite(second, source)

        assertTrue("the destination is now the copy, so the second question is stale", again is OverwriteResult.Stale)
        assertEquals(1, rig.device.queuedApplyCount())
    }

    @Test
    fun `a SAVE tap queued behind an earlier SAVE is ignored instead of re-asking about a stale screen`() {
        val rig = newRig()
        val source = twoGames(rig)
        // First tap: empty slot 3, saved at once (and made active). The screen has not recomposed yet, so the
        // second tap still carries the old state, but slot 3 now holds a game.
        assertTrue(rig.saves.requestSlotSave(3, source) is SlotSaveRequest.Saved)
        rig.device.flushApplies()
        val memory = live(rig)

        assertEquals(SlotSaveRequest.Stale, rig.saves.requestSlotSave(3, source))
        assertEquals(memory, live(rig))
        assertEquals(0, rig.device.queuedApplyCount())
    }

    @Test
    fun `a queued SAVE for an empty slot with an outdated screen is ignored, not copied`() {
        val rig = newRig()
        val source = twoGames(rig)
        assertTrue(rig.saves.requestSlotSave(3, source) is SlotSaveRequest.Saved) // first tap: active slot is now 3
        rig.device.flushApplies()
        val memory = live(rig)

        // second queued tap, still carrying the screen from before the switch, aimed at another empty slot
        assertEquals(SlotSaveRequest.Stale, rig.saves.requestSlotSave(4, source))

        assertFalse("nothing was copied into slot 4", rig.saves.hasSave(4))
        assertEquals(3, rig.saves.currentActiveSlot())
        assertEquals(memory, live(rig))
        assertEquals(0, rig.device.queuedApplyCount())
    }

    @Test
    fun `a queued same-slot SAVE with an outdated screen is ignored, not written`() {
        val rig = newRig()
        val source = twoGames(rig)
        assertTrue(rig.saves.requestSlotSave(3, source) is SlotSaveRequest.Saved)
        rig.device.flushApplies()
        val memory = live(rig)

        // tap on the old active slot's own SAVE, with the old screen
        assertEquals(SlotSaveRequest.Stale, rig.saves.requestSlotSave(1, source.copy(gold = 1)))

        assertEquals("slot 1 untouched", 99, rig.saves.load(1)!!.gold)
        assertEquals("active slot not switched back", 3, rig.saves.currentActiveSlot())
        assertEquals(memory, live(rig))
        assertEquals(0, rig.device.queuedApplyCount())
    }

    @Test
    fun `a brand-new game with no persisted active slot can still save to the same and to an empty slot`() {
        val rig = newRig()
        assertEquals(null, rig.saves.currentActiveSlot())
        val fresh = GameState(activeSlot = 1)

        assertTrue(rig.saves.requestSlotSave(1, fresh) is SlotSaveRequest.Saved)
        val rig2 = newRig()
        assertTrue(rig2.saves.requestSlotSave(2, fresh) is SlotSaveRequest.Saved)
        assertTrue(rig2.saves.hasSave(2))
    }

    @Test
    fun `a normal same-slot SAVE still saves when the screen matches the persisted active slot`() {
        val rig = newRig()
        val source = twoGames(rig)
        assertEquals(1, rig.saves.currentActiveSlot())

        assertTrue(rig.saves.requestSlotSave(1, source.copy(gold = 5)) is SlotSaveRequest.Saved)
        assertEquals(5, rig.saves.load(1)!!.gold)
    }

    // ---- stale confirmations ----------------------------------------------------------------------------------

    private fun assertStale(rig: Rig, r: OverwriteRequest, current: GameState, reason: StaleReason, memory: Map<String, Any?>) {
        val result = rig.saves.confirmSlotOverwrite(r, current)
        assertTrue("expected stale, got $result", result is OverwriteResult.Stale)
        assertEquals(reason, (result as OverwriteResult.Stale).reason)
        assertEquals("a stale confirmation writes nothing", memory, live(rig))
        assertEquals(0, rig.device.queuedApplyCount())
        assertEquals("a stale request is spent", OverwriteResult.AlreadyHandled, rig.saves.confirmSlotOverwrite(r, current))
    }

    @Test
    fun `any change to the destination's stored data after the question aborts`() {
        val changes = mapOf<String, (Rig) -> Unit>(
            "game state" to { it.saves.save(2, GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("OldDestTrophy"), gold = 8), TimelineWrite.Keep) },
            "trophies, same count" to { it.saves.save(2, GameState(activeSlot = 2, sceneId = "fallen_knight", trophies = setOf("OtherTrophy"), gold = 7), TimelineWrite.Keep) },
            "back snapshot" to { it.saves.clearPreviousState(2) },
            "timeline" to { it.device.prefs(Story.LIVE).edit().putString("2.lineage", "ffffffffffffffffffffffffffffffff").commit() },
            "emptied" to { it.device.prefs(Story.LIVE).edit().remove("2.scene").commit() }
        )
        for ((name, change) in changes) {
            val rig = newRig()
            val source = twoGames(rig)
            val r = rig.saves.requestSlotSave(2, source).asked()
            change(rig)
            rig.device.flushApplies(); rig.saves.save(1, source); rig.device.flushApplies() // keep slot 1 active
            val memory = live(rig)
            val result = rig.saves.confirmSlotOverwrite(r, source)
            assertTrue("$name: $result", result is OverwriteResult.Stale)
            assertEquals(name, StaleReason.DESTINATION_CHANGED, (result as OverwriteResult.Stale).reason)
            assertEquals("$name: nothing written", memory, live(rig))
            assertEquals(name, 0, rig.device.queuedApplyCount())
        }
    }

    @Test
    fun `a destination that now looks the same as before at the fields a weak fingerprint reads is still caught`() {
        // Same scene, same level, same trophy count, same lineage as when asked - but different gold, inventory,
        // flags and trophy. A scene+level+trophy-count fingerprint would let this through.
        val rig = newRig()
        val source = twoGames(rig)
        val r = rig.saves.requestSlotSave(2, source).asked()
        rig.saves.save(
            2,
            GameState(activeSlot = 2, sceneId = "fallen_knight", level = 1, trophies = setOf("DifferentTrophy"), gold = 500, inventory = listOf("x"), flags = setOf("f")),
            TimelineWrite.Keep
        )
        rig.saves.save(1, source); rig.device.flushApplies()

        assertStale(rig, r, source, StaleReason.DESTINATION_CHANGED, live(rig))
    }

    @Test
    fun `any change to the game on screen after the question aborts`() {
        val edits = mapOf<String, (GameState) -> GameState>(
            "gold" to { it.copy(gold = it.gold + 1) },
            "health" to { it.copy(health = it.health - 1) },
            "maxHealth" to { it.copy(maxHealth = it.maxHealth + 1) },
            "level" to { it.copy(level = it.level + 1) },
            "xp" to { it.copy(xp = it.xp + 1) },
            "xpToNext" to { it.copy(xpToNextLevel = it.xpToNextLevel + 1) },
            "courage" to { it.copy(courage = it.courage + 1) },
            "honour" to { it.copy(honour = it.honour + 1) },
            "scene" to { it.copy(sceneId = "fallen_knight") },
            "chapter" to { it.copy(chapterId = "chapter_2") },
            "inventory" to { it.copy(inventory = it.inventory + "extra") },
            "trophies" to { it.copy(trophies = it.trophies + "NewTrophy") },
            "trophy renamed" to { it.copy(trophies = setOf("SourceTrophy2")) },
            "flags" to { it.copy(flags = it.flags + "extra") },
            "slot" to { it.copy(activeSlot = 3) }
        )
        for ((name, edit) in edits) {
            val rig = newRig()
            val source = twoGames(rig)
            val r = rig.saves.requestSlotSave(2, source).asked()
            val memory = live(rig)
            val result = rig.saves.confirmSlotOverwrite(r, edit(source))
            assertTrue("$name: $result", result is OverwriteResult.Stale)
            assertEquals(name, StaleReason.SCREEN_STATE_CHANGED, (result as OverwriteResult.Stale).reason)
            assertEquals("$name: nothing written", memory, live(rig))
            assertEquals(name, 0, rig.device.queuedApplyCount())
        }
    }

    @Test
    fun `a change to the source slot's stored data aborts`() {
        val rig = newRig()
        val source = twoGames(rig)
        val r = rig.saves.requestSlotSave(2, source).asked()
        rig.saves.savePreviousState(1, source.copy(gold = 1)) // the source slot's stored data moved underneath the dialog
        rig.device.flushApplies()

        assertStale(rig, r, source, StaleReason.SOURCE_SAVE_CHANGED, live(rig))
    }

    @Test
    fun `a different persisted active slot aborts`() {
        val rig = newRig()
        val source = twoGames(rig)
        val r = rig.saves.requestSlotSave(2, source).asked()
        rig.saves.setActiveSlot(3); rig.device.flushApplies()

        assertStale(rig, r, source, StaleReason.ACTIVE_SLOT_CHANGED, live(rig))
    }

    @Test
    fun `a stale confirmation cannot be revived - a fresh SAVE press asks again with fresh facts`() {
        val rig = newRig()
        val source = twoGames(rig)
        val r = rig.saves.requestSlotSave(2, source).asked()
        rig.saves.save(2, GameState(activeSlot = 2, gold = 3), TimelineWrite.Keep)
        rig.saves.save(1, source); rig.device.flushApplies()
        assertTrue(rig.saves.confirmSlotOverwrite(r, source) is OverwriteResult.Stale)

        val fresh = rig.saves.requestSlotSave(2, source).asked()

        assertNotEquals(r, fresh)
        assertTrue(rig.saves.confirmSlotOverwrite(fresh, source) is OverwriteResult.Saved)
        assertEquals(99, rig.saves.load(2)!!.gold)
    }

    // ---- the signature itself ---------------------------------------------------------------------------------

    @Test
    fun `a slot's signature covers only that slot, and slot 1 is never confused with slot 10`() {
        val rig = newRig()
        twoGames(rig)
        val s1 = rig.saves.slotSignature(1)
        rig.saves.save(10, GameState(activeSlot = 10, gold = 3)); rig.device.flushApplies()
        rig.saves.save(1, rig.saves.load(1)!!); rig.device.flushApplies() // same content again

        assertEquals("a write to slot 10 does not change slot 1's signature", s1, rig.saves.slotSignature(1))
        assertTrue(rig.saves.slotSignature(10).isNotEmpty())
        assertEquals("", rig.saves.slotSignature(9))
    }
}
