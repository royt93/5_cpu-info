package com.galaxyjoy.cpuinfo.util

import kotlin.math.roundToInt

/**
 * U29 — "did the last Android OTA change this benchmark's numbers" detector. Generic over any
 * benchmark's representative metric (same `List<Double>`-style genericity as
 * [BenchPercentileCalculator]), fed `(buildFingerprint, value)` pairs in chronological order —
 * each benchmark screen extracts its own representative metric from its own
 * `*ResultPrefs.SavedResult.osBuildFingerprint`/metric field pair.
 */
object OsUpdateImpactCalculator {

    data class Result(
        val previousBuildFingerprint: String,
        val previousAvgValue: Double,
        val currentBuildFingerprint: String,
        val currentAvgValue: Double,
        val percentChange: Int,
    )

    /**
     * [entries] oldest-first, each a (buildFingerprint, value) pair. Entries with a `null`/blank
     * fingerprint (benchmark runs saved before this feature existed, or a real read failure) are
     * dropped rather than guessed at. Returns `null` when there's nothing to compare: fewer than
     * 2 known-fingerprint entries, or every known entry shares the same single fingerprint (no
     * update has happened within the recorded history yet).
     */
    fun detectImpact(entries: List<Pair<String?, Double>>): Result? {
        val known = entries.filter { !it.first.isNullOrBlank() }
        if (known.size < 2) return null

        val currentFingerprint = known.last().first!!
        val currentGroup = known.takeLastWhile { it.first == currentFingerprint }
        val remaining = known.dropLast(currentGroup.size)
        if (remaining.isEmpty()) return null

        val previousFingerprint = remaining.last().first!!
        val previousGroup = remaining.takeLastWhile { it.first == previousFingerprint }

        val currentAvg = currentGroup.map { it.second }.average()
        val previousAvg = previousGroup.map { it.second }.average()
        val percentChange = if (previousAvg == 0.0) {
            0
        } else {
            (((currentAvg - previousAvg) / previousAvg) * 100.0).roundToInt()
        }

        return Result(previousFingerprint, previousAvg, currentFingerprint, currentAvg, percentChange)
    }
}
