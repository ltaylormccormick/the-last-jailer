# Chapter 6 consequence ledger (M1)

Status: written on `chapters-6-9-expansion` (from 826d719); uncommitted. This is a design record, not player text.
"Planned" entries are proposals for Chapters 7 to 9 and later. None of them is implemented, and none needs an engine or save-format change.

## What the chapter owns

Every flag below is cleared by the chapter's own first decision (`the_choice_of_roads`, both choices), so a replay that reaches
Greymoor again starts with an empty road and an empty yard. Chapter 5 is not edited. The three decision families
(opening, gate approach, verdict) also clear themselves on every choice that sets a member.

| Flag | Set at | Local effect in Chapter 6 | Journal | Planned later use (not implemented) |
|---|---|---|---|---|
| `c6_halvard_confided` | Methodical road, after `told_halvard_the_truth` (or no Ch5 flag) | Halvard gives Kestrel's history in full: one letter nine winters ago, four lines, never answered | "kestrel_talk" group | Ch7: Halvard can say more to Voss and Kaelen; Ch10: his last words about Kestrel |
| `c6_halvard_reticent` | Methodical road, after `kept_the_offer_from_halvard` | Halvard gives part, and offers to trade the rest | same group | **Ch7 must pay off the trade when this flag is set** (Halvard gives the rest in exchange for what Kaelen kept back); a different Ch10 conversation, same fate |
| `c6_stop_tollhouse/cairn/camp`, `c6_road_second` | Road hubs | Markers: which two of three stops were made | hidden (bookkeeping) | none |
| `c6_helped_refugees` + `c6_met_tam` | Tollhouse, "stay the night" | Honour +1, Gold -15, trophy "Kestrel's Apprentice"; Tam's method and nine nails | "tollhouse" group (helped) | Ch10 siege: Tam is at the Hold; Ch13 rites: Tam at the lamp |
| `c6_pressed_edda` | Tollhouse, "ask and press on" | Free; Edda says how the cellar hatch sticks, which saves Kaelen's hands (no Health cost at the hatch) | "tollhouse" group (pressed) | none planned |
| `c6_promised_tam` | Tollhouse, two of three ways to take the satchel | Changes the burial scene's words; the promise is paid if he buries her | PROMISES | Ch24: Tam recognises the nails or the grave |
| `c6_offered_the_hold` | Tollhouse, one way to take the satchel | Kaelen has told Edda that if her people have nowhere else, he will answer to Halvard for them. Halvard has not agreed and the scene says the door is Halvard's to open; the promise is Kaelen's own | PROMISES | **Ch7 must acknowledge it when set** (Halvard learns Kaelen spoke for the Hold; a short beat, not a new system). The larger payoff is Ch10: the refugees are inside the Hold when the siege comes. Double-edged on purpose |
| `c6_nail_method` | Tam's account, or reading the ledger | Gate for the pinning | DISCOVERIES | Ch24: the ward recognises the method |
| `c6_has_ward_nails` | Tam's satchel (nine) or the cairn tin (five) | Gate for the pinning | hidden (redundant) | none |
| `c6_took_cairn_note` | Cairn | Kestrel's own words, including that she wrote to Stonebeard and had no answer | DISCOVERIES | Ch10/Ch13: Halvard reads it |
| `c6_camp_sneaked` | Ford, wade | Courage +1; overhears the riders' orders ("the bearer isn't to be spent") | "ford" group | Ch7+: Cinder does not know his face yet |
| `c6_camp_paid` | Ford, walk up | Gold -20; the riders write down his face | "ford" group | Ch7/Ch8: the Marshal's people know his face and his brand |
| `c6_scout_taken` + `c6_scout_freed` | Ford, take a rider alive, let him go | Honour +1; learns "ask, then wait to see whether asking was enough" and that nobody there carried a chisel | "ford" group (freed) | **No commitment** that he is the later deserter; the flag only records that he was freed |
| `c6_scout_taken` + `c6_scout_bound` | Ford, take a rider alive, bind him | No stat change; same information | "ford" group (bound) | his report reaches the captain |
| `c6_spot_watchpost/cellar/garden`, `c6_ruin_second` | Yard hubs | Markers: which two of three places were seen | hidden (bookkeeping) | none |
| `c6_read_ledger` | Warden's post | Method learned (if not already), trophy "The Ledger, Read to the End"; the ledger is left open on her table | DISCOVERIES | Ch24 (existing canon): ledger "still on the table, open to an unfinished entry" |
| `c6_inspected_breach` | Cellar | Gold +25 and a healing draught from her cache; the wards broke outward, with one fresh chisel notch on the outer face (cause left open) | DISCOVERIES | Ch24: the ward's grief is not settled by this |
| `c6_buried_kestrel` | Cairn row | Honour +1, Health -6, trophy "Buried Properly" | DECISIONS | Ch24: grave is there when Kaelen returns; "search party" can find the grave, not a body |
| `c6_unbound_pinned` | Dusk, nails | Courage +1, XP +120, no damage, keepsake `ward_nails`, trophy "Nails in the Dark"; the creature is drawn into the threshold | DECISIONS | Ch24: the nails hold the crack; the ward-wraith's keening is the ward, not this creature |
| `c6_tam_followed` | Defeat, with Tam | Health +20 instead of +8; Tam leaves the ledger where it lies | PEOPLE | Ch10/Ch13 as above |
| `lost_to_unbound_horror` | Defeat (existing) | Existing bookkeeping | hidden | none |
| `sealed_greymoor_personally`, `reported_greymoor_honestly`, `left_greymoor_as_is` | Verdict (existing) | Fight route: seal costs Health 10, report costs Gold 10, leaving is free. Pin route: seal is free, report costs Gold 10, leaving is free | existing group | Ch7 reads them as before |

## Item

`ward_nails` ("Kestrel's Ward-Nails"): the nails left over once three were driven into Greymoor's crack and left there (six from Tam's nine, two from the cairn's five, more if both were taken). Keepsake, no combat effect, never unique equipment, granted only by the pinning choice. It gates nothing and nothing reads it.
Combat balance is unchanged. Future significance is limited to recognition (Ch24, Ch13), never power.

## Prices and rewards, side by side

| Route | Cost | Reward |
|---|---|---|
| Fight | Health risk (11 to 18 per hit) | XP 150, Gold 40, trophy "What Got Out" |
| Pin | Upstream: Tam's night (Gold up to 15) or cairn plus ledger (two hub slots); Courage 5 | Courage +1, XP 120, no Health loss, keepsake, trophy, free seal |
| Defeat with Tam | The tollhouse night | Health +20, Tam's company |
| Defeat alone | none | Health +8, and the plain cost of being alone |

Gold costs take the stated amount or whatever the purse holds, never below zero, and the scene text says so: the tollhouse ("what he can spare"), the ford ("twenty, or as much of twenty as he has"), the report ("it, or what his purse can find toward it").

## Chronology (as written)

| When | What | Who is there |
|---|---|---|
| Midwinter | A courteous rider offers to take the weight; Kestrel asks who it goes to. Ward-stones begin to go dark, a stone a week. Kestrel writes "I will not write twice" about Stonebeard | Kestrel |
| Weeks later, this month | Dogs shy of the miller's kitchen; the cellar hatch is warm. Kestrel sends the children south with Tam and tells the village to follow. The people look back from the hill road and every lamp is out | Kestrel alone at the post; Edda, Tam and the village on the road |
| That night | Kestrel's last entry ("The last carts are over the hill") breaks mid-sentence. The gate is broken from inside; the creature is out; she dies at the threshold | **Kestrel alone** (Tam is south) |
| Days later | Riders arrive on the ridge ("we got here after"), then camp at the ford; the refugees camp at the burned tollhouse | |
| Kaelen | Three days on the north road, two halts (the tollhouse is the only one that costs a night). Reaches Greymoor by day; the yard hub fills the afternoon; the creature comes at dusk | |
| Tam, if Kaelen stayed the night | Leaves the tollhouse a morning after him, reaches the village as the light goes, hears the fight from the street, finds him afterwards | Never at the death; never before the fight |

## Canon held

- Ch24: ledger left open to an unfinished entry, which stops mid-sentence; locket carried; Kestrel died alone (Tam arrives only after the fight); "empty watch-post"; a search party comes later.
- Ch10: Halvard dies; Ch13: his rites. Nothing here touches either.
- Ch5: the envoy's fate is unresolved. Chapter 6 only reads `kept_the_offer_from_halvard`, and treats "no flag" as the told version.
- Voss is not named in any new scene. Her history is untouched.
- The riders carry a closed hand around a flame, the same mark as the missive's wax. "The bearer isn't to be spent" is left unexplained until a later payoff.

## Binding obligations for the Chapters 7 to 9 package

Decided by the author at the Chapter 6 review. Chapter 6 only sets the flags; these are for the later package.

1. **Refuge promise (Ch7).** When `c6_offered_the_hold` is set, Chapter 7 acknowledges it: Halvard learns that Kaelen spoke for the Hold without asking. A short beat with a real consequence, not a new system. The larger payoff is Ch10 (refugees inside the Hold during the siege).
2. **Halvard's trade (Ch7).** When `c6_halvard_reticent` is set, Chapter 7 pays off the trade Halvard offered: he gives the rest of what he knows about Kestrel in exchange for what Kaelen kept from him at the end of Ch5. When `c6_halvard_confided` is set there is nothing to collect.
3. **The bearer.** The riders' order that "the bearer isn't to be spent" stays unexplained until a suitable later payoff.
4. **The broken stone.** The cause stays ambiguous (burst outward, one fresh chisel notch on the outer face, a scout who says "we don't carry chisels").
5. **The pinned creature** stays contained in the crack and distinct from the ward's grief in Ch24 (the gate's own ward went dark before the creature reached the yard).
6. **The freed scout** is not committed to being the later deserter.

## Open questions for the author

1. `ward_nails` in Ch24 and Ch13: recognition only, or a small choice?
2. Whether the Ch7 acknowledgement of the refuge promise should differ if Kaelen also reported or sealed (probably not).
