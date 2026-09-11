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
        "silent_forge" to R.drawable.silent_forge,
        "warded_gate" to R.drawable.warded_gate,
        "warden_chamber" to R.drawable.warden_chamber,
        "cloaks_truth" to R.drawable.cloaks_truth,
        "first_seal" to R.drawable.first_seal,
        "seal_breaker_ambush" to R.drawable.seal_breaker_ambush,
        "chapter2_threshold" to R.drawable.chapter2_threshold,
        "the_answer" to R.drawable.the_answer,
        "the_binding_rite" to R.drawable.the_binding_rite,
        "the_first_true_knock" to R.drawable.the_first_true_knock,
        "voices_above" to R.drawable.voices_above,
        "a_choice_at_the_door" to R.drawable.a_choice_at_the_door,
        "chapter3_threshold" to R.drawable.chapter3_threshold,
        "prisoners_second_offer" to R.drawable.prisoners_second_offer,
        "the_cost_aftermath" to R.drawable.the_cost_aftermath,
        "not_a_rumour" to R.drawable.not_a_rumour,
        "chapter4_threshold" to R.drawable.chapter4_threshold,
        "cinder_envoy" to R.drawable.cinder_envoy,
        "envoy_reveals_guard" to R.drawable.envoy_reveals_guard,
        "cinder_adept_ambush" to R.drawable.cinder_adept_ambush,
        "aftermath_of_betrayal" to R.drawable.aftermath_of_betrayal,
        "cinder_marshal_missive_read" to R.drawable.cinder_marshal_missive_read,
        "chapter5_threshold" to R.drawable.chapter5_threshold,
        "greymoor_ward" to R.drawable.greymoor_ward,
        "kestrels_locket_found" to R.drawable.kestrels_locket_found,
        "the_unbound_creature" to R.drawable.the_unbound_creature,
        "greymoor_aftermath" to R.drawable.greymoor_aftermath,
        "chapter6_threshold" to R.drawable.chapter6_threshold,
        "voss_at_the_door" to R.drawable.voss_at_the_door,
        "the_price_of_trust" to R.drawable.the_price_of_trust,
        "loyalists_ambush" to R.drawable.loyalists_ambush,
        "after_the_ambush" to R.drawable.after_the_ambush,
        "what_voss_offers" to R.drawable.what_voss_offers,
        "chapter7_threshold" to R.drawable.chapter7_threshold
    )

    fun get(illustrationId: String): Int? = illustrations[illustrationId]
}
