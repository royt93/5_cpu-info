package com.galaxyjoy.cpuinfo.feat.infor.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import javax.inject.Inject

class NetworkInfoProvider @Inject constructor(
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    private val telephonyManager: TelephonyManager,
) {

    data class WifiSnapshot(
        val connected: Boolean,
        val ssid: String?,
        val bssid: String?,
        val ipAddress: String?,
        val gatewayAddress: String?,
        val dnsAddresses: List<String>,
        val rssiDbm: Int?,
        val linkSpeedMbps: Int?,
        val frequencyMhz: Int?,
    )

    data class MobileSnapshot(
        val connected: Boolean,
        val carrierName: String?,
    )

    data class VpnSnapshot(val active: Boolean)

    /**
     * @param hasWifiDetailPermission whether the location-adjacent runtime permission (see
     * [NetworkPermissions]) is currently granted — without it, SSID/BSSID come back null, but
     * IP/gateway/DNS/signal/link-speed/frequency are unaffected (they don't need that permission).
     */
    fun wifiSnapshot(hasWifiDetailPermission: Boolean): WifiSnapshot {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isWifi) {
            return WifiSnapshot(
                connected = false,
                ssid = null,
                bssid = null,
                ipAddress = null,
                gatewayAddress = null,
                dnsAddresses = emptyList(),
                rssiDbm = null,
                linkSpeedMbps = null,
                frequencyMhz = null,
            )
        }

        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo

        val ssid = if (hasWifiDetailPermission) {
            NetworkInfoFormatter.cleanSsid(wifiInfo?.ssid)
        } else {
            null
        }
        val bssid = if (hasWifiDetailPermission) wifiInfo?.bssid else null

        val linkProperties = network?.let { connectivityManager.getLinkProperties(it) }
        val ipAddress = linkProperties?.linkAddresses?.firstOrNull()?.address?.hostAddress
        val gatewayAddress = linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute }
            ?.gateway?.hostAddress
        val dnsAddresses = linkProperties?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()

        return WifiSnapshot(
            connected = true,
            ssid = ssid,
            bssid = bssid,
            ipAddress = ipAddress,
            gatewayAddress = gatewayAddress,
            dnsAddresses = dnsAddresses,
            rssiDbm = wifiInfo?.rssi,
            linkSpeedMbps = wifiInfo?.linkSpeed,
            frequencyMhz = wifiInfo?.frequency,
        )
    }

    /** Carrier name only — no `READ_PHONE_STATE` needed, scoping this permission-free per design. */
    fun mobileSnapshot(): MobileSnapshot {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        return MobileSnapshot(
            connected = isCellular,
            carrierName = telephonyManager.networkOperatorName.takeIf { it.isNotBlank() },
        )
    }

    fun vpnSnapshot(): VpnSnapshot {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        return VpnSnapshot(active = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
    }
}
