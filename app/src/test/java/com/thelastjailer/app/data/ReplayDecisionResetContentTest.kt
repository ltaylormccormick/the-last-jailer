package com.thelastjailer.app.data

import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity: the Chapter 4 aid decision and the Chapter 18 Ilsevet fate decision clear their own
 * sibling flags, so a replay never carries last telling's answer into this one. These tests pin the story
 * data: exactly which choices clear which flags, that nothing else about those choices moved, and that no
 * telling can get past either decision without passing through it.
 */
class ReplayDecisionResetContentTest {

    private val allNodes: List<StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val chapterNumber: Map<String, Int> = StoryRepository.chapters.associate { it.id to it.number }

    private class Spec(
        val nodeId: String,
        val next: String,
        val flag: String,
        val minStats: Map<StatType, Int>,
        val statDeltas: Map<StatType, Int>,
        val items: List<String>
    )

    @Test
    fun `the families are exactly the flags the two decisions set`() {
        assertEquals(setOf("accepted_dark_aid", "refused_dark_aid_again"), DARK_AID_FAMILY)
        assertEquals(setOf("captured_ilsevet", "showed_ilsevet_mercy", "deferred_ilsevets_fate"), ILSEVET_FATE_FAMILY)
    }

    @Test
    fun `both Chapter 4 answers clear the aid family and set only their own flag`() {
        val choices = StoryRepository.node(ReplayKit.AID_NODE).choices
        assertEquals(2, choices.size)
        choices.forEach { c ->
            assertEquals(c.label, DARK_AID_FAMILY, c.consequences.clearFlags)
            assertEquals(c.label, 1, c.consequences.setFlags.size)
            assertTrue(c.label, c.consequences.setFlags.single() in DARK_AID_FAMILY)
        }
        assertEquals(DARK_AID_FAMILY, choices.flatMap { it.consequences.setFlags }.toSet())
    }

    @Test
    fun `all six Chapter 18 answers clear the fate family and set only their own flag`() {
        ReplayKit.FATE_NODES.forEach { nodeId ->
            val choices = StoryRepository.node(nodeId).choices
            assertEquals(nodeId, 3, choices.size)
            choices.forEach { c ->
                assertEquals("$nodeId: ${c.label}", ILSEVET_FATE_FAMILY, c.consequences.clearFlags)
                assertEquals("$nodeId: ${c.label}", 1, c.consequences.setFlags.size)
                assertTrue("$nodeId: ${c.label}", c.consequences.setFlags.single() in ILSEVET_FATE_FAMILY)
            }
            assertEquals(nodeId, ILSEVET_FATE_FAMILY, choices.flatMap { it.consequences.setFlags }.toSet())
        }
    }

    @Test
    fun `exactly these eight choices set or clear any of the five flags, and no other choice does`() {
        val every = DARK_AID_FAMILY + ILSEVET_FATE_FAMILY
        val touching = allNodes
            .flatMap { node -> node.choices.map { node.id to it } }
            .filter { (_, c) -> (c.consequences.setFlags + c.consequences.clearFlags).any { it in every } }
        assertEquals(8, touching.size)
        assertEquals(setOf(ReplayKit.AID_NODE) + ReplayKit.FATE_NODES, touching.map { it.first }.toSet())
    }

    @Test
    fun `the eight choices keep their targets, requirements, stat changes and items`() {
        val blade = listOf("ilsevets_blade")
        val specs = listOf(
            Spec(ReplayKit.AID_NODE, "the_cost_aided", "accepted_dark_aid",
                emptyMap(), mapOf(StatType.COURAGE to 2), emptyList()),
            Spec(ReplayKit.AID_NODE, "the_cost_unaided", "refused_dark_aid_again",
                mapOf(StatType.HONOUR to 2), mapOf(StatType.HONOUR to 1), emptyList())
        ) + ReplayKit.FATE_NODES.flatMap { nodeId ->
            listOf(
                Spec(nodeId, "chapter18_end", "captured_ilsevet",
                    mapOf(StatType.COURAGE to 16), mapOf(StatType.COURAGE to 1), blade),
                Spec(nodeId, "chapter18_end", "showed_ilsevet_mercy",
                    mapOf(StatType.HONOUR to 15), mapOf(StatType.HONOUR to 1), blade),
                Spec(nodeId, "chapter18_end", "deferred_ilsevets_fate",
                    emptyMap(), emptyMap(), blade)
            )
        }
        assertEquals(8, specs.size)
        specs.forEach { sp ->
            val c = StoryRepository.node(sp.nodeId).choices.single { sp.flag in it.consequences.setFlags }
            val label = "${sp.nodeId}: ${c.label}"
            assertEquals(label, sp.next, c.nextNodeId)
            val req = c.requirements
            assertEquals(label, sp.minStats, req?.minStats ?: emptyMap<StatType, Int>())
            assertTrue(label, req == null || (req.requiredFlags.isEmpty() && req.forbiddenFlags.isEmpty()))
            assertEquals(label, sp.statDeltas, c.consequences.statDeltas)
            assertEquals(label, sp.items, c.consequences.grantItemIds)
            assertNull(label, c.consequences.unlockTrophy)
        }
    }

    // ---- no bypass: every telling must pass both decisions, or a stale flag would come back -------------

    private fun successors(node: StoryNode): List<String> {
        val viaChoices = node.choices.map { it.nextNodeId }
        val viaFight = node.combatEncounterId?.let { id ->
            val encounter = CombatRepository.encounter(id)
            listOfNotNull(encounter.victoryNodeId, encounter.defeatNodeId)
        } ?: emptyList()
        return viaChoices + viaFight
    }

    /** Every node reachable from the first scene of the story without ever standing on one of [avoid]. */
    private fun reachableAvoiding(avoid: Set<String>): Set<String> {
        val seen = HashSet<String>()
        val pending = ArrayList<String>()
        pending += StoryRepository.chapters.minByOrNull { it.number }!!.startNodeId
        while (pending.isNotEmpty()) {
            val id = pending.removeAt(pending.size - 1)
            if (id in avoid || !seen.add(id)) continue
            val node = StoryRepository.nodeOrNull(id) ?: continue
            pending += successors(node)
        }
        return seen
    }

    @Test
    fun `chapter 5 cannot be reached without passing the aid decision`() {
        val reached = reachableAvoiding(setOf(ReplayKit.AID_NODE))
        val furthest = reached.maxOf { chapterNumber.getValue(StoryRepository.node(it).chapterId) }
        assertTrue("reached chapter $furthest without the aid decision", furthest <= 4)
    }

    @Test
    fun `chapter 19 cannot be reached without passing a fate decision`() {
        val reached = reachableAvoiding(ReplayKit.FATE_NODES.toSet())
        val furthest = reached.maxOf { chapterNumber.getValue(StoryRepository.node(it).chapterId) }
        assertTrue("reached chapter $furthest without a fate decision", furthest <= 18)
    }
}
