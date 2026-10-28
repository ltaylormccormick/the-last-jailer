package com.thelastjailer.app.data

/**
 * GENERATED for the Chapter 18-20 expansion, from the committed sources at cc21d9b (nothing of Chapters 18 to 20 had been touched).
 * The 31 shipped scenes of Chapters 18, 19 and 20: a SHA-256 of each scene's whitespace-collapsed text, its title, picture and fight, and each
 * choice's label, stat changes, flags set and cleared, items, trophy and gates, with where it led. [approvedEdits] holds the SHA-256 the
 * eight scenes carry once the approved wording corrections are made (derived here from the original text, not from the source), and the
 * encounters and items are as they were except where the approved changes say. Used only by Chapter1820ShippedPinTest.
 */
internal object Chapter1820Shipped {
    class C(val label: String, val deltas: String, val set: String, val clear: String, val items: String, val trophy: String,
            val required: String, val forbidden: String, val min: String, val next: String)
    class S(val id: String, val chapter: String, val title: String, val illustration: String, val combat: String, val sha: String, val choices: List<C>)
    class Fight(val id: String, val shape: String, val enemy: String)
    class Item(val id: String, val name: String, val description: String)
    class Ch(val id: String, val number: Int, val title: String, val start: String)

    val scenes: List<S> = listOf(
    S("no_more_envoys", "chapter_18", "No More Envoys", "no_more_envoys", "", "737ac9bd2ce16d9d4c988888764b8b4e89c859d29af0f0ebc5ca17c8381f28bd", listOf(
        C("\"Then let's make it.\"", "COURAGE=1", "accepted_the_duel_readily", "", "", "", "", "", "", "her_own_terms_readily"),
        C("\"Say what you actually came to say first.\"", "HONOUR=1", "made_her_say_it_first", "", "", "", "", "", "", "her_own_terms_pressed")
    )),
    S("her_own_terms_readily", "chapter_18", "Her Own Terms", "her_own_terms", "", "db3dbc71177d25ee049c6965a16c6837cfba89c92707712e9b8fa7e9f96029fe", listOf(
        C("Accept.", "", "", "", "", "", "", "", "", "what_kaelen_says_first")
    )),
    S("her_own_terms_pressed", "chapter_18", "Her Own Terms", "her_own_terms", "", "fc4af92312d13c0850cba8666e20f979e0ac156c4f5d80d87f9ae02d088ee921", listOf(
        C("Accept.", "", "", "", "", "", "", "", "", "what_kaelen_says_first")
    )),
    S("what_kaelen_says_first", "chapter_18", "What Kaelen Says First", "what_kaelen_says_first", "", "5817744387094b70772fe5f6cb5a48ae8f7ed8d613c1f73ec0a170dc6393c4db", listOf(
        C("\"I'm not doing this because I hate you.\"", "", "spoke_before_the_duel", "", "", "", "", "", "", "the_duel_spoke"),
        C("Say nothing. Draw your sword instead.", "", "answered_with_silence", "", "", "", "", "", "", "the_duel_silent")
    )),
    S("the_duel_spoke", "chapter_18", "The Duel", "the_duel", "ilsevet_duel_encounter", "1e8abea373a48cd2c116cc08428dd77861b6236a0b3e6b5991f963f9d3557961", listOf(

    )),
    S("the_duel_silent", "chapter_18", "The Duel", "the_duel", "ilsevet_duel_encounter", "ff7a108309f85ced2487acad12abeb719368d886a1ae98f5efecfbb84685a4ea", listOf(

    )),
    S("ilsevet_duel_defeat", "chapter_18", "She Doesn't Take It", "the_duel", "", "75b03653bb294cdbb516616dda31aba3d7de2df3d2606c50d2d7971682fad23b", listOf(
        C("Get up.", "", "lost_to_ilsevet_the_cinder_marshal", "", "", "", "", "", "", "what_is_left_of_her")
    )),
    S("what_is_left_of_her", "chapter_18", "What Is Left of Her", "what_is_left_of_her", "", "4f427561e77ebef353f9b108b525a7232f71922232c5b4b236c6cd46c3fc603b", listOf(
        C("Decide.", "", "", "", "", "", "trusted_voss_fully", "", "", "what_kaelen_chooses_trusted"),
        C("Decide.", "", "", "", "", "", "", "trusted_voss_fully", "", "what_kaelen_chooses_guarded")
    )),
    S("what_kaelen_chooses_trusted", "chapter_18", "What Kaelen Chooses", "what_kaelen_chooses", "", "2956723e44f927738edf41e7c33a6bd49a508fce484de782118cf5fe5ddfecd9", listOf(
        C("\"I'm not going to kill you. But you're done deciding what happens to six lives that aren't yours to spend.\" Bind her and bring her back to face what's left of the Order.", "COURAGE=1", "captured_ilsevet", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "COURAGE=16", "chapter18_end"),
        C("\"Ashwell already took enough from you. I won't let this take the rest.\" Let her go, on the condition she never comes back armed.", "HONOUR=1", "showed_ilsevet_mercy", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "HONOUR=15", "chapter18_end"),
        C("Take her weapon, and decide the rest later. There's no clean answer waiting right now.", "", "deferred_ilsevets_fate", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "", "chapter18_end")
    )),
    S("what_kaelen_chooses_guarded", "chapter_18", "What Kaelen Chooses", "what_kaelen_chooses", "", "43882f780764f7ea04ef587037cc2669837e8c4b2ef39a478500ab520930ab32", listOf(
        C("\"I'm not going to kill you. But you're done deciding what happens to six lives that aren't yours to spend.\" Bind her and bring her back to face what's left of the Order.", "COURAGE=1", "captured_ilsevet", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "COURAGE=16", "chapter18_end"),
        C("\"Ashwell already took enough from you. I won't let this take the rest.\" Let her go, on the condition she never comes back armed.", "HONOUR=1", "showed_ilsevet_mercy", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "HONOUR=15", "chapter18_end"),
        C("Take her weapon, and decide the rest later. There's no clean answer waiting right now.", "", "deferred_ilsevets_fate", "captured_ilsevet,deferred_ilsevets_fate,showed_ilsevet_mercy", "ilsevets_blade", "", "", "", "", "chapter18_end")
    )),
    S("chapter18_end", "chapter_18", "End of Chapter XVIII — No More Between Us", "chapter18_threshold", "", "085002a1459ef91cfd2b6d08e805c98b68653dce7313452efa74bf49ccaca0d3", listOf(
        C("Rest, while it lasts.", "", "", "", "", "", "", "", "", "what_ilsevet_knew")
    )),
    S("what_ilsevet_knew", "chapter_19", "What Ilsevet Knew", "what_ilsevet_knew", "", "bde8b4faa90aeca0b42c6f28ffbb44aeb487416573f4732442410a58afa19b1f", listOf(
        C("\"Who drew them, if not you?\"", "COURAGE=1", "pressed_about_the_archive", "", "", "", "", "", "", "the_dead_archivist_pressed"),
        C("Let her finish in her own order.", "HONOUR=1", "let_her_finish", "", "", "", "", "", "", "the_dead_archivist_patient")
    )),
    S("the_dead_archivist_pressed", "chapter_19", "The Dead Archivist", "the_dead_archivist", "", "79339f37c02e9d635c4d9527f0d793ae2c0b2cbc67f0b50734fe0de0e7d0b7d2", listOf(
        C("Bring this to the prisoner.", "", "", "", "", "", "", "", "", "the_prisoners_reaction")
    )),
    S("the_dead_archivist_patient", "chapter_19", "The Dead Archivist", "the_dead_archivist", "", "cde2c733ac77d0825df5db82d71a12ff44d65e5da671eda958ee2b61b2c42b62", listOf(
        C("Bring this to the prisoner.", "", "", "", "", "", "", "", "", "the_prisoners_reaction")
    )),
    S("the_prisoners_reaction", "chapter_19", "The Prisoner's Reaction", "the_prisoners_reaction", "", "05b1a47df5a470a804f63c2991ad1bdb51aa480280c816efec10df5bb4bf9b7c", listOf(
        C("Send word to Selvane.", "", "", "", "", "", "", "", "", "what_selvane_finds")
    )),
    S("what_selvane_finds", "chapter_19", "What Selvane Finds", "what_selvane_finds", "", "96e93240d08a7ae68a7bbb3ab0ba5b5c6e531a767cea867df83486d50694efdb", listOf(
        C("Read the names.", "", "", "", "archivists_frame_notes", "", "", "", "", "the_addressed_names")
    )),
    S("the_addressed_names", "chapter_19", "The Addressed Names", "the_addressed_names", "", "68384b552af85da1468b3f214322541d5afeedd74d3a6f23df51557e989c2a07", listOf(
        C("\"Then whoever it is, they've been waiting on me specifically. Let them keep waiting.\"", "COURAGE=1", "defied_the_prophecy", "", "", "", "", "", "COURAGE=17", "chapter19_end_defied"),
        C("\"If I was always meant to stand here, I intend to make sure I'm the one who decides what that means.\"", "HONOUR=1", "claimed_his_own_meaning", "", "", "", "", "", "HONOUR=16", "chapter19_end_claimed"),
        C("Say nothing. There isn't an answer that fits in a sentence.", "", "left_it_unanswered", "", "", "", "", "", "", "chapter19_end_unanswered")
    )),
    S("chapter19_end_defied", "chapter_19", "End of Chapter XIX — Already Written", "chapter19_threshold", "", "3a4e9fbc22c774a3ade8f953927010ee6d7c06763bdd09de66ad4f5d8dc31552", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_workshop")
    )),
    S("chapter19_end_claimed", "chapter_19", "End of Chapter XIX — Already Written", "chapter19_threshold", "", "b5f8c20a0f48ea3e8ba58d4c89453a7af33894716f6e4f90e3e767cbc773b9c3", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_workshop")
    )),
    S("chapter19_end_unanswered", "chapter_19", "End of Chapter XIX — Already Written", "chapter19_threshold", "", "84685c8bfb801e64c2791b93574c5eabb3798557886a76157659dd42f4f53277", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_workshop")
    )),
    S("the_workshop", "chapter_20", "The Workshop", "black_door_beneath_the_tree", "", "ffec6f0ed0906b9741074fa781eea66c810fdcc09cca091cd5cac100c8e9eafd", listOf(
        C("\"I'll go alone. This deserves a direct look, not a delegation.\"", "COURAGE=1", "went_to_workshop_alone", "", "", "", "", "", "", "the_road_to_the_workshop_alone"),
        C("\"Voss comes with me. Two sets of eyes, not one.\"", "HONOUR=1", "brought_voss_to_workshop", "", "", "", "", "", "", "the_road_to_the_workshop_with_voss")
    )),
    S("the_road_to_the_workshop_alone", "chapter_20", "The Road to the Workshop", "the_road_to_the_workshop", "", "595fd01d9c9fc461c7217aa849e71a17fa841764d457a9287744cc1bdf1460a8", listOf(
        C("Go inside.", "", "", "", "", "", "", "", "", "what_the_workshop_holds")
    )),
    S("the_road_to_the_workshop_with_voss", "chapter_20", "The Road to the Workshop", "the_road_to_the_workshop", "", "af949ed10eb4b4bfa6b10c1570f57f4f97aade49a538c16a12a838481e47fa08", listOf(
        C("Go inside.", "", "", "", "", "", "", "", "", "what_the_workshop_holds")
    )),
    S("what_the_workshop_holds", "chapter_20", "What the Workshop Holds", "what_the_workshop_holds", "", "95ccaf308f000868711ead79fc0c101e523d45335217c5d2cedc3fc15ce109d8", listOf(
        C("Keep reading.", "", "", "", "", "", "", "", "", "the_workshops_own_guardian")
    )),
    S("the_workshops_own_guardian", "chapter_20", "The Workshop's Own Guardian", "the_workshops_own_guardian", "ghostwriter_encounter", "615864d498aff86710f5d74b4de0b1eeb52a076d80568255c3f16ad180d987cb", listOf(

    )),
    S("ghostwriter_defeat", "chapter_20", "It Wears Itself Thin", "the_workshops_own_guardian", "", "db55aaf040526e02dc2ffbd4681102f0bfd3e1873ff18e31602d23e0c00ee650", listOf(
        C("Check the workbench.", "", "lost_to_the_ghostwriter", "", "", "", "", "", "", "the_last_page")
    )),
    S("the_last_page", "chapter_20", "The Last Page", "the_last_page", "", "6ec3a069ec070985803bdebaf7e2d6d6e9420e30df1e981fbc0c93376a517354", listOf(
        C("Take the page, and go home.", "", "", "", "archivists_final_page", "", "", "", "", "what_the_prisoner_says_now")
    )),
    S("what_the_prisoner_says_now", "chapter_20", "What the Prisoner Says Now", "black_door_beneath_the_tree", "", "ebb17c50846913f7d633f4c6591f769c263f791a12cb737301775d23634480ed", listOf(
        C("\"Who is it? Tell me plainly.\"", "COURAGE=1", "demanded_the_truth", "", "", "", "", "", "COURAGE=18", "chapter20_end_demanded"),
        C("\"Take whatever time you need. I'm not going anywhere.\"", "HONOUR=1", "gave_the_prisoner_time", "", "", "", "", "", "HONOUR=17", "chapter20_end_gave_time"),
        C("Wait, and let it decide whether to say more.", "", "waited_in_silence", "", "", "", "", "", "", "chapter20_end_waited")
    )),
    S("chapter20_end_demanded", "chapter_20", "End of Chapter XX — Whoever Finished It", "chapter20_threshold", "", "69eaf18c0b2acae8a0e8a5e018d7bbe75f1840b67f0ce6315fad5ef463b42800", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_prisoner_answers")
    )),
    S("chapter20_end_gave_time", "chapter_20", "End of Chapter XX — Whoever Finished It", "chapter20_threshold", "", "8af89ac350746e0836f133edd22b2e32cf5f0e34f9ffb792d422ccb4888ad461", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_prisoner_answers")
    )),
    S("chapter20_end_waited", "chapter_20", "End of Chapter XX — Whoever Finished It", "chapter20_threshold", "", "576469dde5bf952c76abc9eb1cfa7e08147d5e3b8d90ac304be5d5d89ff2f2b6", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_prisoner_answers")
    ))
    )

    val approvedEdits: Map<String, String> = mapOf(
        "no_more_envoys" to "a5598a6cd704ce4725b6b2b782d47ee4593174767222c02f634e59e35a914f4f",
        "what_kaelen_says_first" to "c7dc7b236ae2a7a87f0cc58e3b25d18f3e168cd877b8801aaa7c4a168e96ccc5",
        "what_selvane_finds" to "93eec838b8a0c0f8175e953a0c3bd02732619f7eea0b84331ef8c8a1832e6a68",
        "the_workshop" to "f3e0716538d44f068b0f37fb6310ca831622f3be6a02738d38cbb7b02acbb48d",
        "the_last_page" to "f760660ba4294e792e4d792476bc60f8113be35293dee4423db76de4a3f79c1d",
        "chapter20_end_demanded" to "6dc3b9ebf1cb2cbe295e3072682799bcb68abf0c12772101b84114318667efe4",
        "chapter20_end_gave_time" to "cd8f95a8bdc0e5452630e89fd49a3502734706a5f6437814414a9b8dd5ff5254",
        "chapter20_end_waited" to "613cd99b903fdd1c23325709b61662f922c943fa452103c208cd402a6636ae15"
    )

    val fights: List<Fight> = listOf(
        Fight("ilsevet_duel_encounter", "CombatEncounter(id=ilsevet_duel_encounter, enemyId=ilsevet_the_cinder_marshal, victoryNodeId=what_is_left_of_her, defeatNodeId=ilsevet_duel_defeat, xpReward=600, goldReward=150, unlockTrophy=No More Between Us, isSurprise=true)", "Enemy(id=ilsevet_the_cinder_marshal, name=Ilsevet, the Cinder Marshal, maxHealth=260, minAttack=22, maxAttack=30, description=No garrison behind her this time, no frame to finish, nothing left to spend but herself. She fights like someone who has already decided this is the last version of this argument she intends to have.)"),
        Fight("ghostwriter_encounter", "CombatEncounter(id=ghostwriter_encounter, enemyId=the_ghostwriter, victoryNodeId=c20_after_guardian, defeatNodeId=ghostwriter_defeat, xpReward=650, goldReward=160, unlockTrophy=Whoever Finished It, isSurprise=false)", "Enemy(id=the_ghostwriter, name=The Ghostwriter, maxHealth=280, minAttack=23, maxAttack=31, description=No clean shape, just pressure and intent — something that finished another man's work rather than write its own, and clearly intends to keep doing exactly that.)")
    )

    val items: List<Item> = listOf(
        Item("ilsevets_blade", "Ilsevet's Blade", "Plain, unceremonial, exactly the kind of weapon someone carries when they've stopped caring about anything but whether it works. Taken, not surrendered."),
        Item("archivists_frame_notes", "Archivist's Frame Notes", "Five sets of plans in the same careful hand, addressed to people who hadn't yet taken up the roles the pages gave them when the ink dried. The fifth is addressed to Kaelen."),
        Item("archivists_final_page", "Archivist's Final Page", "One line, in a hand gone suddenly uncertain: a message meant for \"the sixth,\" whoever that turns out to be.")
    )

    val chapters: List<Ch> = listOf(
        Ch("chapter_18", 18, "Chapter XVIII — No More Between Us", "no_more_envoys"),
        Ch("chapter_19", 19, "Chapter XIX — Already Written", "what_ilsevet_knew"),
        Ch("chapter_20", 20, "Chapter XX — Whoever Finished It", "the_workshop")
    )
}
