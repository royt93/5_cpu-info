package com.galaxyjoy.cpuinfo.feat.siliconlottery

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E04 — real-device tier for [SiliconLotteryRunner] against this device's REAL core count and
 * REAL `sched_setaffinity` behavior, same "runner classes proven for real on-device" precedent as
 * [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchmarkRunnerInstrumentedTest]. `affinityConfirmed`'s
 * value is logged, not asserted true/false — whether the kernel/cgroup allows the pin varies by
 * device/OS-process state (see that same nuance documented for U31 in epic-02-techdebt.md).
 */
@RunWith(AndroidJUnit4::class)
class SiliconLotteryRunnerInstrumentedTest {

    private lateinit var dataNativeProviderCpu: DataNativeProviderCpu

    @Before
    fun setUp() {
        dataNativeProviderCpu = DataNativeProviderCpu()
        dataNativeProviderCpu.initLibrary()
    }

    @Test
    fun run_onRealDeviceCores_producesOneResultPerRealCore() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dataProviderCpu = DataProviderCpu()
        val realCoreCount = dataProviderCpu.getNumberOfCores()
        val temperatureProvider = TemperatureProvider(appContext)

        val runner = SiliconLotteryRunner(dataProviderCpu, dataNativeProviderCpu, temperatureProvider)
        val states = mutableListOf<SiliconLotteryRunner.State>()

        runner.run(durationPerCoreMs = 100L) { state -> states += state }

        val finished = states.filterIsInstance<SiliconLotteryRunner.State.Finished>().single()
        assertTrue(finished.result.cores.size == realCoreCount)
        assertTrue(finished.result.cores.all { it.opsPerSecond > 0 })
        finished.result.cores.forEach {
            android.util.Log.i("SiliconLotteryTest", "core=${it.coreIndex} opsPerSecond=${it.opsPerSecond} affinityConfirmed=${it.affinityConfirmed}")
        }
    }
}
