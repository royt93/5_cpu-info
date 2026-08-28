package com.galaxyjoy.cpuinfo.feat.vip.streak

import android.content.Context
import android.content.SharedPreferences
import java.util.TimeZone

/**
 * Persistence for the daily check-in streak (U09). Separate `SharedPreferences` file from
 * [com.galaxyjoy.cpuinfo.feat.vip.VipPrefs] — different lifecycle/reset semantics.
 */
internal class CheckInStreakPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastCheckInEpochDay(): Long = sp.getLong(KEY_LAST_DAY, 0L)

    fun getStreak(): Int = sp.getInt(KEY_STREAK, 0)

    /**
     * Whether a 7-day milestone is waiting to be claimed. Deliberately a flag, not a counter —
     * if the user reaches multiple milestones without opening [ShieldScoreBottomSheet], claiming
     * once still only grants the flat reward once; that's an acceptable simplification for an
     * edge case that requires weeks of unclaimed streaks to hit.
     */
    fun hasUnclaimedMilestone(): Boolean = sp.getBoolean(KEY_UNCLAIMED_MILESTONE, false)

    fun saveCheckIn(epochDay: Long, streak: Int, milestoneReached: Boolean) {
        val editor = sp.edit()
            .putLong(KEY_LAST_DAY, epochDay)
            .putInt(KEY_STREAK, streak)
        if (milestoneReached) {
            editor.putBoolean(KEY_UNCLAIMED_MILESTONE, true)
        }
        editor.apply()
    }

    fun consumeMilestoneClaim() {
        sp.edit().putBoolean(KEY_UNCLAIMED_MILESTONE, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "vip_streak_prefs"
        private const val KEY_LAST_DAY = "last_checkin_epoch_day"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_UNCLAIMED_MILESTONE = "unclaimed_milestone"

        /**
         * Local-calendar-day number (not UTC day) — avoids `java.time` which needs API 26+ /
         * core library desugaring (this project targets minSdk 24 without desugaring enabled).
         */
        fun todayEpochDay(): Long {
            val nowMs = System.currentTimeMillis()
            val offsetMs = TimeZone.getDefault().getOffset(nowMs)
            return (nowMs + offsetMs) / DAY_MS
        }

        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
