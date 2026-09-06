package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter I — The Fallen Knight.
 *
 * Arc: an inciting choice at the black door (three genuinely different opening beats), a quiet
 * descent that lets Kaelen's past catch up with him, a rising sense of wrongness as the dwarven
 * forge falls silent, an encounter with an ally who tells the truth plainly, a warning heeded or
 * not, a climax in the dark, and an epilogue that reflects on the road behind without resolving
 * what's still ahead. The three opening branches and the roots/forge branches all converge before
 * Stonebeard Hold, matching the chapter roadmap (Fallen Knight -> Black Door -> Dwarven Path ->
 * Stonebeard Hold -> First Blood) while giving the middle of the chapter real texture instead of
 * a single straight line between illustrated beats.
 */
val chapter1Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "fallen_knight",
        chapterId = "chapter_1",
        title = "The Fallen Knight",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Rain fell across the ruined road.

            Kaelen had once worn the silver cloak of the King's Guard.

            Now the cloak was gone.

            His sword remained.

            His honour did not.

            Ahead, half-hidden beneath the roots of an ancient tree, stood a black iron door.

            There was no wall. No building.

            Only the door.

            And someone was knocking from the other side.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "OPEN THE DOOR",
                nextNodeId = "black_door_opened",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 2),
                    setFlags = setOf("opened_black_door")
                )
            ),
            Choice(
                label = "DRAW YOUR SWORD",
                nextNodeId = "sword_drawn",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("drew_sword")
                )
            ),
            Choice(
                label = "WALK AWAY",
                nextNodeId = "walked_the_road",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("walked_away")
                )
            )
        )
    ),
    StoryNode(
        id = "black_door_opened",
        chapterId = "chapter_1",
        title = "The Door Without a Wall",
        illustrationId = "iron_door_open_tunnel",
        narrativeText = """
            The door needs no push. It swings inward on its own, iron groaning against roots
            that have grown through its hinges for a hundred years.

            Beyond it, stone steps spiral down into the dark, carved by hands older than the tree above.
            Cold air rises to meet Kaelen's face. It smells of stone, and iron, and something waiting.

            The knocking has stopped. Whatever was on the other side knows he is coming.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Descend into the dark.", nextNodeId = "beneath_the_roots")
        )
    ),
    StoryNode(
        id = "sword_drawn",
        chapterId = "chapter_1",
        title = "Steel Answers Steel",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            Kaelen draws his sword. The motion is old habit, older than shame.

            The knocking stops at once, as if listening. Rain falls. A long silence stretches out
            between one heartbeat and the next.

            Then the black door creaks open on its own, cold air spilling out over the blade
            still raised in his hand.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Enter, blade first.", nextNodeId = "beneath_the_roots")
        )
    ),
    StoryNode(
        id = "walked_the_road",
        chapterId = "chapter_1",
        title = "The Weight of a Name",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Kaelen takes ten steps before the knocking comes again, softer now, almost patient.

            He remembers the men who died when he obeyed an order he should have questioned.
            A disgraced knight has no business with black doors and things that wait beneath trees.

            But the ruined road has its own memory. A few strides on, the earth has collapsed
            around a tangle of roots, opening a second way down into the dark — one he did not choose,
            but one that leads to the same place all the same.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Follow the broken ground down.", nextNodeId = "beneath_the_roots")
        )
    ),
    StoryNode(
        id = "beneath_the_roots",
        chapterId = "chapter_1",
        title = "What the Roots Remember",
        illustrationId = "roots_descent",
        narrativeText = """
            The stairway steadies beneath his boots, stone worn smooth by feet older than memory.

            Kaelen's hand finds something half-buried in the rubble at the base of the steps — a
            scrap of tarnished silver, bent and blackened, but he knows its shape before his
            fingers close around it. A clasp from a King's Guard cloak. Not his own. Someone
            else's, once.

            He could carry it. He could leave it where three years of rain and rot have already
            tried to bury it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep the token.",
                nextNodeId = "dwarven_path_kept",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("kept_the_token"),
                    grantItemIds = listOf("tarnished_guard_token")
                )
            ),
            Choice(
                label = "Leave the past buried.",
                nextNodeId = "dwarven_path_buried",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("buried_the_past")
                )
            )
        )
    ),
    StoryNode(
        id = "dwarven_path_kept",
        chapterId = "chapter_1",
        title = "The Dwarven Path",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The clasp goes into his pocket, a weight that was never his to carry and now is anyway.

            The stairway becomes a tunnel, its walls squared and chiseled by dwarven hands long before
            the tree above ever took root. Old runes catch what little light Kaelen carries with him.

            Somewhere far below, iron rings against stone — the steady rhythm of hammers at a forge
            that has not gone cold in longer than he has been alive.

            The rhythm falters. Then stops.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Follow the tunnel toward the silence.", nextNodeId = "the_silent_forge")
        )
    ),
    StoryNode(
        id = "dwarven_path_buried",
        chapterId = "chapter_1",
        title = "The Dwarven Path",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            He leaves the clasp where he found it, and doesn't look back to check whether that
            was mercy or just cowardice wearing a gentler word.

            The stairway becomes a tunnel, its walls squared and chiseled by dwarven hands long before
            the tree above ever took root. Old runes catch what little light Kaelen carries with him.

            Somewhere far below, iron rings against stone — the steady rhythm of hammers at a forge
            that has not gone cold in longer than he has been alive.

            The rhythm falters. Then stops.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Follow the tunnel toward the silence.", nextNodeId = "the_silent_forge")
        )
    ),
    StoryNode(
        id = "the_silent_forge",
        chapterId = "chapter_1",
        title = "The Silent Forge",
        illustrationId = "silent_forge",
        narrativeText = """
            The hammering does not resume.

            Kaelen reaches a wide landing where the tunnel opens toward a distant, warmer dark —
            the forge-light of Stonebeard Hold, still glowing, still burning. But the sound that
            should fill it has gone entirely still, and stillness, this far under the earth, is
            its own kind of warning.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Call out into the dark.",
                nextNodeId = "stonebeard_hold",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("announced_himself")
                )
            ),
            Choice(
                label = "Wait, and listen.",
                nextNodeId = "stonebeard_hold",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("listened_first")
                )
            ),
            Choice(
                label = "Move without a sound, blade ready.",
                nextNodeId = "stonebeard_hold",
                requirements = ChoiceRequirement(requiredFlags = setOf("drew_sword")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("crept_in")
                )
            )
        )
    ),
    StoryNode(
        id = "stonebeard_hold",
        chapterId = "chapter_1",
        title = "Stonebeard Hold",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The tunnel opens onto a great carved hold, its gate cracked through and half-fallen.
            A single dwarven smith works alone at the broken frame, favoring one arm, the forge behind
            him the only warmth in the room.

            He looks up at Kaelen — a stranger, a knight's ruined bearing, a sword that has seen use —
            and says nothing, waiting to see which one he is.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Help him repair the gate.",
                nextNodeId = "smiths_warning_helped",
                consequences = Consequences(
                    setFlags = setOf("helped_dwarf"),
                    grantItemIds = listOf("dwarven_token", "healing_draught"),
                    unlockTrophy = "Friend of Stonebeard"
                )
            ),
            Choice(
                label = "Tell him honestly why your cloak is gone.",
                nextNodeId = "smiths_warning_confessed",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 1)),
                consequences = Consequences(
                    setFlags = setOf("confessed_to_dwarf"),
                    statDeltas = mapOf(StatType.HONOUR to 1)
                )
            ),
            Choice(
                label = "Move on without a word.",
                nextNodeId = "smiths_warning_moved_on"
            )
        )
    ),
    StoryNode(
        id = "smiths_warning_helped",
        chapterId = "chapter_1",
        title = "A Warning in the Dark",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The gate sits a little straighter for the work, and the smith straightens with it,
            some of the wariness gone out of his shoulders now that he's seen what Kaelen's hands
            are actually good for.

            "Something's been moving in the lower passage," he says. "Pale. Too many arms for
            anything honest. It's taken two of my kin already, and it isn't finished." He does
            not ask Kaelen to stay. He does not ask him to go, either.

            The passage beyond the hold is the only way forward.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the buckler he offers.",
                nextNodeId = "first_blood",
                requirements = ChoiceRequirement(requiredFlags = setOf("helped_dwarf")),
                consequences = Consequences(
                    setFlags = setOf("took_buckler"),
                    grantItemIds = listOf("sturdy_buckler")
                )
            ),
            Choice(
                label = "Heed his warning and go on.",
                nextNodeId = "first_blood"
            )
        )
    ),
    StoryNode(
        id = "smiths_warning_confessed",
        chapterId = "chapter_1",
        title = "A Warning in the Dark",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The smith hears him out without interrupting, and whatever he makes of a stranger's
            honesty, he keeps most of it to himself, though something in how he speaks next is a
            shade less guarded than before.

            "Something's been moving in the lower passage," he says. "Pale. Too many arms for
            anything honest. It's taken two of my kin already, and it isn't finished." He does
            not ask Kaelen to stay. He does not ask him to go, either.

            The passage beyond the hold is the only way forward.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the buckler he offers.",
                nextNodeId = "first_blood",
                requirements = ChoiceRequirement(requiredFlags = setOf("helped_dwarf")),
                consequences = Consequences(
                    setFlags = setOf("took_buckler"),
                    grantItemIds = listOf("sturdy_buckler")
                )
            ),
            Choice(
                label = "Heed his warning and go on.",
                nextNodeId = "first_blood"
            )
        )
    ),
    StoryNode(
        id = "smiths_warning_moved_on",
        chapterId = "chapter_1",
        title = "A Warning in the Dark",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Kaelen doesn't explain himself, and the smith doesn't ask him to. Whatever judgment
            passed between them back at the broken gate, neither one puts it into words.

            "Something's been moving in the lower passage," he says. "Pale. Too many arms for
            anything honest. It's taken two of my kin already, and it isn't finished." He does
            not ask Kaelen to stay. He does not ask him to go, either.

            The passage beyond the hold is the only way forward.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Take the buckler he offers.",
                nextNodeId = "first_blood",
                requirements = ChoiceRequirement(requiredFlags = setOf("helped_dwarf")),
                consequences = Consequences(
                    setFlags = setOf("took_buckler"),
                    grantItemIds = listOf("sturdy_buckler")
                )
            ),
            Choice(
                label = "Heed his warning and go on.",
                nextNodeId = "first_blood"
            )
        )
    ),
    StoryNode(
        id = "first_blood",
        chapterId = "chapter_1",
        title = "First Blood",
        illustrationId = "cavern_ambush",
        narrativeText = """
            Even forewarned, nothing could have made the passage feel less narrow, or the dark
            less absolute. Something that is not stone detaches itself from the black ahead —
            pale, many-limbed, hungry. It has been waiting far longer than Kaelen has been
            walking, and it has already fed twice tonight.

            There is no door to knock on here. Only the fight.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "first_blood_encounter"
    ),
    StoryNode(
        id = "first_blood_defeat",
        chapterId = "chapter_1",
        title = "End of Chapter I — What the Dark Let Him Keep",
        illustrationId = "threshold_ahead",
        narrativeText = """
            The thing doesn't stop moving. It simply decides, with the same cold patience it's
            clearly used on the two kin it already took, that Kaelen isn't worth finishing
            tonight — and drags itself back into the black before he can find the strength to
            make it regret that. He comes back to himself some time later, alone in the passage,
            one hand pressed to a wound that will scar and the other closed around nothing where
            his coin purse used to hang.

            Ten gold pieces lighter and considerably less sure of his own sword arm, he takes
            stock of what's left. Behind him: a black door that opened for no one and everyone,
            roots that remembered a war he wasn't part of, a dwarf who fed him nothing but truth.
            Ahead, past where the dwarven chisel marks give out and something older and far
            smoother takes over, the tunnel keeps going regardless of whether he won anything
            down here or not.

            He is not the knight he was three years ago. Tonight made sure he'd feel every year
            of that gap. But he's still walking, which is more than the thing in the passage
            expected.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Follow the passage deeper.",
                nextNodeId = "the_tended_dark",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1, StatType.GOLD to -10),
                    setFlags = setOf("pressed_deeper", "survived_first_blood_defeat")
                )
            ),
            Choice(
                label = "Carry word back to Stonebeard Hold first.",
                nextNodeId = "word_at_stonebeard",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.GOLD to -10),
                    setFlags = setOf("returned_to_hold", "survived_first_blood_defeat")
                )
            )
        )
    ),
    StoryNode(
        id = "chapter1_end",
        chapterId = "chapter_1",
        title = "End of Chapter I — The First Threshold",
        illustrationId = "threshold_ahead",
        narrativeText = """
            The creature falls still. Kaelen's breath comes ragged in the dark beneath Stonebeard
            Hold.

            Behind him: a black door that opened for no one and everyone, roots that remembered a
            war he wasn't part of, a dwarf who fed him nothing but truth. Ahead: whatever waits
            past this threshold, indifferent to the cloak he no longer wears or the name he
            can't unspeak.

            He is not the knight he was three years ago. He was never going to be. But something
            in the dark has finally noticed that he's still walking, and that will have to be
            enough for now.

            Chapter II awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Go on.", nextNodeId = "the_deeper_dark")
        )
    )
)

val chapter1 = com.thelastjailer.app.Chapter(
    id = "chapter_1",
    number = 1,
    title = "Chapter I — The Fallen Knight",
    startNodeId = "fallen_knight"
)
