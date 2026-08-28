package com.galaxyjoy.cpuinfo.feat.vip.streak

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInStreakTest {

    @Test
    fun `first ever check-in starts streak at 1`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 0L, currentStreak = 0, todayEpochDay = 20000L)
        assertEquals(1, result.streak)
        assertTrue(result.isNewCheckIn)
        assertFalse(result.milestoneReached)
    }

    @Test
    fun `consecutive day increments streak`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20000L, currentStreak = 3, todayEpochDay = 20001L)
        assertEquals(4, result.streak)
        assertTrue(result.isNewCheckIn)
    }

    @Test
    fun `skipped day resets streak to 1`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20000L, currentStreak = 5, todayEpochDay = 20002L)
        assertEquals(1, result.streak)
        assertTrue(result.isNewCheckIn)
        assertFalse(result.milestoneReached)
    }

    @Test
    fun `same day check-in is idempotent no-op`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20000L, currentStreak = 3, todayEpochDay = 20000L)
        assertEquals(3, result.streak)
        assertFalse(result.isNewCheckIn)
        assertFalse(result.milestoneReached)
    }

    @Test
    fun `reaching 7 consecutive days triggers milestone`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20000L, currentStreak = 6, todayEpochDay = 20001L)
        assertEquals(7, result.streak)
        assertTrue(result.milestoneReached)
    }

    @Test
    fun `milestone repeats every 7 days, not just once`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20006L, currentStreak = 13, todayEpochDay = 20007L)
        assertEquals(14, result.streak)
        assertTrue(result.milestoneReached)
    }

    @Test
    fun `day 8 after a milestone is not itself a milestone`() {
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20007L, currentStreak = 7, todayEpochDay = 20008L)
        assertEquals(8, result.streak)
        assertFalse(result.milestoneReached)
    }

    @Test
    fun `clock moved backwards resets streak instead of throwing`() {
        // Defensive behavior for a tampered/incorrect system clock — not a security boundary
        // (this is a cosmetic streak, not a VIP grant), just must not crash or under/overflow.
        val result = CheckInStreak.evaluate(lastCheckInEpochDay = 20010L, currentStreak = 9, todayEpochDay = 20005L)
        assertEquals(1, result.streak)
        assertTrue(result.isNewCheckIn)
    }
}
