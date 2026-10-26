package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * The four markers every hub in Chapters VIII and IX shares. A hub sets one marker per place seen and the
 * second-visit marker, and the choice that leaves the hub clears all four, so no marker outlives its hub and
 * the story needs four flag names for every hub it will ever have, not four for each.
 */
val HUB_MARKERS: Set<String> = setOf("hub_a", "hub_b", "hub_c", "hub_second")

/** Family C8a: how Kaelen left the Sanctum's wall. Fighting the sentinel sets neither. */
val C8_EXIT_FAMILY: Set<String> = setOf("c8_exit_ramp", "c8_exit_tithe")

/** Family C8b: how Kaelen spent the long night after Voss was taken. */
val C8_NIGHT_FAMILY: Set<String> = setOf("c8_wrote_halvard", "c8_ash_farmers", "c8_marked_route")

/** Family C8c: how Kaelen answered Voss on the third day of the road (the two shipped answers). */
val C8_ROAD_FAMILY: Set<String> = setOf("reassured_voss", "acknowledged_the_risk")

/** Family C8d: how Kaelen approached the walls (the two shipped approaches). */
val C8_APPROACH_FAMILY: Set<String> = setOf("moved_in_stealthily", "circled_wide")

/** Family C8e: the vow made on the road south (the three shipped vows). */
val C8_VOW_FAMILY: Set<String> = setOf("vowed_to_return_now", "vowed_to_return_prepared", "undecided_on_voss")

/**
 * Everything Chapter VIII records, the shipped flags included. The chapter's first choices clear it all, so a replay
 * reaches the Ash Road with an empty slate. Chapter VII's flags are left alone: they describe a telling the replay has
 * already decided again.
 */
val C8_TELLING_FLAGS: Set<String> = setOf(
    "c8_rotation", "c8_chapel_story", "c8_tithe_seen", "c8_cart_robbed", "c8_seal_spent", "c8_voss_cells",
    "stole_the_schematic", "lost_to_sanctum_sentinel"
) + C8_EXIT_FAMILY + C8_NIGHT_FAMILY + C8_ROAD_FAMILY + C8_APPROACH_FAMILY + C8_VOW_FAMILY + HUB_MARKERS

/**
 * One continue choice for a place on the Ash Road: back to the road with one place seen, or on to the third day once
 * the second is seen. Exactly one of the pair is ever visible. The second leaves the hub and clears its markers.
 */
private fun ashOnward(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c8_road_hub_two",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_second")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "the_road_to_ashfall",
        requirements = ChoiceRequirement(requiredFlags = setOf("hub_second")),
        consequences = consequences.copy(clearFlags = consequences.clearFlags + HUB_MARKERS)
    )
)

/**
 * The same pair, for a place whose "go on" also moves Voss's rung by [delta] (clamped). Written once per rung; exactly one
 * of the six is ever visible, and the lowest rung also covers a save that holds no rung at all.
 */
private fun ashOnwardRung(label: String, delta: Int, base: Consequences = Consequences()): List<Choice> =
    rungOrder.indices.flatMap { i ->
        val target = rungOrder[(i + delta).coerceIn(0, rungOrder.size - 1)]
        val rungRequired = if (i == 0) emptySet() else setOf(rungOrder[i])
        val rungForbidden = if (i == 0) setOf("c7_rung_working", "c7_rung_trusted") else emptySet()
        val moved = base.copy(clearFlags = base.clearFlags + C7_RUNG_FAMILY, setFlags = base.setFlags + target)
        listOf(
            Choice(
                label = label,
                nextNodeId = "c8_road_hub_two",
                requirements = ChoiceRequirement(rungRequired, rungForbidden + "hub_second"),
                consequences = moved
            ),
            Choice(
                label = label,
                nextNodeId = "the_road_to_ashfall",
                requirements = ChoiceRequirement(rungRequired + "hub_second", rungForbidden),
                consequences = moved.copy(clearFlags = moved.clearFlags + HUB_MARKERS)
            )
        )
    }

/**
 * Chapter VIII — The Seventh Door.
 *
 * Delivers on Chapter VII's destination and reframes the entire threat. The road to Ashfall is now two days of
 * choices before the confession on the third: a deserter with a guard rotation, a burned chapel, a tithe cart, of
 * which two can be seen. Recon at the Sanctum reveals that Ilsevet isn't trying to open or destroy the six warded
 * gates, she's building a seventh out of pieces taken from all six, which explains why she wanted Kaelen's brand. When
 * the horn sounds there are three ways out: the sentinel (a fight that pays), the slag ramp (it costs strength), the
 * tithe gate (it spends the signet). Whichever he takes, Voss is taken alive, and what the rung between them has become
 * decides whether she is caught or lets herself be. The long night after is spent on a letter, on the ash-farmers, or
 * on marking the way back, and the vow that follows ends the chapter on a debt Kaelen owes.
 */
val chapter8Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c8_ash_camp",
        chapterId = "chapter_8",
        title = "The First Night",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The Ash Road begins where the last farm gives up. For two days the ground goes from brown
            to grey to a grey that has stopped pretending to be anything else, and the sky comes down
            to meet it. Voss leads and says little. At dusk on the first day she picks a hollow below
            the road's last good wall, builds a fire the size of two hands, and sits with her back to
            the stones and her face to the dark, the way a person sits who has done this alone more
            often than with company.

            Kaelen sits across the fire from her. There is a great deal he could say, and he has the
            sense that she is waiting to see which.
        """.trimIndent(),
        choices = rungChoices(
            label = "Offer her the first sleep. You will take the watch.",
            next = "c8_road_hub_one",
            delta = 1,
            base = Consequences(
                statDeltas = mapOf(StatType.HONOUR to 1),
                clearFlags = C8_TELLING_FLAGS
            )
        ) + rungChoices(
            label = "Keep your own counsel, and your own watch.",
            next = "c8_road_hub_one",
            delta = 0,
            base = Consequences(clearFlags = C8_TELLING_FLAGS)
        )
    ),
    StoryNode(
        id = "c8_road_hub_one",
        chapterId = "chapter_8",
        title = "The Ash Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The Ash Road is longer than it looks on any map Halvard owns, and it is not empty. A thread
            of smoke rises from a hollow at the ash's edge, too thin to belong to a patrol. A ridge to
            the north carries the black bones of a roof that used to be a chapel. And on the road itself,
            half a day behind and gaining, a cart with a grey tarpaulin creaks under the weight of
            somebody's grain.

            There is daylight enough to give two of them a look before the road runs out.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The thin smoke in the hollow.",
                nextNodeId = "c8_deserter",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_a")),
                consequences = Consequences(setFlags = setOf("hub_a"))
            ),
            Choice(
                label = "The burned chapel on the ridge.",
                nextNodeId = "c8_chapel",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_b")),
                consequences = Consequences(setFlags = setOf("hub_b"))
            ),
            Choice(
                label = "The grey cart on the road.",
                nextNodeId = "c8_cart",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_c")),
                consequences = Consequences(setFlags = setOf("hub_c"))
            )
        )
    ),
    StoryNode(
        id = "c8_road_hub_two",
        chapterId = "chapter_8",
        title = "The Ash Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            One place seen, and the light already low and yellow over the grey. The road goes on
            toward a horizon that gives nothing back. There is time for one more, and not for two.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The thin smoke in the hollow.",
                nextNodeId = "c8_deserter",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_a")),
                consequences = Consequences(setFlags = setOf("hub_a", "hub_second"))
            ),
            Choice(
                label = "The burned chapel on the ridge.",
                nextNodeId = "c8_chapel",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_b")),
                consequences = Consequences(setFlags = setOf("hub_b", "hub_second"))
            ),
            Choice(
                label = "The grey cart on the road.",
                nextNodeId = "c8_cart",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_c")),
                consequences = Consequences(setFlags = setOf("hub_c", "hub_second"))
            )
        )
    ),
    StoryNode(
        id = "c8_deserter",
        chapterId = "chapter_8",
        title = "A Dead Fire",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The smoke belongs to a man sitting alone beside a fire that has mostly gone out, a grey
            cloak folded under him and a pin that used to sit at its collar, a closed hand around a
            flame, held in his fist instead of worn. He does not reach for the sword across his knees.
            "I walked out of that place three nights ago," he says. "I would rather not be asked why.
            I can tell you when the watch changes on the south wall, if the asking is worth something
            to you. I am not proud of the price."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Pay him for it.",
                nextNodeId = "c8_deserter_paid",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to -30),
                    setFlags = setOf("c8_rotation")
                )
            ),
            Choice(
                label = "Talk it out of him. He wants to be told he is not a coward.",
                nextNodeId = "c8_deserter_talked",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("c8_rotation")
                )
            ),
            Choice(
                label = "Frighten it out of him.",
                nextNodeId = "c8_deserter_scared",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("c8_rotation")
                )
            )
        ) + ashOnward("Leave him to his fire.")
    ),
    StoryNode(
        id = "c8_deserter_paid",
        chapterId = "chapter_8",
        title = "The Price",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Kaelen counts out thirty, or what he can find of thirty, and the man takes it without
            counting it again. "South wall, the hour after the second bell. Two walk the parapet and
            one sleeps. There are never more." He folds the cloak around the pin and does not look up.
            "It is the sleeping one that gets you killed, if you let it. He wakes badly."
        """.trimIndent(),
        choices = ashOnward("Go on.")
    ),
    StoryNode(
        id = "c8_deserter_talked",
        chapterId = "chapter_8",
        title = "Not a Coward",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            It takes an hour and no coin. Kaelen does not tell the man he did the right thing; he
            does not know that. He tells him that leaving was the first thing anyone there has done
            that cost something, and that a coward does not walk three nights through ash to sit alone
            with it. The man looks at the dead fire for a long time. "South wall, the hour after the
            second bell," he says. "Two walk the parapet and one sleeps. Thank you for not making it
            a bargain."
        """.trimIndent(),
        choices = ashOnward("Go on.")
    ),
    StoryNode(
        id = "c8_deserter_scared",
        chapterId = "chapter_8",
        title = "What Fear Buys",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He gets it all, quickly, in a flat voice with a shake in the middle of it: the south wall,
            the hour after the second bell, two on the parapet and one asleep. It is a good deal more
            than the man meant to sell. Voss, a few paces off, has not moved, and she does not say
            anything about the way Kaelen got it. She does not need to. She watches the deserter walk
            off into the ash with the particular care of someone watching a thing she has decided to
            remember.
        """.trimIndent(),
        choices = ashOnwardRung("Go on.", delta = -1)
    ),
    StoryNode(
        id = "c8_chapel",
        chapterId = "chapter_8",
        title = "The Burned Chapel",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The chapel was small and built to be noticed by no one but the people who needed it: a
            single nave, a belfry that never held a bell, a carved lintel the fire has taken most of.
            Voss stops at the door and does not go in. Her hand has found the lintel without her
            seeming to know it, and there is a place on the stone where a mark has been scratched out so
            long ago that moss has taken the scratches. "I know who kept this place," she says. That is
            all she says.

            Beneath the nave there is an undercroft the fire never reached, dry, with straw pallets
            still laid out in rows.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go down and sleep in the undercroft. You are tired in a way the road did not fix.",
                nextNodeId = "c8_chapel_rested",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to 30))
            ),
            Choice(
                label = "Stand at the door with her, and wait.",
                nextNodeId = "c8_chapel_door",
                consequences = Consequences(setFlags = setOf("c8_chapel_story"))
            )
        ) + ashOnward("Leave it. The light will not wait.")
    ),
    StoryNode(
        id = "c8_chapel_rested",
        chapterId = "chapter_8",
        title = "The Undercroft",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The pallets are cold and dry and smell of old straw, and Kaelen sleeps like a man set down.
            When he wakes, Voss has not moved from the door. She stood the watch the whole time without
            being asked, and does not mention it. The day has cost them an hour they will want later, and
            given him back something he did not know he was missing.
        """.trimIndent(),
        choices = ashOnward("Go on.")
    ),
    StoryNode(
        id = "c8_chapel_door",
        chapterId = "chapter_8",
        title = "What Voss Would Not Say",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He stands beside her and says nothing, which turns out to be the right amount. The wind
            works at the broken lintel. She does not say who kept the place, or what the scratched mark
            was. After a long while she says, to the door, that she had hoped it would still be standing.
            Then she takes her hand from the stone as if it had been somebody else's, and they go on,
            and neither of them mentions it again.
        """.trimIndent(),
        choices = ashOnwardRung("Go on.", delta = 1)
    ),
    StoryNode(
        id = "c8_cart",
        chapterId = "chapter_8",
        title = "The Grey Cart",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The cart catches them at noon, drawn by a patient grey mule and driven by two ash-farmers
            with scarves over their mouths. The grain under the tarpaulin is a tithe, a share of every
            harvest taken to the Sanctum by people who do not look at the spire when it comes into
            view. A collector in black rides behind on a horse, bored, a ledger across his saddle.
            Nobody on the road looks at anyone.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Walk beside the cart a while and watch how the tithe is taken.",
                nextNodeId = "c8_cart_watched",
                consequences = Consequences(setFlags = setOf("c8_tithe_seen"))
            ),
            Choice(
                label = "Take a share from the sacks while the collector's back is turned.",
                nextNodeId = "c8_cart_robbed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to 25, StatType.HONOUR to -1),
                    setFlags = setOf("c8_cart_robbed")
                )
            )
        ) + ashOnward("Let it pass.")
    ),
    StoryNode(
        id = "c8_cart_watched",
        chapterId = "chapter_8",
        title = "How a Tithe Is Taken",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The collector asks for no names. He asks for a mark. Each farmer shows the back of the same
            hand, a thumbprint of grey ash pressed there at the Sanctum's gate on the last visit, and
            the collector's eye goes to the ash and not the face. The cart is waved through a side arch
            by the tithe road, away from the gate the soldiers use. Anyone who carries the right kind
            of mark and walks with the cart will be taken for part of it. Kaelen files that away with
            the thing he is trying not to think about, which is how much of this place runs on people
            not looking.
        """.trimIndent(),
        choices = ashOnward("Go on.")
    ),
    StoryNode(
        id = "c8_cart_robbed",
        chapterId = "chapter_8",
        title = "Light Sacks",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            It is easy, and that is the worst of it. The collector never turns. The farmers do not turn
            either, which means they saw, and chose not to see. A child on the box looks at Kaelen once,
            very steadily, and then at her own feet. He has twenty-five coins more than he had and
            something less that does not have a price. Voss says nothing. Her silence has an edge to it
            that was not there an hour ago.
        """.trimIndent(),
        choices = ashOnwardRung("Go on.", delta = -1)
    ),
    StoryNode(
        id = "the_road_to_ashfall",
        chapterId = "chapter_8",
        title = "The Road to Ashfall",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Voss travels light and says little, which suits Kaelen fine for the first two days.
            On the third, somewhere past the last real town before the Ashfall reaches turn to
            true wasteland, she finally says the thing she's clearly been carrying since
            Stonebeard.

            "If this goes wrong, it's my name that hangs for it as well as yours. I want that
            said plainly, once, before we're close enough that saying it stops mattering." She
            doesn't look at him when she says it, eyes fixed on the grey horizon like the words
            are easier to spend on the road than on a person.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we make sure it doesn't go wrong.\"",
                nextNodeId = "the_sanctum_from_afar_reassured",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C8_ROAD_FAMILY,
                    setFlags = setOf("reassured_voss")
                )
            ),
            Choice(
                label = "\"I know what I'm risking. Let's move.\"",
                nextNodeId = "the_sanctum_from_afar_acknowledged",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C8_ROAD_FAMILY,
                    setFlags = setOf("acknowledged_the_risk")
                )
            )
        )
    ),
    StoryNode(
        id = "the_sanctum_from_afar_reassured",
        chapterId = "chapter_8",
        title = "The Sanctum, From Afar",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Something in Voss's shoulders loosens, fractionally, at the certainty in his voice,
            even if neither of them quite believes certainty is worth much out here. She doesn't
            say thank you. She just nods once and keeps walking.

            The Ashfall Sanctum isn't a fortress so much as a wound with walls built around it:
            black stone quarried from somewhere that clearly wasn't meant to be quarried,
            arranged around a central spire that hums faintly even from this distance, felt more
            than heard.

            Voss goes still beside him. "Three months ago that spire wasn't finished. Whatever
            she's building, she's close."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Move in under cover of the ash-fall itself.",
                nextNodeId = "what_they_see_stealthy",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C8_APPROACH_FAMILY,
                    setFlags = setOf("moved_in_stealthily")
                )
            ),
            Choice(
                label = "Circle wide and find a safer vantage first.",
                nextNodeId = "what_they_see_wide",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C8_APPROACH_FAMILY,
                    setFlags = setOf("circled_wide")
                )
            )
        )
    ),
    StoryNode(
        id = "the_sanctum_from_afar_acknowledged",
        chapterId = "chapter_8",
        title = "The Sanctum, From Afar",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Voss gives him a long look, the kind that measures a man rather than thanks him, and
            something in it settles whatever she'd been carrying better than reassurance would
            have. "Good," is all she says. "Then we don't need to have this conversation twice."

            The Ashfall Sanctum isn't a fortress so much as a wound with walls built around it:
            black stone quarried from somewhere that clearly wasn't meant to be quarried,
            arranged around a central spire that hums faintly even from this distance, felt more
            than heard.

            Voss goes still beside him. "Three months ago that spire wasn't finished. Whatever
            she's building, she's close."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Move in under cover of the ash-fall itself.",
                nextNodeId = "what_they_see_stealthy",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C8_APPROACH_FAMILY,
                    setFlags = setOf("moved_in_stealthily")
                )
            ),
            Choice(
                label = "Circle wide and find a safer vantage first.",
                nextNodeId = "what_they_see_wide",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C8_APPROACH_FAMILY,
                    setFlags = setOf("circled_wide")
                )
            )
        )
    ),
    StoryNode(
        id = "what_they_see_stealthy",
        chapterId = "chapter_8",
        title = "What They See",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            They go in fast, using the ash-fall itself as cover, grey flakes settling on Kaelen's
            shoulders like a sky quietly grieving something. Close enough to risk it, Voss steals
            a set of half-burned plans from an unguarded worktable while Kaelen keeps watch,
            close enough to hear voices through a wall that shouldn't be this thin, and neither
            of them breathes easy until they're clear of the table.

            What the plans show stops them both cold anyway. Not a weapon, not exactly. A frame,
            six-sided, six empty settings marked in a hand that matches the wards on Kaelen's own
            gate.

            "Six jailers," Voss murmurs, reading the same margin notes Kaelen can't quite parse.
            "She's not trying to free what's behind any of the doors. She's trying to build a
            seventh one, using pieces of the six that already exist." Her eyes go to the brand on
            Kaelen's palm without meaning to. He doesn't need her to finish the thought.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get out. Now, before anyone notices what's missing.",
                nextNodeId = "c8_the_horn",
                consequences = Consequences(
                    grantItemIds = listOf("torn_sanctum_plans"),
                    setFlags = setOf("stole_the_schematic")
                )
            )
        )
    ),
    StoryNode(
        id = "what_they_see_wide",
        chapterId = "chapter_8",
        title = "What They See",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            They circle wide instead, losing the better part of an hour finding a rise that
            gives them sight of the whole compound without putting either of them inside a
            patrol's easy reach. From here, Voss can pick out the guard rotations, the unfinished
            sections of wall, the single worktable near the spire's base that nobody seems to be
            watching closely enough.

            Close enough to risk it once they've mapped the gap, Voss steals a set of half-burned
            plans from that table while Kaelen keeps watch, and what they show stops them both
            cold. Not a weapon, not exactly. A frame, six-sided, six empty settings marked in a
            hand that matches the wards on Kaelen's own gate.

            "Six jailers," Voss murmurs, reading the same margin notes Kaelen can't quite parse.
            "She's not trying to free what's behind any of the doors. She's trying to build a
            seventh one, using pieces of the six that already exist." Her eyes go to the brand on
            Kaelen's palm without meaning to. He doesn't need her to finish the thought.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get out. Now, before anyone notices what's missing.",
                nextNodeId = "c8_the_horn",
                consequences = Consequences(
                    grantItemIds = listOf("torn_sanctum_plans"),
                    setFlags = setOf("stole_the_schematic")
                )
            )
        )
    ),
    StoryNode(
        id = "sentinels_close_in",
        chapterId = "chapter_8",
        title = "Sentinels Close In",
        illustrationId = "sentinels_close_in",
        narrativeText = """
            Someone notices. A horn sounds somewhere inside the walls, low and unhurried in a way
            that's somehow worse than an alarm would be: the sound of people who have drilled for
            exactly this and don't need to rush.

            The first sentinel finds them before they clear the ash-field, armor the same black
            stone as the walls behind it, moving like it was quarried rather than born, each step
            grinding like stone settling into stone.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "sanctum_sentinel_encounter"
    ),
    StoryNode(
        id = "sanctum_sentinel_defeat",
        chapterId = "chapter_8",
        title = "Barely Clear",
        illustrationId = "sentinels_close_in",
        narrativeText = """
            The sentinel doesn't fall so much as Kaelen runs out of ways to keep it from closing
            the distance, stone-slow and stone-patient, exactly the kind of fight that punishes
            anyone hoping to end it quickly. He's still moving when it finally gets a grip on him,
            but only because Voss puts herself in the gap at the worst possible moment for her and
            the best possible one for him.

            It costs her more than it should have. It costs him nothing he didn't already have
            coming, and neither of them is in any position to argue about which of those is worse
            right now.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep running.",
                nextNodeId = "the_price_of_escape",
                consequences = Consequences(setFlags = setOf("lost_to_sanctum_sentinel"))
            )
        )
    ),
    StoryNode(
        id = "the_price_of_escape",
        chapterId = "chapter_8",
        title = "The Price of Escape",
        illustrationId = "the_price_of_escape",
        narrativeText = """
            They almost make it clean. Almost. A second horn answers the first, closer, and Voss
            shoves Kaelen hard toward the treeline without breaking stride.

            "Go. I can talk my way out of being caught alone. I can't talk either of us out of
            being caught together." She's already turning back toward the sentinels giving
            chase, putting herself between them and him, and there isn't time left to argue about
            it.

            Kaelen runs. He hears her taken, not killed, shouted orders, not a killing blow, and
            that's the only mercy the wasteland offers him on the way out, thin as it is.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep running until the sanctum is out of sight.",
                nextNodeId = "c8_her_choice",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted")),
                consequences = Consequences(setFlags = setOf("c8_voss_cells"), unlockTrophy = "Her Choice")
            ),
            Choice(
                label = "Keep running until the sanctum is out of sight.",
                nextNodeId = "c8_long_night",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_trusted"))
            )
        )
    ),
    StoryNode(
        id = "what_must_be_risked",
        chapterId = "chapter_8",
        title = "What Must Be Risked",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            Alone on the wasteland road, the schematic Voss stole burning a hole in his coat
            pocket, Kaelen has exactly one certainty left: whatever the Sanctum wants those six
            settings for, it isn't finished yet. That's the only reason Voss is worth anything to
            them alive, and it's a colder comfort than he'd like to admit out loud.

            It isn't much. It's what he's got, and the road doesn't leave him time to look for
            more.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not leaving her there to buy me time I didn't ask for.\"",
                nextNodeId = "c8_vow_now",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 7)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C8_VOW_FAMILY,
                    setFlags = setOf("vowed_to_return_now")
                )
            ),
            Choice(
                label = "\"I go back for her properly, or not at all. Halvard first.\"",
                nextNodeId = "c8_vow_prepared",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C8_VOW_FAMILY,
                    setFlags = setOf("vowed_to_return_prepared")
                )
            ),
            Choice(
                label = "Keep moving. Decide the rest on the road.",
                nextNodeId = "c8_vow_undecided",
                consequences = Consequences(clearFlags = C8_VOW_FAMILY, setFlags = setOf("undecided_on_voss"))
            )
        )
    ),
    StoryNode(
        id = "c8_the_horn",
        chapterId = "chapter_8",
        title = "The Horn",
        illustrationId = "sentinels_close_in",
        narrativeText = """
            Somewhere inside the walls a horn sounds, low and unhurried in a way that is somehow worse
            than an alarm would be: the sound of people who have drilled for exactly this and do not need
            to rush. They have perhaps a minute. Behind them the ash-field is open ground. Ahead, between
            the worktable and the outer wall, the first stone sentinel is already turning its head.

            Voss's eyes go to three places in order: the slag ramp that climbs the wall's eastern
            shoulder, the arch where the tithe carts come in, and Kaelen.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Stand and fight the sentinel.",
                nextNodeId = "sentinels_close_in"
            ),
            Choice(
                label = "Take the slag ramp, timing it to the watch change the deserter sold you.",
                nextNodeId = "c8_capture_ramp",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_rotation")),
                consequences = Consequences(clearFlags = C8_EXIT_FAMILY, setFlags = setOf("c8_exit_ramp"))
            ),
            Choice(
                label = "Take the slag ramp and trust your legs.",
                nextNodeId = "c8_capture_ramp",
                requirements = ChoiceRequirement(
                    forbiddenFlags = setOf("c8_rotation"),
                    minStats = mapOf(StatType.COURAGE to 4)
                ),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -10),
                    clearFlags = C8_EXIT_FAMILY,
                    setFlags = setOf("c8_exit_ramp")
                )
            ),
            Choice(
                label = "Walk out by the tithe arch with the signet in your fist, as if you belonged to the cart.",
                nextNodeId = "c8_capture_gate",
                requirements = ChoiceRequirement(requiredFlags = setOf("earned_voss_seal", "c8_tithe_seen")),
                consequences = Consequences(clearFlags = C8_EXIT_FAMILY, setFlags = setOf("c8_exit_tithe", "c8_seal_spent"))
            )
        )
    ),
    StoryNode(
        id = "c8_capture_ramp",
        chapterId = "chapter_8",
        title = "The Head of the Ramp",
        illustrationId = "the_price_of_escape",
        narrativeText = """
            The ramp is a shelf of cooled slag, wide enough for one and steep enough to punish two.
            Kaelen is a third of the way up when he understands that Voss has stopped climbing.

            "Go," she says, from the foot of it, and the way she says it closes the question. "I will hold
            the bottom of this. They will want me alive, and they will find out how much of that I mean."
            He hears the sentinels reach her. He hears her not stop them. He hears a voice he does not
            know say, calmly, that she is to be taken whole, and then the ramp turns under his boots into
            the wall's shadow and he cannot hear anything else.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep running until the Sanctum is out of sight.",
                nextNodeId = "c8_her_choice",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted")),
                consequences = Consequences(setFlags = setOf("c8_voss_cells"), unlockTrophy = "Her Choice")
            ),
            Choice(
                label = "Keep running until the Sanctum is out of sight.",
                nextNodeId = "c8_long_night",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_trusted"))
            )
        )
    ),
    StoryNode(
        id = "c8_capture_gate",
        chapterId = "chapter_8",
        title = "Recognised",
        illustrationId = "the_price_of_escape",
        narrativeText = """
            The tithe arch admits them both for the length of a breath. The collector under it looks at
            the signet, then at Voss, and does not look at the signet again. "There is a price on your
            name that makes this worth no more than a button," he says, pleasantly, to Voss. "I would not
            have thought you would use it."

            Voss shoves Kaelen sideways, through the grain carts toward the side door, and turns to face
            the arch. "They know me," she says. "They do not know you. Do not make them learn." The signet
            in his hand is a good deal heavier than it was, and a good deal less useful. Behind him the
            horn sounds a second time, and a voice he does not know says that she is to be taken whole.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep running until the Sanctum is out of sight.", nextNodeId = "c8_long_night"))
    ),
    StoryNode(
        id = "c8_her_choice",
        chapterId = "chapter_8",
        title = "Her Choice",
        illustrationId = "the_price_of_escape",
        narrativeText = """
            A quarter of a mile on, the rest of what she said catches up with him, the way a thrown thing
            does. It was under the horn, in the same breath as the order to go, in the flat voice she keeps
            for facts: "They put the ones they want to talk to nearest the east stair. If you come, come
            that way." He had taken it for nothing. It was a route.

            He stops running. She planned this. She did not plan the capture, but she planned the choosing
            of it: she looked at the ways out of that wall, took the one that cost him least, and spent a
            breath she could not spare on telling him how to come back.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep running until the Sanctum is out of sight.", nextNodeId = "c8_long_night")
        )
    ),
    StoryNode(
        id = "c8_long_night",
        chapterId = "chapter_8",
        title = "The Long Night",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            He makes a camp he does not want in a hollow he cannot see out of, and the night that follows
            is the longest he can remember. The plans are a hot coal in his coat. Somewhere behind him a
            woman is being asked questions. There are three things a man can do with a night like this, and
            he can do one of them well.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Write to Halvard. Everything, including what you do not understand.",
                nextNodeId = "c8_night_letter",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C8_NIGHT_FAMILY,
                    setFlags = setOf("c8_wrote_halvard")
                )
            ),
            Choice(
                label = "Go down to the ash-farmers' huts and ask what they know.",
                nextNodeId = "c8_night_farmers",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c8_cart_robbed")),
                consequences = Consequences(clearFlags = C8_NIGHT_FAMILY, setFlags = setOf("c8_ash_farmers"))
            ),
            Choice(
                label = "Mark the way back, cairn by cairn, while you still remember it.",
                nextNodeId = "c8_night_route",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -10, StatType.COURAGE to 1),
                    clearFlags = C8_NIGHT_FAMILY,
                    setFlags = setOf("c8_marked_route")
                )
            )
        )
    ),
    StoryNode(
        id = "c8_night_letter",
        chapterId = "chapter_8",
        title = "A Letter by Firelight",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            The letter takes most of the night. Kaelen is not a man who writes, and it shows; he crosses
            out Voss's name twice and writes it a third time without crossing it out. He sets down the
            plans, and the horn, and the thing the plans seem to be for. He does not set down what he
            thinks of himself for leaving, because Halvard will read that without being told. By dawn the
            page is folded small enough for a bird, and he has the odd, light feeling of having put down
            something he did not know he was carrying.
        """.trimIndent(),
        choices = listOf(Choice(label = "Get up. The vow will not make itself.", nextNodeId = "what_must_be_risked"))
    ),
    StoryNode(
        id = "c8_night_farmers",
        chapterId = "chapter_8",
        title = "What the Ash-Farmers Know",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            The huts are low and shuttered, and nobody opens until Kaelen says that he has no coin and
            no grievance and a friend inside the walls. An old woman with ash worked into the creases of
            her hands lets him sit by the door. The Sanctum has been taking tithe for three winters, she
            says, and for the last two the carts have come back lighter and the drivers quieter. They
            use a cart for the cells' food. Anyone who drives it in is not looked at. She will not drive it
            herself. She will not say that someone else could.
        """.trimIndent(),
        choices = listOf(Choice(label = "Get up. The vow will not make itself.", nextNodeId = "what_must_be_risked"))
    ),
    StoryNode(
        id = "c8_night_route",
        chapterId = "chapter_8",
        title = "Cairn by Cairn",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            He walks back along the way he came in the dark, building small piles of ash-crusted stone at
            every turn he will need to find again: a hand's height, no more, the kind of mark that means
            nothing to anyone who is not looking for it. His boots fill with grit. His hands bleed a little.
            By the time the sky greys he can trace the road to the Sanctum's shoulder with his eyes shut,
            and he is so tired that the road seems to trace itself.
        """.trimIndent(),
        choices = listOf(Choice(label = "Get up. The vow will not make itself.", nextNodeId = "what_must_be_risked"))
    ),
    StoryNode(
        id = "c8_vow_now",
        chapterId = "chapter_8",
        title = "Without Waiting",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            He does not sleep. He turns for Stonebeard before the ash has finished settling, walking
            through what is left of the dark, and the vow goes with him like a second heartbeat. It buys
            him the one thing he is certain he can afford: speed. It spends what speed always spends. By
            the time the first tunnel mouth shows grey he is walking on habit, and his legs have begun to
            argue.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go on.",
                nextNodeId = "chapter8_end",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -15))
            )
        )
    ),
    StoryNode(
        id = "c8_vow_prepared",
        chapterId = "chapter_8",
        title = "Properly",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            He makes himself sleep four hours first, and wakes ashamed of every one of them. Then he does
            what a man does who means to go back properly: he buys what the road can sell, rope and oil
            and a second flask, and counts what he cannot buy. The vow buys him a rescue that has a chance
            of working. It spends days Voss is spending in a cell, and he will not be allowed to forget
            that arithmetic for as long as it takes.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go on.",
                nextNodeId = "chapter8_end",
                consequences = Consequences(statDeltas = mapOf(StatType.GOLD to -20))
            )
        )
    ),
    StoryNode(
        id = "c8_vow_undecided",
        chapterId = "chapter_8",
        title = "Left Open",
        illustrationId = "what_must_be_risked",
        narrativeText = """
            He walks south without deciding, and the not-deciding walks with him, a stone in the boot he
            declines to stop and shake out. It buys him the road's long thinking. It spends nothing
            tonight. It will spend more later, the way debts do that nobody has named, and part of him
            knows that and walks on anyway.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter8_end"))
    ),
    StoryNode(
        id = "chapter8_end",
        chapterId = "chapter_8",
        title = "End of Chapter VIII — The Seventh Door",
        illustrationId = "chapter8_threshold",
        narrativeText = """
            The road south is longer alone than it was with company, even company as guarded as
            Voss's. Kaelen reaches Stonebeard with the schematic, four gates still unaccounted
            for, and a debt he didn't ask to owe but isn't about to set down either.

            Halvard reads the stolen plans in silence, longer than he's ever taken over
            anything, and when he finally looks up, the dry patience is entirely gone from his
            face. "A seventh door," he says. "Built from pieces of the six. She's not trying to
            free what's behind any of them, Kaelen. She's trying to build something new out of
            all of you at once."

            Chapter IX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Decide what happens next.",
                nextNodeId = "a_debt_remembered_trusted",
                requirements = ChoiceRequirement(requiredFlags = setOf("trusted_voss_fully"))
            ),
            Choice(
                label = "Decide what happens next.",
                nextNodeId = "a_debt_remembered_guarded",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("trusted_voss_fully"))
            )
        )
    )
)

val chapter8 = com.thelastjailer.app.Chapter(
    id = "chapter_8",
    number = 8,
    title = "Chapter VIII — The Seventh Door",
    startNodeId = "c8_ash_camp"
)
