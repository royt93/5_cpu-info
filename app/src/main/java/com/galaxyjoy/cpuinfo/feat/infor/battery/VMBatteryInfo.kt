package com.galaxyjoy.cpuinfo.feat.infor.battery

import android.content.Intent
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureFormatter
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Utils
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.round2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Surface battery status + capacity diagnostics via [BatteryManager] and the sticky
 * `ACTION_BATTERY_CHANGED` intent (through [BatteryStatusProvider], moved here from
 * [com.galaxyjoy.cpuinfo.feat.infor.hardware.VMHardwareInfo]).
 *
 * Polls every [REFRESH_DELAY_MS] while the tab is alive — cheap local `BatteryManager` reads, no
 * I/O — so charging state, instantaneous current, and this-session min/max current stay live
 * without a separate `ACTION_POWER_CONNECTED` broadcast receiver. Session min/max is
 * intentionally text-only, not a rendered graph: no charting component exists elsewhere in the
 * app yet (see doc/task/epic-03-new-features.md F01), and this app's list-of-rows tabs don't have
 * one to reuse — adding a one-off chart widget for a single row wasn't worth the extra surface.
 */
@HiltViewModel
class VMBatteryInfo @Inject constructor(
    private val resources: Resources,
    private val batteryManager: BatteryManager,
    private val batteryStatusProvider: BatteryStatusProvider,
    private val temperatureProvider: TemperatureProvider,
    private val temperatureFormatter: TemperatureFormatter,
    private val dispatchersProvider: DispatchersProvider,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    private var sessionMinCurrentMa: Double? = null
    private var sessionMaxCurrentMa: Double? = null

    init {
        viewModelScope.launch(dispatchersProvider.io) {
            while (isActive) {
                listLiveData.replace(buildRows())
                delay(REFRESH_DELAY_MS)
            }
        }
    }

    private fun buildRows(): List<Pair<String, String>> {
        val batteryStatus = batteryStatusProvider.getBatteryStatusIntent()
        val rows = mutableListOf<Pair<String, String>>()
        rows.addAll(statusSection(batteryStatus))
        rows.addAll(capacitySection(batteryStatus))
        return rows
    }

    private fun statusSection(batteryStatus: Intent?): List<Pair<String, String>> {
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

        val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        if (voltage > 0) {
            rows.add(resources.getString(R.string.voltage) to "${voltage / 1000.0}V")
        }

        val temperature = temperatureProvider.getBatteryTemperature()
        if (temperature > 0) {
            rows.add(resources.getString(R.string.temperature) to temperatureFormatter.format(temperature.toFloat()))
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

    private fun capacitySection(batteryStatus: Intent?): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        rows.add(resources.getString(R.string.battery_section_capacity) to "")

        val designedCapacity = batteryStatusProvider.getBatteryCapacity().round2()
        if (designedCapacity != -1.0) {
            rows.add(resources.getString(R.string.battery_designed_capacity) to resources.getString(R.string.battery_mah_value, designedCapacity.toString()))
        }

        batteryManager.longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.let { microAh ->
            rows.add(resources.getString(R.string.battery_charge_counter) to resources.getString(R.string.battery_mah_value, (microAh / 1000.0).round2().toString()))
        }

        batteryManager.longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)?.let { nanoWh ->
            rows.add(resources.getString(R.string.battery_energy_counter) to resources.getString(R.string.battery_mwh_value, (nanoWh / 1_000_000.0).round2().toString()))
        }

        // Cycle count is an ACTION_BATTERY_CHANGED extra (not a BatteryManager property),
        // added in Android 14.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && batteryStatus != null) {
            val cycleCount = batteryStatus.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
            if (cycleCount >= 0) {
                rows.add(resources.getString(R.string.battery_cycle_count) to cycleCount.toString())
            }
        }

        batteryManager.longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.let { rawCurrent ->
            val currentMa = microAmpsToMa(rawCurrent).round2()
            sessionMinCurrentMa = minOf(sessionMinCurrentMa ?: currentMa, currentMa)
            sessionMaxCurrentMa = maxOf(sessionMaxCurrentMa ?: currentMa, currentMa)

            rows.add(resources.getString(R.string.battery_section_session) to "")
            rows.add(resources.getString(R.string.battery_current_now) to resources.getString(R.string.battery_ma_value, currentMa.toString()))
            rows.add(resources.getString(R.string.battery_session_min_current) to resources.getString(R.string.battery_ma_value, sessionMinCurrentMa.toString()))
            rows.add(resources.getString(R.string.battery_session_max_current) to resources.getString(R.string.battery_ma_value, sessionMaxCurrentMa.toString()))
        }

        return rows
    }

    private fun BatteryManager.longPropertyOrNull(id: Int): Long? {
        val value = getLongProperty(id)
        return value.takeIf { it != Long.MIN_VALUE }
    }

    /**
     * `BATTERY_PROPERTY_CURRENT_NOW`/`CURRENT_AVERAGE` are documented in µA, but several OEMs
     * (confirmed on a Samsung Galaxy S24 Ultra here — `adb shell dumpsys battery` reports
     * `current now: 1039` raw, matching what this property returns, for a device visibly pulling
     * ~1A over USB) report already-scaled mA instead. A real µA reading for an active phone is
     * never under [ALREADY_MA_THRESHOLD] in magnitude (that would mean sub-10mA total system
     * draw), so a raw value under that can only be the buggy already-mA case.
     */
    private fun microAmpsToMa(raw: Long): Double =
        if (abs(raw) < ALREADY_MA_THRESHOLD) raw.toDouble() else raw / 1000.0

    companion object {
        private const val REFRESH_DELAY_MS = 3000L
        private const val ALREADY_MA_THRESHOLD = 20_000L
    }
}
