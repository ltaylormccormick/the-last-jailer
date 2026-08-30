package com.thelastjailer.app.data

import com.thelastjailer.app.Item

/** Canonical definitions for every item id that can appear in a [com.thelastjailer.app.GameState] inventory. */
object ItemCatalog {
    private val items: Map<String, Item> = listOf(
        Item(
            id = "broken_sword",
            name = "Broken Sword",
            description = "The blade Kaelen carried out of disgrace. Still sharp enough to matter."
        ),
        Item(
            id = "dwarven_token",
            name = "Dwarven Token",
            description = "A stonebeard's mark of thanks, warm to the touch even in the cold of the hold."
        ),
        Item(
            id = "traveler_ration",
            name = "Traveler's Ration",
            description = "Hard bread and dried meat. Not much, but it will keep a man walking."
        ),
        Item(
            id = "healing_draught",
            name = "Healing Draught",
            description = "A dwarven remedy, bitter and effective."
        )
    ).associateBy { it.id }

    fun get(id: String): Item? = items[id]

    fun resolve(ids: List<String>): List<Item> = ids.mapNotNull { items[it] }
}
