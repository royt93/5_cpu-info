package com.galaxyjoy.cpuinfo.feat.ramtruth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMRamTruth @Inject constructor(
    private val runner: RamTruthRunner,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val chunkIndex: Int, val chunkCount: Int) : UiState
        data class Done(val result: RamTruthBenchmark.Result) : UiState
        data class Aborted(val reason: RamTruthBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is RamTruthRunner.State.Running -> UiState.Running(state.chunkIndex, state.chunkCount)
                    is RamTruthRunner.State.Finished -> UiState.Done(state.result)
                    is RamTruthRunner.State.Aborted -> UiState.Aborted(state.reason)
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
