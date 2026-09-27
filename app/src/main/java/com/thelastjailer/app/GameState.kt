package com.thelastjailer.app

/**
 * Passive heal applied on an ordinary story-scene transition (see [applyChoice] and
 * [GameState.passiveRegenAmount]). At full or near-full health this is deliberately tiny: a typical
 * chapter has ~5-9 such transitions, so even at +1 this totals only ~5-9 HP per chapter - a fraction
 * of what a Healing Draught provides, keeping the shop useful while still easing the walk between
 * fights.
 *
 * [WOUNDED_TRANSITION_REGEN] and [CRITICAL_TRANSITION_REGEN] step up much more sharply once a run is
 * actually in danger. Real playtesting showed that the previous +4/+9 tiers could still leave a
 * player too weak for the next encounter when only one or two story beats separated fights. Healthy
 * runs are unchanged; only sub-50% health recovery is stronger.
 */
private const val SCENE_TRANSITION_REGEN = 1
private const val WOUNDED_TRANSITION_REGEN = 6
private const val CRITICAL_TRANSITION_REGEN = 15

/** A combat defeat cannot leave the next stretch of story below 25% max health. */
private const val DEFEAT_RECOVERY_DIVISOR = 4

/** MaxHealth gained per level-up (see [levelAttackBonus]/[levelDamageReduction] for the rest of level's combat payoff). */
private const val HEALTH_PER_LEVEL = 9

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

/**
 * Applies a [Choice]'s consequences and moves the player to its next node. [applyRegen] adds
 * [GameState.passiveRegenAmount] on top - left `false` only by [resolveCombat]'s victory path, since
 * that transition already carries the fight's own health cost and isn't an ordinary story beat.
 */
fun GameState.applyChoice(choice: Choice, applyRegen: Boolean = true): GameState {
    val consequences = choice.consequences
    var next = this
    consequences.statDeltas.forEach { (stat, delta) -> next = next.withStatDelta(stat, delta) }
    if (applyRegen) next = next.withStatDelta(StatType.HEALTH, next.passiveRegenAmount())
    return next.copy(
        sceneId = choice.nextNodeId,
        flags = next.flags + consequences.setFlags,
        inventory = next.inventory + consequences.grantItemIds,
        trophies = consequences.unlockTrophy?.let { next.trophies + it } ?: next.trophies
    )
}

/** See [SCENE_TRANSITION_REGEN]/[WOUNDED_TRANSITION_REGEN]/[CRITICAL_TRANSITION_REGEN]. */
private fun GameState.passiveRegenAmount(): Int = when {
    health * 4 < maxHealth -> CRITICAL_TRANSITION_REGEN // below 25% max health
    health * 2 < maxHealth -> WOUNDED_TRANSITION_REGEN // below 50% max health
    else -> SCENE_TRANSITION_REGEN
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
 * Combat is never fatal to the run. A win preserves the health actually left at the end of the
 * fight and grants the encounter's XP/gold/trophy. A loss moves to the defeat node (falling back to
 * the victory node) with no reward and stabilizes health at a minimum of 25% max health, preventing
 * one defeat from turning the next encounter into an unavoidable second defeat. Either way,
 * whatever items were used during the fight are consumed from inventory.
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
            ),
            applyRegen = false
        )
    } else {
        val defeatRecoveryFloor = (maxHealth + DEFEAT_RECOVERY_DIVISOR - 1) / DEFEAT_RECOVERY_DIVISOR
        survived.copy(
            health = maxOf(survived.health, defeatRecoveryFloor),
            sceneId = encounter.defeatNodeId ?: encounter.victoryNodeId
        )
    }
}

/**
 * Spends [price] gold to add [itemId] to the inventory, from the Inventory screen's shop. A no-op
 * if the player can't afford it — gold never buys Courage or Honour, only items, so this is the
 * only way gold ever leaves a run.
 */
fun GameState.purchaseItem(itemId: String, price: Int): GameState {
    if (gold < price) return this
    return copy(gold = gold - price, inventory = inventory + itemId)
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
    var newMaxHealth = maxHealth
    while (newXp >= newThreshold) {
        newXp -= newThreshold
        newLevel += 1
        newThreshold = newLevel * 100
        newMaxHealth += HEALTH_PER_LEVEL
    }
    return copy(xp = newXp, level = newLevel, xpToNextLevel = newThreshold, maxHealth = newMaxHealth)
}

/**
 * Level's combat payoff, read by [com.thelastjailer.app.ui.CombatScreen] alongside item bonuses:
 * without this, XP had zero mechanical effect — a Monte Carlo simulation of every encounter
 * (generous play: full combat gear, draughts refreshed each fight) showed combat becoming
 * unwinnable from roughly the back third of the story onward, since enemy stats scale linearly
 * per chapter while player power was hard-capped ([maxHealth] never increased; items cap out at
 * +3 attack/+7 damage reduction total, see [com.thelastjailer.app.data.ItemCatalog]). These two
 * coefficients (plus [HEALTH_PER_LEVEL] above) were tuned by re-running that simulation until the
 * whole curve stayed comfortably winnable, with the final encounter deliberately left as the one
 * real climactic risk rather than flattened to the same near-100% as everything before it.
 */
fun GameState.levelAttackBonus(): Int = (level - 1) * 7 / 10

/** See [levelAttackBonus]. */
fun GameState.levelDamageReduction(): Int = (level - 1) * 5 / 10

/** Persists [GameState] across numbered save slots, plus which slot is currently active. */
class SaveStore(private val prefs: android.content.SharedPreferences) {
    private val stateFields = listOf(
        "chapter", "scene", "courage", "honour", "health", "maxHealth", "gold", "level",
        "xp", "xpToNextLevel", "inventory", "trophies", "flags"
    )

    fun save(slot: Int, state: GameState) {
        writeState(slot.toString(), state, prefs.edit())
            .putInt("active_slot", slot)
            .apply()
    }

    /**
     * Persists exactly one pre-choice state for the active slot. Restoring this snapshot is safer
     * than merely changing sceneId because choices can also change stats, health, flags, items and
     * trophies. Only ordinary story choices create this checkpoint; combat and shop mutations clear
     * it so the back action cannot rewind a fight or duplicate purchases.
     */
    fun savePreviousState(slot: Int, state: GameState) {
        writeState(previousPrefix(slot), state, prefs.edit()).apply()
    }

    fun loadPreviousState(slot: Int): GameState? = readState(previousPrefix(slot), slot)

    fun hasPreviousState(slot: Int): Boolean = prefs.contains("${previousPrefix(slot)}.scene")

    fun clearPreviousState(slot: Int) {
        val editor = prefs.edit()
        stateFields.forEach { field -> editor.remove("${previousPrefix(slot)}.$field") }
        editor.apply()
    }

    /** Restores and consumes the one-step story checkpoint, and autosaves the restored state. */
    fun restorePreviousState(slot: Int): GameState? {
        val previous = loadPreviousState(slot) ?: return null
        save(slot, previous)
        clearPreviousState(slot)
        return previous
    }

    /** Resets one slot to a clean Chapter I state without affecting entitlements or other slots. */
    fun restart(slot: Int): GameState {
        clearPreviousState(slot)
        val fresh = GameState(activeSlot = slot)
        save(slot, fresh)
        return fresh
    }

    fun load(slot: Int): GameState? = readState(slot.toString(), slot)

    private fun writeState(
        prefix: String,
        state: GameState,
        editor: android.content.SharedPreferences.Editor
    ): android.content.SharedPreferences.Editor = editor
        .putString("$prefix.chapter", state.chapterId)
        .putString("$prefix.scene", state.sceneId)
        .putInt("$prefix.courage", state.courage)
        .putInt("$prefix.honour", state.honour)
        .putInt("$prefix.health", state.health)
        .putInt("$prefix.maxHealth", state.maxHealth)
        .putInt("$prefix.gold", state.gold)
        .putInt("$prefix.level", state.level)
        .putInt("$prefix.xp", state.xp)
        .putInt("$prefix.xpToNextLevel", state.xpToNextLevel)
        .putString("$prefix.inventory", state.inventory.joinToString(","))
        .putStringSet("$prefix.trophies", state.trophies)
        .putStringSet("$prefix.flags", state.flags)

    private fun readState(prefix: String, activeSlot: Int): GameState? {
        val scene = prefs.getString("$prefix.scene", null) ?: return null
        val level = prefs.getInt("$prefix.level", 1)
        // A save written before level granted +HEALTH_PER_LEVEL maxHealth per level stored a
        // maxHealth that never accounted for levels already earned at that point. Recompute the
        // level-derived floor here for both normal saves and persisted one-step checkpoints.
        val levelDerivedMaxHealth = 100 + HEALTH_PER_LEVEL * (level - 1)
        return GameState(
            activeSlot = activeSlot,
            chapterId = prefs.getString("$prefix.chapter", null) ?: "chapter_1",
            sceneId = scene,
            courage = prefs.getInt("$prefix.courage", 1),
            honour = prefs.getInt("$prefix.honour", 0),
            health = prefs.getInt("$prefix.health", 100),
            maxHealth = maxOf(prefs.getInt("$prefix.maxHealth", 100), levelDerivedMaxHealth),
            gold = prefs.getInt("$prefix.gold", 25),
            level = level,
            xp = prefs.getInt("$prefix.xp", 0),
            xpToNextLevel = prefs.getInt("$prefix.xpToNextLevel", 100),
            inventory = readInventory(prefix),
            trophies = prefs.getStringSet("$prefix.trophies", emptySet()) ?: emptySet(),
            flags = prefs.getStringSet("$prefix.flags", emptySet()) ?: emptySet()
        )
    }

    /**
     * A pre-rework build stored inventory as a StringSet under this same key; reading that value
     * back with [android.content.SharedPreferences.getString] throws ClassCastException on real
     * Android (unlike the JVM test double), so fall back to the legacy format instead of crashing.
     */
    private fun readInventory(prefix: String): List<String> = try {
        prefs.getString("$prefix.inventory", "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    } catch (e: ClassCastException) {
        prefs.getStringSet("$prefix.inventory", emptySet())?.toList() ?: emptyList()
    }

    private fun previousPrefix(slot: Int): String = "$slot.previous"

    fun hasSave(slot: Int): Boolean = prefs.contains("$slot.scene")

    fun currentActiveSlot(): Int? = prefs.getInt("active_slot", -1).takeIf { it > 0 }

    fun setActiveSlot(slot: Int) {
        prefs.edit().putInt("active_slot", slot).apply()
    }
}
