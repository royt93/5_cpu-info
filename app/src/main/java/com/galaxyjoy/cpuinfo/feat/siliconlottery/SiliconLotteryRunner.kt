package com.galaxyjoy.cpuinfo.feat.siliconlottery

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Drives the E04 per-core workload: for each logical core (0 until [DataProviderCpu.getNumberOfCores],
 * the same possible-core-count source the CPU tab's per-core list already uses — NOT
 * `Runtime.availableProcessors()`, which undercounts power-collapsed big cores, see B27), runs
 * [SiliconLotteryBenchmark.DURATION_PER_CORE_MS] of busy-loop work on a DEDICATED single-thread
 * executor pinned to exactly that core, created and torn down entirely within one core's
 * measurement. Same reasoning as [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchmarkRunner]
 * for why a dedicated pool matters: affinity is sticky per OS thread, so restricting a
 * shared-pool thread and returning it to the pool would leak that restriction onto whatever
 * unrelated coroutine runs on it next.
 *
 * Cores are measured sequentially (not concurrently) — measuring core throughput while other
 * cores are also under load would confound "this core's silicon quality" with "chip-wide thermal/
 * scheduling contention", which is exactly what U31's cluster benchmark measures instead.
 */
class SiliconLotteryRunner @Inject constructor(
    private val dataProviderCpu: DataProviderCpu,
    private val dataNativeProviderCpu: DataNativeProviderCpu,
    private val temperatureProvider: TemperatureProvider,
) {

    sealed interface State {
        data class Running(val coreIndex: Int, val coreCount: Int) : State
        data class Finished(val result: SiliconLotteryBenchmark.Result) : State
        data class Aborted(val reason: SiliconLotteryBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(
        durationPerCoreMs: Long = SiliconLotteryBenchmark.DURATION_PER_CORE_MS,
        onState: suspend (State) -> Unit,
    ) {
        stopRequested = false
        val coreCount = dataProviderCpu.getNumberOfCores()
        val results = mutableListOf<SiliconLotteryBenchmark.CoreResult>()

        for (coreIndex in 0 until coreCount) {
            if (SiliconLotteryBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
                onState(State.Aborted(SiliconLotteryBenchmark.AbortReason.OVERHEAT))
                return
            }
            onState(State.Running(coreIndex, coreCount))
            val (opsPerSecond, affinityConfirmed) = benchmarkCore(coreIndex, durationPerCoreMs)
            if (stopRequested) {
                onState(State.Aborted(SiliconLotteryBenchmark.AbortReason.INTERRUPTED))
                return
            }
            results += SiliconLotteryBenchmark.CoreResult(coreIndex, opsPerSecond, affinityConfirmed)
        }

        onState(State.Finished(SiliconLotteryBenchmark.Result(results)))
    }

    private suspend fun benchmarkCore(coreIndex: Int, durationMs: Long): Pair<Long, Boolean> {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        return try {
            withContext(dispatcher) {
                val affinityConfirmed = dataNativeProviderCpu.setThreadAffinity(coreIndex, 1)
                val opsPerSecond = burnCpu(durationMs)
                opsPerSecond to affinityConfirmed
            }
        } finally {
            dispatcher.close()
        }
    }

    /** Tight FP busy-loop, same shape as the other 4 `*BenchmarkRunner`s — no allocation, checks
     * cancellation/stop every iteration. */
    private suspend fun burnCpu(durationMs: Long): Long {
        var ops = 0L
        var seed = 1.0
        val startNanos = System.nanoTime()
        val endNanos = startNanos + durationMs * 1_000_000L
        while (System.nanoTime() < endNanos && coroutineContext.isActive && !stopRequested) {
            seed = sqrt(seed + 1.0) * sin(seed)
            if (!seed.isFinite()) seed = 1.0
            ops++
        }
        val elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0
        return if (elapsedSeconds > 0) (ops / elapsedSeconds).roundToLong() else 0L
    }
}
