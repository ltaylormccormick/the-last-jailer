package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XI — The Sixth Name.
 *
 * Turns Kaelen's vow at the end of X into action: find the last unaccounted gate before Ilsevet
 * does. The warden's chain he inherited from Halvard gives him a dream of its location (Fenmoor, a
 * drowned marsh gate), which Voss corroborates from old Order garrison records — a deliberate
 * narrative rhyme with Chapter II, where Halvard was the one holding a failing gate alone and
 * Kaelen was the outsider who chose to help. Here Kaelen and Voss arrive just ahead of an Ilsevet
 * extraction team already trying to force Fenmoor's living warden, Thessaly, to give up her own
 * binding. Arc: the vision, Voss's confirmation, the road to Fenmoor, the standoff, an eleventh and
 * toughest-yet combat encounter, and a stat-gated choice about what Kaelen owes Thessaly going
 * forward — echoing III/IV/V/VI/VII/VIII/IX's aftermath-choice pattern (X deliberately skipped it).
 * Ends on the first real tactical win in the Ilsevet arc: for the first time since the missive, the
 * math favors Kaelen rather than her.
 */
val chapter11Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_chain_speaks",
        chapterId = "chapter_11",
        title = "The Chain Speaks",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Kaelen doesn't mean to fall asleep wearing it. The warden's chain isn't heavy, not
            really, but it sits differently than any weight he's carried before — six links, six
            names Halvard never got to teach him.

            He dreams anyway. Not of Halvard. Of water — grey, tidal, wrapping around black stone
            the same shape as the door beneath the ancient tree, except this one stands
            half-drowned in a marsh that has no business being this far from any coast. When he
            wakes, the shape of it hasn't faded the way dreams usually do.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I think I know where the last gate is.\"",
                nextNodeId = "voss_confirms_it",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("trusted_the_vision")
                )
            ),
            Choice(
                label = "Sit with it a while longer before saying anything.",
                nextNodeId = "voss_confirms_it",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("doubted_the_vision")
                )
            )
        )
    ),
    StoryNode(
        id = "voss_confirms_it",
        chapterId = "chapter_11",
        title = "Voss Confirms It",
        illustrationId = "voss_confirms_it",
        narrativeText = """
            She doesn't laugh at a vision the way he half-expects her to. "Fenmoor," she says
            instead, the name landing like she's been waiting for someone else to say it first.
            "There's an old Order garrison posted near there, or there was. We always wondered why
            headquarters kept funding a watch on a marsh nobody wanted. Now I suppose we know."

            If Kaelen dreamed it and Voss half-remembers funding it, that's more agreement than
            either of them expected to get.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to Fenmoor.", nextNodeId = "the_road_to_fenmoor")
        )
    ),
    StoryNode(
        id = "the_road_to_fenmoor",
        chapterId = "chapter_11",
        title = "The Road to Fenmoor",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Fenmoor turns out to be exactly as unwelcoming as its name promises — flat grey water
            under a flatter grey sky, reeds taller than a man, and a smell that never quite goes
            away. Somewhere in the middle of it, if the dream is worth trusting, the last gate
            that isn't Kaelen's own is still standing.

            They're not the only ones who came looking. Fresh tracks, too many of them, cut
            straight through the reeds toward the water's heart.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Follow them.", nextNodeId = "the_drowned_ward")
        )
    ),
    StoryNode(
        id = "the_drowned_ward",
        chapterId = "chapter_11",
        title = "The Drowned Ward",
        illustrationId = "the_drowned_ward",
        narrativeText = """
            The gate stands exactly where the dream put it — half-sunk, wards guttering weakly
            above the waterline, and a lone warden standing between it and four cinder-grey
            figures who clearly didn't come to ask permission. She's holding, barely, a spear in
            one hand and something that might be a prayer in the other.

            "Thessaly," she snaps, not turning, when Kaelen and Voss come crashing through the
            reeds behind her. "Warden of Fenmoor. You're either here to help or you're the next
            problem — pick fast."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Fight beside her.", nextNodeId = "the_marsh_holds")
        )
    ),
    StoryNode(
        id = "the_marsh_holds",
        chapterId = "chapter_11",
        title = "The Marsh Holds",
        illustrationId = "the_marsh_holds",
        narrativeText = """
            Their leader doesn't bother with words either — Ilsevet's people have stopped
            pretending diplomacy is anything but a formality reserved for jailers she'd rather not
            fight twice. Whatever's in the case on her back, she's not putting it down until
            Fenmoor's ward is inside it.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "fenmoor_extraction_encounter"
    ),
    StoryNode(
        id = "what_thessaly_decides",
        chapterId = "chapter_11",
        title = "What Thessaly Decides",
        illustrationId = "what_thessaly_decides",
        narrativeText = """
            They win the marsh, if not the war — the extraction team breaks and scatters into the
            reeds rather than press a fight that's turned against them. Thessaly doesn't thank
            anyone. She just watches the water settle and asks the only question that matters.
            "That's twice today someone's tried to take what I'm holding. What happens the third
            time, when I'm alone again?"
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"You're not alone again. I'll send whoever Stonebeard can spare.\"",
                nextNodeId = "chapter11_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 9)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("fenmoor_ward_shard"),
                    setFlags = setOf("pledged_support_to_fenmoor")
                )
            ),
            Choice(
                label = "\"Come back with us. Two wardens standing together are harder to take than one.\"",
                nextNodeId = "chapter11_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 8)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("fenmoor_ward_shard"),
                    setFlags = setOf("invited_thessaly_to_stonebeard")
                )
            ),
            Choice(
                label = "\"I don't have an answer for that yet. But I know now, and so does Halvard's memory.\"",
                nextNodeId = "chapter11_end",
                consequences = Consequences(
                    grantItemIds = listOf("fenmoor_ward_shard"),
                    setFlags = setOf("left_fenmoor_uncertain")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter11_end",
        chapterId = "chapter_11",
        title = "End of Chapter XI — The Sixth Name",
        illustrationId = "chapter11_threshold",
        narrativeText = """
            They make the long walk back from Fenmoor with the marsh's damp still in their boots,
            and for the first time since the missive, the math has changed in Kaelen's favor
            rather than against it: both remaining gates held, both wardens warned, both doors
            still shut.

            It isn't victory. Ilsevet has four settings and two names left she can't reach easily
            anymore, which only means the easy part of her plan is over. Voss says as much,
            quietly, somewhere past the halfway point home. Kaelen doesn't disagree.

            Chapter XII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep walking.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter11 = com.thelastjailer.app.Chapter(
    id = "chapter_11",
    number = 11,
    title = "Chapter XI — The Sixth Name",
    startNodeId = "the_chain_speaks"
)
