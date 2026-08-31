package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter VIII — The Seventh Door.
 *
 * Delivers on Chapter VII's destination and reframes the entire threat. Recon at the Ashfall
 * Sanctum reveals Ilsevet isn't trying to open or destroy the six warded gates — she's building a
 * seventh, artificial one out of pieces taken from all six, which retroactively explains why she
 * wanted access to Kaelen's brand specifically back in V (it's a component, not a curiosity). The
 * chapter ends on a personal cost rather than a combat trophy: Voss stays behind to buy Kaelen's
 * escape and is taken alive, turning "four gates left" into a rescue Kaelen now owes. Arc: the
 * road to Ashfall, first sight of the Sanctum, the stolen schematic reveal, a seventh and
 * toughest-yet combat encounter (Sanctum Sentinel), Voss's capture, and a stat-gated choice about
 * how urgently to go back for her, mirroring III/IV/V/VI/VII but for the first time gating on
 * what to do next rather than what was just done.
 */
val chapter8Nodes: List<StoryNode> = listOf(
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
            said plainly, once, before we're close enough that saying it stops mattering."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we make sure it doesn't go wrong.\"",
                nextNodeId = "the_sanctum_from_afar",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("reassured_voss")
                )
            ),
            Choice(
                label = "\"I know what I'm risking. Let's move.\"",
                nextNodeId = "the_sanctum_from_afar",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("acknowledged_the_risk")
                )
            )
        )
    ),
    StoryNode(
        id = "the_sanctum_from_afar",
        chapterId = "chapter_8",
        title = "The Sanctum, From Afar",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            The Ashfall Sanctum isn't a fortress so much as a wound with walls built around it —
            black stone quarried from somewhere that clearly wasn't meant to be quarried, arranged
            around a central spire that hums faintly even from this distance, felt more than heard.

            Voss goes still beside him. "Three months ago that spire wasn't finished. Whatever
            she's building, she's close."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Move in under cover of the ash-fall itself.",
                nextNodeId = "what_they_see",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("moved_in_stealthily")
                )
            ),
            Choice(
                label = "Circle wide and find a safer vantage first.",
                nextNodeId = "what_they_see",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("circled_wide")
                )
            )
        )
    ),
    StoryNode(
        id = "what_they_see",
        chapterId = "chapter_8",
        title = "What They See",
        illustrationId = "the_device_glimpsed",
        narrativeText = """
            Close enough to risk it, Voss steals a set of half-burned plans from an unguarded
            worktable while Kaelen keeps watch — and what they show stops them both cold. Not a
            weapon, not exactly. A frame, six-sided, six empty settings marked in a hand that
            matches the wards on Kaelen's own gate.

            "Six jailers," Voss murmurs, reading the same margin notes Kaelen can't quite parse.
            "She's not trying to free what's behind any of the doors. She's trying to build a
            seventh one — using pieces of the six that already exist." Her eyes go to the brand on
            Kaelen's palm without meaning to. He doesn't need her to finish the thought.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get out. Now, before anyone notices what's missing.",
                nextNodeId = "sentinels_close_in",
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
            that's somehow worse than an alarm would be — the sound of people who have drilled for
            exactly this and don't need to rush.

            The first sentinel finds them before they clear the ash-field, armor the same black
            stone as the walls behind it, moving like it was quarried rather than born.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "sanctum_sentinel_encounter"
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

            Kaelen runs. He hears her taken, not killed — shouted orders, not a killing blow —
            and that's the only mercy the wasteland offers him on the way out.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep running until the sanctum is out of sight.",
                nextNodeId = "what_must_be_risked"
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
            them alive.

            It isn't much of a comfort. It's what he's got.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not leaving her there to buy me time I didn't ask for.\"",
                nextNodeId = "chapter8_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 7)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("vowed_to_return_now")
                )
            ),
            Choice(
                label = "\"I go back for her properly, or not at all. Halvard first.\"",
                nextNodeId = "chapter8_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 6)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("vowed_to_return_prepared")
                )
            ),
            Choice(
                label = "Keep moving. Decide the rest on the road.",
                nextNodeId = "chapter8_end",
                consequences = Consequences(setFlags = setOf("undecided_on_voss"))
            )
        )
    ),
    StoryNode(
        id = "chapter8_end",
        chapterId = "chapter_8",
        title = "End of Chapter VIII — The Seventh Door",
        illustrationId = "chapter8_threshold",
        narrativeText = """
            The road south is longer alone than it was with company, even company as guarded as
            Voss's. Kaelen reaches Stonebeard with the schematic, four gates still unaccounted
            for, and a debt he didn't ask to owe.

            Halvard reads the stolen plans in silence, longer than he's ever taken over anything,
            and when he finally looks up, the dry patience is entirely gone from his face. "A
            seventh door," he says. "Built from pieces of the six. She's not trying to free what's
            behind any of them, Kaelen. She's trying to build something new out of all of you at
            once."

            Chapter IX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Decide what happens next.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter8 = com.thelastjailer.app.Chapter(
    id = "chapter_8",
    number = 8,
    title = "Chapter VIII — The Seventh Door",
    startNodeId = "the_road_to_ashfall"
)
