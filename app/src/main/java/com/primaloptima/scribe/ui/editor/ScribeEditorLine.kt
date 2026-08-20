package com.primaloptima.scribe.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.primaloptima.scribe.engine.FormatRegistry
import com.primaloptima.scribe.engine.FormatType
import com.primaloptima.scribe.engine.ScribeEditorEngine

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ScribeEditorLine(
    lineIndex: Int,
    engine: ScribeEditorEngine,
    textStyle: TextStyle,
    cursorBrush: Brush,
    registry: FormatRegistry,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val state = remember(lineIndex) { TextFieldState(engine.lineSnapshot(lineIndex)) }

    LaunchedEffect(lineIndex, engine.lineCount.value) {
        val latest = engine.lineSnapshot(lineIndex)
        if (state.text.toString() != latest) {
            state.edit {
                replace(0, length, latest)
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { value ->
            if (value != engine.lineSnapshot(lineIndex)) {
                engine.onLineChanged(lineIndex, value, state.selection.end)
            }
        }
    }

    BasicTextField(
        state = state,
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Enter -> {
                        engine.onLineInserted(lineIndex, state.selection.end)
                        true
                    }
                    Key.Backspace -> {
                        if (state.selection.start == 0 && state.selection.end == 0 && lineIndex > 0) {
                            engine.onLineMerge(lineIndex)
                            true
                        } else false
                    }
                    else -> false
                }
            },
        textStyle = textStyle,
        cursorBrush = cursorBrush,
        lineLimits = TextFieldLineLimits.SingleLine,
        outputTransformation = ScribeOutputTransformation(
            registry = registry,
            lineStart = engine.buffer.lineStart(lineIndex),
            styleFor = ::styleFor
        ),
        inputTransformation = ScribeInputTransformation,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        )
    )
}

private fun styleFor(type: FormatType) = when (type) {
    FormatType.BOLD -> androidx.compose.ui.text.SpanStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
    FormatType.ITALIC -> androidx.compose.ui.text.SpanStyle(
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
    FormatType.UNDERLINE -> androidx.compose.ui.text.SpanStyle(
        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
    )
    FormatType.STRIKETHROUGH -> androidx.compose.ui.text.SpanStyle(
        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
    )
    FormatType.BLOCKQUOTE -> androidx.compose.ui.text.SpanStyle(
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
    else -> androidx.compose.ui.text.SpanStyle()
}