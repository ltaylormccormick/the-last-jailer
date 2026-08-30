package com.thelastjailer.app

data class GameState(
    val activeSlot: Int = 1,
    val chapterId: String = "chapter_1",
    val sceneId: String = "fallen_knight",
    val courage: Int = 1,
    val honour: Int = 0,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val gold: Int = 25,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNextLevel: Int = 100,
    val inventory: List<String> = emptyList(),
    val trophies: Set<String> = emptySet(),
    val flags: Set<String> = emptySet()
) {
    fun statValue(stat: StatType): Int = when (stat) {
        StatType.COURAGE -> courage
        StatType.HONOUR -> honour
        StatType.HEALTH -> health
        StatType.GOLD -> gold
        StatType.XP -> xp
    }
}

/** Applies a [Choice]'s consequences and moves the player to its next node. */
fun GameState.applyChoice(choice: Choice): GameState {
    val consequences = choice.consequences
    var next = this
    consequences.statDeltas.forEach { (stat, delta) -> next = next.withStatDelta(stat, delta) }
    return next.copy(
        sceneId = choice.nextNodeId,
        flags = next.flags + consequences.setFlags,
        inventory = next.inventory + consequences.grantItemIds,
        trophies = consequences.unlockTrophy?.let { next.trophies + it } ?: next.trophies
    )
}

private fun GameState.withStatDelta(stat: StatType, delta: Int): GameState = when (stat) {
    StatType.COURAGE -> copy(courage = courage + delta)
    StatType.HONOUR -> copy(honour = honour + delta)
    StatType.HEALTH -> copy(health = (health + delta).coerceIn(0, maxHealth))
    StatType.GOLD -> copy(gold = (gold + delta).coerceAtLeast(0))
    StatType.XP -> applyXpGain(delta)
}

/**
 * Applies the result of a [CombatEncounter] played out in [com.thelastjailer.app.ui.CombatScreen].
 * Combat is never fatal to the run: health always ends up at least 1, however much [CombatOutcome.damageTaken]
 * was. A win grants the encounter's XP/gold/trophy and moves to [CombatEncounter.victoryNodeId];
 * a loss moves to [CombatEncounter.defeatNodeId] (falling back to the same node as victory) with
 * no reward. Either way, whatever items were used during the fight are consumed from inventory.
 */
fun GameState.resolveCombat(encounter: CombatEncounter, outcome: CombatOutcome): GameState {
    val survived = copy(health = (health - outcome.damageTaken).coerceIn(1, maxHealth))
        .consumeItems(outcome.consumedItemIds)
    return if (outcome.victory) {
        survived.applyChoice(
            Choice(
                label = "",
                nextNodeId = encounter.victoryNodeId,
                consequences = Consequences(
                    statDeltas = mapOf(StatType.XP to encounter.xpReward, StatType.GOLD to encounter.goldReward),
                    unlockTrophy = encounter.unlockTrophy
                )
            )
        )
    } else {
        survived.copy(sceneId = encounter.defeatNodeId ?: encounter.victoryNodeId)
    }
}

/** Removes one occurrence per id in [itemIds] from the inventory (e.g. a consumed potion). */
fun GameState.consumeItems(itemIds: List<String>): GameState {
    if (itemIds.isEmpty()) return this
    val remaining = inventory.toMutableList()
    itemIds.forEach { remaining.remove(it) }
    return copy(inventory = remaining)
}

/** XP gains roll over into levels; each level needs 100 more XP than the last. */
private fun GameState.applyXpGain(delta: Int): GameState {
    if (delta <= 0) return copy(xp = (xp + delta).coerceAtLeast(0))
    var newXp = xp + delta
    var newLevel = level
    var newThreshold = xpToNextLevel
    while (newXp >= newThreshold) {
        newXp -= newThreshold
        newLevel += 1
        newThreshold = newLevel * 100
    }
    return copy(xp = newXp, level = newLevel, xpToNextLevel = newThreshold)
}

/** Persists [GameState] across numbered save slots, plus which slot is currently active. */
class SaveStore(private val prefs: android.content.SharedPreferences) {
    fun save(slot: Int, state: GameState) {
        prefs.edit()
            .putString("$slot.chapter", state.chapterId)
            .putString("$slot.scene", state.sceneId)
            .putInt("$slot.courage", state.courage)
            .putInt("$slot.honour", state.honour)
            .putInt("$slot.health", state.health)
            .putInt("$slot.maxHealth", state.maxHealth)
            .putInt("$slot.gold", state.gold)
            .putInt("$slot.level", state.level)
            .putInt("$slot.xp", state.xp)
            .putInt("$slot.xpToNextLevel", state.xpToNextLevel)
            .putString("$slot.inventory", state.inventory.joinToString(","))
            .putStringSet("$slot.trophies", state.trophies)
            .putStringSet("$slot.flags", state.flags)
            .putInt("active_slot", slot)
            .apply()
    }

    fun load(slot: Int): GameState? {
        val scene = prefs.getString("$slot.scene", null) ?: return null
        return GameState(
            activeSlot = slot,
            chapterId = prefs.getString("$slot.chapter", null) ?: "chapter_1",
            sceneId = scene,
            courage = prefs.getInt("$slot.courage", 1),
            honour = prefs.getInt("$slot.honour", 0),
            health = prefs.getInt("$slot.health", 100),
            maxHealth = prefs.getInt("$slot.maxHealth", 100),
            gold = prefs.getInt("$slot.gold", 25),
            level = prefs.getInt("$slot.level", 1),
            xp = prefs.getInt("$slot.xp", 0),
            xpToNextLevel = prefs.getInt("$slot.xpToNextLevel", 100),
            inventory = prefs.getString("$slot.inventory", "")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            trophies = prefs.getStringSet("$slot.trophies", emptySet()) ?: emptySet(),
            flags = prefs.getStringSet("$slot.flags", emptySet()) ?: emptySet()
        )
    }

    fun hasSave(slot: Int): Boolean = prefs.contains("$slot.scene")

    fun currentActiveSlot(): Int? = prefs.getInt("active_slot", -1).takeIf { it > 0 }

    fun setActiveSlot(slot: Int) {
        prefs.edit().putInt("active_slot", slot).apply()
    }
}
