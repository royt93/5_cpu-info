package com.galaxyjoy.cpuinfo.feat.storagebench

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusMapper
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import com.galaxyjoy.cpuinfo.ui.component.BenchTrendChart
import com.galaxyjoy.cpuinfo.util.BenchPercentileCalculator
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun StorageBenchScreen(
    uiState: VMStorageBench.UiState,
    thermalSnapshot: ThermalStatusProvider.Snapshot,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (StorageBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (thermalSnapshot.statusSupported) {
            ThermalStatusCard(thermalSnapshot)
            Spacer(Modifier.height(16.dp))
        }
        when (uiState) {
            is VMStorageBench.UiState.Idle -> IdleContent(uiState.previous, onStartClicked)
            is VMStorageBench.UiState.Running -> RunningContent(uiState.phase, onStopClicked)
            is VMStorageBench.UiState.Done -> DoneContent(uiState, onDoneClicked, onShareClicked)
            VMStorageBench.UiState.Aborted -> AbortedContent(onDoneClicked)
        }
    }
}

@Composable
private fun ThermalStatusCard(snapshot: ThermalStatusProvider.Snapshot) {
    val mapping = ThermalStatusMapper.mappingFor(snapshot.status)
    val color = when (mapping.severity) {
        ThermalStatusMapper.Severity.OK -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ThermalStatusMapper.Severity.WARNING -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        ThermalStatusMapper.Severity.DANGER -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.thermal_status_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(mapping.labelRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun IdleContent(previous: StorageBenchResultPrefs.SavedResult?, onStartClicked: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.storage_bench_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.storage_bench_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))

    if (previous != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    R.string.storage_bench_previous_summary,
                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(previous.timestampMs)),
                    formatDecimal(previous.seqWriteMbPerSec),
                    formatDecimal(previous.seqReadMbPerSec),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.storage_bench_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(phase: StorageBenchmarkRunner.Phase, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.storage_bench_running_label, stringResource(phase.labelRes())),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.storage_bench_stop_button))
    }
}

private fun StorageBenchmarkRunner.Phase.labelRes(): Int = when (this) {
    StorageBenchmarkRunner.Phase.SEQ_WRITE -> R.string.storage_bench_phase_seq_write
    StorageBenchmarkRunner.Phase.SEQ_READ -> R.string.storage_bench_phase_seq_read
    StorageBenchmarkRunner.Phase.RANDOM_WRITE -> R.string.storage_bench_phase_random_write
    StorageBenchmarkRunner.Phase.RANDOM_READ -> R.string.storage_bench_phase_random_read
    StorageBenchmarkRunner.Phase.HASH -> R.string.storage_bench_phase_hash
}

@Composable
private fun AbortedContent(onDoneClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.storage_bench_aborted_overheat),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.storage_bench_done_button))
    }
}

@Composable
private fun DoneContent(
    state: VMStorageBench.UiState.Done,
    onDoneClicked: () -> Unit,
    onShareClicked: (StorageBenchmark.Result) -> Unit,
) {
    val result = state.result
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(20.dp))

    ResultRow(stringResource(R.string.storage_bench_phase_seq_write), formatMbPerSec(result.seqWriteMbPerSec))
    ResultRow(stringResource(R.string.storage_bench_phase_seq_read), formatMbPerSec(result.seqReadMbPerSec))
    ResultRow(stringResource(R.string.storage_bench_phase_random_write), formatOpsPerSec(result.randomWriteOpsPerSec))
    ResultRow(stringResource(R.string.storage_bench_phase_random_read), formatOpsPerSec(result.randomReadOpsPerSec))
    ResultRow(stringResource(R.string.storage_bench_phase_hash), formatMbPerSec(result.hashMbPerSec))

    Spacer(Modifier.height(16.dp))
    if (state.previous != null) {
        Text(
            text = stringResource(
                R.string.storage_bench_previous_summary,
                DateFormat.getDateInstance(DateFormat.SHORT).format(Date(state.previous.timestampMs)),
                formatDecimal(state.previous.seqWriteMbPerSec),
                formatDecimal(state.previous.seqReadMbPerSec),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(R.string.storage_bench_no_previous),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(16.dp))
    BenchTrendChart(values = state.history.map { it.seqWriteMbPerSec })

    BenchPercentileCalculator.percentileOfLast(state.history.map { it.seqWriteMbPerSec })?.let { percentile ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.bench_percentile_message, percentile),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.storage_bench_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.storage_bench_done_button))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatMbPerSec(value: Double): String = "${formatDecimal(value)} MB/s"

private fun formatOpsPerSec(value: Double): String = "${"%.0f".format(Locale.US, value)} ops/s"

/** Always renders with a "." decimal point, regardless of the device's default locale. */
private fun formatDecimal(value: Double): String = "%.1f".format(Locale.US, value)
