package com.thelastjailer.app.checkpoint

/** A record that was left out of a copy, and why. */
data class SkippedRecord(val chapterNumber: Int, val reason: RejectReason)

/** The outcome of choosing which of a source slot's records may be copied. */
data class CopySelection(val kept: List<CheckpointRecord>, val skipped: List<SkippedRecord>)

/**
 * Pure operations on groups of records. Nothing here touches storage, so nothing here can change a
 * live save or another slot.
 */
object CheckpointSets {

    /**
     * Rebinds [records] to [newLineage]: new immutable records with the new lineage and a recomputed
     * CRC. The inputs are not modified and the outputs share no mutable state with them.
     */
    fun rebind(records: List<CheckpointRecord>, newLineage: String): List<CheckpointRecord> {
        require(Lineage.isWellFormed(newLineage)) { "malformed lineage" }
        return records.sortedBy { it.chapterNumber }.map { it.withLineage(newLineage) }
    }

    /**
     * Chooses the records of a source slot that belong in a copy made at [liveChapterNumber]: one per
     * chapter, none above the chapter the player is actually in (those would belong to an abandoned
     * timeline), and none with a bad CRC. [records] should already come from
     * [CheckpointStore.readAll], which has done the lineage and content checks.
     */
    fun selectCopyable(records: List<CheckpointRecord>, liveChapterNumber: Int): CopySelection {
        val kept = ArrayList<CheckpointRecord>()
        val skipped = ArrayList<SkippedRecord>()
        val seen = HashSet<Int>()
        records.sortedBy { it.chapterNumber }.forEach { record ->
            when {
                record.chapterNumber > liveChapterNumber ->
                    skipped.add(SkippedRecord(record.chapterNumber, RejectReason.CHAPTER_OUT_OF_RANGE))
                !record.hasValidCrc() ->
                    skipped.add(SkippedRecord(record.chapterNumber, RejectReason.CRC_MISMATCH))
                !seen.add(record.chapterNumber) ->
                    skipped.add(SkippedRecord(record.chapterNumber, RejectReason.DUPLICATE_CHAPTER))
                else -> kept.add(record)
            }
        }
        return CopySelection(kept, skipped)
    }

    /** [records] with anything above [liveChapterNumber] removed, ordered by chapter, one per chapter. */
    fun withoutFuture(records: List<CheckpointRecord>, liveChapterNumber: Int): List<CheckpointRecord> =
        selectCopyable(records, liveChapterNumber).kept
}
