package com.biasnil.stylusnotes

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.biasnil.stylusnotes.input.PenKeys
import com.biasnil.stylusnotes.ui.AppRoot
import com.biasnil.stylusnotes.ui.StylusNotesTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            StylusNotesTheme(vm) {
                AppRoot(vm)
            }
        }

        // "Follow device orientation": rotate even when the system rotation lock is on.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.appSettings.map { it.followOrientation }.distinctUntilChanged().collect { follow ->
                    requestedOrientation =
                        if (follow) ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    /**
     * Lenovo pen buttons often arrive as key events (600..604) rather than as
     * MotionEvent button states, so they're caught here before any view sees them.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (PenKeys.isPenButton(event.keyCode)) {
            vm.onPenKey(event)
            return true
        }
        vm.recordKey(event)
        return super.dispatchKeyEvent(event)
    }
}
