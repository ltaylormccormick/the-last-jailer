package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitPurchase
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitSlotSave
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Batch O3-B. The slot stamp binds a slot's stored game state to its stored lineage and ledger, in the same
 * editor batch. A build that does not stamp (every build before this one) changes the state and leaves the
 * stamp, so afterwards the timeline is unvouched: history goes missing, it is never trusted. These tests pin
 * that, and that nothing a stamping build does can re-bless a ledger the stamp no longer vouches for.
 *
 * "Old build" below is [oldBuildWrite]: exactly what `SaveStore.save` did before the timeline and stamp existed.
 */
class SlotStampTest {
    private fun distinct(message: String, a: Any?, b: Any?) = assertTrue(message, a != b)

    private val stateKeys = listOf("chapter", "scene", "courage", "honour", "health", "maxHealth", "gold", "level", "xp", "xpToNextLevel", "inventory", "trophies", "flags")

    /** A build from before the stamp: the state fields and active_slot, nothing else (no stamp, no timeline handling). */
    private fun oldBuildWrite(rig: Rig, slot: Int, s: GameState) {
        rig.device.prefs(Story.LIVE).edit()
            .putString("$slot.chapter", s.chapterId).putString("$slot.scene", s.sceneId)
            .putInt("$slot.courage", s.courage).putInt("$slot.honour", s.honour).putInt("$slot.health", s.health)
            .putInt("$slot.maxHealth", s.maxHealth).putInt("$slot.gold", s.gold).putInt("$slot.level", s.level)
            .putInt("$slot.xp", s.xp).putInt("$slot.xpToNextLevel", s.xpToNextLevel)
            .putString("$slot.inventory", s.inventory.joinToString(","))
            .putStringSet("$slot.trophies", s.trophies).putStringSet("$slot.flags", s.flags)
            .putInt("active_slot", slot).commit()
    }

    /** The old build plays the real crossing out of chapter [from] on [slot], with its own tag. */
    private fun oldStep(rig: Rig, from: Int, slot: Int, tag: String): GameState {
        val (node, choice) = Story.crossing(from)
        val next = Story.stateAt(node, slot, tag).applyChoice(choice)
        oldBuildWrite(rig, slot, next)
        return next
    }

    private fun rawTimelineKeys(rig: Rig, slot: Int = 1) =
        rig.liveFile().keys.filter { it == "$slot.lineage" || it == "$slot.arrivals" }

    // ---- the stamp is part of every write, and normal play never loses history -------------------------------------

    @Test
    fun `every kind of save leaves a valid stamp`() {
        val rig = Rig()
        rig.arrive(1)
        assertTrue(rig.saves.stampValid(1))
        rig.arrive(2); rig.arrive(3)
        val live = rig.saves.load(1)!!
        rig.saves.commitPurchase(live.copy(gold = 80), "healing_draught", 5, rig.recorder)
        assertTrue(rig.saves.stampValid(1))
        val (n, c) = Story.staying(4)
        rig.saves.commitChoice(Story.stateAt(n), c, rig.recorder)
        rig.saves.restorePreviousState(1)
        assertTrue("Back", rig.saves.stampValid(1))
        rig.saves.commitSlotSave(1, rig.saves.load(1)!!)
        assertTrue("same-slot SAVE", rig.saves.stampValid(1))
        rig.saves.commitSlotSave(2, rig.saves.load(1)!!)
        assertTrue("SAVE into another slot", rig.saves.stampValid(2) && rig.saves.stampValid(1))
        rig.saves.commitRestart(1, rig.recorder)
        assertTrue("Start Again", rig.saves.stampValid(1))
        rig.saves.save(3, GameState(activeSlot = 3))
        assertTrue("default save", rig.saves.stampValid(3))
    }

    @Test
    fun `ordinary play by a stamping build never loses history`() {
        val rig = Rig()
        rig.playTo(5)
        assertEquals(listOf(2, 3, 4, 5), rig.usable())
        rig.saves.commitSlotSave(1, rig.saves.load(1)!!)
        assertEquals("same-slot SAVE", listOf(2, 3, 4, 5), rig.usable())
        rig.saves.commitPurchase(rig.saves.load(1)!!.copy(gold = 80), "healing_draught", 5, rig.recorder)
        assertEquals("purchase", listOf(2, 3, 4, 5), rig.usable())
        val (n, c) = Story.staying(5)
        rig.saves.commitChoice(Story.stateAt(n), c, rig.recorder)
        assertEquals("a choice inside the chapter", listOf(2, 3, 4, 5), rig.usable())
        rig.saves.restorePreviousState(1)
        assertEquals("Back", listOf(2, 3, 4, 5), rig.usable())
        assertEquals("after a restart of the process", listOf(2, 3, 4, 5), rig.reopen().usable())
    }

    // ---- an older build touches the slot ---------------------------------------------------------------------------

    @Test
    fun `an older build that only plays forward makes history missing but keeps the progress and trophies`() {
        val rig = Rig(); rig.playTo(4)
        val trophies = setOf("T1", "T2")
        oldBuildWrite(rig, 1, rig.saves.load(1)!!.copy(trophies = trophies))
        val s = oldStep(rig, 4, 1, "a").copy(trophies = trophies)
        oldBuildWrite(rig, 1, s)

        val re = rig.reopen()
        assertEquals("history is missing, never wrong", emptyList<Int>(), re.usable())
        assertTrue(re.timeline() == null)
        assertEquals(s, re.saves.load(1))
        assertEquals(trophies, re.saves.load(1)!!.trophies)
    }

    @Test
    fun `an older build Start Again then a different route to the same chapter leaves no stale history`() {
        val rig = Rig(); rig.playTo(4, tag = "a")
        oldBuildWrite(rig, 1, GameState(activeSlot = 1))
        oldStep(rig, 1, 1, "b"); oldStep(rig, 2, 1, "b")
        assertEquals(emptyList<Int>(), rig.reopen().usable())
    }

    @Test
    fun `an older build SAVE over a slot that has history leaves no history for the copied game`() {
        val rig = Rig(); rig.playTo(3, slot = 2, tag = "old")
        oldBuildWrite(rig, 2, Story.stateAt(Story.crossing(3).first, 2, "src"))
        assertEquals(emptyList<Int>(), rig.reopen().usable(2))
    }

    @Test
    fun `an older build re-arriving in the same chapter by another route leaves no stale record`() {
        val rig = Rig(); rig.playTo(3, tag = "a")
        oldStep(rig, 2, 1, "b")
        assertEquals(emptyList<Int>(), rig.reopen().usable())
    }

    // ---- the re-trust paths ----------------------------------------------------------------------------------------

    @Test
    fun `an ordinary Keep save after an older build removes the stale ledger instead of re-stamping it`() {
        val actions = mapOf<String, (Rig) -> Unit>(
            "same-slot SAVE" to { it.saves.commitSlotSave(1, it.saves.load(1)!!) },
            "Back" to { it.saves.restorePreviousState(1) },
            "explicit Keep" to { it.saves.save(1, it.saves.load(1)!!.copy(gold = 1), TimelineWrite.Keep) }
        )
        for ((name, act) in actions) {
            val rig = Rig(); rig.playTo(4, tag = "a")
            val (n, c) = Story.staying(4)
            rig.saves.commitChoice(Story.stateAt(n), c, rig.recorder) // gives Back something to restore
            oldBuildWrite(rig, 1, rig.saves.load(1)!!.copy(gold = 777))
            val re = rig.reopen()

            act(re)

            assertEquals("$name: no timeline keys left", emptyList<String>(), rawTimelineKeys(re))
            assertTrue("$name: the slot is stamped again", re.saves.stampValid(1))
            assertEquals("$name: and the stale ledger did not come back", emptyList<Int>(), re.usable())
            assertEquals("$name: still nothing after another restart", emptyList<Int>(), re.reopen().usable())
        }
    }

    @Test
    fun `an older build changes the state and the new build immediately records another arrival`() {
        val rig = Rig(); rig.playTo(3, tag = "a")
        val oldLineage = rig.timeline()!!.lineage
        oldStep(rig, 3, 1, "a") // the old build is now in chapter 4
        val re = rig.reopen()

        val (_, arrival) = re.arrive(4, tag = "z") // the new build crosses into chapter 5

        val tl = re.timeline()!!
        distinct("a new timeline, not the stale one", oldLineage, tl.lineage)
        assertEquals("only the new arrival is on record", listOf(5), tl.ledger.chapters())
        assertEquals(listOf(5), re.usable())
        assertEquals(arrival.flags, re.usableRecord(5)!!.flags)
        for (stale in 2..4) assertTrue("chapter $stale has no usable record", re.usableRecord(stale) == null)
    }

    @Test
    fun `after invalidation a fresh game records from its first choice on`() {
        val rig = Rig(); rig.playTo(4)
        oldBuildWrite(rig, 1, GameState(activeSlot = 1))
        val re = rig.reopen()
        val fresh = GameState(activeSlot = 1)
        re.saves.commitChoice(fresh, StoryRepository.node(fresh.sceneId).choices.first(), re.recorder)
        assertEquals("the genesis record of the new game", listOf(1), re.usable())
        assertTrue(re.timeline()!!.lineage != rig.timeline()?.lineage)
    }

    @Test
    fun `Back, a purchase or SAVE right after an older build never revives history, but progress is preserved`() {
        val rig = Rig(); rig.playTo(4)
        val modified = rig.saves.load(1)!!.copy(gold = 9)
        oldBuildWrite(rig, 1, modified)
        val re = rig.reopen()
        re.saves.commitPurchase(re.saves.load(1)!!, "healing_draught", 5, re.recorder)
        assertEquals(emptyList<Int>(), re.usable())
        assertEquals("the purchase itself worked", modified.gold - 5, re.saves.load(1)!!.gold)
    }

    // ---- missing, malformed, foreign and replayed stamps -----------------------------------------------------------

    @Test
    fun `a missing, damaged, wrong typed, foreign or replayed stamp is untrusted and never throws`() {
        val rig = Rig(); rig.playTo(3); rig.saves.commitSlotSave(2, rig.saves.load(1)!!)
        val d = rig.device
        val good = d.disk(Story.LIVE)["1.stamp"] as String
        val live = d.prefs(Story.LIVE)

        fun untrusted(what: String) {
            assertFalse(what, rig.saves.stampValid(1))
            assertTrue(what, rig.timeline() == null)
            assertEquals(what, emptyList<Int>(), rig.usable())
        }
        assertEquals(listOf(2, 3), rig.usable())

        live.edit().remove("1.stamp").commit(); untrusted("missing")
        live.edit().putString("1.stamp", "zzz").commit(); untrusted("garbage")
        live.edit().putString("1.stamp", good.reversed()).commit(); untrusted("altered")
        live.edit().putInt("1.stamp", 7).commit(); untrusted("wrong type")
        live.edit().putString("1.stamp", d.disk(Story.LIVE)["2.stamp"] as String).commit(); untrusted("another slot's stamp")
        live.edit().putString("1.stamp", good).commit()
        assertEquals("restoring the exact stamp restores trust", listOf(2, 3), rig.usable())
        live.edit().putString("1.gold", "x").commit(); untrusted("a state field of the wrong type")
    }

    @Test
    fun `a wrong typed timeline key is not the same as an absent one`() {
        // The slot was stamped with NO timeline. A wrong-typed value later appearing under a timeline key must
        // not look like "still absent": the stamp must fail closed, and a Keep save must remove the stray key.
        for (key in listOf("1.lineage", "1.arrivals")) {
            val rig = Rig(withRecorder = false)
            rig.saves.save(1, GameState(activeSlot = 1, gold = 5))
            assertTrue(rig.saves.stampValid(1))
            rig.device.prefs(Story.LIVE).edit().putInt(key, 7).commit()

            assertFalse("$key as an int", rig.saves.stampValid(1))
            assertTrue(rig.timeline() == null)

            rig.saves.save(1, rig.saves.load(1)!!, TimelineWrite.Keep)
            assertFalse("$key: the stray value is removed by the downgraded Keep", rig.liveFile().containsKey(key))
            assertTrue("$key: and the slot is vouched for again", rig.saves.stampValid(1))
        }
        for (key in listOf("1.lineage", "1.arrivals")) {   // other wrong types, and a set where a string belongs
            val rig = Rig(withRecorder = false)
            rig.saves.save(1, GameState(activeSlot = 1))
            rig.device.prefs(Story.LIVE).edit().putStringSet(key, setOf("x")).commit()
            assertFalse("$key as a set", rig.saves.stampValid(1))
            rig.device.prefs(Story.LIVE).edit().remove(key).putBoolean(key, true).commit()
            assertFalse("$key as a boolean", rig.saves.stampValid(1))
            rig.device.prefs(Story.LIVE).edit().remove(key).commit()
            assertTrue("$key genuinely absent is valid again", rig.saves.stampValid(1))
        }
    }

    @Test
    fun `a stamp replayed from an earlier moment does not vouch for a later state`() {
        val rig = Rig(); rig.playTo(3)
        val early = rig.device.disk(Story.LIVE)["1.stamp"]
        rig.arrive(3)
        rig.device.putDiskValue(Story.LIVE, "1.stamp", early)
        assertEquals(emptyList<Int>(), rig.reopen().usable())
    }

    @Test
    fun `an unstamped timeline left by a development build is untrusted, dropped on the next save, progress untouched`() {
        val rig = Rig(); rig.playTo(3)
        rig.saves.save(1, rig.saves.load(1)!!.copy(trophies = setOf("Earned")), TimelineWrite.Keep) // stamped, history intact
        assertEquals(listOf(2, 3), rig.usable())
        // what a development build left: the timeline keys, the state, but no stamp
        rig.device.prefs(Story.LIVE).edit().remove("1.stamp").commit()
        val before = rig.saves.load(1)!!
        val re = rig.reopen()
        assertTrue(re.timeline() == null)
        assertTrue("the timeline keys are still there, just unvouched", re.liveFile().containsKey("1.lineage"))

        re.saves.commitSlotSave(1, re.saves.load(1)!!)

        assertEquals(emptyList<String>(), rawTimelineKeys(re))
        assertEquals(before, re.saves.load(1))
        assertEquals(setOf("Earned"), re.saves.load(1)!!.trophies)
        assertTrue(re.saves.stampValid(1))
    }

    @Test
    fun `a tester slot with no timeline and no stamp is stamped by its first save and records from the next arrival`() {
        val rig = Rig()
        oldBuildWrite(rig, 1, Story.stateAt(Story.crossing(2).first, 1, "t"))
        assertFalse(rig.saves.stampValid(1))
        rig.saves.commitSlotSave(1, rig.saves.load(1)!!)
        assertTrue(rig.saves.stampValid(1))
        assertTrue(rig.timeline() == null)
        val (node, choice) = Story.crossing(2)
        rig.saves.commitChoice(Story.stateAt(node, 1, "t"), choice, rig.recorder)
        assertEquals(listOf(3), rig.usable())
    }

    // ---- limits that are accepted, and checks that stay mandatory --------------------------------------------------

    @Test
    fun `documented limitation - an older build that restores the byte identical stamped state restores trust`() {
        val rig = Rig(); rig.playTo(4)
        val stamped = rig.saves.load(1)!!
        oldBuildWrite(rig, 1, stamped.copy(gold = stamped.gold + 5))
        assertEquals(emptyList<Int>(), rig.reopen().usable())
        oldBuildWrite(rig, 1, stamped)
        assertEquals(listOf(2, 3, 4), rig.reopen().usable())
        oldBuildWrite(rig, 1, stamped.copy(flags = stamped.flags + "x"))
        assertEquals("any difference at all stays untrusted", emptyList<Int>(), rig.reopen().usable())
    }

    @Test
    fun `a valid stamp is an additional gate - the ledger digest and lineage checks still apply`() {
        val rig = Rig(); rig.playTo(3)
        val tl = rig.timeline()!!
        val live = rig.saves.load(1)!!

        rig.saves.save(1, live, TimelineWrite.Set(tl.lineage, tl.ledger.with(3, "0".repeat(CheckpointDigest.HEX_LENGTH))))
        assertTrue("the stamp vouches for this ledger", rig.saves.stampValid(1))
        assertEquals("but chapter 3's record does not match its digest", listOf(2), rig.usable())

        rig.saves.save(1, live, TimelineWrite.Set(Cp.L3, tl.ledger))
        assertTrue(rig.saves.stampValid(1))
        assertEquals("and a lineage the checkpoint file does not carry yields no records", emptyList<Int>(), rig.usable())
    }

    // ---- atomicity and isolation -----------------------------------------------------------------------------------

    @Test
    fun `the stamp adds no write batch, and every crash prefix of an arrival is a consistent triple`() {
        val one = Rig(SimulatedDevice().also { it.asyncApply = true }, withRecorder = false)
        one.saves.save(1, GameState(activeSlot = 1), TimelineWrite.Set(Cp.L1, Ledger.EMPTY.with(1, "a".repeat(CheckpointDigest.HEX_LENGTH))))
        assertEquals("state, timeline keys and stamp are one batch", 1, one.device.queuedApplyCount())

        val probe = Rig(SimulatedDevice().also { it.asyncApply = true }); probe.playTo(3); probe.device.flushApplies()
        probe.arrive(3)
        val batches = probe.device.queuedApplyCount()
        assertTrue(batches >= 1)
        for (prefix in 0..batches) {
            val rig = Rig(SimulatedDevice().also { it.asyncApply = true }); rig.playTo(3); rig.device.flushApplies()
            val before = rig.usable()
            rig.arrive(3)
            rig.device.flushFirstApplies(prefix)
            val re = rig.reopen()
            assertTrue("after $prefix of $batches batches the slot is vouched for", re.saves.stampValid(1))
            val u = re.usable()
            assertTrue("after $prefix of $batches: $u", u == before || u == listOf(2, 3, 4))
        }
    }

    @Test
    fun `reading the timeline or the stamp writes nothing`() {
        val rig = Rig(SimulatedDevice().also { it.asyncApply = true }); rig.playTo(3); rig.device.flushApplies()
        val disk = rig.liveFile()
        repeat(3) { rig.saves.readTimeline(1); rig.saves.stampValid(1); rig.saves.readTimeline(2) }
        assertEquals(0, rig.device.queuedApplyCount())
        assertEquals(disk, rig.liveFile())
    }

    @Test
    fun `a copy gives the destination its own stamp and leaves the source stamp and timeline alone`() {
        val rig = Rig(); rig.playTo(3)
        val sourceStamp = rig.liveFile()["1.stamp"]
        val sourceTimeline = rig.timeline()

        rig.saves.commitSlotSave(2, rig.saves.load(1)!!)

        assertTrue(rig.saves.stampValid(2)); assertTrue(rig.timeline(2) == null)
        assertEquals(sourceStamp, rig.liveFile()["1.stamp"]); assertEquals(sourceTimeline, rig.timeline())
        rig.device.prefs(Story.LIVE).edit().putString("2.stamp", sourceStamp as String).commit()
        assertFalse("a stamp is bound to its slot", rig.saves.stampValid(2))
    }

    @Test
    fun `two slots holding identical games with no history still cannot borrow each other's stamp`() {
        val rig = Rig(withRecorder = false)
        val game = GameState(activeSlot = 1, gold = 42, flags = setOf("f"))
        rig.saves.save(1, game); rig.saves.save(2, game.copy(activeSlot = 2))
        val stamp1 = rig.liveFile()["1.stamp"] as String
        assertTrue(rig.saves.stampValid(1) && rig.saves.stampValid(2))
        assertTrue("each slot has its own stamp", stamp1 != rig.liveFile()["2.stamp"])
        rig.device.prefs(Story.LIVE).edit().putString("2.stamp", stamp1).commit()
        assertFalse(rig.saves.stampValid(2))
    }

    // ---- the stamp function ----------------------------------------------------------------------------------------

    @Test
    fun `the stamp separates every field, type and slot, and ignores set order`() {
        val base = stateKeys.map { it to (if (it == "trophies" || it == "flags") setOf("a", "b") as Any else "v") }
        val s = SlotStamp.of(1, base, "L", "A")
        assertEquals(32, s.length)
        assertEquals("deterministic", s, SlotStamp.of(1, base, "L", "A"))
        assertEquals("set order does not matter", s, SlotStamp.of(1, base.map { if (it.first == "flags") it.first to linkedSetOf("b", "a") else it }, "L", "A"))
        distinct("slot", s, SlotStamp.of(2, base, "L", "A"))
        distinct("lineage", s, SlotStamp.of(1, base, "L2", "A"))
        distinct("ledger", s, SlotStamp.of(1, base, "L", "A2"))
        distinct("absent ledger", s, SlotStamp.of(1, base, "L", null))
        distinct("lineage and ledger are not interchangeable", SlotStamp.of(1, base, "L", "A"), SlotStamp.of(1, base, "A", "L"))
        distinct("int versus text", SlotStamp.of(1, listOf("gold" to 1), null, null), SlotStamp.of(1, listOf("gold" to "1"), null, null))
        distinct("null versus the text null", SlotStamp.of(1, listOf("gold" to null), null, null), SlotStamp.of(1, listOf("gold" to "null"), null, null))
        distinct("a value cannot spill into the next field", SlotStamp.of(1, listOf("a" to "x;b=s1:y", "c" to "z"), null, null), SlotStamp.of(1, listOf("a" to "x", "b" to "y", "c" to "z"), null, null))
        distinct("set members are delimited", SlotStamp.of(1, listOf("f" to setOf("ab", "c")), null, null), SlotStamp.of(1, listOf("f" to setOf("a", "bc")), null, null))
        for (key in stateKeys) {
            val changed = base.map { if (it.first == key) it.first to (if (it.second is Set<*>) setOf("zzz") else "other") else it }
            distinct("field $key is covered", s, SlotStamp.of(1, changed, "L", "A"))
        }
    }

    // ---- guard rails (source scans) --------------------------------------------------------------------------------

    private fun mainSources(): List<File> {
        val root = listOf(File("app/src/main/java"), File("src/main/java")).firstOrNull { it.isDirectory }
            ?: error("cannot find the main sources from ${File(".").absolutePath}")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun codeLines(f: File): List<String> =
        f.readLines().filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }

    @Test
    fun `only SaveStore writes a slot's state, timeline keys or stamp`() {
        val outside = mainSources().filter { !it.path.contains("/checkpoint/") && it.name != "GameState.kt" }
        for (token in listOf("TimelineKeys", "SlotStamp", ".stamp\"", "stampValid(")) {
            val offenders = outside.filter { f -> codeLines(f).any { it.contains(token) } }
            assertTrue("$token is used outside SaveStore: ${offenders.map { it.name }}", offenders.isEmpty())
        }
        val editors = mainSources().filter { !it.path.contains("/checkpoint/") && codeLines(it).any { l -> l.contains(".edit()") } }.map { it.name }.toSet()
        assertEquals("the only files outside the checkpoint package that edit preferences", setOf("GameState.kt", "EntitlementRepository.kt"), editors)
    }

    @Test
    fun `the raw timeline keys are touched only by save, readTimeline and stampValid, and readTimeline checks the stamp first`() {
        val lines = codeLines(mainSources().first { it.name == "GameState.kt" })
        val functions = HashSet<String>()
        var current = ""
        for (line in lines) {
            Regex("\\bfun\\s+(?:[A-Za-z.]+\\.)?(\\w+)\\s*\\(").find(line)?.let { current = it.groupValues[1] }
            if (Regex("TimelineKeys\\.(lineage|arrivals)\\(").containsMatchIn(line)) functions += current
        }
        assertEquals(setOf("save", "readTimeline", "stampValid"), functions)
        val read = lines.joinToString("\n").substringAfter("fun readTimeline").substringBefore("internal fun stampValid")
        assertTrue(read.indexOf("stampValid(slot)") in 0 until read.indexOf("TimelineKeys.lineage"))
        val others = mainSources().filter { it.name != "GameState.kt" && !it.path.contains("/checkpoint/") }
            .flatMap { f -> codeLines(f).filter { Regex("TimelineKeys\\.(lineage|arrivals)\\(").containsMatchIn(it) } }
        assertTrue(others.toString(), others.isEmpty())
    }
}
