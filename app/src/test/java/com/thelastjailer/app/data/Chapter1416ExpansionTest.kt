package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.resolveCombat
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapters 14, 15 and 16 as written. Chapter 14: the nights at the gate after Chapter 13, the two memories the stone keeps of
 * Kaelen before it shows him the vision, and Voss after it. Chapter 15: how Kaelen asked Selvane, what else was in the case,
 * Voss reading it, and Ashfall again. Chapter 16: a piece put in Voss's hand, and what the theft cost. The shipped scenes are pinned by
 * Chapter1316ShippedPinTest; this holds what the new ones do.
 */
class Chapter1416ExpansionTest {

    private val rungs = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")
    private fun scenes(chapter: String, prefix: String) = StoryRepository.nodesInChapter(chapter).filter { it.id.startsWith(prefix) }
    private fun text(id: String) = node(id).narrativeText.replace(Regex("\\s+"), " ")

    // ---------------------------------------------------------------------------------------------
    // Chapter 14
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `each ending of Chapter 13 leads to its own night at the gate and clears what Chapter 14 records`() {
        val dirty = setOf("asked_the_prisoner_directly", "told_voss_everything", "lost_to_the_memory_itself", "kept_the_vision_to_himself")
        mapOf(
            "chapter13_end_training" to "c14_gate_training",
            "chapter13_end_watch" to "c14_gate_watch",
            "chapter13_end_deferred" to "c14_gate_deferred"
        ).forEach { (end, gate) ->
            val after = take(at(end, flags = dirty), gate)
            assertEquals(end, emptySet<String>(), after.flags.intersect(C14_TELLING_FLAGS))
            assertEquals(gate, listOf("what_waits_speaks"), visible(at(gate)).map { it.nextNodeId })
        }
    }

    @Test
    fun `the stone keeps the first knock and the day of the office, as Kaelen answered them in Chapter 3`() {
        assertEquals(listOf("c14_knock_listened"), visible(at("the_offered_glimpse", flags = setOf("listened_to_the_prisoner"))).map { it.nextNodeId })
        assertEquals(listOf("c14_knock_refused"), visible(at("the_offered_glimpse", flags = setOf("refused_the_prisoner"))).map { it.nextNodeId })
        assertEquals("a save that holds neither is told it refused", listOf("c14_knock_refused"), visible(at("the_offered_glimpse")).map { it.nextNodeId })
        listOf("c14_knock_listened", "c14_knock_refused").forEach { knock ->
            assertEquals(knock, listOf("c14_office_full"), visible(at(knock, flags = setOf("accepted_fully"))).map { it.nextNodeId })
            assertEquals(knock, listOf("c14_office_reluctant"), visible(at(knock, flags = setOf("accepted_reluctantly"))).map { it.nextNodeId })
            assertEquals(knock, listOf("c14_office_refusedonce"), visible(at(knock, flags = setOf("refused_once"))).map { it.nextNodeId })
        }
        listOf("c14_office_full", "c14_office_reluctant", "c14_office_refusedonce").forEach {
            assertEquals(it, listOf("inside_the_memory"), visible(at(it)).map { c -> c.nextNodeId })
        }
        assertEquals("the fight is entered from the memories and nowhere else", "memory_confrontation_encounter", node("inside_the_memory").combatEncounterId)
    }

    @Test
    fun `Voss has what Kaelen saw at her own rung, whether he told her or wrote it, and the shard is given once`() {
        val told = "Tell Voss everything, tonight, whatever it costs to say out loud."
        val written = "Write it down instead. Some truths need to be chosen, not sprung on someone."
        val kept = "Sit with it alone a while longer."
        val scene = mapOf("c7_rung_wary" to "c14_voss_heard_wary", "c7_rung_working" to "c14_voss_heard_working", "c7_rung_trusted" to "c14_voss_heard_trusted")
        rungs.forEach { rung ->
            val s = at("what_kaelen_remembers", flags = setOf(rung), courage = 12, honour = 11)
            assertEquals("told, written and kept, each once at this rung", 3, visible(s).size)
            listOf(told, written).forEach { label ->
                val after = takeLabel(s, label)
                assertEquals(rung, scene.getValue(rung), after.sceneId)
                assertEquals(1, after.inventory.count { it == "shard_of_the_first_ward" })
                assertEquals(listOf("chapter14_end"), visible(after).map { it.nextNodeId })
            }
            val withheld = takeLabel(s, kept)
            assertEquals("c14_voss_withheld", withheld.sceneId)
            assertEquals(1, withheld.inventory.count { it == "shard_of_the_first_ward" })
        }
        assertEquals("the stat gates are the shipped ones: only the ungated answer is left", listOf("c14_voss_withheld"),
            visible(at("what_kaelen_remembers", courage = 1, honour = 0, flags = setOf("c7_rung_wary"))).map { it.nextNodeId })
        assertEquals(setOf("told_voss_everything"), takeLabel(at("what_kaelen_remembers", flags = setOf("c7_rung_wary"), courage = 12, honour = 11), told).flags.intersect(C14_TELLING_FLAGS))
    }

    @Test
    fun `losing to the memory leads on exactly as shipped`() {
        val lost = take(at("memory_confrontation_defeat"), "what_kaelen_remembers")
        assertTrue("lost_to_the_memory_itself" in lost.flags)
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 15
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Voss speaks of how Kaelen asked before the dispatch comes, and Chapter 15's own record is cleared`() {
        val dirty = setOf("read_alone_first", "resolved_despite_sympathy", "lost_to_cinder_reprisal_leader")
        val open = take(at("chapter14_end", flags = dirty + "c13_asked_openly"), "c15_waiting_open")
        val low = take(at("chapter14_end", flags = dirty), "c15_waiting_private")
        listOf(open, low).forEach { assertEquals(emptySet<String>(), it.flags.intersect(C15_TELLING_FLAGS)) }
        listOf("c15_waiting_open", "c15_waiting_private").forEach { assertEquals(listOf("what_selvane_sends"), visible(at(it)).map { c -> c.nextNodeId }) }
        assertTrue(text("c15_waiting_open").contains("where I could hear"))
        assertTrue(text("c15_waiting_private").contains("I did not hear what you asked"))
    }

    @Test
    fun `the dispatch is answered by Ilsevet's own request and the form that noted it, and Voss reads it at her rung`() {
        listOf("the_woman_before_ilsevet_alone", "the_woman_before_ilsevet_with_voss").forEach { id ->
            val after = takeLabel(at(id), "Keep reading.")
            assertEquals(id, "c15_the_reply", after.sceneId)
            assertEquals(id, 1, after.inventory.count { it == "record_of_ashwell" })
        }
        val reading = mapOf("c7_rung_wary" to "c15_voss_reads_wary", "c7_rung_working" to "c15_voss_reads_working", "c7_rung_trusted" to "c15_voss_reads_trusted")
        rungs.forEach { assertEquals(listOf(reading.getValue(it)), visible(at("c15_the_reply", flags = setOf(it))).map { c -> c.nextNodeId }) }
        assertEquals(listOf("c15_voss_reads_wary"), visible(at("c15_the_reply")).map { it.nextNodeId })
        reading.values.forEach { assertEquals(listOf("what_it_made_of_her"), visible(at(it)).map { c -> c.nextNodeId }) }
        // Voss says nothing in the reading: the shipped scene that follows opens on her silence before she speaks
        reading.values.forEach { assertFalse(it, text(it).contains("\"")) }
    }

    @Test
    fun `Ashfall comes back as Kaelen met Ilsevet there`() {
        fun echo(vararg flags: String) = visible(at("what_it_made_of_her", flags = flags.toSet())).map { it.nextNodeId }
        assertEquals(listOf("c15_echo_reached"), echo("warned_ilsevet_directly", "chose_to_help_ilsevet"))
        assertEquals(listOf("c15_echo_reached"), echo("appealed_to_ilsevets_conscience", "chose_to_help_ilsevet"))
        assertEquals(listOf("c15_echo_left"), echo("left_ilsevet_to_her_wreckage", "chose_to_help_ilsevet"))
        assertEquals(listOf("c15_echo_refused"), echo("refused_to_help_ilsevet"))
        assertEquals("a save that holds none of them is told Kaelen let her face it", listOf("c15_echo_refused"), echo())
        listOf("c15_echo_reached", "c15_echo_left", "c15_echo_refused").forEach { assertEquals(listOf("old_debts"), visible(at(it)).map { c -> c.nextNodeId }) }
    }

    @Test
    fun `the choice at Old Debts is still the player's, with every shipped answer and no new one`() {
        val s = at("old_debts", courage = 99, honour = 99)
        assertEquals(setOf("the_reprisal_resolved", "the_reprisal_committed", "the_reprisal_deferred"), visible(s).map { it.nextNodeId }.toSet())
        assertEquals(3, visible(s).size)
        assertEquals(listOf("chapter15_end"), visible(at("after_the_reprisal")).map { it.nextNodeId })
        val end = take(at("chapter15_end", flags = setOf("kept_shards_close", "dispersed_the_shards", "pressed_the_prisoner_for_detail", "lost_to_cinder_reliquary_thief")), "what_kaelen_isnt_prepared_for")
        assertEquals("the end of Chapter 15 clears what Chapter 16 records", emptySet<String>(), end.flags.intersect(C16_TELLING_FLAGS))
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 16
    // ---------------------------------------------------------------------------------------------

    private val split = "\"Split them between Stonebeard, Fenmoor, and Voss. No single loss ends this.\""

    @Test
    fun `a Kaelen who splits the shards puts a piece in Voss's hand first, at her rung, and the choice is otherwise as shipped`() {
        val handing = mapOf("c7_rung_wary" to "c16_handing_wary", "c7_rung_working" to "c16_handing_working", "c7_rung_trusted" to "c16_handing_trusted")
        rungs.forEach { rung ->
            val s = at("what_to_protect", flags = setOf(rung), courage = 14, honour = 13)
            assertEquals(3, visible(s).size)
            val after = takeLabel(s, split)
            assertEquals(rung, handing.getValue(rung), after.sceneId)
            assertEquals(rung, setOf("dispersed_the_shards"), after.flags.intersect(C16_TELLING_FLAGS))
            assertEquals(14, after.courage)
            assertEquals(14, after.honour)
            assertTrue(visible(after).all { it.nextNodeId == "the_reliquary_thief_dispersed" })
        }
        assertEquals("below the shipped Honour gate the split is not offered", 2, visible(at("what_to_protect", courage = 14, honour = 12, flags = setOf("c7_rung_wary"))).size)
        assertEquals(listOf("the_reliquary_thief_close"), visible(at("what_to_protect", courage = 14, honour = 0, flags = setOf("c7_rung_trusted"))).map { it.nextNodeId }.filter { it != "the_reliquary_thief_luck" })
    }

    @Test
    fun `Voss's rung moves one step at most in Chapter 16, up, and only where Kaelen gives the piece freely`() {
        val wary = at("c16_handing_wary", flags = setOf("c7_rung_wary"))
        assertEquals(setOf("c7_rung_working"), takeLabel(wary, "Give it to her without a condition.").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_wary"), takeLabel(wary, "Tell her where the rest will be kept, and why.").flags.intersect(C7_RUNG_FAMILY))
        val working = at("c16_handing_working", flags = setOf("c7_rung_working"))
        assertEquals(setOf("c7_rung_trusted"), takeLabel(working, "Tell her she is the one he trusts most with it.").flags.intersect(C7_RUNG_FAMILY))
        assertEquals(setOf("c7_rung_working"), takeLabel(working, "Tell her to go carefully.").flags.intersect(C7_RUNG_FAMILY))
        val trusted = at("c16_handing_trusted", flags = setOf("c7_rung_trusted"))
        assertEquals(setOf("c7_rung_trusted"), takeLabel(trusted, "Go on.").flags.intersect(C7_RUNG_FAMILY))
    }

    @Test
    fun `what the theft cost is told by the door Kaelen answered Edda, and is the same ending either way`() {
        assertEquals(listOf("c16_aftermath_open"), visible(at("what_was_taken", flags = setOf("c13_door_open"))).map { it.nextNodeId })
        assertEquals(listOf("c16_aftermath_barred"), visible(at("what_was_taken", flags = setOf("c13_door_barred"))).map { it.nextNodeId })
        assertEquals(listOf("c16_aftermath_none"), visible(at("what_was_taken")).map { it.nextNodeId })
        listOf("c16_aftermath_open", "c16_aftermath_barred", "c16_aftermath_none").forEach {
            assertEquals(it, listOf("chapter16_end"), visible(at(it)).map { c -> c.nextNodeId })
            assertTrue(it, text(it).contains("The pieces the thief carried off are more than one"))
            assertTrue(it, text(it).contains("No one else is hurt") || text(it).contains("no one else is hurt"))
        }
        assertEquals("the defeat leads to the same place", listOf("c16_aftermath_none"), visible(take(at("reliquary_thief_defeat"), "what_was_taken")).map { it.nextNodeId })
    }

    @Test
    fun `the shipped lines that counted the shards or the keepers no longer do, and nothing else in those scenes changed`() {
        assertTrue(text("what_to_protect").contains("Several fragments"))
        assertFalse(text("what_to_protect").contains("Four fragments"))
        // a player who refused Ilsevet holds two shards, so the split between "three keepers" became a split between keepers
        assertTrue(text("the_reliquary_thief_dispersed").contains("Splitting the shards between keepers means"))
        assertFalse(text("the_reliquary_thief_dispersed").contains("three keepers"))
        assertTrue(text("the_reliquary_thief_dispersed").contains("the one shard the hold was trusted to keep"))
        val newText = (scenes("chapter_13", "c13_") + scenes("chapter_14", "c14_") + scenes("chapter_15", "c15_") + scenes("chapter_16", "c16_"))
            .joinToString(" ") { it.narrativeText.replace(Regex("\\s+"), " ") }
        listOf("four fragments", "three fragments", "two fragments", "four shards", "three shards", "two shards", "three keepers").forEach {
            assertFalse(it, newText.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `the prisoner's warning names where a shard may have come from, in both versions, for two shards and for three`() {
        val sentence = "Every shard you've carried out of a fight or a failed working: Fenmoor's ward, whatever came away from the frame at Ashfall, " +
            "whatever the vision at this gate left behind."
        // the three shard items and the phrase of the sentence that stands for where each one comes from
        val source = mapOf(
            "fenmoor_ward_shard" to "Fenmoor's ward",
            "shard_of_the_first_ward" to "whatever the vision at this gate left behind",
            "shard_of_the_seventh_door" to "whatever came away from the frame at Ashfall"
        )
        val twoShards = listOf("fenmoor_ward_shard", "shard_of_the_first_ward")
        val threeShards = twoShards + "shard_of_the_seventh_door"
        listOf(
            twoShards to setOf("refused_to_help_ilsevet"),
            threeShards to setOf("chose_to_help_ilsevet", "warned_ilsevet_directly")
        ).forEach { (held, flags) ->
            val start = at("what_kaelen_isnt_prepared_for", flags = flags, inventory = held)
            listOf("the_prisoners_warning_pressed", "the_prisoners_warning_patient").forEach { scene ->
                val said = text(take(start, scene).sceneId)
                val where = "$scene, ${held.size} shards"
                assertTrue(where, said.contains(sentence))
                // whatever shards he holds, the sentence has a source to name for each of them
                held.forEach { assertTrue("$where: the source of $it", said.contains(source.getValue(it))) }
                // Ashfall appears once, and only as what came away from the frame: never the frame itself listed as something he carried
                assertEquals(where, 1, Regex("Ashfall").findAll(said).count())
                assertFalse(where, said.contains("Fenmoor's ward, the frame at Ashfall"))
                // and nothing in the paragraph says he holds all of them
                listOf("all three", "all of them", "both", "each of the three").forEach { assertFalse("$where: $it", said.contains(it, ignoreCase = true)) }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The chapters as played
    // ---------------------------------------------------------------------------------------------

    private fun mix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    private fun score(c: Choice, seed: Int): Long = mix((c.label.hashCode().toLong() shl 32) xor c.nextNodeId.hashCode().toLong() xor mix(seed.toLong()))

    /** Plays from [start] to [stop], winning or losing each fight by the seed, taking the choice the seed scores highest. */
    private fun play(start: GameState, seed: Int, stop: (GameState) -> Boolean): Pair<List<String>, GameState> {
        var s = start
        val trail = ArrayList<String>()
        var steps = 0
        while (!stop(s)) {
            check(steps++ < 200) { "never finished; stuck at ${s.sceneId} (seed $seed)" }
            val n = node(s.sceneId)
            trail += n.id
            s = if (n.combatEncounterId != null) {
                val win = (mix(n.id.hashCode().toLong() xor mix(seed.toLong())) and 1L) == 0L
                s.resolveCombat(CombatRepository.encounter(n.combatEncounterId!!), CombatOutcome(victory = win, damageTaken = 0))
            } else {
                val choices = visible(s)
                check(choices.isNotEmpty()) { "dead end at ${n.id} with ${s.flags} (seed $seed)" }
                s.applyChoice(choices.maxByOrNull { score(it, seed) }!!)
            }
        }
        return trail to s
    }

    private fun starts(random: Random, node: String): GameState {
        val flags = HashSet<String>()
        flags += rungs[random.nextInt(3)]
        flags += listOf("listened_to_the_prisoner", "refused_the_prisoner")[random.nextInt(2)]
        flags += listOf("accepted_fully", "accepted_reluctantly", "refused_once")[random.nextInt(3)]
        when (random.nextInt(4)) {
            0 -> flags += listOf("warned_ilsevet_directly", "chose_to_help_ilsevet")
            1 -> flags += listOf("appealed_to_ilsevets_conscience", "chose_to_help_ilsevet")
            2 -> flags += listOf("left_ilsevet_to_her_wreckage", "chose_to_help_ilsevet")
            else -> flags += "refused_to_help_ilsevet"
        }
        when (random.nextInt(3)) { 0 -> flags += "c13_door_open"; 1 -> flags += "c13_door_barred" }
        if (random.nextBoolean()) flags += "c13_asked_openly"
        val inventory = ArrayList<String>().apply {
            add("fenmoor_ward_shard")
            if ("chose_to_help_ilsevet" in flags) add("shard_of_the_seventh_door")
        }
        return at(node, flags = flags, courage = 15, honour = 14, inventory = inventory)
    }

    @Test
    fun `fifteen hundred tellings of Chapters 14 to 16 pass through every new scene they should and nothing else`() {
        val random = Random(0x1416L)
        val seen = HashMap<String, Int>()
        repeat(1500) { run ->
            val ends = listOf("chapter13_end_training", "chapter13_end_watch", "chapter13_end_deferred")
            val start = starts(random, ends[random.nextInt(3)])
            val (trail, end) = play(start, run) { it.sceneId == "the_signal" }
            trail.filter { it.startsWith("c14_") || it.startsWith("c15_") || it.startsWith("c16_") }.forEach { seen[it] = (seen[it] ?: 0) + 1 }
            val flags = start.flags
            fun has(id: String) = id in trail
            val ch13End = start.sceneId
            // Chapter 14
            assertTrue("run $run", has(mapOf("chapter13_end_training" to "c14_gate_training", "chapter13_end_watch" to "c14_gate_watch", "chapter13_end_deferred" to "c14_gate_deferred").getValue(ch13End)))
            assertTrue("run $run: knock", has(if ("listened_to_the_prisoner" in flags) "c14_knock_listened" else "c14_knock_refused"))
            assertTrue("run $run: office", has(mapOf("accepted_fully" to "c14_office_full", "accepted_reluctantly" to "c14_office_reluctant", "refused_once" to "c14_office_refusedonce").entries.first { it.key in flags }.value))
            assertEquals("run $run: Voss hears or does not", 1, trail.count { it.startsWith("c14_voss_") })
            assertEquals("run $run: the order of the memories", true, trail.indexOf("c14_knock_" + (if ("listened_to_the_prisoner" in flags) "listened" else "refused")) < trail.indexOf("inside_the_memory"))
            val told = "told_voss_everything" in end.flags || "recorded_the_vision_first" in end.flags
            assertEquals("run $run", if (told) 1 else 0, trail.count { it.startsWith("c14_voss_heard_") })
            assertEquals("run $run", if (told) 0 else 1, trail.count { it == "c14_voss_withheld" })
            // Chapter 15
            assertTrue("run $run: waiting", has(if ("c13_asked_openly" in flags) "c15_waiting_open" else "c15_waiting_private"))
            assertTrue("run $run: the reply", has("c15_the_reply"))
            assertEquals("run $run: one reading", 1, trail.count { it.startsWith("c15_voss_reads_") })
            assertEquals("run $run: one echo", 1, trail.count { it.startsWith("c15_echo_") })
            val echo = when {
                "left_ilsevet_to_her_wreckage" in flags -> "c15_echo_left"
                "refused_to_help_ilsevet" in flags -> "c15_echo_refused"
                else -> "c15_echo_reached"
            }
            assertTrue("run $run: $echo", has(echo))
            // Chapter 16
            val split = "dispersed_the_shards" in end.flags
            assertEquals("run $run: a piece for Voss only where the shards are split", if (split) 1 else 0, trail.count { it.startsWith("c16_handing_") })
            val door = when { "c13_door_open" in flags -> "c16_aftermath_open"; "c13_door_barred" in flags -> "c16_aftermath_barred"; else -> "c16_aftermath_none" }
            assertTrue("run $run: $door", has(door))
            assertEquals("run $run: one aftermath", 1, trail.count { it.startsWith("c16_aftermath_") })
            // the rung moves up by at most the one step Chapter 16 allows, and the Chapters 14 and 15 scenes move nothing
            val before = rungs.indexOf(rungs.first { it in flags })
            val after = rungs.indexOf(rungs.first { it in end.flags })
            assertTrue("run $run: rung $before to $after", after - before in 0..1)
            assertEquals("run $run: one rung", 1, end.flags.count { it in rungs })
            assertEquals("run $run", "the_signal", end.sceneId)
            assertTrue("run $run: the shards are carried on", end.inventory.count { it == "shard_of_the_first_ward" } == 1)
            assertTrue("run $run: reading length ${trail.size}", trail.size in 20..40)
        }
        val newIds = (scenes("chapter_14", "c14_") + scenes("chapter_15", "c15_") + scenes("chapter_16", "c16_")).map { it.id }
        assertEquals("new scenes no telling reached", emptyList<String>(), newIds.filter { (seen[it] ?: 0) == 0 })
    }

    @Test
    fun `Chapters 14 to 16 have their shipped scenes and the new ones the package adds, and gain no combat, item or trophy`() {
        assertEquals(9 + 12, StoryRepository.nodesInChapter("chapter_14").size)
        assertEquals(12, scenes("chapter_14", "c14_").size)
        assertEquals(11 + 9, StoryRepository.nodesInChapter("chapter_15").size)
        assertEquals(9, scenes("chapter_15", "c15_").size)
        assertEquals(10 + 6, StoryRepository.nodesInChapter("chapter_16").size)
        assertEquals(6, scenes("chapter_16", "c16_").size)
        (scenes("chapter_14", "c14_") + scenes("chapter_15", "c15_") + scenes("chapter_16", "c16_")).forEach { n ->
            assertEquals(n.id, null, n.combatEncounterId)
            n.choices.forEach { c ->
                assertTrue("${n.id}: grants nothing", c.consequences.grantItemIds.isEmpty())
                assertEquals("${n.id}: no trophy", null, c.consequences.unlockTrophy)
                assertTrue("${n.id}: no flag set", c.consequences.setFlags.all { it in C7_RUNG_FAMILY })
                assertTrue("${n.id}: no stat change", c.consequences.statDeltas.isEmpty())
            }
        }
    }
}
