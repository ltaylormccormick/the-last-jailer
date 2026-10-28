package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XX — Whoever Finished It.
 *
 * Kaelen visits the dead archivist's workshop himself rather than relying on Selvane's secondhand
 * report — going alone versus bringing Voss changes the texture of the trip there, not just a
 * stat. The reveal deepens rather than resolves XIX's mystery: the archivist has been dead for
 * years, but the notes kept being written after his death, in the same hand — something finished
 * his work rather than merely continuing it. Twentieth and toughest combat encounter yet (The
 * Ghostwriter, 280 HP, wearing the dead man's coat "like a memory that hasn't noticed it doesn't
 * fit anymore"). The workshop's final page is a message addressed to "the sixth" — which the
 * prisoner recognizes, for the first time responding with grief rather than fear or wary alliance,
 * confirming it has a real history with whatever is behind all of this, and whether Kaelen demands
 * a name, gives it time, or simply waits now shapes how it responds to that grief. Deliberately
 * doesn't name the entity yet; the prisoner's answer is held for a future chapter.
 *
 * Continues the worldbuilding thread opened in XIV (the vision of the six wards severed from
 * something closer to a choice) and escalated in XIX (Kaelen's name written into the design before
 * he existed) — this is the natural next step in the same arc rather than a new tonal swing, so no
 * separate judgment-call flag this round beyond the one already raised in XIX's PR.
 */
val chapter20Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "c20_before_departure",
        chapterId = "chapter_20",
        title = "The Weight of a Report",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The workshop had become a place on a map and a bundle of pages on Kaelen's table. That
            was not enough. He wanted to see the bench where the ink had dried, the window the
            archivist had looked through and the door Selvane's people had marked. Voss found him
            studying the road before dawn. "You're going," she said. "Yes." She looked at the
            report. "Then take the part that says what they actually found. Leave the guesses."
        """.trimIndent(),
        choices = listOf(Choice(label = "Decide who goes.", nextNodeId = "the_workshop"))
    ),
    StoryNode(
        id = "the_workshop",
        chapterId = "chapter_20",
        title = "The Workshop",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Selvane's report is thorough. It is also, Kaelen decides within a day of reading it,
            not the same as seeing the place himself. Whatever wrote five names into a design
            eleven years before any of them had taken up the roles it
            gave them deserves more than a secondhand account, and
            some part of him needs to see the ink for himself before he can believe any of it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll go alone. This deserves a direct look, not a delegation.\"",
                nextNodeId = "c20_alone_mile",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("went_to_workshop_alone")
                )
            ),
            Choice(
                label = "\"Voss comes with me. Two sets of eyes, not one.\"",
                nextNodeId = "c20_with_voss_mile",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("brought_voss_to_workshop")
                )
            )
        )
    ),
    StoryNode(
        id = "c20_alone_mile",
        chapterId = "chapter_20",
        title = "The Road Without Witnesses",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            The first mile alone was easier than Kaelen expected. He could stop whenever the road
            offered a question and take as long as he needed to answer it. The second mile was
            worse. Every noise behind him became a footstep, and every footstep reminded him how
            much he had grown used to Voss being there. He kept walking. Solitude was a choice he
            had made, not a punishment the road had imposed.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep to the road.", nextNodeId = "the_road_to_the_workshop_alone"))
    ),
    StoryNode(
        id = "c20_with_voss_mile",
        chapterId = "chapter_20",
        title = "Two Sets of Eyes",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            Voss carried the report and made him carry the water. It was a small division of labour,
            but it settled them into the road. At the first fork she spotted Selvane's mark on a
            stone before he did. At the second she asked why the map had been copied twice in
            different hands. Kaelen had no answer. "Good," she said. "That means we have something
            to ask when we get there."
        """.trimIndent(),
        choices = listOf(Choice(label = "Follow Selvane's markers.", nextNodeId = "the_road_to_the_workshop_with_voss"))
    ),
    StoryNode(
        id = "the_road_to_the_workshop_alone",
        chapterId = "chapter_20",
        title = "The Road to the Workshop",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            He goes alone, and something about the solitude feels correct for what he's about to
            read, the way certain conversations only work with exactly one listener.

            The workshop sits half-collapsed at the edge of a village that stopped being a
            village sometime in the last decade, the kind of quiet abandonment that doesn't leave
            ruins so much as questions. Selvane's people marked the door. Nobody local,
            apparently, ever remembers seeing it open.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Enter the village.", nextNodeId = "c20_village_edge")
        )
    ),
    StoryNode(
        id = "the_road_to_the_workshop_with_voss",
        chapterId = "chapter_20",
        title = "The Road to the Workshop",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            Voss falls into step beside him without needing to be asked twice, and Kaelen finds
            he's glad of the company sooner than he expected to be, the road out feeling longer
            somehow when there's someone to notice it with him.

            The workshop sits half-collapsed at the edge of a village that stopped being a
            village sometime in the last decade, the kind of quiet abandonment that doesn't leave
            ruins so much as questions. Selvane's people marked the door. Nobody local,
            apparently, ever remembers seeing it open.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Enter the village.", nextNodeId = "c20_village_edge")
        )
    ),
    StoryNode(
        id = "c20_village_edge",
        chapterId = "chapter_20",
        title = "The Village That Left",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            Kaelen walked in past the last field wall. The village had not burned. It had simply
            emptied. Roofs sagged under moss, a trough held rainwater and a child's wooden hoop lay
            half-buried beside the road. The workshop stood apart from the houses, its narrow
            windows turned away from the square. Whoever had worked there had wanted light without
            company. Kaelen wondered whether the archivist had chosen that loneliness or inherited
            it from the work.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Inspect the village records.", nextNodeId = "c20_register"),
            Choice(label = "Approach the workshop directly.", nextNodeId = "c20_workshop_door")
        )
    ),
    StoryNode(
        id = "c20_register",
        chapterId = "chapter_20",
        title = "The Last Ordinary Entry",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            The village register ended with a list of grain owed and two names crossed out in a
            different hand. There was no entry for an archivist, no lease on the workshop and no
            record of anyone dying there. That absence did not prove he had never lived. It proved
            only that the village had stopped writing about him, or had never thought to begin.
            Kaelen copied the final date before leaving.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the workshop.", nextNodeId = "c20_workshop_door"))
    ),
    StoryNode(
        id = "c20_workshop_door",
        chapterId = "chapter_20",
        title = "The Mark on the Lintel",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            Selvane's people had marked the lintel with a clean white stroke. Beneath it lay an
            older mark, so worn Kaelen could not tell whether it had been carved or burned. The door
            opened without resistance. Someone had been here recently enough to clear the threshold,
            but not recently enough to chase away the smell of damp paper. He stood outside for one
            breath longer, then crossed into the room.
        """.trimIndent(),
        choices = listOf(Choice(label = "Examine the first table.", nextNodeId = "c20_bench"))
    ),
    StoryNode(
        id = "c20_bench",
        chapterId = "chapter_20",
        title = "The Work of One Hand",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            The bench held three kinds of work. The oldest pages were measured and hesitant, crowded
            with corrections. Later sheets were cleaner, the lines more certain. The newest pages
            were almost bare of mistakes. Kaelen set them beside one another. The handwriting
            matched. The confidence did not. He thought of the prisoner's word, "remembering", and
            wished it had offered him a less frightening one.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Study the ink.", nextNodeId = "c20_ink_layers"),
            Choice(label = "Look for the archivist's belongings.", nextNodeId = "c20_personal_effects")
        )
    ),
    StoryNode(
        id = "c20_ink_layers",
        chapterId = "chapter_20",
        title = "Six Years of Impossible Work",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            Some pages had been written long after the others. The paper had come from a different
            mill; the ink was made with a later recipe. Yet the hand was the same down to the way it
            closed the letter a. Kaelen checked the dates against Selvane's notes. Whatever had
            continued the work had not been careful to hide the passage of time. It had only been
            careful to keep the handwriting.
        """.trimIndent(),
        choices = listOf(Choice(label = "Look behind the bench.", nextNodeId = "c20_recess"))
    ),
    StoryNode(
        id = "c20_personal_effects",
        chapterId = "chapter_20",
        title = "The Man Before the Pages",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            A cup with a chipped handle stood beneath the bench. Beside it lay a mended glove and a
            small knife sharpened almost to nothing. They belonged to someone who had eaten, worked
            and worn things out. Kaelen held the glove for a moment. The archivist had been a person
            before he became a mystery. That mattered. It made the later pages an intrusion into a
            life, not merely a clever trick.
        """.trimIndent(),
        choices = listOf(Choice(label = "Look behind the bench.", nextNodeId = "c20_recess"))
    ),
    StoryNode(
        id = "c20_recess",
        chapterId = "chapter_20",
        title = "What Selvane Could Not See",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            A shelving unit had fallen against the back wall. Beyond it was a shallow recess hidden
            from the doorway. Kaelen moved the loose boards and found the remains of a man beneath a
            collapsed coat, long undisturbed. Selvane's investigators had catalogued the desk and
            the notes. They had not moved the shelf. Their report had been incomplete, not
            dishonest. The distinction offered little comfort. The dead man had remained here while
            his handwriting travelled on.
        """.trimIndent(),
        choices = listOf(Choice(label = "Check the oldest page against the remains.", nextNodeId = "c20_oldest_page"))
    ),
    StoryNode(
        id = "c20_oldest_page",
        chapterId = "chapter_20",
        title = "Where the Hand Stopped",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            The oldest page ended halfway through a calculation. Its last line shook, then broke
            off. The next sheet began the calculation again in a steadier hand. No note explained
            the interruption. No name marked the change. Kaelen imagined a person sitting here,
            setting down a pen for the last time, and something else taking it up. He did not know
            whether the image was true. He knew he could no longer dismiss it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go to the workbench's centre.", nextNodeId = "what_the_workshop_holds"))
    ),
    StoryNode(
        id = "what_the_workshop_holds",
        chapterId = "chapter_20",
        title = "What the Workshop Holds",
        illustrationId = "what_the_workshop_holds",
        narrativeText = """
            Dust doesn't explain everything. The workbench holds decades of notes in the same
            hand Ilsevet described, and near the back of the room, what's left of the man who
            apparently wrote the earliest pages: bones, long settled, undisturbed since whatever
            killed him. The ink on the desk is newer than the bones by at least six years.

            Someone, or something, kept writing after the hand that started it stopped being able
            to, and never once let the handwriting slip.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Keep reading.", nextNodeId = "c20_guardian_warning")
        )
    ),
    StoryNode(
        id = "c20_guardian_warning",
        chapterId = "chapter_20",
        title = "A Page Moves",
        illustrationId = "the_workshops_own_guardian",
        narrativeText = """
            Kaelen kept reading until the loose page lifted at one corner, though the windows were
            shut. He put his hand over it. Something answered from the darkness behind the bench, a
            pressure rather than a sound. He drew his sword. The ink on the page remained still. The
            room did not. Whatever had guarded this work had mistaken his arrival for an
            interruption, and it had no intention of allowing the sentence to end elsewhere.
        """.trimIndent(),
        choices = listOf(Choice(label = "Face it.", nextNodeId = "the_workshops_own_guardian"))
    ),
    StoryNode(
        id = "the_workshops_own_guardian",
        chapterId = "chapter_20",
        title = "The Workshop's Own Guardian",
        illustrationId = "the_workshops_own_guardian",
        narrativeText = """
            Whatever finished the work doesn't take kindly to being interrupted mid-page. It
            rises out of the dark at the back of the workshop the way water rises when something
            displaces it, no clean shape, just pressure and intent, wearing the dead archivist's
            coat like a memory that hasn't noticed it doesn't fit anymore.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "ghostwriter_encounter"
    ),
    StoryNode(
        id = "ghostwriter_defeat",
        chapterId = "chapter_20",
        title = "It Wears Itself Thin",
        illustrationId = "the_workshops_own_guardian",
        narrativeText = """
            It doesn't hit like a person, it hits like weather, pressure without a clean edge to
            block or parry, and Kaelen spends more of the fight finding out what that means than
            doing anything useful about it. He's on the workshop floor, ears ringing, before he
            manages to land anything that actually counts.

            Whatever's wearing the dead archivist's coat doesn't seem to notice the difference
            between winning and simply continuing, and that turns out to be its own kind of
            weakness: it keeps pressing long after there's nothing left of itself to press with,
            and dissolves back into the dark mid-motion, worn through rather than beaten.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Check the workbench.",
                nextNodeId = "c20_after_guardian",
                consequences = Consequences(setFlags = setOf("lost_to_the_ghostwriter"))
            )
        )
    ),
    StoryNode(
        id = "c20_after_guardian",
        chapterId = "chapter_20",
        title = "The Coat on the Floor",
        illustrationId = "the_workshops_own_guardian",
        narrativeText = """
            When the pressure faded, the coat lay empty. Kaelen did not touch it at once; he went to
            the workbench instead. The thing had worn a dead man's belongings without becoming the
            man. That was the part he wanted to remember. The fight had proved that the workshop
            could defend itself. It had not proved who gave the order, or whether anyone living
            still knew the order existed.
        """.trimIndent(),
        choices = listOf(Choice(label = "Look at the final page.", nextNodeId = "c20_final_margin"))
    ),
    StoryNode(
        id = "c20_final_margin",
        chapterId = "chapter_20",
        title = "A Hand That Hesitated",
        illustrationId = "the_last_page",
        narrativeText = """
            The last page was unlike the rest. Its lines were uneven. One word had been written
            twice and crossed out once. The writer had known how to finish the frame and yet seemed
            uncertain how to finish a sentence. Kaelen thought of the dead man's glove beneath the
            bench. He could not decide whether the hesitation made the message more human or more
            terrible.
        """.trimIndent(),
        choices = listOf(Choice(label = "Read the message.", nextNodeId = "the_last_page"))
    ),
    StoryNode(
        id = "the_last_page",
        chapterId = "chapter_20",
        title = "The Last Page",
        illustrationId = "the_last_page",
        narrativeText = """
            The workbench's last page is different from the rest, not
            plans this time, just a single line, in a hand gone suddenly less certain than all
            the pages before it:

            "Tell the sixth I remember being asked, once, whether I wanted this. Tell him I said
            yes before I understood the question. Tell him that's not the same as choosing."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the page, and go home.",
                nextNodeId = "c20_sixth_question",
                consequences = Consequences(grantItemIds = listOf("archivists_final_page"))
            )
        )
    ),
    StoryNode(
        id = "c20_sixth_question",
        chapterId = "chapter_20",
        title = "The Space After Five",
        illustrationId = "the_last_page",
        narrativeText = """
            He took the page and started for home, and the message kept pace with him. The message
            addressed a sixth, though Selvane had found only five names. Kaelen counted the
            envelopes again in his mind. Five people had been named. A sixth had been remembered.
            The distinction felt deliberate. He resisted the urge to supply a name simply to close
            the gap. The last page asked for something more difficult than recognition: it asked
            someone to distinguish agreement from choice.
        """.trimIndent(),
        choices = listOf(Choice(label = "Keep walking.", nextNodeId = "c20_homeward"))
    ),
    StoryNode(
        id = "c20_homeward",
        chapterId = "chapter_20",
        title = "The Road Back",
        illustrationId = "the_road_to_the_workshop",
        narrativeText = """
            The road home seemed shorter, though the page in Kaelen's pack made every step heavier.
            He passed the abandoned trough and Selvane's marks without stopping. The workshop had
            offered proof of a dead man and a hand that continued without him. It had not offered an
            explanation. He wondered whether the prisoner would hear the difference when he read the
            final line aloud.
        """.trimIndent(),
        choices = listOf(Choice(label = "Return to the black door.", nextNodeId = "what_the_prisoner_says_now"))
    ),
    StoryNode(
        id = "what_the_prisoner_says_now",
        chapterId = "chapter_20",
        title = "What the Prisoner Says Now",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner doesn't answer right away when Kaelen repeats the message at the gate.
            When it finally does, its voice has changed, not fear this time, not the wary
            alliance of the last several months. Something closer to grief.

            "I know that voice," it says. "I haven't heard it in longer than your kingdom has
            existed. I didn't think there was enough of it left to still be sending anything."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Who is it? Tell me plainly.\"",
                nextNodeId = "chapter20_end_demanded",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 18)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("demanded_the_truth")
                )
            ),
            Choice(
                label = "\"Take whatever time you need. I'm not going anywhere.\"",
                nextNodeId = "chapter20_end_gave_time",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 17)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("gave_the_prisoner_time")
                )
            ),
            Choice(
                label = "Wait, and let it decide whether to say more.",
                nextNodeId = "chapter20_end_waited",
                consequences = Consequences(setFlags = setOf("waited_in_silence"))
            )
        )
    ),
    StoryNode(
        id = "chapter20_end_demanded",
        chapterId = "chapter_20",
        title = "End of Chapter XX — Whoever Finished It",
        illustrationId = "chapter20_threshold",
        narrativeText = """
            It doesn't answer the demand, not tonight, and for once its silence doesn't feel like
            evasion so much as genuine inability. "Not like this," it says. "Not shouted at.
            Please." It's the first time it's ever said please to him.

            Kaelen leaves the gate no closer to a name than he arrived with, carrying instead the
            specific, unsettling knowledge that whatever wrote those five names has been keeping
            track of all six wards for far longer than anyone still living has been keeping track
            of anything.

            Ilsevet spent eleven years thinking she was building something new. Kaelen is
            starting to suspect nobody involved in any of this has ever been doing anything
            except finishing somebody else's very old, very patient work.

            Chapter XXI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_prisoner_answers")
        )
    ),
    StoryNode(
        id = "chapter20_end_gave_time",
        chapterId = "chapter_20",
        title = "End of Chapter XX — Whoever Finished It",
        illustrationId = "chapter20_threshold",
        narrativeText = """
            It doesn't take him up on the offer that night, though something in the quality of
            its silence afterward feels less strained than usual, like patience was the one thing
            nobody had thought to give it in longer than it could easily remember.

            Kaelen leaves the gate no closer to a name than he arrived with, carrying instead the
            specific, unsettling knowledge that whatever wrote those five names has been keeping
            track of all six wards for far longer than anyone still living has been keeping track
            of anything.

            Ilsevet spent eleven years thinking she was building something new. Kaelen is
            starting to suspect nobody involved in any of this has ever been doing anything
            except finishing somebody else's very old, very patient work.

            Chapter XXI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_prisoner_answers")
        )
    ),
    StoryNode(
        id = "chapter20_end_waited",
        chapterId = "chapter_20",
        title = "End of Chapter XX — Whoever Finished It",
        illustrationId = "chapter20_threshold",
        narrativeText = """
            The silence stretches long enough that Kaelen starts to wonder if it will ever be
            broken tonight, and eventually decides that not breaking it himself is its own kind
            of answer worth giving.

            Whatever answer the prisoner eventually gives, it isn't tonight's. Kaelen leaves the
            gate no closer to a name than he arrived with, carrying instead the specific,
            unsettling knowledge that whatever wrote those five names has been keeping track of
            all six wards for far longer than anyone still living has been keeping track of
            anything.

            Ilsevet spent eleven years thinking she was building something new. Kaelen is
            starting to suspect nobody involved in any of this has ever been doing anything
            except finishing somebody else's very old, very patient work.

            Chapter XXI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_prisoner_answers")
        )
    )
)

val chapter20 = com.thelastjailer.app.Chapter(
    id = "chapter_20",
    number = 20,
    title = "Chapter XX — Whoever Finished It",
    startNodeId = "the_workshop"
)
