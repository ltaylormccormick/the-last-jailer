package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXIV — The Second Silence.
 *
 * Continues the "race to reach the fragments" plot engine XXIII established: rather than a fresh
 * location, this chapter deliberately returns to Greymoor (first seen in VI, "The Weight of Others'
 * Doors," where warden Kestrel was found dead) — paying off that earlier setup by revealing Kestrel's
 * gate is itself one of the six fragments now being courted by the whole. Unlike Emberlow's slow,
 * eleven-year loneliness, Greymoor's loss was recent and violent, giving this chapter a different
 * emotional register (grief/fury rather than patience) while mirroring XXIII's structure closely on
 * purpose — a deliberate "same problem, different shape" beat rather than a new mechanic. Twenty-
 * fourth and toughest combat yet (Greymoor Ward-Wraith, 325 HP — grief given just enough shape to
 * defend what's left of the failing ward). Mirrors XXIII's refusal to resolve cleanly: Greymoor goes
 * quiet rather than answering, continuing the decrementing "how many fragments are left" count XXIII
 * introduced (three remaining there; two remaining after this chapter).
 */
val chapter24Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "word_of_greymoor",
        chapterId = "chapter_24",
        title = "Word of Greymoor",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Three days after Emberlow goes quiet, the prisoner speaks a name Kaelen has been
            dreading since Voss said "the nearest." "Greymoor," it says. "Kestrel's gate. It's
            already listening."

            Kaelen remembers the shape of that failure well enough without needing reminding — an
            empty watch-post, a locket he still carries, and a warden who died alone doing exactly
            what he's spent three years doing.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Voss should hear this too. We go together.\"",
                nextNodeId = "what_the_prisoner_recalls",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("brought_voss_to_greymoor")
                )
            ),
            Choice(
                label = "\"How long has it been listening?\"",
                nextNodeId = "what_the_prisoner_recalls",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_how_long_ch24")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_recalls",
        chapterId = "chapter_24",
        title = "What the Prisoner Recalls",
        illustrationId = "what_the_prisoner_recalls",
        narrativeText = """
            "Not years, like Emberlow. Weeks," it says. "Kestrel didn't fade out of the world —
            she was taken out of it, mid-watch, mid-sentence. That kind of ending doesn't leave a
            fragment lonely so much as it leaves one furious. I don't know if furious says no more
            easily than lonely does, or less."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Greymoor.", nextNodeId = "the_road_to_greymoor")
        )
    ),
    StoryNode(
        id = "the_road_to_greymoor",
        chapterId = "chapter_24",
        title = "The Road to Greymoor",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Greymoor hasn't changed since Kaelen last stood in it — the same low grey hills, the
            same watch-post with its door still hanging the way the search party left it months
            ago. What's changed is what's underneath. The ward here doesn't hum the way a healthy
            one does. It keens, low and constant, like something that hasn't stopped being
            surprised it's still standing.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Approach the watch-post.", nextNodeId = "what_greymoor_became")
        )
    ),
    StoryNode(
        id = "what_greymoor_became",
        chapterId = "chapter_24",
        title = "What Greymoor Became",
        illustrationId = "what_greymoor_became",
        narrativeText = """
            Kestrel's ledger is still on the table where she left it, open to an unfinished entry.
            Beside it, half-formed shapes have begun peeling away from the ward itself — not
            guardians posted here, but pieces of the failing ward given just enough shape to
            defend what's left of it. Grief, Kaelen is starting to understand, can build a wall as
            well as anger can.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the ledger before it's lost too.",
                nextNodeId = "what_waits_in_the_keening",
                consequences = Consequences(grantItemIds = listOf("greymoor_wardens_ledger"))
            )
        )
    ),
    StoryNode(
        id = "what_waits_in_the_keening",
        chapterId = "chapter_24",
        title = "What Waits in the Keening",
        illustrationId = "what_waits_in_the_keening",
        narrativeText = """
            The shapes don't so much attack as insist — the same way a held breath insists on
            being released. Whatever's left of Greymoor's ward isn't trying to kill him. It's
            trying to make sure nothing else gets taken from it without a fight.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "greymoor_unraveling_encounter"
    ),
    StoryNode(
        id = "reaching_greymoor",
        chapterId = "chapter_24",
        title = "Reaching Greymoor",
        illustrationId = "reaching_greymoor",
        narrativeText = """
            The keening stops the moment the last shape unravels, and for a long moment Greymoor
            is simply quiet — not the patient quiet of something listening, but the exhausted
            quiet of something that's been holding its breath since Kestrel died. Kaelen has stood
            in exactly this kind of silence before. He knows better than to fill it too quickly.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"She didn't get a choice in how this ended. You still do.\"",
                nextNodeId = "chapter24_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 22)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("reminded_greymoor_of_choice")
                )
            ),
            Choice(
                label = "\"I'm not here to replace her. Just to make sure you're not alone with this.\"",
                nextNodeId = "chapter24_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 21)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("offered_presence_not_replacement")
                )
            ),
            Choice(
                label = "Sit with the silence, and let it decide when to break.",
                nextNodeId = "chapter24_end",
                consequences = Consequences(setFlags = setOf("sat_with_greymoors_silence"))
            )
        )
    ),
    StoryNode(
        id = "chapter24_end",
        chapterId = "chapter_24",
        title = "End of Chapter XXIV — The Second Silence",
        illustrationId = "chapter24_threshold",
        narrativeText = """
            Greymoor doesn't answer that night either — not yes, not no, not even the deliberate
            silence Emberlow chose. It simply stops keening, which the prisoner tells him,
            carefully, is not nothing.

            Voss counts the tally on the ride back the way she's taken to doing lately: two
            fragments visited, two silences instead of answers, and by the prisoner's own
            reckoning, two more conversations happening somewhere out there without them. "We're
            not losing," she says. "But I don't think we're winning, either."

            Kaelen doesn't disagree. He adds Kestrel's ledger to the growing weight he carries out
            of other people's doors, and turns toward whatever comes next.

            Chapter XXV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter24 = com.thelastjailer.app.Chapter(
    id = "chapter_24",
    number = 24,
    title = "Chapter XXIV — The Second Silence",
    startNodeId = "word_of_greymoor"
)
