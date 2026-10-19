package com.biasnil.stylusnotes.paper

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Points per inch / per millimetre. Page units are points. */
internal const val IN = 72f
internal const val MM = 72f / 25.4f

internal const val COLLEGE = 20.25f // 9/32 in
internal const val WIDE = 24.75f // 11/32 in
internal const val NARROW = 18f // 1/4 in

internal object PaperColors {
    const val RULE = 0xFFA9C6EE.toInt()
    const val GRID = 0xFFCADAF2.toInt()
    const val GRID_BOLD = 0xFF8FB0E2.toInt()
    const val DOT = 0xFF94AFD8.toInt()
    const val LABEL = 0xFF8C8C96.toInt()
    const val FILL = 0xFFE3ECF7.toInt()
    const val LINE = 0xFF7F9CC9.toInt()
    const val INK = 0xFF333333.toInt()
    const val GREEN = 0xFFC4E3C4.toInt()
    const val GREEN_BOLD = 0xFF86C486.toInt()
    const val POLAR = 0xFFC2CCDB.toInt()
    const val POLAR_BOLD = 0xFF7D8BA3.toInt()
    const val RED = 0xFFD9434A.toInt()
    const val BLUE = 0xFF4A86E8.toInt()
    const val CREASE = 0x554A86E8
}

/**
 * Small drawing helper. Note that pen()/dashed()/brush() return a shared Paint,
 * so always use the result straight away.
 */
class PaperPainter internal constructor(
    val c: Canvas,
    val w: Float,
    val h: Float,
    /** Thinnest line allowed, in page units, so lines don't vanish when zoomed out. */
    private val minLine: Float,
) {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.SANS_SERIF }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun pen(color: Int, width: Float): Paint = strokePaint.apply {
        this.color = color
        strokeWidth = max(width, minLine)
        pathEffect = null
    }

    fun dashed(color: Int, width: Float, on: Float, off: Float): Paint =
        pen(color, width).apply { pathEffect = DashPathEffect(floatArrayOf(on, off), 0f) }

    fun brush(color: Int): Paint = fillPaint.apply { this.color = color }

    fun hLine(y: Float, p: Paint, x0: Float = 0f, x1: Float = w) = c.drawLine(x0, y, x1, y, p)

    fun vLine(x: Float, p: Paint, y0: Float = 0f, y1: Float = h) = c.drawLine(x, y0, x, y1, p)

    fun label(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int = PaperColors.LABEL,
        align: Paint.Align = Paint.Align.LEFT,
        bold: Boolean = false,
    ) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = align
        textPaint.isFakeBoldText = bold
        c.drawText(text, x, y, textPaint)
    }

    fun hasGlyph(s: String): Boolean = textPaint.hasGlyph(s)

    fun dots(points: FloatArray, count: Int, color: Int, size: Float) {
        if (count <= 0) return
        dotPaint.color = color
        dotPaint.strokeWidth = max(size, minLine * 1.5f)
        c.drawPoints(points, 0, count, dotPaint)
    }
}

internal fun snap(len: Float, step: Float) = floor(len / step) * step

object PaperRenderer {

    /**
     * Draws a paper background in page units onto [canvas] (already scaled by the caller).
     * Returns false if the id isn't known, in which case nothing is drawn (blank paper).
     */
    fun draw(canvas: Canvas, templateId: String, w: Float, h: Float, minLine: Float): Boolean {
        val p = PaperPainter(canvas, w, h, minLine)
        when (templateId) {
            "blank" -> Unit
            "college" -> p.ruled(COLLEGE)
            "wide" -> p.ruled(WIDE)
            "narrow" -> p.ruled(NARROW)
            "cornell_basic" -> p.cornell(ruled = false, styled = false)
            "cornell_basic_college" -> p.cornell(ruled = true, styled = false)
            "cornell_styled" -> p.cornell(ruled = false, styled = true)
            "cornell_styled_college" -> p.cornell(ruled = true, styled = true)

            "graph_4" -> p.graph(IN / 4, 0)
            "graph_4_bold" -> p.graph(IN / 4, 4)
            "graph_5" -> p.graph(IN / 5, 0)
            "graph_5_bold" -> p.graph(IN / 5, 5)
            "graph_1mm_bold" -> p.graph(MM, 10, thin = 0.3f)
            "graph_5mm" -> p.graph(5 * MM, 0)
            "graph_1cm" -> p.graph(10 * MM, 0)
            "cross_4" -> p.crossGrid(IN / 4)
            "cross_5" -> p.crossGrid(IN / 5)
            "dot_4" -> p.dotGrid(IN / 4)
            "dot_5" -> p.dotGrid(IN / 5)
            "iso_grid" -> p.isoGrid()
            "iso_dots" -> p.isoDots()

            "engineering" -> p.engineering()
            "polar" -> p.polarFull()
            "polar_deg" -> p.polarQuadrant(radians = false)
            "polar_rad" -> p.polarQuadrant(radians = true)
            "loglog" -> p.logLog()
            "semilogx_4" -> p.semiLog(logOnX = true, perInch = 4, bold = false)
            "semilogx_4_bold" -> p.semiLog(logOnX = true, perInch = 4, bold = true)
            "semilogx_5" -> p.semiLog(logOnX = true, perInch = 5, bold = false)
            "semilogx_5_bold" -> p.semiLog(logOnX = true, perInch = 5, bold = true)
            "semilogy_4" -> p.semiLog(logOnX = false, perInch = 4, bold = false)
            "semilogy_4_bold" -> p.semiLog(logOnX = false, perInch = 4, bold = true)
            "semilogy_5" -> p.semiLog(logOnX = false, perInch = 5, bold = false)
            "semilogy_5_bold" -> p.semiLog(logOnX = false, perInch = 5, bold = true)

            "staff", "double_staff", "grand_staff", "treble_staff", "bass_staff" -> p.music(templateId)

            else -> return p.sports(templateId) || p.planner(templateId)
        }
        return true
    }
}

// ---------------------------------------------------------------------- standard

internal fun PaperPainter.ruled(
    spacing: Float,
    top: Float = IN,
    bottom: Float = h - 0.4f * IN,
    x0: Float = 0f,
    x1: Float = w,
) {
    var i = 0
    while (true) {
        val y = top + i * spacing
        if (y > bottom) break
        hLine(y, pen(PaperColors.RULE, 0.6f), x0, x1)
        i++
    }
}

internal fun PaperPainter.cornell(ruled: Boolean, styled: Boolean) {
    val cue = 2.3f * IN
    val summary = h - 2f * IN
    val header = if (styled) 0.85f * IN else 0f
    if (ruled) ruled(COLLEGE, top = header + COLLEGE * 2)
    vLine(cue, pen(PaperColors.LINE, 0.9f), 0f, summary)
    hLine(summary, pen(PaperColors.LINE, 0.9f))
    if (styled) {
        hLine(header, pen(PaperColors.LINE, 0.9f), cue, w)
        label("KEY POINTS", 8f, 14f, 7f)
        label("NAME / DATE / SUBJECT", cue + 8f, 14f, 7f)
        label("NOTES", cue + 8f, header + 14f, 7f)
        label("SUMMARY", 8f, summary + 14f, 7f)
    }
}

// ---------------------------------------------------------------------- grid

internal fun PaperPainter.graph(
    spacing: Float,
    boldEvery: Int,
    color: Int = PaperColors.GRID,
    boldColor: Int = PaperColors.GRID_BOLD,
    x0: Float = 0f,
    y0: Float = 0f,
    x1: Float = w,
    y1: Float = h,
    thin: Float = 0.45f,
    thick: Float = 0.9f,
) {
    var i = 0
    while (true) {
        val x = x0 + i * spacing
        if (x > x1 + 0.01f) break
        val bold = boldEvery > 0 && i % boldEvery == 0
        vLine(x, if (bold) pen(boldColor, thick) else pen(color, thin), y0, y1)
        i++
    }
    i = 0
    while (true) {
        val y = y0 + i * spacing
        if (y > y1 + 0.01f) break
        val bold = boldEvery > 0 && i % boldEvery == 0
        hLine(y, if (bold) pen(boldColor, thick) else pen(color, thin), x0, x1)
        i++
    }
}

internal fun PaperPainter.crossGrid(spacing: Float) {
    val arm = spacing * 0.17f
    val p = pen(PaperColors.GRID_BOLD, 0.6f)
    var i = 1
    while (i * spacing < w) {
        val x = i * spacing
        var j = 1
        while (j * spacing < h) {
            val y = j * spacing
            c.drawLine(x - arm, y, x + arm, y, p)
            c.drawLine(x, y - arm, x, y + arm, p)
            j++
        }
        i++
    }
}

internal fun PaperPainter.dotGrid(
    spacing: Float,
    x0: Float = spacing,
    y0: Float = spacing,
    x1: Float = w - spacing / 2,
    y1: Float = h - spacing / 2,
) {
    if (x1 < x0 || y1 < y0) return
    val cols = floor((x1 - x0) / spacing).toInt() + 1
    val rows = floor((y1 - y0) / spacing).toInt() + 1
    val pts = FloatArray(cols * rows * 2)
    var k = 0
    for (i in 0 until cols) for (j in 0 until rows) {
        pts[k++] = x0 + i * spacing
        pts[k++] = y0 + j * spacing
    }
    dots(pts, k, PaperColors.DOT, 1.6f)
}

private const val ISO_SIDE = 0.3f * IN
private const val COS30 = 0.8660254f
private const val TAN30 = 0.57735026f

internal fun PaperPainter.isoGrid() {
    val a = ISO_SIDE
    val d = a * COS30
    var i = 0
    while (i * d <= w) {
        vLine(i * d, pen(PaperColors.GRID, 0.5f))
        i++
    }
    val rise = w * TAN30
    val start = -ceil(rise / a).toInt()
    var k = start
    while (k * a <= h + rise) {
        val y = k * a
        c.drawLine(0f, y, w, y + rise, pen(PaperColors.GRID, 0.5f))
        c.drawLine(0f, y, w, y - rise, pen(PaperColors.GRID, 0.5f))
        k++
    }
}

internal fun PaperPainter.isoDots() {
    val a = ISO_SIDE
    val d = a * COS30
    val cols = floor(w / d).toInt() + 1
    val rows = floor(h / a).toInt() + 2
    val pts = FloatArray(cols * rows * 2)
    var k = 0
    for (i in 0 until cols) {
        val x = i * d
        var y = if (i % 2 == 1) a / 2 else 0f
        while (y <= h && k + 1 < pts.size) {
            pts[k++] = x
            pts[k++] = y
            y += a
        }
    }
    dots(pts, k, PaperColors.DOT, 1.8f)
}

// ---------------------------------------------------------------------- math & engineering

internal fun PaperPainter.engineering() {
    val sp = IN / 5
    val l = 0.5f * IN
    val t = 1.0f * IN
    val gw = snap(w - 2 * l, sp)
    val gh = snap(h - t - 0.5f * IN, sp)
    hLine(0.7f * IN, pen(PaperColors.GREEN_BOLD, 0.8f), l, l + gw)
    vLine(l + gw * 0.7f, pen(PaperColors.GREEN_BOLD, 0.8f), 0.35f * IN, 0.7f * IN)
    graph(sp, 5, PaperColors.GREEN, PaperColors.GREEN_BOLD, l, t, l + gw, t + gh)
}

internal fun PaperPainter.polarFull() {
    val cx = w / 2
    val cy = h / 2
    val r = min(w, h) / 2 - 0.5f * IN
    val step = 0.25f * IN
    var k = 1
    while (k * step <= r + 0.01f) {
        c.drawCircle(cx, cy, k * step, if (k % 4 == 0) pen(PaperColors.POLAR_BOLD, 0.8f) else pen(PaperColors.POLAR, 0.5f))
        k++
    }
    for (deg in 0 until 360 step 15) {
        val rad = Math.toRadians(deg.toDouble())
        val bold = deg % 90 == 0
        c.drawLine(
            cx, cy, cx + r * cos(rad).toFloat(), cy - r * sin(rad).toFloat(),
            if (bold) pen(PaperColors.POLAR_BOLD, 0.8f) else pen(PaperColors.POLAR, 0.5f),
        )
    }
}

internal fun PaperPainter.polarQuadrant(radians: Boolean) {
    val ox = 0.6f * IN
    val oy = h - 0.6f * IN
    val r = min(w - ox - 0.7f * IN, oy - 0.7f * IN)
    val step = 0.25f * IN
    val rect = RectF()
    var k = 1
    while (k * step <= r + 0.01f) {
        val rr = k * step
        rect.set(ox - rr, oy - rr, ox + rr, oy + rr)
        c.drawArc(rect, 270f, 90f, false, if (k % 4 == 0) pen(PaperColors.POLAR_BOLD, 0.8f) else pen(PaperColors.POLAR, 0.5f))
        k++
    }
    for (deg in 0..90 step 5) {
        val rad = Math.toRadians(deg.toDouble())
        val major = deg % 15 == 0
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        c.drawLine(ox, oy, ox + r * cs, oy - r * sn, if (major) pen(PaperColors.POLAR_BOLD, 0.8f) else pen(PaperColors.POLAR, 0.45f))
        if (major) {
            val text = if (radians) {
                when (deg) {
                    0 -> "0"; 15 -> "π/12"; 30 -> "π/6"; 45 -> "π/4"
                    60 -> "π/3"; 75 -> "5π/12"; else -> "π/2"
                }
            } else "$deg°"
            val lr = r + 12f
            label(text, ox + lr * cs, oy - lr * sn + 3f, 8f, PaperColors.LABEL, Paint.Align.CENTER)
        }
    }
}

/** Positions of log lines across [length]: (position, isDecadeLine). */
private fun logPositions(start: Float, length: Float, cycles: Int): List<Pair<Float, Boolean>> {
    val out = ArrayList<Pair<Float, Boolean>>()
    for (cy in 0 until cycles) for (k in 1..9) {
        out += (start + length * (cy + log10(k.toFloat())) / cycles) to (k == 1)
    }
    out += (start + length) to true
    return out
}

private fun PaperPainter.logPen(major: Boolean, bold: Boolean): Paint = when {
    major && bold -> pen(PaperColors.GRID_BOLD, 0.9f)
    major -> pen(PaperColors.RULE, 0.6f)
    else -> pen(PaperColors.GRID, 0.45f)
}

internal fun PaperPainter.logLog() {
    val l = 0.5f * IN
    val t = 0.75f * IN
    val r = w - 0.5f * IN
    val b = h - 0.5f * IN
    for ((x, major) in logPositions(l, r - l, 3)) vLine(x, logPen(major, true), t, b)
    for ((yy, major) in logPositions(0f, b - t, 4)) hLine(b - yy, logPen(major, true), l, r)
}

internal fun PaperPainter.semiLog(logOnX: Boolean, perInch: Int, bold: Boolean) {
    val l = 0.5f * IN
    val t = 0.75f * IN
    val r = w - 0.5f * IN
    val b = h - 0.5f * IN
    val sp = IN / perInch
    if (logOnX) {
        for ((x, major) in logPositions(l, r - l, 3)) vLine(x, logPen(major, bold), t, b)
        val gh = snap(b - t, sp)
        var i = 0
        while (i * sp <= gh + 0.01f) {
            hLine(b - i * sp, logPen(i % perInch == 0, bold), l, r)
            i++
        }
    } else {
        for ((yy, major) in logPositions(0f, b - t, 4)) hLine(b - yy, logPen(major, bold), l, r)
        val gw = snap(r - l, sp)
        var i = 0
        while (i * sp <= gw + 0.01f) {
            vLine(l + i * sp, logPen(i % perInch == 0, bold), t, b)
            i++
        }
    }
}

// ---------------------------------------------------------------------- music

private const val GAP = 6.5f
private const val STAFF_H = GAP * 4
private const val TREBLE = "𝄞"
private const val BASS = "𝄢"

private fun PaperPainter.staff(x0: Float, x1: Float, top: Float) {
    for (i in 0..4) hLine(top + i * GAP, pen(PaperColors.INK, 0.6f), x0, x1)
}

/** Clef glyphs come from the system music font. If the device has none, they're skipped. */
private fun PaperPainter.clef(treble: Boolean, x: Float, top: Float) {
    val glyph = if (treble) TREBLE else BASS
    if (!hasGlyph(glyph)) return
    val baseline = if (treble) top + 3 * GAP else top + GAP
    label(glyph, x, baseline, STAFF_H, PaperColors.INK)
}

private fun PaperPainter.brace(x: Float, top: Float, bottom: Float) {
    val mid = (top + bottom) / 2
    val path = Path()
    path.moveTo(x, top)
    path.cubicTo(x - 9f, top + (mid - top) * 0.25f, x + 1f, mid - 8f, x - 7f, mid)
    path.cubicTo(x + 1f, mid + 8f, x - 9f, bottom - (bottom - mid) * 0.25f, x, bottom)
    c.drawPath(path, pen(PaperColors.INK, 1.4f))
}

internal fun PaperPainter.music(kind: String) {
    val x0 = 0.6f * IN
    val x1 = w - 0.5f * IN
    val limit = h - 0.5f * IN
    when (kind) {
        "staff" -> {
            var top = 0.8f * IN
            while (top + STAFF_H <= limit) {
                staff(x0, x1, top)
                top += 0.8f * IN
            }
        }
        "treble_staff", "bass_staff" -> {
            var top = 0.9f * IN
            while (top + STAFF_H <= limit) {
                staff(x0, x1, top)
                vLine(x0, pen(PaperColors.INK, 0.8f), top, top + STAFF_H)
                clef(kind == "treble_staff", x0 + 4f, top)
                top += 1.0f * IN
            }
        }
        else -> { // double_staff, grand_staff
            val inner = 0.45f * IN
            val pairH = STAFF_H * 2 + inner
            var top = 0.8f * IN
            while (top + pairH <= limit) {
                val lower = top + STAFF_H + inner
                staff(x0, x1, top)
                staff(x0, x1, lower)
                vLine(x0, pen(PaperColors.INK, 0.9f), top, top + pairH)
                vLine(x1, pen(PaperColors.INK, 0.6f), top, top + pairH)
                if (kind == "grand_staff") {
                    brace(x0 - 5f, top, top + pairH)
                    clef(true, x0 + 4f, top)
                    clef(false, x0 + 4f, lower)
                }
                top += pairH + 0.6f * IN
            }
        }
    }
}
