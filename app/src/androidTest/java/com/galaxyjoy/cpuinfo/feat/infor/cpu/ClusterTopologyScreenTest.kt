package com.galaxyjoy.cpuinfo.feat.infor.cpu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder.CacheLevel
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder.RawCache
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder.RawCluster
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget test for U06's cache-per-cluster row — renders [ClusterTopologyScreen] directly with
 * hand-built [ClusterTopologyBuilder] output (no Activity/native calls needed, same "render bare
 * Compose UI" pattern as [com.galaxyjoy.cpuinfo.feat.vipreport.VipDiagnosticContentTest]).
 */
@RunWith(AndroidJUnit4::class)
class ClusterTopologyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun privateCacheRowRendersSizeAndPrivateLabel() {
        val clusters = ClusterTopologyBuilder.build(
            listOf(RawCluster(coreStart = 0, coreCount = 2, vendorId = 3, uarchId = 0, maxFreqMhz = 1800)),
            listOf(RawCache(CacheLevel.L1D, sizeBytes = 65536, processorStart = 0, processorCount = 1)),
        )

        composeRule.setContent { CpuInfoTheme { ClusterTopologyScreen(clusters) } }

        val expected = appContext.getString(R.string.cluster_cache_private, "L1d", "64.00 KB")
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun sharedCacheRowRendersSizeAndCoreCount() {
        val clusters = ClusterTopologyBuilder.build(
            listOf(RawCluster(coreStart = 2, coreCount = 6, vendorId = 3, uarchId = 0, maxFreqMhz = 2200)),
            listOf(RawCache(CacheLevel.L2, sizeBytes = 524288, processorStart = 2, processorCount = 6)),
        )

        composeRule.setContent { CpuInfoTheme { ClusterTopologyScreen(clusters) } }

        val expected = appContext.getString(R.string.cluster_cache_shared, "L2", "512.00 KB", 6)
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun clusterWithNoCachesRendersWithoutCrashingOrCacheRows() {
        val clusters = ClusterTopologyBuilder.build(
            listOf(RawCluster(coreStart = 0, coreCount = 4, vendorId = 3, uarchId = 0, maxFreqMhz = 2000)),
        )

        composeRule.setContent { CpuInfoTheme { ClusterTopologyScreen(clusters) } }

        composeRule.onNodeWithText(
            appContext.getString(R.string.cluster_core_range, 4, 0, 3),
        ).assertExists()
    }
}
