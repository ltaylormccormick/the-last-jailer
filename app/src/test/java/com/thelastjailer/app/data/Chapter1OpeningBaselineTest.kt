package com.thelastjailer.app.data

import com.thelastjailer.app.GameState
import com.thelastjailer.app.resolveCombat
import com.thelastjailer.app.CombatOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1-0 baseline. Pins exactly how Chapter 1 behaves on `main` BEFORE the opening is rebuilt, so every
 * later Chapter 1 batch can show precisely what it changed.
 *
 * These are characterization tests, not statements that the current behaviour is ideal. When a later
 * batch changes behaviour on purpose, it must update the matching entry here in the same commit and
 * say so in its report; an unexplained failure here means something changed that nobody meant to change.
 *
 * Findings (all verified by the golden table below):
 *  - The opening costs and pays NO gold, health or XP on any path: every path reaches the first fight
 *    with 25 gold, full health and 0 XP. Only Courage, Honour, items, trophies and flags differ.
 *  - There are 53 selectable paths from `fallen_knight` to the first fight (`first_blood`).
 *
 * Path keys name the distinguishing choices in order: opening, token, forge, Stonebeard, final.
 * Choices that are the only option at their node ("Descend into the dark.") are left out.
 */
class Chapter1OpeningBaselineTest {

    private val paths = Chapter1Paths.enumerate(GameState(), setOf("first_blood"))

    @Test
    fun `there are exactly 53 selectable paths from the opening to the first fight`() {
        assertEquals(53, paths.size)
        assertEquals("every path has a distinct key", paths.size, paths.map { Chapter1Paths.routeKey(it.labels) }.toSet().size)
    }

    @Test
    fun `every path ends at the first fight with the exact baseline state`() {
        val actual = paths.associate { Chapter1Paths.routeKey(it.labels) to Chapter1Paths.summary(it.end) }
        assertEquals("same set of paths", GOLDEN.keys, actual.keys)
        GOLDEN.forEach { (key, expected) -> assertEquals("path $key", expected, actual.getValue(key)) }
    }

    @Test
    fun `the opening never changes gold, health, xp or level on any path`() {
        paths.forEach { p ->
            val key = Chapter1Paths.routeKey(p.labels)
            assertEquals("gold $key", 25, p.end.gold)
            assertEquals("health $key", 100, p.end.health)
            assertEquals("xp $key", 0, p.end.xp)
            assertEquals("level $key", 1, p.end.level)
        }
    }

    @Test
    fun `paths per opening choice and the Courage and Honour each route can reach`() {
        fun route(prefix: String) = paths.filter { it.labels.first().startsWith(prefix) }
        val door = route("OPEN THE DOOR")
        val sword = route("DRAW YOUR SWORD")
        val walk = route("WALK AWAY")
        assertEquals(listOf(15, 22, 16), listOf(door.size, sword.size, walk.size))
        fun range(ps: List<Chapter1Paths.Path>, f: (GameState) -> Int) = ps.minOf { f(it.end) }..ps.maxOf { f(it.end) }
        assertEquals(3..5, range(door) { it.courage })
        assertEquals(0..3, range(door) { it.honour })
        assertEquals(2..4, range(sword) { it.courage })
        assertEquals(0..3, range(sword) { it.honour })
        assertEquals(1..3, range(walk) { it.courage })
        assertEquals(1..4, range(walk) { it.honour })
    }

    @Test
    fun `the Forge sneak option exists only for a player who drew the sword`() {
        // Pins the one place the opening gates a later choice: `drew_sword` unlocks "Move without a sound".
        fun forgeLabels(first: String) =
            Chapter1Paths.follow(GameState(), first, "", "Keep").let { s ->
                val atForge = Chapter1Paths.pick(s, "Follow the tunnel")
                StoryRepository.visibleChoices(StoryRepository.node(atForge.sceneId), atForge).map { it.label }
            }
        assertTrue(forgeLabels("DRAW YOUR SWORD").any { it.startsWith("Move without a sound") })
        assertTrue(forgeLabels("OPEN THE DOOR").none { it.startsWith("Move without a sound") })
        assertTrue(forgeLabels("WALK AWAY").none { it.startsWith("Move without a sound") })
    }

    @Test
    fun `the first fight is the same on every path - cave lurker, 40 xp, 10 gold, First Blood`() {
        val encounter = CombatRepository.encounter("first_blood_encounter")
        assertEquals("cave_lurker", encounter.enemyId)
        assertEquals("chapter1_end", encounter.victoryNodeId)
        assertEquals("first_blood_defeat", encounter.defeatNodeId)
        assertEquals(40, encounter.xpReward)
        assertEquals(10, encounter.goldReward)
        assertEquals("First Blood", encounter.unlockTrophy)
        val lurker = EnemyCatalog.get("cave_lurker")
        assertEquals(listOf(40, 4, 9), listOf(lurker.maxHealth, lurker.minAttack, lurker.maxAttack))

        val before = paths.first { Chapter1Paths.routeKey(it.labels) == "door.keep.call.help.buckler" }.end
        val won = before.resolveCombat(encounter, CombatOutcome(victory = true, damageTaken = 30))
        assertEquals("chapter1_end", won.sceneId)
        assertEquals(before.xp + 40, won.xp)
        assertEquals(before.gold + 10, won.gold)
        assertTrue("First Blood" in won.trophies)
        assertTrue("victory never leaves the player below 40% health", won.health >= 40)

        val lost = before.resolveCombat(encounter, CombatOutcome(victory = false, damageTaken = 500))
        assertEquals("first_blood_defeat", lost.sceneId)
        assertEquals("defeat gives no xp", before.xp, lost.xp)
        assertEquals("defeat gives no gold", before.gold, lost.gold)
        assertTrue("First Blood" !in lost.trophies)
        assertTrue("defeat stabilises at 45% health", lost.health >= 45)
    }

    private companion object {
        val GOLDEN: Map<String, String> = mapOf(
        "door.keep.call.help.buckler" to "C5 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,helped_dwarf,kept_the_token,opened_black_door,took_buckler",
        "door.keep.call.help.heed" to "C5 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,helped_dwarf,kept_the_token,opened_black_door",
        "door.keep.call.moveon.heed" to "C5 H0 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=announced_himself,kept_the_token,opened_black_door",
        "door.keep.wait.help.buckler" to "C4 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=helped_dwarf,kept_the_token,listened_first,opened_black_door,took_buckler",
        "door.keep.wait.help.heed" to "C4 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=helped_dwarf,kept_the_token,listened_first,opened_black_door",
        "door.keep.wait.confess.heed" to "C4 H2 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=confessed_to_dwarf,kept_the_token,listened_first,opened_black_door",
        "door.keep.wait.moveon.heed" to "C4 H1 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=kept_the_token,listened_first,opened_black_door",
        "door.bury.call.help.buckler" to "C4 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,helped_dwarf,opened_black_door,took_buckler",
        "door.bury.call.help.heed" to "C4 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,helped_dwarf,opened_black_door",
        "door.bury.call.confess.heed" to "C4 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,confessed_to_dwarf,opened_black_door",
        "door.bury.call.moveon.heed" to "C4 H1 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,opened_black_door",
        "door.bury.wait.help.buckler" to "C3 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=buried_the_past,helped_dwarf,listened_first,opened_black_door,took_buckler",
        "door.bury.wait.help.heed" to "C3 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=buried_the_past,helped_dwarf,listened_first,opened_black_door",
        "door.bury.wait.confess.heed" to "C3 H3 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,confessed_to_dwarf,listened_first,opened_black_door",
        "door.bury.wait.moveon.heed" to "C3 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,listened_first,opened_black_door",
        "sword.keep.call.help.buckler" to "C4 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,drew_sword,helped_dwarf,kept_the_token,took_buckler",
        "sword.keep.call.help.heed" to "C4 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,drew_sword,helped_dwarf,kept_the_token",
        "sword.keep.call.moveon.heed" to "C4 H0 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=announced_himself,drew_sword,kept_the_token",
        "sword.keep.wait.help.buckler" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=drew_sword,helped_dwarf,kept_the_token,listened_first,took_buckler",
        "sword.keep.wait.help.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=drew_sword,helped_dwarf,kept_the_token,listened_first",
        "sword.keep.wait.confess.heed" to "C3 H2 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=confessed_to_dwarf,drew_sword,kept_the_token,listened_first",
        "sword.keep.wait.moveon.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=drew_sword,kept_the_token,listened_first",
        "sword.keep.creep.help.buckler" to "C4 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=crept_in,drew_sword,helped_dwarf,kept_the_token,took_buckler",
        "sword.keep.creep.help.heed" to "C4 H0 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=crept_in,drew_sword,helped_dwarf,kept_the_token",
        "sword.keep.creep.moveon.heed" to "C4 H0 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=crept_in,drew_sword,kept_the_token",
        "sword.bury.call.help.buckler" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,drew_sword,helped_dwarf,took_buckler",
        "sword.bury.call.help.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,drew_sword,helped_dwarf",
        "sword.bury.call.confess.heed" to "C3 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,confessed_to_dwarf,drew_sword",
        "sword.bury.call.moveon.heed" to "C3 H1 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,drew_sword",
        "sword.bury.wait.help.buckler" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=buried_the_past,drew_sword,helped_dwarf,listened_first,took_buckler",
        "sword.bury.wait.help.heed" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=buried_the_past,drew_sword,helped_dwarf,listened_first",
        "sword.bury.wait.confess.heed" to "C2 H3 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,confessed_to_dwarf,drew_sword,listened_first",
        "sword.bury.wait.moveon.heed" to "C2 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,drew_sword,listened_first",
        "sword.bury.creep.help.buckler" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=buried_the_past,crept_in,drew_sword,helped_dwarf,took_buckler",
        "sword.bury.creep.help.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=buried_the_past,crept_in,drew_sword,helped_dwarf",
        "sword.bury.creep.confess.heed" to "C3 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,confessed_to_dwarf,crept_in,drew_sword",
        "sword.bury.creep.moveon.heed" to "C3 H1 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,crept_in,drew_sword",
        "walk.keep.call.help.buckler" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,helped_dwarf,kept_the_token,took_buckler,walked_away",
        "walk.keep.call.help.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=announced_himself,helped_dwarf,kept_the_token,walked_away",
        "walk.keep.call.confess.heed" to "C3 H2 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=announced_himself,confessed_to_dwarf,kept_the_token,walked_away",
        "walk.keep.call.moveon.heed" to "C3 H1 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=announced_himself,kept_the_token,walked_away",
        "walk.keep.wait.help.buckler" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler,tarnished_guard_token | tro=Friend of Stonebeard | flags=helped_dwarf,kept_the_token,listened_first,took_buckler,walked_away",
        "walk.keep.wait.help.heed" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,tarnished_guard_token | tro=Friend of Stonebeard | flags=helped_dwarf,kept_the_token,listened_first,walked_away",
        "walk.keep.wait.confess.heed" to "C2 H3 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=confessed_to_dwarf,kept_the_token,listened_first,walked_away",
        "walk.keep.wait.moveon.heed" to "C2 H2 HP100 G25 XP0 L1 | inv=tarnished_guard_token | tro= | flags=kept_the_token,listened_first,walked_away",
        "walk.bury.call.help.buckler" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,helped_dwarf,took_buckler,walked_away",
        "walk.bury.call.help.heed" to "C2 H2 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=announced_himself,buried_the_past,helped_dwarf,walked_away",
        "walk.bury.call.confess.heed" to "C2 H3 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,confessed_to_dwarf,walked_away",
        "walk.bury.call.moveon.heed" to "C2 H2 HP100 G25 XP0 L1 | inv= | tro= | flags=announced_himself,buried_the_past,walked_away",
        "walk.bury.wait.help.buckler" to "C1 H3 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught,sturdy_buckler | tro=Friend of Stonebeard | flags=buried_the_past,helped_dwarf,listened_first,took_buckler,walked_away",
        "walk.bury.wait.help.heed" to "C1 H3 HP100 G25 XP0 L1 | inv=dwarven_token,healing_draught | tro=Friend of Stonebeard | flags=buried_the_past,helped_dwarf,listened_first,walked_away",
        "walk.bury.wait.confess.heed" to "C1 H4 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,confessed_to_dwarf,listened_first,walked_away",
        "walk.bury.wait.moveon.heed" to "C1 H3 HP100 G25 XP0 L1 | inv= | tro= | flags=buried_the_past,listened_first,walked_away",
        )
    }
}
