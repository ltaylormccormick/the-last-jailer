package com.thelastjailer.app.saveguard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stages A and B1 are dormant by contract: nothing in the shipped app calls any of this. These tests read the source
 * tree (Gradle runs unit tests with the module directory as the working directory) and fail closed if they cannot
 * find it. B2 wired only the write gate (GameState.kt); the gate stays permanently open.
 */
class SaveGuardDormancyTest {

    private val mainRoot = File("src/main/java/com/thelastjailer/app")
    private val guardDir = File(mainRoot, "saveguard")

    private fun kotlinFiles(dir: File): List<File> = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun codeOf(f: File): String = f.readLines()
        .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }
        .joinToString("\n") { it.replace(Regex("\\s//.*$"), "") }

    private fun rel(f: File): String = f.relativeTo(guardDir).path.replace(File.separatorChar, '/')

    private val allowedFiles = setOf(
        "SaveClassifier.kt",
        "raw/RawPrefsShape.kt", "raw/RawFileAssessor.kt",
        "gate/SaveGate.kt",
        "quarantine/QuarantineFileOps.kt", "quarantine/SnapshotFormat.kt", "quarantine/QuarantineStore.kt"
    )

    @Test
    fun `the source tree is found`() {
        assertTrue("run from the module directory: ${File(".").absolutePath}", guardDir.isDirectory)
        assertTrue(kotlinFiles(mainRoot).size > 20)
    }

    @Test
    fun `no production code outside the saveguard package refers to it`() {
        val guardCanonical = guardDir.canonicalFile
        val names = listOf(
            "SaveClassifier", "SaveReport", "StartupDecision", "SaveLayout", "RawPrefsShape", "RawFileAssessor", "RawCrossCheck", "SaveGate",
            "GuardedEditor", "GateMode", "WriteIntent", "QuarantineStore", "QuarantineFileOps", "JavaQuarantineFileOps", "SnapshotMeta", "SourceFiles"
        )
        // B2: only GameState.kt (SaveStore) may use the write gate - SaveGate, GateMode, GateOutcome, WriteIntent.
        // Everything else stays dormant: raw assessment, quarantine, the classifier, startup decisions.
        val gateNames = setOf("SaveGate", "GuardedEditor", "GateMode", "WriteIntent")
        val offenders = kotlinFiles(mainRoot).filter { !it.canonicalPath.startsWith(guardCanonical.path + File.separator) }
            .filter { f ->
                val text = f.readText()
                if (f.name == "GameState.kt") {
                    val imports = f.readLines().filter { it.startsWith("import com.thelastjailer.app.saveguard") }
                    val allowedImports = setOf(
                        "import com.thelastjailer.app.saveguard.gate.GateMode",
                        "import com.thelastjailer.app.saveguard.gate.GateOutcome",
                        "import com.thelastjailer.app.saveguard.gate.SaveGate",
                        "import com.thelastjailer.app.saveguard.gate.WriteIntent"
                    )
                    imports.any { it !in allowedImports } || text.contains("saveguard.raw") || text.contains("saveguard.quarantine") ||
                        (names.filter { it !in gateNames }).any { text.contains(it) }
                } else {
                    text.contains("thelastjailer.app.saveguard") || names.any { text.contains(it) }
                }
            }
        assertEquals("these files use the dormant components: ${offenders.map { it.name }}", emptyList<File>(), offenders)
    }

    @Test
    fun `the saveguard package contains exactly the reviewed files`() {
        // A new file here is a new component: add it on purpose, with its own rules below.
        assertEquals(allowedFiles, kotlinFiles(guardDir).map { rel(it) }.toSet())
    }

    @Test
    fun `no file anywhere in the package logs, prints, reads the clock or touches the preferences factory`() {
        for (f in kotlinFiles(guardDir)) {
            val code = codeOf(f)
            for (forbidden in listOf("Log.", "println", "System.", "Timber", "getSharedPreferences", "SaveStore", "CheckpointStore", "BillingRepository", "Context")) {
                assertFalse("${rel(f)} must not contain $forbidden", Regex("(^|[^A-Za-z])" + Regex.escape(forbidden)).containsMatchIn(code))
            }
        }
    }

    @Test
    fun `the classifier and the raw and gate components are pure or only use the preferences interface`() {
        for (f in kotlinFiles(guardDir).filter { !rel(it).startsWith("quarantine/") }) {
            val code = codeOf(f)
            val imports = f.readLines().filter { it.startsWith("import ") }
            val android = imports.filter { it.startsWith("import android.") || it.startsWith("import androidx.") }
            if (rel(f).startsWith("raw/")) assertEquals("${rel(f)} must not import Android", emptyList<String>(), android)
            else assertEquals(listOf("import android.content.SharedPreferences"), android)
            for (forbidden in listOf("java.io.File", "java.nio", "FileInputStream", "FileOutputStream", "RandomAccessFile")) {
                assertFalse("${rel(f)} must not contain $forbidden", code.contains(forbidden))
            }
        }
    }

    @Test
    fun `the classifier performs no write and no typed read`() {
        val code = codeOf(File(guardDir, "SaveClassifier.kt"))
        for (forbidden in listOf(".edit(", ".apply()", ".commit()", ".putString", ".putInt", ".putLong", ".putFloat", ".putBoolean", ".putStringSet", ".remove(", ".clear(")) {
            assertFalse("SaveClassifier.kt must not contain $forbidden", code.contains(forbidden))
        }
        for (getter in listOf(".getInt(", ".getString(", ".getLong(", ".getFloat(", ".getBoolean(", ".getStringSet(")) {
            assertFalse("SaveClassifier.kt must not call $getter", code.contains(getter))
        }
    }

    @Test
    fun `the gate and the raw components never read a typed value and never create an editor themselves`() {
        for (f in kotlinFiles(guardDir).filter { rel(it).startsWith("gate/") || rel(it).startsWith("raw/") }) {
            val code = codeOf(f)
            for (forbidden in listOf(".getInt(", ".getString(", ".getLong(", ".getFloat(", ".getBoolean(", ".getStringSet(", ".edit(", ".getAll(", ".all\n")) {
                assertFalse("${rel(f)} must not contain $forbidden", code.contains(forbidden))
            }
        }
    }

    @Test
    fun `the quarantine package has no Android dependency and cannot rename, overwrite or delete a tree`() {
        for (f in kotlinFiles(File(guardDir, "quarantine"))) {
            val code = codeOf(f)
            val imports = f.readLines().filter { it.startsWith("import ") }
            assertEquals("${rel(f)} must not import Android", emptyList<String>(), imports.filter { it.startsWith("import android.") || it.startsWith("import androidx.") })
            for (forbidden in listOf(
                "renameTo", "Files.move", "Files.copy", "copyTo", "copyRecursively", "deleteRecursively", "walkFileTree", "File.createTempFile",
                "writeBytes", "writeText", "appendText", "FileWriter", "TRUNCATE_EXISTING", "StandardCopyOption", "REPLACE_EXISTING", "createDirectories", "mkdirs("
            )) {
                assertFalse("${rel(f)} must not contain $forbidden", code.contains(forbidden))
            }
            // java.nio.file lives only behind the seam; the store and the format code are filesystem-agnostic
            if (f.name != "QuarantineFileOps.kt") {
                assertFalse("${rel(f)} must not use java.nio.file.Files", code.contains("Files."))
                assertFalse("${rel(f)} must not open streams", code.contains("InputStream") || code.contains("OutputStream"))
            }
        }
    }

    @Test
    fun `the seam has no rename operation and its only deletions are narrow`() {
        val code = codeOf(File(guardDir, "quarantine/QuarantineFileOps.kt"))
        val interfaceBody = code.substringAfter("interface QuarantineFileOps").substringBefore("class JavaQuarantineFileOps")
        assertFalse(interfaceBody.contains("rename") || interfaceBody.contains("move"))
        assertEquals(setOf("deleteFile", "deleteEmptyDirectory"), Regex("fun (delete\\w*)\\(").findAll(interfaceBody).map { it.groupValues[1] }.toSet())
        assertTrue(code.contains("NOFOLLOW_LINKS"))
        assertTrue(code.contains("CREATE_NEW"))
    }

    @Test
    fun `the store deletes only through the two narrow operations`() {
        val code = codeOf(File(guardDir, "quarantine/QuarantineStore.kt"))
        val deletes = Regex("ops\\.(delete\\w*)\\(").findAll(code).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("deleteFile", "deleteEmptyDirectory"), deletes)
        assertFalse(code.contains("java.io.File(") && code.contains(".delete()"))
        assertFalse("direct File.delete bypasses the seam", Regex("\\.delete\\(\\)").containsMatchIn(code))
    }

    @Test
    fun `no sentence in a result type or a message can carry saved content`() {
        // Results are enums, counts and reason codes. This guards the obvious regression: a message built from content.
        for (f in kotlinFiles(guardDir)) {
            val code = codeOf(f)
            assertFalse("${rel(f)} must not build an exception message from content", Regex("(Exception|Error)\\([^)]*\\$\\{?(key|value|bytes|name|text|sceneId)").containsMatchIn(code))
        }
    }
}
