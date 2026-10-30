package com.thelastjailer.app.ending

import com.thelastjailer.app.StatType
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * The legacy rule: table T2 and the margin rule. The table is pinned as literals and checked against the story itself; the
 * rule is checked on fixtures, on every clean telling, and by seeded simulation of the saves it must never get wrong.
 */
class LegacyRuleTest {
    private val decisions = LegacyRule.decisions

    // ---- the table -------------------------------------------------------------------------------------------

    private fun describe(d: LegacyRule.Decision): String =
        d.id + ": " + d.options.joinToString(", ") { it.flag + "=" + (it.axis?.name?.take(1) ?: "N") }

    /** The approved fourteen decisions in story order. C Courage, H Honour, S Steadfast, N recorded but scores nothing. */
    private val approved = listOf(
        "aid: accepted_dark_aid=C, refused_dark_aid_again=H",
        "wardens: began_training_new_wardens=C, stood_watch_rather_than_delegate=H, deferred_the_question=N",
        "old_debts: resolved_despite_sympathy=C, committed_to_proving_her_wrong=H, deferred_the_question_of_ilsevet=N",
        "frame: destroyed_the_frame_outright=C, disrupted_carefully=H, stopped_it_however_possible=N",
        "fate: captured_ilsevet=C, showed_ilsevet_mercy=H, deferred_ilsevets_fate=N",
        "sixth_vow: vowed_to_protect_the_prisoner=C, affirmed_its_own_choice=H, left_the_question_open=S",
        "commit: vowed_constant_vigilance=C, vowed_to_share_the_burden=H, simply_stayed=S",
        "emberlow: refused_to_leave_emberlow=C, insisted_on_real_choice=H, waited_at_emberlow=S",
        "greymoor: reminded_greymoor_of_choice=C, offered_presence_not_replacement=H, sat_with_greymoors_silence=S",
        "duskmere: vowed_to_hold_duskmere=C, sought_to_remember_wren=H, secured_duskmere_in_silence=S",
        "forgotten: pressed_the_forgotten_to_remember=C, respected_the_forgetting=H, stepped_back_from_the_sundering=S",
        "vigil: offered_the_vigil_alliance=C, respected_the_vigils_pace=H, simply_bore_witness_at_wraithspire=S",
        "start: chose_to_seek_the_ending=C, insisted_on_another_way=H",
        "final: demanded_it_be_earned=C, offered_connection_without_merging=H, simply_stayed_at_the_end=S"
    )

    /** Where each decision is taken. Fate is one decision in two variant scenes, picked by how far Voss is trusted. */
    private val scenes = mapOf(
        "aid" to listOf("what_the_prisoner_offers_again"), "wardens" to listOf("who_stands_with_stonebeard"),
        "old_debts" to listOf("old_debts"), "frame" to listOf("what_must_be_broken"),
        "fate" to listOf("what_kaelen_chooses_trusted", "what_kaelen_chooses_guarded"),
        "sixth_vow" to listOf("what_the_sixth_wants"), "commit" to listOf("what_kaelen_commits_to"),
        "emberlow" to listOf("reaching_emberlow"), "greymoor" to listOf("reaching_greymoor"),
        "duskmere" to listOf("what_yielding_cost"), "forgotten" to listOf("what_kaelen_offers_the_forgotten"),
        "vigil" to listOf("what_wraithspire_reveals"), "start" to listOf("what_must_happen_next"),
        "final" to listOf("what_kaelen_offers_at_the_end")
    )

    @Test
    fun `the table is the approved fourteen decisions with forty distinct option flags`() {
        assertEquals(approved, decisions.map { describe(it) })
        assertEquals(40, decisions.sumOf { it.options.size })
        assertEquals(40, decisions.flatMap { d -> d.options.map { it.flag } }.toSet().size)
        assertEquals(8, decisions.sumOf { d -> d.options.count { it.axis == Axis.STEADFAST } })
        assertEquals(4, decisions.sumOf { d -> d.options.count { it.axis == null } })
    }

    @Test
    fun `every option is a choice of the decision's own scene, every choice there is an option, and each sets exactly one flag of it`() {
        decisions.forEach { d ->
            val flags = d.options.map { it.flag }.toSet()
            val sceneIds = scenes.getValue(d.id)
            sceneIds.forEach { sceneId ->
                val choices = StoryRepository.node(sceneId).choices
                assertEquals("${d.id} @ $sceneId offers a different number of answers than the table", d.options.size, choices.size)
                choices.forEach { c -> assertEquals("${d.id} @ $sceneId: ${c.label}", 1, c.consequences.setFlags.count { it in flags }) }
                assertEquals("${d.id} @ $sceneId", flags, choices.flatMap { it.consequences.setFlags }.filter { it in flags }.toSet())
            }
        }
    }

    @Test
    fun `an option's axis is what the story itself rewards, and the quiet options are ungated`() {
        decisions.forEach { d ->
            val choices = scenes.getValue(d.id).flatMap { StoryRepository.node(it).choices }
            d.options.forEach { o ->
                val matching = choices.filter { o.flag in it.consequences.setFlags }
                assertTrue("${d.id}: ${o.flag} is set by no choice", matching.isNotEmpty())
                matching.forEach { c ->
                    val courage = c.consequences.statDeltas[StatType.COURAGE] ?: 0
                    val honour = c.consequences.statDeltas[StatType.HONOUR] ?: 0
                    when (o.axis) {
                        Axis.COURAGE -> assertTrue("${o.flag} should reward Courage only ($courage/$honour)", courage > 0 && honour == 0)
                        Axis.HONOUR -> assertTrue("${o.flag} should reward Honour only ($courage/$honour)", honour > 0 && courage == 0)
                        Axis.STEADFAST, null -> {
                            assertTrue("${o.flag} should reward neither ($courage/$honour)", courage == 0 && honour == 0)
                            assertTrue("${o.flag} should be ungated", c.requirements?.minStats.isNullOrEmpty())
                        }
                    }
                }
            }
        }
    }

    // ---- the rule: fixtures ------------------------------------------------------------------------------------

    private fun pick(vararg picks: Pair<String, Int>): Set<String> =
        picks.map { (id, k) -> decisions.first { it.id == id }.options[k].flag }.toSet()

    @Test
    fun `the three example routes`() {
        assertEquals(Axis.COURAGE, LegacyRule.evaluate(EndingKit.routeA).axis)
        assertEquals(Axis.HONOUR, LegacyRule.evaluate(EndingKit.routeB).axis)
        val c = LegacyRule.evaluate(EndingKit.routeC)
        assertEquals(Axis.STEADFAST, c.axis)
        assertEquals(mapOf(Axis.COURAGE to 1, Axis.HONOUR to 1, Axis.STEADFAST to 8), c.tally)
        assertEquals("all fourteen decisions are recorded on this route", 0, c.undetermined)
    }

    @Test
    fun `postponing your own decisions never makes a Steadfast, but the eight quiet answers do`() {
        val deferralsThenCourage = pick("aid" to 0, "wardens" to 2, "old_debts" to 2, "frame" to 2, "fate" to 2, "sixth_vow" to 0, "commit" to 0,
            "emberlow" to 0, "greymoor" to 0, "duskmere" to 0, "forgotten" to 0, "vigil" to 0, "start" to 0, "final" to 0)
        assertEquals(Axis.COURAGE, LegacyRule.evaluate(deferralsThenCourage).axis)
        val allDeferralsOnly = LegacyRule.evaluate(pick("wardens" to 2, "old_debts" to 2, "frame" to 2, "fate" to 2))
        assertEquals(mapOf(Axis.COURAGE to 0, Axis.HONOUR to 0, Axis.STEADFAST to 0), allDeferralsOnly.tally)
        assertEquals("the four postponing answers are recorded, so only the other ten are undetermined", 10, allDeferralsOnly.undetermined)
        val eightQuietAgainstSixCourage = pick("aid" to 0, "wardens" to 0, "old_debts" to 0, "frame" to 0, "fate" to 0, "start" to 0,
            "sixth_vow" to 2, "commit" to 2, "emberlow" to 2, "greymoor" to 2, "duskmere" to 2, "forgotten" to 2, "vigil" to 2, "final" to 2)
        assertEquals(Axis.STEADFAST, LegacyRule.evaluate(eightQuietAgainstSixCourage).axis)
    }

    @Test
    fun `a contradiction counts as undetermined, and the lead must beat the number of unknowns`() {
        val contradicted = EndingKit.routeA + "waited_at_emberlow"
        val r = LegacyRule.evaluate(contradicted)
        assertEquals(1, r.undetermined)
        assertEquals(Axis.COURAGE, r.axis)
        val narrow = pick("aid" to 0, "wardens" to 0, "old_debts" to 0, "frame" to 0, "fate" to 0, "sixth_vow" to 0, "commit" to 0,
            "emberlow" to 1, "greymoor" to 1, "duskmere" to 1, "forgotten" to 1, "vigil" to 1, "start" to 1)
        val n = LegacyRule.evaluate(narrow)
        assertEquals(1, n.undetermined)
        assertEquals(mapOf(Axis.COURAGE to 7, Axis.HONOUR to 6, Axis.STEADFAST to 0), n.tally)
        assertNull("a 7 to 6 lead with one decision unknown is not proof", n.axis)
        assertNull("the unknown decision contradicted instead of missing", LegacyRule.evaluate(narrow + "demanded_it_be_earned" + "offered_connection_without_merging").axis)
        assertEquals("with the last decision known the lead stands", Axis.COURAGE, LegacyRule.evaluate(narrow + "demanded_it_be_earned").axis)
        val wide = pick("aid" to 0, "wardens" to 0, "old_debts" to 0, "frame" to 0, "fate" to 0, "sixth_vow" to 0, "commit" to 0, "emberlow" to 0,
            "greymoor" to 1, "duskmere" to 1, "forgotten" to 1, "vigil" to 1, "start" to 1)
        assertEquals("8 to 5 with one unknown: lead 3 is more than 1", Axis.COURAGE, LegacyRule.evaluate(wide).axis)
    }

    @Test
    fun `a tie of fully determined decisions goes to the latest tied decision`() {
        val seven = pick("aid" to 0, "wardens" to 0, "old_debts" to 0, "frame" to 0, "fate" to 0, "sixth_vow" to 0, "commit" to 0,
            "emberlow" to 1, "greymoor" to 1, "duskmere" to 1, "forgotten" to 1, "vigil" to 1, "start" to 1, "final" to 1)
        val r = LegacyRule.evaluate(seven)
        assertEquals(mapOf(Axis.COURAGE to 7, Axis.HONOUR to 7, Axis.STEADFAST to 0), r.tally)
        assertEquals(Axis.HONOUR, r.axis)
        assertTrue(r.tieBroken)
        val reversed = pick("aid" to 1, "wardens" to 1, "old_debts" to 1, "frame" to 1, "fate" to 1, "sixth_vow" to 1, "commit" to 1,
            "emberlow" to 0, "greymoor" to 0, "duskmere" to 0, "forgotten" to 0, "vigil" to 0, "start" to 0, "final" to 0)
        assertEquals(Axis.COURAGE, LegacyRule.evaluate(reversed).axis)
    }

    @Test
    fun `sparse, empty and unrelated flag sets give nothing`() {
        val sparse = LegacyRule.evaluate(pick("aid" to 0, "wardens" to 0, "old_debts" to 0, "frame" to 0, "fate" to 0, "sixth_vow" to 0))
        assertEquals(8, sparse.undetermined)
        assertNull(sparse.axis)
        assertNull(LegacyRule.evaluate(emptySet()).axis)
        assertEquals(14, LegacyRule.evaluate(emptySet()).undetermined)
        assertNull(LegacyRule.evaluate(setOf("halvard_died", "c7_rung_trusted", "earned_voss_seal")).axis)
    }

    // ---- the rule: every clean telling ---------------------------------------------------------------------------

    /** The first design (skip undetermined decisions, quorum 9), kept here only to prove the margin rule changes nothing on clean tellings. */
    private fun firstRule(flags: Set<String>, quorum: Int = 9): Axis? {
        val tally = Axis.values().associateWith { 0 }.toMutableMap()
        val seen = ArrayList<Pair<Int, Axis?>>()
        decisions.forEachIndexed { i, d ->
            val held = d.options.filter { it.flag in flags }
            if (held.size != 1) return@forEachIndexed
            seen += i to held.single().axis
            held.single().axis?.let { tally[it] = tally.getValue(it) + 1 }
        }
        if (seen.size < quorum) return null
        val top = tally.values.max()
        val tied = Axis.values().filter { tally.getValue(it) == top }
        if (tied.size == 1) return tied.single()
        return seen.sortedByDescending { it.first }.firstOrNull { it.second in tied }?.second
    }

    @Test
    fun `all 2,125,764 clean tellings - the margin rule equals the first rule and the counts are exact`() {
        val opts = decisions.map { d -> d.options.map { it.flag } }
        val idx = IntArray(decisions.size)
        var total = 0L; var courage = 0L; var honour = 0L; var steadfast = 0L; var none = 0L; var ties = 0L; var differ = 0L
        while (true) {
            val flags = HashSet<String>(32)
            for (i in idx.indices) flags += opts[i][idx[i]]
            val r = LegacyRule.evaluate(flags)
            total++
            when (r.axis) { Axis.COURAGE -> courage++; Axis.HONOUR -> honour++; Axis.STEADFAST -> steadfast++; null -> none++ }
            if (r.tieBroken) ties++
            if (r.undetermined != 0 || r.axis != firstRule(flags)) differ++
            var k = idx.size - 1
            while (k >= 0) { idx[k]++; if (idx[k] < opts[k].size) break; idx[k] = 0; k-- }
            if (k < 0) break
        }
        println("LEGACY clean tellings=$total courage=$courage honour=$honour steadfast=$steadfast none=$none tieBroken=$ties differ=$differ")
        assertEquals(2_125_764L, total)
        assertEquals(0L, differ)
        assertEquals(0L, none)
        assertEquals(984_451L, courage)
        assertEquals(984_451L, honour)
        assertEquals(156_862L, steadfast)
        assertEquals(317_496L, ties)
    }

    // ---- the rule: the saves it must never get wrong ---------------------------------------------------------------

    private fun anyAnswer(random: Random): List<String> = decisions.map { d -> d.options[random.nextInt(d.options.size)].flag }

    /** A save from a build that never reset Chapters 21 to 30: both tellings' late flags are held, the early families are the last telling's. */
    private fun staleAfterReplay(first: List<String>, second: List<String>): Set<String> =
        HashSet<String>().apply { addAll(second.take(5)); addAll(first.drop(5)); addAll(second.drop(5)) }

    @Test
    fun `older replayed saves, late answers random - the margin rule never awards a wrong legacy`() {
        val random = Random(2910)
        val n = 300_000
        var wrong = 0; var none = 0; var right = 0; var firstRuleWrong = 0
        repeat(n) {
            val first = anyAnswer(random)
            val second = anyAnswer(random)
            val want = LegacyRule.evaluate(second.toSet()).axis
            val stale = staleAfterReplay(first, second)
            val got = LegacyRule.evaluate(stale).axis
            if (got == null) none++ else if (got != want) wrong++ else right++
            val old = firstRule(stale)
            if (old != null && old != want) firstRuleWrong++
        }
        println("LEGACY older replayed saves (random): margin none=${100.0 * none / n}% right=${100.0 * right / n}% wrong=$wrong | first rule wrong=${100.0 * firstRuleWrong / n}%")
        assertEquals(0, wrong)
        assertTrue("the comparison should show the first rule was wrong sometimes", firstRuleWrong > 0)
    }

    @Test
    fun `older replayed saves, seven in ten late answers repeated - never wrong`() {
        val random = Random(777)
        val n = 300_000
        var wrong = 0; var none = 0; var right = 0
        repeat(n) {
            val first = anyAnswer(random)
            val second = decisions.mapIndexed { i, d -> if (i >= 5 && random.nextInt(10) < 7) first[i] else d.options[random.nextInt(d.options.size)].flag }
            val want = LegacyRule.evaluate(second.toSet()).axis
            val got = LegacyRule.evaluate(staleAfterReplay(first, second)).axis
            if (got == null) none++ else if (got != want) wrong++ else right++
        }
        println("LEGACY older replayed saves (70% repeated): none=${100.0 * none / n}% right=${100.0 * right / n}% wrong=$wrong")
        assertEquals(0, wrong)
    }

    @Test
    fun `missing decisions - zero to eight removed, never wrong`() {
        val random = Random(4242)
        for (missing in 0..8) {
            var wrong = 0; var none = 0
            val n = 100_000
            repeat(n) {
                val answers = anyAnswer(random)
                val full = LegacyRule.evaluate(answers.toSet()).axis
                val dropped = HashSet<Int>()
                while (dropped.size < missing) dropped += random.nextInt(decisions.size)
                val part = LegacyRule.evaluate(answers.filterIndexed { i, _ -> i !in dropped }.toSet()).axis
                if (part == null) none++ else if (part != full) wrong++
            }
            println("LEGACY missing $missing of 14: no legacy ${100.0 * none / n}% wrong=$wrong")
            assertEquals("missing $missing", 0, wrong)
        }
    }
}
