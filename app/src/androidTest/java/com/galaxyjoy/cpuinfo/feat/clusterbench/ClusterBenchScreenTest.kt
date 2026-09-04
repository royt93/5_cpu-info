package com.galaxyjoy.cpuinfo.feat.clusterbench

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.util.Locale

/**
 * Widget test for [ClusterBenchScreen] (U31) — same "render bare Compose UI" pattern as
 * [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchScreenTest].
 */
@RunWith(AndroidJUnit4::class)
class ClusterBenchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun idleState_showsStartButtonAndDisclaimer() {
        composeRule.setContent {
            CpuInfoTheme { ClusterBenchScreen(VMClusterBench.UiState.Idle, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.cluster_bench_disclaimer_title)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.cluster_bench_start_button)).assertExists()
    }

    @Test
    fun runningState_showsClusterProgressLabel() {
        val state = VMClusterBench.UiState.Running(
            clusterIndex = 0,
            clusterCount = 3,
            tier = ClusterTopologyBuilder.Tier.PRIME,
        )

        composeRule.setContent {
            CpuInfoTheme { ClusterBenchScreen(state, {}, {}, {}, {}) }
        }

        val expected = appContext.getString(
            R.string.cluster_bench_running_label,
            1,
            3,
            appContext.getString(R.string.cluster_tier_prime),
        )
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun doneState_showsOneResultRowPerCluster() {
        val result = ClusterBenchmark.Result(
            clusters = listOf(
                ClusterBenchmark.ClusterResult(ClusterTopologyBuilder.Tier.PRIME, coreCount = 1, opsPerSecond = 9_000_000L),
                ClusterBenchmark.ClusterResult(ClusterTopologyBuilder.Tier.PERFORMANCE, coreCount = 3, opsPerSecond = 6_000_000L),
                ClusterBenchmark.ClusterResult(ClusterTopologyBuilder.Tier.EFFICIENCY, coreCount = 4, opsPerSecond = 3_000_000L),
            ),
        )
        val state = VMClusterBench.UiState.Done(result)

        composeRule.setContent {
            CpuInfoTheme { ClusterBenchScreen(state, {}, {}, {}, {}) }
        }

        val ops = NumberFormat.getIntegerInstance(Locale.getDefault())
        composeRule.onNodeWithText(
            appContext.getString(R.string.cluster_bench_row_label, appContext.getString(R.string.cluster_tier_prime), 1),
        ).assertExists()
        composeRule.onNodeWithText(
            appContext.getString(R.string.cluster_bench_ops_value, ops.format(9_000_000L)),
        ).assertExists()
        composeRule.onNodeWithText(
            appContext.getString(R.string.cluster_bench_row_label, appContext.getString(R.string.cluster_tier_efficiency), 4),
        ).assertExists()
        composeRule.onNodeWithText(
            appContext.getString(R.string.cluster_bench_ops_value, ops.format(3_000_000L)),
        ).assertExists()
    }

    @Test
    fun abortedState_overheat_showsOverheatMessageNotInterruptedMessage() {
        val state = VMClusterBench.UiState.Aborted(ClusterBenchmark.AbortReason.OVERHEAT)

        composeRule.setContent {
            CpuInfoTheme { ClusterBenchScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.cluster_bench_aborted_overheat)).assertExists()
    }

    @Test
    fun abortedState_interrupted_showsInterruptedMessageNotOverheatMessage() {
        val state = VMClusterBench.UiState.Aborted(ClusterBenchmark.AbortReason.INTERRUPTED)

        composeRule.setContent {
            CpuInfoTheme { ClusterBenchScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.cluster_bench_aborted_interrupted)).assertExists()
    }
}
