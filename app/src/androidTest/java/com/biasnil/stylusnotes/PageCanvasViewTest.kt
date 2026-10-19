package com.biasnil.stylusnotes

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.biasnil.stylusnotes.canvas.PageCanvasView
import com.biasnil.stylusnotes.canvas.Tool
import com.biasnil.stylusnotes.canvas.ToolSettings
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.StrokeTool
import com.biasnil.stylusnotes.input.DeviceLookup
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenIdentity
import com.biasnil.stylusnotes.input.StylusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feeds synthetic MotionEvents straight into the page canvas and checks what gets drawn.
 * Run with: ./gradlew connectedAndroidTest (device or emulator on API 34+).
 */
@RunWith(AndroidJUnit4::class)
class PageCanvasViewTest {

    private val pen = 42
    private val otherPen = 43
    private val touchscreen = 7

    private lateinit var view: PageCanvasView
    private lateinit var filter: StylusFilter
    private var time = 1000L

    private data class Ptr(val id: Int, val tool: Int, val x: Float, val y: Float)

    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Before
    fun setUp() {
        main {
            view = PageCanvasView(InstrumentationRegistry.getInstrumentation().targetContext)
            // Fake lookup: no real devices, so the filter matches on deviceId alone.
            filter = StylusFilter(DeviceLookup { null }).apply {
                registeredPens = listOf(PenIdentity(pen, "", "Test Pen", 0, 0))
            }
            view.filter = filter
            view.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, 1000, 1000)
            view.setPage(Page(id = "p1", width = 1000f, height = 1000f))
        }
    }

    private fun send(action: Int, deviceId: Int, pointers: List<Ptr>, flags: Int = 0) {
        val props = pointers.map { p ->
            MotionEvent.PointerProperties().apply { id = p.id; toolType = p.tool }
        }.toTypedArray()
        val coords = pointers.map { p ->
            MotionEvent.PointerCoords().apply { x = p.x; y = p.y; pressure = 0.6f; size = 0.1f }
        }.toTypedArray()
        time += 8
        val source = if (pointers.any { it.tool == MotionEvent.TOOL_TYPE_STYLUS || it.tool == MotionEvent.TOOL_TYPE_ERASER })
            InputDevice.SOURCE_STYLUS or InputDevice.SOURCE_TOUCHSCREEN
        else InputDevice.SOURCE_TOUCHSCREEN
        val ev = MotionEvent.obtain(1000L, time, action, pointers.size, props, coords, 0, 0, 1f, 1f, deviceId, 0, source, flags)
        main { view.onTouchEvent(ev) }
        ev.recycle()
    }

    private fun pointerAction(action: Int, index: Int) = action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)

    private fun strokeCount(): Int {
        var n = 0
        main { n = view.strokes.size }
        return n
    }

    private fun drawLine(deviceId: Int, tool: Int, y: Float = 100f) {
        send(MotionEvent.ACTION_DOWN, deviceId, listOf(Ptr(0, tool, 100f, y)))
        send(MotionEvent.ACTION_MOVE, deviceId, listOf(Ptr(0, tool, 150f, y + 20f)))
        send(MotionEvent.ACTION_MOVE, deviceId, listOf(Ptr(0, tool, 200f, y + 40f)))
        send(MotionEvent.ACTION_UP, deviceId, listOf(Ptr(0, tool, 220f, y + 50f)))
    }

    @Test fun toolTypeConstantsMatchAndroid() {
        assertEquals(MotionEvent.TOOL_TYPE_FINGER, StylusFilter.TOOL_TYPE_FINGER)
        assertEquals(MotionEvent.TOOL_TYPE_STYLUS, StylusFilter.TOOL_TYPE_STYLUS)
        assertEquals(MotionEvent.TOOL_TYPE_MOUSE, StylusFilter.TOOL_TYPE_MOUSE)
        assertEquals(MotionEvent.TOOL_TYPE_ERASER, StylusFilter.TOOL_TYPE_ERASER)
    }

    @Test fun fingerProducesNoMarks() {
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_FINGER)
        assertEquals(0, strokeCount())
    }

    @Test fun fingerScrollsInPenOnlyMode() {
        var before = 0f
        main {
            view.setPage(Page(id = "tall", width = 1000f, height = 3000f))
            before = view.toPageY(500f)
        }
        send(MotionEvent.ACTION_DOWN, touchscreen, listOf(Ptr(0, MotionEvent.TOOL_TYPE_FINGER, 500f, 600f)))
        send(MotionEvent.ACTION_MOVE, touchscreen, listOf(Ptr(0, MotionEvent.TOOL_TYPE_FINGER, 500f, 300f)))
        send(MotionEvent.ACTION_UP, touchscreen, listOf(Ptr(0, MotionEvent.TOOL_TYPE_FINGER, 500f, 300f)))
        var after = 0f
        main { after = view.toPageY(500f) }
        assertNotEquals(before, after, 0.01f)
        assertEquals(0, strokeCount())
    }

    @Test fun mouseProducesNoMarks() {
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_MOUSE)
        assertEquals(0, strokeCount())
    }

    @Test fun registeredPenDraws() {
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(1, strokeCount())
    }

    @Test fun secondStylusIsRejected() {
        drawLine(otherPen, MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(0, strokeCount())
    }

    @Test fun secondRegisteredPenDraws() {
        main { filter.registeredPens = filter.registeredPens + PenIdentity(otherPen, "", "Pen 2", 0, 0) }
        drawLine(otherPen, MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(1, strokeCount())
    }

    @Test fun cancelledStrokeLeavesNothing() {
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 100f, 100f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 180f, 130f)))
        send(MotionEvent.ACTION_CANCEL, pen, listOf(Ptr(0, s, 180f, 130f)), MotionEvent.FLAG_CANCELED)
        assertEquals(0, strokeCount())
    }

    @Test fun upWithCanceledFlagLeavesNothing() {
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 100f, 100f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 180f, 130f)))
        send(MotionEvent.ACTION_UP, pen, listOf(Ptr(0, s, 180f, 130f)), MotionEvent.FLAG_CANCELED)
        assertEquals(0, strokeCount())
    }

    @Test fun cancelFromAnotherDeviceDoesNotKillPenStroke() {
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 100f, 100f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 150f, 120f)))
        send(MotionEvent.ACTION_CANCEL, touchscreen, listOf(Ptr(0, MotionEvent.TOOL_TYPE_FINGER, 500f, 500f)))
        send(MotionEvent.ACTION_UP, pen, listOf(Ptr(0, s, 200f, 140f)))
        assertEquals(1, strokeCount())
    }

    /** Palm rests first, then the pen comes down as a second pointer and writes. */
    @Test fun palmFirstThenPenStillWrites() {
        val f = MotionEvent.TOOL_TYPE_FINGER
        val s = MotionEvent.TOOL_TYPE_STYLUS
        val palm = Ptr(0, f, 600f, 800f)
        send(MotionEvent.ACTION_DOWN, pen, listOf(palm))
        send(pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1), pen, listOf(palm, Ptr(1, s, 100f, 100f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(palm.copy(x = 610f), Ptr(1, s, 160f, 120f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(palm.copy(x = 620f), Ptr(1, s, 220f, 140f)))
        send(pointerAction(MotionEvent.ACTION_POINTER_UP, 1), pen, listOf(palm.copy(x = 620f), Ptr(1, s, 230f, 145f)))
        send(MotionEvent.ACTION_UP, pen, listOf(palm.copy(x = 620f)))
        assertEquals(1, strokeCount())
    }

    /** Pen is writing, palm lands and lifts mid-stroke; stroke continues and palm draws nothing. */
    @Test fun palmDuringStrokeDoesNotInterrupt() {
        val f = MotionEvent.TOOL_TYPE_FINGER
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 100f, 100f)))
        send(pointerAction(MotionEvent.ACTION_POINTER_DOWN, 1), pen, listOf(Ptr(0, s, 120f, 105f), Ptr(1, f, 600f, 800f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 160f, 120f), Ptr(1, f, 605f, 805f)))
        send(
            pointerAction(MotionEvent.ACTION_POINTER_UP, 1), pen,
            listOf(Ptr(0, s, 170f, 125f), Ptr(1, f, 605f, 805f)), MotionEvent.FLAG_CANCELED,
        )
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 200f, 140f)))
        send(MotionEvent.ACTION_UP, pen, listOf(Ptr(0, s, 210f, 145f)))
        assertEquals(1, strokeCount())
        var points = 0
        main { points = view.strokes[0].points.size }
        // DOWN, MOVE, MOVE, UP from the pen. Nothing from the palm.
        assertEquals(4, points)
    }

    @Test fun fingerDrawsInPenAndFingerMode() {
        main { filter.inputMode = InputMode.PEN_AND_FINGER }
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_FINGER)
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS, y = 300f)
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_FINGER, y = 500f)
        assertEquals(3, strokeCount())
    }

    @Test fun autoModeFingerThenPenTurnsFingersOff() {
        main { filter.inputMode = InputMode.AUTO }
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_FINGER)
        assertEquals(1, strokeCount())
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS, y = 300f)
        assertEquals(2, strokeCount())
        drawLine(touchscreen, MotionEvent.TOOL_TYPE_FINGER, y = 500f)
        assertEquals(2, strokeCount())
    }

    /** Hand starts a finger stroke, pen comes down: the finger stroke is dropped, the pen writes. */
    @Test fun penTakesOverFromFingerStroke() {
        main { filter.inputMode = InputMode.AUTO }
        val f = MotionEvent.TOOL_TYPE_FINGER
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, touchscreen, listOf(Ptr(0, f, 600f, 800f)))
        send(MotionEvent.ACTION_MOVE, touchscreen, listOf(Ptr(0, f, 610f, 805f)))
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 100f, 100f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 160f, 120f)))
        send(MotionEvent.ACTION_UP, pen, listOf(Ptr(0, s, 200f, 140f)))
        assertEquals(1, strokeCount())
        var x = 0f
        var expected = 0f
        main {
            x = view.strokes[0].points[0].x
            expected = view.toPageX(100f)
        }
        assertEquals(expected, x, 0.01f)
    }

    @Test fun eraserEndRemovesStroke() {
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS)
        assertEquals(1, strokeCount())
        drawLine(pen, MotionEvent.TOOL_TYPE_ERASER)
        assertEquals(0, strokeCount())
    }

    @Test fun undoRestoresErasedStroke() {
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS)
        drawLine(pen, MotionEvent.TOOL_TYPE_ERASER)
        main { view.undo() }
        assertEquals(1, strokeCount())
    }

    @Test fun highlighterMakesHighlighterStroke() {
        main { view.tools = ToolSettings(tool = Tool.HIGHLIGHTER) }
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS)
        var tool: StrokeTool? = null
        main { tool = view.strokes[0].tool }
        assertEquals(StrokeTool.HIGHLIGHTER, tool)
    }

    @Test fun undoHistoryIsPerPage() {
        drawLine(pen, MotionEvent.TOOL_TYPE_STYLUS)
        main {
            view.setPage(Page(id = "p2", width = 1000f, height = 1000f))
            view.undo() // nothing to undo on the new page
        }
        assertEquals(0, strokeCount())
    }

    @Test fun selectToolMovesPicture() {
        main {
            view.tools = ToolSettings(tool = Tool.SELECT)
            view.addImage(PlacedImage(5, "missing.jpg", view.toPageX(100f), view.toPageY(100f), 200f, 200f))
        }
        val s = MotionEvent.TOOL_TYPE_STYLUS
        send(MotionEvent.ACTION_DOWN, pen, listOf(Ptr(0, s, 150f, 150f)))
        send(MotionEvent.ACTION_MOVE, pen, listOf(Ptr(0, s, 250f, 150f)))
        send(MotionEvent.ACTION_UP, pen, listOf(Ptr(0, s, 250f, 150f)))
        var moved = 0f
        var start = 0f
        main {
            moved = view.images[0].x
            start = view.toPageX(100f)
        }
        assertEquals(start + (view.toPageX(250f) - view.toPageX(150f)), moved, 0.5f)
        assertEquals(0, strokeCount())
    }
}
