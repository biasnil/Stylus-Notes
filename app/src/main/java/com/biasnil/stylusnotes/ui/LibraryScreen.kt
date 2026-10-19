package com.biasnil.stylusnotes.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.MainViewModel
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.Section
import com.biasnil.stylusnotes.StylusNotesApp
import com.biasnil.stylusnotes.data.Folder
import com.biasnil.stylusnotes.data.LibraryOps
import com.biasnil.stylusnotes.data.NoteMeta
import com.biasnil.stylusnotes.data.SortMode
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenPolicy
import com.biasnil.stylusnotes.util.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private sealed interface LibDialog {
    data object NewFolder : LibDialog
    data class RenameFolder(val folder: Folder) : LibDialog
    data class MoveFolder(val folder: Folder) : LibDialog
    data class DeleteFolder(val folder: Folder) : LibDialog
    data class RenameNote(val note: NoteMeta) : LibDialog
    data class MoveNote(val note: NoteMeta) : LibDialog
    data class DeleteForever(val note: NoteMeta) : LibDialog
    data object EmptyTrash : LibDialog
}

/** Actions behind the Create button. */
private class CreateActions(
    val newNote: () -> Unit,
    val newFolder: () -> Unit,
    val importPdf: () -> Unit,
    val fromPictures: () -> Unit,
)

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val c = Palette
    val compact = isCompact
    val index by vm.index.collectAsState()
    val section by vm.section.collectAsState()
    val folderId by vm.folder.collectAsState()
    val settings by vm.appSettings.collectAsState()
    val input by vm.inputSettings.collectAsState()
    val context = LocalContext.current

    var showCreate by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<LibDialog?>(null) }
    var pendingPdf by remember { mutableStateOf<Uri?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            showCreate = false
            pendingPdf = uri
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            showCreate = false
            vm.createNoteFromImages(uris)
        }
    }
    val pickPdf = { pdfPicker.launch(arrayOf("application/pdf")) }
    val pickImages = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val create = CreateActions(
        newNote = { showCreate = true },
        newFolder = { dialog = LibDialog.NewFolder },
        importPdf = pickPdf,
        fromPictures = pickImages,
    )

    val nothingCanDraw = input.pens.isEmpty() && input.mode == InputMode.PEN_ONLY && input.policy == PenPolicy.REGISTERED_ONLY
    val onSection: (Section) -> Unit = { if (it == Section.NOTES) vm.openFolder(folderId) else vm.openSection(it) }

    if (compact) {
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = c.sidebar, modifier = Modifier.width(280.dp)) {
                    SidebarContent(
                        section = section,
                        needsPen = nothingCanDraw,
                        showCreate = false,
                        create = create,
                        onSection = { s -> onSection(s); scope.launch { drawer.close() } },
                        onPenSetup = { scope.launch { drawer.close() }; vm.showRegisterPen() },
                        onSettings = { scope.launch { drawer.close() }; vm.showSettings() },
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(c.topBar).height(56.dp).padding(horizontal = 4.dp),
                    ) {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu), tint = c.onTopBar)
                        }
                        Box(Modifier.weight(1f)) {
                            Breadcrumb(vm, section, folderId, onTopBar = true)
                        }
                        HeaderActions(vm, section, settings.sort, index.notes.any { it.trashedAt != null }, onTopBar = true) {
                            dialog = LibDialog.EmptyTrash
                        }
                    }
                    LibraryBody(vm, Modifier.padding(horizontal = 12.dp), nothingCanDraw) { dialog = it }
                }
                CreateFab(create, Modifier.align(Alignment.BottomEnd).padding(20.dp))
            }
        }
    } else {
        Row(Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
            Column(
                Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(c.sidebar),
            ) {
                SidebarContent(
                    section = section,
                    needsPen = nothingCanDraw,
                    showCreate = true,
                    create = create,
                    onSection = onSection,
                    onPenSetup = vm::showRegisterPen,
                    onSettings = vm::showSettings,
                )
            }
            Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Box(Modifier.weight(1f)) { Breadcrumb(vm, section, folderId, onTopBar = false) }
                    HeaderActions(vm, section, settings.sort, index.notes.any { it.trashedAt != null }, onTopBar = false) {
                        dialog = LibDialog.EmptyTrash
                    }
                }
                LibraryBody(vm, Modifier, nothingCanDraw) { dialog = it }
            }
        }
    }

    // ---------------------------------------------------------- sheets & dialogs
    if (showCreate) {
        CreateSheet(
            mode = SheetMode.NEW_NOTE,
            paperSize = settings.paperSize,
            quickTemplate = settings.defaultTemplate,
            onPaper = {
                showCreate = false
                vm.createNoteFromPaper(it)
            },
            onDismiss = { showCreate = false },
            onPdf = pickPdf,
            onImage = pickImages,
        )
    }

    pendingPdf?.let { uri ->
        PdfImportDialog(
            fileName = remember(uri) { displayName(context, uri) ?: "PDF" },
            defaultKeepText = settings.pdfKeepText,
            onImport = { keep ->
                pendingPdf = null
                vm.createNoteFromPdf(uri, keep)
            },
            onDismiss = { pendingPdf = null },
        )
    }

    LibraryDialogs(vm, dialog) { dialog = null }
}

// ---------------------------------------------------------------------- header

@Composable
private fun Breadcrumb(vm: MainViewModel, section: Section, folderId: String?, onTopBar: Boolean) {
    val c = Palette
    val index by vm.index.collectAsState()
    val main = if (onTopBar) c.onTopBar else c.textPrimary
    val dim = if (onTopBar) c.onTopBar.copy(alpha = 0.7f) else c.textSecondary
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
        when (section) {
            Section.NOTES -> {
                Crumb(stringResource(R.string.notes), folderId == null, main, dim) { vm.openFolder(null) }
                LibraryOps.path(index, folderId).forEach { f ->
                    Text("  ›  ", color = dim, style = MaterialTheme.typography.titleLarge)
                    Crumb(f.name, f.id == folderId, main, dim) { vm.openFolder(f.id) }
                }
            }
            Section.RECENT -> Crumb(stringResource(R.string.recent), true, main, dim) {}
            Section.TRASH -> Crumb(stringResource(R.string.trash), true, main, dim) {}
        }
    }
}

@Composable
private fun HeaderActions(
    vm: MainViewModel,
    section: Section,
    sort: SortMode,
    hasTrash: Boolean,
    onTopBar: Boolean,
    onEmptyTrash: () -> Unit,
) {
    val c = Palette
    val color = if (onTopBar) c.onTopBar else c.accent
    when (section) {
        Section.NOTES -> TextButton(onClick = {
            vm.updateSettings { it.copy(sort = if (sort == SortMode.NAME) SortMode.MODIFIED else SortMode.NAME) }
        }) {
            Text(stringResource(if (sort == SortMode.NAME) R.string.sort_name else R.string.sort_modified), color = color)
        }
        Section.TRASH -> if (hasTrash) {
            TextButton(onClick = onEmptyTrash) { Text(stringResource(R.string.empty_trash), color = color) }
        }
        Section.RECENT -> Unit
    }
}

@Composable
private fun Crumb(text: String, current: Boolean, main: Color, dim: Color, onClick: () -> Unit) {
    Text(
        text,
        color = if (current) main else dim,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp),
    )
}

// ---------------------------------------------------------------------- body

@Composable
private fun LibraryBody(vm: MainViewModel, modifier: Modifier, nothingCanDraw: Boolean, openDialog: (LibDialog) -> Unit) {
    val c = Palette
    val compact = isCompact
    val index by vm.index.collectAsState()
    val section by vm.section.collectAsState()
    val folderId by vm.folder.collectAsState()
    val settings by vm.appSettings.collectAsState()
    val thumbVersion by vm.thumbVersion.collectAsState()

    Column(modifier.fillMaxSize()) {
        if (nothingCanDraw) {
            Card(
                onClick = vm::showRegisterPen,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(stringResource(R.string.nothing_can_draw), modifier = Modifier.padding(16.dp))
            }
        }

        val folders = if (section == Section.NOTES) LibraryOps.childFolders(index, folderId, settings.sort) else emptyList()
        val notes = when (section) {
            Section.NOTES -> LibraryOps.notesIn(index, folderId, settings.sort)
            Section.RECENT -> LibraryOps.recent(index)
            Section.TRASH -> LibraryOps.trash(index)
        }
        val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

        if (folders.isEmpty() && notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(
                        when (section) {
                            Section.NOTES -> if (folderId == null) R.string.empty_notes else R.string.empty_folder
                            Section.RECENT -> R.string.empty_recent
                            Section.TRASH -> R.string.empty_trash_list
                        },
                    ),
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Column
        }

        val sRename = stringResource(R.string.rename)
        val sMove = stringResource(R.string.move_to)
        val sDelete = stringResource(R.string.delete)
        val sOpen = stringResource(R.string.open)
        val sRestore = stringResource(R.string.restore)
        val sDeleteForever = stringResource(R.string.delete_forever)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(if (compact) 140.dp else 150.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(folders, key = { "f_" + it.id }) { f ->
                TileWithMenu(
                    onClick = { vm.openFolder(f.id) },
                    actions = listOf(
                        sRename to { openDialog(LibDialog.RenameFolder(f)) },
                        sMove to { openDialog(LibDialog.MoveFolder(f)) },
                        sDelete to { openDialog(LibDialog.DeleteFolder(f)) },
                    ),
                ) { FolderTile(f) }
            }
            items(notes, key = { "n_" + it.id }) { n ->
                val inTrash = section == Section.TRASH
                val subtitle = if (inTrash) {
                    stringResource(R.string.deleted_on, dateFormat.format(Date(n.trashedAt ?: n.modified)))
                } else {
                    dateFormat.format(Date(n.modified)) + " · " + pluralStringResource(R.plurals.page_count, n.pageCount, n.pageCount)
                }
                val actions: List<Pair<String, () -> Unit>> = if (inTrash) listOf(
                    sRestore to { vm.restoreNote(n.id) },
                    sDeleteForever to { openDialog(LibDialog.DeleteForever(n)) },
                ) else listOf(
                    sOpen to { vm.openNote(n.id) },
                    sRename to { openDialog(LibDialog.RenameNote(n)) },
                    sMove to { openDialog(LibDialog.MoveNote(n)) },
                    sDelete to { vm.trashNote(n.id) },
                )
                TileWithMenu(onClick = if (inTrash) null else ({ vm.openNote(n.id) }), actions = actions) {
                    NoteTile(n, subtitle, thumbVersion)
                }
            }
        }
    }
}

@Composable
private fun LibraryDialogs(vm: MainViewModel, dialog: LibDialog?, close: () -> Unit) {
    val index by vm.index.collectAsState()
    when (dialog) {
        null -> Unit
        LibDialog.NewFolder -> TextInputDialog(
            stringResource(R.string.new_folder), stringResource(R.string.new_folder), stringResource(R.string.create_action),
            { vm.createFolder(it); close() }, close,
        )
        is LibDialog.RenameFolder -> TextInputDialog(
            stringResource(R.string.rename_folder), dialog.folder.name, stringResource(R.string.rename),
            { vm.renameFolder(dialog.folder.id, it); close() }, close,
        )
        is LibDialog.RenameNote -> TextInputDialog(
            stringResource(R.string.rename_note), dialog.note.title, stringResource(R.string.rename),
            { vm.renameNote(dialog.note.id, it); close() }, close,
        )
        is LibDialog.MoveFolder -> MoveDialog(
            index = index,
            exclude = LibraryOps.descendants(index, dialog.folder.id),
            current = dialog.folder.parentId,
            onPick = { vm.moveFolder(dialog.folder.id, it); close() },
            onDismiss = close,
        )
        is LibDialog.MoveNote -> MoveDialog(
            index = index,
            exclude = emptySet(),
            current = dialog.note.folderId,
            onPick = { vm.moveNote(dialog.note.id, it); close() },
            onDismiss = close,
        )
        is LibDialog.DeleteFolder -> ConfirmDialog(
            stringResource(R.string.delete_folder_title, dialog.folder.name),
            stringResource(R.string.delete_folder_text),
            stringResource(R.string.delete),
            { vm.deleteFolder(dialog.folder.id); close() },
            close,
        )
        is LibDialog.DeleteForever -> ConfirmDialog(
            stringResource(R.string.delete_forever_title),
            stringResource(R.string.delete_forever_text, dialog.note.title.ifBlank { stringResource(R.string.untitled) }),
            stringResource(R.string.delete),
            { vm.deleteForever(dialog.note.id); close() },
            close,
        )
        LibDialog.EmptyTrash -> ConfirmDialog(
            stringResource(R.string.empty_trash_title),
            stringResource(R.string.empty_trash_text),
            stringResource(R.string.empty_action),
            { vm.emptyTrash(); close() },
            close,
        )
    }
}

// ---------------------------------------------------------------------- sidebar

@Composable
private fun ColumnScope.SidebarContent(
    section: Section,
    needsPen: Boolean,
    showCreate: Boolean,
    create: CreateActions,
    onSection: (Section) -> Unit,
    onPenSetup: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = Palette
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 18.dp)) {
            Icon(Icons.Filled.Gesture, contentDescription = null, tint = c.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_name), color = c.accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (showCreate) CreatePill(create)
        NavItem(Icons.Filled.Description, stringResource(R.string.notes), section == Section.NOTES) { onSection(Section.NOTES) }
        NavItem(Icons.Filled.History, stringResource(R.string.recent), section == Section.RECENT) { onSection(Section.RECENT) }
        NavItem(Icons.Filled.Delete, stringResource(R.string.trash), section == Section.TRASH) { onSection(Section.TRASH) }
    }
    HorizontalDivider(color = c.divider, modifier = Modifier.padding(horizontal = 14.dp))
    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        NavItem(Icons.Filled.Gesture, stringResource(R.string.pen_setup), false, badge = needsPen, onClick = onPenSetup)
        NavItem(Icons.Filled.Settings, stringResource(R.string.settings), false, onClick = onSettings)
    }
}

@Composable
private fun CreateMenuItems(create: CreateActions, close: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.new_note)) },
        leadingIcon = { Icon(Icons.Filled.Description, null) },
        onClick = { close(); create.newNote() },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.new_folder)) },
        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
        onClick = { close(); create.newFolder() },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.import_pdf)) },
        leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
        onClick = { close(); create.importPdf() },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.note_from_pictures)) },
        leadingIcon = { Icon(Icons.Filled.Image, null) },
        onClick = { close(); create.fromPictures() },
    )
}

@Composable
private fun CreatePill(create: CreateActions) {
    val c = Palette
    var menu by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { menu = true },
            shape = CircleShape,
            color = c.accent,
            contentColor = c.onAccent,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 18.dp, end = 26.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.create), style = MaterialTheme.typography.titleMedium)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            CreateMenuItems(create) { menu = false }
        }
    }
}

@Composable
private fun CreateFab(create: CreateActions, modifier: Modifier) {
    val c = Palette
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        FloatingActionButton(onClick = { menu = true }, containerColor = c.accent, contentColor = c.onAccent) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            CreateMenuItems(create) { menu = false }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, badge: Boolean = false, onClick: () -> Unit) {
    val c = Palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(CircleShape)
            .background(if (selected) c.selected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) c.accent else c.textSecondary)
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (selected) c.textPrimary else c.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        if (badge) Box(Modifier.size(8.dp).background(Color(0xFFFF8A80), CircleShape))
    }
}

// ---------------------------------------------------------------------- tiles

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileWithMenu(
    onClick: (() -> Unit)?,
    actions: List<Pair<String, () -> Unit>>,
    content: @Composable () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (onClick != null) onClick() else menu = true },
                onLongClick = { menu = true },
            ),
    ) {
        content()
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            actions.forEach { (label, act) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; act() })
            }
        }
    }
}

@Composable
private fun FolderTile(folder: Folder) {
    val c = Palette
    val tab = c.folderTab
    val body = c.folder
    Box(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
        Canvas(Modifier.fillMaxSize()) {
            val tabW = size.width * 0.42f
            val tabH = size.height * 0.13f
            val r = 14.dp.toPx()
            drawRoundRect(tab, topLeft = Offset(0f, 0f), size = Size(tabW, tabH * 2), cornerRadius = CornerRadius(r * 0.6f))
            drawRoundRect(body, topLeft = Offset(0f, tabH), size = Size(size.width, size.height - tabH), cornerRadius = CornerRadius(r))
        }
        Text(
            folder.name,
            color = c.onFolder,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp, top = 34.dp, end = 14.dp),
        )
    }
}

@Composable
private fun NoteTile(meta: NoteMeta, subtitle: String, thumbVersion: Int) {
    val c = Palette
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
        ) {
            NoteThumbnail(meta, thumbVersion, Modifier.fillMaxSize())
        }
        Text(
            meta.title.ifBlank { stringResource(R.string.untitled) },
            color = c.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp),
        )
        Text(
            subtitle,
            color = c.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun NoteThumbnail(meta: NoteMeta, version: Int, modifier: Modifier) {
    val app = LocalContext.current.applicationContext as StylusNotesApp
    val bmp by produceState<ImageBitmap?>(null, meta.id, meta.modified, version) {
        value = withContext(Dispatchers.IO) {
            val f = app.library.thumbFile(meta.id)
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
        }
    }
    bmp?.let {
        Image(it, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = modifier)
    }
}
