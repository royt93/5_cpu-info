package com.galaxyjoy.cpuinfo.feat.snapshot

import android.os.Build
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderGpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Captures a [HardwareSnapshot] from the live device and persists a single baseline (the last
 * saved snapshot) to DataStore as a JSON string — one row, overwritten on every save, matching
 * the "compare to last baseline" scope of U03 rather than a full history log.
 */
class HardwareSnapshotProvider @Inject constructor(
    private val dataProviderCpu: DataProviderCpu,
    private val dataProviderRam: DataProviderRam,
    private val dataProviderGpu: DataProviderGpu,
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val dataStore: DataStore<Preferences>,
) {

    private val gson = Gson()

    fun captureSnapshot(): HardwareSnapshot {
        val coreCount = dataProviderCpu.getNumberOfCores()
        val maxFreqMhz = (0 until coreCount)
            .map { dataProviderCpu.getMinMaxFreq(it).second }
            .filter { it > 0 }
            .maxOrNull()
            ?.toInt() ?: -1
        val internalPath = Environment.getDataDirectory()

        return HardwareSnapshot(
            timestampMillis = System.currentTimeMillis(),
            cpuName = dataNativeProviderCpu.getCpuName(),
            cpuVendorId = dataNativeProviderCpu.getCoreVendor(0),
            cpuUarchId = dataNativeProviderCpu.getCoreUarch(0),
            coreCount = coreCount,
            maxFreqMhz = maxFreqMhz,
            totalRamBytes = dataProviderRam.getTotalBytes(),
            availableRamBytes = dataProviderRam.getAvailableBytes(),
            internalStorageTotalBytes = internalPath.totalSpace,
            internalStorageFreeBytes = internalPath.usableSpace,
            securityPatchLevel = Build.VERSION.SECURITY_PATCH ?: "Unknown",
            glEsVersion = dataProviderGpu.getGlEsVersion(),
        )
    }

    suspend fun loadSavedSnapshot(): HardwareSnapshot? {
        val json = dataStore.data.first()[KEY_SNAPSHOT_JSON] ?: return null
        return try {
            gson.fromJson(json, HardwareSnapshot::class.java)
        } catch (e: Exception) {
            Timber.e(e, "loadSavedSnapshot() - malformed JSON")
            null
        }
    }

    suspend fun saveSnapshot(snapshot: HardwareSnapshot) {
        dataStore.edit { it[KEY_SNAPSHOT_JSON] = gson.toJson(snapshot) }
    }

    companion object {
        private val KEY_SNAPSHOT_JSON = stringPreferencesKey("hardware_snapshot_json")
    }
}
