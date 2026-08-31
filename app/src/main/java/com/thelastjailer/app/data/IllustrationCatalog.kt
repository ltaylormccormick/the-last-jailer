package com.thelastjailer.app.data

import com.thelastjailer.app.R

/** Maps a [com.thelastjailer.app.StoryNode.illustrationId] to its real drawable resource, if art exists for it yet. */
object IllustrationCatalog {
    private val illustrations: Map<String, Int> = mapOf(
        "black_door_beneath_the_tree" to R.drawable.black_door_beneath_the_tree,
        "iron_door_open_tunnel" to R.drawable.iron_door_open_tunnel,
        "knight_sword_drawn_door" to R.drawable.knight_sword_drawn_door,
        "road_away_from_tree" to R.drawable.road_away_from_tree,
        "root_tunnel_dwarven_path" to R.drawable.root_tunnel_dwarven_path,
        "dwarven_hold_gate" to R.drawable.dwarven_hold_gate,
        "cavern_ambush" to R.drawable.cavern_ambush,
        "threshold_ahead" to R.drawable.threshold_ahead,
        "roots_descent" to R.drawable.roots_descent,
        "silent_forge" to R.drawable.silent_forge
    )

    fun get(illustrationId: String): Int? = illustrations[illustrationId]
}
