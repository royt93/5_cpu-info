package com.galaxyjoy.cpuinfo.feat.infor.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyjoy.cpuinfo.domain.model.TemperatureData
import com.galaxyjoy.cpuinfo.domain.model.TimeSeriesPoint
import com.galaxyjoy.cpuinfo.domain.observable.ObservableCpuData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableRamData
import com.galaxyjoy.cpuinfo.domain.observable.ObservableTemperatureData
import com.galaxyjoy.cpuinfo.domain.observe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [FrmDashboard] (F01) — accumulates session-only rolling history for CPU
 * utilization, RAM usage, and battery temperature so they can be charted over the last
 * [HISTORY_WINDOW_MS]. Zooming/panning into a shorter window (1/5/15 min) is left to
 * MPAndroidChart's built-in pinch-zoom/pan rather than a custom time-window picker.
 */
@HiltViewModel
class VMDashboard @Inject constructor(
    private val observableCpuData: ObservableCpuData,
    private val observableRamData: ObservableRamData,
    private val observableTemperatureData: ObservableTemperatureData,
) : ViewModel() {

    data class UiState(
        val cpuLoadPoints: List<TimeSeriesPoint> = emptyList(),
        val ramUsedPoints: List<TimeSeriesPoint> = emptyList(),
        val batteryTempPoints: List<TimeSeriesPoint> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val cpuHistory = HistoryBuffer(HISTORY_WINDOW_MS)
    private val ramHistory = HistoryBuffer(HISTORY_WINDOW_MS)
    private val tempHistory = HistoryBuffer(HISTORY_WINDOW_MS)

    private var collectJob: Job? = null

    fun startCollecting() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            launch {
                observableCpuData.observe().collect { cpu ->
                    val avgUtilizationPercent = cpu.frequencies
                        .filter { it.max > 0 }
                        .map { it.current.toFloat() / it.max.toFloat() * 100f }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.toFloat()
                        ?: return@collect
                    _uiState.update { it.copy(cpuLoadPoints = cpuHistory.record(avgUtilizationPercent)) }
                }
            }
            launch {
                observableRamData.observe().collect { ram ->
                    val usedPercent = (100 - ram.availablePercentage).toFloat()
                    _uiState.update { it.copy(ramUsedPoints = ramHistory.record(usedPercent)) }
                }
            }
            launch {
                observableTemperatureData.observe().collect { temp ->
                    val batteryTemp = (temp as? TemperatureData.Available)?.batteryTemp ?: return@collect
                    _uiState.update { it.copy(batteryTempPoints = tempHistory.record(batteryTemp)) }
                }
            }
        }
    }

    fun stopCollecting() {
        collectJob?.cancel()
    }

    companion object {
        internal const val HISTORY_WINDOW_MS = 15 * 60 * 1000L
    }
}
