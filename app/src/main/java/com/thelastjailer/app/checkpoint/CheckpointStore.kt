package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences

/** How a write to a checkpoint file ended. Only [Committed] and [RolledBack] leave the file trustworthy. */
sealed class StoreWriteResult {
    /** `commit()` returned true: the new contents are on disk. */
    object Committed : StoreWriteResult() {
        override fun toString() = "Committed"
    }

    /** Refused before anything was written (invalid input, wrong lineage, untrusted store). Nothing changed. */
    data class Rejected(val reason: String) : StoreWriteResult()

    /**
     * The write failed, and a second, compensating `commit()` of the previous contents *succeeded*. The
     * previous data is therefore verifiably back on disk and in memory.
     */
    data class RolledBack(val cause: String) : StoreWriteResult()

    /**
     * The write failed and the previous contents could **not** be restored. `SharedPreferences` can keep
     * the failed write's new values visible in memory even though they never reached disk, so nothing
     * read from this slot's file in this process can be trusted. The store refuses further work on the
     * slot until the app restarts and reloads from disk.
     */
    data class StateUnknown(val cause: String) : StoreWriteResult()

    val isCommitted: Boolean get() = this is Committed
}

enum class FileState { EMPTY, OK, LINEAGE_MISMATCH, SLOT_MISMATCH, UNSUPPORTED_VERSION, NO_HEADER, UNREADABLE }

data class RejectedEntry(val chapterNumber: Int, val rejection: Rejection)

/**
 * What [CheckpointStore.readAll] found. [records] holds only records that passed every check, in
 * chapter order. A bad record never hides the good ones; a bad *file* (wrong lineage or slot) yields no
 * records at all and says why in [fileState]. [memoryMayDiffer] is true after a failed write left the
 * in-memory view unreliable.
 */
data class CheckpointReadResult(
    val records: List<CheckpointRecord>,
    val rejected: List<RejectedEntry>,
    val fileState: FileState,
    val storedLineage: String?,
    val memoryMayDiffer: Boolean
)

/**
 * Per-slot checkpoint storage. Each slot has its **own** `SharedPreferences` file, obtained from
 * [prefsForSlot]; nothing here can read or write another slot's file, the ordinary save file, or the
 * entitlement file. Every write is one `commit()` and checks its result.
 *
 * Failed-write contract (the reason this class is careful): when `SharedPreferences.commit()` returns
 * false, Android has *already applied the edit to its in-memory map* even though the disk write failed.
 * The store therefore never assumes a failure left the old values readable. After a failed commit it
 * writes the previous contents back and only reports [StoreWriteResult.RolledBack] if that second
 * commit succeeds; otherwise it reports [StoreWriteResult.StateUnknown] and stops trusting the file.
 *
 * Process-wide guarantees (see [StoreRegistry]): the "untrusted" flag and the write lock belong to the
 * physical preferences file, so they hold across every `CheckpointStore` built over it in this process.
 * All operations on one slot, reads included, are serialised; different slots never block each other.
 *
 * Incremental writes ([upsert], [replaceFrom], [removeFrom], [pruneAbove]) only proceed on an empty file
 * (initialisation) or on a file whose header is intact, current-version, for this slot and for the
 * caller's lineage. Anything else is refused untouched; only the explicit whole-file operations
 * ([replaceAll], [clear]) can replace such a file, and choosing to do so is the caller's policy decision.
 *
 * ### Notes for later batches (not implemented here; this package is dormant and unwired)
 * * **Copying a slot (2C).** Copy the source's complete valid history: [readAll], then
 *   [CheckpointSets.selectCopyable], then [CheckpointSets.rebind] to a fresh [Lineage], stage with
 *   [CheckpointStaging], and only after the single live-save commit point promote via [replaceAll].
 *   The destination's previous save, Back snapshot and checkpoints are preserved in a recovery bundle
 *   until the replacement is complete. Source and destination are independent afterwards.
 * * **Recovery retention (2C/2D).** A pre-rewind (or pre-overwrite, or pre-restart) bundle is kept until
 *   the player explicitly accepts the new timeline or a later operation has safely replaced it. Closing
 *   the app, or entering new chapters, does not discard it. Bundles are double-buffered so a slot is
 *   never left without a recovery copy.
 * * **Achievements (2B/2D).** Trophies are separate from story flags and survive rewinds, Back, and undo:
 *   restoring takes `trophies = live union checkpoint`, and a swap-style undo keeps the union of both
 *   timelines. Replaying an achievement never duplicates it (sets).
 * * **Start Again.** Gets the same recovery protection as overwriting a slot, under a freshly minted
 *   lineage.
 * * **Arrival detection (2B).** A record is written only on a committed transition into a different
 *   chapter; never on load, Back, rewind or copy. See `CheckpointDesign.kt` for the arrival-ledger
 *   requirements that make a record trustworthy.
 */
class CheckpointStore(
    private val prefsForSlot: (Int) -> SharedPreferences,
    private val rules: CheckpointRules = CheckpointRules.forStory()
) {
    /**
     * False after a [StoreWriteResult.StateUnknown] on [slot], for every store over the same preferences
     * file in this process, until the process restarts and reloads the file from disk.
     */
    fun isTrusted(slot: Int): Boolean = !StoreRegistry.isUntrusted(prefsForSlot(slot))

    /** Reads the slot's file, returning only records valid for [expectedLineage]. Never throws. */
    fun readAll(slot: Int, expectedLineage: String): CheckpointReadResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            val memoryMayDiffer = StoreRegistry.isUntrusted(prefs)
            try {
                readAllUnsafe(slot, prefs, expectedLineage, memoryMayDiffer)
            } catch (e: RuntimeException) {
                CheckpointReadResult(emptyList(), emptyList(), FileState.UNREADABLE, null, memoryMayDiffer)
            }
        }
    }

    /** The lineage stored in the slot's header, or null when there is no readable header. */
    fun storedLineage(slot: Int): String? {
        requireSlot(slot)
        return locked(slot) { prefs ->
            try {
                prefs.getString(CheckpointCodec.HEADER_LINEAGE, null)
            } catch (e: RuntimeException) {
                null
            }
        }
    }

    /**
     * Replaces the slot's entire contents with [records] under [lineage] in one commit. Every record must
     * be valid for [lineage] and chapters must be distinct; otherwise nothing is touched. This is the
     * explicit whole-file operation: it replaces whatever is there, including a file the incremental
     * operations refuse, so the caller decides when that is appropriate.
     */
    fun replaceAll(slot: Int, lineage: String, records: List<CheckpointRecord>): StoreWriteResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            refuseIfUntrusted(slot, prefs)
                ?: validateForWrite(lineage, records)?.let { StoreWriteResult.Rejected(it) }
                ?: guardedCommit(slot, prefs) { editor, _ ->
                    editor.clear()
                    CheckpointCodec.writeHeader(editor, slot, lineage)
                    records.forEach { CheckpointCodec.write(editor, it) }
                }
        }
    }

    /**
     * Writes or replaces the single record for its chapter, leaving every other record alone. Refused
     * unless the file is empty or has an intact header for this slot and [lineage] (use [replaceAll] to
     * start a new timeline).
     */
    fun upsert(slot: Int, lineage: String, record: CheckpointRecord): StoreWriteResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            refuseIfUntrusted(slot, prefs)
                ?: validateForWrite(lineage, listOf(record))?.let { StoreWriteResult.Rejected(it) }
                ?: preflightIncremental(slot, prefs, lineage)
                ?: guardedCommit(slot, prefs) { editor, keys ->
                    CheckpointCodec.keysOfGroup(keys, record.chapterNumber).forEach { editor.remove(it) }
                    CheckpointCodec.writeHeader(editor, slot, lineage)
                    CheckpointCodec.write(editor, record)
                }
        }
    }

    /**
     * The chapter-arrival write: in **one** commit, removes every record for chapters `>=` the record's
     * chapter K (the abandoned future, including K itself and any malformed leftovers in those groups)
     * and writes [record] as K. Records below K are untouched. Because it is a single `commit()` a crash
     * leaves either the old file or the new one, never "the future removed but K missing" or the reverse.
     * Same preconditions as [upsert].
     */
    fun replaceFrom(slot: Int, lineage: String, record: CheckpointRecord): StoreWriteResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            refuseIfUntrusted(slot, prefs)
                ?: validateForWrite(lineage, listOf(record))?.let { StoreWriteResult.Rejected(it) }
                ?: preflightIncremental(slot, prefs, lineage)
                ?: guardedCommit(slot, prefs) { editor, keys ->
                    // Removes first, writes after: a put to the same key must win over the remove.
                    CheckpointCodec.chapterGroups(keys).filter { it >= record.chapterNumber }.forEach { chapter ->
                        CheckpointCodec.keysOfGroup(keys, chapter).forEach { editor.remove(it) }
                    }
                    CheckpointCodec.writeHeader(editor, slot, lineage)
                    CheckpointCodec.write(editor, record)
                }
        }
    }

    /** Removes every record for chapters `>= chapterNumber` (the "arrival replaces the abandoned future" rule). */
    fun removeFrom(slot: Int, lineage: String, chapterNumber: Int): StoreWriteResult =
        removeWhere(slot, lineage) { it >= chapterNumber }

    /** Removes every record for chapters `> chapterNumber` (the "nothing above the current chapter" invariant). */
    fun pruneAbove(slot: Int, lineage: String, chapterNumber: Int): StoreWriteResult =
        removeWhere(slot, lineage) { it > chapterNumber }

    /** Deletes the slot's whole checkpoint file contents. Explicit whole-file operation, like [replaceAll]. */
    fun clear(slot: Int): StoreWriteResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            refuseIfUntrusted(slot, prefs) ?: guardedCommit(slot, prefs) { editor, _ -> editor.clear() }
        }
    }

    /**
     * Reads the slot back and compares it with [expected]. This checks the codec round trip and that the
     * write landed completely; it reads this process's view, so it complements, and does not replace, the
     * commit result.
     */
    fun verify(slot: Int, lineage: String, expected: List<CheckpointRecord>): Boolean {
        val read = readAll(slot, lineage)
        if (read.memoryMayDiffer || read.fileState != FileState.OK) return false
        return read.rejected.isEmpty() && read.records == expected.sortedBy { it.chapterNumber }
    }

    // ---- internals -------------------------------------------------------------------------

    /** Runs [block] on the slot's preferences while holding the one process-wide lock for that file. */
    private inline fun <T> locked(slot: Int, block: (SharedPreferences) -> T): T {
        val prefs = prefsForSlot(slot)
        return synchronized(StoreRegistry.lockFor(prefs)) { block(prefs) }
    }

    private fun removeWhere(slot: Int, lineage: String, predicate: (Int) -> Boolean): StoreWriteResult {
        requireSlot(slot)
        return locked(slot) { prefs ->
            refuseIfUntrusted(slot, prefs)
                ?: preflightIncremental(slot, prefs, lineage)
                ?: run {
                    val doomed = try {
                        CheckpointCodec.chapterGroups(prefs.all.keys).filter(predicate)
                    } catch (e: RuntimeException) {
                        return@run StoreWriteResult.Rejected("could not read the current contents: ${e.javaClass.simpleName}")
                    }
                    if (doomed.isEmpty()) {
                        StoreWriteResult.Committed // already as requested; nothing to write
                    } else {
                        guardedCommit(slot, prefs) { editor, keys ->
                            doomed.forEach { chapter ->
                                CheckpointCodec.keysOfGroup(keys, chapter).forEach { editor.remove(it) }
                            }
                        }
                    }
                }
        }
    }

    /** What the header says about a file, without decoding any record. */
    private class Header(val state: FileState, val storedLineage: String?)

    /**
     * Classifies the file header. [FileState.OK] here means only "intact, supported version, this slot";
     * comparing the lineage is the caller's job. Throws on an unreadable file (callers handle it).
     */
    private fun inspectHeader(slot: Int, prefs: SharedPreferences, keys: Set<String>): Header {
        if (keys.isEmpty()) return Header(FileState.EMPTY, null)
        val storedLineage = try { prefs.getString(CheckpointCodec.HEADER_LINEAGE, null) } catch (e: ClassCastException) { null }
        val headerVersion = try { prefs.getInt(CheckpointCodec.HEADER_VERSION, -1) } catch (e: ClassCastException) { return Header(FileState.NO_HEADER, storedLineage) }
        val headerSlot = try { prefs.getInt(CheckpointCodec.HEADER_SLOT, -1) } catch (e: ClassCastException) { return Header(FileState.NO_HEADER, storedLineage) }
        if (storedLineage == null || !prefs.contains(CheckpointCodec.HEADER_VERSION) || !prefs.contains(CheckpointCodec.HEADER_SLOT)) {
            return Header(FileState.NO_HEADER, storedLineage)
        }
        if (headerVersion != CHECKPOINT_FORMAT_VERSION) return Header(FileState.UNSUPPORTED_VERSION, storedLineage)
        if (headerSlot != slot) return Header(FileState.SLOT_MISMATCH, storedLineage)
        return Header(FileState.OK, storedLineage)
    }

    /**
     * The gate every incremental write passes. Allowed only on an empty file (first write) or an intact
     * header for this slot whose lineage equals [lineage]. Everything else is refused before any edit is
     * built, so a newer-format file, a file copied from another slot, a headerless leftover or an
     * unreadable file is never modified or adopted.
     */
    private fun preflightIncremental(slot: Int, prefs: SharedPreferences, lineage: String): StoreWriteResult.Rejected? {
        val header = try {
            inspectHeader(slot, prefs, prefs.all.keys)
        } catch (e: RuntimeException) {
            return StoreWriteResult.Rejected("could not read the current contents: ${e.javaClass.simpleName}")
        }
        return when (header.state) {
            FileState.EMPTY -> null
            FileState.OK ->
                if (header.storedLineage == lineage) null
                else StoreWriteResult.Rejected("file lineage ${header.storedLineage} differs from $lineage")
            else -> StoreWriteResult.Rejected("checkpoint file is ${header.state}; incremental writes are refused")
        }
    }

    private fun readAllUnsafe(
        slot: Int,
        prefs: SharedPreferences,
        expectedLineage: String,
        memoryMayDiffer: Boolean
    ): CheckpointReadResult {
        val keys = prefs.all.keys
        val header = inspectHeader(slot, prefs, keys)
        val storedLineage = header.storedLineage
        fun empty(state: FileState) = CheckpointReadResult(emptyList(), emptyList(), state, storedLineage, memoryMayDiffer)
        if (header.state != FileState.OK) return empty(header.state)
        if (storedLineage != expectedLineage) return empty(FileState.LINEAGE_MISMATCH)

        val records = ArrayList<CheckpointRecord>()
        val rejected = ArrayList<RejectedEntry>()
        CheckpointCodec.chapterGroups(keys).forEach { chapter ->
            when (val decoded = CheckpointCodec.read(prefs, chapter, storedLineage)) {
                is CheckpointCodec.DecodeOutcome.Bad -> rejected.add(RejectedEntry(chapter, decoded.rejection))
                is CheckpointCodec.DecodeOutcome.Ok -> {
                    val problem = rules.validate(decoded.record, expectedLineage)
                    if (problem != null) rejected.add(RejectedEntry(chapter, problem)) else records.add(decoded.record)
                }
            }
        }
        return CheckpointReadResult(records.sortedBy { it.chapterNumber }, rejected, FileState.OK, storedLineage, memoryMayDiffer)
    }

    private fun validateForWrite(lineage: String, records: List<CheckpointRecord>): String? {
        if (!Lineage.isWellFormed(lineage)) return "malformed lineage"
        val seen = HashSet<Int>()
        records.forEach { record ->
            rules.validate(record, lineage)?.let { return "chapter ${record.chapterNumber}: ${it.reason} ${it.detail}".trim() }
            if (!seen.add(record.chapterNumber)) return "duplicate chapter ${record.chapterNumber}"
        }
        return null
    }

    private fun refuseIfUntrusted(slot: Int, prefs: SharedPreferences): StoreWriteResult.Rejected? =
        if (StoreRegistry.isUntrusted(prefs)) StoreWriteResult.Rejected("slot $slot is untrusted until restart") else null

    /**
     * Runs [build] on a fresh editor and commits it once. On any failure it restores the captured previous
     * contents with a second commit and reports honestly whether that worked. Callers hold the slot lock,
     * so nothing else can touch the file between the snapshot and the restore.
     */
    private fun guardedCommit(
        slot: Int,
        prefs: SharedPreferences,
        build: (SharedPreferences.Editor, Set<String>) -> Unit
    ): StoreWriteResult {
        val before = try {
            RawSnapshot.capture(prefs)
        } catch (e: RuntimeException) {
            return StoreWriteResult.Rejected("could not read the current contents: ${e.javaClass.simpleName}")
        }
        val committed = try {
            val editor = prefs.edit()
            build(editor, before.keys)
            editor.commit()
        } catch (e: RuntimeException) {
            false
        }
        if (committed) return StoreWriteResult.Committed

        val restored = try {
            val editor = prefs.edit()
            before.applyTo(editor)
            editor.commit()
        } catch (e: RuntimeException) {
            false
        }
        return if (restored) {
            StoreWriteResult.RolledBack("commit failed; previous contents re-committed")
        } else {
            StoreRegistry.markUntrusted(prefs)
            StoreWriteResult.StateUnknown("commit failed and the previous contents could not be re-committed")
        }
    }

    private fun requireSlot(slot: Int) {
        require(slot >= 1) { "slot must be 1 or higher" }
    }

    /** A typed copy of a preferences file's contents, so it can be written back exactly. */
    private class RawSnapshot(private val values: Map<String, Any?>) {
        val keys: Set<String> get() = values.keys

        fun applyTo(editor: SharedPreferences.Editor) {
            editor.clear()
            values.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    else -> Unit
                }
            }
        }

        companion object {
            fun capture(prefs: SharedPreferences): RawSnapshot {
                val copy = LinkedHashMap<String, Any?>()
                prefs.all.forEach { (key, value) ->
                    copy[key] = if (value is Set<*>) value.filterIsInstance<String>().toSet() else value
                }
                return RawSnapshot(copy)
            }
        }
    }
}
