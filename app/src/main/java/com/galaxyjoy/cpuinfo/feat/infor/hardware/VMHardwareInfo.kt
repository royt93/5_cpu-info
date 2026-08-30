@file:Suppress("DEPRECATION")

package com.galaxyjoy.cpuinfo.feat.infor.hardware

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.ConsumerIrManager
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.RandomAccessFile
import javax.inject.Inject

/**
 * ViewModel for [FrmHardwareInfo]
 *
 */
@HiltViewModel
class VMHardwareInfo @Inject constructor(
    private val resources: Resources,
    private val packageManager: PackageManager,
    private val contentResolver: ContentResolver,
    private val wifiManager: WifiManager,
    private val irManager: ConsumerIrManager?,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    init {
        refreshHardwareInfo()
    }

    /**
     * Refresh all info connected with hardware: wireless connection (Wi-Fi, Bluetooth, NFC, IR)
     * and USB
     */
    @Synchronized
    fun refreshHardwareInfo() {
        if (listLiveData.isNotEmpty()) {
            listLiveData.clear()
        }

        // Battery info moved to the dedicated Battery tab (VMBatteryInfo) — richer capacity/
        // current diagnostics than fit here, and it polls live instead of only on power-connect.
        // Camera info moved to the dedicated Camera tab (VMCameraInfo, Camera2 API) — this
        // section used the deprecated android.hardware.Camera API and duplicated that tab.

        listLiveData.addAll(getWirelessInfo())
        listLiveData.addAll(getUsbInfo())
    }

    /**
     * Get Wi-Fi and Bluetooth mac address and Bluetooth LE support
     */
    @SuppressLint("InlinedApi")
    private fun getWirelessInfo(): List<Pair<String, String>> {
        val functionsList = mutableListOf<Pair<String, String>>()
        functionsList.add(resources.getString(R.string.wireless) to "")
        // Bluetooth
        functionsList.add(
            resources.getString(R.string.bluetooth) to getYesNoString(
                packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            )
        )
        val hasBluetoothLe = getYesNoString(
            packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        )
        functionsList.add(resources.getString(R.string.bluetooth_le) to hasBluetoothLe)
        // GPS
        functionsList.add(
            "GPS" to getYesNoString(
                packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
            )
        )
        // NFC
        functionsList.add(
            "NFC" to getYesNoString(
                packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
            )
        )
        functionsList.add(
            "NFC Card Emulation" to getYesNoString(
                packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
            )
        )
        // Wi-Fi
        val hasWiFi = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        functionsList.add("Wi-Fi" to getYesNoString(hasWiFi))
        if (hasWiFi) {
            functionsList.add(
                "Wi-Fi Aware" to getYesNoString(
                    packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
                )
            )
            functionsList.add(
                "Wi-Fi Direct" to getYesNoString(
                    packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
                )
            )
            functionsList.add(
                "Wi-Fi Passpoint" to getYesNoString(
                    packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_PASSPOINT)
                )
            )
            functionsList.add("Wi-Fi 5Ghz" to getYesNoString(wifiManager.is5GHzBandSupported))
            functionsList.add("Wi-Fi P2P" to getYesNoString(wifiManager.isP2pSupported))
        }

        try {
            val bluetoothMac = android.provider.Settings.Secure.getString(
                contentResolver,
                "bluetooth_address"
            )
            if (bluetoothMac != null && bluetoothMac.isNotEmpty())
                functionsList.add(resources.getString(R.string.bluetooth_mac) to bluetoothMac)
        } catch (_: Exception) {
            // ignored
        }

        // Wi-Fi mac
        val filePath = "/sys/class/net/wlan0/address"
        try {
            val reader = RandomAccessFile(filePath, "r")
            val value = reader.readLine()
            reader.close()
            functionsList.add(resources.getString(R.string.wifi_mac) to value)
        } catch (_: Exception) {
        }

        // IR
        val hasIr = irManager?.hasIrEmitter() == true
        functionsList.add(resources.getString(R.string.ir_emitter) to getYesNoString(hasIr))

        return functionsList
    }

    private fun getUsbInfo(): List<Pair<String, String>> {
        val featureList = mutableListOf<Pair<String, String>>()
        featureList.add("USB" to "")
        featureList.add(
            "OTG" to getYesNoString(
                packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
            )
        )
        return featureList
    }

    private fun getYesNoString(yesValue: Boolean) = if (yesValue) {
        resources.getString(R.string.yes)
    } else {
        resources.getString(R.string.no)
    }
}
