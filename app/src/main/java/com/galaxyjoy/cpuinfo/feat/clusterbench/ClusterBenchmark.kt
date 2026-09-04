package com.galaxyjoy.cpuinfo.feat.clusterbench

import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder

/**
 * Pure U31 model — no Android deps. [ClusterBenchmarkRunner] drives the actual per-cluster
 * pinned-thread workload and feeds raw ops counts here. Unlike the other 4 `*Benchmark` objects,
 * this one measures N independent results (1 per core cluster) instead of 1-2 scalars, and
 * deliberately has no persisted history/previous-run comparison (see
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchScreen] doc) — it's a one-shot
 * "compare your device's tiers right now" tool, same shape as
 * [com.galaxyjoy.cpuinfo.feat.fleet.FleetCompareBottomSheet]/
 * [com.galaxyjoy.cpuinfo.feat.canmydevice.CanMyDeviceBottomSheet], not a trend-tracked benchmark.
 */
object ClusterBenchmark {

    const val DURATION_PER_CLUSTER_MS = 3_000L

    /** Same threshold as U02's [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint]. */
    const val SAFETY_ABORT_TEMP_C = 43

    enum class AbortReason { OVERHEAT, INTERRUPTED }

    data class ClusterResult(
        val tier: ClusterTopologyBuilder.Tier,
        val coreCount: Int,
        val opsPerSecond: Long,
    )

    data class Result(val clusters: List<ClusterResult>)

    fun shouldAbortForSafety(tempC: Int): Boolean = tempC >= SAFETY_ABORT_TEMP_C
}
