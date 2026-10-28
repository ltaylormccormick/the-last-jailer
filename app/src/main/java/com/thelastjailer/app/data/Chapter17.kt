package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

private const val C17_VISITED_STORES = "c17_visited_stores"
private const val C17_VISITED_WATCH = "c17_visited_watch"
private const val C17_VISITED_RECORDS = "c17_visited_records"
private const val C17_TOOK_GUARD = "c17_took_guard"
private const val C17_STUDIED_JUNCTIONS = "c17_studied_junctions"

/**
 * What the preparation at Stonebeard leaves behind: which of the three sites Kaelen visited, whether he took a guard from the watch,
 * and whether he studied where the old records' lines meet. The chain at the gate clears all five as it begins, so a replay reaches
 * the hub with an empty slate, and the final "Enter the chamber." choice clears them again once every payoff has read them.
 */
val C17_PREPARATION_FLAGS: Set<String> = setOf(
    C17_VISITED_STORES, C17_VISITED_WATCH, C17_VISITED_RECORDS, C17_TOOK_GUARD, C17_STUDIED_JUNCTIONS
)

private val C17_CLEAR = Consequences(clearFlags = C17_PREPARATION_FLAGS)

/**
 * The step after the chain at the gate: what Stonebeard made of the answer Kaelen gave in Chapter XIII about who stands with it.
 * Training new wardens, standing the watch himself and leaving the question open are three exclusive flags, and the last copy is read
 * by the absence of the other two, so a save that holds none still goes on. Each copy clears the preparation markers.
 */
private fun stonebeardsAnswer(label: String): List<Choice> = listOf(
    Choice(
        label = label,
        nextNodeId = "c17_wardens",
        requirements = ChoiceRequirement(requiredFlags = setOf("began_training_new_wardens")),
        consequences = C17_CLEAR
    ),
    Choice(
        label = label,
        nextNodeId = "c17_watch",
        requirements = ChoiceRequirement(requiredFlags = setOf("stood_watch_rather_than_delegate")),
        consequences = C17_CLEAR
    ),
    Choice(
        label = label,
        nextNodeId = "c17_council",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("began_training_new_wardens", "stood_watch_rather_than_delegate")),
        consequences = C17_CLEAR
    )
)

/**
 * The step after the policy scene: the door Kaelen told Edda about in Chapter XIII, open or barred. A player who made Edda no promise
 * has neither, and goes straight to the preparation, so the last copy is read by the absence of both.
 */
private fun theDoor(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c17_door_open", requirements = ChoiceRequirement(requiredFlags = setOf("c13_door_open"))),
    Choice(label = label, nextNodeId = "c17_door_barred", requirements = ChoiceRequirement(requiredFlags = setOf("c13_door_barred"))),
    Choice(label = label, nextNodeId = "c17_prepare", requirements = ChoiceRequirement(forbiddenFlags = C13_DOOR_FAMILY))
)

/**
 * The three sites, in the order the tables list them. Each choice marks its site visited as it is taken, and the second table does
 * not offer a site that is already marked, which is what keeps the hub from looping: a site is entered only from a table, the first
 * table is entered once, and the second table only after exactly one site.
 */
private fun sites(secondTable: Boolean): List<Choice> = listOf(
    Choice(
        label = "Visit the stores.",
        nextNodeId = "c17_stores",
        requirements = if (secondTable) ChoiceRequirement(forbiddenFlags = setOf(C17_VISITED_STORES)) else null,
        consequences = Consequences(setFlags = setOf(C17_VISITED_STORES))
    ),
    Choice(
        label = "Speak to the watch captain.",
        nextNodeId = "c17_watch_site",
        requirements = if (secondTable) ChoiceRequirement(forbiddenFlags = setOf(C17_VISITED_WATCH)) else null,
        consequences = Consequences(setFlags = setOf(C17_VISITED_WATCH))
    ),
    Choice(
        label = "Study the old records.",
        nextNodeId = "c17_records",
        requirements = if (secondTable) ChoiceRequirement(forbiddenFlags = setOf(C17_VISITED_RECORDS)) else null,
        consequences = Consequences(setFlags = setOf(C17_VISITED_RECORDS))
    )
)

/**
 * Leaving the hub for the road. The markers stay set: the approach and the payoff scenes read them, and the final "Enter the
 * chamber." choice clears them.
 */
private fun setOut(label: String, requirements: ChoiceRequirement? = null) = Choice(
    label = label,
    nextNodeId = "the_road_to_ashfall_again",
    requirements = requirements
)

private fun departFromTheHub(): List<Choice> = listOf(setOut("Depart for Ashfall."))

/**
 * The way on from a site. It tells a first visit from a second by the other two sites' flags: after the first, the second table;
 * after the second, a single "Set out" (written once for each of the other two sites, because a choice cannot gate on either of two
 * flags). There is no third table, so a third site cannot be reached.
 */
private fun afterASite(a: String, b: String): List<Choice> = listOf(
    Choice(label = "Continue.", nextNodeId = "c17_prepare_second", requirements = ChoiceRequirement(forbiddenFlags = setOf(a, b))),
    setOut("Set out for Ashfall.", ChoiceRequirement(requiredFlags = setOf(a))),
    setOut("Set out for Ashfall.", ChoiceRequirement(requiredFlags = setOf(b)))
)

private fun gate(required: Set<String> = emptySet(), forbidden: Set<String> = emptySet()) =
    ChoiceRequirement(requiredFlags = required, forbiddenFlags = forbidden)

/**
 * The shipped road's single "Get to the chamber." choice, written once for each of Voss's three rungs: she walks beside Kaelen, and
 * what she says depends on where the two of them stand. The lowest rung also covers a save that holds none, so exactly one copy is
 * ever shown. The label, and the absence of any effect, are the shipped ones.
 */
private fun getToTheChamber(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c17_road_trusted", requirements = gate(required = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "c17_road_working", requirements = gate(required = setOf("c7_rung_working"))),
    Choice(label = label, nextNodeId = "c17_road_wary", requirements = gate(forbidden = setOf("c7_rung_working", "c7_rung_trusted")))
)

private fun goOn(nextNodeId: String, requirements: ChoiceRequirement) =
    Choice(label = "Go on.", nextNodeId = nextNodeId, requirements = requirements)

/**
 * The last choice before the guardian, and the only way into it: it clears the five preparation markers, so a finished telling
 * leaves none, and a replay cannot meet a stale one. It is taken only after every payoff has been shown, so nothing is cleared
 * before a payoff gate has been read.
 */
private fun enterTheChamber(requirements: ChoiceRequirement? = null) = Choice(
    label = "Enter the chamber.",
    nextNodeId = "the_chamber_defended",
    requirements = requirements,
    consequences = C17_CLEAR
)

/*
 * The preparation payoffs are static scenes chosen by gated choices, never assembled text. They come in a fixed order: the stores,
 * then the records (the arrangement, or the junctions if Kaelen studied where the lines meet), then the watch (the guard taken, or
 * left), and only the ones that apply. A player who visited nothing gets the one passage for that. Each scene hands on to the next
 * that applies, so every applicable payoff appears exactly once, and the last hands on to the chamber.
 */

/** From the approach, the first payoff that applies: the stores, else the records, else the watch, else the no-preparation passage. */
private fun afterTheApproach(): List<Choice> = listOf(
    goOn("c17_pay_stores", gate(required = setOf(C17_VISITED_STORES))),
    goOn("c17_pay_interruption", gate(required = setOf(C17_STUDIED_JUNCTIONS), forbidden = setOf(C17_VISITED_STORES))),
    goOn(
        "c17_pay_arrangement",
        gate(required = setOf(C17_VISITED_RECORDS), forbidden = setOf(C17_VISITED_STORES, C17_STUDIED_JUNCTIONS))
    ),
    goOn("c17_pay_taken", gate(required = setOf(C17_TOOK_GUARD), forbidden = setOf(C17_VISITED_STORES, C17_VISITED_RECORDS))),
    goOn(
        "c17_pay_left",
        gate(required = setOf(C17_VISITED_WATCH), forbidden = setOf(C17_VISITED_STORES, C17_VISITED_RECORDS, C17_TOOK_GUARD))
    ),
    goOn("c17_pay_none", gate(forbidden = setOf(C17_VISITED_STORES, C17_VISITED_RECORDS, C17_VISITED_WATCH)))
)

/** After the stores: the records, else the watch, else the chamber. */
private fun afterTheStores(): List<Choice> = listOf(
    goOn("c17_pay_interruption", gate(required = setOf(C17_STUDIED_JUNCTIONS))),
    goOn("c17_pay_arrangement", gate(required = setOf(C17_VISITED_RECORDS), forbidden = setOf(C17_STUDIED_JUNCTIONS))),
    goOn("c17_pay_taken", gate(required = setOf(C17_TOOK_GUARD), forbidden = setOf(C17_VISITED_RECORDS))),
    goOn("c17_pay_left", gate(required = setOf(C17_VISITED_WATCH), forbidden = setOf(C17_VISITED_RECORDS, C17_TOOK_GUARD))),
    enterTheChamber(gate(forbidden = setOf(C17_VISITED_RECORDS, C17_VISITED_WATCH)))
)

/** After the records: the watch, else the chamber. */
private fun afterTheRecords(): List<Choice> = listOf(
    goOn("c17_pay_taken", gate(required = setOf(C17_TOOK_GUARD))),
    goOn("c17_pay_left", gate(required = setOf(C17_VISITED_WATCH), forbidden = setOf(C17_TOOK_GUARD))),
    enterTheChamber(gate(forbidden = setOf(C17_VISITED_WATCH)))
)

/**
 * Chapter XVII — What Almost Finished.
 *
 * Where XII's four-setting activation was an accident Ilsevet was still testing the edges of, this
 * chapter is deliberate: with the stolen shards from XVI patched in as substitutes for the two
 * settings she can't take outright, she moves to finish the frame on purpose. Asking the prisoner
 * how much time is left versus simply moving before it finishes speaking now changes the mood of
 * the scramble that follows, not just which stat moves. Kaelen and Voss race to the Sanctum with
 * whatever Stonebeard can spare rather than an army, culminating in a seventeenth and toughest
 * combat encounter yet (a non-humanoid guardian assembled the same desperate way the frame itself
 * was) and a stat-gated choice about *how* to stop the activation — outright destruction versus
 * careful disruption — that deliberately calls back to the prisoner's fear from XIV about being
 * violently unmade rather than freed. Ends having prevented the immediate crisis without resolving
 * the larger one: Ilsevet herself is never seen, and "tonight" turns out not to have been the
 * deadline that mattered.
 */
val chapter17Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_signal",
        chapterId = "chapter_17",
        title = "The Signal",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The prisoner doesn't wait for nightfall this time. "She's not testing anymore," it
            says, mid-afternoon, pressing against the wards hard enough that Kaelen feels it
            through the stone before he even reaches the gate. "Four true settings, whatever she
            took from you, and she's stopped being careful about the difference. She means to
            finish it. Soon." Its urgency alone tells him more than the words do.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"How soon?\"",
                nextNodeId = "gathering_what_they_have_asked",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("asked_how_soon")
                )
            ),
            Choice(
                label = "\"Then we don't wait to find out.\"",
                nextNodeId = "gathering_what_they_have_moved",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("moved_without_waiting")
                )
            )
        )
    ),
    StoryNode(
        id = "gathering_what_they_have_asked",
        chapterId = "chapter_17",
        title = "Gathering What They Have",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            "I don't know exactly," it admits, and something in the admission costs it visibly.
            "Days, not weeks. Maybe less." That uncertainty is its own kind of answer, and Kaelen
            spends the time he does have making sure every hour of it counts rather than wasting
            any of it wondering.

            There isn't time to build an army, and Kaelen doesn't have one to build regardless.
            What he has is Voss, healed enough to hold a blade properly again, whatever
            Stonebeard can spare without leaving its own gate undefended, and word sent to
            Fenmoor that may or may not arrive before this is decided one way or the other.

            It will have to be enough. It's what "enough" has meant since the day the black door
            first opened.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Move out.", nextNodeId = "c17_chain_gate")
        )
    ),
    StoryNode(
        id = "gathering_what_they_have_moved",
        chapterId = "chapter_17",
        title = "Gathering What They Have",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            There's no time spent asking how soon is soon enough. Kaelen is already moving before
            the prisoner finishes the thought, and whatever hours that saves him, he intends to
            spend all of them on the road rather than on questions with no comfortable answer.

            There isn't time to build an army, and Kaelen doesn't have one to build regardless.
            What he has is Voss, healed enough to hold a blade properly again, whatever
            Stonebeard can spare without leaving its own gate undefended, and word sent to
            Fenmoor that may or may not arrive before this is decided one way or the other.

            It will have to be enough. It's what "enough" has meant since the day the black door
            first opened.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Move out.", nextNodeId = "c17_chain_gate")
        )
    ),
    StoryNode(
        id = "c17_chain_gate",
        chapterId = "chapter_17",
        title = "The Chain at the Gate",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The gate is still scarred where the siege reached it. New iron sits beside old stone,
            and the repairs have the uneven look of work done by people who could not afford to wait
            for the mortar to settle.

            Kaelen stops where Halvard made his stand.

            The six-link chain rests against his breastplate. He has carried it through every road
            since the burial, but here its weight feels different. There are people working on the
            wall now who never saw Halvard alive. Others remember exactly where he fell.

            Voss waits beside the stair.

            "They've been asking what happens when you're gone," she says.

            "I gave them an answer."

            "You did. They're finding out what it costs."
        """.trimIndent(),
        choices = stonebeardsAnswer("Continue.")
    ),
    StoryNode(
        id = "c17_wardens",
        chapterId = "chapter_17",
        title = "The New Wardens",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Three recruits are practising at the north gap. Their shields do not quite meet, and the
            youngest keeps stepping forward when the others step back.

            An older guard corrects the line without raising his voice.

            "Again."

            They reset. This time the shields meet.

            Kaelen watches them make the same mistake twice more before they get it right.

            "Halvard would have made them do it another twenty times," Voss says.

            "And complained that they were getting soft."

            She almost smiles.

            The older guard notices the chain at Kaelen's chest and straightens.

            "We're not ready."

            "I know."

            "We'll keep at it."

            Kaelen looks at the three shields, joined properly now.

            "That's what matters."
        """.trimIndent(),
        choices = theDoor("Continue.")
    ),
    StoryNode(
        id = "c17_watch",
        chapterId = "chapter_17",
        title = "The Watch Kept",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The watch roster still bears Kaelen's name.

            Someone has written it in fresh ink beside the north gap, although the previous entry
            has barely dried. A guard stands nearby with a charcoal stub and a question he does not
            quite know how to ask.

            "You're leaving again?"

            "I have to."

            The guard looks towards the wall.

            "Then who holds it?"

            Kaelen has no answer that makes the question smaller.

            He takes the charcoal and writes the name of the senior guard beneath his own.

            "You hold it until I return. And if I don't, you find someone who can stand beside you."

            He hands the charcoal back.

            "Don't wait for another Halvard."
        """.trimIndent(),
        choices = theDoor("Continue.")
    ),
    StoryNode(
        id = "c17_council",
        chapterId = "chapter_17",
        title = "The Question Left Open",
        illustrationId = "who_stands_with_stonebeard",
        narrativeText = """
            The smiths have cleared a table in the gatehouse. Around it stand guards, stonecutters
            and two people who have never carried a weapon in their lives.

            They are arguing about watch rotations.

            It is not a comfortable argument. Nobody agrees on how much the Hold can spare, who
            should decide, or whether the next threat will come from the road or from within the
            mountain.

            But they are arguing together.

            One of the smiths sees Kaelen in the doorway.

            "We haven't settled it."

            "You don't need my permission."

            "We thought you'd want the final word."

            Kaelen looks at the empty place at the head of the table.

            "Halvard would have told you what he thought. Then he'd have made you decide for
            yourselves."

            The smith snorts.

            "He'd have told us what he thought for an hour."

            "At least."

            For a moment the gatehouse sounds like somewhere people might live, rather than
            somewhere they are waiting to die.
        """.trimIndent(),
        choices = theDoor("Continue.")
    ),
    StoryNode(
        id = "c17_door_open",
        chapterId = "chapter_17",
        title = "Those Who Came Inside",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The stair door stands open while the repair crews carry stone through the passage.

            A family from the road has joined them. One of the children has been given the important
            task of counting nails and is taking it seriously enough to count each one twice.

            "There were complaints," the guard tells Kaelen. "Still are."

            "And?"

            "We've got more hands helping with the repairs than we had yesterday."

            The child looks up from the basket.

            "Six," she says. "There's six."

            The guard checks, then nods.

            "Six, then."

            Kaelen looks towards the open passage. Welcoming people has brought new work and new
            arguments, but it has also brought people willing to share the work already waiting.
        """.trimIndent(),
        choices = listOf(Choice(label = "Continue.", nextNodeId = "c17_prepare"))
    ),
    StoryNode(
        id = "c17_door_barred",
        chapterId = "chapter_17",
        title = "The Help Beyond the Gate",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The stair door is barred, just as Kaelen ordered.

            Outside the gatehouse, a guard is preparing a small parcel of provisions for the road. A
            messenger has been arranged to carry it to the families waiting beyond the Hold.

            "Edda asked whether we'd reconsider," the guard says.

            "What did you tell her?"

            "That the order still stands."

            He tightens the cord around the parcel.

            "She wasn't pleased."

            "I didn't expect her to be."

            "She said keeping a door shut doesn't settle what we owe the people outside it."

            Kaelen looks at the iron bar across the passage.

            He has not forgotten why he gave the order. Edda has not forgotten whom it affects. Both
            will have to live with the answer until someone has reason to change it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Continue.", nextNodeId = "c17_prepare"))
    ),
    StoryNode(
        id = "c17_prepare",
        chapterId = "chapter_17",
        title = "What Stonebeard Can Spare",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            The prisoner has warned them that Ilsevet means to finish the frame soon. Stonebeard can
            offer supplies, a handful of willing people and the kind of advice that only becomes
            useful after something has already gone wrong.

            Kaelen cannot take everything.

            Voss spreads a rough map across the gatehouse table.

            "We need to choose what matters before we reach Ashfall."

            "And leave the rest?"

            "There will always be a rest."

            She marks three places with the tip of her knife.

            "The stores. The watch. The old records."

            Kaelen studies the marks. There is time for two, if they move quickly.
        """.trimIndent(),
        choices = sites(secondTable = false) + departFromTheHub()
    ),
    StoryNode(
        id = "c17_prepare_second",
        chapterId = "chapter_17",
        title = "Time for One More",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            Voss turns the rough map back towards Kaelen. One mark has been dealt with. Two are
            still open.

            "One more," she says. "If we're quick."

            Kaelen studies what is left. Whatever he does not choose here, Stonebeard will have to
            keep.
        """.trimIndent(),
        choices = sites(secondTable = true) + departFromTheHub()
    ),
    StoryNode(
        id = "c17_stores",
        chapterId = "chapter_17",
        title = "The Quartermaster's Promise",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            The quartermaster has already laid out what can be spared: bandages, dried provisions, a
            coil of rope and a small flask of clean water.

            "You can take this," she says. "The rest stays for the people who'll be here when you're
            gone."

            Kaelen lifts the lighter pack.

            "Halvard used to argue with you about the stores."

            "He argued with me about everything."

            She pauses.

            "He always brought back what he borrowed."

            Kaelen looks at the space beside the shelves where a battered hammer once rested while
            its owner counted provisions.

            "I'll bring back what I can."

            "See that you do."

            She closes the store ledger before either of them has to say anything else.
        """.trimIndent(),
        choices = afterASite(C17_VISITED_WATCH, C17_VISITED_RECORDS)
    ),
    StoryNode(
        id = "c17_watch_site",
        chapterId = "chapter_17",
        title = "The North Gap",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The north gap is quieter than Kaelen remembers. The repairs have narrowed the opening,
            and the guards have learned to work around the broken stone rather than pretend it isn't
            there.

            The watch captain meets him beside the replacement braces.

            "I can spare one experienced guard for Ashfall," she says. "Only one. And I'd rather you
            didn't ask."

            "Then why offer?"

            "Because Halvard would have asked what happened if you went without one."

            She glances towards the men on the wall.

            "And he'd have made me answer."

            Kaelen studies the line. One more person beside him on the road means one fewer person
            standing here.

            He must decide where that risk belongs.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take one guard to watch the approach.",
                nextNodeId = "c17_watch_take",
                consequences = Consequences(setFlags = setOf(C17_TOOK_GUARD))
            ),
            Choice(label = "Leave every defender at Stonebeard.", nextNodeId = "c17_watch_leave")
        )
    ),
    StoryNode(
        id = "c17_watch_take",
        chapterId = "chapter_17",
        title = "One for the Road",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The captain chooses a guard whose shield is scratched almost white along one edge.

            "You've been to Ashfall?" Kaelen asks.

            "Once. Before the spire started shining."

            "You know what we're walking towards?"

            "Enough to know I might not come back."

            Kaelen looks towards the north gap.

            "Stonebeard needs you too."

            "Stonebeard needs you to stop what's coming."

            The guard checks the fastening on his shield and falls into step beside Voss.

            Behind them, the captain begins rearranging the watch. The cost of the decision is
            already being counted.
        """.trimIndent(),
        choices = afterASite(C17_VISITED_STORES, C17_VISITED_RECORDS)
    ),
    StoryNode(
        id = "c17_watch_leave",
        chapterId = "chapter_17",
        title = "The Wall Before the Road",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            "Keep your guard," Kaelen says.

            The captain looks surprised.

            "You're certain?"

            "If the frame fails to stop, you'll need every person you have. If we succeed, you'll
            still have a gate to defend."

            She considers that, then offers her hand.

            "Come back and tell us which it was."

            Kaelen takes it.

            On the way down from the wall, Voss glances back towards the gap.

            "One fewer blade for us."

            "One more for them."

            She nods. Neither answer makes the road safer.
        """.trimIndent(),
        choices = afterASite(C17_VISITED_STORES, C17_VISITED_RECORDS)
    ),
    StoryNode(
        id = "c17_records",
        chapterId = "chapter_17",
        title = "The Six Settings",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            The old ward records have been moved away from the damaged gatehouse wall. Most are
            copies, made by people who knew the originals might not survive another siege.

            A clerk lays out the diagrams of the six wards.

            "These aren't Ilsevet's plans," she warns. "They're older records of how the wards were
            meant to hold."

            Four positions are marked with the same sign. The remaining two have different
            annotations, written by several hands over generations.

            "We've been trying to understand what she could force into those places," the clerk
            says.

            Kaelen studies the page.

            The records cannot tell him exactly what Ilsevet has built. They may still tell him
            where the old design was weakest.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Study the six-position arrangement.", nextNodeId = "c17_records_study"),
            Choice(
                label = "Look for ways to interrupt the connections.",
                nextNodeId = "c17_records_interrupt",
                consequences = Consequences(setFlags = setOf(C17_STUDIED_JUNCTIONS))
            )
        )
    ),
    StoryNode(
        id = "c17_records_study",
        chapterId = "chapter_17",
        title = "What the Wards Remember",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            Kaelen follows the old diagram from its first marked position to its last.

            "These were meant to hold together," he says.

            "That's what the records suggest," the clerk replies. "But the notes disagree about what
            they were holding."

            Voss leans closer.

            "A door?"

            "Perhaps. Or something on the other side of it."

            Kaelen copies the positions onto a spare sheet. The order matters more than the marks
            beside each position. If the chamber follows the same arrangement, he will at least
            recognise what he is looking at.

            It is a small advantage against a thing nobody in the room understands.
        """.trimIndent(),
        choices = afterASite(C17_VISITED_STORES, C17_VISITED_WATCH)
    ),
    StoryNode(
        id = "c17_records_interrupt",
        chapterId = "chapter_17",
        title = "Where the Pattern Breaks",
        illustrationId = "gathering_what_they_have",
        narrativeText = """
            Kaelen ignores the marks beside the positions and looks instead at the places where the
            lines meet.

            "If one setting fails, what happens to the others?"

            The clerk studies the page.

            "The records aren't clear."

            "What do they agree on?"

            She turns a second sheet towards him.

            "That the connection matters. Whatever passes between them was never meant to be
            interrupted while the arrangement was under strain."

            Voss studies the marked junctions.

            "So breaking the connection could stop it."

            "Or make the strain go somewhere else," the clerk answers.

            Kaelen copies the junctions. There is no promise of a safe answer, only a clearer
            picture of the danger.
        """.trimIndent(),
        choices = afterASite(C17_VISITED_STORES, C17_VISITED_WATCH)
    ),
    StoryNode(
        id = "the_road_to_ashfall_again",
        chapterId = "chapter_17",
        title = "The Road to Ashfall, Again",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The Ashfall reaches have changed since the last two times Kaelen crossed them. The
            Sanctum's spire doesn't just hum now; it's visible for a day's travel in every
            direction, a wrongness on the horizon that doesn't need darkness to be seen.

            Whatever's left of Ilsevet's garrison isn't hiding it anymore. There's nothing left to
            hide it from, and no one left who'd believe the lie if they tried.
        """.trimIndent(),
        choices = getToTheChamber("Get to the chamber.")
    ),
    StoryNode(
        id = "c17_road_trusted",
        chapterId = "chapter_17",
        title = "What We Carry",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            They have been walking for an hour when Voss speaks.

            "You keep touching the chain."

            Kaelen lowers his hand.

            "I didn't know I was."

            "I did."

            She lets the silence settle before continuing.

            "Halvard chose where to stand. You didn't choose what happened to him."

            "I chose enough."

            "So did he."

            Kaelen looks down at the six links.

            "I keep thinking about what he would have done differently."

            "He'd have told you. At length."

            That earns a brief smile.

            Voss walks beside him without trying to take the weight away.
        """.trimIndent(),
        choices = listOf(Choice(label = "Continue.", nextNodeId = "c17_approach"))
    ),
    StoryNode(
        id = "c17_road_working",
        chapterId = "chapter_17",
        title = "What We Carry",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            Voss notices the chain when Kaelen checks the fastening on his bracer.

            "Does it help?"

            "Sometimes."

            "And the rest of the time?"

            "It reminds me."

            She considers that.

            "Then keep it where you can reach it."

            They walk a little farther.

            "Halvard trusted you to carry it," she adds.

            "He didn't have much time to reconsider."

            "He knew what he was doing."

            Kaelen looks towards the road ahead. Neither of them says what the chain reminds him of.
        """.trimIndent(),
        choices = listOf(Choice(label = "Continue.", nextNodeId = "c17_approach"))
    ),
    StoryNode(
        id = "c17_road_wary",
        chapterId = "chapter_17",
        title = "What We Carry",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            Voss looks at the chain once, then back to the road.

            "He knew the gate wouldn't hold without him."

            "He knew."

            "I don't think he expected you to carry the answer afterward."

            Kaelen says nothing.

            A little later, she checks the road behind them and signals that it is clear.

            They continue towards Ashfall with a careful distance between them, close enough to
            fight together, not yet close enough to speak freely.

            Whatever else remains unsettled, Kaelen knows she will warn him if danger comes from
            behind.
        """.trimIndent(),
        choices = listOf(Choice(label = "Continue.", nextNodeId = "c17_approach"))
    ),
    StoryNode(
        id = "c17_approach",
        chapterId = "chapter_17",
        title = "The Last Mile",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            By the time they see the Sanctum clearly, the light above it has stopped resembling
            anything made by fire.

            It holds steady against the clouds, pale and cold, while the stones beneath Kaelen's
            boots give a faint answering tremor.

            Voss studies the approaches.

            "There's no sign of a full garrison."

            "Ilsevet doesn't need one if the frame finishes."

            The road narrows between broken walls. Beyond them stands the chamber entrance.

            Kaelen thinks of the gate he left behind, the people still working on its repairs, and
            the six links resting against his armour.

            Stonebeard cannot afford another Halvard.

            Neither can anywhere else.

            He draws his sword.

            "We stop it here."
        """.trimIndent(),
        choices = afterTheApproach()
    ),
    StoryNode(
        id = "c17_pay_stores",
        chapterId = "chapter_17",
        title = "The Quartermaster's Pack",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            The pack from Stonebeard pulls against Kaelen's shoulder. He stops long enough to check
            the bandages and the small flask the quartermaster allowed him to take.

            Voss watches him tighten the strap.

            "You could have asked for more."

            "The people behind us need supplies too."

            "That's beginning to sound like Halvard."

            Kaelen fastens the pack without answering.
        """.trimIndent(),
        choices = afterTheStores()
    ),
    StoryNode(
        id = "c17_pay_arrangement",
        chapterId = "chapter_17",
        title = "The Marked Threshold",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            At the chamber threshold, Kaelen unfolds the copied ward diagram. Six positions are
            marked around its edge, like the numbers on a clock.

            He looks from the page to the worn carvings beside the entrance. Their spacing is
            familiar, though the door hides whatever lies beyond.

            "The records were right about the arrangement," he says.

            Voss studies the marks in the stone.

            "Were they right about what it does?"

            "They didn't agree."

            Kaelen folds the page again. Knowing the shape of a danger is not the same as knowing
            how to stop it.
        """.trimIndent(),
        choices = afterTheRecords()
    ),
    StoryNode(
        id = "c17_pay_interruption",
        chapterId = "chapter_17",
        title = "Where the Lines Meet",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Kaelen holds the copied diagram against the light. The old drawing marks where the ward
            lines meet, but the chamber door keeps the workings beyond it hidden.

            "There," he says, pointing to a junction on the page.

            Voss follows his finger.

            "A weakness?"

            "A connection. Breaking it might stop the frame."

            "Might?"

            "The records couldn't tell us where the strain would go."

            Voss looks towards the closed entrance.

            "Then we'd better be certain before we strike."
        """.trimIndent(),
        choices = afterTheRecords()
    ),
    StoryNode(
        id = "c17_pay_taken",
        chapterId = "chapter_17",
        title = "A Shield on the Road",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            The guard from Stonebeard crouches beside a broken wall, studying the entrance.

            "The eastern stones have shifted," he says. "There's a narrow way through, but I
            wouldn't trust it if the ground starts moving."

            Kaelen studies the gap.

            "Stay outside when we enter. Watch the road and warn anyone coming after us."

            The guard looks disappointed, then glances back towards the distant light.

            "Understood."

            He takes his position with his shield raised.

            Kaelen knows the Hold is one defender poorer because of that shield.
        """.trimIndent(),
        choices = listOf(enterTheChamber())
    ),
    StoryNode(
        id = "c17_pay_left",
        chapterId = "chapter_17",
        title = "The Shield Left Behind",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Kaelen studies the broken approach. An additional shield would have been useful here.

            Voss follows his gaze.

            "Thinking about the guard?"

            "I'm thinking about the gap in Stonebeard's wall."

            "You made your choice."

            "I know."

            They move forward without another word. Somewhere behind them, the watch remains at its
            post.
        """.trimIndent(),
        choices = listOf(enterTheChamber())
    ),
    StoryNode(
        id = "c17_pay_none",
        chapterId = "chapter_17",
        title = "No Time to Prepare",
        illustrationId = "ashfall_sanctum_vista",
        narrativeText = """
            Stonebeard had offered what little it could spare. Kaelen had chosen to leave before the
            preparations cost them more time.

            Now the Sanctum stands before him, and there is no opportunity to go back for a map,
            another shield or an extra bandage.

            Voss draws the borrowed blade.

            "Ready?"

            "As ready as we're going to be."
        """.trimIndent(),
        choices = listOf(enterTheChamber())
    ),
    StoryNode(
        id = "the_chamber_defended",
        chapterId = "chapter_17",
        title = "The Chamber, Defended",
        illustrationId = "the_chamber_defended",
        narrativeText = """
            Whatever guards the frame chamber now isn't cinder-grey soldiers: Ilsevet doesn't
            have enough of those left to spare, not after Fenmoor, not after the reprisal squad,
            not after the reliquary theft. What she's put in front of the door is something built
            rather than born, assembled the same desperate way the frame itself was, and it
            doesn't so much fight as insist.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "chamber_guardian_encounter"
    ),
    StoryNode(
        id = "chamber_guardian_defeat",
        chapterId = "chapter_17",
        title = "It Runs Out of Purpose Before He Runs Out of Time",
        illustrationId = "the_chamber_defended",
        narrativeText = """
            Whatever built this thing didn't build it to improvise, and Kaelen learns that only
            after it's already put him on the ground twice, each hit landing with the same flat,
            mechanical certainty as the last. There's no adjusting to it. There's only surviving
            long enough for something else to go wrong first.

            Something does. It seizes mid-motion, whatever half-finished purpose was driving it
            finally outrunning whatever was holding it together, and goes still the way a machine
            stops rather than the way a creature dies. Kaelen doesn't feel like he won. He'll take
            it anyway — the chamber's gone quiet, and that's the only clock that matters right now.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get to the frame.",
                nextNodeId = "what_must_be_broken",
                consequences = Consequences(setFlags = setOf("lost_to_sanctum_construct"))
            )
        )
    ),
    StoryNode(
        id = "what_must_be_broken",
        chapterId = "chapter_17",
        title = "What Must Be Broken",
        illustrationId = "what_must_be_broken",
        narrativeText = """
            The frame itself is worse up close than the vision back at Stonebeard's own gate ever
            managed to convey: six settings, four of them dark and humming with something that
            isn't light, two of them fitted with fragments too broken to properly belong. It
            isn't finished. It's close enough to almost not matter.

            Kaelen has exactly as long as it takes Ilsevet's remaining people to notice the
            chamber's gone quiet to decide what "stopping this" actually means, and not one
            second longer.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Tear it apart. Whatever the pieces do loose is better than what six settings does whole.\"",
                nextNodeId = "chapter17_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 15)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("destroyed_the_frame_outright")
                )
            ),
            Choice(
                label = "\"Disrupt the settings, don't destroy them. The prisoner was afraid of being unmade — I won't be the one who does it by accident.\"",
                nextNodeId = "chapter17_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 14)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("disrupted_carefully")
                )
            ),
            Choice(
                label = "Do whatever stops it fastest, and worry about the difference later.",
                nextNodeId = "chapter17_end",
                consequences = Consequences(
                    grantItemIds = listOf("twisted_frame_component"),
                    setFlags = setOf("stopped_it_however_possible")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter17_end",
        chapterId = "chapter_17",
        title = "End of Chapter XVII — What Almost Finished",
        illustrationId = "chapter17_threshold",
        narrativeText = """
            Whatever Kaelen does to the frame, it stops being a threat tonight: Ilsevet's voice,
            somewhere beyond the chamber, goes from giving orders to something closer to silence
            once the humming cuts out. He doesn't see her. He isn't sure, afterward, whether that
            was mercy or just bad timing.

            They make it out with Voss's cut arm reopened and nothing else worse than that, which
            counts as the best outcome any of Ilsevet's plans has produced yet. The frame won't
            finish tonight. Kaelen has a growing, specific fear that "tonight" was never really
            the deadline that mattered.

            Chapter XVIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go home.", nextNodeId = "no_more_envoys")
        )
    )
)

val chapter17 = com.thelastjailer.app.Chapter(
    id = "chapter_17",
    number = 17,
    title = "Chapter XVII — What Almost Finished",
    startNodeId = "the_signal"
)
