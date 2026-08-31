package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XVI — What Was Already Taken.
 *
 * Delivers on XV's closing hook by making the party's own accumulated trophies the target instead
 * of another gate or person — a deliberate mechanical callback to the shard items collected across
 * X-XV (Fenmoor Ward Shard, Shard of the Seventh Door, Shard of the First Ward). The prisoner,
 * continuing its wary-ally role from XIV, warns that Ilsevet has stopped insisting on true settings
 * and started treating broken fragments as an acceptable substitute now that "passable" has replaced
 * "complete" in her calculus. A specialized infiltrator — not a squad, not a siege — is sent to
 * steal what force and diplomacy couldn't take. Sixteenth and toughest combat encounter yet (Cinder
 * Reliquary Thief, 220 HP). Deliberately no new item this chapter — the theme is loss, not
 * acquisition — and the theft itself is narrative rather than a mechanical inventory removal (the
 * data model has no consume-on-choice mechanism for story consequences), consistent with how
 * Halvard's death and other narrative losses aren't mirrored by removing entries from any tracked
 * list.
 */
val chapter16Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_kaelen_isnt_prepared_for",
        chapterId = "chapter_16",
        title = "What Kaelen Isn't Prepared For",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner speaks first this time, before Kaelen's even settled at the gate for the
            night. "She's stopped trying to take gates and people," it says, without preamble.
            "She's started counting what you've already taken from her failures instead."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What does that mean?\"",
                nextNodeId = "the_prisoners_warning",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_the_prisoner_for_detail")
                )
            ),
            Choice(
                label = "Let it explain in its own time.",
                nextNodeId = "the_prisoners_warning",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("let_the_prisoner_explain")
                )
            )
        )
    ),
    StoryNode(
        id = "the_prisoners_warning",
        chapterId = "chapter_16",
        title = "The Prisoner's Warning",
        illustrationId = "the_prisoners_warning",
        narrativeText = """
            "Every shard you've carried out of a fight or a failed working — Fenmoor's ward, the
            frame at Ashfall, whatever the vision at this gate left behind — none of them are a
            true setting. But broken pieces of six can still make a passable seventh, if the hand
            assembling them stops caring whether passable is good enough. She's stopped caring.
            That's what four settings and a dead garrison taught her."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Take stock of what you're carrying.", nextNodeId = "what_to_protect")
        )
    ),
    StoryNode(
        id = "what_to_protect",
        chapterId = "chapter_16",
        title = "What to Protect",
        illustrationId = "what_to_protect",
        narrativeText = """
            Kaelen counts what he's actually carrying and doesn't like the total. Four fragments,
            gathered without ever meaning to build anything, sitting in the same pack that's
            crossed half the ruined kingdom in the last year.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Keep them on me. If someone wants them, they'll have to go through me directly.\"",
                nextNodeId = "the_reliquary_thief",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 14)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("kept_shards_close")
                )
            ),
            Choice(
                label = "\"Split them between Stonebeard, Fenmoor, and Voss. No single loss ends this.\"",
                nextNodeId = "the_reliquary_thief",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 13)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("dispersed_the_shards")
                )
            ),
            Choice(
                label = "There isn't time to do anything but keep watch and hope it's enough.",
                nextNodeId = "the_reliquary_thief",
                consequences = Consequences(setFlags = setOf("trusted_to_luck"))
            )
        )
    ),
    StoryNode(
        id = "the_reliquary_thief",
        chapterId = "chapter_16",
        title = "The Reliquary Thief",
        illustrationId = "the_reliquary_thief",
        narrativeText = """
            Whoever Ilsevet sent this time doesn't move like a soldier — no column, no horn,
            nothing but a shape that shouldn't be able to move that quietly through Stonebeard's
            own tunnels. Kaelen catches them with a hand already closing around the first shard.

            "You collect what other people fail to hold onto," the thief says, almost admiring,
            drawing a blade shaped for cutting straps rather than throats. "So do I."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reliquary_thief_encounter"
    ),
    StoryNode(
        id = "what_was_taken",
        chapterId = "chapter_16",
        title = "What Was Taken",
        illustrationId = "what_was_taken",
        narrativeText = """
            The thief doesn't win, not quite — whatever training lets someone move that quietly
            through a dwarven hold doesn't help much once the fight stops being subtle. But
            they're fast to run once the balance tips, and Kaelen doesn't catch all of them.

            He still has most of what he came in with. Not all of it. Whatever else Ilsevet's
            reliquary thief walked away with, it was clearly enough to be worth the risk.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Take stock of the damage.", nextNodeId = "chapter16_end")
        )
    ),
    StoryNode(
        id = "chapter16_end",
        chapterId = "chapter_16",
        title = "End of Chapter XVI — What Was Already Taken",
        illustrationId = "chapter16_threshold",
        narrativeText = """
            Voss takes the loss harder than Kaelen expects. "Four settings and now this," she
            says. "She's not building toward six anymore. She's building toward however many
            pieces she can actually get her hands on, whatever that number ends up being."

            It isn't the argument Kaelen wants to have at the end of a long night, but it isn't
            wrong, either. Whatever Ilsevet assembles next won't be clean, won't be complete, and
            — if the prisoner's fear back at the gate meant anything at all — might not be any
            safer for that.

            Chapter XVII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_signal")
        )
    )
)

val chapter16 = com.thelastjailer.app.Chapter(
    id = "chapter_16",
    number = 16,
    title = "Chapter XVI — What Was Already Taken",
    startNodeId = "what_kaelen_isnt_prepared_for"
)
