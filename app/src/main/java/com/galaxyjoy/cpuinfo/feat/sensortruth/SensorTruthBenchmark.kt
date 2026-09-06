package com.galaxyjoy.cpuinfo.feat.sensortruth

import kotlin.math.sqrt

/**
 * Pure E02 "Sensor Truth" model — no Android deps. [SensorTruthRunner] registers a real
 * `SensorEventListener` at `SENSOR_DELAY_FASTEST` and feeds the raw event timestamps here.
 *
 * Detects sensors that don't deliver what they advertise: budget devices sometimes report a
 * gyroscope/accelerometer capable of a high sample rate (`Sensor.getMinDelay()`) but the driver
 * never actually sustains it — a well-documented class of "spec sheet vs reality" fraud distinct
 * from (but in the same "Truth" family as) [com.galaxyjoy.cpuinfo.feat.storagetruth]/[com.galaxyjoy.cpuinfo.feat.ramtruth].
 */
object SensorTruthBenchmark {

    const val TEST_DURATION_MS = 3_000L

    /** Below this many events in [TEST_DURATION_MS], the sample is too small to say anything
     * meaningful about sustained rate — same reasoning as every other Truth-series minimum. */
    const val MIN_EVENTS_FOR_CONCLUSION = 10

    /** A sensor delivering less than this fraction of its advertised rate is flagged — chosen
     * loosely (not tight) since real event delivery always has some scheduling overhead even on
     * genuine hardware; only a dramatic shortfall counts as suspect. */
    const val SUSPECT_RATIO = 0.5

    enum class Verdict { GENUINE, SUSPECT_UNDERDELIVERING, INCONCLUSIVE }

    enum class AbortReason { INTERRUPTED }

    data class SensorAudit(
        val sensorType: Int,
        /** `null` if the sensor is missing on this device, or its driver doesn't declare a
         * meaningful minimum delay (`Sensor.getMinDelay() <= 0`) — either way, nothing to compare
         * the measured rate against. */
        val advertisedHz: Double?,
        val actualHz: Double,
        val jitterMs: Double,
        val eventCount: Int,
    )

    data class Result(val audits: List<SensorAudit>)

    fun evaluate(audit: SensorAudit): Verdict = when {
        audit.eventCount < MIN_EVENTS_FOR_CONCLUSION -> Verdict.INCONCLUSIVE
        audit.advertisedHz == null || audit.advertisedHz <= 0.0 -> Verdict.INCONCLUSIVE
        audit.actualHz < audit.advertisedHz * SUSPECT_RATIO -> Verdict.SUSPECT_UNDERDELIVERING
        else -> Verdict.GENUINE
    }

    /** Worst verdict across all audits, for a single at-a-glance icon — [Verdict.SUSPECT_UNDERDELIVERING]
     * beats [Verdict.INCONCLUSIVE] beats [Verdict.GENUINE], so one bad sensor is never hidden
     * behind an average. */
    fun overallVerdict(result: Result): Verdict {
        val verdicts = result.audits.map { evaluate(it) }
        return when {
            verdicts.isEmpty() -> Verdict.INCONCLUSIVE
            verdicts.any { it == Verdict.SUSPECT_UNDERDELIVERING } -> Verdict.SUSPECT_UNDERDELIVERING
            verdicts.all { it == Verdict.INCONCLUSIVE } -> Verdict.INCONCLUSIVE
            else -> Verdict.GENUINE
        }
    }

    /** Pure computation from real event timestamps (nanoseconds, monotonic — i.e. [android.hardware.SensorEvent.timestamp])
     * — decoupled from [android.hardware.SensorEventListener] so it's unit-testable with
     * fabricated timestamps, no device or mocked sensor framework needed. */
    fun computeAudit(sensorType: Int, advertisedHz: Double?, timestampsNanos: List<Long>): SensorAudit {
        if (timestampsNanos.size < 2) {
            return SensorAudit(sensorType, advertisedHz, actualHz = 0.0, jitterMs = 0.0, eventCount = timestampsNanos.size)
        }
        val intervalsMs = timestampsNanos.sorted().zipWithNext { a, b -> (b - a) / 1_000_000.0 }
        val meanIntervalMs = intervalsMs.average()
        val actualHz = if (meanIntervalMs > 0) 1000.0 / meanIntervalMs else 0.0
        val variance = intervalsMs.sumOf { val d = it - meanIntervalMs; d * d } / intervalsMs.size
        return SensorAudit(sensorType, advertisedHz, actualHz, jitterMs = sqrt(variance), eventCount = timestampsNanos.size)
    }
}
