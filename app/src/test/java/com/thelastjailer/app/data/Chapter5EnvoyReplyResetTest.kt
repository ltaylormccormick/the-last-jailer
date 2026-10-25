package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.applyChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay integrity for Chapter 5's answer to the Marshal's envoy. Refusing him, hearing him out and
 * confronting him with the prisoner's words each clear their two siblings, so a replay that answers
 * differently never carries last telling's answer into this one. Chapter VII reads all three flags as
 * dialogue options with Voss, and the Journal groups them as one moment, so a stale flag would show up as
 * extra options and a contradicting Journal line.
 *
 * The family is spelled out in this file rather than read from [ENVOY_REPLY_FAMILY], so an emptied or wrong
 * constant fails these tests instead of passing them. Nothing here depends on how many scenes lead to the
 * answer, so later Chapter 5 batches can add more answer scenes without rewriting it.
 */
class Chapter5EnvoyReplyResetTest {

    private val refused = "refused_envoy"
    private val heard = "heard_envoy_offer"
    private val confronted = "confronted_envoy_with_prisoners_words"
    private val family = setOf(refused, heard, confronted)

    private val offerNodes = listOf("the_envoys_offer_alone", "the_envoys_offer_with_halvard")
    private val maskSlips = setOf(
        "the_envoys_mask_slips_refused", "the_envoys_mask_slips_heard", "the_envoys_mask_slips_confronted"
    )

    /** Chapter VII's dialogue option for each answer, in both of Voss's scenes that offer it. */
    private val chapter7Scenes = listOf("what_voss_knows_pressed", "what_voss_knows_welcomed")
    private val chapter7Option = mapOf(
        refused to "the_price_of_trust_accountable",
        heard to "the_price_of_trust_wary",
        confronted to "the_price_of_trust_unsurprised"
    )

    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val everyChoice: List<Pair<String, Choice>> = allNodes.flatMap { n -> n.choices.map { n.id to it } }

    /** Kaelen as he reaches the envoy having taken the dark aid, so every answer is on offer. */
    private val aided = ReplayKit.STRONG.copy(flags = setOf("accepted_dark_aid"))

    private fun answer(nodeId: String, flag: String): Choice =
        StoryRepository.node(nodeId).choices.single { flag in it.consequences.setFlags }

    private fun heldFamily(state: GameState): Set<String> = state.flags.intersect(family)

    // ---- the data ---------------------------------------------------------------------------------------

    @Test
    fun `the family is exactly the three answer flags`() {
        assertEquals(family, ENVOY_REPLY_FAMILY)
    }

    @Test
    fun `all six answers clear the family and set only their own flag`() {
        offerNodes.forEach { nodeId ->
            val choices = StoryRepository.node(nodeId).choices
            assertEquals(nodeId, 3, choices.size)
            choices.forEach { c ->
                assertEquals("$nodeId: ${c.label}", family, c.consequences.clearFlags)
                assertEquals("$nodeId: ${c.label}", 1, c.consequences.setFlags.size)
                assertTrue("$nodeId: ${c.label}", c.consequences.setFlags.single() in family)
            }
            assertEquals(nodeId, family, choices.flatMap { it.consequences.setFlags }.toSet())
        }
    }

    @Test
    fun `every choice in the story that sets an answer flag clears the whole family, and none clears one without setting one`() {
        val setters = everyChoice.filter { (_, c) -> c.consequences.setFlags.any { it in family } }
        val clearers = everyChoice.filter { (_, c) -> c.consequences.clearFlags.any { it in family } }
        assertTrue(setters.map { it.first }.containsAll(offerNodes))
        setters.forEach { (id, c) -> assertEquals("$id: ${c.label}", family, c.consequences.clearFlags) }
        assertEquals(
            setters.map { it.first to it.second.label }.toSet(),
            clearers.map { it.first to it.second.label }.toSet()
        )
    }

    private class Spec(
        val flag: String,
        val label: String,
        val next: String,
        val requiredFlags: Set<String>,
        val statDeltas: Map<StatType, Int>
    )

    @Test
    fun `the six answers keep their labels, targets, requirements, stat changes and items`() {
        val specs = listOf(
            Spec(refused, "\"Whatever the Marshal wants with the brand, the answer is no.\"",
                "the_envoys_mask_slips_refused", emptySet(), mapOf(StatType.HONOUR to 1)),
            Spec(heard, "\"Keep talking. I'm listening.\"",
                "the_envoys_mask_slips_heard", emptySet(), emptyMap()),
            Spec(confronted,
                "\"The thing behind that gate already told me what it wants. Did the Marshal send you to finish what it started?\"",
                "the_envoys_mask_slips_confronted", setOf("accepted_dark_aid"), mapOf(StatType.COURAGE to 1))
        )
        offerNodes.forEach { nodeId ->
            specs.forEach { sp ->
                val c = answer(nodeId, sp.flag)
                val label = "$nodeId: ${sp.flag}"
                assertEquals(label, sp.label, c.label)
                assertEquals(label, sp.next, c.nextNodeId)
                val req = c.requirements
                assertEquals(label, sp.requiredFlags, req?.requiredFlags ?: emptySet<String>())
                assertTrue(label, req == null || (req.forbiddenFlags.isEmpty() && req.minStats.isEmpty()))
                assertEquals(label, sp.statDeltas, c.consequences.statDeltas)
                assertTrue(label, c.consequences.grantItemIds.isEmpty())
                assertNull(label, c.consequences.unlockTrophy)
            }
        }
    }

    @Test
    fun `every route into a mask-slips scene is an answer that clears the family`() {
        val into = everyChoice.filter { (_, c) -> c.nextNodeId in maskSlips }
        assertTrue("no choice leads to the mask-slips scenes", into.isNotEmpty())
        into.forEach { (id, c) -> assertEquals("$id: ${c.label}", family, c.consequences.clearFlags) }
        val fightExits = allNodes.mapNotNull { it.combatEncounterId }.distinct().flatMap { encounterId ->
            val e = CombatRepository.encounter(encounterId)
            listOfNotNull(e.victoryNodeId, e.defeatNodeId)
        }
        assertTrue("a fight leads straight to a mask-slips scene", fightExits.none { it in maskSlips })
    }

    // ---- the behaviour ----------------------------------------------------------------------------------

    @Test
    fun `a first play holds exactly the answer given`() {
        offerNodes.forEach { nodeId ->
            family.forEach { flag ->
                val s = ReplayKit.jumpTo(aided, nodeId).applyChoice(answer(nodeId, flag))
                assertEquals("$nodeId $flag", setOf(flag), heldFamily(s))
            }
        }
    }

    @Test
    fun `a replay that answers differently never carries the earlier answer`() {
        offerNodes.forEach { firstNode ->
            offerNodes.forEach { secondNode ->
                family.forEach { first ->
                    family.forEach { second ->
                        val label = "$firstNode $first, then $secondNode $second"
                        val firstTelling = ReplayKit.jumpTo(aided, firstNode).applyChoice(answer(firstNode, first))
                        assertEquals(label, setOf(first), heldFamily(firstTelling))
                        // The replay: back at the envoy with every flag, stat, item and trophy kept.
                        val secondTelling = ReplayKit.jumpTo(firstTelling, secondNode).applyChoice(answer(secondNode, second))
                        assertEquals(label, setOf(second), heldFamily(secondTelling))
                    }
                }
            }
        }
    }

    @Test
    fun `answering again keeps earned progression and unrelated discoveries`() {
        val unrelated = setOf(
            "accepted_dark_aid", "halvard_died", "kept_the_token", "refused_the_prisoner", "lost_to_cinder_adept",
            "chased_the_envoy", "met_envoy_alone", "brought_halvard", "vowed_to_return_prepared"
        )
        val earned = GameState(
            activeSlot = 1, courage = 7, honour = 6, health = 100, maxHealth = 100, gold = 88, level = 3, xp = 40,
            xpToNextLevel = 300,
            inventory = listOf("ashen_signet", "cinder_marshal_missive", "healing_draught", "healing_draught"),
            trophies = setOf("The Marshal's Warning", "Line Held"),
            flags = unrelated + heard
        )
        val node = offerNodes[0]
        val refusedAgain = ReplayKit.jumpTo(earned, node).applyChoice(answer(node, refused))
        assertEquals(unrelated + refused, refusedAgain.flags)
        assertEquals(7, refusedAgain.courage)
        assertEquals(7, refusedAgain.honour) // the refusal's own +1 Honour, and nothing taken away
        assertEquals(88, refusedAgain.gold)
        assertEquals(3, refusedAgain.level)
        assertEquals(40, refusedAgain.xp)
        assertEquals(earned.inventory, refusedAgain.inventory)
        assertEquals(earned.trophies, refusedAgain.trophies)

        val confrontedAgain = ReplayKit.jumpTo(refusedAgain, node).applyChoice(answer(node, confronted))
        assertEquals(unrelated + confronted, confrontedAgain.flags)
        assertEquals(8, confrontedAgain.courage) // the confrontation's own +1 Courage
        assertEquals(7, confrontedAgain.honour)
        assertEquals(earned.inventory, confrontedAgain.inventory)
        assertEquals(earned.trophies, confrontedAgain.trophies)
    }

    @Test
    fun `Chapter 7 offers only the current answer's option after any three tellings`() {
        val orders = family.flatMap { a -> family.flatMap { b -> family.map { c -> listOf(a, b, c) } } }
        assertEquals(27, orders.size)
        orders.forEach { tellings ->
            var s = aided
            tellings.forEachIndexed { i, flag ->
                val node = offerNodes[i % 2] // alternate the alone and with-Halvard approaches
                s = ReplayKit.jumpTo(s, node).applyChoice(answer(node, flag))
            }
            val current = tellings.last()
            chapter7Scenes.forEach { sceneId ->
                val offered = StoryRepository.visibleChoices(StoryRepository.node(sceneId), s)
                    .map { it.nextNodeId }.filter { it in chapter7Option.values }
                assertEquals("$tellings at $sceneId", listOf(chapter7Option.getValue(current)), offered)
            }
        }
    }

    @Test
    fun `the Journal shows the current answer alone after a replay`() {
        val lines = mapOf(
            refused to "I told the envoy the Marshal's answer was no.",
            heard to "I heard the envoy out, though I promised nothing.",
            confronted to "I told the envoy the prisoner had already said what the Marshal wants."
        )
        family.forEach { first ->
            family.forEach { second ->
                var s = ReplayKit.jumpTo(aided, offerNodes[0]).applyChoice(answer(offerNodes[0], first))
                s = ReplayKit.jumpTo(s, offerNodes[1]).applyChoice(answer(offerNodes[1], second))
                val envoyLines = FlagDescriptions.sections(s.flags).flatMap { it.lines }.filter { "envoy" in it }
                assertEquals("$first, then $second", listOf(lines.getValue(second)), envoyLines)
            }
        }
    }

    @Test
    fun `end to end - two tellings through the real chapter leave only the second answer`() {
        fun throughChapter5(from: GameState, answerFlag: String, approachFlag: String): GameState = ReplayKit.play(
            ReplayKit.jumpTo(from, "the_marshals_envoy"),
            stop = { it.sceneId == "the_choice_of_roads" },
            pick = { node, visible ->
                when {
                    node.id == "the_marshals_envoy" -> visible.firstOrNull { approachFlag in it.consequences.setFlags }
                    node.id in offerNodes -> visible.firstOrNull { answerFlag in it.consequences.setFlags }
                    else -> null
                }
            }
        )
        val first = throughChapter5(aided, refused, "met_envoy_alone")
        assertEquals(setOf(refused), heldFamily(first))
        val second = throughChapter5(first, confronted, "brought_halvard")
        assertEquals(setOf(confronted), heldFamily(second))
        val third = throughChapter5(second, heard, "met_envoy_alone")
        assertEquals(setOf(heard), heldFamily(third))
        assertEquals("the_choice_of_roads", third.sceneId)
        // The chapter still hands over the missive and the progression it always did.
        assertTrue("cinder_marshal_missive" in third.inventory)
        assertTrue("accepted_dark_aid" in third.flags)
    }
}
