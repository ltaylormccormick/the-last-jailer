package com.thelastjailer.app.checkpoint

import android.content.SharedPreferences
import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.commitChoice
import com.thelastjailer.app.commitPurchase
import com.thelastjailer.app.commitRestart
import com.thelastjailer.app.commitSlotSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The recorder is invisible: nothing it does may break gameplay or the ordinary save, whatever fails, and a
 * checkpoint file from a newer app is never overwritten or hammered. Plus guard rails on how it is wired.
 */
class ArrivalContainmentTest {
    private val ckptFile = Cp.file(1)

    /** The state the ordinary game would reach with no recorder at all. */
    private fun plainOutcome(from: Int): Pair<GameState, GameState> {
        val (node, choice) = Story.crossing(from)
        val before = Story.stateAt(node)
        return before to before.applyChoice(choice)
    }

    private fun assertGameUnaffected(rig: Rig, from: Int) {
        val (before, expected) = plainOutcome(from)
        val (node, choice) = Story.crossing(from)
        val got = rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)
        assertEquals("the transition is exactly what the plain game computes", expected, got)
        val saved = rig.saves.load(1)!!
        assertEquals(expected.sceneId, saved.sceneId)
        assertEquals(expected.chapterId, saved.chapterId)
        assertEquals(expected.gold, saved.gold)
        assertEquals(expected.flags, saved.flags)
        assertTrue("the Back snapshot was still taken", rig.saves.hasPreviousState(1))
        assertEquals(before.sceneId, rig.saves.loadPreviousState(1)!!.sceneId)
        assertEquals(1, rig.saves.currentActiveSlot())
    }

    // ---- exceptions anywhere in the recorder ---------------------------------------------------------------

    @Test
    fun `a digest that throws never disturbs the game`() {
        val rig = Rig(hooks = ArrivalRecorder.Hooks(digest = { _, _ -> throw IllegalStateException("boom") }))
        assertGameUnaffected(rig, 5)
        assertTrue("a plan that cannot be made invalidates", rig.timeline() == null)
    }

    @Test
    fun `a lineage source that throws never disturbs the game`() {
        val rig = Rig(hooks = ArrivalRecorder.Hooks(newLineage = { throw IllegalStateException("boom") }))
        assertGameUnaffected(rig, 5)
        assertNull(rig.timeline())
    }

    @Test
    fun `a record builder that throws never disturbs the game`() {
        val rig = Rig(hooks = ArrivalRecorder.Hooks(buildRecord = { _, _, _ -> throw IllegalStateException("boom") }))
        assertGameUnaffected(rig, 5)
        assertNull(rig.timeline())
    }

    @Test
    fun `an exception raised after the checkpoint committed is contained and leaves a coherent state`() {
        var calls = 0
        val rig = Rig(hooks = ArrivalRecorder.Hooks(digest = { slot, r ->
            calls += 1
            if (calls == 2) throw IllegalStateException("boom after commit") else CheckpointDigest.of(slot, r)
        }))
        assertGameUnaffected(rig, 5)
        assertEquals(listOf(6), rig.timeline()!!.ledger.chapters())
        assertEquals(listOf(6), rig.usable())
    }

    @Test
    fun `an exception in the retry path is contained`() {
        var explode = false
        val device = SimulatedDevice()
        val rig = Rig(device, hooks = ArrivalRecorder.Hooks(digest = { slot, r ->
            if (explode) throw IllegalStateException("boom") else CheckpointDigest.of(slot, r)
        }))
        device.armFault(ckptFile, 1, CommitFault.FALSE_UNCHANGED)
        rig.arrive(5)
        explode = true
        val (node, choice) = Story.staying(6)
        val next = rig.saves.commitChoice(Story.stateAt(node), choice, rig.recorder)
        assertEquals(Story.stateAt(node).applyChoice(choice), next)
        assertEquals(next.sceneId, rig.saves.load(1)!!.sceneId)
    }

    @Test
    fun `a checkpoint store that throws on every access never disturbs the game`() {
        val device = SimulatedDevice()
        val exploding = CheckpointStore(object : CheckpointStorage {
            override val memoryScope: Any = Any()
            override fun fileName(slot: Int) = Cp.file(slot)
            override fun prefs(slot: Int): SharedPreferences = throw IllegalStateException("no storage")
        }, Cp.rules)
        val rig = Rig(device, ckptStore = exploding)
        assertGameUnaffected(rig, 5)
        (6..8).forEach { assertGameUnaffected(rig, it) }
    }

    @Test
    fun `an unreadable checkpoint file never disturbs the game`() {
        val device = SimulatedDevice()
        val rig = Rig(device, ckptStore = Cp.storeOver(device) { slot -> unreadablePrefs(device.prefs(Cp.file(slot))) })
        assertGameUnaffected(rig, 5)
        assertGameUnaffected(rig, 6)
    }

    @Test
    fun `a checkpoint commit that throws never disturbs the game`() {
        val device = SimulatedDevice()
        val rig = Rig(device, ckptStore = Cp.storeOver(device) { slot -> commitThrowsOnce(device.prefs(Cp.file(slot))) })
        assertGameUnaffected(rig, 5)
        assertGameUnaffected(rig, 6)
    }

    @Test
    fun `a damaged checkpoint file and a damaged ledger just mean no checkpoints`() {
        val device = SimulatedDevice()
        device.writeDisk(ckptFile, mapOf("h.version" to "oops", "garbage" to 1))
        device.putDiskValue(Story.LIVE, "1.lineage", Cp.L1)
        device.putDiskValue(Story.LIVE, "1.arrivals", "not a ledger")
        val rig = Rig(device)
        assertGameUnaffected(rig, 5)
        assertTrue(rig.usable().isEmpty() || rig.usable() == listOf(6))
    }

    @Test
    fun `without any recorder the game and its saves work and the timeline is simply invalidated`() {
        val rig = Rig(withRecorder = false)
        assertGameUnaffected(rig, 5)
        assertNull(rig.timeline())
    }

    // ---- the kill switch ---------------------------------------------------------------------------------------

    @Test
    fun `with recording off no checkpoint file is ever created`() {
        val rig = Rig(enabled = false)
        rig.playTo(8)
        rig.saves.commitRestart(1, rig.recorder)
        rig.saves.commitSlotSave(2, rig.saves.load(1)!!)
        assertFalse(rig.device.fileNames().any { it.startsWith("jailer_ckpt") })
        assertNull(rig.timeline())
        assertEquals(1, rig.saves.load(1)!!.activeSlot.coerceAtLeast(1))
    }

    // ---- a newer-format checkpoint file (an app downgrade) -------------------------------------------------------

    private fun newerFile(device: SimulatedDevice) = device.writeDisk(
        ckptFile,
        mapOf("h.version" to CHECKPOINT_FORMAT_VERSION + 1, "h.slot" to 1, "h.lineage" to Cp.L1, "c05.future" to "data", "c05.crc" to 7L)
    )

    @Test
    fun `a newer format file is never overwritten and recording stops for that slot without repeated attempts`() {
        val device = SimulatedDevice()
        newerFile(device)
        val original = device.disk(ckptFile)
        var counting: CountingPrefs? = null
        val rig = Rig(device, ckptStore = Cp.storeOver(device) { slot ->
            CountingPrefs(device.prefs(Cp.file(slot))).also { if (slot == 1) counting = it }
        })

        rig.arrive(4)
        assertTrue(rig.recorder!!.isDisabledForSlot(1))
        assertEquals(original, device.disk(ckptFile))
        val afterFirst = counting!!.touches

        // more arrivals, taps, a re-arrival: the checkpoint file is not even looked at
        rig.arrive(5)
        rig.arrive(6)
        val live = rig.saves.load(1)!!
        rig.saves.commitChoice(live, Story.staying(ChapterIds.numberOf(live.chapterId)!!).second, rig.recorder)
        rig.saves.commitPurchase(live.copy(gold = 50), "healing_draught", 1, rig.recorder)
        assertEquals("no further attempts on the slot", afterFirst, counting!!.touches)
        assertEquals(original, device.disk(ckptFile))

        // the live save and the game carried on, and the ledger is maintained, not left to go stale
        assertEquals(Cp.chapterId(7), rig.saves.load(1)!!.chapterId)
        assertTrue(rig.usable().isEmpty())

        // another slot has its own file and still records
        rig.arrive(4, slot = 2)
        assertEquals(listOf(5), rig.usable(2))
        assertFalse(rig.recorder.isDisabledForSlot(2))
    }

    @Test
    fun `a new process probes a newer format file once and still leaves it untouched`() {
        val device = SimulatedDevice()
        newerFile(device)
        val original = device.disk(ckptFile)
        val first = Rig(device)
        first.arrive(4)
        val second = first.reopen()
        second.arrive(4)
        second.arrive(5)
        assertTrue(second.recorder!!.isDisabledForSlot(1))
        assertEquals(original, device.disk(ckptFile))
        assertEquals("only the newer app's commits, none from this one", 0, device.commitCount(ckptFile))
    }

    @Test
    fun `rule R and the retry also respect a newer format file`() {
        val device = SimulatedDevice()
        val rig = Rig(device)
        device.armFault(ckptFile, 1, CommitFault.FALSE_UNCHANGED)
        rig.arrive(4) // the write fails; a record is pending
        newerFile(device) // and the file turns out to be from a newer app
        val original = device.disk(ckptFile)
        val reopened = rig.reopen()
        val live = reopened.saves.load(1)!!
        reopened.saves.commitChoice(live, Story.staying(5).second, reopened.recorder)
        assertEquals(original, device.disk(ckptFile))
        assertTrue(reopened.recorder!!.isDisabledForSlot(1))
    }

    // ---- everything else in the game is untouched ----------------------------------------------------------------

    @Test
    fun `a full session with the recorder leaves the live save byte for byte what it was before, apart from the timeline keys`() {
        fun session(rig: Rig) {
            rig.arrive(1); rig.arrive(2); rig.arrive(3)
            val (n, c) = Story.staying(4)
            rig.saves.commitChoice(Story.stateAt(n), c, rig.recorder)
            rig.saves.restorePreviousState(1)
            rig.arrive(3, tag = "b")
            val live = rig.saves.load(1)!!
            rig.saves.commitPurchase(live.copy(gold = 80), "healing_draught", 5, rig.recorder)
            rig.saves.commitSlotSave(2, rig.saves.load(1)!!)
            rig.saves.commitChoice(GameState(activeSlot = 3), Choice("x", Cp.startScene(1)), rig.recorder)
            rig.saves.commitRestart(1, rig.recorder)
        }
        val withRec = Rig(SimulatedDevice().also { it.writeDisk(Story.ENTITLEMENTS, mapOf("u" to true)) })
        val without = Rig(SimulatedDevice().also { it.writeDisk(Story.ENTITLEMENTS, mapOf("u" to true)) }, withRecorder = false)
        session(withRec)
        session(without)

        fun stripped(rig: Rig) = rig.liveFile().filterKeys { !it.endsWith(".lineage") && !it.endsWith(".arrivals") }
        assertEquals(stripped(without), stripped(withRec))
        assertEquals(without.device.disk(Story.ENTITLEMENTS), withRec.device.disk(Story.ENTITLEMENTS))
        val previousKeys = withRec.liveFile().keys.filter { it.startsWith("1.previous.") }.map { it.removePrefix("1.previous.") }.toSet()
        assertTrue(previousKeys.all { it in setOf("chapter", "scene", "courage", "honour", "health", "maxHealth", "gold", "level", "xp", "xpToNextLevel", "inventory", "trophies", "flags") })
    }

    // ---- wiring guard rails (source scans) -------------------------------------------------------------------------

    private fun mainSources(): List<File> {
        val roots = listOf(File("app/src/main/java"), File("src/main/java"))
        val root = roots.firstOrNull { it.isDirectory } ?: error("cannot find the main sources from ${File(".").absolutePath}")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun codeLines(f: File): List<String> =
        f.readLines().filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }

    @Test
    fun `nothing in production ever passes discardNewerFormat true or clears a checkpoint file`() {
        val offenders = mainSources().flatMap { f -> codeLines(f).map { it.replace(Regex("\"[^\"]*\""), "\"\"") }.filter { Regex("discardNewerFormat\\s*=\\s*true").containsMatchIn(it) }.map { "${f.name}: $it" } }
        assertTrue("found: $offenders", offenders.isEmpty())
        val recorder = mainSources().first { it.name == "ArrivalRecorder.kt" }
        val text = codeLines(recorder).joinToString("\n")
        assertFalse("the recorder must not mention discarding", text.contains("discardNewerFormat"))
        assertFalse("the recorder never clears a file", Regex("store\\.clear\\(").containsMatchIn(text))
        assertFalse("the recorder never removes records by itself", Regex("store\\.(removeFrom|pruneAbove)\\(").containsMatchIn(text))
    }

    @Test
    fun `there is exactly one app level checkpoint store and it is scoped to the application context`() {
        val outside = mainSources().filter { !it.path.contains("/checkpoint/") }
        fun count(token: String) = outside.sumOf { f -> codeLines(f).count { it.contains(token) } }
        assertEquals(1, count("CheckpointStore("))
        assertEquals(1, count("PrefsCheckpointStorage("))
        assertEquals(1, count("ArrivalRecorder("))
        val main = outside.first { it.name == "MainActivity.kt" }.readText()
        assertTrue(main.contains("val app = context.applicationContext"))
        assertTrue(main.contains("PrefsCheckpointStorage(app)"))
        assertFalse("checkpoint file names come from CheckpointFiles only", outside.any { f -> codeLines(f).any { it.contains("\"jailer_ckpt") } })
    }

    @Test
    fun `only the checkpoint package and the commit helpers know about the new timeline keys`() {
        val offenders = mainSources().filter { f ->
            !f.path.contains("/checkpoint/") && f.name !in setOf("GameState.kt") && codeLines(f).any { it.contains(".lineage\"") || it.contains(".arrivals\"") }
        }
        assertTrue(offenders.map { it.name }.toString(), offenders.isEmpty())
    }
}
