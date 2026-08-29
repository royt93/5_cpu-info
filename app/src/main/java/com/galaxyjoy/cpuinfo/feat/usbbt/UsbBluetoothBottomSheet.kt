package com.galaxyjoy.cpuinfo.feat.usbbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * F03 "USB/BT Inspector" — attached USB devices (zero permission needed to enumerate) plus
 * Bluetooth adapter state. Paired-device count is only shown pre-API31, since showing it on
 * newer devices would need the dangerous `BLUETOOTH_CONNECT` runtime permission, which this
 * app deliberately doesn't request anywhere else.
 */
@AndroidEntryPoint
class UsbBluetoothBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var usbInspectorProvider: UsbInspectorProvider

    @Inject
    lateinit var bluetoothInspectorProvider: BluetoothInspectorProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val usbDevices = usbInspectorProvider.listAttachedDevices()
        val bluetoothStatus = bluetoothInspectorProvider.status()

        setContent {
            CpuInfoTheme {
                UsbBluetoothContent(usbDevices = usbDevices, bluetoothStatus = bluetoothStatus)
            }
        }
    }

    companion object {
        const val TAG = "UsbBluetoothBottomSheet"
    }
}

@Composable
private fun UsbBluetoothContent(
    usbDevices: List<UsbInspectorProvider.UsbDeviceInfo>,
    bluetoothStatus: BluetoothInspectorProvider.BluetoothStatus,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.usb_bt_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            SectionTitle(stringResource(R.string.usb_bt_bluetooth_section))
            BluetoothStatusCard(bluetoothStatus)

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.usb_bt_usb_section, usbDevices.size))

            if (usbDevices.isEmpty()) {
                Text(
                    text = stringResource(R.string.usb_bt_no_usb_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            } else {
                usbDevices.forEach { device ->
                    UsbDeviceCard(device)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun BluetoothStatusCard(status: BluetoothInspectorProvider.BluetoothStatus) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CapabilityRow(
                stringResource(R.string.usb_bt_bluetooth_supported),
                stringResource(if (status.supported) R.string.yes else R.string.no),
            )
            if (status.supported) {
                CapabilityRow(
                    stringResource(R.string.usb_bt_bluetooth_enabled),
                    stringResource(if (status.enabled) R.string.yes else R.string.no),
                )
                CapabilityRow(
                    stringResource(R.string.usb_bt_bluetooth_paired_count),
                    status.pairedDeviceCount?.toString()
                        ?: stringResource(R.string.usb_bt_bluetooth_paired_unavailable),
                )
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UsbDeviceCard(device: UsbInspectorProvider.UsbDeviceInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.productName ?: device.deviceName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (device.manufacturerName != null) {
                Text(
                    text = device.manufacturerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            CapabilityRow(
                stringResource(R.string.usb_bt_vendor_product_id),
                "%04X:%04X".format(device.vendorId, device.productId),
            )
            CapabilityRow(
                stringResource(R.string.usb_bt_device_class),
                UsbDeviceClassCatalog.labelFor(device.deviceClass),
            )
            CapabilityRow(
                stringResource(R.string.usb_bt_interface_count),
                device.interfaceCount.toString(),
            )
        }
    }
}
