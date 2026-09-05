package com.galaxyjoy.cpuinfo.data.provider

import android.content.Context
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataProviderBatteryInstrumentedTest {
    @Test fun realBatteryCurrentApiAndStickyVoltage_doNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(BatteryManager::class.java)
        // Real API call, intentionally no fixed numeric expectation or charging assumption.
        val raw = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val current = BatteryReading.currentMa(raw)
        assertTrue(current == null || current.isFinite())

        val data = DataProviderBattery(manager, BatteryStatusProvider(context)).getBatteryData()
        assertTrue(data.chargingCurrentMa == null || data.chargingCurrentMa!! >= 0.0)
        assertTrue(data.voltageMv == null || data.voltageMv!! > 0)
    }
}
