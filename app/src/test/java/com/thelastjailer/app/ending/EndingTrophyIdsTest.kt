package com.thelastjailer.app.ending

import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.ReplayKit
import com.thelastjailer.app.data.StoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The nine ending trophies and the 37 that already exist. A trophy's name is its id in every save, so both lists are pinned
 * as literals: renaming or colliding with any of them would orphan or merge trophies players already hold.
 */
class EndingTrophyIdsTest {
    /** Every trophy a scene or a fight can give today (26 fight victories and 11 choice trophies). */
    private val existing37: Set<String> = setOf(
        "An Uneasy Alliance", "Blood and Silver", "Buried Properly", "Called Off", "First Blood", "Friend of Stonebeard",
        "Her Choice", "Ilsevet's Own Hand", "Kestrel's Apprentice", "Leaderless, Not Harmless", "Line Held", "Nails in the Dark",
        "Nearly Enough", "No More Between Us", "Not Today", "Out of the Ash", "Refused Gently", "Seal Held", "Six, Not One",
        "The Last Evening", "The Last Gate", "The Last Jailer", "The Ledger, Read to the End", "The Marsh Holds",
        "The Marshal's Warning", "The Second Silence", "The Watch Goes On", "The Whole, Undisguised", "What Almost Finished",
        "What Four Settings Do", "What Got Out", "What It Sent For the Thread", "What Was Lost", "What the Stone Remembers",
        "When Duskmere Answered", "Where It Began", "Whoever Finished It"
    )

    private fun folded(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()

    @Test
    fun `the nine ids are exactly the approved names`() {
        assertEquals(9, EndingTrophies.all.size)
        assertEquals(EndingKit.NINE, EndingTrophies.all)
        assertEquals("The Tale Told", EndingTrophies.TALE_TOLD)
        assertEquals("The Unyielding", EndingTrophies.UNYIELDING)
        assertEquals("The Honourbound", EndingTrophies.HONOURBOUND)
        assertEquals("The Steadfast", EndingTrophies.STEADFAST)
        assertEquals("Borrowed Strength", EndingTrophies.BORROWED_STRENGTH)
        assertEquals("Unaided", EndingTrophies.UNAIDED)
        assertEquals("Her Chains", EndingTrophies.HER_CHAINS)
        assertEquals("Her Freedom", EndingTrophies.HER_FREEDOM)
        assertEquals("Her Fate Unwritten", EndingTrophies.HER_FATE_UNWRITTEN)
    }

    @Test
    fun `each axis has its own legacy trophy`() {
        assertEquals("The Unyielding", EndingTrophies.forAxis(Axis.COURAGE))
        assertEquals("The Honourbound", EndingTrophies.forAxis(Axis.HONOUR))
        assertEquals("The Steadfast", EndingTrophies.forAxis(Axis.STEADFAST))
        assertEquals(EndingKit.LEGACIES, Axis.values().map { EndingTrophies.forAxis(it) }.toSet())
    }

    @Test
    fun `availableTrophies is still exactly the 37 existing trophies and holds none of the nine`() {
        assertEquals(37, existing37.size)
        assertEquals(existing37, StoryRepository.availableTrophies)
        assertTrue(StoryRepository.availableTrophies.intersect(EndingTrophies.all).isEmpty())
    }

    @Test
    fun `none of the nine equals any existing trophy, even with case and punctuation folded`() {
        assertTrue(existing37.intersect(EndingTrophies.all).isEmpty())
        val taken = existing37.map { folded(it) }.toSet()
        EndingTrophies.all.forEach { assertFalse("$it collides once folded", folded(it) in taken) }
    }

    @Test
    fun `no scene, fight or flag in the story already uses any of the nine`() {
        val nodes = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }
        val trophies = nodes.flatMap { n -> n.choices.mapNotNull { it.consequences.unlockTrophy } } +
            nodes.mapNotNull { n -> n.combatEncounterId?.let { CombatRepository.encounter(it).unlockTrophy } }
        val flags = nodes.flatMap { n -> n.choices.flatMap { it.consequences.setFlags + it.consequences.clearFlags } }
        assertTrue(trophies.none { it in EndingTrophies.all })
        assertTrue(flags.none { it in EndingTrophies.all })
    }

    @Test
    fun `the six ending ids the award step knows are exactly the six the story marks as endings`() {
        val marked = StoryRepository.chapters.flatMap { StoryRepository.nodesInChapter(it.id) }.filter { StoryRepository.isEnding(it) }.map { it.id }.toSet()
        assertEquals(6, marked.size)
        assertEquals(marked, EndingAwards.endingIds)
        assertEquals(ReplayKit.ENDINGS.map { it.nodeId }.toSet(), EndingAwards.endingIds)
    }
}
