package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.GameState
import com.thelastjailer.app.LockedDisplay
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The eight scenes that shipped in Chapter 17 stay as they were. Their prose is pinned word for word (a SHA-256 of the
 * whitespace-collapsed text, as for Chapters 5 to 16), and every shipped choice keeps its label, its stat changes, its flags, its
 * items, its trophy and its gates. What may change is where a choice leads, and only for the three retargets the author approved:
 * the two "Move out." choices lead to `c17_chain_gate`, and "Get to the chamber." becomes three rung-gated copies of itself. This
 * class accepts the chapter as it is now (B0, before any of that is built) and as it will be once the package is in, and nothing
 * between. The fight, its routing and its enemy are pinned as they are in the combat catalogue. The expected values are in
 * [Chapter17Shipped], generated at B0 from the sources at 369a95f.
 *
 * The checks are pure functions of a scene, so the last tests can put changed copies of real scenes through them and show that each
 * kind of damage is noticed, and that the planned state is accepted, without touching the story.
 */
class Chapter17ShippedPinTest {

    private val shippedIds: Set<String> = Chapter17Shipped.scenes.map { it.id }.toSet()
    private fun shipped(id: String) = Chapter17Shipped.scenes.single { it.id == id }
    private fun node(id: String) = StoryRepository.node(id)

    @Test
    fun `the 8 shipped scenes are all still there, in Chapter 17, with their titles, pictures and fight`() {
        assertEquals(8, Chapter17Shipped.scenes.size)
        val problems = Chapter17Shipped.scenes.flatMap { Chapter17Pin.sceneProblems(it, node(it.id)) }
        assertEquals("shipped scenes that changed", emptyList<String>(), problems)
        val c = Chapter17Shipped.chapter
        val actual = StoryRepository.chapters.single { it.id == c.id }
        assertEquals(c.number, actual.number)
        assertEquals(c.title, actual.title)
        assertEquals("start scene", c.start, actual.startNodeId)
    }

    @Test
    fun `the shipped prose is word for word`() {
        val changed = Chapter17Shipped.scenes.filter { Chapter17Pin.sha(node(it.id).narrativeText) != it.sha }.map { it.id }
        assertEquals("shipped scenes whose prose has changed", emptyList<String>(), changed)
    }

    @Test
    fun `every shipped choice keeps its label, effects, items, trophy and gates, and leads only where the list says`() {
        val problems = Chapter17Shipped.scenes.flatMap { Chapter17Pin.choiceProblems(it, node(it.id)) }.toMutableList()
        val unused = Chapter17Pin.plannedRetargets.keys - Chapter17Shipped.scenes.flatMap { s -> s.choices.map { "${s.id}|${it.label}" } }.toSet()
        if (unused.isNotEmpty()) problems += "retargets listed for choices that do not exist: $unused"
        assertEquals("shipped choices that no longer match", emptyList<String>(), problems)
    }

    @Test
    fun `the four approved retargets are the only shipped choices whose destination may change`() {
        assertEquals(
            setOf(
                "gathering_what_they_have_asked|Move out.",
                "gathering_what_they_have_moved|Move out.",
                "the_road_to_ashfall_again|Get to the chamber.",
                "chapter17_end|Go home."
            ),
            Chapter17Pin.plannedRetargets.keys
        )
        // every other shipped choice leads exactly where it led
        Chapter17Shipped.scenes.forEach { s ->
            s.choices.filter { "${s.id}|${it.label}" !in Chapter17Pin.plannedRetargets }.forEach { e ->
                assertEquals("${s.id}: ${e.label}", setOf(e.next), node(s.id).choices.filter { it.label == e.label }.map { it.nextNodeId }.toSet())
            }
        }
    }

    @Test
    fun `the fight, its routing and its enemy are as shipped`() {
        assertEquals(1, Chapter17Shipped.fights.size)
        Chapter17Shipped.fights.forEach { f ->
            val encounter = CombatRepository.encounter(f.id)
            assertEquals("${f.id}: encounter", f.shape, encounter.toString())
            assertEquals("${f.id}: enemy", f.enemy, EnemyCatalog.get(encounter.enemyId).toString())
        }
        // a win and a defeat still lead where they led: the frame, and the frame by way of the defeat scene
        val e = CombatRepository.encounter("chamber_guardian_encounter")
        assertEquals("what_must_be_broken", e.victoryNodeId)
        assertEquals("chamber_guardian_defeat", e.defeatNodeId)
        assertEquals("chamber_guardian_encounter", node("the_chamber_defended").combatEncounterId)
        assertTrue("the fight scene has no choices of its own", node("the_chamber_defended").choices.isEmpty())
    }

    @Test
    fun `Chapter 17 holds only the eight shipped scenes and the 25 the package plans`() {
        val planned = Chapter17Pin.plannedSceneIds
        assertEquals(25, planned.size)
        assertEquals("no id twice", planned.size, planned.toSet().size)
        assertTrue("a planned id is also a shipped id", planned.none { it in shippedIds })
        val nodes = StoryRepository.nodesInChapter("chapter_17")
        val foreign = nodes.filter { it.id !in shippedIds && it.id !in planned }.map { it.id }
        assertEquals("scenes in Chapter 17 that are neither shipped nor planned", emptyList<String>(), foreign)
        planned.mapNotNull { StoryRepository.nodeOrNull(it) }.forEach { assertEquals(it.id, "chapter_17", it.chapterId) }
    }

    @Test
    fun `no planned scene id or flag is used anywhere outside Chapter 17`() {
        val outside = StoryRepository.chapters.filter { it.id != "chapter_17" }.flatMap { StoryRepository.nodesInChapter(it.id) }
        assertEquals("a planned id is already a scene elsewhere", emptyList<String>(), outside.map { it.id }.filter { it in Chapter17Pin.plannedSceneIds })
        val leads = outside.flatMap { n -> n.choices.map { n.id to it } }.filter { (_, c) -> c.nextNodeId in Chapter17Pin.plannedSceneIds }
        assertEquals("a choice outside Chapter 17 leads to a planned scene", emptyList<String>(), leads.map { it.first })
        val flags = Chapter17Pin.plannedFlags.toSet()
        val touching = outside.flatMap { n ->
            n.choices.filter { c ->
                val r = c.requirements
                c.consequences.setFlags.any { it in flags } || c.consequences.clearFlags.any { it in flags } ||
                    (r?.requiredFlags ?: emptySet()).any { it in flags } || (r?.forbiddenFlags ?: emptySet()).any { it in flags }
            }.map { n.id }
        }
        assertEquals("a planned flag is already used elsewhere", emptyList<String>(), touching)
        assertEquals(5, flags.size)
    }

    // ---------------------------------------------------------------------------------------------
    // The pins notice damage, and accept the planned state (changed copies of real scenes; nothing in the story changes)
    // ---------------------------------------------------------------------------------------------

    private fun firstChoiceOf(id: String): Choice = node(id).choices.first()
    private fun withChoice(id: String, index: Int, change: (Choice) -> Choice): StoryNode {
        val n = node(id)
        return n.copy(choices = n.choices.mapIndexed { i, c -> if (i == index) change(c) else c })
    }

    private fun noticed(id: String, damaged: StoryNode, what: String) {
        val s = shipped(id)
        val problems = Chapter17Pin.sceneProblems(s, damaged) + Chapter17Pin.choiceProblems(s, damaged)
        assertTrue("$what went unnoticed in $id", problems.isNotEmpty())
    }

    @Test
    fun `the pins notice a reworded scene, a changed title or picture, a changed fight and a changed chapter`() {
        val n = node("the_signal")
        noticed("the_signal", n.copy(narrativeText = n.narrativeText.replace("finish it", "end it")), "a reworded line")
        noticed("the_signal", n.copy(narrativeText = n.narrativeText + " One more sentence."), "an added sentence")
        noticed("the_signal", n.copy(title = "The Sign"), "a changed title")
        noticed("the_signal", n.copy(illustrationId = "dwarven_hold_gate"), "a changed picture")
        noticed("the_signal", n.copy(chapterId = "chapter_18"), "a moved scene")
        val fight = node("the_chamber_defended")
        noticed("the_chamber_defended", fight.copy(combatEncounterId = "reliquary_thief_encounter"), "a different fight")
        noticed("the_chamber_defended", fight.copy(combatEncounterId = null), "a removed fight")
    }

    @Test
    fun `the pins notice a changed label, effect, flag, item, trophy, gate, clear or destination, and a stray choice`() {
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(label = "\"How long?\"") }, "a changed label")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(consequences = it.consequences.copy(statDeltas = mapOf(StatType.HONOUR to 1))) }, "a changed stat change")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(consequences = it.consequences.copy(statDeltas = emptyMap())) }, "a removed stat change")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(consequences = it.consequences.copy(setFlags = setOf("another_flag"))) }, "a changed flag")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(consequences = it.consequences.copy(clearFlags = setOf("c13_door_open"))) }, "a new clear on a shipped choice")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(nextNodeId = "gathering_what_they_have_moved") }, "an unapproved destination")
        noticed("the_signal", withChoice("the_signal", 0) { it.copy(lockedDisplay = LockedDisplay.SHOW_LOCKED) }, "a changed lock display")
        noticed("what_must_be_broken", withChoice("what_must_be_broken", 0) { it.copy(consequences = it.consequences.copy(grantItemIds = emptyList())) }, "a dropped item")
        noticed("what_must_be_broken", withChoice("what_must_be_broken", 0) { it.copy(consequences = it.consequences.copy(unlockTrophy = "A New Trophy")) }, "a new trophy")
        noticed("what_must_be_broken", withChoice("what_must_be_broken", 0) { it.copy(requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 14))) }, "a lowered stat gate")
        noticed("what_must_be_broken", withChoice("what_must_be_broken", 0) { it.copy(requirements = null) }, "a removed stat gate")
        noticed("what_must_be_broken", withChoice("what_must_be_broken", 2) { it.copy(nextNodeId = "the_chamber_defended") }, "a frame choice that leads elsewhere")
        noticed("chapter17_end", withChoice("chapter17_end", 0) { it.copy(nextNodeId = "c17_chain_gate") }, "a changed transition into Chapter 18")
        val road = node("the_road_to_ashfall_again")
        noticed("the_road_to_ashfall_again", road.copy(choices = road.choices + road.choices.first().copy(label = "Turn back.")), "a stray choice")
        val end = node("chapter17_end")
        noticed("chapter17_end", end.copy(choices = emptyList()), "a removed choice")
    }

    @Test
    fun `the pins accept the two Move out choices once they lead to the chain gate, and not before they are both whole`() {
        listOf("gathering_what_they_have_asked", "gathering_what_they_have_moved").forEach { id ->
            val s = shipped(id)
            assertEquals("$id as it is", emptyList<String>(), Chapter17Pin.choiceProblems(s, node(id)))
            val planned = withChoice(id, 0) { it.copy(nextNodeId = "c17_chain_gate") }
            assertEquals("$id retargeted", emptyList<String>(), Chapter17Pin.choiceProblems(s, planned))
            // a retarget that also keeps the old way, or goes somewhere else, or splits in two, is not the approved one
            val both = node(id).let { n -> n.copy(choices = n.choices + n.choices.first().copy(nextNodeId = "c17_chain_gate")) }
            assertTrue("$id: the old way kept beside the new", Chapter17Pin.choiceProblems(s, both).isNotEmpty())
            val elsewhere = withChoice(id, 0) { it.copy(nextNodeId = "c17_prepare") }
            assertTrue("$id: a different planned scene", Chapter17Pin.choiceProblems(s, elsewhere).isNotEmpty())
            val gated = withChoice(id, 0) { it.copy(nextNodeId = "c17_chain_gate", requirements = ChoiceRequirement(requiredFlags = setOf("began_training_new_wardens"))) }
            assertEquals("$id: a gate on the copy is a superset of the shipped gate, which was none", emptyList<String>(), Chapter17Pin.choiceProblems(s, gated))
        }
    }

    /**
     * The road's one shipped choice as it was before the rung copies (the label and the destination come from the pinned shipped data), so
     * that these tests hold both while the road is as shipped and once it carries the three copies.
     */
    private fun shippedRoadChoice(): Choice {
        val e = shipped("the_road_to_ashfall_again").choices.single()
        return Choice(label = e.label, nextNodeId = e.next)
    }

    @Test
    fun `the pins accept the end of Chapter 17 once it leads to the Chapter 18 gate, and nothing else`() {
        val s = shipped("chapter17_end")
        val n = node("chapter17_end")
        assertEquals("as it is", emptyList<String>(), Chapter17Pin.choiceProblems(s, n))
        assertEquals("the shipped destination is still acceptable", emptyList<String>(),
            Chapter17Pin.choiceProblems(s, withChoice("chapter17_end", 0) { it.copy(nextNodeId = "no_more_envoys") }))
        assertTrue("another scene", Chapter17Pin.choiceProblems(s, withChoice("chapter17_end", 0) { it.copy(nextNodeId = "c18_marks") }).isNotEmpty())
        assertTrue("both ways at once", Chapter17Pin.choiceProblems(s, n.copy(choices = n.choices + n.choices.first().copy(nextNodeId = "no_more_envoys"))).isNotEmpty())
        assertTrue("a changed label", Chapter17Pin.choiceProblems(s, withChoice("chapter17_end", 0) { it.copy(label = "Go back.") }).isNotEmpty())
    }

    private fun roadCopies(
        trusted: (Choice) -> Choice = { it },
        working: (Choice) -> Choice = { it },
        wary: (Choice) -> Choice = { it }
    ): StoryNode {
        val n = node("the_road_to_ashfall_again")
        val c = shippedRoadChoice()
        return n.copy(
            choices = listOf(
                trusted(c.copy(nextNodeId = "c17_road_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted")))),
                working(c.copy(nextNodeId = "c17_road_working", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working")))),
                wary(c.copy(nextNodeId = "c17_road_wary", requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))))
            )
        )
    }

    @Test
    fun `the pins accept the road choice as three exclusive rung copies, and nothing else`() {
        val s = shipped("the_road_to_ashfall_again")
        assertEquals("as it is", emptyList<String>(), Chapter17Pin.choiceProblems(s, node("the_road_to_ashfall_again")))
        assertEquals("the planned three copies", emptyList<String>(), Chapter17Pin.choiceProblems(s, roadCopies()))
        // each damage to the planned state is noticed
        val road = roadCopies()
        assertTrue("only two copies", Chapter17Pin.choiceProblems(s, road.copy(choices = road.choices.take(2))).isNotEmpty())
        assertTrue("no wary copy", Chapter17Pin.choiceProblems(s, road.copy(choices = road.choices.filter { it.nextNodeId != "c17_road_wary" })).isNotEmpty())
        assertTrue("a fourth copy to somewhere else", Chapter17Pin.choiceProblems(s, road.copy(choices = road.choices + road.choices.first().copy(nextNodeId = "c17_approach"))).isNotEmpty())
        assertTrue("the working copy has no gate", Chapter17Pin.choiceProblems(s, roadCopies(working = { it.copy(requirements = null) })).isNotEmpty())
        assertTrue("the working copy also forbids nothing and needs trusted", Chapter17Pin.choiceProblems(s, roadCopies(working = { it.copy(requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))) })).isNotEmpty())
        assertTrue("the wary copy does not forbid trusted", Chapter17Pin.choiceProblems(s, roadCopies(wary = { it.copy(requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working"))) })).isNotEmpty())
        assertTrue("the wary copy needs a flag", Chapter17Pin.choiceProblems(s, roadCopies(wary = { it.copy(requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_wary"), forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))) })).isNotEmpty())
        assertTrue("a copy with an effect added", Chapter17Pin.choiceProblems(s, roadCopies(trusted = { it.copy(consequences = it.consequences.copy(setFlags = setOf("c7_rung_trusted"))) })).isNotEmpty())
        assertTrue("a copy that clears", Chapter17Pin.choiceProblems(s, roadCopies(trusted = { it.copy(consequences = it.consequences.copy(clearFlags = setOf("c7_rung_wary"))) })).isNotEmpty())
        assertTrue("a copy with a stat gate", Chapter17Pin.choiceProblems(s, roadCopies(wary = { it.copy(requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"), minStats = mapOf(StatType.HONOUR to 1))) })).isNotEmpty())
        assertTrue("the shipped destination kept beside the copies", Chapter17Pin.choiceProblems(s, road.copy(choices = road.choices + shippedRoadChoice())).isNotEmpty())
    }

    @Test
    fun `exactly one of the three road copies shows for every rung a save can hold, including none and the two in the wrong order`() {
        val road = roadCopies()
        fun shown(flags: Set<String>) = StoryRepository.visibleChoices(road, GameState(flags = flags)).map { it.nextNodeId }
        assertEquals(listOf("c17_road_wary"), shown(emptySet()))
        assertEquals(listOf("c17_road_wary"), shown(setOf("c7_rung_wary")))
        assertEquals(listOf("c17_road_working"), shown(setOf("c7_rung_working")))
        assertEquals(listOf("c17_road_trusted"), shown(setOf("c7_rung_trusted")))
        assertEquals("one rung and unrelated flags", listOf("c17_road_trusted"), shown(setOf("c7_rung_trusted", "c13_door_open", "began_training_new_wardens")))
        // a save that somehow holds two rungs would show two copies; the family is exclusive, which Chapter17FamilyProofTest proves
        assertEquals(2, shown(setOf("c7_rung_trusted", "c7_rung_working")).size)
    }
}

/** The pins as pure functions of a scene, so that changed copies of real scenes can be put through them. */
internal object Chapter17Pin {
    /** Choices whose destination may change, as "scene|label" -> every destination its copies lead to in the planned state. */
    val plannedRetargets: Map<String, Set<String>> = mapOf(
        "gathering_what_they_have_asked|Move out." to setOf("c17_chain_gate"),
        "gathering_what_they_have_moved|Move out." to setOf("c17_chain_gate"),
        "the_road_to_ashfall_again|Get to the chamber." to setOf("c17_road_trusted", "c17_road_working", "c17_road_wary"),
        // the Chapter 18-20 expansion: the end of Chapter 17 now leads to the gate before `no_more_envoys`; label and effects are the shipped ones
        "chapter17_end|Go home." to setOf("c18_gate_after_ashfall")
    )
    private const val ROAD_KEY = "the_road_to_ashfall_again|Get to the chamber."

    /** The gates the three road copies carry in the planned state: destination -> (flags required, flags forbidden). */
    private val roadGates: Map<String, Pair<Set<String>, Set<String>>> = mapOf(
        "c17_road_trusted" to (setOf("c7_rung_trusted") to emptySet()),
        "c17_road_working" to (setOf("c7_rung_working") to emptySet()),
        "c17_road_wary" to (emptySet<String>() to setOf("c7_rung_working", "c7_rung_trusted"))
    )

    /** What a shipped choice may newly clear: nothing. The five Chapter 17 flags are cleared by new choices only. */
    private val allowedClears: Set<String> = emptySet()

    /** The 25 scenes the package adds: 18 authored, the second table, and six payoffs. */
    val plannedSceneIds: List<String> = listOf(
        "c17_chain_gate", "c17_wardens", "c17_watch", "c17_council", "c17_door_open", "c17_door_barred", "c17_prepare", "c17_prepare_second",
        "c17_stores", "c17_watch_site", "c17_watch_take", "c17_watch_leave", "c17_records", "c17_records_study", "c17_records_interrupt",
        "c17_road_trusted", "c17_road_working", "c17_road_wary", "c17_approach",
        "c17_pay_stores", "c17_pay_taken", "c17_pay_left", "c17_pay_arrangement", "c17_pay_interruption", "c17_pay_none"
    )

    /** The five hidden flags the package adds. */
    val plannedFlags: List<String> = listOf("c17_visited_stores", "c17_visited_watch", "c17_visited_records", "c17_took_guard", "c17_studied_junctions")

    fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()
    fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(collapse(text).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun stats(m: Map<StatType, Int>) = m.entries.sortedBy { it.key.name }.joinToString(",") { "${it.key.name}=${it.value}" }
    private fun sorted(s: Set<String>) = s.sorted().joinToString(",")
    private fun list(s: String) = s.split(",").filter { it.isNotEmpty() }

    fun sceneProblems(s: Chapter17Shipped.S, n: StoryNode): List<String> {
        val p = ArrayList<String>()
        if (n.chapterId != s.chapter) p += "${s.id}: moved to ${n.chapterId}"
        if (n.title != s.title) p += "${s.id}: title is now '${n.title}'"
        if (n.illustrationId != s.illustration) p += "${s.id}: picture is now '${n.illustrationId}'"
        if ((n.combatEncounterId ?: "") != s.combat) p += "${s.id}: fight is now '${n.combatEncounterId}'"
        if (sha(n.narrativeText) != s.sha) p += "${s.id}: prose changed"
        return p
    }

    fun choiceProblems(s: Chapter17Shipped.S, actual: StoryNode): List<String> {
        val problems = ArrayList<String>()
        val accounted = ArrayList<Choice>()
        s.choices.forEach { e ->
            val copies = actual.choices.filter { c ->
                c.label == e.label && stats(c.consequences.statDeltas) == e.deltas && sorted(c.consequences.setFlags) == e.set &&
                    c.consequences.grantItemIds.joinToString(",") == e.items && (c.consequences.unlockTrophy ?: "") == e.trophy
            }
            if (copies.isEmpty()) { problems += "${s.id}: '${e.label}' is gone or its effects changed"; return@forEach }
            accounted += copies
            copies.forEach { c ->
                val shippedClears = list(e.clear).toSet()
                if (c.consequences.clearFlags - allowedClears != shippedClears) problems += "${s.id}: '${e.label}' now clears ${c.consequences.clearFlags}"
                val req = c.requirements
                if (!(req?.requiredFlags ?: emptySet()).containsAll(list(e.required))) problems += "${s.id}: '${e.label}' lost a required flag"
                if (!(req?.forbiddenFlags ?: emptySet()).containsAll(list(e.forbidden))) problems += "${s.id}: '${e.label}' lost a forbidden flag"
                if (stats(req?.minStats ?: emptyMap()) != e.min) problems += "${s.id}: '${e.label}' changed its stat gate"
                if (c.lockedDisplay != LockedDisplay.HIDE || c.lockedHint != null) problems += "${s.id}: '${e.label}' changed how it shows when locked"
            }
            val destinations = copies.map { it.nextNodeId }.toSet()
            val key = "${s.id}|${e.label}"
            val planned = plannedRetargets[key]
            when {
                destinations == setOf(e.next) && copies.size == 1 -> Unit
                planned != null && destinations == planned -> {
                    if (planned.size == 1 && copies.size != 1) problems += "${s.id}: '${e.label}' has ${copies.size} copies, expected one"
                    if (key == ROAD_KEY) problems += roadProblems(copies)
                }
                else -> problems += "${s.id}: '${e.label}' leads to $destinations (${copies.size} copies), expected ${setOf(e.next)}" +
                    (if (planned != null) " or $planned" else "")
            }
        }
        val strays = actual.choices.filter { c -> accounted.none { it === c } }.map { it.label }
        if (strays.isNotEmpty()) problems += "${s.id}: choices that are not copies of a shipped choice: $strays"
        return problems
    }

    /** The planned road choice: three copies, each with exactly its rung gate and no other gate, and exactly one visible for any rung. */
    private fun roadProblems(copies: List<Choice>): List<String> {
        val p = ArrayList<String>()
        if (copies.size != 3) p += "the road choice has ${copies.size} copies, expected three"
        roadGates.forEach { (dest, gate) ->
            val c = copies.singleOrNull { it.nextNodeId == dest }
            if (c == null) { p += "no single copy leads to $dest"; return@forEach }
            val r = c.requirements
            if ((r?.requiredFlags ?: emptySet()) != gate.first) p += "$dest: requires ${r?.requiredFlags}, expected ${gate.first}"
            if ((r?.forbiddenFlags ?: emptySet()) != gate.second) p += "$dest: forbids ${r?.forbiddenFlags}, expected ${gate.second}"
            if (!r?.minStats.isNullOrEmpty()) p += "$dest: has a stat gate"
            if (c.consequences != com.thelastjailer.app.Consequences()) p += "$dest: has effects"
        }
        listOf(emptySet(), setOf("c7_rung_wary"), setOf("c7_rung_working"), setOf("c7_rung_trusted")).forEach { flags ->
            val shown = copies.filter { it.requirements?.isSatisfiedBy(GameState(flags = flags)) ?: true }
            if (shown.size != 1) p += "with rung flags $flags ${shown.size} copies show"
        }
        return p
    }
}
