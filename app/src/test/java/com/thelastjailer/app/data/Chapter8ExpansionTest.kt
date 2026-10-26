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
 * Chapter 8 as written: the Ash Road (a hub of three places of which two are seen), the confession on the third day,
 * the Sanctum, the horn and its three ways out (the sentinel, the ramp, the tithe arch), Voss taken alive on every route,
 * the long night, and the vow with its price. These tests pin the routing and the gates, the costs and rewards of each
 * decision, that every route reaches Chapter 9, and that the scenes that shipped are word for word as before.
 */
class Chapter8ExpansionTest {

    private val goal = setOf("a_debt_remembered_trusted", "a_debt_remembered_guarded")
    private val stops = setOf("c8_deserter", "c8_chapel", "c8_cart")

    private fun words(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun isGoal(s: GameState) = s.sceneId in goal

    // ---------------------------------------------------------------------------------------------
    // Into the chapter
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `every departure from Chapter 7 leads to the first night on the Ash Road`() {
        listOf("chapter7_end_now", "chapter7_end_warn_halvard", "chapter7_end_asked_for_time", "c7_depart_now", "c7_depart_blessing", "c7_depart_time")
            .forEach { assertEquals(it, listOf("c8_ash_camp"), visible(at(it)).map { c -> c.nextNodeId }) }
        assertEquals("c8_ash_camp", StoryRepository.chapters.single { it.id == "chapter_8" }.startNodeId)
    }

    @Test
    fun `the first night clears everything the chapter owns, offers the first sleep, and moves the rung`() {
        val stale = C8_TELLING_FLAGS + C7_RUNG_FAMILY.first() + "c7_halvard_open"
        val offer = takeLabel(at("c8_ash_camp", flags = stale, honour = 0), "Offer her the first sleep. You will take the watch.")
        assertTrue(offer.flags.intersect(C8_TELLING_FLAGS).isEmpty())
        assertTrue("c7_halvard_open" in offer.flags)
        assertEquals(1, offer.honour)
        assertEquals(setOf("c7_rung_working"), offer.flags.intersect(C7_RUNG_FAMILY))
        val counsel = takeLabel(at("c8_ash_camp", flags = setOf("c7_rung_working"), honour = 0), "Keep your own counsel, and your own watch.")
        assertEquals(0, counsel.honour)
        assertEquals(setOf("c7_rung_working"), counsel.flags.intersect(C7_RUNG_FAMILY))
        val trusted = takeLabel(at("c8_ash_camp", flags = setOf("c7_rung_trusted")), "Offer her the first sleep. You will take the watch.")
        assertEquals("the top rung stays", setOf("c7_rung_trusted"), trusted.flags.intersect(C7_RUNG_FAMILY))
        val legacy = takeLabel(at("c8_ash_camp"), "Offer her the first sleep. You will take the watch.")
        assertEquals("no rung counts as the lowest", setOf("c7_rung_working"), legacy.flags.intersect(C7_RUNG_FAMILY))
    }

    // ---------------------------------------------------------------------------------------------
    // The Ash Road
    // ---------------------------------------------------------------------------------------------

    private fun roadWalks(courage: Int, honour: Int) =
        explore(at("c8_road_hub_one", courage = courage, honour = honour), { it.sceneId == "the_road_to_ashfall" })

    @Test
    fun `the Ash Road is a hub of three places of which exactly two are seen`() {
        // Courage 1 / Honour 5 has 53 walks, not 54: robbing the cart first costs the Honour that persuading the deserter needs.
        listOf((99 to 99) to 66, (1 to 0) to 42, (1 to 5) to 53).forEach { (stats, count) ->
            val walks = roadWalks(stats.first, stats.second)
            assertEquals("walks at $stats", count, walks.size)
            walks.forEach { (trail, end) ->
                val seen = trail.filter { it in stops }
                assertEquals(trail.toString(), 2, seen.toSet().size)
                assertEquals(trail.toString(), 2, seen.size)
                assertEquals(trail.toString(), 1, trail.count { it == "c8_road_hub_two" })
                assertTrue("no marker outlives the hub", end.flags.intersect(HUB_MARKERS).isEmpty())
            }
        }
    }

    @Test
    fun `the first hub offers all three and the second the other two`() {
        val one = at("c8_road_hub_one")
        assertEquals(listOf("c8_deserter", "c8_chapel", "c8_cart"), visible(one).map { it.nextNodeId })
        stops.forEach { first ->
            var s = take(one, first)
            var guard = 0
            while (s.sceneId != "c8_road_hub_two") { check(guard++ < 5); s = s.applyChoice(visible(s).first()) }
            assertEquals(stops - first, visible(s).map { it.nextNodeId }.toSet())
        }
    }

    @Test
    fun `the deserter sells the rotation for coin, kindness or fear, and each costs something different`() {
        val start = at("c8_deserter", courage = 6, honour = 5, gold = 50)
        val paid = take(start, "c8_deserter_paid")
        assertEquals(20, paid.gold)
        assertTrue("c8_rotation" in paid.flags)
        val talked = take(start, "c8_deserter_talked")
        assertEquals(6, talked.honour)
        assertEquals(50, talked.gold)
        val scared = take(start, "c8_deserter_scared")
        assertEquals(7, scared.courage)
        assertEquals(50, scared.gold)
        val poor = take(at("c8_deserter", gold = 12), "c8_deserter_paid")
        assertEquals("a short purse is emptied, never made negative", 0, poor.gold)
        assertEquals(listOf("c8_deserter_paid", "c8_road_hub_two"), visible(at("c8_deserter", courage = 1, honour = 0)).map { it.nextNodeId })
        assertTrue(words(node("c8_deserter_paid").narrativeText).contains("or what he can find of thirty"))
    }

    @Test
    fun `frightening the deserter costs Voss's good opinion and kindness does not`() {
        val atWork = at("c8_deserter_scared", flags = setOf("c7_rung_working"))
        assertEquals(setOf("c7_rung_wary"), take(atWork, "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_wary"), take(at("c8_deserter_scared", flags = setOf("c7_rung_wary")), "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_working"), take(at("c8_deserter_scared", flags = setOf("c7_rung_trusted")), "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
        val kind = take(at("c8_deserter_talked", flags = setOf("c7_rung_working")), "c8_road_hub_two")
        assertEquals(setOf("c7_rung_working"), kind.flags.intersect(C7_RUNG_FAMILY))
    }

    @Test
    fun `the chapel gives rest or Voss's silence, not both`() {
        val start = at("c8_chapel", health = 300, flags = setOf("c7_rung_working"))
        val rested = take(start, "c8_chapel_rested")
        assertEquals(331, rested.health)
        assertFalse("c8_chapel_story" in rested.flags)
        val door = take(start, "c8_chapel_door")
        assertEquals(301, door.health)
        assertTrue("c8_chapel_story" in door.flags)
        assertEquals("standing with her raises the rung", setOf("c7_rung_trusted"), take(door, "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
        assertEquals("the top rung stays", setOf("c7_rung_trusted"),
            take(at("c8_chapel_door", flags = setOf("c7_rung_trusted")), "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
    }

    @Test
    fun `the cart can be watched or robbed, and robbing it costs Honour and Voss and the farmers`() {
        val start = at("c8_cart", honour = 3, gold = 10, flags = setOf("c7_rung_working"))
        val watched = take(start, "c8_cart_watched")
        assertEquals(10, watched.gold)
        assertTrue("c8_tithe_seen" in watched.flags)
        val robbed = take(start, "c8_cart_robbed")
        assertEquals(35, robbed.gold)
        assertEquals(2, robbed.honour)
        assertTrue("c8_cart_robbed" in robbed.flags && "c8_tithe_seen" !in robbed.flags)
        assertEquals(setOf("c7_rung_wary"), take(robbed, "c8_road_hub_two").flags.intersect(C7_RUNG_FAMILY))
    }

    @Test
    fun `robbing the cart first costs the Honour that talking to the deserter needs`() {
        val robbed = take(at("c8_cart", honour = 5), "c8_cart_robbed")
        assertEquals(4, robbed.honour)
        val deserter = take(take(robbed, "c8_road_hub_two"), "c8_deserter")
        assertTrue(visible(deserter).none { it.nextNodeId == "c8_deserter_talked" })
        val kind = take(take(take(at("c8_cart", honour = 5), "c8_cart_watched"), "c8_road_hub_two"), "c8_deserter")
        assertTrue(visible(kind).any { it.nextNodeId == "c8_deserter_talked" })
    }

    // ---------------------------------------------------------------------------------------------
    // The Sanctum
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the third day and the two approaches keep their answers exclusive and lead to the horn`() {
        val road = at("the_road_to_ashfall", flags = C8_ROAD_FAMILY)
        assertEquals(setOf("reassured_voss"), takeLabel(road, "\"Then we make sure it doesn't go wrong.\"").flags.intersect(C8_ROAD_FAMILY))
        assertEquals(setOf("acknowledged_the_risk"), takeLabel(road, "\"I know what I'm risking. Let's move.\"").flags.intersect(C8_ROAD_FAMILY))
        listOf("the_sanctum_from_afar_reassured", "the_sanctum_from_afar_acknowledged").forEach { id ->
            val s = at(id, flags = C8_APPROACH_FAMILY)
            assertEquals(setOf("moved_in_stealthily"), takeLabel(s, "Move in under cover of the ash-fall itself.").flags.intersect(C8_APPROACH_FAMILY))
            assertEquals(setOf("circled_wide"), takeLabel(s, "Circle wide and find a safer vantage first.").flags.intersect(C8_APPROACH_FAMILY))
        }
        listOf("what_they_see_stealthy", "what_they_see_wide").forEach { id ->
            val out = take(at(id), "c8_the_horn")
            assertTrue("torn_sanctum_plans" in out.inventory && "stole_the_schematic" in out.flags)
        }
    }

    private fun exits(flags: Set<String>, courage: Int = 99) = visible(at("c8_the_horn", flags = flags, courage = courage))

    @Test
    fun `the horn always allows a fight and offers the other ways out only to someone who earned them`() {
        assertEquals(listOf("sentinels_close_in"), exits(emptySet(), courage = 1).map { it.nextNodeId })
        assertEquals(listOf("sentinels_close_in", "c8_capture_ramp"), exits(emptySet(), courage = 4).map { it.nextNodeId })
        assertEquals(listOf("sentinels_close_in", "c8_capture_ramp"), exits(setOf("c8_rotation"), courage = 1).map { it.nextNodeId })
        assertEquals("the ramp is one choice, never two", 1, exits(setOf("c8_rotation")).count { it.nextNodeId == "c8_capture_ramp" })
        assertEquals(listOf("sentinels_close_in", "c8_capture_ramp"), exits(setOf("earned_voss_seal"), courage = 4).map { it.nextNodeId })
        assertEquals(
            listOf("sentinels_close_in", "c8_capture_ramp", "c8_capture_gate"),
            exits(setOf("earned_voss_seal", "c8_tithe_seen"), courage = 4).map { it.nextNodeId }
        )
        assertEquals(
            "the tithe arch needs both the signet and a look at the tithe",
            listOf("sentinels_close_in"), exits(setOf("c8_tithe_seen"), courage = 1).map { it.nextNodeId }
        )
    }

    @Test
    fun `the ramp costs strength unless the watch change is known, and the arch spends the signet`() {
        val legs = takeLabel(at("c8_the_horn", health = 300), "Take the slag ramp and trust your legs.")
        assertEquals(291, legs.health)
        assertTrue("c8_exit_ramp" in legs.flags)
        val timed = takeLabel(at("c8_the_horn", flags = setOf("c8_rotation"), health = 300), "Take the slag ramp, timing it to the watch change the deserter sold you.")
        assertEquals(301, timed.health)
        assertTrue("c8_exit_ramp" in timed.flags)
        val arch = take(at("c8_the_horn", flags = setOf("earned_voss_seal", "c8_tithe_seen", "c8_exit_ramp")), "c8_capture_gate")
        assertEquals(setOf("c8_exit_tithe"), arch.flags.intersect(C8_EXIT_FAMILY))
        assertTrue("c8_seal_spent" in arch.flags)
        assertNull(node("c8_capture_ramp").combatEncounterId)
        assertNull(node("c8_capture_gate").combatEncounterId)
    }

    @Test
    fun `fighting the sentinel is the shipped encounter and the only way out that pays`() {
        assertEquals("sanctum_sentinel_encounter", node("sentinels_close_in").combatEncounterId)
        val enc = CombatRepository.encounter("sanctum_sentinel_encounter")
        assertEquals("the_price_of_escape", enc.victoryNodeId)
        assertEquals("sanctum_sentinel_defeat", enc.defeatNodeId)
        assertEquals(190, enc.xpReward)
        assertEquals(50, enc.goldReward)
        val won = winOrLose(at("sentinels_close_in"), win = true)
        assertTrue("Out of the Ash" in won.trophies)
        val ramp = takeLabel(at("c8_the_horn"), "Take the slag ramp and trust your legs.")
        assertTrue(ramp.trophies.isEmpty() && ramp.xp == 0)
        val lost = winOrLose(at("sentinels_close_in"), win = false)
        assertEquals("sanctum_sentinel_defeat", lost.sceneId)
    }

    @Test
    fun `a Voss who trusts him chooses her capture and tells him where the cells are`() {
        val trusted = at("the_price_of_escape", flags = setOf("c7_rung_trusted"))
        assertEquals(listOf("c8_her_choice"), visible(trusted).map { it.nextNodeId })
        val taken = take(trusted, "c8_her_choice")
        assertTrue("c8_voss_cells" in taken.flags && "Her Choice" in taken.trophies)
        assertEquals(listOf("c8_long_night"), visible(taken).map { it.nextNodeId })
        listOf(setOf("c7_rung_working"), setOf("c7_rung_wary"), emptySet<String>()).forEach {
            val s = at("the_price_of_escape", flags = it)
            assertEquals(listOf("c8_long_night"), visible(s).map { c -> c.nextNodeId })
        }
        val ramp = take(at("c8_capture_ramp", flags = setOf("c7_rung_trusted")), "c8_her_choice")
        assertTrue("c8_voss_cells" in ramp.flags && "Her Choice" in ramp.trophies)
        assertEquals("the ramp without that trust has no east stair", listOf("c8_long_night"),
            visible(at("c8_capture_ramp", flags = setOf("c7_rung_working"))).map { c -> c.nextNodeId })
        val gate = at("c8_capture_gate", flags = setOf("c7_rung_trusted"))
        assertEquals("at the arch she was recognised, not choosing", listOf("c8_long_night"), visible(gate).map { c -> c.nextNodeId })
        assertFalse("c8_voss_cells" in take(gate, "c8_long_night").flags)
    }

    @Test
    fun `no route frees Voss or keeps her in the party`() {
        val texts = listOf("c8_capture_ramp", "c8_capture_gate", "c8_her_choice", "the_price_of_escape").map { words(node(it).narrativeText) }
        assertTrue(texts.all { it.contains("taken") || it.contains("planned this") })
        val outs = listOf("c8_capture_ramp", "c8_capture_gate", "c8_her_choice", "the_price_of_escape")
            .flatMap { node(it).choices.map { c -> c.nextNodeId } }.toSet()
        assertEquals(setOf("c8_long_night", "c8_her_choice"), outs)
    }

    // ---------------------------------------------------------------------------------------------
    // The night, the vow, the end
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the long night can be spent on a letter, on the ash-farmers or on the road, and the farmers will not talk to a thief`() {
        val start = at("c8_long_night", honour = 0, courage = 1, health = 300)
        assertEquals(listOf("c8_night_letter", "c8_night_farmers", "c8_night_route"), visible(start).map { it.nextNodeId })
        assertEquals(listOf("c8_night_letter", "c8_night_route"), visible(at("c8_long_night", flags = setOf("c8_cart_robbed"))).map { it.nextNodeId })
        val letter = take(start, "c8_night_letter")
        assertEquals(1, letter.honour)
        assertEquals(setOf("c8_wrote_halvard"), letter.flags.intersect(C8_NIGHT_FAMILY))
        val route = take(start, "c8_night_route")
        assertEquals(2, route.courage)
        assertEquals(291, route.health)
        assertEquals(setOf("c8_marked_route"), route.flags.intersect(C8_NIGHT_FAMILY))
        assertEquals(setOf("c8_ash_farmers"), take(start, "c8_night_farmers").flags.intersect(C8_NIGHT_FAMILY))
        listOf("c8_night_letter", "c8_night_farmers", "c8_night_route").forEach {
            assertEquals(listOf("what_must_be_risked"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `the three vows keep their gates and each pays its price on the way to the end`() {
        val s = at("what_must_be_risked", courage = 7, honour = 6, health = 300, gold = 50)
        assertEquals(listOf("c8_vow_now", "c8_vow_prepared", "c8_vow_undecided"), visible(s).map { it.nextNodeId })
        assertEquals(listOf("c8_vow_undecided"), visible(at("what_must_be_risked", courage = 1, honour = 0)).map { it.nextNodeId })
        val now = take(s, "c8_vow_now")
        assertEquals(8, now.courage)
        assertEquals(setOf("vowed_to_return_now"), now.flags.intersect(C8_VOW_FAMILY))
        assertEquals(300 + 1 - 15 + 1, take(now, "chapter8_end").health)
        val prepared = take(s, "c8_vow_prepared")
        assertEquals(7, prepared.honour)
        assertEquals(30, take(prepared, "chapter8_end").gold)
        val undecided = take(s, "c8_vow_undecided")
        assertEquals(50, take(undecided, "chapter8_end").gold)
        assertEquals(300 + 1 + 1, take(undecided, "chapter8_end").health)
        val stale = at("what_must_be_risked", flags = C8_VOW_FAMILY, courage = 7, honour = 6)
        listOf("c8_vow_now", "c8_vow_prepared", "c8_vow_undecided").forEach {
            assertEquals(1, take(stale, it).flags.count { f -> f in C8_VOW_FAMILY })
        }
    }

    @Test
    fun `the chapter's end still hands Chapter 9 its two openings`() {
        assertEquals(listOf("a_debt_remembered_trusted"), visible(at("chapter8_end", flags = setOf("trusted_voss_fully"))).map { it.nextNodeId })
        assertEquals(listOf("a_debt_remembered_guarded"), visible(at("chapter8_end")).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // The shipped scenes
    // ---------------------------------------------------------------------------------------------

    private val shipped = mapOf(
        "the_road_to_ashfall" to "c3f7cdcf3de00dff256a6a6f3235400d16724549bb067b5c91f195332a66b3a4",
        "the_sanctum_from_afar_reassured" to "89ff8d62dc8aa0a5c6c554d719ee08c3e5c0384e36403dddc4ca4511ebcda9c5",
        "the_sanctum_from_afar_acknowledged" to "8e5944c1c2d9906d80e44e1bd4b647fbd9c31ebff9fb6e7a188e9f21f96b8f0a",
        "what_they_see_stealthy" to "16ad72167da62c006ed5c7aeb8759ca029ae32e088c58ad78f36847ce8a75e0f",
        "what_they_see_wide" to "151b4141bf03d98ee4fef7c549ce222f44ea6711688c77f310e5c64d54974432",
        "sentinels_close_in" to "3de4ab7b0e78f68fbfab5c4aa44e9b89822cfd20c8930f0fd826c135c5e98e98",
        "sanctum_sentinel_defeat" to "b14b63a67ae1e6f2ee76c946eaad826e128a530edfdb37fd94a78c0c0fcc0ef2",
        "the_price_of_escape" to "7790df06627c6baae16160868e1e1681fff8eaf0b2f52ea151a0999d154a9edb",
        "what_must_be_risked" to "9a8354a1077aaf443ad063417a08acbf953acd44e4c4c8db39ac2664d0b55b49",
        "chapter8_end" to "732f6bb2c7e5b59ad9bbc0713e733343e6773e53e1759658732db05f6c9f376c"
    )

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(words(text).toByteArray()).joinToString("") { "%02x".format(it) }

    @Test
    fun `all ten scenes that shipped in Chapter 8 are word for word as before`() {
        val moved = shipped.filter { (id, hash) -> sha(node(id).narrativeText) != hash }.keys
        assertTrue("Changed scenes: $moved", moved.isEmpty())
    }

    // ---------------------------------------------------------------------------------------------
    // Old saves, replays, and the whole chapter
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a save parked on any scene Chapter 8 had before it grew plays on to Chapter 9`() {
        val legacyFlags = mapOf(
            "the_sanctum_from_afar_reassured" to setOf("reassured_voss"),
            "what_they_see_stealthy" to setOf("reassured_voss", "moved_in_stealthily"),
            "what_they_see_wide" to setOf("acknowledged_the_risk", "circled_wide"),
            "sentinels_close_in" to setOf("stole_the_schematic"),
            "sanctum_sentinel_defeat" to setOf("stole_the_schematic"),
            "the_price_of_escape" to setOf("stole_the_schematic", "trusted_voss_fully"),
            "what_must_be_risked" to setOf("stole_the_schematic"),
            "chapter8_end" to setOf("vowed_to_return_now")
        )
        legacyFlags.keys.plus(shipped.keys).toSet().forEach { id ->
            listOf(true, false).forEach { strong ->
                val start = at(id, flags = legacyFlags[id].orEmpty(), courage = if (strong) 99 else 1, honour = if (strong) 99 else 0, health = 300)
                val end = ReplayKit.play(start, stop = { isGoal(it) })
                assertTrue("$id strong=$strong ended at ${end.sceneId}", isGoal(end))
            }
        }
    }

    @Test
    fun `a second telling that decides differently carries nothing from the first`() {
        val first = ReplayKit.play(
            at("c8_ash_camp", flags = setOf("c7_rung_trusted", "earned_voss_seal", "trusted_voss_fully")),
            stop = { isGoal(it) },
            pick = picker(
                "c8_ash_camp" to "Offer her the first sleep",
                "c8_road_hub_one" to "->c8_deserter",
                "c8_deserter" to "Pay him",
                "c8_road_hub_two" to "->c8_cart",
                "c8_cart" to "Walk beside the cart",
                "the_road_to_ashfall" to "Then we make sure",
                "the_sanctum_from_afar_reassured" to "Move in under cover",
                "what_they_see_stealthy" to "Get out",
                "c8_the_horn" to "Walk out by the tithe arch",
                "c8_long_night" to "Write to Halvard",
                "what_must_be_risked" to "properly, or not at all"
            )
        )
        assertTrue(first.flags.containsAll(setOf("c8_rotation", "c8_tithe_seen", "reassured_voss", "moved_in_stealthily", "c8_exit_tithe", "c8_seal_spent", "c8_wrote_halvard", "vowed_to_return_prepared")))
        val second = ReplayKit.play(
            first.copy(sceneId = "c8_ash_camp", chapterId = "chapter_8"),
            stop = { isGoal(it) },
            pick = picker(
                "c8_ash_camp" to "Keep your own counsel",
                "c8_road_hub_one" to "->c8_chapel",
                "c8_chapel" to "Go down and sleep",
                "c8_road_hub_two" to "->c8_cart",
                "c8_cart" to "Take a share",
                "the_road_to_ashfall" to "I know what I'm risking",
                "the_sanctum_from_afar_acknowledged" to "Circle wide",
                "what_they_see_wide" to "Get out",
                "c8_the_horn" to "Stand and fight",
                "c8_long_night" to "Mark the way back",
                "what_must_be_risked" to "Keep moving"
            )
        )
        val gone = setOf("c8_rotation", "c8_tithe_seen", "reassured_voss", "moved_in_stealthily", "c8_exit_tithe", "c8_seal_spent", "c8_wrote_halvard", "vowed_to_return_prepared")
        assertEquals("first-telling flags survive: ${second.flags intersect gone}", emptySet<String>(), second.flags intersect gone)
        assertTrue(second.flags.containsAll(setOf("c8_cart_robbed", "acknowledged_the_risk", "circled_wide", "c8_marked_route", "undecided_on_voss")))
        assertTrue(second.flags.intersect(HUB_MARKERS).isEmpty())
        assertEquals(1, second.flags.count { it in C8_NIGHT_FAMILY })
    }

    @Test
    fun `fifteen hundred varied tellings reach Chapter 9 with a consistent set of flags`() {
        val random = java.util.Random(0x8008L)
        repeat(1500) { run ->
            val flags = HashSet<String>()
            flags += rungOrder[random.nextInt(3)]
            if (random.nextBoolean()) flags += "earned_voss_seal"
            if (random.nextBoolean()) flags += "trusted_voss_fully"
            var s = at(
                "c8_ash_camp", flags = flags, courage = 1 + random.nextInt(9), honour = random.nextInt(8),
                gold = random.nextInt(90), health = 200 + random.nextInt(300)
            )
            var steps = 0
            while (!isGoal(s)) {
                check(steps++ < 140) { "run $run never finished; stuck at ${s.sceneId}" }
                val n = node(s.sceneId)
                s = if (n.combatEncounterId != null) {
                    winOrLose(s, win = random.nextBoolean())
                } else {
                    val choices = visible(s)
                    check(choices.isNotEmpty()) { "run $run dead end at ${n.id} with ${s.flags}" }
                    s.applyChoice(choices[random.nextInt(choices.size)])
                }
            }
            val f = s.flags
            assertEquals("run $run road", 1, f.count { it in C8_ROAD_FAMILY })
            assertEquals("run $run approach", 1, f.count { it in C8_APPROACH_FAMILY })
            assertEquals("run $run vow", 1, f.count { it in C8_VOW_FAMILY })
            assertEquals("run $run night", 1, f.count { it in C8_NIGHT_FAMILY })
            assertTrue("run $run exit", f.count { it in C8_EXIT_FAMILY } <= 1)
            assertEquals("run $run rung", 1, f.count { it in C7_RUNG_FAMILY })
            assertEquals("run $run markers", emptySet<String>(), f.intersect(HUB_MARKERS))
            assertEquals("run $run seal spent only by the arch", "c8_exit_tithe" in f, "c8_seal_spent" in f)
            assertTrue("run $run plans", "torn_sanctum_plans" in s.inventory && "stole_the_schematic" in f)
            assertTrue("run $run tithe and robbery are exclusive", !("c8_tithe_seen" in f && "c8_cart_robbed" in f))
            if ("c8_ash_farmers" in f) assertTrue("run $run farmers", "c8_cart_robbed" !in f)
            if ("c8_exit_ramp" in f || "c8_exit_tithe" in f) assertTrue("run $run no sentinel prize", "Out of the Ash" !in s.trophies)
            if ("c8_voss_cells" in f) {
                assertTrue("run $run cells need the top rung", "c7_rung_trusted" in f)
                assertTrue("run $run cells are never learned at the arch", "c8_exit_tithe" !in f)
                assertTrue("run $run the trophy goes with the cells", "Her Choice" in s.trophies)
            }
            assertTrue("run $run gold", s.gold >= 0)
        }
    }
}
