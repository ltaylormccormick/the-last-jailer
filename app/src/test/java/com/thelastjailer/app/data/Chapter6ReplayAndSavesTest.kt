package com.thelastjailer.app.data

import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.KEPT
import com.thelastjailer.app.data.Chapter6Kit.NEXT_CHAPTER
import com.thelastjailer.app.data.Chapter6Kit.ROADS
import com.thelastjailer.app.data.Chapter6Kit.SIGHTED
import com.thelastjailer.app.data.Chapter6Kit.TOLD
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.picker
import com.thelastjailer.app.data.Chapter6Kit.playToChapter7
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.visible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay and save safety for Chapter 6. A replay keeps flags, stats, items and trophies and jumps back to the
 * first scene of the story, so a second telling reaches Chapter 6 carrying everything the first one set. The
 * chapter's own opening choices and verdict choices clear the flags they own, which these tests hold in place
 * without touching Chapter 5. Older saves, parked on any scene the chapter had before it grew, must keep
 * playing to Chapter 7.
 */
class Chapter6ReplayAndSavesTest {

    private val verdicts = setOf("sealed_greymoor_personally", "reported_greymoor_honestly", "left_greymoor_as_is")

    // ---------------------------------------------------------------------------------------------
    // What the families clear, spelled out here so an emptied or wrong constant fails
    // ---------------------------------------------------------------------------------------------

    private val owned = setOf(
        "c6_halvard_confided", "c6_halvard_reticent",
        "c6_stop_tollhouse", "c6_stop_cairn", "c6_stop_camp", "c6_road_second",
        "c6_helped_refugees", "c6_met_tam", "c6_pressed_edda", "c6_nail_method", "c6_has_ward_nails",
        "c6_promised_tam", "c6_offered_the_hold", "c6_took_cairn_note",
        "c6_camp_sneaked", "c6_camp_paid", "c6_scout_taken", "c6_scout_freed", "c6_scout_bound",
        "c6_spot_watchpost", "c6_spot_cellar", "c6_spot_garden", "c6_ruin_second",
        "c6_read_ledger", "c6_inspected_breach", "c6_buried_kestrel", "c6_unbound_pinned", "c6_tam_followed"
    )

    @Test
    fun `the entry clear covers exactly the flags the chapter sets`() {
        val setByChapter = chapter6Nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
        assertEquals(owned, setByChapter.filter { it.startsWith("c6_") }.toSet())
        assertEquals(owned + "lost_to_unbound_horror", C6_TELLING_FLAGS)
        assertEquals(
            owned + "lost_to_unbound_horror" + setOf("chose_to_travel", "chose_deliberately") +
                setOf("knelt_for_the_warden", "searched_the_gate_first") + verdicts,
            C6_ENTRY_CLEARS
        )
    }

    @Test
    fun `both openings clear everything the chapter owns before setting their own flag`() {
        val dirty = owned + verdicts + setOf("lost_to_unbound_horror", "chose_to_travel", "chose_deliberately", "knelt_for_the_warden", "searched_the_gate_first")
        val volunteered = at(ROADS, flags = dirty).let { take(it, "the_road_north_volunteered") }
        assertEquals(setOf("chose_to_travel"), volunteered.flags)
        val deliberate = at(ROADS, flags = dirty).let { take(it, "the_road_north_deliberate") }
        assertEquals(setOf("chose_deliberately"), deliberate.flags)
    }

    @Test
    fun `the opening clears nothing that belongs to another chapter`() {
        val other = setOf("told_halvard_the_truth", "refused_envoy", "met_envoy_alone", "halvard_died", "trusted_voss_fully")
        val s = take(at(ROADS, flags = other + owned), "the_road_north_deliberate")
        assertTrue(s.flags.containsAll(other))
    }

    @Test
    fun `the clears sit only on Chapter 6's own choices, never on Chapter 5's end`() {
        val clearing = StoryRepository.chapters.flatMap { c -> StoryRepository.nodesInChapter(c.id).map { it to c.id } }
            .flatMap { (n, chapter) -> n.choices.filter { it.consequences.clearFlags.any { f -> f in C6_ENTRY_CLEARS } }.map { n.id to chapter } }
        assertTrue(clearing.isNotEmpty())
        assertTrue("only Chapter 6 clears Chapter 6's flags: $clearing", clearing.all { it.second == "chapter_6" })
    }

    @Test
    fun `every choice that sets a member of a family clears the whole family`() {
        val families = mapOf(
            "opening" to C6_OPENING_FAMILY,
            "gate approach" to C6_GATE_APPROACH_FAMILY,
            "verdict" to C6_VERDICT_FAMILY
        )
        families.forEach { (name, family) ->
            val setters = chapter6Nodes.flatMap { n -> n.choices.map { n.id to it } }.filter { (_, c) -> c.consequences.setFlags.any { it in family } }
            assertTrue("$name has setters", setters.isNotEmpty())
            setters.forEach { (nodeId, c) ->
                assertTrue("$name: $nodeId '${c.label}' must clear the family", c.consequences.clearFlags.containsAll(family))
                assertEquals("$name: $nodeId sets one member", 1, c.consequences.setFlags.count { it in family })
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // A second telling with different decisions
    // ---------------------------------------------------------------------------------------------

    private val firstTelling = picker(
        ROADS to "I'll go",
        "c6_road_hub_one" to "->c6_tollhouse",
        "c6_tollhouse" to "Stay the night",
        "c6_tollhouse_helped" to "answer to Halvard for them",
        "c6_road_hub_two" to "->c6_camp",
        "c6_camp" to "Take one of them",
        "c6_camp_scout" to "Let him go",
        "the_second_gate" to "Kneel",
        "c6_ruin_hub_one" to "->c6_watchpost",
        "c6_watchpost" to "Read her last pages",
        "c6_ruin_hub_two" to "->c6_garden",
        "c6_garden" to "Carry her",
        SIGHTED to "Take out the nails",
        "c6_pin_decided" to "finish the seal"
    )

    private fun secondTelling() = picker(
        ROADS to "Tell me which gate",
        "c6_road_hub_one" to "->c6_tollhouse",
        "c6_tollhouse" to "Ask Edda",
        "c6_road_hub_two" to "->c6_cairn",
        "the_second_gate" to "Search the gate",
        "c6_ruin_hub_one" to "->c6_cellar",
        "c6_cellar" to "Lift the hatch",
        "c6_ruin_hub_two" to "->c6_watchpost",
        "c6_watchpost" to "Leave her book unread",
        SIGHTED to "Meet it in the yard",
        "what_must_be_decided" to "Leave it as it is"
    )

    @Test
    fun `a second telling that decides differently carries nothing from the first`() {
        val first = playToChapter7(at(ROADS, flags = setOf(TOLD)), firstTelling)
        assertTrue(first.flags.containsAll(setOf(
            "chose_to_travel", "c6_helped_refugees", "c6_met_tam", "c6_promised_tam", "c6_offered_the_hold", "c6_nail_method",
            "c6_has_ward_nails", "c6_scout_freed", "knelt_for_the_warden", "c6_read_ledger",
            "c6_buried_kestrel", "c6_unbound_pinned", "sealed_greymoor_personally"
        )))
        assertTrue("ward_nails" in first.inventory && "Nails in the Dark" in first.trophies)

        // A replay keeps everything and returns to the start of the story; it reaches Chapter 6 again with the Chapter 5 answer set.
        val replay = first.copy(sceneId = ROADS, chapterId = "chapter_6", flags = first.flags + KEPT - TOLD)
        val second = playToChapter7(replay, secondTelling())

        val gone = setOf(
            "chose_to_travel", "c6_helped_refugees", "c6_met_tam", "c6_promised_tam", "c6_offered_the_hold", "c6_nail_method",
            "c6_scout_taken", "c6_scout_freed", "c6_camp_sneaked", "c6_camp_paid", "c6_stop_camp", "knelt_for_the_warden",
            "c6_read_ledger", "c6_buried_kestrel", "c6_spot_garden", "c6_unbound_pinned", "sealed_greymoor_personally",
            "reported_greymoor_honestly"
        )
        assertEquals("first-telling flags survive: ${second.flags intersect gone}", emptySet<String>(), second.flags intersect gone)
        assertTrue(second.flags.containsAll(setOf(
            "chose_deliberately", "c6_halvard_reticent", "c6_pressed_edda", "c6_took_cairn_note", "c6_has_ward_nails",
            "searched_the_gate_first", "c6_inspected_breach", "left_greymoor_as_is"
        )))
        assertEquals(1, second.flags.count { it in verdicts })
        assertEquals(2, second.flags.count { it.startsWith("c6_stop_") })
        assertEquals(2, second.flags.count { it.startsWith("c6_spot_") })
        assertFalse("the first telling's wins are not repeated", "c6_halvard_confided" in second.flags)
    }

    @Test
    fun `the second telling's road and yard start with every stop and place offered again`() {
        val first = playToChapter7(at(ROADS, flags = setOf(TOLD)), firstTelling)
        val afterOpening = take(first.copy(sceneId = ROADS, chapterId = "chapter_6"), "the_road_north_volunteered")
        val road = take(afterOpening, "c6_road_hub_one")
        assertEquals(listOf("c6_tollhouse", "c6_cairn", "c6_camp"), visible(road).map { it.nextNodeId })
        val gate = at("what_the_warden_left_knelt", flags = afterOpening.flags)
        val yard = take(gate, "c6_ruin_hub_one")
        assertEquals(listOf("c6_watchpost", "c6_cellar", "c6_garden"), visible(yard).map { it.nextNodeId })
    }

    @Test
    fun `a second telling cannot pin the creature on the first telling's knowledge`() {
        val first = playToChapter7(at(ROADS, flags = setOf(TOLD)), firstTelling)
        // Inventory and trophies are kept by a replay; flags are cleared by the opening.
        val second = playToChapter7(
            first.copy(sceneId = ROADS, chapterId = "chapter_6"),
            picker(
                ROADS to "Tell me which gate",
                "c6_road_hub_one" to "->c6_cairn",
                "c6_road_hub_two" to "->c6_camp",
                "c6_camp" to "Walk up",
                "c6_ruin_hub_one" to "->c6_garden",
                "c6_ruin_hub_two" to "->c6_cellar",
                SIGHTED to "Meet it in the yard"
            )
        )
        assertTrue("ward_nails" in second.inventory)
        // The same state stood at the dusk: the keepsake is carried but the knowledge is not.
        val dusk = at(SIGHTED, flags = emptySet(), inventory = first.inventory, courage = 99)
        assertFalse(visible(dusk).any { it.nextNodeId == "c6_pin_held" })
        assertTrue(second.flags.containsAll(setOf("c6_has_ward_nails")))
        assertFalse("c6_nail_method" in second.flags)
    }

    // ---------------------------------------------------------------------------------------------
    // Older saves
    // ---------------------------------------------------------------------------------------------

    private val oldScenes = listOf(
        "the_choice_of_roads", "the_road_north_volunteered", "the_road_north_deliberate", "the_second_gate",
        "what_the_warden_left_knelt", "what_the_warden_left_searched", "the_unbound", "unbound_defeat",
        "what_must_be_decided", "chapter6_end_sealed", "chapter6_end_reported", "chapter6_end_left"
    )

    private val oldFlags = mapOf(
        "the_choice_of_roads" to emptySet<String>(),
        "the_road_north_volunteered" to setOf("chose_to_travel"),
        "the_road_north_deliberate" to setOf("chose_deliberately"),
        "the_second_gate" to setOf("chose_to_travel"),
        "what_the_warden_left_knelt" to setOf("chose_to_travel", "knelt_for_the_warden"),
        "what_the_warden_left_searched" to setOf("chose_deliberately", "searched_the_gate_first"),
        "the_unbound" to setOf("chose_to_travel", "knelt_for_the_warden", "took_kestrels_locket"),
        "unbound_defeat" to setOf("chose_deliberately", "searched_the_gate_first", "took_kestrels_locket"),
        "what_must_be_decided" to setOf("chose_to_travel", "knelt_for_the_warden", "took_kestrels_locket"),
        "chapter6_end_sealed" to setOf("sealed_greymoor_personally"),
        "chapter6_end_reported" to setOf("reported_greymoor_honestly"),
        "chapter6_end_left" to setOf("left_greymoor_as_is")
    )

    @Test
    fun `a save parked on any scene the chapter had before it grew plays on to Chapter 7`() {
        oldScenes.forEach { id ->
            listOf(emptySet(), setOf(TOLD), setOf(KEPT)).forEach { ch5 ->
                listOf(true, false).forEach { strong ->
                    val start = at(id, flags = oldFlags.getValue(id) + ch5, courage = if (strong) 99 else 1, honour = if (strong) 99 else 0, gold = 25, health = 100)
                    val end = playToChapter7(start)
                    assertEquals("$id (ch5=$ch5 strong=$strong)", NEXT_CHAPTER, end.sceneId)
                }
            }
        }
    }

    @Test
    fun `a save with no Chapter 5 account flag on the methodical road still gets Halvard's account`() {
        val s = at("the_road_north_deliberate", flags = setOf("chose_deliberately"))
        val onward = take(s, "c6_maps_told")
        assertTrue("c6_halvard_confided" in onward.applyChoice(visible(onward).single()).flags)
    }

    @Test
    fun `a save parked after a lost fight is found by nobody, since it never met Tam`() {
        val s = at("unbound_defeat", flags = setOf("chose_to_travel", "took_kestrels_locket"), health = 300)
        assertEquals(listOf("c6_defeat_alone"), visible(s).map { it.nextNodeId })
        val withTam = at("unbound_defeat", flags = setOf("c6_met_tam"), health = 300)
        assertEquals(listOf("c6_defeat_tam"), visible(withTam).map { it.nextNodeId })
    }

    @Test
    fun `the old verdict scene still offers the same three verdicts to the same stats`() {
        val decided = at("what_must_be_decided", courage = 5, honour = 4)
        val labels = visible(decided).map { it.label }
        assertEquals(
            listOf(
                "\"I'll seal what's left myself, however long it holds.\"",
                "\"This isn't mine to hold. I'll bring word to people who can actually rebuild it.\"",
                "Leave it as it is, and carry what you learned back to Halvard."
            ),
            labels
        )
    }

    @Test
    fun `GameState needs nothing new to carry the chapter`() {
        // Compile-time guard that the chapter adds flags, items and trophies only: the save shape is unchanged.
        val fields = GameState::class.java.declaredFields.map { it.name }.filter { !it.startsWith("$") }.toSet()
        assertTrue(fields.containsAll(setOf("sceneId", "chapterId", "flags", "inventory", "trophies", "courage", "honour", "gold", "health")))
        assertEquals(
            setOf("activeSlot", "chapterId", "sceneId", "courage", "honour", "health", "maxHealth", "gold", "level", "xp", "xpToNextLevel", "inventory", "trophies", "flags"),
            fields
        )
    }
}
