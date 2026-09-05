package com.galaxyjoy.cpuinfo.feat.infor.battery

import android.content.Intent
import android.content.res.Resources
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureFormatter
import com.galaxyjoy.cpuinfo.util.Utils
import com.galaxyjoy.cpuinfo.util.round2
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.galaxyjoy.cpuinfo.domain.model.BatteryData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableBatteryData
import com.galaxyjoy.cpuinfo.domain.observe
import javax.inject.Inject

/** Formats snapshots from the Battery interactor; Android reads live in DataProviderBattery. */
@HiltViewModel
class VMBatteryInfo @Inject constructor(
    private val resources: Resources,
    observableBatteryData: ObservableBatteryData,
    private val temperatureFormatter: TemperatureFormatter,
) : ViewModel() {

    private var sessionMinCurrentMa: Double? = null
    private var sessionMaxCurrentMa: Double? = null

    val rows = observableBatteryData.observe()
        .map { data -> statusSection(data) + capacitySection(data) }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

    private fun statusSection(data: BatteryData): List<Pair<String, String>> {
        val batteryStatus = data.status
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(resources.getString(R.string.battery) to "")

        if (batteryStatus == null) return rows

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level != -1 && scale != -1) {
            val batteryPct = level / scale.toFloat() * 100.0
            rows.add(resources.getString(R.string.level) to "${batteryPct.round2()}%")
        }

        val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        if (health != -1) {
            rows.add(resources.getString(R.string.battery_health) to getBatteryHealthStatus(health))
        }

        data.voltageMv?.let { voltage ->
            rows.add(resources.getString(R.string.voltage) to "${voltage / 1000.0}V")
        }
        data.chargingCurrentMa?.let { current ->
            rows.add(resources.getString(R.string.battery_charging_current) to
                resources.getString(R.string.battery_ma_value, current.round2().toString()))
        }

        data.temperature?.let { temperature ->
            rows.add(resources.getString(R.string.temperature) to temperatureFormatter.format(temperature))
        }

        val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        Utils.addPairIfExists(rows, resources.getString(R.string.technology), technology)

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        rows.add(
            resources.getString(R.string.is_charging) to
                if (isCharging) resources.getString(R.string.yes) else resources.getString(R.string.no),
        )
        if (isCharging) {
            rows.add(resources.getString(R.string.charging_type) to chargingType(batteryStatus))
        }

        return rows
    }

    private fun chargingType(batteryStatus: Intent): String = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> resources.getString(R.string.battery_charging_wireless)
        else -> resources.getString(R.string.unknown)
    }

    private fun getBatteryHealthStatus(healthInt: Int): String = when (healthInt) {
        BatteryManager.BATTERY_HEALTH_COLD -> resources.getString(R.string.battery_cold)
        BatteryManager.BATTERY_HEALTH_GOOD -> resources.getString(R.string.battery_good)
        BatteryManager.BATTERY_HEALTH_DEAD -> resources.getString(R.string.battery_dead)
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> resources.getString(R.string.battery_overheat)
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> resources.getString(R.string.battery_overvoltage)
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> resources.getString(R.string.battery_unknown)
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> resources.getString(R.string.battery_unspecified_failure)
        else -> resources.getString(R.string.battery_unknown)
    }

    private fun capacitySection(data: BatteryData): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(resources.getString(R.string.battery_section_capacity) to "")

        val designedCapacity = data.designedCapacity.round2()
        if (designedCapacity != -1.0) {
            rows.add(resources.getString(R.string.battery_designed_capacity) to resources.getString(R.string.battery_mah_value, designedCapacity.toString()))
        }

        data.chargeCounter?.let { microAh ->
            rows.add(resources.getString(R.string.battery_charge_counter) to resources.getString(R.string.battery_mah_value, (microAh / 1000.0).round2().toString()))
        }

        data.energyCounter?.let { nanoWh ->
            rows.add(resources.getString(R.string.battery_energy_counter) to resources.getString(R.string.battery_mwh_value, (nanoWh / 1_000_000.0).round2().toString()))
        }

        data.cycleCount?.let { cycleCount ->
            rows.add(resources.getString(R.string.battery_cycle_count) to cycleCount.toString())
        }

        data.currentMa?.let { rawCurrentMa ->
            val currentMa = rawCurrentMa.round2()
            sessionMinCurrentMa = minOf(sessionMinCurrentMa ?: currentMa, currentMa)
            sessionMaxCurrentMa = maxOf(sessionMaxCurrentMa ?: currentMa, currentMa)

            rows.add(resources.getString(R.string.battery_section_session) to "")
            rows.add(resources.getString(R.string.battery_current_now) to resources.getString(R.string.battery_ma_value, currentMa.toString()))
            rows.add(resources.getString(R.string.battery_session_min_current) to resources.getString(R.string.battery_ma_value, sessionMinCurrentMa.toString()))
            rows.add(resources.getString(R.string.battery_session_max_current) to resources.getString(R.string.battery_ma_value, sessionMaxCurrentMa.toString()))
        }

        return rows
    }

}
