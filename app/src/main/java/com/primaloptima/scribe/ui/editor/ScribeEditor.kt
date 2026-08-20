package com.primaloptima.scribe.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.primaloptima.scribe.engine.ScribeEditorEngine

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScribeEditor(
    engine: ScribeEditorEngine,
    textStyle: TextStyle,
    cursorBrush: Brush,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequesters = remember { mutableMapOf<Int, androidx.compose.ui.focus.FocusRequester>() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            count = engine.lineCount.value,
            key = { index -> "scribe-line-$index" }
        ) { lineIndex ->
            if (engine.lineSnapshot(lineIndex) == "---") {
                androidx.compose.material3.HorizontalDivider()
            } else {
                val requester = focusRequesters.getOrPut(lineIndex) {
                    androidx.compose.ui.focus.FocusRequester()
                }
                ScribeEditorLine(
                    lineIndex = lineIndex,
                    engine = engine,
                    textStyle = textStyle,
                    cursorBrush = cursorBrush,
                    registry = engine.formats,
                    focusRequester = requester
                )
            }
        }
    }
}