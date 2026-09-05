package com.galaxyjoy.cpuinfo.feat.gpubench

import com.galaxyjoy.cpuinfo.util.BenchmarkSafety

/**
 * Pure GPU micro-benchmark math (U15) — no GL/Android deps, unlike [GpuBenchmarkRunner] which
 * drives the actual `GLSurfaceView.Renderer` workload. No hardcoded score table (unlike a
 * calibrated CPU-Z-style score) — GPU driver/vendor spread (Mali/Adreno/PowerVR/Xclipse) is too
 * wide for a made-up scale to mean anything without real calibration data, so this only reports
 * raw average FPS and lets [GpuBenchResultPrefs] show a "vs last run" comparison, same convention
 * [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark]/[com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark]
 * already use.
 */
object GpuBenchmark {

    /** Runs a few unmeasured frames first so the GPU clocks ramp up before the timed window
     * starts — without this, the first measured frames would be artificially slow. */
    const val WARMUP_DURATION_MS = 1_000L
    const val MEASURE_DURATION_MS = 5_000L

    /** Same threshold as U02's [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint] and
     * U16's [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark]. */
    const val SAFETY_ABORT_TEMP_C = BenchmarkSafety.SAFETY_ABORT_TEMP_C

    enum class AbortReason { OVERHEAT, INTERRUPTED }

    data class Result(
        val avgFps: Double,
        val frameCount: Long,
        val durationMs: Long,
    )

    fun shouldAbortForSafety(tempC: Int): Boolean = BenchmarkSafety.shouldAbortForSafety(tempC)

    fun fps(frameCount: Long, elapsedNanos: Long): Double {
        if (elapsedNanos <= 0L) return 0.0
        val seconds = elapsedNanos / 1_000_000_000.0
        return frameCount / seconds
    }
}
