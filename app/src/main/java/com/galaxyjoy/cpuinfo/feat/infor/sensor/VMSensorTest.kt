package com.galaxyjoy.cpuinfo.feat.infor.sensor

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMSensorTest @Inject constructor(
    private val runner: SensorTestRunner,
    private val sensorManager: SensorManager,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState

        data class Running(
            val stepIndex: Int,
            val totalSteps: Int,
            val sensorType: Int,
            val liveValues: List<Float>?,
        ) : UiState

        data class Done(val results: List<SensorTestRunner.StepResult>) : UiState

        /** No sensor in [SensorTestEvaluator.TARGET_SENSOR_TYPES] exists on this device. */
        data object NoTestableSensors : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        val availableTypes = SensorTestEvaluator.TARGET_SENSOR_TYPES.filter {
            sensorManager.getDefaultSensor(it) != null
        }
        if (availableTypes.isEmpty()) {
            _uiState.value = UiState.NoTestableSensors
            return
        }

        testJob = viewModelScope.launch {
            val results = mutableListOf<SensorTestRunner.StepResult>()
            availableTypes.forEachIndexed { index, sensorType ->
                _uiState.value = UiState.Running(index, availableTypes.size, sensorType, liveValues = null)
                val outcome = runner.runStep(sensorType) { values ->
                    _uiState.value = UiState.Running(index, availableTypes.size, sensorType, values.toList())
                }
                results += SensorTestRunner.StepResult(sensorType, outcome)
            }
            _uiState.value = UiState.Done(results)
        }
    }

    fun onSkipClicked() {
        runner.requestSkip()
    }

    fun onDoneClicked() {
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        runner.requestSkip()
        super.onCleared()
    }
}
