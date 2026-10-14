package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family S: the flags that record how this telling went at Stonebeard Hold. Each `stonebeard_hold`
 * choice clears them all, then sets its own, so only the current telling's decision survives a replay.
 * This runs when a choice is taken, never when a scene is entered or loaded, so it cannot fire twice.
 * `took_buckler` is set one scene later and is read only by the Journal.
 * Later Chapter 1 batches add their new Stonebeard flags to this list as they introduce them.
 */
val STONEBEARD_FAMILY: Set<String> = setOf(
    "helped_dwarf", "confessed_to_dwarf", "took_buckler",
    "took_from_the_bench", "returned_the_draught", "owes_the_smith", "smith_vouched"
)

/**
 * Family O: the flags that record how this telling's opening went. Each `fallen_knight` choice clears
 * them all, then sets its own route's flags, so a replay never carries two routes at once. It fires
 * when a choice is taken, never when a scene is entered or loaded. Later batches add each new opening
 * flag here as they introduce it (Routes B and C bring `fought_the_riders`, `evaded_the_riders`, and so on).
 */
val OPENING_FAMILY: Set<String> = setOf(
    "opened_black_door", "drew_sword", "walked_away",
    "hunted_on_the_road", "hunters_saw_door", "met_red_cord_rider",
    "evaded_the_riders", "found_the_old_shaft", "found_red_cord",
    "fought_the_riders", "parleyed_with_rider", "withdrew_from_the_riders", "fell_through_the_door",
    "spared_the_rider", "robbed_the_rider", "rider_clue", "knows_riders_hired",
    "searched_the_wagon", "stripped_the_wagon", "left_the_wagon_be"
)

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

            Behind him a horn sounded, and was answered twice from further back along the road. Riders, strung out along the ridge, no colours and no crest, coming hard. The one in front rode with his left hand bound to the rein in red cord, wound tight across the knuckles.

            He had as long as a prayer.

            Ahead, half-hidden beneath the roots of an ancient tree, stood a black iron door.

            There was no wall. No building.

            Only the door.

            And someone was knocking from the other side.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "OPEN THE DOOR",
                nextNodeId = "ch1_door_witness",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 2),
                    clearFlags = OPENING_FAMILY,
                    setFlags = setOf("opened_black_door", "hunted_on_the_road")
                )
            ),
            Choice(
                label = "TURN AND FACE THEM",
                nextNodeId = "ch1_stand_and_turn",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = OPENING_FAMILY,
                    setFlags = setOf("drew_sword", "hunted_on_the_road", "met_red_cord_rider")
                )
            ),
            Choice(
                label = "BREAK FOR THE TREES",
                nextNodeId = "ch1_into_the_trees",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.HEALTH to -8),
                    clearFlags = OPENING_FAMILY,
                    setFlags = setOf("walked_away", "hunted_on_the_road")
                )
            )
        )
    ),
    StoryNode(
        id = "ch1_door_witness",
        chapterId = "chapter_1",
        title = "A Witness at the Door",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The hooves are on the road now, close enough to feel through his boots.

            He reaches the iron a heartbeat before the horses reach the tree.

            The knocking stops the instant his hand finds it.

            Behind him, the first rider hauls his mount up short. He has a bound left hand, red cord wound tight across the knuckles, and he does not draw. He only stares at a door standing in a place where no door should be, and at the man about to go through it.

            Behind the horns, a voice bellows a name across the rain. His. Not as a question.

            Kaelen watches recognition settle across the rider's face. The rider knows he's been seen.

            Then the iron begins to swing.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go through.",
                nextNodeId = "black_door_opened",
                consequences = Consequences(setFlags = setOf("hunters_saw_door", "met_red_cord_rider"))
            )
        )
    ),
    StoryNode(
        id = "ch1_into_the_trees",
        chapterId = "chapter_1",
        title = "Off the Road",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The hooves are on the road behind him, many and driven hard.

            Kaelen does not draw, and he does not run for the iron. He looks at the door, at the road, at the black wall of trees beside it, and chooses the trees. A man on foot in the dark can watch horses go by. Horses cannot watch him.

            He breaks left, off the road, into brush that tears at his sleeves.

            The ground falls away under him. He goes down a wet bank ribs-first and lies at the bottom with his breath gone, as the hooves thunder past above. A voice shouts his name into the rain, and then that he is close. A rider dismounts. Boots on the bank.

            Then the horn sounds again, and the boots stop, and go back up.

            Kaelen lies still until the sound of them has moved on along the road. His purse has torn from his belt in the fall. It is somewhere in the dark, down the slope.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Search the gully for what the fall took.",
                nextNodeId = "ch1_gully_searched",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HEALTH to -5),
                    setFlags = setOf("found_red_cord")
                )
            ),
            Choice(
                label = "Leave it. It isn't worth the noise.",
                nextNodeId = "ch1_gully_left",
                consequences = Consequences(statDeltas = mapOf(StatType.GOLD to -10))
            )
        )
    ),
    StoryNode(
        id = "ch1_gully_searched",
        chapterId = "chapter_1",
        title = "What the Gully Kept",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He works along the bottom of the gully on his hands and knees, one ear on the slope above, and every handful of mud costs his ribs a little more.

            The purse is wedged among roots, split at the seam but not empty. Beside it, snagged where a boot came down hard on the bank and stopped, lies a length of red cord. Plaited, thick as a finger, torn rather than cut, the torn end stiff and dark. Someone wound this tight around a hand. Someone did not get it back.

            Kaelen coils it and puts it away without quite deciding to.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Climb out along the gully.", nextNodeId = "ch1_gully_wagon")
        )
    ),
    StoryNode(
        id = "ch1_gully_left",
        chapterId = "chapter_1",
        title = "What the Gully Kept",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He gives the slope one long look and leaves it. The purse has burst somewhere in the mud, and what it held is scattered through roots and rain where he cannot find it without making a noise. What is left in his coat will have to be enough.

            Some things are worth being heard for. A handful of coin is not.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Climb out along the gully.", nextNodeId = "ch1_gully_wagon")
        )
    ),
    StoryNode(
        id = "ch1_the_old_shaft",
        chapterId = "chapter_1",
        title = "The Old Shaft",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            The hooves have gone. Up on the road the horn sounds once more, further away, and then there is only rain. The knocking at the door, which stopped when the horses came, has not begun again.

            At the far end of the gully the bank has been cut away, and Kaelen stops.

            The cut is too straight to be natural. Behind a curtain of roots a narrow shaft runs into the hillside, its mouth framed in dressed stone, breathing out a draught that is warm, and wrong for a night this wet, and smells of iron and coal. It is far too steep and narrow for a man in armour. It is old. Whoever cut it never meant it to be found from above.

            He marks it, the way a soldier marks a gate he may need later, and turns back toward the road.

            Nobody saw where he went.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Go back up to the road.",
                nextNodeId = "walked_the_road",
                consequences = Consequences(setFlags = setOf("evaded_the_riders", "found_the_old_shaft"))
            )
        )
    ),
    StoryNode(
        id = "ch1_watched_from_above",
        chapterId = "chapter_1",
        title = "From Above",
        illustrationId = "silent_forge",
        narrativeText = """
            The draught on the hillside did not lie. A low side-cut leads off the landing, dry and hardly wider than his shoulders, and it climbs until it ends at a grille of rusted iron set high in the wall of the hold.

            Below him, the smith is alone. He is not working. He stands with his good hand flat on the anvil and he is talking, low and level, the way a man talks to people who are not in the room. He names the tools on the rack, one by one. Kaelen understands that each name belonged to somebody.

            He was not meant to see this. He lets the smith finish. Then he works the grille loose without a sound and lowers himself down into the forge-light, where he can be seen before he can be mistaken for anything worse.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Step into the light.", nextNodeId = "stonebeard_hold")
        )
    ),
    // ---------------------------------------------------------------------------------------------
    // Route B - Turn and Face Them. A rider comes alone across the roots. The fight is optional: the
    // player can call across the roots instead, or back through the door. Prose for the fight outcomes
    // follows the agreed Route B text (chapter1-v2-reconciliation.md).
    // ---------------------------------------------------------------------------------------------
    StoryNode(
        id = "ch1_stand_and_turn",
        chapterId = "chapter_1",
        title = "Under the Tree",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            Kaelen turns his back on the door and draws, and the rain runs down the steel.

            The riders come out of the dark in a line and rein in at the edge of the roots, and the horses will not cross them. They toss their heads and blow and back away, white-eyed, from ground that looks like any other ground. The horn sounds a second time. Somewhere in the line a voice bellows a name across the rain. His.

            One rider swings down alone and walks forward into the roots. His left hand is bound in red cord. His sword is out, and he holds it like a man who has done this for coin before and expects to be paid for it again.

            "Come easy, knight," he says. "Nobody's paying for you dead."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Fight the one who comes.",
                nextNodeId = "ch1_rider_fight",
                consequences = Consequences(setFlags = setOf("fought_the_riders"))
            ),
            Choice(
                label = "Call across the roots: \"Who is paying you?\"",
                nextNodeId = "ch1_parley_answered",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("parleyed_with_rider", "knows_riders_hired")
                )
            ),
            Choice(
                label = "Back through the door.",
                nextNodeId = "ch1_back_to_the_door",
                consequences = Consequences(setFlags = setOf("withdrew_from_the_riders", "hunters_saw_door"))
            )
        )
    ),
    StoryNode(
        id = "ch1_rider_fight",
        chapterId = "chapter_1",
        title = "The Rider Comes",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            The rider comes in low and fast, and the rain closes over the first ring of steel.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "opening_rider_encounter"
    ),
    StoryNode(
        id = "ch1_rider_down",
        chapterId = "chapter_1",
        title = "The Rider Who Did Not Get Up",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            The rider goes down hard in the mud and stays there, breathing, one hand clamped to a sword-opened shoulder. Behind him the horn sounds a third time, and the other horses shy at the edge of the roots as though the ground itself had warned them off.

            They do not come closer. Kaelen does not wait to learn why.

            The man's saddlebag has split open. A purse. A stoppered vial. And the rider's eyes on him, waiting to find out which kind of man Kaelen is.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Search his saddlebags.",
                nextNodeId = "ch1_rider_searched",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to -1, StatType.GOLD to 12),
                    grantItemIds = listOf("healing_draught"),
                    setFlags = setOf("robbed_the_rider")
                )
            ),
            Choice(
                label = "Make him talk.",
                nextNodeId = "ch1_rider_questioned",
                requirements = ChoiceRequirement(forbiddenFlags = setOf("received_wax_disc")),
                consequences = Consequences(
                    grantItemIds = listOf("scraped_wax_disc"),
                    setFlags = setOf("rider_clue", "knows_riders_hired", "received_wax_disc")
                )
            ),
            // Replay only: the disc is a durable item, granted once per save lineage.
            Choice(
                label = "Press him again for what he knows.",
                nextNodeId = "ch1_rider_questioned",
                requirements = ChoiceRequirement(requiredFlags = setOf("received_wax_disc")),
                consequences = Consequences(setFlags = setOf("rider_clue", "knows_riders_hired"))
            ),
            Choice(
                label = "Bind his wound.",
                nextNodeId = "ch1_rider_bound",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("spared_the_rider")
                )
            )
        )
    ),
    StoryNode(
        id = "ch1_rider_searched",
        chapterId = "chapter_1",
        title = "What the Saddlebag Held",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            He takes the purse and the vial. The rider watches him do it and says nothing, which is worse than any curse.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wipe the blade.", nextNodeId = "ch1_knock_resumes"))
    ),
    StoryNode(
        id = "ch1_rider_questioned",
        chapterId = "chapter_1",
        title = "Half in Advance",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            "Half in advance," the rider says through his teeth. "The rest on delivery. Alive." He tugs the purse from his belt. On the flap is a disc of grey wax, its face ground flat. "Whoever sealed it scraped it first."

            He does not say who. Kaelen does not think he knows.
        """.trimIndent(),
        choices = listOf(Choice(label = "Wipe the blade.", nextNodeId = "ch1_knock_resumes"))
    ),
    StoryNode(
        id = "ch1_rider_bound",
        chapterId = "chapter_1",
        title = "A Binding in the Rain",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            He tears the man's own sleeve for a binding and pulls it tight. "I don't forgive you," the rider says, and means it. "But I'll remember this."
        """.trimIndent(),
        choices = listOf(Choice(label = "Wipe the blade.", nextNodeId = "ch1_knock_resumes"))
    ),
    StoryNode(
        id = "ch1_parley_answered",
        chapterId = "chapter_1",
        title = "Across the Roots",
        illustrationId = "knight_sword_drawn_door",
        narrativeText = """
            The rider stops at the edge of the roots as though he had walked into glass. For a moment Kaelen thinks he will laugh. He does not.

            "Half in advance," he says at last. "The rest on delivery. Alive." He spits into the wet. "I never asked who. The purse is the same colour whoever it comes from."

            The horn sounds a third time, and the horses scream at the roots. The rider looks once at the door, once at Kaelen, and goes back to his line without turning his back on either.
        """.trimIndent(),
        choices = listOf(Choice(label = "Sheathe the blade.", nextNodeId = "ch1_knock_resumes"))
    ),
    StoryNode(
        id = "ch1_back_to_the_door",
        chapterId = "chapter_1",
        title = "Back Through the Iron",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            Kaelen backs into the iron with the blade held between himself and the dismounted rider. The man does not follow. He stops at the edge of the roots and watches, his red-bound hand loose at his side, as the door swings wide behind Kaelen as if it had only been waiting to be asked.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go through.", nextNodeId = "black_door_opened"))
    ),
    StoryNode(
        id = "ch1_driven_to_the_door",
        chapterId = "chapter_1",
        title = "Driven to the Door",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The rider's second blow takes Kaelen's feet out from under him, and the iron is at his back. He braces for the wall.

            There is no wall. The door does not give under his shoulder. It simply opens, the way it opens for no one, and he goes through it backward with the third horn still in his ears.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "black_door_opened",
                consequences = Consequences(setFlags = setOf("hunters_saw_door", "fell_through_the_door"))
            )
        )
    ),
    StoryNode(
        id = "ch1_knock_resumes",
        chapterId = "chapter_1",
        title = "The Knocking Again",
        illustrationId = "black_door_beneath_the_tree",
        narrativeText = """
            The horn does not sound a fourth time. The riders are gone, or waiting. The blade is back in its sheath, and Kaelen stands in the rain, listening.

            Behind him, slow and heavy, stone against metal, the knocking begins again. His hand goes back to the hilt before he has decided anything.
        """.trimIndent(),
        choices = listOf(Choice(label = "Turn to the door.", nextNodeId = "sword_drawn"))
    ),
    // ---------------------------------------------------------------------------------------------
    // Route C - the gully wagon. An optional discovery for the evader. Generic by design: nothing says
    // whose cart it was.
    // ---------------------------------------------------------------------------------------------
    StoryNode(
        id = "ch1_gully_wagon",
        chapterId = "chapter_1",
        title = "The Wagon in the Gully",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Halfway along the gully the bank has swallowed a cart. It lies on its side in the mud with one wheel in the air, the canvas rotted to rags, the yoke snapped. No horse. No driver. No mark burned into the boards. It has been here longer than the hunt has been on the road.

            Under the tailboard, something has stayed dry.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Search it carefully.",
                nextNodeId = "ch1_wagon_searched",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.GOLD to 8),
                    setFlags = setOf("searched_the_wagon")
                )
            ),
            Choice(
                label = "Strip it of everything.",
                nextNodeId = "ch1_wagon_stripped",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to -1, StatType.GOLD to 14),
                    setFlags = setOf("stripped_the_wagon")
                )
            ),
            Choice(
                label = "Say a word over it and go on.",
                nextNodeId = "ch1_wagon_left",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("left_the_wagon_be")
                )
            )
        )
    ),
    StoryNode(
        id = "ch1_wagon_searched",
        chapterId = "chapter_1",
        title = "What Stayed Dry",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Under the tailboard: a few coins in a twist of cloth, and a flask of lamp oil with its stopper still sound. He takes the oil and the coins and leaves the rest, and the cart keeps its silence.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on along the gully.", nextNodeId = "ch1_the_old_shaft"))
    ),
    StoryNode(
        id = "ch1_wagon_stripped",
        chapterId = "chapter_1",
        title = "Nothing Left to Mind It",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He takes everything it has: the coins, the oil, the iron fittings that will fetch something later, the strap off the yoke. It takes a while, and it is not clean work. When he is done the cart looks like it has been robbed twice.

            It had no one left to mind it. That is what he tells himself.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on along the gully.", nextNodeId = "ch1_the_old_shaft"))
    ),
    StoryNode(
        id = "ch1_wagon_left",
        chapterId = "chapter_1",
        title = "A Word Over the Wreck",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            He stands a moment in the mud with his hand on the tailboard and says what a man says over something that cannot hear it. Then he takes his hand away and leaves the cart as he found it.
        """.trimIndent(),
        choices = listOf(Choice(label = "Go on along the gully.", nextNodeId = "ch1_the_old_shaft"))
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

            Beside it, under a rotted wool sleeve, lies the rest of whoever wore it.

            He could carry it. He could leave it where three years of rain and rot have already
            tried to bury it.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Keep the token.",
                nextNodeId = "dwarven_path_kept",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    // Mutually exclusive with burying it: a later telling must not hold both decisions.
                    clearFlags = setOf("buried_the_past"),
                    setFlags = setOf("kept_the_token"),
                    grantItemIds = listOf("tarnished_guard_token")
                )
            ),
            Choice(
                label = "Leave the past buried.",
                nextNodeId = "dwarven_path_buried",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    // Mutually exclusive with keeping it. A clasp carried over from an earlier telling
                    // stays in the inventory (known replay limitation; see docs/CONSEQUENCE_LEDGER.md).
                    clearFlags = setOf("kept_the_token"),
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
            ),
            Choice(
                label = "Find the old shaft and watch from above.",
                nextNodeId = "ch1_watched_from_above",
                requirements = ChoiceRequirement(requiredFlags = setOf("found_the_old_shaft")),
                consequences = Consequences(statDeltas = mapOf(StatType.COURAGE to 1))
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
                    clearFlags = STONEBEARD_FAMILY,
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
                    clearFlags = STONEBEARD_FAMILY,
                    setFlags = setOf("confessed_to_dwarf"),
                    statDeltas = mapOf(StatType.HONOUR to 1)
                )
            ),
            Choice(
                label = "Move on without a word.",
                nextNodeId = "smiths_warning_moved_on",
                consequences = Consequences(clearFlags = STONEBEARD_FAMILY)
            ),
            Choice(
                label = "Take a draught and the coin from the bench while his back is turned.",
                nextNodeId = "smiths_warning_robbed",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to -1, StatType.GOLD to 15),
                    clearFlags = STONEBEARD_FAMILY,
                    setFlags = setOf("took_from_the_bench", "owes_the_smith"),
                    grantItemIds = listOf("healing_draught")
                )
            )
        )
    ),
    StoryNode(
        id = "smiths_warning_robbed",
        chapterId = "chapter_1",
        title = "A Warning in the Dark",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            The smith turns back to the gate frame, and the bench is behind him, and Kaelen's hand is on the draught before he has decided to put it there. A stoppered vial, still warm from the forge. Beside it sits a tin with a few coins in it, left out for whatever dwarves need coin for, down here. He takes those too. He tells himself it is for the road.

            The smith does not turn. Kaelen could not say whether that is because he did not hear, or because he did.

            "Something's been moving in the lower passage," he says to the wall. "Pale. Too many arms for anything honest. It's taken two of my kin already." He does not look round. "Mind yourself."

            The passage beyond the hold is the only way forward.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Heed his warning and go on.",
                nextNodeId = "first_blood"
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
            stock of what's left. Behind him: a hunt he got ahead of, a door that knocked, roots that
            remembered a war he wasn't part of, a smith who watched him and said little.
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
                requirements = ChoiceRequirement(forbiddenFlags = setOf("took_from_the_bench")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.GOLD to -10),
                    setFlags = setOf("returned_to_hold", "survived_first_blood_defeat")
                )
            ),
            Choice(
                label = "Carry word back to Stonebeard Hold first.",
                nextNodeId = "word_at_stonebeard_thief",
                requirements = ChoiceRequirement(requiredFlags = setOf("took_from_the_bench")),
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

            Behind him: a hunt he got ahead of, a door that knocked, roots that remembered a
            war he wasn't part of, a smith who watched him and said little. Ahead: whatever waits
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
