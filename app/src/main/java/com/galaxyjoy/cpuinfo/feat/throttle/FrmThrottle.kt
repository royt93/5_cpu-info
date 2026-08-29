package com.galaxyjoy.cpuinfo.feat.throttle

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

@AndroidEntryPoint
class FrmThrottle : Fragment() {

    private val viewModel: VMThrottle by viewModels()

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
                ThrottleScreen(
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

    private fun shareResult(result: ThrottleFingerprint.Result) {
        val text = getString(
            R.string.throttle_share_text,
            result.peakFreqMhz,
            result.sustainedFreqMhz,
            result.throttlePercent,
            result.maxTempC,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.throttle_share_button)))
    }
}
