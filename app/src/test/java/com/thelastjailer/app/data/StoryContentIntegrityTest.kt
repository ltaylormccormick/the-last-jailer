package com.thelastjailer.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-references every chapter's story content against itself and against the combat/item
 * catalogs, the same way the by-hand verification script has been run before every chapter commit
 * throughout this project's history. Making it a real test means a future chapter (or an edit to
 * an existing one) that breaks one of these invariants fails CI automatically instead of relying on
 * someone remembering to run the script.
 */
class StoryContentIntegrityTest {

    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val allNodeIds = allNodes.map { it.id }.toSet()

    @Test
    fun `every node id is unique across all chapters`() {
        val duplicates = allNodes.map { it.id }.groupingBy { it }.eachCount().filter { it.value > 1 }

        assertTrue("Duplicate node ids: ${duplicates.keys}", duplicates.isEmpty())
    }

    @Test
    fun `every choice's nextNodeId resolves to a real node`() {
        val dangling = allNodes
            .flatMap { node -> node.choices.map { node.id to it.nextNodeId } }
            .filter { (_, nextNodeId) -> nextNodeId !in allNodeIds }

        assertTrue("Choices pointing at nodes that don't exist: $dangling", dangling.isEmpty())
    }

    @Test
    fun `every combatEncounterId referenced by a node is defined in CombatRepository`() {
        val unresolvable = allNodes
            .mapNotNull { it.combatEncounterId }
            .filter { runCatching { CombatRepository.encounter(it) }.isFailure }

        assertTrue("combatEncounterId values with no matching encounter: $unresolvable", unresolvable.isEmpty())
    }

    @Test
    fun `every encounter reachable from a node has a real enemy, victory node and (if set) defeat node`() {
        val encounters = allNodes.mapNotNull { it.combatEncounterId }.distinct()
            .map { CombatRepository.encounter(it) }

        val missingEnemies = encounters.filter { runCatching { EnemyCatalog.get(it.enemyId) }.isFailure }
        assertTrue("Encounters with an undefined enemyId: ${missingEnemies.map { it.id }}", missingEnemies.isEmpty())

        val danglingVictory = encounters.filter { it.victoryNodeId !in allNodeIds }
        assertTrue(
            "Encounters whose victoryNodeId doesn't exist: ${danglingVictory.map { it.id to it.victoryNodeId }}",
            danglingVictory.isEmpty()
        )

        val danglingDefeat = encounters.filter { it.defeatNodeId != null && it.defeatNodeId !in allNodeIds }
        assertTrue(
            "Encounters whose defeatNodeId doesn't exist: ${danglingDefeat.map { it.id to it.defeatNodeId }}",
            danglingDefeat.isEmpty()
        )
    }

    @Test
    fun `every grantItemIds entry resolves to a real item in ItemCatalog`() {
        val missingItems = allNodes
            .flatMap { it.choices }
            .flatMap { it.consequences.grantItemIds }
            .distinct()
            .filter { ItemCatalog.get(it) == null }

        assertTrue("grantItemIds not defined in ItemCatalog: $missingItems", missingItems.isEmpty())
    }

    @Test
    fun `every requiredFlags entry is set by some choice's consequences somewhere in the story`() {
        val everSetFlags = allNodes.flatMap { it.choices }.flatMap { it.consequences.setFlags }.toSet()
        val everRequiredFlags = allNodes.flatMap { it.choices }
            .mapNotNull { it.requirements }
            .flatMap { it.requiredFlags }
            .toSet()

        val deadGates = everRequiredFlags - everSetFlags

        assertTrue("requiredFlags that no choice ever sets (a dead gate): $deadGates", deadGates.isEmpty())
    }

    @Test
    fun `every node is reachable from some chapter's start node`() {
        val startNodeIds = StoryRepository.chapters.map { it.startNodeId }.toSet()
        val nextNodeTargets = allNodes.flatMap { it.choices }.map { it.nextNodeId }.toSet()
        val encounterTargets = allNodes.mapNotNull { it.combatEncounterId }.distinct()
            .map { CombatRepository.encounter(it) }
            .flatMap { listOfNotNull(it.victoryNodeId, it.defeatNodeId) }
            .toSet()

        val reachable = startNodeIds + nextNodeTargets + encounterTargets
        val unreachable = allNodeIds - reachable

        assertTrue("Nodes never reached by a start node, choice, or combat outcome: $unreachable", unreachable.isEmpty())
    }

    @Test
    fun `every node has a non-blank illustrationId and narrativeText`() {
        val blankIllustration = allNodes.filter { it.illustrationId.isBlank() }.map { it.id }
        assertTrue("Nodes with a blank illustrationId: $blankIllustration", blankIllustration.isEmpty())

        val blankNarrative = allNodes.filter { it.narrativeText.isBlank() }.map { it.id }
        assertTrue("Nodes with blank narrativeText: $blankNarrative", blankNarrative.isEmpty())
    }

    @Test
    fun `a combat node has no choices of its own - CombatScreen replaces them`() {
        val combatNodesWithChoices = allNodes.filter { it.combatEncounterId != null && it.choices.isNotEmpty() }

        assertTrue(
            "Combat nodes that also define choices (choices would never be shown): " +
                combatNodesWithChoices.map { it.id },
            combatNodesWithChoices.isEmpty()
        )
    }

    @Test
    fun `a non-combat node has at least one choice, so play can never dead-end`() {
        val deadEnds = allNodes.filter { it.combatEncounterId == null && it.choices.isEmpty() }

        assertTrue("Nodes with no combat and no choices (a dead end): ${deadEnds.map { it.id }}", deadEnds.isEmpty())
    }
}
