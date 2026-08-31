package com.galaxyjoy.cpuinfo.feat.canmydevice

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * U05 "Can My Device?" — answers concrete user questions ("Netflix HD?", "quay RAW?", "game
 * Vulkan?") from capability data other tabs already collect (DRM, camera, Vulkan, AI Readiness).
 * MVP: fixed rule set, no remote rule-pack, no VIP gating.
 */
@AndroidEntryPoint
class CanMyDeviceBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var canMyDeviceProvider: CanMyDeviceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val result = canMyDeviceProvider.evaluate()
        setContent {
            CpuInfoTheme {
                CanMyDeviceContent(result)
            }
        }
    }

    companion object {
        const val TAG = "CanMyDeviceBottomSheet"
    }
}

@Composable
private fun CanMyDeviceContent(result: CanMyDeviceEvaluator.Result) {
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
                text = stringResource(R.string.can_my_device_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            result.rules.forEach { rule ->
                RuleCard(rule)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.can_my_device_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RuleCard(rule: CanMyDeviceEvaluator.Rule) {
    val (accentColor, containerColor) = when (rule.verdict) {
        CanMyDeviceEvaluator.Verdict.YES -> Color(0xFF4CAF50) to Color(0xFF4CAF50).copy(alpha = 0.08f)
        CanMyDeviceEvaluator.Verdict.NO ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        CanMyDeviceEvaluator.Verdict.PARTIAL -> Color(0xFFFFA726) to Color(0xFFFFA726).copy(alpha = 0.08f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(questionRes(rule.id)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(0.dp))
                Box(
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(verdictLabelRes(rule.verdict)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = rule.reasonDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun questionRes(id: CanMyDeviceEvaluator.RuleId): Int = when (id) {
    CanMyDeviceEvaluator.RuleId.NETFLIX_HD -> R.string.can_my_device_q_netflix_hd
    CanMyDeviceEvaluator.RuleId.HDCP_4K -> R.string.can_my_device_q_hdcp_4k
    CanMyDeviceEvaluator.RuleId.CAMERA_RAW -> R.string.can_my_device_q_camera_raw
    CanMyDeviceEvaluator.RuleId.SLOW_MOTION -> R.string.can_my_device_q_slow_motion
    CanMyDeviceEvaluator.RuleId.VULKAN_GAMING -> R.string.can_my_device_q_vulkan_gaming
    CanMyDeviceEvaluator.RuleId.ON_DEVICE_AI -> R.string.can_my_device_q_on_device_ai
}

private fun verdictLabelRes(verdict: CanMyDeviceEvaluator.Verdict): Int = when (verdict) {
    CanMyDeviceEvaluator.Verdict.YES -> R.string.can_my_device_verdict_yes
    CanMyDeviceEvaluator.Verdict.NO -> R.string.can_my_device_verdict_no
    CanMyDeviceEvaluator.Verdict.PARTIAL -> R.string.can_my_device_verdict_partial
}
