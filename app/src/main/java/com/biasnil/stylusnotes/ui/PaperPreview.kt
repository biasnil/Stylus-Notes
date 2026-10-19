package com.biasnil.stylusnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.biasnil.stylusnotes.paper.PaperRenderer

/**
 * Draws a paper background scaled to fit the width of the box. Portrait pages show
 * their top part; landscape pages are centred. The box should have a white background.
 */
@Composable
fun PaperPreview(templateId: String, pageW: Float, pageH: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (size.width <= 0f || pageW <= 0f) return@Canvas
        val s = size.width / pageW
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            nc.save()
            nc.clipRect(0f, 0f, size.width, size.height)
            val ch = pageH * s
            if (ch < size.height) nc.translate(0f, (size.height - ch) / 2f)
            nc.scale(s, s)
            PaperRenderer.draw(nc, templateId, pageW, pageH, 0.9f / s)
            nc.restore()
        }
    }
}
