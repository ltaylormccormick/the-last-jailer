package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XV — Old Debts.
 *
 * Deepens Ilsevet without softening the threat she poses. Selvane's Order archives reveal her
 * history: fifteen years ago she lost someone at a minor, under-reinforced ward that failed
 * completely rather than gradually, and concluded that eternal partial containment is a promise
 * no line of jailers can actually keep forever — the seventh door, in her own accounting, isn't a
 * weapon so much as an attempt to end the danger permanently rather than merely outlast it. Reading
 * that record alone first versus reading it together with Voss changes how the reveal actually
 * lands, and the stance Kaelen settles on afterward now colors the tone he carries into the fight
 * that follows, not just which flag it leaves behind. This doesn't recontextualize her as right,
 * but it does make her legible, continuing XII's approach of treating her as a person making a
 * catastrophic bet rather than a simple villain. The chapter's escalation is personal rather than
 * territorial: a reprisal squad targets Voss directly for her role in Kaelen's continued success,
 * not another attempt on the gates. Fifteenth and toughest combat encounter yet (Cinder Reprisal
 * Squad Leader, 205 HP).
 */
val chapter15Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_selvane_sends",
        chapterId = "chapter_15",
        title = "What Selvane Sends",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            No rider this time, just a locked dispatch case, delivered by a courier who doesn't
            wait for a reply and won't say who sent it beyond "the Inquisitor-General thought the
            jailer should have this." Inside: Order archive pages, decades old, water-stained, and
            a short unsigned note. You asked what she was before. This is the closest the Order's
            records come to an answer.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Read it alone first.",
                nextNodeId = "the_woman_before_ilsevet_alone",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("read_alone_first")
                )
            ),
            Choice(
                label = "Read it with Voss.",
                nextNodeId = "the_woman_before_ilsevet_with_voss",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("read_with_voss")
                )
            )
        )
    ),
    StoryNode(
        id = "the_woman_before_ilsevet_alone",
        chapterId = "chapter_15",
        title = "The Woman Before Ilsevet",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Kaelen waits until he's alone to break the seal, some instinct telling him this is the
            kind of reading better done without an audience for the first pass, even a trusted
            one.

            Fifteen years ago, before "Ilsevet" meant anything to anyone outside a garrison
            roster, she was posted to a lesser ward in the Ashwell hills, not one of the six,
            nothing the Order considered urgent, just a minor containment nobody had thought worth
            reinforcing in a generation. It failed anyway. Whatever it held got out in full, not
            in pieces, and it cost her someone the records only ever call "her second."

            The report ends there. Whatever happened to Ashwell's ward after that isn't written
            down, or wasn't kept. He reads it twice more before he decides Voss needs to see it
            too.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep reading.",
                nextNodeId = "what_it_made_of_her",
                consequences = Consequences(grantItemIds = listOf("record_of_ashwell"))
            )
        )
    ),
    StoryNode(
        id = "the_woman_before_ilsevet_with_voss",
        chapterId = "chapter_15",
        title = "The Woman Before Ilsevet",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Kaelen breaks the seal with Voss already looking over his shoulder, both of them
            reading at the same pace, neither willing to be the one who finishes first and has to
            wait for the other.

            Fifteen years ago, before "Ilsevet" meant anything to anyone outside a garrison
            roster, she was posted to a lesser ward in the Ashwell hills, not one of the six,
            nothing the Order considered urgent, just a minor containment nobody had thought worth
            reinforcing in a generation. It failed anyway. Whatever it held got out in full, not
            in pieces, and it cost her someone the records only ever call "her second."

            The report ends there. Whatever happened to Ashwell's ward after that isn't written
            down, or wasn't kept. Voss goes very still beside him, the kind of stillness that
            means she's recognized something in the account he hasn't caught yet.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep reading.",
                nextNodeId = "what_it_made_of_her",
                consequences = Consequences(grantItemIds = listOf("record_of_ashwell"))
            )
        )
    ),
    StoryNode(
        id = "what_it_made_of_her",
        chapterId = "chapter_15",
        title = "What It Made of Her",
        illustrationId = "what_it_made_of_her",
        narrativeText = """
            Voss reads it twice before she says anything. "Partial containment always eventually
            fails. That's the lesson she took from Ashwell, and near enough every report since has
            agreed with her: Greymoor, Fenmoor, your own door twice over." She sets the pages down
            carefully, like they might still be able to hurt someone. "She's not building a weapon
            because she stopped caring what it costs. She's building it because she thinks eternal
            watching is a promise nobody can actually keep forever, and she's decided fusing six
            threats into one she can end for good is safer than trusting six more centuries of
            jailers to hold the line."

            Kaelen thinks of Halvard. Of six wardens who were meant to be six, and were down to
            one for longer than anyone noticed, and of how easily that same arithmetic could have
            swallowed Stonebeard whole before he ever arrived.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Sit with that.", nextNodeId = "old_debts")
        )
    ),
    StoryNode(
        id = "old_debts",
        chapterId = "chapter_15",
        title = "Old Debts",
        illustrationId = "old_debts",
        narrativeText = """
            It doesn't make her right. Whatever "ending it for good" actually means for the six
            things fused into her seventh door, Kaelen has watched enough of what almost happened
            at four settings to know it isn't mercy, for them or for anyone standing nearby when
            it finishes. But it isn't nothing, either, understanding what she thinks she's doing
            and why, and he hates a little how much that understanding costs him.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Understanding her doesn't mean stopping her matters any less.\"",
                nextNodeId = "the_reprisal_resolved",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 13)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("resolved_despite_sympathy")
                )
            ),
            Choice(
                label = "\"If eternal watching really can't be trusted forever, we owe it to everyone to prove her wrong instead of just outlasting her.\"",
                nextNodeId = "the_reprisal_committed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 12)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("committed_to_proving_her_wrong")
                )
            ),
            Choice(
                label = "Set the question aside. There isn't time to resolve it tonight.",
                nextNodeId = "the_reprisal_deferred",
                consequences = Consequences(setFlags = setOf("deferred_the_question_of_ilsevet"))
            )
        )
    ),
    StoryNode(
        id = "the_reprisal_resolved",
        chapterId = "chapter_15",
        title = "The Reprisal",
        illustrationId = "the_reprisal",
        narrativeText = """
            Whatever sympathy the archive earned her, Kaelen carries it into the fight settled
            rather than conflicted, understanding and resolve sitting easier together than he
            expected them to.

            The answer to why Selvane's records reached Stonebeard so quickly arrives with the
            reprisal squad that follows barely a day behind them, cinder-grey, moving fast and
            quiet, and entirely uninterested in the gate. They came for Voss specifically, the
            traitor whose name is apparently worth more to Ilsevet right now than another failed
            attempt on the brand.

            Voss doesn't look surprised. She looks like she's been waiting for this bill to come
            due since the day she gave Kaelen her signet.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reprisal_squad_encounter"
    ),
    StoryNode(
        id = "the_reprisal_committed",
        chapterId = "chapter_15",
        title = "The Reprisal",
        illustrationId = "the_reprisal",
        narrativeText = """
            Kaelen carries the vow into the fight like a weight he chose rather than one handed to
            him, proving her wrong feeling less like a slogan now and more like something he
            actually intends to spend years on.

            The answer to why Selvane's records reached Stonebeard so quickly arrives with the
            reprisal squad that follows barely a day behind them, cinder-grey, moving fast and
            quiet, and entirely uninterested in the gate. They came for Voss specifically, the
            traitor whose name is apparently worth more to Ilsevet right now than another failed
            attempt on the brand.

            Voss doesn't look surprised. She looks like she's been waiting for this bill to come
            due since the day she gave Kaelen her signet.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reprisal_squad_encounter"
    ),
    StoryNode(
        id = "the_reprisal_deferred",
        chapterId = "chapter_15",
        title = "The Reprisal",
        illustrationId = "the_reprisal",
        narrativeText = """
            The question follows him out of the room unresolved, a low hum under everything else
            he does for the rest of the evening, right up until it's interrupted by something far
            louder.

            The answer to why Selvane's records reached Stonebeard so quickly arrives with the
            reprisal squad that follows barely a day behind them, cinder-grey, moving fast and
            quiet, and entirely uninterested in the gate. They came for Voss specifically, the
            traitor whose name is apparently worth more to Ilsevet right now than another failed
            attempt on the brand.

            Voss doesn't look surprised. She looks like she's been waiting for this bill to come
            due since the day she gave Kaelen her signet.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "reprisal_squad_encounter"
    ),
    StoryNode(
        id = "after_the_reprisal",
        chapterId = "chapter_15",
        title = "After the Reprisal",
        illustrationId = "after_the_reprisal",
        narrativeText = """
            They don't get her. That's the only certainty worth holding onto once the last of the
            squad breaks and runs, Voss bleeding from a shallow cut along one arm, furious in a
            way that has nothing to do with fear.

            "Ashwell taught her partial containment fails eventually," she says, once she can
            breathe steadily again. "Today taught her the same lesson about partial revenge."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Tend the cut, and let her finish being angry.", nextNodeId = "chapter15_end")
        )
    ),
    StoryNode(
        id = "chapter15_end",
        chapterId = "chapter_15",
        title = "End of Chapter XV — Old Debts",
        illustrationId = "chapter15_threshold",
        narrativeText = """
            Whatever patience Ilsevet had left for indirect approaches, Kaelen suspects the
            reprisal squad spent the last of it. Four settings taught her a completed door might
            not survive its own completion. Ashwell taught her, a long time before any of this,
            that waiting never actually keeps anyone safe.

            Somewhere between those two lessons is whatever she does next, and Kaelen has a
            growing, uncomfortable certainty that it won't look like anything he's prepared for,
            whatever preparing for it even means at this point.

            Chapter XVI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Prepare anyway.", nextNodeId = "what_kaelen_isnt_prepared_for")
        )
    )
)

val chapter15 = com.thelastjailer.app.Chapter(
    id = "chapter_15",
    number = 15,
    title = "Chapter XV — Old Debts",
    startNodeId = "what_selvane_sends"
)
