package com.biasnil.stylusnotes.input

import android.view.KeyEvent

/**
 * Pen side buttons sent as key events. Lenovo pens often use key codes 600..604
 * instead of stylus button states. Which code is which button varies by pen, so the
 * secondary button can be assigned in Settings → Input methods.
 */
object PenKeys {
    val LENOVO_RANGE = 600..604

    val EXTRA_CODES = setOf(
        KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL,
    )

    fun isPenButton(keyCode: Int): Boolean = keyCode in LENOVO_RANGE || keyCode in EXTRA_CODES

    /**
     * True if this key is the secondary button: either the one assigned in settings,
     * or Android's standard secondary stylus key. Every other pen key counts as primary.
     */
    fun isSecondary(keyCode: Int, assignedSecondary: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY || (assignedSecondary != -1 && keyCode == assignedSecondary)
}
