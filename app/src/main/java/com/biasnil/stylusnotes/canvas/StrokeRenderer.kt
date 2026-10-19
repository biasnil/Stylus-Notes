package com.biasnil.stylusnotes.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint
import com.biasnil.stylusnotes.data.StrokeTool

/** Draws strokes. Shared by the live canvas, thumbnails and export. */
object StrokeRenderer {

    fun newPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Highlighter ink is see-through. */
    fun highlighterColor(color: Int): Int = (color and 0x00FFFFFF) or 0x59000000

    /** Highlighter strokes go underneath pen strokes. */
    fun drawAll(canvas: Canvas, strokes: List<Stroke>, paint: Paint) {
        for (s in strokes) if (s.tool == StrokeTool.HIGHLIGHTER) draw(canvas, s, paint)
        for (s in strokes) if (s.tool == StrokeTool.PEN) draw(canvas, s, paint)
    }

    fun draw(canvas: Canvas, stroke: Stroke, paint: Paint) =
        draw(canvas, stroke.points, stroke.color, stroke.baseWidth, stroke.tool, paint)

    fun draw(canvas: Canvas, points: List<StrokePoint>, color: Int, baseWidth: Float, tool: StrokeTool, paint: Paint) {
        if (points.isEmpty()) return
        paint.style = Paint.Style.STROKE
        if (tool == StrokeTool.HIGHLIGHTER) {
            // One path with a fixed width, so overlapping segments don't darken.
            paint.color = highlighterColor(color)
            paint.strokeWidth = baseWidth
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
            if (points.size == 1) path.lineTo(points[0].x + 0.01f, points[0].y)
            canvas.drawPath(path, paint)
            return
        }
        paint.color = color
        if (points.size == 1) {
            val p = points[0]
            paint.style = Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, StrokeGeometry.widthFor(baseWidth, p.p) / 2f, paint)
            paint.style = Paint.Style.STROKE
            return
        }
        // One segment per sample pair, each with its own width. Round caps hide the joins.
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            paint.strokeWidth = StrokeGeometry.widthFor(baseWidth, (a.p + b.p) / 2f)
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        }
    }
}
