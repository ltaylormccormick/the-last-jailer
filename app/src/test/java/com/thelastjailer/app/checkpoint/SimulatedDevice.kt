package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences

/** Marks a removal inside an editor's pending operations. */
private object Remove

/** Thrown to stand in for the process dying. An [Error] so ordinary `catch (e: Exception)` cannot swallow it. */
class SimulatedCrash(message: String) : Error(message)

/** What a commit does when a fault is armed for it. */
enum class CommitFault {
    /**
     * Real Android behaviour on a failed disk write: the edit is applied to the in-memory map, the disk is
     * left as it was, and `commit()` returns false.
     */
    FALSE_MEMORY_MUTATED,

    /** `commit()` returns false and neither memory nor disk changes (a kinder failure than Android's). */
    FALSE_UNCHANGED,

    /** The process dies before anything is written. */
    CRASH_BEFORE_WRITE,

    /** The process dies just after the disk write completed. */
    CRASH_AFTER_WRITE
}

/**
 * A pretend phone with a persistent "disk" and per-process "memory". It exists to test failed and
 * interrupted writes honestly:
 *
 *  * [prefs] returns the in-memory view for a file. Reads go to memory, as in real Android.
 *  * A commit writes memory *and* disk, unless a fault says otherwise.
 *  * [restart] throws every in-memory view away; the next [prefs] call reloads from disk only.
 *  * Typed getters throw [ClassCastException] on a type mismatch, like `SharedPreferencesImpl`.
 *
 * Tests assert the in-memory view and the post-restart (persisted) view separately.
 */
class SimulatedDevice {
    private val disks = LinkedHashMap<String, LinkedHashMap<String, Any?>>()
    private val live = LinkedHashMap<String, DevicePrefs>()
    private val commitCounts = HashMap<String, Int>()
    private val armed = ArrayList<ArmedFault>()

    /** One lock for the whole pretend phone, so tests can drive it from several threads. */
    private val lock = Any()

    /**
     * Real `apply()` updates the in-memory map at once but reaches disk later, on a background queue, and
     * can be lost if the process dies first. When this is true, `apply()` behaves like that: memory now,
     * disk only on [flushApplies]; [restart] throws unflushed applies away. A `commit()` on the same file
     * writes the whole map, so it makes earlier applies to that file durable too, exactly as on Android.
     * Default false keeps the older tests' "apply is immediately durable" behaviour.
     */
    @Volatile
    var asyncApply: Boolean = false

    private val queuedApplies = ArrayList<Pair<String, LinkedHashMap<String, Any?>>>()

    /** Writes every queued `apply()` to disk, in order (the background queue finishing). */
    fun flushApplies() = synchronized(lock) {
        queuedApplies.forEach { (name, snapshot) -> disks[name] = LinkedHashMap(copyOf(snapshot)) }
        queuedApplies.clear()
    }

    /** Throws the queued `apply()` writes away without a restart (they never reach disk). */
    fun dropQueuedApplies() = synchronized(lock) { queuedApplies.clear() }

    fun queuedApplyCount(): Int = synchronized(lock) { queuedApplies.size }

    /**
     * Identifies this phone's in-memory world, like the application `Context` does in production. A
     * [restart] gives the next "process" a new scope, which is what makes it start with nothing flagged.
     */
    @Volatile
    var memoryScope: Any = Any()
        private set

    private class ArmedFault(val file: String?, var remaining: Int, val fault: CommitFault, val persistent: Boolean)

    /** The (cached) in-memory view of [name]. */
    fun prefs(name: String): SharedPreferences = synchronized(lock) { live.getOrPut(name) { DevicePrefs(name) } }

    /** Simulates the process dying and starting again: memory is discarded, disk stays. */
    fun restart() = synchronized(lock) {
        live.clear()
        armed.clear()
        queuedApplies.clear()
        memoryScope = Any()
    }

    /** A copy of what is on disk for [name] (empty if the file does not exist). */
    fun disk(name: String): Map<String, Any?> = synchronized(lock) { copyOf(disks[name] ?: emptyMap()) }

    /** A copy of what the live process currently sees for [name]. */
    fun memory(name: String): Map<String, Any?> = synchronized(lock) { copyOf((prefs(name) as DevicePrefs).memory) }

    fun fileNames(): Set<String> = synchronized(lock) { disks.keys.toSet() }

    fun allDisks(): Map<String, Map<String, Any?>> = synchronized(lock) { disks.mapValues { copyOf(it.value) } }

    /** Puts a value straight onto disk (to build fixtures or inject corruption), then forgets memory. */
    fun writeDisk(name: String, values: Map<String, Any?>) {
        disks[name] = LinkedHashMap(copyOf(values))
        live.remove(name)
    }

    fun removeDiskKey(name: String, key: String) {
        disks[name]?.remove(key)
        live.remove(name)
    }

    fun putDiskValue(name: String, key: String, value: Any?) {
        disks.getOrPut(name) { LinkedHashMap() }[key] = if (value is Set<*>) value.toSet() else value
        live.remove(name)
    }

    fun wipeDisk(name: String) {
        disks.remove(name)
        live.remove(name)
    }

    /** Arms [fault] for the [nth] commit (1 = the very next) to [file], or to any file when null. */
    fun armFault(file: String?, nth: Int, fault: CommitFault) {
        armed.add(ArmedFault(file, nth, fault, persistent = false))
    }

    /** Every commit to [file] (or any file when null) fails with [fault] from now on. */
    fun armFaultAlways(file: String?, fault: CommitFault) {
        armed.add(ArmedFault(file, 1, fault, persistent = true))
    }

    fun clearFaults() = armed.clear()

    fun commitCount(file: String): Int = synchronized(lock) { commitCounts[file] ?: 0 }

    private fun faultFor(file: String): CommitFault? {
        // Every armed countdown that matches this file advances on every commit to it.
        val firing = ArrayList<ArmedFault>()
        armed.filter { it.file == null || it.file == file }.forEach { a ->
            if (!a.persistent) {
                a.remaining -= 1
                if (a.remaining == 0) firing.add(a)
            }
        }
        armed.removeAll(firing.toSet())
        firing.firstOrNull()?.let { return it.fault }
        return armed.firstOrNull { it.persistent && (it.file == null || it.file == file) }?.fault
    }

    private fun copyOf(map: Map<String, Any?>): LinkedHashMap<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        map.forEach { (k, v) -> out[k] = if (v is Set<*>) LinkedHashSet(v) else v }
        return out
    }

    private inner class DevicePrefs(private val name: String) : SharedPreferences {
        val memory: LinkedHashMap<String, Any?> = copyOf(disks[name] ?: emptyMap())

        override fun getAll(): MutableMap<String, *> = synchronized(lock) { copyOf(memory) }

        override fun getString(key: String?, defValue: String?): String? {
            synchronized(lock) {
                val v = memory[key] ?: return defValue
                return v as String
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            synchronized(lock) {
                val v = memory[key] ?: return defValues
                return LinkedHashSet(v as Set<String>)
            }
        }

        override fun getInt(key: String?, defValue: Int): Int {
            synchronized(lock) {
                val v = memory[key] ?: return defValue
                return v as Int
            }
        }

        override fun getLong(key: String?, defValue: Long): Long {
            synchronized(lock) {
                val v = memory[key] ?: return defValue
                return v as Long
            }
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            synchronized(lock) {
                val v = memory[key] ?: return defValue
                return v as Float
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            synchronized(lock) {
                val v = memory[key] ?: return defValue
                return v as Boolean
            }
        }

        override fun contains(key: String?): Boolean = synchronized(lock) { memory.containsKey(key) }

        override fun edit(): SharedPreferences.Editor = DeviceEditor()

        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private inner class DeviceEditor : SharedPreferences.Editor {
            private val ops = ArrayList<Pair<String, Any?>>() // key -> value; REMOVE marker for removals
            private var cleared = false

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) ops.add(key to (value ?: Remove))
                return this
            }

            override fun putString(key: String?, value: String?) = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.let { LinkedHashSet(it) })
            override fun putInt(key: String?, value: Int) = put(key, value)
            override fun putLong(key: String?, value: Long) = put(key, value)
            override fun putFloat(key: String?, value: Float) = put(key, value)
            override fun putBoolean(key: String?, value: Boolean) = put(key, value)
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) ops.add(key to Remove)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                cleared = true
                return this
            }

            private fun applyTo(target: LinkedHashMap<String, Any?>) {
                if (cleared) target.clear()
                ops.forEach { (k, v) -> if (v === Remove) target.remove(k) else target[k] = v }
            }

            private fun write(): Boolean = synchronized(lock) {
                commitCounts[name] = (commitCounts[name] ?: 0) + 1
                when (faultFor(name)) {
                    CommitFault.CRASH_BEFORE_WRITE -> throw SimulatedCrash("crash before write of $name")
                    CommitFault.FALSE_UNCHANGED -> false
                    CommitFault.FALSE_MEMORY_MUTATED -> {
                        applyTo(memory)
                        false
                    }
                    CommitFault.CRASH_AFTER_WRITE -> {
                        applyTo(memory)
                        disks[name] = copyOf(memory)
                        throw SimulatedCrash("crash after write of $name")
                    }
                    null -> {
                        applyTo(memory)
                        disks[name] = copyOf(memory)
                        queuedApplies.removeAll { it.first == name } // this write already contains them
                        true
                    }
                }
            }

            override fun commit(): Boolean = write()

            override fun apply() {
                if (asyncApply) {
                    synchronized(lock) {
                        applyTo(memory)
                        queuedApplies.add(name to copyOf(memory))
                    }
                } else {
                    write()
                }
            }
        }
    }
}
