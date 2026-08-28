package com.galaxyjoy.cpuinfo.data.provider

import android.app.ActivityManager
import android.content.pm.ConfigurationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import com.galaxyjoy.cpuinfo.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class DataProviderGpuTest {

    private val activityManager: ActivityManager = mockk()
    private val packageManager: PackageManager = mockk()
    private val resources: Resources = mockk {
        every { getString(R.string.unknown) } returns "unknown"
    }
    private val provider = DataProviderGpu(activityManager, packageManager, resources)

    @Test
    fun `getGlEsVersion returns value from ActivityManager configurationInfo`() {
        val configInfo: ConfigurationInfo = mockk {
            every { glEsVersion } returns "3.2"
        }
        every { activityManager.deviceConfigurationInfo } returns configInfo

        assertEquals("3.2", provider.getGlEsVersion())
    }

    @Test
    fun `getVulkanVersion returns unknown on JVM where SDK_INT defaults below 24`() {
        // Build.VERSION.SDK_INT defaults to 0 on JVM tests → method short-circuits to "unknown".
        // This regression-tests the API-gate, which is the most likely path to break on legacy devices.
        assertEquals("unknown", provider.getVulkanVersion())
    }

    @Test
    fun `decodeVulkanVersion extracts 1-3-250 correctly`() {
        // 1.3.250 packed per Android spec: major=1(bits22-31), minor=3(bits12-21), patch=250(bits0-11)
        val packed = (1 shl 22) or (3 shl 12) or 250
        assertEquals("1.3.250", DataProviderGpu.decodeVulkanVersion(packed))
    }

    @Test
    fun `decodeVulkanVersion does not sign-extend when minor high bit is set`() {
        // Regression for B04: old code used signed `shr`, which turned this into a huge
        // negative number whenever bit 21 (minor's high bit) was set.
        val minorWithHighBitSet = 512 // 0b1000000000, bit 9 of the 10-bit minor field
        val packed = (1 shl 22) or (minorWithHighBitSet shl 12) or 100
        assertEquals("1.512.100", DataProviderGpu.decodeVulkanVersion(packed))
    }

    @Test
    fun `decodeVulkanVersion does not sign-extend when patch high bit is set`() {
        val patchWithHighBitSet = 2048 // 0b100000000000, bit 11 of the 12-bit patch field
        val packed = (2 shl 22) or (0 shl 12) or patchWithHighBitSet
        assertEquals("2.0.2048", DataProviderGpu.decodeVulkanVersion(packed))
    }

    @Test
    fun `decodeVulkanVersion handles zero`() {
        assertEquals("0.0.0", DataProviderGpu.decodeVulkanVersion(0))
    }
}
