package com.galaxyjoy.cpuinfo.feat.p2pcompare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R

private const val BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0

@Composable
internal fun P2PCompareScreen(
    uiState: VMP2PCompare.UiState,
    onPastedCodeChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.p2p_compare_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        when (uiState) {
            is VMP2PCompare.UiState.Export -> ExportContent(uiState, onPastedCodeChanged, onCompareClicked, onShareClicked)
            is VMP2PCompare.UiState.Result -> ResultContent(uiState.comparison, onBackClicked)
        }
    }
}

@Composable
private fun ExportContent(
    state: VMP2PCompare.UiState.Export,
    onPastedCodeChanged: (String) -> Unit,
    onCompareClicked: () -> Unit,
    onShareClicked: () -> Unit,
) {
    OutlinedButton(onClick = onShareClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
        Text(text = stringResource(R.string.p2p_compare_share_button), modifier = Modifier.padding(start = 8.dp))
    }
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = state.pastedCode,
        onValueChange = onPastedCodeChanged,
        label = { Text(stringResource(R.string.p2p_compare_paste_label)) },
        modifier = Modifier.fillMaxWidth(),
        isError = state.parseError,
        supportingText = {
            if (state.parseError) Text(stringResource(R.string.p2p_compare_parse_error))
        },
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onCompareClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.p2p_compare_compare_button))
    }
}

@Composable
private fun ResultContent(comparison: DeviceCompareEvaluator.Result, onBackClicked: () -> Unit) {
    Text(
        text = comparison.local.deviceModel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = comparison.remote.deviceModel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))

    CompareRow(
        label = stringResource(R.string.fleet_compare_ram_label),
        field = comparison.ram,
    )
    CompareRow(
        label = stringResource(R.string.fleet_compare_storage_label),
        field = comparison.storage,
    )

    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onBackClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.p2p_compare_back_button))
    }
}

@Composable
private fun CompareRow(label: String, field: DeviceCompareEvaluator.FieldComparison) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.fleet_compare_gb_value, bytesToGb(field.localBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = winnerColor(field.winner == DeviceCompareEvaluator.Winner.LOCAL),
                )
                Text(
                    text = stringResource(R.string.fleet_compare_gb_value, bytesToGb(field.remoteBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = winnerColor(field.winner == DeviceCompareEvaluator.Winner.REMOTE),
                )
            }
        }
    }
}

@Composable
private fun winnerColor(isWinner: Boolean) =
    if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

private fun bytesToGb(bytes: Long): Int = (bytes / BYTES_PER_GB).toInt()
