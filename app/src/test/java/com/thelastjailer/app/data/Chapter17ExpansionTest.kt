package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * The Chapter 17 expansion, B1 and B2: Stonebeard before the road, and Ashfall after it. A chain scene at the gate, one scene for the
 * policy Kaelen set in Chapter XIII, one for the door he told Edda about (or none, for a player who promised her nothing), and a
 * preparation hub of two tables: Stores, Watch and Records, of which Kaelen may visit none, one or two, never the same one twice.
 * Five hidden markers record what he did there. Then the shipped road, one scene for where Voss stands (trusted, working, or wary),
 * the approach, and the preparation payoffs: the stores, then the records, then the watch, only the ones that apply, each exactly
 * once, or one passage for a player who visited nothing. The final "Enter the chamber." choice clears the five markers and leads into
 * the shipped guardian. The guardian, the frame and the way on to Chapter XVIII are the shipped ones.
 *
 * The story is proved by walking it, not by reading it. [explore] takes every choice that is open in every state from the chain gate
 * to the guardian, for every policy, every door and every rung, and with or without markers left over from an earlier telling, and
 * [problemsOf] collects whatever goes wrong: a loop, a dead end, a third site, a site offered twice, a marker that is lost before the
 * payoffs read it or survives the final choice, a scene that shows the wrong number of choices, a state or a payoff the plan does not
 * list, a choice that can never be taken. It is a plain function of the scenes, so the negative controls run it on copies of the real
 * scenes with one thing broken each. [expectedPayoffs] is written by hand from the package's conditions, not computed from the scenes.
 */
class Chapter17ExpansionTest {

    private val prep = C17_PREPARATION_FLAGS
    private val stores = "c17_visited_stores"
    private val watch = "c17_visited_watch"
    private val records = "c17_visited_records"
    private val took = "c17_took_guard"
    private val studied = "c17_studied_junctions"
    private val road = "the_road_to_ashfall_again"
    private val chamber = "the_chamber_defended"

    private val policy = listOf("began_training_new_wardens", "stood_watch_rather_than_delegate", "deferred_the_question")
    private val door = listOf("c13_door_open", "c13_door_barred")
    private val rungs = listOf(setOf("c7_rung_trusted"), setOf("c7_rung_working"), setOf("c7_rung_wary"), emptySet())

    private val chainScenes = setOf("c17_chain_gate", "c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred")
    private val hubScenes = setOf(
        "c17_prepare", "c17_prepare_second", "c17_stores", "c17_watch_site", "c17_watch_take", "c17_watch_leave", "c17_records",
        "c17_records_study", "c17_records_interrupt"
    )
    private val rungScenes = setOf("c17_road_trusted", "c17_road_working", "c17_road_wary")
    private val payoffScenes = setOf("c17_pay_stores", "c17_pay_arrangement", "c17_pay_interruption", "c17_pay_taken", "c17_pay_left", "c17_pay_none")
    private val newScenes = chainScenes + hubScenes + rungScenes + "c17_approach" + payoffScenes
    private val siteScenes = setOf("c17_stores", "c17_watch_site", "c17_records")
    private val siteExits = setOf("c17_stores", "c17_watch_take", "c17_watch_leave", "c17_records_study", "c17_records_interrupt")
    private val siteFlag = mapOf("c17_stores" to stores, "c17_watch_site" to watch, "c17_records" to records)

    /** How many choices each scene shows in every state it can be reached in. */
    private val expectedVisible = mapOf(
        "c17_chain_gate" to 1, "c17_wardens" to 1, "c17_watch" to 1, "c17_council" to 1, "c17_door_open" to 1, "c17_door_barred" to 1,
        "c17_prepare" to 4, "c17_prepare_second" to 3, "c17_stores" to 1, "c17_watch_site" to 2, "c17_watch_take" to 1,
        "c17_watch_leave" to 1, "c17_records" to 2, "c17_records_study" to 1, "c17_records_interrupt" to 1,
        road to 1, "c17_road_trusted" to 1, "c17_road_working" to 1, "c17_road_wary" to 1, "c17_approach" to 1,
        "c17_pay_stores" to 1, "c17_pay_arrangement" to 1, "c17_pay_interruption" to 1, "c17_pay_taken" to 1, "c17_pay_left" to 1, "c17_pay_none" to 1
    )

    /**
     * Every state the markers can hold when Kaelen leaves the hub (none, one or two sites; the guard's fate and the records' study count
     * as part of the visit), with the payoffs the package says each shows: the stores, then the records (the arrangement, or the
     * junctions if studied), then the watch (the guard taken, or left); the no-preparation passage only for no visit. Fourteen states.
     */
    private val expectedPayoffs: Map<Set<String>, List<String>> = mapOf(
        emptySet<String>() to listOf("c17_pay_none"),
        setOf(stores) to listOf("c17_pay_stores"),
        setOf(watch) to listOf("c17_pay_left"),
        setOf(watch, took) to listOf("c17_pay_taken"),
        setOf(records) to listOf("c17_pay_arrangement"),
        setOf(records, studied) to listOf("c17_pay_interruption"),
        setOf(stores, watch) to listOf("c17_pay_stores", "c17_pay_left"),
        setOf(stores, watch, took) to listOf("c17_pay_stores", "c17_pay_taken"),
        setOf(stores, records) to listOf("c17_pay_stores", "c17_pay_arrangement"),
        setOf(stores, records, studied) to listOf("c17_pay_stores", "c17_pay_interruption"),
        setOf(watch, records) to listOf("c17_pay_arrangement", "c17_pay_left"),
        setOf(watch, took, records) to listOf("c17_pay_arrangement", "c17_pay_taken"),
        setOf(watch, records, studied) to listOf("c17_pay_interruption", "c17_pay_left"),
        setOf(watch, took, records, studied) to listOf("c17_pay_interruption", "c17_pay_taken")
    )

    private val real: Map<String, StoryNode> = StoryRepository.nodesInChapter("chapter_17").associateBy { it.id }
    private val allNodes: List<StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val graph = FlagFamilyProbe.StoryGraph(
        allNodes,
        combatTargets = { n ->
            n.combatEncounterId?.let { CombatRepository.encounter(it) }?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId ?: it.victoryNodeId) }
                ?: emptyList()
        }
    )

    // ---- walking the story ----

    /**
     * One way from the chain gate to the guardian: the scenes passed (the last holds the final choice), the markers held on arriving
     * at each, the label of the choice taken at each, and the markers held after the final choice.
     */
    private class Exit(val path: List<String>, val trail: List<Set<String>>, val labels: List<String>, val after: Set<String>)

    private class Report(
        val start: GameState, val exits: List<Exit>, val taken: Set<String>, val counts: Map<String, Set<Int>>, val problems: List<String>
    )

    private fun five(s: GameState): Set<String> = s.flags.intersect(prep)

    private fun openChoices(n: StoryNode, s: GameState) = n.choices.withIndex().filter { (_, c) -> c.requirements?.isSatisfiedBy(s) ?: true }

    /** Every way from [start] to the guardian through [nodes], taking every choice that is open in every state. */
    private fun explore(nodes: Map<String, StoryNode>, start: GameState): Report {
        val exits = ArrayList<Exit>()
        val taken = HashSet<String>()
        val problems = ArrayList<String>()
        val counts = HashMap<String, MutableSet<Int>>()
        fun go(s: GameState, path: List<String>, trail: List<Set<String>>, labels: List<String>) {
            val n = nodes[s.sceneId] ?: run { problems += "no scene ${s.sceneId} after $path"; return }
            if (n.id in path) { problems += "loop: ${(path + n.id).joinToString(" > ")}"; return }
            if (n.id == "c17_prepare" && five(s).isNotEmpty()) problems += "markers held on entering the hub: ${five(s)}"
            val here = openChoices(n, s)
            counts.getOrPut(n.id) { HashSet() } += here.size
            if (here.isEmpty()) { problems += "dead end at ${n.id} holding ${five(s)}"; return }
            if ((path + n.id).count { it in siteScenes } > 2) problems += "a third site: ${(path + n.id).joinToString(" > ")}"
            here.forEach { (i, c) ->
                taken += "${n.id}#$i"
                val flag = siteFlag[c.nextNodeId]
                if (flag != null && flag in s.flags) problems += "${n.id} offers ${c.nextNodeId} again"
                val next = s.applyChoice(c)
                if (c.nextNodeId == chamber) exits += Exit(path + n.id, trail + listOf(five(s)), labels + c.label, five(next))
                else go(next, path + n.id, trail + listOf(five(s)), labels + c.label)
            }
        }
        go(start, emptyList(), emptyList(), emptyList())
        return Report(start, exits, taken, counts, problems)
    }

    /** A player at the chain gate for every policy (or none), door (or none) and rung (or none), with or without markers left from before. */
    private fun chainStarts(stale: Boolean): List<GameState> =
        (policy.map { setOf(it) } + listOf(emptySet<String>())).flatMap { p ->
            (door.map { setOf(it) } + listOf(emptySet<String>())).flatMap { d ->
                rungs.map { r ->
                    GameState(chapterId = "chapter_17", sceneId = "c17_chain_gate", flags = p + d + r + (if (stale) prep else emptySet()))
                }
            }
        }

    private fun expectedRung(start: GameState) = when {
        "c7_rung_trusted" in start.flags -> "c17_road_trusted"
        "c7_rung_working" in start.flags -> "c17_road_working"
        else -> "c17_road_wary"
    }

    private fun problemsOf(nodes: Map<String, StoryNode>): List<String> {
        val out = ArrayList<String>()
        val reports = (chainStarts(false) + chainStarts(true)).map { explore(nodes, it) }
        reports.forEach { out += it.problems }
        val counts = reports.flatMap { it.counts.entries }.groupBy({ it.key }, { it.value }).mapValues { (_, v) -> v.flatten().toSet() }
        expectedVisible.forEach { (id, n) -> if (counts[id] != setOf(n)) out += "$id shows ${counts[id]} choices, expected $n" }
        val hubEnds = HashSet<Set<String>>()
        reports.forEach { r ->
            if (r.exits.size != 22) out += "${r.exits.size} ways through the hub, expected 22"
            r.exits.forEach { e ->
                val at = e.path.indexOf(road)
                if (at < 0) { out += "the chamber is entered without the road: ${e.path.joinToString(" > ")}"; return@forEach }
                val hubPath = e.path.take(at)
                val hubEnd = e.trail[at - 1]
                hubEnds += hubEnd
                // the way out of the hub: Depart with no or one site, Set out with two
                val sites = hubPath.count { it in siteScenes }
                val label = e.labels[at - 1]
                val fine = if (sites == 2) label == "Set out for Ashfall." && hubPath.last() in siteExits
                else label == "Depart for Ashfall." && hubPath.last() in setOf("c17_prepare", "c17_prepare_second")
                if (!fine) out += "after $sites sites the way out is '$label' from ${hubPath.last()}"
                // what the hub recorded, and that nothing loses it before the payoffs have read it
                val plan = hubPath.flatMap { markersOf(it) }.toSet()
                if (hubEnd != plan) out += "markers held after ${hubPath.joinToString(" > ")}: $hubEnd, the plan says $plan"
                val lost = e.trail.drop(at).firstOrNull { it != hubEnd }
                if (lost != null) out += "markers lost between the hub and the last payoff in ${e.path.joinToString(" > ")}: $hubEnd became $lost"
                // the final choice
                if (e.after.isNotEmpty()) out += "markers survive the final choice from ${e.path.last()}: ${e.after}"
                if (e.labels.last() != "Enter the chamber.") out += "the final choice from ${e.path.last()} is '${e.labels.last()}'"
                // Voss, the approach, then the payoffs in the planned order, each once
                val after = e.path.drop(at + 1)
                if (after.firstOrNull() != expectedRung(r.start)) out += "after the road comes ${after.firstOrNull()}, expected ${expectedRung(r.start)}"
                if (after.getOrNull(1) != "c17_approach") out += "the approach does not follow ${after.firstOrNull()}"
                val payoffs = after.drop(2)
                val wanted = expectedPayoffs[hubEnd] ?: emptyList()
                if (payoffs != wanted) out += "payoffs after $hubEnd are $payoffs, the plan says $wanted"
                if (payoffs.toSet().size != payoffs.size) out += "a payoff repeats: $payoffs"
            }
        }
        if (hubEnds != expectedPayoffs.keys) out += "end states differ: missing ${expectedPayoffs.keys - hubEnds}, unexpected ${hubEnds - expectedPayoffs.keys}"
        val taken = reports.flatMap { it.taken }.toSet()
        nodes.filterKeys { it in newScenes || it == road }.forEach { (id, n) -> n.choices.indices.forEach { if ("$id#$it" !in taken) out += "$id choice $it can never be taken" } }
        return out.distinct()
    }

    /** What visiting a scene means for the markers: the site, and for the watch and the records the answer given there. */
    private fun markersOf(scene: String): Set<String> = when (scene) {
        "c17_stores" -> setOf(stores)
        "c17_watch_take" -> setOf(watch, took)
        "c17_watch_leave" -> setOf(watch)
        "c17_records_study" -> setOf(records)
        "c17_records_interrupt" -> setOf(records, studied)
        else -> emptySet()
    }

    private fun labels(s: GameState): List<String> = StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).map { it.label }

    private fun GameState.pick(label: String): GameState {
        val options = StoryRepository.visibleChoices(StoryRepository.node(sceneId), this).filter { it.label == label }
        assertEquals("$sceneId shows '$label' ${options.size} times", 1, options.size)
        return applyChoice(options.single())
    }

    private fun at(scene: String, vararg flags: String) = GameState(chapterId = "chapter_17", sceneId = scene, flags = flags.toSet())

    /** Takes the one choice on offer, scene after scene, until [target]; returns where it ended and every scene it passed. */
    private fun walkTo(from: GameState, target: String): Pair<GameState, List<String>> {
        var s = from
        val seen = arrayListOf(s.sceneId)
        while (s.sceneId != target) {
            val offered = labels(s)
            assertEquals("one way on from ${s.sceneId} holding ${five(s)}", 1, offered.size)
            s = s.pick(offered.single())
            seen += s.sceneId
            assertTrue("did not reach $target: $seen", seen.size < 40)
        }
        return s to seen
    }

    // ---- the plan, proved on the real scenes ----

    @Test
    fun `every way through the chain, the hub, the road, Voss, the approach and the payoffs is clean`() {
        val problems = problemsOf(real)
        assertEquals("problems walking the story from the chain gate to the guardian: $problems", emptyList<String>(), problems)
        val one = explore(real, chainStarts(false).first())
        println("WALK ${one.exits.size} ways through the hub; end states ${one.exits.map { it.trail[it.path.indexOf(road) - 1] }.toSet().size}; scenes ${newScenes.size}; choices ${newScenes.sumOf { real.getValue(it).choices.size }} (+${real.getValue(road).choices.size} on the road); starts ${chainStarts(false).size} fresh and ${chainStarts(true).size} replayed")
    }

    @Test
    fun `the chain reads the policy and then the door, and shows one way on at every step`() {
        val policyScene = mapOf<String?, String>(
            "began_training_new_wardens" to "c17_wardens", "stood_watch_rather_than_delegate" to "c17_watch",
            "deferred_the_question" to "c17_council", null to "c17_council"
        )
        val doorScene = mapOf<String?, String?>("c13_door_open" to "c17_door_open", "c13_door_barred" to "c17_door_barred", null to null)
        (policy + listOf(null)).forEach { p ->
            (door + listOf(null)).forEach { d ->
                var s = at("c17_chain_gate", *listOfNotNull(p, d).toTypedArray())
                val seen = mutableListOf(s.sceneId)
                while (s.sceneId != "c17_prepare") {
                    assertEquals("one way on from ${s.sceneId} for $p / $d", listOf("Continue."), labels(s))
                    s = s.applyChoice(StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).single())
                    seen += s.sceneId
                }
                assertEquals("the chain for $p / $d", listOfNotNull("c17_chain_gate", policyScene[p], doorScene[d], "c17_prepare"), seen)
            }
        }
    }

    @Test
    fun `the first table lists the three sites and the way out, and the way out is always there`() {
        val s = at("c17_prepare")
        assertEquals(listOf("Visit the stores.", "Speak to the watch captain.", "Study the old records.", "Depart for Ashfall."), labels(s))
        val left = s.pick("Depart for Ashfall.")
        assertEquals(road, left.sceneId)
        assertEquals("no visit is a valid way through", emptySet<String>(), five(left))
    }

    @Test
    fun `after one site the second table offers only the two unvisited sites and the way out`() {
        val second = mapOf(
            "Visit the stores." to listOf("Speak to the watch captain.", "Study the old records.", "Depart for Ashfall."),
            "Speak to the watch captain." to listOf("Visit the stores.", "Study the old records.", "Depart for Ashfall."),
            "Study the old records." to listOf("Visit the stores.", "Speak to the watch captain.", "Depart for Ashfall.")
        )
        second.forEach { (firstSite, expected) ->
            var s = at("c17_prepare").pick(firstSite)
            // each site has its own decision, if any, before it hands back to the table
            s = when (s.sceneId) {
                "c17_watch_site" -> s.pick("Leave every defender at Stonebeard.")
                "c17_records" -> s.pick("Study the six-position arrangement.")
                else -> s
            }
            assertEquals("after $firstSite", listOf("Continue."), labels(s))
            s = s.pick("Continue.")
            assertEquals("the second table after $firstSite", "c17_prepare_second", s.sceneId)
            assertEquals("the second table after $firstSite", expected, labels(s))
            assertEquals("depart after one site", road, s.pick("Depart for Ashfall.").sceneId)
        }
    }

    @Test
    fun `after a second site there is no table and no third site, only one Set out for Ashfall, and the markers stay set`() {
        val orders = listOf(
            listOf("Visit the stores.", "Speak to the watch captain."), listOf("Visit the stores.", "Study the old records."),
            listOf("Speak to the watch captain.", "Visit the stores."), listOf("Speak to the watch captain.", "Study the old records."),
            listOf("Study the old records.", "Visit the stores."), listOf("Study the old records.", "Speak to the watch captain.")
        )
        orders.forEach { (a, b) ->
            fun decide(s: GameState) = when (s.sceneId) {
                "c17_watch_site" -> s.pick("Take one guard to watch the approach.")
                "c17_records" -> s.pick("Look for ways to interrupt the connections.")
                else -> s
            }
            var s = decide(at("c17_prepare").pick(a)).pick("Continue.")
            s = decide(s.pick(b))
            assertEquals("after $a then $b the only way on", listOf("Set out for Ashfall."), labels(s))
            assertTrue("no table after the second site", s.sceneId in siteExits)
            val out = s.pick("Set out for Ashfall.")
            assertEquals(road, out.sceneId)
            assertEquals("the markers stay set on the way out of the hub, for the payoffs to read", five(s), five(out))
            assertTrue("two sites leave at least two markers", five(out).size >= 2)
        }
    }

    @Test
    fun `the decisions set exactly the markers the plan says`() {
        val afterWatch = at("c17_prepare").pick("Speak to the watch captain.")
        assertEquals(setOf(watch), five(afterWatch))
        assertEquals("taking a guard", setOf(watch, took), five(afterWatch.pick("Take one guard to watch the approach.")))
        assertEquals("leaving every defender", setOf(watch), five(afterWatch.pick("Leave every defender at Stonebeard.")))
        val afterRecords = at("c17_prepare").pick("Study the old records.")
        assertEquals(setOf(records), five(afterRecords))
        assertEquals("studying the arrangement", setOf(records), five(afterRecords.pick("Study the six-position arrangement.")))
        assertEquals("interrupting the connections", setOf(records, studied), five(afterRecords.pick("Look for ways to interrupt the connections.")))
        assertEquals("the stores", setOf(stores), five(at("c17_prepare").pick("Visit the stores.")))
    }

    // ---- Voss: exactly one scene for every rung ----

    @Test
    fun `exactly one Voss scene shows for every rung a save can hold, including none, and it leaves the markers alone`() {
        val byRung = mapOf(
            setOf("c7_rung_trusted") to "c17_road_trusted", setOf("c7_rung_working") to "c17_road_working",
            setOf("c7_rung_wary") to "c17_road_wary", emptySet<String>() to "c17_road_wary"
        )
        byRung.forEach { (rung, scene) ->
            listOf(emptySet(), setOf(stores, watch, took), setOf(records, studied)).forEach { held ->
                val s = at(road, *(rung + held).toTypedArray())
                assertEquals("one way on from the road for $rung", listOf("Get to the chamber."), labels(s))
                val voss = s.pick("Get to the chamber.")
                assertEquals("the Voss scene for $rung", scene, voss.sceneId)
                assertEquals("the Voss scene leaves the markers alone", held, five(voss))
                assertEquals(listOf("Continue."), labels(voss))
                val approach = voss.pick("Continue.")
                assertEquals("c17_approach", approach.sceneId)
                assertEquals(held, five(approach))
            }
        }
    }

    @Test
    fun `the road keeps its label and its prose, has no effect and no stat gate, and leads to Voss by exactly one gated copy`() {
        val r = real.getValue(road)
        assertEquals(3, r.choices.size)
        r.choices.forEach {
            assertEquals("Get to the chamber.", it.label)
            assertEquals(Consequences(), it.consequences)
            assertTrue(it.requirements?.minStats.isNullOrEmpty())
        }
        assertEquals(
            listOf("c17_road_trusted", "c17_road_working", "c17_road_wary"), r.choices.map { it.nextNodeId }
        )
        assertEquals(setOf("c7_rung_trusted"), r.choices[0].requirements?.requiredFlags)
        assertEquals(setOf("c7_rung_working"), r.choices[1].requirements?.requiredFlags)
        assertEquals(setOf("c7_rung_working", "c7_rung_trusted"), r.choices[2].requirements?.forbiddenFlags)
        assertTrue(r.choices[2].requirements?.requiredFlags.isNullOrEmpty())
        // the shipped prose, title and art are pinned word for word by Chapter17ShippedPinTest; the pin also accepts exactly this shape
        assertEquals("The Road to Ashfall, Again", r.title)
    }

    // ---- the payoffs: order, uniqueness and omission ----

    @Test
    fun `from the approach each of the fourteen states shows exactly its payoffs, once each, in the order stores, records, watch`() {
        expectedPayoffs.forEach { (state, wanted) ->
            val start = at("c17_approach", *state.toTypedArray())
            assertEquals("the approach shows one way on holding $state", listOf("Go on."), labels(start))
            val (end, seen) = walkTo(start, chamber)
            assertEquals("the payoffs shown holding $state", wanted, seen.filter { it in payoffScenes })
            assertEquals("every scene between is the approach or a payoff", listOf("c17_approach") + wanted + chamber, seen)
            assertEquals("the final choice clears all five", emptySet<String>(), five(end))
        }
    }

    @Test
    fun `no preparation shows only the no-preparation passage, one site shows one payoff, two sites show two`() {
        val byVisits = expectedPayoffs.entries.groupBy { e -> e.key.intersect(setOf(stores, watch, records)).size }
        assertEquals(setOf(0, 1, 2), byVisits.keys)
        assertEquals(listOf(listOf("c17_pay_none")), byVisits.getValue(0).map { it.value })
        byVisits.getValue(1).forEach { assertEquals(1, it.value.size); assertFalse("c17_pay_none" in it.value) }
        byVisits.getValue(2).forEach { assertEquals(2, it.value.size); assertFalse("c17_pay_none" in it.value) }
        // one Watch payoff and one Records payoff at most; the stores first, the watch last
        expectedPayoffs.values.forEach { list ->
            assertTrue(list.count { it in setOf("c17_pay_taken", "c17_pay_left") } <= 1)
            assertTrue(list.count { it in setOf("c17_pay_arrangement", "c17_pay_interruption") } <= 1)
            if ("c17_pay_stores" in list) assertEquals("c17_pay_stores", list.first())
            list.firstOrNull { it in setOf("c17_pay_taken", "c17_pay_left") }?.let { assertEquals(it, list.last()) }
        }
    }

    @Test
    fun `the approach hands on to the payoffs, and the guardian is entered only by the final choice from a payoff`() {
        fun into(id: String) = graph.edges.filter { it.to == id }.toSet()
        assertEquals(rungScenes, into("c17_approach").map { it.from }.toSet())
        rungScenes.forEach { assertEquals(setOf(road), into(it).map { e -> e.from }.toSet()) }
        assertEquals(payoffScenes, graph.edges.filter { it.from == "c17_approach" }.map { it.to }.toSet())
        assertEquals("the guardian is entered only from a payoff", payoffScenes, into(chamber).map { it.from }.toSet())
        into(chamber).forEach { e ->
            assertEquals("Enter the chamber.", e.choice!!.label)
            assertEquals("every final choice clears all five, and nothing else", prep, e.choice.consequences.clearFlags)
            assertTrue(e.choice.consequences.setFlags.isEmpty())
        }
        assertEquals("six final choices, one from each payoff scene", 6, into(chamber).size)
        assertTrue("every road to the guardian passes the road scene", graph.mustPass(into(road), chamber, "the_signal"))
        assertTrue("every road to the guardian passes the approach", graph.mustPass(into("c17_approach"), chamber, "the_signal"))
    }

    // ---- structure: where each marker is set and cleared, and that nothing after the payoffs can see them ----

    @Test
    fun `each marker is set only where the plan says`() {
        fun setters(flag: String) = graph.settersOf(setOf(flag)).map { "${it.from} > ${it.to}" }.sorted()
        assertEquals(listOf("c17_prepare > c17_stores", "c17_prepare_second > c17_stores"), setters(stores))
        assertEquals(listOf("c17_prepare > c17_watch_site", "c17_prepare_second > c17_watch_site"), setters(watch))
        assertEquals(listOf("c17_prepare > c17_records", "c17_prepare_second > c17_records"), setters(records))
        assertEquals(listOf("c17_watch_site > c17_watch_take"), setters(took))
        assertEquals(listOf("c17_records > c17_records_interrupt"), setters(studied))
    }

    @Test
    fun `the markers are cleared on entering the chain and by every final choice into the guardian, and nowhere else`() {
        prep.forEach { f ->
            val clearers = graph.clearersOf(f)
            assertEquals("$f: three chain choices and six final choices", 9, clearers.size)
            clearers.forEach { e ->
                assertTrue("$f cleared at ${e.from} -> ${e.to}", e.from == "c17_chain_gate" || (e.to == chamber && e.from in payoffScenes))
            }
            assertTrue("$f is cleared on leaving the hub for the road", clearers.none { it.to == road })
        }
    }

    @Test
    fun `nothing outside the new scenes reads, sets or clears a marker, and nothing after the guardian can come back`() {
        val outside = allNodes.filter { it.id !in newScenes }.flatMap { n ->
            n.choices.filter { c ->
                val touched = c.consequences.setFlags + c.consequences.clearFlags + (c.requirements?.requiredFlags ?: emptySet()) +
                    (c.requirements?.forbiddenFlags ?: emptySet())
                touched.any { it in prep }
            }.map { n.id }
        }
        assertEquals("scenes that touch a Chapter 17 marker", emptyList<String>(), outside)
        assertTrue("a scene of the chain, the hub or the payoffs can be reached from the guardian", graph.reach(listOf(chamber)).none { it in newScenes })
    }

    @Test
    fun `every road from the first scene to the shipped road passes the chain gate and the first table`() {
        fun into(id: String) = graph.edges.filter { it.to == id }.toSet()
        assertTrue(graph.mustPass(into("c17_chain_gate"), road, "the_signal"))
        assertTrue(graph.mustPass(into("c17_prepare"), road, "the_signal"))
        assertEquals(setOf("gathering_what_they_have_asked", "gathering_what_they_have_moved"), into("c17_chain_gate").map { it.from }.toSet())
        assertEquals(setOf("c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred"), into("c17_prepare").map { it.from }.toSet())
        assertEquals("the road is entered only from the hub", setOf("c17_prepare", "c17_prepare_second") + siteExits, into(road).map { it.from }.toSet())
        assertEquals("the second table is entered only from a first visit's exit", siteExits, into("c17_prepare_second").map { it.from }.toSet())
    }

    @Test
    fun `the new scenes change no stat, item or trophy, and set or clear nothing but the five markers`() {
        newScenes.forEach { id ->
            real.getValue(id).choices.forEach { c ->
                val q = c.consequences
                assertTrue("$id '${c.label}' changes a stat", q.statDeltas.isEmpty())
                assertTrue("$id '${c.label}' grants an item", q.grantItemIds.isEmpty())
                assertNull("$id '${c.label}' unlocks a trophy", q.unlockTrophy)
                assertTrue("$id '${c.label}' sets ${q.setFlags - prep}", prep.containsAll(q.setFlags))
                assertTrue("$id '${c.label}' clears ${q.clearFlags - prep}", q.clearFlags.isEmpty() || q.clearFlags == prep)
                assertNull("$id '${c.label}' is gated on a stat", c.requirements?.minStats?.takeIf { it.isNotEmpty() })
                assertEquals("$id '${c.label}' shows a locked choice", com.thelastjailer.app.LockedDisplay.HIDE, c.lockedDisplay)
            }
            assertNull("$id is a fight", real.getValue(id).combatEncounterId)
        }
    }

    @Test
    fun `the five markers are hidden in the Journal as bookkeeping`() {
        val described = FlagDescriptions.entries.map { it.flag }.toSet()
        prep.forEach {
            assertEquals(it, HiddenReason.BOOKKEEPING, FlagDescriptions.hidden[it])
            assertFalse("$it is described in the Journal", it in described)
        }
    }

    // ---- replay ----

    @Test
    fun `a replay that arrives with every marker still held reaches the hub clean`() {
        listOf(prep, setOf(stores, took), setOf(watch, records, studied), setOf(studied)).forEach { stale ->
            val s = at("c17_chain_gate", *stale.toTypedArray()).pick("Continue.")
            val atHub = generateSequence(s) { if (it.sceneId == "c17_prepare") null else it.pick("Continue.") }.last()
            assertEquals("the hub", "c17_prepare", atHub.sceneId)
            assertEquals("markers at the hub after $stale", emptySet<String>(), five(atHub))
            assertEquals("a replayed first table", listOf("Visit the stores.", "Speak to the watch captain.", "Study the old records.", "Depart for Ashfall."), labels(atHub))
        }
    }

    @Test
    fun `a replay holding every marker is shown only the payoffs of this telling, and leaves clean`() {
        // visits nothing: only the no-preparation passage, whatever the last telling left behind
        var s = walkTo(at("c17_chain_gate", *(prep + "c7_rung_trusted").toTypedArray()), "c17_prepare").first
        var (end, seen) = walkTo(s.pick("Depart for Ashfall."), chamber)
        assertEquals(listOf("the_road_to_ashfall_again", "c17_road_trusted", "c17_approach", "c17_pay_none", chamber), seen)
        assertEquals(emptySet<String>(), five(end))
        // visits only the watch and leaves the guard: only that payoff, none of the five the last telling left
        s = walkTo(at("c17_chain_gate", *prep.toTypedArray()), "c17_prepare").first
        val watchSite = s.pick("Speak to the watch captain.").pick("Leave every defender at Stonebeard.")
        val out = watchSite.pick("Continue.").pick("Depart for Ashfall.")
        val result = walkTo(out, chamber)
        end = result.first
        seen = result.second
        assertEquals(listOf(road, "c17_road_wary", "c17_approach", "c17_pay_left", chamber), seen)
        assertEquals(emptySet<String>(), five(end))
    }

    // ---- Chapter 18 continuity ----

    @Test
    fun `from every end state through the guardian and the frame to Chapter 18, no marker is held and the frame still pays`() {
        val encounter = CombatRepository.encounter("chamber_guardian_encounter")
        expectedPayoffs.keys.forEach { state ->
            listOf(true, false).forEach { victory ->
                listOf(GameState(), GameState(courage = 20, honour = 20)).forEach { base ->
                    val approach = base.copy(chapterId = "chapter_17", sceneId = "c17_approach", flags = state)
                    var s = walkTo(approach, chamber).first
                    assertEquals("markers at the guardian for $state", emptySet<String>(), five(s))
                    s = s.resolveCombat(encounter, CombatOutcome(victory = victory, damageTaken = 0))
                    assertEquals(if (victory) "what_must_be_broken" else "chamber_guardian_defeat", s.sceneId)
                    if (!victory) s = s.pick("Get to the frame.")
                    assertEquals("what_must_be_broken", s.sceneId)
                    val frame = StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s)
                    assertTrue("a frame choice is open", frame.isNotEmpty())
                    frame.forEach { c ->
                        var t = s.applyChoice(c)
                        assertEquals("chapter17_end", t.sceneId)
                        assertTrue("the frame component is granted", "twisted_frame_component" in t.inventory)
                        assertEquals(emptySet<String>(), five(t))
                        t = t.pick("Go home.")
                        assertEquals("no_more_envoys", t.sceneId)
                        assertEquals("a marker reaches Chapter 18", emptySet<String>(), five(t))
                    }
                }
            }
        }
    }

    // ---- negative controls: the same walk on copies of the real scenes with one thing broken ----

    private fun mutated(id: String, change: (StoryNode) -> StoryNode): Map<String, StoryNode> = real + (id to change(real.getValue(id)))

    private fun StoryNode.changing(index: Int, change: (Choice) -> Choice) =
        copy(choices = choices.mapIndexed { i, c -> if (i == index) change(c) else c })

    private fun noticed(what: String, nodes: Map<String, StoryNode>, containing: String) {
        val problems = problemsOf(nodes)
        assertTrue("the walk did not notice $what (found: $problems)", problems.any { it.contains(containing) })
    }

    @Test
    fun `the walk notices a site offered twice, a loop, a dead end, a stale marker and a wrong count in the chain and the hub`() {
        assertEquals("the real scenes are clean", emptyList<String>(), problemsOf(real))
        noticed("the second table offering the stores again", mutated("c17_prepare_second") { it.changing(0) { c -> c.copy(requirements = null) } }, "offers c17_stores again")
        noticed("a site leading back to the first table", mutated("c17_stores") { it.changing(0) { c -> c.copy(nextNodeId = "c17_prepare") } }, "loop")
        noticed("a missing Set out copy", mutated("c17_stores") { it.copy(choices = it.choices.take(2)) }, "dead end")
        noticed("a chain choice that does not clear", mutated("c17_chain_gate") { it.changing(0) { c -> c.copy(consequences = Consequences()) } }, "markers held on entering the hub")
        noticed("a policy copy shown together with another", mutated("c17_chain_gate") { it.changing(2) { c -> c.copy(requirements = null) } }, "c17_chain_gate shows")
        noticed("the way out missing from the second table", mutated("c17_prepare_second") { it.copy(choices = it.choices.dropLast(1)) }, "c17_prepare_second shows")
        noticed("the way out missing from the first table", mutated("c17_prepare") { it.copy(choices = it.choices.dropLast(1)) }, "c17_prepare shows")
        noticed("the guard marked by leaving instead of taking", mutated("c17_watch_site") {
            it.changing(0) { c -> c.copy(consequences = Consequences()) }.changing(1) { c -> c.copy(consequences = Consequences(setFlags = setOf(took))) }
        }, "markers held after")
        noticed("a typo in a visit marker", mutated("c17_prepare") { it.changing(0) { c -> c.copy(consequences = Consequences(setFlags = setOf("c17_visited_store"))) } }, "end states differ")
        noticed("a Continue that survives a second visit", mutated("c17_watch_take") { it.changing(0) { c -> c.copy(requirements = null) } }, "c17_watch_take shows")
        noticed("a decision whose two answers are the same", mutated("c17_records") { it.changing(1) { c -> c.copy(consequences = Consequences()) } }, "end states differ")
    }

    @Test
    fun `the walk notices a lost marker, a marker that survives, a skipped, repeated or misordered payoff, and a wrong Voss scene`() {
        // the clear moved: a hub exit, or a Voss scene, that clears before the payoffs have read the markers
        noticed("a hub exit that still clears the markers", mutated("c17_records_study") { it.changing(1) { c -> c.copy(consequences = Consequences(clearFlags = prep)) } }, "markers lost between the hub and the last payoff")
        noticed("a Voss scene that clears the markers", mutated("c17_road_trusted") { it.changing(0) { c -> c.copy(consequences = Consequences(clearFlags = prep)) } }, "markers lost between the hub and the last payoff")
        // the final choice
        noticed("a final choice that does not clear", mutated("c17_pay_taken") { it.changing(0) { c -> c.copy(consequences = Consequences()) } }, "markers survive the final choice")
        noticed("a final choice that clears only four", mutated("c17_pay_taken") { it.changing(0) { c -> c.copy(consequences = Consequences(clearFlags = prep - took)) } }, "markers survive the final choice")
        noticed("an entrance to the guardian that skips the road", mutated("c17_prepare") { it.changing(3) { c -> c.copy(nextNodeId = chamber) } }, "the chamber is entered without the road")
        // the payoffs
        noticed("a skipped payoff", mutated("c17_approach") { it.changing(1) { c -> c.copy(nextNodeId = "c17_pay_taken") } }, "payoffs after")
        noticed("a payoff shown twice", mutated("c17_pay_left") { it.changing(0) { c -> c.copy(label = "Go on.", nextNodeId = "c17_pay_arrangement", consequences = Consequences()) } }, "loop")
        noticed("the watch shown before the records", mutated("c17_approach") { it.changing(3) { c -> c.copy(requirements = c.requirements?.copy(forbiddenFlags = setOf(stores))) } }, "c17_approach shows")
        noticed("the no-preparation passage shown with a visit", mutated("c17_approach") { it.changing(5) { c -> c.copy(requirements = null) } }, "c17_approach shows")
        noticed("a payoff that leads nowhere for one state", mutated("c17_pay_stores") { it.copy(choices = it.choices.drop(1)) }, "dead end")
        noticed("the approach leading straight to the guardian for no preparation", mutated("c17_approach") { it.changing(5) { c -> c.copy(nextNodeId = chamber) } }, "payoffs after")
        // Voss
        noticed("a rung copy with the wrong gate", mutated(road) { it.changing(1) { c -> c.copy(requirements = c.requirements?.copy(requiredFlags = setOf("c7_rung_trusted"))) } }, "$road shows")
        noticed("a Voss scene that skips the approach", mutated("c17_road_wary") { it.changing(0) { c -> c.copy(nextNodeId = chamber) } }, "the approach does not follow")
    }

    // ---- the approved wording ----

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private val titles = mapOf(
        "c17_chain_gate" to "The Chain at the Gate", "c17_wardens" to "The New Wardens", "c17_watch" to "The Watch Kept",
        "c17_council" to "The Question Left Open", "c17_door_open" to "Those Who Came Inside", "c17_door_barred" to "The Help Beyond the Gate",
        "c17_prepare" to "What Stonebeard Can Spare", "c17_prepare_second" to "Time for One More", "c17_stores" to "The Quartermaster's Promise",
        "c17_watch_site" to "The North Gap", "c17_watch_take" to "One for the Road", "c17_watch_leave" to "The Wall Before the Road",
        "c17_records" to "The Six Settings", "c17_records_study" to "What the Wards Remember", "c17_records_interrupt" to "Where the Pattern Breaks",
        "c17_road_trusted" to "What We Carry",
        "c17_road_working" to "What We Carry",
        "c17_road_wary" to "What We Carry",
        "c17_approach" to "The Last Mile",
        "c17_pay_stores" to "The Quartermaster's Pack",
        "c17_pay_arrangement" to "The Marked Threshold",
        "c17_pay_interruption" to "Where the Lines Meet",
        "c17_pay_taken" to "A Shield on the Road",
        "c17_pay_left" to "The Shield Left Behind",
        "c17_pay_none" to "No Time to Prepare"
    )

    /** SHA-256 of each scene's whitespace-collapsed text, from the approved package. `c17_prepare_second` was written for the engine and approved by the author. */
    private val approved = mapOf(
        "c17_chain_gate" to "5597658d8fb4070a1e485be808a13914d4eb1d44e91567c89505d7bf1ad18b9a",
        "c17_wardens" to "99f22b7fe73d110d57c1a57d4d90abd50f67a6c1f67f6e56f0f4529c21ccaa2f",
        "c17_watch" to "64fb1c311ccd65fe9cb7660fae74ce99d35488cc52997c588ceacc05c7e82042",
        "c17_council" to "79921e70f3d7350c422e15e4ba60ebced175933ce391dc5022d5fc755e47ea48",
        "c17_door_open" to "235de18d9525f52e625546ae973a64d778e8462937952e730e8ed94694068049",
        "c17_door_barred" to "3e0c73fa1c1516f6a0bbd9125eaf38472b47e08fa04c1f0d8e9dfd022bc0df92",
        "c17_prepare" to "739cd329e31bf03f9e6c732802d2f81f5e48a357d9810376237d08edc3739ea1",
        "c17_prepare_second" to "b928ff02b0abee5c2a3603cd37c1da80c878cb2830e4d0e9673a32c9692d6148",
        "c17_stores" to "f57e634f7d18cf4668d31965f421a9cb445cbcb71ab9dd3c7b57b442eab940a0",
        "c17_watch_site" to "858df159b897a89f42f2c0c92d69052e9072625025be15ffeb8d1b6ec014ecde",
        "c17_watch_take" to "95c31060fd26c4e011deb7b7f8498097b58fd941cce244a57eaf42e3902d1dbd",
        "c17_watch_leave" to "e1d28f9c0f30d20383dd2fe1ccea8e02e6a04a28bf1a04a2a8d7ee613b875bd9",
        "c17_records" to "cde56391758c30146a2c18c110656156369b99da059c0fb6231d1de58e39cc31",
        "c17_records_study" to "7b66883a44dd71194ef06c8a70d71f537d998e8427866cf3cad52686759ca1e7",
        "c17_records_interrupt" to "d85c492f50e7e1a9c0cbe9e2e2acfceffecfc9c2c80e3379652fb4f5ad45a050",
        "c17_road_trusted" to "a97873e5f2d09a1439d357fa6bf8a93d24eeaced9b2ebf52d7dc492cf2359329",
        "c17_road_working" to "2df3f7348293a610d38b711d4ed7b8dd2cd3c37bf6f547dffbb8c26b15e16fdc",
        "c17_road_wary" to "e7686e214d93c29e86f29a0e748b7ad1229bfd4054c5831c640bea8e0bfad554",
        "c17_approach" to "51121ba99bd7ec1ff49014fe9b3c844271eb1a762bd8fda6ec0bcabc86d5e592",
        "c17_pay_stores" to "058e405ea668c2d872aa4ecd7ac177849ba8ab55a2287620bc5e0345e15166ed",
        "c17_pay_arrangement" to "06e12dd67e5ace3dfe5f1c9e1fd537c045472c710dd2cb87438a638d10df1d64",
        "c17_pay_interruption" to "b0a7308b67fd3dba5e0359aaf810c019a5d015a4575eedf165c067d2349766ce",
        "c17_pay_taken" to "e20c91415cc0479b175109e330ba58eb58316e468a95417e7e5c564a86c32763",
        "c17_pay_left" to "0867b5cd9ddbf4341c49619314875c7a26e4fa7867f4c56d0724f3b0e956e1b6",
        "c17_pay_none" to "697f494dd88c7a264d600d4f2449c9844b70336a2159993edf4e1630a94d36e7"
    )

    @Test
    fun `the approved prose and titles are word for word, in Chapter 17, with art that exists`() {
        assertEquals(newScenes, approved.keys)
        assertEquals(newScenes, titles.keys)
        val art = setOf("dwarven_hold_gate", "who_stands_with_stonebeard", "gathering_what_they_have", "back_to_the_ash", "ashfall_sanctum_vista")
        newScenes.forEach { id ->
            val n = real.getValue(id)
            assertEquals("$id chapter", "chapter_17", n.chapterId)
            assertEquals("$id title", titles.getValue(id), n.title)
            assertEquals("$id prose", approved.getValue(id), sha(collapse(n.narrativeText)))
            assertTrue("$id art ${n.illustrationId}", n.illustrationId in art)
            assertNotNull("$id art does not exist", IllustrationCatalog.get(n.illustrationId))
        }
        assertEquals(StoryRepository.chapters.single { it.id == "chapter_17" }.startNodeId, "the_signal")
    }

    @Test
    fun `the new scenes keep the house style`() {
        val prose = newScenes.flatMap { id -> real.getValue(id).let { n -> listOf(id to collapse(n.narrativeText), id to n.title) + n.choices.map { id to it.label } } }
        fun hits(terms: List<String>) = prose.flatMap { (id, t) -> terms.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" } }
        assertEquals("dashes", emptyList<String>(), prose.filter { (_, t) -> t.contains('—') || t.contains('–') }.map { it.first }.distinct())
        assertEquals("non-ASCII characters", emptyList<String>(), prose.filter { (_, t) -> t.any { it.code > 127 } }.map { it.first }.distinct())
        val fragment = Regex("(^|[.!?][\"']?\\s)Not\\s")
        assertEquals("Not-fragments", emptyList<String>(), prose.filter { (_, t) -> fragment.containsMatchIn(t) }.map { it.first }.distinct())
        val reserved = listOf("archivist", "Duskmere", "Wraithspire", "Emberlow", "Sundering", "the whole", "lad")
        assertEquals("reserved terms", emptyList<String>(), hits(reserved))
        val counts = listOf("three riders", "four riders", "four fragments", "three fragments", "four shards", "three shards", "fifteen years", "four settings")
        assertEquals("counts", emptyList<String>(), hits(counts))
        newScenes.forEach { id -> assertTrue("$id is short", collapse(real.getValue(id).narrativeText).split(" ").size >= 25) }
    }

    @Test
    fun `the choice labels are the approved ones`() {
        fun labelsOf(id: String) = real.getValue(id).choices.map { it.label }.distinct()
        assertEquals(listOf("Continue."), labelsOf("c17_chain_gate"))
        listOf("c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred").forEach { assertEquals(listOf("Continue."), labelsOf(it)) }
        assertEquals(listOf("Visit the stores.", "Speak to the watch captain.", "Study the old records.", "Depart for Ashfall."), labelsOf("c17_prepare"))
        assertEquals(listOf("Visit the stores.", "Speak to the watch captain.", "Study the old records.", "Depart for Ashfall."), labelsOf("c17_prepare_second"))
        assertEquals(listOf("Take one guard to watch the approach.", "Leave every defender at Stonebeard."), labelsOf("c17_watch_site"))
        assertEquals(listOf("Study the six-position arrangement.", "Look for ways to interrupt the connections."), labelsOf("c17_records"))
        siteExits.forEach { assertEquals(it, listOf("Continue.", "Set out for Ashfall."), labelsOf(it)) }
        assertEquals(listOf("Get to the chamber."), labelsOf(road))
        rungScenes.forEach { assertEquals(it, listOf("Continue."), labelsOf(it)) }
        assertEquals(listOf("Go on."), labelsOf("c17_approach"))
        assertEquals(listOf("Go on.", "Enter the chamber."), labelsOf("c17_pay_stores"))
        listOf("c17_pay_arrangement", "c17_pay_interruption").forEach { assertEquals(it, listOf("Go on.", "Enter the chamber."), labelsOf(it)) }
        listOf("c17_pay_taken", "c17_pay_left", "c17_pay_none").forEach { assertEquals(it, listOf("Enter the chamber."), labelsOf(it)) }
    }
}
