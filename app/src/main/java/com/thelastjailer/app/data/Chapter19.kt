package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XIX — Already Written.
 *
 * Reveals that Ilsevet, whatever her fate at the end of XVIII, was never the whole story: the
 * seventh-door frame's design wasn't hers — she found it already complete in a dead archivist's
 * workshop eleven years before the story began, in a hand that "got surer of the frame the longer
 * it worked on it," which the prisoner (continuing its role since XIV) reads as a description of
 * something remembering rather than someone learning. Pressing her for the archivist's identity
 * versus letting her tell it in her own order changes how that confession actually unfolds, and how
 * Kaelen answers discovering his own name in the dead archivist's notes (defiance, ownership, or
 * silence) now colors the chapter's close, not just which flag it sets. Selvane's investigation
 * into the workshop finds five more sets of plans addressed by name to people who hadn't yet taken up
 * the roles they were given — including Kaelen's own name, tying directly back to Ilsevet's "like
 * they'd met before" greeting in Chapter X and reframing the entire Ilsevet arc as a symptom of
 * something older and still active. Deliberately no new combat encounter — a lore/investigation
 * chapter matching the pacing pattern established by III, XIII, and XVI, giving the reveal room
 * after XVIII's duel rather than immediately escalating into another fight.
 *
 * Judgment call flagged for review: this reframes the entire story's antagonist structure —
 * Ilsevet's arc, just closed in XVIII, becomes a first movement rather than the whole piece, and
 * Kaelen is now confirmed to have been specifically anticipated by whoever authored the original
 * design. This sets the scope for chapters beyond XIX and is worth a look before merging.
 */
val chapter19Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c19_fate_captured",
        chapterId = "chapter_19",
        title = "A Room Without a Sword",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            Kaelen rested as long as the day allowed, which was not long. Ilsevet was kept in a room
            with a barred window and two guards beyond the door. Her wrists were bound loosely, and
            Kaelen had refused every audience. Her blade was already his. She stood when he entered,
            more from habit than respect. "I said I was finished," she told him. "You said you had
            something else to say," he answered. She looked at the empty place where her sword
            should have been. "Then close the door."
        """.trimIndent(),
        choices = listOf(Choice(label = "Hear her account.", nextNodeId = "c19_testimony_terms"))
    ),
    StoryNode(
        id = "c19_fate_mercy",
        chapterId = "chapter_19",
        title = "A Message Left at the Gate",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            Kaelen rested as long as the day allowed, which was not long. Ilsevet had been allowed
            to leave. Before she took the road, she had given Tam a folded page and made her promise
            it would reach Kaelen unopened. The writing was spare, almost angry: the name of a place
            and a single line. "I will answer once, if you come without soldiers." Kaelen read the
            note twice. Mercy had not made her trustworthy. It had left her free to decide whether
            to tell the truth.
        """.trimIndent(),
        choices = listOf(Choice(label = "Meet her where she named.", nextNodeId = "c19_mercy_meeting"))
    ),
    StoryNode(
        id = "c19_mercy_meeting",
        chapterId = "chapter_19",
        title = "The Last Meeting",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            She waited in a roofless wayhouse beyond the Hold, unarmed as she had promised. Voss
            remained near enough to intervene and far enough not to turn the meeting into an arrest.
            "You could have sent a lie," Kaelen said. "I could have stayed away," Ilsevet answered.
            Both were true. Rain gathered in the broken roof. She took a breath and told him there
            was one thing about the frame she had never admitted aloud.
        """.trimIndent(),
        choices = listOf(Choice(label = "Listen.", nextNodeId = "c19_testimony_terms"))
    ),
    StoryNode(
        id = "c19_fate_deferred",
        chapterId = "chapter_19",
        title = "The Answer Still Owed",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            Kaelen rested as long as the day allowed, which was not long. Ilsevet remained where
            Kaelen had left her, under watch but without a sentence. Her sword was gone. Her
            arguments had not followed it. When he returned, she looked as though she had spent the
            night trying to decide whether an unfinished judgement was kinder or crueller than a
            finished one. "You haven't come to answer me," she said. "I came to ask something
            first," Kaelen replied. "Then perhaps we're both waiting."
        """.trimIndent(),
        choices = listOf(Choice(label = "Ask her what she has not said.", nextNodeId = "c19_testimony_terms"))
    ),
    StoryNode(
        id = "c19_testimony_terms",
        chapterId = "chapter_19",
        title = "The Truth Has Conditions",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            "There is something about the frame I have never said aloud," Ilsevet began. She would
            not say more until Kaelen promised he would send someone to verify it. He made no
            promise about protecting her from what the evidence might prove. "Good," she said,
            surprising him. "I have spent too long asking people to accept my reasons instead of
            checking my work." He waited until she was ready to speak plainly.
        """.trimIndent(),
        choices = listOf(Choice(label = "Hear what she knew.", nextNodeId = "what_ilsevet_knew"))
    ),
    StoryNode(
        id = "what_ilsevet_knew",
        chapterId = "chapter_19",
        title = "What Ilsevet Knew",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            Whatever else changes about what happens to her, Ilsevet has exactly one more honest
            conversation left in her, and she spends it on something Kaelen doesn't expect.

            "The frame's design wasn't mine," she says. "I found the first pages of it in a dead
            archivist's collection eleven years ago, already drawn, already complete down to
            details I hadn't worked out myself yet. I told myself it was luck. I have had a very
            long time since then not to believe that."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Who drew them, if not you?\"",
                nextNodeId = "the_dead_archivist_pressed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("pressed_about_the_archive")
                )
            ),
            Choice(
                label = "Let her finish in her own order.",
                nextNodeId = "the_dead_archivist_patient",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("let_her_finish")
                )
            )
        )
    ),
    StoryNode(
        id = "the_dead_archivist_pressed",
        chapterId = "chapter_19",
        title = "The Dead Archivist",
        illustrationId = "the_dead_archivist",
        narrativeText = """
            She answers the demand at once, words coming out clipped, like she's relieved to have
            the question forced rather than volunteered. "I don't know. I never found a name,
            only a workshop nobody local remembered him keeping, and notes in a hand that got
            smaller and more certain the deeper into the design it went, like whoever wrote them
            understood the frame better the longer they spent almost finishing it." She looks at
            Kaelen directly for the first time since the duel. "I used to think I found him. I've
            started to think it's closer to the truth that I was allowed to."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask what came before the drawings.", nextNodeId = "c19_ashwell_years")
        )
    ),
    StoryNode(
        id = "the_dead_archivist_patient",
        chapterId = "chapter_19",
        title = "The Dead Archivist",
        illustrationId = "the_dead_archivist",
        narrativeText = """
            She takes her time getting to it, the way a person does when a story has been sitting
            unspoken for eleven years and finally has somewhere to go. "I don't know. I never
            found a name, only a workshop nobody local remembered him keeping, and notes in a
            hand that got smaller and more certain the deeper into the design it went, like
            whoever wrote them understood the frame better the longer they spent almost finishing
            it." She looks at Kaelen directly for the first time since the duel. "I used to think
            I found him. I've started to think it's closer to the truth that I was allowed to."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask what came before the drawings.", nextNodeId = "c19_ashwell_years")
        )
    ),
    StoryNode(
        id = "c19_ashwell_years",
        chapterId = "chapter_19",
        title = "Four Years Before the Plans",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            "Ashwell was fifteen years ago," Ilsevet said. "For four years afterward I tried to make
            a ward that would never fail the way ours had. I drew circles, burned pages and buried
            more mistakes than I can count. Then, eleven years ago, the work changed." Kaelen heard
            the distinction. The grief had come first. Whatever had changed the work had come later.
            She had still chosen to follow it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Ask why she trusted the drawings.", nextNodeId = "c19_the_familiar_hand"))
    ),
    StoryNode(
        id = "c19_the_familiar_hand",
        chapterId = "chapter_19",
        title = "An Answer Too Convenient",
        illustrationId = "what_ilsevet_knew",
        narrativeText = """
            "Because they answered questions I had not told anyone I was asking," she said. "Every
            correction was where I needed it. Every missing piece looked as though it had been left
            for me." Kaelen thought of the six settings in Ashfall. "You never wondered why?" "Of
            course I did." Her expression hardened. "I wondered, and I kept building. Those are two
            different failures." She looked away before he could answer.
        """.trimIndent(),
        choices = listOf(Choice(label = "Take the account to the gate.", nextNodeId = "the_prisoners_reaction"))
    ),
    StoryNode(
        id = "the_prisoners_reaction",
        chapterId = "chapter_19",
        title = "The Prisoner's Reaction",
        illustrationId = "the_prisoners_reaction",
        narrativeText = """
            The prisoner goes quiet for longer than it ever has, once Kaelen repeats it back at
            the gate that night. "A hand that understood the frame better the longer it worked,"
            it says finally, "is not a description of a person learning. It's a description of
            something remembering."

            It doesn't finish the thought. For the first time, Kaelen gets the distinct sense
            it's afraid of finishing it, and doesn't press for the rest.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask about the archivist.", nextNodeId = "c19_prisoner_pause")
        )
    ),
    StoryNode(
        id = "c19_prisoner_pause",
        chapterId = "chapter_19",
        title = "The Word It Would Not Use",
        illustrationId = "the_prisoners_reaction",
        narrativeText = """
            The prisoner repeated the phrase "a hand that remembered" very softly. Then it fell
            silent. Kaelen had heard it threaten, bargain and mock. He had never heard it search for
            a word and refuse to speak it. "Did you know the archivist?" he asked. "You have not yet
            told me what you think I am," it replied. "I am asking what you know." "Then ask about
            the writing, not the writer."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask what the ink remembers.", nextNodeId = "c19_ink"),
            Choice(label = "Ask what the prisoner fears.", nextNodeId = "c19_fear")
        )
    ),
    StoryNode(
        id = "c19_ink",
        chapterId = "chapter_19",
        title = "A Hand Without a Life",
        illustrationId = "the_prisoners_reaction",
        narrativeText = """
            "Ink does not remember," the prisoner said. "But a pattern can be copied until no one
            remembers where the first line was drawn." Kaelen thought of the same careful hand
            appearing on every page. "And if the person who drew it died?" The silence behind the
            door deepened. "Then you should ask why the next page looks the same." It would give him
            no more than that.
        """.trimIndent(),
        choices = listOf(Choice(label = "Write to Selvane.", nextNodeId = "c19_letter"))
    ),
    StoryNode(
        id = "c19_fear",
        chapterId = "chapter_19",
        title = "What It Recognised",
        illustrationId = "the_prisoners_reaction",
        narrativeText = """
            "I fear being certain," the prisoner said. "I have been certain before." The answer was
            so unlike its usual riddles that Kaelen nearly missed its meaning. "Of what?" "That a
            voice belonged to someone who was gone." Kaelen waited. The prisoner did not speak
            again. He left with a question that felt heavier than a warning and less useful than a
            name.
        """.trimIndent(),
        choices = listOf(Choice(label = "Write to Selvane.", nextNodeId = "c19_letter"))
    ),
    StoryNode(
        id = "c19_letter",
        chapterId = "chapter_19",
        title = "The Request for Evidence",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            Kaelen's letter to Selvane contained no theory. He gave her the place Ilsevet named, the
            year she claimed to have found the pages and the description of the handwriting. He
            asked for the dates to be checked before anyone called the drawings prophecy. Voss read
            the letter before it was sealed. "You've left out what the prisoner said." "It didn't
            say enough." "Then write that too." He did.
        """.trimIndent(),
        choices = listOf(Choice(label = "Send the letter.", nextNodeId = "what_selvane_finds"))
    ),
    StoryNode(
        id = "what_selvane_finds",
        chapterId = "chapter_19",
        title = "What Selvane Finds",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            Selvane, when word reaches her, doesn't waste time being skeptical, not after
            Ashfall, not after everything her own archives already confirmed about Ashwell. What
            her people find at the dead archivist's old workshop, three weeks and a hard ride
            later, isn't proof of who wrote the later pages.

            It's five more sets of frame notes, in the same hand, each one addressed, by name, in
            careful, deliberate script, to someone who hadn't yet taken up the role the page gave
            them when the ink dried.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Read Selvane's report.",
                nextNodeId = "c19_report_seal",
                consequences = Consequences(grantItemIds = listOf("archivists_frame_notes"))
            )
        )
    ),
    StoryNode(
        id = "c19_report_seal",
        chapterId = "chapter_19",
        title = "The Report Arrives",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            The report came in a leather tube bearing Selvane's seal. Its pages smelled of damp
            paper and the oil used to preserve brittle records. There were sketches of a bench, a
            narrow window and five envelopes laid side by side. Selvane had written a warning in the
            margin: "Do not mistake an addressed name for a dated birth.". Kaelen read it twice. The
            dates were certain enough. Their meaning was not.
        """.trimIndent(),
        choices = listOf(Choice(label = "Compare the dates.", nextNodeId = "c19_dates"))
    ),
    StoryNode(
        id = "c19_dates",
        chapterId = "chapter_19",
        title = "Before the Role, Not Before the Child",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            The oldest legible dates were eleven years old. Whoever the pages addressed had been
            alive then. The impossible part was not that they existed but the roles written beside
            their names: keepers, witnesses, places none of them had yet reached. The writing knew a
            future meeting. That was disturbing enough without claiming it had named children before
            their births.
        """.trimIndent(),
        choices = listOf(Choice(label = "Examine the envelopes.", nextNodeId = "c19_envelopes"))
    ),
    StoryNode(
        id = "c19_envelopes",
        chapterId = "chapter_19",
        title = "Five Closed Answers",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            Five envelopes had been found together, each folded in the same manner and sealed with
            wax of the same dull colour. Selvane's investigators had opened four and recognised the
            names inside. They had left the last sealed. Voss ran her thumb along the edge of the
            register beside them without touching the ink.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Ask Voss what she sees.", nextNodeId = "c19_voss_reading"),
            Choice(label = "Look at the first line alone.", nextNodeId = "c19_kaelen_reading")
        )
    ),
    StoryNode(
        id = "c19_voss_reading",
        chapterId = "chapter_19",
        title = "A Name Is Not an Order",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            "It says where someone will stand," Voss said. "It doesn't say what they'll do there."
            Kaelen looked at the line again. She was right. The page described a place, a person and
            a moment, but no choice anyone had made since. "Someone wanted whoever reads this to
            think the rest was settled," he said. "Then don't help them," Voss replied. It was a
            plain answer, and he needed one.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn to the last envelope.", nextNodeId = "c19_choice_preparation"))
    ),
    StoryNode(
        id = "c19_kaelen_reading",
        chapterId = "chapter_19",
        title = "A Hand Over the Page",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            He covered the first line with his palm and uncovered it again. The letters did not
            change. He had spent months being told what he was meant to guard, break, carry or
            become, and the page read like one more voice doing the same. He thought of Halvard's
            chain. Halvard had given it to him, but had never told him what kind of man wearing it
            would make him.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn to the last envelope.", nextNodeId = "c19_choice_preparation"))
    ),
    StoryNode(
        id = "c19_choice_preparation",
        chapterId = "chapter_19",
        title = "What Was Written",
        illustrationId = "what_selvane_finds",
        narrativeText = """
            The room waited for Kaelen to open the last envelope as though the page had spoken
            aloud. He almost laughed at that. A name in ink could be a warning, a trap or a record.
            It could not make a decision. Still, the hand that wrote it had known something it
            should not have known. Whatever it claimed, he could deny it, accept the burden of
            answering it or refuse to give it an answer yet.
        """.trimIndent(),
        choices = listOf(Choice(label = "Ask Voss to read the names aloud.", nextNodeId = "the_addressed_names"))
    ),
    StoryNode(
        id = "the_addressed_names",
        chapterId = "chapter_19",
        title = "The Addressed Names",
        illustrationId = "the_addressed_names",
        narrativeText = """
            Voss reads the names aloud, one at a time, and stops cold on the fourth. Thessaly's
            isn't a surprise; Fenmoor was always going to be found eventually. It's the fifth
            name that stops the room: Kaelen's own, written in ink dry a decade before he ever
            stood in front of the black door.

            Whoever drew those plans didn't just anticipate the seventh door. They anticipated
            him, specifically, standing exactly where he's standing now, and the room goes quiet
            in a way that has nothing to do with respect.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then whoever it is, they've been waiting on me specifically. Let them keep waiting.\"",
                nextNodeId = "c19_after_defiance",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 17)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("defied_the_prophecy")
                )
            ),
            Choice(
                label = "\"If I was always meant to stand here, I intend to make sure I'm the one who decides what that means.\"",
                nextNodeId = "c19_after_claim",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 16)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("claimed_his_own_meaning")
                )
            ),
            Choice(
                label = "Say nothing. There isn't an answer that fits in a sentence.",
                nextNodeId = "c19_after_silence",
                consequences = Consequences(setFlags = setOf("left_it_unanswered"))
            )
        )
    ),
    StoryNode(
        id = "c19_after_defiance",
        chapterId = "chapter_19",
        title = "A Line Crossed",
        illustrationId = "the_addressed_names",
        narrativeText = """
            Kaelen had refused the page the satisfaction of his fear. It did not make the dates less
            strange. It made the next question easier to ask: who had needed him to read it? He
            folded the report carefully. Somewhere a dead man's hand had left instructions for a
            living man. He intended to find out who was still following them.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter19_end_defied"))
    ),
    StoryNode(
        id = "c19_after_claim",
        chapterId = "chapter_19",
        title = "The Name He Keeps",
        illustrationId = "the_addressed_names",
        narrativeText = """
            He would not give his name back to the person who wrote it. That was the first thing he
            understood. The second was that a written future could still contain a choice, even if
            the writer had tried to leave no room for one. Voss watched him close the report.
            "Ready?" she asked. "No," he said. "But I know what I'm going to ask next."
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter19_end_claimed"))
    ),
    StoryNode(
        id = "c19_after_silence",
        chapterId = "chapter_19",
        title = "An Answer Deferred",
        illustrationId = "the_addressed_names",
        narrativeText = """
            Kaelen said nothing. The room eventually began to move again around him. Someone folded
            a map. Someone closed a shutter against the wind. He had not agreed with the page, and
            he had not challenged it. He had simply refused to pretend that a sentence could settle
            the thing. Later, when he looked at the fifth name again, he found that the silence
            still belonged to him.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on.", nextNodeId = "chapter19_end_unanswered"))
    ),
    StoryNode(
        id = "chapter19_end_defied",
        chapterId = "chapter_19",
        title = "End of Chapter XIX — Already Written",
        illustrationId = "chapter19_threshold",
        narrativeText = """
            Ilsevet's arithmetic is finished, one way or another. Whatever's left of her faction
            without her to run it is Selvane's problem now, or the Order's, or nobody's; Kaelen
            finds he genuinely doesn't know which, and isn't sure it matters as much as he once
            thought it would.

            Defiance doesn't make the ink any less dry, or the workshop any less real, but it
            gives Kaelen somewhere to put the unease instead of just carrying it. What matters is
            five names, one workshop, and a hand that got surer of the frame the longer it worked
            on something it wouldn't live to see finished. Ilsevet spent fifteen years thinking
            she chose this. Kaelen isn't sure anymore that either of them ever really did, and he
            intends to make whoever's waiting regret the wait regardless.

            Chapter XX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "c20_before_departure")
        )
    ),
    StoryNode(
        id = "chapter19_end_claimed",
        chapterId = "chapter_19",
        title = "End of Chapter XIX — Already Written",
        illustrationId = "chapter19_threshold",
        narrativeText = """
            Ilsevet's arithmetic is finished, one way or another. Whatever's left of her faction
            without her to run it is Selvane's problem now, or the Order's, or nobody's; Kaelen
            finds he genuinely doesn't know which, and isn't sure it matters as much as he once
            thought it would.

            Claiming his own meaning doesn't make the ink any less dry, or the workshop any less
            real, but it changes what he does with knowing. What matters is five names, one
            workshop, and a hand that got surer of the frame the longer it worked on something it
            wouldn't live to see finished. Ilsevet spent fifteen years thinking she chose this.
            Kaelen isn't sure anymore that either of them ever really did. He intends to be the
            one exception to that, whatever it costs to insist on it.

            Chapter XX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "c20_before_departure")
        )
    ),
    StoryNode(
        id = "chapter19_end_unanswered",
        chapterId = "chapter_19",
        title = "End of Chapter XIX — Already Written",
        illustrationId = "chapter19_threshold",
        narrativeText = """
            Ilsevet's arithmetic is finished, one way or another. Whatever's left of her faction
            without her to run it is Selvane's problem now, or the Order's, or nobody's; Kaelen
            finds he genuinely doesn't know which, and isn't sure it matters as much as he once
            thought it would.

            Not answering doesn't make the question go away. It just means Kaelen carries it a
            while longer before he has to. What matters is five names, one workshop, and a hand
            that got surer of the frame the longer it worked on something it wouldn't live to see
            finished. Ilsevet spent fifteen years thinking she chose this. Kaelen isn't sure
            anymore that either of them ever really did.

            Chapter XX awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "c20_before_departure")
        )
    )
)

val chapter19 = com.thelastjailer.app.Chapter(
    id = "chapter_19",
    number = 19,
    title = "Chapter XIX — Already Written",
    startNodeId = "what_ilsevet_knew"
)
