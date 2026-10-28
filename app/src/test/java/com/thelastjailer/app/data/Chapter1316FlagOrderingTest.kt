package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The order in which a choice clears and sets flags, and the final state each new set-and-clear choice leaves. The engine does
 * `(flags - clearFlags) + setFlags` in one place ([applyChoice]), so a flag named in both lists ends up set, and a flag named only in
 * the clear list ends up gone. Chapters 13 to 16 lean on that in three ways: the exclusive answers (the ask, the door, the Voss
 * ladder) clear their own family and then set one member; the first choices of a chapter clear everything the chapter records,
 * which includes the flag the choice itself sets; and the chapter-entry choices clear the next chapter's flags and set nothing.
 *
 * The existing per-scene tests start each choice from the states a player can reach. These start from the worst state a save can
 * hold, every member of the family at once, and the assertion is the whole flag set, not one member of it.
 */
class Chapter1316FlagOrderingTest {

    private val unrelated = "unrelated_marker_flag"

    /** Every chapter the package reads from or edits. */
    private val chapters = listOf(3, 11, 12, 13, 14, 15, 16)

    private fun nodes(): List<StoryNode> = chapters.flatMap { StoryRepository.nodesInChapter("chapter_$it") }

    private fun clearing(): List<Pair<StoryNode, Choice>> =
        nodes().flatMap { n -> n.choices.filter { it.consequences.clearFlags.isNotEmpty() }.map { n to it } }

    private fun run(choice: Choice, flags: Set<String>): Set<String> = GameState(flags = flags).applyChoice(choice).flags

    // ---------------------------------------------------------------------------------------------
    // The engine
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the engine clears before it sets, for every combination of held, cleared and set`() {
        for (held in listOf(false, true)) for (cleared in listOf(false, true)) for (set in listOf(false, true)) {
            val choice = Choice(
                "x", "next",
                consequences = Consequences(
                    clearFlags = if (cleared) setOf("f") else emptySet(),
                    setFlags = if (set) setOf("f") else emptySet()
                )
            )
            val end = run(choice, if (held) setOf("f", unrelated) else setOf(unrelated))
            assertEquals("held=$held cleared=$cleared set=$set", set || (held && !cleared), "f" in end)
            assertTrue("held=$held cleared=$cleared set=$set", unrelated in end)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Every choice that clears, in the chapters the package reads or edits
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `every clearing choice leaves exactly its own set flags in what it clears, from the worst state a save can hold`() {
        val all = clearing()
        assertTrue("the sweep found ${all.size} clearing choices", all.size >= 25)
        all.forEach { (node, c) ->
            val clear = c.consequences.clearFlags
            val set = c.consequences.setFlags
            val where = "${node.id}: ${c.label}"
            // everything the choice touches is held at once, with one flag it must not touch
            val worst = run(c, clear + set + unrelated)
            assertEquals(where, set + unrelated, worst)
            // a save holding none of it
            assertEquals(where, set + unrelated, run(c, setOf(unrelated)))
            // a save that already holds what the choice sets
            assertEquals(where, set + unrelated, run(c, set + unrelated))
        }
    }

    @Test
    fun `the choices that clear and set the same flag are exactly the ones the package relies on`() {
        val overlap = clearing().filter { (_, c) -> c.consequences.clearFlags.intersect(c.consequences.setFlags).isNotEmpty() }
        val inPackage = overlap.filter { (n, _) -> n.chapterId in setOf("chapter_13", "chapter_14", "chapter_15", "chapter_16") }
        // the new exclusive answers of Chapter 13, the stances the delegation re-asserts, and the rung steps of Chapters 13 and 16
        assertEquals(
            setOf("c13_asked_openly", "c13_door_open", "c13_door_barred", "stood_with_voss", "let_voss_go_alone", "c7_rung_working", "c7_rung_trusted"),
            inPackage.flatMap { it.second.consequences.setFlags }.toSet()
        )
        // each sets exactly one flag, and it is a flag the same choice clears: a choice that sets one answer leaves one answer
        inPackage.forEach { (node, c) ->
            assertEquals("${node.id}: ${c.label}", 1, c.consequences.setFlags.size)
            assertTrue("${node.id}: ${c.label}", c.consequences.setFlags.single() in c.consequences.clearFlags)
        }
        // the four delegation choices, the open question (the low one clears and sets nothing), the two door answers, and the rung
        // steps: three copies each at the two Chapter 13 night scenes (one per cover scene the eve can lead to), one each in Chapter 16
        assertEquals(4 + 1 + 2 + (3 + 3 + 1 + 1), inPackage.size)
    }

    // ---------------------------------------------------------------------------------------------
    // The flags the review asked about
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the ask at the stirrup leaves c13_asked_openly set only for the open question`() {
        val everything = C13_TELLING_FLAGS + unrelated
        val open = takeLabel(at("c13_parting", flags = everything), "Ask what Ilsevet was before she was the Marshal, where Voss can hear.")
        assertEquals("c13_parting_open", open.sceneId)
        assertEquals(setOf("c13_asked_openly"), open.flags.intersect(C13_ASK_FAMILY))
        // the ask clears the family and nothing else: the other answers of the chapter are as they were
        assertEquals(everything, open.flags)

        val low = takeLabel(at("c13_parting", flags = everything), "Ask what Ilsevet was before she was the Marshal, quietly, once Voss has turned away.")
        assertEquals("c13_parting_low", low.sceneId)
        assertEquals(emptySet<String>(), low.flags.intersect(C13_ASK_FAMILY))
        assertEquals(everything - "c13_asked_openly", low.flags)
    }

    @Test
    fun `Edda's door leaves exactly one of c13_door_open and c13_door_barred, whichever a save held`() {
        val both = C13_DOOR_FAMILY + unrelated
        val none = setOf(unrelated)
        listOf(both, none, setOf("c13_door_open", unrelated), setOf("c13_door_barred", unrelated)).forEach { held ->
            val open = takeLabel(at("c13_door", flags = held), "Keep the door open to them, night and day.")
            assertEquals(held.toString(), setOf("c13_door_open"), open.flags.intersect(C13_DOOR_FAMILY))
            assertTrue(held.toString(), unrelated in open.flags)
            val barred = takeLabel(at("c13_door", flags = held), "Bar it at night, and tell them why.")
            assertEquals(held.toString(), setOf("c13_door_barred"), barred.flags.intersect(C13_DOOR_FAMILY))
            assertTrue(held.toString(), unrelated in barred.flags)
        }
    }

    @Test
    fun `each honest answer to Voss moves the ladder exactly one rung and leaves one rung, from a save holding all three`() {
        val ladder = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")
        val steps = (13..16).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
            .flatMap { n -> n.choices.filter { c -> c.consequences.clearFlags == C7_RUNG_FAMILY }.map { n to it } }
        // two scenes at night in Chapter 13 and two at the handing in Chapter 16, each from the rung below the one it sets; at night the
        // honest answer is written once for each of the three eves it can lead to, and the copies agree in everything but where they go
        assertEquals(setOf("c13_night_wary", "c13_night_working", "c16_handing_wary", "c16_handing_working"), steps.map { it.first.id }.toSet())
        assertEquals(
            mapOf("c13_night_wary" to 3, "c13_night_working" to 3, "c16_handing_wary" to 1, "c16_handing_working" to 1),
            steps.groupingBy { it.first.id }.eachCount()
        )
        steps.groupBy { it.first.id }.forEach { (id, copies) ->
            assertEquals(id, 1, copies.map { it.second.label }.toSet().size)
            assertEquals(id, 1, copies.map { it.second.consequences }.toSet().size)
            assertEquals(id, copies.size, copies.map { it.second.nextNodeId }.toSet().size)
        }
        steps.forEach { (node, c) ->
            val target = c.consequences.setFlags.single()
            val from = ladder[ladder.indexOf(target) - 1]
            val where = "${node.id}: ${c.label}"
            assertEquals(where, node.id.substringAfterLast('_'), from.substringAfterLast('_'))
            // from the rung the scene is read at
            assertEquals(where, setOf(target), run(c, setOf(from)).intersect(C7_RUNG_FAMILY))
            // from a save holding every rung, which no play reaches but a corrupted or merged save could
            assertEquals(where, setOf(target), run(c, ladder.toSet()).intersect(C7_RUNG_FAMILY))
            // from a save holding none: the lowest rung is read by absence, and the step still lands on its target
            assertEquals(where, setOf(target), run(c, emptySet()).intersect(C7_RUNG_FAMILY))
        }
    }

    @Test
    fun `the stance a delegation choice sets is the only stance it leaves, whatever the save held`() {
        val entries = StoryRepository.nodesInChapter("chapter_13")
            .flatMap { n -> n.choices.filter { it.consequences.clearFlags == C13_TELLING_FLAGS }.map { n to it } }
        // the guarded and unguarded versions of the delegation, two choices each
        assertEquals(setOf("the_orders_delegation_trusted", "the_orders_delegation_guarded"), entries.map { it.first.id }.toSet())
        assertEquals(4, entries.size)
        val stances = setOf("stood_with_voss", "let_voss_go_alone")
        entries.forEach { (node, c) ->
            val mine = c.consequences.setFlags.single()
            assertTrue("${node.id}: ${c.label}", mine in stances)
            val everything = C13_TELLING_FLAGS + C14_TELLING_FLAGS + unrelated
            val end = run(c, everything)
            assertEquals("${node.id}: ${c.label}", setOf(mine), end.intersect(C13_TELLING_FLAGS))
            // the later chapters' flags are not this choice's to touch
            assertEquals("${node.id}: ${c.label}", C14_TELLING_FLAGS + unrelated + mine, end)
        }
        assertEquals(setOf("stood_with_voss", "let_voss_go_alone"), entries.map { it.second.consequences.setFlags.single() }.toSet())
    }

    @Test
    fun `nothing between the setter of an Edda or ask flag and the scenes that read it can clear it`() {
        val flags = listOf("c13_asked_openly") + C13_DOOR_FAMILY
        val everywhere = (1..40).flatMap { StoryRepository.nodesInChapter("chapter_$it") }
        flags.forEach { flag ->
            val clearers = everywhere.flatMap { n -> n.choices.filter { flag in it.consequences.clearFlags }.map { n.id to it } }
            val setters = everywhere.flatMap { n -> n.choices.filter { flag in it.consequences.setFlags }.map { n.id to it } }
            // the setters are in Chapter 13, and so is every clearer: the delegation that opens the chapter and the answers themselves
            assertTrue(flag, setters.isNotEmpty())
            (setters + clearers).forEach { (id, _) -> assertEquals("$flag at $id", "chapter_13", StoryRepository.node(id).chapterId) }
            // and the entry choices of Chapters 14 to 16 clear none of it, so it survives to the scenes that read it
            assertTrue(flag, listOf(C14_TELLING_FLAGS, C15_TELLING_FLAGS, C16_TELLING_FLAGS).none { flag in it })
        }
    }
}
