package com.galaxyjoy.cpuinfo.feat.gnssdiag

import android.location.GnssStatus

data class GnssSatelliteInfo(
    val constellation: String,
    val svid: Int,
    val cn0DbHz: Float,
    val usedInFix: Boolean,
    val carrierFrequencyHz: Float?,
)

data class GnssStatusSnapshot(
    val satellites: List<GnssSatelliteInfo>,
    val usedInFixCount: Int,
    val dualFrequencySatelliteCount: Int,
    val constellationCounts: Map<String, Int>,
)

/**
 * Pure mapping from a live [GnssStatus] snapshot. A single [GnssStatus] update lists one row per
 * tracked SIGNAL, not per physical satellite — a receiver tracking a satellite on both L1 and L5
 * reports it as two entries sharing the same (constellation, svid) but different
 * [GnssStatus.getCarrierFrequencyHz]. [GnssStatusSnapshot.dualFrequencySatelliteCount] counts
 * satellites with ≥2 distinct carrier frequencies in the current snapshot — an empirical,
 * per-snapshot signal, not a static hardware capability claim (there is no public API for the
 * latter either, same category of platform limitation as E11's "max touch points").
 */
internal object GnssStatusMapper {

    fun map(status: GnssStatus): GnssStatusSnapshot {
        val satellites = (0 until status.satelliteCount).map { i ->
            GnssSatelliteInfo(
                constellation = constellationLabel(status.getConstellationType(i)),
                svid = status.getSvid(i),
                cn0DbHz = status.getCn0DbHz(i),
                usedInFix = status.usedInFix(i),
                carrierFrequencyHz = if (status.hasCarrierFrequencyHz(i)) status.getCarrierFrequencyHz(i) else null,
            )
        }
        return fromSatellites(satellites)
    }

    internal fun fromSatellites(satellites: List<GnssSatelliteInfo>): GnssStatusSnapshot {
        val dualFrequencyCount = satellites
            .filter { it.carrierFrequencyHz != null }
            .groupBy { it.constellation to it.svid }
            .count { (_, group) -> group.mapNotNull { it.carrierFrequencyHz }.distinct().size >= 2 }

        return GnssStatusSnapshot(
            satellites = satellites,
            usedInFixCount = satellites.count { it.usedInFix },
            dualFrequencySatelliteCount = dualFrequencyCount,
            constellationCounts = satellites.groupingBy { it.constellation }.eachCount(),
        )
    }

    fun constellationLabel(type: Int): String = when (type) {
        GnssStatus.CONSTELLATION_GPS -> "GPS"
        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
        GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
        GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
        GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
        GnssStatus.CONSTELLATION_SBAS -> "SBAS"
        else -> "UNKNOWN"
    }
}
