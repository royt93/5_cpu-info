package com.galaxyjoy.cpuinfo.feat.shield

import android.os.BatteryManager
import android.os.Environment
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import javax.inject.Inject

/**
 * Reads current RAM/storage/battery state and hands off to the pure [ShieldScoreCalculator].
 * Kept thin on purpose — all scoring logic lives in the calculator so it stays unit-testable
 * without Android deps.
 */
class ShieldScoreProvider @Inject constructor(
    private val dataProviderRam: DataProviderRam,
    private val batteryStatusProvider: BatteryStatusProvider,
) {

    fun compute(): ShieldScoreCalculator.Result {
        val batteryIntent = batteryStatusProvider.getBatteryStatusIntent()
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevelPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        val batteryHealth = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN,
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        return ShieldScoreCalculator.compute(
            ramAvailablePercent = dataProviderRam.getAvailablePercentage(),
            storageFreePercent = internalStorageFreePercent(),
            batteryLevelPercent = batteryLevelPercent,
            batteryHealth = batteryHealth,
        )
    }

    @Suppress("DEPRECATION")
    private fun internalStorageFreePercent(): Int {
        val internal = Environment.getDataDirectory()
        val total = internal.totalSpace
        if (total <= 0) return 100
        return ((internal.usableSpace * 100) / total).toInt().coerceIn(0, 100)
    }
}
