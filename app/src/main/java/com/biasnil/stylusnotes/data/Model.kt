package com.biasnil.stylusnotes.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** One sampled pen position, in page units (1 unit = 1/72 inch). Short names keep files small. */
@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    /** Pressure, 0..1 */
    val p: Float,
    /** Event time in ms (uptime clock) */
    val t: Long,
)

@Serializable
enum class StrokeTool { PEN, HIGHLIGHTER }

@Serializable
data class Stroke(
    val id: Long,
    /** ARGB color int */
    val color: Int,
    /** Width in page units at mid pressure (pen) or the fixed width (highlighter) */
    val baseWidth: Float,
    val points: List<StrokePoint>,
    val tool: StrokeTool = StrokeTool.PEN,
)

/** A page whose background is a page of an imported PDF. */
@Serializable
data class PdfRef(
    /** File name in the assets folder */
    val assetId: String,
    val pageIndex: Int,
    /** True: export keeps the original PDF page (text stays selectable). False: exported as an image. */
    val keepText: Boolean = false,
)

/** A picture placed on a page. Position and size in page units. */
@Serializable
data class PlacedImage(
    val id: Long,
    val assetId: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    /** Degrees, clockwise, around the centre. */
    val rotation: Float = 0f,
)

/** Where the page was scrolled and zoomed, so it can be restored. */
@Serializable
data class PageView(
    /** Zoom relative to fit-to-width. */
    val zoom: Float,
    /** Page point shown at the top-left of the screen. */
    val x: Float,
    val y: Float,
)

@Serializable
data class Page(
    val id: String = newId(),
    /** Paper background id from PaperCatalog. Ignored when [pdf] is set. */
    val template: String = "blank",
    val width: Float = 612f,
    val height: Float = 792f,
    val pdf: PdfRef? = null,
    val images: List<PlacedImage> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
    val view: PageView? = null,
) {
    val hasContent: Boolean get() = strokes.isNotEmpty() || images.isNotEmpty() || pdf != null
}

@Serializable
data class Note(
    val id: String,
    val title: String = "",
    val created: Long,
    val modified: Long,
    val pages: List<Page> = emptyList(),
    /** Only present in files saved by the first version (one canvas, no pages). */
    val strokes: List<Stroke> = emptyList(),
) {
    val assetIds: List<String>
        get() = pages.flatMap { p -> listOfNotNull(p.pdf?.assetId) + p.images.map { it.assetId } }.distinct()
}

fun newId(): String = UUID.randomUUID().toString()

object NoteMigration {
    /**
     * Version 1 notes had their strokes directly on the note, in screen pixels.
     * They become a single blank page big enough to hold everything.
     */
    fun migrate(note: Note): Note {
        if (note.pages.isNotEmpty()) {
            return if (note.strokes.isEmpty()) note else note.copy(strokes = emptyList())
        }
        var w = 612f
        var h = 792f
        if (note.strokes.isNotEmpty()) {
            val right = note.strokes.maxOf { s -> s.points.maxOfOrNull { it.x } ?: 0f } + 32f
            val bottom = note.strokes.maxOf { s -> s.points.maxOfOrNull { it.y } ?: 0f } + 32f
            w = maxOf(w, right)
            h = maxOf(w * 11f / 8.5f, bottom)
        }
        val page = Page(template = "blank", width = w, height = h, strokes = note.strokes)
        return note.copy(pages = listOf(page), strokes = emptyList())
    }
}
