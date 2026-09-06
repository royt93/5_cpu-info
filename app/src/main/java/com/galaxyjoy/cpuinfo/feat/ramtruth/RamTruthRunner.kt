package com.galaxyjoy.cpuinfo.feat.ramtruth

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Drives the E03 growing-allocation workload on [Dispatchers.Default]. Allocates
 * [RamTruthBenchmark.CHUNK_SIZE_BYTES]-sized chunks via [ByteBuffer.allocateDirect] — **off the
 * regular Dalvik/ART heap** (not subject to the manifest `largeHeap`/`-Xmx` ceiling that would cap
 * a normal `ByteArray` allocation at a few hundred MB regardless of the device's real RAM), so
 * this can actually probe into the gigabyte range that a real-vs-virtual-RAM boundary would live
 * in. Each new chunk is "touched" (written, forcing the OS to commit a real physical page instead
 * of leaving it an unbacked virtual mapping) and timed; [RamTruthBenchmark] turns that series of
 * per-chunk throughputs into a verdict.
 *
 * Every allocated chunk is kept referenced until the run ends, then all references are dropped and
 * a GC requested — direct buffers have no synchronous free, so the underlying native memory is
 * only reclaimed once the GC actually runs the buffer's `Cleaner`, not the instant this function
 * returns. Checks `ActivityManager.MemoryInfo.lowMemory` before every single chunk (not just the
 * budget computed once up front) — respects Android's own real-time low-memory signal, not just
 * this test's own byte counter, since an OS-triggered low-memory kill of this process would be an
 * uncatchable `SIGKILL`, not a normal exception this code could handle.
 */
class RamTruthRunner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    sealed interface State {
        data class Running(val chunkIndex: Int, val chunkCount: Int) : State
        data class Finished(val result: RamTruthBenchmark.Result) : State
        data class Aborted(val reason: RamTruthBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(onState: suspend (State) -> Unit): Unit = withContext(Dispatchers.Default) {
        stopRequested = false
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val probeCapBytes = RamTruthBenchmark.probeCapBytes(memInfo.availMem)
        val chunkCount = (probeCapBytes / RamTruthBenchmark.CHUNK_SIZE_BYTES).toInt()
        val minChunksNeeded = RamTruthBenchmark.BASELINE_CHUNK_COUNT + RamTruthBenchmark.SUSTAINED_CLIFF_CHUNK_COUNT
        if (chunkCount < minChunksNeeded) {
            onState(State.Aborted(RamTruthBenchmark.AbortReason.INSUFFICIENT_AVAILABLE_MEMORY))
            return@withContext
        }

        val buffers = mutableListOf<ByteBuffer>()
        val measurements = mutableListOf<RamTruthBenchmark.ChunkMeasurement>()
        try {
            for (i in 0 until chunkCount) {
                if (stopRequested) {
                    onState(State.Aborted(RamTruthBenchmark.AbortReason.INTERRUPTED))
                    return@withContext
                }
                activityManager.getMemoryInfo(memInfo)
                if (memInfo.lowMemory) {
                    onState(State.Aborted(RamTruthBenchmark.AbortReason.LOW_MEMORY_SIGNAL))
                    return@withContext
                }
                onState(State.Running(i, chunkCount))
                val buffer = ByteBuffer.allocateDirect(RamTruthBenchmark.CHUNK_SIZE_BYTES)
                buffers += buffer
                measurements += RamTruthBenchmark.ChunkMeasurement(i, touchAndMeasureMbPerSec(buffer))
            }
            onState(State.Finished(RamTruthBenchmark.Result(measurements)))
        } catch (_: OutOfMemoryError) {
            // A hard allocation ceiling was hit — treat whatever was measured so far as the full
            // result rather than losing it; evaluate() handles a short list as INCONCLUSIVE.
            onState(State.Finished(RamTruthBenchmark.Result(measurements)))
        } finally {
            buffers.clear()
            System.gc()
        }
    }

    /** Writes one byte per page (not every byte) — a single write per page is enough to force the
     * OS to fault in and commit a real physical page for it, which is the event whose latency this
     * test actually cares about; touching every byte would just make the test slower for no extra
     * signal. */
    private fun touchAndMeasureMbPerSec(buffer: ByteBuffer): Double {
        val start = System.nanoTime()
        var offset = 0
        while (offset < buffer.capacity()) {
            buffer.put(offset, 1)
            offset += PAGE_STRIDE_BYTES
        }
        val elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0
        return if (elapsedSeconds > 0) (buffer.capacity() / (1024.0 * 1024.0)) / elapsedSeconds else 0.0
    }

    private companion object {
        /** Conservative stride: real page size is 4096 or 16384 depending on device (this app
         * already handles both for its native lib, see root `CLAUDE.md`) — touching every 4096
         * bytes always lands at least once per real page regardless of which size is in effect. */
        const val PAGE_STRIDE_BYTES = 4096
    }
}
