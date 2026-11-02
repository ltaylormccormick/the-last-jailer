package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.LockedDisplay
import com.thelastjailer.app.StatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * The 31 scenes Chapters 18, 19 and 20 shipped with, held to what they were, with the approved changes of the Chapter 18-20 expansion
 * written out as the only exceptions:
 *  - eight scenes carry approved wording corrections (their hash is the one derived from the original text with the correction applied);
 *  - the shipped choices lead where the new scenes now sit between them (the retarget table), six labels say what the next scene now
 *    does (the relabel table), the two "Decide." choices became one, and the end of Chapter 18 is written once per fate;
 *  - every shipped choice keeps its stat changes, flags set and cleared, items, trophy and stat gates, and every other choice keeps its
 *    label, gates and destination;
 *  - the fights, their enemies and rewards are as shipped, except that the Ghostwriter's victory now passes the two new scenes after it.
 */
class Chapter1820ShippedPinTest {

    private fun node(id: String) = StoryRepository.node(id)

    private fun collapse(t: String) = t.replace(Regex("\\s+"), " ").trim()
    private fun sha(t: String) = MessageDigest.getInstance("SHA-256").digest(collapse(t).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun stats(m: Map<StatType, Int>) = m.entries.sortedBy { it.key.name }.joinToString(",") { "${it.key.name}=${it.value}" }
    private fun sorted(s: Set<String>) = s.sorted().joinToString(",")

    /** What a choice does, apart from where it leads and what it is called. */
    private fun effect(c: Choice) = listOf(
        stats(c.consequences.statDeltas), sorted(c.consequences.setFlags), sorted(c.consequences.clearFlags), c.consequences.grantItemIds.joinToString(","),
        c.consequences.unlockTrophy ?: "", stats(c.requirements?.minStats ?: emptyMap())
    )

    private fun effect(c: Chapter1820Shipped.C) = listOf(c.deltas, c.set, c.clear, c.items, c.trophy, c.min)

    private fun gate(c: Choice) = sorted(c.requirements?.requiredFlags ?: emptySet()) to sorted(c.requirements?.forbiddenFlags ?: emptySet())

    /** "scene|label as shipped" -> the label the choice carries now. */
    private val relabelled = mapOf(
        "the_dead_archivist_pressed|Bring this to the prisoner." to "Ask what came before the drawings.",
        "the_dead_archivist_patient|Bring this to the prisoner." to "Ask what came before the drawings.",
        "the_prisoners_reaction|Send word to Selvane." to "Ask about the archivist.",
        "what_selvane_finds|Read the names." to "Read Selvane's report.",
        "the_road_to_the_workshop_alone|Go inside." to "Enter the village.",
        "the_road_to_the_workshop_with_voss|Go inside." to "Enter the village."
    )

    /** "scene|label as shipped" -> every scene the copies of that choice lead to now. */
    private val retargeted: Map<String, Set<String>> = mapOf(
        "her_own_terms_readily|Accept." to setOf("c18_after_terms"),
        "her_own_terms_pressed|Accept." to setOf("c18_after_terms"),
        "what_is_left_of_her|Decide." to setOf("c18_after_duel"),
        "chapter18_end|Rest, while it lasts." to setOf("c19_fate_captured", "c19_fate_mercy", "c19_fate_deferred"),
        "the_dead_archivist_pressed|Bring this to the prisoner." to setOf("c19_ashwell_years"),
        "the_dead_archivist_patient|Bring this to the prisoner." to setOf("c19_ashwell_years"),
        "the_prisoners_reaction|Send word to Selvane." to setOf("c19_prisoner_pause"),
        "what_selvane_finds|Read the names." to setOf("c19_report_seal"),
        "the_addressed_names|\"Then whoever it is, they've been waiting on me specifically. Let them keep waiting.\"" to setOf("c19_after_defiance"),
        "the_addressed_names|\"If I was always meant to stand here, I intend to make sure I'm the one who decides what that means.\"" to setOf("c19_after_claim"),
        "the_addressed_names|Say nothing. There isn't an answer that fits in a sentence." to setOf("c19_after_silence"),
        "chapter19_end_defied|Go on." to setOf("c20_before_departure"),
        "chapter19_end_claimed|Go on." to setOf("c20_before_departure"),
        "chapter19_end_unanswered|Go on." to setOf("c20_before_departure"),
        "the_workshop|\"I'll go alone. This deserves a direct look, not a delegation.\"" to setOf("c20_alone_mile"),
        "the_workshop|\"Voss comes with me. Two sets of eyes, not one.\"" to setOf("c20_with_voss_mile"),
        "the_road_to_the_workshop_alone|Go inside." to setOf("c20_village_edge"),
        "the_road_to_the_workshop_with_voss|Go inside." to setOf("c20_village_edge"),
        "what_the_workshop_holds|Keep reading." to setOf("c20_guardian_warning"),
        "ghostwriter_defeat|Check the workbench." to setOf("c20_after_guardian"),
        "the_last_page|Take the page, and go home." to setOf("c20_sixth_question")
    )

    /** How many copies of a shipped choice there are now, where it is not one. */
    private val copies = mapOf("what_is_left_of_her|Decide." to 1, "chapter18_end|Rest, while it lasts." to 3)

    /** The two scenes whose gates are new: one ungated choice, and a dispatch by the three fate flags. The tests of the expansion prove both. */
    private val regated = setOf("what_is_left_of_her", "chapter18_end")

    @Test
    fun `the 31 shipped scenes are all still there, in their chapters, with their titles, pictures and fights`() {
        assertEquals(31, Chapter1820Shipped.scenes.size)
        Chapter1820Shipped.scenes.forEach { e ->
            val n = node(e.id)
            assertEquals("${e.id} chapter", e.chapter, n.chapterId)
            assertEquals("${e.id} title", e.title, n.title)
            assertEquals("${e.id} picture", e.illustration, n.illustrationId)
            assertEquals("${e.id} fight", e.combat, n.combatEncounterId ?: "")
        }
        Chapter1820Shipped.chapters.forEach { c ->
            val actual = StoryRepository.chapters.single { it.id == c.id }
            assertEquals(c.number, actual.number)
            assertEquals(c.title, actual.title)
            assertEquals("start scene of ${c.id}", c.start, actual.startNodeId)
        }
    }

    @Test
    fun `the shipped prose is word for word, except the eight approved corrections which are exactly those`() {
        val changed = ArrayList<String>()
        Chapter1820Shipped.scenes.forEach { e ->
            val expected = Chapter1820Shipped.approvedEdits[e.id] ?: e.sha
            if (sha(node(e.id).narrativeText) != expected) changed += e.id
        }
        assertEquals("shipped scenes whose prose is not as shipped or as approved", emptyList<String>(), changed)
        assertEquals(
            setOf(
                "no_more_envoys", "what_kaelen_says_first", "what_selvane_finds", "the_workshop", "the_last_page",
                "chapter20_end_demanded", "chapter20_end_gave_time", "chapter20_end_waited"
            ),
            Chapter1820Shipped.approvedEdits.keys
        )
        Chapter1820Shipped.approvedEdits.forEach { (id, approved) ->
            val original = Chapter1820Shipped.scenes.single { it.id == id }.sha
            assertTrue("$id: the approved hash is the original", approved != original)
        }
    }

    @Test
    fun `every shipped choice keeps its effects, items, trophy and stat gate, and every unchanged choice keeps its label, gates and destination`() {
        val problems = ArrayList<String>()
        Chapter1820Shipped.scenes.forEach { e ->
            val n = node(e.id)
            val groups = e.choices.groupBy { it.label }
            val expectedLabels = groups.keys.map { relabelled["${e.id}|$it"] ?: it }.toSet()
            if (expectedLabels != n.choices.map { it.label }.toSet()) problems += "${e.id}: labels are ${n.choices.map { it.label }}, expected $expectedLabels"
            groups.forEach { (label, originals) ->
                val key = "${e.id}|$label"
                val now = relabelled[key] ?: label
                val mine = n.choices.filter { it.label == now }
                if (mine.size != (copies[key] ?: originals.size)) problems += "$key: ${mine.size} copies, expected ${copies[key] ?: originals.size}"
                if (originals.map { effect(it) }.toSet() != mine.map { effect(it) }.toSet()) problems += "$key: effects, items, trophy or stat gate changed"
                val expectedDestinations = retargeted[key] ?: originals.map { it.next }.toSet()
                if (mine.map { it.nextNodeId }.toSet() != expectedDestinations) problems += "$key: leads to ${mine.map { it.nextNodeId }.toSet()}, expected $expectedDestinations"
                if (e.id !in regated && originals.map { it.required to it.forbidden }.toSet() != mine.map { gate(it) }.toSet()) problems += "$key: gate changed"
                mine.forEach { if (it.lockedDisplay != LockedDisplay.HIDE || it.lockedHint != null) problems += "$key: changed how it shows when locked" }
            }
        }
        assertEquals("shipped choices that no longer match", emptyList<String>(), problems)
        assertEquals("the retarget table lists only choices that exist", emptySet<String>(),
            retargeted.keys - Chapter1820Shipped.scenes.flatMap { s -> s.choices.map { "${s.id}|${it.label}" } }.toSet())
    }

    @Test
    fun `the two Decide choices are now one, ungated, and the end of Chapter 18 is written once per fate`() {
        val decide = node("what_is_left_of_her").choices
        assertEquals(1, decide.size)
        assertEquals("Decide.", decide.single().label)
        assertNull(decide.single().requirements)
        assertEquals(com.thelastjailer.app.Consequences(), decide.single().consequences)
        val rest = node("chapter18_end").choices
        assertEquals(3, rest.size)
        rest.forEach { assertEquals("Rest, while it lasts.", it.label) }
        assertEquals(
            setOf("c19_fate_captured" to Pair(setOf("captured_ilsevet"), emptySet<String>()),
                "c19_fate_mercy" to Pair(setOf("showed_ilsevet_mercy"), setOf("captured_ilsevet")),
                "c19_fate_deferred" to Pair(emptySet(), setOf("captured_ilsevet", "showed_ilsevet_mercy"))),
            rest.map { it.nextNodeId to Pair(it.requirements?.requiredFlags ?: emptySet(), it.requirements?.forbiddenFlags ?: emptySet()) }.toSet()
        )
        rest.forEach { assertEquals(com.thelastjailer.app.Consequences(), it.consequences) }
    }

    @Test
    fun `the fights, their enemies and rewards are as shipped, and the Ghostwriter's victory passes the two new scenes`() {
        Chapter1820Shipped.fights.forEach { f ->
            val encounter = CombatRepository.encounter(f.id)
            assertEquals("${f.id}: encounter", f.shape, encounter.toString())
            assertEquals("${f.id}: enemy", f.enemy, EnemyCatalog.get(encounter.enemyId).toString())
        }
        val duel = CombatRepository.encounter("ilsevet_duel_encounter")
        assertEquals("what_is_left_of_her", duel.victoryNodeId)
        assertEquals("ilsevet_duel_defeat", duel.defeatNodeId)
        val ghost = CombatRepository.encounter("ghostwriter_encounter")
        assertEquals("c20_after_guardian", ghost.victoryNodeId)
        assertEquals("ghostwriter_defeat", ghost.defeatNodeId)
        assertEquals("Whoever Finished It", ghost.unlockTrophy)
        listOf("the_duel_spoke", "the_duel_silent").forEach { assertEquals("ilsevet_duel_encounter", node(it).combatEncounterId); assertTrue(node(it).choices.isEmpty()) }
        assertEquals("ghostwriter_encounter", node("the_workshops_own_guardian").combatEncounterId)
        assertTrue(node("the_workshops_own_guardian").choices.isEmpty())
    }

    @Test
    fun `the three items are as they were, and the frame notes carry the approved description`() {
        Chapter1820Shipped.items.forEach { i ->
            val actual = ItemCatalog.get(i.id)!!
            assertEquals(i.id, i.name, actual.name)
            assertEquals(i.id, i.description, actual.description)
        }
    }

    @Test
    fun `Chapters 18, 19 and 20 hold only the 31 shipped scenes and the 55 the expansion adds`() {
        val shippedIds = Chapter1820Shipped.scenes.map { it.id }.toSet()
        val added = listOf("chapter_18", "chapter_19", "chapter_20").associateWith { c -> StoryRepository.nodesInChapter(c).map { it.id }.filter { it !in shippedIds } }
        assertEquals(19, added.getValue("chapter_18").size)
        assertEquals(20, added.getValue("chapter_19").size)
        assertEquals(16, added.getValue("chapter_20").size)
        added.forEach { (chapter, ids) ->
            val prefix = "c" + chapter.removePrefix("chapter_") + "_"
            assertTrue("$chapter holds a scene that is neither shipped nor new: $ids", ids.all { it.startsWith(prefix) })
        }
        listOf("c20_grief_heard", "c20_need", "c20_wait", "c20_choice_return").forEach {
            assertNull("$it is held out of this batch", StoryRepository.nodeOrNull(it))
        }
    }
}
