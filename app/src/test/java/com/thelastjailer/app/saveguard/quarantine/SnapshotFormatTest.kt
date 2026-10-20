package com.thelastjailer.app.saveguard.quarantine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotFormatTest {
    private val meta = SnapshotMeta("1.2.3", 34, 1, "SUSPECT_FILE", listOf(1, 3), "2026-09-13T10:00:00Z")
    private val hash = "a".repeat(64)
    private val files = listOf(ManifestFile("main.xml", 10, hash), ManifestFile("bak.xml", 0, "b".repeat(64)))

    @Test fun `render then parse round trips`() {
        val m = Manifest.parse(Manifest.render(7, meta, files))!!
        assertEquals(7, m.seq)
        assertEquals(2, m.files.size)
        assertEquals(10L, m.fileNamed("main.xml")!!.length)
        assertNull(m.fileNamed("other"))
    }

    @Test fun `parse rejects every strict violation`() {
        val good = String(Manifest.render(1, meta, files))
        val bad = listOf(
            "", good.trimEnd('\n'), good.replace("tljq-manifest 1", "tljq-manifest 2"), good.replace("seq 1", "seq 01"), good.replace("seq 1", "seq -1"),
            good + "extra line\n", good.replace(hash, "A".repeat(64)), good.replace(hash, "a".repeat(63)), good.replace("file main.xml 10", "file main.xml +10"),
            good.replace("file main.xml 10", "file ../main.xml 10"), good.replace("seq 1\n", "seq 1\nseq 1\n"), good.replace("slots 1,3", "slots 1,,3"),
            good + good, good.replace("\n", "\r\n"), "x".repeat(5000)
        )
        for (b in bad) assertNull("accepted: ${b.take(60)}", Manifest.parse(b.toByteArray()))
        assertNull(Manifest.parse(byteArrayOf(0xFF.toByte(), 0xFE.toByte())))
        assertNotNull(Manifest.parse(good.toByteArray()))
    }

    @Test fun `a manifest with no files is rejected`() {
        val none = String(Manifest.render(1, meta, files)).lines().filterNot { it.startsWith("file ") }.joinToString("\n")
        assertNull(Manifest.parse(none.toByteArray()))
    }

    @Test fun `the marker matches exactly its manifest and nothing else`() {
        val m1 = Manifest.render(1, meta, files)
        val m2 = Manifest.render(2, meta, files)
        val c = CompleteMarker.render(m1)
        assertTrue(CompleteMarker.matches(c, m1))
        assertFalse(CompleteMarker.matches(c, m2))
        for (n in 0 until c.size) assertFalse(CompleteMarker.matches(c.copyOf(n), m1))
        assertFalse(CompleteMarker.matches(c + byteArrayOf(10), m1))
        val flipped = c.copyOf(); flipped[8] = (flipped[8].toInt() xor 1).toByte()
        assertFalse(CompleteMarker.matches(flipped, m1))
        assertTrue(String(c).startsWith("tljq1 "))
        assertTrue(String(c).endsWith("\n"))
    }

    @Test fun `sha256 matches a known vector`() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(ByteArray(0)))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc".toByteArray()))
        assertArrayEquals("tljq1 ".toByteArray(), CompleteMarker.render(ByteArray(0)).copyOf(6))
    }

    @Test fun `no secret content reaches a manifest`() {
        val tricky = SnapshotMeta("v\"<&>", 1, 0, "reason with spaces\nand lines", listOf(), "t")
        val text = String(Manifest.render(1, tricky, files))
        assertFalse(text.contains("<"))
        assertFalse(text.contains("spaces"))
        assertTrue(text.contains("slots -"))
    }
}
