package com.thelastjailer.app.checkpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointRecordTest {

    @Test
    fun `a record round-trips the full game state exactly`() {
        val state = Cp.state(7)
        val record = CheckpointRecord.fromState(state, 7, Cp.L1)
        assertEquals(state.copy(activeSlot = 3), record.toGameState(3))
        assertEquals(7, record.chapterNumber)
        assertEquals(Cp.L1, record.lineage)
        assertTrue(record.hasValidCrc())
    }

    @Test
    fun `a record shares no mutable state with the list and sets it was built from`() {
        val inventory = mutableListOf("healing_draught")
        val flags = mutableSetOf("a")
        val trophies = mutableSetOf("T")
        val record = Cp.record(3, state = Cp.state(3, inventory = inventory, flags = flags, trophies = trophies))

        inventory.add("sturdy_buckler"); flags.add("b"); trophies.add("U")

        assertEquals(listOf("healing_draught"), record.inventory)
        assertEquals(setOf("a"), record.flags)
        assertEquals(setOf("T"), record.trophies)
        assertTrue(record.hasValidCrc())
    }

    @Test
    fun `a record's collections cannot be modified through the record`() {
        val record = Cp.record(3)
        @Suppress("UNCHECKED_CAST")
        assertThrows(UnsupportedOperationException::class.java) { (record.inventory as MutableList<String>).add("x") }
        @Suppress("UNCHECKED_CAST")
        assertThrows(UnsupportedOperationException::class.java) { (record.flags as MutableSet<String>).add("x") }
        @Suppress("UNCHECKED_CAST")
        assertThrows(UnsupportedOperationException::class.java) { (record.trophies as MutableSet<String>).add("x") }
    }

    @Test
    fun `restoring a record gives fresh collections, not the record's own`() {
        val record = Cp.record(4)
        val a = record.toGameState(1)
        val b = record.toGameState(2)
        assertEquals(a.copy(activeSlot = 2), b)
        assertEquals(record.flags, a.flags)
    }

    @Test
    fun `identical content gives an identical CRC regardless of set insertion order`() {
        val one = Cp.record(5, state = Cp.state(5, flags = linkedSetOf("a", "b", "c"), trophies = linkedSetOf("x", "y")))
        val two = Cp.record(5, state = Cp.state(5, flags = linkedSetOf("c", "b", "a"), trophies = linkedSetOf("y", "x")))
        assertEquals(one.crc, two.crc)
        assertEquals(one, two)
    }

    @Test
    fun `inventory order and repeated items are part of the checksum`() {
        val base = Cp.record(5, state = Cp.state(5, inventory = listOf("a_item", "b_item")))
        val reordered = Cp.record(5, state = Cp.state(5, inventory = listOf("b_item", "a_item")))
        val extra = Cp.record(5, state = Cp.state(5, inventory = listOf("a_item", "b_item", "b_item")))
        assertNotEquals(base.crc, reordered.crc)
        assertNotEquals(base.crc, extra.crc)
    }

    @Test
    fun `changing any single field changes the CRC`() {
        val base = Cp.record(6)
        val variants = listOf(
            "version" to Cp.rebuilt(base, version = 2),
            "chapterNumber" to Cp.rebuilt(base, chapterNumber = 7),
            "lineage" to Cp.rebuilt(base, lineage = Cp.L2),
            "chapterId" to Cp.rebuilt(base, chapterId = "chapter_7"),
            "sceneId" to Cp.rebuilt(base, sceneId = "other_scene"),
            "courage" to Cp.rebuilt(base, courage = base.courage + 1),
            "honour" to Cp.rebuilt(base, honour = base.honour + 1),
            "health" to Cp.rebuilt(base, health = base.health - 1),
            "maxHealth" to Cp.rebuilt(base, maxHealth = base.maxHealth + 1),
            "gold" to Cp.rebuilt(base, gold = base.gold + 1),
            "level" to Cp.rebuilt(base, level = base.level + 1),
            "xp" to Cp.rebuilt(base, xp = base.xp + 1),
            "xpToNextLevel" to Cp.rebuilt(base, xpToNextLevel = base.xpToNextLevel + 1),
            "inventory" to Cp.rebuilt(base, inventory = base.inventory + "extra"),
            "trophies" to Cp.rebuilt(base, trophies = base.trophies + "extra"),
            "flags" to Cp.rebuilt(base, flags = base.flags + "extra")
        )
        variants.forEach { (name, variant) -> assertTrue("CRC must change with $name", base.crc != variant.crc) }
        assertEquals(variants.map { it.second.crc }.toSet().size, variants.size)
    }

    @Test
    fun `a forged CRC is detected`() {
        val base = Cp.record(6)
        val forged = Cp.withCrc(base, base.crc xor 1L)
        assertFalse(forged.hasValidCrc())
        assertTrue(base.hasValidCrc())
    }

    @Test
    fun `rebinding to a new lineage gives a valid record and leaves the original alone`() {
        val original = Cp.record(9, Cp.L1)
        val before = original.crc
        val rebound = original.withLineage(Cp.L2)

        assertEquals(Cp.L2, rebound.lineage)
        assertTrue(rebound.hasValidCrc())
        assertNotEquals(original.crc, rebound.crc)
        assertEquals(Cp.L1, original.lineage)
        assertEquals(before, original.crc)
        assertTrue(original.hasValidCrc())
        assertEquals(original.toGameState(1), rebound.toGameState(1))
        assertNull(Cp.rules.validate(rebound, Cp.L2))
        assertEquals(RejectReason.LINEAGE_MISMATCH, Cp.rules.validate(rebound, Cp.L1)?.reason)
    }

    @Test
    fun `chapter ids parse only in the chapter_N form`() {
        assertEquals(1, ChapterIds.numberOf("chapter_1"))
        assertEquals(30, ChapterIds.numberOf("chapter_30"))
        assertNull(ChapterIds.numberOf("chapter_"))
        assertNull(ChapterIds.numberOf("chapter_x"))
        assertNull(ChapterIds.numberOf("Chapter_3"))
        assertNull(ChapterIds.numberOf("chapter_3 "))
        assertNull(ChapterIds.numberOf(""))
    }

    @Test
    fun `a record has no field that could carry a purchase or entitlement`() {
        val names = CheckpointRecord::class.java.declaredFields.map { it.name.lowercase() }
        names.forEach { name ->
            assertFalse("unexpected field $name", "entitle" in name || "purchase" in name || "unlock" in name || "billing" in name)
        }
    }
}
