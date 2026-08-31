package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXII — Refused Gently.
 *
 * Delivers on XXI's closing line: the whole, having lost Ilsevet as an intermediary, tries "other
 * means" — reaching directly into the prisoner's mind, not with threats but with something far
 * harder to refuse: an honest, patient reminder of what wholeness used to feel like, offered by the
 * one voice that actually understands three centuries of loneliness. This inverts the temptation
 * pattern from IV/V/VIII (Kaelen refusing power offered to him) into Kaelen supporting an ally
 * refusing intimacy offered to it — a genuinely different kind of stakes. Reuses XIV's vision-combat
 * device (a symbolic, non-physical confrontation) for the twenty-second and toughest encounter yet
 * (The Patient Voice, 300 HP — the first to cross that threshold), framed explicitly as refusing an
 * outstretched hand rather than winning a fight. Deliberately no new item — the chapter's currency
 * is emotional support, not loot.
 */
val chapter22Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_prisoner_troubled",
        chapterId = "chapter_22",
        title = "The Prisoner, Troubled",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Something changes in the days after the prisoner tells Kaelen what it is. It answers
            when spoken to. It stops volunteering anything on its own — a silence that feels less
            like withdrawal and more like someone listening very hard to something else.

            "You're somewhere else," Kaelen says, the fourth night it happens.

            "I'm being asked something," it admits, finally. "Gently, this time. That's worse than
            I expected it to be."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Asked what?\"",
                nextNodeId = "what_it_confesses",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("asked_directly_ch22")
                )
            ),
            Choice(
                label = "\"Take your time. I'm listening whenever you're ready.\"",
                nextNodeId = "what_it_confesses",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("offered_patience_ch22")
                )
            )
        )
    ),
    StoryNode(
        id = "what_it_confesses",
        chapterId = "chapter_22",
        title = "What It Confesses",
        illustrationId = "what_it_confesses",
        narrativeText = """
            "It doesn't threaten. It doesn't bargain the way Ilsevet's people did. It just...
            remembers, out loud, what it was like before the tearing — being whole, being certain,
            never once being lonely in three hundred years because there was no 'alone' to be. It
            asks if I miss that. It already knows the answer. That's what makes it difficult."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Decide what to offer.", nextNodeId = "what_kaelen_offers_first")
        )
    ),
    StoryNode(
        id = "what_kaelen_offers_first",
        chapterId = "chapter_22",
        title = "What Kaelen Offers First",
        illustrationId = "what_kaelen_offers_first",
        narrativeText = """
            Kaelen doesn't have an argument that beats three hundred years of loneliness spoken
            back at someone by the one voice that would actually understand it. He has, at best,
            the only thing he's ever reliably had to offer: showing up.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Let me stand with you while you answer it.\"",
                nextNodeId = "into_the_asking",
                consequences = Consequences(setFlags = setOf("stood_with_it_in_the_asking"))
            ),
            Choice(
                label = "\"Whatever you decide, I won't think less of you for how hard this is.\"",
                nextNodeId = "into_the_asking",
                consequences = Consequences(setFlags = setOf("reassured_without_conditions"))
            )
        )
    ),
    StoryNode(
        id = "into_the_asking",
        chapterId = "chapter_22",
        title = "Into the Asking",
        illustrationId = "into_the_asking",
        narrativeText = """
            Kaelen presses his branded palm to the black iron the way he has twice before, and
            this time the vision doesn't open as a memory or a battlefield. It opens as an
            invitation — warm, patient, entirely without malice, and somehow more dangerous for
            it. Refusing it doesn't feel like winning a fight. It feels like closing a door on
            someone who never once raised their voice.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "patient_voice_encounter"
    ),
    StoryNode(
        id = "what_it_costs_to_refuse",
        chapterId = "chapter_22",
        title = "What It Costs to Refuse",
        illustrationId = "what_it_costs_to_refuse",
        narrativeText = """
            The vision lets go eventually, the way a hand lets go when it finally accepts the
            answer is no. The prisoner is quiet for a long time afterward, and when it finally
            speaks, its voice is steadier than Kaelen has heard it in weeks.

            "I said no," it says. "I don't know if I'll be able to say it again next time. I don't
            know how many more times it intends to ask."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Decide what to commit to.", nextNodeId = "what_kaelen_commits_to")
        )
    ),
    StoryNode(
        id = "what_kaelen_commits_to",
        chapterId = "chapter_22",
        title = "What Kaelen Commits To",
        illustrationId = "what_kaelen_commits_to",
        narrativeText = """
            Kaelen has spent this entire year learning that patience is the one weapon nobody
            he's fought has ever run out of — not Ilsevet, not the Order, and apparently not
            whatever's left of the whole either. He isn't sure he can out-wait something that's
            already waited three centuries. He can, at least, decide what he does with the time in
            between.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then I'll be here every single time it asks. However many times that takes.\"",
                nextNodeId = "chapter22_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 20)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("vowed_constant_vigilance")
                )
            ),
            Choice(
                label = "\"You don't have to win this alone every time. Let me carry some of it.\"",
                nextNodeId = "chapter22_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 19)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("vowed_to_share_the_burden")
                )
            ),
            Choice(
                label = "Say nothing. Just stay until morning.",
                nextNodeId = "chapter22_end",
                consequences = Consequences(setFlags = setOf("simply_stayed"))
            )
        )
    ),
    StoryNode(
        id = "chapter22_end",
        chapterId = "chapter_22",
        title = "End of Chapter XXII — Refused Gently",
        illustrationId = "chapter22_threshold",
        narrativeText = """
            Morning comes the way it always does down here, which is to say: by the numbers, since
            the sun has no say in it. Kaelen counts this as a victory anyway, small and exhausting
            and entirely uncelebratable to anyone who wasn't there for it.

            Voss, when he tells her, doesn't ask if the prisoner is safe now. She's learned enough
            by this point to know "now" was never going to be the operative word. What she asks
            instead is how many more nights like this one Kaelen thinks either of them has left in
            them.

            He doesn't have an answer. He has, for the moment, a door still standing, a friend who
            said no, and a very long, very patient silence on the other side of both, waiting to
            ask again.

            Chapter XXIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, before it asks again.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter22 = com.thelastjailer.app.Chapter(
    id = "chapter_22",
    number = 22,
    title = "Chapter XXII — Refused Gently",
    startNodeId = "the_prisoner_troubled"
)
