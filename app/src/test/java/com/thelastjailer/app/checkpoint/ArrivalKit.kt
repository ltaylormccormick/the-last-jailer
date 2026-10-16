package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences
import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.data.StoryRepository

/** Finds real edges in the real story, so the tests exercise the same transitions the game does. */
object Story {
    const val LIVE = "jailer_saves"
    const val ENTITLEMENTS = "jailer_entitlements"

    private fun chapterOf(id: String): String? = StoryRepository.nodeOrNull(id)?.chapterId

    /** A node in chapter [n] with a choice that moves the player into chapter [n] + 1. */
    fun crossing(n: Int): Pair<StoryNode, Choice> {
        for (node in StoryRepository.nodesInChapter(Cp.chapterId(n)).sortedBy { it.id }) {
            for (c in node.choices) if (chapterOf(c.nextNodeId) == Cp.chapterId(n + 1)) return node to c
        }
        error("no story edge from chapter $n to ${n + 1}")
    }

    /** Every (node, choice) edge in the story whose destination lies in a different chapter. */
    fun allCrossings(): List<Triple<StoryNode, Choice, StoryNode>> {
        val out = ArrayList<Triple<StoryNode, Choice, StoryNode>>()
        for (ch in StoryRepository.chapters) for (node in StoryRepository.nodesInChapter(ch.id).sortedBy { it.id }) {
            for (c in node.choices) {
                val dest = StoryRepository.nodeOrNull(c.nextNodeId) ?: continue
                if (dest.chapterId != node.chapterId) out += Triple(node, c, dest)
            }
        }
        return out
    }

    /** The six Chapter 30 to Chapter 1 "the watch goes on" edges. */
    fun loops(): List<Pair<StoryNode, Choice>> =
        allCrossings().filter { it.first.chapterId == Cp.chapterId(30) && it.third.chapterId == Cp.chapterId(1) }
            .map { it.first to it.second }

    /** A node in chapter [n] with a choice that stays in chapter [n], different from [avoid] if possible. */
    fun staying(n: Int, avoid: String? = null): Pair<StoryNode, Choice> {
        val candidates = ArrayList<Pair<StoryNode, Choice>>()
        for (node in StoryRepository.nodesInChapter(Cp.chapterId(n)).sortedBy { it.id }) {
            for (c in node.choices) if (chapterOf(c.nextNodeId) == Cp.chapterId(n)) candidates += node to c
        }
        return candidates.firstOrNull { it.first.id != avoid } ?: candidates.first()
    }

    /** A believable state standing on [node] in [slot], distinguishable by [tag]. */
    fun stateAt(node: StoryNode, slot: Int = 1, tag: String = "a"): GameState {
        val n = ChapterIds.numberOf(node.chapterId)!!
        val base = Cp.state(n, scene = node.id, flags = setOf("flag_$n", tag)).copy(activeSlot = slot)
        // Consistent with what a reload computes (max health follows the level), so a state that is saved
        // and loaded again is the same state, as it is for any save written by this build.
        val maxHealth = 100 + 9 * (base.level - 1)
        return base.copy(maxHealth = maxHealth, health = minOf(base.health, maxHealth))
    }
}

/**
 * One pretend process: the live save, the checkpoint store and the recorder, over a [SimulatedDevice].
 * [reopen] kills the process and starts a new one over the same disk.
 */
class Rig(
    val device: SimulatedDevice = SimulatedDevice(),
    val enabled: Boolean = true,
    val hooks: ArrivalRecorder.Hooks = ArrivalRecorder.Hooks(),
    ckptStore: CheckpointStore? = null,
    val withRecorder: Boolean = true
) {
    val logs = ArrayList<String>()
    val saves = SaveStore(device.prefs(Story.LIVE))
    val ckpt: CheckpointStore = ckptStore ?: Cp.store(device)
    val recorder: ArrivalRecorder? =
        if (withRecorder) ArrivalRecorder(ckpt, Cp.rules, enabled, { m, _ -> logs += m }, hooks) else null

    fun reopen(enabled: Boolean = this.enabled, hooks: ArrivalRecorder.Hooks = this.hooks): Rig {
        device.restart()
        return Rig(device, enabled, hooks, null, withRecorder)
    }

    /** Takes the real crossing choice from chapter [from] into [from] + 1. Returns (state before, state after). */
    fun arrive(from: Int, slot: Int = 1, tag: String = "a"): Pair<GameState, GameState> {
        val (node, choice) = Story.crossing(from)
        val before = Story.stateAt(node, slot, tag)
        return before to saves.commitChoice(before, choice, recorder)
    }

    /** Plays arrivals 1->2, 2->3 ... up to chapter [to] and returns the arrival state of each chapter. */
    fun playTo(to: Int, slot: Int = 1, tag: String = "a"): Map<Int, GameState> {
        val arrivals = LinkedHashMap<Int, GameState>()
        for (from in 1 until to) arrivals[from + 1] = arrive(from, slot, tag).second
        return arrivals
    }

    fun timeline(slot: Int = 1): Timeline? = saves.readTimeline(slot)

    fun fileRecords(slot: Int = 1): List<CheckpointRecord> {
        val lineage = timeline(slot)?.lineage ?: ckpt.storedLineage(slot) ?: return emptyList()
        return ckpt.readAll(slot, lineage).records
    }

    /**
     * The independent oracle for the trust rule: the chapters whose record is usable *right now*. It mirrors
     * the rule in the contract (lineage match, rules, K <= live chapter, ledger digest equals record
     * digest) and shares no code with the recorder beyond the digest function.
     */
    fun usable(slot: Int = 1): List<Int> {
        val tl = timeline(slot) ?: return emptyList()
        val live = saves.load(slot) ?: return emptyList()
        val c = ChapterIds.numberOf(live.chapterId) ?: return emptyList()
        return ckpt.readAll(slot, tl.lineage).records
            .filter { it.chapterNumber <= c && tl.ledger.digestFor(it.chapterNumber) == CheckpointDigest.of(slot, it) }
            .map { it.chapterNumber }
    }

    fun usableRecord(chapter: Int, slot: Int = 1): CheckpointRecord? {
        val tl = timeline(slot) ?: return null
        if (chapter !in usable(slot)) return null
        return ckpt.readAll(slot, tl.lineage).records.first { it.chapterNumber == chapter }
    }

    fun liveFile(): Map<String, Any?> = device.disk(Story.LIVE)
    fun ckptFile(slot: Int = 1): Map<String, Any?> = device.disk(Cp.file(slot))
}

/** What the arrival record for [chapter] should be, given the state the player actually arrived in. */
fun expectedRecord(arrival: GameState, chapter: Int, lineage: String): CheckpointRecord =
    CheckpointRecord.fromState(arrival, chapter, lineage)

/** A preferences view that counts how often it is touched, to prove a slot is left alone. */
class CountingPrefs(private val base: SharedPreferences) : SharedPreferences by base {
    var touches = 0
        private set

    override fun getAll(): MutableMap<String, *> { touches++; return base.all }
    override fun edit(): SharedPreferences.Editor { touches++; return base.edit() }
    override fun getString(key: String?, defValue: String?): String? { touches++; return base.getString(key, defValue) }
    override fun getInt(key: String?, defValue: Int): Int { touches++; return base.getInt(key, defValue) }
    override fun contains(key: String?): Boolean { touches++; return base.contains(key) }
}

/** The chapters a ledger has entries for, ascending. */
fun Ledger.chapters(): List<Int> = if (isEmpty) emptyList() else encode().split(",").map { it.substringBefore(':').toInt() }
