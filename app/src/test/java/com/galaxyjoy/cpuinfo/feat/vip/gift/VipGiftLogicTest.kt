package com.galaxyjoy.cpuinfo.feat.vip.gift

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class VipGiftLogicTest {

    private val dayMs = 24L * 60L * 60L * 1000L

    @Test
    fun `can generate when nothing was ever generated`() {
        assertTrue(VipGiftLogic.canGenerateToday(null, todayEpochDay = 100L))
    }

    @Test
    fun `cannot generate again the same day`() {
        assertFalse(VipGiftLogic.canGenerateToday(lastGeneratedEpochDay = 100L, todayEpochDay = 100L))
    }

    @Test
    fun `can generate again on a later day`() {
        assertTrue(VipGiftLogic.canGenerateToday(lastGeneratedEpochDay = 100L, todayEpochDay = 101L))
    }

    @Test
    fun `can redeem when nothing was ever redeemed`() {
        assertTrue(VipGiftLogic.canRedeemToday(null, todayEpochDay = 100L))
    }

    @Test
    fun `cannot redeem again the same day`() {
        assertFalse(VipGiftLogic.canRedeemToday(lastRedeemedEpochDay = 100L, todayEpochDay = 100L))
    }

    @Test
    fun `a code issued today is fresh`() {
        assertTrue(VipGiftLogic.isCodeFresh(issuedEpochDay = 100L, todayEpochDay = 100L))
    }

    @Test
    fun `a code within the max age is fresh`() {
        assertTrue(VipGiftLogic.isCodeFresh(issuedEpochDay = 100L, todayEpochDay = 100L + VipGiftLogic.MAX_CODE_AGE_DAYS))
    }

    @Test
    fun `a code older than the max age is not fresh`() {
        assertFalse(VipGiftLogic.isCodeFresh(issuedEpochDay = 100L, todayEpochDay = 100L + VipGiftLogic.MAX_CODE_AGE_DAYS + 1))
    }

    @Test
    fun `a code issued in the future is not fresh`() {
        assertFalse(VipGiftLogic.isCodeFresh(issuedEpochDay = 101L, todayEpochDay = 100L))
    }

    @Test
    fun `not currently VIP grants exactly the gift days`() {
        val now = 0L
        val currentExpiryMs = 0L // already expired / never VIP

        assertEquals(1, VipGiftLogic.daysToGrantForAccumulate(currentExpiryMs, now))
    }

    @Test
    fun `already VIP with time left extends instead of overwriting`() {
        val now = 0L
        val currentExpiryMs = 10 * dayMs // 10 days of real VIP left

        // Redeeming a 1-day gift should land on (10 + 1) days from now, not reset to 1.
        val days = VipGiftLogic.daysToGrantForAccumulate(currentExpiryMs, now)

        assertEquals(11, days)
    }

    @Test
    fun `already VIP expiring exactly now behaves like not VIP`() {
        val now = 5 * dayMs
        val currentExpiryMs = 5 * dayMs

        assertEquals(1, VipGiftLogic.daysToGrantForAccumulate(currentExpiryMs, now))
    }

    @Test
    fun `never returns fewer than 1 day even for a partial-day remainder`() {
        val now = 0L
        val currentExpiryMs = dayMs / 2 // half a day left

        val days = VipGiftLogic.daysToGrantForAccumulate(currentExpiryMs, now)

        assertTrue(days >= 1)
    }

    /** Documents the known rounding-up behavior (see the function's own `ponytail:` comment) —
     * 12h remaining rounds up to a full 2 days rather than a fractional 1.5, an intentional
     * bounded over-grant (never under-grants) rather than an oversight. Pinning the exact value
     * here (not just `>= 1`) so a future change to this rounding is a deliberate decision, not a
     * silent behavior drift. */
    @Test
    fun `partial-day remainder rounds up to the next whole day, a bounded over-grant by design`() {
        val now = 0L
        val currentExpiryMs = dayMs / 2 // 12h remaining

        assertEquals(2, VipGiftLogic.daysToGrantForAccumulate(currentExpiryMs, now))
    }
}
