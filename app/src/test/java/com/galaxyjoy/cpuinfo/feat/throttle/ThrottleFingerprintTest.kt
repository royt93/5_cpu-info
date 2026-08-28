package com.galaxyjoy.cpuinfo.feat.throttle

import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint.AbortReason
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottleFingerprintTest {

    @Test
    fun `evaluate returns null for empty samples`() {
        assertNull(ThrottleFingerprint.evaluate(emptyList(), aborted = false, abortReason = null))
    }

    @Test
    fun `evaluate detects throttling when sustained freq drops well below peak`() {
        val samples = listOf(
            Sample(elapsedMs = 0, avgFreqMhz = 2000, tempC = 30),
            Sample(elapsedMs = 5_000, avgFreqMhz = 2800, tempC = 35),
            Sample(elapsedMs = 10_000, avgFreqMhz = 2800, tempC = 38),
            Sample(elapsedMs = 15_000, avgFreqMhz = 2200, tempC = 40),
            Sample(elapsedMs = 20_000, avgFreqMhz = 2000, tempC = 41),
            Sample(elapsedMs = 25_000, avgFreqMhz = 1900, tempC = 42),
            Sample(elapsedMs = 30_000, avgFreqMhz = 1800, tempC = 42),
        )

        val result = ThrottleFingerprint.evaluate(samples, aborted = false, abortReason = null)!!

        assertEquals(2800L, result.peakFreqMhz)
        // sustained window = last 5s → samples at 25s and 30s → avg(1900,1800)=1850
        assertEquals(1850L, result.sustainedFreqMhz)
        assertTrue(result.throttled)
        assertEquals(33, result.throttlePercent) // (2800-1850)*100/2800 = 33.9 -> 33 (int division)
        assertEquals(30, result.startTempC)
        assertEquals(42, result.maxTempC)
        assertFalse(result.aborted)
    }

    @Test
    fun `evaluate reports stable when frequency barely drops`() {
        val samples = listOf(
            Sample(elapsedMs = 0, avgFreqMhz = 2400, tempC = 30),
            Sample(elapsedMs = 10_000, avgFreqMhz = 2400, tempC = 33),
            Sample(elapsedMs = 20_000, avgFreqMhz = 2350, tempC = 34),
            Sample(elapsedMs = 30_000, avgFreqMhz = 2300, tempC = 35),
        )

        val result = ThrottleFingerprint.evaluate(samples, aborted = false, abortReason = null)!!

        assertFalse(result.throttled)
        assertTrue(result.throttlePercent < ThrottleFingerprint.THROTTLE_THRESHOLD_PERCENT)
    }

    @Test
    fun `evaluate handles zero peak frequency without dividing by zero`() {
        val samples = listOf(
            Sample(elapsedMs = 0, avgFreqMhz = 0, tempC = 30),
            Sample(elapsedMs = 1_000, avgFreqMhz = 0, tempC = 30),
        )

        val result = ThrottleFingerprint.evaluate(samples, aborted = false, abortReason = null)!!

        assertEquals(0, result.throttlePercent)
        assertFalse(result.throttled)
    }

    @Test
    fun `evaluate preserves abort metadata`() {
        val samples = listOf(Sample(elapsedMs = 0, avgFreqMhz = 2000, tempC = 44))

        val result = ThrottleFingerprint.evaluate(
            samples,
            aborted = true,
            abortReason = AbortReason.OVERHEAT,
        )!!

        assertTrue(result.aborted)
        assertEquals(AbortReason.OVERHEAT, result.abortReason)
    }

    @Test
    fun `shouldAbortForSafety triggers at and above threshold only`() {
        assertFalse(ThrottleFingerprint.shouldAbortForSafety(ThrottleFingerprint.SAFETY_ABORT_TEMP_C - 1))
        assertTrue(ThrottleFingerprint.shouldAbortForSafety(ThrottleFingerprint.SAFETY_ABORT_TEMP_C))
        assertTrue(ThrottleFingerprint.shouldAbortForSafety(ThrottleFingerprint.SAFETY_ABORT_TEMP_C + 5))
    }

    @Test
    fun `shouldStop triggers at and above hard duration cap only`() {
        assertFalse(ThrottleFingerprint.shouldStop(ThrottleFingerprint.TEST_DURATION_MS - 1))
        assertTrue(ThrottleFingerprint.shouldStop(ThrottleFingerprint.TEST_DURATION_MS))
        assertTrue(ThrottleFingerprint.shouldStop(ThrottleFingerprint.TEST_DURATION_MS + 1_000))
    }

    @Test
    fun `evaluate with single sample uses it as both peak and sustained`() {
        val samples = listOf(Sample(elapsedMs = 0, avgFreqMhz = 2000, tempC = 31))

        val result = ThrottleFingerprint.evaluate(samples, aborted = false, abortReason = null)!!

        assertEquals(2000L, result.peakFreqMhz)
        assertEquals(2000L, result.sustainedFreqMhz)
        assertEquals(0, result.throttlePercent)
    }
}
