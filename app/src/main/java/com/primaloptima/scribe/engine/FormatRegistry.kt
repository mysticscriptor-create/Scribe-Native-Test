package com.primaloptima.scribe.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

enum class FormatType {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, H1, H2, H3, BLOCKQUOTE, SCENE_SEPARATOR;

    fun headingLevel(): Int = when (this) {
        H1 -> 1
        H2 -> 2
        H3 -> 3
        else -> 0
    }
}

data class FormatSpan(val type: FormatType, val start: Int, val end: Int)

sealed class FormatEdit {
    data class Added(val span: FormatSpan) : FormatEdit()
    data class Removed(val spans: List<FormatSpan>) : FormatEdit()
    data class Replaced(val before: List<FormatSpan>, val after: List<FormatSpan>) : FormatEdit()
}

class FormatRegistry {
    private val spans = mutableListOf<FormatSpan>()
    val all: List<FormatSpan> get() = spans.toList()

    fun loadAll(input: List<FormatSpan>) {
        spans.clear()
        spans += input.filter { it.start >= 0 && it.start < it.end }
    }

    fun spansIn(start: Int, end: Int): List<FormatSpan> =
        spans.filter { it.start < end && it.end > start }

    fun addSpan(span: FormatSpan): FormatEdit {
        require(span.start < span.end)
        spans += span
        return FormatEdit.Added(span)
    }

    fun removeSpan(span: FormatSpan): FormatEdit {
        val removed = spans.filter { it == span }
        spans.removeAll(removed.toSet())
        return FormatEdit.Removed(removed)
    }

    fun toggleSpan(type: FormatType, selStart: Int, selEnd: Int): FormatEdit {
        if (selStart >= selEnd) return FormatEdit.Removed(emptyList())
        val before = spans.toList()
        val covering = spans.filter {
            it.type == type && it.start <= selStart && it.end >= selEnd
        }
        if (covering.isNotEmpty()) {
            spans.removeAll(covering.toSet())
        } else {
            spans += FormatSpan(type, selStart, selEnd)
        }
        return FormatEdit.Replaced(before, spans.toList())
    }

    fun adjustForInsert(pos: Int, insertedLength: Int) {
        if (insertedLength == 0) return
        for (index in spans.indices) {
            val span = spans[index]
            spans[index] = when {
                span.start >= pos -> span.copy(
                    start = span.start + insertedLength,
                    end = span.end + insertedLength
                )
                span.end > pos -> span.copy(end = span.end + insertedLength)
                else -> span
            }
        }
    }

    fun adjustForDelete(start: Int, deletedLength: Int) {
        if (deletedLength == 0) return
        val end = start + deletedLength
        val adjusted = spans.mapNotNull { span ->
            when {
                span.end <= start -> span
                span.start >= end -> span.copy(
                    start = span.start - deletedLength,
                    end = span.end - deletedLength
                )
                else -> {
                    val newStart = when {
                        span.start < start -> span.start
                        else -> start
                    }
                    val newEnd = when {
                        span.end > end -> span.end - deletedLength
                        else -> start
                    }
                    if (newStart < newEnd) span.copy(start = newStart, end = newEnd) else null
                }
            }
        }
        spans.clear()
        spans += adjusted
    }

    fun toAnnotatedString(
        text: String,
        lineStart: Int,
        lineEnd: Int,
        styleFor: (FormatType) -> SpanStyle = ::defaultStyle
    ): AnnotatedString {
        val localText = text.substring(lineStart, lineEnd)
        return AnnotatedString.Builder(localText).apply {
            spansIn(lineStart, lineEnd).forEach { span ->
                val start = (span.start - lineStart).coerceAtLeast(0)
                val end = (span.end - lineStart).coerceAtMost(localText.length)
                if (start < end) addStyle(styleFor(span.type), start, end)
            }
        }.toAnnotatedString()
    }
}

private fun defaultStyle(type: FormatType): SpanStyle = when (type) {
    FormatType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    FormatType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    FormatType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    FormatType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    FormatType.BLOCKQUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
    else -> SpanStyle()
}