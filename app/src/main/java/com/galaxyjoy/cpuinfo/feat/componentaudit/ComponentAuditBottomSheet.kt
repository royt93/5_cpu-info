package com.galaxyjoy.cpuinfo.feat.componentaudit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComponentAuditBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var auditProvider: ExportedComponentAuditProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val result = auditProvider.scan()
        setContent {
            CpuInfoTheme { ComponentAuditContent(result = result) }
        }
    }

    companion object {
        const val TAG = "ComponentAuditBottomSheet"
    }
}

@Composable
internal fun ComponentAuditContent(result: ComponentAuditResult) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.component_audit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.component_audit_scope_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(
                    R.string.component_audit_summary,
                    result.findings.size,
                    result.scannedPackageCount,
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            if (result.findings.isEmpty()) {
                Text(
                    text = stringResource(R.string.component_audit_no_findings),
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                val typeProviderLabel = stringResource(R.string.component_audit_type_provider)
                val typeReceiverLabel = stringResource(R.string.component_audit_type_receiver)
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    result.findings.forEach { finding ->
                        val typeLabel = when (finding.type) {
                            UnguardedComponent.Type.PROVIDER -> typeProviderLabel
                            UnguardedComponent.Type.RECEIVER -> typeReceiverLabel
                        }
                        Text(
                            text = stringResource(
                                R.string.component_audit_row,
                                finding.appLabel,
                                finding.componentName,
                                typeLabel,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
