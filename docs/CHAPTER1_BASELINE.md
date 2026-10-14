# Chapter 1 baseline (C1-0)

Captured from `main` at `9c388da`, before any Chapter 1 redesign work. The tests that pin everything
below live in `app/src/test/java/com/thelastjailer/app/` (`Chapter1OpeningBaselineTest`,
`Chapter1LegacyNodesTest`, `Chapter1SaveCompatibilityTest`, `Chapter1ReplayBaselineTest`, helper
`data/Chapter1Paths`). This document explains them; the tests are the source of truth.

## Branch arrangement

C1-0 lives on `chapter1-c1-0-baseline`, cut from `main`, with its own draft PR against `main`.
It is tests and this document only, so it depends on **nothing** from Phase 0 (PR #107) and measures the
story exactly as it ships today. The same tests were also run on top of the Phase 0 branch and pass
there, so the baseline is valid on both.

C1-1 onward needs Phase 0 (`Consequences.clearFlags`). Those batches must therefore be based on the
Phase 0 branch (or on `main` after Phase 0 merges). That dependency is recorded here deliberately; it is
not accidental. This branch should be merged (or rebased) before C1-1 begins so the baseline tests ride
along with it.

## The opening today

`fallen_knight` offers three choices; each leads through one single-purpose node to `beneath_the_roots`.

| Choice | Courage | Honour | Flag | Route |
|---|---|---|---|---|
| OPEN THE DOOR | +2 | | `opened_black_door` | `black_door_opened` |
| DRAW YOUR SWORD | +1 | | `drew_sword` | `sword_drawn` |
| WALK AWAY | | +1 | `walked_away` | `walked_the_road` |

Then: token (Keep: Courage +1, `kept_the_token`, item; or Bury: Honour +1, `buried_the_past`), the
Forge (call out: Courage +1; wait: Honour +1; sneak: Courage +1, needs `drew_sword`), Stonebeard Hold
(help; confess, needs Honour 1; move on), a smith aftermath node, then the first fight.

## Pinned results

- 53 selectable paths from `fallen_knight` to `first_blood`: 15 via the door, 22 via the sword, 16 via the walk.
- **Every path reaches the first fight with 25 gold, 100 health, 0 XP and level 1.** The opening neither
  costs nor pays gold, health or XP. Only Courage, Honour, items, trophies and flags differ.
- Courage at the first fight: door 3 to 5, sword 2 to 4, walk 1 to 3. Honour: door 0 to 3, sword 0 to 3, walk 1 to 4.
- The first fight: Cave Lurker (40 HP, 4 to 9 damage), 40 XP, 10 gold, trophy "First Blood". A victory
  leads to `chapter1_end`; a defeat to `first_blood_defeat` (no reward; health stabilises at 45%).
- The full per-path table (stats, items, trophies, flags) is the `GOLDEN` map in `Chapter1OpeningBaselineTest`.

## Legacy nodes that must survive

All 15 Chapter 1 node IDs are snapshotted. `black_door_opened`, `sword_drawn` and `walked_the_road` are
pinned further (text hash, artwork, exits) because the redesign reuses them unchanged. The only edges
between Chapter 1 and other chapters are the six "Begin the tale again" nodes into `fallen_knight`, and
Chapter 1's exits `first_blood_defeat` to `word_at_stonebeard` / `the_tended_dark` and `chapter1_end` to `the_deeper_dark`.

## Save compatibility

Fixtures write the raw preference keys an existing install already has, with non-default stats, duplicate
items, trophies and later-chapter flags, so they do not depend on a new game's starting values. A save on
any of the 15 scenes loads exactly and can be played forward. None contain any redesign flag. Unknown
flags round-trip safely. A ratchet test proves no current choice requires, forbids or sets any of the 14
planned redesign flags; each later batch that introduces one must update it in the same commit.

## Replay and other behaviour pinned as-is

| Behaviour today | Expected to change in |
|---|---|
| Replay keeps stats, level, XP, gold, items, trophies and all flags; only adds "The Watch Goes On" | unchanged (by design) |
| ~~Route flags stack across tellings (`opened_black_door` and `walked_away` can both be set)~~ | **Changed in C1-2**: every `fallen_knight` choice clears the opening family (see below) |
| ~~A stale `helped_dwarf` makes the buckler offer appear after "Move on" on a replay~~ | **Changed in C1-1**: the hold clears the Stonebeard family and the dead buckler offers are removed (see below) |
| Replay grants the Stonebeard token and draught a second time | separate ticket, not part of Chapter 1. **The buckler is no longer re-granted** (Batch 1: unique combat equipment is carried once) |
| New Game (`SaveStore.restart`) is a full reset | unchanged |

## Updating the baseline

A later batch that changes any of the above on purpose must edit the matching test in the same commit
and say so in its report. An unexplained failure in these tests means something changed that nobody meant to change.

## C1-1 changes (intentional)

- The two unreachable "Take the buckler he offers." choices are removed from `smiths_warning_confessed` and
  `smiths_warning_moved_on`; each now has one exit ("Heed his warning and go on."). Node IDs are unchanged.
  The reachability known-dead list is now empty. No fresh-game path changed, so the 53-path golden table is untouched.
- Each `stonebeard_hold` choice clears the Stonebeard family (`STONEBEARD_FAMILY`: `helped_dwarf`,
  `confessed_to_dwarf`, `took_buckler`) and then sets its own flag. It fires when a choice is taken, never on
  entering or loading a scene, and no later scene clears anything. Existing saves are not modified on load.
- `Chapter1ReplayBaselineTest` now asserts the new replay behaviour; `Chapter1StonebeardResetTest` covers the rest.
- Still as before: replay re-grants the Stonebeard token and draught (separate ticket; the buckler is protected since Batch 1); the opening route flags still stack (C1-2).

## C1-2 changes (intentional)

- **Route A.** `OPEN THE DOOR` now leads to the new `ch1_door_witness`, whose single exit "Go through." leads to the
  unchanged `black_door_opened`. Courage +2 is still granted by the opening choice; the route costs no HP, gold or items.
  Route A flags: `opened_black_door` and `hunted_on_the_road` (opening choice), `hunters_saw_door` and `met_red_cord_rider` (witness exit).
  Routes B and C (`sword_drawn`, `walked_the_road`) are untouched apart from the reset below.
- **Opening reset.** All three `fallen_knight` choices clear `OPENING_FAMILY` (`opened_black_door`, `drew_sword`,
  `walked_away`, `hunted_on_the_road`, `hunters_saw_door`, `met_red_cord_rider`) and then set their own flags. It fires on choosing, never on
  loading or entering a scene. A stale `drew_sword` can no longer unlock the Forge sneak option on a later telling.
  Later batches add their own opening flags to the family as they introduce them.
- **Baseline table.** The 15 door rows of the golden table gained the three new Route A flags; the other 38 rows and all numbers are unchanged.
  Door paths now need one extra "continue" step in the path-following tests.
- **Journal.** `hunters_saw_door` is now a visible line; `hunted_on_the_road` and `met_red_cord_rider` stay hidden internal flags.
- The legacy-node text hashes still pass untouched.

## C1-3 changes (intentional)

- **Route C (the trees).** `WALK AWAY` now leads to `ch1_into_the_trees` (a fall, HP -8, Honour +1 as before), then a real
  choice: search the gully (HP -5, keeps the gold, finds the red cord, flag `found_red_cord`) or leave it (-10 gold, no cord).
  Both reach `ch1_the_old_shaft` (sets `evaded_the_riders`, `found_the_old_shaft`) and then the unchanged `walked_the_road`.
  `hunted_on_the_road` is set by the opening choice.
- **Forge.** A fourth Forge option, "Find the old shaft and watch from above." (Courage +1, same as the other sneak),
  requires `found_the_old_shaft`, shows an unobserved scene (`ch1_watched_from_above`) and rejoins `stonebeard_hold`.
- **Reset.** `OPENING_FAMILY` gains `evaded_the_riders`, `found_the_old_shaft`, `found_red_cord`.
- **Baseline numbers.** Paths from the opening to the first fight: 85 (15 door, 22 sword, 48 trees; was 53). Door and sword rows are
  unchanged. Trees rows are regenerated: searching ends at 25 gold and 97 to 98 HP (passive regeneration returns most of the 13),
  leaving ends at 15 gold and 100 HP. The "every path ends with 25 gold and 100 HP" statement above now holds only for the door and sword routes.
- **Not changed here.** Chapter 3 `the_answer` still says "ten steps taken and reconsidered on a ruined road"; that remains a separate decision.
