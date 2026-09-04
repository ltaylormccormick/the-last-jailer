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
 * strategy choice with Halvard that genuinely stages the siege differently depending on the
 * answer (barricaded at the door versus met head-on out on the road), the siege itself (a third
 * and toughest combat encounter yet), the prisoner escalating its temptation mid-battle, a real
 * cost paid for holding the line, and a hint that Voss answers to something bigger than herself —
 * setting up the chapters beyond this one rather than resolving the Order as a threat.
 */
val chapter4Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_order_returns",
        chapterId = "chapter_4",
        title = "The Order Returns",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Weeks under the earth blur the same way days once did. Kaelen has learned the shape
            of Halvard's silences by now, and the particular ache of standing watch over something
            that never sleeps and never once thanks you for the effort.

            It's Halvard who feels it first: boots on the ruined road again, and this time not
            four swords behind Voss but a column of them, banners and all. Whatever this is, it
            was never going to be a conversation twice.
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
            "There were meant to be six wardens," Halvard says again, like it still surprises him
            every time he counts. "There's one, and whatever you are. That's what we hold this
            door with."

            No reinforcements are coming. No message will reach kin who don't even know Stonebeard
            still stands. Whatever happens on the road above happens with the two of them, and
            nothing else, and Halvard says it plainly enough that there's no comfort left to
            pretend otherwise.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Fortify the door. Make them come to us.\"",
                nextNodeId = "the_siege_begins_fortified",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("fortified_the_gate")
                )
            ),
            Choice(
                label = "\"Meet them on the road, before they reach the tree.\"",
                nextNodeId = "the_siege_begins_road",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("met_them_on_the_road")
                )
            )
        )
    ),
    StoryNode(
        id = "the_siege_begins_fortified",
        chapterId = "chapter_4",
        title = "The Siege Begins",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            They brace the door instead of the road: Halvard's remaining ward-work reinforcing the
            black stone from the outside now instead of only the inside, broken timber and
            scavenged iron wedged into the gap where a hinge should be but isn't. It won't hold
            forever. It only has to hold long enough.

            Voss doesn't lead the column in herself. She sends her vanguard instead, a single
            knight in ash-grey plate, unhurried up the last stretch of the ruined road, the way
            Voss herself was unhurried, as if speed were beneath the Order's dignity.

            "The Ashen Order gave you a chance to stand aside," the knight says, stopping just
            short of the barricade to draw a blade built for breaking locks as much as bone. "You
            built a wall instead. That's an answer too."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "siege_encounter"
    ),
    StoryNode(
        id = "the_siege_begins_road",
        chapterId = "chapter_4",
        title = "The Siege Begins",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            They go to meet the column instead of waiting for it, out past the tree line where the
            ruined road still remembers being a road, rain turning the old stone slick underfoot.
            Halvard insisted on walking it too, chain and all, and Kaelen didn't have the heart,
            or the standing, to tell a warden of six centuries to stay behind.

            Voss doesn't lead the column in herself. She sends her vanguard instead, a single
            knight in ash-grey plate, unhurried, cresting the rise ahead of the banners as if
            speed were beneath the Order's dignity.

            "The Ashen Order gave you a chance to stand aside," the knight says, drawing a blade
            built for breaking locks as much as bone. "You came out here to meet us instead.
            Braver than the tree deserved. Foolish, all the same."
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
            speaks again, not patient this time, almost eager. It felt that. It felt Kaelen's
            strength start to run thin, and it has more than enough patience left to spend,
            waiting for exactly this kind of moment to make its offer again.

            All he'd have to do is open his hand, the one with the brand on it, and ask. It would
            be so easy, is the worst part. Easier than the sword still cutting notches into his
            arm with every parry.
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

            It isn't a clean victory. Halvard took a wound shielding the door that he's too old to
            have taken and too stubborn to admit hurts as much as it does. Stonebeard Hold's gate,
            already cracked, is cracked further, and neither of them says out loud how much
            further it can crack before it stops being a gate at all.

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
            The signet ring under the vanguard's gauntlet isn't the Order's own mark. It's a rank
            insignia, and beneath the ash-grey enamel, a title stamped small enough to miss:
            answers to the Cinder Marshal.

            Voss was never in command here. She was a scout, sent ahead to measure a door before
            someone with real authority decided what to do about it. Whoever the Cinder Marshal
            is, they now know a jailer held a door against their vanguard and won, and Kaelen has
            the distinct, unpleasant feeling that losing was going to be the smaller problem all
            along.
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
            guarding. Kaelen sits watch over both of them, the old warden and the gate, and finds
            he doesn't resent it the way he thought he would when this all started.

            The brand on his palm aches, faintly, whenever he stops paying attention to it. He
            doesn't know yet what he agreed to, or refused, back on the road, only that some part
            of that answer is still owed. He suspects he's about to find out either way, and
            probably sooner than he'd like.

            Chapter V awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, while you still can.", nextNodeId = "the_marshals_envoy")
        )
    )
)

val chapter4 = com.thelastjailer.app.Chapter(
    id = "chapter_4",
    number = 4,
    title = "Chapter IV — The Ashen Vanguard",
    startNodeId = "the_order_returns"
)
