package com.galaxyjoy.cpuinfo.data.provider

import android.os.BatteryManager
import android.os.Build
import com.galaxyjoy.cpuinfo.domain.model.BatteryData
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import javax.inject.Inject

class DataProviderBattery @Inject constructor(
    private val batteryManager: BatteryManager,
    private val batteryStatusProvider: BatteryStatusProvider,
) {
    fun getBatteryData(): BatteryData {
        // Reuse the null-receiver sticky query; no receiver is retained or needs unregistering.
        val status = readOrNull { batteryStatusProvider.getBatteryStatusIntent() }
        val activelyCharging = readOrNull {
            status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        } == BatteryManager.BATTERY_STATUS_CHARGING
        val current = readOrNull {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }
        return BatteryData(
            status = status,
            voltageMv = readOrNull {
                status?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.let(BatteryReading::voltageMv)
            },
            chargingCurrentMa = current?.let { BatteryReading.chargingCurrentMa(it, activelyCharging) },
            currentMa = current?.let(BatteryReading::currentMa),
            temperature = readOrNull {
                status?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }
                    ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    ?.div(10f)
            },
            designedCapacity = readOrNull { batteryStatusProvider.getBatteryCapacity() } ?: -1.0,
            chargeCounter = longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            energyCounter = longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
            cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                readOrNull { status?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 } }
            } else null,
        )
    }

    private fun longPropertyOrNull(id: Int): Long? = readOrNull {
        batteryManager.getLongProperty(id).takeIf { it != Long.MIN_VALUE }
    }

    private inline fun <T> readOrNull(read: () -> T): T? = try {
        read()
    } catch (_: Exception) {
        null
    }
}
