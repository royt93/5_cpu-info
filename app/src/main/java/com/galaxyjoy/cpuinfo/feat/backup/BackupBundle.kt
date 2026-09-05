package com.galaxyjoy.cpuinfo.feat.backup

import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs
import com.google.gson.Gson

/**
 * U32 — the whole-app backup/restore payload: the 4 benchmark histories (the actual hard-to-
 * reproduce data — re-running old device configurations isn't possible on a new phone) plus the
 * handful of settings worth carrying over. `version` follows the same forward-compat shape as
 * [com.galaxyjoy.cpuinfo.feat.p2pcompare.DeviceComparePayload]. Every field is nullable with no
 * default read on write — Gson's reflection-based deserializer bypasses Kotlin's constructor
 * defaults entirely, so a field missing from a foreign/older JSON lands as a real `null` at
 * runtime regardless of the Kotlin type; declaring these non-null here would just mean [decode]
 * silently lies about safety. Every consumer of a decoded bundle must null-check per field (same
 * convention [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs.SavedResult.osBuildFingerprint]
 * already established for this exact Gson gotcha).
 */
data class BackupBundle(
    val version: Int,
    val throttleHistory: List<ThrottleResultPrefs.SavedResult>? = null,
    val storageHistory: List<StorageBenchResultPrefs.SavedResult>? = null,
    val ramHistory: List<RamBenchResultPrefs.SavedResult>? = null,
    val gpuHistory: List<GpuBenchResultPrefs.SavedResult>? = null,
    val temperatureUnit: String? = null,
    val theme: String? = null,
    val languageTag: String? = null,
) {
    companion object {
        const val CURRENT_VERSION = 1
        private val gson = Gson()

        fun encode(bundle: BackupBundle): String = gson.toJson(bundle)

        /** Never throws — an imported file can be anything (empty, garbage text, JSON from a
         * different app, a future/older app version). */
        fun decode(json: String): BackupBundle? = try {
            gson.fromJson(json, BackupBundle::class.java)?.takeIf { it.version in 1..CURRENT_VERSION }
        } catch (e: Exception) {
            null
        }
    }
}
