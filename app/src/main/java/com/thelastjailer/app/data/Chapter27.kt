package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXVII — What It Sent For the Thread.
 *
 * Opens the true endgame arc XXVI set up: rather than another journey to a new location, the fight
 * comes to Kaelen this time, a deliberate structural break from XXIII-XXVI's travel-then-combat
 * shape. Demanding a plain warning versus resolving to hold on regardless changes how much the
 * prisoner tells him before the attack arrives, not just which stat moves. The thread pulled from
 * the Sundering Ground reacts at night, and the whole — now aware, through its Duskmere foothold, of
 * exactly what was taken and what it might be able to locate — sends its most direct incursion yet
 * to reclaim it. Twenty-seventh and toughest combat yet (The Reclamation, 370 HP), and for the first
 * time not a fragment, a straggler, or a haunted ward, but a piece of the whole's own attacking body,
 * deployed rather than merely persuading. Ends with a genuine gain rather than another ambiguity:
 * repelling the assault leaves a splinter of the whole itself in Kaelen's hands, and the thread,
 * having been fought over, finally shows a direction rather than just a memory — the first real lead
 * on the true, still-unnamed sixth gate.
 */
val chapter27Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_the_thread_wants",
        chapterId = "chapter_27",
        title = "What the Thread Wants",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The thread won't stay still. Kaelen wakes to find it's worked its way out of the
            pouch he sealed it in and is lying across the floor, pointing at the wall like a
            compass needle that doesn't understand walls are supposed to stop it.

            The prisoner is awake too, and has been for a while. "It's not the thread doing
            that," it says. "Something's noticed we're holding it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Noticed how? Say it plainly.\"",
                nextNodeId = "what_the_prisoner_warns_demanded",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("demanded_plain_warning")
                )
            ),
            Choice(
                label = "\"Then we hold onto it harder. Whatever's coming, let it come.\"",
                nextNodeId = "what_the_prisoner_warns_resolved",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("resolved_to_hold_the_thread")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_warns_demanded",
        chapterId = "chapter_27",
        title = "What the Prisoner Warns",
        illustrationId = "what_the_prisoner_warns",
        narrativeText = """
            It gives him the plain version without softening it, respecting the demand enough not
            to dress it up. "The foothold it took through Wren runs both directions, remember. It
            saw us pull that thread free the same way I saw the Sundering Ground through it. It
            knows what that thread might be able to point toward, and it knows we don't
            understand it yet. It's not going to let us catch up."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Wake Voss, and get ready.", nextNodeId = "what_arrives_in_the_dark")
        )
    ),
    StoryNode(
        id = "what_the_prisoner_warns_resolved",
        chapterId = "chapter_27",
        title = "What the Prisoner Warns",
        illustrationId = "what_the_prisoner_warns",
        narrativeText = """
            It doesn't try to talk him out of the resolve, only makes sure he understands exactly
            what it's going to cost. "The foothold it took through Wren runs both directions,
            remember. It saw us pull that thread free the same way I saw the Sundering Ground
            through it. It knows what that thread might be able to point toward, and it knows we
            don't understand it yet. It's not going to let us catch up, and holding on harder
            means it's coming for us directly this time."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Wake Voss, and get ready.", nextNodeId = "what_arrives_in_the_dark")
        )
    ),
    StoryNode(
        id = "what_arrives_in_the_dark",
        chapterId = "chapter_27",
        title = "What Arrives in the Dark",
        illustrationId = "what_arrives_in_the_dark",
        narrativeText = """
            It doesn't knock, doesn't announce itself, doesn't bother pretending to be anything
            but what it is: a piece of the whole itself, on loan from whatever ground it's
            managed to take at Duskmere and the Sundering Ground both, here in person rather than
            in a fragment's dream or a straggler's borrowed loyalty. Voss goes pale in a way
            Kaelen has never seen from her. "That's not a jailer," she says. "That's it."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Hold the line.", nextNodeId = "the_reclamation")
        )
    ),
    StoryNode(
        id = "the_reclamation",
        chapterId = "chapter_27",
        title = "The Reclamation",
        illustrationId = "the_reclamation",
        narrativeText = """
            It doesn't fight like anything Kaelen's faced before, because it isn't trying to win
            so much as take, every strike aimed less at him than at the pouch at his belt, at the
            thread inside it, at the one small thing in this fight it actually came for.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "the_reclamation_encounter"
    ),
    StoryNode(
        id = "what_the_thread_showed",
        chapterId = "chapter_27",
        title = "What the Thread Showed",
        illustrationId = "what_the_thread_showed",
        narrativeText = """
            It breaks off rather than dies, a piece of it left behind in the dirt, still warm,
            still faintly moving, as the rest withdraws to wherever it came from rather than risk
            losing more than a splinter. The thread, freed from the struggle, finally does more
            than point at a wall. It shows Kaelen a place he's never seen and somehow already
            recognizes: a sixth gate, real this time, with a name he can almost read.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Whatever that place is, we go before it recovers enough to stop us.\"",
                nextNodeId = "chapter27_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 25)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("chose_to_pursue_the_vision"),
                    grantItemIds = listOf("splinter_of_the_whole")
                )
            ),
            Choice(
                label = "\"We rest first. A vision half-understood has gotten people killed before.\"",
                nextNodeId = "chapter27_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 24)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("chose_caution_before_pursuit"),
                    grantItemIds = listOf("splinter_of_the_whole")
                )
            ),
            Choice(
                label = "Say nothing yet. Just keep the splinter and the thread safe until morning.",
                nextNodeId = "chapter27_end",
                consequences = Consequences(
                    setFlags = setOf("secured_the_splinter_quietly"),
                    grantItemIds = listOf("splinter_of_the_whole")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter27_end",
        chapterId = "chapter_27",
        title = "End of Chapter XXVII — What It Sent For the Thread",
        illustrationId = "chapter27_threshold",
        narrativeText = """
            Dawn finds them all still standing, which is more than any of them expected an hour
            into the fight. Voss turns the splinter over in her hands like it might still be
            listening. "First time it's shown up in person," she says. "First time it's lost
            anything, too. Those two things happening on the same night feels like it should mean
            something."

            It does. The prisoner confirms it before Kaelen can ask: the thread's vision was
            real, the sixth gate exists, and for the first time since this began, they know
            roughly where to start looking instead of waiting for the next silence, the next yes,
            the next thing lost before they arrive.

            Somewhere out there, the whole is down one splinter of itself and knows exactly what
            that cost it. Kaelen intends to find out just how much.

            Chapter XXVIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_the_thread_shows_now")
        )
    )
)

val chapter27 = com.thelastjailer.app.Chapter(
    id = "chapter_27",
    number = 27,
    title = "Chapter XXVII — What It Sent For the Thread",
    startNodeId = "what_the_thread_wants"
)
