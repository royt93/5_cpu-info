package com.galaxyjoy.cpuinfo.feat.gnssdiag

import android.location.GnssStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GnssStatusMapperConstellationLabelTest {

    @Test
    fun `maps every known constellation constant`() {
        assertEquals("GPS", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_GPS))
        assertEquals("GLONASS", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_GLONASS))
        assertEquals("GALILEO", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_GALILEO))
        assertEquals("BEIDOU", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_BEIDOU))
        assertEquals("QZSS", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_QZSS))
        assertEquals("IRNSS", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_IRNSS))
        assertEquals("SBAS", GnssStatusMapper.constellationLabel(GnssStatus.CONSTELLATION_SBAS))
    }

    @Test
    fun `unknown constellation constant falls back to UNKNOWN instead of throwing`() {
        assertEquals("UNKNOWN", GnssStatusMapper.constellationLabel(-1))
    }
}

class GnssStatusMapperFromSatellitesTest {

    @Test
    fun `usedInFixCount only counts satellites actually used in the fix`() {
        val satellites = listOf(
            sat(usedInFix = true),
            sat(usedInFix = false),
            sat(usedInFix = true),
        )

        assertEquals(2, GnssStatusMapper.fromSatellites(satellites).usedInFixCount)
    }

    @Test
    fun `a satellite with two distinct carrier frequencies counts as dual-frequency`() {
        val satellites = listOf(
            sat(constellation = "GPS", svid = 1, carrierFrequencyHz = 1575.42e6f),
            sat(constellation = "GPS", svid = 1, carrierFrequencyHz = 1176.45e6f), // same svid, L5 band
        )

        assertEquals(1, GnssStatusMapper.fromSatellites(satellites).dualFrequencySatelliteCount)
    }

    @Test
    fun `same svid with only one distinct frequency is not dual-frequency`() {
        val satellites = listOf(
            sat(constellation = "GPS", svid = 1, carrierFrequencyHz = 1575.42e6f),
            sat(constellation = "GPS", svid = 1, carrierFrequencyHz = 1575.42e6f), // duplicate reading, same band
        )

        assertEquals(0, GnssStatusMapper.fromSatellites(satellites).dualFrequencySatelliteCount)
    }

    @Test
    fun `entries with no carrier frequency data never count toward dual-frequency`() {
        val satellites = listOf(sat(carrierFrequencyHz = null), sat(carrierFrequencyHz = null))

        assertEquals(0, GnssStatusMapper.fromSatellites(satellites).dualFrequencySatelliteCount)
    }

    @Test
    fun `constellationCounts groups satellites by constellation`() {
        val satellites = listOf(sat(constellation = "GPS"), sat(constellation = "GPS"), sat(constellation = "GLONASS"))

        val counts = GnssStatusMapper.fromSatellites(satellites).constellationCounts

        assertEquals(2, counts["GPS"])
        assertEquals(1, counts["GLONASS"])
    }

    @Test
    fun `empty satellite list produces zeroed-out snapshot without crashing`() {
        val snapshot = GnssStatusMapper.fromSatellites(emptyList())

        assertEquals(0, snapshot.usedInFixCount)
        assertEquals(0, snapshot.dualFrequencySatelliteCount)
        assertEquals(emptyMap(), snapshot.constellationCounts)
    }

    private fun sat(
        constellation: String = "GPS",
        svid: Int = 1,
        cn0DbHz: Float = 30f,
        usedInFix: Boolean = true,
        carrierFrequencyHz: Float? = 1575.42e6f,
    ) = GnssSatelliteInfo(constellation, svid, cn0DbHz, usedInFix, carrierFrequencyHz)
}

class GnssStatusMapperMapTest {

    @Test
    fun `map extracts every field from a real GnssStatus, nulling carrier frequency when absent`() {
        val status: GnssStatus = mockk()
        every { status.satelliteCount } returns 2

        every { status.getConstellationType(0) } returns GnssStatus.CONSTELLATION_GPS
        every { status.getSvid(0) } returns 5
        every { status.getCn0DbHz(0) } returns 32.5f
        every { status.usedInFix(0) } returns true
        every { status.hasCarrierFrequencyHz(0) } returns true
        every { status.getCarrierFrequencyHz(0) } returns 1575.42e6f

        every { status.getConstellationType(1) } returns GnssStatus.CONSTELLATION_GALILEO
        every { status.getSvid(1) } returns 9
        every { status.getCn0DbHz(1) } returns 18.0f
        every { status.usedInFix(1) } returns false
        every { status.hasCarrierFrequencyHz(1) } returns false

        val snapshot = GnssStatusMapper.map(status)

        assertEquals(2, snapshot.satellites.size)
        assertEquals(GnssSatelliteInfo("GPS", 5, 32.5f, true, 1575.42e6f), snapshot.satellites[0])
        assertEquals("GALILEO", snapshot.satellites[1].constellation)
        assertNull(snapshot.satellites[1].carrierFrequencyHz)
        assertEquals(1, snapshot.usedInFixCount)
    }
}
