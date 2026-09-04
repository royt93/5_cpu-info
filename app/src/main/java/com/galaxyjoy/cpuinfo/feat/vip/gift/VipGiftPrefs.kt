package com.galaxyjoy.cpuinfo.feat.vip.gift

import android.content.Context
import android.content.SharedPreferences
import java.util.TimeZone

/** U11 — local-only "1 gift generated/redeemed per day" tracking. No backend exists in this app
 * (see doc/task/epic-04-unique-ideas.md's U11 research), so this is device-local, same trust tier
 * as the existing hardcoded `VipKeys` redeem codes — not abuse-proof, just a reasonable throttle. */
internal class VipGiftPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastGeneratedEpochDay(): Long? =
        if (sp.contains(KEY_LAST_GENERATED)) sp.getLong(KEY_LAST_GENERATED, 0L) else null

    fun saveLastGeneratedEpochDay(day: Long) {
        sp.edit().putLong(KEY_LAST_GENERATED, day).apply()
    }

    fun getLastRedeemedEpochDay(): Long? =
        if (sp.contains(KEY_LAST_REDEEMED)) sp.getLong(KEY_LAST_REDEEMED, 0L) else null

    fun saveLastRedeemedEpochDay(day: Long) {
        sp.edit().putLong(KEY_LAST_REDEEMED, day).apply()
    }

    companion object {
        private const val PREFS_NAME = "vip_gift_prefs"
        private const val KEY_LAST_GENERATED = "last_generated_epoch_day"
        private const val KEY_LAST_REDEEMED = "last_redeemed_epoch_day"
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        /** Same local-timezone epoch-day convention as
         * [com.galaxyjoy.cpuinfo.feat.vip.streak.CheckInStreakPrefs.todayEpochDay]. */
        fun todayEpochDay(): Long {
            val nowMs = System.currentTimeMillis()
            val offsetMs = TimeZone.getDefault().getOffset(nowMs)
            return (nowMs + offsetMs) / DAY_MS
        }
    }
}
