package com.galaxyjoy.cpuinfo.feat.rambench

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the last completed benchmark so a new run can show a before/after comparison. Same
 * pattern as [com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs]. */
@Singleton
class RamBenchResultPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class SavedResult(
        val timestampMs: Long,
        val writeMbPerSec: Double,
        val readMbPerSec: Double,
    )

    fun getLastResult(): SavedResult? {
        val timestamp = sp.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp == 0L) return null
        return SavedResult(
            timestampMs = timestamp,
            writeMbPerSec = sp.getFloat(KEY_WRITE, 0f).toDouble(),
            readMbPerSec = sp.getFloat(KEY_READ, 0f).toDouble(),
        )
    }

    fun saveResult(result: RamBenchmark.Result) {
        sp.edit()
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putFloat(KEY_WRITE, result.writeMbPerSec.toFloat())
            .putFloat(KEY_READ, result.readMbPerSec.toFloat())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "ram_bench_result_prefs"
        const val KEY_TIMESTAMP = "timestamp_ms"
        const val KEY_WRITE = "write_mb_s"
        const val KEY_READ = "read_mb_s"
    }
}
