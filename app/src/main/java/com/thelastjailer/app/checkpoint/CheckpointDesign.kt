// Batch 2A: dormant checkpoint foundation. Nothing in this package is called by the game yet.
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
package com.thelastjailer.app.checkpoint
