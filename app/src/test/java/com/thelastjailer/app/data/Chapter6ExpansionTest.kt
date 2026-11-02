package com.thelastjailer.app.data

import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.ARRIVE
import com.thelastjailer.app.data.Chapter6Kit.KEPT
import com.thelastjailer.app.data.Chapter6Kit.ROAD_STOPS
import com.thelastjailer.app.data.Chapter6Kit.SIGHTED
import com.thelastjailer.app.data.Chapter6Kit.TOLD
import com.thelastjailer.app.data.Chapter6Kit.YARD_SPOTS
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.explore
import com.thelastjailer.app.data.Chapter6Kit.healthAfter
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.data.Chapter6Kit.winOrLose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 6 as written: the north road (a three-stop hub of which two can be made), Greymoor's yard (a
 * three-place hub of which two can be seen), the fight or the pinning that replaces it, the two defeat
 * endings, and the three verdicts with their prices. These tests pin the routing and the gates, what each
 * decision costs and gives, and that every route reaches Chapter 7.
 */
class Chapter6ExpansionTest {

    // ---------------------------------------------------------------------------------------------
    // The opening and Halvard's account
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the methodical road gives exactly one account of Kestrel, whatever Chapter 5 recorded`() {
        val expected = mapOf(
            emptySet<String>() to "c6_maps_told",
            setOf(TOLD) to "c6_maps_told",
            setOf(KEPT) to "c6_maps_unsaid"
        )
        expected.forEach { (flags, scene) ->
            val s = at("the_road_north_deliberate", flags = flags)
            assertEquals("flags $flags", listOf(scene), visible(s).map { it.nextNodeId })
        }
    }

    @Test
    fun `both accounts lead to the road and record which one was given`() {
        val told = take(at("c6_maps_told"), "c6_road_hub_one")
        assertTrue("c6_halvard_confided" in told.flags)
        assertFalse("c6_halvard_reticent" in told.flags)
        val unsaid = take(at("c6_maps_unsaid"), "c6_road_hub_one")
        assertTrue("c6_halvard_reticent" in unsaid.flags)
        assertFalse("c6_halvard_confided" in unsaid.flags)
    }

    @Test
    fun `the impatient road skips Halvard's account and goes straight to the road`() {
        val s = at("the_road_north_volunteered")
        assertEquals(listOf("c6_road_hub_one"), visible(s).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // The north road
    // ---------------------------------------------------------------------------------------------

    private fun roadWalks(courage: Int) = explore(at("c6_road_hub_one", courage = courage), { it.sceneId == ARRIVE })

    @Test
    fun `the road is a three-stop hub of which exactly two are made`() {
        listOf(99 to 48, 5 to 48, 4 to 28, 3 to 18, 1 to 18).forEach { (courage, count) ->
            val walks = roadWalks(courage)
            assertEquals("walks at Courage $courage", count, walks.size)
            walks.forEach { (trail, end) ->
                val stops = trail.filter { it in ROAD_STOPS }
                assertEquals("two different stops: $trail", 2, stops.toSet().size)
                assertEquals("each made once: $trail", 2, stops.size)
                assertEquals(1, trail.count { it == "c6_road_hub_two" })
                assertEquals(2, end.flags.count { it.startsWith("c6_stop_") })
                assertTrue("c6_road_second" in end.flags)
            }
        }
    }

    @Test
    fun `the first hub offers all three stops and the second offers only the other two`() {
        val one = at("c6_road_hub_one")
        assertEquals(listOf("c6_tollhouse", "c6_cairn", "c6_camp"), visible(one).map { it.nextNodeId })
        ROAD_STOPS.forEach { first ->
            val two = backTo(take(one, first), "c6_road_hub_two")
            assertEquals(ROAD_STOPS - first, visible(two).map { it.nextNodeId }.toSet())
        }
    }

    /** Finishes a stop by its first choice at every step until [hub] is reached again. */
    private fun backTo(afterStop: GameState, hub: String): GameState {
        var s = afterStop
        var guard = 0
        while (s.sceneId != hub) {
            check(guard++ < 6) { "never got back to $hub from ${afterStop.sceneId}" }
            s = s.applyChoice(visible(s).first())
        }
        return s
    }

    @Test
    fun `the third stop can never be made`() {
        roadWalks(99).forEach { (_, end) ->
            assertTrue(visible(end).none { it.nextNodeId in ROAD_STOPS })
        }
    }

    @Test
    fun `the ford is gated by Courage and always has a way through`() {
        val gated = mapOf(1 to setOf("c6_camp_paid"), 3 to setOf("c6_camp_paid"), 4 to setOf("c6_camp_sneaked", "c6_camp_paid"),
            5 to setOf("c6_camp_sneaked", "c6_camp_paid", "c6_camp_scout"), 12 to setOf("c6_camp_sneaked", "c6_camp_paid", "c6_camp_scout"))
        gated.forEach { (courage, targets) ->
            val offered = visible(at("c6_camp", courage = courage)).map { it.nextNodeId }.toSet()
            assertEquals("Courage $courage", targets, offered)
        }
    }

    @Test
    fun `helping at the tollhouse costs a night and fifteen gold and gives Honour, the method, the nails and a trophy`() {
        val start = at("c6_tollhouse", courage = 1, honour = 0, gold = 100)
        val helped = take(start, "c6_tollhouse_helped", "Stay the night")
        assertEquals(1, helped.honour)
        assertEquals(85, helped.gold)
        assertTrue("Kestrel's Apprentice" in helped.trophies)
        assertTrue(helped.flags.containsAll(setOf("c6_helped_refugees", "c6_met_tam")))
        assertFalse("c6_pressed_edda" in helped.flags)

        val offered = take(helped, "c6_road_hub_two", "answer to Halvard for them")
        val promised = Chapter6Kit.takeLabel(helped, "Take the satchel, and promise Tam you will.").also { assertFalse("c6_offered_the_hold" in it.flags) }
        val silent = take(helped, "c6_road_hub_two", "promise nothing")
        listOf(offered, promised, silent).forEach {
            assertTrue(it.flags.containsAll(setOf("c6_nail_method", "c6_has_ward_nails")))
        }
        assertTrue(offered.flags.containsAll(setOf("c6_promised_tam", "c6_offered_the_hold")))
        assertTrue("c6_promised_tam" in promised.flags)
        assertFalse("c6_promised_tam" in silent.flags)
        assertFalse("c6_offered_the_hold" in silent.flags)
    }

    @Test
    fun `pressing on at the tollhouse costs nothing and gives only what Edda knows`() {
        val start = at("c6_tollhouse", courage = 1, honour = 0, gold = 100)
        val pressed = take(start, "c6_tollhouse_pressed")
        assertEquals(0, pressed.honour)
        assertEquals(100, pressed.gold)
        assertEquals(setOf("c6_pressed_edda"), pressed.flags)
        assertTrue(pressed.trophies.isEmpty())
        val onward = take(pressed, "c6_road_hub_two")
        assertEquals(setOf("c6_pressed_edda"), onward.flags)
    }

    @Test
    fun `the cairn gives the nails and Kestrel's note but not the method`() {
        val s = take(at("c6_cairn", gold = 100), "c6_road_hub_two")
        assertTrue(s.flags.containsAll(setOf("c6_took_cairn_note", "c6_has_ward_nails")))
        assertFalse("c6_nail_method" in s.flags)
        assertEquals(100, s.gold)
    }

    @Test
    fun `each way past the ford has its own price`() {
        val sneaked = take(at("c6_camp", courage = 4, gold = 100), "c6_camp_sneaked")
        assertEquals(5, sneaked.courage)
        assertEquals(100, sneaked.gold)
        val paid = take(at("c6_camp", courage = 4, gold = 100), "c6_camp_paid")
        assertEquals(4, paid.courage)
        assertEquals(80, paid.gold)
        val poor = take(at("c6_camp", courage = 1, gold = 5), "c6_camp_paid")
        assertEquals("a short purse is emptied, never made negative", 0, poor.gold)
        val scout = take(at("c6_camp", courage = 5, honour = 0), "c6_camp_scout")
        assertEquals(0, scout.honour)
        val freed = take(scout, "c6_road_hub_two", "Let him go")
        assertEquals(1, freed.honour)
        assertTrue(freed.flags.containsAll(setOf("c6_scout_taken", "c6_scout_freed")))
        val bound = take(scout, "c6_road_hub_two", "Bind him")
        assertEquals(0, bound.honour)
        assertTrue(bound.flags.containsAll(setOf("c6_scout_taken", "c6_scout_bound")))
        assertFalse("c6_scout_freed" in bound.flags)
    }

    @Test
    fun `after the second stop the road ends at Greymoor and Greymoor leads to the gate`() {
        assertEquals(listOf("the_second_gate"), visible(at(ARRIVE)).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // The warden and the locket
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `either way of meeting the warden ends with the locket and the yard, not the fight`() {
        listOf("what_the_warden_left_knelt", "what_the_warden_left_searched").forEach { id ->
            val s = at(id)
            val choice = visible(s).single()
            assertEquals("c6_ruin_hub_one", choice.nextNodeId)
            assertEquals(listOf("kestrels_locket"), choice.consequences.grantItemIds)
            assertTrue("took_kestrels_locket" in choice.consequences.setFlags)
            assertTrue(s.applyChoice(choice).inventory.contains("kestrels_locket"))
        }
    }

    @Test
    fun `the locket is carried and nothing in the chapter buries or leaves it`() {
        val grants = chapter6Nodes.flatMap { n -> n.choices.map { n.id to it } }
            .filter { "kestrels_locket" in it.second.consequences.grantItemIds }
        assertEquals(setOf("what_the_warden_left_knelt", "what_the_warden_left_searched"), grants.map { it.first }.toSet())
        assertTrue(chapter6Nodes.none { n -> n.choices.any { c -> c.consequences.setFlags.any { it.contains("locket_buried") || it.contains("locket_left") } } })
    }

    @Test
    fun `Kestrel's ledger is read where it lies and never taken`() {
        val grants = chapter6Nodes.flatMap { n -> n.choices.flatMap { it.consequences.grantItemIds } }
        assertFalse("greymoor_wardens_ledger" in grants)
        val ledger = node("c6_ledger_read").narrativeText
        assertTrue("the entry stops in the middle of a sentence", ledger.contains("trust that nobody\""))
        assertFalse(ledger.contains("trust that nobody."))
        assertTrue(ledger.contains("exactly as it was"))
    }

    // ---------------------------------------------------------------------------------------------
    // The yard
    // ---------------------------------------------------------------------------------------------

    private fun yardWalks(flags: Set<String> = emptySet()) =
        explore(at("c6_ruin_hub_one", flags = flags), { it.sceneId == SIGHTED })

    @Test
    fun `the yard is a three-place hub of which exactly two are seen`() {
        listOf(emptySet<String>(), setOf("c6_pressed_edda"), setOf("c6_promised_tam"), setOf("c6_pressed_edda", "c6_promised_tam")).forEach { flags ->
            val walks = yardWalks(flags)
            assertEquals("walks with $flags", 24, walks.size)
            walks.forEach { (trail, end) ->
                val spots = trail.filter { it in YARD_SPOTS }
                assertEquals(trail.toString(), 2, spots.toSet().size)
                assertEquals(trail.toString(), 2, spots.size)
                assertEquals(1, trail.count { it == "c6_ruin_hub_two" })
                assertEquals(2, end.flags.count { it.startsWith("c6_spot_") })
                assertTrue("c6_ruin_second" in end.flags)
            }
        }
    }

    @Test
    fun `the yard offers all three places first and the other two second`() {
        val one = at("c6_ruin_hub_one")
        assertEquals(listOf("c6_watchpost", "c6_cellar", "c6_garden"), visible(one).map { it.nextNodeId })
        YARD_SPOTS.forEach { first ->
            val second = backTo(take(one, first), "c6_ruin_hub_two")
            assertEquals(YARD_SPOTS - first, visible(second).map { it.nextNodeId }.toSet())
        }
    }

    @Test
    fun `reading the ledger teaches the method and earns its trophy, while leaving it unread does neither`() {
        val post = at("c6_watchpost")
        val read = take(post, "c6_ledger_read")
        assertTrue(read.flags.containsAll(setOf("c6_read_ledger", "c6_nail_method")))
        assertTrue("The Ledger, Read to the End" in read.trophies)
        val left = take(post, "c6_ruin_hub_two", "Leave her book unread")
        assertFalse("c6_read_ledger" in left.flags)
        assertFalse("c6_nail_method" in left.flags)
        assertTrue(left.trophies.isEmpty())
    }

    @Test
    fun `the cellar costs hands unless Edda said how the hatch sticks, and pays the same either way`() {
        val hatch = at("c6_cellar", health = 500, gold = 40)
        val forced = take(hatch.copy(flags = emptySet()), "c6_breach_found")
        assertEquals(healthAfter(500, -6), forced.health)
        val lifted = take(hatch.copy(flags = setOf("c6_pressed_edda")), "c6_breach_found")
        assertEquals(500, lifted.health)
        listOf(forced, lifted).forEach {
            assertEquals(65, it.gold)
            assertTrue("healing_draught" in it.inventory)
            assertTrue("c6_inspected_breach" in it.flags)
        }
        assertEquals("only one way down is ever offered", 2, visible(hatch).size)
        assertEquals(2, visible(hatch.copy(flags = setOf("c6_pressed_edda"))).size)
        val unmoved = take(hatch, "c6_ruin_hub_two", "Leave the hatch shut")
        assertFalse("c6_inspected_breach" in unmoved.flags)
        assertEquals(40, unmoved.gold)
        assertFalse("healing_draught" in unmoved.inventory)
    }

    @Test
    fun `burying Kestrel costs strength and gives Honour, and the promise to Tam changes only the words`() {
        val row = at("c6_garden", honour = 0, health = 500)
        val plain = take(row, "c6_buried")
        assertEquals(1, plain.honour)
        assertEquals(healthAfter(500, -6), plain.health)
        assertTrue("Buried Properly" in plain.trophies)
        assertTrue("c6_buried_kestrel" in plain.flags)
        val promised = take(row.copy(flags = setOf("c6_promised_tam")), "c6_buried_promised")
        assertEquals(1, promised.honour)
        assertEquals(healthAfter(500, -6), promised.health)
        assertTrue("c6_buried_kestrel" in promised.flags)
        assertTrue(visible(row.copy(flags = setOf("c6_promised_tam"))).none { it.nextNodeId == "c6_buried" })
        assertTrue(visible(row).none { it.nextNodeId == "c6_buried_promised" })
        val left = take(row, "c6_ruin_hub_two", "She chose that ground")
        assertEquals(0, left.honour)
        assertFalse("c6_buried_kestrel" in left.flags)
    }

    // ---------------------------------------------------------------------------------------------
    // The dusk: fight it or pin it
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the fight is always offered and the pinning needs the method, the nails and Courage 5`() {
        for (method in listOf(true, false)) for (nails in listOf(true, false)) for (courage in listOf(4, 5, 9)) {
            val flags = buildSet {
                if (method) add("c6_nail_method")
                if (nails) add("c6_has_ward_nails")
            }
            val targets = visible(at(SIGHTED, flags = flags, courage = courage)).map { it.nextNodeId }
            assertTrue("fight, method=$method nails=$nails courage=$courage", "the_unbound" in targets)
            assertEquals(
                "pin, method=$method nails=$nails courage=$courage",
                method && nails && courage >= 5,
                "c6_pin_held" in targets
            )
        }
    }

    @Test
    fun `pinning costs no health and gives Courage, experience, the keepsake nails and a trophy`() {
        val dusk = at(SIGHTED, flags = setOf("c6_nail_method", "c6_has_ward_nails"), courage = 5, health = 500)
        val pinned = take(dusk, "c6_pin_held")
        assertEquals(6, pinned.courage)
        assertEquals("120 experience from a fresh start is one level and 20 over", 2, pinned.level)
        assertEquals(20, pinned.xp)
        assertEquals(500 + 9, pinned.maxHealth)
        assertEquals("no damage: only the scene's one point of recovery", 501, pinned.health)
        assertTrue("ward_nails" in pinned.inventory)
        assertTrue("Nails in the Dark" in pinned.trophies)
        assertTrue("c6_unbound_pinned" in pinned.flags)
        assertNull("the pinning is not a fight", node("c6_pin_held").combatEncounterId)
        assertEquals(listOf("c6_pin_decided"), visible(pinned).map { it.nextNodeId })
    }

    @Test
    fun `the nails are a keepsake granted only by the pinning and carry no combat effect`() {
        val item = ItemCatalog.get("ward_nails")
        assertNotNull(item)
        assertNull(item!!.combatEffect)
        assertFalse(ItemCatalog.isUniqueEquipment("ward_nails"))
        val granters = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
            .flatMap { n -> n.choices.filter { "ward_nails" in it.consequences.grantItemIds }.map { n.id to it.nextNodeId } }
        assertEquals(listOf(SIGHTED to "c6_pin_held"), granters)
    }

    @Test
    fun `the fight is the original encounter and still ends where it always did`() {
        assertEquals("unbound_encounter", node("the_unbound").combatEncounterId)
        val encounter = CombatRepository.encounter("unbound_encounter")
        assertEquals("what_must_be_decided", encounter.victoryNodeId)
        assertEquals("unbound_defeat", encounter.defeatNodeId)
        assertEquals(150, encounter.xpReward)
        assertEquals(40, encounter.goldReward)
        val won = winOrLose(at("the_unbound", health = 500), win = true)
        assertEquals("what_must_be_decided", won.sceneId)
        assertTrue("What Got Out" in won.trophies)
        val lost = winOrLose(at("the_unbound", health = 500), win = false)
        assertEquals("unbound_defeat", lost.sceneId)
    }

    // ---------------------------------------------------------------------------------------------
    // Defeat
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a lost fight ends with Tam if the refugees were helped and alone if not`() {
        val afterDefeat = winOrLose(at("the_unbound", health = 300, flags = setOf("c6_met_tam")), win = false)
        assertEquals("unbound_defeat", afterDefeat.sceneId)
        assertEquals(listOf("c6_defeat_tam"), visible(afterDefeat).map { it.nextNodeId })
        val withTam = take(afterDefeat, "c6_defeat_tam")
        assertTrue("lost_to_unbound_horror" in withTam.flags)
        val up = take(withTam, "what_must_be_decided")
        assertEquals(healthAfter(withTam.health, 20), up.health)
        assertTrue("c6_tam_followed" in up.flags)

        val alone = winOrLose(at("the_unbound", health = 300), win = false)
        assertEquals(listOf("c6_defeat_alone"), visible(alone).map { it.nextNodeId })
        val aloneUp = take(take(alone, "c6_defeat_alone"), "what_must_be_decided")
        assertEquals(healthAfter(alone.health + 1, 8), aloneUp.health)
        assertFalse("c6_tam_followed" in aloneUp.flags)
        assertTrue("lost_to_unbound_horror" in aloneUp.flags)
    }

    @Test
    fun `defeat gives no reward and Tam is named only where she is remembered or arrives after the fight`() {
        listOf("c6_defeat_tam", "c6_defeat_alone").forEach { id ->
            assertTrue(id, node(id).choices.all { c -> c.consequences.grantItemIds.isEmpty() && c.consequences.unlockTrophy == null })
        }
        val namingTam = chapter6Nodes.filter { Regex("\\bTam\\b").containsMatchIn(it.narrativeText) }.map { it.id }.toSet()
        assertEquals(
            setOf("c6_tollhouse_helped", "c6_ledger_read", "c6_buried_promised", "c6_defeat_tam"),
            namingTam
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The verdict and its price
    // ---------------------------------------------------------------------------------------------

    private val verdicts = setOf("sealed_greymoor_personally", "reported_greymoor_honestly", "left_greymoor_as_is")

    @Test
    fun `after the fight each verdict pays its own price`() {
        val decided = at("what_must_be_decided", courage = 5, honour = 4, health = 500, gold = 100)
        assertEquals(3, visible(decided).size)

        val sealed = take(decided, "c6_cost_sealed")
        assertEquals(6, sealed.courage)
        assertEquals("sealed_greymoor_personally", sealed.flags.single { it in verdicts })
        val sealedPaid = take(sealed, "chapter6_end_sealed")
        assertEquals(healthAfter(sealed.health, -10), sealedPaid.health)
        assertEquals(100, sealedPaid.gold)

        val reported = take(decided, "c6_cost_reported")
        assertEquals(5, reported.honour)
        val reportedPaid = take(reported, "chapter6_end_reported")
        assertEquals(90, reportedPaid.gold)
        assertEquals(healthAfter(reported.health, 0), reportedPaid.health)

        val left = take(decided, "chapter6_end_left")
        assertEquals(5, left.courage)
        assertEquals(4, left.honour)
        assertEquals(100, left.gold)
        assertEquals("left_greymoor_as_is", left.flags.single { it in verdicts })
    }

    @Test
    fun `after the pinning the seal needs no price, the report still does and leaving is free`() {
        val decided = at("c6_pin_decided", courage = 6, honour = 4, health = 500, gold = 100)
        assertEquals(3, visible(decided).size)
        val sealed = take(decided, "chapter6_end_sealed")
        assertEquals(7, sealed.courage)
        assertEquals(500, sealed.health)
        assertEquals(100, sealed.gold)
        assertEquals("sealed_greymoor_personally", sealed.flags.single { it in verdicts })
        val reported = take(decided, "c6_cost_reported")
        assertEquals(90, take(reported, "chapter6_end_reported").gold)
        val left = take(decided, "chapter6_end_left")
        assertEquals("left_greymoor_as_is", left.flags.single { it in verdicts })
    }

    @Test
    fun `the verdicts keep their stat gates and leaving is always possible`() {
        listOf("what_must_be_decided", "c6_pin_decided").forEach { id ->
            val weak = visible(at(id, courage = 1, honour = 0)).map { it.nextNodeId }
            assertEquals(id, listOf("chapter6_end_left"), weak)
            val brave = visible(at(id, courage = 5, honour = 0)).map { it.nextNodeId }.toSet()
            assertTrue(id, brave.any { it == "c6_cost_sealed" || it == "chapter6_end_sealed" })
            assertFalse(id, brave.contains("c6_cost_reported"))
            val honourable = visible(at(id, courage = 1, honour = 4)).map { it.nextNodeId }.toSet()
            assertEquals(id, setOf("c6_cost_reported", "chapter6_end_left"), honourable)
        }
    }

    @Test
    fun `every verdict clears the other two`() {
        listOf("what_must_be_decided", "c6_pin_decided").forEach { id ->
            visible(at(id, flags = verdicts)).forEach { choice ->
                val after = at(id, flags = verdicts).applyChoice(choice)
                assertEquals("${choice.label} from $id", 1, after.flags.count { it in verdicts })
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Corrections from the Chapter 6 review
    // ---------------------------------------------------------------------------------------------

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    @Test
    fun `the refuge promise is Kaelen's own and never says Halvard has agreed`() {
        val helped = node("c6_tollhouse_helped")
        val offers = helped.choices.filter { "c6_offered_the_hold" in it.consequences.setFlags }
        assertEquals("the offer exists as a gated pair", 2, offers.size)
        offers.forEach {
            assertTrue(it.label, it.label.contains("you will answer to Halvard for them"))
            assertFalse(it.label, it.label.contains("Stonebeard will take"))
        }
        assertTrue(collapse(helped.narrativeText).contains("Stonebeard's door is Halvard's to open, not Kaelen's"))
        val claims = listOf("Halvard has agreed", "Halvard agreed", "Halvard said yes", "Halvard has said", "Halvard has offered")
        val bad = chapter6Nodes.filter { n -> claims.any { collapse(n.narrativeText).contains(it) || n.choices.any { c -> c.label.contains(it) } } }
        assertTrue("A scene claims Halvard's agreement: ${bad.map { it.id }}", bad.isEmpty())
        assertEquals("the Journal line says Kaelen answers for them", "I told Edda that if her people had nowhere else, I would answer to Halvard for them.",
            FlagDescriptions.entries.single { it.flag == "c6_offered_the_hold" }.text)
    }

    @Test
    fun `the ford toll takes twenty or whatever the purse holds, and the scene says so`() {
        listOf(0 to 0, 5 to 0, 19 to 0, 20 to 0, 21 to 1, 100 to 80).forEach { (gold, left) ->
            assertEquals("Gold $gold", left, take(at("c6_camp", courage = 1, gold = gold), "c6_camp_paid").gold)
        }
        val text = collapse(node("c6_camp_paid").narrativeText)
        assertTrue(text.contains("counts twenty into his palm, or as much of twenty as he has"))
        assertFalse("no claim that the full price was paid", text.contains("Kaelen pays what he can find toward it"))
        // From the starting purse of 25, a night with Edda's people leaves 10, and the ford takes the last of it.
        val afterHelp = take(at("c6_tollhouse", courage = 1, gold = 25), "c6_tollhouse_helped", "Stay the night")
        assertEquals(10, afterHelp.gold)
        assertEquals(0, take(at("c6_camp", courage = 1, gold = afterHelp.gold), "c6_camp_paid").gold)
        assertTrue(collapse(node("c6_tollhouse_helped").narrativeText).contains("puts what he can spare into the man's palm"))
    }

    @Test
    fun `the report fee takes ten or whatever the purse holds, and the scene says so`() {
        listOf(0 to 0, 4 to 0, 10 to 0, 11 to 1, 100 to 90).forEach { (gold, left) ->
            assertEquals("Gold $gold", left, take(at("c6_cost_reported", gold = gold), "chapter6_end_reported").gold)
        }
        assertTrue(collapse(node("c6_cost_reported").narrativeText).contains("Kaelen pays it, or what his purse can find toward it"))
    }

    @Test
    fun `Tam follows from the tollhouse and reaches the village at dusk, not days behind`() {
        val text = collapse(node("c6_defeat_tam").narrativeText)
        assertFalse(text.contains("three days"))
        assertTrue(text.contains("a morning behind you since the tollhouse"))
        assertTrue(text.contains("reached the village as the light went"))
        assertTrue(text.contains("I heard it from the street"))
        assertEquals("only a Kaelen who stayed the night at the tollhouse meets her", "c6_met_tam",
            node("unbound_defeat").choices.single { it.nextNodeId == "c6_defeat_tam" }.requirements!!.requiredFlags.single())
    }

    @Test
    fun `the pinning leaves three nails holding the crack, keeps the rest, and the gate's own ward stays dark`() {
        val text = collapse(node("c6_pin_held").narrativeText)
        assertTrue(text.contains("He pulls none of the three. The nails he did not need go back into his pocket."))
        assertTrue(text.contains("the nails stand in a row along the crack, holding it shut"))
        assertTrue(text.contains("its ward went dark before the creature ever reached the yard, and it stays dark, and the nails keep only what came through"))
        val item = ItemCatalog.get("ward_nails")!!
        assertTrue(item.description, item.description.startsWith("The nails left over once three were driven into Greymoor's crack and left there"))
        assertTrue(collapse(node("c6_pin_decided").narrativeText).contains("A gate with three nails through its crack"))
    }

    // ---------------------------------------------------------------------------------------------
    // Whole-chapter soak
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `two thousand varied tellings all reach Chapter 7 with a consistent set of flags and rewards`() {
        val random = java.util.Random(0x6D006L)
        repeat(2000) { run ->
            val start = at(
                Chapter6Kit.ROADS,
                flags = when (random.nextInt(3)) { 0 -> setOf(TOLD); 1 -> setOf(KEPT); else -> emptySet() },
                courage = 1 + random.nextInt(9),
                honour = random.nextInt(7),
                gold = random.nextInt(90),
                health = 200 + random.nextInt(300)
            )
            var s = start
            var steps = 0
            while (s.sceneId != Chapter6Kit.NEXT_CHAPTER) {
                check(steps++ < 160) { "run $run never finished; stuck at ${s.sceneId}" }
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
            assertEquals("run $run opening", 1, f.count { it == "chose_to_travel" || it == "chose_deliberately" })
            assertEquals("run $run approach", 1, f.count { it == "knelt_for_the_warden" || it == "searched_the_gate_first" })
            assertEquals("run $run verdict", 1, f.count { it in verdicts })
            assertEquals("run $run road stops", 2, f.count { it.startsWith("c6_stop_") })
            assertEquals("run $run yard places", 2, f.count { it.startsWith("c6_spot_") })
            assertTrue("run $run", ("c6_unbound_pinned" in f) == ("ward_nails" in s.inventory))
            assertTrue("run $run", ("c6_unbound_pinned" in f) == ("Nails in the Dark" in s.trophies))
            assertFalse("run $run a pinned creature is never also a lost fight", "c6_unbound_pinned" in f && "lost_to_unbound_horror" in f)
            if ("c6_unbound_pinned" in f) assertTrue("run $run", f.containsAll(setOf("c6_nail_method", "c6_has_ward_nails")))
            if ("c6_tam_followed" in f) assertTrue("run $run", "c6_met_tam" in f && "lost_to_unbound_horror" in f)
            if ("c6_met_tam" in f) assertTrue("run $run", "c6_helped_refugees" in f && "c6_pressed_edda" !in f)
            assertTrue("run $run", ("c6_scout_taken" in f) == ("c6_scout_freed" in f || "c6_scout_bound" in f))
            assertFalse("run $run", "c6_scout_freed" in f && "c6_scout_bound" in f)
            assertFalse("run $run", "c6_halvard_confided" in f && "c6_halvard_reticent" in f)
            assertTrue("run $run", (f.contains("c6_halvard_confided") || f.contains("c6_halvard_reticent")) == f.contains("chose_deliberately"))
            assertTrue("run $run gold never negative", s.gold >= 0)
        }
    }
}
