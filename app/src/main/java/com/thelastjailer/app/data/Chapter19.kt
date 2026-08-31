package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XIX — Already Written.
 *
 * Reveals that Ilsevet, whatever her fate at the end of XVIII, was never the whole story: the
 * seventh-door frame's design wasn't hers — she found it already complete in a dead archivist's
 * workshop eleven years before the story began, in a hand that "got surer of the frame the longer
 * it worked on it," which the prisoner (continuing its role since XIV) reads as a description of
 * something remembering rather than someone learning. Selvane's investigation into the workshop
 * finds five more sets of plans addressed by name to people who hadn't been born when they were
 * written — including Kaelen's own name, tying directly back to Ilsevet's "like they'd met before"
 * greeting in Chapter X and reframing the entire Ilsevet arc as a symptom of something older and
 * still active. Deliberately no new combat encounter — a lore/investigation chapter matching the
 * pacing pattern established by III, XIII, and XVI, giving the reveal room after XVIII's duel
 * rather than immediately escalating into another fight.
 *
 * Judgment call flagged for review: this reframes the entire story's antagonist structure —
 * Ilsevet's arc, just closed in XVIII, becomes a first movement rather than the whole piece, and
 * Kaelen is now confirmed to have been specifically anticipated by whoever authored the original
 * design. This sets the scope for chapters beyond XIX and is worth a look before merging.
 */
val chapter19Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_ilsevet_knew",
        chapterId = "chapter_19",
        title = "What Ilsevet Knew",
        illustrationId = "threshold_ahead",
        narrativeText = """
            Whatever else changes about what happens to her, Ilsevet has exactly one more honest
            conversation left in her, and she spends it on something Kaelen doesn't expect.

            "The frame's design wasn't mine," she says. "I found the first pages of it in a dead
            archivist's collection eleven years ago, already drawn, already complete down to
            details I hadn't worked out myself yet. I told myself it was luck. I have had a very
            long time since then not to believe that."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Who drew them, if not you?\"",
                nextNodeId = "the_dead_archivist",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_about_the_archive")
                )
            ),
            Choice(
                label = "Let her finish in her own order.",
                nextNodeId = "the_dead_archivist",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("let_her_finish")
                )
            )
        )
    ),
    StoryNode(
        id = "the_dead_archivist",
        chapterId = "chapter_19",
        title = "The Dead Archivist",
        illustrationId = "the_dead_archivist",
        narrativeText = """
            "I don't know. I never found a name, only a workshop nobody local remembered him
            keeping, and notes in a hand that got smaller and more certain the deeper into the
            design it went — like whoever wrote them understood the frame better the longer they
            spent almost finishing it." She looks at Kaelen directly for the first time since the
            duel. "I used to think I found him. I've started to think it's closer to the truth
            that I was allowed to."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Bring this to the prisoner.", nextNodeId = "the_prisoners_reaction")
        )
    ),
    StoryNode(
        id = "the_prisoners_reaction",
        chapterId = "chapter_19",
        title = "The Prisoner's Reaction",
        illustrationId = "the_prisoners_reaction",
        narrativeText = """
            The prisoner goes quiet for longer than it ever has, once Kaelen repeats it back at
            the gate that night. "A hand that understood the frame better the longer it worked,"
            it says finally, "is not a description of a person learning. It's a description of
            something remembering."

            It doesn't finish the thought. For the first time, Kaelen gets the distinct sense it's
            afraid of finishing it.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Send word to Selvane.", nextNodeId = "what_selvane_finds")
        )
    ),
    StoryNode(
        id = "what_selvane_finds",
        chapterId = "chapter_19",
        title = "What Selvane Finds",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            Selvane, when word reaches her, doesn't waste time being skeptical — not after
            Ashfall, not after everything her own archives already confirmed about Ashwell. What
            her people find at the dead archivist's old workshop, three weeks and a hard ride
            later, isn't proof of a person at all.

            It's five more sets of frame notes, in the same hand, each one addressed — by name, in
            careful, deliberate script — to someone who hadn't been born yet when the ink dried.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Read the names.",
                nextNodeId = "the_addressed_names",
                consequences = Consequences(grantItemIds = listOf("archivists_frame_notes"))
            )
        )
    ),
    StoryNode(
        id = "the_addressed_names",
        chapterId = "chapter_19",
        title = "The Addressed Names",
        illustrationId = "the_addressed_names",
        narrativeText = """
            Voss reads the names aloud, one at a time, and stops cold on the fourth. Thessaly's
            isn't a surprise — Fenmoor was always going to be found eventually. It's the fifth
            name that stops the room: Kaelen's own, written in ink dry a decade before he ever
            stood in front of the black door.

            Whoever drew those plans didn't just anticipate the seventh door. They anticipated
            him, specifically, standing exactly where he's standing now.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then whoever it is, they've been waiting on me specifically. Let them keep waiting.\"",
                nextNodeId = "chapter19_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 17)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_the_prophecy")
                )
            ),
            Choice(
                label = "\"If I was always meant to stand here, I intend to make sure I'm the one who decides what that means.\"",
                nextNodeId = "chapter19_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 16)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("claimed_his_own_meaning")
                )
            ),
            Choice(
                label = "Say nothing. There isn't an answer that fits in a sentence.",
                nextNodeId = "chapter19_end",
                consequences = Consequences(setFlags = setOf("left_it_unanswered"))
            )
        )
    ),
    StoryNode(
        id = "chapter19_end",
        chapterId = "chapter_19",
        title = "End of Chapter XIX — Already Written",
        illustrationId = "chapter19_threshold",
        narrativeText = """
            Ilsevet's arithmetic is finished, one way or another. Whatever's left of her faction
            without her to run it is Selvane's problem now, or the Order's, or nobody's — Kaelen
            finds he genuinely doesn't know which, and isn't sure it matters as much as he once
            thought it would.

            What matters is five names, one workshop, and a hand that got surer of the frame the
            longer it worked on something it wouldn't live to see finished. Ilsevet spent fifteen
            years thinking she chose this. Kaelen isn't sure anymore that either of them ever
            really did.

            Chapter XX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter19 = com.thelastjailer.app.Chapter(
    id = "chapter_19",
    number = 19,
    title = "Chapter XIX — Already Written",
    startNodeId = "what_ilsevet_knew"
)
