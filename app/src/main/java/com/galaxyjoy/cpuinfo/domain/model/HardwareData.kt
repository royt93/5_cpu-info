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
    /** E05 haptics profile — no permission needed, pure [android.os.Vibrator] capability reads. */
    val hasHapticsAmplitudeControl: Boolean,
    /** Null on API<30 — `areAllPrimitivesSupported` doesn't exist there. */
    val hasHapticsAllPrimitives: Boolean?,
    /** Null on API<33, or when the driver reports 0 (unsupported) on API33+. */
    val hapticsResonantFrequencyHz: Float?,
)
