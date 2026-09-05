package com.galaxyjoy.cpuinfo.feat.siliconlottery

import com.galaxyjoy.cpuinfo.util.BenchmarkSafety

/**
 * Pure E04 "Silicon Lottery" model — no Android deps. [SiliconLotteryRunner] pins a single
 * dedicated thread to each logical core in turn (reusing [com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu.setThreadAffinity]
 * built for U31) and feeds raw ops counts here. Unlike [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchmark]
 * (which measures per CLUSTER), this measures per individual CORE — "identical" cores off the
 * same die can still bin slightly differently, which this surfaces as a strongest/weakest core
 * and a spread percentage. Same one-shot "measure right now" shape as ClusterBenchmark: no
 * persisted history, no cross-device baseline (no calibration data exists for that).
 */
object SiliconLotteryBenchmark {

    const val DURATION_PER_CORE_MS = 1_500L

    /** Same threshold as every other on-device benchmark in this app. */
    const val SAFETY_ABORT_TEMP_C = BenchmarkSafety.SAFETY_ABORT_TEMP_C

    enum class AbortReason { OVERHEAT, INTERRUPTED }

    data class CoreResult(
        val coreIndex: Int,
        val opsPerSecond: Long,
        /** False if `setThreadAffinity` failed for this core — same caveat as
         * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchmark.ClusterResult.affinityConfirmed]:
         * the measurement may not actually reflect this specific core. */
        val affinityConfirmed: Boolean = true,
    )

    data class Result(val cores: List<CoreResult>)

    fun shouldAbortForSafety(tempC: Int): Boolean = BenchmarkSafety.shouldAbortForSafety(tempC)

    /** Highest-throughput core, or `null` if [Result.cores] is empty. */
    fun strongest(result: Result): CoreResult? = result.cores.maxByOrNull { it.opsPerSecond }

    /** Lowest-throughput core, or `null` if [Result.cores] is empty. */
    fun weakest(result: Result): CoreResult? = result.cores.minByOrNull { it.opsPerSecond }

    /** % difference between the strongest and weakest core's throughput. `0.0` if fewer than 2
     * cores were measured, or the strongest core measured 0 ops/s (e.g. safety-aborted before
     * producing real data for any core). */
    fun spreadPercent(result: Result): Double {
        if (result.cores.size < 2) return 0.0
        val strong = strongest(result) ?: return 0.0
        val weak = weakest(result) ?: return 0.0
        if (strong.opsPerSecond <= 0) return 0.0
        return (strong.opsPerSecond - weak.opsPerSecond) * 100.0 / strong.opsPerSecond
    }
}
