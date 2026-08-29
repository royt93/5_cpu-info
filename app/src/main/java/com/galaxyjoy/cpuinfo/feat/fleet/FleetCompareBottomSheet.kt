package com.galaxyjoy.cpuinfo.feat.fleet

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import javax.inject.Inject

/**
 * U04 "Privacy-preserving Fleet Compare" — see [FleetSpecCatalog] for why this is an offline
 * reference-table check rather than a live comparison against other users' devices.
 */
@AndroidEntryPoint
class FleetCompareBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var fleetCompareProvider: FleetCompareProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val result = fleetCompareProvider.evaluate()

        setContent {
            CpuInfoTheme {
                FleetCompareContent(result = result)
            }
        }
    }

    companion object {
        const val TAG = "FleetCompareBottomSheet"
    }
}

@Composable
private fun FleetCompareContent(result: FleetCompareEvaluator.Result) {
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
                text = stringResource(R.string.fleet_compare_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            val entry = result.matchedEntry
            if (entry == null) {
                NoMatchContent()
            } else {
                VerdictBadge(hasMismatch = result.hasMismatch)
                Spacer(Modifier.height(20.dp))

                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))

                SpecRow(
                    label = stringResource(R.string.fleet_compare_ram_label),
                    actual = stringResource(R.string.fleet_compare_gb_value, result.actualRamGb),
                    expected = stringResource(R.string.fleet_compare_min_gb_value, entry.minRamGb),
                    mismatch = result.ramMismatch,
                )
                SpecRow(
                    label = stringResource(R.string.fleet_compare_storage_label),
                    actual = stringResource(R.string.fleet_compare_gb_value, result.actualStorageGb),
                    expected = stringResource(R.string.fleet_compare_min_gb_value, entry.minStorageGb),
                    mismatch = result.storageMismatch,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.fleet_compare_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoMatchContent() {
    Text(
        text = stringResource(R.string.fleet_compare_no_match, Build.MODEL ?: ""),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.fleet_compare_supported_models),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

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
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.05f))),
                shape = CircleShape,
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
            if (hasMismatch) R.string.fleet_compare_verdict_mismatch else R.string.fleet_compare_verdict_ok,
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SpecRow(label: String, actual: String, expected: String, mismatch: Boolean) {
    val accentColor = if (mismatch) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = actual,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = expected,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
