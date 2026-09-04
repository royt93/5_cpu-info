package com.galaxyjoy.cpuinfo.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class OsUpdateImpactCalculatorTest {

    @Test
    fun `empty history returns null`() {
        assertNull(OsUpdateImpactCalculator.detectImpact(emptyList()))
    }

    @Test
    fun `single known entry returns null - nothing to compare against`() {
        assertNull(OsUpdateImpactCalculator.detectImpact(listOf("build_a" to 100.0)))
    }

    @Test
    fun `all entries share the same fingerprint - no update detected yet`() {
        val entries = listOf("build_a" to 100.0, "build_a" to 105.0, "build_a" to 98.0)

        assertNull(OsUpdateImpactCalculator.detectImpact(entries))
    }

    @Test
    fun `entries with null or blank fingerprints are ignored`() {
        val entries = listOf(null to 100.0, "" to 50.0, "  " to 60.0)

        assertNull(OsUpdateImpactCalculator.detectImpact(entries))
    }

    @Test
    fun `a clear improvement after an update is detected correctly`() {
        // before: 100, after: 110 -> +10%
        val entries = listOf("build_a" to 100.0, "build_b" to 110.0)

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals("build_a", result?.previousBuildFingerprint)
        assertEquals(100.0, result?.previousAvgValue)
        assertEquals("build_b", result?.currentBuildFingerprint)
        assertEquals(110.0, result?.currentAvgValue)
        assertEquals(10, result?.percentChange)
    }

    @Test
    fun `a clear regression after an update is detected correctly`() {
        // before: 100, after: 80 -> -20%
        val entries = listOf("build_a" to 100.0, "build_b" to 80.0)

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals(-20, result?.percentChange)
    }

    @Test
    fun `averages each build's group of runs, not just the single closest run`() {
        // build_a: avg of (90, 110) = 100; build_b: avg of (198, 202) = 200 -> +100%
        val entries = listOf(
            "build_a" to 90.0, "build_a" to 110.0,
            "build_b" to 198.0, "build_b" to 202.0,
        )

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals(100.0, result?.previousAvgValue)
        assertEquals(200.0, result?.currentAvgValue)
        assertEquals(100, result?.percentChange)
    }

    @Test
    fun `only the most recent build transition matters when there are multiple updates`() {
        // build_a -> build_b -> build_c: only compares b (previous) vs c (current)
        val entries = listOf("build_a" to 50.0, "build_b" to 100.0, "build_c" to 150.0)

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals("build_b", result?.previousBuildFingerprint)
        assertEquals(100.0, result?.previousAvgValue)
        assertEquals("build_c", result?.currentBuildFingerprint)
        assertEquals(150.0, result?.currentAvgValue)
        assertEquals(50, result?.percentChange)
    }

    @Test
    fun `zero previous average does not crash with a divide-by-zero - reports 0 percent`() {
        val entries = listOf("build_a" to 0.0, "build_b" to 50.0)

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals(0, result?.percentChange)
    }

    @Test
    fun `unknown fingerprints before a known run are ignored, not treated as a build`() {
        val entries = listOf(null to 999.0, "build_a" to 100.0, "build_b" to 110.0)

        val result = OsUpdateImpactCalculator.detectImpact(entries)

        assertEquals("build_a", result?.previousBuildFingerprint)
        assertEquals(100.0, result?.previousAvgValue)
    }
}
