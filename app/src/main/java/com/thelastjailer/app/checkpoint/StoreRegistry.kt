package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences
import java.util.Collections
import java.util.WeakHashMap

/**
 * Process-wide state that belongs to a *physical preferences file*, not to whichever [CheckpointStore]
 * object happens to be holding it.
 *
 * Both facts below are about the in-memory map of one `SharedPreferences` instance. Android hands every
 * caller in a process the **same** instance for a given file name, so two `CheckpointStore` objects
 * built over the same file share one in-memory map. That is exactly why they must share these facts:
 *
 *  * **Untrusted.** A failed `commit()` that could not be rolled back leaves values in that shared map
 *    which never reached disk. Every store over the map must stop trusting it, not only the one that saw
 *    the failure.
 *  * **Lock.** Snapshot -> commit -> compensating re-commit is a read-modify-write on that shared map.
 *    Two writers interleaving can commit each other's half-applied values or restore over each other's
 *    work, so all access to one map is serialised by one lock.
 *
 * Keys are the `SharedPreferences` *instances* (compared by identity, held weakly). That is the right
 * unit because it is precisely what a process restart discards: a restarted process gets new instances,
 * so the untrusted flag disappears with the poisoned map and nothing has to be reset by hand. Different
 * slots use different files, so they never share a lock or a flag.
 *
 * This object holds no preferences data and does no I/O.
 */
internal object StoreRegistry {
    private val locks = WeakHashMap<SharedPreferences, Any>()
    private val untrusted: MutableSet<SharedPreferences> =
        Collections.newSetFromMap(WeakHashMap<SharedPreferences, Boolean>())

    /** The one lock that guards [prefs], created on first use and shared by every store over it. */
    fun lockFor(prefs: SharedPreferences): Any = synchronized(locks) {
        locks.getOrPut(prefs) { Any() }
    }

    fun markUntrusted(prefs: SharedPreferences) {
        synchronized(untrusted) { untrusted.add(prefs) }
    }

    fun isUntrusted(prefs: SharedPreferences): Boolean = synchronized(untrusted) { prefs in untrusted }
}
