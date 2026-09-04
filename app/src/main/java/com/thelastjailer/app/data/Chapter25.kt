package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXV — When Duskmere Answered.
 *
 * Breaks the pattern XXIII and XXIV established on purpose: this time Kaelen and Voss arrive too
 * late. Duskmere's keeper, Wren — six years alone at her gate with no relief ever sent — has already
 * said yes, and the chapter delivers on the "someone eventually will" dread XXIII's title gestured at
 * without ever actually confirming a yes. Asking the prisoner exactly how it happened versus already
 * accepting they're too late changes how it delivers that fear, and how Kaelen answers standing over
 * Wren's empty watch-post — vowing to hold the gate himself, vowing to learn who she really was, or
 * simply securing it in silence — now colors the chapter's close, not just which flag it leaves
 * behind. This escalates real stakes rather than repeating a third ambiguous silence: a keeper's
 * binding was actually surrendered, something took a first, small, physical foothold through it, and
 * Wren herself is gone rather than merely lost or grieving — a genuinely new kind of cost.
 * Twenty-fifth and toughest combat yet (The Answering Door, 340 HP — a threshold given just enough
 * shape to defend what it's climbing out of). Continues the decrementing "how many fragments remain
 * unaccounted for" count from XXIII/XXIV (two remaining there; one remaining after this chapter),
 * sharpening the endgame the story has been building toward.
 */
val chapter25Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "word_of_duskmere",
        chapterId = "chapter_25",
        title = "Word of Duskmere",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner doesn't wake him gently this time. "Duskmere," it says, voice tight in a
            way Kaelen has never heard from it. "It already answered. I felt it happen." The
            words come out clipped, like it's still catching up to what it's just felt.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Answered how?\"",
                nextNodeId = "what_the_prisoner_fears_asked",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_answered_how")
                )
            ),
            Choice(
                label = "\"Then we're already too late to stop it. What do we do now?\"",
                nextNodeId = "what_the_prisoner_fears_accepted",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("accepted_too_late")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_fears_asked",
        chapterId = "chapter_25",
        title = "What the Prisoner Fears",
        illustrationId = "what_the_prisoner_fears",
        narrativeText = """
            It answers the question exactly as asked, laying out what it knows rather than what
            it fears first. "I don't know yet what 'yes' costs a keeper who isn't a fragment
            herself," it admits. "Wren held that gate alone for six years. Nobody sent to relieve
            her, nobody sent to ask if she still could. I don't blame her for saying yes. I'm
            terrified of what saying yes to it does to the person who says it."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Duskmere.", nextNodeId = "the_road_to_duskmere")
        )
    ),
    StoryNode(
        id = "what_the_prisoner_fears_accepted",
        chapterId = "chapter_25",
        title = "What the Prisoner Fears",
        illustrationId = "what_the_prisoner_fears",
        narrativeText = """
            It doesn't correct the assumption that they're already too late, only builds on it.
            "I don't know yet what 'yes' costs a keeper who isn't a fragment herself," it admits.
            "Wren held that gate alone for six years. Nobody sent to relieve her, nobody sent to
            ask if she still could. I don't blame her for saying yes. I'm terrified of what
            saying yes to it does to the person who says it, and we're about to find out
            together."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Duskmere.", nextNodeId = "the_road_to_duskmere")
        )
    ),
    StoryNode(
        id = "the_road_to_duskmere",
        chapterId = "chapter_25",
        title = "The Road to Duskmere",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Duskmere sits low in a fog that doesn't lift even at midday, and Kaelen feels the
            wrongness of the place before he sees it: the ward-stone at its heart isn't failing
            the way Emberlow's was, or keening the way Greymoor's did. It's changed shape.
            Something that used to be a threshold has started, very slowly, becoming a door.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Approach the ward-stone.", nextNodeId = "what_duskmere_became")
        )
    ),
    StoryNode(
        id = "what_duskmere_became",
        chapterId = "chapter_25",
        title = "What Duskmere Became",
        illustrationId = "what_duskmere_became",
        narrativeText = """
            Keeper Wren's watch-post is empty, her journal left open on the table exactly the way
            Kestrel's ledger was, except this entry isn't unfinished. It ends mid-page with a
            single line, steady-handed, deliberate: I said yes so someone else wouldn't have to
            hold this alone anymore. I hope that's still a kind of mercy.

            Whatever answered her is still here, and it isn't finished arriving.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the journal, and be ready.",
                nextNodeId = "the_answering_shape",
                consequences = Consequences(grantItemIds = listOf("duskmere_keepers_journal"))
            )
        )
    ),
    StoryNode(
        id = "the_answering_shape",
        chapterId = "chapter_25",
        title = "The Answering Shape",
        illustrationId = "the_answering_shape",
        narrativeText = """
            It doesn't look like a wraith or a straggler captain or anything Kaelen's fought
            before, it looks like a door standing up, wearing just enough of a shape to defend
            the threshold it's climbing out of. Whatever Wren said yes to, it's using her gate to
            become something it couldn't be alone.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "duskmere_threshold_encounter"
    ),
    StoryNode(
        id = "what_yielding_cost",
        chapterId = "chapter_25",
        title = "What Yielding Cost",
        illustrationId = "what_yielding_cost",
        narrativeText = """
            The shape doesn't die so much as it stops, folding back down into the ward-stone the
            way a wave folds back into the sea, leaving the threshold exactly as strange and
            unfinished as it was before, and Wren nowhere to be found. Whatever "yes" cost her,
            it didn't leave a body behind to grieve, and Kaelen isn't sure yet if that's better
            or worse than Halvard's ending.

            Voss finds him standing over the empty watch-post and doesn't ask what he's thinking,
            because she's clearly thinking it too.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"We don't leave this gate empty too. I'll send someone, or I'll stay myself.\"",
                nextNodeId = "chapter25_end_vowed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 23)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("vowed_to_hold_duskmere")
                )
            ),
            Choice(
                label = "\"Wren deserves to be remembered as more than a warning. Find out who she was.\"",
                nextNodeId = "chapter25_end_remember",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 22)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("sought_to_remember_wren")
                )
            ),
            Choice(
                label = "Say nothing yet. Just get the gate secured before dark.",
                nextNodeId = "chapter25_end_secured",
                consequences = Consequences(setFlags = setOf("secured_duskmere_in_silence"))
            )
        )
    ),
    StoryNode(
        id = "chapter25_end_vowed",
        chapterId = "chapter_25",
        title = "End of Chapter XXV — When Duskmere Answered",
        illustrationId = "chapter25_threshold",
        narrativeText = """
            Wren's answer changes the count in a way neither silence Kaelen has stood through so
            far did. Emberlow and Greymoor are still deciding. Duskmere already decided, and
            whatever "yes" gave the whole, the prisoner says, carefully, that it wasn't nothing,
            a piece of shape it didn't have a night ago, a door it can stand a little further
            inside of than before. Kaelen leaves word behind before they go, the beginning of a
            promise this gate won't stay empty the way it did for six years.

            "One left after this," Voss says quietly, doing the arithmetic Kaelen's been
            avoiding. "One more silence we haven't heard yet, and we don't even know where to
            start listening."

            The prisoner doesn't answer that. For the first time since this began, Kaelen isn't
            sure it knows either.

            Chapter XXVI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_duskmere_left_behind")
        )
    ),
    StoryNode(
        id = "chapter25_end_remember",
        chapterId = "chapter_25",
        title = "End of Chapter XXV — When Duskmere Answered",
        illustrationId = "chapter25_threshold",
        narrativeText = """
            Wren's answer changes the count in a way neither silence Kaelen has stood through so
            far did. Emberlow and Greymoor are still deciding. Duskmere already decided, and
            whatever "yes" gave the whole, the prisoner says, carefully, that it wasn't nothing,
            a piece of shape it didn't have a night ago, a door it can stand a little further
            inside of than before. Kaelen takes the journal and means to learn the whole of who
            Wren was before this, not just the six years she spent alone at the end of it.

            "One left after this," Voss says quietly, doing the arithmetic Kaelen's been
            avoiding. "One more silence we haven't heard yet, and we don't even know where to
            start listening."

            The prisoner doesn't answer that. For the first time since this began, Kaelen isn't
            sure it knows either.

            Chapter XXVI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_duskmere_left_behind")
        )
    ),
    StoryNode(
        id = "chapter25_end_secured",
        chapterId = "chapter_25",
        title = "End of Chapter XXV — When Duskmere Answered",
        illustrationId = "chapter25_threshold",
        narrativeText = """
            Wren's answer changes the count in a way neither silence Kaelen has stood through so
            far did. Emberlow and Greymoor are still deciding. Duskmere already decided, and
            whatever "yes" gave the whole, the prisoner says, carefully, that it wasn't nothing,
            a piece of shape it didn't have a night ago, a door it can stand a little further
            inside of than before. Kaelen works through the rest of the daylight securing what's
            left, grief pushed down where it can wait until there's time to actually feel it.

            "One left after this," Voss says quietly, doing the arithmetic Kaelen's been
            avoiding. "One more silence we haven't heard yet, and we don't even know where to
            start listening."

            The prisoner doesn't answer that. For the first time since this began, Kaelen isn't
            sure it knows either.

            Chapter XXVI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_duskmere_left_behind")
        )
    )
)

val chapter25 = com.thelastjailer.app.Chapter(
    id = "chapter_25",
    number = 25,
    title = "Chapter XXV — When Duskmere Answered",
    startNodeId = "word_of_duskmere"
)
