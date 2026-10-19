package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.paper.PaperCatalog
import com.biasnil.stylusnotes.paper.PaperCategory
import com.biasnil.stylusnotes.paper.PaperSize
import com.biasnil.stylusnotes.paper.PaperSpec

/**
 * NEW_NOTE: create a note. ADD_PAGE: add a page to the open note.
 * BACKGROUND: change this page's paper. PICK: choose the default paper in Settings.
 */
enum class SheetMode { NEW_NOTE, ADD_PAGE, BACKGROUND, PICK }

/**
 * Full-screen picker: quick buttons along the top (paper / PDF / image),
 * then every paper background grouped by category.
 */
@Composable
fun CreateSheet(
    mode: SheetMode,
    paperSize: PaperSize,
    quickTemplate: String,
    onPaper: (String) -> Unit,
    onDismiss: () -> Unit,
    onSame: () -> Unit = {},
    onPdf: () -> Unit = {},
    onImage: () -> Unit = {},
) {
    val c = Palette
    val compact = isCompact
    var tabName by rememberSaveable { mutableStateOf<String?>(null) }
    val tab = tabName?.let { PaperCategory.valueOf(it) }
    val title = stringResource(
        when (mode) {
            SheetMode.NEW_NOTE -> R.string.create_note_from
            SheetMode.ADD_PAGE -> R.string.add_page
            SheetMode.BACKGROUND -> R.string.change_background
            SheetMode.PICK -> R.string.default_paper
        },
    )
    val showQuick = mode == SheetMode.NEW_NOTE || mode == SheetMode.ADD_PAGE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(c.topBar)) {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = c.onTopBar)
                }
                Text(
                    title,
                    color = c.onTopBar,
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (showQuick) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 36.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
                ) {
                    val itemWidth = if (compact) 96.dp else 110.dp
                    if (mode == SheetMode.ADD_PAGE) {
                        QuickButton(Icons.Filled.Description, stringResource(R.string.same_as_this_page), itemWidth, onSame)
                    } else {
                        QuickButton(Icons.Filled.Description, paperName(quickTemplate), itemWidth) { onPaper(quickTemplate) }
                    }
                    QuickButton(Icons.Filled.PictureAsPdf, stringResource(R.string.pdf), itemWidth, onPdf)
                    QuickButton(Icons.Filled.Image, stringResource(R.string.image), itemWidth, onImage)
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(c.background)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(c.textSecondary, CircleShape),
                )
                Text(
                    stringResource(R.string.paper_background),
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 12.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    TabPill(stringResource(R.string.all), tab == null) { tabName = null }
                    PaperCategory.entries.forEach { cat -> TabPill(categoryName(cat), tab == cat) { tabName = cat.name } }
                }
                Spacer(Modifier.height(12.dp))

                val categories = if (tab == null) PaperCategory.entries else listOf(tab)
                val side = if (compact) 16.dp else 24.dp
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compact) 104.dp else 150.dp),
                    contentPadding = PaddingValues(start = side, end = side, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    for (cat in categories) {
                        if (tab == null) {
                            item(key = "header_${cat.name}", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    categoryName(cat),
                                    color = c.textPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        items(PaperCatalog.inCategory(cat), key = { it.id }) { spec ->
                            PaperTile(spec, paperSize, selected = mode == SheetMode.PICK && spec.id == quickTemplate) {
                                onPaper(spec.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickButton(icon: ImageVector, label: String, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val c = Palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).background(c.accent, CircleShape),
        ) {
            Icon(icon, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(28.dp))
        }
        Text(
            label,
            color = c.onTopBar,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Palette
    Text(
        label,
        color = if (selected) c.accent else c.textSecondary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) c.selected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun PaperTile(spec: PaperSpec, size: PaperSize, selected: Boolean, onClick: () -> Unit) {
    val c = Palette
    val (pw, ph) = PaperCatalog.pageSize(spec, size)
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) c.accent else Color(0xFFD6D0E0),
                    RoundedCornerShape(12.dp),
                ),
        ) {
            PaperPreview(spec.id, pw, ph, Modifier.fillMaxSize())
        }
        Text(
            paperName(spec.id),
            color = c.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
    }
}
