package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.content.Intent
import android.hardware.Sensor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.setting.BaseRoundedBottomSheet
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * F07 "Interactive Sensor Test Suite" — walks the user through a short physical action per
 * available sensor (shake, rotate, cover, blow) and reports whether real hardware input was
 * detected. Complements the passive live-readout list on [FrmSensorsInfo] rather than replacing
 * it — this is a guided pass/fail check, that screen is a raw data view.
 */
@AndroidEntryPoint
class SensorTestBottomSheet : BaseRoundedBottomSheet() {

    private val viewModel: VMSensorTest by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CpuInfoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SensorTestScreen(
                    uiState = uiState,
                    onStartClicked = viewModel::onStartClicked,
                    onSkipClicked = viewModel::onSkipClicked,
                    onDoneClicked = viewModel::onDoneClicked,
                    onShareClicked = ::shareResults,
                )
            }
        }
    }

    private fun shareResults(results: List<SensorTestRunner.StepResult>) {
        val text = buildString {
            appendLine(getString(R.string.sensor_test_title))
            results.forEach { result ->
                val nameRes = sensorNameResFor(result.sensorType)
                val outcomeRes = when (result.outcome) {
                    SensorTestRunner.StepOutcome.DETECTED -> R.string.sensor_test_result_detected
                    SensorTestRunner.StepOutcome.SKIPPED,
                    SensorTestRunner.StepOutcome.UNAVAILABLE,
                    -> R.string.sensor_test_result_skipped
                    SensorTestRunner.StepOutcome.TIMED_OUT -> R.string.sensor_test_result_timed_out
                }
                appendLine("${getString(nameRes)}: ${getString(outcomeRes)}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.sensor_test_share_button)))
    }

    private fun sensorNameResFor(sensorType: Int): Int = when (sensorType) {
        Sensor.TYPE_ACCELEROMETER -> R.string.sensor_test_name_accelerometer
        Sensor.TYPE_GYROSCOPE -> R.string.sensor_test_name_gyroscope
        Sensor.TYPE_MAGNETIC_FIELD -> R.string.sensor_test_name_magnetometer
        Sensor.TYPE_LIGHT -> R.string.sensor_test_name_light
        Sensor.TYPE_PROXIMITY -> R.string.sensor_test_name_proximity
        Sensor.TYPE_PRESSURE -> R.string.sensor_test_name_pressure
        else -> R.string.sensor_test_title
    }

    companion object {
        const val TAG = "SensorTestBottomSheet"
    }
}
