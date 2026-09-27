# Combat portrait audit — 27 September 2026

Baseline: PR #91, commit `6261b42fbea4320d7576c89d99a405dc7cfd977b`.

All 26 `CombatEncounter` definitions have one or more launching story nodes. The combat screen uses dedicated portraits where mapped, and otherwise uses the first launching node's `illustrationId` cropped into a 128 dp card. The table records original story art and current review status. Story illustration files remain intact.

| Chapter | Encounter / enemy | Current scene art | Portrait assessment |
| --- | --- | --- | --- |
| 1 | first_blood / Cave Lurker | `cavern_ambush` | Dedicated `cave_lurker_combat` now selected; phone acceptance pending. |
| 2 | seal_breaker / Seal-Bound Wraith | `seal_breaker_ambush` | Dedicated `seal_wraith_combat` now selected; phone acceptance pending. |
| 4 | siege / Ashen Vanguard | `dwarven_hold_gate` or `road_away_from_tree` | Dedicated `ashen_vanguard_combat` selected on both routes; phone acceptance pending. |
| 5 | cinder_envoy / Cinder Adept | `cinder_adept_ambush` | Hooded enemy left; close crop candidate. |
| 6 | unbound / Unbound Horror | `the_unbound_creature` | Creature at right; close crop candidate. |
| 7 | loyalist_ambush / Cinder Loyalist Enforcer | `loyalists_ambush` | Multiple combatants; dedicated portrait required. |
| 8 | sanctum_sentinel / Sanctum Sentinel | `sentinels_close_in` | Distant construct and multiple figures; dedicated portrait required. |
| 9 | right_hand / Castellan Ordrun | `the_right_hand_intercepts` | Several figures; dedicated portrait required. |
| 10 | stonebeard_siege / Ilsevet's Vanguard Captain | `the_gate_falls` | Foreground armoured figure, but other soldiers; crop candidate. |
| 11 | fenmoor_extraction / Cinder Extraction Leader | `the_marsh_holds` | Multiple figures; dedicated portrait required. |
| 12 | unfinished_thing / The Unfinished Thing | `the_unfinished_thing` | Large central creature; close crop candidate. |
| 14 | memory_confrontation / The Memory Itself | `inside_the_memory` | Abstract force; scene may suit creature identity, inspect on phone. |
| 15 | reprisal_squad / Cinder Reprisal Leader | `the_reprisal` | Central hooded figure with others; crop candidate. |
| 16 | reliquary_thief / Cinder Reliquary Thief | `the_reliquary_thief` | Hooded figure at right; crop candidate. |
| 17 | chamber_guardian / Sanctum Construct | `the_chamber_defended` | Central construct; crop candidate. |
| 18 | ilsevet_duel / Ilsevet the Cinder Marshal | `the_duel` | Opponent among two fighters; dedicated portrait preferable. |
| 20 | ghostwriter / The Ghostwriter | `the_workshops_own_guardian` | Spectral figure at right; crop candidate. |
| 22 | patient_voice / The Patient Voice | `into_the_asking` | **Only Kaelen at door; dedicated enemy visual required.** |
| 23 | emberlow_stragglers / Cinder Straggler Captain | `what_waits_at_the_threshold` | Multiple fighters; dedicated portrait required. |
| 24 | greymoor_unraveling / Greymoor Ward-Wraith | `what_waits_in_the_keening` | Diffuse wraith to right; crop candidate. |
| 25 | duskmere_threshold / The Answering Door | `the_answering_shape` | Central construct; crop candidate. |
| 26 | sundering_ground / The Unremembering | `what_guards_the_beginning` | Diffuse figure; dedicated portrait likely needed. |
| 27 | the_reclamation / The Reclamation | `the_reclamation` | Right-hand creature; close crop candidate. |
| 28 | wraithspire_vigil / Vigil Captain | `the_vigils_challenge` | Several fighters; dedicated portrait required. |
| 29 | the_whole / The Whole, Undisguised | `into_the_undisguised` | Abstract entity fills scene; crop candidate. |
| 30 | the_last_reach / The Whole at Full Reach | `the_last_reach` | Distant figure and Kaelen; dedicated portrait required. |

A crop candidate is **unverified**. Each needs a portrait-sized preview and phone acceptance. Do not mark the full enemy-art task complete until every row has a deliberate, visually checked portrait (asset or verified crop), including both siege routes. Kaelen now has a separate draft portrait; verify its canon details and phone crop before acceptance. Combat mechanics, XP and HP progression stay outside this art pass.
