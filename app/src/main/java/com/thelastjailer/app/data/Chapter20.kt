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
        id = "the_workshop",
        chapterId = "chapter_20",
        title = "The Workshop",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Selvane's report is thorough. It is also, Kaelen decides within a day of reading it,
            not the same as seeing the place himself. Whatever wrote five names into a design
            eleven years before any of them existed deserves more than a secondhand account, and
            some part of him needs to see the ink for himself before he can believe any of it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"I'll go alone. This deserves a direct look, not a delegation.\"",
                nextNodeId = "the_road_to_the_workshop_alone",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("went_to_workshop_alone")
                )
            ),
            Choice(
                label = "\"Voss comes with me. Two sets of eyes, not one.\"",
                nextNodeId = "the_road_to_the_workshop_with_voss",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("brought_voss_to_workshop")
                )
            )
        )
    ),
    StoryNode(
        id = "the_road_to_the_workshop_alone",
        chapterId = "chapter_20",
        title = "The Road to the Workshop",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He goes alone, and something about the solitude feels correct for what he's about to
            read, the way certain conversations only work with exactly one listener.

            The workshop sits half-collapsed at the edge of a village that stopped being a
            village sometime in the last decade, the kind of quiet abandonment that doesn't leave
            ruins so much as questions. Selvane's people marked the door. Nobody local,
            apparently, ever remembers seeing it open.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go inside.", nextNodeId = "what_the_workshop_holds")
        )
    ),
    StoryNode(
        id = "the_road_to_the_workshop_with_voss",
        chapterId = "chapter_20",
        title = "The Road to the Workshop",
        illustrationId = "road_away_from_tree",
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
            Choice(label = "Go inside.", nextNodeId = "what_the_workshop_holds")
        )
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
            Choice(label = "Keep reading.", nextNodeId = "the_workshops_own_guardian")
        )
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
        id = "the_last_page",
        chapterId = "chapter_20",
        title = "The Last Page",
        illustrationId = "the_last_page",
        narrativeText = """
            It goes still, eventually, dissolving back into whatever passed for dark before
            Kaelen's blade found it. The workbench's last page is different from the rest, not
            plans this time, just a single line, in a hand gone suddenly less certain than all
            the pages before it:

            "Tell the sixth I remember being asked, once, whether I wanted this. Tell him I said
            yes before I understood the question. Tell him that's not the same as choosing."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the page, and go home.",
                nextNodeId = "what_the_prisoner_says_now",
                consequences = Consequences(grantItemIds = listOf("archivists_final_page"))
            )
        )
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

            Ilsevet spent fifteen years thinking she was building something new. Kaelen is
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

            Ilsevet spent fifteen years thinking she was building something new. Kaelen is
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

            Ilsevet spent fifteen years thinking she was building something new. Kaelen is
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
