package com.thelastjailer.app.saveguard.gate

import android.content.SharedPreferences
import com.thelastjailer.app.CommitStatus
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.checkpoint.CHECKPOINT_FORMAT_VERSION
import com.thelastjailer.app.checkpoint.CommitFault
import com.thelastjailer.app.checkpoint.Cp
import com.thelastjailer.app.checkpoint.SimulatedDevice
import com.thelastjailer.app.checkpoint.Story
import com.thelastjailer.app.commitChoiceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The boundary behind the B2 review: `beforeCommit` switches recording off, the arrival is replanned and the timeline
 * keys are restaged AFTER the gate's pre-apply check. If one editor operation throws at that point, the gate swallows
 * the exception (hooks are best effort by design) and goes on to `apply()`. These tests fail an editor operation at
 * every possible position of one such action and record what ended up persisted.
 *
 * The one rule: the persisted main-save map is either exactly the intended batch or exactly what it was before the
 * action. A half-staged batch must never reach `apply()`.
 */
class RestageBoundaryTest {

    private class Faulty(private val base: SharedPreferences) : SharedPreferences by base {
        var op = 0
        var failAt = -1
        var applies = 0
        var failApply = false
        override fun edit(): SharedPreferences.Editor = Ed(base.edit())
        private inner class Ed(private val real: SharedPreferences.Editor) : SharedPreferences.Editor {
            private fun tick() { op++; if (op == failAt) throw IllegalStateException("editor operation $op failed") }
            override fun putString(key: String?, value: String?) = also { tick(); real.putString(key, value) }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = also { tick(); real.putStringSet(key, values) }
            override fun putInt(key: String?, value: Int) = also { tick(); real.putInt(key, value) }
            override fun putLong(key: String?, value: Long) = also { tick(); real.putLong(key, value) }
            override fun putFloat(key: String?, value: Float) = also { tick(); real.putFloat(key, value) }
            override fun putBoolean(key: String?, value: Boolean) = also { tick(); real.putBoolean(key, value) }
            override fun remove(key: String?) = also { tick(); real.remove(key) }
            override fun clear() = also { real.clear() }
            override fun commit(): Boolean = throw AssertionError("commit() must never be used")
            override fun apply() { applies++; if (failApply) throw IllegalStateException("apply failed"); real.apply() }
        }
    }

    private class Run(
        val status: CommitStatus,
        val before: Map<String, Any?>,
        val after: Map<String, Any?>,
        val ckptBefore: Map<String, Any?>,
        val ckptAfter: Map<String, Any?>,
        val applies: Int,
        val ops: Int
    )

    /** The arrival into chapter 6 while a record is pending and the checkpoint file has turned out to be newer. */
    private fun scenario(failAt: Int, failApply: Boolean = false): Run {
        val device = SimulatedDevice()
        val prefs = Faulty(device.prefs(Story.LIVE))
        val store = SaveStore(prefs)
        val recorder = ArrivalRecorder(Cp.store(device), Cp.rules, true, { _, _ -> }, ArrivalRecorder.Hooks())
        device.armFault(Cp.file(1), 1, CommitFault.FALSE_UNCHANGED)
        val (n4, c4) = Story.crossing(4)
        assertEquals(CommitStatus.SAVED, store.commitChoiceResult(Story.stateAt(n4, 1, "a"), c4, recorder).status)
        device.writeDisk(
            Cp.file(1),
            mapOf("h.version" to CHECKPOINT_FORMAT_VERSION + 1, "h.slot" to 1, "h.lineage" to Cp.L1, "c05.future" to "data", "c05.crc" to 7L)
        )
        val before = device.disk(Story.LIVE)
        val ckptBefore = device.disk(Cp.file(1))
        prefs.op = 0
        prefs.applies = 0
        prefs.failAt = failAt
        prefs.failApply = failApply
        val (node, choice) = Story.crossing(5)
        val r = store.commitChoiceResult(Story.stateAt(node, 1, "a"), choice, recorder)
        if (failAt == -1) assertTrue("the shutoff must have happened inside the action", recorder.isDisabledForSlot(1))
        return Run(r.status, before, device.disk(Story.LIVE), ckptBefore, device.disk(Cp.file(1)), prefs.applies, prefs.op)
    }

    @Test
    fun `an editor operation failing at any position never persists a half-staged batch`() {
        val clean = scenario(failAt = -1)
        assertEquals(CommitStatus.SAVED, clean.status)
        val intended = clean.after
        assertNotEquals("the action must change something", clean.before, intended)
        val report = StringBuilder()
        val partial = ArrayList<Int>()
        for (k in 1..clean.ops) {
            val run = scenario(failAt = k)
            val persisted = when (run.after) {
                intended -> "intended"
                run.before -> "unchanged"
                else -> "PARTIAL"
            }
            report.append("op $k/${clean.ops}: status=${run.status} applies=${run.applies} persisted=$persisted\n")
            println("op $k/${clean.ops}: status=${run.status} applies=${run.applies} persisted=$persisted")
            if (persisted == "PARTIAL") partial += k
            assertEquals("op $k: checkpoint file untouched", run.ckptBefore, run.ckptAfter)
            if (persisted == "unchanged") assertTrue("op $k: nothing persisted must not be reported as saved", run.status != CommitStatus.SAVED)
        }
        assertTrue("failing these operations left a partial batch persisted: $partial\n$report", partial.isEmpty())
    }

    @Test
    fun `apply failing after the replan persists nothing and reports uncertain`() {
        val run = scenario(failAt = -1, failApply = true)
        assertEquals(CommitStatus.UNCERTAIN, run.status)
        assertEquals(run.before, run.after)
        assertEquals(run.ckptBefore, run.ckptAfter)
    }
}
