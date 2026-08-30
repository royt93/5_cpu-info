package com.galaxyjoy.cpuinfo.data.provider

import android.content.Intent
import android.os.BatteryManager
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderTemperatureTest {

    private val batteryStatusProvider: BatteryStatusProvider = mockk()
    private val provider = DataProviderTemperature(batteryStatusProvider)

    @Test
    fun `getBatteryTemperature converts tenths of a degree to Celsius`() {
        val intent: Intent = mockk {
            every { getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 389
        }
        every { batteryStatusProvider.getBatteryStatusIntent() } returns intent

        assertEquals(38.9f, provider.getBatteryTemperature())
    }

    @Test
    fun `getBatteryTemperature returns null when sticky intent is unavailable`() {
        every { batteryStatusProvider.getBatteryStatusIntent() } returns null

        assertNull(provider.getBatteryTemperature())
    }

    @Test
    fun `getBatteryTemperature returns null when reported raw value is exactly zero`() {
        val intent: Intent = mockk {
            every { getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 0
        }
        every { batteryStatusProvider.getBatteryStatusIntent() } returns intent

        assertNull(provider.getBatteryTemperature())
    }

    @Test
    fun `findCpuTempPath returns null when none of the known sysfs locations exist`() {
        // On the JVM test host none of the hardcoded sysfs paths exist — this is also the real
        // behavior on any device that doesn't expose CPU temperature, which must fail silently
        // rather than crash (see TemperatureData.Unavailable).
        assertNull(provider.findCpuTempPath())
    }
}
