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
    data class Available(val cpuTemp: Float?, val batteryTemp: Float?) : TemperatureData
    data object Unavailable : TemperatureData
}
