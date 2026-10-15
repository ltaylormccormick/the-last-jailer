package com.thelastjailer.app.checkpoint

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineageTest {
    @Test
    fun `a generated lineage is 16 lowercase hex characters`() {
        repeat(200) {
            val id = Lineage.generate()
            assertEquals(16, id.length)
            assertTrue(id, Lineage.isWellFormed(id))
        }
    }

    @Test
    fun `the same seed gives the same lineage and different seeds differ`() {
        assertEquals(Lineage.generate(Random(42)), Lineage.generate(Random(42)))
        assertFalse(Lineage.generate(Random(1)) == Lineage.generate(Random(2)))
    }

    @Test
    fun `a thousand lineages from one generator are all distinct`() {
        val random = Random(7)
        val ids = (1..1000).map { Lineage.generate(random) }
        assertEquals(1000, ids.toSet().size)
    }

    @Test
    fun `the well-formed check rejects the wrong length, case and characters`() {
        assertFalse(Lineage.isWellFormed(""))
        assertFalse(Lineage.isWellFormed("0123456789abcde"))
        assertFalse(Lineage.isWellFormed("0123456789abcdef0"))
        assertFalse(Lineage.isWellFormed("0123456789ABCDEF"))
        assertFalse(Lineage.isWellFormed("0123456789abcdeg"))
        assertFalse(Lineage.isWellFormed("0123456789abcde "))
        assertTrue(Lineage.isWellFormed("0123456789abcdef"))
    }

    @Test
    fun `a negative random value still renders as 16 characters`() {
        val random = object : Random() {
            override fun nextLong(): Long = -1L
        }
        assertEquals("ffffffffffffffff", Lineage.generate(random))
        val small = object : Random() {
            override fun nextLong(): Long = 1L
        }
        assertEquals("0000000000000001", Lineage.generate(small))
    }
}
