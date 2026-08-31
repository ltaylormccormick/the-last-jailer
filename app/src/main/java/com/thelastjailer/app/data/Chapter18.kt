package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XVIII — No More Between Us.
 *
 * The first direct duel between Kaelen and Ilsevet. With the frame destroyed or disrupted in XVII
 * and nothing left to send in her place, she comes to Stonebeard alone to settle this personally —
 * a deliberate escalation past every squad, captain, and construct sent in her stead since V. The
 * eighteenth combat encounter is the first true jump in scale (Ilsevet herself, 260 HP), and the
 * chapter's real weight sits after the fight rather than during it: a three-way stat-gated choice
 * about what to do with a defeated Ilsevet (capture, mercy, or deferral), each paired to the
 * courage/honour throughlines established since III.
 *
 * Judgment call flagged for review: resolving Ilsevet's personal threat-arc in direct combat this
 * decisively is the biggest structural swing since Halvard's death in X — it closes the "villain
 * hunt" phase of the story that's driven chapters V-XVII. It deliberately doesn't end the larger
 * story (the Cinder faction, the four compromised gates, and the prisoner's unresolved fate all
 * remain open), and the closing line telegraphs directly that a new threat is coming, but this is a
 * real turning point and worth a second look before merging.
 */
val chapter18Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "no_more_envoys",
        chapterId = "chapter_18",
        title = "No More Envoys",
        illustrationId = "threshold_ahead",
        narrativeText = """
            She comes back to the black door three days after the Sanctum, alone this time in
            every sense that matters — no vanguard, no captain, no reliquary thief. Just Ilsevet,
            on foot, a fresh scar along one arm that wasn't there the last time Kaelen saw her up
            close.

            "I'm done sending people to do this," she says. "The frame's gone. Whatever I build
            next starts with a decision only the two of us can actually make."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then let's make it.\"",
                nextNodeId = "her_own_terms",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("accepted_the_duel_readily")
                )
            ),
            Choice(
                label = "\"Say what you actually came to say first.\"",
                nextNodeId = "her_own_terms",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("made_her_say_it_first")
                )
            )
        )
    ),
    StoryNode(
        id = "her_own_terms",
        chapterId = "chapter_18",
        title = "Her Own Terms",
        illustrationId = "her_own_terms",
        narrativeText = """
            "You beat what I sent to guard the frame. You've beaten everything I've sent since
            Ashfall, one way or another. I've run out of things to send." She draws a single
            blade, plain, nothing ceremonial about it. "So: me, you, and whichever of us is still
            standing decides what happens to the brand. No garrison. No prisoner behind your door
            getting a vote. Just the two people this has actually been about since the beginning."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Accept.", nextNodeId = "what_kaelen_says_first")
        )
    ),
    StoryNode(
        id = "what_kaelen_says_first",
        chapterId = "chapter_18",
        title = "What Kaelen Says First",
        illustrationId = "what_kaelen_says_first",
        narrativeText = """
            There's a version of this where Kaelen has something clever to say — about Ashwell,
            about Halvard, about everything four settings and a stolen shard and a reliquary thief
            have cost people who never chose to be part of her arithmetic. He finds, when it
            actually comes to it, that he doesn't much feel like making a speech.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not doing this because I hate you.\"",
                nextNodeId = "the_duel",
                consequences = Consequences(setFlags = setOf("spoke_before_the_duel"))
            ),
            Choice(
                label = "Say nothing. Draw your sword instead.",
                nextNodeId = "the_duel",
                consequences = Consequences(setFlags = setOf("answered_with_silence"))
            )
        )
    ),
    StoryNode(
        id = "the_duel",
        chapterId = "chapter_18",
        title = "The Duel",
        illustrationId = "the_duel",
        narrativeText = """
            She's better than anyone who's fought for her before — faster, colder, entirely
            unhurried even now, the same way she was unhurried walking into Stonebeard's broken
            gate the first time. This isn't a garrison holding a line. This is Ilsevet herself,
            and she isn't holding anything back.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "ilsevet_duel_encounter"
    ),
    StoryNode(
        id = "what_is_left_of_her",
        chapterId = "chapter_18",
        title = "What Is Left of Her",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            She goes down eventually — not killed, not quite broken, just finally, finally out of
            whatever kept her upright through four settings and a burned garrison and every
            failure since Ashwell. Kaelen stands over her with a blade he doesn't lower right
            away, and for the first time since any of this started, Ilsevet looks like exactly
            what she is: a woman who has spent fifteen years losing an argument with grief, and
            just lost the last round of it too.

            "Go on, then," she says, not quite steady. "Finish what Ashwell should have taught me
            to expect."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Decide.", nextNodeId = "what_kaelen_chooses")
        )
    ),
    StoryNode(
        id = "what_kaelen_chooses",
        chapterId = "chapter_18",
        title = "What Kaelen Chooses",
        illustrationId = "what_kaelen_chooses",
        narrativeText = """
            It isn't a clean choice. It was never going to be — not after Ashwell, not after
            Halvard, not after everyone else who paid for the space between what she's afraid of
            and what she's willing to do about it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not going to kill you. But you're done deciding what happens to six lives that aren't yours to spend.\" Bind her and bring her back to face what's left of the Order.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 16)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    setFlags = setOf("captured_ilsevet")
                )
            ),
            Choice(
                label = "\"Ashwell already took enough from you. I won't let this take the rest.\" Let her go, on the condition she never comes back armed.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 15)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    setFlags = setOf("showed_ilsevet_mercy")
                )
            ),
            Choice(
                label = "Take her weapon, and decide the rest later. There's no clean answer waiting right now.",
                nextNodeId = "chapter18_end",
                consequences = Consequences(
                    grantItemIds = listOf("ilsevets_blade"),
                    setFlags = setOf("deferred_ilsevets_fate")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter18_end",
        chapterId = "chapter_18",
        title = "End of Chapter XVIII — No More Between Us",
        illustrationId = "chapter18_threshold",
        narrativeText = """
            Whatever Kaelen decides, one thing is certain by the time the black door's shadow
            falls long again: the Cinder Marshal doesn't get to make this decision for anyone else
            anymore, not today. Voss finds him still standing over where the fight ended, and
            doesn't ask what he chose before he's ready to say it.

            Word will need to reach Selvane. Word will need to reach Fenmoor, and Thessaly, and
            whatever's left of Ilsevet's own people scattered across the Ashfall reaches without a
            Marshal left to answer to. None of that happens tonight.

            Tonight, for the first time since a black door opened beneath an ancient tree, Kaelen
            lets himself believe the worst of it might actually be over.

            He's wrong about that. He just doesn't know it yet.

            Chapter XIX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, while it lasts.", nextNodeId = "what_ilsevet_knew")
        )
    )
)

val chapter18 = com.thelastjailer.app.Chapter(
    id = "chapter_18",
    number = 18,
    title = "Chapter XVIII — No More Between Us",
    startNodeId = "no_more_envoys"
)
