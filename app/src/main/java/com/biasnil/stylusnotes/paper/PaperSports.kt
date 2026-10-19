package com.biasnil.stylusnotes.paper

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sqrt

/** Maps real-world field units (feet, yards or metres) onto the page. */
internal class Field(val l: Float, val t: Float, val k: Float) {
    fun x(u: Float) = l + u * k
    fun y(v: Float) = t + v * k
    fun d(u: Float) = u * k
    fun rect(u0: Float, v0: Float, u1: Float, v1: Float) = RectF(x(u0), y(v0), x(u1), y(v1))
    fun circle(cu: Float, cv: Float, r: Float) = RectF(x(cu - r), y(cv - r), x(cu + r), y(cv + r))
}

private fun PaperPainter.fit(len: Float, wid: Float, margin: Float = 0.5f * IN): Field {
    val k = min((w - 2 * margin) / len, (h - 2 * margin) / wid)
    return Field((w - len * k) / 2, (h - wid * k) / 2, k)
}

private fun PaperPainter.line(width: Float = 1.1f): Paint = pen(PaperColors.INK, width)

private fun PaperPainter.ln(f: Field, u0: Float, v0: Float, u1: Float, v1: Float, p: Paint) =
    c.drawLine(f.x(u0), f.y(v0), f.x(u1), f.y(v1), p)

private fun PaperPainter.box(f: Field, u0: Float, v0: Float, u1: Float, v1: Float, p: Paint) =
    c.drawRect(f.rect(u0, v0, u1, v1), p)

private fun PaperPainter.circ(f: Field, cu: Float, cv: Float, r: Float, p: Paint) =
    c.drawCircle(f.x(cu), f.y(cv), f.d(r), p)

private fun PaperPainter.arc(f: Field, cu: Float, cv: Float, r: Float, start: Float, sweep: Float, p: Paint) =
    c.drawArc(f.circle(cu, cv, r), start, sweep, false, p)

private fun PaperPainter.spot(f: Field, cu: Float, cv: Float, color: Int = PaperColors.INK) =
    c.drawCircle(f.x(cu), f.y(cv), 1.6f, brush(color))

/** Draws [block] once, then again mirrored left-to-right around the field's centre. */
private inline fun PaperPainter.mirrored(f: Field, len: Float, block: () -> Unit) {
    block()
    c.save()
    c.scale(-1f, 1f, f.x(len / 2), 0f)
    block()
    c.restore()
}

private fun PaperPainter.fieldDash(f: Field) = dashed(PaperColors.INK, 0.9f, f.d(1.2f), f.d(1.2f))

internal fun PaperPainter.sports(id: String): Boolean {
    when (id) {
        "basketball_hs" -> basketball()
        "basketball_half_hs" -> basketballHalf()
        "football" -> football()
        "football_red_zone" -> footballRedZone()
        "baseball" -> baseball()
        "baseball_score" -> baseballScore()
        "cricket" -> cricket()
        "soccer" -> soccer()
        "soccer_half" -> soccerHalf()
        "rugby_union" -> rugbyUnion()
        "rugby_league" -> rugbyLeague()
        "field_hockey" -> fieldHockey()
        "hockey_intl" -> hockey(60f, 30f, 8.5f, 4f, 22.86f, 4.5f, 10f, 7f, 24.36f, 1.8f, 0.915f, 1.1f)
        "hockey_usa" -> hockey(200f, 85f, 28f, 11f, 75f, 15f, 31f, 22f, 70f, 6f, 3f, 3.33f)
        "tennis" -> tennis()
        "volleyball" -> volleyball()
        "volleyball_half" -> volleyballHalf()
        "pickleball" -> pickleball()
        else -> return false
    }
    return true
}

// High-school basketball, feet. 84 x 50.
private fun PaperPainter.basketball() {
    val f = fit(84f, 50f)
    box(f, 0f, 0f, 84f, 50f, line(1.3f))
    ln(f, 42f, 0f, 42f, 50f, line())
    circ(f, 42f, 25f, 6f, line())
    mirrored(f, 84f) {
        box(f, 0f, 19f, 19f, 31f, line())
        circ(f, 19f, 25f, 6f, line())
        arc(f, 5.25f, 25f, 19.75f, -105.4f, 210.8f, line())
        ln(f, 4f, 22f, 4f, 28f, line(1.8f))
        circ(f, 5.25f, 25f, 0.75f, line())
    }
}

// Half court, baseline at the top. 50 wide x 47 deep.
private fun PaperPainter.basketballHalf() {
    val f = fit(50f, 47f)
    box(f, 0f, 0f, 50f, 47f, line(1.3f))
    box(f, 19f, 0f, 31f, 19f, line())
    circ(f, 25f, 19f, 6f, line())
    arc(f, 25f, 5.25f, 19.75f, -15.4f, 210.8f, line())
    ln(f, 22f, 4f, 28f, 4f, line(1.8f))
    circ(f, 25f, 5.25f, 0.75f, line())
    arc(f, 25f, 47f, 6f, 180f, 180f, line())
}

private fun PaperPainter.footballHashes(f: Field, from: Int, to: Int, width: Float) {
    for (yd in from..to) {
        if (yd % 5 == 0) continue
        val x = yd.toFloat()
        ln(f, x, 0f, x, 0.7f, line(0.6f))
        ln(f, x, width - 0.7f, x, width, line(0.6f))
        ln(f, x, 23.25f, x, 23.92f, line(0.6f))
        ln(f, x, width - 23.92f, x, width - 23.25f, line(0.6f))
    }
}

// American football, yards. 120 x 53.33 including end zones.
private fun PaperPainter.football() {
    val wid = 53.33f
    val f = fit(120f, wid)
    box(f, 0f, 0f, 120f, wid, line(1.3f))
    ln(f, 10f, 0f, 10f, wid, line(1.6f))
    ln(f, 110f, 0f, 110f, wid, line(1.6f))
    for (yd in 15..105 step 5) ln(f, yd.toFloat(), 0f, yd.toFloat(), wid, line())
    footballHashes(f, 11, 109, wid)
    for (k in 1..9) {
        val n = if (k <= 5) k * 10 else (10 - k) * 10
        val x = f.x(10f + k * 10f)
        label("$n", x, f.y(11f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
        label("$n", x, f.y(wid - 9f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
    }
}

// Goal-line end of the field: end zone plus 22 yards.
private fun PaperPainter.footballRedZone() {
    val wid = 53.33f
    val f = fit(32f, wid)
    ln(f, 0f, 0f, 0f, wid, line(1.3f))
    ln(f, 0f, 0f, 32f, 0f, line(1.3f))
    ln(f, 0f, wid, 32f, wid, line(1.3f))
    ln(f, 10f, 0f, 10f, wid, line(1.6f))
    for (yd in 15..30 step 5) ln(f, yd.toFloat(), 0f, yd.toFloat(), wid, line())
    footballHashes(f, 11, 31, wid)
    label("10", f.x(20f), f.y(11f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
    label("20", f.x(30f), f.y(11f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
    label("10", f.x(20f), f.y(wid - 9f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
    label("20", f.x(30f), f.y(wid - 9f), f.d(2.2f), PaperColors.INK, Paint.Align.CENTER)
}

// Baseball, feet. Home plate at the bottom.
private fun PaperPainter.baseball() {
    val f = fit(480f, 430f)
    val hx = 240f
    val hy = 415f
    val pole = 233.35f // 330 ft foul line at 45 degrees
    val base = 63.64f // 90 ft at 45 degrees
    ln(f, hx, hy, hx - pole, hy - pole, line())
    ln(f, hx, hy, hx + pole, hy - pole, line())
    val y0 = hy - pole
    val apex = hy - 400f
    val fence = Path().apply {
        moveTo(f.x(hx - pole), f.y(y0))
        quadTo(f.x(hx), f.y(2 * apex - y0), f.x(hx + pole), f.y(y0))
    }
    c.drawPath(fence, line())
    val diamond = Path().apply {
        moveTo(f.x(hx), f.y(hy))
        lineTo(f.x(hx + base), f.y(hy - base))
        lineTo(f.x(hx), f.y(hy - 2 * base))
        lineTo(f.x(hx - base), f.y(hy - base))
        close()
    }
    c.drawPath(diamond, line())
    val mound = hy - 60.5f
    arc(f, hx, mound, 95f, 200f, 140f, line())
    circ(f, hx, mound, 9f, line())
    circ(f, hx, hy, 13f, line())
    spot(f, hx + base, hy - base)
    spot(f, hx, hy - 2 * base)
    spot(f, hx - base, hy - base)
    spot(f, hx, hy)
}

private fun PaperPainter.baseballScore() {
    val l = 0.4f * IN
    val t = 0.5f * IN
    val r = w - 0.4f * IN
    val b = h - 0.4f * IN
    val fixed = listOf(0.35f * IN, 1.9f * IN, 0.45f * IN)
    val totals = List(4) { 0.42f * IN }
    val innW = (r - l - fixed.sum() - totals.sum()) / 9
    val widths = fixed + List(9) { innW } + totals
    val names = listOf("#", "PLAYER", "POS") + (1..9).map { "$it" } + listOf("AB", "R", "H", "RBI")
    val header = 0.3f * IN
    val rows = 12
    val rowH = (b - t - header) / rows
    for (j in 0 until rows) {
        if (j % 2 == 0) c.drawRect(l, t + header + j * rowH, l + fixed.sum(), t + header + (j + 1) * rowH, brush(PaperColors.FILL))
    }
    c.drawRect(l, t, r, b, pen(PaperColors.LINE, 1f))
    for (j in 0..rows) hLine(t + header + j * rowH, pen(PaperColors.LINE, 0.6f), l, r)
    var x = l
    widths.forEachIndexed { i, cw ->
        label(names[i], x + cw / 2, t + header * 0.68f, 7.5f, PaperColors.LABEL, Paint.Align.CENTER, bold = true)
        if (i in 3 until 12) {
            val s = min(cw, rowH) * 0.28f
            for (j in 0 until rows) {
                val cx = x + cw / 2
                val cy = t + header + (j + 0.5f) * rowH
                val d = Path().apply {
                    moveTo(cx, cy - s); lineTo(cx + s, cy); lineTo(cx, cy + s); lineTo(cx - s, cy); close()
                }
                c.drawPath(d, pen(PaperColors.GRID_BOLD, 0.6f))
            }
        }
        x += cw
        if (i < widths.lastIndex) vLine(x, pen(PaperColors.LINE, 0.6f), t, b)
    }
}

// Cricket, yards.
private fun PaperPainter.cricket() {
    val f = fit(150f, 130f)
    c.drawOval(f.rect(0f, 0f, 150f, 130f), line(1.3f))
    c.drawOval(f.rect(45f, 24f, 105f, 106f), line())
    box(f, 73.33f, 54f, 76.67f, 76f, line())
    ln(f, 72f, 55.22f, 78f, 55.22f, line(0.8f))
    ln(f, 72f, 74.78f, 78f, 74.78f, line(0.8f))
}

// Soccer, metres. 105 x 68.
private fun PaperPainter.soccer() {
    val f = fit(105f, 68f)
    box(f, 0f, 0f, 105f, 68f, line(1.3f))
    ln(f, 52.5f, 0f, 52.5f, 68f, line())
    circ(f, 52.5f, 34f, 9.15f, line())
    spot(f, 52.5f, 34f)
    mirrored(f, 105f) {
        box(f, 0f, 13.84f, 16.5f, 54.16f, line())
        box(f, 0f, 24.84f, 5.5f, 43.16f, line())
        spot(f, 11f, 34f)
        arc(f, 11f, 34f, 9.15f, -53.13f, 106.26f, line())
        box(f, -1.5f, 30.34f, 0f, 37.66f, line())
        arc(f, 0f, 0f, 1f, 0f, 90f, line(0.8f))
        arc(f, 0f, 68f, 1f, 270f, 90f, line(0.8f))
    }
}

// One half, goal at the bottom. 68 wide x 52.5 deep.
private fun PaperPainter.soccerHalf() {
    val f = fit(68f, 54.5f)
    box(f, 0f, 0f, 68f, 52.5f, line(1.3f))
    arc(f, 34f, 0f, 9.15f, 0f, 180f, line())
    spot(f, 34f, 0f)
    box(f, 13.84f, 36f, 54.16f, 52.5f, line())
    box(f, 24.84f, 47f, 43.16f, 52.5f, line())
    spot(f, 34f, 41.5f)
    arc(f, 34f, 41.5f, 9.15f, 216.87f, 106.26f, line())
    box(f, 30.34f, 52.5f, 37.66f, 54.5f, line())
    arc(f, 0f, 52.5f, 1f, 270f, 90f, line(0.8f))
    arc(f, 68f, 52.5f, 1f, 180f, 90f, line(0.8f))
}

// Rugby union, metres. 100 between try lines + 10 in-goal each end, 70 wide.
private fun PaperPainter.rugbyUnion() {
    val f = fit(120f, 70f)
    box(f, 0f, 0f, 120f, 70f, line(1.3f))
    for (x in listOf(10f, 110f)) ln(f, x, 0f, x, 70f, line(1.6f))
    for (x in listOf(32f, 88f, 60f)) ln(f, x, 0f, x, 70f, line())
    for (x in listOf(50f, 70f, 15f, 105f)) ln(f, x, 0f, x, 70f, fieldDash(f))
    for (y in listOf(5f, 15f, 55f, 65f)) ln(f, 10f, y, 110f, y, fieldDash(f))
}

// Rugby league, metres. 100 between try lines + 8 in-goal each end, 68 wide.
private fun PaperPainter.rugbyLeague() {
    val f = fit(116f, 68f)
    box(f, 0f, 0f, 116f, 68f, line(1.3f))
    ln(f, 8f, 0f, 8f, 68f, line(1.6f))
    ln(f, 108f, 0f, 108f, 68f, line(1.6f))
    for (k in 1..9) {
        val x = 8f + k * 10f
        val color = if (k == 4 || k == 6) PaperColors.RED else PaperColors.INK
        ln(f, x, 0f, x, 68f, pen(color, 1.1f))
        val n = if (k <= 5) k * 10 else (10 - k) * 10
        label("$n", f.x(x), f.y(9f), f.d(3.2f), color, Paint.Align.CENTER)
        label("$n", f.x(x), f.y(62f), f.d(3.2f), color, Paint.Align.CENTER)
    }
}

// Field hockey, metres. 91.4 x 55.
private fun PaperPainter.fieldHockey() {
    val f = fit(91.4f, 55f)
    box(f, 0f, 0f, 91.4f, 55f, line(1.3f))
    for (x in listOf(22.9f, 45.7f, 68.5f)) ln(f, x, 0f, x, 55f, line())
    mirrored(f, 91.4f) {
        val p1 = 25.67f
        val p2 = 29.33f
        arc(f, 0f, p1, 14.63f, 270f, 90f, line())
        arc(f, 0f, p2, 14.63f, 0f, 90f, line())
        ln(f, 14.63f, p1, 14.63f, p2, line())
        arc(f, 0f, p1, 19.63f, 270f, 90f, fieldDash(f))
        arc(f, 0f, p2, 19.63f, 0f, 90f, fieldDash(f))
        ln(f, 19.63f, p1, 19.63f, p2, fieldDash(f))
        spot(f, 6.4f, 27.5f)
        box(f, -1.2f, p1, 0f, p2, line())
    }
}

private fun cornerInset(x: Float, r: Float): Float =
    if (x >= r) 0f else r - sqrt(r * r - (r - x) * (r - x))

private fun PaperPainter.hockey(
    len: Float, wid: Float, corner: Float, goal: Float, blue: Float,
    circleR: Float, faceX: Float, faceY: Float, neutralX: Float,
    crease: Float, netHalf: Float, netDepth: Float,
) {
    val f = fit(len, wid)
    c.drawRoundRect(f.rect(0f, 0f, len, wid), f.d(corner), f.d(corner), line(1.3f))
    val cy = wid / 2
    ln(f, len / 2, 0f, len / 2, wid, pen(PaperColors.RED, 1.8f))
    circ(f, len / 2, cy, circleR, pen(PaperColors.BLUE, 1f))
    spot(f, len / 2, cy, PaperColors.BLUE)
    mirrored(f, len) {
        val inset = cornerInset(goal, corner)
        ln(f, goal, inset, goal, wid - inset, pen(PaperColors.RED, 1f))
        ln(f, blue, 0f, blue, wid, pen(PaperColors.BLUE, 2.4f))
        for (s in listOf(-1f, 1f)) {
            val fy = cy + s * faceY
            circ(f, faceX, fy, circleR, pen(PaperColors.RED, 1f))
            spot(f, faceX, fy, PaperColors.RED)
            spot(f, neutralX, fy, PaperColors.RED)
        }
        c.drawArc(f.circle(goal, cy, crease), 270f, 180f, true, brush(PaperColors.CREASE))
        arc(f, goal, cy, crease, 270f, 180f, pen(PaperColors.RED, 0.8f))
        box(f, goal - netDepth, cy - netHalf, goal, cy + netHalf, line(0.8f))
    }
}

// Tennis, feet. 78 x 36 doubles.
private fun PaperPainter.tennis() {
    val f = fit(78f, 36f)
    box(f, 0f, 0f, 78f, 36f, line(1.3f))
    ln(f, 0f, 4.5f, 78f, 4.5f, line())
    ln(f, 0f, 31.5f, 78f, 31.5f, line())
    ln(f, 39f, -1f, 39f, 37f, line(2f))
    ln(f, 18f, 4.5f, 18f, 31.5f, line())
    ln(f, 60f, 4.5f, 60f, 31.5f, line())
    ln(f, 18f, 18f, 60f, 18f, line())
    ln(f, 0f, 18f, 0.6f, 18f, line())
    ln(f, 77.4f, 18f, 78f, 18f, line())
}

// Volleyball, metres. 18 x 9 with the attack lines extended outside.
private fun PaperPainter.volleyball() {
    val f = fit(20f, 12f)
    val ox = 1f
    val oy = 1.5f
    box(f, ox, oy, ox + 18f, oy + 9f, line(1.3f))
    ln(f, ox + 9f, 0.3f, ox + 9f, 11.7f, line(2f))
    for (x in listOf(6f, 12f)) {
        ln(f, ox + x, oy, ox + x, oy + 9f, line())
        ln(f, ox + x, 0.2f, ox + x, oy, fieldDash(f))
        ln(f, ox + x, oy + 9f, ox + x, 11.8f, fieldDash(f))
    }
}

// One side, net at the top. 9 x 9.
private fun PaperPainter.volleyballHalf() {
    val f = fit(11f, 10.5f)
    box(f, 1f, 1f, 10f, 10f, line(1.3f))
    ln(f, 0.2f, 1f, 10.8f, 1f, line(2f))
    ln(f, 1f, 4f, 10f, 4f, line())
    ln(f, 0f, 4f, 1f, 4f, fieldDash(f))
    ln(f, 10f, 4f, 11f, 4f, fieldDash(f))
}

// Pickleball, feet. 44 x 20.
private fun PaperPainter.pickleball() {
    val f = fit(44f, 20f)
    box(f, 0f, 0f, 44f, 20f, line(1.3f))
    ln(f, 22f, -0.8f, 22f, 20.8f, line(2f))
    ln(f, 15f, 0f, 15f, 20f, line())
    ln(f, 29f, 0f, 29f, 20f, line())
    ln(f, 0f, 10f, 15f, 10f, line())
    ln(f, 29f, 10f, 44f, 10f, line())
}
