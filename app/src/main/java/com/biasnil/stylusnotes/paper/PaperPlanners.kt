package com.biasnil.stylusnotes.paper

import android.graphics.Paint
import android.graphics.RectF

private const val M = 0.5f * IN

private enum class Area { BLANK, RULED, DOT }

internal fun PaperPainter.planner(id: String): Boolean {
    when (id) {
        "daily_blank" -> daily(Area.BLANK)
        "daily_college" -> daily(Area.RULED)
        "daily_dot" -> daily(Area.DOT)
        "weekly_blank" -> weekly(Area.BLANK)
        "weekly_college" -> weekly(Area.RULED)
        "weekly_dot" -> weekly(Area.DOT)
        "weekly_cols_5" -> weeklyColumns(seven = false)
        "weekly_cols_7" -> weeklyColumns(seven = true)
        "monthly_5" -> monthly(5)
        "monthly_6" -> monthly(6)
        "tasks" -> taskList(twoColumns = false)
        "tasks_2col" -> taskList(twoColumns = true)
        else -> return false
    }
    return true
}

private fun PaperPainter.fillArea(kind: Area, l: Float, t: Float, r: Float, b: Float) {
    when (kind) {
        Area.BLANK -> Unit
        Area.RULED -> ruled(COLLEGE, top = t + COLLEGE, bottom = b - 2f, x0 = l, x1 = r)
        Area.DOT -> dotGrid(IN / 5, l + IN / 10, t + IN / 10, r - 1f, b - 1f)
    }
}

private fun PaperPainter.dateTab(right: Float, top: Float) {
    val rect = RectF(right - 1.3f * IN, top, right, top + 0.32f * IN)
    c.drawRoundRect(rect, 3f, 3f, brush(PaperColors.FILL))
    c.drawRoundRect(rect, 3f, 3f, pen(PaperColors.LINE, 0.8f))
}

private fun PaperPainter.daily(area: Area) {
    val l = M
    val r = w - M
    dateTab(r, M)
    val y0 = M + 0.55f * IN
    val b = h - M
    val col = 1.9f * IN
    val rows = 16
    val rowH = (b - y0) / rows
    for (i in 0 until rows) {
        val y = y0 + i * rowH
        if (i % 2 == 0) c.drawRect(l, y, l + col, y + rowH, brush(PaperColors.FILL))
        hLine(y, pen(PaperColors.LINE, 0.4f), l, l + col)
        val hour = 6 + i
        val text = when {
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
        label(text, l + 6f, y + 12f, 7f)
    }
    fillArea(area, l + col, y0, r, b)
    c.drawRect(l, y0, r, b, pen(PaperColors.LINE, 0.9f))
    vLine(l + col, pen(PaperColors.LINE, 0.9f), y0, b)
}

private fun PaperPainter.weekly(area: Area) {
    val l = M
    val r = w - M
    dateTab(r, M)
    val y0 = M + 0.55f * IN
    val b = h - M
    val cw = (r - l) / 2
    val ch = (b - y0) / 4
    val head = 0.3f * IN
    val names = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "NOTES")
    for (i in names.indices) {
        val x = l + (i % 2) * cw
        val y = y0 + (i / 2) * ch
        c.drawRect(x, y, x + cw, y + head, brush(PaperColors.FILL))
        label(names[i], x + 6f, y + 14f, 7.5f, bold = true)
        fillArea(area, x, y + head, x + cw, y + ch)
        c.drawRect(x, y, x + cw, y + ch, pen(PaperColors.LINE, 0.9f))
    }
}

private fun PaperPainter.dayColumn(x0: Float, x1: Float, t: Float, b: Float, name: String) {
    val head = 0.32f * IN
    val stripe = 0.3f * IN
    c.drawRect(x0, t, x1, t + head, brush(PaperColors.FILL))
    label(name, x0 + 5f, t + 15f, 7.5f, bold = true)
    var i = 0
    while (t + head + (i + 1) * stripe <= b) {
        if (i % 2 == 1) c.drawRect(x0, t + head + i * stripe, x1, t + head + (i + 1) * stripe, brush(PaperColors.FILL))
        i++
    }
    c.drawRect(x0, t, x1, b, pen(PaperColors.LINE, 0.9f))
}

private fun PaperPainter.weeklyColumns(seven: Boolean) {
    val l = M
    val r = w - M
    dateTab(r, M)
    val y0 = M + 0.55f * IN
    val notesTop = h - M - 2.0f * IN
    val colBottom = notesTop - 0.15f * IN
    val n = if (seven) 6 else 5
    val cw = (r - l) / n
    val names = listOf("MON", "TUE", "WED", "THU", "FRI")
    for (i in 0 until 5) dayColumn(l + i * cw, l + (i + 1) * cw, y0, colBottom, names[i])
    if (seven) {
        val x0 = l + 5 * cw
        val mid = (y0 + colBottom) / 2
        dayColumn(x0, r, y0, mid, "SAT")
        dayColumn(x0, r, mid, colBottom, "SUN")
    }
    c.drawRect(l, notesTop, r, h - M, pen(PaperColors.LINE, 0.9f))
    label("NOTES", l + 6f, notesTop + 14f, 7.5f, bold = true)
    ruled(COLLEGE, top = notesTop + 0.35f * IN, bottom = h - M - 4f, x0 = l, x1 = r)
}

private fun PaperPainter.monthly(weeks: Int) {
    val l = M
    val r = w - M
    c.drawRect(l, M, l + 2.6f * IN, M + 0.45f * IN, brush(PaperColors.FILL))
    label("MONTH", l + 8f, M + 0.3f * IN, 8f, bold = true)
    val y0 = M + 0.75f * IN
    val head = 0.3f * IN
    val b = h - M
    val cw = (r - l) / 7
    val rh = (b - y0 - head) / weeks
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    c.drawRect(l, y0, r, y0 + head, brush(PaperColors.FILL))
    days.forEachIndexed { i, d -> label(d, l + (i + 0.5f) * cw, y0 + head * 0.68f, 7.5f, align = Paint.Align.CENTER, bold = true) }
    val box = 0.2f * IN
    for (j in 0 until weeks) for (i in 0 until 7) {
        val x = l + i * cw
        val y = y0 + head + j * rh
        c.drawRect(x + 3f, y + 3f, x + 3f + box, y + 3f + box, brush(PaperColors.FILL))
    }
    for (i in 0..7) vLine(l + i * cw, pen(PaperColors.LINE, 0.8f), y0, b)
    hLine(y0, pen(PaperColors.LINE, 0.8f), l, r)
    for (j in 0..weeks) hLine(y0 + head + j * rh, pen(PaperColors.LINE, 0.8f), l, r)
}

private fun PaperPainter.taskRows(x0: Float, x1: Float, top: Float, bottom: Float, rowH: Float, box: Float) {
    var y = top
    var i = 0
    while (y + rowH <= bottom) {
        if (i % 2 == 0) c.drawRect(x0, y, x1, y + rowH, brush(PaperColors.FILL))
        val bx = x0 + 8f
        val by = y + (rowH - box) / 2
        c.drawRect(bx, by, bx + box, by + box, pen(PaperColors.LINE, 0.8f))
        y += rowH
        i++
    }
}

private fun PaperPainter.taskList(twoColumns: Boolean) {
    val l = M
    val r = w - M
    if (!twoColumns) {
        taskRows(l, r, 1.2f * IN, h - M, 0.42f * IN, 0.18f * IN)
    } else {
        val mid = (l + r) / 2
        taskRows(l, mid - 0.1f * IN, 0.9f * IN, h - M, 0.26f * IN, 0.12f * IN)
        taskRows(mid + 0.1f * IN, r, 0.9f * IN, h - M, 0.26f * IN, 0.12f * IN)
        vLine(mid, pen(PaperColors.LINE, 0.6f), 0.9f * IN, h - M)
    }
}
