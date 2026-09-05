package com.galaxyjoy.cpuinfo.feat.rambench

import com.galaxyjoy.cpuinfo.util.BenchmarkSafety

/**
 * Pure RAM micro-benchmark math (U16) — no Android deps. [RamBenchmarkRunner] drives the actual
 * memory read/write workload and feeds raw byte counts + elapsed time here. Same shape as F06's
 * [com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark], reusing its `mbPerSec`/safety-gate
 * pattern, but measuring in-memory `ByteArray` copy throughput instead of file I/O.
 */
object RamBenchmark {

    /** Single scratch buffer size. Kept well under typical Android per-app heap limits (usually
     * 192MB+ even on low-end/minSdk=24 devices) — [RamBenchmarkRunner] additionally checks
     * [Runtime.maxMemory] before allocating, since this is the one benchmark in this app that
     * allocates a large buffer up front rather than streaming small chunks. */
    const val BUFFER_SIZE_BYTES = 32 * 1024 * 1024
    const val CHUNK_BYTES = 1 * 1024 * 1024
    const val WRITE_DURATION_MS = 500L
    const val READ_DURATION_MS = 500L

    /** Same threshold as U02's [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint]. */
    const val SAFETY_ABORT_TEMP_C = BenchmarkSafety.SAFETY_ABORT_TEMP_C

    enum class AbortReason { OVERHEAT, INSUFFICIENT_MEMORY }

    data class Result(
        val writeMbPerSec: Double,
        val readMbPerSec: Double,
    )

    fun shouldAbortForSafety(tempC: Int): Boolean = BenchmarkSafety.shouldAbortForSafety(tempC)

    /** Requires at least 4x the scratch buffer free in the JVM heap before allocating — leaves
     * headroom for the rest of the app + GC, rather than allocating right up to the limit. */
    fun hasEnoughMemory(maxMemoryBytes: Long, allocatedMemoryBytes: Long): Boolean =
        (maxMemoryBytes - allocatedMemoryBytes) >= BUFFER_SIZE_BYTES * 4L

    fun mbPerSec(bytes: Long, elapsedNanos: Long): Double {
        if (elapsedNanos <= 0L) return 0.0
        val seconds = elapsedNanos / 1_000_000_000.0
        return (bytes / (1024.0 * 1024.0)) / seconds
    }
}
