package com.galaxyjoy.cpuinfo.domain.observable

import com.galaxyjoy.cpuinfo.data.provider.DataProviderTemperature
import com.galaxyjoy.cpuinfo.domain.ImmutableInteractor
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.util.DispatchersProvider
import com.galaxyjoy.cpuinfo.util.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObservableTemperatureData @Inject constructor(
    dispatchersProvider: DispatchersProvider,
    private val dataProviderTemperature: DataProviderTemperature,
    private val prefs: Prefs,
) : ImmutableInteractor<Unit, TemperatureData>() {

    override val dispatcher = dispatchersProvider.io

    override fun createObservable(params: Unit) = flow {
        val cachedPath = prefs.get(CPU_TEMP_PATH_KEY, "")
        val cpuTempPath = if (cachedPath.isNotEmpty()) {
            cachedPath
        } else {
            emit(TemperatureData.Probing)
            dataProviderTemperature.findCpuTempPath()?.also { prefs.insert(CPU_TEMP_PATH_KEY, it) }
        }

        val hasBattery = dataProviderTemperature.getBatteryTemperature() != null
        if (cpuTempPath == null && !hasBattery) {
            emit(TemperatureData.Unavailable)
            return@flow
        }

        while (true) {
            emit(
                TemperatureData.Available(
                    cpuTemp = cpuTempPath?.let { dataProviderTemperature.getCpuTemp(it) },
                    batteryTemp = dataProviderTemperature.getBatteryTemperature(),
                ),
            )
            delay(REFRESH_DELAY_MS)
        }
    }

    companion object {
        // Deliberately a different key than the old RxJava VM's "temp_result_key": that one
        // cached a whole CpuTemperatureResult object (Gson-serialized to JSON under the hood),
        // this caches just the path string — reusing the old key would read back a JSON blob as
        // if it were a file path.
        private const val CPU_TEMP_PATH_KEY = "temp_cpu_path_key"
        private const val REFRESH_DELAY_MS = 3000L
    }
}
