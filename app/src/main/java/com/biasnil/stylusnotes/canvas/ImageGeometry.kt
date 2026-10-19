package com.biasnil.stylusnotes.canvas

import com.biasnil.stylusnotes.data.PlacedImage
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

/**
 * Maths for pictures that can be moved, scaled and rotated. Rotation is in degrees,
 * clockwise on screen (Android's y axis points down), around the picture's centre.
 * Pure Kotlin so it runs in JVM tests.
 */
object ImageGeometry {

    fun centerX(img: PlacedImage) = img.x + img.w / 2
    fun centerY(img: PlacedImage) = img.y + img.h / 2

    /** Page point -> picture-local point (origin at the centre, unrotated). */
    fun toLocal(img: PlacedImage, x: Float, y: Float): FloatArray {
        val r = Math.toRadians(-img.rotation.toDouble())
        val dx = x - centerX(img)
        val dy = y - centerY(img)
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return floatArrayOf(dx * c - dy * s, dx * s + dy * c)
    }

    /** Picture-local point -> page point. */
    fun toPage(img: PlacedImage, lx: Float, ly: Float): FloatArray {
        val r = Math.toRadians(img.rotation.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return floatArrayOf(centerX(img) + lx * c - ly * s, centerY(img) + lx * s + ly * c)
    }

    fun contains(img: PlacedImage, x: Float, y: Float): Boolean {
        val l = toLocal(img, x, y)
        return abs(l[0]) <= img.w / 2 && abs(l[1]) <= img.h / 2
    }

    /** Bottom-right corner, the resize handle. */
    fun resizeHandle(img: PlacedImage): FloatArray = toPage(img, img.w / 2, img.h / 2)

    /** Above the top edge, the rotate handle. [gap] in page units. */
    fun rotateHandle(img: PlacedImage, gap: Float): FloatArray = toPage(img, 0f, -img.h / 2 - gap)

    /** Angle of a point around the picture's centre, in degrees. */
    fun angleTo(img: PlacedImage, x: Float, y: Float): Float =
        Math.toDegrees(atan2((y - centerY(img)).toDouble(), (x - centerX(img)).toDouble())).toFloat()

    fun distanceToCenter(img: PlacedImage, x: Float, y: Float): Float = hypot(x - centerX(img), y - centerY(img))

    /** Scale around the centre, keeping the shape. */
    fun scaled(img: PlacedImage, factor: Float, minSide: Float = 24f): PlacedImage {
        val f = maxOf(factor, minSide / minOf(img.w, img.h))
        val w = img.w * f
        val h = img.h * f
        return img.copy(x = centerX(img) - w / 2, y = centerY(img) - h / 2, w = w, h = h)
    }

    /** Keeps rotation in 0..360 and snaps to the nearest 45° when within [snap] degrees. */
    fun normalizeRotation(deg: Float, snap: Float = 4f): Float {
        var d = deg % 360f
        if (d < 0) d += 360f
        val nearest = round(d / 45f) * 45f
        if (abs(d - nearest) <= snap) d = nearest
        return if (d >= 360f) d - 360f else d
    }

    /**
     * PDF image placement in page coordinates (y down): maps the image's unit square
     * (v pointing up) onto the rotated picture. Returns [a, b, c, d, e, f].
     */
    fun pdfImageMatrix(img: PlacedImage): FloatArray {
        val r = Math.toRadians(img.rotation.toDouble())
        val cs = cos(r).toFloat()
        val sn = sin(r).toFloat()
        val cx = centerX(img)
        val cy = centerY(img)
        val w = img.w
        val h = img.h
        return floatArrayOf(
            cs * w,
            sn * w,
            sn * h,
            -cs * h,
            cx - 0.5f * cs * w - 0.5f * sn * h,
            cy - 0.5f * sn * w + 0.5f * cs * h,
        )
    }
}
