package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryRepositoryTest {

    @Test
    fun `node resolves a real story node by id`() {
        val node = StoryRepository.node("fallen_knight")

        assertEquals("fallen_knight", node.id)
        assertEquals("chapter_1", node.chapterId)
    }

    @Test
    fun `node falls back to chapter one's start node for an unknown id`() {
        val node = StoryRepository.node("this_id_does_not_exist_anywhere")

        assertEquals(chapter1.startNodeId, node.id)
    }

    @Test
    fun `chapter resolves a real chapter by id`() {
        val chapter = StoryRepository.chapter("chapter_1")

        assertEquals(1, chapter?.number)
        assertEquals("fallen_knight", chapter?.startNodeId)
    }

    @Test
    fun `chapter returns null for an unknown chapter id`() {
        assertNull(StoryRepository.chapter("chapter_does_not_exist"))
    }

    @Test
    fun `nodesInChapter returns only nodes belonging to that chapter`() {
        val nodes = StoryRepository.nodesInChapter("chapter_1")

        assertTrue(nodes.isNotEmpty())
        assertTrue(nodes.all { it.chapterId == "chapter_1" })
    }

    @Test
    fun `nodesInChapter returns nothing for a chapter with no nodes`() {
        assertTrue(StoryRepository.nodesInChapter("chapter_does_not_exist").isEmpty())
    }

    @Test
    fun `visibleChoices hides a choice whose requirement isn't met`() {
        val node = testNode(
            gatedChoice("Only with courage", ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 10)))
        )

        val visible = StoryRepository.visibleChoices(node, GameState(courage = 1))

        assertTrue(visible.isEmpty())
    }

    @Test
    fun `visibleChoices shows a choice whose requirement is met`() {
        val node = testNode(
            gatedChoice("Only with courage", ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 10)))
        )

        val visible = StoryRepository.visibleChoices(node, GameState(courage = 10))

        assertEquals(1, visible.size)
    }

    @Test
    fun `visibleChoices always shows a choice with no requirement`() {
        val node = testNode(Choice(label = "Just go", nextNodeId = "next"))

        val visible = StoryRepository.visibleChoices(node, GameState())

        assertEquals(1, visible.size)
    }

    private fun gatedChoice(label: String, requirement: ChoiceRequirement) =
        Choice(label = label, nextNodeId = "next", requirements = requirement)

    private fun testNode(vararg choices: Choice) = StoryNode(
        id = "test_node",
        chapterId = "chapter_1",
        title = "Test Node",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = "",
        choices = choices.toList()
    )
}
