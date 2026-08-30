package com.thelastjailer.app.data

import com.thelastjailer.app.Chapter
import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode

/** All chapters and story nodes, aggregated from the per-chapter content files. */
object StoryRepository {
    val chapters: List<Chapter> =
        listOf(chapter1, chapter2, chapter3, chapter4, chapter5, chapter6, chapter7, chapter8)

    private val nodesById: Map<String, StoryNode> =
        (
            chapter1Nodes + chapter2Nodes + chapter3Nodes + chapter4Nodes + chapter5Nodes +
                chapter6Nodes + chapter7Nodes + chapter8Nodes
            ).associateBy { it.id }

    fun node(id: String): StoryNode = nodesById[id] ?: nodesById.getValue(chapter1.startNodeId)

    fun chapter(id: String): Chapter? = chapters.find { it.id == id }

    /** All nodes belonging to a chapter, in authoring order — used for the scene thumbnail strip. */
    fun nodesInChapter(chapterId: String): List<StoryNode> = nodesById.values.filter { it.chapterId == chapterId }

    /** Choices whose [com.thelastjailer.app.ChoiceRequirement] (if any) the current state satisfies. */
    fun visibleChoices(node: StoryNode, state: GameState): List<Choice> =
        node.choices.filter { it.requirements?.isSatisfiedBy(state) ?: true }
}
