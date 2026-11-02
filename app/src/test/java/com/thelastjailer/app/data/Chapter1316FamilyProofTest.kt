package com.thelastjailer.app.data

import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.data.FlagFamilyProbe.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Milestone 0 of the Chapters 13 to 16 package. The new scenes read families of flags as alternatives, which is true only if
 * the story makes them so on every road. Three kinds of evidence, each sound for what it claims (see [FlagFamilyProbe]):
 *  - the structure of the choice graph proves a family is never held two at a time and is held at least once on every road;
 *  - real walks of the real transitions witness each member, each combination the new scenes depend on, and the inventories a
 *    player can arrive with, and never witness a combination the structure forbids;
 *  - the exact search is not used here: with thirty flags retained for the whole story it overflows its budget (verified at M0).
 */
class Chapter1316FamilyProofTest {

    private class Family(val name: String, val flags: List<String>, val at: String, val exhaustive: Boolean?)

    private val families = listOf(
        Family("stance", listOf("stood_with_voss", "let_voss_go_alone"), "selvanes_verdict", true),
        Family("vouch", listOf("kaelen_vouched_for_voss", "trusted_voss_account"), "selvanes_verdict", true),
        Family("greymoor", listOf("sealed_greymoor_personally", "reported_greymoor_honestly", "left_greymoor_as_is"), "selvanes_verdict", true),
        Family("called_off", listOf("c7_voss_called_them_off"), "selvanes_verdict", false),
        Family("seal", listOf("c8_exit_tithe", "c9_door_seal"), "selvanes_verdict", false),
        Family("rung", listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted"), "selvanes_verdict", null),
        Family("cover", listOf("c10_cover_voss", "c10_cover_stair", "c10_cover_forge"), "selvanes_verdict", null),
        Family("rites", listOf("c10_rites_name", "c10_rites_deeds", "c10_rites_plain"), "selvanes_verdict", false),
        Family("hold", listOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden"), "selvanes_verdict", false),
        Family("thessaly", listOf("pledged_support_to_fenmoor", "invited_thessaly_to_stonebeard", "left_fenmoor_uncertain"), "selvanes_verdict", true),
        Family("knock", listOf("listened_to_the_prisoner", "refused_the_prisoner"), "the_names_it_wont_give", true),
        Family("office", listOf("accepted_fully", "accepted_reluctantly", "refused_once"), "the_names_it_wont_give", true),
        Family("aid", listOf("accepted_dark_aid", "refused_dark_aid_again"), "the_names_it_wont_give", true),
        Family("keepers", listOf("began_training_new_wardens", "stood_watch_rather_than_delegate", "deferred_the_question"), "what_waits_speaks", true),
        Family("vision", listOf("told_voss_everything", "recorded_the_vision_first", "kept_the_vision_to_himself"), "chapter14_end", true),
        Family("ilsevet_help", listOf("chose_to_help_ilsevet", "refused_to_help_ilsevet"), "what_it_made_of_her", true),
        Family(
            "ilsevet_way",
            listOf("warned_ilsevet_directly", "appealed_to_ilsevets_conscience", "left_ilsevet_to_her_wreckage", "refused_to_help_ilsevet"),
            "what_it_made_of_her", true
        ),
        Family("shards", listOf("kept_shards_close", "dispersed_the_shards", "trusted_to_luck"), "what_was_taken", true)
    )

    /** Combinations the new scenes depend on, each of which a real walk must be able to stand in. */
    private val needed: Map<String, List<Question>> = mapOf(
        "selvanes_verdict" to listOf(
            Question("seal:neither", forbidden = setOf("c8_exit_tithe", "c9_door_seal")),
            Question("seal:tithe_with_door_force", setOf("c8_exit_tithe", "c9_door_force")),
            Question("seal:tithe_with_nails", setOf("c8_exit_tithe", "c9_door_nails")),
            Question("tam:terms", setOf("c6_met_tam", "c7_hold_terms")),
            Question("tam:refused", setOf("c6_met_tam", "c7_hold_refused")),
            Question("tam:hidden", setOf("c6_met_tam", "c7_hold_hidden")),
            Question("tam:no_promise", setOf("c6_met_tam"), setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")),
            Question("chapel:trusted", setOf("c8_chapel_story", "c7_rung_trusted")),
            Question("chapel:not_trusted", setOf("c8_chapel_story"), setOf("c7_rung_trusted")),
            Question("nochapel:trusted", setOf("c7_rung_trusted"), setOf("c8_chapel_story")),
            Question("rung:wary_by_flag", setOf("c7_rung_wary")),
            Question("rung:wary_by_absence", forbidden = setOf("c7_rung_working", "c7_rung_trusted")),
            Question("called_off:with_seal_door", setOf("c7_voss_called_them_off", "c9_door_seal"))
        ),
        "what_kaelen_remembers" to listOf(
            Question("rung:wary", forbidden = setOf("c7_rung_working", "c7_rung_trusted")),
            Question("rung:working", setOf("c7_rung_working")),
            Question("rung:trusted", setOf("c7_rung_trusted"))
        ),
        "what_it_made_of_her" to listOf(
            Question("rung:wary", forbidden = setOf("c7_rung_working", "c7_rung_trusted")),
            Question("rung:working", setOf("c7_rung_working")),
            Question("rung:trusted", setOf("c7_rung_trusted"))
        ),
        "what_to_protect" to listOf(
            Question("rung:wary", forbidden = setOf("c7_rung_working", "c7_rung_trusted")),
            Question("rung:working", setOf("c7_rung_working")),
            Question("rung:trusted", setOf("c7_rung_trusted")),
            Question("ilsevet:helped", setOf("chose_to_help_ilsevet")),
            Question("ilsevet:refused", setOf("refused_to_help_ilsevet"))
        )
    )

    private val shardItems = listOf("item:fenmoor_ward_shard", "item:shard_of_the_seventh_door", "item:shard_of_the_first_ward")

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
        ).also { it.run(WALKS, seed = 0x1316L); walker = it }
    }

    private val universe: Set<String> =
        families.flatMap { it.flags }.toSet() + needed.values.flatten().flatMap { it.required + it.forbidden } + shardItems +
            setOf("c8_chapel_story", "c6_met_tam")
    private val targets: Set<String> = families.map { it.at }.toSet() + needed.keys

    private val report = StringBuilder()
    private fun say(s: String) { println(s); report.appendLine(s) }

    // ---- structure: exclusive and exhaustive, on every road ----

    @Test
    fun `by the structure of the story every family is held at most one at a time, and the exhaustive ones at least once on every road`() {
        val failures = ArrayList<String>()
        families.forEach { f ->
            val p = Evidence.graph.prove(f.flags.toSet(), f.at)
            say("STRUCTURE ${f.name} @${f.at}: exclusive=${p.exclusive} exhaustive=${p.exhaustive} ${p.notes}")
            if (!p.exclusive) failures += "${f.name}: not exclusive ${p.notes}"
            if (f.exhaustive == true && !p.exhaustive) failures += "${f.name}: not exhaustive ${p.notes}"
        }
        assertTrue("family assumptions that the structure does not support: $failures", failures.isEmpty())
    }

    // ---- reachable states: every member, every combination the new scenes depend on, and nothing the structure forbids ----

    @Test
    fun `real walks stand in every member of every family and every combination the new scenes depend on`() {
        val w = Evidence.walked(universe, targets)
        say("WALKS ${w.walksFinished} walks, ${w.stepsTaken} steps; projections seen: " + targets.sorted().joinToString { "$it=${w.seen[it]?.size ?: 0}" })
        val failures = ArrayList<String>()
        families.forEach { f ->
            val qs = FlagFamilyProbe.familyQuestions(f.name, f.flags)
            val line = qs.filter { w.witnessed(f.at, it) }.map { it.name.removePrefix(f.name + ":") }
            say("WITNESSED ${f.name} @${f.at}: $line")
            f.flags.forEach { m -> if (!w.witnessed(f.at, Question("m", setOf(m), f.flags.toSet() - m))) failures += "${f.name}: $m alone was never witnessed" }
            val none = w.witnessed(f.at, Question("none", forbidden = f.flags.toSet()))
            when (f.exhaustive) {
                true -> if (none) failures += "${f.name}: a walk stood at ${f.at} holding none of ${f.flags}"
                false -> if (!none) failures += "${f.name}: no walk stood at ${f.at} holding none of ${f.flags}"
                null -> say("  ${f.name}: 'none' witnessed = $none")
            }
            qs.filter { it.name.contains(":both:") }.forEach { q -> if (w.witnessed(f.at, q)) failures += "${f.name}: ${q.name} was witnessed together" }
        }
        needed.forEach { (at, qs) -> qs.forEach { q ->
            val ok = w.witnessed(at, q)
            say("NEEDED @$at ${q.name}: $ok")
            if (!ok) failures += "@$at ${q.name} never witnessed"
        } }
        assertTrue("assumptions that real walks do not bear out: $failures", failures.isEmpty())
    }

    // ---- Chapter 6 to Chapter 13: Edda's people are met only where Tam was met ----

    @Test
    fun `a promise to Edda is only ever made after Tam is met, so wherever Edda's people are Tam is`() {
        val g = Evidence.graph
        val holdSetters = g.settersOf(setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden"))
        assertTrue(holdSetters.isNotEmpty())
        holdSetters.forEach { s ->
            assertTrue("${s.from}: the hold is decided only on a road that carries the promise", g.onlyEnteredRequiring(s.from, "c6_offered_the_hold"))
        }
        val metTam = g.settersOf(setOf("c6_met_tam")).toSet()
        g.settersOf(setOf("c6_offered_the_hold")).forEach { p ->
            assertTrue("${p.from}: Tam is met on every road before the promise is made", g.mustPass(metTam, p.from))
        }
        listOf("c6_met_tam", "c6_offered_the_hold").forEach { f ->
            val setters = g.settersOf(setOf(f))
            g.clearersOf(f).forEach { c ->
                setters.forEach { s -> assertTrue("$f is cleared at ${c.from}#${c.index} after it can be set at ${s.from}", c.from !in g.reach(listOf(s.to))) }
            }
        }
        say("TAM every road that reaches a decision about Edda's people has met Tam; ${holdSetters.size} choices decide it")
    }

    // ---- Chapter 12 to Chapter 15: a helped Ilsevet took one of three ways, and only then ----

    @Test
    fun `a helped Ilsevet took exactly one of three ways, and the shard of the seventh door goes with helping`() {
        val g = Evidence.graph
        val helpSetters = g.settersOf(setOf("chose_to_help_ilsevet")).toSet()
        val ways = g.settersOf(setOf("warned_ilsevet_directly", "appealed_to_ilsevets_conscience", "left_ilsevet_to_her_wreckage"))
        val seventh = g.grantorsOf("shard_of_the_seventh_door")
        say("WAYS ${ways.size} choices set a way, ${helpSetters.size} choices set the helping flag, ${seventh.size} choices grant the seventh shard")
        ways.forEach { w -> assertTrue("${w.from}#${w.index}: a way is taken only after helping", g.mustPass(helpSetters, w.from)) }
        helpSetters.forEach { h ->
            assertTrue("${h.from}#${h.index}: after helping, every road to the scene passes one of the three ways", g.mustPass(ways.toSet(), "what_it_made_of_her", from = h.to))
        }
        seventh.forEach { s -> assertTrue("${s.from}#${s.index}: the seventh shard is granted only after helping", g.mustPass(helpSetters, s.from)) }
        helpSetters.forEach { h ->
            assertTrue("${h.from}#${h.index}: after helping, every road to Chapter 16 passes the grant of the seventh shard", g.mustPass(seventh.toSet(), "what_to_protect", from = h.to))
        }
    }

    // ---- Chapter 16: what a player can be carrying ----

    @Test
    fun `a player arrives at Chapter 16 holding two shards or three, never fewer, never four`() {
        val g = Evidence.graph
        val shardLike = Evidence.nodes.flatMap { n -> n.choices.flatMap { it.consequences.grantItemIds } }.toSet()
            .filter { it.contains("shard") || it.contains("fragment") }.toSet()
        assertEquals("the only items that are shards or fragments", shardItems.map { it.removePrefix("item:") }.toSet(), shardLike)
        // the Fenmoor shard and the First Ward shard are granted on every road; the Seventh Door shard only after helping Ilsevet (previous test)
        listOf("fenmoor_ward_shard", "shard_of_the_first_ward").forEach { item ->
            assertTrue("$item is granted on every road to Chapter 16", g.mustPass(g.grantorsOf(item).toSet(), "what_to_protect"))
        }
        val w = Evidence.walked(universe, targets)
        val inventories = w.seen.getValue("what_to_protect").map { held -> held.filter { it in shardItems }.toSet() }.toSet()
        say("INVENTORIES at what_to_protect: " + inventories.map { it.map { i -> i.removePrefix("item:") }.sorted() })
        assertEquals(
            setOf(
                setOf("item:fenmoor_ward_shard", "item:shard_of_the_first_ward"),
                setOf("item:fenmoor_ward_shard", "item:shard_of_the_seventh_door", "item:shard_of_the_first_ward")
            ),
            inventories
        )
    }

    // ---- the Journal's headroom, as the package finds it ----

    @Test
    fun `report the Journal headroom`() {
        val storyFlags = Evidence.nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        val described = FlagDescriptions.entries.map { it.flag }.toSet()
        val hidden = FlagDescriptions.hidden.keys
        val visible = storyFlags.intersect(described).size
        val hiddenCount = storyFlags.intersect(hidden).size
        say(
            "JOURNAL story flags=${storyFlags.size} visible=$visible (allowed ${storyFlags.size / 2 + 20}, headroom ${storyFlags.size / 2 + 20 - visible}) " +
                "hidden=$hiddenCount (needed at least ${storyFlags.size / 3}, spare ${hiddenCount - storyFlags.size / 3})"
        )
        say("JOURNAL with 3 more flags the allowance is ${(storyFlags.size + 3) / 2 + 20}")
    }
}
