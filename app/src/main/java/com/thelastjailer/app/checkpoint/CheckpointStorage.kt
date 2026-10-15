package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences

/** The names of the per-slot checkpoint files, in one place so callers cannot mistype them. */
object CheckpointFiles {
    /** The live checkpoint file for [slot]. Never the same file as the ordinary save or entitlements. */
    fun live(slot: Int): String = "jailer_ckpt_$slot"

    /** The scratch file used while staging a slot copy (see [CheckpointStaging]). */
    fun staging(slot: Int): String = "jailer_ckpt_stage_$slot"
}

/**
 * Where a [CheckpointStore] keeps its files, and the identity that decides which stores share locks and
 * the "untrusted" flag.
 *
 * Sharing is decided by **(memoryScope, fileName)**, never by the identity of the `SharedPreferences`
 * object. Wrappers, decorators or a fresh object per call over the same file therefore still share one
 * lock and one flag, so they cannot be used to step around the protections.
 *
 *  * [memoryScope] names the in-memory world the preferences maps live in. In production it is the
 *    application `Context` (one per process), so it never changes while the process lives and a new
 *    process starts with a new scope. It must compare by identity (a `Context` does).
 *  * [fileName] must be unique per physical file: two slots must never map to one name.
 */
interface CheckpointStorage {
    val memoryScope: Any
    fun fileName(slot: Int): String
    fun prefs(slot: Int): SharedPreferences
}

/**
 * The ordinary implementation: [open] returns the process-cached preferences for a file name (in the
 * app, `context.getSharedPreferences(name, MODE_PRIVATE)`), and [memoryScope] is the application context.
 * [nameForSlot] defaults to the live checkpoint files; staging uses [CheckpointFiles.staging].
 */
class PrefsCheckpointStorage(
    override val memoryScope: Any,
    private val nameForSlot: (Int) -> String = CheckpointFiles::live,
    private val open: (String) -> SharedPreferences
) : CheckpointStorage {
    override fun fileName(slot: Int): String = nameForSlot(slot)
    override fun prefs(slot: Int): SharedPreferences = open(nameForSlot(slot))
}
