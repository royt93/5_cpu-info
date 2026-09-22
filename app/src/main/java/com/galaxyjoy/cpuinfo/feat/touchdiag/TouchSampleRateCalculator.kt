package com.galaxyjoy.cpuinfo.feat.touchdiag

/**
 * Pure sample-rate math — given the `uptimeMillis` of consecutive touch samples (Compose's
 * `PointerInputChange.historical` entries plus the current change, the analog of
 * `MotionEvent.getHistoricalEventTime()` at the raw-input level), returns the average sampling
 * rate in Hz. `null` when there aren't at least 2 timestamps to derive an interval from.
 */
internal object TouchSampleRateCalculator {

    fun hzFrom(timestampsMillis: List<Long>): Double? {
        if (timestampsMillis.size < 2) return null

        val sorted = timestampsMillis.sorted()
        val totalSpanMillis = sorted.last() - sorted.first()
        if (totalSpanMillis <= 0) return null

        val intervalCount = sorted.size - 1
        val avgIntervalMillis = totalSpanMillis.toDouble() / intervalCount
        return 1000.0 / avgIntervalMillis
    }
}
