# G2L B2 - save mutations through the open write gate

Scope: behaviour-preserving refactor. Every write to the main save file (`jailer_saves`) now goes through the B1
`SaveGate` / `GuardedEditor`, one logical action = one lock acquisition, one `WriteIntent`, one editor batch, one `apply()`.
The gate is constructed `GateMode.Open` and is never switched in B2. Raw-file classification, quarantine, FROZEN/PARTIAL
behaviour and recovery UI are not referenced by production code. No save schema, key, type, stamp or checkpoint format changed.

## Main-save write pathways (complete inventory)

`grep` for `.edit()` / `getSharedPreferences` / `.commit()` / `.apply()` over `app/src/main` finds exactly one editor on
`jailer_saves`: `SaveStore.gated(...)` in `GameState.kt`. Other editors write other files: `jailer_entitlements`
(`EntitlementRepository`, 3 boolean flags, unchanged, out of scope) and the `jailer_ckpt_*` checkpoint files
(`CheckpointStore`, `commit()`, separate files, unchanged).

| Action (caller) | Function | Intent (slots / active_slot) | Batch contents | Hooks |
|---|---|---|---|---|
| Story choice (MainActivity) | `commitChoice` -> `commitTransition` -> `SaveStore.save` | {slot} / yes | Back snapshot + new state + stamp + timeline keys | before = `beforeCommit`, after = `afterSave` |
| Combat result | `commitCombatResult` -> `commitTransition` | {slot} / yes | new state + stamp + timeline + Back keys removed | same |
| Purchase | `commitPurchase` -> `commitTransition` | {slot} / yes | same as combat | same |
| Start Again (Save screen) | `commitRestart` -> `save` | {slot} / yes | fresh state + stamp + timeline + Back keys removed | after = `afterSave` (genesis record) |
| SAVE same slot / empty slot / confirmed overwrite | `commitSlotSave` -> `save` | {destination} / yes | state + stamp; other slot also removes timeline and Back keys | none |
| GO BACK ONE STEP | `restorePreviousState` -> `save(Keep, clearBack)` | {slot} / yes | restored state + stamp + Back keys removed (was two applies before; final map identical) | none |
| LOAD (Save screen) | `setActiveSlot` | {} / yes | `active_slot` | none |
| (API, not called by the app) | `savePreviousState`, `clearPreviousState`, `restart` | {slot} / no (`restart`: yes) | Back snapshot / removal / fresh state | none |

Before B2 a story choice made two applies (Back snapshot, then state) and Back made two (state, then snapshot removal);
both are now one batch. The final preferences map is identical (see equivalence below) and the window in which a crash
could leave a half-done action is smaller.

## Outcomes

`GateOutcome` is mapped by `SaveStore.gated` into `CommitStatus`:

| Gate outcome | Status | Checkpoint hooks | What the caller shows |
|---|---|---|---|
| `Handed` | `SAVED` | before ran ahead of apply, after ran once handed | the new state |
| `Denied` | `NOT_SAVED` | neither ran | the state the player already had |
| `Failed` (no editor / staging threw) | `NOT_SAVED` | neither ran | the state the player already had |
| `Uncertain` (`apply()` threw) | `UNCERTAIN` | before ran, after did not | whatever `load(slot)` reads now |

`Handed` still means "handed to SharedPreferences", not "on disk". Cross-file atomicity between `jailer_saves` and the
checkpoint files is not claimed and was never present.

User-visible change (the only one): when the write is not confirmed handed over, the Save screen shows
"Could not save. Please check your save slots and try again." instead of reporting success, and LOAD does not switch to the
other slot if the active-slot write was not handed over. With the gate open this can only happen if `SharedPreferences.edit()`
or `apply()` throws, which does not happen on normal devices.

Also changed: an exception thrown while staging a save (for example a wrongly typed stored value read while deciding to keep
a timeline) is now contained by the gate as `Failed`/`NOT_SAVED` instead of propagating to the caller.

## Checkpoint ordering

Unchanged: `beforeCommit` (maintenance of the old state) -> main `apply()` -> `afterSave`. One difference in how the
plan is computed: the old code ran `beforeCommit` and then planned the arrival; the gate must know the batch before it runs
`beforeCommit`, so the plan is now made first and re-checked after `beforeCommit`. `beforeCommit` can switch recording off for
a slot (newer-format checkpoint file found). If `isRecording` changed, the arrival is replanned.

Review of PR #133 (head 83fd50e) found that this replan restaged on the editor after the gate's pre-apply check. Corrected:

- Everything that can read or throw (the replan, `stampValid`, the stored values, the stamp text) is computed first, away from the
  editor. If that throws, the batch keeps the plan it was staged with (a ledger entry without a record vouches for nothing).
- The restage is then only `putString`/`remove` on this slot's own `lineage`, `arrivals` and `stamp` keys. These are the same
  per-slot keys the gate already checked (the gate's policy is per slot prefix); a test records every key staged and asserts
  they are `active_slot` or `1.*`.
- The restage is explicit. Found while fixing: when the replan said "keep", the old restage left the keys staged by the first
  plan in place (stale ledger entry for the arrival chapter, stamp from the stored values, so the timeline was silently dropped,
  and the earlier test passed vacuously). It now writes the stored values back, so the result is what the old order produced
  (verified byte-identical, new golden scenario `flipDuringArrival`; the previous head differed there).
- The stamp is written last, so a restage cut short can only make the timeline untrusted, never wrong.
- `Denied`/`Failed` never reach the replan; `Uncertain` after a replan leaves the checkpoint files untouched (tests).

Residual limitation: the gate cannot veto the `apply()` if a restage `put` throws after `beforeCommit` ran (the gate swallows
hook exceptions by design). That outcome is a saved state with an untrusted timeline, covered by a test.

## Verification (local JVM harness; Gradle/Android SDK not available in the workspace)

- Main sources (all non-UI) compile with `-Werror` (kotlinc 2.0.21). UI files (`SaveScreen.kt`, `MainActivity.kt`) are not
  compilable locally (Compose); they are covered by the Android CI build.
- Existing tests before and after: 1008 passing before adding B2 tests; 1035 after (27 new in `SaveStoreGateIntegrationTest`).
  Five test classes that need Compose/BuildConfig/R (`ApplicationIdTest`, `Batch1SafetyFoundationsTest`, `CombatEngineTest`,
  `CombatConditionTest`, `ReleasePolishTest`) are not run locally and are left to CI.
- Pre/post equivalence: `qa/g2l_b2/B2Golden.kt.txt` (a driver, not part of the build) runs 27 scenarios, with and without a
  recorder: arrivals to Chapter 6, same-chapter choice, Back, combat, purchase, save onto same/other slot, active slot,
  Start Again, Start Again after arrivals, the six Chapter 30 loops, recording disabled, and a newer-format checkpoint file
  appearing mid-session. It dumps the exact live preferences map and the three checkpoint files after each step. Run against
  main `a8d8a62` and against this branch: byte-identical output (937 lines).
- Mutation testing of the integration (`qa/g2l_b2/mutate_b2.py.txt`): 44 mutants over `GameState.kt`, `StateCommits.kt`,
  `SlotOverwrite.kt` (outcome mapping, hook placement/order, intent, Back snapshot rules, timeline rules, refused/uncertain
  display state, replan, slot-save request mapping), run against the fast tests (about 620). First pass: 34 killed, 6 survived (restore
  uncertain display, Start Again refused display x2, purchase keeps Back, replan never, uncertain slot save reported saved).
  Tests were added for each; second pass and the re-run after the review fix: 44/44 killed, no survivors, no equivalents claimed. (B1 gate mutants G01-G18 are
  unchanged and were not re-run; `SaveGate.kt` is untouched.)
- `SaveGuardDormancyTest` now allows only the four gate types (`SaveGate`, `GateMode`, `GateOutcome`, `WriteIntent`) in
  `GameState.kt`; raw assessment, quarantine, classifier and startup decisions remain unreferenced by production code.

## Not verifiable without an Android emulator / device

- Real `SharedPreferencesImpl` behaviour of the single larger `apply()` (background write, `.bak` handling) versus the earlier
  two applies; the batch size is a few KB of keys, well under the payload ceiling measured in G2.
- Compose Save screen: the new notice text and LOAD behaviour on a real device (no way to make `apply()` throw on demand).
- Process death between `apply()` and `afterSave` on a real device (simulated in the JVM crash sweeps only).
- Upgrade of an existing install with real older saves (a healthy-path smoke on the `.debug` build is recommended before
  anything activating the gate).

No player save content appears in tests output, CI artifacts or logs added here; tests use synthetic scenes.
