package com.galaxyjoy.cpuinfo.feat.gpubench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMGpuBench @Inject constructor(
    private val runner: GpuBenchmarkRunner,
    private val resultPrefs: GpuBenchResultPrefs,
    private val thermalStatusProvider: ThermalStatusProvider,
) : ViewModel() {

    sealed interface UiState {
        data class Idle(val previous: GpuBenchResultPrefs.SavedResult?) : UiState
        data class Running(val warmingUp: Boolean) : UiState
        data class Done(
            val result: GpuBenchmark.Result,
            val previous: GpuBenchResultPrefs.SavedResult?,
        ) : UiState
        data class Aborted(val reason: GpuBenchmark.AbortReason) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(Idle())
    val uiState = _uiState.asStateFlow()

    private val _thermalSnapshot = MutableStateFlow(thermalStatusProvider.snapshot())
    val thermalSnapshot = _thermalSnapshot.asStateFlow()

    init {
        viewModelScope.launch {
            runner.state.collect { state ->
                _uiState.value = when (state) {
                    is GpuBenchmarkRunner.State.Idle -> Idle()
                    is GpuBenchmarkRunner.State.Running -> UiState.Running(state.warmingUp)

                    is GpuBenchmarkRunner.State.Finished -> {
                        val previous = resultPrefs.getLastResult()
                        resultPrefs.saveResult(state.result)
                        _thermalSnapshot.value = thermalStatusProvider.snapshot()
                        UiState.Done(state.result, previous)
                    }

                    is GpuBenchmarkRunner.State.Aborted -> UiState.Aborted(state.reason)
                }
            }
        }
    }

    fun newRenderer() = runner.newRenderer()

    fun onStartClicked() {
        if (_uiState.value is UiState.Running) return
        runner.start()
    }

    fun onStopClicked() {
        runner.requestStop()
    }

    fun onDoneClicked() {
        _thermalSnapshot.value = thermalStatusProvider.snapshot()
        runner.reset()
    }

    override fun onCleared() {
        runner.requestStop()
        super.onCleared()
    }

    private fun Idle() = UiState.Idle(resultPrefs.getLastResult())
}
