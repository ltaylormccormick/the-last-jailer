package com.thelastjailer.app.data

import android.content.SharedPreferences
import com.thelastjailer.app.BuildConfig

/**
 * Gates chapter access and save-slot count behind the single "unlock full story" purchase.
 *
 * This is the only place the rest of the app should ask "can the player see this chapter" or
 * "how many save slots do they get" — never check purchase state ad hoc elsewhere. The local,
 * SharedPreferences-backed implementation below can be swapped for a Play Billing-backed one
 * later without touching any caller of this interface.
 */
interface EntitlementRepository {
    /** Chapters I-III are always free; anything past that needs [unlockFullStory]. */
    fun isChapterUnlocked(chapterId: String): Boolean

    fun hasUnlockedFullStory(): Boolean

    /** Grants full-story access (the eventual result of a successful Play Billing purchase). */
    fun unlockFullStory()

    fun maxSaveSlots(): Int

    /** No-op outside a debug build; lets the "unlock full story" purchase be simulated while developing. */
    fun setDebugPurchaseSimulated(enabled: Boolean)
}

class LocalEntitlementRepository(private val prefs: SharedPreferences) : EntitlementRepository {

    override fun isChapterUnlocked(chapterId: String): Boolean {
        val chapterNumber = StoryRepository.chapter(chapterId)?.number
            ?: chapterId.substringAfterLast('_').toIntOrNull()
            ?: 1
        return chapterNumber <= FREE_CHAPTER_LIMIT || hasUnlockedFullStory()
    }

    override fun hasUnlockedFullStory(): Boolean {
        val purchased = prefs.getBoolean(KEY_UNLOCKED, false)
        val debugSimulated = BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_SIMULATE_PURCHASE, false)
        return purchased || debugSimulated
    }

    override fun unlockFullStory() {
        prefs.edit().putBoolean(KEY_UNLOCKED, true).apply()
    }

    override fun maxSaveSlots(): Int = if (hasUnlockedFullStory()) UNLOCKED_SAVE_SLOTS else FREE_SAVE_SLOTS

    override fun setDebugPurchaseSimulated(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().putBoolean(KEY_DEBUG_SIMULATE_PURCHASE, enabled).apply()
    }

    companion object {
        const val FREE_CHAPTER_LIMIT = 3
        const val FREE_SAVE_SLOTS = 3
        const val UNLOCKED_SAVE_SLOTS = 10

        private const val KEY_UNLOCKED = "unlocked_full_story"
        private const val KEY_DEBUG_SIMULATE_PURCHASE = "debug_simulate_purchase"
    }
}
