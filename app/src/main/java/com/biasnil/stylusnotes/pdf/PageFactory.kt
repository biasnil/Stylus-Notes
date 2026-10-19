package com.biasnil.stylusnotes.pdf

import android.content.Context
import android.net.Uri
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.data.LibraryRepository
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PdfRef
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.paper.PaperCatalog
import com.biasnil.stylusnotes.paper.PaperSize
import com.biasnil.stylusnotes.util.displayName
import kotlin.math.min

/** Builds pages from paper templates, PDFs and pictures. */
class PageFactory(
    private val context: Context,
    private val library: LibraryRepository,
    private val assets: AssetLoader,
) {

    data class PdfImport(val title: String, val pages: List<Page>)

    fun templatePage(templateId: String, size: PaperSize): Page {
        val spec = PaperCatalog.find(templateId)
        val (w, h) = PaperCatalog.pageSize(spec, size)
        return Page(template = spec.id, width = w, height = h)
    }

    /** Copies the PDF into the app and makes one page per PDF page. */
    suspend fun pdfPages(uri: Uri, keepText: Boolean): PdfImport {
        val name = displayName(context, uri) ?: "PDF"
        val assetId = library.importAsset(uri, "pdf")
        val sizes = try {
            assets.pdfPageSizes(library.assetFile(assetId))
        } catch (e: SecurityException) {
            library.deleteAsset(assetId)
            throw IllegalStateException(context.getString(R.string.msg_pdf_password))
        } catch (e: Exception) {
            library.deleteAsset(assetId)
            throw IllegalStateException(context.getString(R.string.msg_pdf_unreadable))
        }
        if (sizes.isEmpty()) {
            library.deleteAsset(assetId)
            throw IllegalStateException(context.getString(R.string.msg_pdf_no_pages))
        }
        val pages = sizes.mapIndexed { i, (w, h) ->
            Page(template = "blank", width = w, height = h, pdf = PdfRef(assetId, i, keepText))
        }
        return PdfImport(name.removeSuffix(".pdf").removeSuffix(".PDF"), pages)
    }

    /** One page per picture, the picture filling the page. */
    suspend fun imagePages(uris: List<Uri>): List<Page> = uris.mapNotNull { uri ->
        val id = importImage(uri) ?: return@mapNotNull null
        val b = assets.loadImage(id) ?: return@mapNotNull null
        val w = 612f
        val h = (w * b.height / b.width).coerceIn(200f, 2400f)
        Page(template = "blank", width = w, height = h, images = listOf(PlacedImage(System.nanoTime(), id, 0f, 0f, w, h)))
    }

    /** A picture placed in the middle of [page], at most 60% of its width. */
    suspend fun placedImage(uri: Uri, page: Page): PlacedImage? {
        val id = importImage(uri) ?: return null
        val b = assets.loadImage(id) ?: return null
        val k = min(page.width * 0.6f / b.width, page.height * 0.5f / b.height)
        val w = b.width * k
        val h = b.height * k
        return PlacedImage(System.nanoTime(), id, (page.width - w) / 2, (page.height - h) / 3, w, h)
    }

    private suspend fun importImage(uri: Uri): String? {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            "png" in mime -> "png"
            "webp" in mime -> "webp"
            "heic" in mime || "heif" in mime -> "heic"
            "gif" in mime -> "gif"
            else -> "jpg"
        }
        return runCatching { library.importAsset(uri, ext) }.getOrNull()
    }
}
