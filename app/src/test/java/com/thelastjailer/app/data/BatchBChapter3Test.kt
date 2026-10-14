package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.applyChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Follow-up Batch B: a shorter refusal sequence in Chapter 3, and Voss reacting to what Kaelen
 * actually did at the Order's camp (theft, parley, or neither).
 */
class BatchBChapter3Test {

    private fun node(id: String) = StoryRepository.node(id)
    private fun pick(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun labels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }
    private fun at(id: String, flags: Set<String> = emptySet(), honour: Int = 0, courage: Int = 1) =
        GameState(chapterId = node(id).chapterId, sceneId = id, flags = flags, honour = honour, courage = courage)
    private fun text(id: String) = node(id).narrativeText.replace(Regex("\\s+"), " ")

    private val allNodes = listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }
    private fun referrers(target: String) = allNodes.filter { n -> n.choices.any { it.nextNodeId == target } }.map { it.id }

    // ---------------------------------------------------------------- Refusal sequence

    @Test
    fun `asking for time now takes two short scenes before the rite, not four`() {
        val start = at("the_answer", honour = 2)
        val refused = pick(start, "\"I need time")
        val visited = mutableListOf(refused.sceneId)
        var s = refused
        s = pick(s, "Go back"); visited += s.sceneId
        s = pick(s, "Give him your answer"); visited += s.sceneId
        assertEquals(listOf("the_walk_away", "the_return", "the_binding_rite_returned"), visited)
        assertEquals("the_first_true_knock", pick(s, "Endure").sceneId)
    }

    @Test
    fun `the refusal still differs from accepting and still ends in the binding, brand and trophy`() {
        val start = at("the_answer", honour = 2, courage = 2)
        val refused = pick(start, "\"I need time")
        assertTrue("refused_once" in refused.flags)
        assertFalse("accepted_fully" in refused.flags || "accepted_reluctantly" in refused.flags)
        val returned = pick(pick(refused, "Go back"), "Give him your answer")
        val bound = pick(returned, "Endure")
        assertTrue("bound_to_the_gate" in bound.flags && "refused_once" in bound.flags)
        assertTrue("jailers_brand" in bound.inventory)
        assertTrue("The Last Jailer" in bound.trophies)
        // Same Honour as before the change: the single +1 for going back.
        assertEquals(start.honour + 1, returned.honour)
        val accepted = pick(start, "\"I accept the office")
        assertTrue("accepted_fully" in accepted.flags && "refused_once" !in accepted.flags)
    }

    @Test
    fun `the merged scene keeps Kaelen's hesitation and the tremor that ends it`() {
        val t = text("the_walk_away")
        listOf("Halvard doesn't call after him", "only a delay", "simply not decide", "six wardens down to one", "Neither thought makes the decision for him",
            "the floor does", "on his feet before he's decided to be").forEach { assertTrue(it, t.contains(it)) }
        assertEquals(listOf("Go back."), node("the_walk_away").choices.map { it.label })
    }

    @Test
    fun `the two retired scenes are untouched and reachable only from each other, so parked saves play on`() {
        assertEquals(listOf("the_walk_away"), referrers("the_return").filter { it == "the_walk_away" })
        assertTrue(referrers("what_the_road_offers").isEmpty())
        assertEquals(listOf("what_the_road_offers"), referrers("the_ground_remembers"))
        val store = SaveStore(FakeSharedPreferences())
        listOf("the_walk_away", "what_the_road_offers", "the_ground_remembers", "the_return").forEachIndexed { i, id ->
            val parked = at(id, flags = setOf("refused_once"), honour = 1)
            store.save(i + 1, parked)
            var s = store.load(i + 1)!!
            assertEquals(id, parked.copy(activeSlot = i + 1), s)
            var guard = 0
            while (s.sceneId != "the_binding_rite_returned") {
                s = s.applyChoice(StoryRepository.visibleChoices(node(s.sceneId), s).first())
                check(++guard < 8) { "stuck from $id" }
            }
            // Parked on a retired scene, the player still gets the single +1 Honour exactly once.
            assertEquals(id, if (id == "the_return") 1 else 2, s.honour)
        }
    }

    // ---------------------------------------------------------------- Voss and the camp

    private fun arrival(path: String) = when (path) {
        "robbed" -> pick(at("ch3_camp_robbed", flags = setOf("robbed_the_order"), honour = 1), "Walk down to the door")
        "parley" -> pick(at("ch3_camp_parley", flags = setOf("parleyed_with_sentry"), honour = 1), "Walk down to the door")
        "studied" -> pick(at("ch3_camp_studied", flags = setOf("read_voss_orders"), honour = 1), "Walk down to the door")
        else -> pick(at("ch3_order_camp", honour = 1), "Walk down to the door")
    }

    @Test
    fun `theft, parley and neither each arrive at a different Voss, and only the earned reaction appears`() {
        assertEquals("ch3_surface_robbed", arrival("robbed").sceneId)
        assertEquals("ch3_surface_parleyed", arrival("parley").sceneId)
        assertEquals("ch3_surface_approached", arrival("studied").sceneId)
        assertEquals("ch3_surface_approached", arrival("direct").sceneId)
        assertTrue(text("ch3_surface_robbed").contains("A purse of coin has gone missing from a tent my people were meant to be watching") && text("ch3_surface_robbed").contains("coincidence"))
        assertTrue(text("ch3_surface_parleyed").contains("asked leave"))
        listOf("ch3_surface_approached", "the_surface_again").forEach {
            val t = text(it)
            assertFalse(it, t.contains("purse of coin") || t.contains("asked leave") || t.contains("coincidence"))
        }
        // The theft line never shows on the parley arrival, or the reverse.
        assertFalse(text("ch3_surface_parleyed").contains("purse of coin"))
        assertFalse(text("ch3_surface_robbed").contains("asked leave"))
    }

    @Test
    fun `the reactions cost and pay nothing, and Voss still offers the same earned rows`() {
        listOf("robbed", "parley").forEach { path ->
            val before = when (path) {
                "robbed" -> at("ch3_camp_robbed", flags = setOf("robbed_the_order", "knows_riders_hired"), honour = 1, courage = 3)
                else -> at("ch3_camp_parley", flags = setOf("parleyed_with_sentry", "knows_riders_hired"), honour = 1, courage = 3)
            }
            val arrived = pick(before, "Walk down to the door")
            assertEquals(before.copy(sceneId = arrived.sceneId), arrived)
            val plain = before.copy(sceneId = "the_surface_again")
            assertEquals(path, labels(plain), labels(arrived))
        }
    }

    @Test
    fun `Voss suspects the theft but does not know it, and never reveals what she was not told`() {
        val robbed = text("ch3_surface_robbed")
        assertTrue(robbed.contains("I'm sure that's a coincidence"))
        listOf("you stole", "thief", "you took", "you robbed", "I know it was you", "Ilsevet", "prisoner", "who paid", "hired", "disc")
            .forEach { assertFalse("robbed arrival says '$it'", robbed.contains(it, ignoreCase = true)) }
        val parley = text("ch3_surface_parleyed")
        listOf("Ilsevet", "prisoner", "who paid", "hired", "disc", "you stole", "thief").forEach { assertFalse("parley arrival says '$it'", parley.contains(it, ignoreCase = true)) }
        // What she knows of the parley she could have been told by the sentry, who is shown speaking to her.
        assertTrue(parley.contains("The sentry has already crossed to the door ahead of him"))
    }

    @Test
    fun `the new arrivals are reached only from their own camp scenes`() {
        assertEquals(listOf("ch3_camp_robbed"), referrers("ch3_surface_robbed"))
        assertEquals(listOf("ch3_camp_parley"), referrers("ch3_surface_parleyed"))
        assertEquals(setOf("ch3_order_camp", "ch3_camp_studied"), referrers("ch3_surface_approached").toSet())
        listOf("ch3_surface_robbed", "ch3_surface_parleyed").forEach { assertEquals(it, "chapter_3", node(it).chapterId) }
    }

    @Test
    fun `a stale theft flag from an earlier telling cannot colour a later arrival, and the family resets`() {
        val stale = at("ch3_order_camp", flags = setOf("robbed_the_order", "parleyed_with_sentry"), honour = 1)
        assertEquals("ch3_surface_approached", pick(stale, "Walk down to the door").sceneId)
        val answered = pick(at("the_answer", flags = setOf("robbed_the_order", "parleyed_with_sentry", "approached_by_camp")), "\"I accept the office")
        assertTrue(listOf("robbed_the_order", "parleyed_with_sentry", "approached_by_camp").none { it in answered.flags })
    }

    @Test
    fun `Journal lines match what happened and neither appears for a player who did neither`() {
        val robbed = FlagDescriptions.sections(setOf("robbed_the_order")).flatMap { it.lines }
        val parley = FlagDescriptions.sections(setOf("parleyed_with_sentry")).flatMap { it.lines }
        assertEquals(listOf("I took coin and a vial from the Order's stores before I met her."), robbed)
        assertEquals(listOf("I asked an Order sentry for leave to speak, and he gave it."), parley)
        assertTrue(FlagDescriptions.sections(setOf("read_voss_orders")).flatMap { it.lines }.none { it.contains("stores") || it.contains("leave to speak") })
    }

    @Test
    fun `every way through the camp still reaches Voss and the pilot node`() {
        listOf("ch3_surface_robbed", "ch3_surface_parleyed", "ch3_surface_approached").forEach { id ->
            val s = at(id, courage = 1)
            val onward = pick(s, "\"What do you want with it")
            assertEquals(id, "a_choice_at_the_door", onward.sceneId)
            assertTrue(labels(onward).any { it.startsWith("\"Give me time") })
        }
    }

    @Test
    fun `the camp and every Voss arrival agree on riders, horses and Voss's mount`() {
        // Voss plus four riders: five people, five horses, five bedrolls. Voss rides (chapter3_end_stalled).
        assertTrue(text("ch3_order_camp").contains("Five horses. Five bedrolls."))
        listOf("ch3_surface_approached", "ch3_surface_robbed", "ch3_surface_parleyed").forEach {
            assertTrue(it, text(it).contains("five horses lift their heads together"))
            assertFalse(it, text(it).contains("four horses"))
        }
        assertTrue(text("ch3_surface_approached").contains("Four riders"))
        assertTrue(text("the_surface_again").contains("Four riders wait beside it") && text("the_surface_again").contains("a fifth figure"))
        assertTrue(text("a_choice_at_the_door").contains("The four behind her"))
        assertTrue(text("chapter3_end_stalled").contains("swings back up onto her horse"))
        val everyCampLine = listOf("ch3_order_camp", "ch3_camp_studied", "ch3_camp_robbed", "ch3_camp_parley", "ch3_surface_approached",
            "ch3_surface_robbed", "ch3_surface_parleyed", "the_surface_again").joinToString(" ") { text(it) }
        assertFalse("no stray 'Four horses' or 'Four bedrolls'", Regex("four (horses|bedrolls)", RegexOption.IGNORE_CASE).containsMatchIn(everyCampLine))
    }
}
