package com.galaxyjoy.cpuinfo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkSafetyTest {

    @Test
    fun `shouldAbortForSafety is false below the threshold`() {
        assertFalse(BenchmarkSafety.shouldAbortForSafety(BenchmarkSafety.SAFETY_ABORT_TEMP_C - 1))
    }

    @Test
    fun `shouldAbortForSafety is true at and above the threshold`() {
        assertTrue(BenchmarkSafety.shouldAbortForSafety(BenchmarkSafety.SAFETY_ABORT_TEMP_C))
        assertTrue(BenchmarkSafety.shouldAbortForSafety(BenchmarkSafety.SAFETY_ABORT_TEMP_C + 5))
    }
}
