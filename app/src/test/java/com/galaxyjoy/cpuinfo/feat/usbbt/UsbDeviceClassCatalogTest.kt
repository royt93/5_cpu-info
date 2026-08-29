package com.galaxyjoy.cpuinfo.feat.usbbt

import kotlin.test.assertEquals
import org.junit.Test

class UsbDeviceClassCatalogTest {

    @Test
    fun `mass storage class resolves to a readable label`() {
        assertEquals("Mass Storage", UsbDeviceClassCatalog.labelFor(0x08))
    }

    @Test
    fun `HID class resolves to a readable label`() {
        assertEquals("Human Interface Device (HID)", UsbDeviceClassCatalog.labelFor(0x03))
    }

    @Test
    fun `hub class resolves to a readable label`() {
        assertEquals("Hub", UsbDeviceClassCatalog.labelFor(0x09))
    }

    @Test
    fun `unknown class falls back to a hex-formatted placeholder`() {
        assertEquals("Unknown (0x7A)", UsbDeviceClassCatalog.labelFor(0x7A))
    }

    @Test
    fun `interface-defined class 0x00 resolves correctly`() {
        assertEquals("Defined at interface level", UsbDeviceClassCatalog.labelFor(0x00))
    }
}
