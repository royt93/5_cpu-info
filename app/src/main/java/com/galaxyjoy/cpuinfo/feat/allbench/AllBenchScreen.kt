package com.galaxyjoy.cpuinfo.feat.allbench

import android.opengl.GLSurfaceView
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
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuSurfaceView
import java.util.Locale

@Composable
internal fun AllBenchScreen(
    uiState: VMAllBench.UiState,
    onCreateGpuRenderer: () -> GLSurfaceView.Renderer,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (VMAllBench.Results) -> Unit,
    onShareImageClicked: (VMAllBench.Results) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            VMAllBench.UiState.Idle -> IdleContent(onStartClicked)
            is VMAllBench.UiState.Running -> RunningContent(uiState.step, onCreateGpuRenderer, onStopClicked)
            is VMAllBench.UiState.Done -> DoneContent(uiState.results, onDoneClicked, onShareClicked, onShareImageClicked)
            is VMAllBench.UiState.Aborted -> AbortedContent(uiState.step, onDoneClicked)
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
        text = stringResource(R.string.all_bench_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.all_bench_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.all_bench_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(
    step: VMAllBench.Step,
    onCreateGpuRenderer: () -> GLSurfaceView.Renderer,
    onStopClicked: () -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.all_bench_running_label, stepIndex(step), stringResource(step.labelRes())),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(20.dp))
    if (step == VMAllBench.Step.GPU) {
        GpuSurfaceView(onCreateGpuRenderer, onStopClicked)
    } else {
        CircularProgressIndicator()
    }
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.all_bench_stop_button))
    }
}

@Composable
private fun AbortedContent(step: VMAllBench.Step, onDoneClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.all_bench_aborted, stringResource(step.labelRes())),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.all_bench_done_button))
    }
}

@Composable
private fun DoneContent(
    results: VMAllBench.Results,
    onDoneClicked: () -> Unit,
    onShareClicked: (VMAllBench.Results) -> Unit,
    onShareImageClicked: (VMAllBench.Results) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(20.dp))

    ResultRow(stringResource(R.string.all_bench_row_throttle), "${results.throttle.sustainedFreqMhz} MHz")
    ResultRow(stringResource(R.string.all_bench_row_storage), "${formatDecimal(results.storage.seqWriteMbPerSec)}/${formatDecimal(results.storage.seqReadMbPerSec)} MB/s")
    ResultRow(stringResource(R.string.all_bench_row_ram), "${formatDecimal(results.ram.writeMbPerSec)}/${formatDecimal(results.ram.readMbPerSec)} MB/s")
    ResultRow(stringResource(R.string.all_bench_row_gpu), "${formatDecimal(results.gpu.avgFps)} FPS")

    Spacer(Modifier.height(24.dp))
    // 3 buttons overflowed a single Row on real devices (the 3rd, "Share as image", pushed
    // "Done" to wrap its label across 2 lines) — split into 2 rows instead of shrinking text or
    // reaching for a wrapping layout just for 3 buttons.
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(results) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.all_bench_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { onShareImageClicked(results) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.bench_result_card_share_image_button), modifier = Modifier.padding(start = 8.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.all_bench_done_button))
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun stepIndex(step: VMAllBench.Step): Int = when (step) {
    VMAllBench.Step.THROTTLE -> 1
    VMAllBench.Step.STORAGE -> 2
    VMAllBench.Step.RAM -> 3
    VMAllBench.Step.GPU -> 4
}

private fun VMAllBench.Step.labelRes(): Int = when (this) {
    VMAllBench.Step.THROTTLE -> R.string.throttle
    VMAllBench.Step.STORAGE -> R.string.storage_bench
    VMAllBench.Step.RAM -> R.string.ram_bench
    VMAllBench.Step.GPU -> R.string.gpu_bench
}

/** Always renders with a "." decimal point, regardless of the device's default locale. */
private fun formatDecimal(value: Double): String = "%.1f".format(Locale.US, value)
