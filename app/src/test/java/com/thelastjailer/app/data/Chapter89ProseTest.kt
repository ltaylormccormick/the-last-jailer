package com.thelastjailer.app.data

import com.thelastjailer.app.StoryNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House style for the scenes Chapters 8 and 9 added: no dashes, no "Not X." fragments, nothing the story has not introduced
 * yet (the frame, the settings and the seventh door arrive with the recon at the Sanctum; Ordrun and the castellan arrive in
 * Chapter IX; the Order's ranks and the later places come much later), no "lad" before Chapter 10, no pronoun for the Marshal.
 * No scene invents a reason for Voss leaving the Order, or says more of the chapel than she did.
 */
class Chapter89ProseTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private val ch8 get() = StoryRepository.nodesInChapter("chapter_8").filter { it.id.startsWith("c8_") }
    private val ch9 get() = StoryRepository.nodesInChapter("chapter_9").filter { it.id.startsWith("c9_") }
    private val newScenes get() = ch8 + ch9

    private fun prose(nodes: List<StoryNode>) =
        nodes.flatMap { n -> listOf(n.id to collapse(n.narrativeText)) + n.choices.map { n.id to it.label } }

    @Test
    fun `Chapter 8 adds twenty-four scenes and Chapter 9 thirty-nine, all of them real`() {
        assertEquals(10 + 24, StoryRepository.nodesInChapter("chapter_8").size)
        assertEquals(12 + 39, StoryRepository.nodesInChapter("chapter_9").size)
        newScenes.forEach { n ->
            assertTrue(n.id, collapse(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty())
        }
        ch8.forEach { assertEquals(it.id, "chapter_8", it.chapterId) }
        ch9.forEach { assertEquals(it.id, "chapter_9", it.chapterId) }
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
        val reserved = listOf("Ashwell", "archivist", "Selvane", "Fenmoor", "Duskmere", "Wraithspire", "Thessaly", "inquisitor")
        val bad = prose(newScenes).flatMap { (id, t) ->
            reserved.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Reserved terms: $bad", bad.isEmpty())
    }

    @Test
    fun `the road and the camp say nothing of the frame or the castellan before the recon`() {
        val beforeRecon = ch8.filter { it.id in setOf(
            "c8_ash_camp", "c8_road_hub_one", "c8_road_hub_two", "c8_deserter", "c8_deserter_paid", "c8_deserter_talked",
            "c8_deserter_scared", "c8_chapel", "c8_chapel_rested", "c8_chapel_door", "c8_cart", "c8_cart_watched", "c8_cart_robbed"
        ) }
        assertEquals(13, beforeRecon.size)
        val early = listOf("frame", "settings", "seventh door", "Ordrun", "castellan", "Emberlow")
        val bad = prose(beforeRecon).flatMap { (id, t) ->
            early.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Early names: $bad", bad.isEmpty())
    }

    @Test
    fun `Chapter 8 does not name Ordrun or Emberlow, which Chapter 9 brings`() {
        val bad = prose(ch8).flatMap { (id, t) ->
            listOf("Ordrun", "castellan", "Emberlow").filter { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Chapter 9 names in Chapter 8: $bad", bad.isEmpty())
    }

    @Test
    fun `Halvard says lad only in Chapter 10`() {
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
    fun `Voss gives no reason for leaving the Order and no fact about the chapel in any new scene`() {
        val text = newScenes.joinToString(" ") { collapse(it.narrativeText) }
        val inventions = listOf("because the Order", "left the Order because", "she left because", "her reason was", "the reason she left")
        assertTrue(inventions.none { text.contains(it, ignoreCase = true) })
        // The chapel stays hers to explain: the scenes may say she will not, never what it was.
        val chapel = (ch8 + ch9).filter { it.id.contains("chapel") }
        val facts = listOf("school", "orphan", "taught her", "raised her", "was a prison", "was a convent")
        chapel.forEach { n -> assertTrue(n.id, facts.none { collapse(n.narrativeText).contains(it, ignoreCase = true) }) }
        assertTrue(text.contains("I know who kept this place"))
    }

    @Test
    fun `Ordrun is never given a history Chapter 9 cannot support`() {
        val text = ch9.joinToString(" ") { collapse(it.narrativeText) }
        // Everything Halvard says of Ordrun rests on a name on a roll: the rank, the twin blades, and the errand he runs.
        assertTrue(text.contains("Twin blades, and he finishes with the second"))
        val inventions = listOf("Ordrun was born", "Ordrun's family", "Ordrun's wife", "Ordrun's son", "Ordrun's brother", "Ordrun swore")
        assertTrue(inventions.none { text.contains(it, ignoreCase = true) })
    }
}
