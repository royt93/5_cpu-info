package com.galaxyjoy.cpuinfo.feat.siliconlottery

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
import java.text.NumberFormat
import java.util.Locale

/**
 * E04 "Silicon Lottery" — same one-shot "measure right now, no history" shape as
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchScreen], one row per logical core instead
 * of per cluster, highlighting the strongest/weakest core and the spread between them.
 */
@Composable
internal fun SiliconLotteryScreen(
    uiState: VMSiliconLottery.UiState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onShareClicked: (SiliconLotteryBenchmark.Result) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            is VMSiliconLottery.UiState.Idle -> IdleContent(onStartClicked)
            is VMSiliconLottery.UiState.Running -> RunningContent(uiState, onStopClicked)
            is VMSiliconLottery.UiState.Done -> DoneContent(uiState.result, onDoneClicked, onShareClicked)
            is VMSiliconLottery.UiState.Aborted -> AbortedContent(uiState.reason, onDoneClicked)
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
        text = stringResource(R.string.silicon_lottery_disclaimer_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.silicon_lottery_disclaimer_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(text = stringResource(R.string.silicon_lottery_start_button), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningContent(state: VMSiliconLottery.UiState.Running, onStopClicked: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    CircularProgressIndicator()
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.silicon_lottery_running_label, state.coreIndex + 1, state.coreCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onStopClicked) {
        Text(text = stringResource(R.string.silicon_lottery_stop_button))
    }
}

@Composable
private fun AbortedContent(reason: SiliconLotteryBenchmark.AbortReason, onDoneClicked: () -> Unit) {
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
                SiliconLotteryBenchmark.AbortReason.OVERHEAT -> R.string.silicon_lottery_aborted_overheat
                SiliconLotteryBenchmark.AbortReason.INTERRUPTED -> R.string.silicon_lottery_aborted_interrupted
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onDoneClicked) {
        Text(text = stringResource(R.string.silicon_lottery_done_button))
    }
}

@Composable
private fun DoneContent(
    result: SiliconLotteryBenchmark.Result,
    onDoneClicked: () -> Unit,
    onShareClicked: (SiliconLotteryBenchmark.Result) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))

    val strongest = SiliconLotteryBenchmark.strongest(result)
    val weakest = SiliconLotteryBenchmark.weakest(result)
    val spreadPercent = SiliconLotteryBenchmark.spreadPercent(result)
    if (result.cores.size >= 2) {
        Text(
            text = stringResource(R.string.silicon_lottery_spread_label, spreadPercent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }

    val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
    result.cores.sortedBy { it.coreIndex }.forEach { core ->
        val badge = when (core.coreIndex) {
            strongest?.coreIndex -> stringResource(R.string.silicon_lottery_strongest_label)
            weakest?.coreIndex -> stringResource(R.string.silicon_lottery_weakest_label)
            else -> null
        }
        ResultRow(
            label = stringResource(R.string.silicon_lottery_row_label, core.coreIndex) + (badge?.let { " $it" } ?: ""),
            value = stringResource(R.string.silicon_lottery_ops_value, ops.format(core.opsPerSecond)),
            color = when (core.coreIndex) {
                strongest?.coreIndex -> Color(0xFF43A047)
                weakest?.coreIndex -> Color(0xFFE53935)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (!core.affinityConfirmed) {
            Text(
                text = stringResource(R.string.silicon_lottery_affinity_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { onShareClicked(result) }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
            Text(text = stringResource(R.string.silicon_lottery_share_button), modifier = Modifier.padding(start = 8.dp))
        }
        Button(onClick = onDoneClicked) {
            Text(text = stringResource(R.string.silicon_lottery_done_button))
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
