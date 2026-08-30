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
 * an aftermath choice, the reveal of the warded gate, a warden who has been alone with this
 * knowledge for too long, a truth about where Kaelen's old order actually came from, a seal
 * beginning to fail, a harder fight than the last one, and a chapter-ending choice that doesn't
 * resolve so much as set the terms for what Chapter III has to answer.
 */
val chapter2Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_deeper_dark",
        chapterId = "chapter_2",
        title = "The Deeper Dark",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The thing that attacked him stops moving. Kaelen's breath is the loudest sound in the
            passage for a long while.

            Beyond where it fell, the tunnel keeps going — not dwarven work anymore. The chisel
            marks stop. What replaces them is older, smoother, and it does not feel abandoned. It
            feels tended.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Follow the passage deeper.",
                nextNodeId = "the_warded_gate",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_deeper")
                )
            ),
            Choice(
                label = "Carry word back to Stonebeard Hold first.",
                nextNodeId = "the_warded_gate",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("returned_to_hold")
                )
            )
        )
    ),
    StoryNode(
        id = "the_warded_gate",
        chapterId = "chapter_2",
        title = "The Warded Gate",
        illustrationId = "warded_gate",
        narrativeText = """
            The tended passage ends at a gate that dwarfs Stonebeard Hold's own — no hinge, no
            handle, only a wall of black stone banded in iron and carved edge to edge with wards.

            More than half of them have gone dark. The rest flicker, the way a candle flickers
            in a room with a door left open somewhere it shouldn't be.

            Kaelen has seen wards before, on old King's Guard armories. He has never seen this many,
            or this tired.
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
            A voice behind him, dry as old rope: "Careful. It answers touch."

            An ancient dwarf leans in the passage mouth, too old to have swung a hammer in years,
            a warden's chain of tarnished silver-iron still around his neck. "Halvard," he says,
            when Kaelen doesn't ask. "Last of the wardens of this gate. There were meant to be six
            of us. There has been one of me for a very long time."

            He does not look surprised to see a stranger here. He looks like a man who has been
            waiting for anyone at all.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What are you guarding?\"",
                nextNodeId = "the_cloaks_truth",
                consequences = Consequences(setFlags = setOf("asked_about_jailers"))
            ),
            Choice(
                label = "\"I'll help you hold it, whatever it is.\"",
                nextNodeId = "the_cloaks_truth",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("offered_to_help")
                )
            )
        )
    ),
    StoryNode(
        id = "the_cloaks_truth",
        chapterId = "chapter_2",
        title = "What the Silver Cloak Was For",
        illustrationId = "cloaks_truth",
        narrativeText = """
            "A prison," Halvard says. "Older than the Hold. Older than the tree above it, I'd
            wager, though trees don't usually grow on purpose over something like this. My kin
            have wardened the inside of that gate for longer than your kingdom has had a name for
            itself. Your King's Guard wardened the outside of it, once — long before it was a
            king's anything. The last jailer stood between us both and whatever's in there, and
            answered to neither."

            He studies Kaelen's ruined bearing, the sword that has seen too much use for a man
            with no colours left to wear. "The line of jailers ended before you were born. I don't
            know what you are. I know what's failing behind me, and I know I can't hold it alone
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
        id = "the_first_seal",
        chapterId = "chapter_2",
        title = "The First Seal",
        illustrationId = "first_seal",
        narrativeText = """
            A ward near the base of the gate gutters out entirely. Somewhere on the other side of
            the black stone, something registers the loss immediately — a pressure, a patience,
            a weight leaning into the place where the light just went out.

            Halvard is already moving, murmuring words in a dwarvish older than the ones carved
            into Stonebeard's own gate. He doesn't ask Kaelen for help. He doesn't have time to.
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
            The dark ward doesn't just go out — it gives way, and something that has been pressed
            thin against the inside of the gate for longer than Halvard has been alive finds
            enough of a gap to pour through it.

            It has no edges worth naming. It has weight, and hunger, and it has clearly been
            practicing patience for centuries. Kaelen's sword feels very small.
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
            The gap closes. The ward does not relight — Halvard says, quietly, that it can't,
            not anymore, not without a jailer to anchor it — but the gate holds, for now, the way
            a dam holds after the flood has already found the crack.

            Halvard doesn't thank him. He just looks at Kaelen the way a man looks at the only
            other hand available to help carry something too heavy for one pair of arms.

            "The office is yours if you want it," he says. "It was never about the cloak. It was
            never about the crown, either, whatever your old order came to believe. It's about
            standing here when the light goes out."

            Kaelen doesn't answer. Not yet. But he doesn't leave, either.

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
