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
- **B, required before Chapters 10 to 12:** a seeded walk of the real story that finds the largest flag, item and trophy set one playthrough holds
  at a checkpoint (137 flags, 274,511 bytes today), and a test that pins that figure and the seed. The worst-case diagnostic stays and keeps its marker.
- **C, not taken:** no flag was dropped to satisfy the guard.
