package com.galaxyjoy.cpuinfo.feat.rambench

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RamBenchmarkTest {

    @Test
    fun `mbPerSec converts bytes and nanoseconds to megabytes per second`() {
        // 32 MiB in exactly 2 seconds = 16 MiB/s
        val bytes = 32L * 1024 * 1024
        val result = RamBenchmark.mbPerSec(bytes, elapsedNanos = 2_000_000_000L)
        assertEquals(16.0, result, 0.001)
    }

    @Test
    fun `mbPerSec returns 0 for non-positive elapsed time instead of dividing by zero`() {
        assertEquals(0.0, RamBenchmark.mbPerSec(1_000_000L, elapsedNanos = 0L))
        assertEquals(0.0, RamBenchmark.mbPerSec(1_000_000L, elapsedNanos = -5L))
    }

    @Test
    fun `shouldAbortForSafety matches the same threshold as U02 throttle test`() {
        assertFalse(RamBenchmark.shouldAbortForSafety(RamBenchmark.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(RamBenchmark.shouldAbortForSafety(RamBenchmark.SAFETY_ABORT_TEMP_C))
        assertTrue(RamBenchmark.shouldAbortForSafety(RamBenchmark.SAFETY_ABORT_TEMP_C + 1))
    }

    @Test
    fun `hasEnoughMemory requires 4x the buffer size free, not just enough for one allocation`() {
        val buffer = RamBenchmark.BUFFER_SIZE_BYTES.toLong()
        // Exactly 4x free — boundary passes.
        assertTrue(RamBenchmark.hasEnoughMemory(maxMemoryBytes = buffer * 4, allocatedMemoryBytes = 0L))
        // Only 1x free (enough for the allocation itself, but no headroom) — must fail.
        assertFalse(RamBenchmark.hasEnoughMemory(maxMemoryBytes = buffer, allocatedMemoryBytes = 0L))
    }

    @Test
    fun `hasEnoughMemory accounts for memory already allocated by the rest of the app`() {
        val maxMemory = RamBenchmark.BUFFER_SIZE_BYTES.toLong() * 10
        // Already using 90% of the heap — not enough headroom left even though maxMemory is large.
        assertFalse(RamBenchmark.hasEnoughMemory(maxMemoryBytes = maxMemory, allocatedMemoryBytes = (maxMemory * 0.9).toLong()))
    }
}
