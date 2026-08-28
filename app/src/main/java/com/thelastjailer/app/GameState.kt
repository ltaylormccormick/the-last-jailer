package com.thelastjailer.app

data class GameState(
    val sceneId: String = "prologue",
    val courage: Int = 0,
    val honour: Int = 0,
    val inventory: Set<String> = emptySet(),
    val trophies: Set<String> = emptySet()
)

class SaveStore(private val prefs: android.content.SharedPreferences) {
    fun save(slot: Int, state: GameState) {
        prefs.edit()
            .putString("$slot.scene", state.sceneId)
            .putInt("$slot.courage", state.courage)
            .putInt("$slot.honour", state.honour)
            .putStringSet("$slot.inventory", state.inventory)
            .putStringSet("$slot.trophies", state.trophies)
            .apply()
    }

    fun load(slot: Int): GameState? {
        val scene = prefs.getString("$slot.scene", null) ?: return null
        return GameState(
            sceneId = scene,
            courage = prefs.getInt("$slot.courage", 0),
            honour = prefs.getInt("$slot.honour", 0),
            inventory = prefs.getStringSet("$slot.inventory", emptySet()) ?: emptySet(),
            trophies = prefs.getStringSet("$slot.trophies", emptySet()) ?: emptySet()
        )
    }
}
