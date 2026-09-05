package com.galaxyjoy.cpuinfo.feat.storagetruth

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
 * E01 "Storage Truth" — same one-shot "measure right now" shape as this app's other benchmark
 * screens. Two-phase Running state (Writing then Verifying) instead of a single progress bar,
 * since [StorageTruthRunner] genuinely does two separate full passes.
 */
@Composable
internal fun StorageTruthScreen(
    uiState: VMStorageTruth.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (StorageTruthBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMStorageTruth.UiState.Idle -> IdleContent(onStartClicked)
            is VMStorageTruth.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMStorageTruth.UiState.Done -> DoneContent(uiState.result, onDoneClicked, onShareClicked)
            is VMStorageTruth.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
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
        text = stringResource(R.string.storage_truth_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.storage_truth_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.storage_truth_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMStorageTruth.UiState.Running, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    val label = when (state.phase) {
        StorageTruthRunner.Phase.WRITING -> stringResource(R.string.storage_truth_writing_label, state.blockIndex + 1, state.blockCount)
        StorageTruthRunner.Phase.VERIFYING -> stringResource(R.string.storage_truth_verifying_label)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.storage_truth_stop_button))
    }
}

@Composable
private fun AbortedContent(reason: StorageTruthBenchmark.AbortReason, onDoneClicked: () -> Unit) {
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
                StorageTruthBenchmark.AbortReason.OVERHEAT -> R.string.storage_truth_aborted_overheat
                StorageTruthBenchmark.AbortReason.INTERRUPTED -> R.string.storage_truth_aborted_interrupted
                StorageTruthBenchmark.AbortReason.INSUFFICIENT_SPACE -> R.string.storage_truth_aborted_insufficient_space
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.storage_truth_done_button))
    }
}

@Composable
private fun DoneContent(
    result: StorageTruthBenchmark.Result,
    onDoneClicked: () -> Unit,
    onShareClicked: (StorageTruthBenchmark.Result) -> Unit,
) {
    val verdict = StorageTruthBenchmark.evaluate(result)
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = if (verdict == StorageTruthBenchmark.Verdict.SUSPECT_FAKE) Icons.Default.Warning else Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (verdict == StorageTruthBenchmark.Verdict.SUSPECT_FAKE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))

    val mb = NumberFormat.getIntegerInstance(Locale.getDefault())
    val testedMb = result.blocksTested * StorageTruthBenchmark.BLOCK_SIZE_BYTES / (1024 * 1024)

    Text(
        text = stringResource(
            when (verdict) {
                StorageTruthBenchmark.Verdict.GENUINE -> R.string.storage_truth_verdict_genuine
                StorageTruthBenchmark.Verdict.SUSPECT_FAKE -> R.string.storage_truth_verdict_suspect_fake
                StorageTruthBenchmark.Verdict.INCONCLUSIVE -> R.string.storage_truth_verdict_inconclusive
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (verdict == StorageTruthBenchmark.Verdict.SUSPECT_FAKE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))

    ResultRow(
        label = stringResource(R.string.storage_truth_tested_label),
        value = stringResource(R.string.storage_truth_tested_value, mb.format(testedMb)),
    )

    if (verdict == StorageTruthBenchmark.Verdict.SUSPECT_FAKE) {
        val firstMismatch = result.mismatches.minByOrNull { it.blockIndex }
        if (firstMismatch != null) {
            val firstMismatchMb = firstMismatch.expectedOffsetBytes / (1024 * 1024)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.storage_truth_mismatch_detail, mb.format(firstMismatchMb)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.storage_truth_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.storage_truth_done_button))
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
