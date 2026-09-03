package com.galaxyjoy.cpuinfo.feat.p2pcompare

/**
 * U22 — unlike [com.galaxyjoy.cpuinfo.feat.fleet.FleetCompareEvaluator] (actual vs a hardcoded
 * catalog spec, with tolerance bands since the catalog number is a marketed minimum, not a real
 * measurement), this compares 2 real measured values directly — no tolerance needed, a straight
 * greater-than decides the winner.
 */
object DeviceCompareEvaluator {

    enum class Winner { LOCAL, REMOTE, TIE }

    data class FieldComparison(val localBytes: Long, val remoteBytes: Long, val winner: Winner)

    data class Result(
        val local: DeviceComparePayload,
        val remote: DeviceComparePayload,
        val ram: FieldComparison,
        val storage: FieldComparison,
    )

    fun compare(local: DeviceComparePayload, remote: DeviceComparePayload): Result = Result(
        local = local,
        remote = remote,
        ram = compareField(local.ramBytes, remote.ramBytes),
        storage = compareField(local.storageBytes, remote.storageBytes),
    )

    private fun compareField(localBytes: Long, remoteBytes: Long): FieldComparison {
        val winner = when {
            localBytes > remoteBytes -> Winner.LOCAL
            remoteBytes > localBytes -> Winner.REMOTE
            else -> Winner.TIE
        }
        return FieldComparison(localBytes, remoteBytes, winner)
    }
}
