package com.galaxyjoy.cpuinfo.feat.throttle

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.util.Locale

/**
 * Widget test for the benchmark-score addition to [ThrottleScreen]'s Done state (quick_win.md
 * #3 "CPU stress test / mini benchmark" — extended the existing Throttle Test rather than
 * building a separate feature, see doc/task/epic-04-unique-ideas.md). Renders directly with
 * hand-built state, same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class ThrottleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val noThermalSnapshot = ThermalStatusProvider.Snapshot(statusSupported = false, status = -1, headroomPercent = null)

    private fun result(opsPerSecond: Long) = ThrottleFingerprint.Result(
        peakFreqMhz = 2800,
        sustainedFreqMhz = 2600,
        throttlePercent = 7,
        throttled = false,
        startTempC = 30,
        maxTempC = 35,
        durationMs = 30_000,
        aborted = false,
        abortReason = null,
        opsPerSecond = opsPerSecond,
    )

    @Test
    fun firstRun_showsScoreRowAndNoPreviousMessage() {
        val state = VMThrottle.UiState.Done(result(opsPerSecond = 5_000_000L), previous = null)

        composeRule.setContent {
            CpuInfoTheme { ThrottleScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        val formattedOps = NumberFormat.getIntegerInstance(Locale.getDefault()).format(5_000_000L)
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_benchmark_score_label)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_benchmark_score_value, formattedOps)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_no_previous)).assertExists()
    }

    @Test
    fun withPreviousRun_showsScoreDeltaPercent() {
        val previous = ThrottleResultPrefs.SavedResult(
            timestampMs = System.currentTimeMillis(),
            peakFreqMhz = 2700,
            sustainedFreqMhz = 2500,
            throttlePercent = 10,
            maxTempC = 36,
            opsPerSecond = 4_000_000L,
        )
        val state = VMThrottle.UiState.Done(result(opsPerSecond = 5_000_000L), previous = previous)

        composeRule.setContent {
            CpuInfoTheme { ThrottleScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        // (5,000,000 - 4,000,000) * 100 / 4,000,000 = +25%
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_score_compare_previous, 25)).assertExists()
    }

    @Test
    fun previousRunPredatingScoreFeature_zeroOpsPerSecond_hidesScoreDeltaInsteadOfShowingBogusValue() {
        // SharedPreferences getLong() defaults to 0 for a key that didn't exist before this
        // feature shipped — must not compute a "+∞%"/divide-by-zero delta from that.
        val legacyPrevious = ThrottleResultPrefs.SavedResult(
            timestampMs = System.currentTimeMillis(),
            peakFreqMhz = 2700,
            sustainedFreqMhz = 2500,
            throttlePercent = 10,
            maxTempC = 36,
            opsPerSecond = 0L,
        )
        val state = VMThrottle.UiState.Done(result(opsPerSecond = 5_000_000L), previous = legacyPrevious)

        composeRule.setContent {
            CpuInfoTheme { ThrottleScreen(state, noThermalSnapshot, {}, {}, {}, {}) }
        }

        // The throttlePercent delta (unaffected by this bug) still renders...
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_compare_previous, -3)).assertExists()
        // ...but no score-delta text exists for any percentage.
        val formattedOps = NumberFormat.getIntegerInstance(Locale.getDefault()).format(5_000_000L)
        composeRule.onNodeWithText(appContext.getString(R.string.throttle_benchmark_score_value, formattedOps)).assertExists()
    }
}
