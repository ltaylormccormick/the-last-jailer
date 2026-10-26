package com.thelastjailer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House style for the scenes Chapter 7 added: no dashes, no "Not X." fragments, nothing the story has not introduced yet
 * (Chapter VIII brings the frame, the settings and the seventh door; Chapter IX brings Ordrun and Emberlow; the Order's
 * ranks and the later places come much later), no "lad" before Chapter 10, no pronoun for the Marshal. The forge
 * scenes happen before Voss names Ashfall, so they do not name it either. No scene invents a reason for Voss leaving the Order.
 */
class Chapter7ProseTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private val newScenes get() = StoryRepository.nodesInChapter("chapter_7").filter { it.id.startsWith("c7_") }
    private val prelude get() = newScenes.filter {
        it.id.startsWith("c7_forge") || it.id.startsWith("c7_trade") || it.id.startsWith("c7_letter") ||
            it.id == "c7_edda_promise" || it.id in setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")
    }

    private fun prose(nodes: List<com.thelastjailer.app.StoryNode>) =
        nodes.flatMap { n -> listOf(n.id to collapse(n.narrativeText)) + n.choices.map { n.id to it.label } }

    @Test
    fun `the chapter adds thirty-two scenes, all of them real`() {
        assertEquals(15 + 32, StoryRepository.nodesInChapter("chapter_7").size)
        newScenes.forEach { n ->
            assertTrue(n.id, collapse(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty())
            assertEquals(n.id, "chapter_7", n.chapterId)
        }
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
        val reserved = listOf(
            "settings", "seventh door", "frame", "Ashwell", "archivist", "Selvane", "Emberlow", "Fenmoor", "Duskmere",
            "Ordrun", "Wraithspire", "Thessaly", "inquisitor", "castellan"
        )
        val bad = prose(newScenes).flatMap { (id, t) ->
            reserved.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Reserved terms: $bad", bad.isEmpty())
    }

    @Test
    fun `the forge scenes happen before Voss names Ashfall or the Sanctum`() {
        val bad = prose(prelude).flatMap { (id, t) ->
            listOf("Ashfall", "Sanctum").filter { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Early names: $bad", bad.isEmpty())
        assertEquals(13, prelude.size)
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
    fun `Voss gives no reason for leaving the Order in any new scene`() {
        // She refuses to give one at the hearth. The scenes may say she will not, never what it was.
        val text = newScenes.joinToString(" ") { collapse(it.narrativeText) }
        assertTrue(text.contains("I will not tell you my reasons for leaving"))
        val inventions = listOf("because the Order", "left the Order because", "she left because", "her reason was", "the reason she left")
        assertTrue(inventions.none { text.contains(it, ignoreCase = true) })
    }
}
