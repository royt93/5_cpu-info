package com.galaxyjoy.cpuinfo.feat.usbbt

/**
 * F03 "USB/BT Inspector" — human-readable labels for USB base device class codes, per the
 * USB-IF "Defined Class Codes" table (https://www.usb.org/defined-class-codes). Values are
 * assigned by the USB-IF and have been stable for decades, matching the same hardcoded
 * reference-table approach as [ChipCatalog][com.galaxyjoy.cpuinfo.feat.truth.ChipCatalog].
 */
object UsbDeviceClassCatalog {

    private val CLASS_NAMES: Map<Int, String> = mapOf(
        0x00 to "Defined at interface level",
        0x01 to "Audio",
        0x02 to "Communications (CDC)",
        0x03 to "Human Interface Device (HID)",
        0x05 to "Physical",
        0x06 to "Image",
        0x07 to "Printer",
        0x08 to "Mass Storage",
        0x09 to "Hub",
        0x0A to "CDC-Data",
        0x0B to "Smart Card",
        0x0D to "Content Security",
        0x0E to "Video",
        0x0F to "Personal Healthcare",
        0x10 to "Audio/Video",
        0x11 to "Billboard",
        0x12 to "USB Type-C Bridge",
        0xDC to "Diagnostic",
        0xE0 to "Wireless Controller",
        0xEF to "Miscellaneous",
        0xFE to "Application Specific",
        0xFF to "Vendor Specific",
    )

    fun labelFor(deviceClass: Int): String = CLASS_NAMES[deviceClass] ?: "Unknown (0x%02X)".format(deviceClass)
}
