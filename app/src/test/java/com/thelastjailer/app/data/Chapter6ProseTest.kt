package com.thelastjailer.app.data

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 6's prose. The ten scenes that shipped and were not meant to change are pinned word for word (a SHA-256 of the
 * whitespace-collapsed text, as for Chapter 5), the two road scenes keep their first paragraph, and every scene and label
 * in the chapter keeps to the house style: no dashes, no "Not X." fragments, no terms the story has not introduced yet,
 * no "lad" before Chapter 10, and no pronoun for the Marshal. The new scenes also never mention Voss, so no history for
 * her is invented here.
 */
class Chapter6ProseTest {

    private fun collapse(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(collapse(text).toByteArray()).joinToString("") { "%02x".format(it) }

    private val preserved = mapOf(
        "the_choice_of_roads" to "6e1923ff6f1315e2b208c0e4810125020a5d65d63ae890e3b24030762b00e13e",
        "the_second_gate" to "f34e2769ae9295413a8d00db330c0889f595114af44d38ec855e858277a03bfd",
        "what_the_warden_left_knelt" to "ddf5b600a5fac33c30faaf9f6afa5786ff9b3a3a2cc689c7ac52abe93ff3f2fe",
        "what_the_warden_left_searched" to "572af6eea0ce5f7662f18300cfae57035b276ae23a2d98e2035b5f9c8e24c52a",
        "the_unbound" to "a85bf04ca3f240e0175f890ebb370cff9e2a605fede5ab64052972b3025ad4ec",
        "unbound_defeat" to "0b5fdf6f637ccf1f0c220afb44cd745ad22ff2bf6a174cbd05561be55c045bf5",
        "what_must_be_decided" to "e16ebcca1f916afadd2cad84fbf9175d6b84f4af57633b9aab699beac3736d8a",
        "chapter6_end_sealed" to "245c6b46043b1ff4e82d0e360c5226e0ad00c1672e459a4458138c43a8ff8daa",
        "chapter6_end_reported" to "489983e4580cb75f14249a57ee3e675ca06f135e8844d36ac069c4ba591cb796",
        "chapter6_end_left" to "e669d5cd6e2111442a6ba56c78ed1ac4da9e63f68d24cfc1aaff39faf7553d28"
    )

    @Test
    fun `the ten scenes that were not meant to change are word for word as shipped`() {
        val moved = preserved.filter { (id, hash) -> sha(StoryRepository.node(id).narrativeText) != hash }.keys
        assertTrue("Changed scenes: $moved", moved.isEmpty())
    }

    @Test
    fun `the two road scenes keep their first paragraph and hand the arrival to Greymoor's own scene`() {
        val volunteered = collapse(StoryRepository.node("the_road_north_volunteered").narrativeText)
        assertTrue(volunteered.startsWith("He leaves before he lets himself think better of it, and the world above ground feels"))
        assertTrue(volunteered.contains("never quite catches up with him."))
        val deliberate = collapse(StoryRepository.node("the_road_north_deliberate").narrativeText)
        assertTrue(deliberate.startsWith("He spends the better part of a day with Halvard's maps first"))
        assertTrue(deliberate.contains("even if nothing else about it is."))
        listOf(volunteered, deliberate).forEach {
            assertTrue("the arrival now belongs to c6_arrive_greymoor", !it.contains("Three days north, the road ends"))
        }
        assertTrue(collapse(StoryRepository.node("c6_arrive_greymoor").narrativeText).startsWith("Three days north, the road ends at a village"))
    }

    private val chapterScenes get() = StoryRepository.nodesInChapter("chapter_6")
    private val newScenes get() = chapterScenes.filter { it.id.startsWith("c6_") }
    private fun prose(nodes: List<com.thelastjailer.app.StoryNode>) =
        nodes.flatMap { n -> listOf(n.id to collapse(n.narrativeText)) + n.choices.map { n.id to it.label } }

    @Test
    fun `the chapter has no dashes in its scenes or choice labels`() {
        val bad = prose(chapterScenes).filter { (_, t) -> t.contains('—') || t.contains('–') }.map { it.first }.distinct()
        assertTrue("Dashes in: $bad", bad.isEmpty())
    }

    @Test
    fun `no sentence in the chapter opens with a Not fragment`() {
        val fragment = Regex("(^|[.!?][\"']?\\s)Not\\s")
        val bad = prose(chapterScenes).filter { (_, t) -> fragment.containsMatchIn(t) }.map { it.first }.distinct()
        assertTrue("Not-fragments in: $bad", bad.isEmpty())
    }

    @Test
    fun `the chapter names nothing the story has not introduced yet`() {
        val reserved = listOf(
            "settings", "seventh door", "frame", "Ashwell", "archivist", "Selvane", "Emberlow", "Fenmoor", "Duskmere",
            "Ordrun", "Ashfall", "sanctum", "Wraithspire", "Thessaly"
        )
        val bad = prose(chapterScenes).flatMap { (id, t) ->
            reserved.filter { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { "$id: $it" }
        }
        assertTrue("Reserved terms: $bad", bad.isEmpty())
    }

    @Test
    fun `Halvard says lad only in Chapter 10`() {
        val bad = prose(chapterScenes).filter { (_, t) -> Regex("\\blad\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) }.map { it.first }
        assertTrue("lad in: $bad", bad.isEmpty())
    }

    @Test
    fun `no sentence about the Marshal in the new scenes uses a pronoun for them`() {
        val pronoun = Regex("\\b(he|she|him|his|her|hers|himself|herself)\\b", RegexOption.IGNORE_CASE)
        val bad = prose(newScenes).flatMap { (id, t) ->
            t.split(Regex("(?<=[.!?])\\s+")).filter { it.contains("Marshal") && pronoun.containsMatchIn(it) }.map { "$id: $it" }
        }
        assertTrue("Marshal pronouns: $bad", bad.isEmpty())
    }

    @Test
    fun `the new scenes invent no history for Voss`() {
        val bad = prose(newScenes).filter { (_, t) -> t.contains("Voss") }.map { it.first }.distinct()
        assertTrue("Voss named in: $bad", bad.isEmpty())
    }

    @Test
    fun `the new scenes are all real scenes with text and a way on`() {
        assertEquals(29, newScenes.size)
        newScenes.forEach { n ->
            assertTrue(n.id, collapse(n.narrativeText).split(" ").size >= 25)
            assertTrue(n.id, n.choices.isNotEmpty() || n.combatEncounterId != null)
            assertEquals(n.id, "chapter_6", n.chapterId)
        }
    }
}
