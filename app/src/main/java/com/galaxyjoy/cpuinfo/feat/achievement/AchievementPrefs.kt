package com.galaxyjoy.cpuinfo.feat.achievement

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** U33 — a single counter shared across all 4 benchmark types ("how many personal records has
 * this device ever broken"), not 4 separate per-type counters — the celebratory value is in the
 * running total shown in [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreBottomSheet], not in
 * per-type breakdowns nobody asked for. */
@Singleton
class AchievementPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecordsBrokenCount(): Int = sp.getInt(KEY_RECORDS_BROKEN, 0)

    fun incrementRecordsBroken(): Int {
        val next = getRecordsBrokenCount() + 1
        sp.edit().putInt(KEY_RECORDS_BROKEN, next).apply()
        return next
    }

    private companion object {
        const val PREFS_NAME = "achievement_prefs"
        const val KEY_RECORDS_BROKEN = "records_broken_count"
    }
}
