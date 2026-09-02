package com.galaxyjoy.cpuinfo.feat.storagebench

/**
 * Pure storage/CPU micro-benchmark math (F06) — no Android deps. [StorageBenchmarkRunner] drives
 * the actual I/O + hash workload and feeds raw byte/op counts + elapsed time here.
 *
 * Deliberately narrower than the "CPU stress test" idea previously skipped in doc/task/quick_win.md
 * over battery/thermal risk (see also U02's [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint]
 * for that reasoning): this is short (a few seconds of file I/O in the app's own cache, no
 * sustained multi-core burn), so it reuses the same conservative battery-temperature gate but
 * doesn't need a hard duration cap or live sampling loop.
 */
object StorageBenchmark {

    const val SEQ_FILE_SIZE_BYTES = 32L * 1024 * 1024
    const val SEQ_CHUNK_BYTES = 1 * 1024 * 1024
    const val RANDOM_FILE_SIZE_BYTES = 8L * 1024 * 1024
    const val RANDOM_BLOCK_BYTES = 4 * 1024
    const val RANDOM_OPS_COUNT = 500
    const val HASH_DURATION_MS = 1_000L
    const val HASH_BUFFER_BYTES = 256 * 1024

    /** Same threshold as U02's [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint]. */
    const val SAFETY_ABORT_TEMP_C = 43

    data class Result(
        val seqWriteMbPerSec: Double,
        val seqReadMbPerSec: Double,
        val randomWriteOpsPerSec: Double,
        val randomReadOpsPerSec: Double,
        val hashMbPerSec: Double,
    )

    fun shouldAbortForSafety(tempC: Int): Boolean = tempC >= SAFETY_ABORT_TEMP_C

    fun mbPerSec(bytes: Long, elapsedNanos: Long): Double {
        if (elapsedNanos <= 0L) return 0.0
        val seconds = elapsedNanos / 1_000_000_000.0
        return (bytes / (1024.0 * 1024.0)) / seconds
    }

    fun opsPerSec(ops: Int, elapsedNanos: Long): Double {
        if (elapsedNanos <= 0L) return 0.0
        val seconds = elapsedNanos / 1_000_000_000.0
        return ops / seconds
    }
}
