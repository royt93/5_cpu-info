package com.galaxyjoy.cpuinfo.feat.sensortruth

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Drives the E02 per-sensor audit: for each of [AUDITED_SENSOR_TYPES] (accelerometer, gyroscope —
 * the two continuous, high-rate motion sensors the "fakes a spec it can't sustain" fraud this
 * feature targets actually applies to; on-change sensors like light/proximity don't have a
 * meaningful "sample rate" to audit the same way), registers a real listener at
 * `SENSOR_DELAY_FASTEST` for [SensorTruthBenchmark.TEST_DURATION_MS], collects real event
 * timestamps, and hands them to [SensorTruthBenchmark.computeAudit].
 */
class SensorTruthRunner @Inject constructor(
    private val sensorManager: SensorManager,
) {

    sealed interface State {
        data class Running(val sensorIndex: Int, val sensorCount: Int, val sensorType: Int) : State
        data class Finished(val result: SensorTruthBenchmark.Result) : State
        data class Aborted(val reason: SensorTruthBenchmark.AbortReason) : State
    }

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun run(onState: suspend (State) -> Unit) {
        stopRequested = false
        val audits = mutableListOf<SensorTruthBenchmark.SensorAudit>()

        for ((index, sensorType) in AUDITED_SENSOR_TYPES.withIndex()) {
            if (stopRequested) {
                onState(State.Aborted(SensorTruthBenchmark.AbortReason.INTERRUPTED))
                return
            }
            onState(State.Running(index, AUDITED_SENSOR_TYPES.size, sensorType))
            audits += auditSensor(sensorType)
        }

        onState(State.Finished(SensorTruthBenchmark.Result(audits)))
    }

    private suspend fun auditSensor(sensorType: Int): SensorTruthBenchmark.SensorAudit {
        val sensor = sensorManager.getDefaultSensor(sensorType)
            ?: return SensorTruthBenchmark.SensorAudit(sensorType, advertisedHz = null, actualHz = 0.0, jitterMs = 0.0, eventCount = 0)

        val advertisedHz = sensor.minDelay.takeIf { it > 0 }?.let { 1_000_000.0 / it }
        val timestampsNanos = mutableListOf<Long>()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                timestampsNanos += event.timestamp
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Do nothing
            }
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        try {
            var elapsedMs = 0L
            while (elapsedMs < SensorTruthBenchmark.TEST_DURATION_MS && !stopRequested) {
                delay(POLL_INTERVAL_MS)
                elapsedMs += POLL_INTERVAL_MS
            }
        } finally {
            sensorManager.unregisterListener(listener)
        }

        return SensorTruthBenchmark.computeAudit(sensorType, advertisedHz, timestampsNanos)
    }

    companion object {
        val AUDITED_SENSOR_TYPES = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE)
        private const val POLL_INTERVAL_MS = 100L
    }
}
