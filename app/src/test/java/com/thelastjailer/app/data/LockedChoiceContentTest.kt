package com.thelastjailer.app.data

import com.thelastjailer.app.LockedDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard rails for the real story. Showing a locked choice is a deliberate, reviewed decision: it can
 * reveal that an option exists. So the set of opted-in choices is an explicit allow-list here, and
 * anything that opts in must have an authored, spoiler-safe hint if it is gated by a flag.
 */
class LockedChoiceContentTest {

    private val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val storyFlags: Set<String> = nodes.flatMap { it.choices }.flatMap { it.consequences.setFlags }.toSet()

    /** (nodeId, label) of every choice approved to show while locked. Empty until the pilot is approved. */
    private val approvedLockedChoices: Set<Pair<String, String>> = emptySet()

    private val optedIn = nodes.flatMap { n -> n.choices.filter { it.lockedDisplay == LockedDisplay.SHOW_LOCKED }.map { n to it } }

    @Test
    fun `only approved choices opt in to being shown locked`() {
        val actual = optedIn.map { (n, c) -> n.id to c.label }.toSet()
        assertEquals("Opt-ins must match the approved list exactly", approvedLockedChoices, actual)
    }

    @Test
    fun `an opted-in choice must actually have a requirement`() {
        optedIn.forEach { (n, c) -> assertTrue("${n.id}: '${c.label}' opts in but has no requirement", c.requirements != null) }
    }

    @Test
    fun `an opted-in choice gated by a flag has an authored hint that does not leak the flag`() {
        optedIn.forEach { (n, c) ->
            val req = c.requirements!!
            val flagGated = req.requiredFlags.isNotEmpty() || req.forbiddenFlags.isNotEmpty()
            if (flagGated) {
                val hint = c.lockedHint
                assertTrue("${n.id}: '${c.label}' is flag-gated and needs a lockedHint", !hint.isNullOrBlank())
                assertFalse("${n.id}: hint contains an identifier: $hint", hint!!.contains('_'))
                (req.requiredFlags + req.forbiddenFlags + storyFlags).forEach { flag ->
                    assertFalse("${n.id}: hint repeats flag '$flag'", hint.contains(flag, ignoreCase = true))
                }
            }
        }
    }

    @Test
    fun `no choice carries a hint without opting in`() {
        // A stray hint on a hidden choice would be dead text that suggests it is shown somewhere.
        nodes.flatMap { it.choices }.forEach { c ->
            assertTrue("'${c.label}' has a lockedHint but is hidden when locked", c.lockedHint == null || c.lockedDisplay == LockedDisplay.SHOW_LOCKED)
        }
    }

    @Test
    fun `the two Chapter 3 pilot choices are still hidden pending approval and have the documented gates`() {
        val door = nodes.single { it.id == "a_choice_at_the_door" }
        val stand = door.choices.single { it.label.startsWith("\"Stand aside yourself") }
        val mercy = door.choices.single { it.label.startsWith("\"There's a man alive") }
        assertEquals(LockedDisplay.HIDE, stand.lockedDisplay)
        assertEquals(LockedDisplay.HIDE, mercy.lockedDisplay)
        assertEquals(mapOf(com.thelastjailer.app.StatType.COURAGE to 3), stand.requirements!!.minStats)
        assertEquals(mapOf(com.thelastjailer.app.StatType.HONOUR to 3), mercy.requirements!!.minStats)
        assertTrue(stand.requirements!!.requiredFlags.isEmpty() && mercy.requirements!!.requiredFlags.isEmpty())
    }
}
