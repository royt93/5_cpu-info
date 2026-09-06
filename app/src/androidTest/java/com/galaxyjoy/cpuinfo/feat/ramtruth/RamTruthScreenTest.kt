package com.galaxyjoy.cpuinfo.feat.ramtruth

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
 * Widget test for [RamTruthScreen] (E03) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.storagetruth.StorageTruthScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class RamTruthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun idleState_showsStartButtonAndDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(VMRamTruth.UiState.Idle, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_disclaimer_title)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_start_button)).assertExists()
    }

    @Test
    fun runningState_showsChunkProgressLabel() {
        val state = VMRamTruth.UiState.Running(chunkIndex = 4, chunkCount = 15)

        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_running_label, 5, 15)).assertExists()
    }

    @Test
    fun doneState_genuine_showsGenuineVerdict() {
        val measurements = (0 until 6).map { RamTruthBenchmark.ChunkMeasurement(it, 1000.0) }
        val state = VMRamTruth.UiState.Done(RamTruthBenchmark.Result(measurements))

        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_verdict_genuine)).assertExists()
    }

    @Test
    fun doneState_suspectVirtualRam_showsSuspectVerdictAndCliffDetail() {
        val measurements = listOf(
            RamTruthBenchmark.ChunkMeasurement(0, 1000.0),
            RamTruthBenchmark.ChunkMeasurement(1, 1000.0),
            RamTruthBenchmark.ChunkMeasurement(2, 1000.0),
            RamTruthBenchmark.ChunkMeasurement(3, 100.0),
            RamTruthBenchmark.ChunkMeasurement(4, 100.0),
            RamTruthBenchmark.ChunkMeasurement(5, 100.0),
        )
        val result = RamTruthBenchmark.Result(measurements)
        val state = VMRamTruth.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(state, {}, {}, {}, {}) }
        }

        val cliffMb = (RamTruthBenchmark.cliffAtBytes(result) ?: 0L) / (1024 * 1024)
        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_verdict_suspect)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_cliff_detail, cliffMb.toString())).assertExists()
    }

    @Test
    fun abortedState_lowMemory_showsLowMemoryMessage() {
        val state = VMRamTruth.UiState.Aborted(RamTruthBenchmark.AbortReason.LOW_MEMORY_SIGNAL)

        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_aborted_low_memory)).assertExists()
    }

    @Test
    fun abortedState_insufficientMemory_showsInsufficientMemoryMessageNotLowMemoryMessage() {
        val state = VMRamTruth.UiState.Aborted(RamTruthBenchmark.AbortReason.INSUFFICIENT_AVAILABLE_MEMORY)

        composeRule.setContent {
            CpuInfoTheme { RamTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_truth_aborted_insufficient_memory)).assertExists()
    }
}
