# G2L Stage A: save classification (dormant)

Status: **dormant, read-only**. Nothing in the app calls this code. It does not change live loading, autosave,
persistence, the save UI or entitlement behaviour. It adds no quarantine and no write gate (Stage B+C). The G2 recovery
store stays dormant and is not used here.

Code: `app/src/main/java/com/thelastjailer/app/saveguard/SaveClassifier.kt` (one file).
Tests: `app/src/test/java/com/thelastjailer/app/saveguard/` (5 classes) and
`app/src/test/java/com/thelastjailer/app/StrictFakeSharedPreferences.kt`.

## Why this exists

G1 showed a wrong-typed value (`1.level` stored as a string) puts the game in a launch crash loop, and that a truncated
or empty save file silently starts a new game that the next save overwrites. The crash loop, awful as it is, leaves the
file untouched. A guard that only stops the crash, without a write gate, would turn it into silent loss. Stage A defines
and tests the failure states first, changing nothing a player can see.

## What the classifier does

It takes one snapshot of the file with `SharedPreferences.getAll` and inspects the **raw value types itself**. It never
calls a typed getter, so it cannot hit the `ClassCastException` a typed getter throws on Android. If `getAll` throws
(`IllegalStateException`, which is what Android does when the file failed to load), the result is **Unreadable**,
never Missing. It performs no write (a test fails if the source contains any), no file access and no logging.

### Slot (and Back snapshot) states

| State | Condition |
|---|---|
| Missing | none of the 13 state fields stored (timeline-only leftovers do not count, matching `hasSave` today) |
| Valid | every stored field has the right type and the scene exists in this build. Absent individual fields are fine: older saves lack some and the loader defaults them |
| Malformed | a stored field has the wrong type (Int vs Long, a String where a Set belongs, a Set of non-strings, ...); or other fields are present but `scene` is not; or `scene` is empty |
| UnsupportedContent | structurally intact, but the scene id is not in this build |
| Unreadable | whole-file level (`SaveReport.Unreadable`) |

Malformed wins over UnsupportedContent. A fault records the key and the *type name* found, never the stored value
(also true of `toString`). The pre-rework `StringSet` inventory is accepted, as `SaveStore` still reads it.

### Active slot and startup decision (computed, not acted on)

| Marker | Slots | Decision |
|---|---|---|
| absent | none hold data | FreshStart(1) |
| absent | any slot holds data (a Back snapshot counts) | **Chooser**: never guess slot 1 |
| not a positive Int | any | **Chooser** |
| valid n | slot n Missing | FreshStart(n), as today |
| valid n | slot n Valid | Resume(n) |
| valid n | slot n Malformed or UnsupportedContent | NeedsAttention(n) |

A damaged slot that is not the active one, or a damaged Back snapshot, never stops the active slot resuming.
`StartupDecision` has no file-level case: an unreadable file is `SaveReport.Unreadable`.

### Entitlements

`classifyEntitlements` reports each of the three boolean flags as ABSENT, TRUE, FALSE or WRONG_TYPE. There is
deliberately no "assume purchased" or "assume not purchased" for a damaged flag: it proves neither. Play Billing
remains the source of truth for ownership.

## The strict test double

`FakeSharedPreferences` (existing) throws `ClassCastException` for `getString` and `getStringSet` mismatches but
quietly returns the default for `getInt`, `getLong`, `getFloat` and `getBoolean`; real Android casts in all of them. So
the existing unit suite cannot reproduce the G1 crash on any int field, on `active_slot`, or on the entitlement
boolean. `StrictFakeSharedPreferences` casts like Android, throws `IllegalStateException` on every read when the file is
"unreadable", treats a `null` put as a remove, and counts writes. It mirrors AOSP `SharedPreferencesImpl` as read at
android-8.0.0_r1, 11.0.0_r1 and 14.0.0_r1.

`StrictDoubleTest` also **characterises the live code's current behaviour** on damaged data (the G1 crash loop,
the inventory fallback that crashes on a non-string non-set, an unreadable file, scene-without-siblings reading as an
empty slot). Those assertions are named `CURRENT - ...`. They document today, they are not requirements, and Stage B+C
replaces them deliberately when it changes the behaviour.

## Verified Android behaviour this design rests on

From the AOSP source (not from memory), at the three tags above:

* The load starts on its own thread when the `SharedPreferencesImpl` is constructed.
* If `<file>.bak` exists at load, Android deletes the main file and renames the `.bak` over it.
* A parse failure is logged and the map is empty; the damaged file stays on disk until the next write.
* The first write renames the current (possibly damaged) file to `.bak`, writes the new file, then deletes the `.bak`.
* Typed getters cast and throw `ClassCastException`. From API 30 a load `Throwable` makes every read throw
  `IllegalStateException` (absent in API 26).
* `Context.getSharedPreferencesPath(name)` is public API.
* An empty map is **not** evidence of damage (a valid empty file, a missing file and a parse failure all give one).
* Not verified: Android versions after 14, OEM builds (the S23 Ultra's One UI), and that nothing in the process opens
  `jailer_saves` before the app does. Stage D must check these.
* Separate finding, out of G2L scope: a crash after a successful write but before the `.bak` is deleted leaves a new
  main file and a stale `.bak`; the next load discards the newer file and keeps the stale one (a one-write rollback).

## Startup probe boundary (proposed, not implemented)

* The first access to `jailer_saves` in this app is `MainActivity.kt` (`JailerApp`, inside `remember`). The checkpoint
  files (`jailer_ckpt_*`) are opened lazily and are different files. There is no `Application` subclass and the
  app's own manifest declares no provider. This is a source reading; a device check is needed (Stage D).
* A probe has to read the raw bytes of the saves file and its `.bak` **before** that first access, because the load
  starts at construction and can delete the main file at once. The path comes from `getSharedPreferencesPath`.
* Candidate placements: the first line of `JailerApp` (no manifest change) or an `Application` subclass (a manifest
  change). Which one is the earliest *reliable* point is a Stage D question; neither is chosen here.
* Concurrent-write ordering during the probe and any copy is a Stage B+C design item (below).

## Every save-mutating entry point (for the future write gate)

| Entry point | What it writes |
|---|---|
| `SaveStore.save` | slot fields, `active_slot`, slot stamp, timeline keys (called by `commitChoice`, `commitCombatResult`, `commitPurchase`, `commitRestart`, `commitSlotSave`, `restorePreviousState`, `restart`) |
| `SaveStore.savePreviousState`, `clearPreviousState`, `removePreviousKeys` (also inside `save(clearBackSnapshot = true)`) | `<slot>.previous.*` |
| `SaveStore.setActiveSlot` (Save-screen LOAD) | `active_slot` |
| `requestSlotSave`, `confirmSlotOverwrite` | destination slot, via `commitSlotSave` |
| `ArrivalRecorder` (`beforeCommit` retries, `afterSave`) and `CheckpointStore` (`upsert`, `replaceAll`, `replaceFrom`, `removeFrom`, `pruneAbove`, `clear`) | `jailer_ckpt_<slot>` and `jailer_ckpt_stage_<slot>`, separate files |
| `LocalEntitlementRepository` (`unlockFullStory`, `unlockForTesting`, `setDebugPurchaseSimulated`) | `jailer_entitlements` |
| Android itself | the `.bak` swap at load, and every `apply()` write; cannot be gated, only preserved against |

Read paths that can throw on damaged data today: `currentActiveSlot` and `load` (first composition), `load` for
every slot on the Save screen, `requestSlotSave`'s destination `load` and `screenIsCurrent`, `loadPreviousState`
(GO BACK), `hasUnlockedFullStory` / `maxSaveSlots` / `isChapterUnlocked` (Story, Map, Save, Options), and
`readInventory`'s fallback (which throws again for any non-string, non-set value).

## Ambiguities that cannot be resolved without an explicit save-format version

The live save has **no schema or version marker** (only the checkpoint files do). So:

1. **Unknown scene id.** A save from a newer build, a node removed or renamed by an update, and a damaged string that
   is still well-formed look identical. Classified UnsupportedContent and protected; "newer version" is an inference.
   Today a release build silently shows the Chapter 1 opening for an unknown scene (a debug build crashes), so this is
   a behaviour change when it is wired in. A node-id stability audit is required first.
2. **Unknown item ids, flags, trophies.** Not validated (`ItemCatalog.resolve` drops unknown ids; flags and trophies
   are arbitrary strings), so content from another version in these fields is not detectable.
3. **Plausibility.** Negative gold, level 0, health above max and similar are loaded as stored and are not classified.
4. **Partial states that look like old saves.** A slot with `scene` and only some other fields is Valid, because older
   saves legitimately lack fields; a truncated write that happened to keep `scene` is indistinguishable from one.
5. **Orphan Back snapshot.** A slot with `previous.*` keys but no live fields is listed with live Missing and back
   Malformed or Valid; the live slot is simply empty.
6. **Timeline-only leftovers** (`stamp`, `lineage`, `arrivals` without state): Missing. Those keys are guarded by existing
   code and do not affect validity.
7. **`active_slot` beyond the slots a player may use**, or pointing at a locked slot: not damage, not classified.

## Open design items before Stage B+C is proposed

* An exact quarantine protocol (which file is created exclusively, how the name is reserved, what each crash point
  leaves), a global cap or a manifest tying each snapshot to the slots it protects, and the full-cap behaviour.
* Whether a raw copy is a consistent snapshot: exclusive ownership of mutations during the probe and copy.
* The Back-snapshot contract: preserving the logical Back keys is not the same as preserving the original file bytes.
* The denied-write contract: a refused edit must reach the caller as a failure and gameplay must not advance as if the
  save succeeded (`apply()` has no result).

## Tests (Stage A)

| Class | Tests | Covers |
|---|---|---|
| `StrictDoubleTest` | 10 | the double behaves like Android; the lenient fake does not; `CURRENT` characterisation of live code |
| `SaveClassifierSlotTest` | 22 | layout pinned to the real writer, Missing, historical saves, wrong types (every field, several types), scene cases, independence, unreadable, read-only, fuzz (3,000 random files) |
| `SaveClassifierStartupTest` | 14 | marker classes, every startup decision, Back snapshot cases, exhaustive 6 x 4 x 4 marker/slot matrix |
| `SaveClassifierEntitlementTest` | 6 | flag states, no inference from damage, key literals pinned to the repository |
| `SaveGuardDormancyTest` | 6 | no production reference, one file, no write or file call, one Android import, no typed getter |

Mutation check: 16 deliberate breaks of the classifier (type checks, scene rules, legacy inventory, marker handling,
unreadable-as-empty, entitlement inference, prefix, value leakage, null handling, damaged-slot resume) are each caught.
