package com.thelastjailer.app

import com.thelastjailer.app.data.Chapter1Paths
import com.thelastjailer.app.data.OPENING_FAMILY
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C1-3: Route C (the trees): off the road, the gully, the old shaft, and the Forge's shaft option. */
class Chapter1RouteCTest {

    private fun choose(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun node(id: String) = StoryRepository.node(id)

    private fun intoTheTrees() = choose(GameState(), "WALK AWAY")
    private fun searched() = choose(intoTheTrees(), "Search the gully")
    private fun left() = choose(intoTheTrees(), "Leave it")
    private fun atShaft(s: GameState) = choose(s, "Climb out")
    private fun atRoots(s: GameState) = choose(choose(atShaft(s), "Go back up"), "Follow the broken ground")

    // ---- Shape ----

    @Test
    fun `WALK AWAY leads into the trees and costs only a fall`() {
        val s = intoTheTrees()
        assertEquals("ch1_into_the_trees", s.sceneId)
        assertEquals(setOf("walked_away", "hunted_on_the_road"), s.flags)
        assertEquals(GameState().honour + 1, s.honour)
        assertEquals(GameState().courage, s.courage)
        assertEquals(GameState().gold, s.gold)
        assertTrue("a short fall, not a wound: ${s.health}", s.health in 90..95)
    }

    @Test
    fun `both gully branches converge through the old shaft into the protected walked_the_road and on to the descent`() {
        listOf(searched(), left()).forEach { g ->
            val shaft = atShaft(g)
            assertEquals("ch1_the_old_shaft", shaft.sceneId)
            val road = choose(shaft, "Go back up")
            assertEquals("walked_the_road", road.sceneId)
            assertEquals("beneath_the_roots", choose(road, "Follow the broken ground").sceneId)
        }
    }

    @Test
    fun `walked_the_road is reached only through the shaft scene and still has its single exit`() {
        val refs = StoryRepository.nodesInChapter("chapter_1").flatMap { n -> n.choices.map { n.id to it } }
            .filter { it.second.nextNodeId == "walked_the_road" }
        assertEquals(listOf("ch1_the_old_shaft"), refs.map { it.first })
        assertEquals(listOf("Follow the broken ground down." to "beneath_the_roots"), node("walked_the_road").choices.map { it.label to it.nextNodeId })
    }

    // ---- The gully decision is a real trade ----

    @Test
    fun `searching keeps the gold and finds the cord at the price of health`() {
        val before = intoTheTrees()
        val after = searched()
        assertEquals("ch1_gully_searched", after.sceneId)
        assertEquals(before.gold, after.gold)
        assertTrue("found_red_cord" in after.flags)
        assertTrue("a few health lost: ${before.health} -> ${after.health}", after.health < before.health + 1)
        assertEquals(before.courage, after.courage)
        assertEquals(before.honour, after.honour)
        assertEquals(before.inventory, after.inventory)
    }

    @Test
    fun `leaving it costs ten gold, finds nothing and costs no health`() {
        val before = intoTheTrees()
        val after = left()
        assertEquals("ch1_gully_left", after.sceneId)
        assertEquals(before.gold - 10, after.gold)
        assertFalse("found_red_cord" in after.flags)
        assertTrue(after.health >= before.health)
        assertEquals(before.inventory, after.inventory)
    }

    @Test
    fun `neither gully branch gives courage, honour, items or xp`() {
        listOf(searched(), left()).forEach { g ->
            val end = atRoots(g)
            assertEquals(GameState().courage, end.courage)
            assertEquals(GameState().honour + 1, end.honour)
            assertEquals(emptyList<String>(), end.inventory)
            assertEquals(0, end.xp)
            assertEquals(emptySet<String>(), end.trophies)
        }
    }

    @Test
    fun `the shaft scene always marks the escape and the shaft, on both branches`() {
        listOf(searched(), left()).forEach { g ->
            val flags = atRoots(g).flags
            assertTrue("evaded_the_riders" in flags && "found_the_old_shaft" in flags)
            assertTrue("walked_away" in flags && "hunted_on_the_road" in flags)
        }
    }

    @Test
    fun `losses cannot go below zero on a replay with little gold or health`() {
        val poor = GameState(chapterId = "chapter_1", sceneId = "fallen_knight", gold = 4, health = 3)
        val trees = choose(poor, "WALK AWAY")
        assertTrue(trees.health >= 0)
        assertEquals(0, choose(trees, "Leave it").gold)
        assertTrue(choose(trees, "Search the gully").health >= 0)
    }

    // ---- Prose ----

    @Test
    fun `the new scenes show the red cord and keep the hunters private`() {
        val ids = listOf("ch1_into_the_trees", "ch1_gully_searched", "ch1_gully_left", "ch1_the_old_shaft", "ch1_watched_from_above")
        val text = ids.joinToString("\n") { node(it).narrativeText }
        assertTrue(node("ch1_gully_searched").narrativeText.contains("red cord"))
        listOf("Ilsevet", "Order", "prisoner", "Marshal", "Selvane", "Thessaly", "paid", "contract", "employer", "hired", "bounty", "your name", "his name", "Door of")
            .forEach { assertFalse("Route C reveals '$it'", text.contains(it, ignoreCase = true)) }
    }

    @Test
    fun `every new scene is in chapter 1 and has at least one way on`() {
        listOf("ch1_into_the_trees", "ch1_gully_searched", "ch1_gully_left", "ch1_the_old_shaft", "ch1_watched_from_above").forEach {
            assertEquals(it, "chapter_1", node(it).chapterId)
            assertTrue(it, node(it).choices.isNotEmpty())
        }
    }

    // ---- The Forge ----

    private fun forgeAfter(start: GameState, vararg route: String): GameState {
        var s = Chapter1Paths.follow(start, *route)
        while (s.sceneId != "beneath_the_roots") s = Chapter1Paths.pick(s, "")
        return Chapter1Paths.follow(s, "Keep", "Follow the tunnel")
    }

    private fun forgeLabels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }

    @Test
    fun `the Forge offers the shaft only to a player who found it`() {
        val c = forgeAfter(GameState(), "WALK AWAY")
        assertEquals("the_silent_forge", c.sceneId)
        assertEquals(listOf("Call out into the dark.", "Wait, and listen.", "Find the old shaft and watch from above."), forgeLabels(c))
        assertFalse(forgeLabels(forgeAfter(GameState(), "OPEN THE DOOR")).any { it.startsWith("Find the old shaft") })
        assertFalse(forgeLabels(forgeAfter(GameState(), "DRAW YOUR SWORD")).any { it.startsWith("Find the old shaft") })
    }

    @Test
    fun `the shaft route is an unobserved moment worth the same Courage as the other sneak, and rejoins the hold`() {
        val forge = forgeAfter(GameState(), "WALK AWAY")
        val above = choose(forge, "Find the old shaft")
        assertEquals("ch1_watched_from_above", above.sceneId)
        assertEquals(forge.courage + 1, above.courage)
        assertEquals(forge.flags, above.flags)
        val hold = choose(above, "Step into the light")
        assertEquals("stonebeard_hold", hold.sceneId)
        // The Stonebeard decisions all still work after it.
        listOf("Help him", "Tell him honestly", "Move on").forEach { assertTrue(it, choose(hold, it).sceneId.startsWith("smiths_warning")) }
    }

    @Test
    fun `every Route C telling still reaches the first fight`() {
        val ends = Chapter1Paths.enumerate(GameState(), setOf("first_blood")).filter { it.labels.first().startsWith("WALK") }
        assertEquals(48, ends.size)
        ends.forEach { assertEquals("first_blood", it.end.sceneId) }
    }

    // ---- Replay and old saves ----

    @Test
    fun `a replay through another route clears every Route C flag, so a stale shaft never unlocks the Forge option`() {
        val routeC = atRoots(searched())
        assertTrue(listOf("found_red_cord", "found_the_old_shaft", "evaded_the_riders").all { it in routeC.flags })
        val again = routeC.copy(sceneId = "fallen_knight")
        listOf("OPEN THE DOOR", "DRAW YOUR SWORD").forEach { first ->
            val s = choose(again, first)
            assertTrue(first, OPENING_FAMILY.intersect(s.flags).none { it in setOf("found_red_cord", "found_the_old_shaft", "evaded_the_riders", "walked_away") })
            assertFalse(first, forgeLabels(forgeAfter(again, first)).any { it.startsWith("Find the old shaft") })
        }
    }

    @Test
    fun `a replay of Route C after Route C starts clean, so the cord is found only if searched again`() {
        val first = atRoots(searched())
        val second = choose(first.copy(sceneId = "fallen_knight"), "WALK AWAY")
        assertFalse("found_red_cord" in second.flags)
        assertFalse("found_red_cord" in choose(second, "Leave it").flags)
        assertTrue("found_red_cord" in choose(second, "Search the gully").flags)
    }

    @Test
    fun `a save parked on the old walked_the_road node plays on exactly as before`() {
        val store = SaveStore(FakeSharedPreferences())
        val s = GameState(chapterId = "chapter_1", sceneId = "walked_the_road", flags = setOf("walked_away"), gold = 25, honour = 1)
        store.save(1, s)
        val loaded = store.load(1)!!
        assertEquals(s, loaded)
        val after = choose(loaded, "Follow the broken ground")
        assertEquals("beneath_the_roots", after.sceneId)
        assertEquals(s.flags, after.flags)
        assertEquals(s.gold, after.gold)
    }

    @Test
    fun `an old save that walked away before this update and now reaches the Forge simply has no shaft option`() {
        val old = GameState(chapterId = "chapter_1", sceneId = "the_silent_forge", flags = setOf("walked_away", "kept_the_token"))
        assertEquals(listOf("Call out into the dark.", "Wait, and listen."), forgeLabels(old))
    }
}
