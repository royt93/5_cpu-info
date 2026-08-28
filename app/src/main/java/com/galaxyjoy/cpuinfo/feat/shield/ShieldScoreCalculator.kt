package com.galaxyjoy.cpuinfo.feat.shield

import android.os.BatteryManager

/**
 * Pure scoring logic (U10) — no Android I/O, takes already-read primitives so it's directly
 * unit-testable. [ShieldScoreProvider] is the thin wrapper that actually reads RAM/storage/
 * battery state and calls this.
 */
object ShieldScoreCalculator {

    data class Result(
        val overall: Int,
        val ramScore: Int,
        val storageScore: Int,
        val batteryScore: Int,
    )

    fun compute(
        ramAvailablePercent: Int,
        storageFreePercent: Int,
        batteryLevelPercent: Int,
        batteryHealth: Int,
    ): Result {
        val ramScore = ramAvailablePercent.coerceIn(0, 100)
        val storageScore = storageFreePercent.coerceIn(0, 100)
        val batteryScore = batteryScore(batteryLevelPercent, batteryHealth)
        val overall = ((ramScore + storageScore + batteryScore) / 3.0).toInt().coerceIn(0, 100)
        return Result(overall, ramScore, storageScore, batteryScore)
    }

    private fun batteryScore(levelPercent: Int, health: Int): Int {
        val healthMultiplier = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> 1.0
            BatteryManager.BATTERY_HEALTH_COLD -> 0.85
            BatteryManager.BATTERY_HEALTH_OVERHEAT,
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE,
            -> 0.7
            BatteryManager.BATTERY_HEALTH_DEAD,
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE,
            -> 0.4
            else -> 0.85 // UNKNOWN — device didn't report health, don't penalize hard
        }
        return (levelPercent.coerceIn(0, 100) * healthMultiplier).toInt().coerceIn(0, 100)
    }
}
