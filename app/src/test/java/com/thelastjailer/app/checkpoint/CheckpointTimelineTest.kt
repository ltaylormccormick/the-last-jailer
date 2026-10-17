package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.commitChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** Ledger parsing, the arrival digest, and the timeline keys on the live save (T1-T5). */
class CheckpointTimelineTest {
    private val d1 = "a".repeat(32)
    private val d2 = "0123456789abcdef0123456789abcdef"
    private val max = 30

    // ---- ledger -------------------------------------------------------------------------------------

    @Test
    fun `an absent or empty ledger value is an empty ledger`() {
        assertTrue(Ledger.parse(null, max)!!.isEmpty)
        assertTrue(Ledger.parse("", max)!!.isEmpty)
    }

    @Test
    fun `a well formed ledger round trips and is ordered`() {
        val ledger = Ledger.EMPTY.with(12, d2).with(3, d1).with(30, d1)
        assertEquals("3:$d1,12:$d2,30:$d1", ledger.encode())
        assertEquals(ledger, Ledger.parse(ledger.encode(), max))
        assertEquals(30 to d1, ledger.latest())
        assertEquals(d2, ledger.digestFor(12))
        assertNull(ledger.digestFor(4))
    }

    @Test
    fun `any malformed ledger value makes the whole ledger malformed`() {
        val bad = listOf(
            "0:$d1", "31:$d1", "1:abc", "1:${d1.uppercase()}", "01:$d1", "100:$d1", "1:$d1,", ",1:$d1",
            "1:$d1,1:$d2", "2:$d1,1:$d2", "1:$d1,,2:$d2", " 1:$d1", "1: $d1", "1:$d1 ", "x:$d1", "1;$d1",
            "1:${d1}0", "1:${d1.dropLast(1)}", "-1:$d1", "1:$d1,31:$d2"
        )
        bad.forEach { assertTrue("should be malformed: '$it'", Ledger.parse(it, max) == null) }
    }

    @Test
    fun `without drops the chapter and everything above and keeps the instance when nothing changes`() {
        val ledger = Ledger.EMPTY.with(2, d1).with(5, d1).with(9, d2)
        assertEquals("2:$d1", ledger.without(5).encode())
        assertEquals("2:$d1,5:$d1", ledger.without(6).encode())
        assertEquals("", ledger.without(1).encode())
        assertSame(ledger, ledger.without(10))
    }

    // ---- digest -------------------------------------------------------------------------------------

    @Test
    fun `the digest is 128 bits of lower case hex and is stable`() {
        val r = Cp.record(7)
        val d = CheckpointDigest.of(1, r)
        assertEquals(32, d.length)
        assertTrue(d.all { it in "0123456789abcdef" })
        assertEquals(d, CheckpointDigest.of(1, r))
        assertEquals(d, CheckpointDigest.of(1, Cp.record(7)))
    }

    @Test
    fun `the digest changes with every field the record holds`() {
        val r = Cp.record(7)
        val base = CheckpointDigest.of(1, r)
        val variants = listOf(
            Cp.rebuilt(r, courage = r.courage + 1), Cp.rebuilt(r, honour = r.honour + 1),
            Cp.rebuilt(r, health = r.health - 1), Cp.rebuilt(r, maxHealth = r.maxHealth + 1),
            Cp.rebuilt(r, gold = r.gold + 1), Cp.rebuilt(r, level = r.level + 1), Cp.rebuilt(r, xp = r.xp + 1),
            Cp.rebuilt(r, xpToNextLevel = r.xpToNextLevel + 1),
            Cp.rebuilt(r, inventory = listOf(r.inventory[1], r.inventory[0], r.inventory[2])), Cp.rebuilt(r, inventory = r.inventory + "proper_helmet"),
            Cp.rebuilt(r, trophies = r.trophies + "Extra"), Cp.rebuilt(r, flags = r.flags + "extra"),
            Cp.rebuilt(r, sceneId = "somewhere_else"), Cp.rebuilt(r, lineage = Cp.L2)
        )
        variants.forEach { assertTrue("digest must change: $it", base != CheckpointDigest.of(1, it)) }
    }

    @Test
    fun `the digest binds the slot and the chapter number`() {
        val r = Cp.record(7)
        assertNotEquals(CheckpointDigest.of(1, r), CheckpointDigest.of(2, r))
        assertNotEquals(CheckpointDigest.of(1, r), CheckpointDigest.of(1, Cp.rebuilt(r, chapterNumber = 8)))
    }

    @Test
    fun `the digest does not depend on the record's own crc field`() {
        val r = Cp.record(7)
        assertEquals(CheckpointDigest.of(1, r), CheckpointDigest.of(1, Cp.withCrc(r, 12345L)))
    }

    @Test
    fun `the digest survives the store round trip for every cross chapter arrival scene in the story`() {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        val edges = Story.allCrossings()
        assertEquals("64 forward edges and 6 loops", 70, edges.size)
        var checked = 0
        for ((_, _, dest) in edges) {
            val n = ChapterIds.numberOf(dest.chapterId)!!
            val r = CheckpointRecord.fromState(Story.stateAt(dest), n, Cp.L1)
            assertTrue(store.replaceAll(1, Cp.L1, listOf(r)).isCommitted)
            val back = store.readAll(1, Cp.L1).records.single()
            assertEquals(CheckpointDigest.of(1, r), CheckpointDigest.of(1, back))
            checked++
        }
        assertEquals(70, checked)
    }

    @Test
    fun `the digest survives the store round trip for randomised states`() {
        val rnd = Random(20261016)
        val items = listOf("healing_draught", "sturdy_buckler", "proper_helmet", "kaelen_token", "rusted_key")
        val device = SimulatedDevice()
        val store = Cp.store(device)
        repeat(200) { i ->
            val n = 1 + rnd.nextInt(30)
            val state = Cp.state(
                n,
                inventory = List(rnd.nextInt(8)) { items[rnd.nextInt(items.size)] },
                trophies = (0 until rnd.nextInt(6)).map { "trophy_${rnd.nextInt(40)}" }.toSet(),
                flags = (0 until rnd.nextInt(12)).map { "flag_${rnd.nextInt(300)}" }.toSet()
            ).copy(honour = rnd.nextInt(30) - 10, courage = rnd.nextInt(20) - 5)
            val r = CheckpointRecord.fromState(state, n, Cp.L1)
            assertTrue("run $i", store.replaceAll(1, Cp.L1, listOf(r)).isCommitted)
            assertEquals("run $i", CheckpointDigest.of(1, r), CheckpointDigest.of(1, store.readAll(1, Cp.L1).records.single()))
            device.restart()
            assertEquals("run $i after restart", CheckpointDigest.of(1, r), CheckpointDigest.of(1, Cp.store(device).readAll(1, Cp.L1).records.single()))
        }
    }

    // ---- timeline keys on the live save -----------------------------------------------------------

    private fun newSaves(): Pair<SimulatedDevice, SaveStore> {
        val device = SimulatedDevice()
        return device to SaveStore(device.prefs(Story.LIVE))
    }

    private val lineage = "0123456789abcdef"

    @Test
    fun `readTimeline is null when nothing is stored and never writes`() {
        val (device, saves) = newSaves()
        assertNull(saves.readTimeline(1))
        assertTrue(device.fileNames().isEmpty())
    }

    @Test
    fun `readTimeline needs a well formed lineage and a well formed ledger`() {
        val device = SimulatedDevice()
        // a fresh process view each time, as after a restart, so what is read is what is on disk
        fun read(): Timeline? = SaveStore(device.prefs(Story.LIVE)).readTimeline(1)
        // This test is about the *format* of the lineage and ledger, so each stored combination carries a valid
        // slot stamp (O3-B) over exactly the text stored; the stamp's own behaviour is in SlotStampTest.
        SaveStore(device.prefs(Story.LIVE)).save(1, GameState(activeSlot = 1), TimelineWrite.Invalidate)
        val stateKeys = listOf("chapter", "scene", "courage", "honour", "health", "maxHealth", "gold", "level", "xp", "xpToNextLevel", "inventory", "trophies", "flags")
        fun put(k: String, v: Any?) {
            device.putDiskValue(Story.LIVE, k, v)
            val d = device.disk(Story.LIVE)
            device.putDiskValue(Story.LIVE, "1.stamp", SlotStamp.of(1, stateKeys.map { it to d["1.$it"] }, d["1.lineage"] as? String, d["1.arrivals"] as? String))
        }
        put("1.lineage", lineage)
        assertEquals(Timeline(lineage, Ledger.EMPTY), read())
        put("1.arrivals", "2:$d1")
        assertEquals(Timeline(lineage, Ledger.EMPTY.with(2, d1)), read())
        put("1.arrivals", "garbage")
        assertTrue("a damaged ledger means no timeline", read() == null)
        put("1.arrivals", "2:$d1")
        put("1.lineage", "not-a-lineage")
        assertTrue(read() == null)
        put("1.lineage", 12345)
        assertTrue("a wrong value type must not throw", read() == null)
        put("1.lineage", lineage)
        put("1.arrivals", 7)
        assertTrue("a wrong value type must not throw", read() == null)
        put("1.arrivals", "2:$d1")
        assertEquals(Timeline(lineage, Ledger.EMPTY.with(2, d1)), read())
        device.putDiskValue(Story.LIVE, "1.arrivals", "2:$d1,3:$d1") // changed without re-stamping: not vouched for
        assertTrue("a timeline the stamp does not vouch for is no timeline", read() == null)
    }

    @Test
    fun `save invalidates the timeline by default and the explicit modes behave`() {
        val (device, saves) = newSaves()
        val s = GameState(activeSlot = 1)
        saves.save(1, s, TimelineWrite.Set(lineage, Ledger.EMPTY.with(1, d1)))
        assertEquals(Timeline(lineage, Ledger.EMPTY.with(1, d1)), saves.readTimeline(1))

        saves.save(1, s.copy(gold = 99), TimelineWrite.Keep)
        assertEquals(Timeline(lineage, Ledger.EMPTY.with(1, d1)), saves.readTimeline(1))

        saves.save(1, s.copy(gold = 98))
        assertTrue("the default save must invalidate", saves.readTimeline(1) == null)
        assertFalse(device.disk(Story.LIVE).containsKey("1.lineage"))
        assertFalse(device.disk(Story.LIVE).containsKey("1.arrivals"))

        saves.save(1, s, TimelineWrite.Set(lineage, Ledger.EMPTY))
        assertEquals(Timeline(lineage, Ledger.EMPTY), saves.readTimeline(1))
        assertFalse("an empty ledger removes the key", device.disk(Story.LIVE).containsKey("1.arrivals"))
    }

    @Test
    fun `timeline keys belong to their own slot`() {
        val (_, saves) = newSaves()
        saves.save(1, GameState(activeSlot = 1), TimelineWrite.Set(lineage, Ledger.EMPTY.with(1, d1)))
        saves.save(2, GameState(activeSlot = 2))
        assertEquals(Timeline(lineage, Ledger.EMPTY.with(1, d1)), saves.readTimeline(1))
        assertNull(saves.readTimeline(2))
    }

    @Test
    fun `the state and its timeline are one batch - after a crash both are old or both are new`() {
        val device = SimulatedDevice().also { it.asyncApply = true }
        val saves = SaveStore(device.prefs(Story.LIVE))
        val old = GameState(activeSlot = 1, gold = 10)
        val new = GameState(activeSlot = 1, gold = 77)
        val tOld = Timeline(lineage, Ledger.EMPTY.with(1, d1))
        val tNew = Timeline("fedcba9876543210", Ledger.EMPTY.with(1, d1).with(2, d2))

        saves.save(1, old, TimelineWrite.Set(tOld.lineage, tOld.ledger))
        device.flushApplies()
        saves.save(1, new, TimelineWrite.Set(tNew.lineage, tNew.ledger))

        // crash before the queued batch reaches disk
        device.restart()
        var again = SaveStore(device.prefs(Story.LIVE))
        assertEquals(old.gold, again.load(1)!!.gold)
        assertEquals(tOld, again.readTimeline(1))

        // same save, but it did reach disk before the crash
        again.save(1, new, TimelineWrite.Set(tNew.lineage, tNew.ledger))
        device.flushApplies()
        device.restart()
        again = SaveStore(device.prefs(Story.LIVE))
        assertEquals(new.gold, again.load(1)!!.gold)
        assertEquals(tNew, again.readTimeline(1))
    }

    // ---- legacy saves (T3) ------------------------------------------------------------------------------

    @Test
    fun `a legacy save loads and plays without ever gaining a timeline key unless it records`() {
        val device = SimulatedDevice()
        val saves = SaveStore(device.prefs(Story.LIVE))
        val (node, choice) = Story.staying(3)
        val state = Story.stateAt(node)
        saves.save(1, state) // as every build before this one wrote it
        val before = device.disk(Story.LIVE)

        // loading, probing and reading the timeline never write
        saves.load(1); saves.hasSave(1); saves.currentActiveSlot(); saves.readTimeline(1)
        device.restart()
        assertEquals(before, device.disk(Story.LIVE))

        // playing with no recorder (or a recorder that records nothing for this move) adds no timeline key
        val rig = Rig(device)
        rig.saves.commitChoice(state, choice, rig.recorder)
        val keys = device.disk(Story.LIVE).keys
        assertFalse(keys.any { it.endsWith(".lineage") || it.endsWith(".arrivals") })
        assertEquals(setOf(Story.LIVE), device.fileNames())
    }
}
