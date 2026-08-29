package com.galaxyjoy.cpuinfo.feat.throttle

import com.galaxyjoy.cpuinfo.R

/**
 * Pure mapping from [android.os.PowerManager]'s raw `THERMAL_STATUS_*` int (F02) to display
 * resources — no Android deps beyond the `R` reference, so the mapping itself is unit-testable.
 */
object ThermalStatusMapper {

    enum class Severity { OK, WARNING, DANGER }

    data class Mapping(val labelRes: Int, val severity: Severity)

    /**
     * @param status one of `PowerManager.THERMAL_STATUS_*` (0=NONE .. 6=SHUTDOWN), or any other
     * value (including negative) treated as unknown/unsupported.
     */
    fun mappingFor(status: Int): Mapping = when (status) {
        0 -> Mapping(R.string.thermal_status_none, Severity.OK)
        1 -> Mapping(R.string.thermal_status_light, Severity.WARNING)
        2 -> Mapping(R.string.thermal_status_moderate, Severity.WARNING)
        3 -> Mapping(R.string.thermal_status_severe, Severity.DANGER)
        4 -> Mapping(R.string.thermal_status_critical, Severity.DANGER)
        5 -> Mapping(R.string.thermal_status_emergency, Severity.DANGER)
        6 -> Mapping(R.string.thermal_status_shutdown, Severity.DANGER)
        else -> Mapping(R.string.thermal_status_unknown, Severity.OK)
    }

    fun isThrottling(status: Int): Boolean = status in 1..6
}
