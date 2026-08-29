package com.thelastjailer.app

/** A stat that a [Choice] can require or a [Consequences] can adjust. */
enum class StatType {
    COURAGE,
    HONOUR,
    HEALTH,
    GOLD,
    XP
}

/** An inventory item definition, looked up by id from [com.thelastjailer.app.data.ItemCatalog]. */
data class Item(
    val id: String,
    val name: String,
    val description: String
)

/** Gates whether a [Choice] is shown/selectable for the current [GameState]. */
data class ChoiceRequirement(
    val requiredFlags: Set<String> = emptySet(),
    val forbiddenFlags: Set<String> = emptySet(),
    val minStats: Map<StatType, Int> = emptyMap()
) {
    fun isSatisfiedBy(state: GameState): Boolean {
        if (!state.flags.containsAll(requiredFlags)) return false
        if (state.flags.any { it in forbiddenFlags }) return false
        return minStats.all { (stat, minValue) -> state.statValue(stat) >= minValue }
    }
}

/** What happens to the player state when a [Choice] is taken. */
data class Consequences(
    val statDeltas: Map<StatType, Int> = emptyMap(),
    val setFlags: Set<String> = emptySet(),
    val grantItemIds: List<String> = emptyList(),
    val unlockTrophy: String? = null
)

data class Choice(
    val label: String,
    val nextNodeId: String,
    val requirements: ChoiceRequirement? = null,
    val consequences: Consequences = Consequences()
)

data class StoryNode(
    val id: String,
    val chapterId: String,
    val title: String,
    val illustrationId: String,
    val narrativeText: String,
    val choices: List<Choice>
)

data class Chapter(
    val id: String,
    val number: Int,
    val title: String,
    val startNodeId: String
)
