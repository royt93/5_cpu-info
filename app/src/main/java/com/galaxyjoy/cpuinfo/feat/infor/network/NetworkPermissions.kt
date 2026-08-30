package com.galaxyjoy.cpuinfo.feat.infor.network

import android.Manifest
import android.os.Build

/**
 * Which runtime permission unlocks Wi-Fi SSID/BSSID, chosen per API level — see the manifest
 * comment next to these `<uses-permission>` entries for why two different permissions exist.
 */
object NetworkPermissions {

    fun wifiDetailPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
}
