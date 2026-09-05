package com.galaxyjoy.cpuinfo.util

/**
 * Single source of truth for the thermal abort threshold shared by all 5 on-device benchmarks
 * (Throttle/Storage/RAM/GPU/Cluster) — previously copy-pasted verbatim into each `*Benchmark`
 * object, so a future policy change (e.g. a device-tier-aware threshold) would have needed
 * editing 5 files in lockstep.
 */
object BenchmarkSafety {
    const val SAFETY_ABORT_TEMP_C = 43
    fun shouldAbortForSafety(tempC: Int): Boolean = tempC >= SAFETY_ABORT_TEMP_C
}
