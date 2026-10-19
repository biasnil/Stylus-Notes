package com.biasnil.stylusnotes

import android.app.Application
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import com.biasnil.stylusnotes.canvas.PenCommand
import com.biasnil.stylusnotes.canvas.ToolSettings
import com.biasnil.stylusnotes.data.LibraryRepository
import com.biasnil.stylusnotes.data.SettingsRepository
import com.biasnil.stylusnotes.input.AndroidDeviceLookup
import com.biasnil.stylusnotes.input.PenRepository
import com.biasnil.stylusnotes.input.StylusFilter
import com.biasnil.stylusnotes.pdf.AssetLoader
import com.biasnil.stylusnotes.pdf.PageFactory
import com.biasnil.stylusnotes.pdf.Thumbnails
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StylusNotesApp : Application() {

    lateinit var penRepository: PenRepository
        private set
    lateinit var library: LibraryRepository
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var assets: AssetLoader
        private set
    lateinit var pages: PageFactory
        private set
    lateinit var thumbs: Thumbnails
        private set

    /** Shared by every canvas. Only touched on the main thread. */
    val stylusFilter = StylusFilter(AndroidDeviceLookup())

    private val _penSeen = MutableStateFlow(false)
    /** AUTO mode: true once a pen has touched and fingers are switched off. */
    val penSeen: StateFlow<Boolean> = _penSeen.asStateFlow()

    /** Current tool, colour and widths. App-wide so the pen button works on any screen. */
    val tools = MutableStateFlow(ToolSettings())

    /** Undo/redo from a pen button, for the open editor. */
    val penCommands = MutableSharedFlow<PenCommand>(extraBufferCapacity = 4)

    /** Short messages shown as snackbars. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** For work that must finish even after a screen closes, like the last save. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        penRepository = PenRepository(this)
        library = LibraryRepository(this)
        settings = SettingsRepository(this)
        assets = AssetLoader(library)
        pages = PageFactory(this, library, assets)
        thumbs = Thumbnails(library, assets)

        stylusFilter.onPenSeenChanged = { _penSeen.value = it }

        appScope.launch {
            penRepository.settings.collect { s ->
                stylusFilter.registeredPens = s.pens
                stylusFilter.penPolicy = s.policy
                stylusFilter.inputMode = s.mode
            }
        }

        // When a pen reconnects it may get a new deviceId, so re-check on any device change.
        getSystemService(InputManager::class.java).registerInputDeviceListener(
            object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) = stylusFilter.invalidateDevices()
                override fun onInputDeviceRemoved(deviceId: Int) = stylusFilter.invalidateDevices()
                override fun onInputDeviceChanged(deviceId: Int) = stylusFilter.invalidateDevices()
            },
            Handler(Looper.getMainLooper()),
        )
    }

    fun toggleEraser() = tools.update { it.toggleEraser() }

    fun message(text: String) {
        messages.tryEmit(text)
    }

    fun message(resId: Int, vararg args: Any) {
        messages.tryEmit(getString(resId, *args))
    }
}
