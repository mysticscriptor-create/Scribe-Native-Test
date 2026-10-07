package com.primaloptima.scribe.engine

/**
 * High-performance, robust list detection and continuation engine for Scribe.
 * Supports:
 * - Unordered bullet lists: `- `, `* `, `+ `, `• `, `◦ `, `▪ `, `▫ `, `– `, `— `, `✦ `, `✧ `, `★ `, `☆ `, `⁃ `, `‣ `
 * - Ordered numeric lists: `1. `, `1) `, `1 - `, `42. `, `99) `
 * - Ordered alphabetic lists: `a. `, `b. `, `a) `, `A. `, `B) `
 * - Task / Checklists: `- [ ] `, `- [x] `, `* [ ] `, `+ [ ] `, `[ ] `, `[x] `, `- [-] `
 * - Blockquotes & Callouts: `> `, `>> `, `> [!NOTE]`
 * - Custom user-defined prefix shortcuts: `// `, `NOTE: `, `Q: `, `A: `, `TODO: `, etc.
 *
 * Handles:
 * - Sequence incrementation (e.g. 1. -> 2., 9. -> 10., a. -> b.)
 * - Sub-list indentation preservation (e.g. "   - " -> "   - ")
 * - List termination on empty marker (double Enter)
 * - Single-stroke marker deletion on Backspace
 */
object ScribeListEngine {

    data class ListMatch(
        val leadingWhitespace: String,
        val marker: String,
        val fullPrefix: String,
        val isOrdered: Boolean = false,
        val orderNumber: Long = 0L,
        val orderDelimiter: Char = '.',
        val isAlphaOrdered: Boolean = false,
        val alphaMarker: String = "",
        val isTask: Boolean = false,
        val isBlockquote: Boolean = false,
        val isCustom: Boolean = false,
        val customContinuation: String? = null
    )

    private val TASK_REGEX = Regex("""^([ \t]*)([-*+•]\s+\[[ xX\-/]\]\s*|\[[ xX\-/]\]\s*)""")
    private val ORDERED_NUMERIC_REGEX = Regex("""^([ \t]*)(\d+)([.)\-:]\s+)""")
    private val ORDERED_ALPHA_REGEX = Regex("""^([ \t]*)([a-zA-Z])([.)]\s+)""")
    private val UNORDERED_REGEX = Regex("""^([ \t]*)([-*+•◦▪▫–—✦✧★☆⁃‣]\s+)""")
    private val BLOCKQUOTE_REGEX = Regex("""^([ \t]*)(>+\s*|>\s*\[![a-zA-Z0-9_-]+\]\s*)""")

    /**
     * Backward-compatible overload for callers passing only lineStr.
     */
    fun parseListLine(lineStr: String): ListMatch? = parseListLine(lineStr, emptyList())

    /**
     * Inspects line text and determines if it starts with an active list marker,
     * blockquote, or user-defined custom prefix shortcut.
     */
    fun parseListLine(lineStr: String, customPrefixes: List<String> = emptyList()): ListMatch? {
        if (lineStr.isEmpty()) return null

        // 1. Task lists: e.g. "- [ ] ", "* [x] ", "[ ] "
        TASK_REGEX.find(lineStr)?.let { match ->
            val ws = match.groupValues[1]
            val marker = match.groupValues[2]
            return ListMatch(
                leadingWhitespace = ws,
                marker = marker,
                fullPrefix = ws + marker,
                isTask = true
            )
        }

        // 2. Ordered Numeric lists: e.g. "1. ", "12) ", "1 - "
        ORDERED_NUMERIC_REGEX.find(lineStr)?.let { match ->
            val ws = match.groupValues[1]
            val numStr = match.groupValues[2]
            val delimWithSpace = match.groupValues[3]
            val num = numStr.toLongOrNull() ?: 1L
            val delim = delimWithSpace.firstOrNull() ?: '.'
            val marker = "$numStr$delimWithSpace"
            return ListMatch(
                leadingWhitespace = ws,
                marker = marker,
                fullPrefix = ws + marker,
                isOrdered = true,
                orderNumber = num,
                orderDelimiter = delim
            )
        }

        // 3. Ordered Alphabetic lists: e.g. "a. ", "B) "
        ORDERED_ALPHA_REGEX.find(lineStr)?.let { match ->
            val ws = match.groupValues[1]
            val alphaStr = match.groupValues[2]
            val delimWithSpace = match.groupValues[3]
            val delim = delimWithSpace.firstOrNull() ?: '.'
            val marker = "$alphaStr$delimWithSpace"
            return ListMatch(
                leadingWhitespace = ws,
                marker = marker,
                fullPrefix = ws + marker,
                isAlphaOrdered = true,
                alphaMarker = alphaStr,
                orderDelimiter = delim
            )
        }

        // 4. Unordered bullet lists: e.g. "- ", "* ", "+ ", "• ", "– "
        UNORDERED_REGEX.find(lineStr)?.let { match ->
            val ws = match.groupValues[1]
            val marker = match.groupValues[2]
            return ListMatch(
                leadingWhitespace = ws,
                marker = marker,
                fullPrefix = ws + marker
            )
        }

        // 5. Blockquotes & Callouts: e.g. "> ", ">> ", "> [!NOTE]"
        BLOCKQUOTE_REGEX.find(lineStr)?.let { match ->
            val ws = match.groupValues[1]
            val marker = match.groupValues[2]
            return ListMatch(
                leadingWhitespace = ws,
                marker = marker,
                fullPrefix = ws + marker,
                isBlockquote = true
            )
        }

        // 6. Custom dynamic prefixes from active shortcuts
        if (customPrefixes.isNotEmpty()) {
            val ws = lineStr.takeWhile { it == ' ' || it == '\t' }
            val lineWithoutIndent = lineStr.substring(ws.length)

            for (prefix in customPrefixes.sortedByDescending { it.length }) {
                val trimmedPrefix = prefix.trimEnd()
                val matches = lineWithoutIndent.startsWith(prefix) ||
                    (prefix.endsWith(" ") && lineWithoutIndent.startsWith(trimmedPrefix) &&
                        (lineWithoutIndent.length == trimmedPrefix.length || lineWithoutIndent[trimmedPrefix.length] == ' '))

                if (matches) {
                    val actualMarker = if (lineWithoutIndent.startsWith(prefix)) prefix else "$trimmedPrefix "
                    
                    // Check if custom prefix ends with a number (e.g. "Step 1. " or "1) ")
                    val numMatch = Regex("""^(.*?)(\d+)([.)\-:]\s*)$""").find(actualMarker)
                    if (numMatch != null) {
                        val prefixHead = numMatch.groupValues[1]
                        val num = numMatch.groupValues[2].toLongOrNull() ?: 1L
                        val delim = numMatch.groupValues[3]
                        return ListMatch(
                            leadingWhitespace = ws,
                            marker = actualMarker,
                            fullPrefix = ws + actualMarker,
                            isOrdered = true,
                            orderNumber = num,
                            customContinuation = "$prefixHead${num + 1}$delim",
                            isCustom = true
                        )
                    }

                    return ListMatch(
                        leadingWhitespace = ws,
                        marker = actualMarker,
                        fullPrefix = ws + actualMarker,
                        isCustom = true,
                        customContinuation = actualMarker
                    )
                }
            }
        }

        return null
    }

    /**
     * Given an existing match, generates the next sequence prefix for smart continuation.
     */
    fun nextSequencePrefix(match: ListMatch): String {
        return when {
            match.isAlphaOrdered -> {
                val nextAlpha = incrementAlpha(match.alphaMarker)
                "${match.leadingWhitespace}$nextAlpha${match.orderDelimiter} "
            }
            match.isOrdered && match.customContinuation != null -> {
                "${match.leadingWhitespace}${match.customContinuation}"
            }
            match.isOrdered -> {
                val nextNum = match.orderNumber + 1
                "${match.leadingWhitespace}$nextNum${match.orderDelimiter} "
            }
            match.isTask -> {
                // Next item in task list is always an unchecked box
                val taskBox = if (match.marker.startsWith("[")) "[ ] " else "- [ ] "
                "${match.leadingWhitespace}$taskBox"
            }
            match.isBlockquote -> {
                "${match.leadingWhitespace}> "
            }
            match.customContinuation != null -> {
                "${match.leadingWhitespace}${match.customContinuation}"
            }
            else -> {
                // Unordered bullet or standard prefix
                "${match.leadingWhitespace}${match.marker}"
            }
        }
    }

    private fun incrementAlpha(alpha: String): String {
        if (alpha.isEmpty()) return "a"
        val lastChar = alpha.last()
        return when {
            lastChar in 'a'..'y' -> alpha.dropLast(1) + (lastChar + 1)
            lastChar == 'z' -> "aa"
            lastChar in 'A'..'Y' -> alpha.dropLast(1) + (lastChar + 1)
            lastChar == 'Z' -> "AA"
            else -> alpha
        }
    }
}
