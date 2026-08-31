package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXIII — The First to Say Yes.
 *
 * Escalates XXII's "gentle asking" beyond Kaelen's own fragment: the whole is making the same
 * patient appeal to all six, and Emberlow — named back in IX as one of the four settings Ilsevet
 * already held, "gave theirs without half this trouble" — has been alone and unwardened for eleven
 * years, far longer than Kaelen's three. Its resistance is far more fragile. This reframes the
 * threat from "stop one villain's device" to "reach five other lonely fragments before the whole
 * does," a mid-late-game plot engine with real urgency. Twenty-third and toughest combat encounter
 * yet (Cinder Straggler Captain, 310 HP — leaderless remnants of Ilsevet's faction still guarding a
 * gate nobody countermanded them from). Deliberately doesn't resolve Emberlow's fate cleanly: the
 * sibling goes quiet rather than answering yes or no, leaving real ambiguity for future chapters.
 */
val chapter23Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "word_of_emberlow",
        chapterId = "chapter_23",
        title = "Word of Emberlow",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner wakes Kaelen in the deep of the night, which it has never once done
            before. "Emberlow," it says, before he's even fully upright. "One of the others.
            She's close to saying yes."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we go. Now.\"",
                nextNodeId = "what_the_prisoner_senses",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("raced_to_emberlow")
                )
            ),
            Choice(
                label = "\"Tell me what 'close' means before we move.\"",
                nextNodeId = "what_the_prisoner_senses",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_before_racing")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_senses",
        chapterId = "chapter_23",
        title = "What the Prisoner Senses",
        illustrationId = "what_the_prisoner_senses",
        narrativeText = """
            "Emberlow's warden died alone eleven years ago, and nobody sent to replace him," it
            says. "The sibling behind that gate has had eleven years of exactly the silence I've
            had three centuries of, except she started this fight already tired. I don't know if
            she says no the way I did. I don't know if I would have, either, at eleven years."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Emberlow.", nextNodeId = "the_road_to_emberlow")
        )
    ),
    StoryNode(
        id = "the_road_to_emberlow",
        chapterId = "chapter_23",
        title = "The Road to Emberlow",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Emberlow turns out to be less a village than the memory of one — the kind of place
            that emptied out slowly enough that nobody ever quite decided to leave, and the gate
            at its heart has been unwatched long enough that moss has found its way into the
            ward-carvings.

            Whatever's happening here, it's already been happening for longer than tonight.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Look closer.", nextNodeId = "what_emberlow_kept")
        )
    ),
    StoryNode(
        id = "what_emberlow_kept",
        chapterId = "chapter_23",
        title = "What Emberlow Kept",
        illustrationId = "what_emberlow_kept",
        narrativeText = """
            They're not the only ones who noticed Emberlow's silence. A handful of cinder-grey
            stragglers — the last of Ilsevet's people, leaderless and apparently still following
            standing orders nobody's countermanded — have made camp at the gate's threshold, not
            guarding it so much as waiting to see what it does next.

            One of them notices Kaelen before he's decided how to approach this quietly.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the fallen warden's token from where it's lain untouched.",
                nextNodeId = "what_waits_at_the_threshold",
                consequences = Consequences(grantItemIds = listOf("emberlow_wardens_token"))
            )
        )
    ),
    StoryNode(
        id = "what_waits_at_the_threshold",
        chapterId = "chapter_23",
        title = "What Waits at the Threshold",
        illustrationId = "what_waits_at_the_threshold",
        narrativeText = """
            Leaderless doesn't mean harmless. Whoever's left in charge here has spent weeks with
            nothing to do but get very good at defending a gate they don't fully understand, and
            they don't intend to let a stranger walk up to it uncontested.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "emberlow_stragglers_encounter"
    ),
    StoryNode(
        id = "reaching_emberlow",
        chapterId = "chapter_23",
        title = "Reaching Emberlow",
        illustrationId = "reaching_emberlow",
        narrativeText = """
            The stragglers break before the gate does. Up close, Emberlow's ward is barely holding
            — not shattered, not breached, just tired in a way Kaelen recognizes uncomfortably well
            from three years of standing watch over his own. Whatever's on the other side has been
            listening to every word the whole has said to it, alone, for eleven years, with nobody
            standing where Voss and Kaelen have stood for him.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"You don't have to decide alone anymore. I'm not leaving until you know that.\"",
                nextNodeId = "chapter23_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 21)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("refused_to_leave_emberlow")
                )
            ),
            Choice(
                label = "\"Whatever you choose, it should be a choice. Not just the only voice you've had in eleven years.\"",
                nextNodeId = "chapter23_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 20)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("insisted_on_real_choice")
                )
            ),
            Choice(
                label = "Stay as long as it takes, and say nothing until she's ready to hear it.",
                nextNodeId = "chapter23_end",
                consequences = Consequences(setFlags = setOf("waited_at_emberlow"))
            )
        )
    ),
    StoryNode(
        id = "chapter23_end",
        chapterId = "chapter_23",
        title = "End of Chapter XXIII — The First to Say Yes",
        illustrationId = "chapter23_threshold",
        narrativeText = """
            Emberlow's sibling doesn't answer that night, or the next. What she does do, by the
            third morning, is stop answering the whole entirely — a silence Kaelen chooses to read
            as a maybe rather than a yes, mostly because the alternative is worse than he can
            afford to sit with right now.

            Voss says what neither of them wants to say out loud: Emberlow was the nearest. It
            won't be the last one to go quiet. Somewhere out there, three more fragments are
            having some version of this same conversation, alone, and Kaelen can't be in four
            places at once.

            Chapter XXIV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "word_of_greymoor")
        )
    )
)

val chapter23 = com.thelastjailer.app.Chapter(
    id = "chapter_23",
    number = 23,
    title = "Chapter XXIII — The First to Say Yes",
    startNodeId = "word_of_emberlow"
)
