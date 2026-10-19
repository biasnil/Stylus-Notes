package com.biasnil.stylusnotes.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.canvas.ImageGeometry
import com.biasnil.stylusnotes.canvas.PageRenderer
import com.biasnil.stylusnotes.canvas.StrokeGeometry
import com.biasnil.stylusnotes.canvas.StrokeRenderer
import com.biasnil.stylusnotes.data.Note
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokeTool
import com.biasnil.stylusnotes.pdf.AssetLoader
import com.biasnil.stylusnotes.pdf.PdfCoords
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Saves notes into Downloads/StylusNotes through MediaStore (no storage permission needed).
 *
 * PDF export:
 *  - Paper pages and flattened PDF pages are drawn with Android's PdfDocument, so ink and
 *    backgrounds stay vector.
 *  - PDF pages imported with "Keep text selectable" are copied from the original file with
 *    PdfBox and the ink is drawn on top, so the text stays selectable and searchable.
 */
object NoteExporter {

    suspend fun exportPdf(context: Context, note: Note, assets: AssetLoader): String = withContext(Dispatchers.IO) {
        if (note.pages.isEmpty()) throw IllegalStateException(context.getString(R.string.msg_export_failed))
        val keepText = note.pages.any { it.pdf?.keepText == true }
        saveToDownloads(context, fileName(note, "pdf"), "application/pdf") { out ->
            if (keepText) writeWithPdfBox(context, note, assets, out)
            else {
                val doc = androidPdf(note.pages, assets)
                try {
                    doc.writeTo(out)
                } finally {
                    doc.close()
                }
            }
        }
    }

    suspend fun exportPagePng(context: Context, note: Note, index: Int, assets: AssetLoader): String =
        withContext(Dispatchers.IO) {
            val page = note.pages.getOrNull(index) ?: throw IllegalStateException(context.getString(R.string.msg_export_failed))
            val scale = min(2.5f, 4000f / max(page.width, page.height))
            val bw = (page.width * scale).roundToInt().coerceAtLeast(1)
            val bh = (page.height * scale).roundToInt().coerceAtLeast(1)
            val pdf = page.pdf?.let { assets.renderPdf(it, bw) }
            val imgs = page.images.associate { it.assetId to assets.loadImage(it.assetId) }
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            try {
                val c = Canvas(bmp)
                c.scale(scale, scale)
                PageRenderer.drawPage(
                    c, page, page.images, page.strokes, 1f / scale, pdf, { imgs[it] }, -1L,
                    StrokeRenderer.newPaint(), Paint(Paint.FILTER_BITMAP_FLAG),
                )
                saveToDownloads(context, fileName(note, "png", "_p${index + 1}"), "image/png") { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } finally {
                bmp.recycle()
            }
        }

    // ---------------------------------------------------------------- Android PdfDocument

    private suspend fun androidPdf(pages: List<Page>, assets: AssetLoader): PdfDocument {
        val doc = PdfDocument()
        pages.forEachIndexed { i, page ->
            val w = page.width.roundToInt().coerceAtLeast(1)
            val h = page.height.roundToInt().coerceAtLeast(1)
            val pdf = page.pdf?.let { assets.renderPdf(it, (page.width * 2.5f).toInt()) }
            val imgs = page.images.associate { it.assetId to assets.loadImage(it.assetId) }
            val p = doc.startPage(PdfDocument.PageInfo.Builder(w, h, i + 1).create())
            PageRenderer.drawPage(
                p.canvas, page, page.images, page.strokes, 0.25f, pdf, { imgs[it] }, -1L,
                StrokeRenderer.newPaint(), Paint(Paint.FILTER_BITMAP_FLAG),
            )
            doc.finishPage(p)
        }
        return doc
    }

    // ---------------------------------------------------------------- PdfBox (selectable text)

    private suspend fun writeWithPdfBox(context: Context, note: Note, assets: AssetLoader, out: OutputStream) {
        val plain = note.pages.filter { it.pdf?.keepText != true }
        val temp = File.createTempFile("export", ".pdf", context.cacheDir)
        val sources = HashMap<String, PDDocument>()
        var tempDoc: PDDocument? = null
        try {
            if (plain.isNotEmpty()) {
                val d = androidPdf(plain, assets)
                try {
                    temp.outputStream().use { d.writeTo(it) }
                } finally {
                    d.close()
                }
                tempDoc = PDDocument.load(temp)
            }
            PDDocument().use { doc ->
                var k = 0
                for (page in note.pages) {
                    val ref = page.pdf
                    if (ref != null && ref.keepText) {
                        val src = sources.getOrPut(ref.assetId) { PDDocument.load(assets.file(ref.assetId)) }
                        val imported = doc.importPage(src.getPage(ref.pageIndex))
                        overlayInk(doc, imported, page, assets)
                    } else {
                        val td = tempDoc ?: throw IllegalStateException(context.getString(R.string.msg_export_failed))
                        doc.importPage(td.getPage(k++))
                    }
                }
                doc.save(out)
            }
        } finally {
            tempDoc?.close()
            sources.values.forEach { it.close() }
            temp.delete()
        }
    }

    private suspend fun overlayInk(doc: PDDocument, pdPage: PDPage, page: Page, assets: AssetLoader) {
        if (page.strokes.isEmpty() && page.images.isEmpty()) return
        val box = pdPage.cropBox
        val rot = pdPage.rotation
        val (dw, dh) = PdfCoords.displaySize(rot, box.width, box.height)
        val m = PdfCoords.displayToPdf(rot, box.lowerLeftX, box.lowerLeftY, box.width, box.height, dw / page.width, dh / page.height)
        val bitmaps = page.images.associate { it.assetId to assets.loadImage(it.assetId) }
        PDPageContentStream(doc, pdPage, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            // From here on everything is drawn in the app's page coordinates.
            cs.transform(Matrix(m[0], m[1], m[2], m[3], m[4], m[5]))
            for (img in page.images) {
                val b = bitmaps[img.assetId]
                if (b != null) {
                    val xobj = LosslessFactory.createFromImage(doc, b)
                    // Our y axis points down, so the image's unit square is flipped (and rotated).
                    val im = ImageGeometry.pdfImageMatrix(img)
                    cs.drawImage(xobj, Matrix(im[0], im[1], im[2], im[3], im[4], im[5]))
                }
            }
            cs.setLineCapStyle(1)
            cs.setLineJoinStyle(1)
            val highlight = PDExtendedGraphicsState().apply { strokingAlphaConstant = 0.35f }
            val ordered = page.strokes.sortedBy { if (it.tool == StrokeTool.HIGHLIGHTER) 0 else 1 }
            for (s in ordered) drawStroke(cs, s, highlight)
        }
    }

    private fun drawStroke(cs: PDPageContentStream, s: Stroke, highlight: PDExtendedGraphicsState) {
        val pts = s.points
        if (pts.isEmpty()) return
        cs.setStrokingColor(Color.red(s.color) / 255f, Color.green(s.color) / 255f, Color.blue(s.color) / 255f)
        if (s.tool == StrokeTool.HIGHLIGHTER) {
            cs.saveGraphicsState()
            cs.setGraphicsStateParameters(highlight)
            cs.setLineWidth(s.baseWidth)
            cs.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) cs.lineTo(pts[i].x, pts[i].y)
            if (pts.size == 1) cs.lineTo(pts[0].x + 0.01f, pts[0].y)
            cs.stroke()
            cs.restoreGraphicsState()
            return
        }
        if (pts.size == 1) {
            cs.setLineWidth(StrokeGeometry.widthFor(s.baseWidth, pts[0].p))
            cs.moveTo(pts[0].x, pts[0].y)
            cs.lineTo(pts[0].x + 0.01f, pts[0].y)
            cs.stroke()
            return
        }
        for (i in 1 until pts.size) {
            val a = pts[i - 1]
            val b = pts[i]
            cs.setLineWidth(StrokeGeometry.widthFor(s.baseWidth, (a.p + b.p) / 2f))
            cs.moveTo(a.x, a.y)
            cs.lineTo(b.x, b.y)
            cs.stroke()
        }
    }

    // ---------------------------------------------------------------- files

    private fun fileName(note: Note, ext: String, suffix: String = ""): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = note.title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "note" }
        return "$base$suffix" + "_$stamp.$ext"
    }

    /** Returns the display path of the saved file. */
    private suspend fun saveToDownloads(
        context: Context,
        name: String,
        mime: String,
        write: suspend (OutputStream) -> Unit,
    ): String {
        val folder = Environment.DIRECTORY_DOWNLOADS + "/StylusNotes"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException(context.getString(R.string.msg_export_failed))
        try {
            val out = resolver.openOutputStream(uri) ?: throw IllegalStateException(context.getString(R.string.msg_export_failed))
            out.use { write(it) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return "Download/StylusNotes/$name"
    }
}
