package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXX — Six, Not One.
 *
 * Major structural pivot, flagged clearly in the PR: this chapter closes the primary conflict the
 * entire game has built toward since the opening chapters — the whole's three-century campaign to
 * reassemble itself, whether by persuasion (XXII, XXIII, XXV) or by force (XXIX). Choosing to seek
 * the ending outright versus insisting on an alternative to a killing blow changes how directly the
 * prisoner answers where to find it, not just a stat. It does not close every thread the story has
 * raised (Ilsevet's ultimate fate, the Cinder faction's future, Emberlow and Greymoor's
 * still-undecided fragments, and Kaelen and Voss's own future are all left open, mirroring how XVIII
 * closed the "hunt Ilsevet" arc while deliberately leaving other threads unresolved) — but the core
 * antagonist force's arc reaches a real, considered resolution here rather than being left open
 * indefinitely. The resolution is deliberately not a kill: consistent with the story's established
 * theme that refusal and consent, not violence, are what actually work against the whole (XXII
 * inverted temptation into supported refusal; XXVIII's Wraithspire succeeded through presence, not
 * force), the ending offers the whole a genuine alternative to both eternal loneliness and coerced
 * reunion — six fragments choosing to stay in each other's lives without losing what each has
 * separately become. Final and toughest combat of the arc (The Whole, At Full Reach, 450 HP) comes
 * first, because the offer only means anything once the whole knows, directly, that it can no longer
 * simply take what it wants.
 */
val chapter30Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_must_happen_next",
        chapterId = "chapter_30",
        title = "What Must Happen Next",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            "It reached all the way into you and lost," the prisoner says, the morning after. "It
            has never once done that before, not in three hundred years. Whatever it is now, it
            isn't done being afraid of that. I don't think it can afford to just wait us out
            anymore." There's something almost like hope threaded through the observation, thin
            but unmistakably there.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we don't wait either. We find it, and we finish this.\"",
                nextNodeId = "where_it_can_be_found_seek",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("chose_to_seek_the_ending")
                )
            ),
            Choice(
                label = "\"Finish it how? I won't spend this on a killing blow if there's another way.\"",
                nextNodeId = "where_it_can_be_found_another_way",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("insisted_on_another_way")
                )
            )
        )
    ),
    StoryNode(
        id = "where_it_can_be_found_seek",
        chapterId = "chapter_30",
        title = "Where It Can Be Found",
        illustrationId = "where_it_can_be_found",
        narrativeText = """
            The prisoner doesn't flinch from his readiness to end this, and answers with the same
            directness he offered it. "The Sundering Ground," it says. "That's where it's rooted
            deepest, the forgetting we fought there was always just the outermost layer of it. If
            it's anywhere close to a single place after three centuries of being scattered on
            purpose, it's there. Where it started. Where it still, somewhere underneath
            everything else, remembers being whole."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go back to where it began.", nextNodeId = "the_road_back_to_the_sundering_ground")
        )
    ),
    StoryNode(
        id = "where_it_can_be_found_another_way",
        chapterId = "chapter_30",
        title = "Where It Can Be Found",
        illustrationId = "where_it_can_be_found",
        narrativeText = """
            The prisoner takes the demand seriously enough to answer it before it answers the
            first question at all. "I don't know yet, either. But I know where to start looking
            for an answer that isn't a killing blow." "The Sundering Ground," it says. "That's
            where it's rooted deepest, the forgetting we fought there was always just the
            outermost layer of it. If it's anywhere close to a single place after three centuries
            of being scattered on purpose, it's there. Where it started. Where it still,
            somewhere underneath everything else, remembers being whole."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go back to where it began.", nextNodeId = "the_road_back_to_the_sundering_ground")
        )
    ),
    StoryNode(
        id = "the_road_back_to_the_sundering_ground",
        chapterId = "chapter_30",
        title = "The Road Back to the Sundering Ground",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Voss rides beside him without needing to be asked, the way she has since Fenmoor. The
            prisoner is quieter than Kaelen has ever known it, holding, he suspects, every
            conversation it's had with every other fragment these past weeks somewhere behind its
            silence, turning them over one more time before whatever happens next.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep going.", nextNodeId = "what_gathers_at_the_ground")
        )
    ),
    StoryNode(
        id = "what_gathers_at_the_ground",
        chapterId = "chapter_30",
        title = "What Gathers at the Ground",
        illustrationId = "what_gathers_at_the_ground",
        narrativeText = """
            They aren't alone when they reach the hillside. A rider from Wraithspire is waiting
            with a message from the Vigil-Captain, brief and plainly meant: whatever happens
            here, the sixth gate stands with whoever's trying to end this without erasing anyone
            to do it. Emberlow and Greymoor send nothing, because they still haven't decided
            anything to send, and for the first time, that silence doesn't feel like failure. It
            feels like two people still being allowed to take their time.

            Only Duskmere is entirely absent, and everyone standing on this hillside feels the
            shape of that absence exactly as sharply as they should.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Face what's waiting underneath.", nextNodeId = "the_last_reach")
        )
    ),
    StoryNode(
        id = "the_last_reach",
        chapterId = "chapter_30",
        title = "The Last Reach",
        illustrationId = "the_last_reach",
        narrativeText = """
            It doesn't hide this time, doesn't send a splinter or a shape wearing borrowed grief.
            The whole meets them at full reach, everything three centuries of scattering has left
            it able to gather in one place, one last time, because it has finally understood
            there won't be a second chance to take what it wants by force.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "the_last_reach_encounter"
    ),
    StoryNode(
        id = "what_the_whole_finally_hears",
        chapterId = "chapter_30",
        title = "What the Whole Finally Hears",
        illustrationId = "what_the_whole_finally_hears",
        narrativeText = """
            It doesn't break so much as stop reaching, the fight simply goes out of it, all at
            once, the way it must have felt three hundred years ago when six pieces first
            realized fighting each other wasn't the same as fighting to stay whole. For the first
            time since any of this began, it isn't asking, persuading, or taking. It's listening.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Say what needs saying.", nextNodeId = "what_kaelen_offers_at_the_end")
        )
    ),
    StoryNode(
        id = "what_kaelen_offers_at_the_end",
        chapterId = "chapter_30",
        title = "What Kaelen Offers at the End",
        illustrationId = "what_kaelen_offers_at_the_end",
        narrativeText = """
            Kaelen has spent three years learning exactly one thing well enough to bet everything
            on it now: that being separate was never the whole's real enemy. Being alone was. He
            says the only thing he has left to offer, and means every word of it, aware this is
            likely the only chance he'll ever get to say it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Earn every one of us, honestly, the way you never once tried to before — or lose all six of us for good. That's the only deal left on the table.\"",
                nextNodeId = "chapter30_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 28)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("demanded_it_be_earned"),
                    grantItemIds = listOf("promise_of_the_six")
                )
            ),
            Choice(
                label = "\"You don't have to be one thing again to stop being alone. Six people who choose to stay close is not the same as losing. I promise you that.\"",
                nextNodeId = "chapter30_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 27)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("offered_connection_without_merging"),
                    grantItemIds = listOf("promise_of_the_six")
                )
            ),
            Choice(
                label = "Say nothing more. Just stay, exactly as long as it takes, and let it choose on its own terms.",
                nextNodeId = "chapter30_end",
                consequences = Consequences(
                    setFlags = setOf("simply_stayed_at_the_end"),
                    grantItemIds = listOf("promise_of_the_six")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter30_end",
        chapterId = "chapter_30",
        title = "End of Chapter XXX — Six, Not One",
        illustrationId = "chapter30_threshold",
        narrativeText = """
            It doesn't answer in words. It answers by letting go: of the hillside, of the
            forgetting, of three hundred years spent believing wholeness and solitude were the
            only two choices anyone ever really had. Whatever it is now, scattered by design and
            no longer scrambling to undo that, it settles into something Kaelen doesn't have a
            name for and doesn't feel any urgent need to find one for tonight.

            The prisoner is the last to speak, once the hillside is ordinary again. "Six," it
            says. "Still six. Just, six who know each other's names now, instead of six keeping
            watch alone in the dark. I didn't know that was allowed to be the ending. I don't
            think it did either, until just now."

            Voss looks at Kaelen the way she's looked at him since Fenmoor, since Ashfall, since
            every gate they've stood at together that didn't have to be faced alone. Ilsevet is
            still out there somewhere, and the Cinder faction with her, and Emberlow and Greymoor
            still haven't decided anything at all, the world doesn't tie itself into a single bow
            just because one very old argument finally ended. But for tonight, on a hillside that
            was once the loneliest place in the world, that feels like enough to be getting on
            with.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, for now.", nextNodeId = "the_watch_continues")
        )
    ),
    StoryNode(
        id = "the_watch_continues",
        chapterId = "chapter_30",
        title = "End of Chapter XXX — For Now",
        illustrationId = "chapter30_threshold",
        narrativeText = """
            This is where Kaelen's part of the story rests, for now — Ilsevet unaccounted for,
            Emberlow and Greymoor still weighing an answer nobody can weigh for them, and a sixth
            door at Duskmere that has yet to open on anyone. The last jailer's watch isn't over.
            It's just, for the first time in three years, not a watch he's keeping alone.

            The rest of it — whatever Emberlow decides, whatever Ilsevet still intends, whatever
            waits at Duskmere — is a tale still being told, not yet finished being written.
            Kaelen's journal and trophy case hold everything the road has given him so far.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Begin the tale again, and see where this telling leads.",
                nextNodeId = "fallen_knight",
                consequences = Consequences(unlockTrophy = "The Watch Goes On")
            )
        )
    )
)

val chapter30 = com.thelastjailer.app.Chapter(
    id = "chapter_30",
    number = 30,
    title = "Chapter XXX — Six, Not One",
    startNodeId = "what_must_happen_next"
)
