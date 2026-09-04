package com.galaxyjoy.cpuinfo.feat.clusterbench

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyProvider
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U31 — real-device tier for the 2 pieces of genuinely new machinery this feature adds that a
 * JVM unit test can't touch: (1) the new native `setThreadAffinity` JNI call actually running
 * against the real kernel, and (2) [ClusterBenchmarkRunner] end-to-end against this device's
 * REAL cluster topology (real thread-per-core pinning, real dedicated thread pool teardown), not
 * a mocked one. Same "runner classes aren't unit-tested, only proven for real on-device" precedent
 * as [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmarkRunner]/etc — none of the 4 existing
 * `*BenchmarkRunner` classes have a JVM unit test either. Uses a tiny `durationPerClusterMs`
 * override so the full multi-cluster run stays fast even though it's real wall-clock work.
 */
@RunWith(AndroidJUnit4::class)
class ClusterBenchmarkRunnerInstrumentedTest {

    private lateinit var dataNativeProviderCpu: DataNativeProviderCpu

    @Before
    fun setUp() {
        dataNativeProviderCpu = DataNativeProviderCpu()
        dataNativeProviderCpu.initLibrary()
    }

    @Test
    fun setThreadAffinity_pinningToCore0_doesNotCrashAndReportsAResult() {
        // Core 0 always exists — this just proves the JNI call round-trips against the real
        // kernel without crashing; whether it returns true or false (cgroup policy can vary by
        // device/OS state) is not asserted, only that it returns rather than throwing/crashing.
        dataNativeProviderCpu.setThreadAffinity(coreIndexStart = 0, coreIndexCount = 1)
    }

    @Test
    fun setThreadAffinity_invalidRange_returnsFalseInsteadOfCrashing() {
        assertTrue(!dataNativeProviderCpu.setThreadAffinity(coreIndexStart = -1, coreIndexCount = 1))
        assertTrue(!dataNativeProviderCpu.setThreadAffinity(coreIndexStart = 0, coreIndexCount = 0))
    }

    @Test
    fun run_onRealDeviceTopology_producesOneResultPerRealCluster() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dataProviderCpu = DataProviderCpu()
        val clusterTopologyProvider = ClusterTopologyProvider(dataNativeProviderCpu, dataProviderCpu)
        val realClusters = clusterTopologyProvider.clusters()
        val temperatureProvider = TemperatureProvider(appContext)

        val runner = ClusterBenchmarkRunner(clusterTopologyProvider, dataNativeProviderCpu, temperatureProvider)
        val states = mutableListOf<ClusterBenchmarkRunner.State>()

        runner.run(durationPerClusterMs = 100L) { state -> states += state }

        val finished = states.filterIsInstance<ClusterBenchmarkRunner.State.Finished>().single()
        assertTrue(finished.result.clusters.size == realClusters.size)
        assertTrue(finished.result.clusters.all { it.opsPerSecond > 0 })
    }
}
