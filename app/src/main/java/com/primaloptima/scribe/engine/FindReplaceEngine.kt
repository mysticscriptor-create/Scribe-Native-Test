package com.primaloptima.scribe.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

data class SearchResult(
    val lineIndex: Int,
    val lineLocalStart: Int,
    val lineLocalEnd: Int,
    val docOffset: Int
)

class FindReplaceEngine(private val engine: ScribeEditorEngine) {
    private val _results = mutableStateListOf<SearchResult>()
    val results: List<SearchResult> get() = _results
    var currentIndex by mutableIntStateOf(-1)
        private set

    private var query = ""
    private var caseSensitive = true
    private var isRegex = false

    fun search(query: String, caseSensitive: Boolean, isRegex: Boolean) {
        this.query = query
        this.caseSensitive = caseSensitive
        this.isRegex = isRegex
        _results.clear()
        if (query.isEmpty()) {
            currentIndex = -1
            return
        }

        engine.buffer.search(query, caseSensitive, isRegex).forEach { offset ->
            val lineIndex = engine.buffer.lineIndexAt(offset)
            val lineStart = engine.buffer.lineStart(lineIndex)
            val length = if (isRegex) {
                Regex(query, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
                    .find(engine.buffer.asString(), offset)?.value?.length ?: query.length
            } else query.length
            _results += SearchResult(
                lineIndex = lineIndex,
                lineLocalStart = offset - lineStart,
                lineLocalEnd = offset - lineStart + length,
                docOffset = offset
            )
        }
        currentIndex = if (_results.isEmpty()) -1 else 0
    }

    fun goToNext() {
        if (_results.isNotEmpty()) currentIndex = (currentIndex + 1) % _results.size
    }

    fun goToPrevious() {
        if (_results.isNotEmpty()) {
            currentIndex = (_results.size + currentIndex - 1) % _results.size
        }
    }

    fun replaceAll(replacement: String) {
        if (_results.isEmpty()) return
        _results.asReversed().forEach { result ->
            engine.replaceRange(result.docOffset, result.docOffset + (result.lineLocalEnd - result.lineLocalStart), replacement)
        }
        search(query, caseSensitive, isRegex)
    }
}