package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderTemperature
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.domain.observe
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Prefs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObservableTemperatureDataTest {

    private val dataProviderTemperature: DataProviderTemperature = mockk()
    private val prefs: Prefs = mockk()
    private val observable = ObservableTemperatureData(DispatchersProvider(), dataProviderTemperature, prefs)

    @Test
    fun `no cached path, discoverable CPU path, battery present - probes then polls Available`() = runTest {
        every { prefs.get("temp_cpu_path_key", "") } returns ""
        every { dataProviderTemperature.findCpuTempPath() } returns "/sys/class/thermal/thermal_zone0/temp"
        every { prefs.insert("temp_cpu_path_key", any()) } returns Unit
        every { dataProviderTemperature.getBatteryTemperature() } returns 30f
        every { dataProviderTemperature.getCpuTemp(any()) } returns 40f

        val emissions = observable.observe().take(3).toList()

        assertEquals(TemperatureData.Probing, emissions[0])
        assertIs<TemperatureData.Available>(emissions[1])
        assertEquals(40f, (emissions[1] as TemperatureData.Available).cpuTemp)
        assertEquals(30f, (emissions[1] as TemperatureData.Available).batteryTemp)
        verify(exactly = 1) { dataProviderTemperature.findCpuTempPath() }
        verify(exactly = 1) { prefs.insert("temp_cpu_path_key", "/sys/class/thermal/thermal_zone0/temp") }
    }

    @Test
    fun `cached path present - skips Probing entirely and polls straight away`() = runTest {
        every { prefs.get("temp_cpu_path_key", "") } returns "/sys/class/thermal/thermal_zone0/temp"
        every { dataProviderTemperature.getBatteryTemperature() } returns 30f
        every { dataProviderTemperature.getCpuTemp(any()) } returns 40f

        val emissions = observable.observe().take(2).toList()

        assertIs<TemperatureData.Available>(emissions[0])
        verify(exactly = 0) { dataProviderTemperature.findCpuTempPath() }
    }

    @Test
    fun `no CPU path and no battery - emits Probing then Unavailable and completes`() = runTest {
        every { prefs.get("temp_cpu_path_key", "") } returns ""
        every { dataProviderTemperature.findCpuTempPath() } returns null
        every { dataProviderTemperature.getBatteryTemperature() } returns null

        val emissions = observable.observe().toList()

        assertEquals(listOf(TemperatureData.Probing, TemperatureData.Unavailable), emissions)
    }

    @Test
    fun `no CPU path but battery available - polls Available with null cpuTemp`() = runTest {
        every { prefs.get("temp_cpu_path_key", "") } returns ""
        every { dataProviderTemperature.findCpuTempPath() } returns null
        every { dataProviderTemperature.getBatteryTemperature() } returns 25f

        val emissions = observable.observe().take(2).toList()

        assertIs<TemperatureData.Available>(emissions[1])
        assertEquals(null, (emissions[1] as TemperatureData.Available).cpuTemp)
        assertEquals(25f, (emissions[1] as TemperatureData.Available).batteryTemp)
    }
}
