package com.galaxyjoy.cpuinfo.feat.ramtruth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.NumberFormat
import java.util.Locale

/**
 * E03 "RAM Truth" — same one-shot "measure right now" shape as this app's other Truth-series
 * benchmarks ([com.galaxyjoy.cpuinfo.feat.storagetruth.StorageTruthScreen]).
 */
@Composable
internal fun RamTruthScreen(
    uiState: VMRamTruth.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (RamTruthBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMRamTruth.UiState.Idle -> IdleContent(onStartClicked)
            is VMRamTruth.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMRamTruth.UiState.Done -> DoneContent(uiState.result, onDoneClicked, onShareClicked)
            is VMRamTruth.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
        }
    }
}

@Composable
private fun IdleContent(onStartClicked: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.ram_truth_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.ram_truth_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.ram_truth_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMRamTruth.UiState.Running, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.ram_truth_running_label, state.chunkIndex + 1, state.chunkCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.ram_truth_stop_button))
    }
}

@Composable
private fun AbortedContent(reason: RamTruthBenchmark.AbortReason, onDoneClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(
            when (reason) {
                RamTruthBenchmark.AbortReason.INTERRUPTED -> R.string.ram_truth_aborted_interrupted
                RamTruthBenchmark.AbortReason.INSUFFICIENT_AVAILABLE_MEMORY -> R.string.ram_truth_aborted_insufficient_memory
                RamTruthBenchmark.AbortReason.LOW_MEMORY_SIGNAL -> R.string.ram_truth_aborted_low_memory
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.ram_truth_done_button))
    }
}

@Composable
private fun DoneContent(
    result: RamTruthBenchmark.Result,
    onDoneClicked: () -> Unit,
    onShareClicked: (RamTruthBenchmark.Result) -> Unit,
) {
    val verdict = RamTruthBenchmark.evaluate(result)
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = if (verdict == RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM) Icons.Default.Warning else Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (verdict == RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))

    val mb = NumberFormat.getIntegerInstance(Locale.getDefault())
    val testedMb = result.measurements.size * RamTruthBenchmark.CHUNK_SIZE_BYTES / (1024 * 1024)

    Text(
        text = stringResource(
            when (verdict) {
                RamTruthBenchmark.Verdict.GENUINE -> R.string.ram_truth_verdict_genuine
                RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM -> R.string.ram_truth_verdict_suspect
                RamTruthBenchmark.Verdict.INCONCLUSIVE -> R.string.ram_truth_verdict_inconclusive
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (verdict == RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))

    ResultRow(
        label = stringResource(R.string.ram_truth_tested_label),
        value = stringResource(R.string.ram_truth_tested_value, mb.format(testedMb)),
    )

    if (verdict == RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM) {
        val cliffMb = (RamTruthBenchmark.cliffAtBytes(result) ?: 0L) / (1024 * 1024)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ram_truth_cliff_detail, mb.format(cliffMb)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.ram_truth_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.ram_truth_done_button))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
