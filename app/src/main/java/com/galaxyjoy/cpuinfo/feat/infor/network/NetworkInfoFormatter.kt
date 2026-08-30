package com.galaxyjoy.cpuinfo.feat.infor.network

/**
 * #5 "Network Info" — pure formatting helpers, kept free of Android framework types so the
 * signal-quality/band thresholds are unit-testable without mocking `WifiInfo`.
 */
object NetworkInfoFormatter {

    enum class SignalQuality { EXCELLENT, GOOD, FAIR, POOR, UNKNOWN }

    /** Standard Android Wi-Fi RSSI buckets (same thresholds `WifiManager.calculateSignalLevel` implies). */
    fun signalQuality(rssiDbm: Int?): SignalQuality = when {
        rssiDbm == null -> SignalQuality.UNKNOWN
        rssiDbm >= -50 -> SignalQuality.EXCELLENT
        rssiDbm >= -60 -> SignalQuality.GOOD
        rssiDbm >= -70 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }

    fun bandLabel(frequencyMhz: Int?): String = when {
        frequencyMhz == null -> "Unknown"
        frequencyMhz in 2400..2500 -> "2.4 GHz"
        frequencyMhz in 4900..5900 -> "5 GHz"
        frequencyMhz in 5925..7125 -> "6 GHz"
        else -> "Unknown"
    }

    /** Strips the surrounding quotes Android's `WifiInfo.getSSID()` wraps a real SSID in. */
    fun cleanSsid(rawSsid: String?): String? {
        if (rawSsid.isNullOrBlank() || rawSsid == UNKNOWN_SSID) return null
        return rawSsid.removeSurrounding("\"")
    }

    private const val UNKNOWN_SSID = "<unknown ssid>"
}
