package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter I — The Fallen Knight.
 *
 * Scene roadmap: The Fallen Knight -> The Black Door -> The Dwarven Path ->
 * Stonebeard Hold -> First Blood. The three opening choices lead to genuinely
 * different scenes (different text, stats and flags) before converging on
 * the shared path down into Stonebeard Hold, matching the linear roadmap.
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
            Choice(label = "Descend into the dark.", nextNodeId = "dwarven_path")
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
            Choice(label = "Enter, blade first.", nextNodeId = "dwarven_path")
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
            Choice(label = "Follow the broken ground down.", nextNodeId = "dwarven_path")
        )
    ),
    StoryNode(
        id = "dwarven_path",
        chapterId = "chapter_1",
        title = "The Dwarven Path",
        illustrationId = "root_tunnel_dwarven_path",
        narrativeText = """
            The stairway becomes a tunnel, its walls squared and chiseled by dwarven hands long before
            the tree above ever took root. Old runes catch what little light Kaelen carries with him.

            Somewhere far below, iron rings against stone — the steady rhythm of hammers at a forge
            that has not gone cold in longer than he has been alive.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Follow the sound of the hammers.", nextNodeId = "stonebeard_hold")
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
                nextNodeId = "first_blood",
                consequences = Consequences(
                    setFlags = setOf("helped_dwarf"),
                    grantItemIds = listOf("dwarven_token"),
                    unlockTrophy = "Friend of Stonebeard"
                )
            ),
            Choice(
                label = "Tell him honestly why your cloak is gone.",
                nextNodeId = "first_blood",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 1)),
                consequences = Consequences(
                    setFlags = setOf("confessed_to_dwarf"),
                    statDeltas = mapOf(StatType.HONOUR to 1)
                )
            ),
            Choice(
                label = "Move on without a word.",
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
            The passage past the hold narrows, and something that is not stone detaches itself
            from the dark — pale, many-limbed, hungry. It has been waiting far longer than Kaelen
            has been walking.

            There is no door to knock on here. Only the fight.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Meet it blade already drawn.",
                nextNodeId = "chapter1_end",
                requirements = ChoiceRequirement(requiredFlags = setOf("drew_sword")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -5, StatType.XP to 40, StatType.GOLD to 10),
                    unlockTrophy = "First Blood"
                )
            ),
            Choice(
                label = "Fight with everything you have.",
                nextNodeId = "chapter1_end",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -15, StatType.XP to 40, StatType.GOLD to 10),
                    unlockTrophy = "First Blood"
                )
            ),
            Choice(
                label = "Use the narrow tunnel against it.",
                nextNodeId = "chapter1_end",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 2)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -8, StatType.XP to 40),
                    unlockTrophy = "First Blood"
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
            The creature falls still. Kaelen's breath comes ragged in the dark of Stonebeard Hold's
            deep road.

            The black door behind him, the hold beside him, the fight just past — none of it has
            given him back what he lost. But something ahead has noticed him now, and the road
            beneath the ancient tree was only ever the first threshold.

            Chapter II awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Begin again.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter1 = com.thelastjailer.app.Chapter(
    id = "chapter_1",
    number = 1,
    title = "Chapter I — The Fallen Knight",
    startNodeId = "fallen_knight"
)
