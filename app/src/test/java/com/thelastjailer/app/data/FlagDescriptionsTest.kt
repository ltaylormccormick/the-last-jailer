package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Journal must read as Kaelen's story, never as the engine's memory: every story flag is either
 * given a line or deliberately hidden, nothing is spoiled early, old saves still work, and a replay
 * (which keeps old flags) never makes the Journal contradict itself.
 */
class FlagDescriptionsTest {

    private val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }

    private fun chapterNumber(chapterId: String) = chapterId.removePrefix("chapter_").toInt()

    /** flag -> earliest chapter whose choices set it. */
    private val setterChapter: Map<String, Int> = HashMap<String, Int>().also { m ->
        nodes.forEach { n ->
            n.choices.flatMap { it.consequences.setFlags }.forEach { f ->
                m[f] = minOf(m[f] ?: Int.MAX_VALUE, chapterNumber(n.chapterId))
            }
        }
    }
    private val storyFlags: Set<String> = setterChapter.keys

    private val described: Set<String> = FlagDescriptions.entries.map { it.flag }.toSet()
    private val hidden: Set<String> = FlagDescriptions.hidden.keys

    // ---------- coverage ----------

    @Test
    fun `every flag the story sets is either described or explicitly hidden`() {
        val unclassified = storyFlags - described - hidden
        assertTrue("Story flags with no Journal decision (describe them or hide them with a reason): $unclassified", unclassified.isEmpty())
    }

    @Test
    fun `no flag is both described and hidden`() {
        assertEquals(emptySet<String>(), described.intersect(hidden))
    }

    @Test
    fun `each flag is described at most once`() {
        val dupes = FlagDescriptions.entries.groupingBy { it.flag }.eachCount().filter { it.value > 1 }
        assertTrue("Duplicate Journal entries: ${dupes.keys}", dupes.isEmpty())
    }

    @Test
    fun `every described or hidden flag is either set by the story or declared planned`() {
        val stray = (described + hidden) - storyFlags - FlagDescriptions.plannedFlags
        assertTrue("Journal knows flags the story never sets (typo or stale?): $stray", stray.isEmpty())
    }

    @Test
    fun `every planned flag is classified`() {
        val unclassified = FlagDescriptions.plannedFlags - described - hidden
        assertTrue("Planned flags missing a Journal decision: $unclassified", unclassified.isEmpty())
    }

    @Test
    fun `the Journal is curated rather than a dump of every flag`() {
        val visibleStoryFlags = storyFlags.intersect(described)
        assertTrue("Only ${visibleStoryFlags.size} story flags are visible", visibleStoryFlags.size in 40..(storyFlags.size / 2 + 20))
        assertTrue("Hidden story flags: ${storyFlags.intersect(hidden).size}", storyFlags.intersect(hidden).size >= storyFlags.size / 3)
        assertTrue("Defeat bookkeeping must be hidden", storyFlags.filter { it.startsWith("lost_to_") }.all { FlagDescriptions.hidden[it] == HiddenReason.BOOKKEEPING })
    }

    // ---------- presentation ----------

    @Test
    fun `entries are well formed first-person lines with no engine identifiers`() {
        FlagDescriptions.entries.forEach { e ->
            assertTrue("${e.flag}: blank", e.text.isNotBlank())
            assertEquals("${e.flag}: stray whitespace", e.text.trim(), e.text)
            assertTrue("${e.flag}: must end with a full stop: ${e.text}", e.text.endsWith("."))
            assertFalse("${e.flag}: contains an underscore: ${e.text}", e.text.contains('_'))
            assertFalse("${e.flag}: repeats its own flag name", e.text.contains(e.flag))
            assertTrue("${e.flag}: too long (${e.text.length})", e.text.length <= 140)
        }
        val dupes = FlagDescriptions.entries.groupingBy { it.text }.eachCount().filter { it.value > 1 }
        assertTrue("Two flags share the same Journal line: ${dupes.keys}", dupes.isEmpty())
    }

    @Test
    fun `an empty or all-hidden flag set gives an empty Journal`() {
        assertTrue(FlagDescriptions.sections(emptySet()).isEmpty())
        assertTrue(FlagDescriptions.sections(hidden).isEmpty())
    }

    @Test
    fun `a Journal with every story flag set shows no hidden flag, no identifier and no empty section`() {
        val sections = FlagDescriptions.sections(storyFlags)
        val lines = sections.flatMap { it.lines }
        assertTrue(lines.isNotEmpty())
        sections.forEach { assertTrue(it.lines.isNotEmpty()) }
        lines.forEach { line -> assertFalse("identifier leaked: $line", line.contains('_')) }
        assertEquals(sections.map { it.category }, sections.map { it.category }.sortedBy { it.ordinal })
    }

    @Test
    fun `entries appear in their own category in story order`() {
        val flags = setOf("opened_black_door", "bound_to_the_gate", "stood_with_voss")
        val sections = FlagDescriptions.sections(flags)
        assertEquals(
            listOf(JournalCategory.DECISIONS, JournalCategory.PEOPLE, JournalCategory.PROMISES),
            sections.map { it.category }
        )
        assertEquals(listOf("I opened the black door beneath the tree."), sections[0].lines)
    }

    // ---------- unknown flags and old saves ----------

    @Test
    fun `unknown flags are ignored and never shown as raw text`() {
        val sections = FlagDescriptions.sections(setOf("some_flag_from_a_future_build", "opened_black_door", "REMOVED_LEGACY_FLAG"))
        val all = sections.flatMap { it.lines }
        assertEquals(1, all.size)
        assertFalse(all.any { it.contains("future") || it.contains("legacy", ignoreCase = true) })
    }

    @Test
    fun `an old save loads with its flags intact and the Journal reads it without error`() {
        val prefs = FakeSharedPreferences()
        val store = SaveStore(prefs)
        val oldFlags = setOf("opened_black_door", "helped_dwarf", "lost_to_unbound_horror", "a_flag_removed_since", "halvard_died")
        store.save(1, GameState(activeSlot = 1, flags = oldFlags))

        val loaded = store.load(1)
        assertNotNull(loaded)
        assertEquals("saving must not change which flags are stored", oldFlags, loaded!!.flags)

        val lines = FlagDescriptions.sections(loaded.flags).flatMap { it.lines }
        assertTrue(lines.contains("I opened the black door beneath the tree."))
        assertTrue(lines.contains("I helped the Stonebeard smith mend his broken gate."))
        assertEquals(3, lines.size) // black door, smith, Halvard. The defeat flag and the unknown flag stay out.
    }

    // ---------- replay / mutually exclusive routes ----------

    @Test
    fun `a replay that leaves contradictory route flags produces one neutral line, not a contradiction`() {
        val lines = FlagDescriptions.sections(setOf("opened_black_door", "walked_away")).flatMap { it.lines }

        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("the black door"))
        assertFalse(lines.single().contains("I opened"))
        assertFalse(lines.single().contains("I walked"))
    }

    @Test
    fun `a single route flag shows its own specific line`() {
        val lines = FlagDescriptions.sections(setOf("walked_away")).flatMap { it.lines }
        assertEquals(listOf("I walked away from the black door."), lines)
    }

    @Test
    fun `every alternative group has a topic and is internally consistent`() {
        val groups = FlagDescriptions.entries.filter { it.group != null }.groupBy { it.group!! }
        groups.forEach { (id, members) ->
            assertTrue("group $id has no topic", FlagDescriptions.groupTopics.containsKey(id))
            assertEquals("group $id mixes categories", 1, members.map { it.category }.toSet().size)
            // Alternatives must come from different choices: if one choice sets two of them they are
            // not alternatives and the replay collapse would hide a real, simultaneous pair.
            nodes.flatMap { it.choices }.forEach { c ->
                val together = members.map { it.flag }.count { it in c.consequences.setFlags }
                assertTrue("group $id: choice '${c.label}' sets more than one member", together <= 1)
            }
        }
        val unusedTopics = FlagDescriptions.groupTopics.keys - groups.keys
        assertTrue("Topics for groups that don't exist: $unusedTopics", unusedTopics.isEmpty())
    }

    @Test
    fun `members of a group are set in the same chapter`() {
        FlagDescriptions.entries.filter { it.group != null && it.flag in storyFlags }.groupBy { it.group!! }.forEach { (id, members) ->
            assertEquals("group $id spans chapters", 1, members.map { setterChapter.getValue(it.flag) }.toSet().size)
        }
    }

    @Test
    fun `groups with several members collapse to exactly one line when all are present`() {
        FlagDescriptions.entries.filter { it.group != null }.groupBy { it.group!! }
            .filter { it.value.size > 1 }
            .forEach { (id, members) ->
                val lines = FlagDescriptions.sections(members.map { it.flag }.toSet()).flatMap { it.lines }
                assertEquals("group $id", 1, lines.size)
                assertTrue(lines.single().contains(FlagDescriptions.groupTopics.getValue(id)))
            }
    }

    // ---------- spoiler safety ----------

    /** Names and terms the player only learns at some point in the story. */
    private val revealedTerms = listOf(
        "Halvard", "Voss", "Ilsevet", "Thessaly", "Selvane", "Kestrel", "Wren", "Greymoor", "Fenmoor", "Emberlow",
        "Duskmere", "Wraithspire", "Ashfall", "Sundering", "Marshal", "Order", "inquisitor", "castellan", "envoy",
        "prisoner", "vigil", "whole", "sixth", "frame", "chain", "Stonebeard", "smith"
    )

    private fun firstChapterMentioning(term: String): Int = nodes
        .filter { n -> n.narrativeText.contains(term, ignoreCase = true) || n.choices.any { it.label.contains(term, ignoreCase = true) } }
        .minOfOrNull { chapterNumber(it.chapterId) } ?: Int.MAX_VALUE

    @Test
    fun `no entry names a character, place or idea before the story has introduced it`() {
        val firstSeen = revealedTerms.associateWith { firstChapterMentioning(it) }
        val problems = mutableListOf<String>()
        FlagDescriptions.entries.filter { it.flag in storyFlags }.forEach { e ->
            val chapter = setterChapter.getValue(e.flag)
            revealedTerms.forEach { term ->
                if (e.text.contains(term, ignoreCase = true) && firstSeen.getValue(term) > chapter) {
                    problems += "${e.flag} (chapter $chapter) mentions '$term' first introduced in chapter ${firstSeen.getValue(term)}"
                }
            }
        }
        FlagDescriptions.groupTopics.forEach { (group, topic) ->
            val chapter = FlagDescriptions.entries.filter { it.group == group && it.flag in storyFlags }.minOfOrNull { setterChapter.getValue(it.flag) }
                ?: return@forEach
            revealedTerms.forEach { term ->
                if (topic.contains(term, ignoreCase = true) && firstSeen.getValue(term) > chapter) {
                    problems += "group $group topic mentions '$term' before chapter ${firstSeen.getValue(term)}"
                }
            }
        }
        assertTrue("Early spoilers:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `no entry reveals private canon or undiscovered motives`() {
        val forbidden = listOf("famil", "comrade", "widow", "orphan", "grief", "avenge", "revenge", "secretly", "betray", "traitor", "turns out", "was always", "never knew")
        val text = FlagDescriptions.entries.map { it.text } + FlagDescriptions.groupTopics.values
        val hits = text.filter { line -> forbidden.any { line.contains(it, ignoreCase = true) } }
        assertTrue("Lines that may reveal private canon: $hits", hits.isEmpty())
    }

    @Test
    fun `the hunters stay unnamed and undescribed beyond what the player saw`() {
        val riderFlags = setOf("spared_the_rider", "robbed_the_rider", "rider_clue", "found_red_cord", "found_the_old_shaft")
        FlagDescriptions.entries.filter { it.flag in riderFlags }.forEach { e ->
            revealedTerms.forEach { term ->
                assertFalse("${e.flag} names '$term', but the riders are unnamed: ${e.text}", e.text.contains(term, ignoreCase = true))
            }
        }
    }

    @Test
    fun `scenes awaiting a narrative decision stay hidden`() {
        listOf("defied_the_prophecy", "claimed_his_own_meaning", "left_it_unanswered").forEach {
            assertEquals(HiddenReason.PENDING_NARRATIVE_DECISION, FlagDescriptions.hidden[it])
        }
    }

    @Test
    fun `lines do not preempt later events`() {
        // A line must only describe what the choice did. These words imply knowledge of what happens next.
        val hindsight = listOf("later", "eventually", "would later", "in the end", "ultimately", "little did")
        val hits = FlagDescriptions.entries.filter { e -> hindsight.any { e.text.contains(it, ignoreCase = true) } }.map { it.flag }
        assertTrue("Hindsight phrasing in: $hits", hits.isEmpty())
    }
}
