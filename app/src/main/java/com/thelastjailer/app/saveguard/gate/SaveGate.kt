package com.thelastjailer.app.saveguard.gate

import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantLock

// G2L Stage B1 (dormant): the one gate every write to the save file will pass through once B2 wires it in.
//
// Contract (design freeze section 4 and amendments A2, C3):
//  * ONE logical game action = ONE lock acquisition = ONE editor batch. Permission check, editor creation, every
//    staged key and the single apply() all happen inside the same lock scope, and the gate's mode cannot change
//    while the lock is held, so "allowed, then forbidden before apply()" is impossible by construction.
//  * A denied action builds no editor and runs no side effect: not even the optional `before` hook (the checkpoint
//    maintenance that runs ahead of a save today).
//  * Outcomes are explicit. HANDED means "the gate allowed it and apply() was called", NOT "the disk has it":
//    SharedPreferences.apply() gives no result and can lose the write on process death. UNCERTAIN means apply()
//    itself threw, so memory may or may not have changed; the caller must re-read instead of trusting its own state.
//  * No claim is made that the save file and any other file commit together. `before` and `after` are best-effort
//    side effects whose failures are counted and never change the outcome.
//
// Pure of file access, logging and keys' values. Denial reasons never contain a key.

sealed class GateMode {
    /** Everything is allowed. */
    object Open : GateMode()

    /** Nothing is written. */
    class Frozen(val reason: FreezeReason) : GateMode()

    /** A verified copy of the damaged file exists; writes are allowed except to these slots' keys. */
    class Partial(val protectedSlots: Set<Int>) : GateMode()
}

enum class FreezeReason {
    NOT_STARTED, PROBING, SUSPECT_FILE, EVIDENCE_NOT_SECURED, QUARANTINE_FAILED, CAP_FULL, ATTEMPTS_EXHAUSTED, BUDGET_FULL, UNSTABLE_READ, PREFS_UNREADABLE
}

enum class DenyReason { FROZEN, PROTECTED_SLOT, FORBIDDEN_KEY, FORBIDDEN_CALL, NESTED, GATE_CHANGED, INTENT_MISMATCH }

/** What a logical action declares up front. The editor refuses any key outside it. */
class WriteIntent(val slots: Set<Int>, val touchesActiveSlot: Boolean = false)

class WriteDeniedException(val reason: DenyReason) : RuntimeException(reason.name)

sealed class GateOutcome<out T> {
    /** Allowed and handed to SharedPreferences. Not a durability claim. */
    class Handed<T>(val value: T, val sideEffectFailures: Int) : GateOutcome<T>()

    /** Nothing was written, no side effect ran. */
    class Denied(val reason: DenyReason) : GateOutcome<Nothing>()

    /** Staging failed before apply(): nothing was applied. */
    class Failed(val cause: Exception) : GateOutcome<Nothing>()

    /** apply() itself threw: the in-memory map may or may not have changed. */
    class Uncertain(val cause: Exception) : GateOutcome<Nothing>()
}

class SaveGate(initial: GateMode = GateMode.Frozen(FreezeReason.NOT_STARTED)) {
    private val lock = ReentrantLock()
    private var mode: GateMode = initial
    private var generation = 0L

    /** Blocks while a logical action is in progress, so the answer is never a half-way value. */
    fun currentMode(): GateMode {
        lock.lock()
        try { return mode } finally { lock.unlock() }
    }

    fun setMode(next: GateMode) {
        check(!lock.isHeldByCurrentThread) { "the gate's mode cannot change inside a write" }
        lock.lock()
        try {
            mode = next
            generation++
        } finally {
            lock.unlock()
        }
    }

    /**
     * Runs one logical action. [stage] receives the only editor it may use and must only stage puts/removes; it must
     * not call apply() or commit() (the gate applies once, afterwards).
     */
    fun <T> run(
        intent: WriteIntent,
        newEditor: () -> SharedPreferences.Editor,
        before: (() -> Unit)? = null,
        after: ((T) -> Unit)? = null,
        onUncertain: (() -> Unit)? = null,
        stage: (GuardedEditor) -> T
    ): GateOutcome<T> {
        if (lock.isHeldByCurrentThread) return GateOutcome.Denied(DenyReason.NESTED)
        lock.lock()
        try {
            val startGeneration = generation
            val current = mode
            val protectedSlots: Set<Int> = when (current) {
                is GateMode.Frozen -> return GateOutcome.Denied(DenyReason.FROZEN)
                is GateMode.Partial -> {
                    if (intent.slots.any { it in current.protectedSlots }) return GateOutcome.Denied(DenyReason.PROTECTED_SLOT)
                    current.protectedSlots
                }
                is GateMode.Open -> emptySet()
            }
            var sideEffectFailures = 0
            if (before != null) {
                try { before() } catch (e: Exception) { sideEffectFailures++ }
            }
            val real = try { newEditor() } catch (e: Exception) { return GateOutcome.Failed(e) }
            val editor = GuardedEditor(real, intent, protectedSlots) { generation == startGeneration }
            val value = try {
                stage(editor)
            } catch (e: WriteDeniedException) {
                editor.close()
                return GateOutcome.Denied(e.reason)
            } catch (e: Exception) {
                editor.close()
                return GateOutcome.Failed(e)
            }
            try {
                editor.applyOnce()
            } catch (e: WriteDeniedException) {
                return GateOutcome.Denied(e.reason)
            } catch (e: Exception) {
                if (onUncertain != null) {
                    try { onUncertain() } catch (ignored: Exception) { sideEffectFailures++ }
                }
                return GateOutcome.Uncertain(e)
            }
            if (after != null) {
                try { after(value) } catch (e: Exception) { sideEffectFailures++ }
            }
            return GateOutcome.Handed(value, sideEffectFailures)
        } finally {
            lock.unlock()
        }
    }
}

/**
 * Wraps a real editor. Every key is checked against the action's [WriteIntent] and the protected slots at the moment
 * it is staged. commit() and clear() are never allowed (nothing in the app uses them, and clear() could wipe a
 * protected slot); apply() is reserved for the gate.
 */
class GuardedEditor internal constructor(
    private val real: SharedPreferences.Editor,
    private val intent: WriteIntent,
    private val protectedSlots: Set<Int>,
    private val stillCurrent: () -> Boolean
) : SharedPreferences.Editor {

    private var violation: DenyReason? = null
    private var closed = false
    private var applied = false

    internal fun close() { closed = true }

    private fun check(key: String?) {
        if (closed || applied) throw WriteDeniedException(DenyReason.FORBIDDEN_CALL)
        val verdict = verdictFor(key)
        if (verdict != null) {
            violation = verdict
            throw WriteDeniedException(verdict)
        }
    }

    private fun verdictFor(key: String?): DenyReason? {
        if (key == null) return DenyReason.FORBIDDEN_KEY
        val slot = slotOf(key)
        if (slot != null) {
            if (slot in protectedSlots) return DenyReason.PROTECTED_SLOT
            if (slot !in intent.slots) return DenyReason.INTENT_MISMATCH
            return null
        }
        if (key == ACTIVE_SLOT_KEY) return if (intent.touchesActiveSlot) null else DenyReason.INTENT_MISMATCH
        return DenyReason.FORBIDDEN_KEY
    }

    override fun putString(key: String?, value: String?): SharedPreferences.Editor { check(key); real.putString(key, value); return this }
    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { check(key); real.putStringSet(key, values); return this }
    override fun putInt(key: String?, value: Int): SharedPreferences.Editor { check(key); real.putInt(key, value); return this }
    override fun putLong(key: String?, value: Long): SharedPreferences.Editor { check(key); real.putLong(key, value); return this }
    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { check(key); real.putFloat(key, value); return this }
    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { check(key); real.putBoolean(key, value); return this }
    override fun remove(key: String?): SharedPreferences.Editor { check(key); real.remove(key); return this }

    override fun clear(): SharedPreferences.Editor { violation = DenyReason.FORBIDDEN_CALL; throw WriteDeniedException(DenyReason.FORBIDDEN_CALL) }
    override fun commit(): Boolean { violation = DenyReason.FORBIDDEN_CALL; throw WriteDeniedException(DenyReason.FORBIDDEN_CALL) }
    override fun apply() { violation = DenyReason.FORBIDDEN_CALL; throw WriteDeniedException(DenyReason.FORBIDDEN_CALL) }

    internal fun applyOnce() {
        if (applied || closed) throw WriteDeniedException(DenyReason.FORBIDDEN_CALL)
        applied = true
        violation?.let { throw WriteDeniedException(it) }
        if (!stillCurrent()) throw WriteDeniedException(DenyReason.GATE_CHANGED)
        real.apply()
    }

    companion object {
        private const val ACTIVE_SLOT_KEY = "active_slot"
        private val SLOT_PREFIX = Regex("^([0-9]{1,9})\\..*")

        /** The slot a key belongs to (`<slot>.<anything>`), or null for a key that is not slot-scoped. */
        fun slotOf(key: String): Int? = SLOT_PREFIX.matchEntire(key)?.groupValues?.get(1)?.toIntOrNull()
    }
}
