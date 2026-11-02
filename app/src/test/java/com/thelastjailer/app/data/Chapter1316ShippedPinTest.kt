package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.StoryNode
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 41 scenes that shipped in Chapters 13 to 16 stay as they were. Their prose is pinned word for word (a SHA-256 of the
 * whitespace-collapsed text, as for Chapters 5 to 12), with one recorded exception, and every shipped choice keeps its label, its
 * stat changes, its flags, its items, its trophy and its gates. What may change is where a choice leads and what a replay clears,
 * and only as listed here. Fights, their routing and their enemies are pinned as they are in the combat catalogue. The expected
 * values are in [Chapter1316Shipped], generated at M0 from the sources at 6f1d69c.
 */
class Chapter1316ShippedPinTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(collapse(text).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    /**
     * The changes to shipped prose the author approved: scene -> (the new phrase, the phrase it replaces). The player can hold two
     * shards or three, never four, and the Seventh Door shard comes only from helping Ilsevet, so "Four fragments" becomes "Several
     * fragments", "between three keepers" becomes "between keepers", and the prisoner's warning (both versions) names "whatever came
     * away from the frame at Ashfall" where it said "the frame at Ashfall", a source and not a claim that Kaelen took a shard there.
     * The pin accepts each scene as shipped or with exactly its phrase changed, and nothing else.
     */
    private val approvedCorrections: Map<String, Pair<String, String>> = mapOf(
        "what_to_protect" to ("Several fragments" to "Four fragments"),
        "the_reliquary_thief_dispersed" to ("between keepers" to "between three keepers"),
        "the_prisoners_warning_pressed" to ("whatever came away from the frame at Ashfall" to "the frame at Ashfall"),
        "the_prisoners_warning_patient" to ("whatever came away from the frame at Ashfall" to "the frame at Ashfall")
    )

    /**
     * Choices whose destination changes, as "scene|label" -> every destination its copies lead to. Where a shipped choice is
     * replaced by gated copies of itself the copies lead to the new scenes; where it stays one choice it leads to one.
     */
    private val retargets: Map<String, Set<String>> = mapOf(
        // Chapter 13: the hearing between Voss's answer and the verdict, the ask at the stirrup, and the word from Fenmoor after the carving
        "what_voss_answers|\"I'll vouch for that myself.\"" to setOf("c13_hearing_vouched"),
        "what_voss_answers|Wait outside, and trust her account." to setOf("c13_hearing_account"),
        "selvanes_verdict|Let them ride out." to setOf("c13_parting"),
        "halvards_rites|Take a rubbing of the mark before you leave." to
            setOf("c13_thessaly_attended", "c13_thessaly_pledged", "c13_thessaly_silent"),
        // Chapter 14: the nights at the gate after Chapter 13, the two memories the stone keeps, and Voss after the vision
        "chapter13_end_training|Listen." to setOf("c14_gate_training"),
        "chapter13_end_watch|Listen." to setOf("c14_gate_watch"),
        "chapter13_end_deferred|Listen." to setOf("c14_gate_deferred"),
        "the_offered_glimpse|Let it in." to setOf("c14_knock_listened", "c14_knock_refused"),
        "what_kaelen_remembers|Tell Voss everything, tonight, whatever it costs to say out loud." to
            setOf("c14_voss_heard_trusted", "c14_voss_heard_working", "c14_voss_heard_wary"),
        "what_kaelen_remembers|Write it down instead. Some truths need to be chosen, not sprung on someone." to
            setOf("c14_voss_heard_trusted", "c14_voss_heard_working", "c14_voss_heard_wary"),
        "what_kaelen_remembers|Sit with it alone a while longer." to setOf("c14_voss_withheld"),
        // Chapter 15: how Kaelen asked, what else was in the case, Voss reading it, and Ashfall again
        "chapter14_end|Go on." to setOf("c15_waiting_open", "c15_waiting_private"),
        "the_woman_before_ilsevet_alone|Keep reading." to setOf("c15_the_reply"),
        "the_woman_before_ilsevet_with_voss|Keep reading." to setOf("c15_the_reply"),
        "what_it_made_of_her|Sit with that." to setOf("c15_echo_reached", "c15_echo_left", "c15_echo_refused"),
        // Chapter 16: a piece put in Voss's hand, and what the theft cost
        "what_to_protect|\"Split them between Stonebeard, Fenmoor, and Voss. No single loss ends this.\"" to
            setOf("c16_handing_trusted", "c16_handing_working", "c16_handing_wary"),
        "what_was_taken|Take stock of the damage." to setOf("c16_aftermath_open", "c16_aftermath_barred", "c16_aftermath_none")
    )

    /** What a shipped choice may newly clear: the telling flags of the chapters the package expands, so a replay starts each chapter clean. */
    private val allowedClears: Set<String> = C13_TELLING_FLAGS + C14_TELLING_FLAGS + C15_TELLING_FLAGS + C16_TELLING_FLAGS

    private val shippedIds: Set<String> = Chapter1316Shipped.scenes.map { it.id }.toSet()
    private val byChapter: Map<String, List<StoryNode>> = Chapter1316Shipped.chapters.associate { it.id to StoryRepository.nodesInChapter(it.id) }

    private fun stats(m: Map<com.thelastjailer.app.StatType, Int>) = m.entries.sortedBy { it.key.name }.joinToString(",") { "${it.key.name}=${it.value}" }
    private fun sorted(s: Set<String>) = s.sorted().joinToString(",")

    @Test
    fun `the 41 shipped scenes are all still there, in their chapters, with their titles, pictures and fights`() {
        assertEquals(41, Chapter1316Shipped.scenes.size)
        Chapter1316Shipped.scenes.forEach { s ->
            val n = StoryRepository.node(s.id)
            assertEquals("${s.id}: chapter", s.chapter, n.chapterId)
            assertEquals("${s.id}: title", s.title, n.title)
            assertEquals("${s.id}: picture", s.illustration, n.illustrationId)
            assertEquals("${s.id}: fight", s.combat, n.combatEncounterId ?: "")
        }
        Chapter1316Shipped.chapters.forEach { c ->
            val actual = StoryRepository.chapters.single { it.id == c.id }
            assertEquals(c.number, actual.number)
            assertEquals(c.title, actual.title)
            assertEquals("${c.id}: start scene", c.start, actual.startNodeId)
        }
    }

    @Test
    fun `the shipped prose is word for word, except the approved corrections`() {
        val changed = ArrayList<String>()
        Chapter1316Shipped.scenes.forEach { s ->
            val text = StoryRepository.node(s.id).narrativeText
            if (sha(text) == s.sha) return@forEach
            val fix = approvedCorrections[s.id]
            val collapsed = collapse(text)
            val ok = fix != null && collapsed.split(fix.first).size == 2 && sha(collapsed.replace(fix.first, fix.second)) == s.sha
            if (!ok) changed += s.id
        }
        assertEquals("shipped scenes whose prose has changed", emptyList<String>(), changed)
    }

    @Test
    fun `an approved correction changes only the approved phrase`() {
        approvedCorrections.forEach { (id, fix) ->
            val shipped = Chapter1316Shipped.scenes.single { it.id == id }
            val text = collapse(StoryRepository.node(id).narrativeText)
            val corrected = text.contains(fix.first)
            val original = sha(text) == shipped.sha
            assertTrue("$id is either as shipped or carries exactly the approved phrase", corrected != original)
            if (corrected) assertEquals(shipped.sha, sha(text.replace(fix.first, fix.second)))
        }
    }

    @Test
    fun `every shipped choice keeps its label, effects, items, trophy and gates, and leads only where the list says`() {
        val problems = ArrayList<String>()
        Chapter1316Shipped.scenes.forEach { s ->
            val actual = StoryRepository.node(s.id)
            val accounted = HashSet<Choice>()
            s.choices.forEach { e ->
                val copies = actual.choices.filter { c ->
                    c.label == e.label && stats(c.consequences.statDeltas) == e.deltas && sorted(c.consequences.setFlags) == e.set &&
                        c.consequences.grantItemIds.joinToString(",") == e.items && (c.consequences.unlockTrophy ?: "") == e.trophy
                }
                if (copies.isEmpty()) { problems += "${s.id}: '${e.label}' is gone or its effects changed"; return@forEach }
                accounted += copies
                copies.forEach { c ->
                    val addedClears = c.consequences.clearFlags - e.clear.split(",").filter { it.isNotEmpty() }.toSet() - allowedClears
                    if (addedClears.isNotEmpty()) problems += "${s.id}: '${e.label}' now also clears $addedClears"
                    val req = c.requirements
                    val required = req?.requiredFlags ?: emptySet()
                    val forbidden = req?.forbiddenFlags ?: emptySet()
                    if (!required.containsAll(e.required.split(",").filter { it.isNotEmpty() })) problems += "${s.id}: '${e.label}' lost a required flag"
                    if (!forbidden.containsAll(e.forbidden.split(",").filter { it.isNotEmpty() })) problems += "${s.id}: '${e.label}' lost a forbidden flag"
                    if (stats(req?.minStats ?: emptyMap()) != e.min) problems += "${s.id}: '${e.label}' changed its stat gate"
                }
                val destinations = copies.map { it.nextNodeId }.toSet()
                val key = "${s.id}|${e.label}"
                val allowed = retargets[key] ?: setOf(e.next)
                if (destinations != allowed) problems += "${s.id}: '${e.label}' leads to $destinations, expected $allowed"
            }
            val strays = actual.choices.filter { it !in accounted }.map { it.label }
            if (strays.isNotEmpty()) problems += "${s.id}: choices that are not copies of a shipped choice: $strays"
        }
        val unused = retargets.keys - Chapter1316Shipped.scenes.flatMap { s -> s.choices.map { "${s.id}|${it.label}" } }.toSet()
        if (unused.isNotEmpty()) problems += "retargets listed for choices that do not exist: $unused"
        assertEquals("shipped choices that no longer match", emptyList<String>(), problems)
    }

    @Test
    fun `the fights, their routing and their enemies are as shipped`() {
        Chapter1316Shipped.fights.forEach { f ->
            val encounter = CombatRepository.encounter(f.id)
            assertEquals("${f.id}: encounter", f.shape, encounter.toString())
            assertEquals("${f.id}: enemy", f.enemy, EnemyCatalog.get(encounter.enemyId).toString())
        }
        assertEquals(3, Chapter1316Shipped.fights.size)
    }

    @Test
    fun `the only scenes in Chapters 13 to 16 are the shipped ones and the ones the package adds`() {
        val prefix = mapOf("chapter_13" to "c13_", "chapter_14" to "c14_", "chapter_15" to "c15_", "chapter_16" to "c16_")
        byChapter.forEach { (chapter, nodes) ->
            val foreign = nodes.filter { it.id !in shippedIds && !it.id.startsWith(prefix.getValue(chapter)) }.map { it.id }
            assertEquals("$chapter: scenes that are neither shipped nor named for the package", emptyList<String>(), foreign)
            nodes.filter { it.id !in shippedIds }.forEach { assertEquals(it.id, chapter, it.chapterId) }
        }
    }
}
