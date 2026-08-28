package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VMSensorsInfoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val sensorA: Sensor = mockk {
        every { name } returns "Sensor A"
    }
    private val sensorB: Sensor = mockk {
        every { name } returns "Sensor B"
    }
    private val unknownSensor: Sensor = mockk {
        every { name } returns "Unknown sensor"
    }

    private val sensorManager: SensorManager = mockk(relaxed = true) {
        every { getSensorList(Sensor.TYPE_ALL) } returns listOf(sensorA, sensorB)
    }

    private val dispatchersProvider: DispatchersProvider = mockk {
        every { io } returns UnconfinedTestDispatcher()
        every { main } returns UnconfinedTestDispatcher()
    }

    /**
     * Regression test for memory leak #1 (doc/MEMORY_LEAK.MD):
     * ViewModel must unregister its SensorEventListener when destroyed,
     * even if [VMSensorsInfo.stopProvidingData] was never called by the Fragment.
     */
    @Test
    fun `onCleared unregisters sensor listener`() {
        val vm = VMSensorsInfo(sensorManager, dispatchersProvider)

        // ViewModel.onCleared is protected — invoke via reflection (the runtime
        // would normally call it via ViewModelStore.clear()).
        val onCleared = ViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(vm)

        verify { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    /**
     * Regression test for B14: registerListener/unregisterListener used to run on a background
     * coroutine, which could interleave on fast start/stop calls. They must now run synchronously
     * on the caller's thread, so registerListener is guaranteed to have already happened by the
     * time startProvidingData() returns.
     */
    @Test
    fun `startProvidingData registers listener synchronously for every sensor`() {
        val vm = VMSensorsInfo(sensorManager, dispatchersProvider)

        vm.startProvidingData()

        verify(exactly = 1) {
            sensorManager.registerListener(vm, sensorA, SensorManager.SENSOR_DELAY_NORMAL)
        }
        verify(exactly = 1) {
            sensorManager.registerListener(vm, sensorB, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @Test
    fun `startProvidingData seeds listLiveData with one placeholder row per sensor`() {
        val vm = VMSensorsInfo(sensorManager, dispatchersProvider)

        vm.startProvidingData()

        assertEquals(2, vm.listLiveData.size)
    }

    /**
     * Regression test for B28: indexOf(event.sensor) can return -1 on custom ROMs where the
     * SensorEvent's sensor instance doesn't match-by-equals the one captured at startup.
     * Writing to listLiveData[-1] used to throw IndexOutOfBoundsException.
     */
    @Test
    fun `indexOfSensor returns null for a sensor not in the tracked list`() {
        val vm = VMSensorsInfo(sensorManager, dispatchersProvider)
        vm.startProvidingData()

        assertNull(vm.indexOfSensor(unknownSensor))
    }

    @Test
    fun `indexOfSensor returns the row index for a tracked sensor`() {
        val vm = VMSensorsInfo(sensorManager, dispatchersProvider)
        vm.startProvidingData()

        assertEquals(0, vm.indexOfSensor(sensorA))
        assertEquals(1, vm.indexOfSensor(sensorB))
    }
}
