package com.biasnil.stylusnotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.data.ThemeChoice

// Generated from res/values/colors.xml: one loader per theme.

@Composable
internal fun loadColors(theme: ThemeChoice, systemDark: Boolean): AppColors = when (theme) {
    ThemeChoice.SYSTEM -> if (systemDark) darkColors() else lightColors()
    ThemeChoice.DARK -> darkColors()
    ThemeChoice.LIGHT -> lightColors()
    ThemeChoice.SAKURA -> sakuraColors()
    ThemeChoice.OCEAN -> oceanColors()
    ThemeChoice.FOREST -> forestColors()
    ThemeChoice.MIDNIGHT -> midnightColors()
}

@Composable
private fun darkColors() = AppColors(
    isDark = true,
    topBar = colorResource(R.color.dark_top_bar),
    onTopBar = colorResource(R.color.dark_on_top_bar),
    accent = colorResource(R.color.dark_accent),
    onAccent = colorResource(R.color.dark_on_accent),
    background = colorResource(R.color.dark_background),
    sidebar = colorResource(R.color.dark_sidebar),
    card = colorResource(R.color.dark_card),
    selected = colorResource(R.color.dark_selected),
    folder = colorResource(R.color.dark_folder),
    folderTab = colorResource(R.color.dark_folder_tab),
    onFolder = colorResource(R.color.dark_on_folder),
    textPrimary = colorResource(R.color.dark_text_primary),
    textSecondary = colorResource(R.color.dark_text_secondary),
    desk = colorResource(R.color.dark_desk),
    divider = colorResource(R.color.dark_divider),
)

@Composable
private fun lightColors() = AppColors(
    isDark = false,
    topBar = colorResource(R.color.light_top_bar),
    onTopBar = colorResource(R.color.light_on_top_bar),
    accent = colorResource(R.color.light_accent),
    onAccent = colorResource(R.color.light_on_accent),
    background = colorResource(R.color.light_background),
    sidebar = colorResource(R.color.light_sidebar),
    card = colorResource(R.color.light_card),
    selected = colorResource(R.color.light_selected),
    folder = colorResource(R.color.light_folder),
    folderTab = colorResource(R.color.light_folder_tab),
    onFolder = colorResource(R.color.light_on_folder),
    textPrimary = colorResource(R.color.light_text_primary),
    textSecondary = colorResource(R.color.light_text_secondary),
    desk = colorResource(R.color.light_desk),
    divider = colorResource(R.color.light_divider),
)

@Composable
private fun sakuraColors() = AppColors(
    isDark = false,
    topBar = colorResource(R.color.sakura_top_bar),
    onTopBar = colorResource(R.color.sakura_on_top_bar),
    accent = colorResource(R.color.sakura_accent),
    onAccent = colorResource(R.color.sakura_on_accent),
    background = colorResource(R.color.sakura_background),
    sidebar = colorResource(R.color.sakura_sidebar),
    card = colorResource(R.color.sakura_card),
    selected = colorResource(R.color.sakura_selected),
    folder = colorResource(R.color.sakura_folder),
    folderTab = colorResource(R.color.sakura_folder_tab),
    onFolder = colorResource(R.color.sakura_on_folder),
    textPrimary = colorResource(R.color.sakura_text_primary),
    textSecondary = colorResource(R.color.sakura_text_secondary),
    desk = colorResource(R.color.sakura_desk),
    divider = colorResource(R.color.sakura_divider),
)

@Composable
private fun oceanColors() = AppColors(
    isDark = true,
    topBar = colorResource(R.color.ocean_top_bar),
    onTopBar = colorResource(R.color.ocean_on_top_bar),
    accent = colorResource(R.color.ocean_accent),
    onAccent = colorResource(R.color.ocean_on_accent),
    background = colorResource(R.color.ocean_background),
    sidebar = colorResource(R.color.ocean_sidebar),
    card = colorResource(R.color.ocean_card),
    selected = colorResource(R.color.ocean_selected),
    folder = colorResource(R.color.ocean_folder),
    folderTab = colorResource(R.color.ocean_folder_tab),
    onFolder = colorResource(R.color.ocean_on_folder),
    textPrimary = colorResource(R.color.ocean_text_primary),
    textSecondary = colorResource(R.color.ocean_text_secondary),
    desk = colorResource(R.color.ocean_desk),
    divider = colorResource(R.color.ocean_divider),
)

@Composable
private fun forestColors() = AppColors(
    isDark = true,
    topBar = colorResource(R.color.forest_top_bar),
    onTopBar = colorResource(R.color.forest_on_top_bar),
    accent = colorResource(R.color.forest_accent),
    onAccent = colorResource(R.color.forest_on_accent),
    background = colorResource(R.color.forest_background),
    sidebar = colorResource(R.color.forest_sidebar),
    card = colorResource(R.color.forest_card),
    selected = colorResource(R.color.forest_selected),
    folder = colorResource(R.color.forest_folder),
    folderTab = colorResource(R.color.forest_folder_tab),
    onFolder = colorResource(R.color.forest_on_folder),
    textPrimary = colorResource(R.color.forest_text_primary),
    textSecondary = colorResource(R.color.forest_text_secondary),
    desk = colorResource(R.color.forest_desk),
    divider = colorResource(R.color.forest_divider),
)

@Composable
private fun midnightColors() = AppColors(
    isDark = true,
    topBar = colorResource(R.color.midnight_top_bar),
    onTopBar = colorResource(R.color.midnight_on_top_bar),
    accent = colorResource(R.color.midnight_accent),
    onAccent = colorResource(R.color.midnight_on_accent),
    background = colorResource(R.color.midnight_background),
    sidebar = colorResource(R.color.midnight_sidebar),
    card = colorResource(R.color.midnight_card),
    selected = colorResource(R.color.midnight_selected),
    folder = colorResource(R.color.midnight_folder),
    folderTab = colorResource(R.color.midnight_folder_tab),
    onFolder = colorResource(R.color.midnight_on_folder),
    textPrimary = colorResource(R.color.midnight_text_primary),
    textSecondary = colorResource(R.color.midnight_text_secondary),
    desk = colorResource(R.color.midnight_desk),
    divider = colorResource(R.color.midnight_divider),
)
