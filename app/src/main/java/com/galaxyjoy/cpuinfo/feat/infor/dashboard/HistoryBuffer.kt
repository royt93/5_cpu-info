package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import com.galaxyjoy.cpuinfo.domain.model.TimeSeriesPoint

/** Rolling append-only buffer that evicts points older than [windowMs] as new ones arrive. */
class HistoryBuffer(
    private val windowMs: Long,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val points = ArrayDeque<TimeSeriesPoint>()

    fun record(value: Float): List<TimeSeriesPoint> {
        val now = nowMs()
        points.addLast(TimeSeriesPoint(now, value))
        val cutoff = now - windowMs
        while (points.size > 1 && points.first().timestampMs < cutoff) points.removeFirst()
        return points.toList()
    }
}
