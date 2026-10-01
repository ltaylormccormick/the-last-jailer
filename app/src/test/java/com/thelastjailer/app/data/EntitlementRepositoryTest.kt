package com.thelastjailer.app.data

import com.thelastjailer.app.FakeSharedPreferences
import com.thelastjailer.app.BuildConfig
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
    fun `the purchase simulation only unlocks debug builds`() {
        repository.setDebugPurchaseSimulated(true)

        assertEquals(BuildConfig.DEBUG, repository.hasUnlockedFullStory())
        assertEquals(BuildConfig.DEBUG, repository.isChapterUnlocked("chapter_10"))

        repository.setDebugPurchaseSimulated(false)

        assertFalse(repository.hasUnlockedFullStory())
        assertTrue(repository.isChapterUnlocked("chapter_9"))
        assertFalse(repository.isChapterUnlocked("chapter_10"))
    }

    @Test
    fun `tester unlock follows the build flag and survives reopening`() {
        val prefs = FakeSharedPreferences()
        val first = LocalEntitlementRepository(prefs)
        first.unlockForTesting()
        val reopened = LocalEntitlementRepository(prefs)
        assertEquals(BuildConfig.TESTER_UNLOCK_ENABLED, reopened.hasUnlockedFullStory())
        (10..30).forEach {
            assertEquals(BuildConfig.TESTER_UNLOCK_ENABLED, reopened.isChapterUnlocked("chapter_$it"))
        }
        assertFalse("Tester access must never create a purchase", prefs.getBoolean("unlocked_full_story", false))
    }

    @Test
    fun `stored tester access is ignored by production builds`() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putBoolean("internal_test_full_story", true).apply()
        val repository = LocalEntitlementRepository(prefs)
        assertEquals(BuildConfig.TESTER_UNLOCK_ENABLED, repository.hasUnlockedFullStory())
        assertEquals(
            if (BuildConfig.TESTER_UNLOCK_ENABLED) 10 else 3,
            repository.maxSaveSlots()
        )
    }

    @Test
    fun `a real purchase is independent of the debug toggle`() {
        repository.unlockFullStory()

        repository.setDebugPurchaseSimulated(false)

        assertTrue("turning the debug toggle off must not lock a real purchase", repository.hasUnlockedFullStory())
    }
}
