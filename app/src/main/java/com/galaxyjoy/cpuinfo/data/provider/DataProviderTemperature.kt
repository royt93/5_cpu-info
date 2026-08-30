package com.galaxyjoy.cpuinfo.data.provider

import android.os.BatteryManager
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.galaxyjoy.cpuinfo.util.Utils
import java.io.File
import javax.inject.Inject

/**
 * There's no public Android API for CPU temperature, so [findCpuTempPath] scans a list of
 * historically-known-working sysfs locations once; the confirmed path is then re-read on every
 * poll via [getCpuTemp]. Battery temperature reuses the sticky `ACTION_BATTERY_CHANGED` intent
 * already fetched by [BatteryStatusProvider] elsewhere in the app.
 */
class DataProviderTemperature @Inject constructor(
    private val batteryStatusProvider: BatteryStatusProvider,
) {

    /** @return battery temperature in Celsius, or null if unavailable. */
    fun getBatteryTemperature(): Float? {
        val raw = batteryStatusProvider.getBatteryStatusIntent()
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return (raw / 10f).takeIf { raw != 0 }
    }

    /** Re-read the temperature at [path], a location already confirmed valid by [findCpuTempPath]. */
    fun getCpuTemp(path: String): Float {
        val temp = Utils.readOneLine(File(path)) ?: 0.0
        return if (isTemperatureValid(temp)) temp.toFloat() else (temp / 1000).toFloat()
    }

    /** Scan well-known sysfs locations for a working CPU temperature file. Slow — call once. */
    fun findCpuTempPath(): String? {
        for (path in CPU_TEMP_FILE_PATHS) {
            val temp = Utils.readOneLine(File(path)) ?: continue
            if (isTemperatureValid(temp) || isTemperatureValid(temp / 1000)) {
                return path
            }
        }
        return null
    }

    /**
     * Check if passed temperature is in normal range: -30 - 250 Celsius
     */
    private fun isTemperatureValid(temp: Double): Boolean = temp in -30.0..250.0

    companion object {
        // Ugly but currently the easiest working solution is to search well known locations.
        // If you know better solution please refactor this :)
        private val CPU_TEMP_FILE_PATHS = listOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
            "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/devices/platform/tegra_tmon/temp1_input",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/platform/s5p-tmu/temperature",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/hwmon/hwmon0/device/temp1_input",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/hwmon/hwmonX/temp1_input",
            "/sys/devices/platform/s5p-tmu/curr_temp",
        )
    }
}
