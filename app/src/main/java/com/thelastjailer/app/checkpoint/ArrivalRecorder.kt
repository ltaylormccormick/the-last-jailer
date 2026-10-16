package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.StoryRepository
import java.util.TreeMap

/**
 * What the live save must do about the slot's timeline for one transition, and what to write to the
 * checkpoint file once the live save has been issued. Produced by [ArrivalRecorder.plan]; pure data.
 */
class ArrivalPlan internal constructor(
    val timeline: TimelineWrite,
    internal val action: Action?
) {
    internal sealed interface Action {
        val lineage: String
        val records: List<CheckpointRecord>
    }

    /** First records under a new lineage: replace the whole file. */
    internal class ReplaceAll(override val lineage: String, override val records: List<CheckpointRecord>) : Action

    /** A forward arrival on an existing timeline: one atomic replace-from-K. */
    internal class ReplaceFrom(override val lineage: String, val record: CheckpointRecord) : Action {
        override val records: List<CheckpointRecord> get() = listOf(record)
    }

    companion object {
        internal val KEEP = ArrivalPlan(TimelineWrite.Keep, null)
        internal val INVALIDATE = ArrivalPlan(TimelineWrite.Invalidate, null)
    }
}

/**
 * Records chapter arrivals into the checkpoint files, **invisibly**: nothing it does can change the
 * player's transition, throw, block, or show anything. This is the "proof-bound arrivals" design:
 *
 *  * The live save is the only commit point. Each arrival's digest goes into the slot's ledger in the same
 *    editor batch as the state ([ArrivalPlan.timeline]); the checkpoint file is written afterwards
 *    ([afterSave]). Correctness does not depend on which of the two reaches disk first, or whether either
 *    does: a record is trusted only when the live ledger vouches for it.
 *  * A record is built once, from the in-memory state at the moment of arrival, never from whatever is
 *    loaded later. A lost record is rebuilt only when the live state hashes to exactly the ledger's
 *    digest ([beforeCommit], rule R), or re-sent verbatim from memory while the ledger still vouches for it.
 *  * Anything that starts a different timeline (Start Again, the Chapter 30 to 1 loop, SAVE into another
 *    slot) drops the lineage and ledger in the same batch. An arrival always drops ledger entries from its
 *    chapter upward, **even when recording is off**, so an abandoned branch can never be re-trusted.
 *
 * Every public method is total: unexpected exceptions are contained and logged. A plan that cannot be
 * computed degrades to [TimelineWrite.Invalidate]: history may be lost, never wrong.
 *
 * One recorder, over one [CheckpointStore], should exist per process.
 */
class ArrivalRecorder(
    private val store: CheckpointStore,
    private val rules: CheckpointRules = CheckpointRules.forStory(),
    /** The kill switch. Off: no checkpoint file is read or written, but the ledger is still maintained. */
    private val recordingEnabled: Boolean = true,
    private val log: (String, Throwable?) -> Unit = { _, _ -> },
    private val hooks: Hooks = Hooks()
) {
    /** Seams so tests can inject failures into the pure parts. Production uses the defaults. */
    class Hooks(
        val newLineage: () -> String = { Lineage.generate() },
        val digest: (Int, CheckpointRecord) -> String = { slot, record -> CheckpointDigest.of(slot, record) },
        val buildRecord: (GameState, Int, String) -> CheckpointRecord = { state, chapter, lineage ->
            CheckpointRecord.fromState(state, chapter, lineage)
        }
    )

    private class Pending(val lineage: String, val records: TreeMap<Int, CheckpointRecord>, var attempts: Int = 0)

    private val pending = HashMap<Int, Pending>()
    private val disabled = HashSet<Int>()

    /** Per slot: (lineage, chapter, ledger digest) triples this process has seen present in, or written to, the file. */
    private val confirmed = HashMap<Int, HashSet<Triple<String, Int, String>>>()

    /** True while recording is on for [slot]: the kill switch is on and the slot was not shut off. */
    fun isRecording(slot: Int): Boolean = synchronized(this) { recordingEnabled && slot !in disabled }

    /** True after a newer-format checkpoint file was found; recording stays off for the slot this process. */
    fun isDisabledForSlot(slot: Int): Boolean = synchronized(this) { slot in disabled }

    // ---- 1. before a state change: repair what can be proven ----------------------------------------------

    /**
     * Called at the start of every state-changing commit with the **pre-change** state and the slot's live
     * timeline. Retries records that failed to write while the app stayed alive, then applies rule R: if the
     * ledger's latest entry is for the chapter the player is standing in, no usable record exists, and the
     * current state hashes to exactly that entry, it *is* the arrival state, so write it.
     */
    fun beforeCommit(slot: Int, state: GameState, timeline: Timeline?) {
        try {
            synchronized(this) {
                if (!recordingEnabled || slot in disabled) return
                retryPending(slot, timeline)
                reconcile(slot, state, timeline)
            }
        } catch (e: Exception) {
            log("beforeCommit failed for slot $slot", e)
        }
    }

    private fun retryPending(slot: Int, timeline: Timeline?) {
        val p = pending[slot] ?: return
        if (timeline == null || timeline.lineage != p.lineage) {
            pending.remove(slot)
            return
        }
        val stillVouched = p.records.filter { (chapter, record) ->
            timeline.ledger.digestFor(chapter) == hooks.digest(slot, record)
        }
        if (stillVouched.isEmpty()) {
            pending.remove(slot)
            return
        }
        if (!store.isTrusted(slot)) return
        if (p.attempts >= MAX_RETRIES) {
            pending.remove(slot)
            return
        }
        p.attempts += 1
        val result = writeRecords(slot, p.lineage, stillVouched.map { it.value })
        if (result === StoreWriteResult.Committed) {
            stillVouched.forEach { (chapter, record) -> confirm(slot, p.lineage, chapter, hooks.digest(slot, record)) }
            pending.remove(slot)
        } else {
            forget(slot, stillVouched.keys.min())
            p.records.keys.retainAll(stillVouched.map { it.key }.toSet())
        }
    }

    /**
     * Rule R. Pure first, I/O only if there is something provable to repair: a ledger entry can be rebuilt
     * only from a state whose digest equals the entry exactly, so the candidates are computed without
     * touching the file. A state that has moved on therefore costs nothing (no read, no write, and nothing
     * cached that could outlive a changed store), and a lost record stays recoverable for as long as its
     * proof state exists.
     *
     * Two candidates exist: the ledger's latest entry when the player is standing in that chapter (the live
     * state is the arrival state), and the Chapter 1 genesis entry, which can be rebuilt only from the exact
     * fresh `GameState` of this slot. A Chapter 1 record that did not come from a fresh game (the Chapter 30
     * to 1 loop) never matches that digest and is never rebuilt.
     */
    private fun reconcile(slot: Int, state: GameState, timeline: Timeline?) {
        if (timeline == null) return
        val lineage = timeline.lineage
        fun alreadyKnown(chapter: Int, digest: String) = confirmed[slot]?.contains(Triple(lineage, chapter, digest)) == true
        val candidates = TreeMap<Int, CheckpointRecord>()
        timeline.ledger.latest()?.let { (chapter, digest) ->
            if (chapterNumberOf(state) == chapter && !alreadyKnown(chapter, digest)) {
                val rebuilt = hooks.buildRecord(state, chapter, lineage)
                if (hooks.digest(slot, rebuilt) == digest) candidates[chapter] = rebuilt
            }
        }
        timeline.ledger.digestFor(1)?.let { digest ->
            if (1 !in candidates && !alreadyKnown(1, digest)) {
                val genesis = hooks.buildRecord(GameState(activeSlot = slot), 1, lineage)
                if (hooks.digest(slot, genesis) == digest) candidates[1] = genesis
            }
        }
        val unconfirmed: Map<Int, CheckpointRecord> = candidates
        if (unconfirmed.isEmpty()) return
        if (!store.isTrusted(slot)) return
        val read = store.readAll(slot, lineage)
        if (read.memoryMayDiffer) return
        val present = unconfirmed.filter { (chapter, rebuilt) ->
            read.records.any { it.chapterNumber == chapter && hooks.digest(slot, it) == hooks.digest(slot, rebuilt) }
        }
        present.forEach { (chapter, rebuilt) -> confirm(slot, lineage, chapter, hooks.digest(slot, rebuilt)) }
        val missing = unconfirmed.filterKeys { it !in present.keys }.toSortedMap()
        if (missing.isEmpty()) return
        val result = writeRecords(slot, lineage, missing.values.toList())
        if (result === StoreWriteResult.Committed) {
            missing.forEach { (chapter, rebuilt) -> confirm(slot, lineage, chapter, hooks.digest(slot, rebuilt)) }
        } else {
            forget(slot, missing.firstKey())
            remember(slot, lineage, missing.values.toList())
        }
    }

    private fun confirm(slot: Int, lineage: String, chapter: Int, digest: String) {
        val set = confirmed.getOrPut(slot) { HashSet() }
        set.removeAll { it.second == chapter }
        set.add(Triple(lineage, chapter, digest))
    }

    /** Drops what is remembered as present for [fromChapter] and above: the file may no longer say so. */
    private fun forget(slot: Int, fromChapter: Int) {
        confirmed[slot]?.removeAll { it.second >= fromChapter }
    }

    // ---- 2. planning: pure, no I/O, never throws ----------------------------------------------------------

    /**
     * Decides, for the transition [state] -> [next], what the live-save batch does to the timeline and what
     * to write to the checkpoint file afterwards. [current] is the slot's live timeline before the change.
     *
     * | transition                                   | live batch                                   | after |
     * |----------------------------------------------|----------------------------------------------|-------|
     * | same chapter                                 | Keep                                         | none  |
     * | forward to K, timeline exists                | ledger without >=K, plus K                   | replaceFrom(K) |
     * | forward to K, no timeline                    | new lineage, ledger {K}                      | replaceAll |
     * | forward, recording off or slot disabled      | ledger without >=K (Invalidate if no lineage)| none  |
     * | Chapter 30 to 1                              | new lineage, ledger {1}                      | replaceAll |
     * | unknown or any other non-forward change      | Invalidate                                   | none  |
     * | fresh game, first choice (no timeline)       | additionally record Chapter 1 from [state]   | in the same replaceAll |
     */
    fun plan(state: GameState, next: GameState, current: Timeline?): ArrivalPlan = try {
        planUnsafe(state, next, current)
    } catch (e: Exception) {
        log("plan failed; invalidating the slot's timeline", e)
        ArrivalPlan.INVALIDATE
    }

    private fun planUnsafe(state: GameState, next: GameState, current: Timeline?): ArrivalPlan {
        val slot = next.activeSlot
        val max = rules.maxChapter
        val before = chapterNumberOf(state)
        val after = chapterNumberOf(next)
        val recording = isRecording(slot)

        if (before == null || after == null || before !in 1..max || after !in 1..max) return ArrivalPlan.INVALIDATE
        if (before > after) {
            // The only backward edge in the story is the Chapter 30 to Chapter 1 "the watch goes on" loop.
            return if (before == max && after == 1) newTimeline(slot, next, recording) else ArrivalPlan.INVALIDATE
        }

        var lineage = current?.lineage
        var ledger = current?.ledger ?: Ledger.EMPTY
        val records = ArrayList<CheckpointRecord>()

        // A brand-new game's first choice: the pre-choice state is provably the Chapter 1 arrival state.
        if (recording && current == null && before == 1 && state == GameState(activeSlot = slot)) {
            val fresh = hooks.newLineage()
            lineage = fresh
            val genesis = hooks.buildRecord(state, 1, fresh)
            records += genesis
            ledger = ledger.with(1, hooks.digest(slot, genesis))
        }

        if (after > before) {
            ledger = ledger.without(after) // always, recording or not: an abandoned branch must not match again
            if (recording) {
                val l = lineage ?: hooks.newLineage().also { lineage = it }
                val arrival = hooks.buildRecord(next, after, l)
                records += arrival
                ledger = ledger.with(after, hooks.digest(slot, arrival))
            }
        }

        val finalLineage = lineage ?: return ArrivalPlan.INVALIDATE
        val timeline = if (current != null && current.lineage == finalLineage && current.ledger == ledger) {
            TimelineWrite.Keep
        } else {
            TimelineWrite.Set(finalLineage, ledger)
        }
        val action: ArrivalPlan.Action? = when {
            records.isEmpty() -> null
            current == null -> ArrivalPlan.ReplaceAll(finalLineage, records)
            else -> ArrivalPlan.ReplaceFrom(finalLineage, records.single())
        }
        return ArrivalPlan(timeline, action)
    }

    /** Start Again: a new timeline whose Chapter 1 record is the known fresh state. */
    fun planRestart(fresh: GameState): ArrivalPlan = try {
        newTimeline(fresh.activeSlot, fresh, isRecording(fresh.activeSlot))
    } catch (e: Exception) {
        log("planRestart failed; invalidating the slot's timeline", e)
        ArrivalPlan.INVALIDATE
    }

    private fun newTimeline(slot: Int, state: GameState, recording: Boolean): ArrivalPlan {
        if (!recording) return ArrivalPlan.INVALIDATE
        val lineage = hooks.newLineage()
        val record = hooks.buildRecord(state, 1, lineage)
        val ledger = Ledger.EMPTY.with(1, hooks.digest(slot, record))
        return ArrivalPlan(TimelineWrite.Set(lineage, ledger), ArrivalPlan.ReplaceAll(lineage, listOf(record)))
    }

    // ---- 3. after the live save was issued ----------------------------------------------------------------

    /**
     * Writes the checkpoint file for [plan]. The result never matters to the player: a failure keeps the
     * frozen records in memory for a retry and is logged. Contains every exception.
     */
    fun afterSave(slot: Int, plan: ArrivalPlan) {
        val action = plan.action ?: return
        try {
            synchronized(this) {
                if (!recordingEnabled || slot in disabled) return
                // Before the write, not after: even if the write never returns, the file may now differ from
                // what was remembered about it, so nothing stale may be trusted as "already present".
                when (action) {
                    is ArrivalPlan.ReplaceAll -> confirmed.remove(slot)
                    is ArrivalPlan.ReplaceFrom -> forget(slot, action.record.chapterNumber)
                }
                val result = when (action) {
                    is ArrivalPlan.ReplaceAll -> store.replaceAll(slot, action.lineage, action.records)
                    is ArrivalPlan.ReplaceFrom -> store.replaceFrom(slot, action.lineage, action.record)
                }
                if (result === StoreWriteResult.Committed) {
                    action.records.forEach { confirm(slot, action.lineage, it.chapterNumber, hooks.digest(slot, it)) }
                    when (action) {
                        is ArrivalPlan.ReplaceAll -> pending.remove(slot)
                        is ArrivalPlan.ReplaceFrom -> pending[slot]?.let { p ->
                            p.records.tailMap(action.record.chapterNumber, true).clear() // superseded by this arrival
                            if (p.records.isEmpty()) pending.remove(slot)
                        }
                    }
                } else {
                    noteFailure(slot, action, result)
                }
            }
        } catch (e: Exception) {
            log("afterSave failed for slot $slot", e)
            try {
                synchronized(this) { remember(slot, action.lineage, action.records) }
            } catch (inner: Exception) {
                log("could not remember records for retry", inner)
            }
        }
    }

    private fun noteFailure(slot: Int, action: ArrivalPlan.Action, result: StoreWriteResult) {
        if (result is StoreWriteResult.Rejected && store.fileState(slot) == FileState.UNSUPPORTED_VERSION) {
            shutOff(slot)
            return
        }
        log("checkpoint write for slot $slot did not commit: $result", null)
        remember(slot, action.lineage, action.records)
    }

    // ---- shared helpers -------------------------------------------------------------------------------------

    /**
     * Writes [records] using the least destructive operation that is allowed: incremental upserts when the
     * file is empty or already belongs to [lineage]; a whole-file replace only when the file belongs to
     * another timeline (and so holds nothing usable for this one). A newer-format file is never touched and
     * switches recording off for the slot.
     */
    private fun writeRecords(slot: Int, lineage: String, records: List<CheckpointRecord>): StoreWriteResult {
        val state = store.fileState(slot)
        if (state == FileState.UNSUPPORTED_VERSION) {
            shutOff(slot)
            return StoreWriteResult.Rejected("checkpoint file was written by a newer version")
        }
        val sameTimeline = state == FileState.EMPTY || (state == FileState.OK && store.storedLineage(slot) == lineage)
        if (!sameTimeline) return store.replaceAll(slot, lineage, records)
        var result: StoreWriteResult = StoreWriteResult.Committed
        for (record in records.sortedBy { it.chapterNumber }) {
            result = store.upsert(slot, lineage, record)
            if (result !== StoreWriteResult.Committed) break
        }
        return result
    }

    private fun remember(slot: Int, lineage: String, records: List<CheckpointRecord>) {
        val p = pending[slot]?.takeIf { it.lineage == lineage } ?: Pending(lineage, TreeMap()).also { pending[slot] = it }
        records.forEach { p.records[it.chapterNumber] = it }
    }

    private fun shutOff(slot: Int) {
        disabled.add(slot)
        pending.remove(slot)
        confirmed.remove(slot)
        log("slot $slot: checkpoint file is from a newer version; recording is off for this slot", null)
    }

    private fun chapterNumberOf(state: GameState): Int? =
        ChapterIds.numberOf(StoryRepository.nodeOrNull(state.sceneId)?.chapterId ?: state.chapterId)

    private companion object {
        const val MAX_RETRIES = 5
    }
}
