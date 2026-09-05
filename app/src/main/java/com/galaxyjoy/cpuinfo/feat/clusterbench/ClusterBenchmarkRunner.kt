package com.galaxyjoy.cpuinfo.feat.clusterbench

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyProvider
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Drives the U31 per-cluster workload: for each cluster reported by [ClusterTopologyProvider],
 * runs [ClusterBenchmark.DURATION_PER_CLUSTER_MS] of busy-loop work on a DEDICATED thread pool
 * (not the shared [Dispatchers.Default] pool [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleTestRunner]
 * uses) sized to exactly that cluster's core count, with each worker pinning itself via
 * [DataNativeProviderCpu.setThreadAffinity] to one specific core within the cluster before
 * starting its loop. A dedicated pool matters here specifically because affinity is sticky per
 * OS thread — restricting a shared-pool thread's affinity and then returning it to the pool would
 * leak that restriction onto whatever unrelated coroutine runs on it next; a pool created and torn
 * down entirely within one cluster's measurement has no such leak.
 *
 * Clusters are measured sequentially (not concurrently) so each result reflects that cluster's
 * throughput in isolation, without cross-cluster thermal/scheduling contention.
 */
class ClusterBenchmarkRunner @Inject constructor(
    private val clusterTopologyProvider: ClusterTopologyProvider,
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val temperatureProvider: TemperatureProvider,
) {

    sealed interface State {
        data class Running(val clusterIndex: Int, val clusterCount: Int, val tier: ClusterTopologyBuilder.Tier) : State
        data class Finished(val result: ClusterBenchmark.Result) : State
        data class Aborted(val reason: ClusterBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(
        durationPerClusterMs: Long = ClusterBenchmark.DURATION_PER_CLUSTER_MS,
        onState: suspend (State) -> Unit,
    ) {
        stopRequested = false
        val clusters = clusterTopologyProvider.clusters()
        val results = mutableListOf<ClusterBenchmark.ClusterResult>()

        for ((index, cluster) in clusters.withIndex()) {
            if (ClusterBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
                onState(State.Aborted(ClusterBenchmark.AbortReason.OVERHEAT))
                return
            }
            onState(State.Running(index, clusters.size, cluster.tier))
            val (opsPerSecond, affinityConfirmed) = benchmarkCluster(cluster, durationPerClusterMs)
            if (stopRequested) {
                onState(State.Aborted(ClusterBenchmark.AbortReason.INTERRUPTED))
                return
            }
            results += ClusterBenchmark.ClusterResult(cluster.tier, cluster.coreCount, opsPerSecond, affinityConfirmed)
        }

        onState(State.Finished(ClusterBenchmark.Result(results)))
    }

    private suspend fun benchmarkCluster(
        cluster: ClusterTopologyBuilder.Cluster,
        durationMs: Long,
    ): Pair<Long, Boolean> = withContext(Dispatchers.Default) {
        val opsCounters = LongArray(cluster.coreCount)
        // Same no-synchronization reasoning as opsCounters: each worker only ever writes its own
        // index, visibility guaranteed by the join() below, not by these writes racing.
        val affinityPinned = BooleanArray(cluster.coreCount)
        val executor = Executors.newFixedThreadPool(cluster.coreCount)
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            val workers = (0 until cluster.coreCount).map { i ->
                launch(dispatcher) {
                    affinityPinned[i] = dataNativeProviderCpu.setThreadAffinity(cluster.coreIndexRange.first + i, 1)
                    burnCpu(i, opsCounters)
                }
            }

            val chunkMs = (durationMs / 10).coerceAtLeast(1L)
            var elapsedChunks = 0
            while (elapsedChunks < 10 && !stopRequested) {
                delay(chunkMs)
                elapsedChunks++
            }

            workers.forEach { it.cancel() }
            // join (not just cancel) so each worker's final opsCounters write is visible to this
            // thread before summing it below — same reasoning as ThrottleTestRunner.
            workers.forEach { it.join() }
        } finally {
            dispatcher.close()
        }

        val elapsedSeconds = durationMs / 1000.0
        val opsPerSecond = if (elapsedSeconds > 0) (opsCounters.sum() / elapsedSeconds).roundToLong() else 0L
        opsPerSecond to affinityPinned.all { it }
    }

    /** Tight FP busy-loop — no allocation, checks cancellation every iteration. Each worker only
     * ever writes its own [index] of [opsCounters], so no synchronization is needed. */
    private suspend fun burnCpu(index: Int, opsCounters: LongArray) {
        var seed = 1.0
        while (coroutineContext.isActive) {
            seed = sqrt(seed + 1.0) * sin(seed)
            if (!seed.isFinite()) seed = 1.0
            opsCounters[index]++
        }
    }
}
