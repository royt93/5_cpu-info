package com.galaxyjoy.cpuinfo.feat.gpubench

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the last completed benchmark so a new run can show a before/after comparison. Same
 * pattern as [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs]. */
@Singleton
class GpuBenchResultPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class SavedResult(
        val timestampMs: Long,
        val avgFps: Double,
    )

    fun getLastResult(): SavedResult? {
        val timestamp = sp.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp == 0L) return null
        return SavedResult(
            timestampMs = timestamp,
            avgFps = sp.getFloat(KEY_FPS, 0f).toDouble(),
        )
    }

    fun saveResult(result: GpuBenchmark.Result) {
        sp.edit()
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putFloat(KEY_FPS, result.avgFps.toFloat())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "gpu_bench_result_prefs"
        const val KEY_TIMESTAMP = "timestamp_ms"
        const val KEY_FPS = "avg_fps"
    }
}
