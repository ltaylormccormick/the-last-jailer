package com.thelastjailer.app.data

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.resolveCombat
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The six chapters as one package, from the three endings of Chapter 6 to the first scene of Chapter 13. The per-chapter tests
 * pin each chapter; these hold the seams between them and the property a replay depends on: that a second telling, begun with
 * every flag the first one left behind, goes exactly where a first telling with the same decisions goes. Chapter 79PackageTest
 * does the same up to Chapter 10's first scene and is unchanged.
 */
class Chapter1012PackageTest {

    private fun isGoal(s: GameState) = s.sceneId.startsWith("the_orders_delegation")

    /** Every flag any choice in Chapters 7 to 12 can set, and the hub markers and the ladder: what a second telling must not inherit. */
    private val owned: Set<String> = (7..12).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
        .flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet() + HUB_MARKERS_FOUR + C7_RUNG_FAMILY

    private fun base(random: Random): Set<String> {
        val flags = HashSet<String>()
        when (random.nextInt(3)) {
            0 -> { flags += "kept_the_offer_from_halvard"; flags += "c6_halvard_reticent" }
            1 -> flags += "kept_the_offer_from_halvard"
            else -> if (random.nextBoolean()) flags += "c6_halvard_confided"
        }
        if (random.nextBoolean()) flags += "c6_took_cairn_note"
        if (random.nextBoolean()) flags += "c6_offered_the_hold"
        if (random.nextBoolean()) flags += "c6_has_ward_nails"
        if (random.nextBoolean()) flags += "c6_met_tam"
        when (random.nextInt(4)) {
            0 -> flags += "refused_envoy"
            1 -> flags += "heard_envoy_offer"
            2 -> flags += "confronted_envoy_with_prisoners_words"
        }
        return flags
    }

    private fun mix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    private fun score(c: com.thelastjailer.app.Choice, seed: Int): Long =
        mix((c.label.hashCode().toLong() shl 32) xor c.nextNodeId.hashCode().toLong() xor mix(seed.toLong()))

    /** The scenes that leave the most behind, so that a first telling sets as many flags as it can. */
    private val rare = listOf(
        "c7_called_off", "c7_trade_made", "c7_letter_page", "c7_hold_terms", "c7_hold_hearth", "c7_wound_tended",
        "c8_deserter_paid", "c8_chapel_door", "c8_cart_watched", "c8_cart_robbed", "c8_capture_gate", "c8_night_farmers",
        "c9_plans", "c9_stores_kit", "c9_stair_people", "c9_gate_braced", "c9_in_by_cart_farmers", "c9_door_nails",
        "c9_door_force", "c9_voss_chapel", "c9_north_stair", "c9_stores", "c9_door_seal", "c9_in_by_stair",
        "c10_forge_gap", "c10_wall_stay", "c10_wall_hurt", "c10_page", "c10_tam", "c10_carter", "c10_forge_side",
        "c11_tithe_route", "c11_cut_them_off", "c12_way_stair", "c12_way_wall", "c12_way_cart"
    )

    private fun play(start: GameState, seed: Int, prefer: Set<String> = emptySet()): Pair<List<String>, GameState> {
        var s = start
        val path = ArrayList<String>()
        var steps = 0
        while (!isGoal(s)) {
            check(steps++ < 320) { "never finished; stuck at ${s.sceneId} (seed $seed)" }
            val node = StoryRepository.node(s.sceneId)
            path += node.id
            s = if (node.combatEncounterId != null) {
                val win = (mix(node.id.hashCode().toLong() xor mix(seed.toLong())) and 1L) == 0L
                s.resolveCombat(CombatRepository.encounter(node.combatEncounterId!!), CombatOutcome(victory = win, damageTaken = 0))
            } else {
                val choices = StoryRepository.visibleChoices(node, s)
                check(choices.isNotEmpty()) { "dead end at ${node.id} with ${s.flags} (seed $seed)" }
                val wanted = choices.firstOrNull { it.nextNodeId in prefer }
                s.applyChoice(wanted ?: choices.maxByOrNull { score(it, seed) }!!)
            }
        }
        return path to s
    }

    private fun starts(random: Random): GameState = at(
        listOf("chapter6_end_sealed", "chapter6_end_reported", "chapter6_end_left")[random.nextInt(3)],
        flags = base(random), courage = 1 + random.nextInt(10), honour = random.nextInt(9),
        gold = random.nextInt(90), health = 200 + random.nextInt(300)
    )

    @Test
    fun `every ending of Chapter 6 reaches Chapter 13 through all six chapters by a fixed policy`() {
        val random = Random(0x1012L)
        repeat(300) { run ->
            val (path, end) = play(starts(random), run)
            listOf("c7_", "c8_", "c9_", "c10_", "c11_").forEach { p -> assertTrue("run $run read no $p scene", path.any { it.startsWith(p) }) }
            assertTrue("run $run: the fall and the last words", "halvard_falls" in path && "what_halvard_leaves_behind" in path)
            assertTrue("run $run: the death flags", end.flags.containsAll(setOf("halvard_died", "took_the_warden_chain")))
            assertTrue("run $run", isGoal(end))
        }
    }

    @Test
    fun `a second telling begun with everything the first left behind goes exactly where a first telling goes`() {
        val random = Random(0x1012EL)
        val leftBehind = HashMap<String, Int>()
        repeat(400) { run ->
            val start = starts(random)
            val leaves = rare.shuffled(Random(run.toLong())).take(18).toSet()
            val (_, firstEnd) = play(start, 1000 + run, prefer = leaves)
            (firstEnd.flags intersect owned).forEach { leftBehind[it] = (leftBehind[it] ?: 0) + 1 }
            val dirty = start.copy(flags = start.flags + (firstEnd.flags intersect owned))
            val (cleanPath, clean) = play(start, run)
            val (dirtyPath, dirtied) = play(dirty, run)
            assertEquals("run $run: the second telling walked a different road", cleanPath, dirtyPath)
            val survived = (dirtied.flags intersect owned) - (clean.flags intersect owned)
            // the death is set in every telling and never cleared, and the shipped Chapter 11 and 12 flags are not cleared either;
            // nothing reads any of them, which is what the identical road above shows
            val neverCleared = setOf("halvard_died", "took_the_warden_chain", "trusted_the_vision", "doubted_the_vision",
                "pledged_support_to_fenmoor", "invited_thessaly_to_stonebeard", "left_fenmoor_uncertain",
                "lost_to_cinder_extraction_leader", "answered_the_word_at_once", "asked_before_answering",
                "chose_to_help_ilsevet", "refused_to_help_ilsevet", "warned_ilsevet_directly", "appealed_to_ilsevets_conscience",
                "left_ilsevet_to_her_wreckage", "lost_to_the_unfinished")
            assertEquals("run $run: flags the first telling left behind survived the second: ${survived - neverCleared}",
                emptySet<String>(), survived - neverCleared)
            assertEquals("run $run: inventory", clean.inventory.count { it == "healing_draught" }, dirtied.inventory.count { it == "healing_draught" })
        }
        val mustBeLeft = listOf(
            "c7_voss_called_them_off", "c7_traded", "c7_hold_terms", "c8_cart_robbed", "c8_tithe_seen", "c9_stair_people", "c9_way_stair",
            "c9_way_cart", "c9_door_nails", "c10_cover_voss", "c10_cover_stair", "c10_cover_forge", "c10_rites_name", "c10_rites_deeds",
            "c10_rites_plain", "halvard_died"
        )
        val thin = mustBeLeft.filter { (leftBehind[it] ?: 0) < 8 }.associateWith { leftBehind[it] ?: 0 }
        println("PACKAGE1012 left behind by first tellings: " + mustBeLeft.joinToString { "$it=${leftBehind[it] ?: 0}" })
        assertTrue("first tellings left too little behind to test inheritance: $thin", thin.isEmpty())
    }

    @Test
    fun `the three chapters together set no flag the Journal does not know`() {
        val setNow = (10..12).flatMap { StoryRepository.nodesInChapter("chapter_$it") }.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        val described = FlagDescriptions.entries.map { it.flag }.toSet() + FlagDescriptions.hidden.keys
        assertEquals("flags set in Chapters 10 to 12 with neither a Journal line nor a hidden reason", emptySet<String>(), setNow - described)
    }
}
