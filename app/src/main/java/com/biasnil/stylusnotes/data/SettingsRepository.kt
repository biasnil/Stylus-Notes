package com.biasnil.stylusnotes.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biasnil.stylusnotes.paper.PaperCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object K {
        val resumeLastPage = booleanPreferencesKey("resume_last_page")
        val firstPageThumb = booleanPreferencesKey("first_page_thumbnail")
        val disableBack = booleanPreferencesKey("disable_back")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val followOrientation = booleanPreferencesKey("follow_orientation")
        val primaryAction = stringPreferencesKey("primary_button")
        val secondaryAction = stringPreferencesKey("secondary_button")
        val eraserEnd = stringPreferencesKey("eraser_end")
        val secondaryKey = intPreferencesKey("secondary_key")
        val defaultTemplate = stringPreferencesKey("last_template") // key kept from earlier versions
        val paperSize = stringPreferencesKey("paper_size")
        val savePositions = booleanPreferencesKey("save_page_positions")
        val defaultZoom = stringPreferencesKey("default_zoom")
        val doubleTap = stringPreferencesKey("two_finger_double_tap")
        val keepText = booleanPreferencesKey("pdf_keep_text")
        val present = booleanPreferencesKey("present_external")
        val theme = stringPreferencesKey("theme")
        val sort = stringPreferencesKey("sort")
        val checkUpdates = booleanPreferencesKey("check_updates")
        val skippedVersion = stringPreferencesKey("skipped_version")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { it.toSettings() }

    /** Reads the current settings, applies [transform] and writes the result. */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private inline fun <reified T : Enum<T>> Preferences.enum(key: Preferences.Key<String>, default: T): T =
        this[key]?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            resumeLastPage = this[K.resumeLastPage] ?: d.resumeLastPage,
            firstPageThumbnail = this[K.firstPageThumb] ?: d.firstPageThumbnail,
            disableBack = this[K.disableBack] ?: d.disableBack,
            keepScreenOn = this[K.keepScreenOn] ?: d.keepScreenOn,
            followOrientation = this[K.followOrientation] ?: d.followOrientation,
            buttons = PenButtons(
                primary = enum(K.primaryAction, d.buttons.primary),
                secondary = enum(K.secondaryAction, d.buttons.secondary),
                eraserEnd = enum(K.eraserEnd, d.buttons.eraserEnd),
            ),
            secondaryKey = this[K.secondaryKey] ?: d.secondaryKey,
            defaultTemplate = this[K.defaultTemplate]?.takeIf { PaperCatalog.contains(it) } ?: d.defaultTemplate,
            paperSize = enum(K.paperSize, d.paperSize),
            savePagePositions = this[K.savePositions] ?: d.savePagePositions,
            defaultZoom = enum(K.defaultZoom, d.defaultZoom),
            twoFingerDoubleTap = enum(K.doubleTap, d.twoFingerDoubleTap),
            pdfKeepText = this[K.keepText] ?: d.pdfKeepText,
            presentOnExternal = this[K.present] ?: d.presentOnExternal,
            theme = enum(K.theme, d.theme),
            sort = enum(K.sort, d.sort),
            checkUpdates = this[K.checkUpdates] ?: d.checkUpdates,
            skippedVersion = this[K.skippedVersion] ?: d.skippedVersion,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.resumeLastPage] = s.resumeLastPage
        this[K.firstPageThumb] = s.firstPageThumbnail
        this[K.disableBack] = s.disableBack
        this[K.keepScreenOn] = s.keepScreenOn
        this[K.followOrientation] = s.followOrientation
        this[K.primaryAction] = s.buttons.primary.name
        this[K.secondaryAction] = s.buttons.secondary.name
        this[K.eraserEnd] = s.buttons.eraserEnd.name
        this[K.secondaryKey] = s.secondaryKey
        this[K.defaultTemplate] = s.defaultTemplate
        this[K.paperSize] = s.paperSize.name
        this[K.savePositions] = s.savePagePositions
        this[K.defaultZoom] = s.defaultZoom.name
        this[K.doubleTap] = s.twoFingerDoubleTap.name
        this[K.keepText] = s.pdfKeepText
        this[K.present] = s.presentOnExternal
        this[K.theme] = s.theme.name
        this[K.sort] = s.sort.name
        this[K.checkUpdates] = s.checkUpdates
        this[K.skippedVersion] = s.skippedVersion
    }
}
