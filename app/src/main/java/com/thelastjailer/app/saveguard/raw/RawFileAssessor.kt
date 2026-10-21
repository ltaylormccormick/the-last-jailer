package com.thelastjailer.app.saveguard.raw

// G2L Stage B1 (dormant): decides whether an existing save file may be treated as a fresh start, and cross-checks
// the raw bytes against the preferences map Android actually loaded.
//
// The rule: a fresh start is allowed ONLY when there is positively no file, or the file is positively a legitimate
// empty preferences file. Everything else that exists and cannot be shown to be a whole, understood, correctly loaded
// preferences file is SUSPECT, which means: keep a verified copy of the raw bytes and keep writes frozen.
//
// Pure: no file access, no logging. Reason codes and counts only, never a key or a value.

/** What the caller found when it looked at one of the two files before preferences were loaded. */
sealed class RawSource {
    /** The file does not exist. */
    object Absent : RawSource()

    /** It exists but cannot be read as a plain regular file (permissions, I/O error, symbolic link, directory). */
    object Unreadable : RawSource()

    /** It exists and is longer than [RawPrefsShape.MAX_BYTES]; its bytes were not read. */
    object TooLarge : RawSource()

    class Bytes(val bytes: ByteArray) : RawSource()
}

/** What Android's SharedPreferences returned from getAll() after loading. */
sealed class LoadedPrefs {
    class Entries(val map: Map<String, *>) : LoadedPrefs()

    /** getAll() threw (for example the load failed and the platform reports it). */
    object Unreadable : LoadedPrefs()
}

enum class RawClass { ABSENT, LEGIT_EMPTY, VALID, SUSPECT }

class RawAssessment(val cls: RawClass, val reason: SuspectReason?, val bakPresent: Boolean) {
    /** The only way an existing-or-missing file may lead to a new game. */
    val freshStartEligible: Boolean get() = (cls == RawClass.ABSENT || cls == RawClass.LEGIT_EMPTY) && !bakPresent

    /** Bytes must be kept: anything suspect, and any interrupted write (a `.bak` is present). */
    val snapshotRequired: Boolean get() = cls == RawClass.SUSPECT || bakPresent

    /** For TOO_LARGE and UNREADABLE sources there are no bytes to keep: the gate can only stay frozen. */
    val canSnapshot: Boolean get() = snapshotRequired && reason != SuspectReason.TOO_LARGE && reason != SuspectReason.UNREADABLE

    override fun toString() = "RawAssessment($cls, $reason, bakPresent=$bakPresent)"
}

object RawFileAssessor {

    /**
     * [main] is `jailer_saves.xml`, [bak] is `jailer_saves.xml.bak`, both as seen BEFORE preferences were loaded.
     * Android, if a `.bak` exists at load, discards the main file and loads the `.bak`, so that is the file whose
     * contents the loaded map must match.
     */
    fun assess(main: RawSource, bak: RawSource, loaded: LoadedPrefs): RawAssessment {
        val bakPresent = bak !is RawSource.Absent
        val effective = if (bakPresent) bak else main
        fun suspect(r: SuspectReason) = RawAssessment(RawClass.SUSPECT, r, bakPresent)

        val loadedMap = when (loaded) {
            is LoadedPrefs.Unreadable -> return suspect(SuspectReason.UNREADABLE_PREFS)
            is LoadedPrefs.Entries -> loaded.map
        }
        return when (effective) {
            is RawSource.Absent ->
                if (loadedMap.isEmpty()) RawAssessment(RawClass.ABSENT, null, false) else suspect(SuspectReason.MISMATCH)
            is RawSource.Unreadable -> suspect(SuspectReason.UNREADABLE)
            is RawSource.TooLarge -> suspect(SuspectReason.TOO_LARGE)
            is RawSource.Bytes -> assessBytes(effective.bytes, bakPresent, loadedMap)
        }
    }

    private fun assessBytes(bytes: ByteArray, bakPresent: Boolean, loadedMap: Map<String, *>): RawAssessment {
        if (RawPrefsShape.isCanonicalEmptyMap(bytes)) {
            // An empty `.bak` next to a (now discarded) main file may hide a newer main file: never a fresh start.
            if (bakPresent) return RawAssessment(RawClass.SUSPECT, SuspectReason.STALE_BAK, true)
            return if (loadedMap.isEmpty()) RawAssessment(RawClass.LEGIT_EMPTY, null, false)
            else RawAssessment(RawClass.SUSPECT, SuspectReason.MISMATCH, false)
        }
        return when (val shape = RawPrefsShape.validate(bytes)) {
            is RawShapeResult.Suspect -> RawAssessment(RawClass.SUSPECT, shape.reason, bakPresent)
            is RawShapeResult.Valid ->
                if (shape.entries.isEmpty()) {
                    // Whole, well-formed, but empty in a non-canonical spelling: not proven legitimate, so not fresh.
                    RawAssessment(RawClass.SUSPECT, SuspectReason.BAD_STRUCTURE, bakPresent)
                } else if (RawCrossCheck.compare(shape.entries, loadedMap) is RawCrossCheck.Result.Match) {
                    RawAssessment(RawClass.VALID, null, bakPresent)
                } else {
                    RawAssessment(RawClass.SUSPECT, SuspectReason.MISMATCH, bakPresent)
                }
        }
    }
}

/** Raw bytes versus the loaded map: same keys, same runtime types, same values. Reports a code, never a key or value. */
object RawCrossCheck {
    sealed class Result {
        object Match : Result()
        class Mismatch(val code: Code) : Result()
    }

    enum class Code { KEY_COUNT, MISSING_KEY, TYPE, VALUE }

    fun compare(expected: Map<String, Any>, loaded: Map<String, *>): Result {
        if (expected.size != loaded.size) return Result.Mismatch(Code.KEY_COUNT)
        for ((key, want) in expected) {
            if (!loaded.containsKey(key)) return Result.Mismatch(Code.MISSING_KEY)
            val got = loaded[key] ?: return Result.Mismatch(Code.TYPE)
            if (kindOf(want) != kindOf(got)) return Result.Mismatch(Code.TYPE)
            if (!sameValue(want, got)) return Result.Mismatch(Code.VALUE)
        }
        return Result.Match
    }

    private fun kindOf(v: Any): String = when (v) {
        is Int -> "int"
        is Long -> "long"
        is Float -> "float"
        is Boolean -> "boolean"
        is String -> "string"
        is Set<*> -> if (v.all { it is String }) "set" else "other"
        else -> "other"
    }

    private fun sameValue(a: Any, b: Any): Boolean = when (a) {
        is Float -> b is Float && a.toRawBits() == b.toRawBits()
        is Set<*> -> b is Set<*> && a.size == b.size && a.all { it in b }
        else -> a == b
    }
}
