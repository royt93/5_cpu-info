package com.galaxyjoy.cpuinfo.feat.shield

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RebootStabilityDetectorTest {

    @Test
    fun `isNewBoot is false when there is no prior known boot count`() {
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootCount = 42, lastKnownBootCount = -1))
    }

    @Test
    fun `isNewBoot is false when boot count is unchanged`() {
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootCount = 42, lastKnownBootCount = 42))
    }

    @Test
    fun `isNewBoot is true once the boot count changes`() {
        assertTrue(RebootStabilityDetector.isNewBoot(currentBootCount = 43, lastKnownBootCount = 42))
    }

    @Test
    fun `isNewBoot is not fooled by a wall-clock-only change since it never looks at the clock`() {
        // Regression: the original implementation compared wall-clock-minus-uptime and could
        // false-positive on any NTP sync/manual date change. BOOT_COUNT is immune by construction
        // — same boot count in, same boot count out, regardless of what the clock does meanwhile.
        assertFalse(RebootStabilityDetector.isNewBoot(currentBootCount = 42, lastKnownBootCount = 42))
    }
}
