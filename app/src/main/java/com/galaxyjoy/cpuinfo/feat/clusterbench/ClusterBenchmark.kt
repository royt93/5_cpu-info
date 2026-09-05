package com.galaxyjoy.cpuinfo.feat.clusterbench

import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import com.galaxyjoy.cpuinfo.util.BenchmarkSafety

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
    const val SAFETY_ABORT_TEMP_C = BenchmarkSafety.SAFETY_ABORT_TEMP_C

    enum class AbortReason { OVERHEAT, INTERRUPTED }

    data class ClusterResult(
        val tier: ClusterTopologyBuilder.Tier,
        val coreCount: Int,
        val opsPerSecond: Long,
        /** False if [com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu.setThreadAffinity]
         * failed for at least one worker — the measurement may have run on the wrong cores
         * (kernel/cgroup denied the pin) so the OS scheduler could have spread load elsewhere. */
        val affinityConfirmed: Boolean = true,
    )

    data class Result(val clusters: List<ClusterResult>)

    fun shouldAbortForSafety(tempC: Int): Boolean = BenchmarkSafety.shouldAbortForSafety(tempC)
}
