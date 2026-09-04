package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter II — The Last Jailer.
 *
 * Picks up immediately after First Blood and pays off the game's title: Stonebeard Hold was never
 * just a mining enclave, it is the last watch-post over a far older prison, and the office of
 * "Last Jailer" that once stood guard over it ran out of hands to fill it generations ago. Arc:
 * an aftermath choice that genuinely forks (push on alone or go back for the Hold first, each with
 * its own scene before they rejoin), the reveal of the warded gate, a warden who has been alone
 * with this knowledge for too long (and answers a direct question differently than an offer of
 * help), a truth about where Kaelen's old order actually came from, a seal beginning to fail, a
 * harder fight than the last one, and a chapter-ending choice that doesn't resolve so much as set
 * the terms for what Chapter III has to answer.
 */
val chapter2Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_deeper_dark",
        chapterId = "chapter_2",
        title = "The Deeper Dark",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The thing that attacked him stops moving, and for a long while Kaelen's own breath is
            the loudest sound in the passage. He checks it twice before he trusts that. His sword
            arm is still shaking, now that there's nothing left to swing it at.

            Beyond where it fell, the tunnel keeps going, and the dwarven work stops with it. The
            chisel marks that guided him this far simply end, mid-stroke, as if whoever cut them
            had stopped one day and never come back to finish. What replaces them is older and far
            smoother, no tool marks at all, and it does not feel abandoned. It feels swept. Tended,
            the way a path gets tended by someone who still walks it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Follow the passage deeper.",
                nextNodeId = "the_tended_dark",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_deeper")
                )
            ),
            Choice(
                label = "Carry word back to Stonebeard Hold first.",
                nextNodeId = "word_at_stonebeard",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("returned_to_hold")
                )
            )
        )
    ),
    StoryNode(
        id = "the_tended_dark",
        chapterId = "chapter_2",
        title = "What Tends the Dark",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            Kaelen goes on alone, and the going gets stranger the farther he walks. No rubble
            underfoot. No old bones, no rust, none of the small human wreckage that litters every
            mine he's ever walked through in his life. Just smooth stone, and a silence with a
            texture to it, the kind that makes a man start listening hard for whatever is
            producing it.

            He doesn't find an answer. He finds a feeling instead, settling somewhere behind his
            sternum: that going forward alone was either very brave or very stupid, and that he
            won't know which one until it's too late to matter. He keeps walking anyway. Down here,
            it's the only direction left that's still his to choose.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press on toward whatever waits at the end of it.", nextNodeId = "the_warded_gate")
        )
    ),
    StoryNode(
        id = "word_at_stonebeard",
        chapterId = "chapter_2",
        title = "Word at Stonebeard Hold",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Stonebeard Hold looks smaller by lamplight than it did mid-fight: just a handful of
            dwarves picking through what the thing in the tunnels wrecked, and they go quiet when
            Kaelen tells them what he found below. A passage that isn't theirs. Tended by something
            that clearly doesn't want visitors.

            An old smith listens longest and says the least, then presses a coil of rope and a
            second lantern into Kaelen's hands without being asked to. "Rather you carried light
            back down there than pride," is the only explanation he offers, and doesn't offer any
            more when Kaelen waits for one. Nobody volunteers to come with him. Nobody tells him
            not to go, either. Down here, that's about as close to a blessing as the Hold has left
            to give.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go back down, better armed for it.",
                nextNodeId = "the_warded_gate",
                consequences = Consequences(grantItemIds = listOf("stonebeard_lantern"))
            )
        )
    ),
    StoryNode(
        id = "the_warded_gate",
        chapterId = "chapter_2",
        title = "The Warded Gate",
        illustrationId = "warded_gate",
        narrativeText = """
            The tended passage ends at a gate that dwarfs Stonebeard Hold's own: no hinge, no
            handle, only a wall of black stone banded in iron and carved edge to edge with wards.

            More than half of them have gone dark. The rest flicker, the way a candle flickers in
            a room with a door left open somewhere it shouldn't be. Cold rolls off the stone in
            slow waves, not the ordinary cold of deep rock but something with intention behind it,
            like breath held on the far side of a very thin wall.

            Kaelen has seen wards before, on old King's Guard armories, gold-worked things meant to
            be admired as much as used. He has never seen this many in one place. He has never
            seen this many, this tired.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press a hand to the failing stone.", nextNodeId = "the_warden_elder")
        )
    ),
    StoryNode(
        id = "the_warden_elder",
        chapterId = "chapter_2",
        title = "Halvard, Last of the Wardens",
        illustrationId = "warden_chamber",
        narrativeText = """
            "Careful. It answers touch." The voice comes from behind him, dry as old rope and
            utterly unsurprised.

            An ancient dwarf leans in the passage mouth, too old by the look of him to have swung
            a hammer in years, a warden's chain of tarnished silver-iron hanging loose around a
            neck too thin for it now. "Halvard," he says, before Kaelen can ask. "Last of the
            wardens of this gate. There were meant to be six of us. There has been one of me for a
            very long time, and the stone has started noticing the difference."

            He doesn't look surprised to find a stranger standing where no stranger has any
            business being. He looks like a man who has been waiting for anyone at all, for so
            long he half forgot what waiting was supposed to feel like.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What are you guarding?\"",
                nextNodeId = "the_cloaks_truth_asked",
                consequences = Consequences(setFlags = setOf("asked_about_jailers"))
            ),
            Choice(
                label = "\"I'll help you hold it, whatever it is.\"",
                nextNodeId = "the_cloaks_truth_offered",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("offered_to_help")
                )
            )
        )
    ),
    StoryNode(
        id = "the_cloaks_truth_asked",
        chapterId = "chapter_2",
        title = "What the Silver Cloak Was For",
        illustrationId = "cloaks_truth",
        narrativeText = """
            Halvard studies him a moment before answering, the way a man checks a stranger's hands
            before he bothers checking his face. "A prison," he says finally. "Older than the
            Hold. Older than the tree above it, I'd wager, though trees don't usually grow on
            purpose over something like this. My kin have wardened the inside of that gate for
            longer than your kingdom has had a name for itself. Your King's Guard wardened the
            outside of it, once, long before it was a king's anything. The last jailer stood
            between us both and whatever's in there, and answered to neither."

            He doesn't soften it for a stranger's benefit. If anything, the answer comes out
            flatter for having been asked so plainly, a man reciting a debt rather than confessing
            to one. "The line of jailers ended before you were born. I don't know what you are,
            only that you asked instead of assuming, which is more than the last few who found
            this door managed. I know what's failing behind me. I know I can't hold it alone
            anymore."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Show him the tarnished token from the roots.",
                nextNodeId = "the_first_seal",
                requirements = ChoiceRequirement(requiredFlags = setOf("kept_the_token")),
                consequences = Consequences(
                    setFlags = setOf("revealed_token"),
                    unlockTrophy = "Blood and Silver"
                )
            ),
            Choice(
                label = "Say nothing of the road behind you.",
                nextNodeId = "the_first_seal"
            )
        )
    ),
    StoryNode(
        id = "the_cloaks_truth_offered",
        chapterId = "chapter_2",
        title = "What the Silver Cloak Was For",
        illustrationId = "cloaks_truth",
        narrativeText = """
            Something crosses Halvard's face before he answers, quick enough that Kaelen almost
            misses it: not suspicion, but the particular startlement of a man who stopped
            expecting an offer a long time ago and has to remember how to accept one.

            "That's not nothing, from a stranger," he says, quieter than before. "A prison. Older
            than the Hold, older than the tree above it, I'd wager, though trees don't usually
            grow on purpose over something like this. My kin have wardened the inside of that gate
            longer than your kingdom has had a name for itself. Your King's Guard wardened the
            outside of it, once, long before it was a king's anything. The last jailer stood
            between us both and whatever's in there, and answered to neither."

            He studies Kaelen's ruined bearing, the sword that has seen too much use for a man
            with no colours left to wear, and doesn't ask what happened to either. "The line of
            jailers ended before you were born. I don't know what you are. I know what's failing
            behind me, and until a moment ago I'd stopped believing anyone would offer to help
            carry it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Show him the tarnished token from the roots.",
                nextNodeId = "the_first_seal",
                requirements = ChoiceRequirement(requiredFlags = setOf("kept_the_token")),
                consequences = Consequences(
                    setFlags = setOf("revealed_token"),
                    unlockTrophy = "Blood and Silver"
                )
            ),
            Choice(
                label = "Say nothing of the road behind you.",
                nextNodeId = "the_first_seal"
            )
        )
    ),
    StoryNode(
        id = "the_first_seal",
        chapterId = "chapter_2",
        title = "The First Seal",
        illustrationId = "first_seal",
        narrativeText = """
            A ward near the base of the gate gutters out entirely. Somewhere on the other side of
            the black stone, something registers the loss at once: a pressure, a patience, a
            weight leaning slowly into the place where the light just went out, testing it the way
            a man tests a floorboard before he trusts it with his full weight.

            Halvard is already moving, murmuring words in a dwarvish older than the ones carved
            into Stonebeard's own gate, his hands never quite steady and never quite stopping
            either. He doesn't ask Kaelen for help. He doesn't have time to, and some part of him,
            Kaelen suspects, has simply forgotten what it's like to have help to ask for.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Brace the failing stone yourself.",
                nextNodeId = "what_presses_against_the_seal",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("warden_sigil"),
                    setFlags = setOf("braced_the_seal")
                )
            ),
            Choice(
                label = "Stand guard and let Halvard work.",
                nextNodeId = "what_presses_against_the_seal",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("guarded_the_warden")
                )
            )
        )
    ),
    StoryNode(
        id = "what_presses_against_the_seal",
        chapterId = "chapter_2",
        title = "What Presses Against the Seal",
        illustrationId = "seal_breaker_ambush",
        narrativeText = """
            The dark ward doesn't just go out; it gives way, and something that has been pressed
            thin against the inside of the gate for longer than Halvard has been alive finds
            enough of a gap to pour through.

            It has no edges worth naming, no shape Kaelen's eyes can hold onto for more than a
            heartbeat at a time. It has weight, and hunger, and it has clearly been practicing
            patience for centuries, the way water practices patience against stone. Kaelen's sword
            feels very small in his hand, and for the first time since the black door, so does he.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "seal_breaker_encounter"
    ),
    StoryNode(
        id = "chapter2_end",
        chapterId = "chapter_2",
        title = "End of Chapter II — The Weight of the Office",
        illustrationId = "chapter2_threshold",
        narrativeText = """
            The gap closes. The ward does not relight; Halvard says quietly that it can't, not
            anymore, not without a jailer to anchor it, but the gate holds for now, the way a dam
            holds after the flood has already found the crack and hasn't yet decided to widen it.

            Halvard doesn't thank him. He just looks at Kaelen the way a man looks at the only
            other pair of hands available to help carry something too heavy for one set of arms
            alone.

            "The office is yours if you want it," he says. "It was never about the cloak. Never
            about the crown either, whatever your old order came to believe. It's about standing
            here when the light goes out, and having somewhere else to stand isn't the same thing
            as having somewhere else you'd rather be."

            Kaelen doesn't answer right away. He doesn't leave, either, and after a while that
            starts to feel like an answer of its own.

            Chapter III awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Give him your answer.", nextNodeId = "the_answer")
        )
    )
)

val chapter2 = com.thelastjailer.app.Chapter(
    id = "chapter_2",
    number = 2,
    title = "Chapter II — The Last Jailer",
    startNodeId = "the_deeper_dark"
)
