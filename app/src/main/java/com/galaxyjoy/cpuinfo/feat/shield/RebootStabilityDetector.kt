package com.galaxyjoy.cpuinfo.feat.shield

import kotlin.math.abs

/** E21 — pure boot-time comparison, no Android deps so it's plain-JVM testable.
 * Boot time = wall clock minus uptime; it shifts by more than [TOLERANCE_MILLIS] only across an
 * actual reboot (clock drift/NTP adjustment alone doesn't move it that far). */
object RebootStabilityDetector {

    private const val TOLERANCE_MILLIS = 60_000L

    fun isNewBoot(currentBootTimeMillis: Long, lastKnownBootTimeMillis: Long): Boolean {
        if (lastKnownBootTimeMillis < 0) return false
        return abs(currentBootTimeMillis - lastKnownBootTimeMillis) > TOLERANCE_MILLIS
    }
}
