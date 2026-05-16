package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

class VMSensorsInfoTest {

    private val sensorManager: SensorManager = mockk(relaxed = true) {
        every { getSensorList(Sensor.TYPE_ALL) } returns emptyList()
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
}
