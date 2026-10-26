package com.thelastjailer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Saves are stored by scene id, so a scene that shipped must never be removed, renamed or moved to another
 * chapter by later work. [ShippedSceneIds] is the list of every scene at the Chapter 5 expansion (826d719).
 * New scenes may be added freely; these may not go.
 */
class ShippedSceneIdsTest {

    @Test
    fun `the snapshot holds the 390 scenes that shipped`() {
        assertEquals(390, ShippedSceneIds.BY_ID.size)
    }

    @Test
    fun `every shipped scene still exists in the chapter it shipped in`() {
        val problems = ShippedSceneIds.BY_ID.mapNotNull { (id, chapter) ->
            val node = StoryRepository.nodeOrNull(id)
            when {
                node == null -> "$id: removed"
                node.chapterId != chapter -> "$id: moved from $chapter to ${node.chapterId}"
                else -> null
            }
        }
        assertTrue("Shipped scenes lost or moved:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `the fight scene keeps its encounter and the encounter keeps its two endings`() {
        assertEquals("unbound_encounter", StoryRepository.node("the_unbound").combatEncounterId)
        val encounter = CombatRepository.encounter("unbound_encounter")
        assertEquals("what_must_be_decided", encounter.victoryNodeId)
        assertEquals("unbound_defeat", encounter.defeatNodeId)
    }

    @Test
    fun `scenes added since the snapshot are all in the chapter their id says, or follow an earlier prefix`() {
        val added = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.filter { it.id !in ShippedSceneIds.BY_ID }
        val chapter6 = added.filter { it.chapterId == "chapter_6" }
        assertTrue("every scene added to Chapter 6 uses the c6_ prefix: ${chapter6.map { it.id }}", chapter6.all { it.id.startsWith("c6_") })
        assertEquals("the shipped scenes plus the added ones are exactly the chapter", 12 + chapter6.size, StoryRepository.nodesInChapter("chapter_6").size)
    }
}
