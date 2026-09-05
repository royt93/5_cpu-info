package com.galaxyjoy.cpuinfo.feat.siliconlottery

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
class FrmSiliconLottery : Fragment() {

    private val viewModel: VMSiliconLottery by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SiliconLotteryScreen(
                    uiState = uiState,
                    onStartClicked = viewModel::onStartClicked,
                    onStopClicked = viewModel::onStopClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResult,
                )
            }
        }
    }

    private fun shareResult(result: SiliconLotteryBenchmark.Result) {
        val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
        val strongest = SiliconLotteryBenchmark.strongest(result)
        val weakest = SiliconLotteryBenchmark.weakest(result)
        val segments = result.cores.sortedBy { it.coreIndex }.joinToString(" · ") { core ->
            val label = getString(R.string.silicon_lottery_row_label, core.coreIndex) + when (core.coreIndex) {
                strongest?.coreIndex -> " " + getString(R.string.silicon_lottery_strongest_label)
                weakest?.coreIndex -> " " + getString(R.string.silicon_lottery_weakest_label)
                else -> ""
            }
            getString(R.string.silicon_lottery_share_segment, label, ops.format(core.opsPerSecond))
        }
        val text = getString(
            R.string.silicon_lottery_share_text,
            segments,
            SiliconLotteryBenchmark.spreadPercent(result),
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.silicon_lottery_share_button)))
    }
}
