package com.galaxyjoy.cpuinfo.feat.storagebench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMStorageBench @Inject constructor(
    private val runner: StorageBenchmarkRunner,
    private val resultPrefs: StorageBenchResultPrefs,
    private val thermalStatusProvider: ThermalStatusProvider,
) : ViewModel() {

    sealed interface UiState {
        data class Idle(val previous: StorageBenchResultPrefs.SavedResult?) : UiState
        data class Running(val phase: StorageBenchmarkRunner.Phase) : UiState
        data class Done(
            val result: StorageBenchmark.Result,
            val previous: StorageBenchResultPrefs.SavedResult?,
        ) : UiState
        data object Aborted : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(Idle())
    val uiState = _uiState.asStateFlow()

    private val _thermalSnapshot = MutableStateFlow(thermalStatusProvider.snapshot())
    val thermalSnapshot = _thermalSnapshot.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is StorageBenchmarkRunner.State.Running -> UiState.Running(state.phase)

                    is StorageBenchmarkRunner.State.Finished -> {
                        val previous = resultPrefs.getLastResult()
                        resultPrefs.saveResult(state.result)
                        _thermalSnapshot.value = thermalStatusProvider.snapshot()
                        UiState.Done(state.result, previous)
                    }

                    StorageBenchmarkRunner.State.Aborted -> UiState.Aborted
                }
            }
        }
    }

    fun onStopClicked() {
        runner.requestStop()
    }

    fun onDoneClicked() {
        _thermalSnapshot.value = thermalStatusProvider.snapshot()
        _uiState.value = Idle()
    }

    override fun onCleared() {
        runner.requestStop()
        super.onCleared()
    }

    private fun Idle() = UiState.Idle(resultPrefs.getLastResult())
}
