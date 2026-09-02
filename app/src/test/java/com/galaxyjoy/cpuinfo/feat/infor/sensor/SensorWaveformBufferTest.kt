package com.galaxyjoy.cpuinfo.feat.infor.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorWaveformBufferTest {

    @Test
    fun `snapshot is empty per axis before any record`() {
        val buffer = SensorWaveformBuffer(axisCount = 3)

        assertEquals(listOf(emptyList<Float>(), emptyList(), emptyList()), buffer.snapshot())
    }

    @Test
    fun `record appends one value per axis in order`() {
        val buffer = SensorWaveformBuffer(axisCount = 3)

        buffer.record(floatArrayOf(1f, 2f, 3f))
        buffer.record(floatArrayOf(4f, 5f, 6f))

        assertEquals(listOf(1f, 4f), buffer.snapshot()[0])
        assertEquals(listOf(2f, 5f), buffer.snapshot()[1])
        assertEquals(listOf(3f, 6f), buffer.snapshot()[2])
    }

    @Test
    fun `oldest sample is evicted once maxSamples is exceeded`() {
        val buffer = SensorWaveformBuffer(axisCount = 1, maxSamples = 3)

        buffer.record(floatArrayOf(1f))
        buffer.record(floatArrayOf(2f))
        buffer.record(floatArrayOf(3f))
        buffer.record(floatArrayOf(4f))

        assertEquals(listOf(2f, 3f, 4f), buffer.snapshot()[0])
    }

    @Test
    fun `single-axis buffer only reads the first element of values`() {
        val buffer = SensorWaveformBuffer(axisCount = 1)

        // Extra elements (e.g. a 3-axis reading passed by mistake) must not crash or leak in.
        buffer.record(floatArrayOf(42f, 99f, 100f))

        assertEquals(listOf(42f), buffer.snapshot()[0])
        assertTrue(buffer.snapshot().size == 1)
    }
}
