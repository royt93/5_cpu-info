package com.galaxyjoy.cpuinfo.feat.touchdiag

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TouchSampleRateCalculatorTest {

    @Test
    fun `fewer than 2 timestamps returns null`() {
        assertNull(TouchSampleRateCalculator.hzFrom(emptyList()))
        assertNull(TouchSampleRateCalculator.hzFrom(listOf(1000L)))
    }

    @Test
    fun `zero span returns null instead of dividing by zero`() {
        assertNull(TouchSampleRateCalculator.hzFrom(listOf(1000L, 1000L, 1000L)))
    }

    @Test
    fun `evenly spaced 10ms samples yield 100Hz`() {
        val timestamps = listOf(0L, 10L, 20L, 30L, 40L)
        assertEquals(100.0, TouchSampleRateCalculator.hzFrom(timestamps)!!, 0.001)
    }

    @Test
    fun `unsorted input is sorted before computing`() {
        val timestamps = listOf(30L, 0L, 20L, 10L, 40L)
        assertEquals(100.0, TouchSampleRateCalculator.hzFrom(timestamps)!!, 0.001)
    }

    @Test
    fun `two samples 8ms apart yield 125Hz`() {
        assertEquals(125.0, TouchSampleRateCalculator.hzFrom(listOf(100L, 108L))!!, 0.001)
    }
}
