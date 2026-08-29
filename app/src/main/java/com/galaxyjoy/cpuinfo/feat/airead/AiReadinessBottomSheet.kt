package com.galaxyjoy.cpuinfo.feat.airead

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import javax.inject.Inject

/**
 * F10/U12 "AI Readiness Score" — combines detected ISA extensions (already parsed by libcpuinfo,
 * no new register reads) with RAM/core count into a simple heuristic score for "how capable is
 * this device of running on-device ML models". Not a benchmark — shown with a disclaimer.
 */
@AndroidEntryPoint
class AiReadinessBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var aiReadinessProvider: AiReadinessProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val result = aiReadinessProvider.evaluate()
        setContent {
            CpuInfoTheme {
                AiReadinessContent(result)
            }
        }
    }

    companion object {
        const val TAG = "AiReadinessBottomSheet"
    }
}

@Composable
private fun AiReadinessContent(result: AiReadinessEvaluator.Result) {
    val tierColor = tierColor(result.tier)
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
                text = stringResource(R.string.ai_readiness_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "${result.score}/${result.maxScore}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = tierColor,
            )
            Text(
                text = tierLabel(result.tier),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tierColor,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            IsaFlagRow(stringResource(R.string.ai_readiness_flag_neon_dot), result.flags.neonDot)
            IsaFlagRow(stringResource(R.string.ai_readiness_flag_i8mm), result.flags.i8mm)
            IsaFlagRow(stringResource(R.string.ai_readiness_flag_bf16), result.flags.bf16)
            IsaFlagRow(stringResource(R.string.ai_readiness_flag_fp16), result.flags.fp16Arith)
            IsaFlagRow(stringResource(R.string.ai_readiness_flag_sve), result.flags.sve || result.flags.sve2)

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ai_readiness_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun IsaFlagRow(label: String, supported: Boolean) {
    val color = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun tierLabel(tier: AiReadinessEvaluator.Tier): String = when (tier) {
    AiReadinessEvaluator.Tier.NOT_READY -> stringResource(R.string.ai_readiness_tier_not_ready)
    AiReadinessEvaluator.Tier.BASIC -> stringResource(R.string.ai_readiness_tier_basic)
    AiReadinessEvaluator.Tier.CAPABLE -> stringResource(R.string.ai_readiness_tier_capable)
    AiReadinessEvaluator.Tier.ADVANCED -> stringResource(R.string.ai_readiness_tier_advanced)
}

internal fun tierColor(tier: AiReadinessEvaluator.Tier): Color = when (tier) {
    AiReadinessEvaluator.Tier.NOT_READY -> Color(0xFF9E9E9E)
    AiReadinessEvaluator.Tier.BASIC -> Color(0xFFFFA726)
    AiReadinessEvaluator.Tier.CAPABLE -> Color(0xFF1E88E5)
    AiReadinessEvaluator.Tier.ADVANCED -> Color(0xFF8E24AA)
}
