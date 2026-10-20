# G2L Stage B1: dormant write gate, raw-file validation and quarantine store

Status: **dormant**. Nothing in the app calls any of this code. It changes no gameplay, no save loading, no autosave,
no entitlement handling and no UI. A source-scan test (`SaveGuardDormancyTest`) fails the build if production code
outside the `saveguard` package refers to any of it, or if any component gains logging, a rename, a recursive delete or
an Android dependency it should not have. The G2 recovery store also stays dormant and is not used here.

Authorised by the independent review of the B+C design freeze, amendments and final clarification: **B1 only**. Not
done and not authorised: B2 (wiring the gate into the live save paths), B3 (activating protection and UI), Stage D
(emulator/device verification), G3, merge, Play release.

Backward compatibility for the testers who already have builds is a release requirement. Nothing here is active, so
nothing here can affect them yet; the activation prerequisites at the end say what must pass first.

## Code (all under `app/src/main/java/com/thelastjailer/app/saveguard/`)

| File | Role |
|---|---|
| `raw/RawPrefsShape.kt` | Strict validation of the raw bytes of a SharedPreferences XML file. Pure JDK. |
| `raw/RawFileAssessor.kt` | Combines main file, `.bak` and what Android actually loaded into one assessment; cross-checks raw against loaded. Pure. |
| `gate/SaveGate.kt` | `SaveGate` (modes, one lock per logical action, outcomes) and `GuardedEditor` (per-key policy). Only imports the `SharedPreferences` interface. |
| `quarantine/QuarantineFileOps.kt` | The only filesystem seam: exclusive create, narrow delete, no rename, no overwrite, no recursive delete; java.nio implementation that never follows links. |
| `quarantine/SnapshotFormat.kt` | MANIFEST and COMPLETE formats, strict parser, SHA-256. Pure. |
| `quarantine/QuarantineStore.kt` | Snapshot creation, hash deduplication, completion verification, bounded retention, fail-closed repair, confirmed cleanup. |

## Raw classification

The failure this answers: when Android cannot parse the preferences file it logs and loads an **empty** map, which the
API cannot tell apart from a new install. Only the raw bytes can. `RawPrefsShape.validate` accepts exactly the six
shapes `SharedPreferences.Editor` can write (`int`, `long`, `float`, `boolean`, `string`, `set` of unnamed `string`),
each with a non-empty unique `name`, one `<map>` root, nothing after it, and rejects everything else, including things
Android's own reader would tolerate (missing names, duplicate keys with last-wins, arrays, lists, nested maps,
trailing bytes, unknown tags). Reason codes only; nothing returned or thrown contains a key or value.

`RawFileAssessor.assess(main, bak, loaded)`:

* **A fresh start is possible only for** an absent file, or a canonical empty map (`<map />` or `<map></map>`, with the
  exact Android declaration or none, no BOM), **and** no `.bak`, **and** an empty loaded map. Everything else that
  exists is either VALID (raw equals loaded: same keys, runtime types, values; floats by bit pattern) or SUSPECT.
* A `.bak` present at load means Android discards the main file and loads the `.bak`. The `.bak` is therefore the
  effective source; both files are to be snapshotted; it is **never** a fresh start, and an empty `.bak` beside a main
  file is `STALE_BAK`.
* A file that cannot be read, is over 1 MiB, or whose load failed is SUSPECT with a reason code. Over-size and
  unreadable files cannot be copied, so the gate can only stay frozen (`canSnapshot == false`).

## Write gate

One logical game action is one lock acquisition and one editor batch: permission check, optional `before` side effect
(only if permitted), staging of every key through `GuardedEditor`, a single `apply()`, optional `after` side effect
(only if applied). The mode cannot change while the lock is held. A denied action builds no editor and runs no side
effect. `GuardedEditor` checks each key against the action's `WriteIntent` and the protected slots; `clear()`,
`commit()` and a caller's `apply()` are forbidden; a swallowed denial still denies the whole action.

Outcomes: `Handed` (the gate allowed it and called `apply()`), `Denied`, `Failed`, `Uncertain` (`apply()` itself
threw). **`Handed` does not mean the data is on disk**: `apply()` gives no result and can lose the write on process
death (design ruling D7). The wording rule for the later UI is "Slot N updated", never "safely saved". Checkpoint files
(`jailer_ckpt_*`) are not part of the transaction.

## Quarantine store

Protocol: no rename anywhere. `snap-NNNN/` is reserved with an exclusive `createDirectory`; evidence (`main.xml`,
`bak.xml`) is written with `CREATE_NEW`, synced, and read back; `MANIFEST` is written; `COMPLETE` (`tljq1 <sha256 of
MANIFEST> <length>`) is written last; then COMPLETE, MANIFEST and every evidence file are re-read and re-hashed before
the result is `Secured`. "Complete" is recomputed from the disk on every scan; no count depends on an in-memory flag.

* **Deduplication**: the same bytes (hash pair of main and bak) return the existing snapshot (`REUSED`), so repeated
  launches never spend the cap.
* **Caps**: 3 complete snapshots globally, 3 incomplete non-empty directories, 1 MiB per source file, 16 MiB measured
  byte budget (the scan sums block-rounded 4 KiB sizes, including foreign files), and free space of at least twice the
  need plus 64 KiB. Hitting a cap refuses the new snapshot (`Refused`) and deletes nothing.
* **Orphans**: an empty snapshot directory is removed (it holds nothing). An incomplete directory whose evidence equals
  the current source is adopted (`PROMOTED`) and finished. Any other incomplete directory is kept and counted.
* **Repair**: an invalid MANIFEST or COMPLETE is first copied to `NAME.invalid-k` (exclusive, k up to 8, read back
  equal) and only then removed and regenerated. If all k are taken the result is `REPAIR_EXHAUSTED` and nothing is lost.
* **Deletions the store can perform, exhaustively**: an empty snapshot directory; this call's own just-created
  evidence files when they have not yet been verified; an invalid MANIFEST/COMPLETE after its bytes were preserved; and
  the confirmed cleanup below. A directory with a valid COMPLETE is never touched.
* **Cleanup requires positive proof.** A directory without a valid COMPLETE is not disposable: it may hold the only
  copy of damaged data. `cleanupPreview` lists only incomplete directories where every entry is a plain file with a
  known name and every evidence file is either empty or a byte-prefix of (or equal to) the same-named file in a
  complete snapshot. With no complete snapshot, nothing is eligible. `cleanupConfirmed(plan)` re-scans and deletes
  nothing unless the eligible set is exactly the plan the player was shown.
* **I/O errors fail closed**: a genuine read error while verifying a snapshot aborts the scan (`Failed`), rather than
  classing a verified snapshot as incomplete. (Found by fault injection; see below.)
* No logging anywhere. Results are enums, counts and codes. The MANIFEST holds reason codes, slot numbers, sizes and
  hashes, never keys, values or scene ids.

## Tests

Run from `app/` by Gradle (`testDebugUnitTest`, `testReleaseUnitTest`, `testInternalTestUnitTest`), all with the
project's existing warnings-as-errors setting.

| Class | Tests | Covers |
|---|---|---|
| `raw/RawPrefsShapeTest` | 19 | writer shapes, float/escape/large-map round trips (no false suspects), every suspect reason, truncation sweep, single-byte corruption sweep, depth bomb, no content in results |
| `raw/RawFileAssessorTest` | 16 | absent/empty/valid/suspect, `.bak` cases, every cross-check difference, bit-level float compare, fresh-start property over all combinations |
| `gate/SaveGateTest` | 24 | mode table, no side effects when denied, per-key policy, forbidden calls, swallowed denials, nested, two-thread atomicity, protected-slot preservation in serialised form with a negative control, exhaustive mode x intent x key sweep |
| `quarantine/QuarantineStoreTest` | 50 | creation, dedupe, caps, budget, space, collisions, links and specials, torn markers (every prefix), flipped evidence, promotion, bounded repair, a fault at every operation x 6 fault kinds then relaunch, repair and cleanup crash matrices, randomized campaigns, positive-proof cleanup, delete-site invariants |
| `quarantine/JavaQuarantineFileOpsTest` | 8 | real filesystem: exclusivity, symlinks never followed, narrow delete, end to end store on a temp directory |
| `quarantine/SnapshotFormatTest` | 7 | strict manifest parse, marker equality, SHA-256 vectors, no content in manifests |
| `SaveGuardDormancyTest` | 11 | no production reference, exact file allow-list, per-subpackage import and token rules (rewritten from Stage A's) |

Test doubles: `SimQuarantineFs` (in-memory filesystem with a numbered fault: throw before/after the effect, crash
before/after the effect, short write then throw/crash) and `AndroidXmlWriter` (a transcription of AOSP `XmlUtils` +
`FastXmlSerializer`, a simulation, see below).

Defects found and fixed by this testing, before review:

1. A transient read error during the snapshot scan made a verified snapshot look incomplete, which could let the
   complete cap be exceeded. The scan now fails closed on I/O errors (regression test: every read failing in turn).
2. The manifest parser accepted a malformed `slots` list. It is now strict.

## Mutation checks

73 hand-written mutants (`mutate_b1.py`, kept outside the repo): raw shape 15, assessor 10, gate 15, store 27, format 3,
file ops 3. Each is one deliberate bug injected into a copy of the source and run against the whole B1 suite.

* First pass: 61 killed, 12 survived (2 more could not compile because of a test edit made mid-run and were re-run).
* The survivors exposed weak or missing tests, not code bugs. Tests were added or strengthened for: the fresh-start
  rule's own bak check (B16), a valid-but-empty non-canonical file (B24), the editor's generation check (G09), the
  early evidence read-back (Q06), repair-copy verification (Q15), cleanup of a partial write (Q16), a directory that
  becomes complete before cleanup deletes (Q22), link handling (Q26), stale-manifest adoption (Q27), directory delete
  refusal (O03), duplicate manifest entries (F02).
* Final: **70 of 73 killed, 3 survive and are equivalent mutants**: B22 (the type comparison in the cross-check is
  redundant with the value comparison), Q17 (the verified-evidence flag is only set after the last point that can
  throw), F03 (the final-newline check is redundant: dropping the last character always breaks the last hash).

## Deviations from the approved design

1. **Float grammar widened.** The clarification said "finite canonical" floats. Android's writer emits
   `Float.toString`, which includes `NaN`, `Infinity`, `-Infinity` and `E` notation. Rejecting those would call a legitimate
   Android-written file suspect, the exact risk the reviewer's first safeguard names. The validator accepts the full
   `Float.toString` grammar. Values are still compared by bit pattern against the loaded map.
2. **Empty `.bak`**: any `.bak` makes the assessment non-fresh; an empty canonical `.bak` is `STALE_BAK` (it may hide a
   newer main file that Android discarded). The design said "never fresh" without naming this case.
3. **Promotion** is limited to incomplete directories whose evidence equals the current source, and only while the
   complete count is below the cap.
4. **Cleanup eligibility** is the prefix-of-a-complete-snapshot or zero-length rule above, the concrete form of the
   reviewer's "positive proof" safeguard. Anything not provable is kept.
5. **I/O errors during a scan abort the operation** instead of degrading to "incomplete" (defect 1 above).

## Assumptions that need Android verification (Stage D), not claims

* The XML shapes above are derived from AOSP `XmlUtils` (android-14.0.0_r1, read) and `FastXmlSerializer`
  (android-8.0.0_r1 and 14, read). For 8.0 only the `map.put` and "Document ended" code was confirmed.
  `AndroidXmlWriter` is a **simulation**. **Real Android-written fixtures captured on an emulator remain mandatory
  before activation**, for every API level we support, including a One UI device.
* Control characters: Android's writer emits `&#N;` for characters below 32. XML 1.0 forbids most of them, so the JDK
  parser rejects such a file (`UNPARSEABLE`, fail closed: frozen, not wiped). Whether the game can ever store one, and
  whether Android's parser reads them, is unverified.
* How Android handles truncated or odd files at load, the exact `.bak` rename/restore behaviour on each API level, and
  OEM variation.
* The JDK DOM parser's feature support (`disallow-doctype-decl`, secure processing) differs by platform; the textual
  DOCTYPE/ENTITY pre-scan is the guarantee, not the feature flags.
* Android binary XML is never used for app preferences (the `ABX` magic is rejected as suspect).
* Directory `fsync` and `force(true)` durability on Android filesystems (F5) are unverified; the code treats directory
  sync as best effort and claims nothing from it. "Verified" here means "read back equal".
* java.nio behaviour on Android for `NOFOLLOW_LINKS`, `CREATE_NEW` and directory channels.
* `apply()` durability and the stale-`.bak` rollback hazard remain documented, not solved.

## Activation prerequisites (none of which B1 satisfies)

B2 wiring review; emulator corruption replay (disposable emulators only, application id `com.thelastjailer.app.debug`);
real Android writer fixtures; healthy-save compatibility checks on the shapes the testers' builds wrote; a healthy-path
smoke test on a phone. Nothing activated reaches the 12 testers before those pass.

## Privacy and CI

No component logs. Test failures print reason codes, slot numbers and counts only; no test prints a file's bytes. The
tests use synthetic data. Nothing in this change uploads artifacts.
