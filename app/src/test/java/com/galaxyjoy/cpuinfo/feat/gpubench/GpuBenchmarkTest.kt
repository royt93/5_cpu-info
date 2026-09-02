package com.galaxyjoy.cpuinfo.feat.gpubench

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpuBenchmarkTest {

    @Test
    fun `fps converts frame count and nanoseconds to frames per second`() {
        // 300 frames in exactly 5 seconds = 60 FPS
        val result = GpuBenchmark.fps(frameCount = 300L, elapsedNanos = 5_000_000_000L)
        assertEquals(60.0, result, 0.001)
    }

    @Test
    fun `fps returns 0 for non-positive elapsed time instead of dividing by zero`() {
        assertEquals(0.0, GpuBenchmark.fps(frameCount = 100L, elapsedNanos = 0L))
        assertEquals(0.0, GpuBenchmark.fps(frameCount = 100L, elapsedNanos = -5L))
    }

    @Test
    fun `shouldAbortForSafety matches the same threshold as U02U16 throttle tests`() {
        assertFalse(GpuBenchmark.shouldAbortForSafety(GpuBenchmark.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(GpuBenchmark.shouldAbortForSafety(GpuBenchmark.SAFETY_ABORT_TEMP_C))
        assertTrue(GpuBenchmark.shouldAbortForSafety(GpuBenchmark.SAFETY_ABORT_TEMP_C + 1))
    }
}
