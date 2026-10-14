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
- `word_at_stonebeard` mentions a coil of rope that is never granted.

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
