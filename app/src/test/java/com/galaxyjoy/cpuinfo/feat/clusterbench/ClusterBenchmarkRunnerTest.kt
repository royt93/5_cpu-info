package com.galaxyjoy.cpuinfo.feat.clusterbench

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyProvider
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Real JVM unit test for the `affinityConfirmed` aggregation this tech-debt sweep added —
 * previously undertestable because [DataNativeProviderCpu.setThreadAffinity] is a native
 * `external fun` that MockK can't stub (throws `UnsatisfiedLinkError`, same wall as U06/U34).
 * Unlocked by making [DataNativeProviderCpu] and that one method `open`, so this test overrides
 * it with a plain fake instead of asking MockK to proxy it — no native library, no device, no
 * `androidTest` needed. This is the first JVM unit test for any `*BenchmarkRunner` in this
 * codebase (the other 4 are only proven for real on-device, per their own doc comments); it only
 * covers this one runner because it's the one with branching logic worth asserting on a boolean,
 * not a wholesale change to that established convention.
 */
class ClusterBenchmarkRunnerTest {

    private class FakeDataNativeProviderCpu(private val deniedCoreIndex: Int?) : DataNativeProviderCpu() {
        override fun setThreadAffinity(coreIndexStart: Int, coreIndexCount: Int): Boolean =
            coreIndexStart != deniedCoreIndex
    }

    private val temperatureProvider: TemperatureProvider = mockk {
        every { getBatteryTemperature() } returns 25
    }

    private fun runnerWithCluster(cluster: ClusterTopologyBuilder.Cluster, deniedCoreIndex: Int?): ClusterBenchmarkRunner {
        val clusterTopologyProvider: ClusterTopologyProvider = mockk {
            every { clusters() } returns listOf(cluster)
        }
        return ClusterBenchmarkRunner(clusterTopologyProvider, FakeDataNativeProviderCpu(deniedCoreIndex), temperatureProvider)
    }

    private val fourCoreCluster = ClusterTopologyBuilder.Cluster(
        tier = ClusterTopologyBuilder.Tier.EFFICIENCY,
        coreIndexRange = 0 until 4,
        coreCount = 4,
        vendorName = "ARM",
        uarchName = "Cortex-A55",
        maxFreqMhz = 1800,
    )

    @Test
    fun `all workers pinned successfully yields affinityConfirmed true`() = runBlocking {
        val runner = runnerWithCluster(fourCoreCluster, deniedCoreIndex = null)
        val states = mutableListOf<ClusterBenchmarkRunner.State>()

        runner.run(durationPerClusterMs = 50L) { states += it }

        val finished = states.filterIsInstance<ClusterBenchmarkRunner.State.Finished>().single()
        val result = finished.result.clusters.single()
        assertTrue(result.affinityConfirmed)
        assertTrue(result.opsPerSecond > 0)
    }

    @Test
    fun `one denied worker out of four yields affinityConfirmed false`() = runBlocking {
        val runner = runnerWithCluster(fourCoreCluster, deniedCoreIndex = 2)
        val states = mutableListOf<ClusterBenchmarkRunner.State>()

        runner.run(durationPerClusterMs = 50L) { states += it }

        val finished = states.filterIsInstance<ClusterBenchmarkRunner.State.Finished>().single()
        assertFalse(finished.result.clusters.single().affinityConfirmed)
    }

    @Test
    fun `denied worker on a single-core cluster still yields affinityConfirmed false`() = runBlocking {
        val oneCoreCluster = fourCoreCluster.copy(coreIndexRange = 5 until 6, coreCount = 1)
        val runner = runnerWithCluster(oneCoreCluster, deniedCoreIndex = 5)
        val states = mutableListOf<ClusterBenchmarkRunner.State>()

        runner.run(durationPerClusterMs = 50L) { states += it }

        val finished = states.filterIsInstance<ClusterBenchmarkRunner.State.Finished>().single()
        assertFalse(finished.result.clusters.single().affinityConfirmed)
    }

    @Test
    fun `multiple clusters each get their own independent affinityConfirmed`() = runBlocking {
        val deniedCluster = fourCoreCluster.copy(tier = ClusterTopologyBuilder.Tier.PRIME, coreIndexRange = 4 until 6, coreCount = 2)
        val clusterTopologyProvider: ClusterTopologyProvider = mockk {
            every { clusters() } returns listOf(fourCoreCluster, deniedCluster)
        }
        val runner = ClusterBenchmarkRunner(clusterTopologyProvider, FakeDataNativeProviderCpu(deniedCoreIndex = 4), temperatureProvider)
        val states = mutableListOf<ClusterBenchmarkRunner.State>()

        runner.run(durationPerClusterMs = 50L) { states += it }

        val finished = states.filterIsInstance<ClusterBenchmarkRunner.State.Finished>().single()
        assertEquals(2, finished.result.clusters.size)
        assertTrue(finished.result.clusters.first { it.tier == ClusterTopologyBuilder.Tier.EFFICIENCY }.affinityConfirmed)
        assertFalse(finished.result.clusters.first { it.tier == ClusterTopologyBuilder.Tier.PRIME }.affinityConfirmed)
    }
}
