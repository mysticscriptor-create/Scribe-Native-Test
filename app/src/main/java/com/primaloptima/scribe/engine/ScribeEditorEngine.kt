package com.primaloptima.scribe.engine

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OutlineEntry(val lineIndex: Int, val level: Int, val title: String)

@OptIn(FlowPreview::class)
class ScribeEditorEngine(
    initialContent: String = "",
    initialFormats: List<FormatSpan> = emptyList()
) : ViewModel() {
    internal var buffer = DocumentBuffer(initialContent)
        private set
    internal val formats = FormatRegistry().also { it.loadAll(initialFormats) }
    private val undoManager = UndoManager(200)

    private val _lineCount = mutableIntStateOf(buffer.lineCount())
    val lineCount: State<Int> get() = _lineCount

    private val _wordCount = MutableStateFlow(countWords(initialContent))
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _charCount = MutableStateFlow(initialContent.length)
    val charCount: StateFlow<Int> = _charCount.asStateFlow()

    private val _outline = MutableStateFlow(extractOutline())
    val outline: StateFlow<List<OutlineEntry>> = _outline.asStateFlow()

    private val _canUndo = mutableStateOf(false)
    val canUndo: State<Boolean> get() = _canUndo
    private val _canRedo = mutableStateOf(false)
    val canRedo: State<Boolean> get() = _canRedo

    private val snapshots = MutableStateFlow(initialContent)
    val findReplace = FindReplaceEngine(this)

    init {
        viewModelScope.launch {
            snapshots.debounce(300).mapLatest { text ->
                withContext(Dispatchers.Default) { countWords(text) to text.length }
            }.flowOn(Dispatchers.Default).collect { (words, chars) ->
                _wordCount.value = words
                _charCount.value = chars
            }
        }
        viewModelScope.launch {
            snapshots.debounce(500).map { extractOutlineFromText(it) }
                .distinctUntilChanged().flowOn(Dispatchers.Default)
                .collect { _outline.value = it }
        }
    }

    fun onLineChanged(lineIndex: Int, newText: String, cursorPos: Int) {
        val start = buffer.lineStart(lineIndex)
        val old = buffer.lineContent(lineIndex)
        val edit = replaceRange(start, start + old.length, newText, recordUndo = false)
        undoManager.push(
            UndoEntry(edit, cursorBefore = CursorPos(lineIndex, 0), cursorAfter = CursorPos(lineIndex, cursorPos))
        )
        refreshHistory()
    }

    fun onLineInserted(afterLineIndex: Int, splitAt: Int) {
        val start = buffer.lineStart(afterLineIndex)
        val lineLength = buffer.lineContent(afterLineIndex).length
        val position = (start + splitAt).coerceIn(start, start + lineLength)
        val edit = replaceRange(position, position, "\n")
        undoManager.push(UndoEntry(edit, label = "Enter"))
        refreshHistory()
    }

    fun onLineMerge(lineIndex: Int) {
        if (lineIndex <= 0) return
        val position = buffer.lineStart(lineIndex) - 1
        val edit = replaceRange(position, position + 1, "")
        undoManager.push(UndoEntry(edit, label = "Merge lines"))
        refreshHistory()
    }

    fun insertAtCursor(lineIndex: Int, cursorPos: Int, text: String) {
        val position = buffer.lineStart(lineIndex) + cursorPos
        val edit = replaceRange(position, position, text)
        undoManager.push(UndoEntry(edit))
        refreshHistory()
    }

    fun toggleFormat(type: FormatType, selStart: Int, selEnd: Int) {
        val formatEdit = formats.toggleSpan(type, selStart, selEnd)
        undoManager.push(UndoEntry(Edit.Compound(emptyList()), formatEdit, label = type.name))
        refreshHistory()
    }

    fun applyFormatWrap(lineIndex: Int, selStart: Int, selEnd: Int, open: String, close: String) {
        val base = buffer.lineStart(lineIndex)
        val start = base + selStart
        val end = base + selEnd
        val edit = replaceRange(end, end, close)
        replaceRange(start, start, open)
        undoManager.push(UndoEntry(Edit.Compound(listOf(edit)), label = "Format"))
        refreshHistory()
    }

    fun replaceRange(start: Int, end: Int, replacement: String, recordUndo: Boolean = true): Edit {
        val deleted = if (end > start) buffer.delete(start, end) else null
        formats.adjustForDelete(start, end - start)
        val inserted = if (replacement.isNotEmpty()) buffer.insert(start, replacement) else null
        formats.adjustForInsert(start, replacement.length)
        publishSnapshot()
        val edit = when {
            deleted != null && inserted != null -> Edit.Compound(listOf(deleted, inserted))
            deleted != null -> deleted
            inserted != null -> inserted
            else -> Edit.Compound(emptyList())
        }
        if (recordUndo) {
            undoManager.push(UndoEntry(edit))
            refreshHistory()
        }
        return edit
    }

    fun undo() {
        undoManager.undo()?.let { entry ->
            reverse(entry)
            refreshHistory()
        }
    }

    fun redo() {
        undoManager.redo()?.let { entry ->
            apply(entry)
            refreshHistory()
        }
    }

    fun saveSnapshot(label: String) {
        undoManager.saveCheckpoint(label)
    }

    fun search(query: String, caseSensitive: Boolean, isRegex: Boolean) =
        findReplace.search(query, caseSensitive, isRegex)

    fun replaceAll(query: String, replacement: String, caseSensitive: Boolean) {
        findReplace.search(query, caseSensitive, false)
        findReplace.replaceAll(replacement)
    }

    fun exportPlainText(): String = buffer.asString()

    fun exportWithFormats(): SerializedDocument =
        SerializedDocument(plainText = buffer.asString(), spans = formats.all.toSerialized())

    fun loadDocument(document: SerializedDocument) {
        buffer = DocumentBuffer(document.plainText)
        formats.loadAll(document.spans.toFormatSpans())
        undoManager.clear()
        _lineCount.intValue = buffer.lineCount()
        publishSnapshot()
        refreshHistory()
    }

    internal fun lineSnapshot(lineIndex: Int): String = buffer.lineContent(lineIndex)

    private fun publishSnapshot() {
        _lineCount.intValue = buffer.lineCount()
        snapshots.value = buffer.asString()
    }

    private fun reverse(entry: UndoEntry) {
        when (val edit = entry.bufferEdit) {
            is Edit.Insert -> buffer.delete(edit.pos, edit.pos + edit.length)
            is Edit.Delete -> buffer.insert(edit.start, edit.text)
            is Edit.Compound -> edit.edits.asReversed().forEach { reverse(UndoEntry(it)) }
        }
        entry.formatEdit?.let { restoreFormatEdit(it, reverse = true) }
        publishSnapshot()
    }

    private fun apply(entry: UndoEntry) {
        when (val edit = entry.bufferEdit) {
            is Edit.Insert -> buffer.insert(edit.pos, edit.text)
            is Edit.Delete -> buffer.delete(edit.start, edit.end)
            is Edit.Compound -> edit.edits.forEach { apply(UndoEntry(it)) }
        }
        entry.formatEdit?.let { restoreFormatEdit(it, reverse = false) }
        publishSnapshot()
    }

    private fun restoreFormatEdit(edit: FormatEdit, reverse: Boolean) {
        when (edit) {
            is FormatEdit.Added -> if (reverse) formats.removeSpan(edit.span) else formats.addSpan(edit.span)
            is FormatEdit.Removed -> if (reverse) edit.spans.forEach(formats::addSpan)
            else -> Unit
        }
    }

    private fun refreshHistory() {
        _canUndo.value = undoManager.canUndo()
        _canRedo.value = undoManager.canRedo()
    }

    private fun extractOutline(): List<OutlineEntry> = extractOutlineFromText(buffer.asString())

    private fun extractOutlineFromText(text: String): List<OutlineEntry> =
        text.lineSequence().mapIndexedNotNull { index, line ->
            val trimmed = line.trimStart()
            val hashes = trimmed.takeWhile { it == '#' }.length
            if (hashes in 1..3 && trimmed.getOrNull(hashes) == ' ') {
                OutlineEntry(index, hashes, trimmed.drop(hashes).trim())
            } else null
        }.toList()

    private fun countWords(text: String): Int =
        Regex("""\b[\p{L}\p{N}_]+\b""").findAll(text).count()
}