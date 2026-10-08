package com.thelastjailer.app.ui

import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.*
import org.junit.Test

class ReleasePolishTest {
    @Test fun proseUnwrapsWithoutJoiningParagraphs() {
        assertEquals("First line continues here.\n\nSecond paragraph.",
            readingText("First line\n  continues here.\n\nSecond paragraph."))
    }

    @Test fun everyEndingHasACompletionPanelAndItsReplayRewardIsCounted() {
        val endings = StoryRepository.nodesInChapter("chapter_30")
            .filter { node -> node.choices.any { it.nextNodeId == "fallen_knight" } }
        assertEquals(6, endings.size)
        assertTrue(endings.all { StoryRepository.isEnding(it) })
        assertTrue("The Watch Goes On" in StoryRepository.availableTrophies)
        assertTrue("Six, Not One" in StoryRepository.availableTrophies)
        assertFalse(StoryRepository.isEnding(StoryRepository.node("fallen_knight")))
    }
}
