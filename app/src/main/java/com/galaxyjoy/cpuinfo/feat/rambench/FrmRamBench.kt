package com.galaxyjoy.cpuinfo.feat.rambench

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
import java.util.Locale

@AndroidEntryPoint
class FrmRamBench : Fragment() {

    private val viewModel: VMRamBench by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val thermalSnapshot by viewModel.thermalSnapshot.collectAsStateWithLifecycle()
                RamBenchScreen(
                    uiState = uiState,
                    thermalSnapshot = thermalSnapshot,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResult,
                )
            }
        }
    }

    private fun shareResult(result: RamBenchmark.Result) {
        val text = getString(
            R.string.ram_bench_share_text,
            "%.1f".format(Locale.US, result.writeMbPerSec),
            "%.1f".format(Locale.US, result.readMbPerSec),
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.ram_bench_share_button)))
    }
}
