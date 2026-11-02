package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * The Chapter 18-20 expansion: 55 scenes around the 31 that shipped. Chapter 18 gains the gate after Ashfall, the account given at
 * Stonebeard (told one of three ways by how the frame was stopped), the watch and the three days, the terms Ilsevet sets, Voss's
 * account after the duel and Ilsevet's last question, and a decision scene that names the right Voss. Chapter 19 gains the three
 * mornings after (one per fate), her account, the archivist's years, the prisoner's pause and the letter to Selvane, and the six
 * scenes between the report and the names. Chapter 20 gains the departure, a mile for each way of going, the village, the bench and
 * the recess, the guardian's warning, what is left after it, the final margin and the walk home.
 *
 * The story is proved by walking it, not by reading it. [explore] takes every choice that is open in every state, and both outcomes
 * of every fight, through one chapter at a time (18 from the gate to its end, 19 from its fate scene to the departure, 20 from the
 * departure to the prisoner's answer), starting from every frame outcome, every rung and every fate flag a save can hold, stale
 * combinations included, with low stats and high. [problemsOf] collects whatever goes wrong: a loop, a dead end, the wrong frame
 * scene or the wrong Voss, two fate flags, an item granted twice or not at all, a defeat flag or a trophy on the wrong outcome, a
 * choice that can never be taken. It is a plain function of the scenes, so the negative controls run it on copies of the real
 * scenes with one thing broken each.
 */
class Chapter1820ExpansionTest {

    private val ch18 = listOf("c18_gate_after_ashfall", "c18_marks", "c18_quiet_wall", "c18_stonebeard_report", "c18_frame_destroyed", "c18_frame_disrupted", "c18_frame_uncertain", "c18_watch", "c18_voss_warning", "c18_voss_silence", "c18_three_days", "c18_after_terms", "c18_ashwell_question", "c18_cost_question", "c18_before_blades", "c18_after_duel", "c18_voss_account", "c18_ilsevet_last_question", "c18_choice_return")
    private val ch19 = listOf("c19_fate_captured", "c19_fate_mercy", "c19_mercy_meeting", "c19_fate_deferred", "c19_testimony_terms", "c19_ashwell_years", "c19_the_familiar_hand", "c19_prisoner_pause", "c19_ink", "c19_fear", "c19_letter", "c19_report_seal", "c19_dates", "c19_envelopes", "c19_voss_reading", "c19_kaelen_reading", "c19_choice_preparation", "c19_after_defiance", "c19_after_claim", "c19_after_silence")
    private val ch20 = listOf("c20_before_departure", "c20_alone_mile", "c20_with_voss_mile", "c20_village_edge", "c20_register", "c20_workshop_door", "c20_bench", "c20_ink_layers", "c20_personal_effects", "c20_recess", "c20_oldest_page", "c20_guardian_warning", "c20_after_guardian", "c20_final_margin", "c20_sixth_question", "c20_homeward")
    private val newScenes: List<String> = ch18 + ch19 + ch20
    private val held = listOf("c20_grief_heard", "c20_need", "c20_wait", "c20_choice_return")

    private val destroyed = "destroyed_the_frame_outright"
    private val disrupted = "disrupted_carefully"
    private val stopped = "stopped_it_however_possible"
    private val trusted = "c7_rung_trusted"
    private val working = "c7_rung_working"
    private val wary = "c7_rung_wary"
    private val captured = "captured_ilsevet"
    private val mercy = "showed_ilsevet_mercy"
    private val deferred = "deferred_ilsevets_fate"
    private val fate: Set<String> = ILSEVET_FATE_FAMILY
    private val frame: Set<String> = C17_FRAME_FLAGS
    private val rungs: Set<String> = setOf(trusted, working, wary)
    private val frameScenes = setOf("c18_frame_destroyed", "c18_frame_disrupted", "c18_frame_uncertain")
    private val answers = mapOf(
        "defied_the_prophecy" to ("c19_after_defiance" to "chapter19_end_defied"),
        "claimed_his_own_meaning" to ("c19_after_claim" to "chapter19_end_claimed"),
        "left_it_unanswered" to ("c19_after_silence" to "chapter19_end_unanswered")
    )
    private val prisonerAnswers = mapOf(
        "demanded_the_truth" to "chapter20_end_demanded",
        "gave_the_prisoner_time" to "chapter20_end_gave_time",
        "waited_in_silence" to "chapter20_end_waited"
    )

    private val real: Map<String, StoryNode> = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.associateBy { it.id }
    private val inChapters: List<StoryNode> = listOf("chapter_18", "chapter_19", "chapter_20").flatMap { StoryRepository.nodesInChapter(it) }
    private val graph = FlagFamilyProbe.StoryGraph(
        real.values.toList(),
        combatTargets = { n ->
            n.combatEncounterId?.let { CombatRepository.encounter(it) }?.let { listOfNotNull(it.victoryNodeId, it.defeatNodeId ?: it.victoryNodeId) }
                ?: emptyList()
        }
    )

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(collapse(text).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun visible(id: String, flags: Set<String>) = StoryRepository.visibleChoices(real.getValue(id), GameState(flags = flags))
    private fun into(id: String) = graph.edges.filter { it.to == id }.map { it.from }.toSet()

    // ---- walking the story ----

    private class Run(val path: List<String>, val end: GameState, val wins: List<Boolean>)

    private class Tally {
        val taken = HashSet<String>()
        val visited = HashSet<String>()
        val problems = LinkedHashSet<String>()
        var runs = 0
    }

    private fun List<String>.times(id: String) = count { it == id }
    private fun List<String>.followed(a: String, b: String) = indexOf(a).let { it >= 0 && getOrNull(it + 1) == b }

    /** Every way from [start] to [stop] through [nodes]: every open choice in every state, and a win and a defeat in every fight. */
    private fun explore(
        nodes: Map<String, StoryNode>, start: GameState, stop: String, t: Tally,
        onEnter: (String, GameState) -> Unit = { _, _ -> },
        onEnd: (Run) -> Unit
    ) {
        fun go(s: GameState, path: List<String>, wins: List<Boolean>) {
            if (s.sceneId == stop) { t.runs++; onEnd(Run(path + s.sceneId, s, wins)); return }
            val n = nodes[s.sceneId]
            if (n == null) { t.problems += "no scene ${s.sceneId} after ${path.takeLast(3)}"; return }
            if (n.id in path) { t.problems += "loop at ${n.id} after ${path.takeLast(4)}"; return }
            t.visited += n.id
            onEnter(n.id, s)
            val fight = n.combatEncounterId
            if (fight != null) {
                val encounter = CombatRepository.encounter(fight)
                listOf(true, false).forEach { v -> go(s.resolveCombat(encounter, CombatOutcome(victory = v, damageTaken = 0)), path + n.id, wins + v) }
                return
            }
            val here = n.choices.withIndex().filter { (_, c) -> c.requirements?.isSatisfiedBy(s) ?: true }
            if (here.isEmpty()) { t.problems += "dead end at ${n.id} holding ${s.flags.intersect(frame + rungs + fate)}"; return }
            here.forEach { (i, c) -> t.taken += "${n.id}#$i"; go(s.applyChoice(c), path + n.id, wins) }
        }
        go(start, emptyList(), emptyList())
    }

    private val bases = listOf(GameState(), GameState(courage = 30, honour = 30))
    private val lostToIlsevet = "lost_to_ilsevet_the_cinder_marshal"
    private val lostToGhostwriter = "lost_to_the_ghostwriter"
    private val blade = "ilsevets_blade"
    private val notes = "archivists_frame_notes"
    private val page = "archivists_final_page"

    private fun GameState.has(item: String) = inventory.count { it == item }

    /** Chapter 18, from the gate to its end, for every frame outcome (stale pairs and triples too), rung, and fate flags left from before. */
    private fun chapter18(nodes: Map<String, StoryNode>, t: Tally) {
        val frames = listOf<Set<String>>(emptySet(), setOf(destroyed), setOf(disrupted), setOf(stopped), setOf(destroyed, disrupted), setOf(disrupted, stopped), frame)
        val rungSets = listOf<Set<String>>(emptySet(), setOf(trusted), setOf(working), setOf(wary))
        val old = listOf<Set<String>>(emptySet(), fate)
        val prize = CombatRepository.encounter("ilsevet_duel_encounter").unlockTrophy
        for (f in frames) for (r in rungSets) for (o in old) for (base in bases) {
            val start = base.copy(chapterId = "chapter_18", sceneId = "c18_gate_after_ashfall", flags = f + r + o)
            explore(nodes, start, "chapter18_end", t) { run ->
                val p = run.path
                val at = "from frame $f rung $r old fate $o"
                val wantFrame = when { destroyed in f -> "c18_frame_destroyed"; disrupted in f -> "c18_frame_disrupted"; else -> "c18_frame_uncertain" }
                val gotFrame = p.filter { it in frameScenes }
                if (gotFrame != listOf(wantFrame)) t.problems += "frame scene $gotFrame, expected $wantFrame $at"
                val wantVoss = if (trusted in r) "what_kaelen_chooses_trusted" else "what_kaelen_chooses_guarded"
                val gotVoss = p.filter { it.startsWith("what_kaelen_chooses_") }
                if (gotVoss != listOf(wantVoss)) t.problems += "Voss scene $gotVoss, expected $wantVoss $at"
                val gotFate = run.end.flags.intersect(fate)
                if (gotFate.size != 1) t.problems += "fate flags at the end of Chapter 18: $gotFate $at"
                if (run.end.has(blade) != 1) t.problems += "$blade held ${run.end.has(blade)} times at the end of Chapter 18 $at"
                val won = run.wins.singleOrNull()
                if (won == null) t.problems += "${run.wins.size} fights in Chapter 18 $at"
                if ((lostToIlsevet in run.end.flags) == (won == true)) t.problems += "$lostToIlsevet is ${lostToIlsevet in run.end.flags} after a fight won=$won $at"
                if (prize != null && (prize in run.end.trophies) != (won == true)) t.problems += "trophy $prize held=${prize in run.end.trophies} after a fight won=$won $at"
                if (("ilsevet_duel_defeat" in p) != (won == false)) t.problems += "the defeat scene is ${"ilsevet_duel_defeat" in p} after a fight won=$won $at"
                if (run.end.flags.intersect(frame) != f) t.problems += "frame flags changed: ${run.end.flags.intersect(frame)} $at"
                if (run.end.flags.intersect(rungs) != r) t.problems += "rung flags changed: ${run.end.flags.intersect(rungs)} $at"
                if (!p.followed("c18_three_days", "no_more_envoys")) t.problems += "no_more_envoys does not follow the three days $at"
                if (!(p.followed("her_own_terms_readily", "c18_after_terms") || p.followed("her_own_terms_pressed", "c18_after_terms"))) t.problems += "the terms are not followed by c18_after_terms $at"
                if (!p.followed("c18_before_blades", "what_kaelen_says_first")) t.problems += "the blades do not lead to the first words $at"
                if (!p.followed("what_is_left_of_her", "c18_after_duel")) t.problems += "the duel's end is not followed by c18_after_duel $at"
                if (!p.followed("c18_choice_return", wantVoss)) t.problems += "the decision does not lead to $wantVoss $at"
                if (!p.followed(wantVoss, "chapter18_end")) t.problems += "$wantVoss does not lead to the end $at"
            }
        }
    }

    /** Chapter 19, from the end of Chapter 18 to the departure, for every fate flag a save can hold. */
    private fun chapter19(nodes: Map<String, StoryNode>, t: Tally) {
        val fates = listOf<Set<String>>(emptySet(), setOf(captured), setOf(mercy), setOf(deferred), setOf(captured, mercy), setOf(mercy, deferred), fate)
        for (held in fates) for (base in bases) {
            val start = base.copy(chapterId = "chapter_18", sceneId = "chapter18_end", flags = held)
            val onEnter: (String, GameState) -> Unit = { id, s ->
                if (id == "what_selvane_finds" && s.has(notes) != 0) t.problems += "$notes is held before the report is read"
                if ((id == "c19_report_seal" || id == "the_addressed_names") && s.has(notes) != 1) t.problems += "$notes held ${s.has(notes)} times at $id"
            }
            explore(nodes, start, "c20_before_departure", t, onEnter) { run ->
                val p = run.path
                val at = "from fate flags $held"
                val want = when { captured in held -> "c19_fate_captured"; mercy in held -> "c19_fate_mercy"; else -> "c19_fate_deferred" }
                if (p.getOrNull(1) != want) t.problems += "the morning after is ${p.getOrNull(1)}, expected $want $at"
                if (("c19_mercy_meeting" in p) != (want == "c19_fate_mercy")) t.problems += "the mercy meeting is ${"c19_mercy_meeting" in p} on the way from $want $at"
                if (!p.followed("c19_testimony_terms", "what_ilsevet_knew")) t.problems += "what_ilsevet_knew does not follow the terms $at"
                if (p.times("what_ilsevet_knew") != 1) t.problems += "what_ilsevet_knew is passed ${p.times("what_ilsevet_knew")} times $at"
                val archivist = p.filter { it == "the_dead_archivist_pressed" || it == "the_dead_archivist_patient" }
                if (archivist.size != 1) t.problems += "the archivist's scene is passed $archivist $at"
                if (!p.followed(archivist.singleOrNull() ?: "", "c19_ashwell_years")) t.problems += "the archivist's scene does not lead to the Ashwell years $at"
                if (!p.followed("c19_ashwell_years", "c19_the_familiar_hand")) t.problems += "the Ashwell years do not lead to the familiar hand $at"
                if (!p.followed("c19_the_familiar_hand", "the_prisoners_reaction")) t.problems += "the familiar hand does not lead to the prisoner's reaction $at"
                if (!p.followed("the_prisoners_reaction", "c19_prisoner_pause")) t.problems += "the prisoner's reaction does not lead to the pause $at"
                if (!p.followed("c19_letter", "what_selvane_finds")) t.problems += "the letter does not lead to the report $at"
                if (!p.followed("what_selvane_finds", "c19_report_seal")) t.problems += "the report is not followed by the seal $at"
                if (!p.followed("c19_choice_preparation", "the_addressed_names")) t.problems += "the names do not follow the preparation $at"
                val flags = run.end.flags.intersect(answers.keys)
                if (flags.size != 1) t.problems += "answer flags at the end of Chapter 19: $flags $at"
                val route = answers[flags.singleOrNull()]
                if (route == null || !p.followed("the_addressed_names", route.first) || !p.followed(route.first, route.second) || !p.followed(route.second, "c20_before_departure")) {
                    t.problems += "the answer $flags does not run names > ${route?.first} > ${route?.second} > the departure $at"
                }
                if (run.end.has(notes) != 1) t.problems += "$notes held ${run.end.has(notes)} times at the end of Chapter 19 $at"
                if (run.end.flags.intersect(fate) != held.intersect(fate)) t.problems += "the fate flags changed in Chapter 19: ${run.end.flags.intersect(fate)} $at"
            }
        }
    }

    /** Chapter 20, from the departure to the prisoner's answer, with low and high stats. */
    private fun chapter20(nodes: Map<String, StoryNode>, t: Tally) {
        val prize = CombatRepository.encounter("ghostwriter_encounter").unlockTrophy
        for (base in bases) {
            val start = base.copy(chapterId = "chapter_19", sceneId = "c20_before_departure")
            val onEnter: (String, GameState) -> Unit = { id, s -> if (id == "the_last_page" && s.has(page) != 0) t.problems += "$page is held before the last page is taken" }
            explore(nodes, start, "the_prisoner_answers", t, onEnter) { run ->
                val p = run.path
                val alone = "went_to_workshop_alone" in run.end.flags
                val withVoss = "brought_voss_to_workshop" in run.end.flags
                if (alone == withVoss) t.problems += "alone=$alone with Voss=$withVoss"
                val mile = if (alone) "c20_alone_mile" else "c20_with_voss_mile"
                val other = if (alone) "c20_with_voss_mile" else "c20_alone_mile"
                val road = if (alone) "the_road_to_the_workshop_alone" else "the_road_to_the_workshop_with_voss"
                val otherRoad = if (alone) "the_road_to_the_workshop_with_voss" else "the_road_to_the_workshop_alone"
                if (!p.followed("the_workshop", mile)) t.problems += "the_workshop does not lead to $mile"
                if (!p.followed(mile, road)) t.problems += "$mile does not lead to $road"
                if (!p.followed(road, "c20_village_edge")) t.problems += "$road does not lead to the village"
                if (other in p || otherRoad in p) t.problems += "the way not taken is on the path: $other $otherRoad"
                if (!p.followed("what_the_workshop_holds", "c20_guardian_warning")) t.problems += "the workbench does not lead to the warning"
                if (!p.followed("c20_guardian_warning", "the_workshops_own_guardian")) t.problems += "the warning does not lead to the guardian"
                val won = run.wins.singleOrNull()
                if (won == null) t.problems += "${run.wins.size} fights in Chapter 20"
                if ((lostToGhostwriter in run.end.flags) == (won == true)) t.problems += "$lostToGhostwriter is ${lostToGhostwriter in run.end.flags} after a fight won=$won"
                if (prize != null && (prize in run.end.trophies) != (won == true)) t.problems += "trophy $prize held=${prize in run.end.trophies} after a fight won=$won"
                if (("ghostwriter_defeat" in p) != (won == false)) t.problems += "the defeat scene is ${"ghostwriter_defeat" in p} after a fight won=$won"
                if (won == true && !p.followed("the_workshops_own_guardian", "c20_after_guardian")) t.problems += "a win does not lead to c20_after_guardian"
                if (won == false && !(p.followed("the_workshops_own_guardian", "ghostwriter_defeat") && p.followed("ghostwriter_defeat", "c20_after_guardian"))) t.problems += "a defeat does not lead through the defeat scene to c20_after_guardian"
                listOf("c20_after_guardian" to "c20_final_margin", "c20_final_margin" to "the_last_page", "the_last_page" to "c20_sixth_question",
                    "c20_sixth_question" to "c20_homeward", "c20_homeward" to "what_the_prisoner_says_now").forEach { (a, b) ->
                    if (!p.followed(a, b)) t.problems += "$a does not lead to $b"
                }
                if (run.end.has(page) != 1) t.problems += "$page held ${run.end.has(page)} times at the end of Chapter 20"
                val flags = run.end.flags.intersect(prisonerAnswers.keys)
                val ending = prisonerAnswers[flags.singleOrNull()]
                if (ending == null || !p.followed("what_the_prisoner_says_now", ending) || !p.followed(ending, "the_prisoner_answers")) {
                    t.problems += "the answer $flags does not run to ${ending} and the prisoner's answer"
                }
            }
        }
    }

    private fun problemsOf(nodes: Map<String, StoryNode>, t: Tally = Tally()): List<String> {
        chapter18(nodes, t)
        chapter19(nodes, t)
        chapter20(nodes, t)
        val out = ArrayList(t.problems)
        newScenes.forEach { id ->
            if (id !in t.visited) out += "$id is never reached"
            nodes.getValue(id).choices.indices.forEach { if ("$id#$it" !in t.taken) out += "$id choice $it can never be taken" }
        }
        return out.take(40)
    }

    // ---- the plan, proved on the real scenes ----

    @Test
    fun `every way through Chapters 18, 19 and 20 is clean, from every frame, rung and fate a save can hold`() {
        val t = Tally()
        val problems = problemsOf(real, t)
        assertEquals("problems walking Chapters 18 to 20: $problems", emptyList<String>(), problems)
        println("WALK ${t.runs} runs through Chapters 18 to 20; ${t.visited.size} scenes visited; ${t.taken.size} choices taken; ${newScenes.size} new scenes, all reached")
    }

    @Test
    fun `the three dispatch scenes show exactly one choice for every combination of the frame, rung and fate flags a save can hold`() {
        val flags = listOf(destroyed, disrupted, stopped, trusted, working, wary, captured, mercy, deferred)
        var checked = 0
        for (mask in 0 until (1 shl flags.size)) {
            val held = flags.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            val wantFrame = when { destroyed in held -> "c18_frame_destroyed"; disrupted in held -> "c18_frame_disrupted"; else -> "c18_frame_uncertain" }
            val wantVoss = if (trusted in held) "what_kaelen_chooses_trusted" else "what_kaelen_chooses_guarded"
            val wantFate = when { captured in held -> "c19_fate_captured"; mercy in held -> "c19_fate_mercy"; else -> "c19_fate_deferred" }
            assertEquals("c18_stonebeard_report holding $held", listOf(wantFrame), visible("c18_stonebeard_report", held).map { it.nextNodeId })
            assertEquals("c18_choice_return holding $held", listOf(wantVoss), visible("c18_choice_return", held).map { it.nextNodeId })
            assertEquals("chapter18_end holding $held", listOf(wantFate), visible("chapter18_end", held).map { it.nextNodeId })
            checked++
        }
        assertEquals(512, checked)
        listOf("c18_stonebeard_report", "c18_choice_return", "chapter18_end").forEach { id ->
            real.getValue(id).choices.forEach { assertEquals("$id: a dispatch copy has an effect", Consequences(), it.consequences) }
        }
        assertEquals("one label on every copy", 1, real.getValue("c18_stonebeard_report").choices.map { it.label }.toSet().size)
        assertEquals("one label on every copy", 1, real.getValue("c18_choice_return").choices.map { it.label }.toSet().size)
    }

    @Test
    fun `the Chapter 17 frame flags are only read here, never set or cleared, so they are reset only where a telling begins`() {
        val offenders = inChapters.flatMap { n -> n.choices.filter { c -> c.consequences.setFlags.any { it in frame } || c.consequences.clearFlags.any { it in frame } }.map { n.id } }
        assertEquals("choices in Chapters 18 to 20 that set or clear a Chapter 17 frame flag", emptyList<String>(), offenders)
        val clearers = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
            .flatMap { n -> n.choices.filter { c -> c.consequences.clearFlags.any { it in frame } }.map { n.id } }.toSet()
        assertEquals("the chain at Chapter XVII's gate is the only place that clears them", setOf("c17_chain_gate"), clearers)
    }

    // ---- how the new scenes join the shipped ones ----

    @Test
    fun `the new scenes are entered only from where the plan says`() {
        assertEquals(setOf("chapter17_end"), into("c18_gate_after_ashfall"))
        assertEquals(setOf("c18_three_days"), into("no_more_envoys"))
        assertEquals(setOf("her_own_terms_readily", "her_own_terms_pressed"), into("c18_after_terms"))
        assertEquals(setOf("what_is_left_of_her"), into("c18_after_duel"))
        assertEquals(setOf("c18_voss_account", "c18_ilsevet_last_question"), into("c18_choice_return"))
        assertEquals(setOf("chapter18_end"), into("c19_fate_captured") + into("c19_fate_mercy") + into("c19_fate_deferred"))
        assertEquals(setOf("c19_testimony_terms"), into("what_ilsevet_knew"))
        assertEquals(setOf("the_dead_archivist_pressed", "the_dead_archivist_patient"), into("c19_ashwell_years"))
        assertEquals(setOf("the_prisoners_reaction"), into("c19_prisoner_pause"))
        assertEquals(setOf("what_selvane_finds"), into("c19_report_seal"))
        assertEquals(setOf("c19_choice_preparation"), into("the_addressed_names"))
        assertEquals(setOf("chapter19_end_defied", "chapter19_end_claimed", "chapter19_end_unanswered"), into("c20_before_departure"))
        assertEquals(setOf("c20_before_departure"), into("the_workshop"))
        assertEquals(setOf("the_workshop"), into("c20_alone_mile") + into("c20_with_voss_mile"))
        assertEquals(setOf("c20_alone_mile"), into("the_road_to_the_workshop_alone"))
        assertEquals(setOf("c20_with_voss_mile"), into("the_road_to_the_workshop_with_voss"))
        assertEquals(setOf("the_road_to_the_workshop_alone", "the_road_to_the_workshop_with_voss"), into("c20_village_edge"))
        assertEquals(setOf("c20_oldest_page"), into("what_the_workshop_holds"))
        assertEquals(setOf("what_the_workshop_holds"), into("c20_guardian_warning"))
        assertEquals(setOf("the_workshops_own_guardian", "ghostwriter_defeat"), into("c20_after_guardian"))
        assertEquals(setOf("c20_final_margin"), into("the_last_page"))
        assertEquals(setOf("the_last_page"), into("c20_sixth_question"))
        assertEquals("the shipped Chapter 21 scene is entered only from the three endings of Chapter 20",
            setOf("chapter20_end_demanded", "chapter20_end_gave_time", "chapter20_end_waited"), into("the_prisoner_answers"))
    }

    @Test
    fun `every road passes the new scenes, so none can be skipped`() {
        fun leaving(id: String) = graph.edges.filter { it.from == id }.toSet()
        assertTrue(graph.mustPass(leaving("chapter17_end"), "c18_stonebeard_report", "the_signal"))
        assertTrue("every road from the gate to the duel passes the terms", graph.mustPass(leaving("c18_after_terms"), "what_kaelen_says_first", "c18_gate_after_ashfall"))
        assertTrue("every road to the decision passes Voss's account", graph.mustPass(leaving("c18_voss_account"), "c18_choice_return", "what_is_left_of_her"))
        assertTrue("every road from the end of Chapter 18 to her account passes a morning after",
            graph.mustPass(leaving("c19_fate_captured") + leaving("c19_fate_mercy") + leaving("c19_fate_deferred"), "what_ilsevet_knew", "chapter18_end"))
        assertTrue("every road to the report passes the letter", graph.mustPass(leaving("c19_letter"), "what_selvane_finds", "what_ilsevet_knew"))
        assertTrue("every road to the names passes the seal", graph.mustPass(leaving("c19_report_seal"), "the_addressed_names", "what_selvane_finds"))
        assertTrue("every road to the guardian passes the village", graph.mustPass(leaving("c20_village_edge"), "the_workshops_own_guardian", "the_workshop"))
        assertTrue("every road to the last page passes what is left after the guardian", graph.mustPass(leaving("c20_after_guardian"), "the_last_page", "the_workshop"))
        assertTrue("every road to the prisoner's answer passes the last page", graph.mustPass(leaving("the_last_page"), "the_prisoner_answers", "the_workshop"))
    }

    @Test
    fun `the way on from Chapter 17 to the prisoner's answer can be walked end to end by a player who always takes the first or the last open choice`() {
        val starts = listOf(
            Triple(setOf(destroyed, trusted), "c18_frame_destroyed", true),
            Triple(setOf(disrupted, working), "c18_frame_disrupted", false),
            Triple(setOf(stopped, wary), "c18_frame_uncertain", true),
            Triple(emptySet<String>(), "c18_frame_uncertain", false)
        )
        starts.forEach { (flags, wantFrame, high) ->
            val base = if (high) GameState(courage = 30, honour = 30) else GameState()
            listOf(true, false).forEach { victory ->
                listOf<(List<Choice>) -> Choice>({ it.first() }, { it.last() }).forEach { pick ->
                    var s = base.copy(chapterId = "chapter_17", sceneId = "chapter17_end", flags = flags)
                    val seen = LinkedHashSet<String>()
                    var steps = 0
                    while (s.sceneId != "the_prisoner_answers") {
                        assertTrue("a scene repeats: ${s.sceneId} after ${seen.toList().takeLast(4)}", seen.add(s.sceneId))
                        assertTrue("no end after $steps steps", ++steps < 400)
                        val n = real.getValue(s.sceneId)
                        val fight = n.combatEncounterId
                        s = if (fight != null) s.resolveCombat(CombatRepository.encounter(fight), CombatOutcome(victory = victory, damageTaken = 0))
                        else {
                            val open = StoryRepository.visibleChoices(n, s)
                            assertTrue("a dead end at ${n.id}", open.isNotEmpty())
                            s.applyChoice(pick(open))
                        }
                    }
                    val at = "from $flags, victory=$victory"
                    assertTrue("$wantFrame $at", wantFrame in seen)
                    assertEquals("one frame scene $at", 1, seen.count { it in frameScenes })
                    assertEquals("one fate flag $at", 1, s.flags.intersect(fate).size)
                    assertEquals("$blade $at", 1, s.has(blade))
                    assertEquals("$notes $at", 1, s.has(notes))
                    assertEquals("$page $at", 1, s.has(page))
                    assertEquals("the frame outcome survives to the end $at", flags.intersect(frame), s.flags.intersect(frame))
                    assertEquals("$lostToIlsevet $at", !victory, lostToIlsevet in s.flags)
                    assertEquals("$lostToGhostwriter $at", !victory, lostToGhostwriter in s.flags)
                    assertEquals("both fights' trophies on a win only $at", victory, setOf("No More Between Us", "Whoever Finished It").all { it in s.trophies })
                    assertEquals("neither trophy on a defeat $at", !victory, setOf("No More Between Us", "Whoever Finished It").none { it in s.trophies })
                }
            }
        }
    }

    // ---- the label tells the truth: the next scene does what the choice said ----

    @Test
    fun `each choice that leads to a new scene is answered by that scene's first words`() {
        val opens = mapOf(
            "c18_after_terms" to "Kaelen accepted,",
            "c18_after_duel" to "Kaelen began to decide,",
            "c19_ashwell_years" to "\"Ashwell was fifteen years ago,\" Ilsevet said.",
            "c19_prisoner_pause" to "The prisoner repeated the phrase",
            "c19_report_seal" to "The report came in a leather tube bearing Selvane's seal.",
            "c20_village_edge" to "Kaelen walked in past the last field wall.",
            "c20_guardian_warning" to "Kaelen kept reading until the loose page lifted",
            "c20_after_guardian" to "When the pressure faded,",
            "c20_sixth_question" to "He took the page and started for home,",
            "c20_alone_mile" to "The first mile alone",
            "c20_with_voss_mile" to "Voss carried the report"
        )
        opens.forEach { (id, start) -> assertTrue("$id opens with: $start", collapse(real.getValue(id).narrativeText).startsWith(start)) }
        // the labels that were corrected say what happens next
        fun labelsTo(from: String, to: String) = real.getValue(from).choices.filter { it.nextNodeId == to }.map { it.label }.toSet()
        assertEquals(setOf("Accept."), labelsTo("her_own_terms_readily", "c18_after_terms") + labelsTo("her_own_terms_pressed", "c18_after_terms"))
        assertEquals(setOf("Ask what came before the drawings."), labelsTo("the_dead_archivist_pressed", "c19_ashwell_years") + labelsTo("the_dead_archivist_patient", "c19_ashwell_years"))
        assertEquals(setOf("Ask about the archivist."), labelsTo("the_prisoners_reaction", "c19_prisoner_pause"))
        assertEquals(setOf("Read Selvane's report."), labelsTo("what_selvane_finds", "c19_report_seal"))
        assertEquals(setOf("Enter the village."), labelsTo("the_road_to_the_workshop_alone", "c20_village_edge") + labelsTo("the_road_to_the_workshop_with_voss", "c20_village_edge"))
        assertEquals(setOf("Keep reading."), labelsTo("what_the_workshop_holds", "c20_guardian_warning"))
        assertEquals(setOf("Check the workbench."), labelsTo("ghostwriter_defeat", "c20_after_guardian"))
        assertEquals(setOf("Take the page, and go home."), labelsTo("the_last_page", "c20_sixth_question"))
        // the choices whose words the player chose are untouched
        assertEquals(setOf("\"I'll go alone. This deserves a direct look, not a delegation.\""), labelsTo("the_workshop", "c20_alone_mile"))
        assertEquals(setOf("\"Voss comes with me. Two sets of eyes, not one.\""), labelsTo("the_workshop", "c20_with_voss_mile"))
        assertEquals(setOf("Rest, while it lasts."), labelsTo("chapter18_end", "c19_fate_captured") + labelsTo("chapter18_end", "c19_fate_mercy") + labelsTo("chapter18_end", "c19_fate_deferred"))
    }

    // ---- the reveals stay where they were ----

    private fun proseOf(id: String) = real.getValue(id).let { n -> collapse(n.narrativeText) + " " + n.title + " " + n.choices.joinToString(" ") { it.label } }

    @Test
    fun `nothing before the original reveals names the drawings, the design or the archivist, or the fifth name, the sixth or the places`() {
        // Ilsevet found the frame design rather than making it: that is first said in what_ilsevet_knew
        val beforeFrameReveal = ch18 + listOf("c19_fate_captured", "c19_fate_mercy", "c19_mercy_meeting", "c19_fate_deferred", "c19_testimony_terms")
        val frameWords = Regex("\\b(drawing|drawings|design|archivist|eleven)\\b", RegexOption.IGNORE_CASE)
        assertEquals("a scene before the frame reveal says what it must not", emptyList<String>(), beforeFrameReveal.filter { frameWords.containsMatchIn(proseOf(it)) })
        // the fifth name is Kaelen's: first said in the_addressed_names
        val between = listOf("c19_report_seal", "c19_dates", "c19_envelopes", "c19_voss_reading", "c19_kaelen_reading", "c19_choice_preparation")
        val nameWords = Regex("\\b(fifth|sixth|Thessaly|Fenmoor|Kaelen's own)\\b", RegexOption.IGNORE_CASE)
        assertEquals("a scene between the report and the names says what it must not", emptyList<String>(), between.filter { nameWords.containsMatchIn(proseOf(it)) })
        // the prisoner's identity is Chapter 21's
        val identity = Regex("one-sixth|six pieces|six prisoners|torn into|was torn|pieces of one|one being|stopped existing", RegexOption.IGNORE_CASE)
        assertEquals("a new scene says what Chapter 21 reveals", emptyList<String>(), newScenes.filter { identity.containsMatchIn(proseOf(it)) })
    }

    // ---- the new scenes: counts, prose, titles and art ----

    private val titles = mapOf(
        "c18_gate_after_ashfall" to "What the Gate Kept",
        "c18_marks" to "Six Lines",
        "c18_quiet_wall" to "What Goes Unasked",
        "c18_stonebeard_report" to "The Account Given",
        "c18_frame_destroyed" to "Broken Settings",
        "c18_frame_disrupted" to "The Pattern Interrupted",
        "c18_frame_uncertain" to "Enough to Stop It",
        "c18_watch" to "The Road Below",
        "c18_voss_warning" to "No More Messengers",
        "c18_voss_silence" to "The Watch Shared",
        "c18_three_days" to "The Third Morning",
        "c18_after_terms" to "The People Between Them",
        "c18_ashwell_question" to "The Name of the Loss",
        "c18_cost_question" to "A List Without an End",
        "c18_before_blades" to "What the Sword Cannot Say",
        "c18_after_duel" to "The Silence After",
        "c18_voss_account" to "Her Own Reckoning",
        "c18_ilsevet_last_question" to "One Thing Unfinished",
        "c18_choice_return" to "The Decision Is His",
        "c19_fate_captured" to "A Room Without a Sword",
        "c19_fate_mercy" to "A Message Left at the Gate",
        "c19_mercy_meeting" to "The Last Meeting",
        "c19_fate_deferred" to "The Answer Still Owed",
        "c19_testimony_terms" to "The Truth Has Conditions",
        "c19_ashwell_years" to "Four Years Before the Plans",
        "c19_the_familiar_hand" to "An Answer Too Convenient",
        "c19_prisoner_pause" to "The Word It Would Not Use",
        "c19_ink" to "A Hand Without a Life",
        "c19_fear" to "What It Recognised",
        "c19_letter" to "The Request for Evidence",
        "c19_report_seal" to "The Report Arrives",
        "c19_dates" to "Before the Role, Not Before the Child",
        "c19_envelopes" to "Five Closed Answers",
        "c19_voss_reading" to "A Name Is Not an Order",
        "c19_kaelen_reading" to "A Hand Over the Page",
        "c19_choice_preparation" to "What Was Written",
        "c19_after_defiance" to "A Line Crossed",
        "c19_after_claim" to "The Name He Keeps",
        "c19_after_silence" to "An Answer Deferred",
        "c20_before_departure" to "The Weight of a Report",
        "c20_alone_mile" to "The Road Without Witnesses",
        "c20_with_voss_mile" to "Two Sets of Eyes",
        "c20_village_edge" to "The Village That Left",
        "c20_register" to "The Last Ordinary Entry",
        "c20_workshop_door" to "The Mark on the Lintel",
        "c20_bench" to "The Work of One Hand",
        "c20_ink_layers" to "Six Years of Impossible Work",
        "c20_personal_effects" to "The Man Before the Pages",
        "c20_recess" to "What Selvane Could Not See",
        "c20_oldest_page" to "Where the Hand Stopped",
        "c20_guardian_warning" to "A Page Moves",
        "c20_after_guardian" to "The Coat on the Floor",
        "c20_final_margin" to "A Hand That Hesitated",
        "c20_sixth_question" to "The Space After Five",
        "c20_homeward" to "The Road Back"
    )

    /** SHA-256 of each scene's whitespace-collapsed text, from the approved package. */
    private val approved = mapOf(
        "c18_gate_after_ashfall" to "972c24da749788d3fc1ddeeb1fb251e1ddfe8bcff5db55a8bcc999a317ef4148",
        "c18_marks" to "54c5a3d9e881c396ba80ef6e794026279765039f10fd420a28ee241a2098c908",
        "c18_quiet_wall" to "0cc68001a4cd738e747ed32ba89bd121f2b3d3255b0cd1579201b38362fda094",
        "c18_stonebeard_report" to "59ae96f9c2fbba28b68e919fa4717ca5de9b9d5f9c7770fb31c1c4931d9e02a9",
        "c18_frame_destroyed" to "193dd227b56855ffb1346cc78bad6a4d240ce14ca9ac3e785d642e52d692220f",
        "c18_frame_disrupted" to "b294139304f05b735b1e1be0d4385457e6a6fd115958273838bb11ded06602b4",
        "c18_frame_uncertain" to "72fcb46562097342e611e325aa079bb81347ec7e063a3c9013520f213de6485d",
        "c18_watch" to "e12604f67f7dc63467c4f9101a267b71234983f43cb0727176dd9ac5409110b0",
        "c18_voss_warning" to "026b4c86c560a816e039a5e9a38fd59e45192ce98d33e6f1220e99dbe1f63ae2",
        "c18_voss_silence" to "6e3d9e4321beec7653b5f97fec63524a282ce01aeacf7a2ec6ca39b50c5a1e86",
        "c18_three_days" to "efa8d964bf9840cdc0176fb6304005e99c49459c2475e1cc18608b141056ae5d",
        "c18_after_terms" to "3d0877790e1f632d3d8488d24fcd869775d539581da823138a885436b4913cef",
        "c18_ashwell_question" to "fa588930797eae3e41609e9c4070e8c18db140d3444d465fa8293accc79b7cca",
        "c18_cost_question" to "26bf596ca82fe1a1ac4d165c68d39e0b07519bf400681b90827f2056a12a5e89",
        "c18_before_blades" to "625a5d23609caae1baa660cbe079a73406655234cb6e8c74274e31ae7cf351a7",
        "c18_after_duel" to "d42503c68cc63ce228c17a0eb0ff0568f9b3cc64e800378769b6c1cc6f792d51",
        "c18_voss_account" to "5249d5acc3e89d7b8896ae51ab390899bab5130fc53fa898f3c2b55f7508d003",
        "c18_ilsevet_last_question" to "d71a2bff155054e681027716ad1fb13743e0937670dac632088e3ee6e5aa65a0",
        "c18_choice_return" to "3b3858b56dee9b71047fbcf3d3f9742163478cf4e85782b2ed3fa208feed14bd",
        "c19_fate_captured" to "90e8cfcba4cafaec8ec122d102990396be73d89962999cf28b268ab8d81f9181",
        "c19_fate_mercy" to "cb9447e2eac53e0a5ba600c8b736fd7ceb2d36935b0739c3e29b3a86f2d8c195",
        "c19_mercy_meeting" to "ccade88e99e4d4f4f81e5481fd6b71b73f02ee0d5c4edd074c00744b19ea49f7",
        "c19_fate_deferred" to "a0cd8421250f3d8d4826d0ce53782e5bf5d5797008898a5ce3b987d76e609c91",
        "c19_testimony_terms" to "e1def2a2457768901f87b2d3fe539a01c930f813a197689055d2bced6eddfbe8",
        "c19_ashwell_years" to "0139dbaa5b116a4016cda5e672698b1ee2cb65fa8a61ab7fd301ffa0a9c6b4e4",
        "c19_the_familiar_hand" to "2aca4cb2fab275fb7ac3ad62e617f94508174a8d3986a98477e1efb77ce9ce7f",
        "c19_prisoner_pause" to "9c765560c7b86822d6dc54e9d4cc884a347d793c28ae5e72eb012c97665c3184",
        "c19_ink" to "27c0875fbadf02982358a35ce9d9e2612797df0d2c4034333889489c159ba5d2",
        "c19_fear" to "c7573248ddd33736e1fa3789fdfdc10b72c30b4cd37719121e0cfed1501383bc",
        "c19_letter" to "a0c2a4edf73d35b8bcd6f87bb23067400a2f983e22b9375ba6a9e41e9e71623d",
        "c19_report_seal" to "4ed06eeab6df233786f4a07d135a30c38b24c4482f60f583113f7d82e5d1eb0d",
        "c19_dates" to "b0b77411bc3b7105c623e0201fe7cf18d0bf05c747c10fb49c3b72e362621d1f",
        "c19_envelopes" to "e1bf752751e7c520cb3b78fce54baa0e7b7e182ef242838df671d95567000307",
        "c19_voss_reading" to "5a006e61d41578e22df281593048af28ec54b58cc580c2338ab696fb72104db5",
        "c19_kaelen_reading" to "5286068a03191d329fb1321b773754b8ae63e493e4847d9681bdb7451e2dcbd8",
        "c19_choice_preparation" to "82bf55ca98c558152c65058bf0c3e83cd13ae62c144e1ed1ecaed618a753d5a5",
        "c19_after_defiance" to "c8eb8e22d5e6fb5ecc29a0764c530606e3f864ace7dcba3f0dcd65c9361b38a9",
        "c19_after_claim" to "bfc5420069f2054e2a4970cea152c74551cb4fd2309e878c723b7e5d97d9cddc",
        "c19_after_silence" to "2de0aa86115e1279922d3c16a7b9b8d3db30776e38b301f86250c4218125ee54",
        "c20_before_departure" to "48f6fd0ce3676776555d613cc26e7a1a20e331561151806fa54c7e541829d99d",
        "c20_alone_mile" to "491c94360a916ff4d4f66a30f0f0887645ef25b9a424e636b065c23111785ff6",
        "c20_with_voss_mile" to "cc29c5b83d810a2989e981185943b448b6b6b6e0ee8e395e539f25148673ee1c",
        "c20_village_edge" to "013ede24e6daa9662480e92b0ef2928f4ca9a132eaecf8c63974159104626798",
        "c20_register" to "310a1fdebccb4d6ef037005e6bcdae7b5e8bb181263e241d0eca23496718f7cf",
        "c20_workshop_door" to "64093cd4ab14b46e30663ff2d9e9374eed6f78cf5b265550f03d4ec38f954425",
        "c20_bench" to "89419dd3f780cf1910d306876a6fb5987f6b91c7a48d4997dffa8906512e529b",
        "c20_ink_layers" to "33fea8a719656b146602384a7157b5abd69d624c18de32fa23ac28d1dfa83571",
        "c20_personal_effects" to "06865f50729afe356c7e7df8b70704dd35ef9330564b64a776caa2e4796ee45f",
        "c20_recess" to "72a7119b4cf84527f1232b929187157080454a8ebd501d1d8bf637a4defdf8b3",
        "c20_oldest_page" to "b0359529797db82f588b1e415d36012dbe09bd708bede72e1c35c6b957b99688",
        "c20_guardian_warning" to "3e9548c1fd30d00a646e67cf656ca84c5c40190ccd845e8b17957b9505f80234",
        "c20_after_guardian" to "a90b2a9ad14379b40c2eae067c7dcbab82a49b912549cdf1a7eae405e6c32be2",
        "c20_final_margin" to "18b7634d16fc52a4d20fd72a8fac69ff945871fab7b360c7be5113f8ff5efc92",
        "c20_sixth_question" to "b4ee982db4164cc4cf859b79ca19a9723ff36e821674b31a53b9055e7b7ea40e",
        "c20_homeward" to "672b13ea8633fbdbf9486fa6c2c32e2f839fab935a51440c9e2b86e126500224"
    )

    private val art = mapOf(
        "c18_gate_after_ashfall" to "dwarven_hold_gate",
        "c18_marks" to "dwarven_hold_gate",
        "c18_quiet_wall" to "dwarven_hold_gate",
        "c18_stonebeard_report" to "who_stands_with_stonebeard",
        "c18_frame_destroyed" to "who_stands_with_stonebeard",
        "c18_frame_disrupted" to "who_stands_with_stonebeard",
        "c18_frame_uncertain" to "who_stands_with_stonebeard",
        "c18_watch" to "ashfall_sanctum_vista",
        "c18_voss_warning" to "ashfall_sanctum_vista",
        "c18_voss_silence" to "ashfall_sanctum_vista",
        "c18_three_days" to "no_more_envoys",
        "c18_after_terms" to "her_own_terms",
        "c18_ashwell_question" to "her_own_terms",
        "c18_cost_question" to "her_own_terms",
        "c18_before_blades" to "her_own_terms",
        "c18_after_duel" to "what_is_left_of_her",
        "c18_voss_account" to "what_is_left_of_her",
        "c18_ilsevet_last_question" to "what_is_left_of_her",
        "c18_choice_return" to "what_is_left_of_her",
        "c19_fate_captured" to "what_ilsevet_knew",
        "c19_fate_mercy" to "what_ilsevet_knew",
        "c19_mercy_meeting" to "what_ilsevet_knew",
        "c19_fate_deferred" to "what_ilsevet_knew",
        "c19_testimony_terms" to "what_ilsevet_knew",
        "c19_ashwell_years" to "what_ilsevet_knew",
        "c19_the_familiar_hand" to "what_ilsevet_knew",
        "c19_prisoner_pause" to "the_prisoners_reaction",
        "c19_ink" to "the_prisoners_reaction",
        "c19_fear" to "the_prisoners_reaction",
        "c19_letter" to "what_selvane_finds",
        "c19_report_seal" to "what_selvane_finds",
        "c19_dates" to "what_selvane_finds",
        "c19_envelopes" to "what_selvane_finds",
        "c19_voss_reading" to "what_selvane_finds",
        "c19_kaelen_reading" to "what_selvane_finds",
        "c19_choice_preparation" to "what_selvane_finds",
        "c19_after_defiance" to "the_addressed_names",
        "c19_after_claim" to "the_addressed_names",
        "c19_after_silence" to "the_addressed_names",
        "c20_before_departure" to "black_door_beneath_the_tree",
        "c20_alone_mile" to "the_road_to_the_workshop",
        "c20_with_voss_mile" to "the_road_to_the_workshop",
        "c20_village_edge" to "the_road_to_the_workshop",
        "c20_register" to "the_road_to_the_workshop",
        "c20_workshop_door" to "the_road_to_the_workshop",
        "c20_bench" to "what_the_workshop_holds",
        "c20_ink_layers" to "what_the_workshop_holds",
        "c20_personal_effects" to "what_the_workshop_holds",
        "c20_recess" to "what_the_workshop_holds",
        "c20_oldest_page" to "what_the_workshop_holds",
        "c20_guardian_warning" to "the_workshops_own_guardian",
        "c20_after_guardian" to "the_workshops_own_guardian",
        "c20_final_margin" to "the_last_page",
        "c20_sixth_question" to "the_last_page",
        "c20_homeward" to "the_road_to_the_workshop"
    )

    @Test
    fun `55 scenes are added, 19 to Chapter 18, 20 to Chapter 19 and 16 to Chapter 20, and the three held scenes are not`() {
        assertEquals(19, ch18.size)
        assertEquals(20, ch19.size)
        assertEquals(16, ch20.size)
        assertEquals(55, newScenes.size)
        assertEquals("no id twice", 55, newScenes.toSet().size)
        listOf("chapter_18" to ch18, "chapter_19" to ch19, "chapter_20" to ch20).forEach { (chapter, ids) ->
            ids.forEach { assertEquals("$it chapter", chapter, real.getValue(it).chapterId) }
        }
        assertEquals(30, StoryRepository.nodesInChapter("chapter_18").size)
        assertEquals(29, StoryRepository.nodesInChapter("chapter_19").size)
        assertEquals(27, StoryRepository.nodesInChapter("chapter_20").size)
        held.forEach { assertNull("$it is held out of this batch", StoryRepository.nodeOrNull(it)) }
        assertEquals("no id twice in the whole story", real.size, StoryRepository.chapters.sumOf { StoryRepository.nodesInChapter(it.id).size })
    }

    @Test
    fun `the approved prose and titles are word for word, with art that exists`() {
        assertEquals(newScenes.toSet(), approved.keys)
        assertEquals(newScenes.toSet(), titles.keys)
        assertEquals(newScenes.toSet(), art.keys)
        newScenes.forEach { id ->
            val n = real.getValue(id)
            assertEquals("$id title", titles.getValue(id), n.title)
            assertEquals("$id prose", approved.getValue(id), sha(n.narrativeText))
            assertEquals("$id art", art.getValue(id), n.illustrationId)
            assertNotNull("$id art does not exist", IllustrationCatalog.get(n.illustrationId))
            assertNull("$id has a fight", n.combatEncounterId)
        }
    }

    @Test
    fun `the new scenes set no flag, change no stat, grant no item, unlock no trophy and clear nothing`() {
        newScenes.forEach { id ->
            real.getValue(id).choices.forEach { c ->
                assertEquals("$id: '${c.label}' has an effect", Consequences(), c.consequences)
                assertTrue("$id: '${c.label}' has a stat gate", c.requirements?.minStats.isNullOrEmpty())
                assertEquals("$id: '${c.label}' shows when locked", com.thelastjailer.app.LockedDisplay.HIDE, c.lockedDisplay)
            }
        }
    }

    @Test
    fun `Chapters 18 to 20 set, clear, read and grant what they did before, plus the frame and the rung they now read`() {
        fun parts(s: String) = s.split(",").filter { it.isNotEmpty() }
        val shippedChoices = Chapter1820Shipped.scenes.flatMap { it.choices }
        val setBefore = shippedChoices.flatMap { parts(it.set) }.toSet()
        val clearBefore = shippedChoices.flatMap { parts(it.clear) }.toSet()
        val readBefore = shippedChoices.flatMap { parts(it.required) + parts(it.forbidden) }.toSet()
        val choices = inChapters.flatMap { it.choices }
        assertEquals("flags set", setBefore, choices.flatMap { it.consequences.setFlags }.toSet())
        assertEquals("flags cleared", clearBefore, choices.flatMap { it.consequences.clearFlags }.toSet())
        val readNow = choices.flatMap { (it.requirements?.requiredFlags ?: emptySet()) + (it.requirements?.forbiddenFlags ?: emptySet()) }.toSet()
        // the third frame copy and the third fate copy are read by the absence of the others, so $stopped and $deferred are never read themselves
        assertEquals("flags newly read: the frame outcomes tested, the trusted rung and the first two fates", setOf(destroyed, disrupted, trusted, captured, mercy), readNow - readBefore)
        assertTrue("$stopped is read by absence only", stopped !in readNow && deferred !in readNow)
        println("READS no longer read in Chapters 18 to 20: ${readBefore - readNow}")
        val grantsBefore = shippedChoices.flatMap { parts(it.items) }.sorted()
        assertEquals("items granted", grantsBefore, choices.flatMap { it.consequences.grantItemIds }.sorted())
        val trophiesBefore = shippedChoices.map { it.trophy }.filter { it.isNotEmpty() }.sorted()
        assertEquals("trophies unlocked by choices", trophiesBefore, choices.mapNotNull { it.consequences.unlockTrophy }.sorted())
    }

    // ---- house style ----

    @Test
    fun `the new scenes keep the house style`() {
        val prose = newScenes.flatMap { id -> real.getValue(id).let { n -> listOf(id to collapse(n.narrativeText), id to n.title) + n.choices.map { id to it.label } } }
        fun hits(terms: List<String>) = prose.flatMap { (id, t) -> terms.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" } }
        assertEquals("dashes", emptyList<String>(), prose.filter { (_, t) -> t.contains('—') || t.contains('–') }.map { it.first }.distinct())
        assertEquals("asterisks, which the engine does not render", emptyList<String>(), prose.filter { (_, t) -> t.contains('*') || t.contains('_') }.map { it.first }.distinct())
        assertEquals("non-ASCII characters", emptyList<String>(), prose.filter { (_, t) -> t.any { it.code > 127 } }.map { it.first }.distinct())
        val fragment = Regex("(^|[.!?][\"']?\\s)Not\\s")
        assertEquals("Not-fragments", emptyList<String>(), prose.filter { (_, t) -> fragment.containsMatchIn(t) }.map { it.first }.distinct())
        val person = Regex("\\bStonebeard (listened|asked|nodded|looked|accepted|wrote)|Stonebeard's pen")
        assertEquals("Stonebeard is the Hold, not a person", emptyList<String>(), prose.filter { (_, t) -> person.containsMatchIn(t) }.map { it.first }.distinct())
        val he = Regex("\\b(he|him|his|himself)\\b")
        val others = Regex("Kaelen|Voss|Selvane|Ilsevet|Stonebeard")
        val tam = prose.flatMap { (id, t) -> t.split(Regex("(?<=[.!?])\\s+")).filter { it.startsWith("Tam") && he.containsMatchIn(it) && !others.containsMatchIn(it) }.map { id } }
        assertEquals("Tam is a woman", emptyList<String>(), tam.distinct())
        // the dead archivist's glove is his; Kaelen wears a bracer
        val ownGlove = Regex("\\b(Kaelen's|his|my|your)\\s+(glove|gloves|gauntlet|gauntlets)\\b|\\bgloved\\b|\\bgauntlet", RegexOption.IGNORE_CASE)
        assertEquals("a glove where Kaelen wears a bracer", emptyList<String>(), prose.filter { (_, t) -> ownGlove.containsMatchIn(t) }.map { it.first }.distinct())
        assertEquals("the old man is the old smith", emptyList<String>(), hits(listOf("Stonebeard's smith", "the old man")))
        val counts = listOf("three riders", "four riders")
        assertEquals("counts the canon rules out", emptyList<String>(), hits(counts))
        // fifteen years is the time since Ashwell (Chapter XV); the frame's design is eleven years old (E5), so fifteen may only count from Ashwell
        val fifteen = prose.flatMap { (id, t) -> t.split(Regex("(?<=[.!?])\\s+")).filter { Regex("\\bfifteen years\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) && !it.contains("Ashwell") }.map { id } }
        assertEquals("fifteen years that are not counted from Ashwell", emptyList<String>(), fifteen.distinct())
        val agreed = Regex("prisoner[^.]{0,40}agreed|agreed[^.]{0,40}prisoner", RegexOption.IGNORE_CASE)
        assertEquals("the prisoner never agreed to be torn", emptyList<String>(), prose.filter { (_, t) -> agreed.containsMatchIn(t) }.map { it.first }.distinct())
        newScenes.forEach { id -> assertTrue("$id is short", collapse(real.getValue(id).narrativeText).split(" ").size >= 25) }
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
    fun `the walk notices a wrong frame scene, a wrong Voss, two fate flags, a lost item and a frame flag cleared in passing`() {
        assertEquals("the real scenes are clean", emptyList<String>(), problemsOf(real))
        noticed("the third frame copy shown beside another", mutated("c18_stonebeard_report") { it.changing(2) { c -> c.copy(requirements = null) } }, "frame scene")
        noticed("the disrupted copy that ignores the destroyed flag", mutated("c18_stonebeard_report") { it.changing(1) { c -> c.copy(requirements = c.requirements?.copy(forbiddenFlags = emptySet())) } }, "frame scene")
        noticed("a decision that names the guarded Voss for the trusted rung", mutated("c18_choice_return") { it.changing(0) { c -> c.copy(nextNodeId = "what_kaelen_chooses_guarded") } }, "Voss scene")
        noticed("a fate choice that does not clear the others", mutated("what_kaelen_chooses_trusted") {
            it.changing(0) { c -> c.copy(consequences = c.consequences.copy(clearFlags = emptySet())) }
        }, "fate flags")
        noticed("a blade granted twice", mutated("c18_after_duel") { it.changing(0) { c -> c.copy(consequences = Consequences(grantItemIds = listOf(blade))) } }, "$blade held")
        noticed("a frame flag cleared by an ordinary continuation", mutated("c18_marks") { it.changing(0) { c -> c.copy(consequences = Consequences(clearFlags = frame)) } }, "frame flags changed")
        noticed("a frame flag set by an ordinary continuation", mutated("c18_quiet_wall") { it.changing(0) { c -> c.copy(consequences = Consequences(setFlags = setOf(destroyed))) } }, "frame")
    }

    @Test
    fun `the walk notices a loop, a dead end, a skipped scene, a wrong morning after and an item or trophy on the wrong outcome`() {
        noticed("a loop", mutated("c18_voss_silence") { it.changing(0) { c -> c.copy(nextNodeId = "c18_watch") } }, "loop")
        noticed("a dead end", mutated("c19_report_seal") { it.copy(choices = emptyList()) }, "dead end")
        noticed("a skipped terms scene", mutated("her_own_terms_readily") { it.changing(0) { c -> c.copy(nextNodeId = "c18_before_blades") } }, "c18_after_terms")
        noticed("a skipped decision scene", mutated("c18_voss_account") { it.changing(1) { c -> c.copy(nextNodeId = "what_kaelen_chooses_guarded") } }, "Voss scene")
        noticed("the third morning shown with the first", mutated("chapter18_end") { it.changing(2) { c -> c.copy(requirements = null) } }, "the morning after")
        noticed("a skipped mile", mutated("the_workshop") { it.changing(0) { c -> c.copy(nextNodeId = "the_road_to_the_workshop_alone") } }, "the_workshop does not lead to")
        noticed("a defeat that skips what is left", mutated("ghostwriter_defeat") { it.changing(0) { c -> c.copy(nextNodeId = "the_last_page") } }, "c20_after_guardian")
        noticed("a last page that grants nothing", mutated("the_last_page") { it.changing(0) { c -> c.copy(consequences = Consequences()) } }, "$page held")
        noticed("a defeat flag set on a win", mutated("c20_after_guardian") { it.changing(0) { c -> c.copy(consequences = Consequences(setFlags = setOf(lostToGhostwriter))) } }, lostToGhostwriter)
        noticed("an answer that does not set its flag", mutated("the_addressed_names") { it.changing(2) { c -> c.copy(consequences = Consequences()) } }, "answer flags")
        noticed("a new scene that can never be reached", mutated("c20_village_edge") { it.copy(choices = it.choices.drop(1)) }, "c20_register is never reached")
    }

}
