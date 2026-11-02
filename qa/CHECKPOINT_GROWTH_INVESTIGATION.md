# Checkpoint growth marker: investigation after Chapters 7 to 9

Status: written on `chapters-6-9-expansion`. **Decision (26 Oct 2026): Option A approved.** `CheckpointSizeTest` is re-baselined to the
measured 530,891 bytes with the same 10% growth warning (allowance 583,979) and the independent 600 KiB hard ceiling unchanged; `qa/G2_RECOVERY_STORE.md`
records the measurement. No narrative flag was trimmed. **Option B is a required investigation before the Chapters 10 to 12 package**: a
deterministic reachable-state checkpoint measurement that complements, and does not silently replace, the worst-case diagnostic. Raw
output: `LJ_QA_evidence/ch79/checkpoint_growth_diagnostic.txt`; the one-off diagnostic that produced it is kept beside it and is not in the repository.

## What fired

| | Bytes |
|---|---|
| Thirty worst-case checkpoints after Chapter 6 (measured at 7f22cd3) | 472,493 |
| Marker (that figure plus 10%) | 519,739 |
| Thirty worst-case checkpoints now | **530,891** |
| Hard ceiling (provisional, 600 KiB) | 614,400 |

The marker fails by 11,152 bytes (2.1% over the allowance, 12.4% over the Chapter 6 figure). The hard ceiling is not close: there are
83,509 bytes between 530,891 and 614,400.

## What grew

The worst-case state puts every flag the story can set, every item it can grant and every trophy into each of thirty checkpoints.
Taking the 48 flags and 2 trophies that Chapters 7 to 9 added out of the model gives 472,481 bytes, **12 bytes from the 472,493 measured
earlier**, so the whole difference is accounted for:

| Part | Added | Bytes | Share |
|---|---:|---:|---:|
| Flags (`c7_*`, `c8_*`, `c9_*`, `hub_*`) | 48 (story total 278 -> 326) | 56,250 | 96.3% |
| Trophies ("Called Off", "Her Choice") | 2 (34 -> 36) | 2,160 | 3.7% |
| Items | 0 | 0 | 0% |

A flag costs about **1,172 bytes** at the margin over thirty checkpoints (1,364 bytes averaged over the whole set, including the
name of the flag, its key and its value in each checkpoint). The figure of "about 600 bytes per flag, headroom for about 78 more flags"
that was recorded when the marker was set was wrong by a factor of two. The real headroom to the marker was about 40 flags; to the hard
ceiling it is about 61 more.

Flags alone are 465,371 of the 530,891 bytes (88%). Left out one at a time, the 33 items account for about 19,500 bytes and the 36
trophies for about 46,000 (a trophy costs about as much as a flag).

## Does a checkpoint need all of it?

Two different answers, and the difference is the useful finding.

1. **Needed by play: far less than the model says.** The model sets all 326 flags at once in every checkpoint. No playthrough can: the
   decisions are exclusive families (the rung ladder, the ambush ground, the hold answers and so on), every chapter clears what it owns when
   it starts, and the hub markers vanish when a hub is left. In 2,500 random whole-game walks the most flags ever set at once was
   **137** (median 121, 99th percentile 131), out of 326 that were each set by some walk. Thirty checkpoints each holding that largest
   realistic set measure **274,511 bytes**, 52% of the worst case. Both figures are for a history bundle (G3) that does not exist; the live
   save is about 7 KB and is unaffected.
2. **Read by a gate: 86 of 326.** The other 240 flags are never required or forbidden by a choice. They exist because the Journal shows
   them and because later chapters are meant to read them; a checkpoint that restores a save has to restore them too, or the Journal
   would lose lines. So "unread" is not a reason to leave a flag out of a checkpoint, but it is a fair test of whether a new flag earns its keep.
   Of the 48 this package added, **14 have no reader at all** (listed below), and `c7_rung_wary` is read only by the absence of the other two rungs.

## Options

| | What it does | Cost | Effect on the marker |
|---|---|---|---|
| **A. Re-baseline** | Set the measurement to 530,891 and keep the 10% guard (allowance 583,980) and the 600 KiB ceiling | none to the story; one number and this document | passes; the next package of the same size (about +58 KB) would fire it again and cross the ceiling's 90% line |
| **B. Measure what play can hold** | Keep the worst-case figure as a diagnostic but guard the *realistic* bound: flags a play can hold at once, found by walking the real story (about 274 KB today), with the 600 KiB ceiling kept | a new deterministic test (the sampler already exists); G3's real payload decision gets a number it can use | the marker would measure something that grows only when a play can really hold more |
| **C. Trim** | Drop never-read flags that carry no Journal weight | needs about 9 of the 14 to pass without a change: `c7_shut`, `c7_asked_about_riders`, `c7_voss_hurt_deeper`, `c8_exit_tithe`, `c7_quiet`, `c9_stair_rules`, `c9_way_stair`, `c9_way_wall`, `c9_way_cart` | passes; loses the Journal lines for those decisions and their planned readers |

**Recommendation:** A now, B at the start of the next package (Chapters 10 to 12), and not C. Trimming is the wrong lever: the flags were
added to be read, and dropping nine decisions to satisfy a guard whose model is about twice what a play can hold would trade story memory
for a number. A alone buys one more package; B stops the guard from forcing the same conversation every time a package adds flags.

## The 14 flags with no reader today

`c7_traded`, `c7_shut`, `c7_page`, `c7_comfort`, `c7_quiet`, `c7_asked_about_riders`, `c7_voss_hurt_deeper`, `c8_exit_tithe`,
`c9_stair_rules`, `c9_way_stair`, `c9_way_wall`, `c9_way_cart`, `c9_door_seal`, `c9_door_nails`. The ledger (`qa/CHAPTER79_CONSEQUENCE_LEDGER.md`) says where the blueprint planned a reader for each.

## Decision

- **A, approved and applied:** `measuredAfterChapter6 = 472_493` became `measuredAfterChapter9 = 530_891`; the test, its comment and its failure
  message say Chapter 9; the hard ceiling and the 10% rule are unchanged. `G2_RECOVERY_STORE.md` has a new paragraph with the table above.
  (The test's name changed with it, so a comparison against an older run shows the old name gone and the new one added.)
- **B, required before Chapters 10 to 12: done on 26 Oct 2026 (M0), see "Option B result" below.** The worst-case diagnostic stays and keeps its marker.
- **C, not taken:** no flag was dropped to satisfy the guard.

## Option B result (M0, 26 Oct 2026, on 8be2d98)

`CheckpointReachableSizeTest` and its walker `ReachableCheckpointMeasure` (both new test sources; `CheckpointSizeTest` is byte for byte unchanged).

| Figure | Bytes | Share of worst case |
|---|---:|---:|
| (a) Worst case: thirty checkpoints, every settable flag, item and trophy (unchanged test) | 530,891 | 100% |
| (b) Thirty arrival records, each the largest found for its chapter | **161,198** | 30% |
| (c) The largest single walk's history (30 records) | 160,911 | 30% |
| Earlier estimate: 137 flags, every item and trophy, in all thirty | 274,511 | 52% |

Method. 3,000 uniform walks (any takeable choice at random) and 3,000 collector walks (seven choices in eight take the one adding most
flags, trophies and items; fights mostly won) from seed 0x10C5, 1,636,468 transitions in all, each using the real `applyChoice` and
`resolveCombat`. At each forward chapter boundary the record `CheckpointRecord.fromState` would build (the call `ArrivalRecorder` makes) is kept
if it holds more stored text than any earlier record for that chapter; sizes are measured through the real store in the same XML model as the
worst case. Coverage: all 30 chapters, all 326 settable flags, no forward jump of other than one chapter. Most flags held at once: 139 (137 in a
single arrival record), agreeing with the earlier diagnostic. Why the earlier estimate was high: a flag set in Chapter 10 is in 21 arrival records, not
30, and the early chapters hold far fewer (Chapter 7: 53 flags, Chapter 10: 78, Chapter 30: 131; trophies 9, 14, 25; items 13, 17, 34).

The guard. `reachableAfterChapter9 = 161,198` with a 10% growth warning (allowed 177,309), the seed and walk counts pinned, a coverage check
(all chapters, at least 95% of settable flags), a repeatability check, and a check that the figure is below the worst case, within rounding of the largest single
walk, and under the 600 KiB ceiling. It sits beside the worst-case test; neither replaces the other, and either one crossing its limit is investigated.

Negative controls (Chapter10.kt edited, two checkpoint test classes run, file restored and hash verified):

| Control | What it does | Reachable guard | Worst-case guard |
|---|---|---|---|
| C1 | 40 flags every play holds from Chapter 10 to the end | **fires**: 193,198 (+19.8%) | passes: 578,891 (+9.0%, under its 10% allowance) |
| C2 | 70 flags set for two scenes, cleared before the chapter ends | passes: 161,198 (no arrival holds them) | **fires**: 608,591 (+14.6%) |

So the reachable guard moves when a play can hold more, the worst-case guard moves when the story can set more, and they disagree in the way the design intended.

What the figure does not cover. It is a lower bound: a walk that never met a state does not prove it unreachable (the two policies and a
single walk within 0.2% of the per-chapter bound are the evidence it is close, not a proof). Shop purchases and items spent in a fight are not modelled, so the inventory
field of a real checkpoint can be larger than sampled by a few hundred bytes per record. The sample is seeded, so a change to the story's graph changes the walks as well as the
content: a move of a few percent in either direction after a chapter package is noise, ten percent is not.

## The nine ending trophies (Batch 1, 30 Oct 2026, on efdc2bd plus the Batch 1 working tree)

The ending trophies (`The Tale Told`, the three legacies, `Borrowed Strength`, `Unaided`, `Her Chains`, `Her Freedom`, `Her Fate Unwritten`) are
added to the trophy set when a telling reaches an ending or replays from one. They are deliberately not in `StoryRepository.availableTrophies`
(still the original 37), so the three tests that build a worst case from `availableTrophies` now add `EndingTrophies.all` explicitly, and
`EndingTrophySizeTest` measures the replayed case. No limit and no pinned number was changed.

| Figure | Before | With the nine | Limit in the test | Headroom |
|---|---:|---:|---:|---:|
| Thirty worst-case checkpoints (`CheckpointSizeTest`) | 551,681 | 562,241 (+10,560; 352 per record) | pinned 530,891, +10% = 583,979; ceiling 614,400 | 21,738 to the marker |
| One worst-case checkpoint | 18,579 | 18,931 | 8,000 to 20,000 | 1,069 |
| Thirty reachable arrival records, first telling (`CheckpointReachableSizeTest`) | 165,634 | 165,634 (unchanged: no ending trophy exists before the Chapter 30 arrival record) | pinned 161,198, +10% = 177,309 | 11,675 |
| Same, replayed (the nine in every record; `EndingTrophySizeTest`) | | 176,194 | 177,309 | **1,115 (0.6%)** |
| Worst-case save slot with its Back snapshot (`RecoveryPayloadSizingTest`) | 17,004 | 17,258 | 256 KiB provisional | large |

Two things to know. The worst-case figure was already 3.9% above its Chapter 9 pin before this batch (the pin has not been moved since; the
marker is 10%), and it is 5.9% above with the nine. The replayed reachable figure is the thin one: any further growth in what a play holds
will reach its marker first, and the investigation procedure above applies when it does.
