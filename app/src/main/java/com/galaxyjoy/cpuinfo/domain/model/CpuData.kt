package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class CpuData(
    val processorName: String,
    val abi: String,
    val coreNumber: Int,
    val hasArmNeon: Boolean,
    val frequencies: List<Frequency>,
    val l1dCaches: String,
    val l1iCaches: String,
    val l2Caches: String,
    val l3Caches: String,
    val l4Caches: String,
    /** U34 — cpufreq governor for core 0 (e.g. "schedutil"), `null` if unreadable. */
    val governor: String? = null
) {

    data class Frequency(
        val min: Long,
        val max: Long,
        val current: Long
    )
}
