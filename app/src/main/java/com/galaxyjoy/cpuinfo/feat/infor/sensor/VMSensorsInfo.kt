package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.round1
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for sensors data
 *
 */
@HiltViewModel
class VMSensorsInfo @Inject constructor(
    private val sensorManager: SensorManager,
    private val dispatchersProvider: DispatchersProvider
) : ViewModel(), SensorEventListener {

    val listLiveData = ListLiveData<Pair<String, String>>()

    private val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)

    /**
     * registerListener()/unregisterListener() are cheap Binder calls, not I/O — running them on
     * viewModelScope.launch(io) let start/stop calls race and interleave on fast tab switches.
     * Calling directly on the caller's thread keeps ordering deterministic.
     */
    @Synchronized
    fun startProvidingData() {
        if (listLiveData.isEmpty()) {
            listLiveData.addAll(sensorList.map { Pair(it.name, " ") })
        }

        for (sensor in sensorList) {
            sensorManager.registerListener(
                this, sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    @Synchronized
    fun stopProvidingData() {
        sensorManager.unregisterListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        // Guarantee sensor unregister even if stopProvidingData() was never called
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent) {
        updateSensorInfo(event)
    }

    /**
     * Replace sensor value with the new one
     */
    @Synchronized
    private fun updateSensorInfo(event: SensorEvent) {
        val updatedRowId = indexOfSensor(event.sensor) ?: return
        listLiveData[updatedRowId] = Pair(event.sensor.name, getSensorData(event))
    }

    /**
     * indexOf can return -1 on custom ROMs where the SensorEvent's sensor instance doesn't
     * match-by-equals the one returned by getSensorList() at startup — guard against writing to
     * a negative/out-of-range row.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    internal fun indexOfSensor(sensor: Sensor): Int? {
        val index = sensorList.indexOf(sensor)
        return index.takeIf { it in listLiveData.indices }
    }

    /**
     * Detect sensor type for passed [SensorEvent] and format it to the correct unit
     */
    @Suppress("DEPRECATION")
    private fun getSensorData(event: SensorEvent): String {
        var data = " "

        val sensorType = event.sensor.type
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION ->
                data = "X=${event.values[0].round1()}m/s²  Y=${
                    event.values[1].round1()
                }m/s²  Z=${event.values[2].round1()}m/s²"

            Sensor.TYPE_GYROSCOPE ->
                data = "X=${event.values[0].round1()}rad/s  Y=${
                    event.values[1].round1()
                }rad/s  Z=${event.values[2].round1()}rad/s"

            Sensor.TYPE_ROTATION_VECTOR ->
                data = "X=${event.values[0].round1()}  Y=${
                    event.values[1].round1()
                }  Z=${event.values[2].round1()}"

            Sensor.TYPE_MAGNETIC_FIELD ->
                data = "X=${event.values[0].round1()}μT  Y=${
                    event.values[1].round1()
                }μT  Z=${event.values[2].round1()}μT"

            Sensor.TYPE_ORIENTATION ->
                data = "Azimuth=${event.values[0].round1()}°  Pitch=${
                    event.values[1].round1()
                }°  Roll=${event.values[2].round1()}°"

            Sensor.TYPE_PROXIMITY ->
                data = "Distance=${event.values[0].round1()}cm"

            Sensor.TYPE_AMBIENT_TEMPERATURE ->
                data = "Air temperature=${event.values[0].round1()}°C"

            Sensor.TYPE_LIGHT ->
                data = "Illuminance=${event.values[0].round1()}lx"

            Sensor.TYPE_PRESSURE ->
                data = "Air pressure=${event.values[0].round1()}hPa"

            Sensor.TYPE_RELATIVE_HUMIDITY ->
                data = "Relative humidity=${event.values[0].round1()}%"

            Sensor.TYPE_TEMPERATURE ->
                data = "Device temperature=${event.values[0].round1()}°C"
        }

        // TODO: Multiline support for this kind of data is necessary
        runOnApiAbove(17) {
            when (sensorType) {
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED ->
                    data = "X=${event.values[0].round1()}rad/s  Y=${
                        event.values[1].round1()
                    }rad/s  Z=${
                        event.values[2].round1()
                    }rad/s" /*\nEstimated drift: X=${
                    event.values[3].round1() }rad/s  Y=${
                    event.values[4].round1() }rad/s  Z=${
                    event.values[5].round1() }rad/s"*/
                Sensor.TYPE_GAME_ROTATION_VECTOR ->
                    data = "X=${event.values[0].round1()}  Y=${
                        event.values[1].round1()
                    }  Z=${event.values[2].round1()}"

                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED ->
                    data = "X=${event.values[0].round1()}μT  Y=${
                        event.values[1].round1()
                    }μT  Z=${
                        event.values[2].round1()
                    }μT" /*\nIron bias: X=${
                    event.values[3].round1() }μT  Y=${
                    event.values[4].round1() }μT  Z=${
                    event.values[5].round1() }μT"*/
            }
        }

        runOnApiAbove(18) {
            when (sensorType) {
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ->
                    data = "X=${event.values[0].round1()}  Y=${
                        event.values[1].round1()
                    }  Z=${event.values[2].round1()}"
            }
        }

        return data
    }
}
