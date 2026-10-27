package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.checkpoint.ReachableCheckpointMeasure.Sample
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reachable-state checkpoint measurement (Option B in qa/CHECKPOINT_GROWTH_INVESTIGATION.md).
 *
 * It sits beside [CheckpointSizeTest], which is unchanged: that test puts every flag the story can set into each of thirty
 * checkpoints and keeps its own 10% growth warning and the 600 KiB hard ceiling. This one measures what a play can hold, by
 * walking the real story ([ReachableCheckpointMeasure]), and keeps its own 10% warning. Neither replaces the other:
 *
 *  * the worst case is the bound no play can exceed and the figure a history bundle (G3) must be able to carry;
 *  * the reachable figure is what a play does hold, and it moves when a play can hold more, not when a flag is added to a
 *    family a play only ever holds one member of.
 *
 * The figure is the size of thirty arrival records, one per chapter, each the largest found for its chapter over the sample
 * (a bound on any single play found by the sample, since no walk's record for a chapter was larger). The largest history a
 * single walk produced is reported beside it. The sample is deterministic: the seed, the walk counts and the policies are
 * pinned here.
 */
class CheckpointReachableSizeTest {

    /**
     * The reachable figure after Chapter 9, as measured on 8be2d98 with the seed and walk counts below: 161,198 bytes, about
     * 30% of the 530,891-byte worst case. The sample reached all 30 chapters and all 326 settable flags; the most flags it saw
     * held at once were 139 (137 in one arrival record), and the largest single walk's history was 160,911 bytes. A rise of more
     * than 10% is looked at, not absorbed. Because the sample is seeded, a change to the story's graph changes the walks as
     * well as the content, so a move of a few percent in either direction is noise; ten is not.
     */
    private val reachableAfterChapter9 = 161_198
    private val hardCeiling = 600 * 1024

    companion object {
        const val SEED = 0x10C5L
        const val UNIFORM_WALKS = 3_000
        const val COLLECTOR_WALKS = 3_000

        val sample: Sample by lazy { ReachableCheckpointMeasure.sample(SEED, UNIFORM_WALKS, COLLECTOR_WALKS) }

        val composed: Int by lazy { ReachableCheckpointMeasure.historyBytes(sample.largestArrival.toSortedMap().values.toList()) }
        val singleWalk: Int by lazy { ReachableCheckpointMeasure.historyBytes(sample.largestHistory) }
    }

    private fun worstCaseBytes(): Int {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val flags = nodes.flatMap { node -> node.choices.flatMap { it.consequences.setFlags } }.toSet()
        val items = nodes.flatMap { node -> node.choices.flatMap { it.consequences.grantItemIds } }.distinct()
        val device = SimulatedDevice()
        val store = Cp.store(device)
        val records = (1..30).map {
            Cp.record(it, state = Cp.state(it, inventory = items, trophies = StoryRepository.availableTrophies, flags = flags))
        }
        assertTrue(store.replaceAll(1, Cp.L1, records).isCommitted)
        return Cp.xmlSize(device.disk(Cp.file(1)))
    }

    @Test
    fun `report the reachable figures beside the worst case`() {
        val s = sample
        val settable = ReachableCheckpointMeasure.settableFlags()
        val worst = worstCaseBytes()
        println("REACHABLE seed=0x${SEED.toString(16)} walks=${s.walks} (uniform $UNIFORM_WALKS, collector $COLLECTOR_WALKS) steps=${s.steps}")
        println("REACHABLE chapters reached=${s.chaptersReached.size} odd jumps=${s.oddJumps} distinct flags seen=${s.distinctFlagsSeen.size} of ${settable.size} settable")
        println("REACHABLE most flags at once (any step)=${s.maxFlagsAtOnce}, in one arrival record=${s.maxFlagsAtArrival}")
        println("REACHABLE (a) worst case, thirty checkpoints with every settable flag, item and trophy = $worst bytes")
        println("REACHABLE (b) thirty arrival records, each the largest found for its chapter = $composed bytes (${100 * composed / worst}% of the worst case)")
        println("REACHABLE (c) the largest single walk's history = $singleWalk bytes (${s.largestHistory.size} records)")
        for ((n, r) in s.largestArrival.toSortedMap()) {
            println("REACHABLE chapter $n: ${r.flags.size} flags, ${r.trophies.size} trophies, ${r.inventory.size} items, scene ${r.sceneId}, stored text ${ReachableCheckpointMeasure.weight(r)} bytes")
        }
    }

    @Test
    fun `the sample reaches every chapter and sees nearly every flag the story can set`() {
        val s = sample
        assertEquals((1..30).toSet(), s.chaptersReached)
        assertEquals("a forward jump of more than one chapter would make the recorder invalidate the timeline", 0, s.oddJumps)
        val settable = ReachableCheckpointMeasure.settableFlags()
        val seen = s.distinctFlagsSeen.intersect(settable).size
        assertTrue("the sample saw only $seen of ${settable.size} settable flags", seen * 100 >= settable.size * 95)
    }

    @Test
    fun `what a play can hold is below the worst case and far below the hard ceiling`() {
        val worst = worstCaseBytes()
        assertTrue("reachable $composed bytes is not below the worst case $worst", composed < worst)
        assertTrue("the largest single walk ($singleWalk) is not within the per-chapter bound ($composed) plus rounding", singleWalk <= composed + 600)
        assertTrue("reachable $composed bytes is not under the hard ceiling $hardCeiling", composed < hardCeiling)
    }

    @Test
    fun `reachable checkpoint growth beyond the Chapter 9 measurement is investigated, not absorbed`() {
        val allowed = reachableAfterChapter9 / 10 * 11
        assertTrue(
            "Thirty reachable arrival records are $composed bytes, more than 10% above the $reachableAfterChapter9 measured after Chapter 9 " +
                "(allowed $allowed). A play can now hold more than it could. Find out which chapter's records grew and which flags, " +
                "update qa/G2_RECOVERY_STORE.md and qa/CHECKPOINT_GROWTH_INVESTIGATION.md with the new measurement, and only then change this number.",
            composed <= allowed
        )
    }

    @Test
    fun `the measurement is repeatable`() {
        val a = ReachableCheckpointMeasure.sample(SEED, 40, 40)
        val b = ReachableCheckpointMeasure.sample(SEED, 40, 40)
        assertEquals(a.steps, b.steps)
        assertEquals(a.maxFlagsAtOnce, b.maxFlagsAtOnce)
        assertEquals(a.distinctFlagsSeen, b.distinctFlagsSeen)
        assertEquals(
            ReachableCheckpointMeasure.historyBytes(a.largestArrival.toSortedMap().values.toList()),
            ReachableCheckpointMeasure.historyBytes(b.largestArrival.toSortedMap().values.toList())
        )
        assertEquals(a.largestHistory.map { it.crc }, b.largestHistory.map { it.crc })
    }
}
