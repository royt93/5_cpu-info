package com.galaxyjoy.cpuinfo.feat.throttle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ThrottleScreen(
    uiState: VMThrottle.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (ThrottleFingerprint.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMThrottle.UiState.Idle -> IdleContent(uiState.previous, onStartClicked)
            is VMThrottle.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMThrottle.UiState.Done -> DoneContent(uiState, onDoneClicked, onShareClicked)
        }
    }
}

@Composable
private fun IdleContent(previous: ThrottleResultPrefs.SavedResult?, onStartClicked: () -> Unit) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.throttle_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.throttle_disclaimer_body),
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
                    R.string.throttle_previous_summary,
                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(previous.timestampMs)),
                    previous.throttlePercent,
                    previous.peakFreqMhz,
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
        Text(text = stringResource(R.string.throttle_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMThrottle.UiState.Running, onStopClicked: () -> Unit) {
    val progress = (state.elapsedMs.toFloat() / ThrottleFingerprint.TEST_DURATION_MS).coerceIn(0f, 1f)
    val remainingSec = ((ThrottleFingerprint.TEST_DURATION_MS - state.elapsedMs) / 1000L).coerceAtLeast(0L)

    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.throttle_running_label, remainingSec),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(20.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        LiveStat(stringResource(R.string.throttle_current_freq_label), "${state.currentFreqMhz} MHz")
        LiveStat(
            stringResource(R.string.throttle_current_temp_label),
            "${state.currentTempC}°C",
            valueColor = tempColor(state.currentTempC),
        )
    }
    Spacer(Modifier.height(20.dp))

    FreqSparkline(state.samples, modifier = Modifier.fillMaxWidth().height(100.dp))
    Spacer(Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.throttle_safety_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.throttle_stop_button))
    }
}

@Composable
private fun LiveStat(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FreqSparkline(samples: List<ThrottleFingerprint.Sample>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(color = trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            if (samples.size < 2) return@Canvas

            val maxFreq = samples.maxOf { it.avgFreqMhz }.coerceAtLeast(1L)
            val minFreq = samples.minOf { it.avgFreqMhz }
            val range = (maxFreq - minFreq).coerceAtLeast(1L)
            val stepX = size.width / (samples.size - 1)

            val path = Path()
            samples.forEachIndexed { index, sample ->
                val x = index * stepX
                val normalized = (sample.avgFreqMhz - minFreq).toFloat() / range
                val y = size.height - (normalized * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
        }
    }
}

@Composable
private fun DoneContent(
    state: VMThrottle.UiState.Done,
    onDoneClicked: () -> Unit,
    onShareClicked: (ThrottleFingerprint.Result) -> Unit,
) {
    val result = state.result
    Spacer(Modifier.height(16.dp))

    if (result.aborted && result.abortReason == ThrottleFingerprint.AbortReason.OVERHEAT) {
        AbortBanner(stringResource(R.string.throttle_aborted_overheat))
        Spacer(Modifier.height(16.dp))
    } else if (result.aborted && result.abortReason == ThrottleFingerprint.AbortReason.USER_STOPPED) {
        AbortBanner(stringResource(R.string.throttle_aborted_user))
        Spacer(Modifier.height(16.dp))
    }

    Icon(
        imageVector = if (result.throttled) Icons.Default.Warning else Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (result.throttled) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
        modifier = Modifier.padding(top = 8.dp),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(
            if (result.throttled) R.string.throttle_verdict_throttled else R.string.throttle_verdict_stable,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(20.dp))

    ResultRow(stringResource(R.string.throttle_peak_freq_label), "${result.peakFreqMhz} MHz")
    ResultRow(stringResource(R.string.throttle_sustained_freq_label), "${result.sustainedFreqMhz} MHz")
    ResultRow(stringResource(R.string.throttle_throttle_percent_label), "${result.throttlePercent}%")
    ResultRow(stringResource(R.string.throttle_max_temp_label), "${result.maxTempC}°C")

    Spacer(Modifier.height(16.dp))
    if (state.previous != null) {
        val deltaPercent = result.throttlePercent - state.previous.throttlePercent
        Text(
            text = stringResource(R.string.throttle_compare_previous, deltaPercent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(R.string.throttle_no_previous),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.throttle_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.throttle_done_button))
        }
    }
}

@Composable
private fun AbortBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center,
        )
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

@Composable
private fun tempColor(tempC: Int): Color = when {
    tempC >= ThrottleFingerprint.SAFETY_ABORT_TEMP_C - 3 -> MaterialTheme.colorScheme.error
    tempC >= ThrottleFingerprint.SAFETY_ABORT_TEMP_C - 8 -> Color(0xFFFFA726)
    else -> MaterialTheme.colorScheme.onSurface
}
