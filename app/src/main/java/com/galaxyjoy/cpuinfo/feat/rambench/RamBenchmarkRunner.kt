package com.galaxyjoy.cpuinfo.feat.rambench

import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

/**
 * Drives the U16 workload on [Dispatchers.Default] (CPU-bound memory copies, not I/O — unlike
 * F06's [com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmarkRunner]): allocate one scratch
 * buffer, repeatedly `System.arraycopy` into it for [RamBenchmark.WRITE_DURATION_MS], then out of
 * it for [RamBenchmark.READ_DURATION_MS]. Duration-based looping (not a fixed total byte count
 * like Storage's sequential phases) — memory copies are fast enough that a single fixed-size pass
 * wouldn't take a reliably measurable amount of time.
 */
class RamBenchmarkRunner @Inject constructor(
    private val temperatureProvider: TemperatureProvider,
) {

    enum class Phase { WRITE, READ }

    sealed interface State {
        data class Running(val phase: Phase) : State
        data class Finished(val result: RamBenchmark.Result) : State
        data class Aborted(val reason: RamBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(onState: suspend (State) -> Unit): Unit = withContext(Dispatchers.Default) {
        stopRequested = false

        if (RamBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
            onState(State.Aborted(RamBenchmark.AbortReason.OVERHEAT))
            return@withContext
        }
        val runtime = Runtime.getRuntime()
        if (!RamBenchmark.hasEnoughMemory(runtime.maxMemory(), runtime.totalMemory() - runtime.freeMemory())) {
            onState(State.Aborted(RamBenchmark.AbortReason.INSUFFICIENT_MEMORY))
            return@withContext
        }

        val buffer = ByteArray(RamBenchmark.BUFFER_SIZE_BYTES)

        onState(State.Running(Phase.WRITE))
        val (writeBytes, writeNanos) = writeBenchmark(buffer)
        if (stopRequested) return@withContext onState(State.Aborted(RamBenchmark.AbortReason.OVERHEAT))

        if (RamBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
            return@withContext onState(State.Aborted(RamBenchmark.AbortReason.OVERHEAT))
        }

        onState(State.Running(Phase.READ))
        val (readBytes, readNanos) = readBenchmark(buffer)

        onState(
            State.Finished(
                RamBenchmark.Result(
                    writeMbPerSec = RamBenchmark.mbPerSec(writeBytes, writeNanos),
                    readMbPerSec = RamBenchmark.mbPerSec(readBytes, readNanos),
                ),
            ),
        )
    }

    /** @return total bytes copied to `RamBenchmark.WRITE_DURATION_MS`. */
    private fun writeBenchmark(buffer: ByteArray): Pair<Long, Long> {
        val source = ByteArray(RamBenchmark.CHUNK_BYTES).also(Random::nextBytes)
        return copyLoop(RamBenchmark.WRITE_DURATION_MS) { offset ->
            System.arraycopy(source, 0, buffer, offset, source.size)
        }
    }

    /** @return total bytes copied to `RamBenchmark.READ_DURATION_MS`. Drains into a throwaway
     * sink — throughput is what's measured, not the content (same convention as
     * [com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmarkRunner.sequentialRead]). */
    private fun readBenchmark(buffer: ByteArray): Pair<Long, Long> {
        val sink = ByteArray(RamBenchmark.CHUNK_BYTES)
        return copyLoop(RamBenchmark.READ_DURATION_MS) { offset ->
            System.arraycopy(buffer, offset, sink, 0, sink.size)
        }
    }

    private inline fun copyLoop(durationMs: Long, copy: (offset: Int) -> Unit): Pair<Long, Long> {
        var totalBytes = 0L
        var offset = 0
        val start = System.nanoTime()
        val durationNanos = durationMs * 1_000_000L
        while (System.nanoTime() - start < durationNanos) {
            copy(offset)
            totalBytes += RamBenchmark.CHUNK_BYTES
            offset += RamBenchmark.CHUNK_BYTES
            if (offset + RamBenchmark.CHUNK_BYTES > RamBenchmark.BUFFER_SIZE_BYTES) offset = 0
        }
        return totalBytes to (System.nanoTime() - start)
    }
}
