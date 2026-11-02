package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C18: the flags that record what this telling decided about a defeated Ilsevet. Each choice
 * at `what_kaelen_chooses_trusted` and `what_kaelen_chooses_guarded` clears all three, then sets its
 * own, so a replay that decides differently never holds two answers at once. Chapter XXIII and the
 * six Chapter XXX endings read these flags, so they must describe this telling only. This runs when
 * a choice is taken, never when a scene is entered or loaded.
 */
val ILSEVET_FATE_FAMILY: Set<String> = setOf("captured_ilsevet", "showed_ilsevet_mercy", "deferred_ilsevets_fate")

/**
 * How the frame was stopped, as Chapter XVII recorded it. One copy is shown, always: destroyed outright, else disrupted carefully,
 * else (stopped however possible, or an older save that holds none) the third. The order makes the copies exclusive even if a replay
 * left an old flag beside a new one; the chain at Chapter XVII's gate clears them as a telling begins.
 */
private fun tellHowTheFrameWasStopped(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c18_frame_destroyed", requirements = ChoiceRequirement(requiredFlags = setOf("destroyed_the_frame_outright"))),
    Choice(
        label = label,
        nextNodeId = "c18_frame_disrupted",
        requirements = ChoiceRequirement(requiredFlags = setOf("disrupted_carefully"), forbiddenFlags = setOf("destroyed_the_frame_outright"))
    ),
    Choice(
        label = label,
        nextNodeId = "c18_frame_uncertain",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("destroyed_the_frame_outright", "disrupted_carefully"))
    )
)

/**
 * The decision about Ilsevet, in the Voss variant the current rung gives: trusting if Voss stands at the trusted rung now, guarded
 * otherwise (the lower rungs, or a save that holds none). The shipped scenes and their three choices are untouched.
 */
private fun chooseForIlsevet(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "what_kaelen_chooses_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "what_kaelen_chooses_guarded", requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_trusted")))
)

/**
 * The way on from the end of the chapter, by what Kaelen decided about Ilsevet. The three fate flags are exclusive (every fate choice
 * clears all three before it sets one); the order keeps the copies exclusive anyway, and the third is read by the absence of the other
 * two, so a save that holds none still goes on.
 */
private fun restWhileItLasts(): List<Choice> = listOf(
    Choice(label = "Rest, while it lasts.", nextNodeId = "c19_fate_captured", requirements = ChoiceRequirement(requiredFlags = setOf("captured_ilsevet"))),
    Choice(
        label = "Rest, while it lasts.",
        nextNodeId = "c19_fate_mercy",
        requirements = ChoiceRequirement(requiredFlags = setOf("showed_ilsevet_mercy"), forbiddenFlags = setOf("captured_ilsevet"))
    ),
    Choice(
        label = "Rest, while it lasts.",
        nextNodeId = "c19_fate_deferred",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("captured_ilsevet", "showed_ilsevet_mercy"))
    )
)

/**
 * Chapter XVIII — No More Between Us.
 *
 * The first direct duel between Kaelen and Ilsevet. With the frame destroyed or disrupted in XVII
 * and nothing left to send in her place, she comes to Stonebeard alone to settle this personally —
 * a deliberate escalation past every squad, captain, and construct sent in her stead since V.
 * Whether Kaelen accepts the terms at once or makes her explain herself first, and whether he
 * speaks before drawing or answers with silence, now earns a distinct beat from Ilsevet going into
 * the fight, not just a flag. The eighteenth combat encounter is the first true jump in scale
 * (Ilsevet herself, 260 HP), and the chapter's real weight sits after the fight rather than during
 * it: a three-way stat-gated choice about what to do with a defeated Ilsevet (capture, mercy, or
 * deferral), each paired to the courage/honour throughlines established since III.
 *
 * Judgment call flagged for review: resolving Ilsevet's personal threat-arc in direct combat this
 * decisively is the biggest structural swing since Halvard's death in X — it closes the "villain
 * hunt" phase of the story that's driven chapters V-XVII. It deliberately doesn't end the larger
 * story (the Cinder faction, the four compromised gates, and the prisoner's unresolved fate all
 * remain open), and the closing line telegraphs directly that a new threat is coming, but this is a
 * real turning point and worth a second look before merging.
 */
val chapter18Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c18_gate_after_ashfall",
        chapterId = "chapter_18",
        title = "What the Gate Kept",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The gate had been repaired in three places since Kaelen last stood beneath it. The new
            mortar was paler than the old stone. Someone had hung a lamp where a broken hinge had
            been, and somebody else had scratched six short lines into the wood beside it. He could
            not tell whether they were a tally or a warning. He left them untouched. Behind him, the
            chain Halvard had carried knocked once against his breastplate as he climbed the steps.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask who made the marks.", nextNodeId = "c18_marks"),
            Choice(label = "Leave the marks alone.", nextNodeId = "c18_quiet_wall")
        )
    ),
    StoryNode(
        id = "c18_marks",
        chapterId = "chapter_18",
        title = "Six Lines",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Tam was the one who answered. She had not cut the marks herself, but she knew who had.
            Each line belonged to someone who had kept the wall while Kaelen was gone. One had been
            made for a man who would never keep it again. Tam did not offer names. Kaelen found that
            he did not need them. The gate had its own way of remembering, even when its books were
            closed.
        """.trimIndent(),
        choices = listOf(Choice(label = "Thank Tam for keeping watch.", nextNodeId = "c18_stonebeard_report"))
    ),
    StoryNode(
        id = "c18_quiet_wall",
        chapterId = "chapter_18",
        title = "What Goes Unasked",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            He passed the marks without asking. By morning he regretted the silence, though he could
            not have said what answer he had wanted. The wall was still standing. That had become
            the measure of too many things. He rested his hand against the newer mortar and felt the
            cold through his bracer. A repaired wall was not an untouched one. It was only proof
            that someone had come back to mend it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Find the old smith.", nextNodeId = "c18_stonebeard_report"))
    ),
    StoryNode(
        id = "c18_stonebeard_report",
        chapterId = "chapter_18",
        title = "The Account Given",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            The old smith listened without interrupting while Kaelen described the chamber at
            Ashfall. He asked about the guardian, the six settings and the people who had held the
            road. Only when Kaelen finished did he ask the question that mattered to him. "Will it
            work again?" Kaelen looked at the old dwarf across the table. "I stopped it," he said.
            "That is what I know." The old smith accepted the difference between those answers
            without pretending it comforted him.
        """.trimIndent(),
        choices = tellHowTheFrameWasStopped("Tell him how the frame was stopped.")
    ),
    StoryNode(
        id = "c18_frame_destroyed",
        chapterId = "chapter_18",
        title = "Broken Settings",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            "I broke the settings," Kaelen said. "Whatever pattern they held, it cannot be made to
            answer in that chamber again without someone building it anew." The old smith nodded
            once. That was a useful answer, but not a final one. There were people in the world who
            would call a broken machine a problem to be solved. Kaelen had met too many of them. He
            told Stonebeard as much, and the dwarf wrote it down.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the watch.", nextNodeId = "c18_watch"))
    ),
    StoryNode(
        id = "c18_frame_disrupted",
        chapterId = "chapter_18",
        title = "The Pattern Interrupted",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            "The settings are still there," Kaelen said. "I broke their agreement, not the stones
            themselves." The old smith's pen stopped. "Can they be put back?" "By someone who knows
            how, perhaps." Neither man liked that answer. It was still the only honest one. He wrote
            down the shape of the danger rather than crossing it out of the ledger. Kaelen was
            grateful for that small refusal to make the story simpler.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the watch.", nextNodeId = "c18_watch"))
    ),
    StoryNode(
        id = "c18_frame_uncertain",
        chapterId = "chapter_18",
        title = "Enough to Stop It",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            "It stopped," Kaelen said. "I cannot promise what remains of the frame." The old smith
            looked at the chain in Kaelen's hands and then at Kaelen. "A thing can be stopped
            without being settled," he said. Halvard might have said the same, though probably with
            fewer words. They marked the chamber for another inspection when the road could be held.
            For now there were wounded people, a damaged wall and a night to get through.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the watch.", nextNodeId = "c18_watch"))
    ),
    StoryNode(
        id = "c18_watch",
        chapterId = "chapter_18",
        title = "The Road Below",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Voss was on the watch platform, her wounded arm resting against the stone as she studied
            the road. She made room for him without turning. Far below, the last supply cart of the
            evening crept towards the gate. "You told them what happened?" she asked. "What I know
            of it." "Good." She watched the cart a little longer. "There will be people who want a
            cleaner account." "They can wait," Kaelen said. Voss gave him a brief, tired smile.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask what she expects next.", nextNodeId = "c18_voss_warning"),
            Choice(label = "Let the road speak for itself.", nextNodeId = "c18_voss_silence")
        )
    ),
    StoryNode(
        id = "c18_voss_warning",
        chapterId = "chapter_18",
        title = "No More Messengers",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            "Ilsevet has run out of people she trusts to fail for her," Voss said. "That doesn't
            mean she's run out of reasons." Kaelen followed her gaze to the far bend in the road.
            "You think she'll come herself?" "I think she'll want to see what you did to the frame.
            And I think she'll tell herself that's the only reason." Voss turned at last. "Whatever
            she says, make her answer for the people who never got to stand in front of you."
        """.trimIndent(),
        choices = listOf(Choice(label = "Promise nothing you cannot keep.", nextNodeId = "c18_three_days"))
    ),
    StoryNode(
        id = "c18_voss_silence",
        chapterId = "chapter_18",
        title = "The Watch Shared",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            They stood together until the cart reached the gate. Voss did not ask for a promise, and
            Kaelen did not make one. A year ago he might have mistaken that for distance. Now he
            understood it as a kindness, at least some of the time. The watch bell sounded below.
            Voss straightened and winced before she could hide it. Kaelen noticed. She noticed him
            noticing. Neither mentioned it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Take the next watch.", nextNodeId = "c18_three_days"))
    ),
    StoryNode(
        id = "c18_three_days",
        chapterId = "chapter_18",
        title = "The Third Morning",
        illustrationId = "no_more_envoys",
        narrativeText = """
            On the third morning after Ashfall, a single figure appeared on the roots road. The
            guards saw the plain sword before they recognised the woman carrying it. Nobody fired.
            Nobody opened the gate. By the time word reached Kaelen, Ilsevet had stopped where the
            road widened and laid her empty left hand against the trunk of the old tree. She waited
            without calling up. That patience frightened him more than a shouted threat would have.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go down to meet her.", nextNodeId = "no_more_envoys"))
    ),
    StoryNode(
        id = "no_more_envoys",
        chapterId = "chapter_18",
        title = "No More Envoys",
        illustrationId = "no_more_envoys",
        narrativeText = """
            She comes back to the black door three days after the Sanctum, alone this time in
            every sense that matters: no vanguard, no captain, no reliquary thief. Just Ilsevet,
            on foot, a fresh scar along one arm that wasn't there the last time Kaelen saw her up
            close.

            "I'm done sending people to do this," she says. "The frame's stopped. Whatever I build
            next starts with a decision only the two of us can actually make."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then let's make it.\"",
                nextNodeId = "her_own_terms_readily",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("accepted_the_duel_readily")
                )
            ),
            Choice(
                label = "\"Say what you actually came to say first.\"",
                nextNodeId = "her_own_terms_pressed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("made_her_say_it_first")
                )
            )
        )
    ),
    StoryNode(
        id = "her_own_terms_readily",
        chapterId = "chapter_18",
        title = "Her Own Terms",
        illustrationId = "her_own_terms",
        narrativeText = """
            Something in her posture eases at the speed of the answer, the particular relief of a
            woman who came expecting an argument and didn't get one. "Good," she says. "You beat
            what I sent to guard the frame. You've beaten everything I've sent since Ashfall, one
            way or another. I've run out of things to send." She draws a single blade, plain,
            nothing ceremonial about it. "So: me, you, and whichever of us is still standing
            decides what happens to the brand. No garrison. No prisoner behind your door getting
            a vote. Just the two people this has actually been about since the beginning."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Accept.", nextNodeId = "c18_after_terms")
        )
    ),
    StoryNode(
        id = "her_own_terms_pressed",
        chapterId = "chapter_18",
        title = "Her Own Terms",
        illustrationId = "her_own_terms",
        narrativeText = """
            She doesn't rush the answer, and something in her expression suggests she respects
            being made to earn it. "You beat what I sent to guard the frame. You've beaten
            everything I've sent since Ashfall, one way or another. I've run out of things to
            send." She draws a single blade, plain, nothing ceremonial about it, only once she's
            said her piece in full. "So: me, you, and whichever of us is still standing decides
            what happens to the brand. No garrison. No prisoner behind your door getting a vote.
            Just the two people this has actually been about since the beginning."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Accept.", nextNodeId = "c18_after_terms")
        )
    ),
    StoryNode(
        id = "c18_after_terms",
        chapterId = "chapter_18",
        title = "The People Between Them",
        illustrationId = "her_own_terms",
        narrativeText = """
            Kaelen accepted, and the terms sounded simple only if he let Ilsevet name the two people
            who mattered. He thought of the riders on the road, the smiths at the Hold and Halvard's
            empty place at the table. "You don't get to say this was only ever between us," he told
            her. Ilsevet's jaw tightened. She had come to settle an argument with one man. He would
            not let her erase the people who had already paid for it.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask who she meant to save.", nextNodeId = "c18_ashwell_question"),
            Choice(label = "Ask who she was willing to lose.", nextNodeId = "c18_cost_question")
        )
    ),
    StoryNode(
        id = "c18_ashwell_question",
        chapterId = "chapter_18",
        title = "The Name of the Loss",
        illustrationId = "her_own_terms",
        narrativeText = """
            "Ashwell," she said before he finished asking. The name was almost an accusation, though
            Kaelen had not been there to earn it. "I thought if I could make one thing hold, nobody
            would have to stand where I stood." Her eyes moved to the wall. "Then I kept finding
            reasons other people had to stand there first." She said it without asking to be
            forgiven. For the first time Kaelen believed she knew the difference.
        """.trimIndent(),
        choices = listOf(Choice(label = "Tell her knowing is not enough.", nextNodeId = "c18_before_blades"))
    ),
    StoryNode(
        id = "c18_cost_question",
        chapterId = "chapter_18",
        title = "A List Without an End",
        illustrationId = "her_own_terms",
        narrativeText = """
            Ilsevet tried to answer with numbers. Men sent, gates held, villages spared, years
            bought. Kaelen let her finish. "And the names?" he asked. She looked away. There were
            names she could give him and names she had never learned. The missing ones troubled him
            more. "I told myself the count mattered," she said. "It did," Kaelen replied. "It
            mattered to the people you counted." She did not argue.
        """.trimIndent(),
        choices = listOf(Choice(label = "Bring the argument to its end.", nextNodeId = "c18_before_blades"))
    ),
    StoryNode(
        id = "c18_before_blades",
        chapterId = "chapter_18",
        title = "What the Sword Cannot Say",
        illustrationId = "her_own_terms",
        narrativeText = """
            Ilsevet set her feet on the wet stones. Kaelen saw the fresh scar along her arm and the
            exhaustion beneath her careful stance. She was dangerous. She was also tired. Neither
            fact cancelled the other. "There is still time to answer without steel," he said. "For
            what?" "For why you kept going when you knew the cost." Ilsevet's grip tightened. "I
            don't have an answer you'll accept." "Then tell the truth anyway."
        """.trimIndent(),
        choices = listOf(Choice(label = "Wait for her to move.", nextNodeId = "what_kaelen_says_first"))
    ),
    StoryNode(
        id = "what_kaelen_says_first",
        chapterId = "chapter_18",
        title = "What Kaelen Says First",
        illustrationId = "what_kaelen_says_first",
        narrativeText = """
            There's a version of this where Kaelen has something clever to say, about Ashwell,
            about Halvard, about everything four settings and stolen shards and a reliquary
            thief have cost people who never chose to be part of her arithmetic. He finds, when
            it actually comes to it, that he doesn't much feel like making a speech.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not doing this because I hate you.\"",
                nextNodeId = "the_duel_spoke",
                consequences = Consequences(setFlags = setOf("spoke_before_the_duel"))
            ),
            Choice(
                label = "Say nothing. Draw your sword instead.",
                nextNodeId = "the_duel_silent",
                consequences = Consequences(setFlags = setOf("answered_with_silence"))
            )
        )
    ),
    StoryNode(
        id = "the_duel_spoke",
        chapterId = "chapter_18",
        title = "The Duel",
        illustrationId = "the_duel",
        narrativeText = """
            Something flickers across her face at that, quick enough to be gone before Kaelen can
            name it, and for just a moment the duel feels like it might not be the only thing
            happening between them. Then it passes, and she's Ilsevet again, faster and colder
            and entirely unhurried, the same way she was unhurried walking into Stonebeard's
            broken gate the first time.

            This isn't a garrison holding a line. This is Ilsevet herself, and whatever Kaelen's
            words cost her, she isn't holding anything back for it.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "ilsevet_duel_encounter"
    ),
    StoryNode(
        id = "the_duel_silent",
        chapterId = "chapter_18",
        title = "The Duel",
        illustrationId = "the_duel",
        narrativeText = """
            She waits half a beat for words that don't come, and something almost like respect
            crosses her face at their absence, as if silence were the one answer she hadn't
            already rehearsed a counter for. She's better than anyone who's fought for her
            before: faster, colder, entirely unhurried even now, the same way she was unhurried
            walking into Stonebeard's broken gate the first time.

            This isn't a garrison holding a line. This is Ilsevet herself, and she isn't holding
            anything back.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "ilsevet_duel_encounter"
    ),
    StoryNode(
        id = "ilsevet_duel_defeat",
        chapterId = "chapter_18",
        title = "She Doesn't Take It",
        illustrationId = "the_duel",
        narrativeText = """
            She's better than anyone who's fought for her before, faster and colder and entirely
            unhurried even now, and Kaelen learns exactly how much distance that leaves between
            them the hard way, on his back, her blade already at the reach it needs to finish this.

            She doesn't take it. Whatever flickered across her face earlier hasn't finished
            flickering, and the half-second she spends deciding not to become the thing Ashwell
            made of her is the half-second Kaelen needs to make sure neither of them has to decide
            anything else tonight.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "what_is_left_of_her",
                consequences = Consequences(setFlags = setOf("lost_to_ilsevet_the_cinder_marshal"))
            )
        )
    ),
    StoryNode(
        id = "what_is_left_of_her",
        chapterId = "chapter_18",
        title = "What Is Left of Her",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            She goes down eventually, not killed, not quite broken, just finally, finally out of
            whatever kept her upright through four settings and a burned garrison and every
            failure since Ashwell. Kaelen stands over her with a blade he doesn't lower right
            away, and for the first time since any of this started, Ilsevet looks like exactly
            what she is: a woman who has spent fifteen years losing an argument with grief, and
            just lost the last round of it too.

            "Go on, then," she says, not quite steady. "Finish what Ashwell should have taught me
            to expect."
        """.trimIndent(),
        choices = listOf(Choice(label = "Decide.", nextNodeId = "c18_after_duel"))
    ),
    StoryNode(
        id = "c18_after_duel",
        chapterId = "chapter_18",
        title = "The Silence After",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            Kaelen began to decide, and found that the road had gone quiet. Ilsevet's sword lay a
            few steps away, its plain blade dark with rain. He stood where the fight had ended and
            listened for the gate bell. It did not ring. The guards were waiting for him to decide
            what kind of victory they had witnessed. He wished they would stop. He had fought to end
            a threat. He had not fought to become the only person allowed to judge what came after.
        """.trimIndent(),
        choices = listOf(Choice(label = "Hear Voss before deciding.", nextNodeId = "c18_voss_account"))
    ),
    StoryNode(
        id = "c18_voss_account",
        chapterId = "chapter_18",
        title = "Her Own Reckoning",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            Voss approached without touching Ilsevet or the fallen sword. "She hurt people," she
            said. "So did the people who obeyed her. Some of them will want a reason to stop. Others
            will want a new Marshal." Kaelen looked at her. "What would you do?" "I would remember
            the people who aren't here to answer." She left the choice with him. He had learned that
            this was different from leaving him alone.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask what Ilsevet can still tell them.", nextNodeId = "c18_ilsevet_last_question"),
            Choice(label = "Make the decision without another answer.", nextNodeId = "c18_choice_return")
        )
    ),
    StoryNode(
        id = "c18_ilsevet_last_question",
        chapterId = "chapter_18",
        title = "One Thing Unfinished",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            Ilsevet's breathing steadied. When Kaelen asked whether the frame had been hers from the
            beginning, she looked at him with sudden alarm, as though he had touched something she
            kept covered. "That is not a question for a road," she said. He waited. She shook her
            head. "If you want the rest, ask me when I can stand without a sword between us." Kaelen
            could not tell whether it was a bargain or the first honest admission she had made that
            day. He chose to remember the words.
        """.trimIndent(),
        choices = listOf(Choice(label = "Decide her fate.", nextNodeId = "c18_choice_return"))
    ),
    StoryNode(
        id = "c18_choice_return",
        chapterId = "chapter_18",
        title = "The Decision Is His",
        illustrationId = "what_is_left_of_her",
        narrativeText = """
            Kaelen retrieved the plain sword. He did not offer it back. Beyond the wall, the day
            carried on: carts arriving, people calling, a hammer striking iron somewhere out of
            sight. Ilsevet had made the world wait for her decisions for too long. He would not do
            the same. Whatever he chose, it would be an answer he could explain to the living and
            carry among the dead.
        """.trimIndent(),
        choices = chooseForIlsevet("Choose what happens to Ilsevet.")
    ),
    StoryNode(
        id = "what_kaelen_chooses_trusted",
        chapterId = "chapter_18",
        title = "What Kaelen Chooses",
        illustrationId = "what_kaelen_chooses",
        narrativeText = """
            It isn't a clean choice. It was never going to be, not after Ashwell, not after
            Halvard, not after everyone else who paid for the space between what she's afraid of
            and what she's willing to do about it.

            Voss doesn't tell him what to do with the woman bleeding out at his feet, the same way
            she never once asked him to simply believe her back at the black door. She just waits,
            the way trust that was given freely tends to be returned.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not going to kill you. But you're done deciding what happens to six lives that aren't yours to spend.\" Bind her and bring her back to face what's left of the Order.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 16)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("captured_ilsevet")
                )
            ),
            Choice(
                label = "\"Ashwell already took enough from you. I won't let this take the rest.\" Let her go, on the condition she never comes back armed.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 15)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("showed_ilsevet_mercy")
                )
            ),
            Choice(
                label = "Take her weapon, and decide the rest later. There's no clean answer waiting right now.",
                nextNodeId = "chapter18_end",
                consequences = Consequences(
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("deferred_ilsevets_fate")
                )
            )
        )
    ),
    StoryNode(
        id = "what_kaelen_chooses_guarded",
        chapterId = "chapter_18",
        title = "What Kaelen Chooses",
        illustrationId = "what_kaelen_chooses",
        narrativeText = """
            It isn't a clean choice. It was never going to be, not after Ashwell, not after
            Halvard, not after everyone else who paid for the space between what she's afraid of
            and what she's willing to do about it.

            Voss doesn't tell him what to do with the woman bleeding out at his feet. She's earned
            that much restraint from him by now, the same slow way he once made her earn his
            belief at the black door.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'm not going to kill you. But you're done deciding what happens to six lives that aren't yours to spend.\" Bind her and bring her back to face what's left of the Order.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 16)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("captured_ilsevet")
                )
            ),
            Choice(
                label = "\"Ashwell already took enough from you. I won't let this take the rest.\" Let her go, on the condition she never comes back armed.",
                nextNodeId = "chapter18_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 15)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("showed_ilsevet_mercy")
                )
            ),
            Choice(
                label = "Take her weapon, and decide the rest later. There's no clean answer waiting right now.",
                nextNodeId = "chapter18_end",
                consequences = Consequences(
                    grantItemIds = listOf("ilsevets_blade"),
                    clearFlags = ILSEVET_FATE_FAMILY,
                    setFlags = setOf("deferred_ilsevets_fate")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter18_end",
        chapterId = "chapter_18",
        title = "End of Chapter XVIII — No More Between Us",
        illustrationId = "chapter18_threshold",
        narrativeText = """
            Whatever Kaelen decides, one thing is certain by the time the black door's shadow
            falls long again: the Cinder Marshal doesn't get to make this decision for anyone else
            anymore, not today. Voss finds him still standing over where the fight ended, and
            doesn't ask what he chose before he's ready to say it.

            Word will need to reach Selvane. Word will need to reach Fenmoor, and Thessaly, and
            whatever's left of Ilsevet's own people scattered across the Ashfall reaches without a
            Marshal left to answer to. None of that happens tonight.

            Tonight, for the first time since a black door opened beneath an ancient tree, Kaelen
            lets himself believe the worst of it might actually be over.

            He's wrong about that. He just doesn't know it yet.

            Chapter XIX awaits.
        """.trimIndent(),
        choices = restWhileItLasts()
    )
)

val chapter18 = com.thelastjailer.app.Chapter(
    id = "chapter_18",
    number = 18,
    title = "Chapter XVIII — No More Between Us",
    startNodeId = "no_more_envoys"
)
