package com.thelastjailer.app.data

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.explore
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.resolveCombat
import java.security.MessageDigest
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 10 as written: the night before the column (four places, two or three of them seen), the Line (what the Hold has, who
 * is in it, where Kaelen stands), three answers that recognise Chapter 5, and the quiet after (who carries Halvard, what was
 * written for him). Halvard's death is mandatory and these tests are its guard: every route passes the fall and the last words,
 * nothing new sets, clears or reads the death, the encounter that leads to it is untouched, and the eleven scenes that shipped
 * are word for word as before.
 */
class Chapter10ExpansionTest {

    private val start = "the_last_two"
    private val end = "chapter10_end"
    private val fall = "halvard_falls"
    private val lastWords = "what_halvard_leaves_behind"

    private fun words(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(words(text).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private val shippedHashes = mapOf(
        "the_last_two" to "2a6c75b77b41b0f4e309ecbff056672b71202ae315c035efa049556052e4f6a7",
        "ilsevet_arrives_fortified" to "d0298114c3eb82badcc6465e4411d516ad0ed2f2956a8aad5c59334fb1766742",
        "ilsevet_arrives_hard" to "1dcabd80ccc79005ff69954a89833ab0a2828f363d3062d1f6357ce538e0316b",
        "kaelens_answer" to "75f58ac2e300ffd13cba5f5f7c29558f223c18e92a3102407b068616746dc8b9",
        "the_gate_falls_defiant" to "3fc2c27b27ef6de6a79260adbaa1f4c3f90f18a6bd16791bff71020ccf639b7f",
        "the_gate_falls_principled" to "b12149ea2d8f873ceff4e78693800d4c5c5f7fa59930d18278e2f1d784177717",
        "the_gate_falls_silent" to "1a99cbcc6d474bf12aeea7ab97893b88d68c4903cff21f552e39eb50bc8917e1",
        "stonebeard_siege_defeat" to "48ad2e04d9afacf68d4ba41ef29417932a8be937756754b323c263eab2337afd",
        "halvard_falls" to "480382b23a2d93dbf1edc84b1b4fab8794caf98b5989e744e1b51751d98da120",
        "what_halvard_leaves_behind" to "d1a2126d72d591d945919deaa07ecb466808e5af80cf6b24d2f6b506a02f3131",
        "chapter10_end" to "53120c1f9ac1f562f3e931741e1139031ddc0d2d8ac90205382334293de02a09"
    )

    private val newScenes get() = StoryRepository.nodesInChapter("chapter_10").filter { it.id.startsWith("c10_") }

    private fun placeOf(sceneId: String): String? = when {
        sceneId.startsWith("c10_forge") -> "forge"
        sceneId.startsWith("c10_wall") -> "wall"
        sceneId.startsWith("c10_people") || sceneId == "c10_tam" || sceneId == "c10_carter" -> "people"
        sceneId == "c10_page" -> "page"
        else -> null
    }

    private fun entryTargets(s: GameState): Map<String, String> =
        visible(s).filter { it.label != "Enough. Go up to the gate." }.associate { it.label to it.nextNodeId }

    private val richFlags = setOf(
        "c7_traded", "c7_rung_working", "c7_hold_terms", "c7_halvard_trusts_voss", "c9_stair_rules", "c6_met_tam",
        "c9_stair_people", "c7_voss_hurt_deeper", "c7_ground_stair"
    )

    // ---------------------------------------------------------------------------------------------
    // Into the chapter
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the chapter still opens on the shipped scene and each preparation leads to the night before`() {
        assertEquals(start, StoryRepository.chapters.single { it.id == "chapter_10" }.startNodeId)
        val choices = visible(at(start))
        assertEquals(listOf("c10_evening", "c10_evening"), choices.map { it.nextNodeId })
        assertEquals(setOf("fortified_for_the_last_stand"), choices[0].consequences.setFlags)
        assertEquals(setOf("prepared_to_meet_them_hard"), choices[1].consequences.setFlags)
    }

    @Test
    fun `the first preparation clears what the chapter owns and nothing from another chapter, not even the death`() {
        val stale = C10_TELLING_FLAGS + setOf("c7_rung_trusted", "c8_rotation", "halvard_died", "took_the_warden_chain", "c9_door_seal")
        val after = take(at(start, flags = stale), "c10_evening", "fortify")
        assertEquals(setOf("fortified_for_the_last_stand"), after.flags.intersect(C10_TELLING_FLAGS))
        assertTrue(after.flags.containsAll(setOf("c7_rung_trusted", "c8_rotation", "halvard_died", "took_the_warden_chain", "c9_door_seal")))
        val hard = take(at(start, flags = stale), "c10_evening", "regrets it")
        assertEquals(setOf("prepared_to_meet_them_hard"), hard.flags.intersect(C10_TELLING_FLAGS))
    }

    // ---------------------------------------------------------------------------------------------
    // The night before
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the forge holds three conversations, chosen by what passed at the forge in Chapter 7`() {
        fun forge(vararg f: String): String {
            val hits = visible(at("c10_evening", flags = f.toSet())).filter { it.label.startsWith("Go down to the forge") }
            assertEquals(f.toString(), 1, hits.size)
            return hits.single().nextNodeId
        }
        assertEquals("c10_forge_open", forge("c7_traded"))
        assertEquals("c10_forge_open", forge("c7_page"))
        assertEquals("c10_forge_kind", forge("c7_comfort"))
        assertEquals("c10_forge_kind", forge("c7_quiet"))
        assertEquals("c10_forge_shut", forge("c7_shut"))
        assertEquals("a save with no forge flag still has the forge", "c10_forge_shut", forge())
    }

    @Test
    fun `the wall holds three conversations, chosen by where Voss and Kaelen stand`() {
        fun wall(vararg f: String) = visible(at("c10_evening", flags = f.toSet())).single { it.label.startsWith("Walk the wall") }.nextNodeId
        assertEquals("c10_wall_wary", wall("c7_rung_wary"))
        assertEquals("a save with no rung counts as the lowest", "c10_wall_wary", wall())
        assertEquals("c10_wall_working", wall("c7_rung_working"))
        assertEquals("c10_wall_trusted", wall("c7_rung_trusted"))
    }

    @Test
    fun `Edda's people are at Stonebeard only where Chapter 7 left a promise made for them`() {
        fun people(vararg f: String) = visible(at("c10_evening", flags = f.toSet())).filter { it.label.startsWith("Look in on Edda") }.map { it.nextNodeId }
        assertEquals(listOf("c10_people_inside"), people("c7_hold_terms"))
        assertEquals(listOf("c10_people_outside"), people("c7_hold_refused"))
        assertEquals(listOf("c10_people_unspent"), people("c7_hold_hidden"))
        assertEquals("no promise, no people to look in on", emptyList<String>(), people())
        assertEquals(4, visible(at("c10_evening", flags = setOf("c7_hold_terms"))).size)
        assertEquals(3, visible(at("c10_evening")).size)
    }

    @Test
    fun `the night allows two or three places, each once, and no marker outlives it`() {
        listOf(richFlags, richFlags - "c7_hold_terms").forEach { flags ->
            val walks = explore(at("c10_evening", flags = flags), { it.sceneId == "c10_before_light" }, maxDepth = 14)
            assertTrue("a rich night has plenty of ways through", walks.size > 100)
            walks.forEach { (trail, endState) ->
                val places = trail.mapNotNull { placeOf(it) }
                val distinct = places.distinct()
                assertTrue(trail.toString(), distinct.size in 2..3)
                // a place is entered once, though its add-ons may follow: entries are the first scene seen of each place
                val entered = trail.filter { it in setOf("c10_forge_open", "c10_forge_kind", "c10_forge_shut", "c10_wall_wary", "c10_wall_working",
                    "c10_wall_trusted", "c10_people_inside", "c10_people_outside", "c10_people_unspent", "c10_page") }
                assertEquals(trail.toString(), entered.size, entered.map { placeOf(it) }.toSet().size)
                assertEquals("no marker outlives the night", emptySet<String>(), endState.flags.intersect(HUB_MARKERS_FOUR))
            }
        }
    }

    @Test
    fun `the third visit lets Kaelen leave after two places, and the fourth is never offered`() {
        val two = at("c10_evening_three", flags = setOf("hub_a", "hub_b", "hub_second", "c7_hold_terms"))
        assertTrue(visible(two).any { it.label == "Enough. Go up to the gate." })
        val left = takeLabel(two, "Enough. Go up to the gate.")
        assertEquals("c10_before_light", left.sceneId)
        assertEquals(emptySet<String>(), left.flags.intersect(HUB_MARKERS_FOUR))
        val onlyOne = at("c10_evening_two", flags = setOf("hub_a"))
        assertFalse(visible(onlyOne).any { it.label == "Enough. Go up to the gate." })
    }

    @Test
    fun `the forge offers the north gap to anyone who sits with him, and the offer earns a secret trophy`() {
        listOf("c10_forge_open" to "c7_traded", "c10_forge_kind" to "c7_quiet", "c10_forge_shut" to "c7_shut").forEach { (scene, flag) ->
            val talk = take(at(scene, flags = setOf(flag, "hub_a")), "c10_forge_talk")
            val gap = take(talk, "c10_forge_gap")
            assertTrue(scene, "The Last Evening" !in gap.trophies)
            val back = takeLabel(gap, "Go on.")
            assertTrue(scene, "The Last Evening" in back.trophies)
            assertEquals("c10_evening_two", back.sceneId)
        }
        // the same continue written for the second and third visits
        listOf("hub_second" to "c10_evening_three", "hub_second,hub_third" to "c10_before_light").forEach { (markers, next) ->
            val flags = markers.split(",").toSet() + "hub_a"
            val out = takeLabel(at("c10_forge_gap", flags = flags), "Go on.")
            assertEquals(next, out.sceneId)
            assertTrue("The Last Evening" in out.trophies)
        }
    }

    @Test
    fun `each conversation at the forge leads to one place where Kaelen may speak, and Halvard on Voss and the stair are there only where the earlier chapters left them`() {
        listOf("c10_forge_open", "c10_forge_kind", "c10_forge_shut").forEach { id ->
            assertEquals(id, listOf("c10_forge_talk"), visible(at(id, flags = setOf("hub_a"))).map { it.nextNodeId }.filter { it == "c10_forge_talk" })
            assertEquals(id, 1, visible(at(id, flags = setOf("hub_a"))).count { it.label == "Leave him to his iron." })
        }
        val s = at("c10_forge_talk", flags = setOf("hub_a"))
        assertEquals(listOf("c10_forge_voss_doubts"), visible(s).filter { it.label.startsWith("Ask what he makes") }.map { it.nextNodeId })
        val trusting = at("c10_forge_talk", flags = setOf("hub_a", "c7_halvard_trusts_voss"))
        assertEquals(listOf("c10_forge_voss_trusts"), visible(trusting).filter { it.label.startsWith("Ask what he makes") }.map { it.nextNodeId })
        assertTrue(visible(s).none { it.nextNodeId == "c10_forge_side" })
        assertEquals(1, visible(at("c10_forge_talk", flags = setOf("hub_a", "c9_stair_rules"))).count { it.nextNodeId == "c10_forge_side" })
    }

    @Test
    fun `the people have Tam and the carter only where Chapters 6 and 9 left them, and the carter only on the north stair`() {
        listOf("c10_people_inside", "c10_people_outside", "c10_people_unspent").forEach { id ->
            val bare = at(id, flags = setOf("hub_c"))
            assertTrue(id, visible(bare).none { it.nextNodeId == "c10_tam" || it.nextNodeId == "c10_carter" })
            assertEquals(id, 1, visible(at(id, flags = setOf("hub_c", "c6_met_tam"))).count { it.nextNodeId == "c10_tam" })
        }
        assertEquals(1, visible(at("c10_people_inside", flags = setOf("hub_c", "c9_stair_people"))).count { it.nextNodeId == "c10_carter" })
        // the carter was met on the north stair, which only the promise Halvard agreed to opens: there is no carter to speak to elsewhere
        listOf("c10_people_outside", "c10_people_unspent").forEach { id ->
            assertTrue(id, visible(at(id, flags = setOf("hub_c", "c9_stair_people"))).none { it.nextNodeId == "c10_carter" })
        }
    }

    @Test
    fun `the wall moves Voss's rung one step when Kaelen stays, never past the top, and never otherwise`() {
        listOf("c7_rung_wary" to "c7_rung_working", "c7_rung_working" to "c7_rung_trusted", "c7_rung_trusted" to "c7_rung_trusted").forEach { (from, to) ->
            val after = takeLabel(at("c10_wall_stay", flags = setOf(from, "hub_b")), "Go on.")
            assertEquals(from, setOf(to), after.flags.intersect(C7_RUNG_FAMILY))
        }
        val bare = takeLabel(at("c10_wall_stay", flags = setOf("hub_b")), "Go on.")
        assertEquals("a save with no rung starts on the lowest", setOf("c7_rung_working"), bare.flags.intersect(C7_RUNG_FAMILY))
        // nowhere else in the night does the rung change
        val walks = explore(at("c10_evening", flags = richFlags), { it.sceneId == "c10_before_light" }, maxDepth = 14)
        walks.forEach { (trail, endState) ->
            val rung = endState.flags.intersect(C7_RUNG_FAMILY)
            if ("c10_wall_stay" in trail) assertEquals(trail.toString(), setOf("c7_rung_trusted"), rung)
            else assertEquals(trail.toString(), setOf("c7_rung_working"), rung)
        }
        // the old wound is asked about only by someone who knows of it
        assertTrue(visible(at("c10_wall_working", flags = setOf("hub_b"))).none { it.nextNodeId == "c10_wall_hurt" })
        assertEquals(1, visible(at("c10_wall_working", flags = setOf("hub_b", "c7_voss_hurt_deeper"))).count { it.nextNodeId == "c10_wall_hurt" })
    }

    @Test
    fun `the page is one of four answers and the three that write something are alternatives`() {
        val page = at("c10_page", flags = setOf("hub_d"))
        assertEquals(4, visible(page).size)
        val flags = listOf("Write his name, and the name of the gate." to "c10_rites_name", "Write what he has done." to "c10_rites_deeds",
            "Write only that he held." to "c10_rites_plain")
        flags.forEach { (label, flag) ->
            val stale = at("c10_page", flags = setOf("hub_d") + C10_RITES_FAMILY)
            val after = takeLabel(stale, label)
            assertEquals(label, setOf(flag), after.flags.intersect(C10_RITES_FAMILY))
        }
        val none = takeLabel(at("c10_page", flags = setOf("hub_d")), "Tell them it is not yours to write.")
        assertEquals(emptySet<String>(), none.flags.intersect(C10_RITES_FAMILY))
    }

    // ---------------------------------------------------------------------------------------------
    // The Line
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the Line reads where the ambush was fought and who is in the Hold, and skips what was never recorded`() {
        fun next(from: String, vararg f: String) = visible(at(from, flags = f.toSet())).map { it.nextNodeId }
        assertEquals(listOf("c10_line_ground_yard"), next("c10_before_light", "c7_ground_yard"))
        assertEquals(listOf("c10_line_ground_stair"), next("c10_before_light", "c7_ground_stair"))
        assertEquals(listOf("c10_line_ground_road"), next("c10_before_light", "c7_ground_road"))
        assertEquals("no ground recorded: straight to who is in the Hold", listOf("c10_line_hold_inside"), next("c10_before_light", "c7_hold_terms"))
        assertEquals(listOf("c10_line_cover"), next("c10_before_light"))
        listOf("c10_line_ground_yard", "c10_line_ground_stair", "c10_line_ground_road").forEach { id ->
            assertEquals(listOf("c10_line_hold_inside"), next(id, "c7_hold_terms"))
            assertEquals(listOf("c10_line_hold_outside"), next(id, "c7_hold_refused"))
            assertEquals(listOf("c10_line_hold_unspent"), next(id, "c7_hold_hidden"))
            assertEquals(listOf("c10_line_cover"), next(id))
        }
        listOf("c10_line_hold_inside", "c10_line_hold_outside", "c10_line_hold_unspent").forEach {
            assertEquals(listOf("c10_line_cover"), next(it))
        }
    }

    @Test
    fun `where Kaelen stands is one of three, each meeting the column the way the preparation chose`() {
        val stands = listOf(
            Triple("Stand with Voss at the gate.", "c10_cover_voss", "COURAGE"),
            Triple("Go to the stair, to the people on it.", "c10_cover_stair", "HONOUR"),
            Triple("Stand at the forge door with the smiths.", "c10_cover_forge", "COURAGE")
        )
        listOf(true, false).forEach { fortified ->
            val prep = if (fortified) setOf("fortified_for_the_last_stand") else setOf("prepared_to_meet_them_hard")
            val expected = if (fortified) "ilsevet_arrives_fortified" else "ilsevet_arrives_hard"
            stands.forEach { (label, flag, stat) ->
                val before = at("c10_line_cover", flags = prep + C10_COVER_FAMILY, courage = 5, honour = 5)
                val after = takeLabel(before, label)
                assertEquals(label, expected, after.sceneId)
                assertEquals(label, setOf(flag), after.flags.intersect(C10_COVER_FAMILY))
                assertEquals(label, 6, if (stat == "COURAGE") after.courage else after.honour)
            }
        }
        assertEquals("a save with neither preparation meets the column hard", "ilsevet_arrives_hard",
            takeLabel(at("c10_line_cover"), "Stand with Voss at the gate.").sceneId)
        assertEquals(3, visible(at("c10_line_cover", flags = setOf("fortified_for_the_last_stand"))).size)
    }

    @Test
    fun `three answers to Ilsevet recognise what Chapter 5 left, and reach the shipped scenes with the shipped flags`() {
        assertEquals(3, visible(at("kaelens_answer")).size)
        val refused = at("kaelens_answer", flags = setOf("refused_envoy"))
        assertEquals(4, visible(refused).size)
        val r = takeLabel(refused, "\"I told your envoy no. I will tell you the same.\"")
        assertEquals("the_gate_falls_defiant", r.sceneId)
        assertTrue("defied_ilsevet" in r.flags)
        val heard = at("kaelens_answer", flags = setOf("heard_envoy_offer"))
        val h = takeLabel(heard, "\"Your envoy made me this offer. I listened then. I have finished listening.\"")
        assertEquals("the_gate_falls_principled", h.sceneId)
        assertTrue("refused_on_principle" in h.flags)
        val confronted = at("kaelens_answer", flags = setOf("confronted_envoy_with_prisoners_words"))
        val c = takeLabel(confronted, "\"Something older than you warned me you would come asking. It was right about that much.\"")
        assertEquals("the_gate_falls_defiant", c.sceneId)
        assertTrue("defied_ilsevet" in c.flags)
        // the shipped three are still there for everyone
        listOf(refused, heard, confronted).forEach { s ->
            assertEquals(setOf("the_gate_falls_defiant", "the_gate_falls_principled", "the_gate_falls_silent"),
                visible(s).map { it.nextNodeId }.toSet())
        }
    }

    // ---------------------------------------------------------------------------------------------
    // After the fall
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `taking the chain is one choice with its item and flags exactly as shipped, and it leads to the quiet after`() {
        val s = at(lastWords)
        val choice = visible(s).single()
        assertEquals("Take the chain.", choice.label)
        assertEquals("c10_carry_bridge", choice.nextNodeId)
        assertEquals(listOf("halvards_warden_chain"), choice.consequences.grantItemIds)
        assertEquals(setOf("halvard_died", "took_the_warden_chain"), choice.consequences.setFlags)
        val after = s.applyChoice(choice)
        assertEquals(1, after.inventory.count { it == "halvards_warden_chain" })
    }

    @Test
    fun `whoever Kaelen stood with carries Halvard, and the stair is read with Edda only where she is`() {
        fun carry(vararg f: String) = visible(at("c10_carry_bridge", flags = f.toSet())).map { it.nextNodeId }
        assertEquals(listOf("c10_carry_voss"), carry("c10_cover_voss"))
        assertEquals(listOf("c10_carry_forge"), carry("c10_cover_forge"))
        assertEquals(listOf("c10_carry_stair_edda"), carry("c10_cover_stair", "c7_hold_terms"))
        assertEquals(listOf("c10_carry_stair_edda"), carry("c10_cover_stair", "c7_hold_refused"))
        assertEquals(listOf("c10_carry_stair_edda"), carry("c10_cover_stair", "c7_hold_hidden"))
        assertEquals(listOf("c10_carry_stair"), carry("c10_cover_stair"))
        assertEquals("a save that never met the Line is carried by Voss", listOf("c10_carry_voss"), carry())
    }

    @Test
    fun `the words at the hasty earth are the page Kaelen wrote, or the plain custom, and then the shipped ending`() {
        listOf("c10_carry_voss", "c10_carry_stair", "c10_carry_stair_edda", "c10_carry_forge").forEach { id ->
            assertEquals(listOf("c10_earth_name"), visible(at(id, flags = setOf("c10_rites_name"))).map { it.nextNodeId })
            assertEquals(listOf("c10_earth_deeds"), visible(at(id, flags = setOf("c10_rites_deeds"))).map { it.nextNodeId })
            assertEquals(listOf("c10_earth_plain"), visible(at(id, flags = setOf("c10_rites_plain"))).map { it.nextNodeId })
            assertEquals(listOf("c10_earth_bare"), visible(at(id)).map { it.nextNodeId })
        }
        listOf("c10_earth_name", "c10_earth_deeds", "c10_earth_plain", "c10_earth_bare").forEach {
            assertEquals(listOf(end), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Halvard's death
    // ---------------------------------------------------------------------------------------------

    private val poolFlags = listOf(
        listOf("c7_traded", "c7_shut", "c7_page", "c7_comfort", "c7_quiet", null),
        listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted", null),
        listOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden", null),
        listOf("c7_ground_yard", "c7_ground_stair", "c7_ground_road", null),
        listOf("c7_halvard_trusts_voss", "c7_halvard_doubts_voss", null),
        listOf("refused_envoy", "heard_envoy_offer", "confronted_envoy_with_prisoners_words", null),
        listOf("c9_stair_rules", "c9_stair_people", null),
        listOf("c6_met_tam", null),
        listOf("c7_voss_hurt_deeper", null)
    )

    private fun randomFlags(random: Random): Set<String> = poolFlags.mapNotNull { it[random.nextInt(it.size)] }.toSet()

    private fun run(startState: GameState, random: Random): Pair<List<String>, GameState> {
        var s = startState
        val trail = ArrayList<String>()
        var steps = 0
        while (s.sceneId != "the_chain_speaks") {
            check(steps++ < 200) { "never finished; stuck at ${s.sceneId}" }
            val n = StoryRepository.node(s.sceneId)
            trail += n.id
            s = if (n.combatEncounterId != null) {
                s.resolveCombat(CombatRepository.encounter(n.combatEncounterId!!), CombatOutcome(victory = random.nextBoolean(), damageTaken = 0))
            } else {
                val choices = visible(s)
                check(choices.isNotEmpty()) { "dead end at ${n.id} with ${s.flags}" }
                s.applyChoice(choices[random.nextInt(choices.size)])
            }
        }
        return trail to s
    }

    @Test
    fun `every telling passes the fall, then the last words, then the quiet after, and the chain is taken once`() {
        val random = Random(0x10F1L)
        repeat(3_000) { n ->
            val (trail, endState) = run(at(start, flags = randomFlags(random)), random)
            val fallAt = trail.indexOf(fall)
            val wordsAt = trail.indexOf(lastWords)
            val bridgeAt = trail.indexOf("c10_carry_bridge")
            val endAt = trail.indexOf(end)
            assertTrue("run $n: fall ${trail.count { it == fall }} times", trail.count { it == fall } == 1 && trail.count { it == lastWords } == 1)
            assertTrue("run $n: order", fallAt in 0 until wordsAt && wordsAt < bridgeAt && bridgeAt < endAt)
            assertTrue("run $n: nothing new before the fall comes after it", trail.take(fallAt).none { it.startsWith("c10_carry") || it.startsWith("c10_earth") })
            assertTrue("run $n: the night is before the fall", trail.indexOf("c10_evening") in 0 until fallAt)
            assertTrue(endState.flags.containsAll(setOf("halvard_died", "took_the_warden_chain")))
            assertEquals("run $n", 1, endState.inventory.count { it == "halvards_warden_chain" })
        }
    }

    @Test
    fun `no route from the first scene of the chapter reaches the end or the next chapter without the fall`() {
        val all = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.associateBy { it.id }
        fun successors(n: StoryNode): List<String> = n.choices.map { it.nextNodeId } +
            (n.combatEncounterId?.let { CombatRepository.encounter(it).let { e -> listOfNotNull(e.victoryNodeId, e.defeatNodeId) } } ?: emptyList())
        val seen = HashSet<String>()
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id == fall || !seen.add(id)) continue
            successors(all.getValue(id)).forEach { queue.addLast(it) }
        }
        assertFalse("the end is reachable without the fall", end in seen)
        assertFalse(lastWords in seen)
        assertTrue("a later chapter is reachable without the fall: ${seen.filter { all.getValue(it).chapterId != "chapter_10" }}",
            seen.all { all.getValue(it).chapterId == "chapter_10" })
    }

    @Test
    fun `nothing in Chapters 10 to 12 sets, clears, requires or forbids the death except the one choice that sets it`() {
        val death = setOf("halvard_died", "took_the_warden_chain")
        (10..12).flatMap { StoryRepository.nodesInChapter("chapter_$it") }.forEach { n ->
            n.choices.forEach { c ->
                val touches = c.consequences.setFlags.intersect(death) + c.consequences.clearFlags.intersect(death) +
                    (c.requirements?.requiredFlags ?: emptySet()).intersect(death) + (c.requirements?.forbiddenFlags ?: emptySet()).intersect(death)
                if (n.id == lastWords) assertEquals(death, c.consequences.setFlags.intersect(death))
                else assertTrue("${n.id} -> ${c.nextNodeId} touches the death: $touches", touches.isEmpty())
            }
        }
        assertTrue("the chain is granted by one choice and no other", (10..12).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
            .flatMap { n -> n.choices.filter { "halvards_warden_chain" in it.consequences.grantItemIds }.map { n.id } } == listOf(lastWords))
    }

    @Test
    fun `the fall, the last words and the fight that leads to them are exactly as shipped`() {
        assertEquals(shippedHashes.getValue(fall), sha(node(fall).narrativeText))
        assertEquals(shippedHashes.getValue(lastWords), sha(node(lastWords).narrativeText))
        val text = words(node(lastWords).narrativeText)
        assertTrue(text.contains("\"Wear it, or don't. Wasn't ever really about the chain.\""))
        assertTrue(text.contains("Ilsevet didn't come here to kill you, lad. She came to see if she could still lose. Make sure the answer stays yes."))
        val encounter = CombatRepository.encounter("stonebeard_siege_encounter")
        assertEquals("ilsevets_vanguard_captain", encounter.enemyId)
        assertEquals(fall, encounter.victoryNodeId)
        assertEquals("stonebeard_siege_defeat", encounter.defeatNodeId)
        assertEquals(listOf(fall), visible(at("stonebeard_siege_defeat")).map { it.nextNodeId })
        assertEquals(listOf(lastWords), visible(at(fall)).map { it.nextNodeId })
        listOf("the_gate_falls_defiant", "the_gate_falls_principled", "the_gate_falls_silent").forEach {
            assertEquals("stonebeard_siege_encounter", node(it).combatEncounterId)
        }
    }

    @Test
    fun `the scenes the night adds grant nothing and set only what the chapter owns`() {
        val allowed = HUB_MARKERS_FOUR + C10_RITES_FAMILY + C10_COVER_FAMILY + C7_RUNG_FAMILY
        newScenes.forEach { n ->
            n.choices.forEach { c ->
                assertTrue("${n.id}: grants ${c.consequences.grantItemIds}", c.consequences.grantItemIds.isEmpty())
                assertTrue("${n.id}: sets ${c.consequences.setFlags - allowed}", allowed.containsAll(c.consequences.setFlags))
                assertTrue("${n.id}: trophy ${c.consequences.unlockTrophy}", c.consequences.unlockTrophy in setOf(null, "The Last Evening"))
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Shipped scenes, old saves, replay
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the eleven scenes that shipped are word for word as before`() {
        assertEquals(11, shippedHashes.size)
        shippedHashes.forEach { (id, hash) -> assertEquals(id, hash, sha(node(id).narrativeText)) }
    }

    @Test
    fun `the chapter has its eleven shipped scenes and thirty-nine new ones, all real`() {
        assertEquals(11 + 39, StoryRepository.nodesInChapter("chapter_10").size)
        assertEquals(39, newScenes.size)
        newScenes.forEach { n ->
            assertTrue(n.id, words(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty())
            assertEquals(n.id, "chapter_10", n.chapterId)
        }
    }

    @Test
    fun `a save parked on any scene Chapter 10 had before it grew plays on to Chapter 11`() {
        val legacyFlags = mapOf(
            "the_last_two" to emptySet(),
            "ilsevet_arrives_fortified" to setOf("fortified_for_the_last_stand"),
            "ilsevet_arrives_hard" to setOf("prepared_to_meet_them_hard"),
            "kaelens_answer" to setOf("prepared_to_meet_them_hard"),
            "the_gate_falls_defiant" to setOf("defied_ilsevet"),
            "stonebeard_siege_defeat" to setOf("defied_ilsevet"),
            "halvard_falls" to setOf("defied_ilsevet"),
            "what_halvard_leaves_behind" to setOf("defied_ilsevet"),
            "chapter10_end" to setOf("halvard_died", "took_the_warden_chain")
        )
        shippedHashes.keys.forEach { id ->
            val flags = legacyFlags[id] ?: emptySet()
            val done = ReplayKit.play(at(id, flags = flags), stop = { it.sceneId == "the_chain_speaks" })
            assertEquals(id, "the_chain_speaks", done.sceneId)
        }
        // an older save that reaches the quiet after with none of the new flags is carried by Voss and hears the plain custom
        val old = ReplayKit.play(at(lastWords), stop = { it.sceneId == end })
        assertEquals(end, old.sceneId)
    }

    @Test
    fun `a second telling begun with everything the first left behind goes exactly where a first telling goes`() {
        val random = Random(0x10E2L)
        val owned = newScenes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet() + C10_TELLING_FLAGS - C7_RUNG_FAMILY
        var compared = 0
        repeat(300) { n ->
            val base = randomFlags(random)
            val seed = random.nextLong()
            val (firstTrail, firstEnd) = run(at(start, flags = base), Random(seed xor 1))
            val dirty = base + (firstEnd.flags.intersect(owned))
            val (cleanTrail, clean) = run(at(start, flags = base), Random(seed))
            val (dirtyTrail, dirtied) = run(at(start, flags = dirty), Random(seed))
            assertEquals("run $n", cleanTrail, dirtyTrail)
            assertEquals("run $n: flags the first telling left behind survived", clean.flags.intersect(owned), dirtied.flags.intersect(owned))
            assertTrue(firstTrail.isNotEmpty())
            compared++
        }
        assertEquals(300, compared)
    }

    @Test
    fun `three thousand tellings read the chapter in a sensible number of scenes and words, and never loop`() {
        val random = Random(0x10C0DEL)
        var minScenes = Int.MAX_VALUE; var maxScenes = 0; var minWords = Int.MAX_VALUE; var maxWords = 0; var totalWords = 0L; var totalScenes = 0L
        repeat(3_000) { n ->
            val (trail, endState) = run(at(start, flags = randomFlags(random)), random)
            assertEquals("run $n: no scene is read twice", trail.size, trail.toSet().size)
            assertTrue("run $n: at most one answer to where he stood", endState.flags.intersect(C10_COVER_FAMILY).size == 1)
            assertTrue("run $n: at most one page", endState.flags.intersect(C10_RITES_FAMILY).size <= 1)
            assertEquals("run $n: no marker outlives the night", emptySet<String>(), endState.flags.intersect(HUB_MARKERS_FOUR))
            val w = trail.sumOf { words(node(it).narrativeText).split(" ").size }
            minScenes = minOf(minScenes, trail.size); maxScenes = maxOf(maxScenes, trail.size)
            minWords = minOf(minWords, w); maxWords = maxOf(maxWords, w); totalWords += w; totalScenes += trail.size
        }
        println("CH10 scenes read $minScenes to $maxScenes, mean ${"%.1f".format(totalScenes / 3000.0)}; words read $minWords to $maxWords, mean ${totalWords / 3000}")
        assertTrue("chapter reads $minScenes scenes at least", minScenes >= 15)
        assertTrue("chapter reads $maxScenes scenes at most", maxScenes <= 32)
    }
}
