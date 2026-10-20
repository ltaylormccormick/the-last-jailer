package com.thelastjailer.app.saveguard

import com.thelastjailer.app.StrictFakeSharedPreferences
import com.thelastjailer.app.data.LocalEntitlementRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveClassifierEntitlementTest {

    private val classifier = SaveClassifier()
    private fun read(p: StrictFakeSharedPreferences) = classifier.classifyEntitlements(p) as EntitlementReport.Read

    @Test
    fun `absent flags are ABSENT`() {
        val r = read(StrictFakeSharedPreferences())
        assertEquals(FlagState.ABSENT, r.fullStory); assertEquals(FlagState.ABSENT, r.testerUnlock); assertEquals(FlagState.ABSENT, r.debugSimulated)
    }

    @Test
    fun `true and false are reported as they are`() {
        assertEquals(FlagState.TRUE, read(StrictFakeSharedPreferences().putRaw(SaveClassifier.KEY_UNLOCKED, true)).fullStory)
        assertEquals(FlagState.FALSE, read(StrictFakeSharedPreferences().putRaw(SaveClassifier.KEY_UNLOCKED, false)).fullStory)
    }

    @Test
    fun `a wrong-typed flag is WRONG_TYPE and is never read as purchased or not purchased`() {
        for (bad in listOf<Any>("true", 1, 1L, 1.0f, setOf("true"))) {
            val r = read(StrictFakeSharedPreferences().putRaw(SaveClassifier.KEY_UNLOCKED, bad))
            assertEquals("$bad", FlagState.WRONG_TYPE, r.fullStory)
        }
    }

    @Test
    fun `the flag the real repository writes classifies TRUE`() {
        val p = StrictFakeSharedPreferences()
        LocalEntitlementRepository(p).unlockFullStory()
        assertEquals(FlagState.TRUE, read(p).fullStory)
    }

    @Test
    fun `the classifier's flag keys are the literals the repository uses`() {
        val source = File("src/main/java/com/thelastjailer/app/data/EntitlementRepository.kt").readText()
        for (k in listOf(SaveClassifier.KEY_UNLOCKED, SaveClassifier.KEY_TESTER_UNLOCKED, SaveClassifier.KEY_DEBUG_SIMULATE_PURCHASE)) {
            assertTrue("EntitlementRepository no longer contains \"$k\"", source.contains("\"$k\""))
        }
    }

    @Test
    fun `the real repository crashes on a wrong-typed flag today, and the classifier does not`() {
        val p = StrictFakeSharedPreferences().putRaw(SaveClassifier.KEY_UNLOCKED, "yes")
        try { LocalEntitlementRepository(p).hasUnlockedFullStory(); throw AssertionError("expected ClassCastException") }
        catch (e: ClassCastException) { /* CURRENT behaviour, changed by Stage B+C */ }
        assertEquals(FlagState.WRONG_TYPE, read(p).fullStory)
    }
}
