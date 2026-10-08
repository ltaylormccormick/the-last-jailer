package com.thelastjailer.app.ui

/** Unwrap source-formatting line breaks, retaining authored paragraph boundaries. */
internal fun readingText(text: String): String = text.trim()
    .split(Regex("\\n[ \t]*\\n"))
    .joinToString("\n\n") { paragraph ->
        paragraph.lines().joinToString(" ") { it.trim() }
    }
