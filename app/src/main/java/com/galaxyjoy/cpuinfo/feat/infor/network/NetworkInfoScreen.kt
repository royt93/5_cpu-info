package com.galaxyjoy.cpuinfo.feat.infor.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R

@Composable
internal fun NetworkInfoScreen(
    uiState: VMNetworkInfo.UiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionCard(title = stringResource(R.string.network_wifi_section)) {
            if (!uiState.wifi.connected) {
                InfoRow(stringResource(R.string.network_status), stringResource(R.string.network_disconnected))
            } else {
                if (!uiState.hasWifiDetailPermission) {
                    PermissionPrompt(
                        canRequestPermission = uiState.canRequestPermission,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenSettings,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                InfoRow(stringResource(R.string.network_status), stringResource(R.string.network_connected))
                uiState.wifi.ssid?.let { InfoRow(stringResource(R.string.network_ssid), it) }
                uiState.wifi.bssid?.let { InfoRow(stringResource(R.string.network_bssid), it) }
                uiState.wifi.ipAddress?.let { InfoRow(stringResource(R.string.network_ip_address), it) }
                uiState.wifi.gatewayAddress?.let { InfoRow(stringResource(R.string.network_gateway), it) }
                if (uiState.wifi.dnsAddresses.isNotEmpty()) {
                    InfoRow(stringResource(R.string.network_dns), uiState.wifi.dnsAddresses.joinToString(", "))
                }
                uiState.wifi.rssiDbm?.let { rssi ->
                    val quality = NetworkInfoFormatter.signalQuality(rssi)
                    InfoRow(
                        stringResource(R.string.network_signal_strength),
                        "$rssi dBm (${signalQualityLabel(quality)})",
                    )
                }
                uiState.wifi.linkSpeedMbps?.let {
                    InfoRow(stringResource(R.string.network_link_speed), stringResource(R.string.network_mbps_value, it))
                }
                InfoRow(stringResource(R.string.network_band), NetworkInfoFormatter.bandLabel(uiState.wifi.frequencyMhz))
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = stringResource(R.string.network_mobile_section)) {
            InfoRow(
                stringResource(R.string.network_status),
                stringResource(if (uiState.mobile.connected) R.string.network_connected else R.string.network_disconnected),
            )
            uiState.mobile.carrierName?.let { InfoRow(stringResource(R.string.network_carrier), it) }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = stringResource(R.string.network_vpn_section)) {
            InfoRow(
                stringResource(R.string.network_status),
                stringResource(if (uiState.vpn.active) R.string.network_active else R.string.network_inactive),
            )
        }
    }
}

@Composable
private fun signalQualityLabel(quality: NetworkInfoFormatter.SignalQuality): String = stringResource(
    when (quality) {
        NetworkInfoFormatter.SignalQuality.EXCELLENT -> R.string.network_signal_excellent
        NetworkInfoFormatter.SignalQuality.GOOD -> R.string.network_signal_good
        NetworkInfoFormatter.SignalQuality.FAIR -> R.string.network_signal_fair
        NetworkInfoFormatter.SignalQuality.POOR -> R.string.network_signal_poor
        NetworkInfoFormatter.SignalQuality.UNKNOWN -> R.string.unknown
    },
)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
private fun PermissionPrompt(
    canRequestPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = Color(0xFFFFA726), shape = CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.network_permission_rationale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (canRequestPermission) {
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.network_grant_permission_button))
                }
            } else {
                OutlinedButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.network_open_settings_button))
                }
            }
        }
    }
}
