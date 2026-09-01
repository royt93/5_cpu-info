package com.galaxyjoy.cpuinfo.domain.model

import android.hardware.Sensor

/**
 * One [android.hardware.SensorEvent] carried through the domain layer. Keeps the raw [Sensor]
 * reference (not a converted value type) because row lookup compares it by identity/equals
 * against the list captured at registration time — `indexOf` can return -1 on custom ROMs where
 * the event's sensor instance doesn't match-by-equals the one from `getSensorList()` (B28),
 * which callers must keep guarding against. Not a `data class`: [values] is never compared or
 * deduplicated downstream, so structural array equality would only be misleading.
 */
class SensorReading(val sensor: Sensor, val values: FloatArray)
