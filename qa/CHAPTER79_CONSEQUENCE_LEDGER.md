# Chapters 7 to 9 consequence ledger

Status: written on `chapters-6-9-expansion` (from 7f22cd3); uncommitted. This is a design record, not player text.
Every row says what the decision does in the story **today**, which scenes **actually read** the flag it sets (so a reader can be
found in the code, not only in a plan), and what a later chapter is **planned** to do with it. "Planned" is a proposal. None of it
is implemented, none of it needs an engine or save-format change, and Chapters 10 onward are not touched by this package.

The readers listed here were found by searching the story sources for the flag in a `requiredFlags` or `forbiddenFlags`
(`ledger_scan.py`, kept with the review pack, not in the repository; the search is repeatable). A flag with no reader is still recorded: it feeds the Journal, and the
last column says where the blueprint wanted it read.

## How the package holds its state

| Family (all exclusive: a choice that sets one clears the others) | Members | Cleared by |
|---|---|---|
| `C7_FORGE_FAMILY` | `c7_traded`, `c7_shut`, `c7_page`, `c7_comfort`, `c7_quiet` | the forge choices |
| `C7_HOLD_FAMILY` | `c7_hold_terms`, `c7_hold_refused`, `c7_hold_hidden` | the promise to Edda |
| `C7_RUNG_FAMILY` (Voss's ladder) | `c7_rung_wary`, `c7_rung_working`, `c7_rung_trusted` | every choice that moves it; a save with no rung counts as the lowest |
| `C7_GROUND_FAMILY` | `c7_ground_yard`, `c7_ground_stair`, `c7_ground_road` | the ambush choice |
| `C7_VERDICT_FAMILY` | `c7_halvard_trusts_voss`, `c7_halvard_doubts_voss` | the hearth |
| `C8_EXIT_FAMILY` | `c8_exit_ramp`, `c8_exit_tithe` (the sentinel fight sets neither) | the horn |
| `C8_NIGHT_FAMILY` | `c8_wrote_halvard`, `c8_ash_farmers`, `c8_marked_route` | the long night |
| `C9_WAY_FAMILY` | the three shipped plans, `c9_way_stair`, `c9_way_wall`, `c9_way_cart` | the plan |
| `C9_DOOR_FAMILY` | `c9_door_seal`, `c9_door_nails`, `c9_door_force` (waiting sets none) | the door |
| `C9_DAYS_FAMILY` | `c9_stair_rules`, `c9_stair_people`, `c9_gate_braced` | the three places that can set them |
| shared hub markers | `hub_a`, `hub_b`, `hub_c`, `hub_second` | the choice that leaves each hub |

Each chapter's own first decision clears everything the chapter owns (`C7_TELLING_FLAGS`, `C8_TELLING_FLAGS`,
`C9_TELLING_FLAGS`), so a replay reaches the new chapter with an empty slate. Chapter VII's, VIII's and VI's flags are never
cleared by a later chapter, because they describe a telling that has already been decided again.

## Chapter 7: An Uneasy Alliance

| Flag | Set at | Local effect | Read by (implemented) | Journal | Planned later use (not implemented) |
|---|---|---|---|---|---|
| `c7_traded`, `c7_halvard_open` | `c7_forge_trade` / `c7_forge_hand`, "open your hand" | Honour +1. Kaelen tells Halvard what the envoy's offer aimed at; Halvard pays the trade he offered in Chapter 6: Kestrel's letter, read in full | `c7_halvard_open` -> `c7_edda_promise` (a Halvard who has opened up is told the promise differently) | forge_talk group (`c7_halvard_open` hidden) | Ch10 Last Evening hub (T10-01): what Halvard says in his last days |
| `c7_shut` | same scenes, "keep your hand shut" | none; Halvard accepts it ("I did the same for nine winters") | none | hidden (tone) | Ch10: the same conversation unfinished |
| `c7_page` | `c7_forge_letter`, "give him the page" (needs the cairn note) | Honour +1; Halvard reads Kestrel's own words | none | forge_talk | Ch10/Ch13: Halvard's rites |
| `c7_comfort` | `c7_forge_letter` | none; Halvard thanks him | none | forge_talk | Ch10 |
| `c7_quiet` | `c7_forge_letter` | none | none | forge_talk | Ch10 |
| `c7_hold_terms` | `c7_edda_promise`, tell him and ask for the north stair | Halvard agrees to thirty on the north stair, with a price: Kaelen stands between them and him | **Ch9** hub offers the north stair instead of the gate; `c9_north_stair` is the bill coming due | promises group | **Ch10 payoff**: the refugees are inside the Hold when the siege comes (Tam, Edda's people) |
| `c7_hold_refused` | same, tell him and ask | Halvard will send grain, not a door; Kaelen still has to tell Edda | **Ch9** `c9_gate_work` (label: "for the man who would not open his door") | promises group | Ch10: refugees outside the Hold; Edda's word |
| `c7_hold_hidden` | `c7_edda_promise`, say nothing | the promise stays unspent | **Ch9** `c9_gate_work` (label: "for the man you did not tell") | promises group | Ch10 |
| `c7_rung_wary/working/trusted` | stance choices (start), then every beat that moves Voss's standing: the wound scenes, the hearth, the night camp, the deserter, the chapel, the cart, the chains | the ladder; it never decides whether Voss is captured or rescued, only how the scene goes and what it costs | Ch7 call-off needs `working`; Ch8 `c8_her_choice` and `c8_capture_ramp` need `trusted`; Ch9 `voss_in_chains`, the blade scenes and the chapel option read it | voss_rung group | Ch12 and 13 (Voss's answers), Ch18 (Selvane's verdict has already cost her something) |
| `c7_ground_yard` | `c7_the_ground` | the forge shed burns (Halvard's tongs, charcoal, whetstones) | **Ch9** `c9_stores`: one remedy and no fine edge | ambush_ground group | Ch10: the forge cannot sharpen what the siege needs |
| `c7_ground_stair` | same | the armoury is whole, but the riders who ran saw where the stair leads | **Ch9** `c9_stores`: two remedies | same | Ch10: the column knows the tunnel |
| `c7_ground_road` | same | the Hold is never touched, but word of Voss reaches Ilsevet | **Ch9** `c9_stores`: one remedy and fifteen coin put by | same | Ch10: Ilsevet already knows who stands beside the jailer |
| `c7_voss_called_them_off` | `c7_the_ground`, "tell Voss to speak to them" (needs rung `working`, Honour 4) | no fight: no XP, no fight trophy; trophy "Called Off"; Voss is hunted openly from now on | **Ch9** `c9_plans`: Halvard can say what the Order will do to her, and names Ordrun | ambush_ground group | Ch13: Selvane judges Voss openly defying the Order; Ch18 |
| `c7_voss_below` | the three cost scenes, "bring her below" | Halvard meets Voss at the hearth | Ch7 `what_voss_offers` (two sets of answers keep the shipped end scenes reachable) | none | none |
| `c7_halvard_trusts_voss` / `c7_halvard_doubts_voss` | `c7_hold_hearth`, three ways to meet them | Halvard's verdict on the stair before dawn | Ch7 the verdict scenes; **Ch9** `c9_table_done` -> the loan, in his own voice either way | halvard_on_voss group | Ch10 and 13 |
| `c7_asked_about_riders` | the stance choices, the extra question | `c7_riders_followed_up`: Voss followed the riders; they follow her now | none | hidden (tone) | none |
| `c7_voss_hurt_deeper` | the lost fight at the ambush | Voss is hurt worse than she shows | none | hidden (tone) | Ch10: "still healing" |
| `earned_voss_seal` + item `voss_seal` | every route after the ambush (shipped flag, now guaranteed) | the signet | Ch8 `c8_the_horn` (the arch), **Ch9** `c9_the_door` (the ward) | existing | Ch13 delegation scene |
| `trusted_voss_fully` (shipped) | the stance choices | Voss's first rung is `working` | Ch8 `chapter8_end`; Ch12, Ch18 (existing readers) | existing | as before |

## Chapter 8: The Seventh Door

| Flag | Set at | Local effect | Read by (implemented) | Journal | Planned later use (not implemented) |
|---|---|---|---|---|---|
| `c8_rotation` | `c8_deserter`: pay 30 Gold, persuade (Honour 5), or frighten (Courage 6) | the watch change on the south wall. Persuading raises Honour; frightening him lowers Voss's rung; paying costs Gold | Ch8 `c8_the_horn` (the ramp costs no strength); **Ch9** the south wall | discoveries | none |
| `c8_chapel_story` | `c8_chapel`, "stand at the door with her" | Voss's rung +1; she says she hoped it would still be standing | **Ch9** the chapel option (needs rung `trusted`): "Thank you for the door" | people | Ch13 Selvane's verdict on Voss refers to the chapel |
| `c8_tithe_seen` | `c8_cart`, "watch how the tithe is taken" | the mark is a thumbprint of grey ash; the side arch | Ch8 `c8_the_horn` (the arch needs the signet and this); **Ch9** the carter's cart | decisions, tithe_cart group | Ch11: the extraction team's route |
| `c8_cart_robbed` | `c8_cart`, "take a share" | Gold +25, Honour -1, Voss's rung -1; the farmers saw | Ch8 `c8_long_night` (the farmers will not talk to a thief); **Ch9** (so no farmers' cart) | decisions, tithe_cart group | none |
| `c8_exit_ramp` | `c8_the_horn`, the slag ramp | Voss holds the foot of the ramp; Health -10 unless the rotation is known | **Ch9** the south wall costs nothing to someone who has been on the ramp | decisions, sanctum_exit group | none |
| `c8_exit_tithe` + `c8_seal_spent` | `c8_the_horn`, the tithe arch (needs the signet and `c8_tithe_seen`) | no fight, no XP, no trophy; the signet is spent; the collector recognises Voss | `c8_seal_spent`: **Ch9** `c9_the_door` (the seal opens nothing) | decisions (`c8_seal_spent` hidden) | Ch13 delegation scene addresses what it was used for |
| the sentinel fight (no exit flag) | `sentinels_close_in` | shipped fight, XP 190, Gold 50, "Out of the Ash" | the only exit that pays | none | none |
| `c8_voss_cells` | `the_price_of_escape` or `c8_capture_ramp`, when the rung is `trusted` | trophy "Her Choice": she tells him the cells are nearest the east stair; the capture was her choosing | **Ch9** the east stair (not after a day was wasted) | people | none |
| `c8_wrote_halvard` | `c8_long_night`, write to Halvard | Honour +1 | **Ch9** `c9_plans`: Halvard has already marked the plans (Ordrun's name and the ward in one go) | decisions, long_night group | none |
| `c8_ash_farmers` | `c8_long_night`, ask the ash-farmers (not if the cart was robbed) | the cells' food cart is driven in by someone who is not looked at | **Ch9** the cart; **Ch9** the way home (they hide you) | decisions, long_night group | none |
| `c8_marked_route` | `c8_long_night`, mark the road back | Health -10, Courage +1 | **Ch9** the way home (the pursuit loses the road) | decisions, long_night group | none |
| `vowed_to_return_now` / `_prepared` / `undecided_on_voss` (shipped) | `what_must_be_risked` | now: Courage +1, Health -15. Prepared: Honour +1, Gold -20. Undecided: nothing yet | **Ch9** `a_debt_remembered_*`: the three days Halvard can spare (the shipped plan scenes say three days in every telling), used in a hurry (one place at the table), properly (two), or lost to indecision (one, and the cells may have moved) | promises group | none |
| `reassured_voss` / `acknowledged_the_risk`, `moved_in_stealthily` / `circled_wide`, `stole_the_schematic` (shipped) | the road, the approach, the recon | the two answers and the two approaches are now alternatives (a replay clears the other) | none (they were never read, and still are not) | hidden (tone) or existing | none |
| `hub_a`, `hub_b`, `hub_c`, `hub_second` | the hubs | which two of three places were seen | the hub's own continue choices | hidden (bookkeeping) | none |

## Chapter 9: What Ordrun Said

| Flag | Set at | Local effect | Read by (implemented) | Journal | Planned later use (not implemented) |
|---|---|---|---|---|---|
| `moved_without_delay` / `moved_with_preparation` / `decided_at_last` (shipped) | `a_debt_remembered_*` | one place at the table; two; one, and the cells may have moved | `decided_at_last`: the east stair is no longer certain | hidden (tone) | none |
| `c9_knows_ordrun` | `c9_plans` (letter, roll, or the hunted question) | Halvard names the castellan, his twin blades, and what he does with a thing like Voss | `what_the_castellan_says` -> `c9_after_known` or `c9_after_cold` | discoveries | Ch10: Kaelen recognises the Vanguard's habits? (not proposed) |
| `c9_knows_cells` | `c9_plans` (letter or the wing) | the ward answers to the Order's mark, a binding as old as the wards, or force that shouts | `c9_the_door`: forcing the ward at its seat costs Health -5, not -15 | discoveries | none |
| `c9_kit` | `c9_stores`, rope and lantern instead of remedies | no remedies | the south wall costs nothing | decisions | none |
| (remedies, no flag) | `c9_stores` | `healing_draught` x1 or x2, by the ambush ground | the existing draught mechanic | none | none |
| `c9_stair_rules` | `c9_north_stair`, stand with Halvard | a remedy | none | stonebeard_work group | Ch10: Halvard remembers whose side you stood on |
| `c9_stair_people` | `c9_north_stair`, stand with the boy | Honour +1; a carter offers her cart | the carter's cart (needs the tithe seen and no farmers' cart) | stonebeard_work group | Ch10: the carter is on the stair at the siege |
| `c9_gate_braced` | `c9_gate_work` | Honour +1, Health -10; Halvard teaches how a ward fails | `c9_the_door`: the cheap forced ward | stonebeard_work group | none |
| `c9_way_stair` / `c9_way_wall` / `c9_way_cart` | `the_plan_takes_shape` | Honour +1; or Health -10 without a rope or a known ramp; or Gold -20 | none (the cost is paid at the time; all ways end at the same door) | rescue_plan group | Ch11 to Ch13: the Sanctum's patrols know how you came (not proposed) |
| `created_a_diversion` / `infiltrated_quietly` / `went_with_what_they_had` (shipped) | `the_plan_takes_shape` | Health -15, free, Health -10 (at the door scene) | none (never read; still not) | rescue_plan group | none |
| `c9_door_seal` | `c9_the_door`, Voss's seal | free; the ward will remember the mark | none | cell_door group | **Ch13** delegation scene: what the seal opened |
| `c9_door_nails` | `c9_the_door`, Kestrel's ward-nail | free; the nail stays in the ward | none | cell_door group | **Ch24**: the nails hold more than one door |
| `c9_door_force` | `c9_the_door`, force | Health -5 or -15; every bell hears | the way home costs Health -10 more | cell_door group | none |
| `broke_her_free` / `trusted_her_to_free_herself` (shipped) | `voss_in_chains` | trusting her raises the rung by one | the rung (which blade scene she gets, in the cell, before the fight; and the chapel option after Ordrun) | people group | Ch12, 13, 18 via the rung |
| `defeated_ordrun`, `lost_to_cinder_castellan` (shipped) | the fight | existing | none | hidden | none |
| item `ordruns_broken_blade` (shipped) | `what_the_castellan_says` | granted on either result | none | none | none |

## Counts

Flags this package adds to what the story can set: 48 (Chapter 7: 20, plus the third rung, which is set through a helper; Chapter 8: 11
plus the four shared hub markers; Chapter 9: 12). Of the 44 that are not hub bookkeeping, **30 have at least one reader in the story
today** (counting `c7_rung_wary`, which is read by the absence of the other two rungs), including every flag Chapter 9 reads from
Chapters 7 and 8. **14 have none**, and each is a recorded choice whose reader the blueprint planned for Chapters 10 to 24:
`c7_traded`, `c7_shut`, `c7_page`, `c7_comfort`, `c7_quiet`, `c7_asked_about_riders`, `c7_voss_hurt_deeper`, `c8_exit_tithe`,
`c9_stair_rules`, `c9_way_stair`, `c9_way_wall`, `c9_way_cart`, `c9_door_seal`, `c9_door_nails`. They cost about 1.2 KB each in the
worst-case checkpoint bound, 16 KB together, which is part of the checkpoint growth question (see `qa/CHECKPOINT_GROWTH_INVESTIGATION.md`).

## Review obligations for the Chapters 10 to 12 package

Approved on 26 Oct 2026: the 14 unread flags stay, and the package that writes Chapters 10 to 12 must review every flag assigned to it
below before it is called finished. For each, the outcome is one of: a reader written (and the ledger row changed from "planned" to a scene
id), a reason recorded for moving the payoff to a later chapter, or a decision to retire the flag. Nothing in this table is implemented.
Where a row says "planned", it is a plan.

| Flag or family | Assigned to | What the plan says | State today |
|---|---|---|---|
| `c7_traded`, `c7_shut`, `c7_page`, `c7_comfort`, `c7_quiet` (and `c7_halvard_open`, read only at the promise to Edda) | Chapter 10 Last Evening (T10-01), then Chapter 13 | what Halvard says in his last days depends on what passed between him and Kaelen at the forge | planned, unread |
| `c7_voss_hurt_deeper` | Chapter 10 | Voss is "still healing" | planned, unread (Chapter 10's existing line is not conditional) |
| `c9_stair_rules` | Chapter 10 | Halvard remembers whose side Kaelen stood on | planned, unread |
| `c9_stair_people` (read in Chapter 9 by the carter's cart) | Chapter 10 | the carter and the refugees are on the stair at the siege | planned; Chapter 9 reader implemented, Chapter 10 reader not |
| `c7_hold_terms` / `_refused` / `_hidden` (read in Chapter 9 by the table) | Chapter 10 | refugees inside the Hold at the siege, outside it, or promised and unspent | planned; Chapter 9 reader implemented, Chapter 10 reader not |
| `c7_ground_yard` / `_stair` / `_road` (read in Chapter 9 by the stores) | Chapter 10 | what the Hold has at the siege: no fine edge, a known tunnel, Ilsevet already knows who stands beside the jailer | planned; Chapter 9 reader implemented, Chapter 10 reader not |
| `c8_tithe_seen` (read in Chapters 8 and 9) | Chapter 11 | the extraction team's route to Fenmoor is known | planned; Chapter 8 and 9 readers implemented, Chapter 11 reader not |
| `c7_rung_*` | Chapter 12, then 13 and 18 | Voss's answers and the cost she has paid | planned; Chapters 8 and 9 readers implemented, later readers not |
| `c9_way_stair`, `c9_way_wall`, `c9_way_cart`, `c7_asked_about_riders`, `c8_exit_tithe` | no chapter assigned | Journal records only; the blueprint named no reader | no plan: the review must keep, assign or retire each |

Planned for later chapters, not for this review (tracked so they are not forgotten, and still unimplemented): `c7_voss_called_them_off`
(Chapter 13, Selvane; Chapter 18), `c8_chapel_story` (Chapter 13), `c8_exit_tithe` / `c9_door_seal` (Chapter 13's delegation scene),
`c9_door_nails` (Chapter 24).

## Items and trophies

- No new item. Two existing items now matter more: `healing_draught` (Chapter 9 gives one or two, by decisions made in Chapter 7) and `ward_nails` (it opens a ward).
- Two trophies, both on choices, none for a fight: **"Called Off"** (Chapter 7, the Loyalists sent away) and **"Her Choice"** (Chapter 8, two routes: the sentinel fight and the ramp; a Voss who trusts Kaelen tells him where the cells are).
  `TrophyBackTest` counted 17 trophy sites; it counts 20.

## Prices and rewards, side by side

| Chapter 8 route out | Cost | Reward |
|---|---|---|
| Fight the sentinel | the fight | XP 190, Gold 50, "Out of the Ash"; with a trusting Voss, "Her Choice" |
| Slag ramp, rotation known | the deserter's price, earlier | nothing lost; with a trusting Voss, "Her Choice" |
| Slag ramp, on your legs | Health -10, Courage 4 | same |
| Tithe arch | the signet is spent; Voss is recognised and gives no cells | no fight |

| Chapter 9 way in | Cost | What it needed |
|---|---|---|
| Loud | Health -15, Courage 8 | nothing |
| Quiet | nothing, Honour 7 | nothing |
| Default | Health -10 | nothing |
| East stair | nothing, Honour +1 | Voss told you where; no day wasted |
| South wall | rope: nothing; known ramp: nothing; neither: Health -10 | the rotation |
| Farmers' cart | Gold -20 | the long night spent with the ash-farmers |
| Carter's cart | nothing | sided with the boy at the north stair; the tithe seen |

Every way ends at the same door and the same fight with Ordrun. No route in Chapter 8 or 9 frees Voss before the chains, keeps her in the party
afterwards, or lets her rung decide whether she is captured or rescued.

## Chronology (as written)

| When | What | Who is there |
|---|---|---|
| The night after Greymoor | Halvard and Kaelen at the dead forge; the trade, the letter, the promise to Edda | Halvard |
| The next day | Voss returns; the ambush; the signet; the hearth | Voss, Halvard |
| Three days north, two of them on the Ash Road | the camp, the deserter, the chapel, the cart; on the third day she says what she has been carrying | Voss |
| The Sanctum | the recon, the horn, the way out, the capture; the long night | Voss is taken |
| Three days (every telling) | Stonebeard's long table; Halvard's blade for Voss | Halvard, Edda's people |
| The Sanctum again | the way in, the ward; the blade laid beside her in the cell, the chains, Ordrun | Voss, Ordrun |
| The run home | Stonebeard's tunnel | Voss, then Halvard (Chapter 10 begins with her "checking the edge on a borrowed blade") |

## Canon held

- Ch10: Halvard dies; Ch13: his rites. Nothing here touches either. The blade he lends Voss is a loan he will not get back; this is deliberate and unstated.
- Continuity checked before the commit (26 Oct 2026): the shipped plan scenes say Halvard can spare "three days" and `into_the_ash_default` says "the last three days of preparation", so every telling of Chapter 9 now has three days (the vow decides how well they are used, not how many there are). The shipped defeat scene has Voss kill Ordrun "to her blade", so Halvard's blade is laid beside her in the cell before the chains come off and before the fight, not handed over afterwards. No shipped text was changed to make either true.
- Ch5: the envoy's fate is unresolved. Chapter 5's expansion (826d719) is in this branch, and it names its decisions without the `c5_` prefix the blueprint used; Chapters 7 to 9 read it only through `kept_the_offer_from_halvard` (the forge scenes), as Chapter 6 already did.
- Voss's reason for leaving the Order is not written. The chapel stays hers to explain; the only scene that touches it ends with "If I tell you, it will be because I've decided to."
- Ordrun dies in Chapter 9 on either result of the existing fight (existing scenes). What is said of him rests on a name on a roll: castellan, twin blades, finishes with the second, fetches what Ilsevet wants. No family, no history.
- Emberlow is named only in Ordrun's last words, as before. Chapter 23's "Emberlow's warden died alone eleven years ago" is not pre-empted.
- The courier material was omitted. The freed scout is not committed to being the deserter.
