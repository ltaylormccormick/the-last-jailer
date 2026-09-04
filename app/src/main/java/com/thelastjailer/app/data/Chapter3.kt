package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter III — What the Door Remembers.
 *
 * Resolves Chapter II's cliffhanger (does Kaelen take the office, and does he take it fully or
 * reluctantly, each with its own binding scene), gives the thing behind the gate its first real
 * voice instead of just a knock (and Kaelen's reaction to what happens next differs depending on
 * whether he listened to it or refused it outright), and introduces the first external antagonist
 * — someone from Kaelen's own world, above ground, who has heard that a jailer has been named and
 * does not consider that good news. The chapter-ending confrontation with her is a genuine
 * three-way fork: standing his ground, arguing for mercy, and stalling for time each earn a
 * distinct reaction from Voss, not just a distinct stat. This is the last free chapter, so it ends
 * on a real hook rather than a resolution: a named threat now knows Kaelen exists, and knows what
 * he's guarding.
 */
val chapter3Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_answer",
        chapterId = "chapter_3",
        title = "The Answer",
        illustrationId = "the_answer",
        narrativeText = """
            Halvard waits the way old men wait for weather: patient, certain it will come whether
            or not he's ready for it.

            Kaelen thinks of the cloak he doesn't wear anymore. Of ten steps taken and
            reconsidered on a ruined road. Of a door that knocked for him specifically, out of
            everyone who might have walked that way before him and kept walking.

            "Say it plainly," Halvard says. "I'm too old for a man who needs three sentences to
            say yes, and this gate is too tired to wait for him to find them."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I accept the office. All of it.\"",
                nextNodeId = "the_binding_rite_full",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("accepted_fully")
                )
            ),
            Choice(
                label = "\"I'll hold the gate. Until someone better is found.\"",
                nextNodeId = "the_binding_rite_reluctant",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("accepted_reluctantly")
                )
            )
        )
    ),
    StoryNode(
        id = "the_binding_rite_full",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really: no crown, no witnesses but one tired dwarf and
            a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer than a
            finger and presses it flat against the black stone, and Kaelen doesn't flinch from it.
            He finds, a little to his own surprise, that he doesn't want to.

            The wards that still hold drink the offering like they've been thirsty for it. For one
            heartbeat, Kaelen feels the full weight of the thing on the other side of the gate:
            not its shape, not its name, only its patience, vast and undiminished by however many
            centuries it has spent waiting for exactly this. He meets it anyway. Better to know
            what he's agreed to hold than to hold it blind.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand, and it doesn't feel like a wound so much as a
            promise finally written down where it can't be taken back.
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
        id = "the_binding_rite_reluctant",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really: no crown, no witnesses but one tired dwarf and
            a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer than a
            finger and presses it flat against the black stone. Kaelen holds still and lets him,
            jaw set, the way he used to hold still for a battlefield surgeon: not because it
            doesn't hurt, but because flinching never once made anything hurt less.

            The wards that still hold drink the offering like they've been thirsty for it. For one
            heartbeat, Kaelen feels the full weight of the thing on the other side of the gate,
            not its shape, not its name, only its patience, vast and undiminished by however many
            centuries it has spent waiting for exactly this. He doesn't welcome the feeling. He
            just refuses to look away from it first.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand. Kaelen flexes his hand once, testing the ache of
            it, and says nothing. Some promises don't need saying twice to be kept.
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
            That night, if the deep road can even be said to have nights, something knocks from
            the other side of the gate for the first time since the black door. Knuckles this
            time, not stone against stone. Deliberate. Patient, the way the whole thing has always
            been patient.

            Then a voice, or the memory of one, shaped out of the same pressure that used to just
            be a knock: it knows his name. It knows what his cloak used to mean. It offers, almost
            gently, to give it back to him: the silver, the rank, the version of Kaelen that never
            failed anyone. All he would have to do is listen a little longer.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen to what it's offering.",
                nextNodeId = "voices_above_after_listening",
                consequences = Consequences(setFlags = setOf("listened_to_the_prisoner"))
            ),
            Choice(
                label = "Turn away before it finishes.",
                nextNodeId = "voices_above_after_refusing",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_the_prisoner")
                )
            )
        )
    ),
    StoryNode(
        id = "voices_above_after_listening",
        chapterId = "chapter_3",
        title = "Voices Above",
        illustrationId = "voices_above",
        narrativeText = """
            Kaelen doesn't answer it, in the end, but he doesn't stop listening as fast as he
            should have either, and the silence after feels different for that. Heavier. Like a
            door he meant to close and only pulled most of the way shut.

            Days blur together this far under the earth, and it's Halvard who notices the next
            change first: a vibration in the stone that has nothing to do with the gate, coming
            from directly overhead. Boots, more than a few, moving with the unhurried confidence
            of people who think they're the only ones with a claim on what's up there.

            "Nobody's climbed that road in longer than I've kept this post," Halvard says.
            "Someone has, now." Kaelen is almost grateful for the interruption. It's easier to
            think about boots overhead than about how much of that offer he let himself actually
            hear.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Climb back up to the black door.", nextNodeId = "the_surface_again")
        )
    ),
    StoryNode(
        id = "voices_above_after_refusing",
        chapterId = "chapter_3",
        title = "Voices Above",
        illustrationId = "voices_above",
        narrativeText = """
            Kaelen turns from it before the offer finishes, and the silence that follows feels
            earned rather than heavy, though some small, tired part of him keeps turning the words
            over anyway, the way a tongue keeps finding a broken tooth.

            Days blur together this far under the earth, and it's Halvard who notices the next
            change first: a vibration in the stone that has nothing to do with the gate, coming
            from directly overhead. Boots, more than a few, moving with the unhurried confidence
            of people who think they're the only ones with a claim on what's up there.

            "Nobody's climbed that road in longer than I've kept this post," Halvard says.
            "Someone has, now." Kaelen is glad of somewhere else to point his attention. Refusing
            was the easy part. Not wondering what he refused is proving harder.
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
            Rain again, or still; Kaelen has honestly lost track of how long he's been below. The
            ancient tree looks exactly as he left it. The black door does not.

            Four riders wait beside it, colours he doesn't recognize, and a fifth figure standing
            close enough to the iron to have already tried it. She turns before Kaelen's boots
            clear the roots. "There you are," she says, as if she'd been expecting him
            specifically. "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist
            had opened out here, and that it had found itself a keeper."
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
            ones. Your gate has had three centuries to be the second kind and failed at it. We're
            here to finish the job properly: brick, fire, and enough of both that nothing behind
            that door ever knocks again. You, and whatever dwarves are left down there, are
            welcome to stand aside."

            She hasn't drawn a weapon. She hasn't needed to. The four behind her have, and they
            hold them the unhurried way people hold weapons they expect to use exactly once and
            correctly.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Stand aside yourself. This isn't yours to bury.\"",
                nextNodeId = "chapter3_end_stood_ground",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("stood_his_ground")
                )
            ),
            Choice(
                label = "\"There's a man alive behind that gate who deserves better than fire.\"",
                nextNodeId = "chapter3_end_argued_mercy",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("argued_for_mercy")
                )
            ),
            Choice(
                label = "\"Give me time to send word below before you try.\"",
                nextNodeId = "chapter3_end_stalled",
                consequences = Consequences(setFlags = setOf("stalled_for_time"))
            )
        )
    ),
    StoryNode(
        id = "chapter3_end_stood_ground",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Something in Voss's expression changes, subtle but real, the look of a woman revising
            an estimate she'd already written down in ink. She doesn't back away from the door.
            She does, for the first time, look at Kaelen like an obstacle rather than an
            inconvenience.

            "Most people who tell the Order to stand aside are already running while they say
            it," she says, swinging back up onto her horse without hurrying. "You're not running.
            That's either very brave or very stupid, jailer, and I've learned not to bet against
            either one too quickly." She studies him a moment longer, weighing something she
            doesn't share. "The Ashen Order doesn't send one rider and four swords for a rumour.
            We send them for a confirmation. Congratulations. You've just been confirmed, and I
            don't think you're going to enjoy what comes next."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and for the first time since the rain started, standing his ground doesn't
            feel like it was the easy half of the fight.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    ),
    StoryNode(
        id = "chapter3_end_argued_mercy",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Voss listens to the whole of it without interrupting, which somehow makes the silence
            after worse than any argument would have. "Mercy," she says at last, turning the word
            over like a coin she isn't sure is real. "The Order stopped weighing mercy against
            what's behind doors like this one a long time before I joined it. I'm not unconvinced
            you believe what you just said. I am entirely unconvinced it changes anything."

            She swings back up onto her horse, and for just a moment, before her expression closes
            again, something almost like doubt crosses it. "You'll want to hear this from me and
            not from someone with less patience for a plea like yours," she says. "The Ashen Order
            doesn't send one rider and four swords for a rumour. We send them for a confirmation.
            Congratulations, jailer. You've just been confirmed."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and he finds himself hoping, against most of what he knows about people like
            her, that the doubt he saw was real.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    ),
    StoryNode(
        id = "chapter3_end_stalled",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Voss doesn't even pretend to consider it. "Send word to whom, exactly?" she says,
            almost gently, the way a person points out a lie too small to be worth real anger
            over. "There's no one left down there to send it to but an old dwarf and a door. I
            admire the attempt. I don't reward it."

            She swings back up onto her horse without hurrying, unbothered, already thinking past
            him. "You'll want to hear this from me and not from someone with less patience," she
            says. "The Ashen Order doesn't send one rider and four swords for a rumour. We send
            them for a confirmation. Congratulations, jailer. You've just been confirmed, and
            you've bought yourself less time than you think you have."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and the silence afterward feels less like a reprieve than like a held breath
            he doesn't get to let out yet.

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
