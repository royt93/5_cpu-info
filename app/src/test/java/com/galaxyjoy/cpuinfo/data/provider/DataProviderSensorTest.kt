package com.galaxyjoy.cpuinfo.data.provider

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class DataProviderSensorTest {

    private val sensorManager: SensorManager = mockk(relaxed = true)
    private val provider = DataProviderSensor(sensorManager)

    @Test
    fun `getSensorList delegates to SensorManager TYPE_ALL`() {
        val sensorA: Sensor = mockk()
        every { sensorManager.getSensorList(Sensor.TYPE_ALL) } returns listOf(sensorA)

        assertEquals(listOf(sensorA), provider.getSensorList())
    }

    @Test
    fun `registerListener forwards to SensorManager with SENSOR_DELAY_NORMAL`() {
        val listener: SensorEventListener = mockk()
        val sensor: Sensor = mockk()

        provider.registerListener(listener, sensor)

        verify(exactly = 1) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @Test
    fun `unregisterListener forwards to SensorManager`() {
        val listener: SensorEventListener = mockk()

        provider.unregisterListener(listener)

        verify(exactly = 1) { sensorManager.unregisterListener(listener) }
    }
}
