package com.primaloptima.scribe.ui.editor

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Small writer-friendly input rules. They only alter user edits; programmatic
 * document loads and engine synchronization are intentionally left untouched.
 */
object ScribeInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val inserted = asCharSequence().toString()
        if (inserted.length != 1) return

        when (inserted.single()) {
            '"' -> replace(selection.start, selection.end, "\u201C\u201D")
            '(' -> replace(selection.start, selection.end, "()")
            '[' -> replace(selection.start, selection.end, "[]")
            '{' -> replace(selection.start, selection.end, "{}")
            '`' -> replace(selection.start, selection.end, "``")
            else -> Unit
        }
    }
}