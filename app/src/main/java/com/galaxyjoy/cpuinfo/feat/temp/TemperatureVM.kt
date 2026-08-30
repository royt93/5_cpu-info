package com.galaxyjoy.cpuinfo.feat.temp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableTemperatureData
import com.galaxyjoy.cpuinfo.domain.observe
import com.galaxyjoy.cpuinfo.util.NonNullMutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [FrmTemperature]
 */
@HiltViewModel
class TemperatureVM @Inject constructor(
    private val observableTemperatureData: ObservableTemperatureData,
) : ViewModel() {

    // Bound directly in frm_temperature.xml
    val isLoading = NonNullMutableLiveData(false)
    val isError = NonNullMutableLiveData(false)

    val temperatureData = MutableLiveData<TemperatureData.Available>()

    private var refreshingJob: Job? = null

    /**
     * Start temperature getting process. It also validates all temperatures availability.
     */
    fun startTemperatureRefreshing() {
        refreshingJob?.cancel()
        refreshingJob = viewModelScope.launch {
            observableTemperatureData.observe().collect { state ->
                when (state) {
                    TemperatureData.Probing -> {
                        isLoading.value = true
                        isError.value = false
                    }

                    is TemperatureData.Available -> {
                        isLoading.value = false
                        isError.value = false
                        temperatureData.value = state
                    }

                    TemperatureData.Unavailable -> {
                        isLoading.value = false
                        isError.value = true
                    }
                }
            }
        }
    }

    /**
     * Stop temperature getting process
     */
    fun stopTemperatureRefreshing() {
        refreshingJob?.cancel()
    }
}
