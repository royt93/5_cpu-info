package com.galaxyjoy.cpuinfo.feat.siliconlottery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMSiliconLottery @Inject constructor(
    private val runner: SiliconLotteryRunner,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val coreIndex: Int, val coreCount: Int) : UiState
        data class Done(val result: SiliconLotteryBenchmark.Result) : UiState
        data class Aborted(val reason: SiliconLotteryBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is SiliconLotteryRunner.State.Running -> UiState.Running(state.coreIndex, state.coreCount)
                    is SiliconLotteryRunner.State.Finished -> UiState.Done(state.result)
                    is SiliconLotteryRunner.State.Aborted -> UiState.Aborted(state.reason)
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
