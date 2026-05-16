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
}
