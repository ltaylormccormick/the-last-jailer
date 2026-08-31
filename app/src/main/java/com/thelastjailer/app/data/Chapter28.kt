package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXVIII — The Last Gate.
 *
 * Follows the thread's vision from XXVII to the true sixth ward: Wraithspire, a name none of them
 * recognize despite three hundred years of records. This inverts every assumption the last four
 * chapters built — Emberlow, Greymoor, and Duskmere were all lost through loneliness, through nobody
 * being sent to relieve an isolated keeper. Wraithspire was never lost at all. It has been guarded in
 * total secrecy, uninterrupted, for three centuries, by a hidden order nobody — not the Ashen Order,
 * not Ilsevet, not even the whole itself — ever found. The toughest combat yet (Vigil-Captain of
 * Wraithspire, 385 HP) is, for the first time since the game's opening chapters, a human elite
 * guardian rather than a fragment, a manifestation, or a haunted ward — a deliberate structural
 * callback to the dwarven-hold roots of the story. Sets up the true final act: with all six fragments
 * now accounted for, the endgame confrontation with the whole itself is the only thing left.
 */
val chapter28Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_the_thread_shows_now",
        chapterId = "chapter_28",
        title = "What the Thread Shows Now",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Three days on the road and the thread still hasn't wavered, pointing steady and
            certain toward a name none of them have ever heard spoken. "Wraithspire," the prisoner
            says, testing the word like it might be wrong. "Three hundred years, and I've never
            once sensed a sixth gate. Not lost, not lonely, not silent. Just... never there to
            begin with, as far as I could tell."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then whatever's kept it hidden is stronger than anything we've faced.\"",
                nextNodeId = "the_road_to_wraithspire",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("expected_wraithspire_to_be_dangerous")
                )
            ),
            Choice(
                label = "\"Or someone's been doing our job for us this whole time, and doing it well.\"",
                nextNodeId = "the_road_to_wraithspire",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("hoped_wraithspire_was_well_kept")
                )
            )
        )
    ),
    StoryNode(
        id = "the_road_to_wraithspire",
        chapterId = "chapter_28",
        title = "The Road to Wraithspire",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The road climbs somewhere the maps stop agreeing with each other, switchbacking up
            through stone that starts to feel less like hillside and more like the roof of
            something buried. Kaelen has walked into enough of these places by now to recognize
            the specific quiet of a threshold that's still standing. This one feels different —
            not failing, not keening, not forgetting. Watched.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep climbing.", nextNodeId = "what_guards_wraithspire")
        )
    ),
    StoryNode(
        id = "what_guards_wraithspire",
        chapterId = "chapter_28",
        title = "What Guards Wraithspire",
        illustrationId = "what_guards_wraithspire",
        narrativeText = """
            They're waiting before Kaelen's party is halfway up the last rise — a disciplined
            line of sentries in plain, unmarked grey, weapons ready but not yet raised, standing
            like people who have done this exact thing, calmly and without panic, more times than
            anyone outside this mountain will ever know about.

            "Turn back," their captain says, not unkindly. "Whatever you think you found here,
            you didn't."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not here to take anything. I'm here because it's running out of places to hide.\"",
                nextNodeId = "the_vigils_challenge",
                consequences = Consequences(setFlags = setOf("tried_to_explain_first"))
            )
        )
    ),
    StoryNode(
        id = "the_vigils_challenge",
        chapterId = "chapter_28",
        title = "The Vigil's Challenge",
        illustrationId = "the_vigils_challenge",
        narrativeText = """
            The captain doesn't look convinced, and three centuries of a duty kept perfectly is
            not about to be undone by one traveler's word. "Then you'll forgive us for not taking
            that on faith," she says, and the line of sentries finally raises its weapons in
            unison. Whatever Kaelen intended to say next, it's going to have to wait until after.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "wraithspire_vigil_encounter"
    ),
    StoryNode(
        id = "what_wraithspire_reveals",
        chapterId = "chapter_28",
        title = "What Wraithspire Reveals",
        illustrationId = "what_wraithspire_reveals",
        narrativeText = """
            The captain doesn't fight to the last breath the way an enemy would. She yields when
            the line is broken, cleanly, like someone who trained for this exact outcome too.
            "Nobody's beaten the Vigil in three hundred years," she says, breathing hard, no
            anger in it at all. "Come and see what we've been keeping, then. You've more than
            earned the answer to why nobody ever found it."

            What waits past the sentries isn't ruin or grief or forgetting. It's a gate as carefully
            tended as any shrine, six generations of an order nobody outside this mountain ever
            knew existed, still standing watch exactly as their ancestors did the night the
            Sundering happened — because, the captain says quietly, they were the ones who helped
            do it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"You've carried this alone for three hundred years so nobody else would have to. That ends today — with allies, not replacements.\"",
                nextNodeId = "chapter28_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 26)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("offered_the_vigil_alliance"),
                    grantItemIds = listOf("sigil_of_the_unbroken_vigil")
                )
            ),
            Choice(
                label = "\"You don't owe me your trust just because I won a fight. Show me only what you're ready to.\"",
                nextNodeId = "chapter28_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 25)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("respected_the_vigils_pace"),
                    grantItemIds = listOf("sigil_of_the_unbroken_vigil")
                )
            ),
            Choice(
                label = "Say nothing yet. Just look at what they've protected, and let that be answer enough.",
                nextNodeId = "chapter28_end",
                consequences = Consequences(
                    setFlags = setOf("simply_bore_witness_at_wraithspire"),
                    grantItemIds = listOf("sigil_of_the_unbroken_vigil")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter28_end",
        chapterId = "chapter_28",
        title = "End of Chapter XXVIII — The Last Gate",
        illustrationId = "chapter28_threshold",
        narrativeText = """
            Six fragments, Kaelen realizes, standing in a chamber nobody outside a mountain has
            stood in for three hundred years. His own. Emberlow's, still deciding. Greymoor's,
            still keening down to quiet. Duskmere's, already gone. The Sundering Ground's,
            forgetting on purpose. And this one — the only one, out of all six, that was never
            once left alone.

            The prisoner is very quiet, taking that in. "Six," it finally says. "All six,
            accounted for, for the first time since the night we were torn apart. I don't know
            what that means is coming next. I don't think it's ever been possible to know, until
            now."

            Voss says what all three of them are thinking. "Then whatever it's been planning
            since Duskmere, it's about to find out it ran out of places to hide too."

            Chapter XXIX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter28 = com.thelastjailer.app.Chapter(
    id = "chapter_28",
    number = 28,
    title = "Chapter XXVIII — The Last Gate",
    startNodeId = "what_the_thread_shows_now"
)
