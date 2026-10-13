package com.thelastjailer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-0 snapshot of the Chapter 1 node IDs that exist on `main`. A player's save stores a node ID, so
 * deleting or renaming one strands that save. Every later Chapter 1 batch must keep all of these.
 * New nodes may be added freely; legacy nodes may be re-routed, but never removed or renamed.
 */
class Chapter1LegacyNodesTest {

    private val legacyIds = listOf(
        "beneath_the_roots",
        "black_door_opened",
        "chapter1_end",
        "dwarven_path_buried",
        "dwarven_path_kept",
        "fallen_knight",
        "first_blood",
        "first_blood_defeat",
        "smiths_warning_confessed",
        "smiths_warning_helped",
        "smiths_warning_moved_on",
        "stonebeard_hold",
        "sword_drawn",
        "the_silent_forge",
        "walked_the_road"
    )

    @Test
    fun `every legacy Chapter 1 node still exists and still belongs to Chapter 1`() {
        val current = StoryRepository.nodesInChapter("chapter_1").map { it.id }.toSet()
        val missing = legacyIds.filterNot { it in current }
        assertTrue("legacy nodes missing or renamed (would strand saves): $missing", missing.isEmpty())
    }

    @Test
    fun `the chapter still starts at fallen_knight and the snapshot is complete for main`() {
        assertEquals("fallen_knight", StoryRepository.chapter("chapter_1")!!.startNodeId)
        assertEquals(15, legacyIds.size)
        assertEquals(legacyIds.toSet().size, legacyIds.size)
    }

    @Test
    fun `the three single-purpose route nodes are pinned exactly - text, art and exits`() {
        // The redesign plan reuses these three nodes unchanged inside the new routes, so their prose,
        // illustration and single exit must not drift. Hashes are String.hashCode of narrativeText.
        val pinned = mapOf(
            "black_door_opened" to Triple(1959063171, "iron_door_open_tunnel", listOf("Descend into the dark." to "beneath_the_roots")),
            "sword_drawn" to Triple(-1049031501, "knight_sword_drawn_door", listOf("Enter, blade first." to "beneath_the_roots")),
            "walked_the_road" to Triple(1140992229, "road_away_from_tree", listOf("Follow the broken ground down." to "beneath_the_roots"))
        )
        pinned.forEach { (id, expected) ->
            val n = StoryRepository.node(id)
            assertEquals("$id text", expected.first, n.narrativeText.hashCode())
            assertEquals("$id art", expected.second, n.illustrationId)
            assertEquals("$id exits", expected.third, n.choices.map { it.label to it.nextNodeId })
        }
    }

    @Test
    fun `the only edges between Chapter 1 and other chapters are the known ones`() {
        val all = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val edges = all.flatMap { n -> n.choices.map { n to it } }
            .filter { (n, c) -> (n.chapterId == "chapter_1") != (StoryRepository.node(c.nextNodeId).chapterId == "chapter_1") }
            .map { (n, c) -> n.id to c.nextNodeId }
            .toSet()
        val replay = setOf(
            "the_watch_continues_dark_aid_captured", "the_watch_continues_dark_aid_mercy",
            "the_watch_continues_dark_aid_deferred", "the_watch_continues_unaided_captured",
            "the_watch_continues_unaided_mercy", "the_watch_continues_unaided_deferred"
        ).map { it to "fallen_knight" }
        val expected = (replay + listOf("first_blood_defeat" to "word_at_stonebeard", "first_blood_defeat" to "word_at_stonebeard_thief", "first_blood_defeat" to "the_tended_dark", "chapter1_end" to "the_deeper_dark")).toSet()
        assertEquals(expected, edges)
    }
}
