package com.galaxyjoy.cpuinfo.feat.gpubench

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusMapper
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import com.galaxyjoy.cpuinfo.ui.component.BenchTrendChart
import com.galaxyjoy.cpuinfo.util.BenchPercentileCalculator
import com.galaxyjoy.cpuinfo.util.OsUpdateImpactCalculator
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GpuBenchScreen(
    uiState: VMGpuBench.UiState,
    thermalSnapshot: ThermalStatusProvider.Snapshot,
    onCreateRenderer: () -> GLSurfaceView.Renderer,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (GpuBenchmark.Result) -> Unit,
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
            is VMGpuBench.UiState.Idle -> IdleContent(uiState.previous, onStartClicked)
            is VMGpuBench.UiState.Running ->
                RunningContent(uiState.warmingUp, onCreateRenderer, onStopClicked)
            is VMGpuBench.UiState.Done -> DoneContent(uiState, onDoneClicked, onShareClicked)
            is VMGpuBench.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
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
private fun IdleContent(previous: GpuBenchResultPrefs.SavedResult?, onStartClicked: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 24.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.gpu_bench_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.gpu_bench_disclaimer_body),
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
                    R.string.gpu_bench_previous_summary,
                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(previous.timestampMs)),
                    formatFps(previous.avgFps),
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
        Text(text = stringResource(R.string.gpu_bench_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Only mounted while [VMGpuBench.UiState.Running] — the `GLSurfaceView` is created here and torn
 * down (`onRelease`) once the state moves on, so the GL context/render thread only exist while a
 * benchmark is actually in flight. [Lifecycle.Event.ON_PAUSE] (app backgrounded mid-run) forwards
 * to both `glSurfaceView.onPause()` (required — `GLSurfaceView` leaks its render thread otherwise)
 * and [onStopClicked] (aborts the run — see [GpuBenchmarkRunner.requestStop] for why a paused run
 * can't just resume where it left off).
 */
@Composable
private fun RunningContent(
    warmingUp: Boolean,
    onCreateRenderer: () -> GLSurfaceView.Renderer,
    onStopClicked: () -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(
            if (warmingUp) R.string.gpu_bench_running_warmup else R.string.gpu_bench_running_measuring,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(16.dp))
    GpuSurfaceView(onCreateRenderer, onStopClicked)
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.gpu_bench_stop_button))
    }
}

/**
 * The actual `GLSurfaceView` + lifecycle plumbing, shared with U17's `feat.allbench.AllBenchScreen`
 * (that screen runs this same GPU workload as its 4th step) — extracted here rather than
 * duplicated since both call sites need the identical pause/resume/interrupt behavior.
 */
@Composable
internal fun GpuSurfaceView(onCreateRenderer: () -> GLSurfaceView.Renderer, onInterrupted: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val glViewRef = remember { mutableStateOf<GLSurfaceView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    glViewRef.value?.onPause()
                    onInterrupted()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(onCreateRenderer())
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                glViewRef.value = this
            }
        },
        modifier = Modifier.size(240.dp),
        onRelease = { it.onPause() },
    )
}

@Composable
private fun AbortedContent(reason: GpuBenchmark.AbortReason, onDoneClicked: () -> Unit) {
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
                GpuBenchmark.AbortReason.OVERHEAT -> R.string.gpu_bench_aborted_overheat
                GpuBenchmark.AbortReason.INTERRUPTED -> R.string.gpu_bench_aborted_interrupted
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.gpu_bench_done_button))
    }
}

@Composable
private fun DoneContent(
    state: VMGpuBench.UiState.Done,
    onDoneClicked: () -> Unit,
    onShareClicked: (GpuBenchmark.Result) -> Unit,
) {
    val result = state.result
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(20.dp))

    ResultRow(stringResource(R.string.gpu_bench_fps_label), formatFps(result.avgFps))

    Spacer(Modifier.height(16.dp))
    if (state.previous != null) {
        Text(
            text = stringResource(
                R.string.gpu_bench_previous_summary,
                DateFormat.getDateInstance(DateFormat.SHORT).format(Date(state.previous.timestampMs)),
                formatFps(state.previous.avgFps),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(R.string.gpu_bench_no_previous),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(16.dp))
    BenchTrendChart(values = state.history.map { it.avgFps })

    BenchPercentileCalculator.percentileOfLast(state.history.map { it.avgFps })?.let { percentile ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.bench_percentile_message, percentile),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    OsUpdateImpactCalculator.detectImpact(state.history.map { it.osBuildFingerprint to it.avgFps })?.let { impact ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.bench_os_update_impact_message, "%+d".format(Locale.US, impact.percentChange)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.gpu_bench_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.gpu_bench_done_button))
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

private fun formatFps(value: Double): String = "%.1f FPS".format(Locale.US, value)
