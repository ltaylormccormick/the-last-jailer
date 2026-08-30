package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter VI — The Weight of Others' Doors.
 *
 * Chapter V ended with the missive's real scope: five other warded gates, not just Kaelen's own.
 * This chapter makes that abstraction concrete by sending Kaelen to check on the nearest of them —
 * and finding it already lost. Where every fight so far has been against something still trying to
 * get out, this chapter's fight is against something that already did, which is a deliberately
 * different kind of threat than the Order/Marshal soldiers of III-V. Arc: the decision to travel,
 * the road north, the discovery of a fallen warden who held her post to the end, what she left
 * behind, a fifth and toughest combat encounter yet against what got loose, a stat-gated choice
 * about what to do with an unwardable ruin (mirroring III/IV/V), and a chapter-ending gut-check
 * that narrows the clock — four gates left, and the Marshal already knows where one of them is.
 */
val chapter6Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_choice_of_roads",
        chapterId = "chapter_6",
        title = "The Choice of Roads",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Halvard doesn't sleep so much as ration it these days, and the missive sits between
            them on the workbench like a coal neither of them wants to touch first.

            "Five other gates," he says, finally. "If even one of them's gone quiet the way
            Stonebeard nearly did, somebody ought to know before Ilsevet's people get there
            first." He doesn't ask Kaelen to go. He doesn't have to. There's only one of him fit
            to travel, and only one of him fit to hold this door.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll go. Someone should see what's actually out there.\"",
                nextNodeId = "the_road_north",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("chose_to_travel")
                )
            ),
            Choice(
                label = "\"Tell me which gate is nearest, and I'll start there.\"",
                nextNodeId = "the_road_north",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("chose_deliberately")
                )
            )
        )
    ),
    StoryNode(
        id = "the_road_north",
        chapterId = "chapter_6",
        title = "The Road North",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Above ground properly for the first time in longer than he can easily count, Kaelen
            finds the world unchanged and utterly changed at once — the same grey sky, the same
            ruined roads, but every stranger on them now a question: Order, Marshal, or neither.

            Three days north, following directions scratched into the missive's margin, the road
            ends at a village that shouldn't be as quiet as it is. Greymoor. Chimney smoke,
            shuttered doors, and not one living soul on the street to ask why.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Find whoever's still here.", nextNodeId = "the_second_gate")
        )
    ),
    StoryNode(
        id = "the_second_gate",
        chapterId = "chapter_6",
        title = "The Greymoor Ward",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The answer is worse than an empty village. Behind the miller's house, where
            Greymoor's own warded gate has stood sealed for as long as Stonebeard's, the black
            stone lies cracked clean through, wards dark from edge to edge. Whatever kept this
            one shut has been loose for days, maybe longer, judging by how thoroughly the village
            has emptied itself of anyone willing to say otherwise.

            A single figure lies at the threshold — not fled, not hiding. A warden, still in her
            chain, exactly where she chose to make her stand.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Kneel beside her.",
                nextNodeId = "what_the_warden_left",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("knelt_for_the_warden")
                )
            ),
            Choice(
                label = "Search the gate for answers first.",
                nextNodeId = "what_the_warden_left",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("searched_the_gate_first")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_warden_left",
        chapterId = "chapter_6",
        title = "What the Warden Left",
        illustrationId = "kestrels_locket_found",
        narrativeText = """
            Her name was carved into the chain itself, dwarven-fashion: Kestrel, Warden of
            Greymoor. Whatever came through the gate didn't kill her cleanly — there are wounds
            here that don't match any blade Kaelen knows, and no body left of whatever she was
            fighting when she fell.

            Clutched in her hand, close enough that it took the last of her strength to keep it
            there: a locket, warded in the same hand as the gate itself, and a scrap of writing
            that isn't a warning. It's an apology — to whoever came looking, and to whoever she
            couldn't hold the line for.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the locket. Someone should carry what she couldn't finish saying.",
                nextNodeId = "the_unbound",
                consequences = Consequences(
                    grantItemIds = listOf("kestrels_locket"),
                    setFlags = setOf("took_kestrels_locket")
                )
            )
        )
    ),
    StoryNode(
        id = "the_unbound",
        chapterId = "chapter_6",
        title = "The Unbound",
        illustrationId = "the_unbound_creature",
        narrativeText = """
            It hasn't gone far. Whatever tore Kestrel apart is still circling the wreck of her
            gate, sluggish with a meal it hasn't finished digesting, and it notices Kaelen exactly
            as fast as he notices it.

            Nothing about it resembles the pressure that speaks from behind Stonebeard's door.
            This isn't patient. This isn't waiting for permission anymore. This is loose, and
            hungry, and has apparently decided Kaelen looks like more of the same meal.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "unbound_encounter"
    ),
    StoryNode(
        id = "what_must_be_decided",
        chapterId = "chapter_6",
        title = "What Must Be Decided",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            It goes still eventually, more from exhaustion on both sides than any clean killing
            blow. Kaelen stands over it in the ruin of a gate that failed years before his own
            nearly did, Kestrel's locket a small, warm weight in his pocket.

            Greymoor's people will come back, or they won't. The gate behind him won't hold itself
            shut a second time — not without hands to warden it, and there are none left in this
            village willing to try.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll seal what's left myself, however long it holds.\"",
                nextNodeId = "chapter6_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("sealed_greymoor_personally")
                )
            ),
            Choice(
                label = "\"This isn't mine to hold. I'll bring word to people who can actually rebuild it.\"",
                nextNodeId = "chapter6_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("reported_greymoor_honestly")
                )
            ),
            Choice(
                label = "Leave it as it is, and carry what you learned back to Halvard.",
                nextNodeId = "chapter6_end",
                consequences = Consequences(setFlags = setOf("left_greymoor_as_is"))
            )
        )
    ),
    StoryNode(
        id = "chapter6_end",
        chapterId = "chapter_6",
        title = "End of Chapter VI — The Weight of Others' Doors",
        illustrationId = "chapter6_threshold",
        narrativeText = """
            The road south feels longer than the road north did, though the distance hasn't
            changed. Kaelen carries Kestrel's locket the whole way, and finds he can't decide
            whether that's grief or evidence.

            Halvard listens to all of it without interrupting, which is its own kind of answer.
            When Kaelen finishes, the old warden is quiet for a long moment. "Four gates left
            unaccounted for," he says at last. "And Ilsevet's people already know where at least
            one of them is." He doesn't say what happens if the Marshal's people reach the others
            before anyone else does. He doesn't have to.

            Chapter VII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, and plan for what's coming.", nextNodeId = "voss_returns_alone")
        )
    )
)

val chapter6 = com.thelastjailer.app.Chapter(
    id = "chapter_6",
    number = 6,
    title = "Chapter VI — The Weight of Others' Doors",
    startNodeId = "the_choice_of_roads"
)
