package com.primaloptima.scribe.engine

enum class Source { ORIGINAL, APPEND }

data class Piece(
    val source: Source,
    val start: Int,
    val length: Int
)

sealed class Edit {
    data class Insert(val pos: Int, val length: Int, val text: String) : Edit()
    data class Delete(val start: Int, val end: Int, val text: String) : Edit()
    data class Compound(val edits: List<Edit>) : Edit()
}

/**
 * A deliberately small piece table for long prose documents. The original
 * buffer is immutable and every new insertion is appended to appendBuffer.
 * Mutations are main-thread-only; callers should pass asString() snapshots to
 * background work.
 */
class DocumentBuffer(initialContent: String) {
    private val original = initialContent
    private val appendBuffer = StringBuilder()
    private val pieces = mutableListOf<Piece>()

    private var cachedPieceIndex = -1
    private var cachedPieceStart = 0
    private var lineStarts = intArrayOf(0)

    init {
        if (initialContent.isNotEmpty()) {
            pieces += Piece(Source.ORIGINAL, 0, initialContent.length)
        }
        rebuildLineIndex()
    }

    fun length(): Int = pieces.sumOf(Piece::length)
    fun lineCount(): Int = lineStarts.size
    fun isEmpty(): Boolean = length() == 0

    fun charAt(pos: Int): Char {
        require(pos in 0 until length()) { "Position $pos is outside the document" }
        val cached = pieces.getOrNull(cachedPieceIndex)
        if (cached != null && pos in cachedPieceStart until cachedPieceStart + cached.length) {
            return sourceText(cached)[pos - cachedPieceStart]
        }

        var offset = 0
        pieces.forEachIndexed { index, piece ->
            if (pos < offset + piece.length) {
                cachedPieceIndex = index
                cachedPieceStart = offset
                return sourceText(piece)[pos - offset]
            }
            offset += piece.length
        }
        error("Piece lookup failed")
    }

    fun substring(start: Int, end: Int): String {
        require(start >= 0 && end >= start && end <= length()) {
            "Invalid range [$start, $end)"
        }
        if (start == end) return ""

        val result = StringBuilder(end - start)
        var offset = 0
        for (piece in pieces) {
            val pieceEnd = offset + piece.length
            if (pieceEnd <= start) {
                offset = pieceEnd
                continue
            }
            if (offset >= end) break
            val localStart = maxOf(start, offset) - offset
            val localEnd = minOf(end, pieceEnd) - offset
            if (localStart < localEnd) result.append(sourceText(piece), localStart, localEnd)
            offset = pieceEnd
        }
        return result.toString()
    }

    fun asString(): String = buildString(length()) {
        pieces.forEach { append(sourceText(it)) }
    }

    fun insert(pos: Int, text: String): Edit.Insert {
        require(pos in 0..length()) { "Position $pos is outside the document" }
        require(text.isNotEmpty()) { "Inserted text cannot be empty" }

        val appendStart = appendBuffer.length
        appendBuffer.append(text)
        val added = Piece(Source.APPEND, appendStart, text.length)

        if (pieces.isEmpty()) {
            pieces += added
        } else {
            var offset = 0
            var index = pieces.lastIndex
            var before = pieces.last().length
            pieces.forEachIndexed { candidateIndex, piece ->
                val end = offset + piece.length
                if (pos <= end) {
                    index = candidateIndex
                    before = pos - offset
                    return@forEachIndexed
                }
                offset = end
            }
            val piece = pieces.removeAt(index)
            val replacement = buildList {
                if (before > 0) add(piece.copy(length = before))
                add(added)
                val after = piece.length - before
                if (after > 0) add(piece.copy(start = piece.start + before, length = after))
            }
            pieces.addAll(index, replacement)
        }
        invalidate()
        rebuildLineIndex()
        return Edit.Insert(pos, text.length, text)
    }

    fun delete(start: Int, end: Int): Edit.Delete {
        require(start >= 0 && end >= start && end <= length()) {
            "Invalid range [$start, $end)"
        }
        val removed = substring(start, end)
        if (start == end) return Edit.Delete(start, end, removed)

        val result = mutableListOf<Piece>()
        var offset = 0
        for (piece in pieces) {
            val pieceEnd = offset + piece.length
            if (pieceEnd <= start || offset >= end) {
                result += piece
            } else {
                val before = maxOf(0, start - offset)
                val after = maxOf(0, pieceEnd - end)
                if (before > 0) result += piece.copy(length = before)
                if (after > 0) result += piece.copy(
                    start = piece.start + piece.length - after,
                    length = after
                )
            }
            offset = pieceEnd
        }
        pieces.clear()
        pieces += coalesce(result)
        invalidate()
        rebuildLineIndex()
        return Edit.Delete(start, end, removed)
    }

    fun search(query: String, caseSensitive: Boolean, isRegex: Boolean): List<Int> {
        if (query.isEmpty()) return emptyList()
        val text = asString()
        if (isRegex) {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            return runCatching {
                Regex(query, options).findAll(text).map { it.range.first }.toList()
            }.getOrDefault(emptyList())
        }
        val needle = if (caseSensitive) query else query.lowercase()
        val haystack = if (caseSensitive) text else text.lowercase()
        val result = mutableListOf<Int>()
        var from = 0
        while (from <= haystack.length - needle.length) {
            val index = haystack.indexOf(needle, from)
            if (index < 0) break
            result += index
            from = index + maxOf(needle.length, 1)
        }
        return result
    }

    fun lineStart(lineIndex: Int): Int {
        require(lineIndex in lineStarts.indices) { "Invalid line index $lineIndex" }
        return lineStarts[lineIndex]
    }

    fun lineIndexAt(pos: Int): Int {
        require(pos in 0..length()) { "Position $pos is outside the document" }
        var low = 0
        var high = lineStarts.lastIndex
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lineStarts[middle] <= pos) low = middle + 1 else high = middle - 1
        }
        return high.coerceAtLeast(0)
    }

    fun lineContent(lineIndex: Int): String {
        val start = lineStart(lineIndex)
        val end = if (lineIndex + 1 < lineCount()) lineStart(lineIndex + 1) else length()
        return substring(start, end).removeSuffix("\n")
    }

    private fun sourceText(piece: Piece): CharSequence =
        (if (piece.source == Source.ORIGINAL) original else appendBuffer)
            .subSequence(piece.start, piece.start + piece.length)

    private fun rebuildLineIndex() {
        val starts = ArrayList<Int>()
        starts += 0
        var offset = 0
        pieces.forEach { piece ->
            val text = sourceText(piece)
            text.forEachIndexed { index, char ->
                if (char == '\n') starts += offset + index + 1
            }
            offset += piece.length
        }
        lineStarts = starts.toIntArray()
    }

    private fun invalidate() {
        cachedPieceIndex = -1
        cachedPieceStart = 0
    }

    private fun coalesce(input: List<Piece>): List<Piece> {
        val output = mutableListOf<Piece>()
        input.filter { it.length > 0 }.forEach { piece ->
            val previous = output.lastOrNull()
            if (
                previous != null &&
                previous.source == piece.source &&
                previous.start + previous.length == piece.start
            ) {
                output[output.lastIndex] = previous.copy(length = previous.length + piece.length)
            } else {
                output += piece
            }
        }
        return output
    }
}