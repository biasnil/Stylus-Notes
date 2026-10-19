package com.biasnil.stylusnotes

import android.graphics.Canvas
import com.biasnil.stylusnotes.paper.PaperCatalog
import com.biasnil.stylusnotes.paper.PaperRenderer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android's graphics classes are no-op stubs in JVM tests (unitTests.isReturnDefaultValues),
 * so this checks that every catalog id has a drawing routine and none of them crash.
 */
class PaperRendererTest {

    @Test fun everyCatalogIdIsDrawn() {
        val canvas = Canvas()
        for (spec in PaperCatalog.all) {
            val (w, h) = if (spec.landscape) 792f to 612f else 612f to 792f
            assertTrue("No renderer for ${spec.id}", PaperRenderer.draw(canvas, spec.id, w, h, 0.5f))
        }
    }

    @Test fun unknownIdReportsFalse() = assertFalse(PaperRenderer.draw(Canvas(), "does_not_exist", 612f, 792f, 0.5f))
}
