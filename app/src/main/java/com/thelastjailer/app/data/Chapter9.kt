package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/** Family C9a: how Kaelen means to get into the Sanctum. The first three are the plans that shipped. */
val C9_WAY_FAMILY: Set<String> = setOf(
    "created_a_diversion", "infiltrated_quietly", "went_with_what_they_had", "c9_way_stair", "c9_way_wall", "c9_way_cart"
)

/** Family C9b: how the ward on the cell wing was dealt with. Waiting for the patrol to open it sets none of them. */
val C9_DOOR_FAMILY: Set<String> = setOf("c9_door_seal", "c9_door_nails", "c9_door_force")

/** Family C9c: what Kaelen did with the days at Stonebeard that were spent neither on the plans nor on the stores. */
val C9_DAYS_FAMILY: Set<String> = setOf("c9_stair_rules", "c9_stair_people", "c9_gate_braced")

/** Family C9d: the two shipped ways of freeing Voss. */
val C9_CHAINS_FAMILY: Set<String> = setOf("broke_her_free", "trusted_her_to_free_herself")

/** Family C9e: the three shipped acknowledgements of the vow made at the end of Chapter VIII. */
val C9_VOW_ACK_FAMILY: Set<String> = setOf("moved_without_delay", "moved_with_preparation", "decided_at_last")

/**
 * Everything Chapter IX records, the shipped flags included. The three answers to the vow clear it all, so a replay reaches
 * Stonebeard's long table with an empty slate. Chapter VII's and VIII's flags are left alone: they describe a telling the
 * replay has already decided again.
 */
val C9_TELLING_FLAGS: Set<String> = setOf(
    "c9_knows_ordrun", "c9_knows_cells", "c9_kit", "lost_to_cinder_castellan", "defeated_ordrun"
) + C9_WAY_FAMILY + C9_DOOR_FAMILY + C9_DAYS_FAMILY + C9_CHAINS_FAMILY + C9_VOW_ACK_FAMILY + HUB_MARKERS

/**
 * One continue choice for a place at the long table: back to the table with one place seen, or out of the hub once the
 * second is seen (or at once, when the vow left time for only one). Exactly one of the pair is ever visible. The second
 * leaves the hub and clears its markers.
 */
private fun tableOnward(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c9_the_table_two",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_second")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c9_table_done",
        requirements = ChoiceRequirement(requiredFlags = setOf("hub_second")),
        consequences = consequences.copy(clearFlags = consequences.clearFlags + HUB_MARKERS)
    )
)

/** The places at the table, written once for the first visit and once for the second (which also sets the second marker). */
private fun tableSpots(second: Boolean): List<Choice> {
    val extra = if (second) setOf("hub_second") else emptySet()
    return listOf(
        Choice(
            label = "The plans under the lamp.",
            nextNodeId = "c9_plans",
            requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_a")),
            consequences = Consequences(setFlags = setOf("hub_a") + extra)
        ),
        Choice(
            label = "The stores behind the forge.",
            nextNodeId = "c9_stores",
            requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_b")),
            consequences = Consequences(setFlags = setOf("hub_b") + extra)
        ),
        Choice(
            label = "The north stair, where Edda's people have made a home.",
            nextNodeId = "c9_north_stair",
            requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_terms"), forbiddenFlags = setOf("hub_c")),
            consequences = Consequences(setFlags = setOf("hub_c") + extra)
        ),
        Choice(
            label = "The gate, where Halvard is working.",
            nextNodeId = "c9_gate_work",
            requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_hold_terms", "hub_c")),
            consequences = Consequences(setFlags = setOf("hub_c") + extra)
        )
    )
}

/** Whichever way Kaelen came in, the next thing he finds is the door of the cell wing. */
private fun findTheDoor(label: String = "Find the door."): List<Choice> = listOf(Choice(label = label, nextNodeId = "c9_the_door"))

/** After Ordrun: the way out, chosen by how the night after Voss was taken was spent. Exactly one is visible. */
private fun runChoices(): List<Choice> = listOf(
    Choice(
        label = "Run.",
        nextNodeId = "c9_run_marked",
        requirements = ChoiceRequirement(requiredFlags = setOf("c8_marked_route"))
    ),
    Choice(
        label = "Run.",
        nextNodeId = "c9_run_farmers",
        requirements = ChoiceRequirement(requiredFlags = setOf("c8_ash_farmers"))
    ),
    Choice(
        label = "Run.",
        nextNodeId = "c9_run_hard",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c8_marked_route", "c8_ash_farmers"))
    )
)

/**
 * The last step out of the Sanctum, with [health] as the road costs or gives. A ward that was forced rang every bell in the
 * place, and the pursuit makes that cost ten more. Exactly one of the pair is ever visible.
 */
private fun outOfTheSanctum(health: Int): List<Choice> = listOf(
    Choice(
        label = "Run. Every bell in the place knows which way.",
        nextNodeId = "chapter9_end",
        requirements = ChoiceRequirement(requiredFlags = setOf("c9_door_force")),
        consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to health - 10))
    ),
    Choice(
        label = "Run.",
        nextNodeId = "chapter9_end",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c9_door_force")),
        consequences = if (health == 0) Consequences() else Consequences(statDeltas = mapOf(StatType.HEALTH to health))
    )
)

/** After Ordrun: the chapel, for a Voss who trusts him and a Kaelen who stood at it, and then the way out. */
private fun aftermathChoices(): List<Choice> = listOf(
    Choice(
        label = "Tell her you remember the chapel door.",
        nextNodeId = "c9_voss_chapel",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted", "c8_chapel_story"))
    )
) + runChoices()

/**
 * The two shipped ways of freeing Voss, written once per rung. Each leads first to the blade scene her standing, after the
 * choice has moved it, calls for (Halvard's blade is laid beside her before the chains come off), and from there to the shipped
 * scene that matches the way Kaelen chose.
 */
private fun chainsChoices(): List<Choice> {
    fun bladeFor(c: Choice) = if ("c7_rung_trusted" in c.consequences.setFlags) "c9_blade_trusted" else "c9_blade_guarded"
    return (rungChoices(
        label = "Break the shackles yourself.",
        next = "the_right_hand_broke_free",
        delta = 0,
        base = Consequences(
            statDeltas = mapOf(StatType.COURAGE to 1),
            clearFlags = C9_CHAINS_FAMILY,
            setFlags = setOf("broke_her_free")
        )
    ) + rungChoices(
        label = "Keep watch. Let her work the lock herself.",
        next = "the_right_hand_trusted",
        delta = 1,
        base = Consequences(
            statDeltas = mapOf(StatType.HONOUR to 1),
            clearFlags = C9_CHAINS_FAMILY,
            setFlags = setOf("trusted_her_to_free_herself")
        )
    )).map { it.copy(nextNodeId = bladeFor(it)) }
}

/** With the blade laid down, the shipped scene that frees her: the one that matches how Kaelen chose to do it. */
private fun bladeToFight(): List<Choice> = listOf(
    Choice(
        label = "Go on.",
        nextNodeId = "the_right_hand_trusted",
        requirements = ChoiceRequirement(requiredFlags = setOf("trusted_her_to_free_herself"))
    ),
    Choice(
        label = "Go on.",
        nextNodeId = "the_right_hand_broke_free",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("trusted_her_to_free_herself"))
    )
)

/** A day on the gate: the work, the bruises, and what Halvard says while he does it. */
private fun gateBraced(): Consequences = Consequences(
    statDeltas = mapOf(StatType.HONOUR to 1, StatType.HEALTH to -10),
    clearFlags = C9_DAYS_FAMILY,
    setFlags = setOf("c9_gate_braced")
)

/**
 * Chapter IX — What Ordrun Said.
 *
 * Pays off Chapter VIII's cliffhanger: the rescue Kaelen owes Voss. The vow made at the end of VIII now decides how many
 * days are well spent: three days used in a hurry, used properly, or lost to indecision. The days are spent at the
 * long table on one or two of three things (the plans, the stores, the people at the north stair or the gate), then Halvard
 * puts a borrowed blade for Voss in Kaelen's hands, and Kaelen lays it beside her in the cell before the chains come off. The
 * plan for the Sanctum is one of the three that shipped or one of three that Chapter VIII made possible (the east stair, the
 * south wall, the cells' food cart). The cell wing's ward is dealt
 * with by whatever Kaelen carries or has learned. The rest is as shipped: the chains, Castellan Ordrun, and his last
 * words, which turn the abstract "four gates unaccounted for" into a deadline. The way home reads the night spent after
 * Voss was taken. Ends handing Voss, wounded, and a borrowed blade to Chapter X's siege.
 */
val chapter9Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "a_debt_remembered_trusted",
        chapterId = "chapter_9",
        title = "A Debt Remembered",
        illustrationId = "a_debt_remembered",
        narrativeText = """
            Voss bought Kaelen the road home with everything she had left to spend. He hasn't
            forgotten it for a moment since — not on the walk south, not in the days it took
            Halvard to hear the whole account without interrupting once.

            He believed her the moment she first asked to be believed, back at the black door,
            and nothing about watching her bleed for that trust in the days since has made him
            regret it.

            Whatever else the schematic means, whatever the seventh door turns out to cost, none
            of it changes what's owed first.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"No more waiting. We move now.\"",
                nextNodeId = "c9_days_now",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_now")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C9_TELLING_FLAGS,
                    setFlags = setOf("moved_without_delay", "hub_second")
                )
            ),
            Choice(
                label = "\"We do this properly, with whatever Halvard can spare.\"",
                nextNodeId = "c9_days_prepared",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_prepared")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C9_TELLING_FLAGS,
                    setFlags = setOf("moved_with_preparation")
                )
            ),
            Choice(
                label = "\"The day I asked for is over. Time to decide.\"",
                nextNodeId = "c9_days_late",
                requirements = ChoiceRequirement(requiredFlags = setOf("undecided_on_voss")),
                consequences = Consequences(clearFlags = C9_TELLING_FLAGS, setFlags = setOf("decided_at_last", "hub_second"))
            )
        )
    ),
    StoryNode(
        id = "a_debt_remembered_guarded",
        chapterId = "chapter_9",
        title = "A Debt Remembered",
        illustrationId = "a_debt_remembered",
        narrativeText = """
            Voss bought Kaelen the road home with everything she had left to spend. He hasn't
            forgotten it for a moment since — not on the walk south, not in the days it took
            Halvard to hear the whole account without interrupting once.

            He made her earn that belief before he'd give it, back at the black door, and
            whatever she made of the delay, she's more than paid down whatever debt his caution
            left standing between them.

            Whatever else the schematic means, whatever the seventh door turns out to cost, none
            of it changes what's owed first.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"No more waiting. We move now.\"",
                nextNodeId = "c9_days_now",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_now")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C9_TELLING_FLAGS,
                    setFlags = setOf("moved_without_delay", "hub_second")
                )
            ),
            Choice(
                label = "\"We do this properly, with whatever Halvard can spare.\"",
                nextNodeId = "c9_days_prepared",
                requirements = ChoiceRequirement(requiredFlags = setOf("vowed_to_return_prepared")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C9_TELLING_FLAGS,
                    setFlags = setOf("moved_with_preparation")
                )
            ),
            Choice(
                label = "\"The day I asked for is over. Time to decide.\"",
                nextNodeId = "c9_days_late",
                requirements = ChoiceRequirement(requiredFlags = setOf("undecided_on_voss")),
                consequences = Consequences(clearFlags = C9_TELLING_FLAGS, setFlags = setOf("decided_at_last", "hub_second"))
            )
        )
    ),
    StoryNode(
        id = "c9_days_now",
        chapterId = "chapter_9",
        title = "No Waiting",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard gives him the three days, and Kaelen does not know what to do with
            them. He sleeps in pieces. He walks the forge floor until the dwarves who
            sleep over the smithy begin to count his steps. There is a point past a vow
            where keeping it stops being a feeling and becomes a list, and his list is
            short: a pack, the plans, and a road with a hole in the middle of it where
            Voss should be. Speed was what he promised, and speed, it turns out, makes
            poor use of days. By the evening of the third, when the lamp is lit at the
            long table, there is time for one thing done properly and a great deal of
            time that was wasted. Halvard watches him across the cold anvil and decides
            not to say what he is thinking. He says something else.

            "One thing, then. Choose it well."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the table.", nextNodeId = "c9_the_table"))
    ),
    StoryNode(
        id = "c9_days_prepared",
        chapterId = "chapter_9",
        title = "Properly",
        illustrationId = "silent_forge",
        narrativeText = """
            Three days. Halvard does not give them like a gift; he gives them like a
            debt he has agreed to run up, and he keeps a careful account of every hour.
            The first goes on sleep, which Kaelen resents. The second goes on eating,
            which he resents less. On the third, the long table in the forge has been
            cleared of everything but a lamp, the plans, and a number of small hard
            questions.

            "You wanted it done properly," Halvard says. "Properly is slower than you
            hoped and faster than I'd like."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the table.", nextNodeId = "c9_the_table"))
    ),
    StoryNode(
        id = "c9_days_late",
        chapterId = "chapter_9",
        title = "The Day That Was Asked For",
        illustrationId = "silent_forge",
        narrativeText = """
            The day he asked for is over, and he spent it turning the question until its
            edges wore smooth. Halvard gives him the three days anyway, and Kaelen finds
            he cannot spend them: every task he picks up he puts down again, because
            every task is a bet on a plan, and he has not made one. He knows what
            Halvard sees when he looks up from the forge, because he has seen it himself
            in the water of a dwarven basin: a man who said one thing and did another.
            Halvard says none of it. "Well," he says, when the lamp is lit on the third
            evening. "You've decided. That's the hard part, and it's done."

            It is not the whole truth, and they both know it. Whatever the Sanctum was a
            day ago, it is something else by now.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the table.", nextNodeId = "c9_the_table"))
    ),
    StoryNode(
        id = "c9_the_table",
        chapterId = "chapter_9",
        title = "The Long Table",
        illustrationId = "silent_forge",
        narrativeText = """
            The long table in Stonebeard's forge has been cleared of everything but a
            lamp, and the lamp is lit for a reason. The plans lie under it, pinned flat
            at the corners with four dwarven cups. Halvard stands at the head with the
            chain wound once round his wrist, looking at them the way he looks at the
            gate: as a thing that will not get better for being stared at.

            There are three things that want doing, and the lamp will not last for all
            of them.
        """.trimIndent(),
        choices = tableSpots(second = false)
    ),
    StoryNode(
        id = "c9_the_table_two",
        chapterId = "chapter_9",
        title = "The Long Table",
        illustrationId = "silent_forge",
        narrativeText = """
            One thing done, and the lamp has burned down by a hand's breadth. There is
            time for one more, and not for two. Halvard does not say which. He has never
            told Kaelen which; it is the nearest thing he has to a method of teaching.
        """.trimIndent(),
        choices = tableSpots(second = true)
    ),
    StoryNode(
        id = "c9_plans",
        chapterId = "chapter_9",
        title = "The Plans",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            The plans are as Kaelen carried them out of the ash, burned at the edges and
            written in a hand that was careful once and hurried later. The six empty
            settings sit round the frame like the numbers of a clock. Beneath the frame,
            in smaller writing, is the Sanctum itself: the spire, the curtain wall, and
            a long low wing that neither the road nor the tithe carts ever showed him.

            "Read what you like," Halvard says. "There is more in them than I can take
            in at one sitting."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Halvard has already marked the margins. Go through them with him.",
                nextNodeId = "c9_plans_letter",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_wrote_halvard"))
            ),
            Choice(
                label = "Read the watch-roll at the foot of the plans.",
                nextNodeId = "c9_plans_ordrun",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c8_wrote_halvard"))
            ),
            Choice(
                label = "Read the long low wing in the margins.",
                nextNodeId = "c9_plans_cells",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c8_wrote_halvard"))
            ),
            Choice(
                label = "Ask Halvard what the Order's hunters will do to a woman who sent their own people away.",
                nextNodeId = "c9_plans_hunted",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_voss_called_them_off")),
                consequences = Consequences(setFlags = setOf("c9_knows_ordrun"))
            )
        ) + tableOnward("Leave the plans to Halvard.")
    ),
    StoryNode(
        id = "c9_plans_letter",
        chapterId = "chapter_9",
        title = "What the Letter Bought",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            "Your letter came two days before you did," Halvard says. He is not smiling,
            but the corners of his eyes have made up their own minds. He has been busy.
            The margins are crowded with his small square writing: this wall, this
            stair, the long wing under the east tower where the plans stop being a frame
            and start being a building. At the foot of the page there is a roll of names
            and duty hours, burned at one corner, and one name on it is signed instead
            of listed.

            "Ordrun," Halvard reads. "Castellan of the place, which is a polite word for
            whoever answers to Ilsevet for every door in it. Twin blades, and he
            finishes with the second, so you watch the first less than you'd like." His
            thumb finds the long wing. "And this is warded in the same dark hand as the
            wards on your own gate. Three things answer a ward like that. The Order's
            own mark. A binding as old as the wards themselves. Or enough force to make
            it spend itself, loudly, and tell the whole spire where you are."

            He lets go of the cup and the corner of the page curls back. "I'd use the
            first two, if you have them. I wouldn't like to be in the room for the
            third."
        """.trimIndent(),
        choices = tableOnward(
            "Remember all of it.",
            Consequences(setFlags = setOf("c9_knows_ordrun", "c9_knows_cells"))
        )
    ),
    StoryNode(
        id = "c9_plans_ordrun",
        chapterId = "chapter_9",
        title = "The Name at the Foot",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            At the foot of the plans, burned away at one corner, is a roll of names and
            duty hours: who walks which wall, when the bells change. Most of it is the
            small hurried writing of a clerk. One name is signed instead of listed.

            "Ordrun," Halvard reads, with the care of a man handling something that
            might still be hot. "Castellan of the place, which is a polite word for
            whoever answers to Ilsevet for every door in it. I know the name. He took
            the Order's ranks and left its rules behind. Twin blades, and he finishes
            with the second, so you watch the first less than you'd like." He sets a cup
            on the page to keep it flat. "When Ilsevet wants something fetched from
            someone who won't give it, Ordrun is who goes. If Voss is alive, it is
            because he judged her worth keeping."
        """.trimIndent(),
        choices = tableOnward("Remember it.", Consequences(setFlags = setOf("c9_knows_ordrun")))
    ),
    StoryNode(
        id = "c9_plans_hunted",
        chapterId = "chapter_9",
        title = "What the Hunters Do",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            Halvard does not answer at once. He has been a warden long enough to know
            that some questions are asked so that the silence after them can answer.

            "She sent her own people away," he says at last. "In daylight, with a
            witness, and with her colours off. That will have reached Ordrun before the
            week was out." He sets a cup on the page to keep it flat. "Castellan of the
            place, which is a polite word for whoever answers to Ilsevet for every door
            in it. Twin blades, and he finishes with the second, so you watch the first
            less than you'd like. He doesn't forgive a thing like that, and he doesn't
            waste it. If Voss is alive, it is because she is worth more to him as an
            example than as a corpse."
        """.trimIndent(),
        choices = tableOnward("Remember it.", Consequences(setFlags = setOf("c9_knows_ordrun")))
    ),
    StoryNode(
        id = "c9_plans_cells",
        chapterId = "chapter_9",
        title = "The Long Low Wing",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            The margins are crowded where the plans stop being a frame and start being a
            building: a long low wing under the east tower, a stair at its near end, a
            single door at its far one. Whoever drew it never meant it to be read by
            anyone it could hurt. Halvard has found the one line on the page that
            matters.

            "The wing is warded," he says, "in the same dark hand as the wards on your
            own gate. Three things answer a ward like that. The Order's mark. A binding
            as old as the wards themselves. Or enough force to make it spend itself,
            loudly, and tell the whole spire where you are. If it were my door I would
            want to know which of the three I had before I reached it."
        """.trimIndent(),
        choices = tableOnward("Remember it.", Consequences(setFlags = setOf("c9_knows_cells")))
    ),
    StoryNode(
        id = "c9_stores",
        chapterId = "chapter_9",
        title = "The Stores",
        illustrationId = "silent_forge",
        narrativeText = """
            Stonebeard's stores are behind the second door past the forge, and they are
            smaller than the war needs them to be. Halvard has set a lamp on the barrel
            by the door and said nothing about what he is leaving to Kaelen's choosing.
            There is one pick, not two: what will keep a man alive, or what will get him
            in.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The armoury behind the stair never saw a blade. Take the pick of it.",
                nextNodeId = "c9_stores_supplies",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_stair")),
                consequences = Consequences(grantItemIds = listOf("healing_draught", "healing_draught"))
            ),
            Choice(
                label = "The forge is a black skeleton and the whetstones are ash. Take what the fire left.",
                nextNodeId = "c9_stores_supplies",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_yard")),
                consequences = Consequences(grantItemIds = listOf("healing_draught"))
            ),
            Choice(
                label = "The Hold was never touched. Take what Halvard can spare, and the coin he put by.",
                nextNodeId = "c9_stores_supplies",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_road")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to 15),
                    grantItemIds = listOf("healing_draught")
                )
            ),
            Choice(
                label = "Take what Halvard can spare.",
                nextNodeId = "c9_stores_supplies",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_ground_stair", "c7_ground_yard", "c7_ground_road")),
                consequences = Consequences(grantItemIds = listOf("healing_draught"))
            ),
            Choice(
                label = "Take rope, hooks and a dark lantern instead.",
                nextNodeId = "c9_stores_kit",
                consequences = Consequences(setFlags = setOf("c9_kit"))
            )
        ) + tableOnward("Leave the stores. You will carry what you carry.")
    ),
    StoryNode(
        id = "c9_stores_supplies",
        chapterId = "chapter_9",
        title = "Remedies",
        illustrationId = "silent_forge",
        narrativeText = """
            Draughts in a canvas roll, brewed by dwarves and bitter enough to be
            believed. Halvard counts them into Kaelen's pack himself, as though the
            number might otherwise change on the way. "They're not for courage," he
            says. "They're for afterwards."
        """.trimIndent(),
        choices = tableOnward("Go on.")
    ),
    StoryNode(
        id = "c9_stores_kit",
        chapterId = "chapter_9",
        title = "Rope and Shutter",
        illustrationId = "silent_forge",
        narrativeText = """
            Rope with hooks spliced into the end, a dark lantern with a sliding shutter,
            a roll of oiled cloth for muffling a chain. It is not what a man takes who
            means to survive. It is what a man takes who means to be somewhere he is not
            supposed to be. Halvard weighs the lantern in his palm, hands it back, and
            says nothing, with an expression Kaelen decides to treat as approval.
        """.trimIndent(),
        choices = tableOnward("Go on.")
    ),
    StoryNode(
        id = "c9_north_stair",
        chapterId = "chapter_9",
        title = "The North Stair",
        illustrationId = "silent_forge",
        narrativeText = """
            The north stair is dry, as Halvard promised, and cold, and Edda's people
            have made a home of it with the speed of people who have done it before:
            blankets hung for walls, a pot over a fire the size of two hands, thirty
            faces turning up as Kaelen comes down. A boy is being held by the collar at
            the bottom, and Halvard is the one holding him.

            "The forge is closed to them," Halvard says, without turning. "That was the
            term. He was in it, taking charcoal." The boy looks at Kaelen. So does
            everyone else. Halvard told him once that he would be the one to stand
            between these people and him. He did not say it would be today.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Stand with Halvard. The forge is closed, and the term was fair.",
                nextNodeId = "c9_stair_rules",
                consequences = Consequences(
                    clearFlags = C9_DAYS_FAMILY,
                    setFlags = setOf("c9_stair_rules"),
                    grantItemIds = listOf("healing_draught")
                )
            ),
            Choice(
                label = "Stand with the boy. It is a handful of charcoal and the stair is cold.",
                nextNodeId = "c9_stair_people",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C9_DAYS_FAMILY,
                    setFlags = setOf("c9_stair_people")
                )
            )
        ) + tableOnward("Leave them to settle it.", Consequences(clearFlags = C9_DAYS_FAMILY))
    ),
    StoryNode(
        id = "c9_stair_rules",
        chapterId = "chapter_9",
        title = "The Term Was Fair",
        illustrationId = "silent_forge",
        narrativeText = """
            Kaelen says the term was fair, and it was, and the sound the stair makes is
            not quite a sigh. Halvard lets the boy go with a hand on his head that is
            gentler than the grip was. Later, at the turn of the stair, he presses a
            flask into Kaelen's hand without ceremony. "For the trouble," he says. "I
            didn't enjoy that either."

            Edda's people go back to their blankets. Nobody says anything unkind, which
            is worse.
        """.trimIndent(),
        choices = tableOnward("Go on.")
    ),
    StoryNode(
        id = "c9_stair_people",
        chapterId = "chapter_9",
        title = "A Handful of Charcoal",
        illustrationId = "silent_forge",
        narrativeText = """
            Kaelen says it is a handful of charcoal and the stair is cold, and that if
            the term has to break it can break on him. Halvard's jaw works. He lets go
            of the boy. "That was my forge," he says, very evenly, "and I'll remember
            that you spent it."

            It is not forgiveness and it is not a quarrel. When it is over a broad woman
            in a carter's coat comes to the foot of the steps. She has driven worse
            roads than the one he is going to need, she says, and her cart is his if he
            wants it, and her silence goes with it. She does not say what she wants in
            return. It is, Kaelen thinks, the most dangerous kind of debt.
        """.trimIndent(),
        choices = tableOnward("Go on.")
    ),
    StoryNode(
        id = "c9_gate_work",
        chapterId = "chapter_9",
        title = "The Gate",
        illustrationId = "silent_forge",
        narrativeText = """
            The gate is where Halvard goes when the table has nothing left to say. It
            has not stopped cracking since the day Kaelen first saw it, and the dwarven
            bracing that holds it is older than the dwarves who set it. Halvard is on a
            ladder with a mallet, working at a wedge that needs no more work.

            "Hold the foot of this," he says, without looking down. "It is not a task.
            It's the only way I know to stand near a thing and not be afraid of it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Hold the ladder, and the gate, for the man who would not open his door.",
                nextNodeId = "c9_gate_braced",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_refused")),
                consequences = gateBraced()
            ),
            Choice(
                label = "Hold the ladder, and the gate, for the man you did not tell.",
                nextNodeId = "c9_gate_braced",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_hidden")),
                consequences = gateBraced()
            ),
            Choice(
                label = "Hold the ladder, and the gate.",
                nextNodeId = "c9_gate_braced",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_hold_refused", "c7_hold_hidden")),
                consequences = gateBraced()
            )
        ) + tableOnward("Leave him to it.", Consequences(clearFlags = C9_DAYS_FAMILY))
    ),
    StoryNode(
        id = "c9_gate_braced",
        chapterId = "chapter_9",
        title = "A Day on the Gate",
        illustrationId = "silent_forge",
        narrativeText = """
            They work until the lamp is needed to see the wedge. Halvard talks the whole
            time, which he never does: about the dwarves who cut the first channels,
            about finding the place where a binding is weakest with the flat of the hand
            and not the eye, about the fact that a ward is only a promise made by stone,
            and that stone can be asked to break it honestly. He does not say why he is
            telling Kaelen any of it. When the mallet is finally put down his knuckles
            are bleeding, and neither of them mentions that either.
        """.trimIndent(),
        choices = tableOnward("Go on.")
    ),
    StoryNode(
        id = "c9_table_done",
        chapterId = "chapter_9",
        title = "The Stair Mouth",
        illustrationId = "warden_chamber",
        narrativeText = """
            The days run out the way they always do, in a rush at the end. Halvard walks
            Kaelen to the stair mouth, where the Hold's light gives up and the ash-wind
            begins, and stops there, as he always stops, one step short of the open air.
            There is a long thing in his other hand, wrapped in oilcloth. He holds it as
            if he has not yet decided whether he will let go of it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Wait for him to speak.",
                nextNodeId = "c9_loan_trusts",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_halvard_trusts_voss"))
            ),
            Choice(
                label = "Wait for him to speak.",
                nextNodeId = "c9_loan_doubts",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_halvard_doubts_voss"))
            ),
            Choice(
                label = "Wait for him to speak.",
                nextNodeId = "c9_loan_plain",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_halvard_trusts_voss", "c7_halvard_doubts_voss"))
            )
        )
    ),
    StoryNode(
        id = "c9_loan_trusts",
        chapterId = "chapter_9",
        title = "A Loan",
        illustrationId = "warden_chamber",
        narrativeText = """
            "She'll want a blade," Halvard says. "They will have taken hers, and a woman
            shouldn't have to walk out of that place carrying nothing." He folds back an
            inch of the oilcloth: plain steel, no crest, an edge that someone has kept
            clean for a long time. "It was Stonebeard's before it was anyone's. Tell her
            it is a loan, and that I expect it back."

            He puts it in Kaelen's hand. It is the nearest thing to a blessing the old
            warden has, and he gives it without looking, the way you hand over a tool.
        """.trimIndent(),
        choices = listOf(Choice(label = "Take the blade.", nextNodeId = "the_plan_takes_shape"))
    ),
    StoryNode(
        id = "c9_loan_doubts",
        chapterId = "chapter_9",
        title = "A Loan",
        illustrationId = "warden_chamber",
        narrativeText = """
            "She'll want a blade," Halvard says. "They will have taken hers." He does
            not sound like a man who enjoys the sentence. "I still don't trust her. I'd
            like that said, so you can hold it against me later. But I won't have you
            come back with a woman who had to walk out of that place carrying nothing,
            either."

            He pushes the bundle into Kaelen's hands. "Tell her it is a loan. Tell her I
            expect it back, and that I mean it as a threat." The corner of his mouth
            does something complicated. "Don't tell her it was my idea."
        """.trimIndent(),
        choices = listOf(Choice(label = "Take the blade.", nextNodeId = "the_plan_takes_shape"))
    ),
    StoryNode(
        id = "c9_loan_plain",
        chapterId = "chapter_9",
        title = "A Loan",
        illustrationId = "warden_chamber",
        narrativeText = """
            "She'll want a blade," Halvard says. "They will have taken hers." He folds
            back an inch of the oilcloth: plain steel, no crest, an edge someone has
            kept clean for a long time. "It was Stonebeard's before it was anyone's.
            Tell her it is a loan, and that I expect it back."

            He puts it in Kaelen's hand and does not say anything else, which from
            Halvard is a speech.
        """.trimIndent(),
        choices = listOf(Choice(label = "Take the blade.", nextNodeId = "the_plan_takes_shape"))
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
                    clearFlags = C9_WAY_FAMILY,
                    setFlags = setOf("created_a_diversion")
                )
            ),
            Choice(
                label = "\"Go in quiet. We can't out-fight a garrison, only outlast their attention.\"",
                nextNodeId = "into_the_ash_quiet",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 7)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C9_WAY_FAMILY,
                    setFlags = setOf("infiltrated_quietly")
                )
            ),
            Choice(
                label = "Take whatever Halvard can spare, and go now.",
                nextNodeId = "into_the_ash_default",
                consequences = Consequences(clearFlags = C9_WAY_FAMILY, setFlags = setOf("went_with_what_they_had"))
            ),
            Choice(
                label = "Go in by the east stair, the way Voss told you.",
                nextNodeId = "c9_in_by_stair",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_voss_cells"), forbiddenFlags = setOf("decided_at_last")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C9_WAY_FAMILY,
                    setFlags = setOf("c9_way_stair")
                )
            ),
            Choice(
                label = "Climb the south wall at the watch change, with Halvard's rope.",
                nextNodeId = "c9_in_by_wall",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_rotation", "c9_kit")),
                consequences = Consequences(clearFlags = C9_WAY_FAMILY, setFlags = setOf("c9_way_wall"))
            ),
            Choice(
                label = "Climb the south wall at the watch change, from the ramp's foot you already know.",
                nextNodeId = "c9_in_by_wall",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_rotation", "c8_exit_ramp"), forbiddenFlags = setOf("c9_kit")),
                consequences = Consequences(clearFlags = C9_WAY_FAMILY, setFlags = setOf("c9_way_wall"))
            ),
            Choice(
                label = "Climb the south wall at the watch change, and trust your hands.",
                nextNodeId = "c9_in_by_wall",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_rotation"), forbiddenFlags = setOf("c9_kit", "c8_exit_ramp")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -10),
                    clearFlags = C9_WAY_FAMILY,
                    setFlags = setOf("c9_way_wall")
                )
            ),
            Choice(
                label = "Drive the cells' food cart in, on the old woman's mule and the ash on your hand.",
                nextNodeId = "c9_in_by_cart_farmers",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_ash_farmers")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to -20),
                    clearFlags = C9_WAY_FAMILY,
                    setFlags = setOf("c9_way_cart")
                )
            ),
            Choice(
                label = "Take the carter's cart up the tithe road, with ash on your hand.",
                nextNodeId = "c9_in_by_cart_carter",
                requirements = ChoiceRequirement(
                    requiredFlags = setOf("c8_tithe_seen", "c9_stair_people"),
                    forbiddenFlags = setOf("c8_ash_farmers")
                ),
                consequences = Consequences(clearFlags = C9_WAY_FAMILY, setFlags = setOf("c9_way_cart"))
            )
        )
    ),
    StoryNode(
        id = "c9_in_by_stair",
        chapterId = "chapter_9",
        title = "The East Stair",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            Voss said it to him once, under the horn, in the voice she keeps for facts:
            they put the ones they want to talk to nearest the east stair. He finds it
            where she said it would be, a narrow steep flight cut into the foundation on
            the Sanctum's eastern flank, unlit, with a door at the top that is not
            locked because nobody ever expected anyone to come at it from outside. Every
            step down smells of old water and older fear. At the bottom a corridor runs
            under the long low wing, and one door stands at its far end with a thread of
            dark light along the frame.
        """.trimIndent(),
        choices = findTheDoor()
    ),
    StoryNode(
        id = "c9_in_by_wall",
        chapterId = "chapter_9",
        title = "The South Wall",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The south wall, the hour after the second bell: two on the parapet and one
            asleep, exactly as the deserter said, and the sleeping one wakes badly, so
            Kaelen takes care not to give him the chance. The climb is a matter of
            patience and fingertips. The drop on the far side is a matter of luck.
            Inside the curtain wall the Sanctum is smaller than it looks from the ash
            and a good deal worse lit. The long low wing lies across a courtyard of
            black flags, and the only way over them is to be a shadow.
        """.trimIndent(),
        choices = findTheDoor()
    ),
    StoryNode(
        id = "c9_in_by_cart_farmers",
        chapterId = "chapter_9",
        title = "The Cells' Food Cart",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The old woman with ash in the creases of her hands is waiting at her door as
            though she had set a place for him. She does not say she will not drive it,
            and she does not say she will. She names a price for the cart and the mule
            and the loan of a grey thumbprint, and the price is not high. What she will
            have to say to herself afterwards is another matter.

            Kaelen takes the reins at dawn with ash worked into the back of his hand and
            drives the cells' food up the tithe road, past a collector who is bored and
            a gate that does not look at him. Anyone who drives it in is not looked at.
            It is true, and it is true in the worst possible way.
        """.trimIndent(),
        choices = findTheDoor()
    ),
    StoryNode(
        id = "c9_in_by_cart_carter",
        chapterId = "chapter_9",
        title = "The Carter's Cart",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The broad woman in the carter's coat does not ask why. She takes Kaelen's
            hand, turns it over, and presses a thumb of ash into the back of it that she
            has already mixed from the grey ends of a cold fire. "You watched the
            tithe," she says. "I can tell by how you hold your shoulders. Do as they
            do."

            The cart is hers, drawn by a mule that belongs to nobody in particular, and
            the tithe road is longer than he remembers. She sits beside him as far as
            the last bend and says four sentences in the whole of it. She says the last
            to the road as she climbs down: "Don't make me regret it." The rest he
            drives alone, past a collector who does not look up, to an arch that does
            not look at him.
        """.trimIndent(),
        choices = findTheDoor()
    ),
    StoryNode(
        id = "c9_the_door",
        chapterId = "chapter_9",
        title = "The Warded Door",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The wing ends in a single door, and the door is not wood. It is slate, black
            as the walls, with a thread of dark light running the length of the frame
            like a seam in cloth. The ward sits at the lock, a palm-sized knot of the
            same dark hand as the one on Kaelen's own gate, and the faint hum of it lies
            against his brand like a hand on a hand.

            On the doorframe someone has cut two lines, close together, the second
            deeper than the first. Whoever walks these rounds counts in pairs. Beyond
            the door, faint, a chain moves.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Hold Voss's seal to the ward. It was made for doors like this.",
                nextNodeId = "c9_door_seal",
                requirements = ChoiceRequirement(requiredFlags = setOf("earned_voss_seal"), forbiddenFlags = setOf("c8_seal_spent")),
                consequences = Consequences(clearFlags = C9_DOOR_FAMILY, setFlags = setOf("c9_door_seal"))
            ),
            Choice(
                label = "Drive one of Kestrel's ward-nails into the ward's seat.",
                nextNodeId = "c9_door_nails",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_has_ward_nails")),
                consequences = Consequences(clearFlags = C9_DOOR_FAMILY, setFlags = setOf("c9_door_nails"))
            ),
            Choice(
                label = "Break the ward at its seat, where the plans said it was weakest.",
                nextNodeId = "c9_door_force",
                requirements = ChoiceRequirement(requiredFlags = setOf("c9_knows_cells"), minStats = mapOf(StatType.COURAGE to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -5),
                    clearFlags = C9_DOOR_FAMILY,
                    setFlags = setOf("c9_door_force")
                )
            ),
            Choice(
                label = "Break the ward at its seat, the way Halvard showed you on the gate.",
                nextNodeId = "c9_door_force",
                requirements = ChoiceRequirement(
                    requiredFlags = setOf("c9_gate_braced"),
                    forbiddenFlags = setOf("c9_knows_cells"),
                    minStats = mapOf(StatType.COURAGE to 5)
                ),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -5),
                    clearFlags = C9_DOOR_FAMILY,
                    setFlags = setOf("c9_door_force")
                )
            ),
            Choice(
                label = "Break the door.",
                nextNodeId = "c9_door_force",
                requirements = ChoiceRequirement(
                    forbiddenFlags = setOf("c9_knows_cells", "c9_gate_braced"),
                    minStats = mapOf(StatType.COURAGE to 7)
                ),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -15),
                    clearFlags = C9_DOOR_FAMILY,
                    setFlags = setOf("c9_door_force")
                )
            ),
            Choice(
                label = "Wait in the dark for the patrol to open it, and slip through behind them.",
                nextNodeId = "c9_door_waited",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -5), clearFlags = C9_DOOR_FAMILY)
            )
        )
    ),
    StoryNode(
        id = "c9_door_seal",
        chapterId = "chapter_9",
        title = "The Order's Mark",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The seal does what it was made to do. The ward knows the Order's mark the
            way a dog knows a voice, shivers once, and lets go of the lock. The door
            swings in without a sound. The ring is warm on his finger, warmer than it
            should be, and somewhere in the slate the ward will remember the mark that
            opened it. Voss will have something to say about that. He hopes she lives to
            say it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "voss_in_chains"))
    ),
    StoryNode(
        id = "c9_door_nails",
        chapterId = "chapter_9",
        title = "Held",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The ward-nail goes in at the seat of the knot with a sound like a tooth
            being drawn. The dark thread along the frame stutters, steadies, and goes
            out, not broken but held, the way a gate is held, by a thing made to hold
            what cannot be bound. Kestrel never saw this door. She would have understood
            it at once. The nail stays where it is. He does not think it is coming out.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "voss_in_chains"))
    ),
    StoryNode(
        id = "c9_door_force",
        chapterId = "chapter_9",
        title = "The Ward Screams",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The ward gives all at once and screams the way old wards do, a high thin
            note that goes up through the slate like a nail through a board. The door
            swings. Somewhere above, a bell begins to say what he has done. He has
            minutes, at the outside, and he has just spent the first of them.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "voss_in_chains"))
    ),
    StoryNode(
        id = "c9_door_waited",
        chapterId = "chapter_9",
        title = "Behind the Patrol",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The patrol comes at the turn of the hour: two men in grey, walking in step,
            one of them whistling through his teeth. The one with the key does not look
            back. Kaelen is through the door behind them, flat against the wall, before
            it has finished swinging, and he watches them out at the far end. It cost
            him an hour in the dark and the feeling in his left leg. He thinks it was
            worth it. He is not certain. The air in the wing smells of cold iron and old
            tallow and something that is almost, but not quite, a person.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "voss_in_chains"))
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
            Choice(
                label = "Find her.",
                nextNodeId = "c9_the_door",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -15))
            )
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
            Choice(label = "Find her.", nextNodeId = "c9_the_door")
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
            Choice(
                label = "Find her.",
                nextNodeId = "c9_the_door",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -10))
            )
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
        choices = chainsChoices()
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
        id = "right_hand_defeat",
        chapterId = "chapter_9",
        title = "Not By His Own Hand",
        illustrationId = "the_right_hand_intercepts",
        narrativeText = """
            Ordrun doesn't fight like a man expecting to lose, and for a stretch of very bad
            seconds neither does Kaelen's body agree that he's going to win this one. Twin blades
            find him twice before he manages to put anything meaningful between them, and the
            second time is the one that puts him down.

            Voss is the one still standing when it matters. Ordrun goes down to her blade instead
            of his, a fact the castellan seems almost more offended by than the wound itself,
            laughing through blood at his teeth like it's a joke only he finds funny.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "what_the_castellan_says",
                consequences = Consequences(setFlags = setOf("lost_to_cinder_castellan"))
            )
        )
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
                nextNodeId = "c9_after_known",
                requirements = ChoiceRequirement(requiredFlags = setOf("c9_knows_ordrun")),
                consequences = Consequences(
                    grantItemIds = listOf("ordruns_broken_blade"),
                    setFlags = setOf("defeated_ordrun")
                )
            ),
            Choice(
                label = "Take what's left of his blade, and go.",
                nextNodeId = "c9_after_cold",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c9_knows_ordrun")),
                consequences = Consequences(
                    grantItemIds = listOf("ordruns_broken_blade"),
                    setFlags = setOf("defeated_ordrun")
                )
            )
        )
    ),
    StoryNode(
        id = "c9_after_known",
        chapterId = "chapter_9",
        title = "As the Table Said",
        illustrationId = "castellan_defeated",
        narrativeText = """
            Ordrun lies where he fell, and he was everything the table said he would be,
            which is its own kind of horror. The second blade came as the first was
            being answered, exactly as promised, and Kaelen had known to watch for it.
            He is not proud of knowing. He stands over what is left of the castellan and
            thinks that somebody ought to be sorry, and finds that nobody is.

            Voss stands over him too, breathing like a bellows. "Four settings," she
            says. "He meant it." Somewhere down the corridor a door bangs. They have a
            minute, perhaps. Perhaps less.
        """.trimIndent(),
        choices = aftermathChoices()
    ),
    StoryNode(
        id = "c9_after_cold",
        chapterId = "chapter_9",
        title = "The Second Blade",
        illustrationId = "castellan_defeated",
        narrativeText = """
            He had not known what to expect of a man like that, and it shows in how long
            it took to find out. Twin blades, and the second always arriving as the
            first was being answered. Ordrun lies where he fell with his dead mouth
            still half open on a laugh, and Kaelen does not want to know how many people
            learned that lesson before him.

            Voss stands over him too, breathing like a bellows. "Four settings," she
            says. "He meant it." Somewhere down the corridor a door bangs. They have a
            minute, perhaps. Perhaps less.
        """.trimIndent(),
        choices = aftermathChoices()
    ),
    StoryNode(
        id = "c9_voss_chapel",
        chapterId = "chapter_9",
        title = "The Chapel Door",
        illustrationId = "castellan_defeated",
        narrativeText = """
            "I stood at the door," Kaelen says. It is the worst possible time, and he
            cannot say why he is saying it. Voss looks at him as though he has produced
            something from his sleeve. "You didn't ask," she says. "Most people ask."

            The corridor shakes under a distant boot. "If I tell you, it will be because
            I've decided to, and not because I'm afraid of a corridor." She almost
            smiles. "Thank you for the door. Now run."
        """.trimIndent(),
        choices = runChoices()
    ),
    StoryNode(
        id = "c9_blade_trusted",
        chapterId = "chapter_9",
        title = "Halvard's Loan",
        illustrationId = "voss_in_chains",
        narrativeText = """
            Before he touches the chain, Kaelen unwraps the oilcloth with his teeth and
            one hand, and the plain steel comes out into the dark with a small clean
            sound. He lays it on the flags where her hand will fall when it is free.
            Voss looks at it for longer than it deserves. "Halvard's," she says. It is
            not a question. "He sent it. I'd have lost a wager."

            As he straightens, her chained fingers find the back of his hand and rest
            there for the length of a breath, quick and light, before they go back to
            the iron. "Then I had better be free to hold it," she says. "He will want it
            back."
        """.trimIndent(),
        choices = bladeToFight()
    ),
    StoryNode(
        id = "c9_blade_guarded",
        chapterId = "chapter_9",
        title = "Halvard's Loan",
        illustrationId = "voss_in_chains",
        narrativeText = """
            Before he touches the chain, Kaelen unwraps the oilcloth and lays the blade
            down hilt first, within reach of where her hand will fall when it is free,
            and steps back from it. Voss does not reach for it yet. She looks at it, and
            at him. "Halvard's," she says. "He wouldn't have sent it if he didn't want
            it back."

            She does not thank him, and he finds he does not mind. "Then we will see
            what I do with it," she says, "once I can lift my hands."
        """.trimIndent(),
        choices = bladeToFight()
    ),
    StoryNode(
        id = "c9_run_marked",
        chapterId = "chapter_9",
        title = "By the Cairns",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            Out by the nearest way and into the ash, and the cairns are where he left
            them: a hand's height each, grey on grey, every one a small promise kept. He
            runs the road by their shapes with Voss at his shoulder. When the horn
            sounds behind them the pursuit, three turns back, has already lost the line
            of the road in the dark. A man who has marked his way home is not an easy
            man to catch.
        """.trimIndent(),
        choices = outOfTheSanctum(health = 0)
    ),
    StoryNode(
        id = "c9_run_farmers",
        chapterId = "chapter_9",
        title = "The Ash-Farmers' Door",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            The old woman's huts are low and shuttered, as he remembered, and she opens
            the door before he knocks. She looks at Voss, and at the blood, and at the
            borrowed blade, and says nothing at all. She puts two bowls on the table and
            the shutters up, and the pursuit goes by on the road outside with its lamps
            swinging. They sleep by turns on straw that smells of ash and goat. In the
            morning there is a loaf by the door and no one to thank. Far behind them the
            horn sounds again, and they go.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the morning road.",
                nextNodeId = "chapter9_end",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to 10))
            )
        )
    ),
    StoryNode(
        id = "c9_run_hard",
        chapterId = "chapter_9",
        title = "The Long Way Out",
        illustrationId = "into_the_ash_sanctum",
        narrativeText = """
            There is no road back that he planned, only the one he came in by, and it is
            the first one they will watch. They run it anyway. The horn begins behind
            them, low and unhurried, and by the second mile it has found a second voice.
            They are not caught. They are chased across four hours of open ash by people
            who do not need to hurry, and the not needing is the worst of it, until at
            last the tunnel mouth shows black against the grey and the chasers turn back
            for reasons of their own.
        """.trimIndent(),
        choices = outOfTheSanctum(health = -20)
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
    startNodeId = "a_debt_remembered_guarded"
)
