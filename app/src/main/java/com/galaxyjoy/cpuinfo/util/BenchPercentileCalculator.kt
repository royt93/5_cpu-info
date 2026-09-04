package com.galaxyjoy.cpuinfo.util

/**
 * U24 — honest "how does this run compare" signal: ranks the just-completed run against this same
 * device's own benchmark history, never against other devices. A cross-device leaderboard would
 * need either fabricated numbers or a crowd-sourced server backend — both rejected: it would
 * either mislead the user with made-up data, or break the offline/no-data-ever-leaves-the-device
 * design already committed to elsewhere (see
 * [com.galaxyjoy.cpuinfo.feat.fleet.FleetCompareBottomSheet]'s disclaimer and
 * `doc/task/quick_win.md`'s floating-overlay rejection for the same privacy-first reasoning).
 */
object BenchPercentileCalculator {

    /**
     * [values] must be the benchmark history ending with the just-completed run's value
     * (oldest-first) — exactly what `*ResultPrefs.getHistory()` already returns right after
     * `saveResult()`. Higher is assumed better for every metric this app tracks (sustained MHz,
     * MB/s, FPS), so the percentile is the share of all runs (including this one) at or below it.
     *
     * Returns null when there isn't enough history for the number to mean anything — same `<2`
     * threshold [com.galaxyjoy.cpuinfo.ui.component.BenchTrendChart] already uses to hide itself.
     */
    fun percentileOfLast(values: List<Double>): Int? {
        if (values.size < 2) return null
        val current = values.last()
        val notBetterCount = values.count { it <= current }
        return (100 * notBetterCount) / values.size
    }
}
