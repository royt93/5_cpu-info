package com.galaxyjoy.cpuinfo.feat.storagebench

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the last completed benchmark so a new run can show a before/after comparison. */
@Singleton
class StorageBenchResultPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class SavedResult(
        val timestampMs: Long,
        val seqWriteMbPerSec: Double,
        val seqReadMbPerSec: Double,
        val randomWriteOpsPerSec: Double,
        val randomReadOpsPerSec: Double,
        val hashMbPerSec: Double,
    )

    fun getLastResult(): SavedResult? {
        val timestamp = sp.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp == 0L) return null
        return SavedResult(
            timestampMs = timestamp,
            seqWriteMbPerSec = sp.getFloat(KEY_SEQ_WRITE, 0f).toDouble(),
            seqReadMbPerSec = sp.getFloat(KEY_SEQ_READ, 0f).toDouble(),
            randomWriteOpsPerSec = sp.getFloat(KEY_RANDOM_WRITE, 0f).toDouble(),
            randomReadOpsPerSec = sp.getFloat(KEY_RANDOM_READ, 0f).toDouble(),
            hashMbPerSec = sp.getFloat(KEY_HASH, 0f).toDouble(),
        )
    }

    fun saveResult(result: StorageBenchmark.Result) {
        sp.edit()
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putFloat(KEY_SEQ_WRITE, result.seqWriteMbPerSec.toFloat())
            .putFloat(KEY_SEQ_READ, result.seqReadMbPerSec.toFloat())
            .putFloat(KEY_RANDOM_WRITE, result.randomWriteOpsPerSec.toFloat())
            .putFloat(KEY_RANDOM_READ, result.randomReadOpsPerSec.toFloat())
            .putFloat(KEY_HASH, result.hashMbPerSec.toFloat())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "storage_bench_result_prefs"
        const val KEY_TIMESTAMP = "timestamp_ms"
        const val KEY_SEQ_WRITE = "seq_write_mb_s"
        const val KEY_SEQ_READ = "seq_read_mb_s"
        const val KEY_RANDOM_WRITE = "random_write_ops_s"
        const val KEY_RANDOM_READ = "random_read_ops_s"
        const val KEY_HASH = "hash_mb_s"
    }
}
