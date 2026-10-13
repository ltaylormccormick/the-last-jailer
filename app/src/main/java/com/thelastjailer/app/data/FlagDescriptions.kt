package com.thelastjailer.app.data

/**
 * The Journal is Kaelen's own record of the road, not a dump of the engine's memory. Each story
 * flag is either given a short first-person line here, or is explicitly *hidden* with a reason.
 * Flags the game does not know about (an old save, a removed flag) are never shown, and a raw flag
 * identifier is never shown to the player.
 *
 * Nothing here changes what is saved: flags are still stored as the same strings, and the Journal
 * only reads them. Every flag the story sets must be classified (see FlagDescriptionsTest), so a
 * new flag cannot silently appear in - or silently be missing from - the Journal.
 */
enum class JournalCategory(val title: String) {
    DECISIONS("Decisions and consequences"),
    PEOPLE("People and relationships"),
    DISCOVERIES("Discoveries and secrets"),
    PROMISES("Promises and responsibilities")
}

/** Why a flag never appears in the Journal. */
enum class HiddenReason {
    /** Engine bookkeeping (for example, which fight was lost). */
    BOOKKEEPING,
    /** A passing tone or dialogue beat that does not change the story the player would recall. */
    TONE_ONLY,
    /** Set together with a flag that already tells the same story. */
    REDUNDANT,
    /** Would describe a scene whose canon is still awaiting a narrative decision. */
    PENDING_NARRATIVE_DECISION,
    /** Internal state for the planned Chapter 1 rework; the visible route lines carry the story. */
    PLANNED_INTERNAL
}

/**
 * One Journal line. Entries sharing a [group] are alternatives for the same moment: if more than
 * one is present (possible only after a replay, which keeps old flags), the Journal shows a single
 * neutral line instead of contradicting itself.
 */
data class JournalEntry(
    val flag: String,
    val category: JournalCategory,
    val text: String,
    val group: String? = null
)

data class JournalSection(val category: JournalCategory, val lines: List<String>)

object FlagDescriptions {

    /** In story order; a section lists its lines in this order. */
    val entries: List<JournalEntry> = listOf(
        JournalEntry("opened_black_door", JournalCategory.DECISIONS, "I opened the black door beneath the tree.", "door"),
        JournalEntry("drew_sword", JournalCategory.DECISIONS, "I drew my sword instead of opening the door.", "door"),
        JournalEntry("walked_away", JournalCategory.DECISIONS, "I walked away from the black door.", "door"),
        JournalEntry("hunters_saw_door", JournalCategory.DISCOVERIES, "A rider with a red cord wound round his left hand watched me go through the black door.", null),
        JournalEntry("kept_the_token", JournalCategory.DISCOVERIES, "I kept the tarnished King's Guard clasp I found in the roots.", "clasp"),
        JournalEntry("buried_the_past", JournalCategory.DISCOVERIES, "I left a tarnished King's Guard clasp where I found it, in the roots.", "clasp"),
        JournalEntry("helped_dwarf", JournalCategory.PEOPLE, "I helped the Stonebeard smith mend his broken gate.", "smith"),
        JournalEntry("confessed_to_dwarf", JournalCategory.PEOPLE, "I told the Stonebeard smith honestly why my cloak was gone.", "smith"),
        JournalEntry("took_buckler", JournalCategory.DECISIONS, "I took the buckler the smith offered me.", null),
        JournalEntry("offered_to_help", JournalCategory.PROMISES, "I told Halvard I would help him hold his gate, whatever it was.", "halvard_first"),
        JournalEntry("revealed_token", JournalCategory.DECISIONS, "I showed Halvard the tarnished token from the roots.", null),
        JournalEntry("braced_the_seal", JournalCategory.DECISIONS, "I braced the failing seal with my own hands.", "seal"),
        JournalEntry("guarded_the_warden", JournalCategory.DECISIONS, "I stood guard while Halvard worked on the failing seal.", "seal"),
        JournalEntry("listened_to_the_prisoner", JournalCategory.DECISIONS, "I listened a little too long to what the thing behind the gate offered.", "first_knock"),
        JournalEntry("refused_the_prisoner", JournalCategory.DECISIONS, "I turned away before the thing behind the gate finished its offer.", "first_knock"),
        JournalEntry("refused_once", JournalCategory.PROMISES, "I refused the office when it was first offered to me.", null),
        JournalEntry("accepted_fully", JournalCategory.PROMISES, "I accepted the office of jailer, all of it.", "office"),
        JournalEntry("accepted_reluctantly", JournalCategory.PROMISES, "I agreed to hold the gate until someone better is found.", "office"),
        JournalEntry("bound_to_the_gate", JournalCategory.PROMISES, "Halvard bound me to the gate. The mark is on my palm.", null),
        JournalEntry("argued_for_mercy", JournalCategory.DECISIONS, "I argued with Voss that the man behind the gate deserved mercy, not fire.", "voss_door"),
        JournalEntry("stood_his_ground", JournalCategory.DECISIONS, "I told Voss to stand aside. The gate was not hers to bury.", "voss_door"),
        JournalEntry("stalled_for_time", JournalCategory.DECISIONS, "I asked Voss for time to send word below. She did not give it.", "voss_door"),
        JournalEntry("fortified_the_gate", JournalCategory.DECISIONS, "I braced the black door and let the column come to us.", "siege"),
        JournalEntry("met_them_on_the_road", JournalCategory.DECISIONS, "I met the column on the ruined road before it reached the tree.", "siege"),
        JournalEntry("accepted_dark_aid", JournalCategory.DECISIONS, "In the worst of the fight I opened my branded hand and took the strength offered from behind the gate.", "dark_aid"),
        JournalEntry("refused_dark_aid_again", JournalCategory.DECISIONS, "I kept my hand closed and finished the fight on my own strength.", "dark_aid"),
        JournalEntry("heard_envoy_offer", JournalCategory.DECISIONS, "I heard the envoy out, though I promised nothing.", "envoy_reply"),
        JournalEntry("refused_envoy", JournalCategory.DECISIONS, "I told the envoy the Marshal's answer was no.", "envoy_reply"),
        JournalEntry("confronted_envoy_with_prisoners_words", JournalCategory.DECISIONS, "I told the envoy the prisoner had already said what the Marshal wants.", "envoy_reply"),
        JournalEntry("took_kestrels_locket", JournalCategory.PROMISES, "I took Kestrel's locket. Someone should carry what she could not finish saying.", null),
        JournalEntry("sealed_greymoor_personally", JournalCategory.DECISIONS, "I sealed Greymoor's gate myself, though it is only a stopgap.", "greymoor_end"),
        JournalEntry("reported_greymoor_honestly", JournalCategory.DECISIONS, "I carried word of Greymoor to people who can rebuild it.", "greymoor_end"),
        JournalEntry("left_greymoor_as_is", JournalCategory.DECISIONS, "I left Greymoor's gate as I found it and took the news back to Halvard.", "greymoor_end"),
        JournalEntry("trusted_voss_fully", JournalCategory.PEOPLE, "I told Voss I believed her.", "voss_trust"),
        JournalEntry("stayed_guarded", JournalCategory.PEOPLE, "I told Voss I would hear her out, and that this was not trust.", "voss_trust"),
        JournalEntry("held_voss_accountable", JournalCategory.PEOPLE, "I told Voss that trust has to run both ways, after her Order sent riders to bury the door.", "voss_trust"),
        JournalEntry("earned_voss_seal", JournalCategory.PEOPLE, "Voss gave me her Order signet, to open a door that will not be kicked down twice.", null),
        JournalEntry("chose_ashfall_now", JournalCategory.DECISIONS, "I went north with Voss to see the Ashfall Sanctum, telling no one.", "ashfall"),
        JournalEntry("chose_to_warn_halvard", JournalCategory.DECISIONS, "I told Halvard everything before leaving with Voss for Ashfall.", "ashfall"),
        JournalEntry("asked_for_time", JournalCategory.DECISIONS, "I asked Voss for a day before deciding about Ashfall.", "ashfall"),
        JournalEntry("stole_the_schematic", JournalCategory.DISCOVERIES, "We carried the Sanctum's plans out of Ashfall.", null),
        JournalEntry("vowed_to_return_now", JournalCategory.PROMISES, "I vowed to go back for Voss at once.", "voss_left"),
        JournalEntry("vowed_to_return_prepared", JournalCategory.PROMISES, "I vowed to go back for Voss properly, with Halvard first.", "voss_left"),
        JournalEntry("undecided_on_voss", JournalCategory.PROMISES, "I left Voss behind and put off deciding what I owed her.", "voss_left"),
        JournalEntry("created_a_diversion", JournalCategory.DECISIONS, "I went into Ashfall loud, to draw the garrison's eyes away from Voss.", "rescue_plan"),
        JournalEntry("infiltrated_quietly", JournalCategory.DECISIONS, "I went into Ashfall quietly, to outlast the garrison's attention.", "rescue_plan"),
        JournalEntry("went_with_what_they_had", JournalCategory.DECISIONS, "I took what Halvard could spare and went into Ashfall with no clearer plan.", "rescue_plan"),
        JournalEntry("broke_her_free", JournalCategory.PEOPLE, "I broke Voss's shackles myself.", "voss_chains"),
        JournalEntry("trusted_her_to_free_herself", JournalCategory.PEOPLE, "I kept watch and let Voss work her own lock.", "voss_chains"),
        JournalEntry("defied_ilsevet", JournalCategory.DECISIONS, "I told Ilsevet to come and take what she wanted.", "ilsevet_answer"),
        JournalEntry("refused_on_principle", JournalCategory.DECISIONS, "I told Ilsevet nothing I hold would be used to build her work.", "ilsevet_answer"),
        JournalEntry("answered_with_steel", JournalCategory.DECISIONS, "I answered Ilsevet with my sword and no words.", "ilsevet_answer"),
        JournalEntry("halvard_died", JournalCategory.PEOPLE, "Halvard died at Stonebeard's gate. I took his warden's chain.", null),
        JournalEntry("pledged_support_to_fenmoor", JournalCategory.PEOPLE, "I promised Thessaly that Stonebeard would send whoever it could spare.", "thessaly"),
        JournalEntry("invited_thessaly_to_stonebeard", JournalCategory.PEOPLE, "I invited Thessaly to stand with us at Stonebeard.", "thessaly"),
        JournalEntry("left_fenmoor_uncertain", JournalCategory.PEOPLE, "I left Fenmoor with no answer for Thessaly about what happens the third time.", "thessaly"),
        JournalEntry("chose_to_help_ilsevet", JournalCategory.DECISIONS, "I went back to Ashfall to stop what Ilsevet had built.", "ashfall_fire"),
        JournalEntry("refused_to_help_ilsevet", JournalCategory.DECISIONS, "I let Ilsevet live with what she built, and warned Fenmoor instead.", "ashfall_fire"),
        JournalEntry("stood_with_voss", JournalCategory.PEOPLE, "I stood beside Voss when the Order's inquisitor came for her.", "delegation"),
        JournalEntry("let_voss_go_alone", JournalCategory.PEOPLE, "I stayed at the tunnel mouth and let Voss face the Order's inquisitor alone.", "delegation"),
        JournalEntry("kaelen_vouched_for_voss", JournalCategory.PEOPLE, "I vouched for Voss myself before the Order's inquisitor.", "voss_verdict"),
        JournalEntry("trusted_voss_account", JournalCategory.PEOPLE, "I waited outside and trusted Voss's own account of herself.", "voss_verdict"),
        JournalEntry("began_training_new_wardens", JournalCategory.PROMISES, "I began training Stonebeard's kin to hold a gate.", "wardens"),
        JournalEntry("stood_watch_rather_than_delegate", JournalCategory.PROMISES, "I told Stonebeard's kin the chain was not mine to hand out, but I would stand watch beside anyone who took it up.", "wardens"),
        JournalEntry("deferred_the_question", JournalCategory.PROMISES, "I decided one gate and one jailer would do for now.", "wardens"),
        JournalEntry("told_voss_everything", JournalCategory.DISCOVERIES, "I told Voss everything I had seen, that same night.", "vision_shared"),
        JournalEntry("recorded_the_vision_first", JournalCategory.DISCOVERIES, "I wrote down what I had seen before telling anyone.", "vision_shared"),
        JournalEntry("kept_the_vision_to_himself", JournalCategory.DISCOVERIES, "I kept what I had seen to myself a while longer.", "vision_shared"),
        JournalEntry("resolved_despite_sympathy", JournalCategory.DECISIONS, "I understood Ilsevet's reasons and still resolved to stop her.", "old_debts"),
        JournalEntry("committed_to_proving_her_wrong", JournalCategory.DECISIONS, "I vowed to prove Ilsevet wrong about eternal watching, rather than just outlast her.", "old_debts"),
        JournalEntry("deferred_the_question_of_ilsevet", JournalCategory.DECISIONS, "I set aside the question of what Ilsevet deserves, for another night.", "old_debts"),
        JournalEntry("kept_shards_close", JournalCategory.DECISIONS, "I kept every fragment of the frame on my own person.", "shards"),
        JournalEntry("dispersed_the_shards", JournalCategory.DECISIONS, "I split the frame fragments between Stonebeard, Fenmoor and Voss.", "shards"),
        JournalEntry("trusted_to_luck", JournalCategory.DECISIONS, "I did nothing for the fragments but keep watch and hope.", "shards"),
        JournalEntry("destroyed_the_frame_outright", JournalCategory.DECISIONS, "I tore Ilsevet's frame apart.", "frame"),
        JournalEntry("disrupted_carefully", JournalCategory.DECISIONS, "I disrupted the frame's settings without destroying them, so the prisoner would not be unmade by accident.", "frame"),
        JournalEntry("stopped_it_however_possible", JournalCategory.DECISIONS, "I stopped the frame by whatever means were fastest.", "frame"),
        JournalEntry("captured_ilsevet", JournalCategory.PEOPLE, "I bound Ilsevet and brought her back to face what is left of the Order.", "ilsevet_fate"),
        JournalEntry("showed_ilsevet_mercy", JournalCategory.PEOPLE, "I let Ilsevet go, on condition she never comes back armed.", "ilsevet_fate"),
        JournalEntry("deferred_ilsevets_fate", JournalCategory.PEOPLE, "I took Ilsevet's weapon and left her fate undecided.", "ilsevet_fate"),
        JournalEntry("vowed_to_protect_the_prisoner", JournalCategory.PEOPLE, "I vowed that nobody would be allowed to erase the prisoner.", "sixth_vow"),
        JournalEntry("affirmed_its_own_choice", JournalCategory.PEOPLE, "I told the prisoner that what it means to be itself is its own to decide.", "sixth_vow"),
        JournalEntry("left_the_question_open", JournalCategory.PEOPLE, "I left the prisoner's question open for now.", "sixth_vow"),
        JournalEntry("vowed_constant_vigilance", JournalCategory.PROMISES, "I vowed to be there every time the prisoner is asked.", "commit22"),
        JournalEntry("vowed_to_share_the_burden", JournalCategory.PROMISES, "I told the prisoner it does not have to win alone every time, and offered to carry some of it.", "commit22"),
        JournalEntry("simply_stayed", JournalCategory.PROMISES, "I stayed with the prisoner until morning, saying nothing.", "commit22"),
        JournalEntry("refused_to_leave_emberlow", JournalCategory.PEOPLE, "I stayed at Emberlow's gate until the one behind it knew she did not have to decide alone.", "emberlow"),
        JournalEntry("insisted_on_real_choice", JournalCategory.PEOPLE, "I told the one behind Emberlow's gate that whatever she chose had to be hers.", "emberlow"),
        JournalEntry("waited_at_emberlow", JournalCategory.PEOPLE, "I waited at Emberlow's gate and said nothing until she was ready to hear it.", "emberlow"),
        JournalEntry("reminded_greymoor_of_choice", JournalCategory.PEOPLE, "I told the one at Greymoor's gate that she still has a choice Kestrel never had.", "greymoor_reach"),
        JournalEntry("offered_presence_not_replacement", JournalCategory.PEOPLE, "I told the one at Greymoor's gate I was not there to replace Kestrel, only to keep her company.", "greymoor_reach"),
        JournalEntry("sat_with_greymoors_silence", JournalCategory.PEOPLE, "I sat with Greymoor's silence and let it decide when to break.", "greymoor_reach"),
        JournalEntry("vowed_to_hold_duskmere", JournalCategory.PROMISES, "I vowed that Duskmere's gate would not be left empty.", "duskmere"),
        JournalEntry("sought_to_remember_wren", JournalCategory.PROMISES, "I vowed to find out who Wren was.", "duskmere"),
        JournalEntry("secured_duskmere_in_silence", JournalCategory.PROMISES, "I secured Duskmere's gate before dark and said nothing.", "duskmere"),
        JournalEntry("pressed_the_forgotten_to_remember", JournalCategory.DECISIONS, "I told the forgotten one at the Sundering Ground it was still part of something.", "forgotten"),
        JournalEntry("respected_the_forgetting", JournalCategory.DECISIONS, "I let the forgetting be, rather than undo it carelessly.", "forgotten"),
        JournalEntry("stepped_back_from_the_sundering", JournalCategory.DECISIONS, "I left the thread where it fell and stepped back.", "forgotten"),
        JournalEntry("offered_the_vigil_alliance", JournalCategory.PEOPLE, "I offered the Wraithspire vigil allies, not replacements.", "vigil"),
        JournalEntry("respected_the_vigils_pace", JournalCategory.PEOPLE, "I asked the Wraithspire vigil to show me only what they were ready to.", "vigil"),
        JournalEntry("simply_bore_witness_at_wraithspire", JournalCategory.PEOPLE, "I looked at what the Wraithspire vigil had protected and let that be answer enough.", "vigil"),
        JournalEntry("chose_to_seek_the_ending", JournalCategory.DECISIONS, "I chose to find the whole and finish this.", "ending_start"),
        JournalEntry("insisted_on_another_way", JournalCategory.DECISIONS, "I refused to spend this on a killing blow if there was another way.", "ending_start"),
        JournalEntry("demanded_it_be_earned", JournalCategory.PROMISES, "I told the whole it must earn every one of us honestly, or lose all six for good.", "final_offer"),
        JournalEntry("offered_connection_without_merging", JournalCategory.PROMISES, "I promised the whole that six who choose to stay close is not the same as losing.", "final_offer"),
        JournalEntry("simply_stayed_at_the_end", JournalCategory.PROMISES, "I stayed with the whole as long as it took, and let it choose on its own terms.", "final_offer"),
        // Planned (Chapter 1 rework): not yet set by the story.
        JournalEntry("spared_the_rider", JournalCategory.DECISIONS, "I spared a rider who hunted me on the ruined road.", "rider"),
        JournalEntry("robbed_the_rider", JournalCategory.DECISIONS, "I took what I could from a fallen rider.", "rider"),
        JournalEntry("rider_clue", JournalCategory.DECISIONS, "I took a scraped wax disc from a rider I questioned.", "rider"),
        JournalEntry("found_red_cord", JournalCategory.DISCOVERIES, "I found a length of red cord in the gully.", null),
        JournalEntry("found_the_old_shaft", JournalCategory.DISCOVERIES, "I found an old shaft among the trees.", null),
        JournalEntry("smith_vouched", JournalCategory.PEOPLE, "The Stonebeard smith vouched for me to Halvard.", null),
        JournalEntry("took_from_the_bench", JournalCategory.DECISIONS, "I took a draught from the smith's bench without asking.", "bench"),
        JournalEntry("returned_the_draught", JournalCategory.DECISIONS, "I paid the smith back for the draught I took.", "bench")
    )

    /** Flags the Chapter 1 rework will introduce; described or hidden here ahead of time. */
    val plannedFlags: Set<String> = setOf(
        "spared_the_rider",
        "robbed_the_rider",
        "rider_clue",
        "found_red_cord",
        "found_the_old_shaft",
        "smith_vouched",
        "took_from_the_bench",
        "returned_the_draught",
        "hunted_on_the_road",
        "hunters_saw_door",
        "met_red_cord_rider",
        "fought_the_riders",
        "evaded_the_riders",
        "fell_through_the_door"
    )

    /** What each alternative-group is about, used for the neutral line shown after a replay. */
    val groupTopics: Map<String, String> = mapOf(
        "ashfall" to "the road to Ashfall",
        "ashfall_fire" to "the fire at Ashfall",
        "bench" to "the smith's bench",
        "clasp" to "the clasp in the roots",
        "commit22" to "my promise to the prisoner",
        "dark_aid" to "the strength offered from behind the gate",
        "delegation" to "the Order's inquisitor",
        "door" to "the black door",
        "duskmere" to "Duskmere's gate",
        "emberlow" to "Emberlow's gate",
        "ending_start" to "how to end this",
        "envoy_reply" to "the envoy's offer",
        "final_offer" to "my final offer",
        "first_knock" to "the first offer from behind the gate",
        "forgotten" to "the forgotten one",
        "frame" to "Ilsevet's frame",
        "greymoor_end" to "Greymoor's gate",
        "greymoor_reach" to "the one at Greymoor's gate",
        "halvard_first" to "my first words with Halvard",
        "ilsevet_answer" to "Ilsevet's demand",
        "ilsevet_fate" to "Ilsevet's fate",
        "office" to "Halvard's gate",
        "old_debts" to "Ilsevet's reasons",
        "rescue_plan" to "the rescue at Ashfall",
        "rider" to "a fallen rider",
        "seal" to "the failing seal",
        "shards" to "the frame fragments",
        "siege" to "the column on the road",
        "sixth_vow" to "the prisoner's future",
        "smith" to "the Stonebeard smith",
        "thessaly" to "Thessaly at Fenmoor",
        "vigil" to "the Wraithspire vigil",
        "vision_shared" to "what I had seen",
        "voss_chains" to "Voss in chains",
        "voss_door" to "Voss at the gate",
        "voss_left" to "what I owed Voss",
        "voss_trust" to "whether to trust Voss",
        "voss_verdict" to "the Order's verdict on Voss",
        "wardens" to "who will hold a gate after me"
    )

    /** Every flag deliberately kept out of the Journal, with the reason. */
    val hidden: Map<String, HiddenReason> = mapOf(
        // BOOKKEEPING
        "lost_to_ashen_vanguard" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_adept" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_castellan" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_extraction_leader" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_reliquary_thief" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_reprisal_leader" to HiddenReason.BOOKKEEPING,
        "lost_to_cinder_straggler_captain" to HiddenReason.BOOKKEEPING,
        "lost_to_greymoor_ward_wraith" to HiddenReason.BOOKKEEPING,
        "lost_to_ilsevet_the_cinder_marshal" to HiddenReason.BOOKKEEPING,
        "lost_to_ilsevets_vanguard_captain" to HiddenReason.BOOKKEEPING,
        "lost_to_loyalist_enforcer" to HiddenReason.BOOKKEEPING,
        "lost_to_sanctum_construct" to HiddenReason.BOOKKEEPING,
        "lost_to_sanctum_sentinel" to HiddenReason.BOOKKEEPING,
        "lost_to_the_answering_door" to HiddenReason.BOOKKEEPING,
        "lost_to_the_ghostwriter" to HiddenReason.BOOKKEEPING,
        "lost_to_the_memory_itself" to HiddenReason.BOOKKEEPING,
        "lost_to_the_patient_voice" to HiddenReason.BOOKKEEPING,
        "lost_to_the_reclamation" to HiddenReason.BOOKKEEPING,
        "lost_to_the_unfinished" to HiddenReason.BOOKKEEPING,
        "lost_to_the_unremembering" to HiddenReason.BOOKKEEPING,
        "lost_to_the_whole_at_full_reach" to HiddenReason.BOOKKEEPING,
        "lost_to_the_whole_undisguised" to HiddenReason.BOOKKEEPING,
        "lost_to_unbound_horror" to HiddenReason.BOOKKEEPING,
        "lost_to_vigil_captain_of_wraithspire" to HiddenReason.BOOKKEEPING,
        "pressed_deeper" to HiddenReason.BOOKKEEPING,
        "returned_to_hold" to HiddenReason.BOOKKEEPING,
        "survived_first_blood_defeat" to HiddenReason.BOOKKEEPING,
        // REDUNDANT
        "took_the_warden_chain" to HiddenReason.REDUNDANT,
        // PENDING_NARRATIVE_DECISION
        "claimed_his_own_meaning" to HiddenReason.PENDING_NARRATIVE_DECISION,
        "defied_the_prophecy" to HiddenReason.PENDING_NARRATIVE_DECISION,
        "left_it_unanswered" to HiddenReason.PENDING_NARRATIVE_DECISION,
        // PLANNED_INTERNAL
        "evaded_the_riders" to HiddenReason.PLANNED_INTERNAL,
        "fell_through_the_door" to HiddenReason.PLANNED_INTERNAL,
        "fought_the_riders" to HiddenReason.PLANNED_INTERNAL,
        "hunted_on_the_road" to HiddenReason.PLANNED_INTERNAL,
        "met_red_cord_rider" to HiddenReason.PLANNED_INTERNAL,
        // TONE_ONLY
        "accepted_the_duel_readily" to HiddenReason.TONE_ONLY,
        "accepted_too_late" to HiddenReason.TONE_ONLY,
        "acknowledged_the_risk" to HiddenReason.TONE_ONLY,
        "already_suspected_ilsevet" to HiddenReason.TONE_ONLY,
        "announced_himself" to HiddenReason.TONE_ONLY,
        "answered_the_word_at_once" to HiddenReason.TONE_ONLY,
        "answered_with_silence" to HiddenReason.TONE_ONLY,
        "appealed_to_ilsevets_conscience" to HiddenReason.TONE_ONLY,
        "asked_about_jailers" to HiddenReason.TONE_ONLY,
        "asked_answered_how" to HiddenReason.TONE_ONLY,
        "asked_before_answering" to HiddenReason.TONE_ONLY,
        "asked_before_racing" to HiddenReason.TONE_ONLY,
        "asked_directly_ch22" to HiddenReason.TONE_ONLY,
        "asked_for_measured_truth" to HiddenReason.TONE_ONLY,
        "asked_how_long_ch24" to HiddenReason.TONE_ONLY,
        "asked_how_soon" to HiddenReason.TONE_ONLY,
        "asked_the_prisoner_directly" to HiddenReason.TONE_ONLY,
        "asked_what_it_needed_before" to HiddenReason.TONE_ONLY,
        "brought_halvard" to HiddenReason.TONE_ONLY,
        "brought_voss_to_greymoor" to HiddenReason.TONE_ONLY,
        "brought_voss_to_workshop" to HiddenReason.TONE_ONLY,
        "chased_the_envoy" to HiddenReason.TONE_ONLY,
        "chose_caution_before_pursuit" to HiddenReason.TONE_ONLY,
        "chose_deliberately" to HiddenReason.TONE_ONLY,
        "chose_to_pursue_the_vision" to HiddenReason.TONE_ONLY,
        "chose_to_travel" to HiddenReason.TONE_ONLY,
        "circled_wide" to HiddenReason.TONE_ONLY,
        "connected_to_the_sundering" to HiddenReason.TONE_ONLY,
        "crept_in" to HiddenReason.TONE_ONLY,
        "decided_at_last" to HiddenReason.TONE_ONLY,
        "defeated_ordrun" to HiddenReason.TONE_ONLY,
        "demanded_plain_warning" to HiddenReason.TONE_ONLY,
        "demanded_the_truth" to HiddenReason.TONE_ONLY,
        "doubted_the_vision" to HiddenReason.TONE_ONLY,
        "expected_wraithspire_to_be_dangerous" to HiddenReason.TONE_ONLY,
        "fortified_for_the_last_stand" to HiddenReason.TONE_ONLY,
        "gave_the_prisoner_time" to HiddenReason.TONE_ONLY,
        "hoped_wraithspire_was_well_kept" to HiddenReason.TONE_ONLY,
        "knelt_for_the_warden" to HiddenReason.TONE_ONLY,
        "left_ilsevet_to_her_wreckage" to HiddenReason.TONE_ONLY,
        "let_her_finish" to HiddenReason.TONE_ONLY,
        "let_the_prisoner_explain" to HiddenReason.TONE_ONLY,
        "listened_first" to HiddenReason.TONE_ONLY,
        "listened_without_asking" to HiddenReason.TONE_ONLY,
        "made_her_say_it_first" to HiddenReason.TONE_ONLY,
        "met_envoy_alone" to HiddenReason.TONE_ONLY,
        "moved_in_stealthily" to HiddenReason.TONE_ONLY,
        "moved_with_preparation" to HiddenReason.TONE_ONLY,
        "moved_without_delay" to HiddenReason.TONE_ONLY,
        "moved_without_waiting" to HiddenReason.TONE_ONLY,
        "noted_hearing_the_offer" to HiddenReason.TONE_ONLY,
        "offered_patience_ch22" to HiddenReason.TONE_ONLY,
        "prepared_to_meet_them_hard" to HiddenReason.TONE_ONLY,
        "pressed_about_the_archive" to HiddenReason.TONE_ONLY,
        "pressed_the_prisoner_for_detail" to HiddenReason.TONE_ONLY,
        "pressed_voss" to HiddenReason.TONE_ONLY,
        "prioritized_the_undecided_fragments" to HiddenReason.TONE_ONLY,
        "raced_to_emberlow" to HiddenReason.TONE_ONLY,
        "read_alone_first" to HiddenReason.TONE_ONLY,
        "read_with_voss" to HiddenReason.TONE_ONLY,
        "reassured_voss" to HiddenReason.TONE_ONLY,
        "reassured_without_conditions" to HiddenReason.TONE_ONLY,
        "resolved_to_go_regardless" to HiddenReason.TONE_ONLY,
        "resolved_to_hold_the_thread" to HiddenReason.TONE_ONLY,
        "resolved_to_seek_the_whole" to HiddenReason.TONE_ONLY,
        "rested_before_deciding" to HiddenReason.TONE_ONLY,
        "searched_the_gate_first" to HiddenReason.TONE_ONLY,
        "searched_the_ground" to HiddenReason.TONE_ONLY,
        "secured_the_splinter_quietly" to HiddenReason.TONE_ONLY,
        "spoke_before_the_duel" to HiddenReason.TONE_ONLY,
        "stayed_with_halvard" to HiddenReason.TONE_ONLY,
        "still_asking" to HiddenReason.TONE_ONLY,
        "stood_defiant_before_the_whole" to HiddenReason.TONE_ONLY,
        "stood_with_it_in_the_asking" to HiddenReason.TONE_ONLY,
        "tried_to_explain_first" to HiddenReason.TONE_ONLY,
        "trusted_the_vision" to HiddenReason.TONE_ONLY,
        "waited_in_silence" to HiddenReason.TONE_ONLY,
        "warned_ilsevet_directly" to HiddenReason.TONE_ONLY,
        "welcomed_voss" to HiddenReason.TONE_ONLY,
        "went_to_workshop_alone" to HiddenReason.TONE_ONLY
    )

    private val byFlag: Map<String, JournalEntry> = entries.associateBy { it.flag }
    private val order: Map<String, Int> = entries.withIndex().associate { (i, e) -> e.flag to i }

    /** The Journal for [flags]: unknown and hidden flags are ignored; categories with no lines are omitted. */
    fun sections(flags: Set<String>): List<JournalSection> {
        val present = entries.filter { it.flag in flags }
        val lines = LinkedHashMap<JournalCategory, MutableList<String>>()
        val doneGroups = HashSet<String>()
        for (entry in present) {
            val group = entry.group
            val line = if (group == null) {
                entry.text
            } else {
                if (!doneGroups.add(group)) continue
                val members = present.count { it.group == group }
                if (members == 1) entry.text
                else "I have faced ${groupTopics.getValue(group)} more than once, and not always chosen the same way."
            }
            lines.getOrPut(entry.category) { ArrayList() }.add(line)
        }
        return JournalCategory.entries.mapNotNull { c -> lines[c]?.let { JournalSection(c, it) } }
    }
}
