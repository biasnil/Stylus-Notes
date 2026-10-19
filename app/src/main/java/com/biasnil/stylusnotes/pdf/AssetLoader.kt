package com.biasnil.stylusnotes.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.biasnil.stylusnotes.canvas.PageCanvasView
import com.biasnil.stylusnotes.data.LibraryRepository
import com.biasnil.stylusnotes.data.PdfRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * Decodes images and renders PDF pages, with memory caches.
 *
 * The non-suspend [image] and [pdf] are for the canvas: they return what's cached
 * (or null) right away and start loading in the background, then tell listeners.
 * PdfRenderer isn't thread-safe, so all PDF work runs on one thread.
 */
class AssetLoader(private val library: LibraryRepository) : PageCanvasView.Assets {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pdfThread = Executors.newSingleThreadExecutor { r -> Thread(r, "pdf-render") }.asCoroutineDispatcher()

    private val images = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val pdfPages = object : LruCache<String, Bitmap>(96 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    /** Last rendered size for each PDF page, shown while a sharper one renders. */
    private val latestPdf = ConcurrentHashMap<String, String>()
    /** Main thread only. */
    private val pending = HashSet<String>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** pdfThread only. Keeps a few documents open. */
    private val renderers = object : LinkedHashMap<String, Pair<ParcelFileDescriptor, PdfRenderer>>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<ParcelFileDescriptor, PdfRenderer>>): Boolean {
            if (size <= 3) return false
            eldest.value.second.close()
            eldest.value.first.close()
            return true
        }
    }

    fun addListener(l: () -> Unit) {
        listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    private fun notifyLoaded() = listeners.forEach { it() }

    fun file(assetId: String): File = library.assetFile(assetId)

    // ---------------------------------------------------------------- canvas (main thread)

    override fun image(assetId: String): Bitmap? {
        images.get(assetId)?.let { return it }
        val key = "img:$assetId"
        if (pending.add(key)) scope.launch {
            loadImage(assetId)
            pending.remove(key)
            notifyLoaded()
        }
        return null
    }

    override fun pdf(ref: PdfRef, widthPx: Int): Bitmap? {
        val w = bucket(widthPx)
        val key = cacheKey(ref, w)
        pdfPages.get(key)?.let { return it }
        if (pending.add(key)) scope.launch {
            renderPdf(ref, w)
            pending.remove(key)
            notifyLoaded()
        }
        return latestPdf[pageKey(ref)]?.let { pdfPages.get(it) }
    }

    // ---------------------------------------------------------------- suspend (any thread)

    suspend fun loadImage(assetId: String): Bitmap? {
        images.get(assetId)?.let { return it }
        val b = withContext(Dispatchers.IO) { decodeImage(library.assetFile(assetId), 2048) }
        if (b != null) images.put(assetId, b)
        return b
    }

    suspend fun renderPdf(ref: PdfRef, widthPx: Int): Bitmap? {
        val w = bucket(widthPx)
        val key = cacheKey(ref, w)
        pdfPages.get(key)?.let { return it }
        val b = withContext(pdfThread) { runCatching { renderOnThread(ref, w) }.getOrNull() }
        if (b != null) {
            pdfPages.put(key, b)
            latestPdf[pageKey(ref)] = key
        }
        return b
    }

    /** Page sizes in points. Throws SecurityException for password-protected files. */
    suspend fun pdfPageSizes(file: File): List<Pair<Float, Float>> = withContext(pdfThread) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                (0 until r.pageCount).map { i -> r.openPage(i).use { p -> p.width.toFloat() to p.height.toFloat() } }
            }
        }
    }

    // ---------------------------------------------------------------- internals

    private fun renderOnThread(ref: PdfRef, w: Int): Bitmap? {
        val renderer = renderers.getOrPut(ref.assetId) {
            val pfd = ParcelFileDescriptor.open(library.assetFile(ref.assetId), ParcelFileDescriptor.MODE_READ_ONLY)
            pfd to PdfRenderer(pfd)
        }.second
        if (ref.pageIndex !in 0 until renderer.pageCount) return null
        return renderer.openPage(ref.pageIndex).use { page ->
            val h = (w.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }

    private fun decodeImage(file: File, maxSide: Int): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            // Software bitmaps, because the canvas cache is a software canvas.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val m = max(w, h)
            if (m > maxSide) {
                val k = maxSide.toFloat() / m
                decoder.setTargetSize((w * k).toInt().coerceAtLeast(1), (h * k).toInt().coerceAtLeast(1))
            }
        }
    }.getOrNull()

    private fun bucket(px: Int) = ((px + 511) / 512 * 512).coerceIn(512, 4096)
    private fun pageKey(ref: PdfRef) = "${ref.assetId}:${ref.pageIndex}"
    private fun cacheKey(ref: PdfRef, w: Int) = "${pageKey(ref)}:$w"
}
