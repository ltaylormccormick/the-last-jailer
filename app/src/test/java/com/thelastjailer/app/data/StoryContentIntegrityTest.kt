package com.thelastjailer.app.data

import org.junit.Assert.assertEquals
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

    private val nodesById = allNodes.associateBy { it.id }

    /** Every (sourceNodeId, targetNodeId) edge in the game: a choice's nextNodeId, or a combat node's victory/defeat targets. */
    private val allEdges: List<Pair<String, String>> = allNodes.flatMap { node ->
        val combatTargets = node.combatEncounterId
            ?.let { runCatching { CombatRepository.encounter(it) }.getOrNull() }
            ?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId) }
            ?: emptyList()
        (node.choices.map { it.nextNodeId } + combatTargets).map { node.id to it }
    }

    private val outgoingEdges: Map<String, List<String>> = allEdges.groupBy({ it.first }, { it.second })

    /**
     * Nodes a *different* chapter's choice or combat outcome routes into directly - the legitimate
     * way a chapter can have more than one true entry point (e.g. two upstream chapters, or two
     * flag-gated exits from the same upstream choice, each landing on a different variant of this
     * chapter's opening beat; see the trusted/guarded convergent-branch pattern used throughout
     * chapters IX, XIII and XXIX). [chapter.startNodeId] is only ever one of these - it doesn't
     * have to be the only one - so per-chapter reachability below seeds from all of them, not just
     * the one recorded on [com.thelastjailer.app.Chapter].
     */
    private val externalEntryPointsByChapter: Map<String, Set<String>> = allEdges
        .mapNotNull { (sourceId, targetId) ->
            val source = nodesById[sourceId] ?: return@mapNotNull null
            val target = nodesById[targetId] ?: return@mapNotNull null
            if (source.chapterId != target.chapterId) target.chapterId to target.id else null
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }

    /** Every node reachable by walking forward from any of [startIds], however many chapters that crosses. */
    private fun reachableFrom(startIds: Collection<String>): Set<String> {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>().apply { addAll(startIds) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            outgoingEdges[current]?.forEach { next -> if (next !in visited) queue.add(next) }
        }
        return visited
    }

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
    fun `every chapter's startNodeId resolves to one of that chapter's own nodes`() {
        val badStartNodes = StoryRepository.chapters
            .filterNot { chapter -> allNodes.any { it.id == chapter.startNodeId && it.chapterId == chapter.id } }
            .map { it.id to it.startNodeId }

        assertTrue(
            "Chapters whose startNodeId doesn't resolve to one of their own nodes (stale after a rename?): $badStartNodes",
            badStartNodes.isEmpty()
        )
    }

    /**
     * Stronger than "every node is reachable from *some* start node in the whole graph" (which a
     * chapter's own stale/dangling startNodeId can pass vacuously, since some other chapter's real
     * start node happens to reach the same id): walks forward from each chapter's own entry points -
     * [Chapter.startNodeId] plus any node a different chapter routes into directly, see
     * [externalEntryPointsByChapter] - and checks that walk actually covers every node tagged with
     * that chapter's id. Reaching past the chapter's end into the next one is expected and fine -
     * only a node missing from its own chapter's walk fails.
     */
    @Test
    fun `every node in a chapter is reachable from that chapter's own entry points`() {
        val unreachableByChapter = StoryRepository.chapters.mapNotNull { chapter ->
            val seeds = (externalEntryPointsByChapter[chapter.id].orEmpty() + chapter.startNodeId)
                .filter { it in allNodeIds } // a dangling startNodeId is covered by the test above
            val reachable = reachableFrom(seeds)
            val ownNodeIds = allNodes.filter { it.chapterId == chapter.id }.map { it.id }
            val missing = ownNodeIds.filterNot { it in reachable }
            if (missing.isEmpty()) null else chapter.id to missing
        }

        assertTrue(
            "Chapters with nodes none of their own entry points ever reach: $unreachableByChapter",
            unreachableByChapter.isEmpty()
        )
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

    /**
     * [CombatEncounter.isSurprise] opts a fight out of [com.thelastjailer.app.ui.StoryScreen]'s
     * pre-combat accent-stripe signal. Pinning the exact set here means adding or removing one is
     * a deliberate, reviewable edit to this test rather than a silent change to what the player is
     * warned about.
     */
    @Test
    fun `exactly the encounters written as a deliberate surprise opt out of the pre-combat signal`() {
        val encounters = allNodes.mapNotNull { it.combatEncounterId }.distinct().map { CombatRepository.encounter(it) }
        val surprises = encounters.filter { it.isSurprise }.map { it.id }.toSet()

        assertEquals(setOf("ilsevet_duel_encounter"), surprises)
    }
}
