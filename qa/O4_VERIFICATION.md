# O4 verification record: save compatibility across three fixed builds

This file records what the O4 testing exercise verified, with the exact commits and CI runs, and what it did **not**
verify. It is deliberately outside `docs/` (which GitHub Pages publishes). It contains no captured save files or contents.

## What O4 is, and what it is not

O4 checked that a game saved by an older build still loads and can be continued after the app is updated in place.
It was run on **three fixed, reviewed commits**. It is **evidence about those three builds**. It is **not** a regression
guard: the workflows build the fixed commits below, not the code of whatever pull request or branch triggers them, so a
green run says nothing about whether a *later* change would break existing saves. Turning it into a guard needs a
separately designed and reviewed way to choose the candidate build (validated refs, read-only permissions, no secrets).

| Label | Commit | What it is |
|---|---|---|
| OLD-A | `3127d2a04115e1992d2c64b32396d27e6da3df12` | Batch 1: safety foundations (#117). Has no checkpoint recorder. |
| OLD-B | `4755ae45610371f82e26f80d455abd392f52eebe` | Batch 2C-1b-1: confirm before SAVE replaces an occupied slot (#124). Has the checkpoint recorder, no slot stamp. |
| NEW | `071439fc1145a4df21fc844492639ac82e48e6c6` | O3-B: slot state stamp (#125). |

All three are built in one job and signed by one throwaway debug key (created per run, never stored), so they can update
each other. The package under test is always `com.thelastjailer.app.debug`. The Play app `com.thelastjailer.app` is never touched.

## The two workflows

- `.github/workflows/o4-qa-apks.yml` (Stage 1): builds the three APKs and uploads them (with a manifest) as an artifact
  retained 14 days, for installing on a phone. These are debug builds and include the debug-only "SIMULATE PURCHASE" button.
- `.github/workflows/o4-emulator-spike.yml` (Stages 2 to 4): builds the same three APKs again in its own job (an
  APK from another run would be signed by a different key) and runs the emulator checks. It uploads nothing.

Both run by hand (`workflow_dispatch`) or when their own file changes in a pull request. Neither runs on ordinary pull
requests. Both use `permissions: contents: read`, no secrets and no `pull_request_target`.

## Evidence

| Stage | What was checked | Run | Result |
|---|---|---|---|
| 1 | Three APKs built with one signer; package, versionCode, debuggable and signer checks | `39076147891` on `a04a2c6` (PR #126) | success |
| 1 (phone) | Installing OLD-A, then OLD-B, then NEW on a Samsung Galaxy S23 Ultra | manual test by the project owner | passed: both slots, level 2 and all six trophies intact |
| 2 | Emulator boots; the three builds install in place (same `firstInstallTime`, APK hash matches); `run-as` works; a harness marker survives each update | `39328069847` on `87d8b4d`; re-confirmed in `39330727682` and `39338515186` | 25 passed, 0 failed |
| 3 | NEW launches with no input: healthy foreground, stable ~16 s, no crash/ANR for the package, six navigation labels present, no save or checkpoint file created by launching, marker unchanged | `39330727682` on `55d0118`; re-confirmed in `39338515186` | 9 passed, 0 failed, 0 inconclusive |
| 4 | OLD-A makes one Chapter 1 choice; the save reaches disk and survives a restart; OLD-B then NEW install in place with the save unchanged; NEW cold-launches and shows the saved scene and state; NEW makes one further choice and the story advances | `39338515186` on `f9b755a` | 17 passed, 0 failed, 0 inconclusive |

History worth keeping: Stage 4's first run (`39337134592`) was **inconclusive** because of a bug in the test harness, not
in the game. The harness measured the bottom navigation bar from the smallest top among nodes named like navigation
labels, and the Story header has an "Inventory" icon at the top, so every choice button was rejected. Nothing was tapped
and no save was created. The fix (the bar is the *lowest* row of labels) was reproduced locally first and then passed.

Final validation of the combined workflow after consolidation: run `39373615939` on `474db95` (PR #127), success: Stage 2 25 passed, Stage 3 9 passed, Stage 4 17 passed, 0 failed, 0 inconclusive. The Stage 4 correction first ran in `39338515186`; later commits to this file change documentation only.

## What Stage 4 shows

A save written by OLD-A (two flags, courage 3, scene `ch1_door_witness`) was left unchanged by both updates (compared in memory, not printed),
was loaded by NEW (it showed the same scene and the same state), and NEW continued from it (scene `black_door_opened`, the
earlier flags kept and two more added, and a slot-stamp key present). The save was synthetic (made by two scripted story choices on a throwaway emulator) and its file content was never printed or stored; only
true/false state checks, scene names from the story source and a file size were reported.

## Not demonstrated

- Checkpoint stamp validity or rejection, lineage correctness, stale-history rejection, and interrupted-write durability
  or recovery. Stage 4's restart check is an ordinary stop after the save had settled, not an interrupted write.
- Trophies, items, levels, combat, or any save created after Chapter 1's first two scenes.
- Any older-save path that needs OLD-B to be launched. OLD-B is only installed over, never run.
- Other devices. This is an Android 14 (API 34) x86_64 emulator, not a phone.
- The Play Billing path.
- That a fresh NEW game's first choice creates a checkpoint file (see below).

## Why Stage 4 shows no checkpoint file (expected)

After Stage 4 the only file in `shared_prefs` was `jailer_saves.xml`; there was no `jailer_ckpt_1.xml`. This is expected:

- OLD-A, which made the first choice, has no checkpoint recorder at all.
- NEW records a checkpoint only for a brand-new game's first choice (from the pristine state) or on a chapter arrival.
  Both Stage 4 choices stay inside Chapter 1, and NEW's choice was made on an existing save. The JVM test "a legacy save
  in the middle of Chapter 1 gets no genesis record" asserts exactly this.

## Harness lessons (for anyone changing the emulator workflow)

- GitHub runs steps under `bash -e`. A failing `wait` or probe silently aborted a step; the scripts use `set +e` and record
  every outcome. Test scripts under the same shell settings the runner uses.
- Set `ANDROID_AVD_HOME` explicitly; the emulator did not find an AVD in the default folder on the runner.
- The emulator binary needs `libpulse0` (install it; do not `ldd` the QEMU binary, which reports bundled libraries missing).
- Match UI text defensively: nodes may carry text or content-description, and the same label can appear in more than one
  place on a screen. Tap only after the safe area is established; otherwise report INCONCLUSIVE.
- GraphQL is not available to the automation; use the REST API.

## Maintenance

- The OLD and NEW commits are pinned; changing them is a reviewed change to the workflow files, and each must stay on `main`.
- Stage 4 depends on Chapter 1's first two scenes (titles `The Fallen Knight`, `A Witness at the Door`, `The Door Without
  a Wall`, and the buttons `OPEN THE DOOR` and `Go through.`). A deliberate story edit there will fail it on purpose.
- `ubuntu-latest`, the Android SDK, the emulator version and the `android-34` system image are not pinned and can drift.
- A full run takes about five minutes.
