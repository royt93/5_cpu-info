package com.galaxyjoy.cpuinfo.feat.sensortruth

import android.hardware.Sensor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for [SensorTruthScreen] (E02) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.ramtruth.RamTruthScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class SensorTruthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun idleState_showsStartButtonAndDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(VMSensorTruth.UiState.Idle, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_disclaimer_title)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_start_button)).assertExists()
    }

    @Test
    fun runningState_showsSensorProgressLabel() {
        val state = VMSensorTruth.UiState.Running(sensorIndex = 0, sensorCount = 2, sensorType = Sensor.TYPE_ACCELEROMETER)

        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(state, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(
            R.string.sensor_truth_running_label,
            1,
            2,
            appContext.getString(R.string.sensor_test_name_accelerometer),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun doneState_genuineSensor_showsGenuineVerdictAndRateDetail() {
        val audit = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_ACCELEROMETER, advertisedHz = 100.0, actualHz = 98.0, jitterMs = 0.5, eventCount = 50)
        val state = VMSensorTruth.UiState.Done(SensorTruthBenchmark.Result(listOf(audit)))

        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.sensor_test_name_accelerometer)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_verdict_genuine)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_rate_detail, "100", "98")).assertExists()
    }

    @Test
    fun doneState_suspectSensor_showsSuspectVerdict() {
        val audit = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_GYROSCOPE, advertisedHz = 200.0, actualHz = 20.0, jitterMs = 2.0, eventCount = 50)
        val state = VMSensorTruth.UiState.Done(SensorTruthBenchmark.Result(listOf(audit)))

        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_verdict_suspect)).assertExists()
    }

    @Test
    fun doneState_missingSensor_showsNoAdvertisedRateMessage() {
        val audit = SensorTruthBenchmark.SensorAudit(Sensor.TYPE_GYROSCOPE, advertisedHz = null, actualHz = 0.0, jitterMs = 0.0, eventCount = 0)
        val state = VMSensorTruth.UiState.Done(SensorTruthBenchmark.Result(listOf(audit)))

        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_no_advertised_rate)).assertExists()
    }

    @Test
    fun abortedState_showsInterruptedMessage() {
        val state = VMSensorTruth.UiState.Aborted(SensorTruthBenchmark.AbortReason.INTERRUPTED)

        composeRule.setContent {
            CpuInfoTheme { SensorTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.sensor_truth_aborted_interrupted)).assertExists()
    }
}
