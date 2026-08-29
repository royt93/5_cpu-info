package com.galaxyjoy.cpuinfo.feat.fleet

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class FleetCompareEvaluatorTest {

    private val gb = 1024L * 1024L * 1024L

    @Test
    fun `known model prefix matches regardless of regional suffix`() {
        val result = FleetCompareEvaluator.evaluate("SM-S928B", 12 * gb, 256 * gb)

        assertNotNull(result.matchedEntry)
        assertEquals("Samsung Galaxy S24 Ultra", result.matchedEntry?.displayName)
    }

    @Test
    fun `unknown model produces no match and no mismatch flags`() {
        val result = FleetCompareEvaluator.evaluate("Some Random Phone X1", 2 * gb, 32 * gb)

        assertNull(result.matchedEntry)
        assertFalse(result.ramMismatch)
        assertFalse(result.storageMismatch)
        assertFalse(result.hasMismatch)
    }

    @Test
    fun `RAM reported slightly below marketed figure is not flagged`() {
        // Real S24 Ultra (12GB marketed) commonly reports ~10.5GB due to reserved memory.
        val result = FleetCompareEvaluator.evaluate("SM-S928B", (10.5 * gb).toLong(), 256 * gb)

        assertFalse(result.ramMismatch)
    }

    @Test
    fun `RAM far below marketed figure is flagged`() {
        val result = FleetCompareEvaluator.evaluate("SM-S928B", 3 * gb, 256 * gb)

        assertTrue(result.ramMismatch)
        assertTrue(result.hasMismatch)
    }

    @Test
    fun `storage reported slightly below marketed figure is not flagged`() {
        // Real 256GB device commonly reports ~230GB usable due to filesystem overhead.
        val result = FleetCompareEvaluator.evaluate("SM-S928B", 12 * gb, 230 * gb)

        assertFalse(result.storageMismatch)
    }

    @Test
    fun `storage far below marketed figure is flagged`() {
        val result = FleetCompareEvaluator.evaluate("SM-S928B", 12 * gb, 32 * gb)

        assertTrue(result.storageMismatch)
        assertTrue(result.hasMismatch)
    }

    @Test
    fun `device matching spec on both dimensions has no mismatch`() {
        val result = FleetCompareEvaluator.evaluate("Pixel 7 Pro", 11 * gb, 118 * gb)

        assertFalse(result.hasMismatch)
    }
}
