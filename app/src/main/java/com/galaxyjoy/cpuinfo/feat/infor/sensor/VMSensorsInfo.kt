package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.Sensor
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.data.provider.DataProviderSensor
import com.galaxyjoy.cpuinfo.domain.model.SensorReading
import com.galaxyjoy.cpuinfo.domain.observable.ObservableSensorData
import com.galaxyjoy.cpuinfo.domain.observe
import com.galaxyjoy.cpuinfo.util.lifecycle.ListLiveData
import com.galaxyjoy.cpuinfo.util.round1
import com.galaxyjoy.cpuinfo.util.runOnApiAbove
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for sensors data
 *
 */
@HiltViewModel
class VMSensorsInfo @Inject constructor(
    private val dataProviderSensor: DataProviderSensor,
    private val observableSensorData: ObservableSensorData,
) : ViewModel() {

    val listLiveData = ListLiveData<Pair<String, String>>()

    private val sensorList = dataProviderSensor.getSensorList()

    private var collectJob: Job? = null

    /**
     * registerListener()/unregisterListener() (now inside [ObservableSensorData]'s `callbackFlow`)
     * are cheap Binder calls, not I/O — starting/stopping the collection job here keeps ordering
     * deterministic on fast tab switches instead of letting two launches race.
     */
    @Synchronized
    fun startProvidingData() {
        if (listLiveData.isEmpty()) {
            listLiveData.addAll(sensorList.map { Pair(it.name, " ") })
        }

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            observableSensorData.observe().collect { reading ->
                updateSensorInfo(reading)
            }
        }
    }

    @Synchronized
    fun stopProvidingData() {
        collectJob?.cancel()
        collectJob = null
    }

    override fun onCleared() {
        super.onCleared()
        // Guarantee sensor unregister even if stopProvidingData() was never called
        collectJob?.cancel()
    }

    /**
     * Replace sensor value with the new one
     */
    @Synchronized
    private fun updateSensorInfo(reading: SensorReading) {
        val updatedRowId = indexOfSensor(reading.sensor) ?: return
        listLiveData[updatedRowId] = Pair(reading.sensor.name, getSensorData(reading))
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
     * Detect sensor type for passed [SensorReading] and format it to the correct unit
     */
    @Suppress("DEPRECATION")
    private fun getSensorData(reading: SensorReading): String {
        var data = " "

        val sensorType = reading.sensor.type
        val values = reading.values
        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION ->
                data = "X=${values[0].round1()}m/s²  Y=${
                    values[1].round1()
                }m/s²  Z=${values[2].round1()}m/s²"

            Sensor.TYPE_GYROSCOPE ->
                data = "X=${values[0].round1()}rad/s  Y=${
                    values[1].round1()
                }rad/s  Z=${values[2].round1()}rad/s"

            Sensor.TYPE_ROTATION_VECTOR ->
                data = "X=${values[0].round1()}  Y=${
                    values[1].round1()
                }  Z=${values[2].round1()}"

            Sensor.TYPE_MAGNETIC_FIELD ->
                data = "X=${values[0].round1()}μT  Y=${
                    values[1].round1()
                }μT  Z=${values[2].round1()}μT"

            Sensor.TYPE_ORIENTATION ->
                data = "Azimuth=${values[0].round1()}°  Pitch=${
                    values[1].round1()
                }°  Roll=${values[2].round1()}°"

            Sensor.TYPE_PROXIMITY ->
                data = "Distance=${values[0].round1()}cm"

            Sensor.TYPE_AMBIENT_TEMPERATURE ->
                data = "Air temperature=${values[0].round1()}°C"

            Sensor.TYPE_LIGHT ->
                data = "Illuminance=${values[0].round1()}lx"

            Sensor.TYPE_PRESSURE ->
                data = "Air pressure=${values[0].round1()}hPa"

            Sensor.TYPE_RELATIVE_HUMIDITY ->
                data = "Relative humidity=${values[0].round1()}%"

            Sensor.TYPE_TEMPERATURE ->
                data = "Device temperature=${values[0].round1()}°C"
        }

        // TODO: Multiline support for this kind of data is necessary
        runOnApiAbove(17) {
            when (sensorType) {
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED ->
                    data = "X=${values[0].round1()}rad/s  Y=${
                        values[1].round1()
                    }rad/s  Z=${
                        values[2].round1()
                    }rad/s" /*\nEstimated drift: X=${
                    values[3].round1() }rad/s  Y=${
                    values[4].round1() }rad/s  Z=${
                    values[5].round1() }rad/s"*/
                Sensor.TYPE_GAME_ROTATION_VECTOR ->
                    data = "X=${values[0].round1()}  Y=${
                        values[1].round1()
                    }  Z=${values[2].round1()}"

                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED ->
                    data = "X=${values[0].round1()}μT  Y=${
                        values[1].round1()
                    }μT  Z=${
                        values[2].round1()
                    }μT" /*\nIron bias: X=${
                    values[3].round1() }μT  Y=${
                    values[4].round1() }μT  Z=${
                    values[5].round1() }μT"*/
            }
        }

        runOnApiAbove(18) {
            when (sensorType) {
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ->
                    data = "X=${values[0].round1()}  Y=${
                        values[1].round1()
                    }  Z=${values[2].round1()}"
            }
        }

        return data
    }
}
