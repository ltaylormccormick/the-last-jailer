package com.thelastjailer.app.checkpoint

/** How staging a set of records ended. */
sealed class StageResult {
    /** Written, committed and read back identical. Safe to promote later. */
    object Staged : StageResult() {
        override fun toString() = "Staged"
    }

    /** Not staged. [write] says what happened to the staging file; the area has been discarded where possible. */
    data class Failed(val reason: String, val write: StoreWriteResult?) : StageResult()
}

/**
 * A scratch area for building a checkpoint history *before* anything is replaced. It wraps a
 * [CheckpointStore] over **separate staging files** (supply a different file name per slot), so staging
 * can never overwrite a slot's real checkpoints. Promotion to the real file is a later batch's job and is
 * simply `realStore.replaceAll(slot, lineage, staging.readVerified(slot, lineage))`.
 *
 * The staging area is exclusively owned by the copy operation: anything already in it is stale and is
 * overwritten.
 */
class CheckpointStaging(private val store: CheckpointStore) {

    /** Writes [records] under [lineage], commits, and reads them back. Cleans up if anything fails. */
    fun stage(slot: Int, lineage: String, records: List<CheckpointRecord>): StageResult {
        val written = store.replaceAll(slot, lineage, records)
        if (!written.isCommitted) {
            if (written !is StoreWriteResult.StateUnknown) store.clear(slot)
            return StageResult.Failed("staging write did not commit", written)
        }
        if (!store.verify(slot, lineage, records)) {
            store.clear(slot)
            return StageResult.Failed("staged records did not read back identical", written)
        }
        return StageResult.Staged
    }

    /**
     * The staged records, only if the staging file is intact: right lineage, header present, every
     * record valid, no rejects. Null otherwise. An empty list is a valid staged history.
     */
    fun readVerified(slot: Int, lineage: String): List<CheckpointRecord>? {
        val read = store.readAll(slot, lineage)
        if (read.memoryMayDiffer || read.fileState != FileState.OK || read.rejected.isNotEmpty()) return null
        return read.records
    }

    /** Empties the staging file for [slot]. */
    fun discard(slot: Int): StoreWriteResult = store.clear(slot)
}
