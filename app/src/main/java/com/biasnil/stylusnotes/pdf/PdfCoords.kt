package com.biasnil.stylusnotes.pdf

/**
 * Maps the page as the app shows it (origin top-left, y down, already rotated by /Rotate)
 * to PDF user space (origin bottom-left of the crop box, y up, unrotated).
 * Pure math so it can be unit tested.
 */
object PdfCoords {

    fun normalize(rotation: Int) = ((rotation % 360) + 360) % 360

    /** Size of the page as displayed, given its unrotated crop box size. */
    fun displaySize(rotation: Int, w: Float, h: Float): Pair<Float, Float> =
        when (normalize(rotation)) {
            90, 270 -> h to w
            else -> w to h
        }

    /**
     * Affine matrix [a, b, c, d, e, f] with u = a*x + c*y + e and v = b*x + d*y + f.
     * [sx]/[sy] scale app page units to displayed points (normally 1).
     */
    fun displayToPdf(rotation: Int, llx: Float, lly: Float, w: Float, h: Float, sx: Float, sy: Float): FloatArray =
        when (normalize(rotation)) {
            90 -> floatArrayOf(0f, sx, sy, 0f, llx, lly)
            180 -> floatArrayOf(-sx, 0f, 0f, sy, llx + w, lly)
            270 -> floatArrayOf(0f, -sx, -sy, 0f, llx + w, lly + h)
            else -> floatArrayOf(sx, 0f, 0f, -sy, llx, lly + h)
        }

    fun apply(m: FloatArray, x: Float, y: Float): Pair<Float, Float> =
        (m[0] * x + m[2] * y + m[4]) to (m[1] * x + m[3] * y + m[5])
}
