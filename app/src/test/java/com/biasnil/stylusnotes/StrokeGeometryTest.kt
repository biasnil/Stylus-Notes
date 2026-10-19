package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.canvas.StrokeGeometry
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeGeometryTest {

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = Stroke(
        id = 1, color = 0, baseWidth = 4f,
        points = listOf(StrokePoint(x0, y0, 0.5f, 0), StrokePoint(x1, y1, 0.5f, 10)),
    )

    @Test fun morePressureMeansWiderLine() {
        assertTrue(StrokeGeometry.widthFor(4f, 1f) > StrokeGeometry.widthFor(4f, 0.2f))
    }

    @Test fun zeroPressureGetsMidWidth() {
        assertEquals(StrokeGeometry.widthFor(4f, 0.5f), StrokeGeometry.widthFor(4f, 0f), 0.0001f)
    }

    @Test fun pressureAboveOneIsClamped() {
        assertEquals(StrokeGeometry.widthFor(4f, 1f), StrokeGeometry.widthFor(4f, 3f), 0.0001f)
    }

    @Test fun eraserHitsMiddleOfSegment() {
        assertTrue(StrokeGeometry.hits(line(0f, 0f, 100f, 0f), 50f, 5f, 3f))
    }

    @Test fun eraserMissesFarPoint() {
        assertFalse(StrokeGeometry.hits(line(0f, 0f, 100f, 0f), 50f, 40f, 3f))
    }

    @Test fun eraserMissesBeyondEndOfSegment() {
        assertFalse(StrokeGeometry.hits(line(0f, 0f, 100f, 0f), 130f, 0f, 3f))
    }

    @Test fun singlePointStrokeCanBeErased() {
        val dot = Stroke(1, 0, 4f, listOf(StrokePoint(10f, 10f, 0.5f, 0)))
        assertTrue(StrokeGeometry.hits(dot, 12f, 12f, 2f))
    }

    @Test fun boundsOfEmptyIsNull() = assertNull(StrokeGeometry.bounds(emptyList()))

    @Test fun boundsIncludeStrokeWidth() {
        val b = StrokeGeometry.bounds(listOf(line(10f, 20f, 110f, 20f)))!!
        val pad = StrokeGeometry.maxWidth(4f) / 2f
        assertEquals(10f - pad, b[0], 0.001f)
        assertEquals(20f - pad, b[1], 0.001f)
        assertEquals(110f + pad, b[2], 0.001f)
        assertEquals(20f + pad, b[3], 0.001f)
    }
}
