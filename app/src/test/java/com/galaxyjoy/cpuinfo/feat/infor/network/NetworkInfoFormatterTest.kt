package com.galaxyjoy.cpuinfo.feat.infor.network

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class NetworkInfoFormatterTest {

    @Test
    fun `null rssi is unknown signal quality`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.UNKNOWN, NetworkInfoFormatter.signalQuality(null))
    }

    @Test
    fun `strong rssi is excellent`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.EXCELLENT, NetworkInfoFormatter.signalQuality(-45))
    }

    @Test
    fun `boundary rssi of -50 is excellent`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.EXCELLENT, NetworkInfoFormatter.signalQuality(-50))
    }

    @Test
    fun `rssi just below excellent boundary is good`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.GOOD, NetworkInfoFormatter.signalQuality(-51))
    }

    @Test
    fun `moderate rssi is fair`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.FAIR, NetworkInfoFormatter.signalQuality(-65))
    }

    @Test
    fun `weak rssi is poor`() {
        assertEquals(NetworkInfoFormatter.SignalQuality.POOR, NetworkInfoFormatter.signalQuality(-85))
    }

    @Test
    fun `2point4 GHz frequency maps to correct band`() {
        assertEquals("2.4 GHz", NetworkInfoFormatter.bandLabel(2437))
    }

    @Test
    fun `5 GHz frequency maps to correct band`() {
        assertEquals("5 GHz", NetworkInfoFormatter.bandLabel(5180))
    }

    @Test
    fun `6 GHz frequency maps to correct band`() {
        assertEquals("6 GHz", NetworkInfoFormatter.bandLabel(6115))
    }

    @Test
    fun `unrecognized frequency is unknown band`() {
        assertEquals("Unknown", NetworkInfoFormatter.bandLabel(999))
    }

    @Test
    fun `null frequency is unknown band`() {
        assertEquals("Unknown", NetworkInfoFormatter.bandLabel(null))
    }

    @Test
    fun `cleanSsid strips surrounding quotes`() {
        assertEquals("MyNetwork", NetworkInfoFormatter.cleanSsid("\"MyNetwork\""))
    }

    @Test
    fun `cleanSsid returns null for the unknown-ssid sentinel`() {
        assertNull(NetworkInfoFormatter.cleanSsid("<unknown ssid>"))
    }

    @Test
    fun `cleanSsid returns null for blank input`() {
        assertNull(NetworkInfoFormatter.cleanSsid(""))
        assertNull(NetworkInfoFormatter.cleanSsid(null))
    }
}
