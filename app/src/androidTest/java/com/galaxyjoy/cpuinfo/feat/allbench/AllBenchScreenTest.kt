package com.galaxyjoy.cpuinfo.feat.allbench

import android.opengl.GLSurfaceView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchmark
import com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmark
import com.galaxyjoy.cpuinfo.feat.storagebench.StorageBenchmark
import com.galaxyjoy.cpuinfo.feat.throttle.ThrottleFingerprint
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for [AllBenchScreen] (U17) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.gpubench.GpuBenchScreenTest]. The GPU step reuses
 * [com.galaxyjoy.cpuinfo.feat.gpubench.GpuSurfaceView], so a no-op renderer stands in the same
 * way it does there.
 */
@RunWith(AndroidJUnit4::class)
class AllBenchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val noOpRenderer = object : GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {}
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
        override fun onDrawFrame(gl: GL10?) {}
    }

    private val sampleResults = VMAllBench.Results(
        throttle = ThrottleFingerprint.Result(
            peakFreqMhz = 2400, sustainedFreqMhz = 2000, throttlePercent = 17, throttled = true,
            startTempC = 30, maxTempC = 40, durationMs = 30_000, aborted = false, abortReason = null,
            opsPerSecond = 123_456,
        ),
        storage = StorageBenchmark.Result(
            seqWriteMbPerSec = 100.0, seqReadMbPerSec = 200.0,
            randomWriteOpsPerSec = 300.0, randomReadOpsPerSec = 400.0, hashMbPerSec = 50.0,
        ),
        ram = RamBenchmark.Result(writeMbPerSec = 4000.0, readMbPerSec = 5000.0),
        gpu = GpuBenchmark.Result(avgFps = 55.5, frameCount = 300, durationMs = 5000),
    )

    @Test
    fun idleState_showsStartButton() {
        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(VMAllBench.UiState.Idle, { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.all_bench_start_button)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.all_bench_disclaimer_title)).assertExists()
    }

    @Test
    fun runningState_throttleStep_showsStepOneOfFour() {
        val state = VMAllBench.UiState.Running(VMAllBench.Step.THROTTLE)

        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(state, { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(
            R.string.all_bench_running_label, 1, appContext.getString(R.string.throttle),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun runningState_gpuStep_showsStepFourOfFourAndMountsGpuSurface() {
        val state = VMAllBench.UiState.Running(VMAllBench.Step.GPU)

        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(state, { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(
            R.string.all_bench_running_label, 4, appContext.getString(R.string.gpu_bench),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun doneState_showsAllFourResults() {
        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(VMAllBench.UiState.Done(sampleResults), { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("2000 MHz").assertExists()
        composeRule.onNodeWithText("100.0/200.0 MB/s").assertExists()
        composeRule.onNodeWithText("4000.0/5000.0 MB/s").assertExists()
        composeRule.onNodeWithText("55.5 FPS").assertExists()
    }

    /** U23 — the "Share as image" button added to `DoneContent` alongside the pre-existing
     * text-share button. */
    @Test
    fun doneState_showsShareAsImageButton() {
        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(VMAllBench.UiState.Done(sampleResults), { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.bench_result_card_share_image_button)).assertExists()
    }

    @Test
    fun abortedState_ram_showsRamStepNameInMessage() {
        val state = VMAllBench.UiState.Aborted(VMAllBench.Step.RAM)

        composeRule.setContent {
            CpuInfoTheme { AllBenchScreen(state, { noOpRenderer }, {}, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(R.string.all_bench_aborted, appContext.getString(R.string.ram_bench))
        composeRule.onNodeWithText(expected).assertExists()
    }
}
