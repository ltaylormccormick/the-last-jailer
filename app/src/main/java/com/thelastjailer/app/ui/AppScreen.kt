package com.thelastjailer.app.ui

/**
 * Every navigable screen in the app. [bottomNavScreens] lists the ones shown in the bottom bar;
 * [JOURNAL] is reachable only from the Story screen's header icon.
 */
enum class AppScreen(val label: String, val glyph: String) {
    STORY("Story", "📜"),
    CHARACTER("Character", "🛡"),
    INVENTORY("Inventory", "🎒"),
    MAP("Map", "🗺"),
    SAVE("Save", "💾"),
    OPTIONS("Options", "⚙"),
    JOURNAL("Journal", "📖")
}

val bottomNavScreens = listOf(
    AppScreen.STORY,
    AppScreen.CHARACTER,
    AppScreen.INVENTORY,
    AppScreen.MAP,
    AppScreen.SAVE,
    AppScreen.OPTIONS
)

/**
 * Whether the bottom-bar entry for [target] can be tapped. During a fight only Inventory is
 * disabled (it hosts the shop); every other screen, including Save and its "go back one step"
 * action, remains reachable. This documents existing behaviour so changes to it are deliberate.
 */
fun isNavigationEnabled(target: AppScreen, combatActive: Boolean): Boolean =
    !(combatActive && target == AppScreen.INVENTORY)
