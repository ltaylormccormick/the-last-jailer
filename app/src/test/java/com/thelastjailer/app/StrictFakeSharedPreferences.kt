package com.thelastjailer.app

import android.content.SharedPreferences

/**
 * A SharedPreferences test double that fails the way real Android does.
 *
 * Why this exists: [FakeSharedPreferences] throws ClassCastException for `getString` / `getStringSet` mismatches but
 * quietly returns the default for `getInt`, `getLong`, `getFloat` and `getBoolean`. Real `SharedPreferencesImpl` does
 * a bare cast in every one of them (`(Integer) mMap.get(key)`), so a wrong-typed `1.level` crashes the app on a
 * device and is invisible to tests that use the lenient double. That gap is why the G1 crash loop was never caught
 * by the unit suite.
 *
 * Mirrors AOSP SharedPreferencesImpl (read at android-8.0.0_r1, 11.0.0_r1, 14.0.0_r1):
 *  * every typed getter casts and throws [ClassCastException] on a type mismatch;
 *  * when the file could not be loaded (the `mThrowable` path, API 30+), EVERY read throws [IllegalStateException]
 *    wrapping the cause: `getAll`, the typed getters and `contains`;
 *  * `putString(key, null)` and `putStringSet(key, null)` remove the key.
 *
 * It also counts writes, so a test can prove a piece of code is read-only.
 */
class StrictFakeSharedPreferences : SharedPreferences {
    private val values = LinkedHashMap<String, Any?>()

    /** When set, every read throws IllegalStateException(cause), like a SharedPreferencesImpl whose load threw. */
    var unreadableCause: Throwable? = null

    /** Number of `edit()` calls. A read-only caller leaves this at zero. */
    var editCalls = 0
        private set

    /** Number of editor `apply()` / `commit()` calls. */
    var writeCalls = 0
        private set

    /** Seeds any value of any type, including ones Android's editor could never produce through the typed API. */
    fun putRaw(key: String, value: Any?): StrictFakeSharedPreferences {
        if (value == null) values.remove(key) else values[key] = value
        return this
    }

    /** A deterministic text of the whole content, for before/after "nothing changed" checks. */
    fun fingerprint(): String = values.entries.sortedBy { it.key }.joinToString(";") { (k, v) ->
        val rendered = if (v is Set<*>) v.map { it.toString() }.sorted().toString() else v.toString()
        "$k=${v?.javaClass?.simpleName}:$rendered"
    }

    fun keys(): Set<String> = values.keys.toSet()

    private fun awaitLoaded() {
        unreadableCause?.let { throw IllegalStateException(it) }
    }

    override fun getAll(): MutableMap<String, *> { awaitLoaded(); return LinkedHashMap(values) }

    override fun getString(key: String?, defValue: String?): String? {
        awaitLoaded(); val v = values[key] ?: return defValue; return v as String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        awaitLoaded(); val v = values[key] ?: return defValues; return (v as Set<String>).toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int { awaitLoaded(); val v = values[key] ?: return defValue; return v as Int }
    override fun getLong(key: String?, defValue: Long): Long { awaitLoaded(); val v = values[key] ?: return defValue; return v as Long }
    override fun getFloat(key: String?, defValue: Float): Float { awaitLoaded(); val v = values[key] ?: return defValue; return v as Float }
    override fun getBoolean(key: String?, defValue: Boolean): Boolean { awaitLoaded(); val v = values[key] ?: return defValue; return v as Boolean }
    override fun contains(key: String?): Boolean { awaitLoaded(); return values.containsKey(key) }

    override fun edit(): SharedPreferences.Editor { editCalls++; return StrictEditor() }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class StrictEditor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private val removed = LinkedHashSet<String>()
        private var cleared = false

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            if (key != null) { if (value == null) removed += key else { pending[key] = value; removed -= key } }
            return this
        }

        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)
        override fun remove(key: String?): SharedPreferences.Editor { if (key != null) { removed += key; pending.remove(key) }; return this }
        override fun clear(): SharedPreferences.Editor { cleared = true; return this }
        override fun commit(): Boolean { applyChanges(); return true }
        override fun apply() { applyChanges() }

        private fun applyChanges() {
            writeCalls++
            if (cleared) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
