package com.galaxyjoy.cpuinfo.feat.vipreport

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class VipDiagnosticEvaluatorTest {

    private fun snapshot(
        dayOffset: Long,
        cycleCount: Int = -1,
        batteryLevelPercent: Int = 80,
    ): VipDiagnosticSnapshot = VipDiagnosticSnapshot(
        timestampMillis = dayOffset * 24L * 60 * 60 * 1000,
        batteryLevelPercent = batteryLevelPercent,
        designedCapacityMah = 5000.0,
        chargeCounterMah = 4000.0,
        cycleCount = cycleCount,
        batteryHealth = 2,
        ramAvailablePercentage = 40,
        internalStorageFreeBytes = 10L * 1024 * 1024 * 1024,
        internalStorageTotalBytes = 128L * 1024 * 1024 * 1024,
    )

    @Test
    fun `summarize returns null with fewer than 2 entries`() {
        assertNull(VipDiagnosticEvaluator.summarize(emptyList()))
        assertNull(VipDiagnosticEvaluator.summarize(listOf(snapshot(0))))
    }

    @Test
    fun `summarize computes days tracked between oldest and newest entry`() {
        val history = listOf(snapshot(0), snapshot(30))

        val summary = VipDiagnosticEvaluator.summarize(history)

        assertEquals(30L, summary?.daysTracked)
    }

    @Test
    fun `summarize computes cycle count delta when both entries have it`() {
        val history = listOf(snapshot(0, cycleCount = 50), snapshot(30, cycleCount = 62))

        val summary = VipDiagnosticEvaluator.summarize(history)

        assertEquals(12, summary?.cycleCountDelta)
    }

    @Test
    fun `summarize leaves cycle count delta null if either entry lacks it`() {
        val oldestMissing = VipDiagnosticEvaluator.summarize(
            listOf(snapshot(0, cycleCount = -1), snapshot(30, cycleCount = 62)),
        )
        val newestMissing = VipDiagnosticEvaluator.summarize(
            listOf(snapshot(0, cycleCount = 50), snapshot(30, cycleCount = -1)),
        )

        assertNull(oldestMissing?.cycleCountDelta)
        assertNull(newestMissing?.cycleCountDelta)
    }

    @Test
    fun `summarize trusts caller-provided ordering, using first() and last() as oldest and newest`() {
        // Contract: callers (VipDiagnosticReportRepository.loadHistory()) must hand in a
        // chronologically-ascending list — summarize() does not re-sort itself.
        val ascending = listOf(snapshot(0, cycleCount = 50), snapshot(30, cycleCount = 80))
        assertEquals(30, VipDiagnosticEvaluator.summarize(ascending)?.cycleCountDelta)

        val reversed = ascending.asReversed()
        assertEquals(-30, VipDiagnosticEvaluator.summarize(reversed)?.cycleCountDelta)
    }

    @Test
    fun `batteryLevelSeries returns empty list for empty history`() {
        assertTrue(VipDiagnosticEvaluator.batteryLevelSeries(emptyList()).isEmpty())
    }

    @Test
    fun `batteryLevelSeries maps each snapshot's battery level in order`() {
        val history = listOf(
            snapshot(0, batteryLevelPercent = 90),
            snapshot(1, batteryLevelPercent = 85),
            snapshot(2, batteryLevelPercent = 82),
        )

        assertEquals(listOf(90.0, 85.0, 82.0), VipDiagnosticEvaluator.batteryLevelSeries(history))
    }

    @Test
    fun `batteryLevelSeries filters out the -1 sentinel instead of charting it as a real value`() {
        val history = listOf(
            snapshot(0, batteryLevelPercent = 90),
            snapshot(1, batteryLevelPercent = -1),
            snapshot(2, batteryLevelPercent = 82),
        )

        assertEquals(listOf(90.0, 82.0), VipDiagnosticEvaluator.batteryLevelSeries(history))
    }
}
