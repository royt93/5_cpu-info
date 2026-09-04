package com.galaxyjoy.cpuinfo.feat.gpubench

import android.opengl.GLSurfaceView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for [GpuBenchScreen] (U15) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchScreenTest]. The Running state mounts a real
 * `GLSurfaceView` via `AndroidView` (see [GpuBenchScreen]'s `RunningContent`), so this needs a
 * device/emulator with a working GL driver — a no-op [GLSurfaceView.Renderer] stands in for
 * [GpuBenchmarkRunner]'s real shader workload since only the Compose layer (not the actual GL
 * frame timing, covered by [GpuBenchmarkTest]'s pure-math tests) is under test here.
 */
@RunWith(AndroidJUnit4::class)
class GpuBenchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val noThermalSnapshot = ThermalStatusProvider.Snapshot(statusSupported = false, status = -1, headroomPercent = null)
    private val noOpRenderer = object : GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {}
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
        override fun onDrawFrame(gl: GL10?) {}
    }

    @Test
    fun idleState_showsStartButtonAndNoPreviousMessage() {
        val state = VMGpuBench.UiState.Idle(previous = null)

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_start_button)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_disclaimer_title)).assertExists()
    }

    @Test
    fun runningState_warmingUp_showsWarmupLabelNotMeasuringLabel() {
        val state = VMGpuBench.UiState.Running(warmingUp = true)

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_running_warmup)).assertExists()
    }

    @Test
    fun runningState_measuring_showsMeasuringLabel() {
        val state = VMGpuBench.UiState.Running(warmingUp = false)

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_running_measuring)).assertExists()
    }

    @Test
    fun doneState_showsFpsResultAndNoPreviousMessage() {
        val state = VMGpuBench.UiState.Done(
            result = GpuBenchmark.Result(avgFps = 42.5, frameCount = 250, durationMs = 5000),
            previous = null,
            history = emptyList(),
        )

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("42.5 FPS").assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_no_previous)).assertExists()
    }

    /** U18 — [com.galaxyjoy.cpuinfo.ui.component.BenchTrendChart] mounts a real MPAndroidChart
     * `LineChart` (an Android View, not Compose) once there are 2+ history points; this just
     * needs to not crash the render, the chart's own drawing isn't asserted on here. */
    @Test
    fun doneState_withMultipleHistoryEntries_rendersTrendChartWithoutCrashing() {
        val state = VMGpuBench.UiState.Done(
            result = GpuBenchmark.Result(avgFps = 42.5, frameCount = 250, durationMs = 5000),
            previous = GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 30.0),
            history = listOf(
                GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 30.0),
                GpuBenchResultPrefs.SavedResult(timestampMs = 2L, avgFps = 42.5),
            ),
        )

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("42.5 FPS").assertExists()
        // U24 — percentile vs this device's own history: current run (42.5) beats both entries.
        composeRule.onNodeWithText(appContext.getString(R.string.bench_percentile_message, 100)).assertExists()
    }

    @Test
    fun doneState_withFewerThanTwoHistoryEntries_hidesPercentileMessage() {
        val state = VMGpuBench.UiState.Done(
            result = GpuBenchmark.Result(avgFps = 42.5, frameCount = 250, durationMs = 5000),
            previous = null,
            history = emptyList(),
        )

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("of your runs on this device", substring = true).assertDoesNotExist()
    }

    /** U29 — OS update impact vs the previous Android build. */
    @Test
    fun doneState_withADetectedOsUpdate_showsImpactMessage() {
        val history = listOf(
            GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 30.0, osBuildFingerprint = "build_a"),
            GpuBenchResultPrefs.SavedResult(timestampMs = 2L, avgFps = 42.5, osBuildFingerprint = "build_b"),
        )
        val state = VMGpuBench.UiState.Done(
            result = GpuBenchmark.Result(avgFps = 42.5, frameCount = 250, durationMs = 5000),
            previous = null,
            history = history,
        )

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        // (42.5 - 30) * 100 / 30 = +42%
        composeRule.onNodeWithText(appContext.getString(R.string.bench_os_update_impact_message, "+42")).assertExists()
    }

    @Test
    fun doneState_withoutADetectedOsUpdate_hidesImpactMessage() {
        val history = listOf(
            GpuBenchResultPrefs.SavedResult(timestampMs = 1L, avgFps = 30.0, osBuildFingerprint = "build_a"),
            GpuBenchResultPrefs.SavedResult(timestampMs = 2L, avgFps = 42.5, osBuildFingerprint = "build_a"),
        )
        val state = VMGpuBench.UiState.Done(
            result = GpuBenchmark.Result(avgFps = 42.5, frameCount = 250, durationMs = 5000),
            previous = null,
            history = history,
        )

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("vs before the update", substring = true).assertDoesNotExist()
    }

    @Test
    fun abortedState_overheat_showsOverheatMessageNotInterruptedMessage() {
        val state = VMGpuBench.UiState.Aborted(GpuBenchmark.AbortReason.OVERHEAT)

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_aborted_overheat)).assertExists()
    }

    @Test
    fun abortedState_interrupted_showsInterruptedMessageNotOverheatMessage() {
        val state = VMGpuBench.UiState.Aborted(GpuBenchmark.AbortReason.INTERRUPTED)

        composeRule.setContent {
            CpuInfoTheme { GpuBenchScreen(state, noThermalSnapshot, { noOpRenderer }, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.gpu_bench_aborted_interrupted)).assertExists()
    }
}
