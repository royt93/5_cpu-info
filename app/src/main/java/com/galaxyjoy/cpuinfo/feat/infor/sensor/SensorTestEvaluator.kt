package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import com.galaxyjoy.cpuinfo.util.round1
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * F07 "Interactive Sensor Test Suite" — decides whether a user's physical action (shake, rotate,
 * cover, blow) produced a big-enough change from the reading captured when a step started to
 * count as "detected". Thresholds are tuned to be comfortably triggered by a deliberate action
 * while ignoring idle/ambient noise — this is a best-effort check, not a calibrated instrument.
 */
object SensorTestEvaluator {

    /** Order shown to the user — grouped roughly from "easiest to trigger" to "hardest". */
    val TARGET_SENSOR_TYPES: List<Int> = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_PRESSURE,
    )

    private const val ACCELEROMETER_DELTA_THRESHOLD = 4f
    private const val GYROSCOPE_DELTA_THRESHOLD = 1f
    private const val MAGNETIC_FIELD_DELTA_THRESHOLD = 15f
    private const val LIGHT_DELTA_THRESHOLD = 15f
    private const val PRESSURE_DELTA_THRESHOLD = 0.3f

    /**
     * @param baseline the first reading captured when the step started.
     * @param current the latest reading.
     */
    fun isActionDetected(sensorType: Int, baseline: FloatArray, current: FloatArray): Boolean =
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> vectorDelta(baseline, current) > ACCELEROMETER_DELTA_THRESHOLD
            Sensor.TYPE_GYROSCOPE -> vectorDelta(baseline, current) > GYROSCOPE_DELTA_THRESHOLD
            Sensor.TYPE_MAGNETIC_FIELD -> vectorDelta(baseline, current) > MAGNETIC_FIELD_DELTA_THRESHOLD
            Sensor.TYPE_LIGHT -> abs(current[0] - baseline[0]) > LIGHT_DELTA_THRESHOLD
            // Proximity is typically a binary near/far sensor on Android — any change from the
            // starting reading (usually "far") is a real detected event, no threshold needed.
            Sensor.TYPE_PROXIMITY -> current.isNotEmpty() && baseline.isNotEmpty() && current[0] != baseline[0]
            Sensor.TYPE_PRESSURE -> abs(current[0] - baseline[0]) > PRESSURE_DELTA_THRESHOLD
            else -> false
        }

    private fun vectorDelta(baseline: FloatArray, current: FloatArray): Float {
        val count = minOf(baseline.size, current.size)
        if (count == 0) return 0f
        var sumSquares = 0f
        for (i in 0 until count) {
            val diff = current[i] - baseline[i]
            sumSquares += diff * diff
        }
        return sqrt(sumSquares)
    }

    /** Human-readable live value for the running step's readout — mirrors [VMSensorsInfo]'s units. */
    fun formatLiveValue(sensorType: Int, values: FloatArray): String = when (sensorType) {
        Sensor.TYPE_ACCELEROMETER -> "${vectorMagnitude(values).round1()} m/s²"
        Sensor.TYPE_GYROSCOPE -> "${vectorMagnitude(values).round1()} rad/s"
        Sensor.TYPE_MAGNETIC_FIELD -> "${vectorMagnitude(values).round1()} µT"
        Sensor.TYPE_LIGHT -> "${values.getOrElse(0) { 0f }.round1()} lx"
        Sensor.TYPE_PROXIMITY -> "${values.getOrElse(0) { 0f }.round1()} cm"
        Sensor.TYPE_PRESSURE -> "${values.getOrElse(0) { 0f }.round1()} hPa"
        else -> ""
    }

    private fun vectorMagnitude(values: FloatArray): Float {
        var sumSquares = 0f
        for (v in values) sumSquares += v * v
        return sqrt(sumSquares)
    }
}
