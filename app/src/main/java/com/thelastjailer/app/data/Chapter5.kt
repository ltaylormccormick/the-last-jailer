package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Chapter V — What the Marshal Wants.
 *
 * Follows through on the Cinder Marshal thread Chapter IV left hanging. Where Voss represented the
 * Ashen Order's official position (bury the door), the Marshal's envoy represents a splinter that
 * wants the thing behind the gate understood and used rather than destroyed — and is willing to
 * talk first, which makes the eventual betrayal land harder than a straightforward siege would.
 * Arc: a parley that isn't what it looks like, an offer aimed specifically at the jailer's brand,
 * a fourth and toughest combat encounter yet, a stat-gated aftermath choice (mirroring III and IV),
 * and a chapter-ending reveal that raises the stakes past "one door" — the Marshal, named for the
 * first time, is going after every warded gate like this one at once. Ends on an escalation hook
 * rather than a resolution, same as II and III.
 */
val chapter5Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_marshals_envoy",
        chapterId = "chapter_5",
        title = "The Marshal's Envoy",
        illustrationId = "threshold_ahead",
        narrativeText = """
            The weeks after the siege pass slower than any before them. Halvard's wound closes
            crooked, and he curses the cold more than he used to, but he mends. Stonebeard Hold's
            gate does not — cracked twice now, patched with iron that doesn't quite match the
            stone around it.

            Then, on a morning gray enough that Kaelen almost misses it, a single rider comes up
            the ruined road to the black door beneath the tree. No banner. No column behind him
            this time. He dismounts well short of a sword's reach and raises both empty hands.

            "I'm not here to fight you," he calls. "I'm here to talk. My name doesn't matter yet.
            What I represent does."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Meet him at the door yourself.",
                nextNodeId = "the_envoys_offer",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("met_envoy_alone")
                )
            ),
            Choice(
                label = "Bring Halvard up to stand with you.",
                nextNodeId = "the_envoys_offer",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("brought_halvard")
                )
            )
        )
    ),
    StoryNode(
        id = "the_envoys_offer",
        chapterId = "chapter_5",
        title = "The Envoy's Offer",
        illustrationId = "cinder_envoy",
        narrativeText = """
            Up close he looks younger than Voss, and considerably less certain of himself — a man
            reciting words he half-believes. "The Ashen Order sent Voss to bury your door," he
            says. "The Cinder Marshal sent me to tell you Voss was wrong to try. What's behind
            that gate isn't refuse to be swept up. It's a resource nobody has had the nerve to
            actually use in three centuries."

            He glances at the brand on Kaelen's palm and doesn't pretend he hasn't noticed it.
            "The Marshal doesn't want it destroyed. Doesn't even need it opened, not right away.
            Just wants to understand what a jailer's binding actually does — starting with yours.
            Cooperate, and Stonebeard's kin get the Order's protection instead of its blade.
            Refuse, and Voss's failure becomes someone else's problem to fix. Permanently."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Whatever the Marshal wants with the brand, the answer is no.\"",
                nextNodeId = "the_envoys_mask_slips",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    setFlags = setOf("refused_envoy")
                )
            ),
            Choice(
                label = "\"Keep talking. I'm listening.\"",
                nextNodeId = "the_envoys_mask_slips",
                consequences = Consequences(setFlags = setOf("heard_envoy_offer"))
            ),
            Choice(
                label = "\"The thing behind that gate already told me what it wants. Did the Marshal send you to finish what it started?\"",
                nextNodeId = "the_envoys_mask_slips",
                requirements = ChoiceRequirement(requiredFlags = setOf("accepted_dark_aid")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    setFlags = setOf("confronted_envoy_with_prisoners_words")
                )
            )
        )
    ),
    StoryNode(
        id = "the_envoys_mask_slips",
        chapterId = "chapter_5",
        title = "The Mask Slips",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            Whatever answer Kaelen gives, something in the envoy's face changes — the recitation
            drops away, and what's underneath is colder and far better prepared than the empty
            hands suggested.

            "That's unfortunate," he says, and doesn't sound like he means it as an apology. He
            doesn't reach for a weapon. He doesn't need to. Along the treeline, shapes that
            weren't there a moment ago straighten out of the undergrowth — an escort that walked
            the last mile of the ruined road in absolute silence, waiting for exactly this word.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Stand your ground.", nextNodeId = "the_marshals_warning")
        )
    ),
    StoryNode(
        id = "the_marshals_warning",
        chapterId = "chapter_5",
        title = "The Marshal's Warning",
        illustrationId = "cinder_adept_ambush",
        narrativeText = """
            One of them steps forward alone — cinder-grey robes over something harder underneath,
            a mark on the back of each hand that isn't the Ashen Order's sigil at all. Whatever the
            Cinder Marshal branded onto this one, it wasn't mercy.

            "The Marshal did warn me you might refuse," the adept says, almost fondly, and comes
            on fast.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "cinder_envoy_encounter"
    ),
    StoryNode(
        id = "aftermath_of_betrayal",
        chapterId = "chapter_5",
        title = "Aftermath",
        illustrationId = "aftermath_of_betrayal",
        narrativeText = """
            The adept goes down hard, and whatever nerve held the rest of the escort together goes
            with them — they scatter back down the ruined road faster than they arrived. The envoy
            himself never drew a blade. He's already gone, horse and all, by the time Kaelen gets
            his breath back.

            Halvard took a graze along the ribs holding the line beside him — not enough to fell
            him, more than enough to slow him down for a week he doesn't have. Someone left
            something behind in the retreat: a dispatch case, half-buried where the adept fell.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Chase the envoy down before he reaches word of this to anyone.\"",
                nextNodeId = "what_the_missive_says",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("chased_the_envoy")
                )
            ),
            Choice(
                label = "\"Get Halvard back down to the Hold first. The envoy can wait.\"",
                nextNodeId = "what_the_missive_says",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("stayed_with_halvard")
                )
            ),
            Choice(
                label = "Let him go, and see what he left behind.",
                nextNodeId = "what_the_missive_says",
                consequences = Consequences(
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("searched_the_ground")
                )
            )
        )
    ),
    StoryNode(
        id = "what_the_missive_says",
        chapterId = "chapter_5",
        title = "What the Missive Says",
        illustrationId = "cinder_marshal_missive_read",
        narrativeText = """
            The dispatch case holds one folded page, sealed in black wax stamped with a closed fist
            wrapped around a flame. The handwriting inside is precise, unhurried, and signed with a
            single name Kaelen has never heard spoken aloud: Ilsevet.

            The orders aren't about one gate. They name five others like it, scattered across
            ground Kaelen once swore to protect as a King's Guard, each with its own warden running
            as thin as Halvard. Voss's siege wasn't a punishment. It was a rehearsal — for
            unmaking every ward like this one, all at once, and seeing what answers when they fall
            together instead of one at a time.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Bring this to Halvard.", nextNodeId = "chapter5_end")
        )
    ),
    StoryNode(
        id = "chapter5_end",
        chapterId = "chapter_5",
        title = "End of Chapter V — What the Marshal Wants",
        illustrationId = "chapter5_threshold",
        narrativeText = """
            Halvard reads the missive twice before he says anything, and when he does, his voice
            has lost the dry patience Kaelen has gotten used to. "Six wards," he says. "Six
            jailers, when the office was whole. I always assumed the others fell the way mine
            nearly did — one at a time, one door at a time, forgotten." He folds the page shut.
            "I never considered someone might be counting."

            Outside, the ruined road is empty again, rain filling in the tracks the escort left
            behind. It won't stay empty. Kaelen knows that now with a certainty that has nothing
            to do with the ache in his branded palm.

            Chapter VI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, while you can.", nextNodeId = "fallen_knight")
        )
    )
)

val chapter5 = com.thelastjailer.app.Chapter(
    id = "chapter_5",
    number = 5,
    title = "Chapter V — What the Marshal Wants",
    startNodeId = "the_marshals_envoy"
)
