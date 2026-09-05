package com.galaxyjoy.cpuinfo.feat.achievement

/**
 * U33 — pure decision logic, no Android deps. A first-ever run (`previousBest == null`) is never
 * a "record" — there's nothing yet to beat, same "nothing to compare against yet" reasoning as
 * [com.galaxyjoy.cpuinfo.util.BenchPercentileCalculator.percentileOfLast]'s `<2 points -> null`.
 */
object AchievementLogic {

    fun isNewRecord(previousBest: Double?, current: Double): Boolean =
        previousBest != null && current > previousBest
}
