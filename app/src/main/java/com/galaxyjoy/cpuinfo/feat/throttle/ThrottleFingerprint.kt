package com.galaxyjoy.cpuinfo.feat.throttle

import kotlin.math.roundToLong

/**
 * Pure throttle-test math (U02) — no Android deps. [ThrottleTestRunner] drives the actual
 * stress workload + sampling and feeds collected [Sample]s here to compute a verdict.
 *
 * Test is hard-capped at [TEST_DURATION_MS] and self-aborts if battery temperature reaches
 * [SAFETY_ABORT_TEMP_C] — this is a deliberately conservative safety-limited redesign of the
 * "CPU stress test" idea previously skipped in doc/quick_win.md over battery/thermal risk.
 */
object ThrottleFingerprint {

    const val TEST_DURATION_MS = 30_000L
    const val SAMPLE_INTERVAL_MS = 1_000L
    const val SAFETY_ABORT_TEMP_C = 43
    const val THROTTLE_THRESHOLD_PERCENT = 15
    private const val SUSTAINED_WINDOW_MS = 5_000L

    data class Sample(
        val elapsedMs: Long,
        val avgFreqMhz: Long,
        val tempC: Int,
    )

    enum class AbortReason { OVERHEAT, USER_STOPPED }

    data class Result(
        val peakFreqMhz: Long,
        val sustainedFreqMhz: Long,
        val throttlePercent: Int,
        val throttled: Boolean,
        val startTempC: Int,
        val maxTempC: Int,
        val durationMs: Long,
        val aborted: Boolean,
        val abortReason: AbortReason?,
    )

    fun shouldAbortForSafety(tempC: Int): Boolean = tempC >= SAFETY_ABORT_TEMP_C

    fun shouldStop(elapsedMs: Long): Boolean = elapsedMs >= TEST_DURATION_MS

    /**
     * @return null only when [samples] is empty (test cancelled before the first sample tick).
     */
    fun evaluate(samples: List<Sample>, aborted: Boolean, abortReason: AbortReason?): Result? {
        if (samples.isEmpty()) return null

        val peakFreqMhz = samples.maxOf { it.avgFreqMhz }
        val sustainedWindowStart = samples.last().elapsedMs - SUSTAINED_WINDOW_MS
        val sustainedSamples = samples.filter { it.elapsedMs >= sustainedWindowStart }
        val sustainedFreqMhz = sustainedSamples.map { it.avgFreqMhz }.average().roundToLong()

        val throttlePercent = if (peakFreqMhz > 0) {
            (((peakFreqMhz - sustainedFreqMhz) * 100) / peakFreqMhz).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return Result(
            peakFreqMhz = peakFreqMhz,
            sustainedFreqMhz = sustainedFreqMhz,
            throttlePercent = throttlePercent,
            throttled = throttlePercent >= THROTTLE_THRESHOLD_PERCENT,
            startTempC = samples.first().tempC,
            maxTempC = samples.maxOf { it.tempC },
            durationMs = samples.last().elapsedMs,
            aborted = aborted,
            abortReason = abortReason,
        )
    }
}
