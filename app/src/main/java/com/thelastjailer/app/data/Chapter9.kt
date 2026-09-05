package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter IX — What Ordrun Said.
 *
 * Pays off Chapter VIII's cliffhanger: the rescue Kaelen owes Voss. Opens with three flag-gated
 * variants (only one visible per playthrough, matching the convergent-branch technique used for
 * Chapter I's opening) reflecting which vow the player made at the end of VIII — deliberately left
 * as a true convergence, since only one of the three is ever seen in a given playthrough and there
 * is no real choice being made there, just an acknowledgment of one already made — then converges
 * into a single infiltration arc whose method (loud diversion, quiet infiltration, or neither) now
 * colors the approach to the Sanctum itself, and whether Kaelen breaks Voss's chains himself or
 * trusts her to do it changes the beat between them before the Castellan appears. Ends with a new
 * escalation rather than Voss's rescue closing the loop: Ilsevet's own second-in-command reveals
 * four of the six settings in the seventh-door frame are already filled, and Kaelen's own gate is
 * one of only two left — turning the abstract "four gates unaccounted for" into a hard, personal
 * deadline. Ninth and toughest combat encounter yet (Castellan Ordrun, 145 HP). Stat-gated
 * infiltration-method choice (courage>=8 / honour>=7) continues the III-VIII escalation, paired
 * with an ungated fallback.
 */
val chapter9Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "a_debt_remembered",
        chapterId = "chapter_9",
        title = "A Debt Remembered",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Voss bought Kaelen the road home with everything she had left to spend. He hasn't
            forgotten it for a moment since — not on the walk south, not in the days it took
            Halvard to hear the whole account without interrupting once.

            Whatever else the schematic means, whatever the seventh door turns out to cost, none
            of it changes what's owed first.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"No more waiting. We move now.\"",
                nextNodeId = "the_plan_takes_shape",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_now")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("moved_without_delay")
                )
            ),
            Choice(
                label = "\"We do this properly, with whatever Halvard can spare.\"",
                nextNodeId = "the_plan_takes_shape",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_prepared")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("moved_with_preparation")
                )
            ),
            Choice(
                label = "\"The day I asked for is over. Time to decide.\"",
                nextNodeId = "the_plan_takes_shape",
                requirements = ChoiceRequirement(requiredFlags = setOf("undecided_on_voss")),
                consequences = Consequences(setFlags = setOf("decided_at_last"))
            )
        )
    ),
    StoryNode(
        id = "the_plan_takes_shape",
        chapterId = "chapter_9",
        title = "The Plan Takes Shape",
        illustrationId = "the_plan_takes_shape",
        narrativeText = """
            Halvard can't spare fighters: Stonebeard's gate still needs hands on it, gate cracked
            and warden aging and neither of those problems solving themselves while he's away.
            But he can spare gear, and three days he clearly begrudges every hour of. What's left
            is a plan with exactly one virtue: it's the only one they have time left to try.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Go in loud. Give them a reason not to look for her.\"",
                nextNodeId = "into_the_ash_loud",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 8)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("created_a_diversion")
                )
            ),
            Choice(
                label = "\"Go in quiet. We can't out-fight a garrison, only outlast their attention.\"",
                nextNodeId = "into_the_ash_quiet",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 7)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("infiltrated_quietly")
                )
            ),
            Choice(
                label = "Take whatever Halvard can spare, and go now.",
                nextNodeId = "into_the_ash_default",
                consequences = Consequences(setFlags = setOf("went_with_what_they_had"))
            )
        )
    ),
    StoryNode(
        id = "into_the_ash_loud",
        chapterId = "chapter_9",
        title = "Into the Ash",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            They go in loud, on purpose, Kaelen picking a fight with the outer watch just visible
            enough to matter while the rest of the garrison scrambles toward a threat that isn't
            where their real problem is. It works, mostly, in the way a controlled burn works:
            messier than anyone would choose, effective all the same.

            The Sanctum looks different from the inside of its own shadow, smaller in places,
            larger in ways that have nothing to do with stone. Kaelen finds the holding cells the
            way he's found everything else since the black door: by following the part of the
            place that feels most like waiting, ears still ringing faintly from the fight he
            started on purpose.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Find her.", nextNodeId = "voss_in_chains")
        )
    ),
    StoryNode(
        id = "into_the_ash_quiet",
        chapterId = "chapter_9",
        title = "Into the Ash",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            They go in quiet instead, Halvard's gear doing more of the work than either of them
            do: muffled boots, a route Voss half-remembers from her own time inside these walls,
            patience stretched thin enough to feel like a held breath the whole way through.

            The Sanctum looks different from the inside of its own shadow, smaller in places,
            larger in ways that have nothing to do with stone. Kaelen finds the holding cells the
            way he's found everything else since the black door: by following the part of the
            place that feels most like waiting, moving like a man who has decided noise is the
            one thing he can't afford tonight.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Find her.", nextNodeId = "voss_in_chains")
        )
    ),
    StoryNode(
        id = "into_the_ash_default",
        chapterId = "chapter_9",
        title = "Into the Ash",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            They go with whatever Halvard managed to spare and no clearer plan than that, Kaelen
            trusting the last three days of preparation to be enough even though it doesn't feel
            like it as the walls close in around them. Sometimes the plan is just refusing to
            turn back.

            The Sanctum looks different from the inside of its own shadow, smaller in places,
            larger in ways that have nothing to do with stone. Kaelen finds the holding cells the
            way he's found everything else since the black door: by following the part of the
            place that feels most like waiting.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Find her.", nextNodeId = "voss_in_chains")
        )
    ),
    StoryNode(
        id = "voss_in_chains",
        chapterId = "chapter_9",
        title = "Voss in Chains",
        illustrationId = "voss_in_chains",
        narrativeText = """
            She's alive, upright, and furious in exactly that order: warded shackles on both
            wrists, the same dark hand at work in the ironwork as the wards on Kaelen's own gate.
            She doesn't waste breath on surprise, or on thanking him, not yet.

            "Took you long enough," she says, already testing the give in the chain. "I assume
            you have a plan and not just a very good reason to be standing here."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Break the shackles yourself.",
                nextNodeId = "the_right_hand_broke_free",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("broke_her_free")
                )
            ),
            Choice(
                label = "Keep watch. Let her work the lock herself.",
                nextNodeId = "the_right_hand_trusted",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("trusted_her_to_free_herself")
                )
            )
        )
    ),
    StoryNode(
        id = "the_right_hand_broke_free",
        chapterId = "chapter_9",
        title = "The Right Hand",
        illustrationId = "the_right_hand_intercepts",
        narrativeText = """
            Kaelen wrenches the shackles apart with more force than finesse, and Voss rubs at the
            raw skin beneath them without complaint, though something in her expression suggests
            she'd have managed it herself given another minute. "Efficient," she says. "I'll take
            it."

            They almost reach the outer wall clean. Almost. A figure steps into the corridor
            ahead of them, robes the deep grey-black of command rather than rank-and-file cinder,
            twin blades already drawn like this is a formality rather than a fight.

            "Castellan Ordrun," Voss breathes, and doesn't sound relieved. "Ilsevet's own hand.
            She doesn't send him for stragglers."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "right_hand_encounter"
    ),
    StoryNode(
        id = "the_right_hand_trusted",
        chapterId = "chapter_9",
        title = "The Right Hand",
        illustrationId = "the_right_hand_intercepts",
        narrativeText = """
            Kaelen keeps his back to the corridor and lets her work, and she has the lock beaten
            in under a minute, muttering something in dwarvish that sounds less like a prayer
            than a professional critique of whoever built the shackles. "Good instinct," she
            says, flexing her freed wrists. "Trusting the wrong hands gets people killed down
            here."

            They almost reach the outer wall clean. Almost. A figure steps into the corridor
            ahead of them, robes the deep grey-black of command rather than rank-and-file cinder,
            twin blades already drawn like this is a formality rather than a fight.

            "Castellan Ordrun," Voss breathes, and doesn't sound relieved. "Ilsevet's own hand.
            She doesn't send him for stragglers."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "right_hand_encounter"
    ),
    StoryNode(
        id = "what_the_castellan_says",
        chapterId = "chapter_9",
        title = "What the Castellan Says",
        illustrationId = "castellan_defeated",
        narrativeText = """
            He goes down hard but not silent. Ordrun laughs even as he's falling, blood at his
            teeth. "Doesn't matter," he manages. "Four settings full already. Emberlow gave
            theirs without half this trouble. Won't be long before the last two fall in line too.
            She only really needs yours, jailer. The rest is just... thoroughness."

            He says nothing else worth hearing, though it takes him a long moment to stop trying.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take what's left of his blade, and go.",
                nextNodeId = "chapter9_end",
                consequences = Consequences(
                    grantItemIds = listOf("ordruns_broken_blade"),
                    setFlags = setOf("defeated_ordrun")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter9_end",
        chapterId = "chapter_9",
        title = "End of Chapter IX — What Ordrun Said",
        illustrationId = "chapter9_threshold",
        narrativeText = """
            They clear the ash-field with the horn sounding behind them for the second time in as
            many visits, though this time Voss runs beside him rather than behind. Neither of
            them speaks until Stonebeard's tunnel swallows the both of them whole and the sound
            of the Sanctum finally falls away.

            Halvard doesn't ask how it went. One look at Voss, free and furious and alive, tells
            him enough. What Kaelen tells him after, four settings filled, two gates left, and
            one of them his own, empties whatever relief was left in the old warden's face.

            "Then we're not looking for time to prepare anymore," Halvard says quietly. "We're
            looking for whatever's left of it."

            Chapter X awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare for what's left.", nextNodeId = "the_last_two")
        )
    )
)

val chapter9 = com.thelastjailer.app.Chapter(
    id = "chapter_9",
    number = 9,
    title = "Chapter IX — What Ordrun Said",
    startNodeId = "a_debt_remembered"
)
