package com.thelastjailer.app.saveguard.gate

import android.content.SharedPreferences
import com.thelastjailer.app.StrictFakeSharedPreferences
import com.thelastjailer.app.saveguard.raw.AndroidXmlWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveGateTest {

    private fun seeded(): StrictFakeSharedPreferences = StrictFakeSharedPreferences()
        .putRaw("active_slot", 1)
        .putRaw("1.level", 1).putRaw("1.name", "One")
        .putRaw("2.level", 2).putRaw("2.name", "Two")
        .putRaw("3.level", 3)
        .putRaw("jailer_entitlement", 1)

    private fun open() = SaveGate(GateMode.Open)

    private fun SaveGate.write(
        prefs: StrictFakeSharedPreferences,
        slots: Set<Int> = setOf(1),
        active: Boolean = false,
        before: (() -> Unit)? = null,
        after: ((Unit) -> Unit)? = null,
        stage: (GuardedEditor) -> Unit
    ) = run(WriteIntent(slots, active), { prefs.edit() }, before, after, null, stage)

    // ---------------------------------------------------------------- mode table

    @Test fun `a new gate is frozen`() {
        val gate = SaveGate()
        val prefs = seeded()
        val before = prefs.fingerprint()
        val outcome = gate.write(prefs) { it.putInt("1.level", 9) }
        assertEquals(DenyReason.FROZEN, (outcome as GateOutcome.Denied).reason)
        assertEquals(before, prefs.fingerprint())
        assertEquals("no editor is even created when frozen", 0, prefs.editCalls)
    }

    @Test fun `every frozen reason denies and does nothing, not even the before hook`() {
        for (reason in FreezeReason.values()) {
            val gate = SaveGate(GateMode.Frozen(reason))
            val prefs = seeded()
            var beforeRan = false
            val o = gate.write(prefs, before = { beforeRan = true }) { it.putInt("1.level", 9) }
            assertTrue(reason.name, o is GateOutcome.Denied)
            assertFalse("before ran for $reason", beforeRan)
            assertEquals(0, prefs.editCalls)
        }
    }

    @Test fun `open mode hands a permitted write`() {
        val prefs = seeded()
        val o = open().write(prefs) { it.putInt("1.level", 9).putString("1.name", "New") }
        assertTrue(o is GateOutcome.Handed)
        assertEquals(9, prefs.getInt("1.level", 0))
        assertEquals("New", prefs.getString("1.name", ""))
        assertEquals("exactly one apply", 1, prefs.writeCalls)
    }

    @Test fun `partial mode denies a write whose intent includes a protected slot, before any side effect`() {
        val gate = SaveGate(GateMode.Partial(setOf(2)))
        val prefs = seeded()
        var beforeRan = false
        val o = gate.write(prefs, slots = setOf(2), before = { beforeRan = true }) { it.putInt("2.level", 9) }
        assertEquals(DenyReason.PROTECTED_SLOT, (o as GateOutcome.Denied).reason)
        assertFalse(beforeRan)
        assertEquals(0, prefs.editCalls)
        assertEquals(2, prefs.getInt("2.level", 0))
    }

    @Test fun `partial mode permits a write to an unprotected slot and leaves the protected slot untouched`() {
        val gate = SaveGate(GateMode.Partial(setOf(2)))
        val prefs = seeded()
        val protectedBefore = prefs.keys().filter { it.startsWith("2.") }.associateWith { prefs.all[it] }
        val o = gate.write(prefs, slots = setOf(1, 3), active = true) { it.putInt("1.level", 9).putInt("3.level", 8).putInt("active_slot", 3) }
        assertTrue(o is GateOutcome.Handed)
        val protectedAfter = prefs.keys().filter { it.startsWith("2.") }.associateWith { prefs.all[it] }
        assertEquals(protectedBefore, protectedAfter)
        assertEquals(9, prefs.getInt("1.level", 0))
    }

    @Test fun `staging a protected key inside an allowed intent is denied and nothing is applied`() {
        val gate = SaveGate(GateMode.Partial(setOf(2)))
        val prefs = seeded()
        val before = prefs.fingerprint()
        val o = gate.write(prefs, slots = setOf(1, 2)) { }  // intent itself covers 2
        assertTrue(o is GateOutcome.Denied)
        val o2 = gate.write(prefs, slots = setOf(1)) { it.putInt("1.level", 9); it.putInt("2.level", 99) }
        assertEquals(DenyReason.PROTECTED_SLOT, (o2 as GateOutcome.Denied).reason)
        assertEquals("the earlier staged 1.level is discarded too", before, prefs.fingerprint())
        assertEquals(0, prefs.writeCalls)
    }

    // ---------------------------------------------------------------- per-key policy

    @Test fun `keys outside the intent, foreign keys, null keys and the active slot without permission are denied`() {
        val cases: List<Pair<(GuardedEditor) -> Any?, DenyReason>> = listOf(
            ({ e: GuardedEditor -> e.putInt("3.level", 1) } to DenyReason.INTENT_MISMATCH),
            ({ e: GuardedEditor -> e.putInt("jailer_entitlement", 5) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.putInt("unknown", 5) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.putString(null, "x") } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.remove(null) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.putInt("active_slot", 3) } to DenyReason.INTENT_MISMATCH),
            ({ e: GuardedEditor -> e.putInt("x.level", 3) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.putInt("1level", 3) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.putInt(".level", 3) } to DenyReason.FORBIDDEN_KEY),
            ({ e: GuardedEditor -> e.remove("2.name") } to DenyReason.INTENT_MISMATCH)
        )
        for ((stage, expected) in cases) {
            val prefs = seeded()
            val before = prefs.fingerprint()
            val o = open().write(prefs, slots = setOf(1)) { e -> stage(e) }
            assertEquals(expected, (o as GateOutcome.Denied).reason)
            assertEquals(before, prefs.fingerprint())
            assertEquals(0, prefs.writeCalls)
        }
    }

    @Test fun `all typed puts and remove are policed identically`() {
        val prefs = seeded()
        val ed = open().run(WriteIntent(setOf(1)), { prefs.edit() }, null, null, null) { e ->
            e.putString("1.s", "v"); e.putInt("1.i", 1); e.putLong("1.l", 2L); e.putFloat("1.f", 0.5f); e.putBoolean("1.b", true)
            e.putStringSet("1.set", mutableSetOf("a")); e.remove("1.name")
        }
        assertTrue(ed is GateOutcome.Handed)
        assertFalse(prefs.contains("1.name"))
        assertEquals(0.5f, prefs.getFloat("1.f", 0f))
        for (bad in listOf<(GuardedEditor) -> Unit>(
            { it.putString("9.s", "v") }, { it.putInt("9.i", 1) }, { it.putLong("9.l", 2L) }, { it.putFloat("9.f", 1f) },
            { it.putBoolean("9.b", true) }, { it.putStringSet("9.set", mutableSetOf()) }, { it.remove("9.x") }
        )) {
            assertTrue(open().write(seeded(), slots = setOf(1)) { e -> bad(e) } is GateOutcome.Denied)
        }
    }

    @Test fun `clear commit and apply by the caller are forbidden and swallowing the exception does not help`() {
        for (call in listOf<(GuardedEditor) -> Unit>({ it.clear() }, { it.commit() }, { it.apply() })) {
            val prefs = seeded()
            val before = prefs.fingerprint()
            val o = open().write(prefs) { e -> try { call(e) } catch (ignored: WriteDeniedException) { } ; e.putInt("1.level", 7) }
            assertEquals(DenyReason.FORBIDDEN_CALL, (o as GateOutcome.Denied).reason)
            assertEquals(before, prefs.fingerprint())
            assertEquals(0, prefs.writeCalls)
        }
    }

    @Test fun `a swallowed key denial still denies the whole action`() {
        val prefs = seeded()
        val before = prefs.fingerprint()
        val o = open().write(prefs) { e ->
            e.putInt("1.level", 7)
            try { e.putInt("3.level", 7) } catch (ignored: WriteDeniedException) { }
        }
        assertEquals(DenyReason.INTENT_MISMATCH, (o as GateOutcome.Denied).reason)
        assertEquals(before, prefs.fingerprint())
    }

    @Test fun `a retained editor is dead after the action`() {
        var kept: GuardedEditor? = null
        val prefs = seeded()
        open().write(prefs) { kept = it; it.putInt("1.level", 2) }
        val before = prefs.fingerprint()
        try { kept!!.putInt("1.level", 3); throw AssertionError("must throw") } catch (e: WriteDeniedException) { assertEquals(DenyReason.FORBIDDEN_CALL, e.reason) }
        assertEquals(before, prefs.fingerprint())
    }

    // ---------------------------------------------------------------- outcomes and side effects

    @Test fun `an exception from the stage is Failed and nothing is applied`() {
        val prefs = seeded()
        val before = prefs.fingerprint()
        val o = open().write(prefs) { it.putInt("1.level", 5); throw IllegalStateException("boom") }
        assertTrue(o is GateOutcome.Failed)
        assertEquals(before, prefs.fingerprint())
    }

    @Test fun `editor creation failure is Failed`() {
        val o = open().run(WriteIntent(setOf(1)), { throw IllegalStateException("no editor") }, null, null, null) { }
        assertTrue(o is GateOutcome.Failed)
    }

    @Test fun `apply that throws is Uncertain, the hook runs and after does not`() {
        var uncertain = 0
        var afterRan = false
        val broken = object : SharedPreferences.Editor {
            override fun putString(k: String?, v: String?) = this
            override fun putStringSet(k: String?, v: MutableSet<String>?) = this
            override fun putInt(k: String?, v: Int) = this
            override fun putLong(k: String?, v: Long) = this
            override fun putFloat(k: String?, v: Float) = this
            override fun putBoolean(k: String?, v: Boolean) = this
            override fun remove(k: String?) = this
            override fun clear() = this
            override fun commit(): Boolean = true
            override fun apply() { throw RuntimeException("disk") }
        }
        val o = open().run(WriteIntent(setOf(1)), { broken }, null, { afterRan = true }, { uncertain++ }) { it.putInt("1.level", 1) }
        assertTrue(o is GateOutcome.Uncertain)
        assertEquals(1, uncertain)
        assertFalse(afterRan)
    }

    @Test fun `before and after run in order on success, and their failures are counted not fatal`() {
        val log = ArrayList<String>()
        val prefs = seeded()
        val o = open().write(prefs, before = { log += "before"; throw RuntimeException("x") }, after = { log += "after"; throw RuntimeException("y") }) {
            log += "stage"; it.putInt("1.level", 4)
        }
        assertEquals(listOf("before", "stage", "after"), log)
        assertEquals(2, (o as GateOutcome.Handed).sideEffectFailures)
        assertEquals(4, prefs.getInt("1.level", 0))
    }

    @Test fun `after does not run when the action is denied or fails`() {
        var ran = false
        open().write(seeded(), after = { ran = true }) { it.putInt("9.x", 1) }
        open().write(seeded(), after = { ran = true }) { throw RuntimeException() }
        assertFalse(ran)
    }

    @Test fun `a nested run on the same thread is denied`() {
        val gate = open()
        val prefs = seeded()
        var inner: GateOutcome<Unit>? = null
        val outer = gate.write(prefs) { e ->
            inner = gate.write(prefs) { it.putInt("1.level", 50) }
            e.putInt("1.level", 2)
        }
        assertEquals(DenyReason.NESTED, (inner as GateOutcome.Denied).reason)
        assertTrue(outer is GateOutcome.Handed)
        assertEquals(2, prefs.getInt("1.level", 0))
        assertEquals(1, prefs.writeCalls)
    }

    @Test fun `changing the mode inside an action is refused`() {
        val gate = open()
        var refused = false
        gate.write(seeded()) { e ->
            try { gate.setMode(GateMode.Open) } catch (ex: IllegalStateException) { refused = true }
            e.putInt("1.level", 2)
        }
        assertTrue(refused)
    }

    // ---------------------------------------------------------------- concurrency

    @Test fun `a mode change from another thread waits for the running action, so a permitted action is never applied under a later mode`() {
        val gate = open()
        val prefs = seeded()
        val inside = CountDownLatch(1)
        val release = CountDownLatch(1)
        val outcome = arrayOfNulls<GateOutcome<Unit>>(1)
        val writer = Thread { outcome[0] = gate.write(prefs) { e -> e.putInt("1.level", 77); inside.countDown(); release.await(5, TimeUnit.SECONDS) } }
        writer.start()
        assertTrue(inside.await(5, TimeUnit.SECONDS))
        val freezer = Thread { gate.setMode(GateMode.Frozen(FreezeReason.SUSPECT_FILE)) }
        freezer.start()
        Thread.sleep(150)
        assertTrue("freeze must be blocked while the action holds the lock", freezer.isAlive)
        release.countDown()
        writer.join(5000); freezer.join(5000)
        assertTrue(outcome[0] is GateOutcome.Handed)
        assertEquals(77, prefs.getInt("1.level", 0))
        // after the freeze, further writes are denied
        assertTrue(gate.write(prefs) { it.putInt("1.level", 1) } is GateOutcome.Denied)
        assertEquals(77, prefs.getInt("1.level", 0))
    }

    @Test fun `concurrent actions never interleave their batches`() {
        val gate = open()
        val prefs = seeded()
        val handed = AtomicInteger()
        val threads = (1..8).map { t ->
            Thread { repeat(200) { n ->
                val o = gate.write(prefs) { e -> e.putInt("1.level", t * 1000 + n); e.putString("1.name", "$t:$n") }
                if (o is GateOutcome.Handed) handed.incrementAndGet()
            } }
        }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        assertEquals(1600, handed.get())
        val level = prefs.getInt("1.level", 0)
        assertEquals("level and name come from the same action", "${level / 1000}:${level % 1000}", prefs.getString("1.name", ""))
        assertEquals(1600, prefs.writeCalls)
    }

    @Test fun `current mode is observable and reflects the last set`() {
        val gate = SaveGate()
        assertTrue(gate.currentMode() is GateMode.Frozen)
        gate.setMode(GateMode.Partial(setOf(1)))
        assertEquals(setOf(1), (gate.currentMode() as GateMode.Partial).protectedSlots)
        gate.setMode(GateMode.Open)
        assertTrue(gate.currentMode() is GateMode.Open)
    }

    // ---------------------------------------------------------------- preservation in serialised form

    @Test fun `protected slot bytes in the serialised file are identical before and after permitted writes, with a negative control`() {
        val gate = SaveGate(GateMode.Partial(setOf(2)))
        val prefs = seeded()
        fun slot2Xml(): String {
            val only = prefs.all.filterKeys { it.startsWith("2.") }.mapValues { it.value!! }
            return String(AndroidXmlWriter.write(only), Charsets.UTF_8)
        }
        val before = slot2Xml()
        repeat(20) { n -> gate.write(prefs, slots = setOf(1, 3), active = true) { it.putInt("1.level", n).putInt("3.level", n).putInt("active_slot", 1 + (n % 2) * 2) } }
        assertEquals(before, slot2Xml())
        // negative control: an unguarded editor does change slot 2, so the assertion above is not vacuous
        prefs.edit().putInt("2.level", 999).apply()
        assertFalse(before == slot2Xml())
        assertNotNull(prefs.getInt("2.level", 0))
    }

    @Test fun `exhaustive sweep, every key in every mode is either permitted by the table or denied without any write`() {
        val keys = listOf("active_slot", "1.level", "2.level", "3.level", "jailer_entitlement", "zzz", "10.level", "0.level")
        val modes = listOf(GateMode.Open, GateMode.Partial(setOf(2)), GateMode.Partial(setOf(1, 2, 3)), GateMode.Frozen(FreezeReason.SUSPECT_FILE))
        val intents = listOf(WriteIntent(emptySet()), WriteIntent(setOf(1)), WriteIntent(setOf(2)), WriteIntent(setOf(1, 3), true), WriteIntent(setOf(2), true))
        for (m in modes) for (intent in intents) for (k in keys) {
            val prefs = seeded()
            val before = prefs.fingerprint()
            val o = SaveGate(m).run(intent, { prefs.edit() }, null, null, null) { it.putInt(k, 5) }
            val slot = GuardedEditor.slotOf(k)
            val protected = (m as? GateMode.Partial)?.protectedSlots ?: emptySet()
            val permitted = m !is GateMode.Frozen && intent.slots.none { it in protected } && when {
                slot != null -> slot in intent.slots && slot !in protected
                k == "active_slot" -> intent.touchesActiveSlot
                else -> false
            }
            if (permitted) {
                assertTrue("$m $k", o is GateOutcome.Handed)
                assertEquals(5, prefs.getInt(k, 0))
            } else {
                assertTrue("$m ${intent.slots} $k", o is GateOutcome.Denied)
                assertEquals(before, prefs.fingerprint())
                assertEquals(0, prefs.writeCalls)
            }
        }
    }
}
