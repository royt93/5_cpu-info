package com.galaxyjoy.cpuinfo.feat.usbbt

import android.hardware.usb.UsbManager
import javax.inject.Inject

/**
 * Lists USB devices currently attached via [UsbManager.getDeviceList] — enumeration needs no
 * runtime permission (only claiming an interface to actually talk to a device does), so this
 * works with zero permission changes.
 */
class UsbInspectorProvider @Inject constructor(
    private val usbManager: UsbManager,
) {

    data class UsbDeviceInfo(
        val deviceName: String,
        val vendorId: Int,
        val productId: Int,
        val deviceClass: Int,
        val interfaceCount: Int,
        val manufacturerName: String?,
        val productName: String?,
    )

    fun listAttachedDevices(): List<UsbDeviceInfo> =
        usbManager.deviceList.values.map { device ->
            UsbDeviceInfo(
                deviceName = device.deviceName,
                vendorId = device.vendorId,
                productId = device.productId,
                deviceClass = device.deviceClass,
                interfaceCount = device.interfaceCount,
                manufacturerName = device.manufacturerName,
                productName = device.productName,
            )
        }
}
