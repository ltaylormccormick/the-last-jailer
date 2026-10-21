package com.thelastjailer.app.saveguard.gate

import android.content.SharedPreferences
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.CommitResult
import com.thelastjailer.app.CommitStatus
import com.thelastjailer.app.GameState
import com.thelastjailer.app.OverwriteResult
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.SlotSaveRequest
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.checkpoint.Cp
import com.thelastjailer.app.checkpoint.SimulatedDevice
import com.thelastjailer.app.checkpoint.Story
import com.thelastjailer.app.checkpoint.TimelineWrite
import com.thelastjailer.app.commitChoiceResult
import com.thelastjailer.app.commitCombatResultOutcome
import com.thelastjailer.app.commitPurchaseResult
import com.thelastjailer.app.commitRestartResult
import com.thelastjailer.app.commitSlotSaveResult
import com.thelastjailer.app.confirmSlotOverwrite
import com.thelastjailer.app.requestSlotSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G2L B2: every main-save write of [SaveStore] goes through the (permanently open) [SaveGate] as one lock acquisition,
 * one [WriteIntent], one editor batch and one `apply()`. These tests drive each action through the four outcomes:
 * handed, denied (frozen gate), failed (no editor) and uncertain (`apply()` throws), and prove that a refused or failed
 * write changes nothing on screen, in the preferences or in the checkpoint files.
 */
class SaveStoreGateIntegrationTest {

    /** Counts attempted applies on the main save file and can make `edit()` or `apply()` throw. */
    private class FaultPrefs(private val base: SharedPreferences) : SharedPreferences by base {
        var applyAttempts = 0
        var editCalls = 0
        var failEdit = false
        var failApply = false
        var failPutKey: ((String) -> Boolean)? = null
        val keysStaged = ArrayList<String>()
        val log = ArrayList<String>()

        override fun edit(): SharedPreferences.Editor {
            editCalls++
            if (failEdit) throw IllegalStateException("no editor")
            return Ed(base.edit())
        }

        private inner class Ed(private val real: SharedPreferences.Editor) : SharedPreferences.Editor {
            private fun note(key: String?) {
                keysStaged += key.orEmpty()
                if (key != null && failPutKey?.invoke(key) == true) throw IllegalStateException("put failed")
            }
            override fun putString(key: String?, value: String?) = also { note(key); real.putString(key, value) }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = also { real.putStringSet(key, values) }
            override fun putInt(key: String?, value: Int) = also { note(key); real.putInt(key, value) }
            override fun putLong(key: String?, value: Long) = also { real.putLong(key, value) }
            override fun putFloat(key: String?, value: Float) = also { real.putFloat(key, value) }
            override fun putBoolean(key: String?, value: Boolean) = also { real.putBoolean(key, value) }
            override fun remove(key: String?) = also { note(key); real.remove(key) }
            override fun clear() = also { real.clear() }
            override fun commit(): Boolean = throw AssertionError("commit() must never be used")
            override fun apply() {
                applyAttempts++
                log += "apply"
                if (failApply) throw IllegalStateException("apply failed")
                real.apply()
            }
        }
    }

    private class Env(gate: SaveGate = SaveGate(GateMode.Open), hooks: ArrivalRecorder.Hooks = ArrivalRecorder.Hooks()) {
        val device = SimulatedDevice()
        val prefs = FaultPrefs(device.prefs(Story.LIVE))
        val store = SaveStore(prefs, gate)
        val plain = SaveStore(device.prefs(Story.LIVE)) // an ungated twin, only to seed state
        val recorder = ArrivalRecorder(Cp.store(device), Cp.rules, true, { _, _ -> }, hooks)
        fun ckptFiles(): Map<String, Map<String, Any?>> = (1..3).associate { Cp.file(it) to device.disk(Cp.file(it)) }
        fun live(): Map<String, Any?> = device.disk(Story.LIVE)
    }

    private val staying = Story.staying(2)
    private val crossing = Story.crossing(2)
    private val encounter = CombatEncounter(
        id = "e", enemyId = "x", victoryNodeId = "after_victory", defeatNodeId = "after_defeat",
        xpReward = 10, goldReward = 5, unlockTrophy = "T"
    )

    /** An action, the game it starts from, and what a refused run must leave on screen. */
    private class Action(val name: String, val seed: (Env) -> GameState, val run: (Env, GameState) -> CommitResult)

    private fun seeded(env: Env, slot: Int = 1): GameState {
        val s = Story.stateAt(staying.first, slot, "seed")
        env.plain.commitChoiceResult(s, staying.second, env.recorder)
        return env.plain.load(slot)!!
    }

    private val actions = listOf(
        Action("choice (same chapter)", { Story.stateAt(staying.first, 1, "a") }) { e, s -> e.store.commitChoiceResult(s, staying.second, e.recorder) },
        Action("choice (chapter arrival)", { Story.stateAt(crossing.first, 1, "a") }) { e, s -> e.store.commitChoiceResult(s, crossing.second, e.recorder) },
        Action("choice (no recorder)", { Story.stateAt(staying.first, 1, "a") }) { e, s -> e.store.commitChoiceResult(s, staying.second, null) },
        Action("combat", { seeded(it) }) { e, s -> e.store.commitCombatResultOutcome(s, encounter, CombatOutcome(true, 3, emptyList()), e.recorder) },
        Action("purchase", { seeded(it).copy(gold = 90) }) { e, s -> e.store.commitPurchaseResult(s, "healing_draught", 5, e.recorder) },
        Action("restart", { seeded(it) }) { e, _ -> e.store.commitRestartResult(1, e.recorder) },
        Action("slot save (same slot)", { seeded(it) }) { e, s -> e.store.commitSlotSaveResult(1, s) },
        Action("slot save (other slot)", { seeded(it) }) { e, s -> e.store.commitSlotSaveResult(2, s) }
    )

    private fun frozen() = SaveGate(GateMode.Frozen(FreezeReason.SUSPECT_FILE))

    // ---- allowed: one batch, one apply, same result as before ---------------------------------------------------------

    @Test
    fun `every action is exactly one editor batch and one apply`() {
        for (a in actions) {
            val env = Env()
            val start = a.seed(env)
            val applies = env.prefs.applyAttempts
            val edits = env.prefs.editCalls
            val r = a.run(env, start)
            assertEquals("${a.name} status", CommitStatus.SAVED, r.status)
            assertEquals("${a.name} applies", 1, env.prefs.applyAttempts - applies)
            assertEquals("${a.name} edits", 1, env.prefs.editCalls - edits)
        }
    }

    @Test
    fun `the Back, clear and active slot writes are one batch each`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        assertEquals(CommitStatus.SAVED, env.store.savePreviousState(1, s))
        assertEquals(1, env.prefs.applyAttempts)
        assertEquals(CommitStatus.SAVED, env.store.clearPreviousState(1))
        assertEquals(2, env.prefs.applyAttempts)
        assertEquals(CommitStatus.SAVED, env.store.setActiveSlot(2))
        assertEquals(3, env.prefs.applyAttempts)
        assertEquals(2, env.store.currentActiveSlot())
        env.plain.commitChoiceResult(s, staying.second, null)
        val before = env.prefs.applyAttempts
        assertNotNull(env.store.restorePreviousState(1))
        assertEquals("restoring Back is one batch", 1, env.prefs.applyAttempts - before)
    }

    @Test
    fun `an allowed action returns the new state and nothing else`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        val r = env.store.commitChoiceResult(s, staying.second, env.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertEquals(env.store.load(1), r.state)
        assertEquals(s.applyChoiceForTest(), r.state.sceneId)
    }

    private fun GameState.applyChoiceForTest(): String = staying.second.nextNodeId

    @Test
    fun `Back snapshot rules per action are unchanged`() {
        fun hasBack(a: Action): Boolean { val e = Env(); val s = a.seed(e); assertTrue(e.plain.hasPreviousState(1) || a.name.startsWith("choice")); a.run(e, s); return e.store.hasPreviousState(1) }
        val byName = actions.associateBy { it.name }
        assertTrue("an ordinary choice leaves a Back snapshot", hasBack(byName.getValue("choice (same chapter)")))
        assertTrue(hasBack(byName.getValue("choice (chapter arrival)")))
        assertTrue(hasBack(byName.getValue("choice (no recorder)")))
        assertTrue("combat clears Back", !hasBack(byName.getValue("combat")))
        assertTrue("a purchase clears Back", !hasBack(byName.getValue("purchase")))
        assertTrue("Start Again clears Back", !hasBack(byName.getValue("restart")))
        assertTrue("saving onto the same slot keeps Back", hasBack(byName.getValue("slot save (same slot)")))
        val e = Env(); val s = byName.getValue("slot save (other slot)").seed(e)
        e.store.commitSlotSaveResult(2, s)
        assertTrue("the destination has no Back snapshot", !e.store.hasPreviousState(2))
    }

    @Test
    fun `an uncertain restore shows the stored game, not the restored one`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        env.plain.commitChoiceResult(s, staying.second, null)
        val stored = env.plain.load(1)
        env.prefs.failApply = true
        val shown = env.store.restorePreviousState(1)
        assertEquals(stored, shown)
        assertTrue(s.sceneId != shown!!.sceneId)
        assertTrue(env.store.hasPreviousState(1))
    }

    @Test
    fun `when the checkpoint file turns out to be newer the arrival is planned with recording off`() {
        // beforeCommit finds a newer-format file while retrying a pending record, which switches recording off for the
        // slot. The plan made before that is stale: it would put a ledger entry for the new chapter into the same batch
        // although no record can exist for it. The restage replaces it with the recording-off plan.
        val device = SimulatedDevice()
        val first = com.thelastjailer.app.checkpoint.Rig(device)
        device.armFault(Cp.file(1), 1, com.thelastjailer.app.checkpoint.CommitFault.FALSE_UNCHANGED)
        first.arrive(4) // the checkpoint write fails: a record is pending in memory
        device.writeDisk(
            Cp.file(1),
            mapOf("h.version" to com.thelastjailer.app.checkpoint.CHECKPOINT_FORMAT_VERSION + 1, "h.slot" to 1, "h.lineage" to Cp.L1, "c05.future" to "data", "c05.crc" to 7L)
        )
        val rig = first // same process: the pending record is retried by beforeCommit of the next action
        val (node, choice) = Story.crossing(5)
        val r = rig.saves.commitChoiceResult(Story.stateAt(node, 1, "a"), choice, rig.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertTrue(rig.recorder!!.isDisabledForSlot(1))
        val tl = rig.timeline(1)
        assertNotNull("the replanned batch must leave a timeline the stamp vouches for", tl)
        assertNull("no ledger entry for the arrival chapter", tl!!.ledger.digestFor(6))
    }

    @Test
    fun `an allowed chapter arrival does write its checkpoint record after the main apply`() {
        // makes the "no checkpoint work" assertions below meaningful: the same action does write when allowed
        val env = Env()
        val before = env.ckptFiles()
        val r = env.store.commitChoiceResult(Story.stateAt(crossing.first, 1, "a"), crossing.second, env.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertTrue(before != env.ckptFiles())
        assertTrue(env.device.disk(Cp.file(1)).isNotEmpty())
    }

    // ---- denied / failed / uncertain ---------------------------------------------------------------------------------

    private fun assertNothingChanged(name: String, env: Env, liveBefore: Map<String, Any?>, ckptBefore: Map<String, Map<String, Any?>>) {
        assertEquals("$name live map", liveBefore, env.live())
        assertEquals("$name checkpoint files", ckptBefore, env.ckptFiles())
    }

    @Test
    fun `a denied write changes nothing and runs no checkpoint work`() {
        for (a in actions) {
            val env = Env(frozen())
            val start = a.seed(env)
            val live = env.live()
            val ckpt = env.ckptFiles()
            val r = a.run(env, start)
            assertEquals("${a.name} status", CommitStatus.NOT_SAVED, r.status)
            assertEquals("${a.name} applies", 0, env.prefs.applyAttempts)
            assertNothingChanged(a.name, env, live, ckpt)
            assertEquals("${a.name} state on screen", start, r.state)
        }
    }

    @Test
    fun `a failed write changes nothing and runs no checkpoint work`() {
        for (a in actions) {
            val env = Env()
            val start = a.seed(env)
            env.prefs.failEdit = true
            val live = env.live()
            val ckpt = env.ckptFiles()
            val r = a.run(env, start)
            assertEquals("${a.name} status", CommitStatus.NOT_SAVED, r.status)
            assertEquals("${a.name} applies", 0, env.prefs.applyAttempts)
            assertNothingChanged(a.name, env, live, ckpt)
            assertEquals("${a.name} state on screen", start, r.state)
        }
    }

    @Test
    fun `an uncertain write reports it, shows what is stored and does no post-save checkpoint work`() {
        for (a in actions) {
            val env = Env()
            val start = a.seed(env)
            env.prefs.failApply = true
            val live = env.live()
            val ckpt = env.ckptFiles()
            val r = a.run(env, start)
            assertEquals("${a.name} status", CommitStatus.UNCERTAIN, r.status)
            assertEquals("${a.name} checkpoint files", ckpt, env.ckptFiles())
            assertEquals("${a.name} applies", 1, env.prefs.applyAttempts)
            assertEquals("${a.name} live map", live, env.live())
            // after-save work (writing the arrival record) must not have run: no checkpoint file may have records
            // that the seed did not already have
            assertEquals("${a.name} shown state is what is stored", env.plain.load(start.activeSlot) ?: start, r.state)
        }
    }

    @Test
    fun `a refused story choice does not advance the story, inventory or gold on screen`() {
        val env = Env(frozen())
        val s = Story.stateAt(staying.first, 1, "a").copy(gold = 42, inventory = listOf("healing_draught"))
        val r = env.store.commitPurchaseResult(s, "sturdy_buckler", 10, env.recorder)
        assertEquals(CommitStatus.NOT_SAVED, r.status)
        assertEquals(42, r.state.gold)
        assertEquals(listOf("healing_draught"), r.state.inventory)
        val c = env.store.commitChoiceResult(s, staying.second, env.recorder)
        assertEquals(s.sceneId, c.state.sceneId)
    }

    @Test
    fun `slot save requests report NotSaved instead of Saved when the write is refused`() {
        val env = Env(frozen())
        val s = seeded(env)
        val request = env.store.requestSlotSave(1, s)
        assertTrue(request is SlotSaveRequest.NotSaved)
        assertEquals(s, (request as SlotSaveRequest.NotSaved).state)

        // an occupied other slot asks first; confirming a refused write is NotSaved and nothing is replaced
        val open = Env()
        val src = seeded(open)
        open.plain.commitSlotSaveResult(2, src.copy(gold = 7))
        open.plain.setActiveSlot(1)
        val ask = open.store.requestSlotSave(2, src) as SlotSaveRequest.NeedsConfirmation
        val frozenStore = SaveStore(open.prefs, frozen())
        val live = open.live()
        val res = frozenStore.confirmSlotOverwrite(ask.request, src)
        assertTrue(res is OverwriteResult.NotSaved)
        assertEquals(live, open.live())
    }

    @Test
    fun `an uncertain slot save is not reported as saved either`() {
        val env = Env()
        val s = seeded(env)
        env.prefs.failApply = true
        val request = env.store.requestSlotSave(1, s)
        assertTrue(request is SlotSaveRequest.NotSaved)
        assertEquals(s, (request as SlotSaveRequest.NotSaved).state)

        val open = Env()
        val src = seeded(open)
        open.plain.commitSlotSaveResult(2, src.copy(gold = 7))
        open.plain.setActiveSlot(1)
        val ask = open.store.requestSlotSave(2, src) as SlotSaveRequest.NeedsConfirmation
        open.prefs.failApply = true
        assertTrue(open.store.confirmSlotOverwrite(ask.request, src) is OverwriteResult.NotSaved)
    }

    @Test
    fun `SaveStore restart reports each outcome and shows what is stored when it did not save`() {
        val denied = Env(frozen())
        val s = seeded(denied)
        val shown = denied.store.restart(1)
        assertEquals(s, shown)
        assertEquals(s, denied.plain.load(1))

        val failed = Env()
        val s2 = seeded(failed)
        failed.prefs.failEdit = true
        assertEquals(s2, failed.store.restart(1))

        val ok = Env()
        seeded(ok)
        assertEquals(GameState(activeSlot = 1), ok.store.restart(1))
        assertEquals(GameState(activeSlot = 1), ok.plain.load(1))
    }

    @Test
    fun `setActiveSlot reports each outcome and a refused switch leaves the active slot alone`() {
        val denied = Env(frozen())
        denied.plain.setActiveSlot(1)
        assertEquals(CommitStatus.NOT_SAVED, denied.store.setActiveSlot(2))
        assertEquals(1, denied.plain.currentActiveSlot())

        val failed = Env()
        failed.plain.setActiveSlot(1)
        failed.prefs.failEdit = true
        assertEquals(CommitStatus.NOT_SAVED, failed.store.setActiveSlot(2))
        assertEquals(1, failed.plain.currentActiveSlot())

        val uncertain = Env()
        uncertain.plain.setActiveSlot(1)
        uncertain.prefs.failApply = true
        assertEquals(CommitStatus.UNCERTAIN, uncertain.store.setActiveSlot(2))
        assertEquals(1, uncertain.plain.currentActiveSlot())
    }

    @Test
    fun `restorePreviousState does not pretend to restore when the write is refused`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        env.plain.commitChoiceResult(s, staying.second, null)
        val gatedClosed = SaveStore(env.prefs, frozen())
        val live = env.live()
        assertNull(gatedClosed.restorePreviousState(1))
        assertEquals(live, env.live())
        assertTrue(gatedClosed.hasPreviousState(1))
        // and the open gate still can
        assertEquals(s.sceneId, env.store.restorePreviousState(1)!!.sceneId)
    }

    // ---- ordering ---------------------------------------------------------------------------------------------------

    @Test
    fun `before runs ahead of the apply and after runs once the batch was handed over`() {
        val env = Env()
        val log = env.prefs.log
        val s = Story.stateAt(staying.first, 1, "a")
        val status = env.store.save(1, s, TimelineWrite.Invalidate, before = { log += "before" }, after = { log += "after" })
        assertEquals(CommitStatus.SAVED, status)
        assertEquals(listOf("before", "apply", "after"), log)
    }

    @Test
    fun `neither hook runs when the gate refuses or the editor cannot be created`() {
        val s = Story.stateAt(staying.first, 1, "a")
        val closed = Env(frozen())
        closed.store.save(1, s, before = { closed.prefs.log += "before" }, after = { closed.prefs.log += "after" })
        assertEquals(emptyList<String>(), closed.prefs.log)

        val broken = Env()
        broken.prefs.failEdit = true
        broken.store.save(1, s, before = { broken.prefs.log += "before" }, after = { broken.prefs.log += "after" })
        assertEquals(emptyList<String>(), broken.prefs.log)
    }

    @Test
    fun `after never runs when apply throws`() {
        val env = Env()
        env.prefs.failApply = true
        val s = Story.stateAt(staying.first, 1, "a")
        val status = env.store.save(1, s, before = { env.prefs.log += "before" }, after = { env.prefs.log += "after" })
        assertEquals(CommitStatus.UNCERTAIN, status)
        assertEquals(listOf("before", "apply"), env.prefs.log)
    }

    @Test
    fun `a throwing before hook does not stop the save and a throwing after hook does not undo it`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        val status = env.store.save(1, s, before = { error("boom") }, after = { error("boom") })
        assertEquals(CommitStatus.SAVED, status)
        assertEquals(s.sceneId, env.store.load(1)!!.sceneId)
    }

    @Test
    fun `a changed plan after the before hook restages the timeline in the same batch`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        var applies = 0
        val first = TimelineWrite.Invalidate
        val status = env.store.save(
            1, s, first,
            before = { applies++ },
            retimeline = { TimelineWrite.Set(Cp.L1, com.thelastjailer.app.checkpoint.Ledger.EMPTY) }
        )
        assertEquals(CommitStatus.SAVED, status)
        assertEquals(1, env.prefs.applyAttempts)
        assertEquals(Cp.L1, env.live()["1.lineage"])
        assertNotNull(env.store.readTimeline(1))
    }

    @Test
    fun `no timeline restage when the plan did not change`() {
        val env = Env()
        val s = Story.stateAt(staying.first, 1, "a")
        env.store.save(1, s, TimelineWrite.Invalidate, retimeline = { null })
        assertNull(env.live()["1.lineage"])
    }

    // ---- the pre-apply replan (review finding on PR #133) --------------------------------------------------------------

    private fun newerFile(env: Env) = env.device.writeDisk(
        Cp.file(1),
        mapOf("h.version" to com.thelastjailer.app.checkpoint.CHECKPOINT_FORMAT_VERSION + 1, "h.slot" to 1, "h.lineage" to Cp.L1, "c05.future" to "data", "c05.crc" to 7L)
    )

    /** Arrival into chapter 5 whose checkpoint write fails (a record is pending), then the file turns out to be newer. */
    private fun pendingThenNewer(env: Env) {
        env.device.armFault(Cp.file(1), 1, com.thelastjailer.app.checkpoint.CommitFault.FALSE_UNCHANGED)
        val (n4, c4) = Story.crossing(4)
        assertEquals(CommitStatus.SAVED, env.store.commitChoiceResult(Story.stateAt(n4, 1, "a"), c4, env.recorder).status)
        newerFile(env)
        env.prefs.keysStaged.clear()
    }

    @Test
    fun `the replan after the checkpoint step only touches this slot's own keys`() {
        val env = Env()
        pendingThenNewer(env)
        val (node, choice) = Story.crossing(5)
        val r = env.store.commitChoiceResult(Story.stateAt(node, 1, "a"), choice, env.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertTrue(env.recorder.isDisabledForSlot(1))
        assertTrue("keys: ${env.prefs.keysStaged}", env.prefs.keysStaged.isNotEmpty())
        for (k in env.prefs.keysStaged) assertTrue("unexpected key staged: $k", k == "active_slot" || k.startsWith("1."))
        assertEquals("one apply", 2, env.prefs.applyAttempts) // the pending arrival above + this action
    }

    @Test
    fun `the restage replaces staged entries in memory so the real editor sees each timeline key once`() {
        val env = Env()
        pendingThenNewer(env)
        var lineagePuts = 0
        env.prefs.failPutKey = { k -> if (k.endsWith(".lineage")) lineagePuts++; false }
        val (node, choice) = Story.crossing(5)
        val r = env.store.commitChoiceResult(Story.stateAt(node, 1, "a"), choice, env.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertEquals("one put per key on the real editor", 1, lineagePuts)
        assertTrue(env.recorder.isDisabledForSlot(1))
    }

    @Test
    fun `a failing real editor call during the replay persists nothing and is reported uncertain`() {
        val env = Env()
        pendingThenNewer(env)
        val liveBefore = env.live()
        val newer = env.device.disk(Cp.file(1))
        env.prefs.failPutKey = { k -> k.endsWith(".lineage") }
        val (node, choice) = Story.crossing(5)
        val start = Story.stateAt(node, 1, "a")
        val r = env.store.commitChoiceResult(start, choice, env.recorder)
        assertEquals(CommitStatus.UNCERTAIN, r.status)
        assertEquals(liveBefore, env.live())
        assertEquals(env.plain.load(1), r.state)
        assertEquals("no post-save checkpoint work", newer, env.device.disk(Cp.file(1)))
    }

    @Test
    fun `a replan that throws leaves a consistent batch`() {
        var explode = false
        val env = Env(hooks = ArrivalRecorder.Hooks(buildRecord = { st, ch, l ->
            if (explode) error("boom")
            com.thelastjailer.app.checkpoint.CheckpointRecord.fromState(st, ch, l)
        }))
        pendingThenNewer(env)
        explode = true
        val (node, choice) = Story.crossing(5)
        val r = env.store.commitChoiceResult(Story.stateAt(node, 1, "a"), choice, env.recorder)
        assertEquals(CommitStatus.SAVED, r.status)
        assertEquals(r.state, env.store.load(1))
        // whatever was staged, a timeline is either absent or vouched for by its stamp
        env.store.readTimeline(1)
    }

    @Test
    fun `apply failing after the replan is uncertain and does no post-save checkpoint work`() {
        val env = Env()
        pendingThenNewer(env)
        val newer = env.device.disk(Cp.file(1))
        env.prefs.failApply = true
        val (node, choice) = Story.crossing(5)
        val start = Story.stateAt(node, 1, "a")
        val r = env.store.commitChoiceResult(start, choice, env.recorder)
        assertEquals(CommitStatus.UNCERTAIN, r.status)
        assertEquals(env.plain.load(1) ?: start, r.state)
        assertEquals(newer, env.device.disk(Cp.file(1)))
    }

    @Test
    fun `a denied action never reaches the replan`() {
        val env = Env(frozen())
        env.prefs.log.clear()
        var replanned = false
        val s = Story.stateAt(staying.first, 1, "a")
        env.store.save(1, s, before = { replanned = true }, retimeline = { replanned = true; null })
        assertTrue(!replanned)
        assertEquals(emptyList<String>(), env.prefs.keysStaged.filter { true })
    }
}
