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

    @Test
    fun `thirty worst-case checkpoints stay under the planned ceiling`() {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        val records = (1..30).map { Cp.record(it, state = worstCaseState(it)) }
        assertTrue(store.replaceAll(1, Cp.L1, records).isCommitted)

        val size = Cp.xmlSize(device.disk(Cp.file(1)))
        assertTrue("size was $size bytes", size < 450 * 1024)
        assertEquals(records, store.readAll(1, Cp.L1).records)
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
