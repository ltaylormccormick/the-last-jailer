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
 * Chapter 5, the optional press about the brand. Each of the two offer scenes (met alone, met with Halvard)
 * gains one ungated choice that leads to a scene of its own, where the envoy says a little more and the
 * morning moves on. That scene carries exact copies of the same three answers, so the player returns to the
 * answers they always had, with the same gates, stat changes, flags and replay reset.
 *
 * Pressing is narrative only. These tests pin that it changes no game state, that the answers are exact
 * copies, that the two scenes say different things, that the information is hedged as something the envoy
 * was told, and that nothing reaches the mask-slips scenes except through an answer that clears the family.
 */
class Chapter5ParleyPressTest {

    private val refused = "refused_envoy"
    private val heard = "heard_envoy_offer"
    private val confronted = "confronted_envoy_with_prisoners_words"
    private val family = setOf(refused, heard, confronted)

    private val offerAlone = "the_envoys_offer_alone"
    private val offerWith = "the_envoys_offer_with_halvard"
    private val explainAlone = "the_envoy_explains_alone"
    private val explainWith = "the_envoy_explains_with_halvard"
    private val pairs = listOf(offerAlone to explainAlone, offerWith to explainWith)
    private val approachFlags = listOf("met_envoy_alone", "brought_halvard")

    private val pressLabel = "\"Say what the Marshal actually wants with the brand.\""
    private val maskSlips = setOf(
        "the_envoys_mask_slips_refused", "the_envoys_mask_slips_heard", "the_envoys_mask_slips_confronted"
    )
    private val chapter7Scenes = listOf("what_voss_knows_pressed", "what_voss_knows_welcomed")
    private val chapter7Option = mapOf(
        refused to "the_price_of_trust_accountable",
        heard to "the_price_of_trust_wary",
        confronted to "the_price_of_trust_unsurprised"
    )
    private val journalLine = mapOf(
        refused to "I told the envoy the Marshal's answer was no.",
        heard to "I heard the envoy out, though I promised nothing.",
        confronted to "I told the envoy the prisoner had already said what the Marshal wants."
    )

    private val chapterNodes = StoryRepository.nodesInChapter("chapter_5")
    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val everyChoice: List<Pair<String, Choice>> = allNodes.flatMap { n -> n.choices.map { n.id to it } }
    private val aided = ReplayKit.STRONG.copy(flags = setOf("accepted_dark_aid"))

    private fun node(id: String): StoryNode = StoryRepository.node(id)
    private fun flat(s: String): String = s.replace(Regex("\\s+"), " ").trim()
    private fun words(n: StoryNode): Int = flat(n.narrativeText).split(" ").size
    private fun answer(nodeId: String, flag: String): Choice =
        node(nodeId).choices.single { flag in it.consequences.setFlags }
    private fun heldFamily(s: GameState): Set<String> = s.flags.intersect(family)

    // ---- the press choice on each offer scene -----------------------------------------------------------

    @Test
    fun `each offer scene gains one ungated press choice, last, and keeps its three answers first`() {
        pairs.forEach { (offer, explain) ->
            val choices = node(offer).choices
            assertEquals(offer, 4, choices.size)
            assertEquals(offer, listOf(refused, heard, confronted), choices.take(3).map { it.consequences.setFlags.single() })
            val press = choices.last()
            assertEquals(offer, pressLabel, press.label)
            assertEquals(offer, explain, press.nextNodeId)
            assertNull(offer, press.requirements)
            assertTrue(offer, press.consequences.statDeltas.isEmpty())
            assertTrue(offer, press.consequences.setFlags.isEmpty() && press.consequences.clearFlags.isEmpty())
            assertTrue(offer, press.consequences.grantItemIds.isEmpty())
            assertNull(offer, press.consequences.unlockTrophy)
            assertEquals(offer, 1, choices.count { it.label == pressLabel })
        }
    }

    @Test
    fun `pressing changes no game state, so the player returns to exactly the answers they always had`() {
        pairs.forEach { (offer, explain) ->
            family.forEach { flag ->
                val direct = ReplayKit.jumpTo(aided, offer).applyChoice(answer(offer, flag))
                val press = node(offer).choices.single { it.nextNodeId == explain }
                val atExplain = ReplayKit.jumpTo(aided, offer).applyChoice(press)
                assertEquals(explain, atExplain.sceneId)
                assertEquals("press alone changes nothing but the scene", aided.copy(sceneId = explain), atExplain.copy(chapterId = aided.chapterId))
                val viaPress = atExplain.applyChoice(answer(explain, flag))
                assertEquals("$offer, $flag: pressed and direct end in the same state", direct, viaPress)
            }
        }
    }

    // ---- the explain scenes -----------------------------------------------------------------------------

    @Test
    fun `each explain scene carries exact copies of the three answers, each clearing the whole family`() {
        pairs.forEach { (offer, explain) ->
            val copies = node(explain).choices
            assertEquals(explain, 3, copies.size)
            family.forEach { flag ->
                val original = answer(offer, flag)
                val copy = copies.single { flag in it.consequences.setFlags }
                assertEquals("$explain $flag", original, copy)
                assertEquals("$explain $flag", family, copy.consequences.clearFlags)
                assertEquals("$explain $flag", setOf(flag), copy.consequences.setFlags)
            }
            // The gate and the stat changes, spelled out rather than only compared.
            val confrontCopy = answer(explain, confronted)
            assertEquals(setOf("accepted_dark_aid"), confrontCopy.requirements?.requiredFlags)
            assertEquals(mapOf(StatType.COURAGE to 1), confrontCopy.consequences.statDeltas)
            assertEquals(mapOf(StatType.HONOUR to 1), answer(explain, refused).consequences.statDeltas)
            assertTrue(answer(explain, heard).consequences.statDeltas.isEmpty())
            assertNull(answer(explain, refused).requirements)
            assertNull(answer(explain, heard).requirements)
        }
    }

    @Test
    fun `each explain scene is reached only from its own offer, has no loop, and only the answers reach the mask-slips scenes`() {
        assertEquals(listOf(offerAlone), everyChoice.filter { it.second.nextNodeId == explainAlone }.map { it.first })
        assertEquals(listOf(offerWith), everyChoice.filter { it.second.nextNodeId == explainWith }.map { it.first })
        pairs.forEach { (_, explain) ->
            node(explain).choices.forEach { assertTrue("$explain: ${it.label}", it.nextNodeId in maskSlips) }
        }
        val into = everyChoice.filter { (_, c) -> c.nextNodeId in maskSlips }
        assertEquals(setOf(offerAlone, offerWith, explainAlone, explainWith), into.map { it.first }.toSet())
        into.forEach { (id, c) -> assertEquals("$id: ${c.label}", family, c.consequences.clearFlags) }
        pairs.forEach { (_, explain) ->
            val n = node(explain)
            assertEquals("chapter_5", n.chapterId)
            assertEquals("What the Marshal Asks", n.title)
            assertEquals("cinder_envoy", n.illustrationId)
            assertNotNull(IllustrationCatalog.get(n.illustrationId))
            assertNull(n.combatEncounterId)
        }
    }

    // ---- replay safety through the pressed paths --------------------------------------------------------

    private fun tell(from: GameState, approach: Int, pressed: Boolean, flag: String): GameState {
        val (offer, explain) = pairs[approach]
        val atOffer = ReplayKit.jumpTo(from, offer)
        if (!pressed) return atOffer.applyChoice(answer(offer, flag))
        val press = node(offer).choices.single { it.nextNodeId == explain }
        return atOffer.applyChoice(press).applyChoice(answer(explain, flag))
    }

    @Test
    fun `pressed and unpressed tellings in any order carry only the current answer`() {
        val orders = family.flatMap { a -> family.flatMap { b -> family.map { c -> listOf(a, b, c) } } }
        assertEquals(27, orders.size)
        orders.forEach { flags ->
            for (pattern in 0 until 8) {
                var s = aided
                flags.forEachIndexed { i, flag ->
                    val pressed = (pattern shr i) and 1 == 1
                    s = tell(s, i % 2, pressed, flag)
                    assertEquals("$flags, pattern $pattern, telling ${i + 1}", setOf(flag), heldFamily(s))
                }
                val current = flags.last()
                chapter7Scenes.forEach { sceneId ->
                    val offered = StoryRepository.visibleChoices(node(sceneId), s).map { it.nextNodeId }
                        .filter { it in chapter7Option.values }
                    assertEquals("$flags, pattern $pattern at $sceneId", listOf(chapter7Option.getValue(current)), offered)
                }
                val envoyLines = FlagDescriptions.sections(s.flags).flatMap { it.lines }.filter { "envoy" in it }
                assertEquals("$flags, pattern $pattern", listOf(journalLine.getValue(current)), envoyLines)
            }
        }
    }

    // ---- the whole path ---------------------------------------------------------------------------------

    @Test
    fun `end to end - every approach and answer, pressed or not, reaches the escort scene and the fight`() {
        val encounter = CombatRepository.encounter("cinder_envoy_encounter")
        approachFlags.forEachIndexed { approach, startFlag ->
            family.forEach { reply ->
                listOf(false, true).forEach { pressed ->
                    var s = ReplayKit.jumpTo(aided, "the_marshals_envoy")
                    val path = ArrayList<String>()
                    while (s.sceneId != "the_marshals_warning") {
                        val n = node(s.sceneId)
                        path += n.id
                        val visible = StoryRepository.visibleChoices(n, s)
                        val choice = when (n.id) {
                            "the_marshals_envoy" -> visible.firstOrNull { startFlag in it.consequences.setFlags }
                            pairs[approach].first ->
                                if (pressed) visible.firstOrNull { it.label == pressLabel }
                                else visible.firstOrNull { reply in it.consequences.setFlags }
                            pairs[approach].second -> visible.firstOrNull { reply in it.consequences.setFlags }
                            else -> null
                        } ?: visible.first()
                        s = s.applyChoice(choice)
                        check(path.size < 20) { "no way to the warning: $path" }
                    }
                    val label = "$startFlag, $reply, pressed=$pressed: $path"
                    assertEquals(label, pressed, pairs[approach].second in path)
                    assertEquals(label, "the_escort_closes", path.last())
                    assertEquals(label, setOf(reply), heldFamily(s))
                    val won = s.resolveCombat(encounter, CombatOutcome(victory = true, damageTaken = 0))
                    assertEquals(label, "aftermath_of_betrayal", won.sceneId)
                }
            }
        }
    }

    // ---- what the two scenes say ------------------------------------------------------------------------

    @Test
    fun `the two scenes say different things, and what they reveal is hedged as something the envoy was told`() {
        val alone = flat(node(explainAlone).narrativeText)
        val with = flat(node(explainWith).narrativeText)
        // Alone: Kaelen's King's Guard past, and the hand that must answer.
        assertTrue(alone.contains("King's Guard"))
        assertTrue(alone.contains("I was told a taken brand is a broken one"))
        assertTrue(alone.contains("That the hand must answer when it is offered"))
        assertTrue(alone.contains("I don't know what that means"))
        assertTrue(alone.contains("the birds have stopped"))
        assertFalse(alone.contains("Halvard"))
        assertFalse(alone.contains("Warden"))
        // With Halvard: the offer of rest, his weariness and his loyalty.
        assertTrue(with.contains("six centuries"))
        assertTrue(with.contains("There is a bed waiting"))
        assertTrue(with.contains("a watch isn't mine to put down because I'm tired"))
        assertTrue(with.contains("You've looked at that treeline four times"))
        assertFalse(with.contains("King's Guard"))
        // The Marshal is never said to have been at a failing gate: the envoy only repeats what he was told.
        assertTrue(with.contains("The Marshal knows what it costs when a gate fails"))
        assertTrue(with.contains("That is how I was told to put it"))
        assertTrue(with.contains("How the Marshal knows, I wasn't told"))
        assertFalse(with.contains("stood at a gate"))
        assertFalse(alone.contains("stood at a gate"))
    }

    @Test
    fun `the new text follows the prose rules and keeps the Marshal ungendered`() {
        val reserved = Regex(
            "\\b(lad|settings|seventh door|frame|ashwell|archivist|selvane|emberlow|fenmoor|duskmere|ordrun|ilsevet|ring)\\b",
            RegexOption.IGNORE_CASE
        )
        val gendered = Regex("\\b(he|him|his|she|her|hers)\\b", RegexOption.IGNORE_CASE)
        listOf(explainAlone, explainWith).forEach { id ->
            val n = node(id)
            // The title and each label are checked as pieces of their own: a title has no full stop, so joined
            // to the prose it would run into the first sentence.
            val pieces = (listOf(n.title, n.narrativeText) + n.choices.map { it.label }).map { flat(it) }
            val text = pieces.joinToString(" ")
            assertFalse("$id has a dash used as punctuation", text.any { it == '—' || it == '–' })
            assertFalse("$id has a sentence fragment starting 'Not'", Regex("(^|[.!?\"] )Not ").containsMatchIn(text))
            assertNull("$id uses a reserved word: ${reserved.find(text)?.value}", reserved.find(text))
            pieces.forEach { piece ->
                piece.split(Regex("(?<=[.!?]\"?) ")).filter { "Marshal" in it }.forEach { sentence ->
                    assertFalse("$id: '$sentence'", gendered.containsMatchIn(sentence))
                }
            }
        }
    }

    @Test
    fun `the new scenes stay inside their word budgets and the chapter within its tolerance`() {
        assertTrue("alone has ${words(node(explainAlone))} words", words(node(explainAlone)) <= 160)
        assertTrue("with Halvard has ${words(node(explainWith))} words", words(node(explainWith)) <= 195)
        val chapterTotal = chapterNodes.sumOf { words(it) }
        assertTrue("Chapter 5 totals $chapterTotal words, tolerance 3,200 (target 3,000)", chapterTotal <= 3_200)
    }
}
