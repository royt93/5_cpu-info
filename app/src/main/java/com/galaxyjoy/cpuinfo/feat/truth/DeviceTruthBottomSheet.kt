package com.galaxyjoy.cpuinfo.feat.truth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * U01 "Device Truth Score" — cross-references real hardware detected via native `cpuinfo` (MIDR,
 * and where available MPIDR/REVIDR read directly from silicon) against what the device claims
 * about itself. Shows concrete per-field evidence, not a single opaque score — a device with
 * spoofed specs or a wrong-model refurb should be visible in the specific row that disagrees.
 */
@AndroidEntryPoint
class DeviceTruthBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var deviceTruthProvider: DeviceTruthProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val result = DeviceTruthEvaluator.evaluate(deviceTruthProvider.snapshot())

        setContent {
            CpuInfoTheme {
                DeviceTruthContent(result = result, onShareClicked = { shareResult(result) })
            }
        }
    }

    private fun shareResult(result: DeviceTruthEvaluator.Result) {
        val text = buildString {
            appendLine(getString(R.string.device_truth_title))
            result.rows.forEach { row ->
                appendLine("${row.label}: ${row.detected}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.device_truth_share_button)))
    }

    companion object {
        const val TAG = "DeviceTruthBottomSheet"
    }
}

@Composable
private fun DeviceTruthContent(
    result: DeviceTruthEvaluator.Result,
    onShareClicked: () -> Unit,
) {
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
                text = stringResource(R.string.device_truth_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            VerdictBadge(hasMismatch = result.hasMismatch)
            Spacer(Modifier.height(24.dp))

            result.rows.forEachIndexed { index, row ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(durationMillis = 250, delayMillis = index * 60)) +
                        slideInVertically(
                            animationSpec = tween(durationMillis = 250, delayMillis = index * 60),
                            initialOffsetY = { it / 3 },
                        ),
                ) {
                    EvidenceCard(row)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.device_truth_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Button(onClick = onShareClicked, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.height(18.dp))
                Text(text = stringResource(R.string.device_truth_share_button), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * Circular badge that scales+fades in with the check/warning icon — same "reveal" feel as
 * Shield Score's ring gauge, but simpler since there's no numeric progress to animate here.
 */
@Composable
private fun VerdictBadge(hasMismatch: Boolean) {
    val color = if (hasMismatch) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(hasMismatch) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 350))
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.05f))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (hasMismatch) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((40 * scale.value).dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(
            if (hasMismatch) R.string.device_truth_verdict_mismatch else R.string.device_truth_verdict_ok,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun EvidenceCard(row: DeviceTruthEvaluator.Row) {
    val (accentColor, containerColor) = when (row.verdict) {
        DeviceTruthEvaluator.Verdict.MATCH -> Color(0xFF4CAF50) to Color(0xFF4CAF50).copy(alpha = 0.08f)
        DeviceTruthEvaluator.Verdict.MISMATCH ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        DeviceTruthEvaluator.Verdict.INFO ->
            MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
    val isRegisterRow = row.label.endsWith("_EL1")

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
                    text = row.detected,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isRegisterRow) FontFamily.Monospace else FontFamily.Default,
                    color = accentColor,
                )
            }
            if (row.declared != "-") {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = row.declared,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
