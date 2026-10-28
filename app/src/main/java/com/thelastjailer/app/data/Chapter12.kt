package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C12a: whether Kaelen went back to Ashfall for Ilsevet, and the way he met her there. The two calls and the three ways are
 * alternatives, and each call clears all five, so a replay that answers differently never carries the earlier answer. Chapter XV
 * tells Ashfall again by this family.
 */
val C12_ILSEVET_FAMILY: Set<String> = setOf(
    "chose_to_help_ilsevet", "refused_to_help_ilsevet", "warned_ilsevet_directly", "appealed_to_ilsevets_conscience",
    "left_ilsevet_to_her_wreckage"
)

/** Family C12b: the three ways Kaelen met Ilsevet in the wreck. Each way clears the other two and nothing else. */
val C12_WAY_FAMILY: Set<String> = setOf("warned_ilsevet_directly", "appealed_to_ilsevets_conscience", "left_ilsevet_to_her_wreckage")

/** From the ash-field gate to Voss's word at the last dry stone, by where she and Kaelen stand. The lowest rung also covers a save with none. */
private fun vossGoes(label: String): List<Choice> = listOf(
    Choice(label = label, nextNodeId = "c12_voss_trusted", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_trusted"))),
    Choice(label = label, nextNodeId = "c12_voss_working", requirements = ChoiceRequirement(requiredFlags = setOf("c7_rung_working"))),
    Choice(
        label = label,
        nextNodeId = "c12_voss_wary",
        requirements = ChoiceRequirement(forbiddenFlags = setOf("c7_rung_working", "c7_rung_trusted"))
    )
)

/**
 * From Voss's word into the Sanctum: by the way Kaelen went in the last time, or straight to the frame chamber. The east stair
 * is offered only where Voss trusts him ([stair]): it was open to Kaelen only because she had told him where the cells were,
 * which a Voss on a lower rung at the capture never did, and the ladder does not fall after it. Where it is not offered, a save
 * that somehow holds that flag still goes straight to the frame chamber.
 */
private fun wayChoices(label: String, stair: Boolean): List<Choice> = (if (stair) listOf(
    Choice(label = label, nextNodeId = "c12_way_stair", requirements = ChoiceRequirement(requiredFlags = setOf("c9_way_stair")))
) else emptyList()) + listOf(
    Choice(label = label, nextNodeId = "c12_way_wall", requirements = ChoiceRequirement(requiredFlags = setOf("c9_way_wall"))),
    Choice(label = label, nextNodeId = "c12_way_cart", requirements = ChoiceRequirement(requiredFlags = setOf("c9_way_cart"))),
    Choice(
        label = label,
        nextNodeId = "what_the_frame_did",
        requirements = ChoiceRequirement(
            forbiddenFlags = setOf("c9_way_wall", "c9_way_cart") + (if (stair) setOf("c9_way_stair") else emptySet())
        )
    )
)

/**
 * Chapter XII — Four Settings.
 *
 * Ilsevet, denied both remaining gates in Chapter XI, tries activating the seventh-door frame with
 * only the four settings she already has — and it nearly destroys the Sanctum. Word reaches
 * Stonebeard, and how urgently Kaelen reacts to that word (already moving before the messenger's
 * even caught his breath, versus making the man sit and recover before hearing him out) now colors
 * the lead-up to "the_call_to_go", where the player makes a genuine, unforced choice: go help
 * contain it (which puts Kaelen in the position of saving the woman who has tried twice to take
 * the brand by force), or refuse and let her face the consequences of her own gamble alone. The two
 * branches diverge for the rest of the chapter and rejoin only at the chapter boundary (Chapter
 * XIII's start), each with its own closing node, its own flag, and different rewards — helping is
 * the only route to the twelfth combat encounter (The Unfinished, 175 HP, a non-humanoid threat
 * echoing VI's tonal shift), the item it drops, and the stat-gated aftermath choice with Ilsevet;
 * refusing skips all three and resolves the same "four settings nearly cost everything" beat
 * secondhand, by report rather than by presence. Both paths still deliberately subvert the "siege"
 * pattern of III/IV/V/VII/IX/X: Ilsevet isn't attacking this time, she's overwhelmed, and whichever
 * way the player goes, both Kaelen and Ilsevet come out the other side knowing exactly what a
 * completed seventh door would do.
 *
 * On the way into the ruined Sanctum, Voss says what going back costs her, according to where she and Kaelen stand, and a
 * Kaelen who went in by the east stair, the south wall or the tithe road in Chapter IX meets that way again. The shipped
 * scenes and both endings, with their routing on trusted_voss_fully, are untouched; only where the single choice at the
 * ash-field gate leads has changed.
 */
val chapter12Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "word_from_ashfall",
        chapterId = "chapter_12",
        title = "Word from Ashfall",
        illustrationId = "word_from_ashfall",
        narrativeText = """
            It's one of Voss's old Order contacts who brings the word, riding hard enough to
            nearly founder his horse at Stonebeard's gate: the kind of messenger who doesn't
            dismount so much as fall off deliberately. "The Sanctum," he manages, between
            breaths. "Something's gone wrong there. Badly wrong. Half the ash-field's on fire
            that shouldn't burn, and whatever she built isn't staying where she put it."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then we go.\"",
                nextNodeId = "the_call_to_go_eager",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("answered_the_word_at_once")
                )
            ),
            Choice(
                label = "\"Tell me everything you know first.\"",
                nextNodeId = "the_call_to_go_thorough",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("asked_before_answering")
                )
            )
        )
    ),
    StoryNode(
        id = "the_call_to_go_eager",
        chapterId = "chapter_12",
        title = "The Call to Go",
        illustrationId = "the_call_to_go",
        narrativeText = """
            Kaelen is already reaching for his sword before the messenger finishes falling off
            his horse, and Voss has to half-jog to keep pace with him toward the tunnel. "You
            might want to hear the rest before you commit to a direction," she says, not quite
            breathless yet, but getting there.

            "She tried to kill you twice. Take your brand by force, once by knife and once by
            siege. And now something she built is loose and she can't put it back." She lets
            that sit a moment before she says the rest, even as they keep walking. "I won't tell
            you which way to go on this one."

            Kaelen isn't sure himself, not yet, Halvard's voice and the shape of every enemy
            who's come through that gate arguing for opposite answers at the same time. He just
            knows he can't stand still while he decides.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go. Whatever else she's done, this isn't a door that should open by accident.",
                nextNodeId = "back_to_the_ash",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C12_ILSEVET_FAMILY,
                    setFlags = setOf("chose_to_help_ilsevet")
                )
            ),
            Choice(
                label = "Let her live with what she built. Warn Fenmoor, and wait.",
                nextNodeId = "watched_from_a_distance",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C12_ILSEVET_FAMILY,
                    setFlags = setOf("refused_to_help_ilsevet")
                )
            )
        )
    ),
    StoryNode(
        id = "the_call_to_go_thorough",
        chapterId = "chapter_12",
        title = "The Call to Go",
        illustrationId = "the_call_to_go",
        narrativeText = """
            Kaelen makes the messenger sit, drink something, breathe properly before he asks
            anything else, and only once the man's hands have stopped shaking does he let Voss
            pick the account back up.

            "She tried to kill you twice. Take your brand by force, once by knife and once by
            siege. And now something she built is loose and she can't put it back." She lets
            that sit a moment before she says the rest. "I won't tell you which way to go on
            this one."

            Kaelen isn't sure himself, not yet, Halvard's voice and the shape of every enemy
            who's come through that gate arguing for opposite answers at the same time. Taking
            the time to ask hasn't made the answer any clearer. It's only made him more certain
            he owes himself the asking.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go. Whatever else she's done, this isn't a door that should open by accident.",
                nextNodeId = "back_to_the_ash",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = C12_ILSEVET_FAMILY,
                    setFlags = setOf("chose_to_help_ilsevet")
                )
            ),
            Choice(
                label = "Let her live with what she built. Warn Fenmoor, and wait.",
                nextNodeId = "watched_from_a_distance",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = C12_ILSEVET_FAMILY,
                    setFlags = setOf("refused_to_help_ilsevet")
                )
            )
        )
    ),
    StoryNode(
        id = "back_to_the_ash",
        chapterId = "chapter_12",
        title = "Back to the Ash",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The Ashfall reaches look different at speed and under smoke than they did the last
            two times Kaelen crossed them: the spire that used to hum is visible from twice the
            distance now, wrong light bleeding out of cracks that shouldn't exist in stone that
            old.

            Whatever's loose, it hasn't left the Sanctum's walls yet. That's the only mercy on
            offer, and neither of them slows down to be grateful for it.
        """.trimIndent(),
        choices = vossGoes("Go in.")
    ),
    StoryNode(
        id = "c12_voss_wary",
        chapterId = "chapter_12",
        title = "The Last Dry Stone",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The ash-field is quieter than Kaelen remembers, the way a bell is quiet
            after it has been struck. Voss stops at the last dry stone before the
            Sanctum's outer wall and does not go on.

            "I was in that wing a season ago," she says, to the wall. "I intend to enter
            by a different door." She looks, at last, at him. "I am going in because I
            know the corridors and nobody else here does. That is the reason. Don't ask
            me for the others." Her hand rests on the borrowed blade, not gripping it.
            "I won't be slower than you."
        """.trimIndent(),
        choices = wayChoices("Go in.", stair = false)
    ),
    StoryNode(
        id = "c12_voss_working",
        chapterId = "chapter_12",
        title = "The Last Dry Stone",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The ash-field is quieter than Kaelen remembers, the way a bell is quiet
            after it has been struck. Voss stops at the last dry stone before the
            Sanctum's outer wall, and does not go on until he has drawn level.

            "I said I would not tell you which way to go," she says. "I will tell you
            which way I am going. In." She nods at the wall. "I know where the corridors
            run. I have been dragged down most of them." She says it dryly, the way you
            say a thing so that nobody can say it for you. "If the woman who built this
            is out of options, I would sooner be near her with a blade than far from her
            with an opinion."
        """.trimIndent(),
        choices = wayChoices("Go in.", stair = false)
    ),
    StoryNode(
        id = "c12_voss_trusted",
        chapterId = "chapter_12",
        title = "The Last Dry Stone",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The ash-field is quieter than Kaelen remembers, the way a bell is quiet
            after it has been struck. Voss stops at the last dry stone before the
            Sanctum's outer wall and waits for him to stop beside her.

            "I was chained in that wing," she says. "I would like to see it from the
            other side of its door. I think I am allowed that." She does not ask whether
            he is sure about helping the woman who sent the column to Stonebeard. She
            asks, "Are you ready?", which is the kinder question, and she waits as
            though she has all night. The borrowed blade is at her hip, and her hand is
            nowhere near it.
        """.trimIndent(),
        choices = wayChoices("Go in.", stair = true)
    ),
    StoryNode(
        id = "c12_way_stair",
        chapterId = "chapter_12",
        title = "The East Stair",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The east stair is where Voss said it would be, and where it was. The door at
            the top hangs from one hinge, and nobody is behind it. Voss looks at the
            first step for a moment longer than the step deserves, and then goes up
            ahead of him, because that is a thing she has decided to be able to do.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "what_the_frame_did"))
    ),
    StoryNode(
        id = "c12_way_wall",
        chapterId = "chapter_12",
        title = "The South Wall",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The south wall has a breach in it where the fire went through, and the hold
            Kaelen climbed on the night of the watch change is a charred stump of
            masonry. He looks up at it out of habit, and then at the open gate beside
            it, and finds it strange to feel almost fond of a wall.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "what_the_frame_did"))
    ),
    StoryNode(
        id = "c12_way_cart",
        chapterId = "chapter_12",
        title = "The Tithe Road",
        illustrationId = "back_to_the_ash",
        narrativeText = """
            The tithe road is empty. No cart, no collector, no mule belonging to nobody
            in particular, and the arch at its end stands open with no one there to not
            look at him. He walks under it with ash on his hand that is only memory, and
            hears himself breathe in the quiet.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go in.", nextNodeId = "what_the_frame_did"))
    ),
    StoryNode(
        id = "what_the_frame_did",
        chapterId = "chapter_12",
        title = "What the Frame Did",
        illustrationId = "what_the_frame_did",
        narrativeText = """
            Four settings out of six was apparently enough to open something, if not enough to
            control it. The frame's chamber is a ruin, cinder-grey robes scattered and unmoving
            among the wreckage, and at the center of it Ilsevet herself, no guards, no vanguard,
            just her, bracing a ward that's failing in her own two hands the exact same way
            Halvard once braced one in his.

            She doesn't look surprised to see him. She looks, for the first time since Kaelen
            has known her, out of options. "I wasn't expecting you to come," she says. "I was
            hoping you might anyway."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Help her hold it.", nextNodeId = "the_unfinished_thing")
        )
    ),
    StoryNode(
        id = "the_unfinished_thing",
        chapterId = "chapter_12",
        title = "The Unfinished Thing",
        illustrationId = "the_unfinished_thing",
        narrativeText = """
            It doesn't have a shape so much as an argument about what shape it should have,
            pieces of six different wards fighting over one body that was never meant to hold
            all of them at once. It notices Kaelen the way flame notices air, not as an enemy
            exactly, just as something it hasn't finished consuming yet, and won't stop until it
            has.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "unfinished_thing_encounter"
    ),
    StoryNode(
        id = "unfinished_thing_defeat",
        chapterId = "chapter_12",
        title = "It Wins By Falling Apart",
        illustrationId = "the_unfinished_thing",
        narrativeText = """
            Six half-made wards fighting for one body was never a stable arrangement, and Kaelen
            doesn't so much defeat the thing as survive long enough to watch it lose an argument
            with itself. He's on the ground more than he's on his feet by the end, and the last
            blow that connects is more luck than skill, on either side of it.

            It goes still anyway, whatever held it together finally giving out under its own
            contradictions rather than anything Kaelen did particularly well. He'll take it. He
            isn't in any shape to ask for better.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "what_kaelen_does_with_her",
                consequences = Consequences(setFlags = setOf("lost_to_the_unfinished"))
            )
        )
    ),
    StoryNode(
        id = "what_kaelen_does_with_her",
        chapterId = "chapter_12",
        title = "What Kaelen Does With Her",
        illustrationId = "what_kaelen_does_with_her",
        narrativeText = """
            It goes still, eventually, the argument between six half-made wards finally losing
            enough coherence to stop being a threat. Ilsevet doesn't thank him; she looks like a
            woman doing arithmetic she doesn't like the answer to, over and over, in the
            wreckage of the only thing she's built in years.

            "Four settings," she says, mostly to herself. "Four, and it nearly took the whole
            Sanctum with it. I don't know what six would have done." She looks at Kaelen like
            the question underneath that sentence is the only one left worth asking.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Then you already have your answer. Stop before six finishes the job.\"",
                nextNodeId = "chapter12_end_helped",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 10)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    clearFlags = C12_WAY_FAMILY,
                    setFlags = setOf("warned_ilsevet_directly")
                )
            ),
            Choice(
                label = "\"Whatever this cost you, it isn't worth what it almost cost everyone else.\"",
                nextNodeId = "chapter12_end_helped",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 9)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    clearFlags = C12_WAY_FAMILY,
                    setFlags = setOf("appealed_to_ilsevets_conscience")
                )
            ),
            Choice(
                label = "Say nothing. Take a fragment of the broken frame, and leave her to what's left.",
                nextNodeId = "chapter12_end_helped",
                consequences = Consequences(
                    grantItemIds = listOf("shard_of_the_seventh_door"),
                    clearFlags = C12_WAY_FAMILY,
                    setFlags = setOf("left_ilsevet_to_her_wreckage")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter12_end_helped",
        chapterId = "chapter_12",
        title = "End of Chapter XII — Four Settings",
        illustrationId = "chapter12_threshold",
        narrativeText = """
            Nobody stops Kaelen and Voss on the way out. There's nobody left in fighting shape
            to try.

            The fragment of the frame sits wrong in Kaelen's pack the whole walk home, warm when
            it should be cold, humming faintly the way the spire used to hum from a distance.
            Voss doesn't ask to hold it. Neither of them says Ilsevet's name again until
            Stonebeard's tunnel swallows them whole.

            Four settings very nearly ended everything in that chamber. Two settings remain
            unaccounted for by her hand, and Kaelen has both of them. Whatever Ilsevet decides to
            do next, she now knows exactly what happens if she gets what she's been asking for.

            That should feel like more of a victory than it does.

            Chapter XIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go on.",
                nextNodeId = "the_orders_delegation_trusted",
                requirements = ChoiceRequirement(requiredFlags = setOf("trusted_voss_fully"))
            ),
            Choice(
                label = "Go on.",
                nextNodeId = "the_orders_delegation_guarded",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("trusted_voss_fully"))
            )
        )
    ),
    StoryNode(
        id = "watched_from_a_distance",
        chapterId = "chapter_12",
        title = "Watched From a Distance",
        illustrationId = "watched_from_a_distance",
        narrativeText = """
            Voss doesn't argue with the decision, which somehow makes it heavier rather than
            lighter. "She's had every chance to be someone worth risking your neck for," she
            says. "She's used every one of them to come at you with a blade instead."

            They send word to Fenmoor instead, brace for anything that might come out of the
            ash-field, and wait at Stonebeard for news that isn't theirs to make happen.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Wait.", nextNodeId = "word_of_what_happened")
        )
    ),
    StoryNode(
        id = "word_of_what_happened",
        chapterId = "chapter_12",
        title = "Word of What Happened",
        illustrationId = "word_of_what_happened",
        narrativeText = """
            It's four days before anyone reliable enough to trust brings word down from the
            Ashfall reaches: the fire burned itself out on its own. Whatever tore loose from the
            frame is gone now, one way or another, the messenger doesn't know which, and doesn't
            seem eager to find out. Ilsevet is alive. Most of her garrison isn't.

            Voss reads more into the silence around that report than Kaelen can. "Four settings
            nearly cost her everything she has left standing," she says. "She knows that now,
            whether either of us was there to watch it happen or not."
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Let it be someone else's cost, this time.", nextNodeId = "chapter12_end_refused")
        )
    ),
    StoryNode(
        id = "chapter12_end_refused",
        chapterId = "chapter_12",
        title = "End of Chapter XII — Four Settings",
        illustrationId = "chapter12_threshold",
        narrativeText = """
            Nobody comes to Stonebeard's gate in the days that follow. Nobody comes to
            Fenmoor's either. Whatever's left of Ilsevet's people is busy with wreckage that
            isn't Kaelen's problem to help clean up, this time.

            Four settings very nearly ended everything in that chamber, and Kaelen only knows
            the shape of it secondhand. Two settings remain unaccounted for by her hand, and
            Kaelen has both of them. Whatever Ilsevet decides to do next, she now knows exactly
            what happens if she gets what she's been asking for, and so, at a safer distance
            than he expected to keep it, does he.

            Chapter XIII awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go on.",
                nextNodeId = "the_orders_delegation_trusted",
                requirements = ChoiceRequirement(requiredFlags = setOf("trusted_voss_fully"))
            ),
            Choice(
                label = "Go on.",
                nextNodeId = "the_orders_delegation_guarded",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("trusted_voss_fully"))
            )
        )
    )
)

val chapter12 = com.thelastjailer.app.Chapter(
    id = "chapter_12",
    number = 12,
    title = "Chapter XII — Four Settings",
    startNodeId = "word_from_ashfall"
)
