package com.galaxyjoy.cpuinfo.feat.vipreport

/**
 * U07 "VIP diagnostic report lịch sử" — one dated capture of the fields meaningful for spotting
 * battery/performance drift over months. `chargeCounterMah`/`designedCapacityMah`/`cycleCount`
 * are `-1` when the device/API level doesn't expose them (cycle count needs API 34+; designed
 * capacity needs the `PowerProfile` reflection API to succeed).
 */
data class VipDiagnosticSnapshot(
    val timestampMillis: Long,
    val batteryLevelPercent: Int,
    val designedCapacityMah: Double,
    val chargeCounterMah: Double,
    val cycleCount: Int,
    val batteryHealth: Int,
    val ramAvailablePercentage: Int,
    val internalStorageFreeBytes: Long,
    val internalStorageTotalBytes: Long,
)

object VipDiagnosticEvaluator {

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Only a cycle-count delta is reported — it's meaningful regardless of the charge level each
     * snapshot was taken at. A "capacity health %" from `chargeCounterMah` would need every
     * snapshot taken at a comparable (near-full) charge level to mean anything; forcing that
     * constraint on the user was cut as scope the "save whenever" UX doesn't fit — deliberate
     * simplification, not an oversight.
     */
    data class TrendSummary(val daysTracked: Long, val cycleCountDelta: Int?)

    fun summarize(history: List<VipDiagnosticSnapshot>): TrendSummary? {
        if (history.size < 2) return null
        val oldest = history.first()
        val newest = history.last()
        val cycleDelta = if (oldest.cycleCount >= 0 && newest.cycleCount >= 0) {
            newest.cycleCount - oldest.cycleCount
        } else {
            null
        }
        return TrendSummary(
            daysTracked = (newest.timestampMillis - oldest.timestampMillis) / DAY_MILLIS,
            cycleCountDelta = cycleDelta,
        )
    }
}
