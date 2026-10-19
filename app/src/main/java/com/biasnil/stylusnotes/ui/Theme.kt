package com.biasnil.stylusnotes.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.biasnil.stylusnotes.MainViewModel

/** The app's colours. Values come from res/values/colors.xml (see ThemeColors.kt). */
data class AppColors(
    val isDark: Boolean,
    val topBar: Color,
    val onTopBar: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val sidebar: Color,
    val card: Color,
    val selected: Color,
    val folder: Color,
    val folderTab: Color,
    val onFolder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val desk: Color,
    val divider: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("No theme") }

/** Shorthand for the current theme's colours inside composables. */
val Palette: AppColors
    @Composable get() = LocalAppColors.current

/** Phones and narrow windows get the compact layouts. */
val isCompact: Boolean
    @Composable get() = LocalConfiguration.current.screenWidthDp < 600

@Composable
fun StylusNotesTheme(vm: MainViewModel, content: @Composable () -> Unit) {
    val settings by vm.appSettings.collectAsState()
    val colors = loadColors(settings.theme, isSystemInDarkTheme())

    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.selected,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.folder,
            onSecondary = colors.onFolder,
            tertiaryContainer = colors.selected,
            onTertiaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.sidebar,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.card,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.card,
            surfaceContainerHigh = colors.card,
            surfaceContainerHighest = colors.selected,
            outline = colors.divider,
            outlineVariant = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.selected,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.folder,
            onSecondary = colors.onFolder,
            tertiaryContainer = colors.selected,
            onTertiaryContainer = colors.textPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.sidebar,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainer = colors.card,
            surfaceContainerHigh = colors.card,
            surfaceContainerHighest = colors.sidebar,
            outline = colors.divider,
            outlineVariant = colors.divider,
        )
    }

    // Status/navigation bar icons: light icons on dark themes, dark icons on light ones.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.decorView.setBackgroundColor(colors.background.toArgb())
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false // the top bars are always dark enough
            controller.isAppearanceLightNavigationBars = !colors.isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
