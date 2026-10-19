package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Color as AColor

/** One hexagon in the honeycomb, in axial coordinates (q, r). */
private data class Cell(val q: Int, val r: Int, val hue: Float, val sat: Float)

private const val RINGS = 6
private val SQRT3 = sqrt(3f)

/** Honeycomb cells: white in the middle, full colour at the edge, hue going round. */
private val CELLS: List<Cell> = buildList {
    for (q in -RINGS..RINGS) for (r in -RINGS..RINGS) {
        val s = -q - r
        if (abs(s) > RINGS) continue
        val ring = maxOf(abs(q), abs(r), abs(s))
        // Pointy-top layout, unit size.
        val x = SQRT3 * (q + r / 2f)
        val y = 1.5f * r
        var hue = Math.toDegrees(atan2(-y.toDouble(), x.toDouble())).toFloat()
        if (hue < 0) hue += 360f
        add(Cell(q, r, hue, ring / RINGS.toFloat()))
    }
}

private val GRAYS = listOf(0xFF000000, 0xFF333333, 0xFF555555, 0xFF777777, 0xFF999999, 0xFFBBBBBB, 0xFFDDDDDD, 0xFFFFFFFF)
    .map { it.toInt() }

private fun hsv(h: Float, s: Float, v: Float): Int = AColor.HSVToColor(floatArrayOf(h, s, v))

private fun parseHex(text: String): Int? {
    val t = text.trim().removePrefix("#")
    if (t.length != 6) return null
    return t.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
}

private fun toHex(c: Int) = "#%06X".format(c and 0xFFFFFF)

/**
 * "Pick a colour" dialog: a honeycomb of hues like Office's, a brightness slider,
 * a row of greys and a hex code box.
 */
@Composable
fun ColorPickerDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val start = remember { FloatArray(3).also { AColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(if (start[2] < 0.05f) 1f else start[2]) }
    var picked by remember { mutableStateOf(initial) }
    var hexText by remember { mutableStateOf(toHex(initial)) }

    fun setFromHsv(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        picked = hsv(h, s, v)
        hexText = toHex(picked)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 360.dp)) {
                Honeycomb(
                    hue = hue,
                    sat = sat,
                    value = value,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.05f),
                ) { h, s -> setFromHsv(h, s, value) }

                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                Slider(value = value, onValueChange = { setFromHsv(hue, sat, it) }, valueRange = 0f..1f)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    GRAYS.forEach { g ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(g))
                                .border(if (picked == g) 3.dp else 1.dp, if (picked == g) Palette.accent else Color.Gray, RoundedCornerShape(6.dp))
                                .clickable {
                                    picked = g
                                    hexText = toHex(g)
                                },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(picked))
                            .border(1.dp, Color.Gray, RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { t ->
                            hexText = t
                            parseHex(t)?.let { c ->
                                val hsvArr = FloatArray(3)
                                AColor.colorToHSV(c, hsvArr)
                                hue = hsvArr[0]
                                sat = hsvArr[1]
                                value = hsvArr[2]
                                picked = c
                            }
                        },
                        label = { Text(stringResource(R.string.hex_code)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(picked) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Honeycomb(
    hue: Float,
    sat: Float,
    value: Float,
    modifier: Modifier,
    onPick: (hue: Float, sat: Float) -> Unit,
) {
    // Nearest cell to the current colour, to outline it.
    val selected = remember(hue, sat) {
        CELLS.minByOrNull { c ->
            val dh = min(abs(c.hue - hue), 360f - abs(c.hue - hue)) / 180f
            val ds = c.sat - sat
            (if (c.sat == 0f || sat < 0.05f) 0f else dh * dh * c.sat) + ds * ds
        }
    }
    var layout by remember { mutableStateOf(Triple(0f, 0f, 1f)) } // centre x, centre y, hex size

    fun pickAt(p: Offset) {
        val (cx, cy, size) = layout
        val cell = CELLS.minByOrNull { c ->
            val x = cx + size * SQRT3 * (c.q + c.r / 2f)
            val y = cy + size * 1.5f * c.r
            (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y)
        } ?: return
        onPick(cell.hue, cell.sat)
    }

    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { pickAt(it) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> pickAt(change.position) } },
    ) {
        // Fit (2*RINGS+1) hexes across the width.
        val hs = min(size.width / (SQRT3 * (2 * RINGS + 1)), size.height / (1.5f * 2 * RINGS + 2f))
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        layout = Triple(cx, cy, hs)
        val hex = Path()
        for (c in CELLS) {
            val x = cx + hs * SQRT3 * (c.q + c.r / 2f)
            val y = cy + hs * 1.5f * c.r
            hex.reset()
            for (k in 0..5) {
                val a = Math.toRadians((60.0 * k) - 30.0)
                val px = x + hs * 0.98f * cos(a).toFloat()
                val py = y + hs * 0.98f * sin(a).toFloat()
                if (k == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
            }
            hex.close()
            drawPath(hex, Color(hsv(c.hue, c.sat, value)))
            if (c == selected) {
                drawPath(hex, Color.Black, style = Stroke(width = max(2f, hs * 0.18f)))
                drawPath(hex, Color.White, style = Stroke(width = max(1f, hs * 0.08f)))
            }
        }
    }
}
