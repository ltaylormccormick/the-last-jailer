package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Everything Chapter XIV records: the two shipped ways of meeting the prisoner's voice, the three shipped ways of telling Voss
 * what Kaelen saw, and the defeat marker. The chapter's first choices clear it all, so a replay reaches the prisoner with an
 * empty slate.
 */
val C14_TELLING_FLAGS: Set<String> = setOf(
    "asked_the_prisoner_directly", "listened_without_asking", "told_voss_everything", "recorded_the_vision_first",
    "kept_the_vision_to_himself", "lost_to_the_memory_itself"
)

/** From the stone's offer to the first of the two memories it keeps: the first knock, as Kaelen answered it in Chapter III. */
private fun toTheKnock(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c14_knock_listened", requirements = ChoiceRequirement(requiredFlags = setOf("listened_to_the_prisoner"))),
    Choice(label = label, nextNodeId = "c14_knock_refused", requirements = ChoiceRequirement(forbiddenFlags = setOf("listened_to_the_prisoner")))
)

/** From the first knock to the day of the office, as Kaelen answered Halvard in Chapter III. The walk away is read by the absence of the other two. */
private fun toTheOffice(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c14_office_full", requirements = ChoiceRequirement(requiredFlags = setOf("accepted_fully"))),
    Choice(label = label, nextNodeId = "c14_office_reluctant", requirements = ChoiceRequirement(requiredFlags = setOf("accepted_reluctantly"))),
    Choice(
        label = label,
        nextNodeId = "c14_office_refusedonce",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("accepted_fully", "accepted_reluctantly"))
    )
)

/**
 * One of the shipped ways of giving Voss what Kaelen saw, written once per rung: whether he told her or wrote it, she has it, and
 * what she does with it is hers. The lowest rung also covers a save that holds none. The stat gate, the item and the flag are the shipped ones.
 */
private fun vossHears(label: String, minStats: Map<StatType, Int>, consequences: Consequences): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c14_voss_heard_trusted",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"), minStats = minStats),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c14_voss_heard_working",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"), minStats = minStats),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c14_voss_heard_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"), minStats = minStats),
        consequences = consequences
    )
)

/**
 * Chapter XIV — Furniture.
 *
 * Returns to the thread dormant since III/V: the pressure behind Kaelen's own gate, which XIII
 * ended with speaking again for the first time in many chapters. Where its two prior offers
 * (IV/V/VIII's "dark aid" temptation) were about power, this chapter reframes it as something more
 * complicated — the prisoner is afraid of what a completed seventh door would do to it specifically
 * (dissolve six distinct entities into one that remembers being none of them), which makes it, for
 * the first time, aligned with Kaelen against Ilsevet for reasons that have nothing to do with him.
 * Asking it outright versus staying silent and letting it volunteer the answer now changes how
 * readily it opens up, not just which stat moves. A vision it offers manifests as this chapter's
 * combat: a symbolic, non-physical confrontation with a memory-construct of whatever severed the
 * six wards from something closer to a choice, three centuries ago — new worldbuilding rather than
 * a resolved mystery, deliberately left open. Fourteenth and toughest combat encounter yet (The
 * Memory Itself, 190 HP). Ends with the prisoner's stance toward Kaelen shifted from
 * adversary/tempter to something closer to a wary ally.
 *
 * The expansion lets the prisoner remember Kaelen, and lets Voss hear what he saw. After Chapter XIII the nights at the gate are
 * told as the prisoner finds them, by whether Kaelen trained others, stood watch with them or kept the gate alone. Before the stone
 * shows him the vision it shows him what it has kept of him: the first knock, as he answered it, and the day he answered Halvard.
 * After the vision, Voss has what he gave her at her own rung, or sees that he has kept it. Nothing here says what the prisoner is,
 * who cut the six wards apart, or what it was before; the promise in "The Names It Won't Give" is not paid. The shipped scenes
 * and their fight are untouched; only where some of their choices lead has changed.
 */
val chapter14Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c14_gate_training",
        chapterId = "chapter_14",
        title = "The Gate at Night",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            In the nights after the drilling, the pressure behind the door learns the three
            of them by their feet. It knows the one who walks heavy on his heels, and the
            one who holds her breath at the threshold, and it knows the third by the way the
            other two make room. It says nothing of this. But the wards lean toward each
            footfall the way a plant leans toward a window, and Kaelen understands that
            something which has spent three centuries unable to see its keeper has been
            given, at last, a household to listen to. He cannot say whether he has done it
            a kindness or a harm.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep listening.", nextNodeId = "what_waits_speaks"))
    ),
    StoryNode(
        id = "c14_gate_watch",
        chapterId = "chapter_14",
        title = "The Gate at Night",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            There is usually someone at the gate now. A smith sits for an hour on an
            upturned bucket and says nothing. Someone from the stair comes up with a
            blanket and sleeps against the wall. The pressure behind the door does not
            speak while they are there. It goes quiet in a particular way, like a room when
            a guest enters it, and it begins again when the last of them has gone down.
            Kaelen cannot tell whether the quiet is courtesy or study. He suspects the
            prisoner cannot tell either.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep listening.", nextNodeId = "what_waits_speaks"))
    ),
    StoryNode(
        id = "c14_gate_deferred",
        chapterId = "chapter_14",
        title = "The Gate at Night",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The nights at the gate are his alone, and the pressure behind the door has had
            a long time to learn the shape of one man sitting still. It is patient with
            him, in the way of something that has outlasted every jailer before him. With
            nobody else in the passage the wards have grown a little careless of the sound
            of his breathing, the way people grow careless of a sound that has always been
            there. He cannot say whether being known that well is the same as being kept
            company.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep listening.", nextNodeId = "what_waits_speaks"))
    ),
    StoryNode(
        id = "what_waits_speaks",
        chapterId = "chapter_14",
        title = "What Waits Speaks",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            It doesn't stop at one sentence. Night after night, the pressure behind the black
            door keeps finding new ways to press against the wards, not testing them the way it
            used to, not asking for anything. Just talking, the way something talks when it's
            finally decided silence isn't working for it anymore.

            "Four settings," it says again, the third night running. "You felt it too. I know
            you did. Ask me what happens at six."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What happens at six?\"",
                nextNodeId = "what_it_says_asked",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("asked_the_prisoner_directly")
                )
            ),
            Choice(
                label = "Say nothing. Listen instead.",
                nextNodeId = "what_it_says_listened",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("listened_without_asking")
                )
            )
        )
    ),
    StoryNode(
        id = "what_it_says_asked",
        chapterId = "chapter_14",
        title = "What It Says",
        illustrationId = "what_it_says",
        narrativeText = """
            "Since you're asking plainly, I'll answer plainly." Something in the pressure behind
            the wards eases, fractionally, at being asked outright rather than made to volunteer
            it. "At six, whatever she's building doesn't stay a cage. It becomes a door with no
            other side, six of us, poured into one shape, and whatever walks out afterward won't
            remember being any of us individually. I have been many things in three centuries
            behind this stone. I would rather not become someone else's furniture."

            It has never, in all the times it has spoken to him, sounded uncertain before.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press further.", nextNodeId = "the_names_it_wont_give")
        )
    ),
    StoryNode(
        id = "what_it_says_listened",
        chapterId = "chapter_14",
        title = "What It Says",
        illustrationId = "what_it_says",
        narrativeText = """
            The silence stretches long enough that Kaelen almost thinks it's satisfied with
            having said nothing more, and then it keeps talking anyway, as if his quiet gave it
            more room rather than less. "At six, whatever she's building doesn't stay a cage. It
            becomes a door with no other side, six of us, poured into one shape, and whatever
            walks out afterward won't remember being any of us individually. I have been many
            things in three centuries behind this stone. I would rather not become someone
            else's furniture."

            It has never, in all the times it has spoken to him, sounded uncertain before, or
            said so much unasked.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press further.", nextNodeId = "the_names_it_wont_give")
        )
    ),
    StoryNode(
        id = "the_names_it_wont_give",
        chapterId = "chapter_14",
        title = "The Names It Won't Give",
        illustrationId = "the_names_it_wont_give",
        narrativeText = """
            "What are you?" Kaelen asks, the same question he's never quite gotten a straight
            answer to since the black door first opened. "Who were you, before this."

            A long pause, long enough that Kaelen wonders if it's decided silence is working for
            it again after all. "Ask me that again," it says finally, "after there are fewer of
            us left to lose. I'll answer honestly. I promise you that much costs me nothing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "\"Show me, then.\"", nextNodeId = "the_offered_glimpse")
        )
    ),
    StoryNode(
        id = "the_offered_glimpse",
        chapterId = "chapter_14",
        title = "The Offered Glimpse",
        illustrationId = "the_offered_glimpse",
        narrativeText = """
            "Press your hand to the stone," it says. "I'll show you instead of telling you. It
            will cost you something. It always does."

            Kaelen has done this before, once, at the binding rite, and once more than he'd like
            to admit since. He presses his palm flat against the black iron anyway, bracing for
            the ache before it arrives this time.
        """.trimIndent(),
        choices = toTheKnock("Let it in.")
    ),
    StoryNode(
        id = "c14_knock_listened",
        chapterId = "chapter_14",
        title = "What It Kept: The Knock",
        illustrationId = "the_first_true_knock",
        narrativeText = """
            The stone opens on the wrong thing. It is not the vision. It is the night of the
            first true knock, from the other side: knuckles on iron, deliberate, patient,
            and then the offer, gentle as a hand on a shoulder. Kaelen hears himself not
            answer. He hears the pause where he should have turned away, and the pause is
            longer than he remembered it.

            "You listened," the prisoner says. It is not an accusation. "Almost nobody has.
            They hear the knock and run, or they hear the offer and take it. You did
            neither. You stood in the cold and let me finish the sentence." A silence. "I
            have thought about that more than is reasonable."
        """.trimIndent(),
        choices = toTheOffice("Go on.")
    ),
    StoryNode(
        id = "c14_knock_refused",
        chapterId = "chapter_14",
        title = "What It Kept: The Knock",
        illustrationId = "the_first_true_knock",
        narrativeText = """
            The stone opens on the wrong thing. It is not the vision. It is the night of the
            first true knock, from the other side: knuckles on iron, deliberate, patient,
            and then the offer, gentle as a hand on a shoulder. Kaelen hears himself turn
            away. He hears the sentence cut off in the middle of its promise, the way a rope
            parts.

            "You left before I finished," the prisoner says. "I had been working on that
            offer for a long time. I thought it was a good one." It is not bitter. "I have
            wondered since what the end of the sentence would have done. Whether you would
            have heard it and gone anyway. It matters to me that I do not know."
        """.trimIndent(),
        choices = toTheOffice("Go on.")
    ),
    StoryNode(
        id = "c14_office_full",
        chapterId = "chapter_14",
        title = "What It Kept: The Office",
        illustrationId = "the_answer",
        narrativeText = """
            Then the stone shows him the day he answered Halvard. He is standing in the deep
            road with the old man waiting, and he says it plainly, all of it, and the
            prisoner feels the words arrive like a weight set down. "I know the sound of a
            man who has made up his mind," it says. "I have heard it a few times. Mostly
            from people who were about to be wrong." A pause. "You did not sound wrong. You
            sounded as if you could not afford to be. That is not the same, and I was never
            sure which you were."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "inside_the_memory"))
    ),
    StoryNode(
        id = "c14_office_reluctant",
        chapterId = "chapter_14",
        title = "What It Kept: The Office",
        illustrationId = "the_answer",
        narrativeText = """
            Then the stone shows him the day he answered Halvard. He is standing in the deep
            road with the old man waiting, and he says the smaller thing, until someone
            better is found, and the prisoner feels the words arrive like a bargain. "Until
            someone better," it says. "I have heard that sentence in a good many mouths. It
            is the most honest way to take a post that I know. Everyone who says it means to
            leave, and a surprising number of them stay." A pause. "I noticed that you said
            it quietly. I took it for a promise."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "inside_the_memory"))
    ),
    StoryNode(
        id = "c14_office_refusedonce",
        chapterId = "chapter_14",
        title = "What It Kept: The Office",
        illustrationId = "the_answer",
        narrativeText = """
            Then the stone shows him the day he did not answer Halvard. He is standing in
            the deep road with the old man waiting, and he says he needs time, and walks
            away from the gate toward the tunnels. The prisoner remembers the road he took.
            "You went up and sat with it," it says. "I felt the stone move, the day the
            ground remembered you. You were a long way from me, and you came back. People
            who leave and come back are more use than people who never leave. They know what
            the gate costs." A pause. "I counted the days. There were not many. It felt like
            a season."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "inside_the_memory"))
    ),
    StoryNode(
        id = "inside_the_memory",
        chapterId = "chapter_14",
        title = "Inside the Memory",
        illustrationId = "inside_the_memory",
        narrativeText = """
            The vision doesn't open gently. It opens as a fight, some fragment of memory shaped
            like the moment three centuries ago when the six wards were first cut apart from
            whatever they used to be whole, replaying itself with Kaelen standing directly in the
            middle of it. Whatever this used to be, it doesn't want to be remembered by force,
            and it fights the memory of him the way it must once have fought whatever severed it.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "memory_confrontation_encounter"
    ),
    StoryNode(
        id = "memory_confrontation_defeat",
        chapterId = "chapter_14",
        title = "The Memory Wins This Round",
        illustrationId = "inside_the_memory",
        narrativeText = """
            Whatever this fragment of memory used to fight, it still remembers how, and Kaelen
            loses ground he doesn't get back before the vision decides it's finished with him
            rather than the other way around. It doesn't kill him — a memory three centuries old
            has nothing left to kill with — it simply lets go, the same way a hand opens once
            it's made its point.

            He comes back to himself anyway, gasping, palm bleeding faintly where the iron bit
            into it. Losing didn't stop the memory from happening. It just meant he had less say
            in how it ended.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Try to make sense of what you saw.",
                nextNodeId = "what_kaelen_remembers",
                consequences = Consequences(setFlags = setOf("lost_to_the_memory_itself"))
            )
        )
    ),
    StoryNode(
        id = "what_kaelen_remembers",
        chapterId = "chapter_14",
        title = "What Kaelen Remembers",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            He comes back to himself on his knees in front of the gate, palm bleeding faintly
            where the iron bit into it, the vision already fraying at the edges the way dreams
            do. He remembers enough. Six wards, once something closer to a choice than a cage.
            Something took that choice away from all six of them at once, a very long time before
            the Ashen Order ever existed to guard the results.

            The prisoner doesn't say anything else that night. It doesn't need to, and for once,
            neither does Kaelen.
        """.trimIndent(),
        choices = vossHears(
            "Tell Voss everything, tonight, whatever it costs to say out loud.",
            mapOf(StatType.COURAGE to 12),
            Consequences(
                statDeltas = mapOf(StatType.COURAGE to 1),
                grantItemIds = listOf("shard_of_the_first_ward"),
                setFlags = setOf("told_voss_everything")
            )
        ) + vossHears(
            "Write it down instead. Some truths need to be chosen, not sprung on someone.",
            mapOf(StatType.HONOUR to 11),
            Consequences(
                statDeltas = mapOf(StatType.HONOUR to 1),
                grantItemIds = listOf("shard_of_the_first_ward"),
                setFlags = setOf("recorded_the_vision_first")
            )
        ) + Choice(
            label = "Sit with it alone a while longer.",
            nextNodeId = "c14_voss_withheld",
            consequences = Consequences(
                grantItemIds = listOf("shard_of_the_first_ward"),
                setFlags = setOf("kept_the_vision_to_himself")
            )
        )
    ),
    StoryNode(
        id = "c14_voss_heard_wary",
        chapterId = "chapter_14",
        title = "What Voss Has",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            Voss has what he saw now, all of it, and she says nothing for long enough that
            he begins to explain. "No," she says. "Let me hold it." She holds it the way
            she held the question of the signet, carefully, at arm's length. "You are
            telling me because you want me to decide what it means for the Order," she
            says, "or because you want to see what I do with it." She meets his eyes. "I
            will tell you what I intend to do with it. Nothing, for now. It is yours, and
            it is older than the Order, and neither of us has earned the right to be
            clever about it."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter14_end"))
    ),
    StoryNode(
        id = "c14_voss_heard_working",
        chapterId = "chapter_14",
        title = "What Voss Has",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            Voss has what he saw now, all of it. She puts it somewhere behind her face, and
            then, because she is Voss, she asks the practical question. "Does anything in it
            say who to look for?" He tells her it does not. "No," she says. "The Order's
            records do not go back that far. If they did, I would have read them in a cell
            with nothing else to do." She turns the idea over once. "Whatever this is, it
            was old before the Order was built to guard what came of it. That is worth
            knowing. I do not yet know what to do with it." She looks at him. "Thank you
            for not making me guess."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter14_end"))
    ),
    StoryNode(
        id = "c14_voss_heard_trusted",
        chapterId = "chapter_14",
        title = "What Voss Has",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            Voss has what he saw now, all of it. She is quiet, and the quiet does not ask
            anything of him. "That cost you," she says at last. She does not mean the
            vision. "To say it, or to write it, and then to put it in my hands and let go. I
            know what that costs." It is the nearest she has come to speaking about
            herself, and it is not that, and both of them know it. "I will carry it as
            carefully as you did. If you ever need me to forget it, say so, and I will tell
            you honestly whether I can."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter14_end"))
    ),
    StoryNode(
        id = "c14_voss_withheld",
        chapterId = "chapter_14",
        title = "Voss Does Not Ask",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            Voss does not ask. She sees him come up from the gate with the look of a man
            carrying something that does not fit in his hands, and she sees him not put it
            down, and she says nothing about either. In the morning she leaves a cup of
            something hot on the step by the door and goes to the wall. It is not a
            question. It is the shape a question would have if she had decided to wait for
            the answer.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter14_end"))
    ),
    StoryNode(
        id = "chapter14_end",
        chapterId = "chapter_14",
        title = "End of Chapter XIV — Furniture",
        illustrationId = "chapter14_threshold",
        narrativeText = """
            Whatever Kaelen decides to do with what he's seen, one thing is no longer abstract:
            the thing behind his own door isn't just a threat to be managed or a bargain to be
            refused. It's afraid, in whatever way three-hundred-year-old pressure gets to be
            afraid, and it has started treating Kaelen less like a jailer and more like the only
            ally it has left outside its own stone.

            That should worry him more than it does.

            Chapter XV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go on.",
                nextNodeId = "c15_waiting_open",
                requirements = ChoiceRequirement(requiredFlags = setOf("c13_asked_openly")),
                consequences = Consequences(clearFlags = C15_TELLING_FLAGS)
            ),
            Choice(
                label = "Go on.",
                nextNodeId = "c15_waiting_private",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c13_asked_openly")),
                consequences = Consequences(clearFlags = C15_TELLING_FLAGS)
            )
        )
    )
)

val chapter14 = com.thelastjailer.app.Chapter(
    id = "chapter_14",
    number = 14,
    title = "Chapter XIV — Furniture",
    startNodeId = "what_waits_speaks"
)
