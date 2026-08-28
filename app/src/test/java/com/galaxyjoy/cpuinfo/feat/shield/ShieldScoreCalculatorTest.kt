package com.galaxyjoy.cpuinfo.feat.shield

import android.os.BatteryManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShieldScoreCalculatorTest {

    @Test
    fun `perfect device scores 100 overall`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 100,
            storageFreePercent = 100,
            batteryLevelPercent = 100,
            batteryHealth = BatteryManager.BATTERY_HEALTH_GOOD,
        )
        assertEquals(100, result.overall)
        assertEquals(100, result.ramScore)
        assertEquals(100, result.storageScore)
        assertEquals(100, result.batteryScore)
    }

    @Test
    fun `worst device scores near 0`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 0,
            storageFreePercent = 0,
            batteryLevelPercent = 0,
            batteryHealth = BatteryManager.BATTERY_HEALTH_DEAD,
        )
        assertEquals(0, result.overall)
    }

    @Test
    fun `overheat battery penalizes battery score even at full charge`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 100,
            storageFreePercent = 100,
            batteryLevelPercent = 100,
            batteryHealth = BatteryManager.BATTERY_HEALTH_OVERHEAT,
        )
        assertEquals(70, result.batteryScore) // 100 * 0.7
        assertTrue(result.batteryScore < result.ramScore)
    }

    @Test
    fun `dead battery health penalizes hard regardless of charge level`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 100,
            storageFreePercent = 100,
            batteryLevelPercent = 100,
            batteryHealth = BatteryManager.BATTERY_HEALTH_DEAD,
        )
        assertEquals(40, result.batteryScore) // 100 * 0.4
    }

    @Test
    fun `unknown health does not crash and applies mild penalty`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 50,
            storageFreePercent = 50,
            batteryLevelPercent = 50,
            batteryHealth = BatteryManager.BATTERY_HEALTH_UNKNOWN,
        )
        assertEquals(42, result.batteryScore) // 50 * 0.85 = 42.5 -> 42
    }

    @Test
    fun `overall is the average of the three components`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 90,
            storageFreePercent = 60,
            batteryLevelPercent = 100,
            batteryHealth = BatteryManager.BATTERY_HEALTH_GOOD,
        )
        assertEquals(83, result.overall) // (90 + 60 + 100) / 3 = 83.33 -> 83
    }

    @Test
    fun `out-of-range inputs are clamped instead of producing invalid scores`() {
        val result = ShieldScoreCalculator.compute(
            ramAvailablePercent = 150,
            storageFreePercent = -20,
            batteryLevelPercent = 999,
            batteryHealth = BatteryManager.BATTERY_HEALTH_GOOD,
        )
        assertEquals(100, result.ramScore)
        assertEquals(0, result.storageScore)
        assertEquals(100, result.batteryScore)
    }
}
