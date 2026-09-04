package com.galaxyjoy.cpuinfo.feat.healthalert

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthAlertLogicTest {

    private val dayMs = 24L * 60 * 60 * 1000L

    @Test
    fun `alerts when score drops at least the threshold since last check`() {
        assertTrue(
            HealthAlertLogic.shouldAlert(
                previousScore = 80,
                currentScore = 80 - HealthAlertLogic.SCORE_DROP_THRESHOLD,
                lastAlertTimestampMs = 0L,
                nowMs = dayMs,
            ),
        )
    }

    @Test
    fun `does not alert for a drop smaller than the threshold`() {
        assertFalse(
            HealthAlertLogic.shouldAlert(
                previousScore = 80,
                currentScore = 80 - HealthAlertLogic.SCORE_DROP_THRESHOLD + 1,
                lastAlertTimestampMs = 0L,
                nowMs = dayMs,
            ),
        )
    }

    @Test
    fun `alerts when score is below the absolute threshold regardless of previous score`() {
        assertTrue(
            HealthAlertLogic.shouldAlert(
                previousScore = null,
                currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1,
                lastAlertTimestampMs = 0L,
                nowMs = dayMs,
            ),
        )
    }

    @Test
    fun `does not alert when score is at or above the absolute threshold and no previous score exists`() {
        assertFalse(
            HealthAlertLogic.shouldAlert(
                previousScore = null,
                currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD,
                lastAlertTimestampMs = 0L,
                nowMs = dayMs,
            ),
        )
    }

    @Test
    fun `does not alert twice within the cooldown window even if the score stays low`() {
        assertFalse(
            HealthAlertLogic.shouldAlert(
                previousScore = null,
                currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1,
                lastAlertTimestampMs = 0L,
                nowMs = HealthAlertLogic.COOLDOWN_MS - 1,
            ),
        )
    }

    @Test
    fun `alerts again once the cooldown window has fully elapsed`() {
        assertTrue(
            HealthAlertLogic.shouldAlert(
                previousScore = null,
                currentScore = HealthAlertLogic.ABSOLUTE_LOW_THRESHOLD - 1,
                lastAlertTimestampMs = 0L,
                nowMs = HealthAlertLogic.COOLDOWN_MS,
            ),
        )
    }

    /** U26 */
    @Test
    fun `weeklyDigestDelta is null when there is no baseline yet`() {
        assertEquals(null, HealthAlertLogic.weeklyDigestDelta(previousWeekScore = null, currentScore = 80))
    }

    @Test
    fun `weeklyDigestDelta is positive when the score improved since the baseline`() {
        assertEquals(5, HealthAlertLogic.weeklyDigestDelta(previousWeekScore = 70, currentScore = 75))
    }

    @Test
    fun `weeklyDigestDelta is negative when the score dropped since the baseline`() {
        assertEquals(-10, HealthAlertLogic.weeklyDigestDelta(previousWeekScore = 80, currentScore = 70))
    }

    @Test
    fun `weeklyDigestDelta is zero when the score did not change`() {
        assertEquals(0, HealthAlertLogic.weeklyDigestDelta(previousWeekScore = 80, currentScore = 80))
    }
}
