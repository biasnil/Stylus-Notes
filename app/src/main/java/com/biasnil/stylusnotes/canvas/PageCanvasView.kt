package com.biasnil.stylusnotes.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.biasnil.stylusnotes.data.EraserEnd
import com.biasnil.stylusnotes.data.Page
import com.biasnil.stylusnotes.data.PageView
import com.biasnil.stylusnotes.data.PenAction
import com.biasnil.stylusnotes.data.PenButtons
import com.biasnil.stylusnotes.data.ZoomMode
import com.biasnil.stylusnotes.data.PdfRef
import com.biasnil.stylusnotes.data.PlacedImage
import com.biasnil.stylusnotes.data.Stroke
import com.biasnil.stylusnotes.data.StrokePoint
import com.biasnil.stylusnotes.data.StrokeTool
import com.biasnil.stylusnotes.input.StylusFilter
import com.biasnil.stylusnotes.input.StylusFilter.Verdict
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * The writing surface for one page at a time.
 *
 * Input rules:
 *  - Every touch is consumed, so a palm that lands first doesn't stop the pen
 *    from arriving as a later pointer in the same gesture.
 *  - One pointer draws at a time, and only if StylusFilter accepts it.
 *    A pen arriving while a finger draws takes over; the finger stroke is dropped.
 *  - Fingers that aren't allowed to draw scroll the page (one finger) or zoom it (two),
 *    unless the pen is hovering, is down, or the touch is palm-sized.
 *  - ACTION_CANCEL, or an UP carrying FLAG_CANCELED, throws the stroke in progress away.
 *
 * Strokes and images are stored in page units (points). The view maps them to the
 * screen with scale + offset.
 */
@SuppressLint("ClickableViewAccessibility")
class PageCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onContentChanged(pageId: String, strokes: List<Stroke>, images: List<PlacedImage>)
        fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean)
        fun onRejected(verdict: Verdict)
        fun onSelectionChanged(hasSelection: Boolean)
        /** Two fingers tapped twice quickly. */
        fun onTwoFingerDoubleTap() {}
        /** Scroll or zoom finished; [view] can be stored and passed back with the page. */
        fun onViewChanged(pageId: String, view: PageView) {}
    }

    /** Supplies decoded images and rendered PDF pages. Returns null while still loading. */
    interface Assets {
        fun image(assetId: String): Bitmap?
        fun pdf(ref: PdfRef, widthPx: Int): Bitmap?
    }

    /** No filter means nothing can draw. Safer than accepting everything. */
    var filter: StylusFilter? = null
    var listener: Listener? = null
    var assets: Assets? = null

    /** What the pen's side buttons and eraser end do while touching the page. */
    var buttons: PenButtons = PenButtons()

    /** How a page fits the screen when opened (and when zoom is reset). */
    var zoomMode: ZoomMode = ZoomMode.FIT_WIDTH

    /** Use the scroll/zoom saved in Page.view when a page is shown. */
    var restoreViews: Boolean = true

    var deskColor: Int = 0xFFE9E5EF.toInt()
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var tools: ToolSettings = ToolSettings()
        set(value) {
            if (field == value) return
            val oldTool = field.tool
            field = value
            if (oldTool == Tool.SELECT && value.tool != Tool.SELECT) clearSelection()
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val pad = 16f * density
    private val palmPx = 22f / 25.4f * resources.displayMetrics.xdpi
    private val eraserRadiusPx = 12f * density
    private val handleRadiusPx = 9f * density
    private val handleHitPx = 24f * density
    private val rotateGapPx = 28f * density

    // ---------------------------------------------------------------- page content

    private var page: Page? = null
    private val _strokes = ArrayList<Stroke>()
    private val _images = ArrayList<PlacedImage>()
    val strokes: List<Stroke> get() = _strokes
    val images: List<PlacedImage> get() = _images
    private var nextStrokeId = 1L

    private data class Snapshot(val strokes: List<Stroke>, val images: List<PlacedImage>)

    private class History {
        val undo = ArrayDeque<Snapshot>()
        val redo = ArrayDeque<Snapshot>()
    }

    /** Undo history per page, kept while the note is open. */
    private val histories = HashMap<String, History>()

    // ---------------------------------------------------------------- view transform

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f
    private var fitted = false

    // ---------------------------------------------------------------- gestures

    private enum class Mode { INK, ERASE, MOVE, RESIZE, ROTATE }

    private class Gesture(
        val pointerId: Int,
        val deviceId: Int,
        val isFinger: Boolean,
        val mode: Mode,
        val strokeTool: StrokeTool,
        val color: Int,
        val width: Float,
        /** Content before the gesture, for undo/cancel of erase and image moves. */
        val before: Snapshot?,
        val startX: Float,
        val startY: Float,
        val image: PlacedImage?,
        /** ROTATE: picture rotation minus the finger's starting angle. RESIZE: starting distance. */
        val ref: Float = 0f,
    ) {
        val points = ArrayList<StrokePoint>(256)
        var changed = false
        var lastX = startX
        var lastY = startY
    }

    private var gesture: Gesture? = null

    private val navPointers = LinkedHashMap<Int, PointF>()
    private var navigating = false
    private var navDeviceId = -1
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var lastSpan = 0f
    private var lastPenHover = -10_000L
    private var navStart = 0L
    private var navMoved = 0f
    private var navMaxPointers = 0
    private var lastEventTime = 0L
    private var lastTwoFingerTap = -10_000L
    /** Saved view to apply at the next fit (set by setPage). */
    private var pendingView: PageView? = null

    private var selectedId = -1L

    // ---------------------------------------------------------------- rendering

    private var cache: Bitmap? = null
    private var cacheCanvas: Canvas? = null
    private var cacheValid = false
    private var cacheScale = 1f
    private var cacheOffX = 0f
    private var cacheOffY = 0f

    private val inkPaint = StrokeRenderer.newPaint()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint().apply { color = 0x33000000 }
    private val deskPaint = Paint()
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF7A4FD0.toInt()
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7A4FD0.toInt() }
    private val handleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.GRAY
    }

    // ================================================================ public API

    /**
     * Shows [p]. Calling it again with the same page id only picks up background changes,
     * so the content and undo history the view holds aren't thrown away.
     */
    fun setPage(p: Page) {
        val cur = page
        if (cur != null && cur.id == p.id) {
            if (cur.template != p.template || cur.pdf != p.pdf || cur.width != p.width || cur.height != p.height) {
                page = p.copy(strokes = emptyList(), images = emptyList())
                invalidateCache()
            }
            return
        }
        gesture = null
        endNav(released = false)
        page = p.copy(strokes = emptyList(), images = emptyList())
        _strokes.clear()
        _strokes.addAll(p.strokes)
        _images.clear()
        _images.addAll(p.images)
        nextStrokeId = (p.strokes.maxOfOrNull { it.id } ?: 0L) + 1
        if (selectedId != -1L) {
            selectedId = -1L
            listener?.onSelectionChanged(false)
        }
        pendingView = if (restoreViews) p.view else null
        fitted = false
        fitIfNeeded()
        invalidateCache()
        notifyHistory()
    }

    /**
     * Read-only mirror (external display): always replaces the content and keeps the
     * current scroll and zoom. No undo history.
     */
    fun mirror(p: Page) {
        val same = page?.id == p.id
        page = p.copy(strokes = emptyList(), images = emptyList())
        _strokes.clear()
        _strokes.addAll(p.strokes)
        _images.clear()
        _images.addAll(p.images)
        if (!same) {
            fitted = false
            fitIfNeeded()
        }
        invalidateCache()
    }

    /** Current scroll and zoom, to store with the page. */
    fun currentView(): PageView? {
        if (page == null || width <= 0) return null
        return PageView(zoom = scale / fitWidthScale(), x = -offX / scale, y = -offY / scale)
    }

    fun undo() {
        val h = history() ?: return
        if (gesture != null || h.undo.isEmpty()) return
        h.redo.addLast(snapshot())
        restore(h.undo.removeLast())
    }

    fun redo() {
        val h = history() ?: return
        if (gesture != null || h.redo.isEmpty()) return
        h.undo.addLast(snapshot())
        restore(h.redo.removeLast())
    }

    /** Removes all ink and images from the page. Undoable. */
    fun clearPage() {
        if (_strokes.isEmpty() && _images.isEmpty()) return
        pushUndo(snapshot())
        _strokes.clear()
        _images.clear()
        clearSelection()
        invalidateCache()
        notifyChanged()
    }

    fun addImage(img: PlacedImage) {
        if (page == null) return
        pushUndo(snapshot())
        _images.add(img)
        // New pictures start selected, so they can be moved, resized and rotated right away.
        select(img.id)
        invalidateCache()
        notifyChanged()
    }

    /** Turns the selected picture by [degrees] (e.g. ±90). Undoable. */
    fun rotateSelected(degrees: Float) {
        val sel = selectedImage() ?: return
        pushUndo(snapshot())
        replaceImage(sel.copy(rotation = ImageGeometry.normalizeRotation(sel.rotation + degrees, 0f)))
        invalidate()
        notifyChanged()
    }

    /** Puts the selected picture on top of the others. */
    fun bringSelectedToFront() {
        val sel = selectedImage() ?: return
        if (_images.lastOrNull()?.id == sel.id) return
        pushUndo(snapshot())
        _images.remove(sel)
        _images.add(sel)
        notifyChanged()
    }

    fun deselect() = clearSelection()

    fun deleteSelectedImage() {
        val sel = selectedImage() ?: return
        pushUndo(snapshot())
        _images.remove(sel)
        clearSelection()
        invalidateCache()
        notifyChanged()
    }

    /** Back to the default zoom, scrolled to the top. */
    fun fitPage() {
        pendingView = null
        fitted = false
        fitIfNeeded()
        invalidateCache()
        reportView()
    }

    /** Call when an image or PDF page finished loading. */
    fun onAssetsLoaded() = invalidateCache()

    fun toPageX(viewX: Float) = (viewX - offX) / scale
    fun toPageY(viewY: Float) = (viewY - offY) / scale

    // ================================================================ input

    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastEventTime = event.eventTime
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)
            MotionEvent.ACTION_MOVE -> {
                moveGesture(event)
                moveNav(event)
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> onPointerUp(event)
            MotionEvent.ACTION_CANCEL -> {
                val g = gesture
                // Only cancel the pen's own stream. On Android 15+ another device's
                // cancel can arrive while the pen is still down.
                if (g != null && event.deviceId == g.deviceId) cancelGesture()
                if (navigating && event.deviceId == navDeviceId) endNav(released = false)
            }
        }
        // Always consume, otherwise we stop getting pointers for the rest of the gesture.
        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val t = event.getToolType(0)
        if (t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER) lastPenHover = event.eventTime
        return super.onHoverEvent(event)
    }

    private fun onPointerDown(e: MotionEvent) {
        if (page == null) return
        val i = e.actionIndex
        val toolType = e.getToolType(i)
        val verdict = filter?.classify(toolType, e.deviceId) ?: Verdict.REJECT_TOOL
        val isPen = verdict == Verdict.ACCEPT_INK || verdict == Verdict.ACCEPT_ERASER

        val g = gesture
        if (g != null) {
            when {
                // The hand landing as you start to write: the pen wins.
                g.isFinger && isPen -> cancelGesture()
                // A second finger while one finger draws: it's a pinch, not a stroke.
                g.isFinger && toolType == MotionEvent.TOOL_TYPE_FINGER && e.deviceId == g.deviceId -> {
                    val first = e.findPointerIndex(g.pointerId)
                    cancelGesture()
                    if (first >= 0) {
                        addNavPointer(e, first)
                        addNavPointer(e, i)
                    }
                    return
                }
                // Anything else while drawing (palm under the pen) is ignored.
                else -> return
            }
        }

        when (verdict) {
            Verdict.ACCEPT_INK, Verdict.ACCEPT_ERASER -> {
                endNav(released = false)
                var erase = false
                var highlight = false
                if (verdict == Verdict.ACCEPT_ERASER) {
                    when (buttons.eraserEnd) {
                        EraserEnd.ERASE -> erase = true
                        EraserEnd.PEN -> Unit
                        EraserEnd.NOTHING -> return
                    }
                }
                val bs = e.buttonState
                val action = when {
                    bs and MotionEvent.BUTTON_STYLUS_PRIMARY != 0 -> buttons.primary
                    bs and MotionEvent.BUTTON_STYLUS_SECONDARY != 0 -> buttons.secondary
                    else -> PenAction.NOTHING
                }
                when (action) {
                    PenAction.TOGGLE_ERASER, PenAction.HOLD_ERASER -> erase = true
                    PenAction.TOGGLE_HIGHLIGHTER -> highlight = true
                    else -> Unit
                }
                startGesture(e, i, forceErase = erase, isFinger = false, forceHighlighter = highlight)
            }
            Verdict.ACCEPT_FINGER ->
                if (navigating) addNavPointer(e, i) else startGesture(e, i, forceErase = false, isFinger = true)
            Verdict.REJECT_TOOL ->
                if (toolType == MotionEvent.TOOL_TYPE_FINGER && canNavigate(e, i)) addNavPointer(e, i)
            else -> listener?.onRejected(verdict)
        }
    }

    private fun startGesture(e: MotionEvent, i: Int, forceErase: Boolean, isFinger: Boolean, forceHighlighter: Boolean = false) {
        if (page == null) return
        parent?.requestDisallowInterceptTouchEvent(true)
        requestUnbufferedDispatch(e)
        val x = toPageX(e.getX(i))
        val y = toPageY(e.getY(i))
        val t = tools

        var mode = when {
            forceErase || t.tool == Tool.ERASER -> Mode.ERASE
            t.tool == Tool.SELECT -> Mode.MOVE
            else -> Mode.INK
        }
        var image: PlacedImage? = null
        var ref = 0f
        if (forceHighlighter && mode == Mode.MOVE) mode = Mode.INK

        // A selected picture takes the touch whatever the tool: handles rotate and resize,
        // inside moves it, and a touch anywhere else just deselects it.
        val sel = selectedImage()
        if (sel != null && !forceErase) {
            val hit = handleHitPx / scale
            val rh = ImageGeometry.rotateHandle(sel, rotateGapPx / scale)
            val sh = ImageGeometry.resizeHandle(sel)
            when {
                hypot(x - rh[0], y - rh[1]) <= hit -> {
                    mode = Mode.ROTATE
                    ref = sel.rotation - ImageGeometry.angleTo(sel, x, y)
                }
                hypot(x - sh[0], y - sh[1]) <= hit -> {
                    mode = Mode.RESIZE
                    ref = ImageGeometry.distanceToCenter(sel, x, y).coerceAtLeast(1f)
                }
                ImageGeometry.contains(sel, x, y) -> mode = Mode.MOVE
                else -> {
                    clearSelection()
                    if (t.tool == Tool.SELECT) {
                        val other = _images.lastOrNull { ImageGeometry.contains(it, x, y) } ?: return
                        select(other.id)
                        image = other
                        mode = Mode.MOVE
                    } else {
                        return
                    }
                }
            }
            if (image == null) image = sel
        } else if (mode == Mode.MOVE) {
            val hit = _images.lastOrNull { ImageGeometry.contains(it, x, y) }
            if (hit == null) return
            select(hit.id)
            image = hit
        }

        val highlighter = mode == Mode.INK && (forceHighlighter || t.tool == Tool.HIGHLIGHTER)
        val g = Gesture(
            pointerId = e.getPointerId(i),
            deviceId = e.deviceId,
            isFinger = isFinger,
            mode = mode,
            strokeTool = if (highlighter) StrokeTool.HIGHLIGHTER else StrokeTool.PEN,
            color = if (highlighter) t.highlighterColor else t.penColor,
            width = if (highlighter) t.highlighterWidth else t.penWidth,
            before = if (mode == Mode.INK) null else snapshot(),
            startX = x,
            startY = y,
            image = image,
            ref = ref,
        )
        gesture = g
        addSample(g, e.getX(i), e.getY(i), pressureOf(g, e.getPressure(i)), e.eventTime)
        invalidate()
    }

    private fun moveGesture(e: MotionEvent) {
        val g = gesture ?: return
        if (e.deviceId != g.deviceId) return
        val i = e.findPointerIndex(g.pointerId)
        if (i < 0) return
        // Batched samples between frames; using them keeps fast strokes smooth.
        for (h in 0 until e.historySize) {
            addSample(
                g,
                e.getHistoricalX(i, h),
                e.getHistoricalY(i, h),
                pressureOf(g, e.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, i, h)),
                e.getHistoricalEventTime(h),
            )
        }
        addSample(g, e.getX(i), e.getY(i), pressureOf(g, e.getAxisValue(MotionEvent.AXIS_PRESSURE, i)), e.eventTime)
        invalidate()
    }

    private fun onPointerUp(e: MotionEvent) {
        val i = e.actionIndex
        val pid = e.getPointerId(i)
        val g = gesture
        if (g != null && e.deviceId == g.deviceId && pid == g.pointerId) {
            if (e.flags and MotionEvent.FLAG_CANCELED != 0) {
                // The system decided this touch was accidental.
                cancelGesture()
            } else {
                addSample(g, e.getX(i), e.getY(i), pressureOf(g, e.getPressure(i)), e.eventTime)
                finishGesture()
            }
        }
        if (navigating && e.deviceId == navDeviceId) {
            navPointers.remove(pid)
            if (navPointers.isEmpty() || e.actionMasked == MotionEvent.ACTION_UP) endNav(released = true) else resetNavBaseline()
        }
    }

    /** Fingers report meaningless pressure, so they get a steady mid width. */
    private fun pressureOf(g: Gesture, raw: Float) = if (g.isFinger) 0.5f else raw

    private fun addSample(g: Gesture, vx: Float, vy: Float, pressure: Float, time: Long) {
        val x = toPageX(vx)
        val y = toPageY(vy)
        g.lastX = x
        g.lastY = y
        when (g.mode) {
            Mode.INK -> g.points.add(StrokePoint(x, y, pressure.coerceIn(0f, 1f), time))
            Mode.ERASE -> {
                val r = eraserRadiusPx / scale
                if (_strokes.removeAll { StrokeGeometry.hits(it, x, y, r) }) {
                    g.changed = true
                    invalidateCache()
                }
            }
            Mode.MOVE -> {
                val s = g.image ?: return
                replaceImage(s.copy(x = s.x + x - g.startX, y = s.y + y - g.startY))
                g.changed = true
            }
            Mode.RESIZE -> {
                val s = g.image ?: return
                replaceImage(ImageGeometry.scaled(s, ImageGeometry.distanceToCenter(s, x, y) / g.ref))
                g.changed = true
            }
            Mode.ROTATE -> {
                val s = g.image ?: return
                replaceImage(s.copy(rotation = ImageGeometry.normalizeRotation(ImageGeometry.angleTo(s, x, y) + g.ref)))
                g.changed = true
            }
        }
    }

    private fun finishGesture() {
        val g = gesture ?: return
        gesture = null
        if (g.mode == Mode.INK) {
            if (g.points.isNotEmpty()) {
                pushUndo(snapshot())
                val s = Stroke(nextStrokeId++, g.color, g.width, g.points.toList(), g.strokeTool)
                _strokes.add(s)
                val c = cacheCanvas
                if (s.tool == StrokeTool.PEN && c != null && cacheMatchesView()) {
                    c.save()
                    c.translate(offX, offY)
                    c.scale(scale, scale)
                    StrokeRenderer.draw(c, s, inkPaint)
                    c.restore()
                } else {
                    // Highlighter goes under the ink, so redraw everything.
                    invalidateCache()
                }
                notifyChanged()
            }
        } else if (g.changed) {
            g.before?.let { pushUndo(it) }
            notifyChanged()
        }
        invalidate()
    }

    /** Drops the gesture in progress. Ink was never committed; erasing and moving are rolled back. */
    private fun cancelGesture() {
        val g = gesture ?: return
        gesture = null
        val before = g.before
        if (g.mode != Mode.INK && g.changed && before != null) {
            _strokes.clear()
            _strokes.addAll(before.strokes)
            _images.clear()
            _images.addAll(before.images)
            invalidateCache()
        }
        invalidate()
    }

    // ---------------------------------------------------------------- scroll & zoom

    private fun canNavigate(e: MotionEvent, i: Int): Boolean {
        if (gesture != null) return false
        if (e.eventTime - lastPenHover < 600) return false // pen hovering: that's a palm
        if (e.getTouchMajor(i) > palmPx) return false
        return !navigating || e.deviceId == navDeviceId
    }

    private fun addNavPointer(e: MotionEvent, i: Int) {
        if (!navigating) {
            navigating = true
            navDeviceId = e.deviceId
            navPointers.clear()
            navStart = e.eventTime
            navMoved = 0f
            navMaxPointers = 0
        }
        if (e.deviceId != navDeviceId) return
        navPointers[e.getPointerId(i)] = PointF(e.getX(i), e.getY(i))
        navMaxPointers = max(navMaxPointers, navPointers.size)
        resetNavBaseline()
    }

    private fun focusAndSpan(): FloatArray {
        var sx = 0f
        var sy = 0f
        for (p in navPointers.values) {
            sx += p.x
            sy += p.y
        }
        val n = navPointers.size.coerceAtLeast(1)
        var span = 0f
        if (navPointers.size >= 2) {
            val iter = navPointers.values.iterator()
            val a = iter.next()
            val b = iter.next()
            span = hypot(a.x - b.x, a.y - b.y)
        }
        return floatArrayOf(sx / n, sy / n, span)
    }

    private fun resetNavBaseline() {
        val f = focusAndSpan()
        lastFocusX = f[0]
        lastFocusY = f[1]
        lastSpan = f[2]
    }

    private fun moveNav(e: MotionEvent) {
        if (!navigating || e.deviceId != navDeviceId || navPointers.isEmpty()) return
        for ((pid, pt) in navPointers) {
            val idx = e.findPointerIndex(pid)
            if (idx >= 0) pt.set(e.getX(idx), e.getY(idx))
        }
        val f = focusAndSpan()
        navMoved += abs(f[0] - lastFocusX) + abs(f[1] - lastFocusY) + (if (lastSpan > 0f) abs(f[2] - lastSpan) else 0f)
        if (navPointers.size >= 2 && lastSpan > 10f && f[2] > 10f) zoomAround(lastFocusX, lastFocusY, f[2] / lastSpan)
        offX += f[0] - lastFocusX
        offY += f[1] - lastFocusY
        clamp()
        lastFocusX = f[0]
        lastFocusY = f[1]
        lastSpan = f[2]
        invalidate()
    }

    private fun zoomAround(fx: Float, fy: Float, factor: Float) {
        val base = fitWidthScale()
        val next = (scale * factor).coerceIn(min(base, fitPageScale()) * 0.6f, base * 6f)
        val k = next / scale
        offX = fx - (fx - offX) * k
        offY = fy - (fy - offY) * k
        scale = next
    }

    /** [released]: the fingers lifted (as opposed to the pen taking over or a cancel). */
    private fun endNav(released: Boolean) {
        if (!navigating) return
        navigating = false
        navPointers.clear()
        navDeviceId = -1
        clamp()
        if (!cacheMatchesView()) invalidateCache()
        if (released && navMaxPointers == 2 && lastEventTime - navStart < 300 && navMoved < 24f * density) {
            // A two-finger tap. Two of them close together is a double tap.
            if (lastEventTime - lastTwoFingerTap < 450) {
                lastTwoFingerTap = -10_000L
                listener?.onTwoFingerDoubleTap()
            } else {
                lastTwoFingerTap = lastEventTime
            }
        } else {
            reportView()
        }
    }

    private fun reportView() {
        val p = page ?: return
        val v = currentView() ?: return
        listener?.onViewChanged(p.id, v)
    }

    private fun fitWidthScale(): Float {
        val p = page ?: return 1f
        return ((width - 2 * pad) / p.width).coerceAtLeast(0.05f)
    }

    private fun fitPageScale(): Float {
        val p = page ?: return 1f
        return min((width - 2 * pad) / p.width, (height - 2 * pad) / p.height).coerceAtLeast(0.05f)
    }

    /** Scale for the default zoom setting. "Actual" shows the paper at its real size. */
    private fun defaultScale(): Float = when (zoomMode) {
        ZoomMode.FIT_WIDTH -> fitWidthScale()
        ZoomMode.FIT_PAGE -> fitPageScale()
        ZoomMode.ACTUAL -> resources.displayMetrics.xdpi / 72f
    }

    private fun fitIfNeeded() {
        if (page == null || fitted || width <= 0 || height <= 0) return
        val saved = pendingView
        pendingView = null
        if (saved != null) {
            scale = (fitWidthScale() * saved.zoom).coerceAtLeast(0.05f)
            offX = -saved.x * scale
            offY = -saved.y * scale
        } else {
            scale = defaultScale()
            offX = pad
            offY = pad
        }
        fitted = true
        clamp()
    }

    /** Keeps the page on screen; centres it on an axis where it's smaller than the view. */
    private fun clamp() {
        val p = page ?: return
        val cw = p.width * scale
        val ch = p.height * scale
        offX = if (cw + 2 * pad <= width) (width - cw) / 2 else offX.coerceIn(width - cw - pad, pad)
        offY = if (ch + 2 * pad <= height) pad else offY.coerceIn(height - ch - pad, pad)
    }

    // ---------------------------------------------------------------- drawing

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitted = false
        fitIfNeeded()
        invalidateCache()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Fill only our own bounds. drawColor() fills the whole clip, and inside Compose
        // that can be the entire window, which painted over the toolbar.
        canvas.save()
        canvas.clipRect(0, 0, width, height)
        deskPaint.color = deskColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), deskPaint)
        if (page == null) {
            canvas.restore()
            return
        }
        fitIfNeeded()
        if (!cacheValid) rebuildCache()
        val bmp = cache
        if (bmp != null && cacheValid) {
            if (cacheMatchesView()) {
                canvas.drawBitmap(bmp, 0f, 0f, null)
            } else {
                // Mid scroll/zoom: stretch the last frame; it's redrawn sharp when the gesture ends.
                val k = scale / cacheScale
                canvas.save()
                canvas.translate(offX - cacheOffX * k, offY - cacheOffY * k)
                canvas.scale(k, k)
                canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
                canvas.restore()
            }
        }

        canvas.save()
        canvas.translate(offX, offY)
        canvas.scale(scale, scale)
        selectedImage()?.let { sel ->
            canvas.save()
            canvas.rotate(sel.rotation, ImageGeometry.centerX(sel), ImageGeometry.centerY(sel))
            assets?.image(sel.assetId)?.let {
                canvas.drawBitmap(it, null, RectF(sel.x, sel.y, sel.x + sel.w, sel.y + sel.h), bitmapPaint)
            }
            selectPaint.strokeWidth = 1.5f * density / scale
            selectPaint.pathEffect = DashPathEffect(floatArrayOf(6f * density / scale, 4f * density / scale), 0f)
            canvas.drawRect(sel.x, sel.y, sel.x + sel.w, sel.y + sel.h, selectPaint)
            // Rotate handle above the top edge, joined by a short line.
            val cx = ImageGeometry.centerX(sel)
            val gap = rotateGapPx / scale
            selectPaint.pathEffect = null
            canvas.drawLine(cx, sel.y, cx, sel.y - gap, selectPaint)
            canvas.drawCircle(cx, sel.y - gap, handleRadiusPx / scale, handlePaint)
            canvas.drawCircle(cx, sel.y - gap, handleRadiusPx * 0.45f / scale, handleDotPaint)
            // Resize handle on the bottom-right corner.
            canvas.drawCircle(sel.x + sel.w, sel.y + sel.h, handleRadiusPx / scale, handlePaint)
            canvas.restore()
        }
        val g = gesture
        if (g != null && g.mode == Mode.INK) {
            StrokeRenderer.draw(canvas, g.points, g.color, g.width, g.strokeTool, inkPaint)
        }
        if (g != null && g.mode == Mode.ERASE) {
            eraserPaint.strokeWidth = density / scale
            canvas.drawCircle(g.lastX, g.lastY, eraserRadiusPx / scale, eraserPaint)
        }
        canvas.restore()
        canvas.restore() // bounds clip
    }

    private fun cacheMatchesView() = cacheValid && cacheScale == scale && cacheOffX == offX && cacheOffY == offY

    private fun rebuildCache() {
        val p = page ?: return
        if (width <= 0 || height <= 0) return
        var bmp = cache
        if (bmp == null || bmp.width != width || bmp.height != height) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            cache = bmp
            cacheCanvas = Canvas(bmp)
        }
        val target = bmp ?: return
        val c = cacheCanvas ?: return
        target.eraseColor(Color.TRANSPARENT)
        c.save()
        c.translate(offX, offY)
        c.scale(scale, scale)
        val px = 1f / scale
        c.drawRect(2f * density * px, 3f * density * px, p.width + 2f * density * px, p.height + 3f * density * px, shadowPaint)
        val pdf = p.pdf?.let { assets?.pdf(it, (p.width * scale).toInt()) }
        PageRenderer.drawPage(c, p, _images, _strokes, px, pdf, { id -> assets?.image(id) }, selectedId, inkPaint, bitmapPaint)
        c.restore()
        cacheScale = scale
        cacheOffX = offX
        cacheOffY = offY
        cacheValid = true
    }

    private fun invalidateCache() {
        cacheValid = false
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cache?.recycle()
        cache = null
        cacheCanvas = null
        cacheValid = false
    }

    // ---------------------------------------------------------------- helpers

    private fun history(): History? = page?.let { histories.getOrPut(it.id) { History() } }

    private fun snapshot() = Snapshot(ArrayList(_strokes), ArrayList(_images))

    private fun pushUndo(s: Snapshot) {
        val h = history() ?: return
        h.undo.addLast(s)
        if (h.undo.size > MAX_UNDO) h.undo.removeFirst()
        h.redo.clear()
    }

    private fun restore(s: Snapshot) {
        _strokes.clear()
        _strokes.addAll(s.strokes)
        _images.clear()
        _images.addAll(s.images)
        if (selectedImage() == null) clearSelection()
        invalidateCache()
        notifyChanged()
    }

    private fun selectedImage(): PlacedImage? =
        if (selectedId == -1L) null else _images.firstOrNull { it.id == selectedId }

    private fun select(id: Long) {
        selectedId = id
        listener?.onSelectionChanged(true)
        invalidateCache()
    }

    private fun clearSelection() {
        if (selectedId == -1L) return
        selectedId = -1L
        listener?.onSelectionChanged(false)
        invalidateCache()
    }

    private fun replaceImage(img: PlacedImage) {
        val i = _images.indexOfFirst { it.id == img.id }
        if (i >= 0) _images[i] = img
    }

    private fun notifyChanged() {
        val p = page ?: return
        listener?.onContentChanged(p.id, ArrayList(_strokes), ArrayList(_images))
        notifyHistory()
    }

    private fun notifyHistory() {
        val h = history()
        listener?.onHistoryChanged(h?.undo?.isNotEmpty() == true, h?.redo?.isNotEmpty() == true)
    }

    companion object {
        private const val MAX_UNDO = 100
    }
}
