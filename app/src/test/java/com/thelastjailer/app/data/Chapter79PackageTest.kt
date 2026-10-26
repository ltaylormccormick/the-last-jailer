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
 * The three chapters as one package, from the three endings of Chapter 6 to Chapter 10's first scene. The per-chapter tests
 * pin each chapter; these hold the seams between them and the property a replay depends on: that a second telling, begun with
 * every flag the first one left behind, goes exactly where a first telling with the same decisions goes.
 */
class Chapter79PackageTest {

    private val goal = "the_last_two"

    /** Every flag any choice in Chapters 7, 8 or 9 can set: the flags a second telling must not inherit from the first. */
    private val owned: Set<String> = (7..9).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
        .flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet() + HUB_MARKERS + C7_RUNG_FAMILY

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
        return flags
    }

    /** A 64-bit mixer, so that the choice taken is not decided by how the labels happen to hash against each other. */
    private fun mix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    private fun score(c: com.thelastjailer.app.Choice, seed: Int): Long =
        mix((c.label.hashCode().toLong() shl 32) xor c.nextNodeId.hashCode().toLong() xor mix(seed.toLong()))

    /** The scenes that leave the most behind: the rare answers, so that a first telling sets as many flags as it can. */
    private val rare = listOf(
        "c7_called_off", "c7_trade_made", "c7_letter_page", "c7_hold_terms", "c7_hold_hearth", "c7_wound_tended",
        "c8_deserter_paid", "c8_deserter_scared", "c8_chapel_door", "c8_cart_watched", "c8_cart_robbed", "c8_capture_gate",
        "c8_night_farmers", "c8_vow_now", "c9_plans", "c9_stores_kit", "c9_stair_people", "c9_gate_braced",
        "c9_in_by_cart_farmers", "c9_door_nails", "c9_door_force", "c9_voss_chapel", "c9_plans_hunted", "c9_north_stair",
        "c9_stores", "c8_chapel", "c8_cart", "c8_deserter", "c9_door_seal", "c7_cost_stair", "c9_in_by_stair"
    )

    /**
     * Plays from [start] to Chapter 10 by a fixed policy: of the choices visible, take the one whose label, target and [seed] hash
     * highest, and win or lose a fight by the same hash. Two states that see the same choices take the same path. A choice that
     * leads to one of the [prefer] scenes is taken before the hash is consulted.
     */
    private fun play(start: GameState, seed: Int, prefer: Set<String> = emptySet()): Pair<List<String>, GameState> {
        var s = start
        val path = ArrayList<String>()
        var steps = 0
        while (s.sceneId != goal) {
            check(steps++ < 200) { "never finished; stuck at ${s.sceneId} (seed $seed)" }
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
    fun `every ending of Chapter 6 reaches Chapter 10 through all three chapters by a fixed policy`() {
        val random = Random(0x79L)
        repeat(300) { run ->
            val (path, end) = play(starts(random), run)
            assertTrue("run $run read Chapter 7", path.any { it.startsWith("c7_") })
            assertTrue("run $run read Chapter 8", path.any { it.startsWith("c8_") })
            assertTrue("run $run read Chapter 9", path.any { it.startsWith("c9_") })
            assertEquals("run $run", goal, end.sceneId)
        }
    }

    @Test
    fun `a second telling begun with everything the first left behind goes exactly where a first telling goes`() {
        val random = Random(0x7ECL)
        var compared = 0
        val leftBehind = HashMap<String, Int>()
        repeat(400) { run ->
            val start = starts(random)
            val leaves = rare.shuffled(Random(run.toLong())).take(12).toSet()
            val (_, firstEnd) = play(start, 1000 + run, prefer = leaves)
            (firstEnd.flags intersect owned).forEach { leftBehind[it] = (leftBehind[it] ?: 0) + 1 }
            val dirty = start.copy(flags = start.flags + (firstEnd.flags intersect owned))
            val (cleanPath, clean) = play(start, run)
            val (dirtyPath, dirtied) = play(dirty, run)
            assertEquals("run $run: the second telling walked a different road", cleanPath, dirtyPath)
            assertEquals(
                "run $run: flags the first telling left behind survived the second: " +
                    "${(dirtied.flags intersect owned) - (clean.flags intersect owned)}",
                clean.flags intersect owned, dirtied.flags intersect owned
            )
            assertEquals("run $run: inventory", clean.inventory.count { it == "healing_draught" }, dirtied.inventory.count { it == "healing_draught" })
            compared++
        }
        assertEquals(400, compared)
        // A replay test that never leaves anything behind proves nothing. These are flags a second telling could most easily inherit.
        val mustBeLeft = listOf(
            "c7_voss_called_them_off", "c7_traded", "c7_page", "c7_hold_terms", "c7_ground_stair", "c8_cart_robbed", "c8_voss_cells",
            "c8_exit_tithe", "c8_ash_farmers", "c9_stair_people", "c9_gate_braced", "c9_knows_cells", "c9_kit", "c9_door_seal",
            "c9_door_nails", "c9_door_force"
        )
        val thin = mustBeLeft.filter { (leftBehind[it] ?: 0) < 8 }.associateWith { leftBehind[it] ?: 0 }
        println("PACKAGE left behind by first tellings: " + mustBeLeft.joinToString { "$it=${leftBehind[it] ?: 0}" })
        assertTrue("first tellings left too little behind to test inheritance: $thin", thin.isEmpty())
    }

    @Test
    fun `the three chapters together set no flag the Journal and the checkpoint model do not know`() {
        val setNow = (7..9).flatMap { StoryRepository.nodesInChapter("chapter_$it") }.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        val described = FlagDescriptions.entries.map { it.flag }.toSet() + FlagDescriptions.hidden.keys
        assertEquals("flags set in Chapters 7 to 9 with neither a Journal line nor a hidden reason", emptySet<String>(), setNow - described)
    }
}
