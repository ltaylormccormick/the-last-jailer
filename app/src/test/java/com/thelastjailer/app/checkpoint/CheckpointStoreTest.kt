package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckpointStoreTest {
    private lateinit var device: SimulatedDevice
    private lateinit var store: CheckpointStore

    @Before
    fun setUp() {
        device = SimulatedDevice()
        store = Cp.store(device)
    }

    private fun chapters(read: CheckpointReadResult) = read.records.map { it.chapterNumber }

    // ---- round trips -----------------------------------------------------------------------

    @Test
    fun `replaceAll then readAll returns the same records in chapter order`() {
        val records = Cp.records(8)
        assertTrue(store.replaceAll(1, Cp.L1, records.reversed()).isCommitted)
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertEquals(records, read.records)
        assertTrue(read.rejected.isEmpty())
        assertEquals(Cp.L1, read.storedLineage)
    }

    @Test
    fun `awkward trophy, flag and item names survive exactly`() {
        val state = Cp.state(
            11,
            inventory = listOf("healing_draught", "healing_draught", "sturdy_buckler"),
            trophies = setOf("Six, Not One", "Warden's \"Vigil\"", "Ünïcode ⚔️", "a;b|c:d"),
            flags = setOf("with space", "semi;colon", "ünï", "equals=sign", "quote\"s")
        )
        val record = Cp.record(11, state = state)
        assertTrue(store.replaceAll(2, Cp.L1, listOf(record)).isCommitted)
        val back = store.readAll(2, Cp.L1).records.single()
        assertEquals(record, back)
        assertEquals(state.copy(activeSlot = 2), back.toGameState(2))
    }

    @Test
    fun `empty sets and an empty inventory round-trip`() {
        val state = Cp.state(1, inventory = emptyList(), trophies = emptySet(), flags = emptySet())
        val record = Cp.record(1, state = state)
        store.replaceAll(1, Cp.L1, listOf(record))
        val back = store.readAll(1, Cp.L1).records.single()
        assertEquals(emptyList<String>(), back.inventory)
        assertEquals(emptySet<String>(), back.flags)
        assertEquals(emptySet<String>(), back.trophies)
        assertEquals(record, back)
    }

    @Test
    fun `an empty history is a valid file with a header and no records`() {
        assertTrue(store.replaceAll(1, Cp.L1, emptyList()).isCommitted)
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertTrue(read.records.isEmpty())
        assertTrue(store.verify(1, Cp.L1, emptyList()))
    }

    @Test
    fun `all thirty chapters round-trip`() {
        val records = Cp.records(30)
        store.replaceAll(1, Cp.L1, records)
        assertEquals(records, store.readAll(1, Cp.L1).records)
    }

    @Test
    fun `repeated items keep their order and count`() {
        val inv = listOf("healing_draught", "sturdy_buckler", "healing_draught", "healing_draught")
        val record = Cp.record(4, state = Cp.state(4, inventory = inv))
        store.replaceAll(1, Cp.L1, listOf(record))
        assertEquals(inv, store.readAll(1, Cp.L1).records.single().inventory)
    }

    // ---- replacing and editing -------------------------------------------------------------

    @Test
    fun `replaceAll leaves no trace of earlier records`() {
        store.replaceAll(1, Cp.L1, Cp.records(6))
        store.replaceAll(1, Cp.L1, Cp.records(2))
        assertEquals(listOf(1, 2), chapters(store.readAll(1, Cp.L1)))
        assertTrue(device.disk(Cp.file(1)).keys.none { it.startsWith("c03.") || it.startsWith("c06.") })
    }

    @Test
    fun `replaceAll can move a file to a new lineage`() {
        store.replaceAll(1, Cp.L1, Cp.records(4))
        store.replaceAll(1, Cp.L2, Cp.records(3, Cp.L2))
        assertEquals(FileState.LINEAGE_MISMATCH, store.readAll(1, Cp.L1).fileState)
        assertEquals(listOf(1, 2, 3), chapters(store.readAll(1, Cp.L2)))
    }

    @Test
    fun `upsert replaces only its own chapter`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        val before = store.readAll(1, Cp.L1).records
        val changed = Cp.record(3, state = Cp.state(3, flags = setOf("changed")))
        assertTrue(store.upsert(1, Cp.L1, changed).isCommitted)
        val after = store.readAll(1, Cp.L1).records
        assertEquals(setOf("changed"), after.first { it.chapterNumber == 3 }.flags)
        assertEquals(before.filter { it.chapterNumber != 3 }, after.filter { it.chapterNumber != 3 })
        assertEquals(5, after.size)
    }

    @Test
    fun `upsert into an empty slot writes the header`() {
        assertTrue(store.upsert(2, Cp.L1, Cp.record(1)).isCommitted)
        val read = store.readAll(2, Cp.L1)
        assertEquals(FileState.OK, read.fileState)
        assertEquals(listOf(1), chapters(read))
        assertEquals(2, device.disk(Cp.file(2))[CheckpointCodec.HEADER_SLOT])
    }

    @Test
    fun `upsert under a different lineage is refused and changes nothing`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        val before = device.disk(Cp.file(1))
        val result = store.upsert(1, Cp.L2, Cp.record(2, Cp.L2))
        assertTrue(result is StoreWriteResult.Rejected)
        assertEquals(before, device.disk(Cp.file(1)))
    }

    @Test
    fun `upsert clears stray keys of the chapter it replaces`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        device.putDiskValue(Cp.file(1), "c02.leftover", "junk")
        device.restart()
        store.upsert(1, Cp.L1, Cp.record(2))
        assertFalse(device.disk(Cp.file(1)).containsKey("c02.leftover"))
    }

    @Test
    fun `removeFrom drops that chapter and everything above`() {
        store.replaceAll(1, Cp.L1, Cp.records(7))
        assertTrue(store.removeFrom(1, Cp.L1, 4).isCommitted)
        assertEquals(listOf(1, 2, 3), chapters(store.readAll(1, Cp.L1)))
        assertTrue(device.disk(Cp.file(1)).keys.none { it.startsWith("c04.") || it.startsWith("c07.") })
    }

    @Test
    fun `pruneAbove keeps the stated chapter`() {
        store.replaceAll(1, Cp.L1, Cp.records(7))
        assertTrue(store.pruneAbove(1, Cp.L1, 4).isCommitted)
        assertEquals(listOf(1, 2, 3, 4), chapters(store.readAll(1, Cp.L1)))
    }

    @Test
    fun `pruning under the wrong lineage is refused and changes nothing`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        val before = device.disk(Cp.file(1))
        assertTrue(store.pruneAbove(1, Cp.L2, 2) is StoreWriteResult.Rejected)
        assertTrue(store.removeFrom(1, Cp.L2, 2) is StoreWriteResult.Rejected)
        assertEquals(before, device.disk(Cp.file(1)))
    }

    @Test
    fun `clear empties the slot`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        assertTrue(store.clear(1).isCommitted)
        assertEquals(FileState.EMPTY, store.readAll(1, Cp.L1).fileState)
        assertTrue(device.disk(Cp.file(1)).isEmpty())
    }

    // ---- header checks ---------------------------------------------------------------------

    @Test
    fun `records under another lineage are never returned`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        val read = store.readAll(1, Cp.L2)
        assertEquals(FileState.LINEAGE_MISMATCH, read.fileState)
        assertTrue(read.records.isEmpty())
        assertEquals(Cp.L1, read.storedLineage)
    }

    @Test
    fun `a file copied from another slot is refused`() {
        store.replaceAll(2, Cp.L1, Cp.records(5))
        device.writeDisk(Cp.file(1), device.disk(Cp.file(2)))
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.SLOT_MISMATCH, read.fileState)
        assertTrue(read.records.isEmpty())
    }

    @Test
    fun `a missing or damaged header yields no records`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        device.removeDiskKey(Cp.file(1), CheckpointCodec.HEADER_LINEAGE)
        assertEquals(FileState.NO_HEADER, store.readAll(1, Cp.L1).fileState)

        store.replaceAll(1, Cp.L1, Cp.records(5))
        device.putDiskValue(Cp.file(1), CheckpointCodec.HEADER_VERSION, "one")
        assertEquals(FileState.NO_HEADER, store.readAll(1, Cp.L1).fileState)
    }

    @Test
    fun `an unknown header version yields no records`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        device.putDiskValue(Cp.file(1), CheckpointCodec.HEADER_VERSION, 99)
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.UNSUPPORTED_VERSION, read.fileState)
        assertTrue(read.records.isEmpty())
    }

    // ---- corruption is local ---------------------------------------------------------------

    @Test
    fun `one bad record is dropped and the others survive`() {
        store.replaceAll(1, Cp.L1, Cp.records(8))
        val file = Cp.file(1)
        device.putDiskValue(file, "c03.crc", "deadbeef")        // bad checksum
        device.putDiskValue(file, "c04.gold", "lots")             // wrong type
        device.removeDiskKey(file, "c05.scene")                   // missing field
        device.putDiskValue(file, "c02.n", 9)                     // group says it holds another chapter
        device.putDiskValue(file, "c06.scene", "no_such_scene")   // scene removed by an update (crc now stale too)
        val read = store.readAll(1, Cp.L1)

        assertEquals(listOf(1, 7, 8), chapters(read))
        val reasons = read.rejected.associate { it.chapterNumber to it.rejection.reason }
        assertEquals(RejectReason.CRC_MISMATCH, reasons[3])
        assertEquals(RejectReason.WRONG_TYPE, reasons[4])
        assertEquals(RejectReason.MISSING_FIELD, reasons[5])
        assertEquals(RejectReason.KEY_MISMATCH, reasons[2])
        assertNotNull(reasons[6])
        assertEquals(5, read.rejected.size)
    }

    @Test
    fun `a record edited on disk without fixing its CRC is refused`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        device.putDiskValue(Cp.file(1), "c02.gold", 999_999)
        val read = store.readAll(1, Cp.L1)
        assertEquals(listOf(1, 3), chapters(read))
        assertEquals(RejectReason.CRC_MISMATCH, read.rejected.single().rejection.reason)
    }

    @Test
    fun `a wiped file reads as empty and never throws`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        device.wipeDisk(Cp.file(1))
        val read = store.readAll(1, Cp.L1)
        assertEquals(FileState.EMPTY, read.fileState)
        assertTrue(read.records.isEmpty())
    }

    @Test
    fun `a file that cannot be read yields an unreadable result and never throws`() {
        store.replaceAll(1, Cp.L1, Cp.records(5))
        val broken = CheckpointStore({ unreadablePrefs(device.prefs(Cp.file(it))) }, Cp.rules)
        val read = broken.readAll(1, Cp.L1)
        assertEquals(FileState.UNREADABLE, read.fileState)
        assertTrue(read.records.isEmpty())
    }

    @Test
    fun `unknown extra keys inside the file do not disturb reading`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        device.putDiskValue(Cp.file(1), "something.else", "future")
        device.putDiskValue(Cp.file(1), "c02.futureField", 7)
        val read = store.readAll(1, Cp.L1)
        assertEquals(listOf(1, 2, 3), chapters(read))
    }

    // ---- validation cannot damage anything -------------------------------------------------

    @Test
    fun `invalid input is rejected before any commit and the file is untouched`() {
        store.replaceAll(1, Cp.L1, Cp.records(4))
        val before = device.disk(Cp.file(1))
        val commits = device.commitCount(Cp.file(1))

        val bad = Cp.withCrc(Cp.record(2), 123L)
        assertTrue(store.replaceAll(1, Cp.L1, listOf(bad)) is StoreWriteResult.Rejected)
        assertTrue(store.upsert(1, Cp.L1, bad) is StoreWriteResult.Rejected)
        assertTrue(store.replaceAll(1, Cp.L1, listOf(Cp.record(2, Cp.L2))) is StoreWriteResult.Rejected)
        assertTrue(store.replaceAll(1, "not-a-lineage", Cp.records(2)) is StoreWriteResult.Rejected)
        assertTrue(store.replaceAll(1, Cp.L1, listOf(Cp.record(2), Cp.record(2))) is StoreWriteResult.Rejected)
        assertTrue(store.replaceAll(1, Cp.L1, listOf(Cp.rebuilt(Cp.record(2), sceneId = "gone"))) is StoreWriteResult.Rejected)

        assertEquals(before, device.disk(Cp.file(1)))
        assertEquals(commits, device.commitCount(Cp.file(1)))
    }

    @Test
    fun `a slot below one is a programming error`() {
        var threw = false
        try { store.readAll(0, Cp.L1) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    // ---- isolation -------------------------------------------------------------------------

    @Test
    fun `operations on one slot never change another slot, the live save or entitlements`() {
        // Fixtures: three checkpoint slots, a real live save with a Back snapshot, and an entitlement file.
        store.replaceAll(1, Cp.L1, Cp.records(3, Cp.L1))
        store.replaceAll(2, Cp.L2, Cp.records(5, Cp.L2))
        store.replaceAll(3, Cp.L3, Cp.records(7, Cp.L3))
        val saves = SaveStore(device.prefs("jailer_saves"))
        saves.save(2, GameState(activeSlot = 2, chapterId = "chapter_5", sceneId = Cp.startScene(5), gold = 99))
        saves.savePreviousState(2, GameState(activeSlot = 2, sceneId = Cp.startScene(4)))
        device.prefs("jailer_entitlements").edit().putBoolean("unlocked_full_story", true).commit()

        val one = device.disk(Cp.file(1))
        val three = device.disk(Cp.file(3))
        val savesDisk = device.disk("jailer_saves")
        val entitlements = device.disk("jailer_entitlements")

        store.upsert(2, Cp.L2, Cp.record(4, Cp.L2, Cp.state(4, flags = setOf("x"))))
        store.removeFrom(2, Cp.L2, 3)
        store.pruneAbove(2, Cp.L2, 1)
        store.replaceAll(2, Cp.L3, Cp.records(2, Cp.L3))
        store.clear(2)
        device.armFault(Cp.file(2), 1, CommitFault.FALSE_MEMORY_MUTATED)
        store.replaceAll(2, Cp.L1, Cp.records(2))

        assertEquals(one, device.disk(Cp.file(1)))
        assertEquals(three, device.disk(Cp.file(3)))
        assertEquals(savesDisk, device.disk("jailer_saves"))
        assertEquals(entitlements, device.disk("jailer_entitlements"))
        assertEquals(99, SaveStore(device.prefs("jailer_saves")).load(2)!!.gold)
        assertTrue(SaveStore(device.prefs("jailer_saves")).hasPreviousState(2))
    }

    @Test
    fun `the store only ever touches its own per-slot files`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        store.upsert(2, Cp.L2, Cp.record(1, Cp.L2))
        store.removeFrom(1, Cp.L1, 2)
        store.clear(2)
        assertEquals(setOf(Cp.file(1), Cp.file(2)), device.fileNames())
    }

    @Test
    fun `reads do not write`() {
        store.replaceAll(1, Cp.L1, Cp.records(3))
        val commits = device.commitCount(Cp.file(1))
        val disk = device.disk(Cp.file(1))
        store.readAll(1, Cp.L1); store.readAll(1, Cp.L2); store.storedLineage(1); store.verify(1, Cp.L1, Cp.records(3))
        assertEquals(commits, device.commitCount(Cp.file(1)))
        assertEquals(disk, device.disk(Cp.file(1)))
    }

    // ---- verification ----------------------------------------------------------------------

    @Test
    fun `verify is true for what was written and false for anything else`() {
        val records = Cp.records(4)
        store.replaceAll(1, Cp.L1, records)
        assertTrue(store.verify(1, Cp.L1, records))
        assertFalse(store.verify(1, Cp.L1, Cp.records(3)))
        assertFalse(store.verify(1, Cp.L2, records))
        device.putDiskValue(Cp.file(1), "c02.gold", 5)
        assertFalse(Cp.store(device).verify(1, Cp.L1, records))
    }

    @Test
    fun `a record reading back from disk after a restart equals what was written`() {
        val records = Cp.records(12)
        store.replaceAll(1, Cp.L1, records)
        device.restart()
        assertEquals(records, Cp.store(device).readAll(1, Cp.L1).records)
    }
}
