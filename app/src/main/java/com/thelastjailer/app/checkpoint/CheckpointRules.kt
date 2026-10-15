package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.data.StoryRepository

/** Why a record was refused. Used in results and tests; never shown to players. */
enum class RejectReason {
    UNSUPPORTED_VERSION,
    MALFORMED_LINEAGE,
    LINEAGE_MISMATCH,
    CHAPTER_OUT_OF_RANGE,
    CHAPTER_ID_MISMATCH,
    SCENE_MISSING,
    SCENE_CHAPTER_MISMATCH,
    IMPOSSIBLE_VALUE,
    BAD_INVENTORY,
    TOO_LARGE,
    CRC_MISMATCH,
    MISSING_FIELD,
    WRONG_TYPE,
    KEY_MISMATCH,
    DUPLICATE_CHAPTER
}

data class Rejection(val reason: RejectReason, val detail: String = "")

/**
 * What makes a record acceptable. Pure: it reads nothing from storage and cannot touch a live save.
 *
 * [chapterOfScene] resolves a scene id to the chapter id its story node belongs to (null when the scene
 * no longer exists, for example after a content update). [maxChapter] is the number of chapters.
 */
class CheckpointRules(
    val maxChapter: Int,
    private val chapterOfScene: (String) -> String?
) {
    /** The first problem with [record], or null when it is acceptable for [expectedLineage]. */
    fun validate(record: CheckpointRecord, expectedLineage: String): Rejection? {
        if (record.version != CHECKPOINT_FORMAT_VERSION) {
            return Rejection(RejectReason.UNSUPPORTED_VERSION, "version ${record.version}")
        }
        if (!Lineage.isWellFormed(record.lineage)) return Rejection(RejectReason.MALFORMED_LINEAGE)
        if (record.lineage != expectedLineage) return Rejection(RejectReason.LINEAGE_MISMATCH)
        if (record.chapterNumber !in 1..maxChapter) {
            return Rejection(RejectReason.CHAPTER_OUT_OF_RANGE, "chapter ${record.chapterNumber}")
        }
        if (ChapterIds.numberOf(record.chapterId) != record.chapterNumber) {
            return Rejection(RejectReason.CHAPTER_ID_MISMATCH, record.chapterId)
        }
        val sceneChapter = chapterOfScene(record.sceneId)
            ?: return Rejection(RejectReason.SCENE_MISSING, record.sceneId)
        if (sceneChapter != record.chapterId) {
            return Rejection(RejectReason.SCENE_CHAPTER_MISMATCH, "${record.sceneId} is in $sceneChapter")
        }
        impossibleValue(record)?.let { return Rejection(RejectReason.IMPOSSIBLE_VALUE, it) }
        if (record.inventory.size > MAX_INVENTORY || record.flags.size > MAX_FLAGS || record.trophies.size > MAX_TROPHIES) {
            return Rejection(RejectReason.TOO_LARGE)
        }
        if (record.inventory.any { it.isBlank() || INVENTORY_SEPARATOR in it }) {
            return Rejection(RejectReason.BAD_INVENTORY)
        }
        if (!record.hasValidCrc()) return Rejection(RejectReason.CRC_MISMATCH)
        return null
    }

    private fun impossibleValue(r: CheckpointRecord): String? = when {
        r.maxHealth < 1 -> "maxHealth ${r.maxHealth}"
        r.health < 0 || r.health > r.maxHealth -> "health ${r.health}/${r.maxHealth}"
        r.level < 1 -> "level ${r.level}"
        r.xp < 0 -> "xp ${r.xp}"
        r.xpToNextLevel < 1 -> "xpToNextLevel ${r.xpToNextLevel}"
        r.gold < 0 -> "gold ${r.gold}"
        else -> null
    }

    companion object {
        // Generous sanity ceilings. The real story sets 248 distinct flags, 32 granted items and 30 trophies.
        const val MAX_INVENTORY = 1_000
        const val MAX_FLAGS = 2_000
        const val MAX_TROPHIES = 500

        /** Rules backed by the real story content. */
        fun forStory(): CheckpointRules =
            CheckpointRules(StoryRepository.chapters.size) { StoryRepository.nodeOrNull(it)?.chapterId }
    }
}
