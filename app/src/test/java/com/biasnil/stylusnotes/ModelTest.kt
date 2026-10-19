package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.canvas.Tool
import com.biasnil.stylusnotes.canvas.ToolSettings
import com.biasnil.stylusnotes.data.Note
import com.biasnil.stylusnotes.data.NoteMigration
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PdfRef
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    private val stroke = Stroke(1, 0, 4f, listOf(StrokePoint(1500f, 2400f, 0.5f, 0)))

    @Test fun v1NoteBecomesOnePageBigEnoughForItsInk() {
        val old = Note(id = "x", created = 0, modified = 0, strokes = listOf(stroke))
        val m = NoteMigration.migrate(old)
        assertEquals(1, m.pages.size)
        assertTrue(m.strokes.isEmpty())
        val p = m.pages[0]
        assertEquals(listOf(stroke), p.strokes)
        assertTrue(p.width >= 1532f)
        assertTrue(p.height >= 2432f)
    }

    @Test fun emptyV1NoteGetsLetterPage() {
        val m = NoteMigration.migrate(Note(id = "x", created = 0, modified = 0))
        assertEquals(612f, m.pages[0].width)
        assertEquals(792f, m.pages[0].height)
    }

    @Test fun currentNotesAreUntouched() {
        val n = Note(id = "x", created = 0, modified = 0, pages = listOf(Page()))
        assertSame(n, NoteMigration.migrate(n))
    }

    @Test fun assetIdsCollectPdfsAndImages() {
        val n = Note(
            id = "x", created = 0, modified = 0,
            pages = listOf(
                Page(pdf = PdfRef("doc.pdf", 0)),
                Page(pdf = PdfRef("doc.pdf", 1), images = listOf(PlacedImage(1, "pic.jpg", 0f, 0f, 1f, 1f))),
            ),
        )
        assertEquals(listOf("doc.pdf", "pic.jpg"), n.assetIds)
    }

    @Test fun sideButtonTogglesEraserAndBack() {
        val t = ToolSettings(tool = Tool.HIGHLIGHTER)
        val erasing = t.toggleEraser()
        assertEquals(Tool.ERASER, erasing.tool)
        assertEquals(Tool.HIGHLIGHTER, erasing.toggleEraser().tool)
    }

    @Test fun pickingEraserFromMenuRemembersTool() {
        val t = ToolSettings(tool = Tool.PEN).select(Tool.ERASER)
        assertEquals(Tool.PEN, t.toggleEraser().tool)
    }

    @Test fun defaultSettingsMatchSquidStyleDefaults() {
        val s = com.biasnil.stylusnotes.data.AppSettings()
        assertTrue(s.resumeLastPage)
        assertTrue(s.savePagePositions)
        assertEquals(com.biasnil.stylusnotes.data.ThemeChoice.SYSTEM, s.theme)
        assertEquals(com.biasnil.stylusnotes.data.PenAction.TOGGLE_ERASER, s.buttons.primary)
        assertEquals(-1, s.secondaryKey)
    }
}
