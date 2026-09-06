package com.galaxyjoy.cpuinfo.feat.sensortruth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMSensorTruth @Inject constructor(
    private val runner: SensorTruthRunner,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val sensorIndex: Int, val sensorCount: Int, val sensorType: Int) : UiState
        data class Done(val result: SensorTruthBenchmark.Result) : UiState
        data class Aborted(val reason: SensorTruthBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is SensorTruthRunner.State.Running -> UiState.Running(state.sensorIndex, state.sensorCount, state.sensorType)
                    is SensorTruthRunner.State.Finished -> UiState.Done(state.result)
                    is SensorTruthRunner.State.Aborted -> UiState.Aborted(state.reason)
                }
            }
        }
    }

    fun onStopClicked() {
        runner.requestStop()
    }

    fun onDoneClicked() {
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        runner.requestStop()
        super.onCleared()
    }
}
