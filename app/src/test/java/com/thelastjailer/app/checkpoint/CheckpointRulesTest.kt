package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointRulesTest {
    private val rules = Cp.rules
    private val base = Cp.record(5)

    private fun reason(record: CheckpointRecord, lineage: String = Cp.L1) = rules.validate(record, lineage)?.reason

    @Test
    fun `a good record is accepted`() {
        assertNull(rules.validate(base, Cp.L1))
    }

    @Test
    fun `an unknown format version is refused`() {
        assertEquals(RejectReason.UNSUPPORTED_VERSION, reason(Cp.rebuilt(base, version = 2)))
        assertEquals(RejectReason.UNSUPPORTED_VERSION, reason(Cp.rebuilt(base, version = 0)))
    }

    @Test
    fun `lineage must be well formed and must match`() {
        assertEquals(RejectReason.MALFORMED_LINEAGE, reason(Cp.rebuilt(base, lineage = "short")))
        assertEquals(RejectReason.MALFORMED_LINEAGE, reason(Cp.rebuilt(base, lineage = "ZZZZZZZZZZZZZZZZ")))
        assertEquals(RejectReason.LINEAGE_MISMATCH, reason(base, Cp.L2))
    }

    @Test
    fun `chapter numbers outside the story are refused`() {
        assertEquals(RejectReason.CHAPTER_OUT_OF_RANGE, reason(Cp.rebuilt(base, chapterNumber = 0, chapterId = "chapter_0")))
        assertEquals(RejectReason.CHAPTER_OUT_OF_RANGE, reason(Cp.rebuilt(base, chapterNumber = 31, chapterId = "chapter_31")))
        assertEquals(RejectReason.CHAPTER_OUT_OF_RANGE, reason(Cp.rebuilt(base, chapterNumber = -1, chapterId = "chapter_-1")))
    }

    @Test
    fun `the chapter id must agree with the chapter number`() {
        assertEquals(RejectReason.CHAPTER_ID_MISMATCH, reason(Cp.rebuilt(base, chapterId = "chapter_6")))
        assertEquals(RejectReason.CHAPTER_ID_MISMATCH, reason(Cp.rebuilt(base, chapterId = "banana")))
    }

    @Test
    fun `the scene must exist and belong to the stated chapter`() {
        assertEquals(RejectReason.SCENE_MISSING, reason(Cp.rebuilt(base, sceneId = "no_such_scene")))
        assertEquals(RejectReason.SCENE_CHAPTER_MISMATCH, reason(Cp.rebuilt(base, sceneId = Cp.startScene(6))))
    }

    @Test
    fun `impossible numbers are refused`() {
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, maxHealth = 0, health = 0)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, health = base.maxHealth + 1)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, health = -1)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, level = 0)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, xp = -5)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, xpToNextLevel = 0)))
        assertEquals(RejectReason.IMPOSSIBLE_VALUE, reason(Cp.rebuilt(base, gold = -1)))
    }

    @Test
    fun `zero health is allowed because the live game can reach it`() {
        assertNull(reason(Cp.rebuilt(base, health = 0)))
    }

    @Test
    fun `negative courage and honour are allowed because choices can lower them`() {
        assertNull(reason(Cp.rebuilt(base, courage = -3, honour = -2)))
    }

    @Test
    fun `blank or separator-bearing item ids are refused`() {
        assertEquals(RejectReason.BAD_INVENTORY, reason(Cp.rebuilt(base, inventory = listOf("ok", ""))))
        assertEquals(RejectReason.BAD_INVENTORY, reason(Cp.rebuilt(base, inventory = listOf("a${INVENTORY_SEPARATOR}b"))))
    }

    @Test
    fun `absurdly large collections are refused`() {
        val manyFlags = (1..CheckpointRules.MAX_FLAGS + 1).map { "f$it" }.toSet()
        assertEquals(RejectReason.TOO_LARGE, reason(Cp.rebuilt(base, flags = manyFlags)))
        val manyItems = (1..CheckpointRules.MAX_INVENTORY + 1).map { "i$it" }
        assertEquals(RejectReason.TOO_LARGE, reason(Cp.rebuilt(base, inventory = manyItems)))
    }

    @Test
    fun `a record whose CRC does not match its content is refused`() {
        assertEquals(RejectReason.CRC_MISMATCH, reason(Cp.withCrc(base, base.crc + 1)))
    }

    @Test
    fun `every real chapter entry scene produces an acceptable record`() {
        // Entry scenes = every scene reached from another chapter, plus each chapter's start scene.
        val entries = HashSet<String>()
        StoryRepository.chapters.forEach { chapter ->
            entries.add(chapter.startNodeId)
            StoryRepository.nodesInChapter(chapter.id).forEach { node ->
                val targets = node.choices.map { it.nextNodeId }.toMutableList()
                node.combatEncounterId?.let {
                    val encounter = com.thelastjailer.app.data.CombatRepository.encounter(it)
                    targets.add(encounter.victoryNodeId)
                    encounter.defeatNodeId?.let { d -> targets.add(d) }
                }
                targets.forEach { id ->
                    val target = StoryRepository.nodeOrNull(id)
                    if (target != null && target.chapterId != node.chapterId) entries.add(id)
                }
            }
        }
        assertTrue("expected 36 entry scenes, found ${entries.size}", entries.size == 36)
        entries.forEach { sceneId ->
            val node = StoryRepository.node(sceneId)
            val number = ChapterIds.numberOf(node.chapterId)!!
            val record = Cp.record(number, state = Cp.state(number, scene = sceneId))
            assertTrue("entry scene $sceneId should be acceptable", rules.validate(record, Cp.L1) == null)
        }
    }
}
