package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.input.DeviceLookup
import com.biasnil.stylusnotes.input.InputMode
import com.biasnil.stylusnotes.input.PenIdentity
import com.biasnil.stylusnotes.input.PenPolicy
import com.biasnil.stylusnotes.input.StylusFilter
import com.biasnil.stylusnotes.input.StylusFilter.Companion.TOOL_TYPE_ERASER
import com.biasnil.stylusnotes.input.StylusFilter.Companion.TOOL_TYPE_FINGER
import com.biasnil.stylusnotes.input.StylusFilter.Companion.TOOL_TYPE_MOUSE
import com.biasnil.stylusnotes.input.StylusFilter.Companion.TOOL_TYPE_STYLUS
import com.biasnil.stylusnotes.input.StylusFilter.Companion.TOOL_TYPE_UNKNOWN
import com.biasnil.stylusnotes.input.StylusFilter.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StylusFilterTest {

    private val lenovoPen = PenIdentity(12, "desc-lenovo", "Lenovo Precision Pen", 0x17EF, 0x6161)
    private val otherPen = PenIdentity(13, "desc-other", "Other Pen", 0x1234, 0x0001)
    private val strangerPen = PenIdentity(14, "desc-stranger", "Stranger Pen", 0x9999, 0x0002)

    /** Fake device table, editable per test. */
    private val devices = HashMap<Int, PenIdentity>()
    private lateinit var filter: StylusFilter

    @Before
    fun setUp() {
        devices.clear()
        devices[12] = lenovoPen
        devices[13] = otherPen
        devices[14] = strangerPen
        filter = StylusFilter(DeviceLookup { devices[it] })
        filter.registeredPens = listOf(lenovoPen)
    }

    // ------------------------------------------------------------ pen only (default)

    @Test fun fingerIsRejected() =
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_FINGER, 12))

    @Test fun mouseIsRejected() =
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_MOUSE, 12))

    @Test fun unknownToolIsRejected() =
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_UNKNOWN, 12))

    @Test fun registeredPenDraws() =
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))

    @Test fun registeredPenEraserEndErases() =
        assertEquals(Verdict.ACCEPT_ERASER, filter.classify(TOOL_TYPE_ERASER, 12))

    @Test fun secondStylusIsRejected() =
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 13))

    @Test fun secondStylusEraserIsRejected() =
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_ERASER, 13))

    @Test fun nothingRegistered() {
        filter.registeredPens = emptyList()
        assertEquals(Verdict.NO_PEN_REGISTERED, filter.classify(TOOL_TYPE_STYLUS, 12))
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_FINGER, 12))
    }

    // ------------------------------------------------------------ device matching

    @Test fun penReconnectsWithNewDeviceIdStillAccepted() {
        devices.remove(12)
        devices[27] = lenovoPen.copy(deviceId = 27)
        filter.invalidateDevices()
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 27))
    }

    @Test fun oldDeviceIdReusedByAnotherDeviceIsRejected() {
        devices[12] = otherPen.copy(deviceId = 12)
        filter.invalidateDevices()
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 12))
    }

    @Test fun vendorProductFallbackWhenNoDescriptor() {
        val noDesc = lenovoPen.copy(descriptor = "")
        filter.registeredPens = listOf(noDesc)
        devices[40] = noDesc.copy(deviceId = 40)
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 40))
    }

    @Test fun unknownDeviceFallsBackToIdEquality() {
        devices.clear()
        filter.invalidateDevices()
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 99))
    }

    @Test fun changingRegisteredPensClearsCache() {
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))
        filter.registeredPens = listOf(otherPen)
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 12))
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 13))
    }

    // ------------------------------------------------------------ multiple pens

    @Test fun multipleRegisteredPensAllDraw() {
        filter.registeredPens = listOf(lenovoPen, otherPen)
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 13))
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 14))
    }

    @Test fun anyStylusPolicyAcceptsUnregisteredPen() {
        filter.penPolicy = PenPolicy.ANY_STYLUS
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 14))
        assertEquals(Verdict.ACCEPT_ERASER, filter.classify(TOOL_TYPE_ERASER, 14))
    }

    @Test fun anyStylusPolicyWorksWithNoPensRegistered() {
        filter.registeredPens = emptyList()
        filter.penPolicy = PenPolicy.ANY_STYLUS
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 99))
    }

    @Test fun anyStylusStillBlocksFingersInPenOnly() {
        filter.penPolicy = PenPolicy.ANY_STYLUS
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_FINGER, 12))
    }

    @Test fun samePenComparesDescriptorFirst() {
        assertTrue(StylusFilter.samePen(lenovoPen, lenovoPen.copy(deviceId = 50)))
        assertFalse(StylusFilter.samePen(lenovoPen, otherPen.copy(deviceId = 12)))
    }

    // ------------------------------------------------------------ pen + finger

    @Test fun penAndFingerModeAcceptsFingers() {
        filter.inputMode = InputMode.PEN_AND_FINGER
        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))
        // Fingers keep working after the pen is used.
        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
    }

    @Test fun penAndFingerModeStillRejectsMouseAndStrangerPen() {
        filter.inputMode = InputMode.PEN_AND_FINGER
        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_MOUSE, 7))
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 14))
    }

    @Test fun fingersWorkWithNoPenRegisteredInPenAndFinger() {
        filter.registeredPens = emptyList()
        filter.inputMode = InputMode.PEN_AND_FINGER
        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
    }

    // ------------------------------------------------------------ auto

    @Test fun autoModeFingerUntilPenThenPenOnly() {
        var notified: Boolean? = null
        filter.onPenSeenChanged = { notified = it }
        filter.inputMode = InputMode.AUTO

        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
        assertFalse(filter.penSeen)

        assertEquals(Verdict.ACCEPT_INK, filter.classify(TOOL_TYPE_STYLUS, 12))
        assertTrue(filter.penSeen)
        assertEquals(true, notified)

        assertEquals(Verdict.REJECT_TOOL, filter.classify(TOOL_TYPE_FINGER, 7))
    }

    @Test fun autoModeUnregisteredPenDoesNotTurnFingersOff() {
        filter.inputMode = InputMode.AUTO
        assertEquals(Verdict.REJECT_DEVICE, filter.classify(TOOL_TYPE_STYLUS, 14))
        assertFalse(filter.penSeen)
        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
    }

    @Test fun autoModeEraserEndAlsoCountsAsPen() {
        filter.inputMode = InputMode.AUTO
        filter.classify(TOOL_TYPE_ERASER, 12)
        assertTrue(filter.penSeen)
    }

    @Test fun autoModeResetTurnsFingersBackOn() {
        filter.inputMode = InputMode.AUTO
        filter.classify(TOOL_TYPE_STYLUS, 12)
        filter.resetPenSeen()
        assertEquals(Verdict.ACCEPT_FINGER, filter.classify(TOOL_TYPE_FINGER, 7))
    }

    @Test fun changingModeResetsPenSeen() {
        filter.inputMode = InputMode.AUTO
        filter.classify(TOOL_TYPE_STYLUS, 12)
        filter.inputMode = InputMode.PEN_ONLY
        filter.inputMode = InputMode.AUTO
        assertFalse(filter.penSeen)
    }

    @Test fun settingSameModeAgainKeepsPenSeen() {
        // DataStore can re-emit the same settings; that mustn't turn fingers back on.
        filter.inputMode = InputMode.AUTO
        filter.classify(TOOL_TYPE_STYLUS, 12)
        filter.inputMode = InputMode.AUTO
        assertTrue(filter.penSeen)
    }

    @Test fun penOnlyNeverSetsPenSeen() {
        filter.classify(TOOL_TYPE_STYLUS, 12)
        assertFalse(filter.penSeen)
    }
}
