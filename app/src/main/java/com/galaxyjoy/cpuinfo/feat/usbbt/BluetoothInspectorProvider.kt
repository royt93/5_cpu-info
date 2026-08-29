package com.galaxyjoy.cpuinfo.feat.usbbt

import android.bluetooth.BluetoothManager
import javax.inject.Inject

/**
 * Reads Bluetooth adapter state. [android.bluetooth.BluetoothAdapter.isEnabled] needs no
 * permission at all. Paired-device count needs `BLUETOOTH_CONNECT` on API 31+ (a dangerous
 * runtime permission this app deliberately doesn't request — no other feature needs it, and
 * adding a first-ever runtime-permission flow just for a device count isn't worth the UX
 * surface). On API 30 and below the normal `BLUETOOTH` manifest permission covers it with no
 * dialog, so the count is always shown there.
 */
class BluetoothInspectorProvider @Inject constructor(
    private val bluetoothManager: BluetoothManager,
) {

    data class BluetoothStatus(
        val supported: Boolean,
        val enabled: Boolean,
        val pairedDeviceCount: Int?,
    )

    fun status(): BluetoothStatus {
        val adapter = bluetoothManager.adapter ?: return BluetoothStatus(
            supported = false,
            enabled = false,
            pairedDeviceCount = null,
        )

        val pairedCount = try {
            adapter.bondedDevices?.size
        } catch (_: SecurityException) {
            null
        }

        return BluetoothStatus(
            supported = true,
            enabled = adapter.isEnabled,
            pairedDeviceCount = pairedCount,
        )
    }
}
