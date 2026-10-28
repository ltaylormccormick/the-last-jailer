package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Everything Chapter XVI records: the two shipped ways of meeting the prisoner's warning, the three shipped answers about the
 * shards, and the defeat marker. The chapter's first choices clear it all, so a replay reaches the warning with an empty slate.
 */
val C16_TELLING_FLAGS: Set<String> = setOf(
    "pressed_the_prisoner_for_detail", "let_the_prisoner_explain", "kept_shards_close", "dispersed_the_shards",
    "trusted_to_luck", "lost_to_cinder_reliquary_thief"
)

/**
 * The shipped answer that splits the shards between three keepers, written once per rung: Kaelen puts a piece in Voss's hand, and
 * what she makes of it depends on where the two of them stand. The lowest rung also covers a save that holds none. The stat gate,
 * the flag and the stat change are the shipped ones.
 */
private fun handedToVoss(label: String, minStats: Map<StatType, Int>, consequences: Consequences): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c16_handing_trusted",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"), minStats = minStats),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c16_handing_working",
        requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"), minStats = minStats),
        consequences = consequences
    ),
    Choice(
        label = label,
        nextNodeId = "c16_handing_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"), minStats = minStats),
        consequences = consequences
    )
)

/**
 * What the theft cost, by the answer Kaelen gave Edda about the Hold's door in Chapter XIII: open, barred, or neither (where Edda's
 * people are not, or an older save that never answered). The last is read by the absence of the other two.
 */
private fun whatItCost(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c16_aftermath_open", requirements = ChoiceRequirement(requiredFlags = setOf("c13_door_open"))),
    Choice(label = label, nextNodeId = "c16_aftermath_barred", requirements = ChoiceRequirement(requiredFlags = setOf("c13_door_barred"))),
    Choice(label = label, nextNodeId = "c16_aftermath_none", requirements = ChoiceRequirement(forbiddenFlags = C13_DOOR_FAMILY))
)

/**
 * Chapter XVI — What Was Already Taken.
 *
 * Delivers on XV's closing hook by making the party's own accumulated trophies the target instead
 * of another gate or person — a deliberate mechanical callback to the shard items collected across
 * X-XV (Fenmoor Ward Shard, Shard of the Seventh Door, Shard of the First Ward). The prisoner,
 * continuing its wary-ally role from XIV, warns that Ilsevet has stopped insisting on true settings
 * and started treating broken fragments as an acceptable substitute now that "passable" has replaced
 * "complete" in her calculus — pressing it for detail versus letting it explain unprompted changes
 * how that warning actually arrives, and where Kaelen decides to keep the shards afterward (on his
 * own person, split between three keepers, or left to chance) now changes where and how the thief
 * actually strikes, not just which flag it sets. A specialized infiltrator — not a squad, not a
 * siege — is sent to steal what force and diplomacy couldn't take. Sixteenth and toughest combat
 * encounter yet (Cinder Reliquary Thief, 220 HP). Deliberately no new item this chapter — the theme
 * is loss, not acquisition — and the theft itself is narrative rather than a mechanical inventory
 * removal (the data model has no consume-on-choice mechanism for story consequences), consistent
 * with how Halvard's death and other narrative losses aren't mirrored by removing entries from any
 * tracked list.
 *
 * The expansion lets the Hold's own state decide what the theft costs, and lets Voss be handed a piece. Four shipped phrases are
 * corrected by the author's decision, because a player holds two shards or three, never four (Chapter1316FamilyProofTest), and the
 * Seventh Door shard comes only from helping Ilsevet: "Four fragments" is now "Several fragments", "between three keepers" is now
 * "between keepers", and in both versions of the prisoner's warning "the frame at Ashfall" is now "whatever came away from the frame
 * at Ashfall", which names where a shard may have come from and does not say Kaelen took one there. Chapter1316ShippedPinTest
 * records those four as the only changes to shipped prose in Chapters XIII to XVI. A Kaelen who split the shards puts one in Voss's hand first, and what she makes of it depends on where the two of
 * them stand. After the fight, the door he told Edda about in Chapter XIII (open, barred, or neither) decides how the thief got in
 * and who pays for it. The scenes name no count of shards: Chapter XVII needs at least two to have been taken, and the pieces are
 * only ever "the pieces". The shipped scenes and their fight are untouched; only where some of their choices lead has changed.
 */
val chapter16Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_kaelen_isnt_prepared_for",
        chapterId = "chapter_16",
        title = "What Kaelen Isn't Prepared For",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner speaks first this time, before Kaelen's even settled at the gate for the
            night, its voice pressing against the wards with an urgency he hasn't heard from it
            before. "She's stopped trying to take gates and people," it says, without preamble.
            "She's started counting what you've already taken from her failures instead."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What does that mean?\"",
                nextNodeId = "the_prisoners_warning_pressed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_the_prisoner_for_detail")
                )
            ),
            Choice(
                label = "Let it explain in its own time.",
                nextNodeId = "the_prisoners_warning_patient",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("let_the_prisoner_explain")
                )
            )
        )
    ),
    StoryNode(
        id = "the_prisoners_warning_pressed",
        chapterId = "chapter_16",
        title = "The Prisoner's Warning",
        illustrationId = "the_prisoners_warning",
        narrativeText = """
            It answers the demand without hesitation, words coming faster than its usual measured
            pace, as if it's relieved not to have to find its own way into the subject. "Every
            shard you've carried out of a fight or a failed working: Fenmoor's ward, whatever
            came away from the frame at Ashfall, whatever the vision at this gate left behind.
            None of them are a true setting. But broken pieces of six can still make a passable
            seventh, if the hand assembling them stops caring whether passable is good enough.
            She's stopped caring. That's what four settings and a dead garrison taught her."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Take stock of what you're carrying.", nextNodeId = "what_to_protect")
        )
    ),
    StoryNode(
        id = "the_prisoners_warning_patient",
        chapterId = "chapter_16",
        title = "The Prisoner's Warning",
        illustrationId = "the_prisoners_warning",
        narrativeText = """
            It takes its time getting there anyway, circling the point the way it always does
            when something matters enough to get wrong. "Every shard you've carried out of a
            fight or a failed working: Fenmoor's ward, whatever came away from the frame at
            Ashfall, whatever the vision at this gate left behind. None of them are a true
            setting. But broken pieces of six can still make a passable seventh, if the hand
            assembling them stops caring whether passable is good enough. She's stopped caring.
            That's what four settings and a dead garrison taught her." Patience earned him the
            whole shape of it, unhurried and unbroken.
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
            Kaelen counts what he's actually carrying and doesn't like the total. Several fragments,
            gathered without ever meaning to build anything, sitting in the same pack that's
            crossed half the ruined kingdom in the last year, each one now a target instead of
            just a keepsake.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Keep them on me. If someone wants them, they'll have to go through me directly.\"",
                nextNodeId = "the_reliquary_thief_close",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 14)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("kept_shards_close")
                )
            )
        ) + handedToVoss(
            "\"Split them between Stonebeard, Fenmoor, and Voss. No single loss ends this.\"",
            mapOf(StatType.HONOUR to 13),
            Consequences(
                statDeltas = mapOf(StatType.HONOUR to 1),
                setFlags = setOf("dispersed_the_shards")
            )
        ) + Choice(
            label = "There isn't time to do anything but keep watch and hope it's enough.",
            nextNodeId = "the_reliquary_thief_luck",
            consequences = Consequences(setFlags = setOf("trusted_to_luck"))
        )
    ),
    StoryNode(
        id = "c16_handing_wary",
        chapterId = "chapter_16",
        title = "A Piece for Voss",
        illustrationId = "what_to_protect",
        narrativeText = """
            Voss looks at the piece for longer than the piece deserves, and then at Kaelen.
            "You are giving the Order's own deserter a thing the Order would cross a kingdom
            to have," she says. "You know that." She does not take it yet. "I will accept it
            the way I accept a debt. It is mine to repay, and I will know exactly what it is
            worth." Her hand closes on it, and the hand is not quite steady. "Do not ask me
            to promise anything. Ask me again when I have kept it a while."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Give it to her without a condition.",
                nextNodeId = "the_reliquary_thief_dispersed",
                consequences = Consequences(clearFlags = C7_RUNG_FAMILY, setFlags = setOf("c7_rung_working"))
            ),
            Choice(label = "Tell her where the rest will be kept, and why.", nextNodeId = "the_reliquary_thief_dispersed")
        )
    ),
    StoryNode(
        id = "c16_handing_working",
        chapterId = "chapter_16",
        title = "A Piece for Voss",
        illustrationId = "what_to_protect",
        narrativeText = """
            Voss takes the piece in two fingers, the way she would take a coin she suspected
            of being false, and then closes her hand on it. "Where will the rest be kept?"
            she asks. He tells her. She nods slowly, as she nods at a plan she would have
            made differently and cannot fault. "Then mine is the one that must not be
            found," she says. "Good. I know how to be unfound." She puts it somewhere inside
            her coat. "I will keep it where I keep the things I intend to keep."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Tell her she is the one he trusts most with it.",
                nextNodeId = "the_reliquary_thief_dispersed",
                consequences = Consequences(clearFlags = C7_RUNG_FAMILY, setFlags = setOf("c7_rung_trusted"))
            ),
            Choice(label = "Tell her to go carefully.", nextNodeId = "the_reliquary_thief_dispersed")
        )
    ),
    StoryNode(
        id = "c16_handing_trusted",
        chapterId = "chapter_16",
        title = "A Piece for Voss",
        illustrationId = "what_to_protect",
        narrativeText = """
            Voss says nothing when he holds the piece out. She takes it, and puts it under
            her coat, against her ribs, and lays her hand flat over it for a moment, as one
            does with a thing that might otherwise move. "You did not tell me where the
            rest will be kept," she says. "Good. I would rather not know." She looks at him.
            "If I am ever made to say where this is, you will know before anyone."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "the_reliquary_thief_dispersed"))
    ),
    StoryNode(
        id = "the_reliquary_thief_close",
        chapterId = "chapter_16",
        title = "The Reliquary Thief",
        illustrationId = "the_reliquary_thief",
        narrativeText = """
            Keeping every shard on him means whoever comes for them has to come through him
            first, and that's exactly what happens. Whoever Ilsevet sent this time doesn't move
            like a soldier, no column, no horn, nothing but a shape that shouldn't be able to
            move that quietly this close to Kaelen's own bedroll before he's even awake enough to
            be surprised.

            "You collect what other people fail to hold onto," the thief says, almost admiring,
            drawing a blade shaped for cutting straps rather than throats. "So do I."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reliquary_thief_encounter"
    ),
    StoryNode(
        id = "the_reliquary_thief_dispersed",
        chapterId = "chapter_16",
        title = "The Reliquary Thief",
        illustrationId = "the_reliquary_thief",
        narrativeText = """
            Splitting the shards between keepers means Ilsevet's thief has to gamble on
            which one to hit, and gambles on Stonebeard, the tunnels giving better cover for a
            quiet approach than Fenmoor's open water or Voss's own watchful company. Kaelen
            catches them with a hand already closing around the one shard the hold was trusted
            to keep.

            "You collect what other people fail to hold onto," the thief says, almost admiring,
            drawing a blade shaped for cutting straps rather than throats. "So do I."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reliquary_thief_encounter"
    ),
    StoryNode(
        id = "the_reliquary_thief_luck",
        chapterId = "chapter_16",
        title = "The Reliquary Thief",
        illustrationId = "the_reliquary_thief",
        narrativeText = """
            There was never a plan beyond watching and hoping, and hope turns out to be a thin
            defense against something this quiet. Whoever Ilsevet sent this time doesn't move
            like a soldier, no column, no horn, nothing but a shape that shouldn't be able to
            move that quietly through Stonebeard's own tunnels. Kaelen catches them by nothing
            more than accident, a footstep landing wrong at exactly the right moment.

            "You collect what other people fail to hold onto," the thief says, almost admiring,
            drawing a blade shaped for cutting straps rather than throats. "So do I."
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reliquary_thief_encounter"
    ),
    StoryNode(
        id = "reliquary_thief_defeat",
        chapterId = "chapter_16",
        title = "Outmatched, Not Outnumbered",
        illustrationId = "the_reliquary_thief",
        narrativeText = """
            Whoever trained this thief trained them to disengage the instant a fight stops being
            easy, and Kaelen never gets the chance to make it stop being easy. He's on the
            ground, arm numb from a strike he never saw coming, before he's fully awake enough to
            call it a fight at all.

            The thief doesn't linger to finish what they started. There's no need to. They
            already have what they came for, and Kaelen coming up swinging a moment too late
            doesn't change what's missing when he finally checks.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take stock of the damage.",
                nextNodeId = "what_was_taken",
                consequences = Consequences(setFlags = setOf("lost_to_cinder_reliquary_thief"))
            )
        )
    ),
    StoryNode(
        id = "what_was_taken",
        chapterId = "chapter_16",
        title = "What Was Taken",
        illustrationId = "what_was_taken",
        narrativeText = """
            The thief doesn't win, not quite: whatever training lets someone move that quietly
            through a dwarven hold doesn't help much once the fight stops being subtle. But
            they're fast to run once the balance tips, and Kaelen doesn't catch all of them.

            He still has most of what he came in with. Not all of it. Whatever else Ilsevet's
            reliquary thief walked away with, it was clearly enough to be worth the risk, and
            that alone tells him more than he wanted to know about what she's willing to settle
            for now.
        """.trimIndent(),
        choices = whatItCost("Take stock of the damage.")
    ),
    StoryNode(
        id = "c16_aftermath_open",
        chapterId = "chapter_16",
        title = "How the Thief Came In",
        illustrationId = "what_was_taken",
        narrativeText = """
            It does not take long to learn how the thief came in. The door was open, as
            Kaelen said it would be, and the stair was full of people asleep with their shoes
            on, and a figure that moves quietly can lose itself in a room full of breathing.
            No one else is hurt. Edda counts her people twice and then a third time, and her voice,
            when she gives Kaelen the number, is perfectly level. Tam is sitting by the lamp
            with her knees drawn up. "It went straight past me," she says. "I held the light
            up. It was no use."

            The pieces the thief carried off are more than one, and it will be days before
            anyone says the word "door" without looking at him. At the gate the pressure is
            awake and pressing, as though it already knows what was taken.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter16_end"))
    ),
    StoryNode(
        id = "c16_aftermath_barred",
        chapterId = "chapter_16",
        title = "How the Thief Came In",
        illustrationId = "what_was_taken",
        narrativeText = """
            The door held. That is the first thing anyone says, and for a while it is the
            only thing, because the thief did not come through it. They came by the old
            tunnels, which no bar would have kept shut, and no one else is hurt. But in the
            morning two of Edda's company are standing at the barred door, back from the
            road too late to be let in, and Edda is standing with them, looking at him with
            great politeness. "A reason I can repeat," she says. "I repeated it." Tam says
            nothing, and moves the lamp to where it can be seen from outside.

            The pieces the thief carried off are more than one. At the gate the pressure is
            awake and pressing, as though it already knows what was taken.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter16_end"))
    ),
    StoryNode(
        id = "c16_aftermath_none",
        chapterId = "chapter_16",
        title = "How the Thief Came In",
        illustrationId = "what_was_taken",
        narrativeText = """
            It does not take long to learn how the thief came in, and there is nobody to
            blame it on. The Hold is a place of dwarves and tunnels, and the thief went
            through all of it as if it had been drawn for the purpose. No one else is hurt. The
            smiths stand in the forge door with their arms folded and say the thing smiths
            say when a lock has been picked: that it is a good lock, and a better picker.

            The pieces the thief carried off are more than one. At the gate the pressure is
            awake and pressing, as though it already knows what was taken.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter16_end"))
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
            wrong, either. Whatever Ilsevet assembles next won't be clean, won't be complete,
            and, if the prisoner's fear back at the gate meant anything at all, might not be any
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
