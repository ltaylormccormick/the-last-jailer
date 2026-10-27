package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/** Family C10a: what Kaelen put on the page the smiths asked for. Alternatives; each clears the others. */
val C10_RITES_FAMILY: Set<String> = setOf("c10_rites_name", "c10_rites_deeds", "c10_rites_plain")

/** Family C10b: where Kaelen stood when the column came. Alternatives; each clears the others. */
val C10_COVER_FAMILY: Set<String> = setOf("c10_cover_voss", "c10_cover_stair", "c10_cover_forge")

/** The hub markers for the night before: the four every hub shares, plus one for a fourth place and one for a third visit. */
val HUB_MARKERS_FOUR: Set<String> = HUB_MARKERS + setOf("hub_d", "hub_third")

/**
 * Everything Chapter X records except the death itself: the two shipped preparation flags, the three shipped answers, the
 * defeat marker, the page, the place Kaelen stood and the hub markers. The chapter's first choices clear it all, so a replay
 * reaches the night before with an empty slate. `halvard_died` and `took_the_warden_chain` are deliberately not here: nothing
 * in Chapters X to XII reads them, and no scene of the expansion sets or clears them.
 */
val C10_TELLING_FLAGS: Set<String> = setOf(
    "fortified_for_the_last_stand", "prepared_to_meet_them_hard", "defied_ilsevet", "refused_on_principle",
    "answered_with_steel", "lost_to_ilsevets_vanguard_captain"
) + C10_RITES_FAMILY + C10_COVER_FAMILY + HUB_MARKERS_FOUR

/** One way into a place from a hub scene: which flags choose the variant, and where it leads. */
private class Way(val target: String, val required: Set<String> = emptySet(), val forbidden: Set<String> = emptySet())

private val forgeWays = listOf(
    Way("c10_forge_open", required = setOf("c7_traded")),
    Way("c10_forge_open", required = setOf("c7_page")),
    Way("c10_forge_kind", required = setOf("c7_comfort")),
    Way("c10_forge_kind", required = setOf("c7_quiet")),
    Way("c10_forge_shut", forbidden = setOf("c7_traded", "c7_page", "c7_comfort", "c7_quiet"))
)

private val wallWays = listOf(
    Way("c10_wall_trusted", required = setOf("c7_rung_trusted")),
    Way("c10_wall_working", required = setOf("c7_rung_working")),
    Way("c10_wall_wary", forbidden = setOf("c7_rung_working", "c7_rung_trusted"))
)

/** Edda's people are at Stonebeard only where Chapter VII recorded what became of the promise made for them. */
private val peopleWays = listOf(
    Way("c10_people_inside", required = setOf("c7_hold_terms")),
    Way("c10_people_outside", required = setOf("c7_hold_refused")),
    Way("c10_people_unspent", required = setOf("c7_hold_hidden"))
)

private val pageWays = listOf(Way("c10_page"))

/** The choices into one place from a hub scene; [extra] is what the hub scene adds (the second or third visit). */
private fun entries(label: String, marker: String, ways: List<Way>, extra: Set<String>): List<Choice> =
    ways.map { w ->
        Choice(
            label = label,
            nextNodeId = w.target,
            requirements = ChoiceRequirement(requiredFlags = w.required, forbiddenFlags = w.forbidden + marker),
            consequences = Consequences(setFlags = setOf(marker) + extra)
        )
    }

/** The four places of the night before. A place nobody can reach in this telling (the people, with no promise) is simply absent. */
private fun eveningHub(extra: Set<String>): List<Choice> =
    entries("Go down to the forge, where Halvard is working.", "hub_a", forgeWays, extra) +
        entries("Walk the wall with Voss.", "hub_b", wallWays, extra) +
        entries("Look in on Edda's people.", "hub_c", peopleWays, extra) +
        entries("Go to the smiths, who have asked for a word.", "hub_d", pageWays, extra)

/**
 * One continue choice for a place on the night before: back to the hub with one place seen, to the hub with two seen (where
 * Kaelen may leave), or out of the hub once a third is seen. Exactly one of the three is ever visible. The last clears the markers.
 */
private fun eveningOnward(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c10_evening_two",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_second")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c10_evening_three",
        requirements = ChoiceRequirement(requiredFlags = setOf("hub_second"), forbiddenFlags = setOf("hub_third")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c10_before_light",
        requirements = ChoiceRequirement(requiredFlags = setOf("hub_third")),
        consequences = consequences.copy(clearFlags = consequences.clearFlags + HUB_MARKERS_FOUR)
    )
)

/** The same trio for a place whose "go on" also moves Voss's rung by [delta] (clamped); nine written, one ever visible. */
private fun eveningOnwardRung(label: String, delta: Int, base: Consequences = Consequences()): List<Choice> =
    rungOrder.indices.flatMap { i ->
        val target = rungOrder[(i + delta).coerceIn(0, rungOrder.size - 1)]
        val rungRequired = if (i == 0) emptySet() else setOf(rungOrder[i])
        val rungForbidden = if (i == 0) setOf("c7_rung_working", "c7_rung_trusted") else emptySet()
        val moved = base.copy(clearFlags = base.clearFlags + C7_RUNG_FAMILY, setFlags = base.setFlags + target)
        listOf(
            Choice(
                label = label,
                nextNodeId = "c10_evening_two",
                requirements = ChoiceRequirement(rungRequired, rungForbidden + "hub_second"),
                consequences = moved
            ),
            Choice(
                label = label,
                nextNodeId = "c10_evening_three",
                requirements = ChoiceRequirement(rungRequired + "hub_second", rungForbidden + "hub_third"),
                consequences = moved
            ),
            Choice(
                label = label,
                nextNodeId = "c10_before_light",
                requirements = ChoiceRequirement(rungRequired + "hub_third", rungForbidden),
                consequences = moved.copy(clearFlags = moved.clearFlags + HUB_MARKERS_FOUR)
            )
        )
    }

private fun forgeOuts(): List<Choice> = listOf(Choice(label = "Sit a while longer.", nextNodeId = "c10_forge_talk")) +
    eveningOnward("Leave him to his iron.")

/** What Kaelen may say to Halvard at the forge, once, whichever conversation came first. */
private fun forgeTalkOuts(): List<Choice> = listOf(
    Choice(label = "Tell him you will hold the north gap when the column comes.", nextNodeId = "c10_forge_gap"),
    Choice(
        label = "Ask what he makes of Voss.",
        nextNodeId = "c10_forge_voss_trusts",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_halvard_trusts_voss"))
    ),
    Choice(
        label = "Ask what he makes of Voss.",
        nextNodeId = "c10_forge_voss_doubts",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_halvard_trusts_voss"))
    ),
    Choice(
        label = "Mention the north stair, and whose side you took.",
        nextNodeId = "c10_forge_side",
        requirements = ChoiceRequirement(requiredFlags = setOf("c9_stair_rules"))
    )
) + eveningOnward("Leave him to his iron.")

private fun wallOuts(): List<Choice> = listOf(
    Choice(label = "Stay on the wall with her until the watch changes.", nextNodeId = "c10_wall_stay"),
    Choice(
        label = "Ask how the old wound is.",
        nextNodeId = "c10_wall_hurt",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_voss_hurt_deeper"))
    )
) + eveningOnward("Leave her to the wall.")

/** The carter was met on the north stair, which only a promise Halvard agreed to opens: she is found only where Edda's people are inside. */
private fun peopleOuts(carter: Boolean): List<Choice> = listOf(
    Choice(
        label = "Find Tam.",
        nextNodeId = "c10_tam",
        requirements = ChoiceRequirement(requiredFlags = setOf("c6_met_tam"))
    )
) + (if (carter) listOf(
    Choice(
        label = "Speak to the carter.",
        nextNodeId = "c10_carter",
        requirements = ChoiceRequirement(requiredFlags = setOf("c9_stair_people"))
    )
) else emptyList()) + eveningOnward("Leave them to the night.")

/** Where the Hold's own count leads from a scene: by what Chapter VII recorded of the promise, or straight on when there is none. */
private fun holdChoices(label: String, forbidden: Set<String> = emptySet()): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c10_line_hold_inside",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_terms"), forbiddenFlags = forbidden)
    ),
    Choice(
        label = label,
        nextNodeId = "c10_line_hold_outside",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_refused"), forbiddenFlags = forbidden)
    ),
    Choice(
        label = label,
        nextNodeId = "c10_line_hold_unspent",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_hidden"), forbiddenFlags = forbidden)
    ),
    Choice(
        label = label,
        nextNodeId = "c10_line_cover",
        requirements = ChoiceRequirement(forbiddenFlags = forbidden + setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden"))
    )
)

/** From the night before to the Line: by where the ambush was fought, or on to the Hold's count when no ground was recorded. */
private fun toTheLine(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c10_line_ground_yard", requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_yard"))),
    Choice(label = label, nextNodeId = "c10_line_ground_stair", requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_stair"))),
    Choice(label = label, nextNodeId = "c10_line_ground_road", requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_road")))
) + holdChoices(label, forbidden = C7_GROUND_FAMILY)

/** One answer to "where to stand", written twice: to the arrival the preparation chose. A save with neither flag meets the column hard. */
private fun coverChoice(label: String, flag: String, stat: StatType): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "ilsevet_arrives_fortified",
        requirements = ChoiceRequirement(requiredFlags = setOf("fortified_for_the_last_stand")),
        consequences = Consequences(statDeltas = mapOf(stat to 1), clearFlags = C10_COVER_FAMILY, setFlags = setOf(flag))
    ),
    Choice(
        label = label,
        nextNodeId = "ilsevet_arrives_hard",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("fortified_for_the_last_stand")),
        consequences = Consequences(statDeltas = mapOf(stat to 1), clearFlags = C10_COVER_FAMILY, setFlags = setOf(flag))
    )
)

/** Who carried him: by where Kaelen stood. The stair has two readings, with and without Edda's people. A save with no answer gets Voss. */
private fun carryChoices(label: String): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c10_carry_stair_edda",
        requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_stair", "c7_hold_terms"))
    ),
    Choice(
        label = label,
        nextNodeId = "c10_carry_stair_edda",
        requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_stair", "c7_hold_refused"))
    ),
    Choice(
        label = label,
        nextNodeId = "c10_carry_stair_edda",
        requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_stair", "c7_hold_hidden"))
    ),
    Choice(
        label = label,
        nextNodeId = "c10_carry_stair",
        requirements = ChoiceRequirement(
            requiredFlags = setOf("c10_cover_stair"),
            forbiddenFlags = setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")
        )
    ),
    Choice(
        label = label,
        nextNodeId = "c10_carry_forge",
        requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_forge"))
    ),
    Choice(
        label = label,
        nextNodeId = "c10_carry_voss",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c10_cover_stair", "c10_cover_forge"))
    )
)

/** The words at the hasty earth: by the page Kaelen wrote, or the plain custom when he wrote none. */
private fun earthChoices(): List<Choice> = listOf(
    Choice(label = "Go on.", nextNodeId = "c10_earth_name", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_name"))),
    Choice(label = "Go on.", nextNodeId = "c10_earth_deeds", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_deeds"))),
    Choice(label = "Go on.", nextNodeId = "c10_earth_plain", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_plain"))),
    Choice(label = "Go on.", nextNodeId = "c10_earth_bare", requirements = ChoiceRequirement(forbiddenFlags = C10_RITES_FAMILY))
)

/** After the burial, the first night at the north gap in Chapter XI: by where Voss and Kaelen stand. The lowest rung also covers a save with none. */
private fun toTheWeight(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c11_weight_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "c11_weight_working", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"))),
    Choice(
        label = label,
        nextNodeId = "c11_weight_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))
    )
)

/**
 * Chapter X — What Ilsevet Came to See.
 *
 * The direct assault Chapter IX's revelation made inevitable: Stonebeard is one of the last two
 * gates Ilsevet needs, so she brings the full weight of the Sanctum to it rather than trying
 * subtlety twice. This is the chapter's biggest structural swing yet — Ilsevet appears in person
 * for the first time, calm and transactional rather than cartoonish, and the siege costs Kaelen
 * the mentor figure who has anchored every chapter since II: Halvard dies holding a breach shut,
 * and passes Kaelen his warden's chain with his last words. Ilsevet herself withdraws once the
 * assault fails rather than press a fight she was only ever testing, leaving the emotional cost
 * to land harder than a combat loss would have. Tenth and toughest combat encounter yet
 * (Ilsevet's Vanguard Captain, 155 HP). The fight and Halvard's death happen regardless of how
 * Kaelen answers Ilsevet's offer, but the three responses (fortifying versus meeting the column
 * head-on beforehand, and defiance versus principle versus silence in the moment itself) now each
 * earn Kaelen a distinct reaction from Ilsevet before the fighting starts, rather than an identical
 * scene with a different stat attached.
 *
 * Judgment call flagged for review: killing off Halvard is the biggest narrative swing since
 * Chapter III introduced an external antagonist. It's played as a genuine loss (not a fake-out)
 * to give the second-act turn toward the finale real weight, and to hand Kaelen tangible narrative
 * momentum (the warden's chain, sole responsibility for Stonebeard) heading into the endgame.
 *
 * The night before the column. Between the preparations and Ilsevet's arrival Kaelen spends the dark on up to three
 * of four places: the forge with Halvard, the wall with Voss, Edda's people (only where Chapter VII left a promise made
 * for them), and the smiths' gate-book. Each reads what Chapters V to IX left behind. Halvard's place at the north gap is
 * never in question and no choice in the night changes it. What the night changes is what Kaelen carries into the morning.
 * Where he stands when the column comes (with Voss, on the stair, or at the forge door) decides who carries Halvard
 * afterwards and what they say to Kaelen, and the page he wrote decides the words at the hasty earth. The shipped scenes
 * are untouched; only where some of their choices lead has changed.
 */
val chapter10Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_last_two",
        chapterId = "chapter_10",
        title = "The Last Two",
        illustrationId = "the_last_two",
        narrativeText = """
            Halvard doesn't waste time being surprised anymore, which is its own kind of grim
            comfort. "One of the last two," he says, turning Ordrun's own words over like a coin
            that keeps landing wrong side up. "She's not going to send an envoy for a second try.
            She's going to send everything."

            Voss, still healing, still here rather than gone back to whatever's left of the
            Order, doesn't argue with him. She just starts checking the edge on a borrowed blade,
            the way a person checks something they already know is sharp enough, just to have
            something to do with their hands.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we fortify everything we have left to fortify.\"",
                nextNodeId = "c10_evening",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C10_TELLING_FLAGS,
                    setFlags = setOf("fortified_for_the_last_stand")
                )
            ),
            Choice(
                label = "\"Then we make sure whoever comes through that tunnel regrets it.\"",
                nextNodeId = "c10_evening",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C10_TELLING_FLAGS,
                    setFlags = setOf("prepared_to_meet_them_hard")
                )
            )
        )
    ),
    StoryNode(
        id = "c10_evening",
        chapterId = "chapter_10",
        title = "The Night Before",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Dusk finds Stonebeard quiet in the way of a place that has finished arguing
            with itself. The scouts are in. The column will be at the tunnel mouth by
            first light, and whatever the Hold can still do to meet it is already being
            done by people who do not need a jailer to say how.

            Halvard has spent the afternoon at the north gap, where the wall was patched
            in a hurry with iron that never matched the stone. Twice since noon Kaelen
            has seen him lay a hand flat against the join, the way a man tests a tooth,
            and twice he has said nothing and gone back to the forge to make more iron
            for it. Nobody has asked him to be anywhere else. Nobody would.

            There are a few hours of dark before the work starts again. Kaelen can spend
            them in several places, and not in all.
        """.trimIndent(),
        choices = eveningHub(emptySet())
    ),
    StoryNode(
        id = "c10_evening_two",
        chapterId = "chapter_10",
        title = "The Night Before",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            One place seen, and the lamps along the gallery already burning lower. There
            is time for another, and perhaps a third, if he does not linger.
        """.trimIndent(),
        choices = eveningHub(setOf("hub_second"))
    ),
    StoryNode(
        id = "c10_evening_three",
        chapterId = "chapter_10",
        title = "The Night Before",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Two places seen, and the oil in the gallery lamps has been topped up once
            already. There is time for one more, or Kaelen can go up to the gate now and
            wait for the light with the others.
        """.trimIndent(),
        choices = eveningHub(setOf("hub_third")) + Choice(
            label = "Enough. Go up to the gate.",
            nextNodeId = "c10_before_light",
            consequences = Consequences(clearFlags = HUB_MARKERS_FOUR)
        )
    ),
    StoryNode(
        id = "c10_forge_open",
        chapterId = "chapter_10",
        title = "Iron for the Gap",
        illustrationId = "silent_forge",
        narrativeText = """
            The forge is the only room in the Hold that is warm, and Halvard is the only
            one in it. He is drawing a bar of black iron out of the coals, turning it to
            read the colour, and putting it back. He does not look up when Kaelen sits
            on the upturned bucket by the door, which is a courtesy of its own.

            "You put Kestrel's words in front of me when you could have kept them," he
            says. "I have been thinking about that." He works the bellows twice. "Nine
            winters I told myself an answer would have changed something. It would not
            have. She would have stood her gate all the same, and I would have had a
            letter to answer instead of a silence."

            He draws the bar out again and holds it to the light. "Whatever you carry
            for her, put it down somewhere you can find it. That is all the advice I
            have, and I know it is not much."
        """.trimIndent(),
        choices = forgeOuts()
    ),
    StoryNode(
        id = "c10_forge_kind",
        chapterId = "chapter_10",
        title = "Iron for the Gap",
        illustrationId = "silent_forge",
        narrativeText = """
            The forge is warm and Halvard is alone in it, drawing a bar of black iron
            from the coals and reading its colour. He nods at the bucket by the door.
            Kaelen sits.

            "You were decent to me, after Kestrel's letter," Halvard says. "I have
            thought about how, and I cannot say it was the words. I would have talked it
            into the ground, and I am glad you did not let me." He sets the bar back in
            the fire. "I keep a forge. People bring me things to mend, and then they
            stand about. It is a long time since someone knew when to sit."

            He works the bellows. "If there is something you want to ask, ask it. If
            there is not, I will be glad of the company."
        """.trimIndent(),
        choices = forgeOuts()
    ),
    StoryNode(
        id = "c10_forge_shut",
        chapterId = "chapter_10",
        title = "Iron for the Gap",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard is at the forge, drawing a bar of black iron from the coals and
            reading the colour of it. He looks at Kaelen once, from the doorway, and
            goes back to the iron.

            "There is a thing you have not said," he says. "To me, I mean. I will not
            ask what it is. I kept one of my own for nine winters, and it was no fault
            in me. It was a weight, and I put it down when I was ready." The bar goes
            back in the fire. "I would sooner you put yours down when you are ready than
            when somebody else has the time to make you."

            He nods at the bucket by the door. Kaelen sits, and neither of them mentions
            the weight again.
        """.trimIndent(),
        choices = forgeOuts()
    ),
    StoryNode(
        id = "c10_forge_talk",
        chapterId = "chapter_10",
        title = "Iron for the Gap",
        illustrationId = "silent_forge",
        narrativeText = """
            The bar goes back into the coals. Halvard works the bellows and waits, in
            the manner of a man who has said what he meant to say and will say more if
            asked. The forge ticks and settles in the draught from the door.
        """.trimIndent(),
        choices = forgeTalkOuts()
    ),
    StoryNode(
        id = "c10_forge_gap",
        chapterId = "chapter_10",
        title = "The Lean of the Wall",
        illustrationId = "silent_forge",
        narrativeText = """
            "I will hold the north gap when they come," Kaelen says. "You have given it
            enough days."

            Halvard does not laugh, though it looks as if he might. He lays the bar
            across the anvil. "You would hold it," he says. "Better than most. You would
            also lean the way the wall asks you to, which is how it comes down." He
            rests his palm on the cold iron as though it were the stone. "That wall has
            leaned since the day it was laid. Whoever stands on it has to lean the other
            way, a little, all the time, and know to the hair when to stop. It took me a
            lifetime to learn where. You have had a season."

            He looks at Kaelen then, steadily. "It is not pride, and I am not ashamed to
            be asked. The wall stands if I am on it and falls if I am not. I would have
            it otherwise if it could be. Go and be useful somewhere a wall cannot fall
            on you."
        """.trimIndent(),
        choices = eveningOnward("Go on.", Consequences(unlockTrophy = "The Last Evening"))
    ),
    StoryNode(
        id = "c10_forge_voss_trusts",
        chapterId = "chapter_10",
        title = "What He Makes of Her",
        illustrationId = "silent_forge",
        narrativeText = """
            "Voss," Halvard says, as though testing the weight of the name. "I told you
            on the stair that she was lying to me about something. She was, a little. So
            were you. We all are." He turns the bar. "She went into that place for you
            with my iron and her own nerve, and she came out of it. Somewhere in the
            middle of that I stopped holding the small lie against her."

            He almost smiles. "If she asks what I made of her, you may tell her I held
            to my first guess. It is a rare week when a man gets to say so."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_forge_voss_doubts",
        chapterId = "chapter_10",
        title = "What He Makes of Her",
        illustrationId = "silent_forge",
        narrativeText = """
            "Voss," Halvard says, and takes his time with it. "I told you on the stair
            that I did not trust her, and I did not. I told you to go anyway, and I
            would like it noted that I knew the difference." He turns the bar in the
            coals. "I lent her iron I had no business lending. I find I do not regret
            it. That is not trust. I think it is on the road there."

            He glances at the door, as if she might be standing in it. "Don't tell her I
            said so. She would be unbearable."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_forge_side",
        chapterId = "chapter_10",
        title = "The Stair Rules",
        illustrationId = "silent_forge",
        narrativeText = """
            "On the north stair you took my side when it was the unpopular one," Halvard
            says. "I did not thank you. I should have." He wipes his hands on his apron,
            slowly. "The rules are what keep thirty people from becoming a crowd. Nobody
            forgives the man who keeps them. They only do better for it, and never know
            why."

            He looks at the bar and not at Kaelen. "I would not have had to ask you to
            stand behind a term like that if I had been a kinder man. I am glad I did
            not have to ask twice."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_wall_wary",
        chapterId = "chapter_10",
        title = "On the Wall",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Voss is on the south parapet with the borrowed blade laid across her
            forearms, looking at nothing in particular. She does not turn when Kaelen
            comes up, which is how he knows she heard him on the stair. Below them the
            Hold's few lamps make a short, uncertain street.

            "The wall needs someone who can hold a stretch of it," she says, before he
            asks. "I can. That is all this is." She tests the footing with her heel, a
            soldier's habit. "Don't make it into more than it is."

            She has not moved her weight to make room for him. She has not moved it
            away, either.
        """.trimIndent(),
        choices = wallOuts()
    ),
    StoryNode(
        id = "c10_wall_working",
        chapterId = "chapter_10",
        title = "On the Wall",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Voss is on the south parapet, and she has made a place for him before he
            reaches the top of the stair: a coat folded on the stone, a hand's width
            from her own. The borrowed blade lies across her knees, and she is working
            oil into a strip of cloth as though the cloth might tell her something.

            "Halvard came up this afternoon," she says. "He showed me which stone to
            watch at the north gap, and what it does when it is about to move. He told
            me not to be the one who tells him." She almost smiles. "I think I was meant
            to find that reassuring."

            She sets the cloth down. "He wants the blade back, you know. He said so as
            though I had stolen it. I intend to be very hurt by that, once there is
            time."
        """.trimIndent(),
        choices = wallOuts()
    ),
    StoryNode(
        id = "c10_wall_trusted",
        chapterId = "chapter_10",
        title = "On the Wall",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Voss is on the south parapet with the borrowed blade across her knees, and
            she looks up as soon as his foot touches the stair, the way you look up for
            someone you were hoping to see. There is room beside her already. Neither of
            them mentions that she saved it.

            "I have stood on a great many walls," she says. "Most of them I was told to
            stand on." She watches the dark where the tunnel mouth is. "I would like it
            noted, for whatever it is worth later, that nobody told me to stand on this
            one."

            Below, a smith crosses the yard with a hod of coals and waves up at the
            wall, and after a moment Voss, to her visible surprise, waves back.
        """.trimIndent(),
        choices = wallOuts()
    ),
    StoryNode(
        id = "c10_wall_stay",
        chapterId = "chapter_10",
        title = "The Watch Changes",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            He does not leave. For a long while neither of them speaks, and the dark
            does what dark does, which is nothing, patiently. Once she shifts the blade
            from one arm to the other and he sees what it costs her to hide how long she
            has been standing. Once a smith's child, far below, laughs at something and
            is hushed.

            When the watch changes, two dwarves coming up the stair with a lantern
            between them, Voss rises and offers him the place she has warmed. "You did
            that well," she says. It is not praise. It is a thing she noticed, set down
            where he can see it.
        """.trimIndent(),
        choices = eveningOnwardRung("Go on.", delta = 1)
    ),
    StoryNode(
        id = "c10_wall_hurt",
        chapterId = "chapter_10",
        title = "The Old Wound",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            She is standing the way she stands when it hurts, which is very straight.
            Kaelen has learned the difference by now. He asks anyway, quietly, how the
            old wound is.

            "Mended enough to hold a blade," Voss says, and there it ends, until she
            adds, to the dark and not to him, "A little worse in the cold. It will be
            cold tomorrow." The wound is not a thing she means to discuss. He can see
            her deciding not to be seen deciding it.

            "If I have to leave the wall, I will say so," she says. "I do not intend to
            have to."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_people_inside",
        chapterId = "chapter_10",
        title = "The North Stair",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The north stair is crowded in the careful way of a place where nobody has
            room and everybody is being polite about it. Blankets hang for walls. A pot
            sits over a fire the size of two hands. Edda's people have lived on this
            stair since Halvard agreed to it, under rules he set and Kaelen promised to
            stand behind, and in all that time there has not been one fight over the
            water.

            Edda rises when she sees him. "They are asking whether the door will hold,"
            she says. "I tell them it has held longer than any of us has been alive."
            She says it flatly, the way you say a thing so it can be leaned on. "You
            will tell me if that changes."

            It is not a question, and he does not answer it as one.
        """.trimIndent(),
        choices = peopleOuts(carter = true)
    ),
    StoryNode(
        id = "c10_people_outside",
        chapterId = "chapter_10",
        title = "The Ridge Camp",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Edda's people are camped in the lee of the ridge, half a mile from the
            tunnel, where the hill takes the wind. Halvard's grain came three days ago
            in a cart and was counted out sack by sack, and there is a fire for every
            ten people and not one more. Nobody has asked for a door. Nobody has stopped
            looking at the Hold's gate, either.

            Edda meets him halfway across the stubble. "He sent grain and no door," she
            says. "I do not hold it against him tonight. Tonight I think I would not
            want a door that other people have to stand in front of." She looks past him
            at the gate. "There is a way through the back galleries to the hill road,
            and Halvard has shown my eldest where. He did not have to."

            She takes a long breath. "Tell him it was noticed."
        """.trimIndent(),
        choices = peopleOuts(carter = false)
    ),
    StoryNode(
        id = "c10_people_unspent",
        chapterId = "chapter_10",
        title = "The Ravine",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Edda's people have come to the foot of the Hold anyway, because the road ran
            out, and they sit in the ravine below the gate in the dusk without asking
            for anything. Kaelen made Edda a promise that Halvard has never been told.
            It sits in his pocket like a coin he has not decided to spend.

            Edda sees him look at the gate, and then at her, and shakes her head before
            he can speak. "Not tonight," she says. "Spend it when there is a Hold to
            spend it on. I would not have it be one more thing Halvard has to carry into
            a hard morning."

            A child is asleep with her head on Edda's knee. Neither of them looks at
            her.
        """.trimIndent(),
        choices = peopleOuts(carter = false)
    ),
    StoryNode(
        id = "c10_tam",
        chapterId = "chapter_10",
        title = "Tam's Lamp",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Tam has found a lamp somewhere and is keeping it lit by her feet, shielded
            with a coat, with the stubbornness of a fifteen-year-old who has been told
            it is wasteful. She is Kestrel's apprentice, and for most of the journey
            north she has been trying to look as if she belongs to nobody.

            "Kestrel's rule," she says when Kaelen sits down beside her. "A warden's
            door is never left dark. I know this isn't a warden's door." She watches the
            flame instead of him. "Halvard let me carry the oil for it. He didn't say
            anything. He just handed me the can."

            After a while she adds, "I went and looked at that wall today. It doesn't
            lean the way a wall leans. It leans the way a ward does when it has stopped
            being one." She says it as a fact. Then, as if someone else had put it on
            her: "I didn't tell him. I think he knows."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_carter",
        chapterId = "chapter_10",
        title = "The Carter",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The broad woman in the carter's coat is sitting on an upturned cart-box at
            the edge of the stair, doing nothing with great economy. Her mule is
            somewhere out of the wind. She has not asked for anything, and Kaelen has
            not forgotten that she has not.

            "The boy's asleep," she says, with a tilt of her head toward the blankets.
            "He wanted to be given something to carry tomorrow. I told him you would
            think of something." She watches the lamp. "You spent a man's charcoal for
            that boy. I would not have. I am glad someone did."

            She says nothing for some time. Then: "Whatever you ask of me tomorrow, ask
            it early."
        """.trimIndent(),
        choices = eveningOnward("Go on.")
    ),
    StoryNode(
        id = "c10_page",
        chapterId = "chapter_10",
        title = "The Gate-Book",
        illustrationId = "silent_forge",
        narrativeText = """
            The smiths have set a lamp on the anvil and an open book beside it, and four
            of them wait with their hands folded in the way of people who do not want to
            be thought to be waiting. It is the Hold's custom, the oldest explains, to
            bring the gate-book up to date before any hard morning. Each keeper of a
            gate has a line in it, written by someone who knows them and read, in time,
            by someone who knew them less. Halvard's line is blank. Keepers tend to
            outlive the people who would write theirs.

            "You have known him a season," the old smith says, "and that is longer than
            some of us have known anyone. We would be glad if you wrote it. We will not
            read it to him."
        """.trimIndent(),
        choices = eveningOnward(
            "Write his name, and the name of the gate.",
            Consequences(clearFlags = C10_RITES_FAMILY, setFlags = setOf("c10_rites_name"))
        ) + eveningOnward(
            "Write what he has done.",
            Consequences(clearFlags = C10_RITES_FAMILY, setFlags = setOf("c10_rites_deeds"))
        ) + eveningOnward(
            "Write only that he held.",
            Consequences(clearFlags = C10_RITES_FAMILY, setFlags = setOf("c10_rites_plain"))
        ) + eveningOnward("Tell them it is not yours to write.", Consequences(clearFlags = C10_RITES_FAMILY))
    ),
    StoryNode(
        id = "c10_before_light",
        chapterId = "chapter_10",
        title = "Before Light",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The Hold does not sleep, quite. It lies still with its eyes open, and the
            sound it makes is the sound of many people being careful. Somewhere below,
            the smiths' hammers have gone quiet for the first time since noon, and in
            the quiet Kaelen hears what the quiet was covering: far down the tunnel, a
            steady wrongness in the air, the sound of a great many feet that are trying
            to be quiet and are not.

            Halvard is already at the north gap, a small figure against the patched
            stone, laying out his iron in the order he will need it. He has not asked
            anyone to join him. He does not look round. There is work left to do before
            the light comes, and the Hold goes to do it.
        """.trimIndent(),
        choices = toTheLine("Go up to the gate.")
    ),
    StoryNode(
        id = "c10_line_ground_yard",
        chapterId = "chapter_10",
        title = "What the Hold Has",
        illustrationId = "warden_chamber",
        narrativeText = """
            The shed in the yard burned in the ambush, and it took the whetstones with
            it. The forge still makes iron, and the smiths have made a great deal of it,
            but nothing fine. Whatever edges the Hold carries into the morning are the
            edges they already have, and there is nobody left to put a better one on
            them.

            Halvard has said this once, without complaint, as a man reads out a list of
            what is in the larder. It is a short list. He seems to find it clarifying.
        """.trimIndent(),
        choices = holdChoices("Go on.")
    ),
    StoryNode(
        id = "c10_line_ground_stair",
        chapterId = "chapter_10",
        title = "What the Hold Has",
        illustrationId = "warden_chamber",
        narrativeText = """
            The armoury is whole, which Halvard counts as the one good turn the ambush
            did him. The riders who ran saw where the stair leads, though, and every
            dwarf in the Hold knows what that means: the column will not have to look
            for the way in.

            Halvard has had the old tunnel mouth marked with white chalk, so that nobody
            in the dark takes a wrong turning, and so that anyone who comes by that road
            comes by one the Hold has chosen. It is not much of an advantage. He has
            chosen to treat it as one.
        """.trimIndent(),
        choices = holdChoices("Go on.")
    ),
    StoryNode(
        id = "c10_line_ground_road",
        chapterId = "chapter_10",
        title = "What the Hold Has",
        illustrationId = "warden_chamber",
        narrativeText = """
            The Hold came through the ambush untouched, and Halvard has been wary of
            that ever since. The riders who ran took the road, and the road carries
            news. By now Ilsevet knows who has been standing beside the jailer, and
            Halvard has said, once, to nobody in particular, that she will want to see
            for herself.

            He said it without heat. It is simply one more thing to be ready for, and he
            has added it to the list.
        """.trimIndent(),
        choices = holdChoices("Go on.")
    ),
    StoryNode(
        id = "c10_line_hold_inside",
        chapterId = "chapter_10",
        title = "Who Is Here",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Edda's people are on the north stair and the galleries behind it, thirty or
            so under the rules Halvard set. He has given every pair of hands that can
            take one a task. The old and the small are in the deepest room, and the rest
            are carrying water and wedging doors. Nobody has been asked to be brave.
            They have been asked to be useful, which Halvard seems to think is kinder.

            Halvard himself does not seem to be anywhere in particular. He is at the
            north gap, then at the forge door, then at the gap again. He moves like a
            man who knows to the stone which parts of his Hold he can afford to leave.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "c10_line_cover"))
    ),
    StoryNode(
        id = "c10_line_hold_outside",
        chapterId = "chapter_10",
        title = "Who Is Here",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Edda's people are not in the Hold. They are leaving it, quietly, by the back
            galleries to the hill road, out of the line of anything that will come up
            the tunnel, with Halvard's grain in their packs and his directions in Edda's
            head. Halvard has asked for one thing in return: that someone keep the back
            stair for the hour it will take them to clear it, in case the morning goes
            badly for the gate.

            The Hold is thin without them. Everyone left in it has more than one job.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "c10_line_cover"))
    ),
    StoryNode(
        id = "c10_line_hold_unspent",
        chapterId = "chapter_10",
        title = "Who Is Here",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Edda's people are in the ravine below the gate, and the promise Kaelen made
            them has not been spent. Edda has put the matter to Halvard herself, without
            Kaelen's help: a pair of her people to carry water on the stair, in exchange
            for the back stair kept open if the gate goes badly. Halvard agreed in four
            words. Kaelen is not sure what that costs the Hold, and nobody has offered
            to tell him.

            Someone will have to keep the back stair for the hour it takes them to climb
            it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "c10_line_cover"))
    ),
    StoryNode(
        id = "c10_line_cover",
        chapterId = "chapter_10",
        title = "Where to Stand",
        illustrationId = "warded_gate",
        narrativeText = """
            Halvard has already walked to the north gap and set his iron down. He does
            not announce it. He stands where the wall leans, with his weight on the
            right foot, and the matter is settled. That leaves the rest of the Hold with
            a question that is Kaelen's to answer, because Halvard has said it is: there
            are three places the morning will press on, and Kaelen has two hands.

            Voss will be at the gate whatever he decides. She has said so, and she does
            not say things twice. The stair has the Hold's families on it, and the back
            way out. The forge door has the smiths, who have never been asked to stand
            anywhere but at an anvil. Everyone has someone. But a person can stand in
            only one place when the light comes, and everyone will see which.
        """.trimIndent(),
        choices = coverChoice("Stand with Voss at the gate.", "c10_cover_voss", StatType.COURAGE) +
            coverChoice("Go to the stair, to the people on it.", "c10_cover_stair", StatType.HONOUR) +
            coverChoice("Stand at the forge door with the smiths.", "c10_cover_forge", StatType.COURAGE)
    ),
    StoryNode(
        id = "ilsevet_arrives_fortified",
        chapterId = "chapter_10",
        title = "Ilsevet Arrives",
        illustrationId = "ilsevet_arrives",
        narrativeText = """
            They spend what little time they have left reinforcing everything reinforceable: the
            broken gate braced twice over, every gap Halvard can reach patched with whatever iron
            Stonebeard has left to give. It isn't enough to stop an army. It might be enough to
            slow one down to a fight instead of a slaughter.

            She doesn't come at the front of a column this time either; she doesn't come with one
            at all, not visibly. Ilsevet walks into Stonebeard's braced gate alone, unhurried, the
            way Voss always was and the Marshal's envoy never quite managed, and behind her the tunnel is
            full of shapes that haven't stepped into the light yet. She takes in the barricades
            with something like professional respect before she speaks.

            She's smaller than the name suggested, and calmer than anyone leading an army has any
            right to be. "Kaelen," she says, like they've met before, though they haven't. "I
            already have four settings filled. I only need one more hand's worth of cooperation
            to save both of us a very long war. Give me the brand willingly, and everyone behind
            you keeps breathing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Answer her.", nextNodeId = "kaelens_answer")
        )
    ),
    StoryNode(
        id = "ilsevet_arrives_hard",
        chapterId = "chapter_10",
        title = "Ilsevet Arrives",
        illustrationId = "ilsevet_arrives",
        narrativeText = """
            There's no time left to build walls, so they spend what little they have sharpening
            themselves instead: blades, nerve, the particular readiness of people who've decided
            retreat isn't on the table. Voss says it plainest: better to meet it standing than
            wait for it crouched.

            She doesn't come at the front of a column this time either; she doesn't come with one
            at all, not visibly. Ilsevet walks into Stonebeard's broken gate alone, unhurried, the
            way Voss always was and the Marshal's envoy never quite managed, and behind her the tunnel
            is full of shapes that haven't stepped into the light yet. She takes one look at the
            two of them, weapons already drawn, and something that might be amusement crosses her
            face.

            She's smaller than the name suggested, and calmer than anyone leading an army has any
            right to be. "Kaelen," she says, like they've met before, though they haven't. "I
            already have four settings filled. I only need one more hand's worth of cooperation
            to save both of us a very long war. Give me the brand willingly, and everyone behind
            you keeps breathing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Answer her.", nextNodeId = "kaelens_answer")
        )
    ),
    StoryNode(
        id = "kaelens_answer",
        chapterId = "chapter_10",
        title = "Kaelen's Answer",
        illustrationId = "kaelens_answer",
        narrativeText = """
            Whatever she expects, fear, negotiation, a stalling question, isn't what she gets.
            The brand aches on Kaelen's palm, the way it always does when the pressure behind the
            gate takes an interest, and for once that ache feels like the easiest thing in the
            room to trust.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Come and take it, then.\"",
                nextNodeId = "the_gate_falls_defiant",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_ilsevet")
                )
            ),
            Choice(
                label = "\"Whatever you're building, it doesn't get built with what's mine.\"",
                nextNodeId = "the_gate_falls_principled",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_on_principle")
                )
            ),
            Choice(
                label = "Say nothing. Draw your sword instead.",
                nextNodeId = "the_gate_falls_silent",
                consequences = Consequences(setFlags = setOf("answered_with_steel"))
            ),
            Choice(
                label = "\"I told your envoy no. I will tell you the same.\"",
                nextNodeId = "the_gate_falls_defiant",
                requirements = ChoiceRequirement(requiredFlags = setOf("refused_envoy")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_ilsevet")
                )
            ),
            Choice(
                label = "\"Your envoy made me this offer. I listened then. I have finished listening.\"",
                nextNodeId = "the_gate_falls_principled",
                requirements = ChoiceRequirement(requiredFlags = setOf("heard_envoy_offer")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_on_principle")
                )
            ),
            Choice(
                label = "\"Something older than you warned me you would come asking. It was right about that much.\"",
                nextNodeId = "the_gate_falls_defiant",
                requirements = ChoiceRequirement(requiredFlags = setOf("confronted_envoy_with_prisoners_words")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_ilsevet")
                )
            )
        )
    ),
    StoryNode(
        id = "the_gate_falls_defiant",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Something that isn't quite a smile touches Ilsevet's mouth, the expression of a woman
            noting a data point rather than reacting to an insult. "Bold," she says. "I've had
            bolder tell me the same thing and mean it a great deal less." She doesn't draw a
            weapon herself; she simply steps back, unbothered, and lets the shapes behind her
            answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "the_gate_falls_principled",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Ilsevet considers that longer than she considered the first answer would have
            warranted, head tilted slightly, as though weighing whether principle is a tool she
            can use or simply an obstacle to route around. "Principle," she says, at last.
            "That's a rarer answer than defiance, down here. It won't change what happens next."
            She doesn't draw a weapon herself; she simply steps back, unbothered, and lets the
            shapes behind her answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "the_gate_falls_silent",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Kaelen's sword clears its sheath before he's decided on a single word to go with it,
            and something in Ilsevet's calm falters, just slightly, at an answer she can't file
            away as easily as speech. "No parting words," she says, almost to herself.
            "Interesting." She doesn't draw a weapon herself; she simply steps back, unbothered,
            and lets the shapes behind her answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "stonebeard_siege_defeat",
        chapterId = "chapter_10",
        title = "Held By Someone Else",
        illustrationId = "the_gate_falls",
        narrativeText = """
            The vanguard captain doesn't so much defeat Kaelen as simply outlast him, patient in a
            way the rest of Ilsevet's people rarely bother to be, and the ground comes up to meet
            him before he's ready to admit he's lost this exchange. It should be the end of it.

            It isn't, because Halvard breaks from the gap he's been holding just long enough to
            put the captain down himself, buying Kaelen the one thing he can't buy for himself
            right now: another minute standing. It costs the old warden something he can't easily
            spare, with the wave still pressing at his back.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go to Halvard.",
                nextNodeId = "halvard_falls",
                consequences = Consequences(setFlags = setOf("lost_to_ilsevets_vanguard_captain"))
            )
        )
    ),
    StoryNode(
        id = "halvard_falls",
        chapterId = "chapter_10",
        title = "Halvard Falls",
        illustrationId = "halvard_falls",
        narrativeText = """
            The captain goes down, but not before the fight spills past Kaelen entirely: a second
            wave crashing through the gap in the wall Halvard's been patching with iron that
            never quite matched the stone. Halvard throws himself into the gap rather than let it
            widen, an old dwarf's whole weight against a door built for younger hands, and takes
            a blow meant for the stone instead of him.

            He doesn't fall right away. He holds the gap shut long enough for the rest of the
            wave to break against it, and only lets go once there's nothing left on the other
            side to hold it shut against, as though even dying were something he'd rather finish
            properly.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to him.", nextNodeId = "what_halvard_leaves_behind")
        )
    ),
    StoryNode(
        id = "what_halvard_leaves_behind",
        chapterId = "chapter_10",
        title = "What Halvard Leaves Behind",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            He's still breathing when Kaelen reaches him, which is its own small, cruel mercy:
            long enough for one more thing to be said. His hand finds the warden's chain at his
            own throat, tarnished silver-iron, six links where there should have been six
            wardens.

            "Wear it, or don't. Wasn't ever really about the chain." His grip tightens once,
            weaker than it should be, stubborn right up to the edge of it. "Ilsevet didn't come
            here to kill you, lad. She came to see if she could still lose. Make sure the answer
            stays yes."

            He doesn't say anything else, though his hand stays closed around Kaelen's a moment
            longer than it needs to.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the chain.",
                nextNodeId = "c10_carry_bridge",
                consequences = Consequences(
                    grantItemIds = listOf("halvards_warden_chain"),
                    setFlags = setOf("halvard_died", "took_the_warden_chain")
                )
            )
        )
    ),
    StoryNode(
        id = "c10_carry_bridge",
        chapterId = "chapter_10",
        title = "After",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            The quiet comes slowly, the way it does after a flood, into the places the
            noise has gone out of. Ilsevet's column has withdrawn up the tunnel with its
            wounded, in good order, as though that had been the plan all along. The Hold
            counts itself. Nobody else is gone. Kaelen counts twice, because the number
            is too small to believe, and then he stops counting and looks at what is on
            the stones.

            Someone has already lifted a door from its hinges. Someone else is asking,
            very quietly, who is to carry him.
        """.trimIndent(),
        choices = carryChoices("Go on.")
    ),
    StoryNode(
        id = "c10_carry_voss",
        chapterId = "chapter_10",
        title = "Who Carried Him",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            Voss and Kaelen carry him, because Kaelen stood where she stood and neither
            of them can think of a reason it should be anyone else. The door is lighter
            than it should be. Halvard is lighter than he should be, and the thing
            Kaelen would like to say about that does not come. They take the north stair
            one step at a time. Voss takes the head end without being asked, though she
            is favouring her side and thinks nobody has seen.

            At the foot of the stair she shifts her grip and says, without looking at
            him, "You could have stood anywhere." "I know," Kaelen says. "I noticed,"
            she says, and takes the weight again. She has the borrowed blade at her
            belt. Nobody mentions it, and she does not touch it.
        """.trimIndent(),
        choices = earthChoices()
    ),
    StoryNode(
        id = "c10_carry_stair_edda",
        chapterId = "chapter_10",
        title = "Who Carried Him",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            The men from the stair carry him, because they asked, and because Edda said
            that if the Hold's door was Halvard's to open then carrying him was theirs
            to do. They go slowly, in step, and nobody speaks. A girl walks ahead of
            them with a lamp that was lit last night and has not been let out.

            Edda stops Kaelen at the turn of the passage. "The door was his to open, and
            he opened it," she says. "It is yours now to keep or to shut. We will not
            ask which tonight." She holds his eye a moment longer than she needs to.
            "But you stood with us when you could have stood somewhere easier. We will
            remember which."
        """.trimIndent(),
        choices = earthChoices()
    ),
    StoryNode(
        id = "c10_carry_stair",
        chapterId = "chapter_10",
        title = "Who Carried Him",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            The men from the stair carry him, because they asked, and because the oldest
            of them said that a keeper who had held the door for their children could be
            carried by their hands. They go slowly, in step, and nobody speaks. A girl
            walks ahead of them with a lamp that was lit last night and has not been let
            out.

            The oldest woman on the stair stops Kaelen at the turn of the passage. "He
            kept the door," she says. "It is yours now to keep or to shut. Nobody will
            ask which tonight." She holds his eye a moment longer than she needs to.
            "But you stood with us when you could have stood somewhere easier. We will
            remember which."
        """.trimIndent(),
        choices = earthChoices()
    ),
    StoryNode(
        id = "c10_carry_forge",
        chapterId = "chapter_10",
        title = "Who Carried Him",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            The smiths carry him, the oldest of them at the head, and they carry him the
            way the Hold carries its own: shoulder high, in step, with a tune hummed so
            low it is more a pressure in the stone than a sound. They have not asked
            Kaelen to help, and he understands that as a courtesy and as a test. He
            falls in at the foot. Nobody says anything about it.

            At the forge door the old smith stops them, lays his palm on the cold anvil,
            and nods to Kaelen: not to the jailer, to the one who stood in the doorway.
            "The forge will be lit at first light," he says. "It will be lit tomorrow
            too. Somebody has to keep working what he left."
        """.trimIndent(),
        choices = earthChoices()
    ),
    StoryNode(
        id = "c10_earth_name",
        chapterId = "chapter_10",
        title = "A Hasty Earth",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            It is not the burial he will have in the end. Everyone there knows it, and
            the old smith says so first, to get it out of the way: a hasty earth for a
            hard morning, to be done properly when there is time and a stone to cut. He
            opens the gate-book on his forearm and reads what Kaelen wrote, which is a
            name and the name of a gate, and nothing else. It is short enough to be said
            in one breath. Said aloud, it sounds like a great deal.

            The first earth is already in someone's hand.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter10_end"))
    ),
    StoryNode(
        id = "c10_earth_deeds",
        chapterId = "chapter_10",
        title = "A Hasty Earth",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            It is not the burial he will have in the end. Everyone there knows it, and
            the old smith says so first, to get it out of the way: a hasty earth for a
            hard morning, to be done properly when there is time and a stone to cut. He
            opens the gate-book on his forearm and reads what Kaelen wrote, which is a
            short account: a door opened to strangers, a wall leaned on through a hard
            night, a blade lent that he did not have to lend. Said aloud, in the old
            smith's flat voice, it sounds less like praise than like a debt being
            squared.

            The first earth is already in someone's hand.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter10_end"))
    ),
    StoryNode(
        id = "c10_earth_plain",
        chapterId = "chapter_10",
        title = "A Hasty Earth",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            It is not the burial he will have in the end. Everyone there knows it, and
            the old smith says so first, to get it out of the way: a hasty earth for a
            hard morning, to be done properly when there is time and a stone to cut. He
            opens the gate-book on his forearm and reads what Kaelen wrote, which is one
            line: that he held. The old smith reads it twice, because the first time it
            seemed too short to be all of it. The second time nobody looks away.

            The first earth is already in someone's hand.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter10_end"))
    ),
    StoryNode(
        id = "c10_earth_bare",
        chapterId = "chapter_10",
        title = "A Hasty Earth",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            It is not the burial he will have in the end. Everyone there knows it, and
            the old smith says so first, to get it out of the way: a hasty earth for a
            hard morning, to be done properly when there is time and a stone to cut. He
            has the gate-book open on his forearm and no line to read, and he stands for
            the length of a breath with his thumb on the blank place. Then he closes it
            and says what the Hold says when there is nothing else: the keeper's name,
            once, and the name of the gate. It is enough. It is not what Kaelen would
            have chosen, if he had chosen.

            The first earth is already in someone's hand.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter10_end"))
    ),
    StoryNode(
        id = "chapter10_end",
        chapterId = "chapter_10",
        title = "End of Chapter X — What Ilsevet Came to See",
        illustrationId = "chapter10_threshold",
        narrativeText = """
            Ilsevet is gone before the dust settles, the way she arrived: unhurried, untroubled,
            having lost a captain and a great deal of a garrison for a hand's worth of cooperation
            she didn't get. Kaelen doesn't imagine that particular arithmetic troubles her for
            long.

            Stonebeard buries its dead the dwarven way, deep and unmarked, and Kaelen stands over
            one grave longer than the rest. Voss doesn't offer comfort. She just stays, which is
            its own kind of answer to a question neither of them asks aloud.

            Somewhere north or south or some direction with a name Kaelen still doesn't know, one
            gate stands between Ilsevet and everything her seventh door was built for. He intends
            to find it before she does, and to make it cost her more than the last one did.

            Chapter XI awaits.
        """.trimIndent(),
        choices = toTheWeight("Go on.")
    )
)

val chapter10 = com.thelastjailer.app.Chapter(
    id = "chapter_10",
    number = 10,
    title = "Chapter X — What Ilsevet Came to See",
    startNodeId = "the_last_two"
)
