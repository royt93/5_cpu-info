package com.galaxyjoy.cpuinfo.domain.observable

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.galaxyjoy.cpuinfo.data.provider.DataProviderSensor
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.SensorReading
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Bridges the live [SensorEventListener] callback API into a cold [kotlinx.coroutines.flow.Flow] —
 * the first `callbackFlow` in this codebase (every other `Observable*Data` is either one-shot or a
 * `while(true) { emit; delay() }` poll loop; sensors instead push events on their own schedule).
 * Registers every sensor on collection start, unregisters all on collection cancel/close — same
 * register-on-start/unregister-on-stop contract [com.galaxyjoy.cpuinfo.feat.infor.sensor.VMSensorsInfo]
 * used to implement directly as a [SensorEventListener] itself.
 *
 * Runs on [DispatchersProvider.main], not `.io`: [android.hardware.SensorManager.registerListener]
 * (the 3-arg overload, no explicit [android.os.Handler]) delivers events via the calling thread's
 * `Looper` — a background IO-dispatcher thread has none, which would crash or silently drop events.
 */
class ObservableSensorData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderSensor: DataProviderSensor,
) : ImmutableInteractor<Unit, SensorReading>() {

    override val dispatcher = dispatchersProvider.main

    override fun createObservable(params: Unit) = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Some OEM HALs reuse the same values array across callbacks — copy defensively
                // since callbackFlow may buffer an emission before the collector reads it.
                trySend(SensorReading(event.sensor, event.values.copyOf()))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Do nothing
            }
        }

        val sensorList = dataProviderSensor.getSensorList()
        for (sensor in sensorList) {
            dataProviderSensor.registerListener(listener, sensor)
        }

        awaitClose {
            dataProviderSensor.unregisterListener(listener)
        }
    }
}
