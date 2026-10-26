package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
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
 * Chapter 5, after the missive: the conversation with Halvard. The missive gains a short paragraph, the choice
 * "Bring this to Halvard." now leads to a hub where Halvard asks what the envoy was really there for, and the
 * player either tells the truth (in the form that fits how they answered the envoy) or keeps the offer back.
 * Four short tails lead on to the unchanged end scene.
 *
 * The decision is expressed through dialogue, two mutually exclusive flags and the Journal; it changes no stat.
 * These tests pin the routing and the gates, that every decision clears the whole account family and nothing
 * else, that replaying with a different decision leaves only the latest flag and the real Journal shows only
 * its entry, that the two insertions are insertions only, and that the conversation works after every
 * aftermath route and both outcomes of the fight.
 */
class Chapter5HalvardHubTest {

    private val missive = "what_the_missive_says"
    private val hub = "halvard_asks"
    private val endScene = "chapter5_end"
    private val offerAlone = "the_envoys_offer_alone"

    private val refused = "refused_envoy"
    private val heard = "heard_envoy_offer"
    private val confronted = "confronted_envoy_with_prisoners_words"
    private val stances = listOf(refused, heard, confronted)

    private val told = "told_halvard_the_truth"
    private val kept = "kept_the_offer_from_halvard"
    /** Spelled out here, not read from the constant, so an emptied or wrong family fails these tests. */
    private val account = setOf(told, kept)

    private val truthTail = mapOf(
        refused to "halvard_hears_the_refusal",
        heard to "halvard_hears_the_listening",
        confronted to "halvard_hears_the_prisoners_words"
    )
    private val concealTail = "halvard_gets_only_the_page"
    private val allTails = truthTail.values.toList() + concealTail
    private val truthLabel = mapOf(
        refused to "Tell him the whole of it: the offer, and that you said no.",
        heard to "Tell him you listened, and that some of it landed.",
        confronted to "Tell him what the prisoner said, and that it said it first."
    )
    private val concealLabel = "Give him the page and say nothing of the offer."

    private val journalTold = "I told Halvard the whole of what the envoy wanted, and how I met it."
    private val journalKept = "I gave Halvard the missive and kept the envoy's offer to myself."
    private val journalTopic = "the question of what to tell Halvard"
    private val envoyLine = mapOf(
        refused to "I told the envoy the Marshal's answer was no.",
        heard to "I heard the envoy out, though I promised nothing.",
        confronted to "I told the envoy the prisoner had already said what the Marshal wants."
    )

    private val addendum = "Beneath the signature stand four short lines. Offer first. If refused, show the cost. " +
        "Do not spend the bearer. Do not recover the envoy. Kaelen reads the last line twice. " +
        "The envoy was never meant to ride home."
    private val endingSentence = "He does not read the last lines aloud, and Kaelen does not ask him to."

    private val missiveBefore = "The dispatch case holds one folded page, sealed in black wax stamped with a closed fist " +
        "wrapped around a flame. The handwriting inside is precise, unhurried, and signed with a single name Kaelen " +
        "has never heard spoken aloud: Ilsevet. The orders aren't about one gate. They name five others like it, " +
        "scattered across ground Kaelen once swore to protect as a King's Guard, each with its own warden running as " +
        "thin as Halvard. Voss's siege wasn't a punishment, reading it now. It was a rehearsal, for unmaking every " +
        "ward like this one at once and seeing what answers when they fall together instead of one at a time."
    private val endBefore = "Halvard reads the missive twice before he says anything, and when he does, his voice has lost " +
        "the dry patience Kaelen has gotten used to. \"Six wards,\" he says. \"Six jailers, when the office was whole. " +
        "I always assumed the others fell the way mine nearly did: one at a time, one door at a time, forgotten.\" " +
        "He folds the page shut. \"I never considered someone might be counting.\" Outside, the ruined road is empty " +
        "again, rain filling in the tracks the escort left behind. It won't stay empty. Kaelen knows that now with a " +
        "certainty that has nothing to do with the ache in his branded palm, and everything to do with a name he's " +
        "already decided he isn't going to forget. Chapter VI awaits."

    private val chapterNodes = StoryRepository.nodesInChapter("chapter_5")
    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val everyChoice: List<Pair<String, Choice>> = allNodes.flatMap { n -> n.choices.map { n.id to it } }
    private val aided = ReplayKit.STRONG.copy(flags = setOf("accepted_dark_aid"))

    private fun node(id: String): StoryNode = StoryRepository.node(id)
    private fun flat(s: String): String = s.replace(Regex("\\s+"), " ").trim()
    private fun words(n: StoryNode): Int = flat(n.narrativeText).split(" ").size
    private fun held(s: GameState, family: Set<String>): Set<String> = s.flags.intersect(family)
    private fun visibleAtHub(s: GameState): List<Choice> = StoryRepository.visibleChoices(node(hub), s)

    // ---- the missive, the hub's only way in, and the ending ---------------------------------------------

    @Test
    fun `the missive gains one paragraph and the hub is the only way on`() {
        val reading = node(missive).choices.single()
        assertEquals("Bring this to Halvard.", reading.label)
        assertEquals(hub, reading.nextNodeId)
        assertNull(reading.requirements)
        assertTrue(reading.consequences.statDeltas.isEmpty() && reading.consequences.setFlags.isEmpty())
        val now = flat(node(missive).narrativeText)
        assertEquals(1, Regex(Regex.escape(addendum)).findAll(now).count())
        assertEquals(missiveBefore, now.replace(" $addendum", ""))
        assertEquals(listOf(missive), everyChoice.filter { it.second.nextNodeId == hub }.map { it.first })
    }

    @Test
    fun `the ending gains one sentence and still leads to Chapter 6`() {
        val now = flat(node(endScene).narrativeText)
        assertEquals(1, Regex(Regex.escape(endingSentence)).findAll(now).count())
        assertEquals(endBefore, now.replace(" $endingSentence", ""))
        val rest = node(endScene).choices.single()
        assertEquals("Rest, while you can.", rest.label)
        assertEquals("the_choice_of_roads", rest.nextNodeId)
        // Only the four tails lead here now: the missive no longer does.
        assertEquals(allTails.sorted(), everyChoice.filter { it.second.nextNodeId == endScene }.map { it.first }.sorted())
    }

    // ---- the hub ----------------------------------------------------------------------------------------

    @Test
    fun `the hub has the exact labels, gates and flags, and changes no stat, item or trophy`() {
        val n = node(hub)
        assertEquals("chapter_5", n.chapterId)
        assertEquals("What Halvard Asks", n.title)
        assertEquals("dwarven_hold_gate", n.illustrationId)
        assertNotNull(IllustrationCatalog.get(n.illustrationId))
        assertNull(n.combatEncounterId)
        assertEquals(4, n.choices.size)
        stances.forEach { stance ->
            val c = n.choices.single { it.label == truthLabel.getValue(stance) }
            assertEquals(stance, truthTail.getValue(stance), c.nextNodeId)
            assertEquals(stance, com.thelastjailer.app.ChoiceRequirement(requiredFlags = setOf(stance)), c.requirements)
            assertEquals(stance, setOf(told), c.consequences.setFlags)
        }
        val conceal = n.choices.single { it.label == concealLabel }
        assertEquals(concealTail, conceal.nextNodeId)
        assertNull(conceal.requirements)
        assertEquals(setOf(kept), conceal.consequences.setFlags)
        n.choices.forEach { c ->
            assertEquals(c.label, account, c.consequences.clearFlags)
            assertTrue(c.label, c.consequences.statDeltas.isEmpty())
            assertTrue(c.label, c.consequences.grantItemIds.isEmpty())
            assertNull(c.label, c.consequences.unlockTrophy)
            // The envoy answers are read here, never set or cleared.
            assertTrue(c.label, (c.consequences.setFlags + c.consequences.clearFlags).none { it in stances })
        }
    }

    @Test
    fun `each telling is offered exactly two choices, and the hub can never leave the player stuck`() {
        stances.forEach { stance ->
            val state = ReplayKit.jumpTo(aided.copy(flags = setOf("accepted_dark_aid", stance)), hub)
            assertEquals(stance, setOf(truthLabel.getValue(stance), concealLabel), visibleAtHub(state).map { it.label }.toSet())
            assertEquals(stance, 2, visibleAtHub(state).size)
        }
        // A save with no envoy answer at all (it cannot happen on a new play) still has a way on.
        val none = ReplayKit.jumpTo(aided, hub)
        assertEquals(listOf(concealLabel), visibleAtHub(none).map { it.label })
    }

    // ---- the tails --------------------------------------------------------------------------------------

    @Test
    fun `each tail is a single step on to the end scene and is reached only from the hub`() {
        allTails.forEach { id ->
            val n = node(id)
            assertEquals(id, "chapter_5", n.chapterId)
            assertEquals(id, "warden_chamber", n.illustrationId)
            assertNotNull(id, IllustrationCatalog.get(n.illustrationId))
            assertNull(id, n.combatEncounterId)
            val c = n.choices.single()
            assertEquals(id, endScene, c.nextNodeId)
            assertEquals(id, if (id == concealTail) "Let him read." else "Give him the page.", c.label)
            assertNull(id, c.requirements)
            assertTrue(id, c.consequences.statDeltas.isEmpty() && c.consequences.setFlags.isEmpty())
            assertTrue(id, c.consequences.clearFlags.isEmpty() && c.consequences.grantItemIds.isEmpty())
            assertNull(id, c.consequences.unlockTrophy)
            assertEquals(id, listOf(hub), everyChoice.filter { it.second.nextNodeId == id }.map { it.first })
        }
        assertEquals("What Halvard Lets Lie", node(concealTail).title)
        truthTail.values.forEach { assertEquals("What Halvard Hears", node(it).title) }
        assertEquals(4, allTails.toSet().size)
    }

    // ---- the flags --------------------------------------------------------------------------------------

    @Test
    fun `the account flags are set only at the hub, and every choice that sets one clears both`() {
        val setters = everyChoice.filter { (_, c) -> c.consequences.setFlags.any { it in account } }
        val clearers = everyChoice.filter { (_, c) -> c.consequences.clearFlags.any { it in account } }
        assertEquals(List(4) { hub }, setters.map { it.first })
        setters.forEach { (id, c) ->
            assertEquals("$id: ${c.label}", account, c.consequences.clearFlags)
            assertEquals("$id: ${c.label}", 1, c.consequences.setFlags.size)
        }
        assertEquals(setters.map { it.second.label }.toSet(), clearers.map { it.second.label }.toSet())
        assertEquals(account, HALVARD_ACCOUNT_FAMILY)
    }

    // ---- replay: only the latest decision, and the real Journal shows only its entry --------------------

    private class Pick(val stance: String, val truth: Boolean) {
        override fun toString() = "$stance/${if (truth) "truth" else "concealed"}"
    }
    private val picks = stances.flatMap { s -> listOf(true, false).map { Pick(s, it) } }

    private fun stanceChoice(flag: String): Choice = node(offerAlone).choices.single { flag in it.consequences.setFlags }

    /** One telling's answer to the envoy and its decision at the hub; returns the states before and after the hub choice. */
    private fun decide(from: GameState, p: Pick): Pair<GameState, GameState> {
        val answered = ReplayKit.jumpTo(from, offerAlone).applyChoice(stanceChoice(p.stance))
        val beforeHub = ReplayKit.jumpTo(answered, hub)
        val choice = visibleAtHub(beforeHub).single { c -> (if (p.truth) told else kept) in c.consequences.setFlags }
        return beforeHub to beforeHub.applyChoice(choice)
    }

    private fun apartFromFlagsAndScene(s: GameState) = s.copy(sceneId = "", chapterId = "", flags = emptySet())

    private fun accountLines(flags: Set<String>): List<String> =
        FlagDescriptions.sections(flags).flatMap { it.lines }
            .filter { it == journalTold || it == journalKept || it.contains(journalTopic) }

    @Test
    fun `replaying with a different decision leaves only the latest account flag, and the Journal shows only its entry`() {
        val unrelated = setOf("accepted_dark_aid", "halvard_died", "kept_the_token", "refused_the_prisoner", "lost_to_cinder_adept")
        assertEquals(216, picks.size * picks.size * picks.size)
        picks.forEach { a -> picks.forEach { b -> picks.forEach { c ->
            var s = aided.copy(flags = unrelated)
            listOf(a, b, c).forEachIndexed { i, p ->
                val label = "$a, $b, $c at telling ${i + 1}"
                val (before, after) = decide(s, p)
                val expectedFlag = if (p.truth) told else kept
                assertEquals(label, setOf(expectedFlag), held(after, account))
                assertEquals(label, setOf(p.stance), held(after, stances.toSet()))
                // The real Journal: exactly this telling's entry, never the neutral "more than once" line.
                assertEquals(label, listOf(if (p.truth) journalTold else journalKept), accountLines(after.flags))
                assertEquals(label, listOf(envoyLine.getValue(p.stance)), FlagDescriptions.sections(after.flags).flatMap { it.lines }.filter { "envoy" in it && "Halvard" !in it })
                // The decision costs nothing and changes no envoy answer or unrelated discovery.
                assertEquals(label, apartFromFlagsAndScene(before), apartFromFlagsAndScene(after))
                assertTrue(label, after.flags.containsAll(unrelated))
                s = after
            }
        } } }
    }

    @Test
    fun `the Journal entries read as the choice made, and a stale pair collapses to one neutral line`() {
        val e = FlagDescriptions.entries.associateBy { it.flag }
        assertEquals(journalTold, e.getValue(told).text)
        assertEquals(journalKept, e.getValue(kept).text)
        account.forEach { f ->
            assertEquals(JournalCategory.PEOPLE, e.getValue(f).category)
            assertEquals("halvard_account", e.getValue(f).group)
            assertFalse(FlagDescriptions.hidden.containsKey(f))
        }
        assertEquals(journalTopic, FlagDescriptions.groupTopics["halvard_account"])
        assertEquals(listOf(journalTold), accountLines(setOf(told)))
        assertEquals(listOf(journalKept), accountLines(setOf(kept)))
        val both = accountLines(setOf(told, kept))
        assertEquals(1, both.size)
        assertTrue(both.single().contains(journalTopic))
        assertFalse(both.single().contains("I told") || both.single().contains("I gave"))
    }

    // ---- the whole path ---------------------------------------------------------------------------------

    @Test
    fun `end to end - every aftermath route and fight outcome reaches the hub, a tail and Chapter 6`() {
        val encounter = CombatRepository.encounter("cinder_envoy_encounter")
        val routeFlag = listOf("chased_the_envoy", "chased_the_envoy", "stayed_with_halvard", "searched_the_ground")
        val fork = listOf("the_ford_caught", "the_ford_lost", null, null)
        var checked = 0
        stances.forEach { stance ->
            for (route in 0..3) for (victory in listOf(true, false)) for (truth in listOf(true, false)) {
                var s = ReplayKit.jumpTo(aided, "the_marshals_envoy")
                val path = ArrayList<String>()
                var beforeHub: GameState? = null
                var afterHub: GameState? = null
                while (s.sceneId != "the_choice_of_roads") {
                    val n = node(s.sceneId)
                    path += n.id
                    check(path.size < 40) { "no way to Chapter 6: $path" }
                    if (n.combatEncounterId != null) { s = s.resolveCombat(encounter, CombatOutcome(victory, 0)); continue }
                    val visible = StoryRepository.visibleChoices(n, s)
                    val choice = when (n.id) {
                        "the_marshals_envoy" -> visible.first { "met_envoy_alone" in it.consequences.setFlags }
                        offerAlone, "the_envoys_offer_with_halvard" -> visible.first { stance in it.consequences.setFlags }
                        "aftermath_of_betrayal" -> visible.first { routeFlag[route] in it.consequences.setFlags }
                        "the_chase_to_the_ford" -> visible.first { it.nextNodeId == fork[route] }
                        hub -> visible.single { (if (truth) told else kept) in it.consequences.setFlags }
                        else -> visible.first()
                    }
                    if (n.id == hub) beforeHub = s
                    s = s.applyChoice(choice)
                    if (n.id == hub) afterHub = s
                }
                val label = "$stance, route $route, victory=$victory, truth=$truth: $path"
                assertEquals(label, 1, path.count { it == hub })
                assertEquals(label, 1, path.count { it in allTails })
                assertEquals(label, 1, path.count { it == endScene })
                assertEquals(label, if (truth) truthTail.getValue(stance) else concealTail, path.single { it in allTails })
                assertEquals(label, 1, s.inventory.count { it == "cinder_marshal_missive" })
                assertEquals(label, setOf(stance), held(s, stances.toSet()))
                assertEquals(label, setOf(if (truth) told else kept), held(s, account))
                assertTrue(label, s.health >= 1)
                // The decision changes no stat, item or trophy. Health may rise by the engine's ordinary recovery on any
                // scene transition (a point or more when wounded), and the decision itself never lowers it.
                val b = beforeHub!!
                val a = afterHub!!
                assertTrue(label, a.health >= b.health)
                assertEquals(label, apartFromFlagsAndScene(b).copy(health = 0), apartFromFlagsAndScene(a).copy(health = 0))
                checked++
            }
        }
        assertEquals(48, checked)
    }

    // ---- prose rules and budgets ------------------------------------------------------------------------

    @Test
    fun `the new text follows the prose rules and keeps the Marshal ungendered`() {
        val reserved = Regex(
            "\\b(lad|settings|seventh door|frame|ashwell|archivist|selvane|emberlow|fenmoor|duskmere|ordrun|ilsevet|ring|same hand|willingly)\\b",
            RegexOption.IGNORE_CASE
        )
        val gendered = Regex("\\b(he|him|his|she|her|hers)\\b", RegexOption.IGNORE_CASE)
        val pieces = (listOf(hub) + allTails).flatMap { id ->
            val n = node(id)
            listOf(n.title, n.narrativeText) + n.choices.map { it.label }
        }.map { flat(it) } + listOf(addendum, endingSentence)
        val text = pieces.joinToString(" ")
        assertFalse("a dash used as punctuation", text.any { it == '—' || it == '–' })
        assertFalse("a sentence fragment starting 'Not'", Regex("(^|[.!?\"] )Not ").containsMatchIn(text))
        assertNull("a reserved word: ${reserved.find(text)?.value}", reserved.find(text))
        pieces.forEach { piece ->
            piece.split(Regex("(?<=[.!?]\"?) ")).filter { "Marshal" in it }.forEach { sentence ->
                assertFalse("'$sentence'", gendered.containsMatchIn(sentence))
            }
        }
        // The orders name no gate, setting or frame, and their lines are exactly these four.
        assertTrue(flat(node(missive).narrativeText).contains("Offer first. If refused, show the cost. Do not spend the bearer. Do not recover the envoy."))
    }

    @Test
    fun `the new scenes and the two insertions stay inside their word budgets and the chapter within its tolerance`() {
        assertTrue("hub ${words(node(hub))}", words(node(hub)) <= 78)
        allTails.forEach { assertTrue("$it ${words(node(it))}", words(node(it)) <= 82) }
        assertTrue("missive ${words(node(missive))}", words(node(missive)) <= 155)
        assertTrue("end ${words(node(endScene))}", words(node(endScene)) <= 145)
        val chapterTotal = chapterNodes.sumOf { words(it) }
        assertTrue("Chapter 5 totals $chapterTotal words, tolerance 3,200 (target 3,000)", chapterTotal <= 3_200)
    }
}
