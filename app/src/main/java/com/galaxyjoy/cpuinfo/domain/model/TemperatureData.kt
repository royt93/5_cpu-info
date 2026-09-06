package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

/**
 * CPU temperature needs a one-time sysfs scan before it's known whether the device exposes a
 * reading at all — [Probing] models that in-flight discovery, [Unavailable] a device with
 * neither CPU nor battery temperature exposed.
 */
@Keep
sealed interface TemperatureData {
    data object Probing : TemperatureData

    /** [allZones] (E07) is a best-effort full `/sys/class/thermal/thermal_zone*` glob, additive to
     * the 2 well-known [cpuTemp]/[batteryTemp] readings above — defaults to empty so it never
     * breaks a call site that only cares about the original 2 fields. */
    data class Available(val cpuTemp: Float?, val batteryTemp: Float?, val allZones: List<ThermalZoneReading> = emptyList()) : TemperatureData
    data object Unavailable : TemperatureData
}

@Keep
data class ThermalZoneReading(val zoneName: String, val tempCelsius: Float)
