// Batches 2A and 2B-1: dormant checkpoint foundation. Nothing in this package is called by the game yet.
//
// What exists: an immutable checkpoint record with a CRC32, validation rules, lineage ids, an
// atomic per-slot SharedPreferences store with honest failed-commit handling, pure rebinding and
// copy-selection helpers, and a staging area. Everything is new; no existing production file was
// changed, the save format is unchanged, and the Back button, trophies, billing, combat and chapter
// progression are untouched.
//
// Decisions this foundation is built for (agreed for the later batches, deliberately NOT implemented):
//
// 1. Arrival (2B). A checkpoint is written only when a committed choice or combat result moves the
//    player into a *different chapter number*. Never on load, Back, rewind, copy or "standing on an
//    entry scene" (three Chapter II entry scenes are also reachable from inside Chapter II).
//    Arrival into chapter K removes records >= K, then writes K.
// 2. Slot copy (2C). Copy the source's complete valid history; mint a fresh lineage for the
//    destination; rebind every record (CheckpointSets.rebind); stage and verify (CheckpointStaging);
//    keep a recovery bundle of the destination until the replacement is complete; one atomic commit
//    to the live save is the decision point; reconcile an interrupted copy at the next launch. The
//    source is never written. The slots are independent afterwards.
// 3. Recovery retention (2C/2D). Pre-rewind, pre-overwrite and pre-restart bundles are held until the
//    player explicitly accepts the new timeline or a later operation safely supersedes them. App
//    close and new chapters do not discard them. Bundles are double-buffered so a slot is never
//    left without a recovery copy. Undo is a reversible swap.
// 4. Achievements (2B/2D). Trophies are per slot, separate from story flags, never duplicated, and
//    survive rewinds, ordinary Back and undo: restore uses live trophies united with the
//    checkpoint's, and a swap keeps both timelines' trophies. (One Batch 1 test that pins Back
//    dropping a just-earned trophy changes deliberately in 2B.)
// 5. Start Again. Receives the same recovery protection as an overwritten slot, under a new lineage.
// 6. Purchases. Records never contain or imply entitlement. The Chapter IX boundary is enforced at
//    render from the scene's chapter, so a restored record cannot pass it.
// 7. CRC32 is an integrity check against accidental damage, not proof of authenticity. Lineage plus
//    the live-save rules prevent accidental mixing. Deliberate editing of a player's own save is out
//    of scope and cannot unlock paid chapters.
// 8. Failed writes. SharedPreferences.commit() returning false can leave the new values visible in
//    memory. CheckpointStore re-commits the previous contents and reports RolledBack only when that
//    succeeds; otherwise StateUnknown, after which the slot is untrusted until restart.
//
// ---- Batch 2B-1 additions (store hardening, still dormant) ----------------------------------------
//
// * CheckpointStore.replaceFrom(slot, lineage, record): the chapter-arrival write. One commit removes
//   every record >= K and writes K, so a crash leaves the old file or the new one, never a mixture.
// * Incremental writes (upsert, replaceFrom, removeFrom, pruneAbove) run only on an empty file or an
//   intact header for this slot and lineage. Unsupported-version, wrong-slot, headerless and unreadable
//   files are refused untouched.
// * replaceAll and clear are the explicit whole-file operations. They can still replace a headerless,
//   wrong-slot or damaged file (that is their job), but they REFUSE to destroy a newer-format file
//   (UNSUPPORTED_VERSION, e.g. after an app downgrade) unless the caller passes discardNewerFormat = true.
//   Nothing in the normal flow should ever pass it.
// * StateUnknown protection and the write lock live in StoreRegistry, keyed by (CheckpointStorage.memoryScope,
//   file name) - never by the identity of a SharedPreferences object - so wrappers, decorators or a new
//   object per call over the same file cannot dodge either protection. In production the scope is the
//   application Context and the preferences come from getSharedPreferences; a new process has a new scope
//   and starts trusted. Operations on one slot, reads included, are serialised.
// * Keys that look like chapter groups but can never be valid (c5., c123., c00., c31.) are swept by
//   replaceFrom, removeFrom and pruneAbove in the same commit. Keys that are not group-shaped at all
//   (C07., x.c07., h.extra) are not ours and are never touched.
// * 2B-2 must build exactly ONE app-level CheckpointStore from PrefsCheckpointStorage (scope = application
//   context, files from CheckpointFiles) and must pin that with a test.
//
// ---- Requirements for the arrival ledger (2B-2). Written down now, NOT implemented ----------------
//
// The live save is written with SharedPreferences.apply(): asynchronous, no result, can be lost, and
// not ordered against a commit() on another file. The checkpoint file cannot therefore rely on write
// order. The agreed design makes the live save the only commit point and makes every record prove itself:
//
// L1. Additive live-save keys, written in the SAME editor batch as the state they describe:
//     "<slot>.lineage" and "<slot>.arrivals" (chapter:CRC pairs). No existing key changes.
// L2. A record is usable only if the lineage matches in live save, file header and record; it passes the
//     2A rules; its chapter is <= the live chapter; and the live ledger entry for that chapter equals the
//     record's CRC. Anything else is treated as absent. A checkpoint may be missing; never wrong.
// L3. Arrival is detected only inside the committed transition (choice, and combat for completeness),
//     forward chapter change only, from the in-memory next state. Unknown scene or chapter => no arrival.
// L4. A lost record is re-created only when the CRC of the loaded live state equals the ledger CRC.
//     Retry runs at startup and before the next state change, on the pre-change state.
// L5. Anything that makes the live save a different timeline (Start Again, SAVE into another slot,
//     Chapter 30 to Chapter 1 loop) mints a new lineage and clears the ledger in the same batch as the
//     state. The default for save() must be invalidating; same-timeline callers opt in.
// L6. A checkpoint failure never changes the player's transition and never surfaces to the player.
// L7. A kill switch may turn recording off but must never turn invalidation off.
//
// Open items that MUST be examined in 2B-2 before it is implemented (not decided here):
//
// O1. Stronger arrival binding. CRC32 of the record is the proposed binding. Examine a wider or second
//     digest, and binding an arrival sequence number or counter, to shrink collision risk and to detect
//     "same state, different path" arrivals that the CRC alone cannot tell apart.
// O2. Back followed by other actions. Back across a chapter boundary leaves the ledger entry for the
//     abandoned chapter in place. Examine every sequence: Back then a different choice into the same
//     chapter, Back then purchase, Back then save to another slot, Back then Start Again, Back then app
//     kill before and after the live writes land, Back twice via two different routes. Confirm each
//     ends with a missing record rather than a wrong one, and that a stale entry cannot be resurrected
//     by a later arrival.
// O3. Older-build rollback: an older app version cannot invalidate on Start Again or SAVE. Examine
//     whether a live-save fingerprint is worth its cost.
// O4. The real-device interrupted-write test (kill during arrival, copy and rewind) is a release gate
//     before any rewind UI ships.
package com.thelastjailer.app.checkpoint
