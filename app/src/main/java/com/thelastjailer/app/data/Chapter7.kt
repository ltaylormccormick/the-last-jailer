package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C7a: what this telling did about Kestrel's letter and the hand it had kept shut. Every choice that
 * sets one clears the rest, so a replay that answers differently never carries the earlier answer.
 */
val C7_FORGE_FAMILY: Set<String> =
    setOf("c7_traded", "c7_shut", "c7_page", "c7_comfort", "c7_quiet")

/** Family C7b: what Halvard said to the promise Kaelen made to Edda, or that he was never told. */
val C7_HOLD_FAMILY: Set<String> =
    setOf("c7_hold_terms", "c7_hold_refused", "c7_hold_hidden")

/**
 * Family C7c: how Kaelen answered Voss's account. The five shipped stances and the question about the riders
 * are alternatives; each clears the rest. Chapters XII and XVIII read `trusted_voss_fully`, so a replay must not
 * carry it into a telling that did not give it.
 */
val C7_STANCE_FAMILY: Set<String> = setOf(
    "trusted_voss_fully", "stayed_guarded", "held_voss_accountable", "noted_hearing_the_offer",
    "already_suspected_ilsevet", "c7_asked_about_riders"
)

/** Family C7d: how far Voss and Kaelen have got. A ladder of exclusive flags; a move clears the three and sets one. */
val C7_RUNG_FAMILY: Set<String> = setOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")

/** Family C7e: where the ambush was fought. */
val C7_GROUND_FAMILY: Set<String> = setOf("c7_ground_yard", "c7_ground_stair", "c7_ground_road")

/** Family C7f: what Halvard made of Voss at the hearth. */
val C7_VERDICT_FAMILY: Set<String> = setOf("c7_halvard_trusts_voss", "c7_halvard_doubts_voss")

/** Family C7g: how Kaelen left for Ashfall (the three flags Chapter VII already set). */
val C7_DEPARTURE_FAMILY: Set<String> = setOf("chose_ashfall_now", "chose_to_warn_halvard", "asked_for_time")

/**
 * Everything Chapter VII records that is not another chapter's: the chapter's own flags, the shipped ones it
 * set before, and the defeat marker. The chapter's first choices clear it all, so a replay reaches Voss with an
 * empty slate. Chapter VI's end is not edited beyond where it leads; the clear happens here.
 */
val C7_TELLING_FLAGS: Set<String> = setOf(
    "pressed_voss", "welcomed_voss", "lost_to_loyalist_enforcer",
    "c7_halvard_open", "c7_voss_called_them_off", "c7_voss_below", "c7_voss_hurt_deeper"
) + C7_FORGE_FAMILY + C7_HOLD_FAMILY + C7_STANCE_FAMILY + C7_RUNG_FAMILY + C7_GROUND_FAMILY +
    C7_VERDICT_FAMILY + C7_DEPARTURE_FAMILY

/** Where the pair of "go on" choices lead from the end of the forge talk: Edda's promise if there is one, else the tree. */
private fun afterForge(label: String, whenPromised: String, consequences: Consequences = Consequences()): List<Choice> = listOf(
    Choice(
        label = whenPromised,
        nextNodeId = "c7_edda_promise",
        requirements = ChoiceRequirement(requiredFlags = setOf("c6_offered_the_hold")),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "voss_returns_alone",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_offered_the_hold")),
        consequences = consequences
    )
)

internal val rungOrder = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")

/**
 * One decision that moves Voss's rung by [delta] (clamped at both ends). It is written once per rung that can exist at
 * this point ([possible] counts them from the lowest), and exactly one is ever visible: the lowest rung also covers a save
 * that holds no rung at all, so an older save is never stuck.
 */
internal fun rungChoices(
    label: String,
    next: String,
    delta: Int,
    base: Consequences = Consequences(),
    possible: Int = rungOrder.size
): List<Choice> =
    rungOrder.take(possible).mapIndexed { i, _ ->
        val target = rungOrder[(i + delta).coerceIn(0, rungOrder.size - 1)]
        val requirement = when (i) {
            0 -> ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))
            else -> ChoiceRequirement(requiredFlags = setOf(rungOrder[i]))
        }
        Choice(
            label = label,
            nextNodeId = next,
            requirements = requirement,
            consequences = base.copy(
                clearFlags = base.clearFlags + C7_RUNG_FAMILY,
                setFlags = base.setFlags + target
            )
        )
    }

/** Halvard's word on the stair, chosen by what he made of Voss at the hearth. The second also covers a save that holds neither. */
private fun verdictChoices(): List<Choice> = listOf(
    Choice(
        label = "Wait for dawn.",
        nextNodeId = "c7_verdict_trusts",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_halvard_trusts_voss"))
    ),
    Choice(
        label = "Wait for dawn.",
        nextNodeId = "c7_verdict_doubts",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_halvard_trusts_voss"))
    )
)

/**
 * The shipped answers to Voss's account, in the order they were shipped, plus the question about the riders. Every answer
 * clears the stance family and sets its own flag and the rung it starts Voss on; the labels, targets, gates and stat
 * changes are exactly as shipped.
 */
private fun vossStanceChoices(): List<Choice> = listOf(
    Choice(
        label = "\"I believe you. What do you need?\"",
        nextNodeId = "the_price_of_trust_trusted",
        consequences = Consequences(
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("trusted_voss_fully", "c7_rung_working")
        )
    ),
    Choice(
        label = "\"I'll hear you out. That's not the same as trust.\"",
        nextNodeId = "the_price_of_trust_guarded",
        consequences = Consequences(
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("stayed_guarded", "c7_rung_wary")
        )
    ),
    Choice(
        label = "\"You let your own Order send four riders to bury this door. Trust has to run both ways.\"",
        nextNodeId = "the_price_of_trust_accountable",
        requirements = ChoiceRequirement(requiredFlags = setOf("refused_envoy")),
        consequences = Consequences(
            statDeltas = mapOf(StatType.COURAGE to 1),
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("held_voss_accountable", "c7_rung_wary")
        )
    ),
    Choice(
        label = "\"An envoy already made me that same offer. I heard him out, too, and still said no in the end.\"",
        nextNodeId = "the_price_of_trust_wary",
        requirements = ChoiceRequirement(requiredFlags = setOf("heard_envoy_offer")),
        consequences = Consequences(
            statDeltas = mapOf(StatType.HONOUR to 1),
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("noted_hearing_the_offer", "c7_rung_working")
        )
    ),
    Choice(
        label = "\"The thing behind that gate already warned me someone like her would come asking. You're not telling me anything I didn't already suspect.\"",
        nextNodeId = "the_price_of_trust_unsurprised",
        requirements = ChoiceRequirement(requiredFlags = setOf("confronted_envoy_with_prisoners_words")),
        consequences = Consequences(
            statDeltas = mapOf(StatType.COURAGE to 1),
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("already_suspected_ilsevet", "c7_rung_working")
        )
    ),
    Choice(
        label = "\"The riders I told you about on the road. Did you ever run them down?\"",
        nextNodeId = "c7_riders_followed_up",
        requirements = ChoiceRequirement(requiredFlags = setOf("told_voss_of_the_riders")),
        consequences = Consequences(
            clearFlags = C7_STANCE_FAMILY + C7_RUNG_FAMILY,
            setFlags = setOf("c7_asked_about_riders", "c7_rung_working")
        )
    )
)

/**
 * Chapter VII — An Uneasy Alliance.
 *
 * Turns Voss from antagonist into a conflicted, unreliable ally: she returns alone, admits the Chapter V envoy
 * was Ilsevet's rogue operation rather than sanctioned Order business, and offers intelligence in exchange for
 * Kaelen's trust. How much trust he extends sets where Voss starts on a ladder of exclusive flags, and what he does
 * for and with her afterwards moves her up or down it.
 *
 * The chapter now opens with Halvard, at the forge, the night Kaelen comes back from Greymoor: the hand Kaelen kept
 * shut at the end of Chapter V, the trade or the confidence Halvard offered on the way out in Chapter VI, Kestrel's
 * letter, and the promise Kaelen made to Edda in his own name. Then Voss arrives; the shared ambush is fought on ground
 * Kaelen picks (or ended by Voss at the cost of her standing, if the ladder allows); the signet is given in every
 * telling; and Kaelen decides whether to bring Voss below, to Halvard's fire, or to keep the matter between the two of
 * them. Either way it ends on a destination, the Ashfall Sanctum, for Chapter VIII.
 */
val chapter7Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c7_forge_night",
        chapterId = "chapter_7",
        title = "The Dead Forge",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard doesn't go to bed. When Kaelen comes up from the lower passage he finds the
            old warden sitting at the dead forge with the chest by his boot lifted open and
            nothing taken out of it, the way a man sits beside a thing he has decided to look at
            later. The gate below is quiet. In this hold that is a kind of noise.

            "Sit," Halvard says. "It is easier to say things to a fire, even a cold one."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Sit.",
                nextNodeId = "c7_forge_trade",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_halvard_reticent")),
                consequences = Consequences(clearFlags = C7_TELLING_FLAGS)
            ),
            Choice(
                label = "Sit.",
                nextNodeId = "c7_forge_hand",
                requirements = ChoiceRequirement(
                    requiredFlags = setOf("kept_the_offer_from_halvard"),
                    forbiddenFlags = setOf("c6_halvard_reticent")
                ),
                consequences = Consequences(clearFlags = C7_TELLING_FLAGS)
            ),
            Choice(
                label = "Sit.",
                nextNodeId = "c7_forge_letter",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c6_halvard_reticent", "kept_the_offer_from_halvard")),
                consequences = Consequences(clearFlags = C7_TELLING_FLAGS)
            )
        )
    ),
    StoryNode(
        id = "c7_forge_trade",
        chapterId = "chapter_7",
        title = "A Hand Held Shut",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard does not look at the chest. "I said I would trade when you wished to," he
            says, "and you have the look of a man who has been carrying the wish a while without
            taking it out." He turns an invisible coal with the toe of his boot. "At the black
            door an envoy put an offer to you. You gave me the page and kept what you made of the
            rest. I have not asked since. I am asking now. In return I will tell you what I did
            not tell you about Greymoor."

            It is a fair trade, plainly said. That does not make it a small one.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Open your hand. Tell him what the offer was aimed at, and what you did with it.",
                nextNodeId = "c7_trade_made",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_FORGE_FAMILY,
                    setFlags = setOf("c7_traded", "c7_halvard_open")
                )
            ),
            Choice(
                label = "Keep your hand shut a while longer.",
                nextNodeId = "c7_trade_refused",
                consequences = Consequences(clearFlags = C7_FORGE_FAMILY, setFlags = setOf("c7_shut"))
            )
        )
    ),
    StoryNode(
        id = "c7_forge_hand",
        chapterId = "chapter_7",
        title = "What Was Kept",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard waits until what he has to say has burned down to something he can hold. "You
            came back from Greymoor with a dead warden's name and a good report," he says. "You
            also came back with a hand still shut around whatever the envoy said to you at the
            door. I think the first has made the second heavier." He does not ask for a trade.
            "If you want to set it down, I will hold it. If you don't, I will not call it a lie."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Set it down. Tell him what the offer was aimed at, and what you did with it.",
                nextNodeId = "c7_trade_made",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_FORGE_FAMILY,
                    setFlags = setOf("c7_traded", "c7_halvard_open")
                )
            ),
            Choice(
                label = "Keep it a while longer.",
                nextNodeId = "c7_trade_refused",
                consequences = Consequences(clearFlags = C7_FORGE_FAMILY, setFlags = setOf("c7_shut"))
            )
        )
    ),
    StoryNode(
        id = "c7_trade_made",
        chapterId = "chapter_7",
        title = "What the Fire Heard",
        illustrationId = "silent_forge",
        narrativeText = """
            It takes less time to say than he feared and longer than he wanted. Halvard listens
            with his eyes on the dead forge, and when it is done he does not say that Kaelen
            should have told him sooner. "They offered you what you lack," he says instead, and
            lets it sit. "A hand to lean on. They are not wrong that it is a want."

            Then he pays it back. The letter comes out of the chest at last, a single sheet gone
            soft at its folds, and he reads it flat, the way a man reads a sentence he has
            already served. "Kestrel of Greymoor to the warden at Stonebeard. The stones hold.
            The miller owes me flour and a debt of manners. The work wants two pairs of hands and
            I have one. If you cannot send a pair, send a word." He folds it along its old lines.
            "I sent neither. I told myself the gate needed me. It did. That is the part I am not
            proud of: that it was true, and I used it anyway."
        """.trimIndent(),
        choices = afterForge(
            label = "Go up to the tree before the rain.",
            whenPromised = "Stay a little longer."
        )
    ),
    StoryNode(
        id = "c7_trade_refused",
        chapterId = "chapter_7",
        title = "Two Shut Hands",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard nods slowly, as if Kaelen had answered a question he had put to himself.
            "Then we both carry ours," he says. "I will not call that dishonest. I did the same
            for nine winters." He lowers the lid of the chest and sits with his palm flat on it.
            "Mine will keep. So will yours, I think, until it doesn't."

            He says nothing more about Greymoor. The silence has a shape, and Kaelen finds that
            he is standing inside it.
        """.trimIndent(),
        choices = afterForge(
            label = "Go up to the tree before the rain.",
            whenPromised = "Stay a little longer."
        )
    ),
    StoryNode(
        id = "c7_forge_letter",
        chapterId = "chapter_7",
        title = "A Letter Nine Winters Old",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard takes the sheet from the chest and holds it in both hands for a while before
            he reads it, the way a man tests a plank before he puts his weight on it. "You went
            to see her," he says. "Whatever I told you before you left, I owe you the rest."

            He reads it flat. "Kestrel of Greymoor to the warden at Stonebeard. The stones hold.
            The miller owes me flour and a debt of manners. The work wants two pairs of hands and
            I have one. If you cannot send a pair, send a word." The sheet goes back along its
            folds. "I sent neither. I told myself the gate needed me. It did. That is the part I
            am not proud of: that it was true, and I used it anyway."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Give him the page from the cairn.",
                nextNodeId = "c7_letter_page",
                requirements = ChoiceRequirement(requiredFlags = setOf("c6_took_cairn_note")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_FORGE_FAMILY,
                    setFlags = setOf("c7_page", "c7_halvard_open")
                )
            ),
            Choice(
                label = "Tell him it was never his to carry alone.",
                nextNodeId = "c7_letter_comfort",
                consequences = Consequences(clearFlags = C7_FORGE_FAMILY, setFlags = setOf("c7_comfort", "c7_halvard_open"))
            ),
            Choice(
                label = "Say nothing. Let him have the quiet.",
                nextNodeId = "c7_letter_quiet",
                consequences = Consequences(clearFlags = C7_FORGE_FAMILY, setFlags = setOf("c7_quiet"))
            )
        )
    ),
    StoryNode(
        id = "c7_letter_page",
        chapterId = "chapter_7",
        title = "What She Left at the Fork",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard reads the page twice, the second time with his lips moving, and when he reaches
            the last line he stops. "I hope someone wrote to you first," he says aloud, and for a
            while that is all he says. When he looks up, the guard has gone out of his face and
            something older is in its place. "She did not blame me. She said so, to a pile of
            stones, in case I came by." He folds the page smaller than it needs to be. "I would
            have preferred to be blamed."

            He puts it in the chest with the letter, the two sheets lying together at last, nine
            winters apart.
        """.trimIndent(),
        choices = afterForge(
            label = "Go up to the tree before the rain.",
            whenPromised = "Stay a little longer."
        )
    ),
    StoryNode(
        id = "c7_letter_comfort",
        chapterId = "chapter_7",
        title = "Not His to Carry Alone",
        illustrationId = "silent_forge",
        narrativeText = """
            "That is the kind of thing one says," Halvard answers, but he does not say it
            unkindly, and he does not say it as though it had missed. "She had a gate and one
            pair of hands. I had a gate and a chest. Two wardens, one problem, and nine winters
            of nothing between them. Neither of us carried it alone, and both of us did." He
            turns the letter over and over. "Thank you for saying it anyway."
        """.trimIndent(),
        choices = afterForge(
            label = "Go up to the tree before the rain.",
            whenPromised = "Stay a little longer."
        )
    ),
    StoryNode(
        id = "c7_letter_quiet",
        chapterId = "chapter_7",
        title = "The Quiet Between Them",
        illustrationId = "silent_forge",
        narrativeText = """
            Kaelen says nothing, and Halvard seems to find that easier to bear than comfort. The
            letter goes back into the chest. "She asked for a word," Halvard says after a while,
            to nobody. "A word is such a small thing. I have thought about it more often than I
            have thought about almost anything."
        """.trimIndent(),
        choices = afterForge(
            label = "Go up to the tree before the rain.",
            whenPromised = "Stay a little longer."
        )
    ),
    StoryNode(
        id = "c7_edda_promise",
        chapterId = "chapter_7",
        title = "The Shut Hand, Again",
        illustrationId = "silent_forge",
        narrativeText = """
            There is one more thing, and it has been sitting in Kaelen's coat since the tollhouse.
            On the north road he told a woman with flour in the creases of her knuckles that if
            her people had nowhere else, he would answer to Halvard for them. He said it to Edda.
            He did not say it to the man whose door it is.

            "You have the look again," Halvard says, not unkindly. "The shut hand. What is it this
            time?"
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Tell him plainly what you promised, and ask for the north stair.",
                nextNodeId = "c7_hold_terms",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_halvard_open")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_HOLD_FAMILY,
                    setFlags = setOf("c7_hold_terms")
                )
            ),
            Choice(
                label = "Tell him plainly what you promised, and ask for the north stair.",
                nextNodeId = "c7_hold_refused",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_halvard_open")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_HOLD_FAMILY,
                    setFlags = setOf("c7_hold_refused")
                )
            ),
            Choice(
                label = "Say nothing. If they come, you will deal with it then.",
                nextNodeId = "c7_hold_hidden",
                consequences = Consequences(clearFlags = C7_HOLD_FAMILY, setFlags = setOf("c7_hold_hidden"))
            )
        )
    ),
    StoryNode(
        id = "c7_hold_terms",
        chapterId = "chapter_7",
        title = "Terms",
        illustrationId = "silent_forge",
        narrativeText = """
            Halvard is quiet long enough that the dead forge seems to listen with him. "That was
            my door you spoke for," he says, "and it was never yours to spend." Then, drily: "I am
            not angry. I am a little tired of how often I am asked to be."

            He gets up, and the chain at his hip knocks the anvil with a small bright sound. "The
            north stair is dry, and nobody has slept on it in my time. Thirty, no more. They will
            work. They will keep to the stair and out of the forge. And when it goes wrong, which
            it will, you will be the one who stands between them and me. That is the price of
            speaking for a door that is not yours."

            It is not a yes. It is a yes with a bill attached, and Kaelen takes it.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go up to the tree before the rain.", nextNodeId = "voss_returns_alone")
        )
    ),
    StoryNode(
        id = "c7_hold_refused",
        chapterId = "chapter_7",
        title = "The Answer Was No",
        illustrationId = "silent_forge",
        narrativeText = """
            "No," Halvard says. He says it gently, which is worse. "That was my door you spoke for,
            and you did not ask me first, and I find I cannot make it mine after the fact. There
            is a gate under us that wants every hand and every loaf, and I will not make this hold
            a refuge on a stranger's word, least of all one I never gave."

            He sees Kaelen's face and softens by a degree. "Tell her the river farms. I will send
            grain, but I will not send a door. The promise was always yours to keep or to break.
            It was never mine to be spent."

            Kaelen sits with the sound of it. Somewhere south a woman is waiting on a word that he
            can no longer carry the way he said it.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go up to the tree before the rain.", nextNodeId = "voss_returns_alone")
        )
    ),
    StoryNode(
        id = "c7_hold_hidden",
        chapterId = "chapter_7",
        title = "Another Shut Hand",
        illustrationId = "silent_forge",
        narrativeText = """
            Kaelen says nothing, and Halvard watches the shut hand and lets it be. "Well," he says.
            "Everyone keeps something." He turns back to the dead forge, and the quiet that settles
            is not the same as the quiet before.

            Somewhere south, on a road Kaelen has already walked once, a woman is waiting on a word
            that has not been spent and has not been kept. It sits in his coat beside the rest.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go up to the tree before the rain.", nextNodeId = "voss_returns_alone")
        )
    ),
    StoryNode(
        id = "voss_returns_alone",
        chapterId = "chapter_7",
        title = "Voss Returns Alone",
        illustrationId = "voss_returns_alone",
        narrativeText = """
            No column this time. No banner, not even the four riders from her first visit. Just
            Voss, on foot, favoring one side, stopping well short of the tree the way she never
            has before, close enough now that Kaelen can see exactly how little sleep has done
            for her since he last saw her ride away.

            "I'm not here as the Order," she says, before Kaelen can ask. "I'm not entirely sure
            I'm here as anything, anymore. I need you to hear something, and I need you to
            believe I'm not the one who sent that envoy to your door."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Why should I believe a word of it?\"",
                nextNodeId = "what_voss_knows_pressed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = setOf("pressed_voss", "welcomed_voss"),
                    setFlags = setOf("pressed_voss")
                )
            ),
            Choice(
                label = "\"Come in out of the rain, then. Talk.\"",
                nextNodeId = "what_voss_knows_welcomed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = setOf("pressed_voss", "welcomed_voss"),
                    setFlags = setOf("welcomed_voss")
                )
            )
        )
    ),
    StoryNode(
        id = "what_voss_knows_pressed",
        chapterId = "chapter_7",
        title = "What Voss Knows",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            "You shouldn't, not yet," Voss says, and something in her voice suggests she respects
            the question more than a warmer welcome would have earned. "So don't believe me. Just
            listen, and decide after."

            "The envoy who came here wasn't Order business," she says. "He was Ilsevet's, and
            Ilsevet answers to nobody's chain of command but her own these days, not really. She
            split from the Order's actual leadership months ago, over exactly what you'd guess:
            whether to bury what's behind these gates or make use of it. What happened here, the
            ambush, the ring on that adept's hand: that's the first proof I've had that isn't
            rumor."

            She names a place Kaelen has never heard of, a sanctum in the Ashfall reaches, with
            three of Ilsevet's targeted gates within a hard week's ride of it. "I think that's
            where she's building toward. I think you're the only person outside her own people
            who might actually believe me, and I understand if that's not saying much right now."
        """.trimIndent(),
        choices = vossStanceChoices()
    ),
    StoryNode(
        id = "what_voss_knows_welcomed",
        chapterId = "chapter_7",
        title = "What Voss Knows",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            Something in Voss's shoulders eases, just slightly, at the offer of a roof instead of
            a reckoning. "You didn't have to do that," she says, quieter than her usual register.
            "I wasn't sure you would."

            "The envoy who came here wasn't Order business," she says. "He was Ilsevet's, and
            Ilsevet answers to nobody's chain of command but her own these days, not really. She
            split from the Order's actual leadership months ago, over exactly what you'd guess:
            whether to bury what's behind these gates or make use of it. What happened here, the
            ambush, the ring on that adept's hand: that's the first proof I've had that isn't
            rumor."

            She names a place Kaelen has never heard of, a sanctum in the Ashfall reaches, with
            three of Ilsevet's targeted gates within a hard week's ride of it. "I think that's
            where she's building toward. I think you're the only person outside her own people
            who might actually believe me." She says it like she still isn't quite sure she
            believes her own luck.
        """.trimIndent(),
        choices = vossStanceChoices()
    ),
    StoryNode(
        id = "the_price_of_trust_trusted",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat, something in her expression
            easing toward relief for just a moment before instinct overrides it. She's moving
            before Kaelen registers why, pulling him sideways as an arrow takes the space where
            his shoulder was a heartbeat ago.

            "They followed me," she says, blade already out, no surprise left in her voice at
            all, only something that might be regret for dragging him into it regardless. "Of
            course they followed me."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "the_price_of_trust_guarded",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat, and if Kaelen's caution stung
            her at all, there's no time left to show it. She's moving before he registers why,
            pulling him sideways as an arrow takes the space where his shoulder was a heartbeat
            ago.

            "They followed me," she says, blade already out, no surprise left in her voice at
            all. "Of course they followed me. Believe me now or don't. It's the same fight
            either way."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "the_price_of_trust_accountable",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat, and for a moment something
            flashes across her face that might have been an answer to what Kaelen said, if
            there'd been time to give it. There isn't. She's moving before he registers why,
            pulling him sideways as an arrow takes the space where his shoulder was a heartbeat
            ago.

            "They followed me," she says, blade already out, no surprise left in her voice at
            all. "You want accountability? Start here. This is what it costs, both ways."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "the_price_of_trust_wary",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat, something almost like relief
            flickering across her face at not having to talk him all the way around to this a
            second time. There isn't time to dwell on that. She's moving before Kaelen registers
            why, pulling him sideways as an arrow takes the space where his shoulder was a
            heartbeat ago.

            "They followed me," she says, blade already out, no surprise left in her voice at
            all. "Good thing you already know how this part goes."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "the_price_of_trust_unsurprised",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat, and for a moment she looks less
            surprised by his suspicion than relieved not to have to be the one breaking it to him
            fresh. There's no time to dwell on that either. She's moving before Kaelen registers
            why, pulling him sideways as an arrow takes the space where his shoulder was a
            heartbeat ago.

            "They followed me," she says, blade already out, no surprise left in her voice at
            all. "Seems like you were ahead of both of us on this one."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "c7_riders_followed_up",
        chapterId = "chapter_7",
        title = "What Voss Did With the Riders",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Voss almost smiles, the first thing like it Kaelen has seen on her since the black door.
            "I did," she says. "It took a month, and it cost me a friend who owed me nothing. The
            riders were hired in halves, as you said. The second half was paid in grey coin, and
            the clerk who remembered the coin was dead before I could ask him a second question."
            She turns her good hand over as though the coin were still in it. "That is when I stopped
            telling myself it was a quarrel inside the house."

            She stops. The look on her face changes in the space of a breath, and she is moving
            before Kaelen registers why, pulling him sideways as an arrow takes the space where his
            shoulder was.

            "They followed me," she says, blade already out. "Of course they followed me."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "c7_the_ground")
        )
    ),
    StoryNode(
        id = "c7_the_ground",
        chapterId = "chapter_7",
        title = "Where to Stand",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            The second arrow is already in the air by the time Kaelen understands what Voss meant
            by followed. It is not one watcher but a line of them, fanned out along the tree line,
            patient in the way of people who were told where to wait. Behind him are the black door
            and the stair beneath it. Ahead is the ruined road they both came in on.

            "Choose," Voss says. She is breathing through her teeth. "Quickly. I know three places
            to fight here and I do not like any of them."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Hold the yard before the gate, where the ground is open and the walls are at your back.",
                nextNodeId = "loyalists_move_in",
                consequences = Consequences(clearFlags = C7_GROUND_FAMILY, setFlags = setOf("c7_ground_yard"))
            ),
            Choice(
                label = "Fall back to the narrow stair and make them come one at a time.",
                nextNodeId = "loyalists_move_in",
                consequences = Consequences(clearFlags = C7_GROUND_FAMILY, setFlags = setOf("c7_ground_stair"))
            ),
            Choice(
                label = "Go out and meet them on the road, away from the Hold.",
                nextNodeId = "loyalists_move_in",
                consequences = Consequences(clearFlags = C7_GROUND_FAMILY, setFlags = setOf("c7_ground_road"))
            ),
            Choice(
                label = "Tell Voss to speak to them. They are her people.",
                nextNodeId = "c7_called_off",
                requirements = ChoiceRequirement(
                    requiredFlags = setOf("c7_rung_working"),
                    minStats = mapOf(StatType.HONOUR to 4)
                ),
                consequences = Consequences(setFlags = setOf("c7_voss_called_them_off"), unlockTrophy = "Called Off")
            )
        )
    ),
    StoryNode(
        id = "c7_called_off",
        chapterId = "chapter_7",
        title = "Called Off",
        illustrationId = "after_the_ambush",
        narrativeText = """
            Voss steps out from behind him with her hands open and her colours stripped, and says
            three words in the Order's own cadence, flat and carrying, the voice of someone giving
            an order she no longer has the right to give. For a long breath nobody moves. Then the
            first bow comes down, and the second, and the man with her own name crossed through on
            his skin looks at her as though she has done something worse than strike him.

            They go. They go quickly and without looking back, which is how Kaelen knows what they
            will do with what they saw.

            "That will be known by morning," Voss says, very quietly. "Not as a rumour. As a fact. I
            am going to be hunted properly now." She holds out the signet, and her hand is steadier
            than her voice. "Take this before I think better of it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the signet. Bring her below, to Halvard's fire.",
                nextNodeId = "c7_hold_hearth",
                consequences = Consequences(
                    grantItemIds = listOf("voss_seal"),
                    setFlags = setOf("earned_voss_seal", "c7_voss_below")
                )
            ),
            Choice(
                label = "Take the signet. Keep this between the two of you.",
                nextNodeId = "what_voss_offers",
                consequences = Consequences(
                    grantItemIds = listOf("voss_seal"),
                    setFlags = setOf("earned_voss_seal")
                )
            )
        )
    ),
    StoryNode(
        id = "c7_cost_yard",
        chapterId = "chapter_7",
        title = "The Forge Shed",
        illustrationId = "after_the_ambush",
        narrativeText = """
            The yard held, and it paid for holding. A torch thrown in the first minute caught the
            forge shed's roof before anyone could reach it, and what is left is a black skeleton of
            beams still giving off heat. Halvard's tongs went with it, and the charcoal, and the
            whetstones he has used for longer than Kaelen has been alive. Anything made at that
            forge in the next month will have to be made somewhere else.

            "Better the shed than the stair," Voss says, to the smoke.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Bring her below, to Halvard's fire.",
                nextNodeId = "c7_hold_hearth",
                consequences = Consequences(setFlags = setOf("c7_voss_below"))
            ),
            Choice(label = "Keep this between the two of you.", nextNodeId = "what_voss_offers")
        )
    ),
    StoryNode(
        id = "c7_cost_stair",
        chapterId = "chapter_7",
        title = "What the Stair Showed",
        illustrationId = "after_the_ambush",
        narrativeText = """
            The stair held and nothing burned. The armoury behind it never saw a blade. But the
            ones who ran saw where the stair goes, and where a door stands that the Order's riders
            have spent months failing to find. Whatever else Ilsevet's people take away from tonight,
            they take away that.

            Voss reads his face. "I know," she says. "I would have chosen it too. That does not make
            it free."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Bring her below, to Halvard's fire.",
                nextNodeId = "c7_hold_hearth",
                consequences = Consequences(setFlags = setOf("c7_voss_below"))
            ),
            Choice(label = "Keep this between the two of you.", nextNodeId = "what_voss_offers")
        )
    ),
    StoryNode(
        id = "c7_cost_road",
        chapterId = "chapter_7",
        title = "Word Carried",
        illustrationId = "after_the_ambush",
        narrativeText = """
            The road was a bad place to fight and a good place to be seen losing from. The Hold was
            never touched. But the ones who ran did not run into nowhere, and by tomorrow Ilsevet
            will know two things she did not know this morning: that Voss is alive, and that she
            stands beside the jailer.

            "They were always going to find out," Voss says. "I would rather it be on my terms than
            on a rumour." Her voice does not quite agree with her.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Bring her below, to Halvard's fire.",
                nextNodeId = "c7_hold_hearth",
                consequences = Consequences(setFlags = setOf("c7_voss_below"))
            ),
            Choice(label = "Keep this between the two of you.", nextNodeId = "what_voss_offers")
        )
    ),
    StoryNode(
        id = "loyalists_move_in",
        chapterId = "chapter_7",
        title = "Loyalists Move In",
        illustrationId = "loyalists_ambush",
        narrativeText = """
            There are more of them than the arrows fired would suggest: Ilsevet doesn't send
            warnings, apparently, only cleanup. One steps forward wearing the same cinder-grey as
            the adept from Greymoor's cousin gate, except this one has a second mark beneath the
            first, fresh enough to still be raw: Voss's own name, crossed through.

            "Traitor gets the same as the jailer," the enforcer says, and doesn't wait for a
            response, or seem to expect one.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "loyalist_ambush_encounter"
    ),
    StoryNode(
        id = "loyalist_ambush_defeat",
        chapterId = "chapter_7",
        title = "The Flank Gave Out",
        illustrationId = "after_the_ambush",
        narrativeText = """
            The enforcer's "traitor gets the same as the jailer" turns out not to be a boast, and
            Kaelen learns exactly how little room the man leaves for a mistake before he's already
            made one. He goes down with the wind driven out of him and no clear idea how to get
            back up in time.

            He doesn't have to. Voss is already there, closing the gap he left open with a cut
            along her own forearm to show for it, and the enforcer breaks off rather than trade
            blows with two blades where he expected one uncovered back.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "after_the_ambush",
                consequences = Consequences(setFlags = setOf("lost_to_loyalist_enforcer", "c7_voss_hurt_deeper"))
            )
        )
    ),
    StoryNode(
        id = "after_the_ambush",
        chapterId = "chapter_7",
        title = "After the Ambush",
        illustrationId = "after_the_ambush",
        narrativeText = """
            It's close, closer than any fight since the siege, and Voss takes a cut along the
            forearm defending a flank Kaelen didn't know he'd left open. When the last of them
            breaks and runs rather than press a fight they've clearly lost, she doesn't
            celebrate. She just breathes, hard, and looks at the blood on her sleeve like it's a
            verdict she already expected.

            "That settles what I couldn't prove with words," she says. She works a signet ring
            free of her own hand, the Order's mark, not Ilsevet's, and holds it out. "Whatever's
            left of the real Order, this still means something to them. Might buy you a door
            that doesn't get kicked down twice."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the signet.",
                nextNodeId = "c7_cost_yard",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_yard")),
                consequences = Consequences(
                    grantItemIds = listOf("voss_seal"),
                    setFlags = setOf("earned_voss_seal")
                )
            ),
            Choice(
                label = "Take the signet.",
                nextNodeId = "c7_cost_stair",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_ground_stair")),
                consequences = Consequences(
                    grantItemIds = listOf("voss_seal"),
                    setFlags = setOf("earned_voss_seal")
                )
            ),
            Choice(
                label = "Take the signet.",
                nextNodeId = "c7_cost_road",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_ground_yard", "c7_ground_stair")),
                consequences = Consequences(
                    grantItemIds = listOf("voss_seal"),
                    setFlags = setOf("earned_voss_seal")
                )
            )
        )
    ),
    StoryNode(
        id = "what_voss_offers",
        chapterId = "chapter_7",
        title = "What Voss Offers",
        illustrationId = "what_voss_offers",
        narrativeText = """
            "I can get you close to the Ashfall Sanctum," Voss says. "Not inside; I'm not that
            trusted, not anymore. But close enough to see what she's actually building there,
            before she's ready to use it." She doesn't pretend it's a small ask, or a safe one,
            and she doesn't dress it up as anything other than what it is.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Take me there. I'd rather see it before it's aimed at anyone else.\"",
                nextNodeId = "chapter7_end_now",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_voss_below"), minStats = mapOf(StatType.COURAGE to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C7_DEPARTURE_FAMILY,
                    setFlags = setOf("chose_ashfall_now")
                )
            ),
            Choice(
                label = "\"Send word to Halvard first. He deserves to know before I disappear north again.\"",
                nextNodeId = "chapter7_end_warn_halvard",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_voss_below"), minStats = mapOf(StatType.HONOUR to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C7_DEPARTURE_FAMILY,
                    setFlags = setOf("chose_to_warn_halvard")
                )
            ),
            Choice(
                label = "\"Give me a day to decide. This isn't a small thing to agree to.\"",
                nextNodeId = "chapter7_end_asked_for_time",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_voss_below")),
                consequences = Consequences(clearFlags = C7_DEPARTURE_FAMILY, setFlags = setOf("asked_for_time"))
            ),
            Choice(
                label = "\"Take me there. I'd rather see it before it's aimed at anyone else.\"",
                nextNodeId = "c7_depart_now",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_voss_below"), minStats = mapOf(StatType.COURAGE to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C7_DEPARTURE_FAMILY,
                    setFlags = setOf("chose_ashfall_now")
                )
            ),
            Choice(
                label = "\"I'll tell Halvard to his face before I go.\"",
                nextNodeId = "c7_depart_blessing",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_voss_below"), minStats = mapOf(StatType.HONOUR to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("healing_draught"),
                    clearFlags = C7_DEPARTURE_FAMILY,
                    setFlags = setOf("chose_to_warn_halvard")
                )
            ),
            Choice(
                label = "\"Give me until dawn. This isn't a small thing to agree to.\"",
                nextNodeId = "c7_depart_time",
                requirements = ChoiceRequirement(requiredFlags = setOf("c7_voss_below")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to 20),
                    clearFlags = C7_DEPARTURE_FAMILY,
                    setFlags = setOf("asked_for_time")
                )
            )
        )
    ),
    StoryNode(
        id = "c7_hold_hearth",
        chapterId = "chapter_7",
        title = "Halvard Meets Her",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard is waiting at the stair mouth with the chain wound once round his fist, and he
            does not lower it for as long as it takes Voss to cross the last ten paces. He looks at
            the cut on her arm. He looks at the colours she is no longer wearing and the ring she
            has just given away. Whatever he decided to say on the way up, he does not say it yet.

            "The Ashen Order at my fire," he says at last, "bleeding on my floor. I have been trying
            to picture this for some time. In my picture there was more shouting." He sets a stool
            by the hearth and does not offer it. "Sit, or don't. But tell me why I should not put you
            out in the rain with the rest of the people who came to bury my door."
        """.trimIndent(),
        choices = rungChoices(
            label = "Stand between them. Let Voss answer in her own time.",
            next = "c7_hearth_mediated",
            delta = 1,
            possible = 2,
            base = Consequences(
                statDeltas = mapOf(StatType.HONOUR to 1),
                clearFlags = C7_VERDICT_FAMILY,
                setFlags = setOf("c7_halvard_trusts_voss")
            )
        ) + rungChoices(
            label = "Let Halvard have his questions. He has earned them.",
            next = "c7_hearth_sided",
            delta = -1,
            possible = 2,
            base = Consequences(clearFlags = C7_VERDICT_FAMILY, setFlags = setOf("c7_halvard_doubts_voss"))
        ) + rungChoices(
            label = "Say nothing. This is theirs to settle.",
            next = "c7_hearth_silent",
            delta = 0,
            possible = 2,
            base = Consequences(clearFlags = C7_VERDICT_FAMILY, setFlags = setOf("c7_halvard_doubts_voss"))
        )
    ),
    StoryNode(
        id = "c7_hearth_mediated",
        chapterId = "chapter_7",
        title = "Sword Arm and Silence",
        illustrationId = "warden_chamber",
        narrativeText = """
            Voss takes the stool, which is the first surprise. She answers him the way she answers
            everything, dry and sparing, and for once Halvard lets her finish. Yes, she was sent to
            bury the door. Yes, she stood in his tunnel and said what the Order says. "I am not proud
            of how close I came," she says. "I will not tell you my reasons for leaving, because you
            have not earned them and I have not decided to give them. You will have my sword arm and
            my silence on that. It is what I have."

            Halvard's eyebrows say it is more than he expected and less than he wanted. "Sword arm
            and silence," he repeats. "I have buried wardens for worse bargains."
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait for the fire to burn down.", nextNodeId = "c7_wound"))
    ),
    StoryNode(
        id = "c7_hearth_sided",
        chapterId = "chapter_7",
        title = "Questions in Order",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard has a great many questions and he asks them in order, like a man laying tools on
            a bench. Voss answers them in the same order, curtly, with her chin up and her good hand
            near her knife, as though each answer were a toll she resents paying. Kaelen lets it
            run. When it is done the old warden has what he wanted, which is the shape of her. She
            has not been shown any kindness in the getting of it, and the room knows.

            "You can stand there and call that fair," Voss says to Kaelen, afterwards, in the voice
            of someone not calling it anything.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait for the fire to burn down.", nextNodeId = "c7_wound"))
    ),
    StoryNode(
        id = "c7_hearth_silent",
        chapterId = "chapter_7",
        title = "A Wall for a Witness",
        illustrationId = "warden_chamber",
        narrativeText = """
            Nobody asks Kaelen what he thinks, and he does not offer it. The argument goes where
            arguments go, round the hearth, into the corners, back out again, Halvard's questions each
            a little shorter and Voss's answers each a little thinner, until the fire has outlasted
            them both. Nothing is settled. Something has been confirmed, which is that neither of
            them will move first.

            When Voss looks at him, it is the look of someone who counted on a witness and was
            given a wall.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait for the fire to burn down.", nextNodeId = "c7_wound"))
    ),
    StoryNode(
        id = "c7_wound",
        chapterId = "chapter_7",
        title = "The Cut Along Her Arm",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            Later, when the hearth has gone quiet, Voss is still on the stool with her sleeve pushed
            back and the cut along her forearm open to the lamp. It is not deep. It has been bleeding
            steadily for an hour into a rag she has not changed, the way a person bleeds who has
            decided the arm is not important. Her hand, when she reaches for the water, is not
            entirely steady.

            She sees him see it. "Don't," she says, and does not say what.
        """.trimIndent(),
        choices = rungChoices(
            label = "Fetch Halvard's salve and tend it.",
            next = "c7_wound_tended",
            delta = 1,
            base = Consequences(statDeltas = mapOf(StatType.HONOUR to 1))
        ) + rungChoices(
            label = "Ask her, while she is tired, who she fears more: Ilsevet or the Order.",
            next = "c7_wound_questioned",
            delta = -1,
            base = Consequences(statDeltas = mapOf(StatType.COURAGE to 1))
        ) + rungChoices(
            label = "Leave her to it.",
            next = "c7_wound_left",
            delta = 0
        )
    ),
    StoryNode(
        id = "c7_wound_tended",
        chapterId = "chapter_7",
        title = "Salve",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            She lets him. That is the whole of it, and it is a good deal. The salve stings and she
            says nothing about the sting. When the binding is done she turns the arm over in the lamplight
            and studies his work with the eye of someone who has done a great deal of it herself. "The
            Order taught me that stitch," she says. "It also taught me what the stitch is for." She does
            not say what. She flexes her fingers, once, twice. "Thank you."
        """.trimIndent(),
        choices = verdictChoices()
    ),
    StoryNode(
        id = "c7_wound_questioned",
        chapterId = "chapter_7",
        title = "A Cheap Trick",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            "Both," Voss says, after a time. "The Order for what it will do to me. Ilsevet for what she
            has already done to it." She says it without heat, the answer of someone too tired to be
            clever. Then, as though she has noticed what she gave away: "That was a cheap trick. You
            waited until I was bleeding." She is not wrong, and she does not forgive it yet. She dresses
            the arm herself, one-handed, and it takes a long time, and Kaelen does not help.
        """.trimIndent(),
        choices = verdictChoices()
    ),
    StoryNode(
        id = "c7_wound_left",
        chapterId = "chapter_7",
        title = "Alone With It",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            He leaves her to it. She does not call after him. When he looks back from the stair she is
            binding the arm one-handed with her teeth, and the rag she is using is the sleeve of her own
            shirt. She does not look up. Some people would rather be alone with a wound than watched
            with it, and Kaelen cannot swear which kind she is.
        """.trimIndent(),
        choices = verdictChoices()
    ),
    StoryNode(
        id = "c7_verdict_trusts",
        chapterId = "chapter_7",
        title = "What Halvard Thinks",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard catches Kaelen on the stair before dawn. "She is lying to me about something," he
            says. "So are you, a little, and I hold it against neither of you. It does not follow that
            she is lying about the thing in the north." He considers the signet in Kaelen's hand, the
            little gold of it. "A signet is worth what the one who gave it still is. Ask yourself what
            she still is."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go back to Voss.", nextNodeId = "what_voss_offers"))
    ),
    StoryNode(
        id = "c7_verdict_doubts",
        chapterId = "chapter_7",
        title = "What Halvard Thinks",
        illustrationId = "warden_chamber",
        narrativeText = """
            Halvard catches Kaelen on the stair before dawn. "I do not trust her," he says, without
            heat, like a man naming the weather. "I think you should go anyway, which is a different
            thing, and I would like you to notice that I know it is." He looks at the signet in Kaelen's
            palm. "A signet is worth what the one who gave it still is. I do not yet know what she is.
            Neither, I think, does she."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go back to Voss.", nextNodeId = "what_voss_offers"))
    ),
    StoryNode(
        id = "c7_depart_now",
        chapterId = "chapter_7",
        title = "Before the Door Wakes",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            They leave in the grey before dawn, while the Hold still thinks it is night. Halvard is
            not at the stair. That is its own message, and Kaelen reads it as clearly as if it had been
            chalked on the wall: he will not stand in the way, and he will not wave anyone off. The
            signet is cold in Kaelen's palm beside the brand that never quite stops aching. Voss does
            not look back to see whether he follows. She already knows.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp"))
    ),
    StoryNode(
        id = "c7_depart_blessing",
        chapterId = "chapter_7",
        title = "A Word at the Stair",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            Halvard is waiting at the stair mouth with a pack he packed himself and does not pretend
            he didn't. "An enemy's enemy isn't a friend," he says. "It's just an enemy you haven't
            finished with yet. Go anyway." He puts a flask in Kaelen's hand, a draught from the Hold's
            last good batch, and closes the hand over it. "Come back and tell me how it ends. That is
            not a request."

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp"))
    ),
    StoryNode(
        id = "c7_depart_time",
        chapterId = "chapter_7",
        title = "A Day to Decide",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            The day passes slowly, and Kaelen spends it the way he spends most days now, in the cold
            between the forge and the stair, turning the signet over. By dusk the answer has not
            changed, only settled, the way a decision does once a man stops arguing with himself about
            it. Halvard says nothing to hurry him or to hold him. Voss sleeps, finally, on a pallet by
            the hearth, with her good hand curled around nothing.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp"))
    ),
    StoryNode(
        id = "chapter7_end_now",
        chapterId = "chapter_7",
        title = "End of Chapter VII — An Uneasy Alliance",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            There's no time to send word anywhere. Voss is already moving, favoring her good
            side, and Kaelen falls into step behind her before he lets himself weigh what he's
            leaving unsaid. Halvard will have questions when he notices the door short one
            jailer. They'll keep until there's an answer worth giving.

            The Order's signet sits cold and unfamiliar against Kaelen's palm, next to the brand
            that never quite stops aching. Voss doesn't look back to see if he's still
            following. She already knows he is.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp")
        )
    ),
    StoryNode(
        id = "chapter7_end_warn_halvard",
        chapterId = "chapter_7",
        title = "End of Chapter VII — An Uneasy Alliance",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            Voss waits at the tree line, patient in a way that costs her something visible, while
            Kaelen goes below to tell Halvard everything: the ambush, the signet, the sanctum in
            the Ashfall reaches waiting to be seen before it's finished.

            Halvard listens to all of it without interrupting, and when Kaelen's done, says only
            one thing that stays with him afterward: "An enemy's enemy isn't a friend. It's just
            an enemy you haven't finished with yet." He doesn't say Kaelen's wrong to go anyway.
            He just makes him promise to come back and tell him how it ends.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp")
        )
    ),
    StoryNode(
        id = "chapter7_end_asked_for_time",
        chapterId = "chapter_7",
        title = "End of Chapter VII — An Uneasy Alliance",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            Voss doesn't argue for the day, though something in her posture suggests she'd hoped
            for less of a wait. "Take it," she says instead. "I'd rather you sure than fast." She
            leaves the way she came, alone, favoring the same side she favored arriving, and
            Kaelen watches her go until the rain swallows her shape entirely.

            The day passes slower than he expects it to. By the end of it the answer hasn't
            changed, only settled, the way a decision does once a man stops arguing with himself
            about it. Halvard, when he hears the whole of it, says only one thing that stays with
            Kaelen afterward: "An enemy's enemy isn't a friend. It's just an enemy you haven't
            finished with yet." He doesn't say Kaelen's wrong to go anyway.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "c8_ash_camp")
        )
    )
)

val chapter7 = com.thelastjailer.app.Chapter(
    id = "chapter_7",
    number = 7,
    title = "Chapter VII — An Uneasy Alliance",
    startNodeId = "c7_forge_night"
)
