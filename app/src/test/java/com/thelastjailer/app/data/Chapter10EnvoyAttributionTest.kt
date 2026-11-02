package com.thelastjailer.app.data

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 10 twice called the Marshal's envoy "Voss's envoy" when Ilsevet walks into Stonebeard's gate alone. The
 * envoy at the black door in Chapter 5 was the Marshal's, not Voss's. The correction is exactly two words in two
 * scenes; these tests pin that nothing else in either scene moved.
 */
class Chapter10EnvoyAttributionTest {

    private val scenes = mapOf(
        "ilsevet_arrives_fortified" to "7c1850cdf80191f7b4e50f49a0bdb309220d0fb860b65dd4e0cba82e506e44cc",
        "ilsevet_arrives_hard" to "5f1821861d1005dc2a0d513bb66d1ed0bc613167bd41a86ea18ffe2aebe2607b"
    )

    private fun fingerprint(text: String): String {
        val normal = text.replace(Regex("\\s+"), " ").trim()
        return MessageDigest.getInstance("SHA-256").digest(normal.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `both arrivals name the Marshal's envoy`() {
        scenes.keys.forEach { id ->
            val text = StoryRepository.node(id).narrativeText.replace(Regex("\\s+"), " ")
            assertEquals(id, 1, Regex("the Marshal's envoy never quite managed").findAll(text).count())
            assertFalse(id, text.contains("Voss's envoy"))
        }
    }

    @Test
    fun `no scene or choice in the story calls the envoy Voss's`() {
        val everything = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        assertTrue(everything.none { n -> n.narrativeText.contains("Voss's envoy") || n.choices.any { it.label.contains("Voss's envoy") } })
    }

    @Test
    fun `undoing the two replacements reproduces the original scenes word for word`() {
        scenes.forEach { (id, original) ->
            val restored = StoryRepository.node(id).narrativeText.replace("the Marshal's envoy never", "Voss's envoy never")
            assertEquals(id, original, fingerprint(restored))
        }
    }

    @Test
    fun `the correction is exactly two occurrences in the whole chapter`() {
        val total = StoryRepository.nodesInChapter("chapter_10").sumOf { Regex("the Marshal's envoy").findAll(it.narrativeText.replace(Regex("\\s+"), " ")).count() }
        assertEquals(2, total)
    }
}
