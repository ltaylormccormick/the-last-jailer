package com.thelastjailer.app

import com.thelastjailer.app.data.Chapter1Paths
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-0 old-save compatibility fixtures. These write the raw preference keys an existing install
 * already has on disk (not a freshly built GameState), load them through [SaveStore], and prove the
 * player can keep playing. The fixtures deliberately use non-default stats, duplicate items, trophies
 * and later-chapter flags, so nothing here quietly depends on a brand-new game's starting values.
 *
 * None of the fixtures contain any of the flags the Chapter 1 redesign will introduce: they model
 * saves made BEFORE the update.
 */
class Chapter1SaveCompatibilityTest {

    /**
     * Every flag the Chapters 1-3 batch introduced. A save made before the update can carry none of them,
     * and the story must treat that exactly like a player who simply never reached those choices.
     * (`hunted_on_the_road`, `hunters_saw_door`, `met_red_cord_rider`, `found_red_cord`, `found_the_old_shaft`
     * and `evaded_the_riders` arrived with C1-2 and C1-3 and are already in saves made since.)
     */
    private val plannedOpeningFlags = setOf(
        "spared_the_rider", "robbed_the_rider", "rider_clue", "knows_riders_hired", "received_wax_disc",
        "parleyed_with_rider", "withdrew_from_the_riders", "fought_the_riders", "fell_through_the_door",
        "searched_the_wagon", "stripped_the_wagon", "left_the_wagon_be",
        "smith_vouched", "took_from_the_bench", "returned_the_draught", "owes_the_smith",
        "read_the_seal", "approached_by_camp", "read_voss_orders", "robbed_the_order", "parleyed_with_sentry",
        "asked_voss_name", "told_voss_of_the_riders", "sold_to_voss", "showed_voss_the_disc", "quoted_orders_to_voss"
    )

    private val stopAt = setOf("first_blood", "chapter1_end", "the_tended_dark", "word_at_stonebeard")

    private fun writeRaw(prefs: FakeSharedPreferences, scene: String, chapter: String = "chapter_1") {
        // Exactly the keys SaveStore wrote before this work, with deliberately non-default values.
        prefs.edit()
            .putString("2.chapter", chapter)
            .putString("2.scene", scene)
            .putInt("2.courage", 6)
            .putInt("2.honour", 3)
            .putInt("2.health", 77)
            .putInt("2.maxHealth", 118)
            .putInt("2.gold", 137)
            .putInt("2.level", 3)
            .putInt("2.xp", 55)
            .putInt("2.xpToNextLevel", 300)
            .putString("2.inventory", "dwarven_token,healing_draught,healing_draught")
            .putStringSet("2.trophies", setOf("Friend of Stonebeard", "First Blood"))
            .putStringSet("2.flags", setOf("kept_the_token", "helped_dwarf"))
            .putInt("active_slot", 2)
            .apply()
    }

    @Test
    fun `a save on every legacy Chapter 1 scene loads exactly and can be played onward`() {
        val scenes = listOf(
            "fallen_knight", "black_door_opened", "sword_drawn", "walked_the_road", "beneath_the_roots",
            "dwarven_path_kept", "dwarven_path_buried", "the_silent_forge", "stonebeard_hold",
            "smiths_warning_helped", "smiths_warning_confessed", "smiths_warning_moved_on",
            "first_blood", "first_blood_defeat", "chapter1_end"
        )
        scenes.forEach { scene ->
            val prefs = FakeSharedPreferences()
            writeRaw(prefs, scene)
            val loaded = SaveStore(prefs).load(2)!!
            assertEquals(scene, loaded.sceneId)
            assertEquals("stats survive: $scene", listOf(6, 3, 77, 118, 137, 3, 55, 300),
                listOf(loaded.courage, loaded.honour, loaded.health, loaded.maxHealth, loaded.gold, loaded.level, loaded.xp, loaded.xpToNextLevel))
            assertEquals(listOf("dwarven_token", "healing_draught", "healing_draught"), loaded.inventory)
            assertEquals(setOf("Friend of Stonebeard", "First Blood"), loaded.trophies)
            assertEquals(setOf("kept_the_token", "helped_dwarf"), loaded.flags)
            assertEquals("scene resolves to a real node: $scene", scene, StoryRepository.node(loaded.sceneId).id)
            if (scene !in stopAt) {
                val ends = Chapter1Paths.enumerate(loaded, stopAt)
                assertTrue("$scene has at least one way forward", ends.isNotEmpty())
                ends.forEach { assertTrue("$scene leads on to ${it.end.sceneId}", it.end.sceneId in stopAt) }
            }
        }
    }

    @Test
    fun `a save with only a scene key loads with the same defaults a new game uses`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("1.scene", "stonebeard_hold").apply()
        val loaded = SaveStore(prefs).load(1)!!
        assertEquals(GameState(activeSlot = 1, sceneId = "stonebeard_hold"), loaded)
    }

    @Test
    fun `an old save without any opening-route flags still reaches the first fight from the opening`() {
        val prefs = FakeSharedPreferences()
        writeRaw(prefs, "fallen_knight")
        val loaded = SaveStore(prefs).load(2)!!
        assertTrue(loaded.flags.intersect(plannedOpeningFlags).isEmpty())
        val ends = Chapter1Paths.enumerate(loaded, setOf("first_blood"))
        ends.forEach { assertEquals("first_blood", it.end.sceneId) }
        // This fixture has more Honour than a new game, so it may see MORE options (e.g. the confession
        // needs Honour 1). It must never see fewer: every path a new game has is still playable.
        val fresh = Chapter1Paths.enumerate(GameState(), setOf("first_blood")).map { Chapter1Paths.routeKey(it.labels) }.toSet()
        assertTrue(ends.map { Chapter1Paths.routeKey(it.labels) }.toSet().containsAll(fresh))
        assertEquals(364, fresh.size)
    }

    @Test
    fun `opening effects on Courage, Honour and items do not depend on new-game starting values`() {
        // Same choices from a new game and from a rich, wounded-free, mid-game state must change the
        // same things by the same amounts (full health, so passive regeneration cannot differ).
        val rich = GameState(
            courage = 11, honour = 7, health = 118, maxHealth = 118, gold = 240, level = 3, xp = 50,
            xpToNextLevel = 300, inventory = listOf("healing_draught"),
            trophies = setOf("First Blood"), flags = setOf("some_later_flag")
        )
        val fresh = GameState()
        val prefixes = arrayOf("OPEN THE DOOR", "", "", "Keep the token", "", "Call out", "Help him", "Take the buckler")
        val a = Chapter1Paths.follow(fresh, *prefixes)
        val b = Chapter1Paths.follow(rich, *prefixes)
        assertEquals(a.courage - fresh.courage, b.courage - rich.courage)
        assertEquals(a.honour - fresh.honour, b.honour - rich.honour)
        assertEquals(a.gold - fresh.gold, b.gold - rich.gold)
        assertEquals(a.health - fresh.health, b.health - rich.health)
        assertEquals(a.inventory - fresh.inventory.toSet(), b.inventory.drop(rich.inventory.size))
        assertEquals(a.flags - fresh.flags, b.flags - rich.flags)
        assertTrue("existing progress is kept", "some_later_flag" in b.flags && "First Blood" in b.trophies)
    }

    @Test
    fun `flags this build has never heard of survive a save and load and do not change the story`() {
        // A save written by a future build (or a tester build) must not be damaged or confuse the story.
        val future = GameState(sceneId = "stonebeard_hold", flags = setOf("kept_the_token") + plannedOpeningFlags + "some_flag_from_nowhere")
        val store = SaveStore(FakeSharedPreferences())
        store.save(1, future)
        val loaded = store.load(1)!!
        assertEquals(future.flags, loaded.flags)
        val node = StoryRepository.node("stonebeard_hold")
        assertEquals(
            StoryRepository.visibleChoices(node, GameState(sceneId = "stonebeard_hold", flags = setOf("kept_the_token"))).map { it.label },
            StoryRepository.visibleChoices(node, loaded).map { it.label }
        )
    }

    @Test
    fun `a state without any batch flag always has a way on from every Chapter 1 to 3 scene that gates on one`() {
        // New rows are gated on new flags, which a pre-update save cannot carry. That is only safe if every
        // such scene still offers something to a player who has none of them, with no gold and no stats.
        val bare = GameState(gold = 0, courage = 0, honour = 0)
        val nodes = listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }
        val gated = nodes.filter { n ->
            n.combatEncounterId == null && n.choices.any {
                val r = it.requirements
                r != null && (r.requiredFlags + r.forbiddenFlags).intersect(plannedOpeningFlags).isNotEmpty()
            }
        }
        assertTrue("expected some gated scenes", gated.isNotEmpty())
        gated.forEach { n ->
            val shown = StoryRepository.visibleChoices(n, bare.copy(sceneId = n.id))
            assertTrue("${n.id}: a bare state has no way on", shown.any { it.requirements?.minStats.isNullOrEmpty() })
        }
    }

    @Test
    fun `the back-one-step checkpoint round trips and restoring it does not corrupt a Chapter 1 save`() {
        val store = SaveStore(FakeSharedPreferences())
        val before = GameState(sceneId = "fallen_knight", courage = 4, honour = 2, gold = 90, inventory = listOf("healing_draught"))
        val after = before.applyChoice(StoryRepository.node("fallen_knight").choices.first())
        store.save(1, after)
        store.savePreviousState(1, before)
        assertTrue(store.hasPreviousState(1))
        assertEquals(before.copy(activeSlot = 1), store.restorePreviousState(1))
        assertFalse(store.hasPreviousState(1))
        assertEquals(before.copy(activeSlot = 1), store.load(1))
    }
}
