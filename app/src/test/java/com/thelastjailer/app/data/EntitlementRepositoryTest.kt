package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntitlementRepositoryTest {
    private lateinit var repository: LocalEntitlementRepository

    @Before
    fun setUp() {
        repository = LocalEntitlementRepository(FakeSharedPreferences())
    }

    @Test
    fun `chapters I through IX are unlocked by default`() {
        (1..9).forEach { assertTrue(repository.isChapterUnlocked("chapter_$it")) }
    }

    @Test
    fun `chapters X through XXX are locked by default`() {
        (10..30).forEach { assertFalse(repository.isChapterUnlocked("chapter_$it")) }
    }

    @Test
    fun `full story is not unlocked by default`() {
        assertFalse(repository.hasUnlockedFullStory())
    }

    @Test
    fun `save slots default to the free limit`() {
        assertEquals(LocalEntitlementRepository.FREE_SAVE_SLOTS, repository.maxSaveSlots())
    }

    @Test
    fun `unlocking the full story opens later chapters and grants more save slots`() {
        repository.unlockFullStory()

        assertTrue(repository.hasUnlockedFullStory())
        (1..30).forEach { assertTrue(repository.isChapterUnlocked("chapter_$it")) }
        assertEquals(LocalEntitlementRepository.UNLOCKED_SAVE_SLOTS, repository.maxSaveSlots())
    }

    @Test
    fun `the debug purchase toggle unlocks the full story`() {
        repository.setDebugPurchaseSimulated(true)

        assertTrue(repository.hasUnlockedFullStory())
        assertTrue(repository.isChapterUnlocked("chapter_10"))

        repository.setDebugPurchaseSimulated(false)

        assertFalse(repository.hasUnlockedFullStory())
        assertTrue(repository.isChapterUnlocked("chapter_9"))
        assertFalse(repository.isChapterUnlocked("chapter_10"))
    }

    @Test
    fun `a real purchase is independent of the debug toggle`() {
        repository.unlockFullStory()

        repository.setDebugPurchaseSimulated(false)

        assertTrue("turning the debug toggle off must not lock a real purchase", repository.hasUnlockedFullStory())
    }
}
