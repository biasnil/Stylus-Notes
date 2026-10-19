package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.data.Folder
import com.biasnil.stylusnotes.data.LibraryIndex
import com.biasnil.stylusnotes.data.Note
import com.biasnil.stylusnotes.data.NoteJson
import com.biasnil.stylusnotes.data.NoteMeta
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PageView
import com.biasnil.stylusnotes.data.PdfRef
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint
import com.biasnil.stylusnotes.data.StrokeTool
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteJsonTest {

    @Test fun roundTripKeepsPagesAndInk() {
        val note = Note(
            id = "abc", title = "Lecture 4", created = 1, modified = 2,
            pages = listOf(
                Page(id = "p1", template = "graph_5", strokes = listOf(
                    Stroke(7, 0xFF1F4FB8.toInt(), 1.8f, listOf(StrokePoint(1f, 2f, 0.3f, 100), StrokePoint(3.5f, 4.25f, 0.9f, 116))),
                    Stroke(8, 0xFFFFD54F.toInt(), 12f, listOf(StrokePoint(5f, 5f, 0.5f, 200)), StrokeTool.HIGHLIGHTER),
                )),
                Page(id = "p2", width = 595f, height = 842f, pdf = PdfRef("x.pdf", 3, keepText = true), view = PageView(1.5f, 10f, 200f)),
            ),
        )
        assertEquals(note, NoteJson.decode(NoteJson.encode(note)))
    }

    @Test fun version1FileIsUpgraded() {
        val v1 = """{"id":"old","title":"","created":1,"modified":1,"strokes":[{"id":1,"color":-16777216,"baseWidth":4.0,"points":[{"x":10.0,"y":20.0,"p":0.5,"t":0}]}]}"""
        val n = NoteJson.decode(v1)
        assertEquals(1, n.pages.size)
        assertEquals(1, n.pages[0].strokes.size)
        assertEquals(StrokeTool.PEN, n.pages[0].strokes[0].tool)
    }

    @Test fun unknownFieldsFromNewerVersionsAreIgnored() {
        val text = """{"id":"x","title":"","created":1,"modified":1,"pages":[],"pageColor":"#fff"}"""
        assertEquals("x", NoteJson.decode(text).id)
    }

    @Test fun indexRoundTrip() {
        val idx = LibraryIndex(
            folders = listOf(Folder("f", "School", null, 5)),
            notes = listOf(NoteMeta("n", "Hi", "f", created = 1, modified = 2, opened = 3, pageCount = 4, lastPage = 2, assets = listOf("a.pdf"))),
        )
        assertEquals(idx, NoteJson.decodeIndex(NoteJson.encodeIndex(idx)))
    }
}
