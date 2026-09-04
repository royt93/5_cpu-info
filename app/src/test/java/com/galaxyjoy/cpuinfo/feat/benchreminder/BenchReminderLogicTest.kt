package com.galaxyjoy.cpuinfo.feat.benchreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchReminderLogicTest {

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `shouldRemind is false when never benchmarked before`() {
        assertFalse(BenchReminderLogic.shouldRemind(lastBenchTimestampMs = null, nowMs = 1_000_000L))
    }

    @Test
    fun `shouldRemind is false before the interval has passed`() {
        val now = BenchReminderLogic.REMINDER_INTERVAL_DAYS * dayMs
        val last = now - (BenchReminderLogic.REMINDER_INTERVAL_DAYS - 1) * dayMs

        assertFalse(BenchReminderLogic.shouldRemind(last, now))
    }

    @Test
    fun `shouldRemind is true exactly at the interval boundary`() {
        val now = BenchReminderLogic.REMINDER_INTERVAL_DAYS * dayMs
        val last = 0L

        assertTrue(BenchReminderLogic.shouldRemind(last, now))
    }

    @Test
    fun `shouldRemind is true well past the interval`() {
        val now = 100 * dayMs
        val last = 0L

        assertTrue(BenchReminderLogic.shouldRemind(last, now))
    }

    @Test
    fun `latestOf returns null when all 4 benchmark types have never run`() {
        assertEquals(null, BenchReminderLogic.latestOf(null, null, null, null))
    }

    @Test
    fun `latestOf returns the max of the known timestamps, ignoring nulls`() {
        assertEquals(300L, BenchReminderLogic.latestOf(100L, null, 300L, 200L))
    }
}
