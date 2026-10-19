package com.biasnil.stylusnotes.input

import android.view.InputDevice

class AndroidDeviceLookup : DeviceLookup {
    override fun identityOf(deviceId: Int): PenIdentity? {
        val device = InputDevice.getDevice(deviceId) ?: return null
        return PenIdentity(
            deviceId = deviceId,
            descriptor = device.descriptor.orEmpty(),
            name = device.name.orEmpty(),
            vendorId = device.vendorId,
            productId = device.productId,
        )
    }
}
