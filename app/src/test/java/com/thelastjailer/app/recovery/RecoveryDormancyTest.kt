package com.thelastjailer.app.recovery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G2 is dormant by contract: nothing in the app may use the recovery package yet, and the package may not depend on
 * Android. These tests read the source tree (Gradle runs unit tests with the module directory as the working
 * directory) and fail closed if they cannot find it.
 */
class RecoveryDormancyTest {

    private val mainRoot = File("src/main/java/com/thelastjailer/app")

    private fun kotlinFiles(dir: File): List<File> = dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `the source tree is found`() {
        assertTrue("run from the module directory: ${File(".").absolutePath}", mainRoot.isDirectory)
        assertTrue(kotlinFiles(mainRoot).size > 20)
    }

    @Test
    fun `no production code outside the recovery package refers to it`() {
        val recoveryDir = File(mainRoot, "recovery").canonicalFile
        val offenders = kotlinFiles(mainRoot).filter { !it.canonicalPath.startsWith(recoveryDir.path + File.separator) }
            .filter { f ->
                val text = f.readText()
                text.contains("thelastjailer.app.recovery") ||
                    listOf("RecoveryBundleStore", "RecoveryCodec", "BundleRequest", "RecoveryFileOps", "BufferSelection").any { text.contains(it) }
            }
        assertEquals("these files use the dormant recovery package: ${offenders.map { it.name }}", emptyList<File>(), offenders)
    }

    @Test
    fun `the recovery package has no Android dependency`() {
        val files = kotlinFiles(File(mainRoot, "recovery"))
        assertEquals(3, files.size)
        for (f in files) {
            val imports = f.readLines().filter { it.startsWith("import ") }
            assertFalse("${f.name} imports android: ${imports.filter { it.startsWith("import android.") }}", imports.any { it.startsWith("import android.") || it.startsWith("import androidx.") })
        }
    }

    @Test
    fun `the recovery package never touches the live save or its preference files`() {
        for (f in kotlinFiles(File(mainRoot, "recovery"))) {
            val code = f.readLines().filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }.joinToString("\n")
            for (forbidden in listOf("SharedPreferences", "getSharedPreferences", "jailer_saves", "jailer_ckpt", "SaveStore", "CheckpointStore")) {
                assertFalse("${f.name} must not reference $forbidden", code.contains(forbidden))
            }
        }
    }
}
