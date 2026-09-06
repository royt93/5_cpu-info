package com.galaxyjoy.cpuinfo.feat.sensortruth

import android.hardware.Sensor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensorTruthBenchmarkTest {

    private fun timestampsAtRate(hz: Double, count: Int, startNanos: Long = 0L): List<Long> {
        val intervalNanos = (1_000_000_000.0 / hz).toLong()
        return (0 until count).map { startNanos + it * intervalNanos }
    }

    @Test
    fun `computeAudit reports full eventCount and near-zero jitter for a perfectly steady stream`() {
        val timestamps = timestampsAtRate(hz = 100.0, count = 50)

        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, advertisedHz = 100.0, timestamps)

        assertEquals(50, audit.eventCount)
        assertEquals(100.0, audit.actualHz, 1.0)
        assertTrue(audit.jitterMs < 1.0)
    }

    @Test
    fun `computeAudit with fewer than 2 timestamps reports zero rate without crashing`() {
        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_GYROSCOPE, advertisedHz = 100.0, listOf(1_000_000L))

        assertEquals(1, audit.eventCount)
        assertEquals(0.0, audit.actualHz)
    }

    @Test
    fun `evaluate is GENUINE when delivered rate matches the advertised rate`() {
        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, advertisedHz = 100.0, timestampsAtRate(100.0, 50))
        assertEquals(SensorTruthBenchmark.Verdict.GENUINE, SensorTruthBenchmark.evaluate(audit))
    }

    @Test
    fun `evaluate is SUSPECT_UNDERDELIVERING when delivered rate is far below advertised`() {
        // Advertises 200Hz but only actually delivers ~20Hz.
        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_GYROSCOPE, advertisedHz = 200.0, timestampsAtRate(20.0, 50))
        assertEquals(SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING, SensorTruthBenchmark.evaluate(audit))
    }

    @Test
    fun `evaluate is INCONCLUSIVE when there is no advertised rate to compare against`() {
        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, advertisedHz = null, timestampsAtRate(100.0, 50))
        assertEquals(SensorTruthBenchmark.Verdict.INCONCLUSIVE, SensorTruthBenchmark.evaluate(audit))
    }

    @Test
    fun `evaluate is INCONCLUSIVE when the sensor is missing (zero events)`() {
        val audit = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_GYROSCOPE, advertisedHz = null, actualHz = 0.0, jitterMs = 0.0, eventCount = 0)
        assertEquals(SensorTruthBenchmark.Verdict.INCONCLUSIVE, SensorTruthBenchmark.evaluate(audit))
    }

    @Test
    fun `overallVerdict is SUSPECT_UNDERDELIVERING if any single sensor is flagged`() {
        val genuine = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, 100.0, timestampsAtRate(100.0, 50))
        val suspect = SensorTruthBenchmark.computeAudit(Sensor.TYPE_GYROSCOPE, 200.0, timestampsAtRate(20.0, 50))

        val overall = SensorTruthBenchmark.overallVerdict(SensorTruthBenchmark.Result(listOf(genuine, suspect)))

        assertEquals(SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING, overall)
    }

    @Test
    fun `overallVerdict is GENUINE when at least one sensor is genuine and none are suspect`() {
        val genuine = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, 100.0, timestampsAtRate(100.0, 50))
        val inconclusive = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_GYROSCOPE, null, 0.0, 0.0, 0)

        val overall = SensorTruthBenchmark.overallVerdict(SensorTruthBenchmark.Result(listOf(genuine, inconclusive)))

        assertEquals(SensorTruthBenchmark.Verdict.GENUINE, overall)
    }

    @Test
    fun `overallVerdict is INCONCLUSIVE when every sensor is inconclusive`() {
        val a = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_ACCELEROMETER, null, 0.0, 0.0, 0)
        val b = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_GYROSCOPE, null, 0.0, 0.0, 0)

        assertEquals(SensorTruthBenchmark.Verdict.INCONCLUSIVE, SensorTruthBenchmark.overallVerdict(SensorTruthBenchmark.Result(listOf(a, b))))
    }

    @Test
    fun `computeAudit measures nonzero jitter for an irregular stream`() {
        val timestamps = listOf(0L, 10_000_000L, 12_000_000L, 25_000_000L, 26_000_000L)

        val audit = SensorTruthBenchmark.computeAudit(Sensor.TYPE_ACCELEROMETER, advertisedHz = null, timestamps)

        assertTrue(audit.jitterMs > 0.0)
    }
}
