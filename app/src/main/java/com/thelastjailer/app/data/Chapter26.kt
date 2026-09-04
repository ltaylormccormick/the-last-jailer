package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XXVI — Where It Began.
 *
 * Major structural pivot, flagged in the PR: the physical foothold the whole gained through
 * Duskmere's yielding (XXV) turns out to run both directions — it lets the prisoner glimpse
 * backward through it for the first time in three centuries, straight to the Sundering Ground, the
 * literal site where the one being was torn into six (the origin event XXI's lore reveal described
 * but never located). Asking what happened there versus simply resolving to go regardless changes
 * how much the prisoner explains before they set out, and how Kaelen treats the site's own
 * defensive forgetting afterward — pressing it to remember, respecting its choice to forget, or
 * stepping back entirely — now shapes the quiet close of the chapter, not just a flag. This
 * reframes the endgame: the sixth and final fragment was never hidden behind a warden's gate at
 * all. It has spent three hundred years disguised as the place itself, having forgotten so
 * completely what it is that even the whole no longer recognizes it as a fragment. Twenty-sixth and
 * toughest combat yet (The Unremembering, 355 HP — not a guardian defending the site, but the
 * site's own defensive forgetting given shape, resisting the idea that it was ever anything else).
 * Deliberately does not resolve the confrontation with a clean answer: this is the setup chapter for
 * the true endgame, not the endgame itself.
 */
val chapter26Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_duskmere_left_behind",
        chapterId = "chapter_26",
        title = "What Duskmere Left Behind",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner has been strange since Duskmere, not withdrawn, exactly, but listening
            in a direction Kaelen doesn't have a name for. "The foothold it took through Wren,"
            it finally says. "It doesn't just let it reach forward. For the first time in three
            hundred years, I can see backward through it too. I know where this started."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Started how? The tearing-apart you told us about?\"",
                nextNodeId = "what_the_prisoner_remembers_connected",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("connected_to_the_sundering")
                )
            ),
            Choice(
                label = "\"Then that's where we go. Whatever's waiting there.\"",
                nextNodeId = "what_the_prisoner_remembers_resolved",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("resolved_to_go_regardless")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_prisoner_remembers_connected",
        chapterId = "chapter_26",
        title = "What the Prisoner Remembers",
        illustrationId = "what_the_prisoner_remembers_ch26",
        narrativeText = """
            "Yes," it says, something like relief in being understood without having to spell out
            every piece of it. "The place where it happened. Where something tore itself into six
            pieces on purpose, so it could never again be whole enough to do whatever it did
            before that. Emberlow, Greymoor, Duskmere, your own door: those are gates someone
            built afterward, to keep a fragment company. This is different. Nobody built anything
            at the Sundering Ground. Nobody needed to. The sixth piece never left."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to the Sundering Ground.", nextNodeId = "the_road_to_the_sundering_ground")
        )
    ),
    StoryNode(
        id = "what_the_prisoner_remembers_resolved",
        chapterId = "chapter_26",
        title = "What the Prisoner Remembers",
        illustrationId = "what_the_prisoner_remembers_ch26",
        narrativeText = """
            It doesn't wait to be asked before it lays the whole thing out, matching his
            readiness with its own. "The place where it happened. Where something tore itself
            into six pieces on purpose, so it could never again be whole enough to do whatever it
            did before that. Emberlow, Greymoor, Duskmere, your own door: those are gates someone
            built afterward, to keep a fragment company. This is different. Nobody built anything
            at the Sundering Ground. Nobody needed to. The sixth piece never left."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to the Sundering Ground.", nextNodeId = "the_road_to_the_sundering_ground")
        )
    ),
    StoryNode(
        id = "the_road_to_the_sundering_ground",
        chapterId = "chapter_26",
        title = "The Road to the Sundering Ground",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            There is no watch-post here, no ledger, no keeper's chair left empty. Just a stretch
            of ordinary hillside that the prisoner insists, with total certainty, is the most
            important place in the world, and that certainty is the only thing marking it as
            anything but ordinary hillside at all.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Look for what isn't ordinary.", nextNodeId = "what_they_find_there")
        )
    ),
    StoryNode(
        id = "what_they_find_there",
        chapterId = "chapter_26",
        title = "What They Find There",
        illustrationId = "what_they_find_there",
        narrativeText = """
            It's a thread, in the end: a single filament of the same warm-cold material as every
            shard Kaelen has carried out of a fight, half-buried in ordinary soil, unremarkable
            enough that three hundred years of farmers must have walked over it without noticing.
            The moment Kaelen's hand closes around it, the hillside stops being ordinary.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Pull the thread free.",
                nextNodeId = "what_guards_the_beginning",
                consequences = Consequences(grantItemIds = listOf("thread_of_the_sundering"))
            )
        )
    ),
    StoryNode(
        id = "what_guards_the_beginning",
        chapterId = "chapter_26",
        title = "What Guards the Beginning",
        illustrationId = "what_guards_the_beginning",
        narrativeText = """
            Whatever rises out of the ground isn't defending the place so much as insisting, with
            everything it has, that there's nothing here to defend, that this is only a hillside,
            that Kaelen is mistaken, that the thread in his hand is nothing at all. Three
            centuries of forgetting have made the forgetting itself strong enough to fight for.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "sundering_ground_encounter"
    ),
    StoryNode(
        id = "what_kaelen_offers_the_forgotten",
        chapterId = "chapter_26",
        title = "What Kaelen Offers the Forgotten",
        illustrationId = "what_kaelen_offers_the_forgotten",
        narrativeText = """
            The forgetting doesn't break so much as pause, the way someone pauses mid-sentence
            when they've lost the thread of what they were about to say. For one unguarded
            moment, the hillside is just a hillside again, and something underneath it is
            listening, without yet knowing to what.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"You were part of something once. You still are. I'm not leaving until you remember that.\"",
                nextNodeId = "chapter26_end_pressed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 24)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_the_forgotten_to_remember")
                )
            ),
            Choice(
                label = "\"Maybe forgetting was the safest thing you ever did. I won't be the one who undoes it carelessly.\"",
                nextNodeId = "chapter26_end_respected",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 23)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("respected_the_forgetting")
                )
            ),
            Choice(
                label = "Leave the thread where it fell, and step back.",
                nextNodeId = "chapter26_end_stepped_back",
                consequences = Consequences(setFlags = setOf("stepped_back_from_the_sundering"))
            )
        )
    ),
    StoryNode(
        id = "chapter26_end_pressed",
        chapterId = "chapter_26",
        title = "End of Chapter XXVI — Where It Began",
        illustrationId = "chapter26_threshold",
        narrativeText = """
            The hillside goes ordinary again before Kaelen has decided whether that's a relief.
            No voice answers him, gentle or otherwise, though this time the silence feels less
            like refusal and more like something turning the question over. The sixth piece has
            spent three hundred years forgetting on purpose, and one conversation, however
            insistent, was never going to undo that in an afternoon, but Kaelen leaves believing
            he's owed at least the trying.

            The prisoner is quieter on the walk back than Kaelen has heard it since the very
            beginning. "Six pieces," it says finally. "Mine, and Emberlow's, and Greymoor's, and
            Duskmere's, and this one. That's five accounted for, counting my own. There should be
            a sixth gate somewhere, with a sixth name on it, and I have never once, in three
            hundred years, been able to remember what it is."

            Voss doesn't have an answer for that either. Somewhere, the whole is still listening
            through the foothold it took at Duskmere, and now it knows exactly where Kaelen just
            went looking.

            Chapter XXVII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_the_thread_wants")
        )
    ),
    StoryNode(
        id = "chapter26_end_respected",
        chapterId = "chapter_26",
        title = "End of Chapter XXVI — Where It Began",
        illustrationId = "chapter26_threshold",
        narrativeText = """
            The hillside goes ordinary again before Kaelen has decided whether that's a relief.
            No voice answers him, gentle or otherwise, and Kaelen finds he's glad, on reflection,
            that he didn't push harder than a hillside that has earned the right to keep its own
            secrets a little longer.

            The prisoner is quieter on the walk back than Kaelen has heard it since the very
            beginning. "Six pieces," it says finally. "Mine, and Emberlow's, and Greymoor's, and
            Duskmere's, and this one. That's five accounted for, counting my own. There should be
            a sixth gate somewhere, with a sixth name on it, and I have never once, in three
            hundred years, been able to remember what it is."

            Voss doesn't have an answer for that either. Somewhere, the whole is still listening
            through the foothold it took at Duskmere, and now it knows exactly where Kaelen just
            went looking.

            Chapter XXVII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_the_thread_wants")
        )
    ),
    StoryNode(
        id = "chapter26_end_stepped_back",
        chapterId = "chapter_26",
        title = "End of Chapter XXVI — Where It Began",
        illustrationId = "chapter26_threshold",
        narrativeText = """
            The hillside goes ordinary again before Kaelen has decided whether that's a relief.
            No voice answers him, gentle or otherwise; the sixth piece has spent three hundred
            years forgetting on purpose, and one conversation, however carefully chosen, was
            never going to undo that in an afternoon, so Kaelen doesn't force one.

            The prisoner is quieter on the walk back than Kaelen has heard it since the very
            beginning. "Six pieces," it says finally. "Mine, and Emberlow's, and Greymoor's, and
            Duskmere's, and this one. That's five accounted for, counting my own. There should be
            a sixth gate somewhere, with a sixth name on it, and I have never once, in three
            hundred years, been able to remember what it is."

            Voss doesn't have an answer for that either. Somewhere, the whole is still listening
            through the foothold it took at Duskmere, and now it knows exactly where Kaelen just
            went looking.

            Chapter XXVII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_the_thread_wants")
        )
    )
)

val chapter26 = com.thelastjailer.app.Chapter(
    id = "chapter_26",
    number = 26,
    title = "Chapter XXVI — Where It Began",
    startNodeId = "what_duskmere_left_behind"
)
