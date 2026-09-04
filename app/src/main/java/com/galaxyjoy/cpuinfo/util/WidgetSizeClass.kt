package com.galaxyjoy.cpuinfo.util

/**
 * U27 — shared "is this home-screen widget instance big enough to show more than the single
 * headline value" decision for [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider]
 * and [com.galaxyjoy.cpuinfo.feat.lastbenchwidget.LastBenchWidgetProvider]. Pure `Int` in/out (no
 * `AppWidgetManager` dependency) so it's plain-JVM unit-testable — the actual dp lookup happens in
 * each provider via `AppWidgetManager.getAppWidgetOptions(appWidgetId)`'s
 * `OPTION_APPWIDGET_MIN_HEIGHT`.
 */
object WidgetSizeClass {

    /** Below the default placement height (90dp, see `shield_score_widget_info.xml`/
     * `last_bench_widget_info.xml`) — the user has to deliberately resize taller to get the
     * detailed layout, so the default just-placed experience stays exactly as before. */
    const val LARGE_MIN_HEIGHT_DP = 180

    fun isLarge(minHeightDp: Int): Boolean = minHeightDp >= LARGE_MIN_HEIGHT_DP
}
