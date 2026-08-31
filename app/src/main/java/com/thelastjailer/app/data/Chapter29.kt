package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXIX — The Whole, Undisguised.
 *
 * Major structural pivot, flagged in the PR: with all six fragments accounted for (XXVIII) and
 * persuasion having largely failed — Emberlow and Greymoor still undecided, the Sundering Ground
 * interrupted, Wraithspire successfully defended, only Duskmere actually yielded — the whole abandons
 * gentleness entirely and makes its most direct move of the entire story: reaching straight for
 * Kaelen's own fragment, the one it has always had the closest access to, through three years of
 * proximity and the vision-combat connection XIV first opened. Rather than asking, this time it
 * simply tries to take. Reuses the vision-combat device (XIV, XXII) for by far the toughest encounter
 * yet (The Whole, Undisguised, 410 HP — the first time facing the whole at anything close to its true
 * scale, not a splinter, a straggler, or a single fragment's dream). Deliberately does not end the
 * overarching conflict here: Kaelen holding the line is a genuine turning point, not the finale, and
 * the chapter closes by setting up a true final confrontation still to come.
 */
val chapter29Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_reaches_for_kaelen",
        chapterId = "chapter_29",
        title = "What Reaches for Kaelen",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            It doesn't come through the prisoner this time. Kaelen feels it directly, for the
            first time in three years — not a request, not a memory, not a gentle asking. A hand,
            closing around the branded mark on his palm from the inside, patient no longer.

            The prisoner's voice is suddenly very small. "It's done asking anyone else. It's
            coming for the fragment it's always had the easiest reach to. Mine."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then it picked the wrong door to force.\"",
                nextNodeId = "what_the_prisoner_says_before",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("stood_defiant_before_the_whole")
                )
            ),
            Choice(
                label = "\"Tell me what you need from me. Exactly.\"",
                nextNodeId = "what_the_prisoner_says_before",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_what_it_needed_before")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_says_before",
        chapterId = "chapter_29",
        title = "What the Prisoner Says Before",
        illustrationId = "what_the_prisoner_says_before",
        narrativeText = """
            "Every other fragment, it could only ask. Yours, it's touched a hundred times over —
            every vision, every fight, every night I've spent behind your eyes instead of my own
            door. It thinks that's the same as a way in. I need you to prove it wrong, and I
            can't do that part for you. Nobody can, this time."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Meet it.", nextNodeId = "into_the_undisguised")
        )
    ),
    StoryNode(
        id = "into_the_undisguised",
        chapterId = "chapter_29",
        title = "Into the Undisguised",
        illustrationId = "into_the_undisguised",
        narrativeText = """
            Kaelen presses his branded palm to nothing at all this time — no black iron, no
            ward-stone, no threshold to cross. The vision simply opens around him, and what's
            waiting isn't a memory, a wraith, or a patient voice anymore. It's the whole itself,
            as close to its true shape as three centuries of scattering has left it able to take,
            filling the space where a fair fight would be with the sheer, undisguised size of what
            it actually is.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "the_whole_encounter"
    ),
    StoryNode(
        id = "what_holding_the_line_costs",
        chapterId = "chapter_29",
        title = "What Holding the Line Costs",
        illustrationId = "what_holding_the_line_costs",
        narrativeText = """
            It doesn't withdraw so much as recoil — genuinely startled, Kaelen realizes, in a way
            nothing this size should be able to afford. Three centuries of never being refused by
            force, and it clearly never planned for the one door it thought it already owned to
            hold anyway.

            When Kaelen comes back to himself, his hand is closed around something that wasn't
            there before — a fragment of the grip that tried to close around him, left behind the
            way the splinter was left behind at the camp, except this time it isn't a piece taken
            in a fight over an object. It's a piece of the attempt itself.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take it, and be ready to keep proving this every time it comes back.",
                nextNodeId = "what_kaelen_decides_after",
                consequences = Consequences(grantItemIds = listOf("broken_grasp_of_the_whole"))
            )
        )
    ),
    StoryNode(
        id = "what_kaelen_decides_after",
        chapterId = "chapter_29",
        title = "What Kaelen Decides After",
        illustrationId = "what_kaelen_decides_after",
        narrativeText = """
            Voss finds him still shaking an hour later and doesn't ask if he's all right, because
            the answer is written plainly enough on his face. "It tried to just take you," she
            says, working it through out loud. "Not ask. Not persuade. Take. That's not the move
            of something that thinks it's winning."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we stop waiting for it to come to us. We take the fight to whatever's left of it.\"",
                nextNodeId = "chapter29_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 27)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("resolved_to_seek_the_whole")
                )
            ),
            Choice(
                label = "\"Then we make sure everyone still deciding knows it can be refused. That matters more right now than chasing it.\"",
                nextNodeId = "chapter29_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 26)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("prioritized_the_undecided_fragments")
                )
            ),
            Choice(
                label = "Say nothing yet. Just rest, and let tomorrow decide what comes next.",
                nextNodeId = "chapter29_end",
                consequences = Consequences(setFlags = setOf("rested_before_deciding"))
            )
        )
    ),
    StoryNode(
        id = "chapter29_end",
        chapterId = "chapter_29",
        title = "End of Chapter XXIX — The Whole, Undisguised",
        illustrationId = "chapter29_threshold",
        narrativeText = """
            The prisoner doesn't say much for the rest of the night, but what it does say, right
            before Kaelen finally sleeps, stays with him longer than anything else that happened
            today. "Three hundred years," it says, "and that's the first time I've ever seen it
            afraid of losing. I didn't know it could be. I don't know what it does with that,
            now that it knows."

            Neither does Kaelen. But for the first time since this began, the fight in front of
            him feels less like six separate doors to defend, and more like one long argument
            they might actually be close to winning.

            Chapter XXX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter29 = com.thelastjailer.app.Chapter(
    id = "chapter_29",
    number = 29,
    title = "Chapter XXIX — The Whole, Undisguised",
    startNodeId = "what_reaches_for_kaelen"
)
