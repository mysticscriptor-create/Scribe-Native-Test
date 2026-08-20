package com.primaloptima.scribe.engine

data class CursorPos(val line: Int, val column: Int)

data class UndoEntry(
    val bufferEdit: Edit,
    val formatEdit: FormatEdit? = null,
    val cursorBefore: CursorPos = CursorPos(0, 0),
    val cursorAfter: CursorPos = CursorPos(0, 0),
    val label: String = ""
)

class UndoManager(private val limit: Int = 200) {
    private val undoStack = ArrayDeque<UndoEntry>()
    private val redoStack = ArrayDeque<UndoEntry>()

    fun push(entry: UndoEntry) {
        undoStack.addLast(entry)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): UndoEntry? {
        val entry = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(entry)
        return entry
    }

    fun redo(): UndoEntry? {
        val entry = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(entry)
        return entry
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun saveCheckpoint(label: String): UndoEntry? =
        undoStack.lastOrNull()?.copy(label = label)
}