package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XII — Four Settings.
 *
 * Ilsevet, denied both remaining gates in Chapter XI, tries activating the seventh-door frame with
 * only the four settings she already has — and it nearly destroys the Sanctum. Kaelen and Voss go
 * to help contain it, which puts them in the position of saving the woman who has tried twice to
 * take the brand by force. The chapter deliberately subverts the "siege" pattern of III/IV/V/VII/
 * IX/X: Ilsevet isn't attacking this time, she's overwhelmed, and the fight is against what tore
 * loose from her own failed device rather than against her people. Twelfth and toughest combat
 * encounter yet (The Unfinished, 175 HP — a non-humanoid threat, echoing VI's tonal shift). Ends
 * with proof, for both Kaelen and Ilsevet, of exactly what a completed seventh door would do.
 *
 * Judgment call flagged for review: this is a real, if brief, sympathetic beat for Ilsevet — she
 * asks for help rather than fighting, and Kaelen chooses to give it. It's a deliberate tonal
 * complication rather than a redemption arc (she remains the antagonist going into XIII), meant to
 * keep her a person making a catastrophic bet rather than a simple villain, and to let the
 * player's response to her genuinely vary by playstyle in the closing choice.
 */
val chapter12Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "word_from_ashfall",
        chapterId = "chapter_12",
        title = "Word from Ashfall",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            It's one of Voss's old Order contacts who brings the word, riding hard enough to
            nearly founder his horse at Stonebeard's gate — the kind of messenger who doesn't
            dismount so much as fall off deliberately. "The Sanctum," he manages, between
            breaths. "Something's gone wrong there. Badly wrong. Half the ash-field's on fire
            that shouldn't burn, and whatever she built isn't staying where she put it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we go.\"",
                nextNodeId = "the_call_to_go",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("answered_the_word_at_once")
                )
            ),
            Choice(
                label = "\"Tell me everything you know first.\"",
                nextNodeId = "the_call_to_go",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_before_answering")
                )
            )
        )
    ),
    StoryNode(
        id = "the_call_to_go",
        chapterId = "chapter_12",
        title = "The Call to Go",
        illustrationId = "the_call_to_go",
        narrativeText = """
            Voss doesn't pretend the decision is simple. "She tried to kill you twice. Take your
            brand by force, once by knife and once by siege. And now something she built is loose
            and she can't put it back." She doesn't finish the thought out loud. She doesn't have
            to.

            Kaelen thinks of Halvard holding a gap shut with his whole body rather than let
            anything through it, willing or not, enemy or not. That answer, at least, isn't
            complicated.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go.", nextNodeId = "back_to_the_ash")
        )
    ),
    StoryNode(
        id = "back_to_the_ash",
        chapterId = "chapter_12",
        title = "Back to the Ash",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The Ashfall reaches look different at speed and under smoke than they did the last two
            times Kaelen crossed them — the spire that used to hum is visible from twice the
            distance now, wrong light bleeding out of cracks that shouldn't exist in stone that
            old.

            Whatever's loose, it hasn't left the Sanctum's walls yet. That's the only mercy on
            offer.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go in.", nextNodeId = "what_the_frame_did")
        )
    ),
    StoryNode(
        id = "what_the_frame_did",
        chapterId = "chapter_12",
        title = "What the Frame Did",
        illustrationId = "what_the_frame_did",
        narrativeText = """
            Four settings out of six was apparently enough to open something, if not enough to
            control it. The frame's chamber is a ruin, cinder-grey robes scattered and unmoving
            among the wreckage, and at the center of it Ilsevet herself — no guards, no vanguard,
            just her, bracing a ward that's failing in her own two hands the exact same way
            Halvard once braced one in his.

            She doesn't look surprised to see him. She looks, for the first time since Kaelen has
            known her, out of options. "I wasn't expecting you to come," she says. "I was hoping
            you might anyway."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Help her hold it.", nextNodeId = "the_unfinished_thing")
        )
    ),
    StoryNode(
        id = "the_unfinished_thing",
        chapterId = "chapter_12",
        title = "The Unfinished Thing",
        illustrationId = "the_unfinished_thing",
        narrativeText = """
            It doesn't have a shape so much as an argument about what shape it should have, pieces
            of six different wards fighting over one body that was never meant to hold all of them
            at once. It notices Kaelen the way flame notices air — not as an enemy exactly, just as
            something it hasn't finished consuming yet.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "unfinished_thing_encounter"
    ),
    StoryNode(
        id = "what_kaelen_does_with_her",
        chapterId = "chapter_12",
        title = "What Kaelen Does With Her",
        illustrationId = "what_kaelen_does_with_her",
        narrativeText = """
            It goes still, eventually, the argument between six half-made wards finally losing
            enough coherence to stop being a threat. Ilsevet doesn't thank him — she looks like a
            woman doing arithmetic she doesn't like the answer to, over and over, in the wreckage
            of the only thing she's built in years.

            "Four settings," she says, mostly to herself. "Four, and it nearly took the whole
            Sanctum with it. I don't know what six would have done." She looks at Kaelen like the
            question underneath that sentence is the only one left worth asking.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then you already have your answer. Stop before six finishes the job.\"",
                nextNodeId = "chapter12_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 10)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    setFlags = setOf("warned_ilsevet_directly")
                )
            ),
            Choice(
                label = "\"Whatever this cost you, it isn't worth what it almost cost everyone else.\"",
                nextNodeId = "chapter12_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 9)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    setFlags = setOf("appealed_to_ilsevets_conscience")
                )
            ),
            Choice(
                label = "Say nothing. Take a fragment of the broken frame, and leave her to what's left.",
                nextNodeId = "chapter12_end",
                consequences = Consequences(
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    setFlags = setOf("left_ilsevet_to_her_wreckage")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter12_end",
        chapterId = "chapter_12",
        title = "End of Chapter XII — Four Settings",
        illustrationId = "chapter12_threshold",
        narrativeText = """
            Nobody stops Kaelen and Voss on the way out. There's nobody left in fighting shape to
            try.

            The fragment of the frame sits wrong in Kaelen's pack the whole walk home — warm when
            it should be cold, humming faintly the way the spire used to hum from a distance. Voss
            doesn't ask to hold it. Neither of them says Ilsevet's name again until Stonebeard's
            tunnel swallows them whole.

            Four settings very nearly ended everything in that chamber. Two settings remain
            unaccounted for by her hand, and Kaelen has both of them. Whatever Ilsevet decides to
            do next, she now knows exactly what happens if she gets what she's been asking for.

            That should feel like more of a victory than it does.

            Chapter XIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter12 = com.thelastjailer.app.Chapter(
    id = "chapter_12",
    number = 12,
    title = "Chapter XII — Four Settings",
    startNodeId = "word_from_ashfall"
)
