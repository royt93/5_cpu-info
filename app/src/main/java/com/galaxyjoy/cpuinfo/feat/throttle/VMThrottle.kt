package com.galaxyjoy.cpuinfo.feat.throttle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMThrottle @Inject constructor(
    private val runner: ThrottleTestRunner,
    private val resultPrefs: ThrottleResultPrefs,
) : ViewModel() {

    sealed interface UiState {
        data class Idle(val previous: ThrottleResultPrefs.SavedResult?) : UiState

        data class Running(
            val elapsedMs: Long,
            val currentFreqMhz: Long,
            val currentTempC: Int,
            val samples: List<ThrottleFingerprint.Sample>,
        ) : UiState

        data class Done(
            val result: ThrottleFingerprint.Result,
            val previous: ThrottleResultPrefs.SavedResult?,
        ) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(Idle())
    val uiState = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun onStartClicked() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            runner.run { state ->
                _uiState.value = when (state) {
                    is ThrottleTestRunner.State.Running -> UiState.Running(
                        elapsedMs = state.elapsedMs,
                        currentFreqMhz = state.currentFreqMhz,
                        currentTempC = state.currentTempC,
                        samples = state.samples,
                    )

                    is ThrottleTestRunner.State.Finished -> {
                        val previous = resultPrefs.getLastResult()
                        resultPrefs.saveResult(state.result)
                        UiState.Done(state.result, previous)
                    }
                }
            }
        }
    }

    fun onStopClicked() {
        runner.requestStop()
    }

    fun onDoneClicked() {
        _uiState.value = Idle()
    }

    override fun onCleared() {
        runner.requestStop()
        super.onCleared()
    }

    private fun Idle() = UiState.Idle(resultPrefs.getLastResult())
}
