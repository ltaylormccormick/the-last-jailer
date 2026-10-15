package com.thelastjailer.app.checkpoint

import java.util.WeakHashMap

/** What every store over one physical checkpoint file shares. */
internal class SharedFile {
    /** Serialises every operation on the file, reads included. */
    val lock = Any()

    /** True after a failed write that could not be rolled back: the in-memory map cannot be trusted. */
    @Volatile
    var untrusted = false
}

/**
 * Process-wide state that belongs to a *physical preferences file*, not to whichever [CheckpointStore]
 * (or wrapper) is holding it.
 *
 *  * **Untrusted.** A failed `commit()` that could not be rolled back leaves values in the file's shared
 *    in-memory map that never reached disk. Every store over that file must stop trusting it.
 *  * **Lock.** Snapshot -> commit -> compensating re-commit is a read-modify-write on that map. Two
 *    writers interleaving can commit each other's half-applied values or restore over each other's
 *    work, so all access to one file is serialised by one lock.
 *
 * Keyed by ([CheckpointStorage.memoryScope], file name) - not by the `SharedPreferences` object - so a
 * wrapper or a new object per call cannot dodge either protection. A new process (new scope) starts
 * with everything trusted, which is exactly right: its maps were just reloaded from disk. The scope is
 * held weakly. Holds no preferences data and does no I/O.
 */
internal object StoreRegistry {
    private val scopes = WeakHashMap<Any, HashMap<String, SharedFile>>()

    fun forFile(scope: Any, fileName: String): SharedFile = synchronized(scopes) {
        scopes.getOrPut(scope) { HashMap() }.getOrPut(fileName) { SharedFile() }
    }
}
