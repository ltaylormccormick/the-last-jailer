package com.thelastjailer.app.data

import com.thelastjailer.app.StoryNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House style for the scenes Chapters 13 to 16 added, and the boundaries the author set: no dashes, no "Not X." fragments, nothing the
 * story has not introduced yet, no "lad" (Halvard's word), no count of riders, fragments or years, no reason for Voss leaving the Order
 * and no word of who kept the chapel, nothing of what the prisoner is or who cut the six wards apart, and nothing that resolves
 * Ilsevet's conflict.
 */
class Chapter1316ProseTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private fun added(chapter: String, prefix: String) = StoryRepository.nodesInChapter(chapter).filter { it.id.startsWith(prefix) }
    private val ch13 get() = added("chapter_13", "c13_")
    private val ch14 get() = added("chapter_14", "c14_")
    private val ch15 get() = added("chapter_15", "c15_")
    private val ch16 get() = added("chapter_16", "c16_")
    private val newScenes get() = ch13 + ch14 + ch15 + ch16

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
        val reserved = listOf("archivist", "Duskmere", "Wraithspire", "Emberlow", "Sundering")
        assertTrue("Reserved terms: ${hits(newScenes, reserved)}", hits(newScenes, reserved).isEmpty())
        // Ashwell is named in "The Woman Before Ilsevet", the first scene of Chapter 15 that says what Ilsevet was before
        val early = hits(ch13 + ch14, listOf("Ashwell")) + hits(ch15.filter { it.id.startsWith("c15_waiting_") }, listOf("Ashwell"))
        assertTrue("Ashwell before the file names it: $early", early.isEmpty())
    }

    @Test
    fun `Halvard says lad only in his last words`() {
        val bad = prose(newScenes).filter { (_, t) -> Regex("\\blad\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { it.first }
        assertTrue("lad in: $bad", bad.isEmpty())
    }

    @Test
    fun `the new scenes count no riders, no fragments and no years`() {
        val counts = listOf(
            "three riders", "four riders", "five riders", "two riders", "fifteen years", "eleven years", "twelve years", "ten years",
            "four fragments", "three fragments", "two fragments", "four shards", "three shards", "two shards", "three keepers", "four settings"
        )
        assertTrue("Counts: ${hits(newScenes, counts)}", hits(newScenes, counts).isEmpty())
    }

    @Test
    fun `Voss gives no reason for leaving the Order, and says nothing of who kept the chapel`() {
        val text = newScenes.joinToString(" ") { collapse(it.narrativeText) }
        val inventions = listOf(
            "because the Order", "left the Order because", "she left because", "her reason was", "the reason she left", "I left the Order",
            "who kept this place", "I kept this place", "the mark was mine", "my mark", "I served", "I was an Inquisitor"
        )
        assertTrue("Reasons for leaving: ${inventions.filter { text.contains(it, ignoreCase = true) }}", inventions.none { text.contains(it, ignoreCase = true) })
        // the one thing she gives about the chapel is that it mattered and that she hoped it still stood
        val chapel = collapse(StoryRepository.node("c13_night_chapel").narrativeText)
        assertTrue(chapel.contains("I had hoped it would still be standing. It was."))
    }

    @Test
    fun `the prisoner is not told what it is, and the six wards are not told what they were`() {
        val forbidden = listOf(
            "one being", "were one", "not always six", "torn", "severed", "cut apart", "my name", "what I am", "who I was", "who I am",
            "I was one", "the whole", "originally", "first of us"
        )
        val bad = hits(ch14, forbidden)
        assertTrue("Identity of the prisoner or the six: $bad", bad.isEmpty())
    }

    @Test
    fun `Ilsevet's conflict is not resolved by any new scene`() {
        val resolving = listOf("forgave her", "forgive her", "she was right", "Ilsevet was right", "he forgives", "the case is closed", "no longer a threat", "surrendered")
        val bad = hits(ch15 + ch16, resolving)
        assertTrue("Resolutions: $bad", bad.isEmpty())
    }

    @Test
    fun `the new scenes are all real`() {
        newScenes.forEach { n ->
            assertTrue(n.id, collapse(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty())
        }
        assertEquals(29 + 12 + 9 + 6, newScenes.size)
    }
}
