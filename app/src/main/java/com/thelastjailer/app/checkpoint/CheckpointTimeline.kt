package com.thelastjailer.app.checkpoint

import java.util.TreeMap

/** The two additive live-save keys that describe a slot's checkpoint timeline. */
object TimelineKeys {
    fun lineage(slot: Int): String = "$slot.lineage"
    fun arrivals(slot: Int): String = "$slot.arrivals"

    /** The slot stamp (see [SlotStamp]): proof that the state, lineage and ledger were written together. */
    fun stamp(slot: Int): String = "$slot.stamp"
}

/**
 * The arrival ledger: for each chapter reached on this timeline, the [CheckpointDigest] of the record that
 * was frozen on arrival. Immutable. It is stored in the live save, in the same editor batch as the state it
 * describes, so the two can never disagree on disk.
 */
class Ledger private constructor(private val entries: TreeMap<Int, String>) {
    val isEmpty: Boolean get() = entries.isEmpty()

    fun digestFor(chapter: Int): String? = entries[chapter]

    /** The highest chapter entry, or null when empty. */
    fun latest(): Pair<Int, String>? = entries.lastEntry()?.let { it.key to it.value }

    /** This ledger without every entry for chapter [fromChapter] and above. */
    fun without(fromChapter: Int): Ledger {
        if (entries.isEmpty() || entries.lastKey() < fromChapter) return this
        val copy = TreeMap(entries)
        copy.tailMap(fromChapter, true).clear()
        return Ledger(copy)
    }

    fun with(chapter: Int, digest: String): Ledger {
        require(chapter >= 1 && DIGEST.matches(digest)) { "bad ledger entry" }
        val copy = TreeMap(entries)
        copy[chapter] = digest
        return Ledger(copy)
    }

    /** `K:digest,K:digest,…` in ascending chapter order; empty string when empty. */
    fun encode(): String = entries.entries.joinToString(",") { "${it.key}:${it.value}" }

    override fun equals(other: Any?): Boolean = other is Ledger && entries == other.entries
    override fun hashCode(): Int = entries.hashCode()
    override fun toString(): String = "Ledger(${encode()})"

    companion object {
        val EMPTY = Ledger(TreeMap())
        private val ENTRY = Regex("^([1-9][0-9]?):([0-9a-f]{${CheckpointDigest.HEX_LENGTH}})$")
        private val DIGEST = Regex("^[0-9a-f]{${CheckpointDigest.HEX_LENGTH}}$")

        /**
         * Strict parse. Null or blank is an empty ledger. Any malformed entry, duplicate, out-of-range or
         * non-ascending chapter makes the **whole** value malformed, reported as null: a damaged ledger is
         * never partly trusted.
         */
        fun parse(raw: String?, maxChapter: Int): Ledger? {
            if (raw.isNullOrEmpty()) return EMPTY
            val map = TreeMap<Int, String>()
            var previous = 0
            for (part in raw.split(',')) {
                val m = ENTRY.matchEntire(part) ?: return null
                val chapter = m.groupValues[1].toInt()
                if (chapter !in 1..maxChapter || chapter <= previous) return null
                map[chapter] = m.groupValues[2]
                previous = chapter
            }
            return Ledger(map)
        }
    }
}

/** A slot's recording timeline as read from the live save: well-formed lineage plus a well-formed ledger. */
class Timeline(val lineage: String, val ledger: Ledger) {
    override fun equals(other: Any?): Boolean = other is Timeline && lineage == other.lineage && ledger == other.ledger
    override fun hashCode(): Int = 31 * lineage.hashCode() + ledger.hashCode()
    override fun toString(): String = "Timeline($lineage, $ledger)"
}

/**
 * What a live-save write does to the slot's timeline keys, always inside the same editor batch as the state.
 * [Invalidate] is the default for every `save` so a caller that forgets to say otherwise can only make
 * history disappear, never leave history that no longer describes the state.
 */
sealed interface TimelineWrite {
    /** Same timeline: leave both keys exactly as they are. */
    object Keep : TimelineWrite

    /** A different (or unknown) timeline: remove both keys. */
    object Invalidate : TimelineWrite

    /** Write this lineage and ledger (an empty ledger removes the arrivals key). */
    class Set(val lineage: String, val ledger: Ledger) : TimelineWrite
}
