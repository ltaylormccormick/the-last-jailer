package com.thelastjailer.app.data

import com.thelastjailer.app.Item

/** Canonical definitions for every item id that can appear in a [com.thelastjailer.app.GameState] inventory. */
object ItemCatalog {
    private val items: Map<String, Item> = listOf(
        Item(
            id = "broken_sword",
            name = "Broken Sword",
            description = "The blade Kaelen carried out of disgrace. Still sharp enough to matter."
        ),
        Item(
            id = "dwarven_token",
            name = "Dwarven Token",
            description = "A stonebeard's mark of thanks, warm to the touch even in the cold of the hold."
        ),
        Item(
            id = "traveler_ration",
            name = "Traveler's Ration",
            description = "Hard bread and dried meat. Not much, but it will keep a man walking."
        ),
        Item(
            id = "healing_draught",
            name = "Healing Draught",
            description = "A dwarven remedy, bitter and effective."
        ),
        Item(
            id = "tarnished_guard_token",
            name = "Tarnished Guard Token",
            description = "A clasp from a King's Guard cloak — not his own. A reminder of what the silver once meant."
        ),
        Item(
            id = "sturdy_buckler",
            name = "Sturdy Buckler",
            description = "Dwarven-forged and dented from use. Given, not lent, by a smith who expects nothing back."
        ),
        Item(
            id = "warden_sigil",
            name = "Warden's Sigil",
            description = "A dwarven sigil marking one who has stood at the failing gate. Small, heavy, and impossible to lose."
        ),
        Item(
            id = "jailers_brand",
            name = "Jailer's Brand",
            description = "A mark on the palm, small and precise, left by the binding rite. It does not fade."
        ),
        Item(
            id = "ashen_signet",
            name = "Ashen Signet",
            description = "A rank insignia stripped from a fallen vanguard. Beneath the enamel: answers to the Cinder Marshal."
        ),
        Item(
            id = "cinder_marshal_missive",
            name = "Cinder Marshal's Missive",
            description = "A sealed dispatch, dropped in the retreat. The wax bears a mark shaped like a closed hand around a flame."
        ),
        Item(
            id = "kestrels_locket",
            name = "Kestrel's Locket",
            description = "Warded in the same hand as a gate that failed anyway. An apology, carried the rest of the way for her."
        ),
        Item(
            id = "voss_seal",
            name = "Voss's Seal",
            description = "A signet ring bearing the Ashen Order's true mark, offered freely rather than seized. What it still opens, this far from anyone who answers to it, remains to be seen."
        ),
        Item(
            id = "torn_sanctum_plans",
            name = "Torn Sanctum Plans",
            description = "Half-burned schematics for a six-sided frame, each setting marked to match the wards of a different jailer's gate. Whatever it's for, it isn't finished."
        ),
        Item(
            id = "ordruns_broken_blade",
            name = "Ordrun's Broken Blade",
            description = "Snapped clean at the crossguard. Cinder-forged steel, deep grey-black — command-rank, not rank-and-file."
        ),
        Item(
            id = "halvards_warden_chain",
            name = "Halvard's Warden Chain",
            description = "Tarnished silver-iron, six links for six wardens who no longer stand watch. Wasn't ever really about the chain, he said. Kaelen isn't so sure that's entirely true."
        ),
        Item(
            id = "fenmoor_ward_shard",
            name = "Fenmoor Ward Shard",
            description = "A fragment struck from Fenmoor's failing ward in the scuffle. Cold to the touch, and colder still where the crack runs through it."
        ),
        Item(
            id = "shard_of_the_seventh_door",
            name = "Shard of the Seventh Door",
            description = "A fragment struck from the failed frame — warm when it should be cold, and humming faintly the way the Sanctum's spire used to hum from a distance. Whatever it remembers being part of, it hasn't stopped trying to finish the job."
        ),
        Item(
            id = "rubbing_of_halvards_mark",
            name = "Rubbing of Halvard's Mark",
            description = "Charcoal on cloth, pressed against the warden's mark carved into Stonebeard's gate. A keepsake, not a weapon."
        ),
        Item(
            id = "shard_of_the_first_ward",
            name = "Shard of the First Ward",
            description = "A fragment that shouldn't exist outside the vision it came from — proof, if proof were needed, that whatever happened to the six wards happened to all of them at once, a very long time ago."
        ),
        Item(
            id = "record_of_ashwell",
            name = "Record of Ashwell",
            description = "Water-stained Order archive pages, fifteen years old. The closest thing to an answer for who Ilsevet was before she was the Cinder Marshal."
        ),
        Item(
            id = "twisted_frame_component",
            name = "Twisted Frame Component",
            description = "Pulled loose from the seventh-door frame before it could finish becoming anything. Warped, dangerous, and — for tonight, at least — inert."
        ),
        Item(
            id = "ilsevets_blade",
            name = "Ilsevet's Blade",
            description = "Plain, unceremonial, exactly the kind of weapon someone carries when they've stopped caring about anything but whether it works. Taken, not surrendered."
        )
    ).associateBy { it.id }

    fun get(id: String): Item? = items[id]

    fun resolve(ids: List<String>): List<Item> = ids.mapNotNull { items[it] }
}
