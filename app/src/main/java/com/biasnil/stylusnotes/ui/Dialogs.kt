package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.data.LibraryIndex
import com.biasnil.stylusnotes.data.LibraryOps
import com.biasnil.stylusnotes.data.Note
import com.biasnil.stylusnotes.data.Page
import kotlinx.coroutines.delay

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Folder picker. [exclude] hides a folder and its subfolders (when moving a folder). */
@Composable
fun MoveDialog(
    index: LibraryIndex,
    exclude: Set<String>,
    current: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val tree = remember(index, exclude) { LibraryOps.folderTree(index, exclude) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_title)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item { FolderRow(stringResource(R.string.top_level), 0, current == null) { onPick(null) } }
                items(tree) { (f, depth) -> FolderRow(f.name, depth + 1, f.id == current) { onPick(f.id) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun FolderRow(name: String, depth: Int, current: Boolean, onClick: () -> Unit) {
    val c = Palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (current) c.selected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = (8 + depth * 18).dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = c.folder)
        Spacer(Modifier.width(10.dp))
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PdfImportDialog(
    fileName: String,
    defaultKeepText: Boolean,
    onImport: (keepText: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var keep by remember { mutableStateOf(defaultKeepText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_pdf)) },
        text = {
            Column {
                Text(fileName, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.keep_text_selectable), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (keep) R.string.keep_text_on_detail else R.string.keep_text_off_detail),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = keep, onCheckedChange = { keep = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onImport(keep) }) { Text(stringResource(R.string.import_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Page list. Each row has its own ⋮ menu so it fits on a phone too. */
@Composable
fun PagesDialog(
    note: Note,
    current: Int,
    onGo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Palette
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pages_title, note.pages.size)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                itemsIndexed(note.pages, key = { _, p -> p.id }) { i, p ->
                    var menu by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i == current) c.selected else Color.Transparent)
                            .clickable { onGo(i) }
                            .padding(8.dp),
                    ) {
                        Box(Modifier.size(width = 40.dp, height = 52.dp).background(Color.White)) {
                            if (p.pdf == null) {
                                PaperPreview(p.template, p.width, p.height, Modifier.fillMaxSize())
                            } else {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(stringResource(R.string.page_n, i + 1), maxLines = 1)
                            Text(
                                describe(p),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_up)) },
                                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                                    enabled = i > 0,
                                    onClick = { menu = false; onMove(i, -1) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_down)) },
                                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                                    enabled = i < note.pages.lastIndex,
                                    onClick = { menu = false; onMove(i, 1) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.duplicate)) },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                    onClick = { menu = false; onDuplicate(i) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                    enabled = note.pages.size > 1,
                                    onClick = { menu = false; onDelete(i) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun describe(p: Page): String {
    val ref = p.pdf
    val base = if (ref != null) stringResource(R.string.pdf_page_n, ref.pageIndex + 1) else paperName(p.template)
    return if (p.strokes.isEmpty()) base else stringResource(R.string.page_detail_strokes, base, p.strokes.size)
}
