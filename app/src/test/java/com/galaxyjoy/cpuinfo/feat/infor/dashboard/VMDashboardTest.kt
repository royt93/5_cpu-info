package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.galaxyjoy.cpuinfo.domain.model.CpuData
import com.galaxyjoy.cpuinfo.domain.model.RamData
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableCpuData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableRamData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableTemperatureData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VMDashboardTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun cpuData(vararg utilizationPairs: Pair<Long, Long>) = CpuData(
        processorName = "Test",
        abi = "arm64-v8a",
        coreNumber = utilizationPairs.size,
        hasArmNeon = true,
        frequencies = utilizationPairs.map { (current, max) ->
            CpuData.Frequency(min = 300_000, max = max, current = current)
        },
        l1dCaches = "",
        l1iCaches = "",
        l2Caches = "",
        l3Caches = "",
        l4Caches = "",
    )

    private val observableCpuData: ObservableCpuData = mockk()
    private val observableRamData: ObservableRamData = mockk()
    private val observableTemperatureData: ObservableTemperatureData = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = VMDashboard(observableCpuData, observableRamData, observableTemperatureData)

    @Test
    fun `startCollecting records average CPU utilization, RAM used percent, and battery temp`() {
        every { observableCpuData.observe(Unit) } returns flowOf(cpuData(1_000_000L to 2_000_000L, 1_500_000L to 2_000_000L))
        every { observableRamData.observe(Unit) } returns
            flowOf(RamData(total = 1000, available = 300, availablePercentage = 30, threshold = 100))
        every { observableTemperatureData.observe(Unit) } returns
            flowOf(TemperatureData.Available(cpuTemp = 40f, batteryTemp = 35f))

        val viewModel = newViewModel()
        viewModel.startCollecting()
        val state = viewModel.uiState.value

        assertEquals(1, state.cpuLoadPoints.size)
        assertEquals(62.5f, state.cpuLoadPoints.first().value) // avg(50%, 75%)
        assertEquals(1, state.ramUsedPoints.size)
        assertEquals(70f, state.ramUsedPoints.first().value) // 100 - 30
        assertEquals(1, state.batteryTempPoints.size)
        assertEquals(35f, state.batteryTempPoints.first().value)
    }

    @Test
    fun `cores with max freq 0 are excluded from the average instead of crashing`() {
        every { observableCpuData.observe(Unit) } returns flowOf(cpuData(0L to 0L))
        every { observableRamData.observe(Unit) } returns
            flowOf(RamData(total = 1000, available = 300, availablePercentage = 30, threshold = 100))
        every { observableTemperatureData.observe(Unit) } returns flowOf(TemperatureData.Unavailable)

        val viewModel = newViewModel()
        viewModel.startCollecting()

        assertTrue(viewModel.uiState.value.cpuLoadPoints.isEmpty())
    }

    @Test
    fun `Unavailable and Probing temperature states are ignored, not recorded as 0`() {
        every { observableCpuData.observe(Unit) } returns flowOf(cpuData(1_000_000L to 2_000_000L))
        every { observableRamData.observe(Unit) } returns
            flowOf(RamData(total = 1000, available = 300, availablePercentage = 30, threshold = 100))
        every { observableTemperatureData.observe(Unit) } returns
            flowOf(TemperatureData.Probing, TemperatureData.Unavailable)

        val viewModel = newViewModel()
        viewModel.startCollecting()

        assertTrue(viewModel.uiState.value.batteryTempPoints.isEmpty())
    }

    @Test
    fun `multiple emissions accumulate into a growing history instead of overwriting`() {
        every { observableCpuData.observe(Unit) } returns
            flowOf(cpuData(1_000_000L to 2_000_000L), cpuData(1_500_000L to 2_000_000L), cpuData(2_000_000L to 2_000_000L))
        every { observableRamData.observe(Unit) } returns
            flowOf(RamData(total = 1000, available = 300, availablePercentage = 30, threshold = 100))
        every { observableTemperatureData.observe(Unit) } returns flowOf(TemperatureData.Unavailable)

        val viewModel = newViewModel()
        viewModel.startCollecting()

        assertEquals(3, viewModel.uiState.value.cpuLoadPoints.size)
        assertEquals(
            listOf(50f, 75f, 100f),
            viewModel.uiState.value.cpuLoadPoints.map { it.value },
        )
    }

    @Test
    fun `stopCollecting cancels the collection job`() {
        every { observableCpuData.observe(Unit) } returns flowOf(cpuData(1_000_000L to 2_000_000L))
        every { observableRamData.observe(Unit) } returns
            flowOf(RamData(total = 1000, available = 300, availablePercentage = 30, threshold = 100))
        every { observableTemperatureData.observe(Unit) } returns flowOf(TemperatureData.Unavailable)

        val viewModel = newViewModel()
        viewModel.startCollecting()
        val stateAfterStart = viewModel.uiState.value

        // Doesn't throw, and leaves the last collected state in place.
        viewModel.stopCollecting()

        assertEquals(stateAfterStart, viewModel.uiState.value)
    }
}
