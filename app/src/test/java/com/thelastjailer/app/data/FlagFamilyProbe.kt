package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.ChoiceRequirement
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.data.ChoiceReachabilityAnalyzer.Status
import com.thelastjailer.app.resolveCombat
import java.util.Random

/**
 * Ways of asking what the states that can stand at a scene look like, for a family of flags the story reads as
 * alternatives ("exactly one of these is set"). Three methods, each sound for what it claims:
 *
 *  - [StoryGraph] proves, from the structure of the choice graph with every gate ignored, that no road takes two members of a
 *    family together and that every road takes one. Ignoring gates can only make the claim harder to prove, never wrong.
 *  - [Walker] takes real walks of the real transitions ([applyChoice], [resolveCombat]) and records the flags and items held
 *    each time a walk stands in a scene. A state it records is a reachable state, so it proves a member or a combination
 *    can happen. It cannot prove one cannot.
 *  - [ask] puts a question to the exact search ([ChoiceReachabilityAnalyzer]) as a synthetic gated choice on a copy of the
 *    scene. It keeps every flag it is asked about for the whole of the story, so only a few at a time are affordable; with the
 *    thirty the package needs it overflows its budget (INCONCLUSIVE, which is never an answer). It is used for single families.
 *
 * Items can be asked about as flags: [withItemFlags] mirrors every item grant as a flag called `item:<id>`.
 */
internal object FlagFamilyProbe {
    private const val END = "__probe_end__"

    class Question(val name: String, val required: Set<String> = emptySet(), val forbidden: Set<String> = emptySet()) {
        fun holdsIn(flags: Set<String>) = flags.containsAll(required) && flags.none { it in forbidden }
    }

    class Result(val node: String, val answers: Map<String, Status>, val statesExplored: Int)

    fun withItemFlags(nodes: List<StoryNode>): List<StoryNode> = nodes.map { n ->
        n.copy(choices = n.choices.map { c ->
            if (c.consequences.grantItemIds.isEmpty()) c
            else c.copy(consequences = c.consequences.copy(setFlags = c.consequences.setFlags + c.consequences.grantItemIds.map { "item:$it" }))
        })
    }

    /** Puts [questions] to the exact search at [atNode]. */
    fun ask(
        nodes: List<StoryNode>,
        combatTargets: (StoryNode) -> List<String>,
        atNode: String,
        questions: List<Question>,
        maxStates: Int
    ): Result {
        require(questions.map { it.name }.toSet().size == questions.size) { "duplicate question names" }
        val probed = nodes.map { n ->
            if (n.id != atNode) n
            else n.copy(choices = n.choices + questions.map { q ->
                Choice(label = "probe:${q.name}", nextNodeId = END, requirements = ChoiceRequirement(q.required, q.forbidden))
            })
        }
        val analyzer = ChoiceReachabilityAnalyzer(probed, combatTargets, maxStates = maxStates, focus = setOf(atNode))
        val verdicts = analyzer.analyse().filter { it.nodeId == atNode }
        return Result(
            atNode,
            questions.associate { q -> q.name to verdicts.first { it.choice.label == "probe:${q.name}" }.status },
            analyzer.statesExplored
        )
    }

    /** The questions that settle a family: none of it, each member alone, each pair together. */
    fun familyQuestions(prefix: String, flags: List<String>, context: Set<String> = emptySet()): List<Question> {
        val all = flags.toSet()
        return listOf(Question("$prefix:none", context, all)) +
            flags.map { Question("$prefix:only:$it", context + it, all - it) } +
            flags.flatMapIndexed { i, a -> flags.drop(i + 1).map { b -> Question("$prefix:both:$a+$b", context + a + b, emptySet()) } }
    }

    /**
     * Random walks from the start. Each time a walk stands in one of [targets] it records the flags and items it holds, limited
     * to [universe] (items appear as `item:<id>`). With a probability of four in ten a step prefers a choice that sets a flag
     * of [lean], so rare combinations are met; leaning never takes a choice that is not takeable.
     */
    class Walker(
        nodes: List<StoryNode>,
        private val combat: (StoryNode) -> CombatEncounter?,
        private val targets: Set<String>,
        private val universe: Set<String>,
        private val lean: Set<String>,
        private val startNodeId: String = "fallen_knight"
    ) {
        private val byId = nodes.associateBy { it.id }

        /** For each target scene, the distinct projections of what a walk held there onto [universe]. */
        val seen: Map<String, Set<Set<String>>> get() = record
        private val record = HashMap<String, MutableSet<Set<String>>>()
        var walksFinished = 0
            private set
        var stepsTaken = 0L
            private set

        fun run(walks: Int, seed: Long, maxSteps: Int = 3_000) {
            val random = Random(seed)
            repeat(walks) {
                walk(random, maxSteps)
                walksFinished++
            }
        }

        private fun walk(random: Random, maxSteps: Int) {
            var s = GameState(sceneId = startNodeId)
            var steps = 0
            while (steps++ < maxSteps) {
                stepsTaken++
                val node = byId[s.sceneId] ?: return
                if (node.id in targets) {
                    val held = s.flags.filterTo(HashSet()) { it in universe } + s.inventory.map { "item:$it" }.filter { it in universe }
                    record.getOrPut(node.id) { HashSet() } += held
                }
                if (node.combatEncounterId != null) {
                    val encounter = combat(node) ?: return
                    s = s.resolveCombat(encounter, CombatOutcome(victory = random.nextBoolean(), damageTaken = 0))
                    continue
                }
                val takeable = node.choices.filter { c -> (c.requirements?.isSatisfiedBy(s) ?: true) && c.nextNodeId != startNodeId }
                if (takeable.isEmpty()) return
                val pool = if (lean.isNotEmpty() && random.nextInt(10) < 4) {
                    takeable.filter { c -> c.consequences.setFlags.any { it in lean } || c.consequences.grantItemIds.any { "item:$it" in lean } }
                        .ifEmpty { takeable }
                } else takeable
                s = s.applyChoice(pool[random.nextInt(pool.size)])
            }
        }

        fun witnessed(at: String, q: Question): Boolean = record[at].orEmpty().any { q.holdsIn(it) }
    }

    /**
     * The choice graph with every gate ignored: an edge for each choice, and for a fight its victory and defeat nodes.
     * Replay edges (back to [start]) are left out, as they are for the exact search.
     */
    class StoryGraph(nodes: List<StoryNode>, combatTargets: (StoryNode) -> List<String>, private val start: String = "fallen_knight") {
        /** A choice taken at [from] (index [index] in the scene), or a fight's outcome (choice null). */
        class Edge(val from: String, val index: Int, val to: String, val choice: Choice?)

        private val byId = nodes.associateBy { it.id }
        private val out: Map<String, List<Edge>> = nodes.associate { n ->
            n.id to if (n.combatEncounterId != null) combatTargets(n).mapIndexed { i, to -> Edge(n.id, -1 - i, to, null) }
            else n.choices.mapIndexedNotNull { i, c -> if (c.nextNodeId == start) null else Edge(n.id, i, c.nextNodeId, c) }
        }
        val edges: List<Edge> = out.values.flatten()

        fun reach(from: Collection<String>, banned: Set<Edge> = emptySet()): Set<String> {
            val seen = HashSet<String>()
            val stack = ArrayDeque<String>().apply { addAll(from) }
            while (stack.isNotEmpty()) {
                val id = stack.removeLast()
                if (!seen.add(id)) continue
                out[id]?.forEach { e -> if (e !in banned && e.to in byId) stack.add(e.to) }
            }
            return seen
        }

        fun reachFromStart(): Set<String> = reach(listOf(start))

        /** True when every road from [from] to [target] takes at least one edge of [banned] (the target is unreachable without them). */
        fun mustPass(banned: Set<Edge>, target: String, from: String = start): Boolean = target !in reach(listOf(from), banned)

        /** The nodes from which [target] can be reached (itself included). */
        fun canReach(target: String): Set<String> {
            val back = HashMap<String, MutableList<String>>()
            edges.forEach { e -> back.getOrPut(e.to) { ArrayList() } += e.from }
            val seen = HashSet<String>()
            val stack = ArrayDeque<String>().apply { add(target) }
            while (stack.isNotEmpty()) {
                val id = stack.removeLast()
                if (!seen.add(id)) continue
                back[id]?.forEach { stack.add(it) }
            }
            return seen
        }

        fun settersOf(flags: Set<String>): List<Edge> =
            edges.filter { e -> e.choice != null && e.choice.consequences.setFlags.any { it in flags } }

        fun grantorsOf(itemId: String): List<Edge> =
            edges.filter { e -> e.choice != null && itemId in e.choice.consequences.grantItemIds }

        /**
         * Why a family of flags cannot be anything but "exactly one" (or "at most one") at [at], by the structure of the story alone.
         * [Proof.exhaustive]: every road from the start to [at] takes a choice that sets a member, and no choice clears a member after
         * the last such choice on any road (a clearing choice is harmless when every road from it to [at] sets a member again).
         * [Proof.exclusive]: no road takes a choice that sets one member and later another that does not clear the first, and no
         * choice sets two.
         */
        class Proof(val exhaustive: Boolean, val exclusive: Boolean, val notes: List<String>)

        /**
         * True when [s2] can never be taken after [s1] because it forbids a flag [s1] sets and no choice on any road between them
         * clears that flag. The only gate this looks at is a forbidden flag; every other gate is ignored, which keeps it sound.
         */
        fun gatedOut(s1: Edge, s2: Edge): Boolean {
            val forbidden = s2.choice?.requirements?.forbiddenFlags ?: return false
            val common = forbidden.intersect(s1.choice!!.consequences.setFlags)
            if (common.isEmpty()) return false
            val after = reach(listOf(s1.to))
            return common.any { f ->
                edges.none { c -> c.choice != null && f in c.choice.consequences.clearFlags && c.from in after && s2.from in reach(listOf(c.to)) }
            }
        }

        /** True when every choice that enters [node] requires [flag]. */
        fun onlyEnteredRequiring(node: String, flag: String): Boolean {
            val into = edges.filter { it.to == node }
            return into.isNotEmpty() && into.all { e -> e.choice != null && flag in (e.choice.requirements?.requiredFlags ?: emptySet()) }
        }

        fun clearersOf(flag: String): List<Edge> = edges.filter { e -> e.choice != null && flag in e.choice.consequences.clearFlags }

        fun prove(family: Set<String>, at: String): Proof {
            val notes = ArrayList<String>()
            val reachable = reachFromStart()
            val toAt = canReach(at)
            val setters = settersOf(family).filter { it.from in reachable && it.to in toAt }
            val banned = setters.toSet()
            val exhaustive = mustPass(banned, at)
            if (!exhaustive) notes += "a road reaches $at without a choice that sets one of $family"
            val pureClearers = edges.filter { e ->
                e.choice != null && e.from in reachable && e.to in toAt &&
                    e.choice.consequences.clearFlags.any { it in family } && e.choice.consequences.setFlags.none { it in family }
            }
            var clearersHarmless = true
            pureClearers.forEach { c ->
                if (!mustPass(banned, at, from = c.to)) { clearersHarmless = false; notes += "${c.from}#${c.index} clears the family and a road from it reaches $at without setting a member" }
            }
            var exclusive = true
            setters.forEach { s ->
                val sets = s.choice!!.consequences.setFlags.filter { it in family }
                if (sets.size > 1) { exclusive = false; notes += "${s.from}#${s.index} sets ${sets.size} members at once" }
            }
            for (s1 in setters) {
                val after = reach(listOf(s1.to))
                for (s2 in setters) {
                    if (s1 === s2 || s2.from !in after) continue
                    if (gatedOut(s1, s2)) continue
                    val a = s1.choice!!.consequences.setFlags.filter { it in family }.toSet()
                    val b = s2.choice!!.consequences.setFlags.filter { it in family }.toSet()
                    if ((b - a).isEmpty()) continue
                    val cleared = s2.choice.consequences.clearFlags
                    if (a.all { it in cleared }) continue
                    exclusive = false
                    notes += "${s1.from}#${s1.index} (sets $a) can be followed by ${s2.from}#${s2.index} (sets $b) without clearing"
                }
            }
            return Proof(exhaustive && clearersHarmless, exclusive, notes.distinct())
        }
    }
}
