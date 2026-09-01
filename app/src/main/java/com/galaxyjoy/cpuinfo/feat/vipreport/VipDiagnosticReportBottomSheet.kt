package com.galaxyjoy.cpuinfo.feat.vipreport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

/**
 * U07 "VIP diagnostic report lịch sử" — history of manually-saved [VipDiagnosticSnapshot]s so a
 * VIP user can eyeball battery/RAM/storage drift over months. Entry point (`FrmSettings`) already
 * gates this behind [com.roy.sdkadbmob.AdManager.isVipByKeyActive] before showing the sheet.
 */
@AndroidEntryPoint
class VipDiagnosticReportBottomSheet : BaseRoundedBottomSheet() {

    @Inject
    lateinit var repository: VipDiagnosticReportRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        setContent {
            CpuInfoTheme {
                VipDiagnosticContent(repository = repository)
            }
        }
    }

    companion object {
        const val TAG = "VipDiagnosticReportBottomSheet"
    }
}

/** Internal (not private) so [VipDiagnosticReportRepositoryInstrumentedTest]-style widget tests can render it directly with a throwaway-DataStore-backed repository, without touching the real app's persisted history. */
@Composable
internal fun VipDiagnosticContent(repository: VipDiagnosticReportRepository) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var history by remember { mutableStateOf<List<VipDiagnosticSnapshot>>(emptyList()) }

    LaunchedEffect(Unit) {
        history = repository.loadHistory()
        isLoading = false
    }

    fun saveToday() {
        scope.launch {
            repository.saveSnapshot(repository.captureSnapshot())
            history = repository.loadHistory()
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
                text = stringResource(R.string.vip_diagnostic_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))

            when {
                isLoading -> Unit
                history.isEmpty() -> EmptyHistoryState(onSaveClicked = ::saveToday)
                else -> HistoryState(history = history, onSaveClicked = ::saveToday)
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(onSaveClicked: () -> Unit) {
    Text(
        text = stringResource(R.string.vip_diagnostic_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.vip_diagnostic_save_button))
    }
}

@Composable
private fun HistoryState(history: List<VipDiagnosticSnapshot>, onSaveClicked: () -> Unit) {
    val summary = remember(history) { VipDiagnosticEvaluator.summarize(history) }

    if (summary != null) {
        Text(
            text = stringResource(R.string.vip_diagnostic_summary_days_tracked, summary.daysTracked),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        summary.cycleCountDelta?.let { delta ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.vip_diagnostic_summary_cycle_delta, delta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    // Newest first, matching how a "history" list reads naturally.
    history.asReversed().forEach { snapshot ->
        SnapshotRowCard(snapshot)
        Spacer(Modifier.height(10.dp))
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.vip_diagnostic_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = onSaveClicked, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.vip_diagnostic_save_button))
    }
}

@Composable
private fun SnapshotRowCard(snapshot: VipDiagnosticSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(snapshot.timestampMillis)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            DiagnosticField(stringResource(R.string.vip_diagnostic_row_battery_level), "${snapshot.batteryLevelPercent}%")
            if (snapshot.chargeCounterMah >= 0) {
                DiagnosticField(
                    stringResource(R.string.battery_charge_counter),
                    stringResource(R.string.battery_mah_value, "%.0f".format(snapshot.chargeCounterMah)),
                )
            }
            if (snapshot.cycleCount >= 0) {
                DiagnosticField(stringResource(R.string.battery_cycle_count), snapshot.cycleCount.toString())
            }
            DiagnosticField(
                stringResource(R.string.vip_diagnostic_row_ram_available),
                "${snapshot.ramAvailablePercentage}%",
            )
            val storageFreeGb = snapshot.internalStorageFreeBytes / (1024.0 * 1024.0 * 1024.0)
            DiagnosticField(stringResource(R.string.vip_diagnostic_row_storage_free), "%.1f GB".format(storageFreeGb))
        }
    }
}

@Composable
private fun DiagnosticField(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
