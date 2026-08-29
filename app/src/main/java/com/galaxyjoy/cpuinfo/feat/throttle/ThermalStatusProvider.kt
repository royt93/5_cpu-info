package com.galaxyjoy.cpuinfo.feat.throttle

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passive thermal status (F02) — unlike [ThrottleTestRunner] (which actively stresses the CPU),
 * this just asks the system's own thermal manager whether it's currently throttling anything,
 * via `PowerManager.getCurrentThermalStatus()` (API 29+) and `getThermalHeadroom()` (API 30+).
 */
@Singleton
class ThermalStatusProvider @Inject constructor(@ApplicationContext context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    data class Snapshot(
        val statusSupported: Boolean,
        val status: Int,
        val headroomPercent: Int?,
    )

    fun snapshot(): Snapshot {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || powerManager == null) {
            return Snapshot(statusSupported = false, status = -1, headroomPercent = null)
        }
        val status = powerManager.currentThermalStatus
        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val forecast = powerManager.getThermalHeadroom(HEADROOM_FORECAST_SECONDS)
            forecast.takeIf { it.isFinite() && it >= 0f }?.let { (it * 100).toInt() }
        } else {
            null
        }
        return Snapshot(statusSupported = true, status = status, headroomPercent = headroom)
    }

    private companion object {
        const val HEADROOM_FORECAST_SECONDS = 10
    }
}
