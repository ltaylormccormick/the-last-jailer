package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/** Family C13a: whether Kaelen asked Selvane what Ilsevet was before where Voss could hear. Asking low sets nothing. */
val C13_ASK_FAMILY: Set<String> = setOf("c13_asked_openly")

/** Family C13b: what Kaelen told Edda about the Hold's door. Alternatives; each clears the other. */
val C13_DOOR_FAMILY: Set<String> = setOf("c13_door_open", "c13_door_barred")

/**
 * Everything Chapter XIII records: the five shipped answers, the three shipped stances on who holds a gate, the two new
 * families and the hub markers. The chapter's first choices clear it all, so a replay reaches the Order's delegation with an
 * empty slate and the stance it chooses is the only one it holds.
 */
val C13_TELLING_FLAGS: Set<String> = setOf(
    "stood_with_voss", "let_voss_go_alone", "kaelen_vouched_for_voss", "trusted_voss_account",
    "began_training_new_wardens", "stood_watch_rather_than_delegate", "deferred_the_question"
) + C13_ASK_FAMILY + C13_DOOR_FAMILY + HUB_MARKERS

/** One way into a scene from a choice: which flags choose the variant, and where it leads. */
private class Variant(val target: String, val required: Set<String> = emptySet(), val forbidden: Set<String> = emptySet())

private val ridersVariants = listOf(
    Variant("c13_m_riders_called", required = setOf("c7_voss_called_them_off")),
    Variant("c13_m_riders_not", forbidden = setOf("c7_voss_called_them_off"))
)

private val greymoorVariants = listOf(
    Variant("c13_m_greymoor_sealed", required = setOf("sealed_greymoor_personally")),
    Variant("c13_m_greymoor_reported", required = setOf("reported_greymoor_honestly")),
    Variant("c13_m_greymoor_left", forbidden = setOf("sealed_greymoor_personally", "reported_greymoor_honestly"))
)

/** The seal is a matter only for a Kaelen who spent the signet at the tithe arch or opened the cell wing with it; the two cannot both be true. */
private val sealVariants = listOf(
    Variant("c13_m_seal_spent", required = setOf("c8_exit_tithe")),
    Variant("c13_m_seal_door", required = setOf("c9_door_seal"))
)

private fun hearingEntries(label: String, marker: String, variants: List<Variant>, extra: Set<String>): List<Choice> =
    variants.map { v ->
        Choice(
            label = label,
            nextNodeId = v.target,
            requirements = ChoiceRequirement(requiredFlags = v.required, forbiddenFlags = v.forbidden + marker),
            consequences = Consequences(setFlags = setOf(marker) + extra)
        )
    }

/** The matters on Selvane's table. The seal is simply absent for a Kaelen who has no such matter. */
private fun hearingHub(extra: Set<String>): List<Choice> =
    hearingEntries("The loyalists at the black door.", "hub_a", ridersVariants, extra) +
        hearingEntries("Greymoor's gate.", "hub_b", greymoorVariants, extra) +
        hearingEntries("The Order's seal.", "hub_c", sealVariants, extra)

/** Out of a matter: back to the table after the first, on to the verdict after the second (which clears the markers). Exactly one is ever visible. */
private fun hearingOnward(label: String): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c13_hearing_again",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("hub_second"))
    ),
    Choice(
        label = label,
        nextNodeId = "selvanes_verdict",
        requirements = ChoiceRequirement(requiredFlags = setOf("hub_second")),
        consequences = Consequences(clearFlags = HUB_MARKERS)
    )
)

/** From the stirrup to the first night after the Order, by where Voss and Kaelen stand. The lowest rung also covers a save with none. */
private fun toTheNight(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c13_night_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "c13_night_working", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"))),
    Choice(
        label = label,
        nextNodeId = "c13_night_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))
    )
)

/**
 * From a night scene to the eve of the rites, by where Kaelen stood when the column came. The stair and the forge are read by
 * their flags and Voss's gate by the absence of both, which is also where a save with no answer goes.
 */
private fun toTheEve(label: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c13_eve_stair", requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_stair")), consequences = consequences),
    Choice(label = label, nextNodeId = "c13_eve_forge", requirements = ChoiceRequirement(requiredFlags = setOf("c10_cover_forge")), consequences = consequences),
    Choice(
        label = label,
        nextNodeId = "c13_eve_voss",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c10_cover_stair", "c10_cover_forge")),
        consequences = consequences
    )
)

/** From the eve to the gate-book, by the page Kaelen wrote in Chapter X, or straight to the rites when he wrote none. */
private fun toTheBook(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c13_book_name", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_name"))),
    Choice(label = label, nextNodeId = "c13_book_deeds", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_deeds"))),
    Choice(label = label, nextNodeId = "c13_book_plain", requirements = ChoiceRequirement(requiredFlags = setOf("c10_rites_plain"))),
    Choice(label = label, nextNodeId = "halvards_rites", requirements = ChoiceRequirement(forbiddenFlags = C10_RITES_FAMILY))
)

/**
 * The shipped rubbing, written once per word from Fenmoor Chapter XI left: Thessaly came, sent her answer, or sent none. Every
 * copy grants the rubbing exactly as shipped. The last copy also covers a save that holds neither of the first two.
 */
private fun afterTheCarving(label: String): List<Choice> {
    val rubbing = Consequences(grantItemIds = listOf("rubbing_of_halvards_mark"))
    return listOf(
        Choice(label = label, nextNodeId = "c13_thessaly_attended", requirements = ChoiceRequirement(requiredFlags = setOf("invited_thessaly_to_stonebeard")), consequences = rubbing),
        Choice(label = label, nextNodeId = "c13_thessaly_pledged", requirements = ChoiceRequirement(requiredFlags = setOf("pledged_support_to_fenmoor")), consequences = rubbing),
        Choice(
            label = label,
            nextNodeId = "c13_thessaly_silent",
            requirements = ChoiceRequirement(forbiddenFlags = setOf("invited_thessaly_to_stonebeard", "pledged_support_to_fenmoor")),
            consequences = rubbing
        )
    )
}

/** From a word about Fenmoor to Edda's question about the door, where Edda's people are (Tam is with them), or on to the shipped question. */
private fun toTheDoor(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c13_door", requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_terms"))),
    Choice(label = label, nextNodeId = "c13_door", requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_refused"))),
    Choice(label = label, nextNodeId = "c13_door", requirements = ChoiceRequirement(requiredFlags = setOf("c7_hold_hidden"))),
    Choice(label = label, nextNodeId = "who_stands_with_stonebeard", requirements = ChoiceRequirement(forbiddenFlags = C7_HOLD_FAMILY))
)

/**
 * Chapter XIII — What the Order Allows.
 *
 * A deliberate pacing breather after twelve chapters of steadily escalating combat (mirroring
 * Chapter III's choice to skip a fight "for variety") — no new combat encounter this chapter at
 * all. Instead: the actual Ashen Order (distinct from Ilsevet's splinter, last seen as a genuine
 * threat in III/IV) sends a ranking official to judge Voss's standing after everything since VII,
 * Stonebeard finally holds proper rites for Halvard instead of the hasty burial after X, and the
 * chapter closes on the first real question of what comes next for the "Last Jailer" title beyond
 * pure survival — and the answer Kaelen gives (training new wardens, standing watch personally
 * beside volunteers, or deferring the question entirely) now colors how the chapter's final quiet
 * night actually feels, not just which flag it sets. `what_voss_answers` is deliberately left as a
 * true convergence, same reasoning as Chapter IX's opening: only one of its two lines is ever seen
 * in a given playthrough, gated by which stance Kaelen took earlier this chapter, so there's no
 * real choice being made there, just an acknowledgment of one already made. Ends on a quiet,
 * unsettling hook rather than another siege: the pressure behind Kaelen's own gate speaks again for
 * the first time since III, aware that Ilsevet's four-setting activation in XII reached all the way
 * to it.
 *
 * The expansion gives the decisions of Chapters VI to XII somewhere to be read. Selvane's hearing puts two of up to three
 * matters to Kaelen and Voss (the loyalists at the black door, Greymoor, the Order's seal), each read by what the story left.
 * At the stirrup Kaelen asks what Ilsevet was before, openly or low, which is what Chapter XV's dispatch answers. Voss, that
 * night, answers according to where the two of them stand. On the eve of the rites the people he stood with in Chapter X come
 * to him, and the smiths answer the line he wrote in the gate-book. After the carving, Fenmoor's word is told according to
 * what Chapter XI left, and where Edda's people are Tam lights Kestrel's lamp and Edda asks what the Hold's door is to be.
 * The shipped scenes and their routing into the next chapter are untouched; only where some of their choices lead has changed.
 */
val chapter13Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_orders_delegation_trusted",
        chapterId = "chapter_13",
        title = "The Order's Delegation",
        illustrationId = "the_orders_delegation",
        narrativeText = """
            Halvard's grave is barely a season old when the second rider in as many months comes
            up the ruined road to the black door: Order colours again, but not Voss's, and not
            Ilsevet's cinder-grey either. An actual delegation this time, banners and all, the
            kind Voss says the real Order hasn't bothered sending anywhere in years.

            "They're not here for you," Voss says, reading Kaelen's face before he's said a
            word. "Not directly. They're here for me."

            Whatever they've come to decide about her, Kaelen already decided his own answer
            back at the black door, the day he chose to believe her before she'd given him a
            single reason that wasn't just her own word.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we meet them together.\"",
                nextNodeId = "the_inquisitor_general_stood",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C13_TELLING_FLAGS,
                    setFlags = setOf("stood_with_voss")
                )
            ),
            Choice(
                label = "\"This one might go better if I'm not standing in the doorway.\"",
                nextNodeId = "the_inquisitor_general_alone",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C13_TELLING_FLAGS,
                    setFlags = setOf("let_voss_go_alone")
                )
            )
        )
    ),
    StoryNode(
        id = "the_orders_delegation_guarded",
        chapterId = "chapter_13",
        title = "The Order's Delegation",
        illustrationId = "the_orders_delegation",
        narrativeText = """
            Halvard's grave is barely a season old when the second rider in as many months comes
            up the ruined road to the black door: Order colours again, but not Voss's, and not
            Ilsevet's cinder-grey either. An actual delegation this time, banners and all, the
            kind Voss says the real Order hasn't bothered sending anywhere in years.

            "They're not here for you," Voss says, reading Kaelen's face before he's said a
            word. "Not directly. They're here for me."

            Whatever they've come to decide about her, Kaelen made his own decision the slow
            way, the day he chose to wait and watch rather than take her word for it — and
            hasn't found a reason since to wish he'd chosen faster.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we meet them together.\"",
                nextNodeId = "the_inquisitor_general_stood",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C13_TELLING_FLAGS,
                    setFlags = setOf("stood_with_voss")
                )
            ),
            Choice(
                label = "\"This one might go better if I'm not standing in the doorway.\"",
                nextNodeId = "the_inquisitor_general_alone",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C13_TELLING_FLAGS,
                    setFlags = setOf("let_voss_go_alone")
                )
            )
        )
    ),
    StoryNode(
        id = "the_inquisitor_general_stood",
        chapterId = "chapter_13",
        title = "The Inquisitor-General",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Kaelen stays where Voss can feel him at her shoulder, and the inquisitor's attention
            finds him there almost immediately, appraising him the same unhurried way she's
            appraising everything else.

            She doesn't look like Voss, or move like her either: older, slower, entirely
            unhurried in a way that has nothing to do with confidence and everything to do with
            rank. "Inquisitor Voss," she says, and doesn't offer a hand. "Or should I say,
            former Inquisitor. You've been reported dead twice and traitor once since
            Stonebeard's gate stopped being a rumour. I'm inclined to believe the second report
            over the first two, unless you give me a reason not to." Her gaze doesn't leave
            Kaelen the whole time she says it, as if he's the reason she'd need one.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let Voss answer.", nextNodeId = "what_voss_answers")
        )
    ),
    StoryNode(
        id = "the_inquisitor_general_alone",
        chapterId = "chapter_13",
        title = "The Inquisitor-General",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Kaelen keeps to the tunnel mouth, close enough to hear every word and far enough that
            the inquisitor has to decide for herself whether that was courtesy or something else.

            She doesn't look like Voss, or move like her either: older, slower, entirely
            unhurried in a way that has nothing to do with confidence and everything to do with
            rank. "Inquisitor Voss," she says, and doesn't offer a hand. "Or should I say,
            former Inquisitor. You've been reported dead twice and traitor once since
            Stonebeard's gate stopped being a rumour. I'm inclined to believe the second report
            over the first two, unless you give me a reason not to." If she notices Kaelen
            keeping his distance, she doesn't spend a word on it.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let Voss answer.", nextNodeId = "what_voss_answers")
        )
    ),
    StoryNode(
        id = "what_voss_answers",
        chapterId = "chapter_13",
        title = "What Voss Answers",
        illustrationId = "what_voss_answers",
        narrativeText = """
            "Explain it plainly," Selvane says. "You aided a man the Order marked as a threat.
            You're standing in the doorway of the exact gate three riders were sent to bury. Give
            me a version of events that doesn't end with your own colours stripped."

            Voss doesn't flinch. "The version where Ilsevet nearly got a King's Guard's binding by
            force, twice, and would have if I hadn't stopped helping the Order pretend she still
            answers to it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll vouch for that myself.\"",
                nextNodeId = "c13_hearing_vouched",
                requirements = ChoiceRequirement(requiredFlags = setOf("stood_with_voss")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("kaelen_vouched_for_voss")
                )
            ),
            Choice(
                label = "Wait outside, and trust her account.",
                nextNodeId = "c13_hearing_account",
                requirements = ChoiceRequirement(requiredFlags = setOf("let_voss_go_alone")),
                consequences = Consequences(setFlags = setOf("trusted_voss_account"))
            )
        )
    ),
    StoryNode(
        id = "c13_hearing_vouched",
        chapterId = "chapter_13",
        title = "What Is on the Table",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Selvane does not answer at once. She turns her head and finds Kaelen where he
            stands at Voss's shoulder, and she keeps looking until he understands that it
            is not a courtesy. "You vouched for her," she says. "Then you will answer for
            what you vouched for." She lays a flat hand on the stack of papers in front of
            her, which is a good deal thicker than a delegation should need. "I have more
            here than there is daylight to read. I will hear two matters. The jailer may
            say which. The rest I will take from the file."
        """.trimIndent(),
        choices = hearingHub(emptySet())
    ),
    StoryNode(
        id = "c13_hearing_account",
        chapterId = "chapter_13",
        title = "What Is on the Table",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Selvane does not answer at once. From the tunnel mouth Kaelen can see her
            decide not to raise her voice to reach him, and then decide that he will hear
            her anyway. "The jailer is listening," she says to Voss, "so he may as well be
            useful." She lays a flat hand on the stack of papers in front of her, which is
            a good deal thicker than a delegation should need. "I have more here than
            there is daylight to read. I will hear two matters, and he may say which, from
            where he stands. The rest I will take from the file."
        """.trimIndent(),
        choices = hearingHub(emptySet())
    ),
    StoryNode(
        id = "c13_hearing_again",
        chapterId = "chapter_13",
        title = "What Is on the Table",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            One matter heard, and the light in the doorway has moved a hand's width along
            the floor. Selvane squares the papers without looking at them. "One more," she
            says. "Choose."
        """.trimIndent(),
        choices = hearingHub(setOf("hub_second"))
    ),
    StoryNode(
        id = "c13_m_riders_called",
        chapterId = "chapter_13",
        title = "The Loyalists",
        illustrationId = "after_the_ambush",
        narrativeText = """
            Selvane lays one sheet flat with two fingers. "A great many of my people were
            told where to wait at your door," she says, "and they waited. Then Voss spoke
            to them in the cadence we teach, and they went home." Her eyes do not leave
            Voss. "Do you know what that is, from where I sit? It is an order, given by
            someone with no right to give it, and obeyed."

            "It was obeyed because they remembered whose it was," Voss says.

            "Yes. That is what I mean." She says it without heat. She is weighing what the
            act cost, not accusing anyone of it. "It will be told in every barracks
            between here and the coast, and not as a rumour. Whatever else the Order does
            with you, it cannot pretend that did not happen." She turns to Kaelen at last.
            "You were on that ground. What did you see?"
        """.trimIndent(),
        choices = hearingOnward("\"I saw them put the bows down.\"")
    ),
    StoryNode(
        id = "c13_m_riders_not",
        chapterId = "chapter_13",
        title = "The Loyalists",
        illustrationId = "after_the_ambush",
        narrativeText = """
            Selvane lays one sheet flat with two fingers. "My people came to your door and
            did not leave it," she says. "Some of them were mistaken about whose word they
            were keeping. They were still mine." Her eyes rest on Voss. "The Order lost
            people on that ground. I mean to know who answers for them."

            "I do," Voss says.

            "I thought you might." Selvane's voice does not change. "I would have thought
            less of you if you had said anything else, and I would have written it down."
            She turns the sheet face down, the way a door is closed. "Nobody else will say
            it for you, and not in this room." Then, to Kaelen: "You fought on that ground
            too. What do you say it cost?"
        """.trimIndent(),
        choices = hearingOnward("\"More than I wanted. Less than they came to take.\"")
    ),
    StoryNode(
        id = "c13_m_seal_spent",
        chapterId = "chapter_13",
        title = "The Tithe Arch",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Selvane sets a small slip of tallow-stained paper on the table. "The tithe arch
            at the Sanctum keeps a tally of what passes it," she says. "It recorded a seal
            of the Order, shown by a woman the Sanctum had a price on, beside a cart that
            carried no tithe." She does not look at Voss. "A seal is a thing the Order
            lends. It is not a key, whatever the clerks believe. You spent one on a grain
            cart."

            "It opened the arch," Voss says.

            "Once. A seal shown to a collector who knew the face above it is worth no more
            than a button, and you knew that." She turns the slip over. "I am not asking
            for it back. I am noting that it cannot be lent again." A pause, exactly long
            enough to be a question. "Whose idea was the cart?"
        """.trimIndent(),
        choices = hearingOnward("\"Mine. She was with me, and she paid for it.\"")
    ),
    StoryNode(
        id = "c13_m_seal_door",
        chapterId = "chapter_13",
        title = "The Mark in the Ward",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            Selvane folds her hands. "The cell wing at the Sanctum has a ward on its door
            that was made to remember the mark which opens it," she says. "Last season it
            remembered one that should not have been used on it." Her eyes go to Kaelen's
            hand, where the ring was, and come away again without remark. "The next
            Inquisitor to walk that corridor will be told whose mark it was. I cannot make
            a ward forget. That is the whole of a ward's use."

            "I know," Voss says.

            "I expect you did, when you gave it away." There is nothing in the sentence but
            fact. "Who held the ring when the door opened?"
        """.trimIndent(),
        choices = hearingOnward("\"I did. It was my hand on the ward.\"")
    ),
    StoryNode(
        id = "c13_m_greymoor_sealed",
        chapterId = "chapter_13",
        title = "A Stopgap",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            Selvane draws a thinner sheet from the stack. "Greymoor," she says. "A gate on
            the north road, failed and left. Then one night it was closed with cord and ash
            and whatever the village had to hand, by a man the Order has never licensed to
            close anything." She looks up. "It is holding. I did not expect it to hold."

            "It is a stopgap," Kaelen says. "I told Halvard so."

            "Most things that hold are." She sets the sheet down. "A seal made of
            understanding fails when the understanding does, and yours is borrowed. I will
            send masons in the spring, and you may tell the village so if you go north
            again." She waits a moment. "It will not be a kindness. It will be what is
            owed. Do you know the difference?"
        """.trimIndent(),
        choices = hearingOnward("\"I do. I paid for the first one with my hands.\"")
    ),
    StoryNode(
        id = "c13_m_greymoor_reported",
        chapterId = "chapter_13",
        title = "The Plain Account",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            Selvane draws three pages from the stack, folded small and softened by a long
            road. "This reached my table by way of a factor in a market town," she says,
            "who sold it to a clerk who knew what it was. What was seen, what is left, what
            is needed, in plain words and in order." She smooths the top page. "I have had
            reports of the north road all year, from men whose business it was to write
            them. Yours is the only one I did not need to send a rider to confirm."

            "It was only what I saw," Kaelen says.

            "That is all a report is. It is rarer than you think." Her mouth moves, not
            quite a smile. "It was also unlicensed. A report is the kind of trespass the
            Order can use. Greymoor will have hands by spring."
        """.trimIndent(),
        choices = hearingOnward("\"Then I did not waste the pages.\"")
    ),
    StoryNode(
        id = "c13_m_greymoor_left",
        chapterId = "chapter_13",
        title = "As Found",
        illustrationId = "greymoor_aftermath",
        narrativeText = """
            Selvane draws a thinner sheet from the stack. "Greymoor," she says. "A gate on
            the north road, failed years ago, and left as you found it, and the news
            carried home to one old man in a hold that was busy." There is no accusation in
            it. "I will not tell you it was wrong. The Order left a good many gates as it
            found them, for a good many years. It is a habit." She looks at him a moment
            longer than the sentence needs. "I would sooner you did not inherit it."

            Voss says nothing, and does not look at either of them.
        """.trimIndent(),
        choices = hearingOnward("\"I could not hold two gates at once.\"")
    ),
    StoryNode(
        id = "selvanes_verdict",
        chapterId = "chapter_13",
        title = "Selvane's Verdict",
        illustrationId = "selvanes_verdict",
        narrativeText = """
            Selvane is quiet long enough that the silence starts to feel like its own kind of
            verdict. "The Order doesn't reinstate deserters," she says finally. "Not officially.
            But the Order also doesn't send its remaining strength chasing a jailer who's twice
            now done our work for us where we couldn't." She looks past Voss, toward the black
            door itself. "Ilsevet's little accident at Ashfall wasn't as quiet as she'd have
            liked. We know what four settings nearly did. We are, for the moment, content to let
            you be the ones standing between six and seven."

            It isn't forgiveness. It isn't quite a truce, either. It's something colder and more
            useful than both, and Voss seems to know better than to ask for more.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let them ride out.", nextNodeId = "c13_parting")
        )
    ),
    StoryNode(
        id = "c13_parting",
        chapterId = "chapter_13",
        title = "At the Stirrup",
        illustrationId = "selvanes_verdict",
        narrativeText = """
            The delegation forms up on the ruined road without hurry. Selvane is the last
            to mount. She stops with one hand on the horse's neck and the reins slack in the
            other, and she does not seem to be in a hurry either. Behind Kaelen the black
            door is shut, and Voss is a few paces off with her arms folded, near enough to
            hear anything that is not said quietly.

            There is a question he has carried since the tunnel mouth. It cannot wait for
            a better day, because a better day will not come with Selvane standing in front
            of him. Ilsevet did not arrive from nowhere. Somebody trained her, and somebody
            sent her north, and the woman at the stirrup outranks every one of them.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Ask what Ilsevet was before she was the Marshal, where Voss can hear.",
                nextNodeId = "c13_parting_open",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C13_ASK_FAMILY,
                    setFlags = setOf("c13_asked_openly")
                )
            ),
            Choice(
                label = "Ask what Ilsevet was before she was the Marshal, quietly, once Voss has turned away.",
                nextNodeId = "c13_parting_low",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C13_ASK_FAMILY
                )
            )
        )
    ),
    StoryNode(
        id = "c13_parting_open",
        chapterId = "chapter_13",
        title = "At the Stirrup",
        illustrationId = "selvanes_verdict",
        narrativeText = """
            "Before." Selvane considers the word as though it were a coin she has not been
            offered in some time. "You will want a file, and a file is not mine to give at
            a stirrup. I will see what the archive holds that I am permitted to send. It
            will not be flattering to anyone, and that includes the Order." Her eyes pass
            over Kaelen's shoulder to Voss, and Voss looks back, and whatever is said in
            that look is not said in words. "You asked it where she could hear. Good. I
            would not trust an answer you had to hide. It will come to the jailer, and he
            may show it to whom he likes."

            She turns the horse. The riders turn with her.
        """.trimIndent(),
        choices = toTheNight("Go back up to the gate.")
    ),
    StoryNode(
        id = "c13_parting_low",
        chapterId = "chapter_13",
        title = "At the Stirrup",
        illustrationId = "selvanes_verdict",
        narrativeText = """
            "Before." Selvane considers the word as though it were a coin she has not been
            offered in some time. She has heard the question lowered, and she makes no
            remark on that. "You will want a file, and a file is not mine to give at a
            stirrup. I will see what the archive holds that I am permitted to send. It
            will not be flattering to anyone, and that includes the Order." Her voice
            drops to match his. "It will come to the jailer. To the jailer, and not to the
            Hold, and not to her. What he does with it is his."

            She turns the horse. The riders turn with her. When Kaelen looks round, Voss
            has found something to do with a strap, and she does not look up from it.
        """.trimIndent(),
        choices = toTheNight("Go back up to the gate.")
    ),
    StoryNode(
        id = "c13_night_wary",
        chapterId = "chapter_13",
        title = "After the Order",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Voss is at the black door when he comes up, with her back to the stone and her
            eyes on the road the delegation took, as if it might come back to correct
            itself. She has not taken the borrowed blade off. "She gave me what she was
            always going to give me," she says. "Time, and a reason to be careful of it."
            She does not sound grateful. "You spoke, or you did not speak, and either way
            you are on their list now, beside me. I did not ask for that."

            She lets the road have her attention a moment longer. "I will not pretend I am
            sorry you did it. I will ask you not to do it twice without telling me first."
        """.trimIndent(),
        choices = toTheEve(
            "Tell her you would do it again, and that you should have said so first.",
            Consequences(clearFlags = C7_RUNG_FAMILY, setFlags = setOf("c7_rung_working"))
        ) + toTheEve("Say that you did it for the Hold, and leave it there.")
    ),
    StoryNode(
        id = "c13_night_working",
        chapterId = "chapter_13",
        title = "After the Order",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Voss is sitting on the step below the black door with the borrowed blade
            across her knees, working oil into an edge that does not need it. "It is worth
            what she said it was worth," she says, without looking up. "No help, and no
            hunters, and nothing in writing. From her." She turns the blade over. "I
            thought I would feel as if I had been let off. I feel as if I have been told
            I am not required."

            The oil goes on in long strokes. "I have been required all my life. I do not
            know what to do with the other thing."
        """.trimIndent(),
        choices = toTheEve(
            "Tell her she is required here.",
            Consequences(clearFlags = C7_RUNG_FAMILY, setFlags = setOf("c7_rung_trusted"))
        ) + toTheEve("Tell her to rest, and think about it in the morning.")
    ),
    StoryNode(
        id = "c13_night_trusted",
        chapterId = "chapter_13",
        title = "After the Order",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Voss is already on the wall above the gate when he comes up, her hands flat on
            the cold stone. She does not turn. "You did not ask me why," she says. "In
            there. When she spoke of what I was, and what the Order would need before it
            forgave me anything. You did not ask me what I had done that made them want a
            reason."

            "It was not mine to ask," Kaelen says.

            "No." The wind takes a strand of her hair across her mouth and she lets it
            stay. "I braced for it the whole way through. I find I am glad it did not come."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Ask about the chapel in the ash.",
                nextNodeId = "c13_night_chapel",
                requirements = ChoiceRequirement(requiredFlags = setOf("c8_chapel_story"))
            )
        ) + toTheEve("Stand beside her on the wall until the watch changes.")
    ),
    StoryNode(
        id = "c13_night_chapel",
        chapterId = "chapter_13",
        title = "What Voss Would Not Say",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Voss is quiet long enough that he thinks the question has gone by. "I thought I
            would feel it today, with her standing there," she says. "I did not. I felt it
            a season ago, in the ash, at a door with a scratched mark on it, and I have been
            feeling it since." She looks at the road. "I had hoped it would still be
            standing. It was. That is what I can give you tonight."

            Her hand moves on the stone, the way it moved on the lintel. "Some things I
            would sooner leave where they stand. Thank you for standing there with me."
        """.trimIndent(),
        choices = toTheEve("Stand beside her on the wall until the watch changes.")
    ),
    StoryNode(
        id = "c13_eve_voss",
        chapterId = "chapter_13",
        title = "The Eve",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The dwarves finish the cutting on the evening of the third day, and the Hold
            goes quiet around the finished thing, the way a room does around a sleeper.
            Voss finds him at the gate. "I will take the watch tomorrow," she says, "so
            that you can stand at the front of it and not look behind you." She does not
            mention the morning the column came, or where he stood when it did. She does
            not need to. It sits between them like a debt that has been paid and is still,
            for the moment, warm. "Sleep if you can. Tomorrow is theirs, and then it is
            yours."
        """.trimIndent(),
        choices = toTheBook("Go on.")
    ),
    StoryNode(
        id = "c13_eve_stair",
        chapterId = "chapter_13",
        title = "The Eve",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The dwarves finish the cutting on the evening of the third day. As the dusk
            comes the people of the north stair climb to the gate with what they have: a
            loaf each, which is a great deal, and a handful of river stones carried from
            wherever they have been sleeping. The woman who speaks for them puts hers in
            Kaelen's hands first. "We said we would remember which," she says. "We have.
            The stones are for the mark, if the dwarves will have them. If they will not,
            they are for you." Behind her nobody is looking at him, which is how he knows
            it was agreed.
        """.trimIndent(),
        choices = toTheBook("Go on.")
    ),
    StoryNode(
        id = "c13_eve_forge",
        chapterId = "chapter_13",
        title = "The Eve",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The dwarves finish the cutting on the evening of the third day, and the old
            smith is waiting at the forge door as the dusk comes, with his palm flat on
            the cold anvil. "The forge was lit at first light after," he says, "and every
            day since. Somebody has to keep working what he left, and you were in the
            doorway when it needed saying." He says it as an entry in a ledger. "The mark
            is cut. Tomorrow the Hold will want to see you stand at it. We will see to the
            rest."
        """.trimIndent(),
        choices = toTheBook("Go on.")
    ),
    StoryNode(
        id = "c13_book_name",
        chapterId = "chapter_13",
        title = "The Gate-Book",
        illustrationId = "silent_forge",
        narrativeText = """
            The old smith has the gate-book open on his forearm, as he had it at the hasty
            earth, and he holds it out so that Kaelen can see the page. Kaelen's line is
            there, short: a name and the name of a gate. Beneath it, in a hand that has
            plainly never written a long sentence, the smiths have written the rest. A
            door opened to strangers. A wall leaned on through every night he could stand.
            A blade lent that he did not have to lend. "It is not in the custom to answer
            a keeper's line," the old smith says. "You wrote him short, and we thought
            the book should be balanced."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "halvards_rites"))
    ),
    StoryNode(
        id = "c13_book_deeds",
        chapterId = "chapter_13",
        title = "The Gate-Book",
        illustrationId = "silent_forge",
        narrativeText = """
            The old smith has the gate-book open on his forearm, as he had it at the hasty
            earth, and he holds it out so that Kaelen can see the page. Kaelen's account is
            there in his own hand: a door, a wall, a blade lent. Beneath it is a single
            line in another hand, written by someone who plainly does not write often, and
            smudged where a thumb has rested on it. It says that he held. "It is not in
            the custom to answer a keeper's line," the old smith says. "But you wrote what
            he did, and we thought somebody should write what he was. It turned out to be
            a short thing." He looks at the smudge. "The word was ours before it was
            yours."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "halvards_rites"))
    ),
    StoryNode(
        id = "c13_book_plain",
        chapterId = "chapter_13",
        title = "The Gate-Book",
        illustrationId = "silent_forge",
        narrativeText = """
            The old smith has the gate-book open on his forearm, as he had it at the hasty
            earth, and he holds it out so that Kaelen can see the page. Kaelen's line is
            there, one line: that he held. Beneath it is a row of short strokes in as many
            hands, one for each smith who stood in the doorway that morning, and a longer
            one that is the old smith's own. "It is not in the custom to answer a keeper's
            line," he says. "A stroke means that I was there, and I saw it as you wrote
            it." He closes the book on his forearm. "You will not need more than that
            tomorrow. Neither will we."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "halvards_rites"))
    ),
    StoryNode(
        id = "halvards_rites",
        chapterId = "chapter_13",
        title = "Halvard's Rites",
        illustrationId = "halvards_proper_rites",
        narrativeText = """
            With Selvane's riders gone and no new siege on the horizon, Stonebeard finally has
            room to grieve properly instead of just quickly. The dwarves who still call the hold
            home carve Halvard's name into the stone above the gate he died holding, not a grave
            marker, they explain, a warden's mark, the same honor given to five names Kaelen's
            never heard spoken and one, now, that he has.

            Kaelen adds his own words when it's his turn, and finds he doesn't have as many as he
            expected. Just: he held. That was always going to be enough, coming from Halvard, and
            saying more would only dress it up as something it doesn't need to be.
        """.trimIndent(),
        choices = afterTheCarving("Take a rubbing of the mark before you leave.")
    ),
    StoryNode(
        id = "c13_thessaly_attended",
        chapterId = "chapter_13",
        title = "A Visitor from Fenmoor",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            Thessaly has come up from the marsh for the carving, with two of Fenmoor's
            people at her back and the look of someone still learning to wear her own
            binding. She stood at the rear through the whole of it. When the
            dwarves were done she crossed the yard, laid two fingers on the new-cut stone as
            if testing it for damp, and took them away again. "I never met him," she tells
            Kaelen. "I want that said. But he sent you down a road that ended at my gate,
            whether he meant to or not." She looks at the mark. "I will stand where I am
            asked. I think I just did."

            She is gone before the Hold has finished arguing about its own kin, with the
            light still good enough to walk by.
        """.trimIndent(),
        choices = toTheDoor("Go on.")
    ),
    StoryNode(
        id = "c13_thessaly_pledged",
        chapterId = "chapter_13",
        title = "Word from Fenmoor",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            Thessaly does not come. Her answer comes in her place, on a marsh pony, with a
            rider who was plainly told to get it there before the carving and has managed
            it with half a day to spare: a coil of reed rope, which is what Fenmoor has
            that Stonebeard does not, "for whatever needs binding," and four lines in a
            hand that does not waste ink. Kaelen had promised to send whoever Stonebeard
            could spare. She writes that she has counted what Fenmoor can spare in return,
            that the number is small, and that it is coming anyway. The last line is for
            the stone, and the rider says it aloud, because he was told to. "Hold."
        """.trimIndent(),
        choices = toTheDoor("Go on.")
    ),
    StoryNode(
        id = "c13_thessaly_silent",
        chapterId = "chapter_13",
        title = "No Word from Fenmoor",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            No word comes from Fenmoor. Kaelen had not expected any, and finds he has been
            watching the road more than the stone. Thessaly asked him at Fenmoor what
            happens the third time, and he had no answer for her then. He has none
            now. The silence from the marsh is not unfriendly, he decides. It is the
            silence of someone who is waiting to be told. The rubbing is folded in his
            coat, against the chain, and he resolves to find an answer before the third
            time finds him.
        """.trimIndent(),
        choices = toTheDoor("Go on.")
    ),
    StoryNode(
        id = "c13_door",
        chapterId = "chapter_13",
        title = "The Door",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Tam has come up with Edda's people for the carving, and she has brought a lamp.
            "Kestrel's rule," she says, before anyone can ask. "A warden's door is never
            left dark." She sets it in the niche beside the new-cut mark, and one of the
            dwarves, a woman with a chisel still in her belt, moves a stone a finger's
            width to give it a better place. "I told you this was not a warden's door,"
            Tam says to Kaelen. "I am not sure any more."

            Edda waits until the lamp is lit. "A season ago the door was Halvard's," she
            says. "You carry his chain, so it is yours. The Hold has not told us to go and
            it has not told us to stay, and my people would sooner be told than guess." She
            brushes at flour on her knuckles that is not there. "Is the door open to us, or
            is it barred at night? We will keep to either."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep the door open to them, night and day.",
                nextNodeId = "c13_door_open_reply",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C13_DOOR_FAMILY,
                    setFlags = setOf("c13_door_open")
                )
            ),
            Choice(
                label = "Bar it at night, and tell them why.",
                nextNodeId = "c13_door_barred_reply",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C13_DOOR_FAMILY,
                    setFlags = setOf("c13_door_barred")
                )
            )
        )
    ),
    StoryNode(
        id = "c13_door_open_reply",
        chapterId = "chapter_13",
        title = "The Door",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            "Then I will tell them it is open," Edda says. She does not smile. "There will
            be nights you wish you had said the other thing. There are always nights." She
            nods once, to the lamp and not to him. "We will keep the stair clean and the
            water counted, as before. Tell the dwarves we will carry our share of the
            stone." Tam, behind her, turns the lamp a quarter turn so that it shines
            through the door and not at the wall.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "who_stands_with_stonebeard"))
    ),
    StoryNode(
        id = "c13_door_barred_reply",
        chapterId = "chapter_13",
        title = "The Door",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            "At night," Edda says. "And the reason, so I can say it the way you mean it."
            He gives it: that the wall leans, and he cannot watch the gap and the door in
            the same dark, and the gap is the one that kills people. She takes that in
            without comment. "A reason I can repeat," she says. "I will repeat it." It is
            a cool answer, and she does not pretend otherwise. Tam, behind her, moves the
            lamp to the outer side of the niche, where it will show anyone coming up to the
            door that somebody is awake.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "who_stands_with_stonebeard"))
    ),
    StoryNode(
        id = "who_stands_with_stonebeard",
        chapterId = "chapter_13",
        title = "Who Stands With Stonebeard",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            Word of Selvane's uneasy truce spreads faster than Kaelen expects. Within the week, a
            handful of Stonebeard's kin who never trained for anything but mining ask, awkwardly,
            whether a warden's chain only fits dwarven hands or whether Halvard just never got
            around to asking anyone else. Thessaly's answer, when word reaches Fenmoor, is
            blunter than the question deserved, and Kaelen finds he has to answer for
            Stonebeard's own kin himself.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Anyone willing to hold a gate can be taught to hold it well. Start training them.\"",
                nextNodeId = "chapter13_end_training",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 11)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("began_training_new_wardens")
                )
            ),
            Choice(
                label = "\"The chain isn't mine to hand out. But I'll stand watch beside anyone who picks it up.\"",
                nextNodeId = "chapter13_end_watch",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 10)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("stood_watch_rather_than_delegate")
                )
            ),
            Choice(
                label = "\"One gate, one jailer, for now. There will be time to decide the rest later.\"",
                nextNodeId = "chapter13_end_deferred",
                consequences = Consequences(setFlags = setOf("deferred_the_question"))
            )
        )
    ),
    StoryNode(
        id = "chapter13_end_training",
        chapterId = "chapter_13",
        title = "End of Chapter XIII — What the Order Allows",
        illustrationId = "chapter13_threshold",
        narrativeText = """
            Life at Stonebeard doesn't return to normal so much as it settles into a new, wearier
            shape, grief worn in like a groove in stone, work still needing doing regardless.
            Three of Stonebeard's kin start drilling with Kaelen most mornings now, clumsy with a
            spear but stubborn about it in exactly the way that makes a warden eventually, and
            their mistakes are the first thing in weeks that's made him laugh instead of ache.

            That night, for the first time since the binding rite, the thing behind the black
            door speaks again, not asking for anything this time. Just one sentence, pressed
            against the wards like a held breath finally let out: "Four settings. It felt four
            settings, and it isn't finished waiting for the rest."

            Chapter XIV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen.",
                nextNodeId = "c14_gate_training",
                consequences = Consequences(clearFlags = C14_TELLING_FLAGS)
            )
        )
    ),
    StoryNode(
        id = "chapter13_end_watch",
        chapterId = "chapter_13",
        title = "End of Chapter XIII — What the Order Allows",
        illustrationId = "chapter13_threshold",
        narrativeText = """
            Life at Stonebeard doesn't return to normal so much as it settles into a new, wearier
            shape, grief worn in like a groove in stone, work still needing doing regardless.
            Kaelen finds himself standing watch at the gate more nights than he needs to, and
            more than a few of Stonebeard's kin start finding reasons to stand there with him,
            chain or no chain, learning the shape of the door by simply keeping it company.

            That night, for the first time since the binding rite, the thing behind the black
            door speaks again, not asking for anything this time. Just one sentence, pressed
            against the wards like a held breath finally let out: "Four settings. It felt four
            settings, and it isn't finished waiting for the rest."

            Chapter XIV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen.",
                nextNodeId = "c14_gate_watch",
                consequences = Consequences(clearFlags = C14_TELLING_FLAGS)
            )
        )
    ),
    StoryNode(
        id = "chapter13_end_deferred",
        chapterId = "chapter_13",
        title = "End of Chapter XIII — What the Order Allows",
        illustrationId = "chapter13_threshold",
        narrativeText = """
            Life at Stonebeard doesn't return to normal so much as it settles into a new, wearier
            shape, grief worn in like a groove in stone, work still needing doing regardless.
            Kaelen finds himself standing watch at the gate more nights than he needs to, chain
            and brand both aching faintly in a way neither used to, the question of who else
            might someday share that watch left open for a later day that hasn't arrived yet.

            That night, for the first time since the binding rite, the thing behind the black
            door speaks again, not asking for anything this time. Just one sentence, pressed
            against the wards like a held breath finally let out: "Four settings. It felt four
            settings, and it isn't finished waiting for the rest."

            Chapter XIV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Listen.",
                nextNodeId = "c14_gate_deferred",
                consequences = Consequences(clearFlags = C14_TELLING_FLAGS)
            )
        )
    )
)

val chapter13 = com.thelastjailer.app.Chapter(
    id = "chapter_13",
    number = 13,
    title = "Chapter XIII — What the Order Allows",
    startNodeId = "the_orders_delegation_guarded"
)
