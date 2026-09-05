package com.galaxyjoy.cpuinfo.feat.siliconlottery

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
 * Widget test for [SiliconLotteryScreen] (E04) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class SiliconLotteryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun idleState_showsStartButtonAndDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(VMSiliconLottery.UiState.Idle, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_disclaimer_title)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_start_button)).assertExists()
    }

    @Test
    fun runningState_showsCoreProgressLabel() {
        val state = VMSiliconLottery.UiState.Running(coreIndex = 0, coreCount = 4)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(R.string.silicon_lottery_running_label, 1, 4)
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun doneState_showsOneResultRowPerCoreWithStrongestAndWeakestBadges() {
        val result = SiliconLotteryBenchmark.Result(
            cores = listOf(
                SiliconLotteryBenchmark.CoreResult(coreIndex = 0, opsPerSecond = 5_000_000L),
                SiliconLotteryBenchmark.CoreResult(coreIndex = 1, opsPerSecond = 9_000_000L),
                SiliconLotteryBenchmark.CoreResult(coreIndex = 2, opsPerSecond = 3_000_000L),
            ),
        )
        val state = VMSiliconLottery.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
        val strongestLabel = appContext.getString(R.string.silicon_lottery_row_label, 1) + " " +
            appContext.getString(R.string.silicon_lottery_strongest_label)
        val weakestLabel = appContext.getString(R.string.silicon_lottery_row_label, 2) + " " +
            appContext.getString(R.string.silicon_lottery_weakest_label)
        composeRule.onNodeWithText(strongestLabel).assertExists()
        composeRule.onNodeWithText(weakestLabel).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_ops_value, ops.format(9_000_000L))).assertExists()
        composeRule.onNodeWithText(
            appContext.getString(R.string.silicon_lottery_spread_label, SiliconLotteryBenchmark.spreadPercent(result)),
        ).assertExists()
    }

    @Test
    fun doneState_singleCore_hidesSpreadLine() {
        val result = SiliconLotteryBenchmark.Result(cores = listOf(SiliconLotteryBenchmark.CoreResult(coreIndex = 0, opsPerSecond = 5_000_000L)))
        val state = VMSiliconLottery.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_spread_label, 0.0)).assertDoesNotExist()
    }

    @Test
    fun doneState_affinityNotConfirmed_showsWarningRow() {
        val result = SiliconLotteryBenchmark.Result(
            cores = listOf(SiliconLotteryBenchmark.CoreResult(coreIndex = 0, opsPerSecond = 5_000_000L, affinityConfirmed = false)),
        )
        val state = VMSiliconLottery.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_affinity_warning)).assertExists()
    }

    @Test
    fun abortedState_overheat_showsOverheatMessageNotInterruptedMessage() {
        val state = VMSiliconLottery.UiState.Aborted(SiliconLotteryBenchmark.AbortReason.OVERHEAT)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_aborted_overheat)).assertExists()
    }

    @Test
    fun abortedState_interrupted_showsInterruptedMessageNotOverheatMessage() {
        val state = VMSiliconLottery.UiState.Aborted(SiliconLotteryBenchmark.AbortReason.INTERRUPTED)

        composeRule.setContent {
            CpuInfoTheme { SiliconLotteryScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.silicon_lottery_aborted_interrupted)).assertExists()
    }
}
