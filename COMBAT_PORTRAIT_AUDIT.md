# Combat portrait audit — 27 September 2026

Baseline: PR #91, commit `6261b42fbea4320d7576c89d99a405dc7cfd977b`.

As of 30 September 2026, all 26 enemies have explicit dedicated portrait mappings. Later batches still await Android build and phone acceptance.

All 26 `CombatEncounter` definitions have one or more launching story nodes. The combat screen uses dedicated portraits where mapped, and otherwise uses the first launching node's `illustrationId` cropped into a 128 dp card. The table records original story art and current review status. Story illustration files remain intact.

| Chapter | Encounter / enemy | Current scene art | Portrait assessment |
| --- | --- | --- | --- |
| 1 | first_blood / Cave Lurker | `cavern_ambush` | Dedicated `cave_lurker_combat` now selected; phone acceptance pending. |
| 2 | seal_breaker / Seal-Bound Wraith | `seal_breaker_ambush` | Dedicated `seal_wraith_combat` now selected; phone acceptance pending. |
| 4 | siege / Ashen Vanguard | `dwarven_hold_gate` or `road_away_from_tree` | Dedicated `ashen_vanguard_combat` selected on both routes; phone acceptance pending. |
| 5 | cinder_envoy / Cinder Adept | `cinder_adept_ambush` | Dedicated `cinder_adept_combat` now selected; phone acceptance pending. |
| 6 | unbound / Unbound Horror | `the_unbound_creature` | Dedicated `unbound_horror_combat` now selected; phone acceptance pending. |
| 7 | loyalist_ambush / Cinder Loyalist Enforcer | `loyalists_ambush` | Dedicated `loyalist_enforcer_combat` now selected; phone acceptance pending. |
| 8 | sanctum_sentinel / Sanctum Sentinel | `sentinels_close_in` | Dedicated `sanctum_sentinel_combat` now selected; phone acceptance pending. |
| 9 | right_hand / Castellan Ordrun | `the_right_hand_intercepts` | Dedicated `cinder_castellan_combat` selected; decoded and visually checked, phone acceptance pending. |
| 10 | stonebeard_siege / Ilsevet's Vanguard Captain | `the_gate_falls` | Dedicated `ilsevets_vanguard_captain_combat` selected; decoded and visually checked, phone acceptance pending. |
| 11 | fenmoor_extraction / Cinder Extraction Leader | `the_marsh_holds` | Dedicated `cinder_extraction_leader_combat` selected; decoded and visually checked, phone acceptance pending. |
| 12 | unfinished_thing / The Unfinished Thing | `the_unfinished_thing` | Dedicated `the_unfinished_combat` selected; decoded and visually checked, phone acceptance pending. |
| 14 | memory_confrontation / The Memory Itself | `inside_the_memory` | Dedicated `the_memory_itself_combat` selected; decoded and visually checked, phone acceptance pending. |
| 15 | reprisal_squad / Cinder Reprisal Leader | `the_reprisal` | Dedicated `cinder_reprisal_leader_combat` selected; decoded and visually checked, phone acceptance pending. |
| 16 | reliquary_thief / Cinder Reliquary Thief | `the_reliquary_thief` | Dedicated `cinder_reliquary_thief_combat` selected; decoded and visually checked, phone acceptance pending. |
| 17 | chamber_guardian / Sanctum Construct | `the_chamber_defended` | Dedicated `sanctum_construct_combat` selected; decoded and visually checked, phone acceptance pending. |
| 18 | ilsevet_duel / Ilsevet the Cinder Marshal | `the_duel` | Dedicated `ilsevet_the_cinder_marshal_combat` selected; decoded and visually checked, phone acceptance pending. |
| 20 | ghostwriter / The Ghostwriter | `the_workshops_own_guardian` | Dedicated `the_ghostwriter_combat` selected; decoded and visually checked, phone acceptance pending. |
| 22 | patient_voice / The Patient Voice | `into_the_asking` | Dedicated `the_patient_voice_combat` selected; decoded and visually checked, phone acceptance pending. |
| 23 | emberlow_stragglers / Cinder Straggler Captain | `what_waits_at_the_threshold` | Dedicated `cinder_straggler_captain_combat` selected; decoded and visually checked, phone acceptance pending. |
| 24 | greymoor_unraveling / Greymoor Ward-Wraith | `what_waits_in_the_keening` | Dedicated `greymoor_ward_wraith_combat` selected; decoded and visually checked, phone acceptance pending. |
| 25 | duskmere_threshold / The Answering Door | `the_answering_shape` | Dedicated `the_answering_door_combat` selected; decoded and visually checked, phone acceptance pending. |
| 26 | sundering_ground / The Unremembering | `what_guards_the_beginning` | Dedicated `the_unremembering_combat` selected; decoded and visually checked, phone acceptance pending. |
| 27 | the_reclamation / The Reclamation | `the_reclamation` | Dedicated `the_reclamation_combat` selected; decoded and visually checked, phone acceptance pending. |
| 28 | wraithspire_vigil / Vigil Captain | `the_vigils_challenge` | Dedicated `vigil_captain_of_wraithspire_combat` selected; decoded and visually checked, phone acceptance pending. |
| 29 | the_whole / The Whole, Undisguised | `into_the_undisguised` | Dedicated `the_whole_undisguised_combat` selected; decoded and visually checked, phone acceptance pending. |
| 30 | the_last_reach / The Whole at Full Reach | `the_last_reach` | Dedicated `the_whole_at_full_reach_combat` selected; decoded and visually checked, phone acceptance pending. |

A crop candidate is **unverified**. Each needs a portrait-sized preview and phone acceptance. Do not mark the full enemy-art task complete until every row has a deliberate, visually checked portrait (asset or verified crop), including both siege routes. Kaelen now has a separate draft portrait; verify its canon details and phone crop before acceptance. Combat mechanics, XP and HP progression stay outside this art pass.
