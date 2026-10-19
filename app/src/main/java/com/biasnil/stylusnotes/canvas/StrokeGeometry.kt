package com.biasnil.stylusnotes.canvas

import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint

/** Pure math used by the canvas and the exporter. No android.* so it's JVM-testable. */
object StrokeGeometry {

    /** Maps pressure to a width. Pens that report 0 (some do on the first sample) get mid width. */
    fun widthFor(baseWidth: Float, pressure: Float): Float {
        val p = if (pressure <= 0f) 0.5f else pressure.coerceAtMost(1f)
        return baseWidth * (0.3f + 1.4f * p)
    }

    /** Max width the stroke can reach, for hit-testing and bounds. */
    fun maxWidth(baseWidth: Float) = baseWidth * 1.7f

    /** True if a circle of radius r at (x, y) touches the stroke. */
    fun hits(stroke: Stroke, x: Float, y: Float, r: Float): Boolean {
        val pts = stroke.points
        if (pts.isEmpty()) return false
        val reach = r + maxWidth(stroke.baseWidth) / 2f
        val reach2 = reach * reach
        if (pts.size == 1) return dist2(pts[0].x, pts[0].y, x, y) <= reach2
        for (i in 1 until pts.size) {
            if (segmentDist2(pts[i - 1], pts[i], x, y) <= reach2) return true
        }
        return false
    }

    /** [left, top, right, bottom] covering all strokes including their width, or null if empty. */
    fun bounds(strokes: List<Stroke>): FloatArray? {
        var l = Float.MAX_VALUE
        var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE
        var b = -Float.MAX_VALUE
        var any = false
        for (s in strokes) {
            val pad = maxWidth(s.baseWidth) / 2f
            for (p in s.points) {
                any = true
                if (p.x - pad < l) l = p.x - pad
                if (p.y - pad < t) t = p.y - pad
                if (p.x + pad > r) r = p.x + pad
                if (p.y + pad > b) b = p.y + pad
            }
        }
        return if (any) floatArrayOf(l, t, r, b) else null
    }

    private fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return dx * dx + dy * dy
    }

    private fun segmentDist2(a: StrokePoint, b: StrokePoint, x: Float, y: Float): Float {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val len2 = vx * vx + vy * vy
        if (len2 == 0f) return dist2(a.x, a.y, x, y)
        val t = (((x - a.x) * vx + (y - a.y) * vy) / len2).coerceIn(0f, 1f)
        return dist2(a.x + t * vx, a.y + t * vy, x, y)
    }
}
