package com.thelastjailer.app

/**
 * Trophies are earned, not undone. A story choice can be taken back, but a trophy the player has genuinely
 * earned stays earned.
 *
 * [restored] is the trophy set of a state that is being brought back (today: the one-step Back snapshot);
 * [live] is the trophy set the same slot holds right now. The result is their union: the restored state keeps
 * everything the player has earned, and nothing else about the restored state is touched. Everything
 * reversible (stats, gold, items, flags, scene) comes back exactly as it was.
 *
 * Properties the callers rely on:
 * - **monotone**: the result always contains every trophy in [restored] and in [live];
 * - **idempotent**: merging the same live set again changes nothing, so a repeat after a crash is harmless;
 * - **pure**: no storage, no clock, no knowledge of the story.
 *
 * Callers must only pass the live set of the **same slot and the same timeline**. Start Again, SAVE into
 * another slot and loading a slot never use this: they replace the trophies on purpose. Trophies are never
 * read from anything that has not already been validated for that slot (a future checkpoint record must pass the
 * 2A/L2 rules first), and merging never awards a trophy that is in neither set.
 */
internal fun mergeEarnedTrophies(restored: Set<String>, live: Set<String>): Set<String> =
    if (live.isEmpty() || restored.containsAll(live)) restored else LinkedHashSet(restored).apply { addAll(live) }
