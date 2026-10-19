package com.biasnil.stylusnotes.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.paper.PaperRenderer

/** Draws a whole page in page units. The caller has already translated and scaled the canvas. */
object PageRenderer {

    private val white = Paint().apply { color = Color.WHITE }

    fun drawPage(
        c: Canvas,
        page: Page,
        images: List<PlacedImage>,
        strokes: List<Stroke>,
        /** Thinnest background line, in page units. */
        minLine: Float,
        pdf: Bitmap?,
        image: (String) -> Bitmap?,
        skipImageId: Long,
        inkPaint: Paint,
        bitmapPaint: Paint,
    ) {
        c.drawRect(0f, 0f, page.width, page.height, white)
        if (page.pdf != null) {
            if (pdf != null) c.drawBitmap(pdf, null, RectF(0f, 0f, page.width, page.height), bitmapPaint)
        } else {
            PaperRenderer.draw(c, page.template, page.width, page.height, minLine)
        }
        val r = RectF()
        for (img in images) {
            if (img.id == skipImageId) continue
            val b = image(img.assetId) ?: continue
            r.set(img.x, img.y, img.x + img.w, img.y + img.h)
            if (img.rotation != 0f) {
                c.save()
                c.rotate(img.rotation, img.x + img.w / 2, img.y + img.h / 2)
                c.drawBitmap(b, null, r, bitmapPaint)
                c.restore()
            } else {
                c.drawBitmap(b, null, r, bitmapPaint)
            }
        }
        StrokeRenderer.drawAll(c, strokes, inkPaint)
    }
}
