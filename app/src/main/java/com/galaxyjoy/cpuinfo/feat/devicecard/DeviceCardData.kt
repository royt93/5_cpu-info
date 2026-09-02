package com.galaxyjoy.cpuinfo.feat.devicecard

import java.util.Locale

/**
 * Everything [DeviceCardRenderer] needs to draw the shareable "device ID card" (U14) — already
 * formatted into display strings so the renderer stays pure layout/drawing, no unit conversion.
 * [shieldScore] is nullable since [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreCalculator] always
 * returns *something*, but a badge-less card should still be possible for a future caller that
 * doesn't want to compute it.
 */
data class DeviceCardData(
    val deviceModel: String,
    val chipName: String,
    val coreCount: Int,
    val ramTotalBytes: Long,
    val storageTotalBytes: Long,
    val screenResolution: String,
    val refreshRateHz: Int,
    val androidVersion: String,
    val shieldScore: Int?,
) {
    companion object {
        private const val GIGA = 1024.0 * 1024.0 * 1024.0

        /** Always renders with a "." decimal point, regardless of the device's default locale —
         * same convention as [com.galaxyjoy.cpuinfo.util.SystemInfoExporter.formatTwoDecimals]. */
        fun formatGb(bytes: Long): String = "%.1f GB".format(Locale.US, bytes / GIGA)
    }
}
