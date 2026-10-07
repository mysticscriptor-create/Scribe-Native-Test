package com.primaloptima.scribe.engine

import com.primaloptima.scribe.util.model.ShortcutAction

/**
 * Universal Smart Enter and Delimiter Exit engine for Scribe.
 *
 * Dynamically links user-created and built-in shortcuts to intelligent typing behaviors:
 * 1. Pair / Enclose Smart Exit:
 *    When typing dialogue, brackets, or markdown formatting, pressing Enter right before
 *    the closing delimiter cleanly jumps past the delimiter and starts a fresh newline,
 *    preventing stranded closing quotes and distorted prose.
 *
 * 2. Prefix / List Smart Continuation & Termination:
 *    When writing lists (numeric, bullet, task, blockquote, or custom headers/notes),
 *    pressing Enter continues the sequence or replicates the prefix.
 *    Pressing Enter on an empty prefix terminates the list and resets indentation cleanly.
 *
 * 3. Auto-Pair Deletion on Backspace:
 *    Pressing Backspace while the cursor sits between an opening and closing pair deletes
 *    both in a single keystroke.
 */
object ScribeSmartEnterEngine {

    data class PairExitResult(
        val jumpPastCol: Int
    )

    /**
     * Inspects if the cursor is positioned directly before a closing delimiter
     * of any active Pair/Wrap shortcut (or standard dialogue/markdown delimiters).
     *
     * Example scenarios:
     * - “Hello world|” + Enter -> cursor jumps past ” to new line!
     * - “|” + Enter -> cursor jumps past ” or starts new line!
     * - **emphasized|** + Enter -> cursor jumps past ** to new line!
     * - [citation|] + Enter -> cursor jumps past ] to new line!
     * - custom pair (e.g. {{ custom| }}) + Enter -> cursor jumps past closing delimiter to new line!
     */
    fun checkPairExit(
        lineStr: String,
        col: Int,
        activeShortcuts: List<ShortcutAction>
    ): PairExitResult? {
        if (col < 0 || col > lineStr.length) return null

        // 1. Gather all active closing delimiters (custom + standard)
        // Sort descending by length so multi-char delimiters ("**", "```", "}}", "*/") match before single-char
        val closings = mutableSetOf(
            "”", "’", "\"", "'", "❞", "❛", "」", "』", "»", "】", "〕", "⟧", "⟩", ")", "]", "}", "`", "**", "~~", "__", "```"
        )

        for (action in activeShortcuts) {
            if (action.kind == "pair" || action.kind == "wrap") {
                val close = action.closing?.ifBlank { null } ?: action.payload
                if (close.isNotBlank()) {
                    closings.add(close)
                }
            }
        }

        val textAfter = lineStr.substring(col)
        for (closing in closings.sortedByDescending { it.length }) {
            if (textAfter.startsWith(closing)) {
                // Cursor is immediately before a closing delimiter
                val jumpCol = col + closing.length
                return PairExitResult(jumpPastCol = jumpCol)
            }
        }

        return null
    }

    /**
     * Parses the current line against built-in rules and custom Prefix shortcuts.
     */
    fun parsePrefix(
        lineStr: String,
        activeShortcuts: List<ShortcutAction>
    ): ScribeListEngine.ListMatch? {
        val customPrefixes = activeShortcuts
            .filter { it.kind == "prefix" && it.payload.isNotBlank() }
            .map { it.payload }

        return ScribeListEngine.parseListLine(lineStr, customPrefixes)
    }

    /**
     * Checks if the cursor is positioned directly between an opening and closing pair
     * on Backspace, enabling single-stroke auto-pair deletion.
     */
    fun checkPairBackspace(
        lineStr: String,
        col: Int,
        activeShortcuts: List<ShortcutAction>
    ): Pair<Int, Int>? {
        if (col <= 0 || col >= lineStr.length) return null

        val pairs = mutableListOf(
            "“" to "”",
            "‘" to "’",
            "\"" to "\"",
            "'" to "'",
            "❝" to "❞",
            "❛" to "❜",
            "「" to "」",
            "『" to "』",
            "«" to "»",
            "【" to "】",
            "〔" to "〕",
            "⟦" to "⟧",
            "⟨" to "⟩",
            "(" to ")",
            "[" to "]",
            "{" to "}",
            "`" to "`",
            "**" to "**",
            "~~" to "~~",
            "__" to "__"
        )

        for (action in activeShortcuts) {
            if (action.kind == "pair" || action.kind == "wrap") {
                val open = action.payload
                val close = action.closing?.ifBlank { null } ?: action.payload
                if (open.isNotBlank() && close.isNotBlank()) {
                    pairs.add(open to close)
                }
            }
        }

        for ((open, close) in pairs.sortedByDescending { it.first.length + it.second.length }) {
            if (col >= open.length && (col + close.length) <= lineStr.length) {
                val before = lineStr.substring(col - open.length, col)
                val after = lineStr.substring(col, col + close.length)
                if (before == open && after == close) {
                    return Pair(open.length, close.length)
                }
            }
        }
        return null
    }
}
