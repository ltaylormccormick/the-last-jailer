# G2: dormant recovery-bundle store

Status: **dormant**. Nothing in the app calls this code. It never reads or writes the live save, restores nothing,
has no UI and recovers nothing automatically. A passing G2 proves we can **store and verify** recovery bundles. It
does **not** prove the game can recover a damaged save; that is what the later gates are for.

Code: `app/src/main/java/com/thelastjailer/app/recovery/` (plain JVM, no Android imports).
Tests: `app/src/test/java/com/thelastjailer/app/recovery/` (89 tests).

## Layout and format (version 1)

`<root>/slot_<n>/{a.bundle, b.bundle}`, with `a.tmp` / `b.tmp` used only while writing. Found by listing the
directory, so it works when the live `SharedPreferences` file is unreadable, missing or empty.

| Part | Content |
|---|---|
| header (16 B) | magic `TLJRBNDL`, format version (u16), reserved (u16, must be 0), body length (u32) |
| body | slot, generation (1..Long.MAX), random bundle id (16 B), `opKind` (u16), `payloadSchema` (u16), payload length, payload |
| trailer (32 B) | SHA-256 over every byte before it |

The payload is opaque. `payloadSchema` is the payload's own version, so G3 can define its state / Back snapshot /
trophy contents without a new container. A change to the container itself needs a new format version; older builds
then see **FUTURE_VERSION** and leave the file alone. `opKind` and `payloadSchema` values this build does not know
are preserved, never rejected.

## Classification of each buffer

ABSENT, VALID, CORRUPT (with a reason), FUTURE_VERSION (recognised from the first 10 bytes, even if larger than this
build's ceiling), and UNREADABLE (an I/O error; added beyond the four states in the brief, because "could not read"
must never be mistaken for "damaged"). CORRUPT is the only verdict that lets a write replace a file, so it is
confirmed by a second identical read; a read that disagrees with itself becomes UNREADABLE and fails closed.

## Buffer selection (all 25 ordered pairs, checked exhaustively by `BufferSelectionTest`)

| Rule | Buffers (A, B) | Action |
|---|---|---|
| 1 | either UNREADABLE | refuse `UNREADABLE_BUFFER` |
| 2 | any ABSENT (incl. FUTURE+ABSENT, ABSENT+FUTURE) | write the absent buffer (A if both); touches no existing file |
| 3 | VALID+VALID, different generations | replace the **lower** generation |
| | VALID+VALID, identical copies | replace B (no distinct state lost) |
| | VALID+VALID, same generation, different content | refuse `AMBIGUOUS_GENERATIONS` (no guessing) |
| 4 | VALID+CORRUPT, FUTURE+CORRUPT (either order) | replace the CORRUPT one |
| | CORRUPT+CORRUPT | replace A |
| 5 | VALID+FUTURE (either order) | refuse `SOLE_KNOWN_GOOD_COPY` |
| | FUTURE+FUTURE | refuse `NO_WRITABLE_BUFFER` |

Also refused before touching anything: invalid request, payload over the ceiling, generation already at
`Long.MAX_VALUE`. The store has no notion of live state, so no bundle is ever treated as redundant because it matches
it.

A payload whose **meaning** (slot, `opKind`, `payloadSchema`, payload bytes) already exists in a VALID buffer is
**skipped** (`SkippedIdentical`), and the result says whether that copy is the newest. The same bytes under a
different `opKind` or schema are a different bundle. Skipping never evicts an older distinct state.

## Outcomes of a write

`Stored` (read back and fully re-verified; says whether the directory entry was synced), `SkippedIdentical`,
`Blocked` (nothing attempted), `Failed` (a rescan **proved** both buffers unchanged, and the failure was before the
rename could have taken effect), `Uncertain` (the disk may have changed; carries a fresh rescan and whether the new
bundle is visible). The store never assumes a failure left the disk as it was and never retries on its own. A caller
that retries an uncertain write is safe: if the bundle landed, the retry is a `SkippedIdentical`.

Write protocol: ensure directories (syncing parents of anything newly created), write temp, fsync temp, atomic rename
over the target, fsync directory, read back and verify, rescan and confirm the other buffer is unchanged.

## What is guaranteed where

| Guarantee | JVM tests | Real Android device |
|---|---|---|
| I1 the sole known-good copy is never evicted | **proved** under the simulator's durability model, for all start states x every fault x 2,496 crash images | not proved |
| I2 a FUTURE file is byte-identical afterwards | **proved** (same sweeps) | not proved |
| I3 generations increase; equal-but-different is never resolved by guessing | **proved** | n/a (logic) |
| I4 only the slot's four files are touched | **proved** | n/a (logic) |
| I5 `Stored` means verified | **proved** | readback proves the bytes the OS returns, not that they survive power loss |
| fsync / rename atomicity / directory sync are durable | **modelled**, not shown | **UNVERIFIED** (G7) |
| `FileChannel.force` on a directory works on every supported Android API and filesystem | n/a | **UNVERIFIED** (G7); a failure surfaces as `Uncertain`, never ignored |
| Auto Backup captures live prefs and bundles consistently | n/a | **UNVERIFIED** (G6) |

The simulator (`SimFs`) keeps volatile and durable file contents and directory entries apart and resolves a crash
every legal way (names as synced or all applied; data synced only, half written, or fully persisted), including
a rename that persisted while the file data did not. A **negative control** (a naive in-place overwrite) is shown to
lose the only good copy in the same simulator, so the sweeps cannot pass vacuously.

## Size ceiling: provisional, not accepted

`PROVISIONAL_MAX_FILE_BYTES = 256 KiB` (262,144 B; 262,060 B of payload). Measured with the real story's worst case
(248 flags, 32 items, 30 trophies) through the real `SaveStore`:

| Data | Size |
|---|---|
| live save, one slot | ~6.9 KB |
| + Back snapshot | ~13.9 KB |
| + timeline keys (lineage, 30 chapter digests) | ~15 KB |

About 17x headroom for state + Back + trophies. **Not included:** the checkpoint *history* file. The existing
`CheckpointSizeTest` bounds thirty worst-case checkpoints at under 450 KiB, so if G3 ever put history in a bundle,
256 KiB would not be enough; history copying is the deferred 2C-1b-2 work. The ceiling should be confirmed or
changed when G3 fixes the real payload.

## Test evidence

89 tests: codec 21, selection table 3, store behaviour 30, fault injection 15 (469 scenarios), crash simulation 5
(2,496 crash images), real-filesystem adapter and concurrency 10, dormancy 4, size measurement 1.
Mutation check: 20 deliberate breaks of the safety rules (sole-copy rule, double read, missing fsync, missing
parent-directory sync, wrong buffer, digest, readback, generation overflow, future detection, ambiguity, ...) are
each caught by the suite.

## Open risks

* Durability on real devices (above). The model is adversarial but is still a model.
* A damaged file that was really written by a newer format and lost its header is indistinguishable from garbage and
  is classified CORRUPT (replaceable).
* After a CORRUPT bundle is replaced, its generation number can be reused.
* Directory sync on Android is unverified; if it throws on some devices, writes there end `Failed` (first write, while creating
  directories) or `Uncertain` (afterwards). That is safe, but the feature would be unusable on those devices until a
  platform adapter handles it.
* One writer per slot, in-process. A second process writing the same files is outside the contract.
