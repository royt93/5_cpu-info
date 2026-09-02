package com.galaxyjoy.cpuinfo.feat.healthalert

/**
 * Pure decision logic for U19 — no Android deps. [HealthAlertWorker] feeds the current Shield
 * Score (already computed by the existing [com.galaxyjoy.cpuinfo.feat.shield.ShieldScoreProvider],
 * no new scoring logic here) plus the previously-saved score/alert-timestamp.
 */
object HealthAlertLogic {

    /** Fire if the score dropped at least this many points since the last check. */
    const val SCORE_DROP_THRESHOLD = 15

    /** Fire regardless of delta once the score itself is this low. */
    const val ABSOLUTE_LOW_THRESHOLD = 40

    /** At most one alert per this window, even if the score stays bad on every subsequent check —
     * without this, a score pinned below [ABSOLUTE_LOW_THRESHOLD] would re-notify every single
     * periodic run. */
    const val COOLDOWN_MS = 24 * 60 * 60 * 1000L

    fun shouldAlert(previousScore: Int?, currentScore: Int, lastAlertTimestampMs: Long, nowMs: Long): Boolean {
        if (nowMs - lastAlertTimestampMs < COOLDOWN_MS) return false
        if (currentScore < ABSOLUTE_LOW_THRESHOLD) return true
        if (previousScore != null && previousScore - currentScore >= SCORE_DROP_THRESHOLD) return true
        return false
    }
}
