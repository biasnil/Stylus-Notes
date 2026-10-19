package com.biasnil.stylusnotes

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biasnil.stylusnotes.canvas.PenCommand
import com.biasnil.stylusnotes.canvas.Tool
import com.biasnil.stylusnotes.data.AppSettings
import com.biasnil.stylusnotes.data.LibraryIndex
import com.biasnil.stylusnotes.data.LibraryOps
import com.biasnil.stylusnotes.data.PenAction
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.InputSettings
import com.biasnil.stylusnotes.input.PenIdentity
import com.biasnil.stylusnotes.input.PenKeys
import com.biasnil.stylusnotes.input.PenPolicy
import com.biasnil.stylusnotes.input.StylusFilter
import com.biasnil.stylusnotes.update.GithubRelease
import com.biasnil.stylusnotes.update.UpdateChecker
import com.biasnil.stylusnotes.update.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Section { NOTES, RECENT, TRASH }

sealed interface Screen {
    data object Library : Screen
    data class Editor(val noteId: String) : Screen
    data object RegisterPen : Screen
    data object Settings : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StylusNotesApp
    private val library = app.library
    private val penRepo = app.penRepository
    private val settingsRepo = app.settings

    private val _screen = MutableStateFlow<Screen>(Screen.Library)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** Where to go back to from Pen setup. */
    private var beforePenSetup: Screen = Screen.Library

    val index: StateFlow<LibraryIndex> = library.state

    private val _section = MutableStateFlow(Section.NOTES)
    val section: StateFlow<Section> = _section.asStateFlow()

    private val _folder = MutableStateFlow<String?>(null)
    /** Open folder in the Notes section; null is the top level. */
    val folder: StateFlow<String?> = _folder.asStateFlow()

    val appSettings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val inputSettings: StateFlow<InputSettings> =
        penRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, InputSettings())

    /** AUTO mode: true once a pen has touched and fingers are off. */
    val penSeen: StateFlow<Boolean> = app.penSeen

    val thumbVersion: StateFlow<Int> = app.thumbs.version

    private val _lastKey = MutableStateFlow<String?>(null)
    val lastKey: StateFlow<String?> = _lastKey.asStateFlow()

    /** Installed version name, e.g. "1.0". */
    val versionName: String =
        runCatching { application.packageManager.getPackageInfo(application.packageName, 0).versionName }.getOrNull() ?: "0"

    private val _update = MutableStateFlow<GithubRelease?>(null)
    /** A newer release found on GitHub, shown as a dialog until dismissed. */
    val update: StateFlow<GithubRelease?> = _update.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    init {
        // Once per launch, when the setting allows it.
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            if (s.checkUpdates) checkForUpdates(manual = false)
        }
    }

    /** Asks GitHub for the latest release. [manual] also reports "up to date" and errors. */
    fun checkForUpdates(manual: Boolean) {
        if (_checking.value) return
        viewModelScope.launch {
            _checking.value = true
            val result = UpdateChecker.check(versionName)
            _checking.value = false
            when (result) {
                is UpdateResult.Available -> {
                    val skipped = appSettings.value.skippedVersion
                    if (manual || result.release.tag != skipped) _update.value = result.release
                }
                UpdateResult.UpToDate -> if (manual) message(R.string.update_up_to_date, versionName)
                UpdateResult.NoReleases -> if (manual) message(R.string.update_no_releases)
                is UpdateResult.Failed -> if (manual) message(R.string.update_failed)
            }
        }
    }

    fun dismissUpdate(skip: Boolean) {
        val tag = _update.value?.tag
        _update.value = null
        if (skip && tag != null) updateSettings { it.copy(skippedVersion = tag) }
    }

    private val _capturingSecondary = MutableStateFlow(false)
    /** Waiting for the user to press the pen button that should be "secondary". */
    val capturingSecondary: StateFlow<Boolean> = _capturingSecondary.asStateFlow()

    private var lastRejectNotice = 0L

    init {
        viewModelScope.launch {
            library.ensureLoaded()
            launch(Dispatchers.Default) { app.thumbs.ensureAll() }
            // First launch with no pen and nothing else allowed to draw: go straight to pen setup.
            val s = penRepo.settings.first()
            if (s.pens.isEmpty() && s.mode == InputMode.PEN_ONLY && s.policy == PenPolicy.REGISTERED_ONLY) {
                showRegisterPen()
            }
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    private fun message(resId: Int, vararg args: Any) = app.message(resId, *args)

    // ---------------------------------------------------------------- navigation

    fun showLibrary() {
        _screen.value = Screen.Library
    }

    fun showRegisterPen() {
        val cur = _screen.value
        if (cur != Screen.RegisterPen) beforePenSetup = cur
        _screen.value = Screen.RegisterPen
    }

    /** Back from Pen setup to wherever it was opened from. */
    fun closeRegisterPen() {
        _screen.value = beforePenSetup
    }

    fun showSettings() {
        _screen.value = Screen.Settings
    }

    fun openSection(s: Section) {
        _section.value = s
    }

    fun openFolder(id: String?) {
        _section.value = Section.NOTES
        _folder.value = id
    }

    fun openNote(id: String) {
        launch { library.markOpened(id) }
        _screen.value = Screen.Editor(id)
    }

    fun closeEditor() {
        _screen.value = Screen.Library
    }

    /** Where new notes and folders go: the open folder, or the top level outside the Notes section. */
    private fun targetFolder(): String? = if (_section.value == Section.NOTES) _folder.value else null

    // ---------------------------------------------------------------- folders

    fun createFolder(name: String) = launch {
        library.createFolder(name.trim().ifBlank { app.getString(R.string.new_folder) }, targetFolder())
    }

    fun renameFolder(id: String, name: String) = launch {
        if (name.isNotBlank()) library.renameFolder(id, name.trim())
    }

    fun moveFolder(id: String, parent: String?) {
        if (!LibraryOps.canMoveFolder(index.value, id, parent)) {
            message(R.string.msg_folder_into_itself)
            return
        }
        launch { library.moveFolder(id, parent) }
    }

    fun deleteFolder(id: String) {
        val idx = index.value
        val gone = LibraryOps.descendants(idx, id)
        val parent = idx.folders.find { it.id == id }?.parentId
        val open = _folder.value
        if (open != null && open in gone) _folder.value = parent
        launch { library.deleteFolder(id) }
    }

    // ---------------------------------------------------------------- notes

    fun renameNote(id: String, title: String) = launch { library.renameNote(id, title.trim()) }

    fun moveNote(id: String, folderId: String?) = launch { library.moveNote(id, folderId) }

    fun trashNote(id: String) = launch {
        library.trashNote(id)
        message(R.string.msg_moved_to_trash)
    }

    fun restoreNote(id: String) = launch { library.restoreNote(id) }

    fun deleteForever(id: String) = launch { library.deleteForever(setOf(id)) }

    fun emptyTrash() = launch { library.emptyTrash() }

    // ---------------------------------------------------------------- creating notes

    fun createNoteFromPaper(templateId: String) = launch {
        val page = app.pages.templatePage(templateId, appSettings.value.paperSize)
        val note = library.createNote("", targetFolder(), listOf(page))
        openNote(note.id)
    }

    fun createNoteFromPdf(uri: Uri, keepText: Boolean) = launch {
        runCatching { app.pages.pdfPages(uri, keepText) }
            .onSuccess { imp ->
                val note = library.createNote(imp.title, targetFolder(), imp.pages)
                app.appScope.launch { runCatching { app.thumbs.update(note) } }
                openNote(note.id)
            }
            .onFailure { app.message(it.message ?: app.getString(R.string.msg_pdf_import_failed)) }
    }

    fun createNoteFromImages(uris: List<Uri>) = launch {
        val pages = app.pages.imagePages(uris)
        if (pages.isEmpty()) {
            message(R.string.msg_pictures_failed)
            return@launch
        }
        val note = library.createNote("", targetFolder(), pages)
        app.appScope.launch { runCatching { app.thumbs.update(note) } }
        openNote(note.id)
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launch { settingsRepo.update(transform) }

    fun startCaptureSecondary() {
        _capturingSecondary.value = true
    }

    fun cancelCaptureSecondary() {
        _capturingSecondary.value = false
    }

    // ---------------------------------------------------------------- pen

    fun registerPen(identity: PenIdentity) = launch {
        penRepo.addPen(identity)
        message(R.string.msg_pen_added, identity.name.ifBlank { "#${identity.deviceId}" })
    }

    fun removePen(identity: PenIdentity) = launch { penRepo.removePen(identity) }

    fun setInputMode(mode: InputMode) {
        // Picking Auto again (even if already on Auto) turns fingers back on.
        if (mode == InputMode.AUTO) app.stylusFilter.resetPenSeen()
        launch { penRepo.setMode(mode) }
    }

    fun setPenPolicy(policy: PenPolicy) = launch { penRepo.setPolicy(policy) }

    fun onRejected(verdict: StylusFilter.Verdict) {
        val res = when (verdict) {
            StylusFilter.Verdict.NO_PEN_REGISTERED -> R.string.msg_no_pen_registered
            StylusFilter.Verdict.REJECT_DEVICE -> R.string.msg_pen_not_registered
            else -> return // fingers and palms are ignored silently
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastRejectNotice > 3000) {
            lastRejectNotice = now
            message(res)
        }
    }

    /** A pen side button sent as a key event (Lenovo 600..604 or the standard stylus keys). */
    fun onPenKey(event: KeyEvent) {
        val down = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        val up = event.action == KeyEvent.ACTION_UP

        if (_capturingSecondary.value) {
            if (down) {
                val code = event.keyCode
                updateSettings { it.copy(secondaryKey = code) }
                _capturingSecondary.value = false
                message(R.string.msg_secondary_assigned, code)
            }
            return
        }

        val s = appSettings.value
        val secondary = PenKeys.isSecondary(event.keyCode, s.secondaryKey)
        val action = if (secondary) s.buttons.secondary else s.buttons.primary
        when (action) {
            PenAction.TOGGLE_ERASER -> if (down) app.toggleEraser()
            PenAction.HOLD_ERASER -> {
                if (down) app.tools.update { if (it.tool == Tool.ERASER) it else it.copy(tool = Tool.ERASER, previous = it.tool) }
                if (up) app.tools.update { if (it.tool == Tool.ERASER) it.copy(tool = it.previous) else it }
            }
            PenAction.TOGGLE_HIGHLIGHTER -> if (down) app.tools.update {
                if (it.tool == Tool.HIGHLIGHTER) it.select(Tool.PEN) else it.select(Tool.HIGHLIGHTER)
            }
            PenAction.UNDO -> if (down) app.penCommands.tryEmit(PenCommand.UNDO)
            PenAction.REDO -> if (down) app.penCommands.tryEmit(PenCommand.REDO)
            PenAction.NOTHING -> Unit
        }
        if (down) {
            val which = app.getString(if (secondary) R.string.button_secondary else R.string.button_primary)
            _lastKey.value = describeKey(event) + " · " + which
        }
    }

    fun recordKey(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            _lastKey.value = describeKey(event)
        }
    }

    private fun describeKey(event: KeyEvent): String =
        "${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode}) · ${event.device?.name ?: "?"}"
}
