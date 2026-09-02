package com.galaxyjoy.cpuinfo.feat.devicecard

import org.junit.Test
import kotlin.test.assertEquals

class DeviceCardDataTest {

    @Test
    fun `formatGb converts bytes to a one-decimal GB string with a dot regardless of locale`() {
        val eightGb = 8L * 1024 * 1024 * 1024
        assertEquals("8.0 GB", DeviceCardData.formatGb(eightGb))
    }

    @Test
    fun `formatGb rounds to one decimal place`() {
        val bytes = (1.64 * 1024 * 1024 * 1024).toLong()
        assertEquals("1.6 GB", DeviceCardData.formatGb(bytes))
    }

    @Test
    fun `formatGb handles zero`() {
        assertEquals("0.0 GB", DeviceCardData.formatGb(0L))
    }
}
