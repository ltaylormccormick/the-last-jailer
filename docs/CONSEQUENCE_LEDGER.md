# Consequence ledger: tracked issues

Entries here are decisions or inconsistencies to resolve in the consequence-ledger review that precedes the Chapter 4+ redesign. Nothing below is changed by Batch A.

## Replay: the tarnished token (pre-existing)

- `kept_the_token` and `revealed_token` are not in any replay reset family, so they survive a new telling.
- The physical `tarnished_guard_token` also survives in the inventory.
- A later telling can therefore describe burying the token (`dwarven_path_buried`) while the "Show him the tarnished token from the roots" row still appears for Halvard, because the flag and item are still held.
- Behaviour is consistent with Kaelen still carrying the item, but the prose can contradict the player's current decision.
- Not changed in Batch A. Replay and inventory behaviour must not be altered casually; resolve as part of the ledger review.

## Related, noted in the Chapters 1–3 audit

- No Chapter 1–3 flag is read by Chapters 4–30 (only stats, gold and items carry forward).
- `word_at_stonebeard` mentioned a coil of rope that is never granted. **Fixed in Batch C** (the smith now presses only a second lantern into Kaelen's hands).

## Batch B decisions (Chapter 3 pacing and the Order's camp)

- **Refusal sequence.** "I need time" now plays `the_walk_away` (the walk, the Hold, the tremor merged into one scene) then `the_return`, then the rite: two short scenes instead of four. The single +1 Honour still comes from "Go back." (moved from `the_ground_remembers`). `refused_once`, the binding, the brand and the trophy are unchanged.
- **Retired scenes kept.** `what_the_road_offers` and `the_ground_remembers` are no longer reached by new play but are kept, unchanged, so a save parked on either still plays on. `StoryContentIntegrityTest` lists them as parkable.
- **Voss and the camp.** `ch3_camp_robbed` leads to `ch3_surface_robbed`; `ch3_camp_parley` leads to `ch3_surface_parleyed`. Voss suspects the theft ("I'm sure that's a coincidence") but is not shown to know it; after the parley she has been told by the sentry and notes the manners. No stat, item or combat change. Studying the camp or walking straight down is unchanged.

## Remembered where, and where it stops

| Decision | Remembered in | Pays off |
|---|---|---|
| Robbed the Order's stores (`robbed_the_order`) | Voss's arrival (`ch3_surface_robbed`), Journal | Chapter 3 only |
| Parleyed with the sentry (`parleyed_with_sentry`) | Voss's arrival (`ch3_surface_parleyed`), Journal | Chapter 3 only |
| Refused the office first (`refused_once`) | Journal, the rite (`the_binding_rite_returned`) | Chapter 3 only |

## Still unresolved

- **Replay-token inconsistency** (above) stays open.
- No Chapter 1–3 flag, including the new ones, is read by Chapters 4–30. Cross-chapter callbacks are deliberately not part of Batch B.
- Voss's later trust in Chapter 4+ does not yet reflect the theft or the parley.
- A thief who takes the camp route still skips Halvard's ledger scene (`ch3_halvards_ledger`).
- `smith_vouched` and the lantern remain cosmetic beyond Chapter 2.

## Batch C: continuity, Stonebeard economy, token replay

### Continuity corrections (prose only; no flags, items, numbers or node IDs changed)

| Scene | Before | After |
|---|---|---|
| `ch1_knock_resumes` | "Kaelen sheathes the blade and stands in the rain, listening." (the player had already chosen "Sheathe the blade." or "Wipe the blade.", so the blade was put away twice) | "The blade is back in its sheath, and Kaelen stands in the rain, listening." Then, after the knocking: "His hand goes back to the hilt before he has decided anything." This also explains the protected redraw in `sword_drawn` ("Kaelen draws his sword. The motion is old habit...") without touching that scene. |
| `word_at_stonebeard` | "presses a coil of rope and a second lantern into Kaelen's hands" (no rope is granted) | "presses a second lantern into Kaelen's hands" |
| `ch3_camp_robbed` | "A sack of coin set aside for fodder." | "A purse of coin set aside for fodder." (matches Voss's "A purse of coin has gone missing" in `ch3_surface_robbed`) |

Horse and rider counts were re-checked across `ch3_order_camp`, the three arrival scenes, `the_surface_again` and the `chapter3_end_*` departure scenes: Voss plus four riders, five horses, five bedrolls everywhere. No further contradiction was found. Protected legacy scenes (`black_door_opened`, `sword_drawn`, `walked_the_road`) are untouched.

### Stonebeard economy (analysis only; no numbers changed)

Basis: a healing draught costs 40 gold in the shop and heals 25 HP, so 1 HP is 1.6 gold and a draught is 40 gold-equivalent. Start: 25 gold. Each scene transition also gives +1 passive HP, which is ignored below. Figures are relative to "Move on without a word" (0).

| Route at the Hold, then at the smith | Gold | Honour | Draught | Other | Net value |
|---|---|---|---|---|---|
| Help him repair the gate | 0 | 0 | +1 | token, trophy, vouch | +40 |
| Tell him honestly | 0 | +1 | 0 | vouch | 0 |
| Move on without a word | 0 | 0 | 0 | none | 0 |
| Rob the bench, then **pay 40** | +15 - 40 = -25 | -1 + 1 = 0 | +1 | lantern | -25 + 40 = **+15** |
| Rob the bench, then **work the shift** | +15 | -1 + 1 = 0 | +1 | lantern; -10 HP (= -16) | 15 + 40 - 16 = **+39** |
| Rob the bench, then **say nothing** (or never return) | +15 | -1 | +1 | debt stays open | 15 + 40 = **+55** |

What the numbers show:

1. **Silence is the best material outcome (+55)**, 15 above helping the smith (+40), at a cost of 1 Honour and an open debt. The debt gets a second chance in Chapter 3 (`ch3_halvards_ledger`) at identical prices (40 gold, or -10 HP, or silence again), so declining twice is still the richest route. The lantern is the only thing silence withholds, and it has no combat effect.
2. **Working the shift (+39) is balanced against helping (+40).** It differs only by the token, the trophy and the vouch.
3. **Paying (+15) is not a penalty.** The thief spends a net 25 gold for a draught that costs 40 in the shop, and ends with Honour 0, the same as a player who helped. The Honour -1 for stealing is exactly cancelled by the +1 for restitution.
4. **Paying is not always possible.** Gold at the Hold on a thief's route: victory 25 + 15 + 10 = 50 (can pay); defeat 25 + 15 - 10 = 30 (the "Put forty gold" row is shown locked). A defeated thief reaches 40 only with earlier income (rider purse +12, or stripping the wagon +14, both -1 Honour; searching the wagon +8 gives 38, not enough). Work and silence are never locked, so nobody is stuck.
5. Honour -1 for the theft, +1 for any restitution, and +1 for returning to the Hold with word (`returned_to_hold`, given to everyone) means a repentant thief and an honest player finish level on Honour.

Proposals (none applied; each needs approval before any number moves):

- **A. Leave as is.** Defensible: honesty is rewarded by the token, trophy and vouch rather than by raw gold, and the pay/shift options are close to even.
- **B. Price the silence.** Make declining twice cost something proportionate, for example a further -1 Honour at `ch3_halvards_ledger` "Say nothing, and climb" (silence then nets +55 with Honour -2). Smallest change; leaves the 40-gold price and the 15-gold theft alone, so the reachability above is unchanged.
- **C. Tighten restitution.** Raise the price so paying cancels the theft (price 55 = draught 40 + coin 15): pay nets 0. Not recommended alone: 55 is above the 50 gold a victorious thief holds at the Hold, so almost nobody could pay until Chapter 3.
- **D. Shrink the theft.** Cut the stolen coin from 15 to 10: silence becomes +50, pay +10, shift +34. Not recommended: the 15 coin is exactly what lets a thief afford 40 (25 + 15), so it would lock payment for everyone.

Recommendation: **B** if silence should stop being the top earner; otherwise **A**. Both keep every reward and price as shipped.

`BatchCContinuityEconomyTest` pins today's numbers so any later retune is a deliberate, reviewed edit.

### Token replay inconsistency: findings and recommendation (no behaviour changed)

Confirmed by `BatchCContinuityEconomyTest`. A first telling that buries the clasp is consistent. On a later telling, `kept_the_token` and the physical `tarnished_guard_token` from the earlier telling survive: burying again sets `buried_the_past` as well, "Show him the tarnished token from the roots" is still offered to Halvard in `the_cloaks_truth_asked` and `the_cloaks_truth_offered`, and the item stays in the inventory. The Journal is already safe: the two clasp lines share the `clasp` group and collapse to one neutral line.

Why it happens: `kept_the_token` and `buried_the_past` are in no reset family; `Consequences` can grant items but not remove them; no choice reads an item, only flags.

Options:

1. **Mutual exclusion at the two choices (recommended).** "Keep the token." also clears `buried_the_past`; "Leave the past buried." also clears `kept_the_token` (and, if wanted, `revealed_token`). Uses the existing `clearFlags` field, the same pattern as `OPENING_FAMILY`; no save-format change, no new flag, no inventory change. Effect: the Halvard row always matches the current telling. Old saves that already hold both flags stay as they are until that player makes the choice again, and the Journal already copes with them. The physical item would still remain after burying.
2. **Add an additive `removeItemIds` to `Consequences` (default empty), used by "Leave the past buried."** Fixes the item as well. Touches the content model rather than the save format (inventory is already a plain list), but it is a new engine capability and needs its own tests, including `received_wax_disc`-style durable items that must not be removed.
3. **Leave it.** The behaviour is consistent with Kaelen still carrying the clasp; only the prose disagrees.

Suggested order if approved: option 1 now (pure content, easily reversed), option 2 only if the inventory showing a "buried" clasp is judged to matter. Whichever is chosen, `revealed_token` and its trophy should stay once-per-lineage.

### What still needs to carry forward into Chapters 4-30

Nothing from Chapters 1-3 is read after Chapter 3 except stats, gold and items. The decisions below are remembered only inside Chapters 1-3 (Journal aside) and are candidates for a bridge:

| Decision | Flag(s) | Remembered now | Natural payoff |
|---|---|---|---|
| Who hired the riders (kin hired riders; not revealed) | `knows_riders_hired`, `rider_clue` | Ch1-3 | Later reveal; a player who pressed the rider should be closer to it than one who did not |
| Stonebeard: helped / confessed / stole / repaid | `helped_dwarf`, `confessed_to_dwarf`, `took_from_the_bench`, `returned_the_draught`, `smith_vouched` | Ch2-3, Journal | Stonebeard's welcome and Halvard's trust when the Hold returns in later chapters (Chapter 13 rites are the obvious place) |
| The clasp | `kept_the_token`, `buried_the_past`, `revealed_token` | Ch2, Journal | Halvard and the Guard thread; once the replay issue is settled |
| The Order's camp | `robbed_the_order`, `parleyed_with_sentry` | Ch3 arrival | Voss's trust in Chapter 4+ (her reactions stop at Chapter 3) |
| The office | `refused_once`, `accepted_fully`, `accepted_reluctantly` | Ch3 rite, Journal | How Halvard and the Hold speak of Kaelen's commitment |
| Thief who skipped Halvard's ledger via the camp route | `owes_the_smith` | nowhere after Ch3 | Either let it lapse or give it one callback |

### Decision needed: how the Chapter 4-30 ledger should work

Proposal for review, not implemented: keep the ledger as design documentation rather than a new engine system. For each decision it records (1) the flag, (2) the scene where it is remembered, (3) the scene where it pays off, and (4) whether a player can reach the payoff without it. Prefer a small number of durable "standing" flags (for example the Hold's regard, Voss's trust, the clasp) that several choices feed into, over one callback per flag. Chapter 4 should be planned against this table before any prose is written.

### Still unresolved after Batch C

- Replay-token inconsistency: recommendation above, awaiting a decision.
- Stonebeard economy: options A-D above, awaiting a decision; no number has moved.
- No Chapter 1-3 flag is read by Chapters 4-30.
- Voss's later trust in Chapter 4+ does not yet reflect the theft or the parley.
- A thief who takes the camp route still skips Halvard's ledger scene.
- `smith_vouched` and the lantern remain cosmetic beyond Chapter 2.
- The redraw in `sword_drawn` after a sheathed fight is now explained by a line in `ch1_knock_resumes`; the protected scene itself still opens "Kaelen draws his sword."
