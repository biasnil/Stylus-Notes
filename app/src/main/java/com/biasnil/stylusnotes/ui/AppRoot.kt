package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.MainViewModel
import com.biasnil.stylusnotes.Screen
import com.biasnil.stylusnotes.StylusNotesApp

@Composable
fun AppRoot(vm: MainViewModel) {
    val app = LocalContext.current.applicationContext as StylusNotesApp
    val screen by vm.screen.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        app.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        when (val s = screen) {
            Screen.Library -> LibraryScreen(vm)
            is Screen.Editor -> EditorScreen(vm, s.noteId)
            Screen.RegisterPen -> RegisterPenScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
        }
        vm.update.collectAsState().value?.let { release ->
            UpdateDialog(
                release = release,
                current = vm.versionName,
                onLater = { vm.dismissUpdate(skip = false) },
                onSkip = { vm.dismissUpdate(skip = true) },
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }
}
