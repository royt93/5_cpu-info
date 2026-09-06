package com.galaxyjoy.cpuinfo.feat.shield

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RebootStabilityDetectorTest {

    @Test
    fun `isNewBoot is false when there is no prior known boot time`() {
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootTimeMillis = 1_000_000L, lastKnownBootTimeMillis = -1L))
    }

    @Test
    fun `isNewBoot is false when boot time is unchanged`() {
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootTimeMillis = 1_000_000L, lastKnownBootTimeMillis = 1_000_000L))
    }

    @Test
    fun `isNewBoot is false within tolerance for clock drift`() {
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootTimeMillis = 1_030_000L, lastKnownBootTimeMillis = 1_000_000L))
    }

    @Test
    fun `isNewBoot is true once the gap exceeds tolerance`() {
        assertTrue(RebootStabilityDetector.isNewBoot(currentBootTimeMillis = 1_100_000L, lastKnownBootTimeMillis = 1_000_000L))
    }

    @Test
    fun `isNewBoot is true regardless of direction of the shift`() {
        assertTrue(RebootStabilityDetector.isNewBoot(currentBootTimeMillis = 900_000L, lastKnownBootTimeMillis = 1_000_000L))
    }
}
