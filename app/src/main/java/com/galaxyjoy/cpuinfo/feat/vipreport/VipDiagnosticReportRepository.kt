package com.galaxyjoy.cpuinfo.feat.vipreport

import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.galaxyjoy.cpuinfo.data.provider.DataProviderRam
import com.galaxyjoy.cpuinfo.feat.infor.hardware.BatteryStatusProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

/**
 * Captures a [VipDiagnosticSnapshot] from the live device and persists a bounded history (capped
 * at [MAX_HISTORY_ENTRIES]) to DataStore as a JSON array — same single-DataStore-key pattern as
 * [com.galaxyjoy.cpuinfo.feat.snapshot.HardwareSnapshotProvider], generalized from one baseline
 * row to a list.
 */
class VipDiagnosticReportRepository @Inject constructor(
    private val batteryManager: BatteryManager,
    private val batteryStatusProvider: BatteryStatusProvider,
    private val dataProviderRam: DataProviderRam,
    private val dataStore: DataStore<Preferences>,
) {

    private val gson = Gson()

    fun captureSnapshot(): VipDiagnosticSnapshot {
        val batteryStatus = batteryStatusProvider.getBatteryStatusIntent()
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (level >= 0 && scale > 0) level * 100 / scale else -1

        val chargeCounterMah = batteryManager.longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.let { it / 1000.0 } ?: -1.0

        // Cycle count is an ACTION_BATTERY_CHANGED extra (not a BatteryManager property), added
        // in Android 14 — same gate as VMBatteryInfo.capacitySection().
        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && batteryStatus != null) {
            batteryStatus.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
        } else {
            -1
        }

        val internalPath = Environment.getDataDirectory()

        return VipDiagnosticSnapshot(
            timestampMillis = System.currentTimeMillis(),
            batteryLevelPercent = levelPercent,
            designedCapacityMah = batteryStatusProvider.getBatteryCapacity(),
            chargeCounterMah = chargeCounterMah,
            cycleCount = cycleCount,
            batteryHealth = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1,
            ramAvailablePercentage = dataProviderRam.getAvailablePercentage(),
            internalStorageFreeBytes = internalPath.usableSpace,
            internalStorageTotalBytes = internalPath.totalSpace,
        )
    }

    suspend fun loadHistory(): List<VipDiagnosticSnapshot> {
        val json = dataStore.data.first()[KEY_HISTORY_JSON] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VipDiagnosticSnapshot>>() {}.type
            gson.fromJson<List<VipDiagnosticSnapshot>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "loadHistory() - malformed JSON")
            emptyList()
        }
    }

    /**
     * Appends [snapshot], replacing today's entry instead of adding a duplicate if one was
     * already saved today — the history is meant to track drift over months, not every tap.
     */
    suspend fun saveSnapshot(snapshot: VipDiagnosticSnapshot) {
        val updated = (loadHistory().filterNot { isSameCalendarDay(it.timestampMillis, snapshot.timestampMillis) } + snapshot)
            .sortedBy { it.timestampMillis }
            .takeLast(MAX_HISTORY_ENTRIES)
        dataStore.edit { it[KEY_HISTORY_JSON] = gson.toJson(updated) }
    }

    private fun isSameCalendarDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun BatteryManager.longPropertyOrNull(id: Int): Long? {
        val value = getLongProperty(id)
        return value.takeIf { it != Long.MIN_VALUE }
    }

    companion object {
        private val KEY_HISTORY_JSON = stringPreferencesKey("vip_diagnostic_history_json")
        const val MAX_HISTORY_ENTRIES = 24
    }
}
