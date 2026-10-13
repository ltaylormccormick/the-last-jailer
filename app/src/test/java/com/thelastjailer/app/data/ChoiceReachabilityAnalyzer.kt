package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StatType
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.applyChoice

/**
 * Path-aware analysis of every *gated* choice (one with [Choice.requirements]).
 *
 * A requirement is evaluated against states that are actually reachable on a first playthrough,
 * not merely against "does any choice anywhere set this flag". The search is exact for everything
 * that can gate a choice:
 *  - flags: `setFlags` and `clearFlags` are applied through [applyChoice], `requiredFlags` and
 *    `forbiddenFlags` are both honoured;
 *  - stats: only Courage, Honour and Gold are ever gated (see [reportsUnsupportedGates]) and the only
 *    thing that changes them is a choice's `statDeltas` (gold is clamped at zero, which is monotone),
 *    so they are tracked exactly;
 *  - combat: a combat node's outgoing edges are its victory and defeat nodes (defeat falls back to
 *    victory when unset), neither of which touches flags, Courage or Honour (see
 *    [com.thelastjailer.app.resolveCombat]). The node's own `choices` are not used.
 *
 * Verdicts:
 *  - [Status.ALIVE]: some reachable state satisfies the requirement (a witness exists);
 *  - [Status.DEAD]: the search completed and no reachable state satisfies it (or its node is
 *    unreachable);
 *  - [Status.INCONCLUSIVE]: the search hit [maxStates] or the story gates on a stat this analysis
 *    does not model. Never counted as safe.
 *
 * Replay edges (anything routed back to [startNodeId] after the game has begun) are excluded: they
 * reset nothing and would let flags from a finished run make first-playthrough choices look alive.
 */
class ChoiceReachabilityAnalyzer(
    private val nodes: List<StoryNode>,
    /** Outgoing combat edges for a combat node (victory, defeat). Defaults to none. */
    private val combatTargets: (StoryNode) -> List<String> = { emptyList() },
    private val startNodeId: String = "fallen_knight",
    private val startState: GameState = GameState(sceneId = startNodeId),
    private val maxStates: Int = 1_500_000
) {
    enum class Status { ALIVE, DEAD, INCONCLUSIVE }

    data class Verdict(val nodeId: String, val choice: Choice, val status: Status, val detail: String)

    private val byId = nodes.associateBy { it.id }

    /** States accepted (not dominated by an earlier one) by the last [analyse] call, for headroom checks. */
    var statesExplored: Int = 0
        private set

    /** A gate on a stat other than Courage/Honour: not modelled, so the affected choices are inconclusive. */
    private val unsupportedStats: Set<StatType> = nodes.flatMap { it.choices }
        .flatMap { it.requirements?.minStats?.keys ?: emptySet() }
        .filter { it != StatType.COURAGE && it != StatType.HONOUR && it != StatType.GOLD }
        .toSet()

    fun reportsUnsupportedGates(): Set<StatType> = unsupportedStats

    private fun edges(node: StoryNode): List<Pair<Choice?, String>> =
        if (node.combatEncounterId != null) combatTargets(node).map { null to it }
        else node.choices.map { it to it.nextNodeId }.filter { (_, to) -> to != startNodeId }

    private val flagsRead: Map<String, Set<String>> = nodes.associate { n ->
        n.id to n.choices.flatMap {
            (it.requirements?.requiredFlags ?: emptySet()) + (it.requirements?.forbiddenFlags ?: emptySet())
        }.toSet()
    }
    private val readsCourage: Map<String, Boolean> =
        nodes.associate { n -> n.id to n.choices.any { it.requirements?.minStats?.containsKey(StatType.COURAGE) == true } }
    private val readsHonour: Map<String, Boolean> =
        nodes.associate { n -> n.id to n.choices.any { it.requirements?.minStats?.containsKey(StatType.HONOUR) == true } }

    private val readsGold: Map<String, Boolean> =
        nodes.associate { n -> n.id to n.choices.any { it.requirements?.minStats?.containsKey(StatType.GOLD) == true } }

    /** For each node: everything readable from it onward (itself included). Used to drop state nothing ahead can see. */
    private class Ahead(val flags: Set<String>, val courage: Boolean, val honour: Boolean, val gold: Boolean)

    private val ahead: Map<String, Ahead> by lazy {
        val result = HashMap<String, Ahead>()
        for (n in nodes) {
            val seen = HashSet<String>()
            val stack = ArrayDeque<String>().apply { add(n.id) }
            val flags = HashSet<String>()
            var c = false
            var h = false
            var g = false
            while (stack.isNotEmpty()) {
                val id = stack.removeLast()
                if (!seen.add(id)) continue
                flags += flagsRead[id].orEmpty()
                c = c || readsCourage[id] == true
                h = h || readsHonour[id] == true
                g = g || readsGold[id] == true
                byId[id]?.let { node -> edges(node).forEach { (_, to) -> if (to in byId) stack.add(to) } }
            }
            result[n.id] = Ahead(flags, c, h, g)
        }
        result
    }

    private data class FlagKey(val node: String, val flags: Set<String>)

    private fun flagKeyOf(state: GameState): FlagKey {
        val a = ahead[state.sceneId]
        return FlagKey(state.sceneId, if (a == null) emptySet() else state.flags.filterTo(HashSet()) { it in a.flags })
    }

    /**
     * Courage/Honour are compared by dominance, not tracked as exact values: gates are only ever
     * minimums and no transition depends on a stat (apart from those gates), so a state with
     * Courage and Honour both at least as high as another state's, at the same node with the same
     * relevant flags, can do everything the other can. Keeping only the Pareto-best pairs per
     * (node, flags) is therefore exact, and keeps the search small.
     */
    private fun statsOf(state: GameState): Triple<Int, Int, Int> {
        val a = ahead[state.sceneId]
        return Triple(
            if (a?.courage == true) state.courage else 0,
            if (a?.honour == true) state.honour else 0,
            if (a?.gold == true) state.gold else 0
        )
    }

    /** Runs the search once and returns one verdict per gated choice. */
    fun analyse(): List<Verdict> {
        val satisfied = HashSet<Pair<String, Int>>() // (nodeId, choice index) seen satisfiable
        val visitedNodes = HashSet<String>()
        val frontier = HashMap<FlagKey, MutableList<Triple<Int, Int, Int>>>()
        var accepted = 0
        val queue = ArrayDeque<GameState>()
        var overflow = false

        fun enqueue(state: GameState) {
            if (overflow) return
            val points = frontier.getOrPut(flagKeyOf(state)) { ArrayList(2) }
            val p = statsOf(state)
            if (points.any { it.first >= p.first && it.second >= p.second && it.third >= p.third }) return // dominated
            points.removeAll { it.first <= p.first && it.second <= p.second && it.third <= p.third }
            points += p
            if (++accepted > maxStates) { overflow = true; return }
            queue.add(state)
        }
        enqueue(startState)

        while (queue.isNotEmpty() && !overflow) {
            val state = queue.removeFirst()
            val node = byId[state.sceneId] ?: continue
            visitedNodes += node.id
            if (node.combatEncounterId != null) {
                combatTargets(node).forEach { to -> if (to in byId) enqueue(state.copy(sceneId = to)) }
                continue
            }
            node.choices.forEachIndexed { index, choice ->
                val req = choice.requirements
                if (req != null && req.isSatisfiedBy(state)) satisfied += node.id to index
                if (choice.nextNodeId == startNodeId) return@forEachIndexed // replay edge
                if (req == null || req.isSatisfiedBy(state)) enqueue(state.applyChoice(choice))
            }
        }

        statesExplored = accepted
        return nodes.flatMap { node ->
            node.choices.mapIndexedNotNull { index, choice ->
                val req = choice.requirements ?: return@mapIndexedNotNull null
                val gatesUnsupported = req.minStats.keys.any { it in unsupportedStats }
                when {
                    (node.id to index) in satisfied -> Verdict(node.id, choice, Status.ALIVE, "witness state found")
                    gatesUnsupported -> Verdict(node.id, choice, Status.INCONCLUSIVE, "gates on a stat this analysis does not model")
                    overflow -> Verdict(node.id, choice, Status.INCONCLUSIVE, "search exceeded $maxStates states")
                    node.id !in visitedNodes -> Verdict(node.id, choice, Status.DEAD, "node is unreachable on a first playthrough")
                    else -> Verdict(node.id, choice, Status.DEAD, "no reachable state satisfies $req")
                }
            }
        }
    }
}
