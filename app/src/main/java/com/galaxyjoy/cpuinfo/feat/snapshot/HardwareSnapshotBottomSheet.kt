package com.galaxyjoy.cpuinfo.feat.snapshot

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * U03 "Hardware Diff/Snapshot" — captures the current hardware state and compares it against the
 * single saved baseline (if any). IDENTITY fields changing is the interesting signal (board swap,
 * spoofed props); DRIFT fields (free storage, available RAM, patch level) are expected to move
 * and are shown for context only.
 */
@AndroidEntryPoint
class HardwareSnapshotBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var hardwareSnapshotProvider: HardwareSnapshotProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val current = hardwareSnapshotProvider.captureSnapshot()

        setContent {
            CpuInfoTheme {
                HardwareSnapshotContent(current = current, provider = hardwareSnapshotProvider)
            }
        }
    }

    companion object {
        const val TAG = "HardwareSnapshotBottomSheet"
    }
}

@Composable
private fun HardwareSnapshotContent(current: HardwareSnapshot, provider: HardwareSnapshotProvider) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf<HardwareSnapshot?>(null) }

    LaunchedEffect(Unit) {
        saved = provider.loadSavedSnapshot()
        isLoading = false
    }

    fun saveBaseline() {
        scope.launch {
            provider.saveSnapshot(current)
            saved = current
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.hardware_snapshot_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            val baseline = saved
            when {
                isLoading -> Unit
                baseline == null -> EmptyBaselineState(onSaveClicked = ::saveBaseline)
                else -> BaselineDiffState(baseline = baseline, current = current, onSaveClicked = ::saveBaseline)
            }
        }
    }
}

@Composable
private fun EmptyBaselineState(onSaveClicked: () -> Unit) {
    Text(
        text = stringResource(R.string.hardware_snapshot_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.hardware_snapshot_save_button))
    }
}

@Composable
private fun BaselineDiffState(
    baseline: HardwareSnapshot,
    current: HardwareSnapshot,
    onSaveClicked: () -> Unit,
) {
    val diff = remember(baseline, current) { HardwareSnapshotEvaluator.diff(baseline, current) }
    val statusColor = if (diff.hasIdentityChange) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

    Text(
        text = stringResource(
            if (diff.hasIdentityChange) {
                R.string.hardware_snapshot_identity_changed
            } else {
                R.string.hardware_snapshot_identity_stable
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = statusColor,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(
            R.string.hardware_snapshot_last_saved,
            DateUtils.getRelativeTimeSpanString(
                baseline.timestampMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))

    diff.rows.forEach { row ->
        DiffRowCard(row)
        Spacer(Modifier.height(10.dp))
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.hardware_snapshot_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.hardware_snapshot_save_new_button))
    }
}

@Composable
private fun DiffRowCard(row: HardwareSnapshotEvaluator.DiffRow) {
    val (accentColor, containerColor) = when {
        !row.changed -> Color(0xFF4CAF50) to Color(0xFF4CAF50).copy(alpha = 0.08f)
        row.kind == HardwareSnapshotEvaluator.FieldKind.IDENTITY ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        else -> Color(0xFF1E88E5) to Color(0xFF1E88E5).copy(alpha = 0.08f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.newValue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
            if (row.changed) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = row.oldValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
