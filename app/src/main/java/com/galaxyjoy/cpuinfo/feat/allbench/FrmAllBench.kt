package com.galaxyjoy.cpuinfo.feat.allbench

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
import androidx.lifecycle.lifecycleScope
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.benchresultcard.BenchResultCardExporter
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FrmAllBench : Fragment() {

    private val viewModel: VMAllBench by viewModels()

    @Inject
    lateinit var benchResultCardExporter: BenchResultCardExporter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AllBenchScreen(
                    uiState = uiState,
                    onCreateGpuRenderer = viewModel::newGpuRenderer,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResults,
                    onShareImageClicked = { benchResultCardExporter.exportBenchResultCard(lifecycleScope, it) },
                )
            }
        }
    }

    private fun shareResults(results: VMAllBench.Results) {
        val text = getString(
            R.string.all_bench_share_text,
            results.throttle.sustainedFreqMhz,
            "%.1f".format(Locale.US, results.storage.seqWriteMbPerSec),
            "%.1f".format(Locale.US, results.storage.seqReadMbPerSec),
            "%.1f".format(Locale.US, results.ram.writeMbPerSec),
            "%.1f".format(Locale.US, results.ram.readMbPerSec),
            "%.1f".format(Locale.US, results.gpu.avgFps),
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.all_bench_share_button)))
    }
}
