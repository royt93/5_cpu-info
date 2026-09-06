package com.galaxyjoy.cpuinfo.feat.ramtruth

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
class FrmRamTruth : Fragment() {

    private val viewModel: VMRamTruth by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RamTruthScreen(
                    uiState = uiState,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResult,
                )
            }
        }
    }

    private fun shareResult(result: RamTruthBenchmark.Result) {
        val mb = NumberFormat.getIntegerInstance(Locale.getDefault())
        val testedMb = result.measurements.size * RamTruthBenchmark.CHUNK_SIZE_BYTES / (1024 * 1024)
        val text = when (RamTruthBenchmark.evaluate(result)) {
            RamTruthBenchmark.Verdict.GENUINE ->
                getString(R.string.ram_truth_share_text_genuine, mb.format(testedMb))
            RamTruthBenchmark.Verdict.SUSPECT_VIRTUAL_RAM -> {
                val cliffMb = (RamTruthBenchmark.cliffAtBytes(result) ?: 0L) / (1024 * 1024)
                getString(R.string.ram_truth_share_text_suspect, mb.format(cliffMb))
            }
            RamTruthBenchmark.Verdict.INCONCLUSIVE ->
                getString(R.string.ram_truth_verdict_inconclusive)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.ram_truth_share_button)))
    }
}
