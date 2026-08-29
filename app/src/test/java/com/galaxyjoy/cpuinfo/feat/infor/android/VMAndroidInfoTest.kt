package com.galaxyjoy.cpuinfo.feat.infor.android

import org.junit.Assert.assertEquals
import org.junit.Test

class VMAndroidInfoTest {

    @Test
    fun `parseSelinuxStatus maps 1 to Enforcing`() {
        assertEquals("Enforcing", VMAndroidInfo.parseSelinuxStatus("1"))
    }

    @Test
    fun `parseSelinuxStatus maps 0 to Permissive`() {
        assertEquals("Permissive", VMAndroidInfo.parseSelinuxStatus("0"))
    }

    @Test
    fun `parseSelinuxStatus trims whitespace before matching`() {
        assertEquals("Enforcing", VMAndroidInfo.parseSelinuxStatus("1\n"))
        assertEquals("Permissive", VMAndroidInfo.parseSelinuxStatus(" 0 "))
    }

    @Test
    fun `parseSelinuxStatus falls back to Unknown for null or unexpected values`() {
        assertEquals("Unknown", VMAndroidInfo.parseSelinuxStatus(null))
        assertEquals("Unknown", VMAndroidInfo.parseSelinuxStatus(""))
        assertEquals("Unknown", VMAndroidInfo.parseSelinuxStatus("garbage"))
    }
}
