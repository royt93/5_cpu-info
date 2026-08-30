package com.galaxyjoy.cpuinfo.domain.model

import androidx.annotation.Keep

@Keep
data class HardwareData(
    val hasBluetooth: Boolean,
    val hasBluetoothLe: Boolean,
    val hasGps: Boolean,
    val hasNfc: Boolean,
    val hasNfcCardEmulation: Boolean,
    val hasWifi: Boolean,
    val hasWifiAware: Boolean,
    val hasWifiDirect: Boolean,
    val hasWifiPasspoint: Boolean,
    val hasWifi5Ghz: Boolean,
    val hasWifiP2p: Boolean,
    val bluetoothMac: String?,
    val wifiMac: String?,
    val hasIrEmitter: Boolean,
    val hasUsbHost: Boolean,
)
