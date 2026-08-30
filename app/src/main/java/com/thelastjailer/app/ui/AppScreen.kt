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
