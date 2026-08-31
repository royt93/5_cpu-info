package com.galaxyjoy.cpuinfo.feat.storagebench

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageBenchmarkTest {

    @Test
    fun `mbPerSec converts bytes and nanoseconds to megabytes per second`() {
        // 32 MiB in exactly 2 seconds = 16 MiB/s
        val bytes = 32L * 1024 * 1024
        val result = StorageBenchmark.mbPerSec(bytes, elapsedNanos = 2_000_000_000L)
        assertEquals(16.0, result, 0.001)
    }

    @Test
    fun `mbPerSec returns 0 for non-positive elapsed time instead of dividing by zero`() {
        assertEquals(0.0, StorageBenchmark.mbPerSec(1_000_000L, elapsedNanos = 0L))
        assertEquals(0.0, StorageBenchmark.mbPerSec(1_000_000L, elapsedNanos = -5L))
    }

    @Test
    fun `opsPerSec converts op count and nanoseconds to ops per second`() {
        val result = StorageBenchmark.opsPerSec(ops = 500, elapsedNanos = 2_000_000_000L)
        assertEquals(250.0, result, 0.001)
    }

    @Test
    fun `opsPerSec returns 0 for non-positive elapsed time instead of dividing by zero`() {
        assertEquals(0.0, StorageBenchmark.opsPerSec(ops = 500, elapsedNanos = 0L))
    }

    @Test
    fun `shouldAbortForSafety matches the same threshold as the U02 throttle test`() {
        assertFalse(StorageBenchmark.shouldAbortForSafety(StorageBenchmark.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(StorageBenchmark.shouldAbortForSafety(StorageBenchmark.SAFETY_ABORT_TEMP_C))
        assertTrue(StorageBenchmark.shouldAbortForSafety(StorageBenchmark.SAFETY_ABORT_TEMP_C + 1))
    }
}
