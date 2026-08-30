package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter IV — The Ashen Vanguard.
 *
 * The first chapter behind the full-story purchase. Voss's promise from Chapter III comes due:
 * the Ashen Order returns to the black door in force, not to talk. A 7-node arc: the warning, a
 * strategy choice with Halvard, the siege itself (a third and toughest combat encounter yet), the
 * prisoner escalating its temptation mid-battle, a real cost paid for holding the line, and a
 * hint that Voss answers to something bigger than herself — setting up the chapters beyond this
 * one rather than resolving the Order as a threat.
 */
val chapter4Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_order_returns",
        chapterId = "chapter_4",
        title = "The Order Returns",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Weeks under the earth blur the same way days once did. Kaelen has learned the shape
            of Halvard's silences, the particular ache of standing watch over something that
            never sleeps.

            It's Halvard who feels it first — boots on the ruined road again, and this time not
            four swords behind Voss but a column of them, banners and all. This was never going
            to be a conversation twice.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Halvard.", nextNodeId = "council_at_the_gate")
        )
    ),
    StoryNode(
        id = "council_at_the_gate",
        chapterId = "chapter_4",
        title = "Council at the Gate",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            "There were meant to be six wardens," Halvard says again, like it still surprises him.
            "There's one, and whatever you are. That's what we hold this door with."

            No reinforcements are coming. No message will reach kin who don't know Stonebeard
            still stands. Whatever happens on the road above happens with the two of them, and
            nothing else.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Fortify the door. Make them come to us.\"",
                nextNodeId = "the_siege_begins",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("fortified_the_gate")
                )
            ),
            Choice(
                label = "\"Meet them on the road, before they reach the tree.\"",
                nextNodeId = "the_siege_begins",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("met_them_on_the_road")
                )
            )
        )
    ),
    StoryNode(
        id = "the_siege_begins",
        chapterId = "chapter_4",
        title = "The Siege Begins",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Voss doesn't lead the column in herself. She sends her vanguard instead — a single
            knight in ash-grey plate, unhurried, the way Voss herself was unhurried, as if
            speed were beneath the Order's dignity.

            "The Ashen Order gave you a chance to stand aside," the knight says, drawing a blade
            built for breaking locks as much as bone. "You're still standing here. That's answer
            enough."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "siege_encounter"
    ),
    StoryNode(
        id = "what_the_prisoner_offers_again",
        chapterId = "chapter_4",
        title = "What the Prisoner Offers, Again",
        illustrationId = "prisoners_second_offer",
        narrativeText = """
            In the space between one clash of steel and the next, the pressure behind the gate
            speaks again — not patient this time, almost eager. It felt that. It felt Kaelen's
            strength start to run out, and it has more than enough to spare.

            All he'd have to do is open his hand, the one with the brand on it, and ask.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Open your hand. Take what it's offering.",
                nextNodeId = "the_cost",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 2),
                    setFlags = setOf("accepted_dark_aid")
                )
            ),
            Choice(
                label = "Keep your hand closed. Finish this on your own strength.",
                nextNodeId = "the_cost",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 2)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_dark_aid_again")
                )
            )
        )
    ),
    StoryNode(
        id = "the_cost",
        chapterId = "chapter_4",
        title = "The Cost",
        illustrationId = "the_cost_aftermath",
        narrativeText = """
            The vanguard falls, and what's left of the column pulls back down the ruined road
            rather than press an attack that already cost them their best blade.

            It isn't a clean victory. Halvard took a wound shielding the door that he's too old
            to have taken, and too stubborn to admit hurts as much as it does. Stonebeard Hold's
            gate, already cracked, is cracked further.

            They held. That's all "won" means, down here.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Search the fallen vanguard's gear.",
                nextNodeId = "no_longer_a_rumour",
                consequences = Consequences(grantItemIds = listOf("ashen_signet"))
            )
        )
    ),
    StoryNode(
        id = "no_longer_a_rumour",
        chapterId = "chapter_4",
        title = "No Longer a Rumour",
        illustrationId = "not_a_rumour",
        narrativeText = """
            The signet ring under the vanguard's gauntlet isn't the Order's own mark. It's a
            rank insignia, and beneath the ash-grey enamel, a title stamped small enough to miss:
            answers to the Cinder Marshal.

            Voss was never in command here. She was a scout. Whoever the Cinder Marshal is,
            they now know a jailer held a door against their vanguard and won — and Kaelen has
            the distinct, unpleasant feeling that losing was going to be the smaller problem.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Help Halvard back down to the Hold.", nextNodeId = "chapter4_end")
        )
    ),
    StoryNode(
        id = "chapter4_end",
        chapterId = "chapter_4",
        title = "End of Chapter IV — The Ashen Vanguard",
        illustrationId = "chapter4_threshold",
        narrativeText = """
            Halvard sleeps, finally, propped against the very stone he's spent his whole life
            guarding. Kaelen sits watch over both of them — the old warden and the gate — and
            finds he doesn't resent it the way he thought he would.

            The brand on his palm aches, faintly, whenever he stops paying attention to it. He
            doesn't know yet what he agreed to, or refused, back on the road. He suspects he's
            about to find out either way.

            Chapter V awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, while you still can.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter4 = com.thelastjailer.app.Chapter(
    id = "chapter_4",
    number = 4,
    title = "Chapter IV — The Ashen Vanguard",
    startNodeId = "the_order_returns"
)
