package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.ending.EndingTrophies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cost of the nine ending trophies in a replayed game. [CheckpointReachableSizeTest] measures first tellings, in which no
 * ending trophy exists yet when the Chapter 30 arrival record is taken; once a game is replayed every record of the next
 * telling carries all nine. This puts them in every record of the reachable sample and holds the result to the same 10%
 * marker as the first-telling measurement. The pin below is that test's pin (161,198 bytes after Chapter 9) and moves only
 * with it, by the documented procedure in qa/CHECKPOINT_GROWTH_INVESTIGATION.md.
 */
class EndingTrophySizeTest {
    private val reachableAfterChapter9 = 161_198

    private fun replayed(): List<CheckpointRecord> =
        CheckpointReachableSizeTest.sample.largestArrival.toSortedMap().values.map { r ->
            CheckpointRecord.fromState(r.toGameState(1).copy(trophies = r.trophies + EndingTrophies.all), r.chapterNumber, Cp.L1)
        }

    @Test
    fun `the thirty reachable arrival records of a replayed game stay inside the reachable 10 percent marker`() {
        val firstTelling = CheckpointReachableSizeTest.composed
        val secondTelling = ReachableCheckpointMeasure.historyBytes(replayed())
        val allowed = reachableAfterChapter9 / 10 * 11
        println("ENDINGSIZE reachable first telling=$firstTelling B, replayed (nine more trophies in every record)=$secondTelling B, delta=${secondTelling - firstTelling} B, allowed=$allowed B, headroom=${allowed - secondTelling} B")
        assertTrue("replayed reachable records are $secondTelling bytes, more than 10% above the $reachableAfterChapter9 measured after Chapter 9 (allowed $allowed)", secondTelling <= allowed)
    }

    /** Thirty worst-case checkpoints, or one, with the story's every flag and item and the given trophies. */
    private fun worstCaseBytes(trophies: Set<String>, records: Int): Int {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val flags = nodes.flatMap { node -> node.choices.flatMap { it.consequences.setFlags } }.toSet()
        val items = nodes.flatMap { node -> node.choices.flatMap { it.consequences.grantItemIds } }.distinct()
        val device = SimulatedDevice()
        val store = Cp.store(device)
        val list = (if (records == 1) listOf(30) else (1..records).toList()).map { Cp.record(it, state = Cp.state(it, inventory = items, trophies = trophies, flags = flags)) }
        assertTrue(store.replaceAll(1, Cp.L1, list).isCommitted)
        return Cp.xmlSize(device.disk(Cp.file(1)))
    }

    /** Bytes of a worst-case save slot plus its Back snapshot, counted as in RecoveryPayloadSizingTest. */
    private fun recoveryBytes(trophies: Set<String>): Int {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val flags = nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        val items = nodes.flatMap { n -> n.choices.flatMap { it.consequences.grantItemIds } }.distinct()
        val last = StoryRepository.chapters.last()
        val state = GameState(
            activeSlot = 1, chapterId = last.id, sceneId = last.startNodeId, courage = 99, honour = 99, health = 100, maxHealth = 999,
            gold = 999_999, level = 99, xp = 99_999, xpToNextLevel = 99_999, inventory = items, trophies = trophies, flags = flags
        )
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        store.save(1, state)
        store.savePreviousState(1, state)
        return prefs.all.entries.filter { it.key.startsWith("1.") }.sumOf { (k, v) ->
            val text = if (v is Set<*>) v.joinToString("\u001F") else v.toString()
            k.toByteArray(Charsets.UTF_8).size + text.toByteArray(Charsets.UTF_8).size + 2
        }
    }

    /** A measurement, not a limit: the limits stay in CheckpointSizeTest, CheckpointReachableSizeTest and RecoveryPayloadSizingTest. */
    @Test
    fun `report what the nine ending trophies add to the worst-case sizes`() {
        val without = StoryRepository.availableTrophies
        val with = without + EndingTrophies.all
        val thirtyWithout = worstCaseBytes(without, 30)
        val thirtyWith = worstCaseBytes(with, 30)
        val oneWithout = worstCaseBytes(without, 1)
        val oneWith = worstCaseBytes(with, 1)
        println("ENDINGSIZE names=${EndingTrophies.all.size} characters=${EndingTrophies.all.sumOf { it.length }}")
        println("ENDINGSIZE thirty worst-case checkpoints: $thirtyWithout B -> $thirtyWith B (+${thirtyWith - thirtyWithout} B); pinned 530891, allowed ${530891 / 10 * 11}, hard ceiling ${600 * 1024}")
        println("ENDINGSIZE one worst-case checkpoint: $oneWithout B -> $oneWith B (+${oneWith - oneWithout} B); the existing test accepts 8000..20000")
        println("ENDINGSIZE recovery payload (state + Back snapshot): ${recoveryBytes(without)} B -> ${recoveryBytes(with)} B")
        assertTrue(thirtyWith > thirtyWithout && oneWith > oneWithout)
    }

    @Test
    fun `a replayed game's records, with all nine ending trophies, still pass the checkpoint rules`() {
        replayed().forEach { assertNull("chapter ${it.chapterNumber}", Cp.rules.validate(it, Cp.L1)) }
        assertEquals(30, replayed().size)
        assertTrue(replayed().all { r -> r.trophies.containsAll(EndingTrophies.all) })
    }
}
