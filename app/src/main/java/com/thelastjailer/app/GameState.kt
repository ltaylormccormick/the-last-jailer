package com.thelastjailer.app

/**
 * Passive heal applied on an ordinary story-scene transition (see [applyChoice] and
 * [GameState.passiveRegenAmount]). At full or near-full health this is deliberately tiny: a typical
 * chapter has ~5-9 such transitions, so even at +1 this totals only ~5-9 HP per chapter (out of 100
 * max health) - a fraction of what a single Healing Draught (40 gold, +25 HP) or Greater Healing
 * Draught (150 gold, +50 HP) provides, so it eases the walk between fights without meaningfully
 * competing with the shop economy.
 *
 * [WOUNDED_TRANSITION_REGEN] and [CRITICAL_TRANSITION_REGEN] exist because that same +1/transition
 * is negligible as an actual recovery path: a player who ends a losing fight in single digits (combat
 * floors health at 1, never 0 - see [resolveCombat]) had no way to meaningfully recover before an
 * unknown next encounter short of a finite, gold-gated draught. Below half health the passive heal
 * steps up, and below a quarter it steps up again, so a genuinely low-health run recovers over the
 * next several story beats without needing an explicit "rest" scene at a specific location, and
 * without changing anything for a player who's already healthy.
 */
private const val SCENE_TRANSITION_REGEN = 1
private const val WOUNDED_TRANSITION_REGEN = 4
private const val CRITICAL_TRANSITION_REGEN = 9

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
            ),
            applyRegen = false
        )
    } else {
        survived.copy(sceneId = encounter.defeatNodeId ?: encounter.victoryNodeId)
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
        val level = prefs.getInt("$slot.level", 1)
        // A save written before level granted +HEALTH_PER_LEVEL maxHealth per level (see
        // GameState.applyXpGain) stored a maxHealth that never accounts for levels already
        // earned at that point - only future level-ups would have added the bonus. Recomputing
        // the level-derived floor here and taking the higher of the two corrects that backlog for
        // existing saves without discarding maxHealth from any other source.
        val levelDerivedMaxHealth = 100 + HEALTH_PER_LEVEL * (level - 1)
        return GameState(
            activeSlot = slot,
            chapterId = prefs.getString("$slot.chapter", null) ?: "chapter_1",
            sceneId = scene,
            courage = prefs.getInt("$slot.courage", 1),
            honour = prefs.getInt("$slot.honour", 0),
            health = prefs.getInt("$slot.health", 100),
            maxHealth = maxOf(prefs.getInt("$slot.maxHealth", 100), levelDerivedMaxHealth),
            gold = prefs.getInt("$slot.gold", 25),
            level = level,
            xp = prefs.getInt("$slot.xp", 0),
            xpToNextLevel = prefs.getInt("$slot.xpToNextLevel", 100),
            inventory = readInventory(slot),
            trophies = prefs.getStringSet("$slot.trophies", emptySet()) ?: emptySet(),
            flags = prefs.getStringSet("$slot.flags", emptySet()) ?: emptySet()
        )
    }

    /**
     * A pre-rework build stored inventory as a StringSet under this same key; reading that value
     * back with [android.content.SharedPreferences.getString] throws ClassCastException on real
     * Android (unlike the JVM test double), so fall back to the legacy format instead of crashing.
     */
    private fun readInventory(slot: Int): List<String> = try {
        prefs.getString("$slot.inventory", "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    } catch (e: ClassCastException) {
        prefs.getStringSet("$slot.inventory", emptySet())?.toList() ?: emptyList()
    }

    fun hasSave(slot: Int): Boolean = prefs.contains("$slot.scene")

    fun currentActiveSlot(): Int? = prefs.getInt("active_slot", -1).takeIf { it > 0 }

    fun setActiveSlot(slot: Int) {
        prefs.edit().putInt("active_slot", slot).apply()
    }
}
