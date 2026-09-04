package com.galaxyjoy.cpuinfo.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class BenchPercentileCalculatorTest {

    @Test
    fun `fewer than 2 runs returns null`() {
        assertNull(BenchPercentileCalculator.percentileOfLast(emptyList()))
        assertNull(BenchPercentileCalculator.percentileOfLast(listOf(100.0)))
    }

    @Test
    fun `current run being the best of all returns 100`() {
        val result = BenchPercentileCalculator.percentileOfLast(listOf(10.0, 20.0, 30.0, 40.0))
        assertEquals(100, result)
    }

    @Test
    fun `current run being the worst of all returns the smallest possible share`() {
        val result = BenchPercentileCalculator.percentileOfLast(listOf(40.0, 30.0, 20.0, 10.0))
        assertEquals(25, result)
    }

    @Test
    fun `current run in the middle returns a proportional share`() {
        // current (30) is <= itself, 20, 10 -> 3 of 5 values are <= current
        val result = BenchPercentileCalculator.percentileOfLast(listOf(10.0, 40.0, 50.0, 20.0, 30.0))
        assertEquals(60, result)
    }

    @Test
    fun `all equal values returns 100`() {
        val result = BenchPercentileCalculator.percentileOfLast(listOf(50.0, 50.0, 50.0))
        assertEquals(100, result)
    }

    @Test
    fun `exactly 2 runs is the minimum meaningful history`() {
        assertEquals(100, BenchPercentileCalculator.percentileOfLast(listOf(10.0, 20.0)))
        assertEquals(50, BenchPercentileCalculator.percentileOfLast(listOf(20.0, 10.0)))
    }

    @Test
    fun `ties at the current value all count as not-better`() {
        // current (30) ties with one other 30 and beats the 10 -> 3 of 4 values are <= current
        val result = BenchPercentileCalculator.percentileOfLast(listOf(10.0, 30.0, 40.0, 30.0))
        assertEquals(75, result)
    }
}
