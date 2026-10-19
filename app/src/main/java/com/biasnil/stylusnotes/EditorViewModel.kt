package com.biasnil.stylusnotes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biasnil.stylusnotes.data.AppSettings
import com.biasnil.stylusnotes.data.Note
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PageView
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.newId
import com.biasnil.stylusnotes.export.NoteExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StylusNotesApp
    private val library = app.library

    private val settings: StateFlow<AppSettings> =
        app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    private val _page = MutableStateFlow(0)
    val pageIndex: StateFlow<Int> = _page.asStateFlow()

    private val _imagesToPlace = MutableSharedFlow<PlacedImage>(extraBufferCapacity = 2)
    /** Pictures picked with the toolbar's image button, for the canvas to add (so it's undoable). */
    val imagesToPlace: SharedFlow<PlacedImage> = _imagesToPlace.asSharedFlow()

    private var loadedId: String? = null
    private var saveJob: Job? = null
    private var dirty = false

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    private fun message(resId: Int, vararg args: Any) = app.message(resId, *args)

    /** Loads from disk. Waits for any save still running, so it never reads a stale file. */
    fun load(id: String) {
        val switching = loadedId != id
        if (switching) {
            _note.value = null
            _page.value = 0
        }
        loadedId = id
        launch {
            val n = library.load(id)
            if (loadedId != id) return@launch
            if (n == null) {
                message(R.string.msg_open_failed)
                return@launch
            }
            val note = if (n.pages.isEmpty()) n.copy(pages = listOf(Page())) else n
            _note.value = note
            val start = when {
                !switching -> _page.value
                settings.first().resumeLastPage -> library.state.value.notes.find { it.id == id }?.lastPage ?: 0
                else -> 0
            }
            _page.value = start.coerceIn(0, note.pages.lastIndex)
            dirty = false
        }
    }

    private val currentPage: Page? get() = _note.value?.pages?.getOrNull(_page.value)

    /** [touch]: false for changes that shouldn't bump the modified date (like scroll position). */
    private fun edit(touch: Boolean = true, block: (Note) -> Note) {
        val n = _note.value ?: return
        val next = block(n)
        _note.value = if (touch) next.copy(modified = System.currentTimeMillis()) else next
        dirty = true
        scheduleSave()
    }

    private fun updatePage(pageId: String, touch: Boolean = true, block: (Page) -> Page) =
        edit(touch) { n -> n.copy(pages = n.pages.map { if (it.id == pageId) block(it) else it }) }

    // ---------------------------------------------------------------- content

    fun onContentChanged(pageId: String, strokes: List<Stroke>, images: List<PlacedImage>) =
        updatePage(pageId) { it.copy(strokes = strokes, images = images) }

    fun onViewChanged(pageId: String, view: PageView) {
        if (!settings.value.savePagePositions) return
        updatePage(pageId, touch = false) { it.copy(view = view) }
    }

    fun setTitle(title: String) = edit { it.copy(title = title) }

    // ---------------------------------------------------------------- pages

    fun goTo(i: Int) {
        val n = _note.value ?: return
        _page.value = i.coerceIn(0, n.pages.lastIndex)
        val id = n.id
        val page = _page.value
        launch { library.setLastPage(id, page) }
    }

    fun next() = goTo(_page.value + 1)
    fun prev() = goTo(_page.value - 1)

    /** Inserts after the current page and jumps to the first new one. */
    private fun insertPages(pages: List<Page>) {
        val n = _note.value ?: return
        if (pages.isEmpty()) return
        val at = (_page.value + 1).coerceAtMost(n.pages.size)
        edit { it.copy(pages = it.pages.toMutableList().apply { addAll(at, pages) }) }
        goTo(at)
    }

    fun addTemplatePage(templateId: String) {
        insertPages(listOf(app.pages.templatePage(templateId, settings.value.paperSize)))
    }

    /** New page with the same paper and size as the current one. */
    fun addSamePage() {
        val cur = currentPage ?: return
        val template = if (cur.pdf != null) "blank" else cur.template
        insertPages(listOf(Page(template = template, width = cur.width, height = cur.height)))
    }

    fun changeBackground(templateId: String) {
        val cur = currentPage ?: return
        if (cur.pdf != null) {
            message(R.string.msg_pdf_keeps_background)
            return
        }
        updatePage(cur.id) { it.copy(template = templateId) }
    }

    fun deletePage(i: Int) {
        val n = _note.value ?: return
        if (n.pages.size <= 1) {
            message(R.string.msg_need_one_page)
            return
        }
        if (i !in n.pages.indices) return
        edit { it.copy(pages = it.pages.filterIndexed { j, _ -> j != i }) }
        val cur = _page.value
        goTo((if (i < cur) cur - 1 else cur).coerceIn(0, n.pages.size - 2))
    }

    fun duplicatePage(i: Int) {
        val n = _note.value ?: return
        val p = n.pages.getOrNull(i) ?: return
        edit { it.copy(pages = it.pages.toMutableList().apply { add(i + 1, p.copy(id = newId())) }) }
        goTo(i + 1)
    }

    fun movePage(i: Int, delta: Int) {
        val n = _note.value ?: return
        val j = i + delta
        if (i !in n.pages.indices || j !in n.pages.indices) return
        edit {
            val list = it.pages.toMutableList()
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
            it.copy(pages = list)
        }
        if (_page.value == i) goTo(j) else if (_page.value == j) goTo(i)
    }

    fun insertPdf(uri: Uri, keepText: Boolean) = launch {
        runCatching { app.pages.pdfPages(uri, keepText) }
            .onSuccess { insertPages(it.pages) }
            .onFailure { app.message(it.message ?: app.getString(R.string.msg_pdf_import_failed)) }
    }

    fun insertImagePages(uris: List<Uri>) = launch {
        val pages = app.pages.imagePages(uris)
        if (pages.isEmpty()) message(R.string.msg_pictures_failed) else insertPages(pages)
    }

    fun placeImage(uri: Uri) = launch {
        val p = currentPage ?: return@launch
        val img = app.pages.placedImage(uri, p)
        if (img == null) message(R.string.msg_picture_failed) else _imagesToPlace.emit(img)
    }

    // ---------------------------------------------------------------- export

    fun exportPdf() = launch {
        val n = _note.value ?: return@launch
        message(R.string.msg_exporting)
        runCatching { NoteExporter.exportPdf(app, n, app.assets) }
            .onSuccess { message(R.string.msg_saved_to, it) }
            .onFailure { app.message(it.message ?: app.getString(R.string.msg_export_failed)) }
    }

    fun exportPagePng() = launch {
        val n = _note.value ?: return@launch
        runCatching { NoteExporter.exportPagePng(app, n, _page.value, app.assets) }
            .onSuccess { message(R.string.msg_saved_to, it) }
            .onFailure { app.message(it.message ?: app.getString(R.string.msg_export_failed)) }
    }

    // ---------------------------------------------------------------- saving

    private fun thumbPage(): Int = if (settings.value.firstPageThumbnail) 0 else _page.value

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        if (!dirty) return
        val n = _note.value ?: return
        dirty = false
        library.save(n)
        runCatching { app.thumbs.update(n, thumbPage()) }
    }

    /** Saves right away on the app scope, so it finishes even if this screen is closing. */
    fun flush() {
        saveJob?.cancel()
        val n = _note.value ?: return
        val thumb = thumbPage()
        val wasDirty = dirty
        dirty = false
        app.appScope.launch {
            if (wasDirty) library.save(n)
            // The thumbnail follows the last viewed page, so refresh it even without edits.
            runCatching { app.thumbs.update(n, thumb) }
        }
    }

    override fun onCleared() {
        flush()
        super.onCleared()
    }
}
