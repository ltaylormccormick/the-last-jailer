package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointSizeTest {

    private fun worstCaseState(n: Int): com.thelastjailer.app.GameState {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val flags = nodes.flatMap { node -> node.choices.flatMap { it.consequences.setFlags } }.toSet()
        val items = nodes.flatMap { node -> node.choices.flatMap { it.consequences.grantItemIds } }.distinct()
        return Cp.state(n, inventory = items, trophies = StoryRepository.availableTrophies, flags = flags)
    }

    /**
     * The measured maximum for thirty worst-case checkpoints after the Chapter 6 expansion: 472,493 bytes (about 461 KiB), with
     * the story setting 28 more flags than at 826d719, where the figure sat just under the old 450 KiB bound. It bounds a
     * hypothetical history bundle (G3). qa/G2_RECOVERY_STORE.md records that ceiling as provisional, not accepted; the live
     * save is about 7 KB. Two guards follow: a hard ceiling that only a deliberate decision may move, and a growth marker that
     * fails when the figure rises more than 10% above this measurement so the rise is looked at rather than absorbed.
     */
    private val measuredAfterChapter6 = 472_493
    private val hardCeiling = 600 * 1024

    private fun thirtyWorstCaseBytes(): Int {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        val records = (1..30).map { Cp.record(it, state = worstCaseState(it)) }
        assertTrue(store.replaceAll(1, Cp.L1, records).isCommitted)
        assertEquals(records, store.readAll(1, Cp.L1).records)
        return Cp.xmlSize(device.disk(Cp.file(1)))
    }

    @Test
    fun `thirty worst-case checkpoints stay under the planned ceiling`() {
        val size = thirtyWorstCaseBytes()
        assertTrue("size was $size bytes, hard ceiling $hardCeiling", size < hardCeiling)
    }

    @Test
    fun `worst-case checkpoint growth beyond the Chapter 6 measurement is investigated, not absorbed`() {
        val size = thirtyWorstCaseBytes()
        val allowed = measuredAfterChapter6 / 10 * 11
        assertTrue(
            "Thirty worst-case checkpoints are $size bytes, more than 10% above the $measuredAfterChapter6 measured after Chapter 6 " +
                "(allowed $allowed). Find out what grew (flags, items, trophies) and whether checkpoints need all of it, update " +
                "qa/G2_RECOVERY_STORE.md with the new measurement, and only then change this number.",
            size <= allowed
        )
    }

    @Test
    fun `one worst-case checkpoint is about fourteen kilobytes`() {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        store.replaceAll(1, Cp.L1, listOf(Cp.record(30, state = worstCaseState(30))))
        val size = Cp.xmlSize(device.disk(Cp.file(1)))
        assertTrue("size was $size bytes", size in 8_000..20_000)
    }

    @Test
    fun `the real story's flags, items and trophies all pass validation as a full record`() {
        val record = Cp.record(30, state = worstCaseState(30))
        assertNull(Cp.rules.validate(record, Cp.L1))
        assertTrue(record.flags.size > 200)
    }

    @Test
    fun `the whole cost is bounded by chapters, not by how long the player plays`() {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        repeat(5) { pass -> // replaying chapters rewrites the same keys
            for (n in 1..30) store.upsert(1, Cp.L1, Cp.record(n, state = Cp.state(n, flags = setOf("pass_$pass"))))
        }
        assertEquals(30, store.readAll(1, Cp.L1).records.size)
        val keyCount = device.disk(Cp.file(1)).size
        assertEquals(3 + 30 * 16, keyCount) // header (3) plus 16 keys per chapter
    }
}
