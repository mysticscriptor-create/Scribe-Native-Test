package com.primaloptima.scribe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScribeApp
    private val db = app.database

    val books: LiveData<List<Book>> = db.bookDao().observeAll().asLiveData()
    val allNotes: LiveData<List<Note>> = db.noteDao().observeAll().asLiveData()
    val allFolders: LiveData<List<Folder>> = db.noteDao().observeFolders().asLiveData()

    private val _searchResults = MutableLiveData<List<Book>>(emptyList())
    val searchResults: LiveData<List<Book>> = _searchResults

    // ── Writing Streak ────────────────────────────────────────────────────────

    private val _currentStreak = MutableStateFlow(app.prefs.getStreak().currentStreak)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    fun refreshStreak() {
        _currentStreak.value = app.prefs.getStreak().currentStreak
    }

    // ── Ongoing Project ───────────────────────────────────────────────────────

    private val _ongoingProjectBookId = MutableStateFlow(app.prefs.ongoingProjectBookId)
    val ongoingProjectBookId: StateFlow<String?> = _ongoingProjectBookId.asStateFlow()

    /**
     * Notes inside /Chapters of the ongoing project, sorted newest-first.
     * Emits an empty list when no project is set.
     */
    val ongoingProjectChapters: StateFlow<List<Note>> =
        _ongoingProjectBookId
            .flatMapLatest { bookId ->
                if (bookId == null) flowOf(emptyList())
                else db.noteDao().observeByBook(bookId)
                    .map { notes -> notes.filter { it.folderPath == "/Chapters" } }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * All notes inside the ongoing project's book (used for total word count).
     * Emits an empty list when no project is set.
     */
    val ongoingProjectAllNotes: StateFlow<List<Note>> =
        _ongoingProjectBookId
            .flatMapLatest { bookId ->
                if (bookId == null) flowOf(emptyList())
                else db.noteDao().observeByBook(bookId)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Designates [bookId] as the ongoing project and ensures /Chapters folder exists.
     * Safe to call multiple times — folder insert is IGNORE on conflict.
     */
    fun setOngoingProject(bookId: String) {
        app.prefs.ongoingProjectBookId = bookId
        _ongoingProjectBookId.value = bookId
        viewModelScope.launch(Dispatchers.IO) {
            db.noteDao().insertFolder(Folder(bookId = bookId, path = "/Chapters"))
        }
    }

    /** Removes the ongoing project designation. Does NOT delete the Chapters folder or notes. */
    fun clearOngoingProject() {
        app.prefs.ongoingProjectBookId = null
        _ongoingProjectBookId.value = null
    }

    /**
     * Creates a new chapter note in /Chapters of the ongoing project.
     * Auto-names it "Chapter N" based on existing chapter count.
     * [onCreated] is called on the Main thread with the new note.
     */
    fun createChapter(onCreated: (Note) -> Unit) {
        val bookId = _ongoingProjectBookId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.noteDao().getByBookFolder(bookId, "/Chapters")
            val nextNumber = existing.size + 1
            val id = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val note = Note(
                id = id,
                bookId = bookId,
                name = "Chapter $nextNumber",
                content = "",
                folderPath = "/Chapters",
                createdAt = now,
                updatedAt = now
            )
            db.noteDao().insert(note)
            withContext(Dispatchers.Main) { onCreated(note) }
        }
    }

    // ── Quick Note Creation ──────────────────────────────────────────────────

    fun createQuickNote(onCreated: (Note) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.noteDao().getByFolder("/Quick Notes")
            val quickNotes = if (existing.isEmpty()) {
                db.noteDao().getByBookFolder(Note.DEFAULT_BOOK_ID, "/Quick Notes")
            } else existing

            val nextNumber = quickNotes.size + 1
            val title = "Note $nextNumber"
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val note = Note(
                id = id,
                bookId = Note.DEFAULT_BOOK_ID,
                name = title,
                content = "",
                folderPath = "/Quick Notes",
                createdAt = now,
                updatedAt = now
            )
            db.noteDao().insertFolder(Folder(bookId = Note.DEFAULT_BOOK_ID, path = "/Quick Notes"))
            db.noteDao().insert(note)
            withContext(Dispatchers.Main) { onCreated(note) }
        }
    }

    // ── Sort mode ──────────────────────────────────────────────────────────────

    enum class SortMode { DATE_UPDATED, DATE_CREATED, TITLE_AZ, MANUAL }

    private val _sortMode = MutableLiveData(SortMode.DATE_UPDATED)
    val sortMode: LiveData<SortMode> = _sortMode

    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun createBook(title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val book = Book(id = id, title = title, createdAt = now, updatedAt = now)
            db.bookDao().insert(book)
            // Create root folder for new book
            db.noteDao().insertFolder(Folder(bookId = id, path = "/"))
            withContext(Dispatchers.Main) { onCreated(id) }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.noteDao().deleteByBook(bookId)
            db.noteDao().deleteFoldersByBook(bookId)
            db.bookDao().deleteById(bookId)
        }
    }

    fun renameBook(bookId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val book = db.bookDao().getById(bookId) ?: return@launch
            db.bookDao().update(book.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateCover(bookId: String, uri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookDao().updateCover(bookId, uri, System.currentTimeMillis())
        }
    }

    fun moveBookManual(bookId: String, newOrder: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookDao().updateSortOrder(bookId, newOrder)
        }
    }

    /** Sort the visible list client-side (books LiveData still sorted by DB default). */
    fun sortedBooks(books: List<Book>): List<Book> = when (_sortMode.value) {
        SortMode.DATE_CREATED -> books.sortedByDescending { it.createdAt }
        SortMode.TITLE_AZ -> books.sortedBy { it.title.lowercase() }
        SortMode.MANUAL -> books.sortedBy { it.sortOrder }
        else -> books.sortedByDescending { it.updatedAt }
    }

    fun searchBooks(query: String, allBooks: List<Book>) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        _searchResults.value = allBooks.filter {
            it.title.contains(query, ignoreCase = true)
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }
}
