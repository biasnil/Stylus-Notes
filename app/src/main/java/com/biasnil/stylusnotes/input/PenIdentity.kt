package com.biasnil.stylusnotes.input

import kotlinx.serialization.Serializable

/**
 * Everything we know about a registered pen.
 *
 * deviceId is the fast check, but Android hands out device IDs per boot / per
 * connection, so it can change. descriptor is the value Android promises to keep
 * stable across reboots, and vendor/product/name is the last-resort fallback.
 */
@Serializable
data class PenIdentity(
    val deviceId: Int,
    val descriptor: String,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    /** Your own name for it, e.g. "Pen 1". Lets you tell two identical pens apart. */
    val label: String = "",
)

/** Looks up live info for an input device ID. Swappable so tests can fake it. */
fun interface DeviceLookup {
    fun identityOf(deviceId: Int): PenIdentity?
}
