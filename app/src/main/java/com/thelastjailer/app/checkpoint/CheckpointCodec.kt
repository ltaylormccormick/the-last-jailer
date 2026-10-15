package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences

/**
 * Reads and writes one record as a group of keys inside a per-slot checkpoint file.
 *
 * Layout (all keys in the slot's own file):
 *   h.version, h.slot, h.lineage        file header
 *   c<NN>.<field>                       one group per chapter, NN = two-digit chapter number
 *
 * The inventory is a single string joined by [INVENTORY_SEPARATOR] so that order and repeated items
 * (stacked draughts) survive. Trophies and flags are string sets.
 */
internal object CheckpointCodec {
    const val HEADER_VERSION = "h.version"
    const val HEADER_SLOT = "h.slot"
    const val HEADER_LINEAGE = "h.lineage"

    private val INT_FIELDS = listOf(
        "v", "n", "courage", "honour", "health", "maxHealth", "gold", "level", "xp", "xpToNextLevel"
    )
    private val STRING_FIELDS = listOf("chapter", "scene", "inventory", "crc")
    private val SET_FIELDS = listOf("trophies", "flags")
    private val ALL_FIELDS = INT_FIELDS + STRING_FIELDS + SET_FIELDS

    private val groupPattern = Regex("^c(\\d{2})\\.")

    fun key(chapterNumber: Int, field: String): String =
        String.format(java.util.Locale.ROOT, "c%02d.%s", chapterNumber, field)

    /** The chapter numbers that have any key in [keys], whatever else is or is not stored. */
    fun chapterGroups(keys: Collection<String>): Set<Int> =
        keys.mapNotNull { groupPattern.find(it)?.groupValues?.get(1)?.toIntOrNull() }.toSortedSet()

    /** Every existing key that belongs to chapter group [chapterNumber], including unknown extras. */
    fun keysOfGroup(keys: Collection<String>, chapterNumber: Int): List<String> {
        val prefix = String.format(java.util.Locale.ROOT, "c%02d.", chapterNumber)
        return keys.filter { it.startsWith(prefix) }
    }

    fun write(editor: SharedPreferences.Editor, record: CheckpointRecord) {
        val n = record.chapterNumber
        editor
            .putInt(key(n, "v"), record.version)
            .putInt(key(n, "n"), record.chapterNumber)
            .putString(key(n, "chapter"), record.chapterId)
            .putString(key(n, "scene"), record.sceneId)
            .putInt(key(n, "courage"), record.courage)
            .putInt(key(n, "honour"), record.honour)
            .putInt(key(n, "health"), record.health)
            .putInt(key(n, "maxHealth"), record.maxHealth)
            .putInt(key(n, "gold"), record.gold)
            .putInt(key(n, "level"), record.level)
            .putInt(key(n, "xp"), record.xp)
            .putInt(key(n, "xpToNextLevel"), record.xpToNextLevel)
            .putString(key(n, "inventory"), record.inventory.joinToString(INVENTORY_SEPARATOR.toString()))
            .putStringSet(key(n, "trophies"), HashSet(record.trophies))
            .putStringSet(key(n, "flags"), HashSet(record.flags))
            .putString(key(n, "crc"), String.format(java.util.Locale.ROOT, "%08x", record.crc))
    }

    fun writeHeader(editor: SharedPreferences.Editor, slot: Int, lineage: String) {
        editor
            .putInt(HEADER_VERSION, CHECKPOINT_FORMAT_VERSION)
            .putInt(HEADER_SLOT, slot)
            .putString(HEADER_LINEAGE, lineage)
    }

    /**
     * Decodes group [chapterNumber]. Any missing key, wrong type or unreadable value rejects *this
     * record only*. Validation against story content happens afterwards, in [CheckpointRules].
     */
    fun read(prefs: SharedPreferences, chapterNumber: Int, lineage: String): DecodeOutcome {
        val n = chapterNumber
        ALL_FIELDS.forEach { field ->
            if (!prefs.contains(key(n, field))) {
                return DecodeOutcome.Bad(Rejection(RejectReason.MISSING_FIELD, key(n, field)))
            }
        }
        return try {
            val storedN = prefs.getInt(key(n, "n"), -1)
            if (storedN != n) {
                return DecodeOutcome.Bad(Rejection(RejectReason.KEY_MISMATCH, "group $n holds chapter $storedN"))
            }
            val inventoryText = prefs.getString(key(n, "inventory"), null)
                ?: return DecodeOutcome.Bad(Rejection(RejectReason.MISSING_FIELD, key(n, "inventory")))
            val inventory = if (inventoryText.isEmpty()) emptyList() else inventoryText.split(INVENTORY_SEPARATOR)
            val crcText = prefs.getString(key(n, "crc"), null)
                ?: return DecodeOutcome.Bad(Rejection(RejectReason.MISSING_FIELD, key(n, "crc")))
            val crc = crcText.toLongOrNull(16)
                ?: return DecodeOutcome.Bad(Rejection(RejectReason.CRC_MISMATCH, "unparseable crc"))
            val record = CheckpointRecord(
                version = prefs.getInt(key(n, "v"), -1),
                chapterNumber = n,
                lineage = lineage,
                chapterId = prefs.getString(key(n, "chapter"), null) ?: "",
                sceneId = prefs.getString(key(n, "scene"), null) ?: "",
                courage = prefs.getInt(key(n, "courage"), 0),
                honour = prefs.getInt(key(n, "honour"), 0),
                health = prefs.getInt(key(n, "health"), 0),
                maxHealth = prefs.getInt(key(n, "maxHealth"), 0),
                gold = prefs.getInt(key(n, "gold"), 0),
                level = prefs.getInt(key(n, "level"), 0),
                xp = prefs.getInt(key(n, "xp"), 0),
                xpToNextLevel = prefs.getInt(key(n, "xpToNextLevel"), 0),
                inventory = inventory,
                trophies = prefs.getStringSet(key(n, "trophies"), null)?.toSet() ?: emptySet(),
                flags = prefs.getStringSet(key(n, "flags"), null)?.toSet() ?: emptySet(),
                crc = crc
            )
            DecodeOutcome.Ok(record)
        } catch (e: ClassCastException) {
            DecodeOutcome.Bad(Rejection(RejectReason.WRONG_TYPE, e.message ?: ""))
        } catch (e: RuntimeException) {
            DecodeOutcome.Bad(Rejection(RejectReason.WRONG_TYPE, e.javaClass.simpleName))
        }
    }

    sealed class DecodeOutcome {
        data class Ok(val record: CheckpointRecord) : DecodeOutcome()
        data class Bad(val rejection: Rejection) : DecodeOutcome()
    }
}
