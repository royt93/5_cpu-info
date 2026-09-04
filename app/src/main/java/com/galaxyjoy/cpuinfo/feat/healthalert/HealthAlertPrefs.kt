package com.galaxyjoy.cpuinfo.feat.healthalert

import android.content.Context
import android.content.SharedPreferences

/** Plain `Context` constructor, no Hilt — same reasoning as
 * [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider] being constructed manually inside
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider]: [HealthAlertWorker] is a
 * plain `CoroutineWorker`, not Hilt-injected. */
class HealthAlertPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastScore(): Int? = if (sp.contains(KEY_LAST_SCORE)) sp.getInt(KEY_LAST_SCORE, 0) else null

    fun saveScore(score: Int) {
        sp.edit().putInt(KEY_LAST_SCORE, score).apply()
    }

    fun getLastAlertTimestampMs(): Long = sp.getLong(KEY_LAST_ALERT_MS, 0L)

    fun saveAlertTimestamp(timestampMs: Long) {
        sp.edit().putLong(KEY_LAST_ALERT_MS, timestampMs).apply()
    }

    /** U26 — separate from [getLastScore]/[saveScore], which the 6-hourly [HealthAlertWorker]
     * overwrites every run and so can never hold a real "score from ~7 days ago". */
    fun getWeeklyBaselineScore(): Int? =
        if (sp.contains(KEY_WEEKLY_BASELINE_SCORE)) sp.getInt(KEY_WEEKLY_BASELINE_SCORE, 0) else null

    fun saveWeeklyBaselineScore(score: Int) {
        sp.edit().putInt(KEY_WEEKLY_BASELINE_SCORE, score).apply()
    }

    /** Test-only reset — [HealthAlertNotifierTest] runs against the real on-device prefs file
     * (same one the real worker would use), so it needs to start from a known-empty state rather
     * than whatever a previous test run or real background check left behind. */
    fun clear() {
        sp.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "health_alert_prefs"
        const val KEY_LAST_SCORE = "last_score"
        const val KEY_LAST_ALERT_MS = "last_alert_ms"
        const val KEY_WEEKLY_BASELINE_SCORE = "weekly_baseline_score"
    }
}
