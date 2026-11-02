package com.thelastjailer.app.data

import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.explore
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.picker
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.data.Chapter6Kit.winOrLose
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 9 as written: the three answers to the vow (three days used in a hurry, used properly, or lost to indecision), the
 * long table at Stonebeard with its three places of which one or two are seen, Halvard's loan of a blade for Voss, the six
 * ways into the Sanctum, the ward on the cell wing, the blade laid beside Voss before the chains come off, Ordrun's last words,
 * and the way home. These
 * tests pin the routing and the gates, what each decision costs and gives, that every route reaches Chapter 10 with a
 * borrowed blade in Voss's hand, and that the twelve scenes that shipped are word for word as before.
 */
class Chapter9ExpansionTest {

    private val goal = "the_last_two"
    private val spots = setOf("c9_plans", "c9_stores", "c9_north_stair", "c9_gate_work")

    private fun words(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun isGoal(s: GameState) = s.sceneId == goal
    private fun draughts(s: GameState) = s.inventory.count { it == "healing_draught" }

    // ---------------------------------------------------------------------------------------------
    // Into the chapter: the vow
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the chapter still opens on the shipped scene and the vow answers lead to three different days`() {
        assertEquals("a_debt_remembered_guarded", StoryRepository.chapters.single { it.id == "chapter_9" }.startNodeId)
        listOf("a_debt_remembered_trusted", "a_debt_remembered_guarded").forEach { id ->
            assertEquals(listOf("c9_days_now"), visible(at(id, flags = setOf("vowed_to_return_now"))).map { it.nextNodeId })
            assertEquals(listOf("c9_days_prepared"), visible(at(id, flags = setOf("vowed_to_return_prepared"))).map { it.nextNodeId })
            assertEquals(listOf("c9_days_late"), visible(at(id, flags = setOf("undecided_on_voss"))).map { it.nextNodeId })
        }
        listOf("c9_days_now", "c9_days_prepared", "c9_days_late").forEach {
            assertEquals(listOf("c9_the_table"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `the vow decides how many places at the table there is time for`() {
        val now = take(at("a_debt_remembered_guarded", flags = setOf("vowed_to_return_now")), "c9_days_now")
        val prepared = take(at("a_debt_remembered_guarded", flags = setOf("vowed_to_return_prepared")), "c9_days_prepared")
        val late = take(at("a_debt_remembered_guarded", flags = setOf("undecided_on_voss")), "c9_days_late")
        assertTrue("moved_without_delay" in now.flags && "hub_second" in now.flags)
        assertTrue("moved_with_preparation" in prepared.flags && "hub_second" !in prepared.flags)
        assertTrue("decided_at_last" in late.flags && "hub_second" in late.flags)
    }

    @Test
    fun `the first answer to the vow clears everything the chapter owns and nothing from another chapter`() {
        val stale = C9_TELLING_FLAGS + setOf("c8_rotation", "c7_rung_trusted", "told_halvard_the_truth", "vowed_to_return_prepared")
        val after = take(at("a_debt_remembered_trusted", flags = stale), "c9_days_prepared")
        assertTrue(after.flags.intersect(C9_TELLING_FLAGS - "moved_with_preparation").isEmpty())
        assertTrue("moved_with_preparation" in after.flags)
        assertTrue(after.flags.containsAll(setOf("c8_rotation", "c7_rung_trusted", "told_halvard_the_truth")))
    }

    // ---------------------------------------------------------------------------------------------
    // The long table
    // ---------------------------------------------------------------------------------------------

    private fun pathsAt(spot: Char, letter: Boolean, terms: Boolean) = when (spot) {
        'a' -> if (letter) 2 else 3
        'b' -> 3
        else -> if (terms) 3 else 2
    }

    private fun expectedWalks(single: Boolean, letter: Boolean, terms: Boolean): Int {
        val ns = listOf('a', 'b', 'c').map { pathsAt(it, letter, terms) }
        if (single) return ns.sum()
        var total = 0
        for (i in ns.indices) for (j in ns.indices) if (i != j) total += ns[i] * ns[j]
        return total
    }

    @Test
    fun `the table offers one place when time is short and two when it is not, and the place seen last differs from the first`() {
        listOf(
            Triple(true, false, false), Triple(true, true, true), Triple(false, false, true), Triple(false, true, false)
        ).forEach { (single, letter, terms) ->
            val flags = HashSet<String>()
            if (single) flags += "hub_second"
            if (letter) flags += "c8_wrote_halvard"
            if (terms) flags += "c7_hold_terms"
            val walks = explore(at("c9_the_table", flags = flags), { it.sceneId == "c9_table_done" })
            assertEquals("single=$single letter=$letter terms=$terms", expectedWalks(single, letter, terms), walks.size)
            walks.forEach { (trail, end) ->
                val seen = trail.filter { it in spots }
                assertEquals(trail.toString(), if (single) 1 else 2, seen.size)
                assertEquals(trail.toString(), seen.size, seen.toSet().size)
                assertTrue("no marker outlives the hub", end.flags.intersect(HUB_MARKERS).isEmpty())
            }
        }
    }

    @Test
    fun `the north stair and the gate are alternatives decided by what Halvard said to Edda`() {
        val withTerms = visible(at("c9_the_table", flags = setOf("c7_hold_terms"))).map { it.nextNodeId }
        assertEquals(listOf("c9_plans", "c9_stores", "c9_north_stair"), withTerms)
        listOf(setOf("c7_hold_refused"), setOf("c7_hold_hidden"), emptySet<String>()).forEach {
            assertEquals(listOf("c9_plans", "c9_stores", "c9_gate_work"), visible(at("c9_the_table", flags = it)).map { c -> c.nextNodeId })
        }
        assertEquals(listOf("c9_stores", "c9_gate_work"), visible(at("c9_the_table_two", flags = setOf("hub_a"))).map { it.nextNodeId })
    }

    @Test
    fun `the plans give the name and the ward in one go to the one who wrote the letter, and one each to everyone else`() {
        val letter = at("c9_plans", flags = setOf("c8_wrote_halvard"))
        assertEquals(listOf("c9_plans_letter", "c9_the_table_two"), visible(letter).map { it.nextNodeId })
        val read = take(take(letter, "c9_plans_letter"), "c9_the_table_two")
        assertTrue(read.flags.containsAll(setOf("c9_knows_ordrun", "c9_knows_cells")))
        val plain = at("c9_plans")
        assertEquals(listOf("c9_plans_ordrun", "c9_plans_cells", "c9_the_table_two"), visible(plain).map { it.nextNodeId })
        val ordrun = take(take(plain, "c9_plans_ordrun"), "c9_the_table_two")
        assertTrue("c9_knows_ordrun" in ordrun.flags && "c9_knows_cells" !in ordrun.flags)
        val cells = take(take(plain, "c9_plans_cells"), "c9_the_table_two")
        assertTrue("c9_knows_cells" in cells.flags && "c9_knows_ordrun" !in cells.flags)
        assertTrue("c9_knows_cells" !in take(plain, "c9_the_table_two").flags)
    }

    @Test
    fun `Voss sending the Order's hunters away gives Halvard something to say about Ordrun that nobody else hears`() {
        val calledOff = at("c9_plans", flags = setOf("c7_voss_called_them_off"))
        assertEquals(listOf("c9_plans_ordrun", "c9_plans_cells", "c9_plans_hunted", "c9_the_table_two"), visible(calledOff).map { it.nextNodeId })
        val read = take(take(calledOff, "c9_plans_hunted"), "c9_the_table_two")
        assertTrue("c9_knows_ordrun" in read.flags && "c9_knows_cells" !in read.flags)
        assertTrue("c9_plans_hunted" !in visible(at("c9_plans")).map { it.nextNodeId })
        assertTrue(words(node("c9_plans_hunted").narrativeText).contains("worth more to him as an example than as a corpse"))
        val withLetter = at("c9_plans", flags = setOf("c7_voss_called_them_off", "c8_wrote_halvard"))
        assertEquals(listOf("c9_plans_letter", "c9_plans_hunted", "c9_the_table_two"), visible(withLetter).map { it.nextNodeId })
    }

    @Test
    fun `the stores give more remedies where the stair kept the armoury and none at all to the one who takes the kit`() {
        val stair = at("c9_stores", flags = setOf("c7_ground_stair"))
        assertEquals(2, draughts(take(stair, "c9_stores_supplies")))
        val yard = at("c9_stores", flags = setOf("c7_ground_yard"))
        assertEquals(1, draughts(take(yard, "c9_stores_supplies")))
        val road = at("c9_stores", flags = setOf("c7_ground_road"), gold = 40)
        val roadTaken = take(road, "c9_stores_supplies")
        assertEquals(1, draughts(roadTaken))
        assertEquals("the Hold that was never touched also put coin by", 55, roadTaken.gold)
        assertEquals(40, take(yard.copy(gold = 40), "c9_stores_supplies").gold)
        assertEquals(1, draughts(take(at("c9_stores"), "c9_stores_supplies")))
        listOf(stair, yard, road, at("c9_stores")).forEach {
            val kit = take(it, "c9_stores_kit")
            assertEquals(0, draughts(kit))
            assertTrue("c9_kit" in kit.flags)
            assertEquals("one visible way to the supplies", 1, visible(it).count { c -> c.nextNodeId == "c9_stores_supplies" })
        }
    }

    @Test
    fun `at the north stair standing with Halvard earns a remedy and standing with the boy earns Honour and a carter`() {
        val start = at("c9_north_stair", honour = 3, flags = setOf("c7_hold_terms"))
        val rules = take(start, "c9_stair_rules")
        assertEquals(1, draughts(rules))
        assertEquals(3, rules.honour)
        assertEquals(setOf("c9_stair_rules"), rules.flags.intersect(C9_DAYS_FAMILY))
        val people = take(start, "c9_stair_people")
        assertEquals(4, people.honour)
        assertEquals(0, draughts(people))
        assertEquals(setOf("c9_stair_people"), people.flags.intersect(C9_DAYS_FAMILY))
        val leave = take(at("c9_north_stair", flags = C9_DAYS_FAMILY), "c9_the_table_two")
        assertTrue(leave.flags.intersect(C9_DAYS_FAMILY).isEmpty())
    }

    @Test
    fun `a day on the gate costs health and earns Honour whichever way the door was left`() {
        listOf(setOf("c7_hold_refused"), setOf("c7_hold_hidden"), emptySet()).forEach { flags ->
            val start = at("c9_gate_work", honour = 3, health = 300, flags = flags)
            assertEquals("one visible way onto the ladder", 1, visible(start).count { it.nextNodeId == "c9_gate_braced" })
            val braced = take(start, "c9_gate_braced")
            assertEquals(4, braced.honour)
            assertEquals(300 - 10 + 1, braced.health)
            assertEquals(setOf("c9_gate_braced"), braced.flags.intersect(C9_DAYS_FAMILY))
        }
        assertEquals(
            "the label names the choice made at the forge",
            listOf("Hold the ladder, and the gate, for the man who would not open his door."),
            visible(at("c9_gate_work", flags = setOf("c7_hold_refused"))).filter { it.nextNodeId == "c9_gate_braced" }.map { it.label }
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Halvard's loan
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Halvard's loan of a blade reads what he made of Voss, and every telling gets one`() {
        val done = at("c9_table_done")
        assertEquals(listOf("c9_loan_trusts"), visible(at("c9_table_done", flags = setOf("c7_halvard_trusts_voss"))).map { it.nextNodeId })
        assertEquals(listOf("c9_loan_doubts"), visible(at("c9_table_done", flags = setOf("c7_halvard_doubts_voss"))).map { it.nextNodeId })
        assertEquals(listOf("c9_loan_plain"), visible(done).map { it.nextNodeId })
        listOf("c9_loan_trusts", "c9_loan_doubts", "c9_loan_plain").forEach {
            assertEquals(listOf("the_plan_takes_shape"), visible(at(it)).map { c -> c.nextNodeId })
            assertTrue(words(node(it).narrativeText).contains("loan"))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The way in
    // ---------------------------------------------------------------------------------------------

    private fun plan(flags: Set<String> = emptySet(), courage: Int = 1, honour: Int = 0) =
        visible(at("the_plan_takes_shape", flags = flags, courage = courage, honour = honour)).map { it.nextNodeId }

    @Test
    fun `the three plans that shipped are still there and keep their stat gates`() {
        assertEquals(listOf("into_the_ash_default"), plan())
        assertEquals(listOf("into_the_ash_loud", "into_the_ash_default"), plan(courage = 8))
        assertEquals(listOf("into_the_ash_quiet", "into_the_ash_default"), plan(honour = 7))
        assertEquals(3, plan(courage = 8, honour = 7).size)
    }

    @Test
    fun `each new way in needs what Chapter 8 left behind`() {
        assertEquals(listOf("into_the_ash_default", "c9_in_by_stair"), plan(setOf("c8_voss_cells")))
        assertEquals("the cells have moved by the time a day is wasted", listOf("into_the_ash_default"),
            plan(setOf("c8_voss_cells", "decided_at_last")))
        listOf(setOf("c8_rotation", "c9_kit"), setOf("c8_rotation", "c8_exit_ramp"), setOf("c8_rotation"),
            setOf("c8_rotation", "c8_exit_ramp", "c9_kit")).forEach {
            assertEquals("exactly one way over the wall for $it", 1, plan(it).count { id -> id == "c9_in_by_wall" })
        }
        assertEquals(listOf("into_the_ash_default"), plan(setOf("c9_kit")))
        assertEquals(listOf("into_the_ash_default", "c9_in_by_cart_farmers"), plan(setOf("c8_ash_farmers")))
        assertEquals(listOf("into_the_ash_default"), plan(setOf("c8_tithe_seen")))
        assertEquals(listOf("into_the_ash_default"), plan(setOf("c9_stair_people")))
        assertEquals(listOf("into_the_ash_default", "c9_in_by_cart_carter"), plan(setOf("c8_tithe_seen", "c9_stair_people")))
        assertEquals("the farmers' cart wins when both are offered", listOf("into_the_ash_default", "c9_in_by_cart_farmers"),
            plan(setOf("c8_tithe_seen", "c9_stair_people", "c8_ash_farmers")))
    }

    @Test
    fun `every way in has its own cost and ends at the door of the cell wing`() {
        val s = at("the_plan_takes_shape", gold = 50, health = 300, honour = 3, courage = 99, flags = setOf("c8_voss_cells", "c8_rotation", "c8_ash_farmers"))
        assertEquals(4, take(s, "c9_in_by_stair").honour)
        assertEquals(30, take(s, "c9_in_by_cart_farmers").gold)
        val wall = takeLabel(s, "Climb the south wall at the watch change, and trust your hands.")
        assertEquals(300 - 10 + 1, wall.health)
        assertEquals(300 + 1, takeLabel(s.copy(flags = s.flags + "c9_kit"), "Climb the south wall at the watch change, with Halvard's rope.").health)
        assertEquals(300 + 1, takeLabel(s.copy(flags = s.flags + "c8_exit_ramp"), "Climb the south wall at the watch change, from the ramp's foot you already know.").health)
        val loud = take(at("the_plan_takes_shape", health = 300, courage = 8), "into_the_ash_loud")
        assertEquals(300 + 1 - 15 + 1, take(loud, "c9_the_door").health)
        assertEquals(300 + 1 - 10 + 1, take(take(at("the_plan_takes_shape", health = 300), "into_the_ash_default"), "c9_the_door").health)
        assertEquals(300 + 1 + 1, take(take(at("the_plan_takes_shape", health = 300, honour = 7), "into_the_ash_quiet"), "c9_the_door").health)
        listOf("c9_in_by_stair", "c9_in_by_wall", "c9_in_by_cart_farmers", "c9_in_by_cart_carter").forEach {
            assertEquals(listOf("c9_the_door"), visible(at(it)).map { c -> c.nextNodeId })
        }
        listOf("into_the_ash_loud", "into_the_ash_quiet", "into_the_ash_default").forEach {
            assertEquals(listOf("c9_the_door"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `the six plans are alternatives, and the carter's cart costs nothing but the stair`() {
        val s = at("the_plan_takes_shape", gold = 50, courage = 99, honour = 99,
            flags = C9_WAY_FAMILY + setOf("c8_voss_cells", "c8_rotation", "c8_ash_farmers"))
        val picks = visible(s)
        assertTrue(picks.size >= 6)
        picks.forEach { c ->
            val after = s.applyChoice(c)
            assertEquals(c.label, 1, after.flags.count { it in C9_WAY_FAMILY })
        }
        val carter = take(at("the_plan_takes_shape", gold = 50, flags = setOf("c8_tithe_seen", "c9_stair_people")), "c9_in_by_cart_carter")
        assertEquals(50, carter.gold)
    }

    // ---------------------------------------------------------------------------------------------
    // The door
    // ---------------------------------------------------------------------------------------------

    private fun door(flags: Set<String> = emptySet(), courage: Int = 1) =
        visible(at("c9_the_door", flags = flags, courage = courage)).map { it.label }

    @Test
    fun `a stranger with nothing can only wait for the patrol, and everything else is earned`() {
        assertEquals(listOf("Wait in the dark for the patrol to open it, and slip through behind them."), door())
        assertEquals(2, door(courage = 7).size)
        assertTrue(door(setOf("earned_voss_seal")).any { it.startsWith("Hold Voss's seal") })
        assertFalse("a seal spent at the arch opens nothing here", door(setOf("earned_voss_seal", "c8_seal_spent")).any { it.startsWith("Hold Voss's seal") })
        assertTrue(door(setOf("c6_has_ward_nails")).any { it.startsWith("Drive one of Kestrel's ward-nails") })
        assertTrue(door(setOf("c9_knows_cells"), courage = 5).any { it.contains("where the plans said") })
        assertFalse(door(setOf("c9_knows_cells"), courage = 4).any { it.contains("where the plans said") })
        assertTrue(door(setOf("c9_gate_braced"), courage = 5).any { it.contains("showed you on the gate") })
        assertFalse(door(setOf("c9_gate_braced", "c9_knows_cells"), courage = 5).any { it.contains("showed you on the gate") })
        assertFalse(door(setOf("c9_gate_braced"), courage = 7).any { it == "Break the door." })
        assertEquals(listOf("Break the door."), door(courage = 7).filter { it.startsWith("Break") })
    }

    @Test
    fun `forcing the ward costs more the less you know, and every other way in leaves it quiet`() {
        val s = at("c9_the_door", health = 300, courage = 99, flags = setOf("earned_voss_seal", "c6_has_ward_nails"))
        assertEquals(300 - 15 + 1, takeLabel(s, "Break the door.").health)
        assertEquals(300 - 5 + 1, takeLabel(s.copy(flags = s.flags + "c9_knows_cells"), "Break the ward at its seat, where the plans said it was weakest.").health)
        assertEquals(300 - 5 + 1, takeLabel(s.copy(flags = s.flags + "c9_gate_braced"), "Break the ward at its seat, the way Halvard showed you on the gate.").health)
        assertEquals(300 + 1, takeLabel(s, "Hold Voss's seal to the ward. It was made for doors like this.").health)
        assertEquals(300 + 1, takeLabel(s, "Drive one of Kestrel's ward-nails into the ward's seat.").health)
        assertEquals(300 - 5 + 1, takeLabel(s, "Wait in the dark for the patrol to open it, and slip through behind them.").health)
        val flagged = mapOf(
            "c9_door_seal" to "Hold Voss's seal to the ward. It was made for doors like this.",
            "c9_door_nails" to "Drive one of Kestrel's ward-nails into the ward's seat.",
            "c9_door_force" to "Break the door."
        )
        flagged.forEach { (flag, label) ->
            val after = takeLabel(s.copy(flags = s.flags + C9_DOOR_FAMILY), label)
            assertEquals(setOf(flag), after.flags.intersect(C9_DOOR_FAMILY))
        }
        val waited = takeLabel(s.copy(flags = s.flags + "c9_door_seal"), "Wait in the dark for the patrol to open it, and slip through behind them.")
        assertTrue("waiting sets no flag and clears the last telling's", waited.flags.intersect(C9_DOOR_FAMILY).isEmpty())
        listOf("c9_door_seal", "c9_door_nails", "c9_door_force", "c9_door_waited").forEach {
            assertEquals(listOf("voss_in_chains"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The chains, Ordrun, and the way home
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `trusting Voss to free herself raises where she stands, and freeing her leaves it`() {
        val breakIt = "Break the shackles yourself."
        val watch = "Keep watch. Let her work the lock herself."
        fun bladeFor(rung: String) = if (rung == "c7_rung_trusted") "c9_blade_trusted" else "c9_blade_guarded"
        listOf(
            emptySet<String>() to ("c7_rung_wary" to "c7_rung_working"),
            setOf("c7_rung_wary") to ("c7_rung_wary" to "c7_rung_working"),
            setOf("c7_rung_working") to ("c7_rung_working" to "c7_rung_trusted"),
            setOf("c7_rung_trusted") to ("c7_rung_trusted" to "c7_rung_trusted")
        ).forEach { (start, expected) ->
            val s = at("voss_in_chains", flags = start)
            assertEquals(1, visible(s).count { it.label == breakIt })
            assertEquals(1, visible(s).count { it.label == watch })
            val broke = takeLabel(s, breakIt)
            assertEquals(setOf(expected.first), broke.flags.intersect(C7_RUNG_FAMILY))
            assertEquals(setOf("broke_her_free"), broke.flags.intersect(C9_CHAINS_FAMILY))
            assertEquals("the blade comes before the fight, matched to her standing", bladeFor(expected.first), broke.sceneId)
            val trusted = takeLabel(s, watch)
            assertEquals(setOf(expected.second), trusted.flags.intersect(C7_RUNG_FAMILY))
            assertEquals(setOf("trusted_her_to_free_herself"), trusted.flags.intersect(C9_CHAINS_FAMILY))
            assertEquals(bladeFor(expected.second), trusted.sceneId)
        }
        assertEquals(setOf("trusted_her_to_free_herself"),
            takeLabel(at("voss_in_chains", flags = C9_CHAINS_FAMILY), watch).flags.intersect(C9_CHAINS_FAMILY))
    }

    @Test
    fun `Halvard's blade is laid beside Voss in the cell, before the chains come off and long before Ordrun`() {
        // the blade scene leads to the shipped scene that matches how she was freed, and a save with neither flag still has a way on
        listOf("c9_blade_trusted", "c9_blade_guarded").forEach { id ->
            assertEquals(listOf("the_right_hand_broke_free"), visible(at(id)).map { it.nextNodeId })
            assertEquals(listOf("the_right_hand_broke_free"), visible(at(id, flags = setOf("broke_her_free"))).map { it.nextNodeId })
            assertEquals(listOf("the_right_hand_trusted"),
                visible(at(id, flags = setOf("trusted_her_to_free_herself"))).map { it.nextNodeId })
            val text = words(node(id).narrativeText)
            assertTrue("$id lays the blade down before the chain is touched", text.startsWith("Before he touches the chain"))
            assertTrue("$id names Halvard", text.contains("Halvard's"))
            assertEquals("the cell, not the corridor", "voss_in_chains", node(id).illustrationId)
        }
        // a Voss who trusts him answers with a touch; one who does not, with a hold
        assertTrue(words(node("c9_blade_trusted").narrativeText).contains("her chained fingers find the back of his hand"))
        assertTrue(words(node("c9_blade_guarded").narrativeText).contains("within reach of where her hand will fall"))
        // after Ordrun nobody is handed anything: only the chapel, for a Voss who trusts him and a Kaelen who stood at it, and the way out
        listOf("c9_after_known", "c9_after_cold").forEach { id ->
            assertEquals(listOf("c9_run_hard"), visible(at(id)).map { it.nextNodeId })
            assertEquals(listOf("c9_run_hard"), visible(at(id, flags = setOf("c7_rung_working", "c8_chapel_story"))).map { it.nextNodeId })
            assertEquals("the chapel is offered only to someone who stood at it", listOf("c9_run_hard"),
                visible(at(id, flags = setOf("c7_rung_trusted"))).map { it.nextNodeId })
            assertEquals(listOf("c9_voss_chapel", "c9_run_hard"),
                visible(at(id, flags = setOf("c7_rung_trusted", "c8_chapel_story"))).map { it.nextNodeId })
            assertFalse("$id hands over no blade", words(node(id).narrativeText).contains("blade Halvard"))
        }
        assertEquals(listOf("c9_run_hard"), visible(at("c9_voss_chapel")).map { it.nextNodeId })
        // the shipped line that has Voss kill Ordrun "to her blade" follows a scene that has already armed her
        assertTrue(words(node("right_hand_defeat").narrativeText).contains("goes down to her blade"))
    }

    @Test
    fun `Ordrun is the shipped fight, and his last words lead to a scene that knows whether the table was read`() {
        assertEquals("right_hand_encounter", node("the_right_hand_broke_free").combatEncounterId)
        assertEquals("right_hand_encounter", node("the_right_hand_trusted").combatEncounterId)
        val enc = CombatRepository.encounter("right_hand_encounter")
        assertEquals("what_the_castellan_says", enc.victoryNodeId)
        assertEquals("right_hand_defeat", enc.defeatNodeId)
        val won = winOrLose(at("the_right_hand_broke_free"), win = true)
        assertEquals("what_the_castellan_says", won.sceneId)
        val lost = winOrLose(at("the_right_hand_trusted"), win = false)
        assertEquals("right_hand_defeat", lost.sceneId)
        assertEquals(listOf("what_the_castellan_says"), visible(lost).map { it.nextNodeId })
        val known = take(at("what_the_castellan_says", flags = setOf("c9_knows_ordrun")), "c9_after_known")
        assertTrue("ordruns_broken_blade" in known.inventory && "defeated_ordrun" in known.flags)
        val cold = take(at("what_the_castellan_says"), "c9_after_cold")
        assertTrue("ordruns_broken_blade" in cold.inventory && "defeated_ordrun" in cold.flags)
        assertEquals(1, visible(at("what_the_castellan_says", flags = setOf("c9_knows_ordrun"))).size)
    }

    @Test
    fun `the way home reads the night after Voss was taken, and a ward that was forced costs ten more`() {
        listOf("c9_after_known", "c9_after_cold", "c9_voss_chapel").forEach { id ->
            assertEquals(listOf("c9_run_marked"), visible(at(id, flags = setOf("c8_marked_route"))).map { it.nextNodeId })
            assertEquals(listOf("c9_run_farmers"), visible(at(id, flags = setOf("c8_ash_farmers"))).map { it.nextNodeId })
            assertEquals(listOf("c9_run_hard"), visible(at(id, flags = setOf("c8_wrote_halvard"))).map { it.nextNodeId })
            assertEquals(listOf("c9_run_hard"), visible(at(id)).map { it.nextNodeId })
        }
        val marked = at("c9_run_marked", health = 300)
        assertEquals(300 + 1, take(marked, "chapter9_end").health)
        assertEquals(300 - 10 + 1, take(marked.copy(flags = marked.flags + "c9_door_force"), "chapter9_end").health)
        val farmers = at("c9_run_farmers", health = 300)
        assertEquals(300 + 10 + 1, take(farmers, "chapter9_end").health)
        val hard = at("c9_run_hard", health = 300)
        assertEquals(300 - 20 + 1, take(hard, "chapter9_end").health)
        assertEquals(300 - 30 + 1, take(hard.copy(flags = hard.flags + "c9_door_force"), "chapter9_end").health)
        assertEquals(listOf("the_last_two"), visible(at("chapter9_end")).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // The shipped scenes
    // ---------------------------------------------------------------------------------------------

    private val shipped = mapOf(
        "a_debt_remembered_trusted" to "0d3c89c4474f8c4dba7bbeebb7be7c7bc5115a41d224e8ad5f78033682be1afc",
        "a_debt_remembered_guarded" to "5cdb4c03dee66a19a7dea8d01f2e42dd23c53c5af3761cac48173a02b389e397",
        "the_plan_takes_shape" to "234184ceff2cbb5636c3fedb5c11b29a85f8e7466f95203debdacf0fad3beb32",
        "into_the_ash_loud" to "089629f61bea66f9550f07ed0b902b6fcd25ba1722e5827642e0052420027bdd",
        "into_the_ash_quiet" to "a34a2bc5c55fed56527307cb11d934148277dc76c67577d1476b43e2546e58ca",
        "into_the_ash_default" to "f84173bdb7a73f3bd770ae2b069fc2f75c70338270db5a066ba88cc4a0f03a44",
        "voss_in_chains" to "774a1708ea98473dca9f08186ba28f7b308efefe259a010e2e19891b813e3bb9",
        "the_right_hand_broke_free" to "7b2d00df3c9d94078e4e2aca59cc9bbd537aecc75b199ffe29f3f41d35be70eb",
        "the_right_hand_trusted" to "e5f9d06d09dc415607f00eb43a74f189cc3ec2e21f2cb71eb8b3941f2976d127",
        "right_hand_defeat" to "a4639f1c923d9504c877e98b77cc5ba06afcccb281a865e63d45c0901c62a3fa",
        "what_the_castellan_says" to "b5111ff2858bf34caae7691f96152df04488eacd6dc4b954da984c7960bf00d9",
        "chapter9_end" to "82aecec9b06dd6575ec1b552d46641505e3a6dc69398f312f1b3039b701590d5"
    )

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(words(text).toByteArray()).joinToString("") { "%02x".format(it) }

    @Test
    fun `all twelve scenes that shipped in Chapter 9 are word for word as before`() {
        val moved = shipped.filter { (id, hash) -> sha(node(id).narrativeText) != hash }.keys
        assertTrue("Changed scenes: $moved", moved.isEmpty())
        assertEquals(12 + 39, StoryRepository.nodesInChapter("chapter_9").size)
    }

    // ---------------------------------------------------------------------------------------------
    // Old saves, replays, and the whole chapter
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a save parked on any scene Chapter 9 had before it grew plays on to Chapter 10`() {
        val legacy = mapOf(
            "a_debt_remembered_trusted" to setOf("vowed_to_return_now"),
            "a_debt_remembered_guarded" to setOf("vowed_to_return_prepared"),
            "the_plan_takes_shape" to setOf("moved_with_preparation"),
            "into_the_ash_loud" to setOf("created_a_diversion"),
            "into_the_ash_quiet" to setOf("infiltrated_quietly"),
            "into_the_ash_default" to setOf("went_with_what_they_had"),
            "voss_in_chains" to setOf("went_with_what_they_had"),
            "the_right_hand_broke_free" to setOf("broke_her_free"),
            "the_right_hand_trusted" to setOf("trusted_her_to_free_herself"),
            "right_hand_defeat" to setOf("lost_to_cinder_castellan"),
            "what_the_castellan_says" to setOf("lost_to_cinder_castellan"),
            "chapter9_end" to setOf("defeated_ordrun")
        )
        legacy.forEach { (id, flags) ->
            listOf(true, false).forEach { strong ->
                val start = at(id, flags = flags, courage = if (strong) 99 else 1, honour = if (strong) 99 else 0, health = 300)
                val end = ReplayKit.play(start, stop = { isGoal(it) })
                assertTrue("$id strong=$strong ended at ${end.sceneId}", isGoal(end))
            }
        }
    }

    @Test
    fun `a second telling that decides differently carries nothing from the first`() {
        val rich = setOf(
            "c7_rung_working", "earned_voss_seal", "c6_has_ward_nails", "c8_wrote_halvard", "c8_voss_cells", "c8_rotation",
            "c8_tithe_seen", "c8_chapel_story", "c7_hold_terms", "c7_halvard_trusts_voss", "vowed_to_return_prepared"
        )
        val first = ReplayKit.play(
            at("a_debt_remembered_guarded", flags = rich),
            stop = { isGoal(it) },
            pick = picker(
                "a_debt_remembered_guarded" to "properly",
                "c9_the_table" to "->c9_plans",
                "c9_plans" to "->c9_plans_letter",
                "c9_the_table_two" to "->c9_north_stair",
                "c9_north_stair" to "->c9_stair_people",
                "the_plan_takes_shape" to "east stair",
                "c9_the_door" to "Hold Voss's seal",
                "voss_in_chains" to "Keep watch"
            )
        )
        assertTrue(first.flags.containsAll(setOf("c9_knows_ordrun", "c9_knows_cells", "c9_stair_people", "c9_way_stair", "c9_door_seal",
            "trusted_her_to_free_herself", "moved_with_preparation", "defeated_ordrun")))
        assertTrue(first.flags.intersect(HUB_MARKERS).isEmpty())
        val second = ReplayKit.play(
            first.copy(sceneId = "a_debt_remembered_guarded", chapterId = "chapter_9", flags = first.flags - "vowed_to_return_prepared" + "undecided_on_voss"),
            stop = { isGoal(it) },
            pick = picker(
                "a_debt_remembered_guarded" to "time to decide",
                "c9_the_table" to "->c9_stores",
                "c9_stores" to "rope, hooks",
                "the_plan_takes_shape" to "Halvard's rope",
                "c9_the_door" to "Wait in the dark",
                "voss_in_chains" to "Break the shackles"
            )
        )
        val gone = setOf("c9_knows_ordrun", "c9_knows_cells", "c9_stair_people", "c9_way_stair", "c9_door_seal",
            "trusted_her_to_free_herself", "moved_with_preparation")
        assertEquals("first-telling flags survive: ${second.flags intersect gone}", emptySet<String>(), second.flags intersect gone)
        assertTrue(second.flags.containsAll(setOf("c9_kit", "c9_way_wall", "broke_her_free", "decided_at_last", "defeated_ordrun")))
        assertTrue(second.flags.intersect(C9_DOOR_FAMILY).isEmpty())
        assertTrue(second.flags.intersect(HUB_MARKERS).isEmpty())
    }

    private fun randomStart(random: java.util.Random): GameState {
        val flags = HashSet<String>()
        flags += listOf("vowed_to_return_now", "vowed_to_return_prepared", "undecided_on_voss")[random.nextInt(3)]
        flags += rungOrder[random.nextInt(3)]
        if (random.nextBoolean()) flags += "earned_voss_seal"
        if (random.nextBoolean()) flags += "c6_has_ward_nails"
        if (random.nextBoolean()) flags += "c8_voss_cells"
        if (random.nextBoolean()) flags += "c8_rotation"
        if (random.nextBoolean()) flags += "c8_chapel_story"
        when (random.nextInt(3)) { 0 -> flags += "c8_exit_ramp"; 1 -> { flags += "c8_exit_tithe"; flags += "c8_seal_spent"; flags -= "earned_voss_seal" }; else -> {} }
        when (random.nextInt(4)) {
            0 -> flags += "c8_wrote_halvard"; 1 -> flags += "c8_marked_route"
            2 -> { flags += "c8_ash_farmers"; flags += "c8_tithe_seen" }; else -> flags += "c8_cart_robbed"
        }
        if ("c8_cart_robbed" !in flags && random.nextBoolean()) flags += "c8_tithe_seen"
        flags += listOf("c7_ground_yard", "c7_ground_stair", "c7_ground_road")[random.nextInt(3)]
        when (random.nextInt(4)) { 0 -> flags += "c7_hold_terms"; 1 -> flags += "c7_hold_refused"; 2 -> flags += "c7_hold_hidden"; else -> {} }
        when (random.nextInt(3)) { 0 -> flags += "c7_halvard_trusts_voss"; 1 -> flags += "c7_halvard_doubts_voss"; else -> {} }
        return at(
            "a_debt_remembered_guarded", flags = flags, courage = 1 + random.nextInt(10), honour = random.nextInt(9),
            gold = random.nextInt(90), health = 200 + random.nextInt(300)
        )
    }

    @Test
    fun `fifteen hundred varied tellings reach Chapter 10 with a consistent set of flags and a borrowed blade`() {
        val random = java.util.Random(0x9009L)
        repeat(1500) { run ->
            val start = randomStart(random)
            var s = start
            val visited = ArrayList<String>()
            var steps = 0
            while (!isGoal(s)) {
                check(steps++ < 160) { "run $run never finished; stuck at ${s.sceneId}" }
                val n = node(s.sceneId)
                visited += n.id
                s = if (n.combatEncounterId != null) {
                    winOrLose(s, win = random.nextBoolean())
                } else {
                    val choices = visible(s)
                    check(choices.isNotEmpty()) { "run $run dead end at ${n.id} with ${s.flags}" }
                    s.applyChoice(choices[random.nextInt(choices.size)])
                }
            }
            val f = s.flags
            assertEquals("run $run way", 1, f.count { it in C9_WAY_FAMILY })
            assertTrue("run $run door", f.count { it in C9_DOOR_FAMILY } <= 1)
            assertTrue("run $run days", f.count { it in C9_DAYS_FAMILY } <= 1)
            assertEquals("run $run chains", 1, f.count { it in C9_CHAINS_FAMILY })
            assertEquals("run $run vow", 1, f.count { it in C9_VOW_ACK_FAMILY })
            assertEquals("run $run rung", 1, f.count { it in C7_RUNG_FAMILY })
            assertEquals("run $run markers", emptySet<String>(), f.intersect(HUB_MARKERS))
            if ("c9_way_stair" in f) assertTrue("run $run stair", "c8_voss_cells" in f && "decided_at_last" !in f)
            if ("c9_way_wall" in f) assertTrue("run $run wall", "c8_rotation" in f)
            if ("c9_way_cart" in f) assertTrue("run $run cart", "c8_ash_farmers" in f || ("c8_tithe_seen" in f && "c9_stair_people" in f))
            if ("c9_door_seal" in f) assertTrue("run $run seal", "earned_voss_seal" in f && "c8_seal_spent" !in f)
            if ("c9_door_nails" in f) assertTrue("run $run nails", "c6_has_ward_nails" in f)
            if ("c9_door_force" in f) assertTrue("run $run force", "c9_door_seal" !in f && "c9_door_nails" !in f)
            if ("c9_stair_people" in f) assertTrue("run $run people", "c7_hold_terms" in f)
            if ("c9_gate_braced" in f) assertTrue("run $run gate", "c7_hold_terms" !in f)
            assertEquals("run $run known", "c9_knows_ordrun" in f, "c9_after_known" in visited)
            assertTrue("run $run blade", "c9_blade_trusted" in visited || "c9_blade_guarded" in visited)
            val bladeAt = visited.indexOfFirst { it == "c9_blade_trusted" || it == "c9_blade_guarded" }
            val fightAt = visited.indexOfFirst { it == "the_right_hand_broke_free" || it == "the_right_hand_trusted" }
            val afterAt = visited.indexOfFirst { it == "c9_after_known" || it == "c9_after_cold" }
            assertTrue("run $run: the blade ($bladeAt) comes before the fight ($fightAt), the aftermath ($afterAt) after it",
                bladeAt in 0 until fightAt && fightAt < afterAt)
            assertEquals("run $run: one blade scene", 1, visited.count { it == "c9_blade_trusted" || it == "c9_blade_guarded" })
            assertTrue("run $run castellan blade", "ordruns_broken_blade" in s.inventory)
            val granted = (if ("c9_stores_supplies" in visited) (if ("c7_ground_stair" in start.flags) 2 else 1) else 0) +
                (if ("c9_stair_rules" in visited) 1 else 0)
            assertEquals("run $run draughts", granted, draughts(s))
            assertTrue("run $run gold", s.gold >= 0)
            assertTrue("run $run health", s.health >= 0)
            val twoSpots = "hub_second" !in start.flags && "vowed_to_return_prepared" in start.flags
            val seen = visited.count { it in spots }
            assertEquals("run $run spots", if (twoSpots) 2 else 1, seen)
        }
    }

    @Test
    fun `no scene of the chapter leads anywhere that does not exist`() {
        val ids = StoryRepository.nodesInChapter("chapter_9").map { it.id }.toSet()
        StoryRepository.nodesInChapter("chapter_9").forEach { n ->
            n.choices.forEach { c ->
                assertTrue("${n.id} -> ${c.nextNodeId}", c.nextNodeId in ids || c.nextNodeId == "the_last_two")
            }
        }
        assertNull(node("chapter9_end").combatEncounterId)
    }
}
