package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXI — What Six Used to Be.
 *
 * Pays off the promise the prisoner made in XIV ("ask me again after there are fewer of us left
 * to lose") and the grief it showed in XX: it finally answers what it is. The six wards were never
 * six separate creatures — they were one being, torn deliberately into six pieces a very long time
 * ago, sealed apart so it could never again be whole enough to do whatever it had already done.
 * Three centuries of separation gave each fragment its own distinct identity; the "voice" behind
 * the archivist's notes (XIX-XX) is what's left of the original whole, patient and still reaching
 * to reassemble itself. This recontextualizes Ilsevet's entire plan: the seventh door was never a
 * new weapon, it was an attempt to undo an ancient act of self-defense, and "four settings" was
 * dangerous specifically because each filled setting brought the original whole closer to erasing
 * six people to make room for something that stopped existing a very long time ago.
 *
 * Deliberately no combat and no new item this chapter — pure reveal, giving XX's grief room to
 * land before any new escalation. This is the direct payoff of the arc opened in XIV and advanced
 * through XIX-XX rather than a new tonal swing, so no separate judgment-call flag.
 */
val chapter21Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_prisoner_answers",
        chapterId = "chapter_21",
        title = "The Prisoner Answers",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            It takes three more nights before the prisoner says anything else, and when it finally
            does, it doesn't wait to be asked twice.

            "You wanted a name," it says. "I don't have one the way you mean it. But I can tell
            you what I am, if you're still willing to hear it. Most people who ask don't actually
            want the answer they think they want."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm still asking.\"",
                nextNodeId = "what_it_was",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("still_asking")
                )
            ),
            Choice(
                label = "\"Tell me as much as you think I can carry.\"",
                nextNodeId = "what_it_was",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_for_measured_truth")
                )
            )
        )
    ),
    StoryNode(
        id = "what_it_was",
        chapterId = "chapter_21",
        title = "What It Was",
        illustrationId = "what_it_was_ch21",
        narrativeText = """
            "We were not always six," it says. "There was one, once — old enough that 'once'
            barely means anything next to it. Something tore it into six pieces on purpose, a very
            long time before your kingdom or the Order or anyone who could still tell you why. Not
            to punish it. To make sure it could never again be whole enough to do whatever it had
            already done."

            It lets that settle before it goes on. "Three centuries alone changes a person, even a
            person who used to be one-sixth of someone else. I am not the piece I was when the
            tearing happened. None of us are. That's what Ilsevet's frame actually threatens — not
            freeing six prisoners. Erasing six people to make room for something that stopped
            existing longer ago than any of us can properly measure."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let Voss hear this too.", nextNodeId = "voss_hears_it_too")
        )
    ),
    StoryNode(
        id = "voss_hears_it_too",
        chapterId = "chapter_21",
        title = "Voss Hears It Too",
        illustrationId = "voss_hears_it_too",
        narrativeText = """
            Voss has gone very quiet by the time the prisoner finishes talking, the particular
            quiet of someone recalculating everything she thought she understood about her own
            life's work. "The Order spent three centuries calling your kind 'prisoners,'" she says
            finally. "Never once, in any record I've read, did anyone stop to ask what that
            actually made you."

            The prisoner doesn't answer that. Kaelen isn't sure it has an answer it likes.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask about the archivist's voice.", nextNodeId = "the_voice_that_wrote")
        )
    ),
    StoryNode(
        id = "the_voice_that_wrote",
        chapterId = "chapter_21",
        title = "The Voice That Wrote",
        illustrationId = "the_voice_that_wrote",
        narrativeText = """
            "The hand that finished the archivist's notes," it says, "is what's left of the whole.
            Not gone — scattered, weakened, patient in the particular way something can only be
            patient after three hundred years of not being able to do anything else. It wants to
            be whole again. I understand that better than I'd like to. I'm just no longer sure
            'whole again' means what it thinks it means, from where I've ended up standing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Decide what to say.", nextNodeId = "what_the_sixth_wants")
        )
    ),
    StoryNode(
        id = "what_the_sixth_wants",
        chapterId = "chapter_21",
        title = "What the Sixth Wants",
        illustrationId = "what_the_sixth_wants",
        narrativeText = """
            Kaelen has spent the better part of two years thinking of the thing behind his door as
            a problem to be managed, then a temptation to resist, then — more recently — an uneasy
            ally with its own reasons for wanting Ilsevet stopped. He isn't sure what to call it
            now that it's told him this much.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we make sure nobody gets to erase you. Not Ilsevet, not the whole, not anyone.\"",
                nextNodeId = "chapter21_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 19)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("vowed_to_protect_the_prisoner")
                )
            ),
            Choice(
                label = "\"You get to decide what 'you' means from here. That's not something I get a vote on.\"",
                nextNodeId = "chapter21_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 18)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("affirmed_its_own_choice")
                )
            ),
            Choice(
                label = "Say nothing yet. Some things need longer than a conversation to actually answer.",
                nextNodeId = "chapter21_end",
                consequences = Consequences(setFlags = setOf("left_the_question_open"))
            )
        )
    ),
    StoryNode(
        id = "chapter21_end",
        chapterId = "chapter_21",
        title = "End of Chapter XXI — What Six Used to Be",
        illustrationId = "chapter21_threshold",
        narrativeText = """
            Whatever Kaelen decides to say, the prisoner doesn't ask him to promise anything he
            can't keep. It's had three centuries to learn the difference between comfort and
            certainty, and it isn't interested in pretending it can't tell them apart.

            What it does ask, quietly, once the conversation is otherwise finished: to be told, if
            the day ever comes, before anyone decides on its behalf what "whole again" is worth.
            Kaelen agrees before he's fully thought through what agreeing might eventually cost
            him.

            Ilsevet built a door meant to end six people by making them one thing again. Whatever's
            left of that original whole is still out there, patient, unfinished, and apparently
            not done reaching for the same door by other means.

            Chapter XXII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_prisoner_troubled")
        )
    )
)

val chapter21 = com.thelastjailer.app.Chapter(
    id = "chapter_21",
    number = 21,
    title = "Chapter XXI — What Six Used to Be",
    startNodeId = "the_prisoner_answers"
)
