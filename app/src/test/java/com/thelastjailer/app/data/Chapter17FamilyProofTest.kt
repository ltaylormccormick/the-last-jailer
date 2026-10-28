package com.thelastjailer.app.data

import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.data.FlagFamilyProbe.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B0 of the Chapter 17 expansion. The new scenes will read three families as alternatives (the Stonebeard policy of Chapter 13, the
 * door answered to Edda, Voss's rung), and the hub will leave five flags that must be gone by the time the chapter ends. This proves
 * on the committed story, before anything is built, what the plan assumes; every proof is stated so that it still holds once the
 * package is in. Three kinds of evidence, each sound for what it claims (see [FlagFamilyProbe]):
 *  - the structure of the choice graph proves a family is never held two at a time, and that it is held at least once on every road;
 *  - real walks of the real transitions witness each member, each combination the new scenes depend on, and the absence the plan
 *    relies on (no door answer for a player with no promise to Edda, no rung for a save that holds none);
 *  - structural invariants of Chapter 17 itself: the road scene, the fight and the frame stay on the only road to Chapter 18, and
 *    nothing in the chapter changes the families or adds an item, a trophy or a fight.
 */
class Chapter17FamilyProofTest {

    private class Family(val name: String, val flags: List<String>, val exhaustive: Boolean?)

    private val keepers = listOf("began_training_new_wardens", "stood_watch_rather_than_delegate", "deferred_the_question")
    private val door = listOf("c13_door_open", "c13_door_barred")
    private val hold = listOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")
    private val rung = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")

    /** The chain gate and its policy and door steps read the state at the chapter's first scene; the Voss scene reads it at the road. */
    private val scenes = listOf("the_signal", "the_road_to_ashfall_again")

    private val families = listOf(
        Family("keepers", keepers, true),
        Family("door", door, false),
        Family("hold", hold, false),
        Family("rung", rung, null)
    )

    private val planned = Chapter17Pin.plannedFlags

    private object Evidence {
        val nodes: List<StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val combatTargets: (StoryNode) -> List<String> = { n ->
            n.combatEncounterId?.let { CombatRepository.encounter(it) }?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId ?: it.victoryNodeId) }
                ?: emptyList()
        }
        val graph = FlagFamilyProbe.StoryGraph(nodes, combatTargets)
        const val WALKS = 30_000
        var walker: FlagFamilyProbe.Walker? = null

        fun walked(universe: Set<String>, targets: Set<String>): FlagFamilyProbe.Walker = walker ?: FlagFamilyProbe.Walker(
            nodes, combat = { n -> n.combatEncounterId?.let { CombatRepository.encounter(it) } },
            targets = targets, universe = universe, lean = universe
        ).also { it.run(WALKS, seed = 0x17L); walker = it }
    }

    private val universe: Set<String> = keepers.toSet() + door + hold + rung + planned
    private val targets: Set<String> = scenes.toSet() + "c18_gate_after_ashfall" + "no_more_envoys"

    private val report = StringBuilder()
    private fun say(s: String) { println(s); report.appendLine(s) }

    // ---- structure: exclusive, and exhaustive where the plan needs it, on every road ----

    @Test
    fun `by the structure of the story the policy, the door, the promise and the rung are each held at most one at a time`() {
        val failures = ArrayList<String>()
        scenes.forEach { at ->
            families.forEach { f ->
                val p = Evidence.graph.prove(f.flags.toSet(), at)
                say("STRUCTURE ${f.name} @$at: exclusive=${p.exclusive} exhaustive=${p.exhaustive} ${p.notes}")
                if (!p.exclusive) failures += "${f.name} @$at: not exclusive ${p.notes}"
                if (f.exhaustive == true && !p.exhaustive) failures += "${f.name} @$at: not exhaustive ${p.notes}"
            }
        }
        assertTrue("family assumptions that the structure does not support: $failures", failures.isEmpty())
    }

    // ---- reachable states: every member, every combination the new scenes depend on, and every absence the plan relies on ----

    @Test
    fun `real walks stand in every policy, every door state including none, every rung, and every pairing of policy and door`() {
        val w = Evidence.walked(universe, targets)
        say("WALKS ${w.walksFinished} walks, ${w.stepsTaken} steps; projections seen: " + targets.sorted().joinToString { "$it=${w.seen[it]?.size ?: 0}" })
        val failures = ArrayList<String>()
        targets.forEach { if (w.seen[it].isNullOrEmpty()) failures += "no walk reached $it" }
        scenes.forEach { at ->
            families.forEach { f ->
                f.flags.forEach { m -> if (!w.witnessed(at, Question("m", setOf(m), f.flags.toSet() - m))) failures += "${f.name} @$at: $m alone was never witnessed" }
                val none = w.witnessed(at, Question("none", forbidden = f.flags.toSet()))
                when (f.exhaustive) {
                    true -> if (none) failures += "${f.name} @$at: a walk stood there holding none of ${f.flags}"
                    false -> if (!none) failures += "${f.name} @$at: no walk stood there holding none of ${f.flags}"
                    null -> say("  ${f.name} @$at: 'none' witnessed = $none")
                }
                FlagFamilyProbe.familyQuestions(f.name, f.flags).filter { it.name.contains(":both:") }.forEach { q ->
                    if (w.witnessed(at, q)) failures += "${f.name} @$at: ${q.name} was witnessed together"
                }
            }
        }
        // the chain gate: one policy step and one door step, so every pair must be a state a player can be in
        val doorStates = door.map { setOf(it) to (door.toSet() - it) } + (emptySet<String>() to door.toSet())
        keepers.forEach { k ->
            doorStates.forEach { (need, forbid) ->
                val q = Question("policy:$k+door:${need.firstOrNull() ?: "neither"}", need + k, forbid + (keepers.toSet() - k))
                val ok = w.witnessed("the_signal", q)
                say("PAIR ${q.name}: $ok")
                if (!ok) failures += "${q.name} never witnessed"
            }
        }
        assertTrue("assumptions that real walks do not bear out: $failures", failures.isEmpty())
    }

    // ---- the door answer goes with the promise to Edda, so "neither" is the normal case for a player with no promise ----

    @Test
    fun `a door answer exists exactly where a promise to Edda does, in the structure and in every walk`() {
        val g = Evidence.graph
        val promised = hold.toSet()
        val intoDoor = g.edges.filter { it.to == "c13_door" }
        assertTrue(intoDoor.isNotEmpty())
        intoDoor.forEach { e ->
            assertTrue("${e.from}#${e.index}: the door is asked only of a player who holds a promise", e.choice != null && (e.choice.requirements?.requiredFlags ?: emptySet()).any { it in promised })
        }
        val doorSetters = g.settersOf(door.toSet())
        assertTrue(doorSetters.isNotEmpty())
        doorSetters.forEach { assertEquals("a door answer is set only at the question", "c13_door", it.from) }
        val replies = setOf("c13_door_open_reply", "c13_door_barred_reply")
        val skipped = g.edges.filter { it.to == "who_stands_with_stonebeard" && it.from !in replies }
        assertTrue(skipped.isNotEmpty())
        skipped.forEach { e ->
            assertTrue("${e.from}#${e.index}: the question is skipped only by a player who holds no promise", e.choice != null && (e.choice.requirements?.forbiddenFlags ?: emptySet()).containsAll(promised))
        }
        hold.forEach { h ->
            g.clearersOf(h).forEach { c -> assertEquals("$h is cleared only in Chapter 7, where it is chosen", "chapter_7", Evidence.nodes.single { it.id == c.from }.chapterId) }
        }
        val w = Evidence.walked(universe, targets)
        scenes.forEach { at ->
            hold.forEach { h -> assertTrue("$h with no door answer at $at", !w.witnessed(at, Question("hold_no_door", setOf(h), door.toSet()))) }
            door.forEach { d -> assertTrue("$d with no promise at $at", !w.witnessed(at, Question("door_no_hold", setOf(d), promised))) }
            assertTrue("no walk with neither a promise nor a door answer at $at", w.witnessed(at, Question("neither", forbidden = promised + door)))
            door.forEach { d -> assertTrue("no walk with the door answered $d at $at", w.witnessed(at, Question("answered", setOf(d)))) }
        }
        say("DOOR ${intoDoor.size} choices lead to the question, ${doorSetters.size} set an answer, ${skipped.size} skip it")
    }

    // ---- Chapter 17 changes none of the state the new scenes read, and the five flags are gone before Chapter 18 ----

    @Test
    fun `nothing in Chapter 17 sets or clears the policy, the door, the promise or the rung`() {
        val read = (keepers + door + hold + rung).toSet()
        val own = StoryRepository.nodesInChapter("chapter_17")
        val offenders = own.flatMap { n -> n.choices.filter { c -> c.consequences.setFlags.any { it in read } || c.consequences.clearFlags.any { it in read } }.map { n.id } }
        assertEquals("choices in Chapter 17 that change what the new scenes read", emptyList<String>(), offenders)
    }

    @Test
    fun `no Chapter 17 flag is held on arrival at Chapter 17 or at Chapter 18, so a replay starts the hub clean`() {
        val w = Evidence.walked(universe, targets)
        listOf("the_signal", "c18_gate_after_ashfall", "no_more_envoys").forEach { at ->
            planned.forEach { f ->
                assertTrue("$f held on arrival at $at", !w.witnessed(at, Question("stale", setOf(f))))
            }
        }
        say("STALE none of ${planned.size} Chapter 17 flags is held at the_signal or no_more_envoys across ${w.walksFinished} walks (true now because the flags do not exist; true after the package because the last choice into the chamber clears them)")
    }

    // ---- the road, the fight and the frame stay on the only road to Chapter 18; no item, trophy or fight is added ----

    @Test
    fun `the road scene, the guardian fight and the frame remain on the only road to Chapter 18`() {
        val g = Evidence.graph
        fun leaving(id: String) = g.edges.filter { it.from == id }.toSet()
        assertTrue("every road to the guardian passes the road scene", g.mustPass(leaving("the_road_to_ashfall_again"), "the_chamber_defended"))
        assertTrue("every road to the frame passes the guardian fight", g.mustPass(leaving("the_chamber_defended"), "what_must_be_broken"))
        assertTrue("every road to the defeat scene passes the guardian fight", g.mustPass(leaving("the_chamber_defended"), "chamber_guardian_defeat"))
        assertTrue("every road to Chapter 18 passes the frame", g.mustPass(leaving("what_must_be_broken"), "no_more_envoys"))
        assertTrue("every road to Chapter 18 passes the end of Chapter 17", g.mustPass(leaving("chapter17_end"), "no_more_envoys"))
        assertTrue("every road to Chapter 18 passes the first scene of Chapter 17", g.mustPass(leaving("the_signal"), "no_more_envoys"))
        assertEquals("the frame is entered only from the fight or its defeat scene", setOf("the_chamber_defended", "chamber_guardian_defeat"), g.edges.filter { it.to == "what_must_be_broken" }.map { it.from }.toSet())
        assertEquals("the end of the chapter is entered only from the frame", setOf("what_must_be_broken"), g.edges.filter { it.to == "chapter17_end" }.map { it.from }.toSet())
        assertTrue("every road to the first Chapter 18 scene passes the end of Chapter 17", g.mustPass(leaving("chapter17_end"), "c18_gate_after_ashfall"))
        assertEquals("Chapter 18 is entered only from the end of Chapter 17", setOf("chapter17_end"), g.edges.filter { it.to == "c18_gate_after_ashfall" }.map { it.from }.toSet())
        assertEquals("the end of Chapter 17 leads only to the gate that opens Chapter 18", setOf("c18_gate_after_ashfall"), g.edges.filter { it.from == "chapter17_end" }.map { it.to }.toSet())
        assertEquals("the shipped first scene of Chapter 18 is now entered from the last scene before it", setOf("c18_three_days"), g.edges.filter { it.to == "no_more_envoys" }.map { it.from }.toSet())
        assertEquals("Chapter 17 is entered only from the end of Chapter 16", setOf("chapter16_end"), g.edges.filter { it.to == "the_signal" }.map { it.from }.toSet())
    }

    @Test
    fun `Chapter 17 grants only the frame component, unlocks no trophy of its own and fights only the guardian`() {
        val own = StoryRepository.nodesInChapter("chapter_17")
        val grants = own.flatMap { n -> n.choices.map { n.id to it.consequences.grantItemIds } }.filter { it.second.isNotEmpty() }
        assertEquals("what_must_be_broken is the only scene that grants", setOf("what_must_be_broken"), grants.map { it.first }.toSet())
        assertEquals(listOf("twisted_frame_component"), grants.flatMap { it.second }.distinct())
        assertEquals("three frame choices, one item each", 3, grants.size)
        assertEquals("no choice in Chapter 17 unlocks a trophy", emptyList<String>(), own.flatMap { n -> n.choices.mapNotNull { it.consequences.unlockTrophy } })
        assertEquals("the only fight in Chapter 17", setOf("chamber_guardian_encounter"), own.mapNotNull { it.combatEncounterId }.toSet())
        assertEquals("the guardian's trophy is the only one the chapter can award", "What Almost Finished", CombatRepository.encounter("chamber_guardian_encounter").unlockTrophy)
    }

    // ---- the Journal's headroom for the five hidden flags the package adds ----

    @Test
    fun `five more hidden flags fit the Journal`() {
        val storyFlags = Evidence.nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet() - planned.toSet()
        val described = FlagDescriptions.entries.map { it.flag }.toSet()
        val hidden = FlagDescriptions.hidden.keys
        val visible = storyFlags.intersect(described).size
        val hiddenCount = storyFlags.intersect(hidden).size
        val after = storyFlags.size + planned.size
        say(
            "JOURNAL story flags=${storyFlags.size} visible=$visible (allowed ${storyFlags.size / 2 + 20}) hidden=$hiddenCount (needed ${storyFlags.size / 3}); " +
                "with ${planned.size} more hidden flags: allowed visible ${after / 2 + 20}, needed hidden ${after / 3}, hidden ${hiddenCount + planned.size}"
        )
        assertTrue("visible entries $visible exceed the allowance ${after / 2 + 20} once the flags are added", visible <= after / 2 + 20)
        assertTrue("hidden entries ${hiddenCount + planned.size} fall short of ${after / 3}", hiddenCount + planned.size >= after / 3)
        assertTrue("a Chapter 17 flag is already a visible entry", planned.none { it in described })
    }
}
