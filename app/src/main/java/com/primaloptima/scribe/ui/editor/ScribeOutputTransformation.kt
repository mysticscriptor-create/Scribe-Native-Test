package com.primaloptima.scribe.ui.editor

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import com.primaloptima.scribe.engine.FormatRegistry
import com.primaloptima.scribe.engine.FormatType

class ScribeOutputTransformation(
    private val registry: FormatRegistry,
    private val lineStart: Int,
    private val styleFor: (FormatType) -> SpanStyle,
    private val searchRanges: List<IntRange> = emptyList()
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        registry.spansIn(lineStart, lineStart + length).forEach { span ->
            val localStart = (span.start - lineStart).coerceAtLeast(0)
            val localEnd = (span.end - lineStart).coerceAtMost(length)
            if (localStart < localEnd) {
                addStyle(styleFor(span.type), localStart, localEnd)
            }
        }
        searchRanges.forEach { range ->
            val localStart = range.first.coerceAtLeast(0)
            val localEnd = range.last.plus(1).coerceAtMost(length)
            if (localStart < localEnd) {
                addStyle(SpanStyle(background = androidx.compose.ui.graphics.Color.Yellow), localStart, localEnd)
            }
        }
    }
}