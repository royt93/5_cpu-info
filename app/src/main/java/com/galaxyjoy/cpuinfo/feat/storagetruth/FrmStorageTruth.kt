package com.galaxyjoy.cpuinfo.feat.storagetruth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class FrmStorageTruth : Fragment() {

    private val viewModel: VMStorageTruth by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                StorageTruthScreen(
                    uiState = uiState,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResult,
                )
            }
        }
    }

    private fun shareResult(result: StorageTruthBenchmark.Result) {
        val mb = NumberFormat.getIntegerInstance(Locale.getDefault())
        val testedMb = result.blocksTested * StorageTruthBenchmark.BLOCK_SIZE_BYTES / (1024 * 1024)
        val text = when (StorageTruthBenchmark.evaluate(result)) {
            StorageTruthBenchmark.Verdict.GENUINE ->
                getString(R.string.storage_truth_share_text_genuine, mb.format(testedMb))
            StorageTruthBenchmark.Verdict.SUSPECT_FAKE -> {
                val firstMismatchMb = result.mismatches.minByOrNull { it.blockIndex }
                    ?.let { it.expectedOffsetBytes / (1024 * 1024) } ?: 0L
                getString(R.string.storage_truth_share_text_suspect, mb.format(firstMismatchMb))
            }
            StorageTruthBenchmark.Verdict.INCONCLUSIVE ->
                getString(R.string.storage_truth_verdict_inconclusive)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.storage_truth_share_button)))
    }
}
