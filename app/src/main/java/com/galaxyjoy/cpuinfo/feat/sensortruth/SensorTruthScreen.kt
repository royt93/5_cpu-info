package com.galaxyjoy.cpuinfo.feat.sensortruth

import android.hardware.Sensor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import java.util.Locale

/**
 * E02 "Sensor Truth" — same shape as the other Truth-series screens, but the `Done` state lists
 * one row per audited sensor (each with its own verdict) instead of a single overall result, same
 * "list of independent sub-results" pattern as
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchScreen]/[com.galaxyjoy.cpuinfo.feat.siliconlottery.SiliconLotteryScreen].
 */
@Composable
internal fun SensorTruthScreen(
    uiState: VMSensorTruth.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (SensorTruthBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMSensorTruth.UiState.Idle -> IdleContent(onStartClicked)
            is VMSensorTruth.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMSensorTruth.UiState.Done -> DoneContent(uiState.result, onDoneClicked, onShareClicked)
            is VMSensorTruth.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
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
        text = stringResource(R.string.sensor_truth_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.sensor_truth_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.sensor_truth_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMSensorTruth.UiState.Running, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(
            R.string.sensor_truth_running_label,
            state.sensorIndex + 1,
            state.sensorCount,
            stringResource(sensorNameRes(state.sensorType)),
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.sensor_truth_stop_button))
    }
}

@Composable
private fun AbortedContent(reason: SensorTruthBenchmark.AbortReason, onDoneClicked: () -> Unit) {
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
                SensorTruthBenchmark.AbortReason.INTERRUPTED -> R.string.sensor_truth_aborted_interrupted
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.sensor_truth_done_button))
    }
}

@Composable
private fun DoneContent(
    result: SensorTruthBenchmark.Result,
    onDoneClicked: () -> Unit,
    onShareClicked: (SensorTruthBenchmark.Result) -> Unit,
) {
    val overall = SensorTruthBenchmark.overallVerdict(result)
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = if (overall == SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING) Icons.Default.Warning else Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (overall == SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))

    result.audits.forEach { audit ->
        SensorAuditCard(audit)
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.sensor_truth_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.sensor_truth_done_button))
        }
    }
}

@Composable
private fun SensorAuditCard(audit: SensorTruthBenchmark.SensorAudit) {
    val verdict = SensorTruthBenchmark.evaluate(audit)
    val verdictColor = when (verdict) {
        SensorTruthBenchmark.Verdict.GENUINE -> Color(0xFF43A047)
        SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING -> Color(0xFFE53935)
        SensorTruthBenchmark.Verdict.INCONCLUSIVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val verdictText = stringResource(
        when (verdict) {
            SensorTruthBenchmark.Verdict.GENUINE -> R.string.sensor_truth_verdict_genuine
            SensorTruthBenchmark.Verdict.SUSPECT_UNDERDELIVERING -> R.string.sensor_truth_verdict_suspect
            SensorTruthBenchmark.Verdict.INCONCLUSIVE -> R.string.sensor_truth_verdict_inconclusive
        },
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(sensorNameRes(audit.sensorType)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(text = verdictText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = verdictColor)
            }
            if (audit.advertisedHz != null) {
                Text(
                    text = stringResource(
                        R.string.sensor_truth_rate_detail,
                        String.format(Locale.getDefault(), "%.0f", audit.advertisedHz),
                        String.format(Locale.getDefault(), "%.0f", audit.actualHz),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.sensor_truth_no_advertised_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun sensorNameRes(sensorType: Int): Int = when (sensorType) {
    Sensor.TYPE_ACCELEROMETER -> R.string.sensor_test_name_accelerometer
    Sensor.TYPE_GYROSCOPE -> R.string.sensor_test_name_gyroscope
    else -> R.string.unknown
}
