package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.Consequences
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode

/**
 * Family C5: the flags that record how this telling answered the Marshal's envoy at the black door.
 * Each answer clears all three, then sets its own, so a replay that answers differently never carries
 * the earlier answer. Chapter VII reads all three as dialogue options with Voss, and the Journal groups
 * them as one moment, so they must describe this telling only. This runs when a choice is taken, never
 * when a scene is entered or loaded.
 */
val ENVOY_REPLY_FAMILY: Set<String> =
    setOf("refused_envoy", "heard_envoy_offer", "confronted_envoy_with_prisoners_words")

/**
 * Chapter V — What the Marshal Wants.
 *
 * Follows through on the Cinder Marshal thread Chapter IV left hanging. Where Voss represented the
 * Ashen Order's official position (bury the door), the Marshal's envoy represents a splinter that
 * wants the thing behind the gate understood and used rather than destroyed — and is willing to
 * talk first, which makes the eventual betrayal land harder than a straightforward siege would.
 * Arc: a parley that isn't what it looks like and plays out differently depending on whether
 * Kaelen meets the envoy alone or brings Halvard as a witness, an offer aimed specifically at the
 * jailer's brand, a fourth and toughest combat encounter yet, a stat-gated aftermath choice
 * (mirroring III and IV), and a chapter-ending reveal that raises the stakes past "one door" — the
 * Marshal, named for the first time, is going after every warded gate like this one at once. Ends
 * on an escalation hook rather than a resolution, same as II and III. This is the last free
 * chapter (see [com.thelastjailer.app.data.LocalEntitlementRepository.FREE_CHAPTER_LIMIT]).
 */
val chapter5Nodes: List<StoryNode> = listOf(
    StoryNode(
        id = "the_marshals_envoy",
        chapterId = "chapter_5",
        title = "The Marshal's Envoy",
        illustrationId = "the_marshals_envoy",
        narrativeText = """
            The weeks after the siege pass slower than any before them. Halvard's wound closes
            crooked, and he curses the cold more than he used to, but he mends. Stonebeard Hold's
            gate does not: cracked twice now, patched with iron that doesn't quite match the stone
            around it, a scar that isn't going to fade the way a warden's wounds eventually do.

            Then, on a morning gray enough that Kaelen almost misses it, a single rider comes up
            the ruined road to the black door beneath the tree. No banner. No column behind him
            this time. He dismounts well short of a sword's reach and raises both empty hands.

            "I'm not here to fight you," he calls. "I'm here to talk. My name doesn't matter yet.
            What I represent does."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Meet him at the door yourself.",
                nextNodeId = "the_envoys_offer_alone",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1, StatType.HEALTH to 40),
                    setFlags = setOf("met_envoy_alone")
                )
            ),
            Choice(
                label = "Bring Halvard up to stand with you.",
                nextNodeId = "the_envoys_offer_with_halvard",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1, StatType.HEALTH to 40),
                    setFlags = setOf("brought_halvard")
                )
            )
        )
    ),
    StoryNode(
        id = "the_envoys_offer_alone",
        chapterId = "chapter_5",
        title = "The Envoy's Offer",
        illustrationId = "cinder_envoy",
        narrativeText = """
            Kaelen goes to the door alone, and the envoy notices that immediately, the way a man
            notices an opening he wasn't sure he'd get. Up close he looks younger than Voss, and
            considerably less certain of himself: someone reciting words he half-believes, testing
            each one against a listener who isn't giving him anything back to work with.

            "The Ashen Order sent Voss to bury your door," he says. "The Cinder Marshal sent me to
            tell you Voss was wrong to try. What's behind that gate isn't refuse to be swept up.
            It's a resource nobody has had the nerve to actually use in three centuries."

            He glances at the brand on Kaelen's palm and doesn't pretend he hasn't noticed it, or
            that he came here for any other reason. "No warden watching. No witnesses. Just you
            and me, and an honest question: what's it worth to you, really, holding this alone?
            The Marshal doesn't want it destroyed. Doesn't even need it opened, not right away.
            Just wants to understand what a jailer's binding actually does, starting with yours.
            Cooperate, and Stonebeard's kin get the Order's protection instead of its blade.
            Refuse, and Voss's failure becomes someone else's problem to fix. Permanently."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Whatever the Marshal wants with the brand, the answer is no.\"",
                nextNodeId = "the_envoys_mask_slips_refused",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("refused_envoy")
                )
            ),
            Choice(
                label = "\"Keep talking. I'm listening.\"",
                nextNodeId = "the_envoys_mask_slips_heard",
                consequences = Consequences(
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("heard_envoy_offer")
                )
            ),
            Choice(
                label = "\"The thing behind that gate already told me what it wants. Did the Marshal send you to finish what it started?\"",
                nextNodeId = "the_envoys_mask_slips_confronted",
                requirements = ChoiceRequirement(requiredFlags = setOf("accepted_dark_aid")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("confronted_envoy_with_prisoners_words")
                )
            )
        )
    ),
    StoryNode(
        id = "the_envoys_offer_with_halvard",
        chapterId = "chapter_5",
        title = "The Envoy's Offer",
        illustrationId = "cinder_envoy",
        narrativeText = """
            Kaelen brings Halvard up with him, and the envoy's easy composure slips a fraction at
            the sight of a second, older set of eyes he now has to account for. Up close he looks
            younger than Voss, and considerably less certain of himself: someone reciting words
            he's rehearsed for one listener, not two.

            "The Ashen Order sent Voss to bury your door," he says, choosing his words more
            carefully now. "The Cinder Marshal sent me to tell you Voss was wrong to try. What's
            behind that gate isn't refuse to be swept up. It's a resource nobody has had the nerve
            to actually use in three centuries."

            Halvard makes a small, unimpressed sound at that and doesn't bother hiding it. The
            envoy presses on regardless, though he glances at the brand on Kaelen's palm like a
            man checking his footing before a long fall. "The Marshal doesn't want it destroyed.
            Doesn't even need it opened, not right away. Just wants to understand what a jailer's
            binding actually does, starting with yours. Cooperate, and Stonebeard's kin get the
            Order's protection instead of its blade. Refuse, and Voss's failure becomes someone
            else's problem to fix. Permanently."
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Whatever the Marshal wants with the brand, the answer is no.\"",
                nextNodeId = "the_envoys_mask_slips_refused",
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("refused_envoy")
                )
            ),
            Choice(
                label = "\"Keep talking. I'm listening.\"",
                nextNodeId = "the_envoys_mask_slips_heard",
                consequences = Consequences(
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("heard_envoy_offer")
                )
            ),
            Choice(
                label = "\"The thing behind that gate already told me what it wants. Did the Marshal send you to finish what it started?\"",
                nextNodeId = "the_envoys_mask_slips_confronted",
                requirements = ChoiceRequirement(requiredFlags = setOf("accepted_dark_aid")),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    clearFlags = ENVOY_REPLY_FAMILY,
                    setFlags = setOf("confronted_envoy_with_prisoners_words")
                )
            )
        )
    ),
    StoryNode(
        id = "the_envoys_mask_slips_refused",
        chapterId = "chapter_5",
        title = "The Mask Slips",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            The refusal doesn't land the way Kaelen expects. No anger, no counteroffer, just a
            beat too long before the envoy answers, the look of a man carefully filing something
            away rather than reacting to it. Whatever he just decided about Kaelen, and about
            Voss for putting him in this position at all, it isn't finished deciding.

            Then the recitation drops away entirely, and what's underneath is colder and far
            better prepared than the empty hands ever suggested.

            "That's unfortunate," he says, and doesn't sound like he means it as an apology. He
            doesn't reach for a weapon. He doesn't need to. Along the treeline, shapes that
            weren't there a moment ago straighten out of the undergrowth, an escort that walked
            the last mile of the ruined road in absolute silence, waiting the entire time for
            exactly this word.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Stand your ground.", nextNodeId = "the_escort_closes")
        )
    ),
    StoryNode(
        id = "the_envoys_mask_slips_heard",
        chapterId = "chapter_5",
        title = "The Mask Slips",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            "Keep talking" isn't the same as agreeing, but something in the way the envoy's
            shoulders ease tells Kaelen he's misjudged that distinction, if only for a moment. It
            doesn't last.

            The recitation drops away, and what's underneath is colder and far better prepared
            than the empty hands ever suggested.

            "That's unfortunate," he says, and doesn't sound like he means it as an apology. He
            doesn't reach for a weapon. He doesn't need to. Along the treeline, shapes that
            weren't there a moment ago straighten out of the undergrowth, an escort that walked
            the last mile of the ruined road in absolute silence, waiting the entire time for
            exactly this word.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Stand your ground.", nextNodeId = "the_escort_closes")
        )
    ),
    StoryNode(
        id = "the_envoys_mask_slips_confronted",
        chapterId = "chapter_5",
        title = "The Mask Slips",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            Naming what the prisoner already told him gets further under the envoy's composure
            than an outright refusal would have: a flicker of something that isn't quite alarm,
            quickly smoothed back over. Whatever he came expecting to negotiate, it wasn't a
            jailer who already knew the shape of the offer before he'd finished making it.

            The recitation drops away regardless, and what's underneath is colder and far better
            prepared than the empty hands ever suggested.

            "That's unfortunate," he says, and doesn't sound like he means it as an apology. He
            doesn't reach for a weapon. He doesn't need to. Along the treeline, shapes that
            weren't there a moment ago straighten out of the undergrowth, an escort that walked
            the last mile of the ruined road in absolute silence, waiting the entire time for
            exactly this word.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Stand your ground.", nextNodeId = "the_escort_closes")
        )
    ),
    StoryNode(
        id = "the_escort_closes",
        chapterId = "chapter_5",
        title = "The Escort Closes",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            They do not rush. The shapes along the treeline walk out in a long curve, each a
            measured pace from the next, and the curve closes around the black door the way
            a hand closes on something it means to keep. Nobody speaks. A palm lifts at one
            end of the line and every one of them stops at once, a clean dozen paces out, as
            if a cord had drawn tight.

            None of them looks at Kaelen's sword. They watch his hands, the branded palm
            most of all. Halvard comes up beside him, chain wound once round his fist, like
            a man who never trusted a quiet morning. Behind them the ruined road stays open,
            the one side the circle leaves unclosed, and that is no oversight. It is meant
            to be seen.

            Then the line opens in one place, exactly as wide as a man.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Watch the gap.",
                nextNodeId = "the_marshals_warning"
            )
        )
    ),
    StoryNode(
        id = "the_marshals_warning",
        chapterId = "chapter_5",
        title = "The Marshal's Warning",
        illustrationId = "cinder_adept_ambush",
        narrativeText = """
            One of them steps forward alone, cinder-grey robes over something harder underneath, a
            mark on the back of each hand that isn't the Ashen Order's sigil at all, and a plain
            band of grey iron on one finger. Whatever the Cinder Marshal branded onto this one, it
            wasn't mercy, and it shows in the way he moves, like a blade that's already decided
            how the fight ends.

            "The Marshal did warn me you might refuse," the adept says, almost fondly, and comes
            on fast.
        """.trimIndent(),
        choices = emptyList(),
        combatEncounterId = "cinder_envoy_encounter"
    ),
    StoryNode(
        id = "cinder_envoy_defeat",
        chapterId = "chapter_5",
        title = "Nearly Overrun",
        illustrationId = "aftermath_of_betrayal",
        narrativeText = """
            The adept doesn't fight like someone testing him, it fights like someone finishing an
            argument, and Kaelen's guard gives out a full breath before he's ready to admit it. He
            goes down hard enough to taste blood, hard enough that the next thing he's aware of is
            Halvard's boot planting itself between him and the follow-up strike.

            Whatever the old warden lacks in speed these days, he doesn't lack in timing. The
            adept falls anyway, moments later, to a blade that isn't Kaelen's own this time — a
            fact Kaelen intends to remember longer than he'd like to admit.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Get up.",
                nextNodeId = "aftermath_of_betrayal",
                consequences = Consequences(setFlags = setOf("lost_to_cinder_adept"))
            )
        )
    ),
    StoryNode(
        id = "aftermath_of_betrayal",
        chapterId = "chapter_5",
        title = "Aftermath",
        illustrationId = "aftermath_of_betrayal",
        narrativeText = """
            The adept goes down hard, and whatever nerve held the rest of the escort together goes
            with them: they scatter back down the ruined road faster than they arrived, boots
            slipping on wet stone, all but two, who fall back in good order as if told to. The
            envoy himself never drew a blade. He's already gone, horse and all, by the time
            Kaelen gets his breath back.

            Halvard took a graze along the ribs holding the line beside him, not enough to fell
            him, more than enough to slow him down for a week he doesn't have to spare. Someone
            left something behind in the retreat, though: a dispatch case, half-buried where the
            adept fell.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "\"Chase the envoy down before he reaches word of this to anyone.\"",
                nextNodeId = "the_chase_to_the_ford",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.COURAGE to 4)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.COURAGE to 1),
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("chased_the_envoy")
                )
            ),
            Choice(
                label = "\"Get Halvard back down to the Hold first. The envoy can wait.\"",
                nextNodeId = "halvard_by_the_door",
                requirements = ChoiceRequirement(minStats = mapOf(StatType.HONOUR to 3)),
                consequences = Consequences(
                    statDeltas = mapOf(StatType.HONOUR to 1),
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("stayed_with_halvard")
                )
            ),
            Choice(
                label = "Let him go, and see what he left behind.",
                nextNodeId = "what_the_ground_says",
                consequences = Consequences(
                    grantItemIds = listOf("cinder_marshal_missive"),
                    setFlags = setOf("searched_the_ground")
                )
            )
        )
    ),
    StoryNode(
        id = "the_chase_to_the_ford",
        chapterId = "chapter_5",
        title = "After the Envoy",
        illustrationId = "road_away_from_tree",
        narrativeText = """
            Kaelen drags the dispatch case out of the mud without breaking stride and runs.
            The churned road shows where the envoy's horse went: down the hill toward the
            stream crossing, at a pace no animal can keep up for long.

            Two of the escort did not scatter with the rest. They have planted themselves
            where the road pinches between rock and trees, blades out, in no hurry at all: a
            rearguard, and a patient one. Beyond them the envoy is a hunched shape on a
            hard-driven horse. To the left the pines drop away down a slope of loose shale
            that cuts the bend in half. It would put Kaelen at the crossing before the
            horse, and it would take something out of him on the way down.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Cut through the pines. Steep and loose underfoot, but it reaches the crossing before he does.",
                nextNodeId = "the_ford_caught",
                consequences = Consequences(statDeltas = mapOf(StatType.HEALTH to -10))
            ),
            Choice(
                label = "Stay on the road and break the rearguard. Surer footing, but he will be across before you arrive.",
                nextNodeId = "the_ford_lost"
            )
        )
    ),
    StoryNode(
        id = "the_ford_caught",
        chapterId = "chapter_5",
        title = "At the Crossing",
        illustrationId = "cinder_envoy",
        narrativeText = """
            The shale throws Kaelen down the slope rather than letting him down it. A stone
            turns under his boot and he goes the last stretch on his side, left forearm
            scraped raw and the breath knocked out of him. He fetches up against a root
            above the crossing.

            The envoy's horse is already in the water, and the stones Kaelen brought down
            rattle around its legs. Kaelen lurches in and catches the bridle, but his left
            hand has gone numb and will not close, and the envoy, white to the lips, can see
            it.

            "Let go," the envoy says. "Nobody is coming back for me, not the Marshal or
            anyone else." The horse rears, the leather tears out of Kaelen's grip, and he
            goes down in the stream as the envoy clatters up the bank. No one runs down a
            horse on foot with a hand that will not close. The rearguard reach the top of
            the slope, see the envoy across, and stop: they were told to hold the road until
            he was over. Kaelen hauls himself out and goes back to Halvard, alone at the
            door, the case still on his back, feeling returning to his hand as a hot ache.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Open the dispatch case.",
                nextNodeId = "what_the_missive_says"
            )
        )
    ),
    StoryNode(
        id = "the_ford_lost",
        chapterId = "chapter_5",
        title = "The Rearguard",
        illustrationId = "envoy_reveals_guard",
        narrativeText = """
            The road is kinder to his feet and crueller to his time. The rearguard fight
            like people told exactly how long to hold: close, patient, yielding a step only
            when they must, and never once trying to kill him. Kaelen takes a cut across the
            sleeve and breaks the first of them at last with a shoulder to the chest.

            By then the hoofbeats below have faded from the crossing to nothing. The two let
            go of him as if a rope had been cut and vanish into the trees. The backs of
            their hands carry the same dull scar as the adept's. Whatever the Marshal sends
            against the Hold, it seems to send it all branded alike.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Turn back. Open the dispatch case.",
                nextNodeId = "what_the_missive_says"
            )
        )
    ),
    StoryNode(
        id = "halvard_by_the_door",
        chapterId = "chapter_5",
        title = "Halvard, Bound Up",
        illustrationId = "dwarven_hold_gate",
        narrativeText = """
            Halvard says he can walk, and manages it as far as the first stair before he
            agrees to lean. Kaelen gets him to the bench in the lower passage and cuts the
            shirt from the graze: a long, shallow furrow along the ribs, ugly and clean.
            "I've been cut by better," Halvard says, "and outlived most of them. Stop
            looking at it as if it owes you something."

            He holds out a hand for the dispatch case anyway and spends longer on the mark
            pressed into the leather over the clasp than on his own wound. "A hand around a
            flame. We scratched a sign like that over watch-posts, if I remember it right.
            The hand was open, though, and this one is shut. I wouldn't hang a theory on a
            stamp."

            Far above, iron rings once against the patched gate, and again, and stops.
            Kaelen is on his feet before he thinks. "Let it knock," Halvard says, already
            closing his eyes. "They won't come down tonight, and the rain will have the road
            by morning whether you stand in it or not." He is asleep before the silence has
            finished settling.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Let him rest, and open the dispatch case.",
                nextNodeId = "what_the_missive_says"
            )
        )
    ),
    StoryNode(
        id = "what_the_ground_says",
        chapterId = "chapter_5",
        title = "What the Ground Says",
        illustrationId = "aftermath_of_betrayal",
        narrativeText = """
            The envoy is long gone, and Kaelen kneels where the adept fell, because a fight
            always leaves something behind.

            The ring comes first: an officer's band of grey iron on the left hand, the Ashen
            Order's stamp scored out of it with a blade and a smaller mark struck over the
            scoring, a closed hand around a flame. It will not come off the finger without a
            knife, and he is unwilling to use one. The backs of both hands carry the same
            dull scar he saw before the fight.

            The ground tells him the rest. The grass under the pines is pressed flat in a
            dozen places, and there is not one scrap of fire-black among them. They came in
            file, in the dark, and lay waiting without a spark to warm them.

            When he stands, something at the far edge of the pines is standing too: a
            figure, perfectly still, watching him the way a clerk watches a ledger. Kaelen
            takes a single step toward it and it is simply gone.
        """.trimIndent(),
        choices = listOf(
            Choice(
                label = "Open the dispatch case.",
                nextNodeId = "what_the_missive_says"
            )
        )
    ),
    StoryNode(
        id = "what_the_missive_says",
        chapterId = "chapter_5",
        title = "What the Missive Says",
        illustrationId = "cinder_marshal_missive_read",
        narrativeText = """
            The dispatch case holds one folded page, sealed in black wax stamped with a closed
            fist wrapped around a flame. The handwriting inside is precise, unhurried, and signed
            with a single name Kaelen has never heard spoken aloud: Ilsevet.

            The orders aren't about one gate. They name five others like it, scattered across
            ground Kaelen once swore to protect as a King's Guard, each with its own warden
            running as thin as Halvard. Voss's siege wasn't a punishment, reading it now. It was a
            rehearsal, for unmaking every ward like this one at once and seeing what answers when
            they fall together instead of one at a time.
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
            nearly did: one at a time, one door at a time, forgotten." He folds the page shut. "I
            never considered someone might be counting."

            Outside, the ruined road is empty again, rain filling in the tracks the escort left
            behind. It won't stay empty. Kaelen knows that now with a certainty that has nothing
            to do with the ache in his branded palm, and everything to do with a name he's
            already decided he isn't going to forget.

            Chapter VI awaits.
        """.trimIndent(),
        choices = listOf(
            Choice(label = "Rest, while you can.", nextNodeId = "the_choice_of_roads")
        )
    )
)

val chapter5 = com.thelastjailer.app.Chapter(
    id = "chapter_5",
    number = 5,
    title = "Chapter V — What the Marshal Wants",
    startNodeId = "the_marshals_envoy"
)
