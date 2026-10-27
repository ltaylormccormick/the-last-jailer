package com.thelastjailer.app.data

import com.thelastjailer.app.StoryNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House style for the scenes Chapters 10 to 12 added: no dashes, no "Not X." fragments, nothing the story has not introduced
 * yet, no "lad" before Halvard's last words, no pronoun for the Marshal, no reason for Voss leaving the Order. And the rules of
 * the death: nothing before the fall speaks of it, and the shipped ending keeps the word it ends on.
 */
class Chapter10to12ProseTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private val ch10 get() = StoryRepository.nodesInChapter("chapter_10").filter { it.id.startsWith("c10_") }
    private val ch11 get() = StoryRepository.nodesInChapter("chapter_11").filter { it.id.startsWith("c11_") }
    private val ch12 get() = StoryRepository.nodesInChapter("chapter_12").filter { it.id.startsWith("c12_") }
    private val newScenes get() = ch10 + ch11 + ch12

    private fun prose(nodes: List<StoryNode>) =
        nodes.flatMap { n -> listOf(n.id to collapse(n.narrativeText)) + n.choices.map { n.id to it.label } }

    private fun hits(nodes: List<StoryNode>, terms: List<String>) = prose(nodes).flatMap { (id, t) ->
        terms.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
    }

    @Test
    fun `new scenes have no dashes`() {
        val bad = prose(newScenes).filter { (_, t) -> t.contains('—') || t.contains('–') }.map { it.first }.distinct()
        assertTrue("Dashes in: $bad", bad.isEmpty())
    }

    @Test
    fun `no sentence in a new scene opens with a Not fragment`() {
        val fragment = Regex("(^|[.!?][\"']?\\s)Not\\s")
        val bad = prose(newScenes).filter { (_, t) -> fragment.containsMatchIn(t) }.map { it.first }.distinct()
        assertTrue("Not-fragments in: $bad", bad.isEmpty())
    }

    @Test
    fun `new scenes name nothing the story has not introduced yet`() {
        val reserved = listOf("Ashwell", "archivist", "Selvane", "Duskmere", "Wraithspire", "inquisitor", "Emberlow")
        assertTrue("Reserved terms: ${hits(newScenes, reserved)}", hits(newScenes, reserved).isEmpty())
        // Fenmoor and Thessaly arrive in Chapter 11, at the road and at the drowned ward
        val early = hits(ch10, listOf("Fenmoor", "Thessaly"))
        assertTrue("Chapter 11 names in Chapter 10: $early", early.isEmpty())
    }

    @Test
    fun `Halvard says lad only in his last words`() {
        val bad = prose(newScenes).filter { (_, t) -> Regex("\\blad\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { it.first }
        assertTrue("lad in: $bad", bad.isEmpty())
    }

    @Test
    fun `no sentence about the Marshal in a new scene uses a pronoun for them`() {
        val pronoun = Regex("\\b(he|she|him|his|her|hers|himself|herself)\\b", RegexOption.IGNORE_CASE)
        val bad = prose(newScenes).flatMap { (id, t) ->
            t.split(Regex("(?<=[.!?])\\s+")).filter { it.contains("Marshal") && pronoun.containsMatchIn(it) }.map { "$id: $it" }
        }
        assertTrue("Marshal pronouns: $bad", bad.isEmpty())
    }

    @Test
    fun `Voss gives no reason for leaving the Order in any new scene`() {
        val text = newScenes.joinToString(" ") { collapse(it.narrativeText) }
        val inventions = listOf("because the Order", "left the Order because", "she left because", "her reason was", "the reason she left")
        assertTrue(inventions.none { text.contains(it, ignoreCase = true) })
    }

    @Test
    fun `nothing before the fall speaks of the death, so no choice can seem to promise otherwise`() {
        val beforeFall = ch10.filter { it.id !in setOf("c10_carry_bridge", "c10_carry_voss", "c10_carry_stair", "c10_carry_stair_edda",
            "c10_carry_forge", "c10_earth_name", "c10_earth_deeds", "c10_earth_plain", "c10_earth_bare") }
        assertEquals(30, beforeFall.size)
        val words = listOf("die", "dies", "died", "dying", "dead", "death", "grave", "buried", "burial", "funeral", "rites", "farewell",
            "goodbye", "mourn", "pyre", "last words", "never see")
        assertTrue("Death words before the fall: ${hits(beforeFall, words)}", hits(beforeFall, words).isEmpty())
    }

    @Test
    fun `stays is kept for the shipped ending, and the new scenes before it do not use it`() {
        val bad = hits(ch10, listOf("stays"))
        assertTrue("stays in: $bad", bad.isEmpty())
        assertTrue(collapse(StoryRepository.node("chapter10_end").narrativeText).contains("She just stays"))
    }

    @Test
    fun `the new scenes are all real`() {
        newScenes.forEach { n ->
            assertTrue(n.id, collapse(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty())
        }
    }
}
