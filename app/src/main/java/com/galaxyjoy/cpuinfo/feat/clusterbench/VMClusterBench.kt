package com.galaxyjoy.cpuinfo.feat.clusterbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.feat.infor.cpu.ClusterTopologyBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMClusterBench @Inject constructor(
    private val runner: ClusterBenchmarkRunner,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val clusterIndex: Int, val clusterCount: Int, val tier: ClusterTopologyBuilder.Tier) : UiState
        data class Done(val result: ClusterBenchmark.Result) : UiState
        data class Aborted(val reason: ClusterBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is ClusterBenchmarkRunner.State.Running ->
                        UiState.Running(state.clusterIndex, state.clusterCount, state.tier)
                    is ClusterBenchmarkRunner.State.Finished -> UiState.Done(state.result)
                    is ClusterBenchmarkRunner.State.Aborted -> UiState.Aborted(state.reason)
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
