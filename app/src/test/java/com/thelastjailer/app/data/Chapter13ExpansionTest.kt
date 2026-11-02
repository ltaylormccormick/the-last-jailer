package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 13 as written: the hearing (two of up to three matters, each read by what Chapters 6 to 9 left), the ask at the
 * stirrup, Voss that night, the eve of the rites by where Kaelen stood in Chapter 10 and the page he wrote, the word from Fenmoor
 * by what Chapter 11 left, and where Edda's people are, Tam's lamp and Edda's question about the door. The shipped scenes are
 * pinned by Chapter1316ShippedPinTest; this holds what the new ones do.
 */
class Chapter13ExpansionTest {

    private val rungs = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")
    private val rungNight = mapOf("c7_rung_wary" to "c13_night_wary", "c7_rung_working" to "c13_night_working", "c7_rung_trusted" to "c13_night_trusted")
    private val greymoorFlags = listOf("sealed_greymoor_personally", "reported_greymoor_honestly", "left_greymoor_as_is")
    private val holdFlags = listOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")
    private val coverFlags = listOf("c10_cover_voss", "c10_cover_stair", "c10_cover_forge")
    private val ritesFlags = listOf("c10_rites_name", "c10_rites_deeds", "c10_rites_plain")
    private val thessalyFlags = listOf("pledged_support_to_fenmoor", "invited_thessaly_to_stonebeard", "left_fenmoor_uncertain")

    private val newScenes get() = StoryRepository.nodesInChapter("chapter_13").filter { it.id.startsWith("c13_") }

    // ---------------------------------------------------------------------------------------------
    // The hearing
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Voss's answer leads to the hearing Selvane opens, by whether Kaelen stood with her or waited`() {
        val stood = take(at("what_voss_answers", flags = setOf("stood_with_voss")), "c13_hearing_vouched")
        assertTrue("kaelen_vouched_for_voss" in stood.flags)
        val alone = take(at("what_voss_answers", flags = setOf("let_voss_go_alone")), "c13_hearing_account")
        assertTrue("trusted_voss_account" in alone.flags)
        assertEquals(listOf("c13_hearing_vouched"), visible(at("what_voss_answers", flags = setOf("stood_with_voss"))).map { it.nextNodeId })
        assertEquals(listOf("c13_hearing_account"), visible(at("what_voss_answers", flags = setOf("let_voss_go_alone"))).map { it.nextNodeId })
    }

    @Test
    fun `the table offers a matter for what the story left, and the seal only to a Kaelen who spent or used it`() {
        fun offered(vararg flags: String) = visible(at("c13_hearing_vouched", flags = flags.toSet())).map { it.nextNodeId }.toSet()
        // a Kaelen who left Greymoor, with no call-off and no seal
        assertEquals(setOf("c13_m_riders_not", "c13_m_greymoor_left"), offered())
        assertEquals(setOf("c13_m_riders_called", "c13_m_greymoor_left"), offered("c7_voss_called_them_off"))
        assertEquals(setOf("c13_m_riders_not", "c13_m_greymoor_sealed"), offered("sealed_greymoor_personally"))
        assertEquals(setOf("c13_m_riders_not", "c13_m_greymoor_reported"), offered("reported_greymoor_honestly"))
        assertEquals(setOf("c13_m_riders_not", "c13_m_greymoor_left", "c13_m_seal_spent"), offered("c8_exit_tithe", "c8_seal_spent"))
        assertEquals(setOf("c13_m_riders_not", "c13_m_greymoor_left", "c13_m_seal_door"), offered("c9_door_seal"))
        // the account of the table is the same whichever way the hearing was opened
        assertEquals(offered("c8_exit_tithe"), visible(at("c13_hearing_account", flags = setOf("c8_exit_tithe"))).map { it.nextNodeId }.toSet())
    }

    @Test
    fun `exactly two matters are heard, one at a time, and the markers are gone when the verdict comes`() {
        val start = at("c13_hearing_vouched", flags = setOf("c7_voss_called_them_off", "reported_greymoor_honestly", "c8_exit_tithe"))
        val firstOptions = visible(start)
        assertEquals(3, firstOptions.size)
        firstOptions.forEach { first ->
            val inFirst = start.applyChoice(first)
            val back = take(inFirst, "c13_hearing_again")
            val second = visible(back)
            assertEquals("the matter just heard is not offered again", 2, second.size)
            assertTrue(second.none { it.nextNodeId == first.nextNodeId })
            second.forEach { choice ->
                val inSecond = back.applyChoice(choice)
                val done = take(inSecond, "selvanes_verdict")
                assertEquals("no marker outlives the hearing", emptySet<String>(), done.flags.intersect(HUB_MARKERS))
            }
        }
    }

    @Test
    fun `a Kaelen with only two matters hears both`() {
        val start = at("c13_hearing_account")
        val first = visible(start)
        assertEquals(2, first.size)
        first.forEach { choice ->
            val back = take(start.applyChoice(choice), "c13_hearing_again")
            val rest = visible(back)
            assertEquals("the one matter left", 1, rest.size)
            assertTrue(rest.single().nextNodeId != choice.nextNodeId)
            assertEquals("selvanes_verdict", take(back.applyChoice(rest.single()), "selvanes_verdict").sceneId)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The ask, and Voss that night
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the verdict leads to the stirrup and Kaelen asks about Ilsevet in one of two ways`() {
        assertEquals(listOf("c13_parting"), visible(at("selvanes_verdict")).map { it.nextNodeId })
        val stirrup = at("c13_parting", courage = 5, honour = 5)
        assertEquals(2, visible(stirrup).size)
        val open = take(stirrup, "c13_parting_open")
        assertTrue("c13_asked_openly" in open.flags)
        assertEquals(6, open.courage)
        val low = take(stirrup, "c13_parting_low")
        assertFalse("c13_asked_openly" in low.flags)
        assertEquals(6, low.honour)
        // a replay that asked openly the first time and low the second does not carry the first
        val replayLow = take(at("c13_parting", flags = setOf("c13_asked_openly")), "c13_parting_low")
        assertFalse("c13_asked_openly" in replayLow.flags)
    }

    @Test
    fun `after the stirrup Voss answers according to where she and Kaelen stand`() {
        listOf("c13_parting_open", "c13_parting_low").forEach { scene ->
            rungs.forEach { rung -> assertEquals(listOf(rungNight.getValue(rung)), visible(at(scene, flags = setOf(rung))).map { it.nextNodeId }) }
            assertEquals("a save with no rung counts as the lowest", listOf("c13_night_wary"), visible(at(scene)).map { it.nextNodeId })
        }
    }

    @Test
    fun `the night moves Voss one step at most, up, and only where Kaelen answers honestly`() {
        val covers = setOf("c10_cover_voss")
        val wary = at("c13_night_wary", flags = covers + "c7_rung_wary")
        assertEquals(setOf("c7_rung_working"), takeLabel(wary, "Tell her you would do it again, and that you should have said so first.").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_wary"), takeLabel(wary, "Say that you did it for the Hold, and leave it there.").flags.intersect(C7_RUNG_FAMILY))
        val working = at("c13_night_working", flags = covers + "c7_rung_working")
        assertEquals(setOf("c7_rung_trusted"), takeLabel(working, "Tell her she is required here.").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_working"), takeLabel(working, "Tell her to rest, and think about it in the morning.").flags.intersect(C7_RUNG_FAMILY))
        val trusted = at("c13_night_trusted", flags = covers + "c7_rung_trusted")
        assertEquals(setOf("c7_rung_trusted"), takeLabel(trusted, "Stand beside her on the wall until the watch changes.").flags.intersect(C7_RUNG_FAMILY))
    }

    @Test
    fun `the chapel is offered only to a Kaelen who stood at it, only where Voss trusts him, and Voss gives no reason for leaving the Order`() {
        val trusted = "c7_rung_trusted"
        assertTrue(visible(at("c13_night_trusted", flags = setOf(trusted, "c8_chapel_story"))).any { it.nextNodeId == "c13_night_chapel" })
        assertTrue(visible(at("c13_night_trusted", flags = setOf(trusted))).none { it.nextNodeId == "c13_night_chapel" })
        listOf("c13_night_wary", "c13_night_working").forEach {
            assertTrue(it, visible(at(it, flags = setOf("c8_chapel_story"))).none { c -> c.nextNodeId == "c13_night_chapel" })
        }
        val text = node("c13_night_chapel").narrativeText.replace(Regex("\\s+"), " ")
        assertTrue(text.contains("I had hoped it would still be standing. It was."))
        listOf("because", "kept this place", "who kept", "was mine", "I left the Order").forEach { assertFalse(it, text.contains(it)) }
    }

    // ---------------------------------------------------------------------------------------------
    // The eve, the gate-book, and what comes after the carving
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the eve is spent with whoever Kaelen stood with when the column came`() {
        listOf("c13_night_wary", "c13_night_working", "c13_night_trusted").forEach { night ->
            val rung = rungs[listOf("c13_night_wary", "c13_night_working", "c13_night_trusted").indexOf(night)]
            val label = visible(at(night, flags = setOf(rung, "c10_cover_voss"))).last().label
            assertEquals(night, "c13_eve_stair", take(at(night, flags = setOf(rung, "c10_cover_stair")), "c13_eve_stair", label).sceneId)
            assertEquals(night, "c13_eve_forge", take(at(night, flags = setOf(rung, "c10_cover_forge")), "c13_eve_forge", label).sceneId)
            assertEquals(night, "c13_eve_voss", take(at(night, flags = setOf(rung, "c10_cover_voss")), "c13_eve_voss", label).sceneId)
            assertEquals("a save with no answer gets Voss", "c13_eve_voss", take(at(night, flags = setOf(rung)), "c13_eve_voss", label).sceneId)
        }
    }

    @Test
    fun `the smiths answer the page Kaelen wrote, and a Kaelen who wrote none goes straight to the rites`() {
        listOf("c13_eve_voss", "c13_eve_stair", "c13_eve_forge").forEach { eve ->
            assertEquals("c13_book_name", visible(at(eve, flags = setOf("c10_rites_name"))).single().nextNodeId)
            assertEquals("c13_book_deeds", visible(at(eve, flags = setOf("c10_rites_deeds"))).single().nextNodeId)
            assertEquals("c13_book_plain", visible(at(eve, flags = setOf("c10_rites_plain"))).single().nextNodeId)
            assertEquals("halvards_rites", visible(at(eve)).single().nextNodeId)
        }
        listOf("c13_book_name", "c13_book_deeds", "c13_book_plain").forEach { assertEquals("halvards_rites", visible(at(it)).single().nextNodeId) }
        val deeds = node("c13_book_deeds").narrativeText.replace(Regex("\\s+"), " ")
        assertTrue("the shipped rites keep Kaelen's few words", deeds.contains("The word was ours before it was yours."))
        // the page is read aloud once, at the hasty earth: the book scenes answer it and do not read it again
        listOf("c13_book_name", "c13_book_deeds", "c13_book_plain").forEach {
            assertTrue(it, node(it).narrativeText.replace(Regex("\\s+"), " ").contains("It is not in the custom to answer a keeper's line"))
        }
    }

    @Test
    fun `the rubbing is taken once whatever Fenmoor said, and Fenmoor's word is told as Chapter 11 left it`() {
        val expected = mapOf(
            "invited_thessaly_to_stonebeard" to "c13_thessaly_attended",
            "pledged_support_to_fenmoor" to "c13_thessaly_pledged",
            "left_fenmoor_uncertain" to "c13_thessaly_silent"
        )
        expected.forEach { (flag, scene) ->
            val after = takeLabel(at("halvards_rites", flags = setOf(flag)), "Take a rubbing of the mark before you leave.")
            assertEquals(flag, scene, after.sceneId)
            assertEquals(flag, 1, after.inventory.count { it == "rubbing_of_halvards_mark" })
        }
        assertEquals("a save with no answer from Fenmoor is told it silent", "c13_thessaly_silent",
            takeLabel(at("halvards_rites"), "Take a rubbing of the mark before you leave.").sceneId)
        assertEquals(1, visible(at("halvards_rites", flags = setOf("pledged_support_to_fenmoor"))).size)
    }

    @Test
    fun `Edda's question about the door is put only where Edda's people are, and the shipped question follows either way`() {
        listOf("c13_thessaly_attended", "c13_thessaly_pledged", "c13_thessaly_silent").forEach { scene ->
            holdFlags.forEach { hold ->
                assertEquals("$scene $hold", listOf("c13_door"), visible(at(scene, flags = setOf(hold, "c6_met_tam"))).map { it.nextNodeId })
            }
            assertEquals(scene, listOf("who_stands_with_stonebeard"), visible(at(scene)).map { it.nextNodeId })
            assertEquals("met Tam, made no promise", listOf("who_stands_with_stonebeard"), visible(at(scene, flags = setOf("c6_met_tam"))).map { it.nextNodeId })
        }
        val door = at("c13_door", courage = 5, honour = 5, flags = setOf("c7_hold_terms"))
        assertEquals(2, visible(door).size)
        val open = take(door, "c13_door_open_reply")
        assertEquals(setOf("c13_door_open"), open.flags.intersect(C13_DOOR_FAMILY))
        assertEquals(6, open.honour)
        val barred = take(door, "c13_door_barred_reply")
        assertEquals(setOf("c13_door_barred"), barred.flags.intersect(C13_DOOR_FAMILY))
        assertEquals(6, barred.courage)
        // a replay that kept the door open and now bars it holds only the new answer, and the other way round
        assertEquals(setOf("c13_door_barred"), take(at("c13_door", flags = setOf("c13_door_open")), "c13_door_barred_reply").flags.intersect(C13_DOOR_FAMILY))
        assertEquals(setOf("c13_door_open"), take(at("c13_door", flags = setOf("c13_door_barred")), "c13_door_open_reply").flags.intersect(C13_DOOR_FAMILY))
        listOf("c13_door_open_reply", "c13_door_barred_reply").forEach { assertEquals("who_stands_with_stonebeard", visible(at(it)).single().nextNodeId) }
    }

    // ---------------------------------------------------------------------------------------------
    // The whole chapter
    // ---------------------------------------------------------------------------------------------

    private fun mix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    private fun score(c: Choice, seed: Int): Long = mix((c.label.hashCode().toLong() shl 32) xor c.nextNodeId.hashCode().toLong() xor mix(seed.toLong()))

    private class Telling(val start: GameState, val trail: List<String>, val end: GameState)

    private fun playChapter(start: GameState, seed: Int): Telling {
        val trail = ArrayList<String>()
        val end = ReplayKit.play(
            start,
            stop = { it.sceneId == "what_waits_speaks" },
            pick = { n, v -> trail += n.id; v.maxByOrNull { score(it, seed) } }
        )
        return Telling(start, trail, end)
    }

    /** A coherent state at the Order's delegation: one answer in every family, with Tam wherever Edda's people are. */
    private fun starts(random: Random): GameState {
        val flags = HashSet<String>()
        flags += rungs[random.nextInt(3)]
        flags += greymoorFlags[random.nextInt(3)]
        flags += coverFlags[random.nextInt(3)]
        flags += thessalyFlags[random.nextInt(3)]
        val rites = random.nextInt(4)
        if (rites > 0) flags += ritesFlags[rites - 1]
        val hold = random.nextInt(4)
        if (hold > 0) { flags += holdFlags[hold - 1]; flags += "c6_met_tam"; flags += "c6_offered_the_hold" } else if (random.nextBoolean()) flags += "c6_met_tam"
        if (random.nextBoolean()) flags += "c7_voss_called_them_off"
        when (random.nextInt(3)) { 1 -> { flags += "c8_exit_tithe"; flags += "c8_seal_spent" }; 2 -> flags += "c9_door_seal" }
        if (random.nextBoolean()) flags += "c8_chapel_story"
        val entry = if (random.nextBoolean()) { flags += "trusted_voss_fully"; "the_orders_delegation_trusted" } else "the_orders_delegation_guarded"
        return at(entry, flags = flags, courage = 12, honour = 11)
    }

    @Test
    fun `three thousand tellings read the chapter in order, each as the story left it`() {
        val random = Random(0x13L)
        val seen = HashMap<String, Int>()
        repeat(3000) { run ->
            val t = playChapter(starts(random), run)
            val flags = t.start.flags
            val trail = t.trail
            fun first(id: String) = trail.indexOf(id).also { assertTrue("run $run: $id never read", it >= 0) }
            fun count(prefix: String) = trail.count { it.startsWith(prefix) }
            trail.filter { it.startsWith("c13_") }.forEach { seen[it] = (seen[it] ?: 0) + 1 }

            // the hearing: opened by the stance, two matters, each the one the story left
            val opener = if ("stood_with_voss" in t.end.flags) "c13_hearing_vouched" else "c13_hearing_account"
            assertTrue("run $run: $opener", opener in trail)
            assertEquals("run $run: the table is opened once", 1, trail.count { it == "c13_hearing_vouched" || it == "c13_hearing_account" })
            assertEquals("run $run: two matters", 2, count("c13_m_"))
            val matters = trail.filter { it.startsWith("c13_m_") }
            assertEquals(2, matters.map { it.substringAfter("c13_m_").substringBefore("_") }.toSet().size)
            matters.forEach { m ->
                when {
                    m == "c13_m_riders_called" -> assertTrue("c7_voss_called_them_off" in flags)
                    m == "c13_m_riders_not" -> assertFalse("c7_voss_called_them_off" in flags)
                    m == "c13_m_seal_spent" -> assertTrue("c8_exit_tithe" in flags)
                    m == "c13_m_seal_door" -> assertTrue("c9_door_seal" in flags)
                    m == "c13_m_greymoor_sealed" -> assertTrue("sealed_greymoor_personally" in flags)
                    m == "c13_m_greymoor_reported" -> assertTrue("reported_greymoor_honestly" in flags)
                    m == "c13_m_greymoor_left" -> assertTrue("left_greymoor_as_is" in flags)
                }
            }
            assertEquals("run $run: the table is visited twice", 1, trail.count { it == "c13_hearing_again" })
            assertTrue(first("selvanes_verdict") > first("c13_hearing_again"))

            // the ask, and the night after
            val asked = trail.filter { it == "c13_parting_open" || it == "c13_parting_low" }
            assertEquals("run $run: Kaelen asks once", 1, asked.size)
            assertEquals("run $run", asked.single() == "c13_parting_open", "c13_asked_openly" in t.end.flags)
            val rung = rungs.first { it in flags }
            val night = trail.filter { it.startsWith("c13_night_") && it != "c13_night_chapel" }
            assertEquals("run $run", listOf(rungNight.getValue(rung)), night)
            if ("c13_night_chapel" in trail) assertTrue("run $run: chapel", rung == "c7_rung_trusted" && "c8_chapel_story" in flags)
            val rungAfter = rungs.first { it in t.end.flags }
            val step = rungs.indexOf(rungAfter) - rungs.indexOf(rung)
            assertTrue("run $run: rung moved $step", step in 0..1)
            assertEquals("run $run: one rung at the end", 1, t.end.flags.count { it in rungs })

            // the eve, the book, the rites
            val cover = coverFlags.first { it in flags }
            assertEquals("run $run", listOf("c13_eve_" + cover.removePrefix("c10_cover_")), trail.filter { it.startsWith("c13_eve_") })
            val rites = ritesFlags.firstOrNull { it in flags }
            assertEquals("run $run", if (rites == null) emptyList() else listOf("c13_book_" + rites.removePrefix("c10_rites_")), trail.filter { it.startsWith("c13_book_") })
            assertTrue(first("halvards_rites") > first("c13_parting"))

            // Fenmoor, the door, and the shipped question
            val thessaly = thessalyFlags.first { it in flags }
            val word = mapOf("pledged_support_to_fenmoor" to "pledged", "invited_thessaly_to_stonebeard" to "attended", "left_fenmoor_uncertain" to "silent")
            assertEquals("run $run", listOf("c13_thessaly_" + word.getValue(thessaly)), trail.filter { it.startsWith("c13_thessaly_") })
            val people = holdFlags.any { it in flags }
            assertEquals("run $run: the door is put to Edda's people", people, "c13_door" in trail)
            assertEquals("run $run: one reply", if (people) 1 else 0, count("c13_door_") )
            assertEquals("run $run", people, t.end.flags.any { it in C13_DOOR_FAMILY })
            assertTrue("run $run: one door answer at most", t.end.flags.count { it in C13_DOOR_FAMILY } <= 1)
            assertTrue(first("who_stands_with_stonebeard") > first("halvards_rites"))
            assertEquals("run $run: the chapter ends in the prisoner's voice", "what_waits_speaks", t.end.sceneId)
            assertEquals("run $run: no hub marker survives", emptySet<String>(), t.end.flags.intersect(HUB_MARKERS))
            assertEquals("run $run: the rubbing", 1, t.end.inventory.count { it == "rubbing_of_halvards_mark" })
            assertTrue("run $run: reading length ${trail.size}", trail.size in 12..30)
        }
        // every new scene was read by some telling
        val missing = newScenes.map { it.id }.filter { (seen[it] ?: 0) == 0 }
        assertEquals("new scenes no telling reached", emptyList<String>(), missing)
    }

    @Test
    fun `a second telling begun with everything the first left behind goes exactly where a first telling goes`() {
        val random = Random(0x13EL)
        // Voss's rung is not Chapter 13's to clear: it is the ladder, reset where Chapter VII begins and carried on through this chapter
        // (Chapter1316PackageTest holds the whole replay), so a second telling that begins here keeps the rung it was left with
        val owned = newScenes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet() + C13_TELLING_FLAGS - C7_RUNG_FAMILY
        repeat(600) { run ->
            val start = starts(random)
            val (_, firstEnd) = playChapter(start, 5000 + run).let { it.trail to it.end }
            val dirty = start.copy(flags = start.flags + (firstEnd.flags intersect owned))
            val clean = playChapter(start, run)
            val again = playChapter(dirty, run)
            assertEquals("run $run: the second telling walked a different road", clean.trail, again.trail)
            val survived = (again.end.flags intersect C13_TELLING_FLAGS) - (clean.end.flags intersect C13_TELLING_FLAGS)
            assertEquals("run $run: flags the first telling left behind survived: $survived", emptySet<String>(), survived)
        }
    }

    @Test
    fun `Chapter 13 has its eleven shipped scenes and twenty nine new ones, and gains no combat, item or trophy`() {
        val all = StoryRepository.nodesInChapter("chapter_13")
        assertEquals(11 + 29, all.size)
        assertEquals(29, newScenes.size)
        assertTrue(all.none { it.combatEncounterId != null })
        newScenes.forEach { n ->
            n.choices.forEach { c ->
                assertTrue("${n.id}: grants nothing but the shipped rubbing", c.consequences.grantItemIds.isEmpty() || (c.consequences.grantItemIds == listOf("rubbing_of_halvards_mark")))
                assertEquals("${n.id}: no trophy", null, c.consequences.unlockTrophy)
            }
        }
        val grants = all.flatMap { n -> n.choices.flatMap { it.consequences.grantItemIds } }.toSet()
        assertEquals(setOf("rubbing_of_halvards_mark"), grants)
    }

    @Test
    fun `the new choices change only Courage and Honour, by one, and only at the ask and the door`() {
        val statChoices = newScenes.flatMap { n -> n.choices.filter { it.consequences.statDeltas.isNotEmpty() }.map { n.id to it } }
        assertEquals(setOf("c13_parting", "c13_door"), statChoices.map { it.first }.toSet())
        statChoices.forEach { (id, c) ->
            assertEquals(id, 1, c.consequences.statDeltas.size)
            c.consequences.statDeltas.forEach { (stat, delta) -> assertTrue(id, stat in setOf(StatType.COURAGE, StatType.HONOUR) && delta == 1) }
        }
    }

    @Test
    fun `a save parked on any scene Chapter 13 had before it grew plays on`() {
        val shipped = listOf(
            "the_orders_delegation_trusted", "the_orders_delegation_guarded", "the_inquisitor_general_stood", "the_inquisitor_general_alone",
            "what_voss_answers", "selvanes_verdict", "halvards_rites", "who_stands_with_stonebeard",
            "chapter13_end_training", "chapter13_end_watch", "chapter13_end_deferred"
        )
        shipped.forEach { id ->
            val flags = when (id) {
                "what_voss_answers", "the_inquisitor_general_stood" -> setOf("stood_with_voss")
                "the_inquisitor_general_alone" -> setOf("let_voss_go_alone")
                else -> emptySet()
            }
            val done: GameState = ReplayKit.play(at(id, flags = flags + setOf("c7_rung_working")), stop = { it.sceneId == "what_waits_speaks" })
            assertEquals(id, "what_waits_speaks", done.sceneId)
        }
    }
}
