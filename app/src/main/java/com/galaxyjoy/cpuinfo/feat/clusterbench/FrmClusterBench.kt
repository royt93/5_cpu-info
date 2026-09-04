package com.galaxyjoy.cpuinfo.feat.clusterbench

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
class FrmClusterBench : Fragment() {

    private val viewModel: VMClusterBench by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ClusterBenchScreen(
                    uiState = uiState,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResult,
                )
            }
        }
    }

    private fun shareResult(result: ClusterBenchmark.Result) {
        val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
        val segments = result.clusters.joinToString(" · ") { cluster ->
            getString(
                R.string.cluster_bench_share_segment,
                getString(tierLabelRes(cluster.tier)),
                cluster.coreCount,
                ops.format(cluster.opsPerSecond),
            )
        }
        val text = getString(R.string.cluster_bench_share_text, segments)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.cluster_bench_share_button)))
    }
}
