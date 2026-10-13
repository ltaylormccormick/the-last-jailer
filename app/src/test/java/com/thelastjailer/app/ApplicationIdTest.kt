package com.thelastjailer.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the QA-build arrangement. CI runs the unit tests for the debug, release and internalTest
 * variants, and BuildConfig.APPLICATION_ID is generated per variant, so between them these tests prove:
 *  - the debug build installs beside production with its own id (so it has its own saves), and
 *  - release and internalTest (the Play and tester builds) keep the production id.
 */
class ApplicationIdTest {

    @Test
    fun `only the debug build uses the separate QA application id`() {
        val expected = if (BuildConfig.DEBUG) "com.thelastjailer.app.debug" else "com.thelastjailer.app"
        assertEquals(expected, BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `the code namespace is unchanged so no source file depends on the application id`() {
        assertEquals("com.thelastjailer.app", BuildConfig::class.java.`package`.name)
    }
}
