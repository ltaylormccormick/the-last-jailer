package com.thelastjailer.app.data

/**
 * GENERATED at B0 of the Chapter 17 expansion, from the committed sources at 369a95f (nothing of Chapter 17 had been touched).
 * The eight shipped scenes of Chapter 17: a SHA-256 of each scene's whitespace-collapsed text, its title, picture and fight, and
 * each choice's label, stat changes, flags set and cleared, items, trophy and gates, with where it led. Used only by Chapter17ShippedPinTest.
 */
internal object Chapter17Shipped {
    class C(val label: String, val deltas: String, val set: String, val clear: String, val items: String, val trophy: String,
            val required: String, val forbidden: String, val min: String, val next: String)
    class S(val id: String, val chapter: String, val title: String, val illustration: String, val combat: String, val sha: String, val choices: List<C>)
    class Fight(val id: String, val shape: String, val enemy: String)
    class Ch(val id: String, val number: Int, val title: String, val start: String)

    val scenes: List<S> = listOf(
    S("the_signal", "chapter_17", "The Signal", "black_door_beneath_the_tree", "", "b543baf589aa13c0c9ca9334b4af0920fa79021e974222e0a8a0145a17bba486", listOf(
        C("\"How soon?\"", "COURAGE=1", "asked_how_soon", "", "", "", "", "", "", "gathering_what_they_have_asked"),
        C("\"Then we don't wait to find out.\"", "HONOUR=1", "moved_without_waiting", "", "", "", "", "", "", "gathering_what_they_have_moved")
    )),
    S("gathering_what_they_have_asked", "chapter_17", "Gathering What They Have", "gathering_what_they_have", "", "6ecdee03664b678b1e1db0e6a2905ccc5f4796b110268235e69807cff6881e3e", listOf(
        C("Move out.", "", "", "", "", "", "", "", "", "the_road_to_ashfall_again")
    )),
    S("gathering_what_they_have_moved", "chapter_17", "Gathering What They Have", "gathering_what_they_have", "", "605bc6b7a881b0f61d883bb8b360fc78cec5ae21bbe7ac3ca6f9c4253803cd49", listOf(
        C("Move out.", "", "", "", "", "", "", "", "", "the_road_to_ashfall_again")
    )),
    S("the_road_to_ashfall_again", "chapter_17", "The Road to Ashfall, Again", "back_to_the_ash", "", "8432ebc4cef920090e28c9909e2571043974587636bb4289e97bddf33bf2fc0a", listOf(
        C("Get to the chamber.", "", "", "", "", "", "", "", "", "the_chamber_defended")
    )),
    S("the_chamber_defended", "chapter_17", "The Chamber, Defended", "the_chamber_defended", "chamber_guardian_encounter", "bf8496dd65cd77ca4f2dfe457bf5839b4fdd8a033f87d0d0a07f6dd3264611f4", listOf(

    )),
    S("chamber_guardian_defeat", "chapter_17", "It Runs Out of Purpose Before He Runs Out of Time", "the_chamber_defended", "", "25a3157bbcf2f8f74f9edc050e861a0861ef020f9af7bf7c0145b401fbd27563", listOf(
        C("Get to the frame.", "", "lost_to_sanctum_construct", "", "", "", "", "", "", "what_must_be_broken")
    )),
    S("what_must_be_broken", "chapter_17", "What Must Be Broken", "what_must_be_broken", "", "6b98ddf345cb88fa794169d8912874911b02957d202e14d0cebcef2954d8235f", listOf(
        C("\"Tear it apart. Whatever the pieces do loose is better than what six settings does whole.\"", "COURAGE=1", "destroyed_the_frame_outright", "", "twisted_frame_component", "", "", "", "COURAGE=15", "chapter17_end"),
        C("\"Disrupt the settings, don't destroy them. The prisoner was afraid of being unmade — I won't be the one who does it by accident.\"", "HONOUR=1", "disrupted_carefully", "", "twisted_frame_component", "", "", "", "HONOUR=14", "chapter17_end"),
        C("Do whatever stops it fastest, and worry about the difference later.", "", "stopped_it_however_possible", "", "twisted_frame_component", "", "", "", "", "chapter17_end")
    )),
    S("chapter17_end", "chapter_17", "End of Chapter XVII — What Almost Finished", "chapter17_threshold", "", "c14e503b8426c919b73608cfba8c12abb12f97610664a90b6d41d45bd97e4d61", listOf(
        C("Go home.", "", "", "", "", "", "", "", "", "no_more_envoys")
    ))
    )

    val fights: List<Fight> = listOf(
        Fight("chamber_guardian_encounter", "CombatEncounter(id=chamber_guardian_encounter, enemyId=sanctum_construct, victoryNodeId=what_must_be_broken, defeatNodeId=chamber_guardian_defeat, xpReward=500, goldReward=125, unlockTrophy=What Almost Finished, isSurprise=false)", "Enemy(id=sanctum_construct, name=The Chamber's Ward, maxHealth=235, minAttack=21, maxAttack=28, description=Assembled the same desperate way the frame itself was — pieces that were never meant to share a single purpose, forced to anyway.)")
    )

    val chapter: Ch = Ch("chapter_17", 17, "Chapter XVII — What Almost Finished", "the_signal")
}
