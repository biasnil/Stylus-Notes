package com.biasnil.stylusnotes.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.biasnil.stylusnotes.canvas.PageRenderer
import com.biasnil.stylusnotes.canvas.StrokeRenderer
import com.biasnil.stylusnotes.data.LibraryRepository
import com.biasnil.stylusnotes.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** First-page thumbnails for the library grid. */
class Thumbnails(private val library: LibraryRepository, private val assets: AssetLoader) {

    private val _version = MutableStateFlow(0)
    /** Bumps after every thumbnail write, so the grid reloads. */
    val version: StateFlow<Int> = _version.asStateFlow()

    /** [pageIndex]: which page to show (first page, or the one last viewed). */
    suspend fun update(note: Note, pageIndex: Int = 0) = withContext(Dispatchers.Default) {
        val page = note.pages.getOrNull(pageIndex) ?: note.pages.firstOrNull() ?: return@withContext
        val tw = 360
        val scale = tw / page.width
        val th = (page.height * scale).toInt().coerceIn(1, 720)
        val pdf = page.pdf?.let { assets.renderPdf(it, tw) }
        val imgs = page.images.associate { it.assetId to assets.loadImage(it.assetId) }
        val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        PageRenderer.drawPage(
            c, page, page.images, page.strokes, 1f / scale, pdf, { imgs[it] }, -1L,
            StrokeRenderer.newPaint(), Paint(Paint.FILTER_BITMAP_FLAG),
        )
        library.writeThumb(note.id, bmp)
        bmp.recycle()
        _version.update { it + 1 }
    }

    /** Makes thumbnails for notes that don't have one yet (e.g. after upgrading). */
    suspend fun ensureAll() {
        for (meta in library.state.value.notes) {
            if (library.thumbFile(meta.id).exists()) continue
            val note = library.load(meta.id) ?: continue
            runCatching { update(note) }
        }
    }
}
