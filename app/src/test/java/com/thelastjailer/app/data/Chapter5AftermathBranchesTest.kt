package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 5, after the fight with the Marshal's adept: the three aftermath choices each lead to a scene of
 * their own (a chase, an hour with Halvard, a search of the ground) before they all rejoin the missive.
 *
 * These tests pin three things. What did not move: every earlier scene, the three choices' labels, gates,
 * stat changes, item and flags, the fight's links and the way out to Chapter 6. What the new scenes do and
 * do not do: they set no flag and grant nothing, and the only mechanical effect in any of them is the 10
 * Health the steep route costs. And the safety of that cost after either outcome of the fight.
 *
 * Nothing here says anything about replays: a second telling still hands over a second copy of the missive,
 * exactly as it did before this change, and these tests neither rely on that nor hide it.
 */
class Chapter5AftermathBranchesTest {

    private val aftermath = "aftermath_of_betrayal"
    private val missive = "what_the_missive_says"
    private val missiveItem = "cinder_marshal_missive"
    private val chase = "the_chase_to_the_ford"
    private val caught = "the_ford_caught"
    private val lost = "the_ford_lost"
    private val withHalvard = "halvard_by_the_door"
    private val ground = "what_the_ground_says"

    private val newIds = listOf(chase, caught, lost, withHalvard, ground)
    private val originalIds = listOf(
        "the_marshals_envoy", "the_envoys_offer_alone", "the_envoys_offer_with_halvard",
        "the_envoys_mask_slips_refused", "the_envoys_mask_slips_heard", "the_envoys_mask_slips_confronted",
        "the_marshals_warning", "cinder_envoy_defeat", aftermath, missive, "chapter5_end"
    )

    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val everyChoice: List<Pair<String, Choice>> = allNodes.flatMap { n -> n.choices.map { n.id to it } }

    private fun node(id: String): StoryNode = StoryRepository.node(id)
    private fun words(n: StoryNode): Int = n.narrativeText.trim().split(Regex("\\s+")).size

    /** Kaelen as he reaches the aftermath: strong enough for every gate, with nothing in his pack yet. */
    private val fresh = ReplayKit.STRONG.copy(inventory = emptyList())

    // ---- what did not move ------------------------------------------------------------------------------

    @Test
    fun `every earlier scene is still there and the five new ones belong to chapter 5`() {
        val ids = StoryRepository.nodesInChapter("chapter_5").map { it.id }.toSet()
        assertTrue(ids.containsAll(originalIds))
        assertTrue(ids.containsAll(newIds))
        assertEquals("a scene id is used twice", allNodes.size, allNodes.map { it.id }.toSet().size)
        assertEquals("the_marshals_envoy", StoryRepository.chapter("chapter_5")!!.startNodeId)
    }

    private class Spec(
        val flag: String,
        val label: String,
        val next: String,
        val minStats: Map<StatType, Int>,
        val statDeltas: Map<StatType, Int>
    )

    @Test
    fun `the three aftermath choices keep their labels, gates, stats, item and flags, and only their target moved`() {
        val specs = listOf(
            Spec("chased_the_envoy", "\"Chase the envoy down before he reaches word of this to anyone.\"", chase,
                mapOf(StatType.COURAGE to 4), mapOf(StatType.COURAGE to 1)),
            Spec("stayed_with_halvard", "\"Get Halvard back down to the Hold first. The envoy can wait.\"", withHalvard,
                mapOf(StatType.HONOUR to 3), mapOf(StatType.HONOUR to 1)),
            Spec("searched_the_ground", "Let him go, and see what he left behind.", ground,
                emptyMap(), emptyMap())
        )
        val choices = node(aftermath).choices
        assertEquals(3, choices.size)
        specs.forEach { sp ->
            val c = choices.single { sp.flag in it.consequences.setFlags }
            val label = sp.flag
            assertEquals(label, sp.label, c.label)
            assertEquals(label, sp.next, c.nextNodeId)
            val req = c.requirements
            assertEquals(label, sp.minStats, req?.minStats ?: emptyMap<StatType, Int>())
            assertTrue(label, req == null || (req.requiredFlags.isEmpty() && req.forbiddenFlags.isEmpty()))
            assertEquals(label, sp.statDeltas, c.consequences.statDeltas)
            assertEquals(label, listOf(missiveItem), c.consequences.grantItemIds)
            assertEquals(label, setOf(sp.flag), c.consequences.setFlags)
            assertTrue(label, c.consequences.clearFlags.isEmpty())
            assertNull(label, c.consequences.unlockTrophy)
        }
    }

    @Test
    fun `the fight's links and the way out to Chapter 6 are unchanged`() {
        val encounter = CombatRepository.encounter("cinder_envoy_encounter")
        assertEquals(aftermath, encounter.victoryNodeId)
        assertEquals("cinder_envoy_defeat", encounter.defeatNodeId)
        assertEquals("the_marshals_warning", node("the_marshals_warning").id)
        assertEquals("cinder_envoy_encounter", node("the_marshals_warning").combatEncounterId)
        val getUp = node("cinder_envoy_defeat").choices.single()
        assertEquals(aftermath, getUp.nextNodeId)
        assertEquals(setOf("lost_to_cinder_adept"), getUp.consequences.setFlags)
        val reading = node(missive).choices.single()
        assertEquals("Bring this to Halvard.", reading.label)
        assertEquals("chapter5_end", reading.nextNodeId)
        assertEquals("the_choice_of_roads", node("chapter5_end").choices.single().nextNodeId)
    }

    // ---- the branches -----------------------------------------------------------------------------------

    /** Every scene between [entry] and the missive scene, following every choice. Fails on a fight or a dead end. */
    private fun branch(entry: String): Set<String> {
        val seen = LinkedHashSet<String>()
        val pending = ArrayList<String>().apply { add(entry) }
        while (pending.isNotEmpty()) {
            val id = pending.removeAt(pending.size - 1)
            if (id == missive || !seen.add(id)) continue
            val n = node(id)
            assertNull("$id is a fight", n.combatEncounterId)
            assertTrue("$id is a dead end", n.choices.isNotEmpty())
            n.choices.forEach { pending += it.nextNodeId }
        }
        return seen
    }

    @Test
    fun `each aftermath choice leads to its own scenes and every way through them reaches the missive`() {
        assertEquals(setOf(chase, caught, lost), branch(chase))
        assertEquals(setOf(withHalvard), branch(withHalvard))
        assertEquals(setOf(ground), branch(ground))
        listOf(caught, lost, withHalvard, ground).forEach { id ->
            assertEquals(id, listOf(missive), node(id).choices.map { it.nextNodeId })
        }
    }

    @Test
    fun `the chase is a real fork with a visible trade, a Health cost on the steep route only`() {
        val choices = node(chase).choices
        assertEquals(2, choices.size)
        assertTrue("a gate on the fork", choices.all { it.requirements == null })
        val pines = choices.single { it.nextNodeId == caught }
        val road = choices.single { it.nextNodeId == lost }
        assertEquals(
            "Cut through the pines. Steep and loose underfoot, but it reaches the crossing before he does.",
            pines.label
        )
        assertEquals(
            "Stay on the road and break the rearguard. Surer footing, but he will be across before you arrive.",
            road.label
        )
        assertEquals(mapOf(StatType.HEALTH to -10), pines.consequences.statDeltas)
        assertTrue(road.consequences.statDeltas.isEmpty())
        // Both routes are offered to the least qualified player who can reach the chase at all.
        val atTheGate = fresh.copy(courage = 4, honour = 0)
        assertEquals(2, StoryRepository.visibleChoices(node(chase), atTheGate).size)
    }

    @Test
    fun `the new scenes set no flag, grant nothing and cost nothing except the one Health loss`() {
        val costs = ArrayList<Pair<String, Map<StatType, Int>>>()
        newIds.forEach { id ->
            val n = node(id)
            assertNull("$id", n.combatEncounterId)
            n.choices.forEach { c ->
                val label = "$id: ${c.label}"
                assertTrue(label, c.consequences.setFlags.isEmpty())
                assertTrue(label, c.consequences.clearFlags.isEmpty())
                assertTrue(label, c.consequences.grantItemIds.isEmpty())
                assertNull(label, c.consequences.unlockTrophy)
                assertNull(label, c.requirements)
                if (c.consequences.statDeltas.isNotEmpty()) costs += id to c.consequences.statDeltas
            }
        }
        assertEquals(listOf(chase to mapOf(StatType.HEALTH to -10)), costs)
    }

    // ---- the missive ------------------------------------------------------------------------------------

    /** Every state in which a traversal first stands on the missive scene, following every choice from [from]. */
    private fun arrivals(from: GameState): List<GameState> {
        if (from.sceneId == missive) return listOf(from)
        return StoryRepository.visibleChoices(node(from.sceneId), from).flatMap { arrivals(from.applyChoice(it)) }
    }

    @Test
    fun `one traversal of any branch hands over the missive exactly once`() {
        val start = ReplayKit.jumpTo(fresh, aftermath)
        assertEquals(0, start.inventory.count { it == missiveItem })
        val landed = arrivals(start)
        assertTrue("fewer routes than branches: ${landed.size}", landed.size >= 4) // pines, road, Halvard, ground
        landed.forEachIndexed { i, s ->
            assertEquals("route ${i + 1}", 1, s.inventory.count { it == missiveItem })
            val end = s.applyChoice(node(missive).choices.single()).applyChoice(node("chapter5_end").choices.single())
            assertEquals("the_choice_of_roads", end.sceneId)
            assertEquals("route ${i + 1} after the chapter", 1, end.inventory.count { it == missiveItem })
        }
    }

    @Test
    fun `only the three aftermath choices grant the missive anywhere in the story`() {
        val granting = everyChoice.filter { (_, c) -> missiveItem in c.consequences.grantItemIds }
        assertEquals(3, granting.size)
        assertTrue(granting.all { it.first == aftermath })
    }

    // ---- Health safety ----------------------------------------------------------------------------------

    private fun floorAfterVictory(maxHealth: Int): Int = (maxHealth * 40 + 99) / 100

    @Test
    fun `the steep route cannot bring Health near zero after either outcome of the fight`() {
        val encounter = CombatRepository.encounter("cinder_envoy_encounter")
        listOf(100, 109, 118, 127, 200).forEach { maxHealth ->
            val atTheFight = GameState(
                activeSlot = 1, chapterId = "chapter_5", sceneId = "the_marshals_warning",
                courage = 4, honour = 0, health = maxHealth, maxHealth = maxHealth
            )
            // The worst possible fight: far more damage than Kaelen has, then the game's own recovery.
            val won = atTheFight.resolveCombat(encounter, CombatOutcome(victory = true, damageTaken = 10_000))
            val lostFight = atTheFight.resolveCombat(encounter, CombatOutcome(victory = false, damageTaken = 10_000))
            assertEquals(aftermath, won.sceneId)
            assertEquals("cinder_envoy_defeat", lostFight.sceneId)
            val afterDefeat = lostFight.applyChoice(node("cinder_envoy_defeat").choices.single())
            assertEquals(aftermath, afterDefeat.sceneId)

            mapOf("victory" to won, "defeat" to afterDefeat).forEach { (how, atAftermath) ->
                val label = "max $maxHealth, after $how"
                val chaseChoice = StoryRepository.visibleChoices(node(aftermath), atAftermath).single { it.nextNodeId == chase }
                val atTheFork = atAftermath.applyChoice(chaseChoice)
                val pines = node(chase).choices.single { it.nextNodeId == caught }
                val hurt = atTheFork.applyChoice(pines)
                assertEquals(label, caught, hurt.sceneId)
                assertTrue("$label: health ${hurt.health}", hurt.health >= 1)
                assertTrue("$label: health ${hurt.health} below the victory floor less the cost",
                    hurt.health >= floorAfterVictory(maxHealth) - 10)
                assertTrue("$label: the cost was not taken", hurt.health < atTheFork.health)
            }
        }
    }

    // ---- art and text -----------------------------------------------------------------------------------

    @Test
    fun `the new scenes reuse art that exists`() {
        newIds.forEach { id ->
            assertNotNull("$id uses ${node(id).illustrationId}", IllustrationCatalog.get(node(id).illustrationId))
        }
    }

    @Test
    fun `the new scenes stay inside their word budgets and the chapter stays within its word tolerance`() {
        val caps = mapOf(chase to 140, caught to 205, lost to 130, withHalvard to 215, ground to 190)
        caps.forEach { (id, cap) -> assertTrue("$id has ${words(node(id))} words, cap $cap", words(node(id)) <= cap) }
        val newTotal = newIds.sumOf { words(node(it)) }
        assertTrue("new scenes total $newTotal words", newTotal <= 840)
        // The agreed target for the finished chapter is 3,000 authored words; up to 3,200 is tolerated.
        val chapterTotal = StoryRepository.nodesInChapter("chapter_5").sumOf { words(it) }
        assertTrue("Chapter 5 totals $chapterTotal words, tolerance 3,200 (target 3,000)", chapterTotal <= 3_200)
    }

    @Test
    fun `the new text follows the prose rules and leaves later chapters their secrets`() {
        val reserved = Regex(
            "\\b(lad|settings|seventh door|frame|ashwell|archivist|selvane|emberlow|fenmoor|duskmere|ordrun)\\b",
            RegexOption.IGNORE_CASE
        )
        val gendered = Regex("\\b(he|him|his|she|her|hers)\\b", RegexOption.IGNORE_CASE)
        newIds.forEach { id ->
            val n = node(id)
            val text = (listOf(n.title, n.narrativeText) + n.choices.map { it.label }).joinToString("\n")
            val flat = text.replace(Regex("\\s+"), " ")
            assertFalse("$id has a dash used as punctuation", text.any { it == '—' || it == '–' })
            assertFalse("$id has a sentence fragment starting 'Not'", Regex("(^|[.!?\"] )Not ").containsMatchIn(flat))
            assertNull("$id uses a reserved word: ${reserved.find(flat)?.value}", reserved.find(flat))
            // The Marshal stays ungendered here: no sentence naming the Marshal carries a personal pronoun.
            flat.split(Regex("(?<=[.!?]\"?) ")).filter { "Marshal" in it }.forEach { sentence ->
                assertFalse("$id: '$sentence'", gendered.containsMatchIn(sentence))
            }
        }
    }

    // ---- end to end -------------------------------------------------------------------------------------

    /** A [ReplayKit.play] picker that takes the aftermath choice setting [flag] and, at the chase, the route to [fork]. */
    private fun picker(flag: String, fork: String?): (StoryNode, List<Choice>) -> Choice? = { n, visible ->
        when (n.id) {
            aftermath -> visible.firstOrNull { flag in it.consequences.setFlags }
            chase -> fork?.let { target -> visible.firstOrNull { it.nextNodeId == target } }
            else -> null
        }
    }

    @Test
    fun `end to end - each branch carries the real chapter through to Chapter 6 with the missive once`() {
        val routes = listOf(
            Triple("chase by the pines", "chased_the_envoy", caught),
            Triple("chase by the road", "chased_the_envoy", lost),
            Triple("Halvard", "stayed_with_halvard", null),
            Triple("ground", "searched_the_ground", null)
        )
        routes.forEach { (name, flag, fork) ->
            val start = ReplayKit.jumpTo(fresh.copy(flags = setOf("accepted_dark_aid")), "the_marshals_envoy")
            val end = ReplayKit.play(start, stop = { it.sceneId == "the_choice_of_roads" }, pick = picker(flag, fork))
            assertEquals(name, 1, end.inventory.count { it == missiveItem })
            assertTrue(name, flag in end.flags)
            assertTrue("$name: health ${end.health}", end.health >= 1)
            // The Chapter 7 dependencies still arrive: exactly one answer to the envoy is held.
            val answers = setOf("refused_envoy", "heard_envoy_offer", "confronted_envoy_with_prisoners_words")
            assertEquals(name, 1, answers.intersect(end.flags).size)
        }
    }
}
