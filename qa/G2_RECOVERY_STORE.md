# G2: dormant recovery-bundle store

Status: **dormant**. Nothing in the app calls this code. It never reads or writes the live save, restores nothing,
has no UI and recovers nothing automatically. A passing G2 proves we can **store and verify** recovery bundles. It
does **not** prove the game can recover a damaged save; that is what the later gates are for.

Code: `app/src/main/java/com/thelastjailer/app/recovery/` (plain JVM, no Android imports).
Tests: `app/src/test/java/com/thelastjailer/app/recovery/` (112 tests).

Hardening patch (after the adversarial review of 702f5f9): F1 lock identity, F2 temp-file and directory confinement,
F3 multi-write tests, D1/D2 documentation. See "Hardening patch" below.

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

Write protocol: ensure the root and slot directories (one level each, never ancestors, never through a symbolic link;
syncing the parent of anything newly created), clear the temp name, create the temp file **exclusively without following
links**, fsync it, atomic rename over the target, fsync the directory, read back and verify, rescan and confirm the other
buffer is unchanged. The root's parent must already exist (in the app, `filesDir`); if it does not the write is `Failed`
at PREPARE and nothing is created.

Reads never follow links either: a symbolic link, directory or special file where a bundle should be is UNREADABLE, so the
write is blocked rather than reading or replacing what is behind it.

## What is guaranteed where

| Guarantee | JVM tests | Real Android device |
|---|---|---|
| I1 the sole known-good copy is never evicted | **proved** under the simulator's durability model, for all start states x every fault x 2,700 crash images (single write) and across chains of up to three writes and two crashes (about 41,000 more) | not proved |
| I2 a FUTURE file is byte-identical afterwards | **proved** (same sweeps, chains included) | not proved |
| I3 generations increase; equal-but-different is never resolved by guessing | **proved** | n/a (logic) |
| I4 only the slot's four files are touched (plus the root and slot directories, one level each) | **proved** | n/a (logic) |
| I5 `Stored` means verified | **proved** | readback proves the bytes the OS returns, not that they survive power loss |
| fsync / rename atomicity / directory sync are durable | **modelled**, not shown | **UNVERIFIED** (G7) |
| `FileChannel.force` on a directory works on every supported Android API and filesystem | n/a | **UNVERIFIED** (G7); a failure surfaces as `Uncertain`, never ignored |
| Planted links / unexpected objects never redirect or are read through | **proved** on a real JVM filesystem with real symlinks | the same calls on Android's runtime are **UNVERIFIED** (G7) |
| Auto Backup captures live prefs and bundles consistently | n/a | **UNVERIFIED** (G6) |

The simulator (`SimFs`) keeps volatile and durable file contents and directory entries apart and resolves a crash
every legal way (names as synced or all applied; data synced only, half written, or fully persisted), including
a rename that persisted while the file data did not. A **negative control** (a naive in-place overwrite) is shown to
lose the only good copy in the same simulator, so the sweeps cannot pass vacuously.

### What I1-I5 do and do not cover (D2)

They are statements about **a write started from a disk that was already durable**, plus the chained sequences the
multi-write tests exercise (a faulted write then another write, a retry of an uncertain write, a crash then a faulted
write then another write, two crashes in a row). They are **not** statements about every possible history on a real
device, and they rest on the simulator's model of fsync / rename / directory sync. The model is adversarial but is a
model: a phone's storage stack may differ, and a readback proves what the OS returns now, not what survives power
loss. Other limits: one process only; a root that is itself a symlink is not a supported alias (writes fail closed);
slot numbers of 10 digits or more are written but not listed (F4, deferred; production uses slots 1-10); the
`fsync` is done on a second file descriptor, which Linux treats as flushing the file but Android has not been shown to
(F5, device investigation).

## Hardening patch (review findings F1, F2, F3, D1, D2)

| Finding | Change | Evidence |
|---|---|---|
| F1 lock identity | The per-slot lock is keyed by the **canonical path** (`RecoveryFileOps.lockIdentity`, resolved on every call). Different spellings (relative, `..`, a symlink in an ancestor such as `/data/data` vs `/data/user/0`) share one lock. A root that is itself a symlink is not an alias: it fails closed. | 702f5f9: 20 of 300 rounds with two writers both "storing" one generation, 1,683 Uncertain. Patched: 0 of 300, 0 Uncertain. Control (same spelling) 0 on both. |
| D1 | The KDoc and this note claimed one lock "shared by every store over the same directory". It was shared only for identically spelled roots. Both now state what is actually keyed and its limits (in-process only). | text |
| F2 temp-file symlink | The temp file is created with `CREATE_NEW` + `NOFOLLOW_LINKS` (`createFileExclusive`): it fails if *anything* exists at the name, a link included. The store still deletes a leftover temp first, but the delete is not the protection. A directory squatting on the name fails closed as before. | 702f5f9: a link at `b.tmp` made the store overwrite an outside file (284 bytes). Patched: outside file untouched, write lands inside the slot. |
| F2 reads | `read` refuses links, directories and special files (no-follow). A link where a bundle should be is UNREADABLE and blocks the write. | tests |
| F2 directories | `ensureDirectory` uses `Files.createDirectory` (one level, parent must exist) and refuses a link where the directory should be. The store only ever asks for the root and the slot directory. | 702f5f9 created `no/such/parent/recovery` and followed a symlinked slot directory; patched fails closed and creates nothing. |
| F3 | `RecoveryMultiWriteTest` (11 tests, listed below) and the simulator now models links, exclusive create and non-recursive mkdir. | sweeps |
| D2 | Scope of I1-I5 and the limits of the durability model stated (section above and the store's KDoc). `RecoveryDormancyTest` carries a note that its exact file count and no-use rule change on purpose at G3. | text |

Behaviour change to be aware of: the old code created the recovery root's missing ancestors; the new code does not. In
the app the root's parent is `filesDir`, which exists.

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

112 tests (89 + 23 new): codec 21, selection table 3, store behaviour 30, fault injection 15 (486 scenarios), crash
simulation 5 (2,700 crash images), real-filesystem adapter and concurrency 10, dormancy 4, size measurement 1,
**hardening on a real filesystem 12** (aliased-store concurrency, links at the temp name, dangling link, exclusive
create, directory squatting, link as a bundle, missing parent, one-level mkdir, link as slot directory, link read),
**multi-write 11** (fault then write 450 chains, uncertain then retry 105, crash then faulted write then write 24,060,
double crash 17,235 images, planted link, squatting directory, link as a bundle, missing root parent, only the two
directories created, alias waits for the lock, unrelated directories do not).

Mutation check: 27 deliberate breaks (the original 20 safety rules, plus lock keyed by spelled path, temp name not
cleared, non-exclusive temp create, recursive mkdir, link accepted as directory, reads following links, non-canonical
lock identity in the adapter) are each caught by the suite.

Old versus new (the review's original experiments, re-run against both): 702f5f9 reproduces E1 (outside file
overwritten) and E3 (20 of 300 rounds); the patch does not. The store-level subset of the new real-filesystem tests
also fails 6 of 8 against 702f5f9 and passes now.

## Open risks

* Durability on real devices (above). The model is adversarial but is still a model.
* A damaged file that was really written by a newer format and lost its header is indistinguishable from garbage and
  is classified CORRUPT (replaceable).
* After a CORRUPT bundle is replaced, its generation number can be reused.
* Directory sync on Android is unverified; if it throws on some devices, writes there end `Failed` (first write, while creating
  directories) or `Uncertain` (afterwards). That is safe, but the feature would be unusable on those devices until a
  platform adapter handles it.
* One writer per slot, in-process. A second process writing the same files is outside the contract.
* F4: slot numbers of 10 digits or more are written but not listed by `slotsOnDisk` (deferred; production uses 1-10).
* F5: the file is fsynced through a second descriptor, and `force` on a directory is unverified on Android (device matrix).
