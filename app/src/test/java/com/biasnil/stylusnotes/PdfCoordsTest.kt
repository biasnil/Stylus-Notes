package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.pdf.PdfCoords
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCoordsTest {

    // Crop box 600 wide x 800 tall, offset (10, 20).
    private val llx = 10f
    private val lly = 20f
    private val w = 600f
    private val h = 800f

    private fun map(rot: Int, x: Float, y: Float): Pair<Float, Float> {
        val (dw, dh) = PdfCoords.displaySize(rot, w, h)
        // App page is the same size as the displayed page here.
        return PdfCoords.apply(PdfCoords.displayToPdf(rot, llx, lly, w, h, dw / dw, dh / dh), x, y)
    }

    private fun assertPoint(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, 0.001f)
        assertEquals(expected.second, actual.second, 0.001f)
    }

    @Test fun displaySizeSwapsForQuarterTurns() {
        assertEquals(600f to 800f, PdfCoords.displaySize(0, w, h))
        assertEquals(800f to 600f, PdfCoords.displaySize(90, w, h))
        assertEquals(600f to 800f, PdfCoords.displaySize(180, w, h))
        assertEquals(800f to 600f, PdfCoords.displaySize(-90, w, h))
    }

    @Test fun noRotationFlipsY() {
        assertPoint(llx to lly + h, map(0, 0f, 0f)) // top-left
        assertPoint(llx + w to lly, map(0, w, h)) // bottom-right
    }

    @Test fun rotated90() {
        // Displayed 800 wide x 600 tall. Display top-left is the PDF's bottom-left.
        assertPoint(llx to lly, map(90, 0f, 0f))
        assertPoint(llx to lly + h, map(90, 800f, 0f))
        assertPoint(llx + w to lly, map(90, 0f, 600f))
    }

    @Test fun rotated180() {
        assertPoint(llx + w to lly, map(180, 0f, 0f))
        assertPoint(llx to lly + h, map(180, w, h))
    }

    @Test fun rotated270() {
        assertPoint(llx + w to lly + h, map(270, 0f, 0f))
        assertPoint(llx to lly, map(270, 800f, 600f))
    }

    @Test fun scaleIsApplied() {
        val m = PdfCoords.displayToPdf(0, 0f, 0f, 600f, 800f, 2f, 2f)
        assertPoint(200f to 600f, PdfCoords.apply(m, 100f, 100f))
    }
}
