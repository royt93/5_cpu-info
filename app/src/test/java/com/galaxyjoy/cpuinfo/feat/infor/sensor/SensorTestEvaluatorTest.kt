package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SensorTestEvaluatorTest {

    @Test
    fun `accelerometer detects a large enough shake`() {
        val baseline = floatArrayOf(0f, 0f, 9.8f)
        val shaken = floatArrayOf(6f, 0f, 9.8f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_ACCELEROMETER, baseline, shaken))
    }

    @Test
    fun `accelerometer ignores small idle noise`() {
        val baseline = floatArrayOf(0f, 0f, 9.8f)
        val idleNoise = floatArrayOf(0.1f, 0f, 9.85f)

        assertFalse(SensorTestEvaluator.isActionDetected(Sensor.TYPE_ACCELEROMETER, baseline, idleNoise))
    }

    @Test
    fun `gyroscope detects a quick rotation`() {
        val baseline = floatArrayOf(0f, 0f, 0f)
        val rotated = floatArrayOf(0f, 2f, 0f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_GYROSCOPE, baseline, rotated))
    }

    @Test
    fun `magnetic field detects a nearby metal object`() {
        val baseline = floatArrayOf(20f, 10f, -30f)
        val disturbed = floatArrayOf(40f, 10f, -30f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_MAGNETIC_FIELD, baseline, disturbed))
    }

    @Test
    fun `light sensor detects covering with a hand`() {
        val baseline = floatArrayOf(300f)
        val covered = floatArrayOf(2f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_LIGHT, baseline, covered))
    }

    @Test
    fun `light sensor ignores tiny flicker`() {
        val baseline = floatArrayOf(300f)
        val flicker = floatArrayOf(305f)

        assertFalse(SensorTestEvaluator.isActionDetected(Sensor.TYPE_LIGHT, baseline, flicker))
    }

    @Test
    fun `proximity detects any change from baseline`() {
        val far = floatArrayOf(5f)
        val near = floatArrayOf(0f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_PROXIMITY, far, near))
    }

    @Test
    fun `proximity reports no change when reading is identical`() {
        val far = floatArrayOf(5f)

        assertFalse(SensorTestEvaluator.isActionDetected(Sensor.TYPE_PROXIMITY, far, far))
    }

    @Test
    fun `pressure detects a noticeable change`() {
        val baseline = floatArrayOf(1013.2f)
        val changed = floatArrayOf(1012.5f)

        assertTrue(SensorTestEvaluator.isActionDetected(Sensor.TYPE_PRESSURE, baseline, changed))
    }

    @Test
    fun `pressure ignores tiny ambient drift`() {
        val baseline = floatArrayOf(1013.2f)
        val drift = floatArrayOf(1013.25f)

        assertFalse(SensorTestEvaluator.isActionDetected(Sensor.TYPE_PRESSURE, baseline, drift))
    }

    @Test
    fun `unsupported sensor type is never detected`() {
        val baseline = floatArrayOf(1f)
        val current = floatArrayOf(999f)

        assertFalse(SensorTestEvaluator.isActionDetected(Sensor.TYPE_STEP_COUNTER, baseline, current))
    }

    @Test
    fun `formatLiveValue reports vector magnitude for accelerometer`() {
        val formatted = SensorTestEvaluator.formatLiveValue(Sensor.TYPE_ACCELEROMETER, floatArrayOf(0f, 0f, 9.8f))

        assertEquals("9.8 m/s²", formatted)
    }

    @Test
    fun `formatLiveValue reports raw scalar for light`() {
        val formatted = SensorTestEvaluator.formatLiveValue(Sensor.TYPE_LIGHT, floatArrayOf(42.3f))

        assertEquals("42.3 lx", formatted)
    }

    @Test
    fun `target sensor types are in a fixed easiest-to-hardest order`() {
        assertEquals(
            listOf(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_LIGHT,
                Sensor.TYPE_PROXIMITY,
                Sensor.TYPE_PRESSURE,
            ),
            SensorTestEvaluator.TARGET_SENSOR_TYPES,
        )
    }
}
