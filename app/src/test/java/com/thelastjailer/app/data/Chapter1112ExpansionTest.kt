package com.thelastjailer.app.data

import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.Chapter6Kit.at
import com.thelastjailer.app.data.Chapter6Kit.node
import com.thelastjailer.app.data.Chapter6Kit.take
import com.thelastjailer.app.data.Chapter6Kit.takeLabel
import com.thelastjailer.app.data.Chapter6Kit.visible
import com.thelastjailer.app.resolveCombat
import java.security.MessageDigest
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapters 11 and 12 as written. Chapter 11 gains the first night at the north gap (Kaelen wears Halvard's chain, a sentry calls
 * him Warden, and Voss answers according to where the two of them stand) and a way across the marsh for a Kaelen who watched
 * the tithe taken. Chapter 12 gains Voss's word at the last dry stone, and the way Kaelen went into the Sanctum in Chapter 9
 * met again. Both chapters keep every shipped scene word for word, and Chapter 12 keeps its routing on trusted_voss_fully.
 */
class Chapter1112ExpansionTest {

    private fun words(text: String) = text.replace(Regex("\\s+"), " ").trim()
    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(words(text).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private val shipped11 = mapOf(
        "the_chain_speaks" to "64694a0c7fc5a22555d2e0ba4235cce824dff10db89d65fc8e370e65c9252cbb",
        "voss_confirms_it_trusted" to "f976ae6ecb5971f1c09635b046229791491827f95a1ffdc2f8b3a197b472522d",
        "voss_confirms_it_doubted" to "806d89e47e0d6d9495e1f963d246117327b6b38760906e9044689ed1103078c9",
        "the_road_to_fenmoor" to "502a38f440498a0b4010b3a4b3c3f48fad50ecf2ffd6ae7e1cf574f61b271ae1",
        "the_drowned_ward" to "ad717f5c35635092292b07c27d961c54ab6bf0e2a73c01fe9533693fa1650d77",
        "the_marsh_holds" to "cd544314c748a0d568eb474567a51a647681504876712a3dcc002091b7ce6c21",
        "fenmoor_extraction_defeat" to "c13f4d46c07bcbfc9a86a61dcca2371116f063af2a1c15ac16915fb2b83c77db",
        "what_thessaly_decides" to "6c25a46d1b03f72a891e9effb5997f6db11030af5f35bb11226534be215ddccc",
        "chapter11_end" to "dfc73b9eacd597e08da6f21da18c2c3bd9e1784a8c75673d530a6c3be6cf655e"
    )

    private val shipped12 = mapOf(
        "word_from_ashfall" to "7370ffa9ea0d9eb468d46227ad59101d67f5908d92e3d13c975f2de7b51ad371",
        "the_call_to_go_eager" to "605d0dc20e39de222d64dca59a6368448599176ab6c05b66a454c55bef4ebafa",
        "the_call_to_go_thorough" to "45e9f286bfa2fb542534649794591aa40cdf7038e6f3e1cd0a1dfbe5912aa94a",
        "back_to_the_ash" to "541ce0e7bc00dc7eead1b5668dc46a86e6f9a8b434c80926c82577ab7f9f9d87",
        "what_the_frame_did" to "8e21573278757cb366a590b8ac0232d858d4f5058500f2c6288df84dd137b7d2",
        "the_unfinished_thing" to "9fc7afbc0b12c8ab56dcdec60a976da11db0bae3da55919e67a98f3ed9f9329f",
        "unfinished_thing_defeat" to "654dbdf7cfcd11ed9fa96d0bb3215ec15c0b2305777390760a41466638d621d0",
        "what_kaelen_does_with_her" to "01d5ee697372d412eda9a5791f1f2d9f3f294abf3cfb754b25b98f39cd5599d5",
        "chapter12_end_helped" to "25cbd9899530adcd0c400d53a29821c24d9763eb946af2c052c517b1a6f5bdb3",
        "watched_from_a_distance" to "873684cf12a94db924ff0841bbb2f2b3f206ff3b9283b2fc3cc00bd28d98242d",
        "word_of_what_happened" to "87eaa760b66e5f1a683d31678eb4674a749dcd55d541ecde99d08a55e62082ef",
        "chapter12_end_refused" to "c81d42339ee9efc1a90320dc4fd12aa43c7a0587379f309874b10a10f2d34115"
    )

    private val rungs = listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted")

    // ---------------------------------------------------------------------------------------------
    // Chapter 11
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the shipped ending of Chapter 10 leads to the weight of the chain, by where Voss and Kaelen stand`() {
        assertEquals(listOf("c11_weight_trusted"), visible(at("chapter10_end", flags = setOf("c7_rung_trusted"))).map { it.nextNodeId })
        assertEquals(listOf("c11_weight_working"), visible(at("chapter10_end", flags = setOf("c7_rung_working"))).map { it.nextNodeId })
        assertEquals(listOf("c11_weight_wary"), visible(at("chapter10_end", flags = setOf("c7_rung_wary"))).map { it.nextNodeId })
        assertEquals("a save with no rung counts as the lowest", listOf("c11_weight_wary"), visible(at("chapter10_end")).map { it.nextNodeId })
    }

    @Test
    fun `the weight of the chain gives each rung its own Voss, and lets Kaelen move it one step by being honest`() {
        val expected = mapOf(
            "c11_weight_wary" to Triple("c7_rung_wary", "Tell her you do not know how it leans.", "c7_rung_working"),
            "c11_weight_working" to Triple("c7_rung_working", "Ask her to show you where it gives.", "c7_rung_trusted")
        )
        expected.forEach { (scene, spec) ->
            val (rung, honest, moved) = spec
            val s = at(scene, flags = setOf(rung))
            assertEquals(scene, 2, visible(s).size)
            val after = takeLabel(s, honest)
            assertEquals(scene, "the_chain_speaks", after.sceneId)
            assertEquals(scene, setOf(moved), after.flags.intersect(C7_RUNG_FAMILY))
        }
        listOf("Tell her you have it in hand." to "c11_weight_wary", "Say you will learn it alone." to "c11_weight_working").forEach { (label, scene) ->
            val rung = if (scene.endsWith("wary")) "c7_rung_wary" else "c7_rung_working"
            val after = takeLabel(at(scene, flags = setOf(rung)), label)
            assertEquals(label, "the_chain_speaks", after.sceneId)
            assertEquals(label, setOf(rung), after.flags.intersect(C7_RUNG_FAMILY))
        }
        // a Voss who trusts him is not asked to move any further
        listOf("Let her take the watch, and sleep.", "Stay on the wall until light.").forEach {
            val after = takeLabel(at("c11_weight_trusted", flags = setOf("c7_rung_trusted")), it)
            assertEquals("the_chain_speaks", after.sceneId)
            assertEquals(setOf("c7_rung_trusted"), after.flags.intersect(C7_RUNG_FAMILY))
        }
    }

    @Test
    fun `the weight of the chain is about the chain, the Warden and the stone, and gives Voss no reason for leaving the Order`() {
        listOf("c11_weight_wary", "c11_weight_working", "c11_weight_trusted").forEach { id ->
            val text = words(node(id).narrativeText)
            assertTrue(id, text.contains("the chain shifts against his throat"))
            assertTrue(id, text.contains("\"Warden,\""))
            assertTrue(id, text.contains("the stone") || text.contains("the wall"))
        }
    }

    @Test
    fun `the road to Fenmoor offers the causeway only to a Kaelen who watched the tithe taken`() {
        assertEquals(listOf("c11_tithe_route"), visible(at("the_road_to_fenmoor", flags = setOf("c8_tithe_seen"))).map { it.nextNodeId })
        assertEquals(listOf("the_drowned_ward"), visible(at("the_road_to_fenmoor")).map { it.nextNodeId })
        assertEquals(listOf("the_drowned_ward", "c11_cut_them_off"), visible(at("c11_tithe_route")).map { it.nextNodeId })
        val cut = at("c11_cut_them_off", courage = 5)
        val after = takeLabel(cut, "Go in.")
        assertEquals("the_drowned_ward", after.sceneId)
        assertEquals(6, after.courage)
    }

    @Test
    fun `the thumbprint of ash is described as the mark the collectors made, so a Kaelen who never saw it is not shown it`() {
        assertTrue(words(node("c11_tithe_route").narrativeText).contains("thumbprint of grey ash"))
        assertTrue(words(node("c11_tithe_route").narrativeText).contains("the mark the collectors made on the Ash Road"))
    }

    @Test
    fun `Chapter 11 has its nine shipped scenes word for word and five new ones`() {
        assertEquals(9, shipped11.size)
        shipped11.forEach { (id, hash) -> assertEquals(id, hash, sha(node(id).narrativeText)) }
        assertEquals(9 + 5, StoryRepository.nodesInChapter("chapter_11").size)
        assertEquals(5, StoryRepository.nodesInChapter("chapter_11").count { it.id.startsWith("c11_") })
    }

    @Test
    fun `Thessaly's three answers, their items and their flags are as shipped`() {
        val s = at("what_thessaly_decides", courage = 99, honour = 99)
        val answers = visible(s)
        assertEquals(3, answers.size)
        answers.forEach {
            assertEquals("chapter11_end", it.nextNodeId)
            assertEquals(listOf("fenmoor_ward_shard"), it.consequences.grantItemIds)
        }
        assertEquals(setOf("pledged_support_to_fenmoor"), answers[0].consequences.setFlags)
        assertEquals(setOf("invited_thessaly_to_stonebeard"), answers[1].consequences.setFlags)
        assertEquals(setOf("left_fenmoor_uncertain"), answers[2].consequences.setFlags)
        assertEquals(listOf("word_from_ashfall"), visible(at("chapter11_end")).map { it.nextNodeId })
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 12
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `at the ash-field gate Voss says what going back costs her, by where she and Kaelen stand`() {
        assertEquals(listOf("c12_voss_trusted"), visible(at("back_to_the_ash", flags = setOf("c7_rung_trusted"))).map { it.nextNodeId })
        assertEquals(listOf("c12_voss_working"), visible(at("back_to_the_ash", flags = setOf("c7_rung_working"))).map { it.nextNodeId })
        assertEquals(listOf("c12_voss_wary"), visible(at("back_to_the_ash", flags = setOf("c7_rung_wary"))).map { it.nextNodeId })
        assertEquals("a save with no rung counts as the lowest", listOf("c12_voss_wary"), visible(at("back_to_the_ash")).map { it.nextNodeId })
    }

    @Test
    fun `the way Kaelen went into the Sanctum in Chapter 9 is met again, and a Kaelen who went another way goes straight on`() {
        // the east stair was open to Kaelen only because Voss, at the top rung, told him where the cells were, and the ladder does not
        // fall after the capture: the stair is met again only at the top, and a lower rung with that flag goes straight on
        assertEquals(listOf("c12_way_stair"), visible(at("c12_voss_trusted", flags = setOf("c9_way_stair"))).map { it.nextNodeId })
        listOf("c12_voss_wary", "c12_voss_working").forEach {
            assertEquals(listOf("what_the_frame_did"), visible(at(it, flags = setOf("c9_way_stair"))).map { c -> c.nextNodeId })
        }
        listOf("c12_voss_wary", "c12_voss_working", "c12_voss_trusted").forEach { id ->
            assertEquals(listOf("c12_way_wall"), visible(at(id, flags = setOf("c9_way_wall"))).map { it.nextNodeId })
            assertEquals(listOf("c12_way_cart"), visible(at(id, flags = setOf("c9_way_cart"))).map { it.nextNodeId })
            assertEquals(listOf("what_the_frame_did"), visible(at(id)).map { it.nextNodeId })
            assertEquals(listOf("what_the_frame_did"), visible(at(id, flags = setOf("created_a_diversion"))).map { it.nextNodeId })
        }
        listOf("c12_way_stair", "c12_way_wall", "c12_way_cart").forEach {
            assertEquals(listOf("what_the_frame_did"), visible(at(it)).map { c -> c.nextNodeId })
        }
    }

    @Test
    fun `Voss's rung does not move anywhere in Chapter 12, whichever way the chapter goes`() {
        val flagsets = listOf(setOf<String>(), setOf("c9_way_stair"), setOf("c9_way_wall"), setOf("c9_way_cart"))
        rungs.forEach { rung ->
            flagsets.forEach { extra ->
                val end = ReplayKit.play(at("word_from_ashfall", flags = setOf(rung) + extra), stop = { it.sceneId.startsWith("the_orders_delegation") })
                assertEquals("$rung $extra", setOf(rung), end.flags.intersect(C7_RUNG_FAMILY))
            }
        }
    }

    @Test
    fun `both endings still route to the delegation by trusted_voss_fully alone, as shipped`() {
        listOf("chapter12_end_helped", "chapter12_end_refused").forEach { id ->
            assertEquals(listOf("the_orders_delegation_trusted"), visible(at(id, flags = setOf("trusted_voss_fully"))).map { it.nextNodeId })
            assertEquals(listOf("the_orders_delegation_guarded"), visible(at(id)).map { it.nextNodeId })
            // the ladder does not decide it: a Voss at the top rung with a guarded stance still meets the guarded delegation
            assertEquals(listOf("the_orders_delegation_guarded"), visible(at(id, flags = setOf("c7_rung_trusted"))).map { it.nextNodeId })
        }
    }

    @Test
    fun `refusing to help Ilsevet does not meet Voss at the ash-field gate and reaches the shipped ending`() {
        val refused = at("the_call_to_go_eager", flags = setOf("trusted_voss_fully"))
        val after = ReplayKit.play(take(refused, "watched_from_a_distance"), stop = { it.sceneId.startsWith("the_orders_delegation") })
        assertEquals("the_orders_delegation_trusted", after.sceneId)
        val trail = ArrayList<String>()
        ReplayKit.play(take(refused, "watched_from_a_distance"), stop = { it.sceneId.startsWith("the_orders_delegation") },
            pick = { n, v -> trail += n.id; v.firstOrNull() })
        assertTrue(trail.none { it.startsWith("c12_") })
    }

    @Test
    fun `Chapter 12 has its twelve shipped scenes word for word and six new ones`() {
        assertEquals(12, shipped12.size)
        shipped12.forEach { (id, hash) -> assertEquals(id, hash, sha(node(id).narrativeText)) }
        assertEquals(12 + 6, StoryRepository.nodesInChapter("chapter_12").size)
        assertEquals(6, StoryRepository.nodesInChapter("chapter_12").count { it.id.startsWith("c12_") })
        assertEquals("the Unfinished fight is untouched", "what_kaelen_does_with_her", CombatRepository.encounter("unfinished_thing_encounter").victoryNodeId)
    }

    // ---------------------------------------------------------------------------------------------
    // Old saves and soak
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a save parked on any scene Chapters 11 or 12 had before they grew plays on`() {
        shipped11.keys.forEach { id ->
            val flags = if (id == "chapter11_end") setOf("left_fenmoor_uncertain") else emptySet()
            val done = ReplayKit.play(at(id, flags = flags), stop = { it.sceneId == "word_from_ashfall" })
            assertEquals(id, "word_from_ashfall", done.sceneId)
        }
        shipped12.keys.forEach { id ->
            val flags = if (id.startsWith("chapter12_end")) setOf("trusted_voss_fully") else emptySet()
            val done = ReplayKit.play(at(id, flags = flags), stop = { it.sceneId.startsWith("the_orders_delegation") })
            assertTrue(id, done.sceneId.startsWith("the_orders_delegation"))
        }
    }

    private fun randomRun(startId: String, random: Random, flags: Set<String>, stop: (GameState) -> Boolean): Pair<List<String>, GameState> {
        var s = at(startId, flags = flags)
        val trail = ArrayList<String>()
        var steps = 0
        while (!stop(s)) {
            check(steps++ < 120) { "never finished; stuck at ${s.sceneId}" }
            val n = StoryRepository.node(s.sceneId)
            trail += n.id
            s = if (n.combatEncounterId != null) {
                s.resolveCombat(CombatRepository.encounter(n.combatEncounterId!!), CombatOutcome(victory = random.nextBoolean(), damageTaken = 0))
            } else {
                val choices = visible(s)
                check(choices.isNotEmpty()) { "dead end at ${n.id} with ${s.flags}" }
                s.applyChoice(choices[random.nextInt(choices.size)])
            }
        }
        return trail to s
    }

    @Test
    fun `fifteen hundred tellings of Chapters 11 and 12 reach Chapter 13, the delegation matches the stance, and nothing loops`() {
        val random = Random(0x1112L)
        val tail = listOf(
            listOf("c7_rung_wary", "c7_rung_working", "c7_rung_trusted", null),
            listOf("c8_tithe_seen", null),
            listOf("c9_way_stair", "c9_way_wall", "c9_way_cart", null),
            listOf("trusted_voss_fully", null),
            listOf("c7_hold_terms", null)
        )
        var chainBeats = 0
        var causeways = 0
        repeat(1_500) { n ->
            val flags = tail.mapNotNull { it[random.nextInt(it.size)] }.toSet() + "halvard_died"
            val (trail, end) = randomRun("chapter10_end", random, flags) { it.sceneId.startsWith("the_orders_delegation") }
            assertEquals("run $n: no scene is read twice", trail.size, trail.toSet().size)
            assertEquals("run $n", if ("trusted_voss_fully" in flags) "the_orders_delegation_trusted" else "the_orders_delegation_guarded", end.sceneId)
            assertEquals("run $n: the chain beat precedes the dream", 1, trail.count { it.startsWith("c11_weight_") })
            assertTrue("run $n", trail.indexOf(trail.first { it.startsWith("c11_weight_") }) < trail.indexOf("the_chain_speaks"))
            assertEquals("run $n: the death flag is untouched", true, "halvard_died" in end.flags)
            chainBeats++
            if ("c11_cut_them_off" in trail) { causeways++; assertTrue("run $n", "c8_tithe_seen" in flags) }
        }
        assertEquals(1_500, chainBeats)
        assertTrue("the causeway was taken in $causeways runs", causeways > 50)
    }
}
