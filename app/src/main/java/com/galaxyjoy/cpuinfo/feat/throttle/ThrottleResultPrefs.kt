package com.galaxyjoy.cpuinfo.feat.throttle

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the last completed throttle-test result (before/after delta — e.g. after a battery
 * swap, ROM update, or reapplying thermal paste) and a bounded history (U18 trend chart) — same
 * JSON-array-in-a-single-key pattern as [com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs].
 */
@Singleton
class ThrottleResultPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    data class SavedResult(
        val timestampMs: Long,
        val peakFreqMhz: Long,
        val sustainedFreqMhz: Long,
        val throttlePercent: Int,
        val maxTempC: Int,
        val opsPerSecond: Long,
        /** U29 — `Build.FINGERPRINT` at the time this run was saved, `null` for entries saved
         * before this field existed (Gson leaves it `null` on old persisted JSON missing the
         * key). Lets [com.galaxyjoy.cpuinfo.util.OsUpdateImpactCalculator] detect an OTA update
         * between 2 runs and compare before/after. */
        val osBuildFingerprint: String? = null,
    )

    fun getLastResult(): SavedResult? = getHistory().lastOrNull()

    fun getHistory(): List<SavedResult> {
        val json = sp.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedResult>>() {}.type
            gson.fromJson<List<SavedResult>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveResult(result: ThrottleFingerprint.Result) {
        val entry = SavedResult(
            timestampMs = System.currentTimeMillis(),
            peakFreqMhz = result.peakFreqMhz,
            sustainedFreqMhz = result.sustainedFreqMhz,
            throttlePercent = result.throttlePercent,
            maxTempC = result.maxTempC,
            opsPerSecond = result.opsPerSecond,
            osBuildFingerprint = Build.FINGERPRINT,
        )
        val updated = (getHistory() + entry).takeLast(MAX_HISTORY_ENTRIES)
        sp.edit().putString(KEY_HISTORY_JSON, gson.toJson(updated)).apply()
    }

    /** U32 — wholesale overwrite for backup restore, unlike [saveResult]'s append-one-entry. */
    fun replaceHistory(entries: List<SavedResult>) {
        sp.edit().putString(KEY_HISTORY_JSON, gson.toJson(entries.takeLast(MAX_HISTORY_ENTRIES))).apply()
    }

    private companion object {
        const val PREFS_NAME = "throttle_result_prefs"
        const val KEY_HISTORY_JSON = "history_json"
        const val MAX_HISTORY_ENTRIES = 24
    }
}
