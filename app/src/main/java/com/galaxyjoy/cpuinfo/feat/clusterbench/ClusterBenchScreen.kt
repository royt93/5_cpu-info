package com.galaxyjoy.cpuinfo.feat.clusterbench

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import java.text.NumberFormat
import java.util.Locale

/**
 * U31 — unlike [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchScreen]/etc, this has no "previous
 * run" card, trend chart, percentile, or OS-update-impact line: [ClusterBenchmark] deliberately
 * has no persisted history (see its doc comment) — this is a one-shot "compare your device's
 * core tiers right now" tool, not a trend-tracked benchmark. Adding history/trend for a 5th
 * benchmark type is left as a deliberate follow-up, not part of this scope.
 */
@Composable
internal fun ClusterBenchScreen(
    uiState: VMClusterBench.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (ClusterBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMClusterBench.UiState.Idle -> IdleContent(onStartClicked)
            is VMClusterBench.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMClusterBench.UiState.Done -> DoneContent(uiState.result, onDoneClicked, onShareClicked)
            is VMClusterBench.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
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
        text = stringResource(R.string.cluster_bench_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.cluster_bench_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.cluster_bench_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMClusterBench.UiState.Running, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(
            R.string.cluster_bench_running_label,
            state.clusterIndex + 1,
            state.clusterCount,
            stringResource(tierLabelRes(state.tier)),
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.cluster_bench_stop_button))
    }
}

@Composable
private fun AbortedContent(reason: ClusterBenchmark.AbortReason, onDoneClicked: () -> Unit) {
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
                ClusterBenchmark.AbortReason.OVERHEAT -> R.string.cluster_bench_aborted_overheat
                ClusterBenchmark.AbortReason.INTERRUPTED -> R.string.cluster_bench_aborted_interrupted
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.cluster_bench_done_button))
    }
}

@Composable
private fun DoneContent(
    result: ClusterBenchmark.Result,
    onDoneClicked: () -> Unit,
    onShareClicked: (ClusterBenchmark.Result) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(20.dp))

    val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
    result.clusters.sortedBy { it.tier.ordinal }.forEach { cluster ->
        ResultRow(
            label = stringResource(R.string.cluster_bench_row_label, stringResource(tierLabelRes(cluster.tier)), cluster.coreCount),
            value = stringResource(R.string.cluster_bench_ops_value, ops.format(cluster.opsPerSecond)),
            color = tierColor(cluster.tier),
        )
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.cluster_bench_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.cluster_bench_done_button))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: Color) {
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
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Same string resources/colors as
 * [com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyScreen]'s private `tierLabel`/`tierColor` —
 * duplicated rather than shared since those are `private` there and this is a 5-line mapping, not
 * worth widening that file's visibility for. */
internal fun tierLabelRes(tier: ClusterTopologyBuilder.Tier): Int = when (tier) {
    ClusterTopologyBuilder.Tier.PRIME -> R.string.cluster_tier_prime
    ClusterTopologyBuilder.Tier.PERFORMANCE -> R.string.cluster_tier_performance
    ClusterTopologyBuilder.Tier.EFFICIENCY -> R.string.cluster_tier_efficiency
    ClusterTopologyBuilder.Tier.ALL_CORES -> R.string.cluster_tier_all_cores
    ClusterTopologyBuilder.Tier.UNLABELED -> R.string.cluster_tier_unlabeled
}

private fun tierColor(tier: ClusterTopologyBuilder.Tier): Color = when (tier) {
    ClusterTopologyBuilder.Tier.PRIME -> Color(0xFFE53935)
    ClusterTopologyBuilder.Tier.PERFORMANCE -> Color(0xFF1E88E5)
    ClusterTopologyBuilder.Tier.EFFICIENCY -> Color(0xFF43A047)
    ClusterTopologyBuilder.Tier.ALL_CORES -> Color(0xFF546E7A)
    ClusterTopologyBuilder.Tier.UNLABELED -> Color(0xFF546E7A)
}
