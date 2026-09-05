package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter X — What Ilsevet Came to See.
 *
 * The direct assault Chapter IX's revelation made inevitable: Stonebeard is one of the last two
 * gates Ilsevet needs, so she brings the full weight of the Sanctum to it rather than trying
 * subtlety twice. This is the chapter's biggest structural swing yet — Ilsevet appears in person
 * for the first time, calm and transactional rather than cartoonish, and the siege costs Kaelen
 * the mentor figure who has anchored every chapter since II: Halvard dies holding a breach shut,
 * and passes Kaelen his warden's chain with his last words. Ilsevet herself withdraws once the
 * assault fails rather than press a fight she was only ever testing, leaving the emotional cost
 * to land harder than a combat loss would have. Tenth and toughest combat encounter yet
 * (Ilsevet's Vanguard Captain, 155 HP). The fight and Halvard's death happen regardless of how
 * Kaelen answers Ilsevet's offer, but the three responses (fortifying versus meeting the column
 * head-on beforehand, and defiance versus principle versus silence in the moment itself) now each
 * earn Kaelen a distinct reaction from Ilsevet before the fighting starts, rather than an identical
 * scene with a different stat attached.
 *
 * Judgment call flagged for review: killing off Halvard is the biggest narrative swing since
 * Chapter III introduced an external antagonist. It's played as a genuine loss (not a fake-out)
 * to give the second-act turn toward the finale real weight, and to hand Kaelen tangible narrative
 * momentum (the warden's chain, sole responsibility for Stonebeard) heading into the endgame.
 */
val chapter10Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_last_two",
        chapterId = "chapter_10",
        title = "The Last Two",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Halvard doesn't waste time being surprised anymore, which is its own kind of grim
            comfort. "One of the last two," he says, turning Ordrun's own words over like a coin
            that keeps landing wrong side up. "She's not going to send an envoy for a second try.
            She's going to send everything."

            Voss, still healing, still here rather than gone back to whatever's left of the
            Order, doesn't argue with him. She just starts checking the edge on a borrowed blade,
            the way a person checks something they already know is sharp enough, just to have
            something to do with their hands.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we fortify everything we have left to fortify.\"",
                nextNodeId = "ilsevet_arrives_fortified",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("fortified_for_the_last_stand")
                )
            ),
            Choice(
                label = "\"Then we make sure whoever comes through that tunnel regrets it.\"",
                nextNodeId = "ilsevet_arrives_hard",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("prepared_to_meet_them_hard")
                )
            )
        )
    ),
    StoryNode(
        id = "ilsevet_arrives_fortified",
        chapterId = "chapter_10",
        title = "Ilsevet Arrives",
        illustrationId = "ilsevet_arrives",
        narrativeText = """
            They spend what little time they have left reinforcing everything reinforceable: the
            broken gate braced twice over, every gap Halvard can reach patched with whatever iron
            Stonebeard has left to give. It isn't enough to stop an army. It might be enough to
            slow one down to a fight instead of a slaughter.

            She doesn't come at the front of a column this time either; she doesn't come with one
            at all, not visibly. Ilsevet walks into Stonebeard's braced gate alone, unhurried, the
            way Voss always was and Voss's envoy never quite managed, and behind her the tunnel is
            full of shapes that haven't stepped into the light yet. She takes in the barricades
            with something like professional respect before she speaks.

            She's smaller than the name suggested, and calmer than anyone leading an army has any
            right to be. "Kaelen," she says, like they've met before, though they haven't. "I
            already have four settings filled. I only need one more hand's worth of cooperation
            to save both of us a very long war. Give me the brand willingly, and everyone behind
            you keeps breathing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Answer her.", nextNodeId = "kaelens_answer")
        )
    ),
    StoryNode(
        id = "ilsevet_arrives_hard",
        chapterId = "chapter_10",
        title = "Ilsevet Arrives",
        illustrationId = "ilsevet_arrives",
        narrativeText = """
            There's no time left to build walls, so they spend what little they have sharpening
            themselves instead: blades, nerve, the particular readiness of people who've decided
            retreat isn't on the table. Voss says it plainest: better to meet it standing than
            wait for it crouched.

            She doesn't come at the front of a column this time either; she doesn't come with one
            at all, not visibly. Ilsevet walks into Stonebeard's broken gate alone, unhurried, the
            way Voss always was and Voss's envoy never quite managed, and behind her the tunnel
            is full of shapes that haven't stepped into the light yet. She takes one look at the
            two of them, weapons already drawn, and something that might be amusement crosses her
            face.

            She's smaller than the name suggested, and calmer than anyone leading an army has any
            right to be. "Kaelen," she says, like they've met before, though they haven't. "I
            already have four settings filled. I only need one more hand's worth of cooperation
            to save both of us a very long war. Give me the brand willingly, and everyone behind
            you keeps breathing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Answer her.", nextNodeId = "kaelens_answer")
        )
    ),
    StoryNode(
        id = "kaelens_answer",
        chapterId = "chapter_10",
        title = "Kaelen's Answer",
        illustrationId = "kaelens_answer",
        narrativeText = """
            Whatever she expects, fear, negotiation, a stalling question, isn't what she gets.
            The brand aches on Kaelen's palm, the way it always does when the pressure behind the
            gate takes an interest, and for once that ache feels like the easiest thing in the
            room to trust.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Come and take it, then.\"",
                nextNodeId = "the_gate_falls_defiant",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_ilsevet")
                )
            ),
            Choice(
                label = "\"Whatever you're building, it doesn't get built with what's mine.\"",
                nextNodeId = "the_gate_falls_principled",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_on_principle")
                )
            ),
            Choice(
                label = "Say nothing. Draw your sword instead.",
                nextNodeId = "the_gate_falls_silent",
                consequences = Consequences(setFlags = setOf("answered_with_steel"))
            )
        )
    ),
    StoryNode(
        id = "the_gate_falls_defiant",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Something that isn't quite a smile touches Ilsevet's mouth, the expression of a woman
            noting a data point rather than reacting to an insult. "Bold," she says. "I've had
            bolder tell me the same thing and mean it a great deal less." She doesn't draw a
            weapon herself; she simply steps back, unbothered, and lets the shapes behind her
            answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "the_gate_falls_principled",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Ilsevet considers that longer than she considered the first answer would have
            warranted, head tilted slightly, as though weighing whether principle is a tool she
            can use or simply an obstacle to route around. "Principle," she says, at last.
            "That's a rarer answer than defiance, down here. It won't change what happens next."
            She doesn't draw a weapon herself; she simply steps back, unbothered, and lets the
            shapes behind her answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "the_gate_falls_silent",
        chapterId = "chapter_10",
        title = "The Gate Falls",
        illustrationId = "the_gate_falls",
        narrativeText = """
            Kaelen's sword clears its sheath before he's decided on a single word to go with it,
            and something in Ilsevet's calm falters, just slightly, at an answer she can't file
            away as easily as speech. "No parting words," she says, almost to herself.
            "Interesting." She doesn't draw a weapon herself; she simply steps back, unbothered,
            and lets the shapes behind her answer instead.

            Her vanguard captain comes through the broken gate first, armored head to foot in the
            same black-grey as everything else that answers to the cinder brand, and doesn't
            bother with words at all.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "stonebeard_siege_encounter"
    ),
    StoryNode(
        id = "halvard_falls",
        chapterId = "chapter_10",
        title = "Halvard Falls",
        illustrationId = "halvard_falls",
        narrativeText = """
            The captain goes down, but not before the fight spills past Kaelen entirely: a second
            wave crashing through the gap in the wall Halvard's been patching with iron that
            never quite matched the stone. Halvard throws himself into the gap rather than let it
            widen, an old dwarf's whole weight against a door built for younger hands, and takes
            a blow meant for the stone instead of him.

            He doesn't fall right away. He holds the gap shut long enough for the rest of the
            wave to break against it, and only lets go once there's nothing left on the other
            side to hold it shut against, as though even dying were something he'd rather finish
            properly.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go to him.", nextNodeId = "what_halvard_leaves_behind")
        )
    ),
    StoryNode(
        id = "what_halvard_leaves_behind",
        chapterId = "chapter_10",
        title = "What Halvard Leaves Behind",
        illustrationId = "what_halvard_leaves_behind",
        narrativeText = """
            He's still breathing when Kaelen reaches him, which is its own small, cruel mercy:
            long enough for one more thing to be said. His hand finds the warden's chain at his
            own throat, tarnished silver-iron, six links where there should have been six
            wardens.

            "Wear it, or don't. Wasn't ever really about the chain." His grip tightens once,
            weaker than it should be, stubborn right up to the edge of it. "Ilsevet didn't come
            here to kill you, lad. She came to see if she could still lose. Make sure the answer
            stays yes."

            He doesn't say anything else, though his hand stays closed around Kaelen's a moment
            longer than it needs to.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the chain.",
                nextNodeId = "chapter10_end",
                consequences = Consequences(
                    grantItemIds = listOf("halvards_warden_chain"),
                    setFlags = setOf("halvard_died", "took_the_warden_chain")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter10_end",
        chapterId = "chapter_10",
        title = "End of Chapter X — What Ilsevet Came to See",
        illustrationId = "chapter10_threshold",
        narrativeText = """
            Ilsevet is gone before the dust settles, the way she arrived: unhurried, untroubled,
            having lost a captain and a great deal of a garrison for a hand's worth of cooperation
            she didn't get. Kaelen doesn't imagine that particular arithmetic troubles her for
            long.

            Stonebeard buries its dead the dwarven way, deep and unmarked, and Kaelen stands over
            one grave longer than the rest. Voss doesn't offer comfort. She just stays, which is
            its own kind of answer to a question neither of them asks aloud.

            Somewhere north or south or some direction with a name Kaelen still doesn't know, one
            gate stands between Ilsevet and everything her seventh door was built for. He intends
            to find it before she does, and to make it cost her more than the last one did.

            Chapter XI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_chain_speaks")
        )
    )
)

val chapter10 = com.thelastjailer.app.Chapter(
    id = "chapter_10",
    number = 10,
    title = "Chapter X — What Ilsevet Came to See",
    startNodeId = "the_last_two"
)
