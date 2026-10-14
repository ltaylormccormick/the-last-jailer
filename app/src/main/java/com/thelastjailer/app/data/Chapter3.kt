package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.LockedDisplay
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C3: the flags that record how this telling's approach to the surface went. Cleared by every
 * choice at `the_answer` (the first scene of the chapter), so a replay never inherits a stale row.
 */
val CHAPTER3_FAMILY: Set<String> = setOf(
    "approached_by_camp", "read_voss_orders", "robbed_the_order", "parleyed_with_sentry",
    "asked_voss_name", "told_voss_of_the_riders", "sold_to_voss", "showed_voss_the_disc", "quoted_orders_to_voss"
)

/**
 * What Kaelen may say to Voss before she states her business. Every row is optional and loops back to
 * `ch3_voss_waits`; the last row always works, so nothing here can block the chapter. Each row rests on
 * something Kaelen has actually earned; none of them reveals who hired the riders.
 */
private fun vossRows(camp: Boolean): List<Choice> = listOfNotNull(
    Choice(
        label = "\"You were expecting me. By name.\"",
        nextNodeId = "ch3_voss_names",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("asked_voss_name")),
        consequences = Consequences(setFlags = setOf("asked_voss_name"))
    ),
    Choice(
        label = "\"The riders on the road were hired. Paid in halves, to bring me in alive.\"",
        nextNodeId = "ch3_voss_shared",
        requirements = ChoiceRequirement(
            requiredFlags = setOf("knows_riders_hired"),
            forbiddenFlags = setOf("told_voss_of_the_riders", "sold_to_voss")
        ),
        consequences = Consequences(
            statDeltas = mapOf(StatType.HONOUR to 1),
            setFlags = setOf("told_voss_of_the_riders")
        )
    ),
    Choice(
        label = "\"What I know about those riders has a price.\"",
        nextNodeId = "ch3_voss_sold",
        requirements = ChoiceRequirement(
            requiredFlags = setOf("knows_riders_hired"),
            forbiddenFlags = setOf("told_voss_of_the_riders", "sold_to_voss"),
            minStats = mapOf(StatType.COURAGE to 3)
        ),
        consequences = Consequences(
            statDeltas = mapOf(StatType.GOLD to 10),
            setFlags = setOf("sold_to_voss")
        )
    ),
    Choice(
        label = "Show her the scraped wax disc.",
        nextNodeId = "ch3_voss_disc",
        requirements = ChoiceRequirement(
            requiredFlags = setOf("rider_clue"),
            forbiddenFlags = setOf("showed_voss_the_disc")
        ),
        consequences = Consequences(setFlags = setOf("showed_voss_the_disc"))
    ),
    if (!camp) null else Choice(
        label = "\"Your orders say to confirm before you engage. Consider it confirmed.\"",
        nextNodeId = "ch3_voss_orders",
        requirements = ChoiceRequirement(
            requiredFlags = setOf("read_voss_orders"),
            forbiddenFlags = setOf("quoted_orders_to_voss")
        ),
        consequences = Consequences(
            statDeltas = mapOf(StatType.COURAGE to 1),
            setFlags = setOf("quoted_orders_to_voss")
        )
    ),
    Choice(label = "\"What do you want with it?\"", nextNodeId = "a_choice_at_the_door")
)

/**
 * Chapter III — What the Door Remembers.
 *
 * Resolves Chapter II's cliffhanger (does Kaelen take the office, and does he take it fully,
 * reluctantly, or only after walking away from it first). The third option is a genuine detour,
 * not a parallel storyline: refusing at `the_answer` sends Kaelen back up through the tunnels to
 * Stonebeard Hold to actually sit with the decision (`the_walk_away`, `what_the_road_offers`),
 * until a tremor in the stone — the same kind of ward failure felt in Chapter II, not a voice or a
 * vision, since Kaelen isn't bound yet and has no way to be reached directly — pulls him back
 * (`the_ground_remembers`, `the_return`) to a third binding-rite variant (`the_binding_rite_returned`)
 * that rejoins the other two paths at the same next scene. Sets `refused_once`, a flag no later
 * choice requires but that's there for future dialogue to reference for texture, the same role
 * `accepted_fully`/`accepted_reluctantly` already play. Also gives the thing behind the gate its
 * first real voice instead of just a knock (and Kaelen's reaction to what happens next differs
 * depending on whether he listened to it or refused it outright), and introduces the first external
 * antagonist — someone from Kaelen's own world, above ground, who has heard that a jailer has been
 * named and does not consider that good news. The chapter-ending confrontation with her is a genuine
 * three-way fork: standing his ground, arguing for mercy, and stalling for time each earn a distinct
 * reaction from Voss, not just a distinct stat. Ends on a real hook rather than a resolution: a
 * named threat now knows Kaelen exists, and knows what he's guarding.
 */
val chapter3Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_answer",
        chapterId = "chapter_3",
        title = "The Answer",
        illustrationId = "the_answer",
        narrativeText = """
            Halvard waits the way old men wait for weather: patient, certain it will come whether
            or not he's ready for it.

            Kaelen thinks of the cloak he doesn't wear anymore. Of a ruined road and a
            name shouted across the rain. Of a door that knocked for him specifically, out of
            everyone who might have walked that way before him and kept walking.

            "Say it plainly," Halvard says. "I'm too old for a man who needs three sentences to
            say yes, and this gate is too tired to wait for him to find them."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I accept the office. All of it.\"",
                nextNodeId = "the_binding_rite_full",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = CHAPTER3_FAMILY,
                    setFlags = setOf("accepted_fully")
                )
            ),
            Choice(
                label = "\"I'll hold the gate. Until someone better is found.\"",
                nextNodeId = "the_binding_rite_reluctant",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = CHAPTER3_FAMILY,
                    setFlags = setOf("accepted_reluctantly")
                )
            ),
            Choice(
                label = "\"I need time. This isn't something I can just say yes to.\"",
                nextNodeId = "the_walk_away",
                consequences = Consequences(clearFlags = CHAPTER3_FAMILY,
                    setFlags = setOf("refused_once"))
            )
        )
    ),
    StoryNode(
        id = "the_binding_rite_full",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really: no crown, no witnesses but one tired dwarf and
            a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer than a
            finger and presses it flat against the black stone, and Kaelen doesn't flinch from it.
            He finds, a little to his own surprise, that he doesn't want to.

            The wards that still hold drink the offering like they've been thirsty for it. For one
            heartbeat, Kaelen feels the full weight of the thing on the other side of the gate:
            not its shape, not its name, only its patience, vast and undiminished by however many
            centuries it has spent waiting for exactly this. He meets it anyway. Better to know
            what he's agreed to hold than to hold it blind.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand, and it doesn't feel like a wound so much as a
            promise finally written down where it can't be taken back.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Endure it without a sound.",
                nextNodeId = "the_first_true_knock",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("jailers_brand"),
                    setFlags = setOf("bound_to_the_gate"),
                    unlockTrophy = "The Last Jailer"
                )
            )
        )
    ),
    StoryNode(
        id = "the_binding_rite_reluctant",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really: no crown, no witnesses but one tired dwarf and
            a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer than a
            finger and presses it flat against the black stone. Kaelen holds still and lets him,
            jaw set, the way he used to hold still for a battlefield surgeon: not because it
            doesn't hurt, but because flinching never once made anything hurt less.

            The wards that still hold drink the offering like they've been thirsty for it. For one
            heartbeat, Kaelen feels the full weight of the thing on the other side of the gate,
            not its shape, not its name, only its patience, vast and undiminished by however many
            centuries it has spent waiting for exactly this. He doesn't welcome the feeling. He
            just refuses to look away from it first.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand. Kaelen flexes his hand once, testing the ache of
            it, and says nothing. Some promises don't need saying twice to be kept.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Endure it without a sound.",
                nextNodeId = "the_first_true_knock",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("jailers_brand"),
                    setFlags = setOf("bound_to_the_gate"),
                    unlockTrophy = "The Last Jailer"
                )
            )
        )
    ),
    StoryNode(
        id = "the_walk_away",
        chapterId = "chapter_3",
        title = "The Walk Away",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Halvard doesn't call after him. That's somehow worse than if he had.

            Kaelen climbs back through the tended dark the way he came, past the warded gate and
            the first seal humming faint and unsteady behind him, and tells himself this is only
            a delay. He isn't sure yet whether he believes it.

            Stonebeard Hold is quiet when he reaches it. None of the dwarves ask why the man who
            walked into their lower passage looks like he's carrying something heavier than a
            sword. He finds a corner and lets himself, for the first time since the black door,
            simply not decide. A debt with no ending date and no one left to relieve him of it.
            Halvard's voice, six wardens down to one, and what it cost the old dwarf to ask a
            stranger instead of no one at all. Neither thought makes the decision for him.

            Then, deep in the night, the floor does. A tremor rolls up through the stone, the same
            low shudder he felt once already when a ward gave out beneath his hands. Nobody else
            in the hold reacts; they have learned not to, with a mountain settling. Kaelen knows
            what this kind of settling means: something down there is losing ground, and there is
            exactly one dwarf left between it and the surface.

            He's on his feet before he's decided to be.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go back.",
                nextNodeId = "the_return",
                consequences = Consequences(statDeltas = mapOf(StatType.HONOUR to 1))
            )
        )
    ),
    // what_the_road_offers and the_ground_remembers are no longer reached by new play (their content now lives in
    // the_walk_away). They are kept, unchanged, so a save parked on either still plays on exactly as before.
    StoryNode(
        id = "what_the_road_offers",
        chapterId = "chapter_3",
        title = "What the Road Offers",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Stonebeard Hold is quiet by the time he reaches it, the dwarves who survived First
            Blood already back at work rebuilding what the fight cost them, and none of them ask
            why the man who walked into their lower passage looks like he's carrying something heavier
            than a sword. He finds a corner to sit in and lets himself, for the first time since
            the black door, simply not decide anything for a while.

            He thinks of the cloak he doesn't wear anymore, and what accepting Halvard's offer
            would actually mean: not a new cloak, no colours to replace the old ones, only a debt
            with no ending date and no one left to relieve him of it. He thinks, too, of Halvard's
            voice, six wardens down to one, and what it cost the old dwarf to ask a stranger
            instead of no one at all. Neither thought makes the decision for him. He lets both
            sit.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, for now.", nextNodeId = "the_ground_remembers")
        )
    ),
    StoryNode(
        id = "the_ground_remembers",
        chapterId = "chapter_3",
        title = "The Ground Remembers",
        illustrationId = "warded_gate",
        narrativeText = """
            It isn't a voice that changes his mind. It's the floor.

            A tremor rolls up through Stonebeard's stone in the depth of the night, faint but
            unmistakable, the same low shudder Kaelen felt once already when a ward gave out
            beneath his hands. It passes in a few seconds. Nobody else in the hold seems to
            notice, or if they do, they've learned not to react to a mountain settling. Kaelen
            has felt this specific kind of settling before, close enough to it to know exactly
            what it means: something down there is losing ground, and there's exactly one dwarf
            left standing between it and the surface.

            He's on his feet before he's decided to be.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go back.",
                nextNodeId = "the_return",
                consequences = Consequences(statDeltas = mapOf(StatType.HONOUR to 1))
            )
        )
    ),
    StoryNode(
        id = "the_return",
        chapterId = "chapter_3",
        title = "The Return",
        illustrationId = "first_seal",
        narrativeText = """
            Halvard doesn't look surprised to see him climbing back down through the tended
            dark, though something in his shoulders eases all the same. The seal is worse than
            when Kaelen left it, a fresh crack spidering out from the same place that failed once
            already, and Halvard's hands are shaking with more than just age by the time Kaelen
            reaches him.

            "You came back," the old warden says, not quite a question.

            "I needed to know I was choosing it. Not just falling into it because a door happened
            to knock on the right night." Kaelen means it more than he expected to, once he's
            said it out loud.

            Halvard studies him a moment longer, then nods, once, like a man closing a ledger
            he'd half expected to stay open. "Then choose it properly this time."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Give him your answer, for real this time.", nextNodeId = "the_binding_rite_returned")
        )
    ),
    StoryNode(
        id = "the_binding_rite_returned",
        chapterId = "chapter_3",
        title = "The Binding Rite",
        illustrationId = "the_binding_rite",
        narrativeText = """
            There is no ceremony to it, not really: no crown, no witnesses but one tired dwarf
            and a gate full of dying light. Halvard opens Kaelen's palm with a blade no longer
            than a finger and presses it flat against the black stone, and this time Kaelen
            doesn't need to brace himself for it the way he expected to. Choosing it twice, it
            turns out, is easier than choosing it once.

            The wards that still hold drink the offering like they've been thirsty for it. For
            one heartbeat, Kaelen feels the full weight of the thing on the other side of the
            gate: not its shape, not its name, only its patience, vast and undiminished by
            however many centuries it has spent waiting for exactly this, and utterly indifferent
            to how long it took him to arrive. He doesn't resent that. He walked away once
            already knowing this was waiting either way.

            When Halvard lifts his hand away, there is a mark on Kaelen's palm that was not there
            before, small and precise as a brand. It doesn't feel like the ending of anything. It
            feels, for the first time since the black door, like the start of something he
            actually chose.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Endure it without a sound.",
                nextNodeId = "the_first_true_knock",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("jailers_brand"),
                    setFlags = setOf("bound_to_the_gate"),
                    unlockTrophy = "The Last Jailer"
                )
            )
        )
    ),
    StoryNode(
        id = "the_first_true_knock",
        chapterId = "chapter_3",
        title = "The First True Knock",
        illustrationId = "the_first_true_knock",
        narrativeText = """
            That night, if the deep road can even be said to have nights, something knocks from
            the other side of the gate for the first time since the black door. Knuckles this
            time, not stone against stone. Deliberate. Patient, the way the whole thing has always
            been patient.

            Then a voice, or the memory of one, shaped out of the same pressure that used to just
            be a knock: it knows his name. It knows what his cloak used to mean. It offers, almost
            gently, to give it back to him: the silver, the rank, the version of Kaelen that never
            failed anyone. All he would have to do is listen a little longer.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen to what it's offering.",
                nextNodeId = "voices_above_after_listening",
                consequences = Consequences(setFlags = setOf("listened_to_the_prisoner"))
            ),
            Choice(
                label = "Turn away before it finishes.",
                nextNodeId = "voices_above_after_refusing",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_the_prisoner")
                )
            )
        )
    ),
    StoryNode(
        id = "voices_above_after_listening",
        chapterId = "chapter_3",
        title = "Voices Above",
        illustrationId = "voices_above",
        narrativeText = """
            Kaelen doesn't answer it, in the end, but he doesn't stop listening as fast as he
            should have either, and the silence after feels different for that. Heavier. Like a
            door he meant to close and only pulled most of the way shut.

            Days blur together this far under the earth, and it's Halvard who notices the next
            change first: a vibration in the stone that has nothing to do with the gate, coming
            from directly overhead. Boots, more than a few, moving with the unhurried confidence
            of people who think they're the only ones with a claim on what's up there.

            "Nobody's climbed that road in longer than I've kept this post," Halvard says.
            "Someone has, now." Kaelen is almost grateful for the interruption. It's easier to
            think about boots overhead than about how much of that offer he let himself actually
            hear.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Climb back up to the black door.",
                nextNodeId = "the_surface_again",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("owes_the_smith"))
            ),
            Choice(
                label = "Climb back up to the black door.",
                nextNodeId = "ch3_halvards_ledger",
                requirements = ChoiceRequirement(requiredFlags = setOf("owes_the_smith"))
            ),
            Choice(
                label = "Come up by the broken ground instead.",
                nextNodeId = "ch3_order_camp",
                consequences = Consequences(setFlags = setOf("approached_by_camp"))
            )
        )
    ),
    StoryNode(
        id = "voices_above_after_refusing",
        chapterId = "chapter_3",
        title = "Voices Above",
        illustrationId = "voices_above",
        narrativeText = """
            Kaelen turns from it before the offer finishes, and the silence that follows feels
            earned rather than heavy, though some small, tired part of him keeps turning the words
            over anyway, the way a tongue keeps finding a broken tooth.

            Days blur together this far under the earth, and it's Halvard who notices the next
            change first: a vibration in the stone that has nothing to do with the gate, coming
            from directly overhead. Boots, more than a few, moving with the unhurried confidence
            of people who think they're the only ones with a claim on what's up there.

            "Nobody's climbed that road in longer than I've kept this post," Halvard says.
            "Someone has, now." Kaelen is glad of somewhere else to point his attention. Refusing
            was the easy part. Not wondering what he refused is proving harder.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Climb back up to the black door.",
                nextNodeId = "the_surface_again",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("owes_the_smith"))
            ),
            Choice(
                label = "Climb back up to the black door.",
                nextNodeId = "ch3_halvards_ledger",
                requirements = ChoiceRequirement(requiredFlags = setOf("owes_the_smith"))
            ),
            Choice(
                label = "Come up by the broken ground instead.",
                nextNodeId = "ch3_order_camp",
                consequences = Consequences(setFlags = setOf("approached_by_camp"))
            )
        )
    ),
    StoryNode(
        id = "ch3_halvards_ledger",
        chapterId = "chapter_3",
        title = "A Count, Not a Complaint",
        illustrationId = "warded_gate",
        narrativeText = """
            Halvard stops him at the foot of the stair with a hand like a knot of root. "The smith sent word by the last man down," he says. "Not a complaint. Only a count."

            He does not say a count of what. He does not need to. Kaelen has been counting a shelf since the Hold.

            "He will not ask," Halvard says. "That is what makes it heavy."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Give Halvard forty gold to carry back.",
                nextNodeId = "the_surface_again",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.GOLD to 40)),
                lockedDisplay = LockedDisplay.SHOW_LOCKED,
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to -40, StatType.HONOUR to 1),
                    setFlags = setOf("returned_the_draught"),
                    clearFlags = setOf("owes_the_smith")
                )
            ),
            Choice(
                label = "Spend the night hauling stone for the gate, and ask him to tell the smith.",
                nextNodeId = "the_surface_again",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -10, StatType.HONOUR to 1),
                    setFlags = setOf("returned_the_draught"),
                    clearFlags = setOf("owes_the_smith")
                )
            ),
            Choice(
                label = "Say nothing, and climb.",
                nextNodeId = "the_surface_again"
            )
        )
    ),
    StoryNode(
        id = "ch3_order_camp",
        chapterId = "chapter_3",
        title = "Above the Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            There is a second way up from the deep road: a place where the hillside has fallen in over an old tunnel mouth, and a man can climb out through the roots without ever passing the door. Kaelen takes it, and comes out on his belly in wet bracken above the road, with the first grey of morning coming up behind the trees.

            Below him, the Order has made camp in the road. Four horses. Four bedrolls. A banked fire under an oilcloth, and a stores tent pegged against the weather. At the door itself a woman in dark colours is standing very still with her hand flat on the iron.

            Nobody has seen him.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Study the camp from cover.",
                nextNodeId = "ch3_camp_studied",
                consequences = Consequences(setFlags = setOf("read_voss_orders"))
            ),
            Choice(
                label = "Slip along the supply line.",
                nextNodeId = "ch3_camp_robbed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to -1, StatType.GOLD to 10),
                    grantItemIds = listOf("healing_draught"),
                    setFlags = setOf("robbed_the_order")
                )
            ),
            Choice(
                label = "Walk down and ask the sentry for leave to speak.",
                nextNodeId = "ch3_camp_parley",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 1)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("parleyed_with_sentry")
                )
            ),
            Choice(
                label = "Walk down to the door.",
                nextNodeId = "ch3_surface_approached"
            )
        )
    ),
    StoryNode(
        id = "ch3_camp_studied",
        chapterId = "chapter_3",
        title = "What the Oilcloth Covered",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Under a stone, against the wind, one sheet is weighted to the lid of a ration crate. A door. A place. A day. And beneath it in the same square hand: confirm whether it has found a keeper before you engage. Do not engage until confirmed.

            It is not the Order's strength that stays with him. It is how little of this is certain. They have come to find out.
        """.trimIndent(),
        choices = listOf(Choice(label = "Walk down to the door.", nextNodeId = "ch3_surface_approached"))
    ),
    StoryNode(
        id = "ch3_camp_robbed",
        chapterId = "chapter_3",
        title = "The Supply Line",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The sentry watches the road and not the bracken, and the stores tent has a loose flap. A sack of coin set aside for fodder. A stoppered vial in a case of them. Kaelen takes the coin and one vial, and does not take the rest, and is back under the roots before the fire has settled.

            Whoever counts these stores in the morning will count them twice.
        """.trimIndent(),
        choices = listOf(Choice(label = "Walk down to the door.", nextNodeId = "ch3_surface_robbed"))
    ),
    StoryNode(
        id = "ch3_camp_parley",
        chapterId = "chapter_3",
        title = "Leave to Speak",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The sentry hears him out with a hand on his hilt and his eyes on the treeline. Kaelen keeps his own hands open and in sight, and says only that he has come to speak to whoever commands here.

            For reasons he does not give, the man steps aside. "She's at the door," he says. "She'll want to see you."
        """.trimIndent(),
        choices = listOf(Choice(label = "Walk down to the door.", nextNodeId = "ch3_surface_parleyed"))
    ),
    StoryNode(
        id = "ch3_surface_robbed",
        chapterId = "chapter_3",
        title = "The Surface, Again",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            He comes down out of the bracken onto the road, and four horses lift their heads together. At the stores tent a rider has stopped with the flap in his hand, counting something twice. The woman at the door does not turn until Kaelen is close enough to touch.

            "There you are," she says, as if she had been expecting him specifically. "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist had opened out here, and that it had found itself a keeper."

            She does not offer her hand. Her eyes go once to his face, once to his hands, and back. "A strange morning," she says pleasantly. "A sack of fodder money has walked out of a tent nobody walked into. I'm sure that's a coincidence." Behind her, no one moves, and no one takes a hand from a hilt.
        """.trimIndent(),
        choices = vossRows(camp = false)
    ),
    StoryNode(
        id = "ch3_surface_parleyed",
        chapterId = "chapter_3",
        title = "The Surface, Again",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            He comes down out of the bracken onto the road with his hands open, and four horses lift their heads together. The sentry has already crossed to the door ahead of him and says something short and low. The woman at the door does not turn until Kaelen is close enough to touch.

            "There you are," she says, as if she had been expecting him specifically. "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist had opened out here, and that it had found itself a keeper."

            Then she does turn, and looks at his open hands before she looks at his face. "My man says you asked leave to speak before you came down. Most don't. Most come from behind." Behind her, one of the four takes his hand from his hilt, and does not seem to know he has done it. "That is noted," Voss says. "It is not a kindness. It is manners, and they are rarer than kindness in my work."
        """.trimIndent(),
        choices = vossRows(camp = false)
    ),
    StoryNode(
        id = "ch3_surface_approached",
        chapterId = "chapter_3",
        title = "The Surface, Again",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            He comes down out of the bracken onto the road, and four horses lift their heads together. Four riders in colours he doesn't recognize watch him come. The woman at the door does not turn until he is close enough to touch.

            "There you are," she says, as if she had been expecting him specifically. "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist had opened out here, and that it had found itself a keeper."
        """.trimIndent(),
        choices = vossRows(camp = true)
    ),
    StoryNode(
        id = "ch3_voss_waits",
        chapterId = "chapter_3",
        title = "Voss, Waiting",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            Voss waits, in no hurry, with the rain running off her hood. The four behind her wait too.
        """.trimIndent(),
        choices = vossRows(camp = true)
    ),
    StoryNode(
        id = "ch3_voss_names",
        chapterId = "chapter_3",
        title = "A Name on the Road",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "You were expecting me. By name."

            Voss's mouth does something that is not quite a smile. "Names carry, jailer. Yours was being shouted loud enough on that road to reach anyone who was listening." She does not say where she heard it. The way she does not say it is practised.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn back to her.", nextNodeId = "ch3_voss_waits"))
    ),
    StoryNode(
        id = "ch3_voss_shared",
        chapterId = "chapter_3",
        title = "Hired, and Wanted Whole",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "The riders on the road were hired. Paid in halves, to bring me in alive."

            Voss is quiet for a moment. "Not ours," she says. "Ours wear a colour, and we do not buy a man in halves. Someone wanted you whole, jailer, and wanted it enough to pay for it." She looks at the door, and then at him. "I would want to know who, if I were you. I would want it before they found a reason to stop paying."
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn back to her.", nextNodeId = "ch3_voss_waits"))
    ),
    StoryNode(
        id = "ch3_voss_sold",
        chapterId = "chapter_3",
        title = "A Price for the Riders",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "What I know about those riders has a price."

            She laughs once, short and without much heat, and counts ten coins into his palm from a purse at her belt. He tells her what he knows: hired, paid in halves, wanted alive. "I had guessed all of it," she says. "You would have given it for nothing, if I had waited." It is not an insult. It is worse. It is a note made in a ledger.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn back to her.", nextNodeId = "ch3_voss_waits"))
    ),
    StoryNode(
        id = "ch3_voss_disc",
        chapterId = "chapter_3",
        title = "The Scraped Wax",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            He holds out the grey wax disc. Voss turns it over in the rain, a thumbnail along the flattened face. "Someone took a file to this before it ever left their hand," she says. "Not an accident. A seal scraped like this is a seal that was never meant to be traced."

            She gives it back. "I cannot tell you whose. I can tell you they were careful."
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn back to her.", nextNodeId = "ch3_voss_waits"))
    ),
    StoryNode(
        id = "ch3_voss_orders",
        chapterId = "chapter_3",
        title = "Orders, Quoted",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "Your orders say to confirm before you engage," Kaelen says. "Consider it confirmed."

            For the first time, Voss's attention comes fully off the door. "Someone," she says softly, "has been reading over my shoulder." It is not quite a threat. She files it away with the rest of him.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn back to her.", nextNodeId = "ch3_voss_waits"))
    ),
    StoryNode(
        id = "the_surface_again",
        chapterId = "chapter_3",
        title = "The Surface, Again",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            Rain again, or still; Kaelen has honestly lost track of how long he's been below. The
            ancient tree looks exactly as he left it. The black door does not.

            Four riders wait beside it, colours he doesn't recognize, and a fifth figure standing
            close enough to the iron to have already tried it. She turns before Kaelen's boots
            clear the roots. "There you are," she says, as if she'd been expecting him
            specifically. "Inquisitor Voss. Ashen Order. We had word a door that shouldn't exist
            had opened out here, and that it had found itself a keeper."
        """.trimIndent(),
        choices = vossRows(camp = false)
    ),
    StoryNode(
        id = "a_choice_at_the_door",
        chapterId = "chapter_3",
        title = "A Choice at the Door",
        illustrationId = "a_choice_at_the_door",
        narrativeText = """
            "The Order doesn't believe in locked doors," Voss says. "Only opened ones and buried
            ones. Your gate has had three centuries to be the second kind and failed at it. We're
            here to finish the job properly: brick, fire, and enough of both that nothing behind
            that door ever knocks again. You, and whatever dwarves are left down there, are
            welcome to stand aside."

            She hasn't drawn a weapon. She hasn't needed to. The four behind her have, and they
            hold them the unhurried way people hold weapons they expect to use exactly once and
            correctly.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Stand aside yourself. This isn't yours to bury.\"",
                nextNodeId = "chapter3_end_stood_ground",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 3)),
                // Pilot (approved): shown locked below Courage 3. Reason text is generated from the stat.
                lockedDisplay = LockedDisplay.SHOW_LOCKED,
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("stood_his_ground")
                )
            ),
            Choice(
                label = "\"There's a man alive behind that gate who deserves better than fire.\"",
                nextNodeId = "chapter3_end_argued_mercy",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("argued_for_mercy")
                )
            ),
            Choice(
                label = "\"Give me time to send word below before you try.\"",
                nextNodeId = "chapter3_end_stalled",
                consequences = Consequences(setFlags = setOf("stalled_for_time"))
            )
        )
    ),
    StoryNode(
        id = "chapter3_end_stood_ground",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Something in Voss's expression changes, subtle but real, the look of a woman revising
            an estimate she'd already written down in ink. She doesn't back away from the door.
            She does, for the first time, look at Kaelen like an obstacle rather than an
            inconvenience.

            "Most people who tell the Order to stand aside are already running while they say
            it," she says, swinging back up onto her horse without hurrying. "You're not running.
            That's either very brave or very stupid, jailer, and I've learned not to bet against
            either one too quickly." She studies him a moment longer, weighing something she
            doesn't share. "The Ashen Order doesn't send one rider and four swords for a rumour.
            We send them for a confirmation. Congratulations. You've just been confirmed, and I
            don't think you're going to enjoy what comes next."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and for the first time since the rain started, standing his ground doesn't
            feel like it was the easy half of the fight.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    ),
    StoryNode(
        id = "chapter3_end_argued_mercy",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Voss listens to the whole of it without interrupting, which somehow makes the silence
            after worse than any argument would have. "Mercy," she says at last, turning the word
            over like a coin she isn't sure is real. "The Order stopped weighing mercy against
            what's behind doors like this one a long time before I joined it. I'm not unconvinced
            you believe what you just said. I am entirely unconvinced it changes anything."

            She swings back up onto her horse, and for just a moment, before her expression closes
            again, something almost like doubt crosses it. "You'll want to hear this from me and
            not from someone with less patience for a plea like yours," she says. "The Ashen Order
            doesn't send one rider and four swords for a rumour. We send them for a confirmation.
            Congratulations, jailer. You've just been confirmed."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and he finds himself hoping, against most of what he knows about people like
            her, that the doubt he saw was real.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    ),
    StoryNode(
        id = "chapter3_end_stalled",
        chapterId = "chapter_3",
        title = "End of Chapter III — What the Door Remembers",
        illustrationId = "chapter3_threshold",
        narrativeText = """
            Voss doesn't even pretend to consider it. "Send word to whom, exactly?" she says,
            almost gently, the way a person points out a lie too small to be worth real anger
            over. "There's no one left down there to send it to but an old dwarf and a door. I
            admire the attempt. I don't reward it."

            She swings back up onto her horse without hurrying, unbothered, already thinking past
            him. "You'll want to hear this from me and not from someone with less patience," she
            says. "The Ashen Order doesn't send one rider and four swords for a rumour. We send
            them for a confirmation. Congratulations, jailer. You've just been confirmed, and
            you've bought yourself less time than you think you have."

            She and her riders leave the way they came. The black door stays shut behind Kaelen,
            for now, and the silence afterward feels less like a reprieve than like a held breath
            he doesn't get to let out yet.

            Chapter IV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep watch, for now.", nextNodeId = "the_order_returns")
        )
    )
)

val chapter3 = com.thelastjailer.app.Chapter(
    id = "chapter_3",
    number = 3,
    title = "Chapter III — What the Door Remembers",
    startNodeId = "the_answer"
)
