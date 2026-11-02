package com.thelastjailer.app.checkpoint

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.resolveCombat
import java.util.Random
import java.util.TreeMap
import org.junit.Assert.assertTrue

/**
 * What a checkpoint history can actually hold, found by walking the real story.
 *
 * [CheckpointSizeTest] puts every flag, item and trophy the story can set into each of thirty checkpoints. No play can hold
 * that: the decisions are exclusive families, every chapter clears what it owns, and hub markers vanish when a hub is left.
 * This object walks the story with the real transitions ([applyChoice], [resolveCombat]) and records the arrival record the
 * recorder would build at each chapter boundary ([CheckpointRecord.fromState], the same call [ArrivalRecorder] makes), so
 * every record measured is a state a play can really be in.
 *
 * Two walking policies run from one seed. UNIFORM takes any takeable choice at random, the way a first play goes. COLLECTOR
 * takes the choice that adds the most (a flag, a trophy, an item) seven times in eight and wins most fights, the way a
 * completionist goes, so the sample leans toward the largest states rather than the typical ones.
 *
 * It is a lower bound on the true reachable maximum and nothing more: a walk that never met a state did not prove it
 * unreachable. What it adds to the worst-case diagnostic is a figure that grows when a play can really hold more, and not
 * merely when the story gains a flag that sits in a family a play only ever holds one member of.
 *
 * Not modelled, and so not in the figures: shop purchases (the inventory can hold repeats of a consumable) and items spent
 * in a fight. Both change the inventory string by a few hundred bytes per checkpoint at most; the worst-case model, which
 * holds every distinct item once, is unaffected.
 */
object ReachableCheckpointMeasure {

    enum class Policy { UNIFORM, COLLECTOR }

    class Sample(
        val walks: Int,
        val steps: Long,
        /** Chapter numbers an arrival record was built for. */
        val chaptersReached: Set<Int>,
        /** Every flag any walk held at any step. */
        val distinctFlagsSeen: Set<String>,
        /** For each chapter number, the arrival record holding the most stored text over all walks. */
        val largestArrival: Map<Int, CheckpointRecord>,
        /** The walk whose whole history of arrival records holds the most stored text. */
        val largestHistory: List<CheckpointRecord>,
        /** The most flags held at once at any step, and the most held in any arrival record. */
        val maxFlagsAtOnce: Int,
        val maxFlagsAtArrival: Int,
        /** Transitions that moved forward by other than one chapter (the recorder invalidates these; the story has none). */
        val oddJumps: Int
    )

    private const val START = "fallen_knight"

    private val byId by lazy {
        StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.associateBy { it.id }
    }

    private fun chapterNumber(s: GameState): Int? =
        ChapterIds.numberOf(StoryRepository.nodeOrNull(s.sceneId)?.chapterId ?: s.chapterId)

    private fun record(state: GameState, chapter: Int): CheckpointRecord =
        CheckpointRecord.fromState(state, chapter, Cp.L1)

    /**
     * Stored text of a record in Android's XML format, ignoring the handful of numbers: what decides which record is the
     * largest. The sizes reported are always measured through the real store ([historyBytes]), never from this.
     */
    fun weight(r: CheckpointRecord): Int =
        r.flags.sumOf { it.length + 26 } + r.trophies.sumOf { it.length + 26 } + r.inventory.sumOf { it.length + 1 }

    private fun gain(s: GameState, c: Choice): Int {
        val k = c.consequences
        var g = ((s.flags - k.clearFlags) + k.setFlags).size - s.flags.size
        if (k.unlockTrophy != null && k.unlockTrophy !in s.trophies) g += 1
        g += k.grantItemIds.count { it !in s.inventory }
        return g
    }

    private class Accumulator {
        var steps = 0L
        var walks = 0
        var oddJumps = 0
        var maxFlagsAtOnce = 0
        var maxFlagsAtArrival = 0
        val chapters = HashSet<Int>()
        val flagsSeen = HashSet<String>()
        val largestArrival = TreeMap<Int, CheckpointRecord>()
        var largestHistory: List<CheckpointRecord> = emptyList()
        var largestHistoryWeight = -1

        fun arrival(chapter: Int, r: CheckpointRecord) {
            chapters += chapter
            if (r.flags.size > maxFlagsAtArrival) maxFlagsAtArrival = r.flags.size
            val best = largestArrival[chapter]
            if (best == null || weight(r) > weight(best)) largestArrival[chapter] = r
        }

        fun history(records: List<CheckpointRecord>) {
            val w = records.sumOf { weight(it) }
            if (w > largestHistoryWeight) {
                largestHistoryWeight = w
                largestHistory = records
            }
        }
    }

    private fun walk(random: Random, policy: Policy, acc: Accumulator, maxSteps: Int = 4_000) {
        acc.walks++
        var s = GameState(sceneId = START)
        val history = TreeMap<Int, CheckpointRecord>()
        // The recorder writes Chapter 1 from the provably fresh state at the first choice.
        record(s, 1).also { history[1] = it; acc.arrival(1, it) }
        var steps = 0
        while (steps++ < maxSteps) {
            acc.steps++
            val node = byId[s.sceneId] ?: break
            val next: GameState
            if (node.combatEncounterId != null) {
                val win = if (policy == Policy.UNIFORM) random.nextBoolean() else random.nextInt(10) < 7
                next = s.resolveCombat(CombatRepository.encounter(node.combatEncounterId!!), CombatOutcome(victory = win, damageTaken = 0))
            } else {
                val takeable = node.choices.filter { c ->
                    (c.requirements == null || c.requirements!!.isSatisfiedBy(s)) && c.nextNodeId != START
                }
                if (takeable.isEmpty()) break
                val chosen = if (policy == Policy.COLLECTOR && random.nextInt(8) != 0) {
                    val best = takeable.maxOf { gain(s, it) }
                    val pool = takeable.filter { gain(s, it) == best }
                    pool[random.nextInt(pool.size)]
                } else {
                    takeable[random.nextInt(takeable.size)]
                }
                next = s.applyChoice(chosen)
            }
            acc.flagsSeen += next.flags
            if (next.flags.size > acc.maxFlagsAtOnce) acc.maxFlagsAtOnce = next.flags.size
            val before = chapterNumber(s)
            val after = chapterNumber(next)
            if (before != null && after != null && after > before) {
                if (after == before + 1) {
                    record(next, after).also { history[after] = it; acc.arrival(after, it) }
                } else {
                    acc.oddJumps++
                }
            }
            s = next
        }
        acc.history(history.values.toList())
    }

    fun sample(seed: Long, uniformWalks: Int, collectorWalks: Int): Sample {
        val acc = Accumulator()
        val random = Random(seed)
        repeat(uniformWalks) { walk(random, Policy.UNIFORM, acc) }
        repeat(collectorWalks) { walk(random, Policy.COLLECTOR, acc) }
        return Sample(
            walks = acc.walks, steps = acc.steps, chaptersReached = acc.chapters, distinctFlagsSeen = acc.flagsSeen,
            largestArrival = acc.largestArrival, largestHistory = acc.largestHistory,
            maxFlagsAtOnce = acc.maxFlagsAtOnce, maxFlagsAtArrival = acc.maxFlagsAtArrival, oddJumps = acc.oddJumps
        )
    }

    /** The size of [records] written as one checkpoint file by the real store, in the same XML model the worst case uses. */
    fun historyBytes(records: List<CheckpointRecord>): Int {
        val device = SimulatedDevice()
        val store = Cp.store(device)
        assertTrue("the store refused a record a walk produced", store.replaceAll(1, Cp.L1, records).isCommitted)
        return Cp.xmlSize(device.disk(Cp.file(1)))
    }

    /** Every flag any choice in the story can set: what the worst-case model puts in each checkpoint. */
    fun settableFlags(): Set<String> =
        byId.values.flatMap { n -> n.choices.flatMap { it.consequences.setFlags } }.toSet()
}
