package com.thelastjailer.app

import com.thelastjailer.app.data.Chapter1Paths
import com.thelastjailer.app.data.OPENING_FAMILY
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-2: Route A (the Door) gains a witness scene, and every `fallen_knight` choice resets the opening
 * flag family so a replay never carries two routes at once.
 */
class Chapter1OpeningResetTest {

    private val unrelated = setOf("kept_the_token", "helped_dwarf", "halvard_died", "unknown_future_flag")

    private fun atOpeningWithEveryOpeningFlag() = GameState(
        chapterId = "chapter_1", sceneId = "fallen_knight", courage = 9, honour = 6, gold = 90, level = 2, xp = 30,
        inventory = listOf("dwarven_token", "sturdy_buckler"),
        trophies = setOf("Friend of Stonebeard", "The Watch Goes On"),
        flags = OPENING_FAMILY + unrelated
    )

    private fun choose(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)

    @Test
    fun `the opening family is the route flags plus every decision flag the opening scenes set`() {
        assertEquals(
            setOf(
                "opened_black_door", "drew_sword", "walked_away", "hunted_on_the_road", "hunters_saw_door",
                "met_red_cord_rider", "evaded_the_riders", "found_the_old_shaft", "found_red_cord",
                "fought_the_riders", "parleyed_with_rider", "withdrew_from_the_riders", "fell_through_the_door",
                "spared_the_rider", "robbed_the_rider", "rider_clue", "knows_riders_hired",
                "searched_the_wagon", "stripped_the_wagon", "left_the_wagon_be"
            ),
            OPENING_FAMILY
        )
    }

    @Test
    fun `each opening choice clears the whole family and keeps only its own route flags`() {
        val expected = mapOf(
            "OPEN THE DOOR" to setOf("opened_black_door", "hunted_on_the_road"),
            "TURN AND FACE THEM" to setOf("drew_sword", "hunted_on_the_road", "met_red_cord_rider"),
            "BREAK FOR THE TREES" to setOf("walked_away", "hunted_on_the_road")
        )
        expected.forEach { (prefix, own) ->
            assertEquals(prefix, unrelated + own, choose(atOpeningWithEveryOpeningFlag(), prefix).flags)
        }
    }

    @Test
    fun `the reset moves no stats, gold, items or trophies beyond each choice's own existing reward`() {
        val before = atOpeningWithEveryOpeningFlag()
        val bonus = mapOf("OPEN THE DOOR" to (2 to 0), "TURN AND FACE THEM" to (1 to 0), "BREAK FOR THE TREES" to (0 to 1))
        bonus.forEach { (prefix, d) ->
            val after = choose(before, prefix)
            assertEquals(prefix, before.courage + d.first, after.courage)
            assertEquals(prefix, before.honour + d.second, after.honour)
            assertEquals(before.gold, after.gold)
            assertEquals(before.level, after.level)
            assertEquals(before.inventory, after.inventory)
            assertEquals(before.trophies, after.trophies)
        }
    }

    @Test
    fun `only the three fallen_knight choices clear opening flags, and nothing after them does`() {
        val clearers = StoryRepository.nodesInChapter("chapter_1").flatMap { n ->
            n.choices.filter { it.consequences.clearFlags.intersect(OPENING_FAMILY).isNotEmpty() }.map { n.id }
        }
        assertEquals(List(3) { "fallen_knight" }, clearers)
        StoryRepository.node("fallen_knight").choices.forEach { assertEquals(OPENING_FAMILY, it.consequences.clearFlags) }
        assertTrue(StoryRepository.node("ch1_door_witness").choices.all { it.consequences.clearFlags.isEmpty() })
    }

    // ---- Route A ----

    @Test
    fun `OPEN THE DOOR now passes through the witness scene and still reaches the descent unchanged`() {
        val fk = StoryRepository.node("fallen_knight").choices.first { it.label == "OPEN THE DOOR" }
        assertEquals("ch1_door_witness", fk.nextNodeId)
        val w = StoryRepository.node("ch1_door_witness")
        assertEquals("chapter_1", w.chapterId)
        assertEquals(listOf("Go through." to "black_door_opened"), w.choices.map { it.label to it.nextNodeId })
        assertEquals(listOf("Descend into the dark." to "beneath_the_roots"),
            StoryRepository.node("black_door_opened").choices.map { it.label to it.nextNodeId })
    }

    @Test
    fun `Route A sets the witness flags and costs nothing`() {
        val atWitness = choose(GameState(), "OPEN THE DOOR")
        assertEquals("ch1_door_witness", atWitness.sceneId)
        assertEquals(setOf("opened_black_door", "hunted_on_the_road"), atWitness.flags)
        val through = choose(atWitness, "Go through")
        assertEquals("black_door_opened", through.sceneId)
        assertEquals(setOf("opened_black_door", "hunted_on_the_road", "hunters_saw_door", "met_red_cord_rider"), through.flags)
        val roots = choose(through, "Descend")
        assertEquals("beneath_the_roots", roots.sceneId)
        assertEquals(listOf(GameState().courage + 2, GameState().honour, 100, 25, 0, 1), listOf(roots.courage, roots.honour, roots.health, roots.gold, roots.xp, roots.level))
        assertEquals(emptyList<String>(), roots.inventory)
        assertEquals(emptySet<String>(), roots.trophies)
    }

    @Test
    fun `the witness prose shows the red cord and keeps the hunters private`() {
        val t = StoryRepository.node("ch1_door_witness").narrativeText
        assertTrue(t.contains("red cord"))
        listOf("Ilsevet", "Order", "prisoner", "Door of", "hunter", "Marshal", "Selvane", "Thessaly", "paid", "contract", "employer", "your name", "his name")
            .forEach { assertFalse("witness scene reveals '$it'", t.contains(it, ignoreCase = true)) }
    }

    @Test
    fun `the witness scene is used only by the door route, and Route B now starts at the stand`() {
        val refs = StoryRepository.nodesInChapter("chapter_1").flatMap { n -> n.choices.map { n.id to it } }
            .filter { it.second.nextNodeId == "ch1_door_witness" }
        assertEquals(listOf("fallen_knight"), refs.map { it.first })
        assertEquals("ch1_stand_and_turn", StoryRepository.node("fallen_knight").choices.first { it.label == "TURN AND FACE THEM" }.nextNodeId)
        val sword = choose(GameState(), "TURN AND FACE THEM")
        assertEquals(setOf("drew_sword", "hunted_on_the_road", "met_red_cord_rider"), sword.flags)
        assertEquals(GameState().health, sword.health)
    }

    // ---- Replay ----

    @Test
    fun `replaying a different route leaves only the new route's flags`() {
        var s = choose(choose(choose(GameState(), "OPEN THE DOOR"), "Go through"), "Descend")
        assertTrue("hunters_saw_door" in s.flags)
        // Second telling, via the sword.
        s = choose(s.copy(sceneId = "fallen_knight"), "TURN AND FACE THEM")
        assertEquals(setOf("drew_sword", "hunted_on_the_road", "met_red_cord_rider"), s.flags)
        // Third telling, via the walk.
        s = choose(s.copy(sceneId = "fallen_knight"), "BREAK FOR THE TREES")
        assertEquals(setOf("walked_away", "hunted_on_the_road"), s.flags)
        // And back to the door.
        s = choose(s.copy(sceneId = "fallen_knight"), "OPEN THE DOOR")
        assertEquals(setOf("opened_black_door", "hunted_on_the_road"), s.flags)
    }

    @Test
    fun `a stale drew_sword no longer unlocks the Forge sneak option on a later telling`() {
        val stale = GameState(chapterId = "chapter_1", sceneId = "fallen_knight", flags = setOf("drew_sword", "crept_in"))
        var s = Chapter1Paths.follow(stale, "OPEN THE DOOR", "", "", "Keep", "Follow the tunnel")
        val labels = StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).map { it.label }
        assertFalse("stale sword flag leaked: $labels", labels.any { it.startsWith("Move without a sound") })
        s = Chapter1Paths.follow(stale, "TURN AND FACE THEM", "Back through", "", "", "Keep", "Follow the tunnel")
        assertTrue(StoryRepository.visibleChoices(StoryRepository.node(s.sceneId), s).any { it.label.startsWith("Move without a sound") })
    }

    // ---- Existing saves ----

    private fun roundTrip(state: GameState): GameState {
        val store = SaveStore(FakeSharedPreferences())
        store.save(1, state)
        return store.load(1)!!
    }

    @Test
    fun `loading never clears - saves keep every opening flag until a new opening choice is made`() {
        listOf("fallen_knight", "ch1_door_witness", "black_door_opened", "sword_drawn", "walked_the_road", "beneath_the_roots").forEach { id ->
            val s = GameState(chapterId = "chapter_1", sceneId = id, flags = OPENING_FAMILY + unrelated)
            assertEquals(id, s, roundTrip(s))
        }
    }

    @Test
    fun `saves parked on the legacy route nodes with no new flags play on exactly as before`() {
        listOf("black_door_opened" to "Descend", "sword_drawn" to "Enter", "walked_the_road" to "Follow").forEach { (id, label) ->
            val s = GameState(chapterId = "chapter_1", sceneId = id, flags = setOf("opened_black_door"))
            val after = choose(roundTrip(s), label)
            assertEquals("beneath_the_roots", after.sceneId)
            assertEquals(s.flags, after.flags)
        }
    }
}
