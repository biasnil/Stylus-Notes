package com.biasnil.stylusnotes.ui

import android.app.LocaleManager
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette as PaletteIcon
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.MainViewModel
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.update.ProjectLinks
import com.biasnil.stylusnotes.data.DoubleTapAction
import com.biasnil.stylusnotes.data.EraserEnd
import com.biasnil.stylusnotes.data.PenAction
import com.biasnil.stylusnotes.data.ThemeChoice
import com.biasnil.stylusnotes.data.ZoomMode
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenPolicy
import com.biasnil.stylusnotes.paper.PaperSize

private enum class SettingsPage(val icon: ImageVector, val title: Int) {
    NOTE_EDITOR(Icons.Filled.Description, R.string.set_note_editor),
    INPUT(Icons.Filled.Edit, R.string.set_input_methods),
    NOTE_DEFAULTS(Icons.Filled.Tune, R.string.set_note_defaults),
    CASTING(Icons.Filled.Cast, R.string.set_casting),
    THEME(Icons.Filled.PaletteIcon, R.string.set_theme),
    LANGUAGE(Icons.Filled.Language, R.string.set_language),
    ABOUT(Icons.Filled.Info, R.string.set_about),
}

private val NOTES_PAGES = listOf(SettingsPage.NOTE_EDITOR, SettingsPage.INPUT, SettingsPage.NOTE_DEFAULTS, SettingsPage.CASTING)
private val GENERAL_PAGES = listOf(SettingsPage.THEME, SettingsPage.LANGUAGE, SettingsPage.ABOUT)

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val c = Palette
    val compact = isCompact
    // On phones null means "show the list"; on tablets the first page is always shown.
    var pageName by rememberSaveable { mutableStateOf<String?>(if (compact) null else SettingsPage.NOTE_EDITOR.name) }
    val page = pageName?.let { SettingsPage.valueOf(it) } ?: if (compact) null else SettingsPage.NOTE_EDITOR

    BackHandler {
        if (compact && page != null) pageName = null else vm.showLibrary()
    }

    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = { if (compact && page != null) pageName = null else vm.showLibrary() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.textPrimary)
            }
            Text(
                if (compact && page != null) stringResource(page.title) else stringResource(R.string.settings),
                color = c.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            if (!compact && page != null) {
                Text(
                    stringResource(page.title),
                    color = c.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1.8f).padding(start = 24.dp),
                )
            }
        }

        if (compact) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (page == null) CategoryList(null) { pageName = it.name } else Column { PageContent(vm, page) }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Box(Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    CategoryList(page) { pageName = it.name }
                }
                Spacer(Modifier.width(32.dp))
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    page?.let { PageContent(vm, it) }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- category list

@Composable
private fun CategoryList(selected: SettingsPage?, onPick: (SettingsPage) -> Unit) {
    Column {
        GroupTitle(stringResource(R.string.set_group_notes))
        Group { NOTES_PAGES.forEachIndexed { i, p -> CategoryRow(p, p == selected, i > 0) { onPick(p) } } }
        GroupTitle(stringResource(R.string.set_group_general))
        Group { GENERAL_PAGES.forEachIndexed { i, p -> CategoryRow(p, p == selected, i > 0) { onPick(p) } } }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryRow(page: SettingsPage, selected: Boolean, divider: Boolean, onClick: () -> Unit) {
    val c = Palette
    if (divider) HorizontalDivider(color = c.divider)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) c.selected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Icon(page.icon, contentDescription = null, tint = c.textPrimary)
        Spacer(Modifier.width(20.dp))
        Text(stringResource(page.title), color = c.textPrimary, style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------------- building blocks

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        color = Palette.accent,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 12.dp),
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.card),
        content = content,
    )
}

@Composable
private fun RowDivider() = HorizontalDivider(color = Palette.divider)

@Composable
private fun RowText(title: String, subtitle: String?, modifier: Modifier) {
    val c = Palette
    Column(modifier) {
        Text(title, color = c.textPrimary, style = MaterialTheme.typography.titleMedium)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = c.textSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        RowText(title, subtitle, Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A row with the current choice on the right and a dropdown of options, like Squid's. */
@Composable
private fun <T> ChoiceRow(
    title: String,
    subtitle: String? = null,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val c = Palette
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        RowText(title, subtitle, Modifier.weight(1f).padding(end = 12.dp))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label(selected),
                    color = c.accent,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.accent)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(label(o)) },
                        trailingIcon = { if (o == selected) Icon(Icons.Filled.Check, contentDescription = null) },
                        onClick = {
                            open = false
                            onSelect(o)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit) {
    val c = Palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        RowText(title, subtitle, Modifier.weight(1f).padding(end = 12.dp))
        if (value != null) Text(value, color = c.accent, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun InfoCard(text: String) {
    val c = Palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = c.textSecondary)
        Spacer(Modifier.width(14.dp))
        Text(text, color = c.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------- pages

@Composable
private fun PageContent(vm: MainViewModel, page: SettingsPage) {
    when (page) {
        SettingsPage.NOTE_EDITOR -> NoteEditorPage(vm)
        SettingsPage.INPUT -> InputPage(vm)
        SettingsPage.NOTE_DEFAULTS -> NoteDefaultsPage(vm)
        SettingsPage.CASTING -> CastingPage(vm)
        SettingsPage.THEME -> ThemePage(vm)
        SettingsPage.LANGUAGE -> LanguagePage()
        SettingsPage.ABOUT -> AboutPage(vm)
    }
}

@Composable
private fun NoteEditorPage(vm: MainViewModel) {
    val s by vm.appSettings.collectAsState()
    GroupTitle(stringResource(R.string.set_open_actions))
    Group {
        SwitchRow(stringResource(R.string.set_resume_last_page), stringResource(R.string.set_resume_last_page_detail), s.resumeLastPage) { v ->
            vm.updateSettings { it.copy(resumeLastPage = v) }
        }
    }
    GroupTitle(stringResource(R.string.set_misc))
    Group {
        SwitchRow(stringResource(R.string.set_first_page_thumb), stringResource(R.string.set_first_page_thumb_detail), s.firstPageThumbnail) { v ->
            vm.updateSettings { it.copy(firstPageThumbnail = v) }
        }
        RowDivider()
        SwitchRow(stringResource(R.string.set_disable_back), stringResource(R.string.set_disable_back_detail), s.disableBack) { v ->
            vm.updateSettings { it.copy(disableBack = v) }
        }
        RowDivider()
        SwitchRow(stringResource(R.string.set_keep_screen_on), stringResource(R.string.set_keep_screen_on_detail), s.keepScreenOn) { v ->
            vm.updateSettings { it.copy(keepScreenOn = v) }
        }
        RowDivider()
        SwitchRow(stringResource(R.string.set_follow_orientation), stringResource(R.string.set_follow_orientation_detail), s.followOrientation) { v ->
            vm.updateSettings { it.copy(followOrientation = v) }
        }
    }
}

@Composable
private fun penActionLabel(a: PenAction): String = stringResource(
    when (a) {
        PenAction.NOTHING -> R.string.action_nothing
        PenAction.TOGGLE_ERASER -> R.string.action_toggle_eraser
        PenAction.HOLD_ERASER -> R.string.action_hold_eraser
        PenAction.TOGGLE_HIGHLIGHTER -> R.string.action_toggle_highlighter
        PenAction.UNDO -> R.string.undo
        PenAction.REDO -> R.string.redo
    },
)

@Composable
private fun eraserEndLabel(e: EraserEnd): String = stringResource(
    when (e) {
        EraserEnd.ERASE -> R.string.eraser_end_erase
        EraserEnd.PEN -> R.string.eraser_end_pen
        EraserEnd.NOTHING -> R.string.action_nothing
    },
)

@Composable
private fun InputPage(vm: MainViewModel) {
    val s by vm.appSettings.collectAsState()
    val input by vm.inputSettings.collectAsState()
    val capturing by vm.capturingSecondary.collectAsState()

    GroupTitle(stringResource(R.string.set_general))
    Group {
        ChoiceRow(
            title = stringResource(R.string.set_primary_input),
            options = PenPolicy.entries,
            selected = input.policy,
            label = { penPolicyTitle(it) },
            onSelect = vm::setPenPolicy,
        )
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.set_finger_action),
            subtitle = inputModeDetail(input.mode),
            options = InputMode.entries,
            selected = input.mode,
            label = { fingerActionLabel(it) },
            onSelect = vm::setInputMode,
        )
        RowDivider()
        ActionRow(
            stringResource(R.string.set_manage_pens),
            stringResource(R.string.set_manage_pens_detail),
            value = input.pens.size.toString(),
            onClick = vm::showRegisterPen,
        )
    }

    GroupTitle(stringResource(R.string.set_pen_buttons))
    Group {
        ChoiceRow(
            title = stringResource(R.string.set_primary_button),
            options = PenAction.entries,
            selected = s.buttons.primary,
            label = { penActionLabel(it) },
            onSelect = { a -> vm.updateSettings { it.copy(buttons = it.buttons.copy(primary = a)) } },
        )
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.set_secondary_button),
            options = PenAction.entries,
            selected = s.buttons.secondary,
            label = { penActionLabel(it) },
            onSelect = { a -> vm.updateSettings { it.copy(buttons = it.buttons.copy(secondary = a)) } },
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.set_secondary_key),
            subtitle = stringResource(if (capturing) R.string.set_secondary_key_listening else R.string.set_secondary_key_detail),
            value = if (s.secondaryKey == -1) stringResource(R.string.not_set) else s.secondaryKey.toString(),
            onClick = { if (capturing) vm.cancelCaptureSecondary() else vm.startCaptureSecondary() },
        )
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.set_eraser_end),
            options = EraserEnd.entries,
            selected = s.buttons.eraserEnd,
            label = { eraserEndLabel(it) },
            onSelect = { e -> vm.updateSettings { it.copy(buttons = it.buttons.copy(eraserEnd = e)) } },
        )
    }
    InfoCard(stringResource(R.string.set_buttons_info))
}

@Composable
private fun fingerActionLabel(m: InputMode): String = stringResource(
    when (m) {
        InputMode.PEN_ONLY -> R.string.finger_scroll
        InputMode.PEN_AND_FINGER -> R.string.finger_draw
        InputMode.AUTO -> R.string.finger_draw_until_pen
    },
)

@Composable
private fun NoteDefaultsPage(vm: MainViewModel) {
    val s by vm.appSettings.collectAsState()
    var pickPaper by remember { mutableStateOf(false) }

    GroupTitle(stringResource(R.string.set_general))
    Group {
        ActionRow(stringResource(R.string.default_paper), value = paperName(s.defaultTemplate)) { pickPaper = true }
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.set_page_size),
            options = PaperSize.entries,
            selected = s.paperSize,
            label = { stringResource(if (it == PaperSize.LETTER) R.string.size_letter else R.string.size_a4) },
            onSelect = { p -> vm.updateSettings { it.copy(paperSize = p) } },
        )
        RowDivider()
        SwitchRow(stringResource(R.string.set_save_positions), stringResource(R.string.set_save_positions_detail), s.savePagePositions) { v ->
            vm.updateSettings { it.copy(savePagePositions = v) }
        }
    }

    GroupTitle(stringResource(R.string.set_zoom))
    Group {
        ChoiceRow(
            title = stringResource(R.string.set_default_zoom),
            options = ZoomMode.entries,
            selected = s.defaultZoom,
            label = {
                stringResource(
                    when (it) {
                        ZoomMode.FIT_WIDTH -> R.string.zoom_fit_width
                        ZoomMode.FIT_PAGE -> R.string.zoom_fit_page
                        ZoomMode.ACTUAL -> R.string.zoom_actual
                    },
                )
            },
            onSelect = { z -> vm.updateSettings { it.copy(defaultZoom = z) } },
        )
        RowDivider()
        ChoiceRow(
            title = stringResource(R.string.set_two_finger_double_tap),
            options = DoubleTapAction.entries,
            selected = s.twoFingerDoubleTap,
            label = {
                stringResource(
                    when (it) {
                        DoubleTapAction.RESET_ZOOM -> R.string.reset_zoom
                        DoubleTapAction.UNDO -> R.string.undo
                        DoubleTapAction.NOTHING -> R.string.action_nothing
                    },
                )
            },
            onSelect = { a -> vm.updateSettings { it.copy(twoFingerDoubleTap = a) } },
        )
    }

    GroupTitle(stringResource(R.string.set_pdf_import))
    Group {
        SwitchRow(
            stringResource(R.string.keep_text_selectable),
            stringResource(if (s.pdfKeepText) R.string.keep_text_on_detail else R.string.keep_text_off_detail),
            s.pdfKeepText,
        ) { v -> vm.updateSettings { it.copy(pdfKeepText = v) } }
    }

    if (pickPaper) {
        CreateSheet(
            mode = SheetMode.PICK,
            paperSize = s.paperSize,
            quickTemplate = s.defaultTemplate,
            onPaper = { id ->
                pickPaper = false
                vm.updateSettings { it.copy(defaultTemplate = id) }
            },
            onDismiss = { pickPaper = false },
        )
    }
}

@Composable
private fun CastingPage(vm: MainViewModel) {
    val s by vm.appSettings.collectAsState()
    GroupTitle(stringResource(R.string.set_external_display))
    Group {
        SwitchRow(stringResource(R.string.set_present), stringResource(R.string.set_present_detail), s.presentOnExternal) { v ->
            vm.updateSettings { it.copy(presentOnExternal = v) }
        }
    }
    InfoCard(stringResource(R.string.set_present_info))
}

@Composable
private fun themeLabel(t: ThemeChoice): String = stringResource(
    when (t) {
        ThemeChoice.SYSTEM -> R.string.theme_system
        ThemeChoice.LIGHT -> R.string.theme_light
        ThemeChoice.DARK -> R.string.theme_dark
        ThemeChoice.SAKURA -> R.string.theme_sakura
        ThemeChoice.OCEAN -> R.string.theme_ocean
        ThemeChoice.FOREST -> R.string.theme_forest
        ThemeChoice.MIDNIGHT -> R.string.theme_midnight
    },
)

/** Two swatches (bar + accent) for each theme. */
@Composable
private fun themeSwatch(t: ThemeChoice): Pair<Color, Color> = when (t) {
    ThemeChoice.SYSTEM -> colorResource(R.color.light_top_bar) to colorResource(R.color.dark_top_bar)
    ThemeChoice.LIGHT -> colorResource(R.color.light_background) to colorResource(R.color.light_accent)
    ThemeChoice.DARK -> colorResource(R.color.dark_background) to colorResource(R.color.dark_accent)
    ThemeChoice.SAKURA -> colorResource(R.color.sakura_background) to colorResource(R.color.sakura_accent)
    ThemeChoice.OCEAN -> colorResource(R.color.ocean_background) to colorResource(R.color.ocean_accent)
    ThemeChoice.FOREST -> colorResource(R.color.forest_background) to colorResource(R.color.forest_accent)
    ThemeChoice.MIDNIGHT -> colorResource(R.color.midnight_background) to colorResource(R.color.midnight_accent)
}

@Composable
private fun ThemePage(vm: MainViewModel) {
    val s by vm.appSettings.collectAsState()
    val c = Palette
    GroupTitle(stringResource(R.string.set_theme))
    Group {
        ThemeChoice.entries.forEachIndexed { i, t ->
            if (i > 0) RowDivider()
            val (a, b) = themeSwatch(t)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.updateSettings { it.copy(theme = t) } }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                RadioButton(selected = s.theme == t, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(themeLabel(t), color = c.textPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(a).border(1.dp, c.divider, CircleShape))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(b).border(1.dp, c.divider, CircleShape))
                }
            }
        }
    }
}

/** null = follow the system language. */
private val LANGUAGES = listOf<String?>(null, "en", "ms", "zh-CN", "ko")

@Composable
private fun LanguagePage() {
    val context = LocalContext.current
    val c = Palette
    val lm = remember { context.getSystemService(LocaleManager::class.java) }
    val current = remember { lm.applicationLocales.toLanguageTags().takeIf { it.isNotBlank() } }
    GroupTitle(stringResource(R.string.set_language))
    Group {
        LANGUAGES.forEachIndexed { i, tag ->
            if (i > 0) RowDivider()
            val selected = current == tag || (tag != null && current?.startsWith(tag) == true)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Android restarts the screen in the new language.
                        lm.applicationLocales = if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(
                    when (tag) {
                        null -> stringResource(R.string.lang_system)
                        "en" -> "English"
                        "ms" -> "Bahasa Melayu"
                        "zh-CN" -> "简体中文"
                        else -> "한국어"
                    },
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
    InfoCard(stringResource(R.string.set_language_info))
}

@Composable
private fun AboutPage(vm: MainViewModel) {
    val context = LocalContext.current
    val version = vm.versionName
    val s by vm.appSettings.collectAsState()
    val checking by vm.checking.collectAsState()
    var dialog by remember { mutableStateOf<Int?>(null) }
    GroupTitle(stringResource(R.string.app_name))
    Group {
        ActionRow(stringResource(R.string.about_version), value = version) {}
        RowDivider()
        ActionRow(stringResource(R.string.about_developer), value = "biasnil") { openLink(context, ProjectLinks.GITHUB) }
        RowDivider()
        ActionRow(stringResource(R.string.about_github), subtitle = ProjectLinks.GITHUB.removePrefix("https://")) {
            openLink(context, ProjectLinks.GITHUB)
        }
        RowDivider()
        ActionRow(stringResource(R.string.about_changelog)) { dialog = R.string.changelog_text }
        RowDivider()
        ActionRow(stringResource(R.string.about_licences)) { dialog = R.string.licences_text }
    }
    GroupTitle(stringResource(R.string.about_updates))
    Group {
        ActionRow(
            stringResource(R.string.update_check_now),
            subtitle = stringResource(if (checking) R.string.update_checking else R.string.update_check_now_detail),
        ) { vm.checkForUpdates(manual = true) }
        RowDivider()
        SwitchRow(stringResource(R.string.update_check_on_start), stringResource(R.string.update_check_on_start_detail), s.checkUpdates) { v ->
            vm.updateSettings { it.copy(checkUpdates = v) }
        }
    }
    GroupTitle(stringResource(R.string.about_support))
    Group {
        ActionRow(stringResource(R.string.report_problem), subtitle = ProjectLinks.REPORT_EMAIL) {
            sendReport(context, version)
        }
    }
    dialog?.let { res ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(if (res == R.string.changelog_text) R.string.about_changelog else R.string.about_licences)) },
            text = { Text(stringResource(res), modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.done)) } },
        )
    }
}
