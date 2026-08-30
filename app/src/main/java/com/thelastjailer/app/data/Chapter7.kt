package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter VII — An Uneasy Alliance.
 *
 * Turns Voss from antagonist into a conflicted, unreliable ally: she returns alone, admits the
 * Chapter V envoy was Ilsevet's rogue operation rather than sanctioned Order business, and offers
 * intelligence in exchange for Kaelen's trust. A shared ambush (Ilsevet's people moving against
 * Voss specifically, for showing doubt) forces the alliance to prove itself in a fight rather than
 * just words, mirroring how every prior chapter has resolved its central tension through combat.
 * Arc: Voss's return, her account of the envoy's betrayal (with a flag-gated callback to how
 * Kaelen handled that betrayal in V), an ambush, a sixth and toughest-yet fight, Voss offering her
 * own Order signet as proof of good faith, and a stat-gated choice about what to do with the
 * alliance — ending on a destination for Chapter VIII (the Ashfall Sanctum) rather than a
 * resolution, continuing the II/III/V pattern of chapter-ending hooks.
 */
val chapter7Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "voss_returns_alone",
        chapterId = "chapter_7",
        title = "Voss Returns Alone",
        illustrationId = "threshold_ahead",
        narrativeText = """
            No column this time. No banner, not even the four riders from her first visit — just
            Voss, on foot, favoring one side, stopping well short of the tree the way she never
            has before.

            "I'm not here as the Order," she says, before Kaelen can ask. "I'm not entirely sure
            I'm here as anything, anymore. I need you to hear something, and I need you to
            believe I'm not the one who sent that envoy to your door."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Why should I believe a word of it?\"",
                nextNodeId = "what_voss_knows",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_voss")
                )
            ),
            Choice(
                label = "\"Come in out of the rain, then. Talk.\"",
                nextNodeId = "what_voss_knows",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("welcomed_voss")
                )
            )
        )
    ),
    StoryNode(
        id = "what_voss_knows",
        chapterId = "chapter_7",
        title = "What Voss Knows",
        illustrationId = "voss_at_the_door",
        narrativeText = """
            "The envoy who came here wasn't Order business," she says. "He was Ilsevet's, and
            Ilsevet answers to nobody's chain of command but her own these days — not really. She
            split from the Order's actual leadership months ago, over exactly what you'd guess:
            whether to bury what's behind these gates or make use of it. What happened here, the
            ambush, the ring on that adept's hand — that's the first proof I've had that isn't
            rumor."

            She names a place Kaelen has never heard of — a sanctum in the Ashfall reaches, with
            three of Ilsevet's targeted gates within a hard week's ride of it. "I think that's
            where she's building toward. I think you're the only person outside her own people who
            might actually believe me."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I believe you. What do you need?\"",
                nextNodeId = "the_price_of_trust",
                consequences = Consequences(setFlags = setOf("trusted_voss_fully"))
            ),
            Choice(
                label = "\"I'll hear you out. That's not the same as trust.\"",
                nextNodeId = "the_price_of_trust",
                consequences = Consequences(setFlags = setOf("stayed_guarded"))
            ),
            Choice(
                label = "\"You let your own Order send four riders to bury this door. Trust has to run both ways.\"",
                nextNodeId = "the_price_of_trust",
                requirements = ChoiceRequirement(requiredFlags = setOf("refused_envoy")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("held_voss_accountable")
                )
            )
        )
    ),
    StoryNode(
        id = "the_price_of_trust",
        chapterId = "chapter_7",
        title = "The Price of Trust",
        illustrationId = "the_price_of_trust",
        narrativeText = """
            Whatever Voss meant to say next dies in her throat. She's moving before Kaelen
            registers why — pulling him sideways as an arrow takes the space where his shoulder
            was a heartbeat ago.

            "They followed me," she says, blade already out, no surprise left in her voice at all.
            "Of course they followed me."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "loyalists_move_in")
        )
    ),
    StoryNode(
        id = "loyalists_move_in",
        chapterId = "chapter_7",
        title = "Loyalists Move In",
        illustrationId = "loyalists_ambush",
        narrativeText = """
            There are more of them than the arrows fired would suggest — Ilsevet doesn't send
            warnings, apparently, only cleanup. One steps forward wearing the same cinder-grey as
            the adept from Greymoor's cousin gate, except this one has a second mark beneath the
            first: Voss's own name, crossed through.

            "Traitor gets the same as the jailer," the enforcer says, and doesn't wait for a
            response.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "loyalist_ambush_encounter"
    ),
    StoryNode(
        id = "after_the_ambush",
        chapterId = "chapter_7",
        title = "After the Ambush",
        illustrationId = "after_the_ambush",
        narrativeText = """
            It's close — closer than any fight since the siege — and Voss takes a cut along the
            forearm defending a flank Kaelen didn't know he'd left open. When the last of them
            breaks and runs rather than press a fight they've clearly lost, she doesn't celebrate.
            She just breathes, hard, and looks at the blood on her sleeve like it's a verdict.

            "That settles what I couldn't prove with words," she says. She works a signet ring
            free of her own hand — the Order's mark, not Ilsevet's — and holds it out. "Whatever's
            left of the real Order, this still means something to them. Might buy you a door that
            doesn't get kicked down twice."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the signet.",
                nextNodeId = "what_voss_offers",
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
            "I can get you close to the Ashfall Sanctum," Voss says. "Not inside — I'm not that
            trusted, not anymore. But close enough to see what she's actually building there,
            before she's ready to use it." She doesn't pretend it's a small ask, or a safe one.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Take me there. I'd rather see it before it's aimed at anyone else.\"",
                nextNodeId = "chapter7_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("chose_ashfall_now")
                )
            ),
            Choice(
                label = "\"Send word to Halvard first. He deserves to know before I disappear north again.\"",
                nextNodeId = "chapter7_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 5)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("chose_to_warn_halvard")
                )
            ),
            Choice(
                label = "\"Give me a day to decide. This isn't a small thing to agree to.\"",
                nextNodeId = "chapter7_end",
                consequences = Consequences(setFlags = setOf("asked_for_time"))
            )
        )
    ),
    StoryNode(
        id = "chapter7_end",
        chapterId = "chapter_7",
        title = "End of Chapter VII — An Uneasy Alliance",
        illustrationId = "chapter7_threshold",
        narrativeText = """
            Voss leaves the way she came, alone, favoring the same side she favored arriving.
            Kaelen watches her go until the rain swallows her shape entirely, the Order's signet a
            cold, unfamiliar weight next to the brand on his palm.

            Halvard, when he hears all of it, says only one thing that stays with Kaelen
            afterward: "An enemy's enemy isn't a friend. It's just an enemy you haven't finished
            with yet." He doesn't say Kaelen's wrong to go anyway.

            Chapter VIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare for the road to Ashfall.", nextNodeId = "the_road_to_ashfall")
        )
    )
)

val chapter7 = com.thelastjailer.app.Chapter(
    id = "chapter_7",
    number = 7,
    title = "Chapter VII — An Uneasy Alliance",
    startNodeId = "voss_returns_alone"
)
