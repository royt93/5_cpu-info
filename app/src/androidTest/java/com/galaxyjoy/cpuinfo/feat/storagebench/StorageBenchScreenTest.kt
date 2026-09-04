package com.galaxyjoy.cpuinfo.feat.storagebench

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for the U24 percentile addition to [StorageBenchScreen]'s Done state — same
 * "render bare Compose UI" pattern as [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchScreenTest].
 * Scoped to just this feature; this screen had no prior widget test file to extend.
 */
@RunWith(AndroidJUnit4::class)
class StorageBenchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val noThermalSnapshot = ThermalStatusProvider.Snapshot(statusSupported = false, status = -1, headroomPercent = null)

    private fun result(seqWriteMbPerSec: Double) = StorageBenchmark.Result(
        seqWriteMbPerSec = seqWriteMbPerSec,
        seqReadMbPerSec = 200.0,
        randomWriteOpsPerSec = 300.0,
        randomReadOpsPerSec = 400.0,
        hashMbPerSec = 50.0,
    )

    @Test
    fun doneState_withAtLeastTwoHistoryEntries_showsPercentileBetterThanAllPreviousRuns() {
        val history = listOf(
            StorageBenchResultPrefs.SavedResult(1L, 80.0, 200.0, 300.0, 400.0, 50.0),
            StorageBenchResultPrefs.SavedResult(2L, 100.0, 200.0, 300.0, 400.0, 50.0),
        )
        val state = VMStorageBench.UiState.Done(result(100.0), previous = null, history = history)

        composeRule.setContent {
            CpuInfoTheme { StorageBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.bench_percentile_message, 100)).assertExists()
    }

    @Test
    fun doneState_withFewerThanTwoHistoryEntries_hidesPercentileMessage() {
        val state = VMStorageBench.UiState.Done(result(100.0), previous = null, history = emptyList())

        composeRule.setContent {
            CpuInfoTheme { StorageBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("of your runs on this device", substring = true).assertDoesNotExist()
    }

    /** U29 — OS update impact vs the previous Android build. */
    @Test
    fun doneState_withADetectedOsUpdate_showsImpactMessage() {
        val history = listOf(
            StorageBenchResultPrefs.SavedResult(1L, 80.0, 200.0, 300.0, 400.0, 50.0, osBuildFingerprint = "build_a"),
            StorageBenchResultPrefs.SavedResult(2L, 100.0, 200.0, 300.0, 400.0, 50.0, osBuildFingerprint = "build_b"),
        )
        val state = VMStorageBench.UiState.Done(result(100.0), previous = null, history = history)

        composeRule.setContent {
            CpuInfoTheme { StorageBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        // (100 - 80) * 100 / 80 = +25%
        composeRule.onNodeWithText(appContext.getString(R.string.bench_os_update_impact_message, "+25")).assertExists()
    }

    @Test
    fun doneState_withoutADetectedOsUpdate_hidesImpactMessage() {
        val history = listOf(
            StorageBenchResultPrefs.SavedResult(1L, 80.0, 200.0, 300.0, 400.0, 50.0, osBuildFingerprint = "build_a"),
            StorageBenchResultPrefs.SavedResult(2L, 100.0, 200.0, 300.0, 400.0, 50.0, osBuildFingerprint = "build_a"),
        )
        val state = VMStorageBench.UiState.Done(result(100.0), previous = null, history = history)

        composeRule.setContent {
            CpuInfoTheme { StorageBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("vs before the update", substring = true).assertDoesNotExist()
    }
}
