package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import java.util.Collections
import java.util.LinkedHashSet
import java.util.zip.CRC32

/** On-disk format version of a single checkpoint record. Bump only with a migration. */
const val CHECKPOINT_FORMAT_VERSION = 1

/** Separates inventory ids inside the stored inventory string (ids themselves never contain it). */
internal const val INVENTORY_SEPARATOR = '\u001F'

/**
 * The state of one slot on arrival in a chapter, frozen. Immutable: every collection is copied on the
 * way in and wrapped read-only, so nothing a caller (or another slot) holds can change a record.
 *
 * The record carries its own [lineage] and a CRC32 of every other field. The CRC is an *integrity*
 * check: it catches truncation and accidental damage. It does not prove a record is genuine. Mixing
 * records from different timelines is prevented by [lineage], not by the CRC.
 *
 * Nothing in this class knows about purchases. A record never stores or implies entitlement.
 */
class CheckpointRecord internal constructor(
    val version: Int,
    val chapterNumber: Int,
    val lineage: String,
    val chapterId: String,
    val sceneId: String,
    val courage: Int,
    val honour: Int,
    val health: Int,
    val maxHealth: Int,
    val gold: Int,
    val level: Int,
    val xp: Int,
    val xpToNextLevel: Int,
    inventory: List<String>,
    trophies: Set<String>,
    flags: Set<String>,
    val crc: Long
) {
    val inventory: List<String> = Collections.unmodifiableList(ArrayList(inventory))
    val trophies: Set<String> = Collections.unmodifiableSet(LinkedHashSet(trophies))
    val flags: Set<String> = Collections.unmodifiableSet(LinkedHashSet(flags))

    /** The CRC this record's content should carry. */
    fun computeCrc(): Long = CheckpointCrc.compute(this)

    fun hasValidCrc(): Boolean = crc == computeCrc()

    /**
     * A copy bound to [newLineage], with its CRC recomputed. The receiver is untouched. This is how a
     * copied slot's history is rebound to the copy's own timeline (see [CheckpointSets.rebind]).
     */
    fun withLineage(newLineage: String): CheckpointRecord =
        sealed(
            version, chapterNumber, newLineage, chapterId, sceneId, courage, honour, health, maxHealth,
            gold, level, xp, xpToNextLevel, inventory, trophies, flags
        )

    /** The restorable game state, for [slot]. Collections are fresh copies. */
    fun toGameState(slot: Int): GameState = GameState(
        activeSlot = slot,
        chapterId = chapterId,
        sceneId = sceneId,
        courage = courage,
        honour = honour,
        health = health,
        maxHealth = maxHealth,
        gold = gold,
        level = level,
        xp = xp,
        xpToNextLevel = xpToNextLevel,
        inventory = inventory.toList(),
        trophies = trophies.toSet(),
        flags = flags.toSet()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckpointRecord) return false
        return version == other.version && chapterNumber == other.chapterNumber && lineage == other.lineage &&
            chapterId == other.chapterId && sceneId == other.sceneId && courage == other.courage &&
            honour == other.honour && health == other.health && maxHealth == other.maxHealth &&
            gold == other.gold && level == other.level && xp == other.xp &&
            xpToNextLevel == other.xpToNextLevel && inventory == other.inventory &&
            trophies == other.trophies && flags == other.flags && crc == other.crc
    }

    override fun hashCode(): Int {
        var h = chapterNumber
        h = 31 * h + lineage.hashCode()
        h = 31 * h + sceneId.hashCode()
        h = 31 * h + crc.hashCode()
        return h
    }

    override fun toString(): String =
        "CheckpointRecord(chapter=$chapterNumber, scene=$sceneId, lineage=$lineage, crc=${crc.toString(16)})"

    companion object {
        /**
         * Captures [state] as the arrival record for [chapterNumber]. The caller decides what counts as
         * an arrival (that is Batch 2B); this only freezes the values.
         */
        fun fromState(state: GameState, chapterNumber: Int, lineage: String): CheckpointRecord =
            sealed(
                CHECKPOINT_FORMAT_VERSION, chapterNumber, lineage, state.chapterId, state.sceneId,
                state.courage, state.honour, state.health, state.maxHealth, state.gold, state.level,
                state.xp, state.xpToNextLevel, state.inventory, state.trophies, state.flags
            )

        /** Builds a record with the CRC the content should have. */
        @Suppress("LongParameterList")
        internal fun sealed(
            version: Int,
            chapterNumber: Int,
            lineage: String,
            chapterId: String,
            sceneId: String,
            courage: Int,
            honour: Int,
            health: Int,
            maxHealth: Int,
            gold: Int,
            level: Int,
            xp: Int,
            xpToNextLevel: Int,
            inventory: List<String>,
            trophies: Set<String>,
            flags: Set<String>
        ): CheckpointRecord {
            val unsealed = CheckpointRecord(
                version, chapterNumber, lineage, chapterId, sceneId, courage, honour, health, maxHealth,
                gold, level, xp, xpToNextLevel, inventory, trophies, flags, crc = 0L
            )
            return CheckpointRecord(
                version, chapterNumber, lineage, chapterId, sceneId, courage, honour, health, maxHealth,
                gold, level, xp, xpToNextLevel, inventory, trophies, flags, crc = unsealed.computeCrc()
            )
        }
    }
}

/** CRC32 over a canonical, length-prefixed rendering of every field except the CRC itself. */
internal object CheckpointCrc {
    fun compute(r: CheckpointRecord): Long {
        val sb = StringBuilder()
        fun s(v: String) { sb.append(v.length).append(':').append(v).append(';') }
        fun i(v: Int) { sb.append(v).append(';') }
        i(r.version)
        i(r.chapterNumber)
        s(r.lineage)
        s(r.chapterId)
        s(r.sceneId)
        i(r.courage); i(r.honour); i(r.health); i(r.maxHealth)
        i(r.gold); i(r.level); i(r.xp); i(r.xpToNextLevel)
        i(r.inventory.size)
        r.inventory.forEach { s(it) } // order matters: it is the player's inventory order
        i(r.trophies.size)
        r.trophies.sorted().forEach { s(it) }
        i(r.flags.size)
        r.flags.sorted().forEach { s(it) }
        val crc = CRC32()
        crc.update(sb.toString().toByteArray(Charsets.UTF_8))
        return crc.value
    }
}

/** Helpers for the `chapter_N` ids used throughout the story data. */
object ChapterIds {
    private val pattern = Regex("^chapter_(\\d+)$")

    /** `chapter_12` becomes 12; anything that is not `chapter_<digits>` is null. */
    fun numberOf(chapterId: String): Int? =
        pattern.matchEntire(chapterId)?.groupValues?.get(1)?.toIntOrNull()
}
