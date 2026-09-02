package com.galaxyjoy.cpuinfo.feat.throttle

import android.os.SystemClock
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Drives the U02 stress workload: [DataProviderCpu.getNumberOfCores] busy-loop coroutines on
 * [Dispatchers.Default] for up to [ThrottleFingerprint.TEST_DURATION_MS], sampling frequency +
 * battery temperature every [ThrottleFingerprint.SAMPLE_INTERVAL_MS]. Stops on whichever comes
 * first: hard duration cap, [ThrottleFingerprint.shouldAbortForSafety], or [requestStop].
 *
 * One instance is owned per [VMThrottle] — not a singleton, [stopRequested] is per-run state.
 */
class ThrottleTestRunner @Inject constructor(
    private val dataProviderCpu: DataProviderCpu,
    private val temperatureProvider: TemperatureProvider,
) {

    sealed interface State {
        data class Running(
            val elapsedMs: Long,
            val currentFreqMhz: Long,
            val currentTempC: Int,
            val samples: List<ThrottleFingerprint.Sample>,
        ) : State

        data class Finished(val result: ThrottleFingerprint.Result) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(onState: suspend (State) -> Unit): Unit = coroutineScope {
        stopRequested = false
        val coreCount = dataProviderCpu.getNumberOfCores().coerceAtLeast(1)
        val samples = mutableListOf<ThrottleFingerprint.Sample>()
        var abortReason: ThrottleFingerprint.AbortReason? = null
        val startElapsed = SystemClock.elapsedRealtime()

        val opsCounters = LongArray(coreCount)
        val workers = List(coreCount) { index -> launch(Dispatchers.Default) { burnCpu(index, opsCounters) } }
        try {
            while (true) {
                delay(ThrottleFingerprint.SAMPLE_INTERVAL_MS)
                val elapsedMs = SystemClock.elapsedRealtime() - startElapsed
                val freqMhz = averageCurrentFreqMhz(coreCount)
                val tempC = temperatureProvider.getBatteryTemperature()
                samples += ThrottleFingerprint.Sample(elapsedMs, freqMhz, tempC)
                onState(State.Running(elapsedMs, freqMhz, tempC, samples.toList()))

                if (ThrottleFingerprint.shouldAbortForSafety(tempC)) {
                    abortReason = ThrottleFingerprint.AbortReason.OVERHEAT
                    break
                }
                if (stopRequested) {
                    abortReason = ThrottleFingerprint.AbortReason.USER_STOPPED
                    break
                }
                if (ThrottleFingerprint.shouldStop(elapsedMs)) {
                    break
                }
            }
        } finally {
            workers.forEach { it.cancel() }
            // join (not just cancel) so each worker's final opsCounters write below is visible
            // to this thread before summing it — cancel() alone only requests, doesn't wait.
            workers.forEach { it.join() }
        }

        val elapsedSeconds = samples.lastOrNull()?.elapsedMs?.div(1000.0) ?: 0.0
        val opsPerSecond = if (elapsedSeconds > 0) (opsCounters.sum() / elapsedSeconds).roundToLong() else 0L

        ThrottleFingerprint.evaluate(samples, opsPerSecond, aborted = abortReason != null, abortReason = abortReason)
            ?.let { onState(State.Finished(it)) }
    }

    private fun averageCurrentFreqMhz(coreCount: Int): Long {
        val freqs = (0 until coreCount).mapNotNull { core ->
            dataProviderCpu.getCurrentFreq(core).takeIf { it > 0 }
        }
        return if (freqs.isEmpty()) 0L else freqs.average().roundToLong()
    }

    /** Tight FP busy-loop — no allocation, checks cancellation every iteration. Each worker only
     * ever writes its own [index] of [opsCounters], so no synchronization is needed here — the
     * caller's `join()` after `cancel()` is what makes the final count visible cross-thread. */
    private suspend fun burnCpu(index: Int, opsCounters: LongArray) {
        var seed = 1.0
        while (coroutineContext.isActive) {
            seed = sqrt(seed + 1.0) * sin(seed)
            if (!seed.isFinite()) seed = 1.0
            opsCounters[index]++
        }
    }
}
