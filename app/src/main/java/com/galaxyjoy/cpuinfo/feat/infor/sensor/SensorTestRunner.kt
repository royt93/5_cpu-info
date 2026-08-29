package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Drives one F07 test step at a time: registers a listener for [sensorType], captures the first
 * reading as baseline, and waits until [SensorTestEvaluator.isActionDetected] fires, the caller
 * requests a skip, or [STEP_TIMEOUT_MS] elapses — whichever comes first. One instance is owned
 * per [VMSensorTest], not a singleton; [skipRequested] is per-step state.
 */
class SensorTestRunner @Inject constructor(
    private val sensorManager: SensorManager,
) {

    enum class StepOutcome { DETECTED, SKIPPED, TIMED_OUT, UNAVAILABLE }

    data class StepResult(val sensorType: Int, val outcome: StepOutcome)

    @Volatile
    private var skipRequested = false

    fun requestSkip() {
        skipRequested = true
    }

    suspend fun runStep(sensorType: Int, onLiveValue: (FloatArray) -> Unit): StepOutcome {
        skipRequested = false
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return StepOutcome.UNAVAILABLE

        var baseline: FloatArray? = null
        var detected = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values.copyOf()
                onLiveValue(values)
                val base = baseline
                if (base == null) {
                    baseline = values
                    return
                }
                if (SensorTestEvaluator.isActionDetected(sensorType, base, values)) {
                    detected = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Do nothing
            }
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        try {
            withTimeoutOrNull(STEP_TIMEOUT_MS) {
                while (!detected && !skipRequested) {
                    delay(POLL_INTERVAL_MS)
                }
            }
        } finally {
            sensorManager.unregisterListener(listener)
        }

        return when {
            detected -> StepOutcome.DETECTED
            skipRequested -> StepOutcome.SKIPPED
            else -> StepOutcome.TIMED_OUT
        }
    }

    companion object {
        const val STEP_TIMEOUT_MS = 12_000L
        private const val POLL_INTERVAL_MS = 100L
    }
}
