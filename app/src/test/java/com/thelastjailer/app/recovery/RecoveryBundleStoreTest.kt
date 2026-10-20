package com.thelastjailer.app.recovery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Behaviour of the store on a healthy filesystem. Faults and crashes are in the fault-injection tests. */
class RecoveryBundleStoreTest {

    private fun stored(r: PutResult): PutResult.Stored {
        assertTrue("expected Stored but was ${r::class.simpleName}" + reason(r), r is PutResult.Stored)
        return r as PutResult.Stored
    }

    private fun reason(r: PutResult) = when (r) {
        is PutResult.Blocked -> " (${r.reason})"
        is PutResult.Failed -> " (${r.stage})"
        is PutResult.Uncertain -> " (${r.stage})"
        else -> ""
    }

    private fun blocked(r: PutResult, expected: BlockReason) {
        assertTrue("expected Blocked($expected) but was ${r::class.simpleName}" + reason(r), r is PutResult.Blocked && r.reason == expected)
    }

    private fun gen(i: SlotInspection, b: Buffer): Long? = (i.report(b).status as? BufferStatus.Valid)?.bundle?.meta?.generation

    private fun payloadOf(i: SlotInspection, b: Buffer): ByteArray =
        (i.report(b).status as BufferStatus.Valid).bundle.payload

    // ---- basics ----

    @Test
    fun `first write into an empty slot creates the directories and stores generation 1 in buffer A`() {
        val fs = Rb.world(withDirs = false)
        val store = Rb.store(fs)
        val r = stored(store.put(Rb.request(1)))
        assertEquals(Buffer.A, r.buffer)
        assertEquals(1L, r.generation)
        assertTrue("directories and file were synced", r.dirSynced)
        assertNotNull(fs.bytesOf(Rb.A))
        assertNull(fs.bytesOf(Rb.B))
        assertEquals(listOf(1), store.slotsOnDisk())
    }

    @Test
    fun `the first write survives a crash that keeps only what was synced`() {
        // Guards the parent-directory syncs: without them, a freshly created recovery/ or slot_1/ could vanish.
        val fs = Rb.world(withDirs = false)
        stored(Rb.store(fs).put(Rb.request(1)))
        val image = fs.crashImage(NameOutcome.AS_SYNCED, DataOutcome.SYNCED_ONLY)
        val inspection = Rb.store(image).inspect(Rb.SLOT)
        assertEquals("VALID+ABSENT", Rb.kinds(inspection))
    }

    @Test
    fun `the stored payload and metadata read back exactly`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        val req = Rb.request(3, opKind = Rb.RESTORE_SWAP, schema = 77, size = 4096)
        stored(store.put(req))
        val b = (store.inspect(Rb.SLOT).a.status as BufferStatus.Valid).bundle
        assertEquals(Rb.RESTORE_SWAP, b.meta.opKind)
        assertEquals(77, b.meta.payloadSchema)
        assertArrayEquals(req.payload, b.payload)
    }

    @Test
    fun `slots are independent`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        stored(store.put(Rb.request(1, slot = 1)))
        stored(store.put(Rb.request(2, slot = 2)))
        assertEquals(listOf(1, 2), store.slotsOnDisk())
        assertEquals(1L, gen(store.inspect(1), Buffer.A))
        assertEquals(1L, gen(store.inspect(2), Buffer.A))
        assertEquals("VALID+ABSENT", Rb.kinds(store.inspect(2)))
    }

    @Test
    fun `discovery needs no live save and ignores everything that is not a slot directory`() {
        val fs = Rb.world()
        for (d in listOf("slot_3", "slot_12", "slot_x", "slot_0", "slot_01", "slot_", "slots_4", "other")) {
            fs.seedDir("${Rb.ROOT}/$d")
        }
        val found = Rb.store(fs).slotsOnDisk()
        assertEquals(listOf(1, 3, 12), found) // names alone decide; inspect() then classifies what is inside
    }

    // ---- A/B retention across repeated operations ----

    @Test
    fun `repeated resets, overwrites and recovery swaps always keep the previous bundle`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        val kinds = listOf(Rb.START_AGAIN, Rb.OVERWRITE, Rb.RESTORE_SWAP)
        var previousPayload: ByteArray? = null
        for (n in 1..30) {
            val req = Rb.request(n, opKind = kinds[n % 3])
            val r = stored(store.put(req))
            assertEquals(n.toLong(), r.generation)
            val i = store.inspect(Rb.SLOT)
            assertEquals("two valid bundles from the second write on", if (n == 1) 1 else 2, Rb.validCount(i))
            val newest = i.newestValid() as Newest.One
            assertEquals(n.toLong(), (newest.report.status as BufferStatus.Valid).bundle.meta.generation)
            if (previousPayload != null) {
                // The immediately previous state is still on disk in the other buffer.
                assertArrayEquals(previousPayload, payloadOf(i, r.buffer.other()))
            }
            previousPayload = req.payload
        }
    }

    @Test
    fun `buffers alternate A, B, A, B and always replace the older generation`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        val order = (1..6).map { stored(store.put(Rb.request(it))).buffer }
        assertEquals(listOf(Buffer.A, Buffer.B, Buffer.A, Buffer.B, Buffer.A, Buffer.B), order)
    }

    @Test
    fun `a bundle that matches the live state is not special - the older generation is always the one replaced`() {
        // The store has no notion of live state, so it cannot treat any bundle as redundant (review correction 2).
        val fs = Rb.world()
        Rb.seed(fs, Kind.VALID, Kind.VALID, genA = 8, genB = 7)
        val store = Rb.store(fs)
        val r = stored(store.put(Rb.request(50)))
        assertEquals(Buffer.B, r.buffer) // gen 7 replaced
        assertEquals(9L, r.generation)
        assertEquals(8L, gen(store.inspect(Rb.SLOT), Buffer.A)) // gen 8 still there
        val r2 = stored(store.put(Rb.request(51)))
        assertEquals(Buffer.A, r2.buffer) // now gen 8 is the older one
    }

    // ---- identical writes ----

    @Test
    fun `an identical bundle is skipped and nothing on disk changes`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        stored(store.put(Rb.request(1)))
        val before = Rb.disk(fs)
        val r = store.put(Rb.request(1))
        assertTrue(r is PutResult.SkippedIdentical)
        r as PutResult.SkippedIdentical
        assertEquals(Buffer.A, r.buffer)
        assertTrue(r.isNewest)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `skipping an identical older state never evicts the newer distinct state`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        stored(store.put(Rb.request(1))) // gen 1 in A
        stored(store.put(Rb.request(2))) // gen 2 in B
        val before = Rb.disk(fs)
        val r = store.put(Rb.request(1)) // same meaning as the OLDER bundle
        assertTrue(r is PutResult.SkippedIdentical)
        r as PutResult.SkippedIdentical
        assertEquals(Buffer.A, r.buffer)
        assertFalse("it is not the newest generation, and the caller is told so", r.isNewest)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
        assertEquals(2, Rb.validCount(store.inspect(Rb.SLOT)))
    }

    @Test
    fun `the same payload under a different operation or schema is a different bundle`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        stored(store.put(Rb.request(1, opKind = Rb.START_AGAIN, schema = 1)))
        stored(store.put(Rb.request(1, opKind = Rb.OVERWRITE, schema = 1)))
        // Now both buffers hold payload 1. A third write with a new schema is still not a duplicate.
        val r = stored(store.put(Rb.request(1, opKind = Rb.OVERWRITE, schema = 2)))
        assertEquals(3L, r.generation)
    }

    @Test
    fun `the same payload in another slot is written, not skipped`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        stored(store.put(Rb.request(1, slot = 1)))
        stored(store.put(Rb.request(1, slot = 2)))
    }

    // ---- future-version protection ----

    @Test
    fun `a future-version file is never overwritten, deleted or altered, whatever sits beside it`() {
        for (other in listOf(Kind.ABSENT, Kind.CORRUPT, Kind.VALID, Kind.FUTURE)) {
            for (futureInA in listOf(true, false)) {
                val fs = Rb.world()
                val future = Rb.futureBytes(2)
                val otherBytes = when (other) {
                    Kind.ABSENT -> null
                    Kind.CORRUPT -> Rb.corruptBytes(3)
                    Kind.VALID -> Rb.validBytes(3, 5)
                    Kind.FUTURE -> Rb.futureBytes(9, 111)
                }
                val futurePath = if (futureInA) Rb.A else Rb.B
                val otherPath = if (futureInA) Rb.B else Rb.A
                fs.seedFile(futurePath, future)
                if (otherBytes != null) fs.seedFile(otherPath, otherBytes)
                val futureBefore = fs.bytesOf(futurePath)!!
                val otherBefore = fs.bytesOf(otherPath)
                val store = Rb.store(fs)

                val r = store.put(Rb.request(9))

                assertArrayEquals("future file untouched next to $other", futureBefore, fs.bytesOf(futurePath))
                when (other) {
                    Kind.VALID -> {
                        blocked(r, BlockReason.SOLE_KNOWN_GOOD_COPY)
                        assertArrayEquals(otherBefore, fs.bytesOf(otherPath))
                    }
                    Kind.FUTURE -> {
                        blocked(r, BlockReason.NO_WRITABLE_BUFFER)
                        assertArrayEquals(otherBefore, fs.bytesOf(otherPath))
                    }
                    Kind.ABSENT, Kind.CORRUPT -> {
                        // The absent buffer (or the corrupt one, holding nothing recoverable) takes the bundle.
                        val s = stored(r)
                        assertEquals(if (futureInA) Buffer.B else Buffer.A, s.buffer)
                    }
                }
            }
        }
    }

    @Test
    fun `a newer-format file beside an absent buffer lets the absent buffer be used - in both positions`() {
        for (futureInA in listOf(true, false)) {
            val fs = Rb.world()
            fs.seedFile(if (futureInA) Rb.A else Rb.B, Rb.futureBytes())
            val s = stored(Rb.store(fs).put(Rb.request(1)))
            assertEquals(if (futureInA) Buffer.B else Buffer.A, s.buffer)
            assertEquals(1L, s.generation)
            val i = Rb.store(fs).inspect(Rb.SLOT)
            assertEquals(if (futureInA) "FUTURE+VALID" else "VALID+FUTURE", Rb.kinds(i))
        }
    }

    @Test
    fun `a newer-format file larger than the ceiling is still future, and is left alone`() {
        val fs = Rb.world()
        val big = Rb.futureBytes(3, RecoveryFormat.PROVISIONAL_MAX_FILE_BYTES + 5000)
        fs.seedFile(Rb.A, big)
        val i = Rb.store(fs).inspect(Rb.SLOT)
        assertTrue(i.a.status is BufferStatus.Future)
        stored(Rb.store(fs).put(Rb.request(1)))
        assertArrayEquals(big, fs.bytesOf(Rb.A))
    }

    // ---- sole known-good copy ----

    @Test
    fun `the only valid bundle is never replaced when the other buffer is unusable`() {
        val fs = Rb.world()
        Rb.seed(fs, Kind.VALID, Kind.FUTURE)
        val before = Rb.disk(fs)
        blocked(Rb.store(fs).put(Rb.request(9)), BlockReason.SOLE_KNOWN_GOOD_COPY)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `a valid bundle next to a corrupt one is kept and the corrupt buffer is the one replaced`() {
        for (validInA in listOf(true, false)) {
            val fs = Rb.world()
            if (validInA) Rb.seed(fs, Kind.VALID, Kind.CORRUPT) else Rb.seed(fs, Kind.CORRUPT, Kind.VALID)
            val keep = fs.bytesOf(if (validInA) Rb.A else Rb.B)!!
            val s = stored(Rb.store(fs).put(Rb.request(9)))
            assertEquals(if (validInA) Buffer.B else Buffer.A, s.buffer)
            assertArrayEquals(keep, fs.bytesOf(if (validInA) Rb.A else Rb.B))
        }
    }

    // ---- generation edge cases ----

    @Test
    fun `generation numbers never wrap - at the maximum the store refuses`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.validBytes(Long.MAX_VALUE, 1))
        val before = Rb.disk(fs)
        blocked(Rb.store(fs).put(Rb.request(9)), BlockReason.GENERATION_EXHAUSTED)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `the last representable generation can still be written`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.validBytes(Long.MAX_VALUE - 1, 1))
        val s = stored(Rb.store(fs).put(Rb.request(9)))
        assertEquals(Long.MAX_VALUE, s.generation)
        blocked(Rb.store(fs).put(Rb.request(10)), BlockReason.GENERATION_EXHAUSTED)
    }

    @Test
    fun `two different bundles with the same generation are ambiguous - nothing is written and no winner is picked`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.validBytes(5, seed = 1, idByte = 11))
        fs.seedFile(Rb.B, Rb.validBytes(5, seed = 2, idByte = 12))
        val before = Rb.disk(fs)
        val store = Rb.store(fs)
        assertTrue(store.inspect(Rb.SLOT).newestValid() is Newest.Ambiguous)
        blocked(store.put(Rb.request(9)), BlockReason.AMBIGUOUS_GENERATIONS)
        assertTrue(Rb.sameDisk(before, Rb.disk(fs)))
    }

    @Test
    fun `identical copies at the same generation are not ambiguous and the next write replaces B`() {
        val fs = Rb.world()
        val same = Rb.validBytes(5, seed = 1)
        fs.seedFile(Rb.A, same)
        fs.seedFile(Rb.B, same)
        val store = Rb.store(fs)
        assertTrue(store.inspect(Rb.SLOT).newestValid() is Newest.One)
        val s = stored(store.put(Rb.request(9)))
        assertEquals(Buffer.B, s.buffer)
        assertEquals(6L, s.generation)
        assertArrayEquals(same, fs.bytesOf(Rb.A))
    }

    @Test
    fun `a bundle whose generation field is zero or negative is corrupt, so it is replaceable but never trusted`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.raw(generation = 0))
        fs.seedFile(Rb.B, Rb.validBytes(3, 1))
        val i = Rb.store(fs).inspect(Rb.SLOT)
        assertEquals("CORRUPT+VALID", Rb.kinds(i))
        val s = stored(Rb.store(fs).put(Rb.request(9)))
        assertEquals(Buffer.A, s.buffer)
        assertEquals(4L, s.generation)
    }

    // ---- hostile or odd disk contents ----

    @Test
    fun `a bundle copied in from another slot is corrupt here`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.validBytes(3, 1, slot = 2))
        assertTrue((Rb.store(fs).inspect(Rb.SLOT).a.status as BufferStatus.Corrupt).reason == CorruptReason.WRONG_SLOT)
    }

    @Test
    fun `an oversize file is corrupt and is never read in full`() {
        val fs = Rb.world()
        val store = Rb.store(fs, max = 2048)
        fs.seedFile(Rb.A, Rb.raw(payload = ByteArray(5000)))
        assertEquals(CorruptReason.OVERSIZE, (store.inspect(Rb.SLOT).a.status as BufferStatus.Corrupt).reason)
    }

    @Test
    fun `a directory where a bundle should be is unreadable and fails closed`() {
        val fs = Rb.world()
        fs.seedDir(Rb.A)
        fs.seedFile(Rb.B, Rb.validBytes(3, 1))
        val before = Rb.disk(fs)
        val store = Rb.store(fs)
        assertTrue(store.inspect(Rb.SLOT).a.status is BufferStatus.Unreadable)
        blocked(store.put(Rb.request(9)), BlockReason.UNREADABLE_BUFFER)
        assertArrayEquals(before[1], fs.bytesOf(Rb.B))
    }

    @Test
    fun `stale temp files and strangers in the directory are ignored and never promoted`() {
        val fs = Rb.world()
        // A leftover temp file that looks like a perfectly valid, very new bundle.
        fs.seedFile("${Rb.ROOT}/slot_1/a.tmp", Rb.validBytes(99, 1))
        fs.seedFile("${Rb.ROOT}/slot_1/notes.txt", "keep me".toByteArray())
        val store = Rb.store(fs)
        assertEquals("ABSENT+ABSENT", Rb.kinds(store.inspect(Rb.SLOT)))
        val s = stored(store.put(Rb.request(1)))
        assertEquals(1L, s.generation)
        assertEquals("keep me", String(fs.bytesOf("${Rb.ROOT}/slot_1/notes.txt")!!))
    }

    // ---- request validation and limits ----

    @Test
    fun `invalid requests are refused before any disk access`() {
        val fs = Rb.world()
        val store = Rb.store(fs)
        for (bad in listOf(
            BundleRequest(0, 1, 1, ByteArray(1)), BundleRequest(-1, 1, 1, ByteArray(1)),
            BundleRequest(1, -1, 1, ByteArray(1)), BundleRequest(1, 0x10000, 1, ByteArray(1)),
            BundleRequest(1, 1, -1, ByteArray(1)), BundleRequest(1, 1, 0x10000, ByteArray(1))
        )) blocked(store.put(bad), BlockReason.INVALID_REQUEST)
        assertEquals(0, fs.opLog.size)
    }

    @Test
    fun `the payload ceiling is exact`() {
        val fs = Rb.world()
        val store = Rb.store(fs, max = 1024)
        assertEquals(1024 - RecoveryFormat.OVERHEAD_BYTES, store.maxPayloadBytes)
        stored(store.put(Rb.request(1, size = store.maxPayloadBytes)))
        blocked(store.put(Rb.request(2, size = store.maxPayloadBytes + 1)), BlockReason.PAYLOAD_TOO_LARGE)
        assertEquals(1, Rb.validCount(store.inspect(Rb.SLOT)))
    }

    @Test
    fun `an unsupported directory sync is reported, not hidden`() {
        val fs = Rb.world()
        fs.faults.add(SimFault(SimOp.SYNC_DIR, 1, FaultMode.SYNC_DIR_UNSUPPORTED))
        val s = stored(Rb.store(fs).put(Rb.request(1)))
        assertFalse(s.dirSynced)
    }

    @Test
    fun `a confirmed-corrupt buffer is read twice, so one bad read cannot cause an overwrite`() {
        val fs = Rb.world()
        fs.seedFile(Rb.A, Rb.corruptBytes())
        val before = fs.count(SimOp.READ)
        Rb.store(fs).inspect(Rb.SLOT)
        assertEquals("A=2 reads (corrupt, confirmed), B=1 read (absent)", 3, fs.count(SimOp.READ) - before)
    }
}
