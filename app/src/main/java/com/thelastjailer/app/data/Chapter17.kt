package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XVII — What Almost Finished.
 *
 * Where XII's four-setting activation was an accident Ilsevet was still testing the edges of, this
 * chapter is deliberate: with the stolen shards from XVI patched in as substitutes for the two
 * settings she can't take outright, she moves to finish the frame on purpose. Kaelen and Voss race
 * to the Sanctum with whatever Stonebeard can spare rather than an army, culminating in a
 * seventeenth and toughest combat encounter yet (a non-humanoid guardian assembled the same
 * desperate way the frame itself was) and a stat-gated choice about *how* to stop the activation —
 * outright destruction versus careful disruption — that deliberately calls back to the prisoner's
 * fear from XIV about being violently unmade rather than freed. Ends having prevented the immediate
 * crisis without resolving the larger one: Ilsevet herself is never seen, and "tonight" turns out
 * not to have been the deadline that mattered.
 */
val chapter17Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_signal",
        chapterId = "chapter_17",
        title = "The Signal",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner doesn't wait for nightfall this time. "She's not testing anymore," it
            says, mid-afternoon, pressing against the wards hard enough that Kaelen feels it
            through the stone before he even reaches the gate. "Four true settings, whatever she
            took from you, and she's stopped being careful about the difference. She means to
            finish it. Soon."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"How soon?\"",
                nextNodeId = "gathering_what_they_have",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("asked_how_soon")
                )
            ),
            Choice(
                label = "\"Then we don't wait to find out.\"",
                nextNodeId = "gathering_what_they_have",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("moved_without_waiting")
                )
            )
        )
    ),
    StoryNode(
        id = "gathering_what_they_have",
        chapterId = "chapter_17",
        title = "Gathering What They Have",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            There isn't time to build an army, and Kaelen doesn't have one to build regardless.
            What he has is Voss, healed enough to hold a blade properly again, whatever Stonebeard
            can spare without leaving its own gate undefended, and word sent to Fenmoor that may
            or may not arrive before this is decided one way or the other.

            It will have to be enough. It's what "enough" has meant since the day the black door
            first opened.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Move out.", nextNodeId = "the_road_to_ashfall_again")
        )
    ),
    StoryNode(
        id = "the_road_to_ashfall_again",
        chapterId = "chapter_17",
        title = "The Road to Ashfall, Again",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The Ashfall reaches have changed since the last two times Kaelen crossed them. The
            Sanctum's spire doesn't just hum now — it's visible for a day's travel in every
            direction, a wrongness on the horizon that doesn't need darkness to be seen.

            Whatever's left of Ilsevet's garrison isn't hiding it anymore. There's nothing left to
            hide it from.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Get to the chamber.", nextNodeId = "the_chamber_defended")
        )
    ),
    StoryNode(
        id = "the_chamber_defended",
        chapterId = "chapter_17",
        title = "The Chamber, Defended",
        illustrationId = "the_chamber_defended",
        narrativeText = """
            Whatever guards the frame chamber now isn't cinder-grey soldiers — Ilsevet doesn't
            have enough of those left to spare, not after Fenmoor, not after the reprisal squad,
            not after the reliquary theft. What she's put in front of the door is something built
            rather than born, assembled the same desperate way the frame itself was, and it
            doesn't so much fight as insist.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "chamber_guardian_encounter"
    ),
    StoryNode(
        id = "what_must_be_broken",
        chapterId = "chapter_17",
        title = "What Must Be Broken",
        illustrationId = "what_must_be_broken",
        narrativeText = """
            The frame itself is worse up close than the vision back at Stonebeard's own gate ever
            managed to convey — six settings, four of them dark and humming with something that
            isn't light, two of them fitted with fragments too broken to properly belong. It isn't
            finished. It's close enough to almost not matter.

            Kaelen has exactly as long as it takes Ilsevet's remaining people to notice the
            chamber's gone quiet to decide what "stopping this" actually means.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Tear it apart. Whatever the pieces do loose is better than what six settings does whole.\"",
                nextNodeId = "chapter17_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 15)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("destroyed_the_frame_outright")
                )
            ),
            Choice(
                label = "\"Disrupt the settings, don't destroy them. The prisoner was afraid of being unmade — I won't be the one who does it by accident.\"",
                nextNodeId = "chapter17_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 14)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("disrupted_carefully")
                )
            ),
            Choice(
                label = "Do whatever stops it fastest, and worry about the difference later.",
                nextNodeId = "chapter17_end",
                consequences = Consequences(
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("stopped_it_however_possible")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter17_end",
        chapterId = "chapter_17",
        title = "End of Chapter XVII — What Almost Finished",
        illustrationId = "chapter17_threshold",
        narrativeText = """
            Whatever Kaelen does to the frame, it stops being a threat tonight — Ilsevet's voice,
            somewhere beyond the chamber, goes from giving orders to something closer to silence
            once the humming cuts out. He doesn't see her. He isn't sure, afterward, whether that
            was mercy or just bad timing.

            They make it out with Voss's cut arm reopened and nothing else worse than that, which
            counts as the best outcome any of Ilsevet's plans has produced yet. The frame won't
            finish tonight. Kaelen has a growing, specific fear that "tonight" was never really the
            deadline that mattered.

            Chapter XVIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go home.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter17 = com.thelastjailer.app.Chapter(
    id = "chapter_17",
    number = 17,
    title = "Chapter XVII — What Almost Finished",
    startNodeId = "the_signal"
)
