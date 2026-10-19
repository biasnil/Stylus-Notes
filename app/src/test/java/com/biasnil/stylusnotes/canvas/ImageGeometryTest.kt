package com.biasnil.stylusnotes.canvas

import com.biasnil.stylusnotes.data.PlacedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageGeometryTest {

    private val img = PlacedImage(id = 1, assetId = "a", x = 100f, y = 100f, w = 200f, h = 100f)
    private val eps = 0.01f

    @Test
    fun containsUnrotated() {
        assertTrue(ImageGeometry.contains(img, 150f, 120f))
        assertFalse(ImageGeometry.contains(img, 90f, 120f))
        assertFalse(ImageGeometry.contains(img, 150f, 210f))
    }

    @Test
    fun containsRotated90() {
        val r = img.copy(rotation = 90f)
        // Centre (200,150); rotated it is 100 wide and 200 tall.
        assertTrue(ImageGeometry.contains(r, 200f, 60f))
        assertFalse(ImageGeometry.contains(r, 110f, 150f))
    }

    @Test
    fun localPageRoundTrip() {
        val r = img.copy(rotation = 37f)
        val l = ImageGeometry.toLocal(r, 260f, 90f)
        val p = ImageGeometry.toPage(r, l[0], l[1])
        assertEquals(260f, p[0], eps)
        assertEquals(90f, p[1], eps)
    }

    @Test
    fun handlesFollowRotation() {
        val h = ImageGeometry.resizeHandle(img)
        assertEquals(300f, h[0], eps)
        assertEquals(200f, h[1], eps)
        val top = ImageGeometry.rotateHandle(img, 20f)
        assertEquals(200f, top[0], eps)
        assertEquals(80f, top[1], eps)
        // Rotated 90° clockwise, "above" now points right.
        val side = ImageGeometry.rotateHandle(img.copy(rotation = 90f), 20f)
        assertEquals(270f, side[0], eps)
        assertEquals(150f, side[1], eps)
    }

    @Test
    fun scaledKeepsCentreAndMinimum() {
        val s = ImageGeometry.scaled(img, 2f)
        assertEquals(400f, s.w, eps)
        assertEquals(200f, s.h, eps)
        assertEquals(200f, ImageGeometry.centerX(s), eps)
        assertEquals(150f, ImageGeometry.centerY(s), eps)
        val tiny = ImageGeometry.scaled(img, 0.01f, minSide = 24f)
        assertEquals(24f, tiny.h, eps)
    }

    @Test
    fun normalizeAndSnap() {
        assertEquals(270f, ImageGeometry.normalizeRotation(-90f), eps)
        assertEquals(45f, ImageGeometry.normalizeRotation(47f), eps)
        assertEquals(60f, ImageGeometry.normalizeRotation(60f), eps)
        assertEquals(0f, ImageGeometry.normalizeRotation(358f), eps)
        assertEquals(10f, ImageGeometry.normalizeRotation(370f), eps)
    }

    @Test
    fun pdfMatrixUnrotated() {
        // Unit square (0,0) maps to bottom-left, (1,1) to top-right in y-down page space.
        val m = ImageGeometry.pdfImageMatrix(img)
        assertEquals(200f, m[0], eps)
        assertEquals(0f, m[1], eps)
        assertEquals(0f, m[2], eps)
        assertEquals(-100f, m[3], eps)
        assertEquals(100f, m[4], eps)
        assertEquals(200f, m[5], eps)
    }

    @Test
    fun pdfMatrixCornersMatchRotatedPicture() {
        val r = img.copy(rotation = 30f)
        val m = ImageGeometry.pdfImageMatrix(r)
        fun map(u: Float, v: Float) = floatArrayOf(m[0] * u + m[2] * v + m[4], m[1] * u + m[3] * v + m[5])
        // u=0,v=1 is the image's top-left.
        val tl = map(0f, 1f)
        val want = ImageGeometry.toPage(r, -r.w / 2, -r.h / 2)
        assertEquals(want[0], tl[0], eps)
        assertEquals(want[1], tl[1], eps)
        val br = map(1f, 0f)
        val wantBr = ImageGeometry.toPage(r, r.w / 2, r.h / 2)
        assertEquals(wantBr[0], br[0], eps)
        assertEquals(wantBr[1], br[1], eps)
    }
}
