package com.thelastjailer.app.data

/**
 * GENERATED at M0 of the Chapters 13 to 16 package, from the sources at 6f1d69c (nothing of Chapters 13 to 16 had been touched).
 * The shipped scenes of Chapters 13 to 16: a SHA-256 of each scene's whitespace-collapsed text, its title, picture and fight, and
 * each choice's label, stat changes, flags set and cleared, items, trophy and gates, with where it led. Used only by Chapter1316ShippedPinTest.
 */
internal object Chapter1316Shipped {
    class C(val label: String, val deltas: String, val set: String, val clear: String, val items: String, val trophy: String,
            val required: String, val forbidden: String, val min: String, val next: String)
    class S(val id: String, val chapter: String, val title: String, val illustration: String, val combat: String, val sha: String, val choices: List<C>)
    class Fight(val id: String, val shape: String, val enemy: String)
    class Ch(val id: String, val number: Int, val title: String, val start: String)

    val scenes: List<S> = listOf(
    S("the_orders_delegation_trusted", "chapter_13", "The Order's Delegation", "the_orders_delegation", "", "e481382300eb0cc7ce3da773e34392ad494c5f59d4afe2bbbab729dd0a1e4869", listOf(
        C("\"Then we meet them together.\"", "COURAGE=1", "stood_with_voss", "", "", "", "", "", "", "the_inquisitor_general_stood"),
        C("\"This one might go better if I'm not standing in the doorway.\"", "HONOUR=1", "let_voss_go_alone", "", "", "", "", "", "", "the_inquisitor_general_alone")
    )),
    S("the_orders_delegation_guarded", "chapter_13", "The Order's Delegation", "the_orders_delegation", "", "6df62848b2d82d5b6ac527bd0ce9e6b959da54ad0cada9b06fb16848acc4f18f", listOf(
        C("\"Then we meet them together.\"", "COURAGE=1", "stood_with_voss", "", "", "", "", "", "", "the_inquisitor_general_stood"),
        C("\"This one might go better if I'm not standing in the doorway.\"", "HONOUR=1", "let_voss_go_alone", "", "", "", "", "", "", "the_inquisitor_general_alone")
    )),
    S("the_inquisitor_general_stood", "chapter_13", "The Inquisitor-General", "the_inquisitor_general", "", "6061498b41015e816462d1772a438f68de7a2c49f2db3712c438e516e423769a", listOf(
        C("Let Voss answer.", "", "", "", "", "", "", "", "", "what_voss_answers")
    )),
    S("the_inquisitor_general_alone", "chapter_13", "The Inquisitor-General", "the_inquisitor_general", "", "c104e93defa5055bfad21d2deb944532263e4723da86212b5378f0b87565454d", listOf(
        C("Let Voss answer.", "", "", "", "", "", "", "", "", "what_voss_answers")
    )),
    S("what_voss_answers", "chapter_13", "What Voss Answers", "what_voss_answers", "", "95959af228511a670e64d17bd8b1026b88643b9b01c5dc0159178c95e6d88db4", listOf(
        C("\"I'll vouch for that myself.\"", "HONOUR=1", "kaelen_vouched_for_voss", "", "", "", "stood_with_voss", "", "", "selvanes_verdict"),
        C("Wait outside, and trust her account.", "", "trusted_voss_account", "", "", "", "let_voss_go_alone", "", "", "selvanes_verdict")
    )),
    S("selvanes_verdict", "chapter_13", "Selvane's Verdict", "selvanes_verdict", "", "dda73522dce1cadfcd54d85054337b0b80511e01374ff7862cf4491fee9746b6", listOf(
        C("Let them ride out.", "", "", "", "", "", "", "", "", "halvards_rites")
    )),
    S("halvards_rites", "chapter_13", "Halvard's Rites", "halvards_proper_rites", "", "bf9bf420d2172e33f2760c4018835a1750bc47a1703e0888c11fc8fd7ed1ad37", listOf(
        C("Take a rubbing of the mark before you leave.", "", "", "", "rubbing_of_halvards_mark", "", "", "", "", "who_stands_with_stonebeard")
    )),
    S("who_stands_with_stonebeard", "chapter_13", "Who Stands With Stonebeard", "who_stands_with_stonebeard", "", "afc356cb0c9c70b56bb20a7a4032249d1ce7fc13ceb75b21db5ffb37c0a386a9", listOf(
        C("\"Anyone willing to hold a gate can be taught to hold it well. Start training them.\"", "COURAGE=1", "began_training_new_wardens", "", "", "", "", "", "COURAGE=11", "chapter13_end_training"),
        C("\"The chain isn't mine to hand out. But I'll stand watch beside anyone who picks it up.\"", "HONOUR=1", "stood_watch_rather_than_delegate", "", "", "", "", "", "HONOUR=10", "chapter13_end_watch"),
        C("\"One gate, one jailer, for now. There will be time to decide the rest later.\"", "", "deferred_the_question", "", "", "", "", "", "", "chapter13_end_deferred")
    )),
    S("chapter13_end_training", "chapter_13", "End of Chapter XIII — What the Order Allows", "chapter13_threshold", "", "c48244499577c20c31b4dd1d95157760a629b8e64e82a15e037a1db1a0fb441a", listOf(
        C("Listen.", "", "", "", "", "", "", "", "", "what_waits_speaks")
    )),
    S("chapter13_end_watch", "chapter_13", "End of Chapter XIII — What the Order Allows", "chapter13_threshold", "", "c66cfc509b64c2904d1549bbe6c6ae16120430b39c058101d48db566901fd8a2", listOf(
        C("Listen.", "", "", "", "", "", "", "", "", "what_waits_speaks")
    )),
    S("chapter13_end_deferred", "chapter_13", "End of Chapter XIII — What the Order Allows", "chapter13_threshold", "", "3a7d69f59136388aee605c76ef3bf5e9a9aa38020e7a6486cfbe08ad764efe98", listOf(
        C("Listen.", "", "", "", "", "", "", "", "", "what_waits_speaks")
    )),
    S("what_waits_speaks", "chapter_14", "What Waits Speaks", "black_door_beneath_the_tree", "", "0d0ff5df7e6d900cd3fd311199b86915941ca8ce797007d66db8cba79e33a0b7", listOf(
        C("\"What happens at six?\"", "COURAGE=1", "asked_the_prisoner_directly", "", "", "", "", "", "", "what_it_says_asked"),
        C("Say nothing. Listen instead.", "HONOUR=1", "listened_without_asking", "", "", "", "", "", "", "what_it_says_listened")
    )),
    S("what_it_says_asked", "chapter_14", "What It Says", "what_it_says", "", "1360a4dae163d2a52ee338883c5a025fd76e60378e793724533226f43b443581", listOf(
        C("Press further.", "", "", "", "", "", "", "", "", "the_names_it_wont_give")
    )),
    S("what_it_says_listened", "chapter_14", "What It Says", "what_it_says", "", "27080d2190a6b89a5031f4e6264a7e8137cb42015ef2143136837f5e6dc20726", listOf(
        C("Press further.", "", "", "", "", "", "", "", "", "the_names_it_wont_give")
    )),
    S("the_names_it_wont_give", "chapter_14", "The Names It Won't Give", "the_names_it_wont_give", "", "fcae324a3baf9c12db70f946fc438743b2bddb05ce5e757092125798d1dc6dbe", listOf(
        C("\"Show me, then.\"", "", "", "", "", "", "", "", "", "the_offered_glimpse")
    )),
    S("the_offered_glimpse", "chapter_14", "The Offered Glimpse", "the_offered_glimpse", "", "2f865f6692686035817693f660e62e7182561acfdc22b880d1c0cf3bb8f23a3b", listOf(
        C("Let it in.", "", "", "", "", "", "", "", "", "inside_the_memory")
    )),
    S("inside_the_memory", "chapter_14", "Inside the Memory", "inside_the_memory", "memory_confrontation_encounter", "596ea5ccfd745a25193100f5bfb1dd2370fb6befd8fddc5b21d489a41a2b20e5", listOf(

    )),
    S("memory_confrontation_defeat", "chapter_14", "The Memory Wins This Round", "inside_the_memory", "", "5f74500ea1bfe2ed1ad474d8884a4050aea6a25777e20e9231276a31d80e8fac", listOf(
        C("Try to make sense of what you saw.", "", "lost_to_the_memory_itself", "", "", "", "", "", "", "what_kaelen_remembers")
    )),
    S("what_kaelen_remembers", "chapter_14", "What Kaelen Remembers", "what_kaelen_remembers", "", "3b2899974761f67bc5b4d4bf535948345f781a7873e58b162ab881b8a31f2863", listOf(
        C("Tell Voss everything, tonight, whatever it costs to say out loud.", "COURAGE=1", "told_voss_everything", "", "shard_of_the_first_ward", "", "", "", "COURAGE=12", "chapter14_end"),
        C("Write it down instead. Some truths need to be chosen, not sprung on someone.", "HONOUR=1", "recorded_the_vision_first", "", "shard_of_the_first_ward", "", "", "", "HONOUR=11", "chapter14_end"),
        C("Sit with it alone a while longer.", "", "kept_the_vision_to_himself", "", "shard_of_the_first_ward", "", "", "", "", "chapter14_end")
    )),
    S("chapter14_end", "chapter_14", "End of Chapter XIV — Furniture", "chapter14_threshold", "", "55419e21a67a7de4b0dbdeb1947610a964f2f2742981cf505af65f8a736e6a0a", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "what_selvane_sends")
    )),
    S("what_selvane_sends", "chapter_15", "What Selvane Sends", "what_selvane_sends", "", "26b50108000e1437f85199971c0adaff98b1e4c7fa959d2392ecb9f7ffad8ee2", listOf(
        C("Read it alone first.", "HONOUR=1", "read_alone_first", "", "", "", "", "", "", "the_woman_before_ilsevet_alone"),
        C("Read it with Voss.", "COURAGE=1", "read_with_voss", "", "", "", "", "", "", "the_woman_before_ilsevet_with_voss")
    )),
    S("the_woman_before_ilsevet_alone", "chapter_15", "The Woman Before Ilsevet", "the_woman_before_ilsevet", "", "f68bb1297f51370fbc7999873c2e398b23f26a3e864c7e6a68a68fa661b1ec19", listOf(
        C("Keep reading.", "", "", "", "record_of_ashwell", "", "", "", "", "what_it_made_of_her")
    )),
    S("the_woman_before_ilsevet_with_voss", "chapter_15", "The Woman Before Ilsevet", "the_woman_before_ilsevet", "", "787b87b27611061781f023224bba5fbd55a19c0f059c375052334ff3acca5995", listOf(
        C("Keep reading.", "", "", "", "record_of_ashwell", "", "", "", "", "what_it_made_of_her")
    )),
    S("what_it_made_of_her", "chapter_15", "What It Made of Her", "what_it_made_of_her", "", "c0e2f5d728bb7cacb5ff19dc584edc6536b3db163f9de65cff5dca473f00d04e", listOf(
        C("Sit with that.", "", "", "", "", "", "", "", "", "old_debts")
    )),
    S("old_debts", "chapter_15", "Old Debts", "old_debts", "", "b6f03868906c036ad48e9d43e8e0cbdbc5d8c9ecade3ae7cce9bc4565da18a01", listOf(
        C("\"Understanding her doesn't mean stopping her matters any less.\"", "COURAGE=1", "resolved_despite_sympathy", "", "", "", "", "", "COURAGE=13", "the_reprisal_resolved"),
        C("\"If eternal watching really can't be trusted forever, we owe it to everyone to prove her wrong instead of just outlasting her.\"", "HONOUR=1", "committed_to_proving_her_wrong", "", "", "", "", "", "HONOUR=12", "the_reprisal_committed"),
        C("Set the question aside. There isn't time to resolve it tonight.", "", "deferred_the_question_of_ilsevet", "", "", "", "", "", "", "the_reprisal_deferred")
    )),
    S("the_reprisal_resolved", "chapter_15", "The Reprisal", "the_reprisal", "reprisal_squad_encounter", "7a1a6f232f350447a2c9dc6ea0dec196bb91a30dc2641595178d1e1c84d34ccf", listOf(

    )),
    S("the_reprisal_committed", "chapter_15", "The Reprisal", "the_reprisal", "reprisal_squad_encounter", "69b83b46660f3a60065cb5661a955e1c5d109523da377ea38e88bbd75af46ec0", listOf(

    )),
    S("the_reprisal_deferred", "chapter_15", "The Reprisal", "the_reprisal", "reprisal_squad_encounter", "c9dc127798ebc344a21da8979a3ffc4faff91b82aa9247a7de9c99f16a9b4294", listOf(

    )),
    S("reprisal_squad_defeat", "chapter_15", "She Held It Alone", "the_reprisal", "", "2860466657c4fac8278431f4f380b34a9243694d733cb58b0a220eacc14164c1", listOf(
        C("Get up and tend the cut she won't mention.", "", "lost_to_cinder_reprisal_leader", "", "", "", "", "", "", "after_the_reprisal")
    )),
    S("after_the_reprisal", "chapter_15", "After the Reprisal", "after_the_reprisal", "", "3624e2227d8cbf521a5e9d4a2253a0c1d952f074e1721a955e2ad7e0e219d3b8", listOf(
        C("Tend the cut, and let her finish being angry.", "", "", "", "", "", "", "", "", "chapter15_end")
    )),
    S("chapter15_end", "chapter_15", "End of Chapter XV — Old Debts", "chapter15_threshold", "", "ba3571731ddb3bacea104769f234a9eef0520b5b0bf93a2eb8794db72f4aac68", listOf(
        C("Prepare anyway.", "", "", "", "", "", "", "", "", "what_kaelen_isnt_prepared_for")
    )),
    S("what_kaelen_isnt_prepared_for", "chapter_16", "What Kaelen Isn't Prepared For", "black_door_beneath_the_tree", "", "d8a697a070f6a096e85ba37a5ffd4ca613ffbfbe57dea5f3d79d62fee1a750ef", listOf(
        C("\"What does that mean?\"", "COURAGE=1", "pressed_the_prisoner_for_detail", "", "", "", "", "", "", "the_prisoners_warning_pressed"),
        C("Let it explain in its own time.", "HONOUR=1", "let_the_prisoner_explain", "", "", "", "", "", "", "the_prisoners_warning_patient")
    )),
    S("the_prisoners_warning_pressed", "chapter_16", "The Prisoner's Warning", "the_prisoners_warning", "", "19b402bf8aff0b36f4459bc10483bff98b50717923e39f0056cf8179f88fd841", listOf(
        C("Take stock of what you're carrying.", "", "", "", "", "", "", "", "", "what_to_protect")
    )),
    S("the_prisoners_warning_patient", "chapter_16", "The Prisoner's Warning", "the_prisoners_warning", "", "a9d8a35ff1a6ef1bcef1a7d214543bfa837cb2991ffc078ec41a8b06e7b97356", listOf(
        C("Take stock of what you're carrying.", "", "", "", "", "", "", "", "", "what_to_protect")
    )),
    S("what_to_protect", "chapter_16", "What to Protect", "what_to_protect", "", "70915444cf44668c24501833e6c41bc1bdd86df2bb20df1d62a2b2b252309639", listOf(
        C("\"Keep them on me. If someone wants them, they'll have to go through me directly.\"", "COURAGE=1", "kept_shards_close", "", "", "", "", "", "COURAGE=14", "the_reliquary_thief_close"),
        C("\"Split them between Stonebeard, Fenmoor, and Voss. No single loss ends this.\"", "HONOUR=1", "dispersed_the_shards", "", "", "", "", "", "HONOUR=13", "the_reliquary_thief_dispersed"),
        C("There isn't time to do anything but keep watch and hope it's enough.", "", "trusted_to_luck", "", "", "", "", "", "", "the_reliquary_thief_luck")
    )),
    S("the_reliquary_thief_close", "chapter_16", "The Reliquary Thief", "the_reliquary_thief", "reliquary_thief_encounter", "c4768c8b8259a2259e9f7ac1931c3400bab22ed661e991f2c52edd3077b1fdaa", listOf(

    )),
    S("the_reliquary_thief_dispersed", "chapter_16", "The Reliquary Thief", "the_reliquary_thief", "reliquary_thief_encounter", "b254c34dae9677d52794d277f37a504090ef2f610e3c9ceb0554bd37b9cf23df", listOf(

    )),
    S("the_reliquary_thief_luck", "chapter_16", "The Reliquary Thief", "the_reliquary_thief", "reliquary_thief_encounter", "e57c9e7311fd405d6f1c45ac340a77c33c2b11d5751cfb40f4ac271f6eae9cf2", listOf(

    )),
    S("reliquary_thief_defeat", "chapter_16", "Outmatched, Not Outnumbered", "the_reliquary_thief", "", "6fc711054b3cf3092ec60f7ce661b7a21dd55898fa66a22bfa012ac610d0296c", listOf(
        C("Take stock of the damage.", "", "lost_to_cinder_reliquary_thief", "", "", "", "", "", "", "what_was_taken")
    )),
    S("what_was_taken", "chapter_16", "What Was Taken", "what_was_taken", "", "0a985bc04f13f1497927d17059b1a2c9c3b3ef98fe2fe66fa86f75bf17034cb9", listOf(
        C("Take stock of the damage.", "", "", "", "", "", "", "", "", "chapter16_end")
    )),
    S("chapter16_end", "chapter_16", "End of Chapter XVI — What Was Already Taken", "chapter16_threshold", "", "bffad00e6c2f7eb1fc5486afe36b61cea5b73994786e59b2372ee9602dfeecf3", listOf(
        C("Go on.", "", "", "", "", "", "", "", "", "the_signal")
    ))
    )

    val fights: List<Fight> = listOf(
        Fight("memory_confrontation_encounter", "CombatEncounter(id=memory_confrontation_encounter, enemyId=the_memory_itself, victoryNodeId=what_kaelen_remembers, defeatNodeId=memory_confrontation_defeat, xpReward=370, goldReward=95, unlockTrophy=What the Stone Remembers, isSurprise=false)", "Enemy(id=the_memory_itself, name=The Memory Itself, maxHealth=190, minAttack=18, maxAttack=25, description=Not flesh, not quite ghost — the shape of a moment three centuries old, defending itself the only way memory knows how.)"),
        Fight("reprisal_squad_encounter", "CombatEncounter(id=reprisal_squad_encounter, enemyId=cinder_reprisal_leader, victoryNodeId=after_the_reprisal, defeatNodeId=reprisal_squad_defeat, xpReward=410, goldReward=105, unlockTrophy=Not Today, isSurprise=false)", "Enemy(id=cinder_reprisal_leader, name=Cinder Reprisal Squad Leader, maxHealth=205, minAttack=19, maxAttack=26, description=Sent for Voss specifically — a debt Ilsevet apparently considers overdue, and entirely separate from anything to do with the gates.)"),
        Fight("reliquary_thief_encounter", "CombatEncounter(id=reliquary_thief_encounter, enemyId=cinder_reliquary_thief, victoryNodeId=what_was_taken, defeatNodeId=reliquary_thief_defeat, xpReward=450, goldReward=115, unlockTrophy=Nearly Enough, isSurprise=false)", "Enemy(id=cinder_reliquary_thief, name=Cinder Reliquary Thief, maxHealth=220, minAttack=20, maxAttack=27, description=Moves like something that was never meant to be caught, sent for exactly the kind of theft a siege could never manage.)")
    )

    val chapters: List<Ch> = listOf(
        Ch("chapter_13", 13, "Chapter XIII — What the Order Allows", "the_orders_delegation_guarded"),
        Ch("chapter_14", 14, "Chapter XIV — Furniture", "what_waits_speaks"),
        Ch("chapter_15", 15, "Chapter XV — Old Debts", "what_selvane_sends"),
        Ch("chapter_16", 16, "Chapter XVI — What Was Already Taken", "what_kaelen_isnt_prepared_for")
    )
}
