package com.galaxyjoy.cpuinfo.feat.rambench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.feat.achievement.AchievementLogic
import com.galaxyjoy.cpuinfo.feat.achievement.AchievementPrefs
import com.galaxyjoy.cpuinfo.feat.throttle.ThermalStatusProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VMRamBench @Inject constructor(
    private val runner: RamBenchmarkRunner,
    private val resultPrefs: RamBenchResultPrefs,
    private val thermalStatusProvider: ThermalStatusProvider,
    private val achievementPrefs: AchievementPrefs,
) : ViewModel() {

    sealed interface UiState {
        data class Idle(val previous: RamBenchResultPrefs.SavedResult?) : UiState
        data class Running(val phase: RamBenchmarkRunner.Phase) : UiState
        data class Done(
            val result: RamBenchmark.Result,
            val previous: RamBenchResultPrefs.SavedResult?,
            val history: List<RamBenchResultPrefs.SavedResult>,
            /** U33 — this run's `writeMbPerSec` beat every prior saved run on this device. */
            val isNewRecord: Boolean = false,
        ) : UiState
        data class Aborted(val reason: RamBenchmark.AbortReason) : UiState
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
                    is RamBenchmarkRunner.State.Running -> UiState.Running(state.phase)

                    is RamBenchmarkRunner.State.Finished -> {
                        val previous = resultPrefs.getLastResult()
                        val previousBest = resultPrefs.getHistory().maxOfOrNull { it.writeMbPerSec }
                        val isNewRecord = AchievementLogic.isNewRecord(previousBest, state.result.writeMbPerSec)
                        if (isNewRecord) achievementPrefs.incrementRecordsBroken()
                        resultPrefs.saveResult(state.result)
                        _thermalSnapshot.value = thermalStatusProvider.snapshot()
                        UiState.Done(state.result, previous, resultPrefs.getHistory(), isNewRecord)
                    }

                    is RamBenchmarkRunner.State.Aborted -> UiState.Aborted(state.reason)
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
