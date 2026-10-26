package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.TOLD
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.explore
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.picker
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.data.Chapter6Kit.winOrLose
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 7 as written: Halvard at the forge (Chapter V's shut hand, Chapter VI's trade or confidence, Kestrel's letter,
 * the promise to Edda), Voss's return and the ladder she starts on, the ambush on ground Kaelen picks or ended by Voss, the
 * signet in every telling, and the fork between bringing Voss below and keeping the matter outside. These tests pin the
 * routing and the gates, what each decision costs and gives, that every route reaches Chapter 8, and that the scenes that
 * shipped are word for word as before.
 */
class Chapter7ExpansionTest {

    private val goal = "c8_ash_camp"
    private val rungs = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")
    private val grounds = listOf("c7_ground_yard", "c7_ground_stair", "c7_ground_road")

    private fun words(text: String) = text.replace(Regex("\\s+"), " ").trim()

    // ---------------------------------------------------------------------------------------------
    // The forge
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `Chapter 6's three endings lead to the forge, and the forge is Chapter 7's first scene`() {
        listOf("chapter6_end_sealed", "chapter6_end_reported", "chapter6_end_left").forEach { id ->
            assertEquals(id, listOf("c7_forge_night"), visible(at(id)).map { it.nextNodeId })
        }
        assertEquals("c7_forge_night", StoryRepository.chapters.single { it.id == "chapter_7" }.startNodeId)
    }

    @Test
    fun `the forge sends each state of Chapter 5 and Chapter 6 to exactly one scene`() {
        val expected = listOf(
            setOf("c6_halvard_reticent", "kept_the_offer_from_halvard") to "c7_forge_trade",
            setOf("kept_the_offer_from_halvard") to "c7_forge_hand",
            setOf("c6_halvard_confided") to "c7_forge_letter",
            setOf("c6_halvard_confided", TOLD) to "c7_forge_letter",
            setOf(TOLD) to "c7_forge_letter",
            emptySet<String>() to "c7_forge_letter"
        )
        expected.forEach { (flags, scene) ->
            assertEquals("flags $flags", listOf(scene), visible(at("c7_forge_night", flags = flags)).map { it.nextNodeId })
        }
    }

    @Test
    fun `the first choice of the chapter clears everything the chapter owns and nothing from another chapter`() {
        val stale = C7_TELLING_FLAGS + setOf(TOLD, "refused_envoy", "c6_offered_the_hold", "halvard_died")
        val after = take(at("c7_forge_night", flags = stale + "c6_halvard_reticent"), "c7_forge_trade")
        assertTrue(after.flags.intersect(C7_TELLING_FLAGS).isEmpty())
        assertTrue(after.flags.containsAll(setOf(TOLD, "refused_envoy", "c6_offered_the_hold", "halvard_died", "c6_halvard_reticent")))
    }

    private fun forgeWalks(flags: Set<String>) = explore(at("c7_forge_night", flags = flags), { it.sceneId == "voss_returns_alone" })

    @Test
    fun `every way through the forge reaches Voss with one answer per family and the right Edda outcome`() {
        val combos = listOf(
            setOf("c6_halvard_reticent", "kept_the_offer_from_halvard"),
            setOf("c6_halvard_reticent", "kept_the_offer_from_halvard", "c6_offered_the_hold"),
            setOf("kept_the_offer_from_halvard"),
            setOf("kept_the_offer_from_halvard", "c6_offered_the_hold"),
            setOf("c6_halvard_confided"),
            setOf("c6_halvard_confided", "c6_took_cairn_note", "c6_offered_the_hold"),
            setOf(TOLD, "c6_offered_the_hold"),
            emptySet()
        )
        combos.forEach { flags ->
            val walks = forgeWalks(flags)
            assertTrue("flags $flags", walks.isNotEmpty())
            walks.forEach { (trail, end) ->
                assertEquals("forge family at $trail", 1, end.flags.count { it in C7_FORGE_FAMILY })
                val hold = end.flags.count { it in C7_HOLD_FAMILY }
                assertEquals("hold family at $trail with $flags", if ("c6_offered_the_hold" in flags) 1 else 0, hold)
                if ("c7_hold_terms" in end.flags) assertTrue(trail.toString(), "c7_halvard_open" in end.flags)
                if ("c7_hold_refused" in end.flags) assertFalse(trail.toString(), "c7_halvard_open" in end.flags)
                assertEquals(
                    "open exactly when the talk went well: $trail",
                    end.flags.any { it in setOf("c7_traded", "c7_page", "c7_comfort") },
                    "c7_halvard_open" in end.flags
                )
            }
        }
    }

    @Test
    fun `opening the hand costs nothing and earns Honour while keeping it shut earns nothing`() {
        val open = take(at("c7_forge_trade", honour = 0), "c7_trade_made")
        assertEquals(1, open.honour)
        assertTrue(open.flags.containsAll(setOf("c7_traded", "c7_halvard_open")))
        val shut = take(at("c7_forge_trade", honour = 0), "c7_trade_refused")
        assertEquals(0, shut.honour)
        assertEquals(setOf("c7_shut"), shut.flags)
        val setDown = take(at("c7_forge_hand", honour = 0), "c7_trade_made")
        assertEquals(1, setDown.honour)
    }

    @Test
    fun `Kestrel's page can be given only by someone who took it, and it is the only way to a page`() {
        val without = visible(at("c7_forge_letter")).map { it.nextNodeId }
        assertEquals(listOf("c7_letter_comfort", "c7_letter_quiet"), without)
        val with = at("c7_forge_letter", flags = setOf("c6_took_cairn_note"), honour = 0)
        assertEquals(listOf("c7_letter_page", "c7_letter_comfort", "c7_letter_quiet"), visible(with).map { it.nextNodeId })
        val given = take(with, "c7_letter_page")
        assertEquals(1, given.honour)
        assertTrue(given.flags.containsAll(setOf("c7_page", "c7_halvard_open")))
        assertTrue("comfort opens him too, silence does not", "c7_halvard_open" in take(with, "c7_letter_comfort").flags)
        assertFalse("c7_halvard_open" in take(with, "c7_letter_quiet").flags)
    }

    @Test
    fun `Edda's promise is acknowledged only by someone who made it, and Halvard's answer depends on how open he is`() {
        listOf("c7_trade_made", "c7_trade_refused", "c7_letter_page", "c7_letter_comfort", "c7_letter_quiet").forEach { id ->
            val promised = at(id, flags = setOf("c6_offered_the_hold"))
            assertEquals(id, listOf("c7_edda_promise"), visible(promised).map { it.nextNodeId })
            assertEquals(id, listOf("voss_returns_alone"), visible(at(id)).map { it.nextNodeId })
        }
        val open = at("c7_edda_promise", flags = setOf("c7_halvard_open"), honour = 0)
        assertEquals(listOf("c7_hold_terms", "c7_hold_hidden"), visible(open).map { it.nextNodeId })
        val told = take(open, "c7_hold_terms")
        assertEquals(1, told.honour)
        assertTrue("c7_hold_terms" in told.flags)
        val closed = at("c7_edda_promise", honour = 0)
        assertEquals(listOf("c7_hold_refused", "c7_hold_hidden"), visible(closed).map { it.nextNodeId })
        assertEquals(1, take(closed, "c7_hold_refused").honour)
        val hidden = take(closed, "c7_hold_hidden")
        assertEquals(0, hidden.honour)
        assertTrue("c7_hold_hidden" in hidden.flags)
    }

    @Test
    fun `the promise to Edda is Kaelen's own and Halvard is never shown to have agreed before he is asked`() {
        val text = words(node("c7_edda_promise").narrativeText)
        assertTrue(text.contains("he would answer to Halvard for them"))
        assertTrue(text.contains("He did not say it to the man whose door it is"))
        assertTrue(words(node("c7_hold_terms").narrativeText).contains("That was my door you spoke for"))
        assertTrue(words(node("c7_hold_refused").narrativeText).contains("I will not make this hold a refuge on a stranger's word"))
        assertFalse(words(node("c7_hold_hidden").narrativeText).contains("agreed"))
    }

    // ---------------------------------------------------------------------------------------------
    // Voss's account and the ladder
    // ---------------------------------------------------------------------------------------------

    private val allEnvoyAnswers = setOf("refused_envoy", "heard_envoy_offer", "confronted_envoy_with_prisoners_words")

    @Test
    fun `each answer to Voss clears the old stance and starts her on the right rung`() {
        val expected = mapOf(
            "the_price_of_trust_trusted" to ("trusted_voss_fully" to "c7_rung_working"),
            "the_price_of_trust_guarded" to ("stayed_guarded" to "c7_rung_wary"),
            "the_price_of_trust_accountable" to ("held_voss_accountable" to "c7_rung_wary"),
            "the_price_of_trust_wary" to ("noted_hearing_the_offer" to "c7_rung_working"),
            "the_price_of_trust_unsurprised" to ("already_suspected_ilsevet" to "c7_rung_working"),
            "c7_riders_followed_up" to ("c7_asked_about_riders" to "c7_rung_working")
        )
        listOf("what_voss_knows_pressed", "what_voss_knows_welcomed").forEach { scene ->
            val stale = C7_STANCE_FAMILY + C7_RUNG_FAMILY + allEnvoyAnswers + "told_voss_of_the_riders"
            val start = at(scene, flags = stale)
            assertEquals(scene, expected.keys, visible(start).map { it.nextNodeId }.toSet())
            expected.forEach { (target, pair) ->
                val after = take(start, target)
                assertEquals("$scene -> $target stance", setOf(pair.first), after.flags.intersect(C7_STANCE_FAMILY))
                assertEquals("$scene -> $target rung", setOf(pair.second), after.flags.intersect(C7_RUNG_FAMILY))
            }
        }
    }

    @Test
    fun `the question about the riders is offered only to someone who told Voss about them`() {
        val without = visible(at("what_voss_knows_pressed", flags = allEnvoyAnswers)).map { it.nextNodeId }
        assertFalse("c7_riders_followed_up" in without)
        val answer = take(at("what_voss_knows_pressed", flags = setOf("told_voss_of_the_riders")), "c7_riders_followed_up")
        assertEquals(listOf("c7_the_ground"), visible(answer).map { it.nextNodeId })
    }

    @Test
    fun `welcoming or pressing Voss are alternatives`() {
        val s = take(at("voss_returns_alone", flags = setOf("pressed_voss", "welcomed_voss")), "what_voss_knows_welcomed")
        assertEquals(setOf("welcomed_voss"), s.flags)
    }

    // ---------------------------------------------------------------------------------------------
    // The ground
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `every answer to Voss leads to the choice of ground`() {
        listOf("trusted", "guarded", "accountable", "wary", "unsurprised").forEach {
            assertEquals(listOf("c7_the_ground"), visible(at("the_price_of_trust_$it")).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `Voss can end the ambush only if she has come far enough and Kaelen's word counts`() {
        fun offered(flags: Set<String>, honour: Int) =
            visible(at("c7_the_ground", flags = flags, honour = honour)).map { it.nextNodeId }.toSet()
        val fight = setOf("loyalists_move_in")
        assertEquals(fight, offered(setOf("c7_rung_wary"), 99))
        assertEquals("a save with no rung is treated as wary", fight, offered(emptySet(), 99))
        assertEquals(fight, offered(setOf("c7_rung_working"), 3))
        assertEquals(fight + "c7_called_off", offered(setOf("c7_rung_working"), 4))
        assertEquals("only one way to call them off is ever shown", 1, visible(at("c7_the_ground", flags = setOf("c7_rung_working"), honour = 9)).count { it.nextNodeId == "c7_called_off" })
    }

    @Test
    fun `calling the loyalists off skips the fight, costs Voss her standing, and still gives the signet`() {
        val start = at("c7_the_ground", flags = setOf("c7_rung_working"), honour = 4)
        val off = take(start, "c7_called_off")
        assertTrue("c7_voss_called_them_off" in off.flags)
        assertTrue("Called Off" in off.trophies)
        assertNull(node("c7_called_off").combatEncounterId)
        assertEquals(listOf("c7_hold_hearth", "what_voss_offers"), visible(off).map { it.nextNodeId })
        val below = take(off, "c7_hold_hearth")
        assertTrue("voss_seal" in below.inventory && "earned_voss_seal" in below.flags && "c7_voss_below" in below.flags)
        val outside = take(off, "what_voss_offers")
        assertTrue("voss_seal" in outside.inventory && "c7_voss_below" !in outside.flags)
        assertFalse("An Uneasy Alliance" in off.trophies)
    }

    @Test
    fun `each ground is fought on the real encounter and costs what it should`() {
        val costScene = mapOf("c7_ground_yard" to "c7_cost_yard", "c7_ground_stair" to "c7_cost_stair", "c7_ground_road" to "c7_cost_road")
        val target = mapOf("c7_ground_yard" to "Hold the yard", "c7_ground_stair" to "Fall back", "c7_ground_road" to "meet them on the road")
        grounds.forEach { ground ->
            val state = at("c7_the_ground", flags = setOf("c7_ground_stair", "c7_ground_road"))
            val label = visible(state).first { it.label.contains(target.getValue(ground)) }.label
            val chosen = takeLabel(state, label)
            assertEquals("a ground replaces the earlier one", setOf(ground), chosen.flags.intersect(C7_GROUND_FAMILY))
            assertEquals("loyalists_move_in", chosen.sceneId)
            val won = winOrLose(chosen, win = true)
            assertEquals("after_the_ambush", won.sceneId)
            assertTrue("An Uneasy Alliance" in won.trophies)
            val signet = take(won, costScene.getValue(ground))
            assertTrue("voss_seal" in signet.inventory)
            assertTrue("earned_voss_seal" in signet.flags)
            assertEquals(listOf("c7_hold_hearth", "what_voss_offers"), visible(signet).map { it.nextNodeId })
        }
    }

    @Test
    fun `a save at the signet with no ground recorded gets the road's cost, which fits what the old scene says`() {
        val legacy = at("after_the_ambush", flags = setOf("trusted_voss_fully"))
        val after = take(legacy, "c7_cost_road")
        assertTrue("voss_seal" in after.inventory)
        assertEquals(1, visible(legacy).size)
    }

    @Test
    fun `losing the ambush hurts Voss deeper and still ends in the signet`() {
        val lost = winOrLose(at("loyalists_move_in", flags = setOf("c7_ground_yard")), win = false)
        assertEquals("loyalist_ambush_defeat", lost.sceneId)
        val up = take(lost, "after_the_ambush")
        assertTrue(up.flags.containsAll(setOf("lost_to_loyalist_enforcer", "c7_voss_hurt_deeper")))
        assertEquals(listOf("c7_cost_yard"), visible(up).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // Below, or outside
    // ---------------------------------------------------------------------------------------------

    private fun rungAfter(start: String?, choiceLabel: String, scene: String, extra: Set<String> = emptySet()): String? {
        val flags = (if (start != null) setOf(start) else emptySet()) + extra
        val s = takeLabel(at(scene, flags = flags, honour = 3, courage = 3), choiceLabel)
        return s.flags.intersect(C7_RUNG_FAMILY).singleOrNull()
    }

    @Test
    fun `the hearth moves Voss one rung up, one down or none, and a save with no rung counts as the lowest`() {
        val mediate = "Stand between them. Let Voss answer in her own time."
        val side = "Let Halvard have his questions. He has earned them."
        val silent = "Say nothing. This is theirs to settle."
        val table = listOf(
            Triple("c7_rung_wary", mediate, "c7_rung_working"), Triple("c7_rung_wary", side, "c7_rung_wary"), Triple("c7_rung_wary", silent, "c7_rung_wary"),
            Triple("c7_rung_working", mediate, "c7_rung_trusted"), Triple("c7_rung_working", side, "c7_rung_wary"), Triple("c7_rung_working", silent, "c7_rung_working"),
            Triple(null, mediate, "c7_rung_working"), Triple(null, side, "c7_rung_wary"), Triple(null, silent, "c7_rung_wary")
        )
        table.forEach { (from, label, to) ->
            assertEquals("$from + $label", to, rungAfter(from, label, "c7_hold_hearth"))
        }
    }

    @Test
    fun `what Halvard makes of Voss follows how the hearth went, and the answer is exclusive`() {
        val start = at("c7_hold_hearth", flags = setOf("c7_rung_working", "c7_halvard_doubts_voss", "c7_halvard_trusts_voss"), honour = 0)
        val mediated = takeLabel(start, "Stand between them. Let Voss answer in her own time.")
        assertEquals(setOf("c7_halvard_trusts_voss"), mediated.flags.intersect(C7_VERDICT_FAMILY))
        assertEquals(1, mediated.honour)
        val sided = takeLabel(start, "Let Halvard have his questions. He has earned them.")
        assertEquals(setOf("c7_halvard_doubts_voss"), sided.flags.intersect(C7_VERDICT_FAMILY))
        assertEquals(0, sided.honour)
        val silent = takeLabel(start, "Say nothing. This is theirs to settle.")
        assertEquals(setOf("c7_halvard_doubts_voss"), silent.flags.intersect(C7_VERDICT_FAMILY))
    }

    @Test
    fun `tending the wound is kind and rewarded, questioning her while she hurts costs her trust, leaving her is neutral`() {
        val start = at("c7_wound", flags = setOf("c7_rung_working"), honour = 0, courage = 1)
        val tended = takeLabel(start, "Fetch Halvard's salve and tend it.")
        assertEquals(1, tended.honour)
        assertEquals(setOf("c7_rung_trusted"), tended.flags.intersect(C7_RUNG_FAMILY))
        val questioned = takeLabel(start, "Ask her, while she is tired, who she fears more: Ilsevet or the Order.")
        assertEquals(2, questioned.courage)
        assertEquals(setOf("c7_rung_wary"), questioned.flags.intersect(C7_RUNG_FAMILY))
        val left = takeLabel(start, "Leave her to it.")
        assertEquals(setOf("c7_rung_working"), left.flags.intersect(C7_RUNG_FAMILY))
        assertEquals(0, left.honour)
    }

    @Test
    fun `Halvard's word on the stair follows his verdict, and a save with none gets the doubting one`() {
        listOf("c7_wound_tended", "c7_wound_questioned", "c7_wound_left").forEach { id ->
            assertEquals(id, listOf("c7_verdict_trusts"), visible(at(id, flags = setOf("c7_halvard_trusts_voss"))).map { it.nextNodeId })
            assertEquals(id, listOf("c7_verdict_doubts"), visible(at(id, flags = setOf("c7_halvard_doubts_voss"))).map { it.nextNodeId })
            assertEquals(id, listOf("c7_verdict_doubts"), visible(at(id)).map { it.nextNodeId })
        }
        listOf("c7_verdict_trusts", "c7_verdict_doubts").forEach {
            assertEquals(listOf("what_voss_offers"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `Voss's offer shows the shipped three to a Voss kept outside and three new ones to a Voss brought below`() {
        val outside = visible(at("what_voss_offers")).map { it.nextNodeId }
        assertEquals(listOf("chapter7_end_now", "chapter7_end_warn_halvard", "chapter7_end_asked_for_time"), outside)
        val below = visible(at("what_voss_offers", flags = setOf("c7_voss_below"))).map { it.nextNodeId }
        assertEquals(listOf("c7_depart_now", "c7_depart_blessing", "c7_depart_time"), below)
        val weak = visible(at("what_voss_offers", flags = setOf("c7_voss_below"), courage = 1, honour = 0)).map { it.nextNodeId }
        assertEquals("the day to decide is always open", listOf("c7_depart_time"), weak)
        val weakOutside = visible(at("what_voss_offers", courage = 1, honour = 0)).map { it.nextNodeId }
        assertEquals(listOf("chapter7_end_asked_for_time"), weakOutside)
    }

    @Test
    fun `leaving from below costs and gives what it should`() {
        val base = at("what_voss_offers", flags = setOf("c7_voss_below"), courage = 6, honour = 5, health = 300)
        val now = takeLabel(base, "\"Take me there. I'd rather see it before it's aimed at anyone else.\"")
        assertEquals(7, now.courage)
        assertTrue("chose_ashfall_now" in now.flags)
        val warn = takeLabel(base, "\"I'll tell Halvard to his face before I go.\"")
        assertEquals(6, warn.honour)
        assertTrue("healing_draught" in warn.inventory)
        assertTrue("chose_to_warn_halvard" in warn.flags)
        val time = takeLabel(base, "\"Give me until dawn. This isn't a small thing to agree to.\"")
        assertEquals(321, time.health)
        assertTrue("asked_for_time" in time.flags)
        listOf(now, warn, time).forEach { assertEquals(1, it.flags.intersect(C7_DEPARTURE_FAMILY).size) }
        listOf("c7_depart_now", "c7_depart_blessing", "c7_depart_time").forEach {
            assertEquals(listOf(goal), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The shipped scenes
    // ---------------------------------------------------------------------------------------------

    private val shipped = mapOf(
        "voss_returns_alone" to "0e83f9d6b50dd74d9eab2e7d1916f92f2c655ccb55e23c78d4f2f8f45503c361",
        "what_voss_knows_pressed" to "3cd046cbfc39e694b9f789ebe6b9f00e138ec95a833292dac142499f4a28fe06",
        "what_voss_knows_welcomed" to "d99bd487f1ee98f993fbb8eb2996d2c90f19e9754341c297d36e183fb833a7b4",
        "the_price_of_trust_trusted" to "3761985d030f2640bca4e576867b538c39b27fe67169a61da4739dd7c33a5f76",
        "the_price_of_trust_guarded" to "3653c480e24cad4b4b731334bc64267d07edfa3fbb792381757c6be6a8b3549d",
        "the_price_of_trust_accountable" to "2449a275bc39a39d9a319d616b93938a46d4cc726667f3c2bf39f97d54b56103",
        "the_price_of_trust_wary" to "604c36ec382610cc8a1fd1859561bdd9b5667db0a1fea1b11fbee33b077852b8",
        "the_price_of_trust_unsurprised" to "595579b19a5a5b31da8921eec2be04e2997345c7b0efee0130cb22d2f59ff42d",
        "loyalists_move_in" to "8ec5765721de416b0d3daff200995d3a4e2a88fb8e9eb0f7413104f66c490e94",
        "loyalist_ambush_defeat" to "d71497dcca47109fadebe7e29eceeaedb0d0a6a79f82c6858ddb64e1ecbe6d4c",
        "after_the_ambush" to "8b9ac09eac5934e9c1c7c6cfbbfd64cfda72bd397801439055e8df0d1fd392a5",
        "what_voss_offers" to "c246f81c146a099659314bb5ffa50ab833492ead505ab734d80a6a005a3fa745",
        "chapter7_end_now" to "a00c7fdaf19fd31eef30f5cb525a8cb941bf1ed947d4a47236c2b070b6fc7b92",
        "chapter7_end_warn_halvard" to "c7dfb1a8c6f7f5ab4b665a5a50c88b760168468ecc49f02f50ab3309287e3603",
        "chapter7_end_asked_for_time" to "35c3d8918a9957aa39416e51c84798fa28b7f1f978594ec8b3f65b3b883bed28"
    )

    private fun sha(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(words(text).toByteArray()).joinToString("") { "%02x".format(it) }

    @Test
    fun `all fifteen scenes that shipped in Chapter 7 are word for word as before`() {
        val moved = shipped.filter { (id, hash) -> sha(node(id).narrativeText) != hash }.keys
        assertTrue("Changed scenes: $moved", moved.isEmpty())
        assertEquals("the fight is the original encounter", "loyalist_ambush_encounter", node("loyalists_move_in").combatEncounterId)
        val enc = CombatRepository.encounter("loyalist_ambush_encounter")
        assertEquals("after_the_ambush", enc.victoryNodeId)
        assertEquals("loyalist_ambush_defeat", enc.defeatNodeId)
    }

    @Test
    fun `no route leaves Chapter 7 without the signet`() {
        val outcomes = mutableListOf<GameState>()
        // fight on each ground, call-off, and a save that never recorded a ground
        grounds.forEach { g ->
            val won = winOrLose(at("loyalists_move_in", flags = setOf(g)), win = true)
            outcomes += take(won, "c7_cost_" + g.removePrefix("c7_ground_"))
        }
        outcomes += take(at("c7_called_off"), "what_voss_offers")
        outcomes += take(at("after_the_ambush"), "c7_cost_road")
        outcomes.forEach { assertTrue("voss_seal" in it.inventory && "earned_voss_seal" in it.flags) }
    }

    // ---------------------------------------------------------------------------------------------
    // Old saves, replays, and the whole chapter
    // ---------------------------------------------------------------------------------------------

    private val shippedScenes = shipped.keys.toList()

    private fun play(start: GameState, pick: (StoryNode, List<Choice>) -> Choice? = { _, _ -> null }): GameState =
        ReplayKit.play(start, stop = { it.sceneId == goal }, pick = pick)

    @Test
    fun `a save parked on any scene Chapter 7 had before it grew plays on to Chapter 8`() {
        val legacyFlags = mapOf(
            "after_the_ambush" to setOf("trusted_voss_fully"),
            "what_voss_offers" to setOf("trusted_voss_fully", "earned_voss_seal"),
            "chapter7_end_now" to setOf("earned_voss_seal"),
            "chapter7_end_warn_halvard" to setOf("earned_voss_seal"),
            "chapter7_end_asked_for_time" to setOf("earned_voss_seal")
        )
        shippedScenes.forEach { id ->
            listOf(true, false).forEach { strong ->
                val start = at(id, flags = legacyFlags[id].orEmpty(), courage = if (strong) 99 else 1, honour = if (strong) 99 else 0, health = 300)
                assertEquals("$id strong=$strong", goal, play(start).sceneId)
            }
        }
    }

    @Test
    fun `a second telling that decides differently carries nothing from the first`() {
        val first = ReplayKit.play(
            at("c7_forge_night", flags = setOf("c6_halvard_reticent", "kept_the_offer_from_halvard", "c6_offered_the_hold")),
            stop = { it.sceneId == goal },
            pick = picker(
                "c7_forge_night" to "Sit",
                "c7_forge_trade" to "Open your hand",
                "c7_trade_made" to "Stay a little longer",
                "c7_edda_promise" to "Tell him plainly",
                "voss_returns_alone" to "Come in out of the rain",
                "what_voss_knows_welcomed" to "I believe you",
                "the_price_of_trust_trusted" to "Fight beside her",
                "c7_the_ground" to "Tell Voss to speak",
                "c7_called_off" to "Take the signet. Bring her below",
                "c7_hold_hearth" to "Stand between them",
                "c7_hearth_mediated" to "Wait for the fire",
                "c7_wound" to "Fetch Halvard's salve",
                "what_voss_offers" to "I'll tell Halvard to his face"
            )
        )
        assertTrue(first.flags.containsAll(setOf("c7_traded", "c7_hold_terms", "welcomed_voss", "trusted_voss_fully", "c7_voss_called_them_off", "c7_voss_below", "c7_halvard_trusts_voss", "chose_to_warn_halvard")))
        val second = ReplayKit.play(
            first.copy(
                sceneId = "c7_forge_night", chapterId = "chapter_7",
                flags = first.flags - "c6_halvard_reticent" - "kept_the_offer_from_halvard" + "c6_halvard_confided" - "c6_offered_the_hold"
            ),
            stop = { it.sceneId == goal },
            pick = picker(
                "c7_forge_night" to "Sit",
                "c7_forge_letter" to "Say nothing",
                "voss_returns_alone" to "Why should I believe",
                "what_voss_knows_pressed" to "hear you out",
                "the_price_of_trust_guarded" to "Fight beside her",
                "c7_the_ground" to "Hold the yard",
                "after_the_ambush" to "Take the signet",
                "c7_cost_yard" to "Keep this between the two of you",
                "what_voss_offers" to "Give me a day"
            )
        )
        val gone = setOf(
            "c7_traded", "c7_halvard_open", "c7_hold_terms", "welcomed_voss", "trusted_voss_fully", "c7_voss_called_them_off",
            "c7_voss_below", "c7_halvard_trusts_voss", "chose_to_warn_halvard"
        )
        assertEquals("first-telling flags survive: ${second.flags intersect gone}", emptySet<String>(), second.flags intersect gone)
        assertTrue(second.flags.containsAll(setOf("c7_quiet", "pressed_voss", "stayed_guarded", "c7_ground_yard", "asked_for_time", "earned_voss_seal")))
        assertEquals(1, second.flags.count { it in C7_RUNG_FAMILY })
        assertEquals(1, second.flags.count { it in C7_FORGE_FAMILY })
    }

    @Test
    fun `eighteen hundred varied tellings reach Chapter 8 with a consistent set of flags and the signet`() {
        val random = java.util.Random(0x7007L)
        repeat(1800) { run ->
            val account = random.nextInt(3)
            val volunteered = random.nextBoolean()
            val flags = HashSet<String>()
            when (account) {
                0 -> flags += TOLD
                1 -> flags += "kept_the_offer_from_halvard"
            }
            if (!volunteered) flags += if (account == 1) "c6_halvard_reticent" else "c6_halvard_confided"
            if (random.nextBoolean()) flags += "c6_offered_the_hold"
            if (random.nextBoolean()) flags += "c6_took_cairn_note"
            if (random.nextBoolean()) flags += allEnvoyAnswers.toList()[random.nextInt(3)]
            if (random.nextBoolean()) flags += "told_voss_of_the_riders"
            var s = at(
                "c7_forge_night", flags = flags, courage = 1 + random.nextInt(9), honour = random.nextInt(8),
                gold = random.nextInt(90), health = 200 + random.nextInt(300)
            )
            var steps = 0
            while (s.sceneId != goal) {
                check(steps++ < 120) { "run $run never finished; stuck at ${s.sceneId}" }
                val n = node(s.sceneId)
                s = if (n.combatEncounterId != null) {
                    winOrLose(s, win = random.nextBoolean())
                } else {
                    val choices = visible(s)
                    check(choices.isNotEmpty()) { "run $run dead end at ${n.id} with ${s.flags}" }
                    s.applyChoice(choices[random.nextInt(choices.size)])
                }
            }
            val f = s.flags
            assertTrue("run $run signet", "voss_seal" in s.inventory && "earned_voss_seal" in f)
            assertEquals("run $run stance", 1, f.count { it in C7_STANCE_FAMILY })
            assertEquals("run $run rung", 1, f.count { it in C7_RUNG_FAMILY })
            assertEquals("run $run departure", 1, f.count { it in C7_DEPARTURE_FAMILY })
            assertEquals("run $run forge", 1, f.count { it in C7_FORGE_FAMILY })
            assertEquals("run $run hold promise", if ("c6_offered_the_hold" in f) 1 else 0, f.count { it in C7_HOLD_FAMILY })
            assertTrue("run $run ground or call-off", f.count { it in C7_GROUND_FAMILY } + (if ("c7_voss_called_them_off" in f) 1 else 0) == 1)
            if ("c7_voss_called_them_off" in f) assertTrue("run $run no fight", "lost_to_loyalist_enforcer" !in f && "An Uneasy Alliance" !in s.trophies)
            if ("c7_voss_below" in f) assertEquals("run $run verdict", 1, f.count { it in C7_VERDICT_FAMILY })
            if ("c7_voss_below" !in f) assertEquals("run $run no verdict outside", 0, f.count { it in C7_VERDICT_FAMILY })
            if ("lost_to_loyalist_enforcer" in f) assertTrue("c7_voss_hurt_deeper" in f)
            assertTrue("run $run gold", s.gold >= 0)
        }
    }
}
