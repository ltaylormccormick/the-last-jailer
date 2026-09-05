package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter XIV — Furniture.
 *
 * Returns to the thread dormant since III/V: the pressure behind Kaelen's own gate, which XIII
 * ended with speaking again for the first time in many chapters. Where its two prior offers
 * (IV/V/VIII's "dark aid" temptation) were about power, this chapter reframes it as something more
 * complicated — the prisoner is afraid of what a completed seventh door would do to it specifically
 * (dissolve six distinct entities into one that remembers being none of them), which makes it, for
 * the first time, aligned with Kaelen against Ilsevet for reasons that have nothing to do with him.
 * Asking it outright versus staying silent and letting it volunteer the answer now changes how
 * readily it opens up, not just which stat moves. A vision it offers manifests as this chapter's
 * combat: a symbolic, non-physical confrontation with a memory-construct of whatever severed the
 * six wards from something closer to a choice, three centuries ago — new worldbuilding rather than
 * a resolved mystery, deliberately left open. Fourteenth and toughest combat encounter yet (The
 * Memory Itself, 190 HP). Ends with the prisoner's stance toward Kaelen shifted from
 * adversary/tempter to something closer to a wary ally.
 */
val chapter14Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "what_waits_speaks",
        chapterId = "chapter_14",
        title = "What Waits Speaks",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            It doesn't stop at one sentence. Night after night, the pressure behind the black
            door keeps finding new ways to press against the wards, not testing them the way it
            used to, not asking for anything. Just talking, the way something talks when it's
            finally decided silence isn't working for it anymore.

            "Four settings," it says again, the third night running. "You felt it too. I know
            you did. Ask me what happens at six."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"What happens at six?\"",
                nextNodeId = "what_it_says_asked",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("asked_the_prisoner_directly")
                )
            ),
            Choice(
                label = "Say nothing. Listen instead.",
                nextNodeId = "what_it_says_listened",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("listened_without_asking")
                )
            )
        )
    ),
    StoryNode(
        id = "what_it_says_asked",
        chapterId = "chapter_14",
        title = "What It Says",
        illustrationId = "what_it_says",
        narrativeText = """
            "Since you're asking plainly, I'll answer plainly." Something in the pressure behind
            the wards eases, fractionally, at being asked outright rather than made to volunteer
            it. "At six, whatever she's building doesn't stay a cage. It becomes a door with no
            other side, six of us, poured into one shape, and whatever walks out afterward won't
            remember being any of us individually. I have been many things in three centuries
            behind this stone. I would rather not become someone else's furniture."

            It has never, in all the times it has spoken to him, sounded uncertain before.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press further.", nextNodeId = "the_names_it_wont_give")
        )
    ),
    StoryNode(
        id = "what_it_says_listened",
        chapterId = "chapter_14",
        title = "What It Says",
        illustrationId = "what_it_says",
        narrativeText = """
            The silence stretches long enough that Kaelen almost thinks it's satisfied with
            having said nothing more, and then it keeps talking anyway, as if his quiet gave it
            more room rather than less. "At six, whatever she's building doesn't stay a cage. It
            becomes a door with no other side, six of us, poured into one shape, and whatever
            walks out afterward won't remember being any of us individually. I have been many
            things in three centuries behind this stone. I would rather not become someone
            else's furniture."

            It has never, in all the times it has spoken to him, sounded uncertain before, or
            said so much unasked.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Press further.", nextNodeId = "the_names_it_wont_give")
        )
    ),
    StoryNode(
        id = "the_names_it_wont_give",
        chapterId = "chapter_14",
        title = "The Names It Won't Give",
        illustrationId = "the_names_it_wont_give",
        narrativeText = """
            "What are you?" Kaelen asks, the same question he's never quite gotten a straight
            answer to since the black door first opened. "Who were you, before this."

            A long pause, long enough that Kaelen wonders if it's decided silence is working for
            it again after all. "Ask me that again," it says finally, "after there are fewer of
            us left to lose. I'll answer honestly. I promise you that much costs me nothing."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "\"Show me, then.\"", nextNodeId = "the_offered_glimpse")
        )
    ),
    StoryNode(
        id = "the_offered_glimpse",
        chapterId = "chapter_14",
        title = "The Offered Glimpse",
        illustrationId = "the_offered_glimpse",
        narrativeText = """
            "Press your hand to the stone," it says. "I'll show you instead of telling you. It
            will cost you something. It always does."

            Kaelen has done this before, once, at the binding rite, and once more than he'd like
            to admit since. He presses his palm flat against the black iron anyway, bracing for
            the ache before it arrives this time.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let it in.", nextNodeId = "inside_the_memory")
        )
    ),
    StoryNode(
        id = "inside_the_memory",
        chapterId = "chapter_14",
        title = "Inside the Memory",
        illustrationId = "inside_the_memory",
        narrativeText = """
            The vision doesn't open gently. It opens as a fight, some fragment of memory shaped
            like the moment three centuries ago when the six wards were first cut apart from
            whatever they used to be whole, replaying itself with Kaelen standing directly in the
            middle of it. Whatever this used to be, it doesn't want to be remembered by force,
            and it fights the memory of him the way it must once have fought whatever severed it.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "memory_confrontation_encounter"
    ),
    StoryNode(
        id = "what_kaelen_remembers",
        chapterId = "chapter_14",
        title = "What Kaelen Remembers",
        illustrationId = "what_kaelen_remembers",
        narrativeText = """
            He comes back to himself on his knees in front of the gate, palm bleeding faintly
            where the iron bit into it, the vision already fraying at the edges the way dreams
            do. He remembers enough. Six wards, once something closer to a choice than a cage.
            Something took that choice away from all six of them at once, a very long time before
            the Ashen Order ever existed to guard the results.

            The prisoner doesn't say anything else that night. It doesn't need to, and for once,
            neither does Kaelen.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Tell Voss everything, tonight, whatever it costs to say out loud.",
                nextNodeId = "chapter14_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 12)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("shard_of_the_first_ward"),
                    setFlags = setOf("told_voss_everything")
                )
            ),
            Choice(
                label = "Write it down instead. Some truths need to be chosen, not sprung on someone.",
                nextNodeId = "chapter14_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 11)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("shard_of_the_first_ward"),
                    setFlags = setOf("recorded_the_vision_first")
                )
            ),
            Choice(
                label = "Sit with it alone a while longer.",
                nextNodeId = "chapter14_end",
                consequences = Consequences(
                    grantItemIds = listOf("shard_of_the_first_ward"),
                    setFlags = setOf("kept_the_vision_to_himself")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter14_end",
        chapterId = "chapter_14",
        title = "End of Chapter XIV — Furniture",
        illustrationId = "chapter14_threshold",
        narrativeText = """
            Whatever Kaelen decides to do with what he's seen, one thing is no longer abstract:
            the thing behind his own door isn't just a threat to be managed or a bargain to be
            refused. It's afraid, in whatever way three-hundred-year-old pressure gets to be
            afraid, and it has started treating Kaelen less like a jailer and more like the only
            ally it has left outside its own stone.

            That should worry him more than it does.

            Chapter XV awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "what_selvane_sends")
        )
    )
)

val chapter14 = com.thelastjailer.app.Chapter(
    id = "chapter_14",
    number = 14,
    title = "Chapter XIV — Furniture",
    startNodeId = "what_waits_speaks"
)
