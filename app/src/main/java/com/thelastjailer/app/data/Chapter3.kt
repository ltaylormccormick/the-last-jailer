package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter III — What the Door Remembers.
 *
 * Resolves Chapter II's cliffhanger (does Kaelen take the office), gives the thing behind the
 * gate its first real voice instead of just a knock, and introduces the first external
 * antagonist — someone from Kaelen's own world, above ground, who has heard that a jailer has
 * been named and does not consider that good news. This is the last free chapter, so it ends on
 * a real hook rather than a resolution: a named threat now knows Kaelen exists, and knows what
 * he's guarding.
 */
val chapter3Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_answer",
        chapterId = "chapter_3",
        title = "The Answer",
        illustrationId = "the_answer",
        narrativeText = """
            Halvard waits the way old men wait for weather — patient, certain it will come whether
            or not he's ready for it.

            Kaelen thinks of the cloak he doesn't wear anymore. Of ten steps taken and reconsidered
            on a ruined road. Of a door that knocked for him specifically, out of everyone who
            might have walked that way.

            "Say it plainly," Halvard says. "I'm too old for a man who needs three sentences to
            say yes."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I accept the office. All of it.\"",
                nextNodeId = "the_binding_rite",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("accepted_fully")
                )
            ),
            Choice(
                label = "\"I'll hold the gate. Until someone better is found.\"",
                nextNodeId = "the_binding_rite",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("accepted_reluctantly")
                )
            )
        )
    ),
    StoryNode(
        id = "the_binding_rite",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really — no crown, no witnesses but one tired dwarf
            and a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer
            than a finger and presses it flat against the black stone.

            The wards that still hold drink the offering like they've been thirsty for it. For
            one heartbeat, Kaelen feels the full weight of the thing on the other side of the
            gate — not its shape, not its name, only its patience, vast and undiminished by
            however many centuries it has spent waiting for exactly this.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Endure it without a sound.",
                nextNodeId = "the_first_true_knock",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("jailers_brand"),
                    setFlags = setOf("bound_to_the_gate"),
                    unlockTrophy = "The Last Jailer"
                )
            )
        )
    ),
    StoryNode(
        id = "the_first_true_knock",
        chapterId = "chapter_3",
        title = "The First True Knock",
        illustrationId = "the_first_true_knock",
        narrativeText = """
            That night — if the deep road has nights — something knocks from the other side of
            the gate for the first time since the black door. Not stone against stone. Knuckles.
            Deliberate. Patient, the way the whole thing has always been patient.

            Then a voice, or the memory of one, shaped out of the same pressure that used to just
            be a knock: it knows his name. It knows what his cloak used to mean. It offers,
            almost gently, to give it back to him — the silver, the rank, the version of Kaelen
            that never failed anyone. All he would have to do is listen a little longer.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen to what it's offering.",
                nextNodeId = "voices_above",
                consequences = Consequences(setFlags = setOf("listened_to_the_prisoner"))
            ),
            Choice(
                label = "Turn away before it finishes.",
                nextNodeId = "voices_above",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_the_prisoner")
                )
            )
        )
    ),
    StoryNode(
        id = "voices_above",
        chapterId = "chapter_3",
        title = "Voices Above",
        illustrationId = "voices_above",
        narrativeText = """
            Days blur together this far under the earth. It's Halvard who notices first —
            a vibration in the stone that has nothing to do with the gate, coming from directly
            overhead. Boots. More than a few. Moving with the unhurried confidence of people who
            think they're the only ones with a claim on what's up there.

            "Nobody's climbed that road in longer than I've kept this post," Halvard says. "Someone
            has, now."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Climb back up to the black door.", nextNodeId = "the_surface_again")
        )
    ),
    StoryNode(
        id = "the_surface_again",
        chapterId = "chapter_3",
        title = "The Surface, Again",
        illustrationId = "threshold_ahead",
        narrativeText = """
            Rain again, or still — Kaelen has lost track of how long he's been below. The ancient
            tree looks exactly as he left it. The black door does not.

            Four riders wait beside it, colours he doesn't recognize, and a fifth figure standing
            close enough to the iron to have already tried it. She turns before Kaelen's boots
            clear the roots. "There you are," she says, as if she'd been expecting him specifically.
            "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist had opened
            somewhere out here — and that it had found itself a keeper."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "\"What do you want with it?\"", nextNodeId = "a_choice_at_the_door")
        )
    ),
    StoryNode(
        id = "a_choice_at_the_door",
        chapterId = "chapter_3",
        title = "A Choice at the Door",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "The Order doesn't believe in locked doors," Voss says. "Only opened ones and buried
            ones. Your gate has had three centuries to be the second kind and failed. We're here
            to finish the job properly — brick, fire, and enough of it that nothing behind that
            door ever knocks again. You, and whatever dwarves are left down there, are welcome to
            stand aside."

            She hasn't drawn a weapon. She hasn't needed to; the four behind her have.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Stand aside yourself. This isn't yours to bury.\"",
                nextNodeId = "chapter3_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("stood_his_ground")
                )
            ),
            Choice(
                label = "\"There's a man alive behind that gate who deserves better than fire.\"",
                nextNodeId = "chapter3_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("argued_for_mercy")
                )
            ),
            Choice(
                label = "\"Give me time to send word below before you try.\"",
                nextNodeId = "chapter3_end",
                consequences = Consequences(setFlags = setOf("stalled_for_time"))
            )
        )
    ),
    StoryNode(
        id = "chapter3_end",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Voss doesn't press it, not today. She studies Kaelen the way she'd study a lock she
            hasn't picked yet — not defeated, only patient, and patience is clearly a professional
            habit of hers.

            "You'll want to hear this from me and not from someone with less patience," she says,
            swinging back up onto her horse. "The Ashen Order doesn't send one rider and four
            swords for a rumour. We send them for a confirmation. Congratulations, jailer. You've
            just been confirmed."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now — and for the first time since the rain started, that doesn't feel like enough.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    )
)

val chapter3 = com.thelastjailer.app.Chapter(
    id = "chapter_3",
    number = 3,
    title = "Chapter III — What the Door Remembers",
    startNodeId = "the_answer"
)
