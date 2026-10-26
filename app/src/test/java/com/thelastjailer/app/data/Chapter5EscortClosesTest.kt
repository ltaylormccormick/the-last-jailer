package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Chapter 5, between the envoy's mask slipping and the adept's attack: the escort closes in. The three
 * mask-slips scenes now lead to the_escort_closes, which leads to the unchanged the_marshals_warning and
 * its one fight. This batch also adds a plain band of grey iron to the adept in the warning and two
 * disciplined rearguard to the aftermath, both as insertions only.
 *
 * These tests pin what did not move (the fight, its links, every other scene's prose), what the new scene
 * is and is not (no combat, no flag, no item, no gate), that the two edits are insertions only, and that the
 * ring's altered markings stay a reward of the ground investigation alone.
 *
 * Batch 4 re-pinned the missive and the end scene, which it changed on purpose, and pinned the scenes added
 * since this test was written. A batch that changes a pinned scene re-pins it in the same commit.
 */
class Chapter5EscortClosesTest {

    private val escort = "the_escort_closes"
    private val warning = "the_marshals_warning"
    private val aftermath = "aftermath_of_betrayal"
    private val ground = "what_the_ground_says"
    private val maskSlips = listOf(
        "the_envoys_mask_slips_refused", "the_envoys_mask_slips_heard", "the_envoys_mask_slips_confronted"
    )

    private val chapterNodes = StoryRepository.nodesInChapter("chapter_5")
    private val allNodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
    private val everyChoice: List<Pair<String, Choice>> = allNodes.flatMap { n -> n.choices.map { n.id to it } }
    private val aided = ReplayKit.STRONG.copy(flags = setOf("accepted_dark_aid"))

    private fun node(id: String): StoryNode = StoryRepository.node(id)
    private fun flat(s: String): String = s.replace(Regex("\\s+"), " ").trim()
    private fun words(n: StoryNode): Int = flat(n.narrativeText).split(" ").size
    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    // ---- the new scene and the way into it --------------------------------------------------------------

    @Test
    fun `the three mask-slips scenes keep their label and now lead to the escort scene, and only they do`() {
        maskSlips.forEach { id ->
            val c = node(id).choices.single()
            assertEquals(id, "Stand your ground.", c.label)
            assertEquals(id, escort, c.nextNodeId)
            assertNull(id, c.requirements)
            assertTrue(id, c.consequences.statDeltas.isEmpty() && c.consequences.setFlags.isEmpty())
            assertTrue(id, c.consequences.clearFlags.isEmpty() && c.consequences.grantItemIds.isEmpty())
        }
        val into = everyChoice.filter { (_, c) -> c.nextNodeId == escort }.map { it.first }
        assertEquals(maskSlips.sorted(), into.sorted())
    }

    @Test
    fun `the escort scene is a single beat with no fight, flag, item, gate or stat change`() {
        val n = node(escort)
        assertEquals("chapter_5", n.chapterId)
        assertEquals("The Escort Closes", n.title)
        assertEquals("envoy_reveals_guard", n.illustrationId)
        assertNotNull(IllustrationCatalog.get(n.illustrationId))
        assertNull(n.combatEncounterId)
        val c = n.choices.single()
        assertEquals("Watch the gap.", c.label)
        assertEquals(warning, c.nextNodeId)
        assertNull(c.requirements)
        assertTrue(c.consequences.statDeltas.isEmpty())
        assertTrue(c.consequences.setFlags.isEmpty() && c.consequences.clearFlags.isEmpty())
        assertTrue(c.consequences.grantItemIds.isEmpty())
        assertNull(c.consequences.unlockTrophy)
    }

    @Test
    fun `Halvard's arrival is explicit, so the scene reads the same whether Kaelen met the envoy alone or with him`() {
        assertTrue(flat(node(escort).narrativeText).contains("Halvard comes up beside him, chain wound once round his fist"))
    }

    private fun successors(n: StoryNode): List<String> {
        val viaFight = n.combatEncounterId?.let { id ->
            val e = CombatRepository.encounter(id)
            listOfNotNull(e.victoryNodeId, e.defeatNodeId)
        } ?: emptyList()
        return n.choices.map { it.nextNodeId } + viaFight
    }

    @Test
    fun `no route reaches the warning without passing the escort scene`() {
        val seen = HashSet<String>()
        val pending = ArrayList<String>().apply { add(StoryRepository.chapter("chapter_5")!!.startNodeId) }
        while (pending.isNotEmpty()) {
            val id = pending.removeAt(pending.size - 1)
            if (id == escort || !seen.add(id)) continue
            pending += successors(node(id))
        }
        assertTrue("the mask-slips scenes were never reached", seen.containsAll(maskSlips))
        assertFalse("the warning is reachable without the escort scene", warning in seen)
    }

    // ---- the fight and what follows are unchanged -------------------------------------------------------

    @Test
    fun `the fight, its links and its rewards are unchanged, and the chapter still has exactly one fight`() {
        val fights = chapterNodes.filter { it.combatEncounterId != null }
        assertEquals(listOf(warning), fights.map { it.id })
        assertEquals("cinder_envoy_encounter", fights.single().combatEncounterId)
        assertTrue(fights.single().choices.isEmpty())
        val e = CombatRepository.encounter("cinder_envoy_encounter")
        assertEquals("cinder_adept", e.enemyId)
        assertEquals(aftermath, e.victoryNodeId)
        assertEquals("cinder_envoy_defeat", e.defeatNodeId)
        assertEquals(120, e.xpReward)
        assertEquals(35, e.goldReward)
        assertEquals("The Marshal's Warning", e.unlockTrophy)
        assertEquals("cinder_adept_ambush", node(warning).illustrationId)
        val getUp = node("cinder_envoy_defeat").choices.single()
        assertEquals(aftermath, getUp.nextNodeId)
        assertEquals(setOf("lost_to_cinder_adept"), getUp.consequences.setFlags)
    }

    // ---- the two insertions and the untouched prose -----------------------------------------------------

    private val warningBefore = flat(
        "One of them steps forward alone, cinder-grey robes over something harder underneath, a " +
            "mark on the back of each hand that isn't the Ashen Order's sigil at all. Whatever the " +
            "Cinder Marshal branded onto this one, it wasn't mercy, and it shows in the way he " +
            "moves, like a blade that's already decided how the fight ends.\n\n" +
            "\"The Marshal did warn me you might refuse,\" the adept says, almost fondly, and comes " +
            "on fast."
    )
    private val aftermathBefore = flat(
        "The adept goes down hard, and whatever nerve held the rest of the escort together goes " +
            "with them: they scatter back down the ruined road faster than they arrived, boots " +
            "slipping on wet stone. The envoy himself never drew a blade. He's already gone, horse " +
            "and all, by the time Kaelen gets his breath back.\n\n" +
            "Halvard took a graze along the ribs holding the line beside him, not enough to fell " +
            "him, more than enough to slow him down for a week he doesn't have to spare. Someone " +
            "left something behind in the retreat, though: a dispatch case, half-buried where the " +
            "adept fell."
    )
    private val bandClause = ", and a plain band of grey iron on one finger"
    private val rearguardClause = ", all but two, who fall back in good order as if told to"

    @Test
    fun `the warning and aftermath edits are insertions only`() {
        val warningNow = flat(node(warning).narrativeText)
        assertEquals(1, Regex(Regex.escape(bandClause)).findAll(warningNow).count())
        assertEquals(warningBefore, warningNow.replace(bandClause, ""))
        val aftermathNow = flat(node(aftermath).narrativeText)
        assertEquals(1, Regex(Regex.escape(rearguardClause)).findAll(aftermathNow).count())
        assertEquals(aftermathBefore, aftermathNow.replace(rearguardClause, ""))
    }

    @Test
    fun `the rearguard the aftermath mentions is the rearguard the chase scene meets`() {
        assertTrue(flat(node(aftermath).narrativeText).contains("all but two"))
        assertTrue(flat(node("the_chase_to_the_ford").narrativeText).contains("Two of the escort did not scatter"))
    }

    /** SHA-256 of each scene's whitespace-collapsed prose, as of Batch 4 and its Halvard hub wording fix. The warning, aftermath and escort scenes are checked above. */
    private val pinnedProse = mapOf(
        "the_marshals_envoy" to "0f3f152fc8ab4f5112e2862f0043f6248d2aa61f724a01717dd77f8dbea82877",
        "the_envoys_offer_alone" to "9df63bdbcd4e60009e4af24182744726ab69ee792636ab12ef7529ba695431ca",
        "the_envoys_offer_with_halvard" to "2817891b487bb7f28fa315712adb7276f2c453b80af418d5bdc6120fddbe095a",
        "the_envoys_mask_slips_refused" to "e26219f6483d8f1ab58eefa1f3c1856a265dc1fc31af489656452777b32043f9",
        "the_envoys_mask_slips_heard" to "50b7a905c83b3228fde58076b473cc250d886c9e2f33480895932ae77bf52d4d",
        "the_envoys_mask_slips_confronted" to "5a72d1767471f04e21b63cf6a9f1fcb47d4e22c34ca79bdc337c0b5f6cc22e1b",
        "cinder_envoy_defeat" to "d76a7e411e64afb7c5102b4cafe69d9c5021e3f87f1070e96938b613aeb01856",
        "what_the_missive_says" to "f43c1ef60bad99bf794c6f445b5ee205513c2818be94010006d288f8aa84f3ff",
        "chapter5_end" to "0acfa9cc1d4590fe989dec1a8d52634a1fbd8db1c02aedc478034538ed495298",
        "the_chase_to_the_ford" to "618c2f805b02215f90449f16e361a5ad52d10ab93dd451526ae929fb21c6dfeb",
        "the_ford_caught" to "af6054a523fe4aad2a679ba4247ce904e179637f6dd883d2f4690e19052eeff5",
        "the_ford_lost" to "3cb0b4b24a86a53272be566d8b9e90845ca6435f38a91c47745c010cc059b0b9",
        "halvard_by_the_door" to "a753d7ecdee7373925a98ab912c5e408d53db4e5944a2418d33b2084b8fcf48e",
        "what_the_ground_says" to "19b88d79b419c6de69e75ecddbab4edac78977ea3745dd8b1256f87347008ef9",
        "the_envoy_explains_alone" to "3d5f1ef284d4cb98113176a47be3bcc4397079e7cd8ec7c6bf7acb177cf8a210",
        "the_envoy_explains_with_halvard" to "708fa4dc7c9e82d349599dc74c5fb2e362c47ca0445b9fdac38e08f30a66fee6",
        "halvard_asks" to "eb9523027bf8778d3168c063898514aa394120c3be468eb07b6c634f28863b36",
        "halvard_hears_the_refusal" to "a9af83de6cefe4e1e64e033343aaf8ad7713fc964291688ee05f6ec1195e8e07",
        "halvard_hears_the_listening" to "e12fff93c4711fec83b50794bb1bf5a70c91958e5f1871f6519a5af6e5d6f952",
        "halvard_hears_the_prisoners_words" to "b72d3b2b0dae6fa603b5553f74555819b3f629d27454ec7036b71b581ecfb60b",
        "halvard_gets_only_the_page" to "d3c74f37faecea134607d70ac2fa1b29da685d965be4588f357aba3b1216c048"
    )

    @Test
    fun `every other scene's prose is exactly as pinned`() {
        assertEquals(21, pinnedProse.size)
        pinnedProse.forEach { (id, expected) -> assertEquals(id, expected, sha256(flat(node(id).narrativeText))) }
    }

    // ---- the ring ---------------------------------------------------------------------------------------

    private fun hasWord(text: String, word: String): Boolean =
        Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)

    @Test
    fun `the ring's altered markings are found on the ground route alone`() {
        val altered = chapterNodes.filter {
            val t = flat(it.narrativeText)
            t.contains("scored") || t.contains("struck over")
        }
        assertEquals(listOf(ground), altered.map { it.id })
        val band = chapterNodes.filter { flat(it.narrativeText).contains("band of grey iron") }.map { it.id }
        assertEquals(setOf(warning, ground), band.toSet())
        // The circle of soldiers is never called a ring, so the word still belongs to the iron band.
        assertFalse(hasWord(node(escort).narrativeText, "ring"))
        assertFalse(hasWord(node(warning).narrativeText, "ring"))
    }

    // ---- prose rules, budgets, and the whole path -------------------------------------------------------

    @Test
    fun `the new text follows the prose rules and keeps the Marshal ungendered`() {
        val reserved = Regex(
            "\\b(lad|settings|seventh door|frame|ashwell|archivist|selvane|emberlow|fenmoor|duskmere|ordrun)\\b",
            RegexOption.IGNORE_CASE
        )
        val gendered = Regex("\\b(he|him|his|she|her|hers)\\b", RegexOption.IGNORE_CASE)
        val n = node(escort)
        val newText = flat((listOf(n.title, n.narrativeText) + n.choices.map { it.label }).joinToString("\n")) +
            " " + bandClause + " " + rearguardClause
        assertFalse("a dash used as punctuation", newText.any { it == '—' || it == '–' })
        assertFalse("a sentence fragment starting 'Not'", Regex("(^|[.!?\"] )Not ").containsMatchIn(newText))
        assertNull("a reserved word: ${reserved.find(newText)?.value}", reserved.find(newText))
        newText.split(Regex("(?<=[.!?]\"?) ")).filter { "Marshal" in it }.forEach { sentence ->
            assertFalse("'$sentence'", gendered.containsMatchIn(sentence))
        }
        // The inserted clauses name nobody, so they cannot gender anyone either.
        assertFalse(gendered.containsMatchIn(bandClause + rearguardClause))
    }

    @Test
    fun `the new scene and the two edited scenes stay inside their word budgets`() {
        assertTrue("the escort scene has ${words(node(escort))} words", words(node(escort)) <= 165)
        assertTrue("the warning has ${words(node(warning))} words", words(node(warning)) <= 90)
        assertTrue("the aftermath has ${words(node(aftermath))} words", words(node(aftermath)) <= 125)
        val chapterTotal = chapterNodes.sumOf { words(it) }
        assertTrue("Chapter 5 totals $chapterTotal words, tolerance 3,200 (target 3,000)", chapterTotal <= 3_200)
    }

    private fun approach(flag: String): (StoryNode, List<Choice>) -> Choice? = { n, visible ->
        if (n.id == "the_marshals_envoy") visible.firstOrNull { flag in it.consequences.setFlags } else null
    }

    @Test
    fun `end to end - every approach and answer reaches the warning through the escort scene, then the fight and aftermath`() {
        val encounter = CombatRepository.encounter("cinder_envoy_encounter")
        listOf("met_envoy_alone", "brought_halvard").forEach { start ->
            listOf("refused_envoy", "heard_envoy_offer", "confronted_envoy_with_prisoners_words").forEach { reply ->
                var s = ReplayKit.jumpTo(aided, "the_marshals_envoy")
                val path = ArrayList<String>()
                while (s.sceneId != warning) {
                    val n = node(s.sceneId)
                    path += n.id
                    val visible = StoryRepository.visibleChoices(n, s)
                    val choice = when (n.id) {
                        "the_marshals_envoy" -> approach(start)(n, visible)
                        "the_envoys_offer_alone", "the_envoys_offer_with_halvard" ->
                            visible.firstOrNull { reply in it.consequences.setFlags }
                        else -> null
                    } ?: visible.first()
                    s = s.applyChoice(choice)
                    check(path.size < 20) { "no way to the warning: $path" }
                }
                val label = "$start, $reply: $path"
                assertEquals(label, escort, path.last())
                assertTrue(label, path[path.size - 2] in maskSlips)
                assertEquals(label, 1, path.count { it == escort })
                assertTrue(label, reply in s.flags)
                // The fight itself: victory and defeat both still reach the aftermath.
                val won = s.resolveCombat(encounter, CombatOutcome(victory = true, damageTaken = 0))
                assertEquals(label, aftermath, won.sceneId)
                val lost = s.resolveCombat(encounter, CombatOutcome(victory = false, damageTaken = 0))
                assertEquals(label, "cinder_envoy_defeat", lost.sceneId)
                assertEquals(label, aftermath, lost.applyChoice(node("cinder_envoy_defeat").choices.single()).sceneId)
            }
        }
    }
}
