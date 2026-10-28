package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Everything Chapter XV records: the two shipped ways of reading the dispatch, the three shipped answers about Ilsevet, and the
 * defeat marker. The chapter's first choices clear it all, so a replay reaches the dispatch with an empty slate.
 */
val C15_TELLING_FLAGS: Set<String> = setOf(
    "read_alone_first", "read_with_voss", "resolved_despite_sympathy", "committed_to_proving_her_wrong",
    "deferred_the_question_of_ilsevet", "lost_to_cinder_reprisal_leader"
)

/** Voss reading the Order's pages, by where she and Kaelen stand. The lowest rung also covers a save that holds none. */
private fun toTheVossReading(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c15_voss_reads_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "c15_voss_reads_working", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"))),
    Choice(
        label = label,
        nextNodeId = "c15_voss_reads_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))
    )
)

/**
 * What Ashfall looks like now that Kaelen has read the file, by how he met Ilsevet there in Chapter XII: he tried to reach her
 * (warned her or appealed to her), took the fragment and left, or let her face it. The last is read by the absence of the others.
 */
private fun toTheEcho(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c15_echo_reached", requirements = ChoiceRequirement(requiredFlags = setOf("warned_ilsevet_directly"))),
    Choice(label = label, nextNodeId = "c15_echo_reached", requirements = ChoiceRequirement(requiredFlags = setOf("appealed_to_ilsevets_conscience"))),
    Choice(label = label, nextNodeId = "c15_echo_left", requirements = ChoiceRequirement(requiredFlags = setOf("left_ilsevet_to_her_wreckage"))),
    Choice(
        label = label,
        nextNodeId = "c15_echo_refused",
        requirements = ChoiceRequirement(
            forbiddenFlags = setOf("warned_ilsevet_directly", "appealed_to_ilsevets_conscience", "left_ilsevet_to_her_wreckage")
        )
    )
)

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
 *
 * The expansion gives the file two more sheets and gives Kaelen's earlier choices a place to land. Before the dispatch comes,
 * Voss says what she thinks of how Kaelen asked Selvane for it, in Chapter XIII, openly or low. The pages hold Ilsevet's own
 * request, written before the ward failed, and the form that answered it. Voss reads them at her own rung, recognising only how
 * the Order closes a file and nothing about herself or Ashwell. And Ashfall returns coloured by how Kaelen met Ilsevet there. Her
 * conflict is not resolved or answered: the choice at "Old Debts" is still the player's. The shipped scenes and their fight are
 * untouched; only where some of their choices lead has changed.
 */
val chapter15Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c15_waiting_open",
        chapterId = "chapter_15",
        title = "Waiting on the Order",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            A long stretch of days has gone by since the Order rode out, and Kaelen finds he
            is still listening for hooves. Voss notices him doing it. "She will send it,"
            she says. "She said so in front of me, and the Inquisitor-General does not say
            a thing in front of a witness that she does not mean to do." She goes back to
            the blade she is mending. "You were right to ask where I could hear. It would
            have been worse to find out afterward that you had not."
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep the watch.", nextNodeId = "what_selvane_sends"))
    ),
    StoryNode(
        id = "c15_waiting_private",
        chapterId = "chapter_15",
        title = "Waiting on the Order",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            A long stretch of days has gone by since the Order rode out, and Kaelen finds he
            is still listening for hooves. Voss notices him doing it. "She will send it,"
            she says. "Whatever you asked her, she will have answered, and she does not
            answer a question and then forget it." She does not look up from the blade she
            is mending. "I did not hear what you asked. I could guess." A pause, short
            enough to be a decision. "You were right to ask it when I was not looking. I
            would have hesitated, if I had been looking."
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep the watch.", nextNodeId = "what_selvane_sends"))
    ),
    StoryNode(
        id = "what_selvane_sends",
        chapterId = "chapter_15",
        title = "What Selvane Sends",
        illustrationId = "what_selvane_sends",
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
                nextNodeId = "c15_the_reply",
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
                nextNodeId = "c15_the_reply",
                consequences = Consequences(grantItemIds = listOf("record_of_ashwell"))
            )
        )
    ),
    StoryNode(
        id = "c15_the_reply",
        chapterId = "chapter_15",
        title = "What Else Was in the Case",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Behind the account, clipped flat and easily taken for its backing, there are two
            more sheets, and neither is from the Order's archivists. The first is in a
            garrison hand, plain and square: a request from the post commander of a lesser
            ward, signed with a name that meant nothing yet to anyone, asking that the hill
            wards be looked at before the winter. It says why. The stone had begun to sweat
            in a dry month, and the oldest watchman would not sleep near it. It was written
            in the year before the ward failed.

            The second sheet is the answer. It is a form, with a clerk's stamp, and the box
            that has been ticked says that the matter has been noted. Kaelen turns the
            sheet over, looking for more, and there is none.
        """.trimIndent(),
        choices = toTheVossReading("Go on.")
    ),
    StoryNode(
        id = "c15_voss_reads_wary",
        chapterId = "chapter_15",
        title = "Voss Reads",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Voss takes the sheets one at a time and reads each as if it might be a trap,
            checking the wax, the hand, the stamp. At the form she stops. Her thumb finds
            the clerk's mark and rests on it, and stays there longer than the mark
            deserves. When she moves on, she squares the sheet against the table as one
            squares something that has to be returned. She does not look at Kaelen. She has
            the face of a woman who knows exactly how that sheet came to be written, and
            has decided not to say so.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait for her.", nextNodeId = "what_it_made_of_her"))
    ),
    StoryNode(
        id = "c15_voss_reads_working",
        chapterId = "chapter_15",
        title = "Voss Reads",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Voss reads the sheets in the order they were written, and then in the order they
            were answered, which is not the same. At the form she goes still. Her eyes go to
            the box that was ticked, and to the clerk's stamp beside it, and for a moment
            she is reading something that is not on the page. Then she sets it down with one
            finger and does not touch it again. Whatever she has recognised, she has put
            away behind her face, and she does what she always does with a thing she cannot
            yet use. She waits.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait with her.", nextNodeId = "what_it_made_of_her"))
    ),
    StoryNode(
        id = "c15_voss_reads_trusted",
        chapterId = "chapter_15",
        title = "Voss Reads",
        illustrationId = "the_woman_before_ilsevet",
        narrativeText = """
            Voss reads the sheets slowly, and Kaelen lets her. At the form she shuts her
            eyes for the length of a breath. Her hand has come to rest beside his on the
            table, not touching, and it stays there while she reads the stamp again. When
            she opens her eyes she is looking at the window and not at him. It is a long
            stillness, and he does not put a name to it. He lets it be as long as it needs
            to be.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait with her.", nextNodeId = "what_it_made_of_her"))
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
        choices = toTheEcho("Sit with that.")
    ),
    StoryNode(
        id = "c15_echo_reached",
        chapterId = "chapter_15",
        title = "What Ashfall Was",
        illustrationId = "what_it_made_of_her",
        narrativeText = """
            He thinks of the chamber at Ashfall, and of what he said to her across the wreck
            of the frame, when it had gone quiet enough for words. He had thought of it as
            a warning, or an appeal, something for her to hear and do with as she liked. He
            had not known then that she had once written the plainest request a post
            commander can write, and been answered with a tick in a box. Whatever he
            offered her in the wreckage was not nothing. It may have been the first answer
            she had been given in a long time that came from a person.
        """.trimIndent(),
        choices = listOf(Choice(label = "Sit with that.", nextNodeId = "old_debts"))
    ),
    StoryNode(
        id = "c15_echo_left",
        chapterId = "chapter_15",
        title = "What He Left Her",
        illustrationId = "what_it_made_of_her",
        narrativeText = """
            He thinks of the chamber at Ashfall, and of the fragment he took out of the
            wreck, and the silence he left her with. He had told himself that a woman who
            builds such a thing has no claim on a parting word. He reads the stamped form
            again, and finds that the silence he left was the same kind of answer. He cannot
            say he is ashamed. He can say that he recognises it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Sit with that.", nextNodeId = "old_debts"))
    ),
    StoryNode(
        id = "c15_echo_refused",
        chapterId = "chapter_15",
        title = "What He Let Her Face",
        illustrationId = "what_it_made_of_her",
        narrativeText = """
            He thinks of Ashfall, which he knows only from a messenger's mouth: a fire that
            burned itself out, a garrison that mostly did not survive. He let her face it
            alone, and sent word to Fenmoor instead. Nothing in the file says he was wrong.
            It says only that the last time someone asked for help and was told it had been
            noted, a ward in the hills failed in full.
        """.trimIndent(),
        choices = listOf(Choice(label = "Sit with that.", nextNodeId = "old_debts"))
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
        id = "reprisal_squad_defeat",
        chapterId = "chapter_15",
        title = "She Held It Alone",
        illustrationId = "the_reprisal",
        narrativeText = """
            The squad came for Voss, not him, and Kaelen learns that the hard way when the leader
            simply steps around him rather than finish a fight he's already losing, more
            interested in the traitor than the jailer standing in the way. Going down proves
            easier than staying between them.

            Voss doesn't need him upright to make her point. She holds the line herself, bleeding
            and furious, until the squad breaks and runs rather than press an attack that's cost
            them more than Ilsevet promised it would.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up and tend the cut she won't mention.",
                nextNodeId = "after_the_reprisal",
                consequences = Consequences(setFlags = setOf("lost_to_cinder_reprisal_leader"))
            )
        )
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
            Choice(
                label = "Prepare anyway.",
                nextNodeId = "what_kaelen_isnt_prepared_for",
                consequences = Consequences(clearFlags = C16_TELLING_FLAGS)
            )
        )
    )
)

val chapter15 = com.thelastjailer.app.Chapter(
    id = "chapter_15",
    number = 15,
    title = "Chapter XV — Old Debts",
    startNodeId = "what_selvane_sends"
)
