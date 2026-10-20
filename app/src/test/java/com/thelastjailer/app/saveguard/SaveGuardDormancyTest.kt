package com.thelastjailer.app.saveguard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage A is dormant and read-only by contract. These tests read the source tree (Gradle runs unit tests with the
 * module directory as the working directory) and fail closed if they cannot find it.
 */
class SaveGuardDormancyTest {

    private val mainRoot = File("src/main/java/com/thelastjailer/app")
    private val guardDir = File(mainRoot, "saveguard")

    private fun kotlinFiles(dir: File): List<File> = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun codeOf(f: File): String = f.readLines()
        .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }
        .joinToString("\n")

    @Test
    fun `the source tree is found`() {
        assertTrue("run from the module directory: ${File(".").absolutePath}", guardDir.isDirectory)
        assertTrue(kotlinFiles(mainRoot).size > 20)
    }

    @Test
    fun `no production code outside the saveguard package refers to it`() {
        val guardCanonical = guardDir.canonicalFile
        val offenders = kotlinFiles(mainRoot).filter { !it.canonicalPath.startsWith(guardCanonical.path + File.separator) }
            .filter { f ->
                val text = f.readText()
                text.contains("thelastjailer.app.saveguard") ||
                    listOf("SaveClassifier", "SaveReport", "StartupDecision", "SaveLayout").any { text.contains(it) }
            }
        assertEquals("these files use the dormant classifier: ${offenders.map { it.name }}", emptyList<File>(), offenders)
    }

    @Test
    fun `the saveguard package contains only the classifier`() {
        // Stage B+C (write gate, quarantine) will add files on purpose, in their own reviewed change.
        assertEquals(listOf("SaveClassifier.kt"), kotlinFiles(guardDir).map { it.name }.sorted())
    }

    @Test
    fun `the classifier performs no write, file or other side-effecting call`() {
        for (f in kotlinFiles(guardDir)) {
            val code = codeOf(f)
            for (forbidden in listOf(
                ".edit(", ".apply()", ".commit()", ".putString", ".putInt", ".putLong", ".putFloat", ".putBoolean", ".putStringSet",
                ".remove(", ".clear(", "java.io", "java.nio", "getSharedPreferences", "SaveStore", "CheckpointStore", "BillingRepository",
                "Log.", "println", "System."
            )) {
                assertFalse("${f.name} must not contain $forbidden", code.contains(forbidden))
            }
        }
    }

    @Test
    fun `the classifier's only Android dependency is the SharedPreferences interface`() {
        for (f in kotlinFiles(guardDir)) {
            val imports = f.readLines().filter { it.startsWith("import ") }
            val android = imports.filter { it.startsWith("import android.") || it.startsWith("import androidx.") }
            assertEquals(listOf("import android.content.SharedPreferences"), android)
        }
    }

    @Test
    fun `the classifier calls no typed getter, so it cannot hit a ClassCastException`() {
        for (f in kotlinFiles(guardDir)) {
            val code = codeOf(f)
            for (getter in listOf(".getInt(", ".getString(", ".getLong(", ".getFloat(", ".getBoolean(", ".getStringSet(")) {
                assertFalse("${f.name} must not call $getter", code.contains(getter))
            }
        }
    }
}
