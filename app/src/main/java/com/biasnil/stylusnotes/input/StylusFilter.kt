package com.biasnil.stylusnotes.input

/** Which tools may draw. */
enum class InputMode {
    /** Only pens draw. Fingers and palms never do. */
    PEN_ONLY,
    /** Pens and fingers both draw. */
    PEN_AND_FINGER,
    /** Fingers draw until an allowed pen touches the screen, then it's pen only. */
    AUTO,
}

/** Which pens count as allowed. */
enum class PenPolicy {
    /** Only pens in the registered list. */
    REGISTERED_ONLY,
    /** Any stylus at all. */
    ANY_STYLUS,
}

/**
 * Decides whether a pointer is allowed to draw.
 *
 * Pure Kotlin on purpose (no android.* imports) so it runs in plain JVM unit tests.
 * The tool-type constants below are the same values as MotionEvent.TOOL_TYPE_*;
 * the instrumented test checks that they still match.
 */
class StylusFilter(private val lookup: DeviceLookup) {

    enum class Verdict {
        ACCEPT_INK,
        ACCEPT_ERASER,
        /** A finger, allowed because of the input mode. */
        ACCEPT_FINGER,
        /** Finger, palm, mouse or anything that isn't allowed by the input mode. */
        REJECT_TOOL,
        /** A stylus, but not one of the registered pens. */
        REJECT_DEVICE,
        /** A stylus touched but no pen has been registered yet. */
        NO_PEN_REGISTERED,
    }

    private val matchCache = HashMap<Int, Boolean>()

    var registeredPens: List<PenIdentity> = emptyList()
        set(value) {
            field = value
            matchCache.clear()
        }

    var penPolicy: PenPolicy = PenPolicy.REGISTERED_ONLY

    var inputMode: InputMode = InputMode.PEN_ONLY
        set(value) {
            if (field != value) {
                field = value
                setPenSeen(false)
            }
        }

    /** In AUTO mode: true once an allowed pen has touched, which turns fingers off. */
    var penSeen: Boolean = false
        private set

    var onPenSeenChanged: ((Boolean) -> Unit)? = null

    /** Turns fingers back on in AUTO mode. */
    fun resetPenSeen() = setPenSeen(false)

    /** Call when input devices are added/removed/changed so IDs get re-checked. */
    fun invalidateDevices() = matchCache.clear()

    fun classify(toolType: Int, deviceId: Int): Verdict {
        when (toolType) {
            TOOL_TYPE_FINGER -> return when (inputMode) {
                InputMode.PEN_ONLY -> Verdict.REJECT_TOOL
                InputMode.PEN_AND_FINGER -> Verdict.ACCEPT_FINGER
                InputMode.AUTO -> if (penSeen) Verdict.REJECT_TOOL else Verdict.ACCEPT_FINGER
            }
            TOOL_TYPE_STYLUS, TOOL_TYPE_ERASER -> Unit
            else -> return Verdict.REJECT_TOOL
        }

        if (penPolicy == PenPolicy.REGISTERED_ONLY) {
            if (registeredPens.isEmpty()) return Verdict.NO_PEN_REGISTERED
            if (!matchesAnyPen(deviceId)) return Verdict.REJECT_DEVICE
        }

        if (inputMode == InputMode.AUTO && !penSeen) setPenSeen(true)
        return if (toolType == TOOL_TYPE_ERASER) Verdict.ACCEPT_ERASER else Verdict.ACCEPT_INK
    }

    private fun setPenSeen(value: Boolean) {
        if (penSeen != value) {
            penSeen = value
            onPenSeenChanged?.invoke(value)
        }
    }

    private fun matchesAnyPen(deviceId: Int): Boolean = matchCache.getOrPut(deviceId) {
        val live = lookup.identityOf(deviceId)
        registeredPens.any { computeMatch(it, live, deviceId) }
    }

    private fun computeMatch(pen: PenIdentity, live: PenIdentity?, deviceId: Int): Boolean {
        if (live != null) {
            // Strongest signal: the persistent descriptor. If both sides have one, it decides.
            // This also catches the case where the old deviceId got reused by another device.
            if (pen.descriptor.isNotEmpty() && live.descriptor.isNotEmpty()) {
                return pen.descriptor == live.descriptor
            }
            // No descriptor: fall back to vendor/product/name.
            if (pen.vendorId != 0 || pen.productId != 0) {
                if (live.vendorId == pen.vendorId &&
                    live.productId == pen.productId &&
                    live.name == pen.name
                ) return true
            }
        }
        return deviceId == pen.deviceId
    }

    companion object {
        // Mirrors android.view.MotionEvent.TOOL_TYPE_*
        const val TOOL_TYPE_UNKNOWN = 0
        const val TOOL_TYPE_FINGER = 1
        const val TOOL_TYPE_STYLUS = 2
        const val TOOL_TYPE_MOUSE = 3
        const val TOOL_TYPE_ERASER = 4

        /** Same physical pen? Used to avoid registering one pen twice. */
        fun samePen(a: PenIdentity, b: PenIdentity): Boolean =
            if (a.descriptor.isNotEmpty() && b.descriptor.isNotEmpty()) a.descriptor == b.descriptor
            else a.deviceId == b.deviceId
    }
}
