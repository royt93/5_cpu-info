package com.galaxyjoy.cpuinfo.feat.lastbenchwidget

import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchResultPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleResultPrefs

/**
 * Pure "which of the 4 benchmark results is the most recent" decision for the U21 home-screen
 * widget — kept separate from [LastBenchWidgetProvider] so it's plain-JVM unit-testable (building
 * a real `RemoteViews`/reading real `SharedPreferences` is not).
 */
object LastBenchPicker {

    enum class Kind { THROTTLE, STORAGE, RAM, GPU }

    data class Latest(
        val kind: Kind,
        val timestampMs: Long,
        val throttle: ThrottleResultPrefs.SavedResult? = null,
        val storage: StorageBenchResultPrefs.SavedResult? = null,
        val ram: RamBenchResultPrefs.SavedResult? = null,
        val gpu: GpuBenchResultPrefs.SavedResult? = null,
    )

    fun pick(
        throttle: ThrottleResultPrefs.SavedResult?,
        storage: StorageBenchResultPrefs.SavedResult?,
        ram: RamBenchResultPrefs.SavedResult?,
        gpu: GpuBenchResultPrefs.SavedResult?,
    ): Latest? {
        val candidates = listOfNotNull(
            throttle?.let { Latest(Kind.THROTTLE, it.timestampMs, throttle = it) },
            storage?.let { Latest(Kind.STORAGE, it.timestampMs, storage = it) },
            ram?.let { Latest(Kind.RAM, it.timestampMs, ram = it) },
            gpu?.let { Latest(Kind.GPU, it.timestampMs, gpu = it) },
        )
        return candidates.maxByOrNull { it.timestampMs }
    }
}
