package com.galaxyjoy.cpuinfo.feat.throttle

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the last completed throttle-test result so a new run can show a before/after delta
 * (e.g. after a battery swap, ROM update, or reapplying thermal paste).
 */
@Singleton
class ThrottleResultPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class SavedResult(
        val timestampMs: Long,
        val peakFreqMhz: Long,
        val sustainedFreqMhz: Long,
        val throttlePercent: Int,
        val maxTempC: Int,
    )

    fun getLastResult(): SavedResult? {
        val timestamp = sp.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp == 0L) return null
        return SavedResult(
            timestampMs = timestamp,
            peakFreqMhz = sp.getLong(KEY_PEAK_FREQ, 0L),
            sustainedFreqMhz = sp.getLong(KEY_SUSTAINED_FREQ, 0L),
            throttlePercent = sp.getInt(KEY_THROTTLE_PERCENT, 0),
            maxTempC = sp.getInt(KEY_MAX_TEMP, 0),
        )
    }

    fun saveResult(result: ThrottleFingerprint.Result) {
        sp.edit()
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putLong(KEY_PEAK_FREQ, result.peakFreqMhz)
            .putLong(KEY_SUSTAINED_FREQ, result.sustainedFreqMhz)
            .putInt(KEY_THROTTLE_PERCENT, result.throttlePercent)
            .putInt(KEY_MAX_TEMP, result.maxTempC)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "throttle_result_prefs"
        const val KEY_TIMESTAMP = "timestamp_ms"
        const val KEY_PEAK_FREQ = "peak_freq_mhz"
        const val KEY_SUSTAINED_FREQ = "sustained_freq_mhz"
        const val KEY_THROTTLE_PERCENT = "throttle_percent"
        const val KEY_MAX_TEMP = "max_temp_c"
    }
}
