package com.galaxyjoy.cpuinfo.data.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.net.wifi.WifiManager
import android.provider.Settings
import com.galaxyjoy.cpuinfo.domain.model.HardwareData
import java.io.RandomAccessFile
import javax.inject.Inject

/**
 * Wireless (Bluetooth/GPS/NFC/Wi-Fi/IR) and USB capability flags — all static for the life of the
 * process, read once by [com.galaxyjoy.cpuinfo.domain.observable.ObservableHardwareData].
 */
class DataProviderHardware @Inject constructor(
    private val packageManager: PackageManager,
    private val contentResolver: ContentResolver,
    private val wifiManager: WifiManager,
    private val irManager: ConsumerIrManager?,
) {

    @SuppressLint("InlinedApi")
    fun getHardwareData(): HardwareData {
        val hasWifi = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        return HardwareData(
            hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothLe = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasGps = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            hasNfc = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            hasNfcCardEmulation = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION),
            hasWifi = hasWifi,
            hasWifiAware = hasWifi && packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE),
            hasWifiDirect = hasWifi && packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT),
            hasWifiPasspoint = hasWifi && packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_PASSPOINT),
            hasWifi5Ghz = hasWifi && wifiManager.is5GHzBandSupported,
            hasWifiP2p = hasWifi && wifiManager.isP2pSupported,
            bluetoothMac = getBluetoothMac(),
            wifiMac = getWifiMac(),
            hasIrEmitter = irManager?.hasIrEmitter() == true,
            hasUsbHost = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
        )
    }

    private fun getBluetoothMac(): String? = try {
        Settings.Secure.getString(contentResolver, "bluetooth_address").takeIf { !it.isNullOrEmpty() }
    } catch (_: Exception) {
        null
    }

    private fun getWifiMac(): String? = try {
        RandomAccessFile("/sys/class/net/wlan0/address", "r").use { it.readLine() }
    } catch (_: Exception) {
        null
    }
}
