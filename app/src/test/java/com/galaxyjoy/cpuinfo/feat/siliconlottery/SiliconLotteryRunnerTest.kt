package com.galaxyjoy.cpuinfo.feat.siliconlottery

import com.galaxyjoy.cpuinfo.data.provider.DataNativeProviderCpu
import com.galaxyjoy.cpuinfo.data.provider.DataProviderCpu
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Real JVM unit test — same "open the one native call, fake it, no MockK/device needed" approach
 * as [com.galaxyjoy.cpuinfo.feat.clusterbench.ClusterBenchmarkRunnerTest], reused here since this
 * runner branches on [DataNativeProviderCpu.setThreadAffinity]'s return value the same way.
 */
class SiliconLotteryRunnerTest {

    private class FakeDataNativeProviderCpu(private val deniedCoreIndex: Int?) : DataNativeProviderCpu() {
        override fun setThreadAffinity(coreIndexStart: Int, coreIndexCount: Int): Boolean =
            coreIndexStart != deniedCoreIndex
    }

    private val temperatureProvider: TemperatureProvider = mockk {
        every { getBatteryTemperature() } returns 25
    }

    private fun runner(coreCount: Int, deniedCoreIndex: Int?): SiliconLotteryRunner {
        val dataProviderCpu: DataProviderCpu = mockk {
            every { getNumberOfCores() } returns coreCount
        }
        return SiliconLotteryRunner(dataProviderCpu, FakeDataNativeProviderCpu(deniedCoreIndex), temperatureProvider)
    }

    @Test
    fun `measures one result per logical core in order`() = runBlocking {
        val states = mutableListOf<SiliconLotteryRunner.State>()

        runner(coreCount = 4, deniedCoreIndex = null).run(durationPerCoreMs = 30L) { states += it }

        val finished = states.filterIsInstance<SiliconLotteryRunner.State.Finished>().single()
        assertEquals(listOf(0, 1, 2, 3), finished.result.cores.map { it.coreIndex })
        assertTrue(finished.result.cores.all { it.opsPerSecond > 0 })
    }

    @Test
    fun `all cores pinned successfully yields affinityConfirmed true for every core`() = runBlocking {
        val states = mutableListOf<SiliconLotteryRunner.State>()

        runner(coreCount = 4, deniedCoreIndex = null).run(durationPerCoreMs = 30L) { states += it }

        val finished = states.filterIsInstance<SiliconLotteryRunner.State.Finished>().single()
        assertTrue(finished.result.cores.all { it.affinityConfirmed })
    }

    @Test
    fun `one denied core reports affinityConfirmed false only for that core`() = runBlocking {
        val states = mutableListOf<SiliconLotteryRunner.State>()

        runner(coreCount = 4, deniedCoreIndex = 2).run(durationPerCoreMs = 30L) { states += it }

        val finished = states.filterIsInstance<SiliconLotteryRunner.State.Finished>().single()
        val byIndex = finished.result.cores.associateBy { it.coreIndex }
        assertFalse(byIndex.getValue(2).affinityConfirmed)
        assertTrue(byIndex.getValue(0).affinityConfirmed)
        assertTrue(byIndex.getValue(1).affinityConfirmed)
        assertTrue(byIndex.getValue(3).affinityConfirmed)
    }

    @Test
    fun `requestStop during the first core aborts as interrupted with no finished result`() = runBlocking {
        val states = mutableListOf<SiliconLotteryRunner.State>()
        val runner = runner(coreCount = 4, deniedCoreIndex = null)

        // run() resets the stop flag on entry (same as ClusterBenchmarkRunner), so requestStop()
        // must fire from inside the callback — after the first Running state, before the loop's
        // own if(stopRequested) check runs — to actually exercise the mid-run abort path.
        runner.run(durationPerCoreMs = 30L) { state ->
            states += state
            if (state is SiliconLotteryRunner.State.Running) runner.requestStop()
        }

        assertEquals(SiliconLotteryBenchmark.AbortReason.INTERRUPTED, states.filterIsInstance<SiliconLotteryRunner.State.Aborted>().single().reason)
        assertTrue(states.filterIsInstance<SiliconLotteryRunner.State.Finished>().isEmpty())
    }

    @Test
    fun `overheating temperature aborts before any core is measured`() = runBlocking {
        val hotTemperatureProvider: TemperatureProvider = mockk {
            every { getBatteryTemperature() } returns SiliconLotteryBenchmark.SAFETY_ABORT_TEMP_C
        }
        val dataProviderCpu: DataProviderCpu = mockk {
            every { getNumberOfCores() } returns 4
        }
        val runner = SiliconLotteryRunner(dataProviderCpu, FakeDataNativeProviderCpu(null), hotTemperatureProvider)
        val states = mutableListOf<SiliconLotteryRunner.State>()

        runner.run(durationPerCoreMs = 30L) { states += it }

        assertEquals(SiliconLotteryBenchmark.AbortReason.OVERHEAT, states.filterIsInstance<SiliconLotteryRunner.State.Aborted>().single().reason)
        assertTrue(states.filterIsInstance<SiliconLotteryRunner.State.Running>().isEmpty())
    }
}
