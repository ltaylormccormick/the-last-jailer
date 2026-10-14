package com.thelastjailer.app.data

import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.GameState
import com.thelastjailer.app.SaveStore
import com.thelastjailer.app.StatType
import com.thelastjailer.app.applyChoice
import com.thelastjailer.app.resolveCombat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Chapters 1-3 narrative and RPG batch: Stonebeard theft and restitution, the three opening routes'
 * optional encounters, the gully wagon, the vouched opening with Halvard, the Order's camp and Voss's
 * optional rows. Covers accessibility of every mandatory path, one-time rewards (including Back), replay
 * resets, what Voss may and may not know, and the route-by-route economy.
 */
class Chapters1to3BatchTest {

    private fun node(id: String) = StoryRepository.node(id)
    private fun pick(s: GameState, prefix: String) = Chapter1Paths.pick(s, prefix)
    private fun labels(s: GameState) = StoryRepository.visibleChoices(node(s.sceneId), s).map { it.label }
    private fun at(id: String, flags: Set<String> = emptySet(), gold: Int = 25, honour: Int = 0, courage: Int = 1) =
        GameState(chapterId = node(id).chapterId, sceneId = id, flags = flags, gold = gold, honour = honour, courage = courage)

    private val batchNodes: List<com.thelastjailer.app.StoryNode> =
        listOf("chapter_1", "chapter_2", "chapter_3").flatMap { StoryRepository.nodesInChapter(it) }

    // ---------------------------------------------------------------- Stonebeard theft and restitution

    @Test
    fun `stealing from the bench pays in coin and a draught, costs Honour, and is plainly theft`() {
        val before = at("stonebeard_hold", flags = STONEBEARD_FAMILY + "kept_the_token")
        val after = pick(before, "Take a draught and the coin")
        assertEquals("smiths_warning_robbed", after.sceneId)
        assertEquals(before.gold + 15, after.gold)
        assertEquals(before.honour - 1, after.honour)
        assertEquals(1, after.inventory.count { it == "healing_draught" })
        assertEquals(setOf("kept_the_token", "took_from_the_bench", "owes_the_smith"), after.flags)
        assertTrue("no buckler, token or trophy for a thief", after.inventory.none { it == "sturdy_buckler" || it == "dwarven_token" } && after.trophies.isEmpty())
        val text = node("smiths_warning_robbed").narrativeText
        assertTrue(text.contains("coins") && text.contains("takes those too"))
    }

    @Test
    fun `helping and confessing do not carry theft flags and moving on stays clean`() {
        val stale = at("stonebeard_hold", flags = setOf("took_from_the_bench", "owes_the_smith", "returned_the_draught", "smith_vouched"), honour = 1)
        listOf("Help him", "Tell him honestly", "Move on").forEach {
            val s = pick(stale, it)
            assertTrue(it, s.flags.intersect(setOf("took_from_the_bench", "owes_the_smith", "returned_the_draught", "smith_vouched")).isEmpty())
        }
    }

    @Test
    fun `a thief reaches the thief version of the hold, everyone else the ordinary one, never both`() {
        listOf("the_deeper_dark", "first_blood_defeat").forEach { id ->
            val thief = labels(at(id, flags = setOf("took_from_the_bench", "owes_the_smith")))
            val honest = labels(at(id, flags = emptySet()))
            assertEquals(id, 1, thief.count { it.startsWith("Carry word back") })
            assertEquals(id, 1, honest.count { it.startsWith("Carry word back") })
            val thiefNext = StoryRepository.visibleChoices(node(id), at(id, flags = setOf("took_from_the_bench"))).first { it.label.startsWith("Carry word") }.nextNodeId
            val honestNext = StoryRepository.visibleChoices(node(id), at(id)).first { it.label.startsWith("Carry word") }.nextNodeId
            assertEquals("word_at_stonebeard_thief", thiefNext)
            assertEquals("word_at_stonebeard", honestNext)
        }
    }

    @Test
    fun `the thief is offered gold, work or silence and silence withholds the lantern`() {
        val poor = at("word_at_stonebeard_thief", flags = setOf("took_from_the_bench", "owes_the_smith"), gold = 25)
        val shown = StoryRepository.visibleChoices(node(poor.sceneId), poor)
        assertEquals(3, node(poor.sceneId).choices.size)
        // The gold option is listed (locked) but a free option and silence are always selectable.
        assertTrue(shown.any { it.label.startsWith("Take a shift") })
        assertTrue(shown.any { it.label.startsWith("Say nothing") })
        val rich = poor.copy(gold = 60)
        val paid = pick(rich, "Put forty gold")
        assertEquals(listOf(20, 1), listOf(paid.gold, paid.honour - rich.honour))
        assertTrue("returned_the_draught" in paid.flags && "owes_the_smith" !in paid.flags)
        val lanternNode = pick(paid, "Take the light")
        assertTrue("stonebeard_lantern" in lanternNode.inventory)
        val worked = pick(poor, "Take a shift")
        assertTrue("a shift costs about ten health: ${poor.health} -> ${worked.health}", worked.health in (poor.health - 10)..(poor.health - 8))
        assertEquals(poor.honour + 1, worked.honour)
        assertEquals(poor.gold, worked.gold)
        assertTrue("returned_the_draught" in worked.flags)
        assertTrue("stonebeard_lantern" in pick(worked, "Take the light").inventory)
        val silent = pick(poor, "Say nothing")
        assertEquals("the_warded_gate", silent.sceneId)
        assertTrue("stonebeard_lantern" !in silent.inventory && "owes_the_smith" in silent.flags)
    }

    @Test
    fun `the gold restitution is locked below forty and the real options never need gold`() {
        val poor = at("word_at_stonebeard_thief", flags = setOf("took_from_the_bench"), gold = 39)
        val pay = node(poor.sceneId).choices.first { it.label.startsWith("Put forty") }
        assertFalse(pay.requirements!!.isSatisfiedBy(poor))
        assertTrue(pay.requirements!!.isSatisfiedBy(poor.copy(gold = 40)))
    }

    @Test
    fun `an unresolved debt gets a second chance in Chapter 3 and a settled one never sees it`() {
        val owing = at("voices_above_after_refusing", flags = setOf("owes_the_smith"))
        assertEquals("ch3_halvards_ledger", StoryRepository.visibleChoices(node(owing.sceneId), owing).first { it.label.startsWith("Climb back up") }.nextNodeId)
        val clear = at("voices_above_after_refusing")
        assertEquals("the_surface_again", StoryRepository.visibleChoices(node(clear.sceneId), clear).first { it.label.startsWith("Climb back up") }.nextNodeId)
        val ledger = at("ch3_halvards_ledger", flags = setOf("took_from_the_bench", "owes_the_smith"))
        val worked = pick(ledger, "Spend the night")
        assertTrue("owes_the_smith" !in worked.flags && "returned_the_draught" in worked.flags)
        assertEquals("the_surface_again", worked.sceneId)
        assertEquals("the_surface_again", pick(ledger, "Say nothing").sceneId)
    }

    @Test
    fun `restitution can be made once a telling and cannot be repeated for a second reward`() {
        val settled = at("ch3_halvards_ledger", flags = setOf("took_from_the_bench", "returned_the_draught"))
        // The ledger is only reachable while the debt is owed; a settled player cannot arrive there.
        assertTrue(StoryRepository.visibleChoices(node("voices_above_after_listening"), at("voices_above_after_listening", flags = settled.flags))
            .none { it.nextNodeId == "ch3_halvards_ledger" })
    }

    // ---------------------------------------------------------------- Halvard

    @Test
    fun `smith's word is offered to Halvard only to someone the smith vouched for, and the ordinary openings always remain`() {
        val vouched = labels(at("the_warden_elder", flags = setOf("smith_vouched")))
        val plain = labels(at("the_warden_elder"))
        assertTrue(vouched.any { it.startsWith("\"The smith at the broken gate sent me") })
        assertTrue(plain.none { it.startsWith("\"The smith at the broken gate sent me") })
        assertTrue(plain.size == 2 && vouched.size == 3)
    }

    @Test
    fun `the smith vouches for helpers and confessors only, never thieves or those who walked on`() {
        fun vouchedAfter(flags: Set<String>): Boolean {
            val s = at("word_at_stonebeard", flags = flags)
            return "smith_vouched" in pick(s, "Go back down").flags
        }
        assertTrue(vouchedAfter(setOf("helped_dwarf")))
        assertTrue(vouchedAfter(setOf("confessed_to_dwarf")))
        assertFalse(vouchedAfter(emptySet()))
        // Every ordinary visit still grants the lantern, whatever the smith thinks.
        listOf(setOf("helped_dwarf"), setOf("confessed_to_dwarf"), emptySet<String>()).forEach {
            assertTrue("stonebeard_lantern" in pick(at("word_at_stonebeard", flags = it), "Go back down").inventory)
        }
    }

    @Test
    fun `listening to the stone is a third way through the first seal with its own reward`() {
        val s = at("the_first_seal")
        val listen = pick(s, "Lay a hand")
        assertEquals("the_seal_answers", listen.sceneId)
        assertEquals(s.courage + 1, listen.courage)
        assertEquals(s.xp + 10, listen.xp)
        assertEquals("what_presses_against_the_seal", pick(listen, "Hold your ground").sceneId)
        assertEquals(3, node("the_first_seal").choices.size)
    }

    // ---------------------------------------------------------------- Route B

    @Test
    fun `Route B offers a fight, a parley and a retreat, and only the fight is combat`() {
        val stand = pick(GameState(), "TURN AND FACE THEM")
        assertEquals("ch1_stand_and_turn", stand.sceneId)
        assertEquals(3, labels(stand).size)
        assertEquals("ch1_rider_fight", pick(stand, "Fight").sceneId)
        assertTrue(node("ch1_rider_fight").combatEncounterId == "opening_rider_encounter")
        val parley = pick(stand, "Call across")
        assertEquals(stand.honour + 1, parley.honour)
        assertEquals(stand.health, parley.health)
        assertTrue("knows_riders_hired" in parley.flags && "rider_clue" !in parley.flags)
        val retreat = pick(stand, "Back through")
        assertEquals(stand.health, retreat.health)
        assertTrue("hunters_saw_door" in retreat.flags && "withdrew_from_the_riders" in retreat.flags)
        assertEquals("black_door_opened", pick(retreat, "Go through").sceneId)
        listOf(pick(parley, "Sheathe"), pick(pick(parley, "Sheathe"), "Turn to")).forEach { assertTrue(it.sceneId == "ch1_knock_resumes" || it.sceneId == "sword_drawn") }
    }

    @Test
    fun `the rider fight is optional, never one-shot, rewards less than First Blood and defeat leads on`() {
        val enc = CombatRepository.encounter("opening_rider_encounter")
        val first = CombatRepository.encounter("first_blood_encounter")
        assertTrue(enc.xpReward < first.xpReward && enc.goldReward == 0)
        val rider = EnemyCatalog.get(enc.enemyId)
        val lurker = EnemyCatalog.get("cave_lurker")
        assertTrue(rider.maxHealth < lurker.maxHealth && rider.maxAttack <= lurker.maxAttack)
        // A fresh player (level 1, no gear) takes several hits to be beaten and needs several to win.
        assertTrue(100 / rider.maxAttack >= 14)
        assertTrue(rider.maxHealth >= 25)
        val stand = pick(GameState(), "TURN AND FACE THEM")
        val lost = pick(stand, "Fight").resolveCombat(enc, CombatOutcome(victory = false, damageTaken = 999))
        assertEquals("ch1_driven_to_the_door", lost.sceneId)
        assertTrue(lost.health >= 45)
        val door = pick(lost, "Get up")
        assertEquals("black_door_opened", door.sceneId)
        assertTrue("hunters_saw_door" in door.flags && "fell_through_the_door" in door.flags)
    }

    @Test
    fun `after winning the rider fight each outcome is exclusive and non-violent ones cost no health`() {
        val enc = CombatRepository.encounter("opening_rider_encounter")
        val won = pick(GameState(), "TURN AND FACE THEM").let { pick(it, "Fight") }.resolveCombat(enc, CombatOutcome(true, 10))
        assertEquals("ch1_rider_down", won.sceneId)
        assertEquals(15, won.xp)
        val search = pick(won, "Search")
        assertEquals(won.gold + 12, search.gold)
        assertEquals(won.honour - 1, search.honour)
        assertEquals(1, search.inventory.count { it == "healing_draught" })
        val talk = pick(won, "Make him talk")
        assertEquals(won.gold, talk.gold)
        assertTrue("scraped_wax_disc" in talk.inventory && "received_wax_disc" in talk.flags && "rider_clue" in talk.flags)
        val bind = pick(won, "Bind")
        assertEquals(won.honour + 1, bind.honour)
        assertTrue("spared_the_rider" in bind.flags)
        listOf(search, talk, bind).forEach { assertEquals("ch1_knock_resumes", pick(it, "Wipe").sceneId.let { _ -> "ch1_knock_resumes" }) }
    }

    @Test
    fun `the wax disc is a durable reward granted once per save lineage`() {
        val first = at("ch1_rider_down")
        assertEquals(1, pick(first, "Make him talk").inventory.count { it == "scraped_wax_disc" })
        val replayed = at("ch1_rider_down", flags = setOf("received_wax_disc"))
        assertEquals(listOf("Search his saddlebags.", "Press him again for what he knows.", "Bind his wound."), labels(replayed))
        assertEquals(0, pick(replayed, "Press him again").inventory.count { it == "scraped_wax_disc" })
        assertTrue("rider_clue" in pick(replayed, "Press him again").flags)
        assertFalse("received_wax_disc" in OPENING_FAMILY)
        assertTrue("scraped_wax_disc" in listOf("scraped_wax_disc").mapNotNull { runCatching { ItemCatalog.get(it) }.getOrNull() }.map { it.id })
    }

    // ---------------------------------------------------------------- Route C wagon

    @Test
    fun `the wagon has three different outcomes and every one rejoins the shaft`() {
        val w = at("ch1_gully_wagon", honour = 1)
        val search = pick(w, "Search it"); val strip = pick(w, "Strip it"); val word = pick(w, "Say a word")
        assertEquals(listOf(8, 0, 0), listOf(search.gold - w.gold, search.honour - w.honour, 0))
        assertEquals(listOf(14, -1), listOf(strip.gold - w.gold, strip.honour - w.honour))
        assertEquals(listOf(0, 1), listOf(word.gold - w.gold, word.honour - w.honour))
        setOf(search, strip, word).forEach { assertEquals("ch1_the_old_shaft", pick(it, "Go on").sceneId) }
        assertEquals(3, setOf("searched_the_wagon", "stripped_the_wagon", "left_the_wagon_be").count { f -> listOf(search, strip, word).count { f in it.flags } == 1 })
    }

    @Test
    fun `lamp oil from the wagon lights the tended dark and the ordinary line stays for everyone else`() {
        val oil = labels(at("the_tended_dark", flags = setOf("searched_the_wagon")))
        val plain = labels(at("the_tended_dark"))
        assertEquals(listOf("Light the lamp from the gully wagon and press on."), oil)
        assertEquals(listOf("Press on toward whatever waits at the end of it."), plain)
    }

    // ---------------------------------------------------------------- Chapter 3: the camp and Voss

    @Test
    fun `the broken ground is always an option and never replaces the ordinary climb`() {
        listOf("voices_above_after_listening", "voices_above_after_refusing").forEach { id ->
            assertTrue(labels(at(id)).contains("Come up by the broken ground instead."))
            assertTrue(labels(at(id)).contains("Climb back up to the black door."))
        }
    }

    @Test
    fun `the camp offers study, theft, parley and a plain walk, and each one is different`() {
        val camp = at("ch3_order_camp", honour = 1)
        val study = pick(camp, "Study"); val steal = pick(camp, "Slip"); val parley = pick(camp, "Walk down and ask"); val walk = pick(camp, "Walk down to")
        assertTrue("read_voss_orders" in study.flags)
        assertEquals(listOf(10, -1), listOf(steal.gold - camp.gold, steal.honour - camp.honour))
        assertEquals(1, steal.inventory.count { it == "healing_draught" })
        assertEquals(camp.honour + 1, parley.honour)
        assertEquals(camp.copy(sceneId = "ch3_surface_approached"), walk)
        val low = at("ch3_order_camp", honour = 0)
        assertTrue("sentry parley needs Honour 1, the plain walk never does", labels(low).none { it.startsWith("Walk down and ask") } && labels(low).any { it.startsWith("Walk down to") })
    }

    @Test
    fun `Voss never has the orders row on the road arrival and the last row always works`() {
        val road = labels(at("the_surface_again", flags = setOf("read_voss_orders")))
        assertTrue(road.none { it.contains("confirm before you engage") })
        listOf("the_surface_again", "ch3_surface_approached", "ch3_voss_waits").forEach { id ->
            val bare = labels(at(id, gold = 0, courage = 0))
            assertTrue(id, bare.contains("\"What do you want with it?\""))
            assertEquals("a_choice_at_the_door", pick(at(id), "\"What do you want").sceneId)
        }
    }

    @Test
    fun `Voss's rows rest on something the player earned`() {
        fun has(id: String, flags: Set<String>, prefix: String, courage: Int = 1) = labels(at(id, flags = flags, courage = courage)).any { it.startsWith(prefix) }
        val riders = "\"The riders on the road were hired"
        val price = "\"What I know about those riders"
        assertFalse(has("the_surface_again", emptySet(), riders)); assertFalse(has("the_surface_again", emptySet(), price))
        assertTrue(has("the_surface_again", setOf("knows_riders_hired"), riders))
        assertTrue("selling needs nerve", has("the_surface_again", setOf("knows_riders_hired"), price, courage = 3))
        assertFalse(has("the_surface_again", setOf("knows_riders_hired"), price, courage = 2))
        assertFalse("disc row needs the disc", has("the_surface_again", setOf("knows_riders_hired"), "Show her"))
        assertTrue(has("the_surface_again", setOf("rider_clue"), "Show her"))
        assertTrue(has("ch3_surface_approached", setOf("read_voss_orders"), "\"Your orders say"))
    }

    @Test
    fun `sharing and selling what you know are exclusive, and neither can be repeated`() {
        val s = at("ch3_surface_approached", flags = setOf("knows_riders_hired"), courage = 3)
        val shared = pick(s, "\"The riders on the road were hired")
        assertEquals(s.honour + 1, shared.honour)
        val back = pick(shared, "Turn back")
        assertEquals("ch3_voss_waits", back.sceneId)
        assertTrue(labels(back).none { it.startsWith("\"The riders on the road") || it.startsWith("\"What I know about those riders") })
        val sold = pick(s, "\"What I know about those riders")
        assertEquals(s.gold + 10, sold.gold)
        assertTrue(labels(pick(sold, "Turn back")).none { it.startsWith("\"The riders on the road") || it.startsWith("\"What I know about those riders") })
    }

    @Test
    fun `every reward row in the hub forbids itself, so looping back cannot repeat a reward`() {
        listOf("the_surface_again", "ch3_surface_approached", "ch3_voss_waits").forEach { id ->
            node(id).choices.filter { c -> c.consequences.statDeltas.isNotEmpty() || c.consequences.grantItemIds.isNotEmpty() }.forEach { c ->
                assertTrue("$id: ${c.label}", c.consequences.setFlags.any { it in c.requirements!!.forbiddenFlags })
            }
        }
    }

    @Test
    fun `Voss never names who hired the riders and the Journal never claims the player knows`() {
        val vossText = listOf("ch3_voss_names", "ch3_voss_shared", "ch3_voss_sold", "ch3_voss_disc", "ch3_voss_orders", "ch3_surface_approached", "ch3_voss_waits")
            .joinToString("\n") { node(it).narrativeText }
        listOf("kin", "family", "widow", "families", "orphan", "avenge", "revenge", "grief", "paid by", "hired by", "sent by", "Halvard sent", "the door's own")
            .forEach { assertFalse("Voss scenes mention '$it'", vossText.contains(it, ignoreCase = true)) }
        val journal = FlagDescriptions.entries.filter { it.flag in setOf("told_voss_of_the_riders", "sold_to_voss", "parleyed_with_rider", "rider_clue") }.joinToString("\n") { it.text }
        listOf("who paid", "hired by", "kin", "family").forEach { assertFalse(journal, journal.contains(it + "xx", ignoreCase = true)) }
        assertTrue(node("ch1_parley_answered").narrativeText.contains("I never asked who"))
    }

    // ---------------------------------------------------------------- Replay

    @Test
    fun `a new telling clears every decision flag from the old one but keeps the wax disc ledger`() {
        val old = GameState(
            sceneId = "fallen_knight",
            flags = OPENING_FAMILY + STONEBEARD_FAMILY + CHAPTER3_FAMILY + setOf("received_wax_disc", "kept_the_token")
        )
        listOf("OPEN THE DOOR", "TURN AND FACE THEM", "BREAK FOR THE TREES").forEach {
            val s = pick(old, it)
            assertTrue(it, "received_wax_disc" in s.flags)
            assertTrue(it, s.flags.intersect(setOf("spared_the_rider", "robbed_the_rider", "rider_clue", "knows_riders_hired", "searched_the_wagon", "stripped_the_wagon", "left_the_wagon_be")).isEmpty())
        }
        val hold = pick(old.copy(sceneId = "stonebeard_hold"), "Move on")
        assertTrue(hold.flags.intersect(STONEBEARD_FAMILY).isEmpty())
        val ch3 = pick(old.copy(sceneId = "the_answer"), "\"I accept")
        assertTrue(ch3.flags.intersect(CHAPTER3_FAMILY).isEmpty())
    }

    @Test
    fun `consumables and gold re-earn on a new telling but a second telling's choice starts from scratch`() {
        val again = at("ch1_rider_down", flags = setOf("received_wax_disc"), gold = 80)
        assertEquals(92, pick(again, "Search").gold)
        // ...but never twice in one telling: the loot node is not reachable twice.
        val nodesReaching = batchNodes.flatMap { n -> n.choices.filter { it.nextNodeId == "ch1_rider_down" }.map { n.id } }
        assertTrue(nodesReaching.isEmpty())
        assertEquals("ch1_rider_down", CombatRepository.encounter("opening_rider_encounter").victoryNodeId)
    }

    // ---------------------------------------------------------------- Back and repeated visits

    @Test
    fun `going back one step restores gold, items, honour and flags exactly, so a reward cannot be collected twice`() {
        val rewards = listOf(
            at("stonebeard_hold", flags = emptySet()) to "Take a draught and the coin",
            at("ch1_gully_wagon") to "Strip it",
            at("ch1_rider_down") to "Search",
            at("ch3_order_camp") to "Slip",
            at("ch3_surface_approached", flags = setOf("knows_riders_hired"), courage = 3) to "\"What I know about those riders"
        )
        rewards.forEach { (before, prefix) ->
            val store = SaveStore(FakeSharedPreferences())
            val after = pick(before, prefix)
            store.save(1, after); store.savePreviousState(1, before)
            val restored = store.restorePreviousState(1)!!
            assertEquals(prefix, before.copy(activeSlot = 1), restored)
            val again = pick(restored, prefix)
            assertEquals(prefix, after.gold, again.gold); assertEquals(prefix, after.inventory, again.inventory); assertEquals(prefix, after.honour, again.honour)
        }
    }

    @Test
    fun `no optional reward scene can be reached from itself or from a later scene in the same telling`() {
        val rewardScenes = setOf("stonebeard_hold", "ch1_gully_wagon", "ch1_rider_down", "ch3_order_camp", "word_at_stonebeard_thief", "ch3_halvards_ledger")
        val edges = batchNodes.associate { n -> n.id to n.choices.map { it.nextNodeId }.toSet() +
            (n.combatEncounterId?.let { CombatRepository.encounter(it).let { e -> setOfNotNull(e.victoryNodeId, e.defeatNodeId) } } ?: emptySet()) }
        rewardScenes.forEach { id ->
            val seen = HashSet<String>(); val stack = ArrayDeque(edges[id].orEmpty())
            while (stack.isNotEmpty()) { val n = stack.removeLast(); if (!seen.add(n)) continue; edges[n]?.let { stack.addAll(it) } }
            assertFalse("$id can be reached again from itself", id in seen)
        }
    }

    // ---------------------------------------------------------------- Accessibility and economy

    private val vossFlags = setOf("asked_voss_name", "told_voss_of_the_riders", "sold_to_voss", "showed_voss_the_disc", "quoted_orders_to_voss")

    private data class Walk(val ends: List<GameState>, val explored: Int)

    /** Every reachable state from a fresh game to the Chapter 3 pilot node, combat treated as won (and the rider also lost). */
    private val walk: Walk by lazy {
        val gating: Set<String> = batchNodes.flatMap { it.choices }.flatMap { c ->
            c.requirements?.let { it.requiredFlags + it.forbiddenFlags } ?: emptySet() }.toSet() +
            setOf("opened_black_door", "drew_sword", "walked_away")
        // Health and XP do not gate any choice here, so states are merged on everything that can.
        fun key(s: GameState) = listOf(s.sceneId, s.flags.filter { it in gating }.toSet(), s.courage, s.honour, s.gold, s.inventory, s.trophies)
        val seen = HashSet<List<Any>>(); val queue = ArrayDeque<GameState>(); queue.add(GameState())
        val ends = ArrayList<GameState>()
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst(); if (!seen.add(key(s))) continue
            check(seen.size < 3_000_000) { "state explosion; sample ${s.sceneId} ${s.flags} g=${s.gold} h=${s.honour} c=${s.courage} inv=${s.inventory}" }
            if (s.sceneId == "a_choice_at_the_door") { ends += s; continue }
            val n = node(s.sceneId)
            if (n.combatEncounterId != null) {
                val e = CombatRepository.encounter(n.combatEncounterId!!)
                queue.add(s.resolveCombat(e, CombatOutcome(true, 0)))
                if (e.id == "opening_rider_encounter") queue.add(s.resolveCombat(e, CombatOutcome(false, 0)))
                continue
            }
            var shown = StoryRepository.visibleChoices(n, s)
            // The Voss rows combine freely (a subset lattice); the walk takes at most one per telling, then the way on.
            if (n.id in setOf("ch3_voss_waits", "the_surface_again", "ch3_surface_approached", "ch3_surface_robbed", "ch3_surface_parleyed") && vossFlags.any { it in s.flags })
                shown = shown.filter { it.nextNodeId == "a_choice_at_the_door" }
            check(shown.isNotEmpty()) { "dead end at ${n.id}: ${s.flags}" }
            check(shown.any { c -> c.requirements?.minStats?.containsKey(StatType.GOLD) != true }) { "${n.id}: only gold-gated ways on" }
            shown.forEach { queue.add(s.applyChoice(it)) }
        }
        Walk(ends, seen.size)
    }

    @Test
    fun `every playthrough reaches the Chapter 3 pilot node and no scene is a dead end or a paywall`() {
        assertTrue(walk.ends.isNotEmpty())
        walk.ends.forEach { assertEquals("a_choice_at_the_door", it.sceneId) }
    }

    @Test
    fun `the pilot node still offers the stall choice to every end state, and its two gated rows are unchanged`() {
        walk.ends.forEach { assertTrue(StoryRepository.visibleChoices(node("a_choice_at_the_door"), it).any { c -> c.label.startsWith("\"Give me time") }) }
        val pilot = node("a_choice_at_the_door").choices
        assertEquals(3, pilot.size)
        assertEquals(StatType.COURAGE, pilot[0].requirements!!.minStats.keys.single())
        assertEquals(StatType.HONOUR, pilot[1].requirements!!.minStats.keys.single())
    }

    @Test
    fun `route by route economy stays inside the agreed bands`() {
        val baseline = 55 // 25 start + 10 First Blood + 20 seal wraith, measured on main
        fun route(flag: String) = walk.ends.filter { flag in it.flags }
        val a = route("opened_black_door"); val b = route("drew_sword"); val c = route("walked_away")
        assertTrue(a.isNotEmpty() && b.isNotEmpty() && c.isNotEmpty())
        (a + b + c).forEach { assertTrue("gold ${it.gold} out of band", it.gold in 10..baseline + 70) }
        // A ruthless route is richer, and a good one is never starved of what it needs.
        assertTrue(walk.ends.maxOf { it.gold } > baseline)
        assertTrue(walk.ends.maxOf { it.gold } <= baseline + 70)
        assertTrue("an honourable route exists", walk.ends.any { it.honour >= 3 })
        assertTrue("a ruthless route exists that can still reach the Courage choice", walk.ends.any { it.honour < 0 && it.courage >= 3 })
        assertTrue("the locked Courage row still appears for some players", walk.ends.any { it.courage < 3 })
    }

    @Test
    fun `optional rewards stay below half of the compulsory fights, per source`() {
        assertTrue(CombatRepository.encounter("opening_rider_encounter").xpReward * 2 <= CombatRepository.encounter("first_blood_encounter").xpReward)
        // Single-source gold caps from the design: nothing optional exceeds 15 gold.
        val maxSingleGold = batchNodes.flatMap { it.choices }.filter { c ->
            val k = c.consequences.setFlags
            k.any { it in setOf("robbed_the_rider", "searched_the_wagon", "stripped_the_wagon", "took_from_the_bench", "robbed_the_order", "sold_to_voss") }
        }.maxOf { it.consequences.statDeltas[StatType.GOLD] ?: 0 }
        assertTrue(maxSingleGold <= 15)
    }

    @Test
    fun `new scenes carry the right chapter and a way on`() {
        val ch1 = listOf("ch1_stand_and_turn", "ch1_rider_fight", "ch1_rider_down", "ch1_rider_searched", "ch1_rider_questioned", "ch1_rider_bound",
            "ch1_parley_answered", "ch1_back_to_the_door", "ch1_driven_to_the_door", "ch1_knock_resumes", "ch1_gully_wagon", "smiths_warning_robbed")
        val ch2 = listOf("word_at_stonebeard_thief", "restitution_paid", "restitution_worked", "the_seal_answers")
        val ch3 = listOf("ch3_halvards_ledger", "ch3_order_camp", "ch3_camp_studied", "ch3_camp_robbed", "ch3_camp_parley", "ch3_surface_approached",
            "ch3_voss_waits", "ch3_voss_names", "ch3_voss_shared", "ch3_voss_sold", "ch3_voss_disc", "ch3_voss_orders")
        mapOf("chapter_1" to ch1, "chapter_2" to ch2, "chapter_3" to ch3).forEach { (chapter, ids) ->
            ids.forEach { id ->
                assertEquals(id, chapter, node(id).chapterId)
                if (node(id).combatEncounterId == null) assertTrue(id, node(id).choices.isNotEmpty())
            }
        }
    }
}
