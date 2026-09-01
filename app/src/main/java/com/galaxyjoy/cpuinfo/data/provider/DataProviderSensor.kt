package com.galaxyjoy.cpuinfo.data.provider

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import javax.inject.Inject

/**
 * Thin wrapper over [SensorManager] — kept this way (rather than folding registration into
 * [com.galaxyjoy.cpuinfo.domain.observable.ObservableSensorData]'s `callbackFlow`) so every
 * direct Android API call in the sensor region lives in one place, same as every other region.
 */
class DataProviderSensor @Inject constructor(
    private val sensorManager: SensorManager,
) {

    fun getSensorList(): List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

    fun registerListener(listener: SensorEventListener, sensor: Sensor) {
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregisterListener(listener: SensorEventListener) {
        sensorManager.unregisterListener(listener)
    }
}
