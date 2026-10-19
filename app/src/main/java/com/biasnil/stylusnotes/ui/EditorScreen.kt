package com.biasnil.stylusnotes.ui

import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Palette as PaletteIcon
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biasnil.stylusnotes.EditorViewModel
import com.biasnil.stylusnotes.MainViewModel
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.StylusNotesApp
import com.biasnil.stylusnotes.canvas.PageCanvasView
import com.biasnil.stylusnotes.canvas.PagePresentation
import com.biasnil.stylusnotes.canvas.PenCommand
import com.biasnil.stylusnotes.canvas.Tool
import com.biasnil.stylusnotes.canvas.ToolSettings
import com.biasnil.stylusnotes.data.DoubleTapAction
import com.biasnil.stylusnotes.data.PageView
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.StylusFilter
import com.biasnil.stylusnotes.util.displayName

@Composable
fun EditorScreen(main: MainViewModel, noteId: String) {
    val vm: EditorViewModel = viewModel()
    val app = LocalContext.current.applicationContext as StylusNotesApp
    val context = LocalContext.current
    val c = Palette
    val compact = isCompact

    LaunchedEffect(noteId) { vm.load(noteId) }

    val note by vm.note.collectAsState()
    val pageIndex by vm.pageIndex.collectAsState()
    val tools by app.tools.collectAsState()
    val input by main.inputSettings.collectAsState()
    val penSeen by main.penSeen.collectAsState()
    val settings by main.appSettings.collectAsState()

    var canvasView by remember { mutableStateOf<PageCanvasView?>(null) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<SheetMode?>(null) }
    var showPages by remember { mutableStateOf(false) }
    var pendingPdf by remember { mutableStateOf<Uri?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sheet = null
            pendingPdf = uri
        }
    }
    val pagesFromImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            sheet = null
            vm.insertImagePages(uris)
        }
    }
    val placeImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.placeImage(uri)
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    // Pictures from the toolbar go through the canvas so they're undoable.
    LaunchedEffect(canvasView) {
        val v = canvasView ?: return@LaunchedEffect
        vm.imagesToPlace.collect { v.addImage(it) }
    }
    // Undo/redo from a pen button.
    LaunchedEffect(canvasView) {
        val v = canvasView ?: return@LaunchedEffect
        app.penCommands.collect { cmd -> if (cmd == PenCommand.UNDO) v.undo() else v.redo() }
    }
    DisposableEffect(Unit) { onDispose { vm.flush() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }

    val close = {
        vm.flush()
        main.closeEditor()
    }
    // "Disable back button": only the toolbar arrow leaves the editor.
    BackHandler { if (!settings.disableBack) close() }

    val n = note
    val page = n?.pages?.getOrNull(pageIndex)
    val pageCount = n?.pages?.size ?: 0

    // ---------------------------------------------------------- external display
    var presentation by remember { mutableStateOf<PagePresentation?>(null) }
    DisposableEffect(settings.presentOnExternal) {
        val dm = context.getSystemService(DisplayManager::class.java)
        fun refresh() {
            val display = if (settings.presentOnExternal) PagePresentation.findDisplay(context) else null
            val cur = presentation
            if (display == null) {
                cur?.dismiss()
                presentation = null
            } else if (cur == null || cur.display.displayId != display.displayId) {
                cur?.dismiss()
                presentation = runCatching { PagePresentation(context, display, app.assets).also { it.show() } }.getOrNull()
            }
        }
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = refresh()
            override fun onDisplayRemoved(displayId: Int) = refresh()
            override fun onDisplayChanged(displayId: Int) = Unit
        }
        dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        refresh()
        onDispose {
            dm.unregisterDisplayListener(listener)
            presentation?.dismiss()
            presentation = null
        }
    }
    LaunchedEffect(presentation, page) {
        if (page != null) presentation?.showPage(page)
    }
    // Repaint when an image or PDF page finishes loading.
    DisposableEffect(canvasView, presentation) {
        val v = canvasView
        val p = presentation
        val listener: () -> Unit = {
            v?.onAssetsLoaded()
            p?.onAssetsLoaded()
        }
        app.assets.addListener(listener)
        onDispose { app.assets.removeListener(listener) }
    }

    // ---------------------------------------------------------- toolbar pieces
    val undoRedo: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { canvasView?.undo() }, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo), tint = barTint(c.onTopBar, canUndo))
        }
        IconButton(onClick = { canvasView?.redo() }, enabled = canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.redo), tint = barTint(c.onTopBar, canRedo))
        }
    }
    val drawingTools: @Composable RowScope.() -> Unit = {
        ToolMenu(tools) { app.tools.value = it }
        ColorMenu(tools) { app.tools.value = it }
        IconButton(onClick = { placeImagePicker.launch(imageOnly) }, enabled = page != null) {
            Icon(Icons.Filled.Image, contentDescription = stringResource(R.string.insert_picture), tint = c.onTopBar)
        }
    }
    val pageNav: @Composable RowScope.() -> Unit = {
        IconButton(onClick = vm::prev, enabled = pageIndex > 0) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_page),
                tint = barTint(c.onTopBar, pageIndex > 0),
            )
        }
        TextButton(onClick = { showPages = true }, enabled = n != null) {
            Text(
                if (pageCount > 0) "${pageIndex + 1} / $pageCount" else "",
                color = c.onTopBar,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val onLast = pageIndex >= pageCount - 1
        IconButton(onClick = { if (onLast) sheet = SheetMode.ADD_PAGE else vm.next() }, enabled = n != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(if (onLast) R.string.add_page else R.string.next_page),
                    tint = c.onTopBar,
                )
                if (onLast) Text("+", color = c.onTopBar, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
    val overflow: @Composable () -> Unit = {
        OverflowMenu(
            isPdfPage = page?.pdf != null,
            inputMode = input.mode,
            penSeen = penSeen,
            onAddPage = { sheet = SheetMode.ADD_PAGE },
            onPages = { showPages = true },
            onBackground = { sheet = SheetMode.BACKGROUND },
            onFit = { canvasView?.fitPage() },
            onInputMode = main::setInputMode,
            onExportPdf = vm::exportPdf,
            onExportPng = vm::exportPagePng,
            onClearPage = { canvasView?.clearPage() },
            onPenSetup = {
                vm.flush()
                main.showRegisterPen()
            },
        )
    }

    Column(Modifier.fillMaxSize().background(c.desk)) {
        // ---------------------------------------------------------- top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(c.topBar)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = close) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.onTopBar)
            }
            var title by remember(n?.id) { mutableStateOf(n?.title ?: "") }
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (title.isEmpty()) {
                    Text(stringResource(R.string.untitled), color = c.onTopBar.copy(alpha = 0.55f), fontSize = 20.sp, maxLines = 1)
                }
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        vm.setTitle(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = c.onTopBar, fontSize = 20.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            undoRedo()
            if (!compact) {
                drawingTools()
                pageNav()
            }
            overflow()
        }

        // ---------------------------------------------------------- page
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (compact) Modifier else Modifier.navigationBarsPadding())
                .clipToBounds(),
        ) {
            if (n != null) {
                AndroidView(
                    factory = { ctx ->
                        PageCanvasView(ctx).also { v ->
                            v.filter = app.stylusFilter
                            v.assets = app.assets
                            v.listener = object : PageCanvasView.Listener {
                                override fun onContentChanged(pageId: String, strokes: List<Stroke>, images: List<PlacedImage>) =
                                    vm.onContentChanged(pageId, strokes, images)

                                override fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) = setHistory(canUndo, canRedo)

                                override fun onRejected(verdict: StylusFilter.Verdict) = main.onRejected(verdict)

                                override fun onSelectionChanged(hasSelection: Boolean) = setSelection(hasSelection)

                                override fun onTwoFingerDoubleTap() = doubleTap()

                                override fun onViewChanged(pageId: String, view: PageView) = vm.onViewChanged(pageId, view)

                                fun setHistory(u: Boolean, r: Boolean) {
                                    canUndo = u
                                    canRedo = r
                                }

                                fun setSelection(s: Boolean) {
                                    hasSelection = s
                                }

                                fun doubleTap() {
                                    when (main.appSettings.value.twoFingerDoubleTap) {
                                        DoubleTapAction.RESET_ZOOM -> v.fitPage()
                                        DoubleTapAction.UNDO -> v.undo()
                                        DoubleTapAction.NOTHING -> Unit
                                    }
                                }
                            }
                            canvasView = v
                        }
                    },
                    update = { v ->
                        v.tools = tools
                        v.buttons = settings.buttons
                        v.zoomMode = settings.defaultZoom
                        v.restoreViews = settings.savePagePositions
                        v.deskColor = c.desk.toArgb()
                        v.keepScreenOn = settings.keepScreenOn
                        page?.let { v.setPage(it) }
                    },
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                )
            }
            if (hasSelection) {
                PictureBar(
                    onRotateLeft = { canvasView?.rotateSelected(-90f) },
                    onRotateRight = { canvasView?.rotateSelected(90f) },
                    onFront = { canvasView?.bringSelectedToFront() },
                    onDelete = { canvasView?.deleteSelectedImage() },
                    onDone = { canvasView?.deselect() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                )
            }
        }

        // ---------------------------------------------------------- bottom bar (phones)
        if (compact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.topBar)
                    .navigationBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) { drawingTools() }
                Row(verticalAlignment = Alignment.CenterVertically) { pageNav() }
            }
        }
    }

    // ---------------------------------------------------------- sheets & dialogs
    when (val mode = sheet) {
        SheetMode.ADD_PAGE -> CreateSheet(
            mode = mode,
            paperSize = settings.paperSize,
            quickTemplate = settings.defaultTemplate,
            onPaper = {
                sheet = null
                vm.addTemplatePage(it)
            },
            onDismiss = { sheet = null },
            onSame = {
                sheet = null
                vm.addSamePage()
            },
            onPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
            onImage = { pagesFromImages.launch(imageOnly) },
        )
        SheetMode.BACKGROUND -> CreateSheet(
            mode = mode,
            paperSize = settings.paperSize,
            quickTemplate = page?.template ?: settings.defaultTemplate,
            onPaper = {
                sheet = null
                vm.changeBackground(it)
            },
            onDismiss = { sheet = null },
        )
        else -> Unit
    }

    pendingPdf?.let { uri ->
        PdfImportDialog(
            fileName = remember(uri) { displayName(context, uri) ?: "PDF" },
            defaultKeepText = settings.pdfKeepText,
            onImport = { keep ->
                pendingPdf = null
                vm.insertPdf(uri, keep)
            },
            onDismiss = { pendingPdf = null },
        )
    }

    if (showPages && n != null) {
        PagesDialog(
            note = n,
            current = pageIndex,
            onGo = {
                vm.goTo(it)
                showPages = false
            },
            onMove = vm::movePage,
            onDuplicate = vm::duplicatePage,
            onDelete = vm::deletePage,
            onDismiss = { showPages = false },
        )
    }
}

private fun barTint(color: Color, enabled: Boolean) = if (enabled) color else color.copy(alpha = 0.35f)

private fun toolIcon(t: Tool): ImageVector = when (t) {
    Tool.PEN -> Icons.Filled.Edit
    Tool.HIGHLIGHTER -> Icons.Filled.BorderColor
    Tool.ERASER -> Icons.Filled.AutoFixNormal
    Tool.SELECT -> Icons.Filled.PanTool
}

@Composable
private fun toolName(t: Tool): String = stringResource(
    when (t) {
        Tool.PEN -> R.string.tool_pen
        Tool.HIGHLIGHTER -> R.string.tool_highlighter
        Tool.ERASER -> R.string.tool_eraser
        Tool.SELECT -> R.string.tool_select
    },
)

@Composable
private fun ToolMenu(tools: ToolSettings, onChange: (ToolSettings) -> Unit) {
    val c = Palette
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(toolIcon(tools.tool), contentDescription = toolName(tools.tool), tint = c.accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Tool.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(toolName(t)) },
                    leadingIcon = { Icon(toolIcon(t), contentDescription = null) },
                    trailingIcon = { if (t == tools.tool) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        onChange(tools.select(t))
                        open = false
                    },
                )
            }
            HorizontalDivider()
            val highlighter = tools.tool == Tool.HIGHLIGHTER
            Text(
                stringResource(if (highlighter) R.string.highlighter_size else R.string.pen_size),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val widths = if (highlighter) ToolSettings.HIGHLIGHTER_WIDTHS else ToolSettings.PEN_WIDTHS
                val current = if (highlighter) tools.highlighterWidth else tools.penWidth
                widths.forEachIndexed { i, wv ->
                    val selected = wv == current
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) c.selected else Color.Transparent)
                            .clickable {
                                onChange(if (highlighter) tools.copy(highlighterWidth = wv) else tools.copy(penWidth = wv))
                            },
                    ) {
                        Box(Modifier.size((6 + i * 6).dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorMenu(tools: ToolSettings, onChange: (ToolSettings) -> Unit) {
    val c = Palette
    var open by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val highlighter = tools.tool == Tool.HIGHLIGHTER
    val shown = if (highlighter) tools.highlighterColor else tools.penColor

    // Picking a pen colour while erasing or moving switches back to the pen.
    fun apply(col: Int): ToolSettings =
        if (highlighter) tools.copy(highlighterColor = col)
        else if (tools.tool == Tool.PEN) tools.copy(penColor = col)
        else tools.select(Tool.PEN).copy(penColor = col)

    @Composable
    fun Swatch(col: Int, onClick: () -> Unit) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(col))
                .border(if (col == shown) 3.dp else 1.dp, if (col == shown) c.accent else Color.Gray, CircleShape)
                .clickable(onClick = onClick),
        )
    }

    Box {
        IconButton(onClick = { open = true }) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(shown))
                    .border(2.dp, c.onTopBar, CircleShape),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val colors = if (highlighter) ToolSettings.HIGHLIGHTER_COLORS else ToolSettings.PEN_COLORS
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                colors.forEach { col -> Swatch(col) { onChange(apply(col)); open = false } }
            }
            val recent = tools.recentColors.filter { it !in colors }
            if (recent.isNotEmpty()) {
                Text(
                    stringResource(R.string.recent_colors),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    recent.take(6).forEach { col -> Swatch(col) { onChange(apply(col).withRecent(col)); open = false } }
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.more_colors)) },
                leadingIcon = { Icon(Icons.Filled.PaletteIcon, contentDescription = null) },
                onClick = { open = false; picker = true },
            )
        }
    }
    if (picker) {
        ColorPickerDialog(
            initial = shown,
            onPick = { col ->
                onChange(apply(col).withRecent(col))
                picker = false
            },
            onDismiss = { picker = false },
        )
    }
}

/** Floating bar shown while a picture is selected. */
@Composable
private fun PictureBar(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFront: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(c.topBar)
            .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = onRotateLeft) {
            Icon(Icons.Filled.RotateLeft, stringResource(R.string.rotate_left), tint = c.onTopBar)
        }
        IconButton(onClick = onRotateRight) {
            Icon(Icons.Filled.RotateRight, stringResource(R.string.rotate_right), tint = c.onTopBar)
        }
        IconButton(onClick = onFront) {
            Icon(Icons.Filled.FlipToFront, stringResource(R.string.bring_to_front), tint = c.onTopBar)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, stringResource(R.string.delete_picture), tint = c.onTopBar)
        }
        TextButton(onClick = onDone) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.done))
        }
    }
}

@Composable
private fun OverflowMenu(
    isPdfPage: Boolean,
    inputMode: InputMode,
    penSeen: Boolean,
    onAddPage: () -> Unit,
    onPages: () -> Unit,
    onBackground: () -> Unit,
    onFit: () -> Unit,
    onInputMode: (InputMode) -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onClearPage: () -> Unit,
    onPenSetup: () -> Unit,
) {
    val c = Palette
    var open by remember { mutableStateOf(false) }
    val close = { open = false }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more), tint = c.onTopBar)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(stringResource(R.string.add_page_menu), close = close, action = onAddPage)
            MenuItem(stringResource(R.string.pages_menu), close = close, action = onPages)
            MenuItem(stringResource(R.string.change_background_menu), enabled = !isPdfPage, close = close, action = onBackground)
            MenuItem(stringResource(R.string.reset_zoom), close = close, action = onFit)
            HorizontalDivider()
            Text(
                stringResource(R.string.what_can_draw),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            InputMode.entries.forEach { m ->
                val base = inputModeTitle(m)
                val label = if (m == InputMode.AUTO && m == inputMode && penSeen) stringResource(R.string.auto_pen_only_now, base) else base
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (m == inputMode) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        onInputMode(m)
                    },
                )
            }
            HorizontalDivider()
            MenuItem(stringResource(R.string.export_pdf), close = close, action = onExportPdf)
            MenuItem(stringResource(R.string.export_png), close = close, action = onExportPng)
            MenuItem(stringResource(R.string.clear_page), close = close, action = onClearPage)
            MenuItem(stringResource(R.string.pen_setup), close = close, action = onPenSetup)
        }
    }
}

@Composable
private fun MenuItem(label: String, enabled: Boolean = true, close: () -> Unit, action: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        enabled = enabled,
        onClick = {
            close()
            action()
        },
    )
}
