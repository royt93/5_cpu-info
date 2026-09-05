package com.galaxyjoy.cpuinfo.data.provider

import android.content.Intent
import android.os.BatteryManager
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataProviderBatteryTest {
    private val manager = mockk<BatteryManager>(relaxed = true)
    private val statusProvider = mockk<BatteryStatusProvider>(relaxed = true)
    private val intent = mockk<Intent>(relaxed = true)
    private val provider = DataProviderBattery(manager, statusProvider)

    @Test fun `reads current int property and voltage from sticky snapshot`() {
        every { statusProvider.getBatteryStatusIntent() } returns intent
        every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING
        every { intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) } returns 4200
        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -1_250_000
        val data = provider.getBatteryData()
        assertEquals(1250.0, data.chargingCurrentMa)
        assertEquals(4200, data.voltageMv)
    }

    @Test fun `full discharging unknown and not charging hide charging row`() {
        every { statusProvider.getBatteryStatusIntent() } returns intent
        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns 1000
        for (status in listOf(BatteryManager.BATTERY_STATUS_FULL, BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING, BatteryManager.BATTERY_STATUS_UNKNOWN, -1)) {
            every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns status
            assertNull(provider.getBatteryData().chargingCurrentMa)
        }
    }

    @Test fun `independent API failures clear previous readings and recover next tick`() {
        every { statusProvider.getBatteryStatusIntent() } returns intent
        every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING
        every { intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) } returns 4100
        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } throws IllegalStateException()
        assertNull(provider.getBatteryData().chargingCurrentMa)
        assertEquals(4100, provider.getBatteryData().voltageMv)
        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns 2_000_000
        assertEquals(2000.0, provider.getBatteryData().chargingCurrentMa)
        every { statusProvider.getBatteryStatusIntent() } throws SecurityException()
        assertNull(provider.getBatteryData().voltageMv)
        assertNull(provider.getBatteryData().chargingCurrentMa)
    }

    @Test fun `temperature reads zero and sub-zero readings, not just positive ones`() {
        every { statusProvider.getBatteryStatusIntent() } returns intent
        every { intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE) } returns true
        every { intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns 0
        assertEquals(0f, provider.getBatteryData().temperature)
        every { intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) } returns -50
        assertEquals(-5f, provider.getBatteryData().temperature)
    }

    @Test fun `temperature is null when the extra is absent, not defaulted to zero`() {
        every { statusProvider.getBatteryStatusIntent() } returns intent
        every { intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE) } returns false
        assertNull(provider.getBatteryData().temperature)
    }

    @Test fun `missing broadcast and unsupported current are unavailable`() {
        every { statusProvider.getBatteryStatusIntent() } returns null
        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns Int.MIN_VALUE
        val data = provider.getBatteryData()
        assertNull(data.voltageMv)
        assertNull(data.chargingCurrentMa)
        assertNull(data.currentMa)
    }
}
