package com.galaxyjoy.cpuinfo.domain.observable

import android.hardware.Sensor
import android.hardware.SensorEventListener
import com.galaxyjoy.cpuinfo.data.provider.DataProviderSensor
import com.galaxyjoy.cpuinfo.domain.observe
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Covers the register-on-start/unregister-on-stop contract of the `callbackFlow` — the part that
 * mirrors what [com.galaxyjoy.cpuinfo.feat.infor.sensor.VMSensorsInfo] used to guarantee directly
 * as a [SensorEventListener] itself (memory leak #1, B14). Whether an actual
 * [android.hardware.SensorEvent] gets mapped into a [com.galaxyjoy.cpuinfo.domain.model.SensorReading]
 * correctly isn't unit-testable here: `SensorEvent.values` is a public Java field, not a method —
 * MockK can't stub field reads on a real Android class, and the event's own constructor is
 * package-private — verified instead by the real-device smoke test.
 */
class ObservableSensorDataTest {

    private val dispatchersProvider: DispatchersProvider = mockk {
        every { main } returns UnconfinedTestDispatcher()
        every { io } returns UnconfinedTestDispatcher()
    }
    private val dataProviderSensor: DataProviderSensor = mockk()
    private val observable = ObservableSensorData(dispatchersProvider, dataProviderSensor)

    @Test
    fun `collecting registers every sensor from the list`() = runTest {
        val sensorA: Sensor = mockk()
        val sensorB: Sensor = mockk()
        every { dataProviderSensor.getSensorList() } returns listOf(sensorA, sensorB)
        every { dataProviderSensor.registerListener(any(), any()) } just Runs
        every { dataProviderSensor.unregisterListener(any()) } just Runs

        val job = launch { observable.observe().collect {} }
        advanceUntilIdle()

        verify(exactly = 1) { dataProviderSensor.registerListener(any<SensorEventListener>(), sensorA) }
        verify(exactly = 1) { dataProviderSensor.registerListener(any<SensorEventListener>(), sensorB) }

        job.cancel()
    }

    @Test
    fun `cancelling the collection unregisters the listener`() = runTest {
        val sensorA: Sensor = mockk()
        every { dataProviderSensor.getSensorList() } returns listOf(sensorA)
        every { dataProviderSensor.registerListener(any(), any()) } just Runs
        every { dataProviderSensor.unregisterListener(any()) } just Runs

        val job = launch { observable.observe().collect {} }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        verify(exactly = 1) { dataProviderSensor.unregisterListener(any<SensorEventListener>()) }
    }
}
