package com.thelastjailer.app.saveguard

import android.content.SharedPreferences
import com.thelastjailer.app.data.StoryRepository

// G2L STAGE A: pure, read-only classification of what is stored in the saves file.
//
// DORMANT BY CONTRACT. Nothing in the app calls this yet (SaveGuardDormancyTest). It never writes, never throws on
// damaged data, and takes no decision that changes live loading, autosave, persistence, the save UI or entitlement
// behaviour. Stage B+C will connect it; until then it exists so the failure states can be defined and tested.
//
// It reads ONE snapshot of the file (`SharedPreferences.getAll`) and inspects the raw value types itself. It never
// calls a typed getter, so it cannot hit the ClassCastException that typed getters throw on real Android when a
// value has the wrong type. If the file could not be loaded at all, `getAll` throws IllegalStateException on
// Android (SharedPreferencesImpl.awaitLoadedLocked); that is reported as Unreadable, never as Missing.

/** The kinds of value a state field is stored as. */
enum class FieldKind { STRING, INT, STRING_SET }

/** One stored game-state field. [alsoAccepts] is a legacy storage form older builds wrote and SaveStore still reads. */
class StateField(val name: String, val kind: FieldKind, val alsoAccepts: FieldKind? = null)

/**
 * The key layout of a slot, mirrored from SaveStore's writeState. `SaveClassifierSlotTest` pins it to the real
 * writer, so a change to one without the other fails a test.
 */
object SaveLayout {
    const val ACTIVE_SLOT = "active_slot"
    const val PREVIOUS = "previous"

    /** Written by the checkpoint/timeline feature; not part of a slot's validity (those reads never throw). */
    val AUXILIARY = listOf("stamp", "lineage", "arrivals")

    val FIELDS: List<StateField> = listOf(
        StateField("chapter", FieldKind.STRING),
        StateField("scene", FieldKind.STRING),
        StateField("courage", FieldKind.INT),
        StateField("honour", FieldKind.INT),
        StateField("health", FieldKind.INT),
        StateField("maxHealth", FieldKind.INT),
        StateField("gold", FieldKind.INT),
        StateField("level", FieldKind.INT),
        StateField("xp", FieldKind.INT),
        StateField("xpToNextLevel", FieldKind.INT),
        // A pre-rework build stored the inventory as a StringSet under this key; SaveStore still reads that.
        StateField("inventory", FieldKind.STRING, alsoAccepts = FieldKind.STRING_SET),
        StateField("trophies", FieldKind.STRING_SET),
        StateField("flags", FieldKind.STRING_SET)
    )
}

enum class FaultReason {
    /** The stored value is not the type the field is written as (a typed getter would throw ClassCastException). */
    WRONG_TYPE,

    /** Other state fields are present but `scene` is not: today this reads as an empty slot and is silently overwritable. */
    SCENE_MISSING_WITH_OTHER_FIELDS,

    /** `scene` is an empty string; no real node has that id. */
    EMPTY_SCENE
}

/** One problem. Carries the key and the *type name* found, never the stored value. */
class Fault(val key: String, val reason: FaultReason, val foundType: String?) {
    override fun toString(): String = "Fault($key, $reason, $foundType)"
}

/** What a stored slot (or its Back snapshot) is. */
sealed class Classification {
    /** Nothing stored. */
    object Missing : Classification() { override fun toString() = "Missing" }

    /** Typed correctly and the scene exists in this build. Missing individual fields are fine (older saves lack some). */
    object Valid : Classification() { override fun toString() = "Valid" }

    /** Structurally damaged. [faults] is never empty. */
    class Malformed(val faults: List<Fault>) : Classification() { override fun toString() = "Malformed($faults)" }

    /**
     * Structurally intact, but its scene is not in this build: a save from another version, content that was
     * removed or renamed, or damage that happens to leave a well-formed string. These cannot be told apart without
     * an explicit save-format version (see qa/G2L_STAGE_A.md). Treated as "protect, do not replace".
     */
    class UnsupportedContent(val sceneId: String) : Classification() { override fun toString() = "UnsupportedContent" }
}

sealed class ActiveSlotClass {
    object Absent : ActiveSlotClass() { override fun toString() = "Absent" }
    class Valid(val slot: Int) : ActiveSlotClass() { override fun toString() = "Valid($slot)" }
    class Invalid(val reason: ActiveSlotFault) : ActiveSlotClass() { override fun toString() = "Invalid($reason)" }
}

enum class ActiveSlotFault { WRONG_TYPE, NON_POSITIVE }

enum class ChooserReason {
    /** No active-slot marker, yet at least one slot holds data: do not guess slot 1. */
    ACTIVE_ABSENT_WITH_DATA,

    /** The marker is stored but is not a positive int. */
    ACTIVE_INVALID
}

/** What a *future* startup should do. Stage A computes it; nothing acts on it. */
sealed class StartupDecision {
    /** No saved data anywhere (or the active slot is simply empty): a new game, as today. */
    class FreshStart(val slot: Int) : StartupDecision() { override fun toString() = "FreshStart($slot)" }

    class Resume(val slot: Int) : StartupDecision() { override fun toString() = "Resume($slot)" }

    /** The active slot exists but is Malformed or UnsupportedContent. */
    class NeedsAttention(val slot: Int, val state: Classification) : StartupDecision() {
        override fun toString() = "NeedsAttention($slot, $state)"
    }

    class Chooser(val reason: ChooserReason) : StartupDecision() { override fun toString() = "Chooser($reason)" }
}

class SlotReport(val slot: Int, val live: Classification, val back: Classification)

sealed class SaveReport {
    /** The file could not be read at all. Not the same as an empty file. */
    object Unreadable : SaveReport() { override fun toString() = "Unreadable" }

    class Readable(
        val active: ActiveSlotClass,
        /** Every slot that has any state or Back-snapshot key, ascending. A slot not listed is Missing. */
        val slots: List<SlotReport>,
        val startup: StartupDecision
    ) : SaveReport() {
        fun slot(slot: Int): SlotReport? = slots.firstOrNull { it.slot == slot }
    }
}

/** What one stored boolean is. Deliberately has no value for "damaged": a damaged flag proves neither state. */
enum class FlagState { ABSENT, TRUE, FALSE, WRONG_TYPE }

sealed class EntitlementReport {
    object Unreadable : EntitlementReport() { override fun toString() = "Unreadable" }

    class Read(val fullStory: FlagState, val testerUnlock: FlagState, val debugSimulated: FlagState) : EntitlementReport()
}

class SaveClassifier(
    private val sceneExists: (String) -> Boolean = { StoryRepository.nodeOrNull(it) != null }
) {

    /** One consistent snapshot of the file, or null when it cannot be read. Never throws. */
    private fun snapshot(prefs: SharedPreferences): Map<String, Any?>? = try {
        prefs.all.toMap()
    } catch (e: RuntimeException) {
        null
    }

    fun report(prefs: SharedPreferences): SaveReport {
        val entries = snapshot(prefs) ?: return SaveReport.Unreadable
        return report(entries)
    }

    internal fun report(entries: Map<String, Any?>): SaveReport.Readable {
        val slots = slotsWithData(entries).map { SlotReport(it, classifyLive(entries, it), classifyBack(entries, it)) }
        val active = classifyActiveSlot(entries)
        return SaveReport.Readable(active, slots, decide(active, slots))
    }

    fun classifyLive(entries: Map<String, Any?>, slot: Int): Classification = classify(entries, "$slot.")

    fun classifyBack(entries: Map<String, Any?>, slot: Int): Classification = classify(entries, "$slot.${SaveLayout.PREVIOUS}.")

    fun classifyActiveSlot(entries: Map<String, Any?>): ActiveSlotClass = when (val v = entries[SaveLayout.ACTIVE_SLOT]) {
        null -> ActiveSlotClass.Absent
        is Int -> if (v > 0) ActiveSlotClass.Valid(v) else ActiveSlotClass.Invalid(ActiveSlotFault.NON_POSITIVE)
        else -> ActiveSlotClass.Invalid(ActiveSlotFault.WRONG_TYPE)
    }

    /**
     * Slots that hold any state field or Back-snapshot key. A slot that has only auxiliary timeline keys
     * (`stamp`, `lineage`, `arrivals`) is not listed: those reads are already guarded elsewhere and such leftovers
     * do not make a slot occupied today (`SaveStore.hasSave` looks only at `scene`).
     */
    fun slotsWithData(entries: Map<String, Any?>): List<Int> {
        val stateNames = SaveLayout.FIELDS.map { it.name }.toSet()
        val slots = sortedSetOf<Int>()
        for ((key, value) in entries) {
            if (value == null) continue
            val dot = key.indexOf('.')
            if (dot <= 0) continue
            val slot = key.substring(0, dot).takeIf { it.length <= 9 && it.all { c -> c in '0'..'9' } }?.toIntOrNull() ?: continue
            if (slot <= 0) continue
            val rest = key.substring(dot + 1)
            if (rest in stateNames || rest.startsWith(SaveLayout.PREVIOUS + ".")) slots.add(slot)
        }
        return slots.toList()
    }

    private fun decide(active: ActiveSlotClass, slots: List<SlotReport>): StartupDecision = when (active) {
        is ActiveSlotClass.Invalid -> StartupDecision.Chooser(ChooserReason.ACTIVE_INVALID)
        ActiveSlotClass.Absent ->
            if (slots.isEmpty()) StartupDecision.FreshStart(1)
            else StartupDecision.Chooser(ChooserReason.ACTIVE_ABSENT_WITH_DATA)
        is ActiveSlotClass.Valid -> {
            val live = slots.firstOrNull { it.slot == active.slot }?.live ?: Classification.Missing
            when (live) {
                Classification.Missing -> StartupDecision.FreshStart(active.slot)
                Classification.Valid -> StartupDecision.Resume(active.slot)
                is Classification.Malformed, is Classification.UnsupportedContent ->
                    StartupDecision.NeedsAttention(active.slot, live)
            }
        }
    }

    private fun classify(entries: Map<String, Any?>, prefix: String): Classification {
        fun valueOf(name: String): Any? = entries[prefix + name]
        val present = SaveLayout.FIELDS.filter { valueOf(it.name) != null }
        val scene = valueOf("scene")
        if (scene == null) {
            return if (present.isEmpty()) Classification.Missing
            else Classification.Malformed(listOf(Fault(prefix + "scene", FaultReason.SCENE_MISSING_WITH_OTHER_FIELDS, null)))
        }
        val faults = ArrayList<Fault>()
        for (field in present) {
            val value = valueOf(field.name)!!
            if (!matches(value, field.kind) && !(field.alsoAccepts != null && matches(value, field.alsoAccepts))) {
                faults.add(Fault(prefix + field.name, FaultReason.WRONG_TYPE, typeName(value)))
            }
        }
        if (scene is String && scene.isEmpty()) faults.add(Fault(prefix + "scene", FaultReason.EMPTY_SCENE, "String"))
        if (faults.isNotEmpty()) return Classification.Malformed(faults)
        return if (sceneExists(scene as String)) Classification.Valid else Classification.UnsupportedContent(scene)
    }

    private fun matches(value: Any, kind: FieldKind): Boolean = when (kind) {
        FieldKind.STRING -> value is String
        FieldKind.INT -> value is Int
        FieldKind.STRING_SET -> value is Set<*> && value.all { it is String }
    }

    private fun typeName(value: Any): String = when (value) {
        is Set<*> -> "Set"
        else -> value.javaClass.simpleName
    }

    // ---- entitlements -------------------------------------------------------------------------------------------

    /** The three boolean flags the entitlement repository reads. A wrong-typed flag is reported as such, never as true or false. */
    fun classifyEntitlements(prefs: SharedPreferences): EntitlementReport {
        val entries = snapshot(prefs) ?: return EntitlementReport.Unreadable
        fun flag(key: String): FlagState = when (val v = entries[key]) {
            null -> FlagState.ABSENT
            is Boolean -> if (v) FlagState.TRUE else FlagState.FALSE
            else -> FlagState.WRONG_TYPE
        }
        return EntitlementReport.Read(flag(KEY_UNLOCKED), flag(KEY_TESTER_UNLOCKED), flag(KEY_DEBUG_SIMULATE_PURCHASE))
    }

    companion object {
        // Mirrors LocalEntitlementRepository's private keys; SaveClassifierEntitlementTest pins them to its source.
        const val KEY_UNLOCKED = "unlocked_full_story"
        const val KEY_TESTER_UNLOCKED = "internal_test_full_story"
        const val KEY_DEBUG_SIMULATE_PURCHASE = "debug_simulate_purchase"
    }
}
