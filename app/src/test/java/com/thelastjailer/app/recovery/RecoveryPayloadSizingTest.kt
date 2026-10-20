package com.thelastjailer.app.recovery

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MEASUREMENT, not a design decision. The 256 KiB bundle ceiling is provisional; this records how much of it the
 * data G3 will need actually uses, using the real story's worst-case flags, items and trophies saved through the
 * real [SaveStore]. The numbers are printed so they can be quoted in the PR and in qa/G2_RECOVERY_STORE.md.
 *
 * What G3 is expected to put in a bundle: the slot's live state, its one-step Back snapshot, and the earned
 * trophies (all inside the state). The checkpoint HISTORY file is deliberately not part of this estimate: copying
 * trusted history is the deferred 2C-1b-2 work.
 */
class RecoveryPayloadSizingTest {

    private fun worstCaseState(): GameState {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val flags = nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        val items = nodes.flatMap { n -> n.choices.flatMap { it.consequences.grantItemIds } }.distinct()
        val last = StoryRepository.chapters.last()
        return GameState(
            activeSlot = 1, chapterId = last.id, sceneId = last.startNodeId,
            courage = 99, honour = 99, health = 100, maxHealth = 999, gold = 999_999, level = 99, xp = 99_999, xpToNextLevel = 99_999,
            inventory = items, trophies = StoryRepository.availableTrophies, flags = flags
        )
    }

    /** Bytes to write every key and value of the slot as `key NUL value NUL` text (sets are joined). */
    private fun slotBytes(prefs: FakeSharedPreferences, slot: Int): Int =
        prefs.all.entries.filter { it.key.startsWith("$slot.") }.sumOf { (k, v) ->
            val text = if (v is Set<*>) v.joinToString("\u001F") else v.toString()
            k.toByteArray(Charsets.UTF_8).size + text.toByteArray(Charsets.UTF_8).size + 2
        }

    @Test
    fun `a worst-case slot - state, Back snapshot and trophies - fits the provisional ceiling many times over`() {
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        val state = worstCaseState()
        store.save(1, state)
        val liveOnly = slotBytes(prefs, 1)
        store.savePreviousState(1, state)
        val withBack = slotBytes(prefs, 1)

        // The timeline keys a save carries alongside the state: a 16-hex lineage and one 128-bit digest per chapter.
        val chapters = StoryRepository.chapters
        val lineage = "0123456789abcdef"
        val arrivals = chapters.joinToString(",") { "${it.number}:${"f".repeat(32)}" }
        val timeline = lineage.length + arrivals.length + 80
        val total = withBack + timeline

        println("G2 payload sizing: chapters=${chapters.size} flags=${state.flags.size} items=${state.inventory.size} " +
            "trophies=${state.trophies.size}; live save ${liveOnly} B, + Back snapshot ${withBack} B, + timeline keys ~${total} B " +
            "(provisional ceiling ${RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES} B, payload room ${RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES - RecoveryFormat.OVERHEAD_BYTES} B)")

        assertTrue("sanity: the worst case really is large (flags=${state.flags.size})", state.flags.size > 50 && state.trophies.isNotEmpty())
        assertTrue("worst case $total B should leave at least 4x headroom under the provisional ceiling",
            total * 4 < RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES)
    }
}
