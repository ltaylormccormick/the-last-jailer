package com.thelastjailer.app.data

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 10's conversation at the forge about the north stair ("Mention the north stair, and whose side you took.") is reached only by a
 * Kaelen who stood with Halvard on the stair in Chapter 9, and each of its ways out depends on how many places of the night before he has
 * already seen. A global random sample reaches it by luck, so the real-story reachability analysis asks a deterministic witness first
 * ([PivotWitnesses]). This holds that witness to account: it is found from the start of the story by real transitions, it replays step by
 * step as the game offers each choice, and it does not move when the scenes after Chapter 12 change.
 */
class Chapter10ForgeSideWitnessTest {

    private val nodes: List<StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val combat = { n: StoryNode -> n.combatEncounterId?.let { CombatRepository.encounter(it) } }

    private fun replay(start: GameState, steps: List<PivotWitness.Step>): GameState {
        var s = start
        for (step in steps) {
            assertEquals("the walk stands where the step says", step.sceneId, s.sceneId)
            val node = StoryRepository.node(step.sceneId)
            s = if (step.choiceIndex < 0) {
                val encounter = CombatRepository.encounter(node.combatEncounterId!!)
                s.resolveCombat(encounter, CombatOutcome(victory = step.won == true, damageTaken = 0))
            } else {
                val choice = node.choices[step.choiceIndex]
                assertEquals(step.label, choice.label)
                assertTrue("${step.sceneId}: '${choice.label}' is offered to the player in this state", choice in StoryRepository.visibleChoices(node, s))
                s.applyChoice(choice)
            }
        }
        return s
    }

    @Test
    fun `a Kaelen who stood with Halvard on the stair can stand in the forge conversation and in every way out of it`() {
        val witness = PivotWitnesses.forgeSide(nodes, combat)
        val maybe = witness.findPivotState()
        assertNotNull("no real walk reached the night before holding c9_stair_rules", maybe)
        val found = maybe!!
        assertTrue("c9_stair_rules" in found.state.flags)
        assertEquals("the_last_two", found.state.sceneId)
        val reached = witness.completeFrom(found.state)
        val side = nodes.single { it.id == "c10_forge_side" }
        val gated = side.choices.indices.filter { side.choices[it].requirements != null }
        println("FORGESIDE walk ${found.seed} reached the night before in ${found.trail.size} steps; the local search visited ${reached.statesVisited} states and " +
            "satisfied ${reached.satisfied.size} gated choices; c10_forge_side has ${gated.size} gated exits")
        gated.forEach { i ->
            assertTrue("c10_forge_side exit #$i ('${side.choices[i].label}', ${side.choices[i].requirements}) was never satisfied", ("c10_forge_side" to i) in reached.satisfied)
            val path = reached.paths.getValue("c10_forge_side" to i)
            println("FORGESIDE exit #$i ${side.choices[i].requirements}: " + path.joinToString(" > ") { it.sceneId.removePrefix("c10_") })
        }
    }

    @Test
    fun `the witness replays step by step as the game offers each choice, and ends standing where it says`() {
        val witness = PivotWitnesses.forgeSide(nodes, combat)
        val found = witness.findPivotState()!!
        val atNight = replay(GameState(sceneId = "fallen_knight"), found.trail)
        assertEquals("the_last_two", atNight.sceneId)
        assertEquals(found.state.flags, atNight.flags)
        val reached = witness.completeFrom(found.state)
        val side = nodes.single { it.id == "c10_forge_side" }
        side.choices.indices.filter { side.choices[it].requirements != null }.forEach { i ->
            val standing = replay(atNight, reached.paths.getValue("c10_forge_side" to i))
            assertEquals("c10_forge_side", standing.sceneId)
            assertTrue("exit #$i is offered in the state the witness ends in", side.choices[i] in StoryRepository.visibleChoices(side, standing))
        }
    }

    @Test
    fun `the walk that is found does not move when the scenes after Chapter 12 change`() {
        val upToTwelve = nodes.filter { (it.chapterId.removePrefix("chapter_").toIntOrNull() ?: 99) <= 12 }
        val whole = PivotWitnesses.forgeSide(nodes, combat).findPivotState()!!
        val cut = PivotWitnesses.forgeSide(upToTwelve, combat).findPivotState()!!
        assertEquals("every walk has its own sequence, and none of them reaches Chapter 13 before the night before the column", whole.seed, cut.seed)
        assertEquals(whole.trail.map { it.sceneId to it.choiceIndex }, cut.trail.map { it.sceneId to it.choiceIndex })
    }
}
