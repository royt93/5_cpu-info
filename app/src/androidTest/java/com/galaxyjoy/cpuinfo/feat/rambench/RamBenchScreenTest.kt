package com.galaxyjoy.cpuinfo.feat.rambench

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
 * Widget test for [RamBenchScreen] (U16) — renders directly with hand-built state, same
 * "render bare Compose UI" pattern as [com.galaxyjoy.cpuinfo.feat.throttle.ThrottleScreenTest].
 * Neither Dashboard nor Storage Benchmark got this treatment (their docs note a harness NPE —
 * but that was specifically from `ActHostSmokeTest` navigating to their ViewPager position via
 * the shared Activity-bound `composeRule`, not from rendering their Screen composable in
 * isolation like this).
 */
@RunWith(AndroidJUnit4::class)
class RamBenchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val noThermalSnapshot = ThermalStatusProvider.Snapshot(statusSupported = false, status = -1, headroomPercent = null)

    @Test
    fun idleState_showsStartButtonAndNoPreviousMessage() {
        val state = VMRamBench.UiState.Idle(previous = null)

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_bench_start_button)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.ram_bench_disclaimer_title)).assertExists()
    }

    @Test
    fun runningState_showsCurrentPhaseLabel() {
        val state = VMRamBench.UiState.Running(RamBenchmarkRunner.Phase.WRITE)

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(
            R.string.ram_bench_running_label,
            appContext.getString(R.string.ram_bench_phase_write),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun doneState_showsWriteAndReadResultsAndNoPreviousMessage() {
        val state = VMRamBench.UiState.Done(
            result = RamBenchmark.Result(writeMbPerSec = 1234.5, readMbPerSec = 2345.6),
            previous = null,
            history = emptyList(),
        )

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("1234.5 MB/s").assertExists()
        composeRule.onNodeWithText("2345.6 MB/s").assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.ram_bench_no_previous)).assertExists()
    }

    /** U24 — percentile vs this device's own history, shown right below the trend chart. */
    @Test
    fun doneState_withAtLeastTwoHistoryEntries_showsPercentileBetterThanAllPreviousRuns() {
        val history = listOf(
            RamBenchResultPrefs.SavedResult(1L, writeMbPerSec = 1000.0, readMbPerSec = 2000.0),
            RamBenchResultPrefs.SavedResult(2L, writeMbPerSec = 1234.5, readMbPerSec = 2345.6),
        )
        val state = VMRamBench.UiState.Done(
            result = RamBenchmark.Result(writeMbPerSec = 1234.5, readMbPerSec = 2345.6),
            previous = null,
            history = history,
        )

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.bench_percentile_message, 100)).assertExists()
    }

    @Test
    fun doneState_withFewerThanTwoHistoryEntries_hidesPercentileMessage() {
        val state = VMRamBench.UiState.Done(
            result = RamBenchmark.Result(writeMbPerSec = 1234.5, readMbPerSec = 2345.6),
            previous = null,
            history = emptyList(),
        )

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("of your runs on this device", substring = true).assertDoesNotExist()
    }

    /** U29 — OS update impact vs the previous Android build. */
    @Test
    fun doneState_withADetectedOsUpdate_showsImpactMessage() {
        val history = listOf(
            RamBenchResultPrefs.SavedResult(1L, writeMbPerSec = 1000.0, readMbPerSec = 2000.0, osBuildFingerprint = "build_a"),
            RamBenchResultPrefs.SavedResult(2L, writeMbPerSec = 1500.0, readMbPerSec = 2345.6, osBuildFingerprint = "build_b"),
        )
        val state = VMRamBench.UiState.Done(
            result = RamBenchmark.Result(writeMbPerSec = 1500.0, readMbPerSec = 2345.6),
            previous = null,
            history = history,
        )

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        // (1500 - 1000) * 100 / 1000 = +50%
        composeRule.onNodeWithText(appContext.getString(R.string.bench_os_update_impact_message, "+50")).assertExists()
    }

    @Test
    fun doneState_withoutADetectedOsUpdate_hidesImpactMessage() {
        val history = listOf(
            RamBenchResultPrefs.SavedResult(1L, writeMbPerSec = 1000.0, readMbPerSec = 2000.0, osBuildFingerprint = "build_a"),
            RamBenchResultPrefs.SavedResult(2L, writeMbPerSec = 1234.5, readMbPerSec = 2345.6, osBuildFingerprint = "build_a"),
        )
        val state = VMRamBench.UiState.Done(
            result = RamBenchmark.Result(writeMbPerSec = 1234.5, readMbPerSec = 2345.6),
            previous = null,
            history = history,
        )

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("vs before the update", substring = true).assertDoesNotExist()
    }

    @Test
    fun abortedState_overheat_showsOverheatMessageNotLowMemoryMessage() {
        val state = VMRamBench.UiState.Aborted(RamBenchmark.AbortReason.OVERHEAT)

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_bench_aborted_overheat)).assertExists()
    }

    @Test
    fun abortedState_insufficientMemory_showsLowMemoryMessageNotOverheatMessage() {
        val state = VMRamBench.UiState.Aborted(RamBenchmark.AbortReason.INSUFFICIENT_MEMORY)

        composeRule.setContent {
            CpuInfoTheme { RamBenchScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.ram_bench_aborted_low_memory)).assertExists()
    }
}
