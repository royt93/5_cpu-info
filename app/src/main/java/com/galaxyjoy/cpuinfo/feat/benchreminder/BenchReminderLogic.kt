package com.galaxyjoy.cpuinfo.feat.benchreminder

/**
 * U30 — pure decision logic, no Android deps. Only nudges a user who has already run at least
 * 1 benchmark before (`lastBenchTimestampMs == null` means never run — that's a feature-discovery
 * gap, not something a background notification should push). Needs no extra "last reminded" pref:
 * the moment the user re-runs any benchmark, `lastBenchTimestampMs` moves forward and
 * [shouldRemind] naturally goes false again until [REMINDER_INTERVAL_DAYS] passes once more —
 * same self-regulating shape as re-deriving state from source-of-truth data instead of tracking a
 * parallel "already notified" flag.
 */
object BenchReminderLogic {

    const val REMINDER_INTERVAL_DAYS = 30
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun shouldRemind(lastBenchTimestampMs: Long?, nowMs: Long): Boolean {
        if (lastBenchTimestampMs == null) return false
        return nowMs - lastBenchTimestampMs >= REMINDER_INTERVAL_DAYS * DAY_MS
    }

    /** Most recent of the 4 `*ResultPrefs.getLastResult()?.timestampMs` values, or `null` if none
     * of the 4 benchmark types has ever been run. */
    fun latestOf(vararg timestampsMs: Long?): Long? = timestampsMs.filterNotNull().maxOrNull()
}
