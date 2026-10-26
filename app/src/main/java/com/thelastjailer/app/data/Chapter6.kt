package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C6a: which way this telling set out for Greymoor. Both openings clear both, then set their own,
 * so a replay that sets out differently never carries the earlier choice. This runs when a choice is
 * taken, never when a scene is entered or loaded.
 */
val C6_OPENING_FAMILY: Set<String> =
    setOf("chose_to_travel", "chose_deliberately")

/**
 * Family C6b: how this telling met the warden at Greymoor's gate. Each approach clears both, then sets
 * its own.
 */
val C6_GATE_APPROACH_FAMILY: Set<String> =
    setOf("knelt_for_the_warden", "searched_the_gate_first")

/**
 * Family C6c: what this telling decided to do with Greymoor's gate. Every route to the verdict clears
 * all three, then sets its own, so the Journal groups them as one moment and Chapter VII never reads a
 * verdict from an earlier telling.
 */
val C6_VERDICT_FAMILY: Set<String> =
    setOf("sealed_greymoor_personally", "reported_greymoor_honestly", "left_greymoor_as_is")

/**
 * Every flag this chapter owns apart from the three families: the hub markers, what the road and the
 * yard gave, and the one defeat marker. The chapter's opening choices clear all of it, so a replay that
 * reaches Greymoor again starts the road and the yard clean instead of finding the earlier telling's
 * stops already visited. Chapter 5's end choice is not edited; the clear happens here, on Chapter 6's own
 * first decision. A test holds this list equal to every Chapter 6 flag that any choice sets.
 */
val C6_TELLING_FLAGS: Set<String> = setOf(
    "c6_halvard_confided", "c6_halvard_reticent",
    "c6_stop_tollhouse", "c6_stop_cairn", "c6_stop_camp", "c6_road_second",
    "c6_helped_refugees", "c6_met_tam", "c6_pressed_edda", "c6_nail_method", "c6_has_ward_nails",
    "c6_promised_tam", "c6_offered_the_hold", "c6_took_cairn_note",
    "c6_camp_sneaked", "c6_camp_paid", "c6_scout_taken", "c6_scout_freed", "c6_scout_bound",
    "c6_spot_watchpost", "c6_spot_cellar", "c6_spot_garden", "c6_ruin_second",
    "c6_read_ledger", "c6_inspected_breach", "c6_buried_kestrel", "c6_unbound_pinned", "c6_tam_followed",
    "lost_to_unbound_horror"
)

/** Everything the chapter's two opening choices clear before setting their own flag. */
val C6_ENTRY_CLEARS: Set<String> =
    C6_OPENING_FAMILY + C6_GATE_APPROACH_FAMILY + C6_VERDICT_FAMILY + C6_TELLING_FLAGS

/**
 * One continue choice for a stop on the north road: back to the road with one halt made, or on to
 * Greymoor once the second halt is made. Exactly one of the pair is ever visible, because the second
 * halt's marker is set by the road's second hub and cleared when the chapter is entered again.
 */
private fun roadOnward(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c6_road_hub_two",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_road_second")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c6_arrive_greymoor",
        requirements = ChoiceRequirement(requiredFlags = setOf("c6_road_second")),
        consequences = consequences
    )
)

/** The same pair for a place in Greymoor's yard: back to the yard, or on to the dusk once two are seen. */
private fun ruinOnward(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c6_ruin_hub_two",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_ruin_second")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c6_unbound_sighted",
        requirements = ChoiceRequirement(requiredFlags = setOf("c6_ruin_second")),
        consequences = consequences
    )
)

/**
 * Chapter VI — The Weight of Others' Doors.
 *
 * Chapter V ended with the missive's real scope: five other warded gates, not just Kaelen's own.
 * This chapter makes that abstraction concrete by sending Kaelen to check on the nearest of them —
 * and finding it already lost. Where every fight so far has been against something still trying to
 * get out, this chapter's fight is against something that already did, which is a deliberately
 * different kind of threat than the Order/Marshal soldiers of III-V.
 *
 * Arc: the decision to travel (impulsive versus methodical; the methodical road earns Halvard's account
 * of Kestrel, told whole or told partly depending on what Kaelen gave him at the end of V), the north road
 * itself as a three-stop hub of which two can be made (a tollhouse of Greymoor's refugees, a wardens' cairn,
 * a Marshal's riders' camp at the ford), the fallen warden at her gate, Greymoor's yard as a second
 * three-place hub of which two can be seen (her ledger, the cellar where the wards broke, the row of
 * wardens' cairns), the fight against what got loose or, for a Kaelen who has both the method and the
 * nails the warden herself used, a quieter way to end it, a defeat that ends differently depending on
 * whether anyone cared enough to follow him north, a stat-gated choice about what to do
 * with an unwardable ruin (mirroring III/IV/V, each way now paying its own price), and a chapter-ending
 * gut-check that narrows the clock: four gates left, and the Marshal already knows where one of them
 * is.
 *
 * The chapter touches nothing outside itself. It does not read Chapter V's end beyond the one flag that
 * records what Kaelen gave Halvard, and it leaves Chapter VII and Chapter XXIV exactly as they were
 * (Chapter X changes only who the envoy belongs to). Kestrel's ledger is read where it lies and left open to its unfinished line, and her locket
 * is carried, which is what Chapter XXIV says of both. Why Greymoor's wards broke is deliberately left
 * unsettled.
 */
val chapter6Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_choice_of_roads",
        chapterId = "chapter_6",
        title = "The Choice of Roads",
        illustrationId = "the_choice_of_roads",
        narrativeText = """
            Halvard doesn't sleep so much as ration it these days, and the missive sits between
            them on the workbench like a coal neither of them wants to touch first.

            "Five other gates," he says, finally, turning the page over as though the other side
            might read differently. "If even one of them's gone quiet the way Stonebeard nearly
            did, somebody ought to know before Ilsevet's people get there first." He doesn't ask
            Kaelen to go. He doesn't have to. There's only one of him fit to travel, and only one
            of him fit to hold this door, and they both already know which is which.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll go. Someone should see what's actually out there.\"",
                nextNodeId = "the_road_north_volunteered",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C6_ENTRY_CLEARS,
                    setFlags = setOf("chose_to_travel")
                )
            ),
            Choice(
                label = "\"Tell me which gate is nearest, and I'll start there.\"",
                nextNodeId = "the_road_north_deliberate",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C6_ENTRY_CLEARS,
                    setFlags = setOf("chose_deliberately")
                )
            )
        )
    ),
    StoryNode(
        id = "the_road_north_volunteered",
        chapterId = "chapter_6",
        title = "The Road North",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He leaves before he lets himself think better of it, and the world above ground feels
            unchanged and utterly changed at once: the same grey sky, the same ruined roads, but
            every stranger on them now a question. Order, Marshal, or neither. He walks fast
            enough that thinking about which gate to check first never quite catches up with him.

            The north road is old warded road, three days to Greymoor, and the wardens who laid
            it built it with places to stop.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Take the north road.", nextNodeId = "c6_road_hub_one")
        )
    ),
    StoryNode(
        id = "the_road_north_deliberate",
        chapterId = "chapter_6",
        title = "The Road North",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He spends the better part of a day with Halvard's maps first, tracing what's left of
            the old warded roads by lamplight before he ever sets a boot outside. Greymoor sits
            nearest by a few days' walk, which makes the choice of where to start itself easy,
            even if nothing else about it is.

            Halvard finds him at it near midnight, and stands over the map without touching it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Ask Halvard what he knows of the warden there.",
                nextNodeId = "c6_maps_told",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("kept_the_offer_from_halvard"))
            ),
            Choice(
                label = "Ask Halvard what he knows of the warden there.",
                nextNodeId = "c6_maps_unsaid",
                requirements = ChoiceRequirement(requiredFlags = setOf("kept_the_offer_from_halvard"))
            )
        )
    ),
    StoryNode(
        id = "c6_maps_told",
        chapterId = "chapter_6",
        title = "The Warden at Greymoor",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard's thumbnail follows the road north until it stops at a village with a small
            gate drawn beside it. "Kestrel," he says. "Warden of Greymoor, and one of mine,
            though I've never laid eyes on her. She wrote to me once, nine winters back. A
            drover carried it through and nearly lost it in a ford. Four lines, and one of them
            a complaint about the miller." His thumb leaves a grey smudge on the vellum. "I put
            it in the chest by the forge and told myself I would answer when the gate stopped
            needing me."

            "It never stopped," Kaelen says.

            "No." Halvard rolls the map and does not look up. "You told me what that rider
            wanted, so you'll have the whole of this from me. If she is dead, I let her go
            quiet, as surely as the wards did. Go and see. Tell me what it was, and not what
            would be kinder."

            Above ground properly for the first time in longer than he can easily count, Kaelen
            finds the world unchanged and utterly changed at once: the same grey sky, the same
            ruined roads, but every stranger on them now a question. The north road is old
            warded road, three days to Greymoor, and the wardens who laid it built it with
            places to stop.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the north road.",
                nextNodeId = "c6_road_hub_one",
                consequences = Consequences(setFlags = setOf("c6_halvard_confided"))
            )
        )
    ),
    StoryNode(
        id = "c6_maps_unsaid",
        chapterId = "chapter_6",
        title = "The Warden at Greymoor",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard's thumbnail follows the road north until it stops at a village with a small
            gate drawn beside it. "Kestrel," he says. "Warden of Greymoor, and one of mine,
            though I've never laid eyes on her. She wrote to me once, nine winters back. A
            drover carried it through." He says nothing for a moment, and in the silence Kaelen
            can hear him choosing what to leave out. "There is more, and I am not proud of it.
            I would sooner give it to someone who has given me the same. You've the look of a
            man holding a hand shut, lately." The map rolls up under his palm. "When you want to
            trade, I'll trade. Until then, go and see, and tell me what's there."

            Above ground properly for the first time in longer than he can easily count, Kaelen
            finds the world unchanged and utterly changed at once: the same grey sky, the same
            ruined roads, but every stranger on them now a question. The north road is old
            warded road, three days to Greymoor, and the wardens who laid it built it with
            places to stop.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the north road.",
                nextNodeId = "c6_road_hub_one",
                consequences = Consequences(setFlags = setOf("c6_halvard_reticent"))
            )
        )
    ),
    StoryNode(
        id = "c6_road_hub_one",
        chapterId = "chapter_6",
        title = "The North Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Three places along the road are worth a halt. Smoke stands over the old river
            crossing, thin and steady, the smoke of people with nowhere to be. At the fork, a
            heap of stones carries a warden's sign scratched into its topmost slab. And beyond
            the ford, a fire burns too neat to be a shepherd's.

            He has the daylight and the bread for two of them.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The smoke at the river crossing.",
                nextNodeId = "c6_tollhouse",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_tollhouse")),
                consequences = Consequences(setFlags = setOf("c6_stop_tollhouse"))
            ),
            Choice(
                label = "The cairn at the fork.",
                nextNodeId = "c6_cairn",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_cairn")),
                consequences = Consequences(setFlags = setOf("c6_stop_cairn"))
            ),
            Choice(
                label = "The neat fire beyond the ford.",
                nextNodeId = "c6_camp",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_camp")),
                consequences = Consequences(setFlags = setOf("c6_stop_camp"))
            )
        )
    ),
    StoryNode(
        id = "c6_road_hub_two",
        chapterId = "chapter_6",
        title = "The North Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            One halt made, and the road bends toward the hills. There is daylight and bread for
            one more, and the road offers what it offered before, less the place he has already
            seen.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The smoke at the river crossing.",
                nextNodeId = "c6_tollhouse",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_tollhouse")),
                consequences = Consequences(setFlags = setOf("c6_stop_tollhouse", "c6_road_second"))
            ),
            Choice(
                label = "The cairn at the fork.",
                nextNodeId = "c6_cairn",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_cairn")),
                consequences = Consequences(setFlags = setOf("c6_stop_cairn", "c6_road_second"))
            ),
            Choice(
                label = "The neat fire beyond the ford.",
                nextNodeId = "c6_camp",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_stop_camp")),
                consequences = Consequences(setFlags = setOf("c6_stop_camp", "c6_road_second"))
            )
        )
    ),
    StoryNode(
        id = "c6_tollhouse",
        chapterId = "chapter_6",
        title = "The Burned Tollhouse",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The smoke belongs to a tollhouse that burned long ago and was never rebuilt, and to
            two dozen people camped in its shell: farm families, mostly, with carts roped nose to
            tail and a goat tied to a wheel. They watch Kaelen come the way people watch
            weather. A broad woman with flour in the creases of her knuckles rises from the fire
            and does not offer him a seat.

            "Greymoor," she says, when he tells her where he is bound, and spits into the ash.
            "I'm Edda. I was its miller, until the dogs stopped going near my kitchen, which is
            where the gate is. We got out in one piece, all of us, and nobody has been back."
            Behind her a girl of fifteen or so, chalk dusting her sleeves, studies him with the
            flat attention of someone deciding whether to be spoken to.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Stay the night, and help where you can.",
                nextNodeId = "c6_tollhouse_helped",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.GOLD to -15),
                    setFlags = setOf("c6_helped_refugees", "c6_met_tam"),
                    unlockTrophy = "Kestrel's Apprentice"
                )
            ),
            Choice(
                label = "Ask Edda what happened, and press on.",
                nextNodeId = "c6_tollhouse_pressed",
                consequences = Consequences(setFlags = setOf("c6_pressed_edda"))
            )
        )
    ),
    StoryNode(
        id = "c6_tollhouse_helped",
        chapterId = "chapter_6",
        title = "A Night Among Greymoor's People",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            There is work enough for a stranger: a cracked cart wheel to bind with strap and
            wedge, water to carry from the river, a goat to be argued with. He does it without
            being asked and without being thanked. A drover passing through sells meal at a
            robber's price, and Kaelen puts what he can spare into the man's palm for the children and does not mention it. Some time after dark the girl sits down on the
            other side of his fire.

            "I'm Tam," she says. "I was Kestrel's apprentice. She sent me south with the
            children before the end, and I went." She turns a satchel over in her lap, and
            chalk dust sifts out of it. "She taught me how to hold a thing that can't be
            bound. Three nails through its shadow, at the head, the heart and the heel, while
            the light is low. It wants two pairs of hands, she said, or one pair that doesn't
            shake. She never let me drive one myself." She holds the satchel out. "There are
            nine in here, and the way of it is in my head, such as I have it. If you find her,
            will you put her somewhere decent?"

            Across the fire, Edda is watching with her arms folded. Stonebeard's door is Halvard's to open,
            not Kaelen's, and Edda knows it as well as he does. Whatever he says next, she will hear it
            too.
        """.trimIndent(),
        choices = roadOnward(
            "Take the satchel, and promise Tam you will. Tell Edda that if her people have nowhere else, you will answer to Halvard for them.",
            Consequences(setFlags = setOf("c6_nail_method", "c6_has_ward_nails", "c6_promised_tam", "c6_offered_the_hold"))
        ) + roadOnward(
            "Take the satchel, and promise Tam you will.",
            Consequences(setFlags = setOf("c6_nail_method", "c6_has_ward_nails", "c6_promised_tam"))
        ) + roadOnward(
            "Take the satchel, but promise nothing. You don't know what you'll find.",
            Consequences(setFlags = setOf("c6_nail_method", "c6_has_ward_nails"))
        )
    ),
    StoryNode(
        id = "c6_tollhouse_pressed",
        chapterId = "chapter_6",
        title = "What Edda Said",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Edda tells it standing, in the flat voice of someone who has told it often. Kestrel
            stopped coming down to market at midwinter. The ward-stones went dark one after
            another, a stone a week. A rider came before that, polite, and talked with the
            warden at her door for an hour, and Kestrel shut it on him. Then the dogs, and the
            cellar hatch in Edda's own kitchen that was warm to the hand, and the day Kestrel
            sent the children south and told the rest of them to follow, and the look back from
            the hill road at a village with every lamp gone out at once.

            "She told us to go," Edda says. "She stayed." She jerks her chin north. "If you go
            down that cellar, lift the hatch before you pull. It swelled in its seat the week
            it went warm." Across the fire the girl with chalk on her sleeves watches Kaelen
            listen, and when he has gone she goes on watching the road.
        """.trimIndent(),
        choices = roadOnward("Go on.")
    ),
    StoryNode(
        id = "c6_cairn",
        chapterId = "chapter_6",
        title = "The Wardens' Cairn",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The heap at the fork is no shepherd's cairn. Each stone was set by a warden's hand
            and scratched underneath with a sign Kaelen half remembers from Stonebeard's lower
            passage: a crossed bar for a safe road, an open ring for water, a closed one for do
            not follow. This cairn carries all three, one over another, laid down across a
            hundred years. Under the topmost slab, wrapped in oiled cloth and weighted against
            the wind, are a page and a flat iron tin, and neither is a hundred years old.

            The tin holds five black nails as long as his finger, cold and clean of rust, laid
            in a row like teeth. The page is written in a hurried, square hand. "To whoever
            keeps the next gate. The wards at Greymoor have been going dark a stone at a time
            since midwinter. A rider came before that, courteous, with an offer. I said what I
            would say to anyone, and he thanked me and went, and I have not liked the quiet
            since. I am sending the children south. I am staying. The nails are for the
            pinning. The way of it is in my ledger at the post, and it is not a thing to do
            alone, and if I am found having tried, remember that I said so. I wrote once to
            Stonebeard and had no answer, and I do not blame them. I would not have known what
            to say either. If you are reading this, I hope someone wrote to you first."
        """.trimIndent(),
        choices = roadOnward(
            "Take the page and the tin. Halvard should see this.",
            Consequences(setFlags = setOf("c6_took_cairn_note", "c6_has_ward_nails"))
        )
    ),
    StoryNode(
        id = "c6_camp",
        chapterId = "chapter_6",
        title = "The Neat Fire",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            The fire is too neat to be a shepherd's: a trench dug for it, the smoke kept low, and
            three grey-cloaked riders who have set their camp where the road crosses the ford and
            nothing can pass unseen. Each wears a small pin at the collar, a closed hand around a
            flame, the same mark that sealed the Marshal's missive. They are not hiding. They are
            waiting, and the waiting is patient in the way a net is patient.

            He could wade the river by the old stones a bowshot downstream. He could walk up to
            the fire and meet whatever they ask of him. Or he could take one of them quietly, and
            learn what the waiting is for.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Wade the river downstream, unseen.",
                nextNodeId = "c6_camp_sneaked",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("c6_camp_sneaked")
                )
            ),
            Choice(
                label = "Walk up to the fire.",
                nextNodeId = "c6_camp_paid",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to -20),
                    setFlags = setOf("c6_camp_paid")
                )
            ),
            Choice(
                label = "Take one of them quietly.",
                nextNodeId = "c6_camp_scout",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5))
            )
        )
    ),
    StoryNode(
        id = "c6_camp_sneaked",
        chapterId = "chapter_6",
        title = "Past the Ford",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            The river is cold enough to hurt and the old stones are slick, and he crosses with
            his boots in one hand and his breath held behind his teeth. Close enough to hear, he
            stops in the reeds.

            "Greymoor's marked," one rider says. "Then why are we still here?" "Because someone
            always comes to look. They send somebody to see what's become of a gate, and we
            write down the face." A third yawns. "And if it's the one with the brand?" "Then we
            write that down too and let it walk. Orders. The bearer isn't to be spent."

            He is across and well into the dark before he lets himself breathe.
        """.trimIndent(),
        choices = roadOnward("Go on.")
    ),
    StoryNode(
        id = "c6_camp_paid",
        chapterId = "chapter_6",
        title = "The Price of the Ford",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            He walks to the fire with his hands open, and nobody stands. The rider nearest the
            trench holds out a hand, palm up, and names a price for the ford, twenty coins, in
            the voice of a man reading it off a card. Kaelen counts twenty into his palm, or as much of twenty as he has, and the rider does not seem to mind which. The coins vanish into a ledger that is not a toll-keeper's, and a second hand, quick
            and neat, writes something beside them that he cannot read upside down.

            "Safe road," the rider says, and seems to mean it. His eyes go to the brand on
            Kaelen's palm and stay there a beat too long.
        """.trimIndent(),
        choices = roadOnward("Go on.")
    ),
    StoryNode(
        id = "c6_camp_scout",
        chapterId = "chapter_6",
        title = "What the Waiting Is For",
        illustrationId = "cinder_envoy",
        narrativeText = """
            He waits until the youngest rider goes down to the river with a bucket, and takes him
            from behind in the shallows, one arm across the throat and the other turning the sword
            hand out and up. It is over before it is loud. When Kaelen lets him sit, the boy, and
            he is a boy, coughs river water and does not reach for the knife he could still reach.

            "We never touched it," he says, before he is asked. "The gate. We got here after. The
            warden was already at her post and the thing was already out, and we stayed on the
            ridge because nobody told us to go in." He looks at the brand, and then quickly away.
            "The captain says someone always comes to look. We write down who. That's all the
            waiting is." Kaelen asks who came before them. "A rider, in the cold of the year. He
            asked her first. That's how it's done: ask, and then wait to see whether asking was
            enough." The boy swallows. "I don't know who broke the stone. We don't carry chisels."
        """.trimIndent(),
        choices = roadOnward(
            "Let him go. He has told you what he knows.",
            Consequences(
                statDeltas = mapOf(StatType.HONOUR to 1),
                setFlags = setOf("c6_scout_taken", "c6_scout_freed")
            )
        ) + roadOnward(
            "Bind him to a tree where his own people will find him by morning.",
            Consequences(setFlags = setOf("c6_scout_taken", "c6_scout_bound"))
        )
    ),
    StoryNode(
        id = "c6_arrive_greymoor",
        chapterId = "chapter_6",
        title = "Greymoor",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Three days north, the road ends at a village that shouldn't be as quiet as it is.
            Greymoor. Shuttered doors, cold chimneys, and not one living soul on the street to ask
            why he came. The miller's house is the largest on the street, and its back door stands
            open on the yard behind.
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

            A single figure lies at the threshold, not fled, not hiding. A warden, still in her
            chain, exactly where she chose to make her stand, and the stillness of her tells
            Kaelen everything the empty street didn't.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Kneel beside her.",
                nextNodeId = "what_the_warden_left_knelt",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C6_GATE_APPROACH_FAMILY,
                    setFlags = setOf("knelt_for_the_warden")
                )
            ),
            Choice(
                label = "Search the gate for answers first.",
                nextNodeId = "what_the_warden_left_searched",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C6_GATE_APPROACH_FAMILY,
                    setFlags = setOf("searched_the_gate_first")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_warden_left_knelt",
        chapterId = "chapter_6",
        title = "What the Warden Left",
        illustrationId = "kestrels_locket_found",
        narrativeText = """
            Kaelen kneels before he does anything else, the way he'd want someone to kneel for
            him if it ever came to that. Her name was carved into the chain itself,
            dwarven-fashion: Kestrel, Warden of Greymoor. Whatever came through the gate didn't
            kill her cleanly; there are wounds here that don't match any blade Kaelen knows, and
            no body left of whatever she was fighting when she fell.

            Clutched in her hand, close enough that it took the last of her strength to keep it
            there: a locket, warded in the same hand as the gate itself, and a scrap of writing
            that isn't a warning. It's an apology, to whoever came looking, and to whoever she
            couldn't hold the line for. He reads it twice before he lets himself stand back up.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the locket. Someone should carry what she couldn't finish saying.",
                nextNodeId = "c6_ruin_hub_one",
                consequences = Consequences(
                    grantItemIds = listOf("kestrels_locket"),
                    setFlags = setOf("took_kestrels_locket")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_warden_left_searched",
        chapterId = "chapter_6",
        title = "What the Warden Left",
        illustrationId = "kestrels_locket_found",
        narrativeText = """
            Kaelen goes to the gate first, running his hands along the cracked stone for anything
            that might explain how six centuries of warding failed in what looks like a single
            night. He finds scorch marks that aren't from fire, and wards gouged out rather than
            simply faded, like something clawed its way past them instead of waiting them out.

            Only once he's satisfied there's nothing more the stone will tell him does he turn to
            Kestrel herself. Her name was carved into her chain, dwarven-fashion: Warden of
            Greymoor. Clutched in her hand, close enough that it took the last of her strength to
            keep it there, is a locket warded in the same hand as the gate, and a scrap of
            writing that isn't a warning so much as an apology, to whoever came looking too late
            to help.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the locket. Someone should carry what she couldn't finish saying.",
                nextNodeId = "c6_ruin_hub_one",
                consequences = Consequences(
                    grantItemIds = listOf("kestrels_locket"),
                    setFlags = setOf("took_kestrels_locket")
                )
            )
        )
    ),
    StoryNode(
        id = "c6_ruin_hub_one",
        chapterId = "chapter_6",
        title = "The Yard Behind the Mill",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The yard behind the miller's house is larger than the empty village makes it feel:
            cracked flagstones, a well with its rope cut, and at the far wall the gate itself,
            black stone split from crown to sill. Something is moving in the lane beyond the
            wall, slowly, and stopping, and moving again. It has not come over. The light has
            perhaps an hour of use left in it, and the yard holds three places that might tell
            him something before the dark does.

            A low stone room with a shuttered window leans against the east wall: the warden's
            post, its door hanging half open. In the miller's kitchen a plank hatch stands in the
            floor, and the stone around it is warm. And along the north wall, under the one tree
            that did not die, a row of small cairns runs toward the corner, each with a name
            scratched on it, and stops one short.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The warden's post.",
                nextNodeId = "c6_watchpost",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_watchpost")),
                consequences = Consequences(setFlags = setOf("c6_spot_watchpost"))
            ),
            Choice(
                label = "The warm hatch in the kitchen.",
                nextNodeId = "c6_cellar",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_cellar")),
                consequences = Consequences(setFlags = setOf("c6_spot_cellar"))
            ),
            Choice(
                label = "The row of cairns.",
                nextNodeId = "c6_garden",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_garden")),
                consequences = Consequences(setFlags = setOf("c6_spot_garden"))
            )
        )
    ),
    StoryNode(
        id = "c6_ruin_hub_two",
        chapterId = "chapter_6",
        title = "The Yard Behind the Mill",
        illustrationId = "greymoor_ward",
        narrativeText = """
            One place seen, and the light going copper along the wall. In the lane, whatever was
            moving has gone quiet, which he likes less than the moving. There is time for one
            more.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "The warden's post.",
                nextNodeId = "c6_watchpost",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_watchpost")),
                consequences = Consequences(setFlags = setOf("c6_spot_watchpost", "c6_ruin_second"))
            ),
            Choice(
                label = "The warm hatch in the kitchen.",
                nextNodeId = "c6_cellar",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_cellar")),
                consequences = Consequences(setFlags = setOf("c6_spot_cellar", "c6_ruin_second"))
            ),
            Choice(
                label = "The row of cairns.",
                nextNodeId = "c6_garden",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_spot_garden")),
                consequences = Consequences(setFlags = setOf("c6_spot_garden", "c6_ruin_second"))
            )
        )
    ),
    StoryNode(
        id = "c6_watchpost",
        chapterId = "chapter_6",
        title = "The Warden's Post",
        illustrationId = "warden_chamber",
        narrativeText = """
            A warden's post is built where a warden can see the thing she keeps: one table, one
            cot, a stove gone cold, and a shuttered window that looks straight at the gate.
            Everything is where she left it. Her cup holds a finger of something that stopped
            being tea long ago. Her spare chain hangs on a nail. On the table, under a lamp
            whose wick has burned to a black thread, lies a ledger bound in plain boards, open
            flat, with a pen laid across the gutter as though set down for a moment.

            It is a warden's private book, and its warden is past asking.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Read her last pages.",
                nextNodeId = "c6_ledger_read",
                consequences = Consequences(
                    setFlags = setOf("c6_read_ledger", "c6_nail_method"),
                    unlockTrophy = "The Ledger, Read to the End"
                )
            )
        ) + ruinOnward("Leave her book unread. It was not written for you.")
    ),
    StoryNode(
        id = "c6_ledger_read",
        chapterId = "chapter_6",
        title = "What the Ledger Says",
        illustrationId = "warden_chamber",
        narrativeText = """
            Most of it is the work of a warden's days, written small: ward-stones counted, dogs
            fed, a debt of flour with the miller that runs eleven pages and ends, to Kaelen's
            surprise, in a joke. Then the hand changes. The entries get shorter and the lines
            straighter, like someone walking a plank.

            Midwinter: "Stone four went dark. A rider came to the door, courteous, and sat in my
            own chair and told me the gate was too heavy for one pair of hands, and that there
            were people who would take the weight. I asked who the weight went to. He thanked me
            for my time." A little further on: "Nothing from Stonebeard. I will not write twice."

            Later comes a drawing, a standing figure with a long shadow and three nails driven
            through the shadow, at the head, the heart and the heel. Beneath it: "It will hold
            anything that casts one. It wants the light low, and two pairs of hands, or one pair
            that does not shake. The first nail and the last must go in on a single breath. Tam
            has the hands. Tam is fifteen, and I will not teach her this tonight." And under
            that: "Sent Tam south with the children and told her it was for the children. That
            was half true."

            The last entry is dated this month. "The last carts are over the hill. The cellar
            hatch is warm again. I have the nails and I have the light, nearly. If I take the
            head first and the heel after, and trust that nobody"

            The line stops there, in the middle, a long stroke running off the page where the
            pen was pulled away. Kaelen does not take the book. It is hers, and this is her post,
            and whoever comes to learn what happened here ought to find it open to the place she
            stopped. He lays the pen back across the gutter exactly as it was.
        """.trimIndent(),
        choices = ruinOnward("Leave the book where it lies.")
    ),
    StoryNode(
        id = "c6_cellar",
        chapterId = "chapter_6",
        title = "The Warm Hatch",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The hatch is a plank door in the kitchen floor, and it is warm to the hand the way a
            loaf is warm. The wood has swollen in its seat with the heat, and the iron ring has
            seized. Below it, stairs run down into a dark with a faint sound in it, like a long
            breath held in a stone throat. The ward-stone's foot runs under the kitchen wall, and
            this is the only place where anyone can see where it broke.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Lift the hatch before you pull, the way Edda said, and go down.",
                nextNodeId = "c6_breach_found",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_pressed_edda")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to 25),
                    grantItemIds = listOf("healing_draught"),
                    setFlags = setOf("c6_inspected_breach")
                )
            ),
            Choice(
                label = "Force the hatch, whatever it costs your hands, and go down.",
                nextNodeId = "c6_breach_found",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_pressed_edda")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -6, StatType.GOLD to 25),
                    grantItemIds = listOf("healing_draught"),
                    setFlags = setOf("c6_inspected_breach")
                )
            )
        ) + ruinOnward("Leave the hatch shut.")
    ),
    StoryNode(
        id = "c6_breach_found",
        chapterId = "chapter_6",
        title = "Where It Came Through",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The ward-stone's foot stands in the cellar like a tooth in a jaw, black and split,
            and the split runs the way he feared: outward from inside, the edges bright where
            they burst and the dark behind them still faintly warm. Whatever came through did not
            wait for the wards to fail. It pushed. Beneath the break the floor is a hollow,
            scraped smooth, the size of something that has lain curled for a long time.

            On the stone's outer face, low down where the seam begins, there is a small square
            notch the width of a chisel's bite, clean-edged, with none of the weathering the rest
            of the stone has taken in a hundred years. Someone set a tool to a seam that was
            already giving. Or the notch is older than any of it and means nothing. Kaelen turns
            it over until the cold gets into his hands, and cannot say which.

            On a shelf above the hollow, where a warden stocks the foot of her gate against a bad
            night, a purse and a stoppered flask have been left untouched.
        """.trimIndent(),
        choices = ruinOnward("Climb out, and shut the hatch on it.")
    ),
    StoryNode(
        id = "c6_garden",
        chapterId = "chapter_6",
        title = "The Wardens' Row",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The cairns are low and old, a name scratched into each: Greymoor's wardens, back
            further than the village remembers, one beside the next. Moss has taken most of the
            names. The last space in the row is empty and the ground there is already soft, as if
            the whole yard has been keeping it.

            Kestrel lies where she fell, forty paces off at the gate, in plain sight of the lane.
            To bury her means carrying her past the wall, digging in the last of the light, and
            setting stones while something in the dark listens.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Carry her to the row and bury her properly. You promised Tam.",
                nextNodeId = "c6_buried_promised",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_promised_tam")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.HEALTH to -6),
                    setFlags = setOf("c6_buried_kestrel"),
                    unlockTrophy = "Buried Properly"
                )
            ),
            Choice(
                label = "Carry her to the row and bury her properly.",
                nextNodeId = "c6_buried",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_promised_tam")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.HEALTH to -6),
                    setFlags = setOf("c6_buried_kestrel"),
                    unlockTrophy = "Buried Properly"
                )
            )
        ) + ruinOnward("Leave her at the gate. She chose that ground.")
    ),
    StoryNode(
        id = "c6_buried",
        chapterId = "chapter_6",
        title = "A Place in the Row",
        illustrationId = "greymoor_ward",
        narrativeText = """
            He carries her himself, because there is no one else and because it seems wrong to
            ask the dark to help. She weighs less than her chain. The ground at the end of the row
            is loose with old digging and takes the spade he finds in the kitchen garden as if it
            remembered the work. He lays her down with her hands crossed over the place the
            locket was clutched, and sets the cairn stones one at a time.

            Beyond the wall, in the lane, something stops moving to listen to stone on stone. It
            does not come. He scratches the name with the flat of a flint, Kestrel, Warden of
            Greymoor, the way her chain has it. The locket stays with him. Of everything in the
            yard, it is the one thing she looked to have meant to be carried.
        """.trimIndent(),
        choices = ruinOnward("Go back to the yard.")
    ),
    StoryNode(
        id = "c6_buried_promised",
        chapterId = "chapter_6",
        title = "Somewhere Decent",
        illustrationId = "greymoor_ward",
        narrativeText = """
            He carries her himself, because there is no one else, and Tam's voice goes with him
            the whole forty paces: somewhere decent. She weighs less than her chain. The ground at
            the end of the row is loose with old digging and takes the spade he finds in the
            kitchen garden as if it remembered the work. He lays her down with her hands crossed
            over the place the locket was clutched, and sets the stones one at a time, and scratches
            the name with the flat of a flint, Kestrel, Warden of Greymoor, the way her chain has
            it. Beneath it, because there is room and because he promised someone, he scratches one
            more word. Stayed.

            Beyond the wall, in the lane, something stops moving to listen to stone on stone. It
            does not come. The locket stays with him. Of everything in the yard, it is the one
            thing she looked to have meant to be carried.
        """.trimIndent(),
        choices = ruinOnward("Go back to the yard.")
    ),
    StoryNode(
        id = "c6_unbound_sighted",
        chapterId = "chapter_6",
        title = "Low Light",
        illustrationId = "greymoor_ward",
        narrativeText = """
            The sun is behind the hills and the yard is full of a copper dusk that makes every
            shadow longer than the thing that casts it. In the lane, the dragging sound has
            changed. It is no longer moving and stopping. It is coming, slow and sure, toward the
            gap where the cracked gate meets the wall, the way an animal comes home.

            Kaelen knows what Kestrel's drawing meant by low light. This is it. Whatever he means
            to do about what is coming, he has about as long as it takes to cross a yard.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Meet it in the yard.", nextNodeId = "the_unbound"),
            Choice(
                label = "Take out the nails, and pin it the way Kestrel meant to.",
                nextNodeId = "c6_pin_held",
                requirements = ChoiceRequirement(
                    requiredFlags = setOf("c6_nail_method", "c6_has_ward_nails"),
                    minStats = mapOf(StatType.COURAGE to 5)
                ),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1, StatType.XP to 120),
                    grantItemIds = listOf("ward_nails"),
                    setFlags = setOf("c6_unbound_pinned"),
                    unlockTrophy = "Nails in the Dark"
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
            This isn't patient, and it isn't waiting for permission anymore. It's loose, and
            hungry, and it has apparently decided Kaelen looks like more of the same meal it
            already started.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "unbound_encounter"
    ),
    StoryNode(
        id = "unbound_defeat",
        chapterId = "chapter_6",
        title = "Outlasted, Not Outfought",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            It's faster than him, and hungrier, and by the time Kaelen realizes he isn't winning
            this one outright, the only choice left is whether to keep his feet under him or not.
            He chooses feet, barely, dragging himself back from its reach again and again until
            it's the creature that tires first, not the man too stubborn to stop moving.

            That isn't the same as beating it. It slinks back into the wreck of Greymoor's gate
            rather than press an advantage it still technically holds, and Kaelen isn't foolish
            enough to call that a victory, whatever it looks like from the outside.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take stock of what's left.",
                nextNodeId = "c6_defeat_tam",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_met_tam")),
                consequences = Consequences(setFlags = setOf("lost_to_unbound_horror"))
            ),
            Choice(
                label = "Take stock of what's left.",
                nextNodeId = "c6_defeat_alone",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_met_tam")),
                consequences = Consequences(setFlags = setOf("lost_to_unbound_horror"))
            )
        )
    ),
    StoryNode(
        id = "c6_defeat_tam",
        chapterId = "chapter_6",
        title = "Chalk on the Sleeve",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            He comes back to himself on the floor of the warden's post with his back against the
            cot and the door pulled shut and wedged with the warden's chair, which he did not do. Someone
            is pressing a wet rag to his scalp with more force than skill. There is chalk on the
            sleeve.

            "You said Greymoor," Tam says, not looking at him. "I have been a morning behind you since the tollhouse,
            and I reached the village as the light went. I heard it from the street. I dragged you in here
            and wedged the door. You are very easy to follow, you know. You look back at everything except
            the road." Her
            hands are not steady. "She sent me south and I let her. I'm not doing that twice."

            She tells him what she can see through the shutter: the cracked gate, the empty
            yard, the thing gone to ground in the broken stone and not dead. She has a flask in
            her satchel and a clean shirt, and she uses both. At the table she stops with her
            palm over the open ledger and reads only the last line, and Kaelen watches her decide
            not to move it. "She'd want the next person to find it like this," she says, in a
            voice that is not quite hers. When she sees the locket in his pocket she does not ask
            for it. "Keep it. She never put a thing down once she'd decided to carry it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Stand over what is left.",
                nextNodeId = "what_must_be_decided",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to 20),
                    setFlags = setOf("c6_tam_followed")
                )
            )
        )
    ),
    StoryNode(
        id = "c6_defeat_alone",
        chapterId = "chapter_6",
        title = "The Bolted Door",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            He comes back to himself on the floor of the warden's post with his back against the
            cot and the door bolted from the inside, which he does not remember doing. His head
            hurts in a way that will keep. In the yard the lane has gone quiet; the thing has
            gone to ground in the broken stone, not dead, not hunting, spent in its own fashion
            the way he is.

            Nobody comes. He takes what the post offers: a spare shirt from the warden's chest,
            water from the pail by the stove, a long minute in the lamplight beside a book left
            open on the table that he leaves alone. What it is to be the only one left standing
            in a place is not something he needed to learn again, and he learns it again. When
            the shaking stops he gets up, because there is nobody else to do it for him.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Stand over what is left.",
                nextNodeId = "what_must_be_decided",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to 8))
            )
        )
    ),
    StoryNode(
        id = "c6_pin_held",
        chapterId = "chapter_6",
        title = "Nails in the Dark",
        illustrationId = "the_unbound_creature",
        narrativeText = """
            The nails are cold in his fist and heavier than iron that size should be. He sets his
            feet where the flagstones are flat. The creature comes through the gap at the pace of
            something that has stopped expecting to be stopped, and in the copper light it throws
            a shadow across the yard that is longer than it is, and thick, and wavering on the
            stones like something poured.

            Head first: the end farthest from the light. Heart: the thickest part. Heel: where it
            touches the ground. The method wants two pairs of hands. He has one, and he makes it
            be still.

            The first nail goes through the far end of the shadow and the creature stops as
            though a hand has closed on its collar. The second goes in at the thickest part, and
            the third, a breath behind the first, at the heel, and the three of them ring on the
            stone with a single note, like a bell struck underwater. For a moment nothing at all
            happens.

            Then the shadow thins the way a held breath thins, and the creature is left standing
            in the middle of the yard, fastened to the ground by something it cannot see. It
            makes a sound that is no roar. It is closer to the noise a ward-stone must make the
            first time it goes dark. It does not fight. It cannot. Slowly, and then all at once,
            it is drawn down into the cracked threshold like water into sand, and the nails stand
            in a row along the crack, holding it shut.

            He pulls none of the three. The nails he did not need go back into his pocket. The gate itself does
            not answer; its ward went dark before the creature ever reached the yard, and it stays dark, and the
            nails keep only what came through. Behind him the yard
            is entirely silent, and the silence is not Kestrel's, quite, but it is the nearest to
            hers he has stood in.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Look at what is left to decide.", nextNodeId = "c6_pin_decided")
        )
    ),
    StoryNode(
        id = "c6_pin_decided",
        chapterId = "chapter_6",
        title = "What the Nails Left",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            The nails hold the crack the way a splint holds a bone: not mended, only kept from
            getting worse. The gate is no more whole than it was an hour ago. It is quieter, and
            quiet is all anyone can honestly say for it. Kestrel's locket is a small, warm weight
            in his pocket, heavier than its size should allow.

            Greymoor's people will come back, or they won't. A gate with three nails through its
            crack can wait for hands to warden it, for a season, perhaps a year if the iron is
            as good as she believed. What it is waiting for is the question.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"The nails did the hard part. I'll finish the seal myself.\"",
                nextNodeId = "chapter6_end_sealed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("sealed_greymoor_personally")
                )
            ),
            Choice(
                label = "\"This isn't mine to hold. I'll bring word to people who can actually rebuild it.\"",
                nextNodeId = "c6_cost_reported",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("reported_greymoor_honestly")
                )
            ),
            Choice(
                label = "Leave it as it is, and carry what you learned back to Halvard.",
                nextNodeId = "chapter6_end_left",
                consequences = Consequences(
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("left_greymoor_as_is")
                )
            )
        )
    ),
    StoryNode(
        id = "what_must_be_decided",
        chapterId = "chapter_6",
        title = "What Must Be Decided",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            It goes still eventually, more from exhaustion on both sides than any clean killing
            blow. Kaelen stands over it in the ruin of a gate that failed years before his own
            nearly did, Kestrel's locket a small, warm weight in his pocket that feels heavier
            than its size should allow.

            Greymoor's people will come back, or they won't. The gate behind him won't hold
            itself shut a second time, not without hands to warden it, and there are none left in
            this village willing to try, or perhaps none left at all.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll seal what's left myself, however long it holds.\"",
                nextNodeId = "c6_cost_sealed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("sealed_greymoor_personally")
                )
            ),
            Choice(
                label = "\"This isn't mine to hold. I'll bring word to people who can actually rebuild it.\"",
                nextNodeId = "c6_cost_reported",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("reported_greymoor_honestly")
                )
            ),
            Choice(
                label = "Leave it as it is, and carry what you learned back to Halvard.",
                nextNodeId = "chapter6_end_left",
                consequences = Consequences(
                    clearFlags = C6_VERDICT_FAMILY,
                    setFlags = setOf("left_greymoor_as_is")
                )
            )
        )
    ),
    StoryNode(
        id = "c6_cost_sealed",
        chapterId = "chapter_6",
        title = "A Night's Work",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            A seal is not a spell, whatever the stories say. It is a night of hands. He works
            from the shape of a ward as he half remembers Halvard drawing it, and from what the
            broken stone is willing to show him, binding the crack with cord and ash and pieces
            of the village's ordinary lives: a door-latch, a flour sieve, a scatter of the
            miller's nails. Twice the dark behind the stone leans on his work, and twice it
            holds, just. Near dawn the crack is closed with something that looks more like a scar
            than a ward.

            His hands are raw to the wrist. He has spent more than he had, and the cold of it
            will follow him south.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Gather yourself, and go.",
                nextNodeId = "chapter6_end_sealed",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -10))
            )
        )
    ),
    StoryNode(
        id = "c6_cost_reported",
        chapterId = "chapter_6",
        title = "The Plain Account",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            He does not try to hold the gate. He writes it down instead, the way she would have:
            what he saw, what is left, what is needed, in plain words and in order, on pages from
            his own pack, because the ledger is hers and stays hers. One copy goes up on the
            miller's back door, where Greymoor's people will find it if they come home. The
            other he means to carry south.

            In the morning a drover comes up the lane with a dog and a cart, wary as a deer,
            and agrees to take a third copy to the nearest market town for a price he names
            without meeting Kaelen's eye. Kaelen pays it, or what his purse can find toward it. It is not much. It is more than he would like to have to spend.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Start the long road south.",
                nextNodeId = "chapter6_end_reported",
                consequences = Consequences(statDeltas = mapOf(StatType.GOLD to -10))
            )
        )
    ),
    StoryNode(
        id = "chapter6_end_sealed",
        chapterId = "chapter_6",
        title = "End of Chapter VI — The Weight of Others' Doors",
        illustrationId = "chapter6_threshold",
        narrativeText = """
            The road south feels longer than the road north did, though the distance hasn't
            changed. What Kaelen leaves behind at Greymoor isn't a proper ward, only a stopgap
            stitched together from what little he understands of Halvard's craft, but it's more
            than the village had an hour before he arrived.

            Halvard listens to all of it without interrupting, and when Kaelen describes the seal
            he improvised, the old warden's face does something complicated: pride and worry
            occupying the same space at once. "You can't hold two gates at a distance from each
            other forever," he says, not unkindly. "Four gates left unaccounted for, and
            Ilsevet's people already know where at least one of them is. I need you whole, not
            spread thin across half a continent." He doesn't say what happens if Kaelen ignores
            that. He doesn't have to.

            Chapter VII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, and plan for what's coming.", nextNodeId = "c7_forge_night")
        )
    ),
    StoryNode(
        id = "chapter6_end_reported",
        chapterId = "chapter_6",
        title = "End of Chapter VI — The Weight of Others' Doors",
        illustrationId = "chapter6_threshold",
        narrativeText = """
            The road south feels longer than the road north did, though the distance hasn't
            changed. Kaelen carries Kestrel's locket the whole way, and the decision he made at
            her gate: that Greymoor deserves better hands than a stranger passing through once and
            never coming back.

            Halvard listens to all of it without interrupting, and nods slowly when Kaelen
            explains why he didn't try to seal it himself. "That's the harder answer, most days,"
            the old warden says. "Wanting to fix everything you touch is easy. Knowing what you
            can't fix alone is not." When Kaelen finishes, Halvard is quiet a moment longer. "Four
            gates left unaccounted for. And Ilsevet's people already know where at least one of
            them is." He doesn't say what happens if the Marshal's people reach the others first.
            He doesn't have to.

            Chapter VII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, and plan for what's coming.", nextNodeId = "c7_forge_night")
        )
    ),
    StoryNode(
        id = "chapter6_end_left",
        chapterId = "chapter_6",
        title = "End of Chapter VI — The Weight of Others' Doors",
        illustrationId = "chapter6_threshold",
        narrativeText = """
            The road south feels longer than the road north did, though the distance hasn't
            changed. Kaelen carries Kestrel's locket the whole way, and finds he can't decide
            whether that's grief or evidence, or just the only thing he had it in him to carry
            out of Greymoor besides the memory of it.

            Halvard listens to all of it without interrupting, which is its own kind of answer.
            When Kaelen finishes, the old warden is quiet for a long moment, and something in his
            face suggests he understands leaving a broken gate behind better than either of them
            would like. "Four gates left unaccounted for," he says at last. "And Ilsevet's people
            already know where at least one of them is." He doesn't say what happens if the
            Marshal's people reach the others before anyone else does. He doesn't have to.

            Chapter VII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, and plan for what's coming.", nextNodeId = "c7_forge_night")
        )
    )
)

val chapter6 = com.thelastjailer.app.Chapter(
    id = "chapter_6",
    number = 6,
    title = "Chapter VI — The Weight of Others' Doors",
    startNodeId = "the_choice_of_roads"
)
