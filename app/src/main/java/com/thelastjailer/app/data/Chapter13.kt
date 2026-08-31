package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XIII — What the Order Allows.
 *
 * A deliberate pacing breather after twelve chapters of steadily escalating combat (mirroring
 * Chapter III's choice to skip a fight "for variety") — no new combat encounter this chapter at
 * all. Instead: the actual Ashen Order (distinct from Ilsevet's splinter, last seen as a genuine
 * threat in III/IV) sends a ranking official to judge Voss's standing after everything since VII,
 * Stonebeard finally holds proper rites for Halvard instead of the hasty burial after X, and the
 * chapter closes on the first real question of what comes next for the "Last Jailer" title beyond
 * pure survival. Ends on a quiet, unsettling hook rather than another siege: the pressure behind
 * Kaelen's own gate speaks again for the first time since III, aware that Ilsevet's four-setting
 * activation in XII reached all the way to it.
 */
val chapter13Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_orders_delegation",
        chapterId = "chapter_13",
        title = "The Order's Delegation",
        illustrationId = "threshold_ahead",
        narrativeText = """
            Halvard's grave is barely a season old when the second rider in as many months comes
            up the ruined road to the black door — Order colours again, but not Voss's, and not
            Ilsevet's cinder-grey either. An actual delegation this time, banners and all, the
            kind Voss says the real Order hasn't bothered sending anywhere in years.

            "They're not here for you," Voss says, reading Kaelen's face. "Not directly. They're
            here for me."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we meet them together.\"",
                nextNodeId = "the_inquisitor_general",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("stood_with_voss")
                )
            ),
            Choice(
                label = "\"This one might go better if I'm not standing in the doorway.\"",
                nextNodeId = "the_inquisitor_general",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("let_voss_go_alone")
                )
            )
        )
    ),
    StoryNode(
        id = "the_inquisitor_general",
        chapterId = "chapter_13",
        title = "The Inquisitor-General",
        illustrationId = "the_inquisitor_general",
        narrativeText = """
            She doesn't look like Voss, or move like her either — older, slower, entirely
            unhurried in a way that has nothing to do with confidence and everything to do with
            rank. "Inquisitor Voss," she says, and doesn't offer a hand. "Or should I say, former
            Inquisitor. You've been reported dead twice and traitor once since Stonebeard's gate
            stopped being a rumour. I'm inclined to believe the second report over the first two,
            unless you give me a reason not to."
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
                nextNodeId = "selvanes_verdict",
                requirements = ChoiceRequirement(requiredFlags = setOf("stood_with_voss")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("kaelen_vouched_for_voss")
                )
            ),
            Choice(
                label = "Wait outside, and trust her account.",
                nextNodeId = "selvanes_verdict",
                requirements = ChoiceRequirement(requiredFlags = setOf("let_voss_go_alone")),
                consequences = Consequences(setFlags = setOf("trusted_voss_account"))
            )
        )
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
            useful than both.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let them ride out.", nextNodeId = "halvards_rites")
        )
    ),
    StoryNode(
        id = "halvards_rites",
        chapterId = "chapter_13",
        title = "Halvard's Rites",
        illustrationId = "halvards_proper_rites",
        narrativeText = """
            With Selvane's riders gone and no new siege on the horizon, Stonebeard finally has
            room to grieve properly instead of just quickly. The dwarves who still call the hold
            home carve Halvard's name into the stone above the gate he died holding — not a grave
            marker, they explain, a warden's mark, the same honor given to five names Kaelen's
            never heard spoken and one, now, that he has.

            Kaelen adds his own words when it's his turn, and finds he doesn't have as many as he
            expected. Just: he held. That was always going to be enough, coming from Halvard.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take a rubbing of the mark before you leave.",
                nextNodeId = "who_stands_with_stonebeard",
                consequences = Consequences(grantItemIds = listOf("rubbing_of_halvards_mark"))
            )
        )
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
            around to asking anyone else. Thessaly's answer, when word reaches Fenmoor, is blunter
            than the question deserved.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Anyone willing to hold a gate can be taught to hold it well. Start training them.\"",
                nextNodeId = "chapter13_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 11)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("began_training_new_wardens")
                )
            ),
            Choice(
                label = "\"The chain isn't mine to hand out. But I'll stand watch beside anyone who picks it up.\"",
                nextNodeId = "chapter13_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 10)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("stood_watch_rather_than_delegate")
                )
            ),
            Choice(
                label = "\"One gate, one jailer, for now. There will be time to decide the rest later.\"",
                nextNodeId = "chapter13_end",
                consequences = Consequences(setFlags = setOf("deferred_the_question"))
            )
        )
    ),
    StoryNode(
        id = "chapter13_end",
        chapterId = "chapter_13",
        title = "End of Chapter XIII — What the Order Allows",
        illustrationId = "chapter13_threshold",
        narrativeText = """
            Life at Stonebeard doesn't return to normal so much as it settles into a new, wearier
            shape — grief worn in like a groove in stone, work still needing doing regardless.
            Kaelen finds himself standing watch at the gate more nights than he needs to, chain
            and brand both aching faintly in a way neither used to.

            That night, for the first time since the binding rite, the thing behind the black
            door speaks again — not asking for anything this time. Just one sentence, pressed
            against the wards like a held breath finally let out: "Four settings. It felt four
            settings, and it isn't finished waiting for the rest."

            Chapter XIV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Listen.", nextNodeId = "what_waits_speaks")
        )
    )
)

val chapter13 = com.thelastjailer.app.Chapter(
    id = "chapter_13",
    number = 13,
    title = "Chapter XIII — What the Order Allows",
    startNodeId = "the_orders_delegation"
)
