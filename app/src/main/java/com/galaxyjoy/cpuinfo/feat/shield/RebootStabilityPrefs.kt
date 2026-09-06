package com.galaxyjoy.cpuinfo.feat.shield

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** E21 — persists the last-known boot time and a running reboot counter, same
 * `SharedPreferences`-wrapper shape as [com.galaxyjoy.cpuinfo.feat.achievement.AchievementPrefs].
 * Frequent unexpected reboots often mean a failing battery/storage or a bad ROM update — a
 * stability signal [ShieldScoreCalculator] doesn't otherwise capture (its 3 existing scores are
 * all point-in-time resource snapshots, not trends over time). */
@Singleton
class RebootStabilityPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRebootCount(): Int = sp.getInt(KEY_REBOOT_COUNT, 0)

    /** -1L = never recorded before (first run since this feature shipped). */
    fun getLastKnownBootTimeMillis(): Long = sp.getLong(KEY_LAST_BOOT_TIME, -1L)

    fun recordBootTime(bootTimeMillis: Long) {
        sp.edit().putLong(KEY_LAST_BOOT_TIME, bootTimeMillis).apply()
    }

    fun incrementRebootCount(): Int {
        val next = getRebootCount() + 1
        sp.edit().putInt(KEY_REBOOT_COUNT, next).apply()
        return next
    }

    private companion object {
        const val PREFS_NAME = "reboot_stability_prefs"
        const val KEY_REBOOT_COUNT = "reboot_count"
        const val KEY_LAST_BOOT_TIME = "last_boot_time_millis"
    }
}
