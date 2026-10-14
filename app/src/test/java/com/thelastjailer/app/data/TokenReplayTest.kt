package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Token replay follow-up: "Keep the token." and "Leave the past buried." clear each other's flag, so a
 * later telling never holds both decisions. Flags only: no inventory removal, no save-format change,
 * no trophy change. A clasp carried over from an earlier telling stays in the inventory (documented
 * replay limitation in docs/CONSEQUENCE_LEDGER.md).
 */
class TokenReplayTest {

    private val keptLine = "I kept the tarnished King's Guard clasp I found in the roots."
    private val buriedLine = "I left a tarnished King's Guard clasp where I found it, in the roots."

    private fun node(id: String) = StoryRepository.node(id)
    private fun pick(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun labels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }
    private fun atRoots(flags: Set<String> = emptySet(), inventory: List<String> = emptyList(), trophies: Set<String> = emptySet()) =
        GameState(chapterId = "chapter_1", sceneId = "beneath_the_roots", flags = flags, inventory = inventory, trophies = trophies)
    private fun atHalvard(from: GameState, scene: String) = from.copy(chapterId = "chapter_2", sceneId = scene)
    private fun halvardOffersToken(s: GameState) =
        listOf("the_cloaks_truth_asked", "the_cloaks_truth_offered").all { id ->
            labels(atHalvard(s, id)).any { it.startsWith("Show him the tarnished token") }
        }
    private fun halvardHidesToken(s: GameState) =
        listOf("the_cloaks_truth_asked", "the_cloaks_truth_offered").all { id ->
            labels(atHalvard(s, id)).none { it.startsWith("Show him the tarnished token") }
        }
    private fun journal(s: GameState) = FlagDescriptions.sections(s.flags).flatMap { it.lines }

    @Test
    fun `the two decisions clear each other and set only their own flag`() {
        val keep = node("beneath_the_roots").choices.first { it.label.startsWith("Keep the token") }.consequences
        val bury = node("beneath_the_roots").choices.first { it.label.startsWith("Leave the past buried") }.consequences
        assertEquals(setOf("buried_the_past"), keep.clearFlags)
        assertEquals(setOf("kept_the_token"), bury.clearFlags)
        assertEquals(setOf("kept_the_token"), keep.setFlags)
        assertEquals(setOf("buried_the_past"), bury.setFlags)
        // Unchanged: item grant, stats, no trophy either way.
        assertEquals(listOf("tarnished_guard_token"), keep.grantItemIds)
        assertTrue(bury.grantItemIds.isEmpty())
        assertEquals(null, keep.unlockTrophy)
        assertEquals(null, bury.unlockTrophy)
    }

    @Test
    fun `burying after keeping leaves only the buried decision`() {
        val replay = atRoots(flags = setOf("kept_the_token"), inventory = listOf("tarnished_guard_token"))
        val buried = pick(replay, "Leave the past buried")
        assertTrue("buried_the_past" in buried.flags)
        assertFalse("kept_the_token" in buried.flags)
    }

    @Test
    fun `keeping after burying leaves only the kept decision`() {
        val replay = atRoots(flags = setOf("buried_the_past"))
        val kept = pick(replay, "Keep the token")
        assertTrue("kept_the_token" in kept.flags)
        assertFalse("buried_the_past" in kept.flags)
    }

    @Test
    fun `alternating across several tellings never holds both`() {
        var s = atRoots()
        listOf("Keep", "Leave", "Leave", "Keep", "Keep", "Leave", "Keep").forEach { p ->
            s = pick(s.copy(sceneId = "beneath_the_roots", chapterId = "chapter_1"), p)
            assertFalse(p, "kept_the_token" in s.flags && "buried_the_past" in s.flags)
            assertEquals(p, p == "Keep", "kept_the_token" in s.flags)
            assertEquals(p, p == "Leave", "buried_the_past" in s.flags)
        }
    }

    @Test
    fun `a carried-over token stays in the inventory and nothing else about the inventory changes`() {
        val inv = listOf("dwarven_token", "tarnished_guard_token", "healing_draught")
        val buried = pick(atRoots(flags = setOf("kept_the_token"), inventory = inv), "Leave the past buried")
        assertEquals(inv, buried.inventory)
        val untouched = pick(atRoots(inventory = listOf("healing_draught")), "Leave the past buried")
        assertEquals(listOf("healing_draught"), untouched.inventory)
    }

    @Test
    fun `trophies are untouched by either decision`() {
        val trophies = setOf("Blood and Silver")
        assertEquals(trophies, pick(atRoots(flags = setOf("kept_the_token"), trophies = trophies), "Leave the past buried").trophies)
        assertEquals(trophies, pick(atRoots(flags = setOf("buried_the_past"), trophies = trophies), "Keep the token").trophies)
    }

    @Test
    fun `Halvard offers the token only when the current telling kept it`() {
        val kept = pick(atRoots(), "Keep the token")
        assertTrue(halvardOffersToken(kept))
        val buriedFirst = pick(atRoots(), "Leave the past buried")
        assertTrue(halvardHidesToken(buriedFirst))
        // A later telling that buries after keeping, with the token still carried, no longer sees the row.
        val replayBuried = pick(atRoots(flags = setOf("kept_the_token"), inventory = listOf("tarnished_guard_token")), "Leave the past buried")
        assertTrue("tarnished_guard_token" in replayBuried.inventory)
        assertTrue(halvardHidesToken(replayBuried))
        // And keeping after burying brings it back.
        assertTrue(halvardOffersToken(pick(atRoots(flags = setOf("buried_the_past")), "Keep the token")))
    }

    @Test
    fun `the Journal reports only the current telling's clasp decision`() {
        val replayBuried = pick(atRoots(flags = setOf("kept_the_token")), "Leave the past buried")
        assertTrue(journal(replayBuried).contains(buriedLine))
        assertFalse(journal(replayBuried).contains(keptLine))
        val replayKept = pick(atRoots(flags = setOf("buried_the_past")), "Keep the token")
        assertTrue(journal(replayKept).contains(keptLine))
        assertFalse(journal(replayKept).contains(buriedLine))
    }

    @Test
    fun `an old save holding both flags is not changed by loading it and still reads neutrally`() {
        val old = GameState(
            chapterId = "chapter_2", sceneId = "the_cloaks_truth_asked",
            flags = setOf("kept_the_token", "buried_the_past"), inventory = listOf("tarnished_guard_token")
        )
        val store = SaveStore(FakeSharedPreferences())
        store.save(1, old)
        val loaded = store.load(1)!!
        assertEquals(old.flags, loaded.flags)
        assertEquals(old.inventory, loaded.inventory)
        // Nothing runs on load, so the Journal still shows its single neutral clasp line rather than contradicting itself.
        val lines = journal(loaded)
        assertEquals(1, lines.count { it.contains("clasp") })
        assertFalse(lines.contains(keptLine) || lines.contains(buriedLine))
        // The old save only changes when the player makes the choice again.
        val rechosen = pick(loaded.copy(chapterId = "chapter_1", sceneId = "beneath_the_roots"), "Leave the past buried")
        assertEquals(true, "kept_the_token" !in rechosen.flags && "buried_the_past" in rechosen.flags)
    }

    @Test
    fun `a save parked after either decision keeps its flags when loaded`() {
        val store = SaveStore(FakeSharedPreferences())
        listOf(setOf("kept_the_token"), setOf("buried_the_past")).forEachIndexed { i, flags ->
            val s = GameState(activeSlot = i + 1, chapterId = "chapter_1", sceneId = "the_silent_forge", flags = flags)
            store.save(i + 1, s)
            assertEquals(flags, store.load(i + 1)!!.flags)
        }
    }

    @Test
    fun `the first telling is unaffected by either choice`() {
        val kept = pick(atRoots(), "Keep the token")
        assertEquals(setOf("kept_the_token"), kept.flags)
        assertEquals(listOf("tarnished_guard_token"), kept.inventory)
        assertEquals(2, kept.courage) // 1 + the choice's +1
    }
}
