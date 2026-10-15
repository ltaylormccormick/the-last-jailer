package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.StoryRepository

/** Shared fixtures for the checkpoint tests. Uses the real story so scene and chapter checks are real. */
object Cp {
    const val L1 = "0123456789abcdef"
    const val L2 = "fedcba9876543210"
    const val L3 = "a1a1a1a1b2b2b2b2"

    val rules: CheckpointRules = CheckpointRules.forStory()

    fun file(slot: Int) = "jailer_ckpt_$slot"
    fun stageFile(slot: Int) = "jailer_ckpt_stage_$slot"

    fun chapterId(n: Int): String = StoryRepository.chapters.first { it.number == n }.id
    fun startScene(n: Int): String = StoryRepository.chapters.first { it.number == n }.startNodeId

    fun state(
        n: Int,
        scene: String = startScene(n),
        inventory: List<String> = listOf("healing_draught", "sturdy_buckler", "healing_draught"),
        trophies: Set<String> = setOf("First Blood", "Six, Not One"),
        flags: Set<String> = setOf("flag_$n", "kept_the_token")
    ): GameState = GameState(
        activeSlot = 1,
        chapterId = chapterId(n),
        sceneId = scene,
        courage = n + 1,
        honour = n - 2,
        health = 80 + n,
        maxHealth = 100 + n,
        gold = 25 * n,
        level = 1 + n / 3,
        xp = 10 * n,
        xpToNextLevel = 100 + n,
        inventory = inventory,
        trophies = trophies,
        flags = flags
    )

    fun record(n: Int, lineage: String = L1, state: GameState = state(n)): CheckpointRecord =
        CheckpointRecord.fromState(state, n, lineage)

    fun records(upTo: Int, lineage: String = L1): List<CheckpointRecord> = (1..upTo).map { record(it, lineage) }

    fun store(device: SimulatedDevice, rules: CheckpointRules = Cp.rules): CheckpointStore =
        CheckpointStore({ slot -> device.prefs(file(slot)) }, rules)

    fun stageStore(device: SimulatedDevice): CheckpointStore =
        CheckpointStore({ slot -> device.prefs(stageFile(slot)) }, rules)

    /** Same content, one field changed, CRC recomputed (so only the content differs, not the integrity). */
    fun rebuilt(
        r: CheckpointRecord,
        version: Int = r.version,
        chapterNumber: Int = r.chapterNumber,
        lineage: String = r.lineage,
        chapterId: String = r.chapterId,
        sceneId: String = r.sceneId,
        courage: Int = r.courage,
        honour: Int = r.honour,
        health: Int = r.health,
        maxHealth: Int = r.maxHealth,
        gold: Int = r.gold,
        level: Int = r.level,
        xp: Int = r.xp,
        xpToNextLevel: Int = r.xpToNextLevel,
        inventory: List<String> = r.inventory,
        trophies: Set<String> = r.trophies,
        flags: Set<String> = r.flags
    ): CheckpointRecord = CheckpointRecord.sealed(
        version, chapterNumber, lineage, chapterId, sceneId, courage, honour, health, maxHealth,
        gold, level, xp, xpToNextLevel, inventory, trophies, flags
    )

    /** Same content as [r] but carrying [crc] (to forge a mismatch). */
    fun withCrc(r: CheckpointRecord, crc: Long): CheckpointRecord = CheckpointRecord(
        r.version, r.chapterNumber, r.lineage, r.chapterId, r.sceneId, r.courage, r.honour, r.health,
        r.maxHealth, r.gold, r.level, r.xp, r.xpToNextLevel, r.inventory, r.trophies, r.flags, crc
    )

    /** An approximation of the on-disk size of a preferences file in Android's XML format. */
    fun xmlSize(map: Map<String, Any?>): Int {
        var n = 60
        map.forEach { (k, v) ->
            n += when (v) {
                is String -> "    <string name=\"$k\">$v</string>\n".length
                is Int -> "    <int name=\"$k\" value=\"$v\" />\n".length
                is Set<*> -> "    <set name=\"$k\">\n".length + v.sumOf { "        <string>$it</string>\n".length } + "    </set>\n".length
                else -> 40
            }
        }
        return n
    }
}

/** A preferences view whose `getAll()` blows up, to prove the store never lets a read failure escape. */
fun unreadablePrefs(base: SharedPreferences): SharedPreferences = object : SharedPreferences by base {
    override fun getAll(): MutableMap<String, *> = throw IllegalStateException("simulated unreadable file")
}

/** A preferences view whose first `commit()` throws a runtime exception instead of returning. */
fun commitThrowsOnce(base: SharedPreferences): SharedPreferences {
    var thrown = false
    return object : SharedPreferences by base {
        override fun edit(): SharedPreferences.Editor {
            val inner = base.edit()
            return object : SharedPreferences.Editor by inner {
                override fun commit(): Boolean {
                    if (!thrown) {
                        thrown = true
                        throw IllegalStateException("simulated commit exception")
                    }
                    return inner.commit()
                }
            }
        }
    }
}
