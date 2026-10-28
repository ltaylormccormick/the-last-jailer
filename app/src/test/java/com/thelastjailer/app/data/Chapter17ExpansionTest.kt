package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * B1 of the Chapter 17 expansion: Stonebeard before the road. A chain scene at the gate, one scene for the policy Kaelen set in
 * Chapter XIII, one for the door he told Edda about (or none, for a player who promised her nothing), and a preparation hub of two
 * tables: Stores, Watch and Records, of which Kaelen may visit none, one or two, never the same one twice. Five hidden markers
 * record what he did there. The road, the guardian, the frame and the way on to Chapter XVIII are the shipped ones.
 *
 * The hub is proved by walking it, not by reading it. [explore] takes every choice that is open in every state from the chain gate
 * to the road, for every policy and every door and with or without markers left over from an earlier telling, and [problemsOf]
 * collects whatever goes wrong: a loop, a dead end, a third site, a site offered twice, a marker that survives the hub, a table that
 * shows the wrong number of choices, a state the plan does not list, a choice that can never be taken. It is a plain function of the
 * scenes, so the negative controls run it on copies of the real scenes with one thing broken each.
 *
 * TEMPORARY B1 BEHAVIOUR. Nothing after the road reads the five markers yet, so this batch clears them on every way out of the hub
 * (`setOut` in Chapter17.kt). That is not the finished design. When the approach and payoff scenes arrive (B2), the markers must
 * survive the road and be cleared by the final "Enter the chamber." choice into `the_chamber_defended`, after every payoff has been
 * shown, and the hub exits must stop clearing. B2 must therefore update the following in this file; each is marked "TEMPORARY (B1)":
 *  - [explore]: the way out no longer clears, so the "markers survive the way out" problem and `Exit.after` change;
 *  - `the markers are cleared on entering the chain and on every way out of the hub, and nowhere else`: the clearers become the three
 *    chain choices plus the final choice into the chamber, not twelve hub exits;
 *  - `every end state the plan lists leaves the hub with no marker, and none can be held on the road`: the markers now survive the hub
 *    and are cleared at the chamber;
 *  - the negative control "an exit that keeps the markers";
 *  - `the road still leads straight to the guardian, and nothing of the approach exists yet`: the road gains three rung copies and the
 *    approach scenes exist.
 * The clear on the chain gate (replay safety) and the replay test stay as they are.
 */
class Chapter17ExpansionTest {

    private val prep = C17_PREPARATION_FLAGS
    private val stores = "c17_visited_stores"
    private val watch = "c17_visited_watch"
    private val records = "c17_visited_records"
    private val took = "c17_took_guard"
    private val studied = "c17_studied_junctions"
    private val road = "the_road_to_ashfall_again"

    private val policy = listOf("began_training_new_wardens", "stood_watch_rather_than_delegate", "deferred_the_question")
    private val door = listOf("c13_door_open", "c13_door_barred")

    private val chainScenes = setOf("c17_chain_gate", "c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred")
    private val hubScenes = setOf(
        "c17_prepare", "c17_prepare_second", "c17_stores", "c17_watch_site", "c17_watch_take", "c17_watch_leave", "c17_records",
        "c17_records_study", "c17_records_interrupt"
    )
    private val newScenes = chainScenes + hubScenes
    private val siteScenes = setOf("c17_stores", "c17_watch_site", "c17_records")
    private val siteExits = setOf("c17_stores", "c17_watch_take", "c17_watch_leave", "c17_records_study", "c17_records_interrupt")
    private val siteFlag = mapOf("c17_stores" to stores, "c17_watch_site" to watch, "c17_records" to records)

    /** How many choices each scene shows in every state it can be reached in. */
    private val expectedVisible = mapOf(
        "c17_chain_gate" to 1, "c17_wardens" to 1, "c17_watch" to 1, "c17_council" to 1, "c17_door_open" to 1, "c17_door_barred" to 1,
        "c17_prepare" to 4, "c17_prepare_second" to 3, "c17_stores" to 1, "c17_watch_site" to 2, "c17_watch_take" to 1,
        "c17_watch_leave" to 1, "c17_records" to 2, "c17_records_study" to 1, "c17_records_interrupt" to 1
    )

    /**
     * What the markers can hold at the moment Kaelen leaves for the road: no site; one site (the guard's fate and the records' study
     * count as part of the visit); two sites. Fourteen states, as the plan lists.
     */
    private val expectedEnds: Set<Set<String>> = setOf(
        emptySet(), setOf(stores), setOf(watch), setOf(watch, took), setOf(records), setOf(records, studied),
        setOf(stores, watch), setOf(stores, watch, took), setOf(stores, records), setOf(stores, records, studied),
        setOf(watch, records), setOf(watch, took, records), setOf(watch, records, studied), setOf(watch, took, records, studied)
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

    private class Exit(val path: List<String>, val before: Set<String>, val after: Set<String>, val label: String)

    private class Report(val exits: List<Exit>, val taken: Set<String>, val counts: Map<String, Set<Int>>, val problems: List<String>)

    private fun five(s: GameState): Set<String> = s.flags.intersect(prep)

    private fun openChoices(n: StoryNode, s: GameState) = n.choices.withIndex().filter { (_, c) -> c.requirements?.isSatisfiedBy(s) ?: true }

    /** Every way from [start] to the road through [nodes], taking every choice that is open in every state. */
    private fun explore(nodes: Map<String, StoryNode>, start: GameState): Report {
        val exits = ArrayList<Exit>()
        val taken = HashSet<String>()
        val problems = ArrayList<String>()
        val counts = HashMap<String, MutableSet<Int>>()
        fun go(s: GameState, path: List<String>) {
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
                if (c.nextNodeId == road) {
                    // TEMPORARY (B1): the hub exits clear the markers. B2 moves the clear to the final "Enter the chamber." choice.
                    if (five(next).isNotEmpty()) problems += "markers survive the way out of ${n.id}: ${five(next)}"
                    exits += Exit(path + n.id, five(s), five(next), c.label)
                } else go(next, path + n.id)
            }
        }
        go(start, emptyList())
        return Report(exits, taken, counts, problems)
    }

    /** A player at the chain gate for every policy (or none) and every door (or none), with or without markers left from before. */
    private fun chainStarts(stale: Boolean): List<GameState> =
        (policy.map { setOf(it) } + listOf(emptySet<String>())).flatMap { p ->
            (door.map { setOf(it) } + listOf(emptySet<String>())).map { d ->
                GameState(chapterId = "chapter_17", sceneId = "c17_chain_gate", flags = p + d + (if (stale) prep else emptySet()))
            }
        }

    private fun problemsOf(nodes: Map<String, StoryNode>): List<String> {
        val out = ArrayList<String>()
        val reports = (chainStarts(false) + chainStarts(true)).map { explore(nodes, it) }
        reports.forEach { out += it.problems }
        val counts = reports.flatMap { it.counts.entries }.groupBy({ it.key }, { it.value }).mapValues { (_, v) -> v.flatten().toSet() }
        expectedVisible.forEach { (id, n) -> if (counts[id] != setOf(n)) out += "$id shows ${counts[id]} choices, expected $n" }
        val ends = reports.flatMap { it.exits }.map { it.before }.toSet()
        if (ends != expectedEnds) out += "end states differ: missing ${expectedEnds - ends}, unexpected ${ends - expectedEnds}"
        reports.forEach { if (it.exits.size != 22) out += "${it.exits.size} ways through the hub, expected 22" }
        reports.flatMap { it.exits }.forEach { e ->
            val sites = e.path.count { it in siteScenes }
            val fine = if (sites == 2) e.label == "Set out for Ashfall." && e.path.last() in siteExits
            else e.label == "Depart for Ashfall." && e.path.last() in setOf("c17_prepare", "c17_prepare_second")
            if (!fine) out += "after $sites sites the way out is '${e.label}' from ${e.path.last()}"
        }
        reports.flatMap { it.exits }.forEach { e ->
            val plan = e.path.flatMap { markersOf(it) }.toSet()
            if (e.before != plan) out += "markers held after ${e.path.joinToString(" > ")}: ${e.before}, the plan says $plan"
        }
        val taken = reports.flatMap { it.taken }.toSet()
        nodes.filterKeys { it in newScenes }.forEach { (id, n) -> n.choices.indices.forEach { if ("$id#$it" !in taken) out += "$id choice $it can never be taken" } }
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

    // ---- the plan, proved on the real scenes ----

    @Test
    fun `every way through the chain and the hub is clean, for every policy and door, fresh or replayed`() {
        val problems = problemsOf(real)
        assertEquals("problems walking the chain and the hub: $problems", emptyList<String>(), problems)
        val one = explore(real, chainStarts(false).first())
        println("HUB ${one.exits.size} ways through the hub; end states ${one.exits.map { it.before }.toSet().size}; scenes ${newScenes.size}; choices ${newScenes.sumOf { real.getValue(it).choices.size }}")
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
    fun `after a second site there is no table and no third site, only one Set out for Ashfall`() {
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
            assertEquals("the markers are cleared on the way out", emptySet<String>(), five(out))
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

    // ---- structure: where each marker is set and cleared, and that nothing after the hub can see them ----

    @Test
    fun `each marker is set only where the plan says`() {
        fun setters(flag: String) = graph.settersOf(setOf(flag)).map { "${it.from} > ${it.to}" }.sorted()
        assertEquals(listOf("c17_prepare > c17_stores", "c17_prepare_second > c17_stores"), setters(stores))
        assertEquals(listOf("c17_prepare > c17_watch_site", "c17_prepare_second > c17_watch_site"), setters(watch))
        assertEquals(listOf("c17_prepare > c17_records", "c17_prepare_second > c17_records"), setters(records))
        assertEquals(listOf("c17_watch_site > c17_watch_take"), setters(took))
        assertEquals(listOf("c17_records > c17_records_interrupt"), setters(studied))
    }

    // TEMPORARY (B1): clearing on every hub exit is interim. B2 removes the clear from the exits and puts it on the final
    // "Enter the chamber." choice, so the expected clearers change (see the class comment).
    @Test
    fun `the markers are cleared on entering the chain and on every way out of the hub, and nowhere else`() {
        prep.forEach { f ->
            val clearers = graph.clearersOf(f)
            assertEquals("$f: three chain choices and twelve hub exits", 15, clearers.size)
            clearers.forEach { e ->
                assertTrue("$f cleared at ${e.from} -> ${e.to}", e.from == "c17_chain_gate" || (e.to == road && e.from in hubScenes))
            }
        }
        assertEquals(setOf("c17_chain_gate"), graph.clearersOf(stores).filter { it.to != road }.map { it.from }.toSet())
    }

    @Test
    fun `nothing outside the fifteen scenes reads, sets or clears a marker, and nothing after the road can come back`() {
        val outside = allNodes.filter { it.id !in newScenes }.flatMap { n ->
            n.choices.filter { c ->
                val touched = c.consequences.setFlags + c.consequences.clearFlags + (c.requirements?.requiredFlags ?: emptySet()) +
                    (c.requirements?.forbiddenFlags ?: emptySet())
                touched.any { it in prep }
            }.map { n.id }
        }
        assertEquals("scenes that touch a Chapter 17 marker", emptyList<String>(), outside)
        assertTrue("a scene of the chain or the hub can be reached from the road", graph.reach(listOf(road)).none { it in newScenes })
    }

    @Test
    fun `every road from the first scene to the shipped road passes the chain gate and the first table`() {
        fun into(id: String) = graph.edges.filter { it.to == id }.toSet()
        assertTrue(graph.mustPass(into("c17_chain_gate"), road, "the_signal"))
        assertTrue(graph.mustPass(into("c17_prepare"), road, "the_signal"))
        assertEquals(setOf("gathering_what_they_have_asked", "gathering_what_they_have_moved"), into("c17_chain_gate").map { it.from }.toSet())
        assertEquals(setOf("c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred"), into("c17_prepare").map { it.from }.toSet())
        assertEquals("the road is entered only from the hub now", setOf("c17_prepare", "c17_prepare_second") + siteExits, into(road).map { it.from }.toSet())
        assertEquals("the second table is entered only from a first visit's exit", siteExits, into("c17_prepare_second").map { it.from }.toSet())
    }

    // ---- the boundary of this batch ----

    // TEMPORARY (B1 boundary): B2 gives the road three rung-gated copies and adds the approach and payoff scenes, so this test is
    // replaced there. The shipped-content pins (Chapter17ShippedPinTest) already accept both states.
    @Test
    fun `the road still leads straight to the guardian, and nothing of the approach exists yet`() {
        val r = real.getValue(road)
        assertEquals(1, r.choices.size)
        val c = r.choices.single()
        assertEquals("Get to the chamber.", c.label)
        assertEquals("the_chamber_defended", c.nextNodeId)
        assertNull(c.requirements)
        assertEquals(Consequences(), c.consequences)
        assertTrue("the Voss, approach and payoff scenes belong to the next batch", real.keys.none { it.startsWith("c17_road_") || it == "c17_approach" || it.startsWith("c17_pay_") })
        assertEquals(setOf(road), graph.edges.filter { it.to == "the_chamber_defended" }.map { it.from }.toSet())
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

    // TEMPORARY (B1): the markers are cleared at the hub exit only until the approach and payoff scenes read them. B2 changes the
    // `after` expectation: they survive the road and are cleared by "Enter the chamber.".
    @Test
    fun `every end state the plan lists leaves the hub with no marker, and none can be held on the road`() {
        val ends = chainStarts(false).flatMap { explore(real, it).exits }
        assertEquals(expectedEnds, ends.map { it.before }.toSet())
        ends.forEach { assertEquals("after ${it.path} holding ${it.before}", emptySet<String>(), it.after) }
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
    fun `the walk notices a site offered twice, a loop, a dead end, a marker that survives, a stale marker and a wrong count`() {
        assertEquals("the real scenes are clean", emptyList<String>(), problemsOf(real))
        noticed("the second table offering the stores again", mutated("c17_prepare_second") { it.changing(0) { c -> c.copy(requirements = null) } }, "offers c17_stores again")
        noticed("a site leading back to the first table", mutated("c17_stores") { it.changing(0) { c -> c.copy(nextNodeId = "c17_prepare") } }, "loop")
        noticed("a missing Set out copy", mutated("c17_stores") { it.copy(choices = it.choices.take(2)) }, "dead end")
        // TEMPORARY (B1): exits must clear. B2 drops this control with the exit clears and checks the final choice instead.
        noticed("an exit that keeps the markers", mutated("c17_records_study") { it.changing(1) { c -> c.copy(consequences = Consequences()) } }, "markers survive")
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

    // ---- the approved wording ----

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private val titles = mapOf(
        "c17_chain_gate" to "The Chain at the Gate", "c17_wardens" to "The New Wardens", "c17_watch" to "The Watch Kept",
        "c17_council" to "The Question Left Open", "c17_door_open" to "Those Who Came Inside", "c17_door_barred" to "The Help Beyond the Gate",
        "c17_prepare" to "What Stonebeard Can Spare", "c17_prepare_second" to "Time for One More", "c17_stores" to "The Quartermaster's Promise",
        "c17_watch_site" to "The North Gap", "c17_watch_take" to "One for the Road", "c17_watch_leave" to "The Wall Before the Road",
        "c17_records" to "The Six Settings", "c17_records_study" to "What the Wards Remember", "c17_records_interrupt" to "Where the Pattern Breaks"
    )

    /** SHA-256 of each scene's whitespace-collapsed text, from the approved package. `c17_prepare_second` is the one proposed line of prose. */
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
        "c17_records_interrupt" to "d85c492f50e7e1a9c0cbe9e2e2acfceffecfc9c2c80e3379652fb4f5ad45a050"
    )

    @Test
    fun `the approved prose and titles are word for word, in Chapter 17, with art that exists`() {
        assertEquals(newScenes, approved.keys)
        assertEquals(newScenes, titles.keys)
        val art = setOf("dwarven_hold_gate", "who_stands_with_stonebeard", "gathering_what_they_have")
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
    }
}
