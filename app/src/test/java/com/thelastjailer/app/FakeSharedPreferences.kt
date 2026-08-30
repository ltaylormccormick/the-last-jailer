package com.thelastjailer.app

import android.content.SharedPreferences

/**
 * A minimal in-memory [SharedPreferences] double for JVM unit tests. SharedPreferences is just an
 * interface, so implementing it directly needs no Android runtime, Robolectric, or instrumentation
 * — every method here is either trivial storage or a documented no-op (the listener methods).
 */
class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    // Real Android's SharedPreferencesImpl does an unchecked `(String) v` / `(Set<String>) v` cast
    // here and throws ClassCastException on a type mismatch rather than falling back to defValue —
    // mirrored below so tests can catch bugs like reading a key back in the wrong format.
    override fun getString(key: String?, defValue: String?): String? {
        val v = values[key] ?: return defValue
        return v as String
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val v = values[key] ?: return defValues
        return (v as Set<String>).toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int = (values[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = (values[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = (values[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var cleared = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pending[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed += key
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            cleared = true
            return this
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            if (cleared) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
