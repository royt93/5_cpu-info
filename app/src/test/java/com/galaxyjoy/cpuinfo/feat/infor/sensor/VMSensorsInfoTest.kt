package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.data.provider.DataProviderSensor
import com.galaxyjoy.cpuinfo.domain.model.SensorReading
import com.galaxyjoy.cpuinfo.domain.observable.ObservableSensorData
import com.galaxyjoy.cpuinfo.domain.observe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VMSensorsInfoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val sensorA: Sensor = mockk {
        every { name } returns "Sensor A"
        every { type } returns Sensor.TYPE_LIGHT
    }
    private val sensorB: Sensor = mockk {
        every { name } returns "Sensor B"
        every { type } returns Sensor.TYPE_LIGHT
    }
    private val unknownSensor: Sensor = mockk {
        every { name } returns "Unknown sensor"
    }

    private val dataProviderSensor: DataProviderSensor = mockk {
        every { getSensorList() } returns listOf(sensorA, sensorB)
    }
    private val observableSensorData: ObservableSensorData = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = VMSensorsInfo(dataProviderSensor, observableSensorData)

    @Test
    fun `startProvidingData seeds listLiveData with one placeholder row per sensor`() {
        every { observableSensorData.observe() } returns emptyFlow()

        val vm = newViewModel()
        vm.startProvidingData()

        assertEquals(2, vm.listLiveData.size)
        assertEquals("Sensor A" to " ", vm.listLiveData[0])
        assertEquals("Sensor B" to " ", vm.listLiveData[1])
    }

    @Test
    fun `startProvidingData collects emitted readings and updates the matching row`() {
        every { observableSensorData.observe() } returns flowOf(SensorReading(sensorA, floatArrayOf(123.4f)))

        val vm = newViewModel()
        vm.startProvidingData()

        assertEquals("Sensor A" to "Illuminance=123.4lx", vm.listLiveData[0])
        assertEquals("Sensor B" to " ", vm.listLiveData[1])
    }

    /**
     * Regression test for B28: indexOf(event.sensor) can return -1 on custom ROMs where the
     * SensorEvent's sensor instance doesn't match-by-equals the one captured at startup.
     * Writing to listLiveData[-1] used to throw IndexOutOfBoundsException.
     */
    @Test
    fun `indexOfSensor returns null for a sensor not in the tracked list`() {
        val vm = newViewModel()
        every { observableSensorData.observe() } returns emptyFlow()
        vm.startProvidingData()

        assertNull(vm.indexOfSensor(unknownSensor))
    }

    @Test
    fun `indexOfSensor returns the row index for a tracked sensor`() {
        val vm = newViewModel()
        every { observableSensorData.observe() } returns emptyFlow()
        vm.startProvidingData()

        assertEquals(0, vm.indexOfSensor(sensorA))
        assertEquals(1, vm.indexOfSensor(sensorB))
    }

    /**
     * Regression test for memory leak #1 (doc/MEMORY_LEAK.MD): the ViewModel must cancel its
     * collection job when destroyed, even if stopProvidingData() was never called by the
     * Fragment — cancellation is what triggers ObservableSensorData's callbackFlow awaitClose,
     * which is what actually unregisters the SensorEventListener (verified separately in
     * ObservableSensorDataTest).
     */
    @Test
    fun `onCleared cancels the collection job`() {
        var wasClosed = false
        every { observableSensorData.observe() } returns callbackFlow {
            awaitClose { wasClosed = true }
        }

        val vm = newViewModel()
        vm.startProvidingData()
        assertTrue(!wasClosed)

        // ViewModel.onCleared is protected — invoke via reflection (the runtime
        // would normally call it via ViewModelStore.clear()).
        val onCleared = ViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(vm)

        assertTrue(wasClosed)
    }

    /**
     * Regression test for B14: registering/unregistering used to run on a background coroutine
     * launch, which could interleave on fast start/stop calls. stopProvidingData() must cancel
     * any in-flight collection instead of leaving a stale one running underneath a new one.
     */
    @Test
    fun `stopProvidingData cancels the collection job`() {
        var wasClosed = false
        every { observableSensorData.observe() } returns callbackFlow {
            awaitClose { wasClosed = true }
        }

        val vm = newViewModel()
        vm.startProvidingData()
        vm.stopProvidingData()

        assertTrue(wasClosed)
    }

    @Test
    fun `startProvidingData cancels a previous collection job before starting a new one`() {
        var firstClosed = false
        every { observableSensorData.observe() } returns callbackFlow {
            awaitClose { firstClosed = true }
        } andThen callbackFlow { awaitClose { } }

        val vm = newViewModel()
        vm.startProvidingData()
        vm.startProvidingData()

        assertTrue(firstClosed)
    }
}
