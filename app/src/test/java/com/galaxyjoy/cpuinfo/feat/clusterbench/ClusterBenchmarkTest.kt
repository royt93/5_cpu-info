package com.galaxyjoy.cpuinfo.feat.clusterbench

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterBenchmarkTest {

    @Test
    fun `shouldAbortForSafety is false below the threshold`() {
        assertFalse(ClusterBenchmark.shouldAbortForSafety(ClusterBenchmark.SAFETY_ABORT_TEMP_C - 1))
    }

    @Test
    fun `shouldAbortForSafety is true at and above the threshold`() {
        assertTrue(ClusterBenchmark.shouldAbortForSafety(ClusterBenchmark.SAFETY_ABORT_TEMP_C))
        assertTrue(ClusterBenchmark.shouldAbortForSafety(ClusterBenchmark.SAFETY_ABORT_TEMP_C + 5))
    }
}
