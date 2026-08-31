package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import org.junit.Test
import kotlin.test.assertEquals

class HistoryBufferTest {

    @Test
    fun `record appends a point with the current timestamp`() {
        var now = 1_000L
        val buffer = HistoryBuffer(windowMs = 60_000L, nowMs = { now })

        val points = buffer.record(42f)

        assertEquals(1, points.size)
        assertEquals(1_000L, points.first().timestampMs)
        assertEquals(42f, points.first().value)
    }

    @Test
    fun `points within the window are all kept`() {
        var now = 0L
        val buffer = HistoryBuffer(windowMs = 10_000L, nowMs = { now })

        buffer.record(1f)
        now = 5_000L
        buffer.record(2f)
        now = 9_000L
        val points = buffer.record(3f)

        assertEquals(3, points.size)
    }

    @Test
    fun `points older than the window are evicted as new ones arrive`() {
        var now = 0L
        val buffer = HistoryBuffer(windowMs = 10_000L, nowMs = { now })

        buffer.record(1f) // t=0, evicted once t=11_000 arrives (0 < 11_000-10_000=1_000)
        now = 5_000L
        buffer.record(2f) // t=5_000, still within window at t=11_000 (5_000 >= 1_000)
        now = 11_000L
        val points = buffer.record(3f)

        assertEquals(2, points.size)
        assertEquals(5_000L, points.first().timestampMs)
        assertEquals(11_000L, points.last().timestampMs)
    }

    @Test
    fun `never empties even if a single point is older than the window`() {
        var now = 0L
        val buffer = HistoryBuffer(windowMs = 10_000L, nowMs = { now })

        buffer.record(1f)
        now = 100_000L
        val points = buffer.record(2f)

        // Only the newest point survives, but the buffer is never empty.
        assertEquals(1, points.size)
        assertEquals(100_000L, points.first().timestampMs)
    }
}
