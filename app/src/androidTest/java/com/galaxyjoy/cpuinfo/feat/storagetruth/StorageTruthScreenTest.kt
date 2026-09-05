package com.galaxyjoy.cpuinfo.feat.storagetruth

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
 * Widget test for [StorageTruthScreen] (E01) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class StorageTruthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun idleState_showsStartButtonAndDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(VMStorageTruth.UiState.Idle, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_disclaimer_title)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_start_button)).assertExists()
    }

    @Test
    fun runningState_writing_showsWritingLabel() {
        val state = VMStorageTruth.UiState.Running(StorageTruthRunner.Phase.WRITING, blockIndex = 4, blockCount = 512)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_writing_label, 5, 512)).assertExists()
    }

    @Test
    fun runningState_verifying_showsVerifyingLabel() {
        val state = VMStorageTruth.UiState.Running(StorageTruthRunner.Phase.VERIFYING, blockIndex = 0, blockCount = 512)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_verifying_label)).assertExists()
    }

    @Test
    fun doneState_genuine_showsGenuineVerdictAndNoMismatchDetail() {
        val result = StorageTruthBenchmark.Result(blocksTested = 512, mismatches = emptyList())
        val state = VMStorageTruth.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_verdict_genuine)).assertExists()
    }

    @Test
    fun doneState_suspectFake_showsSuspectVerdictAndMismatchDetail() {
        val result = StorageTruthBenchmark.Result(
            blocksTested = 512,
            mismatches = listOf(StorageTruthBenchmark.MismatchedBlock(blockIndex = 8, expectedOffsetBytes = 8L * 1024 * 1024)),
        )
        val state = VMStorageTruth.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_verdict_suspect_fake)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_mismatch_detail, "8")).assertExists()
    }

    @Test
    fun abortedState_insufficientSpace_showsInsufficientSpaceMessage() {
        val state = VMStorageTruth.UiState.Aborted(StorageTruthBenchmark.AbortReason.INSUFFICIENT_SPACE)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_aborted_insufficient_space)).assertExists()
    }

    @Test
    fun abortedState_overheat_showsOverheatMessageNotInterruptedMessage() {
        val state = VMStorageTruth.UiState.Aborted(StorageTruthBenchmark.AbortReason.OVERHEAT)

        composeRule.setContent {
            CpuInfoTheme { StorageTruthScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.storage_truth_aborted_overheat)).assertExists()
    }
}
