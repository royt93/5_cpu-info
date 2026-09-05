package com.galaxyjoy.cpuinfo.feat.storagetruth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMStorageTruth @Inject constructor(
    private val runner: StorageTruthRunner,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val phase: StorageTruthRunner.Phase, val blockIndex: Int, val blockCount: Int) : UiState
        data class Done(val result: StorageTruthBenchmark.Result) : UiState
        data class Aborted(val reason: StorageTruthBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is StorageTruthRunner.State.Running -> UiState.Running(state.phase, state.blockIndex, state.blockCount)
                    is StorageTruthRunner.State.Finished -> UiState.Done(state.result)
                    is StorageTruthRunner.State.Aborted -> UiState.Aborted(state.reason)
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
